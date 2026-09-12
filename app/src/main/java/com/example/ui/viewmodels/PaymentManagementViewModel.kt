package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.InternalWalletEntity
import com.example.data.PaymentEntity
import com.example.data.PaymentWalletEntity
import com.example.data.WalletTransactionEntity
import com.example.ui.helpers.AppState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class PaymentManagementViewModel @Inject constructor(
    val appState: AppState,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    val crud = AdminCrudOperations(db)

    val _internalWallets: MutableStateFlow<List<InternalWalletEntity>> get() = appState._internalWallets
    val internalWallets: StateFlow<List<InternalWalletEntity>> = _internalWallets.asStateFlow()

    val _walletTransactions: MutableStateFlow<List<WalletTransactionEntity>> get() = appState._walletTransactions
    val walletTransactions: StateFlow<List<WalletTransactionEntity>> = _walletTransactions.asStateFlow()

    val _paymentWallets: MutableStateFlow<List<PaymentWalletEntity>> get() = appState._paymentWallets
    val paymentWallets: StateFlow<List<PaymentWalletEntity>> = _paymentWallets.asStateFlow()

    val _payments: MutableStateFlow<List<PaymentEntity>> get() = appState._payments
    val payments: StateFlow<List<PaymentEntity>> = _payments.asStateFlow()

    var onTriggerNotification: ((String) -> Unit)? = null

    fun saveInternalWallet(wallet: InternalWalletEntity) {
        viewModelScope.launch {
            crud.saveEntity("internal_wallets", wallet.id, wallet,
                onSuccess = { onTriggerNotification?.invoke("✅ تم تحديث المحفظة بنجاح") }
            )
        }
    }

    fun performWalletTransaction(
        walletId: String,
        amount: Double,
        type: String, // DEPOSIT, WITHDRAW, TRANSFER, FEE, REWARD
        description: String,
        adminName: String = "الأدمن"
    ) {
        val currentWallet = _internalWallets.value.find { it.id == walletId } ?: return
        val newBalance = if (type == "DEPOSIT" || type == "REWARD") {
            currentWallet.balance + amount
        } else {
            (currentWallet.balance - amount).coerceAtLeast(0.0)
        }

        val tx = WalletTransactionEntity(
            id = UUID.randomUUID().toString(),
            walletId = walletId,
            type = type,
            amount = amount,
            balanceAfter = newBalance,
            note = description,
            performByAdmin = true,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            crud.saveEntity("wallet_transactions", tx.id, tx)
            crud.updateFields("internal_wallets", walletId, mapOf(
                "balance" to newBalance,
                "lastTransactionAt" to System.currentTimeMillis()
            ), onSuccess = {
                onTriggerNotification?.invoke("تم تنفيذ حركة مالية ($type) بمبلغ $amount بنجاح")
            })
        }
    }

    fun addPaymentWallet(wallet: PaymentWalletEntity) {
        viewModelScope.launch {
            crud.saveEntity("payment_wallets", wallet.id, wallet,
                onSuccess = { onTriggerNotification?.invoke("✅ تم إضافة المحفظة المالية بنجاح") }
            )
        }
    }

    fun updatePaymentWallet(wallet: PaymentWalletEntity) {
        viewModelScope.launch {
            crud.saveEntity("payment_wallets", wallet.id, wallet,
                onSuccess = { onTriggerNotification?.invoke("✏️ تم تحديث بيانات المحفظة بنجاح") }
            )
        }
    }

    fun deletePaymentWallet(walletId: String) {
        viewModelScope.launch {
            crud.deleteEntity("payment_wallets", walletId, softDelete = false,
                onSuccess = { onTriggerNotification?.invoke("🗑️ تم حذف المحفظة بنجاح") }
            )
        }
    }

    fun togglePaymentWalletVisibility(walletId: String, currentVisible: Boolean) {
        viewModelScope.launch {
            crud.toggleEntityStatus("payment_wallets", walletId, "isVisible", !currentVisible)
        }
    }

    fun createPayment(payment: PaymentEntity) {
        viewModelScope.launch {
            crud.saveEntity("payments", payment.id, payment,
                onSuccess = { onTriggerNotification?.invoke("💳 تم تسجيل عملية الدفع بنجاح") }
            )
        }
    }

    fun confirmPayment(paymentId: String, receiptNumber: String, adminName: String) {
        viewModelScope.launch {
            crud.updateFields("payments", paymentId, mapOf(
                "status" to "CONFIRMED",
                "receiptNumber" to receiptNumber,
                "verifiedBy" to adminName,
                "verifiedAt" to System.currentTimeMillis()
            ), onSuccess = {
                onTriggerNotification?.invoke("✅ تم تأكيد استلام الدفعة")
            })
        }
    }

    fun verifyPayment(paymentId: String, isVerified: Boolean, note: String, adminName: String) {
        val status = if (isVerified) "VERIFIED" else "REJECTED"
        viewModelScope.launch {
            crud.updateFields("payments", paymentId, mapOf(
                "status" to status,
                "adminNote" to note,
                "verifiedBy" to adminName,
                "verifiedAt" to System.currentTimeMillis()
            ), onSuccess = {
                onTriggerNotification?.invoke("تم مراجعة الدفعة: $status")
            })
        }
    }

    fun refundPayment(paymentId: String, reason: String) {
        viewModelScope.launch {
            crud.updateFields("payments", paymentId, mapOf(
                "status" to "REFUNDED",
                "refundReason" to reason,
                "refundedAt" to System.currentTimeMillis()
            ), onSuccess = {
                onTriggerNotification?.invoke("تم استرجاع الدفعة بنجاح")
            })
        }
    }
}
