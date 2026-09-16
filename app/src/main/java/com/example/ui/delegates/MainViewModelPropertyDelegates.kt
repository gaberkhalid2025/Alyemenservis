package com.example.ui

import androidx.lifecycle.viewModelScope
import com.example.data.PropertyEntity
import kotlinx.coroutines.launch

fun MainViewModel.updatePropertyEntity(property: PropertyEntity) {
    viewModelScope.launch { adminViewModel.crud.saveEntity("properties", property.id, property) }
    triggerNotification("✅ تم تحديث بيانات العقار بنجاح")
}

fun MainViewModel.setPropertyBlocked(propertyId: String, isBlocked: Boolean, reason: String = "") =
    adminViewModel.setPropertyBlocked(propertyId, isBlocked, reason)
