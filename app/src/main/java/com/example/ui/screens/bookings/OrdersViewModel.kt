package com.example.ui.screens.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OrderEntity
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class OrderUiState {
    object Idle : OrderUiState()
    object Loading : OrderUiState()
    data class Success(val orders: List<OrderEntity>, val message: String? = null) : OrderUiState()
    data class Error(val errorMessage: String) : OrderUiState()
}

class OrdersViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<OrderUiState>(OrderUiState.Idle)
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    private val _selectedOrderForDeletion = MutableStateFlow<OrderEntity?>(null)
    val selectedOrderForDeletion: StateFlow<OrderEntity?> = _selectedOrderForDeletion.asStateFlow()

    private val _deletionCodeInput = MutableStateFlow("")
    val deletionCodeInput: StateFlow<String> = _deletionCodeInput.asStateFlow()

    private val _showDeleteAllConfirm = MutableStateFlow(false)
    val showDeleteAllConfirm: StateFlow<Boolean> = _showDeleteAllConfirm.asStateFlow()

    fun selectOrderForDeletion(order: OrderEntity?) {
        _selectedOrderForDeletion.value = order
        _deletionCodeInput.value = ""
    }

    fun updateDeletionCodeInput(code: String) {
        _deletionCodeInput.value = code
    }

    fun setShowDeleteAllConfirm(show: Boolean) {
        _showDeleteAllConfirm.value = show
    }

    fun verifyAndDeleteOrder(
        order: OrderEntity,
        inputCode: String,
        mainViewModel: MainViewModel,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val correctCode = (order.id.hashCode().coerceAtLeast(0) % 9000 + 1000).toString()
        if (inputCode.trim() == correctCode) {
            viewModelScope.launch {
                try {
                    _uiState.value = OrderUiState.Loading
                    mainViewModel.deleteOrder(order.id)
                    _selectedOrderForDeletion.value = null
                    _uiState.value = OrderUiState.Success(emptyList(), "تم حذف وإلغاء الطلب بنجاح")
                    onSuccess()
                } catch (e: Exception) {
                    _uiState.value = OrderUiState.Error("فشل في حذف الطلب: ${e.localizedMessage}")
                    onError(e.localizedMessage ?: "حدث خطأ أثناء الحذف")
                }
            }
        } else {
            _uiState.value = OrderUiState.Error("رمز الحذف غير صحيح!")
            onError("رمز الحذف المكون من 4 أرقام غير صحيح!")
        }
    }

    fun confirmDeleteAllOrders(
        phone: String,
        mainViewModel: MainViewModel,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = OrderUiState.Loading
                mainViewModel.deleteAllOrders(phone)
                _showDeleteAllConfirm.value = false
                _uiState.value = OrderUiState.Success(emptyList(), "تمت أرشفة وحذف كافة الطلبات")
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = OrderUiState.Error("فشل في مسح الطلبات: ${e.localizedMessage}")
            }
        }
    }
}
