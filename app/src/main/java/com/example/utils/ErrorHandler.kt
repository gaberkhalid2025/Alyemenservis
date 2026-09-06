package com.example.utils

import android.content.Context
import android.widget.Toast

object ErrorHandler {
    fun handleError(
        context: Context,
        throwable: Throwable,
        defaultMessage: String = "حدث خطأ غير متوقع"
    ) {
        val message = throwable.localizedMessage ?: defaultMessage
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
