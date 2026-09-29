package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.asset.ZineBackupOmission
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.ProjectSummary
import java.nio.file.Path

/**
 * Android repository boundary for restoring one already-private `.zine` archive into the local
 * files-plus-Room library. SAF remains a transport adapter and is deliberately absent here.
 */
public interface LibraryRestoreRepository {
    /**
     * [onCommitStart] runs inside the lock immediately before the non-cancellable commit (ADR-122 R2). `false` means a
     * Cancel already won: nothing is committed and the restore ends as cancelled. After `true` it can't be stopped. A
     * restore that adds nothing never calls it.
     */
    public suspend fun restoreLibrary(
        archive: Path,
        onCommitStart: () -> Boolean = { true },
    ): DataResult<LibraryRestoreReceipt>
}

/** One restored project, including the source identity needed to explain collision remapping. */
public data class RestoredProject(
    val sourceProjectId: String,
    val project: ProjectSummary,
)

/**
 * The result of one additive library restore (ADR-121, ADR-122 R3). Once the commit succeeds this is a success even if
 * the shelf index lags: [addedCount] is known from the files, while [projects] holds only the zines the index already
 * shows, and [shelfUpToDate] is `false` until it catches up. [alreadyHereCount] zines were identical to shelf zines and
 * were not added. [omitted] is what the archive says its backup was saved without, clamped for display.
 */
public data class LibraryRestoreReceipt(
    val projects: List<RestoredProject>,
    val addedCount: Int = projects.size,
    val alreadyHereCount: Int = 0,
    val shelfUpToDate: Boolean = true,
    val omitted: List<ZineBackupOmission> = emptyList(),
)

/**
 * ADR-122 §5: the archive's `omitted` list is untrusted display text. Each entry still counts; only the first
 * [MAX_NAMED_OMISSIONS] keep a title, cut to [MAX_OMISSION_TITLE_CHARS] characters without splitting a character.
 */
internal fun clampedOmissions(omitted: List<ZineBackupOmission>): List<ZineBackupOmission> =
    omitted.mapIndexed { index, omission ->
        val title = omission.title?.takeIf { index < MAX_NAMED_OMISSIONS }?.let { title ->
            if (title.length <= MAX_OMISSION_TITLE_CHARS) {
                title
            } else {
                val cut = MAX_OMISSION_TITLE_CHARS - if (title[MAX_OMISSION_TITLE_CHARS - 1].isHighSurrogate()) 1 else 0
                title.substring(0, cut)
            }
        }
        ZineBackupOmission(title, ZineBackupOmission.normalizedReason(omission.reason))
    }

internal const val MAX_NAMED_OMISSIONS: Int = 50
internal const val MAX_OMISSION_TITLE_CHARS: Int = 120
