package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==============================================================================
// 1. نماذج البيانات (Data Models)
// ==============================================================================
data class BusinessProductItem(
    val id: String,
    val title: String,
    val price: String,
    val category: String,
    val isAvailable: Boolean = true
)

data class ReviewItem(
    val clientName: String,
    val rating: Float,
    val comment: String,
    val date: String
)

data class BusinessProfileState(
    val id: String = "store_101",
    val name: String = "مركز الخليج لصيانة وإلكترونيات السيارات",
    val category: String = "مراكز صيانة وسيارات",
    val isVerified: Boolean = true,
    val rating: Float = 4.9f,
    val totalReviews: Int = 128,
    val location: String = "صنعاء - شارع حدة - مقابل الجندول",
    val workingHours: String = "من 8:00 صباحاً - حتى 10:00 مساءً",
    val isOwnerOrAdmin: Boolean = false, // لتحديد إظهار أزرار الإدارة
    val products: List<BusinessProductItem> = listOf(
        BusinessProductItem("1", "فحص كمبيوتر شامل + برمجيات", "5,000 ر.ي", "صيانة"),
        BusinessProductItem("2", "تغيير زيت وفلتر أصلي", "12,000 ر.ي", "زيوت"),
        BusinessProductItem("3", "بطارية هانكوك 60 أمبير", "45,000 ر.ي", "قطع غيار")
    ),
    val reviews: List<ReviewItem> = listOf(
        ReviewItem("محمد العنسي", 5.0f, "خدمة ممتازة وسريعة والضمان مضمون.", "قبل يومين"),
        ReviewItem("أحمد باجمال", 4.8f, "تعامل راقي وأسعارهم مناسبة جداً.", "قبل أسبوع")
    )
)

// ==============================================================================
// 2. الشاشة الرئيسية لبروفايل المحلات والمراكز (Full Unified Profile)
// ==============================================================================
@Composable
fun ProviderAndBusinessProfileScreen(
    profileState: BusinessProfileState = BusinessProfileState(),
    onAgoraCallClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onProductOrderClick: (BusinessProductItem) -> Unit = {},
    onAddProductClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("🛍️ المنتجات/الخدمات", "📍 الموقع والدوام", "⭐️ التقييمات", "💬 التواصل")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // --- الهيدر الغلاف والمعلومات الرئيسية ---
        ProfileTopHeaderSection(
            profileState = profileState,
            onAgoraCallClick = onAgoraCallClick,
            onChatClick = onChatClick
        )

        // --- أزرار إدارة الحساب (تظهر فقط للأدمن أو صاحب المحل) ---
        if (profileState.isOwnerOrAdmin) {
            Surface(
                color = Color(0xFF1E293B),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚙️ لوحة إدارة الحساب والتعديل السريع", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = onAddProductClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("+ إضافة منتج/خدمة", fontSize = 11.sp)
                    }
                }
            }
        }

        // --- التبويبات الأربعة المتطورة ---
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color(0xFF1E293B),
            contentColor = Color.White,
            divider = {}
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- محتوى التبويب المختار ---
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            when (selectedTabIndex) {
                0 -> ProductsAndServicesTab(products = profileState.products, onOrderClick = onProductOrderClick)
                1 -> LocationAndHoursTab(location = profileState.location, workingHours = profileState.workingHours)
                2 -> ReviewsAndRatingsTab(rating = profileState.rating, totalReviews = profileState.totalReviews, reviews = profileState.reviews)
                3 -> DirectContactTab(onAgoraCallClick = onAgoraCallClick, onChatClick = onChatClick)
            }
        }
    }
}

// ==============================================================================
// 3. المكونات الفرعية للتبويبات (Sub-components)
// ==============================================================================

@Composable
fun ProfileTopHeaderSection(
    profileState: BusinessProfileState,
    onAgoraCallClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏪", fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = profileState.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (profileState.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = "موثوق", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(text = profileState.category, color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text(text = "⭐️ ${profileState.rating} (${profileState.totalReviews} تقييم)", color = Color(0xFFFBBF24), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // أزرار الاتصال والمحادثة السريعة
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAgoraCallClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مكالمة صوتية Agora", fontSize = 12.sp)
                }

                Button(
                    onClick = onChatClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("محادثة فورية", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ProductsAndServicesTab(
    products: List<BusinessProductItem>,
    onOrderClick: (BusinessProductItem) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(products) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = item.category, color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(text = item.price, color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { onOrderClick(item) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("طلب الآن", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LocationAndHoursTab(location: String, workingHours: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFEF4444))
                Spacer(modifier = Modifier.width(8.dp))
                Text("الموقع الجغرافي:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text(text = location, color = Color(0xFF94A3B8), fontSize = 13.sp, modifier = Modifier.padding(start = 32.dp, top = 4.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFFFBBF24))
                Spacer(modifier = Modifier.width(8.dp))
                Text("أوقات وساعات العمل:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text(text = workingHours, color = Color(0xFF94A3B8), fontSize = 13.sp, modifier = Modifier.padding(start = 32.dp, top = 4.dp))
        }
    }
}

@Composable
fun ReviewsAndRatingsTab(rating: Float, totalReviews: Int, reviews: List<ReviewItem>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("التقييم الإجمالي", color = Color.White, fontSize = 13.sp)
                        Text("⭐️ $rating من 5.0", color = Color(0xFFFBBF24), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("إجمالي $totalReviews تقييم معتمد", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }
        }

        items(reviews) { rev ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = rev.clientName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "⭐️ ${rev.rating}", color = Color(0xFFFBBF24), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = rev.comment, color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text(text = rev.date, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
                }
            }
        }
    }
}

@Composable
fun DirectContactTab(onAgoraCallClick: () -> Unit, onChatClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("تواصل مع المركز بشكل مباشر ودمج حماية الضامن", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAgoraCallClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Call, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("بدء مكالمة مجانية عالية الدقة (Agora)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onChatClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Email, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("فتح المحادثة الفورية")
            }
        }
    }
}
