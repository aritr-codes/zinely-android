package com.aritr.zinely.feature.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.editor.EditorUiState
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.KnobSnap
import com.aritr.zinely.core.editor.TurnMath
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.render.android.SelectionChromeGeometry
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Colors
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.zinelyV21LightColors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** The knob's 48dp hit box. */
internal const val TurnHandleTestTag: String = "turn-handle"

/** The angle readout pill, present only while a knob drag is live (A48.5). */
internal const val TurnReadoutTestTag: String = "turn-readout"

/** A48.1: the disc centre sits this far beyond the box's bottom (or, flipped, top) edge. */
internal val TurnKnobDistanceDp = 54.dp
private val TurnKnobHitDp = 48.dp
private val TurnKnobDiscDp = 18.dp
private val TurnKnobGlyphDp = 12.dp
private val TurnKnobStrokeDp = 1.6.dp

/** Where the stem starts: 6dp out, just past the ring (5.2dp) that it hangs from. */
private val TurnKnobStemStartDp = 6.dp

/** A48.5: the readout sits 8dp clear of the piece's on-screen bounds, 4dp inside the canvas. */
private val TurnReadoutGapDp = 8.dp
private val TurnReadoutInsetDp = 4.dp

/**
 * A48.5's readout text: normalised to (−180, 180], whole degrees, a true minus ("15°", "−30°", "180°").
 * Rounds half up, as the prototype's `Math.round` does.
 */
internal fun turnReadout(degrees: Double): String {
    val n = kotlin.math.floor(TurnMath.normalise(degrees) + 0.5).toInt()
    return if (n < 0) "−${-n}°" else "$n°"
}

/**
 * The knob's disc centre in device px (A48.1): [distancePx] beyond the bottom-centre edge point along the
 * piece's TURNED down-normal, or beyond the top-centre one along the up-normal when [top].
 */
internal fun turnKnobCentrePx(
    t: Transform,
    screenPxPerPt: Double,
    pageOffset: PtPoint,
    distancePx: Float,
    top: Boolean,
): Offset {
    val sign = if (top) -1.0 else 1.0
    val edge = SelectionChromeGeometry.handleDevicePx(t, PtPoint(0.0, sign), screenPxPerPt, pageOffset)
    val r = Math.toRadians(t.rotationDegrees)
    // The down-normal (0, 1) turned by the same matrix the handles use: (−sin, cos).
    return Offset((edge.x - sign * sin(r) * distancePx).toFloat(), (edge.y + sign * cos(r) * distancePx).toFloat())
}

/**
 * A48.2's one rule: below if the knob's hit box [below] fits, else above if [above] fits, else `null` (the
 * knob is not drawn). Fits = wholly inside [visible] and overlapping no [obstacles] (the verb bar, a tray,
 * the snack, the first-run hint). All rects in the same space.
 */
internal fun turnKnobTop(below: Rect, above: Rect, visible: Rect, obstacles: List<Rect>): Boolean? {
    fun fits(r: Rect) = r.left >= visible.left && r.right <= visible.right && r.top >= visible.top &&
        r.bottom <= visible.bottom && obstacles.none { it.overlaps(r) }
    return when {
        fits(below) -> false
        fits(above) -> true
        else -> null
    }
}

/** The signed angle swept round [centre] from [from] to [to], in (−180, 180]; screen y is down, so clockwise is positive. */
internal fun sweptDegrees(from: Offset, to: Offset, centre: Offset): Double {
    val a0 = atan2((from.y - centre.y).toDouble(), (from.x - centre.x).toDouble())
    val a1 = atan2((to.y - centre.y).toDouble(), (to.x - centre.x).toDouble())
    return TurnMath.normalise(Math.toDegrees(a1 - a0))
}

private class KnobStart(
    val transform: Transform,
    val id: String,
    val top: Boolean,
    val hitCentre: Offset,
    val screenPxPerPt: Double,
    val pageOffset: PtPoint,
)

private class KnobDrag(val id: String, val before: Transform, val top: Boolean, val hitCentre: Offset, val angle: Double)

/**
 * The turn knob of Bench A48 ([ADR-143](../../../../../../../../docs/DECISIONS.md#adr-143) decision 1): a
 * ninth mark beside the eight [ResizeHandles], drawn in the same overlay and with the same session pattern —
 * `BeginTransform` once the finger passes the touch slop, a preview through [onTurn] each frame, and one
 * `CommitTransform` of `before.copy(rotationDegrees = r)` on release (never re-baked, so x, y, width and
 * height stay bit-identical). The knob alone snaps (A48.4, [KnobSnap]); two fingers are not changed.
 *
 * **Invariants.**
 * - It exposes nothing to the accessibility tree, exactly as the resize handles do: no `clickable`, no
 *   semantics, only `pointerInput`. Its non-pointer twins are Arrange > Turn and the element's actions.
 * - The side is chosen only while no knob drag is live, so it never jumps out from under the finger. The hit
 *   box stays where the drag began; the pointer stays captured by it, and only the drawn knob turns.
 * - No jump on touch: nothing happens until the touch slop is passed, and the angle is the angle at
 *   touch-down plus the angle swept round the box centre since touch-down, unwrapped frame by frame.
 *
 * @param visibleInWindow the visible canvas in window px (cut by the screen edge and the shelf); `null` =
 *   this overlay's own bounds.
 * @param obstaclesInWindow chrome the knob must not touch, in window px.
 * @param readoutColors the room's palette for the readout, which is chrome over the page; the knob itself
 *   always paints the on-paper values.
 */
@Composable
internal fun TurnHandle(
    uiState: EditorUiState,
    currentState: () -> EditorUiState,
    dispatch: (Intent) -> Unit,
    onTurn: (Map<String, Transform>?) -> Unit,
    modifier: Modifier = Modifier,
    visibleInWindow: Rect? = null,
    obstaclesInWindow: List<Rect> = emptyList(),
    readoutColors: ZinelyV21Colors = ZinelyTheme.v21Colors,
) {
    val density = LocalDensity.current
    val haptics = ZinelyTheme.haptics
    var origin by remember { mutableStateOf<Offset?>(null) }
    var size by remember { mutableStateOf(Size.Zero) }
    var knobDrag by remember { mutableStateOf<KnobDrag?>(null) }

    Box(
        modifier = modifier.onGloballyPositioned {
            origin = it.positionInWindow()
            size = Size(it.size.width.toFloat(), it.size.height.toFloat())
        },
    ) {
        val at = origin ?: return@Box
        val page = uiState.document.pages[uiState.currentPageIndex]
        val id = uiState.selection.singleOrNull()
        val element = id?.let { page.elements.firstOrNull { e -> e.id == it } }
        val itx = uiState.interaction
        // A48.7: hidden while typing, in Reframe, and unless exactly one piece is selected.
        if (element == null || itx is Interaction.EditingText || itx is Interaction.Reframing) return@Box
        val screenPxPerPt = uiState.view.screenPxPerPt.toDouble()
        val pageOffset = uiState.view.pageOffset
        val distancePx = with(density) { TurnKnobDistanceDp.toPx() }
        val half = with(density) { TurnKnobHitDp.toPx() } / 2f
        val visible = visibleInWindow?.translate(-at) ?: Rect(Offset.Zero, size)

        val live = knobDrag?.takeIf { it.id == element.id }
        val shown = live?.before?.copy(rotationDegrees = live.angle) ?: element.transform
        val top = live?.top ?: run {
            fun hit(top: Boolean) = Rect(turnKnobCentrePx(element.transform, screenPxPerPt, pageOffset, distancePx, top), half)
            turnKnobTop(hit(false), hit(true), visible, obstaclesInWindow.map { it.translate(-at) })
        } ?: return@Box // A48.2: neither side fits, so the knob is not drawn.

        val knob = turnKnobCentrePx(shown, screenPxPerPt, pageOffset, distancePx, top)
        KnobMark(shown, knob, top, screenPxPerPt, pageOffset)

        val hitCentre = live?.hitCentre ?: knob
        val latest by rememberUpdatedState(KnobStart(element.transform, element.id, top, hitCentre, screenPxPerPt, pageOffset))
        Box(
            Modifier
                .offset { IntOffset((hitCentre.x - half).roundToInt(), (hitCentre.y - half).roundToInt()) }
                .size(TurnKnobHitDp)
                .testTag(TurnHandleTestTag)
                .pointerInput(element.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val start = latest
                        val boxTopLeft = start.hitCentre - Offset(half, half)
                        val centrePt = SelectionChromeGeometry.handleDevicePx(start.transform, PtPoint(0.0, 0.0), start.screenPxPerPt, start.pageOffset)
                        val centre = Offset(centrePt.x.toFloat(), centrePt.y.toFloat())
                        var last = down.position + boxTopLeft
                        // A48.3: nothing turns until the finger has passed the platform touch slop.
                        val past = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                            ?: return@awaitEachGesture
                        dispatch(Intent.BeginTransform(setOf(start.id)))
                        val session = currentState().interaction as? Interaction.Transforming
                        val before = session?.before?.get(start.id)
                        if (session == null || before == null) {
                            session?.let { dispatch(Intent.CancelTransform(it.token)) }
                            return@awaitEachGesture
                        }
                        var knobSnap = KnobSnap.startingAt(before.rotationDegrees)
                        var swept = 0.0
                        var angle = before.rotationDegrees
                        var onQuarter = TurnMath.isOnQuarter(angle)
                        var committed = false
                        fun step(position: Offset) {
                            val p = position + boxTopLeft
                            swept += sweptDegrees(last, p, centre)
                            last = p
                            val raw = before.rotationDegrees + swept
                            knobSnap = knobSnap.next(raw)
                            angle = knobSnap.angleFor(raw)
                            val nowOnQuarter = TurnMath.isOnQuarter(angle)
                            // A48.4: one light tick on entering a snap, never a sound.
                            if (nowOnQuarter && !onQuarter) haptics.perform(ZinelyHaptic.Tick)
                            onQuarter = nowOnQuarter
                            knobDrag = KnobDrag(start.id, before, start.top, start.hitCentre, angle)
                            onTurn(mapOf(start.id to before.copy(rotationDegrees = angle)))
                        }
                        try {
                            step(past.position)
                            past.consume()
                            val ended = drag(past.id) { change ->
                                step(change.position)
                                change.consume()
                            }
                            if (ended) {
                                // One drag, one step; a drag back where it began records none (the reducer's
                                // tolerant test). The angle alone: never re-baked.
                                dispatch(Intent.CommitTransform(mapOf(start.id to before.copy(rotationDegrees = angle)), session.token))
                                committed = true
                            }
                        } finally {
                            if (!committed) dispatch(Intent.CancelTransform(session.token))
                            knobDrag = null
                            onTurn(null)
                        }
                    }
                },
        )

        if (live != null) {
            TurnReadout(shown, live.angle, knobBelow = !top, visible, screenPxPerPt, pageOffset, readoutColors)
        }
    }
}

/** The stem and the disc (A48.1), at their on-paper values in both themes, turned with the piece. */
@Composable
private fun KnobMark(t: Transform, knob: Offset, top: Boolean, screenPxPerPt: Double, pageOffset: PtPoint) {
    val paper = zinelyV21LightColors()
    val density = LocalDensity.current
    val stemStart = turnKnobCentrePx(t, screenPxPerPt, pageOffset, with(density) { TurnKnobStemStartDp.toPx() }, top)
    val discRadius = with(density) { TurnKnobDiscDp.toPx() } / 2f
    Canvas(Modifier.fillMaxSize()) {
        val dir = (knob - stemStart).let { it / it.getDistance().coerceAtLeast(1e-3f) }
        drawLine(paper.ink, stemStart, knob - dir * discRadius, strokeWidth = TurnKnobStrokeDp.toPx())
    }
    Box(
        Modifier
            .offset { IntOffset((knob.x - discRadius).roundToInt(), (knob.y - discRadius).roundToInt()) }
            .size(TurnKnobDiscDp)
            .graphicsLayer { rotationZ = t.rotationDegrees.toFloat() }
            .background(paper.paper, CircleShape)
            .border(TurnKnobStrokeDp, paper.ink, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(TurnGlyph, contentDescription = null, tint = paper.ink, modifier = Modifier.size(TurnKnobGlyphDp))
    }
}

/**
 * A48.5: the pill, on the side of the piece opposite the knob, 8dp clear of the piece's on-screen bounds and
 * kept inside the canvas. Leaf tint only when the angle is exactly on a quarter turn. Not spoken.
 */
@Composable
private fun TurnReadout(
    t: Transform,
    angle: Double,
    knobBelow: Boolean,
    visible: Rect,
    screenPxPerPt: Double,
    pageOffset: PtPoint,
    colors: ZinelyV21Colors,
) {
    val corners = listOf(-1.0 to -1.0, 1.0 to -1.0, 1.0 to 1.0, -1.0 to 1.0).map { (x, y) ->
        SelectionChromeGeometry.handleDevicePx(t, PtPoint(x, y), screenPxPerPt, pageOffset)
    }
    val left = corners.minOf { it.x }.toFloat()
    val right = corners.maxOf { it.x }.toFloat()
    val topY = corners.minOf { it.y }.toFloat()
    val bottomY = corners.maxOf { it.y }.toFloat()
    val snapped = TurnMath.isOnQuarter(angle)
    Text(
        text = turnReadout(angle),
        color = if (snapped) colors.onLeaf else colors.ink,
        fontFamily = ZinelyV21Fonts.Work,
        fontWeight = FontWeight.Bold,
        fontSize = 12.48.sp, // .78rem
        lineHeight = 17.47.sp, // 1.4
        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
        modifier = Modifier
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(p.width, p.height) {
                    val gap = TurnReadoutGapDp.toPx()
                    val inset = TurnReadoutInsetDp.toPx()
                    val wantY = if (knobBelow) topY - gap - p.height else bottomY + gap
                    val wantX = (left + right) / 2f - p.width / 2f
                    val x = wantX.coerceAtMost(visible.right - inset - p.width).coerceAtLeast(visible.left + inset)
                    val y = wantY.coerceAtMost(visible.bottom - inset - p.height).coerceAtLeast(visible.top + inset)
                    p.place(x.roundToInt(), y.roundToInt())
                }
            }
            .testTag(TurnReadoutTestTag)
            .clearAndSetSemantics { }
            .border(BorderStroke(1.5.dp, colors.ink), RoundedCornerShape(percent = 50))
            .background(if (snapped) colors.leafTint else colors.surface, RoundedCornerShape(percent = 50))
            .padding(horizontal = ZinelyV21Dimens.gapMd, vertical = ZinelyV21Dimens.gapHair),
    )
}

/** The knob's 12dp turn glyph: an open circle with an arrowhead (`v21-bench.html` A48 `GLYPH`). */
private val TurnGlyph: ImageVector = ImageVector.Builder(
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    listOf("M19 12a7 7 0 1 1-2.1-5", "M18 3v4h-4").forEach {
        addPath(
            pathData = addPathNodes(it),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.4f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}.build()
