package com.example.ui.screens.register.forms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SimplifiedRegistrationState(
    val imageUri: String = "",
    val entityName: String = "",
    val entityNameError: String? = null,
    val managerName: String = "",
    val managerNameError: String? = null,
    val phone: String = "",
    val phoneError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val confirmPassword: String = "",
    val confirmPasswordError: String? = null,
    val city: String = "صنعاء",
    val specialization: String = "",
    val agreedToTerms: Boolean = false,
    val isLoading: Boolean = false,
    val successMessage: String? = null
) {
    val isFormValid: Boolean
        get() = entityName.isNotBlank() && phone.length >= 9 && password.length >= 6 && password == confirmPassword && agreedToTerms
}

sealed class RegistrationUiEvent {
    data class ImageChanged(val uri: String) : RegistrationUiEvent()
    data class EntityNameChanged(val name: String) : RegistrationUiEvent()
    data class ManagerNameChanged(val name: String) : RegistrationUiEvent()
    data class PhoneChanged(val phone: String) : RegistrationUiEvent()
    data class PasswordChanged(val pass: String) : RegistrationUiEvent()
    data class ConfirmPasswordChanged(val pass: String) : RegistrationUiEvent()
    data class CityChanged(val city: String) : RegistrationUiEvent()
    data class SpecializationChanged(val spec: String) : RegistrationUiEvent()
    data class AgreedToTermsChanged(val agreed: Boolean) : RegistrationUiEvent()
}

class SimplifiedRegistrationViewModel(application: Application) : AndroidViewModel(application) {
    private val draftManager = RegistrationDraftManager(application)
    private val _state = MutableStateFlow(SimplifiedRegistrationState())
    val state: StateFlow<SimplifiedRegistrationState> = _state.asStateFlow()
    private var currentRole: String = "PROVIDER"

    fun loadDraft(role: String) {
        currentRole = role
        val draft = draftManager.getDraft(role)
        _state.value = _state.value.copy(
            entityName = draft["entityName"] ?: _state.value.entityName,
            managerName = draft["managerName"] ?: _state.value.managerName,
            phone = draft["phone"] ?: _state.value.phone,
            city = draft["city"] ?: _state.value.city,
            specialization = draft["specialization"] ?: _state.value.specialization
        )
    }

    fun onEvent(event: RegistrationUiEvent) {
        when (event) {
            is RegistrationUiEvent.ImageChanged -> _state.value = _state.value.copy(imageUri = event.uri)
            is RegistrationUiEvent.EntityNameChanged -> _state.value = _state.value.copy(entityName = event.name, entityNameError = null)
            is RegistrationUiEvent.ManagerNameChanged -> _state.value = _state.value.copy(managerName = event.name, managerNameError = null)
            is RegistrationUiEvent.PhoneChanged -> _state.value = _state.value.copy(phone = event.phone, phoneError = null)
            is RegistrationUiEvent.PasswordChanged -> _state.value = _state.value.copy(password = event.pass, passwordError = null)
            is RegistrationUiEvent.ConfirmPasswordChanged -> _state.value = _state.value.copy(confirmPassword = event.pass, confirmPasswordError = null)
            is RegistrationUiEvent.CityChanged -> _state.value = _state.value.copy(city = event.city)
            is RegistrationUiEvent.SpecializationChanged -> _state.value = _state.value.copy(specialization = event.spec)
            is RegistrationUiEvent.AgreedToTermsChanged -> _state.value = _state.value.copy(agreedToTerms = event.agreed)
        }
    }

    fun submit(onSuccess: (Map<String, String>) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val data = mapOf(
                "role" to currentRole,
                "entityName" to _state.value.entityName,
                "managerName" to _state.value.managerName,
                "phone" to _state.value.phone,
                "city" to _state.value.city,
                "specialization" to _state.value.specialization,
                "imageUri" to _state.value.imageUri
            )
            draftManager.clearDraft(currentRole)
            _state.value = _state.value.copy(isLoading = false, successMessage = "تم التسجيل بنجاح")
            onSuccess(data)
        }
    }
}
