package com.aritr.zinely.home

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.imposition.SingleSheet8Imposer
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.data.android.DocumentRepositoryImpl
import com.aritr.zinely.data.android.projectDocumentLayout
import com.aritr.zinely.editor.blankDocument
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

/**
 * The shelf-thumbnail producer's cache/staleness/failure semantics (S6.4, ADR-045), plain JVM:
 * a real [DocumentRepositoryImpl] over a temp dir supplies real documents and mtimes; the raster
 * half is a recording hand fake (`Bitmap` needs Robolectric — pixels are proven by
 * `ThumbnailRendererTest` in `:render-android`'s NATIVE lane). Given-When-Then.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShelfThumbnailProducerTest {

    @get:Rule
    val temp = TemporaryFolder()

    private class FakeImageBitmap : ImageBitmap {
        override val width: Int = 1
        override val height: Int = 1
        override val config: ImageBitmapConfig = ImageBitmapConfig.Argb8888
        override val hasAlpha: Boolean = false
        override val colorSpace: ColorSpace = ColorSpaces.Srgb
        override fun prepareToDraw() = Unit
        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int,
        ) = Unit
    }

    /** Recording raster fake: scriptable render/decode results, PNG bytes are just markers. */
    private class FakeRaster : ThumbnailRaster {
        var renderCalls = 0
        var decodeCalls = 0
        var renderResult: () -> ImageBitmap? = { FakeImageBitmap() }
        var decodeResult: () -> ImageBitmap? = { FakeImageBitmap() }

        override fun renderPng(
            tape: List<DrawCommand>,
            pageSizePt: PtSize,
            out: OutputStream,
        ): ImageBitmap? {
            renderCalls++
            val result = renderResult()
            if (result != null) out.write(byteArrayOf(1, 2, 3))
            return result
        }

        override fun decodePng(file: Path): ImageBitmap? {
            decodeCalls++
            return decodeResult()
        }
    }

    private lateinit var root: Path
    private lateinit var thumbsDir: Path
    private lateinit var documents: DocumentRepository
    private val raster = FakeRaster()

    private fun producer(maxMemoryEntries: Int = MAX_MEMORY_THUMBNAILS): ShelfThumbnailProducer {
        root = if (::root.isInitialized) root else temp.root.toPath()
        thumbsDir = root.resolve("cache-thumbnails")
        documents = if (::documents.isInitialized) {
            documents
        } else {
            DocumentRepositoryImpl(rootDir = root, store = AtomicFileStore())
        }
        return ShelfThumbnailProducer(
            thumbsDir = thumbsDir,
            layout = projectDocumentLayout(root),
            documents = documents,
            imposer = SingleSheet8Imposer(),
            raster = raster,
            io = UnconfinedTestDispatcher(),
            maxMemoryEntries = maxMemoryEntries,
        )
    }

    private suspend fun saveProject(id: String) {
        documents.save(id, blankDocument())
    }

    private fun docFile(id: String): Path = root.resolve("projects").resolve(id).resolve("document.json")

    private fun pngFile(id: String): Path = thumbsDir.resolve("$id.png")

    @Test
    fun `given a readable document, ensure renders once and stamps the png with the document mtime`() =
        runTest {
            // Given a saved project
            val producer = producer()
            saveProject("p1")

            // When
            val bitmap = producer.ensure("p1")

            // Then — rendered, PNG on disk, stamp == the document's mtime (millisecond precision:
            // the producer stats and stamps via toMillis(), consistently on both sides)
            assertNotNull(bitmap)
            assertEquals(1, raster.renderCalls)
            assertTrue(Files.isRegularFile(pngFile("p1")))
            assertEquals(
                Files.getLastModifiedTime(docFile("p1")).toMillis(),
                Files.getLastModifiedTime(pngFile("p1")).toMillis(),
            )
        }

    @Test
    fun `a fresh thumbnail is a memory hit - same instance, no second render, no decode`() = runTest {
        // Given an already-produced thumbnail
        val producer = producer()
        saveProject("p1")
        val first = producer.ensure("p1")

        // When asked again with an unchanged document
        val second = producer.ensure("p1")

        // Then
        assertSame(first, second)
        assertEquals(1, raster.renderCalls)
        assertEquals(0, raster.decodeCalls)
    }

    @Test
    fun `a changed document mtime invalidates the cache and re-renders`() = runTest {
        // Given a produced thumbnail
        val producer = producer()
        saveProject("p1")
        producer.ensure("p1")

        // When the document is written again (autosave) — mtime moves
        Files.setLastModifiedTime(
            docFile("p1"),
            FileTime.fromMillis(Files.getLastModifiedTime(docFile("p1")).toMillis() + 5_000),
        )
        producer.ensure("p1")

        // Then — a second render, and the new stamp matches the new mtime
        assertEquals(2, raster.renderCalls)
        assertEquals(
            Files.getLastModifiedTime(docFile("p1")).toMillis(),
            Files.getLastModifiedTime(pngFile("p1")).toMillis(),
        )
    }

    @Test
    fun `a fresh process with a fresh memory cache serves the disk png without re-rendering`() =
        runTest {
            // Given a PNG produced by an earlier "process"
            val first = producer()
            saveProject("p1")
            first.ensure("p1")

            // When a new producer (empty memory) is asked
            val second = producer()
            val bitmap = second.ensure("p1")

            // Then — decoded from disk, not re-rendered
            assertNotNull(bitmap)
            assertEquals(1, raster.renderCalls)
            assertEquals(1, raster.decodeCalls)
        }

    @Test
    fun `a corrupt cached png does not pin the placeholder - it falls through to a re-render`() =
        runTest {
            // Given a stamped-fresh but undecodable PNG
            val first = producer()
            saveProject("p1")
            first.ensure("p1")
            raster.decodeResult = { null }

            // When a fresh-memory producer must go through the disk
            val bitmap = producer().ensure("p1")

            // Then — decode failed, so it re-rendered instead of returning null forever
            assertNotNull(bitmap)
            assertEquals(2, raster.renderCalls)
        }

    @Test
    fun `an unknown project is null - warm placeholder, no files created`() = runTest {
        val producer = producer()

        assertNull(producer.ensure("nope"))
        assertFalse(Files.exists(pngFile("nope")))
    }

    @Test
    fun `an unsafe id is refused by the layout chokepoint`() = runTest {
        val producer = producer()

        assertNull(producer.ensure("../escape"))
    }

    @Test
    fun `an unreadable document is null and leaves no stale png behind`() = runTest {
        // Given a project whose document is garbage
        val producer = producer()
        saveProject("p1")
        Files.write(docFile("p1"), "not a document".toByteArray())

        // When
        val bitmap = producer.ensure("p1")

        // Then
        assertNull(bitmap)
        assertFalse(Files.exists(pngFile("p1")))
    }

    @Test
    fun `a render failure is null, deletes the partial png, and the next ensure retries`() = runTest {
        // Given a raster that fails once
        val producer = producer()
        saveProject("p1")
        raster.renderResult = { null }

        // When
        val failed = producer.ensure("p1")

        // Then — null, nothing stamped/left behind
        assertNull(failed)
        assertFalse(Files.exists(pngFile("p1")))

        // And when the raster recovers, the same producer retries
        raster.renderResult = { FakeImageBitmap() }
        assertNotNull(producer.ensure("p1"))
        assertEquals(2, raster.renderCalls)
    }

    @Test
    fun `the memory cache is a capped LRU - an evicted entry falls back to the disk png`() = runTest {
        // Given a producer that remembers only one thumbnail (Codex round-2 cap)
        val producer = producer(maxMemoryEntries = 1)
        saveProject("p1")
        saveProject("p2")
        producer.ensure("p1")
        producer.ensure("p2") // evicts p1 from memory

        // When p1 is asked for again
        val bitmap = producer.ensure("p1")

        // Then — served from the still-stamped disk PNG, not a third render
        assertNotNull(bitmap)
        assertEquals(2, raster.renderCalls)
        assertEquals(1, raster.decodeCalls)
    }
}
