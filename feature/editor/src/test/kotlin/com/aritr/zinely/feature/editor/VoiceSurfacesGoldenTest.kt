package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * **Document voices on the surfaces a maker sees (ADR-126): the Bench with its page strip, Read, and the
 * field they type into.** Plain and Book, each regular, bold, italic and bold italic.
 *
 * The page itself is `render-android`'s `VoiceGoldenTest` (screen and export raster). These are the
 * hosts: the Bench and the strip draw the page through [PagePreview], Read through its own leaf, and the
 * editing field through Compose with [editingFontFamily], which is the one surface that does not replay
 * the tape and so the one that could show a different face from the result.
 *
 * Behavioural half (runs under a plain unit run): Book is not Plain on any surface, and the field's four
 * faces are four different rasters, so a golden can never be recorded off a surface that ignored the font.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class VoiceSurfacesGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "voiceSurfacesGoldenHost"
        const val LINE = "Quiet zine"
        const val FOUR_LINES = "Quiet zine\nof small hours,\nfolded twice\nand left out"
        val PAGE = PtSize(200.0, 300.0)

        /** regular, bold, italic, bold italic: label to (bold, italic). */
        val FACES = listOf(
            "regular" to (false to false),
            "bold" to (true to false),
            "italic" to (false to true),
            "bolditalic" to (true to true),
        )
        val VOICES = listOf("plain" to "sans-serif", "book" to "Fraunces")

        fun aa() = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f))
    }

    /** One page holding the voice's four faces, one per line. */
    private fun fourFaces(family: String, index: Int = 0): Page = Page(
        index = index,
        role = if (index == 0) PageRole.FRONT_COVER else PageRole.INTERIOR,
        elements = FACES.mapIndexed { row, (label, face) ->
            TextElement(
                id = "t-$label",
                transform = Transform(16.0, 24.0 + row * 44.0, 168.0, 34.0),
                text = LINE,
                style = TextStyle(fontFamily = family, sizePt = 24.0, bold = face.first, italic = face.second),
            )
        },
    )

    private fun store(pages: List<Page>): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        return EditorStore(
            EditorModel(ZineDocument(format = ZineFormat.SINGLE_SHEET_8, paperSize = PaperSize.LETTER, pages = pages)),
            CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, runner,
        )
    }

    // The rule takes content once per test; each surface here is shown for two voices and compared, so
    // the content is swapped under a `key` and every show is a fresh composition.
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

    private fun hostBitmap(): Bitmap = cropToBounds(
        composeRule.activity.window.decorView.rasterizeToBitmap(),
        composeRule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot,
    )

    // ── Bench + page strip ────────────────────────────────────────────────────────────────────────

    private fun bench(family: String): Bitmap {
        val s = store(listOf(fourFaces(family)) + (1 until 8).map { Page(index = it, role = PageRole.INTERIOR) })
        show {
            ZinelyTheme {
                Box(Modifier.testTag(HOST_TAG).background(ZinelyTheme.v21Colors.desk)) {
                    EditorScreen(store = s, pageSizePt = PAGE, modifier = Modifier.size(360.dp, 720.dp))
                }
            }
        }
        return hostBitmap()
    }

    @Test
    fun voice_bench_and_strip() {
        val shots = VOICES.associate { (label, family) -> label to bench(family) }
        assertFalse("the Bench drew a Book page as a Plain one", shots.getValue("book").sameAs(shots.getValue("plain")))
        shots.forEach { (label, bmp) -> bmp.captureRoboImage("$GOLDEN_DIR/voice_bench_${label}_light.png", aa()) }
    }

    // ── Read ──────────────────────────────────────────────────────────────────────────────────────

    private fun read(family: String): Bitmap {
        val pages = (0 until 8).map { fourFaces(family, it) }
        show {
            ZinelyTheme {
                Box(Modifier.testTag(HOST_TAG).size(420.dp, 820.dp)) {
                    ProofScreen(onBack = {}, pages = pages, pageSizePt = PAGE, defaults = DocumentDefaults())
                }
            }
        }
        composeRule.onNodeWithTag(proofReadPageTag(1)).assertExists()
        return hostBitmap()
    }

    @Test
    fun voice_read() {
        val shots = VOICES.associate { (label, family) -> label to read(family) }
        assertFalse("Read drew a Book page as a Plain one", shots.getValue("book").sameAs(shots.getValue("plain")))
        shots.forEach { (label, bmp) -> bmp.captureRoboImage("$GOLDEN_DIR/voice_read_${label}_light.png", aa()) }
    }

    // ── The field a maker types into ──────────────────────────────────────────────────────────────

    /** The open editing field for one face, cropped to the field's own bounds. */
    private fun field(
        family: String,
        bold: Boolean,
        italic: Boolean,
        text: String = LINE,
        heightPt: Double = 40.0,
        sizePt: Double = 24.0,
    ): Bitmap {
        val element = TextElement(
            id = "t1",
            // Low on the page, as `bench_editing_state_light` places it and for its reason.
            transform = Transform(16.0, 200.0, 168.0, heightPt),
            text = text,
            style = TextStyle(fontFamily = family, sizePt = sizePt, bold = bold, italic = italic),
        )
        val s = store(listOf(Page(index = 0, role = PageRole.INTERIOR, elements = listOf(element))))
        show {
            ZinelyTheme {
                Box(Modifier.testTag(HOST_TAG).background(ZinelyTheme.v21Colors.desk)) {
                    EditorScreen(store = s, pageSizePt = PAGE, modifier = Modifier.size(360.dp, 720.dp))
                }
            }
        }
        s.dispatch(Intent.BeginEditText("t1"))
        composeRule.waitForIdle()
        val bounds = composeRule.onNodeWithTag(EditTextSessionTestTag).fetchSemanticsNode().boundsInRoot
        val crop = cropToBounds(composeRule.activity.window.decorView.rasterizeToBitmap(), bounds)
        val paper = crop.getPixel(0, 0)
        var ink = 0
        for (y in 0 until crop.height) for (x in 0 until crop.width) if (crop.getPixel(x, y) != paper) ink++
        assertTrue("the field drew no text ($family bold=$bold italic=$italic)", ink > 200)
        return crop
    }

    @Test
    fun voice_editing_field() {
        val shots = VOICES.associate { (voice, family) ->
            voice to FACES.associate { (label, face) -> label to field(family, face.first, face.second) }
        }
        shots.forEach { (voice, faces) ->
            val list = faces.entries.toList()
            for (i in list.indices) for (j in i + 1 until list.size) {
                assertFalse(
                    "$voice: the field drew ${list[i].key} and ${list[j].key} the same",
                    list[i].value.sameAs(list[j].value),
                )
            }
        }
        FACES.forEach { (label, _) ->
            assertFalse(
                "the field drew Book $label in Plain's face",
                shots.getValue("book").getValue(label).sameAs(shots.getValue("plain").getValue(label)),
            )
        }
        shots.forEach { (voice, faces) ->
            faces.forEach { (label, bmp) -> bmp.captureRoboImage("$GOLDEN_DIR/voice_editing_${voice}_$label.png", aa()) }
        }
    }

    /**
     * A draft of four lines in each voice. The one-line pictures above cannot show how far apart the
     * lines of a draft sit; these can. That the lines sit where the page's do is
     * `EditingDraftLineParityTest`'s to prove, not a picture's.
     */
    @Test
    fun voice_editing_multiline() {
        val shots = VOICES.associate { (voice, family) ->
            voice to field(family, bold = false, italic = false, text = FOUR_LINES, heightPt = 76.0, sizePt = 14.0)
        }
        assertFalse(
            "the field drew a Book draft as a Plain one",
            shots.getValue("book").sameAs(shots.getValue("plain")),
        )
        shots.forEach { (voice, bmp) -> bmp.captureRoboImage("$GOLDEN_DIR/voice_editing_multiline_$voice.png", aa()) }
    }
}
