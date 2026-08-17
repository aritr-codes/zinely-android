package com.aritr.zinely.feature.library

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The six **cover marks** — `svg.mark` in `docs/design/mockups/v21-library.html`.
 *
 * Transcribed verbatim from the frozen file's path data, which is why they are built from SVG strings
 * rather than hand-written as `ImageVector` path calls. `addPathNodes` parses the same grammar the
 * prototype is authored in, so the transcription is a copy rather than a translation — and a copy is
 * checkable against the source by eye, which a sequence of `moveTo`/`curveTo` calls is not.
 *
 * All six share the frozen geometry: a `0 0 24 24` viewport, `stroke-width:1.6`, `fill:none`,
 * `stroke-linecap:round`, and SVG's default `miter` join, which none of the six overrides.
 * [Rings] is the one exception the file itself makes — it is drawn from two
 * `<circle>` elements, which carry no linecap because a closed path has no ends.
 *
 * ### These are decorative, and the semantics say so
 *
 * A mark is not the zine's content and does not name it. The cover's meaning is its title, which in
 * V2.1 sits *below* the cover as `.name`. A screen reader announcing "envelope" over a zine called
 * *Letters home* would be inventing content the design does not have — the same ruling [ZineCover]
 * records for V2's stamp. Callers pass `contentDescription = null`.
 *
 * ### Stroke colour is black here and tinted at the call site
 *
 * `stroke="currentColor"` in the frozen file, and `.cover .mark` sets that colour per surface —
 * `rgba(255,246,232,.92)` on ink, `ink-soft` on paper. An `ImageVector` has to bake *some* stroke
 * brush, so these bake black and rely on the caller's tint to replace it. Do not read the black as a
 * value: nothing draws these untinted.
 */
internal object ZineV21CoverMarks {

    /** An open booklet — a spread with a centre fold. `.ink-leaf`, the first cover on the shelf. */
    val Booklet: ImageVector = mark("M4 5h16v14H4z", "M4 9h16M9 9v10")

    /** An envelope. `.paper-s`. */
    val Envelope: ImageVector = mark("M3 6h18v12H3z", "m3 7 9 6 9-6")

    /**
     * Two overlapping rings — riso misregistration, the whole language in one glyph.
     *
     * The frozen file draws these as `<circle cx cy r>`, which has no path-data equivalent to copy, so
     * this is the one mark that is *converted* rather than transcribed: each circle becomes the
     * standard two-arc path, `M cx-r,cy a r,r 0 1,0 2r,0 a r,r 0 1,0 -2r,0`. The numbers are the
     * file's own (`9.5,10` and `14.5,14`, both `r=5.2`); only the notation changed.
     */
    val Rings: ImageVector = mark(
        "M4.3 10a5.2 5.2 0 1 0 10.4 0a5.2 5.2 0 1 0 -10.4 0",
        "M9.3 14a5.2 5.2 0 1 0 10.4 0a5.2 5.2 0 1 0 -10.4 0",
        cap = StrokeCap.Butt,
    )

    /** A sprig — stem with two leaves. `.paper-c`. */
    val Sprig: ImageVector = mark(
        "M12 21V9",
        "M12 9c0-4 3-6 6-6 0 4-2.5 6-6 6Z",
        "M12 13c0-3-2.5-5-5-5 0 3 2 5 5 5Z",
    )

    /** Three ruled lines, the last one short — text, as a mark. `.ink-butter`. */
    val Lines: ImageVector = mark("M6 5h12M6 10h12M6 15h7")

    /** A mug. `.ink-jam`. */
    val Mug: ImageVector = mark(
        "M5 8h11v6a4 4 0 0 1-4 4H9a4 4 0 0 1-4-4V8Z",
        "M16 9h2a2.5 2.5 0 0 1 0 5h-2",
    )

    /** The shelf's own order in the frozen file, for parity rasters and previews. */
    val Frozen: List<ImageVector> = listOf(Booklet, Envelope, Rings, Sprig, Lines, Mug)
}

private const val MARK_VIEWPORT = 24f
private const val MARK_STROKE = 1.6f

private fun mark(
    vararg pathData: String,
    cap: StrokeCap = StrokeCap.Round,
): ImageVector = ImageVector.Builder(
    defaultWidth = MARK_VIEWPORT.dp,
    defaultHeight = MARK_VIEWPORT.dp,
    viewportWidth = MARK_VIEWPORT,
    viewportHeight = MARK_VIEWPORT,
).apply {
    pathData.forEach {
        addPath(
            pathData = addPathNodes(it),
            // fill="none" in every one of the six. A filled mark would read as a sticker rather than
            // as a drawing, which is the distinction the whole handmade language rests on.
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = MARK_STROKE,
            strokeLineCap = cap,
            // SVG's default, and none of the six overrides it. An earlier version set Round without
            // flagging it: at 46% of a 120dp cover the mark scales ~2.8x, so rounding 0.8 user-units
            // of corner is ~2.3dp of visible softening on Booklet, Envelope, Mug and Sprig.
            strokeLineJoin = StrokeJoin.Miter,
        )
    }
}.build()
