package com.example.data.repositories

import com.example.data.LocalAppCacheManager
import com.example.data.SpecialOfferEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OffersRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cacheManager: LocalAppCacheManager
) {
    companion object {
        private const val OFFERS_CACHE_TTL_MS = 5 * 60 * 1000L
        private const val OFFERS_QUERY_LIMIT = 30L
    }

    @Volatile
    private var lastOffersFetchMs: Long = 0L

    suspend fun getOffers(forceRefresh: Boolean = false): Result<List<SpecialOfferEntity>> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && (now - lastOffersFetchMs) < OFFERS_CACHE_TTL_MS) {
            val cached = readLocalCache()
            if (cached.isNotEmpty()) {
                return Result.success(cached)
            }
        }
        return try {
            val snapshot = firestore.collection("special_offers")
                .limit(OFFERS_QUERY_LIMIT)
                .get()
                .await()

            var offers = snapshot.documents.mapNotNull { doc ->
                doc.toObject(SpecialOfferEntity::class.java)?.copy(id = doc.id)
            }

            if (offers.isEmpty()) {
                val legacySnapshot = firestore.collection("offers")
                    .limit(OFFERS_QUERY_LIMIT)
                    .get()
                    .await()
                offers = legacySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(SpecialOfferEntity::class.java)?.copy(id = doc.id)
                }
            }

            lastOffersFetchMs = System.currentTimeMillis()
            cacheManager.saveOffersCache(SpecialOfferEntity.serializeList(offers))
            Result.success(offers)
        } catch (e: Exception) {
            val fallbackStr = cacheManager.getOffersCacheRaw()
            val fallback = if (fallbackStr != "[]") SpecialOfferEntity.parseList(fallbackStr) else emptyList()
            if (fallback.isNotEmpty()) {
                Result.success(fallback)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getOffersForEntity(entityId: String): Result<List<SpecialOfferEntity>> {
        if (entityId.isBlank()) return getOffers()
        return try {
            val snapshot = firestore.collection("special_offers")
                .whereEqualTo("providerId", entityId)
                .limit(OFFERS_QUERY_LIMIT)
                .get()
                .await()
            val offers = snapshot.documents.mapNotNull { doc ->
                doc.toObject(SpecialOfferEntity::class.java)?.copy(id = doc.id)
            }
            Result.success(offers)
        } catch (e: Exception) {
            val cached = readLocalCache().filter { it.providerId == entityId }
            if (cached.isNotEmpty()) Result.success(cached) else Result.failure(e)
        }
    }

    private fun readLocalCache(): MutableList<SpecialOfferEntity> {
        val raw = cacheManager.getOffersCacheRaw()
        return if (raw.isNotBlank() && raw != "[]") {
            SpecialOfferEntity.parseList(raw).toMutableList()
        } else {
            mutableListOf()
        }
    }

    suspend fun addOffer(offer: SpecialOfferEntity): Result<Unit> {
        return try {
            val docId = offer.id.ifBlank { java.util.UUID.randomUUID().toString() }
            val finalOffer = offer.copy(id = docId)
            firestore.collection("special_offers").document(docId).set(finalOffer).await()
            try {
                firestore.collection("offers").document(docId).set(finalOffer).await()
            } catch (_: Exception) {}
            lastOffersFetchMs = 0L
            val currentOffers = readLocalCache()
            if (currentOffers.none { it.id == docId }) {
                currentOffers.add(0, finalOffer)
            }
            cacheManager.saveOffersCache(SpecialOfferEntity.serializeList(currentOffers))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOffer(offer: SpecialOfferEntity): Result<Unit> {
        return try {
            val docId = offer.id.ifBlank { java.util.UUID.randomUUID().toString() }
            val finalOffer = offer.copy(id = docId)
            firestore.collection("special_offers").document(docId).set(finalOffer).await()
            try {
                firestore.collection("offers").document(docId).set(finalOffer).await()
            } catch (_: Exception) {}
            lastOffersFetchMs = 0L
            val currentOffers = readLocalCache().map {
                if (it.id == docId) finalOffer else it
            }
            cacheManager.saveOffersCache(SpecialOfferEntity.serializeList(currentOffers))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOffer(offerId: String): Result<Unit> {
        return try {
            firestore.collection("special_offers").document(offerId).delete().await()
            try {
                firestore.collection("offers").document(offerId).delete().await()
            } catch (_: Exception) {}
            lastOffersFetchMs = 0L
            val currentOffers = readLocalCache().filter { it.id != offerId }
            cacheManager.saveOffersCache(SpecialOfferEntity.serializeList(currentOffers))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
