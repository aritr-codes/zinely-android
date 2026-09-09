package com.aritr.zinely.core.data.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RestoreProjectIdAllocatorTest {
    @Test
    fun `free source ids are retained and collisions mint unique local ids`() {
        val minted = ArrayDeque(listOf("already-there", "fresh-id"))

        val allocated = RestoreProjectIdAllocator.allocate(
            sourceProjectIds = listOf("free", "collision"),
            existingProjectIds = setOf("collision", "already-there"),
            mintId = minted::removeFirst,
        )

        assertEquals(listOf("free", "fresh-id"), allocated)
    }

    @Test
    fun `repeated restore allocates new ids without overwriting the first restore`() {
        var suffix = 0
        val first = RestoreProjectIdAllocator.allocate(listOf("one", "two"), emptySet()) { error("not used") }
        val second = RestoreProjectIdAllocator.allocate(listOf("one", "two"), first.toSet()) {
            "copy-${++suffix}"
        }

        assertEquals(listOf("one", "two"), first)
        assertEquals(listOf("copy-1", "copy-2"), second)
    }

    @Test
    fun `duplicate source ids fail before allocation`() {
        assertThrows(IllegalArgumentException::class.java) {
            RestoreProjectIdAllocator.allocate(listOf("same", "same"), emptySet()) { "unused" }
        }
    }
}
