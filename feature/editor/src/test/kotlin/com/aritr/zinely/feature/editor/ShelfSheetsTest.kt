package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
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
