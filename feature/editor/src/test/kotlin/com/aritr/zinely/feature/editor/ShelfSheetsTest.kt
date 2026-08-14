package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The three frozen Shelf sheets, composed from the M1 component library. */
@RunWith(RobolectricTestRunner::class)
class ShelfSheetsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val card = HomeZineCard(
        id = "z1",
        title = "Corner Store Poems",
        formatLabel = "8-page mini · A4",
        editedLabel = "Edited yesterday",
    )

    /** `.papers` lists A4 first: the frozen markup's order is the recommendation. */
    @Test
    fun `the create sheet offers A4 before Letter, and choosing one starts the zine`() {
        var chosen: PaperSize? = null
        setContent { ShelfCreateSheet(visible = true, onDismiss = {}, onChoosePaper = { chosen = it }) }

        assertEquals(listOf(PaperSize.A4, PaperSize.LETTER), ShelfPaperChoices)
        composeRule.onNodeWithTag(homePaperChoiceTestTag(PaperSize.A4)).performClick()
        assertEquals(PaperSize.A4, chosen)
    }

    @Test
    fun `the create sheet names each stock and its real dimensions`() {
        setContent { ShelfCreateSheet(visible = true, onDismiss = {}, onChoosePaper = {}) }
        composeRule.onNodeWithText("210 × 297 mm").assertExists()
        composeRule.onNodeWithText("8.5 × 11 in").assertExists()
    }

    /**
     * The chooser's one job is to let you compare two sheets, so both are drawn at **one** scale.
     * Letter's frozen `56×72` was ~4% oversized, which inverted the relation: it drew the physically
     * smaller sheet as the larger one. These four assertions pin the fix from four directions — the
     * shared scale, each stock's own proportion, the relation between them, and the un-inverted area.
     */
    @Test
    fun `both stocks are drawn from one common scale`() {
        setContent { ShelfCreateSheet(visible = true, onDismiss = {}, onChoosePaper = {}) }

        val a4 = stockOf(PaperSize.A4)
        val letter = stockOf(PaperSize.LETTER)

        // The scale each stock was actually drawn at, read back off the laid-out node, against the
        // nominal one — never against another measurement, which would spend the budget twice: the
        // two stocks round in opposite directions (A4's width down 0.6%, Letter's up 0.4%).
        val nominal = 74.0 / 841.890
        val scales = listOf(
            a4.width / PaperSize.A4.portrait.width,
            a4.height / PaperSize.A4.portrait.height,
            letter.width / PaperSize.LETTER.portrait.width,
            letter.height / PaperSize.LETTER.portrait.height,
        )
        // One scale, to within the whole-dp rounding the spec's whole pixels also carry: ±0.5dp on
        // the shortest edge (52dp) is ±0.96%, and the worst real case here is 0.62%. A per-stock
        // literal 4% out — the defect — is well outside it.
        scales.forEach { assertEquals(nominal, it, nominal * 0.01) }
    }

    @Test
    fun `each stock keeps its real proportion, and Letter is the wider, shorter sheet`() {
        setContent { ShelfCreateSheet(visible = true, onDismiss = {}, onChoosePaper = {}) }

        val a4 = stockOf(PaperSize.A4)
        val letter = stockOf(PaperSize.LETTER)

        assertEquals(841.890 / 595.276, a4.height / a4.width, 0.02)
        assertEquals(792.0 / 612.0, letter.height / letter.width, 0.02)
        assertTrue("Letter is the wider sheet", letter.width > a4.width)
        assertTrue("Letter is the shorter sheet", letter.height < a4.height)
    }

    /** The defect stated as its consequence: A4 has the greater area, and must be drawn that way. */
    @Test
    fun `the drawn areas are no longer inverted`() {
        setContent { ShelfCreateSheet(visible = true, onDismiss = {}, onChoosePaper = {}) }

        val a4 = stockOf(PaperSize.A4)
        val letter = stockOf(PaperSize.LETTER)
        val trueRatio = (595.276 * 841.890) / (612.0 * 792.0)   // 1.0339 — A4 is the larger sheet
        val drawnRatio = (a4.width * a4.height) / (letter.width * letter.height)

        assertTrue("A4 must be drawn larger than Letter, was $drawnRatio", drawnRatio > 1.0)
        assertEquals(trueRatio, drawnRatio, 0.03)
    }

    /**
     * The stocks differ in height by 4dp, so each sits in a slot as tall as the tallest. Without it
     * the two tiles' name and dimension rows would sit at different heights — a comparison aid that
     * makes the two sides harder to compare.
     */
    @Test
    fun `the two tiles stay the same height, so their labels stay level`() {
        setContent { ShelfCreateSheet(visible = true, onDismiss = {}, onChoosePaper = {}) }

        val a4 = boundsOf(homePaperChoiceTestTag(PaperSize.A4))
        val letter = boundsOf(homePaperChoiceTestTag(PaperSize.LETTER))
        assertEquals(a4.height.value.toDouble(), letter.height.value.toDouble(), 0.5)
        assertEquals(a4.top.value.toDouble(), letter.top.value.toDouble(), 0.5)
    }

    private fun stockOf(paper: PaperSize) = boundsOf(homePaperStockTestTag(paper)).let {
        DrawnStock(width = it.width.value.toDouble(), height = it.height.value.toDouble())
    }

    private fun boundsOf(tag: String) =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private data class DrawnStock(val width: Double, val height: Double)

    /** The rename field is revealed by the Rename item, never stacked as a second dialog. */
    @Test
    fun `rename reveals an inline field whose save carries the edited name`() {
        var renamed: Pair<String, String>? = null
        setContent { actionSheet(onRename = { id, title -> renamed = id to title }) }

        composeRule.onNodeWithTag(HomeRenameFieldTestTag).assertDoesNotExist()
        composeRule.onNodeWithText("Rename").performClick()

        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextClearance()
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextInput("Tuesday")
        composeRule.onNodeWithTag(HomeRenameConfirmTestTag).performClick()

        assertEquals("z1" to "Tuesday", renamed)
    }

    /** `if(z && v)` — an emptied field is not a rename; the sheet closes and the name survives. */
    @Test
    fun `a name emptied to nothing is never committed`() {
        var renamed: Pair<String, String>? = null
        setContent { actionSheet(onRename = { id, title -> renamed = id to title }) }

        composeRule.onNodeWithText("Rename").performClick()
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextClearance()
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextInput("   ")
        composeRule.onNodeWithTag(HomeRenameConfirmTestTag).performClick()

        assertEquals(null, renamed)
    }

    @Test
    fun `a name is committed trimmed`() {
        var renamed: Pair<String, String>? = null
        setContent { actionSheet(onRename = { id, title -> renamed = id to title }) }

        composeRule.onNodeWithText("Rename").performClick()
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextClearance()
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextInput("  Tuesday  ")
        composeRule.onNodeWithTag(HomeRenameConfirmTestTag).performClick()

        assertEquals("z1" to "Tuesday", renamed)
    }

    /**
     * The rename field is no longer `ZTextField` — when it was split off, that component painted `--field`
     * under a coral focus border, which the V2.1 sheets cannot wear and cannot recolour from outside.
     * (`ZTextField` has since been converged onto `.search input` — paper under an ink border — so the
     * original reason no longer holds; the split stands on the sheet's own face, not on that.) Its **keyboard
     * behaviour is the part that must not change**, and the Done key is the half a re-implementation
     * silently drops: it is invisible in a screenshot, it has no test tag of its own, and the Save button
     * beside it keeps working, so the loss reads as nothing at all until someone types a name and
     * presses the only key their thumb is already on.
     */
    @Test
    fun `the rename field keeps its Done key, and Done commits the trimmed name`() {
        var renamed: String? = null
        setContent {
            ShelfRenameSheet(
                visible = true,
                title = card.title,
                onDismiss = {},
                onRename = { renamed = it },
            )
        }

        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextClearance()
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextInput("  Tuesday  ")
        // Throws if no IME action is set on the field — which is the assertion.
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performImeAction()

        assertEquals("Tuesday", renamed)
    }

    @Test
    fun `the action sheet's destructive item reports the zine it will delete`() {
        var deleted: String? = null
        setContent { actionSheet(onDelete = { deleted = it }) }
        composeRule.onNodeWithText("Delete").performClick()
        assertEquals("z1", deleted)
    }

    /**
     * The spec's `Share…` item is deliberately absent — nothing in `ProjectRepository` backs it, and
     * a menu item that does nothing is worse than an honest omission.
     */
    @Test
    fun `the action sheet ships no share item it cannot honour`() {
        setContent { actionSheet() }
        composeRule.onAllNodesWithText("Share…").assertCountEquals(0)
    }

    @Test
    fun `the sort sheet checks exactly the chosen option and reports a change`() {
        var picked: ShelfSort? = null
        setContent {
            ShelfSortSheet(visible = true, selected = ShelfSort.Recent, onDismiss = {}, onSelect = { picked = it })
        }

        composeRule.onNodeWithText(ShelfSort.Recent.menuLabel).assertIsSelected()
        composeRule.onNodeWithText(ShelfSort.Name.menuLabel).assertIsNotSelectedNode()
        composeRule.onNodeWithText(ShelfSort.Oldest.menuLabel).performClick()
        assertEquals(ShelfSort.Oldest, picked)
    }

    /** `assertIsNotSelected` needs the property present; a radio always carries it. */
    private fun SemanticsNodeInteraction.assertIsNotSelectedNode() = apply {
        assertEquals(false, fetchSemanticsNode().config[SemanticsProperties.Selected])
    }

    @Composable
    private fun actionSheet(
        onOpen: (String) -> Unit = {},
        onRename: (String, String) -> Unit = { _, _ -> },
        onDuplicate: (String) -> Unit = {},
        onDelete: (String) -> Unit = {},
    ) = ShelfActionSheet(
        visible = true,
        card = card,
        onDismiss = {},
        onOpen = onOpen,
        onRename = onRename,
        onDuplicate = onDuplicate,
        onDelete = onDelete,
    )

    private fun setContent(content: @Composable () -> Unit) = composeRule.setContent {
        ZinelyTheme {
            // The sheet slides in on `--base`; reduced motion lands it before the first assertion.
            CompositionLocalProvider(LocalZinelyMotion provides ZinelyMotion(reduceMotion = true)) {
                content()
            }
        }
    }
}
