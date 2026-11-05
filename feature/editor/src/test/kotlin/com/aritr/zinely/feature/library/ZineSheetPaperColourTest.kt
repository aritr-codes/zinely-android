package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.data.repository.PaperColour
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** AW6.3a and AW6.7 on the zine sheet; A35.11's tile name. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class ZineSheetPaperColourTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the sheet line names a colour between size and date, and White adds nothing`() {
        assertEquals("A4 · Pastel yellow · 2 days ago", zineSheetLine("A4 · 2 days ago", PaperColour.PASTEL_YELLOW))
        assertEquals("A4 · 2 days ago", zineSheetLine("A4 · 2 days ago", PaperColour.WHITE))
        // A line with no size (an unavailable zine whose paper is unknown) still names the colour.
        assertEquals("Grey · Edited today", zineSheetLine("Edited today", PaperColour.GREY))
    }

    @Test
    fun `a tile says its paper only for US Legal and A5`() {
        assertEquals("Camping trip, US Legal", zineTileLabel("Camping trip", "${Copy.Paper.LEGAL} · 2 days ago"))
        assertEquals("Camping trip, A5", zineTileLabel("Camping trip", "${Copy.Paper.A5} · 2 days ago"))
        assertEquals("Camping trip", zineTileLabel("Camping trip", "${Copy.Paper.A4} · 2 days ago"))
        assertEquals("Camping trip", zineTileLabel("Camping trip", "${Copy.Paper.LETTER} · 2 days ago"))
    }

    private fun setSheet(colour: PaperColour) {
        composeRule.setContent {
            ZinelyTheme {
                ZineActionSheetSurface(target = ZineActionTarget("Sunday market", "A4 · 2 days ago", paperColour = colour), onAction = {})
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `the paper colour row sits after Move and before Delete, one button naming the colour`() {
        setSheet(PaperColour.PASTEL_YELLOW)

        val order = ZineAction.entries.map { it to composeRule.onNodeWithTag(zineActionTestTag(it)).fetchSemanticsNode().boundsInRoot.top }
        assertEquals("rows are drawn in their declared order", order.sortedBy { it.second }.map { it.first }, ZineAction.entries)
        assertEquals(ZineAction.Move.ordinal + 1, ZineAction.Paper.ordinal)
        assertTrue(ZineAction.Paper.ordinal < ZineAction.Delete.ordinal)

        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Paper)).assertContentDescriptionEquals("Paper colour, Pastel yellow")
        composeRule.onNodeWithTag(ZineActionSubtitleTestTag).assertTextEquals("A4 · Pastel yellow · 2 days ago")
    }

    @Test
    fun `share and export says the colour is not in the file, for a coloured paper only`() {
        setSheet(PaperColour.PASTEL_YELLOW)
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.ShareExport))
            .assertContentDescriptionEquals("${ZineAction.ShareExport.label}, ${Copy.PaperColour.NOT_IN_FILE}")
    }

    @Test
    fun `a White zine's sheet reads as before, plus the row`() {
        setSheet(PaperColour.WHITE)
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.ShareExport)).assertContentDescriptionEquals(ZineAction.ShareExport.label)
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Paper)).assertContentDescriptionEquals("Paper colour, White")
        composeRule.onNodeWithTag(ZineActionSubtitleTestTag).assertTextEquals("A4 · 2 days ago")
    }
}
