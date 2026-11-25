package com.example.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.data.BannerEntity
import com.example.data.CouponEntity
import com.example.data.InternalWalletEntity
import com.example.data.OrderEntity
import com.example.data.PaymentEntity
import com.example.data.PaymentWalletEntity
import com.example.data.ProductEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.RatingEntity
import com.example.data.ReportEntity
import com.example.data.StoreEntity
import com.example.data.WalletTransactionEntity
import com.example.data.models.Offer
import com.example.utils.FirebaseStorageUploader
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 💳 AdminCommerceAndEngagementDelegate
 * مفوّض البلاغات، الكوبونات، المحافظ والمدفوعات، المنتجات، العروض،
 * البنرات الإعلانية، الطلبات، والتقييمات.
 */
internal class AdminCommerceAndEngagementDelegate(
    private val vm: AdminViewModel
) {
    // ==========================================
    // 📢 البلاغات (Reports)
    // ==========================================

    fun submitReport(report: ReportEntity, onComplete: () -> Unit = {}) {
        vm.db.collection("reports").document(report.id).set(report)
            .addOnSuccessListener {
                onComplete()
            }
    }

    fun deleteReport(reportId: String) {
        vm.db.collection("reports").document(reportId).delete()
        vm.mainViewModel.triggerNotification("🗑️ تم حذف البلاغ من النظام")
    }

    fun sendReport(providerId: String, providerName: String, reporterName: String, content: String) {
        val newReport = ReportEntity(
            id = UUID.randomUUID().toString(),
            providerId = providerId,
            providerName = providerName,
            reporterName = reporterName,
            content = content
        )
        vm.db.collection("reports").document(newReport.id).set(newReport)
        vm.mainViewModel.triggerNotification("📢 تم إرسال بلاغك ضد $providerName")
    }

    // ==========================================
    // 🎫 الكوبونات والخصومات (Coupons)
    // ==========================================

    fun addCoupon(
        code: String,
        pointsValue: Int,
        expiryMs: Long,
        discountPercentage: Int = 0,
        maxUsageCount: Int = 100
    ) {
        val couponId = UUID.randomUUID().toString()
        val coupon = CouponEntity(
            id = couponId,
            code = code,
            pointsValue = pointsValue,
            expiryTimestamp = System.currentTimeMillis() + expiryMs,
            status = "ACTIVE",
            discountPercentage = discountPercentage,
            maxUsageCount = maxUsageCount,
            usedCount = 0
        )
        vm.db.collection("coupons").document(couponId).set(coupon)
        vm.mainViewModel.triggerNotification("🎫 تم إضافة كوبون جديد بنجاح: $code")
    }

    fun saveCoupon(coupon: CouponEntity) {
        vm.viewModelScope.launch {
            val couponId = if (coupon.id.isBlank()) UUID.randomUUID().toString() else coupon.id
            val finalCoupon = coupon.copy(id = couponId)
            vm.runSafeFirestoreCall(
                operation = {
                    vm.crud.saveEntity("coupons", couponId, finalCoupon)
                },
                onSuccess = {
                    vm.mainViewModel.triggerNotification("🎫 تم حفظ الكوبون بنجاح: ${finalCoupon.code}")
                },
                onError = {
                    vm.mainViewModel.triggerNotification("⚠️ تم حفظ الكوبون محلياً، سيتم المزامنة تلقائياً")
                }
            )
        }
    }

    fun deleteCoupon(couponId: String) {
        vm.viewModelScope.launch {
            vm.runSafeFirestoreCall(
                operation = {
                    vm.crud.deleteEntity("coupons", couponId, false)
                },
                onSuccess = {
                    vm.mainViewModel.triggerNotification("🗑️ تم حذف الكوبون")
                },
                onError = {
                    vm.mainViewModel.triggerNotification("⚠️ تم حذف الكوبون محلياً، سيتم المزامنة تلقائياً")
                }
            )
        }
    }

    // ==========================================
    // 💰 المحافظ والمدفوعات (Wallets & Payments)
    // ==========================================

    fun saveInternalWallet(wallet: InternalWalletEntity) {
        val targetId = if (wallet.id.isEmpty()) vm.db.collection("internal_wallets").document().id else wallet.id
        val finalW = wallet.copy(id = targetId, updatedAt = System.currentTimeMillis())
        vm.db.collection("internal_wallets").document(targetId).set(finalW)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("✅ تم حفظ بيانات المحفظة الرقمية الداخلية (${finalW.ownerName})")
            }
    }

    fun performWalletTransaction(
        walletId: String,
        ownerName: String,
        ownerPhone: String,
        ownerType: String,
        type: String,
        amount: Double,
        note: String
    ) {
        val cleanWalletId = walletId.trim()
        if (cleanWalletId.isBlank() || !amount.isFinite() || amount <= 0.0) {
            vm.mainViewModel.triggerNotification("❌ بيانات المعاملة أو المبلغ غير صالحة")
            return
        }

        val normalizedType = type.trim().uppercase()
        val walletRef = vm.db.collection("internal_wallets").document(cleanWalletId)
        val txId = vm.db.collection("wallet_transactions").document().id
        val txRef = vm.db.collection("wallet_transactions").document(txId)
        val now = System.currentTimeMillis()

        vm.db.runTransaction { transaction ->
            val snap = transaction.get(walletRef)
            val currentW = if (snap.exists()) {
                snap.toObject(InternalWalletEntity::class.java)?.copy(id = cleanWalletId)
                    ?: InternalWalletEntity(
                        id = cleanWalletId,
                        ownerName = ownerName,
                        ownerPhone = ownerPhone,
                        ownerType = ownerType
                    )
            } else {
                InternalWalletEntity(
                    id = cleanWalletId,
                    ownerName = ownerName,
                    ownerPhone = ownerPhone,
                    ownerType = ownerType
                )
            }

            val newBalance = when (normalizedType) {
                "DEPOSIT" -> currentW.balance + amount
                "WITHDRAWAL", "TRANSFER" -> {
                    if (currentW.balance < amount) {
                        throw IllegalStateException("الرصيد غير كافٍ. المتاح: ${currentW.balance} ريال")
                    }
                    currentW.balance - amount
                }
                else -> throw IllegalArgumentException("نوع معاملة غير مدعوم: $type")
            }

            val updatedWallet = currentW.copy(balance = newBalance, updatedAt = now)
            transaction.set(walletRef, updatedWallet)

            val tx = WalletTransactionEntity(
                id = txId,
                walletId = cleanWalletId,
                type = normalizedType,
                amount = amount,
                balanceAfter = newBalance,
                note = note,
                performByAdmin = true,
                timestamp = now
            )
            transaction.set(txRef, tx)
            updatedWallet
        }.addOnSuccessListener { updatedWallet ->
            vm.mainViewModel.triggerNotification(
                "💸 تم تنفيذ عملية ($normalizedType) بمبلغ $amount ريال للمحفظة ${updatedWallet.ownerName}. الرصيد الجديد: ${updatedWallet.balance} ريال"
            )
        }.addOnFailureListener { e ->
            vm.mainViewModel.triggerNotification("❌ فشل تنفيذ المعاملة: ${e.localizedMessage ?: "خطأ غير معروف"}")
        }
    }

    fun addPaymentWallet(wallet: PaymentWalletEntity) {
        val docRef = vm.db.collection("payment_wallets").document()
        val walletWithId = wallet.copy(
            id = docRef.id,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        if (walletWithId.isDefault) {
            vm.db.collection("payment_wallets").whereEqualTo("isDefault", true).get()
                .addOnSuccessListener { qs ->
                    for (doc in qs.documents) {
                        doc.reference.update("isDefault", false)
                    }
                }
        }

        docRef.set(walletWithId).addOnSuccessListener {
            vm.mainViewModel.triggerNotification("✅ تم إضافة المحفظة بنجاح!")
        }.addOnFailureListener {
            vm.mainViewModel.triggerNotification("❌ فشل إضافة المحفظة: ${it.message}")
        }
    }

    fun updatePaymentWallet(wallet: PaymentWalletEntity) {
        if (wallet.id.isEmpty()) return

        if (wallet.isDefault) {
            vm.db.collection("payment_wallets").whereEqualTo("isDefault", true).get()
                .addOnSuccessListener { qs ->
                    for (doc in qs.documents) {
                        if (doc.id != wallet.id) {
                            doc.reference.update("isDefault", false)
                        }
                    }
                }
        }

        val updated = wallet.copy(updatedAt = System.currentTimeMillis())
        vm.db.collection("payment_wallets").document(wallet.id).set(updated).addOnSuccessListener {
            vm.mainViewModel.triggerNotification("✅ تم تحديث المحفظة بنجاح!")
        }.addOnFailureListener {
            vm.mainViewModel.triggerNotification("❌ فشل تحديث المحفظة: ${it.message}")
        }
    }

    fun deletePaymentWallet(walletId: String) {
        vm.db.collection("payment_wallets").document(walletId).delete().addOnSuccessListener {
            vm.mainViewModel.triggerNotification("🗑️ تم حذف المحفظة بنجاح!")
        }.addOnFailureListener {
            vm.mainViewModel.triggerNotification("❌ فشل حذف المحفظة: ${it.message}")
        }
    }

    fun togglePaymentWalletVisibility(walletId: String, currentVisible: Boolean) {
        val newStatus = !currentVisible
        vm.db.collection("payment_wallets").document(walletId).update("isVisibleToUsers", newStatus)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification(
                    if (newStatus) "👁️ تم إظهار المحفظة للمستخدمين" else "🙈 تم إخفاء المحفظة عن المستخدمين"
                )
            }.addOnFailureListener {
                vm.mainViewModel.triggerNotification("❌ فشل تغيير حالة إظهار المحفظة")
            }
    }

    fun createPayment(
        userId: String,
        providerId: String,
        amount: Double,
        method: String,
        bookingId: String = "",
        isLinkedToBooking: Boolean = false,
        bookingServiceType: String = "",
        walletProvider: String = "",
        walletNumber: String = "",
        walletAccountName: String = "",
        transferId: String = "",
        transferPhoto: String = "",
        status: String = "PENDING"
    ) {
        val docRef = vm.db.collection("payments").document()

        val settingsVal = vm.mainViewModel.settingsViewModel._settings.value
        val advanceAmount = if (settingsVal.requireAdvancePayment) {
            var calculated = amount * settingsVal.advancePaymentPercent
            if (calculated < settingsVal.minAdvanceAmount) calculated = settingsVal.minAdvanceAmount
            if (calculated > settingsVal.maxAdvanceAmount) calculated = settingsVal.maxAdvanceAmount
            calculated
        } else {
            0.0
        }

        val commission = if (settingsVal.isCommissionEnabled) {
            amount * settingsVal.paymentCommissionRate
        } else {
            0.0
        }

        val remainingAmount = amount - advanceAmount
        val providerShare = amount - commission

        val payment = PaymentEntity(
            id = docRef.id,
            userId = userId,
            providerId = providerId,
            bookingId = bookingId,
            type = "service",
            method = method,
            status = status,
            amount = amount,
            advanceAmount = advanceAmount,
            remainingAmount = remainingAmount,
            commission = commission,
            providerShare = providerShare,
            currency = "YER",
            walletProvider = walletProvider,
            walletNumber = walletNumber,
            walletAccountName = walletAccountName,
            transferId = transferId,
            transferPhoto = transferPhoto,
            isLinkedToBooking = isLinkedToBooking,
            bookingDate = if (isLinkedToBooking) System.currentTimeMillis() else null,
            bookingServiceType = bookingServiceType,
            createdAt = System.currentTimeMillis()
        )

        docRef.set(payment).addOnSuccessListener {
            vm.mainViewModel.triggerNotification("✅ تم إنشاء طلب الدفع بنجاح!")
        }.addOnFailureListener {
            vm.mainViewModel.triggerNotification("❌ فشل إنشاء طلب الدفع: ${it.message}")
        }
    }

    fun confirmPayment(
        paymentId: String,
        transferId: String,
        transferPhoto: String,
        walletProvider: String,
        walletNumber: String,
        walletAccountName: String
    ) {
        val updates = mapOf(
            "status" to "PROCESSING",
            "transferId" to transferId,
            "transferPhoto" to transferPhoto,
            "walletProvider" to walletProvider,
            "walletNumber" to walletNumber,
            "walletAccountName" to walletAccountName,
            "updatedAt" to System.currentTimeMillis()
        )

        vm.db.collection("payments").document(paymentId).update(updates).addOnSuccessListener {
            vm.mainViewModel.triggerNotification("✅ تم تقديم إثبات التحويل بنجاح! بانتظار مراجعة الإدارة.")
        }.addOnFailureListener {
            vm.mainViewModel.triggerNotification("❌ فشل تأكيد الدفع: ${it.message}")
        }
    }

    fun verifyPayment(paymentId: String, isVerified: Boolean, note: String, adminName: String) {
        val cleanId = paymentId.trim()
        if (cleanId.isBlank()) {
            vm.mainViewModel.triggerNotification("❌ معرف الدفعة غير صالح")
            return
        }
        val existingPayment = vm._payments.value.find { it.id == cleanId }
        if (existingPayment == null && vm._payments.value.isNotEmpty()) {
            vm.mainViewModel.triggerNotification("❌ لم يتم العثور على معاملة الدفع المطلوبة: $cleanId")
            return
        }
        val status = if (isVerified) "COMPLETED" else "FAILED"
        val verificationStatus = if (isVerified) "VERIFIED" else "REJECTED"

        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "verificationStatus" to verificationStatus,
            "verificationNote" to note,
            "verifiedBy" to adminName,
            "verifiedAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        if (isVerified) {
            updates["paidAt"] = System.currentTimeMillis()
        }

        vm.db.collection("payments").document(cleanId).update(updates).addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isVerified) "✅ تم قبول وتأكيد عملية الدفع بنجاح!" else "❌ تم رفض عملية الدفع."
            )

            vm.db.collection("payments").document(paymentId).get().addOnSuccessListener { snapshot ->
                val payment = snapshot.toObject(PaymentEntity::class.java)
                if (payment != null) {
                    if (payment.isLinkedToBooking && payment.bookingId.isNotEmpty()) {
                        vm.db.collection("bookings").document(payment.bookingId)
                            .update("status", if (isVerified) "APPROVED" else "PENDING")
                    }
                }
            }
        }.addOnFailureListener {
            vm.mainViewModel.triggerNotification("❌ فشل التحقق من الدفع: ${it.message}")
        }
    }

    fun refundPayment(paymentId: String, reason: String) {
        val updates = mapOf(
            "status" to "REFUNDED",
            "verificationStatus" to "DISPUTED",
            "verificationNote" to reason,
            "updatedAt" to System.currentTimeMillis()
        )

        vm.db.collection("payments").document(paymentId).update(updates).addOnSuccessListener {
            vm.mainViewModel.triggerNotification("🔄 تم استرداد المبلغ بنجاح.")
        }.addOnFailureListener {
            vm.mainViewModel.triggerNotification("❌ فشل استرداد الدفع: ${it.message}")
        }
    }

    // ==========================================
    // 📦 المنتجات والعروض (Products & Offers)
    // ==========================================

    fun saveProduct(product: ProductEntity) {
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

    fun deleteProduct(productId: String) {
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

    fun updateProductPrice(productId: String, newPrice: Double) {
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

    fun saveOffer(offer: Offer) {
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

    fun deleteOffer(offerId: String) {
        vm.mainViewModel.instantRequestViewModel._offers.value =
            vm.mainViewModel.instantRequestViewModel._offers.value.filter { it.id != offerId }
        vm.db.collection("offers").document(offerId).delete()
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("🗑️ تم حذف العرض بنجاح!")
            }
    }

    fun toggleOfferStatus(offerId: String, isActive: Boolean) {
        vm.mainViewModel.instantRequestViewModel._offers.value =
            vm.mainViewModel.instantRequestViewModel._offers.value.map {
                if (it.id == offerId) it.copy(isActive = isActive) else it
            }
        vm.db.collection("offers").document(offerId).update(
            "isActive", isActive,
            "updatedAt", System.currentTimeMillis()
        )
    }

    fun listenToOffersForEntity(
        entityId: String,
        onResult: (List<Offer>) -> Unit
    ): ListenerRegistration {
        return vm.db.collection("offers")
            .whereEqualTo("entityId", entityId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val fetched = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Offer::class.java)?.copy(id = doc.id)
                    }
                    onResult(fetched)
                }
            }
    }

    fun listenToProductsForStore(
        storeId: String,
        onResult: (List<ProductEntity>) -> Unit
    ): ListenerRegistration {
        return vm.db.collection("products")
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
    }

    // ==========================================
    // 🖼️ البنرات الإعلانية (Banners)
    // ==========================================

    fun addNewBanner(
        title: String,
        url: String,
        redirect: String,
        type: String,
        size: String,
        duration: Int,
        displayTime: String = "طوال اليوم"
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

    fun addBanner(
        title: String,
        url: String,
        redirect: String,
        type: String,
        size: String,
        duration: Int,
        displayTime: String = "طوال اليوم"
    ) {
        addNewBanner(title, url, redirect, type, size, duration, displayTime)
    }

    fun deleteBanner(bannerId: String) {
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

    fun reorderBanners(newOrderedList: List<BannerEntity>) {
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

    // ==========================================
    // 🛍️ الطلبات (Orders)
    // ==========================================

    fun placeOrder(order: OrderEntity) {
        val targetId = vm.db.collection("orders").document().id
        val finalOrder = order.copy(id = targetId)
        vm.db.collection("orders").document(targetId).set(finalOrder).addOnSuccessListener {
            vm.mainViewModel.triggerNotification("🛍️ تم تسجيل طلبك بنجاح! رقم الطلب: ${targetId.take(6)}")
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
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

    fun deleteOrder(orderId: String) {
        vm.db.collection("orders").document(orderId).delete().addOnSuccessListener {
            vm.mainViewModel.triggerNotification("🗑️ تم حذف الطلب بنجاح.")
        }
    }

    fun deleteAllOrders(customerPhone: String) {
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

    // ==========================================
    // ⭐ التقييمات والمراجعات (Ratings)
    // ==========================================

    fun addRating(rating: RatingEntity) {
        val targetId = if (rating.id.isBlank()) vm.db.collection("ratings").document().id else rating.id
        val finalRating = rating.copy(id = targetId)
        val currentList = vm.appState._ratings.value.toMutableList()
        currentList.removeAll { it.id == targetId }
        currentList.add(0, finalRating)
        vm.appState._ratings.value = currentList

        vm.db.collection("ratings").document(targetId).set(finalRating).addOnSuccessListener {
            vm.mainViewModel.triggerNotification("⭐ شكراً لتقييمك! تم إرسال تقييمك بنجاح.")
            recalculateTargetRating(rating.targetId, rating.targetType)
        }
    }

    fun addRatingReply(ratingId: String, replyText: String) {
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

    fun deleteRating(ratingId: String) {
        vm.appState._ratings.value = vm.appState._ratings.value.filter { it.id != ratingId }
        vm.db.collection("ratings").document(ratingId).get().addOnSuccessListener { snap ->
            val rating = snap.toObject(RatingEntity::class.java)
            if (rating != null) {
                vm.db.collection("ratings").document(ratingId).delete().addOnSuccessListener {
                    recalculateTargetRating(rating.targetId, rating.targetType)
                }
            }
        }
    }

    fun approveRating(ratingId: String, isApproved: Boolean) {
        vm.db.collection("ratings").document(ratingId).get().addOnSuccessListener { snap ->
            val rating = snap.toObject(RatingEntity::class.java)
            if (rating != null) {
                vm.db.collection("ratings").document(ratingId).set(rating.copy(isApproved = isApproved))
            }
        }
    }

    fun submitRating(@Suppress("UNUSED_PARAMETER") providerId: String, rating: Int) {
        vm.mainViewModel.triggerNotification("⭐ شكراً لتقييمك $rating نجوم!")
    }

    fun submitRating(ratingEntity: RatingEntity, onComplete: () -> Unit = {}) {
        val currentList = vm.appState._ratings.value.toMutableList()
        currentList.removeAll { it.id == ratingEntity.id }
        currentList.add(0, ratingEntity)
        vm.appState._ratings.value = currentList

        vm.db.collection("ratings").document(ratingEntity.id).set(ratingEntity)
            .addOnSuccessListener {
                recalculateTargetRating(ratingEntity.targetId, ratingEntity.targetType)
                onComplete()
            }
            .addOnFailureListener {
                recalculateTargetRating(ratingEntity.targetId, ratingEntity.targetType)
                onComplete()
            }
    }

    fun recalculateTargetRating(targetId: String, targetType: String) {
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
}
