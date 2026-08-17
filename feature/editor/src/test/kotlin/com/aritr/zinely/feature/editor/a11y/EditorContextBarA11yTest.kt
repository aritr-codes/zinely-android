package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.feature.editor.EditorContextBar
import com.aritr.zinely.ui.a11y.PlatformA11yNode
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.a11y.platformTraversalStops
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * CI-29 (`stateDescription`) + CI-30 (`Role`) for the [EditorContextBar], on the **platform**
 * `AccessibilityNodeInfo` tree — the tree TalkBack actually reads.
 *
 * ## Why this file is platform-tier now (RF-2)
 *
 * It was not. Every assertion here used to be against Compose's **merged** semantics tree, excused by a
 * justification that has since expired twice over:
 *
 * * It claimed the CI-26 harness could not read `getStateDescription()` — stale since C5 added the field.
 * * It then claimed the bar was *"a horizontally scrolled row"*, so a control could scroll off-screen and
 *   materialise an empty platform node, making a per-control platform assertion unreliable in the JVM
 *   harness. **F-2 replaced the scrolling Row with a `FlowRow`** (`EditorContextBar.kt`), so every one of the
 *   eleven controls is laid out at full size at every supported width. There is nothing left to scroll off,
 *   and the obstacle that excused the weaker tier is gone.
 *
 * The upgrade is not bookkeeping. `EditorContextBar.kt:255-265` records a device-measured defect these
 * merged-tree assertions could not see: `clearAndSetSemantics` deletes the `OnClick` action `IconButton`
 * installed, and the platform derives `isClickable` and `ACTION_CLICK` from that action alone — so all nine
 * then-visible controls reported `clickable=false focusable=false` to TalkBack (SM-A176B, Android 16) while
 * still working under a finger. That is the WCAG 2.5.7 non-dragging alternative to nudge/scale/rotate
 * silently unavailable to exactly the users it exists for, through a green suite. `isClickable` on the
 * platform node is the assertion that would have caught it, and it is the first one below.
 *
 * `className` carries the same weight as `Role` did, and more: each control `clearAndSetSemantics`, so it
 * collapses to a single node and the role reaches the platform as `android.widget.Button` rather than
 * degrading to `android.view.View` the way a role over merged child content does
 * ([ADR-059](../../../../../../../../docs/DECISIONS.md#adr-059), [ZButtonPlatformA11yTest]).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorContextBarA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** The eight selection-scoped transform verbs — present whatever the selection's size. */
    private val transformVerbs = listOf(
        Copy.A11y.MOVE_LEFT,
        Copy.A11y.MOVE_RIGHT,
        Copy.A11y.MOVE_UP,
        Copy.A11y.MOVE_DOWN,
        Copy.A11y.MAKE_LARGER,
        Copy.A11y.MAKE_SMALLER,
        Copy.A11y.ROTATE_CLOCKWISE,
        Copy.A11y.ROTATE_COUNTERCLOCKWISE,
    )

    /** Id-scoped, so the bar offers them only for a single selected element. */
    private val reorderVerbs = listOf(Copy.A11y.BRING_FORWARD, Copy.A11y.SEND_BACKWARD)

    /**
     * The eleven controls a single text selection puts on screen: eight transforms, two reorders, Delete —
     * plus the Style disclosure, which is not a twelfth *verb* but is a twelfth node the platform publishes,
     * so it is checked with the rest.
     */
    private val allControls = transformVerbs + reorderVerbs + Copy.A11y.DELETE + Copy.Editor.TEXT_STYLE

    private fun renderBar(
        selection: Set<String> = setOf("only-text"),
        styleOpen: Boolean = false,
        withStyle: Boolean = true,
    ) {
        composeRule.setContent {
            ZinelyTheme {
                EditorContextBar(
                    selection = selection,
                    dispatch = {},
                    onStyle = if (withStyle) ({}) else null,
                    styleOpen = styleOpen,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun node(label: String): PlatformA11yNode =
        composeRule.onNodeWithContentDescription(label).platformNode(composeRule.activity)

    @Test
    fun every_control_is_named_clickable_and_enabled_on_the_platform_tree() {
        renderBar()
        assertEquals("eleven verbs plus the Style disclosure", 12, allControls.size)
        allControls.forEach { label ->
            val n = node(label)
            assertEquals("$label must carry Role.Button to the platform tree", "android.widget.Button", n.className)
            assertEquals("$label must publish its own name to the platform, not inherit or lose it", label, n.contentDescription)
            assertTrue("$label reached the platform unnamed", !n.contentDescription.isNullOrBlank())
            // The one that caught the shipped defect: `clearAndSetSemantics` deletes IconButton's OnClick,
            // and the platform derives ACTION_CLICK from it alone. A control that works under a finger and
            // reports `clickable=false` here is unusable by TalkBack and Switch Access.
            assertTrue("$label must be activatable by an accessibility service, not only by touch", n.isClickable)
            assertTrue("$label must be enabled to the platform — this bar withholds nothing", n.isEnabled)
        }
    }

    @Test
    fun every_control_reports_a_forty_eight_dp_touch_target_to_the_platform() {
        renderBar()
        // WCAG 2.5.5 / ADR-029 §6, measured on the tree that decides where a service may tap — not on the
        // Compose modifier that was asked for. The 1px tolerance is the same one [BenchPageNavA11yTest] uses
        // and the reason is documented in DEVICE-VERIFICATION.md §5: Compose reports touch bounds around the
        // content, and odd-width content puts both edges on a half-pixel that `getBoundsInScreen` rounds in
        // OPPOSITE directions, so a genuine 48px span is reported as 47. That is rounding, not a small target.
        val floor = with(composeRule.density) { 48.dp.toPx() }
        allControls.forEach { label ->
            val bounds = node(label).boundsInScreen
            assertTrue(
                "$label reports a ${bounds.width()}×${bounds.height()}px hit-rect, under the ${floor}px floor",
                bounds.width() >= floor - 1f && bounds.height() >= floor - 1f,
            )
        }
    }

    @Test
    fun the_text_style_disclosure_announces_showing_when_the_type_bar_is_open() {
        renderBar(styleOpen = true)
        assertEquals(
            "the disclosure must tell the PLATFORM what the tap did — the merged tree can carry a state " +
                "the platform never publishes",
            Copy.Editor.SHOWING,
            node(Copy.Editor.TEXT_STYLE).stateDescription,
        )
        // ...as STATE, never folded into the name: the control is called `Text style` in both states.
        assertEquals(Copy.Editor.TEXT_STYLE, node(Copy.Editor.TEXT_STYLE).contentDescription)
    }

    @Test
    fun the_text_style_disclosure_announces_hidden_when_the_type_bar_is_closed() {
        // Its own test rather than a second half of the one above: `setContent` may be called once per
        // activity, so two renders in one method throw before either assertion runs.
        renderBar(styleOpen = false)
        assertEquals(Copy.Editor.HIDDEN, node(Copy.Editor.TEXT_STYLE).stateDescription)
    }

    @Test
    fun a_transform_control_carries_no_state_at_all() {
        renderBar()
        // The half a "just always publish it" simplification would break: a nudge has no state to report,
        // and an announcement on it would be noise between the name and the action.
        assertEquals(null, node(Copy.A11y.MOVE_LEFT).stateDescription)
    }

    @Test
    fun the_reorder_controls_are_absent_from_the_platform_tree_for_a_multi_selection() {
        // Reorder is id-scoped (`Intent.Reorder` takes one id), so the bar cannot offer it for two elements.
        // Asserted by walking the platform tree rather than by a merged-tree absence: a control that is
        // "not shown" but still published is one an accessibility service can still land on and activate.
        renderBar(selection = setOf("a", "b"), withStyle = false)
        val labels = platformTraversalStops(composeRule.activity).map { it.label }.toSet()
        reorderVerbs.forEach {
            assertFalse("$it must not reach the platform tree for a multi-selection: $labels", it in labels)
        }
        // ...and the check is only worth anything if the tree it read was the populated one.
        transformVerbs.forEach {
            assertTrue("$it must still be published for a multi-selection: $labels", it in labels)
        }
    }
}
