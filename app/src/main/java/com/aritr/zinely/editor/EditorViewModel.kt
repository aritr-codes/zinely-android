package com.aritr.zinely.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aritr.zinely.core.data.asset.AssetStore
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.data.storage.DocumentSnapshotProvider
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent as EditorIntent
import com.aritr.zinely.core.editor.cascadedPlacement
import com.aritr.zinely.core.imposition.Imposer
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.data.android.EditorAutosaveBinder
import com.aritr.zinely.data.android.SaveFailureSink
import com.aritr.zinely.data.android.di.EditorAutosaveBinderFactory
import com.aritr.zinely.data.android.prefs.EditorOnboardingStore
import com.aritr.zinely.feature.editor.Announcer
import com.aritr.zinely.home.BUSY_MESSAGE
import com.aritr.zinely.feature.editor.AutosaveSink
import com.aritr.zinely.feature.editor.DefaultEditorEffectRunner
import com.aritr.zinely.feature.editor.EditorStore
import com.aritr.zinely.feature.editor.ImagePickResult
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
 * `SavedStateHandle` key for **persistence of place** — C9 row 9.3.
 *
 * Scoped to the editor's `SavedStateHandle`, which is per navigation entry, so it is already per project:
 * two zines cannot share a remembered page. Survives process death; does not survive leaving the editor,
 * which is correct — reopening a zine is what §E.4's invariant is about, and that goes through a fresh
 * entry whose handle the platform restores.
 */
internal const val KEY_PAGE_INDEX: String = "c9.pageIndex"

/**
 * The restore half of row 9.3, as a pure function — the house's *pure helper extraction* convention, and
 * the only part of persistence of place that has a decision in it.
 *
 * Clamping is the whole guard: a remembered index cannot outlive the pages it referred to. It can only
 * ever be too large (a shorter document than the one the maker left), because [OD-2](../../../../../../docs/DECISIONS.md#adr-089)
 * re-seated variable page counts, so nothing in Phase C grows a document. `pageCount = 0` is defended
 * anyway rather than assumed impossible — an empty document reaching here would otherwise throw inside a
 * `coerceIn` with a reversed range, and turning a missing page into a crash is a poor trade.
 */
internal fun restoredPageIndex(saved: Int?, pageCount: Int): Int =
    (saved ?: 0).coerceIn(0, (pageCount - 1).coerceAtLeast(0))

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
    private val shareInbox: ShareInbox,
    @param:AssetsDir private val assetsDir: File,
    // @param: pins the qualifier to the constructor value parameter (what Dagger reads) and opts out of
    // the KT-73255 default-target migration warning — same convention as EditorAutosaveBinderFactory.
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    @param:com.aritr.zinely.data.android.di.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    /** The project to open — threaded from the type-safe [EditorRoute] (Codex rec E), not a stray constant. */
    private val projectId: String = savedStateHandle.toRoute<EditorRoute>().projectId

    /**
     * **Persistence of place — C9 row 9.3** ([ADR-097](../../../../../../../docs/DECISIONS.md#adr-097)).
     *
     * [V2-BENCH-REVIEW §E.4](../../../../../../../docs/design/V2-BENCH-REVIEW.md) makes reopening *"exactly
     * as left"* **a build invariant, freeze-blocking for the Compose build** — and it is the one signal the
     * prototype cannot show, because a prototype is never killed and restarted.
     *
     * Two things this deliberately is **not**:
     *
     * - **Not the shelf half.** §E.4's invariant covers the page *and* the gathered materials. Owner ruling
     *   [OD-2](../../../../../../../docs/DECISIONS.md#adr-089) re-seated H1 beyond Phase C, so C9 owes the
     *   **page** half only; the shelf half carries forward to the package that builds the shelf, unweakened.
     * - **Not a second state owner.** [ADR-005](../../../../../../../docs/DECISIONS.md#adr-005) makes the
     *   editor MVI with [EditorStore] owning state. The handle is a *carrier* across process death, written
     *   from the store and read exactly once, at boot, into the initial model. Nothing in the UI reads the
     *   page index from here.
     *
     * `SavedStateHandle` rather than the document, because a page index is where the *maker* is, not what
     * the *zine* is — persisting it into `ZineDocument` would be a schema change, which is precisely the
     * "new document-model concept" OD-2 forbids Phase C.
     */
    private val savedPageIndex: SavedStateHandle = savedStateHandle

    private val _bootState = MutableStateFlow<EditorBootState>(EditorBootState.Loading)
    val bootState: StateFlow<EditorBootState> = _bootState.asStateFlow()

    /** A11y live-region channel; the composable collects this and calls `view.announceForAccessibility`. */
    private val _announcements = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val announcements: SharedFlow<String> = _announcements.asSharedFlow()

    /**
     * A UI-originated a11y announcement (WCAG 4.1.3) — the Reframe surface's position/zoom/fit/commit lines
     * (ADR-053, IF3). Routed through the SAME channel the reducer's [com.aritr.zinely.feature.editor.Announcer]
     * uses, so every editor announcement reaches TalkBack via the one `announceForAccessibility` drain; the
     * platform re-announces even identical consecutive text, so a repeated nudge is never silent.
     */
    fun announce(text: String) {
        _announcements.tryEmit(text)
    }

    /**
     * **The import summary, as text a sighted maker can read** (D-081 ruling #3, WCAG 3.3.1).
     *
     * The failure half of an import used to exist only as an [announce] line, i.e. only in the a11y live
     * region — so a maker watching 3 of 5 shared photos appear was told nothing at all about the other two.
     * A live region makes text *reach* a screen reader; it is not a substitute for the text existing.
     * The host collects this and raises a `Toast` (a system surface no HTML freezes, unlike `BenchSnack`).
     *
     * `replay = 0` with a small buffer, same policy as [saved]: a summary emitted into a subscriber gap is
     * dropped rather than replayed onto the next screen — a stale "2 photos added" is worse than silence,
     * and the photos themselves are already durable through the store.
     */
    private val _importSummaries = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val importSummaries: SharedFlow<String> = _importSummaries.asSharedFlow()

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

    /**
     * The across-sessions "already taught the Reframe coach-mark" gate (ADR-053 RF2), read as the same
     * load-aware tri-state as [moveResizeHintSeen]: `null` until loaded, then `false` (teach) / `true`.
     */
    val reframeCoachSeen: StateFlow<Boolean?> =
        onboardingStore.reframeCoachSeen
            .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    /** Persist that the Reframe coach-mark has taught (idempotent). Fire-and-forget on [viewModelScope]. */
    fun markReframeCoachSeen() {
        viewModelScope.launch { onboardingStore.markReframeCoachSeen() }
    }

    init {
        viewModelScope.launch(mainDispatcher) {
            // Await the single-writer slot BEFORE loading (ADR-046 §2, realising ADR-030 Rec1): a fast
            // reopen of a just-closed project must ride out its asynchronous teardown flush — which also
            // means the load below reads the flushed bytes. The bound is the shared AutosaveSessionGate
            // policy; false = still busy at the bound → a warm boot error, never a hang or a crash.
            if (!binderFactory.awaitNoSession(projectId)) {
                _bootState.value = EditorBootState.Error(BUSY_MESSAGE)
                return@launch
            }
            // Load off the UI thread: DocumentRepositoryImpl does blocking nio read/fsync and does not
            // hop dispatchers itself (Codex RF1), so a large document load would otherwise ANR.
            val result = withContext(ioDispatcher) { bootstrapDocument(repository, projectId) }
            // Back on Main: the store's dispatch is main-thread-only by contract, so it is built here.
            _bootState.value = when (result) {
                is DataResult.Success -> try {
                    // Row 9.3: land on the page the maker left, not on page 1. Clamped against the document
                    // actually loaded — a stale index cannot outlive the pages it referred to, and a
                    // clamp is the whole guard because the format's page count is fixed (OD-2 re-seated
                    // variable page counts, so this can only ever shrink through a corrupt/older document).
                    val restored = restoredPageIndex(
                        saved = savedPageIndex.get<Int>(KEY_PAGE_INDEX),
                        pageCount = result.value.pages.size,
                    )
                    ready(EditorModel(result.value, currentPageIndex = restored))
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

        // Row 9.3, the write half: the store stays the single owner and this only mirrors it. Started here
        // rather than in `init` so it cannot observe a store that does not exist, and `distinctUntilChanged`
        // keeps a drag or a text edit from writing an unchanged index on every emission.
        viewModelScope.launch(mainDispatcher) {
            store.uiState
                .map { it.currentPageIndex }
                .distinctUntilChanged()
                .collect { savedPageIndex[KEY_PAGE_INDEX] = it }
        }

        // Step 3: the autosave binder pulls the latest document from the live store at save time.
        val snapshotProvider = DocumentSnapshotProvider { store.uiState.value.document }

        // Step 4: create the binder last — this eagerly registers projectId with the single-writer
        // factory (ADR-026), so it happens exactly once per open project, here.
        binder = binderFactory.create(projectId, snapshotProvider)

        // Share-in (ADR-105 / SUPPLIES-SPEC §6): drain photos another app handed us into THIS zine.
        //
        // ⚠ **Started after step 4, and that ordering is load-bearing.** `binderFactory.create` can throw
        // (a still-releasing single-writer slot — see the boot `catch` above), and this collector would
        // outlive the throw: `viewModelScope` is still alive, so it would go on importing the maker's
        // photos into an orphan store that is never rendered and, with no binder, never autosaved. The
        // photos would be consumed and lost. Launching last means a failed boot leaves the inbox full,
        // and the next zine opened gets them.
        viewModelScope.launch(mainDispatcher) { importSharedPhotos(store, imagePipeline, pageSizePt) }

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

    /**
     * **The share-in drain** ([ShareInbox]) — photos another app handed Zinely become elements of the zine
     * this ViewModel owns, and this is the only place that decides *which* zine that is: the one that is
     * open. Collection lives for as long as the VM does, so a share arriving mid-session lands immediately;
     * `subscriptionCount` on the inbox's flow is what lets the receiving Activity know a zine is open and
     * skip a toast the maker does not need.
     *
     * **The loop is sequential, and that is the point.** Each import decodes a full-resolution master into
     * memory (`ImportMasterDecoder` caps the transient decode at ~24 MP), so a twenty-photo share dispatched
     * as twenty concurrent coroutines on the IO dispatcher is an out-of-memory report, not a feature. One
     * at a time costs latency nobody is watching and removes the failure mode entirely — no cap, no
     * semaphore, no silently dropped photos.
     *
     * Everything downstream is the shipped ADR-031 §5 path
     * ([AndroidImagePickDecodePipeline.decodeAndStore] → [Intent.CommitAddImage]) — same master, same
     * content-addressed dedup, same placement, same undo. A photo that cannot be read (revoked grant,
     * corrupt, or a type the manifest filter let through) is counted, never crashed on, and reported.
     *
     * Shared photos are cascaded rather than stacked ([cascadedPlacement], D-081 ruling #2) — the picker's
     * one-at-a-time centred default is untouched.
     */
    private suspend fun importSharedPhotos(
        store: EditorStore,
        pipeline: AndroidImagePickDecodePipeline,
        pageSizePt: PtSize,
    ) {
        shareInbox.pending.collect {
            // ⚠ **The inner loop is not defensive tidiness — without it photos are silently stranded.**
            // A `StateFlow` re-emits only when the value differs from the one THIS collector last saw, and
            // the body below suspends for the whole decode. So: `[a]` is collected, `takeAll()` leaves `[]`,
            // a second share of the same photo arrives mid-decode and restores the value to `[a]` — and
            // when the body returns, the current value equals the last observed one, so nothing is emitted
            // and those URIs sit in the inbox forever with nothing said about them. Draining until the
            // inbox is actually empty closes the window that conflation opens.
            while (true) {
                val batch = shareInbox.takeAll()
                if (batch.isEmpty()) break
                importBatch(batch, store, pipeline, pageSizePt)
            }
        }
    }

    /** One share's worth of photos, imported in order and reported once. See [importSharedPhotos]. */
    private suspend fun importBatch(
        batch: List<Uri>,
        store: EditorStore,
        pipeline: AndroidImagePickDecodePipeline,
        pageSizePt: PtSize,
    ) {
        var added = 0
        var failed = 0
        for ((batchIndex, uri) in batch.withIndex()) {
            when (val result = pipeline.decodeAndStore(uri)) {
                is ImagePickResult.Success -> {
                    // A13 / D-081 Q10: continue the share-in cascade from photos already on the page
                    // that will receive this commit. Read after the suspending decode so a page switch
                    // during import is honoured; there is no suspension between this snapshot and dispatch.
                    // Text and decor deliberately do not count — this closes the repeated-photo stack,
                    // not general occupied-space placement. `remainingInBatch` keeps one multi-photo share
                    // on the same bounded sequence while allowing failed decodes to leave no fake ordinal.
                    val state = store.uiState.value
                    val existingImageCount = state.document.pages[state.currentPageIndex].elements
                        .filterIsInstance<ImageElement>()
                        .size
                    val remainingInBatch = batch.size - batchIndex
                    val placed = result.element.copy(
                        transform = cascadedPlacement(
                            base = result.element.transform,
                            index = existingImageCount,
                            count = existingImageCount + remainingInBatch,
                            pageSizePt = pageSizePt,
                        ),
                    )
                    store.dispatch(EditorIntent.CommitAddImage(placed))
                    added++
                }
                is ImagePickResult.Failure -> failed++
                ImagePickResult.Cancelled -> Unit // unreachable: share-in never launches a picker
            }
        }
        if (added > 0 || failed > 0) {
            val summary = Copy.ShareIn.importSummary(added, failed)
            // Both paths, deliberately (D-081 ruling #3). The toast is the *visible* half WCAG 3.3.1 asks
            // for, and it is the half that was missing: an assistive-technology-only error report inverts
            // the standard, which requires the error be described in text and treats live regions as the
            // way that text REACHES a screen reader, never as a substitute for it.
            //
            // `announce` is kept only provisionally, and the reason first written here was WRONG. It said
            // TalkBack's handling of toast text "could not be verified from official documentation" —
            // false, because AOSP *is* the documentation once the guides run out. `ToastPresenter.show()`
            // builds a TYPE_NOTIFICATION_STATE_CHANGED event, populates it from the toast's own view and
            // sends it ("treat toasts as notifications since they are used to announce a transient piece
            // of information to the user"); API 30 moved rendering to SystemUI and kept that path intact.
            // Nothing deduplicates that against `announceForAccessibility`'s TYPE_ANNOUNCEMENT, so this
            // very likely speaks the sentence TWICE — and `announceForAccessibility` is deprecated in 36.
            //
            // ⚠ So the evidence points at deleting the `announce`. It stays until a device listen pass,
            // because "the event is emitted carrying the text" and "TalkBack speaks it" are two claims and
            // only the first is proven. Pre-registered question for that pass: **is it spoken twice?**
            _importSummaries.tryEmit(summary)
            announce(summary)
        }
    }

    /** Flush-then-cancel the autosave for this project when the host leaves for good (ADR-030 §6). */
    override fun onCleared() {
        (bootState.value as? EditorBootState.Ready)?.binder?.closeProject()
    }
}
