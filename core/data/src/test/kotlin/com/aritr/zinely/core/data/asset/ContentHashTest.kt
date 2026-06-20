package com.aritr.zinely.core.data.asset

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Assets are content-addressed by the sha256 of their import-master bytes ([ADR-022]/[ADR-023]).
 * [ContentHash] is the validated identity; [ContentHasher] is the pure (JDK, not Android) hashing
 * used both to dedupe on import and to verify integrity on `.zine` restore.
 */
class ContentHashTest {

    @Test
    fun `a valid 64-char lowercase hex string is accepted`() {
        val hex = "a".repeat(64)
        assertTrue(ContentHash.isValid(hex))
        assertEquals(hex, ContentHash.of(hex).hex)
    }

    @Test
    fun `uppercase, wrong-length, or non-hex strings are rejected`() {
        assertFalse(ContentHash.isValid("A".repeat(64)))
        assertFalse(ContentHash.isValid("a".repeat(63)))
        assertFalse(ContentHash.isValid("a".repeat(65)))
        assertFalse(ContentHash.isValid("g".repeat(64)))
        assertNull(ContentHash.ofOrNull("not-a-hash"))
        assertThrows<IllegalArgumentException> { ContentHash.of("nope") }
    }

    @Test
    fun `sha256 matches known test vectors`() {
        // RFC/standard vectors for the empty input and "abc".
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            ContentHasher.sha256(ByteArray(0)).hex,
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            ContentHasher.sha256("abc".toByteArray(Charsets.UTF_8)).hex,
        )
    }

    @Test
    fun `identical bytes hash identically and different bytes differ`() {
        val a = ContentHasher.sha256("photo".toByteArray())
        val b = ContentHasher.sha256("photo".toByteArray())
        val c = ContentHasher.sha256("other".toByteArray())
        assertEquals(a, b)
        assertNotEquals(a, c)
    }
}
