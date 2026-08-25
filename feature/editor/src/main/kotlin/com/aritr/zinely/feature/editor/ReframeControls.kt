package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.components.zinelyV21Frame
import com.aritr.zinely.ui.components.zinelyV21Pressable
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press

/** Test tag on the Reframe controls band (present only during a Reframe session). */
public const val ReframeControlsTestTag: String = "reframe-controls"

/** Test tag on the "Reframe" affordance chip shown on a selected photo. */
public const val ReframeChipTestTag: String = "reframe-chip"

/**
 * Test tag on `.padhint` — the pad's entry-state instruction. Present **only** while every nudge is dead,
 * so `assertDoesNotExist` is the assertion that the hint never goes stale (F-4).
 */
public const val ReframePadHintTestTag: String = "reframe-pad-hint"

/* ── the spec's measurements (`docs/design/mockups/v21-reframe.html`) ───────────────────────────────── */

/** `.pad` — the precision card: `--paper`, 1.5px ink, `--br-md`, a 2px printed shadow. */
private val PadRadius = ZinelyV21Dimens.radiusMd
private val PadPaddingH = ZinelyV21Dimens.gapMd
private val PadPaddingV = ZinelyV21Dimens.gapSm
private val PadGap = ZinelyV21Dimens.gapLg

/**
 * `.pad{gap:var(--gap-sm)}` in its **column** direction — the space between the control row and
 * `.padhint`. It is the card's own vertical padding value, so the hint sits inside the card's rhythm
 * rather than looking bolted under it.
 */
private val PadHintGap = ZinelyV21Dimens.gapSm

/**
 * `.padhint{font-size:12.5px}`. Sp, not dp: it is prose, and the one thing on this card that must grow
 * with the reader's text size — the controls around it are fixed 34/40px targets and do not.
 */
private val PadHintSize = 12.5.sp

/** `.nudge button{34px}` / `.nudge svg{16px}` and `.zoom button{40px}` / `.zoom svg{20px}`. */
private val NudgeCellSize = 34.dp
private val NudgeGlyphSize = 16.dp
private val NudgeGap = ZinelyV21Dimens.gapHair
private val ZoomButtonSize = 40.dp
private val ZoomGlyphSize = 20.dp
private val ZoomGap = ZinelyV21Dimens.gapSm

/** `.zoom b{min-width:46px;font-size:.78rem}` — 12.48sp at the prototype's 16px root. */
private val ZoomReadoutWidth = 46.dp
private val ZoomReadoutSize = 12.48.sp

/** `.fit{--br-md; padding:var(--gap-sm) var(--gap-md)}`, `b{.82rem}` / `span{.68rem}`. */
private val FitRadius = ZinelyV21Dimens.radiusMd
private val FitPaddingH = ZinelyV21Dimens.gapMd
private val FitPaddingV = ZinelyV21Dimens.gapSm
private val FitLineGap = 1.dp
private val FitTitleSize = 13.12.sp
private val FitSubSize = 10.88.sp

/** `.aff{--br-pill; padding:var(--gap-xs) var(--gap-sm); font-size:.7rem}`, `svg{13px}`. */
private val AffPaddingH = ZinelyV21Dimens.gapSm
private val AffPaddingV = ZinelyV21Dimens.gapXs
private val AffTextSize = 11.2.sp
private val AffGlyphSize = 13.dp

/**
 * The Reframe session's chrome, in V2.1's language
 * ([`v21-reframe.html`](../../../../../../../../docs/design/mockups/v21-reframe.html),
 * [ADR-102 §12.16](../../../../../../../../docs/DECISIONS.md#adr-102-reframe)).
 *
 * ### What this is a re-skin *of*
 *
 * The session ADR-053 specified, act for act. Every control it had, it keeps — the fit binary, the four
 * nudge arrows, the zoom steppers and their readout, an in-session Reset, Cancel and Done — and every
 * ability gate with them: a verb that cannot change anything stays visibly and audibly unavailable rather
 * than lit, tappable and inert. Nothing about the [FramingDraft] wiring moves.
 *
 * ⚠ **Reset, Cancel and Done are deliberately ungated**, and that is not an oversight in the gating above.
 * They are the ways *out* of a state where nothing else can move: a session pinned at both zoom limits with
 * no pan room would otherwise offer a maker six dead controls and no exit.
 *
 * ### What changed, and the rule behind all of it
 *
 * **A session changes your tools, not your room.** The old surface repainted the room: a floating
 * `--menu` pill over a `--desk` toolbar — two grounds the rest of V2.1 does not have — with V1's
 * `coralStrong` on Done and on the selected fit segment. V2.1 defines no coral at all, so entering
 * Reframe looked like leaving the app. Now the pad is a `--paper` card on the bench, the band is the
 * Bench's own `.bar`, and the primary is `--leaf` under `--on-leaf` like every other primary.
 *
 * **Done wears [BenchAddButton]'s clothes, ring included.** The one-ring-per-screen rule is not broken
 * by this: `.add` is not on screen during a session, Done stands where it stood and Done is the primary.
 * The ring moves with primacy rather than multiplying.
 *
 * **The fit choice is two chips, not a segmented control** — it is one binary, and the corpus already has
 * a two-state chip. The wording is unchanged and stays in outcome language — "Fill" / "crops edges",
 * "Whole photo" / "may add margins" — which the research brief found beats naming the mechanism
 * ("bleed", "trim", "safe area") for a beginner.
 *
 * ⚠ **The glyphs stay Material, deliberately.** [com.aritr.zinely.ui.theme.ZinelyV2Icons]'s contract is
 * that it *is* the frozen V2 trilogy's marks — `ZinelyV2IconsTest` asserts the set matches those three
 * files exactly and holds "nothing extra". The arrows, the ±, and the reset spiral come from a fourth
 * file, so adding them there would break that contract to win a detail no user can name. Recorded rather
 * than quietly reconciled.
 *
 * ⚠ **The pad is not optional and must not be hidden.** Inside a session the photo is moved by dragging
 * and scaled by pinching, and nothing registers a custom accessibility action for either —
 * [EditorA11y.elementCustomActions]'s move/scale verbs act on the *element*, not on the framing draft. So
 * these buttons are the single-pointer alternative WCAG 2.5.1 (Pointer Gestures, A) and 2.5.7 (Dragging
 * Movements, AA) require, and the W3C's own worked example for both is a pan/zoom surface with exactly
 * these controls. A later pass that "de-clutters" by collapsing this behind a disclosure removes a
 * conformance mechanism and calls it tidying. If it is ever collapsed, the nudge and zoom verbs must
 * exist as custom actions on the photo **first**.
 *
 * @param fit the current draft fit (drives the chips' selected state).
 * @param zoomPercent the current zoom as a whole percent, for the stepper readout.
 * @param abilities which adjustments can currently change anything — see [ReframeAbilities].
 */
@Composable
public fun ReframeControls(
    fit: FrameFit,
    zoomPercent: Int,
    abilities: ReframeAbilities,
    onFit: (FrameFit) -> Unit,
    onNudge: (dx: Int, dy: Int) -> Unit,
    onZoom: (factor: Double) -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    Column(
        modifier = modifier
            .testTag(ReframeControlsTestTag)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
    ) {
        ReframePad(
            zoomPercent = zoomPercent,
            abilities = abilities,
            onNudge = onNudge,
            onZoom = onZoom,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                // No inset padding here: this stands in the same slot as [BenchBottomBar], which takes
                // none either — the Bench applies the navigation-bar inset once, at the scaffold.
                .background(colors.desk),
            verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
        ) {
            // `.fitrow{padding:0 var(--gap-lg) var(--gap-sm)}`
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ZinelyV21Dimens.gapLg),
                horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
            ) {
                FitChip(Copy.Reframe.FILL, Copy.Reframe.CROPS_EDGES, FrameFit.FILL, fit, onFit, Modifier.weight(1f))
                FitChip(
                    Copy.Reframe.WHOLE_PHOTO,
                    Copy.Reframe.MAY_ADD_MARGINS,
                    FrameFit.WHOLE,
                    fit,
                    onFit,
                    Modifier.weight(1f),
                )
            }
            // `.bar{padding:var(--gap-sm) var(--gap-lg) var(--gap-lg)}` — the Bench's own bar, so the
            // session's actions land where the Bench's actions were.
            //
            // The top `gapSm` is transcribed here rather than left to the parent's `spacedBy`: the CSS
            // gives 8 + 8 between the chips and this row, and Done's `--frame` ring paints 5dp up into
            // that gap. A review caught the band sitting 8dp tight.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = ZinelyV21Dimens.gapLg,
                        end = ZinelyV21Dimens.gapLg,
                        top = ZinelyV21Dimens.gapSm,
                        bottom = ZinelyV21Dimens.gapLg,
                    ),
                horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReframeTextButton(Copy.Reframe.CANCEL, Copy.Reframe.CANCEL_REFRAMING, onCancel)
                // F-9: a word, not the rotate glyph. Same clothes as its neighbour, sized to its text.
                ReframeTextButton(Copy.Reframe.RESET, Copy.A11y.RESET_FRAMING, onReset)
                DoneButton(onDone, Modifier.weight(1f))
            }
        }
    }
}

/**
 * `.pad` — the precision card: the nudge cross and the zoom stepper on one sheet of paper.
 *
 * Why it is a card and not a floating pill: the corpus has exactly one raised-paper idiom (ink edge,
 * printed shadow, no blur) and this is it. V1's `--menu` ground and 6dp Material elevation were the two
 * things that made the old bar read as a different app's dialog.
 *
 * ### The entry state, and why the card is now a column
 *
 * A newly placed photo's frame is seeded to its own aspect, so at 100% Fill it overflows on neither axis and
 * [ReframeAbilities] correctly reports all four nudges *and* `Zoom out` unavailable. Five dead controls at
 * the moment the surface opens, and until F-4 the pad said nothing about why — a device pass read that as
 * breakage, which is the same finding, and the same house rule, as the Bench's drawn-and-dim verbs
 * (`docs/BETA-UX-REVIEW.md` F-1/F-4).
 *
 * The spec's answer is `.padhint` (`v21-reframe.html`, revised 2026-08-15): the card gains a column so the
 * hint takes its own line, and the controls keep the row they always had. **`flex-wrap` was tried first and
 * is wrong** — the spec records the arithmetic — which matters here only as the reason the controls keep an
 * explicit inner row instead of being allowed to reflow.
 */
@Composable
private fun ReframePad(
    zoomPercent: Int,
    abilities: ReframeAbilities,
    onNudge: (Int, Int) -> Unit,
    onZoom: (Double) -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val shape = RoundedCornerShape(PadRadius)
    /**
     * The visible hint's non-visual twin, and the **whole** accessibility half of F-4.
     *
     * The first attempt was a traversal group plus `traversalIndex = -1f` on the hint, reasoning that it
     * reproduced `aria-describedby` by having the explanation read first. **Measured, it does nothing here.**
     * Compose publishes children in declaration order and expresses re-sorting through
     * `AccessibilityNodeInfo.setTraversalBefore/After`, which is `UNDEFINED` on every node this repo's
     * harness has probed — so the platform tree still hands a service the five dead buttons first and the
     * explanation eighth. Both instruments agreed: `platformTraversalStops` and a device
     * `uiautomator dump` returned the identical order with the index set and with it removed.
     *
     * So the reason goes **on the controls**, which is the remedy the corpus already uses for exactly this
     * (F-1's `stateDescription` on the Bench's dim verbs) and needs no ordering to work: whichever dead
     * control the maker reaches first tells them what to do. Null whenever the visible hint is absent, so
     * the two can never disagree — one condition, two channels.
     */
    val padReason = Copy.Reframe.ZOOM_IN_TO_MOVE
        .takeIf { abilities.zoomIn && !abilities.panHorizontally && !abilities.panVertically }
    Column(
        modifier = Modifier
            // Nothing that clips may sit left of the shadow — it paints outside the node.
            .zinelyV21Pressable(false, ZinelyV21Press.Flat, colors.inkLine, shape)
            .clip(shape)
            .background(colors.surface)
            .border(BenchChromeBorder, colors.ink, shape)
            .padding(horizontal = PadPaddingH, vertical = PadPaddingV)
            // The spec's `role="group" aria-label="Framing controls"` is deliberately NOT transcribed as a
            // `contentDescription` here: that would merge the seven controls away. The group's real purpose
            // — `aria-describedby`, so the explanation is heard on entering rather than after five dead
            // buttons — is bought instead by putting the reason ON the dead controls; see [padReason].
            .semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(PadHintGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PadGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NudgePad(abilities = abilities, onNudge = onNudge, unavailableBecause = padReason)
            // Deliberately NOT given [padReason]: `Zoom out` is dead because 100% is the floor, which is a
            // different sentence from the nudges', and "Zoom in to move the photo" on the zoom-OUT button
            // would be nonsense. The nudges are what the hint is about.
            ZoomStep(zoomPercent = zoomPercent, abilities = abilities, onZoom = onZoom)
        }
        // Frozen: shown only while EVERY nudge is unavailable, and **not composed at all** the moment one
        // goes live — not merely hidden. A stale instruction that is still in the tree is worse than none:
        // TalkBack would read "Zoom in to move the photo" on a pad whose arrows already work.
        //
        // ⚠ The `zoomIn` term is NOT in the spec's sentence, and it is load-bearing. The spec asserts the
        // hint "can never contradict itself by advising an unavailable control" because `Zoom in` is live at
        // the entry state — true for Fill, and false one tap away: `Framing.abilities()` returns
        // `ReframeAbilities.NONE` for `FrameFit.WHOLE`, and `EditorScreen` uses the same NONE for the inert
        // state before the photo's aspect resolves (M7-01). Both leave every nudge dead with `Zoom in` dead
        // too, and the hint would then point at a control the maker cannot press. Found by review; the
        // condition names what the sentence actually needs rather than what the entry state happens to have.
        if (padReason != null) {
            Text(
                text = Copy.Reframe.ZOOM_IN_TO_MOVE,
                // `.padhint{color:var(--ink-soft)}` — the pad's own quiet ink, deliberately NOT jam. It is
                // an instruction, and an instruction in a warning colour is read as a failure.
                color = colors.inkSoft,
                fontSize = PadHintSize,
                fontFamily = ZinelyV21Fonts.Work,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(ReframePadHintTestTag),
            )
        }
    }
}

/**
 * The cross-shaped 3×3 nudge pad: Up / Left / Right / Down on the cross, the corners inert spacers. 2D
 * position is two axes of discrete targets — not one 1-D adjustable.
 */
@Composable
private fun NudgePad(
    abilities: ReframeAbilities,
    onNudge: (Int, Int) -> Unit,
    unavailableBecause: String? = null,
) {
    // Per axis, not per arrow: pan room is symmetric about the centre, and the clamp that would stop a
    // rightward nudge is the same one that stops a leftward one.
    val h = abilities.panHorizontally
    val v = abilities.panVertically
    // No group-level semantics wrapper: each cell carries its own spoken label (a parent
    // clearAndSetSemantics would clear the children TalkBack + the a11y tests navigate to).
    Column(verticalArrangement = Arrangement.spacedBy(NudgeGap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(NudgeGap)) {
            NudgeSpacer()
            NudgeCell(Icons.Filled.ArrowUpward, Copy.Reframe.MOVE_PHOTO_UP, v, unavailableBecause) { onNudge(0, -1) }
            NudgeSpacer()
        }
        // `Icons.Filled`, deliberately not the AutoMirrored arrows the deprecation warning asks for: these
        // name a physical direction, not a reading order. Under RTL the mirrored glyph would point right
        // on the button that moves the photo left.
        Row(horizontalArrangement = Arrangement.spacedBy(NudgeGap)) {
            NudgeCell(Icons.Filled.ArrowBack, Copy.Reframe.MOVE_PHOTO_LEFT, h, unavailableBecause) { onNudge(-1, 0) }
            NudgeSpacer()
            NudgeCell(Icons.Filled.ArrowForward, Copy.Reframe.MOVE_PHOTO_RIGHT, h, unavailableBecause) { onNudge(1, 0) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NudgeGap)) {
            NudgeSpacer()
            NudgeCell(Icons.Filled.ArrowDownward, Copy.Reframe.MOVE_PHOTO_DOWN, v, unavailableBecause) { onNudge(0, 1) }
            NudgeSpacer()
        }
    }
}

/** One 34dp cross cell: `--paper` under a 1.5dp ink edge, a printed shadow, a decorative glyph. */
@Composable
private fun NudgeCell(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    unavailableBecause: String? = null,
    onClick: () -> Unit,
) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val nudge = benchTap(action = onClick)
    val shape = RoundedCornerShape(ZinelyV21Dimens.radiusSm)
    Box(
        modifier = Modifier
            .testTag("reframe-$description")
            .size(NudgeCellSize)
            // `.nudge button:disabled{opacity:.35;box-shadow:none}` — the corpus's one disabled
            // convention, on the WHOLE chip (edge, fill and glyph), ahead of the paint modifiers so the
            // layer wraps them all. V1 faded to .4 here; V2.1's `--disabledAlpha` is .35 everywhere.
            .alpha(if (enabled) 1f else ZinelyV21Dimens.disabledAlpha)
            // A disabled control loses its shadow too, so it does not sit proud of the card it cannot act on.
            .then(
                if (enabled) {
                    Modifier.zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, shape)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(colors.surface)
            .border(BenchChromeBorder, colors.ink, shape)
            // `semantics`, NOT `clearAndSetSemantics`: the latter wipes the `disabled` flag that
            // `clickable(enabled = false)` sets, so an unavailable control would still announce itself as
            // actionable — a screen-reader user would be told to tap something that cannot respond.
            // Verified on a physical device: this reaches the platform as
            // `class=android.widget.Button, clickable=true, enabled=false`. The role rides the clickable
            // (see [ZoomButton] for what happens when it does not), and the glyph is an
            // `Icon(contentDescription = null)`, so nothing forces a second node.
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = nudge,
            )
            .semantics {
                contentDescription = description
                // F-4's non-visual half: a dead arrow says what would revive it. State, never the name.
                if (!enabled) unavailableBecause?.let { stateDescription = it }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.inkSoft, modifier = Modifier.size(NudgeGlyphSize))
    }
}

/** An inert corner of the cross: keeps the grid square, takes no input. */
@Composable
private fun NudgeSpacer() {
    Spacer(Modifier.size(NudgeCellSize))
}

/** The zoom stepper: − · readout · + . */
@Composable
private fun ZoomStep(zoomPercent: Int, abilities: ReframeAbilities, onZoom: (Double) -> Unit) {
    val colors = ZinelyTheme.v21Colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(ZoomGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ZoomButton(Icons.Filled.Remove, Copy.Reframe.ZOOM_OUT, abilities.zoomOut) { onZoom(1.0 / Framing.ZOOM_STEP) }
        Text(
            text = Copy.Reframe.zoomPercentText(zoomPercent),
            // A number, so it is quiet and tabular — `--ink-soft`, never `--ink-faint`, which fails AA
            // at this size on paper (the finding ADR-102 §12.5 records against the page grid's card).
            color = colors.inkSoft,
            fontWeight = FontWeight.Bold,
            fontSize = ZoomReadoutSize,
            fontFamily = ZinelyV21Fonts.Work,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
            // `font-variant-numeric:tabular-nums` — so 100% and 132% do not shuffle the pad's width
            // as the stepper runs. The `min-width:46px` alone does not stop the digits dancing.
            style = TextStyle(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = ZoomReadoutWidth)
                .clearAndSetSemantics { contentDescription = Copy.Reframe.zoomPercentAnnouncement(zoomPercent) },
        )
        ZoomButton(Icons.Filled.Add, Copy.Reframe.ZOOM_IN, abilities.zoomIn) { onZoom(Framing.ZOOM_STEP) }
    }
}

/**
 * A 40dp zoom step: a pill of `--paper` under an ink edge, a +/− glyph.
 *
 * **The glyph is an [Icon], not a `Text`, and that is an accessibility decision rather than a visual one.**
 * A physical-device check of the platform tree found this control arriving as *three* nodes — the click
 * and its `disabled` flag on one, the spoken label on a second, the button role on a third — because a
 * `Text` child contributes semantics of its own and stops the chain collapsing. TalkBack lands on the
 * labelled node, which reports `enabled=true`, so a disabled zoom step announced itself as available: D3's
 * whole point, lost at the last hop. Marking the `Text` decorative was not enough; a cleared node is still
 * a node. [NudgeCell] never had the fault because `Icon(contentDescription = null)` contributes nothing,
 * so this uses exactly that shape and collapses to one `android.widget.Button` carrying label, role and
 * disabled state together.
 *
 * The Compose test tree cannot see any of this — it reports the merged node, where everything resolves
 * correctly, which is why `assertIsNotEnabled` passed throughout against a control that was telling the
 * platform otherwise.
 */
@Composable
private fun ZoomButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val zoom = benchTap(action = onClick)
    val shape = BenchBarShape
    Box(
        modifier = Modifier
            .testTag("reframe-$description")
            .size(ZoomButtonSize)
            .alpha(if (enabled) 1f else ZinelyV21Dimens.disabledAlpha)
            .then(
                if (enabled) {
                    Modifier.zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, shape)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(colors.surface)
            .border(BenchChromeBorder, colors.ink, shape)
            // `role` on the clickable itself, and the glyph explicitly stripped of semantics.
            //
            // Both are load-bearing, and a physical-device check is what found it: with the role in a
            // trailing `semantics {}` block and a plain `Text` child, this button reached the platform as
            // `class=android.view.View, clickable=false, enabled=true` — no button role, no click action
            // and, fatally for D3, **no disabled state** — while the `−` glyph leaked as its own
            // traversable TextView.
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = zoom,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.inkSoft, modifier = Modifier.size(ZoomGlyphSize))
    }
}

/**
 * One fit chip: a plain-language title over a small helper line, `--paper` at rest and `--leaf` when on.
 *
 * The spoken label stays just the title — the helper line is disclosure, and a screen reader that read
 * "Fill, crops edges" on every pass would be reading the manual aloud twice.
 *
 * ⚠ **`selected` is load-bearing, and it is the *only* thing that may set this chip's state.** Compose
 * supplies *"Selected"* / *"Not selected"* as the platform `stateDescription` for a node carrying
 * `SemanticsProperties.Selected` — but **only when no explicit `stateDescription` is set**, because a
 * state description replaces the platform's rather than adding to it. That is not a guess: an explicit
 * one was written here first, on the theory that `Role.Button` never carried the state at all, and
 * deleting it again left `ReframeControlsRolePlatformA11yTest` green. The platform was already saying it.
 *
 * The theory came from reading `uiautomator dump`, where both chips print `selected="false"` — true, and
 * irrelevant: `isSelected` is a different attribute from the spoken state, Compose maps `Selected` onto it
 * for `Role.Tab` alone, and the XML does not print `stateDescription` at all. **A dump that does not show
 * a thing is not evidence the thing is absent**, which is the reverse of the lesson `BenchPageGrid`'s
 * cells taught and cost about as much.
 *
 * So the chip keeps V1's semantics unchanged, and the standing hazard is written down instead: the day
 * this chip wants to say anything else about itself — the way an element outside the printer's reach does
 * ([Copy.A11y.outsidePrintReachState]) — that string must carry *"Selected"* with it or it silences this
 * one. [ReframeControlsRolePlatformA11yTest.the_platform_is_told_which_fit_is_chosen] is the guard.
 */
@Composable
private fun FitChip(
    label: String,
    sublabel: String,
    value: FrameFit,
    current: FrameFit,
    onFit: (FrameFit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    val isSel = value == current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // A fit is a selection landing, like a page or a swatch.
    val choose = benchTap(ZinelyHaptic.Snap) { onFit(value) }
    val shape = RoundedCornerShape(FitRadius)
    Column(
        modifier = modifier
            .testTag("reframe-fit-$label")
            .zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, shape)
            .clip(shape)
            .background(if (isSel) colors.leaf else colors.surface)
            .border(BenchChromeBorder, colors.ink, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = choose)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                selected = isSel
                onClick { choose(); true }
            }
            .padding(horizontal = FitPaddingH, vertical = FitPaddingV),
        verticalArrangement = Arrangement.spacedBy(FitLineGap),
    ) {
        Text(
            text = label,
            color = if (isSel) colors.onLeaf else colors.ink,
            fontSize = FitTitleSize,
            fontWeight = FontWeight.Bold,
            fontFamily = ZinelyV21Fonts.Work,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
        )
        Text(
            text = sublabel,
            color = if (isSel) colors.onLeaf else colors.inkSoft,
            fontSize = FitSubSize,
            fontFamily = ZinelyV21Fonts.Work,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
        )
    }
}

/**
 * `.text-btn` — a secondary session action, **with its word**.
 *
 * ### Both of them carry one, and the second took a device pass to earn
 *
 * A first draft made both `.icon-btn`s, which put an unlabelled ✕ beside an unlabelled ↻ on the one
 * surface where the difference between *throw this away* and *start it over* is the whole decision. V1,
 * for all its coral, drew the word. Cancel was given its word then — and Reset kept the glyph, which
 * F-9 found out was not good enough: the circular arrow is the **rotate** glyph on the Bench's own
 * transform row, one surface away. Same glyph family, different act, adjacent surfaces.
 *
 * Both defects were visual only; the spoken labels were right throughout, which is why neither showed up
 * in any accessibility assertion. The spec was amended first in both cases (`v21-reframe.html`
 * `.text-btn`, revised 2026-08-15) and the Compose follows it, never the reverse.
 *
 * @param word what is drawn — short, because it sits in a bar with two neighbours.
 * @param spokenLabel what is announced, and deliberately the **long** form: "Reset" and "Cancel" alone
 *   are ambiguous out of context, and a screen reader has no bar to read them in.
 */
@Composable
private fun ReframeTextButton(word: String, spokenLabel: String, onClick: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val act = benchTap(action = onClick)
    val shape = BenchBarShape
    Box(
        modifier = Modifier
            // Keyed on the SPOKEN label, not the word: it is the stable identifier (it survived F-9
            // unchanged, which is the whole point of the finding) and it is what the existing suites
            // already address these two controls by.
            .testTag("reframe-$spokenLabel")
            .height(BenchIconBtnSize)
            .zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, shape)
            .clip(shape)
            .background(colors.surface)
            .border(BenchChromeBorder, colors.ink, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = act)
            .clearAndSetSemantics {
                contentDescription = spokenLabel
                role = Role.Button
                onClick { act(); true }
            }
            .padding(horizontal = ZinelyV21Dimens.gapMd),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = word,
            color = colors.ink,
            fontSize = FitTitleSize,
            fontWeight = FontWeight.Bold,
            fontFamily = ZinelyV21Fonts.Work,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
        )
    }
}

/**
 * The primary Done — `.add`'s clothes, ring included ([ReframeControls]'s KDoc says why that does not
 * break the one-ring-per-screen rule). Commits the session ([Intent.CommitReframe]).
 */
@Composable
private fun DoneButton(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val done = benchTap(action = onDone)
    Row(
        modifier = modifier
            .height(BenchAddHeight)
            // Ring first so it lands under the shadow; neither may sit right of a clip.
            .zinelyV21Frame(colors.butterTint, BenchBarShape)
            .zinelyV21Pressable(pressed, ZinelyV21Press.Hero, colors.inkLine, BenchBarShape)
            .clip(BenchBarShape)
            .background(colors.leaf)
            .border(BenchChromeBorder, colors.ink, BenchBarShape)
            .clickable(interactionSource = interaction, indication = null, onClick = done)
            .testTag("reframe-${Copy.Reframe.DONE_REFRAMING}")
            .clearAndSetSemantics {
                contentDescription = Copy.Reframe.DONE_REFRAMING
                role = Role.Button
                onClick { done(); true }
            },
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = colors.onLeaf,
            modifier = Modifier.size(BenchAddGlyphSize),
        )
        Text(
            text = Copy.Reframe.DONE,
            color = colors.onLeaf,
            fontSize = BenchAddTextSize,
            fontWeight = FontWeight.Bold,
            fontFamily = ZinelyV21Fonts.Work,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
        )
    }
}

/**
 * The persistent "Reframe" affordance chip (ADR-053 RF2): a selected photo always advertises that it can
 * be reframed, so the pan/zoom gesture is discoverable without a missing handle. Tapping it enters
 * Reframe ([Intent.BeginReframe]).
 *
 * ⚠ **It is `--paper` on an ink edge now, not translucent ink over the photo.** V1 laid `ink` at 62%
 * over the picture, which is a scrim — and a scrim's contrast depends on the photo underneath it, so the
 * one thing this chip must do (be legible on *any* photo) was the one thing it could not promise. An
 * opaque paper chip on a printed shadow reads the same over a white sky and a black jacket, and it is the
 * same chip the rest of V2.1 uses.
 *
 * @param teach the first-run coach-mark: pulse twice to draw the eye the first time a photo is selected
 *   on this install. The caller passes `false` under reduced motion (WCAG 2.3.3) and once the coach has
 *   been seen — so the pulse is opt-in and never reaches an animation-averse user.
 */
@Composable
public fun ReframeAffordanceChip(onClick: () -> Unit, modifier: Modifier = Modifier, teach: Boolean = false) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val open = benchTap(action = onClick)
    // Two gentle scale pulses, then rest. Finite — not an infinite transition — so it teaches once and
    // stops; `teach` is already reduced-motion-gated by the caller, so no motion here at all when
    // animations are off.
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(teach) {
        if (teach) repeat(2) { pulse.animateTo(1.08f, tween(300)); pulse.animateTo(1f, tween(600)) }
    }
    val shape = BenchBarShape
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
            .zinelyV21Pressable(pressed, ZinelyV21Press.Flat, colors.inkLine, shape)
            .clip(shape)
            .background(colors.surface)
            .border(BenchChromeBorder, colors.ink, shape)
            .testTag(ReframeChipTestTag)
            // Click via Modifier.clickable (OUTSIDE clearAndSetSemantics), not Surface(onClick) — the
            // latter buries the click action inside the cleared-semantics boundary, so a test/AT click
            // never lands.
            .clickable(interactionSource = interaction, indication = null, onClick = open)
            .clearAndSetSemantics {
                contentDescription = Copy.Reframe.REFRAME_THIS_PHOTO
                role = Role.Button
                onClick { open(); true }
            }
            .padding(horizontal = AffPaddingH, vertical = AffPaddingV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapXs),
    ) {
        Icon(Icons.Filled.CropFree, contentDescription = null, tint = colors.ink, modifier = Modifier.size(AffGlyphSize))
        Text(
            text = Copy.Reframe.REFRAME,
            color = colors.ink,
            fontSize = AffTextSize,
            fontWeight = FontWeight.Bold,
            fontFamily = ZinelyV21Fonts.Work,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
        )
    }
}
