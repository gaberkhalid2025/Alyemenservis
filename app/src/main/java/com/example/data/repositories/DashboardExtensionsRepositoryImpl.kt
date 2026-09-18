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

    // ✨ م2-ج2: دوال العروض الخاصة عبر الـ Repository
    override fun updateSpecialOfferStatus(offerId: String, isEnabled: Boolean) {
        firestore.collection("special_offers")
            .document(offerId)
            .update("isEnabled", isEnabled)
    }

    override fun deleteSpecialOffer(offerId: String) {
        firestore.collection("special_offers")
            .document(offerId)
            .delete()
    }

    override fun addSpecialOffer(offer: SpecialOfferEntity) {
        firestore.collection("special_offers")
            .document(offer.id)
            .set(offer)
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

    // ✨ م2-ج2: دوال الكوبونات عبر الـ Repository
    override fun updateCouponStatus(couponId: String, isEnabled: Boolean) {
        firestore.collection("coupons")
            .document(couponId)
            .update("isEnabled", isEnabled)
    }

    override fun deleteCoupon(couponId: String) {
        firestore.collection("coupons")
            .document(couponId)
            .delete()
    }

    override fun addCoupon(coupon: SpecialOfferEntity) {
        firestore.collection("coupons")
            .document(coupon.id)
            .set(coupon)
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

    // ✨ م2-ج2: نقل منطق Firestore لحفظ وإعادة تعديل المخزون
    override fun updateInventoryQuantity(itemId: String, newQty: Int, inStock: Boolean) {
        firestore.collection("inventory")
            .document(itemId)
            .update("quantity", newQty, "inStock", inStock)
    }

    override fun deleteInventoryItem(itemId: String) {
        firestore.collection("inventory")
            .document(itemId)
            .delete()
    }

    override fun addInventoryItem(item: InventoryItem) {
        firestore.collection("inventory")
            .document(item.id)
            .set(item)
    }

    override fun getLoyaltyPrograms(ownerId: String): Flow<List<LoyaltyProgram>> = callbackFlow {
        val query = if (ownerId.isNotBlank()) {
            firestore.collection("loyalty_programs").whereEqualTo("ownerId", ownerId)
        } else {
            firestore.collection("loyalty_programs")
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

    // ✨ م2-ج2: دوال برامج الولاء عبر الـ Repository
    override fun updateLoyaltyProgramStatus(programId: String, isEnabled: Boolean) {
        firestore.collection("loyalty_programs")
            .document(programId)
            .update("isEnabled", isEnabled)
    }

    override fun deleteLoyaltyProgram(programId: String) {
        firestore.collection("loyalty_programs")
            .document(programId)
            .delete()
    }

    override fun addLoyaltyProgram(program: LoyaltyProgram) {
        firestore.collection("loyalty_programs")
            .document(program.id)
            .set(program)
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

    // ✨ م2-ج2: دوال الموظفين عبر الـ Repository
    override fun deleteStaff(staffId: String) {
        firestore.collection("staff")
            .document(staffId)
            .delete()
    }

    override fun addStaff(staffMember: StaffMember) {
        firestore.collection("staff")
            .document(staffMember.id)
            .set(staffMember)
    }
}
