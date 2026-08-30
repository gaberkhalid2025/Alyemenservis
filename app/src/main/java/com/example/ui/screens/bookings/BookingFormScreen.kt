package com.example.ui.screens.bookings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.utils.BookingUtils
import java.util.Calendar
import java.util.Locale

/**
 * 📝 BookingFormScreen
 * استمارة تقديم طلب حجز موعد جديد
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFormScreen(
    providerId: String = "",
    providerName: String = "",
    providerPhone: String = "",
    preselectedCategory: String = "",
    onBack: () -> Unit,
    onBookingCreated: (BookingEntity) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var fullName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("صنعاء") }
    var fullAddress by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(preselectedCategory.ifEmpty { "صيانة عامة" }) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var serviceDetails by remember { mutableStateOf("") }
    var userSecretPass by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    var createdBookingResult by remember { mutableStateOf<BookingEntity?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCityDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val yemeniCities = listOf("صنعاء", "عدن", "تعز", "إب", "حضرموت", "الحديدة", "ذمار", "مأرب")
    val defaultCategories = listOf("صيانة عامة", "كهرباء", "سباكة", "تكييف وتبريد", "إلكترونيات", "نظافة وتنظيف", "خدمات عقارية", "خدمات طبية")

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = System.currentTimeMillis() - 1000
    }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val period = if (hourOfDay < 12) "صباحاً" else "مساءً"
            val hourFormatted = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
            selectedTime = String.format(Locale.US, "%02d:%02d %s", hourFormatted, minute, period)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "طلب حجز موعد جديد",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Service Info Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (providerName.isNotBlank()) "حجز مع: $providerName" else "حجز موعد خدمة",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "يرجى تعبئة كافة الحقول الإلزامية لتأكيد الحجز",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Full Name (Mandatory)
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; errorMessage = null },
                label = { Text("الاسم الثلاثي بالكامل *") },
                placeholder = { Text("مثال: عبدالله محمد صالح") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // 2. Phone Number (Mandatory)
            OutlinedTextField(
                value = clientPhone,
                onValueChange = {
                    if (it.length <= 9) clientPhone = it
                    errorMessage = null
                },
                label = { Text("رقم الهاتف اليمني (9 أرقام) *") },
                placeholder = { Text("77XXXXXXX أو 73XXXXXXX") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // 3. Address (Mandatory)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedCity,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المدينة *") },
                        trailingIcon = {
                            IconButton(onClick = { showCityDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { showCityDropdown = true },
                        shape = RoundedCornerShape(14.dp)
                    )

                    DropdownMenu(
                        expanded = showCityDropdown,
                        onDismissRequest = { showCityDropdown = false }
                    ) {
                        yemeniCities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city) },
                                onClick = {
                                    selectedCity = city
                                    showCityDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = fullAddress,
                    onValueChange = { fullAddress = it; errorMessage = null },
                    label = { Text("الحي والشارع بالتفصيل *") },
                    placeholder = { Text("الشارع والحي والمنزل") },
                    modifier = Modifier.weight(1.3f),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Category Selection
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("قسم / نوع الخدمة *") },
                    leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton(onClick = { showCategoryDropdown = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showCategoryDropdown = true },
                    shape = RoundedCornerShape(14.dp)
                )

                DropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    defaultCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            // 4. Date and Time Selection
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedCard(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedDate.ifEmpty { "تحديد التاريخ *" },
                            fontSize = 13.sp,
                            fontWeight = if (selectedDate.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                OutlinedCard(
                    onClick = { timePickerDialog.show() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedTime.ifEmpty { "تحديد الوقت *" },
                            fontSize = 13.sp,
                            fontWeight = if (selectedTime.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Optional Notes
            OutlinedTextField(
                value = serviceDetails,
                onValueChange = { serviceDetails = it },
                label = { Text("ملاحظات إضافية (اختياري)") },
                placeholder = { Text("أي تفاصيل إضافية تريد إبلاغ الفني بها...") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                maxLines = 3,
                shape = RoundedCornerShape(14.dp)
            )

            // (Secret Password removed to condense the form)

            // Terms
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreeToTerms,
                    onCheckedChange = { agreeToTerms = it }
                )
                Text(
                    text = "أوافق على سياسة الإلغاء والتعديل (قبل 8 ساعات من الموعد على الأقل)",
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { agreeToTerms = !agreeToTerms }
                )
            }

            // Error display
            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = {
                    if (fullName.trim().isEmpty()) {
                        errorMessage = "الاسم الثلاثي بالكامل مطلوب"
                        return@Button
                    }
                    val cleanPhone = clientPhone.trim()
                    if (cleanPhone.length < 9) {
                        errorMessage = "يرجى إدخال رقم هاتف يمني صحيح من 9 أرقام"
                        return@Button
                    }
                    if (fullAddress.trim().isEmpty()) {
                        errorMessage = "عنوان السكن الكامل والحي مطلوب"
                        return@Button
                    }
                    if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
                        errorMessage = "يرجى تحديد تاريخ ووقت الموعد"
                        return@Button
                    }
                    if (!agreeToTerms) {
                        errorMessage = "يرجى الموافقة على الشروط للمتابعة"
                        return@Button
                    }

                    isSubmitting = true
                    val generatedCode = BookingUtils.generateBookingCode("BK")
                    val generatedPass = if (userSecretPass.trim().isNotBlank()) userSecretPass.trim() else BookingUtils.generateBookingPassword(4)
                    val completeAddressStr = "$selectedCity - ${fullAddress.trim()}"
                    val scheduledTs = BookingUtils.parseScheduledTimestamp(selectedDate, selectedTime)

                    val newBooking = BookingEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        bookingCode = generatedCode,
                        bookingNumber = generatedCode,
                        bookingPassword = generatedPass,
                        pinCode = generatedPass,
                        fullName = fullName.trim(),
                        customerName = fullName.trim(),
                        clientName = fullName.trim(),
                        clientPhone = cleanPhone,
                        customerPhone = cleanPhone,
                        fullAddress = completeAddressStr,
                        customerArea = completeAddressStr,
                        clientAddress = completeAddressStr,
                        category = category,
                        serviceType = category,
                        providerId = providerId,
                        providerName = providerName.ifEmpty { "مقدم خدمة معتمد" },
                        providerPhone = providerPhone,
                        date = selectedDate,
                        dateString = selectedDate,
                        time = selectedTime,
                        timeString = selectedTime,
                        scheduledAt = scheduledTs,
                        serviceDetails = serviceDetails.trim(),
                        status = "PENDING",
                        createdAt = System.currentTimeMillis()
                    )

                    isSubmitting = false
                    createdBookingResult = newBooking
                    showSuccessDialog = true
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد وتوثيق طلب الحجز", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Success Dialog showing code and password
    if (showSuccessDialog && createdBookingResult != null) {
        val bk = createdBookingResult!!
        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تم إنشاء الحجز بنجاح 🎉", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("تم توثيق طلب الحجز بنجاح. يرجى الاحتفاظ بالبيانات التالية لتتبع أو تعديل حجزك:", fontSize = 13.sp)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("كود الحجز:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                SelectionContainer {
                                    Text(bk.bookingCode, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("كلمة السر الخاصة بالحجز:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                SelectionContainer {
                                    Text(bk.bookingPassword, fontWeight = FontWeight.Bold, color = Color(0xFFD97706), fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    Text(
                        "⚠️ يمكنك التعديل أو الإلغاء فقط قبل 8 ساعات من موعد الحجز باستخدام رقم الهاتف وكلمة المرور.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString("كود الحجز: ${bk.bookingCode} | كلمة المرور: ${bk.bookingPassword}"))
                        Toast.makeText(context, "تم نسخ بيانات الحجز للحافظة", Toast.LENGTH_SHORT).show()
                        showSuccessDialog = false
                        onBookingCreated(bk)
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ البيانات والانتقال لحجوزاتي")
                }
            }
        )
    }
}
