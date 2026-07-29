package com.aritr.zinely.feature.editor.a11y

import android.graphics.Rect
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.feature.editor.AddPhotoActionLabel
import com.aritr.zinely.feature.editor.AddWordsActionLabel
import com.aritr.zinely.feature.editor.EditorEffectRunner
import com.aritr.zinely.feature.editor.EditorScreen
import com.aritr.zinely.feature.editor.EditorStore
import com.aritr.zinely.feature.editor.FirstPageInvitationHeadline
import com.aritr.zinely.feature.editor.HomeScreen
import com.aritr.zinely.feature.editor.HomeShelfEvent
import com.aritr.zinely.feature.editor.HomeZineCard
import com.aritr.zinely.feature.editor.ProofAct
import com.aritr.zinely.feature.editor.ProofPrimaryTestTag
import com.aritr.zinely.feature.editor.ProofScreen
import com.aritr.zinely.feature.editor.RedoActionLabel
import com.aritr.zinely.feature.editor.TraySectionLabel
import com.aritr.zinely.feature.editor.UndoActionLabel
import com.aritr.zinely.ui.a11y.PlatformA11yStop
import com.aritr.zinely.ui.a11y.platformTraversalStops
import com.aritr.zinely.ui.components.ZStampButton
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * CI-31 — **a traversal-order assertion per surface entry point**. Before this file the repository had no
 * ordering assertion anywhere; the counts of what it *did* assert instead live in
 * [CI-31](../../../../../../../../../docs/V1-CONFORMANCE-INVENTORY.md) and are deliberately not restated
 * here, per the Documentation Rule — they are a measurement of a moving tree and had already drifted once.
 *
 * ## What "logical order" means here, quoted from the documents that bind it
 *
 * - **Premium Checklist #64** ([ZINELY-DESIGN-SYSTEM §13.1](../../../../../../../../../docs/ZINELY-DESIGN-SYSTEM.md)):
 *   *"Reading order matches visual order matches focus order."* Three orders, one requirement — which is why
 *   each test below makes **two** assertions rather than one (see *Two assertions* below).
 * - **§4.5 Reading order:** *"The visual order and the accessibility order are **the same order**, and both
 *   match the order in which the user needs the information. If the screen reader's traversal surprises you,
 *   the layout is wrong — not the traversal."*
 * - **§11.6:** *"Reading order is the design's order (§4.5). Fixing traversal with an override is treating the
 *   symptom."*
 * - **[DESIGN-RULES](../../../../../../../../../docs/design/DESIGN-RULES.md) per-screen checklist:**
 *   *"Screen-reader pass: labels meaningful, **order logical**, decoration not announced (rule 9)."*
 *
 * **Scope note — this file is additive to §11 and reinterprets nothing.** [validation
 * A-8](../../../../../../../../../docs/ZINELY-DESIGN-SYSTEM-VALIDATION.md) records that §11 does not mention
 * **keyboard focus** order while Premium Checklist #64 requires focus order, and proposes adding it. That
 * amendment is not made here and is not assumed: what is asserted below is **traversal / reading order** —
 * the order §4.5 and §11.6 already bind in the accepted text — over the accessibility tree. Keyboard Tab
 * order is *not* asserted (see *What this does not prove*), so nothing here depends on A-8 landing either way.
 *
 * ## Which tree, and what that does and does not prove
 *
 * Asserted against the **platform `AccessibilityNodeInfo` tree** via [platformTraversalStops] — §11.3, *"the
 * platform's tree is the truth"* — reusing the CI-26 harness in [PlatformAccessibilityTree] rather than
 * Compose's merged semantics tree. That harness's KDoc carries the two measured bounds; the consequences for
 * *this* item are:
 *
 * - The platform tree publishes children in the composition's **declaration** order and sets **no**
 *   `traversalBefore` / `traversalAfter` re-sorting hints on any node of any surface below (measured, not
 *   assumed — every hint read back `UNDEFINED`). So the sequence asserted is the sequence a service walking
 *   this tree would follow, and it is *also* the declaration order. **A sequence assertion alone would
 *   therefore be a declaration-order snapshot, not a reading-order proof** — hence the second assertion.
 * - `getChildId` is a hidden framework method reached by reflection. It resolves under Robolectric; on a
 *   device it is blocked by hidden-API enforcement. This is a JVM check that runs before a device is
 *   involved, not a replacement for the `adb shell uiautomator dump` pass in CLAUDE.md device verification.
 *
 * ### Two assertions, because #64 names three orders
 *
 * 1. [assertTraversalOrder] — the ordered list of spoken stops the platform tree presents, compared
 *    element-by-element against a pinned list. Catches a reorder, an insertion, and a disappearance.
 *
 *    **Be exact about what that list is: it was read off the running composition, not lifted from a
 *    specification artifact.** No frozen prototype is parsed here and none is diffed against; the frozen
 *    `proof.html` that `ProofFold.kt:83` cites is the authority for the Proof's *content* and was not
 *    consulted by this file. So assertion 1 pins the **observed** order and detects that it *changed* — it
 *    does not, on its own, prove the observed order was ever the right one. Its value is regression
 *    detection plus the fact that each list below is small enough to read against the design and argue with;
 *    the clause that carries independent weight is assertion 2, which compares the order against the
 *    surface's own geometry rather than against a previous reading of itself.
 * 2. [assertReadingOrderIsVisualOrder] — that same accessibility order is **monotonic in visual reading
 *    order**, using the platform tree's *own* reported `boundsInScreen`: each stop is either on the same row
 *    as its predecessor and further right, or on a later row. This is the assertion with teeth — it is what
 *    fails when a control is moved without being re-declared, or re-declared without being moved, which is
 *    precisely the *"visual order and accessibility order are the same order"* clause of §4.5.
 *
 * Both assertions are shown to have teeth by the two guards at the top of the class: one injects a control
 * declared out of order, the other a control moved without being re-declared. The second guard also asserts
 * that assertion 1 *passes* on that broken layout — the concrete demonstration that a sequence check alone
 * would wave it through, and the reason CI-31 is not satisfied by a snapshot of declaration order.
 *
 * ### What this does not prove
 *
 * - **Not keyboard Tab order.** `EditorScreen`'s root uses `focusTarget()` — deliberately a focus stop with
 *   no accessibility semantics — so it is a Tab stop that by design never appears in this tree. Asserting Tab
 *   order needs a focus-traversal harness and, per A-8, a §11 clause to assert against; both are out of scope
 *   for a test-only item.
 * - **Not dialog surfaces.** The Shelf's three sheets and the Proof's paper/share choosers are `ZSheet`
 *   `Dialog` windows — a separate window with its own root, which [platformTraversalStops] (scoped to the
 *   Activity's hosted composition) does not reach. Their traversal order is uncovered; see the report.
 * - **Not every conditional state.** One representative state per entry point is pinned, chosen to be the
 *   state that entry point opens in. Later Fold steps, the Proof error pane, the Shelf's loading / empty /
 *   error branches and any selection-active Editor state are uncovered.
 * - **Not anything outside the window.** Every assertion below is bounded by the `@Config` window: content
 *   that does not fit is not in the platform tree, and its absence looks exactly like a stop that does not
 *   exist. This is not hypothetical — it is what the first revision of this file got wrong (see the `@Config`
 *   note). One phone window is asserted; a tablet, a fold, and a large text scale are not.
 *
 * ### Two `src/main` defects this test *found* and deliberately does not fix
 *
 * `src/main` is out of scope for CI-31, so both are **reported, not fixed** — the same STOP-condition
 * [ZButtonPlatformA11yTest] applied to the merged-vs-platform `Role` divergence it found. Each now has its
 * own item in [the inventory](../../../../../../../../../docs/V1-CONFORMANCE-INVENTORY.md) so that ticking
 * CI-31 cannot close them by association:
 *
 * - **CI-96 — the blank-page invitation's sticker cluster is announced.** `EditorEmptyState.kt:89-91`'s three
 *   glyphs (`✿`, `❀`, `★`) reach the platform tree as three separate spoken stops. The sharp part is not the
 *   DESIGN-RULES deviation (*"decoration not announced"*) but that `EditorEmptyState.kt:86-87` carries an
 *   in-code comment stating the cluster is *"not announced to screen readers (purely ornamental)"* — **the
 *   platform tree falsifies the invariant the code documents about itself.** They are pinned below as the
 *   current truth rather than filtered out: a test that hid them would make both the checklist clause and
 *   that comment unfalsifiable, which is how the defect survived this long.
 * - **CI-97 — `ProofPrint.kt:300-309`'s "Change" is an unroled control.** A `Box` with `.clickable()` and no
 *   `Role`, so the platform reports `android.view.View` rather than `android.widget.Button`. It *is* a
 *   traversal stop — the platform flags it `isScreenReaderFocusable` because it is clickable; only its
 *   *label* comes from the child text. This is the [ADR-059](../../../../../../../../../docs/DECISIONS.md)
 *   Role→View family, met on a third surface.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// The same phone window every golden test in this module is captured at. Robolectric's default window is
// 320x470dp, which is smaller than any shipped phone and silently ignores a `Modifier.size` asking for more:
// content simply falls outside it and never reaches the accessibility tree at all. An earlier revision of
// this file ran at that default and the Proof Fold act's step navigation — the control its own asserted copy
// tells the user to tap — was below the viewport and therefore absent from every assertion here.
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class SurfaceTraversalOrderTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // ---------------------------------------------------------------------------------------------
    // The two assertions
    // ---------------------------------------------------------------------------------------------

    /**
     * Assertion 1 — the platform tree presents exactly [expected], in exactly that order.
     *
     * Compared as whole lists so a failure prints both sequences side by side: an inserted, missing, or
     * transposed stop is legible at a glance instead of arriving as an index mismatch.
     */
    private fun assertTraversalOrder(surface: String, expected: List<String>): List<PlatformA11yStop> {
        composeRule.waitForIdle()
        val stops = platformTraversalStops(composeRule.activity)
        assertEquals(
            "$surface: the platform accessibility tree's traversal order is not the design's order " +
                "(Premium Checklist #64, §4.5)",
            expected,
            stops.map { it.label },
        )
        return stops
    }

    /**
     * Assertion 2 — the traversal order is monotonic in visual reading order (§4.5), judged on the bounds the
     * **platform tree itself** reports for each stop.
     *
     * Two stops count as sharing a row when their vertical extents overlap by more than half the height of
     * the shorter one; that tolerance is what lets a tall grid card and the short `⋯` button beside it read
     * as one row, and what keeps a 1px bounds coincidence between stacked labels from being mistaken for one.
     * Within a row the next stop must not start left of its predecessor; across rows it must not start above.
     */
    private fun assertReadingOrderIsVisualOrder(surface: String, stops: List<PlatformA11yStop>) {
        assertTrue("$surface: expected at least two stops to have an order at all", stops.size >= 2)
        stops.zipWithNext().forEach { (previous, next) ->
            val a = previous.boundsInScreen
            val b = next.boundsInScreen
            if (sharesRow(a, b)) {
                assertTrue(
                    "$surface: “${next.label}” is reached after “${previous.label}” but is drawn to its " +
                        "LEFT on the same row (${b.left} < ${a.left}) — visual order and accessibility " +
                        "order disagree (§4.5). $a then $b",
                    b.left >= a.left,
                )
            } else {
                assertTrue(
                    "$surface: “${next.label}” is reached after “${previous.label}” but is drawn ABOVE it " +
                        "(${b.top} < ${a.top}) — visual order and accessibility order disagree (§4.5). " +
                        "$a then $b",
                    b.top >= a.top,
                )
            }
        }
    }

    /** True when [a] and [b] overlap vertically by more than half the shorter one's height. */
    private fun sharesRow(a: Rect, b: Rect): Boolean {
        val overlap = minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)
        val shorter = minOf(a.height(), b.height())
        return overlap * 2 > shorter
    }

    /**
     * Run both assertions — the pair is the CI-31 check; neither half is the check on its own — and then
     * assert the geometry half was **not vacuous on this surface**.
     *
     * [assertReadingOrderIsVisualOrder] has two branches, and a surface laid out as one strict column would
     * only ever take the vertical one: every check would reduce to "each stop is lower than the last", which
     * a broken left-right ordering cannot fail. The two guards at the top of this class prove the *helpers*
     * have teeth on synthetic fixtures; they say nothing about whether the teeth engage *here*. So each
     * surface must contain at least one consecutive pair that shares a row — a top bar, a grid row, an action
     * bar. If a future layout change flattens a surface into a single column, this fails loudly and asks for
     * a decision, rather than leaving a green assertion that has quietly stopped testing anything.
     */
    private fun assertLogicalTraversalOrder(surface: String, expected: List<String>) {
        val stops = assertTraversalOrder(surface, expected)
        assertReadingOrderIsVisualOrder(surface, stops)
        assertTrue(
            "$surface: no two consecutive stops share a row, so the horizontal branch of the reading-order " +
                "check never ran and this surface's geometry assertion proves only that stops descend. " +
                "Either the layout lost its rows or the fixture is not showing them.",
            stops.zipWithNext().any { (a, b) -> sharesRow(a.boundsInScreen, b.boundsInScreen) },
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Non-vacuity — the net is shown to catch an injected divergence, permanently
    // ---------------------------------------------------------------------------------------------

    /**
     * A divergence of the first kind: a control **re-declared out of order**. Two design-system buttons whose
     * specification says Alpha then Beta, declared Beta then Alpha. [assertTraversalOrder] must fail.
     *
     * Kept in the suite rather than performed once in a commit that is then reverted, because the C1
     * principle is that catching an injected divergence *is* the gate — a passing suite proves nothing about
     * a net. This guard, and the moved-control guard below it, re-earn that proof on every run.
     */
    @Test
    fun `traversal order check catches a control declared out of order`() {
        composeRule.setContent {
            ZinelyTheme {
                Column {
                    ZStampButton("Beta", {})
                    ZStampButton("Alpha", {})
                }
            }
        }
        assertThrows(AssertionError::class.java) {
            assertTraversalOrder("injected reorder", listOf("Alpha", "Beta"))
        }
    }

    /**
     * A divergence of the second kind, and the one that matters: a control **moved without being
     * re-declared**. Declaration order is still Alpha then Beta, so [assertTraversalOrder] passes — and it is
     * asserted here that it passes, because the point is that the sequence check alone would have waved this
     * screen through. Beta is drawn *above* Alpha, so [assertReadingOrderIsVisualOrder] must fail: visual
     * order and accessibility order have come apart, which is exactly what §4.5 forbids.
     */
    @Test
    fun `reading order check catches a control moved without being re-declared`() {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.size(300.dp, 240.dp)) {
                    ZStampButton("Alpha", {}, modifier = Modifier.offset(y = 120.dp))
                    ZStampButton("Beta", {}, modifier = Modifier.offset(y = 0.dp))
                }
            }
        }
        // The sequence check is blind to this — declaration order was never touched.
        val stops = assertTraversalOrder("injected move", listOf("Alpha", "Beta"))
        assertThrows(AssertionError::class.java) {
            assertReadingOrderIsVisualOrder("injected move", stops)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Entry point 1 · the Shelf (`HomeRoute`) — "Which zine do I want?"
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `shelf traversal order is the design's order and the visual order`() {
        val events = MutableSharedFlow<HomeShelfEvent>(extraBufferCapacity = 8)
        composeRule.setContent {
            ZinelyTheme {
                CompositionLocalProvider(LocalZinelyMotion provides ZinelyMotion(reduceMotion = true)) {
                    HomeScreen(
                        loading = false,
                        storeEmpty = false,
                        cards = listOf(
                            HomeZineCard("zine-b", "Summer scraps", "8-page mini · Letter", "Edited just now"),
                            HomeZineCard("zine-a", "Cat facts", "8-page mini · A4", "Edited 3 days ago"),
                        ),
                        events = events,
                        onOpenZine = {}, onStartZine = {}, onRenameZine = { _, _ -> },
                        onDuplicateZine = {}, onDeleteZine = {}, onDeleteUndo = {}, onDeleteCommit = {},
                    )
                }
            }
        }
        // Chrome, then the head, then the grid row-major (each card immediately followed by its own `⋯`,
        // never all covers then all menus), then the dock — which paints last and reads last.
        assertLogicalTraversalOrder(
            "Shelf",
            listOf(
                Copy.Common.BRAND,
                Copy.Shelf.ON_THIS_DEVICE,
                Copy.Shelf.YOUR_ZINES,
                "2",
                Copy.Shelf.cardOpenLabel("Summer scraps"),
                Copy.Shelf.actionsFor("Summer scraps"),
                Copy.Shelf.cardOpenLabel("Cat facts"),
                Copy.Shelf.actionsFor("Cat facts"),
                Copy.Common.START_A_ZINE,
            ),
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Entry point 2 · the Editor (`EditorRoute`) — "How do I change this page?"
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `editor traversal order is the design's order and the visual order`() {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        val store = EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
                ),
            ),
            CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, runner,
        )
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(
                    store = store,
                    pageSizePt = PtSize(100.0, 100.0),
                    // Pin the two one-shot coach marks off: this asserts the steady state of the surface,
                    // not the first-run overlay, which has its own tests.
                    moveResizeHintSeen = true,
                    // Production wires `onPreview` (ZinelyNavHost) — the existing Editor fixtures leave it
                    // null, which silently removes the top row. The entry point's real order includes it.
                    onPreview = {},
                )
            }
        }
        assertLogicalTraversalOrder(
            "Editor",
            listOf(
                // Chrome above the work.
                Copy.Editor.PREVIEW,
                // The blank-page invitation. The three glyphs are decoration that is currently announced —
                // pinned, not filtered; see the class KDoc.
                "✿", "❀", "★",
                FirstPageInvitationHeadline,
                Copy.EmptyState.SUPPLY_CUE,
                Copy.EmptyState.OFFLINE_NOTE,
                // The supply tray: its `heading()` first, then the four supplies left-to-right.
                TraySectionLabel,
                AddPhotoActionLabel,
                AddWordsActionLabel,
                UndoActionLabel,
                RedoActionLabel,
                // The page strip, last and lowest.
                Copy.PageStrip.pageNumber(1),
            ),
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Entry point 3 · the Proof (`ProofRoute`) — one destination, four acts, each its own entry state
    // ---------------------------------------------------------------------------------------------

    /** Eight pages in document order, each carrying a word so the Read act has something to render. */
    private fun proofPages(count: Int = 8): List<Page> = (0 until count).map { i ->
        Page(
            index = i,
            role = if (i == 0) PageRole.FRONT_COVER else PageRole.INTERIOR,
            elements = listOf(
                TextElement(
                    id = "t$i",
                    transform = Transform(20.0, 20.0, 160.0, 40.0),
                    text = "page ${i + 1}",
                ),
            ),
        )
    }

    private fun setProof(startAct: ProofAct) {
        composeRule.setContent {
            ZinelyTheme {
                ProofScreen(
                    zineName = "Corner Store Poems",
                    onBack = {},
                    pages = proofPages(),
                    pageSizePt = PtSize(200.0, 300.0),
                    defaults = DocumentDefaults(),
                    startAct = startAct,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun advanceAct() {
        composeRule.onNodeWithTag(ProofPrimaryTestTag).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `proof read act traversal order is the design's order and the visual order`() {
        // The landing act since ADR-058 — "What have I made?" is the first question the Proof answers.
        setProof(ProofAct.READ)
        assertLogicalTraversalOrder(
            "Proof · Read",
            listOf(
                Copy.Proof.BACK_TO_BENCH_SAVED,
                "Corner Store Poems",
                Copy.Proof.ACT_READ,
                // The pager is one stop: its page cards are `clearAndSetSemantics {}` by design.
                Copy.ProofRead.CONTENT_DESCRIPTION,
                Copy.ProofRead.pageOf(1, 8),
                Copy.Proof.PRINT_AND_FOLD,
            ),
        )
    }

    @Test
    fun `proof sheet act traversal order is the design's order and the visual order`() {
        setProof(ProofAct.SHEET)
        assertLogicalTraversalOrder(
            "Proof · Sheet",
            listOf(
                // The Sheet is the one act whose back button says "your zine", not "the bench".
                Copy.Proof.BACK_TO_YOUR_ZINE,
                "Corner Store Poems",
                Copy.Proof.ACT_SHEET,
                Copy.ProofSheet.TITLE,
                Copy.ProofSheet.BODY,
                // The imposed sheet: one Image stop; the eight panels, creases and legend are cleared.
                Copy.ProofSheet.CONTENT_DESCRIPTION,
                // The two panel labels drawn under the sheet, left then right.
                Copy.ProofSheet.FRONT_COVER,
                Copy.ProofSheet.BACK_COVER,
                Copy.Proof.PRINT_SETUP,
            ),
        )
    }

    @Test
    fun `proof print act traversal order is the design's order and the visual order`() {
        setProof(ProofAct.SHEET)
        advanceAct()
        assertLogicalTraversalOrder(
            "Proof · Print",
            listOf(
                Copy.Proof.BACK_TO_BENCH_SAVED,
                "Corner Store Poems",
                Copy.Proof.ACT_PRINT,
                Copy.ProofPrint.TITLE,
                Copy.ProofPrint.BODY,
                // Each recipe row reads label-then-value; the row's icon chip is cleared.
                Copy.ProofPrint.SCALE_LABEL,
                Copy.ProofPrint.SCALE_VALUE + Copy.ProofPrint.SCALE_EMPHASIS,
                Copy.ProofPrint.ORIENTATION_LABEL,
                Copy.ProofPrint.LANDSCAPE + Copy.ProofPrint.ORIENTATION_EMPHASIS,
                Copy.ProofPrint.PAPER_LABEL,
                Copy.Paper.A4,
                // "Change" is trailing *within* the Paper row — same row, further right.
                Copy.ProofPrint.CHANGE,
                Copy.ProofPrint.SIDES_LABEL,
                Copy.ProofPrint.SIDES_VALUE,
                // One TextView built from three spans; the platform reads the flattened string.
                Copy.ProofPrint.SIDES_HELP_PREFIX + Copy.ProofPrint.SIDES_HELP_BOLD +
                    Copy.ProofPrint.SIDES_HELP_SUFFIX,
                // The two export affordances, left then right, below the recipe.
                Copy.ProofPrint.SAVE_PDF,
                Copy.ProofPrint.SHARE,
                // Secondary precedes primary in the action bar, and is drawn to its left.
                Copy.Proof.BACK,
                Copy.Proof.NOW_FOLD_IT,
            ),
        )
    }

    @Test
    fun `proof fold act traversal order is the design's order and the visual order`() {
        setProof(ProofAct.SHEET)
        advanceAct()
        advanceAct()
        assertLogicalTraversalOrder(
            "Proof · Fold",
            listOf(
                Copy.Proof.BACK_TO_BENCH_SAVED,
                "Corner Store Poems",
                Copy.Proof.ACT_FOLD,
                Copy.ProofFold.INTRO_TITLE,
                Copy.ProofFold.INTRO_BODY,
                // The diagram announces the step it illustrates, and precedes the step's own caption.
                Copy.ProofFold.STEP1_TITLE,
                Copy.ProofFold.stepHeading(1, Copy.ProofFold.STEP1_TITLE),
                Copy.ProofFold.STEP1_BODY,
                // The step navigation — "tap the arrow when a step is done" names these two. On step 1 the
                // prev arrow is present but disabled, and the next arrow gives way to the action bar's
                // finish button only on the last step (`ProofFold.kt:215-236`).
                Copy.ProofFold.PREVIOUS_STEP,
                Copy.ProofFold.NEXT_STEP,
            ),
        )
    }
}
