package com.example.ui.components

import androidx.compose.runtime.*
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.StoreListItemCard
import com.example.utils.VisualThemePalette

@Composable
fun StoreListItemCard(
    store: StoreEntity,
    themeColors: VisualThemePalette,
    onClick: () -> Unit,
    viewModel: MainViewModel? = null,
    onChatClick: (() -> Unit)? = null
) {
    StoreListItemCard(
        store = store,
        themeColors = themeColors,
        onClick = onClick,
        viewModel = viewModel,
        onChatClick = onChatClick
    )
}
