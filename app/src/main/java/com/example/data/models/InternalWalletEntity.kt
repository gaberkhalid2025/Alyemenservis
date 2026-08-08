package com.example.data

import androidx.annotation.Keep

@Keep
data class InternalWalletEntity(
    val id: String = "", // owner id or phone
    val ownerType: String = "PROVIDER", // PROVIDER, STORE, RESTAURANT, CENTER, USER
    val ownerName: String = "",
    val ownerPhone: String = "",
    val balance: Double = 0.0,
    val currency: String = "YER",
    val isBlocked: Boolean = false,
    val defaultWalletNumber: String = "",
    val defaultWalletType: String = "الكريمي",
    val updatedAt: Long = System.currentTimeMillis()
)

@Keep
data class WalletTransactionEntity(
    val id: String = "",
    val walletId: String = "",
    val type: String = "DEPOSIT", // DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT, REFUND
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val note: String = "",
    val performByAdmin: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
