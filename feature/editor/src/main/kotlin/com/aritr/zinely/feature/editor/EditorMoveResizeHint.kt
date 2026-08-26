package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Fonts

/** Test tag on the move/resize hint container. */
public const val EditorMoveResizeHintTestTag: String = "editor-move-resize-hint"

/** Test tag (and stable instrumentation hook) on the hint's dismiss control. */
public const val MoveResizeHintDismissTag: String = "move-resize-hint-dismiss"

/** The teaching line (canonical, docs/design/VOICE.md §3 "Hints"). */
public const val MoveResizeHintText: String = Copy.MoveResizeHint.TEXT

/** The photo-specific line that distinguishes frame resize from cropping the contents. */
public const val PhotoMoveResizeHintText: String = Copy.MoveResizeHint.PHOTO_TEXT

/** The dismiss affordance label (the implicit/explicit "got it" VOICE requires for a one-time hint). */
public const val MoveResizeHintDismissLabel: String = Copy.Common.GOT_IT

/** The V2 sticky's own `max-width:320px`, kept: the line is one sentence and reads badly wider. */
private val HintMaxWidth = 320.dp

/**
 * The one-time **move/resize hint** (docs/design/VOICE.md §3 "Hints"). When a beginner first selects a
 * placed element the resize handles and lower transform controls appear, but their meaning is easy to miss.
 * This gentle note names the handles and turn controls already on screen, then gets out of the way. For a
 * photo it also distinguishes resizing its frame from [ReframeAffordanceChip]'s crop-inside action.
 *
 * ### ⚠ Unfrozen surface — the analogy is `.snack`, and it replaces V2's invented sticky
 *
 * The frozen prototype has no coach mark: it teaches through its own caption strip (`.hint`,
 * `v21-bench.html:502`), which is **prototype narration** — it lives outside the phone frame, explaining
 * the demo to someone reading a design file. Transcribing it would put the caption *inside* the product,
 * which is exactly the class of mistake `cap()` and `"the bench"` are excluded for. So the caption's
 * selector name is a false friend and is deliberately not the source here.
 *
 * The real analogy is `.snack` (`:467-485`): a single line of message floating over the page with one
 * control on its end — this hint's shape, element for element. Taking it also **retires a V2 invention**:
 * V2 drew this as a tilted `--paper` sticky with a decorative `✋`, a surface that existed nowhere in the
 * freeze and that the corpus does not otherwise draw. The `-0.6°` lean `.snack` carries in its settled
 * state keeps the handmade feel the sticky was reaching for, without a second dialect for it.
 *
 * The hand glyph goes with the sticky. It was `clearAndSetSemantics`-silenced decoration on a surface
 * whose whole content is one sentence; `.snack` draws its message and nothing else, and a mark that is
 * hidden from every reader and adds nothing to any other is not a loss when it leaves.
 *
 * It is non-modal and non-blocking — the row declares no `pointerInput`, so touches fall straight through
 * to the canvas gesture surface beneath it; only the dismiss button consumes its own tap. The host also
 * auto-dismisses it the instant a live drag/resize begins, so discovering the gesture is itself the
 * dismissal.
 *
 * Accessibility: the instruction is always-present text (never hidden behind motion — there is no motion
 * here, so the reduced-motion path is a no-op), the whole row merges into one spoken label, and the
 * dismiss target stays a real ≥48dp button ([NoticeAction]).
 *
 * Stateless: the host owns the screen-local "already dismissed" flag and the visibility gate; this
 * composable only renders and reports the dismiss tap via [onDismiss].
 *
 * @param photo whether the selected element is a photo, enabling the contextual Reframe sentence.
 * @param onDismiss invoked when the user taps "Got it" (the host marks the hint dismissed for the session).
 * @param modifier sizing/placement applied by the host (typically aligned to the top of the canvas).
 */
@Composable
public fun EditorMoveResizeHint(
    photo: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    Row(
        modifier = modifier
            .testTag(EditorMoveResizeHintTestTag)
            .widthIn(max = HintMaxWidth)
            .graphicsLayer { rotationZ = NoticeRotation }
            .clip(NoticeShape)
            .background(colors.surfaceSoft)
            // A warm support scrap uses the room's ordinary ink for its visible outline.
            .border(NoticeBorder, colors.ink, NoticeShape)
            .padding(
                start = NoticePaddingStart,
                end = NoticePaddingEnd,
                top = NoticePaddingV,
                bottom = NoticePaddingV,
            )
            // One merged label for TalkBack: the teaching line is the content, and the button keeps its
            // own node because `mergeDescendants` does not swallow a clickable child's semantics.
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NoticeGap),
    ) {
        Text(
            text = if (photo) PhotoMoveResizeHintText else MoveResizeHintText,
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = NoticeTextSize,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                color = colors.ink,
            ),
            // `.snack span{flex:1}`. Fill the allocated share rather than measuring at the sentence's
            // intrinsic width: the contextual photo line is longer, and an intrinsic child can otherwise
            // push both its opening words and Got it outside the 320dp support scrap.
            modifier = Modifier.weight(1f),
        )
        NoticeAction(MoveResizeHintDismissLabel, MoveResizeHintDismissTag, colors.ink, onDismiss)
    }
}
