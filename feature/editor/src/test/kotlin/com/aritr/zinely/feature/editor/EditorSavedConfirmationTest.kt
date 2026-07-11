package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import com.aritr.zinely.ui.theme.ZinelyTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The transient **"Saved ✨"** autosave confirmation ([EditorSavedConfirmation]) — a quiet, non-blocking
 * reassurance that work saves automatically (VOICE §3 "Success", DESIGN-LANGUAGE §10). These prove the
 * *static* state is always correct and motion-independent: the line is real text, present whenever the
 * chip is visible, in both the animated and the reduced-motion path (motion is decoration on top of an
 * already-correct static state — DESIGN-LANGUAGE §7/§10). The host wiring (trigger + auto-dismiss) is
 * proven in [EditorScreenTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorSavedConfirmationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun renders_the_saved_line_when_visible_with_motion() {
        composeRule.setContent {
            ZinelyTheme { EditorSavedConfirmation(visible = true, reduceMotion = false) }
        }
        composeRule.onNodeWithTag(EditorSavedConfirmationTestTag).assertIsDisplayed()
    }

    @Test
    fun renders_the_saved_line_when_visible_with_reduced_motion() {
        // Reduced-motion degrades the transition, never the content: the static state is identical, so the
        // confirmation still shows the line with no animation to depend on.
        composeRule.setContent {
            ZinelyTheme { EditorSavedConfirmation(visible = true, reduceMotion = true) }
        }
        composeRule.onNodeWithTag(EditorSavedConfirmationTestTag).assertIsDisplayed()
    }

    @Test
    fun is_absent_when_not_visible() {
        composeRule.setContent {
            ZinelyTheme { EditorSavedConfirmation(visible = false, reduceMotion = true) }
        }
        composeRule.onNodeWithTag(EditorSavedConfirmationTestTag).assertDoesNotExist()
    }
}
