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
 * A one-shot "the file is ready" event (ADR-039 §4, ADR-040): a scoped, read-granted `content://` [uri]
 * and its [mime]. Deliberately delivery-agnostic — *what* the host does with it (an `ACTION_SEND` share
 * or an `ACTION_VIEW` open) is UI consumption the VM has no business knowing; each host tracks its own
 * pending action and maps this event to the right Intent.
 */
internal data class ExportReady(val uri: Uri, val mime: String)

/**
 * Drives one export (ADR-039 §5). Reads no document of its own — the host passes the *live shared editor*
 * document/pageSize/imageBytes (so `export == preview`) into [export]; this VM only renders + shares, so
 * it never touches the single-writer autosave factory (ADR-026) and is safe to scope to the export route.
 *
 * Single-flight: taps while a render is in flight are ignored. Success emits an [ExportReady] event the
 * host collects to launch a share/open Intent; any failure (IO, or an `OutOfMemoryError` on the ~33 MB
 * sheet) becomes a friendly [ExportUiState.Error].
 */
@HiltViewModel
internal class ExportViewModel @Inject constructor(
    private val exporter: ZineExporter,
) : ViewModel() {

    private val _state = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    private val _ready = Channel<ExportReady>(Channel.BUFFERED)
    val ready: Flow<ExportReady> = _ready.receiveAsFlow()

    fun export(
        document: ZineDocument,
        pageSizePt: PtSize,
        imageBytes: AssetBytesSource,
        format: ExportFormat,
    ) {
        if (_state.value is ExportUiState.Working) return // ignore taps while a render is in flight
        viewModelScope.launch {
            _state.value = ExportUiState.Working(format)
            _state.value = try {
                val uri = exporter.export(document, pageSizePt, imageBytes, format)
                _ready.send(ExportReady(uri, format.mime))
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

    /** Dismiss a transient export error (user acknowledged the banner). */
    fun dismissError() {
        if (_state.value is ExportUiState.Error) _state.value = ExportUiState.Idle
    }
}
