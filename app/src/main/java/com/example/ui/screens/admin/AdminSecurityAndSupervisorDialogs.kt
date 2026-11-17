package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.*
import com.example.utils.VisualThemePalette

@Composable
internal fun AdminSecurityAndSupervisorDialogs(
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = state.context

    // 7. Wipe confirming AlertDialog with obscured password verification
    if (state.showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { state.showWipeConfirmDialog = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("🚨 تأكيد الهوية الأمنية للمصفي", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "هذه العملية فائقة الخطورة وذات تصفية كلية فورية للمركز الفني بالمنصة والدليل اليمني. الرجاء كتابة الرمز السري للأدمن لإكمال المسح المخصص:",
                        color = themeColors.textSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = state.wipeInputPassword,
                        onValueChange = { state.wipeInputPassword = it },
                        label = { Text("كلمة مرور الأدمن") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.verifyAdminOrOwnerPassword(state.wipeInputPassword)) {
                            val selectedCols = mutableListOf<String>()
                            if (state.wipeProvidersChecked) selectedCols.add("providers")
                            if (state.wipeBookingsChecked) selectedCols.add("bookings")
                            if (state.wipeChatsChecked) selectedCols.add("chat_channels")
                            if (state.wipeNotifsChecked) selectedCols.add("notifications")
                            if (state.wipeReportsChecked) selectedCols.add("reports")
                            if (state.wipeCategoriesChecked) selectedCols.add("categories")
                            if (state.wipePendingChecked) selectedCols.add("pending_providers")
                            if (state.wipeBannersChecked) selectedCols.add("banners")
                            if (state.wipeSupervisorsChecked) selectedCols.add("supervisors")
                            if (state.wipeCitiesChecked) selectedCols.add("cities")
                            if (state.wipeThemesChecked) selectedCols.add("color_themes")

                            val success = viewModel.wipeSelectedDatabaseData(state.wipeInputPassword, selectedCols)
                            if (success) {
                                state.showWipeConfirmDialog = false
                                state.wipeInputPassword = ""
                                Toast.makeText(context, "💥 تم تصفية الفئات المحددة وإعادتها للصفر بنجاح!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "❌ كلمة مرور الأدمن غير صحيحة! تم منع التطهير.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد مسح وتطهير النظام العظيم", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        state.showWipeConfirmDialog = false
                        state.wipeInputPassword = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("إلغاء عملية التطهير")
                }
            }
        )
    }

    // 9. Editing Supervisor Dialog Control
    state.editingSupervisorObj?.let { supervisor ->
        var editSupName by rememberSaveable(supervisor.id) { mutableStateOf(supervisor.name) }
        var editSupRole by rememberSaveable(supervisor.id) { mutableStateOf(supervisor.role) }
        var editSupPasscode by rememberSaveable(supervisor.id) { mutableStateOf(supervisor.passcodeHash) }
        var editSupPermissions by remember(supervisor.id) { mutableStateOf(supervisor.permissions) }

        Dialog(onDismissRequest = { state.editingSupervisorObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("✏️ تعديل صلاحيات وبيانات المشرف", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = editSupName,
                        onValueChange = { editSupName = it },
                        label = { Text("اسم المشرف الثلاثي") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editSupPasscode,
                        onValueChange = { editSupPasscode = it },
                        label = { Text("رمز المرور والدخول (Passcode)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Text("اختر الأدوار والصلاحيات الأمنية للمشرف (يمكنك اختيار صلاحية واحدة أو أكثر):", color = themeColors.textSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val roles = listOf("ADMIN", "AUDITOR", "SUPPORT", "OPERATIONS")
                        roles.forEach { rl ->
                            val isSel = editSupRole.split(",").contains(rl)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) themeColors.accent else Color.Black.copy(alpha = 0.3f))
                                    .clickable {
                                        val currentSelected = editSupRole.split(",").filter { it.isNotEmpty() }.toMutableList()
                                        if (currentSelected.contains(rl)) {
                                            currentSelected.remove(rl)
                                        } else {
                                            currentSelected.add(rl)
                                        }
                                        if (currentSelected.isEmpty()) {
                                            currentSelected.add("SUPPORT")
                                        }
                                        editSupRole = currentSelected.joinToString(",")
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (rl) {
                                        "ADMIN" -> "مدير 👑"
                                        "AUDITOR" -> "مدقق 🔍"
                                        "OPERATIONS" -> "ميداني 🚗"
                                        else -> "دعم 📞"
                                    },
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    AdminPermissionsSelectorView(
                        themeColors = themeColors,
                        selectedPermissions = editSupPermissions,
                        onPermissionsChanged = { editSupPermissions = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (editSupName.trim().isNotEmpty() && editSupPasscode.trim().isNotEmpty()) {
                                    viewModel.editSupervisor(
                                        id = supervisor.id,
                                        name = editSupName.trim(),
                                        role = editSupRole,
                                        passcode = editSupPasscode.trim(),
                                        permissions = editSupPermissions
                                    )
                                    state.editingSupervisorObj = null
                                } else {
                                    viewModel.triggerNotification("⚠️ يرجى كتابة الاسم والرمز بالكامل")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💾 حفظ التعديل والصلاحيات", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { state.editingSupervisorObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // 10. Password Recovery Notification Action Dialog
    state.selectedRecoveryNotif?.let { notif ->
        var phoneExtract by remember(notif.id) {
            mutableStateOf(
                try {
                    val phoneRegex = Regex("(?:هاتف:|الرقم:|الهاتف:)?\\s*([0-9]{9,10})")
                    phoneRegex.find(notif.message)?.groupValues?.getOrNull(1) ?: ""
                } catch (_: Exception) {
                    ""
                }
            )
        }
        val nameExtract = remember(notif.id) {
            try {
                when {
                    notif.message.contains("المتجر") ->
                        notif.message.split("المتجر").getOrNull(1)?.substringBefore("(")?.trim() ?: "المستخدم"
                    notif.message.contains("الحساب:") ->
                        notif.message.split("الحساب:").getOrNull(1)?.substringBefore("(")?.trim() ?: "المستخدم"
                    else -> "المستخدم"
                }
            } catch (_: Exception) {
                "المستخدم"
            }
        }

        var newPasswordInput by remember(notif.id) { mutableStateOf("123456") }
        var notifyActionChoice by remember(notif.id) { mutableStateOf("DIRECT_PASSWORD") }

        AlertDialog(
            onDismissRequest = { state.selectedRecoveryNotif = null },
            containerColor = Color(0xFF0F172A),
            title = { Text("🔓 إدارة استعادة كلمة المرور للحساب", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("تفاصيل الطلب: ${notif.message}", fontSize = 11.sp, color = Color.White)

                    OutlinedTextField(
                        value = phoneExtract,
                        onValueChange = { phoneExtract = it },
                        label = { Text("رقم الهاتف المستهدف") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("كلمة المرور الجديدة (أو الحالية)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Text("اختر الإجراء الإداري للرد والاشعار:", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { notifyActionChoice = "DIRECT_PASSWORD" }) {
                        RadioButton(selected = notifyActionChoice == "DIRECT_PASSWORD", onClick = { notifyActionChoice = "DIRECT_PASSWORD" })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إعادة تعيين كلمة المرور وإرسالها بإشعار مباشر للمستخدم", fontSize = 10.sp, color = Color.White)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { notifyActionChoice = "VERIFICATION_WHATSAPP" }) {
                        RadioButton(selected = notifyActionChoice == "VERIFICATION_WHATSAPP", onClick = { notifyActionChoice = "VERIFICATION_WHATSAPP" })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("طلب التحقق عبر الواتس / التليجرام أو المحادثة الفورية", fontSize = 10.sp, color = Color.White)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { notifyActionChoice = "INSTANT_CHAT" }) {
                        RadioButton(selected = notifyActionChoice == "INSTANT_CHAT", onClick = { notifyActionChoice = "INSTANT_CHAT" })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فتح محادثة فورية مباشرة مع المستخدم لإثبات الهوية", fontSize = 10.sp, color = Color.White)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (phoneExtract.isNotEmpty() && newPasswordInput.isNotEmpty()) {
                            viewModel.adminResetAccountPassword(
                                phone = phoneExtract,
                                newPassword = newPasswordInput.trim(),
                                notifyAction = notifyActionChoice,
                                customerName = nameExtract
                            )
                            if (notifyActionChoice == "INSTANT_CHAT") {
                                viewModel.getOrCreateChatChannel(
                                    providerId = "admin",
                                    providerName = "الإدارة والدعم",
                                    customerId = phoneExtract,
                                    customerName = nameExtract
                                )
                                Toast.makeText(context, "💬 تم فتح محادثة الدعم وإرسال الإجراء بنجاح!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "✅ تم إعادة تعيين كلمة المرور وإرسال الإشعار بنجاح!", Toast.LENGTH_LONG).show()
                            }
                            state.selectedRecoveryNotif = null
                        } else {
                            Toast.makeText(context, "الرجاء التأكد من رقم الهاتف وكلمة المرور", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("تنفيذ الإجراء وإرسال 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                Button(
                    onClick = { state.selectedRecoveryNotif = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("إلغاء", color = Color.White, fontSize = 11.sp)
                }
            }
        )
    }
}
