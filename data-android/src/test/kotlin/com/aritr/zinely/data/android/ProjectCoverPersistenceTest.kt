package com.aritr.zinely.data.android

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.aritr.zinely.core.data.repository.getOrNull
import com.aritr.zinely.core.data.storage.AtomicFileStore
import com.aritr.zinely.core.data.storage.FileSystemOps
import com.aritr.zinely.core.data.storage.NioFileSystemOps
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.data.android.di.DataModule
import com.aritr.zinely.data.android.room.MIGRATION_1_2
import com.aritr.zinely.data.android.room.ZinelyDatabase
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * B5 — the persisted cover ([ADR-086](docs/DECISIONS.md#adr-086)).
 *
 * Covers the two rulings that make a cover an **identity** rather than decoration:
 * [D-017](docs/design/V2-SPEC-DEFECTS.md#d-017-ruling) (assigned once at creation, persisted, never
 * derived from the title) and [D-026](docs/design/V2-SPEC-DEFECTS.md#d-026-ruling) (a duplicate draws
 * its own; a legacy zine is assigned one on first presentation and it is then persisted).
 *
 * **Wherever "persisted" is the claim, the assertion opens `meta.json`.** A cover that exists only in
 * the value a call returns is precisely the bug these rulings are about, and an assertion against the
 * return value cannot see it — it would pass on an implementation that re-draws on every launch.
 */
@RunWith(RobolectricTestRunner::class)
class ProjectCoverPersistenceTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var root: Path
    private lateinit var store: AtomicFileStore
    private lateinit var documents: DocumentRepositoryImpl
    private lateinit var db: ZinelyDatabase

    private var now = 1_000L
    private var nextId = 1

    @Before
    fun setUp() {
        root = tmp.root.toPath()
        store = AtomicFileStore()
        documents = DocumentRepositoryImpl(rootDir = root, store = store)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ZinelyDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun repo(
        store: AtomicFileStore = this.store,
        random: Random = Random(1),
    ): RoomProjectRepository = RoomProjectRepository(
        rootDir = root,
        dao = db.projectDao(),
        documents = documents,
        store = store,
        sessionGate = ProjectSessionGate { true },
        io = Dispatchers.Unconfined,
        clock = { now },
        newId = { "p${nextId++}" },
        random = random,
    )

    /** The sidecar as it actually sits on disk — the only evidence that counts for "persisted". */
    private fun metaOnDisk(id: String): ProjectMeta {
        val bytes = Files.readAllBytes(root.resolve("projects").resolve(id).resolve("meta.json"))
        return Json { ignoreUnknownKeys = true }
            .decodeFromString(ProjectMeta.serializer(), bytes.decodeToString())
    }

    private fun ProjectMeta.recipeOrNull(): ZineCoverRecipe? {
        val surface = ZineCoverSurface.entries.firstOrNull { it.name == coverSurface }
        val stamp = ZineCoverStamp.entries.firstOrNull { it.name == coverStamp }
        return if (surface != null && stamp != null) ZineCoverRecipe(surface, stamp) else null
    }

    /** Rewrite a project's sidecar the way a pre-B5 build left it: a title, a createdAt, no cover. */
    private fun makeLegacyOnDisk(id: String) {
        val metaFile = root.resolve("projects").resolve(id).resolve("meta.json")
        val title = metaOnDisk(id).title
        Files.write(metaFile, """{"title":"$title","createdAtEpochMs":$now}""".encodeToByteArray())
    }

    /**
     * Drop the derived index so the next read re-enters the reconcile scan — the path the backfill
     * lives on. This is what makes "assigned **once**" falsifiable: without it, reads after the first
     * are answered from the row that already carries the cover, and could not fail on an implementation
     * that re-draws every time it consults the sidecar.
     */
    private fun forgetIndex() = db.clearAllTables()

    private fun metaWriteFailingStore(): AtomicFileStore = AtomicFileStore(
        object : FileSystemOps by NioFileSystemOps {
            override fun atomicReplace(source: Path, replacing: Path) {
                if (replacing.fileName.toString() == "meta.json") throw IOException("meta write blocked")
                NioFileSystemOps.atomicReplace(source, replacing)
            }
        },
    )

    private suspend fun newProject(r: RoomProjectRepository, title: String) =
        r.createProject(title, ZineFormat.SINGLE_SHEET_8, PaperSize.A4).getOrNull()!!

    // ---- creation --------------------------------------------------------------------------------

    @Test
    fun `a new project persists its assigned cover in the same write as its title`() = runTest {
        val created = newProject(repo(), "Sunday market")

        assertNotNull("the created project carries no cover", created.cover)
        assertEquals(
            "the cover exists in the returned value but not on disk",
            created.cover,
            metaOnDisk(created.id).recipeOrNull(),
        )
    }

    @Test
    fun `two projects created with the same title receive independently drawn covers`() = runTest {
        // D-017's content is about INFORMATION FLOW: the title must not reach the cover by any path.
        // A signature or reflection check cannot decide that (B1's guard was rejected for exactly this
        // reason), so the claim is made behaviourally — identical titles, different draws.
        val r = repo(random = Random(7))
        val a = newProject(r, "Same name")
        val b = newProject(r, "Same name")

        assertNotEquals(
            "identical titles produced identical covers — the title is reaching the assigner",
            metaOnDisk(a.id).recipeOrNull(),
            metaOnDisk(b.id).recipeOrNull(),
        )
    }

    // ---- rename ----------------------------------------------------------------------------------

    @Test
    fun `renaming a project does not change its cover`() = runTest {
        // D-017: "a physical object should retain its identity across renames". renameProject rewrites
        // the sidecar WHOLESALE, so any field not carried across is destroyed — a silent repaint
        // disguised as an unrelated edit.
        val r = repo()
        val id = newProject(r, "Before").id
        val before = metaOnDisk(id).recipeOrNull()

        r.renameProject(id, "After")

        assertEquals("After", metaOnDisk(id).title)
        assertEquals("the rename repainted the zine", before, metaOnDisk(id).recipeOrNull())
    }

    // ---- duplication -----------------------------------------------------------------------------

    @Test
    fun `a duplicate draws its own cover instead of inheriting the source's`() = runTest {
        // D-026: "duplicate content, not visual identity."
        val r = repo(random = Random(3))
        val source = newProject(r, "Riso tests")
        val copy = r.duplicateProject(source.id).getOrNull()!!

        assertNotNull("the duplicate carries no cover", metaOnDisk(copy.id).recipeOrNull())
        assertNotEquals(
            "the duplicate inherited its source's cover",
            metaOnDisk(source.id).recipeOrNull(),
            metaOnDisk(copy.id).recipeOrNull(),
        )
    }

    // ---- the legacy backfill ---------------------------------------------------------------------

    @Test
    fun `a legacy project is given a cover on first presentation and it is persisted`() = runTest {
        val id = newProject(repo(), "Old zine").id
        makeLegacyOnDisk(id)
        forgetIndex()
        assertNull("precondition: the sidecar carries no cover", metaOnDisk(id).recipeOrNull())

        val presented = repo().getProject(id).getOrNull()!!

        assertNotNull("a legacy project was presented without a cover", presented.cover)
        assertEquals(
            "the backfilled cover was not written back to the sidecar",
            presented.cover,
            metaOnDisk(id).recipeOrNull(),
        )
    }

    @Test
    fun `re-presenting a legacy project returns the cover it was first given`() = runTest {
        // The whole contract is the word ONCE. "Assign on first presentation" and "assign on every
        // presentation" agree on the first read and disagree on every one after it. Each re-read below
        // goes through a fresh repository with a DIFFERENT random seed and a forgotten index, so a
        // re-drawing implementation cannot coincidentally reproduce the first draw.
        val id = newProject(repo(), "Old zine").id
        makeLegacyOnDisk(id)
        forgetIndex()

        val first = repo(random = Random(11)).getProject(id).getOrNull()!!.cover
        forgetIndex()
        val second = repo(random = Random(29)).getProject(id).getOrNull()!!.cover
        forgetIndex()
        val third = repo(random = Random(97)).getProject(id).getOrNull()!!.cover

        assertNotNull("the first presentation assigned nothing", first)
        assertEquals("the backfill re-drew on the second presentation", first, second)
        assertEquals("the backfill re-drew on the third presentation", first, third)
        assertEquals("the sidecar drifted from what is being shown", first, metaOnDisk(id).recipeOrNull())
    }

    @Test
    fun `a backfill that cannot be persisted does not fabricate an identity`() = runTest {
        // Honest degradation: a cover held only in memory would differ on every launch while appearing
        // to satisfy D-017. Better visibly unassigned, and offered the backfill again next time.
        val id = newProject(repo(), "Old zine").id
        makeLegacyOnDisk(id)
        forgetIndex()

        val presented = repo(store = metaWriteFailingStore()).getProject(id).getOrNull()!!

        assertNull("a cover was invented despite the write failing", presented.cover)
        assertNull("the sidecar was mutated despite the write failing", metaOnDisk(id).recipeOrNull())
    }

    @Test
    fun `an adopted project whose sidecar cannot be written does not fabricate an identity`() = runTest {
        // The OTHER legacy door, and the one the first independent review found standing open: a project
        // with no sidecar at all — a pre-Room folder adopted by the reconcile scan, the S4 "default" seed
        // among them. `readMetaOrBackfill` builds the whole meta rather than patching a read one, so the
        // rule has to be repeated there: no cover unless the disk took it. The index is REBUILDABLE
        // (ADR-042), so a cover living only in the row is repainted on the next rebuild.
        val id = newProject(repo(), "Adopted zine").id
        Files.delete(root.resolve("projects").resolve(id).resolve("meta.json"))
        forgetIndex()

        val presented = repo(store = metaWriteFailingStore()).getProject(id).getOrNull()!!

        assertNull("a cover was invented for an adopted project the disk refused", presented.cover)
        assertFalse(
            "a sidecar appeared despite every write failing",
            Files.exists(root.resolve("projects").resolve(id).resolve("meta.json")),
        )
    }

    @Test
    fun `an adopted project is given a cover once and keeps it across rebuilds`() = runTest {
        // The healthy half of the same path: adoption assigns, and the assignment is on disk, so a later
        // rebuild with a different seed reproduces it rather than redrawing.
        val id = newProject(repo(), "Adopted zine").id
        Files.delete(root.resolve("projects").resolve(id).resolve("meta.json"))
        forgetIndex()

        val first = repo(random = Random(11)).getProject(id).getOrNull()!!.cover
        forgetIndex()
        val second = repo(random = Random(29)).getProject(id).getOrNull()!!.cover

        assertNotNull("adoption presented a project with no cover", first)
        assertEquals("adoption re-drew the cover on the second rebuild", first, second)
        assertEquals("the adopted cover never reached the sidecar", first, metaOnDisk(id).recipeOrNull())
    }

    @Test
    fun `an unreadable sidecar yields no cover rather than a different one each read`() = runTest {
        // Present-but-unreadable is never overwritten — the bytes stay for repair. That means the
        // assignment can never be persisted either, so drawing one here would draw a NEW one on every
        // single read: a zine that changes its face each time the shelf is shown. Two reads, two seeds.
        val id = newProject(repo(), "Corrupt zine").id
        val metaFile = root.resolve("projects").resolve(id).resolve("meta.json")
        Files.write(metaFile, "{ not json".encodeToByteArray())
        forgetIndex()

        val first = repo(random = Random(11)).getProject(id).getOrNull()!!.cover
        forgetIndex()
        val second = repo(random = Random(29)).getProject(id).getOrNull()!!.cover

        assertNull("a cover was invented over bytes that cannot hold it", first)
        assertNull("a cover was invented over bytes that cannot hold it", second)
        assertEquals(
            "the unreadable sidecar was overwritten instead of left for repair",
            "{ not json",
            Files.readAllBytes(metaFile).decodeToString(),
        )
    }

    // ---- the v1 → v2 migration -------------------------------------------------------------------

    @Test
    fun `a real v1 database opens at v2 through the migration with its rows intact`() {
        // No room-testing dependency, so MigrationTestHelper is unavailable — the v1 database is built
        // by hand from the CHECKED-IN v1 schema (schemas/…/1.json), room_master_table identity hash
        // included. That hash is what makes this a real test rather than a DDL rehearsal: Room verifies
        // the post-migration schema against 2.json and throws if MIGRATION_1_2's columns disagree with
        // the entity by so much as a type.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-1-to-2.db"
        seedV1Database(context, name)

        val migrated = Room.databaseBuilder(context, ZinelyDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            migrated.query("SELECT id, title, coverSurface, coverStamp FROM projects", null).use { c ->
                assertTrue("the migration lost the pre-existing row", c.moveToFirst())
                assertEquals("legacy-1", c.getString(0))
                assertEquals("Mum's garden", c.getString(1))
                // NULL is not an oversight — it is what marks the row legacy so D-026's backfill can
                // find it. A migration that invented covers here would assign identity outside the one
                // place assignment is allowed to happen.
                assertTrue("coverSurface should be NULL for a pre-existing row", c.isNull(2))
                assertTrue("coverStamp should be NULL for a pre-existing row", c.isNull(3))
                assertFalse("the migration duplicated the row", c.moveToNext())
            }
        } finally {
            migrated.close()
        }
    }

    @Test
    fun `the graph's own database carries the migration`() = runTest {
        // The migration above is tested through a builder this test writes. **The line that matters to an
        // installed device is in `DataModule`**, and deleting `.addMigrations(MIGRATION_1_2)` there is an
        // `IllegalStateException` on the first launch after update — invisible to every other assertion in
        // this package, which is why the mid-package review asked for this one. The provider is called
        // directly rather than through Hilt: the wiring under test is one line of it.
        val context = ApplicationProvider.getApplicationContext<Context>()
        seedV1Database(context, PRODUCTION_DB)

        val database = DataModule.provideZinelyDatabase(context)
        try {
            assertEquals(
                "the graph's database could not open a v1 file — the migration is not wired",
                1,
                database.projectDao().ids().size,
            )
        } finally {
            database.close()
        }
    }

    /**
     * A real v1 database at [name] holding one row — schema, indices and `room_master_table` identity
     * hash exactly as `schemas/…/1.json` records them, which is what makes Room actually validate the
     * migration rather than merely run its SQL.
     */
    private fun seedV1Database(context: Context, name: String) {
        context.getDatabasePath(name).also { it.parentFile?.mkdirs(); it.delete() }
        val v1 = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(V1_PROJECTS_TABLE)
                        db.execSQL(V1_PROJECTS_INDEX)
                        db.execSQL(V1_MASTER_TABLE)
                        db.execSQL(V1_MASTER_ROW)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        v1.writableDatabase.use {
            it.execSQL("INSERT INTO projects VALUES ('legacy-1','Mum''s garden','SINGLE_SHEET_8','A4',10,20,1)")
        }
        v1.close()
    }

    private companion object {
        /** The name `DataModule` opens (ADR-042) — this test has to seed *that* file, not another. */
        const val PRODUCTION_DB = "zinely.db"

        /** Verbatim from `data-android/schemas/…ZinelyDatabase/1.json`, with `TABLE_NAME` resolved. */
        const val V1_PROJECTS_TABLE =
            "CREATE TABLE IF NOT EXISTS `projects` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`format` TEXT NOT NULL, `paperSize` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, " +
                "`updatedAtEpochMs` INTEGER NOT NULL, `documentSchemaVersion` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        const val V1_PROJECTS_INDEX =
            "CREATE INDEX IF NOT EXISTS `index_projects_updatedAtEpochMs` ON `projects` (`updatedAtEpochMs`)"
        const val V1_MASTER_TABLE =
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"
        const val V1_MASTER_ROW =
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) " +
                "VALUES(42, '0ae63f65a47c48903e197a9da3c846f1')"
    }
}
