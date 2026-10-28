package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Parity rasters of choosing several zines — `v21-library.html` A47 ([ADR-142](docs/DECISIONS.md#adr-142)): the
 * frozen page's *three selected* and *inside a folder* states, both themes, the chooser for a set, and the
 * *1.8× at 360dp* state taken at 2× (the harder case; the frozen page judges A47 at 1.8×).
 *
 * The Shelf, the bar and the dock are composed the way [ZineLibraryScreen] places them, over the frozen
 * seeded Shelf ([ZineShelfGoldenFixture.FROZEN]). Recorded by CI; a plain `testDebugUnitTest` neither writes
 * nor compares them. The claims that can be measured are asserted in [ZineLibrarySelectTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h812dp")
class ZineSelectGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val TAG = "select-viewport"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )

        private val ZINES = ZineShelfGoldenFixture.FROZEN

        private fun chosen(i: Int, on: Boolean) = ZINES[i].copy(choosing = true, chosen = on)
        private fun pile(name: String, vararg of: Int) = ZineShelfItem(
            name,
            ZINES[of[0]].recipe,
            pluralZineCount(of.size),
            pile = of.map { ZINES[it].recipe },
            choosing = true,
        )

        /** `a47c0`: no folders, three of six chosen. */
        val THREE = ZINES.indices.map { chosen(it, it in setOf(0, 2, 5)) }

        /** `a47c1`: My Shelf with its folders, which are quieter; one loose zine chosen. */
        val WITH_FOLDERS = listOf(pile("For the stall", 0, 4, 5), pile("Family", 1, 3), chosen(2, true))

        /** `a47c2`: inside *Family*, both chosen. */
        val FAMILY = listOf(chosen(1, true), chosen(3, true))
    }

    @Test fun `three selected light`() = capture("three_light", dark = false) { Choosing(THREE, count = 3) }

    @Test fun `three selected dark`() = capture("three_dark", dark = true) { Choosing(THREE, count = 3) }

    @Test fun `with folders light`() = capture("folders_light", dark = false) { Choosing(WITH_FOLDERS, count = 1) }

    @Test fun `with folders dark`() = capture("folders_dark", dark = true) { Choosing(WITH_FOLDERS, count = 1) }

    @Test fun `inside a folder light`() = capture("inside_light", dark = false) { Choosing(FAMILY, count = 2, folder = "Family") }

    @Test fun `inside a folder dark`() = capture("inside_dark", dark = true) { Choosing(FAMILY, count = 2, folder = "Family") }

    @Config(qualifiers = "w360dp-h740dp")
    @Test fun `three selected at 2x text on a 360dp phone`() =
        capture("narrow_large", dark = false, fontScale = 2f) { Choosing(THREE, count = 3) }

    @Config(qualifiers = "w360dp-h740dp")
    @Test fun `inside a folder at 2x text on a 360dp phone`() =
        capture("inside_narrow_large", dark = false, fontScale = 2f) { Choosing(FAMILY, count = 2, folder = "Family") }

    @Test fun `the chooser for a set light`() = capture("move_light", dark = false) { MoveMany() }

    @Test fun `the chooser for a set dark`() = capture("move_dark", dark = true) { MoveMany() }

    private fun capture(name: String, dark: Boolean, fontScale: Float = 1f, scene: @Composable BoxScope.() -> Unit) {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                ZinelyTheme(darkTheme = dark) {
                    Box(
                        Modifier
                            .testTag(TAG)
                            .fillMaxSize()
                            .background(ZinelyTheme.v21Colors.desk),
                        content = scene,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG).captureRoboImage("$GOLDEN_DIR/v21_select_$name.png", aa())
    }

    @Composable
    private fun BoxScope.Choosing(items: List<ZineShelfItem>, count: Int, folder: String? = null) {
        ZineShelf(
            items,
            onOpen = {},
            onActions = {},
            modifier = Modifier.fillMaxSize(),
            count = ZINES.size,
            folder = folder,
            choosing = true,
        )
        ShelfChoosingBar(count = count, onStop = {}, modifier = Modifier.align(Alignment.TopCenter))
        val several = pluralZineCount(count)
        ZineDock(
            onStart = {},
            modifier = Modifier.align(Alignment.BottomCenter),
            choosing = ZineDockChoosing(
                moveLabel = if (folder == null) "Move to a folder" else "Move somewhere else",
                moveName = "Move $several",
                deleteName = "Delete $several",
                onMove = {},
                onDelete = {},
            ),
        )
    }

    @Composable
    private fun BoxScope.MoveMany() {
        Choosing(THREE, count = 3)
        ZineActionScrim(onDismiss = {})
        Box(Modifier.align(Alignment.BottomCenter)) {
            FolderMoveSheetSurface(
                FolderMoveTarget("3 zines", null, mapOf("For the stall" to 3, "Family" to 2), paneTitle = "Move 3 zines"),
                onMove = {},
                onNewFolder = {},
            )
        }
    }
}
