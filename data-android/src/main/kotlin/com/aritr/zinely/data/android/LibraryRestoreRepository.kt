package com.aritr.zinely.data.android

import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.ProjectSummary
import java.nio.file.Path

/**
 * Android repository boundary for restoring one already-private `.zine` archive into the local
 * files-plus-Room library. SAF remains a transport adapter and is deliberately absent here.
 */
public interface LibraryRestoreRepository {
    public suspend fun restoreLibrary(archive: Path): DataResult<LibraryRestoreReceipt>
}

/** One restored project, including the source identity needed to explain collision remapping. */
public data class RestoredProject(
    val sourceProjectId: String,
    val project: ProjectSummary,
)

/** The complete, reconciled result of one additive library restore. */
public data class LibraryRestoreReceipt(
    val projects: List<RestoredProject>,
)
