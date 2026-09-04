package com.example.ui.screens.register

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.navigation.AppScreens
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordResetWaitingScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val sp = remember { context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE) }
    
    val targetPhoneFromVm by viewModel.passwordRecoveryWaitingPhone.collectAsState()
    val cleanPhone = remember(targetPhoneFromVm) {
        if (targetPhoneFromVm.isNotBlank()) targetPhoneFromVm
        else sp.getString("password_recovery_waiting_phone", "") ?: ""
    }

    var status by remember { mutableStateOf("PENDING") }
    var newPassword by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("صاحب الحساب") }
    var accountType by remember { mutableStateOf("حساب معتمد") }
    var isSubmittingLogin by remember { mutableStateOf(false) }

    // Listen in real-time to Firestore for reset completion
    DisposableEffect(cleanPhone) {
        if (cleanPhone.isBlank()) {
            onDispose { }
        } else {
            val listener = viewModel.db.collection("password_recovery_requests")
                .document(cleanPhone)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        status = snapshot.getString("status") ?: "PENDING"
                        newPassword = snapshot.getString("newPassword") ?: ""
                        accountName = snapshot.getString("name") ?: accountName
                        accountType = snapshot.getString("accountType") ?: accountType
                    }
                }
            onDispose { listener.remove() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🔑 استعادة كلمة المرور",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.surface
                )
            )
        },
        containerColor = themeColors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(
                    1.5.dp,
                    if (status == "RESOLVED") Color(0xFF10B981) else themeColors.accent.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Glowing Status Icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (status == "RESOLVED") Color(0xFF10B981).copy(alpha = 0.2f)
                                else themeColors.accent.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (status == "RESOLVED") Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (status == "RESOLVED") Color(0xFF10B981) else themeColors.accent,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    if (status == "RESOLVED") {
                        Text(
                            text = "🎉 تم إعادة تعيين كلمة المرور بنجاح!",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "مرحباً $accountName، قام مدير المنصة بتعيين كلمة المرور الجديدة لحسابك:",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )

                        // New Password Display Box
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("كلمة المرور الجديدة:", fontSize = 10.sp, color = Color.Gray)
                                    Text(
                                        text = newPassword,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("New Password", newPassword)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "📋 تم نسخ كلمة المرور!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "نسخ", tint = themeColors.accent)
                                }
                            }
                        }

                        // Direct Login Button
                        Button(
                            onClick = {
                                isSubmittingLogin = true
                                viewModel.searchAccountForRestore(cleanPhone) { match ->
                                    isSubmittingLogin = false
                                    if (match != null) {
                                        val provArea = match.provider?.area ?: match.store?.cityId ?: "اليمن"
                                        viewModel.setUserSessionDetails(context, match.name, cleanPhone, provArea)
                                        
                                        sp.edit()
                                            .putBoolean("is_account_logged_in", true)
                                            .putString("user_account_type", match.type)
                                            .putString("logged_account_id", match.provider?.id ?: match.store?.id ?: match.property?.id ?: "")
                                            .apply()

                                        if (match.provider != null) {
                                            if (match.provider.isDeleted) viewModel.restoreProvider(match.provider.id)
                                            viewModel.selectedProvider = match.provider
                                            viewModel.selectedStore = null
                                            viewModel.selectedProperty = null
                                            viewModel.navigateToScreen(AppScreens.DYNAMIC_PROFILE)
                                        } else if (match.store != null) {
                                            if (match.store.isDeleted) viewModel.restoreStore(match.store.id)
                                            viewModel.selectedStore = match.store
                                            viewModel.selectedProvider = null
                                            viewModel.selectedProperty = null
                                            viewModel.navigateToScreen(AppScreens.DYNAMIC_PROFILE)
                                        } else if (match.property != null) {
                                            if (match.property.isDeleted) viewModel.restoreProperty(match.property.id)
                                            viewModel.selectedProperty = match.property
                                            viewModel.selectedProvider = null
                                            viewModel.selectedStore = null
                                            viewModel.navigateToScreen(AppScreens.DYNAMIC_PROFILE)
                                        } else {
                                            viewModel.selectedProvider = null
                                            viewModel.selectedStore = null
                                            viewModel.selectedProperty = null
                                            viewModel.selectedJob = null
                                            viewModel.navigateToScreen(AppScreens.USER_BROWSE)
                                        }
                                        Toast.makeText(context, "🔓 أهلاً بك، تم تسجيل الدخول إلى حسابك!", Toast.LENGTH_LONG).show()
                                    } else {
                                        viewModel.navigateToScreen(AppScreens.USER_BROWSE)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !isSubmittingLogin
                        ) {
                            Text(
                                text = if (isSubmittingLogin) "جاري الدخول..." else "تسجيل الدخول ومتابعة ملفي الشخصي 🔓",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        // Pending State
                        Text(
                            text = "⏳ طلبك قيد المتابعة لدى الإدارة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.accent,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "تم إشعار إدارة المنصة بطلب إعادة تعيين كلمة المرور لرقمك ($cleanPhone).\nسيقوم المشرف بمراجعة الحساب وتعيين كلمة مرور جديدة وإرسالها لك مباشرة عبر الوسيلة المختارة.",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        // Info card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("📞 رقم الهاتف: $cleanPhone", fontSize = 11.sp, color = Color.White)
                                Text("👤 نوع الحساب: $accountType", fontSize = 11.sp, color = Color.White)
                                Text("🛡️ حالة الطلب: بانتظار المشرف ⌛", fontSize = 11.sp, color = themeColors.accent)
                            }
                        }

                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = themeColors.accent,
                            trackColor = Color.DarkGray
                        )
                    }
                }
            }

            // Quick Contact with Admin Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "💬 وسائل التواصل المباشر مع إدارة المنصة:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // WhatsApp
                        Button(
                            onClick = {
                                val adminPhone = "967777000000"
                                val msg = "مرحباً إدارة دليل خدمات اليمن، أطلب تسريع إعادة تعيين كلمة المرور لحسابي المسجل: $cleanPhone"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$adminPhone?text=${Uri.encode(msg)}"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Telegram
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/yemenservices_support"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تيليجرام", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // In-app chat
                        Button(
                            onClick = {
                                viewModel.navigateToScreen(AppScreens.CHAT_DIRECT)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("محادثة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Return to Home Button
            OutlinedButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp),
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                Text("العودة إلى الصفحة الرئيسية 🏠", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}
