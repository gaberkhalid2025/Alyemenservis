package com.example.ui.dialogs

import androidx.compose.runtime.Composable
import com.example.ui.*
import com.example.data.PropertyEntity
import com.example.ui.MainViewModel
import com.example.PropertyDetailsDialog
import com.example.utils.VisualThemePalette

@Composable
fun PropertyDetailsDialog(
    property: PropertyEntity,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    PropertyDetailsDialog(
        property = property,
        viewModel = viewModel,
        themeColors = themeColors,
        onDismiss = onDismiss
    )
}
