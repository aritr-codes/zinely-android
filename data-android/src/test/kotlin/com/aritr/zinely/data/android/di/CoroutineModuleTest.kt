package com.aritr.zinely.data.android.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.coroutines.ContinuationInterceptor

/**
 * Guards the two load-bearing properties of the `@AutosaveScope` provider (PR-A Step 7, design §4 /
 * invariant B1) on plain JVM — without the Hilt runtime (which needs an Android Application; the
 * project forbids Robolectric). Hilt only *resolves* this binding; whether the produced scope is
 * shaped correctly is logic worth asserting directly.
 */
class CoroutineModuleTest {

    @Test
    fun `io dispatcher binding is Dispatchers IO`() {
        assertSame(Dispatchers.IO, CoroutineModule.provideIoDispatcher())
    }

    @Test
    fun `autosave scope carries a Job and the io dispatcher`() {
        val io: CoroutineDispatcher = CoroutineModule.provideIoDispatcher()

        val scope = CoroutineModule.provideAutosaveScope(io)

        // A Job must be present: AutosaveCoordinatorFactory does requireNotNull(scope.context[Job]).
        assertNotNull("autosave scope must carry a Job (B1)", scope.coroutineContext[Job])
        // The dispatcher must be IO: the binder launches lifecycle/teardown flushes directly on this
        // scope, so a non-IO scope would run autosave writes off the IO pool (B1).
        assertSame(io, scope.coroutineContext[ContinuationInterceptor])
    }
}
