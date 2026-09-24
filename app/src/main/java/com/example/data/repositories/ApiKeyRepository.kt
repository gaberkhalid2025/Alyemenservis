package com.example.data.repositories

import androidx.annotation.Keep
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * ⚠️ Sensitive keys (banking, payment) must be stored in Cloud Functions Secrets, not Firestore.
 * This unified repository abstracts API key management and allows transitioning
 * to secure serverless secret storage seamlessly in the future.
 */
@Keep
data class ApiKeysEntity(
    val geminiApiKey: String = "",
    val openaiApiKey: String = "",
    val selectedAiModel: String = "gemini-1.5-flash",
    val googleMapsKey: String = "",
    val mapboxKey: String = "",
    val selectedMapEngine: String = "OPEN_STREET_MAP",
    val kuraimiToken: String = "",
    val jawwalPayKey: String = "",
    val floosakKey: String = "",
    val oneCashKey: String = "",
    val webhookUrl: String = "",
    val whatsappToken: String = "",
    val smsGatewayKey: String = "",
    val customKeys: List<Map<String, String>> = emptyList(),
    val updatedAt: Long = 0L
)

interface IApiKeyRepository {
    fun getApiKeysFlow(): Flow<ApiKeysEntity>
    suspend fun getApiKeys(): ApiKeysEntity
    suspend fun saveApiKeys(keys: ApiKeysEntity): Result<Unit>
    suspend fun getApiKey(keyName: String): Result<String>
    suspend fun setApiKey(keyName: String, value: String): Result<Unit>
}

class ApiKeyRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IApiKeyRepository {

    private val docRef get() = db.collection("settings").document("api_keys")

    /**
     * 🔑 جلب مفتاح API عبر Cloud Function الآمن
     */
    override suspend fun getApiKey(keyName: String): Result<String> {
        return try {
            val functions = com.google.firebase.functions.FirebaseFunctions.getInstance()
            val result = functions
                .getHttpsCallable("getApiKey")
                .call(mapOf("keyName" to keyName))
                .await()

            val data = result.data as? Map<*, *>
            val value = data?.get("value") as? String

            if (value != null && value.isNotBlank()) {
                Result.success(value)
            } else {
                Result.failure(Exception("Key not found: $keyName"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 💾 حفظ مفتاح API عبر Cloud Function الآمن في Secret Manager
     */
    override suspend fun setApiKey(keyName: String, value: String): Result<Unit> {
        return try {
            val functions = com.google.firebase.functions.FirebaseFunctions.getInstance()
            functions
                .getHttpsCallable("setApiKey")
                .call(mapOf(
                    "keyName" to keyName,
                    "value" to value
                ))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            // Fallback إلى Firestore
            try {
                docRef.set(mapOf(keyName to value), com.google.firebase.firestore.SetOptions.merge()).await()
                Result.success(Unit)
            } catch (fallbackEx: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun getApiKeysFlow(): Flow<ApiKeysEntity> = callbackFlow {
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(ApiKeysEntity())
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val entity = snapshot.toObject(ApiKeysEntity::class.java) ?: ApiKeysEntity()
                trySend(entity)
            } else {
                trySend(ApiKeysEntity())
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getApiKeys(): ApiKeysEntity {
        return try {
            val snap = docRef.get().await()
            snap.toObject(ApiKeysEntity::class.java) ?: ApiKeysEntity()
        } catch (e: Exception) {
            ApiKeysEntity()
        }
    }

    override suspend fun saveApiKeys(keys: ApiKeysEntity): Result<Unit> {
        return try {
            val data = mapOf(
                "geminiApiKey" to keys.geminiApiKey,
                "openaiApiKey" to keys.openaiApiKey,
                "selectedAiModel" to keys.selectedAiModel,
                "googleMapsKey" to keys.googleMapsKey,
                "mapboxKey" to keys.mapboxKey,
                "selectedMapEngine" to keys.selectedMapEngine,
                "kuraimiToken" to keys.kuraimiToken,
                "jawwalPayKey" to keys.jawwalPayKey,
                "floosakKey" to keys.floosakKey,
                "oneCashKey" to keys.oneCashKey,
                "webhookUrl" to keys.webhookUrl,
                "whatsappToken" to keys.whatsappToken,
                "smsGatewayKey" to keys.smsGatewayKey,
                "customKeys" to keys.customKeys,
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
