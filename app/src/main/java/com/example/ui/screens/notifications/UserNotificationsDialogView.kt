@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.notifications




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
import com.example.ui.screens.chat.*
import com.example.ui.screens.notifications.*
import com.example.ui.screens.dashboard.*
import com.example.ui.*
import com.example.ui.utils.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun UserNotificationsDialogView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    val allNotifications by viewModel.notifications.collectAsState()
    val userPhone by viewModel.currentUserPhone.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val readIds = remember { emptyList<String>() }
    val context = androidx.compose.ui.platform.LocalContext.current

    var activeTab by remember { mutableStateOf("ALL") } // "ALL", "UNREAD", "READ"

    LaunchedEffect(Unit) {
        viewModel.loadReadNotifications(context)
    }

    val filteredNotifs = remember(allNotifications, userPhone, adminRole) {
        allNotifications.filter { notif ->
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

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(2.dp, themeColors.accent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔔 مركز الإشعارات السحابية",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (filteredNotifs.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.deleteAllNotifications() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "مسح الكل",
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("مسح الكل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                }
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Red)
                        }
                    }
                }

                // Modern 3-Tab Selector Row (All, Unread, Read)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        Pair("ALL", "الكل 📢"),
                        Pair("UNREAD", "غير مقروءة ✉️"),
                        Pair("READ", "مقروءة ✅")
                    )
                    tabs.forEach { t ->
                        val isSel = activeTab == t.first
                        val unreadCount = if (t.first == "UNREAD") filteredNotifs.count { !readIds.contains(it.id) } else 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) themeColors.accent else Color.Transparent)
                                .clickable { activeTab = t.first }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = t.second,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.Black else Color.White
                                )
                                if (unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(Color.Red, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            unreadCount.toString(),
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                if (finalNotifs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when(activeTab) {
                                    "READ" -> "لا توجد إشعارات مقروءة حالياً"
                                    "UNREAD" -> "لا توجد إشعارات غير مقروءة جديدة"
                                    else -> "لا توجد إشعارات نشطة حالياً"
                                },
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isUnread) BorderStroke(1.dp, Color(0xFF10B981)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.markNotificationAsRead(context, notif.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(iconText, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = notif.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = themeColors.accent
                                            )
                                            if (isUnread) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFF10B981))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text("جديد", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = notif.message,
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            lineHeight = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val formattedTime = remember(notif.timestamp) {
                                            try {
                                                val sdf = java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", java.util.Locale.getDefault())
                                                sdf.format(java.util.Date(notif.timestamp))
                                            } catch (e: Exception) {
                                                ""
                                            }
                                        }
                                        Text(
                                            text = formattedTime,
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteNotification(notif.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف الإشعار",
                                            tint = Color.Red.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
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
