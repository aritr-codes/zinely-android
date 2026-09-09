package com.aritr.zinely.core.data.storage

import com.aritr.zinely.core.data.asset.ContentHash
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.Comparator

/** A complete project directory prepared outside the live project tree. */
public data class PreparedRestoreProject(
    val localProjectId: String,
    val stagedDirectory: Path,
)

/** A hash-verified asset prepared outside the live content-addressed asset tree. */
public data class PreparedRestoreAsset(
    val hash: String,
    val stagedFile: Path,
)

/**
 * The filesystem portion of an additive library restore, ready for its non-cancellable commit.
 *
 * Project directories must already contain every live sidecar required by the Android repository.
 * This pure layer deliberately does not know the private `meta.json` wire format or Room. The later
 * Android adapter chooses collision-free local ids, prepares those directories, serialises repository
 * metadata, and reconciles Room only after [AdditiveLibraryRestoreCommitter.commit] returns.
 */
public data class PreparedLibraryRestore(
    val transactionId: String,
    val projects: List<PreparedRestoreProject>,
    val assets: List<PreparedRestoreAsset>,
)

/**
 * Recoverable additive commit for a fully validated and staged v2 library restore (ADR-110).
 *
 * This class assumes the caller holds the repository's library-wide writer lock from preparation
 * through Room reconciliation. It writes a durable intent journal before touching live paths, moves
 * only complete project directories into previously absent ids, and removes every planned project on
 * a caught failure. If the process dies between those operations, [recoverInterruptedCommit] removes
 * the same planned ids before the repository is allowed to scan files into Room.
 *
 * Assets are installed first and may remain as verified, content-addressed orphans after rollback.
 * They are not user-visible projects, cannot poison an existing hash (existing blobs are re-hashed),
 * and deliberately are not deleted during recovery: concurrent content-addressed consumers could
 * legitimately reference identical bytes. This matches the existing no-GC asset-store guarantee.
 *
 * Commit is synchronous by design. Once it starts, callers must not make it coroutine-cancellable;
 * cancellation is safe during archive staging, while live commit follows the existing
 * non-interruptible persistence discipline.
 */
public class AdditiveLibraryRestoreCommitter(
    private val liveProjectsDir: Path,
    private val liveAssetsDir: Path,
    private val restoreWorkDir: Path,
    private val fs: FileSystemOps = NioFileSystemOps,
) {
    private val journal: Path = restoreWorkDir.resolve(JOURNAL_NAME)

    init {
        val caps = fs.capabilities
        require(caps.atomicReplace && caps.fileFsync) {
            "Library restore commit needs atomic replace and file fsync; got $caps"
        }
    }

    /**
     * Commits [prepared] additively. No existing project or asset is overwritten.
     *
     * @throws IllegalStateException when an interrupted transaction needs recovery first.
     * @throws IllegalArgumentException when the prepared set is unsafe or internally inconsistent.
     * @throws IOException when durable journal or atomic filesystem work fails.
     */
    public fun commit(prepared: PreparedLibraryRestore) {
        check(!Files.exists(journal, LinkOption.NOFOLLOW_LINKS)) {
            "An interrupted library restore must be recovered before another commit"
        }
        validatePrepared(prepared)
        preflightLiveTargets(prepared)
        fsyncPrepared(prepared)
        writeJournal(prepared)

        var committed = false
        try {
            Files.createDirectories(liveAssetsDir)
            prepared.assets.forEach(::installAsset)

            Files.createDirectories(liveProjectsDir)
            prepared.projects.forEach { project ->
                val target = liveProjectsDir.resolve(project.localProjectId)
                atomicMoveNew(project.stagedDirectory, target)
                fs.fsyncDir(liveProjectsDir)
            }

            Files.delete(journal)
            fs.fsyncDir(restoreWorkDir)
            committed = true
        } finally {
            if (!committed) {
                val rollbackFailure = rollbackProjects(prepared.projects.map { it.localProjectId })
                if (rollbackFailure == null) {
                    Files.deleteIfExists(journal)
                    fs.fsyncDir(restoreWorkDir)
                }
            }
        }
    }

    /**
     * Rolls back a process-interrupted commit. Must run before files are reconciled into Room.
     *
     * @return `true` when a pending journal was found and recovered, otherwise `false`.
     */
    public fun recoverInterruptedCommit(): Boolean {
        if (!Files.exists(journal, LinkOption.NOFOLLOW_LINKS)) return false
        require(Files.isRegularFile(journal, LinkOption.NOFOLLOW_LINKS)) {
            "Restore journal is not a regular file: $journal"
        }
        val pending = decodeJournal(Files.readAllBytes(journal))
        val rollbackFailure = rollbackProjects(pending.projectIds)
        if (rollbackFailure != null) throw rollbackFailure
        Files.delete(journal)
        fs.fsyncDir(restoreWorkDir)
        return true
    }

    private fun validatePrepared(prepared: PreparedLibraryRestore) {
        require(TRANSACTION_ID.matches(prepared.transactionId)) { "Unsafe restore transaction id" }
        require(prepared.projects.isNotEmpty()) { "A library restore must contain at least one project" }
        require(prepared.projects.map { it.localProjectId }.toSet().size == prepared.projects.size) {
            "Duplicate local project id in prepared restore"
        }
        require(prepared.assets.map { it.hash }.toSet().size == prepared.assets.size) {
            "Duplicate asset hash in prepared restore"
        }

        val workRoot = restoreWorkDir.toAbsolutePath().normalize()
        val projectRoots = prepared.projects.map { it.stagedDirectory.toAbsolutePath().normalize() }
        requireNoOverlappingDirectories(projectRoots)
        prepared.projects.forEach { project ->
            require(PROJECT_ID.matches(project.localProjectId)) { "Unsafe local project id" }
            requireStagedPath(project.stagedDirectory, workRoot, directory = true)
        }
        val assetFiles = prepared.assets.map { it.stagedFile.toAbsolutePath().normalize() }
        require(assetFiles.distinct().size == assetFiles.size) {
            "Prepared restore assets must not reuse the same staged file"
        }
        prepared.assets.forEach { asset ->
            require(ContentHash.isValid(asset.hash)) { "Invalid asset hash '${asset.hash}'" }
            requireStagedPath(asset.stagedFile, workRoot, directory = false)
            require(sha256(asset.stagedFile) == asset.hash) { "Staged asset hash mismatch for '${asset.hash}'" }
        }
        assetFiles.forEach { assetFile ->
            require(projectRoots.none { assetFile.startsWith(it) }) {
                "Prepared asset files must not live inside prepared project directories"
            }
        }
    }

    private fun requireStagedPath(path: Path, workRoot: Path, directory: Boolean) {
        val absolute = path.toAbsolutePath().normalize()
        require(absolute.startsWith(workRoot) && absolute != workRoot) {
            "Prepared restore content must be inside the restore work directory"
        }
        val hasExpectedType = if (directory) {
            Files.isDirectory(absolute, LinkOption.NOFOLLOW_LINKS)
        } else {
            Files.isRegularFile(absolute, LinkOption.NOFOLLOW_LINKS)
        }
        require(hasExpectedType) { "Prepared restore path has the wrong type: $path" }
    }

    private fun preflightLiveTargets(prepared: PreparedLibraryRestore) {
        prepared.projects.forEach { project ->
            require(!Files.exists(liveProjectsDir.resolve(project.localProjectId), LinkOption.NOFOLLOW_LINKS)) {
                "Project id collision for '${project.localProjectId}'"
            }
        }
        prepared.assets.forEach { asset ->
            val target = liveAssetsDir.resolve(asset.hash)
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                require(Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                    "Existing asset path is not a regular file for '${asset.hash}'"
                }
                require(sha256(target) == asset.hash) {
                    "Existing asset content does not match its hash '${asset.hash}'"
                }
            }
        }
    }

    private fun installAsset(asset: PreparedRestoreAsset) {
        val target = liveAssetsDir.resolve(asset.hash)
        if (Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) return
        atomicMoveNew(asset.stagedFile, target)
        fs.fsyncDir(liveAssetsDir)
    }

    private fun fsyncPrepared(prepared: PreparedLibraryRestore) {
        prepared.assets.forEach { fs.fsyncFile(it.stagedFile) }
        prepared.projects.forEach { project ->
            val paths = Files.walk(project.stagedDirectory).use { stream -> stream.toList() }
            paths.forEach { path ->
                require(!Files.isSymbolicLink(path)) { "Prepared project contains a symbolic link: $path" }
                require(
                    Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) ||
                        Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS),
                ) { "Prepared project contains an unsupported filesystem entry: $path" }
                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) fs.fsyncFile(path)
            }
            paths.asReversed().filter { Files.isDirectory(it, LinkOption.NOFOLLOW_LINKS) }.forEach(fs::fsyncDir)
        }
    }

    private fun atomicMoveNew(source: Path, target: Path) {
        require(!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) { "Restore target already exists: $target" }
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (failure: IOException) {
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                throw IOException("Restore target appeared during commit: $target", failure)
            }
            throw failure
        }
    }

    private fun writeJournal(prepared: PreparedLibraryRestore) {
        Files.createDirectories(restoreWorkDir)
        val bytes = encodeJournal(prepared).toByteArray(StandardCharsets.UTF_8)
        val temp = restoreWorkDir.resolve("$JOURNAL_NAME.tmp")
        try {
            Files.write(
                temp,
                bytes,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )
            fs.fsyncFile(temp)
            fs.atomicReplace(temp, journal)
            fs.fsyncDir(restoreWorkDir)
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private fun rollbackProjects(projectIds: List<String>): IOException? {
        var firstFailure: IOException? = null
        projectIds.forEach { projectId ->
            try {
                deleteTree(liveProjectsDir.resolve(projectId))
            } catch (failure: IOException) {
                if (firstFailure == null) firstFailure = failure else firstFailure?.addSuppressed(failure)
            }
        }
        try {
            fs.fsyncDir(liveProjectsDir)
        } catch (failure: IOException) {
            if (firstFailure == null) firstFailure = failure else firstFailure?.addSuppressed(failure)
        }
        return firstFailure
    }

    private fun requireNoOverlappingDirectories(directories: List<Path>) {
        directories.indices.forEach { leftIndex ->
            ((leftIndex + 1) until directories.size).forEach { rightIndex ->
                val left = directories[leftIndex]
                val right = directories[rightIndex]
                require(!left.startsWith(right) && !right.startsWith(left)) {
                    "Prepared project directories must not overlap"
                }
            }
        }
    }

    private fun deleteTree(root: Path) {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    private fun sha256(path: Path): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun encodeJournal(prepared: PreparedLibraryRestore): String = buildString {
        appendLine(JOURNAL_MAGIC)
        append("transaction=").appendLine(prepared.transactionId)
        prepared.projects.forEach { append("project=").appendLine(it.localProjectId) }
    }

    private fun decodeJournal(bytes: ByteArray): PendingRestore {
        val text = bytes.toString(StandardCharsets.UTF_8)
        val lines = text.lineSequence().filter { it.isNotEmpty() }.toList()
        require(lines.firstOrNull() == JOURNAL_MAGIC) { "Unsupported or corrupt restore journal" }
        val transactionLines = lines.drop(1).filter { it.startsWith("transaction=") }
        require(transactionLines.size == 1) { "Corrupt restore journal transaction" }
        val transactionId = transactionLines.single().removePrefix("transaction=")
        require(TRANSACTION_ID.matches(transactionId)) { "Unsafe restore journal transaction id" }
        require(lines.drop(1).all { it.startsWith("transaction=") || it.startsWith("project=") }) {
            "Corrupt restore journal field"
        }
        val projectIds = lines.drop(1).filter { it.startsWith("project=") }.map { it.removePrefix("project=") }
        require(projectIds.isNotEmpty() && projectIds.distinct().size == projectIds.size) {
            "Corrupt restore journal projects"
        }
        require(projectIds.all(PROJECT_ID::matches)) { "Unsafe project id in restore journal" }
        return PendingRestore(projectIds)
    }

    private data class PendingRestore(val projectIds: List<String>)

    private companion object {
        const val JOURNAL_NAME: String = "pending-library-restore.v1"
        const val JOURNAL_MAGIC: String = "ZINELY_LIBRARY_RESTORE_V1"
        const val BUFFER_BYTES: Int = 64 * 1024
        val TRANSACTION_ID: Regex = Regex("^[A-Za-z0-9_-]{1,64}$")
        val PROJECT_ID: Regex = Regex("^[A-Za-z0-9_-]{1,64}$")
    }
}
