package com.aritr.zinely.editor

import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.feature.editor.SaveErrorKind

/**
 * Map a data-layer [DataError] to the feature-local [SaveErrorKind] the editor banner keys its copy on
 * ([ADR-036](../../../../../../docs/DECISIONS.md)). Only [DataError.OutOfSpace] — the repository's
 * free-space-probe classification — earns the specific storage line; every other failure shows the
 * generic warm line. Keeping this mapping in `:app` (not `:feature:editor`) preserves the ADR-035
 * decoupling: the feature layer never depends on `:core:data`'s `DataError`.
 */
internal fun DataError.toSaveErrorKind(): SaveErrorKind = when (this) {
    is DataError.OutOfSpace -> SaveErrorKind.OutOfSpace
    else -> SaveErrorKind.Generic
}
