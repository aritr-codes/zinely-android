package com.aritr.zinely.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aritr.zinely.core.data.asset.AssetStore
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.data.storage.DocumentSnapshotProvider
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.imposition.Imposer
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.data.android.EditorAutosaveBinder
import com.aritr.zinely.data.android.SaveFailureSink
import com.aritr.zinely.data.android.di.EditorAutosaveBinderFactory
import com.aritr.zinely.data.android.prefs.EditorOnboardingStore
import com.aritr.zinely.feature.editor.Announcer
import com.aritr.zinely.feature.editor.AutosaveSink
import com.aritr.zinely.feature.editor.DefaultEditorEffectRunner
import com.aritr.zinely.feature.editor.EditorStore
import com.aritr.zinely.feature.editor.SaveErrorKind
import com.aritr.zinely.feature.editor.SavedSignal
import com.aritr.zinely.render.android.AssetBytesSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * The bootstrap state the editor host renders (ADR-030 §3, Codex rec B). The store/binder are built
 * once, on the main thread, only after the document is loaded — never published half-constructed.
 */
internal sealed interface EditorBootState {
    /** Loading/seeding the document. */
    data object Loading : EditorBootState

    /** Bootstrap failed (Corrupt / Invalid / Io / SchemaTooNew). [message] is user-facing, cause-free. */
    data class Error(val message: String) : EditorBootState

    /** Ready: the wired [store], the imposition-derived [pageSizePt], the lifecycle [binder], and the
     * render read-path [imageBytes] over the content-addressed master store (ADR-031 §3). */
    data class Ready(
        val store: EditorStore,
        val pageSizePt: PtSize,
        val binder: EditorAutosaveBinder,
        val imageBytes: AssetBytesSource,
    ) : EditorBootState
}

/**
 * Owns the editor's MVI [EditorStore], its effect runner, and the app-side autosave [binder] for the
 * lifetime of one open project (ADR-030 §1/§2). Lifecycle = [viewModelScope] (survives rotation).
 *
 * **Construction cycle (Codex rec A).** The store, the autosave sink, and the binder are mutually
 * dependent (sink → binder.markDirty; binder → snapshotProvider → store.uiState). The cycle is broken
 * by building the store as a `val` first, then closing the sink over a `lateinit` binder that is
 * created last — every edge is read only at runtime (markDirty / snapshot), long after construction,
 * so no half-built object is ever observed across threads.
 *
 * **Accessibility (Codex rec 1).** [Announcer] cannot hold a `LocalView` here (that is composable-only),
 * so it emits into [announcements]; the composable collects and calls `announceForAccessibility`.
 *
 * **Threading (Codex rec B).** Bootstrap runs on [viewModelScope] (Dispatchers.Main.immediate), so the
 * store — whose `dispatch` is main-thread-only by contract — is built and published on the main thread.
 */
@HiltViewModel
internal class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DocumentRepository,
    private val binderFactory: EditorAutosaveBinderFactory,
    private val saveFailureSink: SaveFailureSink,
    private val imposer: Imposer,
    private val assetStore: AssetStore,
    private val imageDecoder: ImportMasterDecoder,
    private val onboardingStore: EditorOnboardingStore,
    @param:AssetsDir private val assetsDir: File,
    // @param: pins the qualifier to the constructor value parameter (what Dagger reads) and opts out of
    // the KT-73255 default-target migration warning — same convention as EditorAutosaveBinderFactory.
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    @param:com.aritr.zinely.data.android.di.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    /** The project to open — threaded from the type-safe [EditorRoute] (Codex rec E), not a stray constant. */
    private val projectId: String = savedStateHandle.toRoute<EditorRoute>().projectId

    private val _bootState = MutableStateFlow<EditorBootState>(EditorBootState.Loading)
    val bootState: StateFlow<EditorBootState> = _bootState.asStateFlow()

    /** A11y live-region channel; the composable collects this and calls `view.announceForAccessibility`. */
    private val _announcements = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val announcements: SharedFlow<String> = _announcements.asSharedFlow()

    /**
     * Autosave-confirmation channel (ADR-034): the effect runner emits one `Unit` per `Effect.Autosave`,
     * the editor host collects it and surfaces the transient "Saved ✨" reassurance. Replay-free
     * (`replay = 0`) with a small extra buffer so `tryEmit` from the runner never blocks. A signal emitted
     * while nobody is collecting (e.g. across a rotation's subscriber gap) is simply **dropped** — by
     * design: a missed "Saved" is harmless feedback, and durability is the binder's job, never this flow's.
     */
    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    /**
     * The kind of **unresolved** autosave failure for this project, or `null` when there is none
     * (ADR-035 / [ADR-036](#adr-036)) — the honest correction to the optimistic "Saved ✨"
     * ([ADR-034](#adr-034)). Derived from the app-scoped [SaveFailureSink] (ADR-026 §5), into which the
     * autosave coordinator's background failures *and* the binder's lifecycle/teardown flush failures are
     * already reported; the project's `DataError` is mapped to the feature-local [SaveErrorKind] via
     * [toSaveErrorKind] (ADR-036) so `:feature:editor` stays free of `DataError`. Only a probe-classified
     * [DataError.OutOfSpace][com.aritr.zinely.core.data.repository.DataError.OutOfSpace] yields
     * [SaveErrorKind.OutOfSpace]; everything else is [SaveErrorKind.Generic]. `Eagerly` so a failure
     * reported during a brief subscriber gap (e.g. an ON_STOP flush while backgrounded) is still reflected
     * the moment the host re-subscribes — the upstream is a hot `StateFlow`, so the latest value is kept.
     *
     * **Auto-clear on silent recovery (ADR-037).** The coordinator's synchronous outcome listener now
     * `clear`s this project from the sink on every **durably-confirmed** save (the factory is the sole
     * feeder), so a failure that later succeeds — a background debounced save, a lifecycle flush, or a
     * teardown flush — auto-dismisses this banner the instant work is safe again. The clear is honest:
     * it only *removes* a resolved failure and never raises a positive "Saved ✨" (that cue is the
     * separate [SavedSignal] path, untouched here), so a false positive is structurally impossible. The
     * user can still dismiss manually via [dismissSaveError]. (The sink also offers `clearAll` for a
     * workspace/project switch, but the app is single-route today, so nothing invokes it yet.)
     */
    val saveError: StateFlow<SaveErrorKind?> =
        saveFailureSink.failures
            .map { it[projectId]?.error?.toSaveErrorKind() }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    /** Dismiss this project's save-failure cue (user tapped "Got it"); re-shows if a later save fails. */
    fun dismissSaveError() {
        saveFailureSink.clear(projectId)
    }

    /**
     * Retry now (user tapped "Try now" on the failure banner, ADR-038): force an immediate save attempt.
     * The outcome flows through the coordinator's ADR-037 listener — a durable success clears the banner,
     * a repeat failure re-reports it — so this method routes nothing itself. No-op until the binder exists.
     */
    fun retrySave() {
        (bootState.value as? EditorBootState.Ready)?.binder?.requestFlush()
    }

    /**
     * The picker rendezvous (ADR-031 §5). VM-held so its lifetime matches the project; the Compose host
     * [bind][PhotoPicker.bind]s its `ActivityResultLauncher` and [deliver][PhotoPicker.deliver]s results,
     * while the import pipeline [await][PhotoPicker.await]s. Single instance ⇒ single-flight is global.
     */
    val photoPicker: PhotoPicker<Uri> = PhotoPicker()

    /**
     * The across-sessions "already saw the move/resize hint" gate (ADR-032), read from the local
     * preferences store as a **load-aware tri-state**: `null` until the persisted value loads, then the
     * real `false` (fresh install) / `true`. The host shows the hint only on `false`, so the `null`
     * loading window can't flash it, yet a first gesture during that window still persists (`null != true`
     * — Codex RF1), avoiding a re-teach next launch. `Eagerly` so the load is in flight at VM creation.
     */
    val moveResizeHintSeen: StateFlow<Boolean?> =
        onboardingStore.moveResizeHintSeen
            .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    /** Persist that the move/resize hint has been seen (idempotent). Fire-and-forget on [viewModelScope]. */
    fun markMoveResizeHintSeen() {
        viewModelScope.launch { onboardingStore.markMoveResizeHintSeen() }
    }

    init {
        viewModelScope.launch(mainDispatcher) {
            // Load/seed off the UI thread: DocumentRepositoryImpl does blocking nio read/write/fsync and
            // does not hop dispatchers itself (Codex RF1), so the first-run seed save would otherwise ANR.
            val result = withContext(ioDispatcher) { bootstrapDocument(repository, projectId) }
            // Back on Main: the store's dispatch is main-thread-only by contract, so it is built here.
            _bootState.value = when (result) {
                is DataResult.Success -> try {
                    ready(EditorModel(result.value))
                } catch (ce: CancellationException) {
                    throw ce
                } catch (t: Throwable) {
                    // ready() can throw if the single-writer factory rejects a still-releasing projectId
                    // (Codex Rec1/Rec2) — surface a boot error instead of crashing or hanging on Loading.
                    EditorBootState.Error("Couldn’t open this project.")
                }
                is DataResult.Failure -> EditorBootState.Error("Couldn’t open this project.")
            }
        }
    }

    /** Build the store + effect runner + binder for [initial] on the main thread (see the cycle note). */
    private fun ready(initial: EditorModel): EditorBootState.Ready {
        // Step 1: the binder reference the sink will close over — populated in step 4, read only at
        // markDirty() time (post-construction), so the lateinit is never observed unset.
        lateinit var binder: EditorAutosaveBinder

        val announcer = Announcer { text -> _announcements.tryEmit(text) }
        val autosave = AutosaveSink { binder.markDirty() }
        // The transient "Saved ✨" reassurance: the runner fires this on every Effect.Autosave, alongside
        // the binder mark-dirty (ADR-034). tryEmit is non-blocking; a full buffer just drops a redundant
        // confirmation (the save still happens — the binder is the durability path, this is only feedback).
        val savedSignal = SavedSignal { _saved.tryEmit(Unit) }
        val pageSizePt = editedPageSize(initial.document, imposer)

        // The real import pipeline (ADR-031 §5): pick on Main via the VM-held picker, decode/store on IO.
        val imagePipeline = AndroidImagePickDecodePipeline(
            picker = photoPicker,
            decoder = imageDecoder,
            assetStore = assetStore,
            io = ioDispatcher,
            main = mainDispatcher,
            pageSizePt = pageSizePt,
        )

        // Step 2: the store (a val) — its effect runner routes Autosave → the sink, image → the
        // pick/decode/store pipeline, announce → the SharedFlow announcer.
        val store = EditorStore(
            initial = initial,
            scope = viewModelScope,
            mainDispatcher = mainDispatcher,
            effectRunner = DefaultEditorEffectRunner(
                scope = viewModelScope,
                io = ioDispatcher,
                main = mainDispatcher,
                autosave = autosave,
                imagePipeline = imagePipeline,
                announcer = announcer,
                savedSignal = savedSignal,
            ),
        )

        // Step 3: the autosave binder pulls the latest document from the live store at save time.
        val snapshotProvider = DocumentSnapshotProvider { store.uiState.value.document }

        // Step 4: create the binder last — this eagerly registers projectId with the single-writer
        // factory (ADR-026), so it happens exactly once per open project, here.
        binder = binderFactory.create(projectId, snapshotProvider)

        return EditorBootState.Ready(
            store = store,
            pageSizePt = pageSizePt,
            binder = binder,
            // Render reads masters straight from the content-addressed store dir (ADR-031 §3). Image
            // *import* (the writer) lands in Inc 2b; until then the seed doc has no images, so this just
            // resolves any future-referenced master and renders a placeholder for a missing one.
            imageBytes = FileAssetBytesSource(assetsDir),
        )
    }

    /** Flush-then-cancel the autosave for this project when the host leaves for good (ADR-030 §6). */
    override fun onCleared() {
        (bootState.value as? EditorBootState.Ready)?.binder?.closeProject()
    }
}
