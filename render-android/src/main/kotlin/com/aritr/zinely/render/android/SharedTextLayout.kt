package com.aritr.zinely.render.android

import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.aritr.zinely.core.model.TextAlign
import com.aritr.zinely.core.model.TextStyle
import kotlin.math.roundToInt

/**
 * The single, shared text-layout path (ADR-028 §4, ADR-006, risk #3) — used **identically** by every
 * canvas provider so preview and export break lines the same way. Layout happens in **point space at a
 * fixed internal scale [LAYOUT_SCALE]**; [CanvasReplayer] pre-concats `scale(1/LAYOUT_SCALE)` so the
 * device matrix is unchanged. All [StaticLayout] knobs are fixed shared constants (§4.1) — only the
 * alignment is sourced from the model. PDF keeps this vector via `StaticLayout.draw` (§4.3).
 */
public object SharedTextLayout {

    /**
     * Fixed internal point→layout-unit scale (Required-fix F, §4). Lay out at `sizePt × K` and
     * `round(boxWidthPt × K)`; the replayer divides by `K`. Shrinks point-quantisation 8× and is
     * identical across preview and export because both divide by the same constant.
     */
    public const val LAYOUT_SCALE: Int = 8

    /**
     * Builds the [StaticLayout] for a `DrawTextBox`, in layout units (points × [LAYOUT_SCALE]). Knobs
     * are pinned: simple greedy break, no hyphenation, FIRSTSTRONG_LTR, no include-pad, unit line
     * spacing, no ellipsize/maxLines (overflow is clipped to the box by the replayer's local clip).
     */
    public fun build(
        text: String,
        style: TextStyle,
        boxWidthPt: Double,
        fontResolver: FontResolver,
    ): StaticLayout {
        val paint = TextPaint().apply {
            isAntiAlias = true
            isSubpixelText = true
            isLinearText = false
            hinting = Paint.HINTING_ON
            typeface = fontResolver.resolve(style.fontFamily, bold = style.bold, italic = style.italic)
            textSize = (style.sizePt * LAYOUT_SCALE).toFloat()
            color = style.color.toArgb()
        }
        val widthLayoutUnits = (boxWidthPt * LAYOUT_SCALE).roundToInt()
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, widthLayoutUnits)
            .setAlignment(style.align.toLayoutAlignment())
            .setIncludePad(false)
            .setLineSpacing(0f, 1f)
            // Layout.* constants (API 23, minSdk-safe) — NOT android.graphics.text.LineBreaker (API 29).
            .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .build()
    }
}

/** START→left, CENTER→centre, END→right under the fixed FIRSTSTRONG_LTR direction. */
internal fun TextAlign.toLayoutAlignment(): Layout.Alignment = when (this) {
    TextAlign.START -> Layout.Alignment.ALIGN_NORMAL
    TextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
    TextAlign.END -> Layout.Alignment.ALIGN_OPPOSITE
}
