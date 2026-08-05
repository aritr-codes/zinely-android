package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.a11y.platformTraversalStops
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.feature.editor.BenchGridButtonTestTag
import com.aritr.zinely.feature.editor.BenchPageGridSurface
import com.aritr.zinely.feature.editor.BenchPageNav
import com.aritr.zinely.feature.editor.benchPageCellTag
import com.aritr.zinely.feature.editor.benchThumbTag
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * CI-29 (`stateDescription`) + CI-30 (`Role`) for the C5 page picker — the same two conformance
 * assertions the retired `EditorPageStripA11yTest` made, re-provided on the surface that replaced it
 * (ADR-095 row 5.9; C2b's rule that a parity phase never weakens a conformance path).
 *
 * The V1 strip's assertions were deleted with the V1 strip. That is only safe because they arrive here
 * unweakened: `Role.Tab`, both state lines, and — new in C5 — the frozen thumb label, which the V1 strip
 * did not have and which is the difference between "Page 3" and "Page 3 of 12 (back)".
 *
 * **Why `Role.Tab` is asserted on the merged tree, not the platform node.** `Role.Tab` never surfaces a
 * distinct `AccessibilityNodeInfo.className` — it rides the platform node's `roleDescription`, which the
 * CI-26 harness does not snapshot. So the merged-tree check is the authoritative one for the *role*.
 *
 * That is a narrow exemption, and it was read far too widely in the first cut of this file, which excused
 * the platform tree entirely on the grounds that each thumb 'merges child content'. It no longer does —
 * see the last two tests, which read the real `AccessibilityNodeInfo` and are the ones that would have
 * caught what Device Pass 1 caught.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BenchPageNavA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val pageSizePt = PtSize(100.0, 130.0)

    /** Four empty sheets — cover, two interiors, back — so every label branch has a page to speak for. */
    private fun renderNav(currentIndex: Int = 0) {
        val pages = (0 until 4).map {
            Page(
                index = it,
                // Every page is INTERIOR, exactly as the product builds them (`EditorBootstrap.kt:26`,
                // `RoomProjectRepository.kt:475`). These fixtures used to fabricate cover roles, which is
                // why the suite proved three frozen rows that never fired on a real document until Device
                // Pass 1 found them dead. Covers are a matter of POSITION now, per the freeze.
                role = PageRole.INTERIOR,
                elements = emptyList(),
            )
        }
        composeRule.setContent {
            ZinelyTheme {
                BenchPageNav(
                    pages = pages,
                    currentPageIndex = currentIndex,
                    pageSizePt = pageSizePt,
                    defaults = DocumentDefaults(),
                    onSelectPage = {},
                    onOpenGrid = {},
                )
            }
        }
    }

    /**
     * Both page pickers over the same four pages, in **one** composition.
     *
     * One composition because the property under test is an *equivalence* between them, and a rule can only
     * `setContent` once — but also because two separate renders would let the two surfaces be asserted in
     * two different tests, which is exactly how they drifted apart in the first place.
     */
    private fun renderBothPickers(currentIndex: Int = 0) {
        val pages = (0 until 4).map { Page(index = it, role = PageRole.INTERIOR, elements = emptyList()) }
        composeRule.setContent {
            ZinelyTheme {
                Column {
                    BenchPageNav(
                        pages = pages,
                        currentPageIndex = currentIndex,
                        pageSizePt = pageSizePt,
                        defaults = DocumentDefaults(),
                        onSelectPage = {},
                        onOpenGrid = {},
                    )
                    BenchPageGridSurface(
                        pages = pages,
                        currentPageIndex = currentIndex,
                        onSelectPage = {},
                        onDismiss = {},
                    )
                }
            }
        }
    }

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    @Test
    fun the_current_thumb_announces_current_page_and_the_others_announce_not_selected() {
        renderNav(currentIndex = 0)
        composeRule.onNodeWithTag(benchThumbTag(1)).assert(hasStateDescription("Current page"))
        composeRule.onNodeWithTag(benchThumbTag(2)).assert(hasStateDescription("Not selected"))
        composeRule.onNodeWithTag(benchThumbTag(4)).assert(hasStateDescription("Not selected"))
    }

    @Test
    fun each_thumb_carries_the_tab_role() {
        renderNav()
        composeRule.onNodeWithTag(benchThumbTag(1)).assert(hasRole(Role.Tab))
        composeRule.onNodeWithTag(benchThumbTag(3)).assert(hasRole(Role.Tab))
    }

    @Test
    fun a_thumb_says_which_page_it_is_out_of_how_many_and_names_the_two_covers() {
        renderNav()
        composeRule.onNodeWithTag(benchThumbTag(1))
            .assertContentDescriptionEquals("Page 1 of 4 (front cover)")
        composeRule.onNodeWithTag(benchThumbTag(2)).assertContentDescriptionEquals("Page 2 of 4")
        composeRule.onNodeWithTag(benchThumbTag(4))
            .assertContentDescriptionEquals("Page 4 of 4 (back)")
    }

    @Test
    fun the_grid_button_is_a_button_that_says_what_it_summons() {
        renderNav()
        composeRule.onNodeWithTag(BenchGridButtonTestTag)
            .assert(hasRole(Role.Button))
            .assertContentDescriptionEquals("All pages")
    }

    /**
     * **Each sheet is exactly ONE traversal stop, and it is named.**
     *
     * This is the assertion Device Pass 1 should have made. Dumping the platform tree and grepping for the
     * label finds Compose's *synthetic* content-description child — which reports `clickable="false"`,
     * `focusable="false"`, `selected="false"` for every merged control in the framework — and reading those
     * attributes as the sheet's produced two defects that did not exist. What matters is not how many nodes
     * Compose emits but where a service stops and what it says there, and that is what this measures.
     *
     * It still fails loudly on the thing worth fearing: a sheet that publishes two stops, or one with no
     * name.
     */
    @Test
    fun every_sheet_is_one_named_traversal_stop() {
        renderNav()
        val stops = platformTraversalStops(composeRule.activity)
        val sheets = stops.filter { it.label.startsWith("Page ") }
        assertEquals(
            "the strip published ${sheets.size} page stops for 4 sheets: ${sheets.map { it.label }}",
            4,
            sheets.size,
        )
        assertEquals(
            listOf(
                "Page 1 of 4 (front cover)",
                "Page 2 of 4",
                "Page 3 of 4",
                "Page 4 of 4 (back)",
            ),
            sheets.map { it.label },
        )
        assertTrue(
            "a sheet stop has no bounds a service could land on",
            sheets.all { it.boundsInScreen.height() > 0 },
        )
    }

    /**
     * D-009 on the platform, not in Compose's own units: a sheet drawn at 26×34dp must still be reported to
     * the accessibility framework with a hit-rect at least 48dp tall.
     *
     * Height only. Horizontally the expansion is clipped by the neighbouring sheets at a 7dp pitch — device
     * verification measured 33dp — and asserting a width the design cannot deliver would be a test written
     * against a wish.
     */
    @Test
    fun a_sheet_reports_a_forty_eight_dp_tall_hit_rect_to_the_platform() {
        renderNav()
        val node = composeRule.onNodeWithTag(benchThumbTag(2)).platformNode(composeRule.activity)
        val floor = with(composeRule.density) { 48.dp.toPx() }
        assertTrue(
            "the platform hit-rect is ${node.boundsInScreen.height()}px tall, under the ${floor}px floor",
            node.boundsInScreen.height() >= floor - 1f,
        )
    }

    /**
     * **The two page pickers tell the platform the same thing about which page you are on.**
     *
     * Device Pass 1 found that they did not. The strip's sheet is a `Role.Tab`, and Compose maps
     * `SemanticsProperties.Selected` through to `AccessibilityNodeInfo.isSelected` for that role; the grid's
     * cell is a `Role.Button`, where it does not — so every cell reported `selected=false`, the current one
     * included, while the 2dp matcha ring said otherwise on screen. Every merged-tree assertion passed.
     *
     * Read on the **platform** node, both surfaces, in one test: the equivalence is the property, and two
     * tests asserting one surface each would let them drift apart again.
     */
    @Test
    fun the_strip_and_the_grid_tell_the_platform_the_same_current_page() {
        renderBothPickers(currentIndex = 2)
        val currentSheet = composeRule.onNodeWithTag(benchThumbTag(3)).platformNode(composeRule.activity)
        val otherSheet = composeRule.onNodeWithTag(benchThumbTag(2)).platformNode(composeRule.activity)
        assertEquals(Copy.PageStrip.CURRENT_PAGE, currentSheet.stateDescription)
        assertEquals(Copy.PageStrip.NOT_SELECTED, otherSheet.stateDescription)

        val currentCell = composeRule.onNodeWithTag(benchPageCellTag(3)).platformNode(composeRule.activity)
        val otherCell = composeRule.onNodeWithTag(benchPageCellTag(2)).platformNode(composeRule.activity)
        assertEquals(
            "the grid's current cell does not tell the platform it is current — the ring is visual only",
            Copy.PageStrip.CURRENT_PAGE,
            currentCell.stateDescription,
        )
        assertEquals(Copy.PageStrip.NOT_SELECTED, otherCell.stateDescription)
        // And the pickers agree on the *name* too, so "the same page" is not two different sentences.
        assertEquals(currentSheet.contentDescription, currentCell.contentDescription)
    }
}
