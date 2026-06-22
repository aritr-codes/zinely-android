package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.serialization.DocumentSerializer
import com.aritr.zinely.core.data.serialization.JsonDocumentSerializer
import com.aritr.zinely.core.data.serialization.PersistedFormat
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.data.storage.FileSystemOps
import com.aritr.zinely.core.data.storage.NioFileSystemOps
import com.aritr.zinely.core.data.validation.DefaultDocumentValidator
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Contract for [DocumentRepositoryImpl] (PR-A Step 3): the file-only adapter that glues the
 * Android-free durability core ([AtomicFileStore]) and the pure-Kotlin (de)serialize/validate
 * stack to the [com.aritr.zinely.core.data.repository.DocumentRepository] contract. All pure JVM —
 * the temp dir stands in for the app's private `filesDir`, so no device/emulator is needed.
 *
 * The durability-regression tests pin ADR-026: the commit stays synchronous/blocking, never
 * `runInterruptible`/`withContext`/async.
 */
class DocumentRepositoryImplTest {

    private val serializer = JsonDocumentSerializer()
    private val validator = DefaultDocumentValidator()

    private fun tempRoot(): Path = Files.createTempDirectory("zinely-doc-repo")

    private fun repo(
        root: Path,
        store: AtomicFileStore = AtomicFileStore(NioFileSystemOps),
        serializer: DocumentSerializer = this.serializer,
    ) = DocumentRepositoryImpl(root, store, serializer, validator)

    private fun docPath(root: Path, id: String): Path =
        root.resolve("projects").resolve(id).resolve("document.json")

    /** A schema-valid single-sheet zine: 8 pages, unique in-range indices, no element issues. */
    private fun validDoc(): ZineDocument = ZineDocument(
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = PaperSize.LETTER,
        pages = (0 until ZineFormat.SINGLE_SHEET_8.pageCount).map { Page(index = it, role = PageRole.INTERIOR) },
    )

    /** Deserialises fine but fails validation: page count does not match the format (ERROR). */
    private fun invalidDoc(): ZineDocument = ZineDocument(
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = PaperSize.LETTER,
        pages = emptyList(),
    )

    private fun writeFile(path: Path, bytes: ByteArray) {
        Files.createDirectories(path.parent)
        Files.write(path, bytes)
    }

    // ---------------------------------------------------------------------------------------------
    // LOAD
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `load returns the previously saved document`() = runTest {
        val root = tempRoot()
        val repo = repo(root)
        val doc = validDoc()
        assertTrue(repo.save("proj1", doc) is DataResult.Success)

        val result = repo.load("proj1")

        assertEquals(doc, (result as DataResult.Success).value)
    }

    @Test
    fun `load of a corrupt payload with no backup fails with Corrupt`() = runTest {
        val root = tempRoot()
        writeFile(docPath(root, "proj1"), "this is not a json document".toByteArray())

        val error = (repo(root).load("proj1") as DataResult.Failure).error

        assertTrue("expected Corrupt, got $error", error is DataError.Corrupt)
    }

    @Test
    fun `load of a newer-than-supported schema fails with SchemaTooNew`() = runTest {
        val root = tempRoot()
        // Well-formed JSON declaring a schema version this build does not support yet.
        writeFile(docPath(root, "proj1"), """{"schemaVersion":2}""".toByteArray())

        val error = (repo(root).load("proj1") as DataResult.Failure).error

        assertTrue("expected SchemaTooNew, got $error", error is DataError.SchemaTooNew)
        error as DataError.SchemaTooNew
        assertEquals(2, error.documentVersion)
        assertEquals(1, error.supportedVersion)
    }

    @Test
    fun `load of a document that fails ERROR-severity validation fails with Invalid`() = runTest {
        val root = tempRoot()
        // Encoded by the real serializer (which does not validate); the page count is wrong.
        writeFile(docPath(root, "proj1"), serializer.serialize(invalidDoc()).toByteArray())

        val error = (repo(root).load("proj1") as DataResult.Failure).error

        assertTrue("expected Invalid, got $error", error is DataError.Invalid)
        assertTrue((error as DataError.Invalid).issues.isNotEmpty())
    }

    @Test
    fun `load surfaces a storage read failure as Io`() = runTest {
        val root = tempRoot()
        // A backend whose repair-write (atomicReplace) fails, forcing AtomicFileStore.read's
        // pathological IOException path when it tries to repair a corrupt primary from a good backup.
        val store = AtomicFileStore(object : FileSystemOps by NioFileSystemOps {
            override fun atomicReplace(source: Path, replacing: Path): Unit =
                throw IOException("repair write blocked")
        })
        val path = docPath(root, "proj1")
        // Good backup the recovery will select...
        writeFile(path.resolveSibling("document.json.bak"), serializer.serialize(validDoc()).toByteArray())
        // ...and a corrupt, *undeletable* primary (a non-empty directory) so both repair and the
        // subsequent quarantine-delete fail and read() throws rather than silently recovering.
        Files.createDirectory(path)
        Files.write(path.resolve("child"), "x".toByteArray())

        val error = (repo(root, store).load("proj1") as DataResult.Failure).error

        assertTrue("expected Io, got $error", error is DataError.Io)
    }

    // --- ADR-026 recovery classification: recover ONLY for corruption, never roll back an intact doc ---

    /** Writes [primary] bytes to the document path and a *valid* serialized document as its `.bak`. */
    private fun writePrimaryWithValidBackup(root: Path, id: String, primary: ByteArray): ZineDocument {
        val path = docPath(root, id)
        writeFile(path, primary)
        val backupDoc = validDoc()
        Files.write(path.resolveSibling("document.json.bak"), serializer.serialize(backupDoc).toByteArray())
        return backupDoc
    }

    @Test
    fun `load does not roll back to a stale backup when the primary declares an unsupported format`() = runTest {
        val root = tempRoot()
        // A complete, parseable document that merely declares a format this build does not read.
        writePrimaryWithValidBackup(root, "proj1", """{"_encoding":"protobuf","schemaVersion":1}""".toByteArray())

        val result = repo(root).load("proj1")

        // It must surface the refusal, never silently promote the older .bak (ADR-026).
        assertTrue("must not roll back to backup, got $result", result is DataResult.Failure)
        assertTrue((result as DataResult.Failure).error is DataError.Corrupt)
    }

    @Test
    fun `load does not roll back to a stale backup when the primary needs an unavailable migration`() = runTest {
        val root = tempRoot()
        // schemaVersion 0 is older than current but no migrator is registered for it -> refused, intact.
        writePrimaryWithValidBackup(root, "proj1", """{"schemaVersion":0}""".toByteArray())

        val result = repo(root).load("proj1")

        assertTrue("must not roll back to backup, got $result", result is DataResult.Failure)
        assertTrue((result as DataResult.Failure).error is DataError.Corrupt)
    }

    @Test
    fun `load does not roll back to a stale backup when the primary is a newer schema`() = runTest {
        val root = tempRoot()
        writePrimaryWithValidBackup(root, "proj1", """{"schemaVersion":2}""".toByteArray())

        val result = repo(root).load("proj1")

        assertTrue("must not roll back to backup, got $result", result is DataResult.Failure)
        val error = (result as DataResult.Failure).error
        assertTrue("expected SchemaTooNew, got $error", error is DataError.SchemaTooNew)
        assertEquals(2, (error as DataError.SchemaTooNew).documentVersion)
    }

    @Test
    fun `load recovers from a valid backup only when the primary is genuinely corrupt`() = runTest {
        val root = tempRoot()
        // Torn/malformed bytes (not parseable) ARE corruption — here recovery to the good backup is correct.
        val backupDoc = writePrimaryWithValidBackup(root, "proj1", "this is not a json document".toByteArray())

        val result = repo(root).load("proj1")

        assertEquals(backupDoc, (result as DataResult.Success).value)
    }

    @Test
    fun `load rejects an invalid project id with Invalid`() = runTest {
        val error = (repo(tempRoot()).load("../etc/passwd") as DataResult.Failure).error

        assertTrue("expected Invalid, got $error", error is DataError.Invalid)
        assertEquals("projectId.invalid", (error as DataError.Invalid).issues.first().code)
    }

    @Test
    fun `load rejects a path-traversal project id without escaping the root`() = runTest {
        val root = tempRoot()
        // A would-be traversal: the chokepoint must reject it, never resolve outside projects/.
        val error = (repo(root).load("..") as DataResult.Failure).error

        assertTrue("expected Invalid, got $error", error is DataError.Invalid)
    }

    @Test
    fun `load of an absent project returns NotFound`() = runTest {
        val error = (repo(tempRoot()).load("ghost") as DataResult.Failure).error

        assertTrue("expected NotFound, got $error", error is DataError.NotFound)
    }

    // ---------------------------------------------------------------------------------------------
    // SAVE
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `save persists a document that load can read back`() = runTest {
        val root = tempRoot()
        val repo = repo(root)
        val doc = validDoc()

        assertTrue(repo.save("proj1", doc) is DataResult.Success)

        assertTrue(Files.exists(docPath(root, "proj1")))
        assertEquals(doc, (repo.load("proj1") as DataResult.Success).value)
    }

    @Test
    fun `save surfaces a serialization failure as Unknown`() = runTest {
        val root = tempRoot()
        val throwingSerializer = object : DocumentSerializer {
            override val format = PersistedFormat.JSON
            override fun serialize(document: ZineDocument): String = error("cannot serialize")
            override fun deserialize(text: String): ZineDocument = error("unused")
        }

        val error = (repo(root, serializer = throwingSerializer).save("proj1", validDoc()) as DataResult.Failure).error

        assertTrue("expected Unknown, got $error", error is DataError.Unknown)
    }

    @Test
    fun `save surfaces a storage write failure as Io`() = runTest {
        val root = tempRoot()
        val store = AtomicFileStore(object : FileSystemOps by NioFileSystemOps {
            override fun atomicReplace(source: Path, replacing: Path): Unit =
                throw IOException("disk full")
        })

        val error = (repo(root, store).save("proj1", validDoc()) as DataResult.Failure).error

        assertTrue("expected Io, got $error", error is DataError.Io)
    }

    @Test
    fun `save rejects an invalid project id with Invalid`() = runTest {
        val error = (repo(tempRoot()).save("../escape", validDoc()) as DataResult.Failure).error

        assertTrue("expected Invalid, got $error", error is DataError.Invalid)
        assertEquals("projectId.invalid", (error as DataError.Invalid).issues.first().code)
    }

    // ---------------------------------------------------------------------------------------------
    // DURABILITY REGRESSION (ADR-026)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `save commits synchronously on the calling thread (no async dispatch)`() = runTest {
        val root = tempRoot()
        val recording = object : FileSystemOps by NioFileSystemOps {
            var commitThread: Thread? = null
            override fun atomicReplace(source: Path, replacing: Path) {
                commitThread = Thread.currentThread()
                NioFileSystemOps.atomicReplace(source, replacing)
            }
        }
        val callingThread = Thread.currentThread()

        repo(root, AtomicFileStore(recording)).save("proj1", validDoc())

        // A withContext/runInterruptible dispatch would move the commit to another thread; it must not.
        assertEquals(callingThread, recording.commitThread)
    }

    @Test
    fun `the implementation introduces no runInterruptible or async commit (ADR-026)`() {
        // Scan code only — comments may legitimately *name* the forbidden constructs to explain why
        // they are absent, so strip block/line comments before asserting on actual usage.
        val code = sourceFile().toFile().readText()
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .lines().filterNot { it.trim().startsWith("//") }.joinToString("\n")

        assertFalse("must not wrap the commit in runInterruptible", code.contains("runInterruptible"))
        assertFalse("must not move the commit onto a dispatcher", code.contains("withContext"))
        assertFalse("must not reference coroutine dispatchers in the commit path", code.contains("Dispatchers"))
        assertFalse("must not launch the commit asynchronously", code.contains("async("))
    }

    private fun sourceFile(): Path {
        val candidates = listOf(
            "src/main/kotlin/com/aritr/zinely/data/android/DocumentRepositoryImpl.kt",
            "data-android/src/main/kotlin/com/aritr/zinely/data/android/DocumentRepositoryImpl.kt",
        )
        return candidates.map { Path.of(it) }.firstOrNull { Files.exists(it) }
            ?: fail("could not locate DocumentRepositoryImpl.kt from ${Path.of("").toAbsolutePath()}").let { error("unreachable") }
    }
}
