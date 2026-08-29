package com.example.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 🚨 RequestError - Sealed class representing standardized network & system errors
 */
sealed class RequestError {
    object NetworkError : RequestError()
    object StorageError : RequestError()
    object PermissionError : RequestError()
    data class Unknown(val message: String) : RequestError()
}

/**
 * 🛠️ ErrorHandler - Centralized exception translator for user-facing Arabic messages
 */
object ErrorHandler {
    /**
     * Translates throwables into clear, localized user error messages
     */
    fun handle(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> "تعذر الاتصال بالشبكة. يرجى التحقق من اتصال الإنترنت."
            is SocketTimeoutException -> "انتهت مهلة الاتصال. يرجى المحاولة مرة أخرى."
            is IOException -> "حدث خطأ في الاتصال بالخادم. يرجى التثبت والمحاولة مجدداً."
            else -> throwable.message ?: "حدث خطأ غير متوقع."
        }
    }
}
