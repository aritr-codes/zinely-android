package com.aritr.zinely.feature.library

import com.aritr.zinely.core.copy.Copy

/**
 * What a typed folder name stands for. The rules are `FolderNames`' in `:core:data`
 * ([ADR-125](docs/DECISIONS.md#adr-125) rule 13), which this module cannot see, so the host answers and
 * the name sheet only reads the answer.
 */
public sealed interface FolderNameVerdict {
    /** Nothing a reader can see. */
    public data object Blank : FolderNameVerdict

    /** The Shelf's own name, in any spelling (A28.10). */
    public data object MyShelf : FolderNameVerdict

    /**
     * A name.
     *
     * @property name what would be written: trimmed and cleaned.
     * @property key the value two names share exactly when they are the same folder.
     * @property cut the typed text was longer than a name may be, and [name] is what fits.
     */
    public data class Name(val name: String, val key: String, val cut: Boolean = false) : FolderNameVerdict
}

/** One thing standing on the Shelf: a zine, or a folder's pile of them. */
internal sealed interface ShelfTile {
    data class Zine(val zine: LibraryZine) : ShelfTile

    /** @property zines the folder's zines, newest first; never empty. */
    data class Pile(val name: String, val zines: List<LibraryZine>) : ShelfTile
}

/**
 * The tiles of one view (A28.4). On My Shelf a folder is one pile, standing where its newest zine would
 * stand; inside [openFolder] the tiles are that folder's zines. [zines] is already newest first and is
 * never re-sorted.
 */
internal fun shelfTiles(zines: List<LibraryZine>, openFolder: String?): List<ShelfTile> {
    if (openFolder != null) return zines.filter { it.folder == openFolder }.map(ShelfTile::Zine)
    val piles = zines.filter { it.folder != null }.groupBy { it.folder }
    val seen = HashSet<String>()
    return zines.mapNotNull { zine ->
        val folder = zine.folder ?: return@mapNotNull ShelfTile.Zine(zine)
        if (seen.add(folder)) ShelfTile.Pile(folder, piles.getValue(folder)) else null
    }
}

/** The Shelf's folders with how many zines each holds, in the order their piles stand. */
internal fun shelfFolders(zines: List<LibraryZine>): Map<String, Int> =
    zines.mapNotNull { it.folder }.groupingBy { it }.eachCount()

/**
 * What the name sheet shows for the text in its field (A28.10): the button's words, whether it can be
 * pressed, the line under the field, and the name pressing it would use.
 */
internal data class FolderNameAnswer(
    val button: String,
    val enabled: Boolean,
    val hint: String = "",
    val name: String? = null,
)

/**
 * *New folder*, for a zine now in [current] (`null` is My Shelf). Typing a name that exists joins that
 * folder and takes its spelling.
 *
 * @param keys each existing folder by its key.
 */
internal fun newFolderAnswer(verdict: FolderNameVerdict, keys: Map<String, String>, current: String?): FolderNameAnswer =
    when (verdict) {
        FolderNameVerdict.Blank -> FolderNameAnswer(Copy.Folders.MAKE_FOLDER, enabled = false)
        FolderNameVerdict.MyShelf ->
            FolderNameAnswer(Copy.Folders.MAKE_FOLDER, enabled = false, hint = Copy.Folders.MY_SHELF_IS_TAKEN)
        is FolderNameVerdict.Name -> when (val hit = keys[verdict.key]) {
            null -> FolderNameAnswer(Copy.Folders.MAKE_FOLDER, enabled = true, name = verdict.name)
            current -> FolderNameAnswer(Copy.Folders.moveTo(hit), enabled = false, hint = Copy.Folders.alreadyIn(hit))
            else -> FolderNameAnswer(Copy.Folders.moveTo(hit), enabled = true, hint = Copy.Folders.willJoin(hit), name = hit)
        }
    }

/**
 * *Rename folder*, for the folder [renaming]. Another folder's name is refused; changing only the capitals
 * of its own name is a rename like any other; its own name unchanged is nothing to do.
 */
internal fun renameFolderAnswer(verdict: FolderNameVerdict, keys: Map<String, String>, renaming: String): FolderNameAnswer =
    when (verdict) {
        FolderNameVerdict.Blank -> FolderNameAnswer(Copy.Folders.RENAME, enabled = false)
        FolderNameVerdict.MyShelf ->
            FolderNameAnswer(Copy.Folders.RENAME, enabled = false, hint = Copy.Folders.MY_SHELF_IS_TAKEN)
        is FolderNameVerdict.Name -> {
            val hit = keys[verdict.key]
            when {
                hit != null && hit != renaming ->
                    FolderNameAnswer(Copy.Folders.RENAME, enabled = false, hint = Copy.Folders.nameTaken(hit))
                else -> FolderNameAnswer(Copy.Folders.RENAME, enabled = verdict.name != renaming, name = verdict.name)
            }
        }
    }
