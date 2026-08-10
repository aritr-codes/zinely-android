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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
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
 * be read side by side. Three to a row, matching the V2 raster's arrangement for the same reason: the
 * shelf's own two-column geometry belongs to the shelf, and this raster's job is to show every surface.
 *
 * Each tile passes its **shelf index**, because the corpus keys tilt and tape placement off
 * `:nth-child(3n+k)` — so the three-cycle is visible across a row rather than having to be reasoned
 * about. The mark slot is left empty: V2.1's glyph set is not transcribed yet, and a stand-in shape
 * would put a mark in the parity raster that the specification does not contain.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Taller than the V2 raster's window: six 120dp covers at 3:4 in two columns is three rows of 160dp
// plus gaps, and h1100dp cropped the last one. The crop is the reason this qualifier is not copied
// from ZineCoverGoldenTest.
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

        /** `.ink-leaf .paper-s .ink-berry .paper-c .ink-butter .ink-jam`, with their stamps. */
        fun frozen(c: ZinelyV21Colors) = listOf(
            Triple(c.leaf, MarkOnInk, "A4"),
            Triple(c.paper, c.inkSoft, "Letter"),
            Triple(c.berry, MarkOnInk, "A5"),
            Triple(c.butterTint, c.inkSoft, "A4"),
            Triple(c.butter, MarkOnInk, "A6"),
            Triple(c.jam, MarkOnInk, "A4"),
        )

        /**
         * `.cover .mark{color:rgba(255,246,232,.92)}` — hardcoded in the frozen file and theme-invariant,
         * per V21-SPEC §4.1. `.paper-s`/`.paper-c` override it to `ink-soft`, which is why the table
         * above carries the mark colour per surface rather than deriving it.
         */
        val MarkOnInk = Color(0xEBFFF6E8)
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
                        fill = c.leaf,
                        onFill = MarkOnInk,
                        stampLabel = "A4",
                        index = 0,
                        pressed = true,
                        modifier = Modifier.width(CELL),
                    )
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
                // Two to a row, not three. At three the sheet is 472dp wide inside a 480dp window and
                // the rightmost stamp — which hangs 7dp past the cover — was clipped, along with the
                // whole bottom row. A parity raster that crops the details being reviewed is worse
                // than no raster: it reads as "checked".
                frozen(c).chunked(PER_ROW).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                        row.forEachIndexed { i, (fill, onFill, stamp) ->
                            ZineV21Cover(
                                fill = fill,
                                onFill = onFill,
                                stampLabel = stamp,
                                index = rowIndex * PER_ROW + i,
                                modifier = Modifier.width(CELL),
                            )
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
