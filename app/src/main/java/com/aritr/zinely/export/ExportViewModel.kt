package com.aritr.zinely.export

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aritr.zinely.editor.ProofRoute
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.ProjectRepository
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.render.android.AssetBytesSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import android.os.Environment
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * What the export screen shows.
 *
 * Both non-idle states name the [ExportDestination] they belong to, and that is the whole point: the Proof
 * offers **two** commit actions (Save PDF, Share), and until ADR-102 neither state said which one was
 * running. The screen could only ask *"is an export happening?"*, so it answered for both buttons at once —
 * tap Share and Save PDF dimmed too, and a failed Share removed the Save button from the screen. The VM
 * knew the answer the whole time and kept it private.
 *
 * ⚠ [ExportFormat] is **not** that discriminator and cannot stand in for it: both buttons export PDF. Only
 * the destination separates them.
 */
internal sealed interface ExportUiState {
    data object Idle : ExportUiState
    data class Working(val format: ExportFormat, val destination: ExportDestination) : ExportUiState
    data class Error(
        val message: String,
        val destination: ExportDestination,
        val failure: ExportFailure = ExportFailure.Other,
    ) : ExportUiState
}

/**
 * Why an export failed, so the Proof can say what to change (`v21-proof.html` A43, D-142). Classified from the exception's **type**
 * and a free-space probe, never from its message.
 */
internal enum class ExportFailure { Other, OutOfMemory, OutOfSpace, PermissionDenied }

/**
 * The delivery-agnostic result of one export (ADR-054 Decision 2). The VM emits it and the host routes on
 * the **subtype** — never on a remembered "which button started this" flag:
 *  - [ExportReady] — a scoped, read-granted `content://` [uri] + [mime] for Intent delivery (Share via
 *    `ACTION_SEND`). Semantics unchanged from ADR-039 §4 / ADR-040.
 *  - [ExportSaved] — a durable copy already written to the device (in [location], e.g. "Downloads") under
 *    [displayName], with the exact scoped [uri] needed by an explicit post-save Open action.
 */
internal sealed interface ExportOutcome
internal data class ExportReady(val uri: Uri, val mime: String) : ExportOutcome
internal data class ExportSaved(
    val uri: Uri,
    val mime: String,
    val displayName: String,
    val location: String,
) : ExportOutcome

/**
 * Drives one export (ADR-039 §5, ADR-054). Reads no document of its own — the host passes the *live shared
 * editor* document/pageSize/imageBytes (so `export == preview`) plus the [ExportDestination] into [export];
 * this VM only renders + delivers, so it never touches the single-writer autosave factory (ADR-026) and is
 * safe to scope to the Proof route. Depends on the [SheetExporter] seam (not the concrete exporter) so it
 * is unit-testable with a fake, per the repository-pattern convention.
 *
 * Single-flight: taps while a render is in flight are ignored. Success emits an [ExportOutcome] the host
 * collects and routes by subtype (ExportReady → share Intent; ExportSaved → the post-save Fold hand-off);
 * any failure (IO, or an `OutOfMemoryError` on the ~33 MB sheet) becomes a friendly [ExportUiState.Error].
 */
@HiltViewModel
internal class ExportViewModel(
    private val exporter: SheetExporter,
    /** Usable bytes where the export lands. A seam so the out-of-space call is testable. */
    private val freeBytes: () -> Long = ::sharedStorageFreeBytes,
    /** The zine's title, read at export time so a rename made after the Proof opened still lands in the filename. */
    private val titleOf: suspend () -> String = { "" },
) : ViewModel() {

    // The Proof route's own argument is in this entry's SavedStateHandle, so no host or EditorViewModel plumbing.
    @Inject
    constructor(exporter: SheetExporter, projects: ProjectRepository, handle: SavedStateHandle) : this(
        exporter,
        ::sharedStorageFreeBytes,
        projectTitleOf(handle, projects),
    )

    private val _state = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    private val _outcomes = Channel<ExportOutcome>(Channel.BUFFERED)
    val outcomes: Flow<ExportOutcome> = _outcomes.receiveAsFlow()

    fun export(
        document: ZineDocument,
        pageSizePt: PtSize,
        imageBytes: AssetBytesSource,
        format: ExportFormat,
        destination: ExportDestination,
    ) {
        if (_state.value is ExportUiState.Working) return // ignore taps while a render is in flight
        viewModelScope.launch {
            _state.value = ExportUiState.Working(format, destination)
            _state.value = try {
                _outcomes.send(exporter.export(document, pageSizePt, imageBytes, format, destination, titleOf()))
                ExportUiState.Idle
            } catch (ce: CancellationException) {
                throw ce
            } catch (oom: OutOfMemoryError) {
                // The one Error we handle: the ~33 MB full-sheet bitmap (ADR-011) can OOM on a low-heap
                // device — a friendly banner beats a crash. Other Errors (linkage/VM) propagate (Codex).
                ExportUiState.Error(
                    "This zine is a bit big to render right now. Please try again.",
                    destination,
                    ExportFailure.OutOfMemory,
                )
            } catch (e: IOException) {
                ExportUiState.Error(COULD_NOT_MAKE_FILE, destination, classifyIo())
            } catch (e: Exception) {
                ExportUiState.Error(COULD_NOT_MAKE_FILE, destination)
            }
        }
    }

    /**
     * The host could not start a delivery to [destination] at all. Today that means Android 7–9 refused
     * Save PDF's storage permission (ADR-054 §8), but the VM doesn't need to know why. It shows the same
     * recoverable error a failed export shows, so "Try again" behaves the same, and like [export] it never
     * interrupts a render in flight.
     */
    fun couldNotStart(destination: ExportDestination) {
        if (_state.value is ExportUiState.Working) return
        _state.value = ExportUiState.Error(COULD_NOT_MAKE_FILE, destination, ExportFailure.PermissionDenied)
    }

    /**
     * A failed write is "out of space" only when the disk demonstrably is: the same false-negative-biased
     * probe as `DocumentRepositoryImpl.classifyWriteFailure` (ADR-036). `IOException` carries no errno, so
     * the probe is the only honest signal; a probe that cannot answer never invents the claim.
     */
    private fun classifyIo(): ExportFailure {
        val low = try {
            freeBytes() < LOW_SPACE_BYTES
        } catch (_: Exception) {
            false
        }
        return if (low) ExportFailure.OutOfSpace else ExportFailure.Other
    }

    /**
     * Re-run the failed export — same destination, against the freshly-passed live document.
     *
     * The destination is read **off the error being shown**, not off a remembered field. That field
     * existed (`lastDestination`) and was deleted with this change: it could disagree with the error on
     * screen, and it made the VM the only place that knew which action had failed while the screen said
     * only "something failed". Retry now cannot retry an action other than the one the user is looking at.
     */
    fun retry(
        document: ZineDocument,
        pageSizePt: PtSize,
        imageBytes: AssetBytesSource,
        format: ExportFormat,
    ) {
        val destination = (_state.value as? ExportUiState.Error)?.destination ?: return
        export(document, pageSizePt, imageBytes, format, destination)
    }

    // `dismissError()` used to live here and is deliberately gone (ADR-102 §12.14). It had exactly one
    // caller — the host, immediately before `retry()` — and once retry started reading the destination off
    // the Error being shown, that call became the thing that would break it. Left in place it was a method
    // whose only documentation was a warning not to call it, which is a trap for the next reader rather
    // than an API. The error pane offers "Try again" and nothing else; if a real dismiss affordance is ever
    // designed, it comes back with a caller.

    private companion object {
        const val COULD_NOT_MAKE_FILE = "Couldn’t make your file just now. Please try again."

        // ponytail: a fixed bar, not the payload size (unknown until written). A one-sheet PDF is a few MB,
        // so under 64 MiB free a failed write is very likely the disk; above it we under-claim (generic).
        const val LOW_SPACE_BYTES = 64L * 1024 * 1024
    }
}

/**
 * Usable bytes of shared storage, where Downloads lives (and, on every shipped layout, the same volume as
 * the app's cache that Share writes to). The storage root always exists, so no walk-up is needed.
 */
@Suppress("DEPRECATION") // getExternalStorageDirectory: statfs only, no file access.
/**
 * The title of the zine the Proof route is open on, read at call time. A missing route argument, a missing
 * zine or a failing read all yield "" — [ExportNaming] turns that into "zine", so a save never fails over a name.
 */
internal fun projectTitleOf(handle: SavedStateHandle, projects: ProjectRepository): suspend () -> String = {
    try {
        val id = handle.toRoute<ProofRoute>().projectId
        (projects.getProject(id) as? DataResult.Success)?.value?.title ?: ""
    } catch (ce: CancellationException) {
        throw ce
    } catch (e: Exception) {
        ""
    }
}

private fun sharedStorageFreeBytes(): Long = Environment.getExternalStorageDirectory().usableSpace
