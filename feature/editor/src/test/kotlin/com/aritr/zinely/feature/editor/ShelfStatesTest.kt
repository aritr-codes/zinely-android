package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The Shelf's chrome and its four non-content states. */
@RunWith(RobolectricTestRunner::class)
class ShelfStatesTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * The frozen prototype gates its tools on a hand-flipped switch, not a count: its `few` shelf
     * holds 5 and hides them, its `many` shelf holds 21 and shows them. The owner's 7 must sit
     * inside that bracket, or the HTML would be contradicting it.
     */
    @Test
    fun `the tools threshold sits inside the bracket the frozen prototype leaves open`() {
        assertEquals(7, SHELF_TOOLS_THRESHOLD)
        assert(SHELF_TOOLS_THRESHOLD > 5) { "the spec's `few` shelf of 5 shows no tools" }
        assert(SHELF_TOOLS_THRESHOLD <= 21) { "the spec's `many` shelf of 21 shows them" }
    }

    @Test
    fun `the shelf head counts the objects standing under it`() {
        setContent { ShelfHead(count = 12) }
        composeRule.onNodeWithTag(ShelfHeadCountTestTag).assertTextEquals("12")
    }

    @Test
    fun `searching reports what was typed, and the field keeps a thumb-sized target`() {
        var typed = ""
        setContent { ShelfTools(query = typed, onQueryChange = { typed = it }, sortLabel = "Recent", onSortClick = {}) }

        composeRule.onNodeWithTag(ShelfSearchFieldTestTag)
            .assertHeightIsAtLeast(48.dp)
            .performTextInput("rain")
        assertEquals("rain", typed)
    }

    @Test
    fun `the sort button wears the current sort and asks for a new one`() {
        var asked = 0
        setContent { ShelfTools(query = "", onQueryChange = {}, sortLabel = "Oldest", onSortClick = { asked++ }) }

        composeRule.onNodeWithText("Oldest").assertExists()
        composeRule.onNodeWithTag(ShelfSortButtonTestTag).performClick()
        assertEquals(1, asked)
    }

    @Test
    fun `the dock starts a zine from a target a thumb can reach`() {
        var started = 0
        setContent { ShelfDock(onStart = { started++ }, wide = false) }

        composeRule.onNodeWithTag(ShelfStartButtonTestTag)
            .assertHeightIsAtLeast(56.dp)
            .performClick()
        assertEquals(1, started)
    }

    /** The empty shelf teaches the product; it never says "no results". */
    @Test
    fun `the empty state invites rather than reports`() {
        setContent { ShelfEmptyState() }
        composeRule.onNodeWithTag(HomeEmptyStateTestTag).assertExists()
        composeRule.onNodeWithText("Make your first little zine.").assertExists()
        composeRule.onNodeWithText("Kept on this device — no account, nothing uploaded").assertExists()
    }

    /** The shelf failed to open; the zines did not vanish, and the copy must not imply they did. */
    @Test
    fun `the error state promises the zines are safe and offers a way back`() {
        var retried = 0
        setContent { ShelfErrorState(onRetry = { retried++ }) }

        composeRule.onNodeWithText("Couldn't open your shelf").assertExists()
        composeRule
            .onNodeWithText("Your zines are safe on this device — we just couldn't read them this time.")
            .assertExists()
        composeRule.onNodeWithTag(ShelfRetryButtonTestTag).performClick()
        assertEquals(1, retried)
    }

    /** The skeleton claims nothing about objects that have not arrived: no text under it. */
    @Test
    fun `the loading skeleton announces itself once and says nothing else`() {
        setContent { ShelfLoadingSkeleton(columns = 2, roomy = false) }
        val node = composeRule.onNodeWithTag(HomeLoadingTestTag).fetchSemanticsNode()
        assertEquals(emptyList<Any>(), node.children)
    }

    @Test
    fun `a search that finds nothing says so quietly`() {
        setContent { ShelfSearchMiss() }
        composeRule.onNodeWithTag(ShelfSearchMissTestTag).assertTextEquals("Nothing here by that name.")
    }

    private fun setContent(content: @Composable () -> Unit) = composeRule.setContent {
        ZinelyTheme {
            // The sheet-hint scores its fold on a 340ms delay; reduced motion lands it immediately.
            CompositionLocalProvider(LocalZinelyMotion provides ZinelyMotion(reduceMotion = true)) {
                content()
            }
        }
    }
}
