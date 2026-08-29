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
fun AdminNotificationsSection(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()
    val context = LocalContext.current

    var notifTitle by remember { mutableStateOf("") }
    var notifBody by remember { mutableStateOf("") }
    var targetRole by remember { mutableStateOf("ALL") }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🔔 بث وتوجيه الإشعارات العامة والخاصة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = notifTitle,
                    onValueChange = { notifTitle = it },
                    label = { Text("عنوان الإشعار", color = Color.Gray) },
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
                    label = { Text("محتوى الإشعار والرسالة", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColors.accent
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL" to "الجميع", "PROVIDERS" to "المزودين", "USERS" to "العملاء").forEach { (role, label) ->
                        FilterChip(
                            selected = targetRole == role,
                            onClick = { targetRole = role },
                            label = { Text(label, fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColors.accent,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (notifTitle.isBlank() || notifBody.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة العنوان والمحتوى", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addNotification(notifTitle, notifBody, "BROADCAST", targetRole)
                            Toast.makeText(context, "تم إرسال الإشعار بنجاح لكافة الفئات المحددة", Toast.LENGTH_SHORT).show()
                            notifTitle = ""
                            notifBody = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إرسال الإشعار الآن", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }
        }

        Text(
            text = "📋 سجل الإشعارات المرسلة سابقاً (${notifications.size})",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
        ) {
            items(notifications, key = { it.id }) { notif ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = notif.title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = notif.message, fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}
