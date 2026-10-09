package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign as ComposeTextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.FontReason
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.fontRow
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DocumentVoice
import com.aritr.zinely.core.model.TextAlign
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.ui.components.zinelyFocusRing
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.components.zinelyV21Pressable
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press
import com.aritr.zinely.ui.theme.rememberReduceMotion
import java.util.Locale
import kotlinx.coroutines.delay

/** Test tag on the Type bar surface; absent from the tree unless a single non-blank text box is selected. */
public const val TypeBarTestTag: String = "type-bar"
internal const val TypeBarInkRowTestTag: String = "type-bar-ink-row"

/** The one reason line under the Font row (A29 rule 8); absent from the tree when there is nothing to say. */
internal const val TypeBarFontReasonTestTag: String = "type-bar-font-reason"

/** The Font row's group node: the named single-choice group A29 rule 1 asks for. */
internal const val TypeBarFontGroupTestTag: String = "type-bar-font-group"

/**
 * The words for a [FontReason] (frozen `v21-typebar.html` A29 rule 8). One string serves the visible line
 * and the spoken reason, so the two cannot drift.
 */
internal fun fontReasonLine(reason: FontReason): String {
    val minPt = DocumentVoice.BOOK_MIN_SIZE_PT.toInt()
    return when (reason) {
        FontReason.UnknownFont -> Copy.Type.FONT_UNKNOWN
        is FontReason.BookLacksScript ->
            Copy.Type.bookHasNoScript(Copy.Type.bookScripts(reason.greek, reason.cyrillic))
        FontReason.BookNeedsSize -> Copy.Type.bookNeedsSize(minPt)
        FontReason.BookAtFloor -> Copy.Type.bookStopsAt(minPt)
    }
}

/**
 * The point ramp the size stepper walks (frozen bench.html `SIZES`). This is a **surface-owned
 * display→model mapping**, not a model constraint: `Intent.StyleText.sizePt` is an unconstrained
 * `Double` and the reducer neither clamps nor snaps (ADR-055 Decision 6). The ramp's ends *are* the
 * stepper's boundaries — index 0 disables "Smaller", the last index disables "Larger".
 */
internal val TypeSizesPt: List<Double> = listOf(10.0, 12.0, 14.0, 16.0, 20.0, 24.0, 28.0, 32.0, 40.0, 48.0)

/**
 * The five text inks (frozen bench.html `INKS`), each pinned to **one fixed paper-space [ColorRgba]**.
 *
 * Theme-independent by design (ADR-055 Decision 6): a colour committed here is printed ink, so it
 * resolves to the *light*-paper value of the token regardless of the viewer's theme — a zine styled in
 * dark mode must print the same as one styled in light mode. `Ochre` is the deliberately darkened
 * yellow-as-text (`#7A5E12`, AA on both papers, bench RF4) rather than the `yellow` token itself.
 *
 * Distinct from the image spot-ink field set — the two must not be conflated (ADR-055 Decision 6).
 */
internal enum class TextInk(val label: String, val rgba: ColorRgba) {
    Ink(Copy.Type.INK_INK, ColorRgba(0x23, 0x20, 0x1C)),
    Coral(Copy.Type.INK_CORAL, ColorRgba(0xA6, 0x3C, 0x22)),
    Teal(Copy.Type.INK_TEAL, ColorRgba(0x2A, 0x9D, 0x8F)),
    Blue(Copy.Type.INK_BLUE, ColorRgba(0x26, 0x46, 0x53)),
    Ochre(Copy.Type.INK_OCHRE, ColorRgba(0x7A, 0x5E, 0x12)),
}

/** The settle window the size stepper coalesces a tap burst into one commit over (bench `sizeCommit`). */
internal const val TypeSizeSettleMs: Long = 400L

/**
 * The style-commit haptic (frozen bench.html `buzz("tick")` — fired on every accepted style change:
 * a size step, an alignment, a bold/italic toggle, an ink).
 *
 * **Reduced motion silences it, deliberately.** That reads odd — a buzz is not motion — but it is the
 * frozen contract (`bench.html` gates `buzz` on `reduced()`), and the platform treats both as the same
 * "quiet, please" preference. Diverging here would be a redesign, not a parity fix.
 *
 * ponytail: this is the whole haptic layer — the [LocalHapticFeedback] the platform already provides,
 * plus the existing [rememberReduceMotion] gate. No injected seam, no interface: tests override the
 * CompositionLocal, which is the Compose-native way to observe it (ADR-055 §8 — the editor's first
 * haptics; establishing a new abstraction for one call site would be the thing to justify, not this).
 *
 * **Bench's `buzz("boundary")` is deliberately not ported — it is not the refusal feedback it looks like.**
 * `syncTypeBar` disables the stepper on the same index expression that guards it, so the one way to reach
 * it in the prototype is a *stale bar*: `syncToolbar` leaves the Type bar open across a **text→text**
 * selection change without re-syncing it, so the flags (and the readout) describe the previously synced
 * block while `setSize` acts on the current one — which contradicts the freeze's own "a selection change
 * closes it". [EditorScreen] closes the bar on any target-id change and this bar derives `enabled` from
 * the live element every recomposition, so the stale state cannot occur here. Porting the buzz would mean
 * porting the staleness.
 */
@Composable
internal fun rememberStyleBuzz(): () -> Unit {
    val haptics = LocalHapticFeedback.current
    val reduceMotion = rememberReduceMotion()
    return remember(haptics, reduceMotion) {
        { if (!reduceMotion) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
}

/**
 * The Bold verb (bench `toggleBold`) — the ONE implementation the Type bar's toggle and the
 * `Ctrl/Cmd + B` shortcut (ADR-055 §4) both call, so the two paths cannot drift apart: same patch, same
 * haptic, same spoken line. The reducer stays the single source of truth; this reads [element]'s current
 * style and hands it the flipped value.
 */
internal fun toggleBold(
    element: TextElement,
    dispatch: (Intent) -> Unit,
    announce: (String) -> Unit,
    buzz: () -> Unit,
) {
    val on = !element.style.bold
    dispatch(Intent.StyleText(id = element.id, bold = on))
    buzz()
    announce(if (on) Copy.Type.BOLD_ON else Copy.Type.BOLD_OFF)
}

/** The Italic verb (bench `toggleItalic`) — [toggleBold]'s twin; same shared-verb contract. */
internal fun toggleItalic(
    element: TextElement,
    dispatch: (Intent) -> Unit,
    announce: (String) -> Unit,
    buzz: () -> Unit,
) {
    val on = !element.style.italic
    dispatch(Intent.StyleText(id = element.id, italic = on))
    buzz()
    announce(if (on) Copy.Type.ITALIC_ON else Copy.Type.ITALIC_OFF)
}

/** Nearest ramp index to an arbitrary `sizePt` (bench `nearestSize`) — the ramp need not contain it. */
internal fun nearestSizeIndex(sizePt: Double): Int {
    var best = 0
    var bestDelta = Double.MAX_VALUE
    TypeSizesPt.forEachIndexed { i, s ->
        val d = kotlin.math.abs(s - sizePt)
        if (d < bestDelta) {
            bestDelta = d
            best = i
        }
    }
    return best
}

/**
 * The non-modal Type bar (FR-3, [ADR-055], frozen bench.html `.typebar`) — the Compose surface over the
 * already-shipped `Intent.StyleText` reducer path.
 *
 * **It owns no styling state.** Every control reads [element]`.style` and dispatches a nullable-patch
 * [Intent.StyleText]; the reducer is the single source of truth. There is no session, no draft, no
 * commit/cancel — **cancel is undo** — so an undo/redo that restores a different style re-syncs this bar
 * for free on the next recomposition (ADR-055 §3).
 *
 * Font / align / bold / italic / colour each commit **instantly**: one tap, one [Intent.StyleText], one
 * undo step. The Font row is amendment A29 (ADR-126): first in the card, with one reason line under it,
 * and the card scrolls inside itself when its rows are taller than the room it is given. Size is the exception (ADR-055 §3): the stepper coalesces a tap burst behind a
 * [TypeSizeSettleMs] settle window and dispatches **one** patch carrying the final ramp value, so
 * "tap + four times" is one undo step rather than four.
 *
 * **The canvas does not wait for that settle.** The frozen bench splits the two halves of a size step —
 * `applyTextStyle` repaints the block synchronously, only `snapshot()` sits behind the 400 ms
 * `sizeCommit` timer — so the debounce coalesces the *undo entry*, never the preview. The in-flight
 * style therefore leaves this bar through [onPreview] and is projected onto the render page by
 * [com.aritr.zinely.core.editor.LivePreview.applyStyleOverride], exactly as an open drag is. That is a
 * render-time projection, not a styling draft: [pendingSizeIndex] holds a ramp *index*, the reducer
 * remains the only owner of `document`, and it still commits the instant it is handed the intent.
 *
 * The host renders this only for a single non-blank text selection; a selection change to anything else
 * unmounts it (the reducer likewise no-ops a `StyleText` on a blank box, so the two agree).
 *
 * @param element the selected text box — the authoritative style the controls display.
 * @param dispatch forwards an [Intent] into the store.
 * @param onAnnounce speaks a discrete style change (WCAG 4.1.3), routed to the host's
 *   `announceForAccessibility` drain — the same channel Reframe and the image-pick failure use.
 * @param onPreview publishes the in-flight style of a settling size burst (`null` when nothing is
 *   pending) for the host to hand the canvas. Always cleared on dispose — a stale override would
 *   outlive the bar and paint a size the document never took.
 */
@Composable
internal fun TypeBar(
    element: TextElement,
    dispatch: (Intent) -> Unit,
    onAnnounce: (String) -> Unit,
    onPreview: (Map<String, TextStyle>?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = element.style
    val id = element.id
    // Every accepted style change buzzes (bench `buzz("tick")`). Fired at the *step*, not at the commit:
    // a three-tap size burst buzzes three times and commits once, exactly as the prototype does — the
    // haptic confirms the tap, the settle only coalesces the undo entry.
    val buzz = rememberStyleBuzz()

    // The stepper's in-flight readout index while a tap burst settles. Reset per element: a selection
    // change must never carry a half-settled size onto the next box.
    var pendingSizeIndex by remember(id) { mutableStateOf<Int?>(null) }
    val committedIndex = nearestSizeIndex(style.sizePt)
    val sizeIndex = pendingSizeIndex ?: committedIndex

    // bench `applyTextStyle(b)` — the canvas half of a size step, which the frozen prototype runs
    // synchronously *before* scheduling `sizeCommit()`. Published to the host every time the pending
    // index moves (and cleared the moment it settles, is superseded, or the bar goes away), so the block
    // on the page tracks the readout instead of trailing it by the settle window.
    val previewStyle = pendingSizeIndex?.let { mapOf(id to style.copy(sizePt = TypeSizesPt[it])) }
    LaunchedEffect(previewStyle) { onPreview(previewStyle) }

    // The settle window (bench `sizeCommit`): each further tap restarts it by re-keying this effect, so
    // only the final value of a burst reaches the reducer — one undo step, not one per tap.
    LaunchedEffect(id, pendingSizeIndex) {
        val pending = pendingSizeIndex ?: return@LaunchedEffect
        delay(TypeSizeSettleMs)
        dispatch(Intent.StyleText(id = id, sizePt = TypeSizesPt[pending]))
        pendingSizeIndex = null
    }

    // An external change to this box's size — an undo/redo, most likely — supersedes an in-flight burst.
    // Drop the burst rather than let it land 400ms later on top of the restored style (which would also
    // silently wipe the redo stack the user just built). Our OWN commit lands `committedIndex == pending`,
    // so it falls through this guard untouched.
    LaunchedEffect(committedIndex) {
        if (pendingSizeIndex != null && pendingSizeIndex != committedIndex) pendingSizeIndex = null
    }

    // Flush a burst that is still settling when the bar goes away (closed, deselected, an inline edit
    // opened, the page switched). Without this the change is LOST: the readout already moved and the
    // announcement already claimed it, so dropping it would be the surface lying about what it did. The
    // settle exists to coalesce undo entries, not to make a committed-looking change conditional on the
    // user waiting 400ms. Keyed on `id`, so a selection change flushes to the box being LEFT, not the
    // one arriving; a delete races this to a reducer no-op (absent id), which is exactly right.
    DisposableEffect(id) {
        onDispose {
            pendingSizeIndex?.let { dispatch(Intent.StyleText(id = id, sizePt = TypeSizesPt[it])) }
            // Flush first, then drop the override: the dispatch above puts the same size into the
            // document, so the canvas never falls back through the old one. That ordering rests on the
            // host's `uiState` collector resuming before the next composition — not on `dispatch` itself
            // being synchronous — which is the same assumption the shipped `resizeOverride`/
            // `CommitTransform` path already makes. Clearing is unconditional: an override that outlived
            // its bar would paint a style nothing owns.
            onPreview(null)
        }
    }

    // A29: the Font row's state, derived from the element on every recomposition and never stored. It reads
    // the size the READOUT shows, so a settling burst that carries the text across Book's minimum moves the
    // row with it rather than 400ms later.
    val shownStyle = pendingSizeIndex?.let { style.copy(sizePt = TypeSizesPt[it]) } ?: style
    val font = fontRow(element.text, shownStyle, rampMinPt = TypeSizesPt.first())
    val fontLine = font.line?.let(::fontReasonLine)
    val scroll = rememberScrollState()

    // `v21-typebar.html` `.typebar` — **`.inkpop`'s card**, and deliberately not a card of its own. Same
    // app, same canvas, same job: a floating tray of controls belonging to the element that summoned it,
    // standing where `.inkpop` and `.ctx` stand. `--surface` ground, 1.5dp ink edge, `--br-lg`, and the 4dp
    // `--hard` offset shadow the corpus gives anything that has left the surface.
    //
    // V1 asked for a `--menu` ground, a `--fieldEdge` hairline and `shadowElevation = 6.dp`: one ground
    // V2.1 does not define, and a *blurred* shadow in a language whose shadows are all flat and offset.
    // Those three together are what made this panel read as another app's dialog parked on the Bench.
    val cardColors = ZinelyTheme.v21Colors
    val cardShape = RoundedCornerShape(ZinelyV21Dimens.radiusLg)
    Box(
        modifier = modifier
            .testTag(TypeBarTestTag)
            // Nothing that clips may sit left of the shadow — it paints outside the node.
            .zinelyV21HardShadow(ZinelyV21Dimens.hardShadow, cardColors.inkLine, cardShape)
            .clip(cardShape)
            .background(cardColors.surface)
            // A29 rule 11, the second sign that the card scrolls: a shade on whichever edge hides more
            // (`.typebar.a29`'s two `scroll` background layers, 22px, `--soft-shadow`). The first sign is the
            // row the edge cuts, which needs no drawing. Behind the rows, as a CSS background is.
            .drawBehind {
                val shade = TypeBarScrollShade.toPx()
                if (scroll.canScrollBackward) {
                    drawRect(
                        Brush.verticalGradient(listOf(cardColors.softShadow, Color.Transparent), 0f, shade),
                        size = Size(size.width, shade),
                    )
                }
                if (scroll.canScrollForward) {
                    drawRect(
                        Brush.verticalGradient(listOf(Color.Transparent, cardColors.softShadow), size.height - shade, size.height),
                        topLeft = Offset(0f, size.height - shade),
                        size = Size(size.width, shade),
                    )
                }
            }
            .border(BenchChromeBorder, cardColors.ink, cardShape)
            // ⚠ **The card must swallow the taps that land on its own surface**, and this empty
            // `pointerInput` is the whole of that. It is not decoration: `Surface` installs exactly this
            // (`Modifier.pointerInput(Unit) {}`) and dropping `Surface` for a `Box` dropped it silently.
            //
            // Without it, a tap on the card's padding — or in the gap between two controls, or 3dp off a
            // stepper chip — finds nothing here and falls through to the canvas underneath, which
            // deselects the element and closes the bar. So the panel would shut when a finger *nearly*
            // hit a button, which reads as the app throwing the tool away for missing.
            //
            // It also silently disarmed the input-layer touch-target expansion on the controls: a hit
            // just outside a chip's paint resolves as a *speculative* minimum-touch-target hit, and a
            // speculative hit loses to a real one further down the tree. With the canvas taking the real
            // hit, every 48dp expansion on this card was decorative. That is what actually failed —
            // [TypeBarTest.a_tap_outside_the_stepper_chips_frozen_paint_still_steps_the_size], the test
            // written to prove the expansion is real, which caught it exactly as designed.
            .pointerInput(Unit) {},
    ) {
        // The spec pins `font-family:var(--sans)` on the panel's own rules (`.tyval`, `.tytog button`,
        // `.tyalign button`) rather than leaving it to inheritance. Compose's inheritance goes the other
        // way: `MaterialTheme` ends in `ProvideTextStyle(typography.bodyLarge)`, and that scale is still
        // deliberately on `FontFamily.Default` until the last screen is reskinned (Type.kt), so an
        // unstyled `Text` here would paint Roboto — a face `--sans`'s stack lists only as a fallback the
        // app makes unreachable by bundling its own. One provision covers every `Text` in the card, and
        // zeroes the 0.5sp tracking `bodyLarge` also carries (the rules declare none; the label sets its
        // own .13em below). `--sans` is V2.1's [ZinelyV21Fonts.Work], not V1's `typography.shell`.
        ProvideTextStyle(
            LocalTextStyle.current.copy(
                fontFamily = ZinelyV21Fonts.Work,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                letterSpacing = 0.sp,
            ),
        ) {
            // No group-level semantics wrapper (the NudgePad rule): each control carries its own spoken
            // label, and a parent clearAndSetSemantics would clear the children TalkBack navigates to.
            Column(
                // bench `.typebar{width:max-content; align-items:stretch}`: the card is exactly as wide as
                // its widest row, and every row is then stretched to that width. `IntrinsicSize.Max` is the
                // Compose spelling of both halves at once — it asks the rows for their max-content width,
                // takes the largest, and hands that down as a fixed width the rows' `fillMaxWidth` resolves
                // against. Without it a row's `fillMaxWidth` would resolve against the incoming max (the
                // screen).
                // `.typebar{padding:var(--gap-md) var(--gap-lg); gap:var(--gap-sm)}`. The padding is
                // symmetric vertically where `.inkpop`'s is not: `.inkpop` carries an `h4` whose own margin
                // already opens its top, and this card has no heading, so the extra bottom would be padding
                // with nothing to balance.
                // A29 rule 11: **the card never outgrows the room.** The host hands it the canvas area as
                // its max height; when the rows are taller than that, they scroll up and down here and
                // nothing is cut off and unreachable. Never sideways. The padding is INSIDE the scroll so
                // the corner tick and the rest shadows of the first and last rows are not clipped at rest.
                modifier = Modifier
                    .verticalScroll(scroll)
                    .width(IntrinsicSize.Max)
                    .padding(horizontal = ZinelyV21Dimens.gapLg, vertical = ZinelyV21Dimens.gapMd),
                // `.typebar{gap:var(--gap-md)}` — 12dp, and **it is a touch-target measurement, not a
                // rhythm choice** (spec note dated 2026-08-15). A device dump reported Bold/Italic at
                // 48.0 x 46.1dp: the row below them is the 30dp [Swatch], whose input-layer expansion
                // reaches 9dp past its paint, which is further than an 8dp gap. The Colour row claims that
                // strip first (the pruning walk visits siblings in reverse) and the Style row's granted
                // 48dp target is cut back into its own paint. At 12dp the pot's reach lands inside the gap
                // and every row keeps its full target. Raising the toggles to 48dp would NOT have fixed
                // it — the strip is taken from whatever paint is there.
                verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapMd),
            ) {
                // A29 rule 8: `.tywhy{margin-top:calc(-1 * var(--gap-xs))}` — the line sits 4dp closer to
                // its row than a row sits to a row, so it reads as the Font row's and not as a sixth row.
                Column(verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapMd - ZinelyV21Dimens.gapXs)) {
                    TypeRow(Copy.Type.ROW_FONT) {
                        FontSegment(
                            voice = font.voice,
                            bookCant = font.bookBlock != null,
                            bookSpoken = font.bookBlock?.let { Copy.Type.unavailable(Copy.Type.FONT_BOOK, fontReasonLine(it)) }
                                ?: Copy.Type.FONT_BOOK,
                            onVoice = { voice ->
                                val blocked = font.bookBlock.takeIf { voice == DocumentVoice.BOOK }
                                when {
                                    // A29 rule 6: the word already chosen does nothing and adds no step.
                                    voice == font.voice -> Unit
                                    // A29 rule 7: a tap changes nothing and says the reason once more.
                                    blocked != null ->
                                        onAnnounce(Copy.Type.unavailable(Copy.Type.FONT_BOOK, fontReasonLine(blocked)))
                                    else -> {
                                        // A size burst still settling is committed first, as its own step,
                                        // so the font step changes ONLY the font and Undo can say "Font put
                                        // back" (A30). It also means Book is never applied to a size the
                                        // document does not hold yet.
                                        pendingSizeIndex?.let {
                                            dispatch(Intent.StyleText(id = id, sizePt = TypeSizesPt[it]))
                                            pendingSizeIndex = null
                                        }
                                        dispatch(Intent.StyleText(id = id, fontFamily = voice.familyName))
                                        buzz()
                                        onAnnounce(
                                            Copy.Type.fontAnnouncement(
                                                if (voice == DocumentVoice.BOOK) Copy.Type.FONT_BOOK else Copy.Type.FONT_PLAIN,
                                            ),
                                        )
                                    }
                                }
                            },
                        )
                    }
                    if (fontLine != null) FontReasonLine(fontLine)
                }
                TypeRow(Copy.Type.ROW_SIZE) {
                    SizeStepper(
                        index = sizeIndex,
                        // A29 rule 9: Smaller on a Book text at Book's smallest size. Drawn flat, still
                        // tappable; the reason is the line already showing under the Font row.
                        smallerCant = font.smallerBlocked,
                        smallerSpoken = if (font.smallerBlocked) {
                            Copy.Type.unavailable(Copy.Type.SMALLER, fontReasonLine(FontReason.BookAtFloor))
                        } else {
                            Copy.Type.SMALLER
                        },
                        onStep = { dir ->
                            val next = (sizeIndex + dir).coerceIn(0, TypeSizesPt.lastIndex)
                            if (dir < 0 && font.smallerBlocked) {
                                onAnnounce(Copy.Type.unavailable(Copy.Type.SMALLER, fontReasonLine(FontReason.BookAtFloor)))
                            } else if (next != sizeIndex) {
                                pendingSizeIndex = next
                                buzz()
                                onAnnounce(Copy.Type.sizePointAnnouncement(TypeSizesPt[next].toInt()))
                            }
                        },
                    )
                }
                TypeRow(Copy.Type.ROW_ALIGN) {
                    AlignSegment(
                        align = style.align,
                        onAlign = { al ->
                            dispatch(Intent.StyleText(id = id, align = al))
                            buzz()
                            onAnnounce(
                                when (al) {
                                    TextAlign.START -> Copy.Type.LEFT_ALIGNED
                                    TextAlign.CENTER -> Copy.Type.CENTERED
                                    TextAlign.END -> Copy.Type.RIGHT_ALIGNED
                                },
                            )
                        },
                    )
                }
                TypeRow(Copy.Type.ROW_STYLE) {
                    // `.tytog{gap:var(--gap-sm)}`. This was a bare `6.dp` — a literal with no source, in
                    // the one cluster of the four that did not use the token its siblings all use.
                    Row(horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm)) {
                        // Both toggles route through the shared verb the Ctrl/Cmd+B/I shortcuts also call,
                        // so the pointer and keyboard paths are one implementation (ADR-055 §4). The
                        // toggleable `on` is ignored: the verb re-reads `element.style`, the same flip.
                        StyleToggle(Copy.Type.STYLE_BOLD, "B", style.bold, FontWeight.Bold, FontStyle.Normal) {
                            toggleBold(element, dispatch, onAnnounce, buzz)
                        }
                        StyleToggle(Copy.Type.STYLE_ITALIC, "I", style.italic, FontWeight.Normal, FontStyle.Italic) {
                            toggleItalic(element, dispatch, onAnnounce, buzz)
                        }
                    }
                }
                TypeColourRow(Copy.Type.ROW_COLOUR) {
                    InkRow(
                        color = style.color,
                        onInk = { ink ->
                            dispatch(Intent.StyleText(id = id, color = ink.rgba))
                            buzz()
                            onAnnounce(Copy.Type.colourAnnouncement(ink.label))
                        },
                    )
                }
            }
        }
    }
}

/*
 * A NOTE ON HOW THESE CONTROLS LOOK IN THE PLATFORM ACCESSIBILITY TREE — so nobody "fixes" it twice.
 *
 * Read on a physical device (Galaxy A17, Android 16) during the ADR-055 gate, every Type bar control
 * except the swatches dumps like this:
 *
 *     View      desc=''      click=true  focusable=true    <- the node a screen reader focuses
 *       View    desc='Bold'  click=false                   <- the label
 *       CheckBox desc=''     click=false                   <- the role
 *
 * The label and the click action sit on *different* nodes, which looks alarming — an unlabelled
 * interactive node is exactly the IF5 lesson §4 cites. It is not that. This is simply how Compose
 * exposes any clickable that has child content: the touch-target expansion supplies the focusable
 * wrapper and the content stays beneath it. `Add a photo`, `Add words` and the page strip dump in the
 * identical shape, and **those passed an on-device TalkBack pass by ear** (M6/F3, verdict GO), so the
 * shape is known-good on this app, on this device. TalkBack focuses the wrapper and takes the name from
 * the subtree. [Swatch] differs only because it has no child to split off, not because it is more correct.
 *
 * Reordering the modifiers to force the label onto the wrapper was tried and measured: the emitted tree
 * is byte-identical, so it changes nothing and was reverted rather than left in place asserting a fix
 * that is not one.
 *
 * The reason to write this down: the machine tests cannot see any of it — `onNodeWithContentDescription`
 * reads the *merged* tree and passes under every arrangement — so the only way to know is to dump the
 * platform tree on a device, and the only way to know it is *fine* is to have heard it. Both are now on
 * the record; don't spend the afternoon rediscovering it.
 */

/**
 * One labelled Type bar row (bench `.tyrow` + `.tylab`): a soft caption, then the control cluster.
 *
 * **Invariant — the four rows share one right edge.** Bench spells it `justify-content:space-between`
 * inside a `width:max-content`/`align-items:stretch` card: label pinned left, control cluster pinned
 * right, on a grid common to all four rows. That needs `fillMaxWidth` here AND `IntrinsicSize.Max` on the
 * caller's Column — neither works alone. The previous `Arrangement.spacedBy(14.dp, Alignment.End)` was
 * inert for exactly this reason: a wrap-content Row has no free space to align within.
 *
 * The bench `gap:14px` rides as the label's end padding rather than as an `Arrangement` spacing, because
 * `SpaceBetween` reports **zero** spacing to the intrinsic-width query above. An `Arrangement`-borne gap
 * would therefore be measured out of the card's max-content width — shrinking it 14dp and letting the
 * widest row's label butt against its control. As padding it is measured into the label, so the intrinsic
 * stays honest and the gap keeps the CSS meaning it has under `space-between`: a floor, exactly 14dp on
 * the widest row and wider on the rest.
 */
@Composable
private fun TypeRow(label: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeRowLabel(
            label = label,
            modifier = Modifier.padding(end = ZinelyV21Dimens.gapLg),
        )
        control()
    }
}

/**
 * Colour is the only five-choice row. Stacking its caption above one 48dp line keeps every ink visible
 * without making the transient card as tall as the earlier 3+2 grid.
 */
@Composable
private fun TypeColourRow(label: String, control: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapXs),
    ) {
        TypeRowLabel(label = label)
        control()
    }
}

@Composable
private fun TypeRowLabel(label: String, modifier: Modifier = Modifier) {
    Text(
            // `text-transform:uppercase` is a *rendering* transform: the string stays the shipped "Size" /
            // "Align" / "Style" / "Colour" and a screen reader reads it in its own case. Compose has no
            // such transform — uppercasing the string uppercases what TalkBack speaks too — so the case is
            // split explicitly, display here and spoken in the `semantics` below. `Locale.ROOT`, not the
            // default locale: a Turkish device would otherwise render "Sıze" from a dotted i.
            text = label.uppercase(Locale.ROOT),
            // `.tylab{min-width:46px}` — a floor, not a fixed width: under `space-between` a fixed width
            // would clip a longer label instead of widening the label column, and the spec says min-width.
            // Padding sits outside the floor so the row's gap is additive to it.
            modifier = modifier
                .widthIn(min = 46.dp)
                .semantics { contentDescription = label },
            // This card sits on the room-space dark surface in dark mode. `inkSoft` becomes muddy at the
            // small all-caps size; primary room ink clears AA while case and weight keep the caption quiet.
            color = ZinelyTheme.v21Colors.ink,
            // `.tylab{font-size:.6rem;font-weight:700;letter-spacing:.13em}` — `.inklbl`
            // (`v21-bench.html:247-248`) verbatim: the corpus's own row label INSIDE a floating card,
            // which is exactly what these four are. `.inklbl`'s `margin` is the one thing dropped, this
            // being a flex row rather than a stacked popover section. V1 drew them at 11sp/.02em sentence
            // case — the V1 caption scale, which has no V2.1 counterpart.
            fontSize = 9.6.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.13.em,
        )
}

/**
 * The size stepper (bench `.tysize`): − · readout · + , mirroring the Reframe zoom stepper idiom that
 * ADR-055 §4 names as its precedent.
 *
 * **A declared divergence from the frozen HTML, like the alignment group (ADR-055 §4/§5):** bench marks
 * the readout `aria-hidden="true"`, treating it as decoration the live announcement already covers.
 * Here it carries its own `contentDescription` under [clearAndSetSemantics] — ADR-055 §5 requires the
 * size to be *readable*, not only *announced*, so a user who arrives at the stepper after the
 * announcement has passed can still find the current value. The `clearAndSetSemantics` keeps it a
 * single flat node rather than a traversable text run.
 */
@Composable
private fun SizeStepper(
    index: Int,
    smallerCant: Boolean,
    smallerSpoken: String,
    onStep: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(
            Icons.Filled.Remove,
            Copy.Type.SMALLER,
            enabled = index > 0,
            cant = smallerCant,
            spoken = smallerSpoken,
        ) { onStep(-1) }
        Text(
            text = Copy.Type.sizePtLabel(TypeSizesPt[index].toInt()),
            // `min-width:58px`, not `width` — the same correction `.tyval` states over `.zoom b`'s 46:
            // 58dp is the box the settling burst must not shift inside, and a *floor* survives a font
            // scale that a fixed width clips.
            modifier = Modifier
                .widthIn(min = 58.dp)
                .clearAndSetSemantics { contentDescription = Copy.Type.sizePointAnnouncement(TypeSizesPt[index].toInt()) },
            textAlign = ComposeTextAlign.Center,
            // `.tyval{font-size:.78rem;font-weight:700;color:var(--ink-soft)}` — a number, so it is quiet
            // and tabular; room ink is required here because the dark-theme surface is not paper.
            fontSize = 12.48.sp,
            fontWeight = FontWeight.Bold,
            color = ZinelyTheme.v21Colors.ink,
            // `font-variant-numeric:tabular-nums`. Not cosmetic: the readout is a centred number that
            // changes on every tap, so proportional digits shift the glyph run mid-burst. Tabular figures
            // hold the centre still; the `min-width` alone does not stop the digits dancing. Carried on
            // `style` because `Text` has no `fontFeatureSettings` parameter — the card's provided style is
            // the base, so the `--sans` family it sets is preserved.
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
        )
        StepButton(Icons.Filled.Add, Copy.Type.LARGER, enabled = index < TypeSizesPt.lastIndex) { onStep(1) }
    }
}

/**
 * A 40dp stepper button — `.tysize button`, which the spec declares IS the Reframe pad's `.zoom button`
 * (`v21-reframe.html:192-196`), the control ADR-055 §4 already named as this one's precedent. So it is
 * built as [ZoomButton] is built, down to the disabled convention: `opacity:.35` on the WHOLE chip and the
 * shadow dropped, so a control that cannot act does not sit proud of the card. V1 faded to `.4`, which is
 * the same gesture at a V1 number.
 *
 * **One box: the 40dp chip IS the control.** The chip carries the click, the label and the paint, exactly
 * as [Swatch] (30dp) and [StyleToggle] (46dp) do. The ≥48dp touch target is not a layout box — Compose
 * expands any clickable under `ViewConfiguration.minimumTouchTargetSize` at the *input* layer, so
 * `touchBoundsInRoot` reports 48dp off a 40dp paint for free. DESIGN-RULES 1 + 7: grow the target, never
 * the design — and a layout box IS the design, because it is what the row's gaps measure from.
 *
 * ponytail: no `minimumInteractiveComponentSize` and not the house [zinelyControl] helper (which bundles
 * one). That modifier only *reserves layout space* (its own KDoc: "not needed for touch target expansion
 * to happen. It only affects layout") — and reserving it here is precisely the parity defect: a 48dp box
 * around a 40dp chip insets the paint 4dp, painting `.tysize{gap:var(--gap-sm)}` as 12dp and standing the
 * Size row's painted right edge 4dp inside the shared row edge that `.tyrow{justify-content:space-between}`
 * fixes. The stepper's only neighbour is the non-clickable readout, so the expanded targets overlap
 * nothing and both buttons still reach TalkBack at a full 48x48.
 *
 * ⚠ **The glyph is an [Icon], not a `Text`, and that is an accessibility fix rather than a visual choice.**
 * This is the one control in the card that is ever *disabled*, and a `Text` child contributes semantics of
 * its own that stop the chain collapsing: the platform then receives the click and its `disabled` flag on
 * one node and the spoken label on another, TalkBack lands on the labelled one, and a disabled stepper
 * announces itself as available. That exact defect was measured on the sibling `ReframeControls.ZoomButton`
 * and is written up there; `Icon(contentDescription = null)` contributes nothing, so the whole control
 * collapses to one `android.widget.Button` carrying label, role and disabled state together. Compose's own
 * test tree cannot see it — `assertIsNotEnabled` passes under either arrangement.
 *
 * The spec's `.tysize button` declares `font-family`/`font-size`/`font-weight` for a text `&minus;`/`+`
 * where the `.zoom button` it cites uses an `<svg>`; that is an undeclared divergence in the proposal, and
 * this file resolves it toward the cited source. `v21-typebar.html` needs the same correction before it is
 * frozen.
 *
 * **[cant] is not [enabled] = false (A29 rule 9, ADR-126).** Smaller on a Book text at Book's smallest
 * size cannot be used, but it stays enabled and clickable so a tap can say why: `.tysize button.cant` is
 * flat with a hairline edge and a quieter glyph, never the `.35` fade. [spoken] then carries the label,
 * "unavailable" and the reason. The real bottom of the ramp stays truly disabled and faded, as frozen.
 */
@Composable
private fun StepButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    cant: Boolean = false,
    spoken: String = description,
    onClick: () -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = BenchBarShape
    Box(
        modifier = Modifier
            .testTag("$TypeBarTestTag-$description")
            .size(40.dp)
            // `.tysize button:disabled{opacity:.35}` fades the WHOLE chip — edge, fill and glyph — not
            // just the glyph. Group opacity (CompositingStrategy.Auto), like CSS; measure-transparent, and
            // ahead of the paint modifiers so the layer wraps them all (ADR-055 §8).
            .alpha(if (enabled) 1f else ZinelyV21Dimens.disabledAlpha)
            .then(
                // `.tysize button.cant{box-shadow:none}` — a button that cannot be used lies flat, like
                // the truly disabled one, but is NOT faded: it can still be reached and tapped (A29 rule 9).
                if (enabled && !cant) {
                    Modifier.zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, shape)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(colors.surface)
            .border(BenchChromeBorder, if (cant) colors.hair else colors.ink, shape)
            .clickable(
                interactionSource = interaction,
                // indication = null, as [zinelyControl] does: the chip has no ripple, and a default ripple
                // here spins the measure/draw loop when the bar is disposed mid-press.
                indication = null,
                enabled = enabled,
                // `role` on the clickable itself, not in a trailing `semantics {}` block — the other half
                // of the [ZoomButton] finding.
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = spoken },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (cant) colors.inkSoft else colors.ink, modifier = Modifier.size(20.dp))
    }
}

/**
 * The Font row — `.tyvoice` (frozen `v21-typebar.html` A29, ADR-126). Two word buttons, Book and Plain:
 * one choice of two, so a single-choice group **named by its label** (A29 rules 1 and 14: the row follows
 * the frozen page on the two points returned to the owner for Align and Colour, which name no group in the
 * app today; if the owner rules the other way for them, this row follows that ruling).
 *
 * Built the way [AlignSegment] is built, not from Material 3's `SegmentedButton`, which is being
 * deprecated (RESEARCH R24.7).
 *
 * [voice] is `null` for a family this build does not know: then neither word is chosen, as [InkRow] shows
 * none chosen for an ink it does not offer (A29 rule 10).
 */
@Composable
private fun FontSegment(
    voice: DocumentVoice?,
    bookCant: Boolean,
    bookSpoken: String,
    onVoice: (DocumentVoice) -> Unit,
) {
    Row(
        modifier = Modifier
            .testTag(TypeBarFontGroupTestTag)
            .selectableGroup()
            .semantics { contentDescription = Copy.Type.ROW_FONT },
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
    ) {
        // A29 rule 2: each word in its own face, from the faces the INTERFACE already has. No document
        // font file is loaded into the interface: Fraunces Medium is V2's `fraunces_medium`, Inter
        // SemiBold is the card's own `--sans`.
        FontOption(
            label = Copy.Type.FONT_BOOK,
            spoken = bookSpoken,
            family = ZinelyV2Fonts.Voice,
            weight = FontWeight.Medium,
            chosen = voice == DocumentVoice.BOOK,
            cant = bookCant,
            onClick = { onVoice(DocumentVoice.BOOK) },
        )
        FontOption(
            label = Copy.Type.FONT_PLAIN,
            spoken = Copy.Type.FONT_PLAIN,
            family = ZinelyV21Fonts.Work,
            weight = FontWeight.SemiBold,
            chosen = voice == DocumentVoice.PLAIN,
            cant = false,
            onClick = { onVoice(DocumentVoice.PLAIN) },
        )
    }
}

/**
 * One font word — `.tyvoice button`: [AlignOption]'s clothes (A29 rule 3: the 46dp floor, the card's
 * surface, an ink edge, the 2dp rest shadow, leaf under on-leaf when chosen), plus three things of its own.
 *
 *  - **The corner tick (A29 rule 4).** The leaf fill against the card is 2.21:1 by day, under the 3:1 a
 *    state needs when fill is its only sign, so the chosen word also carries the Colour row's paper tick
 *    on its corner, where it costs no width. It sits OUTSIDE the button's clip, which is why the button is
 *    wrapped: the clip belongs to the inner box.
 *  - **The word never wraps (A29 rule 5)**, at any text size.
 *  - **[cant] (A29 rule 7): a word that cannot be used.** It is NOT disabled. It lies flat (no rest
 *    shadow), its edge thins to the hairline and its word drops to `inkSoft`; it is never faded like the
 *    stepper's ends. To the platform it is enabled, clickable and not checked, and [spoken] carries the
 *    word, "unavailable" and the reason. A tap changes nothing and says the reason once more.
 *
 * ⚠ The semantics are [AlignOption]'s for the reason written there: a `Text` child splits the node, and a
 * service reads the node with the label, so everything is cleared and re-declared on the one clickable
 * node. That is also what keeps the unavailable word honest in the PLATFORM tree, which is the
 * `ReframeControls.ZoomButton` trap: [TypeBarFontRowTest] reads it there, not here.
 *
 * The touch target is 48dp both ways off the 46dp paint, expanded at the input layer as for every other
 * control in the card (see [StepButton]); [TypeBarFontRowTest] asserts it.
 */
@Composable
private fun FontOption(
    label: String,
    spoken: String,
    family: FontFamily,
    weight: FontWeight,
    chosen: Boolean,
    cant: Boolean,
    onClick: () -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusMd)
    Box {
        Box(
            modifier = Modifier
                .testTag("$TypeBarTestTag-font-$label")
                .defaultMinSize(minWidth = 46.dp, minHeight = 46.dp)
                // A29 rule 14: this row draws the keyboard focus ring the frozen page draws.
                .zinelyFocusRing(interaction, ZinelyV21Dimens.radiusMd)
                .then(
                    // `.tyvoice button.cant{box-shadow:none}` and `.cant:active{transform:none}`.
                    if (cant) Modifier else Modifier.zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, shape),
                )
                .clip(shape)
                .background(if (chosen) colors.leaf else colors.surface)
                .border(BenchChromeBorder, if (cant) colors.hair else colors.ink, shape)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = spoken
                    role = Role.RadioButton
                    selected = chosen
                    onClick { onClick(); true }
                }
                // `.tyvoice button{padding:var(--gap-sm) var(--gap-md)}`.
                .padding(horizontal = ZinelyV21Dimens.gapMd, vertical = ZinelyV21Dimens.gapSm),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = when {
                    chosen -> colors.onLeaf
                    cant -> colors.inkSoft
                    else -> colors.ink
                },
                fontFamily = family,
                fontSize = 12.48.sp,
                fontWeight = weight,
                maxLines = 1,
                softWrap = false,
            )
        }
        if (chosen) {
            EditorSelectionCue(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = FontCueOutset, y = -FontCueOutset)
                    .testTag(selectionCueTag("$TypeBarTestTag-font-$label")),
                contentColor = colors.onLeaf,
                borderColor = colors.onLeaf,
                size = FontCueSize,
                glyphSize = FontCueGlyphSize,
            )
        }
    }
}

/**
 * The one reason line — `.tywhy` (A29 rule 8). Shown without a tap for as long as its cause lasts, so a
 * sighted maker is never left to guess why a word is quiet. Plain words in `ink`, `.72rem` at 1.35.
 *
 * **It takes the card's width and never sets it** (`width:0;min-width:100%`, A29 rule 5). The card is
 * `width(IntrinsicSize.Max)`, and a `Text`'s max intrinsic width is its whole sentence on one line, which
 * would blow the card out to the screen the moment the line appeared. So this reports no intrinsic width
 * at all and is then measured at whatever width the five rows settled on; the card only grows taller.
 *
 * Not a live region: the reason is part of the unavailable choice's own spoken label, so it is heard when
 * the choice is reached and not announced again on every pass (Brief 02). It is still an ordinary text
 * node a screen reader can read in place.
 */
@Composable
private fun FontReasonLine(text: String) {
    Layout(
        content = {
            Text(
                text = text,
                color = ZinelyTheme.v21Colors.ink,
                fontSize = 11.52.sp,
                lineHeight = 1.35.em,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag(TypeBarFontReasonTestTag),
            )
        },
        modifier = Modifier.fillMaxWidth(),
        measurePolicy = TakesWidthNeverSetsIt,
    )
}

private object TakesWidthNeverSetsIt : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val line = measurables.single().measure(constraints.copy(minHeight = 0))
        return layout(line.width, line.height) { line.place(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int = 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int = 0

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        measurables.single().minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        measurables.single().maxIntrinsicHeight(width)
}

/**
 * Alignment — `.tyalign`. **No longer a joined segmented control**, and that is the one structural change
 * in this re-skin rather than a repaint.
 *
 * V1 drew a single box with hairline dividers and filled the chosen third with coral under white. V2.1 has
 * no joined segment anywhere and no coral at all; what it *does* have for "one choice of N, spelled out in
 * words" is the Proof's `.paperseg` — separate `--paper` buttons on a `--gap-sm` gap, the chosen one
 * `--leaf` under `--on-leaf`. Same three options, same three words, same radio semantics. The dividers go
 * with the box that needed them.
 *
 * **The deliberate divergence from the frozen HTML (ADR-055 §4) is unchanged:** the prototype spells
 * alignment as three independent `aria-pressed` buttons, which would let a screen-reader user believe all
 * three can be pressed at once. Alignment is one choice of three, so the Compose semantic layer says so —
 * [selectableGroup] + [Role.RadioButton]. Only the *semantics* diverge; the paint is the spec's.
 */
@Composable
private fun AlignSegment(align: TextAlign, onAlign: (TextAlign) -> Unit) {
    Row(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
    ) {
        AlignOption(Copy.Type.ALIGN_LEFT, TextAlign.START, align, onAlign)
        AlignOption(Copy.Type.ALIGN_CENTER, TextAlign.CENTER, align, onAlign)
        AlignOption(Copy.Type.ALIGN_RIGHT, TextAlign.END, align, onAlign)
    }
}

/**
 * One alignment button — `.tyalign button`, 46dp floor, `--br-md`, an ink edge in both states.
 *
 * The press tier is [ZinelyV21Press.Flat] (2dp rest, flush when pressed) and **not** `.paperseg`'s own
 * 3dp: `.paperseg` rests in a drawer, this rests in a floating card, and depth in V2.1 is assigned by
 * where a control lives rather than by which rule it was copied from. Every control in this card carries
 * the same tier for that reason — the Reframe pad's, the nearest case by *situation*.
 */
@Composable
private fun AlignOption(label: String, value: TextAlign, current: TextAlign, onAlign: (TextAlign) -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val isSel = value == current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusMd)
    Box(
        modifier = Modifier
            .testTag("$TypeBarTestTag-align-$label")
            // ponytail: 46dp, the spec's `.tyalign button` floor, matching the shipped ReframeControls
            // segment. NOT lifted with minimumInteractiveComponentSize — see [Swatch]: that modifier would
            // only widen the *layout*, pushing the card past the `max-width`, while the 48dp touch target
            // it looks like it buys is already applied at the input layer without it.
            // Widen by growing the button in `v21-typebar.html` first if this ever needs to be 48.
            .defaultMinSize(minWidth = 46.dp, minHeight = 46.dp)
            .zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, shape)
            .clip(shape)
            .background(if (isSel) colors.leaf else colors.surface)
            // The edge stays `--ink` in both states, as every V2.1 control's does. V1 swapped the border to
            // coral when on and so drew the "on" state twice.
            .border(BenchChromeBorder, colors.ink, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = { onAlign(value) })
            // ⚠ `clearAndSetSemantics`, re-declaring everything — NOT `selectable` + a merging
            // `semantics`. A device dump is what settled this, and the old arrangement failed it:
            //
            //     Left   android.view.View   clickable=false   checked=false
            //
            // The `Text` child contributes semantics of its own, so the merge splits the control and the
            // node carrying the LABEL — the node a service reads — kept neither the radio role, nor the
            // click action, nor the chosen state. The swatches in the same card reach the platform as
            // `android.widget.RadioButton, clickable=true` because [Swatch] has no child at all, and the
            // steppers do because their glyph is an `Icon(contentDescription = null)`. This row cannot
            // drop its child: the words ARE the control.
            //
            // So it takes `ReframeControls.FitChip`'s shape instead — clear the subtree and state the
            // four properties explicitly. An earlier version of this comment warned that
            // `clearAndSetSemantics` "would drop the click and the group's selection state"; it does, and
            // then you put them back, which is the half that was missing.
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.RadioButton
                selected = isSel
                onClick { onAlign(value); true }
            }
            .padding(horizontal = ZinelyV21Dimens.gapSm, vertical = ZinelyV21Dimens.gapSm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isSel) colors.onLeaf else colors.ink,
            fontSize = 12.48.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Bold / Italic — `.tytog button`. Independent toggles, not a group (ADR-055 §4): bold and italic genuinely
 * compose, so [Role.Checkbox] — the Compose idiom for `aria-pressed`, and what Material's own icon toggles
 * use — is the honest reading. The glyph wears the style it applies.
 *
 * Three things are taken from three places, which is why the spec spells them out rather than naming one
 * ancestor: the **colour rule** is the corpus's two-state `.chip2` (`--paper`/`--ink-soft` at rest,
 * `--leaf`/`--on-leaf` when on); the **geometry** is the shipped 46dp box, kept because it is one of the
 * two things the card's shared right edge is measured from and a re-skin does not move measurements; and
 * the **2dp rest shadow** is neither of those — `.chip2` has none at all, being a flat chip lying on the
 * desk, while everything in this card has left the surface. No new press tier, no new radius.
 *
 * A `Text` glyph is correct here where it is not in [StepButton]: these toggles are never disabled, so
 * there is no disabled bit for the split node to lose, and this exact shape was read on a device and heard
 * under TalkBack during the ADR-055 gate (the note above [TypeRow]).
 */
@Composable
private fun StyleToggle(
    label: String,
    glyph: String,
    on: Boolean,
    weight: FontWeight,
    fontStyle: FontStyle,
    onToggle: (Boolean) -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusMd)
    Box(
        modifier = Modifier
            .testTag("$TypeBarTestTag-${label.lowercase()}")
            // 46dp, the `.tytog button` floor — the same call [AlignOption] makes above, for the same
            // reason (see [Swatch]).
            .defaultMinSize(minWidth = 46.dp, minHeight = 46.dp)
            .zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, shape)
            .clip(shape)
            .background(if (on) colors.leaf else colors.surface)
            // `--ink` in both states. V1 swapped the border to coral when on, drawing the "on" state twice.
            .border(BenchChromeBorder, colors.ink, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = { onToggle(!on) })
            // The same device finding as [AlignOption], on the same shape: with a `toggleable` and a
            // merging `semantics`, the `B` and `I` glyphs split the node and the platform received
            // `Bold  android.view.View  clickable=false  checked=false` — no checkbox role, no click
            // action, and no on/off state, which is the whole of what this control says. Cleared and
            // re-declared, and `toggleableState` rather than `selected` because a checkbox's platform
            // state is `isChecked`.
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Checkbox
                toggleableState = ToggleableState(on)
                onClick { onToggle(!on); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            fontSize = 16.8.sp,
            fontWeight = weight,
            fontStyle = fontStyle,
            color = if (on) colors.onLeaf else colors.ink,
        )
    }
}

/**
 * The text colour row (bench `.tyinks`) — **text only**. The prototype's image spot-ink popover is a
 * different surface with a different (4-entry) palette and is explicitly not in this batch.
 *
 * Single-select, so it reads as a radio group for the same reason alignment does: a box has exactly one
 * ink. A swatch whose RGBA is not one of the five (a document from elsewhere) simply shows none selected.
 *
 * The 2026-08-29 hardware refinement keeps one visible line of explicit 48dp tiles. The Colour caption
 * sits above it, so the transient card stays shorter while every ink remains visible and the chosen ink
 * keeps its non-colour cue.
 */
@Composable
private fun InkRow(color: ColorRgba, onInk: (TextInk) -> Unit) {
    Row(
        modifier = Modifier
            .testTag(TypeBarInkRowTestTag)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(SwatchGridGap),
    ) {
        TextInk.entries.forEach { ink -> Swatch(ink = ink, selected = ink.rgba == color, onInk = onInk) }
    }
}

/**
 * A 48dp ink tile carrying a 30dp physical pot — the 2026-08-29 compactness ruling taken in
 * `v21-typebar.html`. The chosen state is said twice on purpose: the corpus's dashed pot ring stays, and a
 * small paper check lands in the corner so the state survives greyscale and low-colour perception without
 * changing what any ink will print.
 *
 * ⚠ **The fill is a model value, not a token, and the ink edge is why that is safe.** Every pot's fill is
 * now the paper-space RGBA [TextInk] commits — printed ink, theme-independent by ADR-055 Decision 6 — and
 * it must not resolve through a themed token, or a zine styled at night would print differently from one
 * styled at noon. V1 painted the Coral swatch through `ZinelyTheme.colors.coralText`, a themed lookup on
 * the one control whose whole contract is that it is not themed; that is what changed here. The 1.5dp
 * `--ink` ring is what separates any fill from the card's `--surface` ground whatever the fill becomes,
 * which is the reasoning `v21-bench.html:274-278` already records for `.chip .sw`.
 *
 * **What the ring does not do:** it guarantees every pot is *found*, not that two pots are told *apart*.
 * The physical ink values cannot move with theme (ADR-055 D6); the label and selection ring carry the
 * state while the themed surface keeps the surrounding controls readable.
 *
 * **No `minimumInteractiveComponentSize` — the visible 48dp tile already is the target.** The modifier
 * does not create the target; it only reserves *layout* space for one ("This modifier is not needed for
 * touch target expansion to happen. It only affects layout" — its own KDoc). The target comes from the
 * clickable tile itself, and the input layer would still expand it to `ViewConfiguration.minimumTouchTargetSize`
 * if the visible control were smaller. [TypeBarTest] asserts the platform node the service actually reads.
 *
 * **The earlier overlap defect stays recorded as history, not as the current geometry.** V1's 40dp pitch and
 * V2.1's first 38dp pitch let neighbouring 48dp expansions collide, so Compose pruned the overlap before
 * reporting bounds to accessibility. The 2026-08-29 ruling stops borrowing that hidden expansion: each tile
 * is explicitly 48dp, the 30dp pot remains centered inside it, and the selected cue is visible rather than
 * implied by spacing arithmetic.
 *
 * **Confirmed on device 2026-08-15** (SM-A176B, Android 16, density 420): the shipped row already reached a
 * full 48.0 × 48.0dp per swatch in the platform tree. The stacked-caption row preserves that floor while
 * shortening the card and making the reachable area match the visible tile.
 *
 * `TypeBarSwatchPlatformA11yTest` asserts this on the **platform** tree, which is the only tree that can
 * fail: `touchBoundsInRoot` is the *pre*-pruning value and reported a flat 48dp all through the defect.
 *
 * Reserving the modifier here was a real layout bug, not a harmless belt-and-braces: it answers the
 * `IntrinsicSize.Max` query above (it overrides `measure`, not the intrinsics, so the default
 * `LayoutModifierNode` intrinsics re-run `measure` and return the inflated 48dp). Five swatches at 48dp
 * instead of 32 made the Colour row the widest by 80dp and blew the card out to exactly 360dp — over
 * `max-width:calc(100% - 24px)`, and edge-to-edge on a 360dp phone.
 */
@Composable
private fun Swatch(ink: TextInk, selected: Boolean, onInk: (TextInk) -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val paint = Color(ink.rgba.r, ink.rgba.g, ink.rgba.b, ink.rgba.a)
    val shape = BenchBarShape
    Box(
        modifier = Modifier
            .testTag("$TypeBarTestTag-ink-${ink.label}")
            .size(SwatchTouchSize)
            // `.pot[aria-checked="true"]::after{inset:-5px;border:1.6px dashed}` — an OUTER ring, so it is
            // drawn behind rather than inset into the 30dp box. The pot's own size is unchanged by
            // selection, which is what keeps the row from reflowing as the choice moves along it.
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = { onInk(ink) },
            )
            .semantics { contentDescription = ink.label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(SwatchSize)
                .drawBehind {
                    if (!selected) return@drawBehind
                    val out = SwatchRingInset.toPx()
                    val w = SwatchRingStroke.toPx()
                    val edge = -out + w / 2f
                    val ringW = size.width + 2f * out - w
                    val ringH = size.height + 2f * out - w
                    val dash = w * 2f
                    drawRoundRect(
                        color = colors.ink,
                        topLeft = Offset(edge, edge),
                        size = Size(ringW, ringH),
                        cornerRadius = CornerRadius(ringH / 2f, ringH / 2f),
                        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
                    )
                }
                .clip(shape)
                .background(paint)
                .border(BenchChromeBorder, colors.ink, shape),
        )
        if (selected) {
            EditorSelectionCue(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = SwatchCueInset, end = SwatchCueInset)
                    .testTag(selectionCueTag("$TypeBarTestTag-ink-${ink.label}")),
            )
        }
    }
}

/** `.typebar.a29` scroll shade: `22px` tall on the edge that hides more. */
private val TypeBarScrollShade = 22.dp

/** `.tyvoice button[aria-checked="true"]::after{top:-7px;right:-7px;width:18px;height:18px}`, an 11px tick. */
private val FontCueSize = 18.dp
private val FontCueGlyphSize = 11.dp
private val FontCueOutset = 7.dp

/** `.pot{width:30px;height:30px}`. */
private val SwatchSize = 30.dp

/** The compact colour-tile footprint chosen on 2026-08-29: the visible target is the real target. */
private val SwatchTouchSize = 48.dp

/** A compact visible gap: every tile itself remains a full 48dp target. */
private val SwatchGridGap = ZinelyV21Dimens.gapXs

/** The paper check cue sits just inside the 48dp tile. */
private val SwatchCueInset = 4.dp

/** `.pot[aria-checked="true"]::after{inset:-5px}` — how far the ring sits outside the pot. */
private val SwatchRingInset = 5.dp

/** `.pot[aria-checked="true"]::after{border:1.6px dashed}` — the selection ring's hand, [SelectionChrome]'s. */
private val SwatchRingStroke = SelectionOutlineStrokeDp
