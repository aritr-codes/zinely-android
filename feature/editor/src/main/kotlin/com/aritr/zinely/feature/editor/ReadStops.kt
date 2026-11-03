package com.aritr.zinely.feature.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.SpatialOrder
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * One thing Read says about a page ([ADR-135](../../../../../../../../docs/DECISIONS.md#adr-135) decisions 1-3).
 * [words] is spoken as text (so TalkBack's by-word and by-character controls work on a page's own words);
 * every other stop is a plain label. Bounds are the piece's rotated, axis-aligned box in page points.
 */
internal data class ReadStop(
    val id: String,
    val label: String,
    val isText: Boolean,
    val leftPt: Double,
    val topPt: Double,
    val widthPt: Double,
    val heightPt: Double,
)

/** Id of the single stop on an empty page. */
internal const val READ_EMPTY_PAGE_STOP_ID: String = "read-empty-page"

/** Test tag prefix of a stop's node: `read-stop-<index>`, index in reading order. */
internal const val ReadStopTestTagPrefix: String = "read-stop-"

/**
 * The stops of one page, in [SpatialOrder] (the Bench's own order, so the two never disagree).
 *
 * Text with words is its words; a photo is `Photo` or `Photo: <description>`; Art is a stop only when
 * described (undescribed Art is decoration); blank text gets none. A piece with no part on the page gets none,
 * and is dropped **before** the empty-page test. A page with no stops is `Blank page`, or `Only Art on this
 * page` when Art is drawn on it.
 */
internal fun readStops(page: Page, pageSizePt: PtSize): List<ReadStop> {
    val onPage = page.elements.filter { bounds(it).intersects(pageSizePt) }
    val stops = SpatialOrder.order(onPage, pageSizePt.height).mapNotNull { element ->
        val label = when (element) {
            is TextElement -> element.text.takeIf { it.isNotBlank() }
            is DecorElement -> element.description?.takeIf { it.isNotBlank() }?.let { EditorA11y.label(element) }
            else -> EditorA11y.label(element)
        } ?: return@mapNotNull null
        val b = bounds(element)
        ReadStop(element.id, label, element is TextElement, b.left, b.top, b.right - b.left, b.bottom - b.top)
    }
    if (stops.isNotEmpty()) return stops
    val label = if (onPage.any { it is DecorElement }) Copy.ProofRead.ONLY_ART else Copy.ProofRead.BLANK_PAGE
    return listOf(ReadStop(READ_EMPTY_PAGE_STOP_ID, label, false, 0.0, 0.0, pageSizePt.width, pageSizePt.height))
}

private class Aabb(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    fun intersects(page: PtSize): Boolean = right > 0.0 && bottom > 0.0 && left < page.width && top < page.height
}

private fun bounds(element: Element): Aabb {
    val t = element.transform
    val rad = t.rotationDegrees * PI / 180.0
    val c = abs(cos(rad))
    val s = abs(sin(rad))
    val halfW = (t.widthPt * c + t.heightPt * s) / 2.0
    val halfH = (t.widthPt * s + t.heightPt * c) / 2.0
    val cx = t.xPt + t.widthPt / 2.0
    val cy = t.yPt + t.heightPt / 2.0
    return Aabb(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
}

/**
 * The semantics-only layer over the leaf: no role, no click, no actions. Declared in reading order with
 * `traversalIndex` restating it. [scalePx] is the leaf's pixels per point. Each node is at least 48dp so it is
 * a touch target, as on the Bench.
 */
@Composable
internal fun ReadStopsLayer(stops: List<ReadStop>, scalePx: Float, modifier: Modifier = Modifier) {
    val minPx = with(LocalDensity.current) { 48.dp.toPx() }
    Box(modifier.semantics { isTraversalGroup = true }) {
        stops.forEachIndexed { index, stop ->
            val w = max((stop.widthPt * scalePx).toFloat(), minPx)
            val h = max((stop.heightPt * scalePx).toFloat(), minPx)
            val cx = ((stop.leftPt + stop.widthPt / 2.0) * scalePx).toFloat()
            val cy = ((stop.topPt + stop.heightPt / 2.0) * scalePx).toFloat()
            Box(
                Modifier
                    .offset { IntOffset((cx - w / 2f).toInt(), (cy - h / 2f).toInt()) }
                    .size(with(LocalDensity.current) { w.toDp() }, with(LocalDensity.current) { h.toDp() })
                    .testTag(ReadStopTestTagPrefix + index)
                    .semantics {
                        traversalIndex = index.toFloat()
                        if (stop.isText) text = AnnotatedString(stop.label) else contentDescription = stop.label
                    },
            )
        }
    }
}
