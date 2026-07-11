package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.theme.ZinelyTheme

/** Test tag on the move/resize hint container. */
public const val EditorMoveResizeHintTestTag: String = "editor-move-resize-hint"

/** Test tag (and stable instrumentation hook) on the hint's dismiss control. */
public const val MoveResizeHintDismissTag: String = "move-resize-hint-dismiss"

/** The teaching line (canonical, docs/design/VOICE.md §3 "Hints"). */
public const val MoveResizeHintText: String = "Drag to move it. Pinch to resize."

/** The dismiss affordance label (the implicit/explicit "got it" VOICE requires for a one-time hint). */
public const val MoveResizeHintDismissLabel: String = "Got it"

/**
 * The one-time **move/resize hint** (docs/design/VOICE.md §3 "Hints"; editor-visual-direction.md §6
 * slice 5). When a beginner first selects a placed element the resize handles appear, but the two core
 * manipulations — *drag to move*, *pinch to resize* — are gestures with no discrete-control twin, so a
 * first-timer can miss them. This is the gentle, scrapbook-styled note that teaches exactly those two
 * moves, then gets out of the way.
 *
 * Deliberately **not** a generic tooltip: a tilted paper sticky (the workbench feel), one warm line,
 * and an explicit "Got it". It is non-modal and non-blocking — the card declares no `pointerInput`, so
 * touches fall straight through to the canvas gesture surface beneath it; only the dismiss button (a
 * real ≥48dp [TextButton]) consumes its own tap. The host also auto-dismisses it the instant a live
 * drag/resize begins, so discovering the gesture is itself the dismissal.
 *
 * Accessibility: paper card + ink text (theme colours → AA contrast), the instruction is always-present
 * text (never hidden behind motion — there is no motion here, so the reduced-motion path is a no-op),
 * and the dismiss target is touch-safe. The decorative tilt and glyph carry no meaning and are not
 * announced separately (the line is the spoken content).
 *
 * Stateless: the host owns the screen-local "already dismissed" flag and the visibility gate; this
 * composable only renders and reports the dismiss tap via [onDismiss].
 *
 * @param onDismiss invoked when the user taps "Got it" (the host marks the hint dismissed for the session).
 * @param modifier sizing/placement applied by the host (typically aligned to the top of the canvas).
 */
@Composable
public fun EditorMoveResizeHint(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A tilted paper sticky on the desk (this teaching note has no bench.html counterpart — the frozen
    // prototype has no coach/hint surface; it is a WCAG/onboarding affordance). Frozen sticky vocabulary:
    // a `--paper` sheet with `--ink` text, off the abused Material `secondaryContainer` (a baseline lilac
    // absent from the riso palette).
    val colors = ZinelyTheme.colors
    Row(
        modifier = modifier
            .testTag(EditorMoveResizeHintTestTag)
            .graphicsLayer { rotationZ = -1.5f }
            .widthIn(max = 320.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.paper)
            .padding(start = 14.dp, end = 4.dp)
            // One merged label for TalkBack: the teaching line is the content; glyph/tilt are decorative.
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Decorative hand — kept out of the spoken label (the line below is the content); the merged
        // node would otherwise read "✋, Drag to move it…".
        Text(
            text = "✋",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Text(
            text = MoveResizeHintText,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink,
            modifier = Modifier.weight(1f, fill = false),
        )
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag(MoveResizeHintDismissTag),
        ) {
            Text(
                text = MoveResizeHintDismissLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
            )
        }
    }
}
