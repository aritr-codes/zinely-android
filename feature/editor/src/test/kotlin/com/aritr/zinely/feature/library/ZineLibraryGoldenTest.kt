package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Parity rasters of the **whole Library screen**, in all four of its states, both themes — the artifact
 * B5's review gate lays beside `docs/design/mockups/v2-library.html` with its own four toggles
 * (**content · empty · loading · error**) driven in turn.
 *
 * **What a green run here proves, and what it does not.** Without `-Proborazzi.test.record` or
 * `-Proborazzi.test.verify` nothing is written and nothing is compared, so a plain run only proves the
 * screen composes. **A recorded golden is not evidence until it has passed `verify`** — the principle B3
 * paid for when a raster committed in B2 turned out to have been stale at HEAD. Every measurable claim
 * lives in [ZineLibraryScreenTest], which holds under a plain run.
 *
 * **The window is the frozen phone**, `392dp × 812dp` (`.phone`, `:48`), and the SDK is the module default
 * rather than [ZineLibraryScreenTest]'s API 28 — so B4's grain and B1's soft-light blend are actually
 * drawn here (**D-014**), as they are in [ZineEmptyGoldenTest].
 *
 * ### Eight rasters, because the screen is four screens
 *
 * B1–B4 each shipped rasters of a *part*. This is the first capture of the arrangement — the desk, the
 * chosen state, and the dock over all of them — which is exactly the thing no component-level golden
 * could show and the thing the frozen file's four toggles actually depict.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h812dp")
class ZineLibraryGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the frozen library content light`() = viewport("content_light", content(), dark = false)

    @Test
    fun `the frozen library content dark`() = viewport("content_dark", content(), dark = true)

    @Test
    fun `the frozen library empty light`() = viewport("empty_light", LibraryShelfState.Empty, false)

    @Test
    fun `the frozen library empty dark`() = viewport("empty_dark", LibraryShelfState.Empty, true)

    @Test
    fun `the frozen library loading light`() = viewport("loading_light", LibraryShelfState.Loading, false)

    @Test
    fun `the frozen library loading dark`() = viewport("loading_dark", LibraryShelfState.Loading, true)

    @Test
    fun `the frozen library error light`() = viewport("error_light", LibraryShelfState.Error, false)

    @Test
    fun `the frozen library error dark`() = viewport("error_dark", LibraryShelfState.Error, true)

    private fun viewport(name: String, state: LibraryShelfState, dark: Boolean) {
        composeRule.setContent { Screen(state, dark) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(VIEWPORT).captureRoboImage("$GOLDEN_DIR/v21_library_$name.png", aa())
    }

    @Composable
    private fun Screen(state: LibraryShelfState, dark: Boolean) {
        ZinelyTheme(darkTheme = dark) {
            ZineLibraryScreen(
                state = state,
                events = emptyFlow(),
                onOpenZine = {},
                onShareExport = {},
                onStartZine = {},
                onRenameZine = { _, _ -> },
                onDuplicateZine = {},
                onDeleteZine = {},
                onDeleteUndo = {},
                onDeleteCommit = {},
                onRetry = {},
                modifier = Modifier.fillMaxSize().testTag(VIEWPORT),
            )
        }
    }

    /**
     * The frozen file's own six zines, in its own order and with its own covers — the fixture B2 and B3
     * already capture against, re-projected into the screen's type so the three rasters compare.
     */
    private fun content(): LibraryShelfState.Content = LibraryShelfState.Content(
        ZineShelfGoldenFixture.FROZEN.map {
            LibraryZine(id = it.title, title = it.title, subtitle = it.subtitle, cover = it.recipe)
        },
    )

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val VIEWPORT = "library-viewport"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }
}
