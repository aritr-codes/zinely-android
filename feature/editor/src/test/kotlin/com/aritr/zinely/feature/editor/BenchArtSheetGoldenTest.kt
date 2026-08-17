package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.components.ZSheetSurface
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
 * The golden net for the Art sheet — the frozen `.lbl`/`.grid`/`.tile` block (`v21-bench.html:457-468`),
 * light and dark.
 *
 * **This is where the sheet's painted properties are actually asserted.** [BenchArtSheetTest] reads
 * structure from the semantics tree and cannot see a tint, a tilt, a printed shadow or the dim on twelve
 * tiles; those are paint, and the only honest reading of paint is the raster.
 *
 * Composed on [ZSheetSurface] rather than [BenchArtSheet], for the reason `ZineActionSheetTest` states: a
 * `Dialog`'s own window is invisible to the decor-view capture harness.
 *
 * ⚠ **A raster alone is not an assertion about fine paint.** `changeThreshold = 0.02f` means a change must
 * move 2 % of compared pixels before the gate reacts, and the tint of one tile in sixteen is under that. So
 * the three properties small enough to hide under the threshold are counted in pixels here, and the raster
 * keeps its real job: catching what nobody thought to assert.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h812dp-xhdpi")
class BenchArtSheetGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "benchArtSheetGoldenHost"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )

        val GoldenDeskMargin = 16.dp
    }

    private var deskArgb = 0
    private var leafTintArgb = 0
    private var berryTintArgb = 0
    private var butterTintArgb = 0

    private fun capture(name: String, darkTheme: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                deskArgb = ZinelyTheme.colors.desk.toArgb()
                leafTintArgb = ZinelyTheme.v21Colors.leafTint.toArgb()
                berryTintArgb = ZinelyTheme.v21Colors.berryTint.toArgb()
                butterTintArgb = ZinelyTheme.v21Colors.butterTint.toArgb()
                Box(
                    Modifier
                        .testTag(HOST_TAG)
                        .fillMaxWidth()
                        .background(ZinelyTheme.colors.desk)
                        // The desk margin is the harness's own non-vacuity probe, not a design value: the
                        // sheet fills its host edge to edge, so without it there is no room for the desk to
                        // paint and the "did the theme apply at all" check reads zero.
                        .padding(GoldenDeskMargin),
                ) { content() }
            }
        }
        composeRule.waitForIdle()

        // Non-vacuity: the sheet's own body must be present, so a later blanked re-record fails here rather
        // than passing on a desk-pixel count.
        composeRule.onNodeWithTag(BenchArtSheetTestTag).assertExists()
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val bounds = composeRule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot
        val bmp = cropToBounds(full, bounds)
        assertTrue("the host desk did not paint ($name)", bmp.pixelCountOf(deskArgb) > 100)

        // `.tile{background:var(--leaf-tint)}` with `nth-child(3n)`→berry and `(4n)`→butter. All three
        // grounds must be on screen: a transcription that drops the cycle and paints sixteen leaf tiles
        // moves well under 2 % of the frame and passes the raster gate untouched.
        //
        // ⚠ The margin here is thinner than the tile count suggests, and the first draft of this comment
        // got it wrong. Each family row is leaf·leaf·berry·butter, so the grid holds 8 leaf / 4 berry / 4
        // butter — but twelve of the sixteen are DIMMED, and a dimmed ground is no longer the token. Only
        // the authored *Cut shapes* row paints these colours at full strength: **2 leaf, 1 berry, 1 butter**
        // tiles at ~83dp, which is still several thousand pixels each at xhdpi. 500 clears that with room
        // and is far above any incidental match, but it is one tile of headroom, not four.
        assertTrue("no --leaf-tint tile ground ($name)", bmp.pixelCountOf(leafTintArgb) > 500)
        assertTrue("no --berry-tint tile ground ($name)", bmp.pixelCountOf(berryTintArgb) > 500)
        assertTrue("no --butter-tint tile ground ($name)", bmp.pixelCountOf(butterTintArgb) > 500)

        // The twelve unauthored tiles are dimmed, and the dim reaches the GROUND — so a full-strength tint
        // must cover far less of the sheet than a sixteen-live grid would. Expressed as a ratio between the
        // authored quarter and the whole rather than as an absolute count, which would pin the raster's
        // density rather than the design's rule.
        val authored = cropToBounds(
            full,
            composeRule.onNodeWithTag(benchArtTileTestTag("shape.rect")).fetchSemanticsNode().boundsInRoot,
        )
        val unauthored = cropToBounds(
            full,
            composeRule.onNodeWithTag(benchArtTileTestTag("tape.torn")).fetchSemanticsNode().boundsInRoot,
        )
        // Same grid position (1st in its row) ⇒ same token ground, so the ONLY difference between these two
        // crops is the dim. If the disabled state were dropped, both would count the same.
        assertTrue(
            "the unauthored tile is not dimmed ($name): it paints the full-strength ground",
            authored.pixelCountOf(leafTintArgb) > unauthored.pixelCountOf(leafTintArgb) * 2,
        )

        bmp.captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Test
    fun bench_art_sheet_light() =
        capture("bench_art_sheet_light", darkTheme = false) { Sheet() }

    @Test
    fun bench_art_sheet_dark() =
        capture("bench_art_sheet_dark", darkTheme = true) { Sheet() }

    @Composable
    private fun Sheet() {
        ZSheetSurface(title = BenchArtSheetTitle, sub = null) {
            BenchArtSheetBody(onPick = {})
        }
    }

    /**
     * The Recent shelf, which production does not draw today (§9's deferral) — captured so the row's
     * *structure* is pinned before recents land, rather than being designed for the first time under the
     * pressure of shipping them.
     */
    @Test
    fun bench_art_sheet_with_recents_light() =
        capture("bench_art_sheet_with_recents_light", darkTheme = false) {
            ZSheetSurface(title = BenchArtSheetTitle, sub = null) {
                BenchArtSheetBody(
                    onPick = {},
                    recent = Copy.Supplies.BY_FAMILY.getValue(Copy.Supplies.CUT_SHAPES).keys.take(2).toList(),
                )
            }
        }
}
