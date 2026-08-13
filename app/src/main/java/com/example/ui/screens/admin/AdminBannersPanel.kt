package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager
import com.example.util.UserRole

@Composable
fun AdminBannersPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    val currentRole = RoleManager.fromRoleString(viewModel.adminRole.value)
    if (!PermissionGuard.hasPermission(currentRole, "MANAGE_BANNERS")) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("🔒 ليس لديك صلاحية للوصول إلى هذه اللوحة", color = Color.White, fontSize = 14.sp)
                Text("يرجى التواصل مع المالك أو المدير الرئيسي", color = Color.Gray, fontSize = 12.sp)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("📢 البنرات الترويجية والتوجيه الإعلاني", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Text("إدارة ومراقبة لافتات الدعاية وبنرات التوجيه في واجهة التطبيق الرئيسية.", color = Color.LightGray, fontSize = 11.sp)
        
        val bannersList by viewModel.banners.collectAsState()
        val categories by viewModel.categories.collectAsState()
        
        var newBannerTitle by remember { mutableStateOf("") }
        var newBannerUrl by remember { mutableStateOf("") }
        var newBannerDuration by remember { mutableStateOf("5") }
        var newBannerSize by remember { mutableStateOf("MEDIUM") }
        var selectedRedirectCategoryId by remember { mutableStateOf("") }
        var selectedTargetSections by remember { mutableStateOf(setOf("ALL")) }
        
        // Add Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("➕ إضافة بنر إعلاني جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                
                OutlinedTextField(
                    value = newBannerTitle,
                    onValueChange = { newBannerTitle = it },
                    label = { Text("عنوان الإعلان / النص الدعائي", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White)
                )
                
                OutlinedTextField(
                    value = newBannerUrl,
                    onValueChange = { newBannerUrl = it },
                    label = { Text("رابط الصورة أو الموقع الإلكتروني (URL)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newBannerDuration,
                        onValueChange = { newBannerDuration = it },
                        label = { Text("مدة العرض (ثانية)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White)
                    )
                    
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("حجم البنر:", fontSize = 10.sp, color = Color.LightGray)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val sizes = listOf("SMALL" to "صغير", "MEDIUM" to "وسط", "LARGE" to "كبير")
                            sizes.forEach { (szKey, szLbl) ->
                                val isSel = newBannerSize == szKey
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSel) themeColors.accent else Color.White.copy(alpha = 0.1f))
                                        .clickable { newBannerSize = szKey }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(szLbl, fontSize = 9.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                // Category Selection
                Text("🎯 توجيه البنر عند الضغط (اختر قسم التوجيه):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(6.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedRedirectCategoryId == cat.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) themeColors.accent.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { selectedRedirectCategoryId = cat.id }
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedRedirectCategoryId = cat.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = themeColors.accent),
                                    modifier = Modifier.scale(0.8f)
                                )
                                Text("${cat.icon} ${cat.name}", fontSize = 11.sp, color = if (isSelected) themeColors.accent else Color.White)
                            }
                        }
                    }
                }

                // Targeted Sections Checklist
                Text("📍 واجهات عرض البنر (اختر الواجهات المستهدفة):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                val availableSections = listOf(
                    "ALL" to "الكل (الرئيسية وكل الأقسام)",
                    "HOME" to "صفحة البداية والترحيب",
                    "STORES" to "قسم المحلات والأسواق 🏪",
                    "RESTAURANTS" to "قسم المطاعم والكافيهات 🍔",
                    "MEDICAL" to "قسم المراكز الطبية 🏥",
                    "PROPERTIES" to "قسم العقارات والأراضي 🏠",
                    "JOBS" to "قسم الوظائف وفرص العمل 💼"
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableSections.forEach { (secKey, secName) ->
                        val isChecked = selectedTargetSections.contains(secKey)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isChecked) themeColors.accent.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable {
                                    selectedTargetSections = if (secKey == "ALL") {
                                        if (isChecked) emptySet() else setOf("ALL")
                                    } else {
                                        val next = selectedTargetSections - "ALL"
                                        if (isChecked) next - secKey else next + secKey
                                    }
                                }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    selectedTargetSections = if (secKey == "ALL") {
                                        if (isChecked) emptySet() else setOf("ALL")
                                    } else {
                                        val next = selectedTargetSections - "ALL"
                                        if (isChecked) next - secKey else next + secKey
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = themeColors.accent),
                                modifier = Modifier.scale(0.8f)
                            )
                            Text(secName, fontSize = 11.sp, color = if (isChecked) themeColors.accent else Color.White)
                        }
                    }
                }
                
                Button(
                    onClick = {
                        if (newBannerTitle.isNotEmpty()) {
                            viewModel.addNewBanner(
                                title = newBannerTitle.trim(),
                                url = newBannerUrl.trim(),
                                redirect = selectedRedirectCategoryId,
                                type = "IMAGE",
                                size = newBannerSize,
                                duration = newBannerDuration.toIntOrNull() ?: 5,
                                targetSection = if (selectedTargetSections.isEmpty()) "ALL" else selectedTargetSections.joinToString(",")
                            )
                            newBannerTitle = ""
                            newBannerUrl = ""
                            selectedRedirectCategoryId = ""
                            selectedTargetSections = setOf("ALL")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("حفظ وإضافة البنر الإعلاني 💾", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Banners List
        Text("📋 الإعلانات والبنرات الحالية النشطة (${bannersList.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        
        if (bannersList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد إعلانات نشطة حالياً", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            bannersList.forEach { b ->
                val matchedCat = categories.find { it.id == b.redirectCategory }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(b.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("رابط التوجيه: ${if (matchedCat != null) "${matchedCat.icon} ${matchedCat.name}" else "الرئيسية / غير محدد"}", fontSize = 10.sp, color = themeColors.accent)
                            Text("مدة العرض: ${b.duration} ثانية - الحجم: ${b.size}", fontSize = 9.sp, color = Color.LightGray)
                        }
                        
                        IconButton(onClick = { viewModel.deleteBanner(b.id) }) {
                            Text("🗑️", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
