package com.aritr.zinely.core.data.repository

/**
 * The sealed success/failure boundary every repository returns (S2 spike §1, ARCHITECTURE §9).
 * Repositories map platform exceptions (`IOException`, `SQLiteException`, `SerializationException`)
 * to a [DataError] at this boundary, so no raw exception leaks to ViewModels.
 */
public sealed interface DataResult<out T> {
    public data class Success<out T>(val value: T) : DataResult<T>
    public data class Failure(val error: DataError) : DataResult<Nothing>

    public val isSuccess: Boolean get() = this is Success
    public val isFailure: Boolean get() = this is Failure
}

/** The value on success, or `null` on failure. */
public fun <T> DataResult<T>.getOrNull(): T? = when (this) {
    is DataResult.Success -> value
    is DataResult.Failure -> null
}

/** The error on failure, or `null` on success. */
public fun <T> DataResult<T>.errorOrNull(): DataError? = when (this) {
    is DataResult.Success -> null
    is DataResult.Failure -> error
}

/** The value on success, or the result of [onFailure] (given the error) on failure. */
public inline fun <T> DataResult<T>.getOrElse(onFailure: (DataError) -> T): T = when (this) {
    is DataResult.Success -> value
    is DataResult.Failure -> onFailure(error)
}

/** Map the success value; a failure passes through unchanged. */
public inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(value))
    is DataResult.Failure -> this
}

/** Collapse both variants to a single [R]. */
public inline fun <T, R> DataResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (DataError) -> R,
): R = when (this) {
    is DataResult.Success -> onSuccess(value)
    is DataResult.Failure -> onFailure(error)
}

/** Run [action] on success; returns the receiver for chaining. */
public inline fun <T> DataResult<T>.onSuccess(action: (T) -> Unit): DataResult<T> {
    if (this is DataResult.Success) action(value)
    return this
}

/** Run [action] on failure; returns the receiver for chaining. */
public inline fun <T> DataResult<T>.onFailure(action: (DataError) -> Unit): DataResult<T> {
    if (this is DataResult.Failure) action(error)
    return this
}
