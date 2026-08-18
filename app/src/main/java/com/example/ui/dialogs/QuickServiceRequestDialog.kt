@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.dialogs

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val settingsState by viewModel.settings.collectAsState()

    var nameInput by remember(currentUserName, currentUserPhone) { mutableStateOf(currentUserName.ifEmpty { if (currentUserPhone.isNotBlank()) "عميل ($currentUserPhone)" else "" }) }
    var phoneInput by remember(currentUserPhone) { mutableStateOf(currentUserPhone) }
    var selectedCategoryTab by remember { mutableStateOf("SERVICES") } // SERVICES, STORES, RESTAURANTS, MEDICAL, PROPERTIES, JOBS
    var selectedCity by remember { mutableStateOf("صنعاء") }
    var selectedServiceType by remember { mutableStateOf("سباكة وتمديدات") }
    var problemDescription by remember { mutableStateOf("") }
    var pinCodeInput by remember { mutableStateOf("") }

    // Success dialog state with generated unique code
    var generatedRequestCode by remember { mutableStateOf<String?>(null) }

    val labelName = settingsState.bookingLabelName.ifBlank { "اسم العميل / صاحب الطلب" }
    val labelPhone = settingsState.bookingLabelPhone.ifBlank { "رقم الهاتف اليمني للتواصل (9 أرقام)" }
    val labelArea = settingsState.bookingLabelArea.ifBlank { "المدينة / المحافظة" }
    val labelService = settingsState.bookingLabelService.ifBlank { "وصف المشكلة أو الخدمة بالتفصيل" }
    val bookingTerms = settingsState.bookingTerms

    val yemeniCities = listOf("صنعاء", "عدن", "تعز", "الحديدة", "إب", "حضرموت", "ذمار", "عمران", "صعدة", "مأرب", "شبوة", "البيضاء", "لحج", "أبين", "المهرة")
    
    val categoryTabs = listOf(
        Triple("SERVICES", "🔧 الخدمات والفنيين", listOf("سباكة وتمديدات", "كهرباء وصيانة", "تكييف وتبريد", "صيانة سيارات", "نقل عفش", "تنظيف ومكافحة", "برمجة وهواتف", "أخرى")),
        Triple("STORES", "🏪 المراكز والمتاجر", listOf("مواد غذائية", "إلكترونيات وهواتف", "أجهزة منزلية", "ملابس وأزياء", "عطور ومستحضرات", "أثاث منزلي", "أخرى")),
        Triple("RESTAURANTS", "🍔 المطاعم والكافيهات", listOf("وجبات سريعة", "مشويات ومندي", "حلويات وعصائر", "مأكولات شعبية", "قهوة ومشروبات", "أخرى")),
        Triple("MEDICAL", "🏥 المراكز الطبية", listOf("عيادة عامة وطوارئ", "أسنان وتجميل", "صيدلية وتوصيل أدوية", "مختبر وتحاليل", "أشعة", "أخرى")),
        Triple("PROPERTIES", "🏠 العقارات والأراضي", listOf("شقق إيجار", "بيوت ومنازل للبيع", "أراضي وعقارات تجارية", "استئجار مفروش", "أخرى")),
        Triple("JOBS", "💼 الوظائف والخدمات", listOf("وظيفة شاغرة", "طلب عمل/مهنة", "خدمات حرة", "أخرى"))
    )

    val currentSubTypes = categoryTabs.find { it.first == selectedCategoryTab }?.third ?: listOf("عام")

    Surface(
        color = themeColors.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                    Column {
                        Text("⚡ اطلب خدمتك الآن (المزاد العكسي الفوري)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        Text("أرسل طلبك فوراً مع توليد كود فريد للمتابعة", fontSize = 10.sp, color = themeColors.textSecondary)
                    }
                }
            }

            Divider(color = themeColors.accent.copy(alpha = 0.2f))

            if (bookingTerms.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7).copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("💡", fontSize = 14.sp)
                        Text(bookingTerms, fontSize = 11.sp, color = Color(0xFFFEF3C7), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Customer Name Input
            Column {
                Text("👤 $labelName:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // Phone Input
            Column {
                Text("📱 $labelPhone:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    placeholder = { Text("77XXXXXXX / 73XXXXXXX", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // City Choice
            Column {
                Text("🏙️ $labelArea:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    items(yemeniCities.size) { idx ->
                        val c = yemeniCities[idx]
                        val isSel = selectedCity == c
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSel) themeColors.accent else themeColors.surface)
                                .clickable { selectedCity = c }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        ) {
                            Text(c, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                        }
                    }
                }
            }

            // Category Section Choice
            Column {
                Text("📂 قسم المنشأة / الطلب:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    items(categoryTabs.size) { idx ->
                        val cat = categoryTabs[idx]
                        val isSel = selectedCategoryTab == cat.first
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSel) themeColors.accent else themeColors.surface)
                                .clickable {
                                    selectedCategoryTab = cat.first
                                    selectedServiceType = cat.third.firstOrNull() ?: ""
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        ) {
                            Text(cat.second, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                        }
                    }
                }
            }

            // Specialty Choice
            Column {
                Text("🔧 التخصص أو نوع السلعة/الخدمة المطلوبة:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    items(currentSubTypes.size) { idx ->
                        val s = currentSubTypes[idx]
                        val isSel = selectedServiceType == s
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSel) themeColors.accent else themeColors.surface)
                                .clickable { selectedServiceType = s }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        ) {
                            Text(s, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                        }
                    }
                }
            }

            // Problem Description
            Column {
                Text("📝 $labelService:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = problemDescription,
                    onValueChange = { problemDescription = it },
                    placeholder = { Text("اكتب تفاصيل ما تحتاجه، مثل: عطل في الخزان، تسريب مياه، صيانة مكيف...", color = Color.Gray, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // PIN Code Protection Field
            Column {
                Text("🔒 رمز PIN لحماية وتعديل الطلب (رمز سري):", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = pinCodeInput,
                    onValueChange = { if (it.length <= 6) pinCodeInput = it },
                    placeholder = { Text("رمز سري لحماية إلغاء/تعديل الطلب (مثال: 1234)", color = Color.Gray, fontSize = 11.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // Submit Button
            Button(
                onClick = {
                    val cleanP = phoneInput.trim()
                    if (cleanP.length != 9 || !(cleanP.startsWith("77") || cleanP.startsWith("73") || cleanP.startsWith("71") || cleanP.startsWith("70") || cleanP.startsWith("78"))) {
                        Toast.makeText(context, "⚠️ يرجى إدخال رقم هاتف يمني صحيح مكون من 9 أرقام (77/73/71/70/78)", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val uniqueCode = "R-" + (100000..999999).random()
                    generatedRequestCode = uniqueCode

                    val sectionLabel = categoryTabs.find { it.first == selectedCategoryTab }?.second ?: "المنشآت"
                    val desc = if (problemDescription.isBlank()) "طلب عاجل [$sectionLabel]: $selectedServiceType في $selectedCity (كود: $uniqueCode)" else "[ $sectionLabel - $selectedServiceType ] (كود: $uniqueCode): $problemDescription"

                    viewModel.addBooking(
                        name = nameInput.ifEmpty { "عميل" },
                        phone = cleanP,
                        area = selectedCity,
                        serviceType = desc,
                        providerId = "ALL_${selectedCategoryTab}",
                        providerName = "جميع مزودي $sectionLabel",
                        dateString = "طلب عاجل الآن ⚡ [$uniqueCode]",
                        timeString = "المزاد العكسي الشامل",
                        customPassword = pinCodeInput.trim()
                    )

                    viewModel.triggerNotification("🚀 تم نشر طلبك ($uniqueCode) في قسم ($sectionLabel) بنجاح! تم تنبيه جميع الجهات والمزودين في $selectedCity")
                    Toast.makeText(context, "✅ تم إرسال طلبك برمز فريد: $uniqueCode", Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("إرسال الطلب وتوليد الكود الفريد 🚀", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Success Dialog showing the generated unique code
    if (generatedRequestCode != null) {
        AlertDialog(
            onDismissRequest = {
                generatedRequestCode = null
                onRequestCreated()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎉", fontSize = 24.sp)
                    Text("تم إنشاء طلبك بنجاح!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("تم توليد الكود الفريد الخاص بطلبك بنجاح:", fontSize = 12.sp, color = Color.LightGray)
                    Surface(
                        color = themeColors.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, themeColors.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الكود الفريد للطلب:", fontSize = 11.sp, color = Color.Gray)
                            Text(generatedRequestCode ?: "", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }
                    Text("يظهر طلبك الآن فوراً لجميع الفنيين والمتاجر المتخصصة في مدينتك. يمكنك متابعة العروض في قسم 'طلباتي'.", fontSize = 11.sp, color = Color.White, textAlign = TextAlign.Center)
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
                    Text("تم، الانتقال إلى طلباتي 📋", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
