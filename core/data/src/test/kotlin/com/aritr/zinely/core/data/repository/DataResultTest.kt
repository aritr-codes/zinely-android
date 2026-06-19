package com.aritr.zinely.core.data.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `DataResult<T>` is the sealed success/failure boundary every repository returns (S2 spike §1,
 * ARCHITECTURE §9). These tests pin the combinator behaviour that callers (ViewModels, S4) rely on.
 */
class DataResultTest {

    private val ok: DataResult<Int> = DataResult.Success(2)
    private val err: DataResult<Int> = DataResult.Failure(DataError.NotFound("p1"))

    @Test
    fun `getOrNull returns the value on success and null on failure`() {
        assertEquals(2, ok.getOrNull())
        assertNull(err.getOrNull())
    }

    @Test
    fun `errorOrNull returns the error on failure and null on success`() {
        assertEquals(DataError.NotFound("p1"), err.errorOrNull())
        assertNull(ok.errorOrNull())
    }

    @Test
    fun `isSuccess and isFailure reflect the variant`() {
        assertTrue(ok.isSuccess)
        assertFalse(ok.isFailure)
        assertTrue(err.isFailure)
        assertFalse(err.isSuccess)
    }

    @Test
    fun `map transforms success and passes failure through unchanged`() {
        assertEquals(DataResult.Success(4), ok.map { it * 2 })
        assertEquals(err, err.map { it * 2 })
    }

    @Test
    fun `fold dispatches to the matching branch`() {
        assertEquals("v2", ok.fold(onSuccess = { "v$it" }, onFailure = { "e" }))
        assertEquals("e", err.fold(onSuccess = { "v$it" }, onFailure = { "e" }))
    }

    @Test
    fun `getOrElse returns the fallback only on failure`() {
        assertEquals(2, ok.getOrElse { -1 })
        assertEquals(-1, err.getOrElse { -1 })
    }

    @Test
    fun `onSuccess and onFailure run only for their variant and return the receiver`() {
        var seen = 0
        assertEquals(ok, ok.onSuccess { seen = it }.onFailure { seen = -100 })
        assertEquals(2, seen)
        var errSeen: DataError? = null
        err.onSuccess { errSeen = null }.onFailure { errSeen = it }
        assertEquals(DataError.NotFound("p1"), errSeen)
    }
}
