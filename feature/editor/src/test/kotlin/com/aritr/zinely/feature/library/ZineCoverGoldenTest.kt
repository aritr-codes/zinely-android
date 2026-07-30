package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Parity rasters of the six frozen covers, both themes — the artifact the Phase B review gate lays
 * beside `docs/design/mockups/v2-library.html`.
 *
 * **What a green run here proves, and what it does not.** A plain `testDebugUnitTest` neither writes nor
 * asserts: without `-Proborazzi.test.record` or `-Proborazzi.test.verify`,
 * `finalizeTestRoborazziDebug` is skipped and the rasters keep whatever mtime they already had — a
 * `--rerun-tasks` run with no flag leaves every PNG under `src/test/roborazzi` untouched. So a green plain run only
 * proves the six covers compose without throwing; it is not evidence the raster matches anything, and it
 * is not this file's record step either. The *pixel* comparison against the frozen HTML is a human review
 * step run with `-Proborazzi.test.verify`, and this file exists to make that step cheap. The assertions
 * that hold under a plain run live in [ZineCoverRenderTest] and [ZineCoverRecipeTest].
 *
 * The six titles are the frozen shelf's own (`v2-library.html:149-154`) so a reviewer can read the two
 * side by side — but each cover here is pinned to a **named surface and stamp** rather than hashed from
 * its title, because the raster's job is to show all six surfaces, not to re-test the recipe.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h1100dp")
class ZineCoverGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val TAG = "coverSheet"

        /**
         * Six 120dp covers, **three to a row**, because the test window's content area is 448×528px and
         * a taller arrangement runs off the bottom of the capture — a first recording at 140dp × two
         * columns cut the third row, and half a raster is not a parity artifact. The shelf's own frozen
         * geometry (two columns, `gap:28px 20px`) belongs to the shelf, which is B2; this raster's job is
         * to show all six *surfaces* at a readable size.
         */
        val CELL = 120.dp
        val GAP = 16.dp
        const val PER_ROW = 3

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )

        /** The frozen shelf's own six objects, in its own order. */
        val FROZEN = listOf(
            "Sunday market" to (ZineCoverSurface.MatchaInk to ZineCoverStamp.Sun),
            "Letters home" to (ZineCoverSurface.PaperStrawberryBand to ZineCoverStamp.Letter),
            "Riso tests" to (ZineCoverSurface.TealInk to ZineCoverStamp.Waves),
            "Mum's garden" to (ZineCoverSurface.PaperMatchaBand to ZineCoverStamp.Sprig),
            "Tiny poems" to (ZineCoverSurface.OchreInk to ZineCoverStamp.Star),
            "Coffee log" to (ZineCoverSurface.StrawberryInk to ZineCoverStamp.Face),
        )
    }

    @Test
    fun `six covers light`() = sheet("light", dark = false)

    @Test
    fun `six covers dark`() = sheet("dark", dark = true)

    @Test
    fun `a pressed cover settles`() {
        composeRule.setContent {
            ZinelyTheme(darkTheme = false) {
                Box(
                    // testTag first: the tagged node must span the desk *and* its padding, not just the
                    // cover, or the capture crops to the cover bounds and the press's only visible
                    // effect — the shadow settling into the desk — falls outside the raster.
                    Modifier
                        .testTag(TAG)
                        .background(ZinelyTheme.v2Colors.desk)
                        .padding(GAP),
                ) {
                    ZineCover(
                        title = "Sunday market",
                        recipe = ZineCoverRecipe(ZineCoverSurface.MatchaInk, ZineCoverStamp.Sun),
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
            // The shelf's own ground, so a cover's grounded shadow is read against the desk it sits on
            // rather than against whatever the test host's default surface happens to be.
            Column(
                Modifier
                    .background(ZinelyTheme.v2Colors.desk)
                    .padding(GAP)
                    .testTag(TAG),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                FROZEN.chunked(PER_ROW).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                        row.forEach { (title, look) ->
                            val (surface, stamp) = look
                            ZineCover(
                                title = title,
                                recipe = ZineCoverRecipe(surface, stamp),
                                modifier = Modifier.width(CELL),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun capture(name: String) {
        composeRule.onNodeWithTag(TAG).captureRoboImage("$GOLDEN_DIR/v2_cover_$name.png", aa())
    }
}
