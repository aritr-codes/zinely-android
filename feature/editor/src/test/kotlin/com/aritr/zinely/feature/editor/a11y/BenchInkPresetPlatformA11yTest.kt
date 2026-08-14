package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.feature.editor.BenchContextBarTestTag
import com.aritr.zinely.feature.editor.BenchInkPresetTestTag
import com.aritr.zinely.feature.editor.BenchVerbKind
import com.aritr.zinely.feature.editor.benchContextVerbs
import com.aritr.zinely.feature.editor.EditorEffectRunner
import com.aritr.zinely.feature.editor.EditorScreen
import com.aritr.zinely.feature.editor.EditorStore
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The ink popover's three preset pills on the **platform** accessibility tree.
 *
 * ### Why this is not a merged-tree assertion
 *
 * The pill sets its name with `clearAndSetSemantics`, which sits *below* its `clickable`. A merged-tree
 * `assertHasClickAction` passes either way — measured, by deleting the pill's semantics `onClick` and
 * watching the assertion stay green on both the merged and the unmerged tree. So a Compose-level probe
 * cannot see this defect at all, and writing one would have shipped a test that cannot fail for the right
 * reason. `AccessibilityNodeInfo` is the only reader that agrees with TalkBack, and it is what
 * [platformNode] returns — the same instrument [ReframeControlsRolePlatformA11yTest] and
 * [BenchStyleRowPlatformA11yTest] exist for.
 *
 * What is asserted: the pill reaches the platform as an enabled, **clickable** button. Without that, the
 * pill is reachable by a finger and by nothing else — no Switch Access, no external keyboard, no TalkBack
 * double-tap — and, since the haptics pass, no feedback on any of those routes either.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// The popover does not fit Robolectric's default 320x470dp surface; this is the frozen `.phone`, and the
// same qualifier BenchC6Test states its reason for.
@Config(qualifiers = "w411dp-h891dp")
class BenchInkPresetPlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    /** Places one text box, leaving it selected and the context bar up. */
    private fun selectText() {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        val store = EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
                ),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(
                    store = store,
                    pageSizePt = PtSize(100.0, 130.0),
                    modifier = Modifier.size(360.dp, 720.dp),
                )
            }
        }
        composeRule.waitForIdle()
        store.dispatch(Intent.PlaceText(Transform(20.0, 60.0, 60.0, 18.0), "hi"))
        composeRule.waitForIdle()
    }

    /** …and then opens the ink popover on it, which stands the context bar down. */
    private fun openInk() {
        selectText()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.INK}").performClick()
        composeRule.waitForIdle()
    }

    /**
     * The same question asked of the *other* control this file's note points at.
     *
     * [BenchContextBar]'s KDoc states, as fact, that `clearAndSetSemantics` wiped its verbs'
     * `ACTION_CLICK` and that `uiautomator dump` would have read `clickable="false"` on all seven. The
     * modifier order there is the same as the preset pill's, so both claims cannot be true. Measured here
     * with the instrument rather than argued: the verbs reach the platform clickable. What is genuinely
     * load-bearing in that block is the `disabled()` half — a disabled verb is given no `clickable` at
     * all, which is why the inert ones report unclickable, and that is asserted below too.
     */
    @Test
    fun the_context_bars_verbs_reach_the_platform_the_same_way() {
        selectText()
        for (verb in benchContextVerbs(BenchVerbKind.TEXT)) {
            val node = composeRule.onNodeWithTag("$BenchContextBarTestTag-${verb.label}")
                .platformNode(composeRule.activity)
            assertEquals(
                "${verb.label} must be offered as activatable exactly when it is live",
                verb.enabled,
                node.isClickable,
            )
        }
    }

    @Test
    fun every_preset_reaches_the_platform_as_an_activatable_button() {
        openInk()
        val pills = composeRule.onAllNodesWithTag(BenchInkPresetTestTag)
        assertEquals("the three frozen recipes", 3, pills.fetchSemanticsNodes().size)
        repeat(3) { i ->
            val node = pills[i].platformNode(composeRule.activity)
            assertEquals("preset $i must carry Role.Button to the platform", "android.widget.Button", node.className)
            assertTrue("preset $i must be enabled to the platform", node.isEnabled)
            assertTrue(
                "preset $i must be activatable by an accessibility service, not only by touch",
                node.isClickable,
            )
        }
    }
}
