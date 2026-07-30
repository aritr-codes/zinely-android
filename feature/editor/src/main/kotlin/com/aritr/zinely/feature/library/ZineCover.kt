package com.aritr.zinely.feature.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSupported
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.components.zinelyV2Shadow
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Grain
import com.aritr.zinely.ui.theme.ZinelyV2Settle
import com.aritr.zinely.ui.theme.ZinelyV2ShadowLayer
import com.aritr.zinely.ui.theme.rememberZinelyV2GrainBrush
import com.aritr.zinely.ui.theme.rememberZinelyV2Icon

/**
 * The frozen Library's **Maker's Cover** — `v2-library.html:57-84`, `:149-154`.
 *
 * A zine as *a printed paper object sitting on the surface*: a 3:4 sheet of stock, riso grain in the
 * paper rather than over it, one band of ink printed across the upper third, a scored fold down the
 * spine side, a fore-edge shadow where the leaves stack, a title in the voice face, and a small stamp.
 * There is no page thumbnail and there never will be one: covers are **recipes**
 * ([ADR-069](docs/DECISIONS.md#adr-069)), so nothing renders, encodes or caches a raster per zine.
 *
 * ### What this composable is and is not
 *
 * It is the shelf's *atom* — paint only. It holds no gesture, no click, no semantics of its own beyond
 * the title it draws, and it takes [pressed] as state rather than detecting it: the press belongs to
 * the `.zine` container (`v2-library.html:51-53`), which is the shelf item, not the cover. The `⋯`
 * affordance (`.more`, `:73-77`) likewise belongs to the item and arrives through [overlay], which is
 * how it can be positioned inside the cover's bounds — as the frozen markup nests it — without this
 * file growing an interaction.
 *
 * ### Paint order is the CSS paint order, and it is load-bearing
 *
 * 1. **fill** (`background-color`), then **grain** (`background-image` + `background-blend-mode`) blended
 *    into that fill and nothing else.
 * 2. the **1px inner hairline** (`inset 0 0 0 1px rgba(0,0,0,.05)`) — an inset shadow paints above the
 *    background and below the children.
 * 3. the **fold spine** (`::before`), then the **band**, then the **fore-edge** (`::after`) — pseudo- and
 *    real children in DOM order, all at `z-index:auto`.
 * 4. the **title** and the **stamp** (`z-index:1`), then [overlay] (`.more`, `z-index:2`).
 *
 * Steps 1–3 are **one draw scope**, and that is a correctness requirement rather than a tidiness one:
 * a blend mode composites against what is already in its own layer, so the band drawn as a separate
 * child node found a transparent backdrop and its `multiply` degraded silently to `src-over` — the
 * band came out 83/255 red where the design asks for 47/255. `ZineCoverRenderTest` caught it; nothing
 * about the composition looked wrong. Same reason the grain is painted here from
 * [ZinelyV2Grain]'s own [ZinelyV2Grain.Blend] and [ZinelyV2Grain.IsSupported] rather than through
 * `Modifier.zinelyV2Grain`: that modifier draws the noise *after* the content it wraps, which is right for a page
 * or a desk and wrong for a cover, where the ink band has to land on top of the grained stock. The
 * API-29 policy is read from the token object either way, so there is one policy, not two.
 *
 * ### Grain strength is 1.00 here, and that is not a transcription slip
 *
 * The Bench and Proof draw the same tile at 0.15–0.25. **D-013** ruled the gap deliberate: paper and a
 * printed cover are different physical materials and take ink differently, so the per-surface strengths
 * are the specification. Do not normalise them. Below API 29 the grain draws nothing at all
 * (**D-014** — soft-light does not exist there and an approximation of a material is a second
 * material), so a cover on those devices is flat stock. That is a disclosed Known Limitation.
 *
 * ### On API 24–28 the band is not drawn either
 *
 * `multiply` is missing on the same devices for the same reason, and **D-018** ruled it the same way:
 * omit, do not approximate. So a cover there is stock, crease, fore-edge, stamp and title — see
 * [drawInkBand]. The two omissions are one Known Limitation about one platform ceiling.
 *
 * ### The cover is a physical object, and does not mirror
 *
 * **D-019**, ruled: the printed artifact keeps its binding edge, crease and fore-edge exactly as frozen
 * in every locale — chrome may adapt to RTL, a printed object does not. See [CoverShape], which is
 * absolute for that reason. The title still follows the layout direction, as it does in CSS.
 *
 * ### The two deviations from the frozen file, both logged
 *
 * - **Fraunces 500, not the file's 600.** `.ct` inherits `font-weight:600` from a `.serif` stack that
 *   names Iowan Old Style. **D-005** ruled the canonical V2 serif is Fraunces at **500**, the 600 being
 *   an artefact of the Georgia fallback the Library was authored against. The register is the authority
 *   and the HTML is stale on both counts.
 * - **`text-wrap:balance` is not implemented.** Compose has no line-balancing pass, and there is no
 *   honest approximation of one — a hand-tuned break would be a *different* design decision wearing the
 *   spec's clothes. The 9ch measure and the two-line clamp are transcribed exactly; the balance is
 *   omitted and disclosed rather than faked.
 *
 * @param title the zine's own title. **Drawn and measured here, and nothing else** — no part of the
 *   [recipe] is derived from it, on any path. An earlier draft of this file hashed the title into the
 *   recipe and this `@param` still described that mechanism after **D-017** deleted it; the ruling is that
 *   a cover is assigned once at creation and persisted, so that a rename cannot reprint a physical object.
 * @param recipe which surface and stamp this zine prints on — see [ZineCoverRecipe]. Assigned by the
 *   caller and persisted with the zine (**D-017**); B1 ships no assigner at all and **B5** brings one,
 *   next to the stored field it needs. See [ZineCoverRecipe]'s own note for why it is not here.
 * @param pressed `.zine:active` — flattens the shadow toward the desk. The cover does not rise: the
 *   frozen `:active` rule changes `box-shadow` only.
 * @param overlay content at `z-index:2` inside the cover's bounds; the shelf item's `⋯` lands here.
 */
@Composable
internal fun ZineCover(
    title: String,
    recipe: ZineCoverRecipe,
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val palette = recipe.surface.palette(ZinelyTheme.contentInks)
    val colors = ZinelyTheme.v2Colors

    // `transition: box-shadow .16s` — on the settle curve, per the D-011 ruling (the frozen file's own
    // bare `ease` is the CSS default rather than a choice, and predates the shared token layer).
    // Reduced motion collapses the one-shot to its end state, which is ZinelyV2Motion's policy for
    // one-shot motion; there is nothing continuous here to silence.
    val duration = if (ZinelyTheme.v2Motion.reduceMotion) 0 else CoverPressDurationMillis
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = duration, easing = ZinelyV2Settle),
        label = "zineCoverPress",
    )

    val grain = rememberZinelyV2GrainBrush(CoverGrainTile)
    val titleStyle = coverTitleStyle(palette.onFill)
    val titleMaxWidth = nineCharacters(titleStyle)

    Box(
        modifier
            .aspectRatio(CoverAspectRatio)
            .zinelyV2Shadow(coverShadow(press, colors.contact, colors.shadow), CoverShape)
            .clip(CoverShape),
    ) {
        // 1-3 — the stock, its grain, the inner hairline, the crease, the ink band, the fore-edge.
        Canvas(Modifier.matchParentSize()) { drawCoverPrint(palette.fill, palette.band, grain) }

        // 4 — `.stamp{top:12px;right:13px;width:22px;height:22px;opacity:.8;transform:rotate(-8deg)}`.
        // Decorative: the cover's meaning is its title, and a screen reader announcing "sun" over a
        // zine called "Sunday market" would be inventing content the design does not have.
        Icon(
            imageVector = rememberZinelyV2Icon(recipe.stamp.icon(), StampSize),
            contentDescription = null,
            tint = palette.onFill,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = StampInsetTop, end = StampInsetEnd)
                .size(StampSize)
                .graphicsLayer {
                    rotationZ = StampRotationDegrees
                    alpha = StampAlpha
                },
        )

        // 4 — `.ct` at the bottom of a `justify-content:flex-end` column, inside the cover's padding.
        Text(
            text = title,
            style = titleStyle,
            maxLines = CoverTitleMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = CoverPaddingHorizontal,
                    end = CoverPaddingHorizontal,
                    bottom = CoverPaddingBottom,
                )
                .widthIn(max = titleMaxWidth)
                .padding(bottom = CoverTitlePaddingBottom),
        )

        overlay()
    }
}

/**
 * `.ct{font-size:1.16rem;font-weight:600→500;line-height:1.2;letter-spacing:-.01em}` in the voice face.
 *
 * `1.16rem` against the browser's 16px root is **18.56px**, carried as `18.56.sp` rather than rounded:
 * a cover title is the first type a user meets, and 18.56 → 18 or 19 is a visible change to it. The
 * line-height and tracking stay in `em` because that is the unit the frozen CSS states them in, so they
 * scale with the size instead of being pre-multiplied here.
 */
@Composable
private fun coverTitleStyle(onFill: Color): TextStyle = TextStyle(
    fontFamily = ZinelyTheme.v2Typography.voice,
    // D-005: Fraunces 500 is the canonical V2 serif weight; the file's 600 is a Georgia artefact.
    fontWeight = FontWeight.Medium,
    fontSize = CoverTitleSize,
    lineHeight = CoverTitleLineHeight,
    letterSpacing = CoverTitleTracking,
    color = onFill,
)

/**
 * `max-width:9ch` — nine advances of "0" in the used font, which is what the CSS `ch` unit *is*.
 *
 * Measured rather than approximated as a fraction of the cover: `ch` depends on Fraunces' own zero
 * advance, so a fraction that looked right at one size would be wrong at another and wrong again under
 * font scaling.
 */
@Composable
private fun nineCharacters(style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(measurer, density, style) {
        val zeros = measurer.measure("0".repeat(CoverTitleMaxCharacters), style, softWrap = false)
        with(density) { zeros.size.width.toDp() }
    }
}

/**
 * `box-shadow` at rest and under `:active`, interpolated by [press].
 *
 * Rest: `0 2px 3px -1px contact, 0 20px 24px -16px shadow`.
 * Pressed: `0 1px 2px -1px contact, 0 8px 12px -10px shadow`.
 *
 * The third declared layer in both rules is the `inset` hairline, which is a border rather than a
 * shadow and is drawn by [drawCoverPrint] — see [zinelyV2Shadow]'s KDoc.
 *
 * Interpolating the layers is what a CSS `transition: box-shadow` does; snapping between two sets at
 * the halfway point would read as the object flinching rather than settling.
 */
private fun coverShadow(press: Float, contact: Color, shadow: Color): List<ZinelyV2ShadowLayer> = listOf(
    ZinelyV2ShadowLayer(
        dy = lerp(2.dp, 1.dp, press),
        blur = lerp(3.dp, 2.dp, press),
        spread = (-1).dp,
        color = contact,
    ),
    ZinelyV2ShadowLayer(
        dy = lerp(20.dp, 8.dp, press),
        blur = lerp(24.dp, 12.dp, press),
        spread = lerp((-16).dp, (-10).dp, press),
        color = shadow,
    ),
)

/**
 * The whole printed sheet, in the frozen paint order, in one layer — see [ZineCover]'s KDoc for why the
 * single layer is load-bearing rather than incidental.
 */
private fun DrawScope.drawCoverPrint(fill: Color, band: Color, grain: ShaderBrush) {
    drawRect(fill)
    drawGrain(grain)
    drawInnerHairline()
    drawFoldSpine()
    drawInkBand(band)
    drawForeEdge()
}

/**
 * `background-image:var(--grain); background-size:140px; background-blend-mode:soft-light` at the
 * Library's effective strength of 1.00 (**D-013** — do not normalise this against the Bench's 0.15–0.25).
 *
 * Draws **nothing** where the platform cannot blend soft-light (**D-014**, API 24–28): a cover on those
 * devices is flat stock, because an approximation of a material is a second material. The predicate and
 * the blend both come from [ZinelyV2Grain], so this cannot drift from the paper's own policy.
 */
private fun DrawScope.drawGrain(grain: ShaderBrush) {
    if (!ZinelyV2Grain.IsSupported) return
    drawRect(brush = grain, alpha = CoverGrainAlpha, blendMode = ZinelyV2Grain.Blend)
}

/**
 * `inset 0 0 0 1px rgba(0,0,0,.05)` — a 1px hairline *inside* the cover's own rounded edge.
 *
 * Stroked at twice the width along the boundary and left to the caller's clip to cut the outer half.
 * That is exact rather than approximate: a centred 2px stroke on the outline leaves precisely 1px
 * inside, and it follows the asymmetric 6/9/9/6 radius that no `drawRoundRect` overload can express.
 */
private fun DrawScope.drawInnerHairline() {
    drawPath(
        path = CoverShape.outlinePath(this),
        color = CoverHairline,
        style = Stroke(width = 2 * CoverHairlineWidth.toPx()),
    )
}

/**
 * `.cover::before{left:9px;top:6px;bottom:6px;width:1px;background:linear-gradient(…)}` — the scored
 * fold, a highlight where the sheet is creased rather than a drawn line.
 */
private fun DrawScope.drawFoldSpine() {
    val top = SpineInsetVertical.toPx()
    val height = size.height - 2 * top
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(SpineEdge, SpineCentre, SpineEdge),
            startY = top,
            endY = top + height,
        ),
        topLeft = Offset(SpineInsetStart.toPx(), top),
        size = Size(SpineWidth.toPx(), height),
    )
}

/**
 * `.band{top:33%;height:14%;opacity:.9;mix-blend-mode:multiply}` — the one printed ink, bleeding the
 * full width of the sheet.
 *
 * `multiply` is what makes it read as ink *in* the paper: over the stock it darkens, and over the grain
 * already blended into that stock it darkens unevenly, which is the whole point. A normal-blended band
 * at 90% would sit on top of the paper like a sticker.
 *
 * **Draws nothing below API 29** (**D-018**, owner-ruled 2026-07-30 on D-014's precedent): `multiply`
 * does not exist there and fails silently to `src-over`, so an earlier draft laid the band as an
 * un-multiplied 90% rectangle — a sticker where the design asks for ink. The ruling is that where the
 * platform cannot express the frozen design the implementation **omits and discloses**: no emulation, no
 * substitute blend mode, no approximation. A cover on those devices is stock, crease, fore-edge, stamp
 * and title, with no printed band. Known Limitation, alongside the flat paper of D-014 — which is the
 * same platform ceiling on the same devices, so the two are one sentence in the release notes.
 *
 * Asked of Compose rather than of `Build.VERSION`, for [ZinelyV2Grain.IsSupported]'s own reason: this is
 * the predicate the compositing path branches on, so the guard cannot drift from the thing it guards.
 */
private fun DrawScope.drawInkBand(band: Color) {
    if (!BlendMode.Multiply.isSupported()) return
    drawRect(
        color = band,
        topLeft = Offset(0f, BandTopFraction * size.height),
        size = Size(size.width, BandHeightFraction * size.height),
        alpha = BandAlpha,
        blendMode = BlendMode.Multiply,
    )
}

/**
 * `.cover::after{right:0;top:5px;bottom:5px;width:3px;background:linear-gradient(90deg,…)}` — the
 * fore-edge: the shadow of the stacked leaves at the open side of the little book.
 */
private fun DrawScope.drawForeEdge() {
    val top = ForeEdgeInsetVertical.toPx()
    val height = size.height - 2 * top
    val width = ForeEdgeWidth.toPx()
    val left = size.width - width
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(ForeEdgeDark, ForeEdgeClear),
            startX = left,
            endX = left + width,
        ),
        topLeft = Offset(left, top),
        size = Size(width, height),
    )
}

/** The shape's boundary as a path, so a stroke can follow the asymmetric radius. */
private fun Shape.outlinePath(scope: DrawScope): Path =
    Path().apply {
        when (val outline = createOutline(scope.size, scope.layoutDirection, scope)) {
            is Outline.Rectangle -> addRect(outline.rect)
            is Outline.Rounded -> addRoundRect(outline.roundRect)
            is Outline.Generic -> addPath(outline.path)
        }
    }

// ---------------------------------------------------------------------------------------------
// The frozen values, transcribed from `v2-library.html` at the lines named against each.
//
// These are per-component literals on purpose. V2 publishes no spacing scale — the D-007 owner ruling
// found the frozen CSS only 16.7% on the 8pt grid and ruled that §III's rhythm is an aspiration rather
// than a token inventory, so spacing "stays per-component as frozen" (ADR-074). A `CoverSpacing` object
// here would be inventing the scale that ruling declined to publish.
// ---------------------------------------------------------------------------------------------

/** `.cover{aspect-ratio:3/4}`. */
private const val CoverAspectRatio = 3f / 4f

/**
 * `.cover{border-radius:6px 9px 9px 6px}` — a bound edge on the left, cut edges on the right.
 *
 * **Absolute, not `RoundedCornerShape`.** Compose's start/end corners mirror under RTL while
 * `Brush`-drawn geometry does not, so a logical shape would have moved the tight bound-edge radius to the
 * right while the crease (`left:9px`) and the fore-edge (`right:0`) stayed where the CSS puts them — a
 * cover creased down its cut edge. **D-019**, ruled 2026-07-30: *"the printed object is physical … do not
 * mirror the printed cover based on locale. The physical binding edge, fore-edge and crease remain
 * canonical exactly as frozen. Future UI chrome may adapt to RTL, but the printed artifact itself does
 * not."* So none of this object mirrors, in any locale, and the guard is
 * `ZineCoverRenderTest.a mirrored layout does not mirror the printed object` — verified to fail on the
 * logical shape. The title still follows the layout direction, exactly as it does in CSS.
 */
private val CoverShape: Shape = AbsoluteRoundedCornerShape(
    topLeft = 6.dp,
    topRight = 9.dp,
    bottomRight = 9.dp,
    bottomLeft = 6.dp,
)

/** `.cover{padding:15px 15px 18px}`. The top padding is unused: the content column is bottom-aligned. */
private val CoverPaddingHorizontal = 15.dp
private val CoverPaddingBottom = 18.dp

/** `.cover{background-size:140px 140px}` — the grain tile at its authored size, unscaled. */
private val CoverGrainTile = 140.dp

/** Effective grain strength on a Library cover: no baked alpha, no CSS opacity. See D-013. */
private const val CoverGrainAlpha = 1.0f

/** `inset 0 0 0 1px rgba(0,0,0,.05)`. */
private val CoverHairlineWidth = 1.dp
private val CoverHairline = Color.Black.copy(alpha = 0.05f)

/** `.cover::before` — `left:9px; top/bottom:6px; width:1px`, white at .02 → .24 → .02. */
private val SpineInsetStart = 9.dp
private val SpineInsetVertical = 6.dp
private val SpineWidth = 1.dp
private val SpineEdge = Color.White.copy(alpha = 0.02f)
private val SpineCentre = Color.White.copy(alpha = 0.24f)

/** `.band{top:33%;height:14%;opacity:.9}`. */
private const val BandTopFraction = 0.33f
private const val BandHeightFraction = 0.14f
private const val BandAlpha = 0.9f

/** `.cover::after` — `right:0; top/bottom:5px; width:3px`, black at .12 → 0. */
private val ForeEdgeInsetVertical = 5.dp
private val ForeEdgeWidth = 3.dp
private val ForeEdgeDark = Color.Black.copy(alpha = 0.12f)
private val ForeEdgeClear = Color.Black.copy(alpha = 0f)

/** `.stamp{top:12px;right:13px;width:22px;height:22px;opacity:.8;transform:rotate(-8deg)}`. */
private val StampSize = 22.dp
private val StampInsetTop = 12.dp
private val StampInsetEnd = 13.dp
private const val StampAlpha = 0.8f
private const val StampRotationDegrees = -8f

/** `.ct` — `1.16rem` = 18.56px, `line-height:1.2`, `letter-spacing:-.01em`, `max-width:9ch`. */
private val CoverTitleSize = 18.56.sp
private val CoverTitleLineHeight = 1.2.em
private val CoverTitleTracking = (-0.01).em
private const val CoverTitleMaxCharacters = 9

/** `-webkit-line-clamp:2` with `overflow:hidden`. */
private const val CoverTitleMaxLines = 2

/** `.ct{padding-bottom:1px}` — inside the cover's own 18px, so the text sits 19px off the edge. */
private val CoverTitlePaddingBottom = 1.dp

/** `transition:box-shadow .16s`. */
private const val CoverPressDurationMillis = 160
