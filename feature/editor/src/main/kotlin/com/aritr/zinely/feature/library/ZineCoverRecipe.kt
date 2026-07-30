package com.aritr.zinely.feature.library

import androidx.compose.ui.graphics.Color
import com.aritr.zinely.ui.theme.ZinelyContentInks
import com.aritr.zinely.ui.theme.ZinelyCoverInkId
import com.aritr.zinely.ui.theme.ZinelyV2Icon
import com.aritr.zinely.ui.theme.ZinelyV2Icons

/**
 * The six cover surfaces the frozen Library declares — four flooded inks and the paper stock twice,
 * once banded matcha and once banded strawberry (`v2-library.html:79-84`).
 *
 * Declaration order is the frozen CSS order — kept because that is the order a reader comparing this to
 * `v2-library.html` expects, not because anything indexes into it (nothing does, since D-017 was ruled).
 *
 * The two paper entries are *one* stock. `.paper-c` and `.paper-s` share `background-color:#F1EBDA`
 * and differ only in their band, which is why [ZinelyCoverStock] carries no band of its own — the
 * pairing is a recipe decision, exactly as that type's KDoc says.
 */
internal enum class ZineCoverSurface {
    MatchaInk,
    TealInk,
    StrawberryInk,
    OchreInk,
    PaperMatchaBand,
    PaperStrawberryBand,
}

/**
 * The six stamps the frozen shelf prints on its covers, in the order the frozen markup lays them out
 * (`v2-library.html:149-154`): sun, envelope, waves, sprig, star, face.
 *
 * These are the seven "artwork marks" A7 ported as geometry — each carries `stroke-width:1.6` in its
 * own markup, so unlike most V2 icons they need no paint from the call site
 * ([ZinelyV2Icon.frozenPaint]).
 */
internal enum class ZineCoverStamp { Sun, Letter, Waves, Sprig, Star, Face }

/**
 * One zine's cover: which surface it is printed on and which mark it is stamped with.
 *
 * This is the frozen **Maker's Cover** reduced to what the frozen Library actually draws —
 * `title + ink + stamp` ([V2-IDENTITY.md](docs/design/V2-IDENTITY.md) §5). The fuller recipe grammar
 * that document states (`× paper × motif × layout zone`) is labelled *Direction* there, not frozen, so
 * it is deliberately absent: modelling it now would be implementing a proposal.
 */
internal data class ZineCoverRecipe(
    val surface: ZineCoverSurface,
    val stamp: ZineCoverStamp,
)

// -----------------------------------------------------------------------------------------
// Assignment — deliberately NOT here. See D-017 and the note below.
// -----------------------------------------------------------------------------------------
//
// The frozen Library hard-codes six example covers and states no rule for assigning one to a real
// project ([D-017](docs/design/V2-SPEC-DEFECTS.md)). B1's first draft derived the cover from a hash of
// the title; the owner ruled that out on 2026-07-30:
//
// > "Do not derive the cover surface from the title. Assign the cover surface once when the zine is
// > created and persist that assignment. A physical object should retain its identity across renames.
// > Do not use round-robin assignment. Do not infer from neighbouring zines. The persisted assignment
// > becomes part of the zine's identity."
//
// A second draft shipped an assigner here — `newZineCoverRecipe(random: Random)`, drawing surface and
// stamp independently — guarded by a reflection test that scanned this package for any function mapping
// a String to a cover. Independent review found the guard could not hold the ruling it claimed to: it
// checked for an exact `String` parameter type, so `newZineCoverRecipe(Random(title.hashCode()))` at a
// call site — a title-derived seed rather than a title-typed parameter — passes through it in full view.
// That is not a fixable gap in one test; it is what a syntactic filter can never rule out, because the
// ruling is about **information flow** (must the title reach the cover, ever, by any path), which no
// finite set of signature checks decides.
//
// The correct enforcement is that there is no path for the title to take, because the assigner has no
// caller yet: **B1 has nothing to assign a cover to**. Persistence is the same point ADR-042's project
// index and `meta.json` sidecar make: an assigner with nowhere to store its result "assigns" a cover that
// evaporates on the next recomposition, which is a different bug than a title leaking in, but a bug all
// the same. So the assigner is deleted here and lands in **B5**, next to the persisted surface+stamp field
// it is otherwise unable to keep — one addition, one caller, one guard that means something because it
// can finally see the whole path from a create action to a stored value.
//
// What stays true regardless of where the assigner lands: [ZineCoverSurface] and [ZineCoverStamp] vary
// independently — the frozen grid × swappable ingredients [V2-IDENTITY.md](docs/design/V2-IDENTITY.md) §5
// describes — and the mechanism **supersedes** [ADR-069](docs/DECISIONS.md#adr-069)'s title-hash *for V2
// covers only*; ADR-069's load-bearing rule (a cover is a recipe, never a rendered thumbnail) is untouched,
// and V1's shelf keeps its own title hash until C0 retires it.

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
