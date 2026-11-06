package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.data.repository.PaperColour
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.components.ZSheetCloseTestTag
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * AW6 and AW6-P at the Proof, and A35.8: the chooser's radio group, Print details' colour row, its note and recipe,
 * the band's "not in the file" line, and a US Legal or A5 zine's Print with no switch.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class PaperColourScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val pages = (0 until 8).map { Page(index = it, role = if (it == 0) PageRole.FRONT_COVER else PageRole.INTERIOR) }

    // ── A56 the pile ──────────────────────────────────────────────────────────────────────────────────

    private var picked: PaperColour? = null
    private var done = 0

    private fun setChooser(selected: PaperColour) {
        composeRule.setContent {
            var current by remember { mutableStateOf(selected) }
            ZinelyTheme {
                Box(Modifier.size(430.dp, 932.dp)) {
                    PaperColourSheetSurface(
                        selected = current,
                        onPick = { picked = it; current = it },
                        onDone = { done++ },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `given the pile, then it is one radio group of ten papers in A56_3's order, the current one checked`() {
        setChooser(PaperColour.PASTEL_YELLOW)

        val group = composeRule.onNodeWithTag(PaperColourGroupTestTag).fetchSemanticsNode()
        assertTrue("the sheets are not one selectable group", SemanticsProperties.SelectableGroup in group.config)
        assertEquals(10, group.config[SemanticsProperties.CollectionInfo].rowCount)
        val places = PaperColour.entries.mapIndexed { i, colour ->
            val node = composeRule.onNodeWithTag(paperColourOptionTestTag(colour)).fetchSemanticsNode()
            assertEquals("${colour.id} is not a radio", Role.RadioButton, node.config.getOrNull(SemanticsProperties.Role))
            assertEquals(colour == PaperColour.PASTEL_YELLOW, node.config.getOrNull(SemanticsProperties.Selected))
            assertEquals(i, node.config[SemanticsProperties.CollectionItemInfo].rowIndex)
            node.boundsInRoot.top to node.boundsInRoot.left
        }
        // Two even rows of five, read row by row, left to right.
        assertEquals(2, places.map { it.first }.distinct().size)
        assertEquals(places.sortedWith(compareBy({ it.first }, { it.second })), places)
        assertEquals(
            listOf(
                "White", "Ivory", "Pastel yellow", "Pastel orange", "Pastel pink", "Pastel purple", "Pastel blue",
                "Pastel green", "Light brown", "Grey",
            ),
            PaperColour.entries.map { it.displayName },
        )
    }

    @Test
    fun `given the pile, then each sheet is one node named by its colour, and only White carries a note`() {
        setChooser(PaperColour.WHITE)

        composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.WHITE))
            .assertContentDescriptionEquals("White. ${Copy.PaperColour.WHITE_NOTE}")
        composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.GREY)).assertContentDescriptionEquals("Grey")
        composeRule.onNodeWithText(Copy.PaperColour.FOOT).assertExists()
        composeRule.onNodeWithText(Copy.PaperColour.SUB).assertExists()
    }

    /** A56.5: a tap changes the colour at once and does NOT close; only Done does. The label follows the tap. */
    @Test
    fun `given the pile, when a sheet is tapped, then it is picked and the sheet stays open, and Done closes`() {
        setChooser(PaperColour.WHITE)
        assertEquals("A sample page on white paper, plain printer paper", sampleLabelFor(PaperColour.WHITE))

        composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.PASTEL_BLUE)).performClick()
        composeRule.waitForIdle()

        assertEquals(PaperColour.PASTEL_BLUE, picked)
        assertEquals(0, done)
        composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.PASTEL_BLUE)).assertIsSelected()
        assertEquals("A sample page on pastel blue paper", sampleLabelFor(PaperColour.PASTEL_BLUE))

        composeRule.onNodeWithTag(PaperColourDoneTestTag).assertContentDescriptionEquals("Done").performClick()
        assertEquals(1, done)
    }

    @Test
    fun `given the pile, when the current sheet is tapped again, then nothing is picked`() {
        setChooser(PaperColour.IVORY)
        composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.IVORY)).performClick()
        assertNull(picked)
    }

    /** A56.6 / A56.7: the sample page and its label are hidden; Done comes before the sheets, the foot after them. */
    @Test
    fun `given the pile, then the sample page and its label are hidden from TalkBack and Done comes first`() {
        setChooser(PaperColour.PASTEL_PINK)
        listOf(PaperColourSampleTestTag, PaperColourSampleLabelTestTag).forEach { tag ->
            val config = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().config
            assertTrue("$tag speaks", config.none { e -> e.key == SemanticsProperties.ContentDescription || e.key == SemanticsProperties.Text })
        }
        composeRule.onNodeWithText(sampleLabelFor(PaperColour.PASTEL_PINK)).assertDoesNotExist()
        val done = composeRule.onNodeWithTag(PaperColourDoneTestTag).fetchSemanticsNode().boundsInRoot
        val first = composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.WHITE)).fetchSemanticsNode().boundsInRoot
        val foot = composeRule.onNodeWithText(Copy.PaperColour.FOOT).fetchSemanticsNode().boundsInRoot
        assertTrue("Done is not above the pile", done.bottom <= first.top)
        assertTrue("the foot is not under the pile", foot.top >= first.bottom)
    }

    /**
     * AW6.12 against the tree a service reads, not the merged one: each row reaches the platform checkable, and
     * only the current paper checked. (ProofPaperSegmentsA11yTest is why this is measured, not assumed.)
     */
    @Test
    fun `the platform is told which paper is checked`() {
        setChooser(PaperColour.PASTEL_YELLOW)

        val yellow = composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.PASTEL_YELLOW))
            .platformNode(composeRule.activity)
        val white = composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.WHITE))
            .platformNode(composeRule.activity)

        assertTrue("a row is not checkable on the platform", yellow.isCheckable && white.isCheckable)
        assertTrue("the current paper is not checked on the platform", yellow.isChecked)
        assertFalse("another paper reads as checked", white.isChecked)
        assertEquals("android.widget.RadioButton", yellow.className)
        assertEquals("Pastel yellow", yellow.contentDescription)
    }

    // ── A35.8 the fixed paper ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `switchable is A4 and US Letter only`() {
        assertEquals(setOf(PaperSize.A4, PaperSize.LETTER), PaperSize.entries.filter { it.switchable }.toSet())
    }

    private fun setPanel(paper: PaperSize, colour: PaperColour = PaperColour.WHITE, onOpen: () -> Unit = {}) {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.size(430.dp, 932.dp)) {
                    ProofPrintDetailsPanel(
                        paper = paper, onPaperSelected = {}, onOpenFold = {}, paperColour = colour,
                        onOpenPaperColour = onOpen,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `a US Legal zine shows its paper as one fact and no switch`() {
        setPanel(PaperSize.LEGAL)

        composeRule.onNodeWithTag(ProofPaperSegmentsTestTag).assertDoesNotExist()
        val fact = composeRule.onNodeWithTag(ProofFixedPaperTestTag).assertIsDisplayed()
        fact.assertContentDescriptionEquals(
            "US Legal, long paper, 8.5 by 14 inches. You made this zine for US Legal paper. Use US Legal paper only. " +
                "Put US Legal paper in your printer before you print.",
        )
        composeRule.onNodeWithText("US LEGAL", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(Copy.Paper.LEGAL_PLAIN, useUnmergedTree = true).assertExists()
        val node = fact.fetchSemanticsNode()
        assertNull("the fact is a control", node.config.getOrNull(SemanticsProperties.Role))
        assertTrue("the fact has no click", node.config.none { it.key.name == "OnClick" })
    }

    @Test
    fun `an A5 zine shows its paper as a fact too, and an A4 zine keeps the switch`() {
        setPanel(PaperSize.A5)
        composeRule.onNodeWithTag(ProofPaperSegmentsTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ProofFixedPaperTestTag).assertContentDescriptionEquals(
            "A5, small paper, 148 by 210 millimetres. You made this zine for A5 paper. Use A5 paper only. " +
                "Put A5 paper in your printer before you print.",
        )
    }

    @Test
    fun `an A4 zine keeps the switch`() {
        setPanel(PaperSize.A4)
        composeRule.onNodeWithTag(ProofPaperSegmentsTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofFixedPaperTestTag).assertDoesNotExist()
    }

    // ── AW6-P.2 to P.4 Print details ──────────────────────────────────────────────────────────────────

    @Test
    fun `the colour row is one button naming the colour, and opens the chooser`() {
        var opened = 0
        setPanel(PaperSize.A4, PaperColour.PASTEL_YELLOW) { opened++ }

        composeRule.onNodeWithTag(ProofPaperColourRowTestTag)
            .assertContentDescriptionEquals("Paper colour, Pastel yellow")
            .performScrollTo()
            .performClick()
        assertEquals(1, opened)
    }

    @Test
    fun `a coloured paper says what to load and names both in the recipe`() {
        setPanel(PaperSize.A4, PaperColour.PASTEL_YELLOW)

        composeRule.onNodeWithTag(ProofPaperColourNoteTestTag)
            .assertExists()
        composeRule.onNodeWithText(
            "Put pastel yellow paper in your printer. The PDF has only the ink, so the colour comes from your paper.",
        ).assertExists()
        composeRule.onNodeWithText("A4, pastel yellow paper").assertExists()
    }

    @Test
    fun `White shows no note and the recipe is unchanged`() {
        setPanel(PaperSize.A4, PaperColour.WHITE)

        composeRule.onNodeWithTag(ProofPaperColourNoteTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ProofPaperColourRowTestTag).assertContentDescriptionEquals("Paper colour, White")
        composeRule.onNodeWithText("A4, white paper").assertDoesNotExist()
    }

    // ── The Proof: the chooser's door, the band, and saving ───────────────────────────────────────────

    private var colourPicks = 0
    private lateinit var inputMode: InputModeManager

    private fun setProof(
        paper: PaperSize = PaperSize.A4,
        start: PaperColour = PaperColour.WHITE,
        saved: MutableSharedFlow<String> = MutableSharedFlow(extraBufferCapacity = 1),
        startDrawer: ProofDrawer = ProofDrawer.None,
    ) {
        composeRule.setContent {
            var colour by remember { mutableStateOf(start) }
            var size by remember { mutableStateOf(paper) }
            ZinelyTheme {
                inputMode = LocalInputModeManager.current
                ProofScreen(
                    onBack = {},
                    paper = size,
                    onPaperSelected = { size = it },
                    savedSignals = saved,
                    pages = pages,
                    startDrawer = startDrawer,
                    paperColour = colour,
                    onPaperColourSelected = { colourPicks++; colour = it },
                )
            }
        }
        composeRule.waitForIdle()
    }

    /** AW6.12 at Print details' door: told the chooser has closed, the panel focuses its Paper colour row, once. */
    @Test
    fun `given the chooser has closed, when Print details comes back, then the Paper colour row has focus`() {
        var focusRow by mutableStateOf(false)
        var cleared = 0
        val requester = FocusRequester()
        composeRule.setContent {
            ZinelyTheme {
                inputMode = LocalInputModeManager.current
                ProofPrintDetailsPanel(
                    paper = PaperSize.A4,
                    onPaperSelected = {},
                    onOpenFold = {},
                    paperColour = PaperColour.PASTEL_GREEN,
                    paperColourFocus = requester,
                    focusColourRow = focusRow,
                    onColourRowFocused = { cleared++; focusRow = false },
                )
            }
        }
        composeRule.runOnUiThread { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ProofPaperColourRowTestTag).assertIsNotFocused()

        focusRow = true
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofPaperColourRowTestTag).assertIsFocused()
        assertEquals(1, cleared)
        assertFalse(focusRow)
    }

    @Test
    fun `the not-in-file line shows for a coloured paper only, before and after a save`() {
        val saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
        setProof(start = PaperColour.PASTEL_PINK, saved = saved)
        composeRule.onNodeWithTag(ProofNotInFileTestTag).assertIsDisplayed()
        composeRule.onNodeWithText(Copy.PaperColour.NOT_IN_FILE).assertIsDisplayed()

        saved.tryEmit("zine.pdf")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ProofDoneTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofNotInFileTestTag).assertIsDisplayed()
    }

    @Test
    fun `a White zine shows nothing new in the band`() {
        setProof(start = PaperColour.WHITE)
        composeRule.onNodeWithTag(ProofNotInFileTestTag).assertDoesNotExist()
    }

    @Test
    fun `given Print details' door, when a colour is picked, then the chooser stays, and Done returns to the row and keeps a saved PDF`() {
        val saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
        setProof(saved = saved)
        saved.tryEmit("zine.pdf")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofReadyTestTag).performClick()
        composeRule.onNodeWithTag(ProofPaperColourRowTestTag).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.PASTEL_GREEN)).performClick()
        composeRule.waitForIdle()

        assertEquals(1, colourPicks)
        // A56.5: the chooser stays open on the pick.
        composeRule.onNodeWithTag(PaperColourSheetTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.PASTEL_GREEN)).assertIsSelected()
        composeRule.onNodeWithTag(PaperColourDoneTestTag).performClick()
        composeRule.waitForIdle()
        // Back on Print details, the row names the pick. Its focus (AW6.12) is pinned by the panel test below:
        // Robolectric opens Print details' re-raised drawer window in touch mode whatever the test asks, and in
        // touch mode Compose moves no focus, so it cannot be asserted through the whole screen.
        composeRule.onNodeWithTag(PaperColourSheetTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ProofPaperColourRowTestTag).assertContentDescriptionEquals("Paper colour, Pastel green")
        composeRule.onNodeWithTag(ZSheetCloseTestTag).performClick()
        composeRule.waitForIdle()
        // AW6-P.7: the colour is not in the PDF, so the save stands and nothing calls it stale.
        composeRule.onNodeWithTag(ProofDoneTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofPaperChangedTestTag).assertDoesNotExist()
    }

    @Test
    fun `given Print details' door, when the current colour is tapped again, then nothing changes and the chooser stays`() {
        setProof(start = PaperColour.IVORY, startDrawer = ProofDrawer.PaperColour)
        composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.IVORY)).assertIsSelected().performClick()
        composeRule.waitForIdle()
        assertEquals(0, colourPicks)
        composeRule.onNodeWithTag(PaperColourSheetTestTag).assertIsDisplayed()
    }

    @Test
    fun `dismissing the chooser returns to Print details unchanged`() {
        setProof(start = PaperColour.IVORY, startDrawer = ProofDrawer.PaperColour)
        composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.WHITE)).assertIsNotSelected()
        org.robolectric.shadows.ShadowDialog.getLatestDialog()?.onBackPressed()
        composeRule.waitForIdle()
        assertEquals(0, colourPicks)
        composeRule.onNodeWithTag(ProofPaperColourRowTestTag).assertContentDescriptionEquals("Paper colour, Ivory")
    }

    /**
     * The ADR-150 "As built" blocker: on a US Legal zine nothing at Print may count as a change of paper. There is
     * no switch to tap, and opening and closing Print details leaves a saved PDF standing.
     */
    @Test
    fun `a US Legal zine's Print has no switch and keeps its saved PDF`() {
        val saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
        setProof(paper = PaperSize.LEGAL, saved = saved)
        saved.tryEmit("zine.pdf")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofReadyTestTag).performClick()
        composeRule.onNodeWithTag(ProofPaperSegmentsTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ProofFixedPaperTestTag).assertIsDisplayed()
        composeRule.onNode(hasText(Copy.Paper.LETTER) or hasContentDescription(Copy.Paper.LETTER)).assertDoesNotExist()
        composeRule.onNodeWithTag(ZSheetCloseTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofDoneTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofPaperChangedTestTag).assertDoesNotExist()
    }

    @Test
    fun `the colour row is reachable from the band in one step`() {
        setProof()
        composeRule.onNodeWithTag(ProofReadyTestTag).performClick()
        composeRule.onNodeWithContentDescription("Paper colour, White").assertExists()
    }

    /** A35.8's handler guard, every pair: only A4 and US Letter change places; a Legal or A5 zine never moves. */
    @Test
    fun `only a switch between A4 and US Letter counts as a paper change`() {
        for (current in PaperSize.entries) for (picked in PaperSize.entries) {
            val expected = current != picked && current in setOf(PaperSize.A4, PaperSize.LETTER) &&
                picked in setOf(PaperSize.A4, PaperSize.LETTER)
            assertEquals("$current -> $picked", expected, isPaperChange(current, picked))
        }
    }
}
