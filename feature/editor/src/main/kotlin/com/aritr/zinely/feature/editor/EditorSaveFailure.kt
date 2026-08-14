package com.aritr.zinely.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.rememberReduceMotion

/** Test tag on the warm save-failure banner. */
public const val EditorSaveFailureTestTag: String = "editor-save-failure"

/** Test tag on the banner's dismiss control. */
public const val SaveFailureDismissTag: String = "editor-save-failure-dismiss"

/** Test tag on the banner's manual-retry control ([ADR-038](../DECISIONS.md#adr-038)). */
public const val SaveFailureRetryTag: String = "editor-save-failure-retry"

/** The manual-retry affordance label ([ADR-038](../DECISIONS.md#adr-038); VOICE §Errors). */
public const val SaveFailureRetryLabel: String = Copy.SaveFailure.RETRY_LABEL

// =================================================================================================
// The frozen `.snack` transcription — `v21-bench.html:467-485`.
//
// Shared by the three **unfrozen** canvas messages in this package ([EditorSaveFailure],
// [EditorCoverageNotice], [EditorMoveResizeHint]), each of which says in its own KDoc why `.snack` is its
// analogy. Transcribed once so the three cannot drift apart into three dialects of one surface — which is
// precisely the failure the Documentation Rule names for prose and which applies no less to pixels.
//
// It is deliberately NOT shared with [BenchSnack], which implements the *frozen* delete/undo toast and
// owns its own `BenchSnack*` values: that one is the freeze, these are analogies to it, and collapsing the
// two would silently re-freeze an analogy.
// =================================================================================================

/** `.snack{gap:var(--gap-md)}` — 12dp between the line and the actions. */
internal val NoticeGap = ZinelyV21Dimens.gapMd

/**
 * `.snack{background:var(--ink);color:var(--paper);border:1.5px solid var(--ink-line)}`.
 *
 * ⚠ The border is `inkLine`, **not `ink`**, and the frozen file says why in a comment of its own: this
 * element's ground *is* `--ink`, so an ink border would be invisible. *"The rule is 'a border contrasts
 * with what it sits on', not 'a border is always ink'."* Rule 2 (*drawn line = ink, shadow = inkLine*) is
 * not being swapped here — there is no shadow on `.snack` to swap it with.
 */
internal val NoticeBorder = 1.5.dp

/** `.snack{border-radius:var(--br-pill)}`, as a percent shape so the outline stays exact at any height. */
internal val NoticeShape: Shape = RoundedCornerShape(percent = 50)

/** `.snack{padding:var(--gap-md) var(--gap-sm) var(--gap-md) var(--gap-lg)}` — 12 / 8 / 12 / 16. */
internal val NoticePaddingStart = ZinelyV21Dimens.gapLg
internal val NoticePaddingEnd = ZinelyV21Dimens.gapSm
internal val NoticePaddingV = ZinelyV21Dimens.gapMd

/** `.snack{font-size:.79rem}` = 12.64px. */
internal val NoticeTextSize = 12.64.sp

/**
 * `.snack{transform:rotate(-.6deg)}` in its shown state (`.snack.show`).
 *
 * The lean is the *settled* state, not the entrance: `.snack` (hidden) is `translateY(8px) rotate(-.6deg)`
 * and `.snack.show` is `rotate(-.6deg)`. So a message that has arrived is still off-square — the one place
 * these surfaces are allowed to perform, and the reason none of them needs a tape mark to look handmade.
 */
internal const val NoticeRotation: Float = -0.6f

/**
 * `.snack button{font-size:.78rem;font-weight:700;color:var(--paper);text-decoration:underline;
 * text-underline-offset:3px;text-decoration-thickness:1.5px;background:none;border:0}` = 12.48px.
 *
 * ⚠ The action label is **`paper`, not `butter`**, and the frozen file records the measurement that
 * changed it: butter on the ink ground is 7.89:1 in light but **1.59:1 in dark**, where that ground turns
 * cream. *"V21-SPEC §3.2's single butter exception is retired, not lived with."* The underline is what
 * carries "this is the action" once the colour no longer can — which is also why it may not be dropped as
 * decoration. Its 3px offset and 1.5px thickness have no Compose expression and are not transcribed.
 */
internal val NoticeActionTextSize = 12.48.sp

/** The two fade windows, retained from V2 — `.snack`'s own `.18s` governs its entrance, not this. */
internal const val NoticeFadeInMillis: Int = 150
internal const val NoticeFadeOutMillis: Int = 200

/**
 * Which honest save-failure copy the banner shows ([ADR-036](../DECISIONS.md#adr-036)). A **feature-local**
 * enum so `:feature:editor` keys the warm storage-specific line without ever depending on `:core:data`'s
 * `DataError` or `:data-android`'s `SaveFailure` — the app maps the failure to this enum at the host seam.
 * [Generic] is the cause-agnostic line; [OutOfSpace] is shown only when the repository's free-space probe
 * proved the device cannot hold the document (never a false "low on storage" claim).
 */
public enum class SaveErrorKind {
    /** A save failed for an unclassified reason (the cause-agnostic [SaveFailureText]). */
    Generic,

    /** A save failed and the device verifiably lacks the space to hold it ([SaveFailureOutOfSpaceText]). */
    OutOfSpace,
}

/**
 * The generic failure line (canonical, docs/design/VOICE.md §Errors "autosave couldn't save"). Names
 * what happened and the **real** way out — no blame, no error code, no emoji (VOICE rule 7 forbids emoji
 * on error copy), and no overclaim. There is no autonomous background retry loop in the live system: a
 * failed autosave leaves the document dirty/retryable, and the next save fires on the next edit (which
 * re-ticks the coordinator) or a lifecycle/explicit flush — so the copy points at making another change,
 * not at an imagined self-retry.
 */
public const val SaveFailureText: String = Copy.SaveFailure.GENERIC

/**
 * The storage-specific failure line ([ADR-036](../DECISIONS.md#adr-036); VOICE §Errors). Shown only when
 * the repository's free-space probe proved the device cannot hold the document — an honest **state**
 * statement ("low on storage"), deliberately softer than "full" because the probe is a heuristic, not a
 * proven proximate cause. It carries the **same no-autonomous-retry honesty** as [SaveFailureText]: there
 * is no background retry, so it names the real trigger ("then keep editing — it'll save"), never implying
 * the save will happen on its own once space is freed.
 */
public const val SaveFailureOutOfSpaceText: String = Copy.SaveFailure.OUT_OF_SPACE

/** The honest line for [kind] (VOICE §Errors). */
private fun saveFailureText(kind: SaveErrorKind): String = when (kind) {
    SaveErrorKind.Generic -> SaveFailureText
    SaveErrorKind.OutOfSpace -> SaveFailureOutOfSpaceText
}

/** The dismiss affordance label — the same gentle "Got it" idiom as the move/resize hint. */
public const val SaveFailureDismissLabel: String = Copy.SaveFailure.DISMISS_LABEL

/**
 * The warm, honest **save-failure** banner ([ADR-035](../DECISIONS.md#adr-035); VOICE §Errors). It
 * corrects the optimistic autosave chip ([BenchStatusStrip]): that chip fires when an autosave is
 * *scheduled* (mark-dirty, ADR-034), so a later debounced write can still fail. When the app-scoped
 * `SaveFailureSink` (ADR-026 §5) reports a real failure, the host surfaces this calm line instead — and
 * suppresses the `Saved` chip — so the editor never claims success it doesn't have.
 *
 * ### ⚠ Unfrozen surface — the analogy is `.snack`, and it is the same analogy V2 made
 *
 * `v21-bench.html` is a happy-path prototype: nothing in it fails, so it draws no error banner. Its one
 * **action-bearing notification** is `.snack` (`:467-485`) — a line of message with a control on its end,
 * floating over the page — which is this banner's shape exactly. V2 reached the same conclusion for the
 * same reason and this is that decision re-transcribed, not a new one; the values are shared through
 * [NoticeShape] and its neighbours so the three canvas messages stay one surface.
 *
 * ### ⚠ It is not painted in `--jam`, and that is a deliberate reading of the urgency rule
 *
 * §3.2 makes `jam` the only urgent colour, which licenses jam for an error — it does not require it, and
 * here it cannot have it. `.snack`'s ground *is* `ink`; `jamText` on `ink` measures **1.8:1** in light,
 * and `jam` on the cream `ink` of dark is no better. The corpus authorises jam-as-text on `paper`
 * (`.ctx button.danger`) and jam-as-outline on `paper`
 * ([ZineShelfFail][com.aritr.zinely.feature.library.ZineShelfFail]'s `!`), and neither pairing exists on
 * this ground. Rather than mix a new one after a freeze, the banner keeps `.snack`'s maximum-contrast
 * ink-on-paper inversion — the loudest object the language has — and delivers the urgency where it is
 * actually asymmetric: the assertive live region below. **Flagged for the owner**: if the Bench is ever
 * re-frozen with a `.snack.danger`, this is the call site that should take it.
 *
 * **Persistent, with honest silent-recovery auto-clear ([ADR-037](../DECISIONS.md#adr-037)).** Unlike
 * the auto-dismissing `Saved` chip, a failure is information the user may need to act on (e.g. free
 * space), so it stays until either the user taps [SaveFailureDismissLabel] **or** a later save is
 * *durably confirmed*. The clear only ever *removes* a resolved failure; it never raises a positive cue.
 *
 * **TalkBack: assertive.** A save failure carries asymmetric cost (possible loss of the latest edits), so
 * unlike the polite `Saved` chip this is a [LiveRegionMode.Assertive] live region — it should not wait
 * behind ongoing speech. The copy itself stays calm; assertiveness is delivery, not alarm.
 *
 * **Reduced-motion safe.** The static state is always correct — when [visible] the line is real text.
 * With [reduceMotion] on the fade degrades to an instant appear/disappear.
 *
 * **Manual retry ([ADR-038](../DECISIONS.md#adr-038)).** A "Try now" control forces an immediate save
 * attempt. Its outcome flows through the *same* durable path as every other save (the coordinator's
 * ADR-037 listener): a real success clears this banner, a repeat failure re-shows it.
 *
 * @param visible whether an unresolved save failure is currently known for this project (ADR-026 §5).
 * @param onDismiss invoked when the user taps "Got it" — the host clears the failure from the sink.
 * @param onRetry invoked when the user taps "Try now" — the host forces an immediate save (ADR-038).
 * @param modifier sizing/placement applied by the host (typically aligned to the top of the canvas).
 * @param kind which honest line to show ([ADR-036](../DECISIONS.md#adr-036)): the cause-agnostic
 *   [SaveErrorKind.Generic] or the storage-specific [SaveErrorKind.OutOfSpace]. Defaults to [Generic].
 *   The host retains the last non-null kind across the exit fade so the copy doesn't flip mid-dismissal.
 * @param reduceMotion whether to drop the fade (defaults to the system "remove animations" setting).
 */
@Composable
public fun EditorSaveFailure(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    kind: SaveErrorKind = SaveErrorKind.Generic,
    reduceMotion: Boolean = rememberReduceMotion(),
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (reduceMotion) EnterTransition.None else fadeIn(tween(NoticeFadeInMillis)),
        exit = if (reduceMotion) ExitTransition.None else fadeOut(tween(NoticeFadeOutMillis)),
    ) {
        val colors = ZinelyTheme.v21Colors
        Row(
            modifier = Modifier
                .testTag(EditorSaveFailureTestTag)
                .graphicsLayer { rotationZ = NoticeRotation }
                .clip(NoticeShape)
                .background(colors.ink)
                .border(NoticeBorder, colors.inkLine, NoticeShape)
                .padding(
                    start = NoticePaddingStart,
                    end = NoticePaddingEnd,
                    top = NoticePaddingV,
                    bottom = NoticePaddingV,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NoticeGap),
        ) {
            Text(
                text = saveFailureText(kind),
                style = TextStyle(
                    fontFamily = ZinelyV21Fonts.Work,
                    fontSize = NoticeTextSize,
                    // `.snack` declares no line-height, so it inherits — as everywhere else in the corpus.
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                    color = colors.paper,
                ),
                modifier = Modifier
                    // `.snack span{flex:1}` — the line takes the residual width, the actions their own.
                    .weight(1f, fill = false)
                    // Scope the assertive live region to the message only — not the buttons — so TalkBack
                    // announces the failure (and does not wait behind other speech), while each button
                    // stays an independent, focusable, clickable control.
                    .semantics { liveRegion = LiveRegionMode.Assertive },
            )
            NoticeAction(SaveFailureRetryLabel, SaveFailureRetryTag, onRetry)
            NoticeAction(SaveFailureDismissLabel, SaveFailureDismissTag, onDismiss)
        }
    }
}

/**
 * One `.snack button` — bold underlined `paper`, on no ground at all.
 *
 * The [TextButton] wrapper is kept from V2 rather than rebuilt as a bare `clickable`: it is what supplies
 * the ≥48dp target ([heightIn]) and the button role/semantics, and swapping it would be re-deciding
 * accessibility while re-skinning. It contributes no visible ground of its own, which is what
 * `background:none;border:0` asks for.
 */
@Composable
internal fun NoticeAction(label: String, tag: String, onClick: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    // A notice's one action — Retry, Dismiss — answers the hand like every other Bench control.
    val act = benchTap(action = onClick)
    TextButton(
        onClick = act,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag(tag),
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Work,
                fontWeight = FontWeight.Bold,
                fontSize = NoticeActionTextSize,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                textDecoration = TextDecoration.Underline,
                color = colors.paper,
            ),
        )
    }
}
