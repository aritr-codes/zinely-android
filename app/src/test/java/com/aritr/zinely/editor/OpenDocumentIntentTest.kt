package com.aritr.zinely.editor

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OpenDocumentIntentTest {
    @Test
    fun `saved pdf opens with its exact uri mime and a scoped read grant`() {
        val uri = Uri.parse("content://com.aritr.zinely.fileprovider/downloaded_exports/zine.pdf")
        val intent = openDocumentIntent(uri, "application/pdf")

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(uri, intent.data)
        assertEquals("application/pdf", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
