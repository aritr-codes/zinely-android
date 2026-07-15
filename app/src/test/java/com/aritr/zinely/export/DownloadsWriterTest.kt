package com.aritr.zinely.export

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Checks for [DownloadsWriter]'s testable seams (ADR-054 Decision 6/8). The pure API-branch predicate and
 * the MediaStore `ContentValues` builders are asserted directly; the legacy (API ≤28) File path is driven
 * end-to-end under Robolectric's sandboxed external storage. The API 29+ MediaStore round-trip itself is
 * device/instrumented-only and is not exercised here.
 */
@RunWith(RobolectricTestRunner::class)
class DownloadsWriterTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // ---- API branch selection (pure) ----

    @Test
    fun requiresLegacyWrite_isTrueBelow29_falseFrom29() {
        assertTrue(requiresLegacyWrite(24))
        assertTrue(requiresLegacyWrite(28))
        assertFalse(requiresLegacyWrite(29))
        assertFalse(requiresLegacyWrite(34))
    }

    // ---- MediaStore ContentValues generation + pending lifecycle ----

    @Test
    @Config(sdk = [29])
    fun pendingValues_areScopedToDownloadsAndMarkedPending() {
        val values = pendingValues("My Zine.pdf", "application/pdf")
        assertEquals("My Zine.pdf", values.getAsString(MediaStore.Downloads.DISPLAY_NAME))
        assertEquals("application/pdf", values.getAsString(MediaStore.Downloads.MIME_TYPE))
        assertEquals(Environment.DIRECTORY_DOWNLOADS, values.getAsString(MediaStore.Downloads.RELATIVE_PATH))
        assertEquals(1, values.getAsInteger(MediaStore.Downloads.IS_PENDING))
    }

    @Test
    @Config(sdk = [29])
    fun clearPendingValues_flipsIsPendingToZero() {
        assertEquals(0, clearPendingValues().getAsInteger(MediaStore.Downloads.IS_PENDING))
    }

    // ---- Legacy (API ≤28) File path, end-to-end ----

    @Test
    @Config(sdk = [26])
    fun legacyWrite_writesFileToPublicDownloadsWithSanitisedName() {
        val name = DownloadsWriter(context).write("My Zine", "pdf", "application/pdf") { out ->
            out.write("hello".toByteArray())
        }
        assertEquals("My Zine.pdf", name)
        @Suppress("DEPRECATION")
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, "My Zine.pdf")
        assertTrue("file written to Downloads", file.exists())
        assertEquals("hello", file.readText())
    }

    @Test
    @Config(sdk = [26])
    fun legacyWrite_secondSaveGetsNonDestructiveCollisionSuffix() {
        val writer = DownloadsWriter(context)
        val first = writer.write("My Zine", "pdf", "application/pdf") { it.write("one".toByteArray()) }
        val second = writer.write("My Zine", "pdf", "application/pdf") { it.write("two".toByteArray()) }
        assertEquals("My Zine.pdf", first)
        assertEquals("My Zine (1).pdf", second)
        @Suppress("DEPRECATION")
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        // Non-destructive: the first copy is untouched.
        assertEquals("one", File(dir, "My Zine.pdf").readText())
        assertEquals("two", File(dir, "My Zine (1).pdf").readText())
    }
}
