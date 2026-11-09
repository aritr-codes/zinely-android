package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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

    /** A35.2 / A35.3: four rows, the preferred paper first, then A4, US Letter, US Legal, A5; one tap starts the zine. */
    @Test
    fun `the start sheet offers the four papers and choosing one starts the zine`() {
        var chosen: PaperSize? = null
        setContent { ShelfCreateSheet(visible = true, onDismiss = {}, onChoosePaper = { chosen = it }) }

        assertEquals(listOf(PaperSize.A4, PaperSize.LETTER, PaperSize.LEGAL, PaperSize.A5), ShelfPaperChoices)
        PaperSize.entries.forEach { composeRule.onNodeWithTag(homePaperChoiceTestTag(it)).assertExists() }
        composeRule.onNodeWithTag(homePaperChoiceTestTag(PaperSize.LEGAL)).performClick()
        assertEquals(PaperSize.LEGAL, chosen)
    }

    @Test
    fun `the start sheet leads with the saved preferred paper, then the fixed order`() {
        assertEquals(listOf(PaperSize.LETTER, PaperSize.A4, PaperSize.LEGAL, PaperSize.A5), shelfPaperChoices(PaperSize.LETTER))
        assertEquals(listOf(PaperSize.A5, PaperSize.A4, PaperSize.LETTER, PaperSize.LEGAL), shelfPaperChoices(PaperSize.A5))
    }

    @Test
    fun `every paper size has its own name and dimensions, never US Letter's (ADR-150 decision 9)`() {
        assertEquals("US Legal", PaperSize.LEGAL.choiceName)
        assertEquals("8.5 × 14 in", PaperSize.LEGAL.choiceDimensions)
        assertEquals("A5", PaperSize.A5.choiceName)
        assertEquals("148 × 210 mm", PaperSize.A5.choiceDimensions)
        assertEquals(PaperSize.entries.size, PaperSize.entries.map { it.choiceName }.toSet().size)
    }

    /** A55.4: each row is ONE node, role button, named by its words in the order of the screen, units in full. */
    @Test
    fun `given the Start sheet, then each row is one button named as A55_4 says`() {
        setContent { ShelfCreateSheet(visible = true, onDismiss = {}, onChoosePaper = {}) }
        val legal = composeRule.onNodeWithTag(homePaperChoiceTestTag(PaperSize.LEGAL)).fetchSemanticsNode().config
        assertEquals(
            listOf(
                "US Legal, long paper, 8.5 by 14 inches, pages 3.5 by 4.25 inches. Use US Legal paper only. " +
                    "Make sure that your printer can use it.",
            ),
            legal[SemanticsProperties.ContentDescription],
        )
        assertEquals(Role.Button, legal[SemanticsProperties.Role])
        // A35.11: no "selected", no position: the rows are buttons, not a list of choices.
        assertTrue(SemanticsProperties.Selected !in legal)
        val a4 = composeRule.onNodeWithTag(homePaperChoiceTestTag(PaperSize.A4)).fetchSemanticsNode().config
        assertEquals(
            listOf("A4, printer paper, most countries, 210 by 297 millimetres, pages 74 by 105 millimetres."),
            a4[SemanticsProperties.ContentDescription],
        )
    }

    /** A55.1: the stamp and the plain name, the sizes line, and the warning for US Legal and A5 only. */
    @Test
    fun `given the Start sheet, then each row shows its stamp, plain name, sizes and only the Legal and A5 warning`() {
        setContent { ShelfCreateSheet(visible = true, onDismiss = {}, onChoosePaper = {}) }
        listOf("A4", "US LETTER", "US LEGAL", "A5").forEach {
            composeRule.onNodeWithText(it, useUnmergedTree = true).assertExists()
        }
        listOf(
            com.aritr.zinely.core.copy.Copy.Paper.A4_PLAIN, com.aritr.zinely.core.copy.Copy.Paper.LETTER_PLAIN,
            com.aritr.zinely.core.copy.Copy.Paper.LEGAL_PLAIN, com.aritr.zinely.core.copy.Copy.Paper.A5_PLAIN,
            com.aritr.zinely.core.copy.Copy.Paper.A4_SIZES, com.aritr.zinely.core.copy.Copy.Paper.LETTER_SIZES,
            com.aritr.zinely.core.copy.Copy.Paper.LEGAL_SIZES, com.aritr.zinely.core.copy.Copy.Paper.A5_SIZES,
            "Use US Legal paper only. Make sure that your printer can use it.",
            "Use A5 paper only. Make sure that your printer can use it.",
        ).forEach { composeRule.onNodeWithText(it, useUnmergedTree = true).assertExists() }
        composeRule.onAllNodesWithText("Use A4 paper only.", substring = true, useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("Use US Letter paper only.", substring = true, useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onNodeWithText(
            "Choose the paper that is in your printer. You cannot change the paper of a zine after you start it.",
        ).assertExists()
        composeRule.onNodeWithText("Zinely makes eight pages from one folded sheet.", useUnmergedTree = true).assertExists()
    }

    /**
     * A35.4: ONE scale for all four, re-based to 56dp on US Legal's 1008pt so the tallest fits. These pin the scale,
     * each stock's own proportion and the relations the drawing exists to show.
     */
    @Test
    fun `all four stocks are drawn from one common scale`() {
        setContent { ShelfCreateSheet(visible = true, onDismiss = {}, onChoosePaper = {}) }
        val nominal = 56.0 / 1008.0
        PaperSize.entries.forEach { paper ->
            val s = stockOf(paper)
            // Whole-dp rounding on a 23dp edge is up to ±2.2%; a stock on another scale is far outside it.
            assertEquals("$paper width", nominal, s.width / paper.portrait.width, nominal * 0.025)
            assertEquals("$paper height", nominal, s.height / paper.portrait.height, nominal * 0.025)
        }
        assertEquals(56.0, stockOf(PaperSize.LEGAL).height, 0.5)
    }

    @Test
    fun `A4 stays the taller, narrower sheet and A5 the smallest`() {
        setContent { ShelfCreateSheet(visible = true, onDismiss = {}, onChoosePaper = {}) }
        val a4 = stockOf(PaperSize.A4)
        val letter = stockOf(PaperSize.LETTER)
        val legal = stockOf(PaperSize.LEGAL)
        val a5 = stockOf(PaperSize.A5)
        assertTrue("Letter is the wider sheet", letter.width > a4.width)
        assertTrue("A4 is the taller sheet", a4.height > letter.height)
        assertTrue("Legal is the tallest", legal.height > a4.height)
        assertTrue("A5 is the smallest", a5.width * a5.height < letter.width * letter.height)
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

    /** #157: the cursor used to land at the start, so typing prepended to the old name. */
    @Test
    fun `the action sheet's rename opens with the whole title selected`() {
        setContent { actionSheet() }
        composeRule.onNodeWithText("Rename").performClick()
        assertEquals(TextRange(0, card.title.length), selectionOfRenameField())
    }

    @Test
    fun `the rename sheet opens with the whole title selected, so typing replaces it`() {
        var renamed: String? = null
        setContent { ShelfRenameSheet(visible = true, title = card.title, onDismiss = {}, onRename = { renamed = it }) }
        assertEquals(TextRange(0, card.title.length), selectionOfRenameField())

        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextInput("Tuesday")
        composeRule.onNodeWithTag(HomeRenameConfirmTestTag).performClick()
        assertEquals("Tuesday", renamed)
    }

    @Test
    fun `a title is capped at 60 characters`() {
        var renamed: String? = null
        setContent { ShelfRenameSheet(visible = true, title = card.title, onDismiss = {}, onRename = { renamed = it }) }

        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextInput("a".repeat(75))
        composeRule.onNodeWithTag(HomeRenameConfirmTestTag).performClick()
        assertEquals("a".repeat(60), renamed)
    }

    @Test
    fun `the cap falls on a code point, never inside a surrogate pair`() {
        var renamed: String? = null
        setContent { ShelfRenameSheet(visible = true, title = card.title, onDismiss = {}, onRename = { renamed = it }) }

        val smile = "😀"
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextInput("a".repeat(58) + smile.repeat(3))
        composeRule.onNodeWithTag(HomeRenameConfirmTestTag).performClick()
        assertEquals("a".repeat(58) + smile.repeat(2), renamed)
    }

    @Test
    fun `capTitle leaves a short title alone and never splits a pair`() {
        assertEquals("Tuesday", capTitle("Tuesday"))
        val smile = "😀"
        assertEquals("a".repeat(59) + smile, capTitle("a".repeat(59) + smile + smile))
        assertEquals("a".repeat(60), capTitle("a".repeat(61)))
    }

    /** Device defect: delete one letter, type one back, and the last letter was cut. */
    @Test
    fun `an over-long title survives delete-then-retype as two separate edits`() {
        val title = "b".repeat(59) + " copy"
        assertEquals(64, cps(title))
        var renamed: String? = null
        setContent { ShelfRenameSheet(visible = true, title = title, onDismiss = {}, onRename = { renamed = it }) }

        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextReplacement(title.dropLast(1))
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextInput("y")
        composeRule.onNodeWithTag(HomeRenameConfirmTestTag).performClick()
        assertEquals(title, renamed)
    }

    @Test
    fun `an over-long title still cannot grow past its opening length`() {
        val title = "b".repeat(59) + " copy"
        var renamed: String? = null
        setContent { ShelfRenameSheet(visible = true, title = title, onDismiss = {}, onRename = { renamed = it }) }

        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextReplacement(title)
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextInput("zz")
        composeRule.onNodeWithTag(HomeRenameConfirmTestTag).performClick()
        assertEquals(64, cps(renamed!!))
    }

    private fun cps(s: String) = s.codePointCount(0, s.length)

    /** R1: a duplicate's " copy" can leave a title over 60; editing it must not eat the tail. */
    @Test
    fun `editing one letter of an already-long title keeps its tail and length`() {
        val old = "a".repeat(60) + " copy"
        assertEquals(65, cps(old))
        val edited = old.replaceRange(10, 11, "b")
        val out = titleEdit(old, TextFieldValue(edited, TextRange(11)))
        assertEquals(edited, out.text)
        assertEquals(TextRange(11), out.selection)
    }

    @Test
    fun `an edit may not grow a long title past where it already was`() {
        val old = "a".repeat(65)
        assertEquals(65, cps(titleEdit(old, TextFieldValue(old + "zzz")).text))
    }

    /** I1: truncating mid-text must not throw the cursor to the end. */
    @Test
    fun `a capped edit keeps the cursor clamped, not jumped to the end`() {
        val old = "a".repeat(60)
        val out = titleEdit(old, TextFieldValue("a".repeat(30) + "XY" + "a".repeat(30), TextRange(32)))
        assertEquals(60, cps(out.text))
        assertEquals(TextRange(32), out.selection)
    }

    /** I2: the cut falls on a grapheme-cluster boundary. */
    @Test
    fun `the cap never splits a ZWJ emoji, a flag or a combining mark`() {
        val family = "👨‍👩‍👧"   // 5 code points, 1 cluster
        val flag = "🇺🇸"                              // 2 code points, 1 cluster
        val accent = "é"                                             // 2 code points, 1 cluster
        assertEquals("a".repeat(58), capTitle("a".repeat(58) + family))
        assertEquals("a".repeat(59), capTitle("a".repeat(59) + flag))
        assertEquals("a".repeat(59), capTitle("a".repeat(59) + accent))
        assertEquals("a".repeat(58) + accent, capTitle("a".repeat(58) + accent + "b"))
    }

    private fun selectionOfRenameField(): TextRange =
        composeRule.onNodeWithTag(HomeRenameFieldTestTag)
            .fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]

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
