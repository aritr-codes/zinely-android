package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * **CI-25** golden net for the assembled [EditorScreen], light + dark (roadmap §C1). A full-surface raster
 * in the [HomeScreenGoldenTest] / Proof style: `captureRoboImage` on the tagged surface node is record-only
 * (a no-op under a plain `testDebugUnitTest`), so a green plain run proves the screen composes and rasterises
 * at this size/theme, and `:feature:editor:recordRoborazziDebug` (`record-goldens.yml`) produces the PNG.
 *
 * The empty first-page editor is captured — the most stable, canonical assembly (invitation + supply tray +
 * page strip, no gesture-dependent selection chrome). A structural non-vacuity assertion (the paper surface
 * exists) runs under a plain unit run, proving the assembly mounted before any golden is recorded.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class EditorScreenGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val TAG = "editorScreenGoldenSurface"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(100.0, 100.0)

    private fun store(): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        return EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
                ),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
    }

    private fun capture(name: String, darkTheme: Boolean) {
        // Build the store once, outside composition, so a recomposition (e.g. the measured-canvas
        // SetViewport) does not recreate it.
        val editorStore = store()
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize().testTag(TAG)) {
                    EditorScreen(
                        store = editorStore,
                        pageSizePt = pageSizePt,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        // Structural non-vacuity (runs under a plain unit run): the assembled screen actually mounted.
        composeRule.onNodeWithTag(EditorPaperSurfaceTestTag).assertExists()
        composeRule.onNodeWithTag(TAG).captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Test
    fun editor_screen_light() = capture("editor_screen_light", darkTheme = false)

    @Test
    fun editor_screen_dark() = capture("editor_screen_dark", darkTheme = true)
}
