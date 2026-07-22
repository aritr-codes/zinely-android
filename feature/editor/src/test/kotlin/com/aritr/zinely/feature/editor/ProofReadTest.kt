package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * **Act 0 — Read** ([ADR-058]): the Proof surface's landing shows the finished zine, one page per screen,
 * in reading order, with none of the printer's furniture. Robolectric NATIVE, the same tier as the other
 * Proof suites.
 *
 * The load-bearing assertions are about *what the surface answers*: a user who taps the editor's one
 * forward action must land on their zine, not on an imposition diagram; and the print climb must still be
 * reachable, unchanged, one tap away.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProofReadTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val pageSizePt = PtSize(200.0, 300.0)

    /** Eight pages in document order, each carrying a word so the render has something to draw. */
    private fun pages(count: Int = 8): List<Page> = (0 until count).map { i ->
        Page(
            index = i,
            role = if (i == 0) PageRole.FRONT_COVER else PageRole.INTERIOR,
            elements = listOf(
                TextElement(
                    id = "t$i",
                    transform = Transform(20.0, 20.0, 160.0, 40.0),
                    text = "page ${i + 1}",
                ),
            ),
        )
    }

    private fun setProof(pages: List<Page> = pages(), onBack: () -> Unit = {}) {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.size(420.dp, 820.dp)) {
                    ProofScreen(
                        zineName = "Corner Store Poems",
                        onBack = onBack,
                        pages = pages,
                        pageSizePt = pageSizePt,
                        defaults = DocumentDefaults(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `the proof surface lands on the zine, not on the imposed sheet`() {
        // The whole point of ADR-058. Before it, the single entry out of the editor opened on
        // "Step 1 of 3 · The sheet" — an imposition diagram with blank panels — which reads as lost work.
        setProof()

        composeRule.onNodeWithTag(ProofReadPagerTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofActLabelTestTag).assertTextEquals("Your zine")
        composeRule.onNodeWithTag(ProofReadCaptionTestTag).assertTextEquals("Page 1 of 8")
    }

    @Test
    fun `read shows the pages in reading order, starting at page one`() {
        setProof()

        // Document order IS reading order; imposition rearranges pages for the printer and stays in the
        // print acts. Page 1 is on screen; page 8 is not.
        composeRule.onNodeWithTag(proofReadPageTag(1)).assertExists()
        composeRule.onNodeWithTag(proofReadPageTag(8)).assertDoesNotExist()
    }

    @Test
    fun `a horizontal swipe turns the page and announces where you are`() {
        setProof()

        // A drag past the snap threshold, unhurried enough not to fling: `swipeLeft()`'s default is a
        // full-width flick, which carries two pages at once (it landed on page 3); a third of the width
        // does not clear the threshold and springs back to page 1. This is a page *turn* — far enough to
        // commit, slow enough not to throw.
        composeRule.onNodeWithTag(ProofReadPagerTestTag).performTouchInput {
            swipeLeft(startX = centerX, endX = centerX - width * 0.6f, durationMillis = 1000)
        }
        composeRule.waitForIdle()

        // The rendered page is a Canvas with no readable nodes, so the caption is the only thing that can
        // tell a screen-reader user which page they turned to — it is a polite live region for that reason.
        composeRule.onNodeWithTag(ProofReadCaptionTestTag).assertTextEquals("Page 2 of 8")
        composeRule.onNodeWithTag(proofReadPageTag(2)).assertExists()
    }

    @Test
    fun `read carries none of the printer's furniture`() {
        setProof()

        // No step numbering, no fold-guide copy, and no imposed-sheet language: Read answers "what did I
        // make", and every one of these answers "how is this imposed" instead.
        composeRule.onNodeWithText("Print setup").assertDoesNotExist()
        composeRule.onNodeWithTag(ProofSecondaryTestTag).assertDoesNotExist()
    }

    @Test
    fun `the print climb is one tap away and arrives unchanged`() {
        setProof()

        composeRule.onNodeWithText("Print & fold").performClick()
        composeRule.waitForIdle()

        // The frozen three-step climb, byte-for-byte the same surface it always was.
        composeRule.onNodeWithTag(ProofActLabelTestTag).assertTextEquals("Step 1 of 3 · The sheet")
        composeRule.onNodeWithText("Print setup").assertIsDisplayed()
    }

    @Test
    fun `back from the sheet returns to the zine rather than leaving the surface`() {
        var backs = 0
        setProof(onBack = { backs++ })

        composeRule.onNodeWithText("Print & fold").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ProofBackTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofActLabelTestTag).assertTextEquals("Your zine")
        org.junit.Assert.assertEquals("stepping back to Read is not leaving the surface", 0, backs)

        // …and from Read the same control is the loss-safe exit it has always been, and says so.
        composeRule.onNodeWithContentDescription("Back to the bench (your work is saved)").performClick()
        org.junit.Assert.assertEquals(1, backs)
    }

    @Test
    fun `an empty document renders no reader rather than crashing the pager`() {
        // Unreachable through any shipping path — every format seeds its pages — but a zero-page
        // HorizontalPager throws, so the guard is structural rather than defensive decoration.
        setProof(pages = emptyList())

        composeRule.onNodeWithTag(ProofReadPagerTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ProofScreenTestTag).assertIsDisplayed()
    }
}
