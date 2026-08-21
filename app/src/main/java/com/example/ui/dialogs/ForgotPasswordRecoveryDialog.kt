package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
 * 🔑 نظام استعادة كلمة المرور بدون OTP (تكلفة صفرية وسرعة فائقة)
 * يتيح للمستخدم التواصل مع الإدارة عبر المحادثة الفورية، الواتساب، أو التيليجرام
 */
@Composable
fun ForgotPasswordRecoveryDialog(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit,
    onOpenChatWithAdmin: (channelId: String) -> Unit = {}
) {
    val context = LocalContext.current
    var phoneInput by remember { mutableStateOf("") }
    var selectedChannel by remember { mutableStateOf("IN_APP_CHAT") } // IN_APP_CHAT, WHATSAPP, TELEGRAM
    var noteInput by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔑 استعادة كلمة المرور",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.LightGray)
                    }
                }

                if (!isSubmitted) {
                    Text(
                        text = "أدخل رقم هاتفك المسجل واختر طريقة التواصل المباشرة مع فريق الدعم الفني لاستلام كلمة المرور الجديدة فوراً وبأمان تام دون انتظار رسائل SMS.",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("رقم الهاتف المسجل (مثال: 777123456)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = themeColors.accent) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Text("اختر وسيلة الاستعادة السريعة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    // Recovery Channel Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RecoveryChannelChip(
                            label = "محادثة فورية 💬",
                            isSelected = selectedChannel == "IN_APP_CHAT",
                            color = Color(0xFF3B82F6),
                            onClick = { selectedChannel = "IN_APP_CHAT" },
                            modifier = Modifier.weight(1f)
                        )
                        RecoveryChannelChip(
                            label = "واتساب 🟢",
                            isSelected = selectedChannel == "WHATSAPP",
                            color = Color(0xFF10B981),
                            onClick = { selectedChannel = "WHATSAPP" },
                            modifier = Modifier.weight(1f)
                        )
                        RecoveryChannelChip(
                            label = "تيليجرام ✈️",
                            isSelected = selectedChannel == "TELEGRAM",
                            color = Color(0xFF0EA5E9),
                            onClick = { selectedChannel = "TELEGRAM" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("ملاحظة إضافية (اختياري)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Button(
                        onClick = {
                            if (phoneInput.trim().length >= 7) {
                                val cleanPhone = phoneInput.trim()
                                when (selectedChannel) {
                                    "WHATSAPP" -> {
                                        val adminPhone = "967777000000"
                                        val msg = "مرحباً إدارة دليل خدمات اليمن، أود استعادة كلمة المرور لرقمي المسجل: $cleanPhone"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$adminPhone?text=${Uri.encode(msg)}"))
                                        context.startActivity(intent)
                                        isSubmitted = true
                                    }
                                    "TELEGRAM" -> {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/yemenservices_support"))
                                        context.startActivity(intent)
                                        isSubmitted = true
                                    }
                                    else -> {
                                        // Open in-app support chat channel
                                        val supportChannelId = "support_recovery_$cleanPhone"
                                        viewModel.sendMessageInChat("مرحباً، أطلب استعادة كلمة المرور لرقم الحساب المسجل: $cleanPhone. $noteInput")
                                        onOpenChatWithAdmin(supportChannelId)
                                        isSubmitted = true
                                    }
                                }
                            } else {
                                Toast.makeText(context, "⚠️ يرجى إدخال رقم هاتف صحيح", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("إرسال طلب الاستعادة الفوري 🚀", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                } else {
                    // Success View
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                        Text("تم إرسال طلب استعادة كلمة المرور بنجاح!", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, textAlign = TextAlign.Center)
                        Text("سيقوم المشرف بالتحقق من ملكية الحساب وتزويدك ببيانات الدخول الجديدة مباشرة.", fontSize = 11.sp, color = Color.LightGray, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تم ومتابعة", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecoveryChannelChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) color.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, if (isSelected) color else Color.Gray.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) color else Color.LightGray,
                textAlign = TextAlign.Center
            )
        }
    }
}
