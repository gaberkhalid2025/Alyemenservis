package com.example.ui.screens.admin.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

/**
 * 🏷️ شارة الحالة الإدارية الموحدة (AdminStatusBadge)
 * تعرض حالة العنصر (نشط، محظور، معلق، VIP) بتنسيق لوني متناسق وتصميم Material 3.
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
 * 🎛️ صف التبديل والمفاتيح الموحد للإعدادات والخيارات (AdminSwitchRow)
 * يدعم العنوان والوصف والأيقونة التعبيرية لتوحيد مفاتيح التفعيل والتعطيل في لوحات الإدارة.
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
 * 🔘 أزرار العمليات الإدارية الموحدة (AdminActionButtons)
 * توفر إجراءات الموافقة، الرفض، الحظر/إلغاء الحظر، التعديل والحذف بتصميم تفاعلي موحد.
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
 * 🃏 بطاقة الكيان الإداري الشاملة (AdminEntityCard)
 * بطاقة عرض موحدة لكافة الكيانات الإدارية (الفنيين، المحلات، العقارات، الوظائف، المستخدمين)
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
 * 🏷️ شريط رقائق الفلترة الإدارية الموحدة (AdminFilterChips)
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

/**
 * ⚠️ نافذة التأكيد الإدارية الموحدة للعمليات الحساسة (AdminConfirmDialog)
 * تعرض تنبيهاً واضحاً ومحترماً باللغة العربية مع خيارات التأكيد والإلغاء لحماية البيانات من الحذف العرضي.
 */
@Composable
fun AdminConfirmDialog(
    show: Boolean,
    title: String,
    message: String,
    confirmButtonText: String = "تأكيد الإجراء ⚠️",
    dismissButtonText: String = "إلغاء",
    isDestructive: Boolean = true,
    icon: ImageVector = Icons.Default.Warning,
    themeColors: VisualThemePalette,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF1E293B),
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive) Color(0xFFEF5350) else themeColors.accent,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = message,
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDestructive) Color(0xFFEF5350) else themeColors.accent
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = confirmButtonText,
                        color = if (isDestructive) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = dismissButtonText,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        )
    }
}

/**
 * 🛡️ نظام تسجيل التدقيق الإداري والأمني الخفيف (AdminLogger)
 * يقوم بتسجيل العمليات الحساسة والأخطاء في Logcat ومتابعة الإجراءات الإدارية بدقة.
 */
object AdminLogger {
    private const val TAG = "AdminSecurityAudit"

    fun logAction(action: String, target: String, adminUser: String = "Admin") {
        Log.i(TAG, "[$adminUser] ACTION: $action -> TARGET: $target (Time: ${System.currentTimeMillis()})")
    }

    fun logError(operation: String, throwable: Throwable? = null, message: String = "") {
        Log.e(TAG, "ERROR during $operation: $message", throwable)
    }

    fun logWarning(warning: String) {
        Log.w(TAG, "SECURITY_WARNING: $warning")
    }
}
