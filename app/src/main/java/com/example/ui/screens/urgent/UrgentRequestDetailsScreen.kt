package com.example.ui.screens.urgent

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.dialogs.MultiDimensionRatingDialog
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette
import com.example.viewmodels.UrgentViewModel

/**
 * 🚨 UrgentRequestDetailsScreen
 * Main scaffold and container for Urgent Request Details with 30-min timer and PIN secure cancellation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentRequestDetailsScreen(
    requestId: String,
    viewModel: MainViewModel,
    urgentViewModel: UrgentViewModel = viewModel(),
    themeColors: VisualThemePalette? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToOfferSelection: (offerId: String) -> Unit = {},
    onNavigateToUrgentOfferSubmission: (requestId: String) -> Unit = {},
    onNavigateToChat: (phone: String, name: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val settingsState by viewModel.settings.collectAsState()
    val activeTheme = themeColors ?: resolveThemePalette(settingsState)

    val currentUserId by viewModel.currentUserId.collectAsState()
    val isProvider = viewModel.isProviderUser

    val request by urgentViewModel.selectedRequest.collectAsState()
    val offers by urgentViewModel.offersForRequest.collectAsState()

    var showCancelDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFEBEE))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val req = request
            if (req == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFD32F2F))
                }
            } else {
                UrgentRequestDetailsContent(request = req)

                UrgentOffersList(
                    offers = offers,
                    isOwner = req.userId == currentUserId,
                    onAcceptOffer = { offer ->
                        urgentViewModel.acceptOffer(req.id, offer.id, offer.technicianPhone) { success, msg ->
                            if (success) {
                                onNavigateToOfferSelection(offer.id)
                            }
                        }
                    },
                    onContactProvider = { phone, name ->
                        if (phone.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        } else {
                            onNavigateToChat(phone, name)
                        }
                    }
                )

                if (isProvider && (req.status == "WAITING_FOR_OFFERS" || req.status == "REVIEWING_OFFERS")) {
                    Button(
                        onClick = { onNavigateToUrgentOfferSubmission(req.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تقديم عرض سريع الآن ⚡", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                if (!isProvider && (req.status == "COMPLETED" || req.status == "ACCEPTED")) {
                    Button(
                        onClick = { showRatingDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تقييم الفني / مقدم الخدمة ⭐", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showRatingDialog && request != null) {
        val techId = request!!.acceptedTechnicianId.ifEmpty { request!!.id }
        val techName = request!!.acceptedTechnicianName.ifEmpty { "الفني المنفذ" }
        MultiDimensionRatingDialog(
            targetId = techId,
            targetName = techName,
            targetType = "URGENT_PROVIDER",
            bookingId = request!!.requestCode,
            viewModel = viewModel,
            themeColors = activeTheme,
            onDismiss = { showRatingDialog = false }
        )
    }

    if (showCancelDialog && request != null) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("إلغاء الطلب العاجل", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل رمز PIN التأكيدي لإلغاء هذا الطلب:")
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 6) enteredPin = it },
                        label = { Text("رمز PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        urgentViewModel.cancelUrgentRequest(request!!.id, enteredPin, context) { success, msg ->
                            showCancelDialog = false
                            if (success) {
                                onNavigateBack()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("تأكيد الإلغاء ❌", color = Color.White)
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
