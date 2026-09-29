package com.aritr.zinely.core.data.storage

import com.aritr.zinely.core.model.ZineDocument

/**
 * One zine already on the shelf, as read from its files (never Room). A `null` [title] or [documentSha256] means it
 * couldn't be read, and such a zine matches nothing. [decode] runs at most once, and only when its title equals a
 * backup entry's while its bytes don't; `null` means it couldn't be decoded.
 */
public class ShelfZine(
    public val title: String?,
    public val documentSha256: String?,
    private val decode: suspend () -> ZineDocument?,
) {
    private var decoded: ZineDocument? = null
    private var decodeTried = false

    internal suspend fun decoded(): ZineDocument? {
        if (!decodeTried) {
            decoded = decode()
            decodeTried = true
        }
        return decoded
    }
}

/** The staged zines a restore adds, in manifest order, and how many it found already on the shelf. */
public data class RestoreMatch(
    val added: List<StagedZineProject>,
    val alreadyHereCount: Int,
)

/**
 * ADR-121: restore adds what's new. A backup zine is already here when a shelf zine has the same exact title, format,
 * paper size and content — equal raw bytes, else equal decoded documents. Cover and times are not compared. Each
 * shelf zine accounts for at most one backup zine, walked in manifest order; anything not shown equal is added.
 *
 * Invariant: greedy is exact when shelf decoding is deterministic. A backup zine's bytes determine its decoded value
 * (staging decoded and verified it), so each shelf zine is compatible with exactly one class of backup zines, and
 * which of them it absorbs never changes the count. A shelf zine whose decode fails (even transiently) matches only
 * by bytes, so at worst one extra copy is added: doubt adds, never replaces.
 */
public object RestoreMatcher {
    public suspend fun match(backup: List<StagedZineProject>, shelf: List<ShelfZine>): RestoreMatch {
        val unused = HashMap<String, MutableList<ShelfZine>>()
        shelf.forEach { zine ->
            if (zine.title != null && zine.documentSha256 != null) unused.getOrPut(zine.title) { ArrayList() } += zine
        }
        val added = ArrayList<StagedZineProject>(backup.size)
        var alreadyHere = 0
        for (zine in backup) {
            val entry = zine.manifestEntry
            val candidates = unused[entry.title].orEmpty()
            val found = candidates.firstOrNull { it.documentSha256 == entry.documentSha256 }
                ?: candidates.firstOrNull { shelfZine ->
                    val decoded = shelfZine.decoded()
                    decoded != null && decoded.format == entry.format && decoded.paperSize == entry.paperSize &&
                        decoded == zine.document
                }
            if (found == null) {
                added += zine
            } else {
                unused.getValue(entry.title).remove(found)
                alreadyHere++
            }
        }
        return RestoreMatch(added, alreadyHere)
    }
}
