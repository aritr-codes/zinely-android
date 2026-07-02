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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
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
import com.aritr.zinely.feature.editor.CompletionAction
import com.aritr.zinely.feature.editor.CompletionScreen
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
                onFoldHelp = { navController.navigate(CompletionRoute(route.projectId)) },
                // ADR-041: after a successful export shares the file, land on Completion (the fold-steps
                // payoff) — the natural post-export destination the deferred ADR-040 gap named. launchSingleTop
                // future-proofs the no-duplicate-Completion guarantee against `ready` ever becoming a
                // replaying stream (today it's a one-shot Channel, so the dedupe already holds) — Codex.
                onExported = { navController.navigate(CompletionRoute(route.projectId)) { launchSingleTop = true } },
            )
        }
        composable<CompletionRoute> { entry ->
            val route = entry.toRoute<CompletionRoute>()
            // Same shared-VM seam again (see [CompletionRoute]): Completion reuses the shipped export path
            // to render the live document, then shares/opens the result.
            val editorEntry = remember(route.projectId) {
                navController.getBackStackEntry(EditorRoute(route.projectId))
            }
            CompletionDestination(
                viewModel = hiltViewModel(editorEntry),
                onBack = { navController.popBackStack() },
                onKeepEditing = { navController.popBackStack(EditorRoute(route.projectId), inclusive = false) },
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
 * off-thread and emits an [ExportReady] event. This composable owns only the Android edges the VM must not:
 * it launches the OS share sheet for each finished file, then auto-lands on Completion ([onExported],
 * ADR-041), and routes the manual fold-help seam ([onFoldHelp]) to the same screen.
 */
@Composable
private fun ExportDestination(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onFoldHelp: () -> Unit,
    onExported: () -> Unit,
) {
    val boot by viewModel.bootState.collectAsStateWithLifecycle()
    val exportViewModel: ExportViewModel = hiltViewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Hand each finished export to the OS share sheet (ADR-039 §4): a scoped, read-granted content:// URI.
    // Collect only while STARTED so an export that finishes while backgrounded doesn't launch the chooser
    // at a stopped lifecycle state — the buffered Channel holds the event until we resume (Codex).
    LaunchedEffect(exportViewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            exportViewModel.ready.collect { ready ->
                context.startActivity(Intent.createChooser(shareIntent(ready.uri, ready.mime), "Share your zine"))
                // Auto-land on the Completion payoff after the share is dispatched (ADR-041). No duplicate
                // Completion stacks: `ready` is a one-shot Channel (each success consumed once, never
                // replayed), and this collector is repeatOnLifecycle(STARTED)-gated so Export stops
                // collecting once Completion covers it. onExported navigates launchSingleTop as belt-and-braces.
                onExported()
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
                onFoldHelp = onFoldHelp,
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
 * The Completion · fold-steps host (S5 step 3, [ADR-040](../../../../../../docs/DECISIONS.md#adr-040)) —
 * the payoff screen. Reuses the shipped export seam (its own [ExportViewModel] over the shared editor
 * document, same back-stack-entry seam as [ExportDestination]) so there is no parallel export path: both
 * "Send to a friend" and "Open it" render the current document to a PDF, and this composable maps the one
 * finished-file event to the right Android edge — a share chooser or a viewer.
 *
 * The VM's [ExportReady] event is delivery-agnostic (ADR-040); single-flight guarantees at most one export
 * in flight, so [pending] — the action that started it — is the correct routing for the file that comes
 * back. "Keep editing" is the honest "make another" until the multi-project layer exists (ADR-040).
 */
@Composable
private fun CompletionDestination(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onKeepEditing: () -> Unit,
) {
    val boot by viewModel.bootState.collectAsStateWithLifecycle()
    val exportViewModel: ExportViewModel = hiltViewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // The action that started the in-flight export (single-flight → at most one), so the finished file is
    // routed to the tap that asked for it. rememberSaveable so a config change mid-render doesn't reset it
    // to SEND and misroute an in-flight OPEN (the survivor VM still emits the buffered file) — Codex RF1.
    val pending = rememberSaveable { mutableStateOf(CompletionAction.SEND) }

    LaunchedEffect(exportViewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            exportViewModel.ready.collect { ready ->
                try {
                    when (pending.value) {
                        CompletionAction.SEND -> context.startActivity(
                            Intent.createChooser(shareIntent(ready.uri, ready.mime), "Share your zine"),
                        )
                        // ACTION_VIEW is stricter than SEND; the try/catch below is the necessary fallback
                        // when no viewer is installed (Codex).
                        CompletionAction.OPEN -> context.startActivity(
                            openIntent(context.contentResolver, ready.uri, ready.mime),
                        )
                    }
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "No app on your phone can open that yet.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    when (val state = boot) {
        is EditorBootState.Ready -> {
            val uiState by state.store.uiState.collectAsStateWithLifecycle()
            val exportState by exportViewModel.state.collectAsStateWithLifecycle()
            CompletionScreen(
                onSendToFriend = {
                    pending.value = CompletionAction.SEND
                    exportViewModel.export(uiState.document, state.pageSizePt, state.imageBytes, ExportFormat.PDF)
                },
                onOpenIt = {
                    pending.value = CompletionAction.OPEN
                    exportViewModel.export(uiState.document, state.pageSizePt, state.imageBytes, ExportFormat.PDF)
                },
                onKeepEditing = onKeepEditing,
                onBack = onBack,
                working = if (exportState is ExportUiState.Working) pending.value else null,
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

/** A share-sheet Intent for a finished export: the scoped, read-granted `content://` URI as an attachment. */
private fun shareIntent(uri: Uri, mime: String): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

/**
 * An open-in-viewer Intent for a finished export. `ClipData` alongside `setDataAndType` + the read grant
 * makes more viewers accept the scoped cache URI (Codex); the caller catches `ActivityNotFoundException`.
 */
private fun openIntent(resolver: ContentResolver, uri: Uri, mime: String): Intent =
    Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        clipData = ClipData.newUri(resolver, "zine", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
