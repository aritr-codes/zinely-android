package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.unit.Density
import com.aritr.zinely.core.data.repository.PaperColour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A55.8 and A56.2, the owner's "I do not want a scroller": at font scale 1 on a 360 × 640 dp phone the Start sheet and
 * the paper pile fit with nothing to scroll. At font scale 2 the pile scrolls, and its top row (the sample page and
 * Done) is pinned under the head, so Done stays on screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class PaperSheetFitTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun show(fontScale: Float = 1f, sheet: @Composable () -> Unit) {
        composeRule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                com.aritr.zinely.ui.theme.ZinelyTheme {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { sheet() }
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** Every scroll in the sheet, and how far it can scroll, in px. */
    private fun scrollRanges(): List<Float> =
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .fetchSemanticsNodes().map { it.config[SemanticsProperties.VerticalScrollAxisRange].maxValue() }

    @Test
    fun `given font scale 1 on a 360x640 phone, when the paper pile opens, then nothing scrolls`() {
        show { PaperColourSheetSurface(selected = PaperColour.PASTEL_YELLOW, onPick = {}, onDone = {}) }
        val ranges = scrollRanges()
        assertTrue("the pile has no scroll at all", ranges.isNotEmpty())
        assertEquals("the pile scrolls at font scale 1: $ranges", 0f, ranges.max(), 0.5f)
    }

    @Test
    fun `given font scale 1 on a 360x640 phone, when the Start sheet opens, then nothing scrolls`() {
        show { ShelfCreateSheetSurface(onChoosePaper = {}) }
        val ranges = scrollRanges()
        assertTrue("the sheet has no scroll at all", ranges.isNotEmpty())
        assertEquals("the Start sheet scrolls at font scale 1: $ranges", 0f, ranges.max(), 0.5f)
    }

    @Test
    fun `given font scale 2 on a 360x640 phone, when the pile scrolls to its last sheet, then Done is still on screen`() {
        show(fontScale = 2f) { PaperColourSheetSurface(selected = PaperColour.PASTEL_YELLOW, onPick = {}, onDone = {}) }
        assertTrue("the pile should scroll at font scale 2", scrollRanges().max() > 0f)
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .performScrollToNode(hasTestTag(paperColourOptionTestTag(PaperColour.GREY)))
        composeRule.waitForIdle()
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val done = composeRule.onNodeWithTag(PaperColourDoneTestTag).fetchSemanticsNode().boundsInRoot
        assertTrue("Done left the screen: $done in $root", done.top >= root.top && done.bottom <= root.bottom)
        val grey = composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.GREY)).fetchSemanticsNode().boundsInRoot
        assertTrue("the last sheet runs under Done", grey.top >= done.bottom)
    }
}
