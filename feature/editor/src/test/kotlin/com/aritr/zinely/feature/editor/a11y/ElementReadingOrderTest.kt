package com.aritr.zinely.feature.editor.a11y

import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.ReorderOp
import com.aritr.zinely.core.editor.ViewState
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.feature.editor.EditorEffectRunner
import com.aritr.zinely.feature.editor.EditorStore
import com.aritr.zinely.feature.editor.ElementNodeTagPrefix
import com.aritr.zinely.feature.editor.ElementSemanticsLayer
import com.aritr.zinely.ui.a11y.platformTraversalStops
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Brief 04 A1 / [ADR-119](../../../../../../../../../../../docs/DECISIONS.md#adr-119): the Bench's element
 * nodes are **declared** in spatial reading order on the platform tree, with a matching `traversalIndex`,
 * before and after a restack and after a spread — none of which may move a node.
 *
 * This proves the tree, not TalkBack's walk of it. Where TalkBack actually goes is the device listen.
 *
 * ## The spike (Brief 04 gate 1)
 *
 * Compose publishes `traversalBefore` hints only while accessibility is enabled — which is why
 * [platformTraversalStops]' KDoc measured them `UNDEFINED` here: a setup artefact of the Robolectric host,
 * not a property of the app. `ViewRootForTest.forceAccessibilityForTesting(true)` (present in the pinned
 * Compose) turns them on, and Compose mirrors each hint into the node's extras under
 * [TRAVERSAL_BEFORE_KEY] for tests to read. The last test asserts them.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class ElementReadingOrderTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val pageSize = PtSize(200.0, 300.0)

    private val title = TextElement(id = "t", transform = Transform(20.0, 20.0, 160.0, 30.0), zIndex = 2, text = "Title")
    private val caption = TextElement(id = "c", transform = Transform(100.0, 120.0, 80.0, 30.0), zIndex = 1, text = "Caption")
    private val photo = ImageElement(id = "p", transform = Transform(20.0, 200.0, 80.0, 60.0), zIndex = 0, assetId = "a".repeat(64))

    private val titleLabel = Copy.A11y.textLabel("Title")
    private val captionLabel = Copy.A11y.textLabel("Caption")

    /**
     * Page index 1 (the left of the 2 | 3 spread) carries the three elements **listed bottom-up**, so the
     * old list-order traversal (photo, caption, title) and the reading order (title, caption, photo) disagree.
     */
    private fun store(): EditorStore {
        val pages = List(8) { i ->
            Page(
                index = i,
                role = PageRole.INTERIOR,
                elements = if (i == 1) listOf(photo, caption, title) else emptyList(),
            )
        }
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        return EditorStore(
            EditorModel(
                document = ZineDocument(format = ZineFormat.SINGLE_SHEET_8, paperSize = PaperSize.LETTER, pages = pages),
                currentPageIndex = 1,
                view = ViewState(screenPxPerPt = 2f),
            ),
            CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, runner,
        )
    }

    private fun setLayer(store: EditorStore) {
        composeRule.setContent {
            val ui by store.uiState.collectAsState()
            ElementSemanticsLayer(
                uiState = ui,
                dispatch = store::dispatch,
                modifier = Modifier.fillMaxSize(),
                pageSizePt = pageSize,
            )
        }
        composeRule.waitForIdle()
    }

    /** The platform tree's stops, in declaration order, with each one's `traversalIndex` beside it. */
    private fun assertDeclaredInOrder(expected: List<String>, ids: List<String>) {
        composeRule.waitForIdle()
        assertEquals(
            "the platform tree must declare element nodes in reading order (§4.5 canvas clause, ADR-119)",
            expected,
            platformTraversalStops(composeRule.activity).map { it.label },
        )
        val indices = ids.map { id ->
            composeRule.onNodeWithTag("$ElementNodeTagPrefix$id").fetchSemanticsNode()
                .config[SemanticsProperties.TraversalIndex]
        }
        assertEquals("traversalIndex restates the declared order", ids.indices.map { it.toFloat() }, indices)
    }

    @Test
    fun `element nodes are declared in reading order, not list order`() {
        setLayer(store())
        assertDeclaredInOrder(listOf(titleLabel, captionLabel, Copy.A11y.PHOTO), listOf("t", "c", "p"))
    }

    @Test
    fun `a restack does not move a node`() {
        val store = store()
        setLayer(store)
        store.dispatch(Intent.Reorder("p", ReorderOp.TO_FRONT))
        store.dispatch(Intent.Reorder("t", ReorderOp.TO_BACK))
        assertDeclaredInOrder(listOf(titleLabel, captionLabel, Copy.A11y.PHOTO), listOf("t", "c", "p"))
    }

    @Test
    fun `after a spread the full-page half is read first, as the page's ground`() {
        val store = store()
        setLayer(store)
        store.dispatch(Intent.MakeImageSpread("p", 2.0, pageSize))
        // The source is now the full-page left half at the back of the stack. Taller than the §4.5 guard,
        // it is read first rather than opening a row that would swallow the title and caption.
        assertDeclaredInOrder(listOf(Copy.A11y.PHOTO, titleLabel, captionLabel), listOf("p", "t", "c"))
    }

    @Test
    fun `with accessibility on, traversalBefore hints chain the nodes in the same order`() {
        setLayer(store())
        val root = findComposeView(composeRule.activity.window.decorView)
        // Route 2 alone makes Compose send events the platform then refuses ("Accessibility off"), so the
        // platform manager is switched on too (route 1). Neither route alone is enough on this host.
        val manager = composeRule.activity.getSystemService(AccessibilityManager::class.java)
        composeRule.runOnUiThread { shadowOf(manager).setEnabled(true) }
        (root as ViewRootForTest).forceAccessibilityForTesting(true)
        composeRule.waitForIdle()
        val provider = root.accessibilityNodeProvider!!

        val ids = listOf("t", "c", "p").map { composeRule.onNodeWithTag("$ElementNodeTagPrefix$it").fetchSemanticsNode().id }
        // Each node is hinted to come before its successor in reading order.
        ids.zipWithNext().forEach { (node, next) ->
            val info = provider.createAccessibilityNodeInfo(node)!!
            assertTrue(
                "node $node carries no traversalBefore hint (extras: ${info.extras.keySet()})",
                info.extras.containsKey(TRAVERSAL_BEFORE_KEY),
            )
            assertEquals("node $node must be hinted before $next", next, info.extras.getInt(TRAVERSAL_BEFORE_KEY))
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
}
