package com.aritr.zinely.feature.editor

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.math.hypot

/**
 * Issue #134 / D-119 (`docs/design/V2-SPEC-DEFECTS.md`): at the largest text on
 * a small phone the hint wraps to five lines, and a pill's ends (radius = half the height) cut into its first
 * and last words. Every corner of every laid-out line must sit inside the shape's ink border. The save failure
 * is fenced too: it shares the shape and the inset the bound in [NoticeMaxRadius] is derived from.
 *
 * Measured in the notice's own (unrotated) frame: the Row's inner coordinates are inside its `-0.6°` layer, as
 * the text is, so the lean drops out; the shape is then built at the pill's real size.
 *
 * Bench A54 (#148, #149, #158): on the same 360 x 640dp phone at text size 2, the hint never covers the piece
 * it teaches about (it waits when it does not fit above it), and the save notice stacks its buttons under its
 * line so the whole of it fits on the screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 2f)
class EditorMoveResizeHintLargeTextTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun every_word_clears_the_curved_ends_at_text_size_2_on_a_360dp_phone() =
        assertWordsInside(EditorMoveResizeHintTestTag, MoveResizeHintText) {
            EditorMoveResizeHint(photo = false, onDismiss = {})
        }

    @Test
    fun every_word_of_the_photo_line_clears_the_curved_ends_at_text_size_2_on_a_360dp_phone() =
        assertWordsInside(EditorMoveResizeHintTestTag, PhotoMoveResizeHintText) {
            EditorMoveResizeHint(photo = true, onDismiss = {})
        }

    /**
     * The save failure shares [NoticeShape] and its 16 / 12dp inset. Until A54.4 its two buttons stood beside
     * the line and squeezed it into a 16-line column taller than this 640dp screen (#149), so this case ran on
     * a 1400dp-tall one. It now runs on the phone itself.
     */
    @Test
    fun every_word_of_the_save_failure_clears_the_curved_ends_at_text_size_2_on_a_360dp_phone() =
        assertWordsInside(EditorSaveFailureTestTag, SaveFailureOutOfSpaceText) {
            EditorSaveFailure(visible = true, onDismiss = {}, kind = SaveErrorKind.OutOfSpace, reduceMotion = true)
        }

    /** Bench A54.4 (#149): stacked, the line has the width, the buttons stand under it, and all of it is on screen. */
    @Test
    fun the_save_failure_stacks_its_buttons_under_the_line_and_fits_the_screen() {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.fillMaxSize()) {
                    EditorSaveFailure(visible = true, onDismiss = {}, kind = SaveErrorKind.OutOfSpace, reduceMotion = true)
                }
            }
        }
        composeRule.waitForIdle()
        val root = composeRule.onNodeWithTag(EditorSaveFailureTestTag).fetchSemanticsNode().boundsInRoot
        val line = composeRule.onNodeWithText(SaveFailureOutOfSpaceText, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val retry = composeRule.onNodeWithTag(SaveFailureRetryTag).fetchSemanticsNode().boundsInRoot
        val dismiss = composeRule.onNodeWithTag(SaveFailureDismissTag).fetchSemanticsNode().boundsInRoot
        val screenHeight = 640 * composeRule.density.density
        assertTrue("the notice ($root) must end on the screen", root.bottom <= screenHeight)
        // The -0.6° lean moves an edge by under 2dp; 4dp of slack keeps the checks about layout, not the lean.
        val slack = 4 * composeRule.density.density
        assertTrue("both buttons stand under the line: $line, $retry, $dismiss",
            retry.top >= line.bottom - slack && dismiss.top >= line.bottom - slack)
        assertTrue("Try now comes before Got it", retry.right <= dismiss.left + slack)
        assertTrue("the line has the notice's width (${line.width} of ${root.width})", line.width >= root.width * 0.8f)
        assertTrue("the notice is not taller than the screen's canvas", root.height <= 450 * composeRule.density.density)
    }

    /** Bench A54.1/A54.2 (#148): a centred piece leaves the 187dp hint no room above it, so the hint waits. */
    @Test
    fun the_hint_waits_rather_than_cover_a_centred_piece() {
        // The app's own centred text box: 70% x 16% of the page (`centeredTextBox`).
        showScreen(Transform(45.9, 166.3, 214.2, 63.4))
        turnAccessibilityOn()
        val hint = composeRule.onNodeWithTag(EditorMoveResizeHintTestTag)
        hint.assertIsNotDisplayed()
        // Not placed, so not in the platform tree: TalkBack cannot land on a hint nobody can see.
        assertNull(
            "a waiting hint must not be in the platform tree",
            composeView().accessibilityNodeProvider.createAccessibilityNodeInfo(hint.fetchSemanticsNode().id),
        )
    }

    /** The same rule for a photo-sized piece: 60% x 60% of the page, centred, as a new photo lands. */
    @Test
    fun the_hint_waits_rather_than_cover_a_centred_photo_sized_piece() {
        showScreen(Transform(61.2, 79.2, 183.6, 237.6))
        composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertIsNotDisplayed()
    }

    /** With room above the piece the hint is drawn, and it ends clear of the handle marks. */
    @Test
    fun with_room_above_the_piece_the_hint_shows_and_clears_the_handles() {
        val store = showScreen(Transform(100.0, 300.0, 100.0, 40.0))
        val hint = composeRule.onNodeWithTag(EditorMoveResizeHintTestTag).assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val canvas = composeRule.onNodeWithTag(EditorCanvasTestTag).fetchSemanticsNode().boundsInRoot
        val view = store.uiState.value.view
        val pieceTop = canvas.top + ((300.0 + view.pageOffset.y) * view.screenPxPerPt).toFloat()
        // The handle marks reach 10dp above the box (5.5dp offset + half a 9dp mark); the hint's bounds here
        // include its lean, so this is the drawn hint against the drawn marks.
        val marks = pieceTop - 10 * composeRule.density.density
        assertTrue("the hint ($hint) must end above the handle marks ($marks)", hint.bottom <= marks)
        assertTrue("the whole hint is inside the canvas ($canvas)", hint.top >= canvas.top && hint.bottom <= canvas.bottom)
    }

    private fun showScreen(piece: Transform): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        val store = EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
                ),
            ),
            CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, runner,
        )
        store.dispatch(Intent.PlaceText(piece, "hi")) // auto-selects
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(
                    store = store,
                    pageSizePt = PtSize(306.0, 396.0),
                    modifier = Modifier.fillMaxSize(),
                    moveResizeHintSeen = false,
                    onMoveResizeHintSeen = {},
                )
            }
        }
        composeRule.waitForIdle()
        return store
    }

    private fun turnAccessibilityOn() {
        val manager = composeRule.activity.getSystemService(AccessibilityManager::class.java)
        shadowOf(manager).setEnabled(true)
        shadowOf(manager).setEnabledAccessibilityServiceList(listOf(AccessibilityServiceInfo()))
        shadowOf(manager).setTouchExplorationEnabled(true)
    }

    private fun composeView(): android.view.View {
        val content = composeRule.activity.findViewById<ViewGroup>(android.R.id.content)
        return (content.getChildAt(0) as ViewGroup).getChildAt(0)
    }

    private fun assertWordsInside(tag: String, text: String, notice: @Composable () -> Unit) {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.fillMaxSize().padding(16.dp)) { notice() }
            }
        }
        val row = composeRule.onNodeWithTag(tag).fetchSemanticsNode().layoutInfo.coordinates
        val textNode = composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
        val layout = mutableListOf<TextLayoutResult>()
            .also { textNode.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(it) }
            .single()
        // Non-vacuity: this is the wrapped case the issue reports, not the short one.
        assertTrue("expected the wrapped notice, got ${layout.lineCount} lines", layout.lineCount >= 4)

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
