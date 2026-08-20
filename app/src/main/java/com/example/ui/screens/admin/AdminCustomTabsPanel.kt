package com.example.ui.screens.admin

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodels.AdminViewModel

data class CustomTabItem(
    val id: String,
    val title: String,
    val route: String,
    val iconName: String,
    val isVisible: Boolean = true,
    val order: Int = 1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomTabsPanel(
    onBack: () -> Unit = {},
    adminViewModel: AdminViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tabsList by remember {
        mutableStateOf(
            listOf(
                CustomTabItem("TAB-1", "الرئيسية والخدمات", "home", "Home", true, 1),
                CustomTabItem("TAB-2", "اطلب خدمتك الآن", "urgent", "FlashOn", true, 2),
                CustomTabItem("TAB-3", "المحادثات والرسائل", "chats", "Chat", true, 3),
                CustomTabItem("TAB-4", "حجوزاتي وطلباتي", "bookings", "List", true, 4),
                CustomTabItem("TAB-5", "القطاع الطبي والصيدليات", "medical", "Favorite", true, 5),
                CustomTabItem("TAB-6", "المحفظة والمدفوعات", "wallet", "AccountBalanceWallet", true, 6)
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة التبويبات والأقسام", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF8FAFC)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tabsList, key = { it.id }) { tab ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(tab.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("المسار: ${tab.route} | الترتيب: ${tab.order}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = tab.isVisible,
                            onCheckedChange = { visible ->
                                tabsList = tabsList.map { if (it.id == tab.id) it.copy(isVisible = visible) else it }
                                Toast.makeText(context, if (visible) "تم تفعيل التبويب" else "تم إخفاء التبويب", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}
