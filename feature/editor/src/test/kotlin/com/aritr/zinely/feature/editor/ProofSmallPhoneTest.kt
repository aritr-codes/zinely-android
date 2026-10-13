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
}
