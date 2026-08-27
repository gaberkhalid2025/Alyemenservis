package com.example.ui.screens.urgent

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
import com.example.ui.components.UrgentTimerComponent
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/**
 * 🚨 UrgentRequestDetailsScreen
 * عرض تفاصيل الطلب العاجل ومؤقت الـ 30 دقيقة وقائمة العروض السريعة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentRequestDetailsScreen(
    requestId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToOfferSelection: (offerId: String) -> Unit = {},
    onNavigateToUrgentOfferSubmission: (requestId: String) -> Unit = {},
    onNavigateToChat: (phone: String, name: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestore = remember { FirebaseFirestore.getInstance() }

    val currentUserId by viewModel.currentUserId.collectAsState()
    val isProvider = viewModel.isProviderUser

    var request by remember { mutableStateOf<InstantRequestEntity?>(null) }
    var offers by remember { mutableStateOf<List<RequestOfferEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showCancelDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
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
                        offers = snapshot.documents.mapNotNull { it.toObject(RequestOfferEntity::class.java) }
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                        Text(
                            text = request?.requestCode?.ifBlank { "تفاصيل الطلب العاجل" } ?: "طلب عاجل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (request?.status == "WAITING_FOR_OFFERS" || request?.status == "REVIEWING_OFFERS") {
                        IconButton(onClick = { showCancelDialog = true }) {
                            Icon(Icons.Default.Close, contentDescription = "إلغاء الطلب", tint = Color(0xFFD32F2F))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFEBEE),
                    titleContentColor = Color(0xFFB71C1C)
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFD32F2F))
            }
            return@Scaffold
        }

        val currentRequest = request
        if (currentRequest == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("الطلب العاجل غير موجود أو تم حذفه.")
            }
            return@Scaffold
        }

        val isCustomer = currentRequest.userId == currentUserId || currentRequest.userPhone == currentUserId || !isProvider

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // مؤقت 30 دقيقة بارز للطلب العاجل
            item {
                UrgentTimerComponent(
                    expiresAt = currentRequest.expiresAt,
                    totalDurationMillis = 30 * 60 * 1000L
                )
            }

            // بطاقة بيانات الطلب العاجل
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("urgent_details_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE).copy(alpha = 0.6f)),
                    border = BorderStroke(1.5.dp, Color(0xFFE53935))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Badge(containerColor = Color(0xFFD32F2F)) {
                                Text(
                                    text = if (currentRequest.status == "WAITING_FOR_OFFERS") "طلب عاجل مفتوح" else currentRequest.status,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(currentRequest.requestCode, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFD32F2F))
                        }

                        Text(currentRequest.serviceTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(currentRequest.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF424242))

                        HorizontalDivider(color = Color(0xFFEF9A9A))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD32F2F))
                                Text("${currentRequest.userCity} - ${currentRequest.userNeighborhood}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD32F2F))
                                Text("الاستجابة: 30 دقيقة", fontSize = 13.sp, color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // زر تقديم عرض عاجل للفنيين
            if (isProvider && currentRequest.status == "WAITING_FOR_OFFERS") {
                item {
                    Button(
                        onClick = { onNavigateToUrgentOfferSubmission(currentRequest.id) },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("urgent_submit_offer_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تقديم عرض عاجل فوري", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // قائمة العروض للمستخدم والأدمن
            if (isCustomer) {
                item {
                    Text("العروض العاجلة المستلمة (${offers.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                if (offers.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = Color(0xFFD32F2F), modifier = Modifier.size(32.dp))
                                Text("جاري استلام عروض الاستجابة السريعة من أقرب الفنيين...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    items(offers) { offer ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("urgent_offer_card_${offer.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFFEBEE)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(offer.technicianName.take(1), fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                        }
                                        Column {
                                            Text(offer.technicianName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("وصول: ${offer.estimatedArrivalTime}", fontSize = 12.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Text("${offer.price} ر.ي", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF2E7D32))
                                }

                                if (offer.notes.isNotBlank()) {
                                    Text(offer.notes, fontSize = 12.sp, style = MaterialTheme.typography.bodySmall)
                                }

                                HorizontalDivider()

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { onNavigateToChat(offer.technicianPhone, offer.technicianName) },
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("محادثة", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val uri = Uri.parse("geo:${offer.technicianLatitude},${offer.technicianLongitude}?q=${offer.technicianLatitude},${offer.technicianLongitude}")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                            context.startActivity(mapIntent)
                                        },
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("الخريطة", fontSize = 12.sp)
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

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("إلغاء الطلب العاجل") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يرجى إدخال رمز PIN السري (4 أرقام):")
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) enteredPin = it },
                        label = { Text("رمز PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expectedPin = request?.secretPin ?: request?.cancellationPassword ?: ""
                        if (enteredPin != expectedPin) {
                            Toast.makeText(context, "رمز PIN غير صحيح!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isCancelling = true
                        scope.launch {
                            firestore.collection("instant_requests").document(requestId)
                                .update("status", "CANCELLED")
                                .addOnSuccessListener {
                                    isCancelling = false
                                    showCancelDialog = false
                                    Toast.makeText(context, "تم إلغاء الطلب العاجل", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("تأكيد الإلغاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("تراجع") }
            }
        )
    }
}
