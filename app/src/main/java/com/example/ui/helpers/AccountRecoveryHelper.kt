package com.example.ui.helpers

import android.content.Context
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.MainViewModel.RestoreAccountMatch
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Helper class for searching accounts and managing password recovery / reset operations.
 */
class AccountRecoveryHelper(
    private val db: FirebaseFirestore,
    private val preferenceHelper: AppPreferenceHelper
) {

    fun searchAccountForRestore(cleanPhone: String, onResult: (RestoreAccountMatch?) -> Unit) {
        db.collection("providers").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { providerSnap ->
            val pDoc = providerSnap.documents.firstOrNull()
            val provider = pDoc?.toObject(ProviderEntity::class.java)
            if (provider != null) {
                val pass = pDoc.getString("password") ?: pDoc.getString("passwordHash") ?: ""
                onResult(RestoreAccountMatch("PROVIDER", provider.name, provider = provider, savedPassword = pass))
                return@addOnSuccessListener
            }

            db.collection("stores").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { storeSnap ->
                val sDoc = storeSnap.documents.firstOrNull()
                val store = sDoc?.toObject(StoreEntity::class.java)
                if (store != null) {
                    val pass = sDoc.getString("password") ?: sDoc.getString("passwordHash") ?: ""
                    onResult(RestoreAccountMatch("STORE", store.name, store = store, savedPassword = pass))
                    return@addOnSuccessListener
                }

                db.collection("properties").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { propSnap ->
                    val prDoc = propSnap.documents.firstOrNull()
                    val property = prDoc?.toObject(PropertyEntity::class.java)
                    if (property != null) {
                        val pass = prDoc.getString("password") ?: prDoc.getString("passwordHash") ?: ""
                        onResult(RestoreAccountMatch("PROPERTY", property.title, property = property, savedPassword = pass))
                        return@addOnSuccessListener
                    }

                    db.collection("users").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { userSnap ->
                        val uDoc = userSnap.documents.firstOrNull()
                        if (uDoc != null) {
                            val uName = uDoc.getString("name") ?: "مستخدم مسجل"
                            val pass = uDoc.getString("password") ?: ""
                            onResult(RestoreAccountMatch("CLIENT", uName, savedPassword = pass))
                            return@addOnSuccessListener
                        }

                        db.collection("join_requests").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { reqSnap ->
                            val rDoc = reqSnap.documents.firstOrNull()
                            if (rDoc != null) {
                                val rName = rDoc.getString("name") ?: "حساب مسجل"
                                val rType = rDoc.getString("type") ?: "CLIENT"
                                val pass = rDoc.getString("password") ?: ""
                                onResult(RestoreAccountMatch(rType, rName, savedPassword = pass))
                                return@addOnSuccessListener
                            }
                            onResult(null)
                        }.addOnFailureListener { onResult(null) }
                    }.addOnFailureListener { onResult(null) }
                }.addOnFailureListener { onResult(null) }
            }.addOnFailureListener { onResult(null) }
        }.addOnFailureListener {
            onResult(null)
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
        val cleanPhone = phone.trim().replace(" ", "")
        val reqData = mapOf(
            "id" to cleanPhone,
            "phone" to cleanPhone,
            "name" to name,
            "accountType" to accountType,
            "status" to "PENDING",
            "requestedAt" to System.currentTimeMillis(),
            "newPassword" to "",
            "adminNotes" to ""
        )
        db.collection("password_recovery_requests").document(cleanPhone).set(reqData).addOnSuccessListener {
            onPasswordWaitingPhoneSet(cleanPhone)
            preferenceHelper.setPasswordRecoveryWaitingPhone(context, cleanPhone)
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
        val cleanPhone = phone.trim().replace(" ", "")
        val updates = mapOf(
            "status" to "RESOLVED",
            "newPassword" to newPassword,
            "resolvedAt" to System.currentTimeMillis()
        )
        db.collection("password_recovery_requests").document(cleanPhone).update(updates).addOnSuccessListener {
            db.collection("password_resets").document(cleanPhone).update(
                mapOf(
                    "status" to "APPROVED",
                    "newPassword" to newPassword,
                    "tempPassword" to newPassword
                )
            )
            db.collection("providers").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { snaps ->
                for (doc in snaps.documents) { doc.reference.update("password", newPassword) }
            }
            db.collection("stores").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { snaps ->
                for (doc in snaps.documents) { doc.reference.update("password", newPassword) }
            }
            db.collection("properties").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { snaps ->
                for (doc in snaps.documents) { doc.reference.update("password", newPassword) }
            }
            db.collection("users").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { snaps ->
                for (doc in snaps.documents) { doc.reference.update("password", newPassword) }
            }
            db.collection("registered_users").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { snaps ->
                for (doc in snaps.documents) { doc.reference.update("password", newPassword) }
            }
            db.collection("join_requests").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { snaps ->
                for (doc in snaps.documents) { doc.reference.update("password", newPassword) }
            }
            val notif = mapOf(
                "id" to "notif_pwd_${System.currentTimeMillis()}",
                "title" to "🔑 تم إعادة تعيين كلمة المرور",
                "message" to "تم إعادة تعيين كلمة المرور الخاصة بك بنجاح. كلمة المرور الجديدة: $newPassword",
                "targetPhone" to cleanPhone,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("notifications").add(notif)
            onResult(true)
        }.addOnFailureListener {
            onResult(false)
        }
    }
}
