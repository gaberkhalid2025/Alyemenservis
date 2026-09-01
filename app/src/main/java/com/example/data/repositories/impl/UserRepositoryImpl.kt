package com.example.data.repositories.impl

import android.content.Context
import android.util.Log
import com.example.data.repositories.contracts.IUserRepository
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
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

class UserRepositoryImpl(
    private val context: Context?,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IUserRepository {

    private val listeners = mutableListOf<ListenerRegistration>()
    private val usersCollection = firestore.collection("users")
    private val favoritesCollection = firestore.collection("user_favorites")

    companion object {
        private const val TAG = "UserRepositoryImpl"
    }

    override fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    override fun observeUserFavorites(userId: String): Flow<Set<String>> = callbackFlow {
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
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun toggleFavorite(userId: String, itemId: String): AppResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank() || itemId.isBlank()) {
                return@withContext Result.failure(AppError.ValidationError("userId/itemId", "معرف المستخدم والعنصر مطلوبان"))
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
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تبديل المفضلة"))
        }
    }

    override suspend fun isFavorite(userId: String, itemId: String): AppResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank() || itemId.isBlank()) return@withContext Result.success(false)
            val doc = favoritesCollection.document(userId).get().await()
            val list = (doc.get("itemIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            Result.success(list.contains(itemId))
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل فحص المفضلة"))
        }
    }

    override suspend fun updateUserProfile(userId: String, name: String, city: String, neighborhood: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank()) return@withContext Result.failure(AppError.ValidationError("userId", "معرف المستخدم مطلوب"))

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
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تحديث الملف الشخصي"))
        }
    }

    override suspend fun adjustUserPoints(userId: String, deltaPoints: Int): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank()) return@withContext Result.failure(AppError.ValidationError("userId", "معرف المستخدم مطلوب"))

            usersCollection.document(userId).update(
                "points", FieldValue.increment(deltaPoints.toLong())
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adjusting user points for $userId", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تحديث النقاط"))
        }
    }

    override suspend fun updateUserLocation(userId: String, lat: Double, lng: Double): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank()) return@withContext Result.failure(AppError.ValidationError("userId", "معرف المستخدم مطلوب"))

            usersCollection.document(userId).update(
                mapOf(
                    "latitude" to lat,
                    "longitude" to lng,
                    "locationUpdatedAt" to System.currentTimeMillis()
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تحديث موقع المستخدم"))
        }
    }

    override fun startLocationUpdates(userId: String): Flow<Pair<Double, Double>> = callbackFlow {
        if (userId.isBlank()) {
            close()
            return@callbackFlow
        }
        val listener = usersCollection.document(userId).addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val lat = snapshot.getDouble("latitude") ?: 0.0
                val lng = snapshot.getDouble("longitude") ?: 0.0
                trySend(Pair(lat, lng))
            }
        }
        listeners.add(listener)
        awaitClose { listener.remove() }
    }
}
