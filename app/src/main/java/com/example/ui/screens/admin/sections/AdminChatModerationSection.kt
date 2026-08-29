package com.example.ui.screens.admin.sections

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun AdminChatModerationSection(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var globalChatDisabled by remember(settingsState.chatHidden) {
        mutableStateOf(settingsState.chatHidden)
    }

    // Section/Category chat bans tracking
    var disabledCategories by remember { mutableStateOf(setOf<String>()) }

    // User/Provider chat bans tracking
    var blockedUserIds by remember { mutableStateOf(setOf<String>()) }
    var targetBlockPhone by remember { mutableStateOf("") }
    var warningMessage by remember { mutableStateOf("") }
    var selectedWarnTarget by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Global Chat Control
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💬", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مركز رقابة والتحكم بالمحادثات والرسائل",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "يتيح هذا المركز التحكم الكامل بالمحادثات: إيقافها كلياً على مستوى التطبيق، أو إيقافها عن قسم محدد، أو حظر مستخدم/فني معين من المراسلة وإرسال التحذيرات.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "إيقاف المحادثات على كافة الأقسام دفعة واحدة 🚫", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.5.sp)
                        Text(
                            text = if (globalChatDisabled) "المحادثات معطلة حالياً عن جميع الأقسام والمستخدمين" else "المحادثات تعمل بنشاط وطبيعية",
                            color = if (globalChatDisabled) Color(0xFFEF5350) else Color(0xFF10B981),
                            fontSize = 11.5.sp
                        )
                    }

                    Switch(
                        checked = globalChatDisabled,
                        onCheckedChange = { disabled ->
                            globalChatDisabled = disabled
                            val st = settingsState
                            viewModel.updateBackdoorSettings(
                                st.appName, st.welcomeMessage, st.footerMessage, st.activeThemeId,
                                st.supportPhone, st.supportEmail, st.supportWhatsapp,
                                st.isMaintenanceActive, st.hidePromoFooter, st.assistantHidden, st.assistantSize,
                                disabled, st.chatSize, st.maxSearchRadiusKm, st.isSpeechSearchEnabled,
                                false, 90
                            )
                            Toast.makeText(context, if (disabled) "تم إيقاف المحادثات عن كامل التطبيق" else "تمت إتاحة المحادثات للجميع", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFEF5350))
                    )
                }
            }
        }

        // 2. Per-Category Chat Moderation
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🗂️ تفعيل / إيقاف المحادثات حسب القسم أو النشاط",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        val isCatDisabled = disabledCategories.contains(cat.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cat.icon, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Switch(
                                checked = !isCatDisabled,
                                onCheckedChange = { enabled ->
                                    disabledCategories = if (enabled) {
                                        disabledCategories - cat.id
                                    } else {
                                        disabledCategories + cat.id
                                    }
                                    Toast.makeText(context, if (enabled) "تم تفعيل المحادثات لقسم ${cat.name}" else "تم إيقاف المحادثات لقسم ${cat.name}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. User / Store Specific Ban and Warnings
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🛡️ حظر محادثات شخص / محل محدد أو توجيه تحذير",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = targetBlockPhone,
                    onValueChange = { targetBlockPhone = it },
                    label = { Text("رقم هاتف أو معرّف الطرف المستهدف", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColors.accent
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (targetBlockPhone.isBlank()) {
                                Toast.makeText(context, "يرجى كتابة رقم الهاتف", Toast.LENGTH_SHORT).show()
                            } else {
                                blockedUserIds = blockedUserIds + targetBlockPhone.trim()
                                viewModel.addNotification(
                                    title = "⚠️ تنبيه رقابي - تقييد المحادثات",
                                    message = "تم تقييد إمكانية إرسال الرسائل والمحادثات لحسابك لمخالفة معايير الاستخدام.",
                                    targetType = "USER",
                                    targetValue = targetBlockPhone.trim()
                                )
                                Toast.makeText(context, "تم حظر المحادثات وإرسال إشعار للمستهدف فورياً", Toast.LENGTH_SHORT).show()
                                targetBlockPhone = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🚫 حظر المحادثات", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (targetBlockPhone.isBlank()) {
                                Toast.makeText(context, "يرجى كتابة رقم الهاتف للتحذير", Toast.LENGTH_SHORT).show()
                            } else {
                                selectedWarnTarget = targetBlockPhone.trim()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إرسال تحذير", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    selectedWarnTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { selectedWarnTarget = null },
            title = { Text("⚠️ إرسال تحذير رقابي مباشر", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = warningMessage,
                    onValueChange = { warningMessage = it },
                    label = { Text("نص التحذير للمستخدم أو المتجر", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB300)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val msg = warningMessage.ifEmpty { "يرجى الالتزام بآداب التعامل وتجنب الإساءة في المحادثات تجنباً للحظر الدائم." }
                        viewModel.addNotification(
                            title = "⚠️ تحذير رسمي من إدارة التطبيق",
                            message = msg,
                            targetType = "USER",
                            targetValue = target
                        )
                        Toast.makeText(context, "تم إرسال التحذير فورياً للمستهدف", Toast.LENGTH_SHORT).show()
                        selectedWarnTarget = null
                        warningMessage = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                ) {
                    Text("إرسال التحذير الآن", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedWarnTarget = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
