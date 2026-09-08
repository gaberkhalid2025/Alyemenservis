package com.example.ui.components

import androidx.compose.runtime.Composable
import com.example.ui.*
import com.example.data.EntityType
import com.example.data.StoreEntity
import com.example.data.PropertyEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.entities.*
import com.example.utils.VisualThemePalette

@Composable
fun EntityTabContent(
    entityType: EntityType,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onEntityClick: ((Any) -> Unit)? = null,
    onChatClick: (() -> Unit)? = null,
    onRequestActionClick: (() -> Unit)? = null
) {
    when (entityType) {
        EntityType.STORE -> {
            StoresScreen(
                viewModel = viewModel,
                themeColors = themeColors,
                onStoreClick = { store ->
                    onEntityClick?.invoke(store)
                },
                onChatClick = {
                    if (onChatClick != null) onChatClick() else viewModel.openSupportChat()
                },
                onRequestServiceClick = {
                    onRequestActionClick?.invoke()
                }
            )
        }
        EntityType.PROPERTY -> {
            PropertiesScreen(
                viewModel = viewModel,
                themeColors = themeColors,
                onPropertyClick = { prop ->
                    onEntityClick?.invoke(prop)
                },
                onChatClick = {
                    if (onChatClick != null) onChatClick() else viewModel.openSupportChat()
                },
                onRequestInspectionClick = {
                    onRequestActionClick?.invoke()
                }
            )
        }
        EntityType.RESTAURANT -> {
            RestaurantsScreen(
                viewModel = viewModel,
                themeColors = themeColors,
                onRestaurantClick = { res ->
                    onEntityClick?.invoke(res)
                },
                onChatClick = {
                    if (onChatClick != null) onChatClick() else viewModel.openSupportChat()
                },
                onOrderMealClick = {
                    onRequestActionClick?.invoke()
                }
            )
        }
        EntityType.MEDICAL -> {
            MedicalCentersScreen(
                viewModel = viewModel,
                themeColors = themeColors,
                onMedicalCenterClick = { med ->
                    onEntityClick?.invoke(med)
                },
                onChatClick = {
                    if (onChatClick != null) onChatClick() else viewModel.openSupportChat()
                },
                onBookAppointmentClick = {
                    onRequestActionClick?.invoke()
                }
            )
        }
        EntityType.JOB -> {
            StoresScreen(
                viewModel = viewModel,
                themeColors = themeColors,
                onStoreClick = { store -> onEntityClick?.invoke(store) },
                onChatClick = { if (onChatClick != null) onChatClick() else viewModel.openSupportChat() },
                onRequestServiceClick = { onRequestActionClick?.invoke() }
            )
        }
    }
}
