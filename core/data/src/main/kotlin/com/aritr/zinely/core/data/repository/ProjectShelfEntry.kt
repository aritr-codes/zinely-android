package com.aritr.zinely.core.data.repository

import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineCoverRecipe

/**
 * The Library shelf's view of one project.
 *
 * [ProjectSummary] remains the healthy, index-backed projection used by mutations and the older V1 card
 * path. The shelf needs one more state: a project whose authoritative `document.json` exists locally but
 * cannot currently be opened or duplicated safely.
 */
public sealed interface ProjectShelfEntry {
    public val id: String
    public val title: String
    public val paperSize: PaperSize?
    public val updatedAtEpochMs: Long
    public val cover: ZineCoverRecipe?

    /** A project whose authoritative document is readable right now. */
    public data class Available(val summary: ProjectSummary) : ProjectShelfEntry {
        override val id: String get() = summary.id
        override val title: String get() = summary.title
        override val paperSize: PaperSize get() = summary.paperSize
        override val updatedAtEpochMs: Long get() = summary.updatedAtEpochMs
        override val cover: ZineCoverRecipe? get() = summary.cover
    }

    /**
     * A project whose authoritative document is present but unavailable to this build.
     *
     * The shelf still needs identity, cover, and recency for the actions that remain safe (rename,
     * delete), but opening/share/duplicate must be disabled until the document can be read honestly.
     */
    public data class Unavailable(
        override val id: String,
        override val title: String,
        override val paperSize: PaperSize?,
        override val updatedAtEpochMs: Long,
        override val cover: ZineCoverRecipe?,
        val reason: ProjectUnavailableReason,
    ) : ProjectShelfEntry
}

public enum class ProjectUnavailableReason {
    CORRUPT,
    NEWER_APP_REQUIRED,
}
