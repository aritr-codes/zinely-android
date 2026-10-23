package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.ink.InkHex
import com.aritr.zinely.core.editor.ink.InkLibrary
import com.aritr.zinely.core.editor.ink.InkPalette
import com.aritr.zinely.core.editor.ink.MyInk
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A40 rev. 2's tray order and grids (`v21-ink-mixer.html` `renderTray`; INK task 6), hosted standalone.
 * Positions are read unclipped (`positionInRoot`), so a section scrolled below the fold still has an order.
 */
@RunWith(RobolectricTestRunner::class)
class BenchInkTrayOrderTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun palette(id: Long, name: String, vararg hex: String) = InkPalette(id, name, hex.map(::InkHex))

    private fun library(palettes: Int, inks: Int) = InkLibrary(
        myInks = listOf("1F7A5C", "C0392B", "6B4E71", "8C1C13").take(inks).mapIndexed { i, h -> MyInk(InkHex(h), (inks - i).toLong()) },
        palettes = (1..palettes).map { palette(it.toLong(), "Palette $it", "2A251E", "E27F89") },
        clock = inks.toLong(),
        nextPaletteId = palettes + 1L,
        openPaletteId = if (palettes > 0) 1L else null,
    )

    private fun tray(library: InkLibrary?, kind: BenchVerbKind = BenchVerbKind.TEXT, fontScale: Float? = null) {
        composeRule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale ?: d.fontScale)) {
                ZinelyTheme {
                    Box(Modifier.size(411.dp, 891.dp)) {
                        BenchInkPopover(
                            visible = true,
                            kind = kind,
                            selected = null,
                            onPick = {},
                            onDone = {},
                            inkLibrary = library,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun top(text: String) =
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().positionInRoot.y

    private fun topOfTag(tag: String) = composeRule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y

    private fun count(tag: String) = composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    /** Distinct row tops among the nodes carrying [tag]. */
    private fun rows(tag: String) = composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
        .fetchSemanticsNodes().map { it.positionInRoot.y.toInt() }.distinct().size

    @Test
    fun `given two palettes and three inks when the tray opens on text then the sections run in the A40 order`() {
        tray(library(palettes = 2, inks = 3))
        val order = listOf(
            topOfTag(BenchInkCurrentTestTag),
            top(Copy.BenchInk.MY_PALETTES.uppercase()),
            top(Copy.BenchInk.MY_INKS.uppercase()),
            top(Copy.BenchInk.BUILT_IN_INKS.uppercase()),
            topOfTag(BenchInkStartersTestTag),
        )
        assertEquals("Current ink, My palettes, My inks, Built-in inks, Starter palettes", order.sorted(), order)
        assertTrue("strictly top to bottom: $order", order.zipWithNext().all { (a, b) -> a < b })
        assertEquals("starters closed", 0, count(BenchInkStarterTestTag))
        composeRule.onNodeWithTag(BenchInkStartersTestTag).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
        // The counts appear once there is something to count.
        composeRule.onNodeWithText(Copy.BenchInk.paletteCount(2), useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(Copy.BenchInk.inkCount(3), useUnmergedTree = true).assertExists()
        assertEquals("one open palette card", 1, count(BenchInkPaletteTestTag))
        assertEquals("a strip per palette", 2, count(BenchInkStripTestTag))
    }

    @Test
    fun `given an empty library then the empty paragraph and only the Mix pot show, with no counts`() {
        tray(InkLibrary.EMPTY)
        assertEquals(1, count(BenchInkPalettesEmptyTestTag))
        assertEquals(0, count(BenchInkPaletteTestTag))
        assertEquals(0, count(BenchInkStripTestTag))
        assertEquals(0, count(BenchInkKeptTestTag))
        assertEquals(1, count(BenchInkMixTestTag))
        assertEquals(
            "no `n of 9` anywhere",
            0,
            composeRule.onAllNodesWithText(Copy.BenchInk.inkCount(0), useUnmergedTree = true).fetchSemanticsNodes().size,
        )
        assertEquals(1, composeRule.onAllNodesWithContentDescription(Copy.BenchInk.MIX_AN_INK).fetchSemanticsNodes().size)
    }

    @Test
    fun `given a library still loading then neither My palettes nor My inks is drawn`() {
        tray(library = null)
        assertEquals(0, composeRule.onAllNodesWithText(Copy.BenchInk.MY_PALETTES.uppercase(), useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals(0, count(BenchInkMixTestTag))
        assertEquals(13, count(BenchInkSwatchTestTag))
    }

    @Test
    fun `given a text piece then thirteen built-ins and no D-083 spoken names`() {
        tray(InkLibrary.EMPTY)
        assertEquals(13, count(BenchInkSwatchTestTag))
        assertEquals(0, count(BenchInkTintSeparatorTestTag))
        listOf("Spot ink", "Neutral ink").forEach {
            assertEquals(it, 0, composeRule.onAllNodesWithContentDescription(it).fetchSemanticsNodes().size)
        }
    }

    @Test
    fun `given a decor piece then eighteen built-ins after a tint separator`() {
        tray(InkLibrary.EMPTY, kind = BenchVerbKind.DECOR)
        assertEquals(18, count(BenchInkSwatchTestTag))
        assertEquals(1, count(BenchInkTintSeparatorTestTag))
        val sep = topOfTag(BenchInkTintSeparatorTestTag)
        val tops = composeRule.onAllNodesWithTag(BenchInkSwatchTestTag, useUnmergedTree = true)
            .fetchSemanticsNodes().map { it.positionInRoot.y }
        assertTrue("the 13 built-ins sit above the separator", tops.take(13).all { it < sep })
        assertTrue("the 5 tints sit below it", tops.drop(13).all { it > sep })
    }

    @Test
    fun `given default text size then built-ins are seven across, My inks and strips five across`() {
        tray(library(palettes = 4, inks = 4))
        assertEquals("13 built-ins at 7", 2, rows(BenchInkSwatchTestTag))
        assertEquals("4 kept + Mix at 5", 1, rows(BenchInkKeptTestTag))
        assertEquals("Mix closes the same row", composeRule.onAllNodesWithTag(BenchInkKeptTestTag, useUnmergedTree = true).fetchSemanticsNodes()[0].positionInRoot.y, topOfTag(BenchInkMixTestTag), 1f)
        assertEquals("4 strips at 5", 1, rows(BenchInkStripTestTag))
    }

    @Test
    fun `given largest text then built-ins are five across, My inks and strips three across`() {
        tray(library(palettes = 4, inks = 4), fontScale = 1.3f)
        assertEquals("13 built-ins at 5", 3, rows(BenchInkSwatchTestTag))
        // 4 kept + Mix at 3: the kept inks fill row one and start row two, Mix closes row two.
        val keptRows = rows(BenchInkKeptTestTag)
        val mixTop = topOfTag(BenchInkMixTestTag)
        val keptTops = composeRule.onAllNodesWithTag(BenchInkKeptTestTag, useUnmergedTree = true)
            .fetchSemanticsNodes().map { it.positionInRoot.y }
        assertEquals(2, keptRows)
        assertEquals("Mix shares the second row", keptTops.last(), mixTop, 1f)
        assertEquals("4 strips at 3", 2, rows(BenchInkStripTestTag))
    }

}
