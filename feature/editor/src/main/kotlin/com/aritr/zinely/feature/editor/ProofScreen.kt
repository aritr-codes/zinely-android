package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.ui.components.ZIconButton
import com.aritr.zinely.ui.components.ZSheet
import com.aritr.zinely.ui.components.ZSheetClose
import com.aritr.zinely.ui.components.ZStatusPane
import com.aritr.zinely.ui.components.zinelyFocusRing
import com.aritr.zinely.ui.components.zinelyV21Frame
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Press
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// Test tags — the stable handles the ProofScreen suite and host tests address.
public const val ProofScreenTestTag: String = "proof-screen"
public const val ProofBackTestTag: String = "proof-back"
public const val ProofPrimaryTestTag: String = "proof-primary"
// `ProofSecondaryTestTag` is deleted. It named the retired climb's Back control, and since P1 it was
// applied to nothing — while two tests went on asserting `assertDoesNotExist()` on it. An assertion that
// cannot fail is worse than no assertion: it reports coverage of a thing nobody is checking.
/**
 * The top bar's `.pcount` ticket — the surface's one page readout, and its one live region.
 *
 * Replaces `ProofActLabelTestTag` (P6). The stop it named — a zine name and an act status line — is gone
 * with the frozen top bar; see [ProofTopBar].
 */
public const val ProofPageTicketTestTag: String = "proof-page-ticket"
public const val ProofErrorPaneTestTag: String = "proof-error-pane"
public const val ProofRetryTestTag: String = "proof-retry"

/** The band's opener into the print details — the frozen `.ready` row. */
public const val ProofReadyTestTag: String = "proof-ready"

/** The top bar's right icon — the frozen `How to fold` button. */
public const val ProofFoldOpenTestTag: String = "proof-fold-open"

// The band's `.commit` pair (ADR-101 P2 moved both out of the print recipe). The tag strings are
// unchanged, so nothing that addressed them had to be renamed.
public const val ProofSavePdfTestTag: String = "proof-save-pdf"
public const val ProofShareTestTag: String = "proof-share"

/** The band's `.done` completion block, raised in place of `.commit` once a PDF is in Downloads. */
public const val ProofDoneTestTag: String = "proof-done"

/** `.done`'s one action — the ADR-041 hand-off into the fold guide. */
public const val ProofFoldItUpTestTag: String = "proof-fold-it-up"
public const val ProofPaperChangedTestTag: String = "proof-paper-changed"

/**
 * Which bottom drawer is open, if any — the V2.1 Proof's whole navigation state
 * ([ADR-101](../../../../../../docs/DECISIONS.md#adr-101)).
 *
 * This replaces `ProofAct`, whose `SHEET`/`PRINT`/`FOLD` members described a three-step **climb** the
 * accepted V2.1 design retires: printing and folding are things you reach *from* your finished book, not
 * screens standing between you and it. [ADR-058](../../../../../../docs/DECISIONS.md#adr-058) had already
 * made Read the landing; this finishes the job.
 *
 * At most one drawer is open, which is why this is an enum and not two booleans — two booleans can express
 * "both open", a state the design has no drawing for.
 */
public enum class ProofDrawer { None, Details, Fold }

// `proof.html` primary/back icon path data (24×24 viewport), consumed verbatim so the chrome
// mirrors the frozen SVGs rather than re-drawing them.
private const val ICON_BACK = "M15 5l-7 7 7 7"
private const val ICON_FOLD = "M4 6h16v6l-8 6-8-6z"
private const val ICON_CHECK = "M5 12l5 5 9-11"
private const val ICON_CHEVRON = "M9 6l6 6-6 6"

/**
 * The height a drawer body may take, derived from the screen rather than fixed.
 *
 * **Two separate reasons it has to exist, and the second one cost a test run to find.**
 *
 * First: the drawer bodies ([ProofPrintDetailsPanel], [ProofFoldAct]) apply `.fillMaxSize().verticalScroll(...)`
 * internally, and [ZSheet] measures its content with an unbounded height. Dropping an act in unbounded
 * throws *"Vertically scrollable component was measured with an infinity maximum height constraints"*.
 * A bound gives each act a finite height to scroll inside.
 *
 * Second: the sheet is bottom-anchored and wraps its content, so a body taller than the screen does not
 * scroll — it pushes whatever follows it straight off the bottom edge. A fixed `560.dp` did exactly that
 * to the fold drawer's finish button, and the failure read as *"'It's folded — show me' is not
 * displayed"* — a control that was mounted, laid out, and unreachable.
 *
 * **The first fix for that was itself the bug, and review caught it.** It read
 * `(screenHeightDp * 0.88f - 174f).coerceAtLeast(240f)`, and a floor that clamps *upward* past the space
 * that actually exists is not a floor. Below 414dp of window height the body floored at 240, the sheet
 * demanded `240 + 174 = 414`, and the overflow went off the bottom — so the finish button and both exits
 * were unreachable in **landscape on shipped hardware** (a 430×932 phone reports ~411dp tall on its side,
 * and no orientation is locked), in multi-window, and in freeform. The accompanying ADR text called that
 * Robolectric's 470dp default being *"smaller than any shipped device"*, which was simply wrong: landscape
 * phones are exactly that short. Pinning a phone window in the tests hid a portrait-only reproduction of
 * a permanent condition.
 *
 * So there is no floor now, and no reserve for the actions: the caller lays the actions out **inside**
 * this bound with a `weight`, which is what makes them unclippable at any size or font scale rather than
 * unclipped at the sizes someone thought of. What remains subtracted is only the sheet's own fixed
 * chrome — grip, title, vertical padding — and if a font scale eats the rest, the guide shrinks to
 * nothing and scrolls while the actions stay put. That is the correct failure direction.
 */
@Composable
private fun drawerBodyMaxHeight(): Dp {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    // The frozen drawer's own rule is `max-height: 88%`.
    return ((screenHeight * 0.88f) - SheetChrome).coerceAtLeast(0f).dp
}

/**
 * The sheet's grip + head row (title and, since P3, the `.dclose` button) + vertical padding — [ZSheet]'s
 * own furniture, above whatever body it hosts. Measured: 26 + 32 + a 48dp head row = 106dp, so the 110
 * still covers the taller head with 4dp to spare.
 */
private const val SheetChrome = 110f

/**
 * The unified Proof surface — one screen answering *what did I make?*, with printing and folding reachable
 * from it rather than stacked in front of it.
 *
 * **V2.1 ([ADR-101](../../../../../../docs/DECISIONS.md#adr-101), package P1) retires the climb.** The three
 * acts [ADR-051](../../../../../../docs/DECISIONS.md#adr-051) established as one surface — Sheet → Print →
 * Fold — are gone as screens, and with them the act state machine, the slide transitions, the three passive
 * progress creases and the per-act reconfiguration of the bottom bar. What replaces them is the frozen
 * `v21-proof.html` shape: the reader fills the screen, a band beneath it carries the one opener and (from
 * P2) the commit actions, and two bottom drawers hold the print details and the fold guide. ADR-051's
 * *"you can always leave, nothing is lost"* invariant is kept and extended — back dismisses an open drawer
 * before it will leave the surface, so no drawer can strand you.
 *
 * **P2 finishes the band.** It is the frozen `.band` now: the two-line `.ready` row, the `.commit` pair
 * (Save PDF · Share) that used to sit at the bottom of the print recipe, and the `.done` completion that
 * replaces `.commit` once a PDF is in Downloads.
 *
 * **P3 rebuilds the print details** as one [ProofPrintDetailsPanel] — the two re-homed acts it used to
 * stack inside [drawerBodyMaxHeight] are gone. **P4 rebuilds the fold guide** onto the eight steps of
 * V21-SPEC §5.3. What is still a structural placeholder is the reader (P5), re-homed unchanged and still
 * painting in V1 tokens; the surface chrome and the V1→V2.1 token sweep are P6. Goldens are recorded once,
 * at the end, so nothing here is a raster commitment.
 *
 * The recoverable export-error overlay ([failedTarget], the frozen `#errwrap`) replaces the reader **and**
 * the band, leaving only the top bar's loss-safe back — one recovery action, never one competing with a
 * live Save. That is why the band needs no failure state of its own, and it is what answers the
 * still-open D-066 / OD-32.
 *
 * The [ADR-041](../../../../../../docs/DECISIONS.md#adr-041) post-export → fold hand-off is `.done`'s
 * *"Fold it up"*, and P2 **retired the transient snackbar that used to carry it**: the band already
 * changes under the user's thumb at exactly the place they pressed Save, and a nudge that expires in five
 * seconds is a worse home for the payoff than a block that waits. `.done` also keeps [ADR-054]'s check —
 * it names the file the exporter actually wrote — and carries [ADR-052]'s recipe in three words, because
 * it is the last sentence read before the walk to a printer. What it does not yet demonstrably keep is
 * the snackbar's *spoken* announcement; see [ProofDoneBlock].
 *
 * Stateless except for the drawer pointer, the fold's sub-state and the saved-file name — all
 * [rememberSaveable], so a rotation keeps its place *and* keeps the completion.
 *
 * @param onBack loss-safe back to the editor (the bench) — the work is saved. Only reached when no drawer
 *   is open; see [ProofDrawer].
 * @param pages the document's pages in reading order, for the reader. Empty renders no reader.
 * @param defaults document defaults the reader's renderer folds in; the same value the canvas uses.
 * @param pageSizePt the page size in points — the hoisted size the editor canvas renders at.
 * @param imageBytes import-master byte source, so the reader shows the user's photos, not placeholders.
 * @param startDrawer the drawer to open on arrival. Production takes the default ([ProofDrawer.None]);
 *   tests and goldens pass the drawer they actually mean. Replaces the retired `startAct`.
 * @param paper the paper size shown in the print details and named in the band's summary (the host
 *   threads it into the export).
 * @param onPaperSelected the user picked a paper size in the print details. Changing it also clears any
 *   `.done` state, because the PDF in Downloads was rendered at the *old* size — see [ProofBand].
 * @param onExportPdf export — the host renders the PDF and hands it to the [ProofExportTarget] edge.
 * @param busyTarget **which** export is in flight, or `null`. This is the visible half of a single-flight;
 *   the enforcing half is `ExportViewModel.export`, which returns early while `Working` and sets that
 *   state synchronously, so a double-tap could never have started two renders. What the frozen buttons
 *   lack is any drawing for the interval between — a control that looks live for two seconds and does
 *   nothing reads as a broken button, not as a busy one. It was a Boolean until ADR-102, and a Boolean
 *   cannot say *which*, so both commit buttons reacted to either tap.
 * @param failedTarget **which** export failed, or `null` — shows the recoverable error overlay, named.
 * @param onRetryExport the error overlay's "Try again" — re-fires the export named by [failedTarget].
 * @param savedSignals one emission per **successful Save-PDF** render, carrying the actual saved display
 *   name (e.g. `zine.pdf`) the exporter wrote to Downloads. Each raises the band's `.done` block, whose
 *   copy NAMES that file and its destination — the check that makes [ADR-054]'s *we kept a copy*
 *   verifiable. Share emits nothing: there is no durable copy to point at.
 * @param modifier sizing/placement for the whole surface.
 */
@Composable
public fun ProofScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    paper: PaperSize = PaperSize.A4,
    onPaperSelected: (PaperSize) -> Unit = {},
    onExportPdf: (ProofExportTarget) -> Unit = {},
    busyTarget: ProofExportTarget? = null,
    failedTarget: ProofExportTarget? = null,
    onRetryExport: () -> Unit = {},
    savedSignals: Flow<String> = emptyFlow(),
    pages: List<Page> = emptyList(),
    pageSizePt: PtSize = PtSize(0.0, 0.0),
    defaults: DocumentDefaults = DocumentDefaults(),
    imageBytes: AssetBytesSource = EmptyAssetBytes,
    startDrawer: ProofDrawer = ProofDrawer.None,
) {
    val colors = ZinelyTheme.v21Colors
    val haptics = ZinelyTheme.haptics

    var drawerOrdinal by rememberSaveable { mutableStateOf(startDrawer.ordinal) }
    // Coerced for the same reason the retired act pointer was: a Bundle written by an older build outlives
    // the enum that produced it, and a restore path nobody exercises by hand should degrade rather than
    // throw.
    val drawer = ProofDrawer.entries[drawerOrdinal.coerceIn(0, ProofDrawer.entries.lastIndex)]
    val reduceMotion = ZinelyTheme.motion.reduceMotion

    // The fold guide's step pointer. Hoisted so it survives closing the drawer to look at the paper —
    // the ordinary thing to do mid-fold — and a rotation with it.
    //
    // `foldFinished` and `climaxBeat` used to live here too. The climax they drove is retired; see
    // [ProofFoldAct] for the four reasons and [ADR-101 §6.8](../../../../../../docs/DECISIONS.md#adr-101-p4-device).
    var foldStep by rememberSaveable { mutableStateOf(0) }

    // The `.done` band state: the display name of the PDF this session put in Downloads, or null before
    // there is one. rememberSaveable, and that is the inversion P2 had to get right — the snackbar this
    // replaces was deliberately NOT saveable, because a transient nudge that re-announces itself after a
    // rotation is a bug. A completion the user can still act on is the opposite: losing it on rotation
    // would retract a statement about the filesystem that is still true.
    var savedName by rememberSaveable { mutableStateOf<String?>(null) }
    // The paper size the dropped `.done` was rendered for, or null when nothing was dropped. Saveable for
    // the same reason `savedName` is: the file in Downloads outlives a rotation, so the notice about it
    // has to as well. See [Copy.Proof.paperChangedResave].
    var staleFrom by rememberSaveable { mutableStateOf<String?>(null) }
    // Whether the user has told us they folded it (ADR-101 §6.9). Saveable for the same reason the two
    // above are — the paper on their desk survives a rotation. Cleared on a *new* save, because a new file
    // is a new sheet to print and fold; the claim was about the old one.
    var folded by rememberSaveable { mutableStateOf(false) }
    // The leaf the reader currently shows, 1-based — the top bar's ticket, hoisted here because the ticket
    // is drawn in the bar and the position is known in the reader. Saveable so a rotation restores the
    // ticket in the same frame the reader restores its leaf, rather than showing "Cover" for one frame.
    var leafPage by rememberSaveable { mutableIntStateOf(1) }
    // The error pane replaces the reader, which takes its `index`/`shown` with it — so on recovery the
    // reader remounts at the cover while this still holds the leaf the user was on, and the ticket would
    // read "Page 5 of 8" over the cover for one frame *and speak the correction*. Resetting on the way in
    // costs nothing: the ticket is hidden for the whole of the error state.
    LaunchedEffect(failedTarget) { if (failedTarget != null) leafPage = 1 }
    LaunchedEffect(savedSignals) {
        savedSignals.collect { savedName = it; staleFrom = null; folded = false }
    }


    fun open(target: ProofDrawer) {
        haptics.perform(ZinelyHaptic.Tick)
        drawerOrdinal = target.ordinal
    }
    fun closeDrawer() {
        drawerOrdinal = ProofDrawer.None.ordinal
    }

    /**
     * Finish the guide: acknowledge, and get out of the way.
     *
     * The frozen guide has no terminal state — it simply ends, and closing the drawer returns the user to
     * the zine they made. What this adds to the freeze is only the *acknowledgement*: a `Success` haptic
     * on a real completion, and no dead Next arrow on the last step. The five-beat book reveal that used
     * to live here is retired; [ProofFoldAct] carries the reasoning.
     *
     * The step pointer resets, so the guide is ready if they fold a second copy — and unlike the climax,
     * this state cannot strand anyone, because there is no state.
     *
     * **`folded` is what the band hears.** The haptic alone was the whole reply for one review round, and
     * a cold read caught what that leaves on screen: a stamp primary still saying *"Fold it up"* to
     * someone who has just reported folding it. See [Copy.Proof.NICE_THATS_A_ZINE].
     */
    fun finishFold() {
        haptics.perform(ZinelyHaptic.Success)
        foldStep = 0
        folded = true
        closeDrawer()
    }
    fun advanceFold() {
        if (foldStep < FOLD_LAST_STEP) {
            haptics.perform(ZinelyHaptic.Tick)
            foldStep += 1
        } else {
            // `→` on the last step finishes, matching the visible primary. The frozen `goStep` clamps and
            // does nothing; a keyboard user who can see a live finish button and cannot reach it with the
            // key that drives every other step is the worse of the two divergences.
            finishFold()
        }
    }
    fun retreatFold() {
        if (foldStep > 0) {
            haptics.perform(ZinelyHaptic.Tick)
            foldStep -= 1
        }
    }
    // Jump to any step. The frozen dots have always been buttons (`goStep(i)`); at eight steps that stops
    // being a nicety, because the likeliest thing to happen mid-fold is losing your place, and walking
    // back six arrow-presses to check step 2 is not a recovery. Coerced rather than trusted: the caller
    // is a UI index today, and a bounds check here costs nothing.
    fun goToFoldStep(target: Int) {
        val clamped = target.coerceIn(0, FOLD_LAST_STEP)
        if (clamped != foldStep) {
            haptics.perform(ZinelyHaptic.Tick)
            foldStep = clamped
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.desk)
            .testTag(ProofScreenTestTag),
    ) {
        Column(Modifier.fillMaxSize()) {
            ProofTopBar(
                // Null while the error pane stands in for the reader — there is no leaf to name.
                ticket = if (failedTarget != null || pages.isEmpty()) {
                    null
                } else {
                    Copy.ProofRead.leafLabel(leafPage.coerceIn(1, pages.size), pages.size)
                },
                // ADR-051's exit, unchanged in meaning: the work is autosaved, so leaving is never
                // destructive. It is only ever reached with every drawer shut — the surface's back
                // dismisses a drawer first (see [ProofDrawer]), so the button cannot be the thing that
                // strands a user inside one.
                onBack = onBack,
                onOpenFold = { open(ProofDrawer.Fold) },
            )

            if (failedTarget != null) {
                // Replaces the reader AND the band so there is exactly one recovery action; the top bar's
                // loss-safe back stays available. This carries forward the retired ExportScreen's error
                // surfacing — a failed render must never be silent — and is why the band needs no failure
                // state of its own (ADR-101 §3 item 1).
                ProofErrorPane(
                    onRetry = onRetryExport,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            } else {
                ProofReadAct(
                    pages = pages,
                    pageSizePt = pageSizePt,
                    defaults = defaults,
                    reduceMotion = reduceMotion,
                    imageBytes = imageBytes,
                    onLeafChange = { leafPage = it },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )

                ProofBand(
                    pageCount = pages.size,
                    paper = paper,
                    savedName = savedName,
                    folded = folded,
                    staleFrom = staleFrom,
                    busyTarget = busyTarget,
                    onOpenDetails = { open(ProofDrawer.Details) },
                    onSavePdf = { onExportPdf(ProofExportTarget.SAVE) },
                    onShare = { onExportPdf(ProofExportTarget.SEND) },
                    // Back to step 1. `.done` appears when a *new* PDF lands in Downloads, so its
                    // hand-off means "now fold this sheet", not "resume the sheet you were folding".
                    onFoldItUp = { foldStep = 0; open(ProofDrawer.Fold) },
                )
            }
        }
    }

    // ── The two drawers. ZSheet is Dialog-backed (ADR-049), which buys window-level modality: focus
    // containment, TalkBack isolation, and system-back dismissal for free. It also means each drawer is a
    // separate window, so the surface's root can no longer see key events aimed at drawer content — which
    // is why the fold guide's ←/→ handler lives inside the fold drawer rather than on the screen.
    ZSheet(
        visible = drawer == ProofDrawer.Details,
        onDismiss = ::closeDrawer,
        title = Copy.Proof.PRINT_DETAILS,
        // The frozen `.dclose`, owed since P1: a drawer covers most of the screen with scrolling content
        // and nothing in it named a way out. Scrim tap and system back still work; this is the one a
        // first-time user can see.
        close = ZSheetClose(Copy.Proof.CLOSE, ::closeDrawer),
    ) {
        ProofDetailsDrawerBody(
            paper = paper,
            // The same count the band and the ticket read. Three readouts, one number.
            pageCount = pages.size,
            // Changing the paper invalidates the copy in Downloads — it was imposed for the old size, and
            // "Saved to your phone" would go on being true about a file that no longer matches what the
            // recipe above it now says to print. Dropping `.done` puts Save PDF back in the band, which
            // is the honest reading and also the only way to re-save. It is also done behind a scrim, so
            // the band must SAY it happened; `staleFrom` is what carries that (see the note in the band).
            // ⚠ **Re-picking the size you are already on is not a change**, and this handler used to treat
            // it as one: it dropped `.done`, and raised a note reading *"Paper changed to US Letter — save
            // again to get a US Letter-sized PDF. The US Letter one is still in Downloads."* — a sentence
            // that names one size twice and contradicts itself, about an event that did not happen. It
            // also threw away a save the user still had. A segmented control invites exactly this tap,
            // because tapping the selected segment is how you check which one is selected.
            onPaperSelected = { picked ->
                if (picked != paper) {
                    if (savedName != null) staleFrom = paper.displayName
                    savedName = null
                    folded = false
                    onPaperSelected(picked)
                }
            },
            onOpenFold = { open(ProofDrawer.Fold) },
        )
    }

    ZSheet(
        visible = drawer == ProofDrawer.Fold,
        onDismiss = ::closeDrawer,
        // The frozen drawer's visible heading is `<h3>Fold it up</h3>`; "How to fold" is its `aria-label`
        // and the opener's label, which is where it stays.
        title = Copy.Proof.FOLD_IT_UP,
        // **The precondition is the drawer's subtitle, and it is shown on step 1 only** — the frozen
        // `#foldIntro` gate, arrived at the long way round.
        //
        // It began inside the guide, correctly gated, and on a real phone its disappearance moved every
        // control below it by 108px between steps 1 and 2, dropping the Next arrow onto the dots. Hoisting
        // it to the sheet chrome *and* showing it always fixed the shift by removing the variable — but
        // the variable was never the problem. Two independent reviews landed on the same reading from
        // opposite directions: it asks *"Got your printed sheet?"* at step 5 of someone visibly holding
        // one, and it spends two lines on every step of a drawer that is short of them; and shipping it
        // ungated left the built behaviour contradicting both the spec and this file's own comment.
        //
        // The gate is safe *here* where it was not safe there, and the difference is the anchoring. The
        // guide now wraps its content in a bottom-anchored sheet, so a shorter subtitle shrinks the drawer
        // from the **top** and every control keeps its distance from the screen's bottom edge. Measured on
        // device rather than argued: see [ADR-101 §6.8](../../../../../../docs/DECISIONS.md#adr-101-p4-device).
        sub = Copy.ProofFold.INTRO_BODY.takeIf { foldStep == 0 },
        close = ZSheetClose(Copy.Proof.CLOSE, ::closeDrawer),
    ) {
        // The guide, its finish action, the climax reveal and the finished exits all live in this one
        // drawer. That is a P1 decision worth naming: the finish primary used to sit in the per-act bottom
        // bar, which no longer exists, so leaving it there would have deleted "It's folded — show me"
        // outright. Putting the exits behind the scrim instead — in the band — would have been worse,
        // because the reveal happens *here*, and a payoff you cannot act on from where you are watching it
        // is not a payoff. The frozen design disagrees: it raises `.done` in the band on **save**, and
        // gives the fold drawer no terminal state at all. That is the open owner question in
        // [ADR-101 §5](../../../../../../docs/DECISIONS.md#adr-101-open); until it is answered, P1 keeps
        // the whole climax rather than dropping a shipped payoff on the way past.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Bounded, so the actions below the guide can be laid out by weight rather than by
                // arithmetic. See [drawerBodyMaxHeight] for why guessing the split does not survive
                // landscape.
                .heightIn(max = drawerBodyMaxHeight())
                // The spec's document-level ←/→ step nav. It must be attached in the drawer, not on the
                // surface root: the drawer is its own window, so a root handler would never see these keys
                // while the guide is up.
                //
                // It sits on this **outer** Column, not on the guide, and that placement is a fix rather
                // than a detail: Compose dispatches a preview key event down the ancestor chain of the
                // focused node only. With the handler on the guide alone, focusing the finish button —
                // which is exactly where focus lands on the last step — took the arrows out of the chain
                // and silently killed them. Here every focusable in the drawer is a descendant, which is
                // the reach the retired screen-root handler had.
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionRight -> { advanceFold(); true }
                            Key.DirectionLeft -> { retreatFold(); true }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
        ) {
            ProofFoldAct(
                step = foldStep,
                reduceMotion = reduceMotion,
                onNext = { advanceFold() },
                onPrev = { retreatFold() },
                onGoToStep = { goToFoldStep(it) },
                // weight, not a fixed height: the guide yields whatever the action row needs, so the
                // actions are reachable at every screen size and font scale. It scrolls internally.
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            )

            // The last step hands off to ONE action — never a dead primary, and never a next arrow
            // offering a step that does not exist. It acknowledges and closes; see [finishFold].
            if (foldStep == FOLD_LAST_STEP) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The last V1 control on the surface, and the sweep takes it too: it sat as a teal
                    // `stamp` primary at the foot of a fold guide painted in V2.1 since P4.
                    ProofBandButton(
                        text = Copy.Proof.ITS_FOLDED,
                        onClick = { finishFold() },
                        hero = true,
                        enabled = true,
                        press = ZinelyV21Press.Raised,
                        modifier = Modifier.weight(1f).testTag(ProofPrimaryTestTag),
                        icon = { tint -> ProofVectorIcon(ICON_CHECK, tint) },
                    )
                }
            }
        }
    }

}

/**
 * The recoverable export-error overlay — the frozen `#errwrap`: a coral warning badge, the honest
 * "your zine is safe" reassurance, and a single stamp "Try again" that re-fires the last export. One
 * action only; the top bar's loss-safe back is the other way out.
 */
@Composable
private fun ProofErrorPane(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v21Colors
    Box(modifier, contentAlignment = Alignment.Center) {
        ZStatusPane(
            title = Copy.Proof.COULDNT_MAKE_PDF,
            body = Copy.Proof.ERROR_BODY,
            // `--berry-tint` is the palette's own ground for a `jam-text` mark, and it is AA-measured for
            // that pairing — which a hand-mixed 14% wash of `jam` would not be.
            badgeBackground = colors.berryTint,
            badgeContent = colors.jamText,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .testTag(ProofErrorPaneTestTag),
            badgeIcon = { tint -> ProofWarnBadge(tint) },
            cta = {
                ProofBandButton(
                    text = Copy.Common.TRY_AGAIN,
                    onClick = onRetry,
                    hero = true,
                    enabled = true,
                    modifier = Modifier.testTag(ProofRetryTestTag),
                )
            },
        )
    }
}

/**
 * The frozen `.topbar`: loss-safe back · the `.pcount` ticket · the fold drawer's opener. Three things,
 * which is the whole change.
 *
 * **What P6 removed, and on whose authority.** It used to carry the zine's name and a live status line
 * ("Read · tap the edges to turn"). The frozen bar has no room for either — `.pcount` occupies the centre —
 * so shipping the ticket *was* the decision, and it was put to the owner rather than taken here
 * ([ADR-101 §5](../../../../../../docs/DECISIONS.md#adr-101-open)). Both go:
 *
 * - **The name** answers *which zine is this?*, and you arrive from the editor, where you have been
 *   looking at it. It is the Library's question, asked one screen too late.
 * - **The status line** taught the turn gesture, and P5 replaced that job with something better: chevrons
 *   that are visible where the hand already is and that *disappear* at the ends. What remains of its work
 *   — announcing the position to a screen reader — moves into the ticket, which is the frozen home for it.
 *
 * The ticket therefore carries the polite live region P5 had put on a caption under the book. **One
 * readout, not two**: both wired, TalkBack says the page twice on every turn.
 */
@Composable
private fun ProofTopBar(
    ticket: String?,
    onBack: () -> Unit,
    onOpenFold: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // `.topbar{padding:var(--gap-lg) var(--gap-md) var(--gap-sm)}`
            .padding(
                start = ZinelyV21Dimens.gapMd,
                end = ZinelyV21Dimens.gapMd,
                top = ZinelyV21Dimens.gapLg,
                bottom = ZinelyV21Dimens.gapSm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ProofIconButton(
            onClick = onBack,
            contentDescription = Copy.Proof.BACK_TO_BENCH_SAVED,
            path = ICON_BACK,
            modifier = Modifier.testTag(ProofBackTestTag),
        )

        // Absent while the export-error pane stands in for the reader: the ticket names a leaf, and at
        // that moment there is no leaf on screen to name. `Spacer` rather than a blank ticket, because an
        // empty stamped ticket is a thing that looks broken.
        // Weighted, and that is a fix rather than tidiness. A `Row` measures unweighted children in order
        // against what is left, so an over-wide middle child leaves the **third** measured at zero: at a
        // large font scale, or on a longer document ("PAGE 128 OF 256"), the fold-guide opener — one of
        // three controls on this bar — could be squeezed off the screen entirely. The frozen `.pcount`
        // carries `white-space:nowrap` for the same reason; `fill = false` keeps the ticket its own width
        // rather than stretching it to the gap.
        if (ticket != null) {
            ProofPageTicket(ticket, Modifier.weight(1f, fill = false))
        } else {
            Spacer(Modifier.width(1.dp))
        }

        ProofIconButton(
            onClick = onOpenFold,
            contentDescription = Copy.Proof.HOW_TO_FOLD,
            path = ICON_FOLD,
            modifier = Modifier.testTag(ProofFoldOpenTestTag),
        )
    }
}

/**
 * The frozen `.iconbtn`: a 44px outlined pill on paper, with the printed shadow under it.
 *
 * **Not [ZIconButton]**, for the reason the P3 review gave about `.dclose` — that component is the app's
 * generic borderless 44dp rounded square, and this is a bordered pill that casts a hard shadow. Drawing one
 * and calling it the other is the pixel-parity gap the handbook says to fix or accept explicitly.
 *
 * Drawn at 44dp inside a 48dp target, the same split `.dclose` uses: the frozen size and a reachable
 * control were never in tension, only conflated.
 */
@Composable
private fun ProofIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    path: String,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    val interactionSource = remember { MutableInteractionSource() }
    val pill = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .zinelyFocusRing(interactionSource, ZinelyV21Dimens.radiusPill)
                .size(44.dp)
                // `box-shadow:2px 2px 0 var(--ink-line)` — half the `--hard` the band's buttons take. A
                // top bar sits flatter than a control you press.
                .zinelyV21HardShadow(2.dp, colors.inkLine, pill)
                .clip(pill)
                .background(colors.paper)
                .border(1.5.dp, colors.ink, pill),
            contentAlignment = Alignment.Center,
        ) {
            // `.iconbtn svg{width:20px;height:20px}`.
            Box(Modifier.size(20.dp)) { ProofVectorIcon(path, colors.inkSoft) }
        }
    }
}

/**
 * `.pcount` — **the page counter is a stamped ticket, not a label**, and the frozen CSS says so in every
 * property it sets: a butter ground, a dashed hairline, a pill, and a degree and a half of rotation.
 *
 * It is also the surface's only live region. The words are [Copy.ProofRead.leafLabel]'s — "Cover · 1 of 8",
 * "Page 3 of 8" — and they are what TalkBack speaks; the uppercase is [ProofPageTicket]'s own, applied to
 * the drawing and not to the announcement, so a screen reader is never handed a shouted string to spell.
 *
 * The dashed border is drawn rather than declared: Compose's `border` takes no dash. The 3dp on, 3dp off is
 * the ratio a browser renders `1.5px dashed` at, transcribed rather than derived — CSS does not publish the
 * rule, so this is the one value on the ticket that is a measurement of the prototype and not a token.
 *
 * **The dash is `inkSoft`, and the frozen `--hair` is a deliberate departure with a number behind it.** On
 * this bar the ticket sits on `desk`, where its `butterTint` ground measures **1.01:1** in light and
 * **1.33:1** in dark — so the ground is, in practice, not visible, and the ticket is whatever its edge says
 * it is. `hair` on `desk` measures **1.35:1**: an edge you cannot see either, which would leave small
 * tracked capitals floating on the bar and none of the reason the freeze gives for making this a ticket at
 * all. `inkSoft` clears 3:1 comfortably on both grounds. The alternative — changing the *ground* — would
 * change the object; changing the edge only makes the drawn object legible.
 */
@Composable
private fun ProofPageTicket(ticket: String, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v21Colors
    val pill = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
    val edge = colors.inkSoft
    BasicText(
        text = ticket.uppercase(Locale.ROOT),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .testTag(ProofPageTicketTestTag)
            .graphicsLayer { rotationZ = TICKET_TILT_DEGREES }
            // **The ground takes the shape; there is no `clip`.** Two things were wrong with clipping it:
            // a `Stroke` is centred on the path it follows, so a `clip` to the same bounds cuts the outer
            // half and renders the 1.5dp border at half weight — and a `drawBehind` placed before the
            // background is painted over by it entirely. Shaped background first, dash second, and the
            // border lands on top of its own ground at full width.
            .background(colors.butterTint, pill)
            .drawBehind {
                val w = TicketBorder.toPx()
                drawRoundRect(
                    color = edge,
                    topLeft = Offset(w / 2f, w / 2f),
                    size = Size(size.width - w, size.height - w),
                    style = Stroke(
                        width = w,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(TicketDash.toPx(), TicketDash.toPx()),
                        ),
                    ),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            }
            .padding(horizontal = ZinelyV21Dimens.gapMd, vertical = ZinelyV21Dimens.gapXs)
            // Spoken as written, not as drawn. The live region is here because this is now the only place
            // the position is published; see [ProofTopBar].
            .semantics {
                contentDescription = ticket
                liveRegion = LiveRegionMode.Polite
            },
        style = TextStyle(
            color = colors.inkSoft,
            fontFamily = ZinelyTheme.typography.shell,
            // `.74rem` against the 16px root the prototype sets.
            fontSize = 11.8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.08.em,
            // `.pcount{font-variant-numeric:tabular-nums}` — without it the ticket changes width between
            // page 9 and page 10, and a stamped ticket that twitches as you read is a label again.
            fontFeatureSettings = "tnum",
        ),
    )
}

/** `.pcount{transform:rotate(-1.5deg)}` — stamped by hand, and hands are not square. */
private const val TICKET_TILT_DEGREES = -1.5f
private val TicketBorder = 1.5.dp
private val TicketDash = 3.dp

/**
 * The band beneath the reader — the frozen `.band`, complete as of ADR-101 **P2**.
 *
 * Three parts, and the third replaces the second: the `.ready` summary row that opens the print details,
 * the `.commit` pair that renders the PDF, and the `.done` block that says where it went. One persistent
 * strip belonging to the finished book, in place of a bottom bar that reconfigured itself four different
 * ways depending on which act you were standing in.
 *
 * ### Two deliberate departures from the frozen band
 *
 * **`.ready` survives the save.** The frozen rule is `.band.saved .ready{display:none}` — saving hides the
 * summary row along with the commit pair. That is backwards for this screen: the print recipe is *most*
 * needed after the PDF exists, when the user walks to a printer, and the frozen arrangement makes the
 * scale-and-orientation guidance unreachable at exactly the moment it pays for itself. So `.done` replaces
 * `.commit` only, and the route into the details stays open. Taken under the delegated authority for this
 * pass, priority 1–2 over 5, and it is the Proof's own question — *"how do I print it correctly?"* — that
 * decides it.
 *
 * **`.done` has one action, not two.** The frozen row is *"Fold it up"* beside *"Back to shelf"*, whose
 * flash text admits the shelf does not exist yet (*"the finished book lives in Read"*). Zinely is
 * single-project today, so that control could only be the top bar's own back wearing a name for a place
 * there is no route to. One exit, in the one place it has always been.
 *
 * @param pageCount how many pages the PDF will carry — the first phrase of `.ready`'s summary.
 * @param savedName the display name of the PDF in Downloads, or null before there is one. Non-null swaps
 *   `.commit` for `.done`.
 * @param folded the user has pressed the fold guide's finish button for this file — `.done` answers them
 *   instead of re-issuing the instruction (ADR-101 §6.9).
 * @param staleFrom the paper size a dropped `.done` had been rendered for, or null. Non-null raises
 *   [ProofPaperChangedNote] above the restored commit pair.
 * @param busyTarget which export is in flight, or null — both controls block, only that one says so.
 */
@Composable
private fun ProofBand(
    pageCount: Int,
    paper: PaperSize,
    savedName: String?,
    folded: Boolean,
    staleFrom: String?,
    busyTarget: ProofExportTarget?,
    onOpenDetails: () -> Unit,
    onSavePdf: () -> Unit,
    onShare: () -> Unit,
    onFoldItUp: () -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val card = RoundedCornerShape(ZinelyV21Dimens.radiusLg)
    val inkLine = colors.inkLine
    val hard = ZinelyV21Dimens.hardShadow
    Box(
        // `.band{margin:0 var(--gap-md) var(--gap-md)}` — a card resting on the desk, not a bar welded to
        // the bottom edge. Exactly the frozen margin and no more: an earlier version added `+ hard` "for
        // the shadow", which reserved room on the one edge the shadow does not use. It is drawn right and
        // **up**, over the stage, which is what the frozen `z-index:8` is for.
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ZinelyV21Dimens.gapMd)
            .padding(bottom = ZinelyV21Dimens.gapMd),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // `box-shadow:var(--hard) calc(var(--hard)*-1) 0 var(--ink-line)` — right and **up**, the
                // one object on the surface lit from below. [zinelyV21HardShadow] offsets both axes
                // positively by design, so this one is drawn here rather than bent into that modifier for
                // a single caller.
                .drawBehind {
                    val d = hard.toPx()
                    val r = ZinelyV21Dimens.radiusLg.toPx()
                    translate(left = d, top = -d) {
                        drawRoundRect(color = inkLine, cornerRadius = CornerRadius(r))
                    }
                }
                .clip(card)
                .background(colors.paper)
                .border(1.5.dp, colors.ink, card)
                .padding(
                    start = ZinelyV21Dimens.gapMd,
                    end = ZinelyV21Dimens.gapMd,
                    top = ZinelyV21Dimens.gapMd,
                    bottom = ZinelyV21Dimens.gapLg,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        ProofReadyRow(
            pageCount = pageCount,
            paper = paper,
            saved = savedName != null,
            onOpenDetails = onOpenDetails,
        )
        if (savedName == null) {
            if (staleFrom != null) ProofPaperChangedNote(from = staleFrom, to = paper.displayName)
            ProofCommitRow(busyTarget = busyTarget, onSavePdf = onSavePdf, onShare = onShare)
        } else {
            ProofDoneBlock(savedName = savedName, folded = folded, onFoldItUp = onFoldItUp)
        }
        }

        // `.band .tape` — a torn strip of tape holding the card to the desk, straddling its top edge. It is
        // the clearest statement of ADR-101 §3.2 on this surface: butter is **material**, never chrome and
        // never an action. Declared after the card and outside its clip, because tape sits on top of the
        // thing it holds and hangs over the edge.
        val torn = Color.White.copy(alpha = 0.5f)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -TapeOverhang)
                .graphicsLayer { rotationZ = TAPE_TILT_DEGREES }
                .size(width = 74.dp, height = 20.dp)
                .background(colors.butter.copy(alpha = 0.55f))
                // `border-left/right:1px dashed rgba(255,255,255,.5)` — the two torn ends, and they are
                // what make this read as tape rather than as a yellow rectangle. Left out of the first
                // build, which is why it looked like a sticker.
                .drawBehind {
                    val w = 1.dp.toPx()
                    val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))
                    drawLine(torn, Offset(w / 2f, 0f), Offset(w / 2f, size.height), w, pathEffect = dash)
                    drawLine(
                        torn,
                        Offset(size.width - w / 2f, 0f),
                        Offset(size.width - w / 2f, size.height),
                        w,
                        pathEffect = dash,
                    )
                },
        )
    }
}

/** `.band .tape{top:-11px}` — how far the strip hangs over the card's top edge. */
private val TapeOverhang = 11.dp

/** `.band .tape{transform:rotate(-2.5deg)}` — nobody applies tape square. */
private const val TAPE_TILT_DEGREES = -2.5f

/** `.ready .tick{transform:rotate(-6deg)}` and `.seal{transform:rotate(-8deg)}` — both stamped by hand. */
private const val TICK_TILT_DEGREES = -6f
private const val SEAL_TILT_DEGREES = -8f

/**
 * `.ready` — tick, two lines, chevron; **one** control, announced as one string.
 *
 * The frozen row carries `aria-label="Print details"`, which on Android would *replace* everything inside
 * it: a sighted user reads the page count, the paper and the privacy promise, and a TalkBack user gets
 * three words. So the label is the row's own content ([Copy.Proof.readyLabel]) and the destination rides
 * on the click action instead, which is where Android puts *"what happens if you activate this"*.
 *
 * The heading changes once something is [saved]. Keeping this row past the save is P2's departure from
 * the frozen band, and the first version of it kept the route while leaving the signpost alone: *"Ready
 * when you are"* sat directly above *"Saved to your phone"*, which means *ready for what, I just did it*.
 * A row that survives into a new moment has to be named for that moment.
 */
@Composable
private fun ProofReadyRow(
    pageCount: Int,
    paper: PaperSize,
    saved: Boolean,
    onOpenDetails: () -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val typography = ZinelyTheme.typography
    val haptics = ZinelyTheme.haptics
    val hair = colors.hair
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            // `.ready{border-bottom:1.5px dashed var(--hair)}` — the row is a lid over the details, so it
            // is separated by a tear line rather than by a rule.
            //
            // **Left of the padding, not right of it.** `drawBehind` draws in the coordinate space of the
            // node it sits on, and `Modifier.padding` shrinks that space for everything to its right — so
            // with the padding first, `size.height` is the *text's* bottom and the dashes run along the
            // summary line's descenders, which is exactly what the padding was added to stop.
            .drawBehind {
                drawLine(
                    color = hair,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(TicketDash.toPx(), TicketDash.toPx()),
                    ),
                )
            }
            // `.ready{padding:var(--gap-hair) var(--gap-hair) var(--gap-md)}`
            .padding(
                start = ZinelyV21Dimens.gapHair,
                end = ZinelyV21Dimens.gapHair,
                top = ZinelyV21Dimens.gapHair,
                bottom = ZinelyV21Dimens.gapMd,
            )
            .clickable(role = Role.Button, onClickLabel = Copy.Proof.PRINT_DETAILS) {
                haptics.perform(ZinelyHaptic.Tick)
                onOpenDetails()
            }
            .semantics(mergeDescendants = true) {
                contentDescription = Copy.Proof.readyLabel(pageCount, paper.displayName, saved)
            }
            .testTag(ProofReadyTestTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // `.ready .tick` — the leaf disc, stamped on at six degrees off square, with its own small hard
        // shadow. `onLeaf`, not white: the frozen file's own comment records that a hardcoded `#FFF6E8`
        // here never flipped with the theme and measured 2.86:1 in dark on a meaning-bearing mark.
        Box(
            modifier = Modifier
                .graphicsLayer { rotationZ = TICK_TILT_DEGREES }
                .size(30.dp)
                .zinelyV21HardShadow(2.dp, colors.inkLine, CircleShape)
                .clip(CircleShape)
                .background(colors.leaf),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(17.dp)) { ProofVectorIcon(ICON_CHECK, colors.onLeaf) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                text = if (saved) Copy.Proof.BEFORE_YOU_PRINT else Copy.Proof.READY_WHEN_YOU_ARE,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = colors.ink,
                    fontFamily = typography.voice,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            BasicText(
                text = Copy.Proof.readySummary(pageCount, paper.displayName),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = colors.inkSoft,
                    fontFamily = typography.shell,
                    fontSize = 11.5.sp,
                ),
            )
        }
        Box(Modifier.size(17.dp)) { ProofVectorIcon(ICON_CHEVRON, colors.inkSoft) }
    }
}

/**
 * The stale-file notice above `.commit` — nothing in the freeze, and the P3 design review's top finding.
 *
 * It exists because the correct behaviour was invisible: the paper is changed inside a `Dialog`-backed
 * drawer, so `.done` disappears behind a scrim and the user meets the *effect* without ever seeing the
 * *cause*. Read from their chair, "Saved to your phone" became "Save PDF" while they weren't looking, and
 * the reasonable conclusion is that the app threw their file away. The line names the change, names what
 * to do, and says the old file is still where it was put — the last clause being the one that does the
 * work, because it is the fear, and it is answerable with a true sentence.
 *
 * Polite live region, with the same caveat as [ProofDoneBlock]: this is a **new** node the frame it
 * appears, so whether TalkBack speaks it is a device Pass 1 gate, not a claim.
 */
@Composable
private fun ProofPaperChangedNote(from: String, to: String) {
    val colors = ZinelyTheme.v21Colors
    BasicText(
        text = Copy.Proof.paperChangedResave(newPaper = to, oldPaper = from),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(ProofPaperChangedTestTag),
        style = TextStyle(
            // `.stale{color:var(--jam-text)}` — jam as text, which is the token that measures AA on paper.
            color = colors.jamText,
            fontFamily = ZinelyTheme.typography.shell,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        ),
    )
}

/**
 * `.commit` — Save PDF and Share, the two honest export backends ([ADR-052] drops the frozen third,
 * *Print*: there is no OS path that can promise actual size).
 *
 * Both go non-interactive while an export runs, and **only the one that is running says so**. That state
 * has no drawing in the frozen prototype, which is a gap the prototype could not see: HTML's `doSave()` is
 * instant, and a real 8-page render is not. Without it the first double-tap fires two concurrent renders
 * at the same destination.
 *
 * ⚠ It was drawn wrong for exactly as long as it existed. [busyTarget] was a Boolean `exportBusy`, so both
 * buttons dimmed together and a user who tapped Share watched Save PDF react — reported as *"both buttons
 * become active"*, which is the honest reading of two controls answering one tap. The fix is not in this
 * row: the destination now travels in [ExportUiState] itself, and this row merely asks each button about
 * itself.
 *
 * **Disabling stays coupled, and that is deliberate.** The VM is single-flight — a tap arriving during a
 * render is dropped ([ExportViewModel.export]) — so leaving the other button live would make it a control
 * that visibly does nothing. Two different treatments carry the two different facts: the running button
 * keeps full opacity and swaps to a present participle; the blocked one dims. True concurrency is a
 * separate question (two ~33 MB sheet renders at once is the OOM the VM already special-cases) and is not
 * built on this report.
 *
 * The frozen `.btn-save` also carries a second line (*"8 pages · A4"*) inside the button. It is not built:
 * it repeats `.ready`'s summary two rows above.
 *
 * **P6 stopped drawing these with the V1 design system**, and the reason is the same one the
 * Library's re-skin arrived at — those are the V1 design system, painting `stamp` teal and `field` grey,
 * and no parameter on them expresses a V2.1 button. A leaf pill with an ink cut and a printed shadow is a
 * different object, not a recoloured one. The V2.1 surfaces draw their own controls until the last V1
 * surface moves and the design system can be swept in one piece.
 */
@Composable
private fun ProofCommitRow(
    busyTarget: ProofExportTarget?,
    onSavePdf: () -> Unit,
    onShare: () -> Unit,
) {
    val anyBusy = busyTarget != null
    val haptics = ZinelyTheme.haptics
    Row(
        // `.commit{margin-top:var(--gap-md)}` — and the band's own `spacedBy(12.dp)` already **is** that
        // 12dp. An extra `gapXs` here made it 16.
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
    ) {
        val savingNow = busyTarget == ProofExportTarget.SAVE
        val sharingNow = busyTarget == ProofExportTarget.SEND
        ProofBandButton(
            text = if (savingNow) Copy.Proof.SAVING else Copy.Proof.SAVE_PDF,
            onClick = { haptics.perform(ZinelyHaptic.Tick); onSavePdf() },
            hero = true,
            ring = true,
            enabled = !anyBusy,
            busy = savingNow,
            modifier = Modifier.weight(1f).testTag(ProofSavePdfTestTag),
            icon = { tint -> StrokedGlyph(ICON_SAVE, tint) },
        )
        ProofBandButton(
            text = if (sharingNow) Copy.Proof.PREPARING_SHARE else Copy.Proof.SHARE,
            onClick = { haptics.perform(ZinelyHaptic.Tick); onShare() },
            hero = false,
            enabled = !anyBusy,
            busy = sharingNow,
            modifier = Modifier.testTag(ProofShareTestTag),
            icon = { tint -> ShareGlyph(tint) },
        )
    }
}

/**
 * The frozen `.btn` — a pill with a 1.5dp ink cut and a printed shadow, in one of two fills.
 *
 * [hero] is `.btn-save`: the `leaf` fill, the one action colour, wearing the `--frame` ring in `butter`.
 * The ring is **misregistration made deliberate** — a second impression of the same shape, a hair off — and
 * it is `butter` rather than the frozen `butter-tint` because the tint measured 1.10:1 against the band's
 * paper and read as a bloom rather than a mark ([ADR-100 §4](../../../../../../docs/DECISIONS.md#adr-100-butter-tint)).
 *
 * Otherwise `.btn-share`: paper, `inkSoft`, no ring — the quieter of two real exports, not a lesser one.
 *
 * **The press is hand-chained rather than [zinelyV21Pressable]'d**, for the ring. CSS carries the ring in
 * the same `box-shadow` list as the hard shadow, so `transform` moves both; that modifier begins with the
 * travel offset, which would leave anything applied before it standing still while the button moved out
 * from under it. Ring inside the travel, and under the shadow, is the frozen stack exactly.
 */
@Composable
private fun ProofBandButton(
    text: String,
    onClick: () -> Unit,
    hero: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    // The button is the one currently exporting. Distinct from `!enabled`: both are non-interactive, and
    // they must not look alike, or the screen is back to answering one tap with two controls.
    busy: Boolean = false,
    // Only `.btn-save` wears the ring — `.foldit` is the same leaf pill without one. Two booleans rather
    // than one variant enum, because the corpus genuinely varies them independently.
    ring: Boolean = false,
    press: ZinelyV21Press = ZinelyV21Press.Hero,
    icon: (@Composable (tint: Color) -> Unit)? = null,
) {
    val colors = ZinelyTheme.v21Colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pill = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
    val content = if (hero) colors.onLeaf else colors.inkSoft
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            // The running control stays at full strength — it is the one thing on screen that IS
            // happening. Only a button blocked by someone else's render dims.
            .alpha(if (enabled || busy) 1f else 0.4f)
            .semantics {
                if (busy) stateDescription = Copy.Proof.EXPORT_WORKING
            }
            .offset { if (pressed) IntOffset(press.travel.roundToPx(), press.travel.roundToPx()) else IntOffset.Zero }
            .then(
                if (ring) Modifier.zinelyV21Frame(colors.butter, pill) else Modifier,
            )
            .zinelyV21HardShadow(if (pressed) press.pressed else press.rest, colors.inkLine, pill)
            .zinelyFocusRing(interactionSource, ZinelyV21Dimens.radiusPill)
            .clip(pill)
            .background(if (hero) colors.leaf else colors.paper)
            .border(1.5.dp, colors.ink, pill)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = if (hero) ZinelyV21Dimens.gapMd else ZinelyV21Dimens.gapLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Box(Modifier.size(18.dp)) { icon(content) }
        BasicText(
            text = text,
            maxLines = 1,
            style = TextStyle(
                color = content,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = if (hero) 15.4.sp else 14.sp,
                fontWeight = if (hero) FontWeight.Bold else FontWeight.SemiBold,
            ),
        )
    }
}

/**
 * `.done` — the seal, what happened, where it is, and the one thing left to do.
 *
 * The two lines are merged into one polite live region, the intent being a single spoken sentence when
 * the band changes.
 *
 * **That intent is unverified, and it is load-bearing, so it is a named device Pass 1 gate rather than a
 * claim.** Compose fires a live-region event when an *existing* semantics node's content changes; this
 * block is a **new** node the frame it appears, replacing [ProofCommitRow], so the property-change pass
 * may skip it and leave only a subtree content-changed event TalkBack does not read aloud. Robolectric
 * cannot assert an announcement, and P2 retired the snackbar that *was* the shipped announcement — so if
 * TalkBack stays silent here, the save became less accessible, not more, and the fix is an explicit
 * announcement rather than a live region.
 */
@Composable
private fun ProofDoneBlock(savedName: String, folded: Boolean, onFoldItUp: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val typography = ZinelyTheme.typography
    val haptics = ZinelyTheme.haptics
    Column(
        modifier = Modifier.fillMaxWidth().testTag(ProofDoneTestTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // `.seal` — sealing wax, which ADR-101 §3 keeps red because it is material, not signal. `jamText`
        // rather than `jam` for the wax itself, and `onJam` for the mark: the frozen file amended both
        // after a hardcoded cream measured 2.86:1 in dark on a 26px mark that carries the meaning.
        Box(
            modifier = Modifier
                .graphicsLayer { rotationZ = SEAL_TILT_DEGREES }
                .size(52.dp)
                .zinelyV21HardShadow(3.dp, colors.inkLine, CircleShape)
                .clip(CircleShape)
                .background(colors.jamText)
                .border(1.5.dp, colors.ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(26.dp)) { ProofVectorIcon(ICON_CHECK, colors.onJam) }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BasicText(
                text = if (folded) Copy.Proof.NICE_THATS_A_ZINE else Copy.Proof.SAVED_TO_YOUR_PHONE,
                style = TextStyle(
                    color = colors.ink,
                    fontFamily = typography.voice,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            BasicText(
                text = if (folded) {
                    Copy.Proof.foldedInDownloads(savedName)
                } else {
                    Copy.Proof.savedInDownloads(savedName)
                },
                style = TextStyle(
                    color = colors.inkSoft,
                    fontFamily = typography.shell,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }
        // Folded: the same destination, demoted to the print panel's quiet-link shape. Forgetting step 8
        // five seconds after declaring it done is ordinary, so the guide stays one tap away — but it is no
        // longer the loudest thing on a screen talking to someone who has already folded it.
        if (folded) {
            BasicText(
                text = Copy.Proof.HOW_TO_FOLD,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) {
                        haptics.perform(ZinelyHaptic.Tick)
                        onFoldItUp()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag(ProofFoldItUpTestTag),
                style = TextStyle(
                    // `.foldagain{color:var(--leaf-text);text-decoration:underline}` — the action colour
                    // as text, which is the token measured AA on paper.
                    color = colors.leafText,
                    fontFamily = typography.shell,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                ),
            )
        } else {
            ProofBandButton(
                text = Copy.Proof.FOLD_IT_UP,
                onClick = { haptics.perform(ZinelyHaptic.Tick); onFoldItUp() },
                // `.foldit{background:var(--leaf)}` — "Fold it up" is the next move, so it takes the one
                // action colour. It carries no `--frame` ring: only `.btn-save` does.
                hero = true,
                enabled = true,
                // `.foldit{box-shadow:3px 3px 0}` — a raised control, not the band's hero.
                press = ZinelyV21Press.Raised,
                modifier = Modifier.testTag(ProofFoldItUpTestTag),
                icon = { tint -> ProofVectorIcon(ICON_FOLD, tint) },
            )
        }
    }
}

/**
 * The print-details drawer body — [ProofPrintDetailsPanel] inside [drawerBodyMaxHeight].
 *
 * One composable and one scroll, which is the whole of what P3 changed here: P1 stacked two whole acts
 * at `weight(1f)` each, and P2 could only reorder them. The bound remains, because a drawer that wraps
 * its content will push the overflow off the bottom edge rather than scroll it.
 */
@Composable
private fun ProofDetailsDrawerBody(
    paper: PaperSize,
    pageCount: Int,
    onPaperSelected: (PaperSize) -> Unit,
    onOpenFold: () -> Unit,
) {
    ProofPrintDetailsPanel(
        paper = paper,
        onPaperSelected = onPaperSelected,
        onOpenFold = onOpenFold,
        pageCount = pageCount,
        modifier = Modifier.fillMaxWidth().heightIn(max = drawerBodyMaxHeight()),
    )
}

/**
 * Draws a `proof.html` 24×24 stroked SVG path at the caller's size, in [tint]. Round caps/joins,
 * 2.2px stroke on the 24-unit viewport — the frozen icon weight.
 *
 * `internal` rather than file-private since P5: the reader's two turn chevrons are the same frozen SVG
 * paths at the same weight, and a second renderer beside this one is a second stroke width waiting to
 * drift.
 */
@Composable
internal fun ProofVectorIcon(pathData: String, tint: Color) {
    val path = rememberPath(pathData)
    Canvas(Modifier.fillMaxSize()) {
        val s = size.minDimension / 24f
        scale(s, s, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(
                path = path,
                color = tint,
                style = Stroke(
                    width = 2.2f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                ),
            )
        }
    }
}

@Composable
private fun rememberPath(pathData: String) =
    remember(pathData) {
        PathParser().parsePathString(pathData).toPath()
    }

/** The frozen `#errwrap` badge — a warning triangle with an exclamation (stroke) and its dot (fill). */
@Composable
private fun ProofWarnBadge(tint: Color) {
    val triangle = rememberPath("M12 3l9 16H3l9-16z")
    val stem = rememberPath("M12 8v5")
    Canvas(Modifier.fillMaxSize()) {
        val s = size.minDimension / 24f
        scale(s, s, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            val stroke = Stroke(
                width = 2f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            )
            drawPath(triangle, tint, style = stroke)
            drawPath(stem, tint, style = stroke)
            drawCircle(tint, radius = 1.2f, center = androidx.compose.ui.geometry.Offset(12f, 16.5f))
        }
    }
}
