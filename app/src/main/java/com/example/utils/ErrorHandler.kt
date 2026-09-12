package com.example.utils

import android.content.Context
import android.widget.Toast

object ErrorHandler {

    fun getLocalizedMessage(throwable: Throwable, defaultMessage: String = "حدث خطأ غير متوقع"): String {
        return when (throwable) {
            is java.net.UnknownHostException -> "تعذر الاتصال بالشبكة. يرجى التحقق من اتصال الإنترنت لديك."
            is java.net.SocketTimeoutException -> "انتهت مهلة الاتصال بالإنترنت. يرجى المحاولة مرة أخرى."
            is java.io.IOException -> "تعذر الاتصال بالإنترنت أو قراءة البيانات. يرجى إعادة المحاولة."
            else -> throwable.localizedMessage ?: defaultMessage
        }
    }

    fun handleError(
        context: Context,
        throwable: Throwable,
        defaultMessage: String = "حدث خطأ غير متوقع"
    ) {
        val message = getLocalizedMessage(throwable, defaultMessage)
        Toast.makeText(context, "⚠️ $defaultMessage: $message", Toast.LENGTH_SHORT).show()
        AppErrorLogManager.logFirestoreError("ErrorHandler", defaultMessage, throwable)
    }

    fun handleError(
        error: AppError,
        onShowSnackbar: (String) -> Unit,
        onLogError: (String) -> Unit = {}
    ) {
        val message = error.messageArabic
        onShowSnackbar(message)
        onLogError("[$error] $message - ${error.userActionArabic}")
    }
}
