package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **A loading shelf must not be indistinguishable from an empty one.**
 *
 * ### The defect this pins
 *
 * [ZineShelf] hides its heading while `placeholders > 0` — `alpha(0f)` plus `clearAndSetSemantics`,
 * transcribing the freeze's `visibility:hidden` — and every [ShelfPlaceholder] clears its own semantics
 * too. Both are right on their own, and together they left the loading state contributing **no
 * accessibility node whatsoever**. A sighted maker got a sweeping shimmer that says *"your shelf is
 * coming"*; a TalkBack user got a screen containing only *"Make a zine"* — **which is what the empty
 * state says**. The placeholders exist specifically to prevent that confusion, and they were creating it
 * for the one user who could not see them.
 *
 * Found by an independent review of the V2.1 Library slice, not by a test — there was no `library` a11y
 * test to find it. This file is the missing one.
 *
 * ### Why the control test is here
 *
 * `the loaded shelf does not claim to be loading` is not decoration. A fix that announced *"Loading your
 * zines"* unconditionally would satisfy the first test perfectly and be a worse bug than the one it
 * replaced. The pair only means something together.
 *
 * ⚠ **Scope, stated honestly.** These assertions read the **merged Compose semantics tree**, which is not
 * the platform `AccessibilityNodeInfo` tree TalkBack actually reads — the distinction that
 * [CLAUDE.md](../../../../../../../../CLAUDE.md#pass-1--developer-verification) exists to warn about, and
 * that let a control pass `assertIsNotEnabled` while telling the platform it was enabled. What this file
 * proves is that the node **exists and carries the right words**. Whether TalkBack *speaks* it belongs to
 * the device pass, and is owed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w480dp-h960dp", sdk = [28])
class ZineShelfLoadingA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val SHELF = "shelf-under-test"
        const val PLACEHOLDERS = 4
    }

    /** [ZineShelf] takes [ZineShelfItem], not `LibraryZine` — the shelf layer has no id. */
    private fun zine(title: String) = ZineShelfItem(
        title = title,
        recipe = ZineCoverRecipe(ZineCoverSurface.MatchaInk, ZineCoverStamp.Sun),
        subtitle = "A4 · today",
    )

    private fun shelf(zines: List<ZineShelfItem>, placeholders: Int) {
        composeRule.setContent {
            ZinelyTheme {
                ZineShelf(
                    zines = zines,
                    onOpen = {},
                    onActions = {},
                    modifier = Modifier.fillMaxSize().testTag(SHELF),
                    placeholders = placeholders,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `given a loading shelf, then something in the tree says it is loading`() {
        shelf(zines = emptyList(), placeholders = PLACEHOLDERS)

        composeRule.onNodeWithContentDescription(Copy.Shelf.LOADING_YOUR_ZINES).assertExists()
    }

    @Test
    fun `given a loading shelf, then it does not announce a count it cannot know`() {
        // The heading prints "N zines". During a slow read the list is empty, so an unhidden heading
        // announces "0 zines" to someone who has twelve — the exact failure the D-024 amendment added
        // placeholders to prevent. `clearAndSetSemantics` must drop the count, not merely dim it.
        shelf(zines = emptyList(), placeholders = PLACEHOLDERS)

        val tree = composeRule.onRoot().printToString(maxDepth = Int.MAX_VALUE)
        assertFalse(
            "a loading shelf must not announce a zine count:\n$tree",
            tree.contains(pluralZineCount(0)),
        )
    }

    @Test
    fun `given a loaded shelf, then it does not claim to be loading`() {
        // The control. Without it, announcing "Loading your zines" unconditionally would pass the first
        // test and be a worse defect than the silence it replaced.
        shelf(zines = listOf(zine("a"), zine("b")), placeholders = 0)

        val tree = composeRule.onRoot().printToString(maxDepth = Int.MAX_VALUE)
        assertFalse(
            "a shelf with zines on it must not say it is loading:\n$tree",
            tree.contains(Copy.Shelf.LOADING_YOUR_ZINES),
        )
    }

    @Test
    fun `given a loaded shelf, then the heading is spoken again`() {
        // The other half of the control: hiding the heading must be a state, not a one-way door.
        shelf(zines = listOf(zine("a"), zine("b")), placeholders = 0)

        val tree = composeRule.onRoot().printToString(maxDepth = Int.MAX_VALUE)
        assertTrue(
            "the count must come back when the data lands:\n$tree",
            tree.contains(pluralZineCount(2)),
        )
    }

    @Test
    fun `the two announcements are different sentences`() {
        // Guards the degenerate pass: if these two strings ever converged, every test above would stay
        // green while the shelf said one thing in both states.
        assertTrue(Copy.Shelf.LOADING_YOUR_ZINES != Copy.Shelf.YOUR_ZINES)
    }
}
