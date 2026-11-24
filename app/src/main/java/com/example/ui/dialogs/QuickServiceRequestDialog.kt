@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.dialogs
import com.example.ui.*

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.ui.components.InteractiveMapLocationPickerDialog
import com.example.ui.components.ManualCoordinateDialog
import com.example.ui.components.buildFormattedLocationAddress
import com.example.ui.helpers.AppPreferenceHelper
import com.example.ui.screens.map.LandmarkAddressScreen
import com.example.utils.FirebaseStorageUploader
import com.example.utils.LandmarkAddress
import com.example.utils.LandmarkAddressStore
import com.example.utils.OfflineYemenGazetteer
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 🌟 3-Step Wizard for "اطلب خدمتك الآن" (Reverse Auction & Instant Requests)
 * Step 1: Category & Specialty (Big visual cards)
 * Step 2: Location & Details (Quick city selection + neighborhood + description)
 * Step 3: Contact & Submission (Auto-filled phone + PIN + prominent submit CTA)
 */
@Composable
fun QuickServiceRequestScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBack: () -> Unit,
    onRequestCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val dynamicCategories by viewModel.categories.collectAsState()
    val userLat by viewModel.userLatitude.collectAsState()
    val userLng by viewModel.userLongitude.collectAsState()
    val isRealLoc by viewModel.isUserLocationReal.collectAsState()
    val hasGps = isRealLoc && LandmarkAddress.isValid(userLat, userLng)

    // Wizard Step State (1, 2, 3)
    var currentStep by remember { mutableIntStateOf(1) }

    // Form inputs
    var nameInput by remember(currentUserName, currentUserPhone) { 
        mutableStateOf(currentUserName.ifEmpty { if (currentUserPhone.isNotBlank()) "عميل ($currentUserPhone)" else "" }) 
    }
    var phoneInput by remember(currentUserPhone) { mutableStateOf(currentUserPhone) }
    var selectedCity by remember { mutableStateOf("صنعاء") }
    var neighborhoodInput by remember { mutableStateOf("") }
    var pickedLat by remember { mutableDoubleStateOf(0.0) }
    var pickedLng by remember { mutableDoubleStateOf(0.0) }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 5 Main Sections + Dynamic Admin Categories
    var selectedSection by remember { mutableStateOf("SERVICES") } // "SERVICES", "STORES", "RESTAURANTS", "MEDICAL", "PROPERTIES"
    var selectedSpecialty by remember { mutableStateOf("كهرباء وصيانة") }
    var serviceTitleInput by remember { mutableStateOf("") }
    var orderDetailsInput by remember { mutableStateOf("") }
    var pinCodeInput by remember { mutableStateOf("") }
    var urgencyTime by remember { mutableStateOf("فوراً (خلال 30 دقيقة)") }
    var deliveryMethod by remember { mutableStateOf("توصيل للموقع") }

    var isSubmitting by remember { mutableStateOf(false) }
    var generatedRequestCode by remember { mutableStateOf<String?>(null) }
    var generatedSecretPin by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isSubmitting) {
        if (isSubmitting) {
            delay(35_000L)
            isSubmitting = false
        }
    }

    val yemeniCities = listOf(
        "صنعاء", "عدن", "تعز", "الحديدة", "إب", "حضرموت", 
        "ذمار", "مأرب", "عمران", "صعدة", "شبوة", "لحج", "أبين", "المهرة", "البيضاء"
    )

    // Category options mapping covering all app sectors
    val servicesOptions = listOf(
        "كهرباء وصيانة", "سباكة وتمديدات", "تكييف وتبريد", "طاقة شمسية وبطاريات",
        "دهان وديكور", "نجارة وألمنيوم", "صيانة سيارات", "إلكترونيات وهواتف", "تنظيف ونقل أثاث", "أخرى"
    )
    val restaurantsOptions = listOf(
        "مشويات ومندي", "وجبات سريعة وبرجر", "بيتزا ومعجنات", 
        "أطباق شعبية يمنية", "كافيه ومشروبات", "حلويات ومخبوزات", "بوفية وعصائر", "أخرى"
    )
    val storesOptions = listOf(
        "إلكترونيات وأجهزة", "سوبرماركت ومواد غذائية", "مواد بناء وسيراميك",
        "أثاث ومفروشات منزلية", "طاقة شمسية وبطاريات", "ملابس ومستلزمات", "قطع غيار وزيوت", "أخرى"
    )
    val medicalOptions = listOf(
        "أدوية وصيدليات", "استشارات وعيادات تخصصية", "مختبرات وتحاليل طبية",
        "مستلزمات وأجهزة طبية", "رعاية منزلية وتمريض", "بصريات ونظارات", "أخرى"
    )
    val propertiesOptions = listOf(
        "شقة للإيجار", "شقة أو بيت للتمليك", "شقق مفروشة وفندقية",
        "محل أو مكتب تجاري", "أرض أو فيلا", "استراحة أو سكن شبابي", "أخرى"
    )

    val adminCustomForSection = remember(dynamicCategories, selectedSection) {
        dynamicCategories
            .filter { cat ->
                cat.name.isNotBlank() && (
                    cat.parentId.equals(selectedSection, ignoreCase = true) ||
                        (selectedSection == "SERVICES" && cat.parentId.isBlank())
                    )
            }
            .map { it.name.trim() }
            .distinct()
    }

    val currentSpecialties = remember(selectedSection, adminCustomForSection) {
        val base = when (selectedSection) {
            "SERVICES" -> servicesOptions
            "RESTAURANTS" -> restaurantsOptions
            "STORES" -> storesOptions
            "MEDICAL" -> medicalOptions
            "PROPERTIES" -> propertiesOptions
            else -> servicesOptions
        }
        (adminCustomForSection + base).distinct()
    }

    LaunchedEffect(selectedSection) {
        if (!currentSpecialties.contains(selectedSpecialty)) {
            selectedSpecialty = currentSpecialties.first()
        }
        deliveryMethod = when (selectedSection) {
            "PROPERTIES" -> "معاينة ميدانية"
            "SERVICES" -> "زيارة فنية للموقع"
            else -> "توصيل للموقع"
        }
    }

    BackHandler {
        if (currentStep > 1) {
            currentStep--
        } else {
            onBack()
        }
    }

    Surface(
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Header Bar
            Surface(
                color = Color(0xFF1E293B),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentStep > 1) {
                                    currentStep--
                                } else {
                                    onBack()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚡ اطلب خدمتك الآن (المزاد العكسي)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                            Text(
                                text = "خطوة $currentStep من 3: ${
                                    when(currentStep) {
                                        1 -> "اختر القسم والتخصص"
                                        2 -> "حدد الموقع وتفاصيل الطلب"
                                        else -> "بيانات التواصل والإرسال"
                                    }
                                }",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Indicator Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (step in 1..3) {
                            val isActive = currentStep >= step
                            val isCurrent = currentStep == step
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            isCurrent -> Color(0xFFEF4444)
                                            isActive -> Color(0xFF10B981)
                                            else -> Color.DarkGray.copy(alpha = 0.5f)
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            // Wizard Step Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentStep) {
                    1 -> {
                        // Step 1: Category & Specialty Selection
                        Step1CategorySection(
                            selectedSection = selectedSection,
                            onSectionSelect = { selectedSection = it },
                            selectedSpecialty = selectedSpecialty,
                            onSpecialtySelect = { selectedSpecialty = it },
                            specialtiesList = currentSpecialties,
                            themeColors = themeColors
                        )
                    }
                    2 -> {
                        // Step 2: Location, GPS, Photo & Details
                        Step2LocationAndDetails(
                            citiesList = yemeniCities,
                            selectedCity = selectedCity,
                            onCitySelect = { selectedCity = it },
                            neighborhood = neighborhoodInput,
                            onNeighborhoodChange = { neighborhoodInput = it },
                            pickedLat = pickedLat,
                            pickedLng = pickedLng,
                            onCoordsPicked = { lat, lng ->
                                pickedLat = lat
                                pickedLng = lng
                            },
                            attachedImageUri = attachedImageUri,
                            onImageSelected = { attachedImageUri = it },
                            serviceTitle = serviceTitleInput,
                            onServiceTitleChange = { serviceTitleInput = it },
                            orderDetails = orderDetailsInput,
                            onOrderDetailsChange = { orderDetailsInput = it },
                            urgencyTime = urgencyTime,
                            onUrgencyChange = { urgencyTime = it },
                            deliveryMethod = deliveryMethod,
                            onDeliveryMethodChange = { deliveryMethod = it },
                            selectedSection = selectedSection,
                            gpsLat = userLat,
                            gpsLng = userLng,
                            hasGps = hasGps,
                            themeColors = themeColors
                        )
                    }
                    3 -> {
                        // Step 3: Contact Info & Review Summary
                        Step3ContactAndSubmit(
                            phone = phoneInput,
                            onPhoneChange = { phoneInput = it },
                            name = nameInput,
                            onNameChange = { nameInput = it },
                            pin = pinCodeInput,
                            onPinChange = { pinCodeInput = it },
                            selectedSection = selectedSection,
                            selectedSpecialty = selectedSpecialty,
                            selectedCity = selectedCity,
                            neighborhood = neighborhoodInput,
                            hasExactCoords = LandmarkAddress.isValid(pickedLat, pickedLng) || hasGps,
                            hasAttachment = attachedImageUri != null,
                            deliveryMethod = deliveryMethod,
                            serviceTitle = serviceTitleInput,
                            urgencyTime = urgencyTime,
                            themeColors = themeColors
                        )
                    }
                }
            }

            // Bottom Navigation Actions Dock
            Surface(
                color = Color(0xFF1E293B),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier
                                .weight(0.35f)
                                .height(48.dp)
                        ) {
                            Text("السابق", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            when (currentStep) {
                                1 -> {
                                    currentStep = 2
                                }
                                2 -> {
                                    if (serviceTitleInput.isBlank()) {
                                        Toast.makeText(context, "⚠️ يرجى كتابة عنوان أو نوع الخدمة المطلوبة للمتابعة", Toast.LENGTH_SHORT).show()
                                    } else {
                                        currentStep = 3
                                    }
                                }
                                3 -> {
                                    val cleanP = AppPreferenceHelper.normalizePhoneNumber(phoneInput).trim()
                                    if (cleanP.length != 9 || !cleanP.all { it.isDigit() } || !(cleanP.startsWith("77") || cleanP.startsWith("73") || cleanP.startsWith("71") || cleanP.startsWith("70") || cleanP.startsWith("78"))) {
                                        Toast.makeText(context, "⚠️ يرجى إدخال رقم هاتف يمني صحيح مكون من 9 أرقام (77/73/71/70/78)", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }

                                    val secureRandom = java.security.SecureRandom()
                                    val pinToUse = if (pinCodeInput.isNotBlank()) pinCodeInput.trim() else (1000 + secureRandom.nextInt(9000)).toString()
                                    val sectionLabel = when (selectedSection) {
                                        "SERVICES" -> "الخدمات والفنيين"
                                        "RESTAURANTS" -> "المطاعم والكافيهات"
                                        "STORES" -> "المتاجر والسلع والمحلات"
                                        "MEDICAL" -> "المراكز الطبية والصيدليات"
                                        "PROPERTIES" -> "العقارات والشقق والأراضي"
                                        else -> "الخدمات العامة"
                                    }

                                    val fullDesc = if (orderDetailsInput.isNotBlank()) {
                                        "$orderDetailsInput | [تخصص: $selectedSpecialty] | [المهلة: $urgencyTime] ${if (selectedSection != "SERVICES") "| التسليم/المعاينة: $deliveryMethod" else ""}"
                                    } else {
                                        "طلب عاجل [$selectedSpecialty] في $selectedCity (${neighborhoodInput.ifBlank { "المدينة" }}) | المهلة: $urgencyTime"
                                    }

                                    val finalLat = when {
                                        LandmarkAddress.isValid(pickedLat, pickedLng) -> pickedLat
                                        hasGps -> userLat
                                        else -> 0.0
                                    }
                                    val finalLng = when {
                                        LandmarkAddress.isValid(pickedLat, pickedLng) -> pickedLng
                                        hasGps -> userLng
                                        else -> 0.0
                                    }

                                    isSubmitting = true
                                    scope.launch {
                                        val imgUri = attachedImageUri
                                        val uploadedImages = if (imgUri != null) {
                                            val path = "urgent_requests/req_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.webp"
                                            val uploadRes = runCatching {
                                                FirebaseStorageUploader.uploadImageUri(
                                                    context = context,
                                                    uri = imgUri,
                                                    storagePath = path,
                                                    maxDimension = 900,
                                                    maxSizeBytes = 350 * 1024L
                                                )
                                            }.getOrNull()
                                            val url = uploadRes?.getOrDefault("").orEmpty()
                                            if (url.isNotBlank()) listOf(url) else emptyList()
                                        } else {
                                            emptyList()
                                        }

                                        viewModel.createInstantRequest(
                                            userId = currentUserId.ifBlank { "user_${cleanP}" },
                                            userName = nameInput.ifBlank { "عميل ($cleanP)" },
                                            userPhone = cleanP,
                                            userCity = selectedCity,
                                            userNeighborhood = neighborhoodInput.ifBlank { "وسط المدينة" },
                                            categoryId = selectedSection,
                                            categoryName = "$sectionLabel - ${if (currentSpecialties.contains(selectedSpecialty)) selectedSpecialty else currentSpecialties.first()}",
                                            serviceTitle = serviceTitleInput.trim(),
                                            description = fullDesc,
                                            images = uploadedImages,
                                            urgencyTime = urgencyTime,
                                            deliveryMethod = if (selectedSection != "SERVICES") deliveryMethod else "",
                                            customPin = pinToUse,
                                            latitude = finalLat,
                                            longitude = finalLng,
                                            sectionType = selectedSection,
                                            onResult = { success, reqCode, secretPin ->
                                                isSubmitting = false
                                                if (success) {
                                                    viewModel.setCurrentUserPhone(cleanP)
                                                    generatedRequestCode = reqCode
                                                    generatedSecretPin = secretPin
                                                    Toast.makeText(context, "🔔 تم إطلاق طلبك العاجل بنجاح برقم ($reqCode)!\nتمت إضافته لشاشة 'طلباتي' وتنبيه المختصين.", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "❌ حدث خطأ أثناء إرسال الطلب، يرجى المحاولة ثانية", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStep == 3) Color(0xFF10B981) else Color(0xFFEF4444)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(if (currentStep > 1) 0.65f else 1f)
                            .height(48.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ الإرسال وإطلاق المزاد...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            val btnText = when (currentStep) {
                                1 -> "متابعة للموقع والتفاصيل ➡️"
                                2 -> "متابعة لبيانات التواصل ➡️"
                                else -> "🚀 إرسال الطلب واستقبال العروض الآن"
                            }
                            Text(btnText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Success Confirmation Modal
    if (generatedRequestCode != null) {
        AlertDialog(
            onDismissRequest = {
                generatedRequestCode = null
                onRequestCreated()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🚀", fontSize = 24.sp)
                    Text("تم إطلاق طلبك بنجاح!", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "تم توجيه طلبك فوراً للمزودين والمتخصصين في $selectedCity، وستصلك العروض والمزايدات مباشرة على حسابك.",
                        fontSize = 11.5.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, themeColors.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("كود متابعة الطلب:", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = generatedRequestCode ?: "",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            if (generatedSecretPin != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("الرمز السري (PIN):", fontSize = 10.sp, color = Color.Gray)
                                    Text(generatedSecretPin ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Request Code", "${generatedRequestCode} (PIN: ${generatedSecretPin})")
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(context, "📋 تم نسخ كود الطلب للحافظة!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ الكود للحافظة", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("يمكنك متابعة العروض المقدمة من قسم 'طلباتي' في أي وقت.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        generatedRequestCode = null
                        onRequestCreated()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("الانتقال إلى طلباتي 📋", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        )
    }
}

// ----------------------------------------------------
// Step 1: Clean Visual Category & Specialty Cards
// ----------------------------------------------------
@Composable
private fun Step1CategorySection(
    selectedSection: String,
    onSectionSelect: (String) -> Unit,
    selectedSpecialty: String,
    onSpecialtySelect: (String) -> Unit,
    specialtiesList: List<String>,
    themeColors: VisualThemePalette
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "1️⃣ اختر القسم الرئيسي المناسب لطلبك:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // 5 Big distinct visual cards covering ALL app sectors
        val categoriesData = listOf(
            Triple("SERVICES", "🔧 الخدمات والفنيين والصيانة", "صيانة منازل، سباكة، كهرباء، طاقة شمسية، تكييف وورش"),
            Triple("STORES", "🏬 المتاجر والسلع والمحلات", "أجهزة، سوبرماركت، مواد بناء، أثاث، ملابس، قطع غيار"),
            Triple("RESTAURANTS", "🍽️ المطاعم والكافيهات والوجبات", "وجبات سريعة، مشويات ومندي، أطباق شعبية وكافيهات"),
            Triple("MEDICAL", "🏥 المراكز الطبية والصيدليات", "أدوية وصيدليات، عيادات، تحاليل ورعاية منزلية"),
            Triple("PROPERTIES", "🏢 العقارات والشقق والأراضي", "شقق إيجار أو تمليك، فلل، أراضي، محلات ومكاتب، استراحات")
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            categoriesData.forEach { (id, title, subtitle) ->
                val isSelected = selectedSection == id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSectionSelect(id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF131D2E)
                    ),
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) Color(0xFFEF4444) else Color.White.copy(alpha = 0.1f)
                    )
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color(0xFFEF4444).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(title.take(2), fontSize = 20.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title.drop(2).trim(),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFFEF4444) else Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                fontSize = 10.5.sp,
                                color = Color.LightGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onSectionSelect(id) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFFEF4444),
                                unselectedColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sub-specialties pills
        Text(
            text = "👇 اختر التخصص الدقيق:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.accent
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            specialtiesList.forEach { specialty ->
                val isSelected = selectedSpecialty == specialty
                FilterChip(
                    selected = isSelected,
                    onClick = { onSpecialtySelect(specialty) },
                    label = {
                        Text(
                            text = specialty,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEF4444),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color.LightGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) Color(0xFFEF4444) else Color.DarkGray
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// Step 2: Location & Service Details
// ----------------------------------------------------
@Composable
private fun Step2LocationAndDetails(
    citiesList: List<String>,
    selectedCity: String,
    onCitySelect: (String) -> Unit,
    neighborhood: String,
    onNeighborhoodChange: (String) -> Unit,
    pickedLat: Double,
    pickedLng: Double,
    onCoordsPicked: (Double, Double) -> Unit,
    attachedImageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    serviceTitle: String,
    onServiceTitleChange: (String) -> Unit,
    orderDetails: String,
    onOrderDetailsChange: (String) -> Unit,
    urgencyTime: String,
    onUrgencyChange: (String) -> Unit,
    deliveryMethod: String,
    onDeliveryMethodChange: (String) -> Unit,
    selectedSection: String,
    gpsLat: Double,
    gpsLng: Double,
    hasGps: Boolean,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val landmarkStore = remember { LandmarkAddressStore(context.applicationContext) }
    var savedAddresses by remember { mutableStateOf<List<LandmarkAddress>>(emptyList()) }
    var showMapPicker by remember { mutableStateOf(false) }
    var showCoordDialog by remember { mutableStateOf(false) }
    var showLandmarkWizard by remember { mutableStateOf(false) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) onImageSelected(uri)
    }
    LaunchedEffect(showLandmarkWizard) {
        savedAddresses = withContext(Dispatchers.IO) { landmarkStore.getAll() }
    }
    val hasPickedCoords = LandmarkAddress.isValid(pickedLat, pickedLng)
    val effectiveLat = if (hasPickedCoords) pickedLat else gpsLat
    val effectiveLng = if (hasPickedCoords) pickedLng else gpsLng

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "2️⃣ حدد المحافظة والمنطقة:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Quick City Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(citiesList) { city ->
                val isSelected = selectedCity == city
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF10B981) else Color(0xFF1E293B))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onCitySelect(city) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "📍 $city",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White
                    )
                }
            }
        }

        // Neighborhood text field
        OutlinedTextField(
            value = neighborhood,
            onValueChange = onNeighborhoodChange,
            label = { Text("الحي / الشارع / المعلم أو الموقع الدقيق") },
            placeholder = { Text("اكتب موقعك أو حدده على الخريطة ليصل المندوب/الفني بالضبط") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B)
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { showMapPicker = true },
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("📍 من الخريطة", fontSize = 11.sp, color = Color(0xFF10B981), maxLines = 1)
            }
            OutlinedButton(
                onClick = { showCoordDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("🔢 إحداثيات", fontSize = 11.sp, color = Color(0xFF00E5FF), maxLines = 1)
            }
            OutlinedButton(
                onClick = { showLandmarkWizard = true },
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("🏠 بالمعلم", fontSize = 11.sp, color = Color(0xFFFBBF24), maxLines = 1)
            }
        }

        if (hasPickedCoords || hasGps) {
            Surface(
                color = Color(0xFF064E3B).copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (hasPickedCoords) {
                        "✅ تم تثبيت إحداثيات الطلب: ${OfflineYemenGazetteer.formatCoordinates(pickedLat, pickedLng)}"
                    } else {
                        "📡 سيتم إرفاق إحداثيات GPS الحالية تلقائياً (${OfflineYemenGazetteer.formatCoordinates(gpsLat, gpsLng)})"
                    },
                    color = Color(0xFFA7F3D0),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        if (savedAddresses.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(savedAddresses) { addr ->
                    SuggestionChip(
                        onClick = {
                            onCoordsPicked(addr.pointLat, addr.pointLng)
                            onNeighborhoodChange("${addr.describe()} (${OfflineYemenGazetteer.formatCoordinates(addr.pointLat, addr.pointLng)})")
                        },
                        label = { Text("🏠 ${addr.label} (${addr.landmarkName})", fontSize = 10.sp) }
                    )
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        Text(
            text = "📝 تفاصيل ما تحتاجه بدقة:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Service Title
        OutlinedTextField(
            value = serviceTitle,
            onValueChange = onServiceTitleChange,
            label = { Text("عنوان الطلب / السلعة / الخدمة (مطلوب) *") },
            placeholder = { Text("مثال: صيانة تسريب مياه / طلب مواد غذائية / شقة للإيجار") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFEF4444),
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B)
            )
        )

        // Details / Description
        OutlinedTextField(
            value = orderDetails,
            onValueChange = onOrderDetailsChange,
            label = { Text("شرح إضافي / ملاحظات ومواصفات") },
            placeholder = { Text("اكتب الكميات أو المواصفات أو التفاصيل المطلوبة لتصلك عروض أسعار دقيقة...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFEF4444),
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B)
            )
        )

        // Optional Photo Attachment
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📷 إرفاق صورة توضيحية (اختياري)", fontSize = 11.5.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(if (attachedImageUri != null) "تغيير الصورة" else "اختيار صورة", fontSize = 11.sp, color = themeColors.accent)
                    }
                }
                if (attachedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = attachedImageUri,
                            contentDescription = "معاينة الصورة المرفقة",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { onImageSelected(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إزالة الصورة", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Delivery / Inspection Method for Stores, Restaurants, Medical, Real Estate
        if (selectedSection != "SERVICES") {
            val deliveryOptions = if (selectedSection == "PROPERTIES") {
                listOf("معاينة ميدانية", "تواصل واتفاق مباشر")
            } else {
                listOf("توصيل للموقع", "استلام من المحل")
            }
            Text(
                text = if (selectedSection == "PROPERTIES") "🏠 طريقة المتابعة والمعاينة:" else "🛵 طريقة التوصيل والاستلام:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                deliveryOptions.forEach { method ->
                    val isSel = deliveryMethod == method
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) Color(0xFF10B981) else Color(0xFF1E293B))
                            .border(1.dp, if (isSel) Color(0xFF10B981) else Color.DarkGray, RoundedCornerShape(8.dp))
                            .clickable { onDeliveryMethodChange(method) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = method,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Urgency selector
        Text(
            text = "⏱️ وقت التنفيذ المطلوب:",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.accent
        )
        val urgencies = listOf("فوراً (خلال 30 دقيقة)", "اليوم خلال ساعات", "تحديد موعد لاحق")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            urgencies.forEach { opt ->
                val isSel = urgencyTime == opt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) Color(0xFFEF4444) else Color(0xFF1E293B))
                        .clickable { onUrgencyChange(opt) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showMapPicker) {
        InteractiveMapLocationPickerDialog(
            initialLat = if (LandmarkAddress.isValid(effectiveLat, effectiveLng)) effectiveLat else 15.3694,
            initialLng = if (LandmarkAddress.isValid(effectiveLat, effectiveLng)) effectiveLng else 44.1910,
            hasGps = hasGps,
            gpsLat = gpsLat,
            gpsLng = gpsLng,
            title = "📍 حدد موقع التوصيل / الخدمة على الخريطة",
            onDismiss = { showMapPicker = false },
            onLocationPicked = { res ->
                onCoordsPicked(res.latitude, res.longitude)
                onNeighborhoodChange(res.formattedAddress)
                if (res.note.isNotBlank() && orderDetails.isBlank()) onOrderDetailsChange(res.note)
                showMapPicker = false
            }
        )
    }

    if (showCoordDialog) {
        ManualCoordinateDialog(
            initialLat = if (LandmarkAddress.isValid(effectiveLat, effectiveLng)) effectiveLat else 15.3694,
            initialLng = if (LandmarkAddress.isValid(effectiveLat, effectiveLng)) effectiveLng else 44.1910,
            gpsLat = gpsLat,
            gpsLng = gpsLng,
            hasGps = hasGps,
            title = "🔢 إحداثيات موقع الطلب",
            onDismiss = { showCoordDialog = false },
            onConfirm = { lat, lng, summary ->
                onCoordsPicked(lat, lng)
                onNeighborhoodChange(buildFormattedLocationAddress(lat, lng, nearestSummary = summary))
                showCoordDialog = false
            }
        )
    }

    if (showLandmarkWizard) {
        Dialog(
            onDismissRequest = { showLandmarkWizard = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            LandmarkAddressScreen(
                centerLat = if (LandmarkAddress.isValid(effectiveLat, effectiveLng)) effectiveLat else 15.3694,
                centerLng = if (LandmarkAddress.isValid(effectiveLat, effectiveLng)) effectiveLng else 44.1910,
                hasRealLocation = hasGps,
                selectedCity = selectedCity,
                onSaved = { addr ->
                    onCoordsPicked(addr.pointLat, addr.pointLng)
                    onNeighborhoodChange("${addr.describe()} (${OfflineYemenGazetteer.formatCoordinates(addr.pointLat, addr.pointLng)})")
                },
                onClose = { showLandmarkWizard = false }
            )
        }
    }
}

// ----------------------------------------------------
// Step 3: Contact & Submission Summary
// ----------------------------------------------------
@Composable
private fun Step3ContactAndSubmit(
    phone: String,
    onPhoneChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    selectedSection: String,
    selectedSpecialty: String,
    selectedCity: String,
    neighborhood: String,
    hasExactCoords: Boolean,
    hasAttachment: Boolean,
    deliveryMethod: String,
    serviceTitle: String,
    urgencyTime: String,
    themeColors: VisualThemePalette
) {
    val sectionArabic = when (selectedSection) {
        "SERVICES" -> "الخدمات والفنيين"
        "STORES" -> "المتاجر والسلع والمحلات"
        "RESTAURANTS" -> "المطاعم والكافيهات"
        "MEDICAL" -> "المراكز الطبية والصيدليات"
        "PROPERTIES" -> "العقارات والشقق والأراضي"
        else -> "عام"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "3️⃣ بيانات التواصل ومراجعة الطلب:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Phone Input (Auto-filled)
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("رقم الهاتف للتواصل واستلام العروض (9 أرقام) *") },
            placeholder = { Text("مثال: 771234567") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            leadingIcon = {
                Text("🇾🇪 +967", fontSize = 11.sp, color = themeColors.accent, modifier = Modifier.padding(start = 8.dp))
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B)
            )
        )

        // Name input
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("الاسم أو اللقب (اختياري)") },
            placeholder = { Text("مثال: أبو محمد") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B)
            )
        )

        // PIN (Optional)
        OutlinedTextField(
            value = pin,
            onValueChange = onPinChange,
            label = { Text("رمز PIN السري لمتابعة الطلب (اختياري - 4 أرقام)") },
            placeholder = { Text("إذا تركته فارغاً سيتم توليد رمز سري تلقائياً") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B)
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Summary Review Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📋", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ملخص طلبك قبل الإطلاق:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Text("🏷️ القسم والتخصص: $sectionArabic — $selectedSpecialty", fontSize = 11.sp, color = Color.White)
                Text("📍 الموقع: $selectedCity ${if (neighborhood.isNotBlank()) "($neighborhood)" else ""}", fontSize = 11.sp, color = Color.White)
                if (hasExactCoords) {
                    Text("🛰️ الإحداثيات الدقيقة: مرفقة لتحديد أقرب مقدمي الخدمة", fontSize = 10.5.sp, color = Color(0xFF34D399))
                }
                Text("📝 عنوان الطلب: ${serviceTitle.ifBlank { "طلب خدمة" }}", fontSize = 11.sp, color = Color.White)
                if (selectedSection != "SERVICES" && deliveryMethod.isNotBlank()) {
                    Text("🛵 طريقة الاستلام/المعاينة: $deliveryMethod", fontSize = 11.sp, color = Color.LightGray)
                }
                if (hasAttachment) {
                    Text("📷 المرفقات: صورة توضيحية جاهزة للرفع", fontSize = 10.5.sp, color = Color(0xFF38BDF8))
                }
                Text("⏱️ وقت التنفيذ: $urgencyTime", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickServiceRequestDialog(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit,
    onRequestCreated: () -> Unit
) {
    QuickServiceRequestScreen(
        viewModel = viewModel,
        themeColors = themeColors,
        onBack = onDismiss,
        onRequestCreated = onRequestCreated
    )
}
