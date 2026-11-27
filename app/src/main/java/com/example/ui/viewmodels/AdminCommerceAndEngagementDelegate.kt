package com.example.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.data.BannerEntity
import com.example.data.CouponEntity
import com.example.data.InternalWalletEntity
import com.example.data.OrderEntity
import com.example.data.PaymentWalletEntity
import com.example.data.ProductEntity
import com.example.data.RatingEntity
import com.example.data.ReportEntity
import com.example.data.models.Offer
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
            .addOnSuccessListener { onComplete() }
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
                operation = { vm.crud.saveEntity("coupons", couponId, finalCoupon) },
                onSuccess = { vm.mainViewModel.triggerNotification("🎫 تم حفظ الكوبون بنجاح: ${finalCoupon.code}") },
                onError = { vm.mainViewModel.triggerNotification("⚠️ تم حفظ الكوبون محلياً، سيتم المزامنة تلقائياً") }
            )
        }
    }

    fun deleteCoupon(couponId: String) {
        vm.viewModelScope.launch {
            vm.runSafeFirestoreCall(
                operation = { vm.crud.deleteEntity("coupons", couponId, false) },
                onSuccess = { vm.mainViewModel.triggerNotification("🗑️ تم حذف الكوبون") },
                onError = { vm.mainViewModel.triggerNotification("⚠️ تم حذف الكوبون محلياً، سيتم المزامنة تلقائياً") }
            )
        }
    }

    // ==========================================
    // 💰 المحافظ والمدفوعات (Wallets & Payments)
    // ==========================================

    fun saveInternalWallet(wallet: InternalWalletEntity) = saveInternalWalletOp(vm, wallet)

    fun performWalletTransaction(
        walletId: String,
        ownerName: String,
        ownerPhone: String,
        ownerType: String,
        type: String,
        amount: Double,
        note: String
    ) = performWalletTransactionOp(vm, walletId, ownerName, ownerPhone, ownerType, type, amount, note)

    fun addPaymentWallet(wallet: PaymentWalletEntity) = addPaymentWalletOp(vm, wallet)
    fun updatePaymentWallet(wallet: PaymentWalletEntity) = updatePaymentWalletOp(vm, wallet)
    fun deletePaymentWallet(walletId: String) = deletePaymentWalletOp(vm, walletId)
    fun togglePaymentWalletVisibility(walletId: String, currentVisible: Boolean) =
        togglePaymentWalletVisibilityOp(vm, walletId, currentVisible)

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
    ) = createPaymentOp(
        vm, userId, providerId, amount, method, bookingId, isLinkedToBooking,
        bookingServiceType, walletProvider, walletNumber, walletAccountName,
        transferId, transferPhoto, status
    )

    fun confirmPayment(
        paymentId: String,
        transferId: String,
        transferPhoto: String,
        walletProvider: String,
        walletNumber: String,
        walletAccountName: String
    ) = confirmPaymentOp(vm, paymentId, transferId, transferPhoto, walletProvider, walletNumber, walletAccountName)

    fun verifyPayment(paymentId: String, isVerified: Boolean, note: String, adminName: String) =
        verifyPaymentOp(vm, paymentId, isVerified, note, adminName)

    fun refundPayment(paymentId: String, reason: String) = refundPaymentOp(vm, paymentId, reason)

    // ==========================================
    // 📦 المنتجات والعروض (Products & Offers)
    // ==========================================

    fun saveProduct(product: ProductEntity) = saveProductOp(vm, product)
    fun deleteProduct(productId: String) = deleteProductOp(vm, productId)
    fun updateProductPrice(productId: String, newPrice: Double) = updateProductPriceOp(vm, productId, newPrice)
    fun saveOffer(offer: Offer) = saveOfferOp(vm, offer)
    fun deleteOffer(offerId: String) = deleteOfferOp(vm, offerId)
    fun toggleOfferStatus(offerId: String, isActive: Boolean) = toggleOfferStatusOp(vm, offerId, isActive)
    fun listenToOffersForEntity(entityId: String, onResult: (List<Offer>) -> Unit): ListenerRegistration =
        listenToOffersForEntityOp(vm, entityId, onResult)
    fun listenToProductsForStore(storeId: String, onResult: (List<ProductEntity>) -> Unit): ListenerRegistration =
        listenToProductsForStoreOp(vm, storeId, onResult)

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
    ) = addNewBannerOp(vm, title, url, redirect, type, size, duration, displayTime)

    fun addBanner(
        title: String,
        url: String,
        redirect: String,
        type: String,
        size: String,
        duration: Int,
        displayTime: String = "طوال اليوم"
    ) = addNewBanner(title, url, redirect, type, size, duration, displayTime)

    fun deleteBanner(bannerId: String) = deleteBannerOp(vm, bannerId)
    fun reorderBanners(newOrderedList: List<BannerEntity>) = reorderBannersOp(vm, newOrderedList)

    // ==========================================
    // 🛍️ الطلبات (Orders)
    // ==========================================

    fun placeOrder(order: OrderEntity) = placeOrderOp(vm, order)
    fun updateOrderStatus(orderId: String, status: String) = updateOrderStatusOp(vm, orderId, status)
    fun deleteOrder(orderId: String) = deleteOrderOp(vm, orderId)
    fun deleteAllOrders(customerPhone: String) = deleteAllOrdersOp(vm, customerPhone)

    // ==========================================
    // ⭐ التقييمات والمراجعات (Ratings)
    // ==========================================

    fun addRating(rating: RatingEntity) = addRatingOp(vm, rating)
    fun addRatingReply(ratingId: String, replyText: String) = addRatingReplyOp(vm, ratingId, replyText)
    fun deleteRating(ratingId: String) = deleteRatingOp(vm, ratingId)
    fun approveRating(ratingId: String, isApproved: Boolean) = approveRatingOp(vm, ratingId, isApproved)

    fun submitRating(@Suppress("UNUSED_PARAMETER") providerId: String, rating: Int) {
        vm.mainViewModel.triggerNotification("⭐ شكراً لتقييمك $rating نجوم!")
    }

    fun submitRating(ratingEntity: RatingEntity, onComplete: () -> Unit = {}) =
        submitRatingEntityOp(vm, ratingEntity, onComplete)

    fun recalculateTargetRating(targetId: String, targetType: String) =
        recalculateTargetRatingOp(vm, targetId, targetType)
}
