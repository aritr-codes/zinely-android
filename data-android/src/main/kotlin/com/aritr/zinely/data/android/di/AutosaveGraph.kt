package com.aritr.zinely.data.android.di

import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.data.android.AutosaveCoordinatorFactory
import com.aritr.zinely.data.android.SaveFailureSink
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

/**
 * Compile-time validation surface for the Step 7 graph (design §13.2). Enumerating the key bindings
 * forces each into the `SingletonComponent` graph, so the Android-SDK CI gate
 * (`:app:compileDebugKotlin` with `ZINELY_CORE_ONLY` unset, design §13.1) fails the build if any is
 * unsatisfiable. It notably anchors [EditorAutosaveBinderFactory], which has no production injection
 * site yet, and the `@AutosaveScope` scope (proving that qualified binding resolves).
 *
 * It is also the single resolution handle for the supplemental, device-only graph smoke test (§13.3),
 * via `EntryPointAccessors.fromApplication(context, AutosaveGraph::class.java)`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
public interface AutosaveGraph {
    public fun documentRepository(): DocumentRepository

    public fun saveFailureSink(): SaveFailureSink

    public fun coordinatorFactory(): AutosaveCoordinatorFactory

    public fun binderFactory(): EditorAutosaveBinderFactory

    @AutosaveScope
    public fun autosaveScope(): CoroutineScope
}
