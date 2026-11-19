package com.example.data.repositories

import com.example.data.SpecialOfferEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class DashboardExtensionsRepositoryImpl : IDashboardExtensionsRepository {
    private val firestore = FirebaseFirestore.getInstance()

    override fun getSpecialOffers(ownerId: String): Flow<List<SpecialOfferEntity>> = callbackFlow {
        val cleanId = ownerId.trim()
        val query = if (cleanId.isNotBlank()) {
            firestore.collection("special_offers").whereEqualTo("providerId", cleanId)
        } else {
            firestore.collection("special_offers")
        }
        val listener = query.addSnapshotListener { snap, err ->
            if (err != null) {
                android.util.Log.w("DashboardExtRepo", "getSpecialOffers error: ${err.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snap != null) {
                trySend(snap.documents.mapNotNull { it.toObject(SpecialOfferEntity::class.java)?.copy(id = it.id) })
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    override fun updateSpecialOfferStatus(offerId: String, isEnabled: Boolean) {
        if (offerId.isBlank()) return
        try {
            firestore.collection("special_offers")
                .document(offerId)
                .update("isEnabled", isEnabled)
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardExtRepo", "Failed to update offer status", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("DashboardExtRepo", "Failed to update offer status", e)
        }
    }

    override fun deleteSpecialOffer(offerId: String) {
        if (offerId.isBlank()) return
        try {
            firestore.collection("special_offers")
                .document(offerId)
                .delete()
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardExtRepo", "Failed to delete offer", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("DashboardExtRepo", "Failed to delete offer", e)
        }
    }

    override fun addSpecialOffer(offer: SpecialOfferEntity) {
        try {
            val docId = offer.id.ifBlank { java.util.UUID.randomUUID().toString() }
            firestore.collection("special_offers")
                .document(docId)
                .set(offer.copy(id = docId))
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardExtRepo", "Failed to add offer", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("DashboardExtRepo", "Failed to add offer", e)
        }
    }

    override fun getCoupons(ownerId: String): Flow<List<SpecialOfferEntity>> = callbackFlow {
        val cleanId = ownerId.trim()
        val query = if (cleanId.isNotBlank()) {
            firestore.collection("coupons").whereEqualTo("providerId", cleanId)
        } else {
            firestore.collection("coupons")
        }
        val listener = query.addSnapshotListener { snap, err ->
            if (err != null) {
                android.util.Log.w("DashboardExtRepo", "getCoupons error: ${err.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snap != null) {
                val list = snap.documents.mapNotNull { doc ->
                    val base = doc.toObject(SpecialOfferEntity::class.java)
                    if (base != null) {
                        val code = base.couponCode.ifBlank { doc.getString("code") ?: "" }
                        val discount = if (base.discountPercent > 0) base.discountPercent else (doc.getDouble("discountPercentage")?.toInt() ?: 0)
                        val enabled = doc.getBoolean("isEnabled") ?: doc.getBoolean("isActive") ?: base.isEnabled
                        base.copy(
                            id = doc.id,
                            couponCode = code,
                            discountPercent = discount,
                            isEnabled = enabled
                        )
                    } else null
                }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    override fun updateCouponStatus(couponId: String, isEnabled: Boolean) {
        if (couponId.isBlank()) return
        try {
            firestore.collection("coupons")
                .document(couponId)
                .update(mapOf("isEnabled" to isEnabled, "isActive" to isEnabled))
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardExtRepo", "Failed to update coupon status", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("DashboardExtRepo", "Failed to update coupon status", e)
        }
    }

    override fun deleteCoupon(couponId: String) {
        if (couponId.isBlank()) return
        try {
            firestore.collection("coupons")
                .document(couponId)
                .delete()
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardExtRepo", "Failed to delete coupon", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("DashboardExtRepo", "Failed to delete coupon", e)
        }
    }

    override fun addCoupon(coupon: SpecialOfferEntity) {
        try {
            val docId = coupon.id.ifBlank { java.util.UUID.randomUUID().toString() }
            firestore.collection("coupons")
                .document(docId)
                .set(coupon.copy(id = docId))
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardExtRepo", "Failed to add coupon", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("DashboardExtRepo", "Failed to add coupon", e)
        }
    }

    override fun getInventory(ownerId: String): Flow<List<InventoryItem>> = callbackFlow {
        val cleanId = ownerId.trim()
        if (cleanId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("inventory")
            .whereEqualTo("ownerId", cleanId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.w("DashboardExtRepo", "getInventory error: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snap != null) {
                    trySend(snap.documents.mapNotNull { it.toObject(InventoryItem::class.java)?.copy(id = it.id) })
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }

    override fun updateInventoryQuantity(itemId: String, newQty: Int, inStock: Boolean) {
        if (itemId.isBlank()) return
        try {
            firestore.collection("inventory")
                .document(itemId)
                .update("quantity", newQty, "inStock", inStock)
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardExtRepo", "Failed to update inventory", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("DashboardExtRepo", "Failed to update inventory", e)
        }
    }

    override fun deleteInventoryItem(itemId: String) {
        if (itemId.isBlank()) return
        try {
            firestore.collection("inventory")
                .document(itemId)
                .delete()
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardExtRepo", "Failed to delete inventory", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("DashboardExtRepo", "Failed to delete inventory", e)
        }
    }

    override fun addInventoryItem(item: InventoryItem) {
        try {
            val docId = item.id.ifBlank { java.util.UUID.randomUUID().toString() }
            firestore.collection("inventory")
                .document(docId)
                .set(item.copy(id = docId))
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardExtRepo", "Failed to add inventory", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("DashboardExtRepo", "Failed to add inventory", e)
        }
    }

    override fun getLoyaltyPrograms(ownerId: String): Flow<List<LoyaltyProgram>> = callbackFlow {
        val cleanId = ownerId.trim()
        if (cleanId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("loyalty_programs")
            .whereEqualTo("ownerId", cleanId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.w("DashboardExtRepo", "getLoyaltyPrograms error: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snap != null) {
                    trySend(snap.documents.mapNotNull { it.toObject(LoyaltyProgram::class.java)?.copy(id = it.id) })
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }

    override fun updateLoyaltyProgramStatus(programId: String, isEnabled: Boolean) {
        if (programId.isBlank()) return
        try {
            firestore.collection("loyalty_programs")
                .document(programId)
                .update("isEnabled", isEnabled)
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardExtRepo", "Failed to update loyalty program", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("DashboardExtRepo", "Failed to update loyalty program", e)
        }
    }

    override fun deleteLoyaltyProgram(programId: String) {
        if (programId.isBlank()) return
        try {
            firestore.collection("loyalty_programs")
                .document(programId)
                .delete()
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardExtRepo", "Failed to delete loyalty program", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("DashboardExtRepo", "Failed to delete loyalty program", e)
        }
    }

    override fun addLoyaltyProgram(program: LoyaltyProgram) {
        try {
            val docId = program.id.ifBlank { java.util.UUID.randomUUID().toString() }
            firestore.collection("loyalty_programs")
                .document(docId)
                .set(program.copy(id = docId))
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardExtRepo", "Failed to add loyalty program", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("DashboardExtRepo", "Failed to add loyalty program", e)
        }
    }

    override fun getStaff(ownerId: String): Flow<List<StaffMember>> = callbackFlow {
        val cleanId = ownerId.trim()
        if (cleanId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("staff")
            .whereEqualTo("ownerId", cleanId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.w("DashboardExtRepo", "getStaff error: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snap != null) {
                    trySend(snap.documents.mapNotNull { it.toObject(StaffMember::class.java)?.copy(id = it.id) })
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }

    override fun deleteStaff(staffId: String) {
        if (staffId.isBlank()) return
        try {
            firestore.collection("staff")
                .document(staffId)
                .delete()
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardExtRepo", "Failed to delete staff", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("DashboardExtRepo", "Failed to delete staff", e)
        }
    }

    override fun addStaff(staffMember: StaffMember) {
        try {
            val docId = staffMember.id.ifBlank { java.util.UUID.randomUUID().toString() }
            firestore.collection("staff")
                .document(docId)
                .set(staffMember.copy(id = docId))
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardExtRepo", "Failed to add staff", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("DashboardExtRepo", "Failed to add staff", e)
        }
    }
}

