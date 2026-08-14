package com.example.ui.screens.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.*
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager

@Composable
fun AdminPasswordsResetPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_USERS")) {
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

    val context = LocalContext.current
    val activatedProviders by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val jobs by viewModel.jobs.collectAsState()

    var adminPasswordSubTab by remember { mutableStateOf("SERVICES") }
    var targetUserPhone by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("🔑 لوحة تعيين وإعادة ضبط كلمات المرور لجميع الحسابات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Text("عرض الحسابات والمنشآت المسجلة وكلمات المرور الخاصة بهم مع إمكانية تعديلها وإرسالها فوراً:", fontSize = 11.sp, color = themeColors.textSecondary)

        // Manual Reset Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تغيير سريع ببرقم الهاتف 📱", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                OutlinedTextField(
                    value = targetUserPhone,
                    onValueChange = { targetUserPhone = it },
                    label = { Text("رقم هاتف الحساب المستهدف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = newPasswordInput,
                    onValueChange = { newPasswordInput = it },
                    label = { Text("كلمة المرور الجديدة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Button(
                    onClick = {
                        if (targetUserPhone.isNotBlank() && newPasswordInput.isNotBlank()) {
                            viewModel.resetAccountPassword("PROVIDER", targetUserPhone.trim(), newPasswordInput.trim())
                            Toast.makeText(context, "✅ تم تحديث كلمة المرور للمستخدم $targetUserPhone بنجاح", Toast.LENGTH_SHORT).show()
                            targetUserPhone = ""
                            newPasswordInput = ""
                        } else {
                            Toast.makeText(context, "الرجاء إدخال رقم الهاتف وكلمة المرور الجديدة", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                ) {
                    Text("تحديث كلمة المرور 🔑", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Subtabs
        val passTabs = listOf(
            Triple("SERVICES", "🔧 الخدمات والفنيين", activatedProviders.size),
            Triple("STORES", "🏪 المتاجر", stores.filter { !it.categoryId.contains("طبي") && !it.categoryId.contains("عياد") && !it.categoryId.contains("مطعم") && !it.categoryId.contains("كافيه") }.size),
            Triple("RESTAURANTS", "🍔 المطاعم", stores.filter { it.categoryId.contains("مطعم") || it.categoryId.contains("كافيه") }.size),
            Triple("MEDICAL", "🏥 المراكز الطبية", stores.filter { it.categoryId.contains("طبي") || it.categoryId.contains("عياد") }.size),
            Triple("PROPERTIES", "🏠 العقارات", properties.size),
            Triple("JOBS", "💼 الوظائف", jobs.size)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            items(passTabs.size) { idx ->
                val pt = passTabs[idx]
                val isSel = adminPasswordSubTab == pt.first
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { adminPasswordSubTab = pt.first }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                ) {
                    Text("${pt.second} (${pt.third})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                }
            }
        }

        when (adminPasswordSubTab) {
            "SERVICES" -> {
                if (activatedProviders.isEmpty()) {
                    Text("لا توجد حسابات فنيين مسجلة.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                } else {
                    activatedProviders.forEach { p ->
                        AdminPasswordEntityCard(
                            name = p.name,
                            phone = p.phone,
                            category = "خدمات وفنيين",
                            password = p.password,
                            onResetPassword = { newPass: String ->
                                viewModel.resetAccountPassword("PROVIDER", p.phone, newPass)
                                Toast.makeText(context, "🔐 تم تحديث كلمة المرور لـ ${p.name}", Toast.LENGTH_SHORT).show()
                            },
                            context = context
                        )
                    }
                }
            }
            "STORES", "RESTAURANTS", "MEDICAL" -> {
                val filteredStores = stores.filter { s ->
                    val isMed = s.categoryId.contains("طبي") || s.categoryId.contains("عياد")
                    val isRest = s.categoryId.contains("مطعم") || s.categoryId.contains("كافيه")
                    when (adminPasswordSubTab) {
                        "MEDICAL" -> isMed
                        "RESTAURANTS" -> isRest
                        else -> !isMed && !isRest
                    }
                }
                if (filteredStores.isEmpty()) {
                    Text("لا توجد منشآت مسجلة في هذا القسم.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                } else {
                    filteredStores.forEach { s ->
                        AdminPasswordEntityCard(
                            name = s.name,
                            phone = s.phone,
                            category = s.categoryId,
                            password = s.password,
                            onResetPassword = { newPass: String ->
                                viewModel.resetAccountPassword("STORE", s.phone, newPass)
                                Toast.makeText(context, "🔐 تم تحديث كلمة المرور لـ ${s.name}", Toast.LENGTH_SHORT).show()
                            },
                            context = context
                        )
                    }
                }
            }
            "PROPERTIES" -> {
                if (properties.isEmpty()) {
                    Text("لا توجد عقارات مسجلة.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                } else {
                    properties.forEach { prop ->
                        AdminPasswordEntityCard(
                            name = prop.title,
                            phone = prop.phone,
                            category = "عقار",
                            password = "1234",
                            onResetPassword = { newPass: String ->
                                viewModel.resetAccountPassword("STORE", prop.phone, newPass)
                                Toast.makeText(context, "🔐 تم تحديث كلمة المرور لـ ${prop.title}", Toast.LENGTH_SHORT).show()
                            },
                            context = context
                        )
                    }
                }
            }
            "JOBS" -> {
                if (jobs.isEmpty()) {
                    Text("لا توجد وظائف مسجلة.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                } else {
                    jobs.forEach { job ->
                        AdminPasswordEntityCard(
                            name = job.title,
                            phone = job.phone,
                            category = "وظيفة",
                            password = "1234",
                            onResetPassword = { newPass: String ->
                                viewModel.resetAccountPassword("JOB", job.phone, newPass)
                                Toast.makeText(context, "🔐 تم تحديث كلمة المرور لـ ${job.title}", Toast.LENGTH_SHORT).show()
                            },
                            context = context
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPasswordEntityCard(
    name: String,
    phone: String,
    category: String,
    password: String?,
    onResetPassword: (String) -> Unit,
    context: android.content.Context
) {
    var editPass by remember { mutableStateOf(password ?: "") }
    var showPass by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("القسم: $category", fontSize = 10.sp, color = Color(0xFF3B82F6))
            }
            Text("رقم الهاتف: $phone", fontSize = 11.sp, color = Color.LightGray)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🔑 كلمة المرور الحالية: ${if (showPass) (password ?: "غير متوفرة") else "••••••••"}", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                TextButton(onClick = { showPass = !showPass }) {
                    Text(if (showPass) "إخفاء" else "إظهار", fontSize = 10.sp, color = Color.Yellow)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = editPass,
                    onValueChange = { editPass = it },
                    label = { Text("كلمة مرور جديدة", fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Button(
                    onClick = {
                        if (editPass.isNotBlank()) {
                            onResetPassword(editPass)
                        } else {
                            Toast.makeText(context, "الرجاء إدخال كلمة المرور الجديدة", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("تحديث 🔒", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        val whatsappText = "مرحباً يا غالي، كلمة المرور الخاصة بحسابك في دليل خدمات اليمن هي: ${password ?: "غير متوفرة"}"
                        val whatsappUrl = "https://wa.me/967${phone.trim().removePrefix("0").removePrefix("+967")}?text=${Uri.encode(whatsappText)}"
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "فشل فتح واتساب", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Text("🟢 واتساب", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val smsText = "كلمة المرور الخاصة بحسابك في دليل خدمات اليمن هي: ${password ?: "غير متوفرة"}"
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
                                putExtra("sms_body", smsText)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "فشل فتح SMS", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Text("💬 رسالة SMS", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
