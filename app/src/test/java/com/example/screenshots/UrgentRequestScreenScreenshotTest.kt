package com.example.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Test

/**
 * 🚨 UrgentRequestScreenScreenshotTest
 * Visual screenshot testing for the Urgent Emergency Request Screen:
 * - Active emergency request with countdown timer
 * - 3 price quotes from distinct technicians with prices, arrival times, and ratings
 * Across Light, Dark, RTL, and LTR.
 */
class UrgentRequestScreenScreenshotTest : RoborazziTestBase() {

    @Test
    fun urgentRequest_light() {
        captureScreen(
            screenshotName = "urgent_request_light",
            darkTheme = false,
            layoutDirection = LayoutDirection.Rtl
        ) {
            UrgentRequestScreenTestContent()
        }
    }

    @Test
    fun urgentRequest_dark() {
        captureScreen(
            screenshotName = "urgent_request_dark",
            darkTheme = true,
            layoutDirection = LayoutDirection.Rtl
        ) {
            UrgentRequestScreenTestContent()
        }
    }

    @Test
    fun urgentRequest_rtl() {
        captureScreen(
            screenshotName = "urgent_request_rtl",
            darkTheme = false,
            layoutDirection = LayoutDirection.Rtl
        ) {
            UrgentRequestScreenTestContent()
        }
    }

    @Test
    fun urgentRequest_ltr() {
        captureScreen(
            screenshotName = "urgent_request_ltr",
            darkTheme = false,
            layoutDirection = LayoutDirection.Ltr
        ) {
            UrgentRequestScreenTestContent()
        }
    }
}

private data class UrgentOfferItem(
    val id: String,
    val technicianName: String,
    val price: String,
    val arrivalTime: String,
    val rating: Float,
    val reviewsCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UrgentRequestScreenTestContent() {
    val offers = listOf(
        UrgentOfferItem(
            id = "off_1",
            technicianName = "المهندس عادل الشميري",
            price = "12,000 ريال يمني",
            arrivalTime = "وصول خلال 15 دقيقة",
            rating = 4.9f,
            reviewsCount = 58
        ),
        UrgentOfferItem(
            id = "off_2",
            technicianName = "الفني سالم باوزير",
            price = "10,000 ريال يمني",
            arrivalTime = "وصول خلال 25 دقيقة",
            rating = 4.8f,
            reviewsCount = 34
        ),
        UrgentOfferItem(
            id = "off_3",
            technicianName = "المهندس مروان القدسي",
            price = "14,000 ريال يمني",
            arrivalTime = "وصول خلال 10 دقائق",
            rating = 5.0f,
            reviewsCount = 92
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "طلب طوارئ نشط #EMG-441",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "كهرباء وصيانة عاجلة",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Urgent Header Card with Countdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFDC2626).copy(alpha = 0.12f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFDC2626).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDC2626)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "حالة الطلب: قيد استقبال العروض",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                    Text(
                                        text = "صنعاء - شارع الستين الغربي",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Countdown Timer Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFDC2626).copy(alpha = 0.18f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الوقت المتبقي لانتهاء استقبال العروض:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "00:14:35",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }

            // Offers Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "عروض الأسعار المستلمة (${offers.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "اختر العرض الأنسب",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Offers List
            items(offers, key = { it.id }) { offer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = offer.technicianName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${offer.rating} (${offer.reviewsCount} تقييم)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = offer.price,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⏱️ ${offer.arrivalTime}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {},
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اتصال", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {},
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("قبول العرض", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
