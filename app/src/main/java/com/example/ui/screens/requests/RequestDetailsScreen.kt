package com.example.ui.screens.requests

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.InstantRequestEntity
import com.example.data.RequestOfferEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AppSnackbarHost
import com.example.ui.components.SnackbarType
import com.example.ui.components.showCustomSnackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🔍 RequestDetailsScreen
 * شاشة تفاصيل طلب الخدمة مع العروض وحماية الإلغاء برمز PIN
 * متصلة بـ RequestsViewModel و AppSnackbar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailsScreen(
    requestId: String,
    viewModel: MainViewModel,
    requestsViewModel: RequestsViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToOfferSubmission: (requestId: String) -> Unit = {},
    onNavigateToOfferSelection: (offerId: String) -> Unit = {},
    onNavigateToChat: (phone: String, name: String) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserId by viewModel.currentUserId.collectAsState()
    val isProvider = viewModel.isProviderUser

    val currentRequest by requestsViewModel.currentRequest.collectAsState()
    val offersList by requestsViewModel.currentOffers.collectAsState()

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelPinInput by remember { mutableStateOf("") }
    var isCancelling by remember { mutableStateOf(false) }

    LaunchedEffect(requestId) {
        requestsViewModel.listenToRequestDetails(requestId)
    }

    Scaffold(
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentRequest?.requestCode?.ifBlank { "تفاصيل الطلب" } ?: "تفاصيل الطلب",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    val status = currentRequest?.status
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
        val req = currentRequest
        if (req == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val isMyRequest = req.userId == currentUserId || req.userPhone == currentUserId

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
                            Text(req.requestCode, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Badge(
                                containerColor = when (req.status) {
                                    "WAITING_FOR_OFFERS" -> Color(0xFF1976D2)
                                    "COMPLETED", "ACCEPTED" -> Color(0xFF2E7D32)
                                    "CANCELLED" -> Color(0xFFD32F2F)
                                    else -> Color(0xFFFFA000)
                                }
                            ) {
                                Text(
                                    text = when (req.status) {
                                        "WAITING_FOR_OFFERS" -> "بانتظار العروض"
                                        "COMPLETED", "ACCEPTED" -> "مكتمل"
                                        "CANCELLED" -> "ملغي"
                                        else -> req.status
                                    },
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Text(req.serviceTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(req.description, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)

                        HorizontalDivider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("${req.userCity} - ${req.userNeighborhood}", fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                Text(req.urgencyTime, fontSize = 13.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }

                        val dateFormatted = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date(req.createdAt))
                        Text("تاريخ النشر: $dateFormatted", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            // زر تقديم عرض للفنيين
            if (isProvider && (req.status == "WAITING_FOR_OFFERS" || req.status == "REVIEWING_OFFERS")) {
                item {
                    Button(
                        onClick = { onNavigateToOfferSubmission(req.id) },
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
                    items(offersList, key = { it.id }) { offer ->
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
                        val expectedPin = currentRequest?.secretPin ?: currentRequest?.cancellationPassword ?: ""
                        isCancelling = true
                        requestsViewModel.cancelRequest(
                            requestId = requestId,
                            enteredPin = cancelPinInput,
                            expectedPin = expectedPin,
                            onSuccess = {
                                isCancelling = false
                                showCancelDialog = false
                                scope.launch {
                                    snackbarHostState.showCustomSnackbar(
                                        message = "تم إلغاء الطلب بنجاح",
                                        type = SnackbarType.SUCCESS
                                    )
                                }
                            },
                            onError = { err ->
                                isCancelling = false
                                scope.launch {
                                    snackbarHostState.showCustomSnackbar(
                                        message = err,
                                        type = SnackbarType.ERROR
                                    )
                                }
                            }
                        )
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
