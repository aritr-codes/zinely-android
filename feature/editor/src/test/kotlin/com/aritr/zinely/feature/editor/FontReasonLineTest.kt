package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.editor.FontReason
import com.aritr.zinely.core.model.DocumentVoice
import org.junit.Test
import org.junit.Assert.assertEquals

/** The word after "stays" is the font the text is in (A29H.7), not always Plain. */
class FontReasonLineTest {
    @Test
    fun `Given Greek in a Book text, then the line says it stays Book`() = assertEquals(
        "Hand has no Greek letters, so this text stays Book.",
        fontReasonLine(FontReason.LacksScript(listOf(DocumentVoice.HAND), DocumentVoice.BOOK, greek = true, cyrillic = false)),
    )

    @Test
    fun `Given Cyrillic in a Hand text, then the line says it stays Hand`() = assertEquals(
        "Book has no Cyrillic letters, so this text stays Hand.",
        fontReasonLine(FontReason.LacksScript(listOf(DocumentVoice.BOOK), DocumentVoice.HAND, greek = false, cyrillic = true)),
    )

    @Test
    fun `Given Greek in a Plain text, then both words are named and it stays Plain`() = assertEquals(
        "Book and Hand have no Greek letters, so this text stays Plain.",
        fontReasonLine(
            FontReason.LacksScript(listOf(DocumentVoice.BOOK, DocumentVoice.HAND), DocumentVoice.PLAIN, greek = true, cyrillic = false),
        ),
    )
}
