package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.theme.ZinelyTheme

/** Test tag on the Reframe controls band (present only during a Reframe session). */
public const val ReframeControlsTestTag: String = "reframe-controls"

/** Test tag on the "Reframe" affordance chip shown on a selected photo. */
public const val ReframeChipTestTag: String = "reframe-chip"

/**
 * The Reframe-mode chrome (ADR-053, frozen bench.html `reframe-tools` + `reframebar`): the fit segmented
 * control (Fill / Whole photo), an in-session Reset, the discrete **authoritative** move pad + zoom
 * steppers (the a11y path — gestures are an enhancement, RF1), and Cancel / Done. It swaps in over the
 * supply tray + context bar while a session is open (bench `toolbar[data-mode="reframe"]`).
 *
 * Every control drives the ephemeral [FramingDraft] through the host (never the reducer) except Cancel /
 * Done, which end the session ([Intent.CancelReframe] / [Intent.CommitReframe]). Reset is the *in-session*
 * draft reset to the centred-Fill baseline — distinct from the one-shot [Intent.ResetFraming] menu action.
 *
 * @param fit the current draft fit (drives the segmented selected-state).
 * @param zoomPercent the current zoom as a whole percent, for the stepper readout.
 */
@Composable
public fun ReframeControls(
    fit: FrameFit,
    zoomPercent: Int,
    onFit: (FrameFit) -> Unit,
    onNudge: (dx: Int, dy: Int) -> Unit,
    onZoom: (factor: Double) -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag(ReframeControlsTestTag),
        color = ZinelyTheme.colors.desk,
        contentColor = ZinelyTheme.colors.onDesk,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Move pad + zoom steppers — the authoritative accessible motion path (RF1).
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepButton(Icons.Filled.KeyboardArrowLeft, "Move photo left") { onNudge(-1, 0) }
                StepButton(Icons.Filled.KeyboardArrowUp, "Move photo up") { onNudge(0, -1) }
                StepButton(Icons.Filled.KeyboardArrowDown, "Move photo down") { onNudge(0, 1) }
                StepButton(Icons.Filled.KeyboardArrowRight, "Move photo right") { onNudge(1, 0) }
                StepButton(Icons.Filled.Remove, "Zoom out") { onZoom(1.0 / Framing.ZOOM_STEP) }
                Text(
                    text = "$zoomPercent%",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .width(52.dp)
                        .clearAndSetSemantics { contentDescription = "Zoom $zoomPercent percent" },
                )
                StepButton(Icons.Filled.Add, "Zoom in") { onZoom(Framing.ZOOM_STEP) }
            }
            // Fit segmented control + Reset + Cancel / Done.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FitSegment(fit = fit, onFit = onFit)
                StepButton(Icons.Filled.Refresh, "Reset framing") { onReset() }
                StepButton(Icons.Filled.Close, "Cancel reframing") { onCancel() }
                StepButton(Icons.Filled.Check, "Done reframing") { onDone() }
            }
        }
    }
}

/** Fill / Whole-photo segmented control (bench `.fitseg`); the selected segment carries `selected` semantics. */
@Composable
private fun FitSegment(fit: FrameFit, onFit: (FrameFit) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .border(1.dp, ZinelyTheme.colors.fieldEdge, RoundedCornerShape(13.dp)),
    ) {
        FitOption("Fill", FrameFit.FILL, fit, onFit)
        FitOption("Whole photo", FrameFit.WHOLE, fit, onFit)
    }
}

@Composable
private fun FitOption(label: String, value: FrameFit, current: FrameFit, onFit: (FrameFit) -> Unit) {
    val isSel = value == current
    Box(
        modifier = Modifier
            .testTag("reframe-fit-$label")
            .background(if (isSel) ZinelyTheme.colors.coral else ZinelyTheme.colors.field)
            .clickable { onFit(value) }
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                selected = isSel
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isSel) ZinelyTheme.colors.paper else ZinelyTheme.colors.onDesk,
            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** A ≥48dp icon control with a spoken label; the glyph is decorative (cleared from the a11y tree). */
@Composable
private fun StepButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .testTag("reframe-$description")
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            },
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ZinelyTheme.colors.paper),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = ZinelyTheme.colors.ink, modifier = Modifier.size(22.dp))
        }
    }
}

/**
 * The persistent "Reframe" affordance chip (ADR-053 RF2, bench `.reframe-aff`): a selected photo always
 * advertises that it can be reframed, so the pan/zoom gesture is discoverable without a missing handle.
 * Tapping it enters Reframe ([Intent.BeginReframe]).
 *
 * @param teach the first-run coach-mark (bench `.reframe-aff.teach`): pulse twice to draw the eye the first
 *   time a photo is selected on this install. The caller passes `false` under reduced motion (WCAG 2.3.3)
 *   and once the coach has been seen — so the pulse is opt-in and never reaches an animation-averse user.
 */
@Composable
public fun ReframeAffordanceChip(onClick: () -> Unit, modifier: Modifier = Modifier, teach: Boolean = false) {
    // Two gentle scale pulses (bench affPulse ×2), then rest. Finite — not an infinite transition — so it
    // teaches once and stops; `teach` is already reduced-motion-gated by the caller, so no motion here at all
    // when animations are off.
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(teach) {
        if (teach) repeat(2) { pulse.animateTo(1.08f, tween(300)); pulse.animateTo(1f, tween(600)) }
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
            .testTag(ReframeChipTestTag)
            .clearAndSetSemantics {
                contentDescription = "Reframe this photo"
                role = Role.Button
            },
        shape = RoundedCornerShape(50),
        color = ZinelyTheme.colors.ink,
        contentColor = ZinelyTheme.colors.paper,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Reframe", fontWeight = FontWeight.SemiBold)
        }
    }
}
