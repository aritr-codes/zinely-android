package com.aritr.zinely.render.android

import com.aritr.zinely.core.model.TextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * G3 layout-determinism unit for [SharedTextLayout] (ADR-028 §7.4). Catches font-map/constant
 * regressions before they reach a pixel golden: line count and per-line break offsets must be
 * deterministic and stable, and overflow must exceed the box height (so the replayer's box clip — not
 * `maxLines` — bounds it). Runs under the module's default `graphicsMode=NATIVE` so wrapping uses real
 * Skia metrics rather than legacy placeholders.
 */
@RunWith(RobolectricTestRunner::class)
class SharedTextLayoutTest {

    private val style = TextStyle(sizePt = 12.0)
    // A long run of short words in a narrow box forces many line breaks by geometry, so lineCount > 1
    // holds across font-metric drift (Codex Recommended) rather than depending on a specific font.
    private val text = "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu xi"
    private val boxWidthPt = 36.0

    @Test
    fun wrapsLongTextIntoMultipleLines() {
        val layout = SharedTextLayout.build(text, style, boxWidthPt, FontResolver.Default)
        assertTrue("expected wrapping in a narrow box, got ${layout.lineCount} line(s)", layout.lineCount > 1)
    }

    @Test
    fun lineBreaksAreStableAcrossRepeatedBuilds() {
        val a = SharedTextLayout.build(text, style, boxWidthPt, FontResolver.Default)
        val b = SharedTextLayout.build(text, style, boxWidthPt, FontResolver.Default)

        assertEquals(a.lineCount, b.lineCount)
        val offsetsA = (0 until a.lineCount).map { a.getLineStart(it) to a.getLineEnd(it) }
        val offsetsB = (0 until b.lineCount).map { b.getLineStart(it) to b.getLineEnd(it) }
        assertEquals(offsetsA, offsetsB)
    }

    @Test
    fun overflowExceedsBoxHeightSoTheClipBoundsItNotMaxLines() {
        val boxHeightPt = 20.0 // deliberately shorter than the wrapped content
        val layout = SharedTextLayout.build(text, style, boxWidthPt, FontResolver.Default)

        // StaticLayout.height is in layout units (points × K); convert back to points.
        val contentHeightPt = layout.height.toDouble() / SharedTextLayout.LAYOUT_SCALE
        assertTrue(
            "content ($contentHeightPt pt) should overflow the box ($boxHeightPt pt) and be clipped, not truncated",
            contentHeightPt > boxHeightPt,
        )
    }
}
