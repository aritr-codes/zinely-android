package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Motion
import com.aritr.zinely.ui.theme.zinelyV21DarkColors
import com.aritr.zinely.ui.theme.zinelyV21LightColors
import com.aritr.zinely.ui.theme.zinelyV2DarkColors
import com.aritr.zinely.ui.theme.zinelyV2LightColors
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * C9 — integration: the four states, the motion policy, persistence of place, and the phase gate
 * ([ADR-097](../../../../../../../docs/DECISIONS.md#adr-097) rows 9.1–9.9).
 *
 * C9 owns **seams, not surfaces**, so this suite looks different from C1–C6's: several rows are asserted
 * over the *repository* rather than over a rendered tree, because "no call site bypasses the motion policy"
 * and "no narration ships" are statements about the whole of `src/main` that no single composition can
 * make. Where a row is already discharged by a shipped package, this suite **verifies it is still there**
 * and says whose it is — it does not re-own it.
 *
 * | row | where it is closed | why not a composition assertion here |
 * |---|---|---|
 * | 9.3 persistence of place | `app` module, `EditorPagePersistenceTest` | the `SavedStateHandle` lives on the ViewModel, which is the app module's |
 * | 9.4 canvas a11y | the mandatory device passes | the **platform** `AccessibilityNodeInfo` tree is not the merged semantics tree (ADR-059, CI-26) |
 * | 9.5 contrast over the grain | `core:ui`, `ZinelyV2ContrastTest` | the tokens and the grain material are both Phase A's, and the arithmetic is pure |
 * | 9.2b the caret | C3's own suite — verified present below | [ADR-093](../../../../../../../docs/DECISIONS.md#adr-093) row 3.8 |
 * | 9.3a the shelf half | nowhere — re-seated by OD-2 | asserted by absence, in the exclusions |
 * | 9.8, 9.9 | hardware and the phase gate | process rows, not code rows |
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp")
class BenchC9Test {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** WCAG AA for body text — row 9.5's text floor. */
    private val AaBodyFloor = 4.5f

    /** WCAG non-text contrast — row 9.5's floor for controls and handles. */
    private val ControlFloor = 3.0f

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(100.0, 130.0)
    private val host: Pair<Dp, Dp> = 360.dp to 720.dp

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

    /** The state the *model* is in, read the same way [EditorScreen] reads it — minus the surface flag. */
    private fun modelState(store: EditorStore, addChooserOpen: Boolean = false) = benchStateOf(
        store.uiState.value.selection,
        store.uiState.value.interaction,
        addChooserOpen,
    )

    private fun placedText(store: EditorStore): String {
        store.dispatch(Intent.PlaceText(Transform(20.0, 60.0, 60.0, 18.0), "hi"))
        return store.uiState.value.selection.single()
    }

    // =================================================================================================
    // Row 9.1 — four states, and every action returns to Rest
    // =================================================================================================

    /**
     * Row 9.1 — the precedence, which is **the freeze's and not a preference**. Each of the three
     * higher states is asserted while the states below it are *also* true, which is the only way a
     * precedence can be tested: a `when` that merely happened to be ordered differently would pass a
     * test that fed it one condition at a time.
     */
    @Test
    fun the_four_states_are_ordered_the_way_the_frozen_script_orders_them() {
        val editing = Interaction.EditingText("e1", token = 1L)
        // Adding wins over a live selection *and* over an open edit session — `showSheet` removes `.ctx`
        // (`v2-bench.html:847`).
        assertEquals(BenchState.Adding, benchStateOf(setOf("e1"), editing, addChooserOpen = true))
        // Editing wins over the selection it is editing — `edit()` also removes `.ctx` (`:649`).
        assertEquals(BenchState.Editing, benchStateOf(setOf("e1"), editing, addChooserOpen = false))
        assertEquals(BenchState.Selected, benchStateOf(setOf("e1"), Interaction.Idle, false))
        assertEquals(BenchState.Rest, benchStateOf(emptySet(), Interaction.Idle, false))
        // There are four, and no fifth: the enum is the model.
        assertEquals(4, BenchState.entries.size)
    }

    /**
     * Row 9.1 — **the return-to-Rest invariant** ([EP-2](../../../../../../../docs/design/V2-BENCH-PRINCIPLES.md)),
     * asserted over the store's real reducers rather than over the derivation.
     *
     * Every frozen action that ends an interaction is exercised: `deselect` (`:626`), `del` (`:715`),
     * `undo` (`:721`) and `setPage` (`:791`). A derivation cannot enforce where a transition *leads*;
     * only the reducers can, which is why this row is asserted here and not on `benchStateOf`.
     */
    @Test
    fun every_action_that_ends_an_interaction_returns_the_bench_to_rest() {
        val terminals: List<Pair<String, (EditorStore, String) -> Unit>> = listOf(
            "deselect" to { s, _ -> s.dispatch(Intent.ClearSelection) },
            "delete" to { s, id -> s.dispatch(Intent.Delete(setOf(id))) },
            // The freeze's own sequence — `undo()` (`:721`) is what *restores* a soft-deleted element, and
            // it captions Rest. Asserted as delete-then-undo rather than undo-of-a-placement so the row
            // tests the transition the frozen script actually performs.
            "undo" to { s, id -> s.dispatch(Intent.Delete(setOf(id))); s.dispatch(Intent.Undo) },
            // And undo of the *placement*, which is the path that found the dangling selection: the
            // element the placement auto-selected is gone, so the selection that pointed at it must be
            // too. See EditorReducer.stepHistory.
            "undo of a placement" to { s, _ -> s.dispatch(Intent.Undo) },
            "page change" to { s, _ -> s.dispatch(Intent.GoToPage(1)) },
        )
        terminals.forEach { (name, act) ->
            val store = store(pageCount = 2)
            val id = placedText(store)
            assertEquals("$name: precondition", BenchState.Selected, modelState(store))
            act(store, id)
            assertEquals("after $name", BenchState.Rest, modelState(store))
        }
    }

    /**
     * Row 9.1 — the derivation is **wired**, not decorative. `ctxVisible` is `benchState == Selected`
     * plus this surface's own narrowing terms, so the frozen bar appearing and disappearing is the
     * state model being read at a real call site.
     */
    @Test
    fun the_frozen_context_bar_is_present_exactly_in_the_selected_state() {
        val store = store()
        setScreen(store)
        val verbTag = "$BenchContextBarTestTag-${Copy.BenchVerbs.INK}"

        // Rest — nothing selected, nothing to act on.
        composeRule.onNodeWithTag(verbTag).assertDoesNotExist()

        // Selected — the one state that carries the bar.
        val id = placedText(store)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(verbTag).assertIsDisplayed()

        // Editing — `edit()` removes `.ctx` (`v2-bench.html:649`). All four states are exercised, not
        // just the two either side of the boundary: a first draft checked Rest and Selected only, and a
        // mutant relaxing the predicate to `!= Rest` survived it — the equivalent-mutant shape
        // [ADR-087](../../../../../../../docs/DECISIONS.md#adr-087) exists to name.
        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(verbTag).assertDoesNotExist()
        val token = (store.uiState.value.interaction as? Interaction.EditingText)?.token ?: 0L
        store.dispatch(Intent.CancelText(id, token))
        composeRule.waitForIdle()

        // Adding — `showSheet` removes `.ctx` (`:847`). This is the term the derivation added.
        composeRule.onNodeWithTag(verbTag).assertIsDisplayed()
        composeRule.onNodeWithTag(BenchBarAddTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(verbTag).assertDoesNotExist()
    }

    // =================================================================================================
    // Row 9.1a — the page grid is an overlay within Rest, not a fifth state
    // =================================================================================================

    /**
     * Row 9.1a — `openGrid` captions `All pages` (`:800`), the one caption in the frozen script naming
     * something outside the four. It is still Rest: `closeGrid` restores it (`:802`), and the grid is
     * reachable only from the Rest-state page row.
     *
     * The strongest statement of this is structural — the grid flag is not a parameter of
     * [benchStateOf] at all — so this test asserts the *consequence*: summoning and dismissing it moves
     * the model not at all.
     */
    @Test
    fun opening_and_closing_the_page_grid_leaves_the_model_in_rest() {
        val store = store(pageCount = 4)
        setScreen(store)
        assertEquals(BenchState.Rest, modelState(store))
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchPageGridTestTag).assertIsDisplayed()
        assertEquals("grid open", BenchState.Rest, modelState(store))
        composeRule.onNodeWithTag(BenchPageGridCloseTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchPageGridTestTag).assertDoesNotExist()
        assertEquals("grid closed", BenchState.Rest, modelState(store))

        // Every exit, not only the frozen one. Back is Compose-only — a prototype has no back button —
        // but "an overlay within Rest" is a claim about all of its exits, and a mutant that made Back
        // re-open the grid instead of standing it down survived a version of this test that only used
        // the head's close.
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchPageGridTestTag).assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchPageGridTestTag).assertDoesNotExist()
        assertEquals("after Back", BenchState.Rest, modelState(store))

        // …and the scrim, which P5 added with the bottom sheet (`v21-bench.html:376-378`, `:845`).
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        // Tapped near the scrim's top edge rather than at its centre: the panel covers the scrim's
        // middle, so `performClick()`'s centre-point lands on a page card and *chooses* instead of
        // leaving. See the same correction in `BenchC5Test`, where the wrong tap showed up as a page
        // change rather than as a failure to dismiss.
        composeRule.onNodeWithTag(BenchPageGridScrimTestTag).performTouchInput {
            click(Offset(centerX, top + 4f))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchPageGridTestTag).assertDoesNotExist()
        assertEquals("after the scrim", BenchState.Rest, modelState(store))
    }

    // =================================================================================================
    // Row 9.1b — the narration ships nothing
    // =================================================================================================

    /**
     * Row 9.1b — `cap()` (`v2-bench.html:583`) writes into `.caption`/`.state`/`.hint`, which
     * [ADR-089 row 1.18](../../../../../../../docs/DECISIONS.md#adr-089) classifies **PROTO**: the
     * prototype explaining itself to a viewer. C9 models the states it names and ships none of its text.
     *
     * Asserted over the build rather than over one rendered tree, because "no narration ships" is a claim
     * about the build, not about a screen. **`core:copy` is scanned as well as the feature package** —
     * every user-facing Bench string actually lives in `Copy.kt`, so a scan of `feature/editor` alone
     * would let the mutation "ship one" survive in the module where it would really be planted. Review
     * found that gap, and with it a guard clause (`text.contains("\"")`) that was inert: every Kotlin
     * file contains a quote character. The fragments below are distinctive enough not to need it.
     */
    @Test
    fun no_frozen_caption_or_hint_string_reaches_the_shipped_bench() {
        val narration = listOf(
            "The page fills the screen",
            "Selected · ",
            "Editing · in place",
            "Adding · Art",
            "The whole zine at once",
            "Soft-deleted — no modal",
            "the studio move",
        )
        val offenders = (mainSources() + copySources()).flatMap { f ->
            val text = f.readText()
            narration.filter { text.contains(it) }.map { "${f.name}: $it" }
        }
        assertEquals("frozen narration must not ship", emptyList<String>(), offenders)
    }

    // =================================================================================================
    // Row 9.2 — the reduced-motion policy
    // =================================================================================================

    /**
     * Row 9.2 — `@media (prefers-reduced-motion:reduce){*{transition-duration:.01ms!important;
     * animation:none!important}}` (**`v2-bench.html:460`** — ADR-089 cites `:293`, which is wrong; see
     * ADR-097 §3.1).
     *
     * The distinction the CSS only implies, and the whole reason this class exists: a one-shot
     * **collapses to zero and still arrives**; a continuous animation **does not run at all**. The two
     * are asserted together because collapsing an infinite animation's duration is precisely the failure
     * mode [D-012](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-012) names — it strobes.
     */
    @Test
    fun one_shot_motion_collapses_to_zero_and_still_arrives_while_continuous_motion_does_not_run() {
        val reduced = ZinelyV2Motion(reduceMotion = true)
        val normal = ZinelyV2Motion(reduceMotion = false)

        assertEquals(0, reduced.durationMillis(200))
        assertEquals(0, reduced.settle<Float>(200).durationMillis)
        assertEquals(0, reduced.standard<Float>(200).durationMillis)
        assertFalse("a looping animation must not run at all", reduced.allowsContinuousMotion)

        // "Still arrives" is the half a `!reduceMotion` early-return would silently drop: the spec is a
        // real tween of zero duration, not the absence of one.
        assertEquals(200, normal.durationMillis(200))
        assertTrue(normal.allowsContinuousMotion)
    }

    // =================================================================================================
    // Row 9.2a — the rule's selector is `*`
    // =================================================================================================

    /**
     * Row 9.2a — **every** V2 animation call site honours the policy. New in C9 because it could not
     * have been written before the call sites existed.
     *
     * A repository scan is the honest instrument: no composition can assert a property of ~20 call
     * sites across two feature packages, and a per-site rendering test would assert the sites that were
     * remembered rather than the ones that exist. This found exactly one bypass —
     * `EditorScreen`'s soft-delete fade, which read `tween(BenchDeleteFadeMillis)` flat and kept fading
     * for 200ms under a reduced-motion preference.
     *
     * **The unit of judgement is the animation's own statement, not its neighbourhood.** A first version
     * accepted a site if the policy appeared anywhere within ±6 lines. Independent review re-implemented
     * that check and ran the ADR's own mutation — *hard-code a duration at one call site* — against all
     * eleven hard-codable sites: **six survived**, because a file opening with `val motion =
     * ZinelyTheme.v2Motion` puts the policy token inside every window in the file. The guard only worked
     * where the policy happened to sit on the call line itself.
     *
     * A site now passes only if its **statement** carries the policy, or a value bound from the policy
     * earlier in the same file, matched as a whole identifier rather than as a substring. That second
     * clause is a necessity rather than a loophole: a duration read inside a coroutine must be hoisted
     * out of composition, which separates it from its use by construction.
     */
    @Test
    fun no_animation_in_the_v2_surfaces_bypasses_the_motion_policy() {
        val offenders = v2Surfaces().flatMap { animationBypassesIn(it) }
        assertEquals("V2 animation sites bypassing ZinelyV2Motion", emptyList<String>(), offenders)
    }

    /** The scan behind row 9.2a, separated so it can be reasoned about — and mutated — on its own. */
    private fun animationBypassesIn(file: File): List<String> {
        val policy = Regex("""v2Motion|reduceMotion|durationMillis\(|\bmotion\.(settle|standard)""")
        val animation = Regex(
            """\b(tween|keyframes|spring|infiniteRepeatable|rememberInfiniteTransition|""" +
                """animateFloatAsState|animateDpAsState|animateColorAsState|animateIntAsState)\s*\(""",
        )
        val boundVal = Regex("""\bval\s+(\w+)\s*=.*(?:v2Motion|reduceMotion|durationMillis\()""")
        val lines = file.readText().split("\n")
        val bound = lines.mapNotNull { boundVal.find(it)?.groupValues?.get(1) }.toSet()
        val offenders = mutableListOf<String>()
        lines.forEachIndexed { i, line ->
            val code = line.trim()
            if (code.startsWith("*") || code.startsWith("//")) return@forEachIndexed
            if (!animation.containsMatchIn(line)) return@forEachIndexed
            val statement = statementAt(lines, i)
            val honoured = policy.containsMatchIn(statement) ||
                bound.any { Regex("""\b${Regex.escape(it)}\b""").containsMatchIn(statement) }
            if (!honoured) offenders += "${file.name}:${i + 1}  $code"
        }
        return offenders
    }

    /** [start]'s line, extended forward until its parentheses balance — the call's own statement. */
    private fun statementAt(lines: List<String>, start: Int): String {
        val sb = StringBuilder()
        var depth = 0
        var i = start
        do {
            val line = lines[i]
            sb.append(line).append('\n')
            depth += line.count { it == '(' } - line.count { it == ')' }
            i++
        } while (depth > 0 && i < lines.size && i - start < 20)
        return sb.toString()
    }

    /**
     * Row 9.2b — **C3's**, verified present rather than re-owned
     * ([ADR-093](../../../../../../../docs/DECISIONS.md#adr-093) row 3.8). C9 verifies the caret on
     * hardware and owns no code here; this asserts only that the assertion still exists, so deleting it
     * cannot quietly widen C9's fence into a gap.
     */
    @Test
    fun the_carets_reduced_motion_assertion_is_still_owned_by_c3() {
        val method = BenchC3Test::class.java.methods.firstOrNull {
            it.name == "the_caret_blinks_as_a_square_wave_and_holds_still_under_reduced_motion"
        }
        assertNotNull("ADR-093 row 3.8's assertion has gone missing", method)
    }

    // =================================================================================================
    // Rows 9.6, 9.7 — asserted by absence
    // =================================================================================================

    /**
     * Row 9.6 — the finished-book reveal is **not** on the Bench
     * ([BP-7](../../../../../../../docs/design/V2-BENCH-PRINCIPLES.md),
     * [ADR-058](../../../../../../../docs/DECISIONS.md#adr-058)). It stays on Read, because answering
     * *"what have I made?"* to a maker who arrived asking *"how do I change this page?"* is the exact
     * failure `0.9.0-beta.1` shipped.
     *
     * Asserted by absence over the Bench's own sources: the mutation is "add one", and adding one means
     * calling the Read act from a Bench file.
     */
    @Test
    fun no_read_like_surface_exists_in_the_bench() {
        val offenders = v2Surfaces()
            .filter { it.readText().contains("ProofReadAct") }
            .map { it.name }
        assertEquals("the reveal belongs to Read", emptyList<String>(), offenders)
    }

    /**
     * Row 9.7 — one engine: preview == export == read ([ADR-028](../../../../../../../docs/DECISIONS.md#adr-028)).
     * **Constitutional**: the gate is that C1–C6 added no second draw path, so the assertion is that the
     * existing parity test is still there and still the thing that proves it. It passing *unchanged* is
     * the whole-project regression's job, not this suite's.
     */
    @Test
    fun the_one_engine_parity_assertion_is_still_the_thing_that_proves_it() {
        val parity = PagePreviewParityTest::class.java.methods.count {
            it.isAnnotationPresent(Test::class.java)
        }
        assertTrue("the parity suite has been emptied", parity > 0)
    }

    // =================================================================================================
    // Row 9.5 — contrast measured over the grain, not over the flat token
    // =================================================================================================

    /**
     * Row 9.5 — the floors hold **at the grain's extremes**, not merely on the flat paper token.
     *
     * *Deviation from ADR-097's target, recorded rather than quietly relocated:* the row names
     * `ZinelyV2ContrastTest`, which lives in `core:ui`. The grain **strengths** are the Bench's
     * (`BenchStudio.PAGE_GRAIN_ALPHA`, `SCREEN_GRAIN_ALPHA`) and `core:ui` cannot see `feature:editor`,
     * so the assertion lives with the surface whose numbers it uses. The ★ token gate in
     * `ZinelyV2ContrastTest` is unchanged and still runs.
     *
     * The extremes are computed rather than sampled: soft-light at source luminance 0 and 1 bounds every
     * pixel the tile can contain, so this is worst-case by construction and does not depend on which
     * pixels the authored PNG happens to hold.
     *
     * **The first assertion is the one that carries the row.** The planned mutation — *measure on flat
     * paper* — cannot be caught by the floors alone, because flat paper already clears them: a test that
     * only checked the floors would stay green while measuring the wrong thing, which is
     * [ADR-087](../../../../../../../docs/DECISIONS.md#adr-087)'s equivalent mutant exactly. So the
     * instrument is asserted first: the grain must actually move the paper.
     */
    @Test
    fun the_text_floor_holds_over_the_grain_and_the_measurement_is_actually_over_the_grain() {
        listOf("light" to zinelyV2LightColors(), "dark" to zinelyV2DarkColors()).forEach { (theme, c) ->
            val darkest = softLightExtreme(c.paper, source = 0f, alpha = BenchStudio.PAGE_GRAIN_ALPHA)
            val lightest = softLightExtreme(c.paper, source = 1f, alpha = BenchStudio.PAGE_GRAIN_ALPHA)

            assertTrue(
                "$theme: the grain does not move the paper — this is measuring the flat token",
                darkest != c.paper && lightest != c.paper,
            )
            listOf("darkest grain" to darkest, "lightest grain" to lightest).forEach { (where, paper) ->
                val ratio = contrastRatio(c.ink, paper)
                assertTrue(
                    "$theme ink over $where: ${"%.3f".format(ratio)}:1 is below the AA body floor 4.5:1",
                    ratio >= AaBodyFloor,
                )
            }

            // The chrome half of the row: the weakest ink the Bench draws, over the ground it is actually
            // drawn on. `BenchPageGrid` writes each cell's page number in `inkSoft` (`:353`; it was
            // `inkFaint` until the 2026-08-12 AA fix, which is what the amendment below records) onto the
            // **cell**, whose ground is a flat `colors.paper` (`:287`). Two things about that ground are
            // easy to get wrong, and this row got both wrong before the device corrected it:
            //
            //  * it is **not `desk`**. `desk` (`:198`) fills the sheet *behind* the cells. An earlier
            //    version measured `inkFaint` on `desk`, got **2.880:1**, and filed it as a defect
            //    (D-061) against the ≥3:1 non-text floor. That pairing is drawn nowhere on screen.
            //  * it does **not** carry the *page's* grain. `PAGE_GRAIN_ALPHA` is the sheet's, and the
            //    correction to D-061 composited it over the cell — a surface the product does not draw.
            //    That second wrong model produced **2.817:1 in dark** and very nearly filed a second
            //    defect (D-062) on the strength of it.
            //
            // The device settled the reading, in both themes, on `SM-A176B`:
            //
            //     light  glyph (141,131,106) on cell (247,243,232)  ->  3.396:1
            //     dark   glyph (136,127,108) on cell  (50, 45, 36)  ->  3.449:1
            //
            // **The cell is not flat, and those very pixels are the proof.** `BenchPageGrid` applies no
            // grain of its own, but `benchStudioGround()` (`BenchStudioSurface.kt:321`) is applied to the
            // screen root and draws its grain *after* its content (`ZinelyV2Grain.kt:214`,
            // `drawContent()` then `drawRect`), so `SCREEN_GRAIN_ALPHA` lays over the grid like everything
            // else — `EditorScreen.kt:740` says so in terms. Grained, the ground's range is
            // `(246,240,227)..(248,243,233)` light and `(40,36,29)..(58,53,44)` dark, and both device
            // samples above sit inside those ranges rather than on the flat token. A third wrong model
            // was avoided only because independent review checked the claim against the ADR's own pixels.
            //
            // So this assertion is the **flat pair in both themes deliberately, not incidentally**, and
            // the reason is stated rather than assumed: worst-case-single-pixel is the right instrument
            // for the page's body ink above (a large glyph over a sheet the reader dwells on) and the
            // wrong one for a 9px decorative page number over a noise tile, where the extreme is one
            // pixel of tile and the reader sees the mean — which is the flat value, and which the device
            // measured over the whole badge box at 3.396 and 3.449. Applying the row's own
            // `softLightExtreme` method to the badge instead gives **2.969:1 light / 2.525:1 dark**,
            // below the ≥3:1 floor. Whether that floor binds a decorative page number at all, and whether
            // per-pixel worst case is the right instrument over grain, is **⏳ owner ruling required** —
            // D-064. This row asserts what it can defend and files what it cannot.
            //
            // The cell's dark ground is not a defect either: `v2-bench.html:138` rules it in terms —
            // *"THE PAGE GRID IS STILL NOT AMENDED. `.pgcell` draws no page content, so it has no
            // artifact to dim"* — so OD-23 considered this surface and withheld the light island from it.
            //
            // Four computed readings of one row, three of them wrong, and the hardware right the first
            // time. Being wrong once is not a reason to trust the correction.
            // ⚠ AMENDED 2026-08-12 — the badge is now `inkSoft`, and that change is what makes the
            // paragraph above stop being load-bearing.
            //
            // `inkFaint` measured **3.45:1 dark / 3.41:1 light** flat: over the ≥3:1 control floor, under
            // 1.4.3's 4.5:1 for 9sp text, and — by this row's own `softLightExtreme` instrument —
            // **2.969 / 2.525**, i.e. under every floor on the table. Which of those three numbers binds
            // a decorative page number was D-064's open question, and the honest reading is that the
            // token was passing only on the most generous instrument available.
            //
            // `inkSoft` measures **6.25:1 dark / 6.78:1 light** and clears 4.5:1 with room, so the answer
            // no longer depends on which instrument or which floor D-064 settles on. **That does not
            // resolve D-064** — the question of whether per-pixel worst case is the right instrument over
            // grain is still the owner's, and it still binds other badges. It removes this pairing from
            // the set of things riding on the answer.
            //
            // ⚠ **AMENDED AGAIN by ADR-102 P5 — the card is LIT, and the two themes are now one number.**
            // Everything above this line is the V2 card's history and is kept because it is the record of
            // three wrong models and one device measurement. What it describes no longer exists: the
            // V2.1 `.pgc` restates six on-paper tokens (`v21-bench.html:456-457`,
            // [benchGridCardIsland]), so *whatever room the app is in* the number is `#6E5947` on
            // `#FFF6E8` — **6.78:1** — and the frozen size is 11.52sp bold rather than 9sp. Two defects
            // closed from opposite ends: the token by the 2026-08-12 AA fix, the ground and the size by
            // the freeze.
            //
            // The room's own `inkSoft`/`paper` pairing is still asserted, unchanged, because the page
            // itself and every other chrome badge still draw it. The card's pairing is asserted *beside*
            // it, from the island, so a regression that un-lit the card fails here rather than passing on
            // the room's arithmetic.
            val ratio = contrastRatio(c.inkSoft, c.paper)
            assertTrue(
                "$theme cell badge on the cell's paper: ${"%.3f".format(ratio)}:1 " +
                    "is below the ≥3:1 control floor",
                ratio >= ControlFloor,
            )
            // The stricter floor, asserted separately so the failure message says which one broke: 9sp is
            // not large-scale text under 1.4.3 by any reading (≥18pt, or ≥14pt bold), so if this badge is
            // text at all, 4.5:1 is its floor. Kept as its own assertion rather than folded into the one
            // above because the two would then fail with the same message for different reasons.
            assertTrue(
                "$theme cell badge is ${"%.3f".format(ratio)}:1 — a 9sp number is not large-scale text, " +
                    "so 1.4.3 AA wants 4.5:1",
                ratio >= 4.5f,
            )
        }

        // The card's own pairing, in both rooms, from the island the cell actually reads. A single
        // expected value for the two themes is the assertion — it is what "the card is lit" *means*, and
        // a card that followed the room would give two different numbers here while giving one above.
        listOf(
            "from the light room" to benchGridCardIsland(zinelyV21LightColors()),
            "from the dark room" to benchGridCardIsland(zinelyV21DarkColors()),
        ).forEach { (where, card) ->
            val lit = contrastRatio(card.inkSoft, card.paper)
            assertTrue(
                "the lit card's number $where measures ${"%.3f".format(lit)}:1, below 1.4.3 AA's 4.5:1",
                lit >= 4.5f,
            )
        }

        // …and the pairing itself, because everything above is arithmetic over tokens and would go on
        // passing if the grid stopped drawing them. That is not hypothetical: D-061 *was* the assertion
        // computing a pairing the screen does not draw. A contrast number pins nothing unless something
        // pins which two colours it is about, so the source says so.
        //
        // ⚠ The two strings moved with P5, and the move is the point: the cell now paints from `card`,
        // not from `colors`. Pinning the old strings would have gone on passing while measuring the
        // panel's ground instead of the card's — the panel does still write `.background(colors.paper)`,
        // one screen object away, which is exactly how D-061 happened.
        val grid = File("src/main/kotlin/com/aritr/zinely/feature/editor/BenchPageGrid.kt").readText()
        assertTrue(
            "BenchPageGrid no longer fills its card from the lit island — the ratios above are now " +
                "measuring a pairing the grid does not draw, exactly as D-061 did",
            grid.contains("if (current) card.leafTint else card.paper"),
        )
        assertTrue(
            "BenchPageGrid no longer writes its page number in `card.inkSoft` — same failure, other side",
            grid.contains("if (current) card.leafText else card.inkSoft"),
        )
    }

    /**
     * Soft-light (the W3C formula Compose's [androidx.compose.ui.graphics.BlendMode.Softlight] and CSS
     * both implement), composited at [alpha] over [backdrop] — evaluated at one uniform source value so
     * that `source = 0` and `source = 1` bound the whole tile.
     */
    private fun softLightExtreme(backdrop: androidx.compose.ui.graphics.Color, source: Float, alpha: Float) =
        androidx.compose.ui.graphics.Color(
            red = blendChannel(backdrop.red, source, alpha),
            green = blendChannel(backdrop.green, source, alpha),
            blue = blendChannel(backdrop.blue, source, alpha),
        )

    /**
     * WCAG 2.x relative luminance and contrast ratio.
     *
     * Re-stated here rather than imported: `core:ui`'s `WcagContrast` is `internal` to that module's
     * **test** source set, so it is not visible from this one. Six lines of a published formula are a
     * better answer than promoting a test helper to production API for one caller.
     */
    private fun contrastRatio(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color): Float {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
    }

    private fun luminance(c: androidx.compose.ui.graphics.Color): Float {
        fun ch(v: Float) = if (v <= 0.03928f) v / 12.92f else Math.pow(((v + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        return 0.2126f * ch(c.red) + 0.7152f * ch(c.green) + 0.0722f * ch(c.blue)
    }

    private fun blendChannel(cb: Float, cs: Float, alpha: Float): Float {
        val blended = if (cs <= 0.5f) {
            cb - (1f - 2f * cs) * cb * (1f - cb)
        } else {
            val d = if (cb <= 0.25f) ((16f * cb - 12f) * cb + 4f) * cb else kotlin.math.sqrt(cb)
            cb + (2f * cs - 1f) * (d - cb)
        }
        return (cb + alpha * (blended - cb)).coerceIn(0f, 1f)
    }

    // =================================================================================================
    // Repository access — the seam these scans share
    // =================================================================================================

    /**
     * The module's `src/main` Kotlin sources.
     *
     * Robolectric runs with the module directory as the working directory. Asserted rather than
     * assumed: a scan that silently found no files would pass every absence test in this suite, which
     * is the failure mode [ADR-087](../../../../../../../docs/DECISIONS.md#adr-087) calls an equivalent
     * mutant wearing a green tick.
     */
    private fun mainSources(): List<File> {
        val root = File("src/main/kotlin/com/aritr/zinely/feature")
        assertTrue("source scan found nothing at ${root.absolutePath}", root.isDirectory)
        val files = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("source scan found no Kotlin files", files.size > 20)
        return files
    }

    /**
     * `core:copy`'s sources — where every shipped user-facing string in this app actually lives.
     *
     * Reached by a relative path out of the module, which is why it is asserted rather than assumed: a
     * scan that silently found nothing would pass row 9.1b while checking nothing at all.
     */
    private fun copySources(): List<File> {
        val root = File("../../core/copy/src/main/kotlin")
        assertTrue("core:copy scan found nothing at ${root.absolutePath}", root.isDirectory)
        val files = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("core:copy scan found no Kotlin files", files.isNotEmpty())
        return files
    }

    /**
     * The V2 surfaces: **everything in the feature package except the named V1 exclusions.**
     *
     * A first version was an allowlist (`Bench*` / `Editor*` / `Zine*`) documented as a denylist, which
     * silently left `TypeBar.kt`, `SelectionChrome.kt`, `SnapGuides.kt`, `EditTextSession.kt` and others
     * unscanned — a real V2 Bench surface among them. Review found it. Inverting the filter makes the
     * exclusion list the whole of the exception, which is what the KDoc always claimed.
     *
     * Excluded, and why: these are **V1** surfaces that Phase C does not re-skin, so binding them to V2's
     * motion class would be a change of behaviour outside C9's fence rather than a check on it. `core:ui`
     * is likewise unscanned: its animated components (`ZButton`, `ZSheet`, `ZSnackbar`, `ZToast`,
     * `ZSweep`) are V1 and run on `ZinelyMotion`, which honours reduced motion by its own collapse.
     */
    private fun v2Surfaces(): List<File> {
        val v1 = setOf(
            "ProofFold.kt", "ProofScreen.kt", "ProofRead.kt", "ProofSheet.kt", "ProofPrint.kt",
            "ShelfCover.kt", "ShelfSheets.kt", "ReframeControls.kt",
            "ReframeSession.kt", "ReframeOverlay.kt",
        )
        // ⚠ `ShelfStates.kt` and `ShelfCard.kt` used to sit in this set and are gone from it because the
        // **files** are gone — the V1 shelf they belonged to was deleted once `ZinelyNavHost` stopped
        // routing to it. A name left here would be inert either way, which is exactly why it would rot.
        //
        // ⚠ `ReframeControls.kt` stays excluded even though ADR-102 §12.16 re-skinned it to V2.1. The
        // exclusion is about **motion**, not palette: nothing in that file was bound to V2's motion class
        // by the re-skin, so scanning it would fail on a property the change never claimed. Moving it out
        // of this set is its own piece of work.
        val files = mainSources().filter { it.name !in v1 }
        assertTrue("V2 surface scan found nothing", files.size > 20)
        return files
    }
}
