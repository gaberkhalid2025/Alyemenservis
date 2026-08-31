package com.example.data.utils

sealed class AppError : Exception() {
    data class NetworkError(override val message: String = "خطأ في الاتصال بالإنترنت") : AppError()
    data class ValidationError(override val message: String) : AppError()
    data class NotFoundError(override val message: String = "العنصر غير موجود") : AppError()
    data class ConflictError(override val message: String = "هناك تعارض في البيانات") : AppError()
    data class UnauthorizedError(override val message: String = "غير مصرح لك بهذه العملية") : AppError()
    data class DatabaseError(override val message: String = "حدث خطأ في قاعدة البيانات") : AppError()
    data class UnknownError(override val message: String, val throwable: Throwable? = null) : AppError()
}
