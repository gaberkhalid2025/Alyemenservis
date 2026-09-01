package com.example.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UrgentRequestEntity
import com.example.util.OfflineQueueManager
import com.example.util.OfflineRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ⚡ RequestViewModel
 * إدارة طلبات "اطلب خدمتك الآن" والطلبات المستعجلة والعروض.
 */
class RequestViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val offlineQueueManager: OfflineQueueManager by lazy { OfflineQueueManager(getApplication()) }

    private val _requests = MutableStateFlow<List<UrgentRequestEntity>>(emptyList())
    val requests: StateFlow<List<UrgentRequestEntity>> = _requests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadUserRequests(userPhone: String) {
        if (userPhone.isBlank()) return
        firestore.collection("instant_requests")
            .whereEqualTo("customerPhone", userPhone)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(UrgentRequestEntity::class.java) }
                    _requests.value = list
                }
            }
    }

    fun submitUrgentRequest(req: UrgentRequestEntity, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val id = if (req.id.isNotBlank()) req.id else UUID.randomUUID().toString()
            val finalReq = req.copy(id = id, createdAt = System.currentTimeMillis())

            firestore.collection("instant_requests").document(id).set(finalReq)
                .addOnSuccessListener {
                    _isLoading.value = false
                    _requests.value = listOf(finalReq) + _requests.value
                    onComplete(true)
                }
                .addOnFailureListener {
                    _isLoading.value = false
                    offlineQueueManager.addToQueue(
                        OfflineRequest(
                            id = id,
                            type = "REQUEST",
                            data = mapOf(
                                "id" to id,
                                "customerPhone" to finalReq.customerPhone,
                                "category" to finalReq.category,
                                "description" to finalReq.description
                            )
                        )
                    )
                    onComplete(true)
                }
        }
    }
}
