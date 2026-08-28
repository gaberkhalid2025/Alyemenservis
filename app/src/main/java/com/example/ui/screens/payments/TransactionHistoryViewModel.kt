package com.example.ui.screens.payments

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.Transaction
import com.example.util.WalletManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Representing the UI State for Transaction History.
 */
sealed class TransactionUiState {
    object Loading : TransactionUiState()
    data class Success(val transactions: List<Transaction>) : TransactionUiState()
    data class Error(val message: String) : TransactionUiState()
}

/**
 * ViewModel for managing wallet balance and transaction history data.
 *
 * @param context Android application context for initializing WalletManager.
 * @param currentUserId The ID of the current logged-in user.
 */
class TransactionHistoryViewModel(
    context: Context,
    private val currentUserId: String
) : ViewModel() {

    private val walletManager = WalletManager(context.applicationContext)
    private val walletId = "wallet_$currentUserId"

    private val _uiState = MutableStateFlow<TransactionUiState>(TransactionUiState.Loading)
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow("ALL")
    val typeFilter: StateFlow<String> = _typeFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    init {
        refreshTransactions()
    }

    /**
     * Refreshes the transactions and current wallet balance.
     */
    fun refreshTransactions() {
        _uiState.value = TransactionUiState.Loading
        viewModelScope.launch {
            try {
                val currentBalance = walletManager.getBalance(walletId)
                val allTx = walletManager.getTransactions(walletId)
                _balance.value = currentBalance
                _uiState.value = TransactionUiState.Success(allTx)
            } catch (e: Exception) {
                _uiState.value = TransactionUiState.Error(e.localizedMessage ?: "فشل في تحديث بيانات المحفظة.")
            }
        }
    }

    /**
     * Executes a deposit transaction.
     *
     * @param amount The double amount to deposit.
     * @param note Optional text note for the deposit.
     * @return Boolean indicating success or failure.
     */
    fun deposit(amount: Double, note: String = "شحن رصيد محفظة فوري"): Boolean {
        return try {
            if (amount <= 0) return false
            walletManager.deposit(walletId, amount, note)
            refreshTransactions()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Sets the text search query for transactions.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Sets the type filter (e.g. DEPOSIT, PAYMENT, WITHDRAWAL).
     */
    fun setTypeFilter(filter: String) {
        _typeFilter.value = filter
    }

    /**
     * Sets the status filter (e.g. COMPLETED, PENDING).
     */
    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    /**
     * Reactive flow combining transactions, query and filters into a filtered list.
     */
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        _uiState,
        _searchQuery,
        _typeFilter,
        _statusFilter
    ) { state, query, type, status ->
        if (state !is TransactionUiState.Success) {
            emptyList()
        } else {
            state.transactions.filter { tx ->
                val matchesType = when (type) {
                    "DEPOSIT" -> tx.type == "DEPOSIT"
                    "WITHDRAWAL" -> tx.type == "WITHDRAWAL"
                    "PAYMENT" -> tx.type == "PAYMENT"
                    "TRANSFER" -> tx.type == "TRANSFER"
                    "REFUND" -> tx.type == "REFUND"
                    else -> true
                }
                val matchesStatus = when (status) {
                    "COMPLETED" -> tx.status == "COMPLETED"
                    "PENDING" -> tx.status == "PENDING"
                    "FAILED" -> tx.status == "FAILED"
                    "CANCELLED" -> tx.status == "CANCELLED"
                    else -> true
                }
                val matchesSearch = query.isBlank() ||
                        tx.id.contains(query, ignoreCase = true) ||
                        tx.note.contains(query, ignoreCase = true)

                matchesType && matchesStatus && matchesSearch
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Reactive flow calculating the total completed deposits.
     */
    val totalDeposits: StateFlow<Double> = combine(_uiState) { states ->
        val state = states.first()
        if (state is TransactionUiState.Success) {
            state.transactions.filter { it.type == "DEPOSIT" && it.status == "COMPLETED" }.sumOf { it.amount }
        } else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * Reactive flow calculating the total completed withdrawals or payments.
     */
    val totalWithdrawals: StateFlow<Double> = combine(_uiState) { states ->
        val state = states.first()
        if (state is TransactionUiState.Success) {
            state.transactions.filter { (it.type == "WITHDRAWAL" || it.type == "PAYMENT") && it.status == "COMPLETED" }.sumOf { it.amount }
        } else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}
