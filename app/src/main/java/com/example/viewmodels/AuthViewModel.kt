package com.example.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
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
 * إدارة تسجيل الدخول، إنشاء الحساب، الصلاحيات وحالة جلسة المستخدم الحالية.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _authState = MutableStateFlow(UserAuthState())
    val authState: StateFlow<UserAuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun login(phone: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val cleanPhone = phone.trim()
            try {
                firestore.collection("users").document(cleanPhone).get()
                    .addOnSuccessListener { doc ->
                        _isLoading.value = false
                        if (doc.exists()) {
                            val isBlocked = doc.getBoolean("isBlocked") ?: false
                            if (isBlocked) {
                                _errorMessage.value = "هذا الحساب محظور حالياً، يرجى التواصل مع الدعم."
                                onResult(false, "حساب محظور")
                                return@addOnSuccessListener
                            }
                            val role = doc.getString("role") ?: "CLIENT"
                            val name = doc.getString("name") ?: "مستخدم"
                            _authState.value = UserAuthState(
                                userId = cleanPhone,
                                userName = name,
                                userPhone = cleanPhone,
                                userRole = role,
                                isAuthenticated = true
                            )
                            onResult(true, "تم تسجيل الدخول بنجاح")
                        } else {
                            // الدخول كزائر مسجل
                            _authState.value = UserAuthState(
                                userId = cleanPhone,
                                userName = "مستخدم",
                                userPhone = cleanPhone,
                                userRole = "CLIENT",
                                isAuthenticated = true
                            )
                            onResult(true, "مرحباً بك")
                        }
                    }
                    .addOnFailureListener { e ->
                        _isLoading.value = false
                        _errorMessage.value = e.message
                        onResult(false, e.message ?: "خطأ في الاتصال")
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                onResult(false, e.message ?: "خطأ غير متوقع")
            }
        }
    }

    fun logout() {
        _authState.value = UserAuthState()
    }

    fun setRole(role: String) {
        _authState.value = _authState.value.copy(userRole = role)
    }
}
