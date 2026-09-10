package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.render.android.ImageIntrinsics
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
 * Issue #55's golden net for the photo-bearing [LoadedReframeOverlay], complementing the chrome-only
 * [ReframeControlsGoldenTest]. The three states pin the frozen surface's distinct visual contracts:
 *
 *  1. centred Fill: crop boundary plus dimmed horizontal overflow;
 *  2. panned/zoomed Fill with both reflection axes: representative framing plus reflected overflow;
 *  3. Whole photo: contained pixels, visible frame ground, and no overflow scrim.
 *
 * Each state is captured in both room themes. The asymmetric four-colour photo makes either reflection
 * visible while keeping the fixture independent of image decoding and private/user assets.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class ReframeOverlayGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "reframeOverlayGoldenHost"
        const val PHOTO_WIDTH_PX = 320
        const val PHOTO_HEIGHT_PX = 180
        const val SCREEN_PX_PER_PT = 2f
        const val RED = 0xFFE45B4F.toInt()
        const val GREEN = 0xFF6F8F45.toInt()
        const val BLUE = 0xFF5577B8.toInt()
        const val YELLOW = 0xFFF2C94C.toInt()
        val PHOTO_COLOURS = intArrayOf(RED, GREEN, BLUE, YELLOW)

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private var paperArgb: Int = 0
    private var inkArgb: Int = 0
    private var deskEdgeArgb: Int = 0

    private fun photo(): ReframePhoto {
        val pixels = IntArray(PHOTO_WIDTH_PX * PHOTO_HEIGHT_PX) { index ->
            val x = index % PHOTO_WIDTH_PX
            val y = index / PHOTO_WIDTH_PX
            when {
                x < PHOTO_WIDTH_PX / 2 && y < PHOTO_HEIGHT_PX / 2 -> RED
                x >= PHOTO_WIDTH_PX / 2 && y < PHOTO_HEIGHT_PX / 2 -> GREEN
                x < PHOTO_WIDTH_PX / 2 -> BLUE
                else -> YELLOW
            }
        }
        val bitmap = Bitmap.createBitmap(
            pixels,
            PHOTO_WIDTH_PX,
            PHOTO_HEIGHT_PX,
            Bitmap.Config.ARGB_8888,
        )
        return ReframePhoto(
            intrinsic = ImageIntrinsics(PHOTO_WIDTH_PX, PHOTO_HEIGHT_PX),
            decoded = DecodedPhoto(bitmap.asImageBitmap(), bitmap.width, bitmap.height),
        )
    }

    private fun element(
        flippedHorizontally: Boolean = false,
        flippedVertically: Boolean = false,
    ) = ImageElement(
        id = "photo",
        transform = Transform(xPt = 75.0, yPt = 80.0, widthPt = 150.0, heightPt = 120.0),
        assetId = "golden-photo",
        flippedHorizontally = flippedHorizontally,
        flippedVertically = flippedVertically,
    )

    private fun show(
        darkTheme: Boolean,
        draft: FramingDraft,
        flippedHorizontally: Boolean = false,
        flippedVertically: Boolean = false,
    ) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                BenchSheetIsland {
                    paperArgb = ZinelyTheme.v21Colors.paper.toArgb()
                    inkArgb = ZinelyTheme.v21Colors.ink.toArgb()
                    deskEdgeArgb = ZinelyTheme.v21Colors.deskEdge.toArgb()
                    OverlayPage(
                        draft = draft,
                        element = element(flippedHorizontally, flippedVertically),
                        photo = photo(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun OverlayPage(draft: FramingDraft, element: ImageElement, photo: ReframePhoto) {
        Box(
            Modifier
                .size(width = 300.dp, height = 360.dp)
                .testTag(HOST_TAG)
                .background(ZinelyTheme.v21Colors.paper),
        ) {
            LoadedReframeOverlay(
                element = element,
                draft = draft,
                screenPxPerPt = SCREEN_PX_PER_PT,
                pageOffset = PtPoint(0.0, 0.0),
                photo = photo,
                onDraft = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun capture(
        name: String,
        darkTheme: Boolean,
        draft: FramingDraft,
        flippedHorizontally: Boolean = false,
        flippedVertically: Boolean = false,
        expectFrameGround: Boolean = false,
    ) {
        show(darkTheme, draft, flippedHorizontally, flippedVertically)
        composeRule.onNodeWithTag(ReframeOverlayTestTag).assertExists()
        val bounds = composeRule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot
        val bitmap = cropToBounds(composeRule.activity.window.decorView.rasterizeToBitmap(), bounds)

        assertTrue("paper ground did not paint ($name)", bitmap.pixelCountOf(paperArgb) > 1_000)
        assertTrue("dashed frame boundary did not paint ($name)", boundaryInkCount(bitmap) > 50)
        assertTrue(
            "asymmetric photo did not paint ($name)",
            PHOTO_COLOURS.sumOf(bitmap::pixelCountOf) > 2_000,
        )
        if (expectFrameGround) {
            assertTrue("Whole photo did not expose the frame ground ($name)", bitmap.pixelCountOf(deskEdgeArgb) > 500)
        }

        bitmap.captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    /**
     * Counts ink only in a narrow perimeter around the crop frame. A whole-image ink count is
     * vacuous in dark mode because the crop scrim can contribute the room's ink elsewhere.
     */
    private fun boundaryInkCount(bitmap: Bitmap): Int {
        val transform = element().transform
        val left = (transform.xPt * SCREEN_PX_PER_PT).toInt()
        val top = (transform.yPt * SCREEN_PX_PER_PT).toInt()
        val right = ((transform.xPt + transform.widthPt) * SCREEN_PX_PER_PT).toInt()
        val bottom = ((transform.yPt + transform.heightPt) * SCREEN_PX_PER_PT).toInt()
        val radius = 3

        var count = 0
        for (y in (top - radius)..(bottom + radius)) {
            for (x in (left - radius)..(right + radius)) {
                val nearHorizontal = y in (top - radius)..(top + radius) ||
                    y in (bottom - radius)..(bottom + radius)
                val nearVertical = x in (left - radius)..(left + radius) ||
                    x in (right - radius)..(right + radius)
                if ((nearHorizontal || nearVertical) && bitmap.getPixel(x, y) == inkArgb) count++
            }
        }
        return count
    }

    private fun centredFill() = FramingDraft(FrameFit.FILL, zoom = 1.0, panX = 0.0, panY = 0.0)

    private fun framedAndFlippedFill() =
        FramingDraft(FrameFit.FILL, zoom = 1.8, panX = 0.15, panY = -0.10)

    private fun whole() = FramingDraft(FrameFit.WHOLE, zoom = 1.0, panX = 0.0, panY = 0.0)

    @Test
    fun reframe_overlay_fill_light() =
        capture("reframe_overlay_fill_light", darkTheme = false, draft = centredFill())

    @Test
    fun reframe_overlay_fill_dark() =
        capture("reframe_overlay_fill_dark", darkTheme = true, draft = centredFill())

    @Test
    fun reframe_overlay_framed_and_flipped_light() =
        capture(
            "reframe_overlay_framed_and_flipped_light",
            darkTheme = false,
            draft = framedAndFlippedFill(),
            flippedHorizontally = true,
            flippedVertically = true,
        )

    @Test
    fun reframe_overlay_framed_and_flipped_dark() =
        capture(
            "reframe_overlay_framed_and_flipped_dark",
            darkTheme = true,
            draft = framedAndFlippedFill(),
            flippedHorizontally = true,
            flippedVertically = true,
        )

    @Test
    fun reframe_overlay_whole_light() =
        capture("reframe_overlay_whole_light", darkTheme = false, draft = whole(), expectFrameGround = true)

    @Test
    fun reframe_overlay_whole_dark() =
        capture("reframe_overlay_whole_dark", darkTheme = true, draft = whole(), expectFrameGround = true)
}
