package com.example.ui.screens.register.forms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SimplifiedRegistrationState(
    val entityName: String = "",
    val managerName: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val city: String = "صنعاء",
    val specialization: String = "عام",
    val agreedToTerms: Boolean = false,
    
    // Validation Errors
    val entityNameError: String? = null,
    val managerNameError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    
    val isFormValid: Boolean = false,
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class SimplifiedRegistrationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val draftManager = RegistrationDraftManager(application)
    
    private val _state = MutableStateFlow(SimplifiedRegistrationState())
    val state: StateFlow<SimplifiedRegistrationState> = _state.asStateFlow()
    
    private var currentRole: String = "GUEST"

    fun loadDraft(role: String) {
        currentRole = role
        val draft = draftManager.getDraft(role)
        if (draft.isNotEmpty()) {
            _state.update { 
                it.copy(
                    entityName = draft["entityName"] ?: "",
                    managerName = draft["managerName"] ?: "",
                    phone = draft["phone"] ?: "",
                    password = draft["password"] ?: "",
                    confirmPassword = draft["confirmPassword"] ?: "",
                    city = draft["city"] ?: "صنعاء",
                    specialization = draft["specialization"] ?: "عام"
                )
            }
            validateAll()
        }
    }

    private fun saveDraft() {
        val st = _state.value
        val draftMap = mapOf(
            "entityName" to st.entityName,
            "managerName" to st.managerName,
            "phone" to st.phone,
            "password" to st.password,
            "confirmPassword" to st.confirmPassword,
            "city" to st.city,
            "specialization" to st.specialization
        )
        draftManager.saveDraft(currentRole, draftMap)
    }

    fun onEvent(event: RegistrationEvent) {
        when (event) {
            is RegistrationEvent.EntityNameChanged -> {
                _state.update { it.copy(entityName = event.name) }
                validateEntityName(event.name)
            }
            is RegistrationEvent.ManagerNameChanged -> {
                _state.update { it.copy(managerName = event.name) }
                validateManagerName(event.name)
            }
            is RegistrationEvent.PhoneChanged -> {
                _state.update { it.copy(phone = event.phone) }
                validatePhone(event.phone)
            }
            is RegistrationEvent.PasswordChanged -> {
                _state.update { it.copy(password = event.password) }
                validatePassword(event.password)
                validateConfirmPassword(_state.value.confirmPassword, event.password)
            }
            is RegistrationEvent.ConfirmPasswordChanged -> {
                _state.update { it.copy(confirmPassword = event.password) }
                validateConfirmPassword(event.password, _state.value.password)
            }
            is RegistrationEvent.CityChanged -> {
                _state.update { it.copy(city = event.city) }
            }
            is RegistrationEvent.SpecializationChanged -> {
                _state.update { it.copy(specialization = event.spec) }
            }
            is RegistrationEvent.AgreedToTermsChanged -> {
                _state.update { it.copy(agreedToTerms = event.agreed) }
                validateAll()
            }
        }
        saveDraft()
        checkOverallValidity()
    }

    private fun validateEntityName(name: String) {
        val error = if (name.trim().split(" ").size < 3 && currentRole == "CLIENT") {
            "يرجى إدخال الاسم الثلاثي الكامل"
        } else if (name.trim().isEmpty()) {
            "هذا الحقل مطلوب"
        } else null
        _state.update { it.copy(entityNameError = error) }
    }

    private fun validateManagerName(name: String) {
        val error = if (name.trim().isEmpty()) "اسم المسؤول مطلوب" else null
        _state.update { it.copy(managerNameError = error) }
    }

    private fun validatePhone(phone: String) {
        val cleanPhone = phone.trim()
        val error = if (cleanPhone.length != 9 || !(cleanPhone.startsWith("77") || cleanPhone.startsWith("73") || cleanPhone.startsWith("71") || cleanPhone.startsWith("70") || cleanPhone.startsWith("78"))) {
            "رقم الهاتف يجب أن يكون 9 أرقام ويبدأ بـ 77, 73, 71, 70, أو 78"
        } else null
        _state.update { it.copy(phoneError = error) }
    }

    private fun validatePassword(password: String) {
        val error = if (password.length < 8 || !password.any { it.isDigit() } || !password.any { it.isUpperCase() }) {
            "كلمة المرور يجب أن تحتوي على 8 أحرف، حرف كبير، ورقم"
        } else null
        _state.update { it.copy(passwordError = error) }
    }

    private fun validateConfirmPassword(confirm: String, original: String) {
        val error = if (confirm != original) {
            "كلمة المرور غير متطابقة"
        } else null
        _state.update { it.copy(confirmPasswordError = error) }
    }

    private fun validateAll() {
        val st = _state.value
        validateEntityName(st.entityName)
        validateManagerName(st.managerName)
        validatePhone(st.phone)
        validatePassword(st.password)
        validateConfirmPassword(st.confirmPassword, st.password)
        checkOverallValidity()
    }

    private fun checkOverallValidity() {
        val st = _state.value
        val isValid = st.entityNameError == null && 
                      st.managerNameError == null && 
                      st.phoneError == null && 
                      st.passwordError == null && 
                      st.confirmPasswordError == null &&
                      st.entityName.isNotBlank() &&
                      st.phone.isNotBlank() &&
                      st.password.isNotBlank() &&
                      st.agreedToTerms
        _state.update { it.copy(isFormValid = isValid) }
    }

    fun submit(onSubmitSuccess: (Map<String, String>) -> Unit) {
        validateAll()
        if (_state.value.isFormValid) {
            _state.update { it.copy(isLoading = true) }
            
            // Simulating API call
            viewModelScope.launch {
                kotlinx.coroutines.delay(1500)
                
                draftManager.clearDraft(currentRole)
                _state.update { it.copy(isLoading = false, successMessage = "تم التسجيل بنجاح!") }
                
                onSubmitSuccess(mapOf(
                    "entityName" to _state.value.entityName,
                    "managerName" to _state.value.managerName,
                    "phone" to _state.value.phone,
                    "password" to _state.value.password,
                    "city" to _state.value.city,
                    "specialization" to _state.value.specialization
                ))
            }
        }
    }
    
    fun clearMessages() {
        _state.update { it.copy(successMessage = null, errorMessage = null) }
    }
}

sealed class RegistrationEvent {
    data class EntityNameChanged(val name: String) : RegistrationEvent()
    data class ManagerNameChanged(val name: String) : RegistrationEvent()
    data class PhoneChanged(val phone: String) : RegistrationEvent()
    data class PasswordChanged(val password: String) : RegistrationEvent()
    data class ConfirmPasswordChanged(val password: String) : RegistrationEvent()
    data class CityChanged(val city: String) : RegistrationEvent()
    data class SpecializationChanged(val spec: String) : RegistrationEvent()
    data class AgreedToTermsChanged(val agreed: Boolean) : RegistrationEvent()
}
