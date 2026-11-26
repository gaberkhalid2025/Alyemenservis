@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.ui.dialogs

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.ui.helpers.AppPreferenceHelper
import com.example.utils.FirebaseStorageUploader
import com.example.utils.LandmarkAddress
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.UUID

/**
 * 🌟 3-Step Wizard for "اطلب خدمتك الآن" (Reverse Auction & Instant Requests)
 * Step 1: Category & Specialty (QuickServiceRequestStep1.kt)
 * Step 2: Location & Details (QuickServiceRequestStep2.kt)
 * Step 3: Contact & Submission (QuickServiceRequestStep3.kt)
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
    var selectedSection by remember { mutableStateOf("SERVICES") }
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
                                if (currentStep > 1) currentStep-- else onBack()
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
                                    when (currentStep) {
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
                    1 -> Step1CategorySection(
                        selectedSection = selectedSection,
                        onSectionSelect = { selectedSection = it },
                        selectedSpecialty = selectedSpecialty,
                        onSpecialtySelect = { selectedSpecialty = it },
                        specialtiesList = currentSpecialties,
                        themeColors = themeColors
                    )
                    2 -> Step2LocationAndDetails(
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
                    3 -> Step3ContactAndSubmit(
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

            // Bottom Navigation Actions Dock
            Surface(
                color = Color(0xFF1E293B),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
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
                                1 -> currentStep = 2
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

                                    val secureRandom = SecureRandom()
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

    val code = generatedRequestCode
    if (code != null) {
        QuickServiceSuccessDialog(
            selectedCity = selectedCity,
            requestCode = code,
            secretPin = generatedSecretPin,
            themeColors = themeColors,
            onDone = {
                generatedRequestCode = null
                onRequestCreated()
            }
        )
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
