package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.editor.EditKind
import com.aritr.zinely.core.editor.EditLabel
import com.aritr.zinely.core.editor.EditVerb
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Transform
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM proof of [DefaultEditorEffectRunner] — the effect→I/O routing Codex reviewed (spike §10.3).
 * Verifies each [Effect] reaches the right seam and that background image results are routed back through
 * the supplied (main-confined) dispatch, never silently dropped (Codex rec #6). A failed pick is dispatched
 * as [Intent.ReportImagePickFailure] for the Bench snack to show (A41.3); the runner speaks nothing itself.
 */
class DefaultEditorEffectRunnerTest {

    private val dispatcher = StandardTestDispatcher()

    private fun runner(
        scope: TestScope,
        pipeline: ImagePickDecodePipeline,
        autosave: AutosaveSink = AutosaveSink {},
        savedSignal: SavedSignal = SavedSignal {},
    ) = DefaultEditorEffectRunner(
        scope = scope,
        io = dispatcher,
        autosave = autosave,
        imagePipeline = pipeline,
        savedSignal = savedSignal,
    )

    private fun image() = ImageElement(
        id = "placeholder",
        transform = Transform(0.0, 0.0, 10.0, 10.0),
        assetId = "sha-abc",
    )

    @Test
    fun autosaveEffect_marksBinderDirty() = runTest(dispatcher) {
        var dirty = 0
        val r = runner(this, UnavailableImagePipeline, autosave = { dirty++ })
        r.run(Effect.Autosave(document = stubDoc()), dispatch = {})
        assertEquals(1, dirty)
    }

    @Test
    fun autosaveEffect_signalsSaved() = runTest(dispatcher) {
        // The same autosave event that marks the binder dirty also raises the transient "Saved ✨"
        // confirmation — one save signal, two consumers (persist + reassure), never a second save system.
        var saved = 0
        val r = runner(this, UnavailableImagePipeline, savedSignal = { saved++ })
        r.run(Effect.Autosave(document = stubDoc()), dispatch = {})
        assertEquals(1, saved)
    }

    @Test
    fun historyStep_isNeverHandledByTheRunner() = runTest(dispatcher) {
        // ADR-123: the screen speaks an undo line once through the host drain; the runner must not act on it.
        val dispatched = mutableListOf<Intent>()
        val r = runner(this, UnavailableImagePipeline)
        r.run(Effect.HistoryStepped(EditLabel(EditVerb.DELETE, EditKind.PHOTO), isRedo = false, landedOnPage = 2)) { dispatched += it }
        advanceUntilIdle()
        assertEquals(emptyList<Intent>(), dispatched)
    }

    @Test
    fun pickSuccess_dispatchesCommitAddImage() = runTest(dispatcher) {
        val dispatched = mutableListOf<Intent>()
        val r = runner(this, pipeline = { ImagePickResult.Success(image()) })
        r.run(Effect.PickAndDecodeImage()) { dispatched += it }
        advanceUntilIdle()
        assertEquals(1, dispatched.size)
        assertTrue(dispatched.single() is Intent.CommitAddImage)
    }

    @Test
    fun failed_pick_reports_a_visible_failure() = runTest(dispatcher) {
        // Given a pipeline that fails with "X", When the pick runs
        val dispatched = mutableListOf<Intent>()
        val r = runner(this, pipeline = { ImagePickResult.Failure("X") })
        r.run(Effect.PickAndDecodeImage()) { dispatched += it }
        advanceUntilIdle()
        // Then the one signal is the visible failure (the snack speaks it; nothing else does, A41.3)
        assertEquals(listOf<Intent>(Intent.ReportImagePickFailure("X")), dispatched)
    }

    @Test
    fun pickCancelled_isSilentNoOp() = runTest(dispatcher) {
        val dispatched = mutableListOf<Intent>()
        val r = runner(this, pipeline = { ImagePickResult.Cancelled })
        r.run(Effect.PickAndDecodeImage()) { dispatched += it }
        advanceUntilIdle()
        assertTrue(dispatched.isEmpty())
    }

    @Test
    fun unavailablePlaceholder_reportsFailure() = runTest(dispatcher) {
        assertTrue(UnavailableImagePipeline.pickAndDecode() is ImagePickResult.Failure)
    }

    @Test
    fun targetedPickSuccess_dispatchesReplaceImage_withOnlyTheNewAsset() = runTest(dispatcher) {
        val dispatched = mutableListOf<Intent>()
        val decoded = image().copy(id = "ignored", transform = Transform(99.0, 99.0, 1.0, 1.0))
        val r = runner(this, pipeline = { ImagePickResult.Success(decoded) })

        r.run(Effect.PickAndDecodeImage(replacingId = "photo-1")) { dispatched += it }
        advanceUntilIdle()

        assertEquals(listOf(Intent.ReplaceImage("photo-1", "sha-abc")), dispatched)
    }

    @Test
    fun targetedPickCancelOrFailure_replacesNothing() = runTest(dispatcher) {
        val cancelled = mutableListOf<Intent>()
        runner(this, pipeline = { ImagePickResult.Cancelled })
            .run(Effect.PickAndDecodeImage(replacingId = "photo-1")) { cancelled += it }
        advanceUntilIdle()
        assertTrue(cancelled.isEmpty())

        val failed = mutableListOf<Intent>()
        runner(this, pipeline = { ImagePickResult.Failure("decode failed") })
            .run(Effect.PickAndDecodeImage(replacingId = "photo-1")) { failed += it }
        advanceUntilIdle()
        assertEquals("a failed Replace is shown, and replaces nothing", listOf<Intent>(Intent.ReportImagePickFailure("decode failed")), failed)
    }

    private fun stubDoc() = com.aritr.zinely.core.model.ZineDocument(
        format = com.aritr.zinely.core.model.ZineFormat.SINGLE_SHEET_8,
        paperSize = com.aritr.zinely.core.model.PaperSize.LETTER,
    )
}
