package com.aritr.zinely.editor

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the [PhotoPicker] rendezvous lifecycle (ADR-031 §5, Codex RF2). Generic over `String`
 * so the concurrency rules are exercised without `android.net.Uri`. Given-When-Then.
 */
class PhotoPickerTest {

    @Test
    fun `await with no launcher bound resumes null immediately and never launches`() = runTest {
        val picker = PhotoPicker<String>()

        assertNull(picker.await())
    }

    @Test
    fun `a bound picker launches once and await returns the delivered result`() = runTest {
        val picker = PhotoPicker<String>()
        var launches = 0
        picker.bind { launches++ }

        val pick = async { picker.await() }
        runCurrent() // let await() register its continuation + invoke onLaunch before we deliver
        picker.deliver("photo://1")

        assertEquals("photo://1", pick.await())
        assertEquals(1, launches)
    }

    @Test
    fun `deliver null (user cancelled) resumes null`() = runTest {
        val picker = PhotoPicker<String>()
        picker.bind { }

        val pick = async { picker.await() }
        runCurrent()
        picker.deliver(null)

        assertNull(pick.await())
    }

    @Test
    fun `single-flight — a second await while one is pending resumes null without disturbing the first`() = runTest {
        val picker = PhotoPicker<String>()
        var launches = 0
        picker.bind { launches++ }

        val first = async { picker.await() }
        runCurrent() // first registers as the single pending pick
        val second = async { picker.await() }
        runCurrent()

        assertNull(second.await()) // the second is rejected immediately
        picker.deliver("photo://A")
        assertEquals("photo://A", first.await())
        assertEquals(1, launches) // only the first pick launched the picker
    }

    @Test
    fun `unbind keeps an in-flight pick so a redelivered result (rotation) still resolves it`() = runTest {
        // Inc-2b Codex RF2: ordinary composition disposal must NOT cancel a pick — the VM survives and
        // ActivityResultRegistry redelivers to the re-bound launcher.
        val picker = PhotoPicker<String>()
        picker.bind { }

        val pick = async { picker.await() }
        runCurrent()
        picker.unbind()              // composition disposed (e.g. rotation) — pick stays in flight
        picker.bind { }              // re-composed host re-binds a fresh launcher
        picker.deliver("after-rotation")

        assertEquals("after-rotation", pick.await())
    }

    @Test
    fun `cancelling one await never clears a newer await's pending`() = runTest {
        // Inc-2b Codex RF1: A's cancellation handler must be identity-guarded so it can't clear B.
        val picker = PhotoPicker<String>()
        picker.bind { }

        val first = async { picker.await() }
        runCurrent()
        first.cancel()               // A cancelled — clears only A's pending
        runCurrent()
        val second = async { picker.await() }
        runCurrent()
        picker.deliver("B")          // must reach B, not be dropped

        assertEquals("B", second.await())
    }

    @Test
    fun `after unbind a fresh await is not-bound and resumes null`() = runTest {
        val picker = PhotoPicker<String>()
        picker.bind { }
        picker.unbind()

        assertNull(picker.await())
    }

    @Test
    fun `a delivered result after the pending was already cleared is dropped (no double-resume)`() = runTest {
        val picker = PhotoPicker<String>()
        picker.bind { }

        val pick = async { picker.await() }
        runCurrent()
        picker.deliver("first")
        assertEquals("first", pick.await())

        // A stray late deliver (no pending) must be a no-op, not crash.
        picker.deliver("late")
    }
}
