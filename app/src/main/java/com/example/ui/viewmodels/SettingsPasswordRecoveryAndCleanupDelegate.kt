package com.example.ui.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.data.ActivityLogEntity
import com.example.data.NotificationEntity
import com.example.domain.usecases.ValidatePhoneUseCase
import com.example.utils.AppConstants
import com.example.utils.PasswordHasher
import com.example.utils.SecureHasher
import com.example.utils.SecurityCryptoUtils
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

internal class SettingsPasswordRecoveryAndCleanupDelegate(private val vm: SettingsViewModel) {

    fun resetAccountPassword(entityType: String, phoneOrId: String, newPass: String) {
        val cleanPhone = phoneOrId.trim().replace(" ", "").replace("+967", "").replace("967", "").replace("+", "")
        val hashedPass = PasswordHasher.hash(newPass.trim())
        val passUpdate = mapOf("password" to hashedPass, "passwordHash" to hashedPass)

        val normalized = ValidatePhoneUseCase.normalizePhone(phoneOrId).filter { it.isDigit() }
        val targetPhone = normalized.ifBlank { cleanPhone }
        val phoneVariants = listOf(targetPhone, "0$targetPhone", "+967$targetPhone", "967$targetPhone", "00967$targetPhone")
            .filter { it.isNotBlank() }
            .distinct()

        fun updateByPhone(collectionName: String) {
            for (ph in phoneVariants) {
                vm.db.collection(collectionName)
                    .whereEqualTo("phone", ph)
                    .limit(5)
                    .get()
                    .addOnSuccessListener { qs ->
                        qs.documents.forEach { doc ->
                            vm.db.collection(collectionName).document(doc.id).update(passUpdate)
                        }
                    }
            }
            if (targetPhone.length >= 7) {
                vm.db.collection(collectionName).document(targetPhone).get().addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        doc.reference.update(passUpdate)
                    }
                }
            }
        }

        when (entityType) {
            "PROVIDER", "TECHNICIAN", "TECH" -> {
                updateByPhone("providers")
                vm.mainViewModel.homeViewModel._providers.value = vm.mainViewModel.homeViewModel._providers.value.map {
                    if (it.phone.contains(targetPhone)) it.copy(passwordHash = hashedPass) else it
                }
            }
            "STORE", "RESTAURANT", "MEDICAL", "CENTER", "MALL" -> {
                updateByPhone("stores")
                vm.mainViewModel.adminViewModel._stores.value = vm.mainViewModel.adminViewModel._stores.value.map {
                    if (it.phone.contains(targetPhone)) it.copy(passwordHash = hashedPass) else it
                }
            }
            "JOB" -> updateByPhone("jobs")
            "USER" -> {
                updateByPhone("registered_users")
                updateByPhone("users")
            }
        }

        val logId = vm.db.collection("activity_logs").document().id
        val log = ActivityLogEntity(
            id = logId,
            action = "🔑 إعادة تعيين كلمة المرور للحساب ($entityType - $cleanPhone)",
            timestamp = System.currentTimeMillis()
        )
        vm.db.collection("activity_logs").document(logId).set(log)

        vm.mainViewModel.triggerNotification("🔑 تم تحديث وإعادة تعيين كلمة المرور للحساب ($phoneOrId) بنجاح!")
    }

    fun requestAdminPasswordReset(phone: String) {
        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
        val notif = NotificationEntity(
            id = "reset_" + UUID.randomUUID().toString().take(6),
            title = "🔑 طلب إعادة تعيين كلمة مرور لمستخدم",
            message = "طلب صاحب الهاتف $cleanPhone إعادة تعيين كلمة المرور الخاصة بحسابه الشخصي نظراً لنسيانها أو فقدانها.",
            targetType = "SUPERVISOR",
            targetValue = "ALL",
            timestamp = System.currentTimeMillis(),
            dedupKey = "PWD_RESET_$cleanPhone"
        )
        try {
            if (notif.isValid()) vm.db.collection("notifications").document(notif.id).set(notif)
        } catch (_: Exception) {}
        vm.mainViewModel.triggerNotification("📩 تم إرسال طلب استعادة وإعادة تعيين كلمة المرور لإدارة التطبيق بنجاح")
    }

    fun requestPasswordReset(phone: String, onResult: (Boolean, String) -> Unit) {
        val cleanPhone = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
        if (cleanPhone.length < 7) {
            onResult(false, "رقم الهاتف غير صالح")
            return
        }
        vm.mainViewModel.requestPasswordRecovery(cleanPhone, cleanPhone, "USER")
        requestAdminPasswordReset(cleanPhone)
        onResult(true, "تم إرسال طلب إعادة التعيين للإدارة بنجاح")
    }

    fun approvePasswordReset(phone: String, onResult: (Boolean, String) -> Unit) {
        val cleanPhone = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
        if (cleanPhone.length < 7) {
            onResult(false, "رقم الهاتف غير صالح")
            return
        }

        val chars = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val tempPassword = (1..8).map { chars.random() }.joinToString("")
        val hashedPassword = SecureHasher.hashPassword(tempPassword)
        val encryptedTempPass = SecurityCryptoUtils.encryptCrossDevice(tempPassword)
        val now = System.currentTimeMillis()

        val statusPayload = mapOf(
            "status" to "APPROVED",
            "newPassword" to encryptedTempPass,
            "passwordHash" to hashedPassword,
            "resolvedAt" to now,
            "timestamp" to now
        )
        vm.db.collection("password_recovery_requests").document(cleanPhone)
            .set(statusPayload, SetOptions.merge())
        vm.db.collection("password_resets").document(cleanPhone)
            .set(statusPayload, SetOptions.merge())

        val phoneVariants = listOf(cleanPhone, "0$cleanPhone", "+967$cleanPhone", "967$cleanPhone", "00967$cleanPhone").distinct()
        val passUpdate = mapOf("password" to "", "passwordHash" to hashedPassword)
        val collections = listOf("providers", "pending_providers", "stores", "properties", "registered_users", "users", "join_requests")
        for (col in collections) {
            for (ph in phoneVariants) {
                vm.db.collection(col).whereEqualTo("phone", ph).get().addOnSuccessListener { qs ->
                    for (doc in qs.documents) {
                        doc.reference.update(passUpdate)
                    }
                }
            }
            vm.db.collection(col).document(cleanPhone).get().addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    doc.reference.update(passUpdate)
                }
            }
        }

        val batch = vm.db.batch()
        val notifId = UUID.randomUUID().toString()
        val notifRef = vm.db.collection("notifications").document(notifId)
        val notif = NotificationEntity(
            id = notifId,
            title = "🔑 تم إعادة تعيين كلمة المرور",
            message = "تمت الموافقة على طلبك لإعادة تعيين كلمة المرور. كلمة المرور المؤقتة الجديدة هي: $tempPassword  يرجى تغييرها بعد تسجيل الدخول حفاظاً على خصوصيتك.",
            targetType = "USER",
            targetValue = cleanPhone,
            notificationType = "PASSWORD_RESET_APPROVED",
            timestamp = now,
            dedupKey = "PWD_RESET_APPROVED_$cleanPhone"
        )
        batch.set(notifRef, notif)

        val logId = vm.db.collection("activity_logs").document().id
        val log = ActivityLogEntity(
            id = logId,
            action = "🔑 الموافقة على إعادة تعيين كلمة المرور للهاتف: $cleanPhone",
            timestamp = now
        )
        batch.set(vm.db.collection("activity_logs").document(logId), log)

        batch.commit()
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("🔑 تم الموافقة على طلب إعادة تعيين كلمة المرور للهاتف ($cleanPhone)")
                onResult(true, "تم إعادة التعيين بنجاح. كلمة المرور المؤقتة هي: $tempPassword")
            }
            .addOnFailureListener {
                onResult(false, "فشل حفظ التحديثات: ${it.localizedMessage}")
            }
    }

    fun adminResetAccountPassword(phone: String, newPassword: String, notifyAction: String, customerName: String) {
        val cleanPhone = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
        val cleanNewPass = newPassword.trim()
        if (cleanPhone.length < 7 || cleanNewPass.isBlank()) {
            vm.mainViewModel.triggerNotification("❌ يرجى التأكد من رقم الهاتف وكلمة المرور الجديدة")
            return
        }
        val hashedPassword = SecureHasher.hashPassword(cleanNewPass)
        val encryptedNewPass = SecurityCryptoUtils.encryptCrossDevice(cleanNewPass)
        val passUpdate = mapOf("password" to "", "passwordHash" to hashedPassword)
        val phoneVariants = listOf(cleanPhone, "0$cleanPhone", "+967$cleanPhone", "967$cleanPhone", "00967$cleanPhone").distinct()

        val collections = listOf("providers", "pending_providers", "stores", "properties", "registered_users", "users", "join_requests")
        for (col in collections) {
            for (ph in phoneVariants) {
                vm.db.collection(col).whereEqualTo("phone", ph).get().addOnSuccessListener { snap ->
                    for (doc in snap.documents) {
                        doc.reference.update(passUpdate)
                    }
                }
            }
            vm.db.collection(col).document(cleanPhone).get().addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    doc.reference.update(passUpdate)
                }
            }
        }

        val now = System.currentTimeMillis()
        val resolvePayload = mapOf(
            "status" to "RESOLVED",
            "newPassword" to encryptedNewPass,
            "passwordHash" to hashedPassword,
            "resolvedAt" to now,
            "timestamp" to now
        )
        try {
            vm.db.collection("password_recovery_requests").document(cleanPhone)
                .set(resolvePayload, SetOptions.merge())
            vm.db.collection("password_resets").document(cleanPhone)
                .set(resolvePayload, SetOptions.merge())
        } catch (_: Exception) {}

        vm.mainViewModel.homeViewModel._providers.value = vm.mainViewModel.homeViewModel._providers.value.map {
            if (ValidatePhoneUseCase.normalizePhone(it.phone).filter { c -> c.isDigit() } == cleanPhone) {
                it.copy(passwordHash = hashedPassword)
            } else it
        }
        vm.mainViewModel.adminViewModel._stores.value = vm.mainViewModel.adminViewModel._stores.value.map {
            if (ValidatePhoneUseCase.normalizePhone(it.phone).filter { c -> c.isDigit() } == cleanPhone) {
                it.copy(passwordHash = hashedPassword)
            } else it
        }

        val logId = vm.db.collection("activity_logs").document().id
        val log = ActivityLogEntity(
            id = logId,
            action = "🔑 إعادة تعيين كلمة المرور للحساب: $customerName ($cleanPhone) - الإجراء: $notifyAction",
            timestamp = now
        )
        vm.db.collection("activity_logs").document(logId).set(log)

        val notifId = UUID.randomUUID().toString()
        val (title, message) = when (notifyAction) {
            "DIRECT_PASSWORD" -> Pair(
                "🔑 إعادة تعيين كلمة المرور بنجاح",
                "تم إعادة تعيين كلمة مرور حسابك. يرجى التواصل مع الدعم لاستلام كلمة المرور الجديدة"
            )
            "VERIFICATION_WHATSAPP" -> Pair(
                "🔐 التحقق من الهوية - استعادة الحساب",
                "عزيزي المشترك، يرجى التواصل عبر الواتساب أو التليجرام أو المحادثة الفورية مع الإدارة للتحقق من هويتك وتأكيد ملكيتك للحساب واستلام كلمة المرور."
            )
            "INSTANT_CHAT" -> Pair(
                "💬 محادثة فورية لاستعادة الحساب",
                "تم فتح قناة دعم فورية لك. يرجى التوجه للمحادثة المباشرة مع الإدارة للتحقق من هويتك واسترجاع حسابك فوراً."
            )
            else -> Pair(
                "🔑 تحديث كلمة المرور",
                "قامت الإدارة بتحديث ومعالجة طلب استعادة كلمة المرور الخاصة بحسابك."
            )
        }

        val userNotif = NotificationEntity(
            id = notifId,
            title = title,
            message = message,
            targetType = "USER",
            targetValue = cleanPhone,
            timestamp = now,
            dedupKey = "PWD_RESET_$cleanPhone"
        )
        vm.db.collection("notifications").document(notifId).set(userNotif).addOnSuccessListener {
            vm.mainViewModel.triggerNotification("✅ تم إرسال إشعار إعادة التعيين للمستخدم بنجاح!")
        }
    }

    fun requestPasswordRecoveryForStore(name: String, phone: String, password: String) {
        val cleanPhone = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
        vm.mainViewModel.setPasswordRecoveryWaitingPhone(cleanPhone)
        vm.mainViewModel.requestPasswordRecovery(cleanPhone, name, "STORE")
    }

    fun requestPasswordRecoveryForProperty(title: String, phone: String, password: String) {
        val cleanPhone = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
        vm.mainViewModel.setPasswordRecoveryWaitingPhone(cleanPhone)
        vm.mainViewModel.requestPasswordRecovery(cleanPhone, title, "PROPERTY")
    }

    fun requestPasswordRecoveryGeneral(accountName: String, phone: String, accountType: String, currentPassword: String) {
        val cleanPhone = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
        vm.mainViewModel.setPasswordRecoveryWaitingPhone(cleanPhone)
        vm.mainViewModel.requestPasswordRecovery(cleanPhone, accountName, accountType)
    }

    fun wipeAllDatabaseData(password: String): Boolean {
        return if (vm.mainViewModel.verifyAdminOrOwnerPassword(password)) {
            val collections = listOf(
                "categories", "providers", "pending_providers", "banners", "settings", "reports",
                "bookings", "notifications", "chat_channels", "cities", "stores", "medical",
                "restaurants", AppConstants.COL_JOBS, "job_applications", "properties", "products", "reviews"
            )
            collections.forEach { col ->
                vm.db.collection(col).limit(300).get().addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { doc -> doc.reference.delete() }
                }
            }
            vm.mainViewModel.triggerNotification("💥 تم مسح كامل قاعدة البيانات المحددة بنجاح وإعادتها إلى الصفر!")
            true
        } else {
            vm.mainViewModel.triggerNotification("❌ كلمة المرور غير صحيحة! فشل تطهير البيانات.")
            false
        }
    }

    fun wipeSelectedDatabaseData(password: String, selectedCollections: List<String>): Boolean {
        return if (vm.mainViewModel.verifyAdminOrOwnerPassword(password)) {
            selectedCollections.forEach { col ->
                vm.db.collection(col).limit(300).get().addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { doc ->
                        doc.reference.delete()
                    }
                }
            }
            vm.mainViewModel.triggerNotification("🧹 تم مسح الفئات المحددة وإعادتها إلى الصفر بنجاح!")
            true
        } else {
            vm.mainViewModel.triggerNotification("❌ كلمة المرور غير صحيحة! فشل تطهير البيانات.")
            false
        }
    }

    fun wipeAllMockAndTemporaryData() {
        vm.viewModelScope.launch {
            try {
                val mockPrefixes = listOf("mock_", "test_", "demo_", "temp_", "sample_")
                fun isMockDoc(doc: DocumentSnapshot): Boolean {
                    val idLower = doc.id.lowercase()
                    val isFlagged = doc.getBoolean("isMock") == true ||
                        doc.getBoolean("isTest") == true ||
                        doc.getBoolean("isTemporary") == true
                    val hasPrefix = mockPrefixes.any { idLower.startsWith(it) }
                    return isFlagged || hasPrefix
                }

                for (col in listOf("notifications", "chat_channels", "reports", "bookings", "providers")) {
                    vm.db.collection(col).whereEqualTo("isMock", true).limit(300).get().addOnSuccessListener { snapshot ->
                        snapshot?.documents?.forEach { doc ->
                            if (isMockDoc(doc)) doc.reference.delete()
                        }
                    }
                    vm.db.collection(col).whereEqualTo("isTest", true).limit(300).get().addOnSuccessListener { snapshot ->
                        snapshot?.documents?.forEach { doc ->
                            if (isMockDoc(doc)) doc.reference.delete()
                        }
                    }
                }
                vm.db.collection("admin_audit_logs").add(
                    mapOf(
                        "action" to "WIPE_MOCK_DATA",
                        "performedBy" to vm.mainViewModel.adminViewModel.adminRole.value,
                        "details" to "تنظيف البيانات التجريبية والوهمية",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
                vm.mainViewModel.triggerNotification("🧹 تم تنظيف البيانات التجريبية والوهمية فقط مع تسجيل العملية في سجل التدقيق!")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error wiping mock data", e)
                vm.mainViewModel.triggerNotification("❌ حدث خطأ أثناء عملية التنظيف")
            }
        }
    }

    fun autoCleanupData(daysToKeep: Int = 30) {
        vm.viewModelScope.launch {
            try {
                val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60 * 60 * 1000)
                vm.db.collection("notifications").whereLessThan("timestamp", cutoffTime).limit(300).get()
                    .addOnSuccessListener { snapshot ->
                        snapshot?.documents?.forEach { doc -> doc.reference.delete() }
                    }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error in autoCleanupData", e)
            }
        }
    }

    fun scheduleAutoCleanup(days: Int = 30) {
        vm.viewModelScope.launch {
            while (true) {
                delay(24 * 60 * 60 * 1000L)
                try {
                    autoCleanupData(days)
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Error in scheduleAutoCleanup", e)
                }
            }
        }
    }
}
