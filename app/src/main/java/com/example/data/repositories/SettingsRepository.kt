package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.AdminSettingsEntity
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
 * ⚙️ SettingsRepository
 * مستودع إدارة إعدادات المنصة، الثيمات، الألوان، وأزرار الفوتر (admin_settings)
 */
class SettingsRepository(
    private val context: Context? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val listeners = mutableListOf<ListenerRegistration>()
    private val settingsCollection = firestore.collection("admin_settings")

    companion object {
        private const val TAG = "SettingsRepository"
        private const val SETTINGS_DOC_ID = "main_settings"
    }

    fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
            Log.d(TAG, "All SettingsRepository listeners cleared safely")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    /**
     * مراقبة إعدادات التطبيق الحية
     */
    fun observeSettings(): Flow<AdminSettingsEntity> = callbackFlow {
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
            listeners.remove(listener)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * جلب الإعدادات لمرة واحدة
     */
    suspend fun getSettings(): Result<AdminSettingsEntity> = withContext(Dispatchers.IO) {
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
            Result.failure(e)
        }
    }

    /**
     * حفظ وتحديث إعدادات التطبيق بالكامل
     */
    suspend fun saveSettings(settings: AdminSettingsEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            settingsCollection.document(SETTINGS_DOC_ID).set(settings, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving settings", e)
            Result.failure(e)
        }
    }

    /**
     * تحديث حقول معينة في الإعدادات
     */
    suspend fun updatePartialSettings(updates: Map<String, Any>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            settingsCollection.document(SETTINGS_DOC_ID).set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating partial settings", e)
            Result.failure(e)
        }
    }
}
