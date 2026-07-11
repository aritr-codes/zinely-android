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
) {
    val colors = ZinelyTheme.colors
    val haptics = ZinelyTheme.haptics

    var actOrdinal by rememberSaveable { mutableStateOf(ProofAct.SHEET.ordinal) }
    val act = ProofAct.entries[actOrdinal]
    // The frozen slide is a fixed 16px nudge (translateX 16px), not a width fraction.
    val slidePx = with(LocalDensity.current) { 16.dp.roundToPx() }
    // Capture reduced-motion here: transitionSpec runs outside composition and can't read ZinelyTheme.
    val reduceMotion = ZinelyTheme.motion.reduceMotion

    fun go(target: ProofAct) {
        haptics.perform(ZinelyHaptic.Tick)
        actOrdinal = target.ordinal
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.desk)
            .testTag(ProofScreenTestTag),
    ) {
        ProofTopBar(zineName = zineName, act = act, onBack = onBack)

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
                // ponytail: Fold (B4) body still empty; the Box holds the frame's shape.
                ProofAct.FOLD -> Box(Modifier.fillMaxSize())
            }
        }

        ProofActionBar(
            act = act,
            onPrimary = {
                when (act) {
                    ProofAct.SHEET -> go(ProofAct.PRINT)
                    ProofAct.PRINT -> go(ProofAct.FOLD)
                    ProofAct.FOLD -> Unit // the step nav owns the primary here (B4)
                }
            },
            onSecondary = { if (act == ProofAct.PRINT) go(ProofAct.SHEET) },
        )
    }
}

/** Top bar: loss-safe back · zine name + act-status live region · three passive progress creases. */
@Composable
private fun ProofTopBar(
    zineName: String,
    act: ProofAct,
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
                text = ACT_LABELS.getValue(act),
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
 * - Fold: primary hidden — the fold step nav owns it (B4). No global secondary until finished.
 */
@Composable
private fun ProofActionBar(
    act: ProofAct,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
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
        if (act == ProofAct.PRINT) {
            ZToolButton(
                onClick = onSecondary,
                metrics = ZToolButtonMetrics.ProofGhost,
                text = "Back",
                modifier = Modifier.testTag(ProofSecondaryTestTag),
            )
        }
        when (act) {
            ProofAct.SHEET -> ZPrimaryButton(
                text = "Print setup",
                onClick = onPrimary,
                metrics = ZPrimaryButtonMetrics.Proof,
                modifier = Modifier.weight(1f).testTag(ProofPrimaryTestTag),
                icon = { tint -> ProofVectorIcon(ICON_ARROW, tint) },
            )
            ProofAct.PRINT -> ZPrimaryButton(
                text = "Now fold it",
                onClick = onPrimary,
                metrics = ZPrimaryButtonMetrics.Proof,
                modifier = Modifier.weight(1f).testTag(ProofPrimaryTestTag),
                icon = { tint -> ProofVectorIcon(ICON_FOLD, tint) },
            )
            // Fold: the global primary is hidden (spec `primary.classList.toggle("hide", act===2)`);
            // the step nav supplies the finish action in B4. A spacer keeps the bar from collapsing.
            ProofAct.FOLD -> Spacer(Modifier.weight(1f))
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
