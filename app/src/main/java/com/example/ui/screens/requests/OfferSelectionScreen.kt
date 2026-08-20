package com.example.ui.screens.requests

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.data.NotificationEntity
import com.example.data.models.InstantRequestEntity
import com.example.data.models.RequestOfferEntity
import com.example.ui.MainViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

/**
 * ✅ OfferSelectionScreen
 * شاشة اختيار العرض المناسب وتثبيت موعد الحجز وتوليد كود الحجز الآمن
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferSelectionScreen(
    offerId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit = {},
    onBookingConfirmed: (bookingId: String) -> Unit = {},
    onNavigateToChat: (phone: String, name: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestore = remember { FirebaseFirestore.getInstance() }

    var offer by remember { mutableStateOf<RequestOfferEntity?>(null) }
    var request by remember { mutableStateOf<InstantRequestEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var selectedTime by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(System.currentTimeMillis() + 3600 * 1000L)))
    }
    var userNotes by remember { mutableStateOf("") }
    var userPin by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    var showConfirmationSuccessDialog by remember { mutableStateOf(false) }
    var createdBookingNumber by remember { mutableStateOf("") }

    LaunchedEffect(offerId) {
        if (offerId.isNotBlank()) {
            firestore.collection("instant_offers").document(offerId).get()
                .addOnSuccessListener { offerSnap ->
                    val o = offerSnap.toObject(RequestOfferEntity::class.java)
                    offer = o
                    if (o != null) {
                        firestore.collection("instant_requests").document(o.requestId).get()
                            .addOnSuccessListener { reqSnap ->
                                request = reqSnap.toObject(InstantRequestEntity::class.java)
                                isLoading = false
                            }
                            .addOnFailureListener { isLoading = false }
                    } else {
                        isLoading = false
                    }
                }
                .addOnFailureListener { isLoading = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تأكيد واختيار العرض", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val curOffer = offer
        val curReq = request

        if (curOffer == null || curReq == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("العرض أو الطلب غير موجود.")
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
            // بطاقة ملخص العرض المختار
            Card(
                modifier = Modifier.fillMaxWidth().testTag("selected_offer_summary_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("بيانات مقدم الخدمة المختار", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(curOffer.technicianName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(curOffer.technicianName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                                    Text(" ${curOffer.technicianRating}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text("${curOffer.price} ر.ي", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF2E7D32))
                    }

                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("وقت الوصول: ${curOffer.estimatedArrivalTime}", fontSize = 13.sp)
                        Text("مدة الإنجاز: ${curOffer.estimatedDuration}", fontSize = 13.sp)
                    }

                    if (curOffer.notes.isNotBlank()) {
                        Text("ملاحظات الفني: ${curOffer.notes}", fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            }

            // بطاقة ملخص الطلب
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("الطلب: ${curReq.serviceTitle}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("كود الطلب: ${curReq.requestCode}", fontSize = 12.sp, color = Color.Gray)
                    Text("الموقع: ${curReq.userCity} - ${curReq.userNeighborhood}", fontSize = 12.sp)
                }
            }

            // تحديد وقت وتاريخ الخدمة
            Text("تأكيد الموعد والعنوان", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = { selectedDate = it },
                    label = { Text("تاريخ الخدمة") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    modifier = Modifier.weight(1f).testTag("booking_date_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = { selectedTime = it },
                    label = { Text("وقت الحضور") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.weight(1f).testTag("booking_time_input"),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = userNotes,
                onValueChange = { userNotes = it },
                label = { Text("ملاحظات إضافية للفني عند الوصول") },
                modifier = Modifier.fillMaxWidth().height(85.dp).testTag("booking_notes_input"),
                maxLines = 2
            )

            // رمز PIN للأمان
            Text("رمز سري PIN لتأكيد وإدارة الحجز (4 أرقام)*", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            OutlinedTextField(
                value = userPin,
                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) userPin = it },
                label = { Text("رمز PIN (مثال: 1234)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().testTag("booking_pin_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // زر تأكيد الحجز
            Button(
                onClick = {
                    if (userPin.length < 4) {
                        Toast.makeText(context, "يرجى كتابة رمز PIN مكون من 4 أرقام", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true
                    scope.launch {
                        val bookingId = UUID.randomUUID().toString()
                        val bNum = "BK-${SimpleDateFormat("yyMMddHHmm", Locale.getDefault()).format(Date())}-${Random.nextInt(1000, 9999)}"

                        val newBooking = BookingEntity(
                            id = bookingId,
                            bookingNumber = bNum,
                            bookingPassword = userPin,
                            clientId = curReq.userId,
                            clientName = curReq.userName,
                            clientPhone = curReq.userPhone,
                            clientAddress = "${curReq.userCity} - ${curReq.userNeighborhood}",
                            customerName = curReq.userName,
                            customerPhone = curReq.userPhone,
                            customerArea = "${curReq.userCity} - ${curReq.userNeighborhood}",
                            serviceType = curReq.serviceTitle,
                            providerId = curOffer.technicianId,
                            providerName = curOffer.technicianName,
                            providerPhone = curOffer.technicianPhone,
                            dateString = selectedDate,
                            timeString = selectedTime,
                            date = selectedDate,
                            time = selectedTime,
                            category = curReq.categoryId,
                            subCategory = curReq.categoryName,
                            serviceDetails = "${curReq.description}\nملاحظات: $userNotes",
                            totalAmount = curOffer.price,
                            status = "APPROVED",
                            pinCode = userPin,
                            createdAt = System.currentTimeMillis()
                        )

                        // 1. حفظ الحجز في Firestore
                        firestore.collection("bookings").document(bookingId).set(newBooking)
                            .addOnSuccessListener {
                                // 2. تحديث حالة العرض المختار إلى ACCEPTED
                                firestore.collection("instant_offers").document(curOffer.id).update("status", "ACCEPTED")

                                // 3. تحديث حالة الطلب إلى COMPLETED
                                firestore.collection("instant_requests").document(curReq.id).update(
                                    mapOf(
                                        "status" to "COMPLETED",
                                        "selectedOfferId" to curOffer.id
                                    )
                                )

                                // 4. إشعار للفني بتأكيد الحجز
                                val notifId = UUID.randomUUID().toString()
                                val notif = NotificationEntity(
                                    id = notifId,
                                    title = "🎉 تم اختيار عرضك وتأكيد الحجز!",
                                    message = "تم قبول عرضك لطلب ${curReq.requestCode} بمبلغ ${curOffer.price} ر.ي. رقم الحجز: $bNum",
                                    customerPhone = curOffer.technicianPhone,
                                    targetType = "PROVIDER",
                                    targetValue = curOffer.technicianPhone,
                                    notificationType = "OFFER_ACCEPTED",
                                    timestamp = System.currentTimeMillis()
                                )
                                firestore.collection("notifications").document(notifId).set(notif)

                                isSubmitting = false
                                createdBookingNumber = bNum
                                showConfirmationSuccessDialog = true
                            }
                            .addOnFailureListener { e ->
                                isSubmitting = false
                                Toast.makeText(context, "فشل تأكيد الحجز: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("confirm_booking_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد الحجز والتعاقد مع الفني", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            OutlinedButton(
                onClick = { onNavigateToChat(curOffer.technicianPhone, curOffer.technicianName) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("محادثة الفني قبل التأكيد")
            }
        }
    }

    if (showConfirmationSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                    Text("تم تأكيد الحجز بنجاح!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("تم إرسال إشعار فوري للفني وتأكيد الموعد رسمياً.")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("رقم الحجز المرجعي", fontSize = 12.sp, color = Color(0xFF2E7D32))
                            Text(createdBookingNumber, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("احتفظ برمز PIN لإلغاء أو تعديل الحجز.", fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationSuccessDialog = false
                        onBookingConfirmed(createdBookingNumber)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("عرض تفاصيل الحجوزات")
                }
            }
        )
    }
}
