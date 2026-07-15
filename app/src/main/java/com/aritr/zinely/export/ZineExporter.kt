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
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
 * Where a finished export goes (ADR-054 Decision 3/4) — the delivery-agnostic destination the host maps
 * the Proof button onto: [TRANSPORT] = the ephemeral cache file shared via a FileProvider `content://` URI
 * (Share); [DOWNLOADS] = a durable copy the user keeps in the shared Downloads collection (Save PDF).
 */
internal enum class ExportDestination { TRANSPORT, DOWNLOADS }

/**
 * The export use-case seam (ADR-054 Decision 1): renders the live document once and delivers it to
 * [destination], returning the delivery-agnostic [ExportOutcome]. One funnel — the destination selects the
 * sink, never a second render path. The ViewModel depends on this interface (not the concrete exporter) so
 * it is unit-testable with a fake, per the repository-pattern convention.
 */
internal interface SheetExporter {
    suspend fun export(
        document: ZineDocument,
        pageSizePt: PtSize,
        imageBytes: AssetBytesSource,
        format: ExportFormat,
        destination: ExportDestination,
    ): ExportOutcome
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
    private val downloadsWriter: DownloadsWriter,
) : SheetExporter {

    /**
     * Renders [document] once and delivers it to [destination] (ADR-054 Decision 1), off-main on the IO
     * dispatcher. Throws on render/IO failure (including `OutOfMemoryError` from the ~33 MB sheet bitmap —
     * ADR-011); the caller maps it to a friendly export error.
     */
    override suspend fun export(
        document: ZineDocument,
        pageSizePt: PtSize,
        imageBytes: AssetBytesSource,
        format: ExportFormat,
        destination: ExportDestination,
    ): ExportOutcome = withContext(io) {
        when (destination) {
            ExportDestination.TRANSPORT -> writeTransport(document, pageSizePt, imageBytes, format)
            ExportDestination.DOWNLOADS -> writeToDownloads(document, pageSizePt, imageBytes, format)
        }
    }

    /**
     * TRANSPORT (ADR-054 Decision 4): the existing ephemeral cache write (ADR-039) — a uniquely-named file
     * under `cacheDir/exports`, shared as a scoped [FileProvider] `content://` URI. Behaviourally identical
     * to the prior `export()`: this owns and closes its cache stream (ADR-054 Decision 7), and on failure it
     * deletes the just-created (possibly 0-byte) file so a failed export leaves nothing behind.
     */
    private fun writeTransport(
        document: ZineDocument,
        pageSizePt: PtSize,
        imageBytes: AssetBytesSource,
        format: ExportFormat,
    ): ExportReady {
        val dir = File(context.cacheDir, EXPORTS_DIR).apply { mkdirs() }
        val file = File(dir, "zine-${System.currentTimeMillis()}.${format.ext}")
        try {
            FileOutputStream(file).use { out -> writeSheet(out, document, pageSizePt, imageBytes, format) }
        } catch (t: Throwable) {
            file.delete()
            throw t
        }
        // Prune AFTER writing, so exactly the most-recent KEEP_RECENT files survive (the just-written one
        // is newest, so it is never the file pruned) — bounds cache growth without racing the live URI.
        pruneOldExports(dir)
        return ExportReady(
            uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file),
            mime = format.mime,
        )
    }

    /**
     * DOWNLOADS (ADR-054 Decision 4/6): a durable copy the user keeps, in the shared Downloads collection.
     * [DownloadsWriter] owns the destination stream + the API split; [writeSheet] supplies the bytes. Returns
     * the final display name actually written (a legacy collision may have added a `" (N)"` suffix).
     *
     * ponytail: the display base is a neutral default until the real project title (Room metadata, ADR-042)
     * threads through — the same deferral the Proof topbar carries (`zineName = "Your zine"`). The saved name
     * is not surfaced yet (the Fold hand-off is a bare signal); the final "Saved to …" copy is a later batch.
     */
    private fun writeToDownloads(
        document: ZineDocument,
        pageSizePt: PtSize,
        imageBytes: AssetBytesSource,
        format: ExportFormat,
    ): ExportSaved {
        val name = downloadsWriter.write(DEFAULT_SAVE_TITLE, format.ext, format.mime) { out ->
            writeSheet(out, document, pageSizePt, imageBytes, format)
        }
        return ExportSaved(displayName = name, location = DOWNLOADS_LOCATION)
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
        // Neutral durable-save base until the real project title threads through (see writeToDownloads).
        const val DEFAULT_SAVE_TITLE = "zine"
        const val DOWNLOADS_LOCATION = "Downloads"
    }
}

/** Binds [ZineExporter] as the [SheetExporter] the ExportViewModel injects; test doubles implement the interface directly. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class ExportModule {
    @Binds
    abstract fun bindSheetExporter(impl: ZineExporter): SheetExporter
}
