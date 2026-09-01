package com.example.ui

import androidx.compose.runtime.Composable
import com.example.data.ProviderEntity
import com.example.ui.components.ProviderCard as ComponentProviderCard
import com.example.utils.VisualThemePalette

@Composable
fun ProviderCard(
    provider: ProviderEntity,
    themeColors: VisualThemePalette,
    viewModel: MainViewModel,
    onChatOpen: (String) -> Unit
) {
    ComponentProviderCard(
        provider = provider,
        themeColors = themeColors,
        viewModel = viewModel,
        onChatOpen = onChatOpen
    )
}
