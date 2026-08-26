package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
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

        composeRule.onNodeWithTag(ProofReadBookTestTag).assertIsDisplayed()
        // The readout speaks the booklet: the first leaf is the cover, not "page 1 of 8".
        composeRule.onNodeWithTag(ProofPageTicketTestTag).assertContentDescriptionEquals("Cover · 1 of 8")
    }

    @Test
    fun `read shows the pages in reading order, starting at page one`() {
        setProof()

        // Document order IS reading order; imposition rearranges pages for the printer and stays in the
        // print acts. Page 1 is on screen; page 8 is not.
        composeRule.onNodeWithTag(proofReadPageTag(1)).assertExists()
        composeRule.onNodeWithTag(proofReadPageTag(8)).assertDoesNotExist()
    }

    /**
     * **Tapping the right edge changes one leaf immediately, and says so** — ADR-101 P5's whole gesture
     * change plus the frozen stakeholder P3 amendment.
     *
     * The retired assertion here drove a `HorizontalPager` with a 60%-width drag, tuned between a flick
     * that carried two pages and a nudge that sprang back. That tuning was itself the tell: a page turn
     * should not have a threshold to calibrate. The frozen `.tapz` is a button.
     */
    @Test
    fun `tapping the forward edge changes one leaf immediately and announces where you are`() {
        setProof()

        composeRule.onNodeWithTag(ProofReadNextTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofPageTicketTestTag).assertContentDescriptionEquals("Page 2 of 8")
        composeRule.onNodeWithTag(proofReadPageTag(2)).assertExists()
        composeRule.onNodeWithTag(proofReadPageTag(1)).assertDoesNotExist()
    }

    /**
     * **A drag turns a leaf too** — the finding that made the first P5 build a NO-GO.
     *
     * The booklet model never required the swipe to be deleted; it required the tap to be *added*. Every
     * reading surface on the phone turns on a drag, so a reader that ignores one makes its single most
     * probable first input a silent no-op, and a silent no-op on arrival is indistinguishable from a frozen
     * screen. Read cold, in that order: *"I tapped my zine and it didn't do anything."*
     */
    @Test
    fun `a horizontal drag turns a leaf, in both directions`() {
        setProof()

        composeRule.onNodeWithTag(ProofReadBookTestTag).performTouchInput {
            swipeLeft(startX = centerX + 120f, endX = centerX - 120f, durationMillis = 300)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ProofPageTicketTestTag).assertContentDescriptionEquals("Page 2 of 8")

        composeRule.onNodeWithTag(ProofReadBookTestTag).performTouchInput {
            swipeRight(startX = centerX - 120f, endX = centerX + 120f, durationMillis = 300)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ProofPageTicketTestTag).assertContentDescriptionEquals("Cover · 1 of 8")
    }

    /**
     * **The readout must not contradict the band one inch below it.**
     *
     * Named only — *Cover · Page 2 … Page 7 · Back cover* — the reader emits no "Page 1" and no "Page 8",
     * directly above `.ready` reading *"8 pages · one sheet, one cut"*. The honest cold reading is *"page 1
     * is missing"*, on the one screen whose whole job is to settle whether the zine came out whole. The
     * freeze escapes this by also stamping the number on the leaf (`.pgn`); Compose will not print
     * furniture over the user's artwork, so the number lives here.
     */
    @Test
    fun `the readout names the leaf and still agrees with the page count`() {
        setProof()

        composeRule.onNodeWithTag(ProofPageTicketTestTag).assertContentDescriptionEquals("Cover · 1 of 8")
        repeat(7) {
            composeRule.onNodeWithTag(ProofReadNextTestTag).performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag(ProofPageTicketTestTag).assertContentDescriptionEquals("Back cover · 8 of 8")
    }

    /**
     * **The ends are dead ends, and the control admits it.** A tap that is accepted and changes nothing is
     * how a screen teaches a user it is broken, so both edges disable rather than clamp.
     */
    @Test
    fun `the turn edges disable at the two ends of the book`() {
        setProof()

        composeRule.onNodeWithTag(ProofReadPrevTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(ProofReadNextTestTag).assertIsEnabled()

        repeat(7) {
            composeRule.onNodeWithTag(ProofReadNextTestTag).performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithTag(ProofPageTicketTestTag).assertContentDescriptionEquals("Back cover · 8 of 8")
        composeRule.onNodeWithTag(ProofReadNextTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(ProofReadPrevTestTag).assertIsEnabled()
    }

    /**
     * **The spine lands on the physically correct side** —
     * [V21-SPEC §5.2](../../../../../../../docs/design/V21-SPEC.md), and the reason the reader was rewritten
     * rather than re-skinned.
     *
     * The stack edge is the visible half of that claim: it is the closed remainder of the booklet, so it
     * exists only where a leaf faces another one. The cover and the back cover are free single sheets —
     * they are the outside of the object, and an outside has nothing behind it.
     */
    @Test
    fun `the cover and back cover are free sheets, and the interior leaves are bound`() {
        setProof()

        composeRule.onNodeWithTag(ProofReadStackTestTag, useUnmergedTree = true).assertDoesNotExist()

        composeRule.onNodeWithTag(ProofReadNextTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ProofReadStackTestTag, useUnmergedTree = true).assertExists()

        repeat(6) {
            composeRule.onNodeWithTag(ProofReadNextTestTag).performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag(ProofPageTicketTestTag).assertContentDescriptionEquals("Back cover · 8 of 8")
        composeRule.onNodeWithTag(ProofReadStackTestTag, useUnmergedTree = true).assertDoesNotExist()
    }

    /**
     * **A restored reader shows the leaf you were on, without a transition or intermediate cover.**
     * The stakeholder P3 amendment removed page animation entirely; retaining this immediate assertion
     * protects both restoration and that no-performance rule.
     */
    @Test
    fun `a restored reader is already on the leaf it was on, with no turn to play`() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            ZinelyTheme {
                Box(Modifier.size(420.dp, 820.dp)) {
                    ProofScreen(
                        onBack = {},
                        pages = pages(),
                        pageSizePt = pageSizePt,
                        defaults = DocumentDefaults(),
                    )
                }
            }
        }
        repeat(3) {
            composeRule.onNodeWithTag(ProofReadNextTestTag).performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag(ProofPageTicketTestTag).assertContentDescriptionEquals("Page 4 of 8")

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag(proofReadPageTag(4)).assertExists()
        composeRule.onNodeWithTag(proofReadPageTag(1)).assertDoesNotExist()
        composeRule.onNodeWithTag(ProofPageTicketTestTag).assertContentDescriptionEquals("Page 4 of 8")
    }

    @Test
    fun `read carries none of the printer's furniture`() {
        setProof()

        // No step numbering, no fold-guide copy, and no imposed-sheet language: Read answers "what did I
        // make", and every one of these answers "how is this imposed" instead.
        composeRule.onNodeWithText("Print setup").assertDoesNotExist()
    }

    /**
     * Printing is one tap away — but it is now a **drawer over** the reader rather than the first step of
     * a climb that replaced it ([ADR-101](../../../../../../../docs/DECISIONS.md#adr-101) P1). The
     * distinction is the whole point of the restructure: the finished zine never leaves the screen.
     */
    @Test
    fun `printing is one tap away, and the reader stays behind it`() {
        setProof()

        composeRule.onNodeWithTag(ProofReadyTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofPaperSegmentsTestTag).assertIsDisplayed()
        // The reader is still mounted underneath — the drawer covers it, it does not replace it.
        composeRule.onNodeWithTag(ProofReadBookTestTag).assertExists()
    }

    /**
     * The loss-safe exit, simplified by the restructure. It used to mean two different things depending on
     * which act you stood in — from the Sheet it stepped back to Read, everywhere else it left. With the
     * climb gone there is one meaning left, and the control says exactly it.
     */
    @Test
    fun `the top-bar back is the one loss-safe exit, and says so`() {
        var backs = 0
        setProof(onBack = { backs++ })


        composeRule.onNodeWithContentDescription("Back to the bench (your work is saved)").performClick()
        org.junit.Assert.assertEquals(1, backs)
    }

    @Test
    fun `an empty document renders no reader rather than crashing the pager`() {
        // Unreachable through any shipping path — every format seeds its pages — but a zero-page
        // HorizontalPager throws, so the guard is structural rather than defensive decoration.
        setProof(pages = emptyList())

        composeRule.onNodeWithTag(ProofReadBookTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ProofScreenTestTag).assertIsDisplayed()
    }
}
