package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.feature.editor.ProofReadAct
import com.aritr.zinely.feature.editor.ProofReadNextTestTag
import com.aritr.zinely.feature.editor.ProofReadPrevTestTag
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
 * **The two turn edges, read off the platform tree — because a name is not an affordance.**
 *
 * [SurfaceTraversalOrderTest] already proves these two exist in the `AccessibilityNodeInfo` tree and are
 * reached in the right order; it caught them being *absent* entirely, which is why they are there at all.
 * What it cannot prove is that a screen-reader user can **activate** them, and on this surface that gap is
 * not theoretical: [ProofStepDotsA11yTest] records a real device reporting every fold dot as
 * `android.view.View` with `clickable=false`, with the click sitting on an unlabelled ancestor —
 * [ADR-059](../../../../../../../docs/DECISIONS.md#adr-059)'s surface-wide Role→View defect. The turn
 * edges are built the same way those controls are: a `Box` with `clickable`.
 *
 * They are also **the reader's only navigation**. If they map the way the dots do, a TalkBack user cannot
 * turn a page at all, and the screen answers *"you made a cover"*.
 *
 * ### What this file is, and what it is not
 *
 * It asserts `className`, `isClickable` and `isEnabled` off the platform node. Per the standing lesson in
 * [ProofStepDotsA11yTest], **Robolectric's platform bridge and a phone have disagreed here before, and the
 * device wins** — so this is a *pin on the harness*, not evidence about hardware. The device dump that is
 * the evidence lives in [ADR-101 §6.10](../../../../../../../docs/DECISIONS.md#adr-101-p5). If a Compose
 * or Robolectric upgrade changes what this environment reports, this fails and someone re-measures on a
 * phone rather than inheriting a comforting green.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class ProofReadTurnEdgesA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun pages(count: Int = 8): List<Page> = (0 until count).map { i ->
        Page(
            index = i,
            role = if (i == 0) PageRole.FRONT_COVER else PageRole.INTERIOR,
            elements = listOf(
                TextElement(id = "t$i", transform = Transform(20.0, 20.0, 160.0, 40.0), text = "page ${i + 1}"),
            ),
        )
    }

    private fun renderReader() {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.size(430.dp, 932.dp)) {
                    ProofReadAct(
                        pages = pages(),
                        pageSizePt = PtSize(200.0, 300.0),
                        defaults = DocumentDefaults(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `both turn edges reach the platform as activatable, labelled buttons`() {
        renderReader()

        listOf(
            ProofReadPrevTestTag to Copy.ProofRead.PREVIOUS_PAGE,
            ProofReadNextTestTag to Copy.ProofRead.NEXT_PAGE,
        ).forEach { (tag, label) ->
            val node = composeRule.onNodeWithTag(tag).platformNode(composeRule.activity)
            assertEquals("“$label” lost its spoken label", label, node.contentDescription)
            assertTrue("“$label” is not clickable on the platform", node.isClickable)
            assertEquals("“$label” is not a Button on the platform", "android.widget.Button", node.className)
        }
    }

    /**
     * **Disabled must reach the platform as disabled.** The edges carry no visible chrome of their own
     * beyond a chevron that hides, so a screen-reader user has nothing else to tell them the book has
     * ended — `isEnabled` is the whole signal, and a control that reports enabled while doing nothing is
     * the defect this surface already shipped once (`ReframeControls.ZoomButton`, CLAUDE.md).
     */
    @Test
    fun `the platform sees Previous disabled on the cover and Next disabled on the back cover`() {
        renderReader()

        fun edge(tag: String) = composeRule.onNodeWithTag(tag).platformNode(composeRule.activity)

        assertFalse("Previous page reports enabled on the cover", edge(ProofReadPrevTestTag).isEnabled)
        assertTrue("Next page reports disabled on the cover", edge(ProofReadNextTestTag).isEnabled)

        repeat(7) {
            composeRule.onNodeWithTag(ProofReadNextTestTag).performClick()
            composeRule.waitForIdle()
        }

        assertTrue("Previous page reports disabled on the back cover", edge(ProofReadPrevTestTag).isEnabled)
        assertFalse(
            "Next page reports enabled on the back cover — the book has no ninth leaf",
            edge(ProofReadNextTestTag).isEnabled,
        )
    }
}
