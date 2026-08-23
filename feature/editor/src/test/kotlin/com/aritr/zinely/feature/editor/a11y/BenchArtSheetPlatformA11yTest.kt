package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.render.SupplyCatalog
import com.aritr.zinely.feature.editor.BenchArtSheetBody
import com.aritr.zinely.feature.editor.benchArtTileTestTag
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Art sheet's sixteen tiles on the **platform** accessibility tree — the tier CI-26's `platformNode`
 * exists for.
 *
 * Two defects this file is aimed at, both already shipped once in this repository:
 *
 * - **A tile that announces nothing.** A5 records the frozen tile as an unlabelled `<button>` wrapping a
 *   bare `<svg>`; SUPPLIES-SPEC §8 makes the supply's own name the naming contract. Here the name is
 *   asserted to *arrive* — `Copy.Supplies.NAMES`, never a `supplyId`, and never a family derived from an id
 *   prefix (five prefixes, four families; TalkBack shipped saying *"Rect shape"* for exactly that).
 * - **A control that tells Compose it is disabled and the platform it is not.** `ReframeControls.ZoomButton`
 *   passed `assertIsNotEnabled` against the merged tree while reporting `enabled` to
 *   `AccessibilityNodeInfo` (ADR-058). A merged-tree assertion cannot catch that by construction, so the
 *   all sixteen authored tiles are checked on the real node — `isEnabled = true` and `isClickable = true`,
 *   because `disabled()` alone still leaves a node a service may offer to activate.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h812dp-xhdpi")
class BenchArtSheetPlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Composed WITHOUT the `Dialog`, deliberately: `platformNode` resolves against the activity's own view
     * tree, and a `Dialog` puts the content in a second window the harness does not read.
     */
    private fun render() {
        composeRule.setContent {
            ZinelyTheme { BenchArtSheetBody(onPick = {}) }
        }
        composeRule.waitForIdle()
    }

    private fun tile(supplyId: String) =
        composeRule.onNodeWithTag(benchArtTileTestTag(supplyId)).platformNode(composeRule.activity)

    @Test
    fun every_tile_carries_its_own_spoken_name_to_the_platform() {
        render()
        for ((id, name) in Copy.Supplies.NAMES) {
            assertEquals("$id must be announced by its name, never by its id", name, tile(id).contentDescription?.toString())
            assertEquals("$id must reach the platform as a button", "android.widget.Button", tile(id).className)
        }
    }

    @Test
    fun all_sixteen_are_enabled_and_activatable_by_a_service() {
        render()
        assertEquals(16, SupplyCatalog.OUTLINES.size)
        for (id in Copy.Supplies.NAMES.keys) {
            val node = tile(id)
            assertTrue("$id has an authored outline and must be enabled to the platform", node.isEnabled)
            assertTrue("$id must be activatable by an accessibility service, not only by touch", node.isClickable)
            // A live control has no reason to report state, and a stale one here would read as an
            // explanation of an absence that does not exist.
            assertNull("$id is live and must not claim to be unavailable", node.stateDescription)
        }
    }

}
