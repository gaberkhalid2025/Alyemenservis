package com.example.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserEntity
import com.example.util.SecurityCryptoUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
 * إدارة تسجيل الدخول، إنشاء الحساب، إعادة تعيين كلمة المرور، التحقق من الرموز وإدارة الجلسات.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _authState = MutableStateFlow(UserAuthState())
    val authState: StateFlow<UserAuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _verificationCode = MutableStateFlow<String?>(null)

    init {
        restoreSession()
    }

    private fun restoreSession() {
        try {
            val sp = getApplication<Application>().getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
            val rawPhone = sp.getString("user_phone", "") ?: ""
            val phone = if (rawPhone.isNotEmpty()) SecurityCryptoUtils.decrypt(rawPhone) else ""
            val rawName = sp.getString("user_name", "") ?: ""
            val name = if (rawName.isNotEmpty()) SecurityCryptoUtils.decrypt(rawName) else ""
            val role = sp.getString("user_role", "CLIENT") ?: "CLIENT"
            val isAuth = sp.getBoolean("is_authenticated", false)

            if (isAuth && phone.isNotEmpty()) {
                _authState.value = UserAuthState(
                    userId = phone,
                    userName = name.ifEmpty { "مستخدم" },
                    userPhone = phone,
                    userRole = role,
                    isAuthenticated = true
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun persistSession(phone: String, name: String, role: String) {
        try {
            val sp = getApplication<Application>().getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
            sp.edit()
                .putString("user_phone", SecurityCryptoUtils.encrypt(phone))
                .putString("user_name", SecurityCryptoUtils.encrypt(name))
                .putString("user_role", role)
                .putBoolean("is_authenticated", true)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun login(phone: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
            try {
                firestore.collection("registered_users").document(cleanPhone).get()
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
                            val storedPass = doc.getString("password") ?: ""

                            if (storedPass.isNotEmpty() && pass.isNotEmpty() && storedPass != pass) {
                                _errorMessage.value = "كلمة المرور غير صحيحة"
                                onResult(false, "كلمة المرور غير صحيحة")
                                return@addOnSuccessListener
                            }

                            _authState.value = UserAuthState(
                                userId = cleanPhone,
                                userName = name,
                                userPhone = cleanPhone,
                                userRole = role,
                                isAuthenticated = true
                            )
                            persistSession(cleanPhone, name, role)
                            onResult(true, "تم تسجيل الدخول بنجاح")
                        } else {
                            // الدخول كزائر / مستخدم سريع
                            val name = "مستخدم"
                            _authState.value = UserAuthState(
                                userId = cleanPhone,
                                userName = name,
                                userPhone = cleanPhone,
                                userRole = "CLIENT",
                                isAuthenticated = true
                            )
                            persistSession(cleanPhone, name, "CLIENT")
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

    fun register(name: String, phone: String, pass: String, role: String = "CLIENT", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
            val userMap = mapOf(
                "id" to cleanPhone,
                "name" to name.trim(),
                "phone" to cleanPhone,
                "password" to pass,
                "role" to role,
                "isBlocked" to false,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("registered_users").document(cleanPhone)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener {
                    _isLoading.value = false
                    _authState.value = UserAuthState(
                        userId = cleanPhone,
                        userName = name,
                        userPhone = cleanPhone,
                        userRole = role,
                        isAuthenticated = true
                    )
                    persistSession(cleanPhone, name, role)
                    onResult(true, "تم إنشاء الحساب بنجاح!")
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    _errorMessage.value = e.localizedMessage
                    onResult(false, e.localizedMessage ?: "فشل إنشاء الحساب")
                }
        }
    }

    /**
     * طلب إعادة تعيين كلمة المرور
     */
    fun resetPassword(phone: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
            
            // توليد رمز تحقق عشوائي من 6 أرقام
            val code = (100000..999999).random().toString()
            _verificationCode.value = code

            val resetData = mapOf(
                "phone" to cleanPhone,
                "code" to code,
                "timestamp" to System.currentTimeMillis(),
                "used" to false
            )

            firestore.collection("password_resets").document(cleanPhone)
                .set(resetData)
                .addOnSuccessListener {
                    _isLoading.value = false
                    onResult(true, "تم إرسال رمز التحقق إلى $cleanPhone: $code")
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    onResult(false, e.localizedMessage ?: "فشل طلب الاستعادة")
                }
        }
    }

    /**
     * التحقق من رمز الاستعادة
     */
    fun verifyResetCode(phone: String, code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
            
            firestore.collection("password_resets").document(cleanPhone).get()
                .addOnSuccessListener { doc ->
                    _isLoading.value = false
                    if (doc.exists()) {
                        val savedCode = doc.getString("code")
                        val isUsed = doc.getBoolean("used") ?: false
                        val timestamp = doc.getLong("timestamp") ?: 0L

                        val isExpired = System.currentTimeMillis() - timestamp > (15 * 60 * 1000) // 15 دقيقة

                        if (isUsed) {
                            onResult(false, "تم استخدام هذا الرمز من قبل")
                        } else if (isExpired) {
                            onResult(false, "انتهت صلاحية الرمز، يرجى طلب رمز جديد")
                        } else if (savedCode == code.trim()) {
                            onResult(true, "تم التحقق بنجاح")
                        } else {
                            onResult(false, "رمز التحقق غير صحيح")
                        }
                    } else {
                        onResult(false, "لم يتم العثور على طلب استعادة لهذا الرقم")
                    }
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    onResult(false, e.localizedMessage ?: "فشل التحقق")
                }
        }
    }

    /**
     * تغيير كلمة المرور بعد التحقق
     */
    fun changePassword(phone: String, newPass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
            
            firestore.collection("registered_users").document(cleanPhone)
                .update("password", newPass)
                .addOnSuccessListener {
                    _isLoading.value = false
                    firestore.collection("password_resets").document(cleanPhone).update("used", true)
                    onResult(true, "تم تغيير كلمة المرور بنجاح!")
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    onResult(false, e.localizedMessage ?: "فشل تحديث كلمة المرور")
                }
        }
    }

    fun logout() {
        _authState.value = UserAuthState()
        try {
            val sp = getApplication<Application>().getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
            sp.edit().clear().apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setRole(role: String) {
        _authState.value = _authState.value.copy(userRole = role)
    }
}
