package com.example.ui.screens.entities

import androidx.compose.runtime.Composable

import com.example.utils.VisualThemePalette
import com.example.data.StoreEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity

enum class GenericEntityType {
    STORE, RESTAURANT, PROPERTY, MEDICAL
}

/**
 * 🏢 GenericEntitiesScreen
 * شاشة عامة وموحدة لاستعراض الكيانات بمختلف أنواعها (متاجر، مطاعم، عقارات، مراكز طبية).
 * تعمل كطبقة تجريد (Abstraction Layer) لتقليل حجم تكرار الكود وتبسيط المسارات.
 */
@Composable
fun GenericEntitiesScreen(
    entityType: GenericEntityType,
    viewModel: AuthViewModel,
    themeColors: VisualThemePalette,
    // Store & Restaurant Callbacks
    onStoreClick: ((StoreEntity) -> Unit)? = null,
    onStoreChatClick: ((StoreEntity) -> Unit)? = null,
    onStoreActionClick: ((StoreEntity) -> Unit)? = null,
    // Property Callbacks
    onPropertyClick: ((PropertyEntity) -> Unit)? = null,
    onPropertyChatClick: ((PropertyEntity) -> Unit)? = null,
    onPropertyActionClick: ((PropertyEntity) -> Unit)? = null,
    // Medical/Provider Callbacks
    onMedicalClick: ((ProviderEntity) -> Unit)? = null,
    onMedicalChatClick: ((ProviderEntity) -> Unit)? = null,
    onMedicalActionClick: ((ProviderEntity) -> Unit)? = null
) {
    when (entityType) {
        GenericEntityType.STORE -> {
            StoresScreen(
                viewModel = viewModel,
                themeColors = themeColors,
                onStoreClick = { onStoreClick?.invoke(it) },
                onChatClick = { onStoreChatClick?.invoke(it) },
                onRequestServiceClick = { onStoreActionClick?.invoke(it) }
            )
        }
        GenericEntityType.RESTAURANT -> {
            RestaurantsScreen(
                viewModel = viewModel,
                themeColors = themeColors,
                onRestaurantClick = { onStoreClick?.invoke(it) },
                onChatClick = { onStoreChatClick?.invoke(it) },
                onOrderMealClick = { onStoreActionClick?.invoke(it) }
            )
        }
        GenericEntityType.PROPERTY -> {
            PropertiesScreen(
                viewModel = viewModel,
                themeColors = themeColors,
                onPropertyClick = { onPropertyClick?.invoke(it) },
                onChatClick = { onPropertyChatClick?.invoke(it) },
                onRequestInspectionClick = { onPropertyActionClick?.invoke(it) }
            )
        }
        GenericEntityType.MEDICAL -> {
            MedicalCentersScreen(
                viewModel = viewModel,
                themeColors = themeColors,
                onMedicalCenterClick = { onMedicalClick?.invoke(it) },
                onChatClick = { onMedicalChatClick?.invoke(it) },
                onBookAppointmentClick = { onMedicalActionClick?.invoke(it) }
            )
        }
    }
}
