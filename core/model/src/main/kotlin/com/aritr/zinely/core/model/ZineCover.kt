package com.aritr.zinely.core.model

import kotlin.random.Random

/**
 * The six cover surfaces the frozen Library declares — four flooded inks and the paper stock twice,
 * once banded matcha and once banded strawberry (`v2-library.html:79-84`).
 *
 * Declaration order is the frozen CSS order, kept because that is the order a reader comparing this
 * to `v2-library.html` expects. **Nothing indexes into it**, and nothing may: assignment is a draw,
 * never a position ([D-017](docs/design/V2-SPEC-DEFECTS.md#d-017-ruling) forbids round-robin).
 */
public enum class ZineCoverSurface {
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
 */
public enum class ZineCoverStamp { Sun, Letter, Waves, Sprig, Star, Face }

/**
 * One zine's cover: which surface it is printed on and which mark it is stamped with.
 *
 * This is the frozen **Maker's Cover** reduced to what the frozen Library actually draws —
 * `title + ink + stamp` ([V2-IDENTITY.md](docs/design/V2-IDENTITY.md) §5). The fuller recipe grammar
 * that document states (`× paper × motif × layout zone`) is labelled *Direction* there, not frozen,
 * so it is deliberately absent: modelling it now would be implementing a proposal.
 *
 * ## Why this lives in `core:model` and not beside the composables that draw it
 * A cover is **persisted identity**, not presentation. [D-017](docs/design/V2-SPEC-DEFECTS.md#d-017-ruling)
 * makes the assignment part of the zine, so it has to be storable — and the storage layer
 * (`data-android`) must be able to name the type and call [newZineCoverRecipe] at the moment a project
 * is created. A feature-module type could not be reached from there without inverting the layering.
 * `core:model` is the one place `core:data`, `data-android` and `feature:editor` can all see.
 *
 * The *rendering* of a recipe — which colours a surface resolves to, which glyph a stamp draws — stays
 * in the UI layer, because it needs `core:ui`, which this module must never depend on. The model says
 * **which** cover; the UI says **how to paint it**.
 */
public data class ZineCoverRecipe(
    val surface: ZineCoverSurface,
    val stamp: ZineCoverStamp,
)

/**
 * Draw a cover for a **new** zine.
 *
 * ## The ruling this function exists to satisfy
 * [D-017](docs/design/V2-SPEC-DEFECTS.md#d-017-ruling), 2026-07-30:
 *
 * > *"Assign the cover surface once when the zine is created and persist that assignment. A physical
 * > object should retain its identity across renames. Do not derive it from the title. Do not use
 * > round-robin assignment. Do not infer from neighbouring zines."*
 *
 * and [D-026](docs/design/V2-SPEC-DEFECTS.md#d-026-ruling), 2026-07-31, which completes it:
 *
 * > *"When a zine is duplicated: generate a new cover. Duplicate content. Do not duplicate visual
 * > identity."* — and *"legacy zines receive a cover on first presentation"*, then persist it.
 *
 * So a cover is **not inherited** either, and the only input is entropy.
 *
 * ## Why the parameter is [Random] and not anything else
 * B1 shipped an assigner guarded by a reflection test that scanned for any function mapping a `String`
 * to a cover; independent review found the guard could not hold the ruling, because
 * `newZineCoverRecipe(Random(title.hashCode()))` passes a *title-derived seed* through a signature that
 * mentions no title. No signature check decides a question about **information flow**.
 *
 * The enforcement that does work is structural and lives at the call sites, not here: this function
 * takes entropy and nothing else, and `RoomProjectRepository` — the only caller — holds no title at the
 * point it draws. The behavioural guarantee is testable and is tested: two projects created with the
 * *same* title receive independently drawn covers.
 *
 * ## Independence of the two axes
 * Surface and stamp are drawn **separately**, which is the frozen "grid × swappable ingredients" model
 * ([V2-IDENTITY.md](docs/design/V2-IDENTITY.md) §5). A single draw over 36 pairs would be
 * indistinguishable in one sample and wrong in aggregate — it would correlate the axes.
 */
public fun newZineCoverRecipe(random: Random = Random.Default): ZineCoverRecipe = ZineCoverRecipe(
    surface = ZineCoverSurface.entries[random.nextInt(ZineCoverSurface.entries.size)],
    stamp = ZineCoverStamp.entries[random.nextInt(ZineCoverStamp.entries.size)],
)
