package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * **The three Proof acts actually paint** — the mode-independent half of the goldens
 * [ADR-101](../../../../../../../docs/DECISIONS.md#adr-101) P1 retired, kept alive here.
 *
 * ### Why this file exists
 *
 * P1 deleted `ProofSheetGoldenTest`, `ProofPrintGoldenTest` and `ProofFoldGoldenTest` because each cropped
 * the **activity** decorView to `ProofScreenTestTag`'s bounds, and the content they framed moved into a
 * `ZSheet` — a `Dialog`, i.e. another window. `proof_sheet_*` duly failed with *"paper sheet did not
 * paint"*, which was true of that bitmap.
 *
 * But those suites carried **two** proofs each, and only one of them was about rasters. The
 * `captureRoboImage` half is a no-op under a plain unit run; the `countColour` assertions below are the
 * half that actually executes, and they are what caught a render regression rather than a pixel diff.
 * Deleting the suites deleted thirteen live assertions along with the stale baselines, and review is what
 * noticed. Restored here, mounted **directly against the act composables** — they are `internal` to this
 * module and need no host — so no window boundary is involved and no baseline can go stale.
 *
 * No `captureRoboImage`: rasters for the new surface are recorded once, in P6. This file asserts only that
 * each act draws its own material.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProofActPaintTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val HOST = "act-paint-host"
        val PHONE = 420.dp to 820.dp
        val TABLET = 820.dp to 1100.dp
    }

    private fun mount(darkTheme: Boolean, size: Pair<Dp, Dp>, content: @Composable () -> Unit) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                Box(Modifier.size(size.first, size.second).testTag(HOST)) { content() }
            }
        }
        composeRule.waitForIdle()
    }

    private fun crop(tag: String = HOST): Bitmap {
        val bounds = composeRule.onNodeWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val x = bounds.left.roundToInt().coerceAtLeast(0)
        val y = bounds.top.roundToInt().coerceAtLeast(0)
        val bw = bounds.width.roundToInt().coerceAtMost(full.width - x)
        val bh = bounds.height.roundToInt().coerceAtMost(full.height - y)
        return Bitmap.createBitmap(full, x, y, bw, bh)
    }

    private fun Bitmap.countColour(argb: Int): Int {
        var n = 0
        for (yy in 0 until height) for (xx in 0 until width) if (getPixel(xx, yy) == argb) n++
        return n
    }

    private fun Bitmap.assertPaints(what: String, colour: Color, atLeast: Int) {
        // The count is in the message on purpose: when one of these thresholds has to move, the failure
        // should hand over the number rather than send the next reader back to a debugger.
        val n = countColour(colour.toArgb())
        assertTrue("$what did not paint (counted $n px, wanted more than $atLeast)", n > atLeast)
    }

    // ---- The imposed sheet ----------------------------------------------------------------------

    private fun sheet(darkTheme: Boolean, size: Pair<Dp, Dp>): Bitmap {
        mount(darkTheme, size) { ProofImposedSheetBlock() }
        return crop()
    }

    /**
     * **One expectation for four tests, and that sameness is the assertion.**
     *
     * It very nearly became four. The V2.1 sweep let this sheet take the room's palette, so paper read
     * `#FFF6E8` on a light desk and `#332B22` on a dark one and `jam` lightened to `#E0755A` to survive it
     * — and this file dutifully pinned both pairs, which is how a test can certify a defect. Device
     * verification is what caught it: the dark sheet sat directly above the two lit cover cards, so one
     * drawer showed the same piece of paper two ways, and the darker of the two was the one about to go
     * into a printer.
     *
     * [ProofLitPaper] now holds the whole subtree in the light palette, so a single constant is once again
     * correct — and a future change that re-themes the sheet fails **two** of these tests rather than
     * passing all four with new numbers.
     */
    private fun Bitmap.assertSheetPaints() {
        assertPaints("paper sheet", Color(0xFFFFF6E8), 1000)
        // The one `jam` cut + "ONE CUT" label must actually paint.
        assertPaints("the cut", Color(0xFFCF4A28), 50)
    }

    @Test fun `the imposed sheet paints its paper and its one cut - light phone`() =
        sheet(darkTheme = false, PHONE).assertSheetPaints()

    @Test fun `the imposed sheet paints its paper and its one cut - dark phone`() =
        sheet(darkTheme = true, PHONE).assertSheetPaints()

    @Test fun `the imposed sheet paints its paper and its one cut - light tablet`() =
        sheet(darkTheme = false, TABLET).assertSheetPaints()

    @Test fun `the imposed sheet paints its paper and its one cut - dark tablet`() =
        sheet(darkTheme = true, TABLET).assertSheetPaints()

    // ---- The print recipe -----------------------------------------------------------------------

    private fun print(darkTheme: Boolean, size: Pair<Dp, Dp>): Bitmap {
        mount(darkTheme, size) {
            ProofPrintDetailsPanel(paper = PaperSize.A4, onPaperSelected = {}, onOpenFold = {})
        }
        return crop()
    }

    /**
     * The recipe's cards were V1 `field` — a grey input well. V2.1 has no such token and does not want
     * one: a card in this language is a piece of paper with a hairline on it, so the fill is `paper` and
     * the separation comes from the edge rather than from a second grey.
     */
    private fun Bitmap.assertRecipePaints(card: Color, emphasis: Color) {
        assertPaints("recipe cards", card, 1000)
        // The warn emphasis ("100% · Actual size", "Landscape") + "Change" speak `jam-text`.
        assertPaints("jam-text emphasis", emphasis, 40)
    }

    @Test fun `the print recipe paints its field cards and its warn emphasis - light phone`() =
        print(darkTheme = false, PHONE).assertRecipePaints(Color(0xFFFFF6E8), Color(0xFFA63B20))

    @Test fun `the print recipe paints its field cards and its warn emphasis - dark phone`() =
        print(darkTheme = true, PHONE).assertRecipePaints(Color(0xFF332B22), Color(0xFFE4856D))

    @Test fun `the print recipe paints its field cards and its warn emphasis - light tablet`() =
        print(darkTheme = false, TABLET).assertRecipePaints(Color(0xFFFFF6E8), Color(0xFFA63B20))

    @Test fun `the print recipe paints its field cards and its warn emphasis - dark tablet`() =
        print(darkTheme = true, TABLET).assertRecipePaints(Color(0xFF332B22), Color(0xFFE4856D))

    // ---- The fold guide --------------------------------------------------------------------------

    /**
     * [ProofFoldAct] is stateless — the step is a parameter — so the state under test is passed in rather
     * than clicked to. The retired suite clicked through four steps and a finish to reach the same
     * composition; that was navigation coverage borrowed by a paint test, and `ProofScreenTest` owns
     * navigation.
     */
    private fun fold(darkTheme: Boolean, size: Pair<Dp, Dp>, step: Int = 0): Bitmap {
        mount(darkTheme, size) {
            ProofFoldAct(
                step = step,
                reduceMotion = true,
                onNext = {},
                onPrev = {},
                onGoToStep = {},
            )
        }
        return crop()
    }

    // The fold drawer itself is themed, but the physical sheet it depicts stays lit in both themes for
    // the same reason the imposed sheet does: it is claiming to be the paper in the user's hands.
    @Test fun `the fold guide paints its step diagram - light phone`() =
        fold(darkTheme = false, PHONE).assertPaints("fold diagram sheet", Color(0xFFFFF6E8), 500)

    @Test fun `the fold guide paints its step diagram - dark phone`() =
        fold(darkTheme = true, PHONE).assertPaints("fold diagram sheet", Color(0xFFFFF6E8), 500)

    @Test fun `the fold guide paints its step diagram - light tablet`() =
        fold(darkTheme = false, TABLET).assertPaints("fold diagram sheet", Color(0xFFFFF6E8), 500)

    @Test fun `the fold guide paints its step diagram - dark tablet`() =
        fold(darkTheme = true, TABLET).assertPaints("fold diagram sheet", Color(0xFFFFF6E8), 500)

    @Test fun `dark legend keeps room labels while its marks match the lit sheet`() {
        mount(darkTheme = true, PHONE) {
            ProofFoldAct(
                step = 0, reduceMotion = true,
                onNext = {}, onPrev = {}, onGoToStep = {},
            )
        }
        crop(ProofFoldLegendTestTag).run {
            assertPaints("room-coloured legend labels", Color(0xFFBFAC93), 20)
            assertPaints("lit crease swatch", Color(0xFFA08B74), 5)
            assertPaints("lit fold-now swatch", Color(0xFF4E7A3C), 5)
            assertPaints("lit cut swatch", Color(0xFFCF4A28), 5)
            assertPaints("lit move/action swatches", Color(0xFF33261C), 5)
        }
    }

    /**
     * **A drawn line follows ink; only a shadow follows ink-line.**
     *
     * This test began as the guard against P4's first defect — the sheet outline stroked in `onDesk`, the
     * *room's* token on a *paper* surface, shipping a 1.03:1 dark-theme outline while every count above
     * stayed green, because a stroke colour does not move a paper-pixel count. The V2.1 re-skin retires
     * `onDesk` from this file entirely (V2.1 has one `ink`, and it flips with theme), but it introduces the
     * exact same defect one token over: `inkLine` is byte-identical to `ink` in **light** (`#33261C`) and
     * `#120E0A` in **dark**, so stroking the sheet in the hard-shadow token is invisible in light and puts
     * the outline at 1.38:1 against its own fill in dark. The prototype carries the same ✱ note over
     * `.sheetline`.
     *
     * **Cropped to the diagram, asserted in both directions, and taken on the one step where `ink` can
     * only be the outline.** The absence half is sound on any step — the Canvas draws no `inkLine` at all,
     * since `drawBehind` paints the offset rect *under* a layer that `.background(butterTint)` then
     * repaints, and the Canvas itself sits inside the stage's 6dp padding — so `assertEquals(0, …)` fails
     * the moment the sheet is restroked in the hard-shadow token.
     *
     * The presence half needed the step chosen for it. On step 0 `move()`'s stem and head are `ink` too,
     * so "some ink is present" survives the outline being deleted outright or dropped to `inkFaint`
     * (3.40:1) — a review found that hole, and it is the same hole the first version of this test had.
     * **Step index 4 carries no arrow at all** (cutting is the one instruction whose mark is the blade),
     * so every `ink` pixel in that crop *is* the sheet outline and the count pins it.
     */
    @Test fun `the fold sheet outline is drawn in ink, not in the hard-shadow token - dark phone`() {
        mount(darkTheme = true, PHONE) {
            ProofFoldAct(
                step = 4, reduceMotion = true,
                onNext = {}, onPrev = {}, onGoToStep = {},
            )
        }
        val diagram = crop(ProofFoldDiagramTestTag)
        diagram.assertPaints("fold sheet outline", Color(0xFF33261C), 300)
        assertEquals(
            "the diagram painted the hard-shadow token; a drawn line follows ink",
            0,
            diagram.countColour(Color(0xFF120E0A).toArgb()),
        )
    }

    /**
     * The last step still paints its own diagram — the check the retired *"the finished book paints"*
     * pair used to make about a climax that no longer exists. Step 8 draws the finished cover in `leaf`
     * on the sheet, so it is the one step whose material is the action colour rather than paper.
     */
    @Test fun `the last step paints its finished-cover diagram - light phone`() =
        fold(darkTheme = false, PHONE, step = FOLD_LAST_STEP)
            .assertPaints("step 8 cover outline", Color(0xFF4E7A3C), 200)

    @Test fun `the last step paints its finished-cover diagram - dark phone`() =
        fold(darkTheme = true, PHONE, step = FOLD_LAST_STEP)
            .assertPaints("step 8 cover outline", Color(0xFF4E7A3C), 200)
}
