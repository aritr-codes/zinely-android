package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The frozen `.zine`: one object, two ways in, and a grid that widens rather than crowds. */
@RunWith(RobolectricTestRunner::class)
class ShelfCardTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val card = HomeZineCard(
        id = "z1",
        title = "Corner Store Poems",
        formatLabel = "A5 · 8 pages",
        editedLabel = "Edited today",
    )

    @Test
    fun `the shelf widens at the frozen breakpoints rather than crowding`() {
        assertEquals(2, shelfColumns(360.dp))
        assertEquals(2, shelfColumns(559.dp))
        assertEquals(3, shelfColumns(560.dp))
        assertEquals(3, shelfColumns(819.dp))
        assertEquals(4, shelfColumns(820.dp))
        assertEquals(4, shelfColumns(1179.dp))
        assertEquals(5, shelfColumns(1180.dp))
    }

    @Test
    fun `a tap opens the zine and a long-press asks for its actions`() {
        var opened = 0
        var actions = 0
        setCard(onOpen = { opened++ }, onActions = { actions++ })

        composeRule.onNodeWithTag(homeCardTestTag(card.id)).performClick()
        assertEquals(1, opened)
        assertEquals(0, actions)

        composeRule.onNodeWithTag(homeCardTestTag(card.id)).performTouchInput { longClick() }
        assertEquals(1, opened)
        assertEquals(1, actions)
    }

    @Test
    fun `the more button asks for actions from a target a finger can hit`() {
        var actions = 0
        setCard(onActions = { actions++ })

        composeRule.onNodeWithTag(homeCardMenuTestTag(card.id))
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(1, actions)
    }

    /** `<button class="cover">` is one node: the label already says the title, so the face is mute. */
    @Test
    fun `the cover announces itself once, not three times`() {
        setCard()
        val node = composeRule.onNodeWithTag(homeCardTestTag(card.id)).fetchSemanticsNode()
        assertEquals(
            listOf("Corner Store Poems, finished zine. Open on the bench."),
            node.config[SemanticsProperties.ContentDescription],
        )
        // The kicker, title and edition are cleared, so nothing under the button carries text.
        composeRule.onAllNodesWithText("Edited today").assertCountEquals(0)
        composeRule.onAllNodesWithText("A5 · 8 PAGES").assertCountEquals(0)
    }

    private fun setCard(onOpen: () -> Unit = {}, onActions: () -> Unit = {}) = setContent {
        ShelfCard(card = card, index = 0, onOpen = onOpen, onActions = onActions)
    }

    private fun setContent(content: @Composable () -> Unit) = composeRule.setContent {
        ZinelyTheme {
            // The settle is a delay-then-tween; it would otherwise gate every assertion below.
            CompositionLocalProvider(LocalZinelyMotion provides ZinelyMotion(reduceMotion = true)) {
                content()
            }
        }
    }
}
