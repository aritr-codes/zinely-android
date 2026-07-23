package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.theme.ZinelyTheme

/** Test tag on the supply-tray container. */
public const val EditorSupplyTrayTestTag: String = "editor-supply-tray"

/** Per-supply test tags (also stable hooks for instrumentation). */
public const val SupplyAddPhotoTag: String = "supply-add-photo"
public const val SupplyAddWordsTag: String = "supply-add-words"
public const val SupplyUndoTag: String = "supply-undo"
public const val SupplyRedoTag: String = "supply-redo"

/** Spoken a11y labels = the visible supply labels (reuses the empty-state strings for the add actions). */
public const val UndoActionLabel: String = "Undo"
public const val RedoActionLabel: String = "Redo"

/** The shelf's section heading — orients screen-reader users before the four supplies (ADR-033 follow-up). */
public const val TraySectionLabel: String = "Supplies"

/**
 * The scrapbook **supply tray** (docs/design/editor-visual-direction.md §4 "tool tray"; mockup
 * docs/design/mockups/supply-tray.html) — the visible shelf of "supplies" that replaces the lone
 * floating "Add image" FAB. It surfaces the four primary editor actions as craft supplies sitting in
 * the thumb zone, so nothing essential hides behind a gesture or a menu (DESIGN-RULES 1–3, 7).
 *
 * The supplies are pure carriers of intents the host already owns: **Add a photo** →
 * `Intent.RequestAddImage` (same path as the old FAB), **Add words** → the empty-state add-text
 * behavior (`addTextAndEdit`), **Undo** → `Intent.Undo`, **Redo** → `Intent.Redo`. Undo/Redo are
 * enabled strictly by [canUndo]/[canRedo] (the history-free projection), so a disabled supply is
 * visibly inert rather than a dead tap (DESIGN-RULES 6, and the mockup's greyed Redo).
 *
 * Visual language: "Add a photo" is the coral **primary** supply; the rest are paper (`surface`).
 * A disabled supply fades to a flat, de-saturated card. Slight handmade tilts echo the page strip;
 * the tilt is decorative only. Accessibility: each supply is a `Role.Button` ≥48dp (here 64dp) with
 * the action text as both the visible label and the merged spoken label. A disabled supply is the
 * contract: it reports `enabled = false` to TalkBack and tests (`assertIsNotEnabled`) and `clickable`
 * suppresses its tap — but the OnClick semantics node may remain (marked disabled), so tests assert
 * the disabled/enabled *state*, not the absence of a click action.
 *
 * Orientation: the shelf is titled with a quiet "Supplies" [TraySectionLabel] `heading()`, so a screen
 * reader lands on a named section landmark before traversing the four actions (DESIGN-RULES 9; the
 * ADR-033 follow-up). It is visually low-emphasis so it never competes with the coral primary supply
 * (DESIGN-RULES 3), and adds a label only — no behavior — on every page.
 *
 * Stateless: every value is hoisted; the host reads [canUndo]/[canRedo] from the editor state and
 * dispatches on each callback. No motion beyond M3 ripple, so the reduced-motion path is a no-op.
 *
 * @param canUndo whether there is history to undo (drives the Undo supply's enabled state).
 * @param canRedo whether there is a redo entry (drives the Redo supply's enabled state).
 * @param onAddPhoto tapped "Add a photo" (host dispatches `Intent.RequestAddImage`).
 * @param onAddText tapped "Add words" (host runs the empty-state add-text behavior).
 * @param onUndo tapped "Undo" (host dispatches `Intent.Undo`); only invoked when [canUndo].
 * @param onRedo tapped "Redo" (host dispatches `Intent.Redo`); only invoked when [canRedo].
 * @param modifier sizing/placement applied by the host (full width, under the canvas).
 */
@Composable
public fun EditorSupplyTray(
    canUndo: Boolean,
    canRedo: Boolean,
    onAddPhoto: () -> Unit,
    onAddText: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .testTag(EditorSupplyTrayTestTag)
            // The shelf sits on the frozen "desk" surface (bench.html `--desk`), not the abused Legacy
            // `background` slate that never existed in the palette.
            .background(ZinelyTheme.colors.desk)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // A quiet section heading naming the shelf. Visually low-emphasis (it must not compete with the
        // coral primary supply — DESIGN-RULES 3); semantically a `heading()`, so a screen reader lands on
        // "Supplies" as orientation before traversing the four actions (DESIGN-RULES 9). Always present:
        // the shelf is a stable landmark on every page, and the heading adds no behavior, only a label.
        Text(
            text = TraySectionLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            // Quiet secondary label ON the desk band → `--on-desk-soft` (bench.html), not the paper-ink
            // `onSurfaceVariant` role it used before.
            color = ZinelyTheme.colors.onDeskSoft,
            modifier = Modifier.semantics { heading() },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            // Each supply takes an equal share of the row (weight) rather than a fixed min width, so the
            // four cards never overflow a narrow phone or clip at large font scales (Codex layout finding).
            SupplyButton(
                label = AddPhotoActionLabel,
                glyph = "🖼️",
                primary = true,
                enabled = true,
                tilt = -2f,
                testTag = SupplyAddPhotoTag,
                onClick = onAddPhoto,
                modifier = Modifier.weight(1f),
            )
            SupplyButton(
                label = AddWordsActionLabel,
                glyph = "✏️",
                primary = false,
                enabled = true,
                tilt = 1.5f,
                testTag = SupplyAddWordsTag,
                onClick = onAddText,
                modifier = Modifier.weight(1f),
            )
            SupplyButton(
                label = UndoActionLabel,
                glyph = "↶",
                primary = false,
                enabled = canUndo,
                tilt = -1f,
                testTag = SupplyUndoTag,
                onClick = onUndo,
                modifier = Modifier.weight(1f),
            )
            SupplyButton(
                label = RedoActionLabel,
                glyph = "↷",
                primary = false,
                enabled = canRedo,
                tilt = 1f,
                testTag = SupplyRedoTag,
                onClick = onRedo,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * One "supply" on the shelf — a tilted paper (or coral, when [primary]) card with a glyph over its
 * label. When [enabled] is false it fades to a flat, de-saturated card and `clickable(enabled = false)`
 * blocks the tap, so it reads (and tests) as disabled rather than firing as a silent no-op.
 */
@Composable
private fun SupplyButton(
    label: String,
    glyph: String,
    primary: Boolean,
    enabled: Boolean,
    tilt: Float,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.colors
    // Frozen supply vocabulary: the primary "Add a photo" is the coral FILL under white text
    // (`--coral-strong`, AA 4.6:1 — the palette's designated primary-button fill); the rest are paper
    // sheets (`--paper`); a disabled supply fades to a flat de-saturated paper-edge card.
    val container: Color = when {
        !enabled -> colors.paperEdge.copy(alpha = 0.4f)
        primary -> colors.coralStrong
        else -> colors.paper
    }
    val content: Color = when {
        !enabled -> colors.inkSoft.copy(alpha = 0.5f)
        primary -> Color.White
        else -> colors.ink
    }

    Column(
        modifier = modifier
            .graphicsLayer { rotationZ = tilt }
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag(testTag)
            // One clean merged label for TalkBack (the glyph is decorative). Role.Button comes from
            // clickable; the disabled flag it sets is preserved by this plain (non-clearing) semantics.
            .semantics(mergeDescendants = true) { contentDescription = label }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleLarge,
            color = content,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = content,
        )
    }
}
