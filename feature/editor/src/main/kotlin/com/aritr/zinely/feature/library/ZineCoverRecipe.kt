package com.aritr.zinely.feature.library

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import com.aritr.zinely.ui.theme.ZinelyContentInks
import com.aritr.zinely.ui.theme.ZinelyCoverInkId
import com.aritr.zinely.ui.theme.ZinelyV2Icon
import com.aritr.zinely.ui.theme.ZinelyV2Icons
import com.aritr.zinely.ui.theme.ZinelyV21Colors

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

// -----------------------------------------------------------------------------------------
// V2.1 resolves the SAME persisted recipe onto a different palette and a different mark set.
// -----------------------------------------------------------------------------------------
//
// D-017 makes the cover part of the zine's identity: a recipe is assigned once, at creation, and
// stored. So the re-freeze must NOT change `ZineCoverSurface` or `ZineCoverStamp` — every zine already
// on a tester's phone names one of the six V2 surfaces and one of the six V2 stamps, and renaming them
// would reprint objects that exist. What changes is only how those six names are painted, which is
// exactly the split this file was written around: the model says which cover, this file says how.
//
// Both mappings are one-to-one and both are, in part, ARBITRARY. V2.1's ink set is leaf · berry ·
// butter · jam with two paper stocks; V2's is matcha · teal · strawberry · ochre with two. Three pair
// by hue (matcha/leaf, strawberry/berry, ochre/butter) and teal has no counterpart at all, so it takes
// the remaining ink. Naming that here rather than implying a correspondence that does not exist.

/** The V2.1 cover stock this surface prints on — `.ink-leaf`, `.paper-s`, `.ink-berry`, `.paper-c`, … */
internal fun ZineCoverSurface.v21Fill(colors: ZinelyV21Colors): Color = when (this) {
    ZineCoverSurface.MatchaInk -> colors.leaf
    ZineCoverSurface.StrawberryInk -> colors.berry
    ZineCoverSurface.OchreInk -> colors.butter
    // No V2.1 ink is teal. Jam is what is left, and it is the only ink not already spoken for.
    ZineCoverSurface.TealInk -> colors.jam
    ZineCoverSurface.PaperMatchaBand -> colors.butterTint
    ZineCoverSurface.PaperStrawberryBand -> colors.paper
}

/**
 * The mark's colour on that stock — `.cover .mark{color:rgba(255,246,232,.92)}`, overridden to
 * `ink-soft` by `.paper-s`/`.paper-c`.
 *
 * The ink value is hardcoded in the frozen file and **theme-invariant on purpose** (V21-SPEC §4.1): a
 * printed cover is the maker's palette, not the app's chrome, and it does not restyle in the dark.
 */
internal fun ZineCoverSurface.v21MarkInk(colors: ZinelyV21Colors): Color = when (this) {
    ZineCoverSurface.PaperMatchaBand, ZineCoverSurface.PaperStrawberryBand -> colors.inkSoft
    else -> ZineV21MarkOnInk
}

private val ZineV21MarkOnInk = Color(0xEBFFF6E8)

/**
 * The V2.1 glyph this stamp draws — [ZineV21CoverMarks].
 *
 * Arbitrary in the same way the surfaces are: V2's marks name a sun, a letter, waves, a sprig, a star
 * and a face; V2.1's name a booklet, an envelope, two rings, a sprig, three ruled lines and a mug. Two
 * pair (letter/envelope, sprig/sprig); the rest is a stable assignment, not a translation.
 */
internal fun ZineCoverStamp.v21Mark(): ImageVector = when (this) {
    ZineCoverStamp.Letter -> ZineV21CoverMarks.Envelope
    ZineCoverStamp.Sprig -> ZineV21CoverMarks.Sprig
    ZineCoverStamp.Sun -> ZineV21CoverMarks.Rings
    ZineCoverStamp.Star -> ZineV21CoverMarks.Booklet
    ZineCoverStamp.Waves -> ZineV21CoverMarks.Lines
    ZineCoverStamp.Face -> ZineV21CoverMarks.Mug
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
