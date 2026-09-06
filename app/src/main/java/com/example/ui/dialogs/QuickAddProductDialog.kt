package com.example.ui.dialogs

import androidx.compose.runtime.Composable
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.QuickAddProductDialog
import com.example.utils.VisualThemePalette

@Composable
fun QuickAddProductDialog(
    store: StoreEntity,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    QuickAddProductDialog(
        store = store,
        viewModel = viewModel,
        themeColors = themeColors,
        onDismiss = onDismiss
    )
}
