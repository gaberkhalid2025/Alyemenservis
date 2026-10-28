package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import com.example.ui.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.TajawalFontFamily
import com.example.utils.VisualThemePalette

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

    val cleanUserId = currentUserId.trim()
    val cleanUserPhone = currentUserPhone.trim().replace(" ", "").replace("+", "")
    val isAdminUser = cleanUserId == "admin" || cleanUserId.startsWith("super_") || adminRoleState != "GUEST"

    val myChannels = remember(chatChannels, cleanUserId, cleanUserPhone, myProvider, isAdminUser) {
        if (isAdminUser) {
            chatChannels
        } else if (cleanUserId.isEmpty() && cleanUserPhone.isEmpty()) {
            emptyList()
        } else {
            chatChannels.filter { ch ->
                val isMySupport = ch.id.startsWith("support_") || (cleanUserId.isNotEmpty() && ch.participants.contains(cleanUserId))
                val isMyUser = cleanUserId.isNotEmpty() && ch.participants.contains(cleanUserId)
                val isMyProvider = myProvider != null && ch.participants.contains(myProvider.id)

                isMySupport || isMyUser || isMyProvider
            }
        }
    }

    val headerContext = LocalContext.current
    val headerSp = remember(headerContext) { headerContext.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE) }
    val readNotificationIds by viewModel.readNotificationIds.collectAsState()
    
    // Calculate unread notifications count
    val filteredNotifs = remember(allNotifications, userPhoneState, adminRoleState) {
        val cleanPhone = userPhoneState.trim().replace(" ", "").replace("+", "")
        allNotifications.filter { notif ->
            // Filter sensitive notifications (password recovery, private orders)
            val isSensitive = notif.title.contains("كلمة مرور") || notif.message.contains("كلمة المرور") || notif.title.contains("استعادة") || notif.title.contains("طلب عاجل")
            if (isSensitive) {
                val isAdmin = adminRoleState != "GUEST"
                val isMyTarget = cleanPhone.isNotEmpty() && notif.targetValue.contains(cleanPhone)
                if (!isAdmin && !isMyTarget) return@filter false
            }

            when (notif.targetType) {
                "ALL" -> notif.targetAudience != "ADMIN_ONLY"
                "USER", "PROVIDER" -> notif.targetValue.isEmpty() || (cleanPhone.isNotEmpty() && notif.targetValue.contains(cleanPhone))
                "SUPERVISOR", "ADMIN_ONLY" -> adminRoleState != "GUEST"
                else -> true
            }
        }
    }
    val unreadNotifCount = remember(filteredNotifs, readNotificationIds) {
        if (filteredNotifs.isEmpty()) 0 else filteredNotifs.count { it.id !in readNotificationIds }
    }

    // Calculate unread chats count (تفضيل unreadCount من ChatChannel أولاً ثم fallback لـ SharedPreferences)
    val unreadChatsCount = remember(myChannels, chatChannels, headerSp, chatReadTrigger) {
        if (myChannels.isEmpty()) 0 else myChannels.count { ch ->
            val channelUnread = ch.unreadCount[currentUserId] ?: 0
            if (channelUnread > 0) {
                true
            } else {
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
    }

    val showBackButton = currentScreen != AppScreens.USER_BROWSE && currentScreen != AppScreens.HOME

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xE60B1120)) // Frosted-glass background #0B1120 at 90% opacity
            .statusBarsPadding()
            .testTag("app_header_bar")
    ) {
        // Row 1: Top Bar with Location, Back, Title, and "طلباتي" Far End Action Button
        if (!settingsState.hideTopHeaderBar) {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showBackButton) {
                        IconButton(
                            onClick = { viewModel.goBack() },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF141A2E), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                .shadow(2.dp, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    } else {
                        // Location Pill (صنعاء ▾)
                        Surface(
                            onClick = { /* Select City */ },
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF141A2E),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = Color(0xFF06B6D4), modifier = Modifier.size(13.dp))
                                Text("صنعاء", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = TajawalFontFamily, color = Color.White)
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    val titleText = when (currentScreen) {
                        "REGISTER_FORM" -> if (isEn) "Join" else "الانضمام"
                        "ADMIN_PANEL" -> if (isEn) "Admin" else "الإدارة"
                        "ABOUT_APP" -> if (isEn) "About" else "عن التطبيق"
                        "BOOKINGS_VIEW" -> if (isEn) "Bookings" else "الحجوزات"
                        "MAP_VIEW" -> if (isEn) "Map & Radar" else "الخرائط والرادار"
                        else -> settingsState.customAppName.ifEmpty { "دليل خدمات اليمن" }
                    }

                    Text(
                        text = titleText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = CairoFontFamily,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    // Far End Action Button: "طلباتي" (My Orders) pill button with clipboard icon, elevated dark-glass background, and 1px border
                    Surface(
                        onClick = { viewModel.navigateTo("ORDERS_VIEW") },
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFF141A2E), // Elevated dark-glass background
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Assignment, contentDescription = "طلباتي", tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                            Text(
                                text = if (isEn) "Orders" else "طلباتي",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = CairoFontFamily,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(6.dp))

                    // Refresh Button
                    val isRefreshingHeader by viewModel.isRefreshing.collectAsState()
                    IconButton(
                        onClick = { viewModel.refreshData(showNotification = true) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        if (isRefreshingHeader) {
                            CircularProgressIndicator(color = Color(0xFF06B6D4), strokeWidth = 2.dp, modifier = Modifier.size(13.dp))
                        } else {
                            Icon(Icons.Default.Refresh, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Row 2: Navigation Items (Exact 5 Navigation Icons in Elevated Charcoal-Navy Glass Container)
        Surface(
            color = Color(0xFF141A2E), // Elevated Charcoal-Navy
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 3.dp),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. الرئيسية
                val isBrowse = currentScreen == AppScreens.USER_BROWSE || currentScreen == "HOME"
                Luxury3DNavIcon(
                    emojiIcon = "🏠",
                    vectorIcon = Icons.Default.Home,
                    label = if (isEn) "Home" else "الرئيسية",
                    isSelected = isBrowse,
                    onClick = { viewModel.navigateToScreen(AppScreens.USER_BROWSE) }
                )

                // 2. الخرائط
                val isMap = currentScreen == AppScreens.MAP_VIEW
                Luxury3DNavIcon(
                    emojiIcon = "🗺️",
                    vectorIcon = Icons.Default.Map,
                    label = if (isEn) "Maps" else "الخرائط",
                    isSelected = isMap,
                    onClick = { viewModel.navigateToScreen(AppScreens.MAP_VIEW) }
                )

                // 3. الانضمام
                val isJoin = currentScreen == AppScreens.REGISTER_FORM || currentScreen == AppScreens.JOIN_REQUEST_STATUS
                Luxury3DNavIcon(
                    emojiIcon = "👤",
                    vectorIcon = Icons.Default.Person,
                    label = if (isEn) "Join" else "الانضمام",
                    isSelected = isJoin,
                    onClick = { viewModel.navigateToScreen(AppScreens.REGISTER_FORM) }
                )

                // 4. الإشعارات
                val isNotifScreen = currentScreen == AppScreens.NOTIFICATIONS_VIEW
                Luxury3DNavIcon(
                    emojiIcon = "🔔",
                    vectorIcon = Icons.Default.Notifications,
                    label = if (isEn) "Alerts" else "الإشعارات",
                    isSelected = isNotifScreen,
                    badgeCount = unreadNotifCount,
                    onClick = { onNotificationsClick() }
                )

                // 5. المحادثات
                val isChatsScreen = currentScreen == AppScreens.CHAT_LIST || currentScreen == AppScreens.CHAT_DIRECT
                Luxury3DNavIcon(
                    emojiIcon = "✉️",
                    vectorIcon = Icons.Default.Email,
                    label = if (isEn) "Chats" else "المحادثات",
                    isSelected = isChatsScreen,
                    badgeCount = unreadChatsCount,
                    onClick = { onChatsClick() }
                )
            }
        }
    }
}
