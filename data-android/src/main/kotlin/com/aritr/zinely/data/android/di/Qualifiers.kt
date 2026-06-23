package com.aritr.zinely.data.android.di

import javax.inject.Qualifier

/**
 * The IO [kotlinx.coroutines.CoroutineDispatcher] for autosave/storage work (PR-A Step 7, design §3).
 * Qualified because `CoroutineDispatcher` is a framework type with several legitimate future bindings
 * (Default/Main); an unqualified binding would be ambiguous.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class IoDispatcher

/**
 * The application-lifetime autosave [kotlinx.coroutines.CoroutineScope] (PR-A Step 7, design §3/§4;
 * ADR-026). Qualified for the same reason as [IoDispatcher]: `CoroutineScope` is a framework type with
 * potentially several bindings. This scope **must** carry the IO dispatcher (see [CoroutineModule]).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class AutosaveScope
