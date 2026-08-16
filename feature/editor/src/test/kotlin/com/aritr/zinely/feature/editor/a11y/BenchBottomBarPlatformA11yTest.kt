package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.feature.editor.AddActionLabel
import com.aritr.zinely.feature.editor.BenchBottomBar
import com.aritr.zinely.feature.editor.RedoActionLabel
import com.aritr.zinely.feature.editor.UndoActionLabel
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The C4 bottom bar on the **platform** accessibility tree — [ADR-094](../../../../../../../../docs/DECISIONS.md#adr-094)
 * row 4.5's second clause, which is not optional and is the reason this file exists beside [BenchC4Test].
 *
 * Row 4.5 says the disabled states are read *"off the platform tree"*. Compose can report `enabled = true`
 * to `AccessibilityNodeInfo` while `assertIsNotEnabled` passes on the merged tree — that exact defect
 * shipped once through a green suite (`ReframeControls.ZoomButton`,
 * [ADR-058](../../../../../../../../docs/DECISIONS.md#adr-058) branch) and was caught only by dumping the
 * real tree. A withheld `Undo` that TalkBack still offers to activate is a worse failure than a missing
 * one: it invites a tap that cannot act.
 *
 * `isClickable` is asserted alongside `isEnabled` for the same reason [BenchStyleRowPlatformA11yTest] does:
 * `disabled()` alone leaves a node a service may still try to activate, so the withheld controls carry no
 * `clickable` modifier at all.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BenchBottomBarPlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render(
        canUndo: Boolean = false,
        canRedo: Boolean = false,
        doneEnabled: Boolean = true,
        doneUnavailableBecause: String? = null,
    ) {
        composeRule.setContent {
            ZinelyTheme {
                BenchBottomBar(
                    canUndo = canUndo,
                    canRedo = canRedo,
                    doneEnabled = doneEnabled,
                    doneUnavailableBecause = doneUnavailableBecause,
                    onUndo = {},
                    onRedo = {},
                    onAdd = {},
                    onDone = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun node(label: String) =
        composeRule.onNodeWithContentDescription(label).platformNode(composeRule.activity)

    private fun assertWithheld(label: String) {
        val n = node(label)
        assertEquals("$label must carry Role.Button to the platform tree", "android.widget.Button", n.className)
        assertFalse("$label is withheld and must report DISABLED to the platform", n.isEnabled)
        assertFalse("$label must not be offered as clickable — activating it does nothing", n.isClickable)
    }

    private fun assertLive(label: String) {
        val n = node(label)
        assertEquals("$label must carry Role.Button to the platform tree", "android.widget.Button", n.className)
        assertTrue("$label must be enabled to the platform", n.isEnabled)
        assertTrue("$label must be activatable by an accessibility service, not only by touch", n.isClickable)
    }

    @Test
    fun at_rest_undo_and_redo_report_disabled_and_unclickable_to_the_platform() {
        // The freeze draws both `disabled` (`v2-bench.html:465-466`), and a fresh document has no history.
        render()
        assertWithheld(UndoActionLabel)
        assertWithheld(RedoActionLabel)
    }

    @Test
    fun at_rest_add_and_done_are_live_to_the_platform() {
        render()
        assertLive(AddActionLabel)
        assertLive(Copy.EditText.DONE)
    }

    @Test
    fun history_lights_the_two_controls_on_the_platform_tree_too() {
        // The state that matters is the one the platform sees — a control that lights only in the merged
        // tree is invisible to the service that would announce it.
        render(canUndo = true, canRedo = true)
        assertLive(UndoActionLabel)
        assertLive(RedoActionLabel)
    }

    @Test
    fun during_a_text_session_done_is_withheld_on_the_platform_tree() {
        // Row 4.8a: C3's style-row chip owns "finish editing" while a session is open, so this control is
        // withheld — and must be withheld to TalkBack, not merely greyed.
        render(doneEnabled = false)
        assertWithheld(Copy.EditText.DONE)
    }

    @Test
    fun a_withheld_done_tells_the_platform_WHY_and_a_live_one_says_nothing() {
        // F-1's rule, F-6's second reason. Without this a screen-reader user hears "Done, disabled" and has
        // no way to learn what would revive it — the same defect F-1 fixed one file over on the style row.
        //
        // `stateDescription`, never `contentDescription`: the control's NAME is `Done` in every state, and
        // folding the reason into the name would rename a button according to why it cannot be pressed.
        // Asserted on the PLATFORM node because that is the tree TalkBack reads; the merged tree can carry
        // a state the platform never publishes.
        render(doneEnabled = false, doneUnavailableBecause = Copy.BenchBar.DONE_AFTER_INK)
        assertEquals(
            "a withheld Done must say what would revive it",
            Copy.BenchBar.DONE_AFTER_INK,
            node(Copy.EditText.DONE).stateDescription?.toString(),
        )
        assertEquals(
            "its name must not change with its availability",
            Copy.EditText.DONE,
            node(Copy.EditText.DONE).contentDescription?.toString(),
        )
    }

    @Test
    // Its own test rather than a second half of the one above: `setContent` may be called once per
    // activity, so two renders in one method throw before either assertion runs.
    fun a_live_done_carries_no_state_at_all() {
        // The half a "just always publish it" simplification would break. A live control has nothing to
        // explain, and an instruction on it would be advice to do what the user is already free to do.
        render()
        assertNull("a live Done must carry no state", node(Copy.EditText.DONE).stateDescription)
    }

    @Test
    fun the_two_withheld_states_do_not_give_the_same_reason() {
        // Two states end differently — a text session by the row's own Done, an ink session by the card's —
        // so one shared sentence would be true of both and useful for neither. Pinned as a literal pair
        // because a later "de-duplication" collapsing them is exactly the change this guards against.
        assertTrue(
            "the text and ink reasons must differ",
            Copy.BenchBar.DONE_AFTER_TEXT != Copy.BenchBar.DONE_AFTER_INK,
        )
        render(doneEnabled = false, doneUnavailableBecause = Copy.BenchBar.DONE_AFTER_TEXT)
        assertEquals(
            Copy.BenchBar.DONE_AFTER_TEXT,
            node(Copy.EditText.DONE).stateDescription?.toString(),
        )
    }
}
