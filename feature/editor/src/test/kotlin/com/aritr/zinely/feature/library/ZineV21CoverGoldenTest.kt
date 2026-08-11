package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.ZineCoverSurface
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Colors
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Parity rasters of the **V2.1** cover, both themes — the artifact laid beside
 * `docs/design/mockups/v21-library.html` at the review gate.
 *
 * Same contract as [ZineCoverGoldenTest]: a plain `testDebugUnitTest` neither records nor verifies, so
 * green here only proves the covers compose without throwing. The pixel comparison is a human step run
 * with `-Proborazzi.test.verify`.
 *
 * **Deliberately separate from [ZineCoverGoldenTest] and its `v2_cover_*` rasters.** The V2 tile still
 * ships, its three goldens still guard it, and nothing about this file may disturb them until the shelf
 * stops calling [ZineCover].
 *
 * The six surfaces are the frozen file's own classes — `.ink-leaf`, `.paper-s`, `.ink-berry`,
 * `.paper-c`, `.ink-butter`, `.ink-jam` — in the order the prototype's shelf lists them, so the two can
 * be read side by side, each wearing its own mark from [ZineV21CoverMarks]. Two to a row: the shelf's
 * real two-column geometry belongs to the shelf, and this raster's job is to show every surface at a
 * size where the marks and the stamp text are legible.
 *
 * Each tile passes its **shelf index**, because the corpus keys tilt and tape placement off
 * `:nth-child(3n+k)` — so the three-cycle is visible down the sheet rather than having to be reasoned
 * about.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Taller than the V2 raster's window: six 120dp covers at 3:4 in two columns is three rows of 160dp
// plus gaps, which needs more height than ZineCoverGoldenTest's w480dp-h1100dp gives.
@Config(qualifiers = "w480dp-h1600dp")
class ZineV21CoverGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val TAG = "v21CoverSheet"

        val CELL = 120.dp
        const val PER_ROW = 2

        /**
         * Generous, and not arbitrary. The tape overflows the cover 11dp above its top edge, the stamp
         * 7dp past its right and 9dp below its bottom, and the hard shadow another 4dp down-right —
         * none of which is clipped, by design. A padding that merely looked comfortable would crop the
         * three details this component exists to add.
         */
        val GAP = 28.dp

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )

        /**
         * `.ink-leaf .paper-s .ink-berry .paper-c .ink-butter .ink-jam`, with **the frozen file's own
         * stamp labels** — `A4, Letter, A4, A4, Letter, A4`. An earlier version substituted A5 and A6
         * to get more label widths under test, while the class KDoc claimed the six could be read side
         * by side against the prototype. They could not.
         */
        data class Cover(
            val fill: Color,
            val onFill: Color,
            val borderInk: Color,
            val stamp: String,
            val mark: ImageVector,
        )

        /**
         * **Resolved through the production mapping, not restated beside it.**
         *
         * This table used to write `c.paper` and `c.butterTint` itself, and that made it a second source
         * of design truth. ADR-100 pinned the two paper stocks to their light values in
         * `ZineCoverSurface.v21Fill`, and this raster — the one artifact whose whole job is to show the
         * six stocks side by side — went on recording the old dark ones and passing its own comparison.
         * Exactly the failure `ZineShelfGoldenFixture` was extracted to prevent, one file over.
         *
         * The surfaces are listed in the frozen file's own order with the frozen file's own stamp labels
         * (`A4, Letter, A4, A4, Letter, A4`); everything about how they are *painted* now comes from the
         * functions the shelf calls.
         */
        val FrozenOrder = listOf(
            ZineCoverSurface.MatchaInk to ("A4" to ZineV21CoverMarks.Booklet),
            ZineCoverSurface.PaperStrawberryBand to ("Letter" to ZineV21CoverMarks.Envelope),
            ZineCoverSurface.StrawberryInk to ("A4" to ZineV21CoverMarks.Rings),
            ZineCoverSurface.PaperMatchaBand to ("A4" to ZineV21CoverMarks.Sprig),
            ZineCoverSurface.OchreInk to ("Letter" to ZineV21CoverMarks.Lines),
            ZineCoverSurface.TealInk to ("A4" to ZineV21CoverMarks.Mug),
        )

        fun frozen(c: ZinelyV21Colors) = FrozenOrder.map { (surface, label) ->
            Cover(
                fill = surface.v21Fill(c),
                onFill = surface.v21MarkInk(c),
                borderInk = surface.v21BorderInk(c),
                stamp = label.first,
                mark = label.second,
            )
        }
    }

    @Test
    fun `six covers light`() = sheet("light", dark = false)

    @Test
    fun `six covers dark`() = sheet("dark", dark = true)

    @Test
    fun `a pressed cover squares up against the desk`() {
        // The press does two things at once here and the raster has to show both: the Hero tier's
        // travel-and-shed, AND `rotate(0deg)` — a pressed cover loses its tilt. Index 0 is the -1.4deg
        // tile, so the difference against the light sheet's first cover is visible.
        composeRule.setContent {
            ZinelyTheme(darkTheme = false) {
                val c = ZinelyTheme.v21Colors
                Box(
                    Modifier
                        .testTag(TAG)
                        .background(c.desk)
                        .padding(GAP),
                ) {
                    ZineV21Cover(
                        fill = ZineCoverSurface.MatchaInk.v21Fill(c),
                        stampLabel = "A4",
                        index = 0,
                        pressed = true,
                        borderInk = ZineCoverSurface.MatchaInk.v21BorderInk(c),
                        modifier = Modifier.width(CELL),
                    ) { markModifier ->
                        Image(
                            imageVector = ZineV21CoverMarks.Booklet,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(ZineCoverSurface.MatchaInk.v21MarkInk(c)),
                            modifier = markModifier.aspectRatio(1f),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        capture("pressed_light")
    }

    private fun sheet(name: String, dark: Boolean) {
        composeRule.setContent { CoverSheet(dark) }
        composeRule.waitForIdle()
        capture("six_$name")
    }

    @Composable
    private fun CoverSheet(dark: Boolean) {
        ZinelyTheme(darkTheme = dark) {
            val c = ZinelyTheme.v21Colors
            Column(
                Modifier
                    .background(c.desk)
                    .padding(GAP)
                    .testTag(TAG),
                verticalArrangement = Arrangement.spacedBy(GAP),
            ) {
                // Two to a row. Three fits the window, but at 120dp cells the marks and the 9.5sp
                // stamp text stop being readable in a raster a human is meant to compare by eye.
                frozen(c).chunked(PER_ROW).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                        row.forEachIndexed { i, cover ->
                            ZineV21Cover(
                                fill = cover.fill,
                                stampLabel = cover.stamp,
                                index = rowIndex * PER_ROW + i,
                                borderInk = cover.borderInk,
                                modifier = Modifier.width(CELL),
                            ) { markModifier ->
                                Image(
                                    imageVector = cover.mark,
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(cover.onFill),
                                    modifier = markModifier.aspectRatio(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Captures the **root** rather than the tagged node.
     *
     * The raster measures **324×592**, which is the sheet exactly: `28 + 120 + 28 + 120 + 28` across,
     * and `28 + 3×160 + 2×28 + 28` down. Every deliberate overflow lands inside the 28dp padding —
     * the tape 11dp above the first row, the stamps 7dp right and 9dp below, the hard shadow 4dp
     * down-right — so nothing this component adds is cut.
     *
     * That is worth stating because it was recorded as a **defect first**. Viewed at reduced scale the
     * sheet reads as cropped: the right-hand stamps look shaved and the last row looks cut. Three
     * changes were made against that impression — three columns to two, `h1100dp` to `h1600dp`, and
     * `onNodeWithTag` to [onRoot] — and none of them moved the output by a pixel, because there was
     * nothing to move. Reading the PNG's own header settled it in one command.
     *
     * Two of those changes are kept on their own merits (two columns leaves the surfaces legible; the
     * root is the honest thing to capture when overflow is the point). The lesson is the third:
     * **a raster is measurable, so measure it.** An impression of a rendering is not a reading of one.
     */
    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage("$GOLDEN_DIR/v21_cover_$name.png", aa())
    }
}
