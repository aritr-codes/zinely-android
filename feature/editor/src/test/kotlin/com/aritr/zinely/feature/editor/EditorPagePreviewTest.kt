package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.LiveTransform
import com.aritr.zinely.core.editor.ViewState
import com.aritr.zinely.core.editor.toUiState
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.pixelCountOf
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Robolectric NATIVE smoke proof of the [EditorPagePreview] host wiring — the live re-render path and the
 * pure pieces are proven by [LivePreviewTest] / [SelectionChromeGeometryTest]; this asserts the host
 * composes both layers (preview + chrome) for a selection, idle and mid-gesture, without crashing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorPagePreviewTest {

    @Test
    fun editorPreviewCacheBudgetIsBoundedForSmallAndLargeHeaps() {
        assertEquals(8L * 1024L * 1024L, editorImageCacheBudgetBytes(64L * 1024L * 1024L))
        assertEquals(16L * 1024L * 1024L, editorImageCacheBudgetBytes(256L * 1024L * 1024L))
        assertEquals(24L * 1024L * 1024L, editorImageCacheBudgetBytes(1024L * 1024L * 1024L))
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val pageSizePt = PtSize(100.0, 100.0)

    private fun model(interaction: Interaction = Interaction.Idle): EditorModel {
        val el = TextElement(id = "t1", transform = Transform(40.0, 40.0, 20.0, 20.0), text = "hi")
        return EditorModel(
            document = ZineDocument(
                format = ZineFormat.SINGLE_SHEET_8,
                paperSize = PaperSize.LETTER,
                pages = listOf(Page(index = 0, role = PageRole.INTERIOR, elements = listOf(el))),
            ),
            selection = setOf("t1"),
            view = ViewState(screenPxPerPt = 2f),
            interaction = interaction,
        )
    }

    @Test
    fun renders_preview_and_chrome_for_a_selection_when_idle() {
        composeRule.setContent {
            ZinelyTheme {
                EditorPagePreview(
                    uiState = model().toUiState(),
                    defaults = DocumentDefaults(),
                    pageSizePt = pageSizePt,
                    live = null,
                    modifier = Modifier.size(200.dp, 200.dp),
                )
            }
        }
        composeRule.onNodeWithTag(PagePreviewTestTag).fetchSemanticsNode()
        composeRule.onNodeWithTag(SnapGuidesTestTag).fetchSemanticsNode()
        composeRule.onNodeWithTag(SelectionChromeTestTag).fetchSemanticsNode()
    }

    @Test
    fun renders_mid_gesture_with_live_baked_transform() {
        val before = mapOf("t1" to Transform(40.0, 40.0, 20.0, 20.0))
        val transforming = Interaction.Transforming(pageIndex = 0, ids = setOf("t1"), before = before, token = 1L)
        val live = LiveTransform().accumulate(panXpx = 40.0, panYpx = 0.0, zoomFactor = 1.0, rotationDelta = 0.0)

        composeRule.setContent {
            ZinelyTheme {
                EditorPagePreview(
                    uiState = model(transforming).toUiState(),
                    defaults = DocumentDefaults(),
                    pageSizePt = pageSizePt,
                    live = live,
                    modifier = Modifier.size(200.dp, 200.dp),
                )
            }
        }
        // All three layers still present while a transform session is open + a live frame is applied.
        composeRule.onNodeWithTag(PagePreviewTestTag).fetchSemanticsNode()
        composeRule.onNodeWithTag(SnapGuidesTestTag).fetchSemanticsNode()
        composeRule.onNodeWithTag(SelectionChromeTestTag).fetchSemanticsNode()
    }

    /**
     * ADR-093 row 3.11 — the element under an open in-place session is suppressed **from the tape**.
     *
     * This has to be read off the raster, and the first cut of C3 learned why the hard way: a semantics
     * assertion cannot see it (the tape is one drawing node either way), and a *screen-level golden* cannot
     * see it either, because the in-place field draws the same words at the same rect — so with suppression
     * disabled the two draws land on top of each other and the picture barely changes. Mutating the filter
     * away therefore survived the entire suite.
     *
     * The guard that does bite: render the tape layer **alone**, with an ink no other pixel on the page can
     * produce, and count it. Present without suppression, absent with it. No overlap, no threshold, no
     * dependence on two text engines agreeing.
     */
    @Test
    fun the_tape_omits_the_element_under_an_open_session() {
        val ink = ColorRgba(255, 0, 255)
        val el = TextElement(
            id = "t1",
            transform = Transform(5.0, 20.0, 90.0, 60.0),
            text = "MMMM",
            style = com.aritr.zinely.core.model.TextStyle(sizePt = 36.0, color = ink),
        )
        val state = EditorModel(
            document = ZineDocument(
                format = ZineFormat.SINGLE_SHEET_8,
                paperSize = PaperSize.LETTER,
                pages = listOf(Page(index = 0, role = PageRole.INTERIOR, elements = listOf(el))),
            ),
            view = ViewState(screenPxPerPt = 2f),
        ).toUiState()

        // One `setContent` (the rule forbids two) toggled by state, so both readings come from the same
        // laid-out tree and differ in nothing but the suppression.
        var hidden by mutableStateOf<String?>(null)
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.testTag(TAPE_PROBE_TAG)) {
                    EditorPagePreview(
                        uiState = state,
                        defaults = DocumentDefaults(),
                        pageSizePt = pageSizePt,
                        live = null,
                        modifier = Modifier.size(200.dp, 200.dp),
                        hiddenElementId = hidden,
                    )
                }
            }
        }

        fun inkPixels(): Int {
            composeRule.waitForIdle()
            val bounds = composeRule.onNodeWithTag(TAPE_PROBE_TAG).fetchSemanticsNode().boundsInRoot
            val bmp = cropToBounds(composeRule.activity.window.decorView.rasterizeToBitmap(), bounds)
            return bmp.pixelCountOf(Color.Magenta.toArgb())
        }

        val drawn = inkPixels()
        assertTrue("the probe ink never painted — the test cannot detect suppression ($drawn px)", drawn > 20)
        hidden = "t1"
        assertEquals("the suppressed element was still drawn by the tape", 0, inkPixels())
    }

    private companion object {
        const val TAPE_PROBE_TAG = "tapeSuppressionProbe"
    }
}
