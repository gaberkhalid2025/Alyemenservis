package com.example.screenshots

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Test

/**
 * 🛠️ ServicesListScreenScreenshotTest
 * Visual screenshot testing for the Services List Screen covering:
 * 1. Loading state (spinning progress indicator in center)
 * 2. Empty state ("لا توجد خدمات متاحة حالياً")
 * 3. Populated state (at least 3 services with realistic Yemeni/Arabic names)
 * Across Light, Dark, RTL, and LTR.
 */
class ServicesListScreenScreenshotTest : RoborazziTestBase() {

    private val sampleServices = listOf(
        ServiceScreenshotItem(
            id = "srv_1",
            title = "صيانة وتركيب الطاقة الشمسية",
            category = "طاقة بديلة وكهرباء",
            price = "25,000 ريال يمني",
            rating = 4.9f,
            reviewsCount = 48
        ),
        ServiceScreenshotItem(
            id = "srv_2",
            title = "إصلاح وصيانة المكيفات المركزية والسبليت",
            category = "تكييف وتبريد",
            price = "15,000 ريال يمني",
            rating = 4.8f,
            reviewsCount = 35
        ),
        ServiceScreenshotItem(
            id = "srv_3",
            title = "تمديدات كهربائية منزلية متكاملة",
            category = "كهرباء وتأسيس",
            price = "30,000 ريال يمني",
            rating = 5.0f,
            reviewsCount = 62
        )
    )

    // 1. Loading State Tests
    @Test
    fun servicesList_loading_light() {
        captureScreen(
            screenshotName = "services_list_loading_light",
            darkTheme = false,
            layoutDirection = LayoutDirection.Rtl
        ) {
            ServicesListScreenContent(state = ServicesListState.Loading)
        }
    }

    @Test
    fun servicesList_loading_dark() {
        captureScreen(
            screenshotName = "services_list_loading_dark",
            darkTheme = true,
            layoutDirection = LayoutDirection.Rtl
        ) {
            ServicesListScreenContent(state = ServicesListState.Loading)
        }
    }

    // 2. Empty State Tests
    @Test
    fun servicesList_empty_light() {
        captureScreen(
            screenshotName = "services_list_empty_light",
            darkTheme = false,
            layoutDirection = LayoutDirection.Rtl
        ) {
            ServicesListScreenContent(state = ServicesListState.Empty)
        }
    }

    @Test
    fun servicesList_empty_dark() {
        captureScreen(
            screenshotName = "services_list_empty_dark",
            darkTheme = true,
            layoutDirection = LayoutDirection.Rtl
        ) {
            ServicesListScreenContent(state = ServicesListState.Empty)
        }
    }

    // 3. Populated Data State Tests (Light, Dark, RTL, LTR)
    @Test
    fun servicesList_data_light() {
        captureScreen(
            screenshotName = "services_list_data_light",
            darkTheme = false,
            layoutDirection = LayoutDirection.Rtl
        ) {
            ServicesListScreenContent(state = ServicesListState.Success(sampleServices))
        }
    }

    @Test
    fun servicesList_data_dark() {
        captureScreen(
            screenshotName = "services_list_data_dark",
            darkTheme = true,
            layoutDirection = LayoutDirection.Rtl
        ) {
            ServicesListScreenContent(state = ServicesListState.Success(sampleServices))
        }
    }

    @Test
    fun servicesList_data_rtl() {
        captureScreen(
            screenshotName = "services_list_data_rtl",
            darkTheme = false,
            layoutDirection = LayoutDirection.Rtl
        ) {
            ServicesListScreenContent(state = ServicesListState.Success(sampleServices))
        }
    }

    @Test
    fun servicesList_data_ltr() {
        captureScreen(
            screenshotName = "services_list_data_ltr",
            darkTheme = false,
            layoutDirection = LayoutDirection.Ltr
        ) {
            ServicesListScreenContent(state = ServicesListState.Success(sampleServices))
        }
    }
}

private data class ServiceScreenshotItem(
    val id: String,
    val title: String,
    val category: String,
    val price: String,
    val rating: Float,
    val reviewsCount: Int
)

private sealed class ServicesListState {
    object Loading : ServicesListState()
    object Empty : ServicesListState()
    data class Success(val items: List<ServiceScreenshotItem>) : ServicesListState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServicesListScreenContent(state: ServicesListState) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "قائمة الخدمات المتاحة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state) {
                is ServicesListState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "جاري تحميل قائمة الخدمات...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is ServicesListState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "لا توجد خدمات متاحة حالياً",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                            )
                        }
                    }
                }
                is ServicesListState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.items, key = { it.id }) { service ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = service.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFF59E0B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${service.rating}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = service.category,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = service.price,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Button(
                                            onClick = {},
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text("طلب الخدمة", fontSize = 12.sp)
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
}
