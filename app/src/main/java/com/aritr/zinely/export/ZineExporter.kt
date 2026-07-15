package com.aritr.zinely.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.aritr.zinely.core.imposition.SingleSheet8Imposer
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.render.SceneRenderer
import com.aritr.zinely.data.android.di.IoDispatcher
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.render.android.BundledFontResolver
import com.aritr.zinely.render.android.CanvasReplayer
import com.aritr.zinely.render.android.ImageBlitter
import com.aritr.zinely.render.android.SheetComposer
import com.aritr.zinely.render.android.SheetPanel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject

/** The two home-print export targets (ADR-039). [ext]/[mime] drive the cache filename + the share intent. */
internal enum class ExportFormat(val ext: String, val mime: String) {
    PDF(ext = "pdf", mime = "application/pdf"),
    PNG(ext = "png", mime = "image/png"),
}

/**
 * The export product path (ADR-039 §3): imposes the document, renders each booklet page to a draw tape,
 * composites all panels onto one sheet via [SheetComposer], writes the bytes to a uniquely-named cache
 * file, and returns a shareable [FileProvider] `content://` URI. All off the main thread on the injected
 * IO dispatcher ([ADR-011]).
 *
 * Reuses the *shared* render seams — the same [CanvasReplayer] (with the mandatory [BundledFontResolver]
 * so text matches preview, and an [ImageBlitter] over the editor's [AssetBytesSource]) the preview host
 * uses — so `export == preview` (ADR-006). Nothing here reaches the network or a second document copy;
 * the export is a local file write plus a scoped read grant.
 */
internal class ZineExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {

    /**
     * Renders [document] to a [format] file and returns its shareable URI. Throws on render/IO failure
     * (including `OutOfMemoryError` from the ~33 MB sheet bitmap — ADR-011); the caller maps it to a
     * friendly export error.
     */
    suspend fun export(
        document: ZineDocument,
        pageSizePt: PtSize,
        imageBytes: AssetBytesSource,
        format: ExportFormat,
    ): Uri = withContext(io) {
        val dir = File(context.cacheDir, EXPORTS_DIR).apply { mkdirs() }
        val file = File(dir, "zine-${System.currentTimeMillis()}.${format.ext}")
        // export() OWNS this transport stream and closes it (the stream-ownership invariant, ADR-054
        // Decision 7); writeSheet only writes into it. On any render/IO failure, delete the just-created
        // (possibly 0-byte) cache file before rethrowing, so a failed export leaves nothing behind —
        // identical to the pre-writeSheet-extraction behaviour, where the file was opened only after a
        // successful compose.
        try {
            FileOutputStream(file).use { out -> writeSheet(out, document, pageSizePt, imageBytes, format) }
        } catch (t: Throwable) {
            file.delete()
            throw t
        }
        // Prune AFTER writing, so exactly the most-recent KEEP_RECENT files survive (the just-written one
        // is newest, so it is never the file pruned) — bounds cache growth without racing the live URI.
        pruneOldExports(dir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * The shared compose→stream path (ADR-054 Decision 1): imposes [document], renders each booklet page
     * to a tape, and composites all panels onto one sheet written into [out]. Stream-ownership invariant
     * — this NEVER closes [out]; the caller owns and closes the stream it supplies (the cache
     * `FileOutputStream` here; the Downloads stream in a later batch). Keeping the render ceremony in one
     * place means no destination duplicates it.
     */
    private fun writeSheet(
        out: OutputStream,
        document: ZineDocument,
        pageSizePt: PtSize,
        imageBytes: AssetBytesSource,
        format: ExportFormat,
    ) {
        val replayer = CanvasReplayer(
            fontResolver = BundledFontResolver(context.assets),
            imageBlitter = ImageBlitter(imageBytes),
        )
        val composer = SheetComposer(replayer)
        val layout = SingleSheet8Imposer().layout(document.format, document.paperSize)
        val overlay = SheetGuides.overlay(layout)

        val panels = layout.panels.map { panel ->
            // bookletPage is 1-based; Page.index is 0-based reading order. A missing page renders blank
            // rather than throwing — the seed document is complete, but export must not crash on a gap.
            val page = document.pages.find { it.index == panel.bookletPage - 1 }
            val tape = if (page != null) SceneRenderer.render(page, pageSizePt, document.defaults) else emptyList()
            SheetPanel(contentToSheet = panel.contentToSheet, clip = panel.clipLocalBounds, tape = tape)
        }

        when (format) {
            ExportFormat.PDF -> composer.writePdf(layout.sheet, panels, overlay, out)
            ExportFormat.PNG -> composer.writePng(layout.sheet, panels, overlay, out)
        }
    }

    /**
     * Keeps only the most recent [keep] exports (ADR-039 §4). Runs *after* writing the new (uniquely
     * named) file — which, being newest, always survives — so the cache is bounded to exactly [keep]
     * files and the freshly-shared `content://` URI is never the one deleted.
     */
    private fun pruneOldExports(dir: File, keep: Int = KEEP_RECENT) {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        files.drop(keep).forEach { it.delete() }
    }

    private companion object {
        const val EXPORTS_DIR = "exports"
        const val KEEP_RECENT = 6
    }
}
