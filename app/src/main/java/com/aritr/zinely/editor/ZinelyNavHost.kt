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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.aritr.zinely.home.HomeUiState
import com.aritr.zinely.home.HomeViewModel
import com.aritr.zinely.feature.editor.HomeScreen
import androidx.compose.runtime.DisposableEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.aritr.zinely.feature.editor.EditorScreen
import com.aritr.zinely.feature.editor.ProofScreen

/**
 * The single-Activity navigation graph (ADR-030 §1, re-rooted by ADR-046 §1). [HomeRoute] — the
 * "My zines" shelf — is the start destination and the single back-stack root: a card tap or a fresh
 * create pushes [EditorRoute] (launchSingleTop guards a double-tap), and returning is only ever a
 * pop — no code path *navigates* to Home, so two editor entries can never coexist. The single Proof
 * surface (M5, ADR-051) stacks above the editor and shares its ViewModel (the ADR-026 single-writer seam).
 * [navController] is injectable for the host-level tests; production uses the remembered default.
 */
@Composable
internal fun ZinelyNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> {
            HomeDestination(
                onOpenZine = { id ->
                    navController.navigate(EditorRoute(id)) { launchSingleTop = true }
                },
            )
        }
        composable<EditorRoute> { entry ->
            val route = entry.toRoute<EditorRoute>()
            EditorDestination(
                onPreview = { navController.navigate(ProofRoute(route.projectId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<ProofRoute> { entry ->
            val route = entry.toRoute<ProofRoute>()
            // The single Proof surface (M5, ADR-051): collapses the former Preview/Export/Completion
            // triad into one 3-act screen. Same shared-VM seam the triad used — fetch the editor's live
            // back-stack entry (it stays on the stack under the Proof) and resolve the SAME
            // EditorViewModel against it, never a second instance (ADR-026 single-writer).
            val editorEntry = remember(route.projectId) {
                navController.getBackStackEntry(EditorRoute(route.projectId))
            }
            ProofDestination(
                viewModel = hiltViewModel(editorEntry),
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * The Home · "My zines" shelf host (S6.5, ADR-046 §5) — the root destination. Hosts the S6.2–6.4
 * [HomeViewModel] and threads its state/events into the stateless [HomeScreen]. Navigation is this
 * layer's concern, not the screen's: card taps and "Start a zine" both route through the VM (which
 * first commits any pending undoable deletes — leaving the shelf is a snackbar dismissal, ADR-046
 * §4) and come back as one-shot [HomeViewModel.openEvents] ids collected here into [onOpenZine].
 */
@Composable
private fun HomeDestination(onOpenZine: (String) -> Unit) {
    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.openEvents.collect { id -> onOpenZine(id) }
    }

    HomeScreen(
        loading = state is HomeUiState.Loading,
        // The honest empty signal (ADR-044 §3): the invitation only when the STORE is empty — a
        // shelf filtered to zero by pending deletes stays a zero-card shelf.
        storeEmpty = state is HomeUiState.Empty,
        // The shelf could not be read. The zines are still on the device; the retry re-asks the store.
        error = state is HomeUiState.Error,
        onRetry = viewModel::retry,
        cards = (state as? HomeUiState.Content)?.cards ?: emptyList(),
        events = viewModel.events,
        onOpenZine = viewModel::openZine,
        onStartZine = viewModel::startZine,
        onRenameZine = viewModel::rename,
        onDuplicateZine = viewModel::duplicate,
        onDeleteZine = viewModel::delete,
        onDeleteUndo = viewModel::undoDelete,
        onDeleteCommit = viewModel::commitDelete,
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * The single **Proof** host (M5, [ADR-051](../../../../../../docs/DECISIONS.md#adr-051)) — the collapse
 * of the former Preview + Export + Completion triad into one 3-act surface ([ProofScreen]). B1 stands up
 * the frame only: it boots the *shared* editor VM (the ADR-026 single-writer seam, same back-stack-entry
 * resolution the triad used) and renders the Proof scaffold. Export/share wiring and the ADR-041
 * post-export → Fold hand-off are re-attached to this host in B3, when Act 2 lands.
 */
@Composable
private fun ProofDestination(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
) {
    val boot by viewModel.bootState.collectAsStateWithLifecycle()
    when (val state = boot) {
        is EditorBootState.Ready -> {
            // ponytail: the project title is not on the editor boot state today (it lives in the Room
            // project metadata, ADR-042). B1 is the frame only; the real title threads through with Act
            // content in a later batch. A neutral fallback keeps the topbar honest until then.
            ProofScreen(
                zineName = "Your zine",
                onBack = onBack,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Loading is a transient window (e.g. re-bootstrapping after process-death restore) — spin.
        EditorBootState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        // A failed reopen must not strand the Proof on an infinite spinner: show the honest message and a
        // way back to the editor, mirroring EditorDestination's Error branch.
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
 * Hosts one [EditorViewModel] and renders its [EditorBootState]. The VM owns the store/binder for the
 * project's lifetime; this composable only (a) drives the autosave binder off the UI lifecycle and
 * (b) drains a11y announcements to the platform live region — neither belongs in the VM (Codex rec 1).
 * [onBack] returns to the shelf from the boot-error state: with the seed-on-miss retired a missing
 * project is a normal user path, and the root editor error must not be a dead end (ADR-046 §3).
 */
@Composable
private fun EditorDestination(onPreview: () -> Unit, onBack: () -> Unit) {
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
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.message)
                androidx.compose.material3.TextButton(onClick = onBack) { Text("‹  Back to your shelf") }
            }
        }

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
