package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.PropertyEntity
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
 * 🏠 PropertyRepository
 * مستودع إدارة العقارات (الإيجار، البيع، الأراضي، الشقق، المحلات) (properties)
 */
class PropertyRepository(
    private val context: Context? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val listeners = mutableListOf<ListenerRegistration>()
    private val propertiesCollection = firestore.collection("properties")

    companion object {
        private const val TAG = "PropertyRepository"
    }

    fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
            Log.d(TAG, "All PropertyRepository listeners cleared safely")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    /**
     * مراقبة جميع العقارات المتاحة
     */
    fun observeAllProperties(): Flow<List<PropertyEntity>> = callbackFlow {
        val listener = propertiesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing properties", error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(PropertyEntity::class.java)?.copy(id = doc.id)
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
     * مراقبة العقارات حسب نوع العملية (rent / sale)
     */
    fun observePropertiesByType(type: String): Flow<List<PropertyEntity>> = callbackFlow {
        val listener = propertiesCollection
            .whereEqualTo("type", type)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing properties by type $type", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PropertyEntity::class.java)?.copy(id = doc.id)
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
     * جلب عقار بالمعرف
     */
    suspend fun getPropertyById(propertyId: String): Result<PropertyEntity?> = withContext(Dispatchers.IO) {
        try {
            if (propertyId.isBlank()) return@withContext Result.success(null)
            val doc = propertiesCollection.document(propertyId).get().await()
            if (doc.exists()) {
                val prop = doc.toObject(PropertyEntity::class.java)?.copy(id = doc.id)
                Result.success(prop)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching property $propertyId", e)
            Result.failure(e)
        }
    }

    /**
     * حفظ أو تحديث عقار
     */
    suspend fun saveOrUpdateProperty(property: PropertyEntity): Result<PropertyEntity> = withContext(Dispatchers.IO) {
        try {
            val docId = if (property.id.isNotBlank()) property.id else propertiesCollection.document().id
            val finalProperty = property.copy(id = docId)

            propertiesCollection.document(docId).set(finalProperty, SetOptions.merge()).await()
            Result.success(finalProperty)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving property", e)
            Result.failure(e)
        }
    }

    /**
     * حذف عقار
     */
    suspend fun deleteProperty(propertyId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            propertiesCollection.document(propertyId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting property $propertyId", e)
            Result.failure(e)
        }
    }
}
