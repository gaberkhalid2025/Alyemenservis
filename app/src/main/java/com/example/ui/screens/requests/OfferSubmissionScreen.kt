package com.example.ui.screens.requests

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
import com.example.data.RequestOfferEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AppSnackbarHost
import com.example.ui.components.SnackbarType
import com.example.ui.components.showCustomSnackbar
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 💼 OfferSubmissionScreen
 * شاشة تقديم عرض سعر وموعد وصول من قبل مقدم الخدمة / الفني
 * مرتبطة مع RequestsViewModel ومزودة بـ AppSnackbar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferSubmissionScreen(
    requestId: String,
    viewModel: MainViewModel,
    requestsViewModel: RequestsViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onOfferSubmitted: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()

    val currentRequest by requestsViewModel.currentRequest.collectAsState()

    var priceText by remember { mutableStateOf("") }
    var estimatedArrivalTime by remember { mutableStateOf("خلال 30 دقيقة") }
    var estimatedDuration by remember { mutableStateOf("ساعة واحدة") }
    var notesText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val arrivalOptions = listOf("خلال 15 دقيقة", "خلال 30 دقيقة", "خلال ساعة", "خلال ساعتين", "اليوم مساءً", "غداً صباحاً")
    val durationOptions = listOf("نصف ساعة", "ساعة واحدة", "ساعتان", "نصف يوم", "يوم كامل")

    LaunchedEffect(requestId) {
        requestsViewModel.listenToRequestDetails(requestId)
    }

    Scaffold(
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("تقديم عرض سعر", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { paddingValues ->
        val req = currentRequest
        if (req == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
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
            // بطاقة ملخص الطلب
            Card(
                modifier = Modifier.fillMaxWidth().testTag("offer_submission_request_summary"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(req.requestCode, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        Text(req.urgencyTime, fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }

                    Text(req.serviceTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(req.description, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)

                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المدينة: ${req.userCity}", fontSize = 12.sp)
                        Text("الحي: ${req.userNeighborhood}", fontSize = 12.sp)
                    }
                }
            }

            Text("تفاصيل عرضك الفني والمالي", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            // السعر
            OutlinedTextField(
                value = priceText,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) priceText = it },
                label = { Text("سعر تقديم الخدمة (ريال يمني)*") },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF2E7D32)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("offer_price_input"),
                singleLine = true
            )

            // وقت الوصول
            Text("الوقت المتوقع للوصول لموقع العميل*", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                arrivalOptions.forEach { opt ->
                    FilterChip(
                        selected = estimatedArrivalTime == opt,
                        onClick = { estimatedArrivalTime = opt },
                        label = { Text(opt, fontSize = 12.sp) }
                    )
                }
            }

            // مدة الإنجاز
            Text("المدة المقدرة لإنهاء العمل*", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durationOptions.forEach { opt ->
                    FilterChip(
                        selected = estimatedDuration == opt,
                        onClick = { estimatedDuration = opt },
                        label = { Text(opt, fontSize = 12.sp) }
                    )
                }
            }

            // ملاحظات إضافية
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("ملاحظات / تفاصيل ما يشمله السعر (اختياري)") },
                modifier = Modifier.fillMaxWidth().height(100.dp).testTag("offer_notes_input"),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            // زر إرسال العرض
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull()
                    if (price == null || price <= 0.0) {
                        scope.launch {
                            snackbarHostState.showCustomSnackbar(
                                message = "يرجى إدخال السعر المقترح أكبر من صفر",
                                type = SnackbarType.WARNING
                            )
                        }
                        return@Button
                    }

                    isSubmitting = true
                    val offerId = UUID.randomUUID().toString()
                    val newOffer = RequestOfferEntity(
                        id = offerId,
                        requestId = req.id,
                        requestCode = req.requestCode,
                        technicianId = currentUserId.ifBlank { "tech_${System.currentTimeMillis()}" },
                        technicianName = currentUserName.ifBlank { "فني معتمد" },
                        technicianPhone = currentUserPhone.ifBlank { currentUserId },
                        technicianAvatar = "",
                        technicianRating = 4.9f,
                        price = price,
                        estimatedArrivalTime = estimatedArrivalTime,
                        estimatedDuration = estimatedDuration,
                        notes = notesText,
                        status = "PENDING",
                        createdAt = System.currentTimeMillis()
                    )

                    requestsViewModel.submitOffer(
                        offer = newOffer,
                        userPhone = req.userPhone,
                        onSuccess = {
                            isSubmitting = false
                            onOfferSubmitted()
                        },
                        onError = { errMsg ->
                            isSubmitting = false
                            scope.launch {
                                snackbarHostState.showCustomSnackbar(
                                    message = errMsg,
                                    type = SnackbarType.ERROR
                                )
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_offer_btn"),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال العرض للعميل", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
}
