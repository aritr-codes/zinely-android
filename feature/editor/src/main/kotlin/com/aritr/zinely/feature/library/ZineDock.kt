package com.aritr.zinely.feature.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.a11y.zinelyV2Control
import com.aritr.zinely.ui.components.zinelyV2Shadow
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Dimens
import com.aritr.zinely.ui.theme.ZinelyV2ShadowLayer
import com.aritr.zinely.ui.theme.ZinelyV2Standard

/** The test handle on the landing zone itself — the gradient band, not the button standing in it. */
internal const val ZineDockTestTag = "zine-dock"

/** The test handle on the "Make a zine" button. */
internal const val ZineStartTestTag = "zine-start"

/**
 * The frozen Library's **dock** — `v2-library.html:86-95`, `:168`.
 *
 * ```
 * .dock{position:absolute;left:0;right:0;bottom:0;
 *   padding:52px 20px calc(22px + env(safe-area-inset-bottom));
 *   display:flex;justify-content:center;pointer-events:none;
 *   background:linear-gradient(to top,var(--desk) 80%,transparent)}
 * .start{pointer-events:auto;background:var(--matcha);color:var(--paper);border:none;border-radius:16px;
 *   font-weight:600;font-size:1rem;padding:15px 26px;display:flex;align-items:center;gap:10px;
 *   box-shadow:0 16px 30px -12px var(--shadow);font-family:inherit;transition:transform .14s}
 * ```
 *
 * The frozen file writes its own reasoning above the rule, and it is a layout requirement rather than a
 * flourish: *"Tall, full-width solid 'landing zone': content fades into the desk well ABOVE the button, so
 * no cover title or ... can ever sit against it. Solid to 80% of the taller dock, gentle fade above."*
 * (`:86-87`). The 52px of top padding is therefore **not** whitespace to be trimmed — it is the fade, and
 * it is why [ZineShelf] reserves `padding-bottom:152px` it does not use itself.
 *
 * ### `pointer-events:none` is behaviour, not decoration
 *
 * The dock covers the bottom ~150px of the shelf, so a band that consumed touches would make the last row
 * of covers unscrollable and un-tappable through a region that looks like empty desk. The CSS says the
 * band is inert and only `.start` is live. This composable declares no pointer input on the [Box] at all,
 * which is the same statement — and `the dock does not swallow touches meant for the shelf` asserts it,
 * because the natural mistake (a `clickable` dismiss target, a `Surface`) looks harmless and is not.
 *
 * ### The safe area is transcribed here, and B5 must not consume it twice
 *
 * `calc(22px + env(safe-area-inset-bottom))` is a real dependency on the window, so the bottom padding is
 * the frozen 22dp **plus** the platform's bottom safe area — navigation bars unioned with the display
 * cutout, which is what `env(safe-area-inset-*)` names. `WindowInsets.safeDrawing` is the wrong seam here:
 * it folds in the IME, and this screen has no text field to be pushed by one.
 *
 * It is applied with [Modifier.windowInsetsPadding], which **consumes** what it pads, rather than by reading
 * `asPaddingValues()` and adding it — the two render identically here and behave differently in B5. A screen
 * that pads for the same inset around this dock makes the consuming form a no-op and the reading form a
 * double-count. Review's finding: the first version documented that risk in prose and left it live, when the
 * idiomatic seam removes it. **B5 still owes the check**, but it now has to work at it to get it wrong.
 *
 * Not asserted by any test in this package: Robolectric reports a zero bottom inset at every qualifier B4
 * runs at, so a test could only prove `0 + 22 == 22`. Recorded as untested rather than pinned by a vacuous
 * assertion — it belongs to the B5 device passes, where a gesture bar actually exists.
 *
 * ### Where the button leads is B5's, exactly as the sheet's five rows were
 *
 * The frozen file wires **no handler** to `.start` — the only scripted buttons are the two prototype
 * controls (`:181-183`). So this reports the press and navigates nowhere; the roadmap's *"the CTA into the
 * existing paper chooser"* is route hand-over, which is B5's row. Inventing the destination here would be
 * inventing behaviour absent from the frozen design.
 *
 * @param onStart the button was pressed. B5 takes this to the paper chooser.
 * @param modifier the caller's. The frozen `.dock` is `position:absolute;bottom:0` **inside `.phone`** —
 *   the app window, which is B5's screen — so this composable does not place itself. B5 aligns it to
 *   [Alignment.BottomCenter] in the same [Box] that holds the shelf.
 */
@Composable
internal fun ZineDock(onStart: () -> Unit, modifier: Modifier = Modifier) {
    val desk = ZinelyTheme.v2Colors.desk

    Box(
        modifier
            .testTag(ZineDockTestTag)
            .fillMaxWidth()
            // CSS paints a background over the padding box, so the gradient is declared before the
            // padding it has to cover — including the 52px of fade that is the whole point of the band.
            .background(dockGradient(desk))
            // `env(safe-area-inset-bottom)`, as a *consuming* pad rather than a read value. See the
            // KDoc: this is what makes an enclosing consumer a no-op instead of a double-count.
            .windowInsetsPadding(SafeAreaBottom)
            .padding(
                start = DockPaddingHorizontal,
                end = DockPaddingHorizontal,
                top = DockPaddingTop,
                bottom = DockPaddingBottom,
            ),
        // `display:flex;justify-content:center` on a single child.
        contentAlignment = Alignment.Center,
    ) {
        StartButton(onStart)
    }
}

/**
 * `linear-gradient(to top,var(--desk) 80%,transparent)`.
 *
 * CSS measures `to top` from the bottom edge; [Brush.verticalGradient] measures from the top. So the
 * frozen stop at **80% up** is the stop at **20% down**, and the two ends swap: transparent at the top,
 * desk from 20% to the bottom. Writing it the CSS way round produces a band that fades *downward* into
 * nothing — a dock that dissolves at the button and is opaque where the covers scroll past, which is the
 * failure the frozen comment exists to prevent. `the fade is above the solid, not below it` asserts the
 * direction rather than the stop, because a transposed gradient is symmetrical enough to look deliberate.
 */
private fun dockGradient(desk: Color): Brush = Brush.verticalGradient(
    0f to Color.Transparent,
    DockFadeStop to desk,
    1f to desk,
)

/**
 * `.start` — the one primary action on the screen, `:91-95`, `:168`.
 *
 * ### The label colour is `--paper`, and **D-023** is open against it
 *
 * Every matcha fill in the **Bench** and **Proof** takes `var(--on-matcha)` — which the corpus marks
 * AA-critical on matcha — and the Library takes `var(--paper)` and declares no such token. Both work:
 * `--paper` is declared in both themes, inverts correctly, and clears AA in both directions — measured,
 * not assumed: **5.20:1** light (`#F7F2E7` on `#5E6B2F`) and **5.12:1** dark (`#2F2A22` on `#93A257`),
 * against `--on-matcha`'s own 5.80 and 5.72. A cream label on a warm green is also the reading the rest of
 * this screen supports, where nothing else is pure white.
 *
 * B4's first draft treated that as settling the question, on the ground that **D-005**, **D-011** and
 * **D-022** were each *broken* while this is merely different. Review rejected it: D-005's font stack
 * rendered fine and D-011's `ease` is a valid curve, so "broken" is not what those rulings turned on —
 * **authorship date** is, and D-022's ruling states the general rule and says a fourth will appear. So the
 * frozen value is transcribed and the question is [D-023](docs/design/V2-SPEC-DEFECTS.md), awaiting a
 * ruling. The `by day` / `by night` tests pin the transcription and name the entry, which is what gives a
 * ruling something to flip.
 *
 * ### `:focus-visible` is transcribed, which **D-008** does not contradict
 *
 * `outline:2px solid var(--ink);outline-offset:3px` is stated by the frozen file, so it is transcription,
 * not invention. **D-008** — that two of the three frozen surfaces specify no focus appearance — is open
 * against *those*; its own ruling says the Library's own rules are the reference. Same standing as the
 * `.zine` ring B3 drew.
 *
 * ### The press moves the button, and the ring moves with it
 *
 * `.start:active{transform:translateY(2px)}` on `transition:transform .14s`, on the **standard** curve:
 * **D-011**'s ruling table names `.start` by line and assigns it standard (*"chrome mechanism"*), against
 * the frozen file's bare `ease`, which is the CSS default rather than a choice. A CSS `transform` carries
 * the element's outline with it, so [graphicsLayer] is declared **before** the ring's `drawBehind` and the
 * ring translates too. B3's `.zine` ring is deliberately the other way round for the opposite reason: that
 * cover *scales*, and a focus ring that shrinks under the thumb reads as the indicator breaking.
 */
@Composable
private fun StartButton(onStart: () -> Unit) {
    val colors = ZinelyTheme.v2Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()

    val duration = if (ZinelyTheme.v2Motion.reduceMotion) 0 else StartPressDurationMillis
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = duration, easing = ZinelyV2Standard),
        label = "zineStartPress",
    )

    val labelStyle = TextStyle(
        fontFamily = ZinelyTheme.v2Typography.work,
        fontWeight = FontWeight.SemiBold,
        fontSize = StartLabelSize,
        color = colors.paper,
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StartGap),
        modifier = Modifier
            // Before `zinelyV2Control`: that seam ends in `clearAndSetSemantics`, which discards a tag
            // chained after it. See its KDoc — the node simply becomes unfindable, silently.
            .testTag(ZineStartTestTag)
            .graphicsLayer { translationY = StartPressTranslation.toPx() * press }
            // The shadow first, then the ring over it: CSS paints `outline` above `box-shadow`, and this
            // button's shadow is wide enough (`30px` blur) to tint the ring visibly if the order is flipped.
            .zinelyV2Shadow(StartShadow(colors.shadow), StartShape)
            .drawBehind { if (focused) drawFocusRing(colors.ink) }
            .clip(StartShape)
            .background(colors.matcha)
            .zinelyV2Control(
                label = StartLabelText,
                onClick = onStart,
                interactionSource = interaction,
            )
            .padding(horizontal = StartPaddingHorizontal, vertical = StartPaddingVertical),
    ) {
        StartPlus(colors.paper)
        Text(text = StartLabelText, style = labelStyle)
    }
}

/**
 * `.start .plus{font-size:1.2rem;line-height:0;margin-top:-2px}` — the `＋` at `:168`.
 *
 * ### `line-height:0` is a measurement instruction, and dropping it changes the button's height
 *
 * A flex item with `line-height:0` occupies a **zero-height** line box; the glyph overflows it
 * symmetrically about the baseline and contributes nothing to the row. So `.start`'s height is set by the
 * 16px label alone, and the 19.2px plus does not inflate it. Laid out here the same way — measured, then
 * reported as zero height and placed at `-height/2`, which puts the baseline at `(ascent-descent)/2` from
 * the line box exactly as a zero-height line box does — with `margin-top:-2px` contributing a further **1dp**
 * lift, because `align-items:center` centres the margin box (see [StartPlusMarginTop]). A plain `Text` in the
 * row would be taller than the label and would raise the button past the frozen 15px/26px padding, which is
 * the kind of miss a screenshot ratifies.
 *
 * ### `＋` is U+FF0B and no bundled weight has it — which **D-021** already answers
 *
 * Parsing the `cmap` of all seven bundled faces: U+FF0B FULLWIDTH PLUS SIGN is absent from every one of
 * them (U+2192, U+2014 and U+2019 elsewhere in B4 are present in Inter). **D-021**'s ruling covers this
 * without a new question: *"Keep the literal characters exactly as defined by the frozen HTML. Do not
 * substitute icons. Do not redesign the marks. Bundled-font coverage does not justify changing the design.
 * Platform fallback is acceptable."* So the glyph stands and the platform supplies it — the same standing
 * as B3's five sheet marks, four of which fall back too. `the plus is the frozen fullwidth character`
 * pins the codepoint, because "＋" and "+" are indistinguishable in a diff and not on screen.
 */
@Composable
private fun StartPlus(tint: Color) {
    Text(
        text = StartPlusGlyph,
        style = TextStyle(
            fontFamily = ZinelyTheme.v2Typography.work,
            // Inherited from `.start{font-weight:600}`; `.plus` overrides only the size.
            fontWeight = FontWeight.SemiBold,
            fontSize = StartPlusSize,
            color = tint,
        ),
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, 0) {
                // `-height/2` is the zero-height line box; the margin adds half of itself on top of it.
                placeable.place(x = 0, y = -placeable.height / 2 + StartPlusMarginTop.roundToPx() / 2)
            }
        },
    )
}

/**
 * `.start:focus-visible{outline:2px solid var(--ink);outline-offset:3px}`.
 *
 * A CSS outline starts at the offset **outside** the border box and grows outward from there, so a 2px
 * outline at a 3px offset occupies 3–5px out and its stroke centre is 4px out. Stroked rather than
 * bordered for B3's reason: `Modifier.border` paints *inside* the bounds, which draws a ring that eats
 * into the button's own fill instead of surrounding it — a mutation B3's review caught the absence of.
 * The corner radius grows with the offset, as a CSS outline's does.
 */
private fun DrawScope.drawFocusRing(ink: Color) {
    val stroke = ZinelyV2Dimens.FocusRingWidth.toPx()
    val out = StartFocusOffset.toPx() + stroke / 2f
    drawRoundRect(
        color = ink,
        topLeft = Offset(-out, -out),
        size = Size(size.width + 2 * out, size.height + 2 * out),
        cornerRadius = CornerRadius(StartRadius.toPx() + out),
        style = Stroke(width = stroke),
    )
}

/** `box-shadow:0 16px 30px -12px var(--shadow)` — one layer, and it does not change under the press. */
private fun StartShadow(shadow: Color): List<ZinelyV2ShadowLayer> = listOf(
    ZinelyV2ShadowLayer(dy = 16.dp, blur = 30.dp, spread = (-12).dp, color = shadow),
)

// ---------------------------------------------------------------------------------------------
// The frozen values, transcribed from `v2-library.html` at the lines named against each.
//
// Per-component literals, as B1's cover and B2's shelf have them: V2 publishes no spacing scale (D-007,
// ADR-074). 52 · 20 · 22 · 15 · 26 · 10 · 16 puts two of seven values on the 8pt grid, which is the same
// evidence B2 recorded and the same reason there is no `DockSpacing` object here.
// ---------------------------------------------------------------------------------------------

/** `.dock{padding:52px 20px calc(22px + env(safe-area-inset-bottom))}`. */
private val DockPaddingTop = 52.dp
private val DockPaddingHorizontal = 20.dp
private val DockPaddingBottom = 22.dp

/** `env(safe-area-inset-bottom)` — the bottom edge only; the band spans the width and has no side inset. */
private val SafeAreaBottom: WindowInsets
    @Composable get() = WindowInsets.navigationBars
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Bottom)

/** `linear-gradient(to top,var(--desk) 80%,…)`, expressed downward — see [dockGradient]. */
private const val DockFadeStop = 0.2f

/** `.start{border-radius:16px}` — symmetric, and chrome, so it is free to mirror (D-019). */
private val StartRadius = 16.dp
private val StartShape: Shape = RoundedCornerShape(StartRadius)

/** `.start{padding:15px 26px;gap:10px}`. */
private val StartPaddingVertical = 15.dp
private val StartPaddingHorizontal = 26.dp
private val StartGap = 10.dp

/** `.start{font-size:1rem}` against the browser's 16px root. `font-family:inherit` is the body's Inter. */
private val StartLabelSize = 16.sp

/** The button's own words, `:168`. Also its spoken name — it is a labelled control, not an icon. */
private const val StartLabelText = "Make a zine"

/**
 * `.plus` — U+FF0B FULLWIDTH PLUS SIGN. Not the ASCII `+`. See [StartPlus].
 *
 * `internal` so its codepoint can be pinned: the two characters are indistinguishable in a diff and not on
 * screen, and no rendered assertion can tell them apart on a host whose fallback font stack is a Robolectric
 * fiction. B3's five sheet marks are pinned the same way and for the same reason.
 */
internal const val StartPlusGlyph = "＋"

/** `.start .plus{font-size:1.2rem}` = 19.2px. */
private val StartPlusSize = 19.2.sp

/**
 * `.start .plus{margin-top:-2px}` — the frozen value, which lifts the glyph by **half of it**.
 *
 * `align-items:center` centres a flex item's **margin box**, not its border box. With `line-height:0` the
 * border box is 0 tall, so the margin box is `-2` tall; centring it puts its top at `(L+2)/2` and the border
 * box at `(L+2)/2 - 2 = L/2 - 1`. Against the unmargined `L/2`, the net lift is **1px**. Applied as
 * [StartPlusMarginTop] / 2 rather than as a pre-halved literal so the frozen number is what appears here and
 * the halving is visibly a consequence of the box model.
 *
 * This is the same rule as `.arrow{margin-bottom:18px}` and `.lbl{margin-top:9px}` in [ZineShelfEmpty], both
 * of which were transcribed correctly. Review caught the one cross-axis margin that was not.
 */
private val StartPlusMarginTop = (-2).dp

/** `.start:active{transform:translateY(2px)}` on `transition:transform .14s`. */
private val StartPressTranslation = 2.dp
private const val StartPressDurationMillis = 140

/** `.start:focus-visible{outline-offset:3px}`; the 2px width is [ZinelyV2Dimens.FocusRingWidth]. */
private val StartFocusOffset = 3.dp
