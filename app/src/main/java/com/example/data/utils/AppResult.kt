package com.example.data.utils

typealias AppResult<T> = Result<T>

inline fun <T, R> AppResult<T>.mapAppResult(transform: (T) -> R): AppResult<R> {
    return this.fold(
        onSuccess = { Result.success(transform(it)) },
        onFailure = { Result.failure(it) }
    )
}
