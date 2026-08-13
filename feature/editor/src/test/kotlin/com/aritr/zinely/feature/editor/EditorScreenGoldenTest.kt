package com.aritr.zinely.feature.editor

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
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
 * **CI-25** golden net for the assembled [EditorScreen], light + dark (roadmap §C1). A full-surface raster
 * in the [HomeScreenGoldenTest] / Proof style: `captureRoboImage` on the tagged surface node is record-only
 * (a no-op under a plain `testDebugUnitTest`), so a green plain run proves the screen composes and rasterises
 * at this size/theme, and `:feature:editor:recordRoborazziDebug` (`record-goldens.yml`) produces the PNG.
 *
 * The empty first-page editor is captured — the most stable, canonical assembly (invitation + supply tray +
 * page strip, no gesture-dependent selection chrome). A structural non-vacuity assertion (the paper surface
 * exists) runs under a plain unit run, proving the assembly mounted before any golden is recorded.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class EditorScreenGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val TAG = "editorScreenGoldenSurface"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(100.0, 100.0)

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

    private fun capture(name: String, darkTheme: Boolean) {
        // Build the store once, outside composition, so a recomposition (e.g. the measured-canvas
        // SetViewport) does not recreate it.
        val editorStore = store()
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize().testTag(TAG)) {
                    EditorScreen(
                        store = editorStore,
                        pageSizePt = pageSizePt,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        // Structural non-vacuity (runs under a plain unit run): the assembled screen actually mounted.
        composeRule.onNodeWithTag(EditorPaperSurfaceTestTag).assertExists()
        composeRule.onNodeWithTag(TAG).captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Test
    fun editor_screen_light() = capture("editor_screen_light", darkTheme = false)

    @Test
    fun editor_screen_dark() = capture("editor_screen_dark", darkTheme = true)

    /**
     * **The sheet must clear the context bar, because the bar floats over it.**
     *
     * P2's device Pass 2 (U3): `BenchContextBar` covered the bottom edge of the keep-clear boundary and the
     * page folio with it. The boundary exists to say *where the bottom limit is*, and that was the part
     * hidden — by chrome raised, at the time, on the same predicate that raised the cue, so the two arrived
     * together and contended for the same pixels. (Since OD-49 they have separate triggers — the bar keys on
     * selection, the cue on a *crossing* selection — but the occlusion this test guards is geometric, so the
     * assertions below are unaffected.)
     *
     * The fix reserves the bar's band before fitting the page ([BenchContextBarReservedHeightDp]), so this
     * asserts the outcome rather than the arithmetic: the sheet's bottom, as *placed*, sits above where the
     * bar's card begins. Asserting the reserve constant instead would just restate the implementation.
     *
     * The bar is **not** required to be visible for this: the band is reserved unconditionally, precisely so
     * that selecting something never resizes the page. So the geometry must hold at rest too, and this
     * checks it at rest — the state in which a regression would otherwise look harmless.
     *
     * ### Why the page is not this class's `pageSizePt`
     *
     * The first version of this test used it, passed, and **was vacuous** — the mutation check caught it.
     * `pageSizePt` is square (100×100pt) and this config's canvas is far taller than it is wide, so the fit
     * is decided by *width* and the sheet's bottom never comes near the canvas foot. Reserving nothing
     * passed just as happily as reserving the bar.
     *
     * The defect only exists when the fit is decided by **height**, which is the real case: a LETTER page is
     * taller than it is wide, and on the verification device it fitted to height with the sheet running to
     * the canvas floor and under the bar. So this uses a deliberately tall page, which is the only shape
     * that can fail. A test whose scenario cannot produce the defect is not evidence, however green.
     *
     * ### And why it measures the bar instead of computing where it should be
     *
     * The first version derived the bar's top from [BenchContextBarReservedHeightDp] — the very constant the
     * fix introduces. Both sides of the comparison then read the same number, so the test could not see the
     * constant being *wrong*, only being *inconsistently applied*. It was wrong: it used the frozen 40dp
     * drawn height where the row lays out at Material's 48dp floor, and a review caught it rather than this
     * test. So the bar is composed for real and its own `boundsInRoot` is the reference. An arithmetic error
     * in the reserve now fails here, which is what it is for.
     */
    @Test
    fun `the sheet is fitted above the context bar, not behind it`() {
        val editorStore = store()
        // Taller than the canvas's aspect, so the contain-fit is height-bound — see the KDoc.
        val tallPage = PtSize(100.0, 400.0)
        composeRule.setContent {
            ZinelyTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize().testTag(TAG)) {
                    EditorScreen(store = editorStore, pageSizePt = tallPage, modifier = Modifier.fillMaxSize())
                }
            }
        }
        // Place and select, so the bar exists to be measured. The reserve is unconditional, so the geometry
        // this asserts holds at rest too — but at rest there is no bar to take a reading from.
        editorStore.dispatch(Intent.PlaceText(Transform(10.0, 10.0, 20.0, 20.0), "hi"))
        composeRule.waitForIdle()

        val canvas = composeRule.onNodeWithTag(EditorCanvasTestTag).fetchSemanticsNode().boundsInRoot
        val sheet = composeRule.onNodeWithTag(EditorPaperSurfaceTestTag).fetchSemanticsNode().boundsInRoot
        val bar = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInRoot

        // 1. The outcome the user sees.
        assertTrue(
            "the sheet's bottom (${sheet.bottom}) runs under the context bar (top ${bar.top}) — the " +
                "keep-clear boundary's bottom edge and the page folio are behind it. sheet=$sheet bar=$bar",
            sheet.bottom <= bar.top + 0.5f,
        )

        // 2. **The reserve is the bar's real footprint** — the assertion the first two versions of this test
        // lacked, and the one that catches the defect a review found rather than the test. Assertion 1 alone
        // passes with slack: the tagged node's bounds carry the bar's outer inset, so an under-reserve of a
        // dp or ten still clears it. This compares the constant against a *measurement* of the bar, so
        // reserving the frozen 40dp drawn height where the row lays out at Material's 48dp floor fails here.
        val measuredFootprintPx = canvas.bottom - bar.top
        val reservedPx = with(composeRule.density) { BenchContextBarReservedHeightDp.toPx() }
        assertEquals(
            "the reserved band must equal the bar's measured footprint, or the reserve is a number that " +
                "merely happens to be big enough today. canvas.bottom=${canvas.bottom} bar.top=${bar.top}",
            measuredFootprintPx.toDouble(), reservedPx.toDouble(), 1.0,
        )
    }

    /**
     * **The keep-clear warning must reach the sheet at its own alpha — not through the focus wash.**
     *
     * ADR-102 §12.9's whole accessibility argument is that the `jam` warning clears WCAG 1.4.11's 3:1, and
     * [OD-48](../../../../../../../../docs/DECISIONS.md#adr-102-p2b) leaves it as the *only* mark this
     * boundary draws. That reading is void if something paints over it. Something did: [BenchKeepClear] was
     * nested inside the sheet box, which sits **below** [EditorPagePreview] — and that composable draws
     * [BenchFocusScrim], a single composite bounded to the page rect. The freeze dims `.el:not(.selected)`
     * (`v21-bench.html:207`), i.e. *elements*; `.keepclear` is a sibling of `.content` and is dimmed by
     * nothing. So the Compose composite washed a mark the frozen design never washes, and P2's device pass
     * measured the warning at **1.82:1** against §12.9's 3.66:1.
     *
     * The z-order still matters after OD-48, and if anything it matters more: the wash and the cue now share
     * a trigger. The wash keys on **selection** — `dimAlpha` is derived from `selectedTransforms.isNotEmpty()`
     * ([EditorPagePreview.kt:211-221](../../../../../main/kotlin/com/aritr/zinely/feature/editor/EditorPagePreview.kt))
     * — and since OD-49 a crossing is only reported for a **selected** element. So there is no state in which
     * this mark is drawn over undimmed paper: raising it necessarily lights the wash behind it.
     *
     * ### Why this test renders the whole screen, and drags
     *
     * The defect is pure paint order between two siblings of [EditorScreen], so it is invisible to every
     * cheaper check. Unit tests over [BenchStudio]'s colour maths read the *nominal* alpha and are still
     * correct. A hand-assembled scene — the [SelectionChromeGoldenTest] style — would assert only the order
     * the test itself wrote, which is no evidence at all. And no golden covers it, because the golden page
     * carries no elements.
     *
     * Since OD-48 a selection is no longer enough to raise the mark: it needs a **crossing**. The element is
     * therefore placed already outside the printer's reach — x=y=2pt against a 17pt inset — and `PlaceText`
     * selects it, which is what raises both the mark and the wash.
     *
     * **The drag is no longer what makes it speak.** Under OD-49 this raster could be taken at rest; it is
     * taken mid-gesture because that is the exact frame P2c's device pass measured at 3.51:1, and a golden
     * that guards a published contrast figure should guard the frame the figure came from. A review caught
     * the earlier version of this paragraph claiming the wash needed the gesture — it does not. Any drag
     * direction warns from that start, which keeps the test insensitive to the pan's pixel arithmetic.
     *
     * The assertion is on **implied alpha** rather than an exact colour, because the sheet's grain shifts
     * the paper by a few counts and the AA'd edges of a 1.5dp dash shift it more. Implied alpha is derived
     * against the paper measured *in the same raster*, so the grain divides out. The wash roughly halves
     * it: `.90` passes, `.445` fails, and the gap is far wider than any AA tolerance.
     */
    @Test
    fun `the keep-clear warning is not dimmed by the focus wash it is drawn over`() {
        val editorStore = store()
        composeRule.setContent {
            ZinelyTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize().testTag(TAG)) {
                    EditorScreen(store = editorStore, pageSizePt = pageSizePt, modifier = Modifier.fillMaxSize())
                }
            }
        }
        // Placed AND selected, already across the boundary: x=y=2pt against Imposer's 17pt inset. Under
        // OD-49 that is already enough — `PlaceText` auto-selects, and a selected crossing warns at rest.
        // The drag below holds the frame, it does not create the mark; see the KDoc.
        editorStore.dispatch(Intent.PlaceText(Transform(2.0, 2.0, 20.0, 20.0), "hi"))
        composeRule.waitForIdle()

        val sheet = composeRule.onNodeWithTag(EditorPaperSurfaceTestTag).fetchSemanticsNode().boundsInRoot
        // The element's centre: pt (2..22) of a 100pt page is the sheet's first fifth.
        val grab = Offset(sheet.left + sheet.width * 0.12f, sheet.top + sheet.height * 0.12f)
        composeRule.onNodeWithTag(TAG).performTouchInput {
            down(grab)
            // Past touch slop, and deliberately *inward* — the box still starts outside the inset, so this
            // proves the warning answers for where the content is rather than for which way it is heading.
            moveTo(Offset(grab.x + 40f, grab.y + 40f))
        }
        // No `up()`: the raster is held mid-gesture to match P2c's measured frame. It is **not** because
        // releasing would erase the mark — under OD-49 the warning deliberately outlives the gesture, and
        // the D-032 rule that survives is that it does not outlive the *selection*.
        composeRule.waitForIdle()

        val raster = composeRule.activity.window.decorView.rasterizeToBitmap()

        // The warning is the reddest thing on the sheet. `jam` is the only mark here with a wide R−B spread:
        // paper is warm but shallow (`#FFF6E8` spreads 24), the text is `ink` and near-neutral, and `berry`
        // is no longer drawn at all — OD-48 deleted the resting cue that used to be the other candidate.
        //
        // ⚠ `r > g > b` alone does **not** isolate jam, and a review found the case: `butter` satisfies it
        // too, and the snap guide at `GUIDE_ALPHA` scores ~175 against this warning's ~153 — so a guide
        // raised during the drag would win the maximum and drive the implied alpha to ~0.44, failing while
        // looking exactly like the paint-order regression this test exists to catch. No snap target is
        // within threshold at this geometry, so it cannot happen today; the third clause makes that a
        // property of the test rather than of the fixture. Jam is dark in the middle channel
        // (mixed ≈ 211/91/59, so g sits well below the r–b midpoint); butter is bright there (≈ 250/185/73,
        // g above it). The midpoint separates them by a wide margin in both directions.
        var cue: Int? = null
        var cueScore = 0
        val histogram = mutableMapOf<Int, Int>()
        for (y in sheet.top.toInt() + 2 until sheet.bottom.toInt() - 2) {
            for (x in sheet.left.toInt() + 2 until sheet.right.toInt() - 2) {
                val p = raster.getPixel(x, y)
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)
                val score = r - b
                if (r > g && g > b && g * 2 < r + b && score > cueScore) { cueScore = score; cue = p }
                histogram[p] = (histogram[p] ?: 0) + 1
            }
        }
        val cuePx = requireNotNull(cue) { "no jam-hued pixel on the sheet — the keep-clear warning did not draw at all" }
        // Paper is whatever most of the sheet is: the marks are strokes and the text is one small box, so
        // the mode is the washed paper itself — measured in this raster, so the grain divides back out.
        val paperPx = histogram.maxByOrNull { it.value }!!.key

        // implied alpha per channel, against jam #CF4A28 resolved through the island (light in both themes).
        val channel = listOf(Color::red, Color::green, Color::blue)
        val jam = listOf(0xCF, 0x4A, 0x28)
        val alphas = channel.mapIndexed { i, read ->
            (read(paperPx) - read(cuePx)).toFloat() / (read(paperPx) - jam[i]).toFloat()
        }
        val implied = alphas.average()
        // A **band**, not a floor. A one-sided `>` catches the wash and nothing else: it would pass just as
        // happily if the fade were deleted and the cue painted at 1.0, which is the opposite defect and
        // equally a departure from the frozen `.9`. A review pointed out the asymmetry and it costs a line.
        //
        // ⚠ The half-width is **.07**, and it was `.15` until a second review did the arithmetic OD-48 had
        // invalidated: at the old target of `.85`, `.15` put 1.0 exactly on the boundary and failing; at
        // `.90` it admits 1.0 with room to spare, so the sentence above had stopped being true of the
        // assertion below it. `.07` restores it. The tolerance can be this tight because the sampled pixel
        // is the *highest-scoring* one — a dash's core, not its AA edge — so the grain is what it has to
        // absorb, not antialiasing. The wash leaves .445, which is five times outside the band.
        assertTrue(
            "the keep-clear warning paints at an implied alpha of %.3f; it must reach its own %.2f — not the %.3f "
                .format(implied, BenchStudio.KEEP_CLEAR_WARN_ALPHA, BenchStudio.KEEP_CLEAR_WARN_ALPHA * (1f - BenchFocusDimAlpha)) +
                "the focus wash leaves of it (paint order regressed: the sheet furniture is below " +
                "EditorPagePreview's scrim again), and not more than it either (the reveal fade was lost). " +
                "per-channel=$alphas",
            kotlin.math.abs(implied - BenchStudio.KEEP_CLEAR_WARN_ALPHA) < 0.07f,
        )
    }
}
