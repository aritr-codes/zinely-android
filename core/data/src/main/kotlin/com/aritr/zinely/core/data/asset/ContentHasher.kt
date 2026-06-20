package com.aritr.zinely.core.data.asset

import java.security.MessageDigest

/**
 * Pure content-addressing: the sha256 of a byte payload. Uses the JDK `MessageDigest` (available on
 * both the JVM and Android — this is **not** an Android API), so it stays in the pure data core and
 * is used to dedupe assets on import and to verify blob integrity on `.zine` restore ([ADR-022]).
 */
public object ContentHasher {

    /** The [ContentHash] of [bytes] (the asset's import-master bytes). */
    public fun sha256(bytes: ByteArray): ContentHash {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return ContentHash.of(digest.toHex())
    }

    private fun ByteArray.toHex(): String {
        val out = StringBuilder(size * 2)
        for (byte in this) {
            val value = byte.toInt() and 0xFF
            out.append(HEX[value ushr 4]).append(HEX[value and 0x0F])
        }
        return out.toString()
    }

    private const val HEX = "0123456789abcdef"
}
