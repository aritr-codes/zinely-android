package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.repository.RestoredDetails
import com.aritr.zinely.core.data.storage.AtomicFileStore
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Zines put back from their earlier copy that wait to be told on My Shelf: their details (`meta.json`,
 * [ADR-134](docs/DECISIONS.md#adr-134)), their pages (`document.json`, [ADR-140](docs/DECISIONS.md#adr-140)), or
 * both, in one note per zine.
 *
 * Each note is a marker beside the zine's files (`restored-note.json`), which is what survives a restart; the copy
 * held here is what still shows the note for this run when that write fails (ADR-134 decision 4).
 *
 * **Lock-free, and calls nothing back** (ADR-140 decision 4). Both repositories take this one instance, so neither
 * depends on the other. It may be called with `RoomProjectRepository`'s mutex held (a `documents.load` under it)
 * or without (the Bench's load), so it never takes that mutex and its [changes] signal never suspends. One zine's
 * note changes atomically ([ConcurrentHashMap.compute]), so a details repair and a pages repair found at once on
 * two paths both land in it, and the marker records the merged note.
 */
internal class RepairNotes(rootDir: Path, private val store: AtomicFileStore) {

    private val paths = ProjectPaths(rootDir)
    private val json = Json { ignoreUnknownKeys = true }
    private val waiting = ConcurrentHashMap<String, RestoredDetails>()
    private val changeCount = MutableStateFlow(0)

    /** Bumped whenever a note is added to or cleared: the Shelf lists again (decision 6). */
    val changes: StateFlow<Int> = changeCount.asStateFlow()

    /** [id]'s waiting note, or `null`. A marker that cannot be read is still a details repair, without its old name. */
    fun noteOf(id: String): RestoredDetails? = waiting[id] ?: readMarker(id)

    /** [id]'s page file was put back from its earlier copy. */
    fun addPages(id: String) = add(id) { it?.copy(pages = true) ?: RestoredDetails(null, details = false, pages = true) }

    /**
     * [id]'s details were put back, the Shelf having shown [previousTitle]. A zine whose details already wait keeps
     * the first repair's old name: that is the one the maker knew.
     */
    fun addDetails(id: String, previousTitle: String?) = add(id) { held ->
        when {
            held == null -> RestoredDetails(previousTitle)
            held.details -> held
            else -> held.copy(previousTitle = previousTitle, details = true)
        }
    }

    /**
     * Got it on [shown], the note the maker saw: [id] stops waiting, both kinds, only if its note is still [shown].
     * The check is inside the atomic change, so a pages repair merged in after the note was shown survives and is
     * told. Throws when its marker cannot be removed; the note held here is cleared all the same, and the marker
     * left on disk brings it back on the next listing, in this run or the next.
     */
    fun clear(id: String, shown: RestoredDetails) = change(id) { held -> held.takeIf { it != shown } }

    /**
     * A rename or move: [id]'s details part only (A46.5, narrowing A37.6). A zine whose pages went back too keeps
     * waiting, as a pages-only note. Throws when its marker cannot be changed; the note held here changes all the
     * same, so the old marker is read again only once nothing is held for [id] (next listing, or next start).
     */
    fun clearDetails(id: String) = change(id) { held ->
        held?.takeIf { it.pages }?.copy(previousTitle = null, details = false)
    }

    /** [id]'s directory is gone, and its marker with it. */
    fun forget(id: String) {
        waiting.remove(id)
    }

    private fun add(id: String, merge: (RestoredDetails?) -> RestoredDetails) {
        try {
            change(id, merge)
        } catch (_: IOException) {
            // ADR-134 decision 4: shown this run from `waiting`; a full disk costs only its survival.
        }
    }

    /**
     * One zine's note goes from what it holds to [next] of it, atomically, and its marker follows. A marker that
     * cannot be written or removed still changes the note held here, then throws.
     */
    private fun change(id: String, next: (RestoredDetails?) -> RestoredDetails?) {
        var failure: IOException? = null
        var changed = false
        // ponytail: the marker's I/O runs inside compute, holding this one key's bin; notes are rare and tiny.
        waiting.compute(id) { _, held ->
            val now = held ?: readMarker(id)
            val after = next(now)
            if (after != now) {
                changed = true
                try {
                    if (after == null) deleteMarker(id) else writeMarker(id, after)
                } catch (e: IOException) {
                    failure = e
                } catch (e: Exception) {
                    // Never out of compute: the note held here must still change (ADR-134 decision 4).
                    failure = IOException(e)
                }
            }
            after
        }
        if (changed) changeCount.update { it + 1 }
        failure?.let { throw it }
    }

    private fun readMarker(id: String): RestoredDetails? {
        val marker = paths.restoredNoteFile(id) ?: return null
        if (!Files.exists(marker)) return null
        val file = try {
            store.read(marker)?.let { json.decodeFromString(RestoredNoteFile.serializer(), it.decodeToString()) }
        } catch (_: Exception) {
            null
        } ?: return RestoredDetails(null)
        return RestoredDetails(file.previousTitle, details = file.details, pages = file.pages)
    }

    private fun writeMarker(id: String, note: RestoredDetails) {
        val marker = paths.restoredNoteFile(id) ?: return
        // A zine deleted while its page file was being read: never bring its directory back for a marker.
        if (!Files.isDirectory(marker.parent)) return
        val file = RestoredNoteFile(note.previousTitle, details = note.details, pages = note.pages)
        store.write(marker, json.encodeToString(RestoredNoteFile.serializer(), file).encodeToByteArray())
    }

    /**
     * The `.bak` goes first: [readMarker] checks only the main marker, so a throw on either delete leaves the main
     * marker in place and the failure is never a note silently lost.
     */
    private fun deleteMarker(id: String) {
        val marker = paths.restoredNoteFile(id) ?: return
        Files.deleteIfExists(marker.resolveSibling(marker.fileName.toString() + ".bak"))
        Files.deleteIfExists(marker)
    }

    /**
     * `restored-note.json` (ADR-134 decision 5, ADR-140 decision 5): the title the Shelf showed before a details
     * repair, when known, and which kinds went back. The defaults make a marker written before ADR-140 read as a
     * details repair with its old name. A sidecar, never part of `document.json`.
     */
    @Serializable
    private data class RestoredNoteFile(
        val previousTitle: String? = null,
        val details: Boolean = true,
        val pages: Boolean = false,
    )
}
