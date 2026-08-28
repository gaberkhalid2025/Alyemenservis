package com.example.ui.screens.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.Transaction
import com.example.util.WalletManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed class representing UI States for transaction history.
 */
sealed class TransactionHistoryUiState {
    object Loading : TransactionHistoryUiState()
    data class Success(
        val balance: Double,
        val transactions: List<Transaction>,
        val totalDeposits: Double,
        val totalWithdrawals: Double
    ) : TransactionHistoryUiState()
    data class Error(val message: String) : TransactionHistoryUiState()
}

/**
 * ViewModel managing payment wallet and transactions.
 */
class TransactionHistoryViewModel(
    private val walletManager: WalletManager,
    private val walletId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow("ALL")
    val typeFilter: StateFlow<String> = _typeFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _uiState = MutableStateFlow<TransactionHistoryUiState>(TransactionHistoryUiState.Loading)
    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    init {
        refreshData()
    }

    /**
     * Refreshes wallet balances and logs.
     */
    fun refreshData() {
        viewModelScope.launch {
            try {
                val balance = walletManager.getBalance(walletId)
                val allTx = walletManager.getTransactions(walletId)

                val totalDep = allTx.filter { it.type == "DEPOSIT" && it.status == "COMPLETED" }.sumOf { it.amount }
                val totalWith = allTx.filter { (it.type == "WITHDRAWAL" || it.type == "PAYMENT") && it.status == "COMPLETED" }.sumOf { it.amount }

                _uiState.value = TransactionHistoryUiState.Success(
                    balance = balance,
                    transactions = allTx,
                    totalDeposits = totalDep,
                    totalWithdrawals = totalWith
                )
            } catch (e: Exception) {
                _uiState.value = TransactionHistoryUiState.Error(e.message ?: "فشل تحميل البيانات المالية")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(filter: String) {
        _typeFilter.value = filter
    }

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    /**
     * Execute a custom deposit into the wallet.
     */
    fun deposit(amount: Double, note: String): Result<Transaction> {
        val result = walletManager.deposit(walletId, amount, note)
        if (result.isSuccess) {
            refreshData()
        }
        return result
    }

    /**
     * Execute a custom withdrawal from the wallet.
     */
    fun withdraw(amount: Double, note: String): Result<Transaction> {
        val result = walletManager.withdraw(walletId, amount, note)
        if (result.isSuccess) {
            refreshData()
        }
        return result
    }
}
