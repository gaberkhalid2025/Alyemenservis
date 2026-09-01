package com.example.data.repositories.impl

import android.content.Context
import android.util.Log
import com.example.data.PropertyEntity
import com.example.data.repositories.contracts.IPropertyRepository
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

class PropertyRepositoryImpl(
    private val context: Context?,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IPropertyRepository {

    private val listeners = mutableListOf<ListenerRegistration>()
    private val propertiesCollection = firestore.collection("properties")

    companion object {
        private const val TAG = "PropertyRepositoryImpl"
    }

    override fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    override fun observeAllProperties(): Flow<List<PropertyEntity>> = callbackFlow {
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
        }
    }.flowOn(Dispatchers.IO)

    override fun observePropertiesByType(type: String): Flow<List<PropertyEntity>> = callbackFlow {
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
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getPropertyById(propertyId: String): AppResult<PropertyEntity?> = withContext(Dispatchers.IO) {
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
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل جلب العقار"))
        }
    }

    override suspend fun saveOrUpdateProperty(property: PropertyEntity): AppResult<PropertyEntity> = withContext(Dispatchers.IO) {
        try {
            val docId = property.id.ifBlank { propertiesCollection.document().id }
            val finalProperty = property.copy(id = docId)

            propertiesCollection.document(docId).set(finalProperty, SetOptions.merge()).await()
            Result.success(finalProperty)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving property", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حفظ بيانات العقار"))
        }
    }

    override suspend fun deleteProperty(propertyId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            propertiesCollection.document(propertyId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting property $propertyId", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حذف العقار"))
        }
    }
}
