package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.asset.MAX_BACKUP_ASSET_BYTES
import com.aritr.zinely.core.data.asset.MAX_BACKUP_DOCUMENT_BYTES
import com.aritr.zinely.core.data.asset.MAX_BACKUP_TOTAL_BYTES
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.storage.ZineBackupWritingException
import com.aritr.zinely.core.data.storage.ZineBackupWritingException.Reason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ADR-120: the latch's two interleavings, and how a private-archive writer failure is classified. */
class LibraryBackupFailureTest {

    @Test fun `done first - a later Cancel loses`() {
        val latch = OutcomeLatch()
        assertTrue(latch.markDone())
        assertFalse(latch.requestCancel())
    }

    @Test fun `cancel first - the finished work loses`() {
        val latch = OutcomeLatch()
        assertTrue(latch.requestCancel())
        assertFalse(latch.markDone())
    }

    @Test fun `a write failure on a disk that can't hold the archive is out of space`() {
        val error = backupWriteError(writing(Reason.IO_FAILURE), documentBytes = listOf(10_000), assetBytes = emptyList()) { 9_999 }
        assertTrue(error is DataError.OutOfSpace)
    }

    @Test fun `a write failure with room to spare is a retryable private I-O failure`() {
        val error = backupWriteError(writing(Reason.IO_FAILURE), documentBytes = listOf(10_000), assetBytes = emptyList()) { 10_000 }
        assertTrue(error is DataError.Io)
    }

    @Test fun `a probe that can't answer never invents a full disk`() {
        val error = backupWriteError(writing(Reason.IO_FAILURE), documentBytes = listOf(10_000), assetBytes = emptyList()) { throw SecurityException() }
        assertTrue(error is DataError.Io)
    }

    @Test fun `a writer limit is deterministic`() {
        val error = backupWriteError(writing(Reason.LIMIT_EXCEEDED), documentBytes = listOf(1), assetBytes = emptyList()) { 0 }
        assertTrue("a limit is never reported as space, whatever the disk", error is DataError.LimitExceeded)
    }

    @Test fun `every writer reason has its classification`() {
        val expected = mapOf(
            Reason.DESTINATION_EXISTS to DataError.Io::class,
            Reason.SOURCE_UNAVAILABLE to DataError.Io::class,
            Reason.INVALID_MANIFEST to DataError.Corrupt::class,
            Reason.SOURCE_MISMATCH to DataError.Corrupt::class,
            Reason.INTEGRITY_MISMATCH to DataError.Corrupt::class,
        )
        expected.forEach { (reason, kind) ->
            assertEquals(reason.name, kind, backupWriteError(writing(reason), documentBytes = listOf(1), assetBytes = emptyList()) { 0 }::class)
        }
    }

    @Test fun `a size limit the manifest validator enforces is a limit, not an unreadable zine`() {
        // The writer validates the manifest before its own limit checks, so these arrive as INVALID_MANIFEST.
        val cases = listOf(
            listOf(MAX_BACKUP_DOCUMENT_BYTES + 1) to emptyList(),
            listOf(1L) to listOf(MAX_BACKUP_ASSET_BYTES + 1),
            listOf(1L) to List(65) { MAX_BACKUP_ASSET_BYTES }, // each photo fits; the library doesn't
        )
        assertTrue(65 * MAX_BACKUP_ASSET_BYTES > MAX_BACKUP_TOTAL_BYTES)
        cases.forEach { (documents, assets) ->
            val error = backupWriteError(writing(Reason.INVALID_MANIFEST), documents, assets) { Long.MAX_VALUE }
            assertTrue(error.toString(), error is DataError.LimitExceeded)
        }
    }

    private fun writing(reason: Reason) = ZineBackupWritingException(reason, reason.name)
}
