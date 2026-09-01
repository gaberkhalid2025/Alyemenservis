package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.UserEntity
import com.example.data.models.FcmTokenEntity
import com.example.util.PasswordHasher
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 🔐 AuthRepository
 * مستودع إدارة المصادقة والمستخدمين وحسابات الدخول ومزامنة FCM
 */
class AuthRepository(
    private val context: Context? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val listeners = mutableListOf<ListenerRegistration>()
    private val usersCollection = firestore.collection("users")
    private val fcmTokensCollection = firestore.collection("fcm_tokens")

    companion object {
        private const val TAG = "AuthRepository"
    }

    /**
     * تنظيف جميع المستمعين لمنع تسريب الذاكرة
     */
    fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
            Log.d(TAG, "All AuthRepository listeners cleared safely")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    /**
     * تسجيل الدخول برقم الهاتف وكلمة المرور / الرمز السري
     */
    suspend fun loginWithPhone(phone: String, pinOrPass: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
            if (cleanPhone.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("رقم الهاتف مطلوب"))
            }

            val querySnapshot = usersCollection
                .whereEqualTo("phone", cleanPhone)
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return@withContext Result.failure(NoSuchElementException("المستخدم غير مسجل"))
            }

            val doc = querySnapshot.documents.first()
            val user = doc.toObject(UserEntity::class.java)?.copy(id = doc.id)
                ?: return@withContext Result.failure(IllegalStateException("تعذر قراءة بيانات الحساب"))

            if (user.isBlocked) {
                return@withContext Result.failure(IllegalStateException("تم حظر هذا الحساب من قبل الإدارة"))
            }

            val storedPasswordHash = doc.getString("passwordHash") ?: ""
            val storedPassword = doc.getString("password") ?: ""

            val isValid = when {
                storedPasswordHash.isNotBlank() -> PasswordHasher.verifyPassword(pinOrPass, storedPasswordHash)
                storedPassword.isNotBlank() -> storedPassword == pinOrPass
                else -> true // If no password was required previously
            }

            if (!isValid) {
                return@withContext Result.failure(IllegalArgumentException("كلمة المرور أو الرمز السري غير صحيح"))
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed for phone $phone", e)
            Result.failure(e)
        }
    }

    /**
     * إنشاء أو تحديث حساب مستخدم
     */
    suspend fun saveOrUpdateUser(user: UserEntity, passwordRaw: String = ""): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val docId = if (user.id.isNotBlank()) user.id else usersCollection.document().id
            val updatedUser = user.copy(id = docId)
            val dataMap = mutableMapOf<String, Any>(
                "id" to docId,
                "name" to updatedUser.name,
                "phone" to updatedUser.phone,
                "email" to updatedUser.email,
                "city" to updatedUser.city,
                "neighborhood" to updatedUser.neighborhood,
                "role" to updatedUser.role,
                "isBlocked" to updatedUser.isBlocked,
                "totalBookings" to updatedUser.totalBookings,
                "rating" to updatedUser.rating,
                "createdAt" to updatedUser.createdAt
            )

            if (passwordRaw.isNotBlank()) {
                val hashed = PasswordHasher.createSaltedHash(passwordRaw)
                dataMap["passwordHash"] = hashed
                dataMap["password"] = passwordRaw // For backwards compatibility
            }

            usersCollection.document(docId).set(dataMap, SetOptions.merge()).await()
            Result.success(updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save or update user", e)
            Result.failure(e)
        }
    }

    /**
     * جلب مستخدم بواسطة المعرف
     */
    suspend fun getUserById(userId: String): Result<UserEntity?> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank()) return@withContext Result.success(null)
            val doc = usersCollection.document(userId).get().await()
            if (doc.exists()) {
                val user = doc.toObject(UserEntity::class.java)?.copy(id = doc.id)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user $userId", e)
            Result.failure(e)
        }
    }

    /**
     * جلب مستخدم بواسطة رقم الهاتف
     */
    suspend fun getUserByPhone(phone: String): Result<UserEntity?> = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
            if (cleanPhone.isBlank()) return@withContext Result.success(null)
            val snap = usersCollection.whereEqualTo("phone", cleanPhone).limit(1).get().await()
            if (!snap.isEmpty) {
                val doc = snap.documents.first()
                val user = doc.toObject(UserEntity::class.java)?.copy(id = doc.id)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user by phone $phone", e)
            Result.failure(e)
        }
    }

    /**
     * تدفق حي لمراقبة بيانات المستخدم
     */
    fun observeUser(userId: String): Flow<UserEntity?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = usersCollection.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error in observeUser snapshot listener", error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(UserEntity::class.java)?.copy(id = snapshot.id)
                trySend(user)
            } else {
                trySend(null)
            }
        }
        listeners.add(listener)

        awaitClose {
            listener.remove()
            listeners.remove(listener)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * تحديث توكن الإشعارات السحابية FCM
     */
    suspend fun updateFcmToken(userId: String, phone: String, token: String, role: String = "CLIENT"): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (token.isBlank()) return@withContext Result.success(Unit)

            val fcmEntity = FcmTokenEntity(
                token = token,
                phone = phone,
                role = role,
                updatedAt = System.currentTimeMillis()
            )

            // Save in fcm_tokens collection
            val tokenDocId = if (userId.isNotBlank()) userId else phone
            if (tokenDocId.isNotBlank()) {
                fcmTokensCollection.document(tokenDocId).set(fcmEntity, SetOptions.merge()).await()
            }

            // Also update inside users collection if userId exists
            if (userId.isNotBlank()) {
                usersCollection.document(userId).set(
                    mapOf("fcmToken" to token, "fcmUpdatedAt" to System.currentTimeMillis()),
                    SetOptions.merge()
                ).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update FCM token", e)
            Result.failure(e)
        }
    }

    /**
     * استرجاع وتعيين كلمة مرور جديدة بعد التحقق
     */
    suspend fun resetPassword(phone: String, newPasswordRaw: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
            val snap = usersCollection.whereEqualTo("phone", cleanPhone).limit(1).get().await()
            if (snap.isEmpty) {
                return@withContext Result.failure(NoSuchElementException("لم يتم العثور على حساب بهذا الرقم"))
            }

            val docId = snap.documents.first().id
            val hashed = PasswordHasher.createSaltedHash(newPasswordRaw)

            usersCollection.document(docId).set(
                mapOf(
                    "passwordHash" to hashed,
                    "password" to newPasswordRaw,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset password", e)
            Result.failure(e)
        }
    }
}
