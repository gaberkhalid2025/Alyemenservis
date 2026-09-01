package com.example.data.repositories.impl

import android.content.Context
import android.util.Log
import com.example.data.ProviderEntity
import com.example.data.repositories.contracts.IProviderRepository
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
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

class ProviderRepositoryImpl(
    private val context: Context?,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IProviderRepository {

    private val listeners = mutableListOf<ListenerRegistration>()
    private val providersCollection = firestore.collection("providers")

    companion object {
        private const val TAG = "ProviderRepositoryImpl"
    }

    override fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    override fun observeApprovedProviders(): Flow<List<ProviderEntity>> = callbackFlow {
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
        }
    }.flowOn(Dispatchers.IO)

    override fun observeProviderById(providerId: String): Flow<ProviderEntity?> = callbackFlow {
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
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getProvidersByCategoryAndCity(categoryId: String, cityId: String): AppResult<List<ProviderEntity>> = withContext(Dispatchers.IO) {
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
            Log.e(TAG, "Error getting providers", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل جلب الفنيين"))
        }
    }

    override suspend fun saveOrUpdateProvider(provider: ProviderEntity): AppResult<ProviderEntity> = withContext(Dispatchers.IO) {
        try {
            val docId = provider.id.ifBlank { providersCollection.document().id }
            val finalProvider = provider.copy(id = docId)

            providersCollection.document(docId).set(finalProvider, SetOptions.merge()).await()
            Result.success(finalProvider)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving provider", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حفظ بيانات الفني"))
        }
    }

    override suspend fun updateAvailability(providerId: String, isAvailable: Boolean): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            providersCollection.document(providerId).update("isAvailable", isAvailable).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating provider availability $providerId", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تحديث حالة التوفر"))
        }
    }

    override suspend fun deleteProvider(providerId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            providersCollection.document(providerId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting provider $providerId", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حذف الفني"))
        }
    }
}
