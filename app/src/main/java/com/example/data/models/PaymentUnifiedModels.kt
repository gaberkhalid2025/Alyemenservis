package com.example.data.models

import androidx.annotation.Keep
import com.example.data.*

@Keep
data class PaymentCoreModel(
    val payment: PaymentEntity = PaymentEntity(),
    val wallet: PaymentWalletEntity = PaymentWalletEntity(),
    val internalWallet: InternalWalletEntity = InternalWalletEntity(),
    val adminSettings: PaymentAdminSettingsEntity = PaymentAdminSettingsEntity()
)
