package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * **The whole Proof surface on the smallest phone that ships, at the text sizes people actually set.**
 *
 * `ProofScreenTest` runs at 430x932 and font scale 1.0, which is the one configuration where everything
 * here fits. [ProofFoldNarrowTest] holds the 360dp *width*; this file holds the 640dp *height* and the
 * font scale, through the real [ProofScreen] — because the drawers' height bound and the band's growth
 * only exist there.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class ProofSmallPhoneTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val saved = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /**
     * Font scale comes from `@Config(fontScale = …)` on each test, not from a `LocalDensity` wrapper: the
     * drawers are `Dialog`-backed, a dialog takes its density from its own window, and a wrapper is
     * silently dropped at that boundary — the drawer tests would run at 1.0 whatever they were named.
     */
    private fun setProof(
        startDrawer: ProofDrawer = ProofDrawer.None,
        pageCount: Int = 8,
        busyTarget: ProofExportTarget? = null,
    ) {
        composeRule.setContent {
            ZinelyTheme {
                ProofScreen(
                    onBack = {},
                    busyTarget = busyTarget,
                    savedSignals = saved,
                    onOpenSavedPdf = {},
                    pages = (0 until pageCount).map { i ->
                        Page(index = i, role = if (i == 0) PageRole.FRONT_COVER else PageRole.INTERIOR)
                    },
                    pageSizePt = PtSize(200.0, 300.0),
                    startDrawer = startDrawer,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun save() {
        saved.tryEmit("zine.pdf")
        composeRule.waitForIdle()
    }

    private fun tagged(tag: String) = composeRule.onNodeWithTag(tag, useUnmergedTree = true)

    private fun heightDp(tag: String): Float {
        val node = tagged(tag).fetchSemanticsNode()
        return node.size.height / node.layoutInfo.density.density
    }

    /** Every pixel of the node is inside its window and inside every clip and scroll viewport above it. */
    private fun SemanticsNodeInteraction.assertWhollyVisible(what: String) {
        val node = fetchSemanticsNode()
        val shown = node.boundsInWindow
        assertTrue(
            "$what is cut off: ${shown.height}px of ${node.size.height}px tall and " +
                "${shown.width}px of ${node.size.width}px wide are showing",
            node.size.height > 0 &&
                shown.height >= node.size.height - 1f &&
                shown.width >= node.size.width - 1f,
        )
    }

    /**
     * Whole once scrolled to — "below the fold" is allowed and "cut" is not. The surface's scroller is
     * switched off while everything fits, so there is only something to scroll to when it does not.
     */
    private fun assertReachableWhole(tag: String, what: String) {
        val scrolls = generateSequence(tagged(tag).fetchSemanticsNode().parent) { it.parent }
            .any { hasScrollAction().matches(it) }
        if (scrolls) tagged(tag).performScrollTo()
        composeRule.waitForIdle()
        tagged(tag).assertWhollyVisible(what)
    }

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val out = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(out)
        return out.single()
    }

    private fun label(text: String) = composeRule.onNode(hasText(text), useUnmergedTree = true)

    /**
     * Words were lost: lines dropped past `maxLines`, or a line ending in an ellipsis.
     *
     * Not `hasVisualOverflow`: read back through semantics, that flag was also true for labels that had
     * all the room they needed, and this file briefly reported two defects that were not there.
     */
    private fun TextLayoutResult.dropsText(): Boolean =
        didOverflowHeight || (0 until lineCount).any { isLineEllipsized(it) }

    private fun TextLayoutResult.describe(): String =
        "$lineCount line(s), ${size.width}x${size.height}px, offered ${layoutInput.constraints.maxWidth}px wide"

    // ---- P1: the fold guide's sentence is above the fold ------------------------------------------

    /**
     * The Fold drawer answers *"how do I turn this into a booklet?"*, and the sentence is the answer. It
     * is the last thing in the step's scroll column, which is taller than the room a 640dp phone leaves —
     * and anchored at its top, that put the sentence below the fold with nothing to say it was there.
     */
    @Test
    fun `the fold step's sentence and its holding line are on screen without scrolling`() {
        setProof(startDrawer = ProofDrawer.Fold)

        // The holding line is the last thing in the sentence block, so it stands for the whole of it.
        tagged(ProofStepHoldingTestTag).assertWhollyVisible("step 1's holding line")
        tagged(ProofFoldDiagramTestTag).assertWhollyVisible("step 1's diagram")
    }

    /**
     * With text at twice its size the step cannot fit and has to scroll. It opens at its top, in reading
     * order, with the sentence it ends on reachable; opened at its end it began in the middle of a
     * sentence (seen on a 360 x 640 emulator, 2026-10-13).
     */
    @Test
    @Config(fontScale = 2f)
    fun `at font scale 2 the fold step opens at its top and scrolls to its sentence`() {
        setProof(startDrawer = ProofDrawer.Fold)

        tagged(ProofStepTitleTestTag).assertWhollyVisible("the step counter")
        tagged(ProofStepNextTestTag).assertWhollyVisible("Next step")

        tagged(ProofStepHoldingTestTag).performScrollTo()
        composeRule.waitForIdle()
        tagged(ProofStepHoldingTestTag).assertWhollyVisible("the holding line, once scrolled to")
    }

    /** A step left scrolled to its end must not leave the next one opening there. */
    @Test
    @Config(fontScale = 2f)
    fun `at font scale 2 the next fold step opens at its own top`() {
        setProof(startDrawer = ProofDrawer.Fold)
        tagged(ProofStepHoldingTestTag).performScrollTo()
        composeRule.waitForIdle()

        tagged(ProofStepNextTestTag).performClick()
        composeRule.waitForIdle()

        tagged(ProofStepTitleTestTag).assertWhollyVisible("step 2's counter")
    }

    /**
     * The first fix anchored the step at its bottom, which showed the sentence and hid the counter and
     * the key above the fold with nothing to say they were there. Now the diagram gives way instead, so
     * on this phone at ordinary text a step shows all four of its parts and does not scroll at all.
     */
    @Test
    fun `every part of a fold step is on screen at once, first step and last`() {
        setProof(startDrawer = ProofDrawer.Fold)

        fun assertWholeStep(which: String) {
            tagged(ProofStepTitleTestTag).assertWhollyVisible("$which step's counter")
            tagged(ProofFoldLegendTestTag).assertWhollyVisible("$which step's key")
            tagged(ProofFoldDiagramTestTag).assertWhollyVisible("$which step's diagram")
            tagged(ProofStepHoldingTestTag).assertWhollyVisible("$which step's holding line")
            assertTrue(
                "$which step's diagram is too small to read: ${heightDp(ProofFoldDiagramTestTag)}dp",
                heightDp(ProofFoldDiagramTestTag) >= 100f,
            )
        }
        assertWholeStep("the first")

        tagged(proofStepDotTag(FOLD_LAST_STEP)).performClick()
        composeRule.waitForIdle()
        assertWholeStep("the last")
        tagged(ProofPrimaryTestTag).assertWhollyVisible("the finish button")
    }

    /** The last step is the tight one: the finish button takes another row out of the same drawer. */
    @Test
    fun `the last fold step still shows its sentence, its dots and the finish button`() {
        setProof(startDrawer = ProofDrawer.Fold)

        tagged(proofStepDotTag(FOLD_LAST_STEP)).performClick()
        composeRule.waitForIdle()

        tagged(ProofStepHoldingTestTag).assertWhollyVisible("the last step's holding line")
        tagged(ProofStepDotsTestTag).assertWhollyVisible("the step dots")
        tagged(ProofPrimaryTestTag).assertWhollyVisible("the finish button")
    }

    /** However tall the sentence grows, it is the step that scrolls — never the controls that go. */
    @Test
    @Config(fontScale = 2f)
    fun `the fold controls stay whole at font scale 2 on the last step`() {
        setProof(startDrawer = ProofDrawer.Fold)

        tagged(proofStepDotTag(FOLD_LAST_STEP)).performClick()
        composeRule.waitForIdle()

        tagged(ProofStepPrevTestTag).assertWhollyVisible("Previous step")
        tagged(ProofStepDotsTestTag).assertWhollyVisible("the step dots")
        tagged(ProofPrimaryTestTag).assertWhollyVisible("the finish button")
    }

    // ---- P2: the finished zine survives the band growing ------------------------------------------

    private fun assertZineKeepsItsFloor() {
        val book = heightDp(ProofReadBookTestTag)
        assertTrue("the finished zine has ${book}dp left once the save lands", book >= 159f)
    }

    /**
     * This screen answers *"what have I made?"*, so the zine is the one thing the band may not push off
     * it. The band grows when a save lands and again with the font scale, and the reader used to be
     * whatever was left: 89dp of this window, which is what a 640dp-tall phone gives the app once its own
     * bars are taken out.
     *
     * The floor is paid for by scrolling, and the save is what makes the band tall — so the save also
     * has to bring its own hand-off into view, and the way back up has to stay reachable.
     */
    @Test
    @Config(qualifiers = "w360dp-h568dp-xhdpi", fontScale = 2f)
    fun `in a 568dp window at font scale 2 the zine keeps its floor and a save lands on its hand-off`() {
        setProof()
        save()

        assertZineKeepsItsFloor()
        tagged(ProofFoldItUpTestTag).assertWhollyVisible("Fold it up, as the save lands")
        tagged(ProofOpenPdfTestTag).assertWhollyVisible("Open PDF, as the save lands")
        assertTrue("Fold it up is squeezed", heightDp(ProofFoldItUpTestTag) >= 52f)
        assertReachableWhole(ProofBackTestTag, "Back")
    }

    /**
     * Was a pin that a 360x640 window at font scale 2 shows everything with nothing to scroll. A43.3
     * (D-142) adds Share to the done row, and the frozen 2× specimen wraps it: Fold it up takes a line of
     * its own, the band grows by that line, and the save scrolls the bar out of view — as the 568dp case
     * above already does, and as A44's note accepts ("on the phone the screen scrolls"). What must hold is
     * what that case asserts: the zine keeps its floor, the hand-off lands in view, Back stays reachable.
     */
    @Test
    @Config(fontScale = 2f)
    fun `a 640dp window at font scale 2 keeps the zine and lands a save on its whole hand-off`() {
        setProof()
        save()

        assertZineKeepsItsFloor()
        tagged(ProofFoldItUpTestTag).assertWhollyVisible("Fold it up")
        tagged(ProofOpenPdfTestTag).assertWhollyVisible("Open PDF")
        tagged(ProofShareTestTag).assertWhollyVisible("Share")
        assertReachableWhole(ProofBackTestTag, "Back")
    }

    /** A pin as well: the scroll is the fallback, not the layout — at the default size nothing moves. */
    @Test
    fun `before a save the reader still takes everything the band leaves`() {
        setProof()

        tagged(ProofSavePdfTestTag).assertWhollyVisible("Save PDF")
        tagged(ProofBackTestTag).assertWhollyVisible("Back")
        assertTrue("the reader shrank to ${heightDp(ProofReadBookTestTag)}dp", heightDp(ProofReadBookTestTag) > 300f)
    }

    /**
     * A document with no pages has no reader, and the band has always sat directly under the bar there —
     * which is the arrangement `ProofScaffoldGoldenTest` records. The floor is for a reader; it must not
     * open a gap where there is none.
     */
    @Test
    fun `with no pages the band still sits directly under the top bar`() {
        setProof(pageCount = 0)

        val bar = tagged(ProofBackTestTag).fetchSemanticsNode()
        val ready = tagged(ProofReadyTestTag).fetchSemanticsNode()
        val gapDp = (ready.positionInWindow.y - (bar.positionInWindow.y + bar.size.height)) /
            bar.layoutInfo.density.density
        assertTrue("there are ${gapDp}dp between the top bar and the band", gapDp < 40f)
    }

    // ---- P3: the band's buttons keep their words ---------------------------------------------------

    private fun assertLabelWhole(text: String) {
        val layout = label(text).textLayout()
        assertFalse("\"$text\" is cut short inside its button: ${layout.describe()}", layout.dropsText())
    }

    /**
     * `maxLines = 1` with no ellipsis: a label that did not fit was simply cut off. "Open PDF" went at
     * 1.5 and "Save PDF" at 2.0 — the two actions this band exists to offer.
     */
    private fun assertBandLabelsWhole() {
        setProof()
        assertLabelWhole(Copy.Proof.SAVE_PDF)
        assertLabelWhole(Copy.Proof.SHARE)

        save()
        assertLabelWhole(Copy.Proof.OPEN_PDF)
        assertLabelWhole(Copy.Proof.FOLD_IT_UP)
    }

    @Test
    @Config(fontScale = 1.5f)
    fun `the band's buttons show their whole labels at font scale 1_5`() = assertBandLabelsWhole()

    @Test
    @Config(fontScale = 2f)
    fun `the band's buttons show their whole labels at font scale 2`() = assertBandLabelsWhole()

    /**
     * The tight case: Share is not weighted, so its longer busy label is measured first and Save PDF
     * gets what is left.
     */
    @Test
    @Config(fontScale = 2f)
    fun `the band's buttons show their whole labels while a share is being prepared at font scale 2`() {
        setProof(busyTarget = ProofExportTarget.SEND)

        assertLabelWhole(Copy.Proof.PREPARING_SHARE)
        assertLabelWhole(Copy.Proof.SAVE_PDF)
    }

    /**
     * One label wrapping used to leave its neighbour a shorter pill. Side by side, the pair is one height.
     */
    @Test
    @Config(fontScale = 1.4f)
    fun `two buttons side by side are the same height at font scale 1_4`() {
        // While a share is prepared its label is the long one, and Save PDF wraps in what is left: the
        // case where the two labels are not the same number of lines.
        setProof(busyTarget = ProofExportTarget.SEND)
        assertTrue(
            "the case needs one label to wrap and the other not",
            label(Copy.Proof.SAVE_PDF).textLayout().lineCount != label(Copy.Proof.PREPARING_SHARE).textLayout().lineCount,
        )
        assertEquals(heightDp(ProofSavePdfTestTag), heightDp(ProofShareTestTag), 0.5f)
    }

    @Test
    @Config(fontScale = 1.4f)
    fun `the hand-off pair is the same height at font scale 1_4`() {
        setProof()
        save()
        // A43.3: the done row wraps, so "same height" is per line. Open PDF and Share share the first one.
        assertEquals(heightDp(ProofOpenPdfTestTag), heightDp(ProofShareTestTag), 0.5f)
    }

    /** From 1.5 the pair stacks: each button the full width, so no label is broken inside a word. */
    @Test
    @Config(fontScale = 2f)
    fun `at font scale 2 the two buttons stack and each keeps its label on one line`() {
        setProof(busyTarget = ProofExportTarget.SEND)

        val savePdf = tagged(ProofSavePdfTestTag).fetchSemanticsNode().boundsInRoot
        val share = tagged(ProofShareTestTag).fetchSemanticsNode().boundsInRoot
        assertTrue("Share sits under Save PDF (save $savePdf, share $share)", share.top >= savePdf.bottom)
        assertEquals("and is as wide", savePdf.width, share.width, 1f)
        assertEquals(1, label(Copy.Proof.SAVE_PDF).textLayout().lineCount)
        assertEquals(1, label(Copy.Proof.PREPARING_SHARE).textLayout().lineCount)
        // …and the taller band did not push the top of the screen away.
        tagged(ProofBackTestTag).assertWhollyVisible("Back")
    }

    // ---- P4: the honesty legend wraps --------------------------------------------------------------

    /**
     * The legend was a `Row`, so its last phrase got whatever width the first two left: four lines at
     * 1.5 and twenty-two — a column one letter wide — at 2.0.
     */
    private fun assertLegendReadsAsPhrases(fontScale: Float) {
        setProof(startDrawer = ProofDrawer.Details)

        listOf(
            Copy.ProofSheet.LEGEND_FOLD_LINES,
            Copy.ProofSheet.LEGEND_ONE_CUT,
            Copy.ProofSheet.LEGEND_PRINTER_REACH,
        ).forEach { phrase ->
            assertEquals(
                "\"$phrase\" was squeezed onto several lines at font scale $fontScale",
                1,
                label(phrase).textLayout().lineCount,
            )
        }
    }

    @Test
    @Config(fontScale = 1.5f)
    fun `the honesty legend keeps each phrase on one line at font scale 1_5`() =
        assertLegendReadsAsPhrases(1.5f)

    @Test
    @Config(fontScale = 2f)
    fun `the honesty legend keeps each phrase on one line at font scale 2`() =
        assertLegendReadsAsPhrases(2f)

    // ---- P5: the page ticket keeps its count -------------------------------------------------------

    /**
     * "Back cover · 8 of 8" is the longest thing the ticket says, and on one line it ellipsised — losing
     * the count, which is the half that is the readout. (It fits at 1.3; 2.0 is where it gave way.)
     */
    private fun assertBackCoverTicketWhole(fontScale: Float) {
        setProof()

        val backCover = Copy.ProofRead.leafLabel(8, 8)
        val ticket = tagged(ProofPageTicketTestTag)
        repeat(8) {
            val now = ticket.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)
            if (now?.single() != backCover) {
                tagged(ProofReadNextTestTag).performClick()
                composeRule.waitForIdle()
            }
        }
        assertEquals(
            listOf(backCover),
            ticket.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription),
        )

        val layout = ticket.textLayout()
        assertFalse(
            "the ticket dropped part of \"$backCover\" at font scale $fontScale: ${layout.describe()}",
            layout.dropsText(),
        )
        tagged(ProofFoldOpenTestTag).assertWhollyVisible("How to fold")
    }

    @Test
    @Config(fontScale = 2f)
    fun `the back cover's ticket keeps its count at font scale 2`() = assertBackCoverTicketWhole(2f)
}
