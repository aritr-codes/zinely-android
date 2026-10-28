package com.aritr.zinely.feature.library

import androidx.compose.runtime.saveable.Saver

/**
 * The zines chosen while choosing several (`v21-library.html` A47, [ADR-142](docs/DECISIONS.md#adr-142)).
 *
 * Never empty: letting go of the last zine stops choosing (A47.4), so every way of making one returns `null`
 * instead of an empty choice.
 *
 * @property ids the chosen zines, in the order they were chosen.
 * @property folder the view the choice was made in: `null` is My Shelf. A choice belongs to one view (A47.6),
 *   so opening or leaving a folder ends it.
 * @property last the zine touched last, where ✕ and Back put focus (A47.12).
 */
internal data class ShelfSelection(val ids: List<String>, val folder: String?, val last: String) {

    /** Chooses [id] or lets it go. `null` once nothing is chosen. */
    fun toggle(id: String): ShelfSelection? {
        val next = if (id in ids) ids - id else ids + id
        return if (next.isEmpty()) null else ShelfSelection(next, folder, id)
    }

    /**
     * Keeps only the zines still shown in [view] (A47.10). `null` when none is left, or when the view is not the
     * one the choice was made in.
     */
    fun prune(view: String?, shown: Collection<String>): ShelfSelection? {
        if (view != folder) return null
        val kept = ids.filter { it in shown }
        return when {
            kept.isEmpty() -> null
            kept == ids -> this
            else -> copy(ids = kept)
        }
    }

    companion object {
        fun start(id: String, folder: String?): ShelfSelection = ShelfSelection(listOf(id), folder, id)

        /** Saved like the open folder and an open sheet (A47.10): `[folder or "", last, ids…]`, with a flag for My Shelf. */
        val Saver: Saver<ShelfSelection?, Any> = Saver(
            save = { s -> if (s == null) emptyList<Any>() else listOf(s.folder != null, s.folder.orEmpty(), s.last) + s.ids },
            restore = { saved ->
                runCatching {
                    val f = saved as List<*>
                    val ids = f.drop(3).map { it as String }
                    if (ids.isEmpty()) null else ShelfSelection(ids, (f[1] as String).takeIf { f[0] as Boolean }, f[2] as String)
                }.getOrNull()
            },
        )
    }
}
