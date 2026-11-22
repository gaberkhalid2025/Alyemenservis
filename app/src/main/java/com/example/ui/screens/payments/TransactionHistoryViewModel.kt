package com.example.ui.screens.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.models.Transaction
import com.example.utils.WalletManager
import com.example.utils.WalletTopUpRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.round

/**
 * Sealed class representing UI States for transaction history.
 */
sealed class TransactionHistoryUiState {
    object Loading : TransactionHistoryUiState()
    data class Success(
        val balance: Long,
        val transactions: List<Transaction>,
        val totalDeposits: Long,
        val totalWithdrawals: Long,
        val topUpRequests: List<WalletTopUpRequest> = emptyList()
    ) : TransactionHistoryUiState()
    data class Error(val message: String) : TransactionHistoryUiState()
}

/**
 * Sealed class representing top-up request submission states.
 */
sealed class TopUpUi {
    object Idle : TopUpUi()
    object Sending : TopUpUi()
    data class Done(val requestId: String, val amount: Long) : TopUpUi()
    data class Failed(val message: String) : TopUpUi()
}

/**
 * ViewModel managing payment wallet, top-up requests, and transactions safely using Long integer arithmetic.
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

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _balance = MutableStateFlow(0L)

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        _allTransactions, _searchQuery, _typeFilter, _statusFilter
    ) { transactions, query, type, status ->
        transactions.filter { tx ->
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
                tx.note.contains(query, ignoreCase = true) ||
                tx.referenceNumber.contains(query, ignoreCase = true)

            matchesType && matchesStatus && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<TransactionHistoryUiState>(TransactionHistoryUiState.Loading)
    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    private val _topUpUi = MutableStateFlow<TopUpUi>(TopUpUi.Idle)
    val topUpUi: StateFlow<TopUpUi> = _topUpUi.asStateFlow()

    private val _operationError = MutableStateFlow<String?>(null)
    val operationError: StateFlow<String?> = _operationError.asStateFlow()

    private var lastRefreshTime = 0L
    private val CACHE_TTL_MS = 15_000L

    init {
        refreshData(forceRefresh = true)
    }

    companion object {
        fun computeTotalsLong(transactions: List<Transaction>): Pair<Long, Long> {
            var totalInflows = 0L
            var totalOutflows = 0L
            for (tx in transactions) {
                if (!tx.status.equals("COMPLETED", ignoreCase = true)) continue
                val amtLong = round(tx.amount).toLong()
                if (amtLong <= 0L) continue
                when (tx.type.uppercase()) {
                    "DEPOSIT", "REFUND" -> totalInflows += amtLong
                    "WITHDRAWAL", "PAYMENT", "TRANSFER" -> totalOutflows += amtLong
                }
            }
            return totalInflows to totalOutflows
        }
    }

    /**
     * Refreshes wallet balances, top-up requests, and transaction logs.
     */
    fun refreshData(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && now - lastRefreshTime < CACHE_TTL_MS && _allTransactions.value.isNotEmpty()) return
        lastRefreshTime = now
        viewModelScope.launch {
            try {
                val balance = walletManager.fetchBalanceLong(walletId)
                val allTx = walletManager.fetchTransactions(walletId)
                val topUps = walletManager.getTopUpRequests(walletId)

                _balance.value = balance
                _allTransactions.value = allTx

                val (totalDep, totalWith) = computeTotalsLong(allTx)

                _uiState.value = TransactionHistoryUiState.Success(
                    balance = balance,
                    transactions = allTx,
                    totalDeposits = totalDep,
                    totalWithdrawals = totalWith,
                    topUpRequests = topUps
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
     * Submit a PENDING wallet top-up request for admin verification (never credits client-side).
     */
    fun submitTopUpRequest(
        amount: Long,
        referenceNumber: String,
        paymentMethod: String = "BANK_TRANSFER",
        note: String = ""
    ) {
        if (_topUpUi.value is TopUpUi.Sending) return
        if (amount <= 0L) {
            _topUpUi.value = TopUpUi.Failed("يرجى إدخال مبلغ صحيح أكبر من الصفر")
            return
        }
        val cleanRef = referenceNumber.trim()
        if (cleanRef.length < 3) {
            _topUpUi.value = TopUpUi.Failed("يرجى إدخال رقم الحوالة أو المرجع المالي للتحقق")
            return
        }

        _topUpUi.value = TopUpUi.Sending
        viewModelScope.launch {
            val result = walletManager.requestTopUp(
                walletId = walletId,
                amount = amount,
                referenceNumber = cleanRef,
                paymentMethod = paymentMethod,
                note = note
            )
            result.onSuccess { req ->
                _topUpUi.value = TopUpUi.Done(req.id, req.amount)
                refreshData(forceRefresh = true)
            }.onFailure { err ->
                _topUpUi.value = TopUpUi.Failed(err.message ?: "تعذر إرسال طلب الشحن، حاول مرة أخرى")
            }
        }
    }

    fun resetTopUpUi() {
        _topUpUi.value = TopUpUi.Idle
    }

    fun clearOperationError() {
        _operationError.value = null
    }

    /**
     * Execute a withdrawal from the wallet with explicit failure reporting.
     */
    fun withdraw(amount: Long, note: String) {
        viewModelScope.launch {
            val result = walletManager.withdraw(walletId, amount.toDouble(), "YER", note)
            result.onSuccess {
                refreshData(forceRefresh = true)
            }.onFailure { err ->
                _operationError.value = err.message ?: "تعذر تنفيذ عملية السحب"
            }
        }
    }
}

class TransactionHistoryViewModelFactory(
    private val walletManager: WalletManager,
    private val walletId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionHistoryViewModel::class.java)) {
            return TransactionHistoryViewModel(walletManager, walletId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

