package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.StoreEntity
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
 * 🏬 StoreRepository
 * مستودع إدارة المتاجر، المحال التجارية، المطاعم، والمراكز الطبية (stores)
 */
class StoreRepository(
    private val context: Context? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val listeners = mutableListOf<ListenerRegistration>()
    private val storesCollection = firestore.collection("stores")

    companion object {
        private const val TAG = "StoreRepository"
    }

    fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
            Log.d(TAG, "All StoreRepository listeners cleared safely")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    /**
     * مراقبة جميع المتاجر والأنشطة التجارية
     */
    fun observeAllStores(): Flow<List<StoreEntity>> = callbackFlow {
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
            listeners.remove(listener)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * مراقبة قسم معين (stores, restaurants, medical)
     */
    fun observeStoresBySection(sectionId: String): Flow<List<StoreEntity>> = callbackFlow {
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
            listeners.remove(listener)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * جلب متجر بواسطة المعرف
     */
    suspend fun getStoreById(storeId: String): Result<StoreEntity?> = withContext(Dispatchers.IO) {
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
            Result.failure(e)
        }
    }

    /**
     * حفظ أو تحديث بيانات متجر/منشأة
     */
    suspend fun saveOrUpdateStore(store: StoreEntity): Result<StoreEntity> = withContext(Dispatchers.IO) {
        try {
            val docId = if (store.id.isNotBlank()) store.id else storesCollection.document().id
            val finalStore = store.copy(id = docId)

            storesCollection.document(docId).set(finalStore, SetOptions.merge()).await()
            Result.success(finalStore)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving store", e)
            Result.failure(e)
        }
    }

    /**
     * حذف متجر
     */
    suspend fun deleteStore(storeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            storesCollection.document(storeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting store $storeId", e)
            Result.failure(e)
        }
    }
}
