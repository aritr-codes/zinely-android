package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.down
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyMakerInkId
import com.aritr.zinely.ui.theme.ZinelyPaperTintId
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.zinelyContentInks
import com.aritr.zinely.ui.theme.zinelyV2DarkColors
import com.aritr.zinely.ui.theme.zinelyV2LightColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config

/**
 * C6 — the ink popover, H4, the maker palette
 * ([ADR-096](../../../../../../../docs/DECISIONS.md#adr-096) rows 6.1–6.18).
 *
 * The rows this suite deliberately does **not** close are named rather than quietly skipped:
 *
 * | row | where it is closed instead | why not here |
 * |---|---|---|
 * | 6.1b/6.1c the card's fill, hairline, radius and shadow | [BenchC6GoldenTest] | they are paint, and a raster assertion is the honest instrument for paint |
 * | 6.5b the 1px `--desk-edge` halo, 6.6 the `.sel` ring's own pixels | [BenchC6GoldenTest] | both are drawn *outside* the layout bounds, so no semantics node can measure them |
 * | 6.12c the shield's `stroke-width:1.7` | [BenchC6GoldenTest] | a stroke weight is a count of dark pixels, which is exactly what C5 row 5.2a had to split out |
 * | 6.15's **platform** clause | the mandatory device passes | `Role.RadioButton`'s announcement rides the platform tree, not the merged semantics tree (CI-26) |
 *
 * Rows 6.13c and 6.13d are **framework-delivered** and asserted as such below rather than left implicit:
 * the frozen `applyInk` writes `$('editSw').style.background` and calls `flashSaved()`, and both are
 * already consequences of the one `Intent.StyleText` this surface dispatches.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// The frozen `.phone` is 411x891dp, which is the device this programme measures on and the size the
// amendment log quotes. Robolectric's default surface is 320x470dp, and on it the fourth band and the
// `.inkuse` note fall outside the window - four assertions failed on that alone before this line
// existed. Recorded rather than worked around: on a 320dp-tall canvas this popover genuinely does not
// fit, which is a real observation for Device Pass 1 and not something a qualifier makes untrue.
@Config(qualifiers = "w411dp-h891dp")
class BenchC6Test {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(100.0, 130.0)
    private val host: Pair<Dp, Dp> = 360.dp to 720.dp
    private val inks = zinelyContentInks()

    private fun store(pageCount: Int = 1): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        return EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = (0 until pageCount).map { Page(index = it, role = PageRole.INTERIOR) },
                ),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
    }

    private fun setScreen(store: EditorStore) {
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(
                    store = store,
                    pageSizePt = pageSizePt,
                    modifier = Modifier.size(host.first, host.second),
                )
            }
        }
        composeRule.waitForIdle()
    }

    /** Places one text box, leaving it selected — the reducer auto-selects a placement. */
    private fun placedText(store: EditorStore): String {
        store.dispatch(Intent.PlaceText(Transform(20.0, 60.0, 60.0, 18.0), "hi"))
        return store.uiState.value.selection.single()
    }

    /**
     * Places a text box and opens the popover on it, which is the only route the freeze gives: `.inkpop`
     * has no rest state and `openInk` is its single `add('show')`.
     *
     * The verb is reached through the context bar's own subtree rather than by the word `Ink` alone —
     * the popover's `h4` carries the same word, so a bare text match would be ambiguous the moment the
     * thing under test appears.
     */
    private fun openInk(store: EditorStore): String {
        val id = placedText(store)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.INK}").performClick()
        composeRule.waitForIdle()
        return id
    }

    private fun bounds(tag: String) =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun touchBounds(tag: String) =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().touchBoundsInRoot

    private fun px(dp: Dp) = with(composeRule.density) { dp.toPx() }

    /** Summed absolute per-channel difference between two ARGB pixels — the C4 bar's own instrument. */
    private fun channelDistance(a: Int, b: Int) = listOf(0, 8, 16).sumOf { s ->
        kotlin.math.abs(((a shr s) and 0xFF) - ((b shr s) and 0xFF))
    }

    private fun rootBounds() = composeRule.onRoot().fetchSemanticsNode().boundsInRoot

    private fun count(tag: String) =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun swatchBounds(index: Int) =
        composeRule.onAllNodesWithTag(BenchInkSwatchTestTag, useUnmergedTree = true)
            .fetchSemanticsNodes()[index].boundsInRoot

    private fun textOf(store: EditorStore, id: String) =
        store.uiState.value.document.pages[store.uiState.value.currentPageIndex]
            .elements.first { it.id == id } as TextElement

    // =================================================================================================
    // The fence — OD-24, and the whole of what the owner ruled (rows 6.7, 6.8, 6.9)
    // =================================================================================================

    /**
     * Row 6.9 — **the ruling itself.** A text element is offered `Inks` and `Neutrals`, in that order,
     * and **no paper tints**.
     *
     * Asserted as the exact label list rather than as "does not contain tints": a `!contains` passes on
     * an empty popover, and it would still pass if a fourth band appeared. The list is the contract.
     */
    @Test
    fun a_text_element_is_offered_inks_and_neutrals_and_no_paper_tints() {
        assertEquals(
            listOf(Copy.BenchInk.INKS, Copy.BenchInk.NEUTRALS),
            benchInkBands(inks, BenchVerbKind.TEXT).map { it.label },
        )
    }

    /**
     * Row 6.9b — the fence is a **fence**, not a deletion, which is
     * [OD-21](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-047-ruling)'s own distinction carried
     * into OD-24. Any other target still gets all three bands, so the day a paper target exists the band
     * is already there rather than needing to be re-derived from a deleted list.
     */
    @Test
    fun any_other_ink_target_still_gets_all_three_frozen_bands() {
        listOf(BenchVerbKind.PHOTO, BenchVerbKind.DECOR).forEach { kind ->
            assertEquals(
                "bands for $kind",
                listOf(Copy.BenchInk.INKS, Copy.BenchInk.PAPER_TINTS, Copy.BenchInk.NEUTRALS),
                benchInkBands(inks, kind).map { it.label },
            )
        }
    }

    /**
     * Row 6.9c — the fenced band's *values* are untouched. This is the assertion that would fail if a
     * later reader "cleaned up" `paperTints` because nothing rendered it: the palette is the frozen
     * source's, and only its offer to a text target is withheld.
     */
    @Test
    fun the_paper_tints_survive_the_fence_in_full() {
        assertEquals(
            listOf(
                Copy.BenchInk.CREAM to Color(0xFFF1E9D6),
                Copy.BenchInk.BLUSH to Color(0xFFF0DED9),
                Copy.BenchInk.SKY to Color(0xFFDDE9EE),
                Copy.BenchInk.SAGE to Color(0xFFE1E9D2),
                Copy.BenchInk.KRAFT to Color(0xFFE4D3B4),
            ),
            benchInkBands(inks, BenchVerbKind.PHOTO)[1].swatches.map { it.name to it.value },
        )
    }

    /**
     * Row 6.7 — band 1, `INKS`, in the frozen order, by name **and** value.
     *
     * Both, because either alone is a half-assertion: names alone survive every colour being wrong, and
     * values alone survive `Aqua` being labelled `Plum`.
     */
    @Test
    fun the_inks_band_is_the_frozen_ten_in_the_frozen_order() {
        assertEquals(
            listOf(
                Copy.BenchInk.MATCHA to Color(0xFF7C8A3F),
                Copy.BenchInk.FOREST to Color(0xFF3E5E3A),
                Copy.BenchInk.STRAWBERRY to Color(0xFFE27F89),
                Copy.BenchInk.BRICK to Color(0xFFB0503F),
                Copy.BenchInk.SUNFLOWER to Color(0xFFE7B53C),
                Copy.BenchInk.OCHRE to Color(0xFFD19A3C),
                Copy.BenchInk.AQUA to Color(0xFF57B0A9),
                Copy.BenchInk.CORNFLOWER to Color(0xFF6E86C9),
                Copy.BenchInk.PLUM to Color(0xFF8A5A9B),
                Copy.BenchInk.INK to Color(0xFF2A251E),
            ),
            benchInkBands(inks, BenchVerbKind.TEXT)[0].swatches.map { it.name to it.value },
        )
    }

    /** Row 6.8 — band 3, `NEUT`. `Ink` repeats band 1's, verbatim from the frozen source. */
    @Test
    fun the_neutrals_band_is_the_frozen_four_in_the_frozen_order() {
        assertEquals(
            listOf(
                Copy.BenchInk.INK to Color(0xFF2A251E),
                Copy.BenchInk.SLATE to Color(0xFF5B5347),
                Copy.BenchInk.STONE to Color(0xFF8C8269),
                Copy.BenchInk.FOG to Color(0xFFB7AD93),
            ),
            benchInkBands(inks, BenchVerbKind.TEXT)[1].swatches.map { it.name to it.value },
        )
    }

    // =================================================================================================
    // The presets (rows 6.10, 6.11)
    // =================================================================================================

    /** Row 6.10 — the three frozen recipes, by name and by every dot, in order. */
    @Test
    fun the_three_frozen_recipes_are_transcribed_dot_for_dot() {
        assertEquals(
            listOf(
                Copy.BenchInk.PRESET_TWO_COLOUR to listOf(Color(0xFF2A251E), Color(0xFFE27F89)),
                Copy.BenchInk.PRESET_WARM to
                    listOf(Color(0xFFB0503F), Color(0xFFE7B53C), Color(0xFFF1E9D6)),
                Copy.BenchInk.PRESET_COOL to
                    listOf(Color(0xFF3E5E3A), Color(0xFF57B0A9), Color(0xFFDDE9EE)),
            ),
            benchInkPresets(inks).map { p -> p.name to p.dots.map { it.value } },
        )
    }

    /**
     * Row 6.11 — **OD-24's second half**: a preset applies its *first* colour.
     *
     * The counter-values are asserted too. Without them the test passes on an implementation that
     * happens to return the first element of a one-element list, and the whole point of the ruling is
     * which of *three* is taken.
     */
    @Test
    fun a_preset_applies_its_primary_ink_and_never_its_accent() {
        val applied = benchInkPresets(inks).map { it.applied.value }
        assertEquals(listOf(Color(0xFF2A251E), Color(0xFFB0503F), Color(0xFF3E5E3A)), applied)
        val accents = listOf(Color(0xFFE27F89), Color(0xFFE7B53C), Color(0xFF57B0A9))
        applied.forEachIndexed { i, c ->
            assertTrue("preset $i must not apply its accent", c != accents[i])
        }
    }

    /**
     * Row 6.11b — the recipes are built **from the typed palette**, not from re-typed hexes, so a recipe
     * cannot drift from the band it is made of. Two of the three end in a paper *tint* by value, which is
     * the evidence that decoded `[ink, accent, paper]` in the first place — asserted so that reading
     * survives in the suite rather than only in prose.
     */
    @Test
    fun two_of_the_three_recipes_end_in_a_paper_tint() {
        val presets = benchInkPresets(inks)
        assertEquals(inks[ZinelyPaperTintId.Cream].value, presets[1].dots.last().value)
        assertEquals(inks[ZinelyPaperTintId.Sky].value, presets[2].dots.last().value)
        assertEquals(inks[ZinelyMakerInkId.Ink].value, presets[0].applied.value)
    }

    // =================================================================================================
    // The `.inkuse` count (row 6.12)
    // =================================================================================================

    /**
     * Row 6.12 — the count is **live**, where the prototype hard-codes 2, and it counts the whole zine
     * rather than the open page, because "print cheapest" is a per-job cost.
     */
    @Test
    fun the_ink_note_counts_the_distinct_inks_of_the_whole_zine() {
        val a = ColorRgba(0x2A, 0x25, 0x1E)
        val b = ColorRgba(0xB0, 0x50, 0x3F)
        // The second ink lives ONLY on page 2. An earlier form of this fixture put both inks on page 1,
        // which made the assertion pass identically whether the count spanned the zine or stopped at the
        // open page — the battery caught it as a GREEN mutation (M25), not the suite.
        val pages = listOf(
            Page(index = 0, role = PageRole.INTERIOR, elements = listOf(text("1", a))),
            // The same ink again on another page must not count twice; a third page adds nothing.
            Page(index = 1, role = PageRole.INTERIOR, elements = listOf(text("2", b), text("3", a))),
            Page(index = 2, role = PageRole.INTERIOR),
        )
        assertEquals(2, benchInkCount(pages))
        assertEquals(0, benchInkCount(listOf(Page(index = 0, role = PageRole.INTERIOR))))
    }

    private fun text(id: String, color: ColorRgba) = TextElement(
        id = id,
        transform = Transform(0.0, 0.0, 10.0, 10.0),
        text = "x",
        style = TextStyle(color = color),
    )

    /**
     * Row 6.17b — the colour round trip is exact, which is why OD-24 needed no migration and why the
     * selection ring can be derived by equality rather than by a tolerance.
     */
    @Test
    fun a_swatch_survives_the_document_round_trip_byte_for_byte() {
        (benchInkBands(inks, BenchVerbKind.TEXT).flatMap { it.swatches } + benchInkPresets(inks).map { it.applied })
            .forEach { assertEquals(it.name, it.value, it.value.toColorRgba().toComposeColor()) }
    }

    // =================================================================================================
    // The card (rows 6.1-6.3)
    // =================================================================================================

    /**
     * Row 6.1a — summoned, never default. Nothing is composed until the verb asks for it.
     *
     * ONE store, held in a local. `store()` is a factory, and a first form of this test called it twice —
     * so the text was placed on a store the screen never observed, nothing was ever selected, and the
     * popover was absent for a reason that had nothing to do with the row. The selection is asserted
     * here rather than assumed, so the fixture cannot silently stop exercising the thing under test.
     */
    @Test
    fun the_popover_does_not_exist_until_the_ink_verb_asks_for_it() {
        val store = store()
        setScreen(store)
        placedText(store)
        composeRule.waitForIdle()
        // The element IS selected and its verb bar IS up — the popover's every precondition but the tap.
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.INK}").assertIsDisplayed()
        assertEquals(0, count(BenchInkPopoverTestTag))
    }

    /** Row 6.1 — the frozen `left:12px;right:12px;bottom:12px` against the canvas. */
    @Test
    fun the_popover_sits_at_the_frozen_twelve_dp_inset() {
        val store = store()
        setScreen(store)
        placedText(store)
        composeRule.waitForIdle()
        val bar = bounds(BenchContextBarTestTag)
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.INK}").performClick()
        composeRule.waitForIdle()
        val pop = bounds(BenchInkPopoverTestTag)
        val root = rootBounds()
        // Horizontally the canvas spans the window, so the root is a truthful anchor for these two.
        assertEquals("left", px(12.dp), pop.left - root.left, 1f)
        assertEquals("right", px(12.dp), root.right - pop.right, 1f)
        // `bottom:12px` is anchored to the CANVAS, which is shorter than the window — measured against
        // the root it reads 182px, not 12, and a first form of this test simply omitted it. The honest
        // anchor is the control the popover replaces: the verb bar carries the same frozen three
        // offsets, so "the same bottom edge as the bar" is the freeze's own statement of this property.
        assertEquals("bottom", bar.bottom, pop.bottom, 1f)
    }

    /**
     * Row 6.1f — the popover **replaces** the verb bar rather than stacking on it, which is the freeze's
     * own `ctx.classList.remove('show')`. Both directions are asserted: a swap that only ever hides is
     * a one-way trip, and `Done` has to bring the bar back.
     */
    @Test
    fun the_popover_takes_the_verb_bars_place_and_gives_it_back() {
        val store = store()
        setScreen(store)
        openInk(store)
        assertEquals("the bar stands down", 0, count(BenchContextBarTestTag))
        composeRule.onNodeWithTag(BenchInkDoneTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals("the popover stands down", 0, count(BenchInkPopoverTestTag))
        assertEquals("the bar comes back", 1, count(BenchContextBarTestTag))
    }

    /**
     * Row 6.1e — `.inkpop{transform:translateY(14px)}` at rest, released over `.22s var(--standard)`.
     *
     * Measured mid-flight with the clock held, against the resting position taken after it lands: the
     * offset is a *fixed* 14px in the freeze, not a fraction of the card's own height, so a card that
     * slid its full height would pass any "it moves" assertion and fail this one.
     */
    @Test
    fun the_popover_rises_fourteen_dp_into_place() {
        val store = store()
        setScreen(store)
        placedText(store)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.INK}").performClick()
        // Frame by frame until it is on screen rather than by a guessed delay — at progress exactly 0
        // `AnimatedVisibility` composes nothing, so which frame is the first readable one is an
        // implementation detail this test should not encode (C4 row 4.11 learned the same thing).
        repeat(6) {
            if (count(BenchInkPopoverTestTag) == 0) {
                composeRule.mainClock.advanceTimeByFrame()
                composeRule.waitForIdle()
            }
        }
        val entering = bounds(BenchInkPopoverTestTag)
        composeRule.mainClock.advanceTimeBy(BenchInkPopoverEnterMillis + 200L)
        composeRule.waitForIdle()
        val resting = bounds(BenchInkPopoverTestTag)
        val rise = entering.top - resting.top
        assertTrue(
            "the card must enter from below its resting place: entering=$entering resting=$resting",
            rise > px(7.dp),
        )
        assertTrue("…and by no more than the frozen 14dp: $rise", rise <= px(14.dp) + 0.5f)
        // The measured bound is a range, because the first readable frame is already part-way through
        // the ease and pinning it would encode a frame number. The frozen value itself is exact, so it
        // is asserted as a value as well — 10dp or 8dp would satisfy the range above and neither is 14.
        assertEquals("`translateY(14px)` is the frozen offset", 14.dp, BenchInkPopoverEnterOffsetDp)
        assertEquals("`.22s` is the frozen duration", 220, BenchInkPopoverEnterMillis)
    }

    /** Row 6.2 — the header carries the frozen title and the frozen dismiss, and both are reachable. */
    @Test
    fun the_header_is_the_frozen_title_and_its_done() {
        val store = store()
        setScreen(store)
        openInk(store)
        composeRule.onNodeWithTag(BenchInkPopoverTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(BenchInkDoneTestTag).assertIsDisplayed()
        // Literals, both of them. A first form asserted `TITLE == BenchVerbs.INK`, which is how `Copy.kt`
        // DEFINES `TITLE` — `x == x`, and blind to the header word changing.
        assertEquals(Copy.BenchInk.DONE, "Done")
        assertEquals(Copy.BenchInk.TITLE, "Ink")
        composeRule.onNodeWithText("Ink").assertIsDisplayed()
    }

    /**
     * Row 6.2c — `justify-content:space-between`: `Done` sits at the card's far edge, not against the
     * title.
     *
     * **Device Pass 1 found this and nothing else could have.** The header `Row` had no `fillMaxWidth`,
     * so `Arrangement.SpaceBetween` had no free space to distribute and the header read "InkDone" —
     * measured on an `SM-A176B` at 411dp, title glyphs ending at x=128px with `Done` at 132..203px where
     * the frozen header puts it near x=1007. Every existing assertion passed: both texts were present,
     * both displayed, both the right colour. The golden passed too, because it was recorded from this
     * layout — the third time in this package that a recorded frame could not fail against the
     * implementation it was recorded from (see ADR-096 §6, RF-6).
     */
    @Test
    fun done_sits_at_the_cards_far_edge_and_not_against_the_title() {
        val store = store()
        setScreen(store)
        openInk(store)
        val card = bounds(BenchInkPopoverTestTag)
        val done = bounds(BenchInkDoneTestTag)
        val title = composeRule.onNodeWithText(Copy.BenchInk.TITLE).fetchSemanticsNode().boundsInRoot
        // `.inkpop{padding:12px 14px 14px}` inside a 1px border: the header's content box ends 15dp in.
        val inset = px(14.dp) + px(1.dp)
        assertEquals("Done's right edge is the card's content edge", card.right - inset, done.right, 2f)
        // …and the two are genuinely apart, which is what "space-between" means on a 411dp card.
        assertTrue(
            "Done must not sit against the title (title ends ${title.right}, Done starts ${done.left})",
            done.left - title.right > px(100.dp),
        )
    }

    /**
     * Row 6.1i — the frozen stacking order: `.ctx` 30 (`:357`) < `.snack` 38 (`:444`) < `.inkpop` 42
     * (`:377`). The popover is drawn ABOVE the snack it raises.
     *
     * Also a Device Pass 1 finding. `BenchSnack` is composed after `BenchInkPopover`, so the snack drew
     * over the card and covered the `.inkuse` note — the one line that tells you what your zine now
     * costs to print, hidden by the confirmation that you changed it.
     *
     * Asserted as paint, because z-order is paint: in the light palette the snack's `--ink` ground and
     * the popover's `--sheet` are at opposite ends of the scale, so one pixel inside the note settles it.
     */
    @Test
    fun the_popover_is_drawn_above_the_snack_it_raises() {
        val store = store()
        setScreen(store)
        openInk(store)
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithContentDescription(Copy.BenchInk.FOREST).performClick()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        val note = composeRule.onNodeWithTag(BenchInkUseNoteTestTag, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInWindow
        val bmp = composeRule.activity.window.decorView.rasterizeToBitmap()
        val sheet = zinelyV2LightColors().sheet.toArgb()
        val ink = zinelyV2LightColors().ink.toArgb()
        // Counted across the whole note rather than sampled at one pixel: the note carries a shield
        // glyph and a sentence, both in `--ink-faint`, and a single probe lands on whichever it hits.
        // What settles the z-order is how much of the note's own GROUND survives.
        var onSheet = 0
        var total = 0
        for (y in note.top.toInt() until note.bottom.toInt()) {
            for (x in note.left.toInt() until note.right.toInt()) {
                total++
                if (channelDistance(bmp.getPixel(x, y), sheet) < channelDistance(bmp.getPixel(x, y), ink)) {
                    onSheet++
                }
            }
        }
        assertTrue(
            "only $onSheet of $total pixels of the ink note stand on the popover's --sheet; the snack is " +
                "drawn over the card it should sit behind",
            onSheet > total / 2,
        )
    }

    /**
     * Row 6.3 — four bands in the freeze, **three** for a text target. ADR-089's own table said three
     * and `openInk` emits four; OD-24 then fenced `Paper tints` away from a text element, so a text
     * target sees `Inks · Neutrals · Ready-made palettes`. The labels are asserted as an ordered list for
     * the same reason the swatches are. (The name said "four" while the body asserted three, which
     * independent review named — the count in a test's name is a claim like any other.)
     */
    @Test
    fun a_text_target_draws_three_of_the_freezes_four_labelled_bands() {
        val store = store()
        setScreen(store)
        openInk(store)
        val labels = composeRule.onAllNodesWithTag(BenchInkBandLabelTestTag, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text }
        assertEquals(
            listOf(
                Copy.BenchInk.INKS.uppercase(),
                Copy.BenchInk.NEUTRALS.uppercase(),
                Copy.BenchInk.PRESETS.uppercase(),
            ),
            labels,
        )
    }

    // =================================================================================================
    // The swatches (rows 6.4-6.6)
    // =================================================================================================

    /** Row 6.4/6.5 — fourteen swatches for a text element, each the frozen 26dp. */
    @Test
    fun every_swatch_is_the_frozen_twenty_six_dp() {
        val store = store()
        setScreen(store)
        openInk(store)
        assertEquals("ten inks plus four neutrals", 14, count(BenchInkSwatchTestTag))
        val first = swatchBounds(0)
        assertEquals(px(26.dp), first.width, 1f)
        assertEquals(px(26.dp), first.height, 1f)
    }

    /** Row 6.4b — `.swrow{gap:7px}`, measured edge to edge between two swatches of one band. */
    @Test
    fun the_swatch_row_keeps_the_frozen_seven_dp_gap() {
        val store = store()
        setScreen(store)
        openInk(store)
        val a = swatchBounds(0)
        val b = swatchBounds(1)
        assertEquals(px(7.dp), b.left - a.right, 1f)
    }

    /**
     * Row 6.5c — `.sw2:active{transform:scale(.9)}` over `transition:transform .1s`.
     *
     * Under a **held** pointer, with the clock advanced past the transition. C5 tried to excuse the
     * equivalent row with *"a 100ms press transform has no assertable resting state"*; independent review
     * refused it, and the same refusal applies here.
     */
    @Test
    fun a_pressed_swatch_shrinks_to_the_frozen_nine_tenths() {
        val store = store()
        setScreen(store)
        openInk(store)
        val rest = swatchBounds(0)
        composeRule.mainClock.autoAdvance = false
        composeRule.onAllNodesWithTag(BenchInkSwatchTestTag, useUnmergedTree = true)[0]
            .performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(BenchInkSwatchPressMillis + 100L)
        composeRule.waitForIdle()
        val pressed = swatchBounds(0)
        assertEquals("scale(.9) of the frozen 26dp", rest.width * 0.9f, pressed.width, 1f)
        composeRule.onAllNodesWithTag(BenchInkSwatchTestTag, useUnmergedTree = true)[0]
            .performTouchInput { up() }
        assertEquals("`.1s` is the frozen transition", 100, BenchInkSwatchPressMillis)
    }

    /**
     * Row 6.16 / [D-009](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-009--no-control-in-the-frozen-trilogy-declares-a-minimum-touch-target-and-most-measure-well-under-48dp) —
     * *extend the target, keep the paint*. The paint stays 26dp (asserted above); the target reaches the
     * 48dp floor without `minimumInteractiveComponentSize()`, which would have moved the 7dp gaps.
     */
    @Test
    fun a_swatch_paints_at_twenty_six_and_is_touchable_at_forty_eight() {
        val store = store()
        setScreen(store)
        openInk(store)
        val node = composeRule.onAllNodesWithTag(BenchInkSwatchTestTag, useUnmergedTree = true)
            .fetchSemanticsNodes()[0]
        assertEquals("the paint does not grow", px(26.dp), node.boundsInRoot.width, 1f)
        assertTrue(
            "the target reaches the floor: ${node.touchBoundsInRoot.width}px wide, " +
                "${node.touchBoundsInRoot.height}px tall against ${px(48.dp)}px",
            node.touchBoundsInRoot.width >= px(48.dp) - 1f &&
                node.touchBoundsInRoot.height >= px(48.dp) - 1f,
        )
    }

    // =================================================================================================
    // Applying an ink (rows 6.13, 6.14, 6.6)
    // =================================================================================================

    /** Row 6.13 — one tap, one `Intent.StyleText`, one undoable command with the frozen value. */
    @Test
    fun choosing_a_swatch_inks_the_element_with_that_exact_colour() {
        val store = store()
        setScreen(store)
        val id = openInk(store)
        composeRule.onNodeWithContentDescription(Copy.BenchInk.BRICK).performClick()
        composeRule.waitForIdle()
        assertEquals(ColorRgba(0xB0, 0x50, 0x3F), textOf(store, id).style.color)
    }

    /** Row 6.13b — an ink is undoable, because it is one immediate-commit command and nothing else. */
    @Test
    fun an_ink_is_one_undoable_command() {
        val store = store()
        setScreen(store)
        val id = openInk(store)
        val before = textOf(store, id).style.color
        composeRule.onNodeWithContentDescription(Copy.BenchInk.PLUM).performClick()
        composeRule.waitForIdle()
        assertEquals(ColorRgba(0x8A, 0x5A, 0x9B), textOf(store, id).style.color)
        store.dispatch(Intent.Undo)
        composeRule.waitForIdle()
        assertEquals(before, textOf(store, id).style.color)
    }

    /** Row 6.11c — a preset applies its primary through the same one command. */
    @Test
    fun choosing_a_preset_inks_the_element_with_its_primary() {
        val store = store()
        setScreen(store)
        val id = openInk(store)
        composeRule.onNodeWithContentDescription(
            Copy.BenchInk.presetLabel(Copy.BenchInk.PRESET_WARM, Copy.BenchInk.BRICK),
        ).performClick()
        composeRule.waitForIdle()
        assertEquals("Warm zine's [0], not its accent", ColorRgba(0xB0, 0x50, 0x3F), textOf(store, id).style.color)
    }

    /**
     * Row 6.13c — the frozen snack, and the half that makes it C6's rather than C4's: **no button**.
     * ADR-094 row 4.15 owns the variant; this asserts that the ink path raises *that* one.
     */
    @Test
    fun applying_an_ink_raises_the_buttonless_snack_that_names_it() {
        val store = store()
        setScreen(store)
        openInk(store)
        // The clock is held for the same reason C4's delete-snack row holds it: with it running, the
        // 1600ms window opens and closes inside a single `waitForIdle` and the assertion reads an
        // absence it caused itself.
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithContentDescription(Copy.BenchInk.AQUA).performClick()
        composeRule.mainClock.advanceTimeBy(300L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertIsDisplayed()
        // Read off the snack's own node rather than searched for by text: the popover covers the same
        // 12dp inset the snack occupies, so a free text match resolves against a node the popover is
        // drawn over and `assertIsDisplayed` reports the occlusion rather than the message.
        // By content description, not by text: `BenchSnack` publishes its message through
        // `clearAndSetSemantics` as a polite live region, so the words exist as a *name* and not as
        // `Text` — which is also the form a screen reader hears them in.
        assertEquals(
            "the snack names the ink that landed",
            1,
            composeRule.onAllNodesWithContentDescription(Copy.BenchInk.applied(Copy.BenchInk.AQUA))
                .fetchSemanticsNodes().size,
        )
        assertEquals(
            // Counted by the snack's OWN action tag, never by the word: the bar below it carries an
            // `Undo` of its own, so a bare text search would pass on a snack that did have a button.
            "the ink snack carries no button",
            0,
            count(BenchSnackActionTestTag),
        )

        // Row 6.13d - the frozen 1600ms, in LITERAL milliseconds, and the half that distinguishes this
        // snack from the delete one: at 3000ms the delete window is still open and this one is long shut.
        composeRule.mainClock.advanceTimeBy(1200L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(700L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertDoesNotExist()
        assertEquals("the frozen ink window is 1600ms", 1600L, BenchSnackInkMillis)
    }

    /**
     * Row 6.6 — the ring follows the **document**, not the last tap. Asserted through the selection
     * semantics `selectable` publishes, which is the same state the ring is drawn from.
     */
    @Test
    fun the_ring_marks_the_element_s_own_ink_and_only_that_one() {
        val store = store()
        setScreen(store)
        openInk(store)
        composeRule.onNodeWithContentDescription(Copy.BenchInk.FOREST).performClick()
        composeRule.waitForIdle()
        val selected = composeRule.onAllNodesWithTag(BenchInkSwatchTestTag, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .count { it.config.getOrNull(SemanticsProperties.Selected) == true }
        assertEquals("exactly one swatch is chosen", 1, selected)
        composeRule.onNodeWithContentDescription(Copy.BenchInk.FOREST)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))

        // …and the colour that appears TWICE rings once. `Ink #2A251E` is a member of both the Inks band
        // and the Neutrals band (`ZinelyContentInks.kt:219`, `:231`; `INKS` `:596` and `NEUT` `:598` in
        // the freeze), and both bands are drawn for a text target. Ringing by colour equality ringed it
        // twice and published `Selected` on two RadioButton nodes in one group — invisible to the check
        // above, which uses a unique colour, and invisible to M15. Independent review found it; this is
        // the fixture that would have.
        val inkSwatches = composeRule.onAllNodesWithContentDescription(Copy.BenchInk.INK)
            .fetchSemanticsNodes()
        assertEquals("`Ink` is drawn in two bands, by the freeze's own repetition", 2, inkSwatches.size)
        composeRule.onAllNodesWithContentDescription(Copy.BenchInk.INK)[0].performClick()
        composeRule.waitForIdle()
        assertEquals(
            "the duplicated ink rings exactly one swatch",
            1,
            composeRule.onAllNodesWithTag(BenchInkSwatchTestTag, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .count { it.config.getOrNull(SemanticsProperties.Selected) == true },
        )
    }

    /**
     * Row 6.6b — an ink that came from the shipped Type bar rings **nothing**. `Coral`, `Teal` and `Blue`
     * are in no frozen band ([OD-24](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-028-ruling)
     * §4), and a ring on the nearest swatch would be a lie about what the element carries.
     */
    @Test
    fun an_ink_from_outside_the_frozen_bands_rings_nothing() {
        val store = store()
        setScreen(store)
        val id = openInk(store)
        // TypeBar's Coral — deliberately absent from all nineteen.
        store.dispatch(Intent.StyleText(id, color = ColorRgba(0xA6, 0x3C, 0x22)))
        composeRule.waitForIdle()
        val selected = composeRule.onAllNodesWithTag(BenchInkSwatchTestTag, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .count { it.config.getOrNull(SemanticsProperties.Selected) == true }
        assertEquals("nothing is claimed", 0, selected)
    }

    /**
     * Row 6.14 — the popover belongs to the element that summoned it. Deselecting takes it away, exactly
     * as `deselect()` does (`v2-bench.html:628`).
     */
    @Test
    fun the_popover_stands_down_when_the_selection_does() {
        val store = store()
        setScreen(store)
        openInk(store)
        assertEquals(1, count(BenchInkPopoverTestTag))
        store.dispatch(Intent.ClearSelection)
        composeRule.waitForIdle()
        assertEquals(0, count(BenchInkPopoverTestTag))

        // …and it stands down when the selection MOVES, which is the half that needed asserting on its
        // own. Clearing the selection also nulls `inkTarget`, so the popover hides for a second reason
        // and an inert `LaunchedEffect` survived the check above (battery M30, GREEN). Placing a second
        // text box keeps `inkTarget` non-null throughout: only the effect can close the popover here.
        openInk(store)
        assertEquals(1, count(BenchInkPopoverTestTag))
        placedText(store)
        composeRule.waitForIdle()
        assertEquals(0, count(BenchInkPopoverTestTag))
    }

    /**
     * Row 6.18 — the popover is **chrome over the artifact**, so it takes the room's palette and not the
     * sheet island's ([D-035](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-035)).
     *
     * This row had a golden against it, and the golden could not have failed: [BenchC6GoldenTest] hosts
     * [BenchInkPopover] standalone, where there is no island to take the wrong palette from. The claim is
     * about *where the popover is composed*, so it can only be asserted at the assembly — the battery
     * found that (M38, GREEN) before any reviewer had to.
     *
     * Two assertions, because the row has two failure modes. Sourcing the whole popover from the island
     * gives a cream card floating in a dark room: internally legible, and wrong — caught by the ground.
     * Sourcing only its *text* from the island gives the C2b defect itself, dark ink on the room's dark
     * sheet at 1.05:1 — caught by the contrast. `night` is the only qualifier that can tell either apart;
     * under the light palette the two sources agree, which is how C2b reached a device through a green suite.
     */
    @Test
    @Config(qualifiers = "+night")
    fun the_popover_is_the_rooms_chrome_at_night_and_not_the_islands() {
        val store = store()
        setScreen(store)
        openInk(store)
        val card = composeRule.onNodeWithTag(BenchInkPopoverTestTag).fetchSemanticsNode().boundsInWindow
        val bmp = composeRule.activity.window.decorView.rasterizeToBitmap()

        // Mid-height at the card's left edge: past the 1dp hairline, inside the 14dp start padding, and
        // clear of the corner radius — the card's own ground and nothing else.
        val fill = bmp.getPixel(card.left.toInt() + 4, card.center.y.toInt())
        // Asserted as "which palette did this come from", not as byte-equality: the card's own
        // `zinelyShadow` throw tints its ground by a couple of units per channel (measured #272219
        // against a token of #252017), and a probe that failed on that would be measuring the shadow.
        // The two candidate answers are ~#252017 and ~#FBF7EE, so the comparison has no near-miss.
        //
        // This half cannot fail under the island alone — `sheet` is not among the eight tokens
        // `BenchStudio.sheetIsland` overrides, so a popover sourced from the island keeps this ground.
        // It stands against the coarser break: a wholesale light palette under a dark room. The island's
        // own failure mode is the second assertion's.
        val toRoom = channelDistance(fill, zinelyV2DarkColors().sheet.toArgb())
        val toIsland = channelDistance(fill, zinelyV2LightColors().sheet.toArgb())
        assertTrue(
            "the popover's ground is #%06X — %d from the room's --sheet and %d from the island's; chrome "
                .format(fill and 0xFFFFFF, toRoom, toIsland) + "over the artifact takes the room's",
            toRoom < toIsland,
        )

        // …and the popover's own title is legible on it. The TITLE and not `Done`: the island overrides
        // exactly eight tokens (`BenchStudio.sheetIsland`), and `ink` — the title's colour — is the one
        // C2b measured at 1.05:1. `Done` is `inkFaint`, a mid-grey that stays readable on either ground
        // and cannot fail; a first form of this probe measured it and let the mutation through.
        // `Ink` matches one node: the Neutrals swatch of the same name publishes a contentDescription,
        // not text.
        val title = composeRule.onNodeWithText(Copy.BenchInk.TITLE).fetchSemanticsNode().boundsInWindow
        var widest = 0
        for (y in title.top.toInt() + 1 until title.bottom.toInt() - 1) {
            for (x in title.left.toInt() + 1 until title.right.toInt() - 1) {
                val d = channelDistance(bmp.getPixel(x, y), fill)
                if (d > widest) widest = d
            }
        }
        // The same floor `the_verb_bar_is_legible_at_night` uses: the C2b defect measured 7 across all
        // three channels, correct dark theme measures in the hundreds, and 150 is between the two.
        assertTrue("the popover's own title must be legible on the card it sits on (was $widest)", widest > 150)
    }

    // =================================================================================================
    // What C6 must NOT change (row 6.17)
    // =================================================================================================

    /**
     * Row 6.17 — `Ink` opens the popover and **not** the Type bar, which is the route it borrowed while
     * D-028 was open; and `Size` still opens the Type bar, so
     * [OD-11](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-034-ruling)'s additive surface — the
     * only place `Coral`, `Teal` and `Blue` remain reachable — is untouched.
     */
    @Test
    fun ink_opens_the_popover_while_size_still_opens_the_type_bar() {
        val store = store()
        setScreen(store)
        openInk(store)
        assertEquals("the popover, not the Type bar", 1, count(BenchInkPopoverTestTag))
        assertEquals(0, count(TypeBarTestTag))
        composeRule.onNodeWithTag(BenchInkDoneTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.SIZE}").performClick()
        composeRule.waitForIdle()
        assertEquals("Size keeps its OD-9 route", 1, count(TypeBarTestTag))
    }

    /** Row 6.15 — every swatch is named, and the name is the colour's, because that is its whole meaning. */
    @Test
    fun every_swatch_announces_the_ink_it_is() {
        val store = store()
        setScreen(store)
        openInk(store)
        val expected = benchInkBands(inks, BenchVerbKind.TEXT).flatMap { it.swatches }.map { it.name }
        val announced = composeRule.onAllNodesWithTag(BenchInkSwatchTestTag, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull() }
        assertEquals(expected, announced)
    }

    /**
     * Row 6.15b — a preset publishes **one** node naming the recipe *and* the ink it applies. Three
     * overlapping circles are not speakable, and a reader told only "Warm zine" would not know which of
     * its three colours it is about to get.
     */
    @Test
    fun a_preset_announces_the_ink_it_will_actually_apply() {
        val store = store()
        setScreen(store)
        openInk(store)
        assertEquals(3, count(BenchInkPresetTestTag))
        composeRule.onNodeWithContentDescription(
            Copy.BenchInk.presetLabel(Copy.BenchInk.PRESET_COOL, Copy.BenchInk.FOREST),
        ).assertIsDisplayed()
    }

    /** Row 6.12b — the note is one sentence with the live count, published as one node. */
    @Test
    fun the_ink_note_reads_as_one_sentence() {
        val store = store()
        setScreen(store)
        placedText(store)
        composeRule.waitForIdle()
        openInk(store)
        composeRule.onNodeWithContentDescription(Copy.BenchInk.useNote(1)).assertIsDisplayed()

        // …and it MOVES. Read only at 1, the note cannot tell a live count from a constant, and the
        // zine-wide clause was closed on the pure function alone while the wiring went unasserted.
        // Inking this element apart from the first leaves two distinct inks in the document.
        composeRule.onNodeWithContentDescription(Copy.BenchInk.FOREST).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(Copy.BenchInk.useNote(2)).assertIsDisplayed()
    }
}
