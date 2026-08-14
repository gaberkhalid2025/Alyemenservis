@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager

@Composable
fun AdminAdvancedChatPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_ADVANCED_CHAT")) {
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
    val settingsState by viewModel.settings.collectAsState()

    var routingInput by remember(settingsState.chatRoutingMode) { mutableStateOf(settingsState.chatRoutingMode) }
    var identityModeInput by remember(settingsState.chatDisplayIdentityMode) { mutableStateOf(settingsState.chatDisplayIdentityMode) }
    var allowText by remember(settingsState.isChatTextEnabled) { mutableStateOf(settingsState.isChatTextEnabled) }
    var allowAudio by remember(settingsState.isChatAudioEnabled) { mutableStateOf(settingsState.isChatAudioEnabled) }
    var allowImage by remember(settingsState.isChatImageEnabled) { mutableStateOf(settingsState.isChatImageEnabled) }
    var allowVideo by remember(settingsState.isChatVideoEnabled) { mutableStateOf(settingsState.isChatVideoEnabled) }
    var allowCall by remember(settingsState.isChatCallEnabled) { mutableStateOf(settingsState.isChatCallEnabled) }
    var blockAllChat by remember(settingsState.disableChatAll) { mutableStateOf(settingsState.disableChatAll) }
    var blockUsersChat by remember(settingsState.disableChatUsers) { mutableStateOf(settingsState.disableChatUsers) }
    var blockProvidersChat by remember(settingsState.disableChatProviders) { mutableStateOf(settingsState.disableChatProviders) }
    var blockedIdsInput by remember(settingsState.chatBlockedIds) { mutableStateOf(settingsState.chatBlockedIds) }
    var disabledCatsInput by remember(settingsState.chatDisabledCategories) { mutableStateOf(settingsState.chatDisabledCategories) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("⚡ التحكم المتقدم بنظام المحادثات الفورية وتوجيهها", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("التحكم الشامل بكل صلاحيات الدردشة، الوسائط، التوجيه الذكي، وحظر الحسابات أو الأقسام:", fontSize = 11.sp, color = themeColors.textSecondary)

                // 1. Routing Mode
                Text("نمط توجيه المحادثات الفورية بالمنصة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "DEFAULT" to "توجيه مباشر بين العميل ومقدم الخدمة 🌐",
                        "ADMIN_ONLY" to "تحويل جميع المحادثات للإدارة والدعم الفني فقط 👑",
                        "ADMIN_SUPERVISORS" to "تحويل جميع المحادثات للأدمن والمشرفين 👮"
                    ).forEach { (mode, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { routingInput = mode }) {
                            RadioButton(selected = routingInput == mode, onClick = { routingInput = mode })
                            Text(label, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                // 2. Identity Display Mode
                Text("طريقة عرض هوية أطراف المحادثة بالشات:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("NAME_AND_PHONE" to "الاسم + الرقم", "NAME_ONLY" to "الاسم فقط", "NAME_AND_ID" to "الاسم + ID", "PHONE_ONLY" to "الرقم فقط").forEach { (mode, label) ->
                        FilterChip(
                            selected = identityModeInput == mode,
                            onClick = { identityModeInput = mode },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                        )
                    }
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                // 3. Media Toggles
                Text("أنواع الوسائط والمحتوى المسموح بها بالدردشة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = allowText, onCheckedChange = { allowText = it }); Spacer(modifier = Modifier.width(8.dp)); Text("الرسائل النصية 💬", color = Color.White, fontSize = 11.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = allowAudio, onCheckedChange = { allowAudio = it }); Spacer(modifier = Modifier.width(8.dp)); Text("الرسائل الصوتية والملاحظات الصوتية 🎤", color = Color.White, fontSize = 11.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = allowImage, onCheckedChange = { allowImage = it }); Spacer(modifier = Modifier.width(8.dp)); Text("إرسال المعاينات والصور 📷", color = Color.White, fontSize = 11.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = allowVideo, onCheckedChange = { allowVideo = it }); Spacer(modifier = Modifier.width(8.dp)); Text("إرسال الفيديو 🎥", color = Color.White, fontSize = 11.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = allowCall, onCheckedChange = { allowCall = it }); Spacer(modifier = Modifier.width(8.dp)); Text("المكالمات المباشرة داخل التطبيق 📞", color = Color.White, fontSize = 11.sp) }
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                // 4. Global Toggles & Blacklist
                Text("قيود وتعليق الدردشة الشاملة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = blockAllChat, onCheckedChange = { blockAllChat = it }); Spacer(modifier = Modifier.width(8.dp)); Text("تعليق وإيقاف الشات للجميع بجميع الأقسام ⛔", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = blockUsersChat, onCheckedChange = { blockUsersChat = it }); Spacer(modifier = Modifier.width(8.dp)); Text("منع العملاء والمستخدمين من مراسلة الفنيين 🚫", color = Color.Yellow, fontSize = 11.sp) }
                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = blockProvidersChat, onCheckedChange = { blockProvidersChat = it }); Spacer(modifier = Modifier.width(8.dp)); Text("منع مقدمي الخدمات من المراسلة 🚫", color = Color.Yellow, fontSize = 11.sp) }

                OutlinedTextField(
                    value = blockedIdsInput,
                    onValueChange = { blockedIdsInput = it },
                    label = { Text("قائمة معرفات/أرقام الهواتف المحظورة من الشات (مفصولة بفارزة)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = disabledCatsInput,
                    onValueChange = { disabledCatsInput = it },
                    label = { Text("معرفات الأقسام المعطل بها الشات (مفصولة بفارزة)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Button(
                    onClick = {
                        val updated = settingsState.copy(
                            chatRoutingMode = routingInput,
                            chatDisplayIdentityMode = identityModeInput,
                            isChatTextEnabled = allowText,
                            isChatAudioEnabled = allowAudio,
                            isChatImageEnabled = allowImage,
                            isChatVideoEnabled = allowVideo,
                            isChatCallEnabled = allowCall,
                            disableChatAll = blockAllChat,
                            disableChatUsers = blockUsersChat,
                            disableChatProviders = blockProvidersChat,
                            chatBlockedIds = blockedIdsInput.trim(),
                            chatDisabledCategories = disabledCatsInput.trim()
                        )
                        viewModel.updateAdminSettings(updated)
                        Toast.makeText(context, "تم حفظ ومزامنة صلاحيات ونظام المحادثات الفورية ⚡", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("حفظ ومزامنة صلاحيات المحادثات ⚡", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
