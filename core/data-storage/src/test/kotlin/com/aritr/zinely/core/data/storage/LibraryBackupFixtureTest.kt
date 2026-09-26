package com.aritr.zinely.core.data.storage

import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.ImageElement
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/**
 * The frozen library archive (1.x plan §4 F3). The writer→stager tests build their archive in memory,
 * so a change made to both sides at once passes them while breaking every backup already saved by
 * makers. This archive cannot move with the code: every future build must *stage* it — the pure-JVM
 * restore layer that reads, verifies, decodes (migrating v1/v2 documents) and closure-checks it. The
 * Android half of a restore (id allocation, `meta.json`, the committer and Room reconcile in
 * `:data-android`) is not exercised here.
 *
 * `fixtures/library-backup-v2.zine` was produced at `5f7707a` (0.9.0-beta.5) by the unchanged
 * [ZineLibraryBackupWriter], from three fictional zines (the `:core:data` document corpus, v1/v2/v3)
 * and two synthetic photos encoded as real JPEG bytes. It is not a device export: on a device the
 * Android repository assembles the manifest metadata, which here was built by hand to the same schema.
 *
 * Fixtures are frozen. A failure is a restore break to fix in code; never regenerate the archive to
 * pass. A new backup package version adds a new archive beside this one.
 */
class LibraryBackupFixtureTest {

    @TempDir
    lateinit var temp: Path

    @Test
    fun `the frozen library archive still stages`() = runBlocking {
        val archive = Path.of(checkNotNull(javaClass.getResource("/fixtures/library-backup-v2.zine")).toURI())
        assertEquals(ARCHIVE_SHA256, sha256(Files.readAllBytes(archive))) {
            "library-backup-v2.zine changed. Fixtures are frozen: add a new archive, never regenerate one."
        }

        ZineLibraryBackupStager().stage(archive, temp.resolve("staging")).use { staged ->
            assertEquals(2, staged.manifest.packageVersion)
            assertEquals("library", staged.manifest.kind)
            assertEquals("0.9.0-beta.5", staged.manifest.appVersion)
            assertEquals(
                listOf(
                    Triple("zine-v3", 3, DOC_V3),
                    Triple("zine-v2", 2, DOC_V2),
                    Triple("zine-v1", 1, DOC_V1),
                ),
                staged.projects.map {
                    Triple(it.manifestEntry.sourceProjectId, it.manifestEntry.documentSchemaVersion, it.manifestEntry.documentSha256)
                },
            )

            // The asset closure survives and the photos are still the JPEG bytes restore requires.
            assertEquals(setOf(PHOTO_A, PHOTO_B), staged.assets.keys)
            staged.assets.values.forEach { photo ->
                assertArrayEquals(JPEG_SOI, Files.readAllBytes(photo).copyOf(JPEG_SOI.size))
            }

            // Each project decodes to its own version's content (full equality lives in :core:data).
            val (v3, v2, v1) = staged.projects.map { project ->
                project.document.pages.flatMap { it.elements }
            }
            assertTrue(v3.filterIsInstance<ImageElement>().any { it.copier && it.flippedHorizontally })
            assertTrue(v3.filterIsInstance<DecorElement>().single().flippedVertically)
            assertTrue(v2.filterIsInstance<DecorElement>().single().let { it.mirrored && !it.flippedVertically })
            assertTrue(v1.none { it is DecorElement })
            assertTrue(v1.filterIsInstance<ImageElement>().none { it.copier })
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val ARCHIVE_SHA256 = "fd14efcd1e146bf3dc6541ac8e9cb4d0ff43034bf811a7003edad25eb7542fe0"

        /** The `:core:data` corpus files, pinned there by `DocumentFixtureCorpusTest`. */
        const val DOC_V1 = "b0e82a1a512ae724930f7b32d81213dca927f0e9f1b3233791e20618253da91e"
        const val DOC_V2 = "3ba7370f44123cf9dde809ee83dc4e951d20e19e2cdc4502197ed3fe1346ed24"
        const val DOC_V3 = "bba2f22d633bf0e3da9d9b72559bcfa0a937f459309dcb79b92a4e68fab13ae9"

        const val PHOTO_A = "380704824c8bf2b6f7f69cca9eae828fee7ff44b2328820e49eec362c674ed3a"
        const val PHOTO_B = "8f914e9d17abc02fbccbf33f384481a28e1ccb9dd642f3b4cf0e5b1ddd3f3219"

        val JPEG_SOI = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    }
}
