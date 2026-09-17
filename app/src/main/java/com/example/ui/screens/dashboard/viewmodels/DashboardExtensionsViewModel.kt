package com.example.ui.screens.dashboard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repositories.IDashboardExtensionsRepository
import com.example.data.repositories.DashboardExtensionsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.data.SpecialOfferEntity
import com.example.data.repositories.InventoryItem
import com.example.data.repositories.LoyaltyProgram
import com.example.data.repositories.StaffMember

class DashboardExtensionsViewModel(
    private val ownerId: String,
    private val repository: IDashboardExtensionsRepository = DashboardExtensionsRepositoryImpl()
) : ViewModel() {

    private val _specialOffers = MutableStateFlow<List<SpecialOfferEntity>>(emptyList())
    val specialOffers: StateFlow<List<SpecialOfferEntity>> = _specialOffers.asStateFlow()

    private val _coupons = MutableStateFlow<List<SpecialOfferEntity>>(emptyList())
    val coupons: StateFlow<List<SpecialOfferEntity>> = _coupons.asStateFlow()

    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    private val _loyaltyPrograms = MutableStateFlow<List<LoyaltyProgram>>(emptyList())
    val loyaltyPrograms: StateFlow<List<LoyaltyProgram>> = _loyaltyPrograms.asStateFlow()

    private val _staff = MutableStateFlow<List<StaffMember>>(emptyList())
    val staff: StateFlow<List<StaffMember>> = _staff.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSpecialOffers(ownerId).collect { _specialOffers.value = it }
        }
        viewModelScope.launch {
            repository.getCoupons(ownerId).collect { _coupons.value = it }
        }
        viewModelScope.launch {
            repository.getInventory(ownerId).collect { _inventory.value = it }
        }
        viewModelScope.launch {
            repository.getLoyaltyPrograms(ownerId).collect { _loyaltyPrograms.value = it }
        }
        viewModelScope.launch {
            repository.getStaff(ownerId).collect { _staff.value = it }
        }
    }
}
