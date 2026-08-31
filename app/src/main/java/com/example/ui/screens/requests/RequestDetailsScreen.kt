package com.example.ui.screens.requests

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.InstantRequestEntity
import com.example.data.models.RequestOfferEntity
import com.example.ui.MainViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🔍 RequestDetailsScreen
 * شاشة تفاصيل طلب الخدمة مع العروض وحماية الإلغاء برمز PIN
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailsScreen(
    requestId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToOfferSubmission: (requestId: String) -> Unit = {},
    onNavigateToOfferSelection: (offerId: String) -> Unit = {},
    onNavigateToChat: (phone: String, name: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestore = remember { FirebaseFirestore.getInstance() }

    val currentUserId by viewModel.currentUserId.collectAsState()
    val isProvider = viewModel.isProviderUser

    var request by remember { mutableStateOf<InstantRequestEntity?>(null) }
    var offersList by remember { mutableStateOf<List<RequestOfferEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // حوار الإلغاء برمز PIN
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelPinInput by remember { mutableStateOf("") }
    var isCancelling by remember { mutableStateOf(false) }

    LaunchedEffect(requestId) {
        if (requestId.isNotBlank()) {
            firestore.collection("instant_requests").document(requestId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        request = snapshot.toObject(InstantRequestEntity::class.java)
                    }
                    isLoading = false
                }

            firestore.collection("instant_offers")
                .whereEqualTo("requestId", requestId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        offersList = snapshot.documents.mapNotNull { it.toObject(RequestOfferEntity::class.java) }
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = request?.requestCode?.ifBlank { "تفاصيل الطلب" } ?: "تفاصيل الطلب",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    val status = request?.status
                    if (status == "WAITING_FOR_OFFERS" || status == "REVIEWING_OFFERS") {
                        IconButton(onClick = { showCancelDialog = true }) {
                            Icon(Icons.Default.Close, contentDescription = "إلغاء الطلب", tint = MaterialTheme.colorScheme.error)
                        }
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

        val currentRequest = request
        if (currentRequest == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("الطلب غير موجود.")
            }
            return@Scaffold
        }

        val isMyRequest = currentRequest.userId == currentUserId || currentRequest.userPhone == currentUserId

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // بطاقة حالة وبيانات الطلب
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("request_details_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(currentRequest.requestCode, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Badge(
                                containerColor = when (currentRequest.status) {
                                    "WAITING_FOR_OFFERS" -> Color(0xFF1976D2)
                                    "COMPLETED" -> Color(0xFF2E7D32)
                                    "CANCELLED" -> Color(0xFFD32F2F)
                                    else -> Color(0xFFFFA000)
                                }
                            ) {
                                Text(
                                    text = when (currentRequest.status) {
                                        "WAITING_FOR_OFFERS" -> "بانتظار العروض"
                                        "COMPLETED" -> "مكتمل"
                                        "CANCELLED" -> "ملغي"
                                        else -> currentRequest.status
                                    },
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Text(currentRequest.serviceTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(currentRequest.description, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)

                        HorizontalDivider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("${currentRequest.userCity} - ${currentRequest.userNeighborhood}", fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                Text(currentRequest.urgencyTime, fontSize = 13.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }

                        val dateFormatted = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date(currentRequest.createdAt))
                        Text("تاريخ النشر: $dateFormatted", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            // زر تقديم عرض للفنيين
            if (isProvider && (currentRequest.status == "WAITING_FOR_OFFERS" || currentRequest.status == "REVIEWING_OFFERS")) {
                item {
                    Button(
                        onClick = { onNavigateToOfferSubmission(currentRequest.id) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("provider_submit_offer_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تقديم عرض سعر لهذا الطلب", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // قائمة العروض المستلمة
            if (isMyRequest || !isProvider) {
                item {
                    Text(
                        text = "العروض المستلمة (${offersList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                if (offersList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.Gray)
                                Text("جاري انتظار تقديم الفنيين لعروضهم...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    items(offersList) { offer ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("offer_item_${offer.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(offer.technicianName.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        Column {
                                            Text(offer.technicianName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("وصول: ${offer.estimatedArrivalTime}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    Text("${offer.price} ر.ي", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF2E7D32))
                                }

                                if (offer.notes.isNotBlank()) {
                                    Text(offer.notes, fontSize = 12.sp, color = Color.DarkGray)
                                }

                                HorizontalDivider()

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onNavigateToChat(offer.technicianPhone, offer.technicianName) },
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("محادثة", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { onNavigateToOfferSelection(offer.id) },
                                        modifier = Modifier.weight(1.2f).height(38.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Text("قبول العرض", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // حوار الإلغاء برمز PIN
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("تأكيد إلغاء الطلب") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يرجى إدخال رمز PIN السري (4 أرقام) الذي تم إنشاؤه مع الطلب لتأكيد الإلغاء:")
                    OutlinedTextField(
                        value = cancelPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) cancelPinInput = it },
                        label = { Text("رمز PIN (4 أرقام)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("cancel_pin_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expectedPin = (request?.secretPin ?: request?.cancellationPassword ?: "").trim()
                        if (expectedPin.isBlank()) {
                            Toast.makeText(context, "الرمز السري للطلب غير متوفر، الرجاء التواصل مع الدعم.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (cancelPinInput.trim() != expectedPin) {
                            Toast.makeText(context, "رمز PIN غير صحيح!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isCancelling = true
                        scope.launch {
                            try {
                                firestore.collection("instant_requests").document(requestId)
                                    .update("status", "CANCELLED")
                                    .addOnSuccessListener {
                                        isCancelling = false
                                        showCancelDialog = false
                                        Toast.makeText(context, "تم إلغاء الطلب بنجاح", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        isCancelling = false
                                        Toast.makeText(context, "فشل الإلغاء: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                            } catch (e: Exception) {
                                isCancelling = false
                                Toast.makeText(context, "خطأ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isCancelling
                ) {
                    Text("تأكيد الإلغاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("تراجع")
                }
            }
        )
    }
}
