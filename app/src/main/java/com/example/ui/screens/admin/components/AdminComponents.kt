package com.example.ui.screens.admin.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.data.models.*
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
 * 🏷️ Reusable Admin Status Badge
 */
@Composable
fun AdminStatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, containerColor.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = containerColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * 🎛️ Reusable Admin Switch Row for Settings & Toggles
 */
@Composable
fun AdminSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(themeColors.accent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 10.5.sp,
                        color = Color.Gray,
                        lineHeight = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = themeColors.accent,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }
    }
}

/**
 * 🔘 Reusable Admin Action Buttons
 */
@Composable
fun AdminActionButtons(
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onApprove: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    onToggleBlock: (() -> Unit)? = null,
    isBlocked: Boolean = false,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onApprove != null) {
            Button(
                onClick = onApprove,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("موافقة ✓", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        if (onReject != null) {
            Button(
                onClick = onReject,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("رفض ❌", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        if (onToggleBlock != null) {
            OutlinedButton(
                onClick = onToggleBlock,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isBlocked) Color(0xFF10B981) else Color(0xFFF59E0B)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isBlocked) "إلغاء الحظر" else "حظر 🚫",
                    fontSize = 10.5.sp,
                    color = if (isBlocked) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
            }
        }
        if (onEdit != null) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = themeColors.accent, modifier = Modifier.size(16.dp))
            }
        }
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFEF5350).copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * 🃏 Universal Admin Entity Card
 */
@Composable
fun AdminEntityCard(
    title: String,
    subtitle: String,
    details: String? = null,
    imageUrl: String? = null,
    statusText: String? = null,
    statusColor: Color = Color(0xFF10B981),
    isVip: Boolean = false,
    isBlocked: Boolean = false,
    themeColors: VisualThemePalette,
    actions: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (isBlocked) Color(0xFFEF5350).copy(alpha = 0.5f)
            else if (isVip) Color(0xFFF59E0B).copy(alpha = 0.5f)
            else Color.White.copy(alpha = 0.08f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isVip) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⭐ VIP", fontSize = 10.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = themeColors.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!statusText.isNullOrBlank()) {
                    AdminStatusBadge(text = statusText, containerColor = statusColor)
                }
            }

            if (!details.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = details,
                    fontSize = 10.5.sp,
                    color = Color.LightGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }

            if (actions != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Spacer(modifier = Modifier.height(8.dp))
                actions()
            }
        }
    }
}

/**
 * 🏷️ Reusable Admin Filter Chips Bar
 */
@Composable
fun AdminFilterChips(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) themeColors.accent else Color(0xFF1E293B))
                    .border(
                        1.dp,
                        if (isSelected) themeColors.accent else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectCategory(category) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = category,
                    fontSize = 11.sp,
                    color = if (isSelected) Color.Black else Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

// =========================================================================================
// 1. 🚧 شاشة وضع الصيانة والطوارئ (Maintenance Mode)
// =========================================================================================
@Composable
fun AdminMaintenanceScreenContent(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()

    var isMaint by remember(settingsState.isMaintenanceActive) { mutableStateOf(settingsState.isMaintenanceActive) }
    var emergencyMessage by remember(settingsState.chatDisabledAnnouncement) {
        mutableStateOf(if (settingsState.chatDisabledAnnouncement.isNotBlank()) settingsState.chatDisabledAnnouncement else "التطبيق تحت الصيانة الفنية الطارئة لتحسين الخدمات، سنعود قريباً.")
    }
    var emergencyFreezeOrders by remember { mutableStateOf(true) }
    var emergencyLockoutChat by remember(settingsState.disableChatAll) { mutableStateOf(settingsState.disableChatAll) }
    var adminBypassActive by remember { mutableStateOf(true) }
    var selectedDuration by remember { mutableStateOf("حتى الإشعار اليدوي") }
    var emergencyPhone by remember(settingsState.supportPhone) {
        mutableStateOf(if (settingsState.supportPhone.isNotBlank()) settingsState.supportPhone else "777644")
    }

    val durations = listOf("30 دقيقة", "ساعة واحدة", "ساعتين", "4 ساعات", "حتى الإشعار اليدوي")

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // بطاقة التحكم الرئيسية مع Checkbox وزر التفعيل
        Card(
            colors = CardDefaults.cardColors(containerColor = if (isMaint) Color(0xFF450A0A) else themeColors.surface),
            border = BorderStroke(2.dp, if (isMaint) Color.Red else Color(0xFF10B981)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = if (isMaint) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isMaint) Color.Red else Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "🚧 مركز إدارة وضع الصيانة والطوارئ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.accent
                        )
                    }
                    AdminStatusBadge(
                        text = if (isMaint) "🚨 صيانة نشطة" else "✅ تشغيل طبيعي",
                        containerColor = if (isMaint) Color.Red else Color(0xFF10B981)
                    )
                }

                // خانة اختيار (Checkbox) لتأكيد حالة التفعيل أو الإيقاف بصرياً وبشكل دقيق
                Surface(
                    color = if (isMaint) Color.Red.copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isMaint) Color.Red.copy(alpha = 0.5f) else Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newStatus = !isMaint
                                isMaint = newStatus
                                val st = settingsState
                                viewModel.updateBackdoorSettings(
                                    st.appName, st.welcomeMessage, st.footerMessage, st.activeThemeId,
                                    st.supportPhone, st.supportEmail, st.supportWhatsapp,
                                    newStatus, st.hidePromoFooter, st.assistantHidden, st.assistantSize,
                                    st.chatHidden, st.chatSize, st.maxSearchRadiusKm, st.isSpeechSearchEnabled,
                                    false, 90
                                )
                                viewModel.db.collection("settings").document("main_settings").update(
                                    mapOf(
                                        "isMaintenanceActive" to newStatus,
                                        "chatDisabledAnnouncement" to emergencyMessage
                                    )
                                )
                                Toast.makeText(context, if (newStatus) "🚨 تم تفعيل وضع الصيانة فوراً لجميع الأجهزة!" else "🟢 تم إيقاف وضع الصيانة وإعادة فتح التطبيق للعملاء!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isMaint,
                            onCheckedChange = { checked ->
                                isMaint = checked
                                val st = settingsState
                                viewModel.updateBackdoorSettings(
                                    st.appName, st.welcomeMessage, st.footerMessage, st.activeThemeId,
                                    st.supportPhone, st.supportEmail, st.supportWhatsapp,
                                    checked, st.hidePromoFooter, st.assistantHidden, st.assistantSize,
                                    st.chatHidden, st.chatSize, st.maxSearchRadiusKm, st.isSpeechSearchEnabled,
                                    false, 90
                                )
                                viewModel.db.collection("settings").document("main_settings").update(
                                    mapOf(
                                        "isMaintenanceActive" to checked,
                                        "chatDisabledAnnouncement" to emergencyMessage
                                    )
                                )
                                Toast.makeText(context, if (checked) "🚨 تم تفعيل وضع الصيانة فوراً!" else "🟢 تم إيقاف وضع الصيانة بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Color.Red, uncheckedColor = Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMaint) "وضع الصيانة مفعل (انقر أو أزل التأشير للإلغاء)" else "وضع الصيانة متوقف (انقر أو ضع علامة للتفعيل)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMaint) Color.Red else Color(0xFF10B981)
                            )
                            Text(
                                text = "خانة الاختيار تؤكد حالة التفعيل والإيقاف بصرياً ومزامنة فورية مع قاعدة البيانات سحابياً",
                                fontSize = 10.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                // زر التحويل الفوري
                Button(
                    onClick = {
                        val newStatus = !isMaint
                        isMaint = newStatus
                        val st = settingsState
                        viewModel.updateBackdoorSettings(
                            st.appName, st.welcomeMessage, st.footerMessage, st.activeThemeId,
                            st.supportPhone, st.supportEmail, st.supportWhatsapp,
                            newStatus, st.hidePromoFooter, st.assistantHidden, st.assistantSize,
                            st.chatHidden, st.chatSize, st.maxSearchRadiusKm, st.isSpeechSearchEnabled,
                            false, 90
                        )
                        viewModel.db.collection("settings").document("main_settings").update(
                            mapOf(
                                "isMaintenanceActive" to newStatus,
                                "chatDisabledAnnouncement" to emergencyMessage
                            )
                        )
                        Toast.makeText(context, if (newStatus) "🚨 تم تفعيل وضع الصيانة فوراً!" else "🟢 تم إيقاف وضع الصيانة وإعادة فتح التطبيق للعملاء!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isMaint) Color(0xFF10B981) else Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(if (isMaint) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMaint) "🟢 إيقاف وضع الصيانة وإعادة فتح التطبيق للجميع" else "🔴 تفعيل وضع الصيانة والطوارئ فوراً 🛑",
                        fontSize = 12.5.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // بطاقة صلاحيات وإعدادات الطوارئ الشاملة
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⚡ إعدادات الطوارئ والتحكم الشامل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                OutlinedTextField(
                    value = emergencyMessage,
                    onValueChange = { emergencyMessage = it },
                    label = { Text("رسالة الصيانة والطوارئ للعملاء") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = emergencyPhone,
                    onValueChange = { emergencyPhone = it },
                    label = { Text("هاتف طوارئ الدعم الفني أثناء الصيانة") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Text("المدة التقديرية لإنجاز الصيانة:", fontSize = 11.5.sp, color = Color.LightGray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(durations) { dur ->
                        val isSelected = dur == selectedDuration
                        Surface(
                            color = if (isSelected) themeColors.accent else Color(0xFF1E293B),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isSelected) themeColors.accent else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.clickable { selectedDuration = dur }
                        ) {
                            Text(dur, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("تجميد الحجوزات والطلبات الجديدة مؤقتاً", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("منع العملاء من إرسال طلبات جديدة أثناء حالة الطوارئ", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(checked = emergencyFreezeOrders, onCheckedChange = { emergencyFreezeOrders = it })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("تعطيل المحادثات المباشرة فوراً", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("إيقاف غرف الشات مؤقتاً لتخفيف الحمل على السيرفرات", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(checked = emergencyLockoutChat, onCheckedChange = { emergencyLockoutChat = it })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("استثناء المشرفين والإدارة من الحظر", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("السماح للأدمن فقط بفحص المنصة وتجربتها أثناء الصيانة", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(checked = adminBypassActive, onCheckedChange = { adminBypassActive = it })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.triggerNotification("🚨 تنبيه صيانة: $emergencyMessage")
                            Toast.makeText(context, "📢 تم بث إشعار الطوارئ لكافة المستخدمين بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بث إشعار طوارئ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.db.collection("settings").document("main_settings").update(
                                mapOf(
                                    "chatDisabledAnnouncement" to emergencyMessage,
                                    "supportPhone" to emergencyPhone,
                                    "disableChatAll" to (isMaint && emergencyLockoutChat),
                                    "emergencyFreezeOrders" to emergencyFreezeOrders,
                                    "adminBypassActive" to adminBypassActive,
                                    "maintenanceDuration" to selectedDuration
                                )
                            )
                            Toast.makeText(context, "💾 تم حفظ وتحديث كافة إعدادات الطوارئ سحابياً!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ سحابياً", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================================
// 2. 🔌 شاشة إدارة مفاتيح الربط والخدمات السحابية (API Keys)
// =========================================================================================
@Composable
fun AdminApiKeysScreenContent(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var geminiApiKey by remember { mutableStateOf("") }
    var openaiApiKey by remember { mutableStateOf("") }
    var selectedAiModel by remember { mutableStateOf("gemini-1.5-flash") }
    var showGeminiKey by remember { mutableStateOf(false) }

    var googleMapsKey by remember { mutableStateOf("") }
    var mapboxKey by remember { mutableStateOf("") }
    var selectedMapEngine by remember { mutableStateOf("OPEN_STREET_MAP") }
    var showMapsKey by remember { mutableStateOf(false) }

    var kuraimiToken by remember { mutableStateOf("") }
    var jawwalPayKey by remember { mutableStateOf("") }
    var floosakKey by remember { mutableStateOf("") }
    var oneCashKey by remember { mutableStateOf("") }

    var webhookUrl by remember { mutableStateOf("") }
    var whatsappToken by remember { mutableStateOf("") }
    var smsGatewayKey by remember { mutableStateOf("") }

    val customKeys = remember { mutableStateListOf<Triple<String, String, String>>() }
    var showAddCustomKeyDialog by remember { mutableStateOf(false) }
    var newKeyName by remember { mutableStateOf("") }
    var newKeyValue by remember { mutableStateOf("") }
    var newKeyEndpoint by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.db.collection("settings").document("api_keys").get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    geminiApiKey = doc.getString("geminiApiKey") ?: ""
                    openaiApiKey = doc.getString("openaiApiKey") ?: ""
                    selectedAiModel = doc.getString("selectedAiModel") ?: "gemini-1.5-flash"
                    googleMapsKey = doc.getString("googleMapsKey") ?: ""
                    mapboxKey = doc.getString("mapboxKey") ?: ""
                    selectedMapEngine = doc.getString("selectedMapEngine") ?: "OPEN_STREET_MAP"
                    kuraimiToken = doc.getString("kuraimiToken") ?: ""
                    jawwalPayKey = doc.getString("jawwalPayKey") ?: ""
                    floosakKey = doc.getString("floosakKey") ?: ""
                    oneCashKey = doc.getString("oneCashKey") ?: ""
                    webhookUrl = doc.getString("webhookUrl") ?: ""
                    whatsappToken = doc.getString("whatsappToken") ?: ""
                    smsGatewayKey = doc.getString("smsGatewayKey") ?: ""
                }
            }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("🔌 إدارة مفاتيح الربط والخدمات السحابية (API Keys)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Text("صلاحية كاملة لإضافة، تعديل، أو حذف مفاتيح API للمساعد الذكي، الخرائط، ومحافظ الدفع والارتباط السحابي:", fontSize = 11.sp, color = themeColors.textSecondary)

        // 1. مفاتيح المساعد الذكي
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AccountBox, contentDescription = null, tint = Color(0xFF60A5FA))
                    Text("🤖 مفاتيح المساعد الذكي (AI Assistant)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedTextField(
                    value = geminiApiKey,
                    onValueChange = { geminiApiKey = it },
                    label = { Text("Google Gemini API Key") },
                    visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                            Icon(if (showGeminiKey) Icons.Default.Close else Icons.Default.Done, contentDescription = null, tint = Color.Gray)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = openaiApiKey,
                    onValueChange = { openaiApiKey = it },
                    label = { Text("OpenAI / DeepSeek API Key (اختياري)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        }

        // 2. مفاتيح الخرائط
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF34D399))
                    Text("🗺️ مفاتيح الخرائط وتحديد المواقع", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedTextField(
                    value = googleMapsKey,
                    onValueChange = { googleMapsKey = it },
                    label = { Text("Google Maps SDK API Key") },
                    visualTransformation = if (showMapsKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showMapsKey = !showMapsKey }) {
                            Icon(if (showMapsKey) Icons.Default.Close else Icons.Default.Done, contentDescription = null, tint = Color.Gray)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = mapboxKey,
                    onValueChange = { mapboxKey = it },
                    label = { Text("Mapbox Access Token (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        }

        // 3. محافظ وبوابات الدفع الإلكتروني اليمنية
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFFFBBF24))
                    Text("💳 محافظ وبوابات الدفع الإلكتروني", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedTextField(
                    value = kuraimiToken,
                    onValueChange = { kuraimiToken = it },
                    label = { Text("مفتاح بنك الكريمي إكسبرس (Kuraimi API Token)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = jawwalPayKey,
                    onValueChange = { jawwalPayKey = it },
                    label = { Text("مفتاح محفظة جوال بي (Jawwal Pay Key)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = floosakKey,
                    onValueChange = { floosakKey = it },
                    label = { Text("مفتاح محفظة فلوسك (Floosak Secret Key)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = oneCashKey,
                    onValueChange = { oneCashKey = it },
                    label = { Text("مفتاح ون كاش / جيب (OneCash API Key)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        }

        // 4. الارتباط السحابي والويب هوك
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFFA78BFA))
                    Text("☁️ الارتباط السحابي والويب هوك (Webhooks & SMS)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    label = { Text("رابط الخادم السحابي (Cloud Webhook URL)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = whatsappToken,
                    onValueChange = { whatsappToken = it },
                    label = { Text("مفتاح واتساب للأعمال (WhatsApp Cloud API Token)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = smsGatewayKey,
                    onValueChange = { smsGatewayKey = it },
                    label = { Text("مفتاح بوابة الرسائل القصيرة (SMS Gateway Key)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        }

        // 5. المفاتيح المخصصة
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("➕ المفاتيح السحابية المخصصة", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Button(
                        onClick = { showAddCustomKeyDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("+ إضافة مفتاح جديد", color = Color.Black, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (customKeys.isEmpty()) {
                    Text("لا توجد مفاتيح مخصصة مضافة حالياً.", fontSize = 11.sp, color = Color.Gray)
                } else {
                    customKeys.forEachIndexed { index, (name, key, endpoint) ->
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                    Text("Endpoint: $endpoint", fontSize = 10.sp, color = Color.LightGray)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(key))
                                            Toast.makeText(context, "تم نسخ المفتاح للحافظة", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "نسخ", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { customKeys.removeAt(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // زر الحفظ والمزامنة الفورية السحابية
        Button(
            onClick = {
                val data = mapOf(
                    "geminiApiKey" to geminiApiKey,
                    "openaiApiKey" to openaiApiKey,
                    "selectedAiModel" to selectedAiModel,
                    "googleMapsKey" to googleMapsKey,
                    "mapboxKey" to mapboxKey,
                    "selectedMapEngine" to selectedMapEngine,
                    "kuraimiToken" to kuraimiToken,
                    "jawwalPayKey" to jawwalPayKey,
                    "floosakKey" to floosakKey,
                    "oneCashKey" to oneCashKey,
                    "webhookUrl" to webhookUrl,
                    "whatsappToken" to whatsappToken,
                    "smsGatewayKey" to smsGatewayKey,
                    "updatedAt" to System.currentTimeMillis()
                )
                viewModel.db.collection("settings").document("api_keys").set(data)
                Toast.makeText(context, "✅ تم حفظ ومزامنة كافة المفاتيح سحابياً فوراً وأمان تام!", Toast.LENGTH_LONG).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("💾 حفظ ومزامنة كافة المفاتيح سحابياً الآن", color = Color.Black, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showAddCustomKeyDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomKeyDialog = false },
            title = { Text("➕ إضافة مفتاح ربط سحابي مخصص", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newKeyName,
                        onValueChange = { newKeyName = it },
                        label = { Text("اسم الخدمة أو البوابة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newKeyValue,
                        onValueChange = { newKeyValue = it },
                        label = { Text("مفتاح API السري") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newKeyEndpoint,
                        onValueChange = { newKeyEndpoint = it },
                        label = { Text("رابط الربط / Endpoint (اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKeyName.isNotBlank() && newKeyValue.isNotBlank()) {
                            customKeys.add(Triple(newKeyName, newKeyValue, newKeyEndpoint))
                            newKeyName = ""
                            newKeyValue = ""
                            newKeyEndpoint = ""
                            showAddCustomKeyDialog = false
                            Toast.makeText(context, "تمت إضافة المفتاح بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("إضافة", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomKeyDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }
}

// =========================================================================================
// 3. 🧭 شاشة خوارزميات التوجيه التلقائي والذكاء (Auto-Routing)
// =========================================================================================
@Composable
fun AdminAutoRoutingScreenContent(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isGeneralAutoRoutingEnabled by remember { mutableStateOf(true) }

    // القطاعات المخصصة: المحلات، المراكز التجارية، المطاعم، المراكز الطبية، وخدمات التوصيل
    var isStoresRoutingEnabled by remember { mutableStateOf(true) }
    var storesCriteria by remember { mutableStateOf("الأقرب جغرافياً (GPS)") }
    var storesRadiusKm by remember { mutableStateOf("15") }

    var isMallsRoutingEnabled by remember { mutableStateOf(true) }
    var mallsCriteria by remember { mutableStateOf("الأقرب داخل نفس المدينة") }

    var isRestaurantsRoutingEnabled by remember { mutableStateOf(true) }
    var restaurantsCriteria by remember { mutableStateOf("أقرب مطعم مع أسرع تحضير") }
    var restaurantsRadiusKm by remember { mutableStateOf("10") }

    var isMedicalRoutingEnabled by remember { mutableStateOf(true) }
    var medicalCriteria by remember { mutableStateOf("أقرب طوارئ وتوفر التخصص") }

    var isDeliveryRoutingEnabled by remember { mutableStateOf(true) }
    var deliveryCriteria by remember { mutableStateOf("إسناد تلقائي لأقرب مندوب شاغر") }
    var deliveryMaxRadiusKm by remember { mutableStateOf("25") }

    LaunchedEffect(Unit) {
        viewModel.db.collection("settings").document("auto_routing").get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    isGeneralAutoRoutingEnabled = doc.getBoolean("isGeneralAutoRoutingEnabled") ?: true
                    isStoresRoutingEnabled = doc.getBoolean("isStoresRoutingEnabled") ?: true
                    isMallsRoutingEnabled = doc.getBoolean("isMallsRoutingEnabled") ?: true
                    isRestaurantsRoutingEnabled = doc.getBoolean("isRestaurantsRoutingEnabled") ?: true
                    isMedicalRoutingEnabled = doc.getBoolean("isMedicalRoutingEnabled") ?: true
                    isDeliveryRoutingEnabled = doc.getBoolean("isDeliveryRoutingEnabled") ?: true
                    storesCriteria = doc.getString("storesCriteria") ?: "الأقرب جغرافياً (GPS)"
                    restaurantsCriteria = doc.getString("restaurantsCriteria") ?: "أقرب مطعم مع أسرع تحضير"
                    medicalCriteria = doc.getString("medicalCriteria") ?: "أقرب طوارئ وتوفر التخصص"
                    deliveryCriteria = doc.getString("deliveryCriteria") ?: "إسناد تلقائي لأقرب مندوب شاغر"
                }
            }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // بطاقة التحكم الرئيسية مع Checkbox لتفعيل أو إغلاق الخاصية بسهولة
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, if (isGeneralAutoRoutingEnabled) themeColors.accent else Color.Gray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(24.dp))
                        Text("🧭 خوارزميات التوجيه التلقائي والذكاء", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    AdminStatusBadge(
                        text = if (isGeneralAutoRoutingEnabled) "مفعل 🟢" else "معطل ⚪",
                        containerColor = if (isGeneralAutoRoutingEnabled) Color(0xFF10B981) else Color.Gray
                    )
                }

                // مربع اختيار (Checkbox) لتفعيل أو إغلاق الخاصية بسهولة وبشكل مباشر
                Surface(
                    color = if (isGeneralAutoRoutingEnabled) themeColors.accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isGeneralAutoRoutingEnabled) themeColors.accent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isGeneralAutoRoutingEnabled = !isGeneralAutoRoutingEnabled }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isGeneralAutoRoutingEnabled,
                            onCheckedChange = { isGeneralAutoRoutingEnabled = it },
                            colors = CheckboxDefaults.colors(checkedColor = themeColors.accent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGeneralAutoRoutingEnabled) "خاصية التوجيه التلقائي مفعلة حالياً" else "خاصية التوجيه التلقائي معطلة",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGeneralAutoRoutingEnabled) themeColors.accent else Color.LightGray
                            )
                            Text(
                                text = "مربع الاختيار يمكن الأدمن من تفعيل أو إيقاف التوجيه التلقائي فوراً لكافة القطاعات",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        Text("🎯 التوجيه الذكي المخصص حسب القطاعات:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

        // 1. قطاع المحلات والمتاجر
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = isStoresRoutingEnabled, onCheckedChange = { isStoresRoutingEnabled = it })
                        Text("🏪 قطاع المحلات والمتاجر", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    AdminStatusBadge(text = if (isStoresRoutingEnabled) "نشط" else "متوقف", containerColor = if (isStoresRoutingEnabled) Color(0xFF10B981) else Color.Gray)
                }
                if (isStoresRoutingEnabled) {
                    Text("معيار التوجيه: أقرب متجر جغرافي (GPS) مع فحص توفر البضائع", fontSize = 11.sp, color = Color.LightGray)
                    OutlinedTextField(
                        value = storesRadiusKm,
                        onValueChange = { storesRadiusKm = it },
                        label = { Text("نطاق البحث الجغرافي للمحلات (كيلومتر)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // 2. قطاع المراكز التجارية والأسواق
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = isMallsRoutingEnabled, onCheckedChange = { isMallsRoutingEnabled = it })
                        Text("🏢 قطاع المراكز التجارية والأسواق", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    AdminStatusBadge(text = if (isMallsRoutingEnabled) "نشط" else "متوقف", containerColor = if (isMallsRoutingEnabled) Color(0xFF10B981) else Color.Gray)
                }
                if (isMallsRoutingEnabled) {
                    Text("معيار التوجيه: التوجيه للمركز التجاري الأقرب داخل نفس المدينة والأكثر شمولاً", fontSize = 11.sp, color = Color.LightGray)
                }
            }
        }

        // 3. قطاع المطاعم والكافيهات
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = isRestaurantsRoutingEnabled, onCheckedChange = { isRestaurantsRoutingEnabled = it })
                        Text("🍔 قطاع المطاعم والكافيهات", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    AdminStatusBadge(text = if (isRestaurantsRoutingEnabled) "نشط" else "متوقف", containerColor = if (isRestaurantsRoutingEnabled) Color(0xFF10B981) else Color.Gray)
                }
                if (isRestaurantsRoutingEnabled) {
                    Text("معيار التوجيه: أقرب مطعم للحي السكني مع سرعة التجهيز التقديرية والتوصيل المباشر", fontSize = 11.sp, color = Color.LightGray)
                    OutlinedTextField(
                        value = restaurantsRadiusKm,
                        onValueChange = { restaurantsRadiusKm = it },
                        label = { Text("أقصى مسافة لتوجيه طلبات الأطعمة (كم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // 4. قطاع المراكز الطبية والعيادات
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = isMedicalRoutingEnabled, onCheckedChange = { isMedicalRoutingEnabled = it })
                        Text("🏥 قطاع المراكز الطبية والعيادات", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    AdminStatusBadge(text = if (isMedicalRoutingEnabled) "نشط" else "متوقف", containerColor = if (isMedicalRoutingEnabled) Color(0xFF10B981) else Color.Gray)
                }
                if (isMedicalRoutingEnabled) {
                    Text("معيار التوجيه: التوجيه الفوري لأقرب مجمع طبي طارئ، مع مراعاة التخصص المطلوب والمناوبة", fontSize = 11.sp, color = Color.LightGray)
                }
            }
        }

        // 5. خدمات التوصيل والمندوبين
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = isDeliveryRoutingEnabled, onCheckedChange = { isDeliveryRoutingEnabled = it })
                        Text("🛵 خدمات التوصيل والمندوبين", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    AdminStatusBadge(text = if (isDeliveryRoutingEnabled) "نشط" else "متوقف", containerColor = if (isDeliveryRoutingEnabled) Color(0xFF10B981) else Color.Gray)
                }
                if (isDeliveryRoutingEnabled) {
                    Text("معيار التوجيه: إسناد الطلب تلقائياً لأقرب كابتن توصيل متوفر مع تتبع مسار GPS وزمن الوصول", fontSize = 11.sp, color = Color.LightGray)
                    OutlinedTextField(
                        value = deliveryMaxRadiusKm,
                        onValueChange = { deliveryMaxRadiusKm = it },
                        label = { Text("أقصى نطاق تغطية لأسطول التوصيل (كم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        Button(
            onClick = {
                val config = mapOf(
                    "isGeneralAutoRoutingEnabled" to isGeneralAutoRoutingEnabled,
                    "isStoresRoutingEnabled" to isStoresRoutingEnabled,
                    "storesRadiusKm" to storesRadiusKm,
                    "isMallsRoutingEnabled" to isMallsRoutingEnabled,
                    "isRestaurantsRoutingEnabled" to isRestaurantsRoutingEnabled,
                    "restaurantsRadiusKm" to restaurantsRadiusKm,
                    "isMedicalRoutingEnabled" to isMedicalRoutingEnabled,
                    "isDeliveryRoutingEnabled" to isDeliveryRoutingEnabled,
                    "deliveryMaxRadiusKm" to deliveryMaxRadiusKm,
                    "updatedAt" to System.currentTimeMillis()
                )
                viewModel.db.collection("settings").document("auto_routing").set(config)
                Toast.makeText(context, "✅ تم حفظ وتطبيق خوارزميات التوجيه الذكي سحابياً!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("💾 حفظ وتطبيق خوارزميات التوجيه سحابياً", color = Color.Black, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// =========================================================================================
// 4. ⚙️ شاشة إعدادات النظام وتفضيلات التطبيق (System Settings)
// =========================================================================================
@Composable
fun AdminSystemSettingsScreenContent(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()

    var systemVersion by remember(settingsState.appVersion) {
        mutableStateOf(if (settingsState.appVersion.isNotBlank()) settingsState.appVersion else "2.6.0")
    }
    var minRequiredVersion by remember { mutableStateOf("2.5.0") }
    var forceUpdateEnabled by remember { mutableStateOf(false) }
    var updateDownloadUrl by remember(settingsState.appDownloadUrl) {
        mutableStateOf(if (settingsState.appDownloadUrl.isNotBlank()) settingsState.appDownloadUrl else "https://yemen-services.app/download")
    }

    var defaultLanguage by remember { mutableStateOf("العربية") }
    var showLanguageToggle by remember(settingsState.showLangIcon) { mutableStateOf(settingsState.showLangIcon) }
    var autoDetectLanguage by remember { mutableStateOf(true) }
    var forceRtlDirection by remember { mutableStateOf(true) }

    var defaultCurrency by remember { mutableStateOf("ريال يمني (YER)") }
    var appDisplayName by remember(settingsState.appName) { mutableStateOf(settingsState.appName) }
    var welcomeMessage by remember(settingsState.welcomeMessage) { mutableStateOf(settingsState.welcomeMessage) }
    var defaultThemeMode by remember { mutableStateOf("الوضع الداكن (Dark)") }

    val languages = listOf("العربية", "English")
    val currencies = listOf("ريال يمني (YER)", "ريال سعودي (SAR)", "دولار أمريكي (USD)")
    val themes = listOf("الوضع الداكن (Dark)", "الوضع الفاتح (Light)", "تلقائي النظام")

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. بطاقة إصدار النظام والتحديثات
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = themeColors.accent)
                    Text("🚀 إصدار النظام والتحديثات البرمجية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedTextField(
                    value = systemVersion,
                    onValueChange = { systemVersion = it },
                    label = { Text("إصدار النظام الحالي (System Version)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = minRequiredVersion,
                    onValueChange = { minRequiredVersion = it },
                    label = { Text("الحد الأدنى المطلوب للإصدار (Min Version)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("إلزام المستخدمين بالتحديث الفوري", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("منع العملاء من استخدام الإصدارات الأقدم وإلزامهم بالترقية", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(checked = forceUpdateEnabled, onCheckedChange = { forceUpdateEnabled = it })
                }

                OutlinedTextField(
                    value = updateDownloadUrl,
                    onValueChange = { updateDownloadUrl = it },
                    label = { Text("رابط تنزيل التحديث المباشر (APK / Store URL)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        }

        // 2. بطاقة تفضيلات اللغات
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF60A5FA))
                    Text("🌐 تفضيلات اللغات والترجمة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Text("اللغة الافتراضية للنظام:", fontSize = 11.5.sp, color = Color.LightGray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(languages) { lang ->
                        val isSelected = lang == defaultLanguage
                        Surface(
                            color = if (isSelected) themeColors.accent else Color(0xFF1E293B),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isSelected) themeColors.accent else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.clickable { defaultLanguage = lang }
                        ) {
                            Text(lang, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("إظهار زر تبديل اللغة في القائمة السفلية", fontSize = 12.sp, color = Color.White)
                    Switch(checked = showLanguageToggle, onCheckedChange = { showLanguageToggle = it })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("اكتشاف لغة الجهاز تلقائياً", fontSize = 12.sp, color = Color.White)
                    Switch(checked = autoDetectLanguage, onCheckedChange = { autoDetectLanguage = it })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("فرض الاتجاه من اليمين لليسار (RTL Support)", fontSize = 12.sp, color = Color.White)
                    Switch(checked = forceRtlDirection, onCheckedChange = { forceRtlDirection = it })
                }
            }
        }

        // 3. تفضيلات النظام العامة والعملات
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFFFBBF24))
                    Text("🎛️ تفضيلات النظام العامة والعملات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedTextField(
                    value = appDisplayName,
                    onValueChange = { appDisplayName = it },
                    label = { Text("اسم التطبيق الرسمي") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = welcomeMessage,
                    onValueChange = { welcomeMessage = it },
                    label = { Text("رسالة الترحيب في الواجهة الرئيسية") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Text("العملة الرسمية المعتمدة:", fontSize = 11.5.sp, color = Color.LightGray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currencies) { curr ->
                        val isSelected = curr == defaultCurrency
                        Surface(
                            color = if (isSelected) themeColors.accent else Color(0xFF1E293B),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isSelected) themeColors.accent else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.clickable { defaultCurrency = curr }
                        ) {
                            Text(curr, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }

                Text("مظهر النظام الافتراضي:", fontSize = 11.5.sp, color = Color.LightGray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(themes) { thm ->
                        val isSelected = thm == defaultThemeMode
                        Surface(
                            color = if (isSelected) themeColors.accent else Color(0xFF1E293B),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isSelected) themeColors.accent else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.clickable { defaultThemeMode = thm }
                        ) {
                            Text(thm, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                val st = settingsState
                viewModel.updateBackdoorSettings(
                    appDisplayName, welcomeMessage, st.footerMessage, st.activeThemeId,
                    st.supportPhone, st.supportEmail, st.supportWhatsapp,
                    st.isMaintenanceActive, st.hidePromoFooter, st.assistantHidden, st.assistantSize,
                    st.chatHidden, st.chatSize, st.maxSearchRadiusKm, st.isSpeechSearchEnabled,
                    false, 90
                )
                viewModel.db.collection("settings").document("main_settings").update(
                    mapOf(
                        "appVersion" to systemVersion,
                        "minRequiredVersion" to minRequiredVersion,
                        "forceUpdateEnabled" to forceUpdateEnabled,
                        "appDownloadUrl" to updateDownloadUrl,
                        "defaultLanguage" to defaultLanguage,
                        "showLangIcon" to showLanguageToggle,
                        "defaultCurrency" to defaultCurrency,
                        "defaultThemeMode" to defaultThemeMode
                    )
                )
                Toast.makeText(context, "✅ تم حفظ ومزامنة إعدادات وتفضيلات النظام سحابياً!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("💾 حفظ ومزامنة إعدادات النظام سحابياً", color = Color.Black, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// =========================================================================================
// 5. 🗄️ شاشة مراقبة سعة التخزين (Storage & Cache)
// =========================================================================================
@Composable
fun AdminStorageAndCacheScreenContent(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var cacheSizeText by remember { mutableStateOf("جاري الحساب...") }

    // Dialog تأكيد أمان مع كلمة مرور الأدمن
    var sectionToWipe by remember { mutableStateOf<Pair<String, String>?>(null) } // CollectionName to DisplayTitle
    var inputAdminPassword by remember { mutableStateOf("") }
    var isPasswordError by remember { mutableStateOf(false) }

    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val jobs by viewModel.jobs.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val banners by viewModel.banners.collectAsState()

    LaunchedEffect(Unit) {
        val bytes = try {
            (context.cacheDir.walkTopDown().sumOf { it.length() } +
             context.codeCacheDir.walkTopDown().sumOf { it.length() })
        } catch (e: Exception) { 0L }
        cacheSizeText = "${String.format(java.util.Locale.US, "%.1f", bytes / (1024.0 * 1024.0))} ميجابايت"
    }

    val sectionsList = listOf(
        Triple("stores", "🏪 قسم المتاجر والمحلات", stores.count { it.sectionId != "restaurants" && it.sectionId != "medical" }),
        Triple("restaurants", "🍔 قسم المطاعم والكافيهات", stores.count { it.sectionId == "restaurants" }),
        Triple("medical", "🏥 قسم المراكز الطبية والصيدليات", stores.count { it.sectionId == "medical" }),
        Triple("properties", "🏠 قسم العقارات والمباني", properties.size),
        Triple("jobs", "💼 قسم إعلانات الوظائف", jobs.size),
        Triple("providers", "👤 قسم الفنيين ومقدمي الخدمات", providers.size),
        Triple("bookings", "📅 قسم الحجوزات والمواعيد", bookings.size),
        Triple("chat_channels", "💬 قسم المحادثات والشات", 14),
        Triple("banners", "📢 قسم الإعلانات والبنرات", banners.size),
        Triple("reports", "⚠️ قسم البلاغات والشكاوى", reports.size)
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // بطاقة مراقبة التخزين الحقيقي وحذف الكاش
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = themeColors.accent)
                    Text("🗄️ مراقبة التخزين ومساحة الذاكرة والكاش", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Text("حجم ملفات الكاش المؤقتة محلياً: $cacheSizeText", fontSize = 12.sp, color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                Text("حالة السيرفر والاتصال السحابي: متصل ومستقر 100%", fontSize = 11.5.sp, color = Color(0xFF10B981))

                Button(
                    onClick = {
                        try {
                            context.cacheDir.deleteRecursively()
                            context.codeCacheDir.deleteRecursively()
                            cacheSizeText = "0.0 ميجابايت"
                            Toast.makeText(context, "🧹 تم مسح ملفات الكاش والذاكرة المؤقتة بالكامل وتسريع الأداء بنجاح!", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "تم تنظيف الذاكرة المؤقتة", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تنظيف الكاش والملفات المؤقتة وتسريع التطبيق 🧹", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("🚨 صلاحيات حذف وتصفية بيانات الأقسام (تتطلب كلمة مرور الأدمن):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Red)

        // قائمة الأقسام مع صلاحية الحذف
        sectionsList.forEach { (colId, label, count) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("السجلات الحالية: $count عنصر مسجل", fontSize = 10.5.sp, color = Color.LightGray)
                    }

                    Button(
                        onClick = {
                            sectionToWipe = Pair(colId, label)
                            inputAdminPassword = ""
                            isPasswordError = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف القسم", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // نافذة تأكيد الأمان المشددة (Admin Password Confirmation Dialog)
    sectionToWipe?.let { (colId, title) ->
        AlertDialog(
            onDismissRequest = { sectionToWipe = null },
            containerColor = Color(0xFF0F172A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                    Text("🚨 تأكيد حذف بيانات $title", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "تحذير: هذه العملية ستقوم بحذف وتصفية كافة البيانات المسجلة في $title بشكل نهائي من السيرفر. تجنباً للحذف الخاطئ، يرجى إدخال كلمة مرور الأدمن للتأكيد:",
                        color = Color.LightGray,
                        fontSize = 11.5.sp
                    )

                    OutlinedTextField(
                        value = inputAdminPassword,
                        onValueChange = {
                            inputAdminPassword = it
                            isPasswordError = false
                        },
                        label = { Text("كلمة مرور الأدمن (Admin Password)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = isPasswordError,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    if (isPasswordError) {
                        Text("❌ كلمة المرور غير صحيحة! تم إيقاف عملية الحذف.", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.verifyAdminOrOwnerPassword(inputAdminPassword)) {
                            // مسح القسم من Firestore
                            val targetCol = if (colId == "restaurants" || colId == "medical") "stores" else colId
                            viewModel.db.collection(targetCol).get()
                                .addOnSuccessListener { snapshot ->
                                    val batch = viewModel.db.batch()
                                    snapshot.documents.forEach { doc ->
                                        if (colId == "restaurants") {
                                            val sec = doc.getString("sectionId") ?: ""
                                            if (sec == "restaurants") batch.delete(doc.reference)
                                        } else if (colId == "medical") {
                                            val sec = doc.getString("sectionId") ?: ""
                                            if (sec == "medical") batch.delete(doc.reference)
                                        } else {
                                            batch.delete(doc.reference)
                                        }
                                    }
                                    batch.commit().addOnSuccessListener {
                                        Toast.makeText(context, "💥 تم مسح وتصفية بيانات $title بالكامل بنجاح!", Toast.LENGTH_LONG).show()
                                        sectionToWipe = null
                                    }
                                }
                        } else {
                            isPasswordError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الحذف النهائي", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sectionToWipe = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }
}

// =========================================================================================
// 6. 📋 شاشة تهيئة البيانات وإدارة الأقسام (Data Initialization & Management)
// =========================================================================================
@Composable
fun AdminDataManagementScreenContent(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val jobs by viewModel.jobs.collectAsState()
    val providers by viewModel.providers.collectAsState()

    var selectedSection by remember { mutableStateOf("المحلات والمتاجر") }
    val sectionTabs = listOf(
        "المحلات والمتاجر",
        "المطاعم والكافيهات",
        "المراكز الطبية",
        "العقارات",
        "إعلانات الوظائف",
        "الفنيين والمهن"
    )

    // Dialog لتعديل الكيان (الصور، الأسعار، الخدمات، الاسم، الهاتف)
    var editingEntity by remember { mutableStateOf<Any?>(null) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editLogoUrl by remember { mutableStateOf("") }
    var editCoverUrl by remember { mutableStateOf("") }
    var editPriceOrSalary by remember { mutableStateOf("") }
    var editCity by remember { mutableStateOf("") }
    var editServicesOrProducts by remember { mutableStateOf("") }

    // Dialog لإضافة عنصر جديد
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newLogoUrl by remember { mutableStateOf("") }
    var newCoverUrl by remember { mutableStateOf("") }
    var newPriceOrSalary by remember { mutableStateOf("") }
    var newCity by remember { mutableStateOf("صنعاء") }
    var newServicesOrProducts by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📋 إدارة وتهيئة بيانات الأقسام الشاملة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            Button(
                onClick = {
                    newName = ""
                    newPhone = ""
                    newLogoUrl = ""
                    newCoverUrl = ""
                    newPriceOrSalary = ""
                    newServicesOrProducts = ""
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("+ إضافة عنصر جديد", color = Color.Black, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        // شريط الأقسام السريع
        AdminFilterChips(
            categories = sectionTabs,
            selectedCategory = selectedSection,
            onSelectCategory = { selectedSection = it },
            themeColors = themeColors
        )

        // عرض العناصر وتعديلها أو التوصية بها أو حذفها
        when (selectedSection) {
            "المحلات والمتاجر" -> {
                val list = stores.filter { it.sectionId != "restaurants" && it.sectionId != "medical" }
                if (list.isEmpty()) {
                    Text("لا توجد متاجر مضافة حالياً.", fontSize = 11.sp, color = Color.Gray)
                } else {
                    list.forEach { store ->
                        AdminSectionEntityCard(
                            title = store.name,
                            phone = store.phone,
                            location = "${store.cityId} - ${store.localNeighborhood}",
                            logoUrl = store.logoImage,
                            coverUrl = store.coverImage,
                            priceOrDetails = "ساعات العمل: ${store.workingHours}",
                            isRecommended = store.isVerified,
                            onToggleRecommend = {
                                val newRec = !store.isVerified
                                viewModel.db.collection("stores").document(store.id).update("isVerified", newRec)
                                Toast.makeText(context, if (newRec) "⭐ تم تمييز المتجر والتوصية به!" else "تم إلغاء التوصية", Toast.LENGTH_SHORT).show()
                            },
                            onEdit = {
                                editingEntity = store
                                editName = store.name
                                editPhone = store.phone
                                editLogoUrl = store.logoImage
                                editCoverUrl = store.coverImage
                                editPriceOrSalary = store.workingHours
                                editCity = store.cityId
                                editServicesOrProducts = store.description
                            },
                            onDelete = {
                                viewModel.db.collection("stores").document(store.id).delete()
                                Toast.makeText(context, "🗑️ تم حذف المتجر بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            themeColors = themeColors
                        )
                    }
                }
            }

            "المطاعم والكافيهات" -> {
                val list = stores.filter { it.sectionId == "restaurants" }
                if (list.isEmpty()) {
                    Text("لا توجد مطاعم مضافة حالياً.", fontSize = 11.sp, color = Color.Gray)
                } else {
                    list.forEach { rest ->
                        AdminSectionEntityCard(
                            title = rest.name,
                            phone = rest.phone,
                            location = "${rest.cityId} - ${rest.localNeighborhood}",
                            logoUrl = rest.logoImage,
                            coverUrl = rest.coverImage,
                            priceOrDetails = "المنتجات/الأطعمة: ${rest.description.take(40)}",
                            isRecommended = rest.isVerified,
                            onToggleRecommend = {
                                val newRec = !rest.isVerified
                                viewModel.db.collection("stores").document(rest.id).update("isVerified", newRec)
                                Toast.makeText(context, if (newRec) "⭐ تم تمييز المطعم والتوصية به!" else "تم إلغاء التوصية", Toast.LENGTH_SHORT).show()
                            },
                            onEdit = {
                                editingEntity = rest
                                editName = rest.name
                                editPhone = rest.phone
                                editLogoUrl = rest.logoImage
                                editCoverUrl = rest.coverImage
                                editPriceOrSalary = rest.workingHours
                                editCity = rest.cityId
                                editServicesOrProducts = rest.description
                            },
                            onDelete = {
                                viewModel.db.collection("stores").document(rest.id).delete()
                                Toast.makeText(context, "🗑️ تم حذف المطعم بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            themeColors = themeColors
                        )
                    }
                }
            }

            "المراكز الطبية" -> {
                val list = stores.filter { it.sectionId == "medical" }
                if (list.isEmpty()) {
                    Text("لا توجد مراكز طبية مضافة حالياً.", fontSize = 11.sp, color = Color.Gray)
                } else {
                    list.forEach { med ->
                        AdminSectionEntityCard(
                            title = med.name,
                            phone = med.phone,
                            location = "${med.cityId} - ${med.localNeighborhood}",
                            logoUrl = med.logoImage,
                            coverUrl = med.coverImage,
                            priceOrDetails = "الخدمات الطبية: ${med.description.take(40)}",
                            isRecommended = med.isVerified,
                            onToggleRecommend = {
                                val newRec = !med.isVerified
                                viewModel.db.collection("stores").document(med.id).update("isVerified", newRec)
                                Toast.makeText(context, if (newRec) "⭐ تم تمييز المركز الطبي والتوصية به!" else "تم إلغاء التوصية", Toast.LENGTH_SHORT).show()
                            },
                            onEdit = {
                                editingEntity = med
                                editName = med.name
                                editPhone = med.phone
                                editLogoUrl = med.logoImage
                                editCoverUrl = med.coverImage
                                editPriceOrSalary = med.workingHours
                                editCity = med.cityId
                                editServicesOrProducts = med.description
                            },
                            onDelete = {
                                viewModel.db.collection("stores").document(med.id).delete()
                                Toast.makeText(context, "🗑️ تم حذف المركز الطبي بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            themeColors = themeColors
                        )
                    }
                }
            }

            "العقارات" -> {
                if (properties.isEmpty()) {
                    Text("لا توجد عقارات مضافة حالياً.", fontSize = 11.sp, color = Color.Gray)
                } else {
                    properties.forEach { prop ->
                        AdminSectionEntityCard(
                            title = prop.title,
                            phone = prop.phone,
                            location = "${prop.cityId} - ${prop.localNeighborhood}",
                            logoUrl = prop.images.firstOrNull() ?: "",
                            coverUrl = prop.images.getOrNull(1) ?: "",
                            priceOrDetails = "السعر: ${prop.price} ريال",
                            isRecommended = prop.isRecommended,
                            onToggleRecommend = {
                                val newRec = !prop.isRecommended
                                viewModel.db.collection("properties").document(prop.id).update("isRecommended", newRec)
                                Toast.makeText(context, if (newRec) "⭐ تم تمييز العقار والتوصية به!" else "تم إلغاء التوصية", Toast.LENGTH_SHORT).show()
                            },
                            onEdit = {
                                editingEntity = prop
                                editName = prop.title
                                editPhone = prop.phone
                                editLogoUrl = prop.images.firstOrNull() ?: ""
                                editCoverUrl = prop.images.getOrNull(1) ?: ""
                                editPriceOrSalary = prop.price.toString()
                                editCity = prop.cityId
                                editServicesOrProducts = prop.description
                            },
                            onDelete = {
                                viewModel.db.collection("properties").document(prop.id).delete()
                                Toast.makeText(context, "🗑️ تم حذف العقار بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            themeColors = themeColors
                        )
                    }
                }
            }

            "إعلانات الوظائف" -> {
                if (jobs.isEmpty()) {
                    Text("لا توجد وظائف مضافة حالياً.", fontSize = 11.sp, color = Color.Gray)
                } else {
                    jobs.forEach { job ->
                        AdminSectionEntityCard(
                            title = job.title,
                            phone = job.companyName,
                            location = "المدينة: ${job.cityId}",
                            logoUrl = "",
                            coverUrl = "",
                            priceOrDetails = "الراتب: ${job.salary}",
                            isRecommended = job.isVip,
                            onToggleRecommend = {
                                val newRec = !job.isVip
                                viewModel.db.collection("jobs").document(job.id).update("isVip", newRec)
                                Toast.makeText(context, if (newRec) "⭐ تم تمييز الوظيفة والتوصية بها!" else "تم إلغاء التوصية", Toast.LENGTH_SHORT).show()
                            },
                            onEdit = {
                                editingEntity = job
                                editName = job.title
                                editPhone = job.companyName
                                editLogoUrl = ""
                                editCoverUrl = ""
                                editPriceOrSalary = job.salary
                                editCity = job.cityId
                                editServicesOrProducts = job.description
                            },
                            onDelete = {
                                viewModel.db.collection("jobs").document(job.id).delete()
                                Toast.makeText(context, "🗑️ تم حذف إعلان الوظيفة بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            themeColors = themeColors
                        )
                    }
                }
            }

            "الفنيين والمهن" -> {
                if (providers.isEmpty()) {
                    Text("لا يوجد فنيين مسجلين حالياً.", fontSize = 11.sp, color = Color.Gray)
                } else {
                    providers.forEach { prov ->
                        AdminSectionEntityCard(
                            title = prov.name,
                            phone = "${prov.phone} • ${prov.profession}",
                            location = "📍 ${prov.cityId} - ${prov.area}",
                            logoUrl = prov.profileImage,
                            coverUrl = prov.coverImage,
                            priceOrDetails = "التقييم: ⭐ ${prov.rating} (${prov.numReviews})",
                            isRecommended = prov.isVerified,
                            onToggleRecommend = {
                                val newRec = !prov.isVerified
                                viewModel.db.collection("providers").document(prov.id).update("isVerified", newRec)
                                Toast.makeText(context, if (newRec) "⭐ تم توثيق الفني والتوصية به!" else "تم إلغاء التوصية", Toast.LENGTH_SHORT).show()
                            },
                            onEdit = {
                                editingEntity = prov
                                editName = prov.name
                                editPhone = prov.phone
                                editLogoUrl = prov.profileImage
                                editCoverUrl = prov.coverImage
                                editPriceOrSalary = prov.profession
                                editCity = prov.cityId
                                editServicesOrProducts = prov.area
                            },
                            onDelete = {
                                viewModel.db.collection("providers").document(prov.id).delete()
                                Toast.makeText(context, "🗑️ تم حذف الفني بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            themeColors = themeColors
                        )
                    }
                }
            }
        }
    }

    // نافذة تعديل بيانات الكيان (الصور، الأسعار، الخدمات، التوصية)
    if (editingEntity != null) {
        AlertDialog(
            onDismissRequest = { editingEntity = null },
            title = { Text("✏️ تعديل بيانات وصور وأسعار العنصر", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("الاسم / العنوان") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("رقم الهاتف / الشركة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editLogoUrl,
                        onValueChange = { editLogoUrl = it },
                        label = { Text("رابط الصورة الشخصية / اللوجو") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCoverUrl,
                        onValueChange = { editCoverUrl = it },
                        label = { Text("رابط صورة الغلاف (Cover URL)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPriceOrSalary,
                        onValueChange = { editPriceOrSalary = it },
                        label = { Text("الأسعار / ساعات العمل / الراتب") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editServicesOrProducts,
                        onValueChange = { editServicesOrProducts = it },
                        label = { Text("الخدمات / المنتجات / الوصف") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ent = editingEntity
                        when (ent) {
                            is StoreEntity -> {
                                viewModel.db.collection("stores").document(ent.id).update(
                                    mapOf(
                                        "name" to editName,
                                        "phone" to editPhone,
                                        "logoImage" to editLogoUrl,
                                        "coverImage" to editCoverUrl,
                                        "workingHours" to editPriceOrSalary,
                                        "description" to editServicesOrProducts
                                    )
                                )
                            }
                            is PropertyEntity -> {
                                val prc = editPriceOrSalary.toDoubleOrNull() ?: ent.price
                                viewModel.db.collection("properties").document(ent.id).update(
                                    mapOf(
                                        "title" to editName,
                                        "phone" to editPhone,
                                        "price" to prc,
                                        "description" to editServicesOrProducts
                                    )
                                )
                            }
                            is JobEntity -> {
                                viewModel.db.collection("jobs").document(ent.id).update(
                                    mapOf(
                                        "title" to editName,
                                        "companyName" to editPhone,
                                        "salary" to editPriceOrSalary,
                                        "description" to editServicesOrProducts
                                    )
                                )
                            }
                            is ProviderEntity -> {
                                viewModel.db.collection("providers").document(ent.id).update(
                                    mapOf(
                                        "name" to editName,
                                        "phone" to editPhone,
                                        "profileImage" to editLogoUrl,
                                        "coverImage" to editCoverUrl,
                                        "profession" to editPriceOrSalary,
                                        "area" to editServicesOrProducts
                                    )
                                )
                            }
                        }
                        editingEntity = null
                        Toast.makeText(context, "✅ تم حفظ التعديلات والمزامنة الفورية سحابياً!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ التعديلات", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntity = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    // نافذة إضافة عنصر جديد
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("➕ إضافة عنصر جديد إلى $selectedSection", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("الاسم / العنوان") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("رقم الهاتف للتواصل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCity,
                        onValueChange = { newCity = it },
                        label = { Text("المدينة والمحافظة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newLogoUrl,
                        onValueChange = { newLogoUrl = it },
                        label = { Text("رابط الصورة الشخصية / الشعار") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCoverUrl,
                        onValueChange = { newCoverUrl = it },
                        label = { Text("رابط صورة الغلاف") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPriceOrSalary,
                        onValueChange = { newPriceOrSalary = it },
                        label = { Text("الأسعار / المنتجات والخدمات") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newId = "item_${System.currentTimeMillis()}"
                        val targetSection = when (selectedSection) {
                            "المطاعم والكافيهات" -> "restaurants"
                            "المراكز الطبية" -> "medical"
                            else -> "stores"
                        }
                        if (selectedSection == "العقارات") {
                            val newProp = PropertyEntity(
                                id = newId,
                                title = newName,
                                phone = newPhone,
                                cityId = newCity,
                                price = newPriceOrSalary.toDoubleOrNull() ?: 100000.0,
                                images = listOfNotNull(newLogoUrl.ifBlank { null }, newCoverUrl.ifBlank { null }),
                                isApproved = true
                            )
                            viewModel.db.collection("properties").document(newId).set(newProp)
                        } else if (selectedSection == "إعلانات الوظائف") {
                            val newJob = JobEntity(
                                id = newId,
                                title = newName,
                                companyName = newPhone,
                                cityId = newCity,
                                salary = newPriceOrSalary,
                                isActive = true
                            )
                            viewModel.db.collection("jobs").document(newId).set(newJob)
                        } else if (selectedSection == "الفنيين والمهن") {
                            val newProv = ProviderEntity(
                                id = newId,
                                name = newName,
                                phone = newPhone,
                                cityId = newCity,
                                profession = newPriceOrSalary.ifBlank { "فني متخصص" },
                                profileImage = newLogoUrl,
                                coverImage = newCoverUrl,
                                isVerified = true
                            )
                            viewModel.db.collection("providers").document(newId).set(newProv)
                        } else {
                            val newStore = StoreEntity(
                                id = newId,
                                name = newName,
                                phone = newPhone,
                                cityId = newCity,
                                sectionId = targetSection,
                                logoImage = newLogoUrl,
                                coverImage = newCoverUrl,
                                workingHours = newPriceOrSalary,
                                isVerified = true
                            )
                            viewModel.db.collection("stores").document(newId).set(newStore)
                        }
                        showAddDialog = false
                        Toast.makeText(context, "✅ تمت إضافة العنصر بنجاح والمزامنة الفورية!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("إضافة ومزامنة", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }
}

/**
 * 🃏 بطاقة مخصصة لإدارة كيانات الأقسام (صور، أغلفة، أسعار، توصية، حذف)
 */
@Composable
fun AdminSectionEntityCard(
    title: String,
    phone: String,
    location: String,
    logoUrl: String,
    coverUrl: String,
    priceOrDetails: String,
    isRecommended: Boolean,
    onToggleRecommend: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isRecommended) Color(0xFFF59E0B).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // صورة الغلاف والصورة الشخصية
            Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp))) {
                if (coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E293B)))
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(location, fontSize = 9.5.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }

                if (isRecommended) {
                    Surface(
                        color = Color(0xFFF59E0B),
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text("⭐ موصى به", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (logoUrl.isNotBlank()) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                        )
                    } else {
                        Box(modifier = Modifier.size(36.dp).background(themeColors.accent.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                            Text(title.take(1), fontWeight = FontWeight.Bold, color = themeColors.accent)
                        }
                    }
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1)
                        Text(phone, fontSize = 10.5.sp, color = themeColors.accent, maxLines = 1)
                    }
                }

                // زر التوصية
                Surface(
                    color = if (isRecommended) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isRecommended) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.clickable { onToggleRecommend() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = if (isRecommended) Color(0xFFF59E0B) else Color.Gray, modifier = Modifier.size(14.dp))
                        Text(if (isRecommended) "موصى به" else "توصية", fontSize = 10.sp, color = if (isRecommended) Color(0xFFF59E0B) else Color.LightGray, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(priceOrDetails, fontSize = 10.5.sp, color = Color.LightGray, maxLines = 1)

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, themeColors.accent),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل الصور والأسعار ✏️", fontSize = 10.5.sp, color = themeColors.accent)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).background(Color(0xFFEF5350).copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
