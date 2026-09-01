package com.example.data.repositories.impl

import android.content.Context
import android.util.Log
import com.example.data.UserEntity
import com.example.data.models.FcmTokenEntity
import com.example.data.repositories.contracts.IAuthRepository
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
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

class AuthRepositoryImpl(
    private val context: Context?,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IAuthRepository {

    private val listeners = mutableListOf<ListenerRegistration>()
    private val usersCollection = firestore.collection("users")
    private val fcmTokensCollection = firestore.collection("fcm_tokens")

    companion object {
        private const val TAG = "AuthRepositoryImpl"
    }

    override fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    override suspend fun loginWithPhone(phone: String, pinOrPass: String): AppResult<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
            if (cleanPhone.isBlank()) {
                return@withContext Result.failure(AppError.ValidationError("phone", "رقم الهاتف مطلوب"))
            }

            val querySnapshot = usersCollection
                .whereEqualTo("phone", cleanPhone)
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return@withContext Result.failure(AppError.NotFoundError("المستخدم غير مسجل"))
            }

            val doc = querySnapshot.documents.first()
            val user = doc.toObject(UserEntity::class.java)?.copy(id = doc.id)
                ?: return@withContext Result.failure(AppError.UnknownError("تعذر قراءة بيانات الحساب"))

            if (user.isBlocked) {
                return@withContext Result.failure(AppError.UnauthorizedError("تم حظر هذا الحساب من قبل الإدارة"))
            }

            val storedPasswordHash = doc.getString("passwordHash") ?: ""
            val storedPassword = doc.getString("password") ?: ""

            val isValid = when {
                storedPasswordHash.isNotBlank() -> PasswordHasher.verifyPassword(pinOrPass, storedPasswordHash)
                storedPassword.isNotBlank() -> storedPassword == pinOrPass
                else -> true
            }

            if (!isValid) {
                return@withContext Result.failure(AppError.ValidationError("password", "كلمة المرور أو الرمز السري غير صحيح"))
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed for phone $phone", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تسجيل الدخول"))
        }
    }

    override suspend fun saveOrUpdateUser(user: UserEntity, passwordRaw: String): AppResult<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val docId = user.id.ifBlank { usersCollection.document().id }
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
                dataMap["password"] = passwordRaw
            }

            usersCollection.document(docId).set(dataMap, SetOptions.merge()).await()
            Result.success(updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save or update user", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حفظ بيانات المستخدم"))
        }
    }

    override suspend fun getUserById(userId: String): AppResult<UserEntity?> = withContext(Dispatchers.IO) {
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
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل جلب بيانات المستخدم"))
        }
    }

    override suspend fun getUserByPhone(phone: String): AppResult<UserEntity?> = withContext(Dispatchers.IO) {
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
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل جلب المستخدم برقم الهاتف"))
        }
    }

    override fun observeUser(userId: String): Flow<UserEntity?> = callbackFlow {
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
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun updateFcmToken(userId: String, phone: String, token: String, role: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (token.isBlank()) return@withContext Result.success(Unit)

            val fcmEntity = FcmTokenEntity(
                token = token,
                phone = phone,
                role = role,
                updatedAt = System.currentTimeMillis()
            )

            val tokenDocId = userId.ifBlank { phone }
            if (tokenDocId.isNotBlank()) {
                fcmTokensCollection.document(tokenDocId).set(fcmEntity, SetOptions.merge()).await()
            }

            if (userId.isNotBlank()) {
                usersCollection.document(userId).set(
                    mapOf("fcmToken" to token, "fcmUpdatedAt" to System.currentTimeMillis()),
                    SetOptions.merge()
                ).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update FCM token", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تحديث توكن الإشعارات"))
        }
    }

    override suspend fun resetPassword(phone: String, newPasswordRaw: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
            val snap = usersCollection.whereEqualTo("phone", cleanPhone).limit(1).get().await()
            if (snap.isEmpty) {
                return@withContext Result.failure(AppError.NotFoundError("لم يتم العثور على حساب بهذا الرقم"))
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
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل إعادة تعيين كلمة المرور"))
        }
    }
}
