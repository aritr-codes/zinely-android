package com.aritr.zinely.ui.a11y

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import androidx.compose.ui.test.SemanticsNodeInteraction

/**
 * CI-26 — assert against the tree TalkBack actually reads, not Compose's merged semantics tree.
 *
 * ## Why this exists
 *
 * A Compose semantics assertion (`assertIsEnabled`, `assertIsNotEnabled`, `assertHasClickAction`, …)
 * validates the **merged semantics tree** Compose keeps for its own test framework. An accessibility
 * service (TalkBack, Switch Access) does **not** read that tree — it reads the platform
 * [AccessibilityNodeInfo] tree that Compose synthesises on demand through the hosting `AndroidComposeView`'s
 * [android.view.accessibility.AccessibilityNodeProvider]. The two are derived from the same source but are
 * **not the same object**, and they can disagree.
 *
 * That disagreement is a real, shipped defect class: in commit `f4faaa4` a zoom stepper
 * (`:feature:editor`'s `ReframeControls.ZoomButton` — named in prose, not linked, because this file now
 * lives upstream of that module) passed `assertIsNotEnabled` against the
 * merged tree while the platform node it handed TalkBack reported `isEnabled == true`. A green Compose
 * suite, a control that lied to the screen reader. CLAUDE.md "Device Verification / Pass 1" names the same
 * trap and points at the on-device `adb shell uiautomator dump` recipe — this harness brings that platform
 * view of *class / clickable / enabled / bounds* into the existing Robolectric JVM suite, so the check runs
 * before a device is ever involved.
 *
 * ## How it reaches the platform tree
 *
 * 1. The target Compose control is located the normal way — a [SemanticsNodeInteraction] from
 *    `onNodeWithContentDescription(...)` / `onNodeWithTag(...)`. Its `fetchSemanticsNode().id` is the
 *    **virtual view id** Compose assigns that node in the platform tree.
 * 2. The `AndroidComposeView` hosting the composition is found by walking the Activity's view hierarchy
 *    (matched by class name — the class itself is `internal` to Compose, but every public attribute we read
 *    is plain framework API on [View] / [AccessibilityNodeInfo]).
 * 3. `view.accessibilityNodeProvider` is the exact provider the platform accessibility framework calls;
 *    `provider.createAccessibilityNodeInfo(virtualViewId)` builds the exact [AccessibilityNodeInfo] TalkBack
 *    would receive for that node.
 * 4. The framework-visible attributes are snapshotted into [PlatformA11yNode].
 *
 * ## Usage
 *
 * ```kotlin
 * val node = composeRule.onNodeWithContentDescription("Zoom in").platformNode(composeRule.activity)
 * assertTrue(node.isEnabled)                  // the ENABLED bit TalkBack reads — CI-26's core assertion
 * assertEquals("android.widget.Button", node.className)
 * assertTrue(node.isClickable)
 * assertTrue(node.hasNonEmptyBounds)
 * ```
 *
 * Reusable, no new dependency, no `src/main` seam: everything here is standard test-support that CI-29 /
 * CI-30 / CI-93 can consume as-is.
 *
 * ## Two views of the same tree
 *
 * [platformNode] answers *"what does the platform say about **this** control?"* — the per-node view CI-26/29/
 * 30/93 need. [platformTraversalStops] answers *"in what **order** does the platform present this surface?"*
 * — the whole-surface view CI-31 needs. Same provider, same framework attributes; only the question differs.
 */
public data class PlatformA11yNode(
    /** `AccessibilityNodeInfo.getClassName()` — e.g. `android.widget.Button` for a `Role.Button` control. */
    val className: String?,
    /** `AccessibilityNodeInfo.isClickable()` — whether the platform advertises a click to the service. */
    val isClickable: Boolean,
    /** `AccessibilityNodeInfo.isEnabled()` — the bit that lied in `f4faaa4`; the reason this harness exists. */
    val isEnabled: Boolean,
    /** `AccessibilityNodeInfo.getBoundsInScreen(...)` — the on-screen hit-rect the service reports. */
    val boundsInScreen: Rect,
    /** `AccessibilityNodeInfo.getContentDescription()` — the spoken label, for cross-checking identity. */
    val contentDescription: String?,
    /**
     * `AccessibilityNodeInfo.isLongClickable()` — whether the platform advertises a **second** gesture.
     *
     * Added for B3, whose shelf item is the first V2 control with one. Distinct from [longClickLabel] on
     * purpose: a long press can be present and anonymous, which is the shape the implementation guide's
     * *"every gesture has a named custom action twin"* rule exists to rule out, so a test that only checked
     * this bit could not tell a discoverable gesture from a hidden one.
     */
    val isLongClickable: Boolean,
    /**
     * The label the platform reports for `ACTION_LONG_CLICK`, or `null` when the action is unnamed or absent.
     *
     * Read off `actionList` rather than a convenience getter because there is no getter: an action's label is
     * carried by the `AccessibilityAction` entry itself.
     */
    val longClickLabel: String?,
) {
    /** True when the platform bounds have positive area — a laid-out, hit-testable control. */
    val hasNonEmptyBounds: Boolean
        get() = boundsInScreen.width() > 0 && boundsInScreen.height() > 0
}

/**
 * Read the **platform** [AccessibilityNodeInfo] for this Compose control and snapshot it into a
 * [PlatformA11yNode]. See [PlatformAccessibilityTree] for the full contract and rationale (CI-26).
 *
 * @receiver the located Compose control (from `onNodeWithContentDescription` / `onNodeWithTag`, etc.).
 * @param activity the Activity hosting the composition — typically `composeRule.activity`. Used only to
 *   locate the `AndroidComposeView`; nothing about the Activity is mutated.
 * @throws IllegalStateException if the hosting `AndroidComposeView`, its accessibility provider, or the
 *   node info for this control cannot be obtained — each with a message naming what was missing, so a
 *   failure reads as a harness problem, not a silent false pass.
 */
public fun SemanticsNodeInteraction.platformNode(activity: Activity): PlatformA11yNode {
    val virtualViewId = fetchSemanticsNode().id

    val composeView = findAndroidComposeView(activity.window.decorView)
        ?: error(
            "No AndroidComposeView found in the Activity view hierarchy — the composition must be hosted " +
                "in a real Android view for its platform AccessibilityNodeInfo tree to exist.",
        )

    val provider = composeView.accessibilityNodeProvider
        ?: error(
            "AndroidComposeView returned no AccessibilityNodeProvider — the platform accessibility bridge " +
                "is not installed, so there is no tree for TalkBack (or this harness) to read.",
        )

    val info: AccessibilityNodeInfo = provider.createAccessibilityNodeInfo(virtualViewId)
        ?: error(
            "AccessibilityNodeProvider produced no AccessibilityNodeInfo for virtualViewId=$virtualViewId " +
                "(the control's merged semantics node). It is not present in the platform tree.",
        )

    val bounds = Rect().also { info.getBoundsInScreen(it) }
    val longClick = info.actionList.firstOrNull { it.id == AccessibilityNodeInfo.ACTION_LONG_CLICK }
    return PlatformA11yNode(
        className = info.className?.toString(),
        isClickable = info.isClickable,
        isEnabled = info.isEnabled,
        boundsInScreen = bounds,
        contentDescription = info.contentDescription?.toString(),
        isLongClickable = info.isLongClickable,
        longClickLabel = longClick?.label?.toString(),
    )
}

/**
 * One stop an accessibility service lands on while traversing a surface, as the **platform** tree reports it.
 *
 * A "stop" is a node the platform itself flags `isScreenReaderFocusable` — the public `AccessibilityNodeInfo`
 * bit a service reads to decide what it may land on. It is *not* every named node: Compose publishes a merged
 * control as a focusable but **unnamed** parent whose name lives on a synthetic `contentDescription` child,
 * alongside the control's own glyph and label children and a synthetic role child, none of which are focusable.
 * Using the platform's own flag rather than "has a name" is what keeps a four-button tray four stops instead
 * of twelve. See [platformTraversalStops] for how the name is then resolved.
 *
 * @property label the name a service would speak — see [platformTraversalStops]'s resolution order.
 * @property className `AccessibilityNodeInfo.getClassName()`, kept for cross-checking identity.
 * @property boundsInScreen the on-screen rect the platform reports for this stop — the control's own focus
 *   rect, not its label's. This is the geometry a reading-order assertion compares against (CI-31).
 */
public data class PlatformA11yStop(
    val label: String,
    val className: String?,
    val boundsInScreen: Rect,
)

/**
 * The ordered list of traversal stops the **platform** `AccessibilityNodeInfo` tree presents for the
 * composition hosted by [activity] — CI-31's raw material.
 *
 * ## What this reads, and what that does and does not prove
 *
 * The walk is a pre-order depth-first traversal of the platform tree, visiting each node's children **in the
 * order the tree itself reports them** (`AccessibilityNodeInfo.getChildId(index)`), starting from
 * [AccessibilityNodeProvider.HOST_VIEW_ID]. It is therefore the order an accessibility service sees when it
 * walks the tree — not Compose's merged semantics tree, per §11.3 *"the platform's tree is the truth"*.
 *
 * **Two honest bounds, measured on this repo's Robolectric host rather than assumed:**
 *
 * 1. **Child order here is the composition's declaration order, not a geometry-sorted order.** Compose
 *    publishes children in declaration order and expresses any *re-sorting* separately, through
 *    `AccessibilityNodeInfo.setTraversalBefore/setTraversalAfter` hints. On this host those hints are
 *    **`UNDEFINED` on every node of every surface probed** — so the sequence returned here is exactly what a
 *    service would follow, but it carries no evidence about how a device's TalkBack might additionally
 *    re-sort. A test that only asserted this sequence would prove the declaration order and nothing about the
 *    *visual* order; that is why CI-31's assertion pairs the sequence with the geometry check over
 *    [PlatformA11yStop.boundsInScreen].
 * 2. **`getChildId` is reached by reflection.** `getChildCount()` is public but the per-index child *id*
 *    accessor is a hidden framework method. It resolves under Robolectric (which runs the real
 *    `android-all` framework classes); it would be blocked by hidden-API enforcement in an instrumentation
 *    test on a device. This harness is therefore JVM-only, and an on-device confirmation remains the
 *    `adb shell uiautomator dump` pass in CLAUDE.md's device verification.
 *
 * ## Which nodes become stops, and where each one's name comes from
 *
 * A node is a stop when **no ancestor of it is already a stop** and either
 *
 * - the platform flags it `isScreenReaderFocusable` (public API since API 28), **or**
 * - it speaks for itself — it carries its own non-blank `contentDescription` or `text`.
 *
 * This is a deliberate approximation of TalkBack's own rule, and the shape of the approximation is worth
 * stating because each half exists to correct the other:
 *
 * - `isScreenReaderFocusable` **alone is too narrow**. Compose does not set it on a node that merely carries
 *   a `contentDescription` over children it did not merge — the Proof Read act's pager is exactly that, and a
 *   focusable-only rule drops the one stop that names the user's zine. TalkBack focuses "speaking" nodes,
 *   not only flagged ones.
 * - "has a name" **alone is too wide**. Compose publishes a merged control as a focusable but *unnamed*
 *   parent whose name lives on a synthetic `contentDescription` child, beside the control's own glyph and
 *   label children and a synthetic role child. A name-only rule turns a four-button supply tray into twelve
 *   stops, none of which a service would land on separately.
 *
 * The ancestor rule is what makes the union safe: once a control is a stop, nothing inside it can be one.
 *
 * The name is then resolved over the stop's **own subtree, excluding any nested focusable subtree**, in this
 * order: the first non-blank `contentDescription` found (the stop's own, else the synthetic
 * content-description child Compose attaches to a merged control), else the first non-blank `text`. A stop
 * for which neither exists is still returned, with an empty label, so an unnamed focusable control fails
 * loudly in a caller's diff instead of silently disappearing from the sequence.
 *
 * @param activity the Activity hosting the composition — typically `composeRule.activity`.
 * @throws IllegalStateException if the hosting `AndroidComposeView`, its provider, its root node, or the
 *   hidden child accessor cannot be obtained — each naming what was missing, so a harness failure never reads
 *   as a silent empty pass.
 */
public fun platformTraversalStops(activity: Activity): List<PlatformA11yStop> {
    val composeView = findAndroidComposeView(activity.window.decorView)
        ?: error(
            "No AndroidComposeView found in the Activity view hierarchy — the composition must be hosted " +
                "in a real Android view for its platform AccessibilityNodeInfo tree to exist.",
        )
    val provider = composeView.accessibilityNodeProvider
        ?: error(
            "AndroidComposeView returned no AccessibilityNodeProvider — the platform accessibility bridge " +
                "is not installed, so there is no tree for TalkBack (or this harness) to walk.",
        )
    val root = provider.createAccessibilityNodeInfo(AccessibilityNodeProvider.HOST_VIEW_ID)
        ?: error(
            "AccessibilityNodeProvider produced no root AccessibilityNodeInfo (HOST_VIEW_ID) — there is no " +
                "tree to traverse.",
        )
    val getChildId = runCatching {
        AccessibilityNodeInfo::class.java.getMethod("getChildId", Int::class.javaPrimitiveType)
    }.getOrElse {
        error(
            "AccessibilityNodeInfo.getChildId(int) is not reachable on this runtime (${it.javaClass.name}). " +
                "It is a hidden framework method this harness reflects into to read the platform tree's own " +
                "child ORDER; without it there is no traversal order to assert.",
        )
    }

    // AccessibilityNodeInfo packs a source id as (virtualDescendantId shl 32) or accessibilityViewId.
    // A child the provider refuses to build is an error, never a silent omission: swallowing it would
    // shorten the traversal sequence and a caller would read the gap as "that stop is not there".
    fun childrenOf(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> =
        (0 until node.childCount).map { index ->
            val packed = getChildId.invoke(node, index) as Long
            val virtualViewId = (packed shr 32).toInt()
            provider.createAccessibilityNodeInfo(virtualViewId)
                ?: error(
                    "AccessibilityNodeProvider produced no AccessibilityNodeInfo for child $index of " +
                        "${node.className} (virtualViewId=$virtualViewId), though the parent reports " +
                        "${node.childCount} children. Dropping it would silently shorten the traversal " +
                        "sequence.",
                )
        }

    /**
     * Names inside [node]'s own subtree, in tree order, stopping only at a nested **focusable** child.
     *
     * The recursion rule here is deliberately *narrower* than [walk]'s stop rule (focusable **or** named),
     * and the asymmetry is load-bearing rather than an oversight: a merged control's own name lives on a
     * *named* child — the synthetic content-description node Compose attaches — so excluding named children
     * here would discard the very node the label is read from. Nothing is double-counted, because a named
     * child of a stop can never become a stop itself ([walk] refuses any node with a stop ancestor).
     */
    fun namesUnder(node: AccessibilityNodeInfo, into: MutableList<Pair<String?, String?>>) {
        into += node.contentDescription?.toString()?.takeIf { it.isNotBlank() } to
            node.text?.toString()?.takeIf { it.isNotBlank() }
        childrenOf(node).forEach { if (!it.isScreenReaderFocusable) namesUnder(it, into) }
    }

    fun labelOf(node: AccessibilityNodeInfo): String {
        val names = mutableListOf<Pair<String?, String?>>().also { namesUnder(node, it) }
        return names.firstNotNullOfOrNull { it.first } ?: names.firstNotNullOfOrNull { it.second } ?: ""
    }

    val stops = mutableListOf<PlatformA11yStop>()
    fun walk(node: AccessibilityNodeInfo, insideStop: Boolean) {
        val speaksForItself = !node.contentDescription.isNullOrBlank() || !node.text.isNullOrBlank()
        val isStop = !insideStop && (node.isScreenReaderFocusable || speaksForItself)
        if (isStop) {
            stops += PlatformA11yStop(
                label = labelOf(node),
                className = node.className?.toString(),
                boundsInScreen = Rect().also { node.getBoundsInScreen(it) },
            )
        }
        childrenOf(node).forEach { walk(it, insideStop || isStop) }
    }
    walk(root, insideStop = false)
    return stops
}

/**
 * Depth-first search for the `AndroidComposeView` that hosts the composition. Matched by simple class name
 * because the class is `internal` to Compose; only public [View] API is touched thereafter.
 */
private fun findAndroidComposeView(root: View): View? {
    if (root.javaClass.simpleName == "AndroidComposeView") return root
    if (root is ViewGroup) {
        for (i in 0 until root.childCount) {
            findAndroidComposeView(root.getChildAt(i))?.let { return it }
        }
    }
    return null
}
