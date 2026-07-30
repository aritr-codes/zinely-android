package com.aritr.zinely.feature.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.a11y.zinelyV2Control
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Dimens
import com.aritr.zinely.ui.theme.ZinelyV2Settle

/** The `⋯` on one placed cover — `.more`, keyed by position like [zineShelfCoverTestTag]. */
internal fun zineShelfMoreTestTag(index: Int): String = "shelf-more-$index"

/**
 * `.zine` — one object on the shelf, and the two ways into it — `v2-library.html:51-54`, `:73-77`, `:199-209`.
 *
 * ```css
 * .zine{position:relative;cursor:pointer;-webkit-tap-highlight-color:transparent;transition:transform .16s}
 * .zine:active{transform:translateY(2px) scale(.985)}
 * .zine:focus-visible{outline:2px solid var(--matcha-text);outline-offset:6px;border-radius:9px}
 * .more{position:absolute;bottom:8px;right:8px;z-index:2;width:34px;height:34px;opacity:.5;border-radius:50%}
 * ```
 *
 * The frozen script states the interaction in one line — *"tap = open zine · long-press = actions · …
 * button = actions"* (`:199`) — and the CSS states what a press looks like. That is the whole of B3's
 * behaviour on the shelf; the sheet those two gestures open is [ZineActionSheet].
 *
 * ### Why the `⋯` is a sibling of the cover and not inside it
 *
 * The frozen markup nests `.more` inside `.cover`, and B1 shipped an `overlay` slot to match. B3 deleted
 * that slot. The cover is the tappable object, and the seam that gives it a real `android.widget.Button`
 * ends in `clearAndSetSemantics` — which discards every descendant's semantics, a nested button's included.
 * Nesting the affordance would therefore have produced a `⋯` that no screen reader could reach: the precise
 * opposite of the thing `.more` exists to be. As a sibling inside the same wrapper it occupies the same
 * pixels (the wrapper has no padding of its own, so `BottomEnd` + 8dp is the cover's own bottom-right) and
 * stays a node of its own.
 *
 * ### The `⋯` is not a redundant control
 *
 * The frozen file explains itself where it declares it: *"quiet, visible actions affordance (the fallback;
 * long-press is the accelerator)"* (`:72`). It is `display:flex` at `opacity:.5` **unconditionally** — no
 * hover gate, no long-press-discovered state — because it is the path a screen reader, a keyboard and a
 * switch device can reach, and it is the frozen answer to the *"screen-reader path for long-press"* gate the
 * file's own header lists (`:8`). It is drawn at every rest state, so it belongs to every raster of a shelf.
 *
 * ### Two platform obligations the CSS cannot state, both authorised and both logged
 *
 * - **The touch target.** `.more` is 34×34, under the 48dp floor
 *   ([D-009](docs/design/V2-SPEC-DEFECTS.md)). The ruling is that targets are met *"in a manner that is
 *   visually subordinate to the frozen design"* and that the design must not be resized to suit them — so
 *   the mark stays 34dp and the seam's `minimumInteractiveComponentSize()` grows only what a finger hits.
 *   Both halves are asserted, because meeting the floor by drawing a bigger button would pass a naïve test.
 * - **The long-press timing.** The mock fires at 420ms (`:202`); this uses the platform's own long-press
 *   timeout via `combinedClickable`, per the implementation guide's rule that the HTML is a browser mock and
 *   the device is real. Recorded on [zinelyV2Control], which is where the gesture now lives.
 *
 * ### The haptic, and why there is exactly one
 *
 * A long-press that opens a sheet gets [ZinelyHaptic.Boundary] — V1's `ShelfCard` fires the same verb on the
 * same gesture on the same object, and the platform's own buzz is suppressed so only one plays. A **tap
 * fires none**: the note on V1's `zinelyControl` records the spec's vocabulary as *"Open = nothing"*, and the
 * tap's confirmation is the screen it opens. Neither is invented here; both are read off the repository,
 * and the frozen HTML says nothing about haptics because HTML cannot.
 *
 * @param zine what to print and, for the sheet, what to disclose.
 * @param index the position this object stands at — the identity a position-keyed grid has, and what both
 *   callbacks report. **B5** replaces it with a project id.
 * @param onOpen tap. Where it goes is B5's: the frozen script's own comment marks it *"(mock: no-op)"*.
 * @param onActions long-press, or the `⋯`. The caller opens [ZineActionSheet] with this zine.
 */
@Composable
internal fun ZineOnShelf(
    zine: ZineShelfItem,
    index: Int,
    onOpen: (Int) -> Unit,
    onActions: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v2Colors
    val haptics = ZinelyTheme.haptics

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()

    // `transition:transform .16s` — settle, per the D-011 ruling, and collapsed to a cut under reduced
    // motion by the same policy B1's cover shadow follows.
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(
            durationMillis = ZinelyTheme.v2Motion.durationMillis(PressDurationMillis),
            easing = ZinelyV2Settle,
        ),
        label = "zineOnShelfPress",
    )

    Box(
        modifier
            // `:focus-visible{outline:2px solid var(--matcha-text);outline-offset:6px;border-radius:9px}`.
            // The Library is the only frozen surface that authored a focus appearance, which is why this is
            // transcription rather than the invention D-008 forbids — and the ring sits outside the
            // transform so a pressed, focused cover does not carry a shrinking outline.
            .drawBehind {
                if (!focused) return@drawBehind
                // A CSS outline is drawn *outside* the box: its inner edge sits `outline-offset` out from
                // the edge and its thickness grows outward, so the stroke's centre line is one half-width
                // beyond that. Drawn here rather than with a `border`, which would have to eat 6dp of
                // layout to sit outside — and would then shrink the cover to make room for its own ring.
                val stroke = FocusRingWidthPx()
                val out = FocusRingOffset.toPx() + stroke / 2f
                drawRoundRect(
                    color = colors.matchaText,
                    topLeft = Offset(-out, -out),
                    size = Size(size.width + 2 * out, size.height + 2 * out),
                    cornerRadius = CornerRadius(FocusRingRadius.toPx()),
                    style = Stroke(width = stroke),
                )
            }
            // `translateY(2px) scale(.985)` about the default 50% 50% origin. On the wrapper, so the `⋯`
            // travels with the object it belongs to — `.zine:active` transforms the whole item in CSS.
            .graphicsLayer {
                translationY = PressTranslation.toPx() * press
                val scale = 1f - (1f - PressScale) * press
                scaleX = scale
                scaleY = scale
            },
    ) {
        ZineCover(
            title = zine.title,
            recipe = zine.recipe,
            pressed = pressed,
            modifier = Modifier
                // Before the seam, never after: the seam ends in `clearAndSetSemantics`, and a tag chained
                // behind it leaves a node no test and no service can find.
                .testTag(zineShelfCoverTestTag(index))
                .zinelyV2Control(
                    // The frozen `.zine` carries no `aria-label` — it is a `<div>` with pointer handlers, and
                    // only `.more` was given one. An unnamed control reaches TalkBack silent, so the object's
                    // own name stands in: the title it already prints, not a sentence invented for it. The
                    // gestures are named by their action twins instead.
                    label = zine.title,
                    interactionSource = interaction,
                    onClick = { onOpen(index) },
                    onLongClick = {
                        haptics.perform(ZinelyHaptic.Boundary)
                        onActions(index)
                    },
                    onLongClickLabel = LongPressActionLabel,
                ),
        )
        MoreButton(
            title = zine.title,
            ink = zine.recipe.surface.palette(ZinelyTheme.contentInks).onFill,
            onClick = { onActions(index) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(MoreInset)
                .testTag(zineShelfMoreTestTag(index)),
        )
    }
}

/**
 * `.more` — the quiet `⋯`.
 *
 * `color:currentColor` inherits the cover's printed ink, so the mark is the same colour as the title beside
 * it and changes with the stock. `opacity:.5` at rest, `.95` with a background wash on hover or focus.
 */
@Composable
private fun MoreButton(
    title: String,
    ink: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val lit = hovered || focused

    Box(
        modifier
            .size(MoreSize)
            .clip(CircleShape)
            // `.more:hover,.more:focus-visible{opacity:.95;background:rgba(0,0,0,.10)}`
            .background(if (lit) MoreLitWash else Color.Transparent)
            // `.more:focus-visible{outline:2px solid currentColor;outline-offset:0}` — a ring on the circle,
            // since `border-radius:50%` shapes the outline too. Drawn *outside* the 34px box like `.zine`'s,
            // because that is what a CSS outline does at any offset including zero: `Modifier.border` paints
            // inside the box and would eat 2px of the mark instead of surrounding it.
            .drawBehind {
                if (!focused) return@drawBehind
                val stroke = FocusRingWidthPx()
                drawCircle(
                    color = ink,
                    radius = (size.minDimension + stroke) / 2f,
                    style = Stroke(width = stroke),
                )
            }
            .zinelyV2Control(
                // `aria-label="Actions for Sunday market"` — the frozen file's own string, `:149-154`.
                label = "Actions for $title",
                interactionSource = interaction,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = MoreGlyph,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = ZinelyTheme.v2Typography.work,
                fontSize = MoreGlyphSize,
                color = ink.copy(alpha = if (lit) MoreLitOpacity else MoreRestOpacity),
            ),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// The frozen values — per component, per the D-007 ruling.
// ---------------------------------------------------------------------------------------------

/** `.zine:active{transform:translateY(2px) scale(.985)}` */
private val PressTranslation = 2.dp
private const val PressScale = 0.985f

/** `transition:transform .16s` */
private const val PressDurationMillis = 160

/** `.zine:focus-visible{outline-offset:6px;border-radius:9px}` */
private val FocusRingOffset = 6.dp
private val FocusRingRadius = 9.dp

/** `outline:2px` — the one number all four frozen focus rules agree on, so A4 tokenised it. */
private fun DrawScope.FocusRingWidthPx(): Float = with(this) { ZinelyV2Dimens.FocusRingWidth.toPx() }

/** `.more{bottom:8px;right:8px;width:34px;height:34px;font-size:1.05rem}` */
private val MoreInset = 8.dp
private val MoreSize = 34.dp
private val MoreGlyphSize = 16.8.sp

/**
 * `⋯` U+22EF, the frozen character — **not in the bundled Inter** (D-021), so the device's fallback draws
 * the most visible mark B3 adds. Named here so the ruling costs one line.
 */
private const val MoreGlyph = "⋯"

/** `opacity:.5` at rest, `.95` lit. */
private const val MoreRestOpacity = 0.5f
private const val MoreLitOpacity = 0.95f

/** `background:rgba(0,0,0,.10)` — a literal, unchanged between themes, like the scrim's. */
private val MoreLitWash = Color.Black.copy(alpha = 0.10f)

/**
 * The name of the long-press gesture in the platform tree.
 *
 * The frozen file names the *sheet* (`aria-label="Zine actions"`) and the `⋯` (`"Actions for …"`) but never
 * the gesture, because a browser has no such concept. "Actions" is the shortest name consistent with both.
 */
private const val LongPressActionLabel = "Actions"
