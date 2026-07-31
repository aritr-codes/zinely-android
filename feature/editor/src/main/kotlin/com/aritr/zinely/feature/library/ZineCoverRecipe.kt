package com.aritr.zinely.feature.library

import androidx.compose.ui.graphics.Color
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import com.aritr.zinely.ui.theme.ZinelyContentInks
import com.aritr.zinely.ui.theme.ZinelyCoverInkId
import com.aritr.zinely.ui.theme.ZinelyV2Icon
import com.aritr.zinely.ui.theme.ZinelyV2Icons

// -----------------------------------------------------------------------------------------
// The cover TYPES moved to :core:model in B5.  This file keeps only their RENDERING.
// -----------------------------------------------------------------------------------------
//
// `ZineCoverSurface`, `ZineCoverStamp`, `ZineCoverRecipe` and the assigner `newZineCoverRecipe` now live
// in [com.aritr.zinely.core.model.ZineCoverRecipe]. Nothing about them changed — they moved, because
// B5 made them *persisted* data:
//
//   D-017 makes the cover part of the zine's identity, so it must be storable, which means `core:data`
//   (ProjectSummary) and `data-android` (meta.json + the Room index) must be able to name the type and
//   call the assigner at the moment a project is created. `feature:editor` depends on neither, and it
//   must not — a feature module depending on persistence inverts the layering. `core:model` is the only
//   module all three parties can see, and it is pure Kotlin, which is where the assigner belongs anyway.
//
// What stays HERE is everything that needs `core:ui`, which `core:model` must never depend on: which
// colours a surface resolves to, and which glyph a stamp draws. The model says *which* cover; this file
// says *how to paint it*. That split is why the move cost no behaviour.
//
// The two paper surfaces are *one* stock. `.paper-c` and `.paper-s` share `background-color:#F1EBDA`
// and differ only in their band, which is why [ZinelyCoverStock] carries no band of its own — the
// pairing is a recipe decision, exactly as that type's KDoc says.
//
// The stamps are six of the seven "artwork marks" A7 ported as geometry — each carries
// `stroke-width:1.6` in its own markup, so unlike most V2 icons they need no paint from the call site
// ([ZinelyV2Icon.frozenPaint]).
//
// Unchanged by the move: the mechanism **supersedes** [ADR-069](docs/DECISIONS.md#adr-069)'s title-hash
// *for V2 covers only*; ADR-069's load-bearing rule (a cover is a recipe, never a rendered thumbnail) is
// untouched, and V1's shelf keeps its own title hash until C0 retires it.

/** The mark this stamp draws. */
internal fun ZineCoverStamp.icon(): ZinelyV2Icon = when (this) {
    ZineCoverStamp.Sun -> ZinelyV2Icons.StampSun
    ZineCoverStamp.Letter -> ZinelyV2Icons.StampLetter
    ZineCoverStamp.Waves -> ZinelyV2Icons.StampWaves
    ZineCoverStamp.Sprig -> ZinelyV2Icons.StampSprig
    ZineCoverStamp.Star -> ZinelyV2Icons.StampStar
    ZineCoverStamp.Face -> ZinelyV2Icons.StampFace
}

/**
 * The three colours one cover needs, resolved together.
 *
 * A fill without its title colour is unusable and a band without its fill is meaningless, so they
 * travel as a triple — the same reason [com.aritr.zinely.ui.theme.ZinelyCoverInk] is a triple rather
 * than a colour.
 */
internal data class ZineCoverPalette(
    val fill: Color,
    val onFill: Color,
    val band: Color,
)

/**
 * Resolve a surface against the `content.*` namespace. **No colour is written here** — every value
 * comes from [ZinelyContentInks], which is the only place the frozen cover palette exists.
 *
 * The two paper surfaces take their band from a *cover ink's fill* (`.paper-c .band{background:#7C8A3F}`
 * is matcha's fill; `.paper-s .band{background:#E27F89}` is strawberry's), not from that ink's darker
 * `band` cut. That is the frozen CSS, and it is the one place a cover's band is a different value from
 * the band of the ink it names.
 */
internal fun ZineCoverSurface.palette(inks: ZinelyContentInks): ZineCoverPalette = when (this) {
    ZineCoverSurface.MatchaInk -> inks[ZinelyCoverInkId.Matcha].let {
        ZineCoverPalette(it.fill, it.onFill, it.band)
    }

    ZineCoverSurface.TealInk -> inks[ZinelyCoverInkId.Teal].let {
        ZineCoverPalette(it.fill, it.onFill, it.band)
    }

    ZineCoverSurface.StrawberryInk -> inks[ZinelyCoverInkId.Strawberry].let {
        ZineCoverPalette(it.fill, it.onFill, it.band)
    }

    ZineCoverSurface.OchreInk -> inks[ZinelyCoverInkId.Ochre].let {
        ZineCoverPalette(it.fill, it.onFill, it.band)
    }

    ZineCoverSurface.PaperMatchaBand -> ZineCoverPalette(
        fill = inks.coverStock.fill,
        onFill = inks.coverStock.onFill,
        band = inks[ZinelyCoverInkId.Matcha].fill,
    )

    ZineCoverSurface.PaperStrawberryBand -> ZineCoverPalette(
        fill = inks.coverStock.fill,
        onFill = inks.coverStock.onFill,
        band = inks[ZinelyCoverInkId.Strawberry].fill,
    )
}
