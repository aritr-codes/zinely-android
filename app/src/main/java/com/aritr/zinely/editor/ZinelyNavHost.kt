package com.aritr.zinely.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.compose.runtime.DisposableEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.aritr.zinely.export.ExportFormat
import com.aritr.zinely.export.ExportUiState
import com.aritr.zinely.export.ExportViewModel
import com.aritr.zinely.feature.editor.EditorScreen
import com.aritr.zinely.feature.editor.ExportKind
import com.aritr.zinely.feature.editor.ExportScreen
import com.aritr.zinely.feature.editor.PreviewScreen

/**
 * The single-Activity navigation graph (ADR-030 §1). Two destinations: the editor on a fixed `"default"`
 * project ([EditorRoute], the start), and the reader's-booklet [PreviewRoute] it opens (S5 step 1). Both
 * are type-safe; adding a home/library screen later is a new `composable<…>` + a changed start
 * destination, nothing structural.
 */
@Composable
internal fun ZinelyNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = EditorRoute(projectId = "default"),
        modifier = modifier,
    ) {
        composable<EditorRoute> { entry ->
            val route = entry.toRoute<EditorRoute>()
            EditorDestination(
                onPreview = { navController.navigate(PreviewRoute(route.projectId)) },
            )
        }
        composable<PreviewRoute> { entry ->
            val route = entry.toRoute<PreviewRoute>()
            // Share the editor's already-constructed VM (see [PreviewRoute]): fetch the editor's live
            // back-stack entry (it stays on the stack under the preview) and resolve the SAME
            // EditorViewModel against it — never a second instance for this project (ADR-026 single-writer).
            val editorEntry = remember(route.projectId) {
                navController.getBackStackEntry(EditorRoute(route.projectId))
            }
            PreviewDestination(
                viewModel = hiltViewModel(editorEntry),
                onBack = { navController.popBackStack() },
                onPrintAndFold = { navController.navigate(ExportRoute(route.projectId)) },
            )
        }
        composable<ExportRoute> { entry ->
            val route = entry.toRoute<ExportRoute>()
            // Same shared-VM seam as Preview (see [ExportRoute]): the export reads the editor's live
            // document from its back-stack entry, so export == what Preview showed.
            val editorEntry = remember(route.projectId) {
                navController.getBackStackEntry(EditorRoute(route.projectId))
            }
            ExportDestination(
                viewModel = hiltViewModel(editorEntry),
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * The reader's-booklet preview host (S5 step 1). Renders the *shared* editor store's live document as a
 * paged booklet ([PreviewScreen]) — reading order, not the imposition sheet. Print & fold advances to
 * the [ExportRoute] · Export screen (S5 step 2, [ExportDestination]).
 */
@Composable
private fun PreviewDestination(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onPrintAndFold: () -> Unit,
) {
    val boot by viewModel.bootState.collectAsStateWithLifecycle()
    when (val state = boot) {
        is EditorBootState.Ready -> {
            val uiState by state.store.uiState.collectAsStateWithLifecycle()
            PreviewScreen(
                pages = uiState.document.pages,
                pageSizePt = state.pageSizePt,
                defaults = uiState.document.defaults,
                onBack = onBack,
                onPrintAndFold = onPrintAndFold,
                modifier = Modifier.fillMaxSize(),
                imageBytes = state.imageBytes,
            )
        }
        // Loading is a transient window (e.g. re-bootstrapping after process-death restore) — spin.
        EditorBootState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        // A failed reopen must not strand the preview on an infinite spinner (Codex Required Fix): show
        // the honest message and a way back to the editor, mirroring EditorDestination's Error branch.
        is EditorBootState.Error -> Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.message)
                androidx.compose.material3.TextButton(onClick = onBack) { Text("‹  Back to editing") }
            }
        }
    }
}

/**
 * The Export · "Print & fold" host (S5 step 2, [ADR-039](../../../../../../docs/DECISIONS.md#adr-039)).
 * Reads the *shared* editor document (same back-stack-entry seam as [PreviewDestination]) so `export ==
 * preview`, and hosts its own read-only [ExportViewModel] which renders the imposed sheet to a PDF/PNG
 * off-thread and emits a [ShareRequest]. This composable owns only the Android edges the VM must not: it
 * launches the OS share sheet for each finished file, and stubs the deferred fold-help seam.
 */
@Composable
private fun ExportDestination(viewModel: EditorViewModel, onBack: () -> Unit) {
    val boot by viewModel.bootState.collectAsStateWithLifecycle()
    val exportViewModel: ExportViewModel = hiltViewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Hand each finished export to the OS share sheet (ADR-039 §4): a scoped, read-granted content:// URI.
    // Collect only while STARTED so an export that finishes while backgrounded doesn't launch the chooser
    // at a stopped lifecycle state — the buffered Channel holds the event until we resume (Codex).
    LaunchedEffect(exportViewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            exportViewModel.shares.collect { request ->
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = request.mime
                    putExtra(Intent.EXTRA_STREAM, request.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, "Share your zine"))
            }
        }
    }

    when (val state = boot) {
        is EditorBootState.Ready -> {
            val uiState by state.store.uiState.collectAsStateWithLifecycle()
            val exportState by exportViewModel.state.collectAsStateWithLifecycle()
            ExportScreen(
                onPrintPdf = {
                    exportViewModel.export(uiState.document, state.pageSizePt, state.imageBytes, ExportFormat.PDF)
                },
                onSavePng = {
                    exportViewModel.export(uiState.document, state.pageSizePt, state.imageBytes, ExportFormat.PNG)
                },
                onFoldHelp = {
                    // Fold steps are the S5 Completion screen (SCREEN-INVENTORY) — warm placeholder until then.
                    Toast.makeText(context, "Fold steps are coming soon ✨", Toast.LENGTH_SHORT).show()
                },
                onBack = onBack,
                working = (exportState as? ExportUiState.Working)?.format?.toExportKind(),
                errorMessage = (exportState as? ExportUiState.Error)?.message,
                onDismissError = exportViewModel::dismissError,
                modifier = Modifier.fillMaxSize(),
            )
        }
        EditorBootState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        is EditorBootState.Error -> Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.message)
                androidx.compose.material3.TextButton(onClick = onBack) { Text("‹  Back to editing") }
            }
        }
    }
}

/** Map the host export format to the screen's feature-local kind (module-boundary mirror). */
private fun ExportFormat.toExportKind(): ExportKind = when (this) {
    ExportFormat.PDF -> ExportKind.PDF
    ExportFormat.PNG -> ExportKind.PNG
}

/**
 * Hosts one [EditorViewModel] and renders its [EditorBootState]. The VM owns the store/binder for the
 * project's lifetime; this composable only (a) drives the autosave binder off the UI lifecycle and
 * (b) drains a11y announcements to the platform live region — neither belongs in the VM (Codex rec 1).
 */
@Composable
private fun EditorDestination(onPreview: () -> Unit) {
    val viewModel: EditorViewModel = hiltViewModel()
    val boot by viewModel.bootState.collectAsStateWithLifecycle()

    // Drain the VM's announcement channel to TalkBack. Bound to the View (composable-only), so it
    // lives here, not in the VM. Conflated buffer in the VM tolerates a brief subscriber gap.
    val view = LocalView.current
    LaunchedEffect(view) {
        viewModel.announcements.collect { text -> view.announceForAccessibility(text) }
    }

    // The system photo picker (ADR-031 §5). The launcher lives here (Compose-only); the VM-held
    // PhotoPicker bridges it to the import pipeline. Bind the launch action while composed; unbind on
    // dispose so a pending pick() resumes null instead of hanging (Codex RF2).
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.photoPicker.deliver(uri)
    }
    DisposableEffect(viewModel) {
        viewModel.photoPicker.bind {
            pickLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
        onDispose { viewModel.photoPicker.unbind() }
    }

    when (val state = boot) {
        EditorBootState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        is EditorBootState.Error -> Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) { Text(state.message) }

        is EditorBootState.Ready -> {
            // Attach the autosave binder to the UI lifecycle: it flushes on ON_PAUSE/ON_STOP and
            // re-attaches across rotation. The VM owns teardown (closeProject in onCleared); we never
            // dispose-on-rotation here (Codex rec 2) — only detach this observer.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, state.binder) {
                state.binder.observe(lifecycleOwner.lifecycle)
                onDispose { }
            }
            // The across-sessions move/resize-hint gate (ADR-032): a load-aware tri-state — `null` until
            // the persisted flag loads (the host hides the hint on `null`, so it can't flash before its
            // state is known), then the real `false`/`true`. Lifecycle-aware.
            val moveResizeHintSeen by viewModel.moveResizeHintSeen.collectAsStateWithLifecycle()
            // The unresolved-save-failure kind (ADR-035/ADR-036): the honest correction to "Saved ✨",
            // derived from the app-scoped SaveFailureSink (ADR-026 §5) and mapped to a feature-local
            // SaveErrorKind (null = none). Lifecycle-aware; the host renders the warm banner (copy keyed by
            // the kind) and suppresses the optimistic chip while it is non-null.
            val saveError by viewModel.saveError.collectAsStateWithLifecycle()
            // The editor surface now owns its own add-photo entry point: the EditorSupplyTray's
            // "Add a photo" supply dispatches the same Intent.RequestAddImage → Effect.PickAndDecodeImage
            // → the photo picker bound above. The lone app-level FAB has been removed (ADR-029 follow-up:
            // every primary action is a visible supply in the editor, per DESIGN-RULES 1).
            EditorScreen(
                store = state.store,
                pageSizePt = state.pageSizePt,
                modifier = Modifier.fillMaxSize(),
                imageBytes = state.imageBytes,
                moveResizeHintSeen = moveResizeHintSeen,
                onMoveResizeHintSeen = viewModel::markMoveResizeHintSeen,
                // The autosave-event stream (ADR-034): each emission raises the transient "Saved ✨"
                // reassurance in the host. Hot SharedFlow, collected inside EditorScreen.
                savedSignals = viewModel.saved,
                // The honest save-failure correction (ADR-035/ADR-036): show the warm banner (generic or
                // storage-specific copy, by kind) + suppress "Saved ✨" while a failure is unresolved;
                // "Got it" clears it from the sink.
                saveError = saveError,
                onDismissSaveError = viewModel::dismissSaveError,
                // "Try now" (ADR-038): force an immediate save; the outcome flows through the ADR-037
                // path (clears the banner on success, re-reports on a repeat failure).
                onRetrySaveError = viewModel::retrySave,
                onPreview = onPreview,
            )
        }
    }
}
