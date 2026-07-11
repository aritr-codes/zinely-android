package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.LiveTransform
import com.aritr.zinely.core.editor.ViewState
import com.aritr.zinely.core.editor.toUiState
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyTheme
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
}
