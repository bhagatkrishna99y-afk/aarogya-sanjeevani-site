package com.aarogya.sanjeevani.core.common.result

import com.aarogya.sanjeevani.core.common.error.AppError

/**
 * The one return type every repository/use-case in this project uses
 * instead of throwing. Deliberately not Kotlin's own `Result<T>`: that
 * type's failure case is `Throwable`, which pushes every caller back into
 * stringly-typed `when (exception)` checks -- `AppError` (see
 * [com.aarogya.sanjeevani.core.common.error.AppError]) is a closed,
 * already-categorized hierarchy, so a ViewModel can exhaustively `when`
 * on it without knowing anything about Supabase/Ktor's own exception
 * types.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onError(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> =
    when (this) {
        is AppResult.Success -> AppResult.Success(transform(data))
        is AppResult.Failure -> this
    }

fun <T> T.asSuccess(): AppResult<T> = AppResult.Success(this)

fun AppError.asError(): AppResult<Nothing> = AppResult.Failure(this)
