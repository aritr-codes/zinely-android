package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.feature.editor.ProofPaperSegmentsTestTag
import com.aritr.zinely.feature.editor.ProofPrintDetailsPanel
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **The paper segments, read off the platform tree rather than Compose's own.**
 *
 * ADR-101 P3 shipped `.paperseg` as a `selectableGroup()` of `Role.RadioButton` controls and the ADR
 * claimed TalkBack would therefore say *"A4, selected, 1 of 2"*. The P3 evidence review pointed out that
 * the only assertion behind that sentence was `assertIsSelected()` on the **merged semantics** tree, which
 * is a different tree from the one a service reads — and this repository already owns the defect that
 * proves the distinction matters (`Role.Button` cells drew a selection ring no service could see, while
 * every merged-tree assertion passed; see `BenchPageNavA11yTest`).
 *
 * So the claim is measured here instead of asserted there.
 *
 * The panel is rendered **directly**, not through its [com.aritr.zinely.ui.components.ZSheet] drawer, and
 * that is a limit worth naming rather than hiding: `platformNode` resolves the composition through
 * `activity.window.decorView`, and a `Dialog`-hosted sheet (ADR-049) is a separate window the harness
 * cannot reach. What this proves is that the *control* reports its selection to the platform. Whether the
 * drawer's window presents it in the right traversal order remains a device Pass 1 gate.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class ProofPaperSegmentsA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun renderPanel(paper: PaperSize = PaperSize.A4) {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.size(430.dp, 932.dp)) {
                    ProofPrintDetailsPanel(paper = paper, onPaperSelected = {}, onOpenFold = {})
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun segment(label: String) =
        composeRule.onNodeWithTag(ProofPaperSegmentsTestTag).onChildren().filterToOne(hasText(label))

    /**
     * The load-bearing one: a service is told **which** size is chosen, so the filled stamp-coloured
     * segment is not information only a sighted user has.
     *
     * It arrives as `stateDescription`, not `isSelected` — Compose maps `SemanticsProperties.Selected`
     * onto `AccessibilityNodeInfo.isSelected` for `Role.Tab` alone, and both segments report
     * `isSelected=false` here. That is asserted too, so the day Compose changes it this test says so
     * rather than silently drifting into agreeing with a comment.
     */
    @Test
    fun the_platform_is_told_which_paper_size_is_chosen() {
        renderPanel(paper = PaperSize.A4)

        val a4 = segment(Copy.Paper.A4).platformNode(composeRule.activity)
        val letter = segment(Copy.Paper.LETTER).platformNode(composeRule.activity)

        assertEquals("the chosen segment's state never reaches a service", "Selected", a4.stateDescription)
        assertEquals("the other segment's state never reaches a service", "Not selected", letter.stateDescription)
        assertTrue(
            "Compose now maps Selected → isSelected for this role; prefer that and simplify this test",
            !a4.isSelected && !letter.isSelected,
        )
    }

    /**
     * **The half of the ADR-101 §6.6 claim that turned out to be false, pinned so it stays visible.**
     *
     * The ADR said TalkBack would announce *"A4, selected, 1 of 2"*. The position half cannot happen: the
     * segment reaches the platform as a bare `android.view.View`, not `android.widget.RadioButton`, so no
     * service can count it within its group. `ZButtonPlatformA11yTest` already recorded this collapse for
     * a control that wraps child content, and each segment is exactly that shape — a `Box` around a
     * `BasicText`. What ships is *"A4, Selected"*, which is sufficient and is what [Copy.Paper] now says.
     *
     * Asserted rather than commented because a comment is what produced the wrong claim in the first
     * place. If a Compose upgrade starts emitting the real role, this fails and the announcement improves.
     */
    @Test
    fun the_radio_button_role_does_not_reach_the_platform() {
        renderPanel(paper = PaperSize.A4)

        assertEquals(
            "android.view.View",
            segment(Copy.Paper.A4).platformNode(composeRule.activity).className,
        )
    }
}
