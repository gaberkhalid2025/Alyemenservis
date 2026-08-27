package com.example.ui.screens.register

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.utils.VisualThemePalette

/**
 * 📝 RegisterScreen - الشاشة الرئيسية لخيارات التسجيل والانضمام بـ "دليل خدمات اليمن"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit = {},
    onSelectRegistrationType: (RegistrationType) -> Unit = {},
    onOpenRestoreDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showRestoreDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "📝 التسجيل في دليل خدمات اليمن",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "منصتك الشاملة للخدمات والأنشطة التجارية",
                            fontSize = 10.5.sp,
                            color = themeColors.accent
                        )
                    }
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 🌟 1. Hero Welcome Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.5.dp, themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1E293B),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(themeColors.accent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🇾🇪", fontSize = 28.sp)
                            }

                            Text(
                                text = "مرحباً بك في بوابتك الأولى للخدمات في اليمن",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "اختر نوع الحساب الذي يمثل نشاطك للانطلاق فوراً واستقبال طلبات العملاء أو تصفح الخدمات والمتاجر.",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // 🎯 2. Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📌 اختر نوع الحساب المناسب لك:",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    Text(
                        text = "7 خيارات متاحة",
                        fontSize = 10.5.sp,
                        color = Color.Gray
                    )
                }
            }

            // 📱 3. Grid of Registration Types (7 Options)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RegistrationType.values().forEach { regType ->
                        Card(
                            onClick = { onSelectRegistrationType(regType) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(themeColors.accent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(regType.icon, fontSize = 22.sp)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = regType.title,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = regType.description,
                                        fontSize = 10.5.sp,
                                        color = Color.LightGray,
                                        lineHeight = 15.sp
                                    )
                                }

                                Button(
                                    onClick = { onSelectRegistrationType(regType) },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "اختيار 👈",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 🔓 4. Account Restore Link
            item {
                Card(
                    onClick = {
                        showRestoreDialog = true
                        onOpenRestoreDialog()
                    },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔓", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = "لديك حساب بالفعل في دليل الخدمات؟",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "اضغط هنا لاسترجاع حسابك وحجوزاتك السابقة بالكامل",
                                    fontSize = 10.5.sp,
                                    color = themeColors.accent
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "استرجاع",
                            tint = themeColors.accent
                        )
                    }
                }
            }

            // 📜 5. Bottom Terms Footer
            item {
                Text(
                    text = "بالتسجيل بالمنصة، فإنك توافق على شروط الاستخدام وسياسة حماية البيانات بالجمهورية اليمنية.",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )
            }
        }
    }

    if (showRestoreDialog) {
        GuestRegistrationDialog(
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showRestoreDialog = false },
            onRegisterCompleted = { name, phone, residence, pass ->
                showRestoreDialog = false
            }
        )
    }
}

/**
 * Backwards compatibility wrapper expected by main router
 */
@Composable
fun RegisterScreenLayout() {
    // Legacy placeholder wrapper
}
