package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.VisualThemePalette

@Composable
fun AppHeaderBar(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onNotificationsClick: () -> Unit,
    onChatsClick: () -> Unit,
    chatReadTrigger: Int = 0,
    onMenuClick: () -> Unit
) {
    val settingsState by viewModel.settings.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val allNotifications by viewModel.notifications.collectAsState()
    val userPhoneState by viewModel.currentUserPhone.collectAsState()
    val adminRoleState by viewModel.adminRole.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val chatChannels by viewModel.chatChannels.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isEn = currentLang == "en"

    val myProvider = providers.find { it.phone == currentUserPhone }

    val myChannels = remember(chatChannels, currentUserId, currentUserPhone, myProvider) {
        chatChannels.filter { ch ->
            ch.id == "support_$currentUserId" ||
            ch.id.contains(currentUserId) ||
            (currentUserPhone.isNotEmpty() && ch.id.contains(currentUserPhone)) ||
            (myProvider != null && (ch.id.contains("chat_p_${myProvider.id}_") || ch.id.contains("_u_${myProvider.id}"))) ||
            currentUserId == "admin" || currentUserId.startsWith("super_")
        }
    }

    val headerContext = LocalContext.current
    val headerSp = remember(headerContext) { headerContext.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE) }
    var headerReadIds by remember { mutableStateOf(headerSp.getStringSet("read_notif_ids", emptySet()) ?: emptySet()) }
    
    // Calculate unread notifications count
    val filteredNotifs = remember(allNotifications, userPhoneState, adminRoleState) {
        allNotifications.filter { notif ->
            when (notif.targetType) {
                "ALL" -> true
                "USER" -> notif.targetValue == userPhoneState
                "PROVIDER" -> notif.targetValue == userPhoneState
                "SUPERVISOR" -> adminRoleState != "GUEST"
                else -> true
            }
        }
    }
    val unreadNotifCount = remember(filteredNotifs, headerReadIds) {
        filteredNotifs.count { it.id !in headerReadIds }
    }

    // Calculate unread chats count
    val unreadChatsCount = remember(myChannels, chatChannels, headerSp, chatReadTrigger) {
        myChannels.count { ch ->
            val lastMsg = ch.messages.lastOrNull()
            if (lastMsg == null) {
                false
            } else {
                val isMe = lastMsg.senderId == currentUserId || (myProvider != null && lastMsg.senderId == myProvider.id)
                val readTime = headerSp.getLong("chat_read_${ch.id}", 0L)
                !isMe && lastMsg.timestamp > readTime
            }
        }
    }

    val screenBackStack by viewModel.screenBackStack.collectAsState()
    val showBackButton = screenBackStack.size > 1 || currentScreen != "USER_BROWSE"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(themeColors.primary)
            .statusBarsPadding()
            .testTag("app_header_bar")
    ) {
        // Row 1: Unified TopBar Header with Back Button (Controlled by Admin hideTopHeaderBar)
        if (!settingsState.hideTopHeaderBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = { viewModel.goBack() },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEn) "Back" else "رجوع",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                val defaultTitle = if (isEn) "Yemen Services Directory" else "دليل خدمات اليمن"
                val customTitle = settingsState.customAppName.ifEmpty { settingsState.appName.ifEmpty { defaultTitle } }

                val titleText = when (currentScreen) {
                    "REGISTER_FORM" -> if (isEn) "Join & Register" else "الانضمام والتسجيل"
                    "JOIN_REQUEST_STATUS" -> if (isEn) "Request Status" else "حالة طلب الانضمام"
                    "ADMIN_PANEL" -> if (isEn) "Admin Panel" else "لوحة التحكم والإدارة"
                    "OWNER_PANEL" -> if (isEn) "Owner Backdoor" else "البوابة الخلفية"
                    "ABOUT_APP" -> if (isEn) "About App" else "عن التطبيق"
                    "BOOKINGS_VIEW" -> if (isEn) "Bookings & Orders" else "الحجوزات والطلبات"
                    "MAP_VIEW" -> if (isEn) "Services Map" else "خريطة الخدمات"
                    else -> customTitle
                }

                Text(
                    text = titleText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.weight(1f))

                // [ طلباتي ] Icon Button in Header
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(themeColors.accent)
                        .clickable { viewModel.navigateTo("ORDERS_VIEW") }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("📋", fontSize = 12.sp)
                        val isProvider = viewModel.selectedProvider != null || viewModel.selectedStore != null || viewModel.selectedProperty != null
                        Text(
                            text = if (isEn) (if (isProvider) "Requests" else "My Requests") else (if (isProvider) "الطلبات" else "طلباتي"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                val isRefreshingHeader by viewModel.isRefreshing.collectAsState()
                IconButton(
                    onClick = { viewModel.refreshData() },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .size(32.dp)
                ) {
                    if (isRefreshingHeader) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (isEn) "Refresh" else "تحديث البيانات",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Row 2: Navigation Items (5 luxury 3D golden icons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1. الرئيسية
            val isBrowse = currentScreen == "USER_BROWSE" || currentScreen == "MAIN_DASHBOARD"
            Luxury3DNavIcon(
                emojiIcon = settingsState.topHomeIcon.ifEmpty { "🏠" },
                vectorIcon = Icons.Default.Home,
                label = if (isEn) "Home" else "الرئيسية",
                isSelected = isBrowse,
                iconSizeDp = settingsState.navIconSizeDp,
                iconStyle = settingsState.topNavIconStyle,
                onClick = {
                    viewModel.navigateTo("USER_BROWSE")
                    viewModel.registerBackdoorInteraction()
                }
            )

            // 2. الخرائط
            if (settingsState.isMapFeatureEnabled) {
                val isMap = currentScreen == "MAP_VIEW"
                Luxury3DNavIcon(
                    emojiIcon = settingsState.topMapsIcon.ifEmpty { "🗺️" },
                    vectorIcon = Icons.Default.Place,
                    label = if (isEn) "Maps" else "الخرائط",
                    isSelected = isMap,
                    iconSizeDp = settingsState.navIconSizeDp,
                    iconStyle = settingsState.topNavIconStyle,
                    onClick = { viewModel.navigateTo("MAP_VIEW") }
                )
            }

            // 3. الانضمام
            val isJoin = currentScreen == "REGISTER_FORM" || currentScreen == "JOIN_REQUEST_STATUS"
            Luxury3DNavIcon(
                emojiIcon = settingsState.topJoinIcon.ifEmpty { "👤" },
                vectorIcon = Icons.Default.Person,
                label = if (isEn) "Join" else "الانضمام",
                isSelected = isJoin,
                iconSizeDp = settingsState.navIconSizeDp,
                iconStyle = settingsState.topNavIconStyle,
                onClick = { viewModel.navigateTo("REGISTER_FORM") }
            )

            // 4. الإشعارات
            Luxury3DNavIcon(
                emojiIcon = settingsState.topNotifIcon.ifEmpty { "🔔" },
                vectorIcon = Icons.Default.Notifications,
                label = if (isEn) "Alerts" else "الإشعارات",
                isSelected = unreadNotifCount > 0,
                badgeCount = unreadNotifCount,
                iconSizeDp = settingsState.navIconSizeDp,
                iconStyle = settingsState.topNavIconStyle,
                onClick = {
                    val allIds = filteredNotifs.map { it.id }.toSet()
                    headerSp.edit().putStringSet("read_notif_ids", allIds).apply()
                    headerReadIds = allIds
                    onNotificationsClick()
                }
            )

            // 5. المحادثات
            val hasUnreadChats = unreadChatsCount > 0
            Luxury3DNavIcon(
                emojiIcon = settingsState.topChatsIcon.ifEmpty { "✉️" },
                vectorIcon = Icons.Default.Email,
                label = if (isEn) "Chats" else "المحادثات",
                isSelected = hasUnreadChats,
                badgeCount = unreadChatsCount,
                iconSizeDp = settingsState.navIconSizeDp,
                iconStyle = settingsState.topNavIconStyle,
                onClick = { onChatsClick() }
            )
        }
    }
}
