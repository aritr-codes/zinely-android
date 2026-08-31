package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.render.SupplyCatalog
import com.aritr.zinely.feature.editor.BenchArtSearchTestTag
import com.aritr.zinely.feature.editor.BenchArtSheetBody
import com.aritr.zinely.feature.editor.BenchArtSheetTestTag
import com.aritr.zinely.feature.editor.benchArtFavouriteTestTag
import com.aritr.zinely.feature.editor.benchArtFavouriteTileTestTag
import com.aritr.zinely.feature.editor.benchArtFamilyFilterTestTag
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
 * The Art sheet's thirty-two tiles on the **platform** accessibility tree — the tier CI-26's `platformNode`
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
 *   all thirty-two authored tiles are checked on the real node — `isEnabled = true` and `isClickable = true`,
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
            var favourites by remember { mutableStateOf(emptySet<String>()) }
            ZinelyTheme {
                BenchArtSheetBody(
                    onPick = {},
                    favourites = favourites,
                    onFavouriteChange = { id, on ->
                        favourites = if (on) favourites + id else favourites - id
                    },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun reveal(tag: String) {
        composeRule.onNodeWithTag(BenchArtSheetTestTag).performScrollToNode(hasTestTag(tag))
        composeRule.waitForIdle()
    }

    private fun tile(supplyId: String) =
        benchArtTileTestTag(supplyId).also(::reveal).let { tag ->
            composeRule.onNodeWithTag(tag).platformNode(composeRule.activity)
        }

    @Test
    fun every_tile_carries_its_name_and_live_action_state_to_the_platform() {
        render()
        for ((id, name) in Copy.Supplies.NAMES) {
            val node = tile(id)
            assertEquals("$id must be announced by its name, never by its id", name, node.contentDescription?.toString())
            assertEquals("$id must reach the platform as a button", "android.widget.Button", node.className)
            assertTrue("$id has an authored outline and must be enabled to the platform", node.isEnabled)
            assertTrue("$id must be activatable by an accessibility service, not only by touch", node.isClickable)
            // A live control has no reason to report state, and a stale one here would read as an
            // explanation of an absence that does not exist.
            assertNull("$id is live and must not claim to be unavailable", node.stateDescription)
        }
        assertEquals(32, SupplyCatalog.OUTLINES.size)
    }

    @Test
    fun search_and_favourite_controls_expose_their_label_and_state_to_the_platform() {
        render()
        val search = composeRule.onNodeWithTag(BenchArtSearchTestTag).platformNode(composeRule.activity)
        assertEquals("android.widget.EditText", search.className)
        assertTrue(search.isEnabled)
        composeRule.onNodeWithTag(BenchArtSearchTestTag)
            .assertContentDescriptionEquals(Copy.BenchArt.FIND_A_PIECE)

        reveal(benchArtTileTestTag("tape.torn"))
        val favourite = composeRule.onNodeWithTag(benchArtFavouriteTestTag("tape.torn"))
        val unchecked = favourite.platformNode(composeRule.activity)
        assertEquals("android.widget.CheckBox", unchecked.className)
        assertTrue(unchecked.isCheckable)
        assertTrue(!unchecked.isChecked)
        favourite.performClick()
        composeRule.waitForIdle()
        val checkedCopies = composeRule.onAllNodesWithTag(benchArtFavouriteTestTag("tape.torn"))
        assertEquals(2, checkedCopies.fetchSemanticsNodes().size)
        assertTrue(
            checkedCopies
                .onFirst()
                .platformNode(composeRule.activity)
                .isChecked,
        )
    }

    @Test
    fun family_filter_exposes_name_selection_and_a_full_touch_target_to_the_platform() {
        render()
        val family = Copy.Supplies.CUT_SHAPES
        val control = composeRule.onNodeWithTag(benchArtFamilyFilterTestTag(family)).performScrollTo()
        val unselected = control.platformNode(composeRule.activity)
        val targetFloor = with(composeRule.density) { 48.dp.toPx() }

        assertEquals(family, unselected.contentDescription?.toString())
        assertTrue(unselected.isEnabled && unselected.isClickable)
        assertTrue(!unselected.isSelected)
        assertEquals("Not selected", unselected.stateDescription?.toString())
        assertTrue(unselected.boundsInScreen.height() >= targetFloor - 1f)

        control.performClick()
        composeRule.waitForIdle()
        assertEquals(
            "Selected",
            composeRule.onNodeWithTag(benchArtFamilyFilterTestTag(family))
                .platformNode(composeRule.activity)
                .stateDescription
                ?.toString(),
        )
    }

}
