package com.aritr.zinely.feature.library

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The loading shelf's shimmer under **"Remove animations"** — the one continuous animation the V2.1
 * Library has, and therefore the one place [ZinelyV2Motion.allowsContinuousMotion] is actually load-bearing.
 *
 * ### Why the assertion is a raster diff and not a source check
 *
 * The policy that governs this sweep is [D-012](docs/design/V2-SPEC-DEFECTS.md) as ratified by **OD-25**:
 * a one-shot **collapses to zero and still arrives**, a loop **must not run at all**. Those two are
 * indistinguishable in the source — `tween(motion.durationMillis(1400))` inside an `infiniteRepeatable`
 * reads as correctly gated, compiles, and produces an unbounded loop with no delay, which is a strobe
 * rather than stillness. That is precisely the failure a reduced-motion preference exists to prevent, so
 * the only honest assertion is the one C9 used on the Bench's caret: rasterise two frames a second apart
 * and compare the bytes.
 *
 * The first draft of the sweep did in fact bypass the policy entirely — `BenchC9Test`'s enrolment scan
 * caught it before the diff did — but that scan checks that *some* policy call sits near an animation
 * statement, not which one. This file checks the behaviour.
 *
 * ### The control matters as much as the probe
 *
 * `the shimmer does sweep by default` is not decoration: a placeholder that never animates for any reason
 * (a mis-gated flag, a zero-width gradient, a stopped clock) passes the reduced-motion assertion perfectly.
 * The pair only means something together.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h960dp", sdk = [28])
class ZineShelfMotionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val SHELF = "shelf-loading"
        const val PLACEHOLDERS = 4

        /** A second of clock — the sweep's own period is 1400ms, so this lands mid-travel, not back at 0. */
        const val ADVANCE_MILLIS = 1_000L
    }

    @Test
    fun `with animations removed, the loading shelf holds perfectly still`() {
        forceReduceMotion()
        loadingShelf()

        val before = raster()
        composeRule.mainClock.advanceTimeBy(ADVANCE_MILLIS)
        composeRule.waitForIdle()
        val after = raster()

        // Threshold-free, as C9's caret probe was: not "close enough", but not one byte different.
        assertEquals(
            "the placeholder shimmer must not run at all under a reduced-motion preference",
            0,
            differingPixels(before, after),
        )
    }

    @Test
    fun `by default the shimmer sweeps, so the assertion above is measuring something`() {
        loadingShelf()

        val before = raster()
        composeRule.mainClock.advanceTimeBy(ADVANCE_MILLIS)
        composeRule.waitForIdle()
        val after = raster()

        assertNotEquals(
            "the loading shelf must animate when motion is allowed",
            0,
            differingPixels(before, after),
        )
    }

    // -------------------------------------------------------------------------------------------

    private fun loadingShelf() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ZinelyTheme {
                ZineShelf(
                    zines = emptyList(),
                    onOpen = {},
                    onActions = {},
                    modifier = Modifier.fillMaxSize().testTag(SHELF),
                    placeholders = PLACEHOLDERS,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun raster(): Bitmap = composeRule.activity.window.decorView.rasterizeToBitmap()

    private fun differingPixels(a: Bitmap, b: Bitmap): Int {
        assertEquals("frame width", a.width, b.width)
        assertEquals("frame height", a.height, b.height)
        var differing = 0
        for (y in 0 until a.height) {
            for (x in 0 until a.width) {
                if (a.getPixel(x, y) != b.getPixel(x, y)) differing++
            }
        }
        return differing
    }

    /** Android's "Remove animations" (`ANIMATOR_DURATION_SCALE = 0`) — the reduced-motion signal. */
    private fun forceReduceMotion() {
        android.provider.Settings.Global.putFloat(
            org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
    }
}
