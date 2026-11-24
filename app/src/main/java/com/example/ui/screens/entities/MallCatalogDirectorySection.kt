package com.example.ui.screens.entities

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StoreEntity
import com.example.data.repositories.MallRepository
import com.example.data.repositories.MallTenantItem
import com.example.utils.VisualThemePalette

/**
 * 🏬 MallCatalogDirectorySection
 * يعرض طوابق المركز التجاري ودليل المحلات المستأجرة داخل كل دور للزائر.
 */
@Composable
fun MallCatalogDirectorySection(
    store: StoreEntity,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var floorsCount by remember(store.id) { mutableIntStateOf(3) }
    var tenants by remember(store.id) { mutableStateOf<List<MallTenantItem>>(emptyList()) }
    var selectedFloor by remember(store.id) { mutableIntStateOf(-1) }
    var loaded by remember(store.id) { mutableStateOf(false) }

    LaunchedEffect(store.id) {
        val mall = MallRepository.getOrResolveMall(store)
        floorsCount = mall.floorsCount.coerceIn(1, 20)
        tenants = MallRepository.parseTenantsJson(mall.floorsDirectoryJson)
        loaded = true
    }

    if (!loaded) return

    val visibleTenants = remember(tenants, selectedFloor) {
        if (selectedFloor < 0) tenants else tenants.filter { it.floorNumber == selectedFloor }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, themeColors.border)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏬 دليل الطوابق والمحلات ($floorsCount طوابق)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textPrimary
                )
                if (tenants.isNotEmpty()) {
                    Text(
                        text = "${tenants.size} محل",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedFloor == -1,
                        onClick = { selectedFloor = -1 },
                        label = { Text("الكل", fontSize = 11.sp) }
                    )
                }
                items((1..floorsCount).toList()) { floor ->
                    FilterChip(
                        selected = selectedFloor == floor,
                        onClick = { selectedFloor = floor },
                        label = { Text(MallRepository.defaultFloorLabel(floor), fontSize = 11.sp) }
                    )
                }
            }

            if (visibleTenants.isEmpty()) {
                Text(
                    text = "لم تُضف محلات في هذا الدور بعد — تواصل مع إدارة المركز التجاري للاستفسار.",
                    fontSize = 11.5.sp,
                    color = themeColors.textSecondary
                )
            } else {
                visibleTenants.forEach { item ->
                    Surface(
                        color = themeColors.background,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, themeColors.border.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(item.shopName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                                Text(
                                    text = "${item.floorLabel.ifBlank { MallRepository.defaultFloorLabel(item.floorNumber) }}${if (item.unitCode.isNotBlank()) " • وحدة ${item.unitCode}" else ""}${if (item.category.isNotBlank()) " • ${item.category}" else ""}",
                                    fontSize = 11.sp,
                                    color = themeColors.textSecondary
                                )
                            }
                            if (item.phone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.phone}")))
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("اتصال 📞", fontSize = 11.sp, color = Color(0xFF10B981))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
