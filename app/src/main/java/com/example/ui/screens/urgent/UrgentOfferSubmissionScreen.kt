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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NotificationEntity
import com.example.data.models.InstantRequestEntity
import com.example.data.models.RequestOfferEntity
import com.example.ui.MainViewModel
import com.example.ui.components.UrgentTimerComponent
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ⚡ UrgentOfferSubmissionScreen
 * تقديم عرض استجابة سريعة للطلبات العاجلة مع مؤقت وخيارات وصول فورية
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentOfferSubmissionScreen(
    requestId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit = {},
    onOfferSubmitted: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestore = remember { FirebaseFirestore.getInstance() }

    val currentUserId by viewModel.currentUserId.collectAsState()
    val isProvider = viewModel.isProviderUser

    var request by remember { mutableStateOf<InstantRequestEntity?>(null) }
    var isLoadingRequest by remember { mutableStateOf(true) }

    var priceText by remember { mutableStateOf("") }
    var estimatedArrival by remember { mutableStateOf("الوصول خلال 15 دقيقة") }
    var estimatedDuration by remember { mutableStateOf("نصف ساعة") }
    var notesText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val fastArrivalOptions = listOf("الوصول خلال 15 دقيقة", "الوصول خلال 20 دقيقة", "الوصول خلال 30 دقيقة")
    val durationOptions = listOf("نصف ساعة", "ساعة واحدة", "ساعتان")

    LaunchedEffect(requestId) {
        if (requestId.isNotBlank()) {
            firestore.collection("instant_requests").document(requestId)
                .get()
                .addOnSuccessListener { snapshot ->
                    request = snapshot.toObject(InstantRequestEntity::class.java)
                    isLoadingRequest = false
                }
                .addOnFailureListener {
                    isLoadingRequest = false
                }
        }
    }

    Scaffold(
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
        if (isLoadingRequest) {
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
                fastArrivalOptions.forEach { opt ->
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
                durationOptions.forEach { opt ->
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
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull()
                    if (price == null || price <= 0.0) {
                        Toast.makeText(context, "يرجى تحديد السعر", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true
                    scope.launch {
                        val offerId = UUID.randomUUID().toString()
                        val newOffer = RequestOfferEntity(
                            id = offerId,
                            requestId = currentReq.id,
                            requestCode = currentReq.requestCode,
                            technicianId = currentUserId,
                            technicianName = "فني طوارئ معتمد",
                            technicianPhone = currentUserId,
                            technicianAvatar = "",
                            technicianRating = 5.0f,
                            price = price,
                            estimatedArrivalTime = estimatedArrival,
                            estimatedDuration = estimatedDuration,
                            notes = "🚨 استجابة طوارئ: $notesText",
                            status = "PENDING",
                            createdAt = System.currentTimeMillis()
                        )

                        firestore.collection("instant_offers").document(offerId).set(newOffer)
                            .addOnSuccessListener {
                                firestore.collection("instant_requests").document(currentReq.id)
                                    .update("offersCount", FieldValue.increment(1))

                                // إشعار طارئ للعميل
                                val notifId = UUID.randomUUID().toString()
                                val notif = NotificationEntity(
                                    id = notifId,
                                    title = "🚨 عرض طارئ لطلبك ${currentReq.requestCode}",
                                    message = "وصلك عرض فوري من ${newOffer.technicianName} بسعر ${newOffer.price} ر.ي ووصول ${newOffer.estimatedArrivalTime}",
                                    customerPhone = currentReq.userPhone,
                                    targetType = "USER",
                                    targetValue = currentReq.userPhone,
                                    notificationType = "URGENT_OFFER",
                                    timestamp = System.currentTimeMillis()
                                )
                                firestore.collection("notifications").document(notifId).set(notif)

                                isSubmitting = false
                                Toast.makeText(context, "تم إرسال عرضك الفوري بنجاح!", Toast.LENGTH_SHORT).show()
                                onOfferSubmitted()
                            }
                            .addOnFailureListener { e ->
                                isSubmitting = false
                                Toast.makeText(context, "فشل الإرسال: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                    }
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
