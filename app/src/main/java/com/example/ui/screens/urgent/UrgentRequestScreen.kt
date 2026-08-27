package com.example.ui.screens.urgent

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.InstantRequestEntity
import com.example.ui.MainViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

/**
 * 🚨 UrgentRequestScreen
 * شاشة طلب خدمة عاجلة خلال 30 دقيقة مع مؤقت فوري وتنبيهات أولوية قصوى
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentRequestScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToUrgentList: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestore = remember { FirebaseFirestore.getInstance() }

    val currentUserId by viewModel.currentUserId.collectAsState()

    var customerPhone by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var selectedDepartment by remember { mutableStateOf("خدمات وفنيين") }
    var selectedCategory by remember { mutableStateOf("سباكة طارئة") }
    var serviceTitle by remember { mutableStateOf("") }
    var serviceDetails by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("صنعاء") }
    var selectedArea by remember { mutableStateOf("") }
    var pinCode by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var createdRequestCode by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var expandedCategoryDropdown by remember { mutableStateOf(false) }
    var expandedCityDropdown by remember { mutableStateOf(false) }

    val departments = listOf("خدمات وفنيين", "مراكز ومتاجر", "مطاعم وكافيهات")
    val subCategories = when (selectedDepartment) {
        "خدمات وفنيين" -> listOf("سباكة طارئة", "كهرباء وطوارئ ماس", "تكييف وتبريد", "بنشر وسحب سيارات", "أقفال وأبواب", "أجهزة منزلية")
        "مراكز ومتاجر" -> listOf("قطع غيار مستعجلة", "بطاريات وزيوت", "أدوية ومستلزمات طبية", "إلكترونيات سريعة")
        else -> listOf("وجبات سريعة طارئة", "مشروبات ومياه", "مأكولات سريعة")
    }

    val cities = listOf("صنعاء", "عدن", "تعز", "الحديدة", "إب", "حضرموت", "مأرب", "ذمار")

    LaunchedEffect(selectedDepartment) {
        selectedCategory = subCategories.firstOrNull() ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                        Text(
                            text = "طلب عاجل - 30 دقيقة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFD32F2F)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFEBEE),
                    titleContentColor = Color(0xFFB71C1C)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // شريط تنبيه 30 دقيقة فوري
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                border = BorderStroke(1.5.dp, Color(0xFFE53935))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(32.dp))
                    Column {
                        Text("خدمة الاستجابة السريعة (30 دقيقة)", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C), fontSize = 15.sp)
                        Text("يتم إرسال إشعار فوري عالي الأولوية للفنيين الأقرب لموقعك لاستلام العروض خلال 30 دقيقة فقط.", fontSize = 12.sp, color = Color(0xFFC62828))
                    }
                }
            }

            // بيانات التواصل والموقع
            Text("بيانات التواصل والموقع", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("الاسم") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().testTag("urgent_customer_name"),
                singleLine = true
            )

            OutlinedTextField(
                value = customerPhone,
                onValueChange = { customerPhone = it },
                label = { Text("رقم الهاتف (إجباري)*") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth().testTag("urgent_customer_phone"),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedCity,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المدينة*") },
                        trailingIcon = {
                            IconButton(onClick = { expandedCityDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("urgent_city")
                    )
                    DropdownMenu(expanded = expandedCityDropdown, onDismissRequest = { expandedCityDropdown = false }) {
                        cities.forEach { city ->
                            DropdownMenuItem(text = { Text(city) }, onClick = { selectedCity = city; expandedCityDropdown = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = selectedArea,
                    onValueChange = { selectedArea = it },
                    label = { Text("الحي / الشارع*") },
                    modifier = Modifier.weight(1f).testTag("urgent_area"),
                    singleLine = true
                )
            }

            HorizontalDivider()

            // القسم والتخصص
            Text("قسم الخدمة العاجلة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                departments.forEach { dept ->
                    FilterChip(
                        selected = selectedDepartment == dept,
                        onClick = { selectedDepartment = dept },
                        label = { Text(dept, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("التخصص الطارئ*") },
                    trailingIcon = {
                        IconButton(onClick = { expandedCategoryDropdown = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("urgent_category")
                )
                DropdownMenu(expanded = expandedCategoryDropdown, onDismissRequest = { expandedCategoryDropdown = false }) {
                    subCategories.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { selectedCategory = cat; expandedCategoryDropdown = false })
                    }
                }
            }

            // تفاصيل المشكلة العاجلة
            OutlinedTextField(
                value = serviceTitle,
                onValueChange = { serviceTitle = it },
                label = { Text("عنوان الحالة الطارئة (مثال: عطل كهربائي مفاجئ)*") },
                modifier = Modifier.fillMaxWidth().testTag("urgent_title"),
                singleLine = true
            )

            OutlinedTextField(
                value = serviceDetails,
                onValueChange = { serviceDetails = it },
                label = { Text("وصف الحالة الطارئة والمطلوب بالتفصيل*") },
                modifier = Modifier.fillMaxWidth().height(100.dp).testTag("urgent_details"),
                maxLines = 3
            )

            // رمز PIN للحماية
            Text("رمز PIN للحماية (4 أرقام)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            OutlinedTextField(
                value = pinCode,
                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinCode = it },
                label = { Text("رمز PIN سري (4 أرقام)*") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { isPinVisible = !isPinVisible }) {
                        Icon(imageVector = if (isPinVisible) Icons.Default.Check else Icons.Default.Lock, contentDescription = null)
                    }
                },
                visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().testTag("urgent_pin_code"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // زر إرسال الطلب العاجل
            Button(
                onClick = {
                    if (customerPhone.isBlank() || serviceTitle.isBlank() || serviceDetails.isBlank() || selectedArea.isBlank()) {
                        Toast.makeText(context, "يرجى تعبئة كافة الحقول الإجبارية (*)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (pinCode.length < 4) {
                        Toast.makeText(context, "يرجى كتابة رمز PIN مكون من 4 أرقام", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true
                    scope.launch {
                        try {
                            val uniqueCode = "URG-${Random.nextInt(100000, 999999)}"
                            val reqId = UUID.randomUUID().toString()
                            val now = System.currentTimeMillis()
                            val urgentReq = InstantRequestEntity(
                                id = reqId,
                                requestCode = uniqueCode,
                                secretPin = pinCode,
                                cancellationPassword = pinCode,
                                userId = if (currentUserId.isNotBlank()) currentUserId else customerPhone,
                                userName = customerName.ifBlank { "عميل" },
                                userPhone = customerPhone,
                                userCity = selectedCity,
                                userNeighborhood = selectedArea,
                                categoryId = selectedDepartment,
                                categoryName = selectedCategory,
                                serviceTitle = "🚨 عاجل: $serviceTitle",
                                description = serviceDetails,
                                status = "WAITING_FOR_OFFERS",
                                urgencyTime = "فوراً (خلال 30 دقيقة)",
                                createdAt = now,
                                expiresAt = now + 30 * 60 * 1000L // 30 دقيقة بالضبط
                            )

                            firestore.collection("instant_requests").document(reqId).set(urgentReq)
                                .addOnSuccessListener {
                                    isSubmitting = false
                                    createdRequestCode = uniqueCode
                                    showSuccessDialog = true
                                }
                                .addOnFailureListener { e ->
                                    isSubmitting = false
                                    Toast.makeText(context, "فشل الإرسال: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                        } catch (e: Exception) {
                            isSubmitting = false
                            Toast.makeText(context, "خطأ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_urgent_request_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال الطلب العاجل (مؤقت 30 دقيقة)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إلغاء والعودة")
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFD32F2F))
                    Text("تم تعميم الطلب العاجل!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تم تعميم طلبك الطارئ على جميع الفنيين المتواجدين في منطقتك.")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("كود الطلب العاجل", fontSize = 12.sp, color = Color(0xFFB71C1C))
                            Text(createdRequestCode ?: "", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFD32F2F))
                            Text("المهلة المحددة: 30 دقيقة لاستقبال العروض", fontSize = 12.sp, color = Color(0xFFC62828))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateToUrgentList()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("متابعة الطلب العاجل الآن")
                }
            }
        )
    }
}
