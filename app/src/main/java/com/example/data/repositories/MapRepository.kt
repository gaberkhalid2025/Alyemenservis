package com.example.data.repositories

import android.content.Context
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.local.MapDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MapRepository(context: Context) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val mapDatabase: MapDatabase = MapDatabase.getInstance(context)

    suspend fun getProviders(): List<ProviderEntity> {
        return try {
            val snapshot = firestore.collection("providers")
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ProviderEntity::class.java)?.copy(id = doc.id)
            }
            if (list.isNotEmpty()) {
                mapDatabase.mapDao.saveProviders(list)
                list
            } else {
                mapDatabase.mapDao.getProviders()
            }
        } catch (e: Exception) {
            mapDatabase.mapDao.getProviders()
        }
    }

    suspend fun getStores(): List<StoreEntity> {
        return try {
            val snapshot = firestore.collection("stores")
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(StoreEntity::class.java)?.copy(id = doc.id)
            }
            if (list.isNotEmpty()) {
                mapDatabase.mapDao.saveStores(list)
                list
            } else {
                mapDatabase.mapDao.getStores()
            }
        } catch (e: Exception) {
            mapDatabase.mapDao.getStores()
        }
    }

    suspend fun getProperties(): List<PropertyEntity> {
        return try {
            val snapshot = firestore.collection("properties")
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(PropertyEntity::class.java)?.copy(id = doc.id)
            }
            if (list.isNotEmpty()) {
                mapDatabase.mapDao.saveProperties(list)
                list
            } else {
                mapDatabase.mapDao.getProperties()
            }
        } catch (e: Exception) {
            mapDatabase.mapDao.getProperties()
        }
    }
}
