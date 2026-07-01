package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The S5-step-2 Export · Print & fold screen (docs/design/SCREEN-INVENTORY.md#export--print--fold): two
 * jargon-free format choices, the "Actual size" correctness note, and a fold-help seam. Robolectric NATIVE
 * to match the sibling [PreviewScreenTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExportScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var pdf = 0
    private var png = 0
    private var fold = 0
    private var back = 0

    private fun setExport(working: ExportKind? = null, error: String? = null) {
        composeRule.setContent {
            MaterialTheme {
                ExportScreen(
                    onPrintPdf = { pdf++ },
                    onSavePng = { png++ },
                    onFoldHelp = { fold++ },
                    onBack = { back++ },
                    working = working,
                    errorMessage = error,
                )
            }
        }
    }

    @Test
    fun shows_both_format_choices_and_the_actual_size_note() {
        setExport()
        composeRule.onNodeWithTag(ExportPdfCardTestTag).assertExists()
        composeRule.onNodeWithTag(ExportPngCardTestTag).assertExists()
        composeRule.onNodeWithText("Print at home (PDF)").assertExists()
        composeRule.onNodeWithText("Save as image (PNG)").assertExists()
        composeRule.onNodeWithTag(ExportActualSizeNoteTestTag).assertExists()
    }

    @Test
    fun pdf_card_is_the_primary_export_action() {
        setExport()
        pdf = 0
        composeRule.onNodeWithTag(ExportPdfCardTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(1, pdf)
    }

    @Test
    fun png_card_saves_an_image() {
        setExport()
        png = 0
        composeRule.onNodeWithTag(ExportPngCardTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(1, png)
    }

    @Test
    fun fold_help_and_back_are_reachable() {
        setExport()
        fold = 0
        back = 0
        composeRule.onNodeWithContentDescription("Back to editing").performScrollTo().performClick()
        composeRule.onNodeWithText("How do I fold it?").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(1, fold)
        assertEquals(1, back)
    }

    @Test
    fun cards_disable_while_a_render_is_in_flight() {
        setExport(working = ExportKind.PDF)
        composeRule.onNodeWithTag(ExportPdfCardTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(ExportPngCardTestTag).assertIsNotEnabled()
    }
}
