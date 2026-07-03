package com.aritr.zinely.data.android

import java.nio.file.Path

/**
 * The one deliberately narrow public window onto [ProjectPaths] (S6.4, ADR-045): where a project's
 * `document.json` lives, and nothing else. The shelf-thumbnail producer in `:app` needs exactly
 * this — it stats the document's mtime as the thumbnail invalidation stamp — and re-deriving
 * `projects/<id>` strings outside the ADR-042 whitelist/containment chokepoint is the drift Codex
 * flagged there (R8). [ProjectPaths] itself stays internal; do not widen this surface.
 */
public fun interface ProjectDocumentLayout {

    /** The document file for [projectId], or `null` when the id is not a safe project id. */
    public fun documentFile(projectId: String): Path?
}

/** The production layout over the app-private [rootDir] (`filesDir`), shared with the stores. */
public fun projectDocumentLayout(rootDir: Path): ProjectDocumentLayout {
    val paths = ProjectPaths(rootDir)
    return ProjectDocumentLayout(paths::documentFile)
}
