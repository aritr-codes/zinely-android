package com.aritr.zinely.ui.a11y

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * One accessible stand-in for something the canvas draws — see [ZinelyV2CanvasSemantics].
 *
 * @param key stable identity for the drawn thing; becomes the node's test tag as
 *   `"$ZinelyV2CanvasNodeTagPrefix$key"`.
 * @param corners the drawn thing's outline in **device pixels**, in the surface's own coordinate space.
 *   Any polygon: the node is placed on its axis-aligned bounding box, so a rotated quad is handled by
 *   passing its four rotated corners. A single point is legal — it bounds to a zero-area box, which the
 *   minimum-size inflation then opens to a full target. An **empty** list is not placeable at all, and the
 *   node is skipped.
 * @param label the spoken name.
 * @param selected the selected state, announced on the node.
 * @param activateLabel the name of the default action — what a service offers as "activate".
 * @param onActivate runs on activation.
 * @param actions the **named custom actions**: one per gesture the canvas offers, so a gesture-driven
 *   surface has a single-pointer twin for each of its gestures (WCAG 2.5.7). Required rather than
 *   defaulted: the constitution's rule is *nothing is gesture-only*, and a parameter that defaults to
 *   empty lets a call site skip the twins without ever saying so. Passing `emptyList()` is a claim the
 *   thing has no gestures — which is fine, and now visible in the call site's own source.
 */
@Immutable
public class ZinelyV2CanvasNode(
    public val key: String,
    public val corners: List<Offset>,
    public val label: String,
    public val selected: Boolean,
    public val activateLabel: String,
    public val onActivate: () -> Unit,
    public val actions: List<CustomAccessibilityAction>,
)

/** Stable test-tag prefix; a canvas node's tag is `"$ZinelyV2CanvasNodeTagPrefix${node.key}"`. */
public const val ZinelyV2CanvasNodeTagPrefix: String = "zinely-v2-canvas-node-"

/**
 * The minimum side of a canvas node's focus/hit area. WCAG 2.5.8: a target below this is hard to acquire,
 * and a canvas routinely draws things smaller than a fingertip. Not a design token — a platform floor, the
 * same one `minimumInteractiveComponentSize()` applies to a laid-out control.
 */
public val ZinelyV2CanvasNodeMinSide: Dp = 48.dp

/**
 * A rectangle in device pixels, already inflated to the minimum target size — the output of
 * [zinelyV2CanvasNodeBounds].
 */
@Immutable
public data class ZinelyV2CanvasNodeRect(
    public val leftPx: Int,
    public val topPx: Int,
    public val widthPx: Float,
    public val heightPx: Float,
)

/**
 * Where a canvas node sits: the axis-aligned bounding box of [corners], inflated **about its own centre**
 * to at least [minSidePx] on each side.
 *
 * Inflating about the centre rather than from the top-left is what keeps a tiny element's enlarged target
 * concentric with the thing it stands for; growing from a corner would drift the focus rectangle off the
 * mark it names, and two adjacent small elements would both drift the same way and overlap on one side.
 *
 * Pure, so the geometry is unit-testable without a composition — the arithmetic is the part most likely to
 * be quietly wrong, and a screenshot cannot see a focus rectangle at all.
 *
 * @return `null` when [corners] is empty; a caller with nothing to bound has no node to place.
 */
public fun zinelyV2CanvasNodeBounds(corners: List<Offset>, minSidePx: Float): ZinelyV2CanvasNodeRect? {
    if (corners.isEmpty()) return null
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    for (c in corners) {
        minX = min(minX, c.x); maxX = max(maxX, c.x)
        minY = min(minY, c.y); maxY = max(maxY, c.y)
    }
    val width = max(maxX - minX, minSidePx)
    val height = max(maxY - minY, minSidePx)
    val centerX = (minX + maxX) / 2f
    val centerY = (minY + maxY) / 2f
    return ZinelyV2CanvasNodeRect(
        leftPx = (centerX - width / 2f).roundToInt(),
        topPx = (centerY - height / 2f).roundToInt(),
        widthPx = width,
        heightPx = height,
    )
}

/**
 * The **canvas virtual accessibility node tree**: a screen-reader-navigable mirror of things a `Canvas`
 * draws, which the platform accessibility tree would otherwise see as one undifferentiated rectangle.
 *
 * A canvas is a single node to TalkBack. Everything drawn inside it — a page's elements, a fold guide's
 * panels — is invisible to a screen reader unless something publishes a node per drawn thing. This
 * composable is that something: one focusable semantic node per [ZinelyV2CanvasNode], positioned over the
 * thing's device-pixel bounds, carrying its name, its selected state, an activation, and the named custom
 * actions that are the single-pointer twins of the surface's gestures.
 *
 * ## Three properties that are not obvious and are easy to lose
 *
 * 1. **The nodes carry semantics only — never pointer input.** No `clickable`, no `selectable`. A node
 *    that consumed pointer input would sit on top of the canvas and steal the taps and drags the gesture
 *    layer beneath it exists to receive. A service still focuses and activates the node through the
 *    `onClick` **semantic** action; a finger falls straight through to the canvas.
 * 2. **Traversal order is declaration order, pinned.** The nodes overlap, and overlapping bounds give a
 *    nondeterministic reading order. `isTraversalGroup` plus an explicit `traversalIndex` per node makes
 *    the order the caller's list order — so a caller that passes elements in paint order gets a reading
 *    order that matches what a sighted user sees back-to-front.
 * 3. **Every node is at least [ZinelyV2CanvasNodeMinSide] on a side** (WCAG 2.5.8), inflated about its
 *    own centre — see [zinelyV2CanvasNodeBounds].
 *
 * ## What this is, relative to `ElementSemanticsLayer`
 *
 * `:feature:editor`'s `ElementSemanticsLayer` is the V1 implementation of exactly this mechanism, welded
 * to the editor's `Element`/`Intent` vocabulary. This is the mechanism alone, product-free, in the module
 * every V2 surface already depends on — the Phase A "canvas virtual a11y node-tree scaffolding". It does
 * **not** replace `ElementSemanticsLayer`, which the roadmap requires be preserved; Phase C adapts that
 * layer onto this one, keeping its behaviour and its intent vocabulary and dropping only the duplicated
 * mechanism. Nothing here knows what an element, a page or a zine is, so a fold guide or a proof sheet can
 * use it on the same terms.
 *
 * @param nodes one per drawn thing, in the order they should be read.
 * @param modifier must be sized identically to the sibling canvas, or the device-pixel bounds in [nodes]
 *   will not land over the things they name.
 */
@Composable
public fun ZinelyV2CanvasSemantics(
    nodes: List<ZinelyV2CanvasNode>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val minSidePx = with(density) { ZinelyV2CanvasNodeMinSide.toPx() }

    Box(modifier = modifier.semantics { isTraversalGroup = true }) {
        nodes.forEachIndexed { index, node ->
            val rect = zinelyV2CanvasNodeBounds(node.corners, minSidePx) ?: return@forEachIndexed
            val widthDp = with(density) { rect.widthPx.toDp() }
            val heightDp = with(density) { rect.heightPx.toDp() }
            Box(
                modifier = Modifier
                    .offset { IntOffset(rect.leftPx, rect.topPx) }
                    .size(widthDp, heightDp)
                    .testTag("$ZinelyV2CanvasNodeTagPrefix${node.key}")
                    .semantics {
                        contentDescription = node.label
                        role = Role.Button
                        selected = node.selected
                        traversalIndex = index.toFloat()
                        onClick(label = node.activateLabel) { node.onActivate(); true }
                        customActions = node.actions
                    },
            )
        }
    }
}
