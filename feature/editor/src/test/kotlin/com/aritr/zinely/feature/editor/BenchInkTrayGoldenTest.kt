package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.ink.InkHex
import com.aritr.zinely.core.editor.ink.InkLibrary
import com.aritr.zinely.core.editor.ink.MyInk
import com.aritr.zinely.core.editor.ink.PaletteDraft
import com.aritr.zinely.core.editor.ink.PaletteResult
import com.aritr.zinely.ui.components.ZSheetSurface
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
 * One golden per state of the frozen prototype `docs/design/mockups/v21-ink-mixer.html` (`setState`), so
 * the Compose tray and mixer can be read side by side with the HTML (INK task 10, ADR-138).
 *
 * The seeds are the prototype's own: `seed([...])` (kept inks, newest first, `used = n - i`), `seedPals()`
 * (three `addPal` calls, newest first, the last one open), the nine-palette list of `many` (and its
 * `openPal = pals[2].id`), the nine kept inks of `full`. Palettes are built through [InkLibrary.addPalette],
 * the same newest-first-and-open rule as the prototype's `addPal`. The prototype's `current` starts at
 * `2A251E`, so that is the piece's ink in every tray frame.
 *
 * The tray is the production [BenchInkPopover]; the mixer is the production [InkMixerBody] / [InkMixerFoot]
 * / [InkMixerName] on [ZSheetSurface], the surface [InkMixerSheet] hands to `ZSheet`: a `Dialog` window is
 * invisible to the decor-view capture harness (see [BenchArtSheetGoldenTest]), and it would drop a test's
 * font scale too. "Largest text" is the prototype's `big` toggle, i.e. [BenchInkLargeTextFontScale].
 *
 * Each frame asserts the structure that makes it the state it names before it captures, so a frame that
 * silently fell back to the empty tray is red here rather than blessed on CI. The fine paint is asserted
 * in [BenchC6GoldenTest] and [BenchDecorGoldenTest]; these images are for parity.
 *
 * ⚠ The PNGs are recorded on CI (`record-goldens.yml`) and reviewed, never locally.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class BenchInkTrayGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "benchInkTrayGoldenHost"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )

        /** The prototype's `let current='2A251E'`. */
        val CURRENT = Color(0xFF2A251E)

        /** The prototype's `let state={h:150,…}`: the hue a grey keeps when the mixer opens. */
        const val START_HUE = 150.0

        /** `seed(['505955','C0392B','2E6F95'])`. */
        val TRAY_INKS = listOf("505955", "C0392B", "2E6F95")

        /** `seedPals()`. */
        val SEED_PALS = listOf(
            "Soft pastels" to listOf("E27F89", "57B0A9", "B7AD93", "8A5A9B"),
            "Forest & earth" to listOf("3E5E3A", "7C8A3F", "8C8269", "2A251E"),
            "Warm vintage" to listOf("B0503F", "D19A3C", "E7B53C", "505955"),
        )

        /** The six palettes `many` adds after `seedPals()`. */
        val MANY_PALS = listOf(
            "Risograph" to listOf("FF48B0", "0078BF", "FFE800"),
            "Night bus" to listOf("2A251E", "6E86C9", "E7B53C"),
            "Seaside" to listOf("57B0A9", "DDE9EE", "E7B53C", "6E86C9"),
            "Autumn" to listOf("B0503F", "D19A3C", "5B5347"),
            "Mono" to listOf("2A251E", "5B5347", "8C8269", "B7AD93"),
            "Garden" to listOf("3E5E3A", "7C8A3F", "E7B53C", "E27F89"),
        )

        /** `full`'s nine kept inks. */
        val FULL_INKS = listOf("505955", "C0392B", "2E6F95", "E8A87C", "6B4E71", "1F7A5C", "D4B000", "8C1C13", "3D5A80")
    }

    // ---- seeds ----------------------------------------------------------------------------------------

    /** Prototype `seed(list)`: `used = list.length - i`, `clock = list.length`. */
    private fun seed(list: List<String>) = InkLibrary(
        myInks = list.mapIndexed { i, h -> MyInk(InkHex(h), (list.size - i).toLong()) },
        clock = list.size.toLong(),
    )

    /** Prototype `addPal`, through the real store rule (newest first, opens it). */
    private fun InkLibrary.add(pals: List<Pair<String, List<String>>>): InkLibrary =
        pals.fold(this) { lib, (name, inks) ->
            (lib.addPalette(name, inks.map(::InkHex)) as PaletteResult.Done).library
        }

    private fun trayThree() = seed(TRAY_INKS).add(SEED_PALS)

    private fun many(): InkLibrary {
        val lib = seed(TRAY_INKS).add(SEED_PALS).add(MANY_PALS)
        return lib.open(lib.palettes[2].id) // `openPal=pals[2].id`
    }

    // ---- hosts ----------------------------------------------------------------------------------------

    private fun host(darkTheme: Boolean, fontScale: Float?, content: @Composable () -> Unit) {
        composeRule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale ?: d.fontScale)) {
                ZinelyTheme(darkTheme = darkTheme) {
                    Box(
                        Modifier
                            .testTag(HOST_TAG)
                            .fillMaxWidth()
                            .background(ZinelyTheme.v21Colors.desk),
                    ) { content() }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun tray(
        library: InkLibrary,
        darkTheme: Boolean = false,
        fontScale: Float? = null,
        draft: PaletteDraft? = null,
    ) = host(darkTheme, fontScale) {
        var d by remember { mutableStateOf(draft) }
        BenchInkPopover(
            visible = true,
            kind = BenchVerbKind.TEXT,
            selected = CURRENT,
            onPick = {},
            onDone = {},
            inkLibrary = library,
            draft = d,
            onDraftChange = { d = it },
        )
    }

    private fun mixer(start: String, library: InkLibrary, darkTheme: Boolean = false, fontScale: Float? = null) =
        host(darkTheme, fontScale) {
            var state by remember {
                mutableStateOf(InkMixerState.open(InkHex(start), START_HUE))
            }
            ZSheetSurface(
                title = Copy.InkMix.TITLE,
                sub = null,
                modifier = Modifier.heightIn(max = LocalConfiguration.current.screenHeightDp.dp * InkMixerMaxHeightFraction),
                titleTrailing = { InkMixerName(state.hex) },
                foot = { InkMixerFoot(inkMixerUseLabel(null), onCancel = {}, onUse = {}) },
            ) {
                InkMixerBody(state, { state = it }, BenchVerbKind.TEXT, Copy.InkMix.PREVIEW_SAMPLE, library)
            }
        }

    private fun count(tag: String) =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun capture(name: String) {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HOST_TAG).captureRoboImage("$GOLDEN_DIR/$name.png", roborazziOptions = aa())
    }

    // ---- tray -----------------------------------------------------------------------------------------

    /** `tray0`: `seed([])`, no palettes. */
    @Test
    fun bench_ink_tray_first_time_light() {
        tray(seed(emptyList()))
        composeRule.onNodeWithTag(BenchInkPalettesEmptyTestTag, useUnmergedTree = true).assertExists()
        check(count(BenchInkKeptTestTag) == 0) { "first time keeps no inks" }
        capture("bench_ink_tray_first_time_light")
    }

    /** `tray3`: three kept inks, `seedPals()`. */
    @Test
    fun bench_ink_tray_palettes_light() {
        tray(trayThree())
        assertTrayThree()
        capture("bench_ink_tray_palettes_light")
    }

    @Test
    fun bench_ink_tray_palettes_dark() {
        tray(trayThree(), darkTheme = true)
        assertTrayThree()
        capture("bench_ink_tray_palettes_dark")
    }

    private fun assertTrayThree() {
        composeRule.onAllNodesWithTag(BenchInkStripTestTag, useUnmergedTree = true).assertCountEquals(3)
        composeRule.onAllNodesWithTag(BenchInkKeptTestTag, useUnmergedTree = true).assertCountEquals(3)
    }

    /** `many`: nine palettes, the third (`Autumn`) open. */
    @Test
    fun bench_ink_tray_nine_palettes_light() {
        val lib = many()
        check(lib.openPalette()?.name == "Autumn") { "many opens pals[2]" }
        tray(lib)
        composeRule.onAllNodesWithTag(BenchInkStripTestTag, useUnmergedTree = true).assertCountEquals(9)
        capture("bench_ink_tray_nine_palettes_light")
    }

    /** `newpal`: `startEdit(null)`, then inks `505955, B0503F, E7B53C` and the name `Night bus`. */
    @Test
    fun bench_ink_tray_edit_light() {
        val lib = trayThree()
        val draft = PaletteDraft.newDraft(lib, InkHex("2A251E"), Copy.BenchInk.DEFAULT_PALETTE_NAME)
            .copy(name = "Night bus", inks = listOf("505955", "B0503F", "E7B53C").map(::InkHex))
        tray(lib, draft = draft)
        composeRule.onNodeWithTag(BenchInkPaletteNameTestTag, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(BenchInkSavePaletteTestTag, useUnmergedTree = true).assertExists()
        capture("bench_ink_tray_edit_light")
    }

    /** `starters`: the disclosure clicked open, the tray scrolled to its end. */
    @Test
    fun bench_ink_tray_starters_open_light() {
        tray(trayThree())
        composeRule.onNodeWithTag(BenchInkStartersTestTag).performScrollTo().performClick()
        composeRule.waitForIdle()
        val starters = composeRule.onAllNodesWithTag(BenchInkStarterTestTag, useUnmergedTree = true)
        starters.assertCountEquals(3)
        starters[2].performScrollTo().assertIsDisplayed()
        capture("bench_ink_tray_starters_open_light")
    }

    /** `tray3` under the `big` toggle: strips and My inks three a row, built-ins five. */
    @Test
    fun bench_ink_tray_largest_text_light() {
        tray(trayThree(), fontScale = BenchInkLargeTextFontScale)
        assertTrayThree()
        capture("bench_ink_tray_largest_text_light")
    }

    // ---- mixer ----------------------------------------------------------------------------------------

    /** `mixer`: `seed([])`, `openMixer('505955')`. */
    @Test
    fun bench_ink_mixer_light() {
        mixer("505955", seed(emptyList()))
        assertMixer(pale = false, full = false)
        capture("bench_ink_mixer_light")
    }

    @Test
    fun bench_ink_mixer_dark() {
        mixer("505955", seed(emptyList()), darkTheme = true)
        assertMixer(pale = false, full = false)
        capture("bench_ink_mixer_dark")
    }

    /** `pale`: `openMixer('F2D7A6')`, under 3 : 1 on white. */
    @Test
    fun bench_ink_mixer_pale_light() {
        mixer("F2D7A6", seed(emptyList()))
        assertMixer(pale = true, full = false)
        capture("bench_ink_mixer_pale_light")
    }

    /** `full`: nine kept inks, `openMixer('7A3B69')`, so the full note names the oldest. */
    @Test
    fun bench_ink_mixer_full_light() {
        mixer("7A3B69", seed(FULL_INKS))
        assertMixer(pale = false, full = true)
        capture("bench_ink_mixer_full_light")
    }

    /** `mixer` under the `big` toggle: the 120dp square and the stacked foot. */
    @Test
    fun bench_ink_mixer_largest_text_light() {
        mixer("505955", seed(emptyList()), fontScale = BenchInkLargeTextFontScale)
        assertMixer(pale = false, full = false)
        capture("bench_ink_mixer_largest_text_light")
    }

    private fun assertMixer(pale: Boolean, full: Boolean) {
        composeRule.onNodeWithTag(InkMixerSquareTestTag, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(InkMixerUseTestTag, useUnmergedTree = true).assertExists()
        check(count(InkMixerLegibPaleTestTag) == (if (pale) 1 else 0)) { "pale note: expected $pale" }
        check(count(InkMixerFullNoteTestTag) == (if (full) 1 else 0)) { "full note: expected $full" }
    }
}
