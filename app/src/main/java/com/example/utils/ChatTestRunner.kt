package com.example.utils

import android.content.Context
import com.example.data.models.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatStepReport(
    val stepName: String,
    val description: String,
    var status: String = "PENDING", // SUCCESS, FAILED
    var firestoreVerified: Boolean = false,
    var notes: String = ""
)

data class ChatFullReport(
    val timestamp: String,
    val totalSteps: Int,
    val passed: Int,
    val failed: Int,
    val steps: List<ChatStepReport>
)

class ChatTestRunner(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val clientPhone = "777100100"
    private val providerPhone = "777002002"

    suspend fun runChatTest(
        onProgress: (String) -> Unit,
        onComplete: (ChatFullReport) -> Unit
    ) {
        val stepsList = mutableListOf<ChatStepReport>()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        fun addStep(name: String, desc: String): ChatStepReport {
            val step = ChatStepReport(name, desc)
            stepsList.add(step)
            return step
        }

        val step1 = addStep("إنشاء قناة محادثة", "تأسيس قناة محادثة ثنائية PRIVATE بين العميل والفني")
        val step2 = addStep("التحقق من القناة", "التأكد من وجود القناة وحفظها في Firestore بوضع PRIVATE")
        val step3 = addStep("إرسال رسائل تجريبية", "محاكاة إرسال رسائل نصية وصوت وصور بين الطرفين")
        val step4 = addStep("الرد على الرسالة", "اختبار الرد على رسالة معينة وحفظ معرّف الرسالة الأصلية")
        val step5 = addStep("تعديل الرسالة", "تعديل محتوى رسالة وتحديث حقل isEdited = true")
        val step6 = addStep("حذف الرسالة", "محاكاة حذف رسالة للجميع وتحديث حقل isDeleted = true")
        val step7 = addStep("قناة الدعم الفني", "تأسيس واختبار قناة اتصال خاصة بالدعم الفني SUPPORT")

        try {
            onProgress("🧹 جاري تصفية أي محادثات ورسائل تجريبية قديمة...")
            cleanAllTestData()
            delay(1000)

            // Step 1: Create chat channel
            onProgress("💬 خطوة 1: جاري تأسيس قناة محادثة جديدة بين العميل والفني...")
            val channelId = "test_channel_" + System.currentTimeMillis()
            val testChannel = ChatChannel(
                id = channelId,
                participants = listOf("user_client_99", "user_tech_99"),
                participantNames = mapOf("user_client_99" to "عميل اختبار", "user_tech_99" to "فني اختبار"),
                type = ChannelType.PRIVATE,
                title = "محادثة اختبار الحجز",
                relatedEntityType = "BOOKING",
                relatedEntityId = "test_booking_123",
                lastMessage = "مرحبا بك، هل أنت متاح؟",
                lastMessageTime = System.currentTimeMillis(),
                lastMessageSenderId = "user_client_99",
                targetPhone = providerPhone,
                customerPhone = clientPhone,
                createdAt = System.currentTimeMillis()
            )

            db.collection("chat_channels").document(channelId).set(testChannel).await()
            step1.status = "SUCCESS"
            step1.firestoreVerified = true
            step1.notes = "تم إنشاء قناة المحادثة بنجاح: $channelId"

            // Step 2: Verify channel
            onProgress("🔍 خطوة 2: التحقق من القناة ونوعها في Firestore...")
            delay(1500)
            val channelDoc = db.collection("chat_channels").document(channelId).get().await()
            if (channelDoc.exists() && channelDoc.getString("type") == "PRIVATE") {
                step2.status = "SUCCESS"
                step2.firestoreVerified = true
                step2.notes = "تم التحقق: القناة PRIVATE ومرتبطة بـ BOOKING"
            } else {
                step2.status = "FAILED"
                step2.notes = "خطأ: القناة غير موجودة أو معرّفاتها خاطئة"
            }

            // Step 3: Send experimental messages
            onProgress("✉️ خطوة 3: جاري محاكاة إرسال رسائل نصية ووسائط...")
            val msgId1 = "msg_text_" + System.currentTimeMillis()
            val msgId2 = "msg_audio_" + System.currentTimeMillis()

            val textMsg = ChatMessage(
                id = msgId1,
                channelId = channelId,
                senderId = "user_client_99",
                senderName = "عميل اختبار",
                senderPhone = clientPhone,
                message = "مرحبا، هل تستطيع الحضور اليوم لإنهاء العمل؟",
                mediaType = MediaType.TEXT,
                status = MessageStatus.SENT,
                timestamp = System.currentTimeMillis()
            )

            val audioMsg = ChatMessage(
                id = msgId2,
                channelId = channelId,
                senderId = "user_tech_99",
                senderName = "فني اختبار",
                senderPhone = providerPhone,
                message = "مقطع صوتي",
                mediaType = MediaType.AUDIO,
                mediaUrl = "https://example.com/audio/test.mp3",
                audioDurationSec = 15,
                status = MessageStatus.DELIVERED,
                timestamp = System.currentTimeMillis() + 500
            )

            db.collection("chat_channels").document(channelId).collection("messages").document(msgId1).set(textMsg).await()
            db.collection("chat_channels").document(channelId).collection("messages").document(msgId2).set(audioMsg).await()

            delay(1500)
            val msgSnapshot = db.collection("chat_channels").document(channelId).collection("messages").get().await()
            if (msgSnapshot.size() == 2) {
                step3.status = "SUCCESS"
                step3.firestoreVerified = true
                step3.notes = "تم تسجيل الرسائل الثنائية (نصية + صوتية) بنجاح"
            } else {
                step3.status = "FAILED"
                step3.notes = "خطأ في تسجيل وتخزين الرسائل بداخل القناة"
            }

            // Step 4: Reply on text message
            onProgress("↩️ خطوة 4: تجربة الرد على رسالة نصية محددة...")
            val replyId = "msg_reply_" + System.currentTimeMillis()
            val replyMsg = ChatMessage(
                id = replyId,
                channelId = channelId,
                senderId = "user_tech_99",
                senderName = "فني اختبار",
                senderPhone = providerPhone,
                message = "نعم بكل تأكيد سأحضر بعد ساعة.",
                mediaType = MediaType.TEXT,
                replyToId = msgId1,
                replyToText = "مرحبا، هل تستطيع الحضور اليوم لإنهاء العمل؟",
                replyToSender = "عميل اختبار",
                status = MessageStatus.SENT,
                timestamp = System.currentTimeMillis() + 1000
            )

            db.collection("chat_channels").document(channelId).collection("messages").document(replyId).set(replyMsg).await()
            delay(1000)
            val replyDoc = db.collection("chat_channels").document(channelId).collection("messages").document(replyId).get().await()
            if (replyDoc.getString("replyToId") == msgId1) {
                step4.status = "SUCCESS"
                step4.firestoreVerified = true
                step4.notes = "تم الرد بنجاح وحفظ معرّف الرسالة الأصلية replyToId"
            } else {
                step4.status = "FAILED"
                step4.notes = "فشل في تسجيل الرد أو معطيات replyToId مفقودة"
            }

            // Step 5: Edit Message
            onProgress("✏️ خطوة 5: تعديل الرسالة وإظهار وسم التعديل...")
            db.collection("chat_channels").document(channelId).collection("messages").document(replyId).update(
                "message", "نعم بكل تأكيد سأحضر بعد نصف ساعة فقط.",
                "isEdited", true,
                "editTimestamp", System.currentTimeMillis()
            ).await()
            delay(1000)
            val docEdited = db.collection("chat_channels").document(channelId).collection("messages").document(replyId).get().await()
            if (docEdited.getBoolean("isEdited") == true && docEdited.getString("message")!!.contains("نصف ساعة")) {
                step5.status = "SUCCESS"
                step5.firestoreVerified = true
                step5.notes = "تم تعديل محتوى الرسالة بنجاح ووسم التعديل نشط"
            } else {
                step5.status = "FAILED"
                step5.notes = "فشل في حفظ تحديثات تعديل الرسالة"
            }

            // Step 6: Delete Message (Delete for everyone)
            onProgress("🗑️ خطوة 6: تجربة حذف رسالة معينة للجميع...")
            db.collection("chat_channels").document(channelId).collection("messages").document(msgId1).update(
                "isDeleted", true,
                "message", "تم حذف هذه الرسالة",
                "deletedBy", "user_client_99"
            ).await()
            delay(1000)
            val docDel = db.collection("chat_channels").document(channelId).collection("messages").document(msgId1).get().await()
            if (docDel.getBoolean("isDeleted") == true && docDel.getString("message") == "تم حذف هذه الرسالة") {
                step6.status = "SUCCESS"
                step6.firestoreVerified = true
                step6.notes = "تم مسح وتحديث محتوى الرسالة المحذوفة للجميع بنجاح"
            } else {
                step6.status = "FAILED"
                step6.notes = "فشل الإلغاء للجميع"
            }

            // Step 7: Technical Support Channel (SUPPORT)
            onProgress("🛠️ خطوة 7: محاكاة قناة اتصال الدعم الفني المباشر...")
            val supportChanId = "support_channel_" + System.currentTimeMillis()
            val supportChannel = ChatChannel(
                id = supportChanId,
                participants = listOf("user_client_99", "admin_support_1"),
                type = ChannelType.SUPPORT,
                title = "الدعم والمساعدة",
                lastMessage = "مرحبا، كيف يمكننا مساعدتك اليوم؟",
                lastMessageTime = System.currentTimeMillis(),
                customerPhone = clientPhone,
                createdAt = System.currentTimeMillis()
            )
            db.collection("chat_channels").document(supportChanId).set(supportChannel).await()
            delay(1000)
            val supportDoc = db.collection("chat_channels").document(supportChanId).get().await()
            if (supportDoc.exists() && supportDoc.getString("type") == "SUPPORT") {
                step7.status = "SUCCESS"
                step7.firestoreVerified = true
                step7.notes = "تأسست قناة الدعم SUPPORT وجاهزة للمحاكاة والتواصل المباشر"
            } else {
                step7.status = "FAILED"
                step7.notes = "فشل تأسيس قناة SUPPORT"
            }

            // Clean up
            onProgress("🧹 جاري تصفية وتطهير قنوات ورسائل الاختبار...")
            cleanAllTestData()

        } catch (e: Exception) {
            onProgress("❌ حدث خطأ غير متوقع أثناء الفحص: ${e.message}")
            stepsList.forEach { if (it.status == "PENDING") it.status = "FAILED" }
        }

        val passed = stepsList.count { it.status == "SUCCESS" }
        val failed = stepsList.count { it.status == "FAILED" }
        val report = ChatFullReport(
            timestamp = timestamp,
            totalSteps = stepsList.size,
            passed = passed,
            failed = failed,
            steps = stepsList
        )
        saveReportToFile(report)
        onComplete(report)
    }

    suspend fun cleanAllTestData() {
        try {
            val batch = db.batch()

            // Delete chat_channels matching our test client/provider phone
            val chSnapshot = db.collection("chat_channels").whereEqualTo("customerPhone", clientPhone).get().await()
            for (doc in chSnapshot.documents) {
                // Delete submessages first
                val msgsSnapshot = doc.reference.collection("messages").get().await()
                for (mDoc in msgsSnapshot.documents) {
                    batch.delete(mDoc.reference)
                }
                batch.delete(doc.reference)
            }

            // Explicit prefixed channel deletes
            val chSnapshot2 = db.collection("chat_channels").get().await()
            for (doc in chSnapshot2.documents) {
                if (doc.id.startsWith("test_channel_") || doc.id.startsWith("support_channel_")) {
                    val msgsSnapshot = doc.reference.collection("messages").get().await()
                    for (mDoc in msgsSnapshot.documents) {
                        batch.delete(mDoc.reference)
                    }
                    batch.delete(doc.reference)
                }
            }

            batch.commit().await()
        } catch (_: Exception) {}
    }

    private fun saveReportToFile(report: ChatFullReport) {
        try {
            val file = File(context.filesDir, "chat_test_report.txt")
            val sb = StringBuilder()
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            sb.append("📋 تقرير اختبار نظام المحادثات الفورية الشامل\n")
            sb.append("التاريخ: ${report.timestamp}\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
            for (step in report.steps) {
                sb.append(String.format("%-25s | %-12s | %s\n",
                    step.stepName,
                    if (step.status == "SUCCESS") "✅ نجاح" else "❌ فشل",
                    step.notes
                ))
            }
            file.writeText(sb.toString())
        } catch (_: Exception) {}
    }
}
