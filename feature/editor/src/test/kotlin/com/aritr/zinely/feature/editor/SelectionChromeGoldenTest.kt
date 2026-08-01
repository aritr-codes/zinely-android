package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.SnapAxis
import com.aritr.zinely.core.editor.SnapGuide
import com.aritr.zinely.core.editor.ViewState
import com.aritr.zinely.core.editor.toUiState
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.core.render.SceneRenderer
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import kotlin.math.ceil
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * S4 selection-chrome **goldens** (ADR-029 §5, ADR-028 §7.5 golden discipline). The chrome layers
 * ([SelectionChrome] outline + [SnapGuides] alignment lines) draw in Compose *screen space* with
 * constant stroke widths — they are NOT part of the pure `:core:render` DrawCommand tape, so the
 * [PagePreviewParityTest] (host == raw replayer) cannot cover them. Their appearance — stroke width,
 * theme colour, rotated-corner placement — is pinned here instead, the same two-proof shape as
 * `:render-android`'s `RasterGoldenTest`:
 *
 *  1. **Behavioural pixel assertions** — deterministic, mode-independent; the chrome's theme colour must
 *     actually appear on the rendered box edge / guide line (runs green under a plain `testDebugUnitTest`,
 *     so the headless CI job is meaningful before any golden lands).
 *  2. A committed **Roborazzi golden** — [captureRoboImage] writes/reads the PNG at an explicit
 *     module-relative path, a no-op under a plain unit run; `recordRoborazziDebug` (pinned CI image,
 *     `record-goldens.yml`) produces it and `verifyRoborazziDebug` then gates pixel drift.
 *
 * Capture path: neither `captureToImage()` nor `captureRoboImage()` works headless under Robolectric
 * NATIVE (see [ComposeCanvasProbeTest]); we draw the laid-out decor view ([rasterizeToBitmap]) and crop
 * to the page's ACTUAL placed bounds (via the tagged [PagePreviewTestTag] node) — identical to
 * [PagePreviewParityTest]'s `hostPageBitmap`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SelectionChromeGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val SCREEN_PX_PER_PT = 2.5f

        // AA stroke edges (rotated outline, guide lines) jitter a fraction of pixels run-to-run; the same
        // committed tolerance RasterGoldenTest.aa() uses absorbs that without masking a placement bug.
        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private val sheet = PtSize(72.0, 72.0)
    private val offset = PtPoint(0.0, 0.0)

    private fun devicePx(): Pair<Int, Int> =
        ceil(sheet.width * SCREEN_PX_PER_PT).toInt().coerceAtLeast(1) to
            ceil(sheet.height * SCREEN_PX_PER_PT).toInt().coerceAtLeast(1)

    /** A one-element page with that element selected, at [rotationDegrees]. */
    private fun selectedModel(rotationDegrees: Double): EditorModel {
        val el = TextElement(
            id = "t1",
            transform = Transform(20.0, 20.0, 32.0, 32.0, rotationDegrees),
            text = "Zine",
        )
        return EditorModel(
            document = ZineDocument(
                format = ZineFormat.SINGLE_SHEET_8,
                paperSize = PaperSize.LETTER,
                pages = listOf(Page(index = 0, role = PageRole.INTERIOR, elements = listOf(el))),
            ),
            selection = setOf("t1"),
            view = ViewState(screenPxPerPt = SCREEN_PX_PER_PT, pageOffset = offset),
        )
    }

    /**
     * Compose [content] sized to the page in px, draw the laid-out decor view, then crop to the page's
     * ACTUAL placed bounds. Asserts density == 1.0 (so dp == px) and that the placed pixel size equals the
     * expected device px — exactly [PagePreviewParityTest]'s `hostPageBitmap` contract.
     */
    private fun pageBitmap(content: @Composable (Modifier) -> Unit): Bitmap {
        val (w, h) = devicePx()
        assertEquals(
            "test display density must be 1.0 for dp==px sizing; got ${composeRule.density.density}",
            1.0f, composeRule.density.density, 0.0001f,
        )

        composeRule.setContent {
            ZinelyTheme {
                content(Modifier.size(w.dp, h.dp))
            }
        }
        composeRule.waitForIdle()

        val bounds = composeRule.onNodeWithTag(PagePreviewTestTag).fetchSemanticsNode().boundsInRoot
        val bx = bounds.left.roundToInt()
        val by = bounds.top.roundToInt()
        val bw = bounds.width.roundToInt()
        val bh = bounds.height.roundToInt()
        assertEquals("host placed width != expected device px", w, bw)
        assertEquals("host placed height != expected device px", h, bh)

        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        require(bx >= 0 && by >= 0 && bx + bw <= full.width && by + bh <= full.height) {
            "page bounds ($bx,$by,${bx + bw},${by + bh}) fall outside decor view ${full.width}x${full.height}"
        }
        return Bitmap.createBitmap(full, bx, by, bw, bh)
    }

    /** Count of pixels exactly equal to [argb] — the chrome stroke core is flat colour (pre-AA blend). */
    private fun Bitmap.countColour(argb: Int): Int {
        var n = 0
        for (y in 0 until height) for (x in 0 until width) if (getPixel(x, y) == argb) n++
        return n
    }

    /**
     * Count of pixels carrying the snap guide's **hue signature**.
     *
     * **Retuned for V2 (C1, ADR-089 row 1.10), and the retune is the interesting part.** The guide was
     * `--teal` (#2A9D8F); it is now the frozen Bench's `--matcha` (#7C8A3F) at `.6` — see [SnapGuides].
     * The old predicate required `G−R ≥ 16`, which teal clears easily. Matcha is an *olive*: green and red
     * sit almost level, and once a 1dp AA line splits across two device columns the painted pixels measure
     * `rgb(202,206,188)` against a `rgb(250,250,250)` page — **G−R of 4**. The threshold was therefore
     * unreachable by the new token, and the test failed for the one reason a colour assertion should:
     * the colour changed.
     *
     * So the discriminator moves to **G−B**, which the same measurement puts at **18** against the page's
     * own **0**, with `G > R` retained as the clause that still excludes the `--coral-strong` chrome
     * (`R ≫ G`) and every neutral grey in the capture (`G−R = 0`). Numbers from a probe over the real
     * capture rather than from the token arithmetic, because what survives an AA blend is the question.
     */
    private fun Bitmap.countMatchaGuide(): Int {
        var n = 0
        for (y in 0 until height) for (x in 0 until width) {
            val p = getPixel(x, y)
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            if (g > r && g - b >= 10) n++
        }
        return n
    }

    @Test
    fun axis_aligned_selection_outline() {
        var chromeArgb = 0
        val bmp = pageBitmap { m ->
            chromeArgb = ZinelyTheme.colors.coralStrong.toArgb()
            EditorPagePreview(
                uiState = selectedModel(rotationDegrees = 0.0).toUiState(),
                defaults = DocumentDefaults(),
                pageSizePt = sheet,
                live = null,
                modifier = m,
            )
        }
        // The coral-strong outline must actually be on the page (not a vacuous blank capture); a 2dp
        // stroke around a 32pt box at 2.5 px/pt leaves well over 50 flat-colour core pixels.
        assertTrue(
            "selection outline did not paint the coral-strong token on the page",
            bmp.countColour(chromeArgb) > 50,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/selection_chrome_axis_aligned.png", aa())
    }

    @Test
    fun rotated_selection_outline() {
        var chromeArgb = 0
        val bmp = pageBitmap { m ->
            chromeArgb = ZinelyTheme.colors.coralStrong.toArgb()
            EditorPagePreview(
                uiState = selectedModel(rotationDegrees = 30.0).toUiState(),
                defaults = DocumentDefaults(),
                pageSizePt = sheet,
                live = null,
                modifier = m,
            )
        }
        assertTrue(
            "rotated selection outline did not paint the coral-strong token on the page",
            bmp.countColour(chromeArgb) > 50,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/selection_chrome_rotated.png", aa())
    }

    @Test
    fun selection_outline_with_snap_guides() {
        // Guides are render-only (LiveSnap output, never in history); compose them explicitly with the
        // selected page so the golden pins the guide stroke + colour deterministically, independent of a
        // gesture's exact snap maths. A vertical + horizontal guide crossing the box centre (36,36).
        //
        // CRITICAL (Codex review): replicate EditorPagePreview's PRODUCTION layer order exactly —
        // PagePreview -> SnapGuides -> SelectionChrome (guides BENEATH the chrome). Overlaying an extra
        // SnapGuides on top of a full EditorPagePreview would pin a non-production order where guides cover
        // the selection outline. We rebuild the same stack manually only because EditorPagePreview computes
        // its guides internally (no inject seam) and we need a deterministic guide frame here.
        val model = selectedModel(rotationDegrees = 0.0)
        val uiState = model.toUiState()
        val page = uiState.document.pages[uiState.currentPageIndex]
        val tape = SceneRenderer.render(page, sheet, DocumentDefaults())
        val selectedTransforms = page.elements.filter { it.id in uiState.selection }.map { it.transform }

        val bmp = pageBitmap { m ->
            Box(m) {
                PagePreview(
                    tape = tape,
                    sheet = sheet,
                    screenPxPerPt = SCREEN_PX_PER_PT,
                    pageOffset = offset,
                    modifier = Modifier.fillMaxSize(),
                    imageBytes = EmptyAssetBytes,
                )
                SnapGuides(
                    pageSizePt = sheet,
                    guides = listOf(
                        SnapGuide(SnapAxis.VERTICAL, 36.0),
                        SnapGuide(SnapAxis.HORIZONTAL, 36.0),
                    ),
                    screenPxPerPt = SCREEN_PX_PER_PT,
                    pageOffset = offset,
                    modifier = Modifier.fillMaxSize(),
                )
                SelectionChrome(
                    transforms = selectedTransforms,
                    screenPxPerPt = SCREEN_PX_PER_PT,
                    pageOffset = offset,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // 1dp AA guides blend with the page, so prove them by the matcha hue signature, not exact colour.
        // A vertical + horizontal line at 2.5 px/pt, less the frozen 8dp each end, leaves ~640 matching
        // pixels; a blank/undrawn guide layer leaves none, and the page's own greys leave none either.
        assertTrue(
            "snap guides did not paint the frozen --matcha token on the page",
            bmp.countMatchaGuide() > 50,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/selection_chrome_snap_guides.png", aa())
    }
}
