package com.example.ui.components

import androidx.compose.runtime.*
import com.example.data.PropertyEntity
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun PropertyListItemCard(
    property: PropertyEntity,
    themeColors: VisualThemePalette,
    onClick: () -> Unit,
    viewModel: MainViewModel,
    onChatClick: (() -> Unit)? = null
) {
    com.example.PropertyListItemCard(
        prop = property,
        themeColors = themeColors,
        onClick = onClick,
        viewModel = viewModel
    )
}
