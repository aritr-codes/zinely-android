package com.aritr.zinely.export

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject

/**
 * Writes a durable, user-visible copy of an export into the shared **Downloads** collection — the backend
 * behind "Save PDF = keep a copy" (ADR-054 Decision 6/7). It OWNS the destination stream: it opens and
 * closes its own [OutputStream] (the composer never closes the stream it is handed), while the caller
 * supplies only the byte-producing [body]. It owns no naming policy (that is [ExportNaming]) and no
 * rendering (that is [ZineExporter] / `SheetComposer`).
 *
 * Two platform paths (ADR-054 Decision 6/8):
 *  - **API 29+**: insert into [MediaStore.Downloads] with `IS_PENDING=1`, stream the bytes, then clear
 *    `IS_PENDING`; delete the pending row on failure so a half-file never surfaces. No storage permission.
 *  - **API 24–28**: write a `File` into the public Downloads directory (needs `WRITE_EXTERNAL_STORAGE`,
 *    `maxSdkVersion=28`), resolving name collisions non-destructively via [ExportNaming.nextAvailableName],
 *    then index it with [MediaScannerConnection] so it appears in Files/Downloads.
 *
 * B1 note: this capability is not yet reached from any UI/host path (that is B2). The pure/near-pure
 * helpers below are unit-tested; the API 29+ MediaStore round-trip is device/instrumented-only.
 */
internal class DownloadsWriter @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /**
     * Writes [body]'s bytes to Downloads as a copy named from [title] with extension [ext] and type
     * [mime]. Returns the final display name actually written — the legacy path may have appended a
     * `" (N)"` collision suffix. Throws on IO/MediaStore failure; the API 29+ path removes its pending
     * row first, so Downloads is never left holding a partial file.
     */
    @SuppressLint("NewApi") // requiresLegacyWrite(SDK_INT) is the version guard around the API 29+ branch.
    fun write(title: String, ext: String, mime: String, body: (OutputStream) -> Unit): String =
        if (requiresLegacyWrite(Build.VERSION.SDK_INT)) {
            writeLegacyFile(title, ext, mime, body)
        } else {
            writeMediaStore(ExportNaming.displayName(title, ext), mime, body)
        }

    // ---- API 29+ (MediaStore.Downloads) ----

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeMediaStore(displayName: String, mime: String, body: (OutputStream) -> Unit): String {
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, pendingValues(displayName, mime))
            ?: throw IOException("MediaStore rejected the Downloads insert for \"$displayName\".")
        try {
            resolver.openOutputStream(uri)?.use(body)
                ?: throw IOException("Could not open an output stream for \"$displayName\".")
            resolver.update(uri, clearPendingValues(), null, null)
        } catch (t: Throwable) {
            // Remove the half-written pending row so a partial file never appears in Downloads. This
            // cleanup is best-effort: a delete failure must not mask the original cause, so swallow it
            // and always rethrow [t].
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
        return displayName
    }

    // ---- API 24–28 (public Downloads File + media scan) ----

    @Suppress("DEPRECATION") // getExternalStoragePublicDirectory is the only pre-29 public-Downloads write.
    private fun writeLegacyFile(title: String, ext: String, mime: String, body: (OutputStream) -> Unit): String {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .apply { mkdirs() }
        val name = ExportNaming.nextAvailableName(title, ext) { File(dir, it).exists() }
        val file = File(dir, name)
        FileOutputStream(file).use(body)
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mime), null)
        return name
    }
}

/** MediaStore.Downloads (and `IS_PENDING`) exist only from API 29 (Q); below that we write a File. */
internal fun requiresLegacyWrite(sdkInt: Int): Boolean = sdkInt < Build.VERSION_CODES.Q

/** The pending-insert row for the API 29+ path: a scoped Downloads entry, hidden until the write finishes. */
@RequiresApi(Build.VERSION_CODES.Q)
internal fun pendingValues(displayName: String, mime: String): ContentValues = ContentValues().apply {
    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
    put(MediaStore.Downloads.MIME_TYPE, mime)
    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    put(MediaStore.Downloads.IS_PENDING, 1)
}

/** Clears `IS_PENDING` so the finished file becomes visible in Downloads. */
@RequiresApi(Build.VERSION_CODES.Q)
internal fun clearPendingValues(): ContentValues = ContentValues().apply {
    put(MediaStore.Downloads.IS_PENDING, 0)
}
