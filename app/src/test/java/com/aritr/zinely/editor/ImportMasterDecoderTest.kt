package com.aritr.zinely.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * S7.0 regression suite for [ImportMasterDecoder] — the ADR-031 §4 device-smoke gap, closed with
 * Robolectric NATIVE (real Skia `BitmapFactory`, real PNG bytes) plus a shadow `ContentResolver`
 * serving a **fresh stream per open** exactly like a real provider (the decoder opens the Uri three
 * times: bounds, sampled decode, EXIF). Given-When-Then.
 *
 * The first test is the S7.0 on-device import failure reproduced headlessly: `readBounds` used to
 * return the `BitmapFactory.decodeStream` result of an `inJustDecodeBounds` pass — which is null *by
 * contract* — so every import failed with "That image couldn't be added." on hardware.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImportMasterDecoderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun decoder() = ImportMasterDecoder(context.contentResolver)

    /** A real PNG (Skia-encoded under NATIVE graphics) served as a fresh stream per open. */
    private fun registerPng(widthPx: Int, heightPx: Int): Uri {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            .toByteArray()
        bitmap.recycle()
        val uri = Uri.parse("content://media/picker/0/com.test.provider/media/1")
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) { ByteArrayInputStream(bytes) }
        return uri
    }

    @Test
    fun `decodeToMaster produces a master with the source dimensions for a valid image`() {
        // Given a valid 120×80 PNG behind a content Uri
        val uri = registerPng(widthPx = 120, heightPx = 80)

        // When the import-master is decoded
        val master = decoder().decodeToMaster(uri)

        // Then it succeeds with the source's pixel dimensions (no orientation/cap applies here)
        assertNotNull("a valid image must decode to a master (S7.0 regression)", master)
        assertEquals(120, master!!.widthPx)
        assertEquals(80, master.heightPx)
        assertNotNull(master.bytes)
    }

    @Test
    fun `decodeToMaster consumes an EXIF ROTATE_90 orientation into the pixels`() {
        // Given a 120×80 JPEG whose EXIF says ROTATE_90 (as a camera would tag a portrait shot)
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val file = File.createTempFile("exif", ".jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        bitmap.recycle()
        ExifInterface(file.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        val bytes = file.readBytes().also { file.delete() }
        val uri = Uri.parse("content://media/picker/0/com.test.provider/media/2")
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) { ByteArrayInputStream(bytes) }

        // When the import-master is decoded
        val master = decoder().decodeToMaster(uri)

        // Then the orientation is consumed — the master's pixels are upright (dimensions swapped)
        assertNotNull(master)
        assertEquals(80, master!!.widthPx)
        assertEquals(120, master.heightPx)
    }

    @Test
    fun `decodeToMaster returns null for a stream of undecodable bytes`() {
        // Given a Uri that opens fine but serves bytes no image codec accepts
        val garbage = ByteArray(256) { it.toByte() }
        val uri = Uri.parse("content://media/picker/0/com.test.provider/media/3")
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) { ByteArrayInputStream(garbage) }

        // When the import-master is decoded
        val master = decoder().decodeToMaster(uri)

        // Then bounds stay unreadable and the decoder reports failure (null), never throws
        assertNull(master)
    }

    @Test
    fun `decodeToMaster returns null for an unopenable uri`() {
        // Given a Uri no provider serves
        val uri = Uri.parse("content://media/picker/0/com.test.provider/media/404")

        // When the import-master is decoded
        val master = decoder().decodeToMaster(uri)

        // Then the decoder reports failure (null), never throws
        assertNull(master)
    }
}
