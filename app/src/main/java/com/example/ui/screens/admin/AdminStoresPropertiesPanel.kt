package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.admin.components.AdminEntityCard
import com.example.ui.screens.admin.components.AdminFilterChips
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 🏪 Admin Panel: Stores & Properties Management (إدارة المتاجر والعقارات)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStoresPropertiesPanel(
    onBack: () -> Unit = {},
    viewModel: MainViewModel = viewModel(),
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()

    var selectedSection by remember { mutableStateOf("المتاجر") }
    val sections = listOf("المتاجر", "العقارات")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🏪 إدارة المتاجر والعقارات", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminFilterChips(
                categories = sections,
                selectedCategory = selectedSection,
                onSelectCategory = { selectedSection = it },
                themeColors = themeColors
            )

            if (selectedSection == "المتاجر") {
                if (stores.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد متاجر حالياً", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(stores, key = { it.id }) { store ->
                            AdminEntityCard(
                                title = store.name,
                                subtitle = "📱 ${store.phone} • 📍 ${store.cityId} - ${store.localNeighborhood}",
                                details = "ساعات العمل: ${store.workingHours}",
                                statusText = if (store.isBlocked) "محظور" else "نشط",
                                statusColor = if (store.isBlocked) Color(0xFFEF5350) else Color(0xFF10B981),
                                isBlocked = store.isBlocked,
                                themeColors = themeColors,
                                actions = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.toggleStoreBlocked(store.id, !store.isBlocked)
                                                scope.launch { snackbarHostState.showSnackbar(if (store.isBlocked) "تم إلغاء حظر المتجر" else "تم حظر المتجر") }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (store.isBlocked) Color(0xFF10B981) else Color(0xFFEF5350)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (store.isBlocked) "فك الحظر" else "حظر 🚫", fontSize = 10.5.sp, color = if (store.isBlocked) Color(0xFF10B981) else Color(0xFFEF5350))
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteStore(store.id)
                                                scope.launch { snackbarHostState.showSnackbar("🗑️ تم حذف المتجر") }
                                            },
                                            modifier = Modifier.background(Color(0xFFEF5350).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                if (properties.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد عقارات حالياً", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(properties, key = { it.id }) { prop ->
                            AdminEntityCard(
                                title = prop.title,
                                subtitle = "📱 ${prop.phone} • 📍 ${prop.cityId} - ${prop.localNeighborhood}",
                                details = "السعر: ${prop.price} ${prop.currency} • النوع: ${prop.type}",
                                statusText = if (prop.isBlocked) "محظور" else "متاح",
                                statusColor = if (prop.isBlocked) Color(0xFFEF5350) else Color(0xFF10B981),
                                isBlocked = prop.isBlocked,
                                themeColors = themeColors,
                                actions = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.togglePropertyBlocked(prop.id, !prop.isBlocked)
                                                scope.launch { snackbarHostState.showSnackbar(if (prop.isBlocked) "تم إلغاء حظر العقار" else "تم حظر العقار") }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (prop.isBlocked) Color(0xFF10B981) else Color(0xFFEF5350)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (prop.isBlocked) "فك الحظر" else "حظر 🚫", fontSize = 10.5.sp, color = if (prop.isBlocked) Color(0xFF10B981) else Color(0xFFEF5350))
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteProperty(prop.id)
                                                scope.launch { snackbarHostState.showSnackbar("🗑️ تم حذف العقار") }
                                            },
                                            modifier = Modifier.background(Color(0xFFEF5350).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
