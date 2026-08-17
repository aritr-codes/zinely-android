package com.aritr.zinely.render.android

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.core.render.DrawImage
import com.aritr.zinely.core.render.DrawShape
import com.aritr.zinely.core.render.DrawTextBox
import com.aritr.zinely.core.render.FillRect
import com.aritr.zinely.core.render.Segment
import com.aritr.zinely.core.render.SupplyOutline
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Replays the pure [`:core:render`][DrawCommand] tape (ADR-027) onto a raw [android.graphics.Canvas]
 * — the single draw path shared by every backend (ADR-028 "one replayer, two canvas providers").
 * Preview, raster export, and PDF export differ only in which [Canvas] and [pageToDevice]/[decodePxPerPt]
 * they pass in; the geometry here is identical, which is what makes `preview == export` structural
 * rather than disciplinary (ADR-006).
 *
 * Collaborators are injected so every provider shares one configuration: [fontResolver] for text
 * ([SharedTextLayout]) and [imageBlitter] for images ([ImageBlitter], over an `AssetBytesSource`).
 * A single instance is reused across pages.
 *
 * **Replay quad (ADR-028 clause 2, spike §3.1).** Page scope is set once —
 * `save → concat(pageToDevice) → clip(pageClip)` — then each command replays self-contained:
 * `save → concat(localToPage) → clip(localClip) → draw → restore`. The clip is applied **after** the
 * local transform so a rotated element clips in its own frame. The composed CTM for a command is
 * `pageToDevice × localToPage` (column-vector [AffineTransform2D], `other` applied first).
 *
 * **Coordinate model (ADR-028 clause 3).** [pageToDevice] is the *visual* page→device transform and
 * differs by target — points→pixels (`×300/72`) for raster, points→points for the PDF canvas, screen
 * `px/pt` for preview. [decodePxPerPt] is the **separate** image-decode resolution, never inferred from
 * [pageToDevice]; it is unused until image replay lands (G4).
 *
 * Scope: [FillRect], [DrawTextBox], [DrawShape], and [DrawImage] (the last only when an [imageBlitter]
 * is wired — it needs an `AssetBytesSource`, supplied in app/export wiring).
 */
public class CanvasReplayer(
    private val fontResolver: FontResolver = FontResolver.Default,
    private val imageBlitter: ImageBlitter? = null,
) {

    /** Pinned fill paint: solid, anti-alias off so geometric fills diff at zero tolerance (spike §4.1). */
    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
        isDither = false
    }

    /**
     * [DrawShape]'s own paint — deliberately **not** [fillPaint] (SUPPLIES-SPEC §3.5).
     *
     * Anti-aliasing is on because a supply is a torn, stamped or curved outline and reads as jagged
     * without it; the pinned `fillPaint` above must keep AA *off* so axis-aligned fills stay diffable
     * at zero tolerance, and mutating it would have moved every committed FillRect golden. The two
     * paints are the whole reason that trade-off costs nothing. Print is unaffected either way: the
     * PDF backend ignores the AA flag entirely (§3.2).
     */
    private val shapePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        isDither = false
    }

    public fun replay(
        canvas: Canvas,
        tape: List<DrawCommand>,
        pageToDevice: AffineTransform2D,
        pageClip: PtRect,
        decodePxPerPt: Double,
    ): Unit {
        // try/finally so a throwing draw (e.g. a not-yet-implemented command, or a real decode error
        // in a later gate) still unwinds the canvas to its entry state rather than leaking save scopes.
        val pageScope = canvas.save()
        try {
            canvas.concat(pageToDevice.toMatrix())
            canvas.clipRect(pageClip)

            for (command in tape) {
                val commandScope = canvas.save()
                try {
                    canvas.concat(command.localToPage.toMatrix())
                    command.localClip?.let { canvas.clipRect(it) }
                    draw(canvas, command, decodePxPerPt)
                } finally {
                    canvas.restoreToCount(commandScope)
                }
            }
        } finally {
            canvas.restoreToCount(pageScope)
        }
    }

    private fun draw(canvas: Canvas, command: DrawCommand, decodePxPerPt: Double) {
        when (command) {
            is FillRect -> {
                fillPaint.color = command.color.toArgb()
                val r = command.rect
                canvas.drawRect(
                    r.x.toFloat(), r.y.toFloat(), r.right.toFloat(), r.bottom.toFloat(), fillPaint,
                )
            }
            is DrawTextBox -> drawText(canvas, command)
            is DrawImage -> {
                val blitter = checkNotNull(imageBlitter) {
                    "CanvasReplayer needs an ImageBlitter (AssetBytesSource) to replay DrawImage"
                }
                // decodePxPerPt is the page→device density; localScale is the element's own scale within
                // page space, so the decode footprint = destRect × decodePxPerPt × localScale (§5.1).
                blitter.draw(canvas, command, decodePxPerPt, command.localToPage.uniformScale())
            }
            // P3 — the supply reaches paper, on all four surfaces at once. Four load-bearing details:
            //
            //   1. [shapePaint], never [fillPaint]. AA is on here because a torn edge drawn without it
            //      is visibly jagged; mutating the shared paint would have moved every committed
            //      FillRect golden instead. SkPDF ignores the AA flag, so print is unaffected (§3.5).
            //   2. Even-odd lives on `Path.fillType` — see [toPath]. The Paint has no such field.
            //   3. No MaskFilter, no PathEffect, and no perspective row can reach the CTM (an
            //      [AffineTransform2D] has no third row) — each of those makes SkPDF silently
            //      rasterise instead of emitting vector operators (§3.2 constraint 5).
            //   4. The outline is in **unit** space, not points: `command.localToPage` carries the
            //      element's size through the §3.4.1 scale fold, and it is non-uniform in general —
            //      which is why [uniformScale] is not consulted here. It is an image-decode heuristic,
            //      not a geometry term.
            is DrawShape -> {
                shapePaint.color = command.ink.toArgb()
                canvas.drawPath(command.outline.toPath(), shapePaint)
            }
        }
    }

    /**
     * Lays text out in point-space layout units ([SharedTextLayout.LAYOUT_SCALE]) and draws it at a
     * pre-concatenated `scale(1/K)`, so the device matrix is unchanged and wrapping is resolution-
     * independent (§4). The local clip (= text box, in points) was applied **before** this scale, so it
     * still bounds the text in point space. `StaticLayout.draw` keeps PDF text vector (§4.3) — never a
     * `drawBitmap`. The text origin is the box's local `(0,0)` (the command carries no offset).
     */
    private fun drawText(canvas: Canvas, command: DrawTextBox) {
        val layout = SharedTextLayout.build(command.text, command.style, command.boxWidthPt, fontResolver)
        val inverseScale = 1f / SharedTextLayout.LAYOUT_SCALE
        val textScope = canvas.save()
        try {
            canvas.scale(inverseScale, inverseScale)
            layout.draw(canvas)
        } finally {
            canvas.restoreToCount(textScope)
        }
    }
}

/**
 * Converts an [AffineTransform2D] `(a, b, c, d, e, f)` to an [android.graphics.Matrix]. Both use the
 * column-vector convention `x' = a·x + c·y + e`, `y' = b·x + d·y + f`, so the values map directly onto
 * the Matrix's row-major `[MSCALE_X, MSKEW_X, MTRANS_X, MSKEW_Y, MSCALE_Y, MTRANS_Y, 0, 0, 1]`.
 */
private fun AffineTransform2D.toMatrix(): Matrix = Matrix().apply {
    setValues(
        floatArrayOf(
            a.toFloat(), c.toFloat(), e.toFloat(),
            b.toFloat(), d.toFloat(), f.toFloat(),
            0f, 0f, 1f,
        ),
    )
}

/**
 * The pure [SupplyOutline] → [android.graphics.Path] conversion — the whole platform seam for supplies,
 * kept a free function so it is exercisable without a [Canvas], a replayer or a bitmap.
 *
 * **`fillType = EVEN_ODD` is the load-bearing line in this file.** It lives on the Path because the
 * Paint has no such field, and forgetting it is the one defect surface-parity testing is structurally
 * blind to: a non-zero fill closes every hole *identically* on preview, PNG, PDF and the imposed sheet,
 * so the four surfaces would agree perfectly on the wrong picture. Only a ring whose contours wind the
 * **same** direction renders differently under the two rules, which is why that geometry — not a
 * golden — is the proof (`ShapeReplayTest`, and `SupplyOutlineRingTest` for the representation).
 *
 * Coordinates stay in the authored **unit square**; the size arrives through the command's
 * `localToPage` (SUPPLIES-SPEC §3.4.1), already concatenated onto the canvas by the replay quad.
 * [close] per subpath makes the implicit closure explicit — a fill would close it anyway, and saying so
 * keeps the emitted geometry identical to what the outline documents.
 */
internal fun SupplyOutline.toPath(): Path = Path().apply {
    fillType = Path.FillType.EVEN_ODD
    for (subpath in subpaths) {
        moveTo(subpath.start.x.toFloat(), subpath.start.y.toFloat())
        for (segment in subpath.segments) when (segment) {
            is Segment.LineTo -> lineTo(segment.to.x.toFloat(), segment.to.y.toFloat())
            is Segment.CubicTo -> cubicTo(
                segment.c1.x.toFloat(), segment.c1.y.toFloat(),
                segment.c2.x.toFloat(), segment.c2.y.toFloat(),
                segment.to.x.toFloat(), segment.to.y.toFloat(),
            )
        }
        close()
    }
}

/** Clips to [rect] in the current (already-concatenated) local space. */
private fun Canvas.clipRect(rect: PtRect): Boolean =
    clipRect(rect.x.toFloat(), rect.y.toFloat(), rect.right.toFloat(), rect.bottom.toFloat())

/** Packs a [ColorRgba] (straight, 8-bit) into an Android ARGB int. */
internal fun ColorRgba.toArgb(): Int =
    (a and 0xFF shl 24) or (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF)

/**
 * The element-local → page-local **linear (uniform) scale** = `sqrt(|det|)` of the transform's linear
 * part. For the MVP element transform (rotation + translation, [ADR-027]) this is `1`; for a uniform
 * scale `s` it is `s`. Used to size the image decode footprint (§5.1).
 */
internal fun AffineTransform2D.uniformScale(): Double = sqrt(abs(a * d - b * c))
