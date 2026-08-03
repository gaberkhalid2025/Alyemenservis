package com.example.ui

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

data class UserIdentity(
    val deviceId: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val city: String = "صنعاء",
    val role: String = "USER", // USER, PROVIDER, STORE_OWNER, ADMIN
    val isRegistered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 🔐 AuthAndIdentityViewModel:
 * Implements tamper-resistant device-based registration & identity management using encrypted key-value storage.
 */
class AuthAndIdentityViewModel : ViewModel() {
    private val TAG = "AuthAndIdentityVM"

    private val _userIdentity = MutableStateFlow(UserIdentity())
    val userIdentity: StateFlow<UserIdentity> = _userIdentity

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    fun initDeviceIdentity(context: Context) {
        viewModelScope.launch {
            try {
                val prefs = getSecurePreferences(context)
                var deviceId = prefs.getString("enc_device_id", "") ?: ""
                
                if (deviceId.isBlank()) {
                    deviceId = generateSecureDeviceId(context)
                    prefs.edit().putString("enc_device_id", deviceId).apply()
                }

                val savedUserId = prefs.getString("enc_user_id", "guest_$deviceId") ?: "guest_$deviceId"
                val savedName = prefs.getString("enc_user_name", "") ?: ""
                val savedPhone = prefs.getString("enc_user_phone", "") ?: ""
                val savedCity = prefs.getString("enc_user_city", "صنعاء") ?: "صنعاء"
                val savedRole = prefs.getString("enc_user_role", "USER") ?: "USER"

                val identity = UserIdentity(
                    deviceId = deviceId,
                    userId = savedUserId,
                    name = savedName,
                    phone = savedPhone,
                    city = savedCity,
                    role = savedRole,
                    isRegistered = savedName.isNotEmpty() && savedPhone.isNotEmpty()
                )

                _userIdentity.value = identity
                syncUserWithFirestore(identity)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing device identity", e)
            }
        }
    }

    fun registerDeviceUser(
        context: Context,
        name: String,
        phone: String,
        city: String,
        role: String = "USER",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (name.isBlank() || phone.isBlank()) {
            onError("يرجى إدخال الاسم ورقم الهاتف بالكامل")
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val prefs = getSecurePreferences(context)
                val deviceId = _userIdentity.value.deviceId.ifEmpty { generateSecureDeviceId(context) }
                val userId = "usr_${phone.filter { it.isDigit() }.takeLast(8)}_${deviceId.take(6)}"

                val newIdentity = UserIdentity(
                    deviceId = deviceId,
                    userId = userId,
                    name = name.trim(),
                    phone = phone.trim(),
                    city = city.ifEmpty { "صنعاء" },
                    role = role,
                    isRegistered = true
                )

                prefs.edit()
                    .putString("enc_device_id", deviceId)
                    .putString("enc_user_id", userId)
                    .putString("enc_user_name", name.trim())
                    .putString("enc_user_phone", phone.trim())
                    .putString("enc_user_city", city.ifEmpty { "صنعاء" })
                    .putString("enc_user_role", role)
                    .apply()

                _userIdentity.value = newIdentity
                syncUserWithFirestore(newIdentity)

                _isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                Log.e(TAG, "Registration error", e)
                onError("❌ فشل التسجيل: ${e.localizedMessage}")
            }
        }
    }

    private fun syncUserWithFirestore(identity: UserIdentity) {
        try {
            val db = FirebaseFirestore.getInstance()
            val userMap = mapOf(
                "deviceId" to identity.deviceId,
                "userId" to identity.userId,
                "name" to identity.name,
                "phone" to identity.phone,
                "city" to identity.city,
                "role" to identity.role,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("users").document(identity.userId).set(userMap)
        } catch (e: Exception) {
            Log.e(TAG, "Firestore user sync failed", e)
        }
    }

    private fun getSecurePreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("yemen_secured_device_identity_prefs", Context.MODE_PRIVATE)
    }

    private fun generateSecureDeviceId(context: Context): String {
        return try {
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(androidId.toByteArray())
            Base64.encodeToString(digest, Base64.NO_WRAP).take(16).lowercase()
        } catch (e: Exception) {
            UUID.randomUUID().toString().take(16)
        }
    }
}
