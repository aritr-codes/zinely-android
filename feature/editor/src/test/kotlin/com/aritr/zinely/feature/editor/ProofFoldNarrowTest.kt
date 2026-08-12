package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **The fold nav, on the narrowest window that ships.**
 *
 * The rest of this module runs at `w430dp` — and 430dp is the one width at which the first P4 nav row
 * fitted. It needed 348dp intrinsically (52 + 18 + 8×26 + 18 + 52); a 360dp phone leaves ~284dp inside the
 * drawer's and the guide's padding, the drawer clips, and **both arrows lost more than half their touch
 * target on every one of eight steps**. No test could see it, because every test was run at the one width
 * where it fitted.
 *
 * `drawerBodyMaxHeight`'s own KDoc already records this exact class of blindness on the *height* axis, in
 * this same package, found the same way. So this file exists to hold the other axis, permanently: the
 * controls are asserted at 360dp, not at the developer's device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class ProofFoldNarrowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * The guide is rendered inside the drawer's own horizontal padding (`ZSheetSurface`, 20dp a side).
     * Without it this harness measures a width the user never gets, which is how the first version of
     * this file reported 28dp-wide dots for a build that shipped 23dp ones.
     */
    private fun renderGuide(step: Int) {
        walkableGuide()(step)
    }

    private fun navTop(tag: String): Dp =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot().top

    /**
     * A guide whose step can be changed **without** re-setting the content — `setContent` may be called
     * once per activity, and re-rendering from scratch would also be the wrong measurement: what is under
     * test is what happens to a live composition when the user presses Next.
     */
    private fun walkableGuide(fontScale: Float = 1f): (Int) -> Unit {
        val step = mutableStateOf(0)
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(base.density, fontScale),
            ) {
                ZinelyTheme {
                    Box(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                        ProofFoldAct(
                            step = step.value,
                            reduceMotion = true,
                            onNext = {},
                            onPrev = {},
                            onGoToStep = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        return { to -> step.value = to; composeRule.waitForIdle() }
    }

    /**
     * **The controls do not move between steps** — the assertion the fix that caused this defect never had.
     *
     * The nav row used to sit directly under the caption, so its position was a function of how tall the
     * step above it happened to be. On a device, step 1's precondition line disappearing lifted the whole
     * row 108px and put `Next` exactly where the dots row had been; two taps in the same place — which
     * *"tap the arrow when a step is done"* invites — meant *next, then jump to step 8*. It reached the
     * automation before it reached a person, and only because the automation re-located the arrow each
     * time. The fix (a reserved caption height) shipped with size assertions and no position assertion,
     * which is the same shape as the 44dp dot claim that was built at 23dp: **a fix is not measured until
     * a test can fail on it.**
     *
     * Step 7 is excluded on purpose: the last step replaces `Next` with the finish primary, so the row
     * legitimately moves there. What must not move is steps 0–6, where the same arrow is in play.
     */
    @Test
    fun the_nav_row_and_the_dots_sit_at_the_same_height_on_every_step_that_shares_an_arrow() {
        val goTo = walkableGuide()
        val prev = navTop(ProofStepPrevTestTag)
        val next = navTop(ProofStepNextTestTag)
        val dots = navTop(ProofStepDotsTestTag)

        for (step in 1..FOLD_LAST_STEP - 1) {
            goTo(step)
            assertEquals("Previous step moved on step ${step + 1}", prev, navTop(ProofStepPrevTestTag))
            assertEquals("Next step moved on step ${step + 1}", next, navTop(ProofStepNextTestTag))
            assertEquals("the dots moved on step ${step + 1}", dots, navTop(ProofStepDotsTestTag))
        }
    }

    /**
     * The same invariant at **fontScale 1.3**, because the reserve that holds it is derived from `sp`
     * precisely so that it can be. Written as a `dp` literal it holds three caption lines at 1.0 and stops
     * around 1.15 — reintroducing the mis-tap for the users least able to absorb it, and doing it in a
     * configuration no test covered.
     */
    @Test
    fun the_nav_row_still_does_not_move_between_steps_at_font_scale_1_3() {
        val goTo = walkableGuide(fontScale = 1.3f)
        val next = navTop(ProofStepNextTestTag)

        for (step in 1..FOLD_LAST_STEP - 1) {
            goTo(step)
            assertEquals(
                "Next step moved on step ${step + 1} at fontScale 1.3",
                next,
                navTop(ProofStepNextTestTag),
            )
        }
    }

    /**
     * **The legend wraps rather than clips** — the claim three places in this branch make and nothing
     * measured.
     *
     * Five chips do not fit on one 360dp row; nor did four, which is the point. The row was a fixed `Row`
     * measuring ~295dp against the ~284dp a 360dp phone leaves inside the drawer's and the guide's
     * padding, so it was already clipping its last mark before the fifth was added — invisibly, because
     * the rest of the module runs at `w430dp`, and every fold golden that might have caught it was deleted
     * in this same package.
     *
     * Two assertions, because either alone passes on a broken layout. The height proves it took a second
     * row (a clipping `Row` stays one chip tall); the last chip's own displayed width proves that chip
     * survived the wrap instead of being squeezed to nothing.
     */
    @Test
    fun the_five_mark_legend_wraps_instead_of_clipping_its_last_chip_at_360dp() {
        renderGuide(step = 0)

        val legend = composeRule.onNodeWithTag(ProofFoldLegendTestTag, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val chip = composeRule.onNodeWithText(Copy.ProofFold.LEGEND_CREASE, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val oneRow = chip.bottom - chip.top
        assertTrue(
            "the legend is still one row tall — five chips cannot fit 360dp, so it is clipping",
            legend.bottom - legend.top > oneRow * 1.5f,
        )
        // The fifth mark, the one this package added, is the one a clip eats first.
        composeRule.onNodeWithText(Copy.ProofFold.LEGEND_ACT).assertWidthIsAtLeast(40.dp)
    }

    @Test
    fun both_arrows_keep_their_whole_touch_target_at_360dp() {
        // A middle step, where both arrows exist.
        renderGuide(step = 3)

        composeRule.onNodeWithTag(ProofStepPrevTestTag).assertWidthIsAtLeast(44.dp)
        composeRule.onNodeWithTag(ProofStepPrevTestTag).assertHeightIsAtLeast(44.dp)
        composeRule.onNodeWithTag(ProofStepNextTestTag).assertWidthIsAtLeast(44.dp)
        composeRule.onNodeWithTag(ProofStepNextTestTag).assertHeightIsAtLeast(44.dp)
    }

    /**
     * **Both axes, because only one of them was ever the problem.** The dots' height was never in doubt;
     * their *width* was 23dp while they shared a row with two 44dp arrows — under even WCAG 2.5.8's 24×24
     * floor (adjacent targets, so the spacing exception does not apply), and claimed as 44dp in the ADR.
     * They have their own row now, so the eight of them split the whole 284dp a 360dp phone leaves inside
     * the drawer: ~35dp each.
     */
    @Test
    fun every_step_dot_keeps_a_real_touch_target_on_both_axes_at_360dp() {
        renderGuide(step = 0)

        repeat(PROOF_FOLD_STEP_COUNT) { i ->
            val dot = composeRule.onNodeWithTag(proofStepDotTag(i), useUnmergedTree = true)
            dot.assertHeightIsAtLeast(44.dp)
            dot.assertWidthIsAtLeast(32.dp)
        }
    }
}
