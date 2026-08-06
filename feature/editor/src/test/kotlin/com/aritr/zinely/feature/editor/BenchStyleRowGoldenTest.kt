package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.foundation.layout.size
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.pixelCountOf
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The golden net for [BenchStyleRow] — the frozen `.styletb` (ADR-093 rows 3.5–3.7, 3.9), light + dark, in
 * the [EditTextSessionGoldenTest] two-proof shape.
 *
 * **This is where the row's painted properties are actually asserted.** The behavioural suite
 * ([BenchC3Test]) can read order, enabled-ness and geometry from the semantics tree, but a `--sheet`
 * ground, a 1px `--chrome-line` hairline, `--matcha` under `--on-matcha` on `Done`, and the ink swatch
 * taking the element's own colour are all *paint* — the only honest reading of them is the raster.
 *
 * The swatch is seeded with the coral content ink rather than the default, so the golden distinguishes
 * "reports the element's colour" from "draws a theme default" (row 3.9); a swatch drawn from `--ink` would
 * be visibly different here.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class BenchStyleRowGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "benchStyleRowGoldenHost"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    /** The Coral content ink (`TypeBar`'s `TextInk.Coral`), so the swatch cannot pass by drawing `--ink`. */
    private val coralInk = Color(0xFFA63C22)

    private var deskArgb = 0
    private var matchaArgb = 0

    private fun capture(name: String, darkTheme: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                deskArgb = ZinelyTheme.colors.desk.toArgb()
                matchaArgb = ZinelyTheme.v2Colors.matcha.toArgb()
                Box(
                    Modifier
                        .testTag(HOST_TAG)
                        .fillMaxWidth()
                        .background(ZinelyTheme.colors.desk)
                        .padding(16.dp),
                ) { content() }
            }
        }
        composeRule.waitForIdle()
        // Non-vacuity: the row's own node must be present, so a later blanked re-record fails here rather
        // than passing on a desk-pixel count.
        composeRule.onNodeWithTag(BenchStyleRowTestTag).assertExists()
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val bounds = composeRule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot
        val bmp: Bitmap = cropToBounds(full, bounds)
        assertTrue("the host desk did not paint ($name)", bmp.pixelCountOf(deskArgb) > 100)

        // The golden RASTER is not, on its own, an assertion about fine paint. `changeThreshold = 0.02f`
        // means a change has to move 2 % of the compared pixels before the gate reacts — and a 1px hairline
        // across a 400px row is roughly 0.25 %. The mutation that deletes the hairline outright passed the
        // recorded golden. So the three properties small enough to hide under that threshold are counted
        // here, in pixels, and the raster keeps its real job: catching what nobody thought to assert.
        val row = cropToBounds(full, composeRule.onNodeWithTag(BenchStyleRowTestTag)
            .fetchSemanticsNode().boundsInRoot)

        // 3.5 — `border-top:1px solid var(--chrome-line)`: the row's first scanline must differ from the
        // row's own interior ground. Two earlier shapes of this assertion both failed to bite, and both
        // failures are worth keeping:
        //
        //  * equality with `--chrome-line` — wrong, because the rule is composited over `--sheet` and the
        //    result is neither token;
        //  * inequality with the `--sheet` TOKEN, read from a re-cropped bitmap — wrong, because the crop's
        //    rounded top edge can land a pixel above the row, on the desk, which differs from `--sheet`
        //    whether or not a hairline was ever drawn. It passed with the hairline deleted.
        //
        // So the comparison is against the ground **as actually painted**, sampled from the row's middle,
        // and both are read in root coordinates from the full raster — no second crop to round.
        val rowRect = composeRule.onNodeWithTag(BenchStyleRowTestTag).fetchSemanticsNode().boundsInRoot
        val left = rowRect.left.toInt() + 2
        val right = rowRect.right.toInt() - 2
        val ground = full.getPixel(left, (rowRect.top + rowRect.height / 2f).toInt())
        var hairline = 0
        for (x in left until right) if (full.getPixel(x, rowRect.top.toInt() + 1) != ground) hairline++
        assertTrue("the top hairline is missing ($name): only $hairline/${right - left} px on the row's " +
            "first scanline differ from its own ground", hairline > (right - left) / 2)

        // 3.7 — the `Done` chip's `--matcha` fill. A pill of h34 at this density is thousands of pixels;
        // 500 is far below it and far above any incidental match.
        assertTrue("Done did not paint its --matcha fill ($name)", row.pixelCountOf(matchaArgb) > 500)

        // 3.9 — the swatch reports the ELEMENT's ink, so the seeded coral must be on screen. A 14dp dot is
        // ~780px at xhdpi; 200 survives the circle's antialiased rim.
        assertTrue("the ink swatch is not the element's own colour ($name)",
            row.pixelCountOf(coralInk.toArgb()) > 200)

        bmp.captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Test
    fun bench_style_row_light() =
        capture("bench_style_row_light", darkTheme = false) {
            BenchStyleRow(visible = true, inkSwatch = coralInk, onDone = {})
        }

    @Test
    fun bench_style_row_dark() =
        capture("bench_style_row_dark", darkTheme = true) {
            BenchStyleRow(visible = true, inkSwatch = coralInk, onDone = {})
        }

    /**
     * The whole C3 assembly in one frame: the page lifted by the frozen pan, the element's text drawn
     * **once** by the in-place field rather than twice by field-over-tape (row 3.11), and the style row
     * docked. Screen-level rather than component-level on purpose — every one of those is a property of how
     * the pieces are wired together, and none of them can be seen in a component in isolation.
     */
    @Test
    fun bench_editing_state_light() {
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
        // Low on the page on purpose. When the pan was an unconditional 96dp an element near the page top
        // left the canvas entirely (D-043) and the golden pictured an *empty* editing state while still
        // passing — which is exactly what the first cut of this file did. Since OD-16 the pan is clamped
        // and would no longer strand it, but the position stays: a golden that only frames its subject
        // because a clamp is working is a golden that stops framing it the day the clamp regresses.
        store.dispatch(Intent.PlaceText(Transform(20.0, 76.0, 60.0, 18.0), "Zine"))
        val id = store.uiState.value.selection.single()
        // Coral, not the default ink — because row 3.9's *wiring* is only visible at the screen. The two
        // component goldens pass `inkSwatch` in by hand, so replacing `EditorScreen`'s
        // `editingElement?.style?.color` with a theme default leaves them completely unmoved: that mutation
        // survived the whole golden gate. Here the screen has to read the element to get this colour.
        store.dispatch(Intent.StyleText(id = id, color = ColorRgba(0xA6, 0x3C, 0x22)))

        composeRule.setContent {
            ZinelyTheme(darkTheme = false) {
                deskArgb = ZinelyTheme.colors.desk.toArgb()
                matchaArgb = ZinelyTheme.v2Colors.matcha.toArgb()
                Box(Modifier.testTag(HOST_TAG).background(ZinelyTheme.colors.desk)) {
                    EditorScreen(store = store, pageSizePt = PtSize(100.0, 100.0), modifier = Modifier.size(360.dp, 720.dp))
                }
            }
        }
        composeRule.waitForIdle()
        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(BenchStyleRowTestTag).assertExists()
        composeRule.onNodeWithTag(EditTextSessionTestTag).assertExists()
        val bounds = composeRule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot
        val field = composeRule.onNodeWithTag(EditTextSessionTestTag).fetchSemanticsNode().boundsInRoot
        val row = composeRule.onNodeWithTag(BenchStyleRowTestTag).fetchSemanticsNode().boundsInRoot

        // The KDoc above makes three claims; a raster that does not contain the edited element proves none
        // of them. `assertExists` is not enough — a composed node panned off the top of the host still
        // exists. So the frame is asserted to actually CONTAIN the field, above the docked row.
        assertTrue("the edited field ($field) is outside the captured frame ($bounds)",
            field.top >= bounds.top && field.bottom <= bounds.bottom)
        assertTrue("the edited field ($field) is not above the style row ($row)", field.bottom <= row.top)

        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        // …and to contain SOMETHING: the field's own rect must not be a flat wash of paper. This is a
        // non-vacuity check, and no more than that — it is satisfied by any non-paper pixel, the caret
        // included, so it must not be read as proof of "the words are drawn, once, in place". That
        // property has two real guards of its own:
        // `EditorPagePreviewTest.the_tape_omits_the_element_under_an_open_session` and
        // `BenchC3Test.the_screen_hands_the_edited_elements_id_to_the_tape`, both of which the mutation
        // battery confirms are non-vacuous. An earlier KDoc here claimed the stronger thing.
        // Row 3.9 at the screen: the swatch must be the ELEMENT's coral, which only a screen that reads
        // `editingElement.style.color` can produce. A 14dp dot at xhdpi is ~780px; 200 clears its AA rim.
        val rowCrop = cropToBounds(full, row)
        assertTrue("the swatch is not seeded from the edited element's own colour",
            rowCrop.pixelCountOf(coralInk.toArgb()) > 200)

        // Row 3.8 — the caret's PAINT: `--matcha`, 1.5px wide. Both were named in the frozen property
        // table's assertion column and neither was asserted; the re-review's own mutations reverted the
        // colour to `coralStrong` and multiplied the width by eight, and both survived the whole suite.
        //
        // Read here rather than in a component test because the caret is drawn into the field's own
        // `drawWithContent`, so the only honest reading is the composed frame. `Done` is also `--matcha`,
        // which is exactly why this is scoped to the FIELD's rect and not the screen's.
        val fieldCrop = cropToBounds(full, field)
        // Near-match, not exact: the caret is a 1.5px rule at a fractional x, so `drawRect` antialiases
        // both its edges and — at this width — may leave no fully-covered pixel at all. An exact-equality
        // probe found nothing and reported the caret missing when it was plainly drawn. A tolerance of 24
        // per channel is far tighter than the distance from `--matcha` to any other colour in this rect
        // (paper, ink, and the coral the mutation substitutes are all much further away).
        fun near(p: Int, q: Int): Boolean {
            fun c(v: Int, sh: Int) = (v shr sh) and 0xFF
            return listOf(16, 8, 0).all { sh -> kotlin.math.abs(c(p, sh) - c(q, sh)) <= 24 }
        }
        val matchaCols = (0 until fieldCrop.width).filter { x ->
            (0 until fieldCrop.height).any { y -> near(fieldCrop.getPixel(x, y), matchaArgb) }
        }
        assertTrue("the caret is not drawn in --matcha inside the field (row 3.8)", matchaCols.isNotEmpty())
        // A vertical rule occupies one contiguous run of columns; its width is that run. 1.5dp at xhdpi is
        // 3px, and the tolerance is ±1px for the rule's own antialiased edge — not wide enough to admit the
        // 24px an 8× mutation produces.
        val caretWidthPx = matchaCols.last() - matchaCols.first() + 1
        val expectedCaretPx = with(composeRule.density) { 1.5.dp.toPx() }
        assertTrue("the caret is ${caretWidthPx}px wide; the frozen 1.5px is ${expectedCaretPx}px",
            kotlin.math.abs(caretWidthPx - expectedCaretPx) <= 1f)

        val paperArgb = fieldCrop.getPixel(0, 0)
        assertTrue("the edited text did not paint inside the field",
            fieldCrop.pixelCountOf(paperArgb) < fieldCrop.width * fieldCrop.height)

        cropToBounds(full, bounds).captureRoboImage("$GOLDEN_DIR/bench_editing_state_light.png", aa())
    }
}
