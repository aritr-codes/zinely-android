package com.aritr.zinely.feature.library

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The three pure strings the V2.1 shelf prints: the heading's count, and the two halves the subtitle is
 * split into by [ZineOnShelf] — the postmark on the cover and the date under it.
 *
 * **This file exists because a review found the claim before the evidence.** [pluralZineCount],
 * [zineShelfStampLabel] and [zineShelfDateLabel] each carried a KDoc line saying they were pure *and
 * tested*; the first half was true and the second was not — nothing in the repository referenced any of
 * them. Two of the three then turned out to have a real defect at an input the corpus never shows: a
 * subtitle carrying no `·` printed an **empty postmark pill** on the cover, because `substringBefore`'s
 * missing-delimiter value defaulted to the whole string and was then trimmed to nothing by the split.
 * The prototype only ever renders `"A5 · 2 days ago"`, so no amount of looking at it would have found
 * that. Pure JVM, no Robolectric: these are `String` in, `String` out.
 */
class ZineShelfLabelsTest {

    // -----------------------------------------------------------------------------------------
    // The heading count
    // -----------------------------------------------------------------------------------------

    @Test
    fun `the count pill pluralises, including the one case the corpus never renders`() {
        assertEquals("1 zine", pluralZineCount(1))
        assertEquals("6 zines", pluralZineCount(6)) // the frozen literal, `v21-library.html`
        assertEquals("0 zines", pluralZineCount(0))
        assertEquals("2 zines", pluralZineCount(2))
    }

    // -----------------------------------------------------------------------------------------
    // The subtitle split
    // -----------------------------------------------------------------------------------------

    @Test
    fun `the frozen subtitle splits into a postmark and a date`() {
        assertEquals("A5", zineShelfStampLabel("A5 · 2 days ago"))
        assertEquals("2 days ago", zineShelfDateLabel("A5 · 2 days ago"))
    }

    @Test
    fun `a subtitle with no separator stamps nothing and dates everything`() {
        // The defect: the stamp half must be *empty*, so the caller's `isNotBlank()` guard suppresses the
        // pill entirely rather than printing a blank one. The date half keeps the whole string, because
        // an unseparated subtitle is a date with no format, not a format with no date.
        assertEquals("", zineShelfStampLabel("just now"))
        assertEquals("just now", zineShelfDateLabel("just now"))
    }

    @Test
    fun `both halves are trimmed, and neither is confused by a second separator`() {
        assertEquals("A4", zineShelfStampLabel("  A4  ·  today  "))
        assertEquals("today", zineShelfDateLabel("  A4  ·  today  "))
        // `substringAfter` takes the *first* separator, so a date containing one survives intact.
        assertEquals("A5", zineShelfStampLabel("A5 · 3 · 4 weeks"))
        assertEquals("3 · 4 weeks", zineShelfDateLabel("A5 · 3 · 4 weeks"))
    }

    @Test
    fun `an empty subtitle prints nothing at all`() {
        assertEquals("", zineShelfStampLabel(""))
        assertEquals("", zineShelfDateLabel(""))
    }
}
