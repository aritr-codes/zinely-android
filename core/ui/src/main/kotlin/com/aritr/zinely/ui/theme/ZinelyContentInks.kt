package com.aritr.zinely.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The **`content.*` namespace** — the maker's inks. These live on the *artifact*, never in the
 * interface.
 *
 * This is the load-bearing split of the whole V2 identity
 * ([V2-CONSTITUTION.md](docs/design/V2-CONSTITUTION.md) §II.3): chrome stays calm and restrained so
 * that the user's zines can be loud. [ZinelyV2Colors] is the interface; this is the art supplies.
 * **No chrome component may read across into this namespace, and no `content.*` value may be used to
 * paint the interface** ([V2-BENCH-REVIEW.md](docs/design/V2-BENCH-REVIEW.md) §8: *"Two separate
 * namespaces, enforced… A lint rule keeps them apart."*). They are separate Kotlin types precisely so
 * that reading across is a compile error rather than a code-review question.
 *
 * ### Two distinct sets, not one — and this is not a contradiction in the specs
 *
 * A reader comparing [V2-TOKENS.md](docs/design/V2-TOKENS.md) (which lists **four** cover inks) with
 * [V2-CONSTITUTION.md](docs/design/V2-CONSTITUTION.md) §III (which names *"the 10-ink Bench H4 set"*)
 * will think the corpus disagrees with itself. It does not — the two govern **different axes**, and
 * the reconciliation is explicit in the frozen record:
 *
 *  - [V2-BENCH-REVIEW.md](docs/design/V2-BENCH-REVIEW.md) §8 — *"The 4 cover inks stay for cover
 *    identity ([ADR-069](docs/DECISIONS.md#adr-069)); the in-page maker set is distinct and larger."*
 *  - [V2-BENCH-IA-INTERACTION.md](docs/design/V2-BENCH-IA-INTERACTION.md) — *"The 4 cover inks
 *    (ADR-069) remain for cover identity; this in-page set is distinct."*
 *
 * So: [coverInks] answers *"which zine is mine?"* on the shelf, and [makerInks] is what the maker
 * draws **with** inside a page. `teal #47857B` is a cover ink and is deliberately **not** in the
 * maker set; `Aqua #57B0A9` is a maker ink and is deliberately **not** a cover ink.
 *
 * ### These are theme-invariant, and that is a design truth rather than an oversight
 *
 * The frozen Library declares its cover inks as plain CSS classes (`v2-library.html:79-84`) sitting
 * **outside** every `:root[data-theme]` block and outside the `prefers-color-scheme` block; the Bench's
 * maker set is a flat array (`v2-bench.html:391`) with no theme variant. Nothing here re-derives for
 * dark, so nothing here takes a `darkTheme` parameter.
 *
 * That is correct and worth stating plainly, because "we forgot the dark values" and "there are no dark
 * values" look identical in code: **a printed object does not change colour when the room lights dim.**
 * A zine whose cover restyled itself in dark mode would be the app lying about a physical artifact —
 * the exact dishonesty [V2-CONSTITUTION.md](docs/design/V2-CONSTITUTION.md) §II.6 forbids. Chrome
 * re-derives because chrome is *the room*; content does not because content is *the thing on the desk*.
 *
 * ### Why there are no human names here
 *
 * The frozen Bench carries display names alongside the values (`['Matcha','#7C8A3F']`, …). Those names
 * are **user-facing copy** and belong to `:core:copy` ([ADR-060](docs/DECISIONS.md#adr-060)), not to a
 * colour token. This file therefore models identity as an enum ([ZinelyCoverInkId],
 * [ZinelyMakerInkId]) and leaves the labels to the copy layer, which is where the ink popover will read
 * them from when Phase C builds it.
 */
@Immutable
public data class ZinelyContentInks(
    /**
     * The **four cover inks** ([ADR-069](docs/DECISIONS.md#adr-069)) — cover identity, in the frozen
     * order the shelf presents them (`v2-library.html:79-82`).
     */
    val coverInks: List<ZinelyCoverInk>,
    /** The un-inked **paper cover stock** — the alternative to an ink cover (`v2-library.html:83-84`). */
    val coverStock: ZinelyCoverStock,
    /**
     * The **ten named riso spot inks** — the `INKS` band, in the frozen order the ink popover
     * presents them (`v2-bench.html:391`). This is what the maker draws *with*.
     *
     * **This is one band of three, and is NOT the complete maker palette.** The frozen ink popover
     * renders `bandHTML('Inks',INKS) + bandHTML('Paper tints',TINTS) + bandHTML('Neutrals',NEUT)`
     * (`v2-bench.html:460`) — nineteen swatches, and `applyInk` (`:463-470`) applies **any** of them
     * as an ink. Only the `INKS` band is modelled here, because the corpus does not agree on whether
     * the other two belong to `content.*`: [V2-CONSTITUTION.md](docs/design/V2-CONSTITUTION.md) §III
     * and [V2-IDENTITY.md](docs/design/V2-IDENTITY.md) §4 both say the maker set *is* these ten
     * ("verbatim, no invention"), while the frozen Bench offers nineteen. That contradiction is
     * **D-003** in [V2-SPEC-DEFECTS.md](docs/design/V2-SPEC-DEFECTS.md) and is owner-owed.
     *
     * **Do not read this field as "the maker's supplies" when building the Phase C ink popover** —
     * read D-003 first. Porting the other two bands unilaterally would contradict the Constitution;
     * treating ten as complete would contradict the frozen prototype. Neither is implementation's
     * call to make.
     */
    val makerInks: List<ZinelyMakerInk>,
) {
    /** Look up a cover ink by identity. Phase B resolves one of these per cover render. */
    public operator fun get(id: ZinelyCoverInkId): ZinelyCoverInk = coverInks.single { it.id == id }

    /** Look up a maker ink by identity. */
    public operator fun get(id: ZinelyMakerInkId): ZinelyMakerInk = makerInks.single { it.id == id }
}

/** Stable identity for a cover ink. Human labels live in `:core:copy` — see [ZinelyContentInks]. */
public enum class ZinelyCoverInkId { Matcha, Teal, Strawberry, Ochre }

/** Stable identity for an in-page maker ink. Human labels live in `:core:copy`. */
public enum class ZinelyMakerInkId {
    Matcha, Forest, Strawberry, Brick, Sunflower, Ochre, Aqua, Cornflower, Plum, Ink,
}

/**
 * A cover ink is a **frozen triple**, not a single colour: the sheet's [fill], the title colour that
 * rides on it ([onFill]), and the [band] — the darker cut of the same ink used for the cover's printed
 * band. A fill on its own is unusable, so the three travel together and a caller cannot pick one and
 * invent the others.
 */
@Immutable
public data class ZinelyCoverInk(
    val id: ZinelyCoverInkId,
    /** The cover's printed ink field. */
    val fill: Color,
    /** The cover title, riding on [fill]. */
    val onFill: Color,
    /** The band across the cover — a deeper cut of the same ink. */
    val band: Color,
)

/**
 * The paper cover — an uninked stock rather than a flooded ink field. Its band is supplied by a
 * *cover ink* at composition time (the frozen Library pairs this stock with matcha and with
 * strawberry), so the pairing is a **recipe decision and belongs to Phase B**, not a token here.
 */
@Immutable
public data class ZinelyCoverStock(
    val fill: Color,
    val onFill: Color,
)

/** An in-page maker ink: one colour the maker draws with. */
@Immutable
public data class ZinelyMakerInk(
    val id: ZinelyMakerInkId,
    val value: Color,
)

/**
 * The frozen `content.*` set. Takes **no theme parameter** by design — see [ZinelyContentInks].
 *
 * Sources: `v2-library.html:79-84` (cover inks + stock), `v2-bench.html:391` (the maker set).
 */
public fun zinelyContentInks(): ZinelyContentInks = ZinelyContentInks(
    coverInks = listOf(
        // .ink-matcha{background-color:#7C8A3F;color:#F7F2E7} .ink-matcha .band{background:#4E5A26}
        ZinelyCoverInk(ZinelyCoverInkId.Matcha, Color(0xFF7C8A3F), Color(0xFFF7F2E7), Color(0xFF4E5A26)),
        // .ink-teal{background-color:#47857B;color:#F7F2E7} .ink-teal .band{background:#2E574E}
        ZinelyCoverInk(ZinelyCoverInkId.Teal, Color(0xFF47857B), Color(0xFFF7F2E7), Color(0xFF2E574E)),
        // .ink-straw{background-color:#E27F89;color:#4A211F} .ink-straw .band{background:#C05863}
        ZinelyCoverInk(ZinelyCoverInkId.Strawberry, Color(0xFFE27F89), Color(0xFF4A211F), Color(0xFFC05863)),
        // .ink-ochre{background-color:#D19A3C;color:#3A2A0E} .ink-ochre .band{background:#A9741F}
        ZinelyCoverInk(ZinelyCoverInkId.Ochre, Color(0xFFD19A3C), Color(0xFF3A2A0E), Color(0xFFA9741F)),
    ),
    // .paper-c / .paper-s {background-color:#F1EBDA;color:#2A251E}
    coverStock = ZinelyCoverStock(fill = Color(0xFFF1EBDA), onFill = Color(0xFF2A251E)),
    makerInks = listOf(
        ZinelyMakerInk(ZinelyMakerInkId.Matcha, Color(0xFF7C8A3F)),
        ZinelyMakerInk(ZinelyMakerInkId.Forest, Color(0xFF3E5E3A)),
        ZinelyMakerInk(ZinelyMakerInkId.Strawberry, Color(0xFFE27F89)),
        ZinelyMakerInk(ZinelyMakerInkId.Brick, Color(0xFFB0503F)),
        ZinelyMakerInk(ZinelyMakerInkId.Sunflower, Color(0xFFE7B53C)),
        ZinelyMakerInk(ZinelyMakerInkId.Ochre, Color(0xFFD19A3C)),
        ZinelyMakerInk(ZinelyMakerInkId.Aqua, Color(0xFF57B0A9)),
        ZinelyMakerInk(ZinelyMakerInkId.Cornflower, Color(0xFF6E86C9)),
        ZinelyMakerInk(ZinelyMakerInkId.Plum, Color(0xFF8A5A9B)),
        ZinelyMakerInk(ZinelyMakerInkId.Ink, Color(0xFF2A251E)),
    ),
)
