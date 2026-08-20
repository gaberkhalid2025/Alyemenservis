@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.notifications

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun UserNotificationsScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBack: () -> Unit
) {
    val allNotifications by viewModel.notifications.collectAsState()
    val userPhone by viewModel.currentUserPhone.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val readIds by viewModel.readNotificationIds.collectAsState()
    val context = LocalContext.current

    var activeTab by remember { mutableStateOf("ALL") } // "ALL", "UNREAD", "READ"

    LaunchedEffect(Unit) {
        viewModel.loadReadNotifications(context)
    }

    val filteredNotifs = remember(allNotifications, userPhone, adminRole) {
        allNotifications.filter { notif ->
            // Do not show private urgent requests to general visitors
            if (notif.title.contains("طلب عاجل") || notif.title.contains("حجز عاجل")) {
                if (userPhone.isBlank()) return@filter false
                if (notif.targetType == "USER" || notif.targetType == "PROVIDER") {
                    return@filter notif.targetValue.isEmpty() || notif.targetValue == userPhone
                }
                return@filter false
            }
            
            when (notif.targetType) {
                "ALL" -> true
                "USER" -> notif.targetValue == userPhone
                "PROVIDER" -> notif.targetValue == userPhone
                "SUPERVISOR" -> adminRole != "GUEST"
                else -> true
            }
        }
    }

    val finalNotifs = remember(filteredNotifs, readIds, activeTab) {
        when (activeTab) {
            "READ" -> filteredNotifs.filter { readIds.contains(it.id) }
            "UNREAD" -> filteredNotifs.filter { !readIds.contains(it.id) }
            else -> filteredNotifs
        }
    }

    Surface(
        color = themeColors.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                    Text("🔔 مركز الإشعارات السحابية الشاملة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                }
                if (filteredNotifs.isNotEmpty()) {
                    TextButton(onClick = { viewModel.deleteAllNotifications() }) {
                        Text("مسح الكل 🗑️", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Divider(color = themeColors.accent.copy(alpha = 0.2f))

            // Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColors.surface, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf(
                    Triple("ALL", "الكل 📢", filteredNotifs.size),
                    Triple("UNREAD", "غير مقروءة ✉️", filteredNotifs.count { !readIds.contains(it.id) }),
                    Triple("READ", "مقروءة ✅", filteredNotifs.count { readIds.contains(it.id) })
                )
                tabs.forEach { t ->
                    val isSel = activeTab == t.first
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) themeColors.accent else Color.Transparent)
                            .clickable { activeTab = t.first }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${t.second} (${t.third})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            if (finalNotifs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 56.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (activeTab) {
                                "READ" -> "لا توجد إشعارات مقروءة حالياً"
                                "UNREAD" -> "لا توجد إشعارات غير مقروءة جديدة"
                                else -> "لا توجد إشعارات نشطة حالياً"
                            },
                            color = Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(finalNotifs, key = { it.id }) { notif ->
                        val iconText = when {
                            notif.title.contains("حجز") -> "📅"
                            notif.title.contains("دردشة") || notif.title.contains("محادثة") -> "💬"
                            notif.title.contains("طلب") -> "👷"
                            notif.title.contains("تفعيل") || notif.title.contains("قبول") -> "🎉"
                            notif.title.contains("رفض") -> "❌"
                            else -> "🔔"
                        }
                        val isUnread = !readIds.contains(notif.id)

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUnread) Color(0xFF1E293B) else Color(0xFF151E2E)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isUnread) themeColors.accent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isUnread) 4.dp else 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.markNotificationAsRead(context, notif.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Icon badge
                                Surface(
                                    shape = CircleShape,
                                    color = if (isUnread) themeColors.accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(0.8.dp, if (isUnread) themeColors.accent else Color.White.copy(alpha = 0.15f)),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(iconText, fontSize = 16.sp)
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = notif.title,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUnread) themeColors.accent else Color.White
                                        )
                                        if (isUnread) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF10B981)
                                            ) {
                                                Text(
                                                    "جديد",
                                                    color = Color.White,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notif.message,
                                        fontSize = 11.sp,
                                        color = if (isUnread) Color.White else Color(0xFF94A3B8),
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    val formattedTime = remember(notif.timestamp) {
                                        try {
                                            val sdf = java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", java.util.Locale.getDefault())
                                            sdf.format(java.util.Date(notif.timestamp))
                                        } catch (e: Exception) {
                                            ""
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formattedTime,
                                            fontSize = 9.5.sp,
                                            color = Color.Gray
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteNotification(notif.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف الإشعار",
                                                tint = Color.Red.copy(alpha = 0.7f),
                                                modifier = Modifier.size(15.dp)
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
}

@Composable
fun UserNotificationsDialogView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    UserNotificationsScreen(viewModel = viewModel, themeColors = themeColors, onBack = onDismiss)
}
