package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * The frozen Shelf's inline SVGs, redrawn on Compose's canvas.
 *
 * Every glyph in `shelf.html` is authored on a 24-unit `viewBox`, so each of these draws in the
 * same 24-unit space scaled by `u = width / 24` — the path literals below are the spec's `d`
 * attributes, coordinate for coordinate. Stroke widths, caps and joins are the spec's too; where
 * `stroke-linejoin` is absent the SVG default (miter) applies.
 *
 * They take a `tint` and fill their box, matching the `(tint: Color) -> Unit` icon slot the M1
 * components expose.
 *
 * **Deliberately absent: the action sheet's `share` glyph** (`shelf.html:455`). Nothing backs a
 * share action — `ProjectRepository` exposes no share, and adding one is a product change, not a
 * reskin. Shipping the icon with no consumer would be dead code. The Share menu item is an M2
 * deferral, tracked as an accepted deviation; draw the glyph when the action exists.
 *
 * The sort menu's check glyph is absent for a different reason: `ZMenuItem` already draws its own.
 */
@Composable
internal fun ShelfGlyph(
    tint: Color,
    strokeWidth: Float = 2f,
    cap: StrokeCap = StrokeCap.Butt,
    join: StrokeJoin = StrokeJoin.Miter,
    draw: DrawScope.(u: Float, tint: Color, stroke: Stroke) -> Unit,
) {
    Canvas(Modifier.fillMaxSize()) {
        val u = size.width / 24f
        draw(u, tint, Stroke(width = strokeWidth * u, cap = cap, join = join))
    }
}

/** `.search svg` — `<circle cx=11 cy=11 r=7>` + `<path d="M21 21l-4-4">`. */
@Composable
internal fun SearchGlyph(tint: Color) {
    ShelfGlyph(tint) { u, c, s ->
        drawCircle(c, radius = 7 * u, center = Offset(11 * u, 11 * u), style = s)
        drawLine(c, Offset(21 * u, 21 * u), Offset(17 * u, 17 * u), s.width, StrokeCap.Round)
    }
}

/** `.sortbtn svg` — `M4 7h16 M7 12h10 M10 17h4`. */
@Composable
internal fun SortGlyph(tint: Color) {
    ShelfGlyph(tint, cap = StrokeCap.Round) { u, c, s ->
        drawLine(c, Offset(4 * u, 7 * u), Offset(20 * u, 7 * u), s.width, StrokeCap.Round)
        drawLine(c, Offset(7 * u, 12 * u), Offset(17 * u, 12 * u), s.width, StrokeCap.Round)
        drawLine(c, Offset(10 * u, 17 * u), Offset(14 * u, 17 * u), s.width, StrokeCap.Round)
    }
}

/** `.start svg` — `M12 5v14 M5 12h14`, stroke-width 2.4. */
@Composable
internal fun PlusGlyph(tint: Color) {
    ShelfGlyph(tint, strokeWidth = 2.4f, cap = StrokeCap.Round) { u, c, s ->
        drawLine(c, Offset(12 * u, 5 * u), Offset(12 * u, 19 * u), s.width, StrokeCap.Round)
        drawLine(c, Offset(5 * u, 12 * u), Offset(19 * u, 12 * u), s.width, StrokeCap.Round)
    }
}

/** `.more svg` — three filled `r=1.7` dots. */
@Composable
internal fun MoreGlyph(tint: Color) {
    ShelfGlyph(tint) { u, c, _ ->
        drawCircle(c, radius = 1.7f * u, center = Offset(5 * u, 12 * u))
        drawCircle(c, radius = 1.7f * u, center = Offset(12 * u, 12 * u))
        drawCircle(c, radius = 1.7f * u, center = Offset(19 * u, 12 * u))
    }
}

/** Action sheet `data-act="open"` — `M4 5h16v14H4z` + `M4 9h16`. */
@Composable
internal fun OpenGlyph(tint: Color) {
    ShelfGlyph(tint) { u, c, s ->
        drawRect(c, topLeft = Offset(4 * u, 5 * u), size = Size(16 * u, 14 * u), style = s)
        drawLine(c, Offset(4 * u, 9 * u), Offset(20 * u, 9 * u), s.width)
    }
}

/** Action sheet `data-act="rename"` — `M4 20h16` + `M14 5l5 5L9 20H4v-5L14 5z`. */
@Composable
internal fun RenameGlyph(tint: Color) {
    ShelfGlyph(tint, join = StrokeJoin.Round) { u, c, s ->
        drawLine(c, Offset(4 * u, 20 * u), Offset(20 * u, 20 * u), s.width, StrokeCap.Round)
        val pencil = Path().apply {
            moveTo(14 * u, 5 * u)
            lineTo(19 * u, 10 * u)
            lineTo(9 * u, 20 * u)
            lineTo(4 * u, 20 * u)
            lineTo(4 * u, 15 * u)
            close()
        }
        drawPath(pencil, c, style = s)
    }
}

/**
 * Action sheet `data-act="duplicate"` — the front `<rect x=8 y=8 w=12 h=12 rx=2>` over the back
 * sheet's open outline. The spec's `a2 2 0 0 0` arcs are quarter-circles of radius 2; a quadratic
 * with the corner as control point is the same curve to within a fraction of a device pixel.
 */
@Composable
internal fun DuplicateGlyph(tint: Color) {
    ShelfGlyph(tint) { u, c, s ->
        val back = Path().apply {
            moveTo(16 * u, 8 * u)
            lineTo(16 * u, 6 * u)
            quadraticBezierTo(16 * u, 4 * u, 14 * u, 4 * u)
            lineTo(6 * u, 4 * u)
            quadraticBezierTo(4 * u, 4 * u, 4 * u, 6 * u)
            lineTo(4 * u, 14 * u)
            quadraticBezierTo(4 * u, 16 * u, 6 * u, 16 * u)
            lineTo(8 * u, 16 * u)
        }
        drawPath(back, c, style = s)
        drawRoundRect(
            color = c,
            topLeft = Offset(8 * u, 8 * u),
            size = Size(12 * u, 12 * u),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2 * u),
            style = s,
        )
    }
}

/** Action sheet `data-act="delete"` — `M5 7h14 M10 7V4h4v3 M6 7l1 13h10l1-13`. */
@Composable
internal fun DeleteGlyph(tint: Color) {
    ShelfGlyph(tint, cap = StrokeCap.Round, join = StrokeJoin.Round) { u, c, s ->
        drawLine(c, Offset(5 * u, 7 * u), Offset(19 * u, 7 * u), s.width, StrokeCap.Round)
        val lid = Path().apply {
            moveTo(10 * u, 7 * u)
            lineTo(10 * u, 4 * u)
            lineTo(14 * u, 4 * u)
            lineTo(14 * u, 7 * u)
        }
        drawPath(lid, c, style = s)
        val body = Path().apply {
            moveTo(6 * u, 7 * u)
            lineTo(7 * u, 20 * u)
            lineTo(17 * u, 20 * u)
            lineTo(18 * u, 7 * u)
        }
        drawPath(body, c, style = s)
    }
}

/** `.errorstate .badge svg` — the warning triangle, its stem and its dot. */
@Composable
internal fun ErrorGlyph(tint: Color) {
    ShelfGlyph(tint, join = StrokeJoin.Round) { u, c, s ->
        val triangle = Path().apply {
            moveTo(12 * u, 3 * u)
            lineTo(21 * u, 19 * u)
            lineTo(3 * u, 19 * u)
            close()
        }
        drawPath(triangle, c, style = s)
        drawLine(
            c, Offset(12 * u, 8 * u), Offset(12 * u, 13 * u),
            strokeWidth = 2.2f * u, cap = StrokeCap.Round,
        )
        drawCircle(c, radius = 1.2f * u, center = Offset(12 * u, 16.5f * u))
    }
}
