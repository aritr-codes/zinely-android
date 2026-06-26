package com.aritr.zinely.feature.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.EditorUiState
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.render.android.SelectionChromeGeometry
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Stable test-tag prefix; an element node's tag is `"$ElementNodeTagPrefix${element.id}"`. */
public const val ElementNodeTagPrefix: String = "element-node-"

/**
 * The accessible mirror of the page (ADR-029 §6, WCAG 2.4.7/1.3.1): one focusable semantic node per
 * placed element, positioned over the element's device-px bounds. The page itself renders as a single
 * decorative [PagePreview] Canvas (no per-element semantics of its own); this overlay supplies the
 * screen-reader structure — each node carries the element [EditorA11y.label], its **selected** state, a
 * `Select` click, and the full transform/reorder/delete [EditorA11y.elementCustomActions] (the 2.5.7
 * single-pointer twins of the gesture transforms).
 *
 * Nodes are placed by the element's **rotated axis-aligned bounds** (the bounding box of the four rotated
 * corners from [SelectionChromeGeometry.outlineDevicePx]); a node is at least 48dp on each side so the
 * touch/focus target meets WCAG 2.5.8 even for a tiny element. Document order = the elements list, so the
 * traversal order mirrors the back-to-front paint order.
 *
 * Stateless: the host hoists [uiState] and [dispatch]. The visible [EditorContextBar] reuses the same
 * intents on the current selection.
 *
 * @param uiState the history-free editor projection (current page, selection, view scale/offset).
 * @param dispatch forwards an [Intent] into the store.
 * @param modifier sized identically to the sibling [PagePreview] so node device-px bounds align.
 */
@Composable
public fun ElementSemanticsLayer(
    uiState: EditorUiState,
    dispatch: (Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = uiState.document.pages[uiState.currentPageIndex]
    val screenPxPerPt = uiState.view.screenPxPerPt.toDouble()
    val pageOffset = uiState.view.pageOffset
    val density = LocalDensity.current
    val minSidePx = with(density) { 48.dp.toPx() }

    // A traversal group so TalkBack walks the nodes in document (back-to-front) order via traversalIndex,
    // rather than the nondeterministic order overlapping bounds would otherwise produce.
    Box(modifier = modifier.semantics { isTraversalGroup = true }) {
        page.elements.forEachIndexed { index, element ->
            val corners = SelectionChromeGeometry.outlineDevicePx(element.transform, screenPxPerPt, pageOffset)
            if (corners.size != 4) return@forEachIndexed
            val bounds = aabb(corners)
            // Inflate to a ≥48dp square hit/focus area, centred on the element (WCAG 2.5.8).
            val wPx = max(bounds.width, minSidePx)
            val hPx = max(bounds.height, minSidePx)
            val leftPx = (bounds.centerX - wPx / 2f).roundToInt()
            val topPx = (bounds.centerY - hPx / 2f).roundToInt()
            val wDp = with(density) { wPx.toDp() }
            val hDp = with(density) { hPx.toDp() }

            val selected = element.id in uiState.selection
            val actions = EditorA11y.elementCustomActions(element.id, dispatch)
            val description = EditorA11y.label(element)

            // Semantics-ONLY (no clickable/selectable): the node must not consume pointer input, or it
            // would steal taps/drags from the page gesture layer beneath it. TalkBack still focuses and
            // activates it via the `onClick` semantic action; touch falls through to editorTransformGestures.
            Box(
                modifier = Modifier
                    .offset { IntOffset(leftPx, topPx) }
                    .size(wDp, hDp)
                    .testTag("$ElementNodeTagPrefix${element.id}")
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                        this.selected = selected
                        traversalIndex = index.toFloat()
                        onClick(label = "Select") { dispatch(Intent.Select(element.id)); true }
                        customActions = actions
                    },
            )
        }
    }
}

/** Axis-aligned bounds of a polygon in device px. */
private data class DeviceBounds(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f
}

private fun aabb(corners: List<PtPoint>): DeviceBounds {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    for (c in corners) {
        minX = min(minX, c.x.toFloat()); maxX = max(maxX, c.x.toFloat())
        minY = min(minY, c.y.toFloat()); maxY = max(maxY, c.y.toFloat())
    }
    return DeviceBounds(minX, minY, maxX, maxY)
}
