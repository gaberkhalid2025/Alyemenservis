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

data class BannerItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val actionUrl: String,
    val isActive: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBannersPanel(
    onBack: () -> Unit = {},
    adminViewModel: AdminViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var actionUrl by remember { mutableStateOf("") }

    var bannersList by remember {
        mutableStateOf(
            listOf(
                BannerItem("BAN-1", "خصومات خاصة على صيانة منظومات الطاقة الشمسية ☀️", "خدمة سريعة وضمان معتمد", "service://solar"),
                BannerItem("BAN-2", "انضم الآن كفني أو متجر معتمد في كل خدمات اليمن 🇾🇪", "سجل الآن وابدأ باستقبال الطلبات", "service://join"),
                BannerItem("BAN-3", "عروض المتاجر وقطع الغيار الأصلية 🛍️", "توصيل سريع لكافة المحافظات", "service://stores")
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة الإعلانات والبنرات", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة بنر", tint = Color(0xFF00668B))
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
            items(bannersList, key = { it.id }) { banner ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(banner.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(banner.subtitle, fontSize = 12.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = banner.isActive,
                                onCheckedChange = { active ->
                                    bannersList = bannersList.map { if (it.id == banner.id) it.copy(isActive = active) else it }
                                    Toast.makeText(context, if (active) "تم تفعيل البنر" else "تم إيقاف البنر", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("الرابط: ${banner.actionUrl}", fontSize = 11.sp, color = Color(0xFF00668B))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة بنر إعلاني جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان البنر") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("الوصف الفرعي") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = actionUrl, onValueChange = { actionUrl = it }, label = { Text("رابط التوجيه") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        bannersList = listOf(BannerItem("BAN-${System.currentTimeMillis() % 1000}", title, subtitle, actionUrl)) + bannersList
                        showAddDialog = false
                        title = ""; subtitle = ""; actionUrl = ""
                        Toast.makeText(context, "تمت إضافة البنر بنجاح", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("إضافة") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") } }
        )
    }
}
