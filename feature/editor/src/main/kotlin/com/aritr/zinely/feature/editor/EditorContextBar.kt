package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.ReorderOp
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens

/** Test tag on the contextbar surface; absent from the tree when there's no selection. */
public const val EditorContextBarTestTag: String = "editor-context-bar"

/** `.ctx{gap:var(--gap-hair)}` (`v21-bench.html:224`) — 2dp, which V2 already spent here. */
private val CtxGap = ZinelyV21Dimens.gapHair

/** `.ctx{padding:var(--gap-xs)}` (`v21-bench.html:225`) — the tray's own inset, which V2 had none of. */
private val CtxPadding = ZinelyV21Dimens.gapXs

/** `.ctx button{border-radius:var(--br-pill);padding:var(--gap-sm) var(--gap-sm)}` (`:229`). */
private val CtxButtonShape: Shape = RoundedCornerShape(percent = 50)
private val CtxButtonPadding = ZinelyV21Dimens.gapSm

/** `.ctx svg{width:17px;height:17px}` (`v21-bench.html:233`) — down from the V2 build's 22. */
private val CtxGlyphSize = 17.dp

/**
 * The visible single-pointer transform controls (ADR-029 §6, WCAG 2.5.7) — the on-screen twin of the
 * gesture layer. Shown only when [selection] is non-empty; each control is a ≥48dp [IconButton] that
 * dispatches the **same** reducer intent the gesture commit and the per-element custom actions use
 * ([EditorA11y] step sizes), so the touch, a11y-action, and visible-button paths are one code path —
 * each press is one undo step.
 *
 * Nudge/scale/rotate act on the current selection (the reducer reads `model.selection`); reorder and
 * delete are id-scoped, so those are shown only for a **single** selected element. The bar pads the
 * navigation-bar inset for edge-to-edge (M3).
 *
 * ### ⚠ Unfrozen surface — two analogies, one per half
 *
 * The freeze draws a `.ctx` (`v21-bench.html:220-234`), but it is not this bar: `.ctx` is a *floating*
 * verb pill anchored under the selected element carrying `Edit · Font · Size · Ink · Delete`, and it ships
 * as [BenchContextBar]. **This** bar is the WCAG 2.5.7 twin of the gesture layer — nudge, scale, rotate,
 * order — which the prototype never drew because a prototype has no gestures to provide an alternative to.
 *
 * So each half takes the frozen surface it structurally matches:
 *
 * * **The band is `.bar`'s** (`:341` — `background:var(--desk)` and nothing else). Like `.bar` and unlike
 *   `.ctx`, this is a full-width control strip docked to the foot of the screen; the frozen file's answer
 *   for that shape is the desk, with no hairline, because the room and the band are the same ground and
 *   the controls' own ink draws the boundary. V2's `--chrome-line`-era band is gone with it.
 * * **The controls are `.ctx button`'s** (`:228-233`) — `inkSoft` glyphs on *no ground at all*, at pill
 *   radius, `butterTint` on touch, and `jamText` for the destructive one. They are the same verbs `.ctx`
 *   carries, and `.ctx`'s own comment states the contract they must keep: *"No offset shadow, no tilt, no
 *   tape: it is a tool, and tools do not perform."* That retires all three of V2's habits here — the
 *   38dp stamped `paper` chip, its deterministic handmade tilt, and its 22dp glyph.
 *
 * `butterTint` on press is `.ctx button:hover`, which is the only interaction state the selector declares;
 * hover is a pointer's press. It is legal butter — a material tint marking a touch, never a state on its
 * own and never an action's colour (§3.2).
 *
 * **⚠ Deviation: no visible labels.** `.ctx button` is a column of glyph over a one-word label at
 * `min-width:50px`. Its verbs are single words (`Edit`, `Ink`, `Delete`); these are phrases (*"Move
 * left"*, *"Rotate counterclockwise"*), and eleven of them would either wrap inside a 50dp column or
 * force a bar several screens wide. The controls stay glyph-only with the spoken label on the button, as
 * they have shipped since ADR-029 — a presentation this package is not chartered to re-decide. Flagged
 * for the owner.
 *
 * The control row **scrolls horizontally** (same pattern as [BenchPageNav]'s filmstrip): with up to eleven
 * ≥48dp controls the set overflows a narrow phone, so scrolling keeps every control reachable without
 * shrinking any target below 48dp. Scrolling changes layout only — no action, intent, gating, or semantic
 * label changes.
 *
 * **Style (FR-3, [ADR-055]).** A selected **text** box additionally gets a Style (`Aa`) control that
 * toggles the [TypeBar]. It is not a general control: it is `null` for a photo, for a multi-selection, and
 * for a still-blank box (which the reducer refuses to style anyway).
 *
 * @param selection the current selection; empty ⇒ nothing rendered.
 * @param dispatch forwards an [Intent] into the store.
 * @param modifier sizing/placement applied by the host.
 * @param onStyle toggles the Type bar for the selected text box. `null` (the default) omits the control
 *   entirely — the host passes it only for a single, non-blank text selection.
 * @param styleOpen whether the Type bar is currently showing, for the Style control's spoken state.
 * @param showDelete whether this bar presents Delete. **This is a presentation switch, not a capability
 *   one** ([D-039](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-039)): the host passes `false`
 *   only while the frozen `.ctx` bar is up and carrying Delete itself, so the action is never absent —
 *   only never offered twice at once. The transform verbs above are untouched in every case, which is
 *   what [OD-11](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-034-ruling) and
 *   [ADR-029](../../../../../../../../docs/DECISIONS.md#adr-029) §6 actually protect.
 */
// The directional/rotation glyphs use the non-AutoMirrored Filled icons on purpose: these controls are
// spatial (page-space "left" is screen-left in any layout direction), so RTL auto-mirroring would point
// the arrow the wrong way. The glyph is decorative regardless (cleared from the a11y tree), so the
// deprecation hint toward AutoMirrored does not apply here.
@Suppress("DEPRECATION")
@Composable
public fun EditorContextBar(
    selection: Set<String>,
    dispatch: (Intent) -> Unit,
    modifier: Modifier = Modifier,
    onStyle: (() -> Unit)? = null,
    styleOpen: Boolean = false,
    showDelete: Boolean = true,
) {
    if (selection.isEmpty()) return
    val singleId = selection.singleOrNull()

    Surface(
        modifier = modifier.testTag(EditorContextBarTestTag),
        // `.bar{background:var(--desk)}` — see the KDoc for why this half takes `.bar` and not `.ctx`.
        color = ZinelyTheme.v21Colors.desk,
        contentColor = ZinelyTheme.v21Colors.ink,
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(CtxPadding)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(CtxGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarButton(Icons.Filled.KeyboardArrowLeft, Copy.A11y.MOVE_LEFT) { dispatch(Intent.Nudge(PtPoint(-EditorA11y.NUDGE_STEP_PT, 0.0))) }
            BarButton(Icons.Filled.KeyboardArrowRight, Copy.A11y.MOVE_RIGHT) { dispatch(Intent.Nudge(PtPoint(EditorA11y.NUDGE_STEP_PT, 0.0))) }
            BarButton(Icons.Filled.KeyboardArrowUp, Copy.A11y.MOVE_UP) { dispatch(Intent.Nudge(PtPoint(0.0, -EditorA11y.NUDGE_STEP_PT))) }
            BarButton(Icons.Filled.KeyboardArrowDown, Copy.A11y.MOVE_DOWN) { dispatch(Intent.Nudge(PtPoint(0.0, EditorA11y.NUDGE_STEP_PT))) }
            BarButton(Icons.Filled.Add, Copy.A11y.MAKE_LARGER) { dispatch(Intent.ScaleBy(EditorA11y.SCALE_STEP_FACTOR)) }
            BarButton(Icons.Filled.Remove, Copy.A11y.MAKE_SMALLER) { dispatch(Intent.ScaleBy(1.0 / EditorA11y.SCALE_STEP_FACTOR)) }
            BarButton(Icons.Filled.RotateRight, Copy.A11y.ROTATE_CLOCKWISE) { dispatch(Intent.RotateBy(EditorA11y.ROTATE_STEP_DEGREES)) }
            BarButton(Icons.Filled.RotateLeft, Copy.A11y.ROTATE_COUNTERCLOCKWISE) { dispatch(Intent.RotateBy(-EditorA11y.ROTATE_STEP_DEGREES)) }
            if (singleId != null) {
                BarButton(Icons.Filled.FlipToFront, Copy.A11y.BRING_FORWARD) { dispatch(Intent.Reorder(singleId, ReorderOp.BRING_FORWARD)) }
                BarButton(Icons.Filled.FlipToBack, Copy.A11y.SEND_BACKWARD) { dispatch(Intent.Reorder(singleId, ReorderOp.SEND_BACKWARD)) }
            }
            if (onStyle != null) {
                // A disclosure, not a plain action: it says whether the Type bar is showing, so a
                // screen-reader user knows what the tap did without hunting for the bar.
                BarButton(
                    icon = Icons.Filled.TextFormat,
                    description = Copy.Editor.TEXT_STYLE,
                    state = if (styleOpen) Copy.Editor.SHOWING else Copy.Editor.HIDDEN,
                    onClick = onStyle,
                )
            }
            if (showDelete) {
                // `.ctx button.danger{color:var(--jam-text)}` — the corpus's one destructive mark, and
                // `jamText` rather than `jam` because this is jam **as text** (§4.1 row 2).
                BarButton(Icons.Filled.Delete, Copy.A11y.DELETE, danger = true) { dispatch(Intent.Delete(selection)) }
            }
        }
    }
}

/**
 * One ≥48dp control on `.ctx button`'s terms: a 17dp glyph in `inkSoft` — or `jamText` when [danger] —
 * over no ground, inside a pill that tints `butterTint` under the finger.
 *
 * The tint sits *inside* the [IconButton], so the touch target stays the standard axis-aligned 48dp box.
 * `indication = null` on the button is not available here (the M3 [IconButton] owns its own), so the
 * frozen tint is drawn from the interaction source instead and the ripple is left to the platform.
 *
 * @param state an optional spoken state for a control that has one (a disclosure's Showing/Hidden).
 *   `null` — every transform control — leaves the semantics exactly as they were.
 */
@Composable
private fun BarButton(
    icon: ImageVector,
    description: String,
    state: String? = null,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // The transform row is part of the Bench and answers the hand on the Bench's terms
    // ([benchTap]): Delete is the one control here that pressing again does not undo.
    val fire = benchTap(if (danger) ZinelyHaptic.Boundary else ZinelyHaptic.Tick, onClick)
    IconButton(
        onClick = fire,
        interactionSource = interaction,
        modifier = Modifier
            .testTag("$EditorContextBarTestTag-$description")
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                if (state != null) stateDescription = state
            },
    ) {
        Box(
            modifier = Modifier
                .clip(CtxButtonShape)
                // `.ctx button{background:none}` at rest; `:hover{background:var(--butter-tint)}` on touch.
                .background(if (pressed) colors.butterTint else Color.Transparent)
                .padding(CtxButtonPadding),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (danger) colors.jamText else colors.inkSoft,
                modifier = Modifier.size(CtxGlyphSize),
            )
        }
    }
}
