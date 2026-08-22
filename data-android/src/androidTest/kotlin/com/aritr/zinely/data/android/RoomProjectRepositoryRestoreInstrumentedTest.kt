package com.aritr.zinely.data.android

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aritr.zinely.core.data.asset.AssetEntry
import com.aritr.zinely.core.data.asset.CURRENT_LIBRARY_BACKUP_VERSION
import com.aritr.zinely.core.data.asset.LIBRARY_BACKUP_KIND
import com.aritr.zinely.core.data.asset.ZineBackupProjectEntry
import com.aritr.zinely.core.data.asset.ZineLibraryBackupManifest
import com.aritr.zinely.core.data.repository.getOrNull
import com.aritr.zinely.core.data.serialization.JsonDocumentSerializer
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.data.android.room.ZinelyDatabase
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device proof for the repository restore boundary. The JVM suite owns hostile-input coverage; this
 * test exercises the successful transaction through real app-private storage, real directory
 * fsync/atomic moves, and Room on Android.
 */
@RunWith(AndroidJUnit4::class)
class RoomProjectRepositoryRestoreInstrumentedTest {

    private lateinit var root: Path
    private lateinit var database: ZinelyDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        root = Files.createTempDirectory(context.filesDir.toPath(), "restore-it")
        database = Room.inMemoryDatabaseBuilder(context, ZinelyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun safTransportBacksUpAndRestoresThroughRealContentResolverStreams() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val fileSystem = AndroidFileSystemOps()
        val store = AtomicFileStore(fileSystem)
        val documents = DocumentRepositoryImpl(rootDir = root, store = store)
        var nextId = 0
        val repository = RoomProjectRepository(
            rootDir = root,
            dao = database.projectDao(),
            documents = documents,
            store = store,
            sessionGate = ProjectSessionGate { true },
            libraryWriterGate = LibraryWriterGate { LibraryWriterLease {} },
            fs = fileSystem,
            io = Dispatchers.IO,
            newId = { "p${++nextId}" },
            appVersion = "device-test",
        )
        val original = repository.createProject(
            title = "Portable zine",
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.A4,
        ).getOrNull()!!
        val transport = ContentResolverLibrarySafTransport(
            transferRoot = context.cacheDir.toPath().resolve("zine-transfer-it"),
            streams = ContentResolverSafStreams(context.contentResolver),
            restoreRepository = repository,
            backupRepository = repository,
            io = Dispatchers.IO,
        )
        val destination = createDownloadsDocument("zinely-transport-${System.nanoTime()}.zine")

        try {
            val backup = transport.backupTo(destination).getOrNull()!!
            val restore = transport.restoreFrom(destination).getOrNull()!!

            assertEquals(1, backup.projectCount)
            assertTrue(readBytes(destination).isNotEmpty())
            assertEquals(listOf(original.id), restore.projects.map { it.sourceProjectId })
            assertEquals(listOf("p2"), restore.projects.map { it.project.id })
            assertEquals("Portable zine", restore.projects.single().project.title)
            assertTrue(Files.isRegularFile(root.resolve("projects/p2/document.json")))
            assertTrue(Files.isRegularFile(root.resolve("projects/p2/meta.json")))
        } finally {
            context.contentResolver.delete(destination, null, null)
        }
    }

    @After
    fun tearDown() {
        database.close()
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    @Test
    fun restoreCommitsProjectsAndSharedAssetThroughRealAndroidStorage() = runBlocking {
        val fileSystem = AndroidFileSystemOps()
        val store = AtomicFileStore(fileSystem)
        val documents = DocumentRepositoryImpl(rootDir = root, store = store)
        var nextId = 0
        val repository = RoomProjectRepository(
            rootDir = root,
            dao = database.projectDao(),
            documents = documents,
            store = store,
            sessionGate = ProjectSessionGate { true },
            libraryWriterGate = LibraryWriterGate { LibraryWriterLease {} },
            fs = fileSystem,
            io = Dispatchers.IO,
            newId = { "p${++nextId}" },
        )
        val existingId = repository.createProject(
            title = "Existing",
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
        ).getOrNull()!!.id
        val assetBytes = "device-shared-asset".encodeToByteArray()
        val assetHash = sha256(assetBytes)
        val archive = writeArchive(
            listOf(
                ProjectFixture(existingId, "Collision", document(assetHash)),
                ProjectFixture("incoming", "Incoming", document(assetHash)),
            ),
            assetHash,
            assetBytes,
        )

        val receipt = repository.restoreLibrary(archive).getOrNull()!!

        assertEquals(listOf(existingId, "incoming"), receipt.projects.map { it.sourceProjectId })
        assertEquals(listOf("p2", "incoming"), receipt.projects.map { it.project.id })
        assertEquals(document(assetHash), documents.load("p2").getOrNull())
        assertEquals(document(assetHash), documents.load("incoming").getOrNull())
        assertTrue(Files.isRegularFile(root.resolve("projects/p2/meta.json")))
        assertTrue(Files.isRegularFile(root.resolve("projects/incoming/meta.json")))
        assertTrue(Files.isRegularFile(root.resolve("assets").resolve(assetHash)))
        val assetCount = Files.list(root.resolve("assets")).use { paths ->
            paths.filter(Files::isRegularFile).count()
        }
        assertEquals(1L, assetCount)
    }

    private fun writeArchive(
        projects: List<ProjectFixture>,
        assetHash: String,
        assetBytes: ByteArray,
    ): Path {
        val serializer = JsonDocumentSerializer()
        val documents = projects.associate { project ->
            project.id to serializer.serialize(project.document).encodeToByteArray()
        }
        val manifest = ZineLibraryBackupManifest(
            packageVersion = CURRENT_LIBRARY_BACKUP_VERSION,
            kind = LIBRARY_BACKUP_KIND,
            appVersion = "device-test",
            createdAtEpochMs = 99L,
            projects = projects.map { project ->
                val bytes = documents.getValue(project.id)
                ZineBackupProjectEntry(
                    sourceProjectId = project.id,
                    title = project.title,
                    format = project.document.format,
                    paperSize = project.document.paperSize,
                    createdAtEpochMs = 10L,
                    updatedAtEpochMs = 20L,
                    documentSchemaVersion = project.document.schemaVersion,
                    documentPath = "projects/${project.id}/document.json",
                    documentSha256 = sha256(bytes),
                    documentByteCount = bytes.size.toLong(),
                    assetHashes = listOf(assetHash),
                    coverSurface = null,
                    coverStamp = null,
                )
            },
            assets = listOf(AssetEntry(assetHash, "image/jpeg", 32, 32, assetBytes.size.toLong())),
        )
        val archive = root.resolve("device-restore.zine")
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            writeEntry(
                zip,
                "manifest.json",
                Json.encodeToString(ZineLibraryBackupManifest.serializer(), manifest).encodeToByteArray(),
            )
            documents.forEach { (id, bytes) ->
                writeEntry(zip, "projects/$id/document.json", bytes)
            }
            writeEntry(zip, "assets/$assetHash", assetBytes)
        }
        return archive
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun document(assetHash: String): ZineDocument = ZineDocument(
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = PaperSize.LETTER,
        pages = (0 until 8).map { index ->
            Page(
                index = index,
                role = when (index) {
                    0 -> PageRole.FRONT_COVER
                    7 -> PageRole.BACK_COVER
                    else -> PageRole.INTERIOR
                },
                elements = if (index == 0) {
                    listOf(
                        ImageElement(
                            id = "image",
                            transform = Transform(0.0, 0.0, 100.0, 100.0),
                            assetId = assetHash,
                        ),
                    )
                } else {
                    emptyList()
                },
            )
        },
    )

    private fun sha256(bytes: ByteArray): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun createDownloadsDocument(displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
        }
        return requireNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
    }

    private fun readBytes(uri: Uri): ByteArray =
        requireNotNull(context.contentResolver.openInputStream(uri)).use { input -> input.readBytes() }

    private data class ProjectFixture(
        val id: String,
        val title: String,
        val document: ZineDocument,
    )
}
