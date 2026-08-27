package com.example.ui.screens.admin

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
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 📑 Admin Panel: Custom Profile Tabs Manager (إدارة التبويبات المخصصة)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomTabsPanel(
    onBack: () -> Unit = {},
    viewModel: MainViewModel = viewModel(),
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val customTabs by viewModel.customProfileTabs.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("📑 إدارة التبويبات المخصصة", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) },
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
        if (customTabs.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("📑 لا توجد تبويبات مخصصة معرفة حالياً", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(customTabs, key = { it.id }) { tab ->
                    AdminEntityCard(
                        title = "${tab.icon} ${tab.title}",
                        subtitle = "🎯 الهدف: ${tab.targetType} • الترتيب: ${tab.displayOrder}",
                        statusText = if (tab.isEnabled) "مفعل" else "معطل",
                        statusColor = if (tab.isEnabled) Color(0xFF10B981) else Color(0xFFEF5350),
                        themeColors = themeColors,
                        actions = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("المعرف: ${tab.id}", fontSize = 11.sp, color = Color.Gray)
                                Switch(
                                    checked = tab.isEnabled,
                                    onCheckedChange = { isEnabled ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("تم تحديث حالة التبويب")
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = themeColors.accent,
                                        checkedThumbColor = Color.Black
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
