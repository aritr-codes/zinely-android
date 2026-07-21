package com.aritr.zinely.feature.editor

import android.view.KeyEvent as NativeKeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performClick
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
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The Compose Reframe surface (ADR-053, IF2): the visual layer drives the ephemeral draft and bakes exactly
 * one [Intent.CommitReframe] via a real [EditorStore], so we assert on the store — the single source of
 * truth. Reframe mode swaps its chrome in over the supply tray; Cancel writes nothing; page switch ends the
 * session. Robolectric NATIVE.
 *
 * The photo is a real decodable master ([reframeTestPhoto]) sized to the element's own `1.25` box aspect,
 * so `pratio == bratio` and the crop expectations below are the same numbers they always were — but the
 * session now runs the shipping path rather than the empty-source fallback M7-01 removed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReframeSessionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(300.0, 300.0)
    private val photo = reframeTestPhoto()

    private fun store(pages: Int = 1): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        val s = EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = (0 until pages).map { Page(index = it, role = PageRole.INTERIOR) },
                ),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
        // A photo at (50,50) 100×80 — CommitAddImage forces Fit.FILL and selects it.
        s.dispatch(Intent.CommitAddImage(ImageElement(id = "seed", transform = Transform(50.0, 50.0, 100.0, 80.0), assetId = "a")))
        return s
    }

    private fun imageId(s: EditorStore) = s.uiState.value.selection.single()
    private fun image(s: EditorStore) =
        s.uiState.value.document.pages[s.uiState.value.currentPageIndex].elements
            .first { it is ImageElement } as ImageElement

    private fun render(s: EditorStore, bytes: AssetBytesSource = photo) {
        composeRule.setContent {
            ZinelyTheme {
                val state by s.uiState.collectAsState()
                // Read state so the host recomposes; EditorScreen itself collects the store too.
                @Suppress("UNUSED_EXPRESSION") state
                EditorScreen(store = s, pageSizePt = pageSizePt, imageBytes = bytes)
            }
        }
    }

    @Test
    fun double_tap_on_a_photo_enters_reframe_mode() {
        val s = store()
        render(s)
        s.dispatch(Intent.DoubleTapAt(PtPoint(100.0, 90.0))) // inside the 50..150 × 50..130 box
        composeRule.waitForIdle()

        assertTrue("Reframe session open", s.uiState.value.interaction is Interaction.Reframing)
        composeRule.onNodeWithTag(ReframeControlsTestTag).assertIsDisplayed()
        // Reframe chrome replaces the supply tray + hides the context bar.
        composeRule.onNodeWithTag(EditorSupplyTrayTestTag).assertDoesNotExist()
    }

    /**
     * **Keyboard ownership is immediate on entry (M7-01-R1).**
     *
     * The regression: M7-01's entry-refusal gate moved `requestFocus()` to *after* a
     * `withContext(Dispatchers.IO)` readability read, so the session existed in the reducer while
     * keystrokes went nowhere — Escape pressed straight after entering Reframe was silently swallowed.
     * `waitForIdle()` does not synchronise with that dispatcher, which is why the window was real rather
     * than theoretical.
     *
     * Asserted through the store, and deliberately with **no wait between entry and the keypress** —
     * waiting would paper over exactly the window the defect lived in.
     */
    @Test
    fun escape_cancels_immediately_after_entering_reframe() {
        val s = store()
        val id = imageId(s)
        render(s)
        s.dispatch(Intent.BeginReframe(id))
        assertTrue("session open", s.uiState.value.interaction is Interaction.Reframing)

        composeRule.onNodeWithTag(EditorCanvasTestTag).performKeyPress(
            KeyEvent(NativeKeyEvent(0L, 0L, NativeKeyEvent.ACTION_DOWN, NativeKeyEvent.KEYCODE_ESCAPE, 0, 0)),
        )
        composeRule.waitForIdle()

        assertTrue(
            "Escape must cancel even when pressed before the readability read completes",
            s.uiState.value.interaction is Interaction.Idle,
        )
        assertEquals("a cancelled session writes nothing", Crop.FULL, image(s).crop)
    }

    /**
     * **Entry refusal (M7-01 / RF-4, founder Choice 1).** A photo whose intrinsic size cannot be read
     * cannot be framed, so the session must be declined rather than opened inert — and the document must
     * be left exactly as it was.
     *
     * Asserted through the store: the interaction returns to Idle and no framing is written. The refusal
     * is currently silent because the explanatory line is founder-owned and still outstanding
     * (`ReframeUnavailableAnnouncement`); when that lands, this test stands and an announcement assertion
     * joins it.
     */
    @Test
    fun an_unreadable_photo_is_refused_entry_to_reframe() {
        val s = store()
        val id = imageId(s)
        render(s, AssetBytesSource { null }) // nothing to measure, nothing to show
        s.dispatch(Intent.BeginReframe(id))
        composeRule.waitForIdle()

        assertTrue("the session must be declined", s.uiState.value.interaction is Interaction.Idle)
        assertEquals("framing must be untouched", Crop.FULL, image(s).crop)
        assertEquals("framing must be untouched", Fit.FILL, image(s).fit)
        // Nothing of the session may be presented. Both surfaces are gated on the readability check
        // resolving true, so a refused session composes neither — the editor keeps its ordinary chrome.
        composeRule.onNodeWithTag(ReframeControlsTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ReframeOverlayTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(EditorSupplyTrayTestTag).assertIsDisplayed()
    }

    /**
     * **INV-01 failure mode 3, as a regression guard (M7-01).** A master the renderer can measure but the
     * overlay cannot display must leave the session completely inert: the verbs do nothing and Done writes
     * the element back unchanged. The defect was precisely the opposite — the controls stayed live, the
     * draft moved, and the commit baked a crop against a photo the user had never seen, which the page
     * then rendered letterboxed.
     *
     * Asserted through the store, so this is the committed document, not the overlay's opinion of it.
     */
    @Test
    fun an_undisplayable_photo_commits_nothing_and_records_no_command() {
        val s = store()
        val id = imageId(s)
        render(s, reframeTestPhotoMeasurableOnly())
        s.dispatch(Intent.BeginReframe(id))
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Zoom in").performClick()
        composeRule.onNodeWithContentDescription("Zoom in").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Done reframing").performClick()
        composeRule.waitForIdle()

        assertTrue("session closed", s.uiState.value.interaction is Interaction.Idle)
        assertEquals("framing must be untouched", Crop.FULL, image(s).crop)
        assertEquals("framing must be untouched", Fit.FILL, image(s).fit)

        // ...and no command was recorded at all. One undo therefore walks back past the *placement*,
        // removing the photo — which it could only do if the session pushed nothing of its own. (Contrast
        // `done_bakes_a_zoom_as_one_undoable_edit`, where the same single undo lands on the Fill baseline
        // because the reframe *did* record a command.)
        s.dispatch(Intent.Undo)
        val remaining = s.uiState.value.document.pages[s.uiState.value.currentPageIndex].elements
            .filterIsInstance<ImageElement>()
        assertTrue("an inert session must record no command", remaining.isEmpty())
    }

    @Test
    fun done_bakes_a_zoom_as_one_undoable_edit() {
        val s = store()
        val id = imageId(s)
        render(s)
        s.dispatch(Intent.BeginReframe(id))
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Zoom in").performClick()
        composeRule.onNodeWithContentDescription("Zoom in").performClick()
        // Mid-session the document is untouched — the draft is feature-ephemeral.
        assertEquals(Crop.FULL, image(s).crop)

        composeRule.onNodeWithContentDescription("Done reframing").performClick()
        composeRule.waitForIdle()

        assertTrue("session closed", s.uiState.value.interaction is Interaction.Idle)
        assertEquals("a zoomed Fill persists as FIT over a shrunk crop", Fit.FIT, image(s).fit)
        assertNotEquals(Crop.FULL, image(s).crop)

        // Exactly one command: one undo restores the placement baseline.
        s.dispatch(Intent.Undo)
        assertEquals(Fit.FILL, image(s).fit)
        assertEquals(Crop.FULL, image(s).crop)
    }

    @Test
    fun cancel_writes_nothing() {
        val s = store()
        val id = imageId(s)
        render(s)
        s.dispatch(Intent.BeginReframe(id))
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Zoom in").performClick()
        composeRule.onNodeWithContentDescription("Cancel reframing").performClick()
        composeRule.waitForIdle()

        assertTrue("session closed", s.uiState.value.interaction is Interaction.Idle)
        assertEquals("cancel discards the draft", Fit.FILL, image(s).fit)
        assertEquals(Crop.FULL, image(s).crop)
    }

    @Test
    fun whole_photo_commits_as_fit_over_the_full_crop() {
        val s = store()
        val id = imageId(s)
        render(s)
        s.dispatch(Intent.BeginReframe(id))
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Whole photo").performClick()
        composeRule.onNodeWithContentDescription("Done reframing").performClick()
        composeRule.waitForIdle()

        assertEquals(Fit.FIT, image(s).fit)
        assertEquals(Crop.FULL, image(s).crop)
    }

    @Test
    fun switching_page_commits_the_open_framing_and_ends_the_session() {
        val s = store(pages = 2)
        val id = imageId(s)
        render(s)
        s.dispatch(Intent.BeginReframe(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Zoom in").performClick()
        composeRule.onNodeWithTag(ReframeControlsTestTag).assertIsDisplayed()

        // Tap the page-2 card: the host commits the open framing before navigating (bench: never strand a
        // session on an off-screen photo). Exercises the real onSelectPage wrapper, not a raw GoToPage.
        composeRule.onNodeWithContentDescription("Page 2").performClick()
        composeRule.waitForIdle()

        assertTrue("session cleaned up on page switch", s.uiState.value.interaction is Interaction.Idle)
        assertEquals("moved to page 2", 1, s.uiState.value.currentPageIndex)
        composeRule.onNodeWithTag(ReframeControlsTestTag).assertDoesNotExist()
        // The zoom was baked on the way out (photo lives on page 0).
        val photo = s.uiState.value.document.pages[0].elements.first { it is ImageElement } as ImageElement
        assertEquals(Fit.FIT, photo.fit)
        assertNotEquals(Crop.FULL, photo.crop)
    }
}
