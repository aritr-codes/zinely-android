package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/**
 * The frozen `.cover` object. Two proofs, as in [com.aritr.zinely.ui.components.ZComponentGoldenTest]:
 * behavioural pixel assertions that hold under a plain unit run, plus committed Roborazzi goldens of
 * all four archetypes in both themes.
 *
 * Every render is pinned to reduced motion — the settle animation is a delay-then-tween and would
 * otherwise decide the captured frame.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShelfCoverTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val TAG = "shelfCoverGallery"
        val PAPER = Color(0xFFF4EFE6).toArgb()
        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    @Test
    fun `the tilt cycles every third object, as the spec's nth-child rules do`() {
        assertEquals(-1.1f, shelfTilt(0), 0f)
        assertEquals(0.8f, shelfTilt(1), 0f)
        assertEquals(-0.4f, shelfTilt(2), 0f)
        assertEquals(-1.1f, shelfTilt(3), 0f)
    }

    /** `top:32%; height:47%` — the ink owns the middle; title and edition keep their paper. */
    @Test
    fun `the ink band prints in the middle third and nowhere else`() {
        val bmp = capture(darkTheme = false) {
            Cover(ShelfCoverRecipe(ShelfArchetype.Sun, ShelfInk.Coral, accent = null, usePaper2 = false), index = 1)
        }
        // Off the centre column: the fold crease lives there. Off the top-left corner: the stock's
        // radial shade lives there. 70% across is plain paper above and below the band.
        val x = (bmp.width * 0.7f).roundToInt()
        assertEquals("the band bled above 32%", PAPER, bmp.getPixel(x, (bmp.height * 0.14f).roundToInt()))
        assertTrue(
            "no ink printed inside the band",
            bmp.getPixel(x, (bmp.height * 0.55f).roundToInt()) != PAPER,
        )
        assertEquals("the band bled below 79%", PAPER, bmp.getPixel(x, (bmp.height * 0.88f).roundToInt()))
    }

    /** `mix-blend-mode:multiply` — ink darkens paper, it never lightens it. */
    @Test
    fun `the band multiplies onto the sheet`() {
        val bmp = capture(darkTheme = false) {
            Cover(ShelfCoverRecipe(ShelfArchetype.Sun, ShelfInk.Yellow, accent = null, usePaper2 = false), index = 1)
        }
        val inked = bmp.getPixel((bmp.width * 0.7f).roundToInt(), (bmp.height * 0.55f).roundToInt())
        // Yellow (#E9C46A) is *lighter* than paper (#F4EFE6) in every channel; only multiply darkens.
        for (shift in intArrayOf(16, 8, 0)) {
            val ink = (inked shr shift) and 0xFF
            val paper = (PAPER shr shift) and 0xFF
            assertTrue("channel $shift lightened: $ink vs $paper", ink <= paper)
        }
    }

    @Test
    fun `archetype gallery light`() {
        gallery(darkTheme = false).captureRoboImage("$GOLDEN_DIR/shelf_cover_light.png", aa())
    }

    @Test
    fun `archetype gallery dark`() {
        gallery(darkTheme = true).captureRoboImage("$GOLDEN_DIR/shelf_cover_dark.png", aa())
    }

    @Composable
    private fun Cover(recipe: ShelfCoverRecipe, index: Int) {
        ShelfCover(recipe, index, Modifier.width(120.dp).shelfLedge()) {}
    }

    private fun gallery(darkTheme: Boolean): Bitmap = capture(darkTheme) {
        Row(
            modifier = Modifier.background(ZinelyTheme.colors.desk).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShelfArchetype.entries.forEachIndexed { i, archetype ->
                // Every archetype with a second ink, so the misregistration is goldened too.
                Box(Modifier.width(96.dp)) {
                    ShelfCover(
                        recipe = ShelfCoverRecipe(archetype, ShelfInk.Coral, ShelfInk.Teal, usePaper2 = i % 2 == 1),
                        index = i,
                        modifier = Modifier.shelfLedge().padding(bottom = 16.dp),
                    ) {}
                }
            }
        }
    }

    private fun capture(darkTheme: Boolean, content: @Composable () -> Unit): Bitmap {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalZinelyMotion provides ZinelyMotion(reduceMotion = true)) {
                    Box(Modifier.testTag(TAG)) { content() }
                }
            }
        }
        composeRule.waitForIdle()
        val bounds = composeRule.onNodeWithTag(TAG).fetchSemanticsNode().boundsInRoot
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val x = bounds.left.roundToInt().coerceAtLeast(0)
        val y = bounds.top.roundToInt().coerceAtLeast(0)
        return Bitmap.createBitmap(
            full, x, y,
            bounds.width.roundToInt().coerceAtMost(full.width - x),
            bounds.height.roundToInt().coerceAtMost(full.height - y),
        )
    }
}
