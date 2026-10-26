package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.repository.RestoredDetails
import com.aritr.zinely.core.data.storage.AtomicFileStore
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * #161: the marker's [AtomicFileStore] write uses one fixed `.tmp` sidecar, so two writers on one zine's marker at
 * once could tear it or drop one repair. Production has one [RepairNotes] (a Hilt singleton) and every marker write
 * or delete runs inside its per-zine `compute`, so a details repair and a pages repair found at the same moment on
 * two threads must both reach the marker, and no `.tmp` may be left behind.
 */
class RepairNotesConcurrencyTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `given a details repair and a pages repair on two threads at once then the marker on disk holds both`() {
        // Given one shared notes store and many zines on disk
        val root = tmp.root.toPath()
        val notes = RepairNotes(root, AtomicFileStore())
        val ids = (1..200).map { "z$it" }
        ids.forEach { Files.createDirectories(root.resolve("projects/$it")) }

        // When each zine's two repairs are reported from two threads released together
        val pool = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        val details = pool.submit { start.await(); ids.forEach { notes.addDetails(it, previousTitle = "Old $it") } }
        val pages = pool.submit { start.await(); ids.reversed().forEach { notes.addPages(it) } }
        start.countDown()
        details.get(30, TimeUnit.SECONDS)
        pages.get(30, TimeUnit.SECONDS)
        pool.shutdown()

        // Then a fresh read of each marker (what the next start sees) has both kinds and the old name
        val fresh = RepairNotes(root, AtomicFileStore())
        ids.forEach { id ->
            assertEquals(id, RestoredDetails("Old $id", details = true, pages = true), fresh.noteOf(id))
            assertTrue(id, Files.list(root.resolve("projects/$id")).use { s -> s.noneMatch { it.toString().endsWith(".tmp") } })
        }
    }
}
