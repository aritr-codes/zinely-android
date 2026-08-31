package com.aritr.zinely.core.data.storage

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class AdditiveLibraryRestoreCommitterTest {
    @TempDir
    lateinit var root: Path

    @Test
    fun `commits complete projects and one shared content-addressed asset`() {
        val fixture = fixture()
        val assetBytes = "shared image".toByteArray()
        val hash = sha256(assetBytes)
        val asset = write(fixture.session.resolve("assets/$hash"), assetBytes)
        val first = project(fixture.session, "first")
        val second = project(fixture.session, "second")

        fixture.committer.commit(
            PreparedLibraryRestore(
                transactionId = "restore-1",
                projects = listOf(
                    PreparedRestoreProject("first", first),
                    PreparedRestoreProject("second", second),
                ),
                assets = listOf(PreparedRestoreAsset(hash, asset)),
            ),
        )

        assertTrue(Files.isRegularFile(fixture.projects.resolve("first/document.json")))
        assertTrue(Files.isRegularFile(fixture.projects.resolve("second/document.json")))
        assertArrayEquals(assetBytes, Files.readAllBytes(fixture.assets.resolve(hash)))
        assertFalse(Files.exists(fixture.work.resolve("pending-library-restore.v1")))
    }

    @Test
    fun `project collision fails before journal or live writes`() {
        val fixture = fixture()
        val existing = write(fixture.projects.resolve("kept/document.json"), "existing".toByteArray())
        val staged = project(fixture.session, "incoming")

        assertThrows(IllegalArgumentException::class.java) {
            fixture.committer.commit(
                PreparedLibraryRestore(
                    "restore-2",
                    listOf(PreparedRestoreProject("kept", staged)),
                    emptyList(),
                ),
            )
        }

        assertArrayEquals("existing".toByteArray(), Files.readAllBytes(existing))
        assertFalse(Files.exists(fixture.work.resolve("pending-library-restore.v1")))
    }

    @Test
    fun `commit failure rolls back every newly visible project and preserves existing projects`() {
        val projects = root.resolve("projects")
        val assets = root.resolve("assets")
        val work = root.resolve("restore")
        val session = work.resolve("session")
        val existing = write(projects.resolve("kept/document.json"), "existing".toByteArray())
        val failingFs = FailingProjectDirectoryFsync(projects)
        val committer = AdditiveLibraryRestoreCommitter(projects, assets, work, failingFs)

        assertThrows(IOException::class.java) {
            committer.commit(
                PreparedLibraryRestore(
                    "restore-3",
                    listOf(
                        PreparedRestoreProject("first", project(session, "first")),
                        PreparedRestoreProject("second", project(session, "second")),
                    ),
                    emptyList(),
                ),
            )
        }

        assertFalse(Files.exists(projects.resolve("first")))
        assertFalse(Files.exists(projects.resolve("second")))
        assertArrayEquals("existing".toByteArray(), Files.readAllBytes(existing))
        assertFalse(Files.exists(work.resolve("pending-library-restore.v1")))
    }

    @Test
    fun `startup recovery removes projects named by an interrupted durable journal`() {
        val fixture = fixture()
        write(fixture.projects.resolve("first/document.json"), "first".toByteArray())
        write(fixture.projects.resolve("second/document.json"), "second".toByteArray())
        write(
            fixture.work.resolve("pending-library-restore.v1"),
            """
            ZINELY_LIBRARY_RESTORE_V1
            transaction=restore-4
            project=first
            project=second
            """.trimIndent().plus("\n").toByteArray(),
        )

        assertTrue(fixture.committer.recoverInterruptedCommit())
        assertFalse(Files.exists(fixture.projects.resolve("first")))
        assertFalse(Files.exists(fixture.projects.resolve("second")))
        assertFalse(Files.exists(fixture.work.resolve("pending-library-restore.v1")))
        assertFalse(fixture.committer.recoverInterruptedCommit())
    }

    @Test
    fun `existing asset with bytes that do not match its name fails closed`() {
        val fixture = fixture()
        val goodBytes = "expected".toByteArray()
        val hash = sha256(goodBytes)
        write(fixture.assets.resolve(hash), "poison".toByteArray())
        val stagedAsset = write(fixture.session.resolve("assets/$hash"), goodBytes)

        assertThrows(IllegalArgumentException::class.java) {
            fixture.committer.commit(
                PreparedLibraryRestore(
                    "restore-5",
                    listOf(PreparedRestoreProject("first", project(fixture.session, "first"))),
                    listOf(PreparedRestoreAsset(hash, stagedAsset)),
                ),
            )
        }

        assertFalse(Files.exists(fixture.projects.resolve("first")))
        assertFalse(Files.exists(fixture.work.resolve("pending-library-restore.v1")))
    }

    @Test
    fun `overlapping staged paths fail before journal or live writes`() {
        val fixture = fixture()
        val project = project(fixture.session, "first")
        val assetBytes = "nested".toByteArray()
        val hash = sha256(assetBytes)
        val nestedAsset = write(project.resolve("nested-asset.bin"), assetBytes)

        assertThrows(IllegalArgumentException::class.java) {
            fixture.committer.commit(
                PreparedLibraryRestore(
                    "restore-6",
                    listOf(PreparedRestoreProject("first", project)),
                    listOf(PreparedRestoreAsset(hash, nestedAsset)),
                ),
            )
        }

        assertFalse(Files.exists(fixture.projects.resolve("first")))
        assertFalse(Files.exists(fixture.work.resolve("pending-library-restore.v1")))
    }

    @Test
    fun `disjoint staged project directories may have different nesting depths`() {
        val fixture = fixture()
        val shallow = project(fixture.session, "shallow")
        val deep = project(fixture.session.resolve("nested"), "deep")

        fixture.committer.commit(
            PreparedLibraryRestore(
                "restore-disjoint",
                listOf(
                    PreparedRestoreProject("shallow", shallow),
                    PreparedRestoreProject("deep", deep),
                ),
                emptyList(),
            ),
        )

        assertTrue(Files.isRegularFile(fixture.projects.resolve("shallow/document.json")))
        assertTrue(Files.isRegularFile(fixture.projects.resolve("deep/document.json")))
    }

    @Test
    fun `pending journal blocks a new commit until recovery runs`() {
        val fixture = fixture()
        write(
            fixture.work.resolve("pending-library-restore.v1"),
            """
            ZINELY_LIBRARY_RESTORE_V1
            transaction=restore-pending
            project=stale
            """.trimIndent().encodeToByteArray(),
        )

        assertThrows(IllegalStateException::class.java) {
            fixture.committer.commit(
                PreparedLibraryRestore(
                    "restore-7",
                    listOf(PreparedRestoreProject("first", project(fixture.session, "first"))),
                    emptyList(),
                ),
            )
        }

        assertFalse(Files.exists(fixture.projects.resolve("first")))
        assertTrue(Files.exists(fixture.work.resolve("pending-library-restore.v1")))
    }

    private fun fixture(): Fixture {
        val projects = root.resolve("projects")
        val assets = root.resolve("assets")
        val work = root.resolve("restore")
        return Fixture(
            projects,
            assets,
            work,
            work.resolve("session"),
            AdditiveLibraryRestoreCommitter(projects, assets, work),
        )
    }

    private fun project(parent: Path, id: String): Path {
        val directory = parent.resolve("projects/$id")
        write(directory.resolve("document.json"), "document-$id".toByteArray())
        write(directory.resolve("meta.json"), "meta-$id".toByteArray())
        return directory
    }

    private fun write(path: Path, bytes: ByteArray): Path {
        Files.createDirectories(path.parent)
        Files.write(path, bytes)
        return path
    }

    private fun sha256(bytes: ByteArray): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private data class Fixture(
        val projects: Path,
        val assets: Path,
        val work: Path,
        val session: Path,
        val committer: AdditiveLibraryRestoreCommitter,
    )

    private class FailingProjectDirectoryFsync(private val projectsDir: Path) : FileSystemOps {
        override val capabilities: FsCapabilities =
            FsCapabilities(atomicReplace = true, fileFsync = true, dirFsync = true)

        private var failed = false

        override fun fsyncFile(path: Path) = NioFileSystemOps.fsyncFile(path)

        override fun fsyncDir(dir: Path) {
            if (!failed && dir == projectsDir) {
                failed = true
                throw IOException("injected project-directory fsync failure")
            }
            NioFileSystemOps.fsyncDir(dir)
        }

        override fun atomicReplace(source: Path, replacing: Path) =
            NioFileSystemOps.atomicReplace(source, replacing)
    }
}
