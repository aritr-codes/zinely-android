package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * D-140 / A44: nothing the Proof sets is smaller than 12sp (controller ruling R-W4).
 *
 * Every `Text` node is read through [SemanticsActions.GetTextLayoutResult], so the size checked is the one
 * laid out, not the one a source file declares. The zine's own pages are exempt (D-140 #3) and are drawn on
 * a canvas, so they never appear here. The fold diagram's numbers are canvas text too, which no semantics
 * test can see: [foldCellNumberSp] is checked directly below.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class ProofTypeFloorTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val pages = (0 until 8).map { i ->
        Page(index = i, role = if (i == 0) PageRole.FRONT_COVER else PageRole.INTERIOR)
    }

    private fun mount(content: @Composable () -> Unit) {
        composeRule.setContent { ZinelyTheme { content() } }
        composeRule.waitForIdle()
    }

    /** Every laid-out run of text under 12sp, as "text @ size" — empty when the floor holds. */
    private fun underTheFloor(): List<String> {
        val nodes = composeRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertTrue("no text was laid out", nodes.isNotEmpty())
        return nodes.flatMap { node ->
            val results = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            results.flatMap { r ->
                val text = r.layoutInput.text
                val sizes = listOf(r.layoutInput.style.fontSize) + text.spanStyles.map { it.item.fontSize }
                sizes.filter { it.isTooSmall() }.map { "“${text.text}” @ $it" }
            }
        }
    }

    // Unspecified resolves to the 14sp default; anything not in sp is a size this floor cannot reason about.
    private fun TextUnit.isTooSmall(): Boolean = this != TextUnit.Unspecified && (!isSp || value < FLOOR_SP)

    private fun assertFloor(state: String) {
        val small = underTheFloor()
        assertEquals("$state has text under ${FLOOR_SP}sp: $small", emptyList<String>(), small)
    }

    // ---- Given each Proof state, When its text is laid out, Then nothing is under 12sp ----------

    @Test
    fun the_read_state_and_band_keep_the_floor() {
        mount { ProofScreen(onBack = {}, pages = pages) }
        assertFloor("Read + band")
    }

    @Test
    fun the_done_block_keeps_the_floor() {
        val saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
        mount { ProofScreen(onBack = {}, pages = pages, savedSignals = saved) }
        saved.tryEmit("zine.pdf")
        composeRule.waitForIdle()
        assertFloor("Done")
    }

    @Test
    fun the_error_overlay_keeps_the_floor() {
        mount { ProofScreen(onBack = {}, failedTarget = ProofExportTarget.SAVE, failure = ProofFailure.OutOfSpace) }
        assertFloor("Error")
    }

    /** The Print drawer's body, which carries the imposed sheet, its key and its cut tag. */
    @Test
    fun the_print_drawer_and_sheet_keep_the_floor() {
        mount { ProofPrintDetailsPanel(paper = PaperSize.A4, onPaperSelected = {}, onOpenFold = {}) }
        assertFloor("Print details + sheet")
    }

    @Test
    fun every_fold_step_keeps_the_floor() {
        var step by mutableIntStateOf(0)
        mount { ProofFoldAct(step = step, reduceMotion = true, onNext = {}, onPrev = {}, onGoToStep = {}) }
        (0..FOLD_LAST_STEP).forEach { s ->
            step = s
            composeRule.waitForIdle()
            assertFloor("Fold step ${s + 1}")
        }
    }

    // ---- The fold diagram's canvas numbers (R-W4) ----------------------------------------------

    @Test
    fun a_small_diagram_draws_its_numbers_at_the_floor() {
        // Given a 360dp phone's diagram: about 1.3dp per unit at xhdpi, so 9 units is ~5.9sp
        val sp = foldCellNumberSp(units = 9f, pxPerUnit = 2.6f, density = 2f, fontScale = 1f)
        assertTrue("drew at ${sp}sp", sp >= FLOOR_SP)
    }

    @Test
    fun at_font_scale_2_the_numbers_are_at_least_12sp_times_the_scale_on_screen() {
        val density = 2.75f
        val fontScale = 2f
        val sp = foldCellNumberSp(units = 9f, pxPerUnit = 3.6f, density = density, fontScale = fontScale)
        // sp to px is × density × fontScale, as TextMeasurer resolves it
        val px = sp * density * fontScale
        assertTrue("drew at ${px}px", px >= FLOOR_SP * fontScale * density)
    }

    @Test
    fun a_diagram_drawn_larger_than_the_floor_keeps_its_own_size() {
        // 9 units at 8px a unit is 72px = 36sp at density 2: the diagram's size, untouched
        assertEquals(36f, foldCellNumberSp(units = 9f, pxPerUnit = 8f, density = 2f, fontScale = 1f), 0.001f)
    }

    private companion object {
        const val FLOOR_SP = 12f
    }
}
