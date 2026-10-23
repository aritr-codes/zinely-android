package com.aritr.zinely.core.editor.ink

/** My inks: nine kept plus Mix make two full rows of five (ADR-138). */
public const val MY_INKS_CAP: Int = 9

/** My palettes: named on purpose, so never evicted; the tenth asks for room (owner 2026-10-22). */
public const val PALETTE_CAP: Int = 9
public const val PALETTE_MIN: Int = 2
public const val PALETTE_MAX: Int = 6

/** The prototype name input's `maxlength`. */
public const val PALETTE_NAME_MAX: Int = 30

/** A kept ink. [used] is a logical clock value, not a time: larger means more recently used. */
public data class MyInk(val hex: InkHex, val used: Long)

/** A named palette. Its [inks] are copies of values, never references into My inks. */
public data class InkPalette(val id: Long, val name: String, val inks: List<InkHex>)

/** What a store mutation returns: the screen offers Undo from [changed]. */
public data class InkLibraryEdit(val before: InkLibrary, val after: InkLibrary) {
    public val changed: Boolean get() = before != after
}

public data class UseResult(val library: InkLibrary, val evicted: InkHex?)

public sealed interface PaletteResult {
    public data class Done(val library: InkLibrary, val paletteId: Long) : PaletteResult
    public data object Full : PaletteResult
    public data object TooFew : PaletteResult
    public data object NoName : PaletteResult
    public data object Missing : PaletteResult
}

/** `" copy"` / `" copy 2"` style endings (prototype `replace(/ copy( \d+)?$/, '')`). */
public fun stripCopySuffix(name: String, copySuffix: String): String =
    Regex(Regex.escape(copySuffix) + "( \\d+)?$").replaceFirst(name, "")

/**
 * The persisted shape of My inks and My palettes as pure state (ADR-138). Every operation returns a new
 * value and none throws on user input; the caps are enforced here, on every path, so a full library
 * never changes (the rev. 2 Undo rule: no change, no Undo).
 *
 * [myInks] is newest-made first and stable: a touch or re-use never reorders, it only bumps `used`.
 * [palettes] is newest first. [clock] only grows.
 */
public data class InkLibrary(
    val myInks: List<MyInk> = emptyList(),
    val palettes: List<InkPalette> = emptyList(),
    val clock: Long = 0,
    val nextPaletteId: Long = 1,
    val openPaletteId: Long? = null,
) {
    public fun kept(hex: InkHex): MyInk? = myInks.firstOrNull { it.hex == hex }

    /** Least recently used; the first in the list on a tie, like the prototype's `reduce`. */
    public fun leastRecentlyUsed(): MyInk? = myInks.minByOrNull { it.used }

    /** The ink [use] would drop, for the mixer's full note; non-null only when full and [hex] is not kept. */
    public fun wouldEvict(hex: InkHex): InkHex? =
        if (myInks.size >= MY_INKS_CAP && kept(hex) == null) leastRecentlyUsed()?.hex else null

    /** The mixer's "Use this ink". A kept ink keeps its place; a new one evicts the LRU at the cap. */
    public fun use(hex: InkHex): UseResult {
        val now = clock + 1
        if (kept(hex) != null) {
            return UseResult(copy(myInks = myInks.map { if (it.hex == hex) it.copy(used = now) else it }, clock = now), null)
        }
        val evicted = wouldEvict(hex)
        val rest = if (evicted == null) myInks else myInks.filter { it.hex != evicted }
        return UseResult(copy(myInks = listOf(MyInk(hex, now)) + rest, clock = now), evicted)
    }

    /** A tray pot tap: bumps `used` if kept, otherwise returns `this` so the caller sees "no change". */
    public fun touch(hex: InkHex): InkLibrary = if (kept(hex) == null) this else use(hex).library

    public fun remove(hex: InkHex): InkLibrary = copy(myInks = myInks.filter { it.hex != hex })

    /**
     * First free name, case-insensitive: the (trimmed) base, then "base 2", "base 3". Every candidate is cut
     * to [PALETTE_NAME_MAX] by shortening the stem, so a suffix is never the part that gets cut off.
     */
    public fun uniqueName(base: String): String {
        val b = base.trim()
        var n = b.take(PALETTE_NAME_MAX)
        var i = 2
        while (palettes.any { it.name.equals(n, ignoreCase = true) }) {
            val suffix = " ${i++}"
            n = b.take(PALETTE_NAME_MAX - suffix.length).trimEnd() + suffix
        }
        return n
    }

    public fun addPalette(name: String, inks: List<InkHex>): PaletteResult {
        if (palettes.size >= PALETTE_CAP) return PaletteResult.Full
        val (n, list) = validated(name, inks) ?: return invalid(name, inks)
        val id = nextPaletteId
        return PaletteResult.Done(
            copy(palettes = listOf(InkPalette(id, n, list)) + palettes, nextPaletteId = id + 1, openPaletteId = id),
            id,
        )
    }

    /** Same validation as [addPalette] but no cap check: editing never grows the count. */
    public fun updatePalette(id: Long, name: String, inks: List<InkHex>): PaletteResult {
        if (palettes.none { it.id == id }) return PaletteResult.Missing
        val (n, list) = validated(name, inks) ?: return invalid(name, inks)
        return PaletteResult.Done(
            copy(palettes = palettes.map { if (it.id == id) it.copy(name = n, inks = list) else it }), id,
        )
    }

    public fun duplicate(id: Long, copySuffix: String): PaletteResult {
        val p = palettes.firstOrNull { it.id == id } ?: return PaletteResult.Missing
        val stem = stripCopySuffix(p.name, copySuffix).take(PALETTE_NAME_MAX - copySuffix.length).trimEnd()
        return addPalette(uniqueName(stem + copySuffix), p.inks)
    }

    /** "Save a copy" on a starter palette. */
    public fun saveStarterCopy(name: String, inks: List<InkHex>): PaletteResult = addPalette(uniqueName(name), inks)

    /** Deleting the open palette opens the first remaining one (prototype `pals.find(open) || pals[0]`). */
    public fun deletePalette(id: Long): InkLibrary {
        val rest = palettes.filter { it.id != id }
        return copy(palettes = rest, openPaletteId = if (openPaletteId == id) rest.firstOrNull()?.id else openPaletteId)
    }

    public fun open(id: Long): InkLibrary = if (palettes.any { it.id == id }) copy(openPaletteId = id) else this

    public fun openPalette(): InkPalette? = palettes.firstOrNull { it.id == openPaletteId } ?: palettes.firstOrNull()

    /**
     * Brings anything read from disk back inside the invariants: unique My inks (first wins) capped at
     * [MY_INKS_CAP]; palettes with a name, 2..6 distinct inks, unique ids, capped at [PALETTE_CAP];
     * `clock` and `nextPaletteId` ahead of everything issued; a dangling open id dropped.
     */
    public fun sanitized(): InkLibrary {
        val inks = myInks.distinctBy { it.hex }.take(MY_INKS_CAP)
        val pals = palettes.mapNotNull { p ->
            val (n, list) = validated(p.name, p.inks) ?: return@mapNotNull null
            InkPalette(p.id, n, list)
        }.distinctBy { it.id }.take(PALETTE_CAP)
        return InkLibrary(
            myInks = inks,
            palettes = pals,
            clock = maxOf(clock, inks.maxOfOrNull { it.used } ?: 0L),
            nextPaletteId = maxOf(nextPaletteId, (pals.maxOfOrNull { it.id } ?: 0L) + 1, 1L),
            openPaletteId = openPaletteId?.takeIf { id -> pals.any { it.id == id } },
        )
    }

    public companion object {
        public val EMPTY: InkLibrary = InkLibrary()
    }

    private fun validated(name: String, inks: List<InkHex>): Pair<String, List<InkHex>>? {
        val n = name.trim().take(PALETTE_NAME_MAX).trim()
        val list = inks.distinct().take(PALETTE_MAX)
        return if (n.isEmpty() || list.size < PALETTE_MIN) null else n to list
    }

    private fun invalid(name: String, inks: List<InkHex>): PaletteResult =
        if (inks.distinct().size < PALETTE_MIN) PaletteResult.TooFew else PaletteResult.NoName
}
