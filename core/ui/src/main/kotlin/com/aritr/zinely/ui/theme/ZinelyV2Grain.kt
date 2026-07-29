package com.aritr.zinely.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.isSupported
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.R

/**
 * The **paper grain** — the V2 material, not a decorative overlay.
 *
 * [V2-CONSTITUTION.md](docs/design/V2-CONSTITUTION.md) §II makes warmth a property of *materials*
 * rather than of decoration, and this is where that becomes literal: every paper-metaphor surface in
 * the frozen trilogy — the covers, the editor page, the proof page, the drawer, the desk itself —
 * carries the same noise, soft-light blended, at its own scale and strength.
 *
 * ### What the frozen files declare
 *
 * One noise source, in `--grain`, identical in construction across all three files:
 *
 * ```
 * <feTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='2' stitchTiles='stitch'/>
 * <feColorMatrix type='saturate' values='0'/>
 * ```
 *
 * on a 140×140 tile — high-frequency fractal noise, desaturated to grey. Ten rules draw it.
 *
 * ### Why this ships as a bitmap
 *
 * `feTurbulence` has no Compose equivalent. The nearest thing is `RuntimeShader`, which is **API 33**
 * against a `minSdk` of **24**, so a runtime noise generator would be unavailable to most of the
 * supported range. The tile is therefore **pre-baked from the SVG 1.1 specification's normative
 * turbulence reference code** at exactly the frozen parameters (`fractalNoise`, `baseFrequency` 0.9,
 * `numOctaves` 2, `stitchTiles` stitch, default `seed` 0, 140×140, then `saturate 0`), and shipped as
 * `res/drawable-nodpi/zinely_v2_grain.png`. `nodpi` is deliberate: the tile must be drawn at a **dp**
 * size the caller chooses, never rescaled by screen density behind our back.
 *
 * Two encoding decisions in the asset are worth knowing before anyone regenerates it:
 *
 * - **The tile is sRGB-encoded linear turbulence, so its mean is ~187, not 128.**
 *   `color-interpolation-filters` has an initial value of **linearRGB** (SVG 1.1 §11.7.1) and none of
 *   the three frozen files overrides it, so the filter chain runs in linear light and its result is
 *   encoded to sRGB on the way to the display. Linear 0.5 is sRGB 0.735.
 *
 *   The consequence is not that the tile would be "too dark" — it is that it would be a **different
 *   material**. Soft-light pivots on 0.5: a source below it darkens, above it lightens, and exactly at
 *   it does nothing. A tile centred on 128 is therefore *neutral on average* — measured against the
 *   `#F7F2E7` paper it moves it by 0.0000 — while the correct tile sits above the pivot and **lightens**
 *   (+0.010). Grain that textures without lifting the paper is not a fainter version of grain that
 *   lifts it.
 * - **RGBA, not the smaller 8-bit grey+alpha.** A greyscale PNG carries no colour space and decoders
 *   disagree about what its grey means; Java's `ImageIO` reads one through *linear* grey. R=G=B in
 *   sRGB costs two bytes a pixel and removes the question. (`saturate` acts on RGB only, so the
 *   turbulence's own fourth channel survives as per-pixel alpha and the tile is not flat.)
 *
 * The generator is a transcription of the published reference code, not a reimplementation of any
 * browser, and the distinction matters: **it is not claimed to be pixel-identical to Chrome's
 * `feTurbulence`.** Whether the two agree closely enough is a pixel-parity question for **A9** and
 * Phase B, recorded as open rather than assumed.
 *
 * The generator is committed at [`tools/grain/gen_grain.py`](tools/grain/gen_grain.py) — a binary asset
 * whose provenance lives only in prose is a binary asset nobody can audit — and it self-checks on every
 * run.
 *
 * `stitchTiles='stitch'` is verified **there**, and only there. What it buys is that the turbulence
 * *function* is periodic across the tile boundary: measured in the function's own [-1, 1] units, the
 * seam is **1.8e-6** with stitching on against **0.38** with it off. That is not a pixel measurement,
 * and the distinction is the whole point — at `baseFrequency` 0.9 the lattice cell is ~1.1px, adjacent
 * pixels are effectively uncorrelated, and **no statistic computed on the PNG separates a stitched tile
 * from an unstitched one.** Measured, the stitched tile's wrap edge scores slightly *worse* (1.355 ×
 * interior) than the unstitched one's (1.227). So stitching is implemented because the frozen source
 * asks for it and it is what the spec means — not because it earns its keep at this frequency, and not
 * because any test here could tell.
 *
 * ### Scale and strength are per-surface, and stay at the call site
 *
 * The frozen files use six tile sizes (70, 90, 120, 140, 150, 180px) and five strengths, chosen per
 * surface with no ladder — so, under the ruling closing **D-007**, none of them is tokenised here.
 * They are transcribed at their components from Phase B onward.
 *
 * Two numbers multiply into what a surface actually shows: the alpha baked into the `--grain` SVG,
 * and the CSS `opacity` on the element drawing it. **The two `--grain` definitions do not agree** —
 * the Bench and Proof bake `opacity='.5'` into the SVG's `<rect>`, the Library bakes none — so the
 * effective column is the only fair comparison:
 *
 * | Surface | Tile | Baked | CSS | **Effective** |
 * |---|---|---|---|---|
 * | Library `.cover` | 140px | 1.0 | — | **1.00** |
 * | Library `.sheet-ill` | 90px | 1.0 | — | **1.00** |
 * | Library `.book-ill` | 70px | 1.0 | — | **1.00** |
 * | Bench/Proof `body::before` (the desk) | 180px | .5 | .5 | **0.25** |
 * | Bench `.page::after` | 120px | .5 | .45 | **0.225** |
 * | Proof `.zpage::after` | 120px | .5 | .42 | **0.21** |
 * | Proof `.drawer::after` | 150px | .5 | .3 | **0.15** |
 * | Bench/Proof `.phone::after` | 150px | .5 | .35 | 0.175 — *prototype bezel, not product UI* |
 *
 * So the Library asks for grain **four to nearly seven times** stronger than any Bench or Proof
 * surface (1.00 against 0.25 at the top of that range and 0.15 at the bottom). That is **not**
 * self-evidently a defect — the Library blends over saturated cover inks, where soft-light is far
 * subtler than over near-white paper — but it is a real question Phase B must answer to draw a cover,
 * and it is logged as **D-013** rather than averaged away.
 */
public object ZinelyV2Grain {

    /** The tile's own size. Every frozen `background-size` is a rescaling of this one 140×140 source. */
    public val SourceTileSize: Dp = 140.dp

    /**
     * `soft-light`, in every one of the ten frozen grain-drawing rules — no surface blends its grain
     * any other way.
     *
     * V2 does blend one other thing, and it is worth naming so a future reader does not mistake it
     * for a second material: `.band` at `v2-library.html:67` — the cover's colour band — blends
     * `multiply`. That belongs to a cover component in Phase B, not to the paper.
     *
     * Compose's [BlendMode.Softlight] implements the same W3C formula CSS does. Note the one real
     * difference from the prototypes: CSS distinguishes `background-blend-mode` (the Library, blending
     * the noise with the element's *own* background) from `mix-blend-mode` on an `::after` overlay
     * (the Bench and Proof, blending with whatever has been painted beneath). Compose has no such
     * split — [zinelyV2Grain] blends with the content already drawn in the current layer, which
     * matches the `mix-blend-mode` reading and, for every frozen use, the `background-blend-mode` one
     * too, since those elements paint their own background first.
     */
    public val Blend: BlendMode = BlendMode.Softlight

    /**
     * Whether this device can blend soft-light at all — **API 29**, against a `minSdk` of 24.
     *
     * This is the third API ceiling V2 has run into, after `RuntimeShader` (33) and
     * `fontVariationSettings` (26), and it is the one with teeth. `android.graphics.BlendMode`
     * arrived in Q; below it Compose composites through `PorterDuffXfermode`, whose mode table has
     * no soft-light, so `BlendMode.Softlight` falls through to **`SRC_OVER`**. That is not a subtler
     * grain — it is the noise tile painted *opaquely* over the surface, and at the Library's
     * effective strength of 1.00 that is a flat grey rectangle where a cover should be.
     *
     * So on API 24–28 [zinelyV2Grain] draws **nothing** and the surface stays flat. Losing the paper
     * texture is a smaller, more honest failure than replacing the artwork with grey, and it is
     * reversible: this is a platform floor, not a design decision. What the design should actually do
     * on those devices is owner-owed and logged as **D-014**.
     *
     * Asked of Compose rather than of `Build.VERSION` on purpose: `isSupported` is the same predicate
     * the compositing path itself branches on, so this cannot drift out of agreement with the thing it
     * is guarding — which a hand-written `SDK_INT >= 29` silently could.
     */
    public val IsSupported: Boolean
        get() = Blend.isSupported()
}

/**
 * A repeating, density-correct brush over the grain tile.
 *
 * @param tileSize the surface's own frozen `background-size`, in dp.
 */
@Composable
public fun rememberZinelyV2GrainBrush(tileSize: Dp = ZinelyV2Grain.SourceTileSize): ShaderBrush {
    val image = ImageBitmap.imageResource(R.drawable.zinely_v2_grain)
    val tilePx = with(LocalDensity.current) { tileSize.toPx() }
    return remember(image, tilePx) {
        val shader = ImageShader(image, TileMode.Repeated, TileMode.Repeated)
        // The tile is nodpi, so it arrives at its authored 140px. Scale it to the dp size the
        // surface asks for; without this the grain would be finer on a dense screen than on a
        // sparse one, which is the one thing a *material* must never do — it would read as a
        // different paper stock per device.
        val scale = tilePx / image.width
        shader.setLocalMatrix(android.graphics.Matrix().apply { setScale(scale, scale) })
        ShaderBrush(shader)
    }
}

/**
 * Draw the paper grain over this surface's own content, soft-light blended — the V2 paper material.
 *
 * Takes the brush rather than building one, so this stays an ordinary `Modifier` factory: a
 * `@Composable` modifier cannot be hoisted, keyed, or reused, and there is already a `remember`
 * seam above for the part that genuinely needs composition. Call as
 * `Modifier.zinelyV2Grain(rememberZinelyV2GrainBrush(120.dp), alpha = 0.225f)`.
 *
 * A no-op below API 29 — see [ZinelyV2Grain.IsSupported], which explains why drawing nothing beats
 * drawing the fallback.
 *
 * @param alpha the **effective** strength — the SVG's baked alpha × the element's CSS `opacity`, per
 *   the table on [ZinelyV2Grain]. Per-surface by the D-007 ruling, so it stays at the call site.
 */
public fun Modifier.zinelyV2Grain(brush: ShaderBrush, alpha: Float): Modifier =
    if (!ZinelyV2Grain.IsSupported) {
        this
    } else {
        drawWithContent {
            drawContent()
            drawRect(brush = brush, alpha = alpha, blendMode = ZinelyV2Grain.Blend, style = Fill)
        }
    }
