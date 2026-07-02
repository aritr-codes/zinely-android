package com.aritr.zinely.data.android

import java.nio.file.Path

/**
 * The single project-path resolution chokepoint for everything under `rootDir/projects/<id>`
 * (ADR-042; extracted from [DocumentRepositoryImpl] so the document store and the project store
 * share one whitelist/containment rule that cannot drift — Codex R8). A project id is untrusted:
 * whitelist match, normalise, containment check. No project path is ever built outside this class.
 */
internal class ProjectPaths(rootDir: Path) {

    /** `rootDir/projects`, normalised once so the containment check is a cheap prefix test. */
    val projectsRoot: Path = rootDir.resolve(PROJECTS_DIR).normalize()

    /**
     * The directory for [projectId], or `null` when the id is not on the safe whitelist or would
     * resolve outside [projectsRoot].
     */
    fun projectDir(projectId: String): Path? {
        if (!PROJECT_ID.matches(projectId)) return null
        val resolved = projectsRoot.resolve(projectId).normalize()
        if (!resolved.startsWith(projectsRoot) || resolved == projectsRoot) return null
        return resolved
    }

    /** The project's document file (the content source of truth, ADR-003), or `null` on unsafe ids. */
    fun documentFile(projectId: String): Path? = projectDir(projectId)?.resolve(DOCUMENT_FILE)

    /** The project's shelf-metadata sidecar (title/createdAt authority, ADR-042), or `null` on unsafe ids. */
    fun metaFile(projectId: String): Path? = projectDir(projectId)?.resolve(META_FILE)

    internal companion object {
        const val PROJECTS_DIR = "projects"
        const val DOCUMENT_FILE = "document.json"
        const val META_FILE = "meta.json"

        /** Untrusted-id whitelist: excludes `.`, `/`, `\`, so traversal sequences cannot form. */
        val PROJECT_ID = Regex("^[A-Za-z0-9_-]{1,64}$")
    }
}
