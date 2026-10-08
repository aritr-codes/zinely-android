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
 * Parity rasters of Shelf folders — `v21-library.html` A28 ([ADR-125](docs/DECISIONS.md#adr-125)): the
 * Shelf with its piles, the inside of a folder, and the three sheets, in both themes, with the Shelf and
 * every surface again at 1.8× text. The snack, which belongs to the screen and sits against its dock, is in
 * [ZineLibraryGoldenTest].
 *
 * The Shelf is the frozen page's own seeded one (`seed(true)`): [ZineShelfGoldenFixture.FROZEN]'s six
 * zines, three in *For the stall*, two in *Family*, one loose. The sheets are composed over it the way
 * [ZineActionSheetGoldenTest] composes its sheet, and for the same reason: the production sheets live in a
 * window of their own that a raster of this one cannot see. What these do not show is the slide and the
 * dock (the screen's, not the Shelf's).
 *
 * As with every raster here, a plain `testDebugUnitTest` neither writes nor compares these. The claims that
 * can be measured are asserted in [ZineLibraryFoldersTest] and [ShelfFoldersTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h812dp")
class ZineFoldersGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val TAG = "folders-viewport"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )

        private val ZINES = ZineShelfGoldenFixture.FROZEN
        private fun pile(name: String, vararg of: Int) =
            ZineShelfItem(name, ZINES[of[0]].recipe, pluralZineCount(of.size), pile = of.map { ZINES[it].recipe })

        /** My Shelf: a pile stands where its newest zine would (A28.4). */
        val SHELF = listOf(pile("For the stall", 0, 4, 5), pile("Family", 1, 3), ZINES[2])

        /** Inside *Family*. */
        val FAMILY = listOf(ZINES[1], ZINES[3])

        val FOLDERS = mapOf("For the stall" to 3, "Family" to 2)
        val KEYS = mapOf("for the stall" to "For the stall", "family" to "Family")
    }

    @Test fun `the Shelf with folders light`() = capture("shelf_light", dark = false) { Shelf() }

    @Test fun `the Shelf with folders dark`() = capture("shelf_dark", dark = true) { Shelf() }

    @Test fun `the Shelf with folders at large text`() = capture("shelf_large", dark = false, fontScale = 1.8f) { Shelf() }

    @Test fun `inside a folder light`() = capture("inside_light", dark = false) { Inside() }

    @Test fun `inside a folder dark`() = capture("inside_dark", dark = true) { Inside() }

    @Test fun `inside a folder at large text`() = capture("inside_large", dark = false, fontScale = 1.8f) { Inside() }

    @Test fun `the Move sheet at large text`() = capture("move_large", dark = false, fontScale = 1.8f) { Move() }

    @Test fun `the folder sheet at large text`() = capture("actions_large", dark = false, fontScale = 1.8f) { Actions() }

    /** What a maker making a first folder sees: nothing typed, and a button that cannot be pressed yet. */
    @Test fun `the name sheet before anything is typed`() = capture("name_empty", dark = false) { Name(typed = "") }

    @Test fun `the Move sheet light`() = capture("move_light", dark = false) { Move() }

    @Test fun `the Move sheet dark`() = capture("move_dark", dark = true) { Move() }

    @Test fun `the name sheet light`() = capture("name_light", dark = false) { Name() }

    @Test fun `the name sheet dark`() = capture("name_dark", dark = true) { Name() }

    @Test fun `the name sheet at large text`() = capture("name_large", dark = false, fontScale = 1.8f) { Name() }

    @Test fun `the folder sheet light`() = capture("actions_light", dark = false) { Actions() }

    @Test fun `the folder sheet dark`() = capture("actions_dark", dark = true) { Actions() }

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
        composeRule.onNodeWithTag(TAG).captureRoboImage("$GOLDEN_DIR/v21_folders_$name.png", aa())
    }

    @Composable
    private fun Shelf() {
        ZineShelf(SHELF, onOpen = {}, onActions = {}, modifier = Modifier.fillMaxSize(), count = ZINES.size)
    }

    @Composable
    private fun Inside() {
        ZineShelf(FAMILY, onOpen = {}, onActions = {}, modifier = Modifier.fillMaxSize(), folder = "Family")
    }

    @Composable
    private fun BoxScope.Sheet(sheet: @Composable () -> Unit) {
        Shelf()
        ZineActionScrim(onDismiss = {})
        Box(Modifier.align(Alignment.BottomCenter)) { sheet() }
    }

    /** A zine that is in a folder: *My Shelf* is offered, and its own folder is listed but is not a choice. */
    @Composable
    private fun BoxScope.Move() = Sheet {
        FolderMoveSheetSurface(FolderMoveTarget("Letters home", "Family", FOLDERS), onMove = {}, onNewFolder = {})
    }

    /** A name that exists, so the line under the field and the *Move to* button are both in the picture. */
    @Composable
    private fun BoxScope.Name(typed: String = "family") = Sheet {
        FolderNameSheetSurface(
            drawn = FolderNameTarget(zineTitle = "Riso tests"),
            keys = KEYS,
            check = ::plainFolderNameVerdict,
            onGo = {},
            onCancel = {},
            initial = typed,
        )
    }

    @Composable
    private fun BoxScope.Actions() = Sheet {
        FolderActionsSheetSurface(FolderActionsTarget("Family", 2), onOpen = {}, onRename = {}, onUnpack = {})
    }
}
