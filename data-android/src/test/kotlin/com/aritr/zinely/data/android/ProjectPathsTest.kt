package com.aritr.zinely.data.android

import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shared path-resolution chokepoint (ADR-042, extracted per Codex R8). Pure `Path` logic — no
 * filesystem access — so a plain JVM test suffices. Traversal coverage mirrors
 * [DocumentRepositoryImplTest]'s untrusted-id cases so the two stores can never drift.
 */
class ProjectPathsTest {

    private val paths = ProjectPaths(Path.of("root"))

    @Test
    fun `given a safe id, when resolving, then all paths land inside the projects root`() {
        // Given
        val id = "default"

        // When
        val dir = paths.projectDir(id)
        val document = paths.documentFile(id)
        val meta = paths.metaFile(id)

        // Then
        assertTrue(dir!!.startsWith(paths.projectsRoot))
        assertEquals(dir.resolve("document.json"), document)
        assertEquals(dir.resolve("meta.json"), meta)
    }

    @Test
    fun `given traversal or unsafe ids, when resolving, then resolution refuses with null`() {
        // Given — everything off the [A-Za-z0-9_-]{1,64} whitelist
        val unsafe = listOf("..", ".", "a/b", "a\\b", "", "x.y", "a".repeat(65), "nul:", "a b")

        for (id in unsafe) {
            // When / Then
            assertNull("expected '$id' to be refused", paths.projectDir(id))
            assertNull("expected '$id' to be refused", paths.documentFile(id))
            assertNull("expected '$id' to be refused", paths.metaFile(id))
        }
    }

    @Test
    fun `given a 64-char id, when resolving, then it is accepted at the whitelist boundary`() {
        // Given
        val id = "a".repeat(64)

        // When / Then
        assertTrue(paths.projectDir(id)!!.startsWith(paths.projectsRoot))
    }
}
