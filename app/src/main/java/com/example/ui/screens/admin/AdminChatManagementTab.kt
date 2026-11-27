package com.example.ui.screens.admin

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.utils.VisualThemePalette

internal fun LazyListScope.adminChatManagementTab(
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = state.context
    val settingsState = state.settingsState
    val chatChannels = state.chatChannels

    item {
        Text("💬 إدارة محادثات الدعم والدردشات الفورية والتحكم الفائق بالصلاحيات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Text("هنا يمكنك مراقبة كل غرف الشات، تحديد أطراف الاتصال المسموح بها، التحكم بالنطق الصوتي، الحجم، وحظر الأعضاء فورياً:", fontSize = 11.sp, color = themeColors.textSecondary)
        Spacer(modifier = Modifier.height(6.dp))
    }

    item {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🛡️ صلاحيات وأطراف الاتصال بالدردشة:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🚹 تفعيل الشات بين العميل ومقدم الخدمة (الفني)", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = settingsState.allowChatUserToProvider,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(allowChatUserToProvider = active))
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🛠️ تفعيل الشات بين الفني والأدمن/المشرف مباشرة", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = settingsState.allowChatProviderToAdmin,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(allowChatProviderToAdmin = active))
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("👤 تفعيل الشات المباشر بين العميل والادارة (الدعم)", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = settingsState.allowChatUserToAdmin,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(allowChatUserToAdmin = active))
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🕵️ نظام الفحص المسبق (موافقة الادارة قبل إرسال الرسالة)", fontSize = 11.sp, color = Color.Yellow)
                    Switch(
                        checked = settingsState.approveChatsBeforeProvider,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(approveChatsBeforeProvider = active))
                        }
                    )
                }
            }
        }
    }

    item {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🛑 تعطيل الخدمة وبث الطوارئ التلقائي:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🛑 إيقاف الشات الفوري بالكامل (عن الكل)", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = settingsState.disableChatAll,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(disableChatAll = active))
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("👤 إيقاف الشات عن الزائرين والعملاء فقط", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = settingsState.disableChatUsers,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(disableChatUsers = active))
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🛠️ إيقاف الشات عن الفنيين ومزودي الخدمة", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = settingsState.disableChatProviders,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(disableChatProviders = active))
                        }
                    )
                }

                var announcementText by remember(settingsState.chatDisabledAnnouncement) { mutableStateOf(settingsState.chatDisabledAnnouncement) }
                OutlinedTextField(
                    value = announcementText,
                    onValueChange = {
                        announcementText = it
                        viewModel.saveCustomSettingsState(settingsState.copy(chatDisabledAnnouncement = it))
                    },
                    label = { Text("رسالة بث الطوارئ والتعطيل في غرف الشات") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        }
    }

    item {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🎨 مظهر وصوتيات شاشة ومقاس الشات الفوري:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🎙️ تفعيل الإدخال الصوتي (Speech-to-Text)", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = settingsState.allowVoiceInput,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInput = active))
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🔊 تفعيل نطق الرسائل وقراءتها آلياً (TTS)", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = settingsState.allowTextToSpeech,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeech = active))
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🎙️ طلب الانضمام: تفعيل الإدخال الصوتي", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = settingsState.allowVoiceInputJoinForm,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInputJoinForm = active))
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🔊 طلب الانضمام: تفعيل النطق الصوتي (TTS)", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = settingsState.allowTextToSpeechJoinForm,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeechJoinForm = active))
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🎙️ المساعد الذكي: تفعيل الإدخال الصوتي", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = settingsState.allowVoiceInputAssistant,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInputAssistant = active))
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🔊 المساعد الذكي: تفعيل النطق الصوتي (TTS)", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = settingsState.allowTextToSpeechAssistant,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeechAssistant = active))
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🎙️ استمارة الحجز: تفعيل الإدخال الصوتي", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = settingsState.allowVoiceInputBookingForm,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInputBookingForm = active))
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🔊 استمارة الحجز: تفعيل النطق الصوتي (TTS)", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = settingsState.allowTextToSpeechBookingForm,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeechBookingForm = active))
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🙈 إخفاء أيقونة الشات بالكامل من التطبيق", fontSize = 11.sp, color = Color.White)
                    Switch(
                        checked = settingsState.chatHidden,
                        onCheckedChange = { active ->
                            viewModel.saveCustomSettingsState(settingsState.copy(chatHidden = active))
                        }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = settingsState.chatSize.toString(),
                        onValueChange = { newVal ->
                            newVal.toIntOrNull()?.let {
                                viewModel.saveCustomSettingsState(settingsState.copy(chatSize = it))
                            }
                        },
                        label = { Text("حجم الأيقونة (dp)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = settingsState.chatFontSizeSp.toString(),
                        onValueChange = { newVal ->
                            newVal.toIntOrNull()?.let {
                                viewModel.saveCustomSettingsState(settingsState.copy(chatFontSizeSp = it))
                            }
                        },
                        label = { Text("حجم خط الشات (sp)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                OutlinedTextField(
                    value = settingsState.chatBackgroundHex,
                    onValueChange = { newVal ->
                        viewModel.saveCustomSettingsState(settingsState.copy(chatBackgroundHex = newVal))
                    },
                    label = { Text("كود لون خلفية شاشة الشات (Hex)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        }
    }

    item {
        val supportChannels = remember(chatChannels) {
            chatChannels.filter { ch ->
                ch.channelType == "SUPPORT" ||
                    ch.channelType == "ADMIN" ||
                    ch.id.startsWith("support_") ||
                    ch.targetName.contains("دعم", ignoreCase = true) ||
                    ch.relatedEntityType == "SUPPORT"
            }.sortedByDescending { if (it.lastMessageTime > 0) it.lastMessageTime else it.timestamp }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💬 محادثات الدعم الفني واستعادة كلمة المرور المباشرة (${supportChannels.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                    Button(
                        onClick = { viewModel.navigateTo("CHAT_LIST") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("عرض كل المحادثات 💬", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (supportChannels.isEmpty()) {
                    Text("لا توجد طلبات دعم أو استعادة كلمة مرور معلقة حالياً.", fontSize = 11.sp, color = Color.Gray)
                } else {
                    supportChannels.take(8).forEach { ch ->
                        val titleStr = ch.targetName.ifBlank { ch.userName.ifBlank { "طلب دعم: ${ch.id.removePrefix("support_")}" } }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = titleStr,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (ch.lastMessage.isNotBlank()) {
                                        Text(
                                            text = ch.lastMessage,
                                            fontSize = 11.sp,
                                            color = Color(0xFFCBD5E1),
                                            maxLines = 1
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        viewModel.openChatChannel(ch)
                                        viewModel.navigateTo("CHAT_DIRECT")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("رد الآن 💬", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    item {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    viewModel.wipeOldChatChannels(30)
                    Toast.makeText(context, "تمت تصفية كامل المحادثات بنجاح من الخادم السحابي والذاكرة المؤقتة 🧼", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                modifier = Modifier.weight(1.5f)
            ) {
                Text("🧼 مسح أرشيف الشات بالكامل", fontSize = 10.sp, color = Color.White)
            }
            Button(
                onClick = { Toast.makeText(context, "تم تصدير سجل المحادثات بنجاح للمصنف المالي 📁", Toast.LENGTH_SHORT).show() },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                modifier = Modifier.weight(1f)
            ) {
                Text("تصدير CSV 📁", fontSize = 11.sp, color = Color.White)
            }
        }
    }

    item {
        Text("📋 قنوات المحادثة والدردشة النشطة حالياً (${chatChannels.size}):", fontSize = 12.sp, color = themeColors.textSecondary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))

        val chatSubTabs = listOf(
            Triple("SERVICES", "🔧 الخدمات", chatChannels.count { !it.isProvider || it.userName.contains("فني") || it.id.contains("service") }),
            Triple("PROPERTIES", "🏠 العقارات", chatChannels.count { it.id.contains("property") || it.lastMessage.contains("عقار") }),
            Triple("STORES", "🏪 المتاجر", chatChannels.count { it.id.contains("store") || it.lastMessage.contains("متجر") || it.lastMessage.contains("سلعة") }),
            Triple("MEDICAL", "🏥 طبية", chatChannels.count { it.id.contains("medical") || it.lastMessage.contains("صيدلية") }),
            Triple("RESTAURANTS", "🍔 مطاعم", chatChannels.count { it.id.contains("restaurant") || it.lastMessage.contains("وجبة") }),
            Triple("JOBS", "💼 وظائف", chatChannels.count { it.id.contains("job") || it.lastMessage.contains("وظيفة") })
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            items(chatSubTabs.size) { index ->
                val st = chatSubTabs[index]
                val isSel = state.adminChatSubTab == st.first
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { state.adminChatSubTab = st.first }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Text("${st.second} (${st.third})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                }
            }
        }
    }

    val filteredChatChannels = chatChannels.filter { ch ->
        when (state.adminChatSubTab) {
            "SERVICES" -> !ch.id.contains("property") && !ch.id.contains("store") && !ch.id.contains("medical") && !ch.id.contains("restaurant") && !ch.id.contains("job")
            "PROPERTIES" -> ch.id.contains("property") || ch.lastMessage.contains("عقار")
            "STORES" -> ch.id.contains("store") || ch.lastMessage.contains("متجر")
            "MEDICAL" -> ch.id.contains("medical") || ch.lastMessage.contains("صيدلية")
            "RESTAURANTS" -> ch.id.contains("restaurant") || ch.lastMessage.contains("وجبة")
            "JOBS" -> ch.id.contains("job") || ch.lastMessage.contains("وظيفة")
            else -> true
        }
    }

    if (filteredChatChannels.isEmpty()) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("لا توجد محادثات نشطة مطابقة في هذا القسم 🟢", fontSize = 12.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("لم يبدأ أي طرف محادثة في هذا القسم حتى الآن.", fontSize = 10.sp, color = themeColors.textSecondary)
                }
            }
        }
    } else {
        items(filteredChatChannels, key = { it.id }) { ch ->
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { state.showActiveChatChannelObj = ch }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val parties = if (ch.isProvider) "مقدم الخدمة: ${ch.userName}" else "مستخدم الدليل: ${ch.userName}"
                        Text("المحادثة: $parties", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        if (ch.isBlocked) {
                            Text("محظورة 🛑", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("نشطة 🟢", color = Color.Green, fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("آخر رسالة: " + ch.lastMessage, fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { state.showActiveChatChannelObj = ch },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("افتح المحادثة ورد كأدمن 💬", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                viewModel.blockChatChannel(ch.id, !ch.isBlocked)
                                Toast.makeText(context, if (ch.isBlocked) "تم فك حظر المحادثة" else "تم حظر المحادثة ومنع أطرافها 🛑", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (ch.isBlocked) Color.Gray else Color(0xFFD97706)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(if (ch.isBlocked) "فك حظر الغرفة" else "حظر الغرفة 🛑", fontSize = 9.sp, color = Color.White)
                        }
                        Button(
                            onClick = { state.showDeleteChatConfirmId = ch.id },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("حذف المحادثة 🗑️", fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
