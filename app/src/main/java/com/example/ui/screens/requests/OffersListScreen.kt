package com.example.ui.screens.requests

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.InstantRequestEntity
import com.example.data.RequestOfferEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AppSnackbarHost

/**
 * 📋 OffersListScreen
 * شاشة مقارنة العروض المستلمة للطلب (ترتيب حسب السعر، وقت الوصول، التقييم)
 * متصلة بـ RequestsViewModel و AppSnackbarHost
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersListScreen(
    requestId: String,
    viewModel: MainViewModel,
    requestsViewModel: RequestsViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onSelectOffer: (offerId: String) -> Unit = {},
    onNavigateToChat: (phone: String, name: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val currentRequest by requestsViewModel.currentRequest.collectAsState()
    val offersList by requestsViewModel.currentOffers.collectAsState()

    var sortBy by remember { mutableStateOf("PRICE_LOW") } // PRICE_LOW, FASTEST, RATING

    LaunchedEffect(requestId) {
        requestsViewModel.listenToRequestDetails(requestId)
    }

    val sortedOffers = when (sortBy) {
        "PRICE_LOW" -> offersList.sortedBy { it.price }
        "FASTEST" -> offersList.sortedBy { it.estimatedArrivalTime }
        "RATING" -> offersList.sortedByDescending { it.technicianRating }
        else -> offersList
    }

    Scaffold(
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("العروض المقدمة للطلب", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        currentRequest?.let {
                            Text(it.requestCode, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // شريط الفرز والمقارنة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ترتيب حسب:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                FilterChip(
                    selected = sortBy == "PRICE_LOW",
                    onClick = { sortBy = "PRICE_LOW" },
                    label = { Text("الأقل سعراً", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                FilterChip(
                    selected = sortBy == "FASTEST",
                    onClick = { sortBy = "FASTEST" },
                    label = { Text("الأسرع وصولاً", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                FilterChip(
                    selected = sortBy == "RATING",
                    onClick = { sortBy = "RATING" },
                    label = { Text("الأعلى تقييماً", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }

            if (sortedOffers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(54.dp), tint = Color.Gray)
                        Text("لم تصل أي عروض حتى الآن.", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("سيصلك إشعار فوري عند تقديم أي فني لعرض سعر لطلبك.", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    items(sortedOffers, key = { it.id }) { offer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("compare_offer_card_${offer.id}"),
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
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(offer.technicianName.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        Column {
                                            Text(offer.technicianName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                                                Text(" ${offer.technicianRating} (ممتاز)", fontSize = 12.sp, color = Color.DarkGray)
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${offer.price} ر.ي", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF2E7D32))
                                        Text("شامل الخدمة", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                HorizontalDivider()

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text("الوصول: ${offer.estimatedArrivalTime}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                        Text("المدة: ${offer.estimatedDuration}", fontSize = 13.sp)
                                    }
                                }

                                if (offer.notes.isNotBlank()) {
                                    Text("ملاحظة: ${offer.notes}", fontSize = 12.sp, color = Color.DarkGray)
                                }

                                HorizontalDivider()

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onNavigateToChat(offer.technicianPhone, offer.technicianName) },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("محادثة", fontSize = 13.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val uri = Uri.parse("geo:${offer.technicianLatitude},${offer.technicianLongitude}?q=${offer.technicianLatitude},${offer.technicianLongitude}")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                            context.startActivity(mapIntent)
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("الخريطة", fontSize = 13.sp)
                                    }

                                    Button(
                                        onClick = { onSelectOffer(offer.id) },
                                        modifier = Modifier.weight(1.3f).height(40.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Text("اختيار وتأكيد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
