package com.aritr.zinely.feature.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign as ComposeTextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.TextAlign
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.rememberReduceMotion
import kotlinx.coroutines.delay

/** Test tag on the Type bar surface; absent from the tree unless a single non-blank text box is selected. */
public const val TypeBarTestTag: String = "type-bar"

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
    Ink("Ink", ColorRgba(0x23, 0x20, 0x1C)),
    Coral("Coral", ColorRgba(0xA6, 0x3C, 0x22)),
    Teal("Teal", ColorRgba(0x2A, 0x9D, 0x8F)),
    Blue("Blue", ColorRgba(0x26, 0x46, 0x53)),
    Ochre("Ochre", ColorRgba(0x7A, 0x5E, 0x12)),
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
    announce(if (on) "Bold on" else "Bold off")
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
    announce(if (on) "Italic on" else "Italic off")
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
 * Align / bold / italic / colour each commit **instantly**: one tap, one [Intent.StyleText], one undo
 * step. Size is the exception (ADR-055 §3): the stepper coalesces a tap burst behind a
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
 *   `announceForAccessibility` drain — the same channel Reframe and the reducer's announcements use.
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

    Surface(
        modifier = modifier.testTag(TypeBarTestTag),
        // bench `.typebar`: a floating `--menu` card, hairline edge, 16px corners, lifted off the desk.
        shape = RoundedCornerShape(16.dp),
        color = ZinelyTheme.colors.menu,
        contentColor = ZinelyTheme.colors.onDesk,
        border = BorderStroke(1.dp, ZinelyTheme.colors.fieldEdge),
        shadowElevation = 6.dp,
    ) {
        // bench pins `font-family:var(--shell)` on the Type bar's own rules (`.tyval`, `.tytog button`,
        // `.fitseg button`) rather than leaving it to inheritance. Compose's inheritance goes the other
        // way: `MaterialTheme` ends in `ProvideTextStyle(typography.bodyLarge)`, and that scale is still
        // deliberately on `FontFamily.Default` until the last screen is reskinned (Type.kt), so an
        // unstyled `Text` here would paint Roboto — a face `--shell`'s stack lists only as a fallback the
        // app makes unreachable by bundling Inter. One provision covers every `Text` in the card, and
        // zeroes the 0.5sp tracking `bodyLarge` also carries (the frozen rules declare none; the label
        // sets its own 0.02em below).
        ProvideTextStyle(
            LocalTextStyle.current.copy(
                fontFamily = ZinelyTheme.typography.shell,
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
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TypeRow("Size") {
                    SizeStepper(
                        index = sizeIndex,
                        onStep = { dir ->
                            val next = (sizeIndex + dir).coerceIn(0, TypeSizesPt.lastIndex)
                            if (next != sizeIndex) {
                                pendingSizeIndex = next
                                buzz()
                                onAnnounce("Size ${TypeSizesPt[next].toInt()} point")
                            }
                        },
                    )
                }
                TypeRow("Align") {
                    AlignSegment(
                        align = style.align,
                        onAlign = { al ->
                            dispatch(Intent.StyleText(id = id, align = al))
                            buzz()
                            onAnnounce(
                                when (al) {
                                    TextAlign.START -> "Left aligned"
                                    TextAlign.CENTER -> "Centered"
                                    TextAlign.END -> "Right aligned"
                                },
                            )
                        },
                    )
                }
                TypeRow("Style") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Both toggles route through the shared verb the Ctrl/Cmd+B/I shortcuts also call,
                        // so the pointer and keyboard paths are one implementation (ADR-055 §4). The
                        // toggleable `on` is ignored: the verb re-reads `element.style`, the same flip.
                        StyleToggle("Bold", "B", style.bold, FontWeight.Bold, FontStyle.Normal) {
                            toggleBold(element, dispatch, onAnnounce, buzz)
                        }
                        StyleToggle("Italic", "I", style.italic, FontWeight.Normal, FontStyle.Italic) {
                            toggleItalic(element, dispatch, onAnnounce, buzz)
                        }
                    }
                }
                TypeRow("Colour") {
                    InkRow(
                        color = style.color,
                        onInk = { ink ->
                            dispatch(Intent.StyleText(id = id, color = ink.rgba))
                            buzz()
                            onAnnounce("Colour ${ink.label}")
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
        Text(
            text = label,
            // bench `.tylab{min-width:46px}` — a floor, not a fixed width: under `space-between` a fixed
            // width would clip a longer label instead of widening the label column, and the frozen spec
            // says min-width. Padding sits outside the floor so the row's 14dp gap is additive to it.
            modifier = Modifier
                .padding(end = 14.dp)
                .widthIn(min = 46.dp),
            color = ZinelyTheme.colors.onDeskSoft,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            // bench `.tylab{letter-spacing:.02em}` — the one Type bar rule that asks for tracking, so it
            // overrides the card-level zero rather than inheriting it.
            letterSpacing = 0.02.em,
        )
        control()
    }
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
private fun SizeStepper(index: Int, onStep: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", "Smaller", enabled = index > 0) { onStep(-1) }
        Text(
            text = "${TypeSizesPt[index].toInt()} pt",
            modifier = Modifier
                .width(58.dp)
                .clearAndSetSemantics { contentDescription = "Size ${TypeSizesPt[index].toInt()} point" },
            textAlign = ComposeTextAlign.Center,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            // bench `.tyval{font-variant-numeric:tabular-nums}`. Not cosmetic: the readout is a centred
            // number inside a fixed 58dp box that changes on every tap, so proportional digits shift the
            // glyph run mid-burst. Tabular figures hold the centre still. Carried on `style` because
            // `Text` has no `fontFeatureSettings` parameter — the card's provided style is the base, so
            // the Inter family it sets is preserved.
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
        )
        StepButton("+", "Larger", enabled = index < TypeSizesPt.lastIndex) { onStep(1) }
    }
}

/**
 * A 40dp stepper button (bench `.tysize button`, `:disabled{opacity:.4}`).
 *
 * **One box: the frozen 40dp chip IS the control.** The chip carries the click, the label and the paint,
 * exactly as [Swatch] (32dp) and [StyleToggle] (46dp) do. The ≥48dp touch target is not a layout box —
 * Compose expands any clickable under `ViewConfiguration.minimumTouchTargetSize` at the *input* layer, so
 * `touchBoundsInRoot` reports 48dp off a 40dp paint for free. DESIGN-RULES 1 + 7: grow the target, never
 * the design — and a layout box IS the design, because it is what the row's gaps measure from.
 *
 * ponytail: no `minimumInteractiveComponentSize` and not the house [zinelyControl] helper (which bundles
 * one). That modifier only *reserves layout space* (its own KDoc: "not needed for touch target expansion
 * to happen. It only affects layout") — and reserving it here is precisely the parity defect: a 48dp box
 * around a 40dp chip insets the paint 4dp, painting the frozen `.tysize{gap:8px}` as 12dp and standing the
 * Size row's painted right edge 4dp inside the shared row edge that `.tyrow{justify-content:space-between}`
 * freezes. The stepper's only neighbour is the non-clickable readout, so the expanded targets overlap
 * nothing and both buttons still reach TalkBack at a full 48x48.
 */
@Composable
private fun StepButton(glyph: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .testTag("$TypeBarTestTag-$description")
            .size(40.dp)
            // bench `.tysize button:disabled{opacity:.4}` fades the WHOLE chip — edge, fill and glyph —
            // not just the glyph. Group opacity (CompositingStrategy.Auto), like CSS; measure-transparent,
            // and ahead of the paint modifiers so the layer wraps them all (ADR-055 §8).
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(11.dp))
            .background(ZinelyTheme.colors.field)
            .border(1.dp, ZinelyTheme.colors.fieldEdge, RoundedCornerShape(11.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                // indication = null, as [zinelyControl] does: the frozen chip has no ripple, and a
                // default ripple here spins the measure/draw loop when the bar is disposed mid-press.
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            fontSize = 20.sp,
            color = ZinelyTheme.colors.onDesk,
        )
    }
}

/**
 * Alignment (bench `.fitseg` visuals, `#alLeft/#alCenter/#alRight`).
 *
 * **The deliberate divergence from the frozen HTML (ADR-055 §4):** the prototype spells alignment as
 * three independent `aria-pressed` buttons, which would let a screen-reader user believe all three can
 * be pressed at once. Alignment is one choice of three, so the Compose semantic layer says so —
 * [selectableGroup] + [Role.RadioButton]. Only the *semantics* diverge; the paint is the frozen segment.
 */
@Composable
private fun AlignSegment(align: TextAlign, onAlign: (TextAlign) -> Unit) {
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(13.dp))
            .border(1.dp, ZinelyTheme.colors.fieldEdge, RoundedCornerShape(13.dp))
            .selectableGroup(),
    ) {
        AlignOption("Left", TextAlign.START, align, onAlign)
        SegmentDivider()
        AlignOption("Center", TextAlign.CENTER, align, onAlign)
        SegmentDivider()
        AlignOption("Right", TextAlign.END, align, onAlign)
    }
}

/** The inter-segment hairline (bench `.fitseg button+button{border-left:…}`). */
@Composable
private fun SegmentDivider() {
    Box(Modifier.width(1.dp).fillMaxHeight().background(ZinelyTheme.colors.fieldEdge))
}

/** One alignment segment (bench `.fitseg button`, 46px min): a radio in a segmented coat. */
@Composable
private fun AlignOption(label: String, value: TextAlign, current: TextAlign, onAlign: (TextAlign) -> Unit) {
    val isSel = value == current
    Box(
        modifier = Modifier
            .testTag("$TypeBarTestTag-align-$label")
            // ponytail: 46dp, the frozen `.fitseg button` size, matching the shipped ReframeControls
            // segment. NOT lifted with minimumInteractiveComponentSize — see [Swatch]: that modifier would
            // only widen the *layout*, pushing the card past the frozen `max-width`, while the 48dp touch
            // target it looks like it buys is already applied at the input layer without it.
            // Widen by growing the frozen segment in bench.html first if this ever needs to be 48.
            .defaultMinSize(minWidth = 46.dp, minHeight = 46.dp)
            .background(if (isSel) ZinelyTheme.colors.coralStrong else ZinelyTheme.colors.field)
            .selectable(
                selected = isSel,
                role = Role.RadioButton,
                onClick = { onAlign(value) },
            )
            // Merge (not clear) so the segment speaks as one radio: the label rides along with the
            // `selected` + role + click action that `selectable` installed on this same node. A
            // clearAndSetSemantics here would drop the click and the group's selection state with it.
            .semantics(mergeDescendants = true) { contentDescription = label }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isSel) Color.White else ZinelyTheme.colors.onDesk,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Bold / Italic (bench `.tytog button`, `aria-pressed`). Independent toggles, not a group (ADR-055 §4):
 * bold and italic genuinely compose, so [Role.Checkbox] — the Compose idiom for `aria-pressed`, and what
 * Material's own icon toggles use — is the honest reading. The glyph wears the style it applies.
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
    Box(
        modifier = Modifier
            .testTag("$TypeBarTestTag-${label.lowercase()}")
            // 46dp, the frozen `.tytog button` floor — the same call [AlignOption] makes two functions
            // down, for the same reason (see [Swatch]).
            .defaultMinSize(minWidth = 46.dp, minHeight = 46.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (on) ZinelyTheme.colors.coralStrong else ZinelyTheme.colors.field)
            .border(
                1.dp,
                if (on) ZinelyTheme.colors.coralStrong else ZinelyTheme.colors.fieldEdge,
                RoundedCornerShape(11.dp),
            )
            .toggleable(
                value = on,
                role = Role.Checkbox,
                onValueChange = onToggle,
            )
            .semantics(mergeDescendants = true) { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            fontSize = 17.sp,
            fontWeight = weight,
            fontStyle = fontStyle,
            color = if (on) Color.White else ZinelyTheme.colors.onDesk,
        )
    }
}

/**
 * The text colour row (bench `.tyinks`) — **text only**. The prototype's image spot-ink popover is a
 * different surface with a different (4-entry) palette and is explicitly not in this batch.
 *
 * Single-select, so it reads as a radio group for the same reason alignment does: a box has exactly one
 * ink. A swatch whose RGBA is not one of the five (a document from elsewhere) simply shows none selected.
 */
@Composable
private fun InkRow(color: ColorRgba, onInk: (TextInk) -> Unit) {
    Row(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextInk.entries.forEach { ink -> Swatch(ink = ink, selected = ink.rgba == color, onInk = onInk) }
    }
}

/**
 * A 32dp ink swatch (bench `.tyinks .swatch`): selected wears a 2px `--on-desk` ring.
 *
 * The **paint** is the theme's token, the **committed value** is the fixed paper-space RGBA — the two
 * are deliberately not the same lookup. ADR-055 Decision 6 pins the *ink* theme-independently (printed
 * ink cannot depend on the viewer's theme); it says nothing about the swatch, and the frozen bench
 * paints each swatch with its CSS variable. Only Coral actually differs (`--coral-text` is overridden on
 * dark paper); the other four resolve to the same value in both themes.
 *
 * **No `minimumInteractiveComponentSize` — the target survives without it, at the frozen pitch.** The
 * modifier does not create the target; it only reserves *layout* space for one ("This modifier is not
 * needed for touch target expansion to happen. It only affects layout" — its own KDoc). The target comes
 * from the input layer, which expands any clickable's touch bounds to
 * `ViewConfiguration.minimumTouchTargetSize` regardless (`NodeCoordinator.touchBoundsInRoot`);
 * [TypeBarTest] asserts exactly that on this swatch.
 *
 * **Honest ceiling:** expansion is 48dp, but the frozen 40dp pitch (32dp paint + 8dp gap) makes
 * neighbouring expansions overlap, and Compose prunes the overlap before reporting bounds to the
 * accessibility layer (`SemanticsOwner` intersects each node against the unaccounted region, which
 * `AndroidComposeViewAccessibilityDelegateCompat` hands to `setBoundsInScreen`). So four of the five
 * swatches report **40×48** to TalkBack, not 48×48 — clearing WCAG 2.5.8 AA (24×24) but under Material's
 * 48dp guideline. Tapping is unaffected: a hit inside the 32dp paint always wins outright, and the 8dp
 * gaps resolve to the nearest swatch. 40dp is the pitch the frozen spec asks for; widening it is a
 * re-freeze of `.tyinks`, not a code change (ADR-055 §8).
 *
 * Reserving it here was a real layout bug, not a harmless belt-and-braces: the modifier answers the
 * `IntrinsicSize.Max` query above (it overrides `measure`, not the intrinsics, so the default
 * `LayoutModifierNode` intrinsics re-run `measure` and return the inflated 48dp). Five swatches at 48dp
 * instead of the frozen 32dp made the Colour row the widest by 80dp and blew the card out to exactly
 * 360dp — over the frozen `max-width:calc(100% - 24px)`, and edge-to-edge on a 360dp phone. The frozen
 * 32dp paint on a 40dp pitch is what ships; the target is 48dp either way.
 */
@Composable
private fun Swatch(ink: TextInk, selected: Boolean, onInk: (TextInk) -> Unit) {
    val paint = when (ink) {
        TextInk.Coral -> ZinelyTheme.colors.coralText
        else -> Color(ink.rgba.r, ink.rgba.g, ink.rgba.b, ink.rgba.a)
    }
    Box(
        modifier = Modifier
            .testTag("$TypeBarTestTag-ink-${ink.label}")
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(paint)
            .border(
                2.dp,
                if (selected) ZinelyTheme.colors.onDesk else Color.Transparent,
                RoundedCornerShape(9.dp),
            )
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = { onInk(ink) },
            )
            .semantics { contentDescription = ink.label },
    )
}
