package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 * The frozen screen with its sheet open, both themes — B3's side-by-side artifact.
 *
 * **The whole screen, not the sheet alone.** The frozen file's own reading of an open sheet is the shelf
 * dimmed behind it (`:119-123`, `body.sheet-open`), and the two halves are only checkable together: the
 * scrim's dimming reads against the covers it dims, and the sheet's 10px inset reads against the phone's
 * edge. So this composes shelf → scrim → sheet in the frozen paint order.
 *
 * **Composed, not hosted in the `Dialog`.** The real [ZineActionSheet] puts its scrim and card in a window
 * of their own, which the decor-view raster cannot see — the same reason V1's goldens capture
 * `ZSheetSurface`. The pieces here are the production composable ([ZineActionSheetSurface]) and the frozen
 * scrim literal, arranged the way [ZineActionSheet] arranges them; what this cannot show is the *slide*,
 * which is a device-verification matter rather than a raster one.
 *
 * **What is legitimately absent:** the `.dock` and its "Make a zine" button (`:88-95`) — **B4**. Every other
 * element of an open Library is here.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h812dp")
class ZineActionSheetGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val TAG = "sheet-viewport"

        /**
         * `.sheet{position:absolute;left:0;right:0;bottom:0}` — flush, which is *why*
         * `border-radius:36px 36px 0 0` squares off the bottom two corners.
         *
         * This host wrote **10dp** and a KDoc quoting a `left:10px;right:10px;bottom:10px` rule that does
         * not exist in the frozen file, so the recorded raster showed the paper spanning x 10→381 of 392
         * and stopping 10 rows short of the bottom, with dimmed shelf visible underneath and the square
         * corners rendered as rounded desk. Production never did this — [ZineActionSheet] aligns the
         * surface `BottomCenter` with no padding — so it was the parity artifact itself that was wrong,
         * and it had been ratifying its own mistake since B3.
         */
        val INSET = 0.dp

        /** The zine the frozen sheet is open for — `:172` shows exactly this header. */
        val TARGET = ZineActionTarget("Sunday market", "A4 · 2 days ago")

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    @Test
    fun `the frozen sheet over the shelf light`() = sheet("light", dark = false)

    @Test
    fun `the frozen sheet over the shelf dark`() = sheet("dark", dark = true)

    private fun sheet(name: String, dark: Boolean) {
        composeRule.setContent { Screen(dark) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG).captureRoboImage("$GOLDEN_DIR/v21_sheet_$name.png", aa())
    }

    @Composable
    private fun Screen(dark: Boolean) {
        ZinelyTheme(darkTheme = dark) {
            Box(
                Modifier
                    .testTag(TAG)
                    .fillMaxSize()
                    .background(ZinelyTheme.v21Colors.desk),
            ) {
                ZineShelf(
                    zines = ZineShelfGoldenFixture.FROZEN,
                    onOpen = {},
                    onActions = {},
                    modifier = Modifier.fillMaxSize(),
                )
                // The production scrim, not a copy of its literal: a raster recorded from a second copy
                // would keep agreeing with itself after the sheet's own paint changed (**D-022**).
                ZineActionScrim(onDismiss = {})
                Box(Modifier.align(Alignment.BottomCenter).padding(INSET)) {
                    ZineActionSheetSurface(target = TARGET, onAction = {})
                }
            }
        }
    }
}
