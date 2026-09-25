package com.example.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import com.example.ui.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.screens.dashboard.components.UnifiedProfileSection
import com.example.ui.screens.dashboard.components.UnifiedSettingsSection
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

@Composable
fun TabProfileEdit(
    name: String,
    phone: String,
    cityArea: String,
    description: String,
    workingHours: String = "",
    photoUrl: String = "",
    coverUrl: String = "",
    isAvailable: Boolean = true,
    rating: Double = 5.0,
    reviewCount: Int = 0,
    themeColors: VisualThemePalette,
    onChangePhoto: ((String) -> Unit)? = null,
    onChangeCover: ((String) -> Unit)? = null,
    onSaveProfile: (name: String, phone: String, cityArea: String, description: String, workingHours: String, isAvailable: Boolean) -> Unit,
    onChangePassword: (oldPass: String, newPass: String) -> Unit
) {
    var currentPhoto by remember(photoUrl) { mutableStateOf(photoUrl) }
    var currentCover by remember(coverUrl) { mutableStateOf(coverUrl) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val cleanPhone = remember(phone) {
        com.example.ui.helpers.AppPreferenceHelper.normalizePhoneNumber(phone)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        UnifiedProfileSection(
            title = name,
            subtitle = description,
            phone = phone,
            cityArea = cityArea,
            photoUrl = currentPhoto,
            coverUrl = currentCover,
            rating = rating,
            reviewCount = reviewCount,
            isAvailable = isAvailable,
            themeColors = themeColors,
            onChangePhoto = { newUrl ->
                currentPhoto = newUrl
                onChangePhoto?.invoke(newUrl)
                if (cleanPhone.isNotBlank()) {
                    coroutineScope.launch {
                        try {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val updates = mapOf(
                                "logoImage" to newUrl,
                                "profileImage" to newUrl,
                                "photoUrl" to newUrl
                            )
                            db.collection("users").document(cleanPhone).update(updates)
                            db.collection("providers").document(cleanPhone).update("logoImage", newUrl)
                            db.collection("stores").document(cleanPhone).update("logoImage", newUrl)
                        } catch (e: Exception) {
                            android.util.Log.e("TabProfileEdit", "Error updating photo in Firestore", e)
                        }
                    }
                }
            },
            onChangeCover = { newUrl ->
                currentCover = newUrl
                onChangeCover?.invoke(newUrl)
                if (cleanPhone.isNotBlank()) {
                    coroutineScope.launch {
                        try {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val updates = mapOf(
                                "coverImage" to newUrl
                            )
                            db.collection("users").document(cleanPhone).update(updates)
                            db.collection("providers").document(cleanPhone).update("coverImage", newUrl)
                            db.collection("stores").document(cleanPhone).update("coverImage", newUrl)
                        } catch (e: Exception) {
                            android.util.Log.e("TabProfileEdit", "Error updating cover in Firestore", e)
                        }
                    }
                }
            }
        )

        UnifiedSettingsSection(
            name = name,
            phone = phone,
            cityArea = cityArea,
            description = description,
            workingHours = workingHours,
            isAvailable = isAvailable,
            themeColors = themeColors,
            onSaveProfile = onSaveProfile,
            onChangePassword = onChangePassword
        )
    }
}
