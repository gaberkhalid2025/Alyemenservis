package com.example.ui.dialogs

import androidx.compose.runtime.Composable
import com.example.ui.*
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.BulkPriceAdjusterDialog
import com.example.utils.VisualThemePalette

@Composable
fun BulkPriceAdjusterDialog(
    store: StoreEntity,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    BulkPriceAdjusterDialog(
        store = store,
        viewModel = viewModel,
        themeColors = themeColors,
        onDismiss = onDismiss
    )
}
