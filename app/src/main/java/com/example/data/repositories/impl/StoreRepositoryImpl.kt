package com.example.data.repositories.impl

import android.content.Context
import android.util.Log
import com.example.data.StoreEntity
import com.example.data.repositories.contracts.IStoreRepository
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

class StoreRepositoryImpl(
    private val context: Context?,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IStoreRepository {

    private val listeners = mutableListOf<ListenerRegistration>()
    private val storesCollection = firestore.collection("stores")

    companion object {
        private const val TAG = "StoreRepositoryImpl"
    }

    override fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    override fun observeAllStores(): Flow<List<StoreEntity>> = callbackFlow {
        val listener = storesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing stores", error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(StoreEntity::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        listeners.add(listener)

        awaitClose {
            listener.remove()
        }
    }.flowOn(Dispatchers.IO)

    override fun observeStoresBySection(sectionId: String): Flow<List<StoreEntity>> = callbackFlow {
        val listener = storesCollection
            .whereEqualTo("sectionId", sectionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing stores for section $sectionId", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(StoreEntity::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        listeners.add(listener)

        awaitClose {
            listener.remove()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getStoreById(storeId: String): AppResult<StoreEntity?> = withContext(Dispatchers.IO) {
        try {
            if (storeId.isBlank()) return@withContext Result.success(null)
            val doc = storesCollection.document(storeId).get().await()
            if (doc.exists()) {
                val store = doc.toObject(StoreEntity::class.java)?.copy(id = doc.id)
                Result.success(store)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching store $storeId", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل جلب المتجر"))
        }
    }

    override suspend fun saveOrUpdateStore(store: StoreEntity): AppResult<StoreEntity> = withContext(Dispatchers.IO) {
        try {
            val docId = store.id.ifBlank { storesCollection.document().id }
            val finalStore = store.copy(id = docId)

            storesCollection.document(docId).set(finalStore, SetOptions.merge()).await()
            Result.success(finalStore)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving store", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حفظ بيانات المتجر"))
        }
    }

    override suspend fun deleteStore(storeId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            storesCollection.document(storeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting store $storeId", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حذف المتجر"))
        }
    }
}
