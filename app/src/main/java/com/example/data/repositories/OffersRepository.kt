package com.example.data.repositories

import com.example.data.LocalAppCacheManager
import com.example.data.SpecialOfferEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OffersRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cacheManager: LocalAppCacheManager
) {
    suspend fun getOffers(): Result<List<SpecialOfferEntity>> {
        return try {
            val snapshot = firestore.collection("special_offers")
                .limit(50)
                .get()
                .await()

            var offers = snapshot.documents.mapNotNull { doc ->
                doc.toObject(SpecialOfferEntity::class.java)?.copy(id = doc.id)
            }

            if (offers.isEmpty()) {
                val legacySnapshot = firestore.collection("offers")
                    .limit(50)
                    .get()
                    .await()
                offers = legacySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(SpecialOfferEntity::class.java)?.copy(id = doc.id)
                }
            }

            if (offers.isNotEmpty()) {
                cacheManager.saveOffersCache(SpecialOfferEntity.serializeList(offers))
            }
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

    suspend fun addOffer(offer: SpecialOfferEntity): Result<Unit> {
        return try {
            firestore.collection("special_offers").document(offer.id).set(offer).await()
            try {
                firestore.collection("offers").document(offer.id).set(offer).await()
            } catch (_: Exception) {}
            val currentOffers = getOffers().getOrDefault(emptyList()).toMutableList()
            if (currentOffers.none { it.id == offer.id }) {
                currentOffers.add(0, offer)
            }
            cacheManager.saveOffersCache(SpecialOfferEntity.serializeList(currentOffers))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOffer(offer: SpecialOfferEntity): Result<Unit> {
        return try {
            firestore.collection("special_offers").document(offer.id).set(offer).await()
            try {
                firestore.collection("offers").document(offer.id).set(offer).await()
            } catch (_: Exception) {}
            val currentOffers = getOffers().getOrDefault(emptyList()).map {
                if (it.id == offer.id) offer else it
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
            val currentOffers = getOffers().getOrDefault(emptyList()).filter { it.id != offerId }
            cacheManager.saveOffersCache(SpecialOfferEntity.serializeList(currentOffers))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
