package com.aritr.zinely.home

import androidx.compose.ui.graphics.ImageBitmap
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.imposition.Imposer
import com.aritr.zinely.core.render.SceneRenderer
import com.aritr.zinely.data.android.ProjectDocumentLayout
import com.aritr.zinely.editor.editedPageSize
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

/**
 * Produces and caches the shelf's page-1 thumbnails (S6.4, ADR-045). The cache is **derived, never
 * authoritative** (the Room-index stance, ADR-042): a PNG per project under the app's `cacheDir`
 * (`thumbnails/<id>.png`) plus a small in-memory LRU — both rebuildable at will from
 * `document.json`, both safe to lose.
 *
 * Invalidation is one stamp: the PNG's own mtime is **set to the source document's mtime** after a
 * successful write, and validity is exact equality. The stamp is read *before* the document is
 * loaded, so a concurrent document write on either side of the render leaves stamp ≠ new mtime and
 * the next [ensure] regenerates; a torn write (crash mid-encode) leaves mtime = wall clock ≠ stamp,
 * same outcome. Living in `cacheDir` (not `projects/<id>/`) means `deleteProject`'s recursive
 * directory delete never races an in-flight thumbnail write (Codex F1) — a delete mid-render costs
 * at most one orphaned, system-purgeable cache PNG. No sweeper, no GC interplay: a thumbnail is
 * not a content-addressed blob and never a GC root (ADR-031 untouched).
 *
 * All work runs on [io] under one producer-wide [Mutex]: renders are sequential (no concurrent
 * same-file writes) and a fresh thumbnail costs one `mtime` stat + a map hit, so callers may
 * re-[ensure] on every shelf emission and the system converges.
 */
internal class ShelfThumbnailProducer(
    private val thumbsDir: Path,
    private val layout: ProjectDocumentLayout,
    private val documents: DocumentRepository,
    private val imposer: Imposer,
    private val raster: ThumbnailRaster,
    private val io: CoroutineDispatcher,
    maxMemoryEntries: Int = MAX_MEMORY_THUMBNAILS,
) : ShelfThumbnails {

    private class CachedThumb(val stampEpochMs: Long, val bitmap: ImageBitmap)

    private val mutex = Mutex()

    /**
     * Access-ordered LRU capped at [maxMemoryEntries] (Codex round-2: an uncapped singleton bitmap
     * map grows for the process lifetime). Evictees are dropped, not recycled — a still-composed
     * card may hold the evicted bitmap; GC reclaims it when the UI lets go.
     */
    private val memory = object : LinkedHashMap<String, CachedThumb>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, CachedThumb>): Boolean =
            size > maxMemoryEntries
    }

    override suspend fun ensure(projectId: String): ImageBitmap? = withContext(io) {
        mutex.withLock {
            val documentFile = layout.documentFile(projectId) ?: return@withLock null
            // The stamp, read BEFORE the load: any document write after this point (either side of
            // the render) makes stamp ≠ current mtime, so staleness can hide at most one check.
            val stamp = mtimeOrNull(documentFile) ?: run {
                memory.remove(projectId) // the project is gone; don't serve its ghost
                return@withLock null
            }
            memory[projectId]?.takeIf { it.stampEpochMs == stamp }?.let { return@withLock it.bitmap }

            val png = thumbsDir.resolve("$projectId.png")
            if (mtimeOrNull(png) == stamp) {
                // Disk-fresh; a corrupt PNG (decode null) falls through and re-renders instead of
                // pinning the placeholder forever.
                raster.decodePng(png)?.let {
                    memory[projectId] = CachedThumb(stamp, it)
                    return@withLock it
                }
            }

            val document = (documents.load(projectId) as? DataResult.Success)?.value
                ?: return@withLock null
            val page = document.pages.firstOrNull { it.index == 0 } ?: return@withLock null
            val pageSizePt = editedPageSize(document, imposer)
            val tape = SceneRenderer.render(page, pageSizePt, document.defaults)

            val bitmap = try {
                Files.createDirectories(thumbsDir)
                Files.newOutputStream(png).use { raster.renderPng(tape, pageSizePt, it) }
            } catch (_: IOException) {
                null
            }
            if (bitmap == null) {
                // Never leave a wall-clock-stamped partial behind; the next ensure() retries.
                runCatching { Files.deleteIfExists(png) }
                memory.remove(projectId)
                return@withLock null
            }
            runCatching { Files.setLastModifiedTime(png, FileTime.fromMillis(stamp)) }
            memory[projectId] = CachedThumb(stamp, bitmap)
            bitmap
        }
    }

    private fun mtimeOrNull(path: Path): Long? = try {
        Files.getLastModifiedTime(path).toMillis()
    } catch (_: IOException) {
        null
    }
}

/** Thumbnail longest edge in pixels — crisp at the card's ~72 dp height on a 3× display. */
internal const val THUMBNAIL_LONGEST_EDGE_PX: Int = 320

/** In-memory LRU cap: ~24 × ~160 KB ARGB thumbs ≈ 4 MB worst case. */
internal const val MAX_MEMORY_THUMBNAILS: Int = 24
