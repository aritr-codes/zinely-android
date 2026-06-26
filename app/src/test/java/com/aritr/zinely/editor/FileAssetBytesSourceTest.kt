package com.aritr.zinely.editor

import com.aritr.zinely.core.data.asset.ContentHasher
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for the synchronous render read-path [FileAssetBytesSource] (ADR-031 §3). Pure java.io —
 * a real temp assets dir, no Android. Given-When-Then.
 */
class FileAssetBytesSourceTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val bytes = "master".toByteArray()
    private val validHash = ContentHasher.sha256(bytes).hex

    private fun assetsDir(): File = tmp.newFolder("assets")

    @Test
    fun `a non-hex assetId returns null without touching the filesystem`() {
        val source = FileAssetBytesSource(assetsDir())
        assertNull(source.open("not-a-hash"))
        assertNull(source.open("UPPERCASE0123"))
    }

    @Test
    fun `a path-traversal assetId is rejected by the hex guard`() {
        val source = FileAssetBytesSource(assetsDir())
        assertNull(source.open("../secret"))
        assertNull(source.open("../../etc/passwd"))
    }

    @Test
    fun `a valid hash with no file returns null (missing ⇒ placeholder)`() {
        val source = FileAssetBytesSource(assetsDir())
        assertNull(source.open(validHash))
    }

    @Test
    fun `an existing blob opens fresh independent streams at byte 0 each call`() {
        val dir = assetsDir()
        File(dir, validHash).writeBytes(bytes)
        val source = FileAssetBytesSource(dir)

        val first = source.open(validHash)
        val second = source.open(validHash)

        assertNotNull(first)
        assertNotNull(second)
        // Two independent streams — both read the full bytes from the start (the ImageBlitter 2-open contract).
        first!!.use { assertArrayEquals(bytes, it.readBytes()) }
        second!!.use { assertArrayEquals(bytes, it.readBytes()) }
    }

    @Test
    fun `a directory at the blob path returns null, not a stream`() {
        val dir = assetsDir()
        File(dir, validHash).mkdirs()
        val source = FileAssetBytesSource(dir)

        assertNull(source.open(validHash))
    }
}
