package com.aritr.zinely.feature.editor.a11y

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
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
 * ([com.aritr.zinely.feature.editor.ReframeControls] `ZoomButton`) passed `assertIsNotEnabled` against the
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
    return PlatformA11yNode(
        className = info.className?.toString(),
        isClickable = info.isClickable,
        isEnabled = info.isEnabled,
        boundsInScreen = bounds,
        contentDescription = info.contentDescription?.toString(),
    )
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
