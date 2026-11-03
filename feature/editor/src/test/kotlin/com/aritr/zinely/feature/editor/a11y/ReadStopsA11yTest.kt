package com.aritr.zinely.feature.editor.a11y

import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.platform.ViewRootForTest
import com.aritr.zinely.feature.editor.ReadStopTestTagPrefix
import com.aritr.zinely.feature.editor.ProofReadPrevTestTag
import com.aritr.zinely.ui.a11y.platformNode
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.feature.editor.ProofReadAct
import com.aritr.zinely.feature.editor.ProofReadBookTestTag
import com.aritr.zinely.feature.editor.ProofReadNextTestTag
import com.aritr.zinely.feature.editor.proofReadPageTag
import com.aritr.zinely.ui.a11y.platformTraversalStops
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** ADR-135 part 1: Read's stops in the platform tree, and a page that does not turn under a stop's centre tap. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class ReadStopsA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun pages() = (0 until 4).map { i ->
        Page(
            index = i,
            role = if (i == 0) PageRole.FRONT_COVER else PageRole.INTERIOR,
            elements = if (i == 0) {
                listOf(
                    TextElement("t", Transform(20.0, 180.0, 160.0, 40.0), text = "Moth night"),
                    ImageElement("p", Transform(20.0, 20.0, 160.0, 100.0), assetId = "a".repeat(64), description = "A moth on the porch wall"),
                    DecorElement("d", Transform(150.0, 240.0, 30.0, 30.0), supplyId = "tape.torn", ink = ColorRgba.BLACK),
                )
            } else {
                emptyList()
            },
        )
    }

    private fun render() {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.size(430.dp, 932.dp)) {
                    ProofReadAct(pages = pages(), pageSizePt = PtSize(200.0, 300.0), defaults = DocumentDefaults())
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun touchExploration(on: Boolean) {
        val manager = composeRule.activity.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        shadowOf(manager).setTouchExplorationEnabled(on)
    }

    private fun stopTag(i: Int) = ReadStopTestTagPrefix + i

    @Test
    fun the_page_carries_its_stops_in_spatial_order_and_nothing_else() {
        render()
        val labels = platformTraversalStops(composeRule.activity).map { it.label }
        // Undescribed Art ("tape.torn" at the foot) is decoration: no stop.
        assertEquals(
            listOf("Photo: A moth on the porch wall", "Moth night"),
            labels.dropWhile { it != Copy.ProofRead.NEXT_PAGE }.drop(1),
        )
        assertEquals(listOf(Copy.ProofRead.PREVIOUS_PAGE, Copy.ProofRead.NEXT_PAGE), labels.take(2))
        assertEquals(0, composeRule.onAllNodesWithTag(stopTag(2)).fetchSemanticsNodes().size)
    }

    @Test
    fun a_stop_has_no_role_no_click_and_no_actions() {
        render()
        listOf(0, 1).forEach { i ->
            val node = composeRule.onNodeWithTag(stopTag(i)).platformNode(composeRule.activity)
            assertEquals("stop $i must not be a Button", false, node.className == "android.widget.Button")
            assertEquals("stop $i must not be clickable", false, node.isClickable)
            assertEquals("stop $i must offer no custom action", emptyList<String>(), node.actionLabels)
        }
    }

    /** What a screen reader walks: Previous, the page's stops in SpatialOrder, Next -- by `traversalBefore`. */
    @Test
    fun with_accessibility_on_traversal_chains_previous_then_the_stops_then_next() {
        render()
        val root = findComposeView(composeRule.activity.window.decorView)
        val manager = composeRule.activity.getSystemService(AccessibilityManager::class.java)
        composeRule.runOnUiThread { shadowOf(manager).setEnabled(true) }
        (root as ViewRootForTest).forceAccessibilityForTesting(true)
        composeRule.waitForIdle()
        val provider = root.accessibilityNodeProvider!!
        val ids = listOf(ProofReadPrevTestTag, stopTag(0), stopTag(1), ProofReadNextTestTag)
            .map { composeRule.onNodeWithTag(it).fetchSemanticsNode().id }
        ids.zipWithNext().forEach { (node, next) ->
            val info = provider.createAccessibilityNodeInfo(node)!!
            assertEquals(
                "node $node must be hinted before $next (extras: ${info.extras.keySet()})",
                next,
                info.extras.getInt(TRAVERSAL_BEFORE_KEY, -1),
            )
        }
    }

    private fun findComposeView(v: View): View =
        if (v is ViewRootForTest) {
            v
        } else {
            (0 until ((v as? ViewGroup)?.childCount ?: 0)).firstNotNullOfOrNull {
                runCatching { findComposeView((v as ViewGroup).getChildAt(it)) }.getOrNull()
            } ?: error("no Compose root under $v")
        }

    private companion object {
        const val TRAVERSAL_BEFORE_KEY = "android.view.accessibility.extra.EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL"
    }

    @Test
    fun an_empty_page_has_its_one_stop() {
        render()
        composeRule.onNodeWithTag(ProofReadNextTestTag).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        val labels = platformTraversalStops(composeRule.activity).map { it.label }
        assertEquals(1, labels.count { it == Copy.ProofRead.BLANK_PAGE })
    }

    private fun tapStageAt(rootX: Float, rootY: Float) {
        val stage = composeRule.onNodeWithTag(ProofReadBookTestTag).fetchSemanticsNode().boundsInRoot
        composeRule.onNodeWithTag(ProofReadBookTestTag).performTouchInput { click(Offset(rootX - stage.left, rootY - stage.top)) }
        composeRule.waitForIdle()
    }

    private fun onFirstLeaf() = composeRule.onAllNodesWithTag(proofReadPageTag(1)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun with_touch_exploration_on_a_tap_inside_the_leaf_near_either_side_does_not_turn_the_page() {
        render()
        touchExploration(true)
        val leaf = composeRule.onNodeWithTag(proofReadPageTag(1)).fetchSemanticsNode().boundsInRoot
        tapStageAt(leaf.right - 8f, leaf.center.y)
        assertEquals(true, onFirstLeaf())
    }

    @Test
    fun the_left_side_is_guarded_too() {
        render()
        composeRule.onNodeWithTag(ProofReadNextTestTag).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        touchExploration(true)
        val leaf = composeRule.onNodeWithTag(proofReadPageTag(2)).fetchSemanticsNode().boundsInRoot
        tapStageAt(leaf.left + 8f, leaf.center.y)
        assertEquals(true, composeRule.onAllNodesWithTag(proofReadPageTag(2)).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun the_stops_add_no_live_region_so_a_turn_speaks_only_the_ticket() {
        render()
        val live = composeRule.onAllNodes(
            androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.LiveRegion),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertEquals(0, live.size)
    }

    @Test
    fun a_tap_on_the_edge_outside_the_leaf_still_turns_and_so_does_the_edges_own_click_action() {
        render()
        touchExploration(true)
        val leaf = composeRule.onNodeWithTag(proofReadPageTag(1)).fetchSemanticsNode().boundsInRoot
        val stage = composeRule.onNodeWithTag(ProofReadBookTestTag).fetchSemanticsNode().boundsInRoot
        tapStageAt((leaf.right + stage.right) / 2f, leaf.center.y)
        assertEquals(false, onFirstLeaf())
    }

    @Test
    fun the_edges_semantic_click_still_turns_with_touch_exploration_on() {
        render()
        touchExploration(true)
        composeRule.onNodeWithTag(ProofReadNextTestTag).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals(false, onFirstLeaf())
    }

    @Test
    fun with_touch_exploration_off_tap_to_turn_is_unchanged() {
        render()
        touchExploration(false)
        val leaf = composeRule.onNodeWithTag(proofReadPageTag(1)).fetchSemanticsNode().boundsInRoot
        tapStageAt(leaf.right - 8f, leaf.center.y)
        assertEquals(false, onFirstLeaf())
    }
}
