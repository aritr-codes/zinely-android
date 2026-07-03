package com.aritr.zinely.data.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The narrow public seam over [ProjectPaths] (S6.4, ADR-045): it must resolve exactly the path the
 * stores use and refuse exactly the ids they refuse — one chokepoint, no drift.
 */
class ProjectDocumentLayoutTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun givenSafeId_whenResolved_thenPointsAtTheStoresDocumentFile() {
        // Given the production layout over a root dir
        val root = temp.root.toPath()
        val layout = projectDocumentLayout(root)

        // When a safe id is resolved
        val resolved = layout.documentFile("default")

        // Then it is the same document.json the stores read and write
        assertEquals(root.resolve("projects").resolve("default").resolve("document.json"), resolved)
    }

    @Test
    fun givenTraversalOrUnsafeIds_whenResolved_thenNull() {
        val layout = projectDocumentLayout(temp.root.toPath())

        assertNull(layout.documentFile("../escape"))
        assertNull(layout.documentFile("a/b"))
        assertNull(layout.documentFile(""))
        assertNull(layout.documentFile("."))
    }
}
