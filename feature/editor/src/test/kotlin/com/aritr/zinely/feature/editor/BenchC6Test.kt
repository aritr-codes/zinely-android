package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsActions
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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
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
import com.aritr.zinely.ui.theme.zinelyV21DarkColors
import com.aritr.zinely.ui.theme.zinelyV21LightColors
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
 * | 6.5b the **absence** of V2's halo, 6.6 the `.pot.on` ring's own pixels | [BenchC6GoldenTest] | both live *outside* the layout bounds, so no semantics node can measure them |
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

    private fun potBounds(index: Int) =
        composeRule.onAllNodesWithTag(BenchInkPotTestTag, useUnmergedTree = true)
            .fetchSemanticsNodes()[index].boundsInRoot

    private fun textOf(store: EditorStore, id: String) =
        store.uiState.value.document.pages[store.uiState.value.currentPageIndex]
            .elements.first { it.id == id } as TextElement

    // =================================================================================================
    // The fence — OD-24, and the whole of what the owner ruled (rows 6.7, 6.8, 6.9)
    // =================================================================================================

    /**
     * Row 6.9, as A40 rev. 2 states it — **the ruling itself.** A text element is offered the thirteen
     * built-in inks (ten maker inks, then Slate, Stone, Fog) and **no paper tints**.
     *
     * Was: the band labels `Spot inks · Neutrals`. A40 rev. 2 merged the two bands into one Built-in grid
     * with `Ink` listed once, so the contract is now the swatch list itself.
     */
    @Test
    fun a_text_element_is_offered_the_thirteen_built_ins_and_no_paper_tints() {
        val built = benchBuiltInInks(inks, BenchVerbKind.TEXT)
        assertEquals(13, built.size)
        assertTrue("no tint reaches a text target", built.none { it.tint })
    }

    /**
     * Row 6.9b — the fence is a **fence**, not a deletion: any other target gets the thirteen and then the
     * five tints (prototype `builtFor`). Was: "all three bands"; A40 rev. 2 has one grid and a separator.
     */
    @Test
    fun any_other_ink_target_gets_the_built_ins_then_the_five_tints() {
        listOf(BenchVerbKind.PHOTO, BenchVerbKind.DECOR).forEach { kind ->
            val built = benchBuiltInInks(inks, kind)
            assertEquals("built-ins for $kind", benchBuiltInInks(inks, BenchVerbKind.TEXT), built.take(13))
            assertEquals("tints for $kind", 5, built.drop(13).count { it.tint })
            assertEquals(18, built.size)
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
            benchBuiltInInks(inks, BenchVerbKind.PHOTO).filter { it.tint }.map { it.name to it.value },
        )
    }

    /**
     * Rows 6.7 + 6.8, as A40 rev. 2 orders them (prototype `BUILT`): the ten maker inks, then the neutrals
     * with the duplicate `Ink` shown **once** — by name **and** value, because either alone is a
     * half-assertion. Was: two band tests, and `Ink` in both bands.
     */
    @Test
    fun the_built_ins_are_the_ten_maker_inks_then_slate_stone_fog() {
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
                Copy.BenchInk.SLATE to Color(0xFF5B5347),
                Copy.BenchInk.STONE to Color(0xFF8C8269),
                Copy.BenchInk.FOG to Color(0xFFB7AD93),
            ),
            benchBuiltInInks(inks, BenchVerbKind.TEXT).map { it.name to it.value },
        )
    }


    // =================================================================================================
    // The starter palettes (rows 6.10, 6.11; A40 rev. 2)
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
            benchStarterPalettes(inks).map { p -> p.name to p.inks.map { it.value } },
        )
    }


    /**
     * Row 6.11b — the recipes are built **from the typed palette**, not from re-typed hexes, so a recipe
     * cannot drift from the band it is made of. Two of the three end in a paper *tint* by value, which is
     * the evidence that decoded `[ink, accent, paper]` in the first place — asserted so that reading
     * survives in the suite rather than only in prose.
     */
    @Test
    fun two_of_the_three_recipes_end_in_a_paper_tint() {
        val starters = benchStarterPalettes(inks)
        assertEquals(inks[ZinelyPaperTintId.Cream].value, starters[1].inks.last().value)
        assertEquals(inks[ZinelyPaperTintId.Sky].value, starters[2].inks.last().value)
        assertEquals(inks[ZinelyMakerInkId.Ink].value, starters[0].inks.first().value)
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

    /**
     * A placed supply is a **spot ink** and is counted (ADR-105 S7; independent review). SUPPLIES-SPEC §2
     * makes a supply an outline laid down in one colour precisely so this is true, and `SceneRenderer`
     * draws it as one — so black text plus three berry stars is a two-ink job, not a one-ink job.
     *
     * The fixture uses `shape.circle`, an **authored** supply: a decor assertion whose fixture names one of
     * the twelve unauthored ids can pass while proving nothing, because such a supply reaches no ink on any
     * surface. Mutation: drop the `DecorElement` arm from `benchInkCount` and this reads 1.
     */
    @Test
    fun the_ink_note_counts_a_placed_supplys_ink_as_a_spot_ink() {
        val ink = ColorRgba(0x2A, 0x25, 0x1E)
        val berry = ColorRgba(0xE2, 0x7F, 0x89)
        val star = DecorElement(
            id = "d1",
            transform = Transform(0.0, 0.0, 10.0, 10.0),
            supplyId = "shape.circle",
            ink = berry,
        )
        val pages = listOf(
            Page(index = 0, role = PageRole.INTERIOR, elements = listOf(text("1", ink))),
            Page(index = 1, role = PageRole.INTERIOR, elements = listOf(star)),
        )
        assertEquals(2, benchInkCount(pages))
        // …and a supply sharing the text's ink is still one ink, so this counts colours and not elements.
        assertEquals(1, benchInkCount(listOf(pages[0], Page(1, PageRole.INTERIOR, elements = listOf(star.copy(ink = ink))))))
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
        (benchBuiltInInks(inks, BenchVerbKind.DECOR) + benchStarterPalettes(inks).flatMap { it.inks })
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
     * Row 6.1e — `.inkpop{transform:translateY(10px)}` at rest, released over `.18s`
     * (`v21-bench.html:240`). V2 asked for 14px over `.22s`.
     *
     * Measured mid-flight with the clock held, against the resting position taken after it lands: the
     * offset is a *fixed* 10px in the freeze, not a fraction of the card's own height, so a card that
     * slid its full height would pass any "it moves" assertion and fail this one.
     */
    @Test
    fun the_popover_rises_ten_dp_into_place() {
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
            rise > px(4.dp),
        )
        assertTrue("…and by no more than the frozen 10dp: $rise", rise <= px(10.dp) + 0.5f)
        // The measured bound is a range, because the first readable frame is already part-way through
        // the ease and pinning it would encode a frame number. The frozen value itself is exact, so it
        // is asserted as a value as well — 14dp or 8dp would satisfy the range above and neither is 10.
        assertEquals("`translateY(10px)` is the frozen offset", 10.dp, BenchInkPopoverEnterOffsetDp)
        assertEquals("`.18s` is the frozen duration", 180, BenchInkPopoverEnterMillis)
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
        composeRule.onNodeWithTag(BenchInkTitleTestTag).assertIsDisplayed()
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
        val title = bounds(BenchInkTitleTestTag)
        // **Restated, not relaxed.** The old number (V2's `.inkpop{padding:12px 14px 14px}` plus a 1px
        // border = 15dp) described a box that no longer exists, and it failed by exactly the 3dp it was
        // wrong by. A16 freezes `.inkpop.ink-a16{padding:var(--gap-md)}` (`v21-bench.html:1964`) — 12dp
        // on the sides — and the 1.5dp ink border adds nothing to that:
        // Compose's `border` is a DRAW, painted over the first 1.5dp of the padding, where CSS's border
        // is part of the box and pushes the content in. Neither the 1.5dp border nor the Done pill's
        // 2dp resting shadow (also draw-only) is inside any node's bounds. The control was never
        // misplaced — it sits exactly on the content edge — so this is the expectation catching up.
        // Transcribed from the freeze rather than read from the composable, per this file's rule that
        // the code and the assertion may not agree with each other on a wrong value.
        val inset = px(12.dp)
        // Tolerance tightened 2f -> 1f: the padding is whole pixels and `SpaceBetween` puts the last
        // child's right edge on the Row's, so the only slack left is rounding.
        assertEquals("Done's right edge is the card's content edge", card.right - inset, done.right, 1f)
        // …and the two are genuinely apart, which is what "space-between" means on a 411dp card.
        assertTrue(
            "Done must not sit against the title (title ends ${title.right}, Done starts ${done.left})",
            done.left - title.right > px(100.dp),
        )
    }

    /**
     * **Inverted by A40 rev. 2.** Row 6.1i put `.inkpop` (42) above `.snack` (38), because C6's snack had
     * no button. A40 gives the tray's snack an Undo, so the frozen `.toast2` is `z-index:70` at
     * `bottom:96px` — ABOVE the open tray, or its Undo would sit under the card. Asserted as paint: across
     * the snack's own bounds its `--ink` ground must win over the card's `--surface`.
     */
    @Test
    fun the_snack_is_drawn_above_the_open_tray() {
        val store = store()
        setScreen(store)
        openInk(store)
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithContentDescription(Copy.BenchInk.FOREST).performClick()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        val snack = composeRule.onNodeWithTag(BenchSnackTestTag).fetchSemanticsNode().boundsInWindow
        val card = composeRule.onNodeWithTag(BenchInkPopoverTestTag).fetchSemanticsNode().boundsInWindow
        assertTrue("the snack stands over the card ($snack in $card)", snack.top > card.top && snack.bottom < card.bottom)
        val bmp = composeRule.activity.window.decorView.rasterizeToBitmap()
        val paper = zinelyV21LightColors().surface.toArgb()
        val ink = zinelyV21LightColors().ink.toArgb()
        var onInk = 0
        var total = 0
        val y = snack.center.y.toInt()
        for (x in (snack.left + 8f).toInt() until (snack.right - 8f).toInt()) {
            total++
            if (channelDistance(bmp.getPixel(x, y), ink) < channelDistance(bmp.getPixel(x, y), paper)) onInk++
        }
        assertTrue("only $onInk of $total pixels across the snack show its ink ground; the tray covers it", onInk > total / 2)
    }

    /**
     * Row 6.3, as A40 rev. 2 labels the tray: `My palettes · My inks · Built-in inks`, then the Starter
     * palettes disclosure (a button, not a label). Was: `Spot inks · Neutrals · Starting palettes`.
     */
    @Test
    fun a_text_target_draws_the_three_a40_section_labels() {
        val store = store()
        setScreen(store)
        openInk(store)
        val labels = composeRule.onAllNodesWithTag(BenchInkBandLabelTestTag, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text }
        assertEquals(
            listOf(
                Copy.BenchInk.MY_PALETTES.uppercase(),
                Copy.BenchInk.MY_INKS.uppercase(),
                Copy.BenchInk.BUILT_IN_INKS.uppercase(),
            ),
            labels,
        )
    }

    // =================================================================================================
    // The swatches (rows 6.4-6.6)
    // =================================================================================================

    /** A40 rev. 2 — thirteen built-in targets for text, each a compact 28dp pot (`.pots.compact .pot`). */
    @Test
    fun every_built_in_pot_is_the_compact_twenty_eight_dp() {
        val store = store()
        setScreen(store)
        openInk(store)
        assertEquals("ten maker inks plus Slate, Stone, Fog", 13, count(BenchInkSwatchTestTag))
        val first = potBounds(0)
        assertEquals(px(28.dp), first.width, 1f)
        assertEquals(px(28.dp), first.height, 1f)
    }

    /** A40 rev. 2 — the built-ins are one grid, seven across (`.pots.compact{repeat(7,…)}`). Was: 5×2. */
    @Test
    fun the_built_in_grid_is_seven_across() {
        val store = store()
        setScreen(store)
        openInk(store)
        val firstRow = (0 until 7).map(::swatchBounds)
        val secondRow = (7 until 13).map(::swatchBounds)
        assertTrue(firstRow.zipWithNext().all { (a, b) -> kotlin.math.abs(a.top - b.top) <= 1f })
        assertTrue(secondRow.zipWithNext().all { (a, b) -> kotlin.math.abs(a.top - b.top) <= 1f })
        assertTrue(secondRow.first().top > firstRow.first().top)
        assertEquals(firstRow.first().left, secondRow.first().left, 1f)
    }

    /**
     * **Inverted by ADR-102 P4 — the pot no longer shrinks under a finger, and must not start again.**
     *
     * V2's `.sw2:active{transform:scale(.9)}` over `transition:transform .1s` is gone: `.pot`
     * (`v21-bench.html:250-251`) declares no `:active` at all, because V2.1 says press with a *printed*
     * translation and a pot has no shadow to shed. This test is kept rather than deleted, pointing the
     * other way, so the scale reappearing fails here instead of shipping.
     *
     * Held pointer, clock advanced well past what the old transition would have taken.
     */
    @Test
    fun a_pressed_swatch_does_not_shrink() {
        val store = store()
        setScreen(store)
        openInk(store)
        val rest = swatchBounds(0)
        composeRule.mainClock.autoAdvance = false
        composeRule.onAllNodesWithTag(BenchInkSwatchTestTag, useUnmergedTree = true)[0]
            .performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(300L)
        composeRule.waitForIdle()
        val pressed = swatchBounds(0)
        assertEquals("the choice holds its size under a finger", rest.width, pressed.width, 1f)
        assertEquals("…on both axes", rest.height, pressed.height, 1f)
        composeRule.onAllNodesWithTag(BenchInkSwatchTestTag, useUnmergedTree = true)[0]
            .performTouchInput { up() }
    }

    /**
     * Row 6.16 / D-009 — *extend the target, keep the paint*. The compact pot paints at 28dp
     * (A40 rev. 2); its choice reaches the 48dp floor (`.pots.compact .ink-choice{min-height:48px}`).
     */
    @Test
    fun a_built_in_paints_at_twenty_eight_and_is_touchable_at_forty_eight() {
        val store = store()
        setScreen(store)
        openInk(store)
        val node = composeRule.onAllNodesWithTag(BenchInkSwatchTestTag, useUnmergedTree = true)
            .fetchSemanticsNodes()[0]
        val pot = potBounds(0)
        assertEquals("the paint does not grow", px(28.dp), pot.width, 1f)
        assertEquals("the paint remains circular", px(28.dp), pot.height, 1f)
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

    /**
     * Row 6.11c, as ADR-138 amends it: a starter's pot applies **its own** ink. OD-24's "a preset applies
     * its first colour" is retired. Sunflower is Warm zine's second ink, so the old rule would ink Brick.
     */
    @Test
    fun a_starter_pot_applies_its_own_ink_not_the_starters_first() {
        val store = store()
        setScreen(store)
        val id = openInk(store)
        composeRule.onNodeWithTag(BenchInkStartersTestTag).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(
            Copy.BenchInk.palettePot(Copy.BenchInk.SUNFLOWER, Copy.BenchInk.PRESET_WARM),
        ).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(ColorRgba(0xE7, 0xB5, 0x3C), textOf(store, id).style.color)
    }

    /**
     * Row 6.13c, as A40 rev. 2 states it: the snack names the ink and, because the tap changed the
     * document, **carries Undo** for the frozen `toast` 4000ms. Was: C6's buttonless 1600ms snack (the bar's
     * Undo did the work); rev. 2 puts Undo on the snack whenever something changed.
     */
    @Test
    fun applying_an_ink_raises_a_snack_that_names_it_and_offers_undo() {
        val store = store()
        setScreen(store)
        openInk(store)
        // Held so the 4000ms window does not open and close inside one `waitForIdle`.
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithContentDescription(Copy.BenchInk.AQUA).performClick()
        composeRule.mainClock.advanceTimeBy(300L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertIsDisplayed()
        // By content description: `BenchSnack` publishes its message as a name, not as `Text`.
        assertEquals(
            "the snack names the ink that landed",
            1,
            composeRule.onAllNodesWithContentDescription(Copy.BenchInk.applied(Copy.BenchInk.AQUA))
                .fetchSemanticsNodes().size,
        )
        // Counted by the snack's OWN action tag, never by the word: the bar below carries an `Undo` too.
        assertEquals("the ink snack carries Undo", 1, count(BenchSnackActionTestTag))

        composeRule.mainClock.advanceTimeBy(3400L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(800L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertDoesNotExist()
        assertEquals("the A40 tray snack window is 4000ms", 4000L, BenchSnackInkTrayMillis)
    }

    /**
     * Row 6.6 — the ring follows the **document**, not the last tap, and marks exactly one built-in.
     * A40 rev. 2 lists `Ink` once, so D-083's two spoken names (`Spot ink`, `Neutral ink`) are gone: the
     * one `Ink` choice is spoken `Ink`. Was: both spoken names asserted present and the bare name absent.
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
        assertEquals("exactly one built-in is chosen", 1, selected)
        composeRule.onNodeWithContentDescription(Copy.BenchInk.FOREST)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))

        assertEquals("Ink is one choice now", 1, composeRule.onAllNodesWithContentDescription(Copy.BenchInk.INK).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithContentDescription("Spot ink").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithContentDescription("Neutral ink").fetchSemanticsNodes().size)
        composeRule.onNodeWithContentDescription(Copy.BenchInk.INK).performClick()
        composeRule.waitForIdle()
        assertEquals(
            "the shared pigment rings exactly one built-in",
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

        // Mid-height at the card's left edge: past the 1.5dp ink border, inside the 12dp start padding,
        // and clear of the corner radius — the card's own ground and nothing else. The V2.1 printed
        // shadow falls down-RIGHT and outside the node, so unlike V2's soft throw it cannot tint this.
        val fill = bmp.getPixel(card.left.toInt() + 4, card.center.y.toInt())
        // Asserted as "which palette did this come from", not as byte-equality. Under V2.1 the card's
        // ground is `surface`, which the paper island must not override — so this half catches
        // the island break as well as the coarser one (a wholesale light palette under a dark room).
        val toRoom = channelDistance(fill, zinelyV21DarkColors().surface.toArgb())
        val toIsland = channelDistance(fill, zinelyV21LightColors().surface.toArgb())
        assertTrue(
            "the popover's ground is #%06X — %d from the room's --surface and %d from the island's; chrome "
                .format(fill and 0xFFFFFF, toRoom, toIsland) + "over the artifact takes the room's",
            toRoom < toIsland,
        )

        // …and the popover's own title is legible on it. The TITLE and not `Done`: `ink` — the title's
        // colour — is the token C2b measured at 1.05:1, while `Done` carries its own `leaf` ground and
        // `onLeaf` label and stays readable whichever palette it came from, so it cannot fail. A first
        // form of this probe measured it and let the mutation through.
        // `Ink` matches one node: the Neutrals swatch of the same name publishes a contentDescription,
        // not text.
        val title = composeRule.onNodeWithTag(BenchInkTitleTestTag).fetchSemanticsNode().boundsInWindow
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
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.TEXT}").performClick()
        composeRule.waitForIdle()
        assertEquals("Text keeps its OD-9 route", 1, count(TypeBarTestTag))
    }

    /**
     * **F-15 — Back stands the Type bar down instead of leaving the editor.**
     *
     * This is a regression test for a defect found on a device, not a hypothetical. Before the
     * `BackHandler`, Back with the panel open fell through to the editor's own handler and returned the
     * user to the shelf — the whole editing context discarded in one press, from a surface opened to
     * change a font size (reproduced twice, SM-A176B / Android 16). It is the same rule the page grid
     * (`BenchC5Test.back_stands_the_grid_down_and_leaves_the_editor_where_it_was`) and the ink popover
     * already follow.
     *
     * The `isFinishing` assertion is the one that would have caught it: the panel closing is not enough
     * if the Activity is on its way out behind it.
     *
     * The selection assertion is the other half. Closing the panel must not deselect — `styleTarget` is
     * derived from the selection, so clearing it would close the panel twice over and lose the user's
     * place in the element they were styling.
     */
    @Test
    fun back_stands_the_type_bar_down_and_keeps_both_the_editor_and_the_selection() {
        val store = store()
        setScreen(store)
        placedText(store)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.TEXT}").performClick()
        composeRule.waitForIdle()
        assertEquals("Text did not open the Type bar", 1, count(TypeBarTestTag))
        val selectedBefore = store.uiState.value.selection

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals("Back did not stand the Type bar down", 0, count(TypeBarTestTag))
        assertTrue(
            "Back finished the editor instead of closing the Type bar",
            !composeRule.activity.isFinishing,
        )
        assertEquals(
            "Closing the panel deselected the element it was styling",
            selectedBefore,
            store.uiState.value.selection,
        )
    }

    /** Row 6.15 — every swatch is named, and the name is the colour's, because that is its whole meaning. */
    @Test
    fun every_swatch_announces_the_ink_it_is() {
        val store = store()
        setScreen(store)
        openInk(store)
        val expected = benchBuiltInInks(inks, BenchVerbKind.TEXT).map { it.name }
        val announced = composeRule.onAllNodesWithTag(BenchInkSwatchTestTag, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull() }
        assertEquals(expected, announced)
    }


    /** A16 — the current ink is visible, named, and updates through the same document-backed selection. */
    @Test
    fun the_current_ink_summary_names_the_document_ink_and_updates() {
        val store = store()
        setScreen(store)
        openInk(store)
        composeRule.onNodeWithTag(BenchInkCurrentTestTag).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(Copy.BenchInk.FOREST).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(Copy.BenchInk.currentInk(Copy.BenchInk.FOREST))
            .assertIsDisplayed()
    }

    /**
     * A40 rev. 2 — Starter palettes sit behind a disclosure that starts **closed**; opening it shows the
     * three as palette cards, each with `Save a copy`. Was: A16's always-open "Apply …" pills.
     */
    @Test
    fun starter_palettes_open_from_a_closed_disclosure_with_save_a_copy() {
        val store = store()
        setScreen(store)
        openInk(store)
        assertEquals("closed by default", 0, count(BenchInkStarterTestTag))
        composeRule.onNodeWithTag(BenchInkStartersTestTag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
        composeRule.onNodeWithTag(BenchInkStartersTestTag).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(3, count(BenchInkStarterTestTag))
        assertEquals(3, count(BenchInkSaveCopyTestTag))
        composeRule.onNodeWithTag(BenchInkStartersTestTag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
    }

}
