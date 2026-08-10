@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.register
import com.example.ui.screens.dashboard.*

import com.example.ui.*
import com.example.ui.utils.*
import com.example.ui.components.FlexibleCatalogUploader


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
import com.example.viewmodels.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun ProviderRegisterFormLayout(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    regType: String,
    sectionId: String,
    onRegTypeChange: (String) -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserResidence by viewModel.currentUserResidence.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val pendingProviders by viewModel.pendingProviders.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()

    val reqs = remember(settingsState.registrationRequirements) {
        settingsState.registrationRequirements.split(",").map { req ->
            val parts = req.split("|")
            val reqName = parts.getOrNull(0)?.trim() ?: req.trim()
            val isMandatory = parts.getOrNull(1)?.trim()?.lowercase() != "optional"
            reqName to isMandatory
        }
    }

    val isNameMandatory = reqs.firstOrNull { it.first.contains("الاسم") }?.second ?: true
    val isPhoneMandatory = reqs.firstOrNull { it.first.contains("الهاتف") }?.second ?: true
    val isCatMandatory = reqs.firstOrNull { it.first.contains("القسم") || it.first.contains("الصيانة") }?.second ?: true
    val isAreaMandatory = reqs.firstOrNull { it.first.contains("المدينة") || it.first.contains("المحافظة") || it.first.contains("السكن") }?.second ?: true
    val isNeighbourMandatory = reqs.firstOrNull { it.first.contains("الحي") || it.first.contains("الشارع") }?.second ?: false
    val isSelfieMandatory = reqs.firstOrNull { it.first.contains("سيلفي") || it.first.contains("الشخصية") }?.second ?: false
    val isIdMandatory = reqs.firstOrNull { it.first.contains("البطاقة") || it.first.contains("الهوية") }?.second ?: false

    var showRegistrationFormsAnyway by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf("") }
    var customProfession by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var providerAttachmentsList by remember { mutableStateOf<List<com.example.data.ProductAttachment>>(emptyList()) }

    // Store custom fields
    var storeName by remember { mutableStateOf("") }
    var storeDesc by remember { mutableStateOf("") }
    var storePhone by remember { mutableStateOf("") }
    var storeAddress by remember { mutableStateOf("") }
    var storeAttachmentsList by remember { mutableStateOf<List<com.example.data.ProductAttachment>>(emptyList()) }

    // Property custom fields
    var propTitle by remember { mutableStateOf("") }
    var propDesc by remember { mutableStateOf("") }
    var propPrice by remember { mutableStateOf("") }
    var propArea by remember { mutableStateOf("") }
    var propPhone by remember { mutableStateOf("") }

    var selectedCategoryTab by remember { mutableStateOf(0) }

    // Store Registration Fields
    var storeOwnerName by remember { mutableStateOf("") }
    var storeWorkingHours by remember { mutableStateOf("") }
    var storeCity by remember { mutableStateOf("") }
    var storeCategorySelection by remember { mutableStateOf("") }
    var storePassword by remember { mutableStateOf("") }
    var storeConfirmPassword by remember { mutableStateOf("") }
    var storePhotosList by remember { mutableStateOf<List<String>>(emptyList()) }
    var storePdfBase64 by remember { mutableStateOf("") }
    var storePdfName by remember { mutableStateOf("") }
    var storeLicenseBase64 by remember { mutableStateOf("") }
    var storeLicenseName by remember { mutableStateOf("") }
    var storeAgreementChecked by remember { mutableStateOf(false) }

    // Property Registration Fields
    var propOwnerName by remember { mutableStateOf("") }
    var propTransactionType by remember { mutableStateOf("إيجار") } // إيجار, بيع
    var propCity by remember { mutableStateOf("") }
    var propTypeSelection by remember { mutableStateOf("شقة سكنية") } // شقة سكنية, فيلا متميزة, منزل مستقل, محل تجاري, أرض
    var propPassword by remember { mutableStateOf("") }
    var propConfirmPassword by remember { mutableStateOf("") }
    var propPhotosList by remember { mutableStateOf<List<String>>(emptyList()) }
    var propPdfBase64 by remember { mutableStateOf("") }
    var propPdfName by remember { mutableStateOf("") }
    var propAgreementChecked by remember { mutableStateOf(false) }

    // Restaurant / Cafe Fields
    var restName by remember { mutableStateOf("") }
    var restOwnerName by remember { mutableStateOf("") }
    var restPhone by remember { mutableStateOf("") }
    var restCity by remember { mutableStateOf("") }
    var restAddress by remember { mutableStateOf("") }
    var restWorkingHours by remember { mutableStateOf("") }
    var restCategorySelection by remember { mutableStateOf("مطاعم وجبات سريعة") }
    var restDesc by remember { mutableStateOf("") }
    var restPassword by remember { mutableStateOf("") }
    var restConfirmPassword by remember { mutableStateOf("") }
    var restPhotosList by remember { mutableStateOf<List<String>>(emptyList()) }
    var restPdfBase64 by remember { mutableStateOf("") }
    var restPdfName by remember { mutableStateOf("") }
    var restAgreementChecked by remember { mutableStateOf(false) }

    // Medical Center / Clinic Fields
    var medName by remember { mutableStateOf("") }
    var medOwnerName by remember { mutableStateOf("") }
    var medPhone by remember { mutableStateOf("") }
    var medCity by remember { mutableStateOf("") }
    var medAddress by remember { mutableStateOf("") }
    var medWorkingHours by remember { mutableStateOf("") }
    var medSpecialtySelection by remember { mutableStateOf("عيادة عامة") }
    var medDesc by remember { mutableStateOf("") }
    var medLicenseBase64 by remember { mutableStateOf("") }
    var medLicenseName by remember { mutableStateOf("") }
    var medPassword by remember { mutableStateOf("") }
    var medConfirmPassword by remember { mutableStateOf("") }
    var medPhotosList by remember { mutableStateOf<List<String>>(emptyList()) }
    var medAgreementChecked by remember { mutableStateOf(false) }

    // Job Posting Fields
    var jobTitleInput by remember { mutableStateOf("") }
    var jobCompanyNameInput by remember { mutableStateOf("") }
    var jobManagerNameInput by remember { mutableStateOf("") }
    var jobPhoneInput by remember { mutableStateOf("") }
    var jobCityInput by remember { mutableStateOf("") }
    var jobAddressInput by remember { mutableStateOf("") }
    var jobTypeInput by remember { mutableStateOf("دوام كامل") }
    var jobSalaryInput by remember { mutableStateOf("") }
    var jobDescInput by remember { mutableStateOf("") }
    var jobRequirementsInput by remember { mutableStateOf("") }
    var jobPasswordInput by remember { mutableStateOf("") }
    var jobConfirmPasswordInput by remember { mutableStateOf("") }
    var jobAgreementChecked by remember { mutableStateOf(false) }

    var showRestoreAccountDialog by remember { mutableStateOf(false) }
    var restorePhoneInput by remember { mutableStateOf("") }
    var restoreStep by remember { mutableStateOf(1) }
    var matchedProvider by remember { mutableStateOf<com.example.data.ProviderEntity?>(null) }
    var matchedPending by remember { mutableStateOf<com.example.data.PendingProviderEntity?>(null) }
    var matchedStore by remember { mutableStateOf<com.example.data.StoreEntity?>(null) }
    var matchedProperty by remember { mutableStateOf<com.example.data.PropertyEntity?>(null) }
    var matchedUserDoc by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isSearchingAccount by remember { mutableStateOf(false) }
    var restorePasswordInput by remember { mutableStateOf("") }

    val triggerRestore by viewModel.triggerRestoreAccountDialog.collectAsState()
    if (triggerRestore) {
        showRestoreAccountDialog = true
        viewModel.triggerRestoreAccountDialog.value = false
    }

    var selfiePhotoBase64 by remember { mutableStateOf("") }
    var idPhotoBase64 by remember { mutableStateOf("") }

    val isWorkPhotosRequirement = reqs.firstOrNull { it.first.contains("نماذج") || it.first.contains("الأعمال") || it.first.contains("أعمالك") }
    val showWorkPhotos = isWorkPhotosRequirement != null && settingsState.showWorkPhotos
    val isWorkPhotosMandatory = isWorkPhotosRequirement?.second ?: false

    var workPhotosList by remember { mutableStateOf<List<String>>(emptyList()) }

    val workPhotosUriPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<android.net.Uri> ->
        val converted = uris.map { convertUriToBase64(context, it) }.filter { it.isNotEmpty() }
        val combined = (workPhotosList + converted).take(settingsState.maxWorkPhotos)
        workPhotosList = combined
    }

    val storePdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            storePdfBase64 = com.example.ui.utils.convertGenericUriToBase64(context, it)
            storePdfName = "قائمة_الأسعار_والخدمات.pdf"
        }
    }

    val storeLicensePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            storeLicenseBase64 = com.example.ui.utils.convertGenericUriToBase64(context, it)
            storeLicenseName = "رخصة_الممارسة_المهنية.pdf"
        }
    }

    val storePhotosPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<android.net.Uri> ->
        val converted = uris.map { convertUriToBase64(context, it) }.filter { it.isNotEmpty() }
        storePhotosList = (storePhotosList + converted).take(5)
    }

    val propPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            propPdfBase64 = com.example.ui.utils.convertGenericUriToBase64(context, it)
            propPdfName = "وثيقة_إثبات_الملكية.pdf"
        }
    }

    val propPhotosPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<android.net.Uri> ->
        val converted = uris.map { convertUriToBase64(context, it) }.filter { it.isNotEmpty() }
        propPhotosList = (propPhotosList + converted).take(5)
    }

    // Launcher definitions for Selfie Upload
    val selfieUriPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { selfiePhotoBase64 = convertUriToBase64(context, it) }
    }

    val selfieCameraPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        bitmap?.let { selfiePhotoBase64 = com.example.ui.utils.convertBitmapToBase64(it) }
    }

    // Launcher definitions for ID Card Upload
    val idUriPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { idPhotoBase64 = convertUriToBase64(context, it) }
    }

    val idCameraPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        bitmap?.let { idPhotoBase64 = com.example.ui.utils.convertBitmapToBase64(it) }
    }

    // Camera permission verification logic to prevent crashes
    var cameraActionType by remember { mutableStateOf("") } // "SELFIE" or "ID"

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                if (cameraActionType == "SELFIE") {
                    selfieCameraPicker.launch(null)
                } else if (cameraActionType == "ID") {
                    idCameraPicker.launch(null)
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "⚠️ تعذر تشغيل الكاميرا: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        } else {
            android.widget.Toast.makeText(context, "⚠️ يجب السماح بصلاحية الكاميرا لالتقاط الصورة!", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val safeLaunchCamera: (String) -> Unit = { type ->
        try {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            )
            if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (type == "SELFIE") {
                    selfieCameraPicker.launch(null)
                } else {
                    idCameraPicker.launch(null)
                }
            } else {
                cameraActionType = type
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "⚠️ تعذر تشغيل الكاميرا: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    if (currentUserPhone.isNotEmpty() && !showRegistrationFormsAnyway) {
        ClientPersonalAccountDashboard(
            viewModel = viewModel,
            themeColors = themeColors,
            context = context,
            currentUserName = currentUserName,
            currentUserPhone = currentUserPhone,
            currentUserResidence = currentUserResidence,
            currentUserId = currentUserId,
            bookings = bookings,
            onShowRegistrationFormsAnyway = { showRegistrationFormsAnyway = true }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val joinPhone by viewModel.joinRequestPhone.collectAsState()
        val providers by viewModel.providers.collectAsState()
        val pendingProviders by viewModel.pendingProviders.collectAsState()
        val stores by viewModel.stores.collectAsState()
        val properties by viewModel.properties.collectAsState()

        val matchingPending = pendingProviders.find { it.phone == joinPhone }
        val matchingApproved = providers.find { it.phone == joinPhone }
        val matchingStore = stores.find { it.ownerId.trim() == joinPhone.trim() && joinPhone.isNotEmpty() && !it.isDeleted }
        val matchingProperty = properties.find { it.phone.trim() == joinPhone.trim() && joinPhone.isNotEmpty() && !it.isDeleted }

        if (joinPhone.isNotEmpty() && (matchingPending != null || matchingApproved != null || matchingStore != null || matchingProperty != null)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color.Red),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️ عذراً، لا يمكنك تسجيل حساب آخر!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red, textAlign = TextAlign.Center)
                        Text(
                            "أنت مسجل بالفعل في المنصة برقم الهاتف: $joinPhone. لا يسمح نظامنا الموحد بإنشاء حسابات مكررة لنفس مقدم الخدمة لمنع انتحال الشخصيات والالتزام بالشفافية والمسؤولية الفنية.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (matchingApproved != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A2F)),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📋 بيانات ملفك الشخصي المعتمد بالمنصة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Text("• الاسم الثلاثي: ${matchingApproved.name}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("• رقم الهاتف: ${matchingApproved.phone}", fontSize = 11.sp, color = Color.White)
                            Text("• التخصص: ${categories.find { it.id == matchingApproved.categoryId }?.name ?: "صيانة عامة"}", fontSize = 11.sp, color = Color.White)
                            Text("• المنطقة: ${matchingApproved.area}", fontSize = 11.sp, color = Color.White)
                            Text("• حالة العمل حالياً: ${if (matchingApproved.isAvailable) "متاح للعمل فوراً 🟢" else "مشغول مؤقتاً 🔴"}", fontSize = 11.sp, color = Color.White)
                            
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                            Text("📈 إحصائياتك ونشاطك المهني:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Text("⭐ النقاط الفنية المتراكمة: ${matchingApproved.points} نقطة مهنية", fontSize = 11.sp, color = Color.Yellow)
                        }
                    }
                } else if (matchingPending != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B2E1E)),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⏳ طلبك قيد المراجعة حالياً من قبل الإدارة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                            Text("• الاسم الثلاثي: ${matchingPending.name}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("• رقم الهاتف: ${matchingPending.phone}", fontSize = 11.sp, color = Color.White)
                            Text("• التخصص: ${categories.find { it.id == matchingPending.categoryId }?.name ?: "صيانة عامة"}", fontSize = 11.sp, color = Color.White)
                            Text("• المنطقة: ${matchingPending.area}", fontSize = 11.sp, color = Color.White)
                        }
                    }
                } else if (matchingStore != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (matchingStore.isActive) Color(0xFF1E3A2F) else Color(0xFF3B2E1E)),
                        border = BorderStroke(1.dp, if (matchingStore.isActive) Color(0xFF10B981) else Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (matchingStore.isActive) "🏪 متجرك نشط ومعتمد بالمنصة:" else "⏳ طلب إضافة متجرك قيد المراجعة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (matchingStore.isActive) Color(0xFF10B981) else Color(0xFFF59E0B))
                            Text("• اسم المتجر: ${matchingStore.name}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("• رقم هاتف المتجر: ${matchingStore.phone}", fontSize = 11.sp, color = Color.White)
                            Text("• العنوان: ${matchingStore.localNeighborhood}", fontSize = 11.sp, color = Color.White)
                        }
                    }
                } else if (matchingProperty != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (matchingProperty.isActive) Color(0xFF1E3A2F) else Color(0xFF3B2E1E)),
                        border = BorderStroke(1.dp, if (matchingProperty.isActive) Color(0xFF10B981) else Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (matchingProperty.isActive) "🏠 عقارك نشط ومعروض بالمنصة:" else "⏳ طلب إضافة عقارك قيد المراجعة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (matchingProperty.isActive) Color(0xFF10B981) else Color(0xFFF59E0B))
                            Text("• عنوان العقار: ${matchingProperty.title}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("• رقم هاتف المسؤول: ${matchingProperty.phone}", fontSize = 11.sp, color = Color.White)
                            Text("• السعر: ${matchingProperty.price} ريال يمني", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                Button(
                    onClick = { viewModel.navigateTo("JOIN_REQUEST_STATUS") },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("انتقل لصفحتك الشخصية ولوحة التحكم ⚙️", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        } else {
            // Category Selector Tabs
            val adminRoleState by viewModel.adminRole.collectAsState()
            val isAdmin = adminRoleState != "GUEST"

            val allTabs = listOf(
                Triple("🛠️ خدمات ومهن", 0, settingsState.enableProvidersRegistration),
                Triple("🏪 محل/معرض", 1, settingsState.enableStoresRegistration),
                Triple("🍔 مطعم/كافيه", 2, settingsState.enableRestaurantsRegistration),
                Triple("🏢 إدراج عقار", 3, settingsState.enablePropertiesRegistration),
                Triple("🏥 مركز طبي", 4, settingsState.enableMedicalRegistration),
                Triple("💼 نشر وظيفة", 5, settingsState.enableJobsRegistration)
            )

            val availableTabs = allTabs.filter { it.third || isAdmin }

            if (availableTabs.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Yellow.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🔒 جميع استمارات التسجيل مغلقة حالياً من قبل إدارة المنصة.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Yellow, textAlign = TextAlign.Center)
                    }
                }
            } else {
                if (availableTabs.none { it.second == selectedCategoryTab }) {
                    selectedCategoryTab = availableTabs.first().second
                }

                if (isAdmin && allTabs.any { !it.third }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFD97706).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFD97706).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            "👑 (وضع الأدمن والمالك: يظهر لك كافة استمارات التسجيل للتحكم والتجربة. يمكنك إظهار أو إخفاء أي استمارة للمستخدمين العاديين من لوحة التحكم)",
                            fontSize = 10.sp,
                            color = Color(0xFFFBBF24),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColors.surface, shape = RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(availableTabs.size) { index ->
                        val (title, tabIdx, isEnabled) = availableTabs[index]
                        val isSel = selectedCategoryTab == tabIdx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) themeColors.accent else Color.Transparent)
                                .clickable { selectedCategoryTab = tabIdx }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .border(1.dp, if (isSel) themeColors.accent else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    title,
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isAdmin && !isEnabled) {
                                    Text("🔒", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (selectedCategoryTab == 0) {
                Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "👤 تقديم طلب انضمام كفني محترف",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "املأ البيانات أدناه وسيتولى فريق الدعم الفني بمكالمتكم ومراجعة الوكالة لتفعيل العضوية في اليمن الخدمات.",
                    fontSize = 11.sp,
                    color = themeColors.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { 
                        viewModel.triggerRestoreAccountDialog.value = true
                        showRestoreAccountDialog = true 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, themeColors.accent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("🔄 هل لديك طلب أو حساب سابق؟ اضغط لاسترجاعه", color = themeColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("الاسم الثلاثي للفني" + if (isNameMandatory) " *" else " (اختياري)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            leadingIcon = if (settingsState.allowTextToSpeechJoinForm) {
                {
                    IconButton(onClick = { VoiceManager.onSpeak?.invoke(name.ifBlank { "الاسم الثلاثي للفني" }) }) {
                        Text("🔊", fontSize = 16.sp)
                    }
                }
            } else null,
            trailingIcon = if (settingsState.allowVoiceInputJoinForm) {
                {
                    IconButton(onClick = {
                        VoiceManager.onHear?.invoke { spokenText -> name = spokenText }
                    }) {
                        Text("🎙️", fontSize = 16.sp)
                    }
                }
            } else null
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("رقم الهاتف - واتساب جاهز" + if (isPhoneMandatory) " *" else " (اختياري)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            leadingIcon = if (settingsState.allowTextToSpeechJoinForm) {
                {
                    IconButton(onClick = { VoiceManager.onSpeak?.invoke(phone.ifBlank { "رقم الهاتف" }) }) {
                        Text("🔊", fontSize = 16.sp)
                    }
                }
            } else null,
            trailingIcon = if (settingsState.allowVoiceInputJoinForm) {
                {
                    IconButton(onClick = {
                        VoiceManager.onHear?.invoke { spokenText -> phone = spokenText }
                    }) {
                        Text("🎙️", fontSize = 16.sp)
                    }
                }
            } else null
        )

        var passwordVisible by remember { mutableStateOf(false) }
        var confirmPasswordVisible by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("كلمة المرور *") },
            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "👁️" else "🙈", fontSize = 16.sp)
                }
            }
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("تأكيد كلمة المرور *") },
            visualTransformation = if (confirmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Text(if (confirmPasswordVisible) "👁️" else "🙈", fontSize = 16.sp)
                }
            }
        )

        Text("اختر قسم الصيانة:" + if (isCatMandatory) " *" else " (اختياري)", fontSize = 12.sp, color = themeColors.textSecondary)
        val categoriesWithOther = remember(categories) {
            categories.filter { it.id != "other" } + com.example.data.CategoryEntity(id = "other", name = "أخرى / اكتب بنفسك", icon = "✏️", order = 99)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categoriesWithOther, key = { "reg_cat_${it.id}_${categoriesWithOther.indexOf(it)}" }) { cat ->
                val isSel = cat.id == selectedCatId
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { selectedCatId = cat.id }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(cat.icon, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(cat.name, fontSize = 11.sp, color = if (isSel) { if ((0.2126f * themeColors.accent.red + 0.7152f * themeColors.accent.green + 0.0722f * themeColors.accent.blue) > 0.5f) Color.Black else Color.White } else Color.White)
                }
            }
        }

        if (selectedCatId == "other") {
            OutlinedTextField(
                value = customProfession,
                onValueChange = { customProfession = it },
                label = { Text("اكتب تخصصك الفني بيدك بالتفصيل *") },
                placeholder = { Text("مثال: مصلح غسالات أتوماتيك، منجد كنب...") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        OutlinedTextField(
            value = area,
            onValueChange = { area = it },
            label = { Text("المدينة / المحافظة في اليمن (مثال: صنعاء)" + if (isAreaMandatory) " *" else " (اختياري)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            leadingIcon = if (settingsState.allowTextToSpeechJoinForm) {
                {
                    IconButton(onClick = { VoiceManager.onSpeak?.invoke(area.ifBlank { "المدينة أو المحافظة" }) }) {
                        Text("🔊", fontSize = 16.sp)
                    }
                }
            } else null,
            trailingIcon = if (settingsState.allowVoiceInputJoinForm) {
                {
                    IconButton(onClick = {
                        VoiceManager.onHear?.invoke { spokenText -> area = spokenText }
                    }) {
                        Text("🎙️", fontSize = 16.sp)
                    }
                }
            } else null
        )

        OutlinedTextField(
            value = neighborhood,
            onValueChange = { neighborhood = it },
            label = { Text("الحي أو الشارع (مثال: حدة)" + if (isNeighbourMandatory) " *" else " (اختياري)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            leadingIcon = if (settingsState.allowTextToSpeechJoinForm) {
                {
                    IconButton(onClick = { VoiceManager.onSpeak?.invoke(neighborhood.ifBlank { "الحي أو الشارع" }) }) {
                        Text("🔊", fontSize = 16.sp)
                    }
                }
            } else null,
            trailingIcon = if (settingsState.allowVoiceInputJoinForm) {
                {
                    IconButton(onClick = {
                        VoiceManager.onHear?.invoke { spokenText -> neighborhood = spokenText }
                    }) {
                        Text("🎙️", fontSize = 16.sp)
                    }
                }
            } else null
        )

        // Selfie and ID Photo upload cards
        Text("🪪 وثائق الهوية والتحقق المهني (مطلوب):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Personal Selfie Card
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (selfiePhotoBase64.isNotEmpty()) Color.Green else themeColors.accent.copy(alpha = 0.4f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🤳 صورة سيلفي شخصية" + if (isSelfieMandatory) " *" else " (اختياري)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))

                    val selfieBitmap = remember(selfiePhotoBase64) {
                        if (selfiePhotoBase64.isNotEmpty()) {
                            try {
                                val bytes = android.util.Base64.decode(selfiePhotoBase64, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch(e: Exception) { null }
                        } else null
                    }

                    if (selfieBitmap != null) {
                        Image(
                            bitmap = selfieBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("❌ لم ترفع بعد", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { selfieUriPicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Text("معرض 📂", fontSize = 9.sp, color = Color.White)
                        }
                        Button(
                            onClick = { safeLaunchCamera("SELFIE") },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Text("كاميرا 📸", fontSize = 9.sp, color = Color.Black)
                        }
                    }
                }
            }

            // ID Card Card
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (idPhotoBase64.isNotEmpty()) Color.Green else themeColors.accent.copy(alpha = 0.4f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🪪 صورة بطاقة الهوية" + if (isIdMandatory) " *" else " (اختياري)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))

                    val idBitmap = remember(idPhotoBase64) {
                        if (idPhotoBase64.isNotEmpty()) {
                            try {
                                val bytes = android.util.Base64.decode(idPhotoBase64, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch(e: Exception) { null }
                        } else null
                    }

                    if (idBitmap != null) {
                        Image(
                            bitmap = idBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("❌ لم ترفع بعد", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { idUriPicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Text("معرض 📂", fontSize = 9.sp, color = Color.White)
                        }
                        Button(
                            onClick = { safeLaunchCamera("ID") },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Text("كاميرا 📸", fontSize = 9.sp, color = Color.Black)
                        }
                    }
                }
            }
        }

        // Cover Photo Upload Card
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🖼️ صورة غلاف الصفحة الشخصية / المركز (اختياري)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "تظهر صورة الغلاف كبنر رئيسي أعلى ملفك الشخصي لجذب العملاء",
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (workPhotosList.isNotEmpty()) {
                    val coverPhoto = workPhotosList.first()
                    val bitmap = remember(coverPhoto) {
                        try {
                            val bytes = android.util.Base64.decode(coverPhoto, android.util.Base64.DEFAULT)
                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        } catch(e: Exception) { null }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp))) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "صورة الغلاف",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Red, shape = CircleShape)
                                .clickable { workPhotosList = emptyList() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("×", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Button(
                    onClick = { workPhotosUriPicker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (workPhotosList.isEmpty()) "📂 اختيار صورة الغلاف من المعرض" else "🔄 تغيير صورة الغلاف",
                        fontSize = 11.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Product & Services Attachments Component
        com.example.ui.ProductAttachmentsSection(
            attachments = providerAttachmentsList,
            onAttachmentsChanged = { providerAttachmentsList = it },
            mode = "REGISTRATION",
            themeColors = themeColors,
            departmentType = "PROVIDER"
        )

        Button(
            onClick = {
                val missingList = mutableListOf<String>()
                if (isNameMandatory && name.trim().isEmpty()) missingList.add("الاسم الثلاثي للفني")
                if (isPhoneMandatory && phone.trim().isEmpty()) missingList.add("رقم الهاتف")
                if (isCatMandatory && selectedCatId.isEmpty()) missingList.add("قسم الصيانة")
                if (selectedCatId == "other" && customProfession.trim().isEmpty()) missingList.add("التخصص المكتوب يدوياً")
                if (isAreaMandatory && area.trim().isEmpty()) missingList.add("المدينة والمحافظة")
                if (isNeighbourMandatory && neighborhood.trim().isEmpty()) missingList.add("الحي أو الشارع")
                if (isSelfieMandatory && selfiePhotoBase64.isEmpty()) missingList.add("صورة سيلفي شخصية")
                if (isIdMandatory && idPhotoBase64.isEmpty()) missingList.add("صورة بطاقة الهوية")

                if (password.trim().isEmpty()) missingList.add("كلمة المرور")
                if (confirmPassword.trim().isEmpty()) missingList.add("تأكيد كلمة المرور")

                if (missingList.isEmpty()) {
                    if (password != confirmPassword) {
                        viewModel.triggerNotification("⚠️ كلمتا المرور غير متطابقتين!")
                    } else {
                        val attsJson = com.example.data.ProductAttachment.serializeList(providerAttachmentsList)
                        viewModel.submitJoinForm(context, name, phone, selectedCatId, area, neighborhood, selfiePhotoBase64, idPhotoBase64, "", workPhotosList, customProfession, password, attsJson)
                        name = ""
                        phone = ""
                        password = ""
                        confirmPassword = ""
                        selectedCatId = ""
                        customProfession = ""
                        area = ""
                        neighborhood = ""
                        selfiePhotoBase64 = ""
                        idPhotoBase64 = ""
                        workPhotosList = emptyList()
                        providerAttachmentsList = emptyList()
                        android.widget.Toast.makeText(context, "📨 تم تقديم طلبك بنجاح! جاري عرض حالة الطلب التفاعلية.", android.widget.Toast.LENGTH_LONG).show()
                    }
                } else {
                    viewModel.triggerNotification("⚠️ يرجى تعبئة الحقول الإلزامية المطلوبة: ${missingList.joinToString("، ")}")
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("إرسال طلب الانضمام للمراجعة", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    } else if (selectedCategoryTab == 1) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🏪 تسجيل أصحاب المحلات والمراكز والعيادات",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "سجل متجرك، مطعمك، كافيهك، أو مركزك الطبي بالمنصة لعرض خدماتك ومنتجاتك واستقبال طلبات الحجز المباشرة من العملاء باليمن.",
                    fontSize = 11.sp,
                    color = themeColors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { 
                        viewModel.triggerRestoreAccountDialog.value = true
                        showRestoreAccountDialog = true 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, themeColors.accent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("🔄 هل لديك متجر سابق؟ اضغط لاسترجاعه", color = themeColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedTextField(
            value = storeOwnerName,
            onValueChange = { storeOwnerName = it },
            label = { Text("الاسم الثلاثي لمالك المحل/المركز *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = storeName,
            onValueChange = { storeName = it },
            label = { Text("الاسم التجاري للمحل / المركز *") },
            placeholder = { Text("مثال: سوبرماركت الهدى، كافيه السعادة، مركز الشفاء الطبي") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = storePhone,
            onValueChange = { storePhone = it },
            label = { Text("رقم هاتف المحل والواتساب المباشر *") },
            placeholder = { Text("مثال: 771234567") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = storeDesc,
            onValueChange = { storeDesc = it },
            label = { Text("وصف تفصيلي للسلع أو الخدمات أو الرعاية الصحية المقدمة *") },
            placeholder = { Text("اكتب بالتفصيل الخدمات والعروض المتاحة لتسهيل عثور العملاء عليك...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = storeWorkingHours,
            onValueChange = { storeWorkingHours = it },
            label = { Text("ساعات العمل اليومية *") },
            placeholder = { Text("مثال: 8:00 ص - 10:00 م") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = storeCity,
            onValueChange = { storeCity = it },
            label = { Text("المحافظة اليمنية (مثال: صنعاء، عدن، تعز) *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = storeAddress,
            onValueChange = { storeAddress = it },
            label = { Text("تحديد الحي السكني أو الشارع الرئيسي بالتفصيل *") },
            placeholder = { Text("مثال: شارع حدة - بجانب مركز الكمبيوتر") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Text("تحديد الفئة أو التصنيف للمحل *", fontSize = 12.sp, color = themeColors.textSecondary)
        val storeCategories = listOf("مطاعم وكافيهات", "مراكز تجارية ومحلات", "عيادات ومراكز طبية", "صيانة وأجهزة", "خدمات عامة أخرى")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(storeCategories.size) { idx ->
                val cat = storeCategories[idx]
                val isSel = cat == storeCategorySelection
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { storeCategorySelection = cat }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(cat, fontSize = 11.sp, color = if (isSel) Color.Black else Color.White)
                }
            }
        }

        var storePassVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = storePassword,
            onValueChange = { storePassword = it },
            label = { Text("تعيين كلمة المرور لحساب المالك *") },
            visualTransformation = if (storePassVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            trailingIcon = {
                IconButton(onClick = { storePassVisible = !storePassVisible }) {
                    Text(if (storePassVisible) "👁️" else "🙈", fontSize = 16.sp)
                }
            }
        )

        var storeConfirmPassword by remember { mutableStateOf("") }
        var storeConfirmPassVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = storeConfirmPassword,
            onValueChange = { storeConfirmPassword = it },
            label = { Text("تأكيد كلمة المرور *") },
            visualTransformation = if (storeConfirmPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            trailingIcon = {
                IconButton(onClick = { storeConfirmPassVisible = !storeConfirmPassVisible }) {
                    Text(if (storeConfirmPassVisible) "👁️" else "🙈", fontSize = 16.sp)
                }
            }
        )

        // Multi-Photo Selector for Store
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🖼️ معرض صور المركز والمنتجات والخدمات (حد أقصى 5 صور) *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                
                if (storePhotosList.isEmpty()) {
                    Text("لم تقم باختيار أي صور حتى الآن.", fontSize = 10.sp, color = Color.Gray)
                } else {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(storePhotosList.size) { index ->
                            val photo = storePhotosList[index]
                            val bitmap = remember(photo) {
                                try {
                                    val bytes = android.util.Base64.decode(photo, android.util.Base64.DEFAULT)
                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                } catch(e: Exception) { null }
                            }
                            Box(modifier = Modifier.size(70.dp)) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color.Red, shape = CircleShape)
                                        .clickable { storePhotosList = storePhotosList.filterIndexed { i, _ -> i != index } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("×", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (storePhotosList.size < 5) {
                    Button(
                        onClick = { storePhotosPicker.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إضافة صور من الاستوديو (${storePhotosList.size}/5)", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { storeAgreementChecked = !storeAgreementChecked }
        ) {
            Checkbox(
                checked = storeAgreementChecked,
                onCheckedChange = { storeAgreementChecked = it },
                colors = CheckboxDefaults.colors(checkedColor = themeColors.accent)
            )
            Text("أتعهد بصحة البيانات المدونة ومطابقتها التامة للواقع باليمن 📋", color = Color.White, fontSize = 11.sp)
        }

        // Product & Services Attachments Component for Stores
        com.example.ui.ProductAttachmentsSection(
            attachments = storeAttachmentsList,
            onAttachmentsChanged = { storeAttachmentsList = it },
            mode = "REGISTRATION",
            themeColors = themeColors,
            departmentType = "STORE"
        )

        Button(
            onClick = {
                val missing = mutableListOf<String>()
                if (storeOwnerName.trim().isEmpty()) missing.add("اسم المالك")
                if (storeName.trim().isEmpty()) missing.add("الاسم التجاري للمحل")
                if (storePhone.trim().isEmpty()) missing.add("رقم هاتف المحل")
                if (storeDesc.trim().isEmpty()) missing.add("الوصف التفصيلي")
                if (storeWorkingHours.trim().isEmpty()) missing.add("ساعات العمل")
                if (storeCity.trim().isEmpty()) missing.add("المحافظة")
                if (storeAddress.trim().isEmpty()) missing.add("الحي السكني")
                if (storeCategorySelection.trim().isEmpty()) missing.add("الفئة أو التصنيف")
                if (storePassword.trim().isEmpty()) missing.add("كلمة المرور")
                if (storePhotosList.isEmpty()) missing.add("صورة واحدة على الأقل للمحل")

                if (missing.isNotEmpty()) {
                    viewModel.triggerNotification("⚠️ يرجى إكمال الحقول الإلزامية المطلوبة: ${missing.joinToString("، ")}")
                } else if (storePassword != storeConfirmPassword) {
                    viewModel.triggerNotification("⚠️ كلمتا المرور غير متطابقتين!")
                } else if (!storeAgreementChecked) {
                    viewModel.triggerNotification("⚠️ يجب الموافقة والتعهد بصحة جميع البيانات أولاً!")
                } else {
                    val cleanPhone = storePhone.trim().replace(" ", "").replace("+", "")
                    val duplicateType = viewModel.checkAndGetDuplicateAccountType(cleanPhone, "")
                    if (duplicateType != null) {
                        viewModel.triggerNotification("❌ عذراً! رقم الهاتف ($cleanPhone) مسجل بالفعل كـ ($duplicateType)!")
                    } else {
                        val attsJson = com.example.data.ProductAttachment.serializeList(storeAttachmentsList)
                        val newStore = com.example.data.StoreEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            name = storeName.trim(),
                            description = storeDesc.trim(),
                            ownerId = cleanPhone,
                            ownerName = storeOwnerName.trim(),
                            phone = cleanPhone,
                            categoryId = storeCategorySelection.ifBlank { "مراكز تجارية ومحلات" },
                            cityId = storeCity.trim(),
                            localNeighborhood = storeAddress.trim(),
                            images = storePhotosList,
                            pdfFileBase64 = storePdfBase64,
                            password = storePassword,
                            workingHours = storeWorkingHours.trim(),
                            isActive = false,
                            isApproved = false,
                            productAttachmentsJson = attsJson
                        )
                        viewModel.saveStore(newStore)
                        viewModel.setJoinRequestPhone(context, cleanPhone)
                        android.widget.Toast.makeText(context, "📨 تم إرسال طلب انضمام المحل للإدارة بنجاح!", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("إرسال طلب الانضمام للمراجعة 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

    } else if (selectedCategoryTab == 2) {
        // --- 🍔 RESTAURANTS & CAFES FORM ---
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🍔 انضمام مطعم / كافيه / بوفيه",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "سجل مطعمك أو مقهاك لعرض المنيو والوجبات واستقبال الطلبات المباشرة من الزبائن بجميع المحافظات اليمنية.",
                    fontSize = 11.sp,
                    color = themeColors.textSecondary
                )
            }
        }

        // Terms Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("📋 شروط وضوابط انضمام المطاعم والكافيهات:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• التعهد التام بمعايير النظافة والاشتراطات الصحية لسلامة الأطعمة والمشروبات.\n• توفير قائمة طعام (منيو) واضحة وتحديث الأسعار المعلنة بالريال اليمني.\n• التعهد بالسرعة والجدية في تلبية وتجهيز طلبات الزبائن.", fontSize = 10.sp, color = Color.White)
            }
        }

        OutlinedTextField(
            value = restOwnerName,
            onValueChange = { restOwnerName = it },
            label = { Text("اسم المالك أو المدير المسؤول *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = restName,
            onValueChange = { restName = it },
            label = { Text("الاسم التجاري للمطعم / الكافيه *") },
            placeholder = { Text("مثال: مطعم شيباني، كافيه رويال، بوفيه السعادة") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = restPhone,
            onValueChange = { restPhone = it },
            label = { Text("رقم هاتف المطعم والواتساب *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = restDesc,
            onValueChange = { restDesc = it },
            label = { Text("وصف الوجبات والمنيو والخدمات المتاحة *") },
            placeholder = { Text("وجبات سريعة، مشويات، أطباق شعبية، مشروبات ساخنة وباردة...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = restWorkingHours,
            onValueChange = { restWorkingHours = it },
            label = { Text("ساعات الدوام اليومي (مثال: 9:00 ص - 12:00 ل) *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = restCity,
            onValueChange = { restCity = it },
            label = { Text("المحافظة اليمنية (صنعاء، عدن، تعز، حضرموت) *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = restAddress,
            onValueChange = { restAddress = it },
            label = { Text("العنوان والحي والشارع الرئيسي بالتفصيل *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Text("تصنيف المنشأة *", fontSize = 12.sp, color = themeColors.textSecondary)
        val restCats = listOf("مطاعم وجبات سريعة", "كافيهات ومقاهي", "بوفيه وجبات خفيفة", "مطاعم شعبية يمنية", "حلويات ومخبوزات")
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(restCats.size) { idx ->
                val cat = restCats[idx]
                val isSel = cat == restCategorySelection
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { restCategorySelection = cat }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(cat, fontSize = 11.sp, color = if (isSel) Color.Black else Color.White)
                }
            }
        }

        var restPassVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = restPassword,
            onValueChange = { restPassword = it },
            label = { Text("كلمة المرور *") },
            visualTransformation = if (restPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            trailingIcon = {
                IconButton(onClick = { restPassVisible = !restPassVisible }) {
                    Text(if (restPassVisible) "👁️" else "🙈", fontSize = 16.sp)
                }
            }
        )

        var restConfirmPassVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = restConfirmPassword,
            onValueChange = { restConfirmPassword = it },
            label = { Text("تأكيد كلمة المرور *") },
            visualTransformation = if (restConfirmPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            trailingIcon = {
                IconButton(onClick = { restConfirmPassVisible = !restConfirmPassVisible }) {
                    Text(if (restConfirmPassVisible) "👁️" else "🙈", fontSize = 16.sp)
                }
            }
        )

        // Photos
        Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🖼️ صور المكان والمأكولات (حد أقصى 5 صور) *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (restPhotosList.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(restPhotosList.size) { index ->
                            val photo = restPhotosList[index]
                            val bitmap = remember(photo) {
                                try {
                                    val bytes = android.util.Base64.decode(photo, android.util.Base64.DEFAULT)
                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                } catch(e: Exception) { null }
                            }
                            Box(modifier = Modifier.size(70.dp)) {
                                if (bitmap != null) {
                                    Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                }
                                Box(
                                    modifier = Modifier.size(20.dp).align(Alignment.TopEnd).background(Color.Red, shape = CircleShape)
                                        .clickable { restPhotosList = restPhotosList.filterIndexed { i, _ -> i != index } },
                                    contentAlignment = Alignment.Center
                                ) { Text("×", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
                if (restPhotosList.size < 5) {
                    Button(onClick = { storePhotosPicker.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary), modifier = Modifier.fillMaxWidth()) {
                        Text("إضافة صور المكان والوجبات (${restPhotosList.size}/5)", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { restAgreementChecked = !restAgreementChecked }) {
            Checkbox(checked = restAgreementChecked, onCheckedChange = { restAgreementChecked = it }, colors = CheckboxDefaults.colors(checkedColor = themeColors.accent))
            Text("أتعهد بالالتزام بالنظافة وجودة الغذاء وصحة الأسعار المعلنة 🍔", color = Color.White, fontSize = 11.sp)
        }

        // Product & Menu Attachments Component for Restaurants
        com.example.ui.ProductAttachmentsSection(
            attachments = storeAttachmentsList,
            onAttachmentsChanged = { storeAttachmentsList = it },
            mode = "REGISTRATION",
            themeColors = themeColors,
            departmentType = "RESTAURANT"
        )

        Button(
            onClick = {
                val missing = mutableListOf<String>()
                if (restOwnerName.trim().isEmpty()) missing.add("اسم المالك")
                if (restName.trim().isEmpty()) missing.add("اسم المطعم/الكافيه")
                if (restPhone.trim().isEmpty()) missing.add("رقم الهاتف")
                if (restDesc.trim().isEmpty()) missing.add("وصف الوجبات والمنيو")
                if (restWorkingHours.trim().isEmpty()) missing.add("ساعات الدوام")
                if (restCity.trim().isEmpty()) missing.add("المحافظة")
                if (restAddress.trim().isEmpty()) missing.add("الحي والشارع")
                if (restPassword.trim().isEmpty()) missing.add("كلمة المرور")
                if (restPhotosList.isEmpty()) missing.add("صورة واحدة على الأقل للمطعم أو المأكولات")

                if (missing.isNotEmpty()) {
                    viewModel.triggerNotification("⚠️ يرجى تعبئة الحقول الإلزامية: ${missing.joinToString("، ")}")
                } else if (restPassword != restConfirmPassword) {
                    viewModel.triggerNotification("⚠️ كلمتا المرور غير متطابقتين!")
                } else if (!restAgreementChecked) {
                    viewModel.triggerNotification("⚠️ يجب التعهد بالنظافة وجودة الأطعمة أولاً!")
                } else {
                    val cleanPhone = restPhone.trim().replace(" ", "").replace("+", "")
                    val duplicateType = viewModel.checkAndGetDuplicateAccountType(cleanPhone, "")
                    if (duplicateType != null) {
                        viewModel.triggerNotification("❌ عذراً! رقم الهاتف ($cleanPhone) مسجل بالفعل كـ ($duplicateType)!")
                    } else {
                        val attsJson = com.example.data.ProductAttachment.serializeList(storeAttachmentsList)
                        val newRest = com.example.data.StoreEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            name = restName.trim(),
                            description = restDesc.trim(),
                            ownerId = cleanPhone,
                            ownerName = restOwnerName.trim(),
                            phone = cleanPhone,
                            categoryId = "مطاعم وكافيهات",
                            cityId = restCity.trim(),
                            localNeighborhood = restAddress.trim(),
                            images = restPhotosList,
                            pdfFileBase64 = restPdfBase64,
                            password = restPassword,
                            workingHours = restWorkingHours.trim(),
                            isActive = false,
                            isApproved = false,
                            productAttachmentsJson = attsJson
                        )
                        viewModel.saveStore(newRest)
                        viewModel.setJoinRequestPhone(context, cleanPhone)
                        android.widget.Toast.makeText(context, "📨 تم إرسال طلب انضمام المطعم/الكافيه للإدارة بنجاح!", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(10.dp)
        ) {
            Text("إرسال طلب الانضمام للمراجعة 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

    } else if (selectedCategoryTab == 3) {
        // --- 🏢 REAL ESTATE FORM ---
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🏠 تسجيل طلب إدراج عقار جديد بالمنصة",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "سجل عقارك (شقة، فيلا، أرض، منزل) للبيع أو للإيجار لعرضه على آلاف الباحثين عن عقارات في جميع أنحاء اليمن.",
                    fontSize = 11.sp,
                    color = themeColors.textSecondary
                )
            }
        }

        // Terms Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("📋 شروط وضوابط إدراج العقارات والأراضي:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• أن تكون المالك المباشر للعقار أو لديك تفويض رسمي بالبيع/الإيجار.\n• صحة ودقة مواصفات العقار المكتوبة والأسعار بالريال اليمني.\n• إرفاق صور حقيقية وحديثة تعكس حالة العقار المباشرة.", fontSize = 10.sp, color = Color.White)
            }
        }

        OutlinedTextField(
            value = propOwnerName,
            onValueChange = { propOwnerName = it },
            label = { Text("الاسم الثلاثي للمالك أو المعلن العقاري *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = propPhone,
            onValueChange = { propPhone = it },
            label = { Text("رقم هاتف المالك / المعلن للتواصل *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = propTitle,
            onValueChange = { propTitle = it },
            label = { Text("عنوان الإعلان الرئيسي للعقار *") },
            placeholder = { Text("مثال: شقة ديلوكس مفروشة للإيجار في حي حدة") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = propDesc,
            onValueChange = { propDesc = it },
            label = { Text("المواصفات الكاملة (الغرف، الحمامات، المساحة، الخدمات) *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = propPrice,
            onValueChange = { propPrice = it },
            label = { Text("القيمة السعرية المطلوبة (ريال يمني) *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Text("نوع المعاملة العقارية *", fontSize = 12.sp, color = themeColors.textSecondary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("إيجار", "بيع").forEach { type ->
                val isSel = type == propTransactionType
                Button(
                    onClick = { propTransactionType = type },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSel) themeColors.accent else themeColors.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(type, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        OutlinedTextField(
            value = propCity,
            onValueChange = { propCity = it },
            label = { Text("المحافظة اليمنية (عدن، صنعاء، تعز، إب) *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = propArea,
            onValueChange = { propArea = it },
            label = { Text("تحديد المربع السكني أو المنطقة بالتفصيل *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Text("نوع المنشأة العقارية *", fontSize = 12.sp, color = themeColors.textSecondary)
        val propTypes = listOf("شقة سكنية", "فيلا متميزة", "منزل مستقل", "محل تجاري", "أرض")
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(propTypes.size) { idx ->
                val pType = propTypes[idx]
                val isSel = pType == propTypeSelection
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { propTypeSelection = pType }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(pType, fontSize = 11.sp, color = if (isSel) Color.Black else Color.White)
                }
            }
        }

        var propPassVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = propPassword,
            onValueChange = { propPassword = it },
            label = { Text("تعيين كلمة المرور لتأمين حساب العقار *") },
            visualTransformation = if (propPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            trailingIcon = {
                IconButton(onClick = { propPassVisible = !propPassVisible }) {
                    Text(if (propPassVisible) "👁️" else "🙈", fontSize = 16.sp)
                }
            }
        )

        var propConfirmPassword by remember { mutableStateOf("") }
        var propConfirmPassVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = propConfirmPassword,
            onValueChange = { propConfirmPassword = it },
            label = { Text("تأكيد كلمة المرور *") },
            visualTransformation = if (propConfirmPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            trailingIcon = {
                IconButton(onClick = { propConfirmPassVisible = !propConfirmPassVisible }) {
                    Text(if (propConfirmPassVisible) "👁️" else "🙈", fontSize = 16.sp)
                }
            }
        )

        // Photos
        Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🖼️ معرض صور العقار من الداخل والخارج (حد أقصى 5 صور) *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (propPhotosList.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(propPhotosList.size) { index ->
                            val photo = propPhotosList[index]
                            val bitmap = remember(photo) {
                                try {
                                    val bytes = android.util.Base64.decode(photo, android.util.Base64.DEFAULT)
                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                } catch(e: Exception) { null }
                            }
                            Box(modifier = Modifier.size(70.dp)) {
                                if (bitmap != null) {
                                    Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                }
                                Box(
                                    modifier = Modifier.size(20.dp).align(Alignment.TopEnd).background(Color.Red, shape = CircleShape)
                                        .clickable { propPhotosList = propPhotosList.filterIndexed { i, _ -> i != index } },
                                    contentAlignment = Alignment.Center
                                ) { Text("×", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
                if (propPhotosList.size < 5) {
                    Button(onClick = { propPhotosPicker.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary), modifier = Modifier.fillMaxWidth()) {
                        Text("إضافة صور من الاستوديو (${propPhotosList.size}/5)", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { propAgreementChecked = !propAgreementChecked }) {
            Checkbox(checked = propAgreementChecked, onCheckedChange = { propAgreementChecked = it }, colors = CheckboxDefaults.colors(checkedColor = themeColors.accent))
            Text("أؤكد ملكية العقار وصحة جميع المواصفات والأسعار المعلنة باليمن 📋", color = Color.White, fontSize = 11.sp)
        }

        Button(
            onClick = {
                val dPrice = propPrice.trim().toDoubleOrNull() ?: 0.0
                val missing = mutableListOf<String>()
                if (propOwnerName.trim().isEmpty()) missing.add("اسم المالك")
                if (propPhone.trim().isEmpty()) missing.add("رقم هاتف المالك")
                if (propTitle.trim().isEmpty()) missing.add("عنوان الإعلان")
                if (propDesc.trim().isEmpty()) missing.add("مواصفات العقار")
                if (propPrice.trim().isEmpty() || dPrice <= 0.0) missing.add("السعر بشكل صحيح")
                if (propCity.trim().isEmpty()) missing.add("المحافظة")
                if (propArea.trim().isEmpty()) missing.add("المنطقة")
                if (propPassword.trim().isEmpty()) missing.add("كلمة المرور")
                if (propPhotosList.isEmpty()) missing.add("صورة واحدة على الأقل للعقار")

                if (missing.isNotEmpty()) {
                    viewModel.triggerNotification("⚠️ يرجى تعبئة الحقول الإلزامية: ${missing.joinToString("، ")}")
                } else if (propPassword != propConfirmPassword) {
                    viewModel.triggerNotification("⚠️ كلمتا المرور غير متطابقتين!")
                } else if (!propAgreementChecked) {
                    viewModel.triggerNotification("⚠️ يجب التعهد بملكية العقار أولاً!")
                } else {
                    val cleanPhone = propPhone.trim().replace(" ", "").replace("+", "")
                    val duplicateType = viewModel.checkAndGetDuplicateAccountType(cleanPhone, "")
                    if (duplicateType != null) {
                        viewModel.triggerNotification("❌ عذراً! رقم الهاتف ($cleanPhone) مسجل بالفعل كـ ($duplicateType)!")
                    } else {
                        val newProperty = com.example.data.PropertyEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            title = propTitle.trim(),
                            description = propDesc.trim(),
                            price = dPrice,
                            type = if (propTransactionType == "بيع") "sale" else "rent",
                            propertyType = when (propTypeSelection) {
                                "شقة سكنية" -> "apartment"
                                "فيلا متميزة" -> "house"
                                "منزل مستقل" -> "house"
                                "محل تجاري" -> "shop"
                                else -> "land"
                            },
                            ownerId = cleanPhone,
                            ownerName = propOwnerName.trim(),
                            phone = cleanPhone,
                            cityId = propCity.trim(),
                            localNeighborhood = propArea.trim(),
                            images = propPhotosList,
                            pdfFileBase64 = propPdfBase64,
                            password = propPassword,
                            isActive = false,
                            isApproved = false
                        )
                        viewModel.saveProperty(newProperty)
                        viewModel.setJoinRequestPhone(context, cleanPhone)
                        android.widget.Toast.makeText(context, "📨 تم إرسال طلب إدراج العقار للإدارة بنجاح!", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(10.dp)
        ) {
            Text("إرسال طلب الانضمام للمراجعة 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

    } else if (selectedCategoryTab == 4) {
        // --- 🏥 MEDICAL CENTER & CLINIC FORM ---
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🏥 انضمام مركز طبي / عيادة / مجمع استشاري",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "سجل مركزك الطبي أو عيادتك لعرض التخصصات ومواعيد الاستقبال وحجز المواعيد الطبية المباشرة للمرضى.",
                    fontSize = 11.sp,
                    color = themeColors.textSecondary
                )
            }
        }

        // Terms Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("📋 شروط وضوابط تسجيل المراكز الطبية والعيادات:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• ترخيص رسمي ساري المفعول صادر من وزارة الصحة العامة والسكان باليمن.\n• التعهد بالرعاية الطبية العالية وأخلاقيات المهنة وصحة بيانات الأطباء.\n• تحديد مواعيد العيادة ومواعيد الطوارئ بدقة متناهية.", fontSize = 10.sp, color = Color.White)
            }
        }

        OutlinedTextField(
            value = medOwnerName,
            onValueChange = { medOwnerName = it },
            label = { Text("اسم الطبيب المباشر أو المدير المسؤول *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = medName,
            onValueChange = { medName = it },
            label = { Text("الاسم الرسمي للمركز الطبي / العيادة / الصيدلية *") },
            placeholder = { Text("مثال: مستشفى الشفاء الاستشاري، عيادة د. أحمد للأسنان") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = medPhone,
            onValueChange = { medPhone = it },
            label = { Text("رقم هاتف الاستقبال والواتساب *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = medDesc,
            onValueChange = { medDesc = it },
            label = { Text("التخصصات والأجهزة والخدمات الطبية المتاحة *") },
            placeholder = { Text("عيادة أطفال، باطنية، أسنان، مختبر، أشعة، صيدلية طوارئ 24 ساعة...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = medWorkingHours,
            onValueChange = { medWorkingHours = it },
            label = { Text("مواعيد الاستقبال والطوارئ (مثال: 8:00 ص - 8:00 م) *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = medCity,
            onValueChange = { medCity = it },
            label = { Text("المحافظة اليمنية *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = medAddress,
            onValueChange = { medAddress = it },
            label = { Text("العنوان والحي السكني والشارع *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Text("التخصص الطبي الرئيسي *", fontSize = 12.sp, color = themeColors.textSecondary)
        val medSpecs = listOf("عيادة عامة", "مركز أسنان", "عيادة أطفال", "مختبر تحاليل", "مركز أشعة", "صيدلية", "مستشفى/مجمع طبي")
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(medSpecs.size) { idx ->
                val spec = medSpecs[idx]
                val isSel = spec == medSpecialtySelection
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { medSpecialtySelection = spec }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(spec, fontSize = 11.sp, color = if (isSel) Color.Black else Color.White)
                }
            }
        }

        var medPassVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = medPassword,
            onValueChange = { medPassword = it },
            label = { Text("كلمة المرور *") },
            visualTransformation = if (medPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            trailingIcon = {
                IconButton(onClick = { medPassVisible = !medPassVisible }) {
                    Text(if (medPassVisible) "👁️" else "🙈", fontSize = 16.sp)
                }
            }
        )

        var medConfirmPassVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = medConfirmPassword,
            onValueChange = { medConfirmPassword = it },
            label = { Text("تأكيد كلمة المرور *") },
            visualTransformation = if (medConfirmPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            trailingIcon = {
                IconButton(onClick = { medConfirmPassVisible = !medConfirmPassVisible }) {
                    Text(if (medConfirmPassVisible) "👁️" else "🙈", fontSize = 16.sp)
                }
            }
        )

        // License PDF
        Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🩺 ترخيص وزارة الصحة ورخصة مزاولة المهنة (PDF) (موصى به)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (medLicenseBase64.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("📎 ${medLicenseName.ifEmpty { "الترخيص_الطبى.pdf" }}", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { medLicenseBase64 = ""; medLicenseName = "" }) {
                            Text("🗑️", fontSize = 14.sp)
                        }
                    }
                } else {
                    Button(onClick = { storeLicensePicker.launch("application/pdf") }, colors = ButtonDefaults.buttonColors(containerColor = themeColors.secondary), modifier = Modifier.fillMaxWidth()) {
                        Text("رفع الترخيص الطبي بصيغة PDF 📂", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { medAgreementChecked = !medAgreementChecked }) {
            Checkbox(checked = medAgreementChecked, onCheckedChange = { medAgreementChecked = it }, colors = CheckboxDefaults.colors(checkedColor = themeColors.accent))
            Text("أتعهد بسريان الترخيص الطبي والالتزام بأخلاقيات الرعاية الصحية 🩺", color = Color.White, fontSize = 11.sp)
        }

        Button(
            onClick = {
                val missing = mutableListOf<String>()
                if (medOwnerName.trim().isEmpty()) missing.add("اسم الطبيب/المدير")
                if (medName.trim().isEmpty()) missing.add("اسم المركز الطبي")
                if (medPhone.trim().isEmpty()) missing.add("رقم الهاتف")
                if (medDesc.trim().isEmpty()) missing.add("التخصصات المتاحة")
                if (medWorkingHours.trim().isEmpty()) missing.add("ساعات الدوام والطوارئ")
                if (medCity.trim().isEmpty()) missing.add("المحافظة")
                if (medAddress.trim().isEmpty()) missing.add("الحي والشارع")
                if (medPassword.trim().isEmpty()) missing.add("كلمة المرور")

                if (missing.isNotEmpty()) {
                    viewModel.triggerNotification("⚠️ يرجى تعبئة الحقول الإلزامية: ${missing.joinToString("، ")}")
                } else if (medPassword != medConfirmPassword) {
                    viewModel.triggerNotification("⚠️ كلمتا المرور غير متطابقتين!")
                } else if (!medAgreementChecked) {
                    viewModel.triggerNotification("⚠️ يجب التعهد بسريان الترخيص الطبي أولاً!")
                } else {
                    val cleanPhone = medPhone.trim().replace(" ", "").replace("+", "")
                    val duplicateType = viewModel.checkAndGetDuplicateAccountType(cleanPhone, "")
                    if (duplicateType != null) {
                        viewModel.triggerNotification("❌ عذراً! رقم الهاتف ($cleanPhone) مسجل بالفعل كـ ($duplicateType)!")
                    } else {
                        val newMed = com.example.data.StoreEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            name = medName.trim(),
                            description = medDesc.trim(),
                            ownerId = cleanPhone,
                            ownerName = medOwnerName.trim(),
                            phone = cleanPhone,
                            categoryId = "عيادات ومراكز طبية",
                            cityId = medCity.trim(),
                            localNeighborhood = medAddress.trim(),
                            images = medPhotosList,
                            pdfFileBase64 = medLicenseBase64,
                            password = medPassword,
                            workingHours = medWorkingHours.trim(),
                            isActive = false,
                            isApproved = false
                        )
                        viewModel.saveStore(newMed)
                        viewModel.setJoinRequestPhone(context, cleanPhone)
                        android.widget.Toast.makeText(context, "📨 تم إرسال طلب انضمام المركز الطبي للإدارة بنجاح!", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(10.dp)
        ) {
            Text("إرسال طلب الانضمام للمراجعة 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

    } else if (selectedCategoryTab == 5) {
        // --- 💼 JOB POSTING FORM ---
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "💼 نشر إعلان وظيفة / شاغر وظيفي جديد",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "أنشر وظيفتك الشاغرة للوصول للكوادر والخبرات اليمنية المتميزة وتلقي طلبات التقديم والسير الذاتية المباشرة.",
                    fontSize = 11.sp,
                    color = themeColors.textSecondary
                )
            }
        }

        // Terms Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("📋 شروط وقواعد نشر الفرص الوظيفية والشواغر:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• يمنع تقاضي أي مبالغ أو رسوم مالية من المتقدمين للوظيفة تحت أي مسمى.\n• التعهد بجدية الشاغر الوظيفي والشفافية في الراتب والمتطلبات المعلنة.\n• حفظ سرية ونزاهة بيانات المتقدمين والسير الذاتية الواردة.", fontSize = 10.sp, color = Color.White)
            }
        }

        OutlinedTextField(
            value = jobCompanyNameInput,
            onValueChange = { jobCompanyNameInput = it },
            label = { Text("اسم الشركة أو الجهة المعلنة عن الوظيفة *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = jobManagerNameInput,
            onValueChange = { jobManagerNameInput = it },
            label = { Text("اسم مسؤول التوظيف أو التواصل *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = jobTitleInput,
            onValueChange = { jobTitleInput = it },
            label = { Text("المسمى الوظيفي المطلوب *") },
            placeholder = { Text("مثال: محاسب قانوني، مهندس شبكات، موظف مبيعات") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = jobPhoneInput,
            onValueChange = { jobPhoneInput = it },
            label = { Text("رقم هاتف مسؤول التوظيف والواتساب *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = jobCityInput,
            onValueChange = { jobCityInput = it },
            label = { Text("المحافظة / المدينة المقررة للعمل *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = jobAddressInput,
            onValueChange = { jobAddressInput = it },
            label = { Text("العنوان والحي السكني مقر الشركة *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Text("نظام وطبيعة الدوام *", fontSize = 12.sp, color = themeColors.textSecondary)
        val jobTypes = listOf("دوام كامل", "دوام جزئي", "عن بعد", "بالساعة")
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(jobTypes.size) { idx ->
                val jType = jobTypes[idx]
                val isSel = jType == jobTypeInput
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { jobTypeInput = jType }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(jType, fontSize = 11.sp, color = if (isSel) Color.Black else Color.White)
                }
            }
        }

        OutlinedTextField(
            value = jobSalaryInput,
            onValueChange = { jobSalaryInput = it },
            label = { Text("الراتب المتوقع والحوافز (مثال: 180,000 ريال يمني) *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = jobDescInput,
            onValueChange = { jobDescInput = it },
            label = { Text("تفاصيل الوظيفة والمهام والمسؤوليات اليومية *") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = jobRequirementsInput,
            onValueChange = { jobRequirementsInput = it },
            label = { Text("الشروط والمؤهلات والخبرات المطلوبة *") },
            placeholder = { Text("بكالوريوس، خبرة سنتين، إتقان اللغة الإنجليزية...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        var jobPassVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = jobPasswordInput,
            onValueChange = { jobPasswordInput = it },
            label = { Text("كلمة المرور لإدارة الإعلان الوظيفي *") },
            visualTransformation = if (jobPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            trailingIcon = {
                IconButton(onClick = { jobPassVisible = !jobPassVisible }) {
                    Text(if (jobPassVisible) "👁️" else "🙈", fontSize = 16.sp)
                }
            }
        )

        var jobConfirmPassVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = jobConfirmPasswordInput,
            onValueChange = { jobConfirmPasswordInput = it },
            label = { Text("تأكيد كلمة المرور *") },
            visualTransformation = if (jobConfirmPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            trailingIcon = {
                IconButton(onClick = { jobConfirmPassVisible = !jobConfirmPassVisible }) {
                    Text(if (jobConfirmPassVisible) "👁️" else "🙈", fontSize = 16.sp)
                }
            }
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { jobAgreementChecked = !jobAgreementChecked }) {
            Checkbox(checked = jobAgreementChecked, onCheckedChange = { jobAgreementChecked = it }, colors = CheckboxDefaults.colors(checkedColor = themeColors.accent))
            Text("أتعهد بجدية الإعلان الوظيفي وعدم المطالبة بأي رسوم من المتقدمين 💼", color = Color.White, fontSize = 11.sp)
        }

        Button(
            onClick = {
                val missing = mutableListOf<String>()
                if (jobCompanyNameInput.trim().isEmpty()) missing.add("اسم الجهة/الشركة")
                if (jobManagerNameInput.trim().isEmpty()) missing.add("مسؤول التوظيف")
                if (jobTitleInput.trim().isEmpty()) missing.add("المسمى الوظيفي")
                if (jobPhoneInput.trim().isEmpty()) missing.add("رقم الهاتف")
                if (jobCityInput.trim().isEmpty()) missing.add("المافظة")
                if (jobAddressInput.trim().isEmpty()) missing.add("العنوان")
                if (jobSalaryInput.trim().isEmpty()) missing.add("الراتب المتوقع")
                if (jobDescInput.trim().isEmpty()) missing.add("تفاصيل المهام")
                if (jobRequirementsInput.trim().isEmpty()) missing.add("الشروط والمؤهلات")
                if (jobPasswordInput.trim().isEmpty()) missing.add("كلمة المرور")

                if (missing.isNotEmpty()) {
                    viewModel.triggerNotification("⚠️ يرجى تعبئة الحقول الإلزامية: ${missing.joinToString("، ")}")
                } else if (jobPasswordInput != jobConfirmPasswordInput) {
                    viewModel.triggerNotification("⚠️ كلمتا المرور غير متطابقتين!")
                } else if (!jobAgreementChecked) {
                    viewModel.triggerNotification("⚠️ يجب التعهد بجدية الإعلان الوظيفي أولاً!")
                } else {
                    val cleanPhone = jobPhoneInput.trim().replace(" ", "").replace("+", "")
                    val newJob = com.example.data.JobEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        title = jobTitleInput.trim(),
                        companyName = jobCompanyNameInput.trim(),
                        managerName = jobManagerNameInput.trim(),
                        phone = cleanPhone,
                        cityId = jobCityInput.trim(),
                        address = jobAddressInput.trim(),
                        jobType = jobTypeInput,
                        salary = jobSalaryInput.trim(),
                        description = jobDescInput.trim(),
                        requirements = jobRequirementsInput.trim(),
                        isApproved = false,
                        isActive = false
                    )
                    viewModel.saveJob(newJob)
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                    android.widget.Toast.makeText(context, "📨 تم إرسال إعلان الوظيفة للمراجعة بنجاح!", android.widget.Toast.LENGTH_LONG).show()
                    viewModel.navigateTo("JOIN_REQUEST_STATUS")
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("إرسال طلب الانضمام للمراجعة 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
}
}
