package com.aritr.zinely.feature.library

import com.aritr.zinely.feature.library.LibraryBackupRestoreUiState.BackupSaved
import com.aritr.zinely.feature.library.LibraryBackupRestoreUiState.RestoreAdded
import com.aritr.zinely.feature.library.LibraryOmissionReason.Newer
import com.aritr.zinely.feature.library.LibraryOmissionReason.Photo
import com.aritr.zinely.feature.library.LibraryOmissionReason.Unreadable
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The result sheets' words against the frozen `backup-restore.html` (amendment 1a items 5, 8–10; Amendment N). Every
 * expected string below is copied from the drawing; a derived plural the drawing doesn't show says so.
 */
class LibraryBackupRestoreCopyTest {

    private fun backup(
        saved: Int,
        total: Int,
        vararg omitted: LibraryOmission,
        offShelf: Set<LibraryOmissionReason> = emptySet(),
    ) = backupSavedCopy(BackupSaved(saved, assetCount = 0, totalCount = total, omitted = omitted.toList(), offShelfReasons = offShelf))

    private fun zine(title: String?, reason: LibraryOmissionReason = Unreadable) = LibraryOmission(title, reason)

    // The shipped title "Backup saved" predates this step and differs from the drawing's "Your backup is saved";
    // step 1b leaves it alone (known drift, BACKUP-RESTORE-FREEZE.md).
    @Test fun `a complete backup keeps the shipped saved state`() {
        assertEquals(
            ResultCopy("Backup saved", listOf("All 6 zines are together in one backup file.")),
            backup(6, 6),
        )
    }

    @Test fun `partial - one named`() {
        assertEquals(
            ResultCopy("5 of 6 zines saved", listOf("Zinely couldn’t open 1 zine, so it isn’t in this backup: “Moth Club Bulletin”.")),
            backup(5, 6, zine("Moth Club Bulletin")),
        )
    }

    @Test fun `partial - unnamed`() {
        assertEquals(
            listOf("Zinely couldn’t open 1 zine, so it isn’t in this backup. It has no readable name."),
            backup(5, 6, zine(null)).paragraphs,
        )
    }

    @Test fun `partial - many, three names then and N more, an unnamed one counted`() {
        assertEquals(
            listOf(
                "Zinely couldn’t open 5 zines, so they aren’t in this backup: “Sunday market”, “Letters home”, " +
                    "“Riso tests” and 2 more.",
            ),
            backup(1, 6, zine("Sunday market"), zine("Letters home"), zine("Riso tests"), zine("Night bus"), zine(null)).paragraphs,
        )
    }

    @Test fun `partial - off shelf closes the sentence`() {
        assertEquals(
            listOf(
                "Zinely couldn’t open 5 zines, so they aren’t in this backup: “Moth Club Bulletin”, “Night bus”, " +
                    "“Riso tests” and 2 more. Some of these aren’t on your shelf.",
            ),
            backup(
                4, 9,
                zine("Moth Club Bulletin"), zine("Night bus"), zine("Riso tests"), zine("A"), zine("B"),
                offShelf = setOf(Unreadable),
            ).paragraphs,
        )
    }

    @Test fun `off shelf is said only of the reason that has an off-shelf zine`() {
        assertEquals(
            listOf(
                "Zinely couldn’t open 1 zine, so it isn’t in this backup: “Letters home”. Some of these aren’t on your shelf.",
                "Zinely couldn’t read a photo in 1 zine, so that zine isn’t in this backup: “Moth Club Bulletin”.",
            ),
            backup(4, 6, zine("Letters home"), zine("Moth Club Bulletin", Photo), offShelf = setOf(Unreadable)).paragraphs,
        )
    }

    @Test fun `partial - newer`() {
        assertEquals(
            listOf(
                "“Moth Club Bulletin” was made by a newer Zinely, so it isn’t in this backup. Update Zinely, then back up again.",
            ),
            backup(5, 6, zine("Moth Club Bulletin", Newer)).paragraphs,
        )
    }

    @Test fun `partial - photo, one and shared`() {
        assertEquals(
            listOf("Zinely couldn’t read a photo in 1 zine, so that zine isn’t in this backup: “Moth Club Bulletin”."),
            backup(5, 6, zine("Moth Club Bulletin", Photo)).paragraphs,
        )
        assertEquals(
            listOf(
                "Zinely couldn’t read a photo in 2 zines, so those zines aren’t in this backup: “Moth Club Bulletin” and “Riso tests”.",
            ),
            backup(4, 6, zine("Moth Club Bulletin", Photo), zine("Riso tests", Photo)).paragraphs,
        )
    }

    @Test fun `partial - mixed reasons, one paragraph each in the frozen order`() {
        assertEquals(
            listOf(
                "Zinely couldn’t open 1 zine, so it isn’t in this backup: “Letters home”.",
                "Zinely couldn’t read a photo in 1 zine, so that zine isn’t in this backup: “Moth Club Bulletin”.",
            ),
            // Given out of order, drawn in order.
            backup(4, 6, zine("Moth Club Bulletin", Photo), zine("Letters home")).paragraphs,
        )
    }

    @Test fun `derived, not drawn - unnamed plurals and an unnamed newer zine`() {
        assertEquals(
            listOf(
                "Zinely couldn’t open 2 zines, so they aren’t in this backup. They have no readable names.",
                "1 zine was made by a newer Zinely, so it isn’t in this backup. Update Zinely, then back up again.",
            ),
            backup(3, 6, zine(null), zine(null), zine(null, Newer)).paragraphs,
        )
    }

    @Test fun `N2 all new keeps the frozen restored state`() {
        assertEquals(
            ResultCopy("7 zines added to your shelf", listOf("What was already on this shelf stayed put.")),
            restoreAddedCopy(RestoreAdded(7)),
        )
    }

    @Test fun `N3 some new, plural and singular`() {
        assertEquals(
            ResultCopy(
                "2 zines added to your shelf",
                listOf("The other 5 were already here, so they weren’t added again.", "What was already on this shelf stayed put."),
            ),
            restoreAddedCopy(RestoreAdded(2, alreadyHereCount = 5)),
        )
        assertEquals(
            listOf("The other zine was already here, so it wasn’t added again.", "What was already on this shelf stayed put."),
            restoreAddedCopy(RestoreAdded(6, alreadyHereCount = 1)).paragraphs,
        )
        assertEquals("1 zine added to your shelf", restoreAddedCopy(RestoreAdded(1, alreadyHereCount = 6)).title)
    }

    @Test fun `N4 nothing new, plural and singular, never stayed put`() {
        assertEquals(
            ResultCopy("Nothing new to add", listOf("The zines from this backup are already on your shelf.")),
            restoreAddedCopy(RestoreAdded(0, alreadyHereCount = 7)),
        )
        assertEquals(
            listOf("The zine from this backup is already on your shelf."),
            restoreAddedCopy(RestoreAdded(0, alreadyHereCount = 1)).paragraphs,
        )
    }

    @Test fun `N5 order - already here, the partial notice, then stayed put`() {
        assertEquals(
            listOf(
                "The other 5 were already here, so they weren’t added again.",
                "This backup was saved without 1 zine that couldn’t be opened then: “Moth Club Bulletin”.",
                "What was already on this shelf stayed put.",
            ),
            restoreAddedCopy(RestoreAdded(2, alreadyHereCount = 5, omitted = listOf(zine("Moth Club Bulletin")))).paragraphs,
        )
    }

    @Test fun `R3 lagging replaces stayed put, after already here`() {
        assertEquals(
            ResultCopy(
                "3 zines added to your shelf",
                listOf("They may take a moment to appear. If they don’t, close Zinely and open it again."),
            ),
            restoreAddedCopy(RestoreAdded(3, shelfUpToDate = false)),
        )
        assertEquals(
            listOf(
                "The other 5 were already here, so they weren’t added again.",
                "They may take a moment to appear. If they don’t, close Zinely and open it again.",
            ),
            restoreAddedCopy(RestoreAdded(2, alreadyHereCount = 5, shelfUpToDate = false)).paragraphs,
        )
    }

    @Test fun `nothing new from a partial backup - its notice, no stayed put`() {
        assertEquals(
            listOf(
                "The zines from this backup are already on your shelf.",
                "This backup was saved without 1 zine that couldn’t be opened then: “Moth Club Bulletin”.",
            ),
            restoreAddedCopy(RestoreAdded(0, alreadyHereCount = 7, omitted = listOf(zine("Moth Club Bulletin")))).paragraphs,
        )
    }

    @Test fun `restored partial - photo and unnamed, as drawn`() {
        assertEquals(
            listOf(
                "This backup was saved without 1 zine whose photo couldn’t be read then: “Moth Club Bulletin”.",
                "What was already on this shelf stayed put.",
            ),
            restoreAddedCopy(RestoreAdded(3, omitted = listOf(zine("Moth Club Bulletin", Photo)))).paragraphs,
        )
        assertEquals(
            listOf("This backup was saved without 1 zine that had no readable name.", "What was already on this shelf stayed put."),
            restoreAddedCopy(RestoreAdded(3, omitted = listOf(zine(null, Photo)))).paragraphs,
        )
    }

    @Test fun `derived, not drawn - a newer zine reads as couldn't be opened then, reasons share one paragraph`() {
        assertEquals(
            "This backup was saved without 2 zines that couldn’t be opened then: “Sunday market” and “Riso tests”. " +
                "This backup was saved without 1 zine that had no readable name.",
            restoreAddedCopy(
                RestoreAdded(3, omitted = listOf(zine("Sunday market"), zine("Riso tests", Newer), zine(null, Photo))),
            ).paragraphs.single { it.startsWith("This backup") },
        )
        // A reason with a name counts its unnamed zines into "and N more".
        assertEquals(
            "This backup was saved without 2 zines whose photos couldn’t be read then: “Moth Club Bulletin” and 1 more.",
            restoreAddedCopy(
                RestoreAdded(3, omitted = listOf(zine("Moth Club Bulletin", Photo), zine(null, Photo))),
            ).paragraphs.single { it.startsWith("This backup") },
        )
    }
}
