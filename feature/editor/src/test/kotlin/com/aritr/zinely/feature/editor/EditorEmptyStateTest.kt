package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The first-run empty state (docs/design/DESIGN-LANGUAGE.md §8/§9 · [ADR-033](../DECISIONS.md#adr-033)):
 * a **pure invitation** — warm copy + stickers + the privacy line — that points to the supply tray below.
 * It owns NO add actions: the always-visible [EditorSupplyTray] is the single, thumb-zone home for
 * "Add a photo" / "Add words", so the two actions never appear twice at once (DESIGN-RULES 3, 7). This
 * suite proves the overlay is invitation-only; the host assembly (overlay + tray) is proven in
 * [EditorScreenTest]. Robolectric NATIVE, same tier as [EditorPageStripTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorEmptyStateTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun shows_the_invitation_copy() {
        composeRule.setContent {
            ZinelyTheme { EditorEmptyState() }
        }
        composeRule.onNodeWithTag(EditorEmptyStateTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Let's make something cute", substring = true).assertIsDisplayed()
    }

    @Test
    fun owns_no_add_actions_the_tray_does() {
        // ADR-033 de-dup: the invitation must NOT carry its own "Add a photo" / "Add words" controls —
        // those live solely in the supply tray. Rendered standalone (no tray), neither label appears.
        composeRule.setContent {
            ZinelyTheme { EditorEmptyState() }
        }
        composeRule.onNodeWithText(AddPhotoActionLabel, substring = true).assertDoesNotExist()
        composeRule.onNodeWithText(AddWordsActionLabel, substring = true).assertDoesNotExist()
    }

    @Test
    fun first_blank_page_shows_the_welcoming_line() {
        // VOICE empty states: the first page keeps the warm welcome.
        composeRule.setContent {
            ZinelyTheme { EditorEmptyState(firstPage = true) }
        }
        composeRule.onNodeWithText(FirstPageInvitationHeadline, substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(LaterPageInvitationHeadline, substring = true).assertDoesNotExist()
    }

    @Test
    fun later_blank_page_shows_the_fresh_page_line() {
        // VOICE empty states: a later blank page uses the lighter "fresh page" variant (same
        // invitation-only rule — the tray still owns the add actions).
        composeRule.setContent {
            ZinelyTheme { EditorEmptyState(firstPage = false) }
        }
        composeRule.onNodeWithText(LaterPageInvitationHeadline, substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(FirstPageInvitationHeadline, substring = true).assertDoesNotExist()
    }

    @Test
    fun shows_a_decorative_cue_toward_the_supplies_below() {
        // Orientation polish (ADR-033 follow-up): a subtle cue ties the invitation to the supply shelf
        // below. It lives inside the overlay, so it's present exactly when the blank-page overlay is, and
        // it is decorative — cleared from the a11y tree so it adds no screen-reader noise (the tray's
        // "Supplies" heading carries the spoken orientation instead).
        composeRule.setContent {
            ZinelyTheme { EditorEmptyState() }
        }
        composeRule.onNodeWithTag(EmptyStateTrayCueTag).assertExists()
        // Decorative-with-a-job (DESIGN-RULES 10): the glyph must be cleared from the a11y tree, so it
        // is never announced — proves silence, not just that the tag is queryable (Codex review).
        composeRule.onNodeWithText("⌄", substring = true).assertDoesNotExist()
    }
}
