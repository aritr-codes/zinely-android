package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.DocumentVoice
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Font row (frozen `v21-typebar.html` A29; ADR-126) end-to-end against a **real** [EditorStore], and
 * read in the **platform** accessibility tree wherever the claim is about what a service receives.
 *
 * [TypeBarTest]'s harness and tier (Robolectric NATIVE, the whole [EditorScreen], the bench viewport).
 * The row's pure state is `FontRowTest`'s; what is asserted here is that the card shows, says and commits
 * that state. Given-When-Then.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class TypeBarFontRowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val announced = mutableListOf<String>()
    private val latin = "Saturday, and nothing planned"
    private val greek = "Saturday. Καλημέρα"
    private val min = DocumentVoice.BOOK_MIN_SIZE_PT.toInt()

    private val bookNoGreek = "Book has no Greek letters, so this text stays Plain."
    private val bookNeedsSize = "Book needs $min pt or larger. Make the text larger first."
    private val bookStops = "Book stops at $min pt. Switch to Plain to go smaller."
    private val unknownFont = "This text uses a font this version of Zinely does not have. It is drawn in Plain for now. " +
        "Choosing Book or Plain replaces its font."

    /** One selected text. A style that differs from the default is written before the screen exists. */
    private fun storeWith(text: String = latin, family: String? = null, sizePt: Double? = null): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        val s = EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
                ),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
        s.dispatch(Intent.PlaceText(Transform(40.0, 40.0, 20.0, 20.0), text))
        if (family != null || sizePt != null) {
            s.dispatch(Intent.StyleText(textOf(s).id, sizePt = sizePt, fontFamily = family))
        }
        return s
    }

    private fun textOf(s: EditorStore): TextElement =
        s.uiState.value.document.pages[0].elements.first { it is TextElement } as TextElement

    private fun open(s: EditorStore, fontScale: Float = 1f) {
        announced.clear()
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                ZinelyTheme {
                    EditorScreen(
                        store = s,
                        pageSizePt = PtSize(300.0, 300.0),
                        imageBytes = reframeTestPhoto(),
                        onStyleAnnounce = { announced += it },
                    )
                }
            }
        }
        composeRule.onNodeWithContentDescription("Text style").performClick()
        composeRule.waitForIdle()
    }

    private fun book() = composeRule.onNodeWithTag("type-bar-font-Book")
    private fun plain() = composeRule.onNodeWithTag("type-bar-font-Plain")
    private fun reason() = composeRule.onNodeWithTag(TypeBarFontReasonTestTag)
    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    // ── A29 rules 1, 6: a named single-choice group that commits ──────────────────────────────────

    @Test
    fun the_row_is_a_group_named_Font_holding_two_radio_words_and_a_new_text_is_Plain() {
        open(storeWith())

        val group = composeRule.onNodeWithTag(TypeBarFontGroupTestTag).fetchSemanticsNode().config
        assertEquals(listOf("Font"), group.getOrNull(SemanticsProperties.ContentDescription))
        assertTrue("the Font row is not a selectable group", group.contains(SemanticsProperties.SelectableGroup))

        book().assert(hasRole(Role.RadioButton)).assertIsNotSelected()
        plain().assert(hasRole(Role.RadioButton)).assertIsSelected()
        assertEquals("Book", book().platformNode(composeRule.activity).contentDescription)
        assertEquals("Plain", plain().platformNode(composeRule.activity).contentDescription)
        // The state reaches a service, not only the merged tree.
        assertEquals("Selected", plain().platformNode(composeRule.activity).stateDescription)
        assertEquals("Not selected", book().platformNode(composeRule.activity).stateDescription)
        reason().assertDoesNotExist()
        // The chosen word carries the corner tick; the other does not (rule 4).
        composeRule.onNodeWithTag(selectionCueTag("type-bar-font-Plain"), useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(selectionCueTag("type-bar-font-Book"), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun choosing_Book_changes_only_the_font_as_one_undo_step_and_says_so() {
        val s = storeWith()
        val before = textOf(s)
        open(s)

        book().performClick()
        composeRule.waitForIdle()

        assertEquals(before.copy(style = before.style.copy(fontFamily = "Fraunces")), textOf(s))
        assertEquals(listOf("Font Book"), announced)
        book().assertIsSelected()
        plain().assertIsNotSelected()

        // The word already chosen does nothing and adds no step: one Undo is back at the start.
        book().performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("Font Book"), announced)
        s.dispatch(Intent.Undo)
        composeRule.waitForIdle()
        assertEquals(before, textOf(s))
        plain().assertIsSelected()
    }

    @Test
    fun a_text_returned_to_Plain_is_the_text_that_was_never_changed() {
        val s = storeWith()
        val before = textOf(s)
        open(s)

        book().performClick()
        plain().performClick()
        composeRule.waitForIdle()

        assertEquals(before, textOf(s))
        assertEquals(listOf("Font Book", "Font Plain"), announced)
    }

    @Test
    fun a_size_burst_still_settling_is_committed_first_so_the_font_is_its_own_undo_step() {
        val s = storeWith()
        open(s)

        composeRule.onNodeWithContentDescription("Larger").performClick()
        book().performClick()
        composeRule.waitForIdle()

        assertEquals(TextStyle(sizePt = 14.0, fontFamily = "Fraunces"), textOf(s).style)
        s.dispatch(Intent.Undo)
        composeRule.waitForIdle()
        assertEquals(TextStyle(sizePt = 14.0), textOf(s).style)
        s.dispatch(Intent.Undo)
        composeRule.waitForIdle()
        assertEquals(TextStyle(), textOf(s).style)
    }

    // ── A29 rule 7: a word that cannot be used is not a disabled control ──────────────────────────

    @Test
    fun an_unavailable_Book_is_enabled_clickable_and_not_chosen_to_the_platform_and_says_why() {
        val s = storeWith(greek)
        val before = textOf(s)
        open(s)

        val node = book().platformNode(composeRule.activity)
        assertTrue("an unavailable Book must stay enabled to the platform", node.isEnabled)
        assertTrue("an unavailable Book must stay clickable to the platform", node.isClickable)
        assertFalse(node.isSelected)
        assertFalse(node.isChecked)
        assertEquals("Book, unavailable. $bookNoGreek", node.contentDescription)
        book().assertIsEnabled()
        // The reason is on screen without a tap, and it is the same sentence.
        reason().assertIsDisplayed().assertTextEquals(bookNoGreek)

        book().performClick()
        composeRule.waitForIdle()

        assertEquals("a tap on an unavailable Book must change nothing", before, textOf(s))
        assertEquals(listOf("Book, unavailable. $bookNoGreek"), announced)
        plain().assertIsSelected()
    }

    @Test
    fun the_one_reason_line_shows_the_unknown_font_first_then_the_script_then_the_size() {
        open(storeWith(greek, family = "Averia Sans Libre", sizePt = 10.0))
        reason().assertTextEquals(unknownFont)
        // Book's own reason is still what Book says when reached.
        assertEquals("Book, unavailable. $bookNoGreek", book().platformNode(composeRule.activity).contentDescription)
    }

    @Test
    fun the_script_outranks_the_size_on_the_reason_line() {
        open(storeWith(greek, sizePt = 10.0))
        reason().assertTextEquals(bookNoGreek)
    }

    @Test
    fun a_Plain_text_under_Books_minimum_is_told_to_grow_and_Book_opens_when_it_does() {
        val s = storeWith(sizePt = 10.0)
        open(s)
        reason().assertTextEquals(bookNeedsSize)
        assertEquals("Book, unavailable. $bookNeedsSize", book().platformNode(composeRule.activity).contentDescription)

        // The row follows the size the READOUT shows, not the size still waiting to be committed.
        composeRule.onNodeWithContentDescription("Larger").performClick()
        composeRule.waitForIdle()

        reason().assertDoesNotExist()
        assertEquals("Book", book().platformNode(composeRule.activity).contentDescription)
        book().performClick()
        composeRule.waitForIdle()
        assertEquals(TextStyle(sizePt = min.toDouble(), fontFamily = "Fraunces"), textOf(s).style)
    }

    // ── A29 rule 10: a font this build does not know ──────────────────────────────────────────────

    @Test
    fun an_unknown_font_shows_neither_word_chosen_is_kept_until_one_is_and_undo_puts_it_back() {
        val s = storeWith(family = "Averia Sans Libre")
        open(s)

        book().assertIsNotSelected()
        plain().assertIsNotSelected()
        reason().assertTextEquals(unknownFont)
        // Plain is a real choice here: it replaces the kept family, so it is clickable, unlike a chosen radio.
        assertTrue(plain().platformNode(composeRule.activity).isClickable)

        composeRule.onNodeWithContentDescription("Bold").performClick()
        composeRule.waitForIdle()
        assertEquals("Averia Sans Libre", textOf(s).style.fontFamily)

        plain().performClick()
        composeRule.waitForIdle()
        assertEquals("sans-serif", textOf(s).style.fontFamily)
        reason().assertDoesNotExist()

        s.dispatch(Intent.Undo)
        composeRule.waitForIdle()
        assertEquals("Averia Sans Libre", textOf(s).style.fontFamily)
        reason().assertTextEquals(unknownFont)
    }

    // ── A29 rule 9: Smaller at Book's smallest size ───────────────────────────────────────────────

    @Test
    fun Smaller_on_a_Book_text_at_Books_minimum_stays_reachable_changes_nothing_and_says_why() {
        val s = storeWith(family = "Fraunces")
        assertEquals("the default size is Book's minimum", min.toDouble(), textOf(s).style.sizePt, 0.0)
        open(s)

        val smaller = composeRule.onNodeWithTag("type-bar-Smaller")
        val node = smaller.platformNode(composeRule.activity)
        assertTrue("Smaller at Book's floor must stay enabled to the platform", node.isEnabled)
        assertTrue(node.isClickable)
        assertEquals("Smaller, unavailable. $bookStops", node.contentDescription)
        reason().assertTextEquals(bookStops)

        smaller.performClick()
        composeRule.waitForIdle()
        // No settle to wait for: nothing was scheduled. The readout has not moved either.
        composeRule.onNodeWithContentDescription("Size $min point").assertExists()
        assertEquals(min.toDouble(), textOf(s).style.sizePt, 0.0)
        assertEquals(listOf("Smaller, unavailable. $bookStops"), announced)

        // The way out the line names works: Plain, and Smaller is an ordinary button again.
        plain().performClick()
        composeRule.waitForIdle()
        reason().assertDoesNotExist()
        assertEquals("Smaller", smaller.platformNode(composeRule.activity).contentDescription)
    }

    // ── R24: targets, width, and the card at the largest text ─────────────────────────────────────

    @Test
    fun both_font_words_report_a_full_48dp_target_to_the_platform_in_every_state() {
        fun assertTargets(state: String) = listOf("Book", "Plain").forEach { word ->
            val b = composeRule.onNodeWithTag("type-bar-font-$word").platformNode(composeRule.activity).boundsInScreen
            with(composeRule.density) {
                assertTrue(
                    "$word ($state) reports ${b.width().toDp()} x ${b.height().toDp()} to the platform tree",
                    b.width().toDp() >= 47.9.dp && b.height().toDp() >= 47.9.dp,
                )
            }
        }
        open(storeWith())
        assertTargets("Plain chosen")
        // The rows around it keep theirs with a fifth row and a reason line in the card.
        composeRule.onNodeWithContentDescription("Smaller").assertExists()
    }

    @Test
    fun the_unavailable_and_unknown_states_keep_the_48dp_targets() {
        open(storeWith(greek, family = "Averia Sans Libre"))
        listOf("Book", "Plain").forEach { word ->
            val b = composeRule.onNodeWithTag("type-bar-font-$word").platformNode(composeRule.activity).boundsInScreen
            with(composeRule.density) {
                assertTrue(
                    "$word reports ${b.width().toDp()} x ${b.height().toDp()} to the platform tree",
                    b.width().toDp() >= 47.9.dp && b.height().toDp() >= 47.9.dp,
                )
            }
        }
    }

    @Test
    fun the_reason_line_takes_the_cards_width_and_never_sets_it() {
        // The longest of the lines. 288dp is the card's measured width without one
        // ([TypeBarTest.the_card_honours_the_frozen_max_width_on_the_smallest_supported_phone]); a `Text`
        // that reported its own intrinsic width would put the card at the screen's edge instead.
        open(storeWith(greek, family = "Averia Sans Libre"))
        val card = composeRule.onNodeWithTag(TypeBarTestTag).fetchSemanticsNode().boundsInRoot
        val line = reason().fetchSemanticsNode().boundsInRoot

        with(composeRule.density) {
            assertEquals("the reason line changed the card's width", 288f, card.width.toDp().value, 0.5f)
            assertTrue("the reason line is not wrapped inside the card", line.width <= card.width && line.height.toDp() > 30.dp)
        }
    }

    /** A29 rule 11, measured at the two sizes R24 names. The figures are printed so the run records them. */
    private fun assertCardStaysInTheRoomAndEveryControlIsReachable(screenHeightDp: Int) {
        open(storeWith(greek, family = "Averia Sans Libre"), fontScale = 2f)

        val card = composeRule.onNodeWithTag(TypeBarTestTag).fetchSemanticsNode().boundsInRoot
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        with(composeRule.density) {
            println(
                "A29.11 MEASURED 360x$screenHeightDp dp, font scale 2.0, unknown font + reason line: " +
                    "card ${card.width.toDp()} x ${card.height.toDp()}, top ${card.top.toDp()}, " +
                    "bottom ${card.bottom.toDp()}, screen ${root.height.toDp()}",
            )
            assertTrue("the card is ${card.width.toDp()} wide, over the frozen 336dp cap", card.width.toDp() <= 336.dp)
        }
        assertTrue("the card runs off the top of the screen", card.top >= root.top)
        assertTrue("the card runs off the bottom of the screen", card.bottom <= root.bottom)

        // Nothing is cut off and unreachable: every control can be brought on screen, top row and bottom.
        listOf("type-bar-font-Book", "type-bar-font-Plain", TypeBarFontReasonTestTag, "type-bar-Smaller", "type-bar-ink-Ochre")
            .forEach { composeRule.onNodeWithTag(it).performScrollTo().assertIsDisplayed() }
        listOf("Right", "Italic").forEach { composeRule.onNodeWithContentDescription(it).performScrollTo().assertIsDisplayed() }
        // And still usable after the scroll.
        composeRule.onNodeWithTag("type-bar-font-Plain").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("type-bar-font-Plain").assertIsSelected()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xhdpi")
    fun at_font_scale_2_on_360_by_800_the_card_stays_in_the_room_and_scrolls_inside_itself() =
        assertCardStaysInTheRoomAndEveryControlIsReachable(800)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun at_font_scale_2_on_360_by_640_the_card_stays_in_the_room_and_scrolls_inside_itself() =
        assertCardStaysInTheRoomAndEveryControlIsReachable(640)
}
