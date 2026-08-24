package com.aritr.zinely.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.a11y.zinelyV2Control
import com.aritr.zinely.ui.components.ZinelyV21FocusOffsetLibrary
import com.aritr.zinely.ui.components.zinelyFocusRing
import com.aritr.zinely.ui.components.zinelySweep
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.components.zinelyV21Pressable
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import com.aritr.zinely.MainActivity
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.export.ExportDestination
import com.aritr.zinely.export.ExportFormat
import com.aritr.zinely.export.ExportReady
import com.aritr.zinely.export.ExportSaved
import com.aritr.zinely.export.ExportUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import com.aritr.zinely.export.ExportViewModel
import com.aritr.zinely.home.HomeUiState
import com.aritr.zinely.home.HomeViewModel
import com.aritr.zinely.home.LibraryBackupRestorePickerRequest
import com.aritr.zinely.feature.library.LibraryShelfState
import com.aritr.zinely.feature.library.LibraryBackupRestoreMode
import com.aritr.zinely.feature.library.ZineLibraryScreen
import androidx.compose.runtime.DisposableEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.aritr.zinely.feature.editor.EditorScreen
import com.aritr.zinely.feature.editor.ProofExportTarget
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
                // **Share & export pushes the editor and THEN the Proof** — the one navigation invariant
                // B5 can break expensively ([ADR-086](../../../../../../docs/DECISIONS.md#adr-086) §6).
                // `ProofRoute` resolves the *shared* editor ViewModel off the editor's live back-stack
                // entry (the ADR-026 single-writer seam, see below), so a direct navigate to the Proof
                // finds no such entry and **throws at runtime**. The failure is a crash rather than a
                // wrong pixel, which is why the assertion is on the back stack.
                //
                // Going *back* from the Proof therefore lands on the bench, not the shelf. That is the
                // existing flow's own behaviour, and D-025 says reuse the flow — which means its
                // behaviour too, not merely its destination.
                onShareExport = { id ->
                    navController.navigate(EditorRoute(id)) { launchSingleTop = true }
                    navController.navigate(ProofRoute(id))
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
            val editorEntry = remember(entry) {
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
 * [HomeViewModel] and threads its state/events into the stateless
 * [ZineLibraryScreen][com.aritr.zinely.feature.library.ZineLibraryScreen]. Navigation is this
 * layer's concern, not the screen's: card taps and "Start a zine" both route through the VM (which
 * first commits any pending undoable deletes — leaving the shelf is a snackbar dismissal, ADR-046
 * §4) and come back as one-shot [HomeViewModel.openEvents] ids collected here into [onOpenZine].
 */
@Composable
private fun HomeDestination(
    onOpenZine: (String) -> Unit,
    onShareExport: (String) -> Unit,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backupRestoreState by viewModel.backupRestoreState.collectAsStateWithLifecycle()
    val preferredPaper by viewModel.preferredPaper.collectAsStateWithLifecycle()
    val appVersion = remember(context) {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        viewModel.backupPicked(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.restorePicked(uri)
    }

    LaunchedEffect(viewModel) {
        viewModel.openEvents.collect { id -> onOpenZine(id) }
    }
    LaunchedEffect(viewModel, backupLauncher, restoreLauncher) {
        viewModel.backupRestorePickerRequests.collect { request ->
            launchBackupRestorePicker(
                request = request,
                launchBackup = backupLauncher::launch,
                launchRestore = restoreLauncher::launch,
                onFailure = viewModel::backupRestorePickerFailed,
            )
        }
    }

    ZineLibraryScreen(
        state = state.toLibraryShelfState(),
        events = viewModel.events,
        backupRestoreState = backupRestoreState,
        onOpenZine = viewModel::openZine,
        onShareExport = onShareExport,
        onStartZine = viewModel::startZine,
        onRenameZine = viewModel::rename,
        onDuplicateZine = viewModel::duplicate,
        onDeleteZine = viewModel::delete,
        onDeleteUndo = viewModel::undoDelete,
        onDeleteCommit = viewModel::commitDelete,
        onRetry = viewModel::retry,
        onStartBackup = viewModel::startBackup,
        onStartRestore = viewModel::startRestore,
        onDismissBackupRestore = viewModel::dismissBackupRestoreSurface,
        onCancelBackupRestore = viewModel::cancelBackupRestore,
        onRetryBackupRestore = viewModel::retryBackupRestore,
        preferredPaper = preferredPaper,
        appVersion = appVersion,
        onPreferredPaperChange = viewModel::setPreferredPaper,
        modifier = Modifier.fillMaxSize(),
    )
}

internal fun launchBackupRestorePicker(
    request: LibraryBackupRestorePickerRequest,
    launchBackup: (String) -> Unit,
    launchRestore: (Array<String>) -> Unit,
    onFailure: (LibraryBackupRestoreMode) -> Unit,
) {
    try {
        when (request) {
            is LibraryBackupRestorePickerRequest.Backup -> launchBackup(request.suggestedName)
            LibraryBackupRestorePickerRequest.Restore -> launchRestore(arrayOf("*/*"))
        }
    } catch (_: RuntimeException) {
        onFailure(
            if (request is LibraryBackupRestorePickerRequest.Backup) {
                LibraryBackupRestoreMode.Backup
            } else {
                LibraryBackupRestoreMode.Restore
            },
        )
    }
}

/**
 * [HomeUiState] → the Library's four states (B5).
 *
 * A total `when` over a sealed type on both sides, so the mapping cannot lose a state — and
 * [HomeUiState.Empty] is the **honest** empty signal (ADR-044 §3): it means the *store* is empty, so a
 * shelf filtered to zero by a pending undoable delete stays a zero-zine [LibraryShelfState.Content] and
 * never flashes the invitation over a zine the user can still get back.
 */
private fun HomeUiState.toLibraryShelfState(): LibraryShelfState = when (this) {
    is HomeUiState.Loading -> LibraryShelfState.Loading
    is HomeUiState.Error -> LibraryShelfState.Error
    is HomeUiState.Empty -> LibraryShelfState.Empty
    is HomeUiState.Content -> LibraryShelfState.Content(zines)
}

/**
 * The single **Proof** host (M5, [ADR-051](../../../../../../docs/DECISIONS.md#adr-051)) — the collapse
 * of the former Preview + Export + Completion triad into one surface ([ProofScreen]). That surface was a
 * three-act climb until [ADR-101](../../../../../../docs/DECISIONS.md#adr-101) P1 retired the acts; it is
 * now the reader, a band, and two drawers. It boots the
 * *shared* editor VM (the ADR-026 single-writer seam, same back-stack-entry resolution the triad used)
 * and hosts its own read-only [ExportViewModel] over that shared document so `export == preview`
 * ([ADR-039]) without touching the single-writer autosave path. This composable owns the Android edges the
 * VM must not: it routes each finished export purely by the emitted [com.aritr.zinely.export.ExportOutcome]
 * subtype (ADR-054) — [ExportReady] → the OS share chooser (`ACTION_SEND`), [ExportSaved] → the post-save
 * Fold hand-off. It never remembers which button started the export; the subtype decides.
 *
 * Per [ADR-052] there is **no OS print path** — the honest home-print handoff is Save PDF (a durable copy
 * to Downloads, ADR-054) + Share. The chosen paper size is Proof-local UI state threaded into the export as
 * a document copy (no store write), so a paper change flows to the file without mutating the document.
 */
@Composable
private fun ProofDestination(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
) {
    val boot by viewModel.bootState.collectAsStateWithLifecycle()
    val exportViewModel: ExportViewModel = hiltViewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // The Proof stacks ABOVE the editor over the same ViewModel, so without this a share landing here is
    // reported to nobody — D-081 Q9. The sink's RESUMED gate is what stops the two destinations from both
    // reporting it during the transition between them.
    ImportReportSink(viewModel)

    // One emission per successful Save-PDF render, carrying the actual saved display name — drives the
    // Proof band's `.done` completion, whose copy names that file (the ADR-041 post-export → fold payoff,
    // now intra-screen). extraBufferCapacity so the collector's tryEmit never suspends.
    //
    // This flow is deliberately transient and **not** replayed: the *event* must not re-fire on a config
    // change. The screen's `.done` state is what persists, in a `rememberSaveable` there — so a rotation
    // keeps the completion without re-announcing it, and it cannot resurrect a completion the screen
    // cleared. (ADR-101 P2 retired the five-second snackbar this used to raise.)
    val saved = remember { MutableSharedFlow<String>(extraBufferCapacity = 1) }

    // Route each finished export purely by its ExportOutcome subtype (ADR-054) — no remembered target.
    // Collect only while STARTED so an export finishing while backgrounded doesn't launch at a stopped
    // lifecycle — the buffered Channel holds the event until resume.
    LaunchedEffect(exportViewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            exportViewModel.outcomes.collect { outcome ->
                when (outcome) {
                    // Share → the OS chooser (ACTION_SEND, ADR-039 §4). This is the only Intent path now,
                    // so the no-app-installed failure is narrowed to it.
                    is ExportReady -> try {
                        context.startActivity(
                            Intent.createChooser(
                                shareIntent(outcome.uri, outcome.mime),
                                Copy.Nav.SHARE_CHOOSER_TITLE,
                            ).apply {
                                // **Zinely must not appear in Zinely's own share sheet** (D-081 ruling #8).
                                // Share-in (ADR-105) registered an `image/*` filter on MainActivity, and
                                // `ZineExporter` can emit `image/png` — which matches it, so such a share
                                // would offer "send this zine to Zinely", a loop that re-imports the export
                                // as a photo.
                                //
                                // ⚠ Latent, not live, and the device is what established that: `outcome.mime`
                                // is `application/pdf` for every share a maker can currently reach, because
                                // both `ExportFormat` requests below ask for PDF and nothing in `src/main`
                                // asks for PNG. Verified on hardware — the Samsung chooser offers no Zinely
                                // for the Proof's share, which proves the mime type and NOT this exclusion.
                                // It stays because the day any surface shares a PNG the loop appears with no
                                // warning, and this line is cheaper than the bug report.
                                // EXTRA_EXCLUDE_COMPONENTS (API 24; minSdk is 24, so no
                                // guard) goes on the **chooser**, not the inner send Intent: the chooser is
                                // the Activity that reads it, and on the payload it would just be an unread
                                // extra handed to whatever app the maker picked.
                                putExtra(
                                    Intent.EXTRA_EXCLUDE_COMPONENTS,
                                    arrayOf(ComponentName(context, MainActivity::class.java)),
                                )
                            },
                        )
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, Copy.Nav.NO_APP_TO_OPEN, Toast.LENGTH_SHORT).show()
                    }
                    // Save PDF → a durable copy is already in Downloads (ADR-054): raise the band's
                    // completion and its fold hand-off (ADR-041), naming the file actually written. No
                    // Intent, so no ActivityNotFound path here.
                    is ExportSaved -> saved.tryEmit(outcome.displayName)
                }
            }
        }
    }

    when (val state = boot) {
        is EditorBootState.Ready -> {
            val uiState by state.store.uiState.collectAsStateWithLifecycle()
            val exportState by exportViewModel.state.collectAsStateWithLifecycle()
            // The Proof-local paper choice (ADR-052): seeded from the document, threaded into the export as
            // a copy so `export == what you see` without writing through the single-writer store (ADR-026).
            val paper = rememberSaveable { mutableStateOf(uiState.document.paperSize) }
            ProofScreen(
                // The `zineName` this used to pass was `Copy.Nav.ZINE_NAME_FALLBACK` — a placeholder,
                // because the real title lives in Room project metadata (ADR-042) and never reached the
                // editor boot state. ADR-101 P6 retired the top bar's name outright, so the placeholder
                // goes with it and nothing is owed here.
                onBack = onBack,
                paper = paper.value,
                onPaperSelected = { paper.value = it },
                onExportPdf = { target ->
                    exportViewModel.export(
                        uiState.document.copy(paperSize = paper.value),
                        state.pageSizePt,
                        state.imageBytes,
                        ExportFormat.PDF,
                        target.toDestination(),
                    )
                },
                // Single-flight, but named: WHICH button is rendering, not merely that one is. Both
                // controls still go non-interactive (the VM drops a concurrent tap, so an enabled button
                // that silently does nothing would be a worse lie) — only the running one wears the
                // in-flight treatment.
                busyTarget = (exportState as? ExportUiState.Working)?.destination?.toTarget(),
                // A failed render surfaces the recoverable error overlay (carries forward the retired
                // ExportScreen's error surfacing — never a silent failure), and it now names the action
                // that failed, so "Try again" is a promise the screen can keep.
                failedTarget = (exportState as? ExportUiState.Error)?.destination?.toTarget(),
                onRetryExport = {
                    // ⚠ No `dismissError()` here. It would clear the very state `retry` reads the
                    // destination from — and `export` overwrites Error with Working by itself.
                    exportViewModel.retry(
                        uiState.document.copy(paperSize = paper.value),
                        state.pageSizePt,
                        state.imageBytes,
                        ExportFormat.PDF,
                    )
                },
                // The post-export hand-off (ADR-041): one signal per successful Save-PDF, which raises
                // the band's `.done` block and its "Fold it up".
                savedSignals = saved,
                // The Read act (ADR-058) — the finished zine, page by page. Everything it needs was
                // already resolved here for the export path (the ADR-051 shared-VM seam), so this is
                // three existing values threaded one level down, not a new seam: the SAME document,
                // page size and image source the PDF is rendered from, which is what makes
                // `read == export` structural rather than a claim.
                pages = uiState.document.pages,
                pageSizePt = state.pageSizePt,
                defaults = uiState.document.defaults,
                imageBytes = state.imageBytes,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Loading is a transient window (e.g. re-bootstrapping after process-death restore) — the
        // unprinted sheet, not a spinner. See [BootLoading].
        EditorBootState.Loading -> BootLoading()

        // A failed reopen must not strand the Proof on an infinite spinner: show the honest message and a
        // way back to the editor, mirroring EditorDestination's Error branch.
        is EditorBootState.Error -> BootFailure(
            message = state.message,
            actionLabel = Copy.Nav.BACK_TO_EDITING,
            onAction = onBack,
        )
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
 * Map the Proof export button (the feature-local [ProofExportTarget]) to the app-level [ExportDestination]:
 * [ProofExportTarget.SEND] → [ExportDestination.TRANSPORT] (share the cache file), [ProofExportTarget.SAVE]
 * → [ExportDestination.DOWNLOADS] (a durable copy). The feature enum never reaches the VM/exporter — the
 * host does this one mapping (ADR-054 Decision 3).
 */
internal fun ProofExportTarget.toDestination(): ExportDestination = when (this) {
    ProofExportTarget.SEND -> ExportDestination.TRANSPORT
    ProofExportTarget.SAVE -> ExportDestination.DOWNLOADS
}

/**
 * The inverse, so the *state* can say which button it belongs to (ADR-102).
 *
 * The mapping already existed one way only, which is exactly how the two commit buttons ended up sharing
 * one in-flight flag: the host could turn a tap into a destination, but could not turn a running export
 * back into the button that started it, so it asked "is anything exporting?" and both buttons answered.
 */
internal fun ExportDestination.toTarget(): ProofExportTarget = when (this) {
    ExportDestination.TRANSPORT -> ProofExportTarget.SEND
    ExportDestination.DOWNLOADS -> ProofExportTarget.SAVE
}

// ---------------------------------------------------------------------------------------------
// The two boot states, in V2.1. Both the editor and the Proof boot the SAME EditorViewModel and so
// have the same two non-Ready states; they were two hand-written copies of a bare
// `CircularProgressIndicator` and a bare `Text` + `TextButton` — the last framework defaults in the
// app. One implementation each, because a divergence between them was never a feature.
// ---------------------------------------------------------------------------------------------

/** The unprinted sheet the editor and the Proof wait behind — `.ph`. */
internal const val BootLoadingTestTag: String = "boot-loading"

/** The whole failure column — `.fail`. */
internal const val BootFailureTestTag: String = "boot-failure"

/** Its one control — `.retry`. */
internal const val BootFailureActionTestTag: String = "boot-failure-action"

/**
 * **Opening a zine is a state no prototype freezes**, so this is drawn by analogy with the nearest
 * frozen surface: the Library's own loading state, `.ph` (`v21-library.html:271-281`), whose comment in
 * the frozen file is *"loading — the same objects, unprinted"*.
 *
 * ```css
 * .ph{aspect-ratio:3/4;border-radius:var(--br-xs) var(--br-md) var(--br-md) var(--br-xs);
 *     background:var(--paper);border:1.5px dashed var(--hair);
 *     box-shadow:var(--hard) var(--hard) 0 var(--hair)}
 * .ph::after{background:linear-gradient(100deg,transparent 20%,rgba(255,255,255,.5) 50%,transparent 80%);
 *     transform:translateX(-100%);animation:sweep 1.5s cubic-bezier(.2,0,0,1) infinite}
 * ```
 *
 * The analogy is exact in the one place it has to be: **the object being waited for**. The Library waits
 * for covers and draws four cover-shaped `.ph`s at `.zine`'s box; the editor and the Proof wait for a
 * page and draw **one**, at `.page`'s box (`v21-bench.html:177` — `width:100%;max-width:266px;
 * aspect-ratio:3/4`). The paint is `.ph`'s, unchanged, because the statement it makes — *"the thing you
 * asked for is not printed yet"* — is the statement this screen needs and is not a statement a spinner
 * can make. A `CircularProgressIndicator` was what stood here, and it said only *"software is busy"*.
 *
 * The ground is `desk`, which is what both destinations resolve to when they are Ready. A boot window
 * that changes the room's colour and then changes it back is a flash, not a transition.
 *
 * ### ⚠ Its hard shadow is `hair`, and that is the corpus's **second** exception to `inkLine`
 *
 * `box-shadow:var(--hard) var(--hard) 0 var(--hair)` — transcribed as frozen. `.fail .mk`'s `jam` shadow
 * is the exception V21-SPEC §4.3 names; this is the other one, and it is recorded here because a
 * count-based audit of "shadow = `inkLine`" reads an unexplained `hair` as the defect it is designed to
 * catch. The reason it is right: an unprinted sheet has not been printed, so nothing about it is drawn
 * at full strength yet — the shadow is as provisional as the dashed edge above it.
 *
 * ### The sweep is [zinelySweep], not a second implementation of it
 *
 * The shimmer already exists as a public modifier, transcribed from the same frozen gradient, and it
 * already answers reduced motion (it freezes the band in place rather than removing it, so the surface
 * still reads as *loading* and can never strobe). ⚠ Its white is `.35` where V2.1's `.ph::after` writes
 * `.5`; recorded rather than forked, because a second sweep in the codebase is a worse defect than a 15%
 * band, and the fix belongs in the one modifier.
 *
 * ### It keeps the indicator's semantics without keeping the indicator
 *
 * `progressBarRangeInfo = Indeterminate` is exactly what M3's `CircularProgressIndicator` contributed to
 * the tree, and dropping the component must not drop it: it is what makes a service say *"in progress"*
 * rather than announce an empty screen. The frozen `.ph` is silent, but the frozen `.ph` is one of four
 * tiles under a shelf that speaks for itself — here the placeholder **is** the screen.
 *
 * ⚠ **That paragraph claimed more than the code delivered, and a review said so.** `Indeterminate` alone
 * reaches TalkBack as *"progress bar"* — the control's category, not a statement about what is in
 * progress — so the sentence *"makes a service say **in progress**"* was describing an announcement the
 * screen did not make. The `contentDescription` is the missing half; it was equally missing from the M3
 * indicator this replaced, which is why nothing regressed and nothing noticed.
 */
@Composable
internal fun BootLoading() {
    val colors = ZinelyTheme.v21Colors
    Box(
        modifier = Modifier
            .testTag(BootLoadingTestTag)
            .fillMaxSize()
            .background(colors.desk)
            .padding(ZinelyV21Dimens.gapXl)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                contentDescription = Copy.Editor.OPENING_ZINE
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .widthIn(max = BootPageMaxWidth)
                .fillMaxWidth()
                .aspectRatio(BootPageAspect)
                // ⚠ Nothing that clips may sit LEFT of a hard shadow: it paints outside the node's own
                // bounds. The `clip` is deliberately downstream of it.
                .zinelyV21HardShadow(ZinelyV21Dimens.hardShadow, colors.hair, BootPageShape)
                .clip(BootPageShape)
                .background(colors.paper)
                // `border:1.5px dashed var(--hair)` on a border-box element — the stroke is INSIDE the
                // shape. Stroked at double width along the shape's own outline and left to the clip
                // above, which cuts the outer half away: the same 1.5dp CSS draws, with no inset path
                // whose corner radii would have to be recomputed. The Library's placeholder does this
                // identically, and the sheet's top rule does it too.
                .drawBehind {
                    drawOutline(
                        outline = BootPageShape.createOutline(size, layoutDirection, this),
                        color = colors.hair,
                        style = Stroke(
                            width = BootPageBorder.toPx() * 2f,
                            // CSS derives a dash rhythm from the border width; at 1.5px it renders at
                            // roughly 3on/3off. The one approximated value here, recorded as one.
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(BootPageDash.toPx(), BootPageDash.toPx()),
                            ),
                        ),
                    )
                }
                // Inside the clip and after the ground, which is where `::after` paints.
                .zinelySweep(),
        )
    }
}

/**
 * **A boot failure is not frozen either**, so it is drawn by analogy with `.fail` — the Library's error
 * state (`v21-library.html:311-322`), implemented by
 * [ZineShelfFail][com.aritr.zinely.feature.library.ZineShelfFail], which this follows structurally:
 * the rotated `!` mark, then the sentence, then one quiet `.retry` pill.
 *
 * ```css
 * .fail{inset:0;flex-direction:column;align-items:center;justify-content:center;text-align:center;
 *       padding:var(--gap-2xl);gap:var(--gap-md)}
 * .fail h2{font-family:var(--voice);font-size:1.6rem;font-weight:700;margin:var(--gap-sm) 0 0}
 * .retry{background:var(--paper);color:var(--ink);border:1.5px solid var(--ink);
 *        border-radius:var(--br-pill);padding:var(--gap-md) var(--gap-xl);
 *        box-shadow:3px 3px 0 var(--ink-line)}
 * ```
 *
 * ### Two deliberate departures from `.fail`, both about not inventing copy
 *
 * 1. **There is no `.fail p`.** `.fail` carries a fixed headline *and* a fixed paragraph; here the only
 *    sentence in existence is the ViewModel's own [message] ("Couldn’t open this project."), and it is a
 *    headline-shaped sentence. It is therefore set in the `h2` slot rather than the paragraph's, and no
 *    second line is written to sit above it. Saying the same thing twice in two type sizes is what a
 *    screen does when it has been given a template instead of a state.
 * 2. **No `150px` bottom padding.** `.fail`'s asymmetric pad centres its column above the Library's dock;
 *    neither of these destinations has a dock, so the column centres in the whole screen. Carrying the
 *    number across would push the content up against nothing.
 *
 * The information is intact ([message] says what failed, the control says what happens next) — the whole
 * point of the branch, and the reason nothing here is "tidied" into a bare icon.
 *
 * ### No frame ring
 *
 * The ring is reserved for a screen's one *primary* action, and `.retry` is expressly not one — the
 * frozen file gives the paper/ink pill on the **Raised** tier to recovery and keeps `--leaf`, the Hero
 * tier and the ring for `.start`. A leaving-the-screen control is the same kind of thing as retrying, so
 * it takes the same paint. This screen therefore wears no ring at all, which is allowed; wearing one on
 * its only control would make the ring mean "a control" rather than "the action".
 *
 * @param message the boot failure, verbatim from [EditorBootState.Error]. Never summarised here.
 * @param actionLabel the way out — the shelf from the editor, the editor from the Proof.
 */
@Composable
internal fun BootFailure(message: String, actionLabel: String, onAction: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    Column(
        modifier = Modifier
            .testTag(BootFailureTestTag)
            .fillMaxSize()
            .background(colors.desk)
            .padding(ZinelyV21Dimens.gap2Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        // `gap:var(--gap-md)` and `justify-content:center`, as one arrangement.
        verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapMd, Alignment.CenterVertically),
    ) {
        BootFailureMark()

        // `.fail h2{font-family:var(--voice);font-size:1.6rem;font-weight:700;margin:var(--gap-sm) 0 0}`
        // — Averia 700, the only weight above 400 the bundled voice face has. No `line-height` is
        // declared, so it inherits.
        //
        // `heading()` because the frozen markup is an `<h2>`: dropping the role costs TalkBack a landmark
        // while looking identical. No `max-width` — `.fail h2` declares none, and the paragraph's `28ch`
        // belongs to the paragraph this screen does not have.
        Text(
            text = message,
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Voice,
                fontWeight = FontWeight.Bold,
                fontSize = FailHeadlineSize,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                color = colors.ink,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = ZinelyV21Dimens.gapSm)
                .semantics { heading() },
        )

        BootFailureAction(actionLabel, onAction)
    }
}

/**
 * `.fail .mk` — a 60dp rotated disc with a `!` in it, `jam` outline and `jam` shadow on `paper`.
 *
 * ```css
 * .fail .mk{width:60px;height:60px;border-radius:var(--br-pill);background:var(--paper);
 *   border:2px solid var(--jam);color:var(--jam);font-family:var(--voice);font-size:1.8rem;
 *   font-weight:700;transform:rotate(-4deg);box-shadow:3px 3px 0 var(--jam)}
 * ```
 *
 * `jam` is the corpus's one urgent ink and this is an urgent state, so it is used as written — as an
 * outline on paper, never as a fill, which is what keeps it a warning mark rather than a deletion. The
 * shadow is `jam` too: the single hard shadow in the corpus that is not `inkLine`, and the reason
 * V21-SPEC §4.3 counts `inkLine` uses rather than asserting all of them.
 *
 * `jam` and not `jamText` for the glyph, matching
 * [ZineShelfFail][com.aritr.zinely.feature.library.ZineShelfFail]: §4.1 moves jam-as-text onto `jamText`
 * everywhere *except* this mark, which at 28.8sp bold is large text and needs 3:1. Painting one 60dp
 * object in two reds is what happens when the exception is missed.
 *
 * **Silent to TalkBack** — the sentence below says what the mark means, in words, and a reader that
 * opened with "!" would spend a whole utterance on punctuation. `clearAndSetSemantics {}` rather than a
 * null description, because the `!` is a `Text` node and would otherwise be read as content.
 */
@Composable
private fun BootFailureMark() {
    val colors = ZinelyTheme.v21Colors
    Box(
        Modifier
            .size(FailMarkSize)
            .graphicsLayer { rotationZ = FailMarkRotation }
            .zinelyV21HardShadow(FailMarkShadow, colors.jam, FailMarkShape)
            .clip(FailMarkShape)
            .background(colors.paper)
            .border(FailMarkBorder, colors.jam, FailMarkShape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = FailMarkText,
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Voice,
                fontWeight = FontWeight.Bold,
                fontSize = FailMarkTextSize,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                color = colors.jam,
            ),
        )
    }
}

/**
 * `.retry` — `paper` under an `ink` border at pill radius, `.94rem` semibold, `12px 24px`, on the
 * **Raised** tier: `3px 3px 0` at rest, `translate(2px,2px)` and `1px 1px 0` pressed.
 *
 * The border is a real CSS `border`, which sits **inside** the element's box, so `Modifier.border` is
 * correct — the opposite of the focus ring, where a CSS `outline` grows outward from its offset.
 *
 * The ring itself is `.start`'s (`outline:2px solid var(--ink);outline-offset:5px`). `.retry` declares
 * none, and borrowing the one ring the frozen file does author is transcription; inventing a second
 * appearance is what D-008 forbids. It is not optional: the M3 `TextButton` this replaces carried focus
 * indication of its own, and a control that loses its focus state to a re-skin is a keyboard regression
 * wearing better paint.
 */
@Composable
private fun BootFailureAction(label: String, onAction: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()

    Text(
        text = label,
        style = TextStyle(
            fontFamily = ZinelyV21Fonts.Work,
            fontWeight = FontWeight.SemiBold,
            fontSize = RetryLabelSize,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            color = colors.ink,
        ),
        modifier = Modifier
            // Above `zinelyV2Control`, which ends in `clearAndSetSemantics` and would swallow a tag
            // chained after it — the seam's own KDoc.
            .testTag(BootFailureActionTestTag)
            .padding(top = ZinelyV21Dimens.gapSm)
            .zinelyV21Pressable(pressed, ZinelyV21Press.Raised, colors.inkLine, RetryShape)
            .zinelyFocusRing(focused, ZinelyV21Dimens.radiusPill, ZinelyV21FocusOffsetLibrary)
            .clip(RetryShape)
            .background(colors.paper)
            .border(RetryBorderWidth, colors.ink, RetryShape)
            .zinelyV2Control(
                label = label,
                onClick = onAction,
                interactionSource = interaction,
            )
            .padding(horizontal = ZinelyV21Dimens.gapXl, vertical = ZinelyV21Dimens.gapMd),
    )
}

// ---------------------------------------------------------------------------------------------
// The frozen values. `.ph` is transcribed from `v21-library.html`, its box from `v21-bench.html`;
// `.fail .mk` and `.retry` from `v21-library.html`.
// ---------------------------------------------------------------------------------------------

/** `.page{width:100%;max-width:266px;aspect-ratio:3/4}` — the object the wait is for. */
private val BootPageMaxWidth = 266.dp
private const val BootPageAspect = 3f / 4f

/**
 * `.ph{border-radius:var(--br-xs) var(--br-md) var(--br-md) var(--br-xs)}` — the spine on the left and
 * the fore-edge on the right.
 *
 * Absolute, not logical: a printed object does not mirror in a right-to-left layout (**D-019**), the same
 * ruling the cover and the little book follow.
 */
private val BootPageShape: Shape = AbsoluteRoundedCornerShape(
    topLeft = ZinelyV21Dimens.radiusXs,
    topRight = ZinelyV21Dimens.radiusMd,
    bottomRight = ZinelyV21Dimens.radiusMd,
    bottomLeft = ZinelyV21Dimens.radiusXs,
)

/** `.ph{border:1.5px dashed var(--hair)}`; the dash rhythm is a browser default, approximated. */
private val BootPageBorder = 1.5.dp
private val BootPageDash = 3.dp

/** `.fail .mk{width:60px;height:60px;border:2px;font-size:1.8rem;rotate(-4deg);box-shadow:3px 3px 0}` */
private val FailMarkSize = 60.dp
private val FailMarkTextSize = 28.8.sp
private val FailMarkBorder = 2.dp
private val FailMarkShadow = 3.dp
private const val FailMarkRotation = -4f
private val FailMarkShape: Shape = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
private const val FailMarkText = "!"

/** `.fail h2{font-size:1.6rem}` = 25.6px. */
private val FailHeadlineSize = 25.6.sp

/** `.retry{font-size:.94rem;border:1.5px solid var(--ink);border-radius:var(--br-pill)}` = 15.04px. */
private val RetryLabelSize = 15.04.sp
private val RetryShape: Shape = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
private val RetryBorderWidth = 1.5.dp


/**
 * **Everything one [EditorViewModel] has to say about an import, spoken and shown.** Both halves live in
 * one composable so they cannot drift apart, and it is called from **both** the editor and the Proof —
 * which is [D-081](../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-081) Q9, and the reason it exists.
 *
 * The bug it fixes: both drains used to sit in [EditorDestination] alone, and the Proof stacks *above* the
 * editor, so a share that landed while the maker was on the Proof reported **nothing to anyone** — not the
 * toast, not the live region. Review found it worse than first reported (the a11y half was assumed safe)
 * and also bounded it: the Proof resolves the *same* ViewModel off the editor's back-stack entry, so the
 * drain keeps running and the photos still land and are durable. It was silence, never loss.
 *
 * **`repeatOnLifecycle(RESUMED)`, and the state is load-bearing.** Navigation-compose keeps the outgoing
 * destination composed through a transition, so a plain `LaunchedEffect` in both places would let one
 * emission reach two live collectors and toast twice. Exactly one entry is RESUMED at a time, so this
 * collects in exactly one place. The cost is honest and small: during the transition itself neither is
 * resumed, and these flows are replay-free, so an emission landing inside that window is still dropped —
 * the same pre-existing class of gap the announcement channel has always had, now narrowed from "the whole
 * time the Proof is open" to a few hundred milliseconds.
 */
@Composable
private fun ImportReportSink(viewModel: EditorViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Drain the VM's announcement channel to TalkBack. Bound to the View (composable-only), so it
    // lives here, not in the VM. Conflated buffer in the VM tolerates a brief subscriber gap.
    val view = LocalView.current
    LaunchedEffect(view, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.announcements.collect { text -> view.announceForAccessibility(text) }
        }
    }

    // The **visible** half of an import report (D-081 ruling #3, WCAG 3.3.1): "2 photos added, 3 couldn't
    // be" has to be readable, not only speakable, or a sighted maker watching 3 of 5 shared photos appear
    // is told nothing. A Toast rather than the editor's BenchSnack: the snack is a frozen surface with its
    // own copy contract driven by local state inside EditorScreen, so reusing it is a freeze question —
    // a Toast is a system surface no HTML draws. LENGTH_LONG because a two-clause sentence is not a "Saved".
    //
    // `applicationContext`, not the Activity: LENGTH_LONG is 3.5 s, and below API 30 the toast's view is
    // the app's own and outlives a destroyed Activity for that whole window. Holding the Activity there is
    // a leak with a timer on it.
    val toastContext = LocalContext.current.applicationContext
    LaunchedEffect(viewModel, toastContext, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.importSummaries.collect { text ->
                Toast.makeText(toastContext, text, Toast.LENGTH_LONG).show()
            }
        }
    }
}

/**
 * Hosts one [EditorViewModel] and renders its [EditorBootState]. The VM owns the store/binder for the
 * project's lifetime; this composable only (a) drives the autosave binder off the UI lifecycle and
 * (b) reports imports through [ImportReportSink] — neither belongs in the VM (Codex rec 1).
 * [onBack] returns to the shelf from the boot-error state: with the seed-on-miss retired a missing
 * project is a normal user path, and the root editor error must not be a dead end (ADR-046 §3).
 */
@Composable
private fun EditorDestination(onPreview: () -> Unit, onBack: () -> Unit) {
    val viewModel: EditorViewModel = hiltViewModel()
    val boot by viewModel.bootState.collectAsStateWithLifecycle()

    ImportReportSink(viewModel)

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
        EditorBootState.Loading -> BootLoading()

        is EditorBootState.Error -> BootFailure(
            message = state.message,
            actionLabel = Copy.Nav.BACK_TO_SHELF,
            onAction = onBack,
        )

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
            // The first-run Reframe coach-mark gate (ADR-053 RF2), same load-aware tri-state as the hint.
            val reframeCoachSeen by viewModel.reframeCoachSeen.collectAsStateWithLifecycle()
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
                reframeCoachSeen = reframeCoachSeen,
                onReframeCoachSeen = viewModel::markReframeCoachSeen,
                // Reframe a11y announcements (ADR-053 IF3) ride the same announceForAccessibility drain as
                // the reducer's selection/undo lines (bound at line ~302).
                onReframeAnnounce = viewModel::announce,
                onStyleAnnounce = viewModel::announce,
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
