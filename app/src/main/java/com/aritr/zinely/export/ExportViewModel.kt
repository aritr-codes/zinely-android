package com.aritr.zinely.export

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** What the export screen shows. [Working] marks which card is rendering; [Error] is a transient banner. */
internal sealed interface ExportUiState {
    data object Idle : ExportUiState
    data class Working(val format: ExportFormat) : ExportUiState
    data class Error(val message: String) : ExportUiState
}

/**
 * The delivery-agnostic result of one export (ADR-054 Decision 2). The VM emits it and the host routes on
 * the **subtype** — never on a remembered "which button started this" flag:
 *  - [ExportReady] — a scoped, read-granted `content://` [uri] + [mime] for Intent delivery (Share via
 *    `ACTION_SEND`). Semantics unchanged from ADR-039 §4 / ADR-040.
 *  - [ExportSaved] — a durable copy already written to the device (in [location], e.g. "Downloads") under
 *    [displayName]; there is no Intent and no URI, so the host only raises the post-save hand-off.
 */
internal sealed interface ExportOutcome
internal data class ExportReady(val uri: Uri, val mime: String) : ExportOutcome
internal data class ExportSaved(val displayName: String, val location: String) : ExportOutcome

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
internal class ExportViewModel @Inject constructor(
    private val exporter: SheetExporter,
) : ViewModel() {

    private val _state = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    private val _outcomes = Channel<ExportOutcome>(Channel.BUFFERED)
    val outcomes: Flow<ExportOutcome> = _outcomes.receiveAsFlow()

    // The destination of the last export, so "Try again" reproduces the same attempt (a Save retries a
    // Save; a Share retries a Share). This is retry *input*, NOT delivery routing — routing is decided
    // solely by the emitted [ExportOutcome] subtype in the host (ADR-054 Decision 2/3).
    private var lastDestination: ExportDestination? = null

    fun export(
        document: ZineDocument,
        pageSizePt: PtSize,
        imageBytes: AssetBytesSource,
        format: ExportFormat,
        destination: ExportDestination,
    ) {
        if (_state.value is ExportUiState.Working) return // ignore taps while a render is in flight
        lastDestination = destination
        viewModelScope.launch {
            _state.value = ExportUiState.Working(format)
            _state.value = try {
                _outcomes.send(exporter.export(document, pageSizePt, imageBytes, format, destination))
                ExportUiState.Idle
            } catch (ce: CancellationException) {
                throw ce
            } catch (oom: OutOfMemoryError) {
                // The one Error we handle: the ~33 MB full-sheet bitmap (ADR-011) can OOM on a low-heap
                // device — a friendly banner beats a crash. Other Errors (linkage/VM) propagate (Codex).
                ExportUiState.Error("This zine is a bit big to render right now. Please try again.")
            } catch (e: Exception) {
                ExportUiState.Error("Couldn’t make your file just now. Please try again.")
            }
        }
    }

    /** Re-run the last export after a failure — same destination, against the freshly-passed live document. */
    fun retry(
        document: ZineDocument,
        pageSizePt: PtSize,
        imageBytes: AssetBytesSource,
        format: ExportFormat,
    ) {
        val destination = lastDestination ?: return
        export(document, pageSizePt, imageBytes, format, destination)
    }

    /** Dismiss a transient export error (user acknowledged the banner). */
    fun dismissError() {
        if (_state.value is ExportUiState.Error) _state.value = ExportUiState.Idle
    }
}
