package com.aritr.zinely.core.data.validation

import com.aritr.zinely.core.model.ZineDocument

/**
 * Validates a [ZineDocument] against the structural invariants the schema cannot express in types
 * (page counts, unique ids, finite/positive geometry, normalised crops, in-range colours). Pure and
 * deterministic — no I/O — so it is unit-tested like the imposition core. The export pipeline (S5)
 * and editor (S4) gate on [ValidationResult.isValid].
 */
public interface DocumentValidator {
    public fun validate(document: ZineDocument): ValidationResult
}
