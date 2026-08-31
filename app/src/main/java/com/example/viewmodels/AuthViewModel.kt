package com.example.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserEntity
import com.example.data.repositories.AuthRepository
import com.example.data.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserAuthState(
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val userRole: String = "CLIENT", // CLIENT, PROVIDER, ADMIN, OWNER
    val isAuthenticated: Boolean = false,
    val isBlocked: Boolean = false
)

/**
 * 🔐 AuthViewModel
 * إدارة تسجيل الدخول، استرجاع الحساب، تحديث FCM Token، والصلاحيات مع حقن الاعتماديات Hilt
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(UserAuthState())
    val authState: StateFlow<UserAuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun login(phone: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            authRepository.loginWithPhone(phone, pass)
                .onSuccess { user ->
                    _isLoading.value = false
                    _authState.value = UserAuthState(
                        userId = user.id,
                        userName = user.name.ifEmpty { "مستخدم" },
                        userPhone = user.phone,
                        userRole = user.role.ifEmpty { "CLIENT" },
                        isAuthenticated = true,
                        isBlocked = user.isBlocked
                    )
                    onResult(true, "تم تسجيل الدخول بنجاح")
                }
                .onFailure { err ->
                    _isLoading.value = false
                    val msg = err.message ?: "خطأ في تسجيل الدخول"
                    _errorMessage.value = msg
                    onResult(false, msg)
                }
        }
    }

    fun registerOrUpdateUser(user: UserEntity, pass: String = "", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.saveOrUpdateUser(user, pass)
                .onSuccess { savedUser ->
                    _isLoading.value = false
                    _authState.value = _authState.value.copy(
                        userId = savedUser.id,
                        userName = savedUser.name,
                        userPhone = savedUser.phone,
                        userRole = savedUser.role,
                        isAuthenticated = true
                    )
                    onResult(true, "تم الحفظ بنجاح")
                }
                .onFailure { err ->
                    _isLoading.value = false
                    val msg = err.message ?: "فشل تسجيل/تحديث البيانات"
                    _errorMessage.value = msg
                    onResult(false, msg)
                }
        }
    }

    fun resetPassword(phone: String, newPass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.resetPassword(phone, newPass)
                .onSuccess {
                    _isLoading.value = false
                    _successMessage.value = "تم تغيير كلمة المرور بنجاح"
                    onResult(true, "تم إعادة تعيين كلمة المرور بنجاح")
                }
                .onFailure { err ->
                    _isLoading.value = false
                    val msg = err.message ?: "فشل إعادة تعيين كلمة المرور"
                    _errorMessage.value = msg
                    onResult(false, msg)
                }
        }
    }

    fun updateFcmToken(userId: String, phone: String, token: String, role: String = "CLIENT") {
        viewModelScope.launch {
            authRepository.updateFcmToken(userId, phone, token, role)
        }
    }

    fun logout() {
        _authState.value = UserAuthState()
    }

    fun setRole(role: String) {
        _authState.value = _authState.value.copy(userRole = role)
    }

    override fun onCleared() {
        super.onCleared()
        authRepository.clearListeners()
        userRepository.clearListeners()
    }
}
