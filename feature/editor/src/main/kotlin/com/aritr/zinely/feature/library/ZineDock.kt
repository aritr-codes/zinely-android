package com.aritr.zinely.feature.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.a11y.zinelyV2Control
import com.aritr.zinely.ui.components.zinelyV21Frame
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Press
import com.aritr.zinely.ui.theme.ZinelyV2Standard

/** The test handle on the landing zone itself — the gradient band, not the button standing in it. */
internal const val ZineDockTestTag = "zine-dock"

/** The test handle on the "Make a zine" button. */
internal const val ZineStartTestTag = "zine-start"
internal const val ZineDockSecondaryActionTestTag = "zine-dock-secondary-action"

internal data class ZineDockSecondaryAction(
    val label: String,
    val onClick: () -> Unit,
)

/**
 * The frozen Library's **dock** — `docs/design/mockups/v21-library.html`.
 *
 * ```css
 * .dock{position:absolute;left:0;right:0;bottom:0;z-index:40;
 *   padding:var(--gap-lg) var(--gap-lg) var(--gap-xl);
 *   display:flex;justify-content:center;pointer-events:none;
 *   background:linear-gradient(to top,var(--desk) 58%,transparent)}
 * ```
 *
 * ### The band got shorter, and the fade got longer
 *
 * V2's dock was 52px of top padding over a gradient solid to 80%, and its own comment explained why:
 * *"content fades into the desk well ABOVE the button, so no cover title or ⋯ can ever sit against it."*
 * V2.1 needs less of that, because the thing that used to sit against the band was a **title printed on
 * a cover**, and V2.1 prints no title on a cover. The fade is now 42% of a shorter band. Both numbers
 * moved together and neither is trimmable on its own.
 *
 * ### `pointer-events:none` is behaviour, not decoration
 *
 * The dock covers the bottom of the shelf, so a band that consumed touches would make the last row of
 * covers unscrollable through a region that looks like empty desk. This composable declares no pointer
 * input on the [Box] at all, which is the same statement.
 *
 * ### ⚠️ The safe area is a carry-over, and the V2.1 file does not state it
 *
 * V2's `.dock` wrote `calc(22px + env(safe-area-inset-bottom))`; V2.1's writes `var(--gap-xl)` and drops
 * the `env()`. That is a **prototype simplification, not a ruling** — a browser mock has no gesture bar,
 * and no design intends its primary action to sit under one. So the platform inset is kept, unioned from
 * navigation bars and the display cutout, which is what `env(safe-area-inset-*)` names.
 *
 * Applied with [Modifier.windowInsetsPadding], which **consumes** what it pads, rather than by reading
 * `asPaddingValues()` and adding it: the two render identically here and behave differently in a screen
 * that pads for the same inset around this dock, where the consuming form is a no-op and the reading
 * form is a double-count.
 *
 * @param onStart the button was pressed.
 * @param modifier the caller's. `.dock` is `position:absolute;bottom:0` **inside `.phone`** — the app
 *   window — so this composable does not place itself.
 */
@Composable
internal fun ZineDock(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryAction: ZineDockSecondaryAction? = null,
    secondaryActionFocusRequester: FocusRequester? = null,
) {
    val desk = ZinelyTheme.v21Colors.desk

    Box(
        modifier
            .testTag(ZineDockTestTag)
            .fillMaxWidth()
            // CSS paints a background over the padding box, so the gradient is declared before the
            // padding it has to cover — including the fade that is the whole point of the band.
            .background(dockGradient(desk))
            .windowInsetsPadding(SafeAreaBottom)
            .padding(
                start = DockPaddingHorizontal,
                end = DockPaddingHorizontal,
                top = DockPaddingTop,
                bottom = DockPaddingBottom,
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
        ) {
            StartButton(onStart)
            secondaryAction?.let { QuietAction(it, secondaryActionFocusRequester) }
        }
    }
}

@Composable
private fun QuietAction(action: ZineDockSecondaryAction, focusRequester: FocusRequester?) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Text(
        text = action.label,
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .testTag(ZineDockSecondaryActionTestTag)
            .clip(RoundedCornerShape(ZinelyV21Dimens.radiusPill))
            .background(if (pressed) colors.paper.copy(alpha = 0.72f) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .zinelyV2Control(
                label = action.label,
                interactionSource = interaction,
                onClick = action.onClick,
            ),
        style = TextStyle(
            color = if (pressed) colors.ink else colors.inkSoft,
            fontFamily = ZinelyV21Fonts.Work,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = ZinelyV21Fonts.InheritedLineHeight,
        ),
    )
}

/**
 * `linear-gradient(to top,var(--desk) 58%,transparent)`.
 *
 * CSS measures `to top` from the bottom edge; [Brush.verticalGradient] measures from the top. So the
 * frozen stop at **58% up** is the stop at **42% down**, and the two ends swap: transparent at the top,
 * desk from 42% to the bottom. Writing it the CSS way round produces a band that fades *downward* into
 * nothing — a dock that dissolves at the button and is opaque where the covers scroll past, which is
 * the failure the band exists to prevent.
 */
private fun dockGradient(desk: Color): Brush = Brush.verticalGradient(
    0f to Color.Transparent,
    DockFadeStop to desk,
    1f to desk,
)

/**
 * `.start` — the one primary action on the screen, and the corpus's only wearer of the frame ring.
 *
 * ```css
 * .start{background:var(--leaf);color:var(--on-leaf);border:1.5px solid var(--ink);
 *   border-radius:var(--br-pill);padding:var(--gap-lg) var(--gap-xl);gap:var(--gap-sm);
 *   font-family:var(--sans);font-size:1rem;font-weight:700;
 *   box-shadow:var(--hard) var(--hard) 0 var(--ink-line), 0 0 0 var(--frame) var(--butter-tint)}
 * .start:active{transform:translate(2px,2px);
 *   box-shadow:1px 1px 0 var(--ink-line),0 0 0 var(--frame) var(--butter-tint)}
 * ```
 *
 * ### Two rings and one of them does not move
 *
 * The `--frame` ring is `0 0 0 5px` — a **spread with no offset**, so it is a halo, not a shadow, and
 * `:active` leaves it untouched while the hard shadow collapses from 4 to 1. That distinction is what
 * [ZinelyV21Press] exists to keep: pressing this button sheds its depth and keeps its emphasis. A
 * version that animated both would read as the whole control shrinking.
 *
 * The two are declared in CSS's own order, and CSS paints `box-shadow` layers **first-declared on top**,
 * so the hard shadow sits over the frame ring where they overlap down and right.
 *
 * ### The label colour is `--on-leaf`, and D-023 is closed by the re-freeze
 *
 * V2's Library took `var(--paper)` on matcha while the Bench and Proof took `var(--on-matcha)`, which
 * is what opened **D-023**. V2.1 writes `var(--on-leaf)` here, the same token every other leaf fill in
 * the corpus takes. The inconsistency the defect recorded no longer exists to rule on.
 *
 * ### `:focus-visible` is transcribed, which D-008 does not contradict
 *
 * `outline:2px solid var(--ink);outline-offset:5px` is stated by the frozen file. A CSS `transform`
 * carries the element's outline with it, so [graphicsLayer] is declared **before** the ring's
 * `drawBehind` and the ring travels with the press.
 */
@Composable
private fun StartButton(onStart: () -> Unit) {
    val colors = ZinelyTheme.v21Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()

    val press = ZinelyV21Press.Hero
    val duration = if (ZinelyTheme.v2Motion.reduceMotion) 0 else StartPressDurationMillis
    val travel by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = duration, easing = ZinelyV2Standard),
        label = "zineStartPress",
    )

    val labelStyle = TextStyle(
        fontFamily = ZinelyV21Fonts.Work,
        fontWeight = FontWeight.Bold,
        fontSize = StartLabelSize,
        // `body{line-height:1.55}`, inherited — `.start` declares none, and this was the most visible
        // of the seven sites that dropped it: the screen's primary control stood ~5dp short.
        lineHeight = ZinelyV21Fonts.InheritedLineHeight,
        color = colors.onLeaf,
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapSm),
        modifier = Modifier
            // Before `zinelyV2Control`: that seam ends in `clearAndSetSemantics`, which discards a tag
            // chained after it. The node simply becomes unfindable, silently.
            .testTag(ZineStartTestTag)
            .graphicsLayer {
                val t = press.travel.toPx() * travel
                translationX = t
                translationY = t
            }
            // `0 0 0 var(--frame) var(--butter-tint)` — a halo, unchanged by the press, and UNDER the
            // hard shadow where the two overlap.
            //
            // Order matters and is not the CSS order. `drawBehind` paints its own layer and *then*
            // calls `drawContent()`, and the chain nests left-outermost, so the LEFTMOST draw modifier
            // paints first — underneath. CSS is the other way round: the first-declared `box-shadow`
            // paints on top. So the two are written here in reverse of the frozen rule, deliberately.
            // (The cover's draw order was got wrong in exactly this way and survived two reviews.)
            // `box-shadow:0 0 0 var(--frame) var(--butter-tint)` — **amended to `--butter`, ADR-100.**
            //
            // The ring is decoration, not the focus indicator (`drawFocusRing` below is that, in `--ink`
            // at 12.3:1, and it is untouched). But at `butter-tint` it measured **1.01:1** against the
            // light desk and **1.33:1** against the dark one — so the corpus's signature stacked-ring
            // move, the thing that makes the one primary action look like a printed label with a border
            // around it, was invisible in light and barely there in dark. A design's emphasis idiom that
            // only appears at night is not an idiom. `--butter` puts it at 1.56 and 8.71, and matches
            // the count chip it now shares a screen with.
            .zinelyV21Frame(colors.butter, StartShape)
            .zinelyV21HardShadow(
                offset = press.rest - (press.rest - press.pressed) * travel,
                color = colors.inkLine,
                shape = StartShape,
            )
            .drawBehind { if (focused) drawFocusRing(colors.ink) }
            .clip(StartShape)
            .background(colors.leaf)
            .border(StartBorder, colors.ink, StartShape)
            .zinelyV2Control(
                label = StartLabelText,
                onClick = onStart,
                interactionSource = interaction,
            )
            .padding(
                horizontal = ZinelyV21Dimens.gapXl,
                vertical = ZinelyV21Dimens.gapLg,
            ),
    ) {
        StartPlus(colors.onLeaf)
        Text(text = StartLabelText, style = labelStyle)
    }
}

/**
 * `.start .plus{font-family:var(--voice);font-size:1.15rem;font-weight:700;line-height:1}`.
 *
 * ### The zero-height line box is gone, and that is a real simplification
 *
 * V2's plus carried `line-height:0;margin-top:-2px`, which meant it contributed nothing to the button's
 * height and had to be laid out by hand — a measured-then-reported-as-zero `layout {}` whose half-margin
 * arithmetic a review had to correct. V2.1 writes `line-height:1`, which is an ordinary line box, so the
 * hand-rolled layout is deleted rather than ported. Recorded because "we used to need that" is exactly
 * the reason such code survives a re-skin.
 *
 * ### `＋` is U+FF0B and no bundled weight has it — D-021 already answers that
 *
 * *"Keep the literal characters exactly as defined by the frozen HTML. … Platform fallback is
 * acceptable."* The glyph stands and the platform supplies it. Now in the **voice** face rather than the
 * sans, per `.plus{font-family:var(--voice)}` — which changes nothing about the fallback, since Averia
 * does not carry it either.
 */
@Composable
private fun StartPlus(tint: Color) {
    Text(
        text = StartPlusGlyph,
        style = TextStyle(
            fontFamily = ZinelyV21Fonts.Voice,
            fontWeight = FontWeight.Bold,
            fontSize = StartPlusSize,
            lineHeight = StartPlusSize,
            color = tint,
        ),
    )
}

/**
 * `.start:focus-visible{outline:2px solid var(--ink);outline-offset:5px}`.
 *
 * A CSS outline starts at the offset **outside** the border box and grows outward from there, so a 2px
 * outline at a 5px offset occupies 5–7px out and its stroke centre is 6px out. Stroked rather than
 * bordered: `Modifier.border` paints *inside* the bounds, which draws a ring that eats into the
 * button's own fill instead of surrounding it.
 *
 * The corner radius grows with the offset, as a CSS outline's does — which on a pill is unobservable,
 * since a pill's radius already exceeds half its height. Written anyway, because the shape token is one
 * edit away from not being a pill.
 */
private fun DrawScope.drawFocusRing(ink: Color) {
    val stroke = StartFocusWidth.toPx()
    val out = StartFocusOffset.toPx() + stroke / 2f
    drawRoundRect(
        color = ink,
        topLeft = Offset(-out, -out),
        size = Size(size.width + 2 * out, size.height + 2 * out),
        cornerRadius = CornerRadius(ZinelyV21Dimens.radiusPill.toPx() + out),
        style = Stroke(width = stroke),
    )
}

// ---------------------------------------------------------------------------------------------
// The frozen values, transcribed from `v21-library.html`.
//
// V2.1 publishes a spacing scale (§3.3), so the dock's padding and the button's are token references.
// The two that are not on a scale are the gradient stop and the type sizes, which are ratios rather
// than spaces.
// ---------------------------------------------------------------------------------------------

/** `.dock{padding:var(--gap-lg) var(--gap-lg) var(--gap-xl)}`. */
private val DockPaddingTop = ZinelyV21Dimens.gapLg
private val DockPaddingHorizontal = ZinelyV21Dimens.gapLg
private val DockPaddingBottom = ZinelyV21Dimens.gapXl

/** `env(safe-area-inset-bottom)` — a carry-over the V2.1 file drops. See [ZineDock]. */
private val SafeAreaBottom: WindowInsets
    @Composable get() = WindowInsets.navigationBars
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Bottom)

/**
 * The room the three shelf states must leave below their content for the dock — the frozen literal
 * **plus the same safe-area inset the dock itself adds**.
 *
 * The frozen file writes three separate clearances (`.shelf{padding-bottom:132px}`,
 * `.empty`/`.fail{padding-bottom:150px}`) against a dock whose height is fixed, because a browser mock
 * has no gesture bar. On a device the dock grows by the navigation bar, and
 * [Modifier.windowInsetsPadding] consumes that inset only for the dock's own **descendants** — the
 * shelf is a *sibling*. So without this the last row's caption sits under the opaque part of the band
 * on any device with three-button navigation, by exactly the height of that bar.
 *
 * A review found it. Published here rather than repeated three times so the three states cannot drift
 * from the component whose height they are clearing.
 *
 * **This is the *reading* form of the inset, which this file's own docs warn against** — and it is correct
 * here only because nothing above it consumes. `ZineLibraryScreen` places the dock and the three states as
 * siblings in a `Box` and applies no inset padding at the root (asserted by `ZineLibraryInsetTest`), so the
 * navigation bar is consumed exactly once, by the dock, for the dock. Wrap the Library in anything that
 * consumes the bottom inset and this double-counts. Stated rather than left to be rediscovered.
 *
 * **Not unit-tested, and the reason is measured rather than assumed.** A case was written for
 * `ZineLibraryInsetTest` and deleted: that harness's decor view applies a dispatched inset as *padding*,
 * so Compose reads zero however large the inset dispatched, and the assertion could only ever have been
 * `0 == 0`. The finding is recorded in that file's docs. This one goes to **device Pass 1** — scroll the
 * shelf to its end on a three-button-navigation device and the last caption must clear the band.
 */
@Composable
internal fun zineDockClearance(frozen: Dp): Dp =
    frozen + WindowInsets.navigationBars
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()

/** `linear-gradient(to top,var(--desk) 58%,…)`, expressed downward — see [dockGradient]. */
private const val DockFadeStop = 0.42f

/** `.start{border-radius:var(--br-pill);border:1.5px solid var(--ink)}`. */
private val StartShape: Shape = RoundedCornerShape(ZinelyV21Dimens.radiusPill)
private val StartBorder = 1.5.dp

/** `.start{font-size:1rem}` against the browser's 16px root. `var(--sans)` is the bundled Inter. */
private val StartLabelSize = 16.sp

/** The button's own words. Also its spoken name — it is a labelled control, not an icon. */
private const val StartLabelText = "Make a zine"

/**
 * `.plus` — U+FF0B FULLWIDTH PLUS SIGN. Not the ASCII `+`. See [StartPlus].
 *
 * `internal` so its codepoint can be pinned: the two characters are indistinguishable in a diff and not
 * on screen, and no rendered assertion can tell them apart on a host whose fallback font stack is a
 * Robolectric fiction.
 */
internal const val StartPlusGlyph = "＋"

/** `.start .plus{font-size:1.15rem}` = 18.4px, with `line-height:1` of the same. */
private val StartPlusSize = 18.4.sp

/** `transition:transform .14s cubic-bezier(.2,.8,.2,1)` — the travel is [ZinelyV21Press.Hero]'s. */
private const val StartPressDurationMillis = 140

/** `.start:focus-visible{outline:2px solid var(--ink);outline-offset:5px}`. */
private val StartFocusWidth = 2.dp
private val StartFocusOffset = 5.dp
