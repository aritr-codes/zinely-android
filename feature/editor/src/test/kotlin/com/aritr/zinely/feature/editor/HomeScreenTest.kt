package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
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
 * The S6.2 Home · "My zines" READ-ONLY shelf (docs/design/SCREEN-INVENTORY.md#home--my-zines,
 * ADR-043): renders the cards it is given (ordering is the repository/VM contract, not the
 * screen's), taps open a zine, and — the ADR-042 hard invariant — **no mutation affordance
 * exists anywhere**, including on the empty shelf (its CTA arrives with the S6.3 create action).
 * Robolectric NATIVE to match the sibling screen tests.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()

    private val twoZines = listOf(
        HomeZineCard(
            id = "zine-b",
            title = "Summer scraps",
            formatLabel = "8-page mini · Letter",
            editedLabel = "Edited just now",
        ),
        HomeZineCard(
            id = "zine-a",
            title = "Cat facts",
            formatLabel = "8-page mini · A4",
            editedLabel = "Edited 3 days ago",
        ),
    )

    private fun setHome(cards: List<HomeZineCard>, loading: Boolean = false) {
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    loading = loading,
                    cards = cards,
                    onOpenZine = { opened += it },
                )
            }
        }
    }

    @Test
    fun `shelf shows every zine with its title, format, and recency`() {
        setHome(twoZines)

        composeRule.onNodeWithTag(HomeShelfTestTag).assertExists()
        composeRule.onNodeWithText("Summer scraps").assertExists()
        composeRule.onNodeWithText("8-page mini · Letter").assertExists()
        composeRule.onNodeWithText("Edited just now").assertExists()
        composeRule.onNodeWithText("Cat facts").assertExists()
        composeRule.onNodeWithText("8-page mini · A4").assertExists()
        composeRule.onNodeWithText("Edited 3 days ago").assertExists()
    }

    @Test
    fun `tapping a zine opens exactly that project`() {
        setHome(twoZines)
        opened.clear()

        composeRule.onNodeWithTag(homeCardTestTag("zine-a")).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("zine-a"), opened)
    }

    @Test
    fun `the only tappable things on the shelf are the zines themselves`() {
        // The ADR-042 NO-mutation-UI invariant, checked structurally: exactly one click
        // target per card — open — and nothing else (no create/duplicate/delete/rename).
        setHome(twoZines)

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(twoZines.size)
        composeRule.onNodeWithText("Start a zine").assertDoesNotExist()
        composeRule.onNodeWithText("Delete").assertDoesNotExist()
        composeRule.onNodeWithText("Duplicate").assertDoesNotExist()
        composeRule.onNodeWithText("Rename").assertDoesNotExist()
    }

    @Test
    fun `the empty shelf is a warm invitation with no actions at all`() {
        setHome(emptyList())

        composeRule.onNodeWithTag(HomeEmptyStateTestTag).assertExists()
        composeRule.onNodeWithText(HomeEmptyHeadline).assertExists()
        // Reassurance, not a void — and the privacy promise stays visible.
        composeRule.onNodeWithText("works offline · stays on your phone").assertExists()
        // Invitation only in S6.2: the Start-a-zine CTA ships with the create action (S6.3).
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
        composeRule.onNodeWithText("Start a zine").assertDoesNotExist()
    }

    @Test
    fun `loading is a quiet spinner, never a flash of the empty invitation`() {
        setHome(emptyList(), loading = true)

        composeRule.onNodeWithTag(HomeLoadingTestTag).assertExists()
        composeRule.onNodeWithTag(HomeEmptyStateTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(HomeShelfTestTag).assertDoesNotExist()
    }
}
