package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.ViewState
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.feature.editor.EditorEffectRunner
import com.aritr.zinely.feature.editor.EditorStore
import com.aritr.zinely.feature.editor.ElementNodeTagPrefix
import com.aritr.zinely.feature.editor.ElementSemanticsLayer
import com.aritr.zinely.ui.a11y.platformNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Bench A52.5 / ADR-147 decision 8: what the **platform** says about a described photo and Art piece. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class DescribePlatformNodeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val photo = ImageElement(
        "p", Transform(20.0, 20.0, 80.0, 60.0), assetId = "a".repeat(64), description = "A moth on the porch wall",
    )
    private val art = DecorElement(
        "d", Transform(20.0, 120.0, 60.0, 40.0), supplyId = "tape.torn", ink = ColorRgba.BLACK, description = "holds the photo",
    )
    private val plain = ImageElement("q", Transform(20.0, 200.0, 80.0, 60.0), assetId = "b".repeat(64))

    private fun render(withHandler: Boolean) {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        val store = EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = List(8) { i -> Page(i, PageRole.INTERIOR, elements = if (i == 1) listOf(photo, art, plain) else emptyList()) },
                ),
                currentPageIndex = 1,
                view = ViewState(screenPxPerPt = 2f),
            ),
            CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, runner,
        )
        composeRule.setContent {
            val ui by store.uiState.collectAsState()
            ElementSemanticsLayer(
                uiState = ui,
                dispatch = store::dispatch,
                modifier = Modifier.fillMaxSize(),
                onDescribe = if (withHandler) ({ _ -> }) else null,
                pageSizePt = PtSize(200.0, 300.0),
            )
        }
        composeRule.waitForIdle()
    }

    private fun node(id: String) = composeRule.onNodeWithTag("$ElementNodeTagPrefix$id").platformNode(composeRule.activity)

    @Test
    fun a_described_photo_is_a_button_that_says_what_Read_says_and_offers_Describe() {
        render(withHandler = true)
        val n = node("p")
        assertEquals("Photo: A moth on the porch wall", n.contentDescription)
        assertEquals("android.widget.Button", n.className)
        assertTrue("a described photo must stay clickable (Select)", n.isClickable)
        assertTrue("Describe must reach the platform: ${n.actionLabels}", Copy.A11y.DESCRIBE in n.actionLabels)
    }

    @Test
    fun described_Art_says_its_name_and_the_words_and_an_undescribed_photo_just_Photo() {
        render(withHandler = true)
        assertEquals("Torn tape: holds the photo", node("d").contentDescription)
        assertTrue(Copy.A11y.DESCRIBE in node("d").actionLabels)
        assertEquals("Photo", node("q").contentDescription)
    }

    @Test
    fun a_host_with_no_sheet_advertises_no_Describe() {
        render(withHandler = false)
        assertTrue(Copy.A11y.DESCRIBE !in node("p").actionLabels)
    }
}
