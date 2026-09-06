package com.example.ui.dialogs

import androidx.compose.runtime.Composable
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.StoreDetailsDialog
import com.example.utils.VisualThemePalette

@Composable
fun StoreDetailsDialog(
    store: StoreEntity,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    StoreDetailsDialog(
        store = store,
        viewModel = viewModel,
        themeColors = themeColors,
        onDismiss = onDismiss
    )
}
