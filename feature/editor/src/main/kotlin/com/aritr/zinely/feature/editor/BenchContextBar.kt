package com.aritr.zinely.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.ui.components.zinelyShadow
import com.aritr.zinely.ui.theme.ZinelyShadowLayer
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Standard

/** Test tag on the frozen verb bar; absent from the tree when it is not showing. */
internal const val BenchContextBarTestTag: String = "bench-context-bar"

/**
 * What the selected element is, for the purpose of choosing verbs. The freeze's `toolsFor()` branches
 * on `data-kind` over three values; [DECOR] is the third and is **unreachable** while `DecorElement` is
 * re-seated ([OD-2](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-029),
 * [ADR-092](../../../../../../../../docs/DECISIONS.md#adr-092) row 2.13). It is named here rather than
 * omitted so the missing branch is a compile-time hole the day the kind lands, not a silent default.
 */
internal enum class BenchVerbKind { TEXT, PHOTO, DECOR }

/**
 * One verb of the frozen contextual bar. [label] is both the drawn caption and the spoken name — the
 * icon above it is decorative, exactly as in the freeze, where the `<span>` carries the word.
 *
 * [enabled] is `false` only for **Font**, which the freeze draws and the product cannot honour:
 * [OD-9](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-031-ruling) ruled that the frozen
 * Bench specifies *the editing surface, not the whole application flow*, so a control it draws is kept
 * drawn and invents nothing. A disabled control is the honest rendering of that: it is visible, it is
 * announced, and it does not promise a tap that goes nowhere.
 */
internal data class BenchVerb(
    val label: String,
    val icon: ImageVector,
    val danger: Boolean = false,
    val enabled: Boolean = true,
)

/**
 * The frozen verb sets, one per kind (`v2-bench.html:450-453`). Pure, so the *sets* are asserted
 * directly rather than through a composition — and asserted as **set-equality plus order**, because a
 * permutation would satisfy "each verb exists" and still be the wrong bar.
 */
internal fun benchContextVerbs(kind: BenchVerbKind, styleable: Boolean = true): List<BenchVerb> = when (kind) {
    BenchVerbKind.TEXT -> listOf(
        BenchVerb(Copy.BenchVerbs.EDIT, Icons.Filled.Edit),
        BenchVerb(Copy.BenchVerbs.FONT, Icons.Filled.TextFields, enabled = false),
        // [styleable] is false for a still-blank box, which the reducer refuses to style (ADR-055) — so
        // these two are drawn and inert there, exactly as `Font` is, under the same OD-9 class. Found by
        // review, not by a test: with them live, tapping either on a blank box set `typeBarOpen`, which
        // hid this bar (its own `!typeBarOpen` term) while the Type bar declined to appear (its
        // `styleTarget != null` term) — and the reset effect is keyed on `styleTarget?.id`, still null,
        // so it never re-ran. The bar did not come back until a non-blank box was selected. A dead end
        // that swallowed the toolbar, and the mirror image of what `TypeBarTest` already forbids on the
        // transform bar (D-040).
        BenchVerb(Copy.BenchVerbs.SIZE, Icons.Filled.FormatSize, enabled = styleable),
        BenchVerb(Copy.BenchVerbs.INK, Icons.Filled.Palette, enabled = styleable),
        BenchVerb(Copy.BenchVerbs.DELETE, Icons.Filled.Delete, danger = true),
    )
    BenchVerbKind.PHOTO -> listOf(
        BenchVerb(Copy.BenchVerbs.REFRAME, Icons.Filled.Crop),
        // Disabled for the same reason as Font, discovered the same way: `Intent.ReplaceImage` exists in
        // the reducer and is dispatched from nowhere, and reaching it needs a picker bound to an existing
        // element — a new effect parameterisation, i.e. a flow, not a re-skin. OD-9's class ("a control the
        // freeze draws is kept drawn and invents nothing") applies; the capability question is
        // [D-038](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-038), for the owner.
        BenchVerb(Copy.BenchVerbs.REPLACE, Icons.Filled.SwapHoriz, enabled = false),
        BenchVerb(Copy.BenchVerbs.DELETE, Icons.Filled.Delete, danger = true),
    )
    // Unreachable by construction: nothing in the document produces this kind yet. Returning an empty
    // list would let a future DecorElement quietly render a bar with no verbs; failing loudly is the
    // behaviour row 2.13 asks for.
    BenchVerbKind.DECOR -> error("decor verbs are unreachable until DecorElement is re-seated (OD-2)")
}

/** The kind of a selected element, or `null` for kinds that have no frozen verb set yet. */
internal fun benchVerbKindOf(element: Element): BenchVerbKind? = when (element) {
    is TextElement -> BenchVerbKind.TEXT
    is ImageElement -> BenchVerbKind.PHOTO
    else -> null
}

/**
 * The frozen contextual verb bar — `.ctx` (`v2-bench.html:211-217`), [ADR-092](../../../../../../../../docs/DECISIONS.md#adr-092).
 *
 * **This is an addition, not a re-skin.** [EditorContextBar] is the WCAG 2.5.7 single-pointer twin of
 * the drag gestures ([ADR-029](../../../../../../../../docs/DECISIONS.md#adr-029) §6);
 * [OD-11](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-034-ruling) ruled that the frozen bar is
 * *additive*, because a parity phase does not remove an accessibility path. `Delete` is the one verb both
 * bars name, and under
 * [OD-14](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-039-ruling) only **one of them shows it at
 * a time**: while this bar is up, [EditorContextBar] withholds its `Delete` and the on-canvas reframe chip
 * withholds itself. A presentation switch, never a capability one — both return the instant this bar stands
 * down, and its eight transform verbs are untouched in every state.
 *
 * The bar floats over the canvas, inset 12dp from its left, right and bottom edges — never over the
 * element ([IA §C.3](../../../../../../../../docs/design/V2-BENCH-IA-INTERACTION.md)) — so the host
 * places it with [Modifier.align] rather than giving it a row of its own.
 *
 * @param visible drives the frozen enter/exit: a 14dp rise and a fade over `.2s var(--standard)`.
 *   Collapses to 0ms under reduced motion ([ADR-075](../../../../../../../../docs/DECISIONS.md#adr-075)).
 * @param verbs the frozen set for the selected element's kind — [benchContextVerbs].
 * @param onVerb invoked with the tapped verb; a disabled verb never reaches it.
 */
@Composable
internal fun BenchContextBar(
    visible: Boolean,
    verbs: List<BenchVerb>,
    onVerb: (BenchVerb) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = if (ZinelyTheme.motion.reduceMotion) 0 else BenchContextBarEnterMillis
    val spec = tween<Float>(motion, easing = ZinelyV2Standard)
    // The freeze rises the bar by a FIXED 14px, not by its own height, so the slide offset is a
    // converted Dp rather than a fraction of `fullHeight`.
    val enterPx = with(LocalDensity.current) { BenchContextBarEnterOffsetDp.roundToPx() }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(tween(motion, easing = ZinelyV2Standard)) { enterPx } + fadeIn(spec),
        exit = slideOutVertically(tween(motion, easing = ZinelyV2Standard)) { enterPx } + fadeOut(spec),
    ) {
        val shape = RoundedCornerShape(BenchContextBarRadiusDp)
        Row(
            modifier = Modifier
                .padding(BenchContextBarInsetDp)
                // The tag sits INSIDE the inset padding on purpose: tagged outside it, the node's bounds
                // are the whole canvas width and the frozen 12dp inset is unmeasurable - which is how the
                // first version of this file passed nothing.
                .testTag(BenchContextBarTestTag)
                // The card swallows taps that miss a verb. Without this, the 8dp padding and the 6dp gaps
                // between verbs are holes: the tap falls through to the canvas, which since OD-13 reads a
                // tap outside the selection as "dismiss" — so aiming at the toolbar and missing by 3dp
                // deselects the element and takes the toolbar away with it. Measured on device, 4 times out
                // of 4 (ADR-092 row 2.10b). Children are offered the event first, so the verbs still get
                // their taps; this only catches what nothing else wanted.
                .pointerInput(Unit) { detectTapGestures { } }
                // `0 12px 30px -12px var(--frame-shadow)` — the corpus's first spread-bearing layer.
                .zinelyShadow(
                    listOf(
                        ZinelyShadowLayer(
                            dy = 12.dp,
                            blur = 30.dp,
                            color = ZinelyTheme.v2Colors.frameShadow,
                            spread = (-12).dp,
                        ),
                    ),
                    shape,
                )
                .background(ZinelyTheme.v2Colors.sheet, shape)
                .border(1.dp, ZinelyTheme.v2Colors.chromeLine, shape)
                .padding(BenchContextBarPaddingDp),
            horizontalArrangement = Arrangement.spacedBy(BenchContextBarGapDp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (verb in verbs) {
                BenchVerbButton(verb = verb, onClick = { onVerb(verb) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * One verb: `flex:1` (equal share of the row, whatever the labels measure), 40dp tall, radius 10, with
 * the icon stacked over a 12.5sp/500 caption at a 2dp gap.
 *
 * The icon is decorative and the caption is the name, so [clearAndSetSemantics] publishes one node with
 * one label — not an icon and a text the reader would announce twice.
 */
@Composable
private fun BenchVerbButton(verb: BenchVerb, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // `.icon-btn:disabled{opacity:.35}` (v2-bench.html:206). The frozen `.ctx button` has no disabled
    // rule of its own — the freeze never disables one — so this borrows the corpus's single existing
    // disabled convention rather than inventing a second. Required because the content colour is passed
    // explicitly below, which overrides the `LocalContentColor` a disabled TextButton would supply: without
    // it, Font and Replace say "disabled" to TalkBack and "tap me" to the eye. Alpha rides on the tint, not
    // on the whole control, so the 40dp box and its ripple bounds are untouched.
    val base = if (verb.danger) ZinelyTheme.v2Colors.consequence else ZinelyTheme.v2Colors.ink
    val tint = if (verb.enabled) base else base.copy(alpha = BenchContextBarDisabledAlpha)
    TextButton(
        onClick = onClick,
        enabled = verb.enabled,
        shape = RoundedCornerShape(BenchContextBarButtonRadiusDp),
        modifier = modifier
            .height(BenchContextBarButtonHeightDp)
            .testTag("$BenchContextBarTestTag-${verb.label}")
            .clearAndSetSemantics {
                contentDescription = verb.label
                role = Role.Button
                // `clearAndSetSemantics` wipes everything the button published, INCLUDING the disabled
                // state `enabled = false` sets - so without this line Font is announced as an ordinary
                // button that simply does nothing when tapped. That is precisely the ADR-058
                // ReframeControls.ZoomButton defect, reproduced here by the same mechanism and caught by
                // the same assertion.
                //
                // It wipes `onClick` by the same rule, and the first version of this file audited only the
                // disabled axis — so every ENABLED verb published a button with no activation action. A
                // pointer tap still worked (it never consulted semantics), which is exactly what made it
                // invisible: TalkBack synthesises a tap, but an ACTION_CLICK consumer — Switch Access, an
                // external keyboard, automation — got a control it could not activate, and `uiautomator
                // dump` would have read `clickable="false"` on all seven. Same defect class as above, one
                // axis over.
                if (verb.enabled) onClick { onClick(); true } else disabled()
            },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BenchContextBarLabelGapDp),
        ) {
            Icon(
                imageVector = verb.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(BenchContextBarIconDp),
            )
            Text(
                text = verb.label,
                color = tint,
                fontSize = BenchContextBarLabelSp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ── The frozen numbers, in one place so the tests read them from here and not from a second copy ─────

internal val BenchContextBarInsetDp = 12.dp
internal val BenchContextBarRadiusDp = 16.dp
internal val BenchContextBarPaddingDp = 8.dp
internal val BenchContextBarGapDp = 6.dp
internal val BenchContextBarButtonHeightDp = 40.dp

/**
 * The verb row's **laid-out** height, which is the frozen drawn 40dp — *not* Material's 48dp interactive
 * floor.
 *
 * The distinction cost two wrong answers, so it is written down. A review argued the row must measure 48,
 * citing this file's own `the frozen 40dp verb still offers a 48dp touch target`. That test is correct and
 * so is the floor — but `minimumInteractiveComponentSize` grows the **touch target**, which extends beyond
 * the layout box without enlarging it or its parent. Measured in place
 * (`EditorScreenGoldenTest."the sheet is fitted above the context bar"`), the bar's footprint from the canvas
 * foot is **68dp** = `12 + 8 + 40 + 8`. The row is 40.
 *
 * It stays a named constant rather than folding back into the reserve because *which* of the two numbers
 * this is has now been got wrong from both directions, and the name is where the answer lives.
 */
internal val BenchContextBarRowHeightDp = BenchContextBarButtonHeightDp
internal val BenchContextBarButtonRadiusDp = 10.dp
internal val BenchContextBarLabelGapDp = 2.dp
internal val BenchContextBarIconDp = 17.dp
internal val BenchContextBarLabelSp = 12.5.sp

/** `.icon-btn:disabled{opacity:.35}` (`v2-bench.html:206`) — the corpus's one disabled treatment. */
internal const val BenchContextBarDisabledAlpha = 0.35f
internal const val BenchContextBarEnterMillis: Int = 200

/** `transform:translateY(14px)` — a fixed rise, not a fraction of the bar's own height. */
internal val BenchContextBarEnterOffsetDp = 14.dp

/**
 * **The vertical band this bar occupies at the canvas's foot** — `inset + (padding + row + padding)`, where
 * `row` is the button's **layout** height, not its drawn one.
 *
 * ### Two wrong answers, from opposite directions, before a measurement settled it
 *
 * The first cut added a **second** [BenchContextBarInsetDp] for the padding *above* the card — which is
 * transparent and occupies nothing — giving 80dp against a real band of 68. It over-reserved, so the fix
 * appeared to work and the sheet simply sat 12dp higher than it needed to.
 *
 * A review then argued the opposite error: that [BenchContextBarRowHeightDp] must be Material's **48dp**
 * interactive floor rather than the frozen 40, citing this file's own
 * `the frozen 40dp verb still offers a 48dp touch target`. That test is right and the floor is real, but it
 * measures a **touch target** — `minimumInteractiveComponentSize` extends the target past the layout box
 * without growing it. Taking that correction would have over-reserved by 8dp instead.
 *
 * Neither was settled by argument. `EditorScreenGoldenTest` composes the bar and measures
 * `canvas.bottom - bar.top`: **68dp**, i.e. `inset + padding + 40 + padding`, and it now asserts this
 * constant against that measurement — so the next person to reason about it from the constants alone will
 * be corrected by the suite rather than by a device.
 *

 * It exists because the bar **floats over the sheet**, and P2's device Pass 2 found it covering the bottom
 * edge of the keep-clear boundary and the page folio with it. A boundary whose whole job is to say *where
 * the bottom limit is* was hiding exactly that, and it was hidden by chrome summoned on the same tap — the
 * cue and the bar answer to one predicate, so they arrive together and then fight for the same pixels.
 *
 * [EditorScreen] subtracts this from the canvas height before fitting the sheet, so the page is sized and
 * centred in the residual band above the bar.
 *
 * **Reserved unconditionally, not only while the bar is up**, and that is the whole of the design decision.
 * Reserving it on visibility would resize the sheet on every select and deselect — the page jumping under
 * the finger that just tapped it, which is a worse defect than the one being fixed — and `screenPxPerPt`
 * keys the gesture `pointerInput`, so a scale change at selection is the same hazard the viewport push is
 * already deferred to avoid. A constant, slightly smaller page costs nothing a maker can perceive; a page
 * that changes size when touched costs trust.
 */
internal val BenchContextBarReservedHeightDp =
    BenchContextBarInsetDp + BenchContextBarPaddingDp + BenchContextBarRowHeightDp +
        BenchContextBarPaddingDp
