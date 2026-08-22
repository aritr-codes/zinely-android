package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.render.SupplyCatalog
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Art sheet's structure — one grid, sixteen tiles, four headings, and nothing that filters.
 *
 * The paint lives in [BenchArtSheetGoldenTest] and the announcement lives in
 * [com.aritr.zinely.feature.editor.a11y.BenchArtSheetPlatformA11yTest]; what is asserted here is the shape
 * the A5 ruling is actually about.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h812dp-xhdpi")
class BenchArtSheetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var picked = mutableListOf<String>()

    private fun render(recent: List<String> = emptyList()) {
        composeRule.setContent {
            ZinelyTheme {
                BenchArtSheet(
                    visible = true,
                    onDismiss = {},
                    onPick = { picked += it },
                    recent = recent,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun renderBody(fontScale: Float) {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                ZinelyTheme { BenchArtSheetBody(onPick = {}) }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun all_sixteen_supplies_are_on_the_one_surface() {
        render()
        // §9's claim, asserted rather than assumed: the whole cabinet is here, not a filtered quarter of it.
        // `assertExists`, not `assertIsDisplayed` — §9 also claims all sixteen fit on one screen, and A5's
        // ~25px of slack is box arithmetic the amendment itself marks as corroborating rather than
        // load-bearing. Turning it into a Robolectric assertion would state on this harness's authority a
        // thing only a device pass can settle.
        for (id in Copy.Supplies.NAMES.keys) {
            composeRule.onNodeWithTag(benchArtTileTestTag(id)).assertExists()
        }
        assertEquals(16, Copy.Supplies.NAMES.size)
    }

    @Test
    fun the_four_families_head_their_own_sections() {
        render()
        for (family in Copy.Supplies.BY_FAMILY.keys) {
            composeRule.onNodeWithTag(benchArtLabelTestTag(family)).assertExists()
        }
        assertEquals(4, Copy.Supplies.BY_FAMILY.size)
    }

    @Test
    fun no_recent_row_is_drawn_until_a_caller_supplies_recents() {
        render()
        // §9 defers favourites and recents. The row's structure exists; its content is not invented — the
        // frozen prototype's two hard-coded tiles are demo data, not a specification of what a first-time
        // maker has recently used.
        composeRule.onNodeWithTag(benchArtLabelTestTag(BenchArtRecentHeading)).assertDoesNotExist()
    }

    @Test
    fun a_recent_supply_is_shelved_twice_and_each_shelf_addresses_its_own_tile() {
        render(recent = listOf("shape.circle"))
        // The frozen prototype shelves `SUP[8]` and `SUP[6]` under Recent *and* under their families; a
        // cabinet's "in front" row is a second placement, not a move. Both nodes must exist and be
        // separately addressable — one tag on two nodes can address neither.
        composeRule.onNodeWithTag(benchArtRecentTileTestTag("shape.circle")).assertExists()
        composeRule.onNodeWithTag(benchArtTileTestTag("shape.circle")).assertExists()
    }

    @Test
    fun the_recent_row_appears_above_the_families_when_there_are_recents() {
        render(recent = listOf("shape.circle"))
        val recentTop = composeRule.onNodeWithTag(benchArtLabelTestTag(BenchArtRecentHeading))
            .fetchSemanticsNode().boundsInRoot.top
        val firstFamilyTop = composeRule
            .onNodeWithTag(benchArtLabelTestTag(Copy.Supplies.BY_FAMILY.keys.first()))
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue("Recent must shelve above the families", recentTop < firstFamilyTop)
    }

    @Test
    fun picking_an_authored_supply_reports_its_id() {
        render()
        composeRule.onNodeWithTag(benchArtTileTestTag("shape.circle")).performClick()
        assertEquals(listOf("shape.circle"), picked)
    }

    @Test
    fun a_supply_with_no_authored_outline_cannot_be_picked() {
        render()
        // The twelve are drawn — the freeze says sixteen — but picking one would place an element that
        // paints nothing. `performClick` on a node with no click action is a no-op, so the assertion is
        // that nothing was reported, not that the call threw.
        composeRule.onNodeWithTag(benchArtTileTestTag("tape.torn")).performClick()
        assertTrue("an unauthored supply must not reach onPick", picked.isEmpty())
    }

    @Test
    fun exactly_the_authored_tiles_are_pickable_and_the_catalogue_is_what_decides() {
        // ⚠ This test used to say in its comment that it was *"not a hard-coded list of four"* and then
        // assert a hard-coded list of four — against `SupplyCatalog` directly, never touching the sheet.
        // It therefore tested the catalogue's contents (which `SupplyCatalogTest` already owns) while
        // claiming to test the sheet's wiring, and it failed the moment it predicted it should not: the
        // day the catalogue grew. Rewritten to do what its name says.
        //
        // Every tile is clicked, and the assertion is the *correspondence* — pickable exactly when
        // authored — so it holds at 4 of 16, at 12 of 16, and at 16 of 16 without an edit.
        render()
        for (id in Copy.Supplies.NAMES.keys) {
            picked.clear()
            composeRule.onNodeWithTag(benchArtTileTestTag(id)).performClick()
            val authored = SupplyCatalog.outlineOf(id) != null
            assertEquals(
                if (authored) {
                    "$id has an authored outline and its tile must report the pick"
                } else {
                    "$id paints nothing, so its tile must not reach onPick — a placed element with no " +
                        "pixels reads as the app losing the maker's action"
                },
                if (authored) listOf(id) else emptyList(),
                picked.toList(),
            )
        }
        // Non-vacuity: the loop above proves nothing unless it saw both branches.
        assertTrue(
            "the sheet must contain both an authored and an unauthored tile for this to assert anything",
            Copy.Supplies.NAMES.keys.any { SupplyCatalog.outlineOf(it) != null } &&
                Copy.Supplies.NAMES.keys.any { SupplyCatalog.outlineOf(it) == null },
        )
    }

    /**
     * D-103, reproduced on SM-A176B at font scale 1.8: the former visible sentence wrapped
     * `AVAILABL` / `E` inside a word. Read the composable's own [TextLayoutResult], rather than measuring a
     * second copy of its style, and also reject the tempting word-boundary fix that merely clips the word.
     * The fuller explanation remains the tile's spoken state in
     * [com.aritr.zinely.feature.editor.a11y.BenchArtSheetPlatformA11yTest].
     */
    @Test
    fun the_compact_unavailable_label_never_breaks_a_word_or_overflows_at_font_scale_1_8() {
        renderBody(fontScale = 1.8f)
        val text = Copy.BenchArt.NOT_YET.uppercase().replace(' ', '\n')
        assertTrue("the fixture must contain a real word boundary", text.any(Char::isWhitespace))

        val labelNode = composeRule.onNodeWithTag(benchArtNotYetTestTag("tape.torn"), useUnmergedTree = true)
        val layouts = mutableListOf<TextLayoutResult>()
        labelNode.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
        val layout = layouts.single()
        val labelBounds = labelNode.fetchSemanticsNode().boundsInRoot
        val tileBounds = composeRule.onNodeWithTag(benchArtTileTestTag("tape.torn"))
            .fetchSemanticsNode().boundsInRoot

        assertTrue(
            "the label node must remain inside its clipped tile; label=$labelBounds tile=$tileBounds",
            labelBounds.left >= tileBounds.left && labelBounds.right <= tileBounds.right &&
                labelBounds.top >= tileBounds.top && labelBounds.bottom <= tileBounds.bottom,
        )
        assertTrue(
            "every laid-out line must fit the tile's visible width",
            (0 until layout.lineCount).all { line ->
                layout.getLineRight(line) - layout.getLineLeft(line) <= tileBounds.width
            },
        )
        assertTrue(
            "the label must not overflow vertically",
            !layout.didOverflowHeight,
        )
        assertTrue(
            "wrapping may use word boundaries, never create more lines than there are words",
            layout.lineCount <= text.split(Regex("\\s+")).size,
        )
        for (line in 0 until layout.lineCount - 1) {
            val end = layout.getLineEnd(line, visibleEnd = true)
            assertTrue(
                "line ${line + 1} ends inside a word at character $end in '$text'",
                end < text.length && text[end].isWhitespace(),
            )
            assertTrue("line ${line + 1} must not be ellipsized", !layout.isLineEllipsized(line))
        }
        val last = layout.lineCount - 1
        assertTrue("the final line must not be ellipsized", !layout.isLineEllipsized(last))
        assertEquals(
            "every character must be laid out; no clipping or truncation is an acceptable substitute",
            text.length,
            layout.getLineEnd(last, visibleEnd = false),
        )
    }
}
