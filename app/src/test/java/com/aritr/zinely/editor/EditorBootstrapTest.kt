package com.aritr.zinely.editor

import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.imposition.SingleSheet8Imposer
import com.aritr.zinely.core.model.ZineDocument
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure editor bootstrap (ADR-030 §4/§5). Given-When-Then; a hand fake stands in for
 * the repository so the seed-on-miss decision and the imposition-derived page size are verified with no
 * Android / Hilt.
 */
class EditorBootstrapTest {

    /** Records calls and replays a scripted load result; captures the saved document for assertions. */
    private class FakeRepository(
        private val loadResult: DataResult<ZineDocument>,
        private val saveResult: DataResult<Unit> = DataResult.Success(Unit),
    ) : DocumentRepository {
        var loadCount = 0
        var savedProjectId: String? = null
        var savedDocument: ZineDocument? = null

        override suspend fun load(projectId: String): DataResult<ZineDocument> {
            loadCount++
            return loadResult
        }

        override suspend fun save(projectId: String, document: ZineDocument): DataResult<Unit> {
            savedProjectId = projectId
            savedDocument = document
            return saveResult
        }
    }

    @Test
    fun `load success returns the stored document and never seeds`() = runTest {
        val stored = blankDocument().copy(schemaVersion = 1)
        val repo = FakeRepository(loadResult = DataResult.Success(stored))

        val result = bootstrapDocument(repo, "default")

        assertSame(stored, (result as DataResult.Success).value)
        assertNull("must not save on a successful load", repo.savedDocument)
    }

    @Test
    fun `NotFound seeds a blank document, persists it, and returns the seed`() = runTest {
        val repo = FakeRepository(loadResult = DataResult.Failure(DataError.NotFound("default")))

        val result = bootstrapDocument(repo, "default")

        val seeded = (result as DataResult.Success).value
        assertEquals(8, seeded.pages.size)
        assertSame("returned doc must be the one persisted", seeded, repo.savedDocument)
        assertEquals("default", repo.savedProjectId)
    }

    @Test
    fun `NotFound but save fails propagates the save failure`() = runTest {
        val ioError = DataError.Io("disk full")
        val repo = FakeRepository(
            loadResult = DataResult.Failure(DataError.NotFound("default")),
            saveResult = DataResult.Failure(ioError),
        )

        val result = bootstrapDocument(repo, "default")

        assertEquals(ioError, (result as DataResult.Failure).error)
    }

    @Test
    fun `a non-NotFound load failure propagates and never overwrites with a blank`() = runTest {
        val corrupt = DataError.Corrupt("bad bytes")
        val repo = FakeRepository(loadResult = DataResult.Failure(corrupt))

        val result = bootstrapDocument(repo, "default")

        assertEquals(corrupt, (result as DataResult.Failure).error)
        assertNull("a recoverable Corrupt doc must never be clobbered by a seed", repo.savedDocument)
    }

    @Test
    fun `editedPageSize comes from imposition, identical for every uniform panel`() {
        val doc = blankDocument()
        val imposer = SingleSheet8Imposer()

        val size = editedPageSize(doc, imposer)

        assertTrue("panel width must be positive", size.width > 0.0)
        assertTrue("panel height must be positive", size.height > 0.0)
        // Single source of truth: it equals panel 0's local bounds, the imposer's own number.
        val panel0 = imposer.layout(doc.format, doc.paperSize).panels.first().panelLocalBounds
        assertEquals(panel0.width, size.width, 1e-9)
        assertEquals(panel0.height, size.height, 1e-9)
    }
}
