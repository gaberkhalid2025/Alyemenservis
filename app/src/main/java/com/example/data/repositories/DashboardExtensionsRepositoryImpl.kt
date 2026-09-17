package com.example.data.repositories

import com.example.data.SpecialOfferEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class DashboardExtensionsRepositoryImpl : IDashboardExtensionsRepository {
    private val firestore = FirebaseFirestore.getInstance()

    override fun getSpecialOffers(ownerId: String): Flow<List<SpecialOfferEntity>> = callbackFlow {
        val query = if (ownerId.isNotBlank()) {
            firestore.collection("special_offers").whereEqualTo("providerId", ownerId)
        } else {
            firestore.collection("special_offers")
        }
        val listener = query.addSnapshotListener { snap, _ ->
            if (snap != null) {
                trySend(snap.documents.mapNotNull { it.toObject(SpecialOfferEntity::class.java)?.copy(id = it.id) })
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getCoupons(ownerId: String): Flow<List<SpecialOfferEntity>> = callbackFlow {
        val query = if (ownerId.isNotBlank()) {
            firestore.collection("coupons").whereEqualTo("providerId", ownerId)
        } else {
            firestore.collection("coupons")
        }
        val listener = query.addSnapshotListener { snap, _ ->
            if (snap != null) {
                trySend(snap.documents.mapNotNull { it.toObject(SpecialOfferEntity::class.java)?.copy(id = it.id) })
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getInventory(ownerId: String): Flow<List<InventoryItem>> = callbackFlow {
        val query = if (ownerId.isNotBlank()) {
            firestore.collection("inventory").whereEqualTo("ownerId", ownerId)
        } else {
            firestore.collection("inventory")
        }
        val listener = query.addSnapshotListener { snap, _ ->
            if (snap != null) {
                trySend(snap.documents.mapNotNull { it.toObject(InventoryItem::class.java)?.copy(id = it.id) })
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getLoyaltyPrograms(ownerId: String): Flow<List<LoyaltyProgram>> = callbackFlow {
        val query = if (ownerId.isNotBlank()) {
            firestore.collection("loyalty").whereEqualTo("ownerId", ownerId)
        } else {
            firestore.collection("loyalty")
        }
        val listener = query.addSnapshotListener { snap, _ ->
            if (snap != null) {
                trySend(snap.documents.mapNotNull { it.toObject(LoyaltyProgram::class.java)?.copy(id = it.id) })
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getStaff(ownerId: String): Flow<List<StaffMember>> = callbackFlow {
        val query = if (ownerId.isNotBlank()) {
            firestore.collection("staff").whereEqualTo("ownerId", ownerId)
        } else {
            firestore.collection("staff")
        }
        val listener = query.addSnapshotListener { snap, _ ->
            if (snap != null) {
                trySend(snap.documents.mapNotNull { it.toObject(StaffMember::class.java)?.copy(id = it.id) })
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }
}
