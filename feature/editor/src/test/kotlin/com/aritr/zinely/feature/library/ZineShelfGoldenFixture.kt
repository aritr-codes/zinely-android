package com.aritr.zinely.feature.library

import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface

/**
 * The frozen shelf's own six objects, in its own order — `v2-library.html:149-154`.
 *
 * Held in one place because two golden tests now compose the same shelf (B2's, and B3's with the sheet over
 * it) and a second copy would be a second source of design truth: a raster recorded against a drifted copy
 * would still pass its own comparison. Each entry carries its `data-sub` even though the shelf draws none of
 * them — the sheet does, and the pair is the design's argument.
 */
internal object ZineShelfGoldenFixture {
    val FROZEN: List<ZineShelfItem> = listOf(
        ZineShelfItem(
            "Sunday market",
            ZineCoverRecipe(ZineCoverSurface.MatchaInk, ZineCoverStamp.Sun),
            "A4 · 2 days ago",
        ),
        ZineShelfItem(
            "Letters home",
            ZineCoverRecipe(ZineCoverSurface.PaperStrawberryBand, ZineCoverStamp.Letter),
            "Letter · today",
        ),
        ZineShelfItem(
            "Riso tests",
            ZineCoverRecipe(ZineCoverSurface.TealInk, ZineCoverStamp.Waves),
            "A4 · 5 days ago",
        ),
        ZineShelfItem(
            "Mum's garden",
            ZineCoverRecipe(ZineCoverSurface.PaperMatchaBand, ZineCoverStamp.Sprig),
            "A4 · 1 week ago",
        ),
        ZineShelfItem(
            "Tiny poems",
            ZineCoverRecipe(ZineCoverSurface.OchreInk, ZineCoverStamp.Star),
            "Letter · 2 weeks ago",
        ),
        ZineShelfItem(
            "Coffee log",
            ZineCoverRecipe(ZineCoverSurface.StrawberryInk, ZineCoverStamp.Face),
            "A4 · 3 weeks ago",
        ),
    )
}
