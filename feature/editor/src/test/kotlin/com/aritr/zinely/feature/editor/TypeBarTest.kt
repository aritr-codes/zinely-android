package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextAlign
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The FR-3 Type bar (ADR-055) end-to-end against a **real** [EditorStore]: every control must land as a
 * committed `Intent.StyleText` in the document, because the bar deliberately owns no styling state.
 *
 * Robolectric NATIVE, same tier as [EditorContextBarTest] / [ReframeA11yTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// The prototype's own viewport (bench `--w:430px`), the same qualifier the golden tier pins. Robolectric's
// default device is far shorter than any real phone, and the Type bar is a tall four-row card: on the
// default screen its bottom (Colour) row measures outside the canvas and no swatch is reachable. Pin the
// device rather than shrink the frozen bar.
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class TypeBarTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(300.0, 300.0)
    private val announced = mutableListOf<String>()

    /**
     * The haptic observer. [LocalHapticFeedback] is the only seam the production code has (ADR-055 §8 —
     * no haptic abstraction was introduced for one call site), and overriding the CompositionLocal is
     * how Compose intends it to be observed.
     */
    private class RecordingHaptics : HapticFeedback {
        val events = mutableListOf<HapticFeedbackType>()
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            events += hapticFeedbackType
        }
    }

    private val haptics = RecordingHaptics()

    private fun store(): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        return EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
                ),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
    }

    /** A store with one selected, non-blank text box — the only state that offers Style. */
    private fun storeWithText(text: String = "hi"): EditorStore =
        store().also { it.dispatch(Intent.PlaceText(Transform(40.0, 40.0, 20.0, 20.0), text)) }

    // A real decodable photo at the 1.25 box aspect used by the reframe case below. Reframe declines a
    // session for a photo it cannot measure (M7-01), so any test that opens one needs real bytes; the
    // text-only cases are unaffected either way.
    private val photo = reframeTestPhoto()

    private fun render(s: EditorStore, fontScale: Float = 1f) {
        announced.clear()
        haptics.events.clear()
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalHapticFeedback provides haptics,
                LocalDensity provides Density(base.density, fontScale),
            ) {
                ZinelyTheme {
                    EditorScreen(
                        store = s,
                        pageSizePt = pageSizePt,
                        imageBytes = photo,
                        onStyleAnnounce = { announced += it },
                    )
                }
            }
        }
    }

    private fun textOf(s: EditorStore): TextElement =
        s.uiState.value.document.pages[0].elements.first { it is TextElement } as TextElement

    /** Open the Type bar via the Style control — the only path a user has. */
    private fun openTypeBar() {
        composeRule.onNodeWithContentDescription("Text style").performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    private fun hasRole(role: Role) =
        SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    /**
     * Press one key with a modifier held, and report whether the editor **consumed** it — the Compose
     * equivalent of the frozen prototype's `preventDefault()` (bench `keydown`).
     *
     * The event is addressed to the canvas node because that is where the receiver lives; Compose routes
     * it to the focused component either way, which the canvas is whenever a shortcut is live.
     */
    private fun pressKey(keyCode: Int, meta: Int = NativeKeyEvent.META_CTRL_ON): Boolean {
        composeRule.waitForIdle()
        val consumed = composeRule.onNodeWithTag(EditorCanvasTestTag).performKeyPress(
            KeyEvent(NativeKeyEvent(0L, 0L, NativeKeyEvent.ACTION_DOWN, keyCode, 0, meta)),
        )
        composeRule.waitForIdle()
        return consumed
    }

    /**
     * Wait — in **real** time — for the size settle window to elapse and its one commit to land.
     *
     * No clock can be fast-forwarded here, and that is not an oversight. The stepper's settle is a
     * `delay` on the composition's coroutine; `AndroidUiDispatcher` does not implement `Delay`, so
     * kotlinx parks it on the default background timer. That timer answers to neither Robolectric's
     * looper clock (`ShadowLooper.idleMainLooper`) nor Compose's frame clock
     * (`mainClock.advanceTimeBy`) — only to the wall clock. `waitForIdle` likewise returns happy with
     * the timer still parked, since a suspended coroutine is not a pending recomposition.
     *
     * So we poll for the observable outcome instead, which is what [until] states.
     */
    private fun settleSize(until: () -> Boolean) {
        // waitForIdle FIRST, and it is not redundant: the settle coroutine is only launched by the
        // recomposition that the tap schedules, and `waitUntil` polls its condition without pumping
        // composition. Straight to waitUntil and the effect never starts — it spins the full timeout.
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) { until() }
        composeRule.waitForIdle()
    }

    // ── Toolbar: the Style hat ─────────────────────────────────────────────────────────────────────

    @Test
    fun the_style_control_is_offered_for_text_and_withheld_from_a_photo() {
        // Text: Style is on the bar.
        val s = storeWithText()
        render(s)
        composeRule.onNodeWithContentDescription("Text style").assertExists()

        // Photo: the bar is exactly what it always was — no Style, and no sixth-tool stand-in. The
        // frozen "Ink ↔ Style" swap has no Ink hat in Compose to swap against (it was never built), so
        // the contract this batch can actually hold is: a photo's bar is untouched.
        s.dispatch(Intent.Delete(s.uiState.value.selection))
        s.dispatch(
            Intent.CommitAddImage(
                ImageElement(id = "photo", transform = Transform(50.0, 50.0, 100.0, 80.0), assetId = "a"),
            ),
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Text style").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Move right").assertExists()
        // Delete is deliberately NOT asserted here any more. C2b put a Delete on the frozen `.ctx` bar too,
        // and D-039 then ruled that one capability gets one visible presentation at a time — so with a
        // photo selected the frozen bar owns Delete and this bar withholds it. The claim this test makes
        // is about *Style*, and the transform verbs around it, which are untouched in every case; whether
        // Delete is here or there is EditorScreenTest's subject, not this file's.
        composeRule.onNodeWithTag("$EditorContextBarTestTag-Delete").assertDoesNotExist()
        composeRule.onNodeWithTag(TypeBarTestTag).assertDoesNotExist()
    }

    @Test
    fun a_still_blank_text_box_is_not_offered_style() {
        // The reducer refuses to style a blank box (it would break the fresh-blank-place undo
        // coalescing), so the surface must not advertise a control that would silently no-op.
        render(storeWithText(text = "   "))
        composeRule.onNodeWithContentDescription("Text style").assertDoesNotExist()
    }

    @Test
    fun style_toggles_the_type_bar_open_and_closed() {
        render(storeWithText())
        composeRule.onNodeWithTag(TypeBarTestTag).assertDoesNotExist()

        openTypeBar()
        composeRule.onNodeWithTag(TypeBarTestTag).assertIsDisplayed()

        openTypeBar()
        composeRule.onNodeWithTag(TypeBarTestTag).assertDoesNotExist()
    }

    @Test
    fun a_selection_change_closes_the_type_bar() {
        val s = storeWithText()
        render(s)
        openTypeBar()
        composeRule.onNodeWithTag(TypeBarTestTag).assertIsDisplayed()

        s.dispatch(Intent.ClearSelection)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TypeBarTestTag).assertDoesNotExist()

        // …and it does not resurrect when the same box is selected again: reopening is the user's call.
        s.dispatch(Intent.Select(textOf(s).id))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TypeBarTestTag).assertDoesNotExist()
    }

    // ── Alignment ──────────────────────────────────────────────────────────────────────────────────

    @Test
    fun alignment_commits_and_reads_as_a_single_select_radio_group() {
        val s = storeWithText()
        render(s)
        openTypeBar()

        // ADR-055 §4: alignment is one choice of three, so it must speak as radios — NOT as the three
        // independent aria-pressed buttons the frozen HTML spells it with.
        composeRule.onNodeWithContentDescription("Left").assert(hasRole(Role.RadioButton))
        composeRule.onNodeWithContentDescription("Center").assert(hasRole(Role.RadioButton))
        composeRule.onNodeWithContentDescription("Right").assert(hasRole(Role.RadioButton))

        // START is the model default ⇒ Left is the selected radio.
        composeRule.onNodeWithContentDescription("Left").assertIsSelected()
        composeRule.onNodeWithContentDescription("Center").assertIsNotSelected()

        composeRule.onNodeWithContentDescription("Center").performClick()
        composeRule.waitForIdle()

        assertEquals(TextAlign.CENTER, textOf(s).style.align)
        composeRule.onNodeWithContentDescription("Center").assertIsSelected()
        composeRule.onNodeWithContentDescription("Left").assertIsNotSelected()
        assertTrue("Centered" in announced)

        composeRule.onNodeWithContentDescription("Right").performClick()
        composeRule.waitForIdle()
        assertEquals(TextAlign.END, textOf(s).style.align)
    }

    /**
     * CI-30 — extend the Role coverage past alignment (the only Role asserted before, at
     * [alignment_commits_and_reads_as_a_single_select_radio_group]) to the bold/italic toggles and the size
     * steppers, and cross the merged/platform boundary the `f4faaa4` defect exposed.
     *
     * The Type-bar controls each merge a glyph child, and a role on a node that merges child content does
     * not carry its Role-derived class onto the platform node: the platform `className` is a deterministic
     * `android.view.View` for these controls (the leaf/cleared-vs-merged rule that governs every role
     * uniformly — see [com.aritr.zinely.feature.editor.a11y.ZButtonPlatformA11yTest]). This test does not
     * assert any platform className — that would be neither meaningful nor load-bearing here — so Role is
     * asserted on the merged tree; the only stable platform-tree Role assertions in this milestone are on
     * the `clearAndSetSemantics` Reframe controls
     * ([com.aritr.zinely.feature.editor.a11y.ReframeControlsRolePlatformA11yTest]). The attribute the
     * platform **does** carry faithfully — the enabled bit (the `f4faaa4` property) — is asserted on the
     * platform node.
     */
    @Test
    fun type_bar_roles_are_declared_and_the_stepper_enabled_bit_reaches_the_platform_tree() {
        val s = storeWithText()
        render(s)
        openTypeBar()

        // Alignment — one-of-three ⇒ RadioButton. Bold/Italic — independent toggles ⇒ Checkbox.
        composeRule.onNodeWithContentDescription("Left").assert(hasRole(Role.RadioButton))
        composeRule.onNodeWithContentDescription("Center").assert(hasRole(Role.RadioButton))
        composeRule.onNodeWithContentDescription("Bold").assert(hasRole(Role.Checkbox))
        composeRule.onNodeWithContentDescription("Italic").assert(hasRole(Role.Checkbox))

        // Size steppers — Role.Button; and the enabled bit carries to the platform tree (a live stepper is
        // enabled to TalkBack — the property f4faaa4 got wrong on a sibling stepper).
        composeRule.onNodeWithContentDescription("Larger").assert(hasRole(Role.Button))
        assertTrue(
            "an active stepper must be enabled to the platform",
            composeRule.onNodeWithContentDescription("Larger").platformNode(composeRule.activity).isEnabled,
        )
    }

    // ── Bold / Italic ──────────────────────────────────────────────────────────────────────────────

    @Test
    fun bold_and_italic_are_independent_toggles_that_commit_immediately() {
        val s = storeWithText()
        render(s)
        openTypeBar()

        composeRule.onNodeWithContentDescription("Bold").assertIsOff()
        composeRule.onNodeWithContentDescription("Bold").performClick()
        composeRule.waitForIdle()
        assertEquals(true, textOf(s).style.bold)
        composeRule.onNodeWithContentDescription("Bold").assertIsOn()
        assertTrue("Bold on" in announced)

        // Independent, not a group: italic must not clear bold (ADR-055 §4 — they genuinely compose).
        composeRule.onNodeWithContentDescription("Italic").performClick()
        composeRule.waitForIdle()
        assertEquals(true, textOf(s).style.bold)
        assertEquals(true, textOf(s).style.italic)

        composeRule.onNodeWithContentDescription("Bold").performClick()
        composeRule.waitForIdle()
        assertEquals(false, textOf(s).style.bold)
        assertEquals(true, textOf(s).style.italic)
        assertTrue("Bold off" in announced)
    }

    // ── Colour ─────────────────────────────────────────────────────────────────────────────────────

    @Test
    fun a_swatch_commits_its_fixed_paper_space_ink() {
        val s = storeWithText()
        render(s)
        openTypeBar()

        composeRule.onNodeWithContentDescription("Teal").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Teal").performClick()
        composeRule.waitForIdle()

        assertEquals(TextInk.Teal.rgba, textOf(s).style.color)
        composeRule.onNodeWithContentDescription("Teal").assertIsSelected()
        composeRule.onNodeWithContentDescription("Coral").assertIsNotSelected()
        assertTrue("Colour Teal" in announced)
    }

    @Test
    fun the_ink_palette_is_the_five_frozen_text_inks() {
        // Pins the palette against drift and against being conflated with the 4-entry image spot-ink
        // set (ADR-055 Decision 6). Fixed RGBA: theme-independent, because ink prints the same either way.
        assertEquals(5, TextInk.entries.size)
        assertEquals(
            listOf("Ink", "Coral", "Teal", "Blue", "Ochre"),
            TextInk.entries.map { it.label },
        )
        assertEquals(com.aritr.zinely.core.model.ColorRgba(0x23, 0x20, 0x1C), TextInk.Ink.rgba)
        assertEquals(com.aritr.zinely.core.model.ColorRgba(0x7A, 0x5E, 0x12), TextInk.Ochre.rgba)
    }

    // ── Size ───────────────────────────────────────────────────────────────────────────────────────

    @Test
    fun the_size_ramp_is_the_frozen_ten_stop_ladder() {
        assertEquals(listOf(10.0, 12.0, 14.0, 16.0, 20.0, 24.0, 28.0, 32.0, 40.0, 48.0), TypeSizesPt)
        // An off-ramp size (a document from elsewhere) lands on its nearest stop rather than snapping to 0.
        assertEquals(2, nearestSizeIndex(14.4))
        assertEquals(3, nearestSizeIndex(15.6))
        // An exact tie resolves to the SMALLER stop — the frozen `nearestSize`'s strict `d<bd` scan, kept
        // deliberately so a 15pt import steps the same way here as in the prototype.
        assertEquals(2, nearestSizeIndex(15.0))
        assertEquals(0, nearestSizeIndex(1.0))
        assertEquals(TypeSizesPt.lastIndex, nearestSizeIndex(999.0))
    }

    @Test
    fun the_size_stepper_disables_at_both_ramp_ends() {
        val s = storeWithText()
        render(s)
        openTypeBar()

        // Default 12pt is ramp index 1 — one stop in, so both ends are live.
        composeRule.onNodeWithContentDescription("Smaller").assertIsEnabled()
        composeRule.onNodeWithContentDescription("Larger").assertIsEnabled()

        composeRule.onNodeWithContentDescription("Smaller").performClick()
        composeRule.onNodeWithContentDescription("Size 10 point").assertExists()
        settleSize { textOf(s).style.sizePt == 10.0 }
        assertEquals(10.0, textOf(s).style.sizePt, 1e-9)
        composeRule.onNodeWithContentDescription("Smaller").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Larger").assertIsEnabled()
    }

    @Test
    fun a_burst_of_size_taps_settles_into_exactly_one_undoable_commit() {
        val s = storeWithText()
        render(s)
        openTypeBar()

        // Three taps in one burst. The settle window is real time (see [settleSize]), so this asserts
        // three synthetic clicks land inside 400ms — they do, by two orders of magnitude, and the
        // mid-burst assertion below fails loudly rather than silently passing if that ever stops holding.
        repeat(3) { composeRule.onNodeWithContentDescription("Larger").performClick() }

        // Mid-burst: the readout has moved to the third stop…
        composeRule.onNodeWithContentDescription("Size 20 point").assertExists()
        // …but NOTHING is committed yet — the reducer has not seen a single intent.
        assertEquals(12.0, textOf(s).style.sizePt, 1e-9)

        settleSize { textOf(s).style.sizePt == 20.0 }

        // One commit, carrying only the burst's final value: 12pt (index 1) + 3 stops ⇒ index 4 = 20pt.
        assertEquals(20.0, textOf(s).style.sizePt, 1e-9)

        // …and it is ONE undo step, not three: a single Undo returns to the placed size.
        s.dispatch(Intent.Undo)
        composeRule.waitForIdle()
        assertEquals(12.0, textOf(s).style.sizePt, 1e-9)
    }

    @Test
    fun closing_the_type_bar_mid_settle_still_commits_the_pending_size() {
        // Regression (review finding 1): the settle lived inside the bar, so closing it inside the 400ms
        // window cancelled the effect and the size was LOST — after the readout had already moved and the
        // announcement had already claimed it. The surface must not lie about what it committed.
        val s = storeWithText()
        render(s)
        openTypeBar()

        composeRule.onNodeWithContentDescription("Larger").performClick()
        composeRule.onNodeWithContentDescription("Size 14 point").assertExists()

        // Close well inside the settle window.
        composeRule.onNodeWithContentDescription("Text style").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TypeBarTestTag).assertDoesNotExist()

        settleSize { textOf(s).style.sizePt == 14.0 }
        assertEquals(14.0, textOf(s).style.sizePt, 1e-9)
    }

    @Test
    fun deselecting_mid_settle_commits_to_the_box_being_left() {
        // Regression (review finding 1), the other unmount path: the flush must land on the element the
        // user was styling, not on whatever is selected next.
        val s = storeWithText()
        render(s)
        openTypeBar()

        composeRule.onNodeWithContentDescription("Larger").performClick()
        composeRule.onNodeWithContentDescription("Size 14 point").assertExists()

        s.dispatch(Intent.ClearSelection)
        composeRule.waitForIdle()

        settleSize { textOf(s).style.sizePt == 14.0 }
        assertEquals(14.0, textOf(s).style.sizePt, 1e-9)
    }

    // ── Undo ───────────────────────────────────────────────────────────────────────────────────────

    @Test
    fun undo_restores_the_style_and_resyncs_the_open_type_bar() {
        val s = storeWithText()
        render(s)
        openTypeBar()

        composeRule.onNodeWithContentDescription("Center").performClick()
        composeRule.onNodeWithContentDescription("Bold").performClick()
        composeRule.waitForIdle()
        assertEquals(TextAlign.CENTER, textOf(s).style.align)
        assertEquals(true, textOf(s).style.bold)

        // Cancel is undo (ADR-055): each control was one step, so Undo peels bold off first.
        s.dispatch(Intent.Undo)
        composeRule.waitForIdle()
        assertEquals(false, textOf(s).style.bold)
        assertEquals(TextAlign.CENTER, textOf(s).style.align)

        // The bar holds no state of its own, so it re-syncs to the restored style — it must not still
        // be showing Bold as on, and it must stay open (the element id never changed).
        composeRule.onNodeWithTag(TypeBarTestTag).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Bold").assertIsOff()
        composeRule.onNodeWithContentDescription("Center").assertIsSelected()

        s.dispatch(Intent.Undo)
        composeRule.waitForIdle()
        assertEquals(TextAlign.START, textOf(s).style.align)
        composeRule.onNodeWithContentDescription("Left").assertIsSelected()
    }

    @Test
    fun an_undo_mid_settle_supersedes_the_burst_instead_of_landing_on_top_of_it() {
        // Regression (review finding 5): a settling burst used to land 400ms after the user pressed
        // Undo — overwriting the style they just restored and wiping the redo stack with it.
        val s = storeWithText()
        render(s)
        openTypeBar()

        composeRule.onNodeWithContentDescription("Larger").performClick()
        settleSize { textOf(s).style.sizePt == 14.0 }

        // Start a second burst, then undo the first one from under it.
        composeRule.onNodeWithContentDescription("Larger").performClick()
        composeRule.onNodeWithContentDescription("Size 16 point").assertExists()
        s.dispatch(Intent.Undo)
        composeRule.waitForIdle()
        assertEquals(12.0, textOf(s).style.sizePt, 1e-9)

        // The abandoned burst must never arrive. Give it well past the settle window to misbehave.
        Thread.sleep(TypeSizeSettleMs * 3)
        composeRule.waitForIdle()
        assertEquals(12.0, textOf(s).style.sizePt, 1e-9)
        // …and the redo the user is entitled to is still there.
        assertTrue(s.uiState.value.canRedo)
        // The bar re-synced to the restored size rather than showing the abandoned burst's readout.
        composeRule.onNodeWithContentDescription("Size 12 point").assertExists()
    }

    // ── Touch targets ──────────────────────────────────────────────────────────────────────────────

    @Test
    fun every_type_bar_control_keeps_a_48dp_target_without_inflating_its_frozen_paint() {
        // Regression (review finding 2): zinelyControl's 48dp minimum sat INSIDE .size(40.dp) on the
        // stepper, so the chip's fill/border/clip rendered at 48x48 in a 40x40 slot and spilled 8dp into
        // the readout. Target and paint are different things; assert the target here.
        //
        // `touchBoundsInRoot` is the INPUT-LAYER bound, not the layout bound — Compose expands any
        // clickable smaller than `ViewConfiguration.minimumTouchTargetSize` to 48dp on its own. That is
        // why "Teal" passes on a 32dp swatch and "Bold" on a 46dp chip, with no
        // `minimumInteractiveComponentSize` on either: the frozen paint keeps its size, the target is
        // still expanded, and removing the modifier (which reserves layout space only) took none of it.
        //
        // Read this assertion for exactly what it says, and no more. It measures the RAW expanded bound.
        // Where expansions OVERLAP — the ink row's frozen 40dp pitch — Compose prunes the overlap before
        // reporting bounds to the accessibility layer, so four of the five swatches reach TalkBack as
        // 40x48, not 48x48 (see [Swatch] and ADR-055 §8). That is a property of the frozen 32dp/8dp grid,
        // not of this modifier decision: it was equally true of any implementation honouring the spec.
        //
        // The alignment segments are deliberately absent: they are the frozen 46dp (see [AlignOption]).
        render(storeWithText())
        openTypeBar()

        listOf("Smaller", "Larger", "Bold", "Italic", "Teal")
            .forEach { label ->
                val size = composeRule.onNodeWithContentDescription(label)
                    .fetchSemanticsNode().touchBoundsInRoot
                val density = composeRule.density
                with(density) {
                    assertTrue(
                        "$label touch target is ${size.width.toDp()} x ${size.height.toDp()}, under 48dp",
                        size.width.toDp() >= 47.9.dp && size.height.toDp() >= 47.9.dp,
                    )
                }
            }
    }


    @Test
    fun a_tap_outside_the_stepper_chips_frozen_paint_still_steps_the_size() {
        // The sibling test above asserts the REPORTED bound (`touchBoundsInRoot`). This one asserts the
        // bound is real: that a press landing in the expanded margin — outside the frozen 40dp paint —
        // actually routes to the button. Reported bounds and hit-testing are separate code paths
        // (`NodeCoordinator.distanceInMinimumTouchTarget`), so a 48dp number in the a11y tree is not by
        // itself proof that a finger 3dp off the chip does anything.
        //
        // This is the assertion that would have caught the fix being wrong: collapsing StepButton to the
        // 40dp chip gave up the 48dp LAYOUT box, and the whole fix rests on the claim that the target did
        // not go with it.
        render(storeWithText())
        openTypeBar()

        // Default 12pt is ramp index 1, so "Smaller" is enabled and one step lands on 10pt.
        composeRule.onNodeWithContentDescription("Smaller").performTouchInput {
            // 3dp left of the chip's own left edge — inside the expanded target, outside the paint.
            click(Offset(-3.dp.toPx(), centerY))
        }
        composeRule.waitForIdle()
        // Past the settle window, so the coalesced burst has committed.
        composeRule.mainClock.advanceTimeBy(TypeSizeSettleMs + 100)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Size 10 point").assertExists()
    }

    // ── Row layout parity with the frozen bench ────────────────────────────────────────────────────

    @Test
    fun the_size_stepper_paints_the_frozen_40dp_chips_at_the_frozen_8dp_pitch() {
        // Regression (2026-07-17, ADR-055 §8): StepButton wrapped its frozen 40dp chip in an explicit
        // `Modifier.size(48.dp)` LAYOUT box "for the touch target". The target never needed it (input-layer
        // expansion — see the touch-target test above), and the box cost 4dp of slack on each side of the
        // chip: bench `.tysize{gap:8px}` over `40px` buttons painted as 12dp gaps, and the cluster went
        // 154 -> 170dp.
        //
        // NOTHING caught it, which is why this test exists and is spelled in PAINT terms:
        //  - the card-width tests are blind because Colour (192dp) is the widest row, so `IntrinsicSize.Max`
        //    resolves against Colour and `SpaceBetween` silently absorbs the Size row's extra 16dp;
        //  - the shared-right-edge test below is blind because it reads `boundsInRoot` — the LAYOUT box —
        //    which was flush to the row edge before the fix and is flush to it now. Only the *paint* moved.
        // So assert the frozen numbers directly: 40dp chips, 8dp gaps, a 154dp cluster.
        render(storeWithText())
        openTypeBar()

        fun bounds(label: String) =
            composeRule.onNodeWithContentDescription(label).fetchSemanticsNode().boundsInRoot

        val minus = bounds("Smaller")
        val plus = bounds("Larger")
        // The readout is `clearAndSetSemantics`, so it is one node carrying the size announcement.
        val readout = bounds("Size 12 point")

        with(composeRule.density) {
            assertEquals("− chip is not the frozen 40dp", 40f, minus.width.toDp().value, 0.5f)
            assertEquals("− chip is not the frozen 40dp tall", 40f, minus.height.toDp().value, 0.5f)
            assertEquals("+ chip is not the frozen 40dp", 40f, plus.width.toDp().value, 0.5f)
            assertEquals(
                "the − chip to readout gap is not the frozen 8dp",
                8f, (readout.left - minus.right).toDp().value, 0.5f,
            )
            assertEquals(
                "the readout to + chip gap is not the frozen 8dp",
                8f, (plus.left - readout.right).toDp().value, 0.5f,
            )
            // 40 + 8 + 58 + 8 + 40 — the whole frozen `.tysize` cluster.
            assertEquals(
                "the size cluster is not the frozen 154dp",
                154f, (plus.right - minus.left).toDp().value, 0.5f,
            )
        }
    }

    @Test
    fun the_four_rows_pin_their_labels_left_and_their_controls_to_one_shared_right_edge() {
        // Regression (P1 parity): TypeRow was a wrap-content Row, which made its
        // `Arrangement.spacedBy(14.dp, Alignment.End)` inert — a wrap-content Row has no free space to
        // align within, so each control hugged its own label at a flat 14dp and the four rows rendered
        // ragged-right. Bench `.typebar{width:max-content; align-items:stretch}` + `.tyrow{justify-
        // content:space-between}` is one left edge for the labels and one right edge for the controls.
        //
        // Asserted on LAYOUT bounds, because bench parity is a question about the edges the user SEES.
        // (This once asserted on touch bounds, which was only equivalent while every control's layout had
        // been inflated to 48dp — the very defect that blew the card out to 360dp. Touch bounds expand
        // past a 32dp swatch's paint by 8dp at the input layer, so they were never the right ruler here.)
        render(storeWithText())
        openTypeBar()

        fun rightOf(label: String) =
            composeRule.onNodeWithContentDescription(label).fetchSemanticsNode().boundsInRoot.right
        // By contentDescription, not by text: `.tylab{text-transform:uppercase}` renders "SIZE" while the
        // string — and what a screen reader says — stays "Size". [TypeRow] splits the two deliberately,
        // and this is the reader that has to agree with the split.
        fun leftOf(label: String) =
            composeRule.onNodeWithContentDescription(label).fetchSemanticsNode().boundsInRoot.left

        // The last control of each row, top to bottom: Size · Align · Style · Colour.
        val rightEdges = listOf(rightOf("Larger"), rightOf("Right"), rightOf("Italic"), rightOf("Ochre"))
        val labelLefts = listOf(leftOf("Size"), leftOf("Align"), leftOf("Style"), leftOf("Colour"))

        assertTrue(
            "control clusters are ragged-right, not on one shared edge: $rightEdges",
            rightEdges.max() - rightEdges.min() < 0.5f,
        )
        assertTrue(
            "labels do not share one left edge: $labelLefts",
            labelLefts.max() - labelLefts.min() < 0.5f,
        )
    }

    @Test
    fun the_card_is_as_wide_as_its_widest_row_and_no_wider() {
        // bench `.typebar{width:max-content}`: `IntrinsicSize.Max` must resolve to the widest row, NOT to
        // the incoming max constraint. Guards the fillMaxWidth/IntrinsicSize.Max pairing — drop the
        // Column's `width(IntrinsicSize.Max)` and each row's `fillMaxWidth` blows the card out to the
        // 430dp viewport; drop the row's `fillMaxWidth` and the rows go ragged again.
        render(storeWithText())
        openTypeBar()

        val card = composeRule.onNodeWithTag(TypeBarTestTag).fetchSemanticsNode().boundsInRoot
        // The widest row is Colour (five 30dp pots + four 18dp `SwatchGap`s = 222dp) plus the label column
        // and the card's 16dp side padding — comfortably inside the 430dp device the golden tier pins.
        with(composeRule.density) {
            assertTrue(
                "the Type bar filled its parent (${card.width.toDp()}) instead of hugging its widest row",
                card.width.toDp() < 430.dp,
            )
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xhdpi")
    fun the_card_honours_the_frozen_max_width_on_the_smallest_supported_phone() {
        // Regression (B4 parity blocker): `minimumInteractiveComponentSize` on each Swatch answered the
        // Column's `IntrinsicSize.Max` query with 48dp — it overrides `measure`, not the intrinsics, so
        // the default LayoutModifierNode intrinsics re-run `measure` and hand back the inflated width.
        // Five swatches at 48 instead of the frozen 32 made Colour the widest row by 80dp:
        //   28 (card padding) + 60 (label + gap) + 272 (5x48 + 4x8) = exactly 360dp
        // i.e. edge-to-edge on this device, over the frozen `max-width:calc(100% - 24px)` (= 336dp), and
        // clipping below 360dp. The frozen cluster was 192dp, which put the card at 280dp.
        //
        // ⚠ The expected width moved to 318.5dp with the 2026-08-15 touch-target fix, and it is the SAME
        // invariant, not a relaxed one: `SwatchGap` went 8dp -> 18dp so the swatch pitch reaches the
        // platform's 48dp minimum (see `every_type_bar_control_reports_a_full_48dp_target_to_the_platform`),
        // which widens the Colour cluster by 40dp to 222dp. The cap it is measured against — the frozen
        // `max-width:calc(100% - 24px)` = 336dp — has not moved, and the card is still under it with 17.5dp
        // to spare. What this second assertion pins is that the width is EXPLAINED: 32 (card padding) +
        // 64.5 (label + gap) + 222 (5x30 + 4x18). Anything wider means a control is inflated again.
        //
        // The sibling test above could not see this: it is pinned to the 430dp bench viewport and only
        // asserts `< 430dp`, which a 360dp card passes. This one pins the smallest phone we support and
        // asserts the frozen CAP, not merely "narrower than the screen".
        render(storeWithText())
        openTypeBar()

        val card = composeRule.onNodeWithTag(TypeBarTestTag).fetchSemanticsNode().boundsInRoot
        with(composeRule.density) {
            assertTrue(
                "the Type bar is ${card.width.toDp()} — over the frozen max-width of 336dp on a 360dp phone",
                card.width.toDp() <= 336.dp,
            )
            // …and it got there by matching the spec's paint, not by being squeezed.
            assertTrue(
                "the Type bar is ${card.width.toDp()}, not the spec's max-content 318.5dp",
                card.width.toDp() <= 319.dp,
            )
        }
        // The cap must not have been bought with the touch target. Asserted on the PLATFORM tree, not on
        // `touchBoundsInRoot` — see [every_type_bar_control_reports_a_full_48dp_target_to_the_platform]
        // for why the latter cannot fail.
        assertPlatformTargetAtLeast48("Teal")
    }

    /**
     * The same cap at **fontScale 2.0**, the accessibility setting that actually threatens it.
     *
     * This exists because a device pass reported the card standing ~3dp from the screen edge at this
     * setting, i.e. the frozen `max-width:calc(100% - 24px)` being violated, and read the cause as Compose
     * "not expressing" the max-width at all. It does express it —`EditorScreen.kt` gives the bar a
     * symmetric `padding(horizontal = 12.dp)`, which lowers the incoming max constraint that the card's
     * `width(IntrinsicSize.Max)` then clamps against (`MaxIntrinsicWidthModifier` enforces the incoming
     * constraints; only `requiredWidth(IntrinsicSize.Max)` would not). So the guard is real, and this test
     * is what says so at the scale where it has to work: on a 360dp phone the card must stay inside 336dp
     * however wide the labels grow. If a device disagrees, the difference is the finding — start with the
     * 4dp `zinelyV21HardShadow`, which paints OUTSIDE the node and is not in any of these bounds.
     */
    @Test
    @Config(qualifiers = "w360dp-h780dp-xhdpi")
    fun the_card_honours_the_frozen_max_width_at_the_largest_font_scale() {
        render(storeWithText(), fontScale = 2f)
        openTypeBar()

        val card = composeRule.onNodeWithTag(TypeBarTestTag).fetchSemanticsNode().boundsInRoot
        with(composeRule.density) {
            assertTrue(
                "at fontScale 2.0 the Type bar is ${card.width.toDp()} — over the frozen 336dp cap",
                card.width.toDp() <= 336.dp,
            )
        }
    }

    /**
     * **Every control in the card must report at least 48 x 48 dp to the PLATFORM accessibility tree.**
     *
     * This replaces an assertion that could not fail. The old one read
     * `onNodeWithContentDescription("Teal").fetchSemanticsNode().touchBoundsInRoot` and asserted
     * `>= 47.9.dp` — but `touchBoundsInRoot` is the **pre-pruning** value: it is the raw input-layer
     * expansion of the control's paint to `ViewConfiguration.minimumTouchTargetSize`, computed without
     * reference to any neighbour. It returns a flat 48dp whatever the pitch, so the assertion was true by
     * construction and stayed green through two separate real defects.
     *
     * What TalkBack actually receives is the *pruned* rect. `SemanticsOwner.getAllUncoveredSemanticsNodes`
     * walks the tree keeping a `Region` of unclaimed screen, intersects each node's touch bounds against
     * it, and subtracts. Siblings are visited in **reverse declaration order**, so the LAST row claims
     * first and the rows above it are cut. The device dump that prompted this test (SM-A176B, Android 16,
     * density override 420) reported:
     *
     *     Ink / Coral / Teal / Blue   38.1 x 48.0 dp   <- pitch 38dp (30dp pot + 8dp gapSm), horizontal
     *     Ochre                       48.0 x 48.0 dp   <- no right-hand neighbour to collide with
     *     Bold / Italic               48.0 x 46.1 dp   <- the Colour row's 9dp reach across an 8dp row gap
     *
     * Both are fixed by pitch, not by paint: `SwatchGap` 8dp -> 18dp (30 + 18 = 48) and the card's row gap
     * `gapSm` -> `gapMd` (12 > the pot's 9dp reach). Neither control's painted size moved.
     *
     * Rendered through the whole [EditorScreen], deliberately: pruning is a property of the SURFACE, and a
     * standalone `TypeBar` would answer a question about a screen that does not exist. Robolectric runs the
     * real `AndroidComposeView` accessibility provider, so this is the same computation a device performs —
     * the harness's own limits are recorded in `PlatformAccessibilityTree`.
     */
    @Test
    fun every_type_bar_control_reports_a_full_48dp_target_to_the_platform() {
        render(storeWithText())
        openTypeBar()

        // The colour row, where the horizontal collision was. Ochre is included even though it never
        // failed: it is the control that proves the cause was the neighbour and not the pot.
        listOf("Ink", "Coral", "Teal", "Blue", "Ochre").forEach { assertPlatformTargetAtLeast48(it) }
        // The style row, where the vertical collision was — cut by the Colour row BELOW it.
        listOf("Bold", "Italic").forEach { assertPlatformTargetAtLeast48(it) }
        // The two rows that were already clean, pinned so a future gap change cannot quietly break them.
        listOf("Smaller", "Larger", "Left", "Center", "Right").forEach { assertPlatformTargetAtLeast48(it) }
    }

    /**
     * The **platform** hit-rect for a control, in dp, asserted against Material's 48dp minimum.
     *
     * `boundsInScreen` is what `uiautomator dump` prints and what a service consumes; a shortfall here is
     * the exact defect the device measured, and no merged-tree assertion can see it.
     */
    private fun assertPlatformTargetAtLeast48(label: String) {
        val bounds = composeRule.onNodeWithContentDescription(label)
            .platformNode(composeRule.activity)
            .boundsInScreen
        with(composeRule.density) {
            val w = bounds.width().toDp()
            val h = bounds.height().toDp()
            assertTrue(
                "$label reports ${w.value} x ${h.value} dp to the platform tree — under the 48dp minimum. " +
                    "The paint is fine; the PITCH is not (neighbouring 48dp expansions overlap and the " +
                    "overlap is pruned before setBoundsInScreen).",
                w >= 47.9.dp && h >= 47.9.dp,
            )
        }
    }

    /**
     * **The selected option in each radio group reports `clickable=false` to the platform — and that is
     * Compose, not this card.**
     *
     * The same device dump found `Left` and `Teal` (the chosen align and the chosen ink) carrying
     * `clickable=false` while their unselected siblings carried `clickable=true`. It is not a semantics
     * declaration this file got wrong, and it is not reachable from here: both shapes are affected —
     * [AlignOption], which re-declares everything under `clearAndSetSemantics`, and [Swatch], which uses
     * the stock `Modifier.selectable` — and the ONLY property they share is `SemanticsProperties.Selected`.
     * `AndroidComposeViewAccessibilityDelegateCompat` gates the platform click on it: a node carrying
     * `Selected == true` is published without `isClickable` and without `ACTION_CLICK`, on the reasoning
     * that re-activating an already-chosen option is a no-op. Every Compose `selectable` behaves this way,
     * including Material's own `RadioButton` and `Tab`.
     *
     * The only way to change it is to stop declaring `Selected` and hand-roll a `stateDescription`, which
     * trades a framework-localised "Selected" for an English literal and loses the property this same
     * suite (and `ReframeControlsRolePlatformA11yTest`) asserts reaches a service. That is a worse card,
     * not a better one, so the behaviour is PINNED here rather than fought: this test fails the day the
     * framework changes its mind, and the fix is then to delete the test, not to chase it.
     *
     * What must never regress is the half that matters — the state is still announced, and every option
     * the user has NOT chosen is still activatable.
     */
    @Test
    fun the_platform_gates_a_selected_radio_click_and_still_announces_its_state() {
        render(storeWithText()) // START-aligned by default; the default RGBA matches no TextInk…
        openTypeBar()
        composeRule.onNodeWithContentDescription("Teal").performClick() // …so commit one.
        composeRule.waitForIdle()

        fun node(label: String) = composeRule.onNodeWithContentDescription(label).platformNode(composeRule.activity)

        // The chosen option of each group: no click published, but the state is.
        listOf("Left", "Teal").forEach {
            assertTrue("$it is selected, so Compose withholds the platform click", !node(it).isClickable)
            assertEquals("$it must still tell a service it is the chosen one", "Selected", node(it).stateDescription)
        }
        // Every option the user has not chosen stays reachable — the half a regression would break.
        listOf("Center", "Right", "Ink", "Coral", "Blue", "Ochre").forEach {
            assertTrue("$it is not selected and must be clickable to the platform", node(it).isClickable)
            assertEquals("Not selected", node(it).stateDescription)
        }
    }

    // ── Keyboard shortcuts (ADR-055 §4) ────────────────────────────────────────────────────────────

    @Test
    fun ctrl_b_and_ctrl_i_toggle_a_selected_text_box_and_consume_the_event() {
        val s = storeWithText()
        render(s)

        // No prior tap on the canvas: selecting the box hands it the keyboard.
        assertTrue("Ctrl+B was not consumed", pressKey(NativeKeyEvent.KEYCODE_B))
        assertEquals(true, textOf(s).style.bold)
        assertTrue("Bold on" in announced)

        assertTrue("Ctrl+I was not consumed", pressKey(NativeKeyEvent.KEYCODE_I))
        assertEquals(true, textOf(s).style.italic)
        // Independent, exactly as the pointer path is — italic must not clear bold.
        assertEquals(true, textOf(s).style.bold)
        assertTrue("Italic on" in announced)

        // They toggle, not latch: each press is its own committed, undoable change.
        pressKey(NativeKeyEvent.KEYCODE_B)
        assertEquals(false, textOf(s).style.bold)
        assertTrue("Bold off" in announced)
        s.dispatch(Intent.Undo)
        composeRule.waitForIdle()
        assertEquals(true, textOf(s).style.bold)
    }

    @Test
    fun the_shortcuts_work_with_the_cmd_key_and_do_not_need_the_type_bar_open() {
        // bench: `const meta = e.ctrlKey||e.metaKey` — and the handler is on the document, not the bar.
        val s = storeWithText()
        render(s)
        composeRule.onNodeWithTag(TypeBarTestTag).assertDoesNotExist()

        assertTrue(pressKey(NativeKeyEvent.KEYCODE_B, meta = NativeKeyEvent.META_META_ON))
        assertEquals(true, textOf(s).style.bold)

        // With the bar open the change must show up in it — it holds no styling state, so it re-syncs.
        openTypeBar()
        composeRule.onNodeWithContentDescription("Bold").assertIsOn()
        pressKey(NativeKeyEvent.KEYCODE_B)
        composeRule.onNodeWithContentDescription("Bold").assertIsOff()
    }

    @Test
    fun a_bare_b_or_i_is_ignored() {
        // The shortcut is the modified chord only: an unmodified letter is not ours to consume.
        val s = storeWithText()
        render(s)

        assertTrue("bare B was consumed", !pressKey(NativeKeyEvent.KEYCODE_B, meta = 0))
        assertEquals(false, textOf(s).style.bold)
        assertTrue(announced.isEmpty())
        assertTrue(haptics.events.isEmpty())
    }

    @Test
    fun the_shortcuts_are_suppressed_inside_an_inline_edit_session() {
        // ADR-055 §4: suppressed while editing, so the keystroke reaches the text field instead of
        // block-styling behind the user's back (the frozen prototype's rich-text-injection guard).
        val s = storeWithText()
        render(s)
        s.dispatch(Intent.BeginEditText(textOf(s).id))
        composeRule.waitForIdle()

        // Consumption is deliberately NOT asserted here: the focused text field consumes the chord
        // itself, which is the whole point — the keystroke belongs to the editor, not to us. What must
        // hold is that no block style was committed behind it.
        pressKey(NativeKeyEvent.KEYCODE_B)
        assertEquals(false, textOf(s).style.bold)
        pressKey(NativeKeyEvent.KEYCODE_I)
        assertEquals(false, textOf(s).style.italic)
        assertTrue(announced.isEmpty())
        assertTrue(haptics.events.isEmpty())
        // Plain-text editor: the chord cannot inject a `<b>`/`<i>` the way the prototype's
        // contenteditable would, so falling through is safe rather than merely suppressed.
        assertEquals("hi", textOf(s).text)
    }

    @Test
    fun the_shortcuts_are_inert_without_a_styleable_text_selection() {
        // Nothing selected.
        val s = storeWithText()
        render(s)
        s.dispatch(Intent.ClearSelection)
        composeRule.waitForIdle()
        assertTrue(!pressKey(NativeKeyEvent.KEYCODE_B))
        assertEquals(false, textOf(s).style.bold)

        // A photo: same bar as ever, and no styling verb behind the keyboard either.
        s.dispatch(
            Intent.CommitAddImage(
                ImageElement(id = "photo", transform = Transform(50.0, 50.0, 100.0, 80.0), assetId = "a"),
            ),
        )
        composeRule.waitForIdle()
        assertTrue(!pressKey(NativeKeyEvent.KEYCODE_B))
        assertEquals(false, textOf(s).style.bold)
        assertTrue(announced.isEmpty())
    }

    @Test
    fun a_blank_text_box_is_not_styleable_by_keyboard_either() {
        // The reducer refuses a blank box; the shortcut must not offer what the bar withholds (and what
        // the reducer would silently no-op) — the two gates are the same `styleTarget`.
        //
        // Start non-blank and press once for real. That is not scene-setting: it proves the receiver HAS
        // the keyboard, so the refusal below is the guard doing its job rather than a key that landed
        // nowhere. (Rendering straight into a blank box would pass with no guard at all — the receiver
        // would simply never have been handed focus. Review finding.)
        val s = storeWithText()
        render(s)
        assertTrue(pressKey(NativeKeyEvent.KEYCODE_B))
        assertEquals(true, textOf(s).style.bold)

        // Select a blank box instead. Focus stays put, so the chord reaches the handler — and is refused.
        s.dispatch(Intent.PlaceText(Transform(80.0, 80.0, 20.0, 20.0), "   "))
        composeRule.waitForIdle()
        val blankId = s.uiState.value.selection.single()
        announced.clear()

        assertTrue("a blank box was styled by keyboard", !pressKey(NativeKeyEvent.KEYCODE_B))
        val blank = s.uiState.value.document.pages[0].elements.first { it.id == blankId } as TextElement
        assertEquals(false, blank.style.bold)
        assertTrue(announced.isEmpty())
    }

    @Test
    fun the_reframe_keyboard_grammar_still_owns_the_keyboard_during_a_session() {
        // Regression: the canvas receiver now serves two grammars. Reframe must keep its own — and a
        // photo has no styleTarget anyway, so B/I stay inert.
        val s = store()
        s.dispatch(
            Intent.CommitAddImage(
                ImageElement(id = "photo", transform = Transform(50.0, 50.0, 100.0, 80.0), assetId = "a"),
            ),
        )
        render(s)
        // The reducer mints the id (`el-<token>`), so read it back rather than assume the one we passed.
        val photoId = s.uiState.value.document.pages[0].elements.first { it is ImageElement }.id
        s.dispatch(Intent.BeginReframe(photoId))
        composeRule.waitForIdle()
        assertTrue("session did not open", s.uiState.value.interaction is Interaction.Reframing)

        pressKey(NativeKeyEvent.KEYCODE_ESCAPE, meta = 0)
        assertTrue("Escape did not cancel the Reframe session", s.uiState.value.interaction !is Interaction.Reframing)
    }

    // ── Haptics (ADR-055 §8, bench `buzz("tick")`) ─────────────────────────────────────────────────

    @Test
    fun every_accepted_style_change_buzzes_once_on_both_the_pointer_and_keyboard_paths() {
        val s = storeWithText()
        render(s)
        openTypeBar()
        haptics.events.clear()

        composeRule.onNodeWithContentDescription("Bold").performClick()
        composeRule.waitForIdle()
        assertEquals(1, haptics.events.size)

        composeRule.onNodeWithContentDescription("Center").performClick()
        composeRule.waitForIdle()
        assertEquals(2, haptics.events.size)

        composeRule.onNodeWithContentDescription("Teal").performClick()
        composeRule.waitForIdle()
        assertEquals(3, haptics.events.size)

        // The keyboard path buzzes on the same verb, not on a second implementation of it.
        pressKey(NativeKeyEvent.KEYCODE_I)
        assertEquals(4, haptics.events.size)
        assertTrue(haptics.events.all { it == HapticFeedbackType.TextHandleMove })
    }

    @Test
    fun the_size_stepper_buzzes_per_tap_and_stays_silent_at_a_ramp_end() {
        // bench: the buzz confirms the TAP (three taps, three buzzes) while the settle coalesces the
        // three into one commit. Silence at a ramp end is deliberate: bench reaches its `buzz("boundary")`
        // only through a stale bar (see [rememberStyleBuzz]), which this surface cannot go.
        val s = storeWithText()
        render(s)
        openTypeBar()
        haptics.events.clear()

        composeRule.onNodeWithContentDescription("Larger").performClick()
        composeRule.onNodeWithContentDescription("Larger").performClick()
        composeRule.waitForIdle()
        assertEquals(2, haptics.events.size)
        settleSize { textOf(s).style.sizePt == 16.0 }
        // Still two: the commit is not a third buzz.
        assertEquals(2, haptics.events.size)

        // Walk to the bottom of the ramp; the disabled "Smaller" cannot fire.
        s.dispatch(Intent.StyleText(id = textOf(s).id, sizePt = TypeSizesPt.first()))
        composeRule.waitForIdle()
        haptics.events.clear()
        composeRule.onNodeWithContentDescription("Smaller").assertIsNotEnabled().performClick()
        composeRule.waitForIdle()
        assertTrue(haptics.events.isEmpty())
    }

    /**
     * The frozen split (bench `setSize`): `applyTextStyle(b)` repaints the block **before** `sizeCommit()`
     * schedules the snapshot, so the settle coalesces the undo entry and never the preview.
     *
     * Given a size step, the in-flight style must reach the canvas *before* the intent reaches the
     * reducer — and must be withdrawn once the commit lands, or the override would outlive the document
     * state it was standing in for. Recording both channels into one ordered log is what makes the
     * *ordering* assertable rather than just the endpoints.
     */
    @Test
    fun a_size_step_previews_on_the_canvas_before_the_settle_commits() {
        val element = TextElement(
            id = "t",
            transform = Transform(40.0, 40.0, 20.0, 20.0),
            text = "hi",
            style = com.aritr.zinely.core.model.TextStyle(sizePt = 12.0),
        )
        val log = mutableListOf<String>()

        composeRule.setContent {
            ZinelyTheme {
                TypeBar(
                    element = element,
                    dispatch = { if (it is Intent.StyleText) log += "commit:${it.sizePt?.toInt()}" },
                    onAnnounce = {},
                    onPreview = { log += "preview:" + (it?.values?.first()?.sizePt?.toInt()?.toString() ?: "none") },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Larger").performClick()
        // The settle coroutine only starts on the recomposition the tap schedules, so pump composition
        // first and then wait the window out — the same order [settleSize] documents.
        settleSize { log.contains("commit:14") }

        // 12pt is ramp index 1, so one step up is 14pt on both channels.
        val previewedAt = log.indexOf("preview:14")
        val committedAt = log.indexOf("commit:14")
        assertTrue("canvas never previewed the step: $log", previewedAt >= 0)
        assertTrue("reducer never got the settled commit: $log", committedAt >= 0)
        assertTrue("preview must precede the commit, got $log", previewedAt < committedAt)
        // The override is withdrawn once the document owns the size — nothing stale left on the canvas.
        assertEquals("preview:none", log.last())
    }
}
