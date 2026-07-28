package com.aritr.zinely.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * WCAG 2.x relative luminance and contrast ratio — pure, no dependency.
 *
 * `Color.red/green/blue` are the sRGB (gamma-encoded) components in `[0,1]`, identical to the 8-bit
 * channel / 255. WCAG linearises each, weights them, and forms `(Llighter+.05)/(Ldarker+.05)`.
 *
 * **Opaque colours only.** Every pairing asserted against this helper must be fully opaque; a
 * translucent foreground would need compositing against its actual backdrop first, and silently
 * measuring the un-composited colour would report a ratio the user never sees. [contrastRatio]
 * rejects translucent input rather than returning a plausible wrong number.
 *
 * `ZinelyColorsContrastTest` (V1) still carries its own private copy of this maths. That is left
 * alone deliberately: it is pinned to the V1 palette and retires with it at C0, and editing it now
 * would put a V1-guarding test at risk for no V2 benefit.
 */
internal object WcagContrast {

    /** WCAG 1.4.3 — normal text (below 18pt / 14pt-bold). */
    const val AA_NORMAL: Float = 4.5f

    /** WCAG 1.4.3 large text / 1.4.11 non-text — the decorative and graphical-boundary floor. */
    const val AA_LARGE: Float = 3.0f

    private fun linearize(channel: Float): Float =
        if (channel <= 0.03928f) channel / 12.92f
        else ((channel + 0.055f) / 1.055f).pow(2.4f)

    fun relativeLuminance(c: Color): Float =
        0.2126f * linearize(c.red) + 0.7152f * linearize(c.green) + 0.0722f * linearize(c.blue)

    fun contrastRatio(fg: Color, bg: Color): Float {
        require(fg.alpha == 1f && bg.alpha == 1f) {
            "contrastRatio needs opaque colours (fg alpha=${fg.alpha}, bg alpha=${bg.alpha}); " +
                "composite a translucent token against its real backdrop before measuring"
        }
        val a = relativeLuminance(fg) + 0.05f
        val b = relativeLuminance(bg) + 0.05f
        return if (a >= b) a / b else b / a
    }
}
