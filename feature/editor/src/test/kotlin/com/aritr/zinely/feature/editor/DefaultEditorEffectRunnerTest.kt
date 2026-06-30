package com.aritr.zinely.feature.editor

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
 * the supplied (main-confined) dispatch / onto the announcer, never silently dropped (Codex rec #6).
 */
class DefaultEditorEffectRunnerTest {

    private val dispatcher = StandardTestDispatcher()

    private fun runner(
        scope: TestScope,
        pipeline: ImagePickDecodePipeline,
        autosave: AutosaveSink = AutosaveSink {},
        announcer: Announcer = Announcer {},
        savedSignal: SavedSignal = SavedSignal {},
    ) = DefaultEditorEffectRunner(
        scope = scope,
        io = dispatcher,
        main = dispatcher,
        autosave = autosave,
        imagePipeline = pipeline,
        announcer = announcer,
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
    fun announceEffect_reachesAnnouncer() = runTest(dispatcher) {
        val said = mutableListOf<String>()
        val r = runner(this, UnavailableImagePipeline, announcer = { said += it })
        r.run(Effect.Announce("Selected text"), dispatch = {})
        assertEquals(listOf("Selected text"), said)
    }

    @Test
    fun pickSuccess_dispatchesCommitAddImage() = runTest(dispatcher) {
        val dispatched = mutableListOf<Intent>()
        val r = runner(this, pipeline = { ImagePickResult.Success(image()) })
        r.run(Effect.PickAndDecodeImage) { dispatched += it }
        advanceUntilIdle()
        assertEquals(1, dispatched.size)
        assertTrue(dispatched.single() is Intent.CommitAddImage)
    }

    @Test
    fun pickFailure_announcesMessage_noDispatch() = runTest(dispatcher) {
        val dispatched = mutableListOf<Intent>()
        val said = mutableListOf<String>()
        val r = runner(this, pipeline = { ImagePickResult.Failure("decode failed") }, announcer = { said += it })
        r.run(Effect.PickAndDecodeImage) { dispatched += it }
        advanceUntilIdle()
        assertTrue(dispatched.isEmpty())
        assertEquals(listOf("decode failed"), said)
    }

    @Test
    fun pickCancelled_isSilentNoOp() = runTest(dispatcher) {
        val dispatched = mutableListOf<Intent>()
        val said = mutableListOf<String>()
        val r = runner(this, pipeline = { ImagePickResult.Cancelled }, announcer = { said += it })
        r.run(Effect.PickAndDecodeImage) { dispatched += it }
        advanceUntilIdle()
        assertTrue(dispatched.isEmpty())
        assertTrue(said.isEmpty())
    }

    @Test
    fun unavailablePlaceholder_reportsFailure() = runTest(dispatcher) {
        assertTrue(UnavailableImagePipeline.pickAndDecode() is ImagePickResult.Failure)
    }

    private fun stubDoc() = com.aritr.zinely.core.model.ZineDocument(
        format = com.aritr.zinely.core.model.ZineFormat.SINGLE_SHEET_8,
        paperSize = com.aritr.zinely.core.model.PaperSize.LETTER,
    )
}
