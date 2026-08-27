package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * 🔐 SecurityAuditAndMediaGuard - سجل التدقيق الأمني، حماية المعاملات المالية، وحارس وسائط المحادثات
 * 
 * المكونات:
 * 1. BookingStateEngine: آلة حالات الحجوزات الصارمة لمنع الانتقالات غير الشرعية.
 * 2. AuditTrailLogger: مسجل التدقيق الأمني في Firebase مع البصمة الرقمية للعمليات الحساسة.
 * 3. PaymentSecurityGuard: التحقق من صحة تفاصيل الدفع وأرقام الإشعارات والمبالغ.
 * 4. ChatMediaGuard: فحص وتطهير وضغط صور المحادثات لتقليل الاستهلاك وحماية الخصوصية.
 */

// ==========================================
// 1. 📅 Strict Booking State Machine
// ==========================================
object BookingStateEngine {
    const val STATUS_PENDING = "PENDING"
    const val STATUS_APPROVED = "APPROVED"
    const val STATUS_IN_PROGRESS = "IN_PROGRESS"
    const val STATUS_COMPLETED = "COMPLETED"
    const val STATUS_REJECTED = "REJECTED"
    const val STATUS_CANCELLED = "CANCELLED"

    /**
     * التحقق مما إذا كان الانتقال بين حالتي الحجز مسموحاً ومنطقياً
     * @param currentStatus الحالة الحالية
     * @param newStatus الحالة المراد الانتقال إليها
     * @return true إذا كان الانتقال صالحاً
     */
    fun isValidTransition(currentStatus: String, newStatus: String): Boolean {
        val current = normalizeStatus(currentStatus)
        val target = normalizeStatus(newStatus)
        return when (current) {
            STATUS_PENDING -> target in listOf(STATUS_APPROVED, STATUS_REJECTED, STATUS_CANCELLED)
            STATUS_APPROVED -> target in listOf(STATUS_IN_PROGRESS, STATUS_CANCELLED, STATUS_REJECTED)
            STATUS_IN_PROGRESS -> target in listOf(STATUS_COMPLETED, STATUS_CANCELLED)
            STATUS_COMPLETED -> false // حالة نهائية
            STATUS_REJECTED -> false // حالة نهائية
            STATUS_CANCELLED -> false // حالة نهائية
            else -> false
        }
    }

    /**
     * توحيد مسمى الحالة للإنجليزية
     */
    fun normalizeStatus(statusStr: String): String {
        val upper = statusStr.uppercase().trim()
        return when {
            upper.contains("مقبول") || upper.contains("APPROVED") || upper.contains("ACCEPTED") -> STATUS_APPROVED
            upper.contains("انتظار") || upper.contains("PENDING") -> STATUS_PENDING
            upper.contains("تنفيذ") || upper.contains("IN_PROGRESS") || upper.contains("STARTED") -> STATUS_IN_PROGRESS
            upper.contains("مكتمل") || upper.contains("COMPLETED") -> STATUS_COMPLETED
            upper.contains("ملغى") || upper.contains("CANCELLED") -> STATUS_CANCELLED
            upper.contains("مرفوض") || upper.contains("REJECTED") -> STATUS_REJECTED
            else -> STATUS_PENDING
        }
    }
}

// ==========================================
// 2. 🛡️ Security Audit Trail Logger
// ==========================================
object AuditTrailLogger {
    private val db = FirebaseFirestore.getInstance()

    /**
     * تسجيل حدث أمني في سجل التدقيق السحابي
     * 
     * @param userId معرف المستخدم القائم بالعملية
     * @param actionType نوع العملية (مثل: PAYMENT_VERIFICATION, BOOKING_STATUS_CHANGE, ADMIN_LOGIN)
     * @param targetEntityId معرف الكيان المستهدف
     * @param details تفاصيل العملية
     * @param status نتيجة العملية (SUCCESS / FAILED)
     */
    fun logSecurityEvent(
        userId: String,
        actionType: String,
        targetEntityId: String,
        details: String,
        status: String = "SUCCESS"
    ) {
        val auditId = EntityIdGenerator.generate(EntityIdGenerator.Prefix.PAYMENT)
        val auditPayload = hashMapOf<String, Any?>(
            "id" to auditId,
            "userId" to userId,
            "actionType" to actionType,
            "targetEntityId" to targetEntityId,
            "details" to SecurityCryptoUtils.sanitizeInput(details),
            "status" to status,
            "timestamp" to System.currentTimeMillis(),
            "deviceHash" to SecurityCryptoUtils.hashPassword("DEVICE_ENV_2026")
        )
        db.collection("audit_logs")
            .document(auditId)
            .set(auditPayload)
            .addOnFailureListener {
                // تسجيل صامت لعدم تعطيل تدفق الواجهة
            }
    }
}

// ==========================================
// 3. 💳 Payment Security Guard
// ==========================================
object PaymentSecurityGuard {
    data class PaymentVerificationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    /**
     * التحقق من سلامة وصحة بيانات المعاملة المالية قبل معالجتها
     * 
     * @param bookingId رقم الحجز أو الطلب
     * @param amount المبلغ المالي
     * @param beneficiaryId معرف المستفيد
     * @param receiptNumber رقم إشعار التحويل
     * @return نتيجة التحقق مع رسالة الخطأ إن وُجدت
     */
    fun verifyTransactionDetails(
        bookingId: String,
        amount: Double,
        beneficiaryId: String,
        receiptNumber: String
    ): PaymentVerificationResult {
        if (bookingId.isBlank()) {
            return PaymentVerificationResult(false, "عفواً، رقم الحجز غير صالح أو مفقود.")
        }
        if (amount <= 0) {
            return PaymentVerificationResult(false, "عفواً، مبلغ المعاملة يجب أن يكون أكبر من الصفر.")
        }
        if (beneficiaryId.isBlank()) {
            return PaymentVerificationResult(false, "عفواً، هُوية المستفيد غير محددة.")
        }
        val cleanReceipt = SecurityCryptoUtils.sanitizeInput(receiptNumber)
        if (cleanReceipt.length < 4) {
            return PaymentVerificationResult(false, "عفواً، يجب إدخال رقم إشعار أو حوالة صحيحة لا تقل عن 4 أرقام.")
        }
        return PaymentVerificationResult(true)
    }
}

// ==========================================
// 4. 💬 Chat Media Compressor & Sanitizer Guard
// ==========================================
object ChatMediaGuard {
    private const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024 // 5 ميجابايت كحد أقصى
    private const val MAX_WIDTH = 1280
    private const val MAX_HEIGHT = 1280

    /**
     * فحص وتصغير وضغط صورة المحادثة لضمان سرعة الرفع وتوفير البيانات
     * 
     * @param context سياق التطبيق
     * @param imageUri رابط الصورة في الجهاز
     * @return Pair يحتوي على نجاح العملية والملف المضغوط
     */
    fun validateAndCompressImage(context: Context, imageUri: Uri): Pair<Boolean, File?> {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return Pair(false, null)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return Pair(false, null)
            
            // تغيير الحجم إن كان كبيراً جداً
            val resizedBitmap = if (originalBitmap.width > MAX_WIDTH || originalBitmap.height > MAX_HEIGHT) {
                val ratio = minOf(MAX_WIDTH.toFloat() / originalBitmap.width, MAX_HEIGHT.toFloat() / originalBitmap.height)
                val targetWidth = (originalBitmap.width * ratio).toInt()
                val targetHeight = (originalBitmap.height * ratio).toInt()
                Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val compressedBytes = outputStream.toByteArray()
            
            if (compressedBytes.size > MAX_IMAGE_SIZE_BYTES) {
                return Pair(false, null)
            }

            val cacheDir = File(context.cacheDir, "chat_media").apply { if (!exists()) mkdirs() }
            val compressedFile = File(cacheDir, "compressed_chat_${System.currentTimeMillis()}.jpg")
            FileOutputStream(compressedFile).use { fos ->
                fos.write(compressedBytes)
                fos.flush()
            }
            Pair(true, compressedFile)
        } catch (e: Exception) {
            Pair(false, null)
        }
    }
}
