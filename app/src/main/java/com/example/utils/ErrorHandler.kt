package com.example.utils

import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import java.io.IOException
import java.io.FileNotFoundException

/**
 * 🛠️ ErrorHandler - فئة مركزية لمعالجة وتراجم الأخطاء للغة العربية بالتعامل مع Firebase و Room والشبكة
 */
object ErrorHandler {

    /**
     * يقوم بتحليل أي استثناء (Throwable) وتحويله إلى [RequestError] آمن ومترجم
     */
    fun handle(throwable: Throwable): RequestError {
        return when (throwable) {
            is RequestError -> throwable
            is IOException -> {
                if (throwable is FileNotFoundException) {
                    RequestError.Disk(throwable)
                } else {
                    RequestError.Network(throwable)
                }
            }
            is FirebaseFirestoreException -> {
                val message = when (throwable.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> "تم رفض الوصول! ليس لديك الصلاحية الكافية لإتمام هذه العملية."
                    FirebaseFirestoreException.Code.UNAVAILABLE -> "قاعدة البيانات غير متاحة حالياً، يرجى التحقق من جودة الإنترنت."
                    FirebaseFirestoreException.Code.ALREADY_EXISTS -> "البيانات التي تحاول تسجيلها موجودة بالفعل في النظام."
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> "انتهت المهلة المحددة للعملية، يرجى المحاولة مرة أخرى."
                    else -> throwable.localizedMessage ?: "حدث خطأ أثناء الاتصال بقاعدة البيانات السحابية."
                }
                RequestError.Server(throwable.code.value(), message)
            }
            is StorageException -> {
                val message = "فشل في رفع أو تحميل الملفات: " + when (throwable.errorCode) {
                    StorageException.ERROR_NOT_AUTHORIZED -> "غير مصرح لك برفع هذه الملفات."
                    StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> "تجاوزت مهلة المحاولة لرفع الصور."
                    else -> throwable.localizedMessage ?: "فشل الاتصال بمخدم التخزين السحابي."
                }
                RequestError.Server(throwable.errorCode, message)
            }
            is FirebaseException -> {
                RequestError.Server(500, throwable.localizedMessage ?: "حدث خطأ في خدمات التحقق والتوثيق السحابية.")
            }
            is android.database.sqlite.SQLiteException -> {
                RequestError.Disk(throwable)
            }
            else -> {
                RequestError.Unknown(throwable)
            }
        }
    }

    /**
     * يستخرج الرسالة المترجمة المناسبة مباشرة لعرضها للمستخدم في واجهات التغذية الراجعة
     */
    fun getLocalizedMessage(throwable: Throwable): String {
        return handle(throwable).message
    }
}
