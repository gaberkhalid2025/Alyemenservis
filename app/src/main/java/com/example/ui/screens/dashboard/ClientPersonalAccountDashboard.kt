@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.dashboard

import com.example.ui.screens.dashboard.*
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
import com.example.ui.*
import com.example.ui.utils.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun ClientPersonalAccountDashboard(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    context: android.content.Context,
    currentUserName: String,
    currentUserPhone: String,
    currentUserResidence: String,
    currentUserId: String,
    bookings: List<com.example.data.BookingEntity>,
    onShowRegistrationFormsAnyway: () -> Unit,
    onNavigateToSupportChat: () -> Unit = { viewModel.navigateTo("CHAT_SUPPORT") }
) {
    var showRestoreDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Welcome Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, themeColors.accent)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(themeColors.accent.copy(alpha = 0.2f))
                        .border(2.dp, themeColors.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 36.sp)
                }
                Text(
                    text = "مرحباً بك: $currentUserName",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "الحساب الشخصي والتحكم الموحد بالخدمات",
                    fontSize = 12.sp,
                    color = themeColors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Account Details Card
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📋 بيانات الحساب الموثقة:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("📱 رقم الهاتف اليمني:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Text(currentUserPhone, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Divider(color = Color.Gray.copy(alpha = 0.2f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("📍 العنوان ومنطقة السكن:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Text(currentUserResidence, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Divider(color = Color.Gray.copy(alpha = 0.2f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🆔 معرف الحساب الفريد (ID):", fontSize = 11.sp, color = themeColors.textSecondary)
                    Text(currentUserId, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                }
            }
        }

        // Live Chat with Support Button
        Button(
            onClick = { onNavigateToSupportChat() },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("💬 محادثة فورية مباشرة مع الإدارة والدعم الفني", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Restore Account reminder / button
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔄 حماية واسترجاع حسابك والدردشات", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text(
                    text = "حسابك مشفر سحابياً برقم هاتفك. في حال قمت بحذف التطبيق أو مسح البيانات أو الانتقال لهاتف جديد، يمكنك استرجاع حسابك بواسطة رقم هاتفك وكلمة المرور فوراً.",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
                Button(
                    onClick = { showRestoreDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, themeColors.accent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("🔓 اضغط لاسترجاع حسابك بواسطة رقم الهاتف وكلمة المرور", color = themeColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Bookings section
        Text("📅 طلبات وحجوزات الخدمة الخاصة بك:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        val myBookings = remember(bookings, currentUserPhone) { bookings.filter { it.customerPhone == currentUserPhone } }
        
        if (myBookings.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "لم تقم بإجراء أي حجوزات بعد. بمجرد قيامك بالحجز، ستظهر تفاصيل الحجز هنا مباشرة للتحكم ومتابعة الفنيين.",
                    fontSize = 11.sp,
                    color = themeColors.textSecondary,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            myBookings.forEach { b ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("الطلب #${b.bookingNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            
                            val statusText = when(b.status) {
                                "PENDING" -> "⏳ قيد الانتظار"
                                "APPROVED" -> "✅ مقبول"
                                "COMPLETED" -> "🎯 مكتمل"
                                "CANCELLED" -> "❌ ملغي"
                                else -> b.status
                            }
                            val statusColor = when(b.status) {
                                "PENDING" -> Color(0xFFFFB703)
                                "APPROVED" -> Color(0xFF2EC4B6)
                                "COMPLETED" -> Color(0xFF00B4D8)
                                "CANCELLED" -> Color(0xFFE63946)
                                else -> Color.White
                            }
                            Text(
                                text = statusText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier
                                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text("الفني المطلوب: ${b.providerName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("الخدمة والمشكلة: ${b.serviceType}", fontSize = 10.sp, color = themeColors.textSecondary)
                        Text("الموعد المقترح: ${b.dateString} - ${b.timeString}", fontSize = 10.sp, color = themeColors.textSecondary)

                        if (b.status == "PENDING" || b.status == "APPROVED") {
                            // Direct Chat with provider button
                            Button(
                                onClick = {
                                    onNavigateToSupportChat()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("💬 مراسلة فورية سريعة بخصوص هذا الطلب", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Divider(color = themeColors.accent.copy(alpha = 0.2f))

        // Options to register as technician or add store anyway
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onShowRegistrationFormsAnyway,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("💼 الانضمام أو إضافة متجر", color = themeColors.accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    viewModel.logout(context)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D).copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, Color.Red),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("🚪 تسجيل الخروج / تبديل الحساب", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Direct Restore account Dialog
    if (showRestoreDialog) {
        var restorePhone by remember { mutableStateOf("") }
        var restorePass by remember { mutableStateOf("") }
        var isRestoring by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showRestoreDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🔓 استرجاع حساب سابق موحد", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    OutlinedTextField(
                        value = restorePhone,
                        onValueChange = { restorePhone = it },
                        label = { Text("رقم هاتفك اليمني المكون من 9 أرقام") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = restorePass,
                        onValueChange = { restorePass = it },
                        label = { Text("كلمة المرور السرية للحساب") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val cleanPhone = restorePhone.trim().replace(" ", "")
                                val cleanPass = restorePass.trim()
                                if (cleanPhone.length == 9 && cleanPass.isNotEmpty()) {
                                    isRestoring = true
                                    viewModel.restoreGuestUser(
                                        context = context,
                                        phone = cleanPhone,
                                        password = cleanPass,
                                        onResult = { success, msg ->
                                            isRestoring = false
                                            if (success) {
                                                showRestoreDialog = false
                                            }
                                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } else {
                                    android.widget.Toast.makeText(context, "❌ يرجى تعبئة كافة الحقول بشكل صحيح!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isRestoring) "جاري الاسترجاع..." else "تأكيد واسترجاع", fontSize = 11.sp, color = Color.White)
                        }
                        TextButton(onClick = { showRestoreDialog = false }) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.2f))

                    TextButton(
                        onClick = {
                            val cleanPhone = restorePhone.trim().replace(" ", "")
                            if (cleanPhone.length == 9) {
                                viewModel.requestAdminPasswordReset(cleanPhone)
                                android.widget.Toast.makeText(context, "📩 تم إرسال طلب إعادة تعيين كلمة المرور لإدارة التطبيق لرقمك ($cleanPhone)", android.widget.Toast.LENGTH_LONG).show()
                                showRestoreDialog = false
                            } else {
                                android.widget.Toast.makeText(context, "⚠️ يرجى إدخال رقم هاتفك في الحقل أولاً لطلب إعادة التعيين من الإدارة", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("❓ نسيت كلمة المرور؟ اضغط لطلب إعادة تعيينها من الإدارة", color = themeColors.accent, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}