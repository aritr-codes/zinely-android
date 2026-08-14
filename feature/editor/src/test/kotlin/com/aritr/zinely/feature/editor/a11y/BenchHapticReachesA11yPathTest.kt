package com.aritr.zinely.feature.editor.a11y

import android.content.Context
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.feature.editor.BenchContextBar
import com.aritr.zinely.feature.editor.BenchContextBarTestTag
import com.aritr.zinely.feature.editor.BenchVerbKind
import com.aritr.zinely.feature.editor.benchContextVerbs
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/**
 * The haptic reaches the **accessibility** activation path, not only the finger.
 *
 * ### Why this test exists in this shape
 *
 * The device evidence in [ADR-102 §12.15](../../../../../../../../docs/DECISIONS.md#adr-102-bench-haptics)
 * counts real vibrations out of `dumpsys vibrator_manager`, but a scripted `input tap` can only ever
 * exercise the *pointer* path. A Bench control is also reached through its semantics `onClick` action —
 * the route TalkBack, Switch Access and an external keyboard take — and that is a different code path on
 * the same modifier chain. `benchTap` is called inside the control precisely so both are fed; this
 * asserts the half a finger cannot reach.
 *
 * [performSemanticsAction] invokes the semantics action directly, injecting no touch input at all, so a
 * pass here cannot be the pointer path in disguise. Robolectric's vibrator shadow is the recorder: it is
 * the same instrument as the device dump, one layer down.
 *
 * `Delete` is the subject because it is the Bench's one `Boundary` verb — the tier the device measurement
 * explicitly does not cover.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BenchHapticReachesA11yPathTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun shadowVibrator() = shadowOf(
        (composeRule.activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
            .defaultVibrator,
    )

    @Test
    fun deleting_through_the_accessibility_action_still_answers_the_hand() {
        var deleted = false
        composeRule.setContent {
            ZinelyTheme {
                BenchContextBar(
                    visible = true,
                    verbs = benchContextVerbs(BenchVerbKind.TEXT),
                    onVerb = { verb -> if (verb.label == Copy.BenchVerbs.DELETE) deleted = true },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.DELETE}")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertTrue("the accessibility action must still perform the verb", deleted)
        // The recorded **pattern**, not `isVibrating`. The flag is set true and scheduled false after the
        // effect's own duration — `Boundary` lasts 24ms — so an assertion on it passes or fails by how far
        // `waitForIdle` happened to advance the frame clock, which is a margin nobody chose. The pattern is
        // a permanent record, it names *which* haptic fired, and it is what a double-fire would disturb.
        //
        // `androidTimings` prepends a zero-length wait, so `Boundary`'s `[24]` reaches the vibrator as
        // `[0, 24]`. Asserting the exact array pins the tier too: a `Tick` here would be the wrong answer
        // to a destructive verb, and this is the only place that can tell.
        //
        // Proven able to fail: with the `benchTap` wrapper removed from `BenchVerbButton` this goes red
        // while the "did the verb run" assertion above it stays green — exactly the state the Bench
        // shipped in for the whole re-skin.
        assertArrayEquals(
            "activating Delete through the accessibility tree must buzz, and buzz Boundary — the finger " +
                "is not the only way in, and a control that answers only touch is silent to a switch",
            longArrayOf(0, 24),
            shadowVibrator().pattern,
        )
    }
}
