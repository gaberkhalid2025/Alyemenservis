package com.example.ui.screens.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.LoadingState
import com.example.util.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 💳 PaymentsViewModel
 * إدارة المعاملات المالية، رصيد المحفظة وعمليات الشحن والسحب عبر Firebase Firestore
 */
class PaymentsViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _transactionsState = MutableStateFlow<LoadingState<List<Transaction>>>(LoadingState.Idle)
    val transactionsState: StateFlow<LoadingState<List<Transaction>>> = _transactionsState.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance.asStateFlow()

    private val _actionState = MutableStateFlow<LoadingState<String>>(LoadingState.Idle)
    val actionState: StateFlow<LoadingState<String>> = _actionState.asStateFlow()

    private var transactionsListener: ListenerRegistration? = null
    private var walletListener: ListenerRegistration? = null

    /**
     * الاستماع لمعاملات ورصيد محفظة المستخدم
     */
    fun listenToUserWallet(userId: String) {
        val walletId = if (userId.startsWith("wallet_")) userId else "wallet_$userId"

        // 1. الاستماع لرصيد المحفظة
        walletListener?.remove()
        walletListener = firestore.collection("wallets").document(walletId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val bal = snapshot.getDouble("balance") ?: 0.0
                    _balance.value = bal
                } else {
                    _balance.value = 0.0
                }
            }

        // 2. الاستماع للمعاملات
        _transactionsState.value = LoadingState.Loading
        transactionsListener?.remove()
        transactionsListener = firestore.collection("transactions")
            .whereEqualTo("walletId", walletId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _transactionsState.value = LoadingState.Error(error.localizedMessage ?: "فشل تحميل المعاملات", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val docWalletId = doc.getString("walletId") ?: walletId
                        val amount = doc.getDouble("amount") ?: 0.0
                        val type = doc.getString("type") ?: "DEPOSIT"
                        val status = doc.getString("status") ?: "COMPLETED"
                        val note = doc.getString("note") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val balanceAfter = doc.getDouble("balanceAfter") ?: 0.0
                        Transaction(
                            id = id,
                            walletId = docWalletId,
                            amount = amount,
                            balanceAfter = balanceAfter,
                            type = type,
                            status = status,
                            note = note,
                            timestamp = timestamp
                        )
                    }.sortedByDescending { it.timestamp }

                    _transactions.value = list
                    if (list.isEmpty()) {
                        _transactionsState.value = LoadingState.Empty
                    } else {
                        _transactionsState.value = LoadingState.Success(list)
                    }
                } else {
                    _transactions.value = emptyList()
                    _transactionsState.value = LoadingState.Empty
                }
            }
    }

    /**
     * شحن رصيد المحفظة
     */
    fun deposit(
        userId: String,
        amount: Double,
        note: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (amount <= 0.0) {
            onError("يرجى إدخال مبلغ صالح أكبر من صفر")
            return
        }

        val walletId = if (userId.startsWith("wallet_")) userId else "wallet_$userId"
        val txId = "TX-${UUID.randomUUID().toString().take(8).uppercase()}"

        _actionState.value = LoadingState.Loading
        viewModelScope.launch {
            val batch = firestore.batch()

            // 1. إنشاء المعاملة
            val txRef = firestore.collection("transactions").document(txId)
            val txData = hashMapOf(
                "id" to txId,
                "walletId" to walletId,
                "amount" to amount,
                "type" to "DEPOSIT",
                "status" to "COMPLETED",
                "note" to note.ifBlank { "شحن رصيد محفظة فوري" },
                "timestamp" to System.currentTimeMillis(),
                "referenceId" to ""
            )
            batch.set(txRef, txData)

            // 2. تحديث الرصيد
            val walletRef = firestore.collection("wallets").document(walletId)
            val newBalance = _balance.value + amount
            batch.set(walletRef, mapOf("balance" to newBalance, "lastUpdated" to System.currentTimeMillis()))

            batch.commit()
                .addOnSuccessListener {
                    _balance.value = newBalance
                    _actionState.value = LoadingState.Success("تم شحن الرصيد بنجاح!")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    val err = e.localizedMessage ?: "فشل عملية الشحن"
                    _actionState.value = LoadingState.Error(err, e)
                    onError(err)
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        transactionsListener?.remove()
        walletListener?.remove()
    }
}
