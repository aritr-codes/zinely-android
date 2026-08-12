package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.feature.editor.ProofFoldAct
import com.aritr.zinely.feature.editor.proofStepDotTag
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **The fold step dots — and the day this harness was caught disagreeing with a phone.**
 *
 * This file was written to close a P4 review finding: `Role.Tab` mapping `Selected` onto the platform had
 * been *asserted in a comment*, never measured. It measures it, off `AccessibilityNodeInfo`, and it passes.
 *
 * **It is not evidence about a device.** On a real SM-A176B (Android 16), `uiautomator dump` of this exact
 * composition reports every dot as `android.view.View` with `clickable=false`, `focusable=false`,
 * `selected=false`, with an unlabelled ancestor carrying the click. Seven implementation variants were
 * built and measured against the phone; the platform tree never changed, and this test passed for all
 * seven. Robolectric's platform bridge and the device do not agree here, and **the device wins** — see
 * ADR-101 §6.8 and [ADR-059](../../../../../../../docs/DECISIONS.md#adr-059), which owns the surface-wide
 * Role→View defect this turned out to be an instance of.
 *
 * So the assertions below are kept deliberately, as a *pin on the harness*: if a Compose or Robolectric
 * upgrade changes what this environment reports, this test fails and someone re-measures on hardware
 * instead of inheriting a comforting green. What it must never again be read as is proof that a screen
 * reader can use these dots. It cannot, today.
 *
 * Rendered directly rather than through the [com.aritr.zinely.ui.components.ZSheet] drawer for the reason
 * [ProofPaperSegmentsA11yTest] names: `platformNode` resolves through `activity.window.decorView`, and a
 * `Dialog`-hosted sheet is a separate window.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class ProofStepDotsA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun renderGuide(step: Int) {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.size(430.dp, 932.dp)) {
                    ProofFoldAct(
                        step = step,
                        reduceMotion = true,
                        onNext = {},
                        onPrev = {},
                        onGoToStep = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun dot(index: Int) =
        composeRule.onNodeWithTag(proofStepDotTag(index), useUnmergedTree = true)
            .platformNode(composeRule.activity)

    /** What this environment reports. The phone reports `false` for all three — see the class comment. */
    @Test
    fun robolectric_reports_the_current_step_as_selected() {
        renderGuide(step = 3)

        assertTrue("the harness stopped mapping Selected; re-measure on hardware", dot(3).isSelected)
        assertFalse(dot(0).isSelected)
        assertFalse(dot(7).isSelected)
    }

    /**
     * `Role.Tab` is a selection mapping, not a widget class: even here a dot arrives as a bare
     * `android.view.View`, so no service can count it within its group. Position comes from the step
     * counter and each dot's own label.
     */
    @Test
    fun the_tab_role_is_a_selection_mapping_not_a_widget_class() {
        renderGuide(step = 0)

        assertEquals("android.view.View", dot(0).className)
    }
}
