package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **F-5 — the ink popover owes the page the same clearance the editing row pays.**
 *
 * Device Pass 2 found a maker choosing ink for type the panel was covering. The cause is not a new one:
 * [benchEditPanMagnitudeDp] (D-043 / OD-16) already lifts the page clear of a docked panel, and
 * `BenchInkPopover` — placed in the same bottom inset, over the same page — simply sat outside the
 * condition that fed it. The fix is the missing term, so these tests are about the *wiring*, not the rule;
 * the rule's own arithmetic is pinned by [BenchC3Test].
 *
 * ### Why this file can assert what C3 structurally could not
 *
 * C3 records that the clearance term has **no screen-level test anywhere**, because on a device its
 * occluder is the IME and Robolectric has no IME — so it is carried by unit arithmetic and a device
 * checklist item. That limitation does not apply here: this occluder is an ordinary composable that
 * measures and docks under Robolectric like any other. This is therefore the first place in the repo where
 * `slack + clearance` is observed end to end, from a real occluder's measured edge to a real page's
 * displacement.
 *
 * ⚠ **That claim is only earned by the first test, and only because of how it is written.** A review killed
 * its first form, which asserted `panned < rest`: the *slack* term alone satisfies that, so a docked edge
 * reported at the canvas bottom — the exact degenerate value the popover's KDoc warns about — passed it
 * while observing nothing. What earns the sentence is the pair of guards plus `lift > slack`, and the box
 * depth chosen so the 96dp ceiling does not bind. Both were verified by mutation: reporting the edge 1000px
 * low, and un-wiring the popover term, each turn this file red.
 */
@RunWith(RobolectricTestRunner::class)
// The popover is tall, and on a short window it lands partly outside — four C6 assertions failed on that
// alone before it adopted this qualifier. A page that cannot fit its own occluder measures nothing here.
@Config(qualifiers = "w411dp-h891dp")
class BenchInkClearanceTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(100.0, 100.0)
    private val host: Pair<Dp, Dp> = 360.dp to 720.dp

    /**
     * Page-space boxes at two depths, and the depths are chosen rather than convenient.
     *
     * [needsClearance] sits with its bottom at 35pt of a 100pt page — far enough down to fall below the
     * docked card (so the clearance term is non-zero) and no further, because a box at the page *bottom*
     * drives `slack + clearance` past the 96dp ceiling and the assertion would then be measuring the clamp
     * instead of the measurement. [topOfPage] is the opposite branch: nowhere near the card, clearance 0.

     *
     * ⚠ Both depths are load-bearing and neither is arbitrary. [topOfPage]'s bottom sat at 20pt until F-2
     * made the transform row wrap: one extra row of chrome means a shorter canvas, a shorter page, and a
     * card covering proportionally more of it, which pushed this box's clearance from 0 to 2px and turned
     * an exact assertion red. Chrome height one surface away is an input to this file.
     */
    private val needsClearance = Transform(20.0, 17.0, 60.0, 18.0)
    private val topOfPage = Transform(20.0, 1.0, 60.0, 6.0)

    private fun store(): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        return EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
                ),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
    }

    /** Places one text box — the reducer auto-selects a placement — and mounts the screen. */
    private fun placedText(box: Transform): EditorStore {
        val store = store()
        store.dispatch(Intent.PlaceText(box, "hi"))
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(store = store, pageSizePt = pageSizePt, modifier = Modifier.size(host.first, host.second))
            }
        }
        composeRule.waitForIdle()
        return store
    }

    /**
     * The only route the freeze gives to the popover: `.inkpop` has no rest state and `openInk` is its
     * single `add('show')`. Reached through the context bar's own subtree because the popover's `h4`
     * carries the same word — a bare text match would go ambiguous the moment the panel appears.
     */
    private fun openInk() {
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.INK}").performClick()
        composeRule.waitForIdle()
    }

    /**
     * ⚠ `positionInRoot`, never `boundsInRoot`. A semantics node's bounds are **clipped by its parents**, and
     * a page lifted to the ceiling leaves the canvas — so `boundsInRoot.top` saturates at the canvas's own
     * top edge and reports a 43px lift for a 96px one. Measured: the first cut of this file did exactly that
     * and the strengthened assertion below failed against a correct implementation.
     */
    private fun paperTop() =
        composeRule.onNodeWithTag(EditorPaperSurfaceTestTag).fetchSemanticsNode().positionInRoot.y

    @Test
    fun opening_the_ink_popover_lifts_the_page_by_MORE_than_the_slack() {
        // ⚠ The first cut of this test asserted only `panned < rest`, and a review killed it: on this host
        // the slack term alone satisfies that, so a docked edge reported at the canvas *bottom* — precisely
        // the "a collapsed card reports the canvas's bottom edge and means nothing" failure the popover's
        // own KDoc names — passed it. The assertion below is the one that cannot: the lift must exceed the
        // slack, and the only term that can make it do so is the element's clearance against the card.
        placedText(needsClearance)
        val rest = paperTop()
        val slack = rest - composeRule.onNodeWithTag(EditorCanvasTestTag).fetchSemanticsNode().boundsInRoot.top

        openInk()
        val lift = rest - paperTop()

        // A16's taller, scrollable tray legitimately reaches the 96dp safety ceiling for this low target.
        // The useful invariant is that the measured panel contributes clearance beyond the sheet slack.
        assertTrue("this host must HAVE slack or the assertion is not about clearance (slack $slack)", slack > 1f)
        assertTrue(
            "the lift must exceed the slack — the excess IS the clearance term (slack $slack, lift $lift)",
            lift > slack + 1f,
        )
    }

    @Test
    fun a_page_top_element_uses_measured_clearance_without_hitting_the_ceiling() {
        // A16's taller tray begins close enough to the sheet top that even this 7pt-high box needs some
        // clearance. It must still remain below the 96dp clamp used by a low-page target: otherwise the
        // implementation has fallen back to the old unconditional maximum pan.
        placedText(topOfPage)
        val rest = paperTop()
        val slack = rest - composeRule.onNodeWithTag(EditorCanvasTestTag).fetchSemanticsNode().boundsInRoot.top

        openInk()

        // Guard before the assertion: on a host silently shrunk to its window the slack is 0 and the whole
        // thing degrades to `assertEquals(0f, 0f)`, which is the green nothing this suite exists to catch.
        assertTrue("this host must HAVE slack or the assertion proves nothing (slack $slack)", slack > 1f)
        val lift = rest - paperTop()
        val ceiling = with(composeRule.density) { 96.dp.toPx() }
        assertTrue("the measured tray must add clearance beyond slack (slack $slack, lift $lift)", lift > slack + 1f)
        assertTrue("a top-page target must not hit the 96dp clamp (lift $lift)", lift < ceiling - 1f)
    }

    @Test
    fun closing_the_popover_returns_the_page_to_rest() {
        // The pan is a state, not a one-way move: a page left leaning after the panel goes away is the same
        // class of defect in the opposite direction, and it is what an `onDockedTopChanged` that keeps
        // reporting a stale edge would produce.
        placedText(needsClearance)
        val rest = paperTop()

        openInk()
        composeRule.onNodeWithTag(BenchInkDoneTestTag).performClick()
        composeRule.waitForIdle()

        assertEquals("the page must settle back exactly where it started", rest, paperTop(), 0.25f)
    }
}
