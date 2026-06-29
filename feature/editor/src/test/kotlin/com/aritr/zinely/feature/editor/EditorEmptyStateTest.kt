package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The first-run empty state (docs/design/DESIGN-LANGUAGE.md §8/§9): the two supplies must be VISIBLE
 * controls (not hidden gestures) and fire the hoisted add-photo / add-words actions. The host wires
 * those to `Intent.RequestAddImage` / `Intent.PlaceText`; here we assert the discoverable buttons
 * invoke them. Robolectric NATIVE, same tier as [EditorPageStripTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorEmptyStateTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun shows_the_invitation_and_both_supplies() {
        composeRule.setContent {
            MaterialTheme { EditorEmptyState(onAddPhoto = {}, onAddText = {}) }
        }
        composeRule.onNodeWithTag(EditorEmptyStateTestTag).assertIsDisplayed()
        composeRule.onNodeWithText(AddPhotoActionLabel, substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(AddWordsActionLabel, substring = true).assertIsDisplayed()
    }

    @Test
    fun tapping_a_supply_fires_its_action() {
        var photo = 0
        var words = 0
        composeRule.setContent {
            MaterialTheme { EditorEmptyState(onAddPhoto = { photo++ }, onAddText = { words++ }) }
        }

        composeRule.onNodeWithText(AddPhotoActionLabel, substring = true).performClick()
        composeRule.onNodeWithText(AddWordsActionLabel, substring = true).performClick()
        composeRule.waitForIdle()

        assertEquals(1, photo)
        assertEquals(1, words)
    }
}
