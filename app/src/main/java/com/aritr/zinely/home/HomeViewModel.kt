package com.aritr.zinely.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aritr.zinely.core.data.asset.ZineBackupOmission
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.FolderNames
import com.aritr.zinely.core.data.repository.ProjectShelfEntry
import com.aritr.zinely.core.data.repository.ProjectRepository
import com.aritr.zinely.core.data.repository.ProjectSummary
import com.aritr.zinely.core.data.repository.ProjectUnavailableReason
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.data.android.prefs.PreferredPaperStore
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.newZineCoverRecipe
import com.aritr.zinely.data.android.LibraryBackupReceipt
import com.aritr.zinely.data.android.LibraryBackupResult
import com.aritr.zinely.data.android.LibraryBackupStage
import com.aritr.zinely.data.android.LibrarySafTransport
import com.aritr.zinely.data.android.OutcomeLatch
import com.aritr.zinely.data.android.prefs.BackupRecord
import com.aritr.zinely.data.android.prefs.BackupRecordStore
import com.aritr.zinely.feature.editor.FolderSnackAction
import com.aritr.zinely.feature.editor.HomeShelfEvent
import com.aritr.zinely.feature.editor.HomeZineCard
import com.aritr.zinely.feature.library.FolderNameVerdict
import com.aritr.zinely.feature.library.LibraryBackupRestoreFailureKind
import com.aritr.zinely.feature.library.LibraryBackupRestoreMode
import com.aritr.zinely.feature.library.LibraryBackupRestoreUiState
import com.aritr.zinely.feature.library.LibraryLastBackup
import com.aritr.zinely.feature.library.LibraryOmission
import com.aritr.zinely.feature.library.LibraryOmissionReason
import com.aritr.zinely.feature.library.LibraryZine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * What the Home shelf shows (ADR-043). [Loading] until the project store first answers (so the
 * empty invitation never flashes); [Empty] is a real state, distinct from a zero-card [Content].
 *
 * [Error] means the shelf could not be *read*. It never means a zine was lost — the store is a file
 * on this device and it is still there — which is why [HomeViewModel.retry] simply re-asks.
 */
internal sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data object Error : HomeUiState
    /**
     * Two shelf projections from one pass over one repository answer, at one `now`.
     *
     * [cards] stays the healthy V1/V1.5 metadata view used by the still-shared actions that need a
     * readable authoritative document. [zines] is the wider V2 Library view, which can keep an
     * unavailable project visible for Rename/Delete without pretending it still opens normally.
     */
    data class Content(
        val cards: List<HomeZineCard>,
        // No default: a `Content` with cards and no zines draws the V2 shelf as a heading over nothing —
        // no covers, and no invitation either, because `Content` is not `Empty`. It is a state the screen
        // cannot represent honestly, so it must not be constructible by omission.
        val zines: List<LibraryZine>,
    ) : HomeUiState
}

/**
 * The Home · "My zines" shelf (read shelf: ADR-043; actions: ADR-044; MVVM — ADR-005 scoped MVI to
 * the editor). Maps the [ProjectRepository]'s newest-first shelf stream to the V2 Library, while the
 * narrower healthy [ProjectSummary] projection continues to feed the older card-shaped seams. Order is
 * passed through untouched; recency labels are computed at emission and go stale until the next one —
 * accepted for a shelf you just navigated to (fresh subscription = fresh labels via WhileSubscribed); a
 * ticking clock is not this slice's problem.
 *
 * S6.3 actions (ADR-044): create with warm defaults; rename/duplicate delegating to the store
 * (which enforces the open-editor exclusion and answers [DataError.Busy]); delete as a durable deferred
 * commit — [PendingDeleteStore] records the intent before the card hides, a queued
 * [HomeShelfEvent.DeletePrompt] drives one undo snackbar, and its dismissal calls
 * [ProjectRepository.deleteProject]. A process restart finishes any recorded intent. A failed commit
 * unhides the card: the shelf never lies about what was deleted. [HomeUiState.Empty] means the
 * STORE is empty; a shelf filtered to zero by pending deletes stays a zero-card
 * [HomeUiState.Content] (the invitation would be dishonest while a delete is still reversible).
 *
 * Shelf folders ([ADR-125](docs/DECISIONS.md#adr-125)): [moveZine], [renameFolder] and [unpackFolder]
 * delegate to the store and answer with a [HomeShelfEvent.FolderSnack]. Each **first finishes a waiting
 * delete and does not go ahead if that delete fails** (rule 11), so no zine the maker cannot see is moved.
 */
@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val librarySafTransport: LibrarySafTransport,
    private val preferredPaperStore: PreferredPaperStore,
    private val pendingDeleteStore: PendingDeleteStore,
    private val backupRecordStore: BackupRecordStore,
) : ViewModel() {

    /** Ids hidden from the shelf while their undo window is open or a durable interrupted delete resumes. */
    private val pendingDeletes = MutableStateFlow<Set<String>>(emptySet())

    /** This session's display-only covers for zines the store could not assign one to. */
    private val fallbackCovers = FallbackCovers()

    /** Queued one-shot events; the buffer absorbs emissions while the screen is between collects. */
    private val eventQueue = Channel<HomeShelfEvent>(Channel.BUFFERED)

    /** One-shot shelf events (undo prompts, warm failure messages), each consumed exactly once. */
    val events: Flow<HomeShelfEvent> = eventQueue.receiveAsFlow()

    /** Queued one-shot "open this project" navigation events (ADR-046 §5) — consumed by the nav
     * destination (navigation is a destination-layer concern), never by the shelf screen itself. */
    private val openQueue = Channel<String>(Channel.BUFFERED)

    /**
     * One-shot open-project ids; the destination collects and navigates `EditorRoute(id)`. Each
     * (re)collection first **discards anything buffered while nobody was collecting** (Codex
     * implementation-review Required Fix): an open that landed after the shelf left composition —
     * e.g. a slow create finishing behind an already-taken card tap — must never auto-navigate on
     * return. Navigation is a fresh user action; a dropped stale open costs one re-tap, data-safe.
     */
    val openEvents: Flow<String> = flow {
        while (openQueue.tryReceive().isSuccess) {
            // discard stale opens buffered between shelf visits
        }
        emitAll(openQueue.receiveAsFlow())
    }

    /** The in-flight create (ADR-046 §5 single-flight): taps during it are no-ops. */
    private var createJob: Job? = null

    /** The folder action in flight. Single-flight, as create is: a second tap while one runs is a no-op. */
    private var folderJob: Job? = null

    /** What the standing folder snack's action does (Undo, or Try again); `null` when it offers none. */
    private var snackAction: (suspend () -> Unit)? = null
    private var backupRestoreJob: Job? = null
    private var backupRestorePickerPending: Boolean = false
    private var backupRestoreCancellationRequested: Boolean = false

    /**
     * The running job's Cancel-vs-complete latch (ADR-120): a backup claims it when its file is complete, a restore
     * when its commit starts (ADR-122 R2).
     */
    private var outcomeLatch: OutcomeLatch? = null

    init {
        val interruptedDeletes = pendingDeleteStore.pendingIds()
        if (interruptedDeletes.isNotEmpty()) {
            pendingDeletes.value = interruptedDeletes
            viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                withContext(NonCancellable) { commitPendingDeletesNow() }
            }
        }
    }

    /**
     * Bumped by [retry]. `flatMapLatest` below turns each bump into a **fresh** subscription to
     * `observeProjects()` — a flow that has thrown is dead, and `catch` cannot revive it, so the only
     * honest retry is a new collection of a new flow.
     */
    private val retries = MutableStateFlow(0)

    private val pickerRequests = Channel<LibraryBackupRestorePickerRequest>(Channel.BUFFERED)
    val backupRestorePickerRequests: Flow<LibraryBackupRestorePickerRequest> = pickerRequests.receiveAsFlow()

    private val _backupRestoreState = MutableStateFlow<LibraryBackupRestoreUiState?>(null)
    val backupRestoreState: StateFlow<LibraryBackupRestoreUiState?> = _backupRestoreState

    val preferredPaper: StateFlow<PaperSize> = preferredPaperStore.preferredPaperSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, PaperSize.A4)

    /** The chooser's last-backup fact; null until a backup has been saved on this phone. */
    val lastBackup: StateFlow<LibraryLastBackup?> = backupRecordStore.lastBackup
        .map { record ->
            record?.let { LibraryLastBackup(it.savedAtEpochMs, it.fileName, it.savedCount, it.totalCount) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setPreferredPaper(paperSize: PaperSize) {
        viewModelScope.launch {
            try {
                preferredPaperStore.setPreferredPaperSize(paperSize)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                eventQueue.send(HomeShelfEvent.Message(Copy.Colophon.PAPER_SAVE_FAILED))
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeUiState> = retries.flatMapLatest { shelfStateFlow() }
        // WhileSubscribed(0), not 5_000 (ADR-046 §6): every return to the shelf re-collects the
        // upstream, so max(row, doc mtime) recency re-derives after an editor round-trip — the store
        // emits nothing on autosave mtime changes, so a warm subscription shows stale cards on
        // exactly the most common flow. stateIn keeps the last value: no Loading flash.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), HomeUiState.Loading)

    /**
     * One subscription's worth of shelf state.
     *
     * `catch` is **outside** the combine, so a throw from either source lands here; it
     * swallows nothing silently — [HomeUiState.Error] is a visible, recoverable state with [retry]
     * behind it.
     *
     * There is deliberately **no** `onStart { emit(Loading) }` here, tempting as it is: this flow is
     * re-collected on every return to the shelf (`WhileSubscribed(0)`, ADR-046 §6), and a Loading
     * emission per subscription would wipe the cached [HomeUiState.Content] and flash the skeleton on
     * the most common flow in the app. A retry therefore holds [HomeUiState.Error] until the store
     * answers; the "Try again" button owns its own pressed state, which is where that cue belongs.
     *
     * `CancellationException` is not caught: `catch` re-throws it by contract, so leaving the shelf
     * cancels the collection rather than painting an error over a screen nobody is looking at.
     */
    private fun shelfStateFlow(): Flow<HomeUiState> =
        combine(
            projectRepository.observeShelfProjects(),
            pendingDeletes,
        ) { projects, pending ->
            if (projects.isEmpty()) {
                HomeUiState.Empty
            } else {
                val now = System.currentTimeMillis()
                // Prune pending ids the store no longer knows (committed deletes it caught up with):
                // this VM is process-lifetime since the ADR-046 re-root, so "stale ids are inert"
                // is no longer enough — they must not accumulate forever (Codex). The update
                // re-triggers this combine once; the second pass is a no-op and it converges.
                val projectIds = projects.mapTo(HashSet()) { it.id }
                if (pending.any { it !in projectIds }) {
                    pendingDeletes.update { current -> current.filterTo(mutableSetOf()) { it in projectIds } }
                }
                val visible = projects.filterNot { it.id in pending }
                // ADR-125 rules 11 and 13: folders are worked out from the zines the Shelf is showing, and
                // a folder has one spelling. Only files changed outside the app can disagree; the screen
                // groups by plain equality, so the one spelling is settled here.
                val spellings = visible.mapNotNull { it.folder }.groupBy(FolderNames::key)
                    .mapValues { (_, names) -> FolderNames.display(names) }
                HomeUiState.Content(
                    cards = visible
                        .mapNotNull { (it as? ProjectShelfEntry.Available)?.summary }
                        .map { it.toCard(now) },
                    zines = visible.map { entry ->
                        entry.toLibraryZine(
                            nowEpochMs = now,
                            fallbackCover = fallbackCovers::get,
                            folder = entry.folder?.let { spellings[FolderNames.key(it)] },
                        )
                    },
                )
            }
        }.catch { emit(HomeUiState.Error) }

    /**
     * The shelf failed to open; ask again. Idempotent from the user's side — each tap is one fresh
     * subscription, and `flatMapLatest` cancels the previous one, so a double-tap cannot leave two
     * collections racing to fill the same shelf.
     */
    fun retry() {
        retries.update { it + 1 }
    }

    fun startBackup() {
        val hasVisibleZines = (state.value as? HomeUiState.Content)?.cards?.isNotEmpty() == true
        if (!hasVisibleZines) return
        requestBackupRestorePicker(LibraryBackupRestorePickerRequest.Backup(suggestedBackupName()))
    }

    fun startRestore() {
        requestBackupRestorePicker(LibraryBackupRestorePickerRequest.Restore)
    }

    fun backupPicked(uri: Uri?) {
        backupRestorePickerPending = false
        if (uri == null) return
        if (backupRestoreJob?.isActive == true) return
        val latch = OutcomeLatch()
        outcomeLatch = latch
        launchBackupRestore(LibraryBackupRestoreMode.Backup) {
            when (val result = librarySafTransport.backupTo(uri, latch)) {
                is LibraryBackupResult.Saved -> {
                    // The only place the record is written: a saved backup, including one a late Cancel
                    // lost to, and a partial one (with its counts). Failure, nothing saved, cancel, picker
                    // cancel/failure and restore never reach here.
                    val receipt = result.receipt
                    recordSavedBackup(
                        BackupRecord(
                            savedAtEpochMs = System.currentTimeMillis(),
                            fileName = result.fileName,
                            savedCount = receipt.projectCount,
                            totalCount = receipt.totalCount,
                        ),
                    )
                    LibraryBackupRestoreUiState.BackupSaved(
                        projectCount = receipt.projectCount,
                        assetCount = receipt.assetCount,
                        totalCount = receipt.totalCount,
                        omitted = receipt.omitted.toUi(),
                        offShelfReasons = receipt.offShelfReasons.mapTo(mutableSetOf(), ::toUiReason),
                    )
                }
                is LibraryBackupResult.NothingSaved -> LibraryBackupRestoreUiState.Failed(
                    mode = LibraryBackupRestoreMode.Backup,
                    kind = nothingSavedKind(result.receipt),
                )
                is LibraryBackupResult.Failed -> LibraryBackupRestoreUiState.Failed(
                    mode = LibraryBackupRestoreMode.Backup,
                    kind = classifyBackupFailure(result),
                )
            }
        }
    }

    private suspend fun recordSavedBackup(record: BackupRecord) {
        try {
            backupRecordStore.recordBackup(record)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The file is saved whether or not this phone could note it; the saved state stays true.
        }
    }

    fun restorePicked(uri: Uri?) {
        backupRestorePickerPending = false
        if (uri == null) return
        if (backupRestoreJob?.isActive == true) return
        val latch = OutcomeLatch()
        outcomeLatch = latch
        // ADR-122 R2: called inside the repository's lock just before the non-cancellable commit. Winning the
        // latch makes a later Cancel a no-op and drops Cancel from the sheet; losing it means Cancel came first,
        // and the repository commits nothing.
        val onCommitStart = {
            latch.markDone().also { committing ->
                if (committing) {
                    _backupRestoreState.value = LibraryBackupRestoreUiState.Running(
                        mode = LibraryBackupRestoreMode.Restore,
                        cancellable = false,
                    )
                }
            }
        }
        launchBackupRestore(LibraryBackupRestoreMode.Restore) {
            when (val result = librarySafTransport.restoreFrom(uri, onCommitStart)) {
                is DataResult.Success -> {
                    val receipt = result.value
                    // R3: committed, but the shelf index lags. A fresh subscription reconciles it.
                    if (!receipt.shelfUpToDate) retry()
                    LibraryBackupRestoreUiState.RestoreAdded(
                        restoredProjectCount = receipt.addedCount,
                        alreadyHereCount = receipt.alreadyHereCount,
                        shelfUpToDate = receipt.shelfUpToDate,
                        omitted = receipt.omitted.toUi(),
                    )
                }
                is DataResult.Failure -> LibraryBackupRestoreUiState.Failed(
                    mode = LibraryBackupRestoreMode.Restore,
                    kind = classifyBackupRestoreFailure(LibraryBackupRestoreMode.Restore, result.error),
                )
            }
        }
    }

    fun backupRestorePickerFailed(mode: LibraryBackupRestoreMode) {
        backupRestorePickerPending = false
        _backupRestoreState.value = LibraryBackupRestoreUiState.Failed(
            mode = mode,
            kind = mode.ioFailureKind(),
        )
    }

    fun cancelBackupRestore() {
        if (backupRestoreJob?.isActive != true) return
        // A backup whose file is already complete, or a restore whose commit has started, can't be cancelled: the
        // latch was claimed as done, so this Cancel is a no-op and the real outcome follows (Brief 01 "the late
        // Cancel", ADR-122 R2).
        if (outcomeLatch?.requestCancel() == false) return
        backupRestoreCancellationRequested = true
        backupRestoreJob?.cancel()
    }

    fun dismissBackupRestoreSurface() {
        _backupRestoreState.value = null
    }

    fun retryBackupRestore() {
        when ((backupRestoreState.value as? LibraryBackupRestoreUiState.Failed)?.mode) {
            LibraryBackupRestoreMode.Backup -> startBackup()
            LibraryBackupRestoreMode.Restore -> startRestore()
            null -> Unit
        }
    }

    /**
     * Open a card's project: commit any pending deletes first (leaving the shelf is a snackbar
     * dismissal — ADR-046 §4), then hand the id to the destination as a one-shot open event. A hidden
     * pending-delete card is untappable, so [id] can never itself be pending.
     */
    fun openZine(id: String) {
        viewModelScope.launch {
            commitPendingDeletesNow()
            openQueue.send(id)
        }
    }

    /**
     * "Start a zine": create on the chosen [paperSize] (S7.1/ADR-047 — the shelf's paper chooser
     * decides; nothing is hardcoded here), then navigate into the new zine via the same one-shot
     * open path (ADR-046 §5). Single-flight — a tap while a create is in flight is a no-op
     * (an unguarded double-tap would mint two projects and two navigations); pending deletes commit
     * first (ADR-046 §4). Create failure keeps the warm message and emits no open event.
     *
     * [folder] is the folder the Shelf has open, or `null` on My Shelf: a zine made inside a folder lands
     * in it (ADR-125 rule 8), written in the zine's first metadata write.
     */
    fun startZine(paperSize: PaperSize, folder: String? = null) {
        if (createJob?.isActive == true) return
        createJob = viewModelScope.launch {
            commitPendingDeletesNow()
            when (val result =
                projectRepository.createProject(DEFAULT_NEW_TITLE, ZineFormat.SINGLE_SHEET_8, paperSize, folder)
            ) {
                is DataResult.Success -> openQueue.send(result.value.id)
                is DataResult.Failure -> eventQueue.send(HomeShelfEvent.Message(result.error.warmMessage()))
            }
        }
    }

    /** Rename with the ADR-044 §4 normalisation: trimmed; blank keeps the existing name. */
    fun rename(id: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { projectRepository.renameProject(id, trimmed).sendMessageOnFailure() }
    }

    fun duplicate(id: String) {
        viewModelScope.launch { projectRepository.duplicateProject(id).sendMessageOnFailure() }
    }

    // ---- Shelf folders (ADR-125; the words and the snack are `v21-library.html` A28's) ----------------

    /**
     * Put a zine in [folder], or back on My Shelf (`null`). The snack says where it went, and that its old
     * folder is put away if this was the last zine in it (A28.3); Undo puts it back where it was.
     */
    fun moveZine(id: String, folder: String?) = folderAction {
        val zines = shownZines()
        val zine = zines.firstOrNull { it.id == id } ?: return@folderAction
        val from = zine.folder
        // The spelling the store will write: the folder's own if it exists, else the name as cleaned.
        val joined = folder?.let { name -> zines.firstNotNullOfOrNull { z -> z.folder?.takeIf { FolderNames.same(it, name) } } }
        val to = joined ?: FolderNames.clean(folder)
        if (!projectRepository.moveProject(id, to).sentMessageOnFailure()) return@folderAction
        val line = when {
            to == null -> Copy.Folders.BACK_ON_MY_SHELF
            joined == null -> Copy.Folders.made(to)
            else -> Copy.Folders.movedTo(to)
        }
        val emptied = from != null && from != to && zines.count { it.folder == from } == 1
        snackAction = {
            if (projectRepository.moveProject(id, from).sentMessageOnFailure()) {
                val back = if (from != null) Copy.Folders.backIn(zine.title, from) else Copy.Folders.backOnMyShelf(zine.title)
                eventQueue.send(HomeShelfEvent.FolderSnack(back, zineId = id, folder = from))
            }
        }
        eventQueue.send(
            HomeShelfEvent.FolderSnack(
                message = if (emptied) Copy.Folders.putAway(line, from!!) else line,
                action = FolderSnackAction.Undo,
                zineId = id,
                folder = to,
            ),
        )
    }

    /**
     * Rename a folder. No Undo (rule 10). It is one file write per zine and can stop partway (rule 18): the
     * snack then says some zines are still under the old name and offers *Try again*, which runs the same
     * rename and finishes it.
     */
    fun renameFolder(from: String, to: String) = folderAction { runRename(from, to) }

    private suspend fun runRename(from: String, to: String) {
        val change = (projectRepository.renameFolder(from, to).valueOrSentMessage() ?: return)
        if (change.complete) {
            eventQueue.send(
                HomeShelfEvent.FolderSnack(
                    message = Copy.Folders.renamedTo(change.folder ?: to),
                    zineId = change.changedIds.firstOrNull(),
                    folder = change.folder,
                ),
            )
        } else {
            snackAction = { runRename(from, to) }
            eventQueue.send(HomeShelfEvent.FolderSnack(Copy.Folders.someStillIn(from), FolderSnackAction.TryAgain))
        }
    }

    /**
     * Take a folder's zines out: they go back on My Shelf and the folder is gone. Undo puts each back, one
     * at a time. Like a rename it can stop partway and be run again (rule 18); the Undo then covers every
     * zine that was taken out, in whichever run.
     */
    fun unpackFolder(name: String) = folderAction { runUnpack(name, alreadyOut = emptyList()) }

    private suspend fun runUnpack(name: String, alreadyOut: List<String>) {
        val change = projectRepository.unpackFolder(name).valueOrSentMessage() ?: return
        val out = alreadyOut + change.changedIds
        if (!change.complete) {
            snackAction = { runUnpack(name, out) }
            eventQueue.send(HomeShelfEvent.FolderSnack(Copy.Folders.someStillIn(name), FolderSnackAction.TryAgain))
            return
        }
        // Focus follows the newest of them, which is where the pile stood.
        val first = shownZines().firstOrNull { it.id in out }?.id ?: out.firstOrNull()
        snackAction = {
            // Every zine is tried even after one fails: what the Shelf then shows is true (rule 18).
            val allBack = out.map { projectRepository.moveProject(it, name) is DataResult.Success }.all { it }
            eventQueue.send(
                if (allBack) {
                    HomeShelfEvent.FolderSnack(Copy.Folders.folderIsBack(name), zineId = first, folder = name)
                } else {
                    HomeShelfEvent.FolderSnack(Copy.Folders.NOT_EVERY_ZINE_WENT_BACK)
                },
            )
        }
        eventQueue.send(
            HomeShelfEvent.FolderSnack(Copy.Folders.unpacked(name), FolderSnackAction.Undo, zineId = first, folder = null),
        )
    }

    /** The folder snack's Undo or Try again was pressed. It is a folder action like any other. */
    fun folderSnackAction() {
        val action = snackAction ?: return
        folderAction { action() }
    }

    /**
     * Runs one folder action. **A waiting delete is finished first, and the action does not go ahead if
     * that delete fails** (ADR-125 rule 11): [commitPendingDeletesNow] has by then unhidden the zine and
     * said so, and a folder action over a Shelf the maker has just been told is wrong would be a guess.
     */
    private fun folderAction(run: suspend () -> Unit) {
        if (folderJob?.isActive == true) return
        folderJob = viewModelScope.launch {
            if (!commitPendingDeletesNow()) return@launch
            snackAction = null
            run()
        }
    }

    private fun shownZines(): List<LibraryZine> = (state.value as? HomeUiState.Content)?.zines.orEmpty()

    /**
     * Durably record the delete intent, then hide the card and prompt for undo. The project store is
     * untouched until [commitDelete], while the tiny marker lets a new process finish the request. Ignored
     * when [id] is not a visible card (double-tap, or already pending).
     */
    fun delete(id: String) {
        val zines = (state.value as? HomeUiState.Content)?.zines ?: return
        val title = zines.firstOrNull { it.id == id }?.title ?: return
        if (!pendingDeleteStore.add(id)) {
            eventQueue.trySend(HomeShelfEvent.Message(GENERIC_FAILURE_MESSAGE))
            return
        }
        pendingDeletes.update { it + id }
        eventQueue.trySend(HomeShelfEvent.DeletePrompt(id, title))
    }

    /** Undo within the window: clear the durable intent, then unhide; the project store was never called. */
    fun undoDelete(id: String) {
        if (!pendingDeleteStore.remove(id)) {
            eventQueue.trySend(HomeShelfEvent.Message(GENERIC_FAILURE_MESSAGE))
            return
        }
        pendingDeletes.update { it - id }
    }

    /**
     * The undo window closed: perform the store delete. On success the id STAYS in
     * [pendingDeletes] — unhiding here would flash the deleted card back for the window between
     * `deleteProject` returning and [ProjectRepository.observeProjects] re-emitting; once the flow
     * emits the shorter list the filter is a no-op (a stale id over a fresh-UUID store is inert).
     * Only a failed commit unhides + messages: the card is still real, and the shelf never lies.
     */
    fun commitDelete(id: String) {
        viewModelScope.launch { performCommit(id) }
    }

    /**
     * Commit any pending delete as Home leaves the foreground.
     *
     * The undo window is still in-memory and reversible while the shelf stays visible. Once the
     * destination stops, though, a process kill can drop that in-memory state before the snackbar's
     * timeout path runs, which makes the zine reappear on a cold boot. Flushing here keeps the
     * user-visible "delete means delete" contract without changing the on-screen undo behaviour.
     */
    fun flushPendingDeletes() {
        if (pendingDeletes.value.isEmpty()) return
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) { commitPendingDeletesNow() }
        }
    }

    /** The one commit path [commitDelete] and [commitPendingDeletesNow] share. */
    private suspend fun performCommit(id: String): Boolean =
        when (val result = projectRepository.deleteProject(id)) {
            is DataResult.Success -> {
                // If clearing the marker fails, retrying this idempotent delete on the next start is safe.
                pendingDeleteStore.remove(id)
                true // stay hidden; the store flow removes the card
            }
            is DataResult.Failure -> {
                // Only show the real card again after clearing the durable retry intent. Otherwise the next
                // process would legitimately finish a delete the current UI appeared to cancel.
                if (pendingDeleteStore.remove(id)) pendingDeletes.update { it - id }
                eventQueue.send(HomeShelfEvent.Message(result.error.warmMessage()))
                false
            }
        }

    /**
     * Commit every pending delete before leaving the shelf (ADR-046 §4): navigating away cancels the
     * snackbar collector, so neither undo nor commit would ever run — this awaits the commits the
     * dismissal would have made. A failed commit rolls back visibly through [performCommit] (unhide +
     * warm message, seen on return) and **never blocks** the requested open/create.
     */
    private suspend fun commitPendingDeletesNow(): Boolean =
        pendingDeletes.value.map { performCommit(it) }.all { it }

    private fun requestBackupRestorePicker(request: LibraryBackupRestorePickerRequest) {
        if (backupRestoreJob?.isActive == true || backupRestorePickerPending) return
        dismissBackupRestoreSurface()
        backupRestorePickerPending = true
        viewModelScope.launch {
            if (!commitPendingDeletesNow()) {
                backupRestorePickerPending = false
                return@launch
            }
            pickerRequests.send(request)
        }
    }

    private fun launchBackupRestore(
        mode: LibraryBackupRestoreMode,
        run: suspend () -> LibraryBackupRestoreUiState,
    ) {
        if (backupRestoreJob?.isActive == true) return
        backupRestoreJob = viewModelScope.launch {
            _backupRestoreState.value = LibraryBackupRestoreUiState.Running(mode)
            try {
                _backupRestoreState.value = run()
            } catch (cancelled: CancellationException) {
                _backupRestoreState.value = null
                if (backupRestoreCancellationRequested) {
                    eventQueue.trySend(
                        HomeShelfEvent.Message(
                            if (mode == LibraryBackupRestoreMode.Backup) {
                                Copy.LibraryBackup.BACKUP_CANCELLED
                            } else {
                                Copy.LibraryBackup.RESTORE_CANCELLED
                            },
                        ),
                    )
                }
                throw cancelled
            } finally {
                // Only this job's own state: a stale finish must never clear a newer backup's latch.
                if (backupRestoreJob == null || backupRestoreJob === coroutineContext[Job]) {
                    backupRestoreCancellationRequested = false
                    backupRestoreJob = null
                    outcomeLatch = null
                }
            }
        }
    }

    /**
     * Owner ruling F1 (ADR-120): a whole backup failure is shown by **where** it failed, in this order —
     * out of space anywhere; the chosen destination; then Zinely's own private archive. Busy is not a
     * failure (F2). A leftover private-archive Corrupt / Invalid is "Couldn't finish that backup": a zine that
     * can't be read is left out, not a failure (ADR-122 §2). Restore keeps [classifyBackupRestoreFailure].
     */
    private fun classifyBackupFailure(failure: LibraryBackupResult.Failed): LibraryBackupRestoreFailureKind =
        when (val error = failure.error) {
            is DataError.OutOfSpace -> LibraryBackupRestoreFailureKind.NotEnoughSpace
            is DataError.Busy -> LibraryBackupRestoreFailureKind.Busy
            else -> if (failure.stage == LibraryBackupStage.Destination) {
                LibraryBackupRestoreFailureKind.SaveFailed
            } else {
                when (error) {
                    is DataError.LimitExceeded -> LibraryBackupRestoreFailureKind.BackupLimitReached
                    else -> LibraryBackupRestoreFailureKind.Generic
                }
            }
        }

    /**
     * 0 of M saved (ADR-122 §4): "A zine here needs a newer Zinely" when every left-out zine is newer (flagged for
     * owner confirmation), the photo wording when every one is a photo, else the general wording.
     */
    private fun nothingSavedKind(receipt: LibraryBackupReceipt): LibraryBackupRestoreFailureKind {
        val reasons = receipt.omitted.map { ZineBackupOmission.normalizedReason(it.reason) }.toSet()
        return when {
            reasons == setOf(ZineBackupOmission.NEWER_VERSION) -> LibraryBackupRestoreFailureKind.BackupZineNewer
            reasons == setOf(ZineBackupOmission.PHOTO) -> LibraryBackupRestoreFailureKind.BackupNoneSavedPhoto
            receipt.omittedOffShelf -> LibraryBackupRestoreFailureKind.BackupNoneSavedOffShelf
            else -> LibraryBackupRestoreFailureKind.BackupNoneSaved
        }
    }

    private fun List<ZineBackupOmission>.toUi(): List<LibraryOmission> = map { omission ->
        LibraryOmission(title = omission.title?.takeIf { it.isNotBlank() }, reason = toUiReason(omission.reason))
    }

    private fun toUiReason(reason: String?): LibraryOmissionReason =
        when (ZineBackupOmission.normalizedReason(reason)) {
            ZineBackupOmission.NEWER_VERSION -> LibraryOmissionReason.Newer
            ZineBackupOmission.PHOTO -> LibraryOmissionReason.Photo
            else -> LibraryOmissionReason.Unreadable
        }

    private fun classifyBackupRestoreFailure(
        mode: LibraryBackupRestoreMode,
        error: DataError,
    ): LibraryBackupRestoreFailureKind = when (error) {
        is DataError.Corrupt, is DataError.Invalid, is DataError.LimitExceeded -> LibraryBackupRestoreFailureKind.Damaged
        is DataError.SchemaTooNew -> LibraryBackupRestoreFailureKind.NewerAppNeeded
        is DataError.OutOfSpace -> LibraryBackupRestoreFailureKind.NotEnoughSpace
        is DataError.Busy -> LibraryBackupRestoreFailureKind.Busy
        is DataError.Io, is DataError.NotFound -> mode.ioFailureKind()
        is DataError.Unknown -> LibraryBackupRestoreFailureKind.Generic
    }

    private fun LibraryBackupRestoreMode.ioFailureKind(): LibraryBackupRestoreFailureKind =
        if (this == LibraryBackupRestoreMode.Backup) {
            LibraryBackupRestoreFailureKind.SaveFailed
        } else {
            LibraryBackupRestoreFailureKind.ReadFailed
        }

    private fun suggestedBackupName(): String = "${Copy.LibraryBackup.PICKER_BACKUP_NAME_PREFIX}-${LocalDate.now()}.zine"

    private suspend fun DataResult<*>.sendMessageOnFailure() {
        if (this is DataResult.Failure) eventQueue.send(HomeShelfEvent.Message(error.warmMessage()))
    }

    /** Whether this succeeded; a failure has been said. */
    private suspend fun DataResult<*>.sentMessageOnFailure(): Boolean {
        sendMessageOnFailure()
        return this is DataResult.Success
    }

    /** The value, or `null` after saying the failure. */
    private suspend fun <T> DataResult<T>.valueOrSentMessage(): T? {
        sendMessageOnFailure()
        return (this as? DataResult.Success)?.value
    }
}

internal sealed interface LibraryBackupRestorePickerRequest {
    data class Backup(val suggestedName: String) : LibraryBackupRestorePickerRequest
    data object Restore : LibraryBackupRestorePickerRequest
}

/**
 * This session's display-only covers for zines the store could not assign one to.
 *
 * [ProjectSummary.cover] is null only when the store could not **persist** an assignment: a legacy
 * backfill whose write failed, or a project adopted from disk whose sidecar could not be written or
 * could not be read. In every one of those cases the store returns the meta *coverless* rather than
 * fabricating an identity it cannot store (`RoomProjectRepository.backfillCoverIfLegacy` and
 * `readMetaOrBackfill` share that rule). A shelf still cannot draw an object with no cover, so one is
 * drawn here — for display, for this session only.
 *
 * **Once per zine, not once per emission.** The shelf re-emits on every store change and on every return
 * from the editor, so drawing inline in the mapper would repaint that zine each time — the identity
 * flicker D-017 exists to prevent, arriving by the back door on the one path a real user can reach. The
 * mid-package review found exactly that, and this class is the fix.
 *
 * **Memoised, not derived.** Computing the recipe *from the id* would also be stable, would look correct
 * forever, and would be precisely the inference D-017 forbids — a cover derived from a name. This holds a
 * drawn recipe instead, and holds it only as long as the ViewModel lives: the honest lifetime for a cover
 * that is not on disk.
 *
 * A class rather than a `ConcurrentHashMap` field so the memo has somewhere to be tested, and so [assign]
 * is injectable — over the real assigner two draws collide once in thirty-six, which is a flaky test, not
 * a proof of anything. Concurrent because `flatMapLatest` can briefly overlap two collections across a
 * [HomeViewModel.retry].
 */
internal class FallbackCovers(private val assign: () -> ZineCoverRecipe = { newZineCoverRecipe() }) {
    private val drawn = ConcurrentHashMap<String, ZineCoverRecipe>()

    operator fun get(id: String): ZineCoverRecipe = drawn.getOrPut(id) { assign() }
}

/** Warm failure copy (VOICE): [DataError.Busy] is "still saving", never a scary failure. */
private fun DataError.warmMessage(): String = when (this) {
    is DataError.Busy -> BUSY_MESSAGE
    else -> GENERIC_FAILURE_MESSAGE
}

/** Default title for a shelf-created zine — aligned with the store's adoption fallback (ADR-044 §4). */
internal const val DEFAULT_NEW_TITLE: String = "My zine"

/** The ADR-044 §1 gate refused: an editor session is still live/releasing. Retry-shaped, warm. */
internal const val BUSY_MESSAGE: String = "That zine is still saving — try again in a moment."

/** Any other mutation failure (VOICE: warm, recoverable, no jargon). */
internal const val GENERIC_FAILURE_MESSAGE: String = "That didn't work — try again?"

/** `ProjectSummary` → the card the shelf shows: only display data past this point (ADR-043). */
internal fun ProjectSummary.toCard(
    nowEpochMs: Long,
): HomeZineCard = HomeZineCard(
    id = id,
    title = title,
    formatLabel = "${format.shelfLabel()} · ${paperSize.shelfLabel()}",
    editedLabel = editedLabel(updatedAtEpochMs, nowEpochMs),
)

/**
 * `ProjectSummary` → the object the V2 Library stands on its shelf (B5, ADR-086 rows 6–8).
 *
 * The **subtitle is composed here, not on the cover**, and that is the frozen design's own argument: the
 * shelf shows covers and withholds metadata (*"Format & date are disclosed there, not stamped on every
 * card"*), so the line exists only for the action sheet. `data-sub` reads `"A4 · 2 days ago"` — paper,
 * then recency — and [editedLabel] supplies the recency it already supplies to V1. The freeze shows five
 * example values and states no thresholds, so the formatter is the authority on the words and the frozen
 * file on the shape; B5 asserts the wiring rather than re-testing the formatter (ADR-086 row 8).
 *
 * **The cover is read, never derived.** [ProjectSummary.cover] is the persisted recipe assigned once at
 * creation (D-017) — this function neither draws nor infers one from the title, the position, or the
 * neighbours. The one exception is stated rather than hidden: `null` means a legacy backfill could not be
 * written (`RoomProjectRepository.backfillCoverIfLegacy` returns the meta unchanged rather than
 * fabricating an identity it cannot store). A shelf cannot draw an object with no cover, so a recipe is
 * drawn for **this rendering only** and is not persisted — the alternative would be an empty hole where a
 * zine is, and deriving one from the id would be exactly the inference D-017 forbids.
 */
internal fun ProjectSummary.toLibraryZine(
    nowEpochMs: Long,
    fallbackCover: (String) -> ZineCoverRecipe = { newZineCoverRecipe() },
): LibraryZine = LibraryZine(
    id = id,
    title = title,
    subtitle = "${paperSize.shelfLabel()} · ${editedLabel(updatedAtEpochMs, nowEpochMs)}",
    // `newZineCoverRecipe()` inline here would re-draw on **every emission**, not once — the identity
    // flicker D-017 exists to prevent, on the one path that reaches a real user. The resolver is what
    // makes the fallback stable for as long as this session lasts; see [HomeViewModel.fallbackCover].
    cover = cover ?: fallbackCover(id),
)

/**
 * @param folder the folder to show this zine in. The entry's own by default; the Shelf passes the one
 *   spelling it settled for that folder (ADR-125 rule 13).
 */
internal fun ProjectShelfEntry.toLibraryZine(
    nowEpochMs: Long,
    fallbackCover: (String) -> ZineCoverRecipe = { newZineCoverRecipe() },
    folder: String? = this.folder,
): LibraryZine = when (this) {
    is ProjectShelfEntry.Available -> summary.toLibraryZine(nowEpochMs, fallbackCover).copy(folder = folder)
    is ProjectShelfEntry.Unavailable -> LibraryZine(
        id = id,
        title = title,
        subtitle = buildString {
            paperSize?.let {
                append(it.shelfLabel())
                append(" · ")
            }
            append(editedLabel(updatedAtEpochMs, nowEpochMs))
        },
        cover = cover ?: fallbackCover(id),
        unavailableReason = reason.shelfUnavailableReason(),
        folder = folder,
    )
}

/**
 * What a typed folder name stands for, for the Shelf's name sheet. [FolderNames] owns the rules (ADR-125
 * rule 13); this only puts its three answers in the shape the screen reads.
 */
internal fun folderNameVerdict(typed: String): FolderNameVerdict {
    val name = FolderNames.clean(typed)
        ?: return if (FolderNames.isMyShelf(typed)) FolderNameVerdict.MyShelf else FolderNameVerdict.Blank
    return FolderNameVerdict.Name(name, FolderNames.key(name), cut = FolderNames.isTooLong(typed))
}

private fun ProjectUnavailableReason.shelfUnavailableReason(): String = when (this) {
    ProjectUnavailableReason.CORRUPT -> Copy.Shelf.UNAVAILABLE_DAMAGED
    ProjectUnavailableReason.NEWER_APP_REQUIRED -> Copy.Shelf.UNAVAILABLE_NEWER_APP
}

/** Warm, jargon-free format name (never the enum's SCREAMING_SNAKE identity). */
private fun ZineFormat.shelfLabel(): String = when (this) {
    ZineFormat.SINGLE_SHEET_8 -> "8-page mini"
}

/**
 * Paper-size name as people say it.
 *
 * Reads [Copy.Paper] rather than repeating the strings, which is what ADR-060 asks for and what this
 * function was quietly not doing: when ADR-101 P3 renamed `LETTER` to *"US Letter"* to match the frozen
 * `.paperseg`, the shelf went on saying *"Letter"* and the two surfaces disagreed about the same size.
 */
private fun PaperSize.shelfLabel(): String = when (this) {
    PaperSize.LETTER -> Copy.Paper.LETTER
    PaperSize.A4 -> Copy.Paper.A4
}

/**
 * The human recency line for a card: "Edited just now / N minutes / N hours ago / yesterday /
 * N days ago". Pure — `now` is a parameter, so tests need no clock seam (Codex). A future
 * timestamp (clock skew) clamps to "just now"; never negative time.
 */
internal fun editedLabel(updatedAtEpochMs: Long, nowEpochMs: Long): String {
    val elapsedMs = (nowEpochMs - updatedAtEpochMs).coerceAtLeast(0L)
    val minutes = elapsedMs / 60_000L
    val hours = elapsedMs / 3_600_000L
    val days = elapsedMs / 86_400_000L
    return when {
        minutes < 1 -> "Edited just now"
        hours < 1 -> "Edited $minutes ${plural(minutes, "minute")} ago"
        days < 1 -> "Edited $hours ${plural(hours, "hour")} ago"
        days < 2 -> "Edited yesterday"
        else -> "Edited $days days ago"
    }
}

private fun plural(n: Long, unit: String): String = if (n == 1L) unit else "${unit}s"
