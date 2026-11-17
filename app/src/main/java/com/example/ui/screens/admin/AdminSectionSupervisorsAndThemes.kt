@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.AdminPermissionsRegistry
import com.example.data.models.PermissionCategory
import com.example.ui.*
import com.example.utils.VisualThemePalette

internal fun LazyListScope.adminSupervisorsAndThemesSection(
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = state.context
    val settingsState = state.settingsState
    val supervisorsList = state.supervisorsList
    val colorPalettesList = state.colorPalettesList

    item {
        Text("👥 إدارة حسابات المشرفين وصلاحيات التطبيق (مزامنة فورية)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Spacer(modifier = Modifier.height(4.dp))
    }

    item {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("أضف مشرفاً جديداً بكلمة مرور وصلاحية محددة:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                OutlinedTextField(
                    value = state.supervisorInputName,
                    onValueChange = { state.supervisorInputName = it },
                    label = { Text("اسم المشرف الكامل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = state.supervisorInputPasscode,
                    onValueChange = { state.supervisorInputPasscode = it },
                    label = { Text("كلمة مرور الدخول") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Text("اختر الأدوار والصلاحيات الأمنية للمشرف المضاف (يمكنك اختيار صلاحية واحدة أو أكثر):", color = themeColors.textSecondary, fontSize = 10.sp)
                val roles = listOf("SUPPORT" to "دعم فني", "AUDITOR" to "مدقق ومراقب", "ADMIN" to "مدير رئيسي", "OPERATIONS" to "عمليات")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    roles.forEach { (roleKey, roleName) ->
                        val isSel = state.supervisorInputRole.split(",").contains(roleKey)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) themeColors.accent else Color.Black.copy(alpha = 0.3f))
                                .clickable {
                                    val currentSelected = state.supervisorInputRole.split(",").filter { it.isNotEmpty() }.toMutableList()
                                    if (currentSelected.contains(roleKey)) {
                                        currentSelected.remove(roleKey)
                                    } else {
                                        currentSelected.add(roleKey)
                                    }
                                    if (currentSelected.isEmpty()) {
                                        currentSelected.add("SUPPORT")
                                    }
                                    state.supervisorInputRole = currentSelected.joinToString(",")
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(roleName, fontSize = 9.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                AdminPermissionsSelectorView(
                    themeColors = themeColors,
                    selectedPermissions = state.supervisorInputPermissions,
                    onPermissionsChanged = { state.supervisorInputPermissions = it }
                )

                Button(
                    onClick = {
                        if (state.supervisorInputName.trim().isNotEmpty() && state.supervisorInputPasscode.trim().isNotEmpty()) {
                            viewModel.addSupervisor(
                                name = state.supervisorInputName.trim(),
                                role = state.supervisorInputRole,
                                passcode = state.supervisorInputPasscode.trim(),
                                permissions = state.supervisorInputPermissions
                            )
                            state.supervisorInputName = ""
                            state.supervisorInputPasscode = ""
                            state.supervisorInputPermissions = emptyList()
                        } else {
                            Toast.makeText(context, "الرجاء تعبئة اسم المشرف وكلمة المرور أولاً!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إضافة المشرف وتثبيت الصلاحيات 🛡️", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }

    if (supervisorsList.isNotEmpty()) {
        item {
            Text("📋 قائمة المشرفين المسجلين في النظام الآن:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }

        items(supervisorsList, key = { it.id }) { sup ->
            var showSupPasscode by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sup.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            val displayRoles = sup.role.split(",").map { r ->
                                when (r.trim()) {
                                    "ADMIN" -> "مدير رئيسي 👑"
                                    "AUDITOR" -> "مدقق ومراقب 🔍"
                                    "OPERATIONS" -> "عمليات ميدانية 🚗"
                                    "SUPPORT" -> "دعم فني 📞"
                                    else -> r.trim()
                                }
                            }.joinToString(" + ")
                            Text(
                                "الصلاحيات الممنوحة: $displayRoles (${if (sup.permissions.isEmpty()) "كاملة/افتراضية" else "${sup.permissions.size}/538 مخصصة"})",
                                fontSize = 10.sp,
                                color = themeColors.accent
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "رمز الدخول (Passcode): ${if (showSupPasscode) sup.passcodeHash else "••••••••"}",
                                    fontSize = 11.sp,
                                    color = themeColors.textSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (showSupPasscode) "🔒 إخفاء" else "👁️ إظهار",
                                    fontSize = 9.sp,
                                    color = Color.Yellow,
                                    modifier = Modifier.clickable { showSupPasscode = !showSupPasscode }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { state.editingSupervisorObj = sup }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل المشرف", tint = Color.Green, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { viewModel.removeSupervisor(sup.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف المشرف", tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    item {
        Spacer(modifier = Modifier.height(10.dp))
        Text("🛡️ مصفوفة الصلاحيات الشاملة للنظام (538 صلاحية موزعة على 18 مجالاً)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Spacer(modifier = Modifier.height(4.dp))
    }

    item {
        var showAll538Matrix by rememberSaveable { mutableStateOf(false) }
        var matrixSearchQuery by rememberSaveable { mutableStateOf("") }

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = themeColors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "538 صلاحية أمنية معتمدة ومتزامنة",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { showAll538Matrix = !showAll538Matrix },
                        colors = ButtonDefaults.buttonColors(containerColor = if (showAll538Matrix) Color.DarkGray else themeColors.accent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (showAll538Matrix) "إخفاء المصفوفة ▲" else "عرض الكل (538) ▼",
                            color = if (showAll538Matrix) Color.White else Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "تغطي المنظومة 38 مجالاً وقطاعاً إدارياً شاملاً يضم 538 صلاحية دقيقة ومفصلة: البوابة الخلفية، طلبات الانضمام، الإضافة اليدوية، المحلات، المطاعم، المراكز الطبية، العقارات، الوظائف، المتقدمين، الإحصائيات، الحجوزات، المحادثات، أعضاء الدليل، كلمات المرور، البنرات، الأقسام، المدن، الشكاوى، ترقيات VIP، المشرفين، الألوان، الإشعارات، النسخ الاحتياطي، تنظيف البيانات، التقييمات، المكالمات، الكوبونات، المحظورين، المحذوفات، المدفوعات، التبويبات المخصصة، الأيقونات الذهبية، الدردشات المتقدمة، تخصيص البطاقات، منشئ الأقسام، استمارات التسجيل، الصلاحيات والأدوار، وتحكم الخرائط.",
                    color = themeColors.textSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                if (showAll538Matrix) {
                    OutlinedTextField(
                        value = matrixSearchQuery,
                        onValueChange = { matrixSearchQuery = it },
                        label = { Text("بحث في مصفوفة الصلاحيات (الاسم أو الرمز)...", fontSize = 10.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    PermissionCategory.values().forEach { cat ->
                        var catItems = AdminPermissionsRegistry.getByCategory(cat)
                        if (matrixSearchQuery.isNotBlank()) {
                            val q = matrixSearchQuery.trim().lowercase()
                            catItems = catItems.filter {
                                it.name.lowercase().contains(q) ||
                                    it.key.lowercase().contains(q) ||
                                    it.description.lowercase().contains(q)
                            }
                        }

                        if (catItems.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${cat.iconEmoji} ${cat.arabicTitle}",
                                            color = themeColors.accent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "${catItems.size} صلاحية",
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }

                                    catItems.forEach { perm ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${perm.id}. ${perm.name}",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = perm.description,
                                                    color = Color.Gray,
                                                    fontSize = 8.sp,
                                                    maxLines = 1
                                                )
                                            }
                                            Text(
                                                text = perm.key,
                                                color = themeColors.accent.copy(alpha = 0.8f),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    item {
        Spacer(modifier = Modifier.height(10.dp))
        Text("🎨 نظام الألوان والسمات المتعددة المتزامن فورياً", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Spacer(modifier = Modifier.height(4.dp))
    }

    item {
        var newPaletteName by rememberSaveable { mutableStateOf("") }
        var newPalettePrimary by rememberSaveable { mutableStateOf("#059669") }
        var newPaletteSecondary by rememberSaveable { mutableStateOf("#115E59") }

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("أضف ستايل لوني مخصص ومثبت بالدليل:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                OutlinedTextField(
                    value = newPaletteName,
                    onValueChange = { newPaletteName = it },
                    label = { Text("اسم لوحة الألوان (مثال: الشتاء المتجمد)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPalettePrimary,
                        onValueChange = { newPalettePrimary = it },
                        label = { Text("اللون الأساسي (Primary)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = newPaletteSecondary,
                        onValueChange = { newPaletteSecondary = it },
                        label = { Text("اللون الثانوي (Secondary)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                Button(
                    onClick = {
                        if (newPaletteName.trim().isNotEmpty() && newPalettePrimary.trim().isNotEmpty() && newPaletteSecondary.trim().isNotEmpty()) {
                            viewModel.addColorPalette(newPaletteName.trim(), newPalettePrimary.trim(), newPaletteSecondary.trim())
                            newPaletteName = ""
                        } else {
                            Toast.makeText(context, "الرجاء تعبئة الاسم والألوان الستة عشرية بالكامل!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حقن وإضافة بالدليل المتكامل للألوان", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }

    if (colorPalettesList.isNotEmpty()) {
        item {
            Text("🌈 لوحات الألوان والسمات المضافة حديثاً بالدليل:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }

        items(colorPalettesList, key = { it.id }) { pal ->
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(try { Color(android.graphics.Color.parseColor(pal.primaryHex)) } catch (e: Exception) { Color.Gray })
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(try { Color(android.graphics.Color.parseColor(pal.secondaryHex)) } catch (e: Exception) { Color.DarkGray })
                        )

                        Column {
                            Text(pal.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("رئيسي: ${pal.primaryHex} | ثانوي: ${pal.secondaryHex}", fontSize = 9.sp, color = themeColors.textSecondary)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                val updatedSettings = settingsState.copy(
                                    activeThemeId = "CUSTOM_THEME",
                                    customPrimaryHex = pal.primaryHex,
                                    customSecondaryHex = pal.secondaryHex,
                                    customBackgroundHex = pal.backgroundHex,
                                    customSurfaceHex = pal.surfaceHex
                                )
                                viewModel.saveCustomSettingsState(updatedSettings)
                                Toast.makeText(context, "🌈 تم تطبيق هذا السطح اللوني الآن ومزامنته فوراً!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "تطبيق فوري", tint = Color.Green, modifier = Modifier.size(20.dp))
                        }

                        IconButton(onClick = { viewModel.deleteColorPalette(pal.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف اللون", tint = Color.Red, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
