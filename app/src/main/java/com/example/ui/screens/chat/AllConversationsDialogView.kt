@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.chat
import android.content.Intent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import okhttp3.MediaType.Companion.toMediaType

import com.example.*

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.utils.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.home.*
import com.example.ui.screens.map.*
import com.example.ui.screens.bookings.*
import com.example.ui.screens.admin.*
import com.example.ui.screens.assistant.*
import com.example.ui.screens.register.*
import com.example.ui.screens.status.*
import com.example.ui.screens.about.*
import com.example.ui.*
import com.example.ui.utils.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun AllConversationsDialogView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    initialSelectedChannelId: String? = null,
    onReadTrigger: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val chatChannels by viewModel.chatChannels.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()

    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()

    val myProvider = providers.find { it.phone == currentUserPhone || it.id == currentUserId }
    val myStore = stores.find { it.phone == currentUserPhone || it.id == currentUserId }
    val myProperty = properties.find { it.phone == currentUserPhone || it.id == currentUserId }
    val isBusinessOwner = myProvider != null || myStore != null || myProperty != null
    val isAdmin = currentUserId == "admin" || currentUserId.startsWith("super_") || adminRole != "GUEST"

    var selectedSectionFilter by remember { mutableStateOf("ALL") } // ALL, PROVIDER, STORE, RESTAURANT, MEDICAL, PROPERTY, JOB, SUPPORT

    val myChannels = remember(chatChannels, currentUserId, currentUserPhone, myProvider, myStore, myProperty, isAdmin, selectedSectionFilter) {
        val baseList = if (isAdmin) {
            chatChannels
        } else {
            val storeId = myStore?.id ?: (viewModel.selectedStore?.id ?: "")
            val propId = myProperty?.id ?: (viewModel.selectedProperty?.id ?: "")
            val prvId = myProvider?.id ?: ""
            val userPhoneClean = currentUserPhone.trim()
            chatChannels.filter { ch ->
                ch.id == "support_$currentUserId" ||
                ch.id == "support_$userPhoneClean" ||
                ch.id.contains(currentUserId) ||
                (userPhoneClean.isNotEmpty() && ch.id.contains(userPhoneClean)) ||
                (prvId.isNotEmpty() && (ch.id.contains("chat_p_${prvId}_") || ch.id.contains("_u_${prvId}") || ch.targetId == prvId)) ||
                (storeId.isNotEmpty() && (ch.id.contains(storeId) || ch.targetId == storeId)) ||
                (propId.isNotEmpty() && (ch.id.contains(propId) || ch.targetId == propId)) ||
                ch.customerId == currentUserId ||
                ch.customerPhone == userPhoneClean
            }
        }

        if (selectedSectionFilter == "ALL") {
            baseList
        } else {
            baseList.filter { ch ->
                val type = ch.channelType.uppercase()
                when (selectedSectionFilter) {
                    "PROVIDER" -> type == "PROVIDER" || ch.id.startsWith("chat_p_")
                    "STORE" -> type == "STORE" || ch.id.contains("store")
                    "RESTAURANT" -> type == "RESTAURANT" || ch.id.contains("restaurant")
                    "MEDICAL" -> type == "MEDICAL" || ch.id.contains("medical")
                    "PROPERTY" -> type == "PROPERTY" || ch.id.contains("property")
                    "JOB" -> type == "JOB" || ch.id.contains("job")
                    "SUPPORT" -> type == "ADMIN" || type == "SUPERVISOR" || ch.id.startsWith("support_")
                    else -> true
                }
            }
        }
    }

    var selectedChannelId by remember { mutableStateOf<String?>(initialSelectedChannelId) }
    val selectedChannel = remember(chatChannels, selectedChannelId) {
        chatChannels.find { it.id == selectedChannelId }
    }
    var replyText by remember { mutableStateOf("") }
    val sharedPrefs = remember(context) { context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE) }

    LaunchedEffect(selectedChannelId, chatChannels) {
        selectedChannelId?.let { chId ->
            val ch = chatChannels.find { it.id == chId }
            val lastMsg = ch?.messages?.lastOrNull()
            if (lastMsg != null) {
                val isMe = lastMsg.senderId == currentUserId || (myProvider != null && lastMsg.senderId == myProvider.id)
                if (!isMe) {
                    sharedPrefs.edit().putLong("chat_read_$chId", System.currentTimeMillis()).apply()
                    onReadTrigger()
                }
            }
        }
    }

    // Auto mark messages as read when opening channel
    LaunchedEffect(selectedChannelId) {
        selectedChannelId?.let { chId ->
            viewModel.markChatMessagesAsRead(chId, currentUserId)
        }
    }

    // Media attachment pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val ch = selectedChannel
            if (ch != null) {
                val currentImagesCount = ch.messages.count { it.imageUrl.isNotEmpty() && !it.imageUrl.contains("video_") }
                if (currentImagesCount >= settingsState.maxImagesPerChat) {
                    Toast.makeText(context, "⚠️ تجاوزت الحد الأقصى للصور (${settingsState.maxImagesPerChat})", Toast.LENGTH_LONG).show()
                } else {
                    val senderName = currentUserName.ifEmpty { myProvider?.name ?: "مستخدم" }
                    val senderId = myProvider?.id ?: currentUserId
                    Toast.makeText(context, "⏳ جاري رفع الصورة والمزامنة...", Toast.LENGTH_SHORT).show()
                    viewModel.uploadChatMediaToStorage(uri, isVideo = false) { uploadedUrl ->
                        viewModel.replyToChatChannel(ch.id, senderId, "[صورة مرفقة]", senderName, imageUrl = uploadedUrl)
                        onReadTrigger()
                        Toast.makeText(context, "📷 تم إرسال الصورة بنجاح!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val ch = selectedChannel
            if (ch != null) {
                val currentVideosCount = ch.messages.count { it.imageUrl.contains("video_") || it.message == "[فيديو مرفق]" }
                if (currentVideosCount >= settingsState.maxVideosPerChat) {
                    Toast.makeText(context, "⚠️ تجاوزت الحد الأقصى للفيديوهات (${settingsState.maxVideosPerChat})", Toast.LENGTH_LONG).show()
                } else {
                    val senderName = currentUserName.ifEmpty { myProvider?.name ?: "مستخدم" }
                    val senderId = myProvider?.id ?: currentUserId
                    Toast.makeText(context, "⏳ جاري رفع الفيديو والمزامنة...", Toast.LENGTH_SHORT).show()
                    viewModel.uploadChatMediaToStorage(uri, isVideo = true) { uploadedUrl ->
                        viewModel.replyToChatChannel(ch.id, senderId, "[فيديو مرفق]", senderName, imageUrl = if (uploadedUrl.startsWith("video_")) uploadedUrl else "video_$uploadedUrl")
                        onReadTrigger()
                        Toast.makeText(context, "🎥 تم إرسال الفيديو بنجاح!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Checking if the chat is disabled by Admin
    val chatDisabledReason = remember(settingsState, currentUserId, myProvider, selectedChannelId) {
        val chId = selectedChannelId ?: ""
        val isCustomerProviderChat = chId.startsWith("chat_p_") || (chId.startsWith("chat_") && !chId.startsWith("support_"))
        if (settingsState.disableChatAll) {
            "⚠️ الدردشة متوقفة حالياً للصيانة بقرار الإدارة: " + settingsState.chatDisabledAnnouncement
        } else if (settingsState.disableChatUsers && myProvider == null && currentUserId != "admin" && !currentUserId.startsWith("super_")) {
            "⚠️ تم تعطيل الدردشة للعملاء والزائرين حالياً."
        } else if (settingsState.disableChatProviders && myProvider != null) {
            "⚠️ تم تعطيل الدردشة للفنيين ومزودي الخدمة حالياً."
        } else if (isCustomerProviderChat && !settingsState.allowChatUserToProvider) {
            "⚠️ تم إيقاف الدردشة المباشرة بين العملاء والفنيين."
        } else {
            null
        }
    }

    // Dynamic Header Title
    val titleText = remember(selectedChannelId, chatChannels, providers, stores, properties, currentUserId, currentUserPhone, myProvider, myStore, myProperty, settingsState) {
        val ch = chatChannels.find { it.id == selectedChannelId }
        if (ch == null) {
            "💬 كل محادثاتك المباشرة"
        } else {
            if (ch.id.startsWith("support_")) {
                "💬 الدعم الفني المباشر والسرع"
            } else if (ch.id.startsWith("chat_p_") || (ch.id.startsWith("chat_") && !ch.id.startsWith("support_"))) {
                val providerId = if (ch.id.startsWith("chat_p_")) {
                    ch.id.substringAfter("chat_p_").substringBefore("_u_")
                } else {
                    val parts = ch.id.removePrefix("chat_").split("_")
                    if (parts.size >= 2) parts[0] else ""
                }
                val customerPhoneFromId = if (ch.id.startsWith("chat_p_")) {
                    ch.id.substringAfter("_u_")
                } else {
                    val parts = ch.id.removePrefix("chat_").split("_")
                    if (parts.size >= 2) parts[1] else ""
                }
                val providerObj = providers.find { it.id == providerId || it.phone == providerId }
                val isMeProvider = (myProvider != null && (myProvider.id == providerId || myProvider.phone == providerId)) ||
                                   (myStore != null && (myStore.id == providerId || myStore.phone == providerId)) ||
                                   (myProperty != null && (myProperty.id == providerId || myProperty.phone == providerId))

                if (isMeProvider) {
                    val rawCustomerName = ch.customerName.ifEmpty { 
                        ch.userName.substringAfter("مع ").ifEmpty { 
                            if (ch.messages.any { it.senderId != providerId }) {
                                ch.messages.firstOrNull { it.senderId != providerId }?.senderName ?: "العميل"
                            } else "العميل"
                        } 
                    }
                    val custPhone = ch.customerPhone.ifEmpty { customerPhoneFromId }
                    val custId = ch.customerId.ifEmpty { if (custPhone.isNotEmpty()) "USR-${custPhone.takeLast(6)}" else "USR-CLIENT" }
                    val label = when (settingsState.chatDisplayIdentityMode) {
                        "NAME_ONLY" -> rawCustomerName
                        "NAME_AND_ID" -> "$rawCustomerName ($custId)"
                        "PHONE_ONLY" -> custPhone.ifEmpty { rawCustomerName }
                        else -> "$rawCustomerName ${if (custPhone.isNotEmpty()) "($custPhone)" else ""}"
                    }
                    "👤 العميل: $label"
                } else {
                    val pName = providerObj?.name ?: ch.targetName.ifEmpty { ch.userName.substringAfter("دردشة: ").substringBefore(" مع").ifEmpty { "الفني / المتجر" } }
                    val pPhone = providerObj?.phone ?: providerId
                    val pId = providerObj?.id ?: "PRV-001"
                    val label = when (settingsState.chatDisplayIdentityMode) {
                        "NAME_ONLY" -> pName
                        "NAME_AND_ID" -> "$pName ($pId)"
                        "PHONE_ONLY" -> pPhone.ifEmpty { pName }
                        else -> "$pName ${if (pPhone.isNotEmpty()) "($pPhone)" else ""}"
                    }
                    "👷 الفني: $label"
                }
            } else {
                ch.userName
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, themeColors.accent),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 20.dp)
                .systemBarsPadding()
                .imePadding()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                var activeVoiceCall by remember { mutableStateOf<Pair<String, String>?>(null) }

                if (activeVoiceCall != null) {
                    com.example.ui.dialogs.InAppVoiceCallDialog(
                        callerName = activeVoiceCall!!.first,
                        callerRole = activeVoiceCall!!.second,
                        onDismiss = { activeVoiceCall = null },
                        themeColors = themeColors
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = titleText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (selectedChannelId != null && !settingsState.disableVoiceCalls) {
                            Button(
                                onClick = {
                                    val cleanTitle = titleText.removePrefix("👤 العميل: ").removePrefix("👷 الفني: ").removePrefix("💬 ")
                                    activeVoiceCall = Pair(cleanTitle, "مكالمة صوتية مباشرة HD")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("🎙️ اتصال", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        IconButton(onClick = { if (selectedChannelId != null) selectedChannelId = null else onDismiss() }, modifier = Modifier.size(28.dp)) {
                            Text(if (selectedChannelId != null) "🔙" else "❌", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 8.dp))

                // Section Filter Chips for Chats
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterOptions = listOf(
                        Pair("ALL", "الكل 🌐"),
                        Pair("PROVIDER", "الفنيين 🛠️"),
                        Pair("STORE", "المتاجر 🏪"),
                        Pair("RESTAURANT", "المطاعم 🍔"),
                        Pair("MEDICAL", "الطبية 🏥"),
                        Pair("PROPERTY", "العقارات 🏠"),
                        Pair("JOB", "الوظائف 💼"),
                        Pair("SUPPORT", "الدعم 💬")
                    )
                    items(filterOptions) { opt ->
                        val isSelected = selectedSectionFilter == opt.first
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSectionFilter = opt.first },
                            label = { Text(opt.second, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColors.accent,
                                selectedLabelColor = Color.Black,
                                containerColor = Color.White.copy(alpha = 0.08f),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                if (selectedChannelId == null) {
                    if (myChannels.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("💬", fontSize = 36.sp)
                                Text("لا توجد محادثات نشطة حالياً.", fontSize = 12.sp, color = Color.Gray)
                                Button(
                                    onClick = {
                                        onDismiss()
                                        viewModel.navigateTo("USER_BROWSE")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                                ) {
                                    Text("ابدأ محادثة مع الدعم الفني", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            myChannels.forEach { ch ->
                                                viewModel.deleteChatChannel(ch.id)
                                            }
                                            viewModel.triggerNotification("🗑️ تم حذف جميع المحادثات بنجاح.")
                                            onReadTrigger()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("حذف جميع المحادثات 🗑️", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            items(myChannels) { ch ->
                                val displayItemTitle = remember(ch, providers, stores, properties, currentUserId, currentUserPhone, myProvider, myStore, myProperty, settingsState) {
                                    if (ch.id.startsWith("support_")) {
                                        "💬 الدعم الفني المباشر"
                                    } else if (ch.id.startsWith("chat_p_") || (ch.id.startsWith("chat_") && !ch.id.startsWith("support_"))) {
                                        val providerId = if (ch.id.startsWith("chat_p_")) {
                                            ch.id.substringAfter("chat_p_").substringBefore("_u_")
                                        } else {
                                            val parts = ch.id.removePrefix("chat_").split("_")
                                            if (parts.size >= 2) parts[0] else ""
                                        }
                                        val customerPhoneFromId = if (ch.id.startsWith("chat_p_")) {
                                            ch.id.substringAfter("_u_")
                                        } else {
                                            val parts = ch.id.removePrefix("chat_").split("_")
                                            if (parts.size >= 2) parts[1] else ""
                                        }
                                        val providerObj = providers.find { it.id == providerId || it.phone == providerId }
                                        val isMeProvider = (myProvider != null && (myProvider.id == providerId || myProvider.phone == providerId)) ||
                                                           (myStore != null && (myStore.id == providerId || myStore.phone == providerId)) ||
                                                           (myProperty != null && (myProperty.id == providerId || myProperty.phone == providerId))

                                        if (isMeProvider) {
                                            val rawCustomerName = ch.customerName.ifEmpty { 
                                                ch.userName.substringAfter("مع ").ifEmpty { 
                                                    if (ch.messages.any { it.senderId != providerId }) {
                                                        ch.messages.firstOrNull { it.senderId != providerId }?.senderName ?: "العميل"
                                                    } else "العميل"
                                                } 
                                            }
                                            val custPhone = ch.customerPhone.ifEmpty { customerPhoneFromId }
                                            val custId = ch.customerId.ifEmpty { if (custPhone.isNotEmpty()) "USR-${custPhone.takeLast(6)}" else "USR-CLIENT" }
                                            val label = when (settingsState.chatDisplayIdentityMode) {
                                                "NAME_ONLY" -> rawCustomerName
                                                "NAME_AND_ID" -> "$rawCustomerName ($custId)"
                                                "PHONE_ONLY" -> custPhone.ifEmpty { rawCustomerName }
                                                else -> "$rawCustomerName ${if (custPhone.isNotEmpty()) "($custPhone)" else ""}"
                                            }
                                            "👤 العميل: $label"
                                        } else {
                                            val pName = providerObj?.name ?: ch.targetName.ifEmpty { ch.userName.substringAfter("دردشة: ").substringBefore(" مع").ifEmpty { "الفني / المتجر" } }
                                            val pPhone = providerObj?.phone ?: providerId
                                            val pId = providerObj?.id ?: "PRV-001"
                                            val label = when (settingsState.chatDisplayIdentityMode) {
                                                "NAME_ONLY" -> pName
                                                "NAME_AND_ID" -> "$pName ($pId)"
                                                "PHONE_ONLY" -> pPhone.ifEmpty { pName }
                                                else -> "$pName ${if (pPhone.isNotEmpty()) "($pPhone)" else ""}"
                                            }
                                            "👷 الفني: $label"
                                        }
                                    } else {
                                        ch.userName
                                    }
                                }

                                val unreadThisChat = remember(ch, chatChannels) {
                                    val lastMsg = ch.messages.lastOrNull()
                                    if (lastMsg == null) {
                                        false
                                    } else {
                                        val isMe = lastMsg.senderId == currentUserId || (myProvider != null && lastMsg.senderId == myProvider.id)
                                        val readTime = sharedPrefs.getLong("chat_read_${ch.id}", 0L)
                                        !isMe && lastMsg.timestamp > readTime
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (unreadThisChat) themeColors.accent.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)),
                                    border = BorderStroke(if (unreadThisChat) 1.dp else 0.5.dp, if (unreadThisChat) themeColors.accent else Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f).clickable { selectedChannelId = ch.id },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("💬", fontSize = 18.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(displayItemTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (unreadThisChat) themeColors.accent else Color.White)
                                                Text(ch.lastMessage, fontSize = 10.sp, color = if (unreadThisChat) themeColors.accent.copy(alpha = 0.8f) else Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteChatChannel(ch.id)
                                                onReadTrigger()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("🗑️", fontSize = 14.sp)
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(
                                            text = "◀️",
                                            fontSize = 11.sp,
                                            color = themeColors.accent,
                                            modifier = Modifier.clickable { selectedChannelId = ch.id }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val ch = selectedChannel
                    if (ch == null) {
                        selectedChannelId = null
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(ch.messages) { msg ->
                                        val isMe = msg.senderId == currentUserId || msg.senderId == myProvider?.id
                                        val alignment = if (isMe) Alignment.End else Alignment.Start
                                        val bubbleBg = if (isMe) themeColors.primary else Color(0xFF1E293B)
                                        
                                        val timeFormatted = remember(msg.timestamp) {
                                            if (msg.timestamp > 0) {
                                                try {
                                                    java.text.SimpleDateFormat("hh:mm a", java.util.Locale("ar")).format(java.util.Date(msg.timestamp))
                                                } catch(e: Exception) { "" }
                                            } else ""
                                        }

                                        Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(bubbleBg)
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    if (msg.imageUrl.startsWith("video_")) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(150.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(Color.Black)
                                                                .clickable {
                                                                    Toast.makeText(context, "▶️ جاري تشغيل الفيديو المرفق...", Toast.LENGTH_SHORT).show()
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.PlayArrow,
                                                                contentDescription = "تشغيل الفيديو",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(48.dp)
                                                            )
                                                            Text(
                                                                "تشغيل الفيديو 🎥",
                                                                fontSize = 10.sp,
                                                                color = Color.White,
                                                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                                                            )
                                                        }
                                                    } else if (msg.imageUrl.isNotEmpty()) {
                                                        AsyncImage(
                                                            model = msg.imageUrl,
                                                            contentDescription = "صورة مرفقة",
                                                            modifier = Modifier.size(150.dp).clip(RoundedCornerShape(8.dp))
                                                        )
                                                    }
                                                    if (msg.message.isNotEmpty() && msg.message != "[صورة مرفقة]" && msg.message != "[فيديو مرفق]") {
                                                        Text(msg.message, fontSize = 13.sp, color = Color.White, lineHeight = 19.sp)
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.End,
                                                        modifier = Modifier.align(Alignment.End)
                                                    ) {
                                                        if (timeFormatted.isNotEmpty()) {
                                                            Text(timeFormatted, fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                                                        }
                                                        if (isMe) {
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            val (statusIcon, statusColor) = when (msg.status) {
                                                                "READ" -> "✔✔" to Color(0xFF38BDF8)
                                                                "DELIVERED" -> "✔✔" to Color.LightGray
                                                                else -> "✔" to Color.LightGray
                                                            }
                                                            Text(statusIcon, fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                            Text(msg.senderName, fontSize = 9.sp, color = themeColors.textSecondary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (replyText.isNotEmpty()) {
                                Text(
                                    text = "💬 جاري الكتابة...",
                                    fontSize = 10.sp,
                                    color = themeColors.accent,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (chatDisabledReason != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(chatDisabledReason, fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Image attachment trigger
                                    if (settingsState.allowSendImages) {
                                        IconButton(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    androidx.activity.result.PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Text("📷", fontSize = 18.sp)
                                        }
                                    }

                                    // Video attachment trigger
                                    if (settingsState.allowSendVideos) {
                                        IconButton(
                                            onClick = {
                                                videoPickerLauncher.launch(
                                                    androidx.activity.result.PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.VideoOnly
                                                    )
                                                )
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Text("🎥", fontSize = 18.sp)
                                        }
                                    }

                                    OutlinedTextField(
                                        value = replyText,
                                        onValueChange = { replyText = it },
                                        placeholder = { Text("اكتب رسالتك...", fontSize = 13.sp, color = Color.Gray) },
                                        modifier = Modifier.weight(1f),
                                        minLines = 1,
                                        maxLines = 4,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Color.White, lineHeight = 18.sp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = themeColors.accent,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                            focusedContainerColor = Color(0xFF1E293B),
                                            unfocusedContainerColor = Color(0xFF1E293B)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Button(
                                        onClick = {
                                            if (replyText.trim().isNotEmpty()) {
                                                val providerId = if (ch.id.startsWith("chat_p_")) {
                                                    ch.id.substringAfter("chat_p_").substringBefore("_u_")
                                                } else {
                                                    val parts = ch.id.removePrefix("chat_").split("_")
                                                    if (parts.size >= 2) parts[0] else ""
                                                }
                                                val isMeProvider = (myProvider != null && (myProvider.id == providerId || myProvider.phone == providerId)) ||
                                                                   (myStore != null && (myStore.id == providerId || myStore.phone == providerId)) ||
                                                                   (myProperty != null && (myProperty.id == providerId || myProperty.phone == providerId))

                                                val senderName = if (isMeProvider) {
                                                    myProvider?.name ?: (myStore?.name ?: (myProperty?.title ?: "الجهة الفنية"))
                                                } else {
                                                    currentUserName.ifEmpty { "عميل ($currentUserPhone)" }
                                                }
                                                val senderId = if (isMeProvider) {
                                                    myProvider?.id ?: (myStore?.id ?: (myProperty?.id ?: currentUserId))
                                                } else {
                                                    currentUserId
                                                }
                                                viewModel.replyToChatChannel(ch.id, senderId, replyText.trim(), senderName)
                                                
                                                replyText = ""
                                                onReadTrigger()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                        modifier = Modifier.height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("إرسال", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

