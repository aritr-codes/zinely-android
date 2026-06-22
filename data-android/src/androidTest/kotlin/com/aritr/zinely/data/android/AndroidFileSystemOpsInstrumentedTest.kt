package com.aritr.zinely.data.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aritr.zinely.core.data.storage.AtomicFileStore
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Real-device durability checks for [AndroidFileSystemOps] (ADR-025 / ADR-026). These exercise the
 * actual `android.system.Os` directory fsync + atomic rename on **app-private internal storage**,
 * which the JVM unit test cannot (it stubs the syscall behind [DirFsync]).
 *
 * **CI limitation:** the current CI has no emulator/device and gates `:data-android` out via
 * `ZINELY_CORE_ONLY`, so this suite does not run there. It runs locally / on any future
 * emulator-backed CI via `connectedDebugAndroidTest`. The fail-closed *policy* is what protects
 * durability, and that is covered without a device by `AndroidFileSystemOpsTest`; this suite proves
 * the real syscall actually fsyncs (and fails closed) on a genuine app-private filesystem.
 */
@RunWith(AndroidJUnit4::class)
class AndroidFileSystemOpsInstrumentedTest {

    private lateinit var root: Path
    private val ops = AndroidFileSystemOps() // production wiring: real Os.fsync

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // App-private internal storage — the only scope these guarantees are claimed for.
        root = Files.createTempDirectory(context.filesDir.toPath(), "fs-it")
    }

    @Test
    fun realDirectoryFsyncOnAppPrivateStorageSucceeds() {
        // Must not throw on a real, valid app-private directory.
        ops.fsyncDir(root)
    }

    @Test
    fun atomicWriteThroughStoreRoundTripsWithRealOs() {
        val store = AtomicFileStore(ops)
        val target = root.resolve("doc.json")

        store.write(target, "real zine".toByteArray())

        assertArrayEquals("real zine".toByteArray(), store.read(target))
    }

    @Test
    fun directoryFsyncFailsClosedOnAnInvalidDirectory() {
        // A path that is not a directory must surface as IOException (the real Errno, e.g. ENOTDIR),
        // proving the production path fails closed rather than swallowing a durability failure.
        val notADir = Files.write(root.resolve("a-file"), "x".toByteArray())

        val threw = try {
            ops.fsyncDir(notADir)
            false
        } catch (_: IOException) {
            true
        }

        assertTrue("fsyncDir must fail closed on a non-directory path", threw)
    }
}
