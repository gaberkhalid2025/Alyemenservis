package com.example.ui.components

import androidx.compose.runtime.*
import com.example.ui.*
import com.example.data.PropertyEntity
import com.example.ui.MainViewModel
import com.example.PropertyListItemCard
import com.example.utils.VisualThemePalette

@Composable
fun PropertyListItemCard(
    property: PropertyEntity,
    themeColors: VisualThemePalette,
    onClick: () -> Unit,
    viewModel: MainViewModel? = null,
    onChatClick: (() -> Unit)? = null
) {
    PropertyListItemCard(
        property = property,
        themeColors = themeColors,
        onClick = onClick,
        viewModel = viewModel,
        onChatClick = onChatClick
    )
}
