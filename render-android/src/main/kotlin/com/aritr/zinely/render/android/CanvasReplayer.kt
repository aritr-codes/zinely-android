package com.aritr.zinely.render.android

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.core.render.DrawImage
import com.aritr.zinely.core.render.DrawTextBox
import com.aritr.zinely.core.render.FillRect
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
 * Scope: [FillRect], [DrawTextBox], and [DrawImage] (the latter only when an [imageBlitter] is wired —
 * it needs an `AssetBytesSource`, supplied in app/export wiring).
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
