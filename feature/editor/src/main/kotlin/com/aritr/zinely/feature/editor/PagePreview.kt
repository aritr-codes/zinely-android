package com.aritr.zinely.feature.editor

import android.content.res.AssetManager
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.aritr.zinely.core.data.repository.PaperColour
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.render.android.BundledFontResolver
import com.aritr.zinely.render.android.CanvasReplayer
import com.aritr.zinely.render.android.ExportScale
import com.aritr.zinely.render.android.FontResolver
import com.aritr.zinely.render.android.ImageBlitter

/** The test tag on the preview Canvas, so a parity test can fetch its actual placed pixel bounds. */
public const val PagePreviewTestTag: String = "page-preview"

/**
 * The S4 Compose **preview host** (ADR-028 §2.4, spike §2.4) — a thin `drawIntoCanvas` bridge that
 * replays the pure [`:core:render`][DrawCommand] tape onto the Compose [nativeCanvas] through the
 * **same** [CanvasReplayer] the export path uses. It carries **no geometry of its own**: every
 * transform comes from [ExportScale.previewPageToDevice], every pixel from [CanvasReplayer.replay].
 * That is what makes `preview == export` structural rather than disciplinary ([ADR-006], [ADR-028]).
 *
 * This host discharges the obligation the S3 spike (§2.4, "Codex Required-fix C") deferred to S4:
 * the raw replayer was proven canvas- and scale-invariant in `:render-android`, but Compose adds its
 * own density/parent-matrix/clip state — so the Compose host owes a real parity proof (driven by
 * `PagePreviewParityTest`, which renders this host and a direct [CanvasReplayer.replay] of the same
 * tape and asserts pixel equality).
 *
 * **Bundled-font wiring (ADR-028 §4.2, hard obligation).** The replayer is built with
 * [BundledFontResolver] over the app/test [AssetManager] — never [FontResolver.Default] — so a family
 * resolves to the same bundled Inter glyphs/metrics in preview, export, and goldens (risk R2). The
 * resolver factory ([previewFontResolver]) is the single seam both this host and the parity test use,
 * so the test verifies the *actual* wiring, not a copy of it.
 *
 * **Image bytes seam ([imageBytes]).** The replayer is wired with an [ImageBlitter] over an
 * [AssetBytesSource], so a [com.aritr.zinely.core.render.DrawImage] tape never crashes the host
 * (without a blitter the replayer throws on the first image command). The **default** source returns
 * `null` for every asset → the defined missing-asset placeholder (exactly what `:render-android`'s
 * `RasterGoldenTest` exercises). The **real** image-master source (the project/document asset store,
 * [ADR-023]) is injected by a later S4 step; this preview-host step does not depend on persistence.
 *
 * **Caller-sizing contract (intentional, for the hoisted-state host).** The host sizes its [Canvas]
 * to the incoming [modifier]; the caller's [modifier] MUST therefore carry the page size (e.g.
 * `Modifier.size(wPx.dp, hPx.dp)`, or an `aspectRatio` inside a sized parent). The host deliberately
 * adds no size of its own so placement/zoom stay hoisted state owned by the caller.
 *
 * Stateless: all inputs are hoisted; the composable owns no mutable state. MVI store, gestures, and
 * undo are **not** here — they land in the following S4 steps.
 *
 * @param tape the back-to-front, z-sorted draw-command tape for one page ([`:core:render`][DrawCommand]).
 * @param sheet the page size in PostScript points; also the page clip ([PtRect] `0,0 → w,h`).
 * @param screenPxPerPt device pixels per point at the current preview scale (density × zoom).
 * @param pageOffset page-space offset applied before the screen scale (pan); default origin.
 * @param modifier sizing/placement applied by the caller; the host sizes the [Canvas] to it (see the
 *   caller-sizing contract above).
 * @param imageBytes source of canonical import-master bytes for [com.aritr.zinely.core.render.DrawImage]
 *   commands; the default `null`-source renders the missing-asset placeholder (see the image seam above).
 * @param paper the paper the zine is loaded on; anything but White is multiplied over the page after the replay
 *   ([drawPaperLayer]). Only the surfaces AW6.7 names pass it; the default draws nothing extra.
 * @param paperSheet the shape of a sheet the host draws under this canvas, or `null`. Non-null means the host lays the
 *   white ground itself ([groundOr]) and the paper layer is clipped to this shape, so the sheet keeps its edge and
 *   corners; `null` means this canvas lays a rectangular white ground under a coloured page.
 * @param imageCacheBytes optional decoded-region LRU budget. Zero keeps one-draw ownership for proof,
 *   thumbnails, goldens, and export parity callers; the interactive editor opts in explicitly.
 */
@Composable
public fun PagePreview(
    tape: List<DrawCommand>,
    sheet: PtSize,
    screenPxPerPt: Float,
    pageOffset: PtPoint = PtPoint(0.0, 0.0),
    modifier: Modifier = Modifier,
    imageBytes: AssetBytesSource = EmptyAssetBytes,
    imageCacheBytes: Long = 0L,
    paper: PaperColour = PaperColour.WHITE,
    paperSheet: Shape? = null,
) {
    val assets = LocalContext.current.assets
    // One replayer per (asset source, image source), rebuilt only if either identity changes — so the
    // default [imageBytes] is the stable [EmptyAssetBytes] singleton, NOT a fresh `AssetBytesSource { null }`
    // in the default-arg expression (which, absent SAM caching, would change identity each recomposition
    // and rebuild the replayer + reload the bundled fonts every frame). The resolver is the §4.2-obligated
    // BundledFontResolver (via the shared previewFontResolver seam); the blitter is wired so a DrawImage
    // tape paints the placeholder instead of throwing.
    val imageBlitter = remember(imageBytes, imageCacheBytes) { ImageBlitter(imageBytes, imageCacheBytes) }
    DisposableEffect(imageBlitter) {
        onDispose { imageBlitter.close() }
    }
    val replayer = remember(assets, imageBlitter) {
        CanvasReplayer(
            fontResolver = previewFontResolver(assets),
            imageBlitter = imageBlitter,
        )
    }

    Canvas(modifier = modifier.testTag(PagePreviewTestTag)) {
        val pageToDevice = ExportScale.previewPageToDevice(screenPxPerPt.toDouble(), pageOffset)
        val pageRect = if (paper.overlay == null) {
            null
        } else {
            val topLeft = pageToDevice.map(PtPoint(0.0, 0.0))
            val bottomRight = pageToDevice.map(PtPoint(sheet.width, sheet.height))
            Rect(topLeft.x.toFloat(), topLeft.y.toFloat(), bottomRight.x.toFloat(), bottomRight.y.toFloat())
        }
        // AW6.8: "White x paper = the paper". The replayer leaves the page's ground to the host, which paints the
        // app's warm page paper; under a coloured paper the ground is the export's opaque white instead, so the
        // multiply below gives the paper itself, not the paper over cream. White zines skip this entirely.
        // A host that draws its own sheet under the canvas ([paperSheet], the Bench) lays that ground itself, inside its
        // edge: a rectangle here would paint out the sheet's ink edge and square off its rounded corners.
        if (pageRect != null && paperSheet == null) drawRect(Color.White, topLeft = pageRect.topLeft, size = pageRect.size)
        drawIntoCanvas { canvas ->
            replayer.replay(
                canvas = canvas.nativeCanvas,
                tape = tape,
                pageToDevice = pageToDevice,
                pageClip = PtRect(0.0, 0.0, sheet.width, sheet.height),
                decodePxPerPt = screenPxPerPt.toDouble(),
            )
        }
        // AW6.8 / ADR-150 decision 5: the paper, multiplied once over the finished page, before any chrome (every
        // chrome layer is a later sibling). The replayer and the export never see it.
        if (pageRect != null) drawPaperLayer(paper, pageRect, paperSheet ?: RectangleShape)
    }
}

/**
 * The single bundled-font resolver seam for the preview host (ADR-028 §4.2). [PagePreview] builds its
 * [CanvasReplayer] from this, and `PagePreviewParityTest` asserts on it, so the wiring obligation —
 * inject [BundledFontResolver], never [FontResolver.Default] — is verified against the real factory
 * the host uses, not a re-implementation. `internal` so it never widens the module's public API.
 */
internal fun previewFontResolver(assets: AssetManager): FontResolver = BundledFontResolver(assets)

/**
 * The stable default [PagePreview.imageBytes] source — a single process-wide [AssetBytesSource] that
 * returns `null` for every asset (→ the missing-asset placeholder). Hoisted out of the default-argument
 * expression so [PagePreview]'s `remember(assets, imageBytes)` key stays stable across recompositions
 * when the caller omits `imageBytes`; a `AssetBytesSource { null }` written inline in the default would
 * (absent SAM caching) allocate a new identity each recomposition and needlessly rebuild the
 * [CanvasReplayer] + reload the bundled fonts every frame.
 */
internal val EmptyAssetBytes: AssetBytesSource = AssetBytesSource { null }
