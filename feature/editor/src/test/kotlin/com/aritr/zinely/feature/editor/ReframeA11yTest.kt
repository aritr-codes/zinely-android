package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.aritr.zinely.render.android.AssetBytesSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The Reframe accessibility contract (ADR-053, IF3): the discrete controls, the element custom actions, the
 * hardware keyboard, and the screen-reader live region all drive the SAME reducer/draft path as the
 * gestures — so a TalkBack / Switch Access / keyboard user reaches every reframe verb, and each one speaks.
 * Robolectric NATIVE, full [EditorScreen] over a real [EditorStore] (the single source of truth).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReframeA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(300.0, 300.0)

    private fun store(): EditorStore {
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
        s.dispatch(Intent.CommitAddImage(ImageElement(id = "seed", transform = Transform(50.0, 50.0, 100.0, 80.0), assetId = "a")))
        return s
    }

    private fun imageId(s: EditorStore) = s.uiState.value.selection.single()
    private fun image(s: EditorStore) =
        s.uiState.value.document.pages[0].elements.first { it is ImageElement } as ImageElement

    private var coachSeenCalls = 0
    private val announced = mutableListOf<String>()

    // A real decodable photo at the element's own 1.25 box aspect: Reframe verbs are inert until the photo
    // is genuinely on screen (M7-01), and every announcement below is the response to a verb.
    // `by lazy` for the reason given in ReframeSessionTest: the fixture's assumption must be thrown
    // from inside the test body, not from the constructor, to be honoured as a skip.
    private val photo by lazy { reframeTestPhoto() }

    private fun render(s: EditorStore, coachSeen: Boolean? = true, bytes: AssetBytesSource = photo) {
        coachSeenCalls = 0
        announced.clear()
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(
                    store = s,
                    pageSizePt = pageSizePt,
                    imageBytes = bytes,
                    reframeCoachSeen = coachSeen,
                    onReframeCoachSeen = { coachSeenCalls++ },
                    onReframeAnnounce = { announced += it },
                )
            }
        }
    }

    /** Invoke a custom accessibility action on this node by its spoken label (the Switch/TalkBack path). */
    private fun SemanticsNodeInteraction.invokeCustomAction(label: String) {
        val actions = fetchSemanticsNode().config[SemanticsActions.CustomActions]
        actions.first { it.label == label }.action()
    }

    @Test
    fun the_reframe_custom_action_enters_reframe() {
        val s = store()
        val id = imageId(s)
        render(s)
        composeRule.onNodeWithTag("$ElementNodeTagPrefix$id").invokeCustomAction("Reframe photo")
        composeRule.waitForIdle()

        assertTrue("custom action opened the session", s.uiState.value.interaction is Interaction.Reframing)
        composeRule.onNodeWithTag(ReframeControlsTestTag).assertIsDisplayed()
    }

    @Test
    fun the_reset_framing_custom_action_reverts_to_the_placement_default() {
        val s = store()
        val id = imageId(s)
        render(s)
        // First bake a real reframe (zoom) so there is something to reset.
        s.dispatch(Intent.BeginReframe(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Zoom in").performClick()
        composeRule.onNodeWithContentDescription("Done reframing").performClick()
        composeRule.waitForIdle()
        assertNotEquals(Crop.FULL, image(s).crop)

        // The a11y "Reset framing" action (one-shot Intent.ResetFraming) → back to Fill / full crop.
        composeRule.onNodeWithTag("$ElementNodeTagPrefix$id").invokeCustomAction("Reset framing")
        composeRule.waitForIdle()
        assertEquals(Fit.FILL, image(s).fit)
        assertEquals(Crop.FULL, image(s).crop)
    }

    @Test
    fun entering_reframe_announces_and_marks_the_coach_seen() {
        val s = store()
        val id = imageId(s)
        render(s, coachSeen = false)
        s.dispatch(Intent.BeginReframe(id))
        composeRule.waitForIdle()

        assertEquals(
            "Reframing photo. Drag to reposition, pinch to zoom, or use the on-screen move and zoom " +
                "controls. Done saves, Cancel discards.",
            announced.last(),
        )
        assertTrue("first reframe persists the coach-seen flag", coachSeenCalls >= 1)
    }

    @Test
    fun a_zoom_step_announces_the_new_percent() {
        val s = store()
        val id = imageId(s)
        render(s)
        s.dispatch(Intent.BeginReframe(id))
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Zoom in").performClick()
        composeRule.waitForIdle()
        // 100% × 1.15 → 115%.
        assertEquals("Zoom 115 percent", announced.last())
    }

    /**
     * **The host-side gate, pinned on its own (M7-01).** When the photo is measurable but cannot be
     * displayed, every adjustment verb must be inert. This asserts that strand independently of the
     * commit-side guard: if the `reframePratio != null` gate is removed from `reframeZoom`, the zoom runs
     * and speaks, and this fails — even though the commit would still write nothing.
     *
     * Silence is asserted rather than a spoken line because M7-01 deliberately added no new copy: what to
     * say (or whether to disable the controls outright) is a founder/designer decision, still open. This
     * test therefore pins the *current* behaviour honestly and will need updating when that lands — which
     * is the point of writing it down rather than leaving the strand untested.
     */
    // TODO(#57): restore this test — quarantined for the 0.9.0-beta.1 release freeze, not deleted.
    //
    // It is flaky because its FIXTURE's precondition is order-dependent, not because the behaviour it
    // asserts is unstable. `reframeTestPhotoMeasurableOnly()` serves real bytes on the first `open` and
    // nothing after, so it only produces "measurable but not displayable" if the overlay reads bounds
    // BEFORE it decodes pixels — and that ordering is two LaunchedEffects. Under CI load it sometimes
    // flips: the decode wins, the photo really is displayable, the verbs legitimately act and speak, and
    // the assertion below fails. A failing precondition, dressed as a failing assertion.
    //
    // This is NOT the decoder-exhaustion problem that `forkEvery = 50` addresses; that mitigation took
    // the failing set from two tests down to this one, and cannot fix this one.
    //
    // The invariant is not left uncovered. The commit-side half — an undisplayable photo cannot bake a
    // crop — is still asserted and still runs, in
    // ReframeSessionTest.an_undisplayable_photo_commits_nothing_and_records_no_command. What is
    // unguarded until #57 lands is specifically the "…and stays silent" clause.
    //
    // The fix is to establish the precondition through a seam rather than by inferring it from which
    // consumer opens the stream first. That is a test-architecture change, deliberately out of scope
    // during the freeze.
    @Ignore("Flaky: fixture precondition is order-dependent — see #57. Commit-side coverage retained.")
    @Test
    fun an_undisplayable_photo_leaves_the_adjustment_verbs_inert_and_silent() {
        val s = store()
        val id = imageId(s)
        render(s, bytes = reframeTestPhotoMeasurableOnly())
        s.dispatch(Intent.BeginReframe(id))
        composeRule.waitForIdle()

        announced.clear() // drop the session-entry announcement; we are testing the verbs
        composeRule.onNodeWithContentDescription("Zoom in").performClick()
        composeRule.onNodeWithContentDescription("Zoom out").performClick()
        composeRule.waitForIdle()

        assertTrue("no verb may act or speak while the photo is undisplayable", announced.isEmpty())
    }

    @Test
    fun an_identical_repeated_nudge_speaks_every_time_rather_than_going_silent() {
        // The announce path force-speaks even identical consecutive text (Review finding #1): three left
        // nudges must emit three "Moved left"s, not one — Compose's change-only live region would drop the
        // repeats, so the announcement is routed through the platform announceForAccessibility drain.
        val s = store()
        val id = imageId(s)
        render(s)
        s.dispatch(Intent.BeginReframe(id))
        composeRule.waitForIdle()

        // Zoom in first to give the photo somewhere to go. This fixture's photo is sized to the element's
        // own box aspect, so at 100% Fill the crop spans the whole image and the pan is clamped to zero on
        // both axes — the pad is correctly disabled there. Before the D3 fix this test passed *without* the
        // zoom, counting three "Moved left"s for a photo that had not moved at all: the phantom
        // announcement was the very defect, and the test had pinned it.
        //
        // Twice, not once: one step leaves ±0.065 of travel against a 0.05 nudge, so the third nudge would
        // hit the clamp and correctly refuse instead of speaking. Three announcements need three real
        // moves — which is the point of the test, and is now something the fixture has to earn.
        repeat(2) {
            composeRule.onNodeWithContentDescription("Zoom in").performClick()
            composeRule.waitForIdle()
        }

        repeat(3) {
            composeRule.onNodeWithContentDescription("Move photo left").performClick()
            composeRule.waitForIdle()
        }
        assertEquals(3, announced.count { it == "Moved left" })
    }

    /**
     * D3 — an adjustment that cannot change anything must not be painted as a live control.
     *
     * The three rules that make a verb inert ([Framing.abilities]) are invisible in the UI: the Fill zoom
     * floor, the pan clamp, and "Whole photo ignores pan and zoom" entirely. A lit, tappable, haptic button
     * that does nothing reads as *the app has frozen*, which is why the reported symptom was "reframe is
     * broken after reopening" — a photo saved as Whole photo reopens in the one mode where every adjustment
     * verb is dead.
     */
    @Test
    fun an_adjustment_that_cannot_act_is_disabled_rather_than_lit() {
        val s = store()
        val id = imageId(s)
        render(s)
        s.dispatch(Intent.BeginReframe(id))
        composeRule.waitForIdle()

        // A fresh Fill sits at the zoom floor, and this photo matches its frame's aspect, so the cover crop
        // already spans the image: only "zoom in" can do anything at all.
        composeRule.onNodeWithContentDescription("Zoom in").assertIsEnabled()
        composeRule.onNodeWithContentDescription("Zoom out").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Move photo left").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Move photo up").assertIsNotEnabled()

        // Zooming in opens pan room on both axes and lifts the floor.
        composeRule.onNodeWithContentDescription("Zoom in").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Zoom out").assertIsEnabled()
        composeRule.onNodeWithContentDescription("Move photo left").assertIsEnabled()

        // Whole photo discards pan and zoom, so the entire stepper pill goes unavailable together — the
        // fit segments remain live, because switching back to Fill is the way out.
        composeRule.onNodeWithContentDescription("Whole photo").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Zoom in").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Zoom out").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Move photo left").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Move photo down").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Fill").assertIsEnabled()
    }

    /**
     * The hardware keyboard reaches the same verbs the disabled buttons do, so it must refuse the same
     * adjustments — but silently refusing is worse for a screen-reader user than the phantom "Moved left"
     * it replaces. It says why, and names the way out.
     */
    @Test
    fun a_refused_keystroke_says_why_instead_of_claiming_a_move() {
        val s = store()
        val id = imageId(s)
        render(s)
        s.dispatch(Intent.BeginReframe(id))
        composeRule.waitForIdle()

        announced.clear() // drop the session-entry line; we are testing the refusal
        composeRule.onNodeWithTag(EditorCanvasTestTag).performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.waitForIdle()
        assertEquals("No room to move that way.", announced.last())

        composeRule.onNodeWithTag(EditorCanvasTestTag).performKeyInput { pressKey(Key.Minus) }
        composeRule.waitForIdle()
        assertEquals("Already at the smallest zoom.", announced.last())

        // The clamp edge, which is the subtle case: the axis is live (so the arrow stays enabled and does
        // not flicker), but this particular step has nowhere left to go. Announcing the direction here
        // would be the same phantom move D3 exists to remove, one state further in. Zoom once for a little
        // travel, then walk left until it runs out.
        composeRule.onNodeWithContentDescription("Zoom in").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Move photo left").assertIsEnabled()
        repeat(3) {
            composeRule.onNodeWithTag(EditorCanvasTestTag).performKeyInput { pressKey(Key.DirectionLeft) }
            composeRule.waitForIdle()
        }
        assertEquals("No room to move that way.", announced.last())
        composeRule.onNodeWithContentDescription("Move photo left").assertIsEnabled()

        composeRule.onNodeWithContentDescription("Whole photo").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditorCanvasTestTag).performKeyInput { pressKey(Key.Equals) }
        composeRule.waitForIdle()
        assertEquals(WholePhotoInertLine, announced.last())
    }

    @Test
    fun the_keyboard_enter_saves_and_esc_cancels() {
        val s = store()
        val id = imageId(s)
        render(s)
        s.dispatch(Intent.BeginReframe(id))
        composeRule.waitForIdle()

        // Arrow + zoom via the hardware keyboard mutate the draft (doc untouched mid-session).
        composeRule.onNodeWithTag(EditorCanvasTestTag).performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag(EditorCanvasTestTag).performKeyInput { pressKey(Key.Equals) }
        assertEquals("mid-session the document is untouched", Crop.FULL, image(s).crop)

        // Enter commits the open framing as one edit.
        composeRule.onNodeWithTag(EditorCanvasTestTag).performKeyInput { pressKey(Key.Enter) }
        composeRule.waitForIdle()
        assertTrue("Enter closed the session", s.uiState.value.interaction is Interaction.Idle)
        assertEquals(Fit.FIT, image(s).fit)
        assertNotEquals(Crop.FULL, image(s).crop)

        // Re-open and Escape: writes nothing.
        s.dispatch(Intent.BeginReframe(id))
        composeRule.waitForIdle()
        val beforeCrop = image(s).crop
        composeRule.onNodeWithTag(EditorCanvasTestTag).performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.onNodeWithTag(EditorCanvasTestTag).performKeyInput { pressKey(Key.Escape) }
        composeRule.waitForIdle()
        assertTrue("Escape closed the session", s.uiState.value.interaction is Interaction.Idle)
        assertEquals("Escape discarded the draft", beforeCrop, image(s).crop)
    }
}
