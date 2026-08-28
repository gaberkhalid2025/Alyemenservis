@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.about

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MainViewModel
import com.example.ui.screens.about.components.AboutContentRenderer
import com.example.ui.screens.about.components.AboutLayoutEditor
import com.example.utils.VisualThemePalette

/**
 * AboutAppDialogView is a full-screen overlay dialog providing general platform information,
 * support links, versioning, and administrative layout configuration controls.
 *
 * @param viewModel The shared global MainViewModel.
 * @param themeColors The palette theme colors.
 * @param onDismiss Invoked when the user requests to close the dialog.
 */
@Composable
fun AboutAppDialogView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = themeColors.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColors.primary)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "معلومات عن التطبيق",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    AboutAppScreenContent(viewModel = viewModel, themeColors = themeColors)
                }
            }
        }
    }
}

/**
 * Main content layout for About screen, supporting both fullscreen and dialog presentations.
 */
@Composable
fun AboutAppScreenContent(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val aboutViewModel = remember(viewModel) { AboutViewModel(viewModel) }
    val uiState by aboutViewModel.uiState.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val context = LocalContext.current

    when (val state = uiState) {
        is AboutUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = themeColors.primary)
            }
        }
        is AboutUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⚠️ حدث خطأ:", color = Color.Red, fontWeight = FontWeight.Bold)
                    Text(state.message, color = Color.White, textAlign = TextAlign.Center)
                }
            }
        }
        is AboutUiState.Success, is AboutUiState.Editing -> {
            val settings = when (val s = state) {
                is AboutUiState.Success -> s.settings
                is AboutUiState.Editing -> s.settings
                else -> throw IllegalStateException()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Render Edit Panel for Admins
                if (adminRole != "GUEST") {
                    AboutLayoutEditor(viewModel = aboutViewModel, themeColors = themeColors)
                }

                // Render Dynamic Ordered Content List
                AboutContentRenderer(settings = settings, themeColors = themeColors, context = context)
            }
        }
    }
}
