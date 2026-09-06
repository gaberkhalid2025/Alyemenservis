package com.example.ui.dialogs

import androidx.compose.runtime.Composable
import com.example.data.PropertyEntity
import com.example.ui.MainViewModel
import com.example.PropertyCreateEditDialog
import com.example.utils.VisualThemePalette

@Composable
fun PropertyCreateEditDialog(
    property: PropertyEntity? = null,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    PropertyCreateEditDialog(
        property = property,
        viewModel = viewModel,
        themeColors = themeColors,
        onDismiss = onDismiss,
        onSaved = onSaved
    )
}
