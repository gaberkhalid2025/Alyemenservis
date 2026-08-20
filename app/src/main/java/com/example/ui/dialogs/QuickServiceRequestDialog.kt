@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.dialogs

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun QuickServiceRequestScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBack: () -> Unit,
    onRequestCreated: () -> Unit
) {
    val context = LocalContext.current
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val settingsState by viewModel.settings.collectAsState()

    var nameInput by remember(currentUserName, currentUserPhone) { 
        mutableStateOf(currentUserName.ifEmpty { if (currentUserPhone.isNotBlank()) "عميل ($currentUserPhone)" else "" }) 
    }
    var phoneInput by remember(currentUserPhone) { mutableStateOf(currentUserPhone) }
    var selectedCity by remember { mutableStateOf("صنعاء") }
    var neighborhoodInput by remember { mutableStateOf("شارع الستين") }
    
    // 4 Main Sections: Services, Stores, Restaurants, Medical
    var selectedSection by remember { mutableStateOf("SERVICES") } // "SERVICES", "STORES", "RESTAURANTS", "MEDICAL"
    var selectedSpecialty by remember { mutableStateOf("كهرباء وصيانة") }
    var serviceTitleInput by remember { mutableStateOf("") }
    var orderDetailsInput by remember { mutableStateOf("") }
    var pinCodeInput by remember { mutableStateOf("") }
    var urgencyTime by remember { mutableStateOf("فوراً (خلال 30 دقيقة)") }
    var deliveryMethod by remember { mutableStateOf("توصيل") } // "توصيل", "استلام من المتجر/المطعم"

    var isSubmitting by remember { mutableStateOf(false) }
    var generatedRequestCode by remember { mutableStateOf<String?>(null) }
    var generatedSecretPin by remember { mutableStateOf<String?>(null) }

    val yemeniCities = listOf("صنعاء", "عدن", "تعز", "الحديدة", "إب", "حضرموت", "ذمار", "عمران", "صعدة", "مأرب", "شبوة", "البيضاء", "لحج", "أبين", "المهرة")

    // Category options mapping
    val servicesOptions = listOf(
        "كهرباء وصيانة", "تكييف وتبريد", "سباكة وتمديدات", 
        "دهان وديكور", "نجارة وألمنيوم", "صيانة سيارات", "إلكترونيات وهواتف", "طاقة شمسية", "أخرى"
    )
    val storesOptions = listOf(
        "إلكترونيات وأجهزة", "مواد بناء وسيراميك", "سوبرماركت وجملة", 
        "أثاث ومفروشات", "طاقة شمسية وبطاريات", "ملابس ومستلزمات", "عطور وهدايا", "أخرى"
    )
    val restaurantsOptions = listOf(
        "مشويات ومندي", "وجبات سريعة", "بيتزا ومعجنات", 
        "أطباق شعبية", "كافيه ومشروبات", "حلويات ومخبوزات", "بوفية وعصائر", "أخرى"
    )
    val medicalOptions = listOf(
        "أدوية وصيدليات", "استشارات وعيادات", "مختبرات وتحاليل",
        "مستلزمات وأجهزة طبية", "رعاية منزلية وتمريض", "بصريات ونظارات", "أخرى"
    )

    val currentSpecialties = when (selectedSection) {
        "SERVICES" -> servicesOptions
        "STORES" -> storesOptions
        "RESTAURANTS" -> restaurantsOptions
        "MEDICAL" -> medicalOptions
        else -> servicesOptions
    }

    // Auto update selected specialty when section changes
    LaunchedEffect(selectedSection) {
        selectedSpecialty = currentSpecialties.first()
    }

    Surface(
        color = themeColors.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Surface(
                color = themeColors.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "🚨 اطلب خدمتك الآن - عاجل (30 دقيقة)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                        Text(
                            text = "نظام المزاد العكسي والطلبات الفورية الموحدة ⚡",
                            fontSize = 9.5.sp,
                            color = themeColors.textSecondary
                        )
                    }
                }
            }

            // Scrollable Form Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Banner Info
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⚡", fontSize = 14.sp)
                            Text(
                                text = "سيتم إرسال طلبك لكل مقدمي الخدمة/السلع/الوجبات المتخصصين فوراً",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE047)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⏱️", fontSize = 12.sp)
                            Text("المهلة المحددة: 30 دقيقة فقط لتلقي أفضل عروض الأسعار", fontSize = 9.5.sp, color = Color(0xFFE2E8F0))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🔑", fontSize = 12.sp)
                            Text("سيتم توليد كود فريد لمتابعة طلبك وعروض الأسعار بحماية تامة", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📌", fontSize = 12.sp)
                            Text("يشمل: الخدمات والفنيين | المراكز والمتاجر | المطاعم", fontSize = 9.5.sp, color = Color(0xFF67E8F9), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 1. Phone Number Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📱 رقم الهاتف اليمني (أرقام فقط):", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            if (digitsOnly.length <= 9) phoneInput = digitsOnly
                        },
                        placeholder = { Text("77XXXXXXX / 73XXXXXXX / 71XXXXXXX", color = Color.Gray, fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = themeColors.surface,
                            unfocusedContainerColor = themeColors.surface
                        )
                    )
                }

                // 2. City & Neighborhood Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🏙️ المدينة والمحافظة:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        items(yemeniCities) { city ->
                            val isSel = selectedCity == city
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isSel) themeColors.accent else themeColors.surface)
                                    .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                                    .clickable { selectedCity = city }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = city,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📍 المنطقة / الحي:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = neighborhoodInput,
                        onValueChange = { neighborhoodInput = it },
                        placeholder = { Text("مثال: شارع الستين، حدة، المنصورة...", color = Color.Gray, fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = themeColors.surface,
                            unfocusedContainerColor = themeColors.surface
                        )
                    )
                }

                // 3. Section Selector: [الخدمات والفنيين] [المراكز والمتاجر] [المطاعم]
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📂 قسم المنشأة / الطلب:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val sectionItems = listOf(
                            Triple("SERVICES", "🔧 فنيين", Color(0xFF3B82F6)),
                            Triple("STORES", "🏬 متاجر", Color(0xFF10B981)),
                            Triple("RESTAURANTS", "🍽️ مطاعم", Color(0xFFF59E0B)),
                            Triple("MEDICAL", "🏥 طبي وصيدلي", Color(0xFFEC4899))
                        )
                        sectionItems.forEach { (secKey, secLabel, secColor) ->
                            val isSel = selectedSection == secKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) secColor else Color.Transparent)
                                    .border(1.dp, if (isSel) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedSection = secKey }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = secLabel,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 4. Specialty / Sub-type Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val sectionHeadline = when (selectedSection) {
                        "SERVICES" -> "🔧 الخدمات والفنيين (اختر التخصص):"
                        "STORES" -> "🏪 المراكز والمتاجر (نوع السلعة):"
                        "RESTAURANTS" -> "🍽️ المطاعم (نوع الوجبة/المطعم):"
                        else -> "التخصص المطلوب:"
                    }
                    Text(sectionHeadline, fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        items(currentSpecialties) { spec ->
                            val isSel = selectedSpecialty == spec
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSel) Color(0xFF10B981) else themeColors.surface)
                                    .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .clickable { selectedSpecialty = spec }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = spec,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Color.LightGray
                                )
                            }
                        }
                    }
                }

                // 5. Title / Short Request Title
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🏷️ عنوان الخدمة/السلعة/الوجبة المطلوبة:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = serviceTitleInput,
                        onValueChange = { serviceTitleInput = it },
                        placeholder = { 
                            val hint = when(selectedSection) {
                                "SERVICES" -> "مثال: إصلاح تسريب مياه / صيانة كهرباء منزلية"
                                "STORES" -> "مثال: طلب ثلاجة منزلية / شاشة ذكية / أسمنت"
                                "RESTAURANTS" -> "مثال: وجبة عشاء عائلية / مشويات مشكلة / بيتزا"
                                "MEDICAL" -> "مثال: فحص طبي واستشارة / دواء صيدلاني / تحاليل"
                                else -> "عنوان ما تحتاجه بدقة"
                            }
                            Text(hint, color = Color.Gray, fontSize = 11.sp) 
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = themeColors.surface,
                            unfocusedContainerColor = themeColors.surface
                        )
                    )
                }

                // 6. Request Details
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📝 تفاصيل الطلب:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = orderDetailsInput,
                        onValueChange = { orderDetailsInput = it },
                        placeholder = { Text("اكتب تفاصيل ما تحتاجه بالتفصيل لمساعدة المزودين على تقديم السعر الأدق...", color = Color.Gray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = themeColors.surface,
                            unfocusedContainerColor = themeColors.surface
                        )
                    )
                }

                // 7. Secret PIN Code (4 Digits)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🔒 رمز PIN لحماية وتعديل الطلب (رمز سري - 4 أرقام):", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = pinCodeInput,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 4) pinCodeInput = digits
                        },
                        placeholder = { Text("● ● ● ● (مثال: 1234)", color = Color.Gray, fontSize = 11.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = themeColors.surface,
                            unfocusedContainerColor = themeColors.surface
                        )
                    )
                }

                // 8. Urgency Time Selector: [فوراً (خلال 30 دقيقة)] [خلال ساعة] [خلال ساعتين]
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("⏱️ الوقت المطلوب للتنفيذ/التسليم:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val times = listOf("فوراً (خلال 30 دقيقة)", "خلال ساعة", "خلال ساعتين")
                        times.forEach { t ->
                            val isSel = urgencyTime == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFFEF4444) else themeColors.surface)
                                    .border(1.dp, if (isSel) Color.White else Color.DarkGray, RoundedCornerShape(8.dp))
                                    .clickable { urgencyTime = t }
                                    .padding(vertical = 8.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = t,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Color.LightGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 9. Delivery Method (For Goods and Meals only)
                if (selectedSection == "STORES" || selectedSection == "RESTAURANTS") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🚚 طريقة التسليم (للسلع والوجبات):", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val deliveryOptions = listOf("توصيل", "استلام من المتجر/المطعم")
                            deliveryOptions.forEach { opt ->
                                val isSel = deliveryMethod == opt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) Color(0xFF0284C7) else themeColors.surface)
                                        .border(1.dp, if (isSel) Color.White else Color.DarkGray, RoundedCornerShape(8.dp))
                                        .clickable { deliveryMethod = opt }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opt,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else Color.LightGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Bottom Sticky Bar with Rocket Action Button
            Surface(
                color = themeColors.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    Button(
                        onClick = {
                            val cleanP = phoneInput.trim()
                            if (cleanP.length != 9 || !(cleanP.startsWith("77") || cleanP.startsWith("73") || cleanP.startsWith("71") || cleanP.startsWith("70") || cleanP.startsWith("78"))) {
                                Toast.makeText(context, "⚠️ يرجى إدخال رقم هاتف يمني صحيح مكون من 9 أرقام (77/73/71/70/78)", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (serviceTitleInput.isBlank()) {
                                Toast.makeText(context, "⚠️ يرجى إدخال عنوان الخدمة/السلعة/الوجبة المطلوبة", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val pinToUse = if (pinCodeInput.isNotBlank()) pinCodeInput.trim() else (1000..9999).random().toString()

                            val sectionLabel = when (selectedSection) {
                                "SERVICES" -> "الخدمات والفنيين"
                                "STORES" -> "المراكز والمتاجر"
                                "RESTAURANTS" -> "المطاعم"
                                else -> "الخدمات"
                            }

                            val fullDesc = if (orderDetailsInput.isNotBlank()) {
                                "$orderDetailsInput | [تخصص: $selectedSpecialty] | [المهلة: $urgencyTime] ${if(selectedSection != "SERVICES") "| التسليم: $deliveryMethod" else ""}"
                            } else {
                                "طلب عاجل [$selectedSpecialty] في $selectedCity ($neighborhoodInput) | المهلة: $urgencyTime"
                            }

                            isSubmitting = true

                            viewModel.createInstantRequest(
                                userId = currentUserId.ifBlank { "user_${cleanP}" },
                                userName = nameInput.ifBlank { "عميل ($cleanP)" },
                                userPhone = cleanP,
                                userCity = selectedCity,
                                userNeighborhood = neighborhoodInput,
                                categoryId = selectedSection,
                                categoryName = "$sectionLabel - $selectedSpecialty",
                                serviceTitle = serviceTitleInput.trim(),
                                description = fullDesc,
                                images = emptyList(),
                                urgencyTime = urgencyTime,
                                deliveryMethod = if (selectedSection != "SERVICES") deliveryMethod else "",
                                customPin = pinToUse,
                                onResult = { success, reqCode, secretPin ->
                                    isSubmitting = false
                                    if (success) {
                                        generatedRequestCode = reqCode
                                        generatedSecretPin = secretPin
                                    } else {
                                        Toast.makeText(context, "❌ حدث خطأ أثناء إرسال الطلب، يرجى المحاولة ثانية", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ الإرسال وإطلاق المزايدة...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🚀 إرسال الطلب وإطلاق المزايدة (30 دقيقة)", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Success Modal with Request Code, PIN, Copy feature and Navigation
    if (generatedRequestCode != null) {
        AlertDialog(
            onDismissRequest = {
                generatedRequestCode = null
                onRequestCreated()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🚀", fontSize = 24.sp)
                    Text("تم إطلاق طلبك العاجل بنجاح!", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "تم توجيه طلبك فوراً لكافة المزودين المتخصصين في $selectedCity، وسيبدأ وصول العروض خلال دقائق معدودة.",
                        fontSize = 11.5.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        color = themeColors.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, themeColors.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("كود متابعة الطلب الفريد:", fontSize = 10.sp, color = Color.Gray)
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
                                    Toast.makeText(context, "📋 تم نسخ الكود والرمز السري إلى الحافظة بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("📋 نسخ الكود للحافظة", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
