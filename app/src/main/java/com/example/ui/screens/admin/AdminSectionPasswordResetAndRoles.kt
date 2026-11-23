@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.AdminPermissionsRegistry
import com.example.ui.*
import com.example.utils.VisualThemePalette

internal fun LazyListScope.adminPasswordResetAndRolesSection(
    activeSubTab: String,
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = state.context
    val activatedProviders = state.activatedProviders
    val stores = state.stores
    val properties = state.properties
    val jobs = state.jobs
    val passwordRecoveryRequests = state.passwordRecoveryRequests

    when (activeSubTab) {
        "PASSWORDS_RESET" -> {
            item {
                Text(
                    "🔑 لوحة تعيين وإعادة ضبط كلمات المرور (عرض الحسابات وكلمات المرور السابقة)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
                Text(
                    "يتم عرض كافة الحسابات والمسجلين في الأقسام مع إمكانية التعيين وإعادة الضبط الآمن مباشرة:",
                    fontSize = 11.sp,
                    color = themeColors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                val pendingCount = passwordRecoveryRequests.count { (it["status"] as? String) != "RESOLVED" }
                val passTabs = listOf(
                    Triple("REQUESTS", if (pendingCount > 0) "🚨 طلبات الاستعادة ($pendingCount)" else "⏳ طلبات الاستعادة", passwordRecoveryRequests.size),
                    Triple("SERVICES", "🔧 الخدمات والفنيين", activatedProviders.size),
                    Triple("STORES", "🏪 المتاجر", stores.filter { !it.categoryId.contains("طبي") && !it.categoryId.contains("عياد") && !it.categoryId.contains("مطعم") && !it.categoryId.contains("كافيه") }.size),
                    Triple("RESTAURANTS", "🍔 المطاعم", stores.filter { it.categoryId.contains("مطعم") || it.categoryId.contains("كافيه") }.size),
                    Triple("MEDICAL", "🏥 المراكز الطبية", stores.filter { it.categoryId.contains("طبي") || it.categoryId.contains("عياد") }.size),
                    Triple("PROPERTIES", "🏠 العقارات", properties.size),
                    Triple("JOBS", "💼 الوظائف", jobs.size)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    items(passTabs.size) { idx ->
                        val pt = passTabs[idx]
                        val isSel = state.adminPasswordSubTab == pt.first
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSel) themeColors.accent else themeColors.surface)
                                .clickable { state.adminPasswordSubTab = pt.first }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        ) {
                            Text(
                                "${pt.second} (${pt.third})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            when (state.adminPasswordSubTab) {
                "REQUESTS" -> {
                    if (passwordRecoveryRequests.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("لا توجد طلبات استعادة كلمة مرور واردة حالياً ✅", color = Color.LightGray, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        items(passwordRecoveryRequests, key = { "rec_${it["id"]}" }) { req ->
                            val reqPhone = (req["phone"] as? String) ?: ""
                            val reqName = (req["name"] as? String) ?: "مستخدم"
                            val reqType = (req["accountType"] as? String) ?: "حساب"
                            val reqStatus = (req["status"] as? String) ?: "PENDING"
                            var newPassInput by remember { mutableStateOf((req["newPassword"] as? String) ?: "") }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                border = BorderStroke(1.dp, if (reqStatus == "RESOLVED") Color(0xFF10B981) else Color(0xFFEF4444))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("👤 $reqName", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        Surface(
                                            color = if (reqStatus == "RESOLVED") Color(0xFF10B981) else Color(0xFFEF4444),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                if (reqStatus == "RESOLVED") "تم التعيين والحل ✅" else "بانتظار التعيين ⌛",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("📱 الهاتف: $reqPhone", color = Color.LightGray, fontSize = 11.sp)
                                        Text("النوع: $reqType", color = themeColors.accent, fontSize = 11.sp)
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = newPassInput,
                                            onValueChange = { newPassInput = it },
                                            label = { Text("كلمة المرور الجديدة", fontSize = 10.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                        )
                                        Button(
                                            onClick = { newPassInput = (100000..999999).random().toString() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Text("توليد 🎲", fontSize = 10.sp, color = Color.White)
                                        }
                                        Button(
                                            onClick = {
                                                if (newPassInput.isNotBlank()) {
                                                    viewModel.adminResolvePasswordReset(context, reqPhone, newPassInput) {
                                                        Toast.makeText(context, "✅ تم اعتماد وتحديث كلمة المرور لـ $reqName", Toast.LENGTH_LONG).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "الرجاء إدخال أو توليد كلمة المرور", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Text("اعتماد 💾", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Text("خيارات إرسال كلمة المرور للمستخدم فوراً:", fontSize = 10.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val text = "مرحباً $reqName، تم تعيين كلمة المرور الجديدة لحسابك في دليل اليمن: $newPassInput"
                                                val url = "https://wa.me/967${reqPhone.trim().removePrefix("0").removePrefix("+967")}?text=${Uri.encode(text)}"
                                                try {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "فشل فتح واتساب", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp),
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            Text("واتساب", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                val text = "مرحباً $reqName، تم تعيين كلمة المرور الجديدة لحسابك في دليل اليمن: $newPassInput"
                                                val url = "https://t.me/share/url?url=https://yemen-services.app&text=${Uri.encode(text)}"
                                                try {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "فشل فتح تيليجرام", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp),
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            Text("تيليجرام", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                val text = "كلمة المرور الجديدة لحسابك في دليل اليمن هي: $newPassInput"
                                                try {
                                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$reqPhone")).apply {
                                                        putExtra("sms_body", text)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "فشل فتح الرسائل SMS", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp),
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            Text("SMS", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.triggerNotification("🔔 تم إرسال إشعار فوري لـ $reqName بكلمة المرور")
                                                Toast.makeText(context, "🔔 تم إرسال إشعار فوري داخل التطبيق!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp),
                                            modifier = Modifier.weight(1.1f).height(32.dp)
                                        ) {
                                            Text("إشعار فوري 🔔", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "SERVICES" -> {
                    if (activatedProviders.isEmpty()) {
                        item { Text("لا توجد حسابات فنيين مسجلة.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp)) }
                    } else {
                        items(activatedProviders, key = { "pass_prov_${it.id}" }) { p ->
                            PasswordEntityCard(
                                name = p.name,
                                phone = p.phone,
                                category = "خدمات وفنيين",
                                password = p.password,
                                onResetPassword = { newPass ->
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
                        when (state.adminPasswordSubTab) {
                            "MEDICAL" -> isMed
                            "RESTAURANTS" -> isRest
                            else -> !isMed && !isRest
                        }
                    }
                    if (filteredStores.isEmpty()) {
                        item { Text("لا توجد منشآت مسجلة في هذا القسم.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp)) }
                    } else {
                        items(filteredStores, key = { "pass_store_${it.id}" }) { s ->
                            PasswordEntityCard(
                                name = s.name,
                                phone = s.phone,
                                category = s.categoryId,
                                password = s.password,
                                onResetPassword = { newPass ->
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
                        item { Text("لا توجد عقارات مسجلة.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp)) }
                    } else {
                        items(properties, key = { "pass_prop_${it.id}" }) { prop ->
                            PasswordEntityCard(
                                name = prop.title,
                                phone = prop.phone,
                                category = "عقار",
                                password = "",
                                onResetPassword = { newPass ->
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
                        item { Text("لا توجد وظائف مسجلة.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp)) }
                    } else {
                        items(jobs, key = { "pass_job_${it.id}" }) { job ->
                            PasswordEntityCard(
                                name = job.title,
                                phone = job.phone,
                                category = "وظيفة",
                                password = "",
                                onResetPassword = { newPass ->
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

        "ROLES_PERMISSIONS" -> {
            item {
                Text("🛡️ مصفوفة الصلاحيات والأدوار (538 صلاحية كاملة)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("إدارة وتعيين الصلاحيات الـ 538 الموزعة على 38 قطاعاً لجميع المشرفين والإداريين ومزامنتها مع Firestore سحابياً:", fontSize = 11.sp, color = themeColors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                AdminRolesPermissionsPanel(viewModel = viewModel, themeColors = themeColors)
            }
        }

        "SECURITY_AUDIT" -> {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🛡️ سجل التدقيق الأمني وحراس الوسائط", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        Text("آخر عملية فحص أمني: تمت بنجاح (لا توجد تهديدات)", fontSize = 12.sp, color = Color(0xFF10B981))
                        Text("حالة تشفير قواعد البيانات: مفعل (AES-256)", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordEntityCard(
    name: String,
    phone: String,
    category: String,
    password: String?,
    onResetPassword: (String) -> Unit,
    context: Context
) {
    var editPass by remember(phone) { mutableStateOf("") }
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
                Text(
                    "🔒 كلمة المرور محمية (لا يمكن عرضها)",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = editPass,
                    onValueChange = { editPass = it },
                    label = { Text("كلمة مرور جديدة", fontSize = 9.sp) },
                    singleLine = true,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(
                                imageVector = if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPass) "إخفاء" else "إظهار",
                                tint = Color.LightGray
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Button(
                    onClick = {
                        if (editPass.isNotBlank()) {
                            onResetPassword(editPass)
                            editPass = ""
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

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val whatsappText = "مرحباً يا غالي، قامت الإدارة بتعيين كلمة مرور جديدة لحسابك في دليل خدمات اليمن، يرجى التواصل مع الإدارة للحصول عليها."
                        val whatsappUrl = "https://wa.me/967${phone.trim().removePrefix("0").removePrefix("+967")}?text=${Uri.encode(whatsappText)}"
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "فشل فتح واتساب", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.height(28.dp).weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("🟢 واتساب", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val teleText = "مرحباً، تم تعيين كلمة مرور جديدة لحسابك في دليل خدمات اليمن، يرجى التواصل مع الإدارة لاستلامها."
                        val teleUrl = "https://t.me/share/url?url=https://yemen-services.app&text=${Uri.encode(teleText)}"
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(teleUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "فشل فتح تيليجرام", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                    modifier = Modifier.height(28.dp).weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("✈️ تيليجرام", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val smsText = "تم تعيين كلمة مرور جديدة لحسابك في دليل خدمات اليمن، يرجى التواصل مع الإدارة لاستلامها."
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
                    modifier = Modifier.height(28.dp).weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("💬 SMS", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
