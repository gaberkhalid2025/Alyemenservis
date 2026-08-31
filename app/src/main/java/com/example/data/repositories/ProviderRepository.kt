package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.ProviderEntity
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
 * 🛠️ ProviderRepository
 * مستودع إدارة الفنيين والمهنيين ومقدمي الخدمات (providers + pending_providers)
 */
class ProviderRepository(
    private val context: Context? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val listeners = mutableListOf<ListenerRegistration>()
    private val providersCollection = firestore.collection("providers")
    private val pendingProvidersCollection = firestore.collection("pending_providers")

    companion object {
        private const val TAG = "ProviderRepository"
    }

    fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
            Log.d(TAG, "All ProviderRepository listeners cleared safely")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    /**
     * مراقبة جميع الفنيين المعتمدين
     */
    fun observeApprovedProviders(): Flow<List<ProviderEntity>> = callbackFlow {
        val listener = providersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing providers", error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ProviderEntity::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        listeners.add(listener)

        awaitClose {
            listener.remove()
            listeners.remove(listener)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * مراقبة فني محدد بالمعرف
     */
    fun observeProviderById(providerId: String): Flow<ProviderEntity?> = callbackFlow {
        if (providerId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = providersCollection.document(providerId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing provider $providerId", error)
                return@addSnapshotListener
            }
            val provider = snapshot?.toObject(ProviderEntity::class.java)?.copy(id = snapshot.id)
            trySend(provider)
        }
        listeners.add(listener)

        awaitClose {
            listener.remove()
            listeners.remove(listener)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * جلب الفنيين حسب القسم والمحافظة
     */
    suspend fun getProvidersByCategoryAndCity(categoryId: String, cityId: String = ""): Result<List<ProviderEntity>> = withContext(Dispatchers.IO) {
        try {
            var query = providersCollection.whereEqualTo("categoryId", categoryId)
            if (cityId.isNotBlank()) {
                query = query.whereEqualTo("cityId", cityId)
            }
            val snap = query.get().await()
            val list = snap.documents.mapNotNull { doc ->
                doc.toObject(ProviderEntity::class.java)?.copy(id = doc.id)
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting providers by category $categoryId and city $cityId", e)
            Result.failure(e)
        }
    }

    /**
     * حفظ أو تحديث بيانات فني
     */
    suspend fun saveOrUpdateProvider(provider: ProviderEntity): Result<ProviderEntity> = withContext(Dispatchers.IO) {
        try {
            val docId = if (provider.id.isNotBlank()) provider.id else providersCollection.document().id
            val finalProvider = provider.copy(id = docId)

            providersCollection.document(docId).set(finalProvider, SetOptions.merge()).await()
            Result.success(finalProvider)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving provider", e)
            Result.failure(e)
        }
    }

    /**
     * تحديث حالة التوفر للفني
     */
    suspend fun updateAvailability(providerId: String, isAvailable: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            providersCollection.document(providerId).update("isAvailable", isAvailable).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating provider availability $providerId", e)
            Result.failure(e)
        }
    }

    /**
     * حذف فني
     */
    suspend fun deleteProvider(providerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            providersCollection.document(providerId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting provider $providerId", e)
            Result.failure(e)
        }
    }
}
