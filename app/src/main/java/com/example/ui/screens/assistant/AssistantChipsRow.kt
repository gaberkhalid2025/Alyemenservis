package com.example.ui.screens.assistant
import com.example.ui.MainViewModel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.utils.VisualThemePalette

/**
 * 💡 AssistantChipsRow - Quick action and navigation chips row in Assistant Dialog
 */
@Composable
fun AssistantChipsRow(
    viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    themeColors: VisualThemePalette,
    onRequestQuickService: () -> Unit,
    onNavigateToMap: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = themeColors.surface.copy(alpha = 0.8f),
        modifier = modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                AssistChip(
                    onClick = onRequestQuickService,
                    label = { Text("⚡ اطلب خدمتك الآن", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFEF4444)),
                    border = null,
                    shape = RoundedCornerShape(8.dp)
                )
            }
            item {
                AssistChip(
                    onClick = onRequestQuickService,
                    label = { Text("🔧 طلب أقرب فني", fontSize = 10.5.sp, color = themeColors.accent, fontWeight = FontWeight.Bold) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.surface),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                )
            }
            item {
                AssistChip(
                    onClick = {
                        viewModel.navigateTo("STORES_VIEW")
                        onDismiss()
                    },
                    label = { Text("🏬 المتاجر والمراكز", fontSize = 10.5.sp, color = themeColors.textPrimary) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(8.dp)
                )
            }
            item {
                AssistChip(
                    onClick = {
                        viewModel.navigateTo("RESTAURANTS_VIEW")
                        onDismiss()
                    },
                    label = { Text("🍽️ المطاعم والكافيهات", fontSize = 10.5.sp, color = themeColors.textPrimary) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(8.dp)
                )
            }
            item {
                AssistChip(
                    onClick = {
                        viewModel.navigateTo("MEDICAL_VIEW")
                        onDismiss()
                    },
                    label = { Text("🏥 المراكز الطبية", fontSize = 10.5.sp, color = themeColors.textPrimary) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(8.dp)
                )
            }
            item {
                AssistChip(
                    onClick = onNavigateToMap,
                    label = { Text("📍 خريطة الخدمات", fontSize = 10.5.sp, color = themeColors.textPrimary) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}
