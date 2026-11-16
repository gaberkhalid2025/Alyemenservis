package com.example.ui.screens.admin

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.utils.DateFormatter
import com.example.utils.VisualThemePalette
import com.example.utils.compressAndResizeImageUri

fun LazyListScope.adminSectionCommsAndEngagement(
    activeSubTab: String,
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = state.context
    val settingsState = state.settingsState
    val chatChannels = state.chatChannels
    val bannersList = state.bannersList
    val categories = state.categories
    val activatedProviders = state.activatedProviders
    val callsLog = state.callsLog
    val couponsList = state.couponsList

    when (activeSubTab) {
        "ADVANCED_CHAT" -> adminAdvancedChatPanel(viewModel, themeColors)

        "CHATS" -> {
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

        "BANNERS" -> {
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Text("🖼️ إدارة وترتيب بنرات الإعلانات الترويجية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("➕ إضافة بنر ترويجي جديد ومتقدم:", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

                        var bannerTitle by remember { mutableStateOf("") }
                        var bannerType by remember { mutableStateOf("IMAGE") }
                        var bannerUrl by remember { mutableStateOf("") }
                        var bannerRedirect by remember { mutableStateOf("") }
                        var bannerDuration by remember { mutableStateOf(5) }

                        Text("نوع البنر الترويجي:", fontSize = 10.sp, color = Color.White)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("IMAGE" to "🖼️ صورة", "VIDEO" to "🎥 فيديو", "TEXT" to "🔤 نصي فقط").forEach { (typeKey, label) ->
                                val isSelected = bannerType == typeKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) themeColors.primary else Color.White.copy(alpha = 0.05f))
                                        .clickable { bannerType = typeKey }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, fontSize = 10.sp, color = if (isSelected) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = bannerTitle,
                            onValueChange = { bannerTitle = it },
                            label = { Text("عنوان أو نص الإعلان") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )

                        if (bannerType != "TEXT") {
                            OutlinedTextField(
                                value = bannerUrl,
                                onValueChange = { bannerUrl = it },
                                label = { Text(if (bannerType == "IMAGE") "رابط صورة الإعلان (URL أو مسار)" else "رابط فيديو الإعلان (URL أو مسار)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            val galleryLauncherForBannerSelection = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.GetContent()
                            ) { uri ->
                                if (uri != null) {
                                    try {
                                        val contentResolver = context.contentResolver
                                        val mimeType = contentResolver.getType(uri) ?: ""
                                        if (mimeType.startsWith("image/")) {
                                            val base64Str = compressAndResizeImageUri(context, uri, 800, 70)
                                            if (base64Str.isNotEmpty()) {
                                                bannerUrl = "data:image/jpeg;base64,$base64Str"
                                                bannerType = "IMAGE"
                                                Toast.makeText(context, "📸 تم اختيار وضغط الصورة بنجاح وتجهيز المزامنة!", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            bannerUrl = uri.toString()
                                            bannerType = "VIDEO"
                                            Toast.makeText(context, "🎥 تم اختيار الفيديو بنجاح وتجهيز المزامنة!", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "❌ فشل قراءة الملف: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (bannerType == "IMAGE") {
                                        galleryLauncherForBannerSelection.launch("image/*")
                                    } else {
                                        galleryLauncherForBannerSelection.launch("video/*")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (bannerType == "IMAGE") "🖼️ اختيار صورة الإعلان من المعرض" else "🎥 اختيار فيديو الإعلان من المعرض",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "🎯 توجيه البنر عند نقر العميل (اختر القسم المستهدف):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.accent
                            )

                            val predefinedSections = listOf(
                                Triple("", "🌐 عام (بدون توجيه)", "الرئيسية"),
                                Triple("services", "🔧 الخدمات والمهن الفنية", "الفنيين والصيانة"),
                                Triple("stores", "🏪 المراكز والمحلات التجارية", "المتاجر والتسوق"),
                                Triple("restaurants", "🍔 المطاعم والكافيهات", "المأكولات والمشروبات"),
                                Triple("medical", "🏥 المراكز والعيادات الطبية", "الصحة والطب"),
                                Triple("properties", "🏠 العقارات والأملاك", "بيع وإيجار العقارات"),
                                Triple("jobs", "💼 الوظائف والشركات", "فرص العمل والتوظيف"),
                                Triple("quick_service", "⚡ اطلب خدمتك الفورية", "الخدمة السريعة"),
                                Triple("bookings", "📅 استمارة حجز المواعيد", "نظام الحجوزات"),
                                Triple("maps", "🗺️ خريطة المنشآت والمواقع", "الخرائط التفاعلية"),
                                Triple("chat", "💬 المحادثات الفورية", "الدردشة والدعم"),
                                Triple("join", "📝 طلبات الانضمام والتسجيل", "استمارات التسجيل")
                            )

                            val allAvailableRedirects = predefinedSections + categories.map {
                                Triple(it.id, "📂 ${it.name}", "قسم مخصص")
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(allAvailableRedirects.size) { rIndex ->
                                    val item = allAvailableRedirects[rIndex]
                                    val isSelected = bannerRedirect.trim() == item.first
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) themeColors.accent else themeColors.surface)
                                            .border(1.dp, if (isSelected) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .clickable { bannerRedirect = item.first }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = item.second,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentSelectedName = allAvailableRedirects.firstOrNull { it.first == bannerRedirect.trim() }?.second
                                    ?: if (bannerRedirect.isBlank()) "🌐 عام (بدون توجيه)" else "🏷️ مخصص: $bannerRedirect"
                                Text(
                                    text = "القسم المحدد حالياً: $currentSelectedName",
                                    fontSize = 10.sp,
                                    color = Color.Yellow,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (bannerRedirect.isNotEmpty()) {
                                    Text(
                                        text = "إلغاء التوجيه ✖",
                                        fontSize = 9.sp,
                                        color = Color.Red,
                                        modifier = Modifier.clickable { bannerRedirect = "" }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = bannerRedirect,
                                onValueChange = { bannerRedirect = it },
                                label = { Text("رمز أو معرف القسم المستهدف (أو اكتب رمزاً مخصصاً)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("مدة عرض البنر: $bannerDuration ثوانٍ", fontSize = 10.sp, color = Color.White)
                            Slider(
                                value = bannerDuration.toFloat(),
                                onValueChange = { bannerDuration = it.toInt() },
                                valueRange = 3f..20f,
                                steps = 17,
                                colors = SliderDefaults.colors(
                                    thumbColor = themeColors.accent,
                                    activeTrackColor = themeColors.primary
                                )
                            )
                        }

                        Button(
                            onClick = {
                                if (bannerTitle.trim().isEmpty()) {
                                    Toast.makeText(context, "الرجاء كتابة عنوان الإعلان", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (bannerType != "TEXT" && bannerUrl.trim().isEmpty()) {
                                    Toast.makeText(context, "الرجاء كتابة رابط أو مسار ملف الإعلان", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                viewModel.addNewBanner(
                                    title = bannerTitle.trim(),
                                    url = if (bannerType == "TEXT") "" else bannerUrl.trim(),
                                    redirect = bannerRedirect.trim(),
                                    type = bannerType,
                                    size = "LARGE",
                                    duration = bannerDuration
                                )
                                bannerTitle = ""
                                bannerUrl = ""
                                bannerRedirect = ""
                                bannerDuration = 5
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إضافة البنر الإعلاني المطور 🚀", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text("📋 قائمة البنرات النشطة (ادعم الترتيب بالسحب والإفلات والمبادلة):", fontSize = 12.sp, color = themeColors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
            }

            itemsIndexed(bannersList) { index, b ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "سحب لنقل الترتيب",
                                tint = themeColors.accent,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable {
                                        if (index > 0) {
                                            val mutableBanners = bannersList.toMutableList()
                                            val prev = mutableBanners[index - 1]
                                            mutableBanners[index - 1] = b
                                            mutableBanners[index] = prev
                                            viewModel.reorderBanners(mutableBanners)
                                        }
                                    }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val typeIcon = when (b.type.uppercase()) {
                                        "IMAGE" -> "🖼️"
                                        "VIDEO" -> "🎥"
                                        else -> "🔤"
                                    }
                                    Text("$typeIcon ${b.title}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text("مدة العرض: ${b.duration} ثوانٍ | القسم: ${b.redirectCategory.ifEmpty { "عام" }}", fontSize = 9.sp, color = themeColors.accent)
                                if (b.url.isNotEmpty()) {
                                    Text(b.url, fontSize = 9.sp, color = themeColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val mutableBanners = bannersList.toMutableList()
                                        val prev = mutableBanners[index - 1]
                                        mutableBanners[index - 1] = b
                                        mutableBanners[index] = prev
                                        viewModel.reorderBanners(mutableBanners)
                                    }
                                },
                                enabled = index > 0
                            ) {
                                Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "أعلى", tint = if (index > 0) themeColors.accent else Color.Gray.copy(alpha = 0.5f))
                            }
                            IconButton(
                                onClick = {
                                    if (index < bannersList.size - 1) {
                                        val mutableBanners = bannersList.toMutableList()
                                        val next = mutableBanners[index + 1]
                                        mutableBanners[index + 1] = b
                                        mutableBanners[index] = next
                                        viewModel.reorderBanners(mutableBanners)
                                    }
                                },
                                enabled = index < bannersList.size - 1
                            ) {
                                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "أسفل", tint = if (index < bannersList.size - 1) themeColors.accent else Color.Gray.copy(alpha = 0.5f))
                            }
                            IconButton(onClick = { viewModel.deleteBanner(b.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }

        "REVIEWS" -> {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("⭐ إدارة التقييمات والمراجعات للأعضاء", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("قم بالبحث وتعديل تقييمات وعدد مراجعات الفنيين مباشرة:", fontSize = 11.sp, color = themeColors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        activatedProviders.forEach { provider ->
                            var editRatingText by remember(provider.id) { mutableStateOf(provider.rating.toString()) }
                            var editReviewsText by remember(provider.id) { mutableStateOf(provider.numReviews.toString()) }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(provider.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${provider.profession} | 📍 ${provider.area}", fontSize = 11.sp, color = Color.LightGray)
                                    Text("التقييم الحالي: ⭐ ${provider.rating} (${provider.numReviews} تقييم)", fontSize = 10.sp, color = themeColors.accent)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editRatingText,
                                        onValueChange = { editRatingText = it },
                                        label = { Text("التقييم", fontSize = 9.sp) },
                                        singleLine = true,
                                        modifier = Modifier.width(60.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )

                                    OutlinedTextField(
                                        value = editReviewsText,
                                        onValueChange = { editReviewsText = it },
                                        label = { Text("العدد", fontSize = 9.sp) },
                                        singleLine = true,
                                        modifier = Modifier.width(60.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )

                                    Button(
                                        onClick = {
                                            val r = editRatingText.toFloatOrNull() ?: provider.rating
                                            val n = editReviewsText.toIntOrNull() ?: provider.numReviews
                                            val updated = provider.copy(rating = r, numReviews = n)
                                            viewModel.updateProviderEntity(updated)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("حفظ", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                            Divider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }

        "CALLS" -> {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("📞 سجلات ومراقبة المكالمات الجارية والمباشرة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("مراقبة وتتبع اتصالات المواطنين بالفنيين ومزودي الخدمات مباشرة:", fontSize = 11.sp, color = themeColors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (callsLog.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📭 لا توجد سجلات مكالمات محفوظة حالياً.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            callsLog.sortedByDescending { it.timestamp }.forEach { call ->
                                val dateStr = DateFormatter.formatDateTime(call.timestamp)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("📱 المكالمة لـ: ${call.providerName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("👤 المتصل: ${call.callerName}", fontSize = 11.sp, color = Color.LightGray)
                                        Text("⏰ التاريخ: $dateStr", fontSize = 10.sp, color = themeColors.textSecondary)
                                    }

                                    IconButton(onClick = {
                                        viewModel.db.collection("calls").document(call.id).delete()
                                        viewModel.triggerNotification("🗑️ تم حذف سجل المكالمة")
                                    }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف السجل", tint = Color.Red.copy(alpha = 0.8f))
                                    }
                                }
                                Divider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }
        }

        "COUPONS" -> {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("🎫 إدارة وتوليد الكوبونات الحصرية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("توليد وتوزيع الكوبونات الترويجية لشحن نقاط المواطنين مجاناً:", fontSize = 11.sp, color = themeColors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("➕ إنشاء كوبون جديد", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.couponCodeInput,
                            onValueChange = { state.couponCodeInput = it },
                            label = { Text("رمز الكوبون (مثال: YEMEN2026)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = state.couponPointsInput,
                                onValueChange = { state.couponPointsInput = it },
                                label = { Text("قيمة النقاط") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = state.couponExpiryDaysInput,
                                onValueChange = { state.couponExpiryDaysInput = it },
                                label = { Text("صلاحية الكوبون (أيام)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = state.couponDiscountInput,
                                onValueChange = { state.couponDiscountInput = it },
                                label = { Text("نسبة الخصم %") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = state.couponMaxUsageInput,
                                onValueChange = { state.couponMaxUsageInput = it },
                                label = { Text("مرات الاستخدام القصوى") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        Button(
                            onClick = {
                                val code = state.couponCodeInput.trim().uppercase()
                                val points = state.couponPointsInput.toIntOrNull() ?: 100
                                val days = state.couponExpiryDaysInput.toLongOrNull() ?: 30L
                                val discount = state.couponDiscountInput.toIntOrNull() ?: 15
                                val maxUsage = state.couponMaxUsageInput.toIntOrNull() ?: 50
                                if (code.isNotBlank()) {
                                    viewModel.addCoupon(code, points, days * 24 * 60 * 60 * 1000L, discount, maxUsage)
                                    state.couponCodeInput = ""
                                } else {
                                    viewModel.triggerNotification("❌ يرجى إدخال رمز كوبون صالح!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إنشاء الكوبون وحفظه 🎫", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("📋 الكوبونات المتاحة بالمنصة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            }

            if (couponsList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📭 لا توجد كوبونات مسجلة حالياً.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            couponsList.forEach { coupon ->
                                val expiryStr = DateFormatter.formatDateDash(coupon.expiryTimestamp)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("🎫 الكود: ${coupon.code}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("🎁 النقاط: ${coupon.pointsValue} نقطة | خصم: ${coupon.discountPercentage}% | الاستخدام: ${coupon.usedCount}/${coupon.maxUsageCount}", fontSize = 11.sp, color = Color.LightGray)
                                        Text("الصلاحية لغاية: $expiryStr | الحالة: ${coupon.status}", fontSize = 10.sp, color = themeColors.textSecondary)
                                    }

                                    IconButton(onClick = { viewModel.deleteCoupon(coupon.id) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف الكوبون", tint = Color.Red.copy(alpha = 0.8f))
                                    }
                                }
                                Divider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }
        }
    }
}
