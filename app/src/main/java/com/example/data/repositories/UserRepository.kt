package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.UserEntity
import com.google.firebase.firestore.FieldValue
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
 * 👤 UserRepository
 * مستودع إدارة بيانات المستخدم، المفضلة، نقاط المكافآت، والملف الشخصي
 */
class UserRepository(
    private val context: Context? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val listeners = mutableListOf<ListenerRegistration>()
    private val usersCollection = firestore.collection("users")
    private val favoritesCollection = firestore.collection("user_favorites")

    companion object {
        private const val TAG = "UserRepository"
    }

    fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
            Log.d(TAG, "All UserRepository listeners cleared safely")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    /**
     * مراقبة قائمة المعرفات المفضلة للمستخدم
     */
    fun observeUserFavorites(userId: String): Flow<Set<String>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }

        val listener = favoritesCollection.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing favorites for user $userId", error)
                return@addSnapshotListener
            }
            val list = snapshot?.get("itemIds") as? List<*>
            val set = list?.filterIsInstance<String>()?.toSet() ?: emptySet()
            trySend(set)
        }
        listeners.add(listener)

        awaitClose {
            listener.remove()
            listeners.remove(listener)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * تبديل حالة إضافة/حذف عنصر من المفضلة
     */
    suspend fun toggleFavorite(userId: String, itemId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank() || itemId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("معرف المستخدم والعنصر مطلوبان"))
            }

            val docRef = favoritesCollection.document(userId)
            val doc = docRef.get().await()

            val currentList = (doc.get("itemIds") as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
            val isNowFavorite: Boolean

            if (currentList.contains(itemId)) {
                currentList.remove(itemId)
                isNowFavorite = false
                docRef.update("itemIds", FieldValue.arrayRemove(itemId)).await()
            } else {
                currentList.add(itemId)
                isNowFavorite = true
                docRef.set(mapOf("itemIds" to FieldValue.arrayUnion(itemId)), SetOptions.merge()).await()
            }

            Result.success(isNowFavorite)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling favorite $itemId for user $userId", e)
            Result.failure(e)
        }
    }

    /**
     * تحديث بيانات الملف الشخصي
     */
    suspend fun updateUserProfile(userId: String, name: String, city: String, neighborhood: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("معرف المستخدم مطلوب"))

            usersCollection.document(userId).update(
                mapOf(
                    "name" to name,
                    "city" to city,
                    "neighborhood" to neighborhood,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile $userId", e)
            Result.failure(e)
        }
    }

    /**
     * زيادة أو خصم نقاط المستخدم
     */
    suspend fun adjustUserPoints(userId: String, deltaPoints: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("معرف المستخدم مطلوب"))

            usersCollection.document(userId).update(
                "points", FieldValue.increment(deltaPoints.toLong())
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adjusting user points for $userId", e)
            Result.failure(e)
        }
    }
}
