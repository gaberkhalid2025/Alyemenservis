package com.example.data.repositories.impl

import android.content.Context
import android.util.Log
import com.example.data.AdminSettingsEntity
import com.example.data.repositories.contracts.ISettingsRepository
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

class SettingsRepositoryImpl(
    private val context: Context?,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ISettingsRepository {

    private val listeners = mutableListOf<ListenerRegistration>()
    private val settingsCollection = firestore.collection("admin_settings")

    companion object {
        private const val TAG = "SettingsRepositoryImpl"
        private const val SETTINGS_DOC_ID = "main_settings"
    }

    override fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    override fun observeSettings(): Flow<AdminSettingsEntity> = callbackFlow {
        val listener = settingsCollection.document(SETTINGS_DOC_ID).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing admin settings", error)
                return@addSnapshotListener
            }
            val settings = if (snapshot != null && snapshot.exists()) {
                snapshot.toObject(AdminSettingsEntity::class.java)?.copy(id = snapshot.id) ?: AdminSettingsEntity()
            } else {
                AdminSettingsEntity()
            }
            trySend(settings)
        }
        listeners.add(listener)

        awaitClose {
            listener.remove()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getSettings(): AppResult<AdminSettingsEntity> = withContext(Dispatchers.IO) {
        try {
            val doc = settingsCollection.document(SETTINGS_DOC_ID).get().await()
            val settings = if (doc.exists()) {
                doc.toObject(AdminSettingsEntity::class.java)?.copy(id = doc.id) ?: AdminSettingsEntity()
            } else {
                AdminSettingsEntity()
            }
            Result.success(settings)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching settings", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل جلب الإعدادات"))
        }
    }

    override suspend fun saveSettings(settings: AdminSettingsEntity): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            settingsCollection.document(SETTINGS_DOC_ID).set(settings, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving settings", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حفظ الإعدادات"))
        }
    }

    override suspend fun updatePartialSettings(updates: Map<String, Any>): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            settingsCollection.document(SETTINGS_DOC_ID).set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating partial settings", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تحديث الإعدادات الجزئية"))
        }
    }
}
