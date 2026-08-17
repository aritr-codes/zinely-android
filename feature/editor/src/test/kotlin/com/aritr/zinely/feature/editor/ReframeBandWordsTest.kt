package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * **F-9 — both secondary session actions draw a word.**
 *
 * `Cancel` and `Reset` sit side by side in the band, and the difference between *throw this away* and
 * *start it over* is the whole decision on that surface. `Reset` shipped as a bare circular arrow — which
 * is the **rotate** glyph on the Bench's own transform row, one surface away. Same glyph family, different
 * act, adjacent surfaces (`docs/BETA-UX-REVIEW.md` F-9; `v21-reframe.html` `.text-btn`, revised
 * 2026-08-15, where both buttons now carry text).
 *
 * ### Why this test exists rather than a golden
 *
 * The defect is visual, and [ReframeControlsGoldenTest] is where a visual defect ought to be caught — but
 * its own KDoc records that `captureRoboImage` is a **no-op** under plain `testDebugUnitTest`, so it went
 * green across this exact change without comparing a pixel. A green golden is not evidence here.
 *
 * What this file asserts instead is the nearest **verifiable** consequence: a control sized by its own word
 * is not the 44dp square it used to be, and two controls carrying different words do not measure the same.
 * That is a proxy, stated as one — the drawn glyphs themselves are only provable by a golden that really
 * compares, or by a device screenshot, both of which are recorded as owed in `docs/BETA-UX-REVIEW.md`.
 *
 * It also pins what did **not** change: the spoken labels were correct throughout, which is precisely why
 * no accessibility assertion in this repo could see the defect. Both are asserted together so a later
 * "simplification" cannot quietly swap the drawn word for the spoken one.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp")
class ReframeBandWordsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun host() {
        composeRule.setContent {
            ZinelyTheme {
                ReframeControls(
                    fit = FrameFit.FILL,
                    zoomPercent = 100,
                    abilities = ReframeAbilities(
                        zoomIn = true, zoomOut = true, panHorizontally = true, panVertically = true,
                    ),
                    onFit = {},
                    onNudge = { _, _ -> },
                    onZoom = {},
                    onReset = {},
                    onCancel = {},
                    onDone = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * ⚠ **The drawn word cannot be queried, and that is not a gap in this test — it is the control's
     * design.** Both buttons publish `clearAndSetSemantics`, which removes their `Text` child from the
     * semantics tree entirely, so `onNodeWithText("Reset")` finds nothing however plainly the word is on
     * screen. (Measured: the first cut of this file asserted exactly that and failed against a correct
     * implementation.) The word is therefore proven by **geometry** below — a control sized to text cannot
     * be the 44dp square the icon button was — which is the strongest signal available without a golden
     * that actually compares pixels.
     */
    @Test
    fun each_button_is_sized_by_its_own_word() {
        host()

        val cancel = composeRule.onNodeWithTag("reframe-${Copy.Reframe.CANCEL_REFRAMING}")
            .fetchSemanticsNode().boundsInRoot
        val reset = composeRule.onNodeWithTag("reframe-${Copy.A11y.RESET_FRAMING}")
            .fetchSemanticsNode().boundsInRoot

        // Same clothes, same height — `.text-btn{height:44px}` is the icon button's height kept.
        assertEquals("the two secondary actions must share one height", cancel.height, reset.height, 0.5f)
        // Different words, different widths. Two equal widths would mean neither is sized by its text,
        // which is what a pair of icon squares looks like.
        assertTrue(
            "Cancel is the longer word and must draw wider — got cancel=$cancel reset=$reset",
            cancel.width > reset.width,
        )
    }

    @Test
    fun the_drawn_word_is_short_and_the_spoken_label_stays_long() {
        host()

        // The two channels differ on purpose: "Reset" alone is ambiguous with no bar around it, so the
        // screen reader keeps the sentence while the button keeps the word.
        composeRule.onNodeWithContentDescription(Copy.Reframe.CANCEL_REFRAMING).assertExists()
        composeRule.onNodeWithContentDescription(Copy.A11y.RESET_FRAMING).assertExists()
        assertTrue(
            "the spoken label must say more than the drawn word",
            Copy.A11y.RESET_FRAMING.length > Copy.Reframe.RESET.length,
        )
    }

    @Test
    fun reset_is_sized_to_its_text_rather_than_to_a_44dp_icon_square() {
        host()

        val node = composeRule.onNodeWithTag("reframe-${Copy.A11y.RESET_FRAMING}").fetchSemanticsNode()
        val bounds = node.boundsInRoot
        // `.text-btn{height:44px; padding:0 var(--gap-md)}` — the height is the icon button's, the width
        // is not. A square node here means the icon button came back.
        assertTrue(
            "Reset is drawn as a square, i.e. still an icon button — got $bounds",
            bounds.width > bounds.height,
        )
    }
}
