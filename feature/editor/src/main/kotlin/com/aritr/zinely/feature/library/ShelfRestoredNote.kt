package com.aritr.zinely.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.a11y.zinelyV2Control
import com.aritr.zinely.ui.components.zinelyFocusRing
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import kotlinx.coroutines.delay

/** One zine on the restored-details note: its title now, and the one My Shelf showed before, if known. */
public data class RestoredZine(val id: String, val title: String, val previousTitle: String?)

/** `.restored` — the note itself. */
internal const val ShelfRestoredNoteTestTag: String = "shelf-restored-note"

/** `.rs-text` — the title and body, one node for TalkBack. */
internal const val ShelfRestoredNoteTextTestTag: String = "shelf-restored-note-text"

/** `.rs-ok` — *Got it*. */
internal const val ShelfRestoredNoteOkTestTag: String = "shelf-restored-note-ok"

/**
 * `.restored` — the restored-details note under the My Shelf heading (`v21-library.html` A37,
 * [ADR-134](docs/DECISIONS.md#adr-134)).
 *
 * ```css
 * .restored{display:flex;flex-direction:column;gap:var(--gap-xs);background:#FFF6E8;color:#27270F;
 *   border:1.5px solid #27270F;border-radius:var(--br-sm) var(--br-md) var(--br-md) var(--br-sm);
 *   box-shadow:var(--hard) var(--hard) 0 var(--ink-line);
 *   padding:var(--gap-lg) var(--gap-lg) var(--gap-xs);margin:var(--gap-sm) 0 var(--gap-xs);transform:rotate(-.5deg)}
 * .restored .tape{left:22%}
 * ```
 *
 * Paper and ink are **pinned** in both themes (A37.9), like the name field: a piece of paper on the desk. Only its
 * shadow follows the theme, `var(--ink-line)`.
 *
 * ### TalkBack (A37.10)
 *
 * The title and body are **one** node, read "{title}. {body}"; *Got it* is the next stop, outside it. The words
 * are a polite live region **only when the note arrives** ([announce]) on a My Shelf that has already drawn its
 * zines. Then they are put in [ArrivalSettleMillis] after the node, as the frozen page does, so the platform sees
 * the words change on a live region rather than a node arriving already filled. A note that is there when the
 * Shelf draws is read in order and never announced, and once its words change while it is up (a zine renamed,
 * moved, deleted or joining) the live region is dropped in that same pass, so the change is silent: the A28 snack
 * already speaks the action.
 *
 * @param announce whether this note arrived on a Shelf already showing its zines. Read once, when the note is
 *   first composed.
 */
@Composable
internal fun ShelfRestoredNote(
    title: String,
    body: String,
    announce: Boolean,
    onGotIt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    val spoken = Copy.Restored.spoken(title, body)
    var held by remember { mutableStateOf(announce) }
    var liveFor by remember { mutableStateOf(if (announce) spoken else null) }
    val live = liveFor == spoken
    // Sticky: words that changed once are never live again, even if they change back.
    SideEffect { if (!live && liveFor != null) liveFor = null }
    LaunchedEffect(Unit) {
        if (held) {
            delay(ArrivalSettleMillis)
            held = false
        }
    }

    // `margin:var(--gap-sm) 0 var(--gap-xs)`, added to the grid's own row gap as a margin is in CSS grid.
    Box(
        modifier
            .testTag(ShelfRestoredNoteTestTag)
            .fillMaxWidth()
            .padding(top = ZinelyV21Dimens.gapSm, bottom = ZinelyV21Dimens.gapXs)
            .graphicsLayer { rotationZ = NoteRotation },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .zinelyV21HardShadow(ZinelyV21Dimens.hardShadow, colors.inkLine, NoteShape)
                .background(NotePaper, NoteShape)
                .border(NoteBorder, NoteInk, NoteShape)
                .padding(
                    start = ZinelyV21Dimens.gapLg,
                    top = ZinelyV21Dimens.gapLg,
                    end = ZinelyV21Dimens.gapLg,
                    bottom = ZinelyV21Dimens.gapXs,
                ),
            verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapXs),
        ) {
            // `.rs-text` — `role="status"`, two paragraphs, merged here into the one node A37.10 asks for.
            Column(
                Modifier
                    .fillMaxWidth()
                    .testTag(ShelfRestoredNoteTextTestTag)
                    .clearAndSetSemantics {
                        if (!held) contentDescription = spoken
                        if (live) liveRegion = LiveRegionMode.Polite
                    },
            ) {
                Text(title, style = TitleStyle)
                Text(body, style = BodyStyle)
            }
            GotIt(onGotIt, Modifier.align(Alignment.End))
        }
        // `.restored .tape{left:22%}` on `.tape{top:-11px;transform:translateX(-50%) rotate(-4deg)}`, in the
        // padding box, inside the 1.5px border.
        BoxWithConstraints(Modifier.matchParentSize().padding(NoteBorder)) {
            ZineTape(TapeRotation, Modifier.offset(x = maxWidth * TapeFraction - TapeWidth / 2, y = TapeOffsetY))
        }
    }
}

/**
 * `.rs-ok` — the snack's text button, in pinned ink, at least 48dp tall at any text size, under the words at the
 * end edge, never beside them (A37.8).
 *
 * ```css
 * .rs-ok{align-self:flex-end;min-height:48px;padding:var(--gap-xs) var(--gap-md);border-radius:var(--br-pill);
 *   color:#27270F;font:700 .86rem/1.2 var(--sans);text-decoration:underline}
 * .rs-ok:active{background:rgba(39,39,15,.10)}
 * .rs-ok:focus-visible{outline:2px solid #27270F;outline-offset:2px}
 * ```
 *
 * `text-underline-offset` and `text-decoration-thickness` have no Compose equivalent: the underline is drawn at
 * the platform's offset and weight, as on the folder's back control.
 */
@Composable
private fun GotIt(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val pill = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
    Box(
        modifier
            .testTag(ShelfRestoredNoteOkTestTag)
            .zinelyFocusRing(focused, ZinelyV21Dimens.radiusPill, OkFocusOffset, color = NoteInk)
            .zinelyV2Control(label = Copy.Restored.GOT_IT, interactionSource = interaction, onClick = onClick)
            .defaultMinSize(minHeight = OkMinHeight)
            .background(if (pressed) OkPressed else Color.Transparent, pill)
            .padding(horizontal = ZinelyV21Dimens.gapMd, vertical = ZinelyV21Dimens.gapXs),
        contentAlignment = Alignment.Center,
    ) {
        Text(Copy.Restored.GOT_IT, style = OkStyle)
    }
}

/**
 * How long the words wait after the note on arrival: the frozen page's `setTimeout(…, 150)`. Compose sends
 * accessibility changes at most every 100ms, so words put in within the same window would reach the platform with
 * the node, already filled, which R29.4 leaves open as to whether TalkBack speaks.
 */
internal const val ArrivalSettleMillis = 150L

/** `background:#FFF6E8;color:#27270F;border:1.5px solid #27270F` — pinned. */
private val NotePaper = Color(0xFFFFF6E8)
private val NoteInk = Color(0xFF27270F)
private val NoteBorder = 1.5.dp

/** `.rs-body{color:#6A452F}`, 7.8:1 on the pinned paper. */
private val NoteBodyInk = Color(0xFF6A452F)

/** `border-radius:var(--br-sm) var(--br-md) var(--br-md) var(--br-sm)` — 8 / 14 / 14 / 8. */
private val NoteShape = RoundedCornerShape(
    topStart = ZinelyV21Dimens.radiusSm,
    topEnd = ZinelyV21Dimens.radiusMd,
    bottomEnd = ZinelyV21Dimens.radiusMd,
    bottomStart = ZinelyV21Dimens.radiusSm,
)

/** `transform:rotate(-.5deg)`. */
private const val NoteRotation = -0.5f

/** `.restored .tape{left:22%}`, keeping `.tape`'s own `rotate(-4deg)`. */
private const val TapeFraction = 0.22f
private const val TapeRotation = -4f

/** `.rs-ttl{font-family:var(--voice);font-weight:700;font-size:1.05rem;line-height:1.2}`. */
private val TitleStyle = TextStyle(
    fontFamily = ZinelyV21Fonts.Voice,
    fontWeight = FontWeight.Bold,
    fontSize = 16.8.sp,
    lineHeight = 20.16.sp,
    color = NoteInk,
)

/** `.rs-body{font-size:.86rem;line-height:1.45;color:#6A452F}` in the body's sans. */
private val BodyStyle = TextStyle(
    fontFamily = ZinelyV21Fonts.Work,
    fontSize = 13.76.sp,
    lineHeight = 19.952.sp,
    color = NoteBodyInk,
)

/** `.rs-ok{font:700 .86rem/1.2 var(--sans);text-decoration:underline}`. */
private val OkStyle = TextStyle(
    fontFamily = ZinelyV21Fonts.Work,
    fontWeight = FontWeight.Bold,
    fontSize = 13.76.sp,
    lineHeight = 16.512.sp,
    color = NoteInk,
    textDecoration = TextDecoration.Underline,
)

private val OkMinHeight = 48.dp
private val OkFocusOffset = 2.dp

/** `.rs-ok:active{background:rgba(39,39,15,.10)}`. */
private val OkPressed = Color(0x1A27270F)
