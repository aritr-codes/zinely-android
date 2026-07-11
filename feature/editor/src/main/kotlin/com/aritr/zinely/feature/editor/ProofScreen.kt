package com.aritr.zinely.feature.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.components.ZIconButton
import com.aritr.zinely.ui.components.ZPrimaryButton
import com.aritr.zinely.ui.components.ZPrimaryButtonMetrics
import com.aritr.zinely.ui.components.ZPrimaryFill
import com.aritr.zinely.ui.components.ZToolButton
import com.aritr.zinely.ui.components.ZToolButtonMetrics
import com.aritr.zinely.ui.theme.ZinelyEasing
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme

// Test tags — the stable handles the ProofScreen suite and host tests address.
public const val ProofScreenTestTag: String = "proof-screen"
public const val ProofBackTestTag: String = "proof-back"
public const val ProofPrimaryTestTag: String = "proof-primary"
public const val ProofSecondaryTestTag: String = "proof-secondary"
public const val ProofActLabelTestTag: String = "proof-act-label"
public const val ProofProgressTestTag: String = "proof-progress"

/**
 * The three acts of the frozen Proof surface, in climb order (`proof.html` `setAct(0..2)`).
 * Sheet → Print → Fold: one surface, three internal acts, never three screens ([ADR-051]).
 */
public enum class ProofAct { SHEET, PRINT, FOLD }

// The topbar act-status captions — `proof.html` ACTLABELS, announced on every act change.
private val ACT_LABELS = mapOf(
    ProofAct.SHEET to "Step 1 of 3 · The sheet",
    ProofAct.PRINT to "Step 2 of 3 · Print",
    ProofAct.FOLD to "Step 3 of 3 · Fold",
)

// `proof.html` primary/back icon path data (24×24 viewport), consumed verbatim so the chrome
// mirrors the frozen SVGs rather than re-drawing them.
private const val ICON_BACK = "M15 5l-7 7 7 7"
private const val ICON_ARROW = "M5 12h13M13 6l6 6-6 6"
private const val ICON_FOLD = "M4 6h16v6l-8 6-8-6z"
private const val ICON_CHECK = "M5 12l5 5 9-11"
private const val ICON_PLUS = "M12 5v14M5 12h14"

/** The frozen climax beat schedule (`finishFold` setTimeouts): cumulative 980/1180/1440/1700/2000ms. */
private val CLIMAX_BEAT_DELAYS = intArrayOf(980, 200, 260, 260, 300)

/**
 * `.act.enter{ animation:actIn .34s var(--ease) }` — the frozen act-slide duration. Proof-specific
 * (neither `--fast` nor `--base`), so it is a named constant here, silenced under reduced motion.
 */
private const val PROOF_ACT_MILLIS: Int = 340

/**
 * The **B1** Proof scaffold (M5, [ADR-051]) — the single 3-act surface that collapses the former
 * Preview + Export + Completion routes into one screen (`proof.html`, DESIGN-FROZEN). This batch
 * stands up the frame only: the shared top bar (loss-safe back · zine name · three passive progress
 * creases), the act state machine (Sheet → Print → Fold with slide transitions, instant under reduced
 * motion), the shared bottom action bar reconfigured per act, and the topbar act-status live region.
 *
 * **Act bodies are intentionally empty here.** Act 1 (the imposed sheet), Act 2 (the print recipe +
 * export), and Act 3 (the fold guide + climax) land in B2/B3/B4; this scaffold defines the seam they
 * hang off. The primary/secondary configuration follows `proof.html` `configurePrimary()` exactly —
 * on the Fold act the primary is hidden because the (not-yet-built) step nav will own it.
 *
 * Stateless except for the act pointer: the act is [rememberSaveable] so a rotation keeps the climb
 * position. [onBack] is the loss-safe exit to the bench (the work is already autosaved).
 *
 * @param zineName the project title shown in the top bar.
 * @param onBack loss-safe back to the editor (the bench) — the work is saved.
 * @param paper the paper size shown in Act 2's recipe (the host threads it into the export).
 * @param onPaperSelected the user picked a paper size in Act 2's chooser.
 * @param onExportPdf Act 2 export — the host renders the PDF and hands it to the [ProofExportTarget] edge.
 * @param exportBusy an export is in flight — Act 2 disables its export row.
 * @param onMakeAnother Act 3 finished — the "Make another" exit (the single-project MVP returns to the bench).
 * @param modifier sizing/placement for the whole surface.
 */
@Composable
public fun ProofScreen(
    zineName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    paper: PaperSize = PaperSize.A4,
    onPaperSelected: (PaperSize) -> Unit = {},
    onExportPdf: (ProofExportTarget) -> Unit = {},
    exportBusy: Boolean = false,
    onMakeAnother: () -> Unit = {},
) {
    val colors = ZinelyTheme.colors
    val haptics = ZinelyTheme.haptics

    var actOrdinal by rememberSaveable { mutableStateOf(ProofAct.SHEET.ordinal) }
    val act = ProofAct.entries[actOrdinal]
    // The frozen slide is a fixed 16px nudge (translateX 16px), not a width fraction.
    val slidePx = with(LocalDensity.current) { 16.dp.roundToPx() }
    // Capture reduced-motion here: transitionSpec runs outside composition and can't read ZinelyTheme.
    val reduceMotion = ZinelyTheme.motion.reduceMotion

    // Act 3 (Fold) sub-state, hoisted so the shared action bar can own the finish button and the
    // finished-state exits (`configurePrimary`, RF-1). The Fold is reached once, forward-only from Print
    // (no in-act back to Print), so first entry starts fresh; rememberSaveable keeps the position — and,
    // once past the reveal, the finished book — across rotation.
    var foldStep by rememberSaveable { mutableStateOf(0) }
    var foldFinished by rememberSaveable { mutableStateOf(false) }
    // The climax reveal pointer, 0→5 (settle → shelf-line → words-h → words-p → exits). Driven by the
    // beat schedule below; saved so a rotation mid-reveal resumes rather than restarts the moment.
    var climaxBeat by rememberSaveable { mutableStateOf(0) }

    fun go(target: ProofAct) {
        haptics.perform(ZinelyHaptic.Tick)
        actOrdinal = target.ordinal
    }
    // The fold guide's step nav (also driven by ←/→). The next arrow past the last step is the finish.
    fun advanceFold() {
        if (foldStep < FOLD_LAST_STEP) {
            haptics.perform(ZinelyHaptic.Tick)
            foldStep += 1
        } else {
            foldFinished = true
        }
    }
    fun retreatFold() {
        if (foldStep > 0) {
            haptics.perform(ZinelyHaptic.Tick)
            foldStep -= 1
        }
    }

    // The climax, delivered in beats so the reveal lands — the book becomes a book BEFORE the words and
    // exits arrive (`finishFold`). The `success` verb fires once at the top (silenced under reduced
    // motion, where every beat is already at its final state); a `tick` marks the shelf-line beat.
    LaunchedEffect(foldFinished) {
        if (!foldFinished) return@LaunchedEffect
        if (reduceMotion) {
            climaxBeat = 5
            return@LaunchedEffect
        }
        if (climaxBeat == 0) haptics.perform(ZinelyHaptic.Success)
        while (climaxBeat < 5) {
            kotlinx.coroutines.delay(CLIMAX_BEAT_DELAYS[climaxBeat].toLong())
            climaxBeat += 1
            if (climaxBeat == 2) haptics.perform(ZinelyHaptic.Tick) // the shelf-line draws under the book
        }
    }

    // The topbar live caption switches to the payoff once the fold is finished (`actLabel` = "Done …").
    val actLabel = if (act == ProofAct.FOLD && foldFinished) {
        "Done · Your zine is ready"
    } else {
        ACT_LABELS.getValue(act)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.desk)
            // ←/→ drive the fold step nav while the guide is up (spec document keydown). Preview so the
            // arrows navigate steps rather than move focus between the step-nav buttons.
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && act == ProofAct.FOLD && !foldFinished) {
                    when (event.key) {
                        Key.DirectionRight -> { advanceFold(); true }
                        Key.DirectionLeft -> { retreatFold(); true }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .testTag(ProofScreenTestTag),
    ) {
        ProofTopBar(zineName = zineName, act = act, actLabel = actLabel, onBack = onBack)

        // The acts container. Forward moves slide in from the right (+16px), a back move from the
        // left (−16px) — `proof.html` actIn / actInBack — both fading in over --fast..ease. Bodies
        // are empty in B1 (B2–B4 fill them).
        AnimatedContent(
            targetState = act,
            transitionSpec = {
                val forward = targetState.ordinal >= initialState.ordinal
                val dur = if (reduceMotion) 0 else PROOF_ACT_MILLIS
                val dx = if (forward) slidePx else -slidePx
                (
                    fadeIn(tween(dur, easing = ZinelyEasing)) +
                        slideInHorizontally(tween(dur, easing = ZinelyEasing)) { dx }
                    ) togetherWith fadeOut(tween(dur, easing = ZinelyEasing))
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            label = "proofAct",
        ) { target ->
            when (target) {
                // Act 1 — the imposed sheet (B2).
                ProofAct.SHEET -> ProofSheetAct(Modifier.fillMaxSize())
                // Act 2 — the honest print recipe + export (B3).
                ProofAct.PRINT -> ProofPrintAct(
                    paper = paper,
                    onPaperSelected = onPaperSelected,
                    onExportPdf = onExportPdf,
                    exportBusy = exportBusy,
                    modifier = Modifier.fillMaxSize(),
                )
                // Act 3 — the fold guide + the staged climax (B4).
                ProofAct.FOLD -> ProofFoldAct(
                    step = foldStep,
                    finished = foldFinished,
                    climaxBeat = climaxBeat,
                    reduceMotion = reduceMotion,
                    onNext = { advanceFold() },
                    onPrev = { retreatFold() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        ProofActionBar(
            act = act,
            onPrimary = {
                when (act) {
                    ProofAct.SHEET -> go(ProofAct.PRINT)
                    ProofAct.PRINT -> go(ProofAct.FOLD)
                    ProofAct.FOLD -> Unit // the fold owns its own actions (finish / exits) below
                }
            },
            onSecondary = { if (act == ProofAct.PRINT) go(ProofAct.SHEET) },
            foldFinished = foldFinished,
            foldOnLastStep = foldStep == FOLD_LAST_STEP,
            foldActionsRevealed = climaxBeat >= 5,
            onFoldFinish = { foldFinished = true },
            onMakeAnother = { haptics.perform(ZinelyHaptic.Tick); onMakeAnother() },
            onBackToBench = { haptics.perform(ZinelyHaptic.Tick); onBack() },
        )
    }
}

/** Top bar: loss-safe back · zine name + act-status live region · three passive progress creases. */
@Composable
private fun ProofTopBar(
    zineName: String,
    act: ProofAct,
    actLabel: String,
    onBack: () -> Unit,
) {
    val colors = ZinelyTheme.colors
    val typography = ZinelyTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ZIconButton(
            onClick = onBack,
            // Loss-safe: the work is autosaved, so leaving is never destructive (`proof.html` #back).
            contentDescription = "Back to the bench (your work is saved)",
            modifier = Modifier.testTag(ProofBackTestTag),
        ) { tint -> ProofVectorIcon(ICON_BACK, tint) }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                text = zineName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = colors.onDesk,
                    fontFamily = typography.voice,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            BasicText(
                text = actLabel,
                modifier = Modifier
                    .testTag(ProofActLabelTestTag)
                    // role="status" — announce the act change to the screen reader.
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = TextStyle(
                    color = colors.onDeskSoft,
                    fontFamily = typography.shell,
                    fontSize = 11.sp,
                ),
            )
        }

        ProgressCreases(act)
    }
}

/**
 * The three progress creases — one per act, filled to the current act. Passive (`aria-hidden` in the
 * spec): decoration, never a tappable act-switcher. The frozen `#segAct` Sheet/Print/Fold buttons
 * live in the prototype dock (review scaffolding), not here.
 */
@Composable
private fun ProgressCreases(act: ProofAct) {
    val colors = ZinelyTheme.colors
    Row(
        modifier = Modifier
            .width(44.dp)
            .testTag(ProofProgressTestTag)
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProofAct.entries.forEach { crease ->
            val current = crease == act
            // opacity: .4 base, .55 reached (done), 1 current — `proof.html` .progress i states.
            val color = when {
                current -> colors.coralStrong
                crease.ordinal < act.ordinal -> colors.onDeskFaint.copy(alpha = 0.55f)
                else -> colors.onDeskFaint.copy(alpha = 0.4f)
            }
            // current crease scales to 1.1 (10px) — spec transform:scale(1.1) on the 8px dot.
            val side = if (current) 9.dp else 8.dp
            Box(
                Modifier
                    .size(side)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
    }
}

/**
 * The single bottom action bar, reconfigured per act — `proof.html` `configurePrimary()`:
 * - Sheet: primary "Print setup" (→ Print). No secondary.
 * - Print: primary "Now fold it" (→ Fold) + a "Back" secondary (→ Sheet).
 * - Fold, mid-steps: empty — the in-body step nav owns navigation (`primary.classList.toggle("hide")`).
 * - Fold, last step: the ONE stamp finish primary "It's folded — show me" (RF-1: never a dead primary).
 * - Fold, finished (after the reveal's `showActions` beat): "Back to bench" + a coral "Make another".
 */
@Composable
private fun ProofActionBar(
    act: ProofAct,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    foldFinished: Boolean,
    foldOnLastStep: Boolean,
    foldActionsRevealed: Boolean,
    onFoldFinish: () -> Unit,
    onMakeAnother: () -> Unit,
    onBackToBench: () -> Unit,
) {
    val colors = ZinelyTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.desk)
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (act) {
            ProofAct.SHEET -> ZPrimaryButton(
                text = "Print setup",
                onClick = onPrimary,
                metrics = ZPrimaryButtonMetrics.Proof,
                modifier = Modifier.weight(1f).testTag(ProofPrimaryTestTag),
                icon = { tint -> ProofVectorIcon(ICON_ARROW, tint) },
            )
            ProofAct.PRINT -> {
                ZToolButton(
                    onClick = onSecondary,
                    metrics = ZToolButtonMetrics.ProofGhost,
                    text = "Back",
                    modifier = Modifier.testTag(ProofSecondaryTestTag),
                )
                ZPrimaryButton(
                    text = "Now fold it",
                    onClick = onPrimary,
                    metrics = ZPrimaryButtonMetrics.Proof,
                    modifier = Modifier.weight(1f).testTag(ProofPrimaryTestTag),
                    icon = { tint -> ProofVectorIcon(ICON_FOLD, tint) },
                )
            }
            ProofAct.FOLD -> when {
                // The last step hands off to ONE prominent finish action (the `.primary.stamp`).
                !foldFinished && foldOnLastStep -> ZPrimaryButton(
                    text = "It’s folded — show me",
                    onClick = onFoldFinish,
                    metrics = ZPrimaryButtonMetrics.Proof,
                    fill = ZPrimaryFill.Stamp,
                    modifier = Modifier.weight(1f).testTag(ProofPrimaryTestTag),
                    icon = { tint -> ProofVectorIcon(ICON_CHECK, tint) },
                )
                // The exits arrive only after the reveal has fully landed (`showActions`, beat 5).
                foldFinished && foldActionsRevealed -> {
                    ZToolButton(
                        onClick = onBackToBench,
                        metrics = ZToolButtonMetrics.ProofGhost,
                        text = "Back to bench",
                        modifier = Modifier.testTag(ProofSecondaryTestTag),
                    )
                    ZPrimaryButton(
                        text = "Make another",
                        onClick = onMakeAnother,
                        metrics = ZPrimaryButtonMetrics.Proof,
                        modifier = Modifier.weight(1f).testTag(ProofPrimaryTestTag),
                        icon = { tint -> ProofVectorIcon(ICON_PLUS, tint) },
                    )
                }
                // Mid-steps (and during the climax beats): the global bar is empty. A spacer holds height.
                else -> Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * Draws a `proof.html` 24×24 stroked SVG path at the caller's size, in [tint]. Round caps/joins,
 * 2.2px stroke on the 24-unit viewport — the frozen icon weight.
 */
@Composable
private fun ProofVectorIcon(pathData: String, tint: Color) {
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
    androidx.compose.runtime.remember(pathData) {
        PathParser().parsePathString(pathData).toPath()
    }
