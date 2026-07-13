package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
 * session. Robolectric NATIVE. Photo bytes are the empty source (no decode) so the frame aspect stands in
 * for the photo aspect — enough to exercise zoom → crop.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReframeSessionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(300.0, 300.0)

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

    private fun render(s: EditorStore) {
        composeRule.setContent {
            ZinelyTheme {
                val state by s.uiState.collectAsState()
                // Read state so the host recomposes; EditorScreen itself collects the store too.
                @Suppress("UNUSED_EXPRESSION") state
                EditorScreen(store = s, pageSizePt = pageSizePt)
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
