package com.example.ui.helpers
import com.example.data.isValid

import android.content.Context
import com.example.ui.*
import com.example.data.NotificationEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.MainViewModel.RestoreAccountMatch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.UUID

/**
 * Helper class for searching accounts and managing password recovery / reset operations.
 */
class AccountRecoveryHelper(
    private val db: FirebaseFirestore,
    private val preferenceHelper: AppPreferenceHelper
) {

    private val recoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun searchAccountForRestore(cleanPhone: String, onResult: (RestoreAccountMatch?) -> Unit) {
        val normalizedPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(cleanPhone).ifBlank { cleanPhone.trim() }
        if (normalizedPhone.isBlank()) {
            onResult(null)
            return
        }
        recoveryScope.launch {
            try {
                val match = supervisorScope {
                    val providerDef = async(Dispatchers.IO) {
                        runCatching {
                            val snap = com.google.android.gms.tasks.Tasks.await(
                                db.collection("providers").whereEqualTo("phone", normalizedPhone).limit(1).get()
                            )
                            snap.documents.firstOrNull()?.toObject(ProviderEntity::class.java)?.let {
                                RestoreAccountMatch("PROVIDER", it.name, provider = it)
                            }
                        }.getOrNull()
                    }
                    val storeDef = async(Dispatchers.IO) {
                        runCatching {
                            val snap = com.google.android.gms.tasks.Tasks.await(
                                db.collection("stores").whereEqualTo("phone", normalizedPhone).limit(1).get()
                            )
                            snap.documents.firstOrNull()?.toObject(StoreEntity::class.java)?.let {
                                val resolvedType = when (it.sectionId.lowercase().trim()) {
                                    "restaurants", "restaurant" -> "RESTAURANT"
                                    "medical" -> "MEDICAL"
                                    else -> "STORE"
                                }
                                RestoreAccountMatch(resolvedType, it.name, store = it)
                            }
                        }.getOrNull()
                    }
                    val propDef = async(Dispatchers.IO) {
                        runCatching {
                            val snap = com.google.android.gms.tasks.Tasks.await(
                                db.collection("properties").whereEqualTo("phone", normalizedPhone).limit(1).get()
                            )
                            snap.documents.firstOrNull()?.toObject(PropertyEntity::class.java)?.let {
                                RestoreAccountMatch("PROPERTY", it.title, property = it)
                            }
                        }.getOrNull()
                    }
                    val userDef = async(Dispatchers.IO) {
                        runCatching {
                            val snap = com.google.android.gms.tasks.Tasks.await(
                                db.collection("users").whereEqualTo("phone", normalizedPhone).limit(1).get()
                            )
                            val doc = snap.documents.firstOrNull() ?: runCatching {
                                com.google.android.gms.tasks.Tasks.await(
                                    db.collection("registered_users").whereEqualTo("phone", normalizedPhone).limit(1).get()
                                ).documents.firstOrNull()
                            }.getOrNull()
                            doc?.let { uDoc ->
                                val uName = uDoc.getString("name")?.takeIf { it.isNotBlank() }
                                    ?: uDoc.getString("fullName")?.takeIf { it.isNotBlank() }
                                    ?: "مستخدم مسجل"
                                RestoreAccountMatch("CLIENT", uName)
                            }
                        }.getOrNull()
                    }
                    val joinReqDef = async(Dispatchers.IO) {
                        runCatching {
                            val snap = com.google.android.gms.tasks.Tasks.await(
                                db.collection("join_requests").whereEqualTo("phone", normalizedPhone).limit(1).get()
                            )
                            snap.documents.firstOrNull()?.let { rDoc ->
                                val rName = rDoc.getString("fullName")?.takeIf { it.isNotBlank() }
                                    ?: rDoc.getString("businessName")?.takeIf { it.isNotBlank() }
                                    ?: rDoc.getString("propertyTitle")?.takeIf { it.isNotBlank() }
                                    ?: rDoc.getString("jobTitle")?.takeIf { it.isNotBlank() }
                                    ?: rDoc.getString("name")?.takeIf { it.isNotBlank() }
                                    ?: "حساب مسجل"
                                val rType = rDoc.getString("type") ?: "CLIENT"
                                RestoreAccountMatch(rType, rName)
                            }
                        }.getOrNull()
                    }

                    val results = awaitAll(providerDef, storeDef, propDef, userDef, joinReqDef)
                    results.firstOrNull { it != null }
                }
                onResult(match)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    fun verifyRestorePassword(cleanPhone: String, accountType: String, passwordInput: String, onResult: (Boolean) -> Unit) {
        val trimmedInput = passwordInput.trim()
        val normalizedPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(cleanPhone).ifBlank { cleanPhone.trim() }
        if (trimmedInput.isBlank() || normalizedPhone.isBlank()) {
            onResult(false)
            return
        }
        val primaryCol = when (accountType.uppercase().trim()) {
            "PROVIDER" -> "providers"
            "STORE", "RESTAURANT", "MEDICAL" -> "stores"
            "PROPERTY" -> "properties"
            "JOB" -> "jobs"
            "CLIENT" -> "users"
            else -> "join_requests"
        }
        recoveryScope.launch {
            try {
                val isVerified = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    val candidateCols = listOf(primaryCol, "join_requests", "pending_providers", "registered_users", "users").distinct()
                    val phoneVariants = listOf(normalizedPhone, cleanPhone.trim()).filter { it.isNotBlank() }.distinct()
                    for (col in candidateCols) {
                        for (ph in phoneVariants) {
                            val snap = runCatching {
                                com.google.android.gms.tasks.Tasks.await(
                                    db.collection(col).whereEqualTo("phone", ph).get()
                                )
                            }.getOrNull() ?: continue
                            for (doc in snap.documents) {
                                val storedHash = doc.getString("passwordHash")?.takeIf { it.isNotBlank() }
                                    ?: doc.getString("password")?.takeIf { it.isNotBlank() }
                                    ?: ""
                                if (storedHash.isNotBlank()) {
                                    val matched = com.example.utils.PasswordHasher.verifyPassword(trimmedInput, storedHash) ||
                                        com.example.utils.SecurityCryptoUtils.verifyAdminPassword(trimmedInput, storedHash)
                                    if (matched) return@withContext true
                                }
                            }
                        }
                    }
                    false
                }
                onResult(isVerified)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun requestPasswordReset(
        context: Context,
        phone: String,
        name: String,
        accountType: String,
        onPasswordWaitingPhoneSet: (String) -> Unit,
        triggerNotification: (String) -> Unit,
        onResult: (Boolean) -> Unit
    ) {
        val cleanPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(phone)
        if (cleanPhone.isBlank()) {
            onResult(false)
            return
        }
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val now = System.currentTimeMillis()
        val reqData = mapOf(
            "id" to cleanPhone,
            "uid" to currentUid,
            "phone" to cleanPhone,
            "name" to name,
            "accountType" to accountType,
            "status" to "PENDING",
            "requestedAt" to now,
            "timestamp" to now,
            "newPassword" to "",
            "adminNotes" to ""
        )
        db.collection("password_recovery_requests").document(cleanPhone).set(reqData).addOnSuccessListener {
            onPasswordWaitingPhoneSet(cleanPhone)
            preferenceHelper.setPasswordRecoveryWaitingPhone(context, cleanPhone)
            val adminNotif = NotificationEntity(
                id = "PWD_NOTIF_$cleanPhone",
                title = "🔑 طلب استعادة كلمة مرور ($name)",
                message = "قدم $name ($accountType) ذو الرقم $cleanPhone طلباً لاستعادة وتعيين كلمة المرور.",
                targetType = "SUPERVISOR",
                targetValue = "ALL",
                timestamp = now,
                dedupKey = "PWD_RESET_${cleanPhone}"
            )
            try { if (adminNotif.isValid()) db.collection("notifications").document(adminNotif.id).set(adminNotif) } catch (e: Exception) {}
            
            // Log in activity_logs
            val logId = db.collection("activity_logs").document().id
            val log = com.example.data.ActivityLogEntity(
                id = logId,
                action = "🔑 طلب استعادة كلمة مرور للحساب: $name ($cleanPhone - $accountType)",
                timestamp = now
            )
            db.collection("activity_logs").document(logId).set(log)

            triggerNotification("🔑 طلب استعادة كلمة مرور جديد من: $name ($cleanPhone)")
            onResult(true)
        }.addOnFailureListener {
            onResult(false)
        }
    }

    fun adminResolvePasswordReset(
        context: Context,
        phone: String,
        newPassword: String,
        onResult: (Boolean) -> Unit
    ) {
        val cleanPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(phone)
        if (cleanPhone.isBlank() || newPassword.isBlank()) {
            onResult(false)
            return
        }
        val now = System.currentTimeMillis()
        val hashedPassword = com.example.utils.PasswordHasher.hash(newPassword.trim())
        val updates = mapOf(
            "id" to cleanPhone,
            "phone" to cleanPhone,
            "status" to "RESOLVED",
            "newPassword" to hashedPassword,
            "resolvedAt" to now,
            "timestamp" to now
        )
        db.collection("password_recovery_requests").document(cleanPhone)
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                db.collection("password_resets").document(cleanPhone).set(
                    mapOf(
                        "phone" to cleanPhone,
                        "status" to "APPROVED",
                        "newPassword" to hashedPassword,
                        "tempPassword" to hashedPassword,
                        "resolvedAt" to now
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                val passUpdate = mapOf(
                    "password" to hashedPassword,
                    "passwordHash" to hashedPassword
                )
                val phoneQueries = listOf(cleanPhone, "0$cleanPhone", "+967$cleanPhone", "967$cleanPhone").distinct()
                val collections = listOf("providers", "stores", "properties", "users", "registered_users", "join_requests", "pending_providers")
                
                for (col in collections) {
                    for (ph in phoneQueries) {
                        db.collection(col).whereEqualTo("phone", ph).get().addOnSuccessListener { snaps ->
                            for (doc in snaps.documents) {
                                doc.reference.update(passUpdate)
                            }
                        }
                    }
                }

                // Log sensitive admin operation in activity_logs
                val logId = db.collection("activity_logs").document().id
                val log = com.example.data.ActivityLogEntity(
                    id = logId,
                    action = "🔑 إعادة تعيين كلمة المرور برقم الهاتف: $cleanPhone بواسطة الإدارة",
                    timestamp = now
                )
                db.collection("activity_logs").document(logId).set(log)

                val notifId = "PWD_RESOLVE_$cleanPhone"
                val notif = mapOf(
                    "id" to notifId,
                    "title" to "🔑 تم إعادة تعيين كلمة المرور",
                    "message" to "تم إعادة تعيين كلمة مرور حسابك بنجاح من قبل الإدارة.",
                    "targetPhone" to cleanPhone,
                    "targetType" to "USER",
                    "targetValue" to cleanPhone,
                    "timestamp" to now,
                    "dedupKey" to "PWD_RESOLVE_$cleanPhone"
                )
                if (notif.isValid()) db.collection("notifications").document(notifId).set(notif)
                onResult(true)
            }.addOnFailureListener {
                onResult(false)
            }
    }
}
