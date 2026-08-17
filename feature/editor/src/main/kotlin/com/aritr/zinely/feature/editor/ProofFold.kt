package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.theme.ZinelyEasing
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import kotlin.math.hypot

// Test tags for the Act 3 body the ProofScreen suite and golden address.
public const val ProofFoldGuideTestTag: String = "proof-fold-guide"
public const val ProofFoldDiagramTestTag: String = "proof-fold-diagram"
public const val ProofStepPrevTestTag: String = "proof-step-prev"
public const val ProofStepNextTestTag: String = "proof-step-next"
public const val ProofStepTitleTestTag: String = "proof-step-title"
public const val ProofFoldLegendTestTag: String = "proof-fold-legend"
public const val ProofStepHoldingTestTag: String = "proof-step-holding"
public const val ProofStepDotsTestTag: String = "proof-step-dots"

/** One `.stepdots` button, addressed by zero-based step index. */
public fun proofStepDotTag(step: Int): String = "proof-step-dot-$step"

/**
 * **The eight frozen fold steps** (`v21-proof.html` `STEPS`, DESIGN-FROZEN) — ADR-101 P4.
 *
 * Five were built. Eight is not a longer version of five: the built step 1 was *"fold the sheet in half
 * three times, then open it flat"*, which is three physical actions in one instruction given to someone
 * holding a sheet of paper for the first time. [V21-SPEC §5.2](../../../../../../../docs/design/V21-SPEC.md)
 * makes one action per step the rule, and the frozen sequence separates those three folds and the unfold
 * into steps 1–4. Copy lives in `Copy.ProofFold.STEP_CAPTIONS` / `STEP_HOLDING`.
 */
internal val PROOF_FOLD_STEP_COUNT: Int = Copy.ProofFold.STEP_CAPTIONS.size

internal const val FOLD_LAST_STEP: Int = 7 // the finish-hand-off step (eight steps, zero-based).

/**
 * **The fold drawer** (`v21-proof.html` `#foldDrawer`, DESIGN-FROZEN) — the eight-step fold walkthrough of
 * V21-SPEC §5.3. A schematic diagram in the frozen paper/crease/cut/move/act vocabulary, a live-region
 * caption plus a *"you should be holding…"* line, and a prev/dots/next nav whose dots are themselves
 * buttons. The step pointer ([step]) and advance/retreat ([onNext]/[onPrev]/[onGoToStep]) are hoisted to
 * [ProofScreen], which owns the finish action on the last step. ←/→ keys are handled at that root.
 *
 * ### The finished-book climax was retired here, on purpose
 *
 * This drawer used to have a second face: on *"It's folded — show me"* it replaced the guide with a
 * revealed book — a cover swinging shut in timed beats, a shelf-line drawing under it, words, then two
 * exits. It was described in this file as *the signature climax, the whole delight budget*, and
 * [ADR-101 §5](../../../../../../../docs/DECISIONS.md#adr-101-open) held it open pending an owner ruling.
 * The ruling went against it, for four reasons that are worth keeping written down:
 *
 * 1. **It answers a question the user is not holding.** They have just folded a real booklet and are
 *    holding it. The screen showed them a *schematic drawing* of a booklet. That is
 *    [ADR-058](../../../../../../../docs/DECISIONS.md#adr-058)'s "Preview" failure exactly — a good answer
 *    at the wrong moment — and the phone showing you a worse copy of the thing in your hand is the most
 *    literal form of it this codebase has produced.
 * 2. **Its two exits went to the same place.** `onMakeAnother` was wired to `onBack` in the nav host, so
 *    *"Make another"* — the loud, filled one, at the emotional peak — promised a new zine and returned to
 *    the bench, beside a quiet *"Back to bench"* that did the identical thing.
 * 3. **The frozen design has no fold terminal state at all.** Completion lives in the band's `.done`, on
 *    **save** — an event the app can verify. *"It's folded"* is a claim it cannot.
 * 4. **It cost a great deal to keep.** A five-beat schedule, a `climaxBeat` saveable, focus
 *    choreography, a deferred tap-to-skip, and a state with no control that left it — which shipped as a
 *    real defect: two of three entry points re-opened onto the climax of a fold already done.
 *
 * What replaces it is what the frozen guide does: it ends. The last step's primary acknowledges and closes
 * the drawer, and the user lands back on the zine they made. The payoff is the booklet in their hand; the
 * phone's contribution is to say *done* and get out of the way.
 *
 * Stateless: all state lives in [ProofScreen].
 */
@Composable
internal fun ProofFoldAct(
    step: Int,
    reduceMotion: Boolean,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onGoToStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FoldGuide(
        step = step,
        reduceMotion = reduceMotion,
        onNext = onNext,
        onPrev = onPrev,
        onGoToStep = onGoToStep,
        modifier = modifier,
    )
}

/**
 * The eight-step guide: stepline, staged diagram, live-region caption, prev/dots/next nav.
 *
 * **The legend is the reason this reads at a glance**, and it was not built before P4. Three marks whose
 * meaning never changes between steps — grey dashed *crease*, green *fold now*, red *cut* — so a user
 * decoding step 6 does not have to re-learn what a line means. Without it the diagrams are eight separate
 * puzzles ([V21-SPEC §5.2](../../../../../../../docs/design/V21-SPEC.md)).
 */
/**
 * The tallest caption and the tallest holding line, used as an invisible size-setter so the controls
 * beneath them never move between steps. Character count stands in for rendered height: the two texts
 * share one style and one width, so the longest string is the tallest block at every font scale.
 */
private val FOLD_LONGEST_CAPTION: String = Copy.ProofFold.STEP_CAPTIONS.maxBy { it.length }
private val FOLD_LONGEST_HOLDING: String = Copy.ProofFold.STEP_HOLDING.maxBy { it.length }

/** The `.foldcap` + `.foldnow` pair. Rendered twice per step — once invisibly, to fix the height. */
@Composable
private fun FoldCaptionBlock(
    caption: String,
    holding: String,
    colors: com.aritr.zinely.ui.theme.ZinelyV21Colors,
    modifier: Modifier = Modifier,
    // The invisible copy carries no test tag. `clearAndSetSemantics` hides it from the *merged* tree,
    // which is what a screen reader walks — but the unmerged tree the suite queries still sees the
    // children, so a tag here would make `ProofStepHoldingTestTag` ambiguous.
    tagged: Boolean = true,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        BasicText(
            text = caption,
            style = TextStyle(
                color = colors.ink,
                fontFamily = ZinelyTheme.typography.voice,
                fontSize = 14.5.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            ),
        )
        BasicText(
            text = holding,
            modifier = if (tagged) Modifier.testTag(ProofStepHoldingTestTag) else Modifier,
            style = TextStyle(
                // `leafText`, not `leaf` — the frozen `.foldnow{color:var(--leaf-text)}`, and the
                // palette's own split: `leaf` is the action colour as a *fill*, `leafText` is the
                // AA-critical text cut. On the drawer's ground that is 4.74:1 against 6.53:1 in light —
                // the tightest text pairing on the surface, and the one place to not be clearing the
                // floor by 0.24.
                color = colors.leafText,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun FoldGuide(
    step: Int,
    reduceMotion: Boolean,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onGoToStep: (Int) -> Unit,
    modifier: Modifier,
) {
    val colors = ZinelyTheme.v21Colors

    // **The step content scrolls; the navigation does not.** Both halves of that are device findings.
    //
    // The nav used to be the last thing in one scrolling Column, directly under the caption — so its
    // position depended on how tall the step above it happened to be. On a real phone the guide's
    // precondition line disappears after step 1, the whole column rose **108px**, and the Next arrow
    // landed exactly where the dots row now sits. A user tapping twice in the same place — which is what
    // *"tap the arrow when a step is done"* invites — presses Next once and then a **dot**, and at the
    // right-hand end of the row that dot is Step 8. Two taps, guide skipped. It happened to this pass's
    // own automation before it happened to a person, which is the only reason it was caught.
    //
    // **But pinning them to the bottom of an 88%-tall drawer is not the same as fixing the shift, and
    // the re-skin's device pass is what separated the two.** The first fix took the whole body height and
    // pushed the controls to the floor of it, which moved the dead space rather than removing it: the
    // caption ended a third of the way down a very tall sheet and the arrow it tells you to tap sat some
    // 450px below, next to the navigation bar. Read cold, that is not breathing room — the drawer looks
    // like a page whose content failed to load.
    //
    // What the shift actually needed was a *constant* control position, not a floor. So the guide now
    // wraps its content (the drawer shrinks to it), and the caption block reserves the tallest caption's
    // height — three lines at 360dp — which holds the nav still across all eight steps without spending
    // the rest of the screen to do it.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 2.dp, bottom = 12.dp)
            .testTag(ProofFoldGuideTestTag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                // `fill = false`: the content takes only what it needs, and the weight is here purely as
                // a cap — when a large font scale or a landscape window makes the step taller than the
                // drawer's bound, this column is what gives, and it scrolls. The controls below it are
                // never the thing that gets pushed off.
                //
                // **Invariant: this guide must be given a bounded height.** A weight resolves against the
                // parent's max, so under an unbounded one — a scrolling ancestor, a preview — `Column`
                // resolves it against the main-axis *minimum* and the whole step body collapses to zero.
                // [ProofScreen]'s drawer supplies the bound via `drawerBodyMaxHeight()`.
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        // .stepline — the counter, then the legend beneath it. The frozen row puts both on one line, and
        // that stopped fitting once the legend gained its fourth mark: counter plus four chips overruns a
        // 360dp window, and this drawer clips. Stacking is the departure that keeps all five readable.
        BasicText(
            text = Copy.ProofFold.stepOf(step + 1, PROOF_FOLD_STEP_COUNT).uppercase(),
            modifier = Modifier.fillMaxWidth().widthIn(max = 380.dp).testTag(ProofStepTitleTestTag),
            style = TextStyle(
                color = colors.inkSoft,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.12.em,
            ),
        )
        FoldLegend()

        FoldDiagram(step = step, reduceMotion = reduceMotion)

        // .foldcap + .foldnow — one live region, so a step change is announced as one sentence: what to
        // do, then what you should be holding when it is done.
        //
        // **The block is as tall as the tallest step, always** — that is what holds the nav still, and it
        // is measured rather than reserved.
        //
        // A `heightIn(min = …)` literal was the first answer and it is the wrong shape of answer. Sized
        // for three lines at fontScale 1.0 it stops holding three lines at about 1.15; sized in `sp` so
        // the *line height* scales, it still breaks, because at 1.3 the longest caption needs a fourth
        // line at the same width — the line **count** grows too, and no arithmetic over a fixed count
        // survives that. `ProofFoldNarrowTest`'s fontScale-1.3 case measured the residue: 41.5dp of shift
        // between steps 1 and 2, i.e. the original defect, in the configuration nothing had tested.
        //
        // So the longest caption is laid out invisibly underneath the real one and sets the height. It is
        // the same text, in the same style, at the same width, so it is correct at every font scale, in
        // every locale, for any future caption — and it costs one extra text measurement per step.
        Box(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            contentAlignment = Alignment.TopCenter,
        ) {
            // The size-setter. `alpha = 0f` rather than absent, `clearAndSetSemantics` so no screen
            // reader ever meets it, and the longest caption + longest holding line by rendered length.
            FoldCaptionBlock(
                caption = FOLD_LONGEST_CAPTION,
                holding = FOLD_LONGEST_HOLDING,
                colors = colors,
                modifier = Modifier.graphicsLayer { alpha = 0f }.clearAndSetSemantics { },
                tagged = false,
            )
            FoldCaptionBlock(
                caption = Copy.ProofFold.STEP_CAPTIONS[step],
                holding = Copy.ProofFold.STEP_HOLDING[step],
                colors = colors,
            )
        }
        }

        Spacer(Modifier.height(10.dp))

        // .foldnav — prev · next on one row, the eight dots on their own beneath.
        //
        // **The frozen single row does not fit on the phone this app is mostly used on, and no amount of
        // flexing makes it.** It laid out 52 + 18 + (8 × 26) + 18 + 52 = 348dp of intrinsic width; a 360dp
        // phone leaves 284dp inside the sheet's and the guide's padding, and `ZSheet` clips — so both
        // arrows lost more than half their touch target on all eight steps. The suite could not see it:
        // `@Config(w430dp)` is the one width where 348 fits.
        //
        // Flexing the dots inside that row was the first fix and it was **also wrong**, just less visibly:
        // 284 − 44 − 44 − 12 = 184dp across eight dots is 23dp each, under even WCAG 2.5.8's 24×24 floor
        // (they are adjacent, so the spacing exception does not apply) and nowhere near this project's
        // 44dp bar. Splitting the row buys the dots the full width — 35dp each at 360dp — for about 14dp
        // of vertical space in a drawer that already scrolls. That is the cheap side of the trade.
        Row(
            modifier = Modifier.fillMaxWidth().widthIn(max = 380.dp).padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepNavButton(
                pathData = ICON_STEP_PREV,
                contentDescription = Copy.ProofFold.PREVIOUS_STEP,
                enabled = step > 0,
                onClick = onPrev,
                testTag = ProofStepPrevTestTag,
            )
            // The next arrow gives way to the drawer's finish button on the last step (one finish action,
            // never a dead primary). A fixed-size spacer holds its place so the row does not shift under
            // the thumb at the moment the user is looking at the finish button.
            if (step < FOLD_LAST_STEP) {
                StepNavButton(
                    pathData = ICON_STEP_NEXT,
                    contentDescription = Copy.ProofFold.NEXT_STEP,
                    enabled = true,
                    onClick = onNext,
                    testTag = ProofStepNextTestTag,
                )
            } else {
                Box(Modifier.size(44.dp))
            }
        }
        StepDots(
            step = step,
            onGoToStep = onGoToStep,
            modifier = Modifier.fillMaxWidth().widthIn(max = 380.dp).padding(bottom = 4.dp),
        )
    }
}

/**
 * `.legend` — the marks, drawn in the diagram's own shapes so the mapping is legible at a glance.
 *
 * **A swatch and the mark it names must be the same colour, and after the re-skin they can be.** While
 * this surface still painted in V1, they could not: the legend sat on the drawer (`menu`) and the diagram
 * on the sheet (`paper`), V1's `ink` deliberately does not flip with the theme, and drawing the `move`
 * swatch in it to match the arrow put the swatch at 1.13:1 on the dark drawer — §6.7 item 6's own defect,
 * inverted. The re-skin removed the split: this function reads [ZinelyTheme.v21Colors], where `ink` flips
 * (`#33261C` / `#F6EAD6`) and the drawer ground *is* `paper`, exactly as the CSS assumes. So every swatch
 * below is the token its mark is drawn in, with no second palette to reconcile.
 *
 * **It gained a fourth mark, because three made it a lie.** The frozen legend says *crease · fold now ·
 * cut*, and the first build drew every arrow in the same red as the cut — so red meant *cut here* on one
 * step and *this paper travels* on seven, including step 4, whose own line reads *"Creases only — nothing
 * is cut yet"* beneath a fat red arrow. A legend whose marks change meaning between steps is worse than
 * none: it teaches a rule and then breaks it at the only irreversible action in the product. Motion and
 * action arrows are `ink` now — a drawn pencil mark — red is the cut and only the cut, and *move* is
 * named. `v21-proof.html` was amended first (`.mstem/.mhead/.hollow` → `var(--ink)`, a fourth `<i>`).
 */
@Composable
private fun FoldLegend() {
    val colors = ZinelyTheme.v21Colors
    // **Wrapping, and that is a fix in its own right.** Four chips plus swatches measure ~295dp against
    // the 284dp a 360dp phone leaves inside the drawer, so the row was already clipping before the fifth
    // mark — invisibly, because the suite runs at `w430dp`, the one width where it fits. Same blindness
    // `drawerBodyMaxHeight` records on the other axis, same place, found the same way.
    FlowRow(
        modifier = Modifier.fillMaxWidth().widthIn(max = 380.dp).testTag(ProofFoldLegendTestTag),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LegendMark(Copy.ProofFold.LEGEND_CREASE, colors.inkFaint, dashed = true, thickness = 2.4f)
        LegendMark(Copy.ProofFold.LEGEND_FOLD_NOW, colors.leaf, dashed = false, thickness = 3f)
        LegendMark(Copy.ProofFold.LEGEND_CUT, colors.jam, dashed = true, thickness = 2.4f)
        LegendMark(Copy.ProofFold.LEGEND_MOVE, colors.ink, dashed = false, thickness = 2.4f)
        // The action mark. Hollow, so the swatch *is* the mark rather than a coloured stand-in for it —
        // the two arrows differ by fill, not by colour, so a filled swatch would name the wrong one.
        LegendMark(Copy.ProofFold.LEGEND_ACT, colors.ink, dashed = false, thickness = 2f, hollow = true)
    }
}

/** The air inside a hollow swatch — the frozen `.legend i.act::before{height:3px}`. */
private val HOLLOW_GAP = 3.dp

@Composable
private fun LegendMark(
    text: String,
    color: Color,
    dashed: Boolean,
    thickness: Float,
    hollow: Boolean = false,
) {
    val colors = ZinelyTheme.v21Colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Hollow is two rules of `thickness` with `HOLLOW_GAP` of air between them, so the frozen
        // `.legend i.act::before` (2px top + 2px bottom + 3px height) is `thickness = 2f` here and nothing
        // is hardcoded. The first version drew 1.4dp rules and discarded the caller's `thickness` — a
        // parameter that looks load-bearing and is not is worse than no parameter.
        Canvas(Modifier.size(13.dp, if (hollow) thickness.dp * 2 + HOLLOW_GAP else thickness.dp)) {
            if (hollow) {
                val t = thickness.dp.toPx()
                drawLine(color, Offset(0f, t / 2f), Offset(size.width, t / 2f), t)
                drawLine(color, Offset(0f, size.height - t / 2f), Offset(size.width, size.height - t / 2f), t)
            } else {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = size.height,
                    pathEffect = if (dashed) {
                        PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5f.dp.toPx()))
                    } else {
                        null
                    },
                )
            }
        }
        BasicText(
            text = text,
            style = TextStyle(
                color = colors.inkSoft,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/** The frozen 44×44 `.fnav`: field-filled, bordered, a single stroked arrow; `opacity:.4` when disabled. */
@Composable
private fun StepNavButton(
    pathData: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    com.aritr.zinely.ui.components.ZToolButton(
        onClick = onClick,
        metrics = com.aritr.zinely.ui.components.ZToolButtonMetrics.ProofStepNav,
        contentDescription = contentDescription,
        enabled = enabled,
        modifier = Modifier.testTag(testTag),
        icon = { tint -> FoldGlyph(pathData, tint) },
    )
}

/**
 * The frozen `.stepdots` — eight bordered discs, the current one filled.
 *
 * **They are buttons, and P4 is when that started to matter.** The frozen markup has always made each dot
 * a `<button onclick="goStep(i)">` with its own `aria-label` and `aria-current`; Compose drew them as
 * decoration under `clearAndSetSemantics`. At five steps that was a shrug. At eight it means a user who
 * loses their place — the likeliest thing to happen in the middle of folding paper — can only walk back
 * one arrow-press at a time, and a screen-reader user is told nothing about position at all beyond the
 * counter. Each dot carries a 42×48dp touch target around a 10dp disc — measured on device, not claimed.
 *
 * **What the device pass found, and what it did NOT fix.** On a real SM-A176B (Android 16, 411dp wide),
 * `uiautomator dump` reports every dot as `android.view.View` with `clickable=false`, `focusable=false`,
 * `selected=false`, and a separate unlabelled ancestor carrying the click:
 * ```
 * View[]        click=true  foc=true  b=[92,1615][202,1741]
 *   View[Step 1] click=false foc=false b=[100,1620][210,1736]
 * ```
 * A service can activate a node with no name, or read a name it cannot activate — never both. Seven
 * variants were built and measured against the device (`Role.Tab` → `Role.Button`; `selectable` →
 * `clickable`; with and without `selectableGroup()`; the group label removed; `mergeDescendants` added;
 * `testTag` moved ahead of the click; the target grown to 48dp; `minimumInteractiveComponentSize()`
 * added). **The platform tree did not change once.** Every Robolectric assertion — including
 * `ProofStepDotsA11yTest`, written specifically to measure this — passed throughout.
 *
 * It is not a dot problem. The same dump shows `Close`, `How to fold`, `Save PDF`, `Share` and P3's paper
 * segments arriving the same way; the only controls on the surface that reach the platform correctly are
 * `ZToolButton`s (the two arrows, `Button`, `clickable=true`, correctly `DISABLED` on step 1). That is
 * [ADR-059](../../../../../../docs/DECISIONS.md#adr-059)'s Role→View defect, surface-wide, and it is
 * booked as such rather than guessed at further here. See ADR-101 §6.8.
 */
@Composable
private fun StepDots(step: Int, onGoToStep: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = ZinelyTheme.v21Colors
    Row(
        modifier = modifier
            .selectableGroup()
            .semantics { contentDescription = Copy.ProofFold.FOLD_STEPS_GROUP }
            .testTag(ProofStepDotsTestTag),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(PROOF_FOLD_STEP_COUNT) { i ->
            val current = i == step
            Box(
                modifier = Modifier
                    // 48dp is Compose's own minimum interactive size; below it the framework wraps the
                    // click in a touch-target-expansion node. Sized at the minimum, so it does not.
                    .heightIn(min = 48.dp)
                    .weight(1f)
                    .selectable(
                        selected = current,
                        role = Role.Tab,
                        // No haptic here: `onGoToStep` is `ProofScreen.goToFoldStep`, which already ticks.
                        onClick = { onGoToStep(i) },
                    )
                    .semantics { contentDescription = Copy.ProofFold.stepDot(i + 1) }
                    .testTag(proofStepDotTag(i)),
                contentAlignment = Alignment.Center,
            ) {
                // Filled behind you, berry where you are, empty paper ahead. The first build drew eight
                // identical hollow discs with one filled, which answers *where am I* and not *how far did
                // I get* — and the second is the question you are holding when you pick the phone back up
                // with folded paper in your other hand. The progressive fill is the departure from the
                // frozen `.stepdots` (two states there, three here) and is the only thing that makes these
                // read as interactive rather than as a pagination indicator.
                //
                // **Berry, not jam, and that is not a preference.** The legend two rows above this one
                // spends a chip establishing that red means *cut* and only cut — the finding that moved
                // every arrow off `--jam` in the first place. A red "you are here" dot on the same screen
                // spends that meaning again on something that is not a mark on paper. The freeze already
                // says `background:var(--berry)`; the re-skin's first pass reached for jam and had to be
                // walked back to it.
                //
                // **Size is the second channel, and it is not decoration.** In light theme berry against
                // inkFaint is 1.28:1 and against paper 2.37:1 — a three-state indicator whose states are
                // told apart by hue alone, which is exactly what a colour-blind or low-contrast reader
                // cannot do. The current dot is larger and carries a heavier ring; visited-versus-ahead
                // survives on fill, which is the pair that matters least.
                Box(
                    Modifier
                        .size(if (current) 14.dp else 10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                current -> colors.berry
                                i < step -> colors.inkFaint
                                else -> colors.paper
                            },
                        )
                        .border(if (current) 2.dp else 1.5.dp, colors.ink, CircleShape),
                )
            }
        }
    }
}

/**
 * The per-step diagram on its `.foldstage` — a 230×172 viewBox drawn in the frozen vocabulary.
 *
 * The node is a single `role=img` labelled by the **step's caption**, matching the frozen
 * `aria-labelledby="foldCap"`. It used to be labelled with the step's title, and dropping the titles is
 * an improvement rather than a loss: *"Fold the sheet in half, bringing the two short edges together"*
 * tells a screen-reader user what the picture shows; *"Crease into eight"* did not.
 *
 * Each step change replays the crease-in (opacity .25→1, +5px settle over .32s), silenced under reduced
 * motion.
 */
@Composable
private fun FoldDiagram(step: Int, reduceMotion: Boolean) {
    val colors = ZinelyTheme.v21Colors
    val shell = ZinelyTheme.typography.shell
    val measurer = rememberTextMeasurer()
    val caption = Copy.ProofFold.STEP_CAPTIONS[step]

    // diagramIn: reset to .25/+5px and settle in on every step change (unless reduced).
    val enter = remember { Animatable(1f) }
    androidx.compose.runtime.LaunchedEffect(step, reduceMotion) {
        if (reduceMotion) {
            enter.snapTo(1f)
        } else {
            enter.snapTo(0f)
            enter.animateTo(1f, tween(320, easing = ZinelyEasing))
        }
    }

    // `.foldstage` — the diagram sits ON something, which is what stops eight line drawings reading as
    // eight unrelated sketches.
    //
    // **Painted in the V2.1 palette, hard shadow and all** — `background:var(--butter-tint);
    // border:1.5px solid var(--ink); box-shadow:var(--hard) var(--hard) 0 var(--ink-line)`. The first
    // build used V1 `field` on the grounds that the token sweep belongs to P6, and the device pass is
    // what settled it: the guide is the one screen a user sits with for minutes, holding paper, and it
    // was the only part of the app that did not look handmade. The hard offset shadow is the whole tell —
    // it is what makes butter read as *taped-down card* rather than as a tinted rectangle.
    val stage = RoundedCornerShape(ZinelyV21Dimens.radiusMd)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp)
            .drawBehind {
                // The frozen `--hard` offset: a flat, un-blurred shadow, drawn as a second card.
                val d = ZinelyV21Dimens.hardShadow.toPx()
                val r = ZinelyV21Dimens.radiusMd.toPx()
                drawRoundRect(
                    color = colors.inkLine,
                    topLeft = Offset(d, d),
                    size = Size(size.width, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r),
                )
            }
            .clip(stage)
            .background(colors.butterTint)
            .border(1.5.dp, colors.ink, stage)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(230f / 172f)
                .testTag(ProofFoldDiagramTestTag)
                .graphicsLayer {
                    alpha = 0.25f + 0.75f * enter.value
                    translationY = (1f - enter.value) * 5.dp.toPx()
                }
                .clearAndSetSemantics {
                    role = Role.Image
                    contentDescription = caption
                },
        ) {
            val d = FoldDiagramScope(this, measurer, colors, shell)
            when (step) {
                0 -> d.step1()
                1 -> d.step2()
                2 -> d.step3()
                3 -> d.step4()
                4 -> d.step5()
                5 -> d.step6()
                6 -> d.step7()
                else -> d.step8()
            }
        }
    }
}

// ---- fold-diagram drawing (the frozen 230×172 STEPS geometry) --------------------------------

private const val ICON_STEP_PREV = "M15 5l-7 7 7 7"
private const val ICON_STEP_NEXT = "M9 5l7 7-7 7"

/**
 * The frozen diagram geometry: one landscape sheet on a 230×172 viewBox, x 27→203, y 18→154, centres
 * 115/86, quarter columns 71 and 159. The canvas is pinned to that aspect ratio, so one scale factor
 * serves both axes and arrowheads stay symmetric.
 */
private class FoldDiagramScope(
    val ds: DrawScope,
    val measurer: androidx.compose.ui.text.TextMeasurer,
    val colors: com.aritr.zinely.ui.theme.ZinelyV21Colors,
    val shell: FontFamily,
) {
    private val s = ds.size.width / 230f
    private fun o(x: Float, y: Float) = Offset(x * s, y * s)
    private fun w(units: Float) = units * s

    private val x0 = 27f
    private val x1 = 203f
    private val y0 = 18f
    private val y1 = 154f
    private val cx = 115f
    private val cy = 86f
    private val q1 = 71f
    private val q3 = 159f

    // ---- the frozen mark vocabulary ---------------------------------------------------------
    //
    // `.sheetline` is stroked in **ink**, not `inkLine`. That is not a detail: V21-SPEC §4.1 row 8
    // records that the fold guide's own outline was drawn in the hard-shadow token and sat at 1.38:1
    // against its fill in dark theme — the load-bearing artefact of the whole surface, very nearly
    // invisible. A drawn line follows `ink`; only a shadow follows `inkLine`.

    private fun sheet(left: Float, top: Float, wUnits: Float, hUnits: Float) {
        ds.drawRect(colors.paper, topLeft = o(left, top), size = Size(w(wUnits), w(hUnits)))
        ds.drawRect(
            colors.ink, topLeft = o(left, top), size = Size(w(wUnits), w(hUnits)),
            style = Stroke(width = w(2.4f), join = StrokeJoin.Round),
        )
    }

    private fun full() = sheet(x0, y0, x1 - x0, y1 - y0)

    /**
     * `.ghost` — the half of the paper that is about to travel, shown where it starts.
     *
     * Outlined **solid**, not dashed. The frozen CSS dashes it, and copying that put a dashed rectangle
     * on the same diagram as dashed creases: a first-time reader sees four more creases, which is the
     * opposite of what the shape means. Solid-and-faint says *this is where that paper starts* without
     * adding a fifth legend mark. The hue stays the frozen `--ink-faint`.
     */
    private fun ghost(left: Float, top: Float, wUnits: Float, hUnits: Float) {
        ds.drawRect(
            colors.paper.copy(alpha = 0.5f), topLeft = o(left, top),
            size = Size(w(wUnits), w(hUnits)),
        )
        ds.drawRect(
            colors.inkFaint, topLeft = o(left, top),
            size = Size(w(wUnits), w(hUnits)),
            style = Stroke(width = w(1.6f)),
        )
    }

    /**
     * `.crease` — dashed: a fold you already made.
     *
     * Known limitation, recorded rather than implied away: `inkFaint` on `paper` is 2.65:1 light /
     * 2.44:1 dark, under WCAG 1.4.11's 3:1 for a meaningful graphic — and in **light** theme the
     * `onDeskFaint` it replaced was 4.66:1, so this is a regression on that axis. It is the frozen
     * `--ink-faint` and it belongs to the paper's family, which is the right call for a mark drawn on
     * paper; the contrast is a token problem for the V2.1 sweep (P6), not something to fix by putting
     * the room's ink back on the sheet.
     */
    private fun crease(ax: Float, ay: Float, bx: Float, by: Float) = ds.drawLine(
        colors.inkFaint, o(ax, ay), o(bx, by), w(1.5f),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(w(5f), w(4f))),
    )

    /** `.now` — green solid: the fold you are making **now**. */
    private fun now(ax: Float, ay: Float, bx: Float, by: Float) =
        ds.drawLine(colors.leaf, o(ax, ay), o(bx, by), w(3.4f), cap = StrokeCap.Round)

    /** `.cutmark` — red **dashed**: cut here. Drawn on exactly one step, the one that cuts. */
    private fun cutMark(ax: Float, ay: Float, bx: Float, by: Float) = ds.drawLine(
        colors.jam, o(ax, ay), o(bx, by), w(3f), cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(w(6f), w(4f))),
    )

    /**
     * Red **solid**: the slit you already made. A different mark from [cutMark] on purpose — the frozen
     * diagrams draw the existing slit with the identical dashed cut-mark on the two steps after the cut,
     * and a reader who has just been taught *dashed red means cut along here* meets it again and reaches
     * for the scissors a second time. Dashed is an instruction; solid is a fact about the paper.
     */
    private fun existingCut(ax: Float, ay: Float, bx: Float, by: Float) =
        ds.drawLine(colors.jam, o(ax, ay), o(bx, by), w(2.4f), cap = StrokeCap.Round)

    /**
     * `.cutstop` — where the cut **ends**. Solid, because it is not something you cut; it is the end of
     * the thing you do cut, and it is the only mark on the diagram that says *one panel deep* out loud.
     * The caption and the holding line both say it; the picture was the one of the three staying quiet,
     * on the single step whose mistake cannot be undone.
     */
    private fun cutStop(x: Float, y: Float) =
        ds.drawLine(colors.jam, o(x, y - 7f), o(x, y + 7f), w(2.4f), cap = StrokeCap.Round)

    // ---- Yoshizawa–Randlett arrows (V21-SPEC §5.3) -------------------------------------------
    //
    // The rejected first version's arrows all FLOATED beside the sheet. Robert Lang's statement of the
    // convention is that the tail is as important as the head: it names the reference point undergoing
    // motion, so an arrow touching no paper instructs nothing and merely decorates. Every tail below
    // sits on the flap that moves; every head sits where that flap lands.
    //
    // Motion and action are also never confusable: motion is a plain stem with a filled head, action
    // (push, unfold) is a HOLLOW stem. Head geometry is identical at every step.

    private val headLen = 9.5f
    private val headHalf = 4.8f

    /** A filled symmetric head, tip at [x],[y], pointing along the unit vector [ux],[uy]. */
    private fun head(x: Float, y: Float, ux: Float, uy: Float) {
        val bx = x - ux * headLen
        val by = y - uy * headLen
        fillPath(
            listOf(
                o(x, y),
                o(bx - uy * headHalf, by + ux * headHalf),
                o(bx + uy * headHalf, by - ux * headHalf),
            ),
            colors.ink,
        )
    }

    /**
     * An arrow of **motion**: this paper travels. [bow] curves the path to the left of travel, and the
     * head is angled along the curve's tangent so it arrives pointing where the paper actually lands.
     */
    private fun move(ax: Float, ay: Float, bx: Float, by: Float, bow: Float) {
        val dx = bx - ax
        val dy = by - ay
        val len = hypot(dx, dy).takeIf { it > 0f } ?: 1f
        val px = -dy / len
        val py = dx / len
        val ctrlX = (ax + bx) / 2f + px * bow
        val ctrlY = (ay + by) / 2f + py * bow
        val path = Path().apply {
            moveTo(o(ax, ay).x, o(ax, ay).y)
            quadraticBezierTo(o(ctrlX, ctrlY).x, o(ctrlX, ctrlY).y, o(bx, by).x, o(bx, by).y)
        }
        ds.drawPath(
            path, colors.ink,
            style = Stroke(width = w(2.4f), cap = StrokeCap.Round),
        )
        val tx = bx - ctrlX
        val ty = by - ctrlY
        val tl = hypot(tx, ty).takeIf { it > 0f } ?: 1f
        head(bx, by, tx / tl, ty / tl)
    }

    /**
     * An arrow of **action**: do this *to* the paper. Hollow stem, so it can never be read as travel.
     * [heads] is 1 for a push and 2 for an unfold.
     */
    private fun act(ax: Float, ay: Float, bx: Float, by: Float, heads: Int) {
        val dx = bx - ax
        val dy = by - ay
        val len = hypot(dx, dy).takeIf { it > 0f } ?: 1f
        val ux = dx / len
        val uy = dy / len
        val px = -uy
        val py = ux
        val stem = 3.2f
        val hLen = 8f
        val hHalf = 7f
        fun p(d: Float, off: Float) = o(ax + ux * d + px * off, ay + uy * d + py * off)
        val pts = mutableListOf(p(len, 0f), p(len - hLen, hHalf), p(len - hLen, stem))
        if (heads == 2) {
            pts += listOf(p(hLen, stem), p(hLen, hHalf), p(0f, 0f), p(hLen, -hHalf), p(hLen, -stem))
        } else {
            pts += listOf(p(0f, stem), p(0f, -stem))
        }
        pts += listOf(p(len - hLen, -stem), p(len - hLen, -hHalf))
        fillPath(pts, colors.paper)
        strokePath(pts, colors.ink, 2f)
    }

    private fun pathOf(points: List<Offset>): Path = Path().apply {
        moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }

    private fun fillPath(points: List<Offset>, color: Color) = ds.drawPath(pathOf(points), color)

    private fun strokePath(points: List<Offset>, color: Color, wUnits: Float) = ds.drawPath(
        pathOf(points), color, style = Stroke(width = w(wUnits), join = StrokeJoin.Round),
    )

    /**
     * The eight imposed page numbers, so every diagram is drawn on *the sheet the printer produced* —
     * read from the engine through [decorativeImpositionRows], never from a literal.
     *
     * ⚠ This function held a raw array until ADR-102, and it was **wrong in five of its eight cells**:
     * `5 4 3 6 / 8 1 2 7` where the engine imposes `5 4 3 2 / 6 7 8 1`. That is the same bad sheet
     * [ADR-050](../../../../../../../../docs/DECISIONS.md#adr-050) had already found in the HTML and
     * corrected once — for the Print Details illustration only. This second illustration, in the frozen
     * `v21-proof.html`, was never corrected, and Compose copied it faithfully. So the user's fold guide
     * and the user's PDF disagreed about where page 2 goes, with the PDF right.
     *
     * ADR-050's ruling is the fix, not the numbers: *"Compose derives the sheet from the convention and
     * never re-encodes a raw layout array."* Any future engine or convention change now reaches this
     * diagram on its own.
     */
    private fun cells() {
        val xs = listOf(q1 - 22f, q1 + 22f, q3 - 22f, q3 + 22f)
        val ys = listOf(cy - 46f, cy + 50f)
        decorativeImpositionRows().forEachIndexed { row, panels ->
            val y = ys.getOrNull(row) ?: return@forEachIndexed
            panels.forEachIndexed { col, panel ->
                val x = xs.getOrNull(col) ?: return@forEachIndexed
                cellNumber(panel.pageNumber.toString(), x, y - 3f, 9f)
            }
        }
    }

    // `.cellno` is text drawn ON the sheet, so it follows the paper's own ink family (`inkSoft`), not
    // the room's (`onDeskSoft` — 1.53:1 on paper in dark theme). The frozen CSS annotates this exact
    // case as a fixed text-contrast fix: 3.04:1 -> 6.16:1.
    private fun cellNumber(text: String, atX: Float, atY: Float, sizeUnits: Float) {
        val style = TextStyle(
            color = colors.inkSoft,
            fontFamily = shell,
            fontSize = with(ds) { (sizeUnits * s).toSp() },
            fontWeight = FontWeight.Bold,
        )
        val res = measurer.measure(text, style)
        ds.drawText(
            res,
            topLeft = Offset(atX * s - res.size.width / 2f, atY * s - res.size.height / 2f),
        )
    }

    // ---- the eight steps ---------------------------------------------------------------------

    /** 1 — the RIGHT half travels onto the left, so the tail sits at its centre. */
    fun step1() {
        full(); cells()
        ghost(cx, y0, x1 - cx, y1 - y0)
        now(cx, y0, cx, y1)
        move(q3, cy, q1, cy, 56f)
    }

    /** 2 — the BOTTOM half travels up onto the top half. */
    fun step2() {
        sheet(q1, y0, q3 - q1, y1 - y0)
        ghost(q1, cy, q3 - q1, y1 - cy)
        now(q1, cy, q3, cy)
        move(cx, cy + 34f, cx, cy - 34f, 48f)
    }

    /** 3 — the RIGHT half again, the same way as the first fold. */
    fun step3() {
        sheet(q1, cy - 34f, q3 - q1, 68f)
        ghost(cx, cy - 34f, q3 - cx, 68f)
        now(cx, cy - 34f, cx, cy + 34f)
        move(cx + 22f, cy, cx - 22f, cy, 36f)
    }

    /** 4 — unfolding is an **action**, not a motion: a double-headed hollow arrow. */
    fun step4() {
        full(); cells()
        crease(cx, y0, cx, y1)
        crease(q1, y0, q1, y1)
        crease(q3, y0, q3, y1)
        crease(x0, cy, x1, cy)
        act(x0 + 31f, cy - 24f, x1 - 31f, cy - 24f, heads = 2)
    }

    /**
     * 5 — cutting carries **no arrow at all**: the blade sits on the line, pointing along the cut.
     *
     * Three departures from the frozen `STEPS[4]`, all on the one step whose mistake cannot be undone,
     * and all made in `v21-proof.html` first:
     * - **The crease starts where the cut stops.** The freeze ran it `q1→q3` with the cut drawn *on top
     *   of* its left half: two dashed lines, same rhythm, collinear, told apart only by hue on a dark
     *   stage. Read cold — *"do I cut to the red bit or across the whole thing?"* Abutting them makes the
     *   handover itself the answer.
     * - **[cutStop] at `cx`.** *One panel deep* is in the caption and *One short slit* in the holding
     *   line; the diagram was the only one of the three not saying it.
     * - **No [now] line.** It sat exactly on the sheet's own left edge — the rect is drawn from `q1`, so
     *   the spine was already ink — and it made green mean *the fold you are making* on steps 1–3 and
     *   *a fold you already made* here, on the step where knowing which edge is folded decides where the
     *   slit lands. That is the same defect as the red arrows: a legend that teaches a rule and breaks
     *   it. The folded edge now reads from the cut's own origin, with the scissors sitting outside it.
     */
    fun step5() {
        sheet(q1, y0, q3 - q1, y1 - y0)
        crease(cx, cy, q3, cy)
        cutMark(q1, cy, cx, cy)
        cutStop(cx, cy)
        scissors(q1 - 34f, cy)
    }

    /** 6 — the BOTTOM half travels up again, now with the slit on the crease. */
    fun step6() {
        full()
        crease(cx, y0, cx, y1)
        crease(q1, y0, q1, y1)
        crease(q3, y0, q3, y1)
        ghost(x0, cy, x1 - x0, y1 - cy)
        now(x0, cy, x1, cy)
        existingCut(q1, cy, q3, cy)
        move(x1 - 24f, cy + 38f, x1 - 24f, cy - 38f, 44f)
    }

    /** 7 — pushing is an **action**: hollow stems, one from each end. */
    fun step7() {
        sheet(x0, cy - 34f, x1 - x0, 68f)
        crease(q1, cy - 34f, q1, cy + 34f)
        crease(q3, cy - 34f, q3, cy + 34f)
        // the slit, opened into a plus: a red diamond on the fold. Solid, like step 6's slit — the cut
        // already happened; only step 5 instructs one.
        ds.drawPath(
            pathOf(listOf(o(q1, cy), o(cx, cy - 15f), o(q3, cy), o(cx, cy + 15f))),
            colors.jam,
            style = Stroke(width = w(2.4f), join = StrokeJoin.Round),
        )
        act(x0 - 25f, cy, x0 - 3f, cy, heads = 1)
        act(x1 + 25f, cy, x1 + 3f, cy, heads = 1)
    }

    /** 8 — the outer panels wrap round; the tail sits on the panel that swings. */
    fun step8() {
        // .coverfill — the finished cover, face on.
        val coverTopLeft = o(cx - 30f, cy - 48f)
        val coverSize = Size(w(60f), w(96f))
        ds.drawRoundRect(
            // `leafTint`, not a hand-derived alpha of `leaf`: the frozen `.coverfill` is
            // `fill:var(--leaf-tint)` and the palette publishes that tint. A `leaf.copy(alpha = …)` here
            // would be a third source of truth for a colour that has one — the review that caught it
            // caught the same derived hue in the retired climax, which is where it came from.
            colors.leafTint, topLeft = coverTopLeft, size = coverSize,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w(3f)),
        )
        ds.drawRoundRect(
            colors.leaf, topLeft = coverTopLeft, size = coverSize,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w(3f)),
            style = Stroke(width = w(2f)),
        )
        // the page block's edge, seen in perspective at the fore-edge.
        val block = listOf(
            o(cx + 30f, cy - 48f), o(cx + 37f, cy - 44f),
            o(cx + 37f, cy + 52f), o(cx + 30f, cy + 48f),
        )
        fillPath(block, colors.paper)
        strokePath(block, colors.ink, 2.4f)
        crease(cx - 30f, cy - 48f, cx - 30f, cy + 48f)
        cellNumber("1", cx, cy + 1f, 11f)
        move(cx + 58f, cy - 34f, cx + 42f, cy + 34f, 26f)
    }

    /** The blade itself, on the cut line — step 5's only mark besides the line. */
    private fun scissors(atX: Float, atY: Float) {
        val stroke = Stroke(width = w(2f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        ds.drawCircle(colors.jam, w(3.6f), o(atX + 2f, atY - 7f), style = stroke)
        ds.drawCircle(colors.jam, w(3.6f), o(atX + 2f, atY + 7f), style = stroke)
        ds.drawLine(colors.jam, o(atX + 5f, atY - 5f), o(atX + 22f, atY), w(2f), cap = StrokeCap.Round)
        ds.drawLine(colors.jam, o(atX + 5f, atY + 5f), o(atX + 22f, atY), w(2f), cap = StrokeCap.Round)
    }
}

/** Draws a `proof.html` 24×24 stroked path at the caller's size, in [tint] (round caps/joins, 2.2px). */
@Composable
private fun FoldGlyph(pathData: String, tint: Color) {
    val path = remember(pathData) { PathParser().parsePathString(pathData).toPath() }
    Canvas(Modifier.fillMaxSize()) {
        val s = size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            drawPath(
                path = path,
                color = tint,
                style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
