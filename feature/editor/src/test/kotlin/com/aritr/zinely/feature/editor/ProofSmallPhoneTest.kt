package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
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
    private fun setProof(startDrawer: ProofDrawer = ProofDrawer.None) {
        composeRule.setContent {
            ZinelyTheme {
                ProofScreen(
                    onBack = {},
                    savedSignals = saved,
                    onOpenSavedPdf = {},
                    pages = (0 until 8).map { i ->
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

    /** Whole once scrolled to — the surface scrolls, so "below the fold" is allowed and "cut" is not. */
    private fun assertReachableWhole(tag: String, what: String) {
        tagged(tag).performScrollTo()
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
        // …and it did not buy its place with the picture: what waits above the fold is the counter and
        // the legend.
        tagged(ProofFoldDiagramTestTag).assertWhollyVisible("step 1's diagram")
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

    /**
     * This screen answers *"what have I made?"*, so the zine is the one thing the band may not push off
     * it. The band grows when a save lands and again with the font scale, and the reader used to be
     * whatever was left.
     */
    private fun assertZineSurvivesTheSave() {
        setProof()
        save()

        val book = heightDp(ProofReadBookTestTag)
        assertTrue("the finished zine has ${book}dp left once the save lands", book >= 159f)
        // The floor is paid for by scrolling, so the actions it pushes down must still be reachable.
        assertReachableWhole(ProofFoldItUpTestTag, "Fold it up")
        assertReachableWhole(ProofOpenPdfTestTag, "Open PDF")
        assertTrue("Fold it up is squeezed", heightDp(ProofFoldItUpTestTag) >= 52f)
    }

    @Test
    @Config(fontScale = 2f)
    fun `the zine keeps its place after a save at font scale 2`() = assertZineSurvivesTheSave()

    /**
     * A phone that is 640dp tall gives the app nearer 568dp once its own bars are taken out — and there
     * the reader was down to 89dp.
     */
    @Test
    @Config(qualifiers = "w360dp-h568dp-xhdpi", fontScale = 2f)
    fun `the zine keeps its place after a save in a 568dp window at font scale 2`() =
        assertZineSurvivesTheSave()

    /** The scroll is the fallback, not the layout: at the default size nothing moves. */
    @Test
    fun `before a save the reader still takes everything the band leaves`() {
        setProof()

        tagged(ProofSavePdfTestTag).assertWhollyVisible("Save PDF")
        tagged(ProofBackTestTag).assertWhollyVisible("Back")
        assertTrue("the reader shrank to ${heightDp(ProofReadBookTestTag)}dp", heightDp(ProofReadBookTestTag) > 300f)
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
