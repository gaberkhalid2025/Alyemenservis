package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*

/**
 * 🏪 StoreProfileScreen:
 * Modern 6-Tab Profile Engine for Shops, Centers, Clinics & Establishments.
 * Dual View Architecture (Visitor View vs Owner Management Control Panel).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreProfileScreen(
    store: StoreEntity,
    isOwner: Boolean,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onDirectChat: () -> Unit = {},
    onRequestBooking: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTabIdx by remember { mutableIntStateOf(0) }

    val tabs = remember(isOwner) {
        if (isOwner) {
            listOf(
                "النبذة والمعلومات ℹ️",
                "المنتجات والخدمات 🛒",
                "العروض والخصومات 🏷️",
                "المنيو والكتالوج 📄",
                "التقييمات والآراء ⭐",
                "لوحة المالك والإعدادات ⚙️"
            )
        } else {
            listOf(
                "النبذة والمعلومات ℹ️",
                "المنتجات والخدمات 🛒",
                "العروض والخصومات 🏷️",
                "المنيو والكتالوج 📄",
                "التقييمات والآراء ⭐",
                "حجز موعد/طلب 📅"
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(store.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        if (isOwner) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(4.dp)) {
                                Text("صاحب المركز (حساب موثق)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable 6-Tab Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTabIdx,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8),
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIdx == index,
                        onClick = { selectedTabIdx = index },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                }
            }

            // Tab Content Switcher
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                when (selectedTabIdx) {
                    0 -> OverviewTabContent(store = store, isOwner = isOwner, onDirectChat = onDirectChat)
                    1 -> ProductsTabContent(store = store, isOwner = isOwner)
                    2 -> OffersTabContent(store = store, isOwner = isOwner)
                    3 -> MenuCatalogTabContent(store = store, isOwner = isOwner)
                    4 -> ReviewsTabContent(store = store, isOwner = isOwner)
                    5 -> if (isOwner) OwnerSettingsTabContent(store = store) else BookingsTabContent(store = store, isOwner = false, onRequestBooking = onRequestBooking)
                }
            }
        }
    }
}

// ------ Tab 1: Overview ------
@Composable
private fun OverviewTabContent(
    store: StoreEntity,
    isOwner: Boolean,
    onDirectChat: () -> Unit
) {
    val context = LocalContext.current
    var editHours by remember { mutableStateOf(store.workingHours) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            // Cover Image & Logo Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
            ) {
                if (store.coverImage.isNotEmpty()) {
                    AsyncImage(
                        model = store.coverImage,
                        contentDescription = "غلاف المركز",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Logo Circle Overlay
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .border(2.dp, Color(0xFF10B981), CircleShape)
                        .align(Alignment.BottomStart),
                    contentAlignment = Alignment.Center
                ) {
                    if (store.logoImage.isNotEmpty()) {
                        AsyncImage(model = store.logoImage, contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Text("🏪", fontSize = 28.sp)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("نبذة عن المركز والخدمات", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = store.description.ifEmpty { "يقدم المركز أفضل الخدمات والمنتجات التجارية مع التزام تام بالجودة والمواعيد." },
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("⏰ أوقات الدوام: ${store.workingHours}", fontSize = 12.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    Text("📍 العنوان: ${store.cityId} • ${store.localNeighborhood}", fontSize = 12.sp, color = Color(0xFF94A3B8))

                    if (isOwner) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editHours,
                            onValueChange = { editHours = it },
                            label = { Text("تحديث أوقات الدوام (لصاحب المركز)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${store.phone}"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("اتصال مباشر 📞", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDirectChat,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("محادثة فورية 💬", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ------ Tab 2: Products Catalog ------
@Composable
private fun ProductsTabContent(store: StoreEntity, isOwner: Boolean) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isOwner) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("إضافة منتج/سلعة جديدة ➕", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("قائمة المنتجات متوفرة ويتم تحديثها مباشرة من صاحب المركز 🛒", color = Color.White, fontSize = 13.sp)
        }
    }
}

// ------ Tab 3: Offered Services ------
@Composable
private fun ServicesTabContent(store: StoreEntity, isOwner: Boolean) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("الخدمات المتاحة لدى المركز 🛠️", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("خدمة الصيانة والفحص الشامل", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("سعر تقديري: حسب المعاينة", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
                Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(6.dp)) {
                    Text("متاحة ✅", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

// ------ Tab 4: Bookings Management ------
@Composable
private fun BookingsTabContent(
    store: StoreEntity,
    isOwner: Boolean,
    onRequestBooking: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isOwner) {
            Text("لوحة إدارة طلبات الحجز الواردة للمركز 📅", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("يمكنك هنا قبول أو رفض الحجوزات وتحديد أيام العطل والأوقات المتاحة.", color = Color(0xFF94A3B8), fontSize = 12.sp)
        } else {
            Text("حجز موعد مباشر مع ${store.name} 📅", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequestBooking,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("تأكيد حجز الموعد الآن ⚡", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

// ------ Tab 5: Reviews & Feedback ------
@Composable
private fun ReviewsTabContent(store: StoreEntity, isOwner: Boolean) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تقييمات وآراء العملاء ⭐", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            Text("${store.rating} / 5.0 (${store.numReviews} تقييم)", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("عميل ممتاز", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Text("خدمة راقية وسريعة وتجربة ممتازة جداً ننصح بالتعامل معهم.", color = Color(0xFFCBD5E1), fontSize = 12.sp)

                if (isOwner) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { }) {
                        Text("الرد على التعليق 💬", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ------ Tab 3: Offers & Discounts ------
@Composable
private fun OffersTabContent(store: StoreEntity, isOwner: Boolean) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏷️ العروض والتخفيضات المتاحة", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            if (isOwner) {
                Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(6.dp)) {
                    Text("إضافة عرض جديد ➕", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFEF4444)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFFEF4444), shape = RoundedCornerShape(6.dp)) {
                        Text("خصم 20%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("عرض الافتتاح والخدمات الشاملة", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("احصل على خصم فوري بقيمة 20% عند الطلب عبر التطبيق مباشرة.", color = Color(0xFFCBD5E1), fontSize = 12.sp)
            }
        }
    }
}

// ------ Tab 4: Catalog & Menu View ------
@Composable
private fun MenuCatalogTabContent(store: StoreEntity, isOwner: Boolean) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📄 المنيو والكتالوج الكامل", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
        Text("تصفح المنيو الرقمي للأسعار والخدمات المتاحة لدى ${store.name}:", color = Color(0xFF94A3B8), fontSize = 12.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.List, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("الكتالوج الرقمي الرسمي (.PDF / Images)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        Toast.makeText(context, "جاري فتح الكتالوج والمنيو التفاعلي...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text("تحميل / فتح المنيو والكتالوج ⬇️", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

// ------ Tab 6: Owner Settings ------
@Composable
private fun OwnerSettingsTabContent(store: StoreEntity) {
    var isOpen by remember { mutableStateOf(store.isActive) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("إعدادات التحكم الخاصة بالمالك ⚙️", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("حالة المركز (مفتوح / مغلق)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Switch(checked = isOpen, onCheckedChange = { isOpen = it })
                }
            }
        }
    }
}
