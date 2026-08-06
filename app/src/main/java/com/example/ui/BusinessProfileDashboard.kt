package com.example.ui

import com.example.utils.*

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UnifiedBusinessProfileDashboard(
    accountType: String, // "TECHNICIAN", "STORE", "RESTAURANT", "MEDICAL", "REAL_ESTATE"
    providerId: String,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val providers by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val ratings by viewModel.ratings.collectAsState()
    val products by viewModel.products.collectAsState()

    // Find entities safely
    val activeProvider = remember(providers, providerId) { providers.find { it.id == providerId } }
    val activeStore = remember(stores, providerId) { stores.find { it.id == providerId || it.ownerId == providerId } }
    val activeProperty = remember(properties, providerId) { properties.find { it.id == providerId || it.phone == providerId } }

    val name = activeProvider?.name ?: activeStore?.name ?: activeProperty?.title ?: "ملف تجاري معتمد"
    val description = if (activeProvider != null) {
        if (activeProvider.profession.isNotEmpty()) activeProvider.profession else activeProvider.specialization
    } else {
        activeStore?.description ?: activeProperty?.description ?: "وصف النشاط التجاري بالمنصة"
    }
    val phone = activeProvider?.phone ?: activeStore?.phone ?: activeProperty?.phone ?: ""
    val address = activeProvider?.localNeighborhood ?: activeStore?.localNeighborhood ?: activeProperty?.localNeighborhood ?: ""
    val ratingValue = activeProvider?.rating ?: activeStore?.rating ?: activeProperty?.rating ?: 5.0f
    val reviewCount = activeProvider?.numReviews ?: activeStore?.numReviews ?: activeProperty?.numReviews ?: 0
    val profileImage = activeProvider?.profileImage ?: activeStore?.logoImage ?: activeProperty?.images?.firstOrNull() ?: ""

    // 6 Customized Tabs based on account type
    val tabs = remember(accountType) {
        when (accountType) {
            "TECHNICIAN" -> listOf(
                "بيانات الملف" to Icons.Default.Person,
                "الخدمات والأسعار" to Icons.Default.Build,
                "معرض الأعمال" to Icons.Default.PlayArrow,
                "آراء العملاء" to Icons.Default.Star,
                "طلبات الحجز" to Icons.Default.DateRange,
                "إحصائيات الأداء" to Icons.Default.Info
            )
            "STORE" -> listOf(
                "بيانات المتجر" to Icons.Default.Home,
                "إدارة المنتجات" to Icons.Default.ShoppingCart,
                "كتالوج الأسعار" to Icons.Default.List,
                "تقييمات المتجر" to Icons.Default.Star,
                "طلبات العملاء" to Icons.Default.MailOutline,
                "تقارير النمو" to Icons.Default.Info
            )
            "RESTAURANT" -> listOf(
                "ملف المطعم" to Icons.Default.Home,
                "قائمة الأكلات" to Icons.Default.Menu,
                "عروض وتنزيلات" to Icons.Default.Favorite,
                "آراء الذواقة" to Icons.Default.Star,
                "حجوزات الطاولات" to Icons.Default.DateRange,
                "مبيعات اليوم" to Icons.Default.Info
            )
            "MEDICAL" -> listOf(
                "ملف العيادة" to Icons.Default.Home,
                "العيادات والأطباء" to Icons.Default.AccountBox,
                "بروشور الخدمات" to Icons.Default.List,
                "تقييمات المرضى" to Icons.Default.Star,
                "حجز المواعيد" to Icons.Default.DateRange,
                "أداء العيادات" to Icons.Default.Info
            )
            "REAL_ESTATE" -> listOf(
                "ملف المكتب" to Icons.Default.Home,
                "إدارة العقارات" to Icons.Default.LocationOn,
                "معرض الصور" to Icons.Default.PlayArrow,
                "آراء المستأجرين" to Icons.Default.Star,
                "الطلبات والزيارات" to Icons.Default.DateRange,
                "إحصائيات السوق" to Icons.Default.Info
            )
            else -> listOf(
                "الملف التعريفي" to Icons.Default.Person,
                "المنتجات والخدمات" to Icons.Default.Build,
                "الألبومات والمعرض" to Icons.Default.PlayArrow,
                "الآراء والتقييمات" to Icons.Default.Star,
                "الحجوزات والطلبات" to Icons.Default.DateRange,
                "الإحصائيات والنمو" to Icons.Default.Info
            )
        }
    }

    var selectedTabIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
    ) {
        // 🌟 GORGEOUS HIGH-FIDELITY HEADER DESIGN
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            // Background Header Banner
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(themeColors.primary, themeColors.secondary)
                        )
                    )
            ) {
                // Diagonal accent lines for modern feel
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawPath(
                        path = Path().apply {
                            moveTo(0f, size.height * 0.4f)
                            lineTo(size.width, size.height * 0.1f)
                            lineTo(size.width, 0f)
                            lineTo(0f, 0f)
                            close()
                        },
                        color = Color.White.copy(alpha = 0.05f)
                    )
                }
            }

            // Top Header Action Buttons Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (accountType) {
                            "TECHNICIAN" -> "👷 حساب مهني فني معتمد"
                            "STORE" -> "🏪 حساب متجر معتمد"
                            "RESTAURANT" -> "🍔 حساب مطعم معتمد"
                            "MEDICAL" -> "🏥 حساب مركز طبي معتمد"
                            "REAL_ESTATE" -> "🏠 حساب مكتب عقاري"
                            else -> "✨ حساب تجاري معتمد"
                        },
                        color = themeColors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Points Badge
                val points = activeProvider?.points ?: 100
                Box(
                    modifier = Modifier
                        .background(themeColors.accent, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "⭐ $points نقطة متميزة",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Business Info Overlay Block
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Photo / Avatar
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                        .border(2.dp, themeColors.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (accountType) {
                            "TECHNICIAN" -> "👷"
                            "STORE" -> "🏪"
                            "RESTAURANT" -> "🍔"
                            "MEDICAL" -> "🏥"
                            else -> "🏠"
                        },
                        fontSize = 32.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "موثق",
                            tint = themeColors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = description.take(45) + (if (description.length > 45) "..." else ""),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "⭐ $ratingValue ($reviewCount تقييم)",
                            color = Color.Yellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "📍 $address",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // 🗂️ TAB BAR ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColors.surface)
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tabs.forEachIndexed { idx, (title, icon) ->
                val isSelected = selectedTabIndex == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) themeColors.accent else themeColors.background)
                        .border(
                            1.dp,
                            if (isSelected) themeColors.accent else themeColors.accent.copy(alpha = 0.2f),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedTabIndex = idx }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.Black else themeColors.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = title,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Divider(color = themeColors.accent.copy(alpha = 0.1f), thickness = 1.dp)

        // 🌀 TAB CONTENT WITH SMOOTH TRANSITION
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> TabProfileEdit(
                        accountType = accountType,
                        activeProvider = activeProvider,
                        activeStore = activeStore,
                        activeProperty = activeProperty,
                        viewModel = viewModel,
                        themeColors = themeColors
                    )
                    1 -> TabProductsServices(
                        accountType = accountType,
                        providerId = providerId,
                        activeProvider = activeProvider,
                        activeStore = activeStore,
                        activeProperty = activeProperty,
                        viewModel = viewModel,
                        themeColors = themeColors
                    )
                    2 -> TabGalleryAlbums(
                        accountType = accountType,
                        providerId = providerId,
                        activeStore = activeStore,
                        activeProvider = activeProvider,
                        viewModel = viewModel,
                        themeColors = themeColors
                    )
                    3 -> TabReviewsFeedback(
                        providerId = providerId,
                        ratings = ratings,
                        viewModel = viewModel,
                        themeColors = themeColors
                    )
                    4 -> TabBookingsOrders(
                        providerId = providerId,
                        bookings = bookings,
                        viewModel = viewModel,
                        themeColors = themeColors
                    )
                    5 -> TabStatisticsGrowth(
                        providerId = providerId,
                        bookings = bookings,
                        ratings = ratings,
                        themeColors = themeColors
                    )
                }
            }
        }
    }
}

// ==========================================
// 📝 TAB 1: PROFILE MANAGEMENT & EDIT
// ==========================================
@Composable
fun TabProfileEdit(
    accountType: String,
    activeProvider: ProviderEntity?,
    activeStore: StoreEntity?,
    activeProperty: PropertyEntity?,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf(activeProvider?.name ?: activeStore?.name ?: activeProperty?.title ?: "") }
    val initialDesc = if (activeProvider != null) {
        if (activeProvider.profession.isNotEmpty()) activeProvider.profession else activeProvider.specialization
    } else {
        activeStore?.description ?: activeProperty?.description ?: ""
    }
    var descInput by remember { mutableStateOf(initialDesc) }
    var phoneInput by remember { mutableStateOf(activeProvider?.phone ?: activeStore?.phone ?: activeProperty?.phone ?: "") }
    var addressInput by remember { mutableStateOf(activeProvider?.localNeighborhood ?: activeStore?.localNeighborhood ?: activeProperty?.localNeighborhood ?: "") }

    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "📝 تحديث معلومات ونشاط حسابك بالتفصيل:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent
            )

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("الاسم التجاري بالكامل") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            OutlinedTextField(
                value = descInput,
                onValueChange = { descInput = it },
                label = { Text("شرح ووصف الخدمات والمنتجات بالتفصيل") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it },
                label = { Text("رقم هاتف التواصل للزبائن") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            OutlinedTextField(
                value = addressInput,
                onValueChange = { addressInput = it },
                label = { Text("العنوان بالتفصيل والحي السكني") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            // 🔥 SPECIAL OFFERS & DISCOUNTS SECTION
            var offersJsonState by remember(activeStore, activeProvider) {
                mutableStateOf(activeStore?.specialOffersJson ?: activeProvider?.specialOffersJson ?: "")
            }
            SpecialOffersSection(
                offersJson = offersJsonState,
                onOffersChanged = { newOffersJson ->
                    offersJsonState = newOffersJson
                    if (activeProvider != null) {
                        viewModel.updateProviderEntity(activeProvider.copy(specialOffersJson = newOffersJson))
                    } else if (activeStore != null) {
                        viewModel.saveStore(activeStore.copy(specialOffersJson = newOffersJson))
                    }
                },
                isEditable = true,
                themeColors = themeColors
            )

            Button(
                onClick = {
                    if (nameInput.trim().isEmpty() || phoneInput.trim().isEmpty()) {
                        Toast.makeText(context, "⚠️ يرجى تعبئة الاسم ورقم الهاتف!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Save changes
                    if (activeProvider != null) {
                        viewModel.updateProviderEntity(
                            activeProvider.copy(
                                name = nameInput.trim(),
                                profession = descInput.trim(),
                                phone = phoneInput.trim(),
                                localNeighborhood = addressInput.trim()
                            )
                        )
                    } else if (activeStore != null) {
                        viewModel.saveStore(
                            activeStore.copy(
                                name = nameInput.trim(),
                                description = descInput.trim(),
                                phone = phoneInput.trim(),
                                localNeighborhood = addressInput.trim()
                            )
                        )
                    }
                    Toast.makeText(context, "✅ تم حفظ التحديثات بنجاح فوراً!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("حفظ التحديثات والبيانات 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// ==========================================
// 📦 TAB 2: PRODUCTS & SERVICES ATTACHMENTS
// ==========================================
@Composable
fun TabProductsServices(
    accountType: String,
    providerId: String,
    activeProvider: ProviderEntity?,
    activeStore: StoreEntity?,
    activeProperty: PropertyEntity?,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var dashboardAttachments by remember(activeStore, activeProvider) {
        val json = activeStore?.productAttachmentsJson ?: activeProvider?.productAttachmentsJson ?: ""
        mutableStateOf(ProductAttachment.parseList(json))
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var pName by remember { mutableStateOf("") }
    var pDesc by remember { mutableStateOf("") }
    var pPrice by remember { mutableStateOf("") }

    val products by viewModel.products.collectAsState()
    val storeProducts = remember(products, providerId) {
        products.filter { it.storeId == providerId && !it.isDeleted }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📁 إدارة الكتالوج وملفات السلع:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent
            )

            if (accountType == "STORE" || accountType == "RESTAURANT") {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("إضافة مادة ➕", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Render Product Attachments list
        ProductAttachmentsSection(
            attachments = dashboardAttachments,
            onAttachmentsChanged = { updatedList ->
                dashboardAttachments = updatedList
                val json = ProductAttachment.serializeList(updatedList)
                if (activeStore != null) {
                    viewModel.saveStore(activeStore.copy(productAttachmentsJson = json))
                } else if (activeProvider != null) {
                    viewModel.updateProviderEntity(activeProvider.copy(productAttachmentsJson = json))
                }
                Toast.makeText(context, "✅ تم تحديث المرفقات وحفظها بنجاح!", Toast.LENGTH_SHORT).show()
            },
            mode = "MANAGEMENT",
            themeColors = themeColors
        )

        if (storeProducts.isNotEmpty()) {
            Text(
                text = "🛒 قائمة المواد والسلع الحالية:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(storeProducts) { prod ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(prod.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(prod.description, color = Color.LightGray, fontSize = 10.sp)
                                Text("${prod.price} ريال يمني", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(onClick = {
                                viewModel.deleteProduct(prod.id)
                                Toast.makeText(context, "🗑️ تم حذف المنتج بنجاح!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("📦 إضافة مادة جديدة للكتالوج", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            containerColor = themeColors.secondary,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("اسم المادة/الأكلة/الخدمة") })
                    OutlinedTextField(value = pDesc, onValueChange = { pDesc = it }, label = { Text("الوصف والتفاصيل") })
                    OutlinedTextField(value = pPrice, onValueChange = { pPrice = it }, label = { Text("السعر بالريال اليمني") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceD = pPrice.toDoubleOrNull() ?: 0.0
                        if (pName.trim().isNotEmpty() && priceD > 0.0) {
                            viewModel.saveProduct(
                                ProductEntity(
                                    id = java.util.UUID.randomUUID().toString(),
                                    storeId = providerId,
                                    name = pName.trim(),
                                    description = pDesc.trim(),
                                    price = priceD,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                            pName = ""
                            pDesc = ""
                            pPrice = ""
                            showAddDialog = false
                            Toast.makeText(context, "✅ تم إضافة المنتج بنجاح!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "⚠️ يرجى إدخال اسم وسعر صحيح!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("إضافة المنتج ✅", color = Color.Black)
                }
            }
        )
    }
}

// ==========================================
// 🖼️ TAB 3: GALLERY ALBUMS & ATTACHMENTS
// ==========================================
@Composable
fun TabGalleryAlbums(
    accountType: String,
    providerId: String,
    activeStore: StoreEntity?,
    activeProvider: ProviderEntity?,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var albumPhotos by remember(activeStore, activeProvider) {
        val list = activeStore?.images ?: activeProvider?.profileImage?.split(",") ?: emptyList()
        mutableStateOf(list.filter { it.isNotEmpty() })
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val base64 = convertUriToBase64(context, it)
            if (base64.isNotEmpty()) {
                val updatedPhotos = albumPhotos + base64
                albumPhotos = updatedPhotos
                if (activeStore != null) {
                    viewModel.saveStore(activeStore.copy(images = updatedPhotos))
                }
                Toast.makeText(context, "📸 تم إضافة الصورة بنجاح للألبوم!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🖼️ معرض الألبومات والصور الموثقة:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent
            )

            Button(
                onClick = { photoPicker.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("إضافة صورة 📸", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (albumPhotos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("لم تقم برفع أي صور للألبوم الخاص بك حتى الآن.", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(albumPhotos) { base64 ->
                    val cleanBase64 = if (base64.contains(",")) base64.substringAfter(",") else base64
                    val bitmap = remember(cleanBase64) {
                        try {
                            val decodedString = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                            android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            Button(
                                onClick = {
                                    val updated = albumPhotos.filter { it != base64 }
                                    albumPhotos = updated
                                    if (activeStore != null) {
                                        viewModel.saveStore(activeStore.copy(images = updated))
                                    }
                                    Toast.makeText(context, "🗑️ تم إزالة الصورة بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, Color.Red),
                                modifier = Modifier.fillMaxWidth().height(30.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("إزالة الصورة 🗑️", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ⭐ TAB 4: REVIEWS & FEEDBACK RESPONSES
// ==========================================
@Composable
fun TabReviewsFeedback(
    providerId: String,
    ratings: List<RatingEntity>,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val myRatings = remember(ratings, providerId) {
        ratings.filter { it.targetId == providerId }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "⭐ آراء العملاء والرد عليها مباشرة:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.accent
        )

        if (myRatings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد أي تقييمات مسجلة لملفك حتى الآن.", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(myRatings) { ratingItem ->
                    var showReplyBox by remember { mutableStateOf(false) }
                    var replyText by remember { mutableStateOf(ratingItem.reply) }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👤 العميل: ${ratingItem.userName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("⭐ ${ratingItem.rating.toInt()}", color = Color.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Text(ratingItem.comment, color = Color.LightGray, fontSize = 11.sp)

                            if (ratingItem.reply.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(themeColors.accent.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Text("💬 ردك: ${ratingItem.reply}", color = themeColors.accent, fontSize = 10.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showReplyBox = !showReplyBox }) {
                                    Text(if (ratingItem.reply.isEmpty()) "رد على التقييم 💬" else "تعديل الرد ✏️", color = themeColors.accent, fontSize = 10.sp)
                                }
                            }

                            if (showReplyBox) {
                                OutlinedTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    label = { Text("اكتب ردك اللبق للزبون") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        viewModel.addRatingReply(ratingItem.id, replyText.trim())
                                        showReplyBox = false
                                        Toast.makeText(context, "✅ تم إرسال ردك للعميل بنجاح!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Text("إرسال الرد 🚀", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 📅 TAB 5: BOOKINGS / ORDERS CONTROL
// ==========================================
@Composable
fun TabBookingsOrders(
    providerId: String,
    bookings: List<BookingEntity>,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val myReceivedBookings = remember(bookings, providerId) {
        bookings.filter { it.providerId == providerId }.sortedByDescending { it.id }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "📅 إدارة الحجوزات والمواعيد الواردة:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.accent
        )

        if (myReceivedBookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد أي حجوزات واردة لحسابك حالياً.", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(myReceivedBookings) { b ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(b.serviceType, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = when (b.status.uppercase()) {
                                        "PENDING" -> "⏳ قيد الانتظار"
                                        "APPROVED" -> "✅ مقبول"
                                        "STARTED" -> "⚡ قيد التنفيذ"
                                        "COMPLETED" -> "🎉 مكتمل"
                                        else -> "❌ ملغي"
                                    },
                                    color = when (b.status.uppercase()) {
                                        "PENDING" -> Color(0xFFF59E0B)
                                        "APPROVED" -> Color(0xFF10B981)
                                        "STARTED" -> Color(0xFF3B82F6)
                                        "COMPLETED" -> Color(0xFF10B981)
                                        else -> Color.Red
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.15f))

                            Text("👤 العميل: ${b.customerName} (هاتف: ${b.customerPhone})", color = Color.LightGray, fontSize = 11.sp)
                            Text("📅 الموعد: ${b.dateString} الساعة ${b.timeString}", color = Color.LightGray, fontSize = 11.sp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (b.status == "PENDING") {
                                    Button(
                                        onClick = { viewModel.updateBookingStatus(b.id, "APPROVED") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("قبول الحجز ✓", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { viewModel.updateBookingStatus(b.id, "REJECTED") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("رفض الحجز ❌", color = Color.White, fontSize = 10.sp)
                                    }
                                } else if (b.status == "APPROVED") {
                                    Button(
                                        onClick = { viewModel.updateBookingStatus(b.id, "STARTED") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                        modifier = Modifier.fillMaxWidth().height(30.dp)
                                    ) {
                                        Text("بدء تنفيذ الموعد والخدمة ⚡", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (b.status == "STARTED") {
                                    Button(
                                        onClick = { viewModel.updateBookingStatus(b.id, "COMPLETED") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        modifier = Modifier.fillMaxWidth().height(30.dp)
                                    ) {
                                        Text("تأكيد اكتمال وإنجاز الخدمة مكتملة ✅", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

// ==========================================
// 📊 TAB 6: STATISTICS & ESTIMATES
// ==========================================
@Composable
fun TabStatisticsGrowth(
    providerId: String,
    bookings: List<BookingEntity>,
    ratings: List<RatingEntity>,
    themeColors: VisualThemePalette
) {
    val myBookings = remember(bookings, providerId) {
        bookings.filter { it.providerId == providerId }
    }

    val totalCount = myBookings.size
    val pendingCount = myBookings.count { it.status == "PENDING" }
    val completedCount = myBookings.count { it.status == "COMPLETED" }
    val cancelledCount = myBookings.count { it.status == "CANCELLED" || it.status == "REJECTED" }

    // Rough estimated income
    val estimatedEarnings = completedCount * 12000

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "📈 تقارير الأداء والإحصائيات والنمو لعام 2026:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.accent
        )

        // Estimated Earnings Box
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F291E)),
            border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("💰 إجمالي الإيرادات المقدرة", color = Color.LightGray, fontSize = 11.sp)
                Text("$estimatedEarnings ريال يمني", color = Color(0xFF4ADE80), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("تحسب على أساس المواعيد المنجزة والسلع المباعة بالكامل بالمنصة.", color = Color.Gray, fontSize = 9.sp, textAlign = TextAlign.Center)
            }
        }

        // Summary Statistics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المكتملة ✅", color = Color.LightGray, fontSize = 10.sp)
                    Text(completedCount.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("قيد الانتظار ⏳", color = Color.LightGray, fontSize = 10.sp)
                    Text(pendingCount.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الملغية ❌", color = Color.LightGray, fontSize = 10.sp)
                    Text(cancelledCount.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Beautiful Graphic Canvas representation of bookings status
        Text("📊 مخطط توزيع حالة الحجز:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            val total = totalCount.toFloat()
            if (total > 0) {
                val completedWidth = (completedCount.toFloat() / total) * size.width
                val pendingWidth = (pendingCount.toFloat() / total) * size.width
                val cancelledWidth = (cancelledCount.toFloat() / total) * size.width

                // Draw horizontal progress stack
                drawRect(
                    color = Color(0xFF10B981),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.4f),
                    size = androidx.compose.ui.geometry.Size(completedWidth, size.height * 0.2f)
                )
                drawRect(
                    color = Color(0xFFF59E0B),
                    topLeft = androidx.compose.ui.geometry.Offset(completedWidth, size.height * 0.4f),
                    size = androidx.compose.ui.geometry.Size(pendingWidth, size.height * 0.2f)
                )
                drawRect(
                    color = Color(0xFFEF4444),
                    topLeft = androidx.compose.ui.geometry.Offset(completedWidth + pendingWidth, size.height * 0.4f),
                    size = androidx.compose.ui.geometry.Size(cancelledWidth, size.height * 0.2f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981)))
                Text("مكتمل", color = Color.LightGray, fontSize = 9.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFF59E0B)))
                Text("قيد الانتظار", color = Color.LightGray, fontSize = 9.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFEF4444)))
                Text("ملغي", color = Color.LightGray, fontSize = 9.sp)
            }
        }
    }
}

