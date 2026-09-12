package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StoreEntity
import com.example.data.ProductEntity
import com.example.ui.helpers.AppState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class StoreManagementViewModel @Inject constructor(
    val appState: AppState,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    val crud = AdminCrudOperations(db)

    val _stores: MutableStateFlow<List<StoreEntity>> get() = appState._stores
    val stores: StateFlow<List<StoreEntity>> = _stores.asStateFlow()

    val _products: MutableStateFlow<List<ProductEntity>> get() = appState._products
    val products: StateFlow<List<ProductEntity>> = _products.asStateFlow()

    var onTriggerNotification: ((String) -> Unit)? = null
    private var productListener: ListenerRegistration? = null

    fun saveStore(store: StoreEntity) {
        viewModelScope.launch {
            crud.saveEntity("stores", store.id, store,
                onSuccess = { onTriggerNotification?.invoke("✅ تم حفظ المتجر بنجاح") },
                onError = { onTriggerNotification?.invoke("❌ فشل حفظ المتجر: ${it.message}") }
            )
        }
    }

    fun deleteStore(storeId: String) {
        viewModelScope.launch {
            crud.deleteEntity("stores", storeId, softDelete = true,
                onSuccess = { onTriggerNotification?.invoke("🗑️ تم نقل المتجر للمهملات") },
                onError = { onTriggerNotification?.invoke("❌ فشل الحذف: ${it.message}") }
            )
        }
    }

    fun restoreStore(storeId: String) {
        viewModelScope.launch {
            crud.updateFields("stores", storeId, mapOf("isDeleted" to false, "deletedAt" to null),
                onSuccess = { onTriggerNotification?.invoke("♻️ تم استعادة المتجر بنجاح") }
            )
        }
    }

    fun deleteStorePermanently(storeId: String) {
        viewModelScope.launch {
            crud.deleteEntity("stores", storeId, softDelete = false,
                onSuccess = { onTriggerNotification?.invoke("🗑️ تم حذف المتجر نهائياً") }
            )
        }
    }

    fun setStoreActive(storeId: String, isActive: Boolean) {
        viewModelScope.launch {
            crud.toggleEntityStatus("stores", storeId, "isActive", isActive)
        }
    }

    fun setStorePinned(storeId: String, isPinned: Boolean) {
        viewModelScope.launch {
            crud.toggleEntityStatus("stores", storeId, "isPinned", isPinned)
        }
    }

    fun setStoreVip(storeId: String, isVip: Boolean) {
        viewModelScope.launch {
            crud.toggleEntityStatus("stores", storeId, "isVip", isVip)
        }
    }

    fun setStoreVerified(storeId: String, isVerified: Boolean) {
        viewModelScope.launch {
            crud.toggleEntityStatus("stores", storeId, "isVerified", isVerified)
        }
    }

    fun setStoreRecommended(storeId: String, isRecommended: Boolean) {
        viewModelScope.launch {
            crud.toggleEntityStatus("stores", storeId, "isRecommended", isRecommended)
        }
    }

    fun setStoreBlocked(storeId: String, isBlocked: Boolean, reason: String = "") {
        viewModelScope.launch {
            crud.updateFields("stores", storeId, mapOf("isBlocked" to isBlocked, "blockReason" to reason))
        }
    }

    fun setStoreChatDisabled(storeId: String, isDisabled: Boolean) {
        viewModelScope.launch {
            crud.toggleEntityStatus("stores", storeId, "isChatDisabled", isDisabled)
        }
    }

    fun setStoreNotificationsDisabled(storeId: String, isDisabled: Boolean) {
        viewModelScope.launch {
            crud.toggleEntityStatus("stores", storeId, "isNotificationsDisabled", isDisabled)
        }
    }

    fun setStorePaymentEnabled(storeId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            crud.toggleEntityStatus("stores", storeId, "isPaymentEnabled", isEnabled)
        }
    }

    fun approveStorePdf(storeId: String, approve: Boolean) {
        viewModelScope.launch {
            crud.toggleEntityStatus("stores", storeId, "isPdfApproved", approve)
        }
    }

    fun listenToProductsForStore(storeId: String, onResult: (List<ProductEntity>) -> Unit) {
        productListener?.remove()
        productListener = db.collection("products")
            .whereEqualTo("storeId", storeId)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val list = snap.toObjects(ProductEntity::class.java)
                    onResult(list)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        productListener?.remove()
    }
}
