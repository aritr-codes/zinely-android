package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineFormat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The S5-step-1 reader's booklet Preview (docs/design/SCREEN-INVENTORY.md#preview). It pages through the
 * document in **reading order** (the reader's booklet, not the imposition sheet) and leads to Print & fold.
 * Robolectric NATIVE — same tier as [EditorPageStripTest], because the booklet mini-renders real page tapes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PreviewScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val pageSizePt = PtSize(100.0, 130.0)

    private fun eightPages(): List<Page> = (0 until ZineFormat.SINGLE_SHEET_8.pageCount).map { i ->
        val elements = if (i == 0) {
            listOf(TextElement(id = "t1", transform = Transform(10.0, 10.0, 40.0, 20.0), text = "hi"))
        } else {
            emptyList()
        }
        Page(index = i, role = PageRole.INTERIOR, elements = elements)
    }

    private var backCount = 0
    private var printCount = 0

    private fun setPreview(pages: List<Page> = eightPages()) {
        composeRule.setContent {
            MaterialTheme {
                PreviewScreen(
                    pages = pages,
                    pageSizePt = pageSizePt,
                    defaults = com.aritr.zinely.core.model.DocumentDefaults(),
                    onBack = { backCount++ },
                    onPrintAndFold = { printCount++ },
                )
            }
        }
    }

    @Test
    fun starts_on_page_one_with_prev_disabled() {
        setPreview()
        composeRule.onNodeWithTag(PreviewPageLabelTestTag).assertExists()
        composeRule.onNodeWithText("page 1 of 8").assertExists()
        composeRule.onNodeWithContentDescription("Previous page").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Next page").assertIsEnabled()
    }

    @Test
    fun next_advances_the_reading_order() {
        setPreview()
        composeRule.onNodeWithContentDescription("Next page").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("page 2 of 8").assertExists()
    }

    @Test
    fun last_page_disables_next() {
        setPreview()
        repeat(ZineFormat.SINGLE_SHEET_8.pageCount - 1) {
            composeRule.onNodeWithContentDescription("Next page").performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithText("page 8 of 8").assertExists()
        composeRule.onNodeWithContentDescription("Next page").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Previous page").assertIsEnabled()
    }

    @Test
    fun print_and_fold_is_the_primary_action() {
        setPreview()
        printCount = 0
        composeRule.onNodeWithText("Print & fold").performClick()
        composeRule.waitForIdle()
        assertEquals(1, printCount)
    }

    @Test
    fun back_returns_to_editing() {
        setPreview()
        backCount = 0
        composeRule.onNodeWithContentDescription("Back to editing").performClick()
        composeRule.waitForIdle()
        assertEquals(1, backCount)
    }
}
