package com.example.ui.screens.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MapLandmarkFilter(
    val labelAr: String,
    val icon: String,
    val description: String
) {
    STREETS("الشوارع والطرق", "🛣️", "أسماء الشوارع الرئيسية والفرعية"),
    MOSQUES("المساجد والجوامع", "🕌", "الجوامع ومصليات الأحياء"),
    SCHOOLS("المدارس والجامعات", "🏫", "المدارس والكليات والمعاهد"),
    HALLS("قاعات وصالات الأفراح", "💒", "صالات المناسبات والأعراس"),
    RESTAURANTS("المطاعم والكافيهات", "🍔", "المطاعم والاستراحات والبوفيات"),
    STORES("المحلات والمولات", "🏪", "المراكز التجارية والأسواق"),
    MEDICAL("المراكز الطبية", "🏥", "المستشفيات والعيادات والصيدليات"),
    PROVIDERS("الفنيون والخدمات", "👷", "الفنيون المعتمدون والعقارات")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MapLandmarkFilterSheet(
    showLabelsOnMap: Boolean,
    onToggleLabelsOnMap: (Boolean) -> Unit,
    visibleFilters: Set<MapLandmarkFilter>,
    onToggleFilter: (MapLandmarkFilter) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
        modifier = Modifier.testTag("map_landmark_filter_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "معالم وأسماء الخريطة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "اختر ما يظهر على الخريطة لتجنب الازدحام",
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = "تم ✓",
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // 1. One-tap Main Switch: Hide/Show All Names & Labels
            Surface(
                onClick = { onToggleLabelsOnMap(!showLabelsOnMap) },
                shape = RoundedCornerShape(16.dp),
                color = if (showLabelsOnMap) Color(0xFF10B981).copy(alpha = 0.16f) else Color(0xFF1E293B).copy(alpha = 0.6f),
                border = BorderStroke(
                    1.2.dp,
                    if (showLabelsOnMap) Color(0xFF10B981) else Color(0xFF475569)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_toggle_all_labels_switch_card")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (showLabelsOnMap) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = if (showLabelsOnMap) Color(0xFF10B981) else Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (showLabelsOnMap) "أسماء الشوارع والمعالم (ظاهرة ✓)" else "إخفاء أسماء الشوارع والمعالم (خريطة هادئة)",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (showLabelsOnMap) "انقر لإخفاء النصوص لتخفيف الازدحام" else "انقر لإظهار أسماء الشوارع والمباني",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = showLabelsOnMap,
                        onCheckedChange = onToggleLabelsOnMap,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF1E293B)
                        )
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // 2. Category selection controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "تخصيص المعالم المعروضة:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = onSelectAll,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("الكل", color = Color(0xFF10B981), fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = onClearAll,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("مسح", color = Color(0xFFEF4444), fontSize = 11.sp)
                    }
                }
            }

            // Chips Grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapLandmarkFilter.entries.forEach { filter ->
                    val isSelected = visibleFilters.contains(filter)
                    Surface(
                        onClick = { onToggleFilter(filter) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.20f) else Color(0xFF1E293B).copy(alpha = 0.65f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF10B981) else Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("landmark_chip_${filter.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(filter.icon, fontSize = 14.sp)
                            Text(
                                text = filter.labelAr,
                                color = if (isSelected) Color(0xFF10B981) else Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
