package com.aritr.zinely.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.TextCoverage
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.rememberReduceMotion

/** Test tag on the unsupported-character coverage notice. */
public const val EditorCoverageNoticeTestTag: String = "editor-coverage-notice"

/**
 * The live **unsupported-character** notice ([ADR-070](../DECISIONS.md#adr-070); VOICE §Errors) — the
 * honesty half of the typography work. The document renderer prints only the ratified script set
 * ([`SupportedScripts.BUNDLED_SCRIPTS`][com.aritr.zinely.core.model.SupportedScripts]); a character
 * outside it would otherwise reach paper blank with **no warning**, silently losing the user's words.
 * This names the script that cannot print, so nothing disappears without a kind, specific explanation.
 *
 * **Permanent product behaviour, not a placeholder.** It stays valuable even after more scripts are
 * supported, because it is driven by the current coverage of the *typed text* against the bundled set —
 * there will always be characters (and future scripts) outside that set. It must never be weakened or
 * removed by later script-support work.
 *
 * **Live and reactive, never blocking.** [coverage] is recomputed from the draft on every keystroke by
 * [EditTextSession]; the notice appears while unprintable characters are present and clears itself the
 * moment they are removed. It carries no dismiss control on purpose: it is a *status of the current
 * text*, not a persisted alert like the save-failure banner — dismissing a truth that is still true, only
 * to have it reappear on the next keystroke, would be noise. The character itself is **never stripped**
 * (that lives in [EditTextSession]'s draft), so it prints unchanged the day its script is supported.
 *
 * **Copy names the script.** The line is built from the distinct human script names in the coverage
 * ([Copy.Coverage.unsupported]); it auto-narrows if the bundled set grows, because a supported script
 * stops appearing in [TextCoverage.unsupportedScripts] and so stops being named — no copy change.
 *
 * **TalkBack: polite.** Unlike the save-failure banner (assertive — edits may already be lost), this is
 * *preventive*: nothing is lost yet. A [LiveRegionMode.Polite] region announces the situation without
 * barging over ongoing speech, and — because the rendered text only changes when the *set* of unprintable
 * scripts changes — it does not re-announce on every keystroke of the same script.
 *
 * ### ⚠ Unfrozen surface — the analogy is `.snack`, minus its button
 *
 * `v21-bench.html` never types a character it cannot print, so it draws nothing for this state. Its one
 * message-over-the-page surface is `.snack` (`:467-485`), and this notice takes it whole — the same
 * inverted `ink`/`paper` box, the same pill, the same `-0.6°` lean, through the shared
 * [NoticeShape]/[NoticeTextSize] transcription in [EditorSaveFailure]. It takes **no** `.snack button`,
 * because it has no action to offer: the notice is a status that clears itself, and a control that only
 * hides a still-true statement is the noise the paragraph above rules out. Dropping the one element the
 * analogy does not need is the reason this stays transcription rather than becoming invention.
 *
 * **Reduced-motion safe.** With [reduceMotion] on, the fade degrades to an instant appear/disappear; the
 * static state (real text when present, absent otherwise) is always correct.
 *
 * @param coverage the current text's coverage; the notice is visible iff it is not fully covered.
 * @param modifier sizing/placement applied by the host (typically aligned to the top of the canvas).
 * @param reduceMotion whether to drop the fade (defaults to the system "remove animations" setting).
 */
@Composable
public fun EditorCoverageNotice(
    coverage: TextCoverage,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = rememberReduceMotion(),
) {
    // Retain the last non-empty script names across the exit fade, so the sentence doesn't blank out
    // mid-dismissal when `coverage` flips back to Covered (which carries no scripts to name).
    var lastNames by remember { mutableStateOf(emptyList<String>()) }
    if (!coverage.isFullyCovered) {
        lastNames = coverage.unsupportedScripts.map { it.displayName }.distinct()
    }

    AnimatedVisibility(
        visible = !coverage.isFullyCovered,
        modifier = modifier,
        enter = if (reduceMotion) EnterTransition.None else fadeIn(tween(NoticeFadeInMillis)),
        exit = if (reduceMotion) ExitTransition.None else fadeOut(tween(NoticeFadeOutMillis)),
    ) {
        val colors = ZinelyTheme.v21Colors
        Row(
            modifier = Modifier
                .testTag(EditorCoverageNoticeTestTag)
                .graphicsLayer { rotationZ = NoticeRotation }
                .clip(NoticeShape)
                .background(colors.surfaceSoft)
                // A warm support scrap uses the room's ordinary ink for its visible outline.
                .border(NoticeBorder, colors.ink, NoticeShape)
                // With no trailing button, `.snack`'s asymmetric 8dp end padding would leave the sentence
                // hard against the pill's curve. The start value is used on both ends, which is the same
                // box the freeze draws — its right side is short only to make room for a control.
                .padding(horizontal = NoticePaddingStart, vertical = NoticePaddingV),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = Copy.Coverage.unsupported(lastNames),
                style = TextStyle(
                    fontFamily = ZinelyV21Fonts.Work,
                    fontSize = NoticeTextSize,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                    color = colors.ink,
                ),
                // Preventive, not a loss that already happened → Polite (never barges over speech). The
                // text is stable while the same scripts are present, so this does not re-announce per key.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}
