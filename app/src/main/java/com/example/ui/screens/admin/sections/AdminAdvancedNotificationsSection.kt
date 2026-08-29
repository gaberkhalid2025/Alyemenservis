package com.example.ui.screens.admin.sections

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun AdminAdvancedNotificationsSection(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notifications by viewModel.notifications.collectAsState()

    var notifTitle by remember { mutableStateOf("") }
    var notifBody by remember { mutableStateOf("") }
    var selectedTargetRole by remember { mutableStateOf("ALL") }
    var specificTargetPhone by remember { mutableStateOf("") }
    var isUrgentPriority by remember { mutableStateOf(false) }

    val roleOptions = listOf(
        "ALL" to "📢 الجميع",
        "PROVIDERS" to "🔧 الفنيين",
        "STORES" to "🏪 المتاجر",
        "RESTAURANTS" to "🍔 المطاعم",
        "MEDICAL" to "🏥 الطبي",
        "REAL_ESTATE" to "🏠 العقارات",
        "JOBS" to "💼 الوظائف",
        "USERS" to "👤 العملاء",
        "SPECIFIC" to "🎯 شخص/محل محدد"
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📢", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مركز رقابة وبث الإشعارات المتقدم",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "توجيه الإشعارات حسب الفئات (فنيين، متاجر، مطاعم، طبي، عقارات، وظائف، عملاء)، أو استهداف شخص ومحل بعينه مع الجدولة وتحديد الأولوية.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = notifTitle,
                    onValueChange = { notifTitle = it },
                    label = { Text("عنوان الإشعار البارز", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColors.accent
                    )
                )

                OutlinedTextField(
                    value = notifBody,
                    onValueChange = { notifBody = it },
                    label = { Text("نص الرسالة والإشعار التفصيلي", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColors.accent
                    )
                )

                Text(text = "الفئة المستهدفة بالتوجيه:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

                // Roles Selection Flow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    roleOptions.take(4).forEach { (roleKey, label) ->
                        FilterChip(
                            selected = selectedTargetRole == roleKey,
                            onClick = { selectedTargetRole = roleKey },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColors.accent,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    roleOptions.drop(4).forEach { (roleKey, label) ->
                        FilterChip(
                            selected = selectedTargetRole == roleKey,
                            onClick = { selectedTargetRole = roleKey },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColors.accent,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                if (selectedTargetRole == "SPECIFIC") {
                    OutlinedTextField(
                        value = specificTargetPhone,
                        onValueChange = { specificTargetPhone = it },
                        label = { Text("رقم هاتف المستهدف (المستخدم أو المنشأة)", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColors.accent
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isUrgentPriority,
                            onCheckedChange = { isUrgentPriority = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF5350))
                        )
                        Text(text = "إشعار عاجل ذو أولوية قصوى 🚨", color = Color.White, fontSize = 12.sp)
                    }

                    Text(text = "⏱️ إرسال فوري ومباشر", color = Color.LightGray, fontSize = 11.5.sp)
                }

                Button(
                    onClick = {
                        if (notifTitle.isBlank() || notifBody.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة عنوان ونص الإشعار", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val target = if (selectedTargetRole == "SPECIFIC") specificTargetPhone.ifEmpty { "ALL" } else selectedTargetRole
                        val finalTitle = if (isUrgentPriority) "🚨 $notifTitle" else notifTitle

                        viewModel.addNotification(
                            title = finalTitle,
                            message = notifBody,
                            targetType = if (selectedTargetRole == "SPECIFIC") "USER" else "ALL",
                            targetValue = target
                        )

                        Toast.makeText(context, "✅ تم إرسال وبث الإشعار فورياً ومزامنته سحابياً لجميع المستهدفين!", Toast.LENGTH_LONG).show()
                        notifTitle = ""
                        notifBody = ""
                        specificTargetPhone = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إرسال وبث الإشعار المتقدم", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Previous notifications list
        Text(
            text = "📋 سجل الإشعارات الصادرة مؤخراً (${notifications.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
        ) {
            items(notifications, key = { it.id }) { notif ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = notif.title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color.White)
                            Surface(
                                color = themeColors.accent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = notif.targetType,
                                    color = themeColors.accent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = notif.message, fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}
