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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.semantics.semantics
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
 * Every adjustment control is painted from [abilities], so a verb that cannot change anything is visibly
 * and audibly unavailable rather than lit, tappable and inert (the fit segments, Reset, Cancel and Done
 * always work — they are the ways *out* of a state where nothing else can move).
 *
 * @param fit the current draft fit (drives the segmented selected-state).
 * @param zoomPercent the current zoom as a whole percent, for the stepper readout.
 * @param abilities which adjustments can currently change anything — see [ReframeAbilities].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun ReframeControls(
    fit: FrameFit,
    zoomPercent: Int,
    abilities: ReframeAbilities,
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
        ReframeStepperBar(
            zoomPercent = zoomPercent,
            abilities = abilities,
            onNudge = onNudge,
            onZoom = onZoom,
        )

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
private fun ReframeStepperBar(
    zoomPercent: Int,
    abilities: ReframeAbilities,
    onNudge: (Int, Int) -> Unit,
    onZoom: (Double) -> Unit,
) {
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
            NudgePad(abilities = abilities, onNudge = onNudge)
            ZoomStep(zoomPercent = zoomPercent, abilities = abilities, onZoom = onZoom)
        }
    }
}

/**
 * The cross-shaped 3×3 nudge pad (bench `.nudgepad`): Up / Left / Right / Down arranged on the cross, with
 * the corners left as inert spacers. 2D position is two axes of discrete targets — not one 1-D adjustable.
 */
@Composable
private fun NudgePad(abilities: ReframeAbilities, onNudge: (Int, Int) -> Unit) {
    // Per axis, not per arrow: pan room is symmetric about the centre, and the clamp that would stop a
    // rightward nudge is the same one that stops a leftward one.
    val h = abilities.panHorizontally
    val v = abilities.panVertically
    // No group-level semantics wrapper: each cell carries its own spoken label (a parent
    // clearAndSetSemantics would clear the children TalkBack + the a11y tests navigate to).
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            NudgeSpacer()
            NudgeCell(Icons.Filled.ArrowUpward, "Move photo up", v) { onNudge(0, -1) }
            NudgeSpacer()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            NudgeCell(Icons.Filled.ArrowBack, "Move photo left", h) { onNudge(-1, 0) }
            NudgeSpacer()
            NudgeCell(Icons.Filled.ArrowForward, "Move photo right", h) { onNudge(1, 0) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            NudgeSpacer()
            NudgeCell(Icons.Filled.ArrowDownward, "Move photo down", v) { onNudge(0, 1) }
            NudgeSpacer()
        }
    }
}

/** A 34dp cross cell (bench `.nudgepad button`): field fill, hairline edge, decorative glyph + spoken label. */
@Composable
private fun NudgeCell(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .testTag("reframe-$description")
            .size(34.dp)
            // Fade the WHOLE chip — edge, fill and glyph — as the Type bar's stepper does (bench
            // `:disabled{opacity:.4}`). Ahead of the paint modifiers so the layer wraps them all.
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(8.dp))
            .background(ZinelyTheme.colors.field)
            .border(1.dp, ZinelyTheme.colors.fieldEdge, RoundedCornerShape(8.dp))
            // `semantics`, NOT `clearAndSetSemantics`: the latter wipes the `disabled` flag that
            // `clickable(enabled = false)` sets, so an unavailable control would still announce itself as
            // actionable — a screen-reader user would be told to tap something that cannot respond.
            // Verified on a physical device: this reaches the platform as
            // `class=android.widget.Button, clickable=true, enabled=false`. The role rides the clickable
            // (see [ZoomButton] for what happens when it does not), and the glyph is an
            // `Icon(contentDescription = null)`, so nothing forces a second node.
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
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
private fun ZoomStep(zoomPercent: Int, abilities: ReframeAbilities, onZoom: (Double) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ZoomButton(Icons.Filled.Remove, "Zoom out", abilities.zoomOut) { onZoom(1.0 / Framing.ZOOM_STEP) }
        Text(
            text = "$zoomPercent%",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier
                .width(50.dp)
                .clearAndSetSemantics { contentDescription = "Zoom $zoomPercent percent" },
        )
        ZoomButton(Icons.Filled.Add, "Zoom in", abilities.zoomIn) { onZoom(Framing.ZOOM_STEP) }
    }
}

/**
 * A 40dp zoom step button (bench `.zoomstep button`): field fill, hairline edge, a +/− glyph.
 *
 * **The glyph is an [Icon], not a `Text`, and that is an accessibility decision rather than a visual one.**
 * A physical-device check of the platform tree found this control arriving as *three* nodes — the click
 * and its `disabled` flag on one, the spoken label on a second, the button role on a third — because a
 * `Text` child contributes semantics of its own and stops the chain collapsing. TalkBack lands on the
 * labelled node, which reports `enabled=true`, so a disabled zoom step announced itself as available: D3's
 * whole point, lost at the last hop. Marking the `Text` decorative was not enough; a cleared node is still
 * a node. [NudgeCell] never had the fault because `Icon(contentDescription = null)` contributes nothing,
 * so this now uses exactly that shape and collapses to one `android.widget.Button` carrying label, role
 * and disabled state together.
 *
 * The Compose test tree cannot see any of this — it reports the merged node, where everything resolves
 * correctly, which is why `assertIsNotEnabled` passed throughout against a control that was telling the
 * platform otherwise. The bench's `+`/`−` are rendered as the Material `Add`/`Remove` vectors at the same
 * weight; there is no committed golden for this bar, and post-freeze accessibility fixes are permitted.
 */
@Composable
private fun ZoomButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .testTag("reframe-$description")
            .size(40.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(11.dp))
            .background(ZinelyTheme.colors.field)
            .border(1.dp, ZinelyTheme.colors.fieldEdge, RoundedCornerShape(11.dp))
            // `role` on the clickable itself, and the glyph explicitly stripped of semantics.
            //
            // Both are load-bearing, and a physical-device check is what found it: with the role in a
            // trailing `semantics {}` block and a plain `Text` child, this button reached the platform as
            // `class=android.view.View, clickable=false, enabled=true` — no button role, no click action
            // and, fatally for D3, **no disabled state** — while the `−` glyph leaked as its own
            // traversable TextView. The Compose test tree showed none of that: it reports the merged node,
            // where the description and the disabled flag both resolve correctly, so `assertIsNotEnabled`
            // passed against a control that told TalkBack it was enabled.
            //
            // [NudgeCell] never had the fault because its `Icon(contentDescription = null)` contributes no
            // semantics at all, so nothing forced the extra node. The difference was the child, not the
            // modifier chain — which is why the two looked identical and behaved differently.
            // CI-93 INJECTED DEFECT (proof pair, reverted in the next commit): hard-wire the click enabled
            // so the platform tree reports enabled=true even when `abilities` say the stepper is disabled —
            // the exact f4faaa4 lie. The `.alpha(if (enabled)…)` above is untouched, so the golden is
            // unchanged and only the platform-tree assertion catches it.
            .clickable(enabled = true, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = ZinelyTheme.colors.onDesk, modifier = Modifier.size(20.dp))
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
