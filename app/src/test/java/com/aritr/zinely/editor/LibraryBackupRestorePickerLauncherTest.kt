package com.aritr.zinely.editor

import com.aritr.zinely.feature.library.LibraryBackupRestoreMode
import com.aritr.zinely.home.LibraryBackupRestorePickerRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryBackupRestorePickerLauncherTest {
    @Test
    fun `backup launcher failure is reported with backup mode`() {
        var failedMode: LibraryBackupRestoreMode? = null

        launchBackupRestorePicker(
            request = LibraryBackupRestorePickerRequest.Backup("my-zines.zine"),
            launchBackup = { throw IllegalStateException("picker unavailable") },
            launchRestore = { error("restore launcher must not run") },
            onFailure = { failedMode = it },
        )

        assertEquals(LibraryBackupRestoreMode.Backup, failedMode)
    }

    @Test
    fun `restore request launches the broad provider-compatible filter`() {
        var requestedTypes: Array<String>? = null
        var failed = false

        launchBackupRestorePicker(
            request = LibraryBackupRestorePickerRequest.Restore,
            launchBackup = { error("backup launcher must not run") },
            launchRestore = { requestedTypes = it },
            onFailure = { failed = true },
        )

        assertTrue(requestedTypes?.contentEquals(arrayOf("*/*")) == true)
        assertTrue(!failed)
    }
}
