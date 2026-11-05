package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.data.repository.PaperColour
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.feature.library.ZineActionScrim
import com.aritr.zinely.feature.library.ZineActionSheetSurface
import com.aritr.zinely.feature.library.ZineActionTarget
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * W6's goldens (AW6, AW6-P, AW6-B, A35): the chooser, the zine sheet's row, the Start sheet's four sizes, the
 * Proof stage and Print details on Pastel yellow, Print for a US Legal zine, and the Bench on Pastel blue.
 *
 * Behavioural half (runs under a plain unit run): a coloured surface paints the paper's multiplied white, and the
 * same surface on White paints none of it, so a golden can never be recorded off a host that ignored the colour.
 * The White goldens elsewhere in the suite are the other half: AW6.7 says White draws no layer at all.
 *
 * Recorded locally; CI's golden-exact job re-records on the pinned image.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class PaperColourGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST = "paperColourGoldenHost"
        val PAGE = PtSize(200.0, 300.0)
        fun aa() = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f))
    }

    private fun page(index: Int) = Page(
        index = index,
        role = if (index == 0) PageRole.FRONT_COVER else PageRole.INTERIOR,
        elements = listOf(
            TextElement(
                id = "t$index",
                transform = Transform(16.0, 40.0, 168.0, 60.0),
                text = "Quiet zine",
                style = TextStyle(fontFamily = "sans-serif", sizePt = 28.0),
            ),
        ),
    )

    private val pages = (0 until 8).map(::page)

    private fun show(dark: Boolean = false, fontScale: Float = 1f, content: @Composable () -> Unit) {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                ZinelyTheme(darkTheme = dark) {
                    Box(Modifier.testTag(HOST).background(ZinelyTheme.v21Colors.desk)) { content() }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun shot(): Bitmap = cropToBounds(
        composeRule.activity.window.decorView.rasterizeToBitmap(),
        composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInRoot,
    )

    /**
     * Pixels within 6 of the paper's sRGB in every channel: the white page ground multiplied by the paper. Not exact:
     * the multiply rounds by one, and the Proof's leaf carries its own faint shade over the page. The app's warm page
     * paper (#FFF6E8-ish) is far outside this band for every coloured paper, so a host that left the cream ground under
     * the multiply fails here (it did, once: see [PagePreview]'s white ground).
     */
    private fun Bitmap.count(paper: PaperColour): Int {
        val c = paper.srgb
        var n = 0
        for (y in 0 until height) for (x in 0 until width) {
            val p = getPixel(x, y)
            if (kotlin.math.abs((p shr 16 and 0xFF) - c.r) <= 6 &&
                kotlin.math.abs((p shr 8 and 0xFF) - c.g) <= 6 &&
                kotlin.math.abs((p and 0xFF) - c.b) <= 6
            ) n++
        }
        return n
    }

    private fun Bitmap.top(): String {
        val m = HashMap<Int, Int>()
        for (y in 0 until height) for (x in 0 until width) m.merge(getPixel(x, y), 1, Int::plus)
        return m.entries.sortedByDescending { it.value }.take(8).joinToString { Integer.toHexString(it.key) + "=" + it.value }
    }

    // ── AW6.4 the chooser ────────────────────────────────────────────────────────────────────────────

    private fun chooser(dark: Boolean, fontScale: Float = 1f, width: Int = 430) {
        show(dark, fontScale) {
            Box(Modifier.size(width.dp, 900.dp), contentAlignment = Alignment.BottomCenter) {
                PaperColourSheetSurface(selected = PaperColour.PASTEL_YELLOW, onPick = {})
            }
        }
    }

    @Test
    fun aw6_chooser_light() {
        chooser(dark = false)
        shot().captureRoboImage("$GOLDEN_DIR/aw6_chooser_light.png", aa())
    }

    @Test
    fun aw6_chooser_dark() {
        chooser(dark = true)
        shot().captureRoboImage("$GOLDEN_DIR/aw6_chooser_dark.png", aa())
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun aw6_chooser_font2_360() {
        chooser(dark = false, fontScale = 2f, width = 360)
        shot().captureRoboImage("$GOLDEN_DIR/aw6_chooser_font2_360.png", aa())
    }

    // ── AW6.3a the zine sheet's row ───────────────────────────────────────────────────────────────────

    @Test
    fun aw6_zine_sheet_pastel_yellow() {
        show {
            Box(Modifier.size(430.dp, 900.dp)) {
                ZineActionScrim(onDismiss = {})
                Box(Modifier.align(Alignment.BottomCenter)) {
                    ZineActionSheetSurface(
                        target = ZineActionTarget("Sunday market", "A4 · 2 days ago", paperColour = PaperColour.PASTEL_YELLOW),
                        onAction = {},
                    )
                }
            }
        }
        assertTrue("the row's swatch did not paint the paper", shot().count(PaperColour.PASTEL_YELLOW) > 300)
        shot().captureRoboImage("$GOLDEN_DIR/aw6_zine_sheet_pastel_yellow.png", aa())
    }

    // ── A35 the Start sheet ───────────────────────────────────────────────────────────────────────────

    @Test
    fun a35_start_sheet_four_sizes() {
        show {
            Box(Modifier.size(430.dp, 900.dp), contentAlignment = Alignment.BottomCenter) {
                ShelfCreateSheetSurface(onChoosePaper = {})
            }
        }
        shot().captureRoboImage("$GOLDEN_DIR/a35_start_sheet_light.png", aa())
    }

    // ── AW6-P the Proof stage and Print details ───────────────────────────────────────────────────────

    private fun stage(paper: PaperColour, dark: Boolean = false, fontScale: Float = 1f): Bitmap {
        show(dark, fontScale) {
            Box(Modifier.size(420.dp, 820.dp)) {
                ProofScreen(
                    onBack = {}, pages = pages, pageSizePt = PAGE, defaults = DocumentDefaults(), paperColour = paper,
                )
            }
        }
        composeRule.onNodeWithTag(proofReadPageTag(1)).assertExists()
        return shot()
    }

    @Test
    fun aw6p_stage_white_draws_no_layer() {
        assertEquals("a White stage painted a paper layer", 0, stage(PaperColour.WHITE).count(PaperColour.PASTEL_YELLOW))
    }

    @Test
    fun aw6p_stage_pastel_yellow() {
        val yellow = stage(PaperColour.PASTEL_YELLOW)
        assertTrue("the stage leaf is not on the paper " + yellow.top(), yellow.count(PaperColour.PASTEL_YELLOW) > 5_000)
        yellow.captureRoboImage("$GOLDEN_DIR/aw6p_stage_pastel_yellow_light.png", aa())
    }

    @Test
    fun aw6p_stage_pastel_yellow_dark() {
        stage(PaperColour.PASTEL_YELLOW, dark = true)
            .captureRoboImage("$GOLDEN_DIR/aw6p_stage_pastel_yellow_dark.png", aa())
    }

    @Test
    fun aw6p_stage_pastel_yellow_font2() {
        stage(PaperColour.PASTEL_YELLOW, fontScale = 2f)
            .captureRoboImage("$GOLDEN_DIR/aw6p_stage_pastel_yellow_font2.png", aa())
    }

    private fun details(paper: PaperSize, colour: PaperColour, dark: Boolean = false, fontScale: Float = 1f): Bitmap {
        show(dark, fontScale) {
            CompositionLocalProvider(LocalPaperColour provides colour) {
                Box(Modifier.size(420.dp, 900.dp).background(ZinelyTheme.v21Colors.surface)) {
                    ProofPrintDetailsPanel(
                        paper = paper, onPaperSelected = {}, onOpenFold = {}, paperColour = colour,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        return shot()
    }

    @Test
    fun aw6p_print_details_pastel_yellow() {
        details(PaperSize.A4, PaperColour.PASTEL_YELLOW)
            .captureRoboImage("$GOLDEN_DIR/aw6p_print_details_pastel_yellow_light.png", aa())
    }

    /**
     * AW6-P.5: "How it becomes a booklet", the whole sheet on the paper, margins included. Counted inside the
     * picture's own bounds only, so the row's swatch above (also the paper) cannot satisfy it.
     */
    private fun sheetPicture(colour: PaperColour): Bitmap {
        details(PaperSize.A4, colour)
        composeRule.onNodeWithTag(ProofSheetPreviewTestTag).performScrollTo()
        composeRule.waitForIdle()
        return cropToBounds(
            composeRule.activity.window.decorView.rasterizeToBitmap(),
            composeRule.onNodeWithTag(ProofSheetPreviewTestTag).fetchSemanticsNode().boundsInRoot,
        )
    }

    @Test
    fun aw6p_print_details_sheet_pastel_yellow() {
        val sheet = sheetPicture(PaperColour.PASTEL_YELLOW)
        assertTrue("the sheet picture is not on the paper " + sheet.top(), sheet.count(PaperColour.PASTEL_YELLOW) > sheet.width * sheet.height / 4)
        sheet.captureRoboImage("$GOLDEN_DIR/aw6p_print_details_sheet_pastel_yellow_light.png", aa())
    }

    @Test
    fun aw6p_print_details_sheet_white_draws_no_layer() {
        assertEquals("a White sheet picture painted a paper layer", 0, sheetPicture(PaperColour.WHITE).count(PaperColour.PASTEL_YELLOW))
    }

    @Test
    fun aw6p_print_details_pastel_yellow_dark() {
        details(PaperSize.A4, PaperColour.PASTEL_YELLOW, dark = true)
            .captureRoboImage("$GOLDEN_DIR/aw6p_print_details_pastel_yellow_dark.png", aa())
    }

    @Test
    fun aw6p_print_details_pastel_yellow_font2() {
        details(PaperSize.A4, PaperColour.PASTEL_YELLOW, fontScale = 2f)
            .captureRoboImage("$GOLDEN_DIR/aw6p_print_details_pastel_yellow_font2.png", aa())
    }

    @Test
    fun a35_print_details_us_legal() {
        details(PaperSize.LEGAL, PaperColour.WHITE)
            .captureRoboImage("$GOLDEN_DIR/a35_print_details_us_legal_light.png", aa())
    }

    // ── AW6-B the Bench ───────────────────────────────────────────────────────────────────────────────

    private fun store(): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        return EditorStore(
            EditorModel(ZineDocument(format = ZineFormat.SINGLE_SHEET_8, paperSize = PaperSize.A4, pages = pages)),
            CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, runner,
        )
    }

    @Test
    fun aw6b_bench_pastel_blue() {
        show {
            CompositionLocalProvider(LocalPaperColour provides PaperColour.PASTEL_BLUE) {
                EditorScreen(store = store(), pageSizePt = PAGE, modifier = Modifier.size(360.dp, 720.dp))
            }
        }
        assertTrue("the Bench canvas is not on the paper " + shot().top(), shot().count(PaperColour.PASTEL_BLUE) > 5_000)
        shot().captureRoboImage("$GOLDEN_DIR/aw6b_bench_pastel_blue_light.png", aa())
    }
}
