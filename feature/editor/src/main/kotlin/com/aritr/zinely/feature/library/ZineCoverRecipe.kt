package com.aritr.zinely.feature.library

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
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

/**
 * The V2.1 cover stock this surface prints on — `.ink-leaf`, `.paper-s`, `.ink-berry`, `.paper-c`, …
 *
 * ### The two paper stocks do not theme, and that is an amendment to the freeze — ADR-100
 *
 * The frozen file writes `.paper-s .fill{background:var(--paper)}` and
 * `.paper-c .fill{background:var(--butter-tint)}`, and the dark block redefines both tokens. Taken
 * literally that is what shipped, and the parity raster shows what it costs: at **1.18:1** and
 * **1.33:1** against `--desk`, with a hard shadow at **1.17:1**, the two paper covers stop being
 * objects resting on a desk and become outlined holes cut into it — beside four ink covers that keep
 * both their fill and their shadow. On the one screen whose whole job is *"which zine do I want?"*, two
 * of six zines have no face at night.
 *
 * So the stocks are pinned to their light values, which is **already the rule for everything else
 * printed on a cover**: `.cover .mark`'s hardcoded `rgba(255,246,232,.92)` is exempted by name in
 * V21-SPEC §4.1 because *cover art is the maker's palette, not the app's chrome, and does not restyle
 * in the dark*. A stock is the most literal cover art there is — it is the paper. Extending the
 * exemption from the ink on the paper to the paper itself is the smaller claim, not the larger one.
 *
 * Measured after: **15.39:1** and **14.02:1** against the dark desk, marks at 6.16 and 5.61 on them.
 * **Light is byte-identical** — these are the light tokens' own values, so the amendment can only be
 * seen at night, which is the only place the defect was.
 *
 * All six maker-stock fills are pinned to the established cover recipe. They are saved visual identity,
 * not app chrome, so changing the room theme or future studio palette must not repaint an existing zine.
 * The four ink stocks remain saturated objects against both rooms; the two paper stocks remain lit.
 */
internal fun ZineCoverSurface.v21Fill(colors: ZinelyV21Colors): Color = when (this) {
    ZineCoverSurface.MatchaInk -> ZineV21CoverLeaf
    ZineCoverSurface.StrawberryInk -> ZineV21CoverBerry
    ZineCoverSurface.OchreInk -> ZineV21CoverButter
    ZineCoverSurface.TealInk -> ZineV21CoverJam
    ZineCoverSurface.PaperMatchaBand -> ZineV21StockCream
    ZineCoverSurface.PaperStrawberryBand -> ZineV21StockPaper
}

/**
 * The mark's colour on that stock — `.cover .mark{color:rgba(255,246,232,.92)}`, overridden to
 * `ink-soft` by `.paper-s`/`.paper-c`.
 *
 * The ink value is hardcoded in the frozen file and **theme-invariant on purpose** (V21-SPEC §4.1): a
 * printed cover is the maker's palette, not the app's chrome, and it does not restyle in the dark. The
 * paper stocks' mark is now invariant for the same reason and by the same amendment ([v21Fill]) — a
 * themed `ink-soft` on an unthemed cream stock would be the defect inverted.
 */
internal fun ZineCoverSurface.v21MarkInk(colors: ZinelyV21Colors): Color = when (this) {
    ZineCoverSurface.PaperMatchaBand, ZineCoverSurface.PaperStrawberryBand -> ZineV21MarkOnStock
    else -> ZineV21MarkOnInk
}

/**
 * `.cover{border:1.5px solid var(--ink)}` — pinned with every authored cover stock.
 *
 * The old dark `--ink` was `#F6EAD6`, which was the cream the paper stocks already were: an outline at **1.01:1**
 * on `.paper-c`'s `#FDEBC4` and **1.11:1** on `.paper-s`'s `#FFF6E8` is no outline. (Both figures read
 * `1.03` here until a review recomputed them; neither was ever measured.)
 * The border is part of the printed object wherever the fill is, so
 * it is pinned with it. The four ink covers are equally physical and keep their authored border.
 */
internal fun ZineCoverSurface.v21BorderInk(colors: ZinelyV21Colors): Color = when (this) {
    ZineCoverSurface.PaperMatchaBand, ZineCoverSurface.PaperStrawberryBand -> ZineV21StockEdge
    else -> colors.ink
}

private val ZineV21MarkOnInk = Color(0xEBFFF6E8)
private val ZineV21CoverLeaf = Color(0xFF4E7A3C)
private val ZineV21CoverBerry = Color(0xFFE4879F)
private val ZineV21CoverButter = Color(0xFFF6B22C)
private val ZineV21CoverJam = Color(0xFFCF4A28)

/** The light `--paper` / `--butter-tint` / `--ink-soft` / `--ink`, pinned. See [v21Fill]. */
private val ZineV21StockPaper = Color(0xFFFFF6E8)
private val ZineV21StockCream = Color(0xFFFDEBC4)
private val ZineV21MarkOnStock = Color(0xFF6E5947)
private val ZineV21StockEdge = Color(0xFF33261C)

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
