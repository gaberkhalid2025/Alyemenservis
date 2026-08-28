package com.example.ui.screens.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repositories.IRegistrationRepository
import com.example.domain.entities.RegistrationEntity
import com.example.domain.usecases.RegisterProviderUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 🧠 RegisterProviderViewModel
 * Specialized ViewModel managing provider registration wizard state and submission.
 */
class RegisterProviderViewModel(
    private val registerProviderUseCase: RegisterProviderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<RegistrationEvent>()
    val eventFlow: SharedFlow<RegistrationEvent> = _eventFlow.asSharedFlow()

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(fullName = name, errorMessage = null)
    }

    fun updatePhone(phone: String) {
        _uiState.value = _uiState.value.copy(phone = phone, errorMessage = null)
    }

    fun updatePassword(pass: String) {
        _uiState.value = _uiState.value.copy(passwordPin = pass, errorMessage = null)
    }

    fun updateCity(city: String) {
        _uiState.value = _uiState.value.copy(city = city)
    }

    fun updateCategory(cat: String) {
        _uiState.value = _uiState.value.copy(category = cat)
    }

    fun updateExperience(exp: String) {
        _uiState.value = _uiState.value.copy(experienceYears = exp)
    }

    fun updateBio(bio: String) {
        _uiState.value = _uiState.value.copy(bio = bio)
    }

    fun nextStep() {
        val state = _uiState.value
        if (state.currentStep == 1) {
            if (state.fullName.isBlank()) {
                _uiState.value = state.copy(errorMessage = "يرجى كتابة الاسم الكامل")
                return
            }
            if (state.phone.length < 9) {
                _uiState.value = state.copy(errorMessage = "يرجى كتابة رقم هاتف صحيح من 9 أرقام")
                return
            }
        } else if (state.currentStep == 2) {
            if (state.category.isBlank()) {
                _uiState.value = state.copy(errorMessage = "يرجى اختيار التخصص أو الحرفة")
                return
            }
        }
        _uiState.value = state.copy(currentStep = (state.currentStep + 1).coerceAtMost(3), errorMessage = null)
    }

    fun prevStep() {
        val state = _uiState.value
        _uiState.value = state.copy(currentStep = (state.currentStep - 1).coerceAtLeast(1), errorMessage = null)
    }

    fun submitRegistration() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            val providerEntity = RegistrationEntity.Provider(
                fullName = state.fullName,
                phone = state.phone,
                professionCategory = state.category,
                city = state.city,
                experienceYears = state.experienceYears.toIntOrNull() ?: 1,
                bio = state.bio,
                identityDocumentUrl = state.identityUrl,
                workImages = state.selectedImageUris,
                passwordHash = state.passwordPin
            )

            val result = registerProviderUseCase(providerEntity)
            result.onSuccess { docId ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    successMessage = "تم ارسال طلب الانضمام بنجاح! سيتم مراجعة طلبك خلال 24 ساعة."
                )
                _eventFlow.emit(RegistrationEvent.NavigateToSuccess(docId))
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = ex.localizedMessage ?: "حدث خطأ أثناء إرسال الطلب"
                )
                _eventFlow.emit(RegistrationEvent.ShowToast(ex.localizedMessage ?: "خطأ"))
            }
        }
    }
}
