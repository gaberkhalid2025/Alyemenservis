package com.example.utils

import android.util.Log
import com.example.data.ActivityLogEntity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

/**
 * 📝 ActivityLogManager
 * الآلية المركزية الرسمية لتسجيل وتوثيق كافة العمليات الحساسة وإجراءات المسؤولين
 * وإعادة تعيين كلمات المرور في مجموعة activity_logs في Firestore.
 */
object ActivityLogManager {

    private const val TAG = "ActivityLogManager"
    private const val COLLECTION_NAME = "activity_logs"

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    /**
     * تسجيل عام لأي إجراء حساس في النظام
     */
    fun logAction(
        action: String,
        category: String = "GENERAL",
        performedBy: String = "ADMIN",
        target: String = "",
        details: String = "",
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        try {
            val docRef = db.collection(COLLECTION_NAME).document()
            val logEntity = ActivityLogEntity(
                id = docRef.id,
                action = action,
                timestamp = System.currentTimeMillis(),
                category = category,
                performedBy = performedBy,
                target = target,
                details = details
            )

            docRef.set(logEntity)
                .addOnSuccessListener {
                    Log.d(TAG, "Activity logged successfully: [${category}] $action")
                    onComplete?.invoke(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to log activity: ${e.message}", e)
                    onComplete?.invoke(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during activity logging: ${e.message}", e)
            onComplete?.invoke(false)
        }
    }

    /**
     * توثيق إجراءات إعادة تعيين أو استعادة كلمات المرور
     */
    fun logPasswordReset(
        targetPhone: String,
        entityType: String,
        performedBy: String = "ADMIN",
        isApproval: Boolean = false,
        details: String = ""
    ) {
        val actionText = if (isApproval) {
            "🔑 الموافقة على إعادة تعيين كلمة المرور ($entityType - $targetPhone)"
        } else {
            "🔑 تنفيذ إعادة تعيين كلمة المرور للحساب ($entityType - $targetPhone)"
        }
        logAction(
            action = actionText,
            category = "AUTH_PASSWORD_RESET",
            performedBy = performedBy,
            target = targetPhone,
            details = details
        )
    }

    /**
     * توثيق تقديم طلب استعادة الحساب وكلمة المرور
     */
    fun logPasswordRecoveryRequest(
        phone: String,
        name: String,
        accountType: String,
        channel: String = ""
    ) {
        logAction(
            action = "🔑 طلب استعادة كلمة المرور للحساب: $name ($phone - $accountType)",
            category = "PASSWORD_RECOVERY_REQUEST",
            performedBy = "USER",
            target = phone,
            details = "قناة التواصل أو الملاحظات: $channel"
        )
    }

    /**
     * توثيق العمليات الحساسة الخاصة بالإدارة
     */
    fun logAdminOperation(
        action: String,
        performedBy: String = "ADMIN",
        target: String = "",
        details: String = ""
    ) {
        logAction(
            action = action,
            category = "ADMIN_OPERATION",
            performedBy = performedBy,
            target = target,
            details = details
        )
    }

    /**
     * توثيق تقديم طلبات التوظيف
     */
    fun logJobApplication(
        applicantName: String,
        applicantPhone: String,
        jobTitle: String
    ) {
        logAction(
            action = "💼 تقديم طلب توظيف جديد: $applicantName لوظيفة ($jobTitle)",
            category = "JOB_APPLICATION",
            performedBy = "APPLICANT",
            target = applicantPhone,
            details = "المتقدم: $applicantName ($applicantPhone) - الوظيفة: $jobTitle"
        )
    }

    /**
     * توثيق مسح وتطهير قواعد البيانات
     */
    fun logDatabaseWipe(
        performedBy: String = "SUPER_ADMIN",
        collections: List<String>
    ) {
        logAction(
            action = "💥 مسح وتطهير بيانات قاعدة البيانات (${collections.size} مجموعات)",
            category = "DATABASE_WIPE",
            performedBy = performedBy,
            target = "FIRESTORE_DATABASE",
            details = "المجموعات الممسوحة: ${collections.joinToString(", ")}"
        )
    }
}
