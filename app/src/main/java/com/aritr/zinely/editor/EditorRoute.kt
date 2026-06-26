package com.aritr.zinely.editor

import kotlinx.serialization.Serializable

/**
 * The type-safe navigation route for the editor (ADR-030 §1). [projectId] is the document this host
 * opens; the MVP seeds a single fixed `"default"` project (no ProjectRepository yet, ADR-030 §4), but
 * the id is a real route argument — threaded into [EditorViewModel] via `SavedStateHandle.toRoute` —
 * so multi-project navigation is an additive change, not a rewrite.
 */
@Serializable
internal data class EditorRoute(val projectId: String)
