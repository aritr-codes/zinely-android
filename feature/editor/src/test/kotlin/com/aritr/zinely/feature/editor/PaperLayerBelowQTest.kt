package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.data.repository.PaperColour
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.render.SceneRenderer
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The paper layer must tint the page, not cover it, on Android 7 to 9 too. Compose's `BlendMode.Multiply` is not
 * supported below API 29 and falls back to source-over, which painted a solid paper over every page on the API 24
 * emulator (the user's work vanished). `sdk = 28` is below that floor, so this fails if the layer ever goes back to a
 * mode the platform cannot draw.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi", sdk = [28])
class PaperLayerBelowQTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun the_page_ink_shows_through_a_coloured_paper_below_api_29() {
        val size = PtSize(200.0, 300.0)
        val page = Page(
            index = 0,
            role = PageRole.FRONT_COVER,
            elements = listOf(
                TextElement(
                    id = "t",
                    transform = Transform(16.0, 40.0, 168.0, 60.0),
                    text = "Quiet zine",
                    style = TextStyle(fontFamily = "sans-serif", sizePt = 28.0),
                ),
            ),
        )
        composeRule.setContent {
            Box(Modifier.testTag("host").background(Color.White)) {
                PagePreview(
                    tape = SceneRenderer.render(page, size, DocumentDefaults()),
                    sheet = size,
                    screenPxPerPt = 2f,
                    modifier = Modifier.size(200.dp, 300.dp),
                    paper = PaperColour.PASTEL_YELLOW,
                )
            }
        }
        composeRule.waitForIdle()
        val bmp = cropToBounds(
            composeRule.activity.window.decorView.rasterizeToBitmap(),
            composeRule.onNodeWithTag("host").fetchSemanticsNode().boundsInRoot,
        )
        var ink = 0
        for (y in 0 until bmp.height) for (x in 0 until bmp.width) {
            val p = bmp.getPixel(x, y)
            if ((p shr 16 and 0xFF) + (p shr 8 and 0xFF) + (p and 0xFF) < 200) ink++
        }
        assertTrue("the paper covered the page's ink: $ink dark pixels", ink > 200)
    }
}
