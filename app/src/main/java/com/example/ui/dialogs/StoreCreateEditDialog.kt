package com.example.ui.dialogs

import androidx.compose.runtime.Composable
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.StoreCreateEditDialog
import com.example.utils.VisualThemePalette

@Composable
fun StoreCreateEditDialog(
    store: StoreEntity? = null,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    StoreCreateEditDialog(
        store = store,
        viewModel = viewModel,
        themeColors = themeColors,
        onDismiss = onDismiss,
        onSaved = onSaved
    )
}
