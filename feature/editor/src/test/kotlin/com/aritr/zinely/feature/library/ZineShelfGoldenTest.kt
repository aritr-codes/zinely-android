package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
 * Parity rasters of the frozen shelf, both themes — the artifact B2's review gate lays beside
 * `docs/design/mockups/v2-library.html`.
 *
 * **What a green run here proves, and what it does not.** A plain `testDebugUnitTest` neither writes nor
 * asserts: without `-Proborazzi.test.record` or `-Proborazzi.test.verify`,
 * `finalizeTestRoborazziDebug` is skipped and the rasters keep whatever mtime they already had. So a
 * green plain run only proves the shelf composes without throwing. The measurable claims — the column
 * count, both gaps, all three paddings, the heading's placement, type and ink — are in
 * [ZineShelfTest], which asserts them under a plain run. This file exists so a reviewer can read the
 * shelf and the frozen file side by side, which is the one thing a number cannot do.
 *
 * **The window is the frozen phone.** `392dp × 812dp` is `.phone{width:392px;height:812px}`
 * (`v2-library.html:41`), so the raster is the frozen viewport rather than a convenient one — the column
 * width, the title wrapping and how much of the second row survives the fold are all functions of that
 * exact width.
 *
 * **The desk is painted by the host, not by the shelf.** `.shelf` declares no background; the desk
 * belongs to `.phone`, which is B5's screen ([ZineShelf]'s KDoc). So this file supplies it, exactly as
 * [ZineCoverGoldenTest] supplies a desk for a lone cover.
 *
 * ### These rasters are **not** a whole-screen rest-state match, and must not be read as one
 *
 * Two elements the frozen file draws at rest are legitimately absent, because they belong to later
 * packages — and both absences are **expected**, not deviations to be filed:
 *
 * - ~~**The six `⋯` buttons.**~~ **B3 landed them**, so these rasters now show one on every cover, at the
 *   frozen `opacity:.5` — `.more` (`:73-77`) is `display:flex` unconditionally, which is what makes it the
 *   *visible* fallback for long-press rather than a hover reveal. The B2 rasters were re-recorded for it.
 * - **The dock and "Make a zine".** `.dock` (`:88-95`) overlays the foot of the frozen screen. **B4.**
 *   The `152px` of empty room these rasters show below the last cover is exactly the space it will fill.
 *
 * So the comparison these rasters support is *the shelf* against the frozen shelf — grid, gaps, heading,
 * covers with their `⋯`, desk. A reviewer laying them beside `v2-library.html` should expect the dock's
 * absence and read everything else as parity.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h812dp")
class ZineShelfGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val TAG = "shelf-viewport"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )

        /**
         * The frozen six, from [ZineShelfGoldenFixture] — shared with B3's open-sheet raster so the two
         * cannot drift apart while each still passes its own comparison.
         */
        val FROZEN = ZineShelfGoldenFixture.FROZEN
    }

    @Test
    fun `the frozen shelf light`() = sheet("light", dark = false)

    @Test
    fun `the frozen shelf dark`() = sheet("dark", dark = true)

    private fun sheet(name: String, dark: Boolean) {
        composeRule.setContent { Desk(dark) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG).captureRoboImage("$GOLDEN_DIR/v2_shelf_$name.png", aa())
    }

    @Composable
    private fun Desk(dark: Boolean) {
        ZinelyTheme(darkTheme = dark) {
            // testTag outermost, so the capture is the whole viewport the shelf fills rather than a crop
            // of its content — the frozen file's own reading is the phone, dock room and all.
            Box(
                Modifier
                    .testTag(TAG)
                    .fillMaxSize()
                    .background(ZinelyTheme.v2Colors.desk),
            ) {
                ZineShelf(FROZEN, onOpen = {}, onActions = {}, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
