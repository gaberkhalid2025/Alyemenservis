package com.example.data.repositories

import com.example.utils.*

import android.content.Context
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.local.MapDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class RealtimeSyncEvent {
    data class NewItemAdded(val title: String, val category: String) : RealtimeSyncEvent()
}

class RealtimeSyncRepository(context: Context) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val mapDatabase: MapDatabase = MapDatabase.getInstance(context)

    fun observeProvidersRealtime(onNewItemDetected: ((String) -> Unit)? = null): Flow<List<ProviderEntity>> = callbackFlow {
        var previousCount = -1
        val listener: ListenerRegistration = firestore.collection("providers")
            .whereEqualTo("isDeleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    val cached = mapDatabase.mapDao.getProviders()
                    trySend(cached)
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ProviderEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (previousCount in 0 until list.size) {
                    val newest = list.lastOrNull()
                    if (newest != null) {
                        onNewItemDetected?.invoke("فني جديد: ${newest.name}")
                    }
                }
                previousCount = list.size

                mapDatabase.mapDao.saveProviders(list)
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    fun observeStoresRealtime(onNewItemDetected: ((String) -> Unit)? = null): Flow<List<StoreEntity>> = callbackFlow {
        var previousCount = -1
        val listener: ListenerRegistration = firestore.collection("stores")
            .whereEqualTo("isDeleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    val cached = mapDatabase.mapDao.getStores()
                    trySend(cached)
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(StoreEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (previousCount in 0 until list.size) {
                    val newest = list.lastOrNull()
                    if (newest != null) {
                        val isRest = newest.sectionId.contains("restaurant", ignoreCase = true) ||
                                newest.name.contains("مطعم") || newest.name.contains("كافيه")
                        val label = if (isRest) "مطعم جديد: ${newest.name}" else "محل جديد: ${newest.name}"
                        onNewItemDetected?.invoke(label)
                    }
                }
                previousCount = list.size

                mapDatabase.mapDao.saveStores(list)
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    fun observePropertiesRealtime(onNewItemDetected: ((String) -> Unit)? = null): Flow<List<PropertyEntity>> = callbackFlow {
        var previousCount = -1
        val listener: ListenerRegistration = firestore.collection("properties")
            .whereEqualTo("isDeleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    val cached = mapDatabase.mapDao.getProperties()
                    trySend(cached)
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(PropertyEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (previousCount in 0 until list.size) {
                    val newest = list.lastOrNull()
                    if (newest != null) {
                        onNewItemDetected?.invoke("عقار جديد: ${newest.title}")
                    }
                }
                previousCount = list.size

                mapDatabase.mapDao.saveProperties(list)
                trySend(list)
            }

        awaitClose { listener.remove() }
    }
}
