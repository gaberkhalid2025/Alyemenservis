package com.example.data.utils

sealed class AppError : Exception() {
    data class NetworkError(val messageString: String = "خطأ في الاتصال بالشبكة") : AppError() {
        override val message: String get() = messageString
    }
    data class ValidationError(override val message: String) : AppError() {
        constructor(field: String, messageString: String) : this("$field: $messageString")
    }
    data class NotFoundError(override val message: String) : AppError()
    data class ConflictError(override val message: String) : AppError()
    data class UnauthorizedError(override val message: String) : AppError()
    data class DatabaseError(override val message: String) : AppError()
    data class UnknownError(override val message: String) : AppError()
}
