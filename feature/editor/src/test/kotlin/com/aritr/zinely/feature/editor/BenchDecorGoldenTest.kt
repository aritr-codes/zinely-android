package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.pixelCountOf
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.zinelyContentInks
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import kotlin.math.PI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The golden net for **decor** — the two surfaces
 * [D-090](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-090) found unobserved.
 *
 * ### Why this file exists, which is not "a PNG was missing"
 *
 * The decor-ink package turned a control on the decor verb row from dim-and-inert to **live** — a change
 * in alpha, fill and ripple — and `verifyRoborazziDebug --rerun-tasks` passed with no diff at all. That
 * green was about to be reported as evidence the change was visually safe. It was not evidence of that:
 * `feature/editor/src/test/roborazzi/` held `editor_context_bar_*` (the **V1** bar) and `bench_ink_popover_*`
 * (the **text** bands), and nothing anywhere constructed a `DecorElement`. A green run over an unobserved
 * surface is the *absence of an observation*, and from the build output the two are indistinguishable.
 * CLAUDE.md's *"a golden that is never verified is a screenshot, not a test"*, one step earlier: **a golden
 * that was never recorded cannot even be a screenshot.**
 *
 * ### The raster is the smaller half
 *
 * Roborazzi compares at `changeThreshold = 0.02f`, so both regressions this file is built to catch would
 * survive it — a verb row fading to `disabledAlpha` and a band quietly dropping out are each well under
 * 2 % of their frame. Each is therefore *counted*, and the image keeps its real job: catching what nobody
 * thought to assert. This is the same division [BenchC6GoldenTest] makes, and for the same reason.
 *
 * The popover assertion here is deliberately the **exact inverse** of that file's
 * `no_paper_tint_is_painted_anywhere_in_a_text_elements_popover`. `benchInkBands` fences `Paper tints` out
 * of a text target and hands them to a decor one; a fence asserted in only one direction is satisfied by a
 * function that draws nothing for anybody. The pair pins it from both sides.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class BenchDecorGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "benchDecorGoldenHost"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private val inks = zinelyContentInks()

    private var inkSoftArgb = 0
    private var jamTextArgb = 0

    private fun px(dp: Float) = with(composeRule.density) { dp.dp.toPx() }

    private fun full() = composeRule.activity.window.decorView.rasterizeToBitmap()

    private fun crop(tag: String, full: Bitmap): Bitmap = cropToBounds(
        full,
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot,
    )

    private fun host(darkTheme: Boolean, content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                inkSoftArgb = ZinelyTheme.v21Colors.inkSoft.toArgb()
                jamTextArgb = ZinelyTheme.v21Colors.jamText.toArgb()
                Box(
                    Modifier
                        .testTag(HOST_TAG)
                        .fillMaxWidth()
                        .background(ZinelyTheme.v21Colors.desk),
                ) { content() }
            }
        }
        composeRule.waitForIdle()
    }

    private fun decorBar(darkTheme: Boolean) = host(darkTheme) {
        BenchContextBar(
            visible = true,
            verbs = benchContextVerbs(BenchVerbKind.DECOR),
            onVerb = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }

    private fun decorPopover(darkTheme: Boolean) = host(darkTheme) {
        BenchInkPopover(
            visible = true,
            // The whole point of the capture: `PHOTO, DECOR ->` is the arm that had never rendered in a
            // golden on any host. Passing `BenchVerbKind.DECOR` rather than the band list directly keeps
            // the routing under test too — a regression that re-hardcoded TEXT here would go red.
            bands = benchInkBands(inks, BenchVerbKind.DECOR),
            presets = benchInkPresets(inks),
            selected = null,
            inkCount = 2,
            onPick = {},
            onPreset = {},
            onDone = {},
        )
    }

    /**
     * [label]'s own verb node paints [argb] **unblended**.
     *
     * A disabled verb is drawn by `graphicsLayer { alpha = ZinelyV21Dimens.disabledAlpha }` on the whole
     * control, so its tint is composited against the card's paper before it lands and **no pixel equals
     * [argb] exactly**. Counting the unblended colour inside the verb's own bounds is therefore a direct
     * read of that one verb's enablement, at a magnitude the 2 % raster threshold cannot see — which is
     * precisely the change that shipped through a green golden run and produced D-090.
     */
    private fun assertVerbAtFullStrength(bar: Bitmap, label: String, argb: Int) {
        val verb = cropToBounds(
            bar,
            composeRule.onNodeWithTag("$BenchContextBarTestTag-$label", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot,
        )
        assertTrue(
            "`$label` paints no unblended tint inside its own bounds — it is being drawn at " +
                "disabledAlpha, which is the D-090 regression this file exists for",
            verb.pixelCountOf(argb) > 50,
        )
    }

    // =================================================================================================

    /**
     * The frozen decor row — `Replace · Ink · Delete` (`v21-bench.html:71`) — drawn at **full strength**.
     *
     * [assertVerbAtFullStrength] carries the measurement and explains it; this test's job is the set.
     */
    @Test
    fun the_decor_verb_row_draws_its_three_frozen_verbs_and_none_of_them_is_dim() {
        decorBar(darkTheme = false)

        // ⚠ **Asserted per verb, not over the card.** The first cut counted `inkSoft` across the whole
        // pill and called the test "three frozen verbs". Independent review showed the name outran the
        // assertion twice over: the count it made was of the CARD node (one), and a DECOR list that had
        // lost `Replace` entirely would still pass, because `Ink` alone clears the floor. Cropping each
        // verb to its own node makes the claim and the measurement the same claim — and it closes the
        // narrower gap the mutation table could not reach, where only ONE of the two live verbs regresses
        // to `disabledAlpha` and the other's pixels cover for it.
        val bar = full()
        assertEquals(
            "the frozen decor set is Replace · Ink · Delete",
            3,
            benchContextVerbs(BenchVerbKind.DECOR).size,
        )
        assertVerbAtFullStrength(bar, Copy.BenchVerbs.REPLACE, inkSoftArgb)
        assertVerbAtFullStrength(bar, Copy.BenchVerbs.INK, inkSoftArgb)
        // `.ctx button.danger{color:var(--jam-text)}` — jam is the one urgent colour in V2.1 and Delete
        // is the one verb entitled to it, so it is measured on its own channel.
        assertVerbAtFullStrength(bar, Copy.BenchVerbs.DELETE, jamTextArgb)

        composeRule.onNodeWithTag(HOST_TAG)
            .captureRoboImage("$GOLDEN_DIR/bench_context_bar_decor_light.png", roborazziOptions = aa())
    }

    /** The dark frame of the same row. Both tints flip with the theme, so both counts are re-taken. */
    @Test
    fun the_decor_verb_row_survives_the_dark_room() {
        decorBar(darkTheme = true)
        val bar = full()
        assertVerbAtFullStrength(bar, Copy.BenchVerbs.REPLACE, inkSoftArgb)
        assertVerbAtFullStrength(bar, Copy.BenchVerbs.INK, inkSoftArgb)
        assertVerbAtFullStrength(bar, Copy.BenchVerbs.DELETE, jamTextArgb)

        composeRule.onNodeWithTag(HOST_TAG)
            .captureRoboImage("$GOLDEN_DIR/bench_context_bar_decor_dark.png", roborazziOptions = aa())
    }

    /**
     * **The higher-value capture** (D-090's ⚠): a supply's popover, carrying the band a text element's
     * never shows.
     *
     * The assertion is the mirror of [BenchC6GoldenTest]'s fence. There, every paper tint had to paint
     * *less* than one preset dot could account for; here each must paint at least **half a pot**, which a
     * 13dp recipe dot cannot reach and only the 30dp swatch can. Asserted per tint rather than in
     * aggregate, so a band that renders with one swatch missing is red.
     */
    @Test
    fun a_supplys_popover_paints_the_paper_tints_a_text_elements_fences_out() {
        decorPopover(darkTheme = false)
        val card = crop(BenchInkPopoverTestTag, full())

        // A 30dp pot; the recipe dots that confused the text-side fence are 13dp, i.e. under a fifth of
        // this area, so the floor cannot be met by a preset.
        val pot = PI * px(15f) * px(15f)
        inks.paperTints.forEach { tint ->
            val n = card.pixelCountOf(tint.value.toArgb())
            assertTrue(
                "${benchInkName(tint.id)} paints ${n}px — under the ${(pot / 2).toInt()}px half a pot " +
                    "needs, so the fenced band is NOT reaching a supply",
                n.toDouble() > pot / 2,
            )
        }

        composeRule.onNodeWithTag(HOST_TAG)
            .captureRoboImage("$GOLDEN_DIR/bench_ink_popover_decor_light.png", roborazziOptions = aa())
    }

    /** The dark frame of the decor bands. */
    @Test
    fun the_supplys_popover_dims_with_the_room_and_keeps_its_tints() {
        decorPopover(darkTheme = true)
        val card = crop(BenchInkPopoverTestTag, full())
        val pot = PI * px(15f) * px(15f)
        inks.paperTints.forEach { tint ->
            assertTrue(
                "${benchInkName(tint.id)} is missing from the dark decor popover",
                card.pixelCountOf(tint.value.toArgb()).toDouble() > pot / 2,
            )
        }

        composeRule.onNodeWithTag(HOST_TAG)
            .captureRoboImage("$GOLDEN_DIR/bench_ink_popover_decor_dark.png", roborazziOptions = aa())
    }

    /**
     * ⚠ **The state a supply is actually in** — and the one the two captures above cannot reach.
     *
     * They pass `selected = null`, but `benchInkColorOf(DecorElement) = element.ink` is non-null for
     * **every** placed supply: a supply always has an ink, so the decor popover always draws a selection
     * ring in production. Independent review found the higher-value capture was blessing the one
     * configuration decor never occupies. That matters beyond tidiness — the ring is drawn at `inset:-5px`
     * *around* a pot, so on a paper tint it eats into the very count the fence above is measured with, and
     * a ring that grew could push a correct tint under the floor with nobody able to say why.
     *
     * Both halves are therefore asserted here, in one composition and two rasters (Robolectric permits
     * `setContent` once per rule): the ring appears, **and** the ringed tint still clears the fence.
     */
    @Test
    fun a_ringed_paper_tint_still_clears_the_fence_it_is_measured_by() {
        val selected = mutableStateOf<Color?>(null)
        // `Kraft` — the last paper tint, and the one a supply inked with paper would ring. Read from the
        // catalog rather than written as a literal so a palette change cannot leave this pointing at a
        // colour no band contains, which would make the ring assertion vacuous.
        val tint = inks.paperTints.last()
        var inkArgb = 0
        composeRule.setContent {
            ZinelyTheme(darkTheme = false) {
                inkArgb = ZinelyTheme.v21Colors.ink.toArgb()
                Box(
                    Modifier
                        .testTag(HOST_TAG)
                        .fillMaxWidth()
                        .background(ZinelyTheme.v21Colors.desk),
                ) {
                    BenchInkPopover(
                        visible = true,
                        bands = benchInkBands(inks, BenchVerbKind.DECOR),
                        presets = benchInkPresets(inks),
                        selected = selected.value,
                        inkCount = 2,
                        onPick = {},
                        onPreset = {},
                        onDone = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val unringed = full().pixelCountOf(inkArgb)

        selected.value = tint.value
        composeRule.waitForIdle()
        val ringed = full()
        assertTrue(
            "no ink ring appeared on the supply's own tint ($unringed -> ${ringed.pixelCountOf(inkArgb)}) " +
                "— a decor popover that cannot ring a paper tint is showing the band and disowning it",
            ringed.pixelCountOf(inkArgb) > unringed,
        )

        val card = crop(BenchInkPopoverTestTag, ringed)
        val pot = PI * px(15f) * px(15f)
        assertTrue(
            "${benchInkName(tint.id)} paints ${card.pixelCountOf(tint.value.toArgb())}px once ringed, " +
                "under the ${(pot / 2).toInt()}px floor the fence is measured by — the ring is eating " +
                "the swatch it marks",
            card.pixelCountOf(tint.value.toArgb()).toDouble() > pot / 2,
        )

        composeRule.onNodeWithTag(HOST_TAG)
            .captureRoboImage(
                "$GOLDEN_DIR/bench_ink_popover_decor_selected_light.png",
                roborazziOptions = aa(),
            )
    }
}
