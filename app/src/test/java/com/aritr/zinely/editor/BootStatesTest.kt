package com.aritr.zinely.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The editor's and the Proof's two non-Ready boot states, restyled into V2.1 — `.ph` for the wait,
 * `.fail` for the failure.
 *
 * They are asserted directly rather than through [ZinelyNavHost] because the loading window is
 * transient by construction: a host-level test that could observe it would have to stall the boot, and
 * what is checked here is what the state *draws and says*, not when it occurs. The host-level contract —
 * a boot error is not a dead end — stays in `ZinelyNavHostTest`.
 */
@RunWith(RobolectricTestRunner::class)
class BootStatesTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * The M3 spinner is gone, and its **semantics are not**.
     *
     * `CircularProgressIndicator` contributed an indeterminate `ProgressBarRangeInfo` and nothing else —
     * no name, no description. Deleting the component without carrying that across would leave a screen
     * a service reads as empty rather than as busy, which is the kind of regression a re-skin ships
     * silently because the pixels look better. Stated as a presence *and* a count, so a second
     * progress node smuggled back in fails too.
     */
    @Test
    fun `the wait is an unprinted sheet, and it still reports itself as in progress`() {
        setContent { BootLoading() }

        composeRule.onNodeWithTag(BootLoadingTestTag).assertExists()
        composeRule.onAllNodes(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate,
            ),
        ).assertCountEquals(1)
    }

    /**
     * A failure keeps saying **what failed** and **what to do next**. The message is the ViewModel's own
     * sentence, never a summary written here, and the way out is a real control rather than a hint.
     */
    @Test
    fun `a boot failure keeps the honest message and a working way out`() {
        var left = 0
        setContent {
            BootFailure(
                message = BOOT_ERROR,
                actionLabel = Copy.Nav.BACK_TO_SHELF,
                onAction = { left++ },
            )
        }

        composeRule.onNodeWithTag(BootFailureTestTag).assertExists()
        composeRule.onNodeWithText(BOOT_ERROR).assertExists()

        // `.retry` speaks its label: `zinelyV2Control` clears the Text node's own semantics and sets a
        // contentDescription, so the label survives the restyle as the control's spoken name.
        composeRule.onNodeWithContentDescription(Copy.Nav.BACK_TO_SHELF).assertExists()
        composeRule.onNodeWithTag(BootFailureActionTestTag).performClick()
        assertEquals(1, left)
    }

    /** The `!` mark is decoration the sentence already explains; it must not be read aloud as "!". */
    @Test
    fun `the failure mark says nothing a screen reader would have to hear`() {
        setContent { BootFailure(message = BOOT_ERROR, actionLabel = Copy.Nav.BACK_TO_SHELF) {} }

        composeRule.onAllNodesWithText("!").assertCountEquals(0)
    }

    private fun setContent(content: @Composable () -> Unit) = composeRule.setContent {
        ZinelyTheme {
            // The placeholder's sweep is infinite by design; reduced motion freezes it in place, which is
            // the frozen behaviour and also the only version a test clock can settle behind.
            CompositionLocalProvider(LocalZinelyMotion provides ZinelyMotion(reduceMotion = true)) {
                content()
            }
        }
    }

    private companion object {
        /** `EditorViewModel`'s own sentence — quoted, because a test that paraphrases it proves nothing. */
        const val BOOT_ERROR = "Couldn’t open this project."
    }
}
