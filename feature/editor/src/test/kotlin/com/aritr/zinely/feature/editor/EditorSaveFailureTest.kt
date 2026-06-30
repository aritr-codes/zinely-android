package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The warm save-failure banner ([EditorSaveFailure]) — the honest correction to the optimistic
 * "Saved ✨" (ADR-035, VOICE §Errors). It proves the *static* state is always correct and
 * motion-independent (the line is real text whenever visible, in both the animated and reduced-motion
 * paths) and that the dismiss control is wired. The host precedence (suppresses the chip + hint) is
 * proven in [EditorScreenTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorSaveFailureTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun renders_the_failure_line_when_visible_with_motion() {
        composeRule.setContent {
            MaterialTheme { EditorSaveFailure(visible = true, onDismiss = {}, reduceMotion = false) }
        }
        composeRule.onNodeWithTag(EditorSaveFailureTestTag).assertIsDisplayed()
        composeRule.onNodeWithText(SaveFailureText, substring = true).assertIsDisplayed()
    }

    @Test
    fun renders_the_failure_line_when_visible_with_reduced_motion() {
        // Reduced-motion degrades the transition, never the content: the static state is identical.
        composeRule.setContent {
            MaterialTheme { EditorSaveFailure(visible = true, onDismiss = {}, reduceMotion = true) }
        }
        composeRule.onNodeWithTag(EditorSaveFailureTestTag).assertIsDisplayed()
    }

    @Test
    fun renders_the_generic_line_for_the_generic_kind() {
        composeRule.setContent {
            MaterialTheme {
                EditorSaveFailure(visible = true, onDismiss = {}, kind = SaveErrorKind.Generic, reduceMotion = true)
            }
        }
        composeRule.onNodeWithText(SaveFailureText, substring = true).assertIsDisplayed()
    }

    @Test
    fun renders_the_storage_line_for_the_out_of_space_kind() {
        // ADR-036: the storage kind keys the specific "low on storage" copy, never the generic line.
        composeRule.setContent {
            MaterialTheme {
                EditorSaveFailure(visible = true, onDismiss = {}, kind = SaveErrorKind.OutOfSpace, reduceMotion = true)
            }
        }
        composeRule.onNodeWithText(SaveFailureOutOfSpaceText, substring = true).assertIsDisplayed()
    }

    @Test
    fun is_absent_when_not_visible() {
        composeRule.setContent {
            MaterialTheme { EditorSaveFailure(visible = false, onDismiss = {}, reduceMotion = true) }
        }
        composeRule.onNodeWithTag(EditorSaveFailureTestTag).assertDoesNotExist()
    }

    @Test
    fun tapping_dismiss_invokes_the_callback() {
        var dismissed = false
        composeRule.setContent {
            MaterialTheme {
                EditorSaveFailure(visible = true, onDismiss = { dismissed = true }, reduceMotion = true)
            }
        }
        composeRule.onNodeWithTag(SaveFailureDismissTag).performClick()
        composeRule.waitForIdle()
        assertTrue(dismissed)
    }
}
