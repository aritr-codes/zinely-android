package com.aritr.zinely.feature.editor

import kotlin.math.abs

/**
 * The four print archetypes a cover's ink field can take (`shelf.html` `ARCH`).
 *
 * Declaration order is load-bearing: it is the spec's array order, and the recipe indexes into it.
 */
internal enum class ShelfArchetype { Sun, Bars, Tone, Split }

/**
 * One of the four riso inks, as a *palette slot* rather than a colour. The cover is drawn from
 * `ZinelyColors`, which is the only place the frozen palette exists; resolving to a `Color` here
 * would fork it.
 */
internal enum class ShelfInk { Coral, Teal, Stamp, Yellow }

/**
 * A cover's generated look: a dominant ink field, an optional misregistered second ink, one of four
 * print archetypes, and which of the two paper stocks it is printed on.
 *
 * @property accent `null` when the recipe calls for a single-ink cover (`--ink2` unset in the spec).
 *   [ShelfArchetype.Split] still paints a second ink then — the frozen CSS gives it a `var(--teal)`
 *   fallback that no other archetype has.
 */
internal data class ShelfCoverRecipe(
    val archetype: ShelfArchetype,
    val field: ShelfInk,
    val accent: ShelfInk?,
    val usePaper2: Boolean,
)

/** `shelf.html` `FIELD` — the dominant-ink lookup. */
private val FIELD = listOf(ShelfInk.Coral, ShelfInk.Teal, ShelfInk.Stamp, ShelfInk.Yellow)

/** `shelf.html` `ACCENT` — the second-ink lookup. Deliberately a different rotation of `FIELD`. */
private val ACCENT = listOf(ShelfInk.Yellow, ShelfInk.Coral, ShelfInk.Teal, ShelfInk.Stamp)

/**
 * ```js
 * const hash = s => { let h=0; for(const c of s) h=(h+c.charCodeAt(0))|0; return Math.abs(h); };
 * ```
 *
 * `|0` is a wrap to signed 32-bit, which is what Kotlin's `Int` addition already does — so the sum
 * overflows identically. `charCodeAt` yields UTF-16 code *units*, which is `Char.code`, not a code
 * point: a title carrying an emoji must hash through both of its surrogates to agree with the spec.
 *
 * `Math.abs` of `Int.MIN_VALUE` widens to a positive double in JS; Kotlin's [abs] returns
 * `Int.MIN_VALUE` unchanged, and a negative would crash the `% 4` lookup below — hence the floor at
 * zero. That single sum is the one input where this disagrees with the spec (JS keeps the magnitude,
 * so its `% 3` accent gate and `% 2` paper stock differ); it is unreachable from any real title, and
 * both sides still produce a valid recipe.
 */
internal fun shelfCoverHash(title: String): Int {
    var h = 0
    for (c in title) h += c.code
    return abs(h).coerceAtLeast(0)
}

/**
 * The frozen cover recipe, keyed by a stable hash of the title:
 *
 * ```js
 * const recipe = t => { const h=hash(t);
 *   return { arch:ARCH[h%4], ink1:FIELD[h%4], ink2:(h%3===0)?ACCENT[(h>>2)%4]:"", stock:(h%2)?"p2":"" }; };
 * ```
 *
 * Invariant, and the whole point of hashing rather than cycling the grid index: a zine keeps its own
 * look under **any** sort or filter, so the shelf reads as a drawer of different objects rather than
 * one template repeated. Renaming a zine reprints its cover — that is the spec's behaviour, not a bug.
 *
 * Note `archetype` and `field` share the `h % 4` index, so archetype and dominant ink are perfectly
 * correlated. That is the spec, verbatim.
 */
internal fun shelfCoverRecipe(title: String): ShelfCoverRecipe {
    val h = shelfCoverHash(title)
    return ShelfCoverRecipe(
        archetype = ShelfArchetype.entries[h % 4],
        field = FIELD[h % 4],
        accent = if (h % 3 == 0) ACCENT[(h shr 2) % 4] else null,
        usePaper2 = h % 2 == 1,
    )
}
