package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The S5-step-3 Completion · fold-steps screen (docs/design/SCREEN-INVENTORY.md#completion--fold-steps):
 * the "your zine is ready" payoff, four never-assumed fold steps, and the three actions
 * (send / open / keep editing). Robolectric NATIVE to match the sibling screen tests.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CompletionScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var send = 0
    private var open = 0
    private var keep = 0
    private var back = 0

    private fun setCompletion(working: CompletionAction? = null, error: String? = null) {
        composeRule.setContent {
            MaterialTheme {
                CompletionScreen(
                    onSendToFriend = { send++ },
                    onOpenIt = { open++ },
                    onKeepEditing = { keep++ },
                    onBack = { back++ },
                    working = working,
                    errorMessage = error,
                )
            }
        }
    }

    @Test
    fun celebrates_and_shows_every_fold_step() {
        setCompletion()
        composeRule.onNodeWithText("Your zine is ready! 🎉").assertExists()
        composeRule.onNodeWithTag(CompletionFoldStepsTestTag).assertExists()
        // All four steps are spelled out — the fold is never assumed known.
        composeRule.onNodeWithText("Fold the sheet in half, the long way. Press the crease flat.").assertExists()
        composeRule.onNodeWithText("Fold it in half again, then open this last fold back up.").assertExists()
        composeRule.onNodeWithText("Snip the little slit in the middle — just along the center crease.").assertExists()
        composeRule.onNodeWithText("Push the ends together so the slit opens, and fold it into a tiny book.").assertExists()
    }

    @Test
    fun send_to_a_friend_is_the_primary_action() {
        setCompletion()
        send = 0
        composeRule.onNodeWithTag(CompletionSendTestTag).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(1, send)
    }

    @Test
    fun open_and_keep_editing_are_reachable() {
        setCompletion()
        open = 0
        keep = 0
        composeRule.onNodeWithTag(CompletionOpenTestTag).performScrollTo().performClick()
        composeRule.onNodeWithTag(CompletionKeepEditingTestTag).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(1, open)
        assertEquals(1, keep)
    }

    @Test
    fun back_returns_to_export() {
        setCompletion()
        back = 0
        composeRule.onNodeWithContentDescription("Back").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(1, back)
    }

    @Test
    fun actions_disable_while_a_render_is_in_flight() {
        setCompletion(working = CompletionAction.SEND)
        composeRule.onNodeWithTag(CompletionSendTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(CompletionOpenTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(CompletionKeepEditingTestTag).assertIsNotEnabled()
    }
}
