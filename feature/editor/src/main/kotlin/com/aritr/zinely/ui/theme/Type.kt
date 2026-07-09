package com.aritr.zinely.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aritr.zinely.feature.editor.R

/**
 * The two type voices of the frozen spec, bundled as local resources.
 *
 * `--shell` is the grotesque UI chrome (Inter); `--voice` is the display serif the product speaks in
 * (Fraunces).
 *
 * The prototypes pull both from `fonts.googleapis.com`. **The app never does**: a CDN font request
 * is a network request, and Zinely's privacy invariant forbids one (PRD §5). Both families are
 * bundled under `res/font/` (SIL OFL 1.1; licences in `assets/fonts/`), so the fallback stacks in
 * `--shell`/`--voice` are unreachable by construction.
 *
 * Weights are exactly those the frozen CSS uses, and no more: Inter at 400/500/600/700, Fraunces at
 * 600 only. Neither family is set in italic anywhere in the trilogy.
 */
public object ZinelyFonts {
    /**
     * `--shell` — Inter (rsms/inter v4.1 statics). `inter_regular.ttf` is byte-identical to the
     * `Inter-Regular.ttf` the export path already renders with, so UI and print share one face.
     */
    public val Shell: FontFamily = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
        Font(R.font.inter_bold, FontWeight.Bold),
    )

    /**
     * `--voice` — Fraunces SemiBold.
     *
     * The spec requests the variable face with automatic optical sizing (`opsz,wght@9..144,600`).
     * Compose can only drive `opsz` through `FontVariation`, which the platform ignores below API 26
     * — and Zinely's `minSdk` is 24. So we bundle the static 9pt cut, the nearest optical size to the
     * 19–30px range `--voice` is actually set at. M2's pixel-parity gate is the check on that choice:
     * if the 9pt cut reads wrong against the frozen Shelf, swap the cut, never the spec.
     */
    public val Voice: FontFamily = FontFamily(
        Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    )
}

/** The frozen type voices, threaded through `LocalZinelyTypography`. */
@Immutable
public data class ZinelyTypography(
    val shell: FontFamily = ZinelyFonts.Shell,
    val voice: FontFamily = ZinelyFonts.Voice,
)

/**
 * The Material 3 type scale.
 *
 * Deliberately **unchanged** from the pre-reskin baseline. Every screen alive today reads
 * `MaterialTheme.typography`; re-basing it on Inter here would restyle all of them inside a milestone
 * whose brief is "infrastructure only, no visual regressions". Screens adopt [ZinelyTypography] as
 * they are reskinned (M2–M5), and this scale retires with the last of them.
 */
public val Typography: Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)
