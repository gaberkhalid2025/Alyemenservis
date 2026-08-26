package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 🎨 Unified Design System Components
 * مجموعة مكونات واجهة المستخدم الموحدة للنظام
 */

/**
 * 1. ProfileHeaderCard
 * بطاقة الترويسة الشخصية لعرض الاسم، الرتبة/الشارة، الصورة، والتفاصيل
 */
@Composable
fun ProfileHeaderCard(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    avatarEmoji: String,
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.15f),
                border = BorderStroke(1.5.dp, badgeColor),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(avatarEmoji, fontSize = 24.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.2f),
                        border = BorderStroke(0.8.dp, badgeColor)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onEditClick != null) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.testTag("profile_edit_btn")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFF00E5FF))
                }
            }
        }
    }
}

/**
 * 2. StatsMetricCard
 * بطاقة عرض الإحصائيات والأرقام المباشرة
 */
@Composable
fun StatsMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    subValue: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            if (subValue != null) {
                Text(subValue, fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * 3. ActionButtonBar
 * شريط الأزرار التفاعلية السريعة
 */
@Composable
fun ActionButtonBar(
    actions: List<Pair<String, () -> Unit>>,
    primaryIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEachIndexed { index, (label, onClick) ->
            val isPrimary = index == primaryIndex
            Button(
                onClick = onClick,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("action_btn_$index"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPrimary) Color(0xFF00E5FF) else Color(0xFF334155),
                    contentColor = if (isPrimary) Color(0xFF0F172A) else Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 4. DataTableRow
 * صف عرض البيانات بشكل مفتاح وقيمة متوازية
 */
@Composable
fun DataTableRow(
    label: String,
    value: String,
    valueColor: Color = Color.White,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            }
            Text(label, fontSize = 12.sp, color = Color(0xFF94A3B8))
        }
        Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

/**
 * 5. FilterChipGroup
 * شريط الشرائح لفلترة البيانات
 */
@Composable
fun FilterChipGroup(
    items: List<Pair<String, String>>,
    selectedKey: String,
    onSelected: (String) -> Unit,
    accentColor: Color = Color(0xFF00E5FF),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { (key, label) ->
            val isSelected = key == selectedKey
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF1E293B),
                border = BorderStroke(1.dp, if (isSelected) accentColor else Color(0xFF334155)),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelected(key) }
            ) {
                Text(
                    text = label,
                    color = if (isSelected) accentColor else Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * 6. EmptyStateView
 * واجهة حالة الفراغ/عدم وجود نتائج
 */
@Composable
fun EmptyStateView(
    emoji: String = "📭",
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(6.dp))
        Text(subtitle, fontSize = 12.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(actionLabel, color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 7. LoadingSkeleton
 * مؤشر تحميل هيكلي للمحتوى
 */
@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    height: Int = 80
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFF00E5FF),
                strokeWidth = 2.dp
            )
        }
    }
}

/**
 * 8. ConfirmationDialog
 * نافذة تأكيد الإجراءات الهامة بالحذف أو الإرسال
 */
@Composable
fun ConfirmationDialog(
    isOpen: Boolean,
    title: String,
    message: String,
    confirmLabel: String = "تأكيد",
    cancelLabel: String = "إلغاء",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = { Text(message, color = Color(0xFFCBD5E1), fontSize = 13.sp) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) Color(0xFFEF4444) else Color(0xFF00E5FF)
                )
            ) {
                Text(confirmLabel, color = if (isDestructive) Color.White else Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel, color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * 9. InputFormField
 * حقل إدخال موحد للشاشات والأنماط
 */
@Composable
fun InputFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 12.sp, color = Color(0xFF64748B)) },
            leadingIcon = if (leadingIcon != null) {
                { Icon(leadingIcon, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp)) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F172A),
                unfocusedContainerColor = Color(0xFF0F172A),
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )
    }
}

/**
 * 9. RatingDisplay
 * مكون عرض التقييم بالنجوم وعدد التقييمات
 */
@Composable
fun RatingDisplay(
    rating: Double,
    reviewCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(Icons.Default.Star, contentDescription = "تقييم", tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
        Text(String.format("%.1f", rating), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        if (reviewCount > 0) {
            Text("($reviewCount)", fontSize = 10.sp, color = Color(0xFF94A3B8))
        }
    }
}

