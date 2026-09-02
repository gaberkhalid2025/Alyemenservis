package com.example.ui.screens.urgent

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.components.UrgentTimerComponent
import com.example.ui.viewmodels.UrgentUiState
import com.example.ui.viewmodels.UrgentViewModel
import kotlinx.coroutines.launch

/**
 * ⚡ UrgentOfferSubmissionScreen
 * تقديم عرض استجابة سريعة للطلبات العاجلة مع مؤقت وخيارات وصول فورية.
 * تعتمد على UrgentViewModel وتدعم Snackbar ورسائل الأخطاء المنظمة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentOfferSubmissionScreen(
    requestId: String,
    viewModel: MainViewModel,
    urgentViewModel: UrgentViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onOfferSubmitted: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserId by viewModel.currentUserId.collectAsState()
    val request by urgentViewModel.selectedRequest.collectAsState()
    val uiState by urgentViewModel.uiState.collectAsState()

    var priceText by remember { mutableStateOf("") }
    var estimatedArrival by remember { mutableStateOf(UrgentConstants.urgencyTimeOptions.first()) }
    var estimatedDuration by remember { mutableStateOf(UrgentConstants.durationOptions.first()) }
    var notesText by remember { mutableStateOf("") }

    val formattedPriceLabel = remember(priceText) {
        val p = priceText.toDoubleOrNull() ?: 0.0
        if (p > 0) {
            "%,.0f ريال يمني".format(p)
        } else {
            ""
        }
    }

    LaunchedEffect(requestId) {
        if (requestId.isNotBlank()) {
            urgentViewModel.observeRequestDetails(requestId)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                        Text("تقديم عرض عاجل", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFB71C1C))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFEBEE))
            )
        }
    ) { paddingValues ->
        if (uiState is UrgentUiState.Loading && request == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFD32F2F))
            }
            return@Scaffold
        }

        val currentReq = request
        if (currentReq == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("الطلب غير متوفر أو تم حذفه.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // مؤقت الطلب العاجل
            UrgentTimerComponent(
                expiresAt = currentReq.expiresAt,
                totalDurationMillis = 30 * 60 * 1000L
            )

            // بطاقة تفاصيل الطلب الطارئ
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE).copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, Color(0xFFEF9A9A))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("بيانات الطلب الطارئ:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFB71C1C))
                    Text(currentReq.serviceTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(currentReq.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF424242))
                    Text("الموقع: ${currentReq.userCity} - ${currentReq.userNeighborhood}", fontSize = 12.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
                }
            }

            Text("تفاصيل العرض السريع", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            // السعر المقترح
            OutlinedTextField(
                value = priceText,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) priceText = it },
                label = { Text("السعر المقترح (ريال يمني)*") },
                supportingText = {
                    if (formattedPriceLabel.isNotBlank()) {
                        Text("السعر المنسق: $formattedPriceLabel", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    } else {
                        Text("الحد الأدنى لتقديم عرض عاجل هو 1,000 ريال يمني")
                    }
                },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF2E7D32)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("urgent_offer_price_input"),
                singleLine = true
            )

            // خيارات الوصول السريع
            Text("وقت الوصول المؤكد لموقع العميل*", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UrgentConstants.urgencyTimeOptions.forEach { opt ->
                    val isSelected = estimatedArrival == opt
                    FilterChip(
                        selected = isSelected,
                        onClick = { estimatedArrival = opt },
                        label = { Text(opt, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFCDD2),
                            selectedLabelColor = Color(0xFFB71C1C)
                        )
                    )
                }
            }

            // مدة الإنجاز
            Text("المدة المتوقعة لإنهاء العمل*", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UrgentConstants.durationOptions.forEach { opt ->
                    val isSelected = estimatedDuration == opt
                    FilterChip(
                        selected = isSelected,
                        onClick = { estimatedDuration = opt },
                        label = { Text(opt, fontSize = 12.sp) }
                    )
                }
            }

            // ملاحظات إضافية
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("ملاحظات / جاهزية العدة وقطع الغيار (اختياري)") },
                modifier = Modifier.fillMaxWidth().height(90.dp).testTag("urgent_offer_notes_input"),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            // زر إرسال العرض العاجل
            val isSubmitting = uiState is UrgentUiState.Loading
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull()
                    if (price == null || price <= 0.0) {
                        scope.launch { snackbarHostState.showSnackbar("السعر يجب أن يكون أكبر من 0") }
                        return@Button
                    }
                    if (price < 1000.0) {
                        scope.launch { snackbarHostState.showSnackbar("الحد الأدنى لتقديم العرض هو 1,000 ريال يمني") }
                        return@Button
                    }

                    urgentViewModel.submitUrgentOffer(
                        currentReq = currentReq,
                        price = price,
                        estimatedArrival = estimatedArrival,
                        estimatedDuration = estimatedDuration,
                        notesText = notesText,
                        currentUserId = currentUserId,
                        onSuccess = {
                            scope.launch { snackbarHostState.showSnackbar("تم إرسال عرضك الفوري بنجاح!") }
                            onOfferSubmitted()
                        },
                        onError = { err ->
                            scope.launch { snackbarHostState.showSnackbar(err) }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_urgent_offer_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال العرض العاجل فوراً", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إلغاء")
            }
        }
    }
}
