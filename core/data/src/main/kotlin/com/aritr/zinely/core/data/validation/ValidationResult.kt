package com.aritr.zinely.core.data.validation

/** Whether a validation issue blocks (ERROR) or merely informs (WARNING). */
public enum class Severity { ERROR, WARNING }

/**
 * A single validation finding. [code] is a stable machine-readable key (e.g. `pages.count.mismatch`)
 * safe to branch on or localise; [path] locates the offending node (e.g.
 * `pages[3].elements[1].transform`); [message] is a human-readable explanation.
 */
public data class ValidationIssue(
    val code: String,
    val message: String,
    val severity: Severity = Severity.ERROR,
    val path: String? = null,
)

/**
 * The structured result of validating a document — the concrete realisation of the `ValidationResult`
 * shape contemplated by [ADR-015]. Errors block (export/save gate on [isValid]); warnings inform.
 * Issues are emitted in stable document order so the result is deterministic and diffable.
 */
public data class ValidationResult(val issues: List<ValidationIssue> = emptyList()) {
    public val errors: List<ValidationIssue> get() = issues.filter { it.severity == Severity.ERROR }
    public val warnings: List<ValidationIssue> get() = issues.filter { it.severity == Severity.WARNING }

    /** A document is valid when it has no ERROR-severity issues; warnings are allowed. */
    public val isValid: Boolean get() = errors.isEmpty()

    public companion object {
        public val VALID: ValidationResult = ValidationResult(emptyList())
    }
}
