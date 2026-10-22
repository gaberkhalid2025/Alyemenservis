package com.example.data.models

import androidx.annotation.Keep
import com.example.data.*

/**
 * 🏦 ExternalPaymentGatewayWallet: يمثل حسابات وبوابات الدفع الخارجية (مثل الكريمي، جيب، جوالي) التي يحول إليها العملاء.
 */
typealias ExternalPaymentGatewayWallet = PaymentWalletEntity

/**
 * 💰 UserBalanceWallet: يمثل المحفظة الداخلية لرصيد المزود أو المستخدم داخل النظام.
 */
typealias UserBalanceWallet = InternalWalletEntity

@Keep
data class PaymentCoreModel(
    val payment: PaymentEntity = PaymentEntity(),
    val wallet: ExternalPaymentGatewayWallet = ExternalPaymentGatewayWallet(),
    val internalWallet: UserBalanceWallet = UserBalanceWallet(),
    val adminSettings: PaymentAdminSettingsEntity = PaymentAdminSettingsEntity()
) {
    val externalGatewayWallet: ExternalPaymentGatewayWallet get() = wallet
    val userBalanceWallet: UserBalanceWallet get() = internalWallet
}

typealias UnifiedBooking = com.example.data.BookingEntity
typealias UnifiedRating = com.example.data.RatingEntity
