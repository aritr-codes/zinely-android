package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
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

        repeat(3) {
            composeRule.onNodeWithContentDescription("Move photo left").performClick()
            composeRule.waitForIdle()
        }
        assertEquals(3, announced.count { it == "Moved left" })
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
