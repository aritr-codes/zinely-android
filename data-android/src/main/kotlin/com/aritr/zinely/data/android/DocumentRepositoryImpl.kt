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
 * **Path safety:** [projectId] is untrusted. Every path flows through the shared [ProjectPaths]
 * chokepoint (ADR-042 extraction — one whitelist/containment rule for every `:data-android` store)
 * — whitelist match, normalise, containment check — so a malicious id can never escape
 * `rootDir/projects/`. Scope is app-private internal storage, where there are no
 * attacker-controlled symlinks (ADR-025); the containment check uses `NOFOLLOW_LINKS` as
 * belt-and-braces.
 */
public class DocumentRepositoryImpl(
    rootDir: Path,
    private val store: AtomicFileStore,
    private val serializer: DocumentSerializer = JsonDocumentSerializer(),
    private val validator: DocumentValidator = DefaultDocumentValidator(),
    // ADR-036 storage-aware classification seam. Probes the usable space of the target's filesystem so a
    // write `IOException` paired with a genuine space shortfall is attributed to storage exhaustion rather
    // than flattened into a generic Io. Injected for deterministic unit tests; the production default reads
    // the real usable-bytes at the nearest existing ancestor of the target (java.io, no Android dependency)
    // — the document file itself does not exist on a first save, and `File.getUsableSpace()` returns 0 for a
    // non-existent path on some platforms, which would otherwise misread as "full". NOT errno/string
    // sniffing — see [classifyWriteFailure] and [DataError.OutOfSpace].
    private val freeSpaceProbe: (Path) -> Long = { usableSpaceNear(it) },
) : DocumentRepository {

    /** The shared untrusted-id → path chokepoint (ADR-042). */
    private val paths = ProjectPaths(rootDir)

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
        // write runs to completion and any storage failure propagates as a DataResult.Failure.
        val bytes = text.encodeToByteArray()
        try {
            store.write(path, bytes)
        } catch (e: IOException) {
            return failure(classifyWriteFailure(projectId, path, bytes.size.toLong(), e))
        }
        return DataResult.Success(Unit)
    }

    /**
     * Map a write `IOException` to the honest [DataError] (ADR-036). When the target filesystem's usable
     * space is below the payload size, the device **verifiably cannot hold this document**, so the failure
     * is attributed to storage exhaustion ([DataError.OutOfSpace]) — a true statement of the disk's state,
     * actionable by the user ("free up space"). Otherwise it stays the generic [DataError.Io].
     *
     * This is a deliberately **false-negative-biased** heuristic, not errno/string sniffing: `java.nio
     * Files.write` (the frozen `AtomicFileStore` write path) surfaces a full disk as a plain `IOException`
     * with no structured `errno`, so the probe is the only honest signal available without changing the
     * frozen pure core. The threshold uses the payload size alone; an overwrite's transient `.bak` copy
     * needs more, so a shortfall in the `payload < usable < payload+existing` band classifies as `Io` (a
     * safe under-claim). We never claim "out of space" unless the disk demonstrably can't fit the payload.
     */
    private fun classifyWriteFailure(
        projectId: String,
        path: Path,
        payloadBytes: Long,
        cause: IOException,
    ): DataError {
        val outOfSpace = try {
            freeSpaceProbe(path) < payloadBytes
        } catch (_: Exception) {
            // A probe that cannot report (e.g. a SecurityException) must never *invent* a storage-full
            // claim — fall back to the generic Io, preserving the no-false-positive bar.
            false
        }
        val message = "failed to write document for '$projectId'"
        return if (outOfSpace) DataError.OutOfSpace(message, cause) else DataError.Io(message, cause)
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

    /** Resolve via the shared [ProjectPaths] chokepoint; `null` when [projectId] is unsafe. */
    private fun resolveDocument(projectId: String): Path? = paths.documentFile(projectId)

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
        /**
         * Usable bytes of the filesystem holding [path], measured at the nearest **existing** ancestor
         * (ADR-036 default probe). A first save's `document.json` — and possibly its project dir — does not
         * exist yet, and `File.getUsableSpace()` returns 0 for a non-existent abstract path on some
         * platforms (Windows), which would falsely read as "no space"; walking up to a real directory
         * yields the true free space. In production the app-private `filesDir` is always an existing
         * ancestor, so a real measurement is always found; the `?: 0L` is only a defensive fallback for the
         * unreachable case of no existing ancestor at all.
         */
        fun usableSpaceNear(path: Path): Long {
            var p: Path? = path
            while (p != null && !Files.exists(p)) p = p.parent
            return p?.toFile()?.usableSpace ?: 0L
        }
    }
}
