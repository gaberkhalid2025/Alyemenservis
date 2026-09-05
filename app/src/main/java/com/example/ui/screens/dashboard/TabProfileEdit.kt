package com.example.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.screens.dashboard.components.UnifiedProfileSection
import com.example.ui.screens.dashboard.components.UnifiedSettingsSection
import com.example.utils.VisualThemePalette

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
    onSaveProfile: (name: String, phone: String, cityArea: String, description: String, workingHours: String, isAvailable: Boolean) -> Unit,
    onChangePassword: (oldPass: String, newPass: String) -> Unit
) {
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
            photoUrl = photoUrl,
            coverUrl = coverUrl,
            rating = rating,
            reviewCount = reviewCount,
            isAvailable = isAvailable,
            themeColors = themeColors
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
