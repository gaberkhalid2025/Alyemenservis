package com.example.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.data.BannerEntity
import com.example.data.OrderEntity
import com.example.data.ProductEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.RatingEntity
import com.example.data.StoreEntity
import com.example.data.models.Offer
import com.example.utils.FirebaseStorageUploader
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.util.UUID

internal fun saveProductOp(vm: AdminViewModel, product: ProductEntity) {
    val targetId = if (product.id.isEmpty()) vm.db.collection("products").document().id else product.id
    val currentUserId = vm.firebaseAuth.currentUser?.uid ?: ""
    val resolvedStoreId = product.storeId.ifEmpty { currentUserId }
    val resolvedOwnerId = product.ownerId.ifEmpty { currentUserId.ifEmpty { resolvedStoreId } }
    val localProduct = product.copy(
        id = targetId,
        storeId = resolvedStoreId,
        ownerId = resolvedOwnerId
    )

    val currentProds = vm._products.value.filter { it.id != targetId }.toMutableList()
    currentProds.add(localProduct)
    vm._products.value = currentProds

    vm.viewModelScope.launch {
        val ctx = vm.appContext
        val finalImg = if (ctx != null && product.imageUrl.isNotEmpty() && !product.imageUrl.startsWith("http")) {
            vm.resolveUploadImage(
                ctx,
                product.imageUrl,
                FirebaseStorageUploader.getStoreProductPath(resolvedStoreId.ifEmpty { "general" }, targetId)
            )
        } else product.imageUrl

        val finalProduct = product.copy(
            id = targetId,
            storeId = resolvedStoreId,
            ownerId = resolvedOwnerId,
            imageUrl = finalImg
        )
        vm.db.collection("products").document(targetId).set(finalProduct)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("✅ تم حفظ المنتج بنجاح!")
            }
    }
}

internal fun deleteProductOp(vm: AdminViewModel, productId: String) {
    vm._products.value = vm._products.value.filter { it.id != productId }
    vm.db.collection("products").document(productId).get().addOnSuccessListener { snapshot ->
        val product = snapshot.toObject(ProductEntity::class.java)
        if (product != null) {
            vm.db.collection("products").document(productId).set(product.copy(isDeleted = true))
        } else {
            vm.db.collection("products").document(productId).update("isDeleted", true)
        }
    }
}

internal fun updateProductPriceOp(vm: AdminViewModel, productId: String, newPrice: Double) {
    val existing = vm._products.value.find { it.id == productId }
    if (existing != null) {
        val updated = existing.copy(
            price = newPrice,
            oldPrice = if (existing.price != newPrice) existing.price else existing.oldPrice
        )
        vm._products.value = vm._products.value.map { if (it.id == productId) updated else it }
        vm.db.collection("products").document(productId)
            .update("price", newPrice, "oldPrice", updated.oldPrice)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("⚡ تم تحديث السعر فورياً لجميع العملاء!")
            }
    } else {
        vm.db.collection("products").document(productId).update("price", newPrice)
    }
}

internal fun saveOfferOp(vm: AdminViewModel, offer: Offer) {
    val targetId = if (offer.id.isEmpty()) vm.db.collection("offers").document().id else offer.id
    val finalOffer = offer.copy(id = targetId, updatedAt = System.currentTimeMillis())

    val currentList = vm.mainViewModel.instantRequestViewModel._offers.value
        .filter { it.id != targetId }
        .toMutableList()
    currentList.add(finalOffer)
    vm.mainViewModel.instantRequestViewModel._offers.value = currentList

    vm.db.collection("offers").document(targetId).set(finalOffer)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("🎁 تم نشر العرض وتحديث الأسعار فورياً!")
        }
        .addOnFailureListener {
            vm.mainViewModel.triggerNotification("⚠️ فشل حفظ العرض: ${it.localizedMessage}")
        }
}

internal fun deleteOfferOp(vm: AdminViewModel, offerId: String) {
    vm.mainViewModel.instantRequestViewModel._offers.value =
        vm.mainViewModel.instantRequestViewModel._offers.value.filter { it.id != offerId }
    vm.db.collection("offers").document(offerId).delete()
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("🗑️ تم حذف العرض بنجاح!")
        }
}

internal fun toggleOfferStatusOp(vm: AdminViewModel, offerId: String, isActive: Boolean) {
    vm.mainViewModel.instantRequestViewModel._offers.value =
        vm.mainViewModel.instantRequestViewModel._offers.value.map {
            if (it.id == offerId) it.copy(isActive = isActive) else it
        }
    vm.db.collection("offers").document(offerId).update(
        "isActive", isActive,
        "updatedAt", System.currentTimeMillis()
    )
}

internal fun listenToOffersForEntityOp(
    vm: AdminViewModel,
    entityId: String,
    onResult: (List<Offer>) -> Unit
): ListenerRegistration = vm.db.collection("offers")
    .whereEqualTo("entityId", entityId)
    .addSnapshotListener { snapshot, error ->
        if (error == null && snapshot != null) {
            val fetched = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Offer::class.java)?.copy(id = doc.id)
            }
            onResult(fetched)
        }
    }

internal fun listenToProductsForStoreOp(
    vm: AdminViewModel,
    storeId: String,
    onResult: (List<ProductEntity>) -> Unit
): ListenerRegistration = vm.db.collection("products")
    .whereEqualTo("storeId", storeId)
    .addSnapshotListener { snapshot, error ->
        if (error == null && snapshot != null) {
            val fetched = snapshot.documents.mapNotNull { doc ->
                val p = doc.toObject(ProductEntity::class.java)
                if (p != null && !p.isDeleted) p.copy(id = doc.id) else null
            }
            onResult(fetched)
        }
    }

internal fun addNewBannerOp(
    vm: AdminViewModel,
    title: String,
    url: String,
    redirect: String,
    type: String,
    size: String,
    duration: Int,
    displayTime: String
) {
    val banner = BannerEntity(
        id = UUID.randomUUID().toString(),
        title = title,
        url = url,
        redirectCategory = redirect,
        type = type,
        size = size,
        duration = duration,
        displayTime = displayTime,
        order = vm.mainViewModel.homeViewModel._banners.value.size + 1
    )
    vm.db.collection("banners").document(banner.id).set(banner)
    vm.mainViewModel.triggerNotification("🖼️ تم إضافة إعلان جديد: $title")
}

internal fun deleteBannerOp(vm: AdminViewModel, bannerId: String) {
    vm.viewModelScope.launch {
        vm.runSafeFirestoreCall(
            operation = {
                vm.crud.deleteEntity("banners", bannerId, false)
                vm.mainViewModel.homeViewModel.deleteBanner(bannerId)
            },
            onSuccess = {
                vm.mainViewModel.triggerNotification("🗑️ تم حذف البنر")
            },
            onError = {
                vm.mainViewModel.triggerNotification("⚠️ تم حذف البنر محلياً، سيتم المزامنة تلقائياً")
            }
        )
    }
}

internal fun reorderBannersOp(vm: AdminViewModel, newOrderedList: List<BannerEntity>) {
    vm.viewModelScope.launch {
        try {
            newOrderedList.forEachIndexed { index, banner ->
                vm.crud.updateFields("banners", banner.id, mapOf("order" to index + 1))
            }
            vm.mainViewModel.triggerNotification("✅ تم تحديث ترتيب البنرات")
        } catch (_: Exception) {
            vm.mainViewModel.triggerNotification("⚠️ تم تحديث الترتيب محلياً، سيتم المزامنة تلقائياً")
        }
    }
}

internal fun placeOrderOp(vm: AdminViewModel, order: OrderEntity) {
    val targetId = vm.db.collection("orders").document().id
    val finalOrder = order.copy(id = targetId)
    vm.db.collection("orders").document(targetId).set(finalOrder).addOnSuccessListener {
        vm.mainViewModel.triggerNotification("🛍️ تم تسجيل طلبك بنجاح! رقم الطلب: ${targetId.take(6)}")
    }
}

internal fun updateOrderStatusOp(vm: AdminViewModel, orderId: String, status: String) {
    vm.db.collection("orders").document(orderId).get().addOnSuccessListener { snap ->
        val order = snap.toObject(OrderEntity::class.java)
        if (order != null) {
            vm.db.collection("orders").document(orderId).set(order.copy(status = status))
                .addOnSuccessListener {
                    vm.mainViewModel.triggerNotification("📦 تم تحديث حالة الطلب إلى $status")
                }
        }
    }
}

internal fun deleteOrderOp(vm: AdminViewModel, orderId: String) {
    vm.db.collection("orders").document(orderId).delete().addOnSuccessListener {
        vm.mainViewModel.triggerNotification("🗑️ تم حذف الطلب بنجاح.")
    }
}

internal fun deleteAllOrdersOp(vm: AdminViewModel, customerPhone: String) {
    vm.db.collection("orders")
        .whereEqualTo("customerPhone", customerPhone)
        .get()
        .addOnSuccessListener { querySnapshot ->
            val docs = querySnapshot.documents
            if (docs.isEmpty()) {
                vm.mainViewModel.triggerNotification("🗑️ لا توجد طلبات لحذفها.")
                return@addOnSuccessListener
            }
            val chunks = docs.chunked(450)
            chunks.forEachIndexed { idx, chunk ->
                val batch = vm.db.batch()
                for (doc in chunk) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnSuccessListener {
                    if (idx == chunks.lastIndex) {
                        vm.mainViewModel.triggerNotification("🗑️ تم حذف جميع الطلبات بنجاح.")
                    }
                }
            }
        }
}

internal fun addRatingOp(vm: AdminViewModel, rating: RatingEntity) {
    val targetId = if (rating.id.isBlank()) vm.db.collection("ratings").document().id else rating.id
    val finalRating = rating.copy(id = targetId)
    val currentList = vm.appState._ratings.value.toMutableList()
    currentList.removeAll { it.id == targetId }
    currentList.add(0, finalRating)
    vm.appState._ratings.value = currentList

    vm.db.collection("ratings").document(targetId).set(finalRating).addOnSuccessListener {
        vm.mainViewModel.triggerNotification("⭐ شكراً لتقييمك! تم إرسال تقييمك بنجاح.")
        recalculateTargetRatingOp(vm, rating.targetId, rating.targetType)
    }
}

internal fun addRatingReplyOp(vm: AdminViewModel, ratingId: String, replyText: String) {
    val updates = mapOf(
        "reply" to replyText,
        "replyTimestamp" to System.currentTimeMillis()
    )
    vm.db.collection("ratings").document(ratingId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("✅ تم إضافة الرد على التعليق بنجاح!")
        }
        .addOnFailureListener {
            vm.mainViewModel.triggerNotification("❌ فشل إضافة الرد: ${it.message}")
        }
}

internal fun deleteRatingOp(vm: AdminViewModel, ratingId: String) {
    vm.appState._ratings.value = vm.appState._ratings.value.filter { it.id != ratingId }
    vm.db.collection("ratings").document(ratingId).get().addOnSuccessListener { snap ->
        val rating = snap.toObject(RatingEntity::class.java)
        if (rating != null) {
            vm.db.collection("ratings").document(ratingId).delete().addOnSuccessListener {
                recalculateTargetRatingOp(vm, rating.targetId, rating.targetType)
            }
        }
    }
}

internal fun approveRatingOp(vm: AdminViewModel, ratingId: String, isApproved: Boolean) {
    vm.db.collection("ratings").document(ratingId).get().addOnSuccessListener { snap ->
        val rating = snap.toObject(RatingEntity::class.java)
        if (rating != null) {
            vm.db.collection("ratings").document(ratingId).set(rating.copy(isApproved = isApproved))
        }
    }
}

internal fun submitRatingEntityOp(vm: AdminViewModel, ratingEntity: RatingEntity, onComplete: () -> Unit) {
    val currentList = vm.appState._ratings.value.toMutableList()
    currentList.removeAll { it.id == ratingEntity.id }
    currentList.add(0, ratingEntity)
    vm.appState._ratings.value = currentList

    vm.db.collection("ratings").document(ratingEntity.id).set(ratingEntity)
        .addOnSuccessListener {
            recalculateTargetRatingOp(vm, ratingEntity.targetId, ratingEntity.targetType)
            onComplete()
        }
        .addOnFailureListener {
            recalculateTargetRatingOp(vm, ratingEntity.targetId, ratingEntity.targetType)
            onComplete()
        }
}

internal fun recalculateTargetRatingOp(vm: AdminViewModel, targetId: String, targetType: String) {
    vm.db.collection("ratings").whereEqualTo("targetId", targetId).get()
        .addOnSuccessListener { snapshot ->
            val ratingsList = snapshot.documents.mapNotNull { it.toObject(RatingEntity::class.java) }
            val count = ratingsList.size
            val avg = if (count > 0) ratingsList.map { it.rating }.sum() / count else 5.0f

            if (targetType.equals("PROVIDER", ignoreCase = true) ||
                targetType.equals("TECHNICIAN", ignoreCase = true)
            ) {
                vm.db.collection("providers").document(targetId).get().addOnSuccessListener { provSnap ->
                    val prov = provSnap.toObject(ProviderEntity::class.java)
                    if (prov != null) {
                        vm.db.collection("providers").document(targetId)
                            .set(prov.copy(rating = avg, numReviews = count))
                    }
                }
                val provList = vm.mainViewModel.homeViewModel.providers.value.toMutableList()
                val pIdx = provList.indexOfFirst { it.id == targetId }
                if (pIdx >= 0) {
                    provList[pIdx] = provList[pIdx].copy(rating = avg, numReviews = count)
                    vm.mainViewModel.homeViewModel._providers.value = provList
                }
            } else if (
                targetType.equals("STORE", ignoreCase = true) ||
                targetType.equals("RESTAURANT", ignoreCase = true) ||
                targetType.equals("MEDICAL", ignoreCase = true)
            ) {
                vm.db.collection("stores").document(targetId).get().addOnSuccessListener { storeSnap ->
                    val store = storeSnap.toObject(StoreEntity::class.java)
                    if (store != null) {
                        vm.db.collection("stores").document(targetId)
                            .set(store.copy(rating = avg, numReviews = count))
                    }
                }
                val sList = vm.stores.value.toMutableList()
                val sIdx = sList.indexOfFirst { it.id == targetId }
                if (sIdx >= 0) {
                    sList[sIdx] = sList[sIdx].copy(rating = avg, numReviews = count)
                    vm._stores.value = sList
                }
            } else if (targetType.equals("PROPERTY", ignoreCase = true)) {
                vm.db.collection("properties").document(targetId).get().addOnSuccessListener { propSnap ->
                    val prop = propSnap.toObject(PropertyEntity::class.java)
                    if (prop != null) {
                        vm.db.collection("properties").document(targetId)
                            .set(prop.copy(rating = avg, numReviews = count))
                    }
                }
                val pList = vm.properties.value.toMutableList()
                val propIdx = pList.indexOfFirst { it.id == targetId }
                if (propIdx >= 0) {
                    pList[propIdx] = pList[propIdx].copy(rating = avg, numReviews = count)
                    vm._properties.value = pList
                }
            }
        }
}
