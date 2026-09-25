package com.aritr.zinely

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Makes the privacy promise mechanical (1.x plan §4 F4): the merged manifest requests exactly this
 * permission set — in particular no `INTERNET`, so no library can open a socket — and opts out of
 * Android backup.
 *
 * **Pinned to SDK 28, and the pin is load-bearing.** Robolectric reads the manifest through the real
 * `PackageParser`, which drops a `maxSdkVersion="28"` permission when the running SDK is higher. At 28
 * `WRITE_EXTERNAL_STORAGE` is still requested, and its presence proves the pin still reaches the
 * pre-scoped-storage behaviour; `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, merged in from androidx.core,
 * proves the *merged* manifest was read. Never "fix" a failure here by deleting either entry.
 * The SDK-34 case checks the same manifest as a current device sees it, where neither storage
 * permission exists, so a permission added without a `maxSdkVersion` cannot hide behind the split
 * `READ_EXTERNAL_STORAGE` that SDK 28 reports.
 *
 * Unit tests see the **debug** merged manifest, which adds test-only components; only permissions and
 * `allowBackup` are asserted, never the component list. The declared set was re-derived from
 * `processReleaseManifest` on 2026-09-25 (the debug set matches it). A new permission is a reviewed
 * decision, never a quiet edit to these lists.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PrivacyManifestTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun mergedManifestRequestsExactlyTheReviewedPermissions() {
        assertEquals(
            sortedSetOf(
                // Not in any manifest: the platform parser adds it as a split permission of
                // WRITE_EXTERNAL_STORAGE, so an Android 9 device reports it as requested too.
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.VIBRATE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "com.aritr.zinely.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
            ),
            requestedPermissions(),
        )
    }

    @Test
    @Config(sdk = [34])
    fun currentDevicesSeeNoStoragePermissionAtAll() {
        assertEquals(
            sortedSetOf(
                "android.permission.VIBRATE",
                "com.aritr.zinely.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
            ),
            requestedPermissions(),
        )
    }

    @Test
    fun applicationOptsOutOfAndroidBackup() {
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    private fun requestedPermissions() = context.packageManager
        .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        .requestedPermissions
        .orEmpty()
        .toSortedSet()
}
