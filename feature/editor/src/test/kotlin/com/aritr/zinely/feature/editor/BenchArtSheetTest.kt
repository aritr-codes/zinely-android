package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.render.SupplyCatalog
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Focused behavior contract for the A16 searchable Art cabinet. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h812dp-xhdpi")
class BenchArtSheetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var picked = mutableListOf<String>()

    private fun render(recent: List<String> = emptyList()) {
        composeRule.setContent {
            ZinelyTheme {
                BenchArtSheet(visible = true, onDismiss = {}, onPick = { picked += it }, recent = recent)
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun all_thirty_two_supplies_and_the_four_family_filters_are_present() {
        render()
        Copy.Supplies.NAMES.keys.forEach { id ->
            composeRule.onNodeWithTag(benchArtTileTestTag(id)).assertExists()
        }
        Copy.Supplies.BY_FAMILY.keys.forEach { family ->
            composeRule.onNodeWithTag(benchArtFamilyFilterTestTag(family)).assertExists()
            composeRule.onNodeWithTag(benchArtLabelTestTag(family)).assertExists()
        }
        assertEquals(32, Copy.Supplies.NAMES.size)
    }

    @Test
    fun name_and_tag_search_is_local_deterministic_and_resettable() {
        render()
        composeRule.onNodeWithTag(BenchArtSearchTestTag).performTextInput("manicule")
        composeRule.onNodeWithTag(BenchArtResultCountTestTag).assertTextEquals("1 piece")
        composeRule.onNodeWithTag(benchArtTileTestTag("mark.hand")).assertExists()
        composeRule.onNodeWithTag(benchArtTileTestTag("shape.circle")).assertDoesNotExist()

        composeRule.onNodeWithTag(BenchArtSearchTestTag).performTextInput(" no-such-piece")
        composeRule.onNodeWithTag(BenchArtNoResultsTestTag).assertExists()
        composeRule.onNodeWithTag(BenchArtShowAllTestTag).performClick()
        composeRule.onNodeWithTag(BenchArtResultCountTestTag).assertTextEquals("32 pieces")
        composeRule.onNodeWithTag(benchArtTileTestTag("shape.circle")).assertExists()
    }

    @Test
    fun a_family_filter_toggles_off_and_never_reorders_the_catalogue() {
        render()
        val family = Copy.Supplies.CUT_SHAPES
        composeRule.onNodeWithTag(benchArtFamilyFilterTestTag(family)).performScrollTo().performClick()
        composeRule.onNodeWithTag(BenchArtResultCountTestTag).assertTextEquals("5 pieces")
        composeRule.onNodeWithTag(benchArtTileTestTag("shape.rect")).assertExists()
        composeRule.onNodeWithTag(benchArtTileTestTag("tape.torn")).assertDoesNotExist()

        composeRule.onNodeWithTag(benchArtFamilyFilterTestTag(family)).performScrollTo().performClick()
        composeRule.onNodeWithTag(BenchArtResultCountTestTag).assertTextEquals("32 pieces")
        composeRule.onNodeWithTag(benchArtTileTestTag("tape.torn")).assertExists()
    }

    @Test
    fun favourite_is_a_separate_action_and_places_a_copy_on_the_favourites_rail() {
        var favourites by mutableStateOf(emptySet<String>())
        composeRule.setContent {
            ZinelyTheme {
                BenchArtSheetBody(
                    onPick = { picked += it },
                    favourites = favourites,
                    onFavouriteChange = { id, on -> favourites = if (on) favourites + id else favourites - id },
                )
            }
        }
        composeRule.onNodeWithTag(benchArtFavouriteTestTag("shape.circle")).performScrollTo().performClick()
        assertEquals(emptyList<String>(), picked)
        composeRule.onNodeWithTag(benchArtFavouriteTileTestTag("shape.circle")).assertExists()
        composeRule.onNodeWithTag(benchArtTileTestTag("shape.circle")).assertExists()
    }

    @Test
    fun recent_is_a_second_shelf_and_picking_an_authored_supply_reports_its_id() {
        render(recent = listOf("shape.circle"))
        composeRule.onNodeWithTag(benchArtRecentTileTestTag("shape.circle")).assertExists()
        composeRule.onNodeWithTag(benchArtTileTestTag("shape.circle")).assertExists().performScrollTo().performClick()
        assertEquals(listOf("shape.circle"), picked)
    }

    @Test
    fun the_ui_and_shared_renderer_catalogues_have_exactly_the_same_ids() {
        render()
        Copy.Supplies.NAMES.keys.forEach { id ->
            composeRule.onNodeWithTag(benchArtTileTestTag(id)).performScrollTo().performClick()
        }
        assertEquals(Copy.Supplies.NAMES.keys, SupplyCatalog.OUTLINES.keys)
        assertEquals(Copy.Supplies.NAMES.keys.toList(), picked)
    }
}
