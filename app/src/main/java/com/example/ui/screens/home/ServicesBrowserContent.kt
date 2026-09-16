package com.example.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.screens.home.sections.ProvidersSectionView
import com.example.utils.VisualThemePalette

/**
 * 📱 ServicesBrowserContent - المحتوى التفاعلي وعرض قوائم الفنيين ومقدمي الخدمات
 */
@Composable
fun ServicesBrowserMainContent(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    displayProviders: List<ProviderEntity>,
    displayStores: List<StoreEntity> = emptyList(),
    displayProperties: List<PropertyEntity> = emptyList(),
    isProvidersLoading: Boolean,
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    providersLimit: Int,
    onLoadMore: () -> Unit,
    onStoreClick: (StoreEntity) -> Unit,
    onPropertyClick: (PropertyEntity) -> Unit,
    onChatOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ProvidersSectionView(
        viewModel = viewModel,
        themeColors = themeColors,
        displayProviders = displayProviders,
        isProvidersLoading = isProvidersLoading,
        categories = categories,
        selectedCategoryId = selectedCategoryId,
        onCategorySelected = onCategorySelected,
        providersLimit = providersLimit,
        onLoadMore = onLoadMore,
        onChatOpen = onChatOpen,
        modifier = modifier
    )
}

