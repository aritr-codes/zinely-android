package com.aritr.zinely.core.editor

/** Recent rail length (unchanged from the session-only sheet it replaces). */
public const val ART_RECENT_CAP: Int = 6

/**
 * Favourites bound. The catalogue holds 32 pieces today; 48 leaves growth room while keeping a damaged or
 * hand-edited value from becoming unbounded. The oldest favourite is dropped first when a new one would exceed it.
 */
public const val ART_FAVOURITES_CAP: Int = 48

/** A stored id longer than this is garbage, not a piece id. */
private const val ART_ID_MAX: Int = 64

/**
 * The Art sheet's retrieval shortcuts (SUPPLIES-SPEC "Favourites and Recent are retrieval shortcuts"): piece
 * ids only, newest first for [recent], oldest first for [favourites]. Pure state; every operation returns a
 * new value that already satisfies [sanitized].
 *
 * Add-only id rule: piece ids are never renamed or reused, so a stored id the catalogue no longer knows is
 * harmless. It is dropped when read against the catalogue (`sanitized(known)`), never matched to something else.
 */
public data class ArtShortcuts(
    val recent: List<String> = emptyList(),
    val favourites: List<String> = emptyList(),
) {
    /** Picking [id] moves it to the front of Recent. */
    public fun picked(id: String): ArtShortcuts = copy(recent = listOf(id) + recent.filterNot { it == id }).sanitized()

    /** Adds [id] as the newest favourite, or removes it. */
    public fun favourite(id: String, on: Boolean): ArtShortcuts =
        copy(favourites = favourites.filterNot { it == id } + if (on) listOf(id) else emptyList()).sanitized()

    /**
     * Drops blank, oversize and duplicate ids and applies both caps. With [known], also drops ids the catalogue
     * does not contain (a removed or damaged id), so the sheet never shows a tile it cannot draw.
     */
    public fun sanitized(known: Set<String>? = null): ArtShortcuts {
        fun clean(ids: List<String>) =
            ids.filter { it.isNotBlank() && it.length <= ART_ID_MAX && (known == null || it in known) }.distinct()
        return ArtShortcuts(clean(recent).take(ART_RECENT_CAP), clean(favourites).takeLast(ART_FAVOURITES_CAP))
    }
}
