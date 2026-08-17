package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
 * ### ⚠ What this test can and cannot reach, stated rather than implied
 *
 * The defect's **trigger** is unreachable: decor's `Ink` verb ships disabled (`benchContextVerbs`), so no
 * click and no accessibility action can dispatch it, and enabling it here to test it would test a build
 * nobody ships. That unreachability is exactly why §10.1 rules the fix is *the routing, not the verb* — and
 * it is why the repair is structural rather than a guard alone: `EditorScreen` now derives
 * `inkPopoverVisible = inkPopoverOpen && inkTarget != null` and **every** consumer reads the derived value,
 * so a popover that cannot be shown is not honoured by the bar, by `Done`, by the bar's caption or by the
 * edit pan. The stranded state is not defended against; it cannot be constructed.
 *
 * What this file pins is the *consequence*: with a supply selected, the frozen bar is up, its `Ink` is
 * drawn-and-inert, `Done` is live, and no popover exists. Those four together are the negation of the state
 * the defect produced. The live half of the same derivation — text, `Ink`, popover appears, bar stands down
 * — is exercised end to end by [BenchC6Test], which is what would fail if the refactor broke the working
 * case.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp")
class BenchDecorInkRoutingTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val decorId = "decor-1"

    private fun storeWithSelectedSupply(): EditorStore {
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
        store.dispatch(Intent.Select(decorId))
        return store
    }

    private fun render() {
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(
                    store = storeWithSelectedSupply(),
                    pageSizePt = PtSize(100.0, 130.0),
                    modifier = Modifier.size(360.dp, 720.dp),
                )
            }
        }
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
    fun the_supplys_ink_verb_is_drawn_and_inert_and_done_stays_live_beside_it() {
        render()
        // Drawn: OD-9 keeps a control the freeze draws. Inert: the popover has no decor branch yet.
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.INK}").assertIsNotEnabled()
        // The other half of the stranded state: `Done` disabled with nothing to finish. It reads the
        // derived visibility now, so it cannot go dim for a popover that is not on screen.
        composeRule.onNodeWithContentDescription(Copy.EditText.DONE).assertIsEnabled()
    }
}
