package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.theme.ZinelyTheme

/** Test tag on the Reframe controls band (present only during a Reframe session). */
public const val ReframeControlsTestTag: String = "reframe-controls"

/** Test tag on the "Reframe" affordance chip shown on a selected photo. */
public const val ReframeChipTestTag: String = "reframe-chip"

/**
 * The Reframe-mode chrome (ADR-053, frozen bench.html) — restored as the frozen **two surfaces**:
 *
 * 1. A floating `--menu` stepper pill (bench `.reframebar`): the discrete **authoritative** cross-shaped
 *    2D nudge pad + zoom steppers — the a11y path; gestures are an enhancement (RF1).
 * 2. The bottom `--desk` toolbar (bench `toolbar[data-mode="reframe"]` + `#rfCancel` + `#rfDone`): the fit
 *    segmented control (Fill / Whole photo, with plain-language sublabels), an in-session Reset, the full
 *    **Cancel** text action, and the primary coral **Done** (`.proof`).
 *
 * It swaps in over the supply tray + context bar while a session is open (bench `toolbar[data-mode="reframe"]`).
 *
 * Every control drives the ephemeral [FramingDraft] through the host (never the reducer) except Cancel /
 * Done, which end the session ([Intent.CancelReframe] / [Intent.CommitReframe]). Reset is the *in-session*
 * draft reset to the centred-Fill baseline — distinct from the one-shot [Intent.ResetFraming] menu action.
 *
 * @param fit the current draft fit (drives the segmented selected-state).
 * @param zoomPercent the current zoom as a whole percent, for the stepper readout.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    Column(
        modifier = modifier
            .testTag(ReframeControlsTestTag)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Surface 1 — the floating stepper pill (bench `.reframebar`): cross nudge pad + zoom steppers.
        ReframeStepperBar(zoomPercent = zoomPercent, onNudge = onNudge, onZoom = onZoom)

        // Surface 2 — the bottom desk toolbar: fit segmented control + reset · Cancel · Done. A FlowRow so
        // the frozen single bar holds on a real phone but wraps (never crushes an off-screen action) on a
        // narrow width — bench parity where there's room, graceful degradation where there isn't.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ZinelyTheme.colors.desk,
            contentColor = ZinelyTheme.colors.onDesk,
        ) {
            FlowRow(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                FitSegment(fit = fit, onFit = onFit)
                ToolIconButton(Icons.Filled.Refresh, "Reset framing") { onReset() }
                CancelButton(onCancel)
                DoneButton(onDone)
            }
        }
    }
}

/**
 * The floating stepper pill (bench `.reframebar`, `--menu` rounded card with a hairline + lift): the
 * authoritative accessible motion path — a cross-shaped 2D nudge pad and a zoom stepper.
 */
@Composable
private fun ReframeStepperBar(zoomPercent: Int, onNudge: (Int, Int) -> Unit, onZoom: (Double) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ZinelyTheme.colors.menu,
        contentColor = ZinelyTheme.colors.onDesk,
        border = BorderStroke(1.dp, ZinelyTheme.colors.fieldEdge),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NudgePad(onNudge)
            ZoomStep(zoomPercent = zoomPercent, onZoom = onZoom)
        }
    }
}

/**
 * The cross-shaped 3×3 nudge pad (bench `.nudgepad`): Up / Left / Right / Down arranged on the cross, with
 * the corners left as inert spacers. 2D position is two axes of discrete targets — not one 1-D adjustable.
 */
@Composable
private fun NudgePad(onNudge: (Int, Int) -> Unit) {
    // No group-level semantics wrapper: each cell carries its own spoken label (a parent
    // clearAndSetSemantics would clear the children TalkBack + the a11y tests navigate to).
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            NudgeSpacer()
            NudgeCell(Icons.Filled.ArrowUpward, "Move photo up") { onNudge(0, -1) }
            NudgeSpacer()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            NudgeCell(Icons.Filled.ArrowBack, "Move photo left") { onNudge(-1, 0) }
            NudgeSpacer()
            NudgeCell(Icons.Filled.ArrowForward, "Move photo right") { onNudge(1, 0) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            NudgeSpacer()
            NudgeCell(Icons.Filled.ArrowDownward, "Move photo down") { onNudge(0, 1) }
            NudgeSpacer()
        }
    }
}

/** A 34dp cross cell (bench `.nudgepad button`): field fill, hairline edge, decorative glyph + spoken label. */
@Composable
private fun NudgeCell(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .testTag("reframe-$description")
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ZinelyTheme.colors.field)
            .border(1.dp, ZinelyTheme.colors.fieldEdge, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = ZinelyTheme.colors.onDesk, modifier = Modifier.size(16.dp))
    }
}

/** An inert corner of the cross (bench `.nudgepad .spacer`): keeps the grid square, takes no input. */
@Composable
private fun NudgeSpacer() {
    Spacer(Modifier.size(34.dp))
}

/** The zoom stepper (bench `.zoomstep`): − · readout · + . */
@Composable
private fun ZoomStep(zoomPercent: Int, onZoom: (Double) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ZoomButton("−", "Zoom out") { onZoom(1.0 / Framing.ZOOM_STEP) }
        Text(
            text = "$zoomPercent%",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier
                .width(50.dp)
                .clearAndSetSemantics { contentDescription = "Zoom $zoomPercent percent" },
        )
        ZoomButton("+", "Zoom in") { onZoom(Framing.ZOOM_STEP) }
    }
}

/** A 40dp zoom step button (bench `.zoomstep button`): field fill, hairline edge, a plain +/− glyph. */
@Composable
private fun ZoomButton(glyph: String, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .testTag("reframe-$description")
            .size(40.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(ZinelyTheme.colors.field)
            .border(1.dp, ZinelyTheme.colors.fieldEdge, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 20.sp, color = ZinelyTheme.colors.onDesk)
    }
}

/** Fill / Whole-photo segmented control (bench `.fitseg`); the selected segment carries `selected` semantics. */
@Composable
private fun FitSegment(fit: FrameFit, onFit: (FrameFit) -> Unit) {
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(13.dp))
            .border(1.dp, ZinelyTheme.colors.fieldEdge, RoundedCornerShape(13.dp)),
    ) {
        FitOption("Fill", "crops edges", FrameFit.FILL, fit, onFit)
        // The inter-segment hairline (bench `.fitseg button+button{border-left:…}`).
        Box(Modifier.width(1.dp).fillMaxHeight().background(ZinelyTheme.colors.fieldEdge))
        FitOption("Whole photo", "may add margins", FrameFit.WHOLE, fit, onFit)
    }
}

/**
 * A fit segment (bench `.fitseg button`): a plain-language primary label over a small helper sublabel
 * (`<small>`), so the choice is legible without jargon. The spoken label stays just the primary label.
 */
@Composable
private fun FitOption(label: String, sublabel: String, value: FrameFit, current: FrameFit, onFit: (FrameFit) -> Unit) {
    val isSel = value == current
    Box(
        modifier = Modifier
            .testTag("reframe-fit-$label")
            .background(if (isSel) ZinelyTheme.colors.coralStrong else ZinelyTheme.colors.field)
            .clickable { onFit(value) }
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                selected = isSel
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = if (isSel) Color.White else ZinelyTheme.colors.onDesk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = sublabel,
                color = if (isSel) Color.White.copy(alpha = 0.85f) else ZinelyTheme.colors.onDeskSoft,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** A 48dp icon tool (bench `.tool.icononly`): field fill, hairline edge, decorative glyph + spoken label. */
@Composable
private fun ToolIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .testTag("reframe-$description")
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ZinelyTheme.colors.field)
            .border(1.dp, ZinelyTheme.colors.fieldEdge, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = ZinelyTheme.colors.onDesk, modifier = Modifier.size(18.dp))
    }
}

/** The full-text Cancel action (bench `#rfCancel` `.tool`): discards the session ([Intent.CancelReframe]). */
@Composable
private fun CancelButton(onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ZinelyTheme.colors.field)
            .border(1.dp, ZinelyTheme.colors.fieldEdge, RoundedCornerShape(14.dp))
            .clickable(onClick = onCancel)
            .clearAndSetSemantics {
                contentDescription = "Cancel reframing"
                role = Role.Button
            }
            .padding(horizontal = 15.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ZinelyTheme.colors.onDesk)
    }
}

/**
 * The primary coral Done action (bench `#rfDone` `.proof`): a coral-strong pill with white "Done" + a check,
 * the one live coral action while reframing. Commits the session ([Intent.CommitReframe]).
 */
@Composable
private fun DoneButton(onDone: () -> Unit) {
    // A plain clickable Row (not Surface(onClick)) so the clickable sits OUTSIDE clearAndSetSemantics — a
    // Surface's onClick lives inside the semantics-clearing boundary and its click never fires under test.
    Row(
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(ZinelyTheme.colors.coralStrong)
            .clickable(onClick = onDone)
            .clearAndSetSemantics {
                contentDescription = "Done reframing"
                role = Role.Button
            }
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Done", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
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
        // Click via Modifier.clickable (OUTSIDE clearAndSetSemantics), not Surface(onClick) — the latter
        // buries the click action inside the cleared-semantics boundary, so a test/AT click never lands.
        modifier = modifier
            .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
            .testTag(ReframeChipTestTag)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = "Reframe this photo"
                role = Role.Button
            },
        // bench `.reframe-aff`: ink at 62% over the photo, paper text, tight 11dp corners.
        shape = RoundedCornerShape(11.dp),
        color = ZinelyTheme.colors.ink.copy(alpha = 0.62f),
        contentColor = ZinelyTheme.colors.paper,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(Icons.Filled.CropFree, contentDescription = null, modifier = Modifier.size(13.dp))
            Text("Reframe", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
