package com.aritr.zinely.feature.editor.a11y

import android.graphics.Rect
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.aritr.zinely.ui.components.ZSheetSurfaceTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import com.aritr.zinely.feature.editor.ProofDrawer
import com.aritr.zinely.feature.editor.ProofScreen
import com.aritr.zinely.feature.editor.AddActionLabel
import com.aritr.zinely.feature.editor.RedoActionLabel
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
 * - **Not dialog surfaces.** The Shelf's three sheets and the Proof's two drawers are `ZSheet`
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
 * ### Two `src/main` defects this test *found* and deliberately did not fix
 *
 * `src/main` was out of scope for CI-31, so both were **reported, not fixed** — the same STOP-condition
 * [ZButtonPlatformA11yTest] applied to the merged-vs-platform `Role` divergence it found. Each got its
 * own item in [the inventory](../../../../../../../../../docs/V1-CONFORMANCE-INVENTORY.md) so that ticking
 * CI-31 could not close them by association:
 *
 * - **CI-96 — CLOSED 2026-08-14 by [ADR-102](../../../../../../../../../docs/DECISIONS.md#adr-102-p3-p8).**
 *   The finding was that the blank-page invitation's three glyphs (`✿`, `❀`, `★`) reached the platform tree
 *   as three separate spoken stops while `EditorEmptyState`'s own KDoc claimed the cluster *"adds nothing
 *   to the a11y tree"* — **the platform tree falsifying the invariant the code documented about itself.**
 *   They were pinned in the expected list below as the current truth rather than filtered out, precisely so
 *   that closing the defect would have to come here and say so; a test that hid them would have made both
 *   the checklist clause and that comment unfalsifiable, which is how the defect survived as long as it did.
 *
 *   `SupplyCluster`'s `Row` now carries `clearAndSetSemantics {}`, so the three stops are gone and the
 *   glyphs are struck from the list. ⚠ **The route by which it was finally fixed is worth more than the
 *   fix**: a device pass ran `adb shell uiautomator dump` on hardware and read the same three `TextView`s
 *   this test had been pinning for weeks. Two independent readers, one booked defect, and neither of them
 *   the merged semantics tree that `EditorEmptyStateTest` reads — which is why that suite now carries
 *   `the_supply_cluster_is_silent` as well. The pin worked exactly as designed.
 * - **CI-97 — CLOSED by ADR-101 P3.** The finding was that the print recipe's "Change" button was a `Box`
 *   with `.clickable()` and no `Role`, so the platform reported `android.view.View`. P3 deleted the
 *   control with the paper chooser sheet it opened. The [ADR-059](../../../../../../../../../docs/DECISIONS.md)
 *   Role→View family did **not** close with it: `ProofPaperSegmentsA11yTest` measures the segmented control
 *   that replaced it reaching the platform as `android.view.View` even though it declares
 *   `Role.RadioButton` — the same family, met by a control that does everything right.
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
    /**
     * The drawer counterpart to [assertLogicalTraversalOrder], reading the **semantics** tree instead of
     * the platform one, and restricted to the nodes inside a `ZSheet`.
     *
     * Weaker than the platform assertion by design, and the weakness is the point of the doc comment on
     * the print-details test: a `Dialog` is a separate window, so `platformTraversalStops(activity)`
     * cannot see it. Compose's semantics tree can, so the *order* of a drawer's labelled content is still
     * checkable here — what is not checkable is how the platform will flatten it for TalkBack, which is
     * why device Pass 1 owns that half.
     *
     * Order is taken from the composition, then asserted to agree with visual top-to-bottom / left-to-
     * right reading order, so a drawer whose DOM order and drawn order disagree still fails.
     */
    private fun assertLogicalContentOrder(
        surface: String,
        expected: List<String>,
        checkGeometry: Boolean = true,
    ) {
        composeRule.waitForIdle()
        val labelled = SemanticsMatcher("has a label") { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.isNotEmpty() == true ||
                node.config.getOrNull(SemanticsProperties.ContentDescription)?.isNotEmpty() == true
        }
        val inSheet = composeRule.onAllNodes(labelled).fetchSemanticsNodes().filter { node ->
            generateSequence(node) { it.parent }
                .any { it.config.getOrNull(SemanticsProperties.TestTag) == ZSheetSurfaceTestTag }
        }
        val labels = inSheet.map { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
                ?: node.config.getOrNull(SemanticsProperties.Text)!!.joinToString("") { it.text }
        }
        assertEquals(
            "$surface: the drawer's content order is not the design's order (§4.5). This reads the " +
                "semantics tree, not the platform tree — a ZSheet is a Dialog, i.e. its own window.",
            expected,
            labels,
        )
        // The same visual-order agreement the platform assertion makes, on the bounds Compose reports.
        // Labels are walked in lockstep with the nodes: an earlier draft called `inSheet.indexOf(...)`
        // inside the message string, which built the message on every iteration whether or not the
        // assertion failed, and scanned the list to do it.
        //
        // **`positionInRoot`, not `boundsInRoot`, and the difference is the whole reason this half of the
        // assertion used to be switched off.** `boundsInRoot` is *clipped* by every scrolling ancestor, so
        // a node below the fold of a scrollable panel collapses to `Rect.Zero` — and a rect at the origin
        // reads as "drawn at the very top", making every off-screen item look like it jumped above its
        // predecessor. That is a property of the instrument, not of the layout: the node is laid out
        // exactly where the design puts it and would be reached by one scroll. `positionInRoot` is
        // unclipped, so the comparison is about the layout rather than about the current scroll offset.
        if (!checkGeometry) return
        val pairs = inSheet.zip(labels).zipWithNext()
        // The same non-vacuity guard [assertLogicalTraversalOrder] carries, and the P3 review was right
        // that its absence here was an inconsistency rather than an exemption: without it, a drawer whose
        // content happened to stack in a single column would run only the vertical branch, and the
        // horizontal check could rot away unnoticed. Both drawers do share rows today — the title with its
        // close button, A4 with US Letter — so the guard costs nothing and keeps them there.
        assertTrue(
            "$surface: no two consecutive labelled nodes share a row, so the horizontal branch of the " +
                "reading-order check never ran and this assertion proves only that content descends.",
            pairs.any { (before, after) ->
                val (a, b) = before.first to after.first
                val overlap = minOf(a.positionInRoot.y + a.size.height, b.positionInRoot.y + b.size.height) -
                    maxOf(a.positionInRoot.y, b.positionInRoot.y)
                overlap * 2 > minOf(a.size.height, b.size.height)
            },
        )
        pairs.forEach { (before, after) ->
            val (previous, previousLabel) = before
            val (next, nextLabel) = after
            val a = previous.positionInRoot
            val b = next.positionInRoot
            val aHeight = previous.size.height.toFloat()
            val bHeight = next.size.height.toFloat()
            val overlap = minOf(a.y + aHeight, b.y + bHeight) - maxOf(a.y, b.y)
            val sharesRow = overlap * 2 > minOf(aHeight, bHeight)
            if (sharesRow) {
                assertTrue(
                    "$surface: “$nextLabel” follows “$previousLabel” but is drawn to its LEFT on the " +
                        "same row",
                    b.x >= a.x,
                )
            } else {
                assertTrue(
                    "$surface: “$nextLabel” follows “$previousLabel” but is drawn ABOVE it",
                    b.y >= a.y,
                )
            }
        }
    }

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
                // The blank-page invitation. The three sticker glyphs used to sit here — decoration the
                // platform announced, pinned rather than filtered. CI-96, closed 2026-08-14; see the class
                // KDoc. The invitation now opens on its headline, as it always claimed to.
                FirstPageInvitationHeadline,
                Copy.EmptyState.SUPPLY_CUE,
                Copy.EmptyState.OFFLINE_NOTE,
                // C5 (ADR-095 rows 5.1, 5.2, 5.9): the navigation row comes BEFORE the bar, because the
                // freeze puts it there — `v2-bench.html:481` opens `.navrow` and `:488` opens `.bar`, both
                // in `.phone`'s normal flow, so the sheets sit above Undo/Redo/Add/Done. This ordering is
                // the assertion: C5 first shipped the two rows inverted and no test noticed, since each
                // row was only ever checked against itself. Grid button ahead of the sheets, which is both
                // the design's order and the row's visual order.
                Copy.PageNav.ALL_PAGES,
                // A one-page document's only sheet is its front cover: covers are a matter of position
                // (`benchCoverAt`), and position 1 is the front. `i===1||i===NP` in the freeze is true of
                // both clauses here, and the front reading wins.
                Copy.PageNav.frontCoverLabel(1, 1),
                // C4 (ADR-094 row 4.5): the frozen bottom bar, left to right. `EditorSupplyTray`'s
                // "Supplies" heading and its four cards are gone with the shelf — the bar has no heading
                // because the freeze gives it none, and its two add verbs now live behind `Add`.
                UndoActionLabel,
                RedoActionLabel,
                AddActionLabel,
                Copy.EditText.DONE,
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

    private fun setProof(startDrawer: ProofDrawer = ProofDrawer.None) {
        composeRule.setContent {
            ZinelyTheme {
                ProofScreen(
                    onBack = {},
                    pages = proofPages(),
                    pageSizePt = PtSize(200.0, 300.0),
                    defaults = DocumentDefaults(),
                    startDrawer = startDrawer,
                )
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * The Proof's surface traversal — [ADR-101](../../../../../../../../docs/DECISIONS.md#adr-101) P1.
     *
     * This replaces four separate act-order assertions (Read / Sheet / Print / Fold). There are no longer
     * four traversals to check, because there are no longer four screens: the surface is the reader, and
     * the print details and the fold guide are drawers over it. Each drawer keeps its own traversal test
     * below, which is the stricter arrangement — a drawer is a separate window, so its order is checked
     * in isolation rather than as a suffix of the screen's.
     *
     * **The open question this list was holding is closed.** It used to assert the zine name and the act
     * status line — the two stops the frozen design deletes — precisely so that dropping them could not
     * happen quietly. The owner answered in favour of the frozen bar (ADR-101 §6.11), and the list moved
     * with it. That is what the mechanism was for: the removal cost a decision and a diff, not silence.
     */
    @Test
    fun `proof surface traversal order is the design's order and the visual order`() {
        setProof()
        assertLogicalTraversalOrder(
            "Proof · surface",
            listOf(
                Copy.Proof.BACK_TO_BENCH_SAVED,
                // ADR-101 P6: the frozen top bar, and the answer to the question the previous version of
                // this list was holding open. The zine name and the act status line are gone; the
                // `.pcount` ticket stands between the two icon buttons and is the surface's one page
                // readout. It is spoken as written — "Cover · 1 of 8" — and drawn in caps, which is why
                // its stop here is the sentence-case string.
                Copy.ProofRead.leafLabel(1, 8),
                Copy.Proof.HOW_TO_FOLD,
                // ADR-101 P5: the reader is a booklet, not a pager. The leaf itself is not a stop — it is
                // `clearAndSetSemantics {}`, because a Canvas of the user's ink has nothing to read aloud
                // — and **the stage carries no label either**: with a `contentDescription` on it, both
                // turn edges were absent from the platform tree while every Compose-semantics assertion
                // passed, and this list is the only thing that saw it. (Not a general law — `StepDots` is
                // a labelled container whose children do reach the platform. The general part is that
                // nothing except the platform tree can tell you.) The edges are declared left-leaf-right
                // so that traversal and the visual row agree (§4.5).
                Copy.ProofRead.PREVIOUS_PAGE,
                Copy.ProofRead.NEXT_PAGE,
                // The band (ADR-101 P2). `.ready` is a single stop carrying its own two lines — the
                // frozen `aria-label="Print details"` would have replaced them, so the destination is on
                // the click action instead and does not appear here. Then the commit pair, left to right.
                Copy.Proof.readyLabel(8, Copy.Paper.A4),
                Copy.Proof.SAVE_PDF,
                Copy.Proof.SHARE,
            ),
        )
    }

    /**
     * **The two drawer traversal tests that belong here do not exist, and the reason is a limitation of
     * this instrument rather than a gap in the design.**
     *
     * `platformTraversalStops(composeRule.activity)` reads the **activity window's**
     * `AccessibilityNodeInfo` tree. A [com.aritr.zinely.ui.components.ZSheet] drawer is hosted in a
     * `Dialog`, which is a *separate window* — so with a drawer open this harness still returns the
     * surface behind it, and an assertion written against the drawer's expected order fails while
     * reporting the reader's stops. Both drawer tests were written, run, and read exactly that way before
     * the cause was found; they are not kept, because a test that silently checks the wrong window is
     * worse than no test.
     *
     * Window-level modality is the *reason* to use `ZSheet` — TalkBack isolation and focus containment
     * come free with it — so this is a cost of the right choice, not an argument against it. Drawer
     * traversal is therefore verified where a separate window can actually be read: **device Pass 1**,
     * via `adb shell uiautomator dump`, which walks every window. ADR-101 P6 carries it.
     */
    @Test
    fun `proof print-details content order is the design's order in the composition`() {
        setProof(ProofDrawer.Details)
        assertLogicalContentOrder(
            "Proof · print details",
            listOf(
                Copy.Proof.PRINT_DETAILS,
                Copy.Proof.CLOSE,
                // 1. The paper question, answered inline by the segmented control (P3 deleted the
                //    chooser sheet). Both options are stops; the selected one is announced selected.
                Copy.ProofPrint.SECT_PAPER.uppercase(),
                Copy.Paper.A4,
                Copy.Paper.LETTER,
                // 2. What the app already handled — the honest replacement for the frozen `ALL SET`
                //    checklist, whose two false ticks P3 declined to draw. Three plain facts about the
                //    artifact; the bullet discs are cleared. `PAPER_HINT` moved here from §1.
                Copy.ProofPrint.SECT_ALREADY.uppercase(),
                Copy.ProofPrint.PAPER_HINT,
                Copy.ProofPrint.ALREADY_CUT,
                Copy.ProofPrint.ALREADY_MARGIN,
                // 3. ADR-052's recipe, which the frozen panel does not contain at all. The hint is what
                //    gives "at the print dialog" a referent — this app never opens one.
                Copy.ProofPrint.SECT_DIALOG.uppercase(),
                Copy.ProofPrint.DIALOG_HINT,
                // Each recipe row reads label-then-value; the row's icon chip is cleared.
                Copy.ProofPrint.SCALE_LABEL,
                Copy.ProofPrint.SCALE_VALUE + Copy.ProofPrint.SCALE_EMPHASIS,
                Copy.ProofPrint.ORIENTATION_LABEL,
                Copy.ProofPrint.LANDSCAPE + Copy.ProofPrint.ORIENTATION_EMPHASIS,
                Copy.ProofPrint.PAPER_LABEL,
                Copy.Paper.A4,
                Copy.ProofPrint.SIDES_LABEL,
                Copy.ProofPrint.SIDES_VALUE,
                // One TextView built from three spans; the platform reads the flattened string.
                Copy.ProofPrint.SIDES_HELP_PREFIX + Copy.ProofPrint.SIDES_HELP_BOLD +
                    Copy.ProofPrint.SIDES_HELP_SUFFIX,
                // 4. The test-sheet card — one node, bold lead and body merged into one sentence.
                Copy.ProofPrint.SECT_TEST.uppercase(),
                Copy.ProofPrint.TEST_SHEET_LEAD + Copy.ProofPrint.TEST_SHEET_BODY,
                // 5. What the imposition is, illustrated by the sheet. Save PDF and Share are no longer
                //    here at all — P2 moved them to the band's `.commit` row.
                Copy.ProofPrint.SECT_BOOKLET.uppercase(),
                Copy.ProofPrint.bookletHint(8),
                // The imposed sheet: one Image stop; the eight panels, creases and legend are cleared.
                Copy.ProofSheet.CONTENT_DESCRIPTION,
                // The caption that pre-empts "the export is broken", read after the picture causes it.
                Copy.ProofSheet.SCRAMBLED_CAPTION,
                // The honesty legend, which P3 stopped hiding from screen readers: it carries facts
                // nothing else on the panel states, and `aria-hidden` was only defensible while it was
                // decoration beside a labelled image.
                Copy.ProofSheet.LEGEND_FOLD_LINES + Copy.ProofSheet.LEGEND_ONE_CUT +
                    Copy.ProofSheet.LEGEND_PRINTER_REACH,
                // The two panel labels drawn under the sheet, left then right.
                Copy.ProofSheet.FRONT_COVER,
                Copy.ProofSheet.BACK_COVER,
                // The panel's closing pointer into the fold drawer — the section titled "how it becomes
                // a booklet" used to end at a picture and decline to say how.
                Copy.ProofPrint.SEE_HOW_TO_FOLD,
            ),
            // **Geometry is asserted again — and turning it back on is what found why it was really off.**
            // The stated reason had been P1's two stacked scrollers. That was a guess: the actual cause is
            // that the check read `boundsInRoot`, which is clipped, so *any* content below the fold of
            // *any* scrollable panel collapsed to `Rect.Zero` and read as drawn at the top. One panel
            // instead of two does not fix that — this panel is taller than its viewport too. Reading
            // `positionInRoot` does, and it makes the assertion mean what §4.5 says: the design's order is
            // the drawn order, whatever happens to be scrolled into view at the moment of the test.
        )
    }

    @Test
    fun `proof fold drawer content order is the design's order in the composition`() {
        setProof(ProofDrawer.Fold)
        assertLogicalContentOrder(
            "Proof · fold guide",
            listOf(
                // The drawer's visible heading is the frozen `<h3>Fold it up</h3>` — the same words as the
                // `.done` button that opens it. `HOW_TO_FOLD` is its aria-label and the opener's label.
                Copy.Proof.FOLD_IT_UP,
                Copy.Proof.CLOSE,
                // The precondition, as the drawer's subtitle on every step: the guide opens from the top
                // bar before anything has been printed, and step 1 says "the sheet" without saying which.
                // It is chrome rather than content because as content it MOVED the controls beneath it
                // when it vanished after step 1 — a measured 108px on device (ADR-101 §6.8).
                Copy.ProofFold.INTRO_BODY,
                // `.stepline` — the counter, then the legend, whose four marks mean the same thing at
                // every step (ADR-101 P4; V21-SPEC §5.2). `move` is the fourth: with three, red meant
                // "cut here" on one step and "this paper travels" on seven.
                Copy.ProofFold.stepOf(1, 8).uppercase(),
                Copy.ProofFold.LEGEND_CREASE,
                Copy.ProofFold.LEGEND_FOLD_NOW,
                Copy.ProofFold.LEGEND_CUT,
                Copy.ProofFold.LEGEND_MOVE,
                // The fifth mark names the hollow Yoshizawa–Randlett *action* arrow of steps 4 and 7,
                // which the key promised completeness about and did not contain.
                Copy.ProofFold.LEGEND_ACT,
                // The diagram is labelled by the step's CAPTION (the frozen `aria-labelledby="foldCap"`),
                // so it announces what the picture shows rather than a title the picture does not carry.
                Copy.ProofFold.STEP_CAPTIONS[0],
                // …then the caption itself and the line telling you what you should be holding, merged
                // into one live region so a step change is announced as one sentence.
                Copy.ProofFold.STEP_CAPTIONS[0] + Copy.ProofFold.STEP_HOLDING[0],
                // The step navigation: both arrows on one row, then the eight dots on their own beneath —
                // the dots need the full width to keep a real touch target on a 360dp phone (ADR-101 §6.7
                // item 7). On step 1 the prev arrow is present but disabled; the next arrow gives way to
                // the finish button only on the last step. The dots are one labelled group carrying eight
                // `Role.Tab` stops — P4 made them buttons, which the frozen markup always had them be.
                Copy.ProofFold.PREVIOUS_STEP,
                Copy.ProofFold.NEXT_STEP,
                Copy.ProofFold.FOLD_STEPS_GROUP,
            ) + (1..8).map { Copy.ProofFold.stepDot(it) },
        )
    }
}
