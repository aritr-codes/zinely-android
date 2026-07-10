package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
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
 * Device-independent parity rasters of the frozen Shelf: every state, both themes, phone and tablet,
 * reduced motion and full.
 *
 * **What this proves and what it does not.** `captureRoboImage` is record-only in a plain
 * `testDebugUnitTest` run — without `-Proborazzi.test.verify` it writes the PNG and asserts nothing.
 * A green run here therefore proves the state composes and rasterizes without throwing at that width
 * and in that theme. The images are the artifact a human (or a `--verify` run against committed
 * goldens) compares to `docs/design/v1/shelf.html`. Pixel parity is a *review* gate, not a test
 * assertion, and this file exists to make that gate cheap rather than to pretend it is automated.
 *
 * The behavioural parity assertions — the ones that hold under a plain run — live in
 * [HomeScreenTest], [ShelfCoverTest], [ShelfCardTest], [ShelfStatesTest] and [ShelfSheetsTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val TAG = "shelfSurface"

        /**
         * The prototype's own viewports: `$("#device").style.setProperty("--w","430px")` and the
         * dock's tablet width.
         *
         * These are **Robolectric qualifiers, not a `Modifier.width`**. `BoxWithConstraints` reads the
         * window, so a width modifier cannot widen the surface past the emulated device — it only
         * makes a narrow raster that looks wide in the file name. The shelf's responsive breakpoints
         * (560/820/1180) are exactly what a parity raster must exercise, so the device has to move.
         */
        const val PHONE = "w430dp-h932dp-xhdpi"
        const val TABLET = "w920dp-h1280dp-xhdpi"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private val cards = List(8) { i ->
        HomeZineCard("z$i", SEED[i], "8-page mini · A4", "Edited today")
    }

    @Config(qualifiers = PHONE)
    @Test
    fun `few light phone`() = shelf("few_light_phone", cards.take(5), dark = false)

    @Config(qualifiers = PHONE)
    @Test
    fun `few dark phone`() = shelf("few_dark_phone", cards.take(5), dark = true)

    /** Eight objects earns the tools row. */
    @Config(qualifiers = TABLET)
    @Test
    fun `many light tablet`() = shelf("many_light_tablet", cards, dark = false)

    @Config(qualifiers = TABLET)
    @Test
    fun `many dark tablet`() = shelf("many_dark_tablet", cards, dark = true)

    @Config(qualifiers = PHONE)
    @Test
    fun `empty light phone`() = shelf("empty_light_phone", emptyList(), dark = false)

    @Config(qualifiers = PHONE)
    @Test
    fun `empty dark phone`() = shelf("empty_dark_phone", emptyList(), dark = true)

    @Config(qualifiers = PHONE)
    @Test
    fun `loading light phone`() =
        shelf("loading_light_phone", emptyList(), dark = false, loading = true)

    @Config(qualifiers = PHONE)
    @Test
    fun `error light phone`() =
        shelf("error_light_phone", emptyList(), dark = false, error = true, storeEmpty = false)

    @Config(qualifiers = PHONE)
    @Test
    fun `error dark phone`() =
        shelf("error_dark_phone", emptyList(), dark = true, error = true, storeEmpty = false)

    /**
     * Reduced motion is not "no motion": the frozen spec collapses durations to `.001ms`, so every
     * object still arrives — settled, folded, panelled. This raster is that end state, and it must be
     * indistinguishable from the full-motion shelf at rest.
     */
    @Config(qualifiers = PHONE)
    @Test
    fun `few light phone full motion settles to the same shelf`() =
        shelf("few_light_phone_motion", cards.take(5), dark = false, reduceMotion = false)

    private fun shelf(
        name: String,
        cards: List<HomeZineCard>,
        dark: Boolean,
        loading: Boolean = false,
        error: Boolean = false,
        storeEmpty: Boolean = cards.isEmpty(),
        reduceMotion: Boolean = true,
    ) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalZinelyMotion provides ZinelyMotion(reduceMotion)) {
                    Box(Modifier.fillMaxSize().testTag(TAG)) {
                        Shelf(cards, loading, storeEmpty, error)
                    }
                }
            }
        }
        if (!reduceMotion) {
            // Let the settle stagger and the fold score run out: the shelf at rest, not mid-arrival.
            composeRule.mainClock.advanceTimeBy(2_500)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG).captureRoboImage("$GOLDEN_DIR/shelf_$name.png", aa())
    }

    @Composable
    private fun Shelf(cards: List<HomeZineCard>, loading: Boolean, storeEmpty: Boolean, error: Boolean) {
        HomeScreen(
            loading = loading,
            storeEmpty = storeEmpty,
            cards = cards,
            events = emptyFlow(),
            onOpenZine = {},
            onStartZine = {},
            onRenameZine = { _, _ -> },
            onDuplicateZine = {},
            onDeleteZine = {},
            onDeleteUndo = {},
            onDeleteCommit = {},
            error = error,
            onRetry = {},
        )
    }
}

/** The frozen prototype's own seed titles, so a raster can be laid beside the HTML's. */
private val SEED = listOf(
    "Corner Store Poems",
    "Tuesday",
    "Small Machines",
    "How to Fold a River",
    "Notes on Rain",
    "Overgrown",
    "The Lint Collector",
    "Bus Window",
)
