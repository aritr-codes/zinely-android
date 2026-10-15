package com.aritr.zinely.feature.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.hypot

/**
 * Issue #134 / D-119 (`docs/design/V2-SPEC-DEFECTS.md`): at the largest text on
 * a small phone the hint wraps to five lines, and a pill's ends (radius = half the height) cut into its first
 * and last words. Every corner of every laid-out line must sit inside the shape's ink border.
 *
 * Measured in the hint's own (unrotated) frame: the Row's inner coordinates are inside its `-0.6°` layer, as
 * the text is, so the lean drops out; the shape is then built at the pill's real size.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 2f)
class EditorMoveResizeHintLargeTextTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun every_word_clears_the_curved_ends_at_text_size_2_on_a_360dp_phone() = assertWordsInside(photo = false)

    @Test
    fun every_word_of_the_photo_line_clears_the_curved_ends_at_text_size_2_on_a_360dp_phone() =
        assertWordsInside(photo = true)

    private fun assertWordsInside(photo: Boolean) {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.fillMaxSize().padding(16.dp)) { EditorMoveResizeHint(photo = photo, onDismiss = {}) }
            }
        }
        val row = composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).fetchSemanticsNode().layoutInfo.coordinates
        val textNode = composeRule
            .onNodeWithText(if (photo) PhotoMoveResizeHintText else MoveResizeHintText, useUnmergedTree = true)
            .fetchSemanticsNode()
        val layout = mutableListOf<TextLayoutResult>()
            .also { textNode.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(it) }
            .single()
        // Non-vacuity: this is the wrapped case the issue reports, not the short one.
        assertTrue("expected the wrapped hint, got ${layout.lineCount} lines", layout.lineCount >= 4)

        val density = composeRule.density
        with(density) {
            val start = NoticePaddingStart.toPx()
            val top = NoticePaddingV.toPx()
            val pill = Size(
                row.size.width + start + NoticePaddingEnd.toPx(),
                row.size.height + 2 * top,
            )
            val outline = NoticeShape.createOutline(pill, LayoutDirection.Ltr, density)
            val rr = (outline as Outline.Rounded).roundRect
            // Text origin in the pill frame: its offset inside the Row's content box, plus the padding.
            val origin = row.localPositionOf(textNode.layoutInfo.coordinates, Offset.Zero) + Offset(start, top)
            val border = NoticeBorder.toPx()
            for (line in 0 until layout.lineCount) {
                val l = layout.getLineLeft(line)
                val r = layout.getLineRight(line)
                val t = layout.getLineTop(line)
                val b = layout.getLineBottom(line)
                for (corner in listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b))) {
                    val p = origin + corner
                    assertTrue(
                        "line $line corner $p falls outside the border of a ${pill.width}x${pill.height}px " +
                            "pill (radius ${rr.topLeftCornerRadius.x}px)",
                        rr.containsInset(p, border),
                    )
                }
            }
        }
    }

    /** Inside [this] uniform-radius rounded rect shrunk by [inset] (the border's width). */
    private fun RoundRect.containsInset(p: Offset, inset: Float): Boolean {
        val radius = topLeftCornerRadius.x
        // Distance from p to the rect's "core" (the rect shrunk by the radius) must not exceed radius - inset.
        val cx = p.x.coerceIn(left + radius, right - radius)
        val cy = p.y.coerceIn(top + radius, bottom - radius)
        return p.x >= left + inset && p.x <= right - inset && p.y >= top + inset && p.y <= bottom - inset &&
            hypot(p.x - cx, p.y - cy) <= radius - inset
    }
}
