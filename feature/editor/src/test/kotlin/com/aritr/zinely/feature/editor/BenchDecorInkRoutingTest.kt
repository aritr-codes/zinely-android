package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.semantics.SemanticsActions
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
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A selected supply leaves the Bench in a state a maker can act on — the invariant the `Ink` routing defect
 * broke (SUPPLIES-SPEC §10.1, S7).
 *
 * ### ⚠ This header is rewritten: the trigger is no longer unreachable
 *
 * It used to say the defect's trigger could not be reached because decor's `Ink` verb shipped disabled, and
 * that *"enabling it here to test it would test a build nobody ships"*. **The decor-ink package ships that
 * build.** The verb is live, so this file now exercises the trigger directly instead of pinning its
 * consequence, and the assertions below flipped accordingly.
 *
 * The structural repair the old header described is still the point, and it went one step further. The
 * popover's state is no longer `Boolean` + a `LaunchedEffect` enforcing "it belongs to the element that
 * summoned it"; it is the **summoning id**, compared against the live selection every composition. That
 * change was forced by a defect this file did not catch and now does — see
 * [the_change_ink_a11y_action_opens_the_popover_when_the_supply_was_NOT_already_selected].
 *
 * **The lesson worth keeping is about the fixture, not the flag.** Every test here rendered with the supply
 * already selected. That is the one state in which the effect's key never changes, so the race was
 * invisible — a green file, an untested primary path, and a header confidently explaining why the gap was
 * fine. The unselected fixture is the whole fix.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp")
class BenchDecorInkRoutingTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val decorId = "decor-1"

    private fun storeWithSelectedSupply(selected: Boolean = true): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        val store = EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(
                        Page(
                            index = 0,
                            role = PageRole.INTERIOR,
                            elements = listOf(
                                DecorElement(
                                    id = decorId,
                                    transform = Transform(20.0, 40.0, 30.0, 30.0),
                                    // An AUTHORED supply, so the element is one the renderer can draw —
                                    // the routing question must not be entangled with the outline question.
                                    supplyId = "shape.rect",
                                    ink = ColorRgba(0x2A, 0x25, 0x1E),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, runner,
        )
        // The unselected case is not a corner: TalkBack's accessibility focus is NOT the app's selection, so
        // a maker reaching a supply by swipe arrives here with an empty selection. It is the a11y action's
        // primary path, and it was the one that was broken.
        if (selected) store.dispatch(Intent.Select(decorId))
        return store
    }

    /** Invokes a custom accessibility action by label, the way the platform's action menu does. */
    private fun invokeA11yAction(label: String) {
        val actions = composeRule
            .onNodeWithContentDescription(Copy.Supplies.NAMES.getValue("shape.rect"), substring = true)
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        val action = actions.single { it.label == label }
        composeRule.runOnUiThread { action.action() }
        composeRule.waitForIdle()
    }

    private lateinit var store: EditorStore

    /** The single supply on the page, read back from the live store. */
    private fun theSupply(): DecorElement =
        store.uiState.value.document.pages[0].elements.filterIsInstance<DecorElement>().single()

    private fun render(selected: Boolean = true) {
        store = storeWithSelectedSupply(selected)
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(
                    store = store,
                    pageSizePt = PtSize(100.0, 130.0),
                    modifier = Modifier.size(360.dp, 720.dp),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun reveal(tag: String) {
        composeRule.onNodeWithTag(BenchArtSheetTestTag).performScrollToNode(hasTestTag(tag))
        composeRule.waitForIdle()
    }

    @Test
    fun a_selected_supply_keeps_the_frozen_verb_bar_and_never_opens_an_empty_popover() {
        render()
        // The bar is the thing the defect made disappear — `ctxVisible` carries `!inkPopoverVisible`.
        composeRule.onNodeWithTag(BenchContextBarTestTag).assertExists()
        // …and nothing took its place. `.inkpop` is text-only; for a supply it must simply not be there.
        composeRule.onNodeWithTag(BenchInkPopoverTestTag).assertDoesNotExist()
    }

    @Test
    fun the_supplys_ink_verb_is_live_and_opens_the_popover_onto_the_supply() {
        render()
        // ⚠ **Inverted by the decor-ink package.** This asserted `assertIsNotEnabled` while the verb was
        // drawn-and-inert under OD-9. The popover now has its decor branch, so the verb is live.
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.INK}").assertIsEnabled()
        composeRule.onNodeWithContentDescription(Copy.EditText.DONE).assertIsEnabled()

        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.INK}").performClick()
        composeRule.waitForIdle()

        // The exchange the original defect could never complete: the popover arrives **and** the bar stands
        // down for it (`ctxVisible` carries `!inkPopoverVisible`). Asserting both is the point — the stranded
        // state this file exists for was precisely a bar that left with no popover arriving in its place.
        composeRule.onNodeWithTag(BenchInkPopoverTestTag).assertExists()
        composeRule.onNodeWithTag(BenchContextBarTestTag).assertDoesNotExist()
    }

    @Test
    fun the_change_ink_a11y_action_opens_the_popover_when_the_supply_was_NOT_already_selected() {
        // ⚠ **The regression test for the defect independent review found by probe, and the reason it
        // shipped green.** `Change ink` selects the supply and then opens the popover. While the popover's
        // state was a `Boolean`, a `LaunchedEffect` keyed on the selected id enforced "the popover belongs
        // to the element that summoned it" — so the action's own `Select` relaunched that effect and cleared
        // the flag it had just set. Nothing opened.
        //
        // It was invisible because every existing test rendered with the supply ALREADY selected, which is
        // the one case where the effect's key does not change. TalkBack's focus is not the app's selection,
        // so the untested branch was the real one.
        render(selected = false)

        invokeA11yAction(Copy.A11y.CHANGE_INK)

        composeRule.onNodeWithTag(BenchInkPopoverTestTag).assertExists()
    }

    @Test
    fun the_change_ink_a11y_action_also_opens_it_when_the_supply_was_already_selected() {
        // The branch that always worked, kept so a fix that repairs one path by breaking the other is red.
        render(selected = true)

        invokeA11yAction(Copy.A11y.CHANGE_INK)

        composeRule.onNodeWithTag(BenchInkPopoverTestTag).assertExists()
    }

    @Test
    fun both_of_section_8s_decor_actions_are_advertised_and_neither_is_a_dead_action() {
        // §8's table of 13, asserted where TalkBack would read it. Both actions earned their place by
        // doing something; `EditorA11y` withholds either one whose callback is absent.
        render()
        val labels = composeRule
            .onNodeWithContentDescription(Copy.Supplies.NAMES.getValue("shape.rect"), substring = true)
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .map { it.label }

        assertTrue("Change ink must be advertised", Copy.A11y.CHANGE_INK in labels)
        assertTrue("Replace supply must be advertised", Copy.A11y.REPLACE_SUPPLY in labels)
    }

    @Test
    fun the_replace_supply_a11y_action_opens_the_art_cabinet_when_not_already_selected() {
        // The same unselected fixture that caught D-091 on the ink action, applied to the second verb
        // before it can repeat the defect. `Replace` selects the supply and then opens a sheet; if the
        // sheet's state were ever re-coupled to the selection, this is the test that would go red.
        render(selected = false)

        invokeA11yAction(Copy.A11y.REPLACE_SUPPLY)

        composeRule.onNodeWithTag(BenchArtSheetTestTag).assertExists()
    }

    @Test
    fun replacing_through_the_cabinet_swaps_the_outline_in_place_at_the_new_family_size() {
        // ⚠ The end-to-end assertion independent review found missing: the two Replace tests above only
        // proved the *cabinet opened*. Nothing exercised the tile tap, so the branch that actually performs
        // the swap — and the owner's scale ruling inside it — was untested through the UI.
        render()
        val before = theSupply()

        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.REPLACE}").performClick()
        composeRule.waitForIdle()
        reveal(benchArtTileTestTag("shape.circle"))
        composeRule.onNodeWithTag(benchArtTileTestTag("shape.circle")).performClick()
        composeRule.waitForIdle()

        val after = theSupply()
        assertEquals("the outline is the tapped one", "shape.circle", after.supplyId)
        // Same element, not a new one: a Replace implemented as delete-then-place would pass the line above
        // and fail this one, and would cost the maker their z-order and two undo steps.
        assertEquals("the element keeps its id", before.id, after.id)
        assertEquals("and its ink", before.ink, after.ink)
        // The ruling, asserted where the maker meets it: centre held, size from the incoming family.
        assertEquals(
            "the centre is preserved",
            before.transform.xPt + before.transform.widthPt / 2.0,
            after.transform.xPt + after.transform.widthPt / 2.0,
            1e-6,
        )
        assertEquals(
            "the size is the incoming family's",
            benchSupplyPlacement("shape.circle", PtSize(100.0, 130.0)).widthPt,
            after.transform.widthPt,
            1e-6,
        )
        // The sheet closes behind the swap; a cabinet left open would hide the change it just made.
        composeRule.onNodeWithTag(BenchArtSheetTestTag).assertDoesNotExist()
    }

    @Test
    fun the_visible_replace_verb_opens_the_art_cabinet_too() {
        render()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.REPLACE}").assertIsEnabled()

        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.REPLACE}").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(BenchArtSheetTestTag).assertExists()
    }

}
