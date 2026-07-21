package com.aritr.zinely.feature.editor

import androidx.compose.ui.geometry.Rect
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.render.computeImageBlit
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * **The Reframe parity proof (M7-01).** The overlay draws the photo itself ([photoDestPx], Compose
 * `drawImage`) while the page renders it through [computeImageBlit] — two independent implementations of
 * the same framing. INV-01 established they agree, but nothing asserted it; the agreement rested on a hand
 * derivation, which is exactly the kind of guarantee that rots silently.
 *
 * The assertion is deliberately end-to-end rather than formula-to-formula: for a given draft, compare
 * **what the user sees through the frame** with **where the committed element actually renders**. Working
 * in a frame anchored at the origin and sized to the box makes overlay device-px space and element-local
 * point space the same coordinate system, so the two rects are directly comparable.
 *
 * A failure here means `preview != export` for photos — Constitution Article 2. No Android; plain JVM.
 * Given-When-Then.
 */
class ReframeParityTest {

    private val tolerance = 1e-6

    private fun image(boxW: Double, boxH: Double, crop: Crop = Crop.FULL, fit: Fit = Fit.FILL) =
        ImageElement(
            id = "img",
            transform = Transform(0.0, 0.0, boxW, boxH),
            assetId = "a",
            crop = crop,
            fit = fit,
        )

    /**
     * Given a photo, a box, and a draft — when the overlay paints it and the commit is rendered — then the
     * region visible through the frame equals the region the renderer draws into.
     */
    private fun assertParity(
        case: String,
        intrinsicW: Int,
        intrinsicH: Int,
        boxW: Double,
        boxH: Double,
        draft: FramingDraft,
    ) {
        val pratio = intrinsicW.toDouble() / intrinsicH.toDouble()
        val bratio = boxW / boxH
        val frame = Rect(0f, 0f, boxW.toFloat(), boxH.toFloat())

        // What the overlay shows: the whole photo is drawn, then clipped by the frame it is framed in.
        val drawn = photoDestPx(draft, frame, pratio, bratio)
        val visibleLeft = max(drawn.left, frame.left).toDouble()
        val visibleTop = max(drawn.top, frame.top).toDouble()
        val visibleRight = min(drawn.right, frame.right).toDouble()
        val visibleBottom = min(drawn.bottom, frame.bottom).toDouble()

        // Where the committed element renders.
        val after = Framing.toImage(image(boxW, boxH), draft, pratio, bratio)
        val blit = computeImageBlit(
            intrinsicWidthPx = intrinsicW,
            intrinsicHeightPx = intrinsicH,
            crop = after.crop,
            fit = after.fit,
            boxWidthPt = boxW,
            boxHeightPt = boxH,
        )

        assertEquals("$case: dest left", blit.destRect.x, visibleLeft, tolerance)
        assertEquals("$case: dest top", blit.destRect.y, visibleTop, tolerance)
        assertEquals("$case: dest right", blit.destRect.right, visibleRight, tolerance)
        assertEquals("$case: dest bottom", blit.destRect.bottom, visibleBottom, tolerance)

        // Destination alone is not enough — and this is the trap. In FILL the drawn photo always covers
        // the frame and `computeImageBlit` always returns the full box, so a destination-only comparison
        // reduces to `frame == frame` and holds no matter which part of the photo is on show. **Which
        // pixels** the frame reveals is the half that depends on pan and zoom, so it is the half that
        // catches a real divergence. The overlay draws the whole image into `drawn`, so the visible
        // window mapped back into `drawn` is the source fraction it is displaying.
        val srcLeft = (visibleLeft - drawn.left) / drawn.width
        val srcTop = (visibleTop - drawn.top) / drawn.height
        val srcRight = (visibleRight - drawn.left) / drawn.width
        val srcBottom = (visibleBottom - drawn.top) / drawn.height

        assertEquals("$case: src left", blit.srcFraction.x, srcLeft, tolerance)
        assertEquals("$case: src top", blit.srcFraction.y, srcTop, tolerance)
        assertEquals("$case: src right", blit.srcFraction.right, srcRight, tolerance)
        assertEquals("$case: src bottom", blit.srcFraction.bottom, srcBottom, tolerance)
    }

    @Test
    fun `baseline fill - overlay and render frame the photo identically`() {
        assertParity("wide photo, square box", 2000, 1000, 100.0, 100.0, Framing.DEFAULT_FILL)
        assertParity("tall photo, square box", 1000, 2000, 100.0, 100.0, Framing.DEFAULT_FILL)
        assertParity("wide photo, wide box", 3000, 1000, 200.0, 100.0, Framing.DEFAULT_FILL)
        assertParity("photo matches box aspect", 1600, 1200, 160.0, 120.0, Framing.DEFAULT_FILL)
    }

    @Test
    fun `panned and zoomed fill - overlay and render frame the photo identically`() {
        val zoomed = Framing.zoomed(Framing.DEFAULT_FILL, 2.0)
        assertParity("zoomed, wide photo", 2000, 1000, 100.0, 100.0, zoomed)
        assertParity("zoomed, tall photo", 1000, 2000, 100.0, 100.0, zoomed)

        val panned = Framing.panned(Framing.zoomed(Framing.DEFAULT_FILL, 2.0), 0.1, -0.05, 2.0, 1.0)
        assertParity("panned + zoomed", 2000, 1000, 100.0, 100.0, panned)

        val clamped = Framing.panned(Framing.zoomed(Framing.DEFAULT_FILL, 1.5), 0.9, 0.9, 1.0, 2.0)
        assertParity("pan clamped at the edge", 1000, 2000, 100.0, 200.0, clamped)
    }

    @Test
    fun `whole photo - overlay and render letterbox identically`() {
        val whole = FramingDraft(FrameFit.WHOLE, zoom = 1.0, panX = 0.0, panY = 0.0)
        assertParity("wide photo letterboxes vertically", 2000, 1000, 100.0, 100.0, whole)
        assertParity("tall photo letterboxes horizontally", 1000, 2000, 100.0, 100.0, whole)
        assertParity("equal aspect fills exactly", 1200, 1200, 100.0, 100.0, whole)
        // Pan/zoom are ignored in WHOLE on both sides (bench parity) — the letterbox must not move.
        assertParity(
            "whole ignores pan and zoom",
            2000, 1000, 100.0, 100.0,
            FramingDraft(FrameFit.WHOLE, zoom = 3.0, panX = 0.3, panY = -0.2),
        )
    }

    @Test
    fun `the box-aspect crop never letterboxes - the premise the overlay relies on`() {
        // photoDestPx maps the resolved crop exactly onto the frame, which is only faithful because
        // resolveCrop always yields a box-aspect rect. If this ever stops holding, FILL silently gains a
        // letterbox in the output that the overlay does not draw — the INV-01 divergence, restated as maths.
        for ((iw, ih) in listOf(2000 to 1000, 1000 to 2000, 1600 to 1200, 900 to 900)) {
            val pratio = iw.toDouble() / ih
            val bratio = 100.0 / 100.0
            val crop = Framing.resolveCrop(Framing.zoomed(Framing.DEFAULT_FILL, 1.7), pratio, bratio)
            val croppedAspect = ((crop.right - crop.left) * iw) / ((crop.bottom - crop.top) * ih)
            assertEquals("cropped aspect must equal the box aspect for ${iw}x$ih", bratio, croppedAspect, 1e-9)
        }
    }
}
