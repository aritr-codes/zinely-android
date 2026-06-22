package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.data.serialization.DocumentDecodeException
import com.aritr.zinely.core.data.serialization.DocumentSerializationException
import com.aritr.zinely.core.data.serialization.DocumentSerializer
import com.aritr.zinely.core.data.serialization.JsonDocumentSerializer
import com.aritr.zinely.core.data.serialization.NewerSchemaVersionException
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.data.validation.DocumentValidator
import com.aritr.zinely.core.data.validation.DefaultDocumentValidator
import com.aritr.zinely.core.data.validation.ValidationIssue
import com.aritr.zinely.core.data.validation.Severity
import com.aritr.zinely.core.model.ZineDocument
import java.io.IOException
import java.nio.charset.CharacterCodingException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.coroutines.cancellation.CancellationException

/**
 * The file-only [DocumentRepository] for app-private internal storage (ADR-025 / ADR-026), Build
 * Order PR-A Step 3. This is **adapter glue only**: every byte-level guarantee lives in the
 * Android-free durability core ([AtomicFileStore], ADR-021) and every parse/validate rule lives in
 * the pure-Kotlin `:core:data` stack ([DocumentSerializer], [DocumentValidator]). The repository
 * orchestrates them and maps their failures onto the [DataError] taxonomy — it owns no business
 * logic of its own.
 *
 * **Durability (ADR-026, frozen):** [save]'s commit is a straight blocking call into
 * [AtomicFileStore.write]. Although [save]/[load] are `suspend` (the contract), the commit body
 * never suspends — there is no `runInterruptible`, no `withContext`, no cancellation check around
 * the write — so coroutine cancellation can never tear a half-written document. Storage failures
 * are simply propagated as [DataError.Io].
 *
 * **Path safety:** [projectId] is untrusted. Every path flows through the single [resolveDocument]
 * chokepoint — whitelist match, normalise, and a `startsWith(projectsRoot)` containment check — so
 * a malicious id can never escape `rootDir/projects/`. Scope is app-private internal storage, where
 * there are no attacker-controlled symlinks (ADR-025); the containment check uses `NOFOLLOW_LINKS`
 * as belt-and-braces.
 */
public class DocumentRepositoryImpl(
    rootDir: Path,
    private val store: AtomicFileStore,
    private val serializer: DocumentSerializer = JsonDocumentSerializer(),
    private val validator: DocumentValidator = DefaultDocumentValidator(),
) : DocumentRepository {

    /** `rootDir/projects`, normalised once so the containment check is a cheap prefix test. */
    private val projectsRoot: Path = rootDir.resolve(PROJECTS_DIR).normalize()

    override suspend fun load(projectId: String): DataResult<ZineDocument> {
        val path = resolveDocument(projectId) ?: return invalidId()

        // ADR-021 open-time recovery: a torn/unreadable primary falls back to the fsync'd .bak. The
        // predicate must NOT reject a newer-than-supported document — that is a complete, deliberate
        // payload we surface as SchemaTooNew below, never something to mask by loading an older backup.
        val bytes = try {
            store.read(path, ::isLoadable)
        } catch (e: IOException) {
            return failure(DataError.Io("failed to read document for '$projectId'", e))
        }
        if (bytes == null) {
            // read() returns null when neither primary nor backup is usable. A present-but-unusable
            // primary is corruption; a genuinely absent one is NotFound. (AtomicFileStore exposes no
            // richer status, so the primary's existence is the only discriminator available here.)
            return failure(
                if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                    DataError.Corrupt("document for '$projectId' is unreadable or corrupt")
                } else {
                    DataError.NotFound(projectId)
                },
            )
        }

        val document = try {
            decode(bytes)
        } catch (e: NewerSchemaVersionException) {
            return failure(DataError.SchemaTooNew(e.documentVersion, e.supportedVersion))
        } catch (e: DocumentSerializationException) {
            return failure(DataError.Corrupt("document for '$projectId' could not be decoded", e))
        } catch (e: CharacterCodingException) {
            return failure(DataError.Corrupt("document for '$projectId' is not valid UTF-8", e))
        }

        val validation = validator.validate(document)
        if (!validation.isValid) return failure(DataError.Invalid(validation.errors))
        return DataResult.Success(document)
    }

    override suspend fun save(projectId: String, document: ZineDocument): DataResult<Unit> {
        val path = resolveDocument(projectId) ?: return invalidId()

        val text = try {
            serializer.serialize(document)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A valid in-memory document always serialises; a failure here is an internal invariant
            // break, not corrupt stored bytes — hence Unknown rather than Corrupt.
            return failure(DataError.Unknown("failed to serialize document for '$projectId'", e))
        }

        // ADR-026 commit: synchronous, blocking, fail-closed. No dispatch, no interruption — the
        // write runs to completion and any storage failure propagates as Io.
        try {
            store.write(path, text.encodeToByteArray())
        } catch (e: IOException) {
            return failure(DataError.Io("failed to write document for '$projectId'", e))
        }
        return DataResult.Success(Unit)
    }

    /**
     * The [AtomicFileStore.read] integrity predicate — it decides, per ADR-026, whether a primary may
     * be recovered from its `.bak`. `true` means "intact, do **not** roll back"; `false` means
     * "corrupt, recover from the backup".
     *
     * Recovery is reserved for **genuine corruption** — a torn/malformed payload that cannot be parsed
     * into a document ([DocumentDecodeException]) or is not even valid UTF-8 ([CharacterCodingException]).
     * A payload that parses into a *complete* document but is then deliberately **refused** — unsupported
     * persisted format, missing/newer schema version, or a missing migration path — is **intact**, not
     * corrupt. It must surface its own error (handled by the caller) and must never be silently rolled
     * back to a stale backup; doing so would violate the ADR-026 consistency model. Such payloads
     * therefore count as "loadable" here so [AtomicFileStore.read] returns the primary untouched.
     */
    private fun isLoadable(bytes: ByteArray): Boolean =
        try {
            decode(bytes)
            true
        } catch (e: DocumentSerializationException) {
            // Only a structural decode failure is corruption; every other refusal is an intact document.
            e !is DocumentDecodeException
        } catch (_: CharacterCodingException) {
            false
        }

    /** Decode persisted bytes, rejecting malformed UTF-8 strictly so damaged bytes never slip through. */
    private fun decode(bytes: ByteArray): ZineDocument =
        serializer.deserialize(bytes.decodeToString(throwOnInvalidSequence = true))

    /**
     * The single path-resolution chokepoint. Returns the document path for [projectId], or `null`
     * when the id is not on the safe whitelist or would resolve outside [projectsRoot]. No path is
     * ever built for a project outside this function.
     */
    private fun resolveDocument(projectId: String): Path? {
        if (!PROJECT_ID.matches(projectId)) return null
        val resolved = projectsRoot.resolve(projectId).resolve(DOCUMENT_FILE).normalize()
        if (!resolved.startsWith(projectsRoot)) return null
        return resolved
    }

    private fun invalidId(): DataResult<Nothing> = failure(
        DataError.Invalid(
            listOf(
                ValidationIssue(
                    code = "projectId.invalid",
                    message = "project id is not a safe identifier",
                    severity = Severity.ERROR,
                    path = "projectId",
                ),
            ),
        ),
    )

    private fun failure(error: DataError): DataResult<Nothing> = DataResult.Failure(error)

    private companion object {
        const val PROJECTS_DIR = "projects"
        const val DOCUMENT_FILE = "document.json"

        /** Untrusted-id whitelist: excludes `.`, `/`, `\`, so traversal sequences cannot form. */
        val PROJECT_ID = Regex("^[A-Za-z0-9_-]{1,64}$")
    }
}
