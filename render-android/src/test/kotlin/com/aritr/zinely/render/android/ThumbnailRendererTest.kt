package com.aritr.zinely.render.android

import android.graphics.Color
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.FillRect
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * S6.4 shelf-thumbnail provider (ADR-045): the same [CanvasReplayer] tape replayed at a
 * thumbnail scale instead of the 300 DPI export scale. Runs under the module-default
 * `graphicsMode=NATIVE` so fills are real Skia pixels. Image-element content is excluded here
 * (BitmapRegionDecoder is blank under NATIVE — the instrumented lane owns it, like G6a).
 */
@RunWith(RobolectricTestRunner::class)
class ThumbnailRendererTest {

    private val renderer = ThumbnailRenderer(CanvasReplayer())

    @Test
    fun givenPortraitPage_whenRendered_thenLongestEdgeIsHeightAtRequestedPixels() {
        // Given a portrait page half as wide as tall
        val pageSizePt = PtSize(100.0, 200.0)

        // When rendered at a 320 px longest edge
        val bitmap = renderer.render(emptyList(), pageSizePt, longestEdgePx = 320)

        // Then height is exactly the longest edge and width keeps the aspect
        assertEquals(320, bitmap.height)
        assertEquals(160, bitmap.width)
    }

    @Test
    fun givenLandscapePage_whenRendered_thenLongestEdgeIsWidth() {
        val bitmap = renderer.render(emptyList(), PtSize(200.0, 100.0), longestEdgePx = 320)

        assertEquals(320, bitmap.width)
        assertEquals(160, bitmap.height)
    }

    @Test
    fun givenEmptyTape_whenRendered_thenPageIsPaperWhite() {
        // Given an empty page (ARGB is transparent by default, which reads as a broken card)
        val bitmap = renderer.render(emptyList(), PtSize(100.0, 200.0), longestEdgePx = 64)

        // Then the thumbnail is paper white, not transparent
        assertEquals(Color.WHITE, bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
    }

    @Test
    fun givenFillCommand_whenRendered_thenFillLandsScaled() {
        // Given a page fully covered by a red fill
        val pageSizePt = PtSize(72.0, 144.0)
        val tape = listOf(
            FillRect(rect = PtRect(0.0, 0.0, 72.0, 144.0), color = ColorRgba(255, 0, 0, 255)),
        )

        // When rendered small
        val bitmap = renderer.render(tape, pageSizePt, longestEdgePx = 128)

        // Then the shared replayer drew it through the thumbnail-scale CTM
        assertEquals(Color.RED, bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
    }

    @Test
    fun givenExtremeAspect_whenRendered_thenShortEdgeNeverCollapsesToZero() {
        val bitmap = renderer.render(emptyList(), PtSize(1.0, 10_000.0), longestEdgePx = 64)

        assertEquals(64, bitmap.height)
        assertEquals(1, bitmap.width)
    }
}
