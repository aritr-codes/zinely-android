package com.aritr.zinely.core.data.asset

/**
 * The validated identity of a content-addressed asset: the lowercase-hex sha256 of its import-master
 * bytes ([ADR-022]/[ADR-023]). The on-disk blob lives at `assets/<hex>`; the same photo placed twice
 * yields one [ContentHash] and one file (dedupe).
 */
@JvmInline
public value class ContentHash private constructor(public val hex: String) {
    override fun toString(): String = hex

    public companion object {
        private val PATTERN = Regex("^[0-9a-f]{64}$")

        /** True if [hex] is a 64-character lowercase-hex sha256 string. */
        public fun isValid(hex: String): Boolean = PATTERN.matches(hex)

        /** Wrap [hex], requiring it to be a valid sha256 hex string. */
        public fun of(hex: String): ContentHash {
            require(isValid(hex)) { "Not a sha256 hex content hash: '$hex'" }
            return ContentHash(hex)
        }

        /** Wrap [hex] if valid, else `null`. */
        public fun ofOrNull(hex: String): ContentHash? = if (isValid(hex)) ContentHash(hex) else null
    }
}
