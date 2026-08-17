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

    /**
     * The frozen six with **one long title** — the raster that shows [ADR-100 §3]'s two-line cap holding.
     *
     * `.name` carries no `max-lines` and no ellipsis in the frozen file, and the Compose caption column
     * is additionally narrowed by the overflow mark's reserved 34dp, so an uncapped long name wrapped to
     * three lines and grew its whole grid row — a shelf where one cover's caption pushes its neighbour's
     * cover **down**. **Every title in [FROZEN] is 14 characters or shorter**, so no parity raster had
     * ever shown this, which is why the review could raise it only as untested.
     *
     * It is no longer open: ADR-100 §3 caps `.name` at two lines with an end ellipsis, and this fixture
     * is now the picture of that cap working — the long tile's cover sits level with its neighbour's.
     * Kept rather than retired, because a raster showing the cap hold is what fails visibly if the cap
     * is removed. The behavioural assertion lives in `ZineOnShelfTest`; this is its parity counterpart.
     */
    val LONG_TITLE: List<ZineShelfItem> = FROZEN.mapIndexed { index, item ->
        if (index == 0) item.copy(title = "Notes from the Sunday market, volume three") else item
    }
}
