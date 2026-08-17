package com.aritr.zinely.ui.components

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyV2ShadowLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/**
 * [zinelyV2Shadow] — the V2 box-shadow, and specifically the two things V1's [zinelyShadow] cannot do:
 * **spread** (20 of V2's 25 chrome shadows carry it, always negative) and CSS's front-to-back layer
 * order.
 *
 * Every assertion reads the raster. A shadow drawn with the spread silently dropped, in the wrong
 * order, or upward instead of downward all compose perfectly well; only pixels tell them apart.
 *
 * Each test renders **once** — two objects side by side when it needs to compare two shadow sets, since
 * `setContent` may only be called once per test and re-rendering would otherwise be the obvious
 * (broken) way to write this.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h960dp", sdk = [28])
class ZinelyV2ShadowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val HOST = "shadow-host"
        const val LEFT = "shadow-left"
        const val RIGHT = "shadow-right"
        val SIDE = 90.dp
        val GUTTER = 40.dp
        val OPAQUE_RED = Color(0xFFFF0000)
        val OPAQUE_BLUE = Color(0xFF0000FF)
    }

    @Test
    fun `a negative spread pulls the shadow in under its object`() {
        // The frozen cover's own layer, with and without its -16dp spread. A spread the draw path
        // dropped would make these two rasters identical — which is exactly the defect to catch.
        renderPair(
            left = listOf(layer(dy = 20.dp, blur = 24.dp, spread = 0.dp)),
            right = listOf(layer(dy = 20.dp, blur = 24.dp, spread = (-16).dp)),
        )
        val decor = decorRaster()
        val none = darknessBelow(decor, LEFT)
        val pulled = darknessBelow(decor, RIGHT)
        assertTrue(
            "a -16dp spread must lay less dark on the desk than no spread (none $none, spread $pulled)",
            pulled < none,
        )
    }

    @Test
    fun `a positive spread haloes wider than none`() {
        renderPair(
            left = listOf(layer(dy = 8.dp, blur = 8.dp, spread = 0.dp)),
            right = listOf(layer(dy = 8.dp, blur = 8.dp, spread = 8.dp)),
        )
        val decor = decorRaster()
        val none = darknessBelow(decor, LEFT)
        val grown = darknessBelow(decor, RIGHT)
        assertTrue("a positive spread must widen the cast (none $none, grown $grown)", grown > none)
    }

    @Test
    fun `dy casts downward, and a negative dy casts upward`() {
        renderPair(
            left = listOf(layer(dy = 16.dp, blur = 6.dp, spread = 0.dp)),
            right = listOf(layer(dy = (-16).dp, blur = 6.dp, spread = 0.dp)),
        )
        val decor = decorRaster()
        assertTrue(
            "a positive dy belongs below its object",
            darknessBelow(decor, LEFT) > darknessAbove(decor, LEFT),
        )
        assertTrue(
            "a negative dy casts upward — the Bench's bottom sheet and the Proof's ready band do",
            darknessAbove(decor, RIGHT) > darknessBelow(decor, RIGHT),
        )
    }

    @Test
    fun `the first declared layer paints on top, as CSS orders them`() {
        // Two opaque, zero-blur, zero-spread layers at the same offset: whichever paints last is the
        // only colour visible. CSS paints the FIRST-declared shadow above those after it.
        renderPair(
            left = listOf(
                ZinelyV2ShadowLayer(dy = 30.dp, blur = 0.dp, color = OPAQUE_RED),
                ZinelyV2ShadowLayer(dy = 30.dp, blur = 0.dp, color = OPAQUE_BLUE),
            ),
            right = listOf(
                ZinelyV2ShadowLayer(dy = 30.dp, blur = 0.dp, color = OPAQUE_BLUE),
                ZinelyV2ShadowLayer(dy = 30.dp, blur = 0.dp, color = OPAQUE_RED),
            ),
        )
        val decor = decorRaster()
        assertEquals("red declared first must be the visible one", OPAQUE_RED, sampleBelow(decor, LEFT))
        assertEquals("blue declared first must be the visible one", OPAQUE_BLUE, sampleBelow(decor, RIGHT))
    }

    @Test
    fun `a Generic outline with spread is refused rather than silently flattened`() {
        // Driven directly rather than through a render: the guard fires inside the draw pass, and a
        // draw-pass throw takes the compose test rule's own teardown down with it, so the render route
        // cannot distinguish "guard fired" from "test harness broke".
        val triangle = Outline.Generic(
            Path().apply {
                moveTo(0f, 0f)
                lineTo(10f, 0f)
                lineTo(5f, 10f)
                close()
            },
        )
        assertTrue(
            "a Generic outline with no spread is fine",
            !spreadPath(triangle, 0f).isEmpty,
        )
        val failure = runCatching { spreadPath(triangle, -4f) }.exceptionOrNull()
        assertTrue(
            "a spread on a Generic outline must fail loudly rather than draw an unspread halo, got $failure",
            failure is IllegalArgumentException,
        )
    }

    @Test
    fun `spread grows the box by the same amount on every side`() {
        val rounded = Outline.Rounded(
            RoundRect(
                left = 0f,
                top = 0f,
                right = 100f,
                bottom = 100f,
                topLeftCornerRadius = CornerRadius(6f),
                topRightCornerRadius = CornerRadius(9f),
                bottomRightCornerRadius = CornerRadius(9f),
                bottomLeftCornerRadius = CornerRadius(6f),
            ),
        )
        // CSS Backgrounds §7.1.1: the shadow's box is inset by -spread on every side. This only pins the
        // bounding box, which a uniformly-scaled (wrong) implementation would also satisfy — the radius
        // itself is checked separately below, since `getBounds()` cannot see a corner's curvature.
        val grown = spreadPath(rounded, 4f).getBounds()
        assertEquals("left", -4f, grown.left, 0.01f)
        assertEquals("top", -4f, grown.top, 0.01f)
        assertEquals("right", 104f, grown.right, 0.01f)
        assertEquals("bottom", 104f, grown.bottom, 0.01f)

        // Negative spread shrinks it, and a radius can never curve inward.
        val shrunk = spreadPath(rounded, -8f).getBounds()
        assertEquals("shrunk left", 8f, shrunk.left, 0.01f)
        assertEquals("shrunk right", 92f, shrunk.right, 0.01f)
    }

    @Test
    fun `spread grows each corner radius additively, not by a uniform scale`() {
        // A box large enough, and a spread wide enough, that additive growth (correct) and a
        // proportional scale about the centre (the obvious wrong implementation) disagree by several
        // pixels at the corner — not the sub-pixel gap the cover's own 6/9/9/6 radii would give at a
        // realistic spread, which `getBounds()` alone cannot resolve either way.
        val rounded = Outline.Rounded(
            RoundRect(
                left = 0f,
                top = 0f,
                right = 200f,
                bottom = 200f,
                topLeftCornerRadius = CornerRadius(20f),
                topRightCornerRadius = CornerRadius(20f),
                bottomRightCornerRadius = CornerRadius(20f),
                bottomLeftCornerRadius = CornerRadius(20f),
            ),
        )
        val grown = spreadPath(rounded, 20f)
        // Additive: radius 20 + 20 = 40. A uniform scale about the centre that reproduces the same
        // 240×240 bounding box would instead scale the radius by 1.2×, to 24.
        //
        // On the diagonal at distance d from the corner, a circle of radius r covers the point once
        // d >= r * (1 - 1/sqrt(2)) =~ r * 0.293. At d = 9: r=40 (correct) does not yet cover it — the
        // corner is still cut away; r=24 (scaled) already does.
        val probe = grown.getBounds().let { it.left.toInt() + 9 to it.top.toInt() + 9 }
        val region = android.graphics.Region().apply {
            setPath(
                grown.asAndroidPath(),
                android.graphics.Region(
                    grown.getBounds().left.toInt() - 1,
                    grown.getBounds().top.toInt() - 1,
                    grown.getBounds().right.toInt() + 1,
                    grown.getBounds().bottom.toInt() + 1,
                ),
            )
        }
        assertTrue(
            "a 40px corner radius must still cut away the point 9px in on the diagonal (got r~24 instead)",
            !region.contains(probe.first, probe.second),
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Harness
    // ---------------------------------------------------------------------------------------------

    private fun layer(dy: Dp, blur: Dp, spread: Dp) = ZinelyV2ShadowLayer(
        dy = dy,
        blur = blur,
        spread = spread,
        color = Color.Black.copy(alpha = 0.5f),
    )

    private fun renderPair(
        left: List<ZinelyV2ShadowLayer>,
        right: List<ZinelyV2ShadowLayer>,
        shape: Shape = RectangleShape,
    ) {
        composeRule.setContent { Host(left, right, shape) }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Host(
        left: List<ZinelyV2ShadowLayer>,
        right: List<ZinelyV2ShadowLayer>,
        shape: Shape,
    ) {
        Box(Modifier.fillMaxSize().background(Color.White).testTag(HOST)) {
            Row(
                Modifier.padding(GUTTER),
                horizontalArrangement = Arrangement.spacedBy(GUTTER),
            ) {
                Swatch(left, shape, LEFT)
                Swatch(right, shape, RIGHT)
            }
        }
    }

    @Composable
    private fun Swatch(layers: List<ZinelyV2ShadowLayer>, shape: Shape, tag: String) {
        Box(
            Modifier
                .size(SIDE)
                .zinelyV2Shadow(layers, shape)
                .clip(shape)
                .background(Color(0xFF808080))
                .testTag(tag),
        )
    }

    /** Mean darkness laid on the white host in a 12dp strip immediately below the object. */
    private fun darknessBelow(decor: Bitmap, tag: String): Float = strip(decor, tag) { top, bottom ->
        bottom + 1 to bottom + dp(12f)
    }

    /** The same, immediately above it. */
    private fun darknessAbove(decor: Bitmap, tag: String): Float = strip(decor, tag) { top, _ ->
        top - dp(12f) to top - 1
    }

    private fun strip(
        decor: Bitmap,
        tag: String,
        rows: (top: Int, bottom: Int) -> Pair<Int, Int>,
    ): Float {
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val (from, to) = rows(bounds.top.roundToInt(), bounds.bottom.roundToInt())
        var dark = 0f
        var counted = 0
        for (y in from..to) {
            for (x in bounds.left.roundToInt() until bounds.right.roundToInt()) {
                dark += 1f - Color(decor.getPixel(x, y)).luminance()
                counted++
            }
        }
        require(counted > 0) { "no host pixels sampled around $tag" }
        return dark / counted
    }

    private fun sampleBelow(decor: Bitmap, tag: String): Color {
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        return Color(decor.getPixel(bounds.center.x.roundToInt(), bounds.bottom.roundToInt() + dp(10f)))
    }

    private fun decorRaster(): Bitmap {
        assertEquals(
            "these pixel offsets assume dp == px; density was ${composeRule.density.density}",
            1.0f,
            composeRule.density.density,
            0.0001f,
        )
        return composeRule.activity.window.decorView.rasterizeToBitmap()
    }

    private fun dp(value: Float): Int = (value * composeRule.density.density).roundToInt()

    private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
}
