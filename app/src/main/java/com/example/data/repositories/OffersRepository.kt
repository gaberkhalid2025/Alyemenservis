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
            val cachedStr = cacheManager.getOffersCacheRaw()
            val cachedOffers = if (cachedStr != "[]") {
                SpecialOfferEntity.parseList(cachedStr)
            } else emptyList()

            val lastUpdate = cacheManager.getOffersCacheTime()
            val oneHour = 60 * 60 * 1000
            
            if (cachedOffers.isNotEmpty() && System.currentTimeMillis() - lastUpdate < oneHour) {
                return Result.success(cachedOffers)
            }

            val snapshot = firestore.collection("offers")
                .limit(50)
                .get()
                .await()

            val offers = snapshot.documents.mapNotNull { it.toObject(SpecialOfferEntity::class.java) }
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
            firestore.collection("offers").document(offer.id).set(offer).await()
            val currentOffers = getOffers().getOrDefault(emptyList()).toMutableList()
            currentOffers.add(offer)
            cacheManager.saveOffersCache(SpecialOfferEntity.serializeList(currentOffers))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOffer(offer: SpecialOfferEntity): Result<Unit> {
        return try {
            firestore.collection("offers").document(offer.id).set(offer).await()
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
            firestore.collection("offers").document(offerId).delete().await()
            val currentOffers = getOffers().getOrDefault(emptyList()).filter { it.id != offerId }
            cacheManager.saveOffersCache(SpecialOfferEntity.serializeList(currentOffers))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
