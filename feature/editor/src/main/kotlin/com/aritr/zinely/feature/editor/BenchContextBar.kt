package com.aritr.zinely.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts

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
    /**
     * Why this verb is disabled, announced as its **state** rather than folded into its name.
     *
     * `stateDescription`, not `contentDescription`: the verb's name is `Font`, and it stays `Font` — a
     * label that reads "Font, not available yet" is a *different control's* name and would break every
     * copy test that asserts the verb set. State is the axis that changes; the name is not.
     *
     * Null for an enabled verb, and null is meaningful: a verb with no reason is either live or disabled
     * for something the user cannot act on, and inventing a sentence for that would be noise.
     */
    val unavailableBecause: String? = null,
    /**
     * `null` for the ordinary verbs, which *do* something. Non-null makes this verb a **toggle** and is
     * its current setting — published as a [stateDescription], because a control whose only feedback is
     * a change on the canvas tells a screen reader nothing, and tells a maker whose photo is small or
     * half-covered by the bar not much more. `Copier` is the only one, and it exists because review
     * caught it shipping as a stateless button ([D-082](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-082) Q4).
     *
     * It shares [unavailableBecause]'s channel and can never collide with it: a disabled verb has no
     * setting to report, and a toggle is live by construction.
     */
    val checked: Boolean? = null,
)

/**
 * The frozen verb sets, one per kind (`v21-bench.html:656-660`). Pure, so the *sets* are asserted
 * directly rather than through a composition — and asserted as **set-equality plus order**, because a
 * permutation would satisfy "each verb exists" and still be the wrong bar.
 */
internal fun benchContextVerbs(
    kind: BenchVerbKind,
    styleable: Boolean = true,
    copierOn: Boolean = false,
): List<BenchVerb> = when (kind) {
    BenchVerbKind.TEXT -> listOf(
        BenchVerb(Copy.BenchVerbs.EDIT, Icons.Filled.Edit),
        BenchVerb(
            Copy.BenchVerbs.FONT, Icons.Filled.TextFields, enabled = false,
            unavailableBecause = Copy.BenchVerbs.NOT_YET,
        ),
        // [styleable] is false for a still-blank box, which the reducer refuses to style (ADR-055) — so
        // these two are drawn and inert there, exactly as `Font` is, under the same OD-9 class. Found by
        // review, not by a test: with them live, tapping either on a blank box set `typeBarOpen`, which
        // hid this bar (its own `!typeBarOpen` term) while the Type bar declined to appear (its
        // `styleTarget != null` term) — and the reset effect is keyed on `styleTarget?.id`, still null,
        // so it never re-ran. The bar did not come back until a non-blank box was selected. A dead end
        // that swallowed the toolbar, and the mirror image of what `TypeBarTest` already forbids on the
        // transform bar (D-040).
        BenchVerb(
            Copy.BenchVerbs.SIZE, Icons.Filled.FormatSize, enabled = styleable,
            unavailableBecause = Copy.BenchVerbs.TYPE_FIRST.takeUnless { styleable },
        ),
        BenchVerb(
            Copy.BenchVerbs.INK, Icons.Filled.Palette, enabled = styleable,
            unavailableBecause = Copy.BenchVerbs.TYPE_FIRST.takeUnless { styleable },
        ),
        BenchVerb(Copy.BenchVerbs.DELETE, Icons.Filled.Delete, danger = true),
    )
    BenchVerbKind.PHOTO -> listOf(
        BenchVerb(Copy.BenchVerbs.REFRAME, Icons.Filled.Crop),
        // The one verb this bar gained after its freeze, and the freeze was amended first
        // (`v21-bench.html:690`) exactly as CLAUDE.md's HTML-first rule requires — never the reverse.
        // (Was cited as :674, which is the *text* list; corrected by review after counting the lines.)
        // It is live, not drawn-and-inert like Font and Replace: the whole feature is one boolean on
        // the selected photo, so there is nothing left to invent ([ADR-106]).
        BenchVerb(Copy.BenchVerbs.COPIER, Icons.Filled.Grain, checked = copierOn),
        // Disabled for the same reason as Font, discovered the same way: `Intent.ReplaceImage` exists in
        // the reducer and is dispatched from nowhere, and reaching it needs a picker bound to an existing
        // element — a new effect parameterisation, i.e. a flow, not a re-skin. OD-9's class ("a control the
        // freeze draws is kept drawn and invents nothing") applies; the capability question is
        // [D-038](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-038), for the owner.
        BenchVerb(
            Copy.BenchVerbs.REPLACE, Icons.Filled.SwapHoriz, enabled = false,
            unavailableBecause = Copy.BenchVerbs.NOT_YET,
        ),
        BenchVerb(Copy.BenchVerbs.DELETE, Icons.Filled.Delete, danger = true),
    )
    // Reachable as of ADR-105 / package P1. The `error(...)` that stood here cited OD-2 as live; OD-2
    // re-seated DecorElement *"beyond Phase C"*, Phase C completed 2026-08-06, and
    // [D-029's 2026-08-16 ruling](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-029-ruling-2026-08-16)
    // assigns the supplies programme as the phase that takes it. The fence expired; this is the record.
    //
    // The frozen verb set is Replace / Ink / Delete (`v21-bench.html:71`), and it is transcribed whole.
    // Two of the three are drawn-and-inert under exactly the OD-9 class Font and Replace-photo already
    // sit in — "a control the freeze draws is kept drawn and invents nothing":
    //  - `Replace` is **live as of the replace-supply package**: it re-summons the Art sheet carrying the
    //    selected supply's id, and the tapped tile becomes that element's new outline
    //    ([Intent.ReplaceSupply]). The incoming family's §5.2 scale is applied at the outgoing element's
    //    centre and rotation — an owner ruling, because the freeze does not say what a swap does to size.
    //  - `Ink` is **live as of the decor-ink package**: [Intent.InkSupply] recolours a `DecorElement` and
    //    `benchInkBands` already had the DECOR arm, so the popover's decor branch was a routing change,
    //    not new material. Its enablement is unconditional because — unlike text, where a blank box cannot
    //    be styled — *every* placed supply has an ink and can always be recoloured. There is no decor
    //    counterpart to `styleable`.
    // Delete is live because it is a shared verb that already works on any element id.
    BenchVerbKind.DECOR -> listOf(
        BenchVerb(Copy.BenchVerbs.REPLACE, Icons.Filled.SwapHoriz),
        BenchVerb(Copy.BenchVerbs.INK, Icons.Filled.Palette),
        BenchVerb(Copy.BenchVerbs.DELETE, Icons.Filled.Delete, danger = true),
    )
}

/**
 * The kind of a selected element. **Total** — every [Element] has a verb set.
 *
 * The `else -> null` that used to close this `when` was the single most dangerous line in the supplies
 * blast radius, and the quietest: a `DecorElement` would have produced *no context bar and no compile
 * error*, so a supply could be selected and simply have no verbs, forever, with a green test suite.
 * D-029's ruling names it specifically. **Do not reintroduce an `else` here** — the exhaustive `when`
 * is the mechanism by which a fourth element kind is forced to declare its verbs.
 */
/**
 * The element the frozen `.inkpop` can actually recolour — a [TextElement] **or a [DecorElement]**, or `null`.
 *
 * ✅ **The prediction at the foot of this note came true, and the binding held.** It was written while
 * decor's `Ink` verb was disabled, and it ended *"the fix that survives someone enabling the verb later."*
 * The decor-ink package is that later. Enabling the verb widened **this one function** and its colour
 * counterpart [benchInkColorOf]; the router, the popover's visibility and the F-5 clearance term each
 * followed for free, because they read the binding rather than casting for themselves. Had the three `as?`
 * casts still been in place, enabling the verb would have re-opened the dead-end screen described below —
 * with no compile error.
 *
 * ### Why this is a function and not an `as?` at each call site
 *
 * It was three `as?` casts, and one of them was in the wrong place. `EditorScreen`'s `Ink` verb opened the
 * popover **unconditionally** while the popover itself resolved its target with `ctxElement as? TextElement`
 * — so for a [com.aritr.zinely.core.model.DecorElement] the two disagreed and the screen entered a state
 * with no context bar (`ctxVisible` carries `!inkPopoverOpen`), no popover, a disabled `Done` and a bottom
 * bar already captioned for an ink session. Nothing on screen could act, and only Back recovered it.
 *
 * That was harmless only because [benchContextVerbs] ships decor's `Ink` disabled — which is the S7′ shape
 * [SUPPLIES-SPEC §10](../../../../../../../../docs/design/SUPPLIES-SPEC.md) warns about in general: *"the
 * **silent** seams — 13 `as?` casts … These fail no test."* A latent defect whose only guard is a control
 * being off is not guarded; it is armed. §10.1 names this exact line and rules that **S7 fixes the routing,
 * not the verb**, so the verb stays disabled and the route stops depending on it.
 *
 * One binding, read by the router, the popover and the F-5 clearance term, makes the three incapable of
 * disagreeing — the fix that survives someone enabling the verb later.
 *
 * Deliberately takes `Element?` and not `Element`: nothing is selected far more often than something is,
 * and pushing the null onto every caller is how the three casts drifted apart in the first place.
 */
internal fun benchInkTargetOf(element: Element?): Element? = when (element) {
    is TextElement -> element
    is DecorElement -> element
    // A photo's colour is the photocopier's job, not an ink — the frozen PHOTO verb set has no `Ink`.
    is ImageElement, null -> null
}

/**
 * The ink [element] is currently laid down in, for the popover's selection ring — or `null` when nothing
 * inkable is selected.
 *
 * Kept beside [benchInkTargetOf] and switched on the same three arms, because the pair has to agree: a
 * target the popover can open on but whose colour reads `null` rings nothing, and the maker sees a popover
 * that has forgotten which ink their supply is already wearing.
 *
 * The two cannot be folded into one function returning a pair — the call sites want them at different
 * times (visibility is computed with `ctxElement`, the ring at the popover's own call site), and threading
 * a pair through both is what pushed these apart into casts the first time.
 */
internal fun benchInkColorOf(element: Element?): ColorRgba? = when (element) {
    is TextElement -> element.style.color
    is DecorElement -> element.ink
    is ImageElement, null -> null
}

internal fun benchVerbKindOf(element: Element): BenchVerbKind = when (element) {
    is TextElement -> BenchVerbKind.TEXT
    is ImageElement -> BenchVerbKind.PHOTO
    is DecorElement -> BenchVerbKind.DECOR
}

/**
 * The frozen contextual verb bar — `.ctx` (`v21-bench.html:220-234`, markup `:529`);
 * [ADR-092](../../../../../../../../docs/DECISIONS.md#adr-092), re-skinned to V2.1 by
 * [ADR-102](../../../../../../../../docs/DECISIONS.md#adr-102) package P4.
 *
 * ### What P4 changed here, and the one sentence that governs all of it
 *
 * The freeze banners this class itself (`v21-bench.html:220-222`): *"Appears BELOW the selection, never
 * over it. No offset shadow, no tilt, no tape: it is a tool, and tools do not perform."* So where the rest
 * of V2.1 gained printed depth, this bar **loses** the depth it had: V2's spread-bearing
 * `0 12px 30px -12px` soft shadow is gone and **nothing replaces it** — no `--hard`, no
 * [zinelyV21Pressable][com.aritr.zinely.ui.components.zinelyV21Pressable] on the card, and no press tier on
 * its verbs, because `.ctx button` declares no `:active`. What is left is a `paper` pill outlined in real
 * ink at the language's 1.5dp pen.
 *
 * It is also no longer a full-width strip. V2 pinned it `left:12px;right:12px`; V2.1 pins it
 * `left:50%;transform:translateX(-50%)` — a **content-width** pill centred over the canvas, 12dp off its
 * foot. Its verbs lost `flex:1` with it and are now sized by `min-width:50px` plus their own labels, which
 * is why `Delete` is wider than `Ink` where the two used to measure the same.
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
 * The bar floats over the canvas, 12dp off its bottom edge and centred on it — never over the
 * element ([IA §C.3](../../../../../../../../docs/design/V2-BENCH-IA-INTERACTION.md)) — so the host
 * places it with [Modifier.align] rather than giving it a row of its own. The host passes a full-width
 * modifier, so the centring is done here, by a [Box] the card sits inside.
 *
 * ### Deviation: the glyphs are still Material's filled icons
 *
 * `.ctx svg` is a 17px **stroked** glyph at `stroke-width:1.8` (`v21-bench.html:233-234`), and V2.1's other
 * chrome draws exactly that through `ZinelyV2Icons`/`toImageVector`. It is not done here because that set
 * has no entry for `Edit`, `Size`, `Ink`, `Delete` or `Reframe` — five new glyphs is a drawing task, not a
 * re-skin, and inventing them would put marks on screen that no frozen file specifies. The icons therefore
 * stay as they were and the stroke weight is recorded but unused. Owner call.
 *
 * @param visible drives the frozen enter/exit: an 8dp rise and a fade over `.16s` (`v21-bench.html:226`),
 *   on the freeze's own `cubic-bezier(.2,.7,.2,1)` — see [BenchContextBarEnterEasing]. Collapses to 0ms
 *   under reduced motion ([ADR-075](../../../../../../../../docs/DECISIONS.md#adr-075)).
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
    val colors = ZinelyTheme.v21Colors
    val motion = if (ZinelyTheme.motion.reduceMotion) 0 else BenchContextBarEnterMillis
    val spec = tween<Float>(motion, easing = BenchContextBarEnterEasing)
    // The freeze rises the bar by a FIXED 8px, not by its own height, so the slide offset is a
    // converted Dp rather than a fraction of `fullHeight`.
    val enterPx = with(LocalDensity.current) { BenchContextBarEnterOffsetDp.roundToPx() }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(tween(motion, easing = BenchContextBarEnterEasing)) { enterPx } + fadeIn(spec),
        exit = slideOutVertically(tween(motion, easing = BenchContextBarEnterEasing)) { enterPx } + fadeOut(spec),
    ) {
        // `left:50%;transform:translateX(-50%)` — the card is content-width and centred, where V2's was a
        // full-width strip inset 12dp on both sides. The host hands this composable a `fillMaxWidth`
        // modifier it cannot change, so the centring happens here.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = BenchContextBarInsetDp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Row(
                modifier = Modifier
                    // The tag is the card itself, so the node's bounds are the pill — its ground, its ink
                    // border and its 4dp padding included. Tagged outside it the bounds would be the whole
                    // canvas width and the frozen bottom inset would be unmeasurable, which is how the
                    // first version of this file passed nothing.
                    .testTag(BenchContextBarTestTag)
                    // The card swallows taps that miss a verb. Without this, the 4dp padding and the 2dp
                    // gaps between verbs are holes: the tap falls through to the canvas, which since OD-13
                    // reads a tap outside the selection as "dismiss" — so aiming at the toolbar and missing
                    // by 3dp deselects the element and takes the toolbar away with it. Measured on device,
                    // 4 times out of 4 (ADR-092 row 2.10b). Children are offered the event first, so the
                    // verbs still get their taps; this only catches what nothing else wanted.
                    .pointerInput(Unit) { detectTapGestures { } }
                    // ⚠ **No shadow, of either material.** `.ctx` declares `background`, `border` and
                    // `border-radius` and stops — the freeze's own banner at `v21-bench.html:220-222` says
                    // why. Nothing that clips needs to sit right of anything here, because there is nothing
                    // painting outside the node to protect.
                    .clip(BenchContextBarShape)
                    .background(colors.paper)
                    .border(BenchChromeBorder, colors.ink, BenchContextBarShape)
                    .padding(BenchContextBarPaddingDp),
                horizontalArrangement = Arrangement.spacedBy(BenchContextBarGapDp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (verb in verbs) {
                    BenchVerbButton(verb = verb, onClick = { onVerb(verb) })
                }
            }
        }
    }
}

/**
 * One verb — `.ctx button` (`v21-bench.html:228-234`): a pill at least 50dp wide, 8dp of padding on every
 * side, with a 17dp glyph stacked over a 9.92sp/600 caption at a 2dp gap.
 *
 * The icon is decorative and the caption is the name, so [clearAndSetSemantics] publishes one node with
 * one label — not an icon and a text the reader would announce twice.
 *
 * ### It has no ground (unless it is on), no border and no press
 *
 * `background:none;border:0` — the verb is drawn entirely by its own marks on the card's `paper`, and
 * `.ctx button` declares no `:active`, so there is no press tier and no
 * [zinelyV21Pressable][com.aritr.zinely.ui.components.zinelyV21Pressable]. The freeze's only feedback is
 * `:hover{background:var(--butter-tint)}`, which a finger never fires and which V21-SPEC §3.2 would forbid
 * borrowing as a pressed state anyway — butter is material, never an action or a state on its own. The
 * platform ripple is therefore left in place as the one thing that answers a finger here; it is not frozen
 * and it is not removed, because removing it would take away feedback the shipped control already had.
 */
@Composable
private fun BenchVerbButton(verb: BenchVerb, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v21Colors
    // `.ctx button.on{background:var(--leaf);color:var(--on-leaf)}` — the freeze's A4 amendment, made in
    // the HTML first. A toggle with no on appearance was measured on device (SM-A176B / Android 16) as
    // pixel-identical before and after its tap; the halftone on the canvas is not feedback when the photo
    // is small, scrolled away, or behind this bar. `leaf`/`onLeaf` is the corpus's existing on-state pair
    // (`.chip2.on`, `.ctl.on`), reused rather than replaced: both halves flip with the theme, so unlike a
    // tint ground it stays visible in dark (ADR-100 §4's invisible-`butterTint` finding).
    val checkedOn = verb.checked == true
    // `.ctx button{color:var(--ink-soft)}` with `.ctx button.danger{color:var(--jam-text)}` — jam is the
    // one urgent colour in V2.1 and Delete is the one verb entitled to it. `.on` wins over both by the
    // cascade: the amendment's <style> is later in the file at equal specificity.
    val tint = when {
        checkedOn -> colors.onLeaf
        verb.danger -> colors.jamText
        else -> colors.inkSoft
    }
    // Delete is the one verb that cannot be taken back by pressing it again — Boundary says so in the
    // hand before the snackbar says it in words. Every other verb is an ordinary action.
    val fire = benchTap(if (verb.danger) ZinelyHaptic.Boundary else ZinelyHaptic.Tick, onClick)
    Column(
        modifier = modifier
            .height(BenchContextBarButtonHeightDp)
            .defaultMinSize(minWidth = BenchContextBarButtonMinWidthDp)
            // `.icon-btn:disabled{opacity:.35}` (`v21-bench.html:345`). `.ctx button` has no disabled rule
            // of its own — the freeze never disables one — so this borrows the corpus's single existing
            // disabled convention rather than inventing a second. It rides on the whole control rather than
            // on the tint, which is the finding [BenchBottomBar] records: fading the mark alone leaves the
            // rest of the control at full strength and draws heavier chrome than the freeze does. Here that
            // costs nothing extra, since the verb has no ground or border to fade.
            .graphicsLayer { alpha = if (verb.enabled) 1f else ZinelyV21Dimens.disabledAlpha }
            .clip(BenchContextBarButtonShape)
            // Below the `clickable`, so the ripple still draws over the checked ground.
            .then(if (checkedOn) Modifier.background(colors.leaf) else Modifier)
            .then(if (verb.enabled) Modifier.clickable(onClick = fire) else Modifier)
            .testTag("$BenchContextBarTestTag-${verb.label}")
            .clearAndSetSemantics {
                contentDescription = verb.label
                role = Role.Button
                // `clearAndSetSemantics` wipes everything the button published, INCLUDING the disabled
                // state - so without this line Font is announced as an ordinary button that simply does
                // nothing when tapped. That is precisely the ADR-058 ReframeControls.ZoomButton defect,
                // reproduced here by the same mechanism and caught by the same assertion.
                //
                // ⚠ **The `onClick` half of this note was wrong, and is corrected here rather than
                // deleted.** It claimed the same wipe took the *activation action* with it, so every
                // enabled verb published a button no ACTION_CLICK consumer — Switch Access, an external
                // keyboard, automation — could fire, and that `uiautomator dump` "would have read
                // `clickable=\"false\"` on all seven". Measured with the instrument that settles it, on
                // the real `AccessibilityNodeInfo`: with this line deleted the enabled verbs still report
                // `isClickable = true` (`BenchInkPresetPlatformA11yTest`). The `clickable` sits ABOVE the
                // `clearAndSetSemantics`, and its action survives; the disabled axis above genuinely does
                // not, which is what made the wrong half plausible. The line stays — it pins what the
                // action *is* if the chain is reordered — but it was never repairing a live defect, and a
                // note that says otherwise sends the next reader hunting for a bug that is not there.
                //
                // A disabled verb carries **no `clickable` at all** rather than `clickable(enabled=false)`,
                // so the platform `AccessibilityNodeInfo` reports it non-clickable too — the distinction
                // ADR-058 shipped past a green Robolectric suite.
                if (verb.enabled) {
                    onClick { fire(); true }
                    // A toggle verb ([BenchVerb.checked]) must say which way it is set, or it announces
                    // identically before and after a tap — the same class of defect as the page grid's
                    // invisible `selected` (ADR-102 §12) and ADR-058's enabled-looking disabled button.
                    // `stateDescription` rather than `Role.Switch` for exactly the reason `BenchPageGrid`
                    // gives: it reaches the platform tree whatever the role does, and the freeze draws a
                    // button, not a switch. Post-freeze accessibility work is allowed.
                    verb.checked?.let {
                        stateDescription = if (it) Copy.BenchVerbs.COPIER_ON else Copy.BenchVerbs.COPIER_OFF
                    }
                } else {
                    disabled()
                    // OD-9 keeps the control drawn; this says WHY it is dim. State, not name.
                    verb.unavailableBecause?.let { stateDescription = it }
                }
            }
            .padding(BenchContextBarButtonPaddingDp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BenchContextBarLabelGapDp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = verb.icon,
            contentDescription = null,
            // No second alpha: the layer above already carries the frozen `.35` for the whole control.
            tint = tint,
            modifier = Modifier.size(BenchContextBarIconDp),
        )
        Text(
            text = verb.label,
            color = tint,
            fontSize = BenchContextBarLabelSp,
            // `.ctx button{font-weight:600}` — V2 asked for 500.
            fontWeight = FontWeight.SemiBold,
            fontFamily = ZinelyV21Fonts.Work,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
        )
    }
}

// ── The frozen numbers, in one place so the tests read them from here and not from a second copy ─────

/**
 * Frozen `.ctx{bottom:12px}` (`v21-bench.html:223`) — and **only** bottom now. V2 also pinned
 * `left:12px;right:12px`; V2.1 replaces both with `left:50%;translateX(-50%)`, so the card is content-width
 * and centred and there is no horizontal inset to transcribe.
 */
internal val BenchContextBarInsetDp = 12.dp

/**
 * Frozen `.ctx{border-radius:var(--br-pill)}` (`v21-bench.html:225`), where V2 drew a 16dp rounded
 * rectangle. Expressed as a percent shape rather than [ZinelyV21Dimens.radiusPill]'s 999dp for the reason
 * [BenchBottomBar]'s `BenchBarShape` records: the outline has to stay exact for anything that builds
 * geometry from it.
 */
internal val BenchContextBarShape: RoundedCornerShape = RoundedCornerShape(percent = 50)

/** Frozen `.ctx{padding:var(--gap-xs)}` (`v21-bench.html:225`) — 4, where V2 padded 8. */
internal val BenchContextBarPaddingDp = ZinelyV21Dimens.gapXs

/** Frozen `.ctx{gap:var(--gap-hair)}` (`v21-bench.html:224`) — 2, where V2 gapped 6. */
internal val BenchContextBarGapDp = ZinelyV21Dimens.gapHair

/**
 * ⚠ **V2.1's `.ctx button` declares no height** (`v21-bench.html:228-230`), where V2 pinned 40dp. Its
 * intrinsic stack is `8 + 17 + 2 + ~15.4 + 8`, which lands within half a dp of **50** — the same number the
 * freeze independently gives it as `min-width`, so the verb is a 50dp square at rest.
 *
 * ### Why this is pinned where [BenchBottomBar]'s `.bar` was left intrinsic
 *
 * Because [BenchContextBarReservedHeightDp] is a **compile-time band** that `EditorScreen` subtracts from
 * the canvas *before* the bar is measured, and an intrinsic height cannot be reserved ahead of layout.
 * Left intrinsic, the reserve would be a number that merely happens to be close, which is exactly what
 * `EditorScreenGoldenTest."the sheet is fitted above the context bar"` exists to fail. Recorded as a
 * deviation from the freeze's silence rather than presented as a frozen value.
 */
internal val BenchContextBarButtonHeightDp = 50.dp

/** Frozen `.ctx button{min-width:50px}` (`v21-bench.html:230`) — the only fixed dimension the freeze gives it. */
internal val BenchContextBarButtonMinWidthDp = 50.dp

/**
 * The verb row's laid-out height. Identical to [BenchContextBarButtonHeightDp] — but kept named, because
 * *which* number this is has been got wrong from both directions (V2's drawn 40 versus Material's 48
 * interactive floor) and the name is where the answer lives. At 50dp the drawn box now clears the floor on
 * its own, so the two readings no longer diverge.
 */
internal val BenchContextBarRowHeightDp = BenchContextBarButtonHeightDp

/** Frozen `.ctx button{border-radius:var(--br-pill)}` (`v21-bench.html:229`), where V2 drew a 10dp radius. */
internal val BenchContextBarButtonShape: RoundedCornerShape = RoundedCornerShape(percent = 50)

/** Frozen `.ctx button{padding:var(--gap-sm) var(--gap-sm)}` (`v21-bench.html:229`) — 8 on every side. */
internal val BenchContextBarButtonPaddingDp = ZinelyV21Dimens.gapSm

/** Frozen `.ctx button{gap:var(--gap-hair)}` (`v21-bench.html:230`) — glyph over caption. */
internal val BenchContextBarLabelGapDp = ZinelyV21Dimens.gapHair

/** Frozen `.ctx svg{width:17px;height:17px}` (`v21-bench.html:233`) — unchanged from V2. */
internal val BenchContextBarIconDp = 17.dp

/**
 * Frozen `.ctx svg{stroke-width:1.8}` (`v21-bench.html:233`).
 *
 * ⚠ **Recorded, not applied.** The verbs still draw Material's filled icons — see the deviation note on
 * [BenchContextBar]. The number is written down so the day the stroked glyphs are drawn it is not
 * re-measured.
 */
internal const val BenchContextBarIconStroke: Float = 1.8f

/** Frozen `.ctx button{font-size:.62rem}` (`v21-bench.html:228`) — 9.92sp at the prototype's 16px root. */
internal val BenchContextBarLabelSp = 9.92.sp


/** Frozen `.ctx{transition:opacity .16s,transform .16s …}` (`v21-bench.html:226`). V2 asked for 200ms. */
internal const val BenchContextBarEnterMillis: Int = 160

/**
 * Frozen `.ctx{transition:… cubic-bezier(.2,.7,.2,1)}` (`v21-bench.html:226`).
 *
 * Not `ZinelyV2Standard`, which is `cubic-bezier(.2,0,0,1)`: V2.1 gives this bar a curve the V2 motion
 * object has no token for, so it is transcribed here rather than approximated by the nearest one. The
 * *duration* still routes through [ZinelyTheme.motion] so the reduced-motion downgrade survives.
 */
internal val BenchContextBarEnterEasing: Easing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

/** Frozen `.ctx{transform:translateY(8px)}` (`v21-bench.html:223`) — a fixed rise, not a fraction. V2 rose 14. */
internal val BenchContextBarEnterOffsetDp = 8.dp

/**
 * **The vertical band this bar occupies at the canvas's foot** — `inset + (padding + row + padding)`, where
 * `row` is the button's **layout** height, not its drawn one. Under V2.1 that is `12 + 4 + 50 + 4` = **70dp**,
 * against V2's 68.
 *
 * ### Two wrong answers, from opposite directions, before a measurement settled it
 *
 * The first cut added a **second** [BenchContextBarInsetDp] for the padding *above* the card — which is
 * transparent and occupies nothing — giving 80dp against a real band of 68. It over-reserved, so the fix
 * appeared to work and the sheet simply sat 12dp higher than it needed to.
 *
 * A review then argued the opposite error: that [BenchContextBarRowHeightDp] must be Material's **48dp**
 * interactive floor rather than V2's frozen 40. That correction would have over-reserved by 8dp instead —
 * `minimumInteractiveComponentSize` extends the *target* past the layout box without growing it. V2.1
 * retires the question by drawing a 50dp control, but the history is kept because it is why
 * [BenchContextBarButtonHeightDp] is pinned rather than left intrinsic.
 *
 * Neither was settled by argument. `EditorScreenGoldenTest` composes the bar and measures
 * `canvas.bottom - bar.top` and asserts this constant against that measurement — so the next person to
 * reason about it from the constants alone will be corrected by the suite rather than by a device.
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
