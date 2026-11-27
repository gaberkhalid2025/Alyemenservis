package com.example.ui.viewmodels

import com.example.data.InternalWalletEntity
import com.example.data.PaymentEntity
import com.example.data.PaymentWalletEntity
import com.example.data.WalletTransactionEntity

internal fun saveInternalWalletOp(vm: AdminViewModel, wallet: InternalWalletEntity) {
    val targetId = if (wallet.id.isEmpty()) vm.db.collection("internal_wallets").document().id else wallet.id
    val finalW = wallet.copy(id = targetId, updatedAt = System.currentTimeMillis())
    vm.db.collection("internal_wallets").document(targetId).set(finalW)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("✅ تم حفظ بيانات المحفظة الرقمية الداخلية (${finalW.ownerName})")
        }
}

internal fun performWalletTransactionOp(
    vm: AdminViewModel,
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

internal fun addPaymentWalletOp(vm: AdminViewModel, wallet: PaymentWalletEntity) {
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

internal fun updatePaymentWalletOp(vm: AdminViewModel, wallet: PaymentWalletEntity) {
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

internal fun deletePaymentWalletOp(vm: AdminViewModel, walletId: String) {
    vm.db.collection("payment_wallets").document(walletId).delete().addOnSuccessListener {
        vm.mainViewModel.triggerNotification("🗑️ تم حذف المحفظة بنجاح!")
    }.addOnFailureListener {
        vm.mainViewModel.triggerNotification("❌ فشل حذف المحفظة: ${it.message}")
    }
}

internal fun togglePaymentWalletVisibilityOp(vm: AdminViewModel, walletId: String, currentVisible: Boolean) {
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

internal fun createPaymentOp(
    vm: AdminViewModel,
    userId: String,
    providerId: String,
    amount: Double,
    method: String,
    bookingId: String,
    isLinkedToBooking: Boolean,
    bookingServiceType: String,
    walletProvider: String,
    walletNumber: String,
    walletAccountName: String,
    transferId: String,
    transferPhoto: String,
    status: String
) {
    val docRef = vm.db.collection("payments").document()

    val settingsVal = vm.mainViewModel.settingsViewModel._settings.value
    val advanceAmount = if (settingsVal.requireAdvancePayment) {
        var calculated = amount * settingsVal.advancePaymentPercent
        if (calculated < settingsVal.minAdvanceAmount) calculated = settingsVal.minAdvanceAmount
        if (calculated > settingsVal.maxAdvanceAmount) calculated = settingsVal.maxAdvanceAmount
        calculated
    } else 0.0

    val commission = if (settingsVal.isCommissionEnabled) {
        amount * settingsVal.paymentCommissionRate
    } else 0.0

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

internal fun confirmPaymentOp(
    vm: AdminViewModel,
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

internal fun verifyPaymentOp(
    vm: AdminViewModel,
    paymentId: String,
    isVerified: Boolean,
    note: String,
    adminName: String
) {
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

internal fun refundPaymentOp(vm: AdminViewModel, paymentId: String, reason: String) {
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
