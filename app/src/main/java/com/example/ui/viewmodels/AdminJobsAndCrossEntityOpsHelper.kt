package com.example.ui.viewmodels

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.data.JobApplicationEntity
import com.example.data.JobEntity
import com.example.data.NotificationEntity
import com.example.data.isValid
import com.example.utils.DateFormatter
import com.google.firebase.firestore.SetOptions
import java.util.UUID

internal fun saveJobOp(vm: AdminViewModel, job: JobEntity) {
    val cleanPhone = job.phone.trim().replace(" ", "").replace("+", "")
    val targetId = if (job.id.isEmpty()) vm.db.collection("jobs").document().id else job.id
    val finalJob = job.copy(id = targetId, phone = cleanPhone)

    val updatedJobs = vm._jobs.value.filter { it.id != targetId }.toMutableList()
    updatedJobs.add(finalJob)
    vm._jobs.value = updatedJobs

    vm.db.collection("jobs").document(targetId).set(finalJob)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (finalJob.isApproved) "✅ تم حفظ ونشر الإعلان الوظيفي بنجاح!" else "📨 تم تقديم إعلان الوظيفة للمراجع من قبل الأدمن!"
            )
        }
        .addOnFailureListener {
            vm.mainViewModel.triggerNotification("⚠️ تم إدراج الوظيفة محلياً وفي انتظار المزامنة")
        }
}

internal fun deleteJobOp(vm: AdminViewModel, jobId: String) {
    vm._jobs.value = vm._jobs.value.map {
        if (it.id == jobId) it.copy(isDeleted = true) else it
    }
    val updates = hashMapOf<String, Any?>(
        "isDeleted" to true,
        "deletedAt" to System.currentTimeMillis()
    )
    vm.db.collection("jobs").document(jobId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("🗑️ تم نقل الإعلان الوظيفي لسلة المحذوفات")
        }
}

internal fun restoreJobOp(vm: AdminViewModel, jobId: String) {
    vm._jobs.value = vm._jobs.value.map {
        if (it.id == jobId) it.copy(isDeleted = false) else it
    }
    val updates = hashMapOf<String, Any?>(
        "isDeleted" to false,
        "deletedAt" to null
    )
    vm.db.collection("jobs").document(jobId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("♻️ تم استعادة الإعلان الوظيفي بنجاح")
        }
}

internal fun deleteJobPermanentlyOp(vm: AdminViewModel, jobId: String) {
    vm.db.collection("jobs").document(jobId).delete()
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("🗑️ تم حذف الإعلان الوظيفي نهائياً من النظام")
        }
}

internal fun setJobApprovedOp(vm: AdminViewModel, jobId: String, isApproved: Boolean) {
    vm._jobs.value = vm._jobs.value.map {
        if (it.id == jobId) it.copy(isApproved = isApproved, isActive = isApproved) else it
    }
    val updates = mapOf("isApproved" to isApproved, "isActive" to isApproved)
    val targetJob = vm._jobs.value.find { it.id == jobId }
    if (targetJob != null && isApproved) {
        val notification = NotificationEntity(
            id = UUID.randomUUID().toString(),
            title = "🎉 تم قبول ونشر إعلان وظيفتك!",
            message = "تهانينا! تم قبول ونشر إعلان الوظيفة (${targetJob.title}) للجميع!",
            targetType = "USER",
            targetValue = targetJob.phone,
            timestamp = System.currentTimeMillis()
        )
        vm.mainViewModel._notifications.value = listOf(notification) + vm.mainViewModel._notifications.value
        try {
            if (notification.isValid()) {
                vm.db.collection("notifications").document(notification.id).set(notification)
            }
        } catch (_: Exception) {
        }
    }
    vm.db.collection("jobs").document(jobId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isApproved) "✅ تم قبول ونشر إعلان الوظيفة بنجاح!" else "❌ تم رفض إعلان الوظيفة"
            )
        }
    vm.db.collection("pending_providers").document(jobId).delete()
    vm.db.collection("join_requests").document(jobId).update(
        mapOf(
            "status" to if (isApproved) "APPROVED" else "REJECTED",
            "approvalStatus" to if (isApproved) "APPROVED" else "REJECTED"
        )
    )
    vm._pendingProviders.value = vm._pendingProviders.value.filter { it.id != jobId }
}

internal fun setJobBlockedOp(vm: AdminViewModel, jobId: String, isBlocked: Boolean, reason: String) {
    val updates = mapOf("isBlocked" to isBlocked, "blockReason" to reason)
    vm.db.collection("jobs").document(jobId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isBlocked) "🚫 تم حظر الإعلان الوظيفي ($reason)" else "✅ تم إلغاء حظر الإعلان الوظيفي"
            )
        }
}

internal fun setJobPinnedOp(vm: AdminViewModel, jobId: String, isPinned: Boolean) {
    vm.db.collection("jobs").document(jobId).update("isPinned", isPinned)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isPinned) "📌 تم تثبيت الوظيفة في الصدارة" else "📌 تم إلغاء تثبيت الوظيفة"
            )
        }
}

internal fun setJobVipOp(vm: AdminViewModel, jobId: String, isVip: Boolean) {
    vm.db.collection("jobs").document(jobId).update("isVip", isVip)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isVip) "🏆 تم تمييز الإعلان الوظيفي كـ VIP" else "🔒 تم إلغاء شارة VIP عن الوظيفة"
            )
        }
}

internal fun setJobChatDisabledOp(vm: AdminViewModel, jobId: String, isDisabled: Boolean) {
    vm.db.collection("jobs").document(jobId).update("isChatDisabled", isDisabled)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isDisabled) "🔇 تم إيقاف الدردشة للإعلان الوظيفي" else "💬 تم تفعيل الدردشة للإعلان الوظيفي"
            )
        }
}

internal fun submitJobApplicationOp(vm: AdminViewModel, application: JobApplicationEntity) {
    val targetId = vm.db.collection("job_applications").document().id
    val finalApp = application.copy(id = targetId)
    vm._jobApplications.value = vm._jobApplications.value + finalApp
    vm.db.collection("job_applications").document(targetId).set(finalApp)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("📨 تم إرسال طلب التقديم على الوظيفة بنجاح!")
        }
        .addOnFailureListener {
            vm.mainViewModel.triggerNotification("❌ فشل تقديم الطلب: ${it.message}")
        }
}

internal fun updateJobApplicationStatusOp(vm: AdminViewModel, appId: String, status: String, reason: String) {
    vm._jobApplications.value = vm._jobApplications.value.map {
        if (it.id == appId) it.copy(status = status, rejectionReason = reason) else it
    }
    val updates = mutableMapOf<String, Any>("status" to status)
    if (reason.isNotEmpty()) {
        updates["rejectionReason"] = reason
    }
    vm.db.collection("job_applications").document(appId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("✅ تم تحديث حالة طلب التقديم إلى: $status")
        }
        .addOnFailureListener {
            vm.db.collection("job_applications").document(appId).set(updates, SetOptions.merge())
        }
}

internal fun deleteJobApplicationOp(vm: AdminViewModel, appId: String) {
    vm._jobApplications.value = vm._jobApplications.value.filter { it.id != appId }
    vm.db.collection("job_applications").document(appId).delete()
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("🗑️ تم حذف طلب التقديم من النظام بنجاح")
        }
}

internal fun sendNotificationToApplicantsOp(vm: AdminViewModel, title: String, message: String, jobId: String) {
    val notif = NotificationEntity(
        id = UUID.randomUUID().toString(),
        title = title,
        message = message,
        targetType = if (jobId.isNotEmpty()) "JOB_APPLICANTS" else "ALL",
        targetValue = jobId,
        timestamp = System.currentTimeMillis()
    )
    if (notif.isValid()) {
        vm.db.collection("notifications").document(notif.id).set(notif)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("🔔 تم إرسال الإشعار لجميع المتقدمين للوظائف بنجاح!")
            }
    }
}

internal fun exportJobApplicantsCsvOp(vm: AdminViewModel, context: Context) {
    try {
        val apps = vm._jobApplications.value
        val csvContent = StringBuilder()
        csvContent.append("المعرف,اسم المتقدم,رقم الهاتف,الوظيفة,الشركة,المؤهلات,الحالة,تاريخ التقديم\n")
        apps.forEach { app ->
            val dateStr = DateFormatter.formatCustom(app.createdAt, "yyyy-MM-dd HH:mm")
            csvContent.append(
                "${app.id},\"${app.applicantName}\",\"${app.applicantPhone}\",\"${app.jobTitle}\",\"${app.companyName}\",\"${app.applicantQuals.replace("\n", " ")}\",\"${app.status}\",\"$dateStr\"\n"
            )
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Job Applicants CSV", csvContent.toString())
        clipboard.setPrimaryClip(clip)
        vm.mainViewModel.triggerNotification("📋 تم نسخ بيانات المتقدمين للوظائف بصيغة CSV إلى الحافظة بنجاح (${apps.size} متقدم)!")
    } catch (e: Exception) {
        vm.mainViewModel.triggerNotification("❌ فشل تصدير البيانات: ${e.message}")
    }
}

internal fun redirectBookingToEntityOp(
    vm: AdminViewModel,
    bookingId: String,
    targetEntityId: String,
    targetEntityName: String,
    targetPhone: String
) {
    val updates = mapOf(
        "providerId" to targetEntityId,
        "providerName" to targetEntityName,
        "providerPhone" to targetPhone,
        "status" to "PENDING"
    )
    vm.db.collection("bookings").document(bookingId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("🔄 تم توجيه الحجز للجهة/الفني ($targetEntityName) بنجاح!")
        }
}

internal fun unbanEntityOp(vm: AdminViewModel, entityType: String, entityId: String) {
    when (entityType.uppercase()) {
        "PROVIDER" -> vm.db.collection("providers").document(entityId).update("isBlocked", false)
        "STORE", "RESTAURANT", "MEDICAL" -> setStoreBlockedOp(vm, entityId, false, "")
        "PROPERTY" -> setPropertyBlockedOp(vm, entityId, false, "")
        "JOB" -> setJobBlockedOp(vm, entityId, false, "")
        else -> {
            vm.db.collection("stores").document(entityId).update("isBlocked", false)
            vm.db.collection("properties").document(entityId).update("isBlocked", false)
            vm.db.collection("jobs").document(entityId).update("isBlocked", false)
            vm.db.collection("providers").document(entityId).update("isBlocked", false)
        }
    }
    vm.mainViewModel.triggerNotification("✅ تم إلغاء حظر الكيان بنجاح!")
}

internal fun restoreEntityOp(vm: AdminViewModel, entityType: String, entityId: String) {
    when (entityType.uppercase()) {
        "PROVIDER" -> vm.restoreProvider(entityId)
        "STORE", "RESTAURANT", "MEDICAL" -> restoreStoreOp(vm, entityId)
        "PROPERTY" -> restorePropertyOp(vm, entityId)
        "JOB" -> restoreJobOp(vm, entityId)
        else -> {
            restoreStoreOp(vm, entityId)
            restorePropertyOp(vm, entityId)
            restoreJobOp(vm, entityId)
        }
    }
}

internal fun hardDeleteEntityOp(vm: AdminViewModel, entityType: String, entityId: String, reason: String) {
    val cleanReason = reason.trim()
    if (cleanReason.isNotBlank()) {
        vm.logAdminActivity("🗑️ حذف نهائي للسجل ($entityType: $entityId) | السبب: $cleanReason")
    }
    when (entityType.uppercase()) {
        "PROVIDER" -> vm.removeProviderPermanently(entityId)
        "STORE", "RESTAURANT", "MEDICAL" -> deleteStorePermanentlyOp(vm, entityId)
        "PROPERTY" -> deletePropertyPermanentlyOp(vm, entityId)
        "JOB" -> deleteJobPermanentlyOp(vm, entityId)
        else -> {
            deleteStorePermanentlyOp(vm, entityId)
            deletePropertyPermanentlyOp(vm, entityId)
            deleteJobPermanentlyOp(vm, entityId)
        }
    }
}
