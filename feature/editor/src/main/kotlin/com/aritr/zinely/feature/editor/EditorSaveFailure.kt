package com.aritr.zinely.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Test tag on the warm save-failure banner. */
public const val EditorSaveFailureTestTag: String = "editor-save-failure"

/** Test tag on the banner's dismiss control. */
public const val SaveFailureDismissTag: String = "editor-save-failure-dismiss"

/**
 * The visible failure line (canonical, docs/design/VOICE.md §Errors "autosave couldn't save"). Names
 * what happened and the **real** way out — no blame, no error code, no emoji (VOICE rule 7 forbids emoji
 * on error copy), and no overclaim. There is no autonomous background retry loop in the live system: a
 * failed autosave leaves the document dirty/retryable, and the next save fires on the next edit (which
 * re-ticks the coordinator) or a lifecycle/explicit flush — so the copy points at making another change,
 * not at an imagined self-retry.
 */
public const val SaveFailureText: String =
    "Couldn’t save your latest change just now. It’ll try again next time you make a change."

/** The dismiss affordance label — the same gentle "Got it" idiom as the move/resize hint. */
public const val SaveFailureDismissLabel: String = "Got it"

/**
 * The warm, honest **save-failure** banner ([ADR-035](../DECISIONS.md#adr-035); VOICE §Errors;
 * DESIGN-LANGUAGE §10). It corrects the optimistic [EditorSavedConfirmation] "Saved ✨": that chip
 * fires when an autosave is *scheduled* (mark-dirty, ADR-034), so a later debounced write can still
 * fail. When the app-scoped `SaveFailureSink` (ADR-026 §5) reports a real failure, the host surfaces
 * this calm line instead — and suppresses the "Saved ✨" chip — so the editor never claims success it
 * doesn't have.
 *
 * **Persistent, not transient.** Unlike the auto-dismissing "Saved ✨", a failure is information the
 * user may need to act on (e.g. free space), so it stays until the user taps [SaveFailureDismissLabel].
 * The frozen `:core:data-storage` coordinator emits no per-edit *success* signal, so the editor cannot
 * auto-clear on a later silent recovery — it errs toward caution (never false reassurance). The copy
 * names the retry honestly: the save reattempts on the user's next change (or a lifecycle flush), not via
 * an autonomous loop the system does not run.
 *
 * **TalkBack: assertive.** A save failure carries asymmetric cost (possible loss of the latest edits),
 * so unlike the polite "Saved ✨" this is a [LiveRegionMode.Assertive] live region — it should not wait
 * behind ongoing speech. The copy itself stays calm; assertiveness is delivery, not alarm.
 *
 * **Quiet placement.** A tonal `errorContainer` pill at the top of the canvas — clear of the thumb-zone
 * supply tray's primary actions below (DESIGN-RULES R3/R7). It takes precedence over the move/resize
 * hint and the "Saved ✨" chip at the top region (the host gates both off while it is visible).
 *
 * **Reduced-motion safe.** The static state is always correct — when [visible] the line is real text.
 * With [reduceMotion] on (system animator scale 0) the fade degrades to an instant appear/disappear;
 * otherwise it is a gentle ~150ms fade in / ~200ms fade out.
 *
 * @param visible whether an unresolved save failure is currently known for this project (ADR-026 §5).
 * @param onDismiss invoked when the user taps "Got it" — the host clears the failure from the sink.
 * @param modifier sizing/placement applied by the host (typically aligned to the top of the canvas).
 * @param reduceMotion whether to drop the fade (defaults to the system "remove animations" setting).
 */
@Composable
public fun EditorSaveFailure(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = rememberReduceMotion(),
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (reduceMotion) EnterTransition.None else fadeIn(tween(durationMillis = 150)),
        exit = if (reduceMotion) ExitTransition.None else fadeOut(tween(durationMillis = 200)),
    ) {
        val colors = MaterialTheme.colorScheme
        Row(
            modifier = Modifier
                .testTag(EditorSaveFailureTestTag)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.errorContainer)
                .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = SaveFailureText,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onErrorContainer,
                modifier = Modifier
                    .weight(1f, fill = false)
                    // Scope the assertive live region to the message only — not the "Got it" button — so
                    // TalkBack announces the failure (and does not wait behind other speech), while the
                    // button stays an independent, focusable, clickable control.
                    .semantics { liveRegion = LiveRegionMode.Assertive },
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(SaveFailureDismissTag),
            ) {
                Text(
                    text = SaveFailureDismissLabel,
                    color = colors.onErrorContainer,
                )
            }
        }
    }
}
