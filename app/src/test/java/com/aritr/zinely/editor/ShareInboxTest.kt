package com.aritr.zinely.editor

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import com.aritr.zinely.MainActivity
import com.aritr.zinely.core.copy.Copy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Share-in ([ShareInbox], ADR-105 / SUPPLIES-SPEC §6). Given-When-Then.
 *
 * The acceptance rule is exercised **pure** (over `String`, no `android.net.Uri` — the same trick
 * [PhotoPickerTest] uses), and the thin `Intent` read is exercised under Robolectric because that is where
 * the two `EXTRA_STREAM` shapes actually differ.
 */
@RunWith(RobolectricTestRunner::class)
class ShareInboxTest {

    // ---- the pure acceptance rule -------------------------------------------------------------

    @Test
    fun `a single-image share is accepted`() {
        assertEquals(
            listOf("photo://1"),
            acceptedShareIn(Intent.ACTION_SEND, "image/jpeg", listOf("photo://1")),
        )
    }

    @Test
    fun `a multi-image share keeps every uri in order`() {
        assertEquals(
            listOf("photo://1", "photo://2", "photo://3"),
            acceptedShareIn(
                Intent.ACTION_SEND_MULTIPLE,
                "image/*",
                listOf("photo://1", "photo://2", "photo://3"),
            ),
        )
    }

    @Test
    fun `a non-image share is refused even though the manifest filter says image only`() {
        // A filter routes implicit intents; an explicit intent reaches an exported Activity regardless.
        assertTrue(acceptedShareIn(Intent.ACTION_SEND, "application/pdf", listOf("doc://1")).isEmpty())
    }

    @Test
    fun `a wildcard type is accepted — gallery apps send multi-image shares under it`() {
        assertEquals(
            listOf("photo://1"),
            acceptedShareIn(Intent.ACTION_SEND_MULTIPLE, "*/*", listOf("photo://1")),
        )
    }

    @Test
    fun `only content uris are readable from a share`() {
        assertTrue(isReadableShareScheme("content"))
        // A sender handing back one of Zinely's OWN private files would otherwise be copied into a zine
        // and could then leave the device through the export path.
        assertFalse(isReadableShareScheme("file"))
        assertFalse(isReadableShareScheme("https"))
        assertFalse(isReadableShareScheme(null))
    }

    @Test
    fun `a share with no declared type is refused`() {
        assertTrue(acceptedShareIn(Intent.ACTION_SEND, null, listOf("photo://1")).isEmpty())
    }

    @Test
    fun `the launcher intent yields nothing`() {
        assertTrue(acceptedShareIn(Intent.ACTION_MAIN, "image/jpeg", listOf("photo://1")).isEmpty())
    }

    @Test
    fun `a text-only share carries no uri and yields nothing`() {
        assertTrue(acceptedShareIn(Intent.ACTION_SEND, "image/jpeg", listOf<String?>(null)).isEmpty())
    }

    // ---- the Intent seam ----------------------------------------------------------------------

    @Test
    fun `ACTION_SEND reads the single EXTRA_STREAM parcelable`() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, Uri.parse("content://media/1"))
        }

        assertEquals(listOf(Uri.parse("content://media/1")), intent.sharedImageUris())
        assertFalse(intent.isUnsupportedShare())
    }

    @Test
    fun `ACTION_SEND_MULTIPLE reads the EXTRA_STREAM list`() {
        val uris = arrayListOf(Uri.parse("content://media/1"), Uri.parse("content://media/2"))
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }

        assertEquals(uris.toList(), intent.sharedImageUris())
    }

    @Test
    fun `a shared PDF is an unsupported share, not a silent no-op`() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, Uri.parse("content://docs/1"))
        }

        assertTrue(intent.sharedImageUris().isEmpty())
        assertTrue(intent.isUnsupportedShare())
    }

    @Test
    fun `the launcher intent is not an unsupported share`() {
        assertFalse(Intent(Intent.ACTION_MAIN).isUnsupportedShare())
    }

    @Test
    fun `a non-Uri parcelable in EXTRA_STREAM is dropped, not cast`() {
        // Below API 33 the only accessor is the erased generic one, so a sender can put any Parcelable
        // here and hand us a List<Uri> the compiler believes and the runtime does not. Unfiltered, the
        // ClassCastException surfaces inside the import coroutine as a crash.
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, Bundle())
        }

        assertTrue(intent.sharedImageUris().isEmpty())
    }

    @Test
    fun `a file uri handed back by a sender is refused`() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///data/user/0/com.aritr.zinely/files/x.json"))
        }

        assertTrue(intent.sharedImageUris().isEmpty())
    }

    // ---- the inbox ----------------------------------------------------------------------------

    @Test
    fun `takeAll drains the inbox exactly once`() {
        val inbox = ShareInbox()
        inbox.offer(listOf(Uri.parse("content://media/1")))

        assertEquals(1, inbox.takeAll().size)
        assertTrue(inbox.takeAll().isEmpty())
    }

    @Test
    fun `a second share appends and never drops the first`() {
        val inbox = ShareInbox()
        inbox.offer(listOf(Uri.parse("content://media/1")))
        inbox.offer(listOf(Uri.parse("content://media/2")))

        assertEquals(2, inbox.takeAll().size)
    }

    @Test
    fun `with no editor collecting there is no open zine`() {
        assertFalse(ShareInbox().hasOpenZine)
    }

    // ---- the manifest, which is the other half of the feature -----------------------------------

    @Test
    fun `MainActivity is task-unique, because a share sheet launches with NEW_TASK`() {
        // The one assertion that would have caught the defect the device found. It cannot *reproduce* it:
        // Robolectric does not simulate the ActivityManager task stack, so there is no second task and no
        // second Activity instance to model here — the failure itself is device-only, permanently.
        //
        // What is checkable is the declaration. `singleTop` shipped first and is WRONG: the system share
        // sheet launches its target with FLAG_ACTIVITY_NEW_TASK, and `singleTop` only collapses a relaunch
        // onto the SAME task, so a real Gallery share opened a second MainActivity in a second task. Both
        // instances share one process and therefore one @Singleton AutosaveCoordinatorFactory, so the
        // second one's editor could never take the ADR-026 single-writer slot the first still held: the
        // zine the photo was shared into answered "That zine is still saving" and never opened again.
        //
        // Until this assert existed, the manifest comment was the only thing defending the attribute, and
        // comments do not run.
        val ctx = RuntimeEnvironment.getApplication()
        val info = ctx.packageManager.getActivityInfo(ComponentName(ctx, MainActivity::class.java), 0)

        assertEquals(
            "MainActivity must be task-unique or a share sheet gets its own task and its own editor",
            ActivityInfo.LAUNCH_SINGLE_TASK,
            info.launchMode,
        )
    }

    // ---- the announcement ---------------------------------------------------------------------

    @Test
    fun `the import summary reports both halves and never collapses a partial import`() {
        assertEquals("Photo added.", Copy.ShareIn.importSummary(added = 1, failed = 0))
        assertEquals("3 photos added.", Copy.ShareIn.importSummary(added = 3, failed = 0))
        assertEquals("One photo couldn’t be added.", Copy.ShareIn.importSummary(added = 0, failed = 1))
        assertEquals("2 photos couldn’t be added.", Copy.ShareIn.importSummary(added = 0, failed = 2))
        assertEquals(
            "2 photos added. One photo couldn’t be added.",
            Copy.ShareIn.importSummary(added = 2, failed = 1),
        )
    }
}
