package com.example.utils

import java.io.IOException

/**
 * ⚠️ RequestError - فئة مختومة لتمثيل وتصنيف نطاقات الأخطاء المختلفة
 */
sealed class RequestError : Throwable() {
    data class Network(val exception: IOException) : RequestError()
    data class Server(val code: Int, val serverMessage: String) : RequestError()
    data class Disk(val exception: Exception) : RequestError()
    data class Validation(val fieldName: String, val validationMessage: String) : RequestError()
    data class Unknown(val originalThrowable: Throwable) : RequestError()

    override val message: String
        get() = when (this) {
            is Network -> "فشل في الاتصال بالإنترنت، يرجى التحقق من الاتصال بالشبكة وإعادة المحاولة."
            is Server -> "حدث خطأ في الخادم (رمز $code): $serverMessage"
            is Disk -> "فشل في حفظ أو قراءة البيانات محلياً: ${exception.localizedMessage ?: "خطأ في مساحة التخزين"}"
            is Validation -> "خطأ في مدخلات حقل '$fieldName': $validationMessage"
            is Unknown -> "عذراً، حدث خطأ غير متوقع: ${originalThrowable.localizedMessage ?: "يرجى المحاولة لاحقاً"}"
        }
}
