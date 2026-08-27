package com.example.ui.screens.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.UnifiedBusinessAccount
import com.example.rememberBase64Bitmap
import com.example.ui.MainViewModel
import com.example.ui.components.AppSnackbarHost
import com.example.ui.components.SnackbarType
import com.example.ui.components.showCustomSnackbar
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 👤 OwnerProfileScreen
 * شاشة تعديل الملف التعريفي للنشاط التجاري مع تكامل OwnerViewModel ونظام AppSnackbar
 */
@Composable
fun OwnerProfileScreen(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    ownerViewModel: OwnerViewModel = viewModel(),
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var nameInput by remember { mutableStateOf(account.name) }
    var ownerNameInput by remember { mutableStateOf(account.ownerName) }
    var phoneInput by remember { mutableStateOf(account.phone) }
    var descInput by remember { mutableStateOf(account.description) }
    var neighborhoodInput by remember { mutableStateOf(account.neighborhood) }
    var hoursInput by remember { mutableStateOf(account.workingHours) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
        containerColor = themeColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
                Text("👤 الملف الشخصي للنشاط التجاري", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            }

            // Header Avatar & Cover Box
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val logoBitmap = rememberBase64Bitmap(account.logoImage)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = logoBitmap,
                                contentDescription = "Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (account.logoImage.startsWith("http")) {
                            AsyncImage(
                                model = account.logoImage,
                                contentDescription = "Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("👤", fontSize = 36.sp)
                        }
                    }

                    Text(
                        text = nameInput.ifBlank { "النشاط التجاري" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = account.businessType.titleArabic,
                        fontSize = 11.sp,
                        color = themeColors.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Edit Form
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("📝 تعديل البيانات الأساسية:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("اسم المحل / المركز / المكتب") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ownerNameInput,
                        onValueChange = { ownerNameInput = it },
                        label = { Text("اسم المالك / المدير المسجل") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("رقم الهاتف للاتصال والواتساب") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("وصف الخدمات والمنتجات") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = neighborhoodInput,
                        onValueChange = { neighborhoodInput = it },
                        label = { Text("الحي / المنطقة / الشارع") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hoursInput,
                        onValueChange = { hoursInput = it },
                        label = { Text("ساعات العمل والمواعيد") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (nameInput.isBlank() || phoneInput.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showCustomSnackbar(
                                        message = "يرجى كتابة الاسم ورقم الهاتف على الأقل",
                                        type = SnackbarType.WARNING
                                    )
                                }
                                return@Button
                            }

                            isSaving = true
                            ownerViewModel.updateProfile(
                                account = account,
                                name = nameInput,
                                ownerName = ownerNameInput,
                                phone = phoneInput,
                                description = descInput,
                                neighborhood = neighborhoodInput,
                                workingHours = hoursInput,
                                onSuccess = {
                                    isSaving = false
                                    scope.launch {
                                        snackbarHostState.showCustomSnackbar(
                                            message = "تم حفظ وتحديث بيانات الملف التجاري بنجاح!",
                                            type = SnackbarType.SUCCESS
                                        )
                                    }
                                },
                                onError = { err ->
                                    isSaving = false
                                    scope.launch {
                                        snackbarHostState.showCustomSnackbar(
                                            message = err,
                                            type = SnackbarType.ERROR
                                        )
                                    }
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "حفظ")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ التغييرات 💾", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
