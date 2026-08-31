package com.example.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repositories.AuthRepository
import com.example.data.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
 * إدارة تسجيل الدخول، إنشاء الحساب، الصلاحيات وحالة جلسة المستخدم الحالية.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow(UserAuthState())
    val authState: StateFlow<UserAuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val triggerRestoreAccountDialog = MutableStateFlow(false)


    val currentUserId: StateFlow<String> = _authState.map { it.userId }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, "")
    val currentUserName: StateFlow<String> = _authState.map { it.userName }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, "")
    val currentUserPhone: StateFlow<String> = _authState.map { it.userPhone }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, "")
    val isOnline: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(true).asStateFlow()
    val isProviderUser: StateFlow<Boolean> = _authState.map { it.userRole == "PROVIDER" }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, false)


    fun login(phone: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val cleanPhone = phone.trim()
            
            val result = authRepository.loginWithPhone(cleanPhone, pass)
            _isLoading.value = false
            
            result.onSuccess { user ->
                if (user.isBlocked) {
                    _errorMessage.value = "هذا الحساب محظور حالياً، يرجى التواصل مع الدعم."
                    onResult(false, "حساب محظور")
                } else {
                    _authState.value = UserAuthState(
                        userId = user.id,
                        userName = user.name,
                        userPhone = user.phone,
                        userRole = user.role,
                        isAuthenticated = true
                    )
                    onResult(true, "تم تسجيل الدخول بنجاح")
                }
            }.onFailure { e ->
                // fallback for guest user login during transition phase
                if (e is NoSuchElementException) {
                    _authState.value = UserAuthState(
                        userId = cleanPhone,
                        userName = "مستخدم",
                        userPhone = cleanPhone,
                        userRole = "CLIENT",
                        isAuthenticated = true
                    )
                    onResult(true, "مرحباً بك كزائر")
                } else {
                    _errorMessage.value = e.message
                    onResult(false, e.message ?: "خطأ في الاتصال")
                }
            }
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
