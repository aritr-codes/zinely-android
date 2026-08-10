package com.aritr.zinely.feature.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import java.util.Locale
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Grain
import com.aritr.zinely.ui.theme.ZinelyV21Press
import com.aritr.zinely.ui.theme.ZinelyV2Settle
import com.aritr.zinely.ui.theme.rememberZinelyV21GrainBrush
import com.aritr.zinely.ui.theme.zinelyV21Grain

/**
 * The **V2.1 zine cover** — `.cover` in `docs/design/mockups/v21-library.html`.
 *
 * A new component rather than a re-skin of [ZineCover], because the two are not the same object. V2's
 * cover carries the **title, the ink band, the crease and the fore-edge**, and prints the zine's name on
 * its face. V2.1's carries a flat ink fill, a centred mark, a spine hairline, a strip of tape and a
 * postmark stamp — and **the title has moved off the cover entirely**, to `.name`/`.sub` below it. A
 * component whose contents, shape, depth model and semantics all change is a different component;
 * editing V2's in place would have meant one file trying to be both, and would have taken 3 goldens and
 * 13 render tests with it. [ZineCover] stays exactly as it is until the shelf stops calling it.
 *
 * ### The one thing here that is not decoration: this is the first caller of the depth primitives
 *
 * `.cover` is [ZinelyV21Press.Hero] — `--hard` at rest, `1px` pressed — and the corpus proves the tier
 * is *not* "the one primary action per screen", because every tile on the shelf wears it. The press does
 * one extra thing the other Hero users do not: `transform:translate(2px,2px) rotate(0deg)` **drops the
 * tilt as well**, so a pressed cover squares itself up against the desk. That is transcribed rather than
 * simplified — it is the clearest signal in the language that these are objects being handled.
 *
 * ### Three colours here are deliberately theme-invariant
 *
 * The tape (`rgba(246,178,44,.55)`), the spine (`rgba(0,0,0,.18)`) and the mark (`rgba(255,246,232,.92)`)
 * are hardcoded in the frozen file and do **not** flip with the theme. That is the same ruling
 * [V21-SPEC §4.1](docs/design/V21-SPEC.md) already recorded for `.cover .mark`: a printed cover is the
 * maker's palette, not the app's chrome, and it does not restyle itself when the room goes dark. Do not
 * "fix" them onto tokens — the tape colour happens to equal light-theme `butter`, which is exactly the
 * coincidence that would make the mistake look like a cleanup.
 *
 * ### Tape and stamp overflow the cover, and the fill does not
 *
 * `.cover{overflow:visible}` with `.fill{overflow:hidden}`: the tape sits 11px above the top edge and the
 * stamp 7px past the right and 9px below the bottom, while the ink and its grain stop at the rounded
 * corners. So the clip lives on the fill layer, never on the cover box — and the caller must not put one
 * there either, or it takes the hard shadow with it ([zinelyV21Pressable]'s chain contract).
 *
 * @param fill the printed ink. From the zine's persisted recipe (**D-017** — a cover is assigned once at
 *   creation, so a rename cannot reprint a physical object).
 * @param stampLabel the paper size, set as a postmark — "A4", "Letter". Not a caption.
 * @param index the tile's position on the shelf, which is what the corpus's `:nth-child(3n+k)` selectors
 *   key the tilt and the tape's placement off. Position, not identity: the same zine tilts differently
 *   after a sibling is deleted, exactly as it does in the prototype.
 * @param pressed `.zine:active`.
 * @param mark the cover's centred glyph, drawn at 46% of the cover's width. A **slot**, not an icon id:
 *   `.cover .mark` takes its colour per surface (`rgba(255,246,232,.92)` on ink, `inkSoft` on paper), so
 *   the tint belongs to whoever knows which surface this is. The six glyphs are [ZineV21CoverMarks].
 *   (An earlier version justified the slot by the mark set being untranscribed. It was transcribed two
 *   commits later and the slot stayed, so that was never the real reason.)
 */
@Composable
internal fun ZineV21Cover(
    fill: Color,
    stampLabel: String,
    index: Int,
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    mark: @Composable BoxScope.(Modifier) -> Unit = {},
) {
    val colors = ZinelyTheme.v21Colors
    val grain = rememberZinelyV21GrainBrush(CoverGrainTileV21)
    val press = ZinelyV21Press.Hero
    val motion = ZinelyTheme.v2Motion

    // `transition:transform .16s cubic-bezier(.2,.8,.2,1), box-shadow .16s` on `.cover`.
    //
    // A first version read `if (pressed) press.travel else 0.dp` straight into the offset, so the one
    // object the whole screen is made of teleported 2dp and lost its tilt in a single frame, while
    // every sibling control in the package animated its press. A review caught it. On **settle**, per
    // the D-011 ruling, and collapsed to a cut under reduced motion by the same policy.
    val pressed01 by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(
            durationMillis = motion.durationMillis(CoverPressDurationMillis),
            easing = ZinelyV2Settle,
        ),
        label = "zineV21CoverPress",
    )
    val travel = press.travel * pressed01

    Box(
        modifier
            .aspectRatio(CoverAspectRatioV21)
            // The tilt is a render-time rotation, not a layout one: a tilted cover must not change the
            // space the grid gives it, or the shelf's columns would breathe as tiles were added.
            // `rotate(0deg)` on :active is why this reads `pressed` rather than being hoisted.
            .graphicsLayer { rotationZ = tiltFor(index) * (1f - pressed01) }
            // Travel and shadow, transcribed from the Hero tier, and stated inline rather than through
            // zinelyV21Pressable so the rotation can sit between them.
            //
            // An earlier comment here defended that order by claiming "CSS applies both in one
            // transform on the rotated box". A review pointed out that CSS *replaces* the whole
            // `transform` on `:active` — there is no composition — and that since `travel` is non-zero
            // only when `rotationZ` is 0, the two states are disjoint and the order is unobservable
            // either way. The chain is right; the argument was not. Kept in this shape because reading
            // rest and pressed off one expression each is clearer than splitting them, not because
            // anything forces it.
            .offset(x = travel, y = travel)
            .zinelyV21HardShadow(
                offset = press.rest - (press.rest - press.pressed) * pressed01,
                color = colors.inkLine,
                shape = CoverShapeV21,
            ),
    ) {
        // `border:1.5px solid var(--ink)` — a drawn line, so it follows `ink` and never `inkLine`
        // ([ZinelyV21Colors]). On the **border box**, which is the only thing at this level.
        Box(
            Modifier
                .fillMaxSize()
                .border(CoverBorderV21, colors.ink, CoverShapeV21),
        )

        // Everything else lives in the **padding box**, and that inset is not cosmetic.
        //
        // `*{box-sizing:border-box}` with `border:1.5px`: `.fill`, `.spine`, `.tape` and `.stamp` are
        // all absolutely positioned, so CSS resolves them against `.cover`'s *padding* box — 1.5px in
        // on every side — and `.mark` is grid content, so its `width:46%` is 46% of the content box
        // too. A first version measured all five against the border box, which put the spine 1.5dp
        // off, sized the mark 3dp large, and moved the tape's `left:50%` centre by 1.5dp.
        //
        // The inset also removes the question the previous version got wrong twice: with the fill
        // strictly inside the border, no draw order between the two is observable, so the border can
        // sit first here and the four overflowing children can still paint above it — which is what
        // `.tape`/`.stamp{z-index:3}` asks for.
        Box(Modifier.fillMaxSize().padding(CoverBorderV21)) {
            // `.fill` — the ink and its multiply grain, clipped to the cover's corners.
            //
            // Siblings rather than one modifier chain, and that is a correction. A first version
            // chained background -> grain -> spine -> border on a single Box and commented that "the
            // border is drawn last so neither the grain nor the spine tints the edge". **The opposite
            // was true.** Compose draw modifiers nest left-outermost and both `zinelyV21Grain` and a
            // `drawWithContent` call `drawContent()` before their own paint, so the real order was
            // background -> border -> spine -> grain: the border was drawn first and the grain
            // multiplied over it. A review caught it. Siblings draw in declaration order, so stating
            // them separately makes the order readable instead of inferred — which is what let the
            // mistake hide.
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CoverShapeV21)
                    .background(fill)
                    .zinelyV21Grain(grain, CoverGrainAlphaV21, ZinelyV21Grain.PaperBlend),
            )

            // `.spine` — a 1px shade 6px in from the binding edge, inset 7px top and bottom. Above the
            // grain, as its DOM order says. The one piece of the V2 cover's anatomy that survived.
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CoverShapeV21)
                    .drawBehind {
                        val top = SpineInsetVerticalV21.toPx()
                        drawRect(
                            color = SpineShade,
                            topLeft = Offset(SpineInsetStartV21.toPx(), top),
                            size = Size(SpineWidthV21.toPx(), size.height - 2 * top),
                        )
                    },
            )

            mark(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(CoverMarkWidthFraction),
            )

            // `.tape` — held to the desk. Overflows the top edge by 11dp, so it must not be clipped.
            //
            // `left:50%|38%|62%` with `translateX(-50%)`: the tape's CENTRE sits at that fraction of
            // the padding box's width. A first version mapped the three positions onto
            // Alignment.TopCenter/Start/End, which is not the same thing at all — Start is the left
            // edge, not 38% — and the parity raster showed it immediately.
            BoxWithConstraints(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .offset(y = TapeOffsetY),
            ) {
                val centre = maxWidth * tapeFractionFor(index)
                Box(
                    Modifier
                        .offset(x = centre - TapeWidth / 2)
                        .size(width = TapeWidth, height = TapeHeight)
                        .graphicsLayer { rotationZ = tapeRotationFor(index) }
                        // `box-shadow:0 1px 2px rgba(0,0,0,.12)` — the one soft shadow in a language
                        // of hard ones, because tape lifts off the paper rather than printing on it.
                        .shadow(TapeShadowElevation, clip = false)
                        .background(TapeFilm)
                        // `border-left`/`border-right:1px dashed rgba(255,255,255,.5)` — the torn
                        // edges. Without them the strip reads as a yellow rectangle rather than as
                        // tape, which is what a review found it doing.
                        .drawBehind {
                            val w = TapeEdgeWidth.toPx()
                            val dash = PathEffect.dashPathEffect(
                                floatArrayOf(TapeDashOn.toPx(), TapeDashOff.toPx()),
                            )
                            listOf(w / 2f, size.width - w / 2f).forEach { x ->
                                drawLine(
                                    color = TapeEdge,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = w,
                                    pathEffect = dash,
                                )
                            }
                        },
                )
            }

            // `.stamp` — the paper size as a postmark, hanging off the bottom-right corner.
            //
            // **Not drawn when there is nothing to stamp.** A subtitle carrying no `·` yields an empty
            // label, and the first version still drew the pill: a paper-filled, ink-bordered,
            // hard-shadowed blank hanging off the corner. Its own caller's KDoc claimed it "stamps
            // nothing rather than guessing", which a review checked and found false.
            if (stampLabel.isNotBlank()) Text(
                // `text-transform:uppercase`. Locale.ROOT rather than the default: this is a
                // paper-size code, not prose, and a Turkish locale would give "LETTER" a dotted I.
                text = stampLabel.uppercase(Locale.ROOT),
                style = stampStyle(colors.inkSoft),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = StampOffsetXV21, y = StampOffsetYV21)
                    .graphicsLayer { rotationZ = StampRotationV21 }
                    .zinelyV21HardShadow(StampShadowV21, colors.inkLine, StampShapeV21)
                    .background(colors.paper, StampShapeV21)
                    .border(CoverBorderV21, colors.ink, StampShapeV21)
                    .padding(horizontal = ZinelyV21Dimens.gapSm, vertical = ZinelyV21Dimens.gapHair),
            )
        }
    }
}

/** `border-radius: var(--br-xs) var(--br-md) var(--br-md) var(--br-xs)` — 4 / 14 / 14 / 4. */
internal val CoverShapeV21: Shape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 14.dp,
    bottomEnd = 14.dp,
    bottomStart = 4.dp,
)

private const val CoverAspectRatioV21 = 3f / 4f

/** `border:1.5px solid var(--ink)`. */
private val CoverBorderV21: Dp = 1.5.dp

/**
 * `.zine .cover{transition:transform .16s cubic-bezier(.2,.8,.2,1), box-shadow .16s cubic-bezier(.2,.8,.2,1)}`
 * — the Hero tier's travel, animated rather than teleported.
 *
 * **The duration is transcribed; the curve is not.** The corpus writes its own `cubic-bezier(.2,.8,.2,1)`
 * here and this uses [ZinelyV2Settle] `(.05,.7,.1,1)`, because
 * [D-011](docs/design/V2-SPEC-DEFECTS.md#d-011) is RESOLVED in favour of the shared motion tokens over the
 * Library file's inline curves. Quoting the declaration in full rather than eliding the part that needed a
 * ruling: the two curves are close (both leave fast and land slow) and neither is the other.
 */
private const val CoverPressDurationMillis = 160

/** `background-size:130px 130px; mix-blend-mode:multiply; opacity:.5` × the .42 baked alpha. */
private val CoverGrainTileV21: Dp = 130.dp
private const val CoverGrainAlphaV21 = ZinelyV21Grain.BakedAlpha * 0.5f

/** `.cover .mark{width:46%}`. */
private const val CoverMarkWidthFraction = 0.46f

/**
 * `:root{--tiltA:-1.4deg; --tiltB:1.1deg; --tiltC:-.6deg}`, applied by `:nth-child(3n+1|3n+2|3n)`.
 *
 * CSS counts children from 1, so tile 0 here is `3n+1`. `[data-tilt="off"]` zeroes all three in the
 * prototype; that is a prototype control, and its Compose equivalent is the platform's own
 * reduce-motion setting — a *static* rotation is not motion, so it is deliberately not silenced by it.
 */
private fun tiltFor(index: Int): Float = when (index % 3) {
    0 -> -1.4f
    1 -> 1.1f
    else -> -0.6f
}

/** `.tape{left:50%}`, then `:nth-child(3n+2){left:38%}` and `:nth-child(3n){left:62%}`. */
private fun tapeFractionFor(index: Int): Float = when (index % 3) {
    0 -> 0.50f
    1 -> 0.38f
    else -> 0.62f
}

private fun tapeRotationFor(index: Int): Float = when (index % 3) {
    0 -> -4f
    1 -> 5f
    else -> -2f
}

private val TapeWidth: Dp = 56.dp
private val TapeHeight: Dp = 19.dp
private val TapeOffsetY: Dp = (-11).dp

/**
 * `rgba(246,178,44,.55)` — hardcoded in the frozen file and **theme-invariant on purpose**. It equals
 * light-theme `butter`, which is the coincidence that makes tokenising it look like a tidy-up.
 */
private val TapeFilm = Color(0x8CF6B22C)

/**
 * `border-left`/`border-right:1px dashed rgba(255,255,255,.5)` — the strip's torn edges.
 *
 * CSS does not publish its dash rhythm; a browser derives one from the border width. `2on/2off` at 1px
 * is the usual rendering and is what this transcribes, which makes it the one **approximated** value on
 * this component — recorded rather than presented as frozen.
 */
private val TapeEdge = Color(0x80FFFFFF)
private val TapeEdgeWidth: Dp = 1.dp
private val TapeDashOn: Dp = 2.dp
private val TapeDashOff: Dp = 2.dp

/** `box-shadow:0 1px 2px rgba(0,0,0,.12)`, as the nearest elevation Compose expresses. */
private val TapeShadowElevation: Dp = 1.dp

/** `.spine{left:6px;top:7px;bottom:7px;width:1px;background:rgba(0,0,0,.18)}`. */
private val SpineInsetStartV21: Dp = 6.dp
private val SpineInsetVerticalV21: Dp = 7.dp
private val SpineWidthV21: Dp = 1.dp
private val SpineShade = Color(0x2E000000)

private val StampOffsetXV21: Dp = 7.dp
private val StampOffsetYV21: Dp = 9.dp
private val StampRotationV21 = -3f
private val StampShadowV21: Dp = 2.dp
private val StampShapeV21: Shape = RoundedCornerShape(percent = 50)

/**
 * `.stamp` — Inter 700 at 9.5px, `letter-spacing:.08em`, uppercase, in `inkSoft`.
 *
 * The size is carried as `9.5.sp` rather than rounded for the reason [ZineCover]'s title gives: this is
 * the smallest type in the design and half a point is visible at that size.
 */
private fun stampStyle(color: Color) = TextStyle(
    fontFamily = ZinelyV21Fonts.Work,
    fontWeight = FontWeight.Bold,
    fontSize = 9.5.sp,
    // `body{line-height:1.55}`, inherited — `.stamp` declares none.
    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
    letterSpacing = 0.08.em,
    color = color,
)
