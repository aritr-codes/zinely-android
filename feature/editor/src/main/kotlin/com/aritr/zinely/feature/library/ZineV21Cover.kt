package com.aritr.zinely.feature.library

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.aritr.zinely.ui.components.zinelyV21HardShadow
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import com.aritr.zinely.ui.theme.ZinelyV21Grain
import com.aritr.zinely.ui.theme.ZinelyV21Press
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
 * @param onFill the mark's colour: the near-paper film on an ink surface, `inkSoft` on a paper one.
 * @param stampLabel the paper size, set as a postmark — "A4", "Letter". Not a caption.
 * @param index the tile's position on the shelf, which is what the corpus's `:nth-child(3n+k)` selectors
 *   key the tilt and the tape's placement off. Position, not identity: the same zine tilts differently
 *   after a sibling is deleted, exactly as it does in the prototype.
 * @param pressed `.zine:active`.
 * @param mark the cover's centred glyph, drawn at 46% of the cover's width. A slot rather than an icon
 *   id, because V2.1's mark set is not transcribed yet and inventing one here would be the fabrication
 *   D-006 refused — the caller passes what it has.
 */
@Composable
internal fun ZineV21Cover(
    fill: Color,
    onFill: Color,
    stampLabel: String,
    index: Int,
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    mark: @Composable BoxScope.(Modifier) -> Unit = {},
) {
    val colors = ZinelyTheme.v21Colors
    val grain = rememberZinelyV21GrainBrush(CoverGrainTileV21)
    val press = ZinelyV21Press.Hero
    val travel = if (pressed) press.travel else 0.dp

    Box(
        modifier
            .aspectRatio(CoverAspectRatioV21)
            // The tilt is a render-time rotation, not a layout one: a tilted cover must not change the
            // space the grid gives it, or the shelf's columns would breathe as tiles were added.
            // `rotate(0deg)` on :active is why this reads `pressed` rather than being hoisted.
            .graphicsLayer { rotationZ = if (pressed) 0f else tiltFor(index) }
            // Travel and shadow, transcribed from the Hero tier. Not zinelyV21Pressable, because the
            // rotation above has to sit between the two — a layout offset applied outside the
            // graphicsLayer would translate along the *desk*, and inside it would translate along the
            // *tilted cover*. CSS applies both in one transform on the rotated box, which is this order.
            .offset(x = travel, y = travel)
            .zinelyV21HardShadow(press.offset(pressed), colors.inkLine, CoverShapeV21),
    ) {
        // `.fill` — the ink, its multiply grain, the spine, and the only clip on this component.
        //
        // The border is drawn last so neither the grain nor the spine tints the edge, and it follows
        // `ink` rather than `inkLine`: a border is a drawn line, a shadow is the absence of light
        // ([ZinelyV21Colors]). `border()` inside the clip keeps the stroke inside the bounds, which is
        // what a CSS `border` on a `border-box` element does.
        Box(
            Modifier
                .fillMaxSize()
                .clip(CoverShapeV21)
                .background(fill)
                .zinelyV21Grain(grain, CoverGrainAlphaV21, ZinelyV21Grain.PaperBlend)
                // `.spine` — a 1px shade 6px in from the binding edge, inset 7px top and bottom. The
                // one piece of the V2 cover's anatomy that survived the re-freeze.
                .drawWithContent {
                    drawContent()
                    val top = SpineInsetVerticalV21.toPx()
                    drawRect(
                        color = SpineShade,
                        topLeft = Offset(SpineInsetStartV21.toPx(), top),
                        size = Size(SpineWidthV21.toPx(), size.height - 2 * top),
                    )
                }
                .border(CoverBorderV21, colors.ink, CoverShapeV21),
        )

        mark(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(CoverMarkWidthFraction),
        )

        // `.tape` — held to the desk. Overflows the top edge by 11dp, so it must not be clipped.
        //
        // `left:50%|38%|62%` with `translateX(-50%)`: the tape's CENTRE sits at that fraction of the
        // cover's width. A first version mapped the three positions onto Alignment.TopCenter/Start/End,
        // which is not the same thing at all — Start is the left edge, not 38% — and the parity raster
        // showed it immediately. Measured against the cover's own width, so it tracks the cell size.
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
                    .background(TapeFilm),
            )
        }

        // `.stamp` — the paper size as a postmark, hanging off the bottom-right corner.
        Text(
            text = stampLabel,
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
    letterSpacing = 0.08.em,
    color = color,
)
