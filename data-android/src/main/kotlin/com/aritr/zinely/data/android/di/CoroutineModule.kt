package com.aritr.zinely.data.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Coroutine infrastructure for the autosave stack (PR-A Step 7, design §4/§5). Wiring only — adds no
 * behavior; the dispatcher and scope are the exact collaborators the frozen
 * [AutosaveCoordinatorFactory][com.aritr.zinely.data.android.AutosaveCoordinatorFactory] and
 * [EditorAutosaveBinder][com.aritr.zinely.data.android.EditorAutosaveBinder] already expect.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object CoroutineModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * The application-lifetime autosave scope (ADR-026; design §4). Two properties are load-bearing,
     * not cosmetic:
     * - **A [SupervisorJob]** satisfies `AutosaveCoordinatorFactory`'s `requireNotNull(Job)` and keeps
     *   one project's failure from cancelling the app scope or sibling projects.
     * - **The IO dispatcher** is part of the context because `EditorAutosaveBinder` launches its
     *   lifecycle (ON_PAUSE/ON_STOP) and teardown flushes **directly** on this scope; a non-IO scope
     *   would run those flushes off the IO pool (e.g. on Default), violating the Step-6 threading
     *   contract. (The factory's per-project child scopes also set IO explicitly, but the binder's
     *   own launches inherit from here.)
     *
     * Process-lifetime: never explicitly cancelled. Per-project teardown is the binder's job
     * ([EditorAutosaveBinder.closeProject][com.aritr.zinely.data.android.EditorAutosaveBinder.closeProject]),
     * not cancellation of this scope.
     */
    @Provides
    @Singleton
    @AutosaveScope
    fun provideAutosaveScope(@IoDispatcher io: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + io + CoroutineName("autosave"))
}
