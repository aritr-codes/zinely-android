package com.aritr.zinely.ui.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * [ZinelyV2CanvasSemantics] in a composition — the three properties its KDoc calls easy to lose, each
 * asserted where losing it would otherwise be silent. The geometry itself is covered purely in
 * [ZinelyV2CanvasSemanticsTest]; this file is only about what the node tree does once it exists.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ZinelyV2CanvasSemanticsLayerTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun node(key: String, label: String, actions: List<CustomAccessibilityAction> = emptyList()) =
        ZinelyV2CanvasNode(
            key = key,
            corners = when (key) {
                "a" -> listOf(Offset(0f, 0f), Offset(60f, 0f), Offset(60f, 60f), Offset(0f, 60f))
                else -> listOf(Offset(0f, 80f), Offset(60f, 80f), Offset(60f, 140f), Offset(0f, 140f))
            },
            label = label,
            selected = key == "a",
            activateLabel = "Select",
            onActivate = {},
            actions = actions,
        )

    @Test
    fun `each drawn thing gets its own named node, carrying its selected state`() {
        composeRule.setContent {
            ZinelyV2CanvasSemantics(
                nodes = listOf(node("a", "Title text"), node("b", "Photo")),
                modifier = Modifier.size(200.dp),
            )
        }
        val a = composeRule.onNodeWithTag("${ZinelyV2CanvasNodeTagPrefix}a").fetchSemanticsNode()
        val b = composeRule.onNodeWithTag("${ZinelyV2CanvasNodeTagPrefix}b").fetchSemanticsNode()
        assertEquals(listOf("Title text"), a.config[SemanticsProperties.ContentDescription])
        assertEquals(listOf("Photo"), b.config[SemanticsProperties.ContentDescription])
        assertEquals(true, a.config[SemanticsProperties.Selected])
        assertEquals(false, b.config[SemanticsProperties.Selected])
    }

    @Test
    fun `the named custom actions reach the node, in the order the caller declared them`() {
        // The constitution's rule is that every gesture has a named single-pointer twin. The layer's job
        // is to carry them intact; a silently reordered or truncated list would strand a gesture.
        val names = listOf("Move left", "Move right", "Delete")
        composeRule.setContent {
            ZinelyV2CanvasSemantics(
                nodes = listOf(node("a", "Title text", names.map { CustomAccessibilityAction(it) { true } })),
                modifier = Modifier.size(200.dp),
            )
        }
        val actions = composeRule.onNodeWithTag("${ZinelyV2CanvasNodeTagPrefix}a")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(names, actions.map { it.label })
    }

    @Test
    fun `traversal order is pinned to declaration order, not left to overlapping bounds`() {
        composeRule.setContent {
            ZinelyV2CanvasSemantics(
                nodes = listOf(node("a", "Title text"), node("b", "Photo")),
                modifier = Modifier.size(200.dp),
            )
        }
        val a = composeRule.onNodeWithTag("${ZinelyV2CanvasNodeTagPrefix}a").fetchSemanticsNode()
        val b = composeRule.onNodeWithTag("${ZinelyV2CanvasNodeTagPrefix}b").fetchSemanticsNode()
        assertEquals(0f, a.config[SemanticsProperties.TraversalIndex], 0f)
        assertEquals(1f, b.config[SemanticsProperties.TraversalIndex], 0f)
    }

    @Test
    fun `a node does not consume pointer input - a touch falls through to the canvas beneath it`() {
        // The property most easily destroyed by "tidying" the layer into `clickable`. If these nodes took
        // pointer input they would sit on top of the drawing surface and swallow every tap and drag the
        // gesture layer exists to receive — the canvas would stop responding to a finger entirely, while
        // every semantics test stayed green.
        var canvasTaps = 0
        composeRule.setContent {
            Box(Modifier.size(200.dp)) {
                Box(
                    Modifier
                        .testTag("canvas-beneath")
                        .size(200.dp)
                        .clickable { canvasTaps++ },
                )
                ZinelyV2CanvasSemantics(
                    nodes = listOf(node("a", "Title text")),
                    modifier = Modifier.size(200.dp),
                )
            }
        }
        // Tap at (20, 20) px — INSIDE node "a", whose bounds are 0..60 px. An earlier version of this
        // test used performClick() on the surface, which taps the CENTRE of the 200dp box, nowhere near
        // any node: it passed with the nodes made `clickable`, and so proved nothing.
        composeRule.onNodeWithTag("canvas-beneath").performTouchInput { click(Offset(20f, 20f)) }
        assertEquals("the tap must reach the surface under the semantic overlay", 1, canvasTaps)
    }

    @Test
    fun `a node with no corners is skipped rather than placed at the origin`() {
        composeRule.setContent {
            ZinelyV2CanvasSemantics(
                nodes = listOf(
                    ZinelyV2CanvasNode("ghost", emptyList(), "Nothing", false, "Select", {}, emptyList()),
                    node("b", "Photo"),
                ),
                modifier = Modifier.size(200.dp),
            )
        }
        composeRule.onNodeWithTag("${ZinelyV2CanvasNodeTagPrefix}ghost").assertDoesNotExist()
        composeRule.onNodeWithTag("${ZinelyV2CanvasNodeTagPrefix}b").assertExists()
    }

    @Test
    fun `each node is placed over the thing it names, at the size the geometry computed`() {
        // RF-4: the pure geometry can be perfect while the composable wires it to the wrong modifier.
        // Nothing else reads a node's real position, so a swapped x/y — or a missing .offset{} that
        // stacks every node at the origin — would be invisible. Asserted in root coordinates at
        // density 1.0, where 1 px == 1 dp, against the same pure function the layer uses.
        composeRule.setContent {
            ZinelyV2CanvasSemantics(
                nodes = listOf(node("a", "Title text"), node("b", "Photo")),
                modifier = Modifier.size(200.dp),
            )
        }
        val minPx = with(composeRule.density) { ZinelyV2CanvasNodeMinSide.toPx() }
        for (key in listOf("a", "b")) {
            val expected = zinelyV2CanvasNodeBounds(node(key, "x").corners, minPx)!!
            val actual = composeRule.onNodeWithTag("$ZinelyV2CanvasNodeTagPrefix$key")
                .fetchSemanticsNode().boundsInRoot
            assertEquals("node $key left", expected.leftPx.toFloat(), actual.left, 1f)
            assertEquals("node $key top", expected.topPx.toFloat(), actual.top, 1f)
            assertEquals("node $key width", expected.widthPx, actual.width, 1f)
            assertEquals("node $key height", expected.heightPx, actual.height, 1f)
        }
    }

    @Test
    fun `a node can be activated - the one action a screen-reader user has on the whole layer`() {
        // RF-6: without the onClick semantic action the layer is readable and inert. Fired through the
        // semantics action, which is the path a service uses; the node takes no pointer input at all.
        var activated = 0
        composeRule.setContent {
            ZinelyV2CanvasSemantics(
                nodes = listOf(
                    ZinelyV2CanvasNode(
                        key = "a",
                        corners = listOf(Offset(0f, 0f), Offset(60f, 0f), Offset(60f, 60f), Offset(0f, 60f)),
                        label = "Title text",
                        selected = false,
                        activateLabel = "Select",
                        onActivate = { activated++ },
                        actions = emptyList(),
                    ),
                ),
                modifier = Modifier.size(200.dp),
            )
        }
        val semantics = composeRule.onNodeWithTag("${ZinelyV2CanvasNodeTagPrefix}a").fetchSemanticsNode()
        val onClick = semantics.config[SemanticsActions.OnClick]
        assertEquals("the activation must carry the caller's name", "Select", onClick.label)
        onClick.action!!.invoke()
        assertEquals("activating the node must run the caller's handler", 1, activated)
    }

    @Test
    fun `a canvas node reaches the PLATFORM tree as a Button, not as a generic View`() {
        // The package's own thesis, asserted for the canvas half. Every other test in this file reads
        // Compose's merged semantics tree, where the role is always present and always correct — so
        // deleting `role = Role.Button` from the layer left the whole suite green while the node it
        // publishes reverted to `android.view.View`. That is the ADR-059 defect, reintroduced in the half
        // of the package that exists for screen-reader users, invisible to 146 passing tests.
        //
        // This is the assertion the CI-26 harness was moved into :core:ui to make possible.
        composeRule.setContent {
            ZinelyV2CanvasSemantics(
                nodes = listOf(node("a", "Title text")),
                modifier = Modifier.size(200.dp),
            )
        }
        val platform = composeRule.onNodeWithTag("${ZinelyV2CanvasNodeTagPrefix}a")
            .platformNode(composeRule.activity)
        assertEquals("android.widget.Button", platform.className)
        assertEquals("Title text", platform.contentDescription)
        assertTrue("the node must present a real hit-rect to the platform", platform.hasNonEmptyBounds)
    }

    @Test
    fun `placement and inflation are computed in density-scaled pixels, not raw dp`() {
        // RI: this host runs at density 1.0, where every dp<->px conversion in the layer is the identity
        // — `toPx()` could be `.value` and a `.toDp()` could be missing without one test noticing. Forcing
        // density 2 makes the three conversions distinguishable. It also exercises the INFLATION path in
        // composition for the first time: at density 2 the 48dp floor is 96 px, so this 60 px node is
        // genuinely below it and must be opened up.
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                ZinelyV2CanvasSemantics(
                    nodes = listOf(node("a", "Title text")),
                    modifier = Modifier.size(200.dp),
                )
            }
        }
        val bounds = composeRule.onNodeWithTag("${ZinelyV2CanvasNodeTagPrefix}a")
            .fetchSemanticsNode().boundsInRoot
        // Corners span 0..60 px; the floor is 48.dp = 96 px at density 2, so the node inflates about its
        // centre (30, 30) to 96x96 — i.e. -18..78. A layer that treated the floor as 48 raw pixels, or
        // that forgot to convert its size back to dp, cannot produce this.
        assertEquals("width must be the density-scaled floor", 96f, bounds.width, 1f)
        assertEquals("height must be the density-scaled floor", 96f, bounds.height, 1f)
        assertEquals("inflation stays centred on the element", 30f, bounds.left + bounds.width / 2f, 1f)
        assertEquals("inflation stays centred on the element", 30f, bounds.top + bounds.height / 2f, 1f)
    }

    @Test
    fun `the whole layer is one traversal group, so the pinned order is honoured`() {
        composeRule.setContent {
            Box(Modifier.testTag("layer")) {
                ZinelyV2CanvasSemantics(
                    nodes = listOf(node("a", "Title text")),
                    modifier = Modifier.size(200.dp),
                )
            }
        }
        val group = composeRule.onNodeWithTag("layer").fetchSemanticsNode().children.first()
        assertTrue(
            "without isTraversalGroup the per-node traversalIndex values are not scoped and overlapping " +
                "bounds decide the reading order instead",
            group.config[SemanticsProperties.IsTraversalGroup],
        )
    }
}
