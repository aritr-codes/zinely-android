package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
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

    // ── AW6.4 / AW6.12 the chooser ────────────────────────────────────────────────────────────────────

    private var picked: PaperColour? = null

    private fun setChooser(selected: PaperColour) {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.size(430.dp, 932.dp)) {
                    PaperColourOptions(selected = selected, onPick = { picked = it })
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `the chooser is one radio group of seven papers, light to dark, the current one checked`() {
        setChooser(PaperColour.PASTEL_YELLOW)

        val group = composeRule.onNodeWithTag(PaperColourGroupTestTag).fetchSemanticsNode()
        assertTrue("the rows are not one selectable group", SemanticsProperties.SelectableGroup in group.config)
        val tops = PaperColour.entries.map { colour ->
            val node = composeRule.onNodeWithTag(paperColourOptionTestTag(colour)).fetchSemanticsNode()
            assertEquals("${colour.id} is not a radio", Role.RadioButton, node.config.getOrNull(SemanticsProperties.Role))
            assertEquals(colour == PaperColour.PASTEL_YELLOW, node.config.getOrNull(SemanticsProperties.Selected))
            node.boundsInRoot.top
        }
        assertEquals("AW6.5's order", tops.sorted(), tops)
        assertEquals(
            listOf("White", "Ivory", "Pastel yellow", "Pastel pink", "Pastel blue", "Pastel green", "Grey"),
            PaperColour.entries.map { it.displayName },
        )
    }

    @Test
    fun `each row is one node named by its colour, and only White carries a note`() {
        setChooser(PaperColour.WHITE)

        composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.WHITE))
            .assertContentDescriptionEquals("White. ${Copy.PaperColour.WHITE_NOTE}")
        composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.GREY)).assertContentDescriptionEquals("Grey")
        composeRule.onNodeWithText(Copy.PaperColour.FOOT).assertExists()
    }

    @Test
    fun `one tap picks`() {
        setChooser(PaperColour.WHITE)
        composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.PASTEL_BLUE)).performClick()
        assertEquals(PaperColour.PASTEL_BLUE, picked)
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
            "${Copy.Paper.LEGAL}, ${Copy.Paper.LEGAL_DIMENSIONS_SPOKEN}. ${Copy.Paper.printsOnlyOn(Copy.Paper.LEGAL)}",
        )
        val node = fact.fetchSemanticsNode()
        assertNull("the fact is a control", node.config.getOrNull(SemanticsProperties.Role))
        assertTrue("the fact has no click", node.config.none { it.key.name == "OnClick" })
    }

    @Test
    fun `an A5 zine shows its paper as a fact too, and an A4 zine keeps the switch`() {
        setPanel(PaperSize.A5)
        composeRule.onNodeWithTag(ProofPaperSegmentsTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ProofFixedPaperTestTag).assertContentDescriptionEquals(
            "${Copy.Paper.A5}, ${Copy.Paper.A5_DIMENSIONS_SPOKEN}. ${Copy.Paper.printsOnlyOn(Copy.Paper.A5)}",
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
        composeRule.onNodeWithText(Copy.PaperColour.loadNote("pastel yellow")).assertExists()
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
    fun `picking a colour returns to Print details and keeps a saved PDF`() {
        val saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
        setProof(saved = saved)
        saved.tryEmit("zine.pdf")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ProofReadyTestTag).performClick()
        composeRule.onNodeWithTag(ProofPaperColourRowTestTag).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.PASTEL_GREEN)).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(1, colourPicks)
        // Back on Print details, the row names the pick.
        composeRule.onNodeWithTag(ProofPaperColourRowTestTag).assertContentDescriptionEquals("Paper colour, Pastel green")
        composeRule.onNodeWithTag(ZSheetCloseTestTag).performClick()
        composeRule.waitForIdle()
        // AW6-P.7: the colour is not in the PDF, so the save stands and nothing calls it stale.
        composeRule.onNodeWithTag(ProofDoneTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ProofPaperChangedTestTag).assertDoesNotExist()
    }

    @Test
    fun `re-picking the current colour changes nothing`() {
        setProof(start = PaperColour.IVORY, startDrawer = ProofDrawer.PaperColour)
        composeRule.onNodeWithTag(paperColourOptionTestTag(PaperColour.IVORY)).assertIsSelected().performClick()
        composeRule.waitForIdle()
        assertEquals(0, colourPicks)
        composeRule.onNodeWithTag(ProofPaperColourRowTestTag).assertExists()
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
