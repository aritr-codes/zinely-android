package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.TextAlign
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.render.android.BundledFontResolver
import com.aritr.zinely.render.android.DocumentFontRegistry
import com.aritr.zinely.render.android.SharedTextLayout
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * **The draft's lines sit where the page's do** (Brief 02 acceptance criterion 3, ADR-126 decision 6).
 *
 * The page is laid out by [SharedTextLayout] at the font's own line pitch; the editing field is a Compose
 * text field. This holds the field's real [TextLayoutResult] (read off the composed [BenchEditingSurface],
 * not off a copy of its style) to the page's `StaticLayout`, line by line: every baseline, the first one
 * included, for every registered family, each of its four faces and every size on the type bar's ramp.
 *
 * Families come from the registry, so a third voice is held to this without touching the test. The lines
 * are broken by hand so that this measures line *placement* and nothing else; where two engines wrap a
 * long line is a separate, older limit that [BenchEditingSurface] records.
 *
 * **Tolerance.** Each engine rounds a line's ascent and descent to whole pixels at its own scale: the page
 * at `sizePt x 8` layout units, the field at `sizePt x screenPxPerPt` device pixels. So a line's pitch can
 * differ by up to one device pixel and the difference adds up down the box. [PX_PER_LINE] is that bound.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class EditingDraftLineParityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        /** Six lines: descenders, an empty line, and a line of tall accented capitals that is not the first. */
        const val LINES = "Quiet zine\nof small hours, typed\n\nÅsa, Émile, jumpy fjord\nand a last\nline"

        /** Tall accented capitals on the very first line: ink above the font's ascent. */
        const val TALL_FIRST = "ÅÉÔ\nsecond\nthird"

        /** Page-to-device scales: a small canvas, the golden host's, and a dense phone's. */
        val SCALES = listOf(2.0f, 3.37f, 4.6f)

        /** Allowed drift per line, in device pixels. See the class note. */
        const val PX_PER_LINE = 1.0f

        const val BOX_W_PT = 700.0
        const val BOX_H_PT = 500.0

        /** Holds the box at the largest scale: 700 x 4.6 = 3220 px wide, 500 x 4.6 = 2300 px tall, at 2 px per dp. */
        val HOST_W = 1700.dp
        val HOST_H = 1200.dp

        val FACES = listOf(false to false, true to false, false to true, true to true)
    }

    private var screen by mutableStateOf<(@Composable () -> Unit)?>(null)
    private var hosted = false

    private fun show(content: @Composable () -> Unit) {
        if (!hosted) {
            composeRule.setContent { key(screen) { screen?.invoke() } }
            hosted = true
        }
        screen = content
        composeRule.waitForIdle()
    }

    private fun element(text: String, style: TextStyle) = TextElement(
        id = "t1",
        // Wide and tall enough that no size on the ramp wraps or runs out of box.
        transform = Transform(0.0, 0.0, BOX_W_PT, BOX_H_PT),
        text = text,
        style = style,
    )

    /** The composed field's own layout, at [scale], under system font scale [fontScale]. */
    private fun draft(element: TextElement, scale: Float, fontScale: Float = 1f): TextLayoutResult {
        show {
            ZinelyTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    // Larger than the window on purpose: the field takes the box's own size, and a box
                    // squeezed to the screen would wrap the larger sizes and measure wrapping instead.
                    Box(Modifier.requiredSize(HOST_W, HOST_H)) {
                        BenchEditingSurface(
                            session = Interaction.EditingText("t1", 1L),
                            element = element,
                            commitText = { false },
                            screenPxPerPt = scale,
                            pageOffset = PtPoint(0.0, 0.0),
                        )
                    }
                }
            }
        }
        val results = mutableListOf<TextLayoutResult>()
        val node = composeRule.onNodeWithTag(EditTextSessionTestTag).fetchSemanticsNode()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)!!.action!!(results)
        return results.single()
    }

    /** The page's baselines for [element], in device pixels at [scale]. */
    private fun pageBaselines(element: TextElement, scale: Float): List<Float> {
        val layout = SharedTextLayout.build(
            element.text, element.style, element.transform.widthPt, BundledFontResolver(composeRule.activity.assets),
        )
        return (0 until layout.lineCount).map { layout.getLineBaseline(it) * scale / SharedTextLayout.LAYOUT_SCALE }
    }

    /** Largest |draft - page| baseline difference per line index, over [text] at [scale]. */
    private fun residuals(text: String, style: TextStyle, scale: Float): List<Float> {
        val el = element(text, style)
        val draft = draft(el, scale)
        val page = pageBaselines(el, scale)
        assertEquals("line count ($style @ $scale)", page.size, draft.lineCount)
        return page.indices.map { abs(draft.getLineBaseline(it) - page[it]) }
    }

    @Test
    fun `every baseline of the draft lands on the page's, for each face of each voice at each size`() {
        val families = DocumentFontRegistry.Bundled.families
        assertTrue("the registry holds Book as well as Plain", families.size >= 2)
        val report = StringBuilder()
        val failures = mutableListOf<String>()
        for (family in families) for ((bold, italic) in FACES) {
            var worstFirst = 0f
            var worstPerLine = 0f
            var worstLast = 0f
            var sumPerLine = 0f
            var samples = 0
            for (sizePt in TypeSizesPt) for (scale in SCALES) {
                val style = TextStyle(fontFamily = family.name, sizePt = sizePt, bold = bold, italic = italic)
                val r = residuals(LINES, style, scale)
                worstFirst = maxOf(worstFirst, r[0])
                worstLast = maxOf(worstLast, r.last())
                r.forEachIndexed { line, px ->
                    // Line n has n + 1 rounded line boxes above its baseline.
                    worstPerLine = maxOf(worstPerLine, px / (line + 1))
                    sumPerLine += px / (line + 1)
                    samples++
                    if (px > PX_PER_LINE * (line + 1)) {
                        failures += "${family.name} b=$bold i=$italic ${sizePt}pt @$scale line $line: $px px"
                    }
                }
            }
            report.appendLine(
                "PARITY ${family.name} bold=$bold italic=$italic: first baseline <= $worstFirst px, " +
                    "drift <= $worstPerLine px per line (mean ${sumPerLine / samples}), " +
                    "sixth baseline <= $worstLast px",
            )
        }
        println(report)
        assertTrue("draft and page baselines differ:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `alignment does not move a baseline`() {
        for (family in DocumentFontRegistry.Bundled.families) for (align in TextAlign.entries) {
            val style = TextStyle(fontFamily = family.name, sizePt = 24.0, align = align)
            residuals(LINES, style, 3.37f).forEachIndexed { line, px ->
                assertTrue("${family.name} $align line $line: $px px", px <= PX_PER_LINE * (line + 1))
            }
        }
    }

    @Test
    fun `tall capitals on the first line do not push the draft down`() {
        for (family in DocumentFontRegistry.Bundled.families) for ((bold, italic) in FACES) {
            val style = TextStyle(fontFamily = family.name, sizePt = 24.0, bold = bold, italic = italic)
            residuals(TALL_FIRST, style, 3.37f).forEachIndexed { line, px ->
                assertTrue("${family.name} b=$bold i=$italic line $line: $px px", px <= PX_PER_LINE * (line + 1))
            }
        }
    }

    @Test
    fun `the system font scale does not move the draft, because the page is in points`() {
        for (family in DocumentFontRegistry.Bundled.families) {
            val el = element(LINES, TextStyle(fontFamily = family.name, sizePt = 14.0))
            val normal = draft(el, 3.37f, fontScale = 1f)
            val large = draft(el, 3.37f, fontScale = 2f)
            for (line in 0 until normal.lineCount) {
                assertEquals(
                    "${family.name} line $line",
                    normal.getLineBaseline(line), large.getLineBaseline(line), 0.5f,
                )
            }
        }
    }
}
