package com.aritr.zinely.feature.editor

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Test tag on the transient autosave-confirmation chip. */
public const val EditorSavedConfirmationTestTag: String = "editor-saved-confirmation"

/** The visible confirmation line (canonical, docs/design/VOICE.md §3 "Success & encouragement"). */
public const val SavedConfirmationText: String = "Saved ✨"

/**
 * The spoken (TalkBack) form — the emoji is decoration and is never required to parse meaning
 * (VOICE rule 7 / accessibility), so the live region announces just "Saved".
 */
public const val SavedConfirmationSpokenLabel: String = "Saved"

/**
 * How long the confirmation stays up before it dismisses itself (ms). Long enough to read at a glance,
 * short enough to stay out of the way — the host owns the timer; this is the shared constant the host
 * and its tests agree on.
 */
public const val SavedConfirmationVisibleMs: Long = 1600L

/**
 * The quiet, transient **"Saved ✨"** autosave confirmation (docs/design/VOICE.md §3 "Success &
 * encouragement"; DESIGN-LANGUAGE.md §10 "a soft 'Saved ✨' fade"; [ADR-034](../DECISIONS.md#adr-034)).
 * It reassures a beginner that work saves automatically — *earned, not constant* (VOICE rule 8): it
 * appears only when a real autosave event fires and removes itself after a short window. The host
 * ([EditorScreen]) owns the trigger (the existing `Effect.Autosave` signal stream) and the timer; this
 * composable only renders the chip for a hoisted [visible] flag.
 *
 * **Quiet by design.** A small tilted paper pill in a top corner of the canvas — far from the thumb-zone
 * supply tray, so it never competes with the one primary-action area (DESIGN-RULES R3/R7). It declares no
 * `pointerInput`, so it is purely passive: touches fall through to the canvas beneath.
 *
 * **TalkBack: polite, not assertive.** The chip is a [LiveRegionMode.Polite] live region, so the
 * announcement waits its turn behind whatever the user is doing rather than interrupting (a save is
 * reassurance, never urgent). The spoken label is [SavedConfirmationSpokenLabel] ("Saved") — the ✨ is
 * decorative and stripped from the a11y tree (VOICE rule 7).
 *
 * **Reduced-motion safe.** The static state is always correct — when [visible] the line is real text,
 * present with or without motion. Motion is decoration on top (DESIGN-LANGUAGE §7/§10): with
 * [reduceMotion] on (the system animator scale is 0), the soft fade degrades to an instant
 * appear/disappear; otherwise it is a gentle ~150ms fade in / ~200ms fade out.
 *
 * @param visible whether an autosave was just scheduled (the host's transient flag) — optimistic
 *   reassurance that work is saving, not a persistence-complete receipt (ADR-034).
 * @param modifier sizing/placement applied by the host (typically aligned to a top corner of the canvas).
 * @param reduceMotion whether to drop the fade (defaults to the system "remove animations" setting).
 */
@Composable
public fun EditorSavedConfirmation(
    visible: Boolean,
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
                .testTag(EditorSavedConfirmationTestTag)
                // A hair of scrapbook tilt — decorative, carries no meaning.
                .graphicsLayer { rotationZ = -2f }
                .clip(RoundedCornerShape(12.dp))
                .background(colors.secondaryContainer)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                // One polite live region; the spoken content is "Saved" (the ✨ below is decorative).
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = SavedConfirmationSpokenLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSecondaryContainer,
            )
            // Decorative sparkle — kept out of the spoken label so TalkBack says "Saved", not "sparkles".
            Text(
                text = "✨",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSecondaryContainer,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

/**
 * The system "remove animations" preference, read once: the global animator-duration scale is `0` when
 * the user has turned animations off (the platform signal a reduced-motion path keys on). Defaults to
 * motion-on if the setting is unreadable.
 */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
