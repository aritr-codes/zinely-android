package com.aritr.zinely.editor

import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.validation.ValidationIssue
import com.aritr.zinely.feature.editor.SaveErrorKind
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The app-side `DataError → SaveErrorKind` mapping (ADR-036). Pure JVM — proves only the probe-classified
 * [DataError.OutOfSpace] earns the storage-specific banner kind, and every other failure stays generic, so
 * the editor never shows the "low on storage" line for a non-storage fault.
 */
class SaveErrorMappingTest {

    @Test
    fun `out of space maps to the storage kind`() {
        assertEquals(SaveErrorKind.OutOfSpace, DataError.OutOfSpace("no space").toSaveErrorKind())
    }

    @Test
    fun `a generic io failure maps to the generic kind`() {
        assertEquals(SaveErrorKind.Generic, DataError.Io("io failed").toSaveErrorKind())
    }

    @Test
    fun `an unknown failure maps to the generic kind`() {
        assertEquals(SaveErrorKind.Generic, DataError.Unknown("unclassified").toSaveErrorKind())
    }

    @Test
    fun `a non-storage failure never claims the storage kind`() {
        // The honesty bar: no false "low on storage" for failures the probe did not classify as OutOfSpace.
        val nonStorage = listOf(
            DataError.NotFound("p"),
            DataError.Corrupt("bad"),
            DataError.Invalid(listOf<ValidationIssue>()),
            DataError.SchemaTooNew(documentVersion = 2, supportedVersion = 1),
        )
        nonStorage.forEach { assertEquals(SaveErrorKind.Generic, it.toSaveErrorKind()) }
    }
}
