package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CategoryEntity
import com.example.data.CityEntity
import com.example.data.OfferEntity
import com.example.data.UrgentRequestEntity

@Composable
fun CreateUrgentRequestScreen(
    viewModel: MainViewModel,
    categories: List<CategoryEntity>,
    cities: List<CityEntity>,
    onClose: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(categories.firstOrNull()) }
    var selectedCity by remember { mutableStateOf<CityEntity?>(cities.firstOrNull()) }
    var area by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "اطلب خدمتك الآن ⚡",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "أنشئ طلباً عاجلاً ليصل فوراً لجميع المختصين في مدينتك",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section / Category Selection
                item {
                    Text(
                        text = "1. اختر قسم الخدمة المطلوبة *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategory?.id == cat.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.name) },
                                leadingIcon = {
                                    if (cat.icon.isNotBlank()) Text(cat.icon)
                                }
                            )
                        }
                    }
                }

                // City & Area Selection
                item {
                    Text(
                        text = "2. المحافظة / المدينة والمنطقة *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cities) { city ->
                            val isSelected = selectedCity?.id == city.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCity = city },
                                label = { Text(city.nameAr) },
                                leadingIcon = { Text("📍") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = area,
                        onValueChange = { area = it },
                        label = { Text("المنطقة / الحي والشارع بالتفصيل") },
                        placeholder = { Text("مثال: شارع الستين - جوار جولة الأصبحي") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Request Title & Description
                item {
                    Text(
                        text = "3. تفاصيل المشكلة أو الخدمة *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان مختصر للطلب") },
                        placeholder = { Text("مثال: إصلاح تسريب مياه في المطبخ") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("الوصف التفصيلي للمشكلة") },
                        placeholder = { Text("اكتب تفاصيل الخدمة أو العطل المطلوبة لمساعدة المختصين في تقديم العرض المناسب...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 4
                    )
                }

                // Optional Photo URL / Attachment
                item {
                    Text(
                        text = "4. مرفقات توضيحية (اختياري)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("رابط صورة المشكلة إن وجد") },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Button(
                onClick = {
                    if (title.isBlank() || description.isBlank()) {
                        viewModel.setUiError("يرجى إدخال عنوان ووصف الخدمة المطلوبة")
                        return@Button
                    }
                    isSubmitting = true
                    viewModel.createUrgentRequest(
                        title = title,
                        description = description,
                        sectionId = selectedCategory?.id ?: "general",
                        categoryName = selectedCategory?.name ?: "خدمة عامة",
                        cityId = selectedCity?.id ?: "sanaa",
                        cityName = selectedCity?.nameAr ?: "صنعاء",
                        area = area.ifBlank { selectedCity?.nameAr ?: "صنعاء" },
                        imageUrl = imageUrl,
                        onSuccess = {
                            isSubmitting = false
                            onClose()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إرسال الطلب العاجل ⚡",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyUrgentRequestsScreen(
    viewModel: MainViewModel,
    urgentRequests: List<UrgentRequestEntity>,
    offers: List<OfferEntity>,
    currentUserId: String,
    onCreateNewClick: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    var selectedRequestForDetails by remember { mutableStateOf<UrgentRequestEntity?>(null) }
    val userRequests = remember(urgentRequests, currentUserId) {
        urgentRequests.filter { it.userId == currentUserId || currentUserId == "guest" || currentUserId.startsWith("admin") }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "طلباتي العاجلة 📋",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "متابعة طلباتك والعروض المقدمة من الفنيين والمحلات",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onCreateNewClick,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("طلب جديد ⚡", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (userRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لا توجد لديك طلبات عاجلة حالياً",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اضغط على 'طلب جديد ⚡' لإرسال طلبك إلى الفنيين فوراً",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onCreateNewClick) {
                        Text("اطلب خدمتك الآن ⚡")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(userRequests) { req ->
                    val reqOffers = offers.filter { it.requestId == req.id }
                    UrgentRequestCard(
                        request = req,
                        offersCount = reqOffers.size,
                        onClick = { selectedRequestForDetails = req }
                    )
                }
            }
        }
    }

    selectedRequestForDetails?.let { req ->
        val reqOffers = offers.filter { it.requestId == req.id }
        UrgentRequestDetailsDialog(
            request = req,
            offers = reqOffers,
            isProvider = false,
            onClose = { selectedRequestForDetails = null },
            onAcceptOffer = { offerId ->
                viewModel.acceptOffer(req.id, offerId) {
                    selectedRequestForDetails = null
                    onOpenChat("chat_" + req.id)
                }
            },
            onSubmitOffer = { _, _, _ -> }
        )
    }
}

@Composable
fun ProviderRequestsScreen(
    viewModel: MainViewModel,
    urgentRequests: List<UrgentRequestEntity>,
    offers: List<OfferEntity>,
    currentProviderId: String,
    onOpenChat: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, MY_OFFERS, CLOSED
    var selectedRequestForDetails by remember { mutableStateOf<UrgentRequestEntity?>(null) }

    val filteredRequests = remember(urgentRequests, offers, currentProviderId, selectedFilter) {
        when (selectedFilter) {
            "MY_OFFERS" -> {
                val myReqIds = offers.filter { it.providerId == currentProviderId }.map { it.requestId }.toSet()
                urgentRequests.filter { it.id in myReqIds }
            }
            "CLOSED" -> urgentRequests.filter { it.status == "COMPLETED" || it.status == "CANCELLED" }
            else -> urgentRequests.filter { it.status == "OPEN" || it.status == "HAS_OFFERS" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "طلبات الخدمات العاجلة ⚡",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "استعرض طلبات العملاء العاجلة ودم خدماتك وعروض أسعارك",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("الطلبات الجديدة ⚡") }
            )
            FilterChip(
                selected = selectedFilter == "MY_OFFERS",
                onClick = { selectedFilter = "MY_OFFERS" },
                label = { Text("عروضي المقدمة 🏷️") }
            )
            FilterChip(
                selected = selectedFilter == "CLOSED",
                onClick = { selectedFilter = "CLOSED" },
                label = { Text("المغلقة 📁") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد طلبات عاجلة في هذه القائمة حالياً",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRequests) { req ->
                    val reqOffers = offers.filter { it.requestId == req.id }
                    val myOffer = reqOffers.find { it.providerId == currentProviderId }

                    UrgentRequestCard(
                        request = req,
                        offersCount = reqOffers.size,
                        myOfferStatus = myOffer?.status,
                        onClick = { selectedRequestForDetails = req }
                    )
                }
            }
        }
    }

    selectedRequestForDetails?.let { req ->
        val reqOffers = offers.filter { it.requestId == req.id }
        UrgentRequestDetailsDialog(
            request = req,
            offers = reqOffers,
            isProvider = true,
            currentProviderId = currentProviderId,
            onClose = { selectedRequestForDetails = null },
            onAcceptOffer = { },
            onSubmitOffer = { price, eta, note ->
                viewModel.submitOffer(req.id, price, eta, note) {
                    selectedRequestForDetails = null
                }
            }
        )
    }
}

@Composable
fun UrgentRequestCard(
    request: UrgentRequestEntity,
    offersCount: Int,
    myOfferStatus: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = request.categoryName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                StatusBadge(status = request.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = request.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = request.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍 ", fontSize = 12.sp)
                    Text(
                        text = "${request.cityName} - ${request.area}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (offersCount > 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (myOfferStatus != null) "عرضك: $myOfferStatus" else "🏷️ العروض: $offersCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (text, color, textColor) = when (status) {
        "OPEN" -> Triple("مفتوح (بانتظار عروض)", Color(0xFF059669), Color.White)
        "HAS_OFFERS" -> Triple("وصلت عروض 🏷️", Color(0xFFD97706), Color.White)
        "ACCEPTED" -> Triple("تم قبول عرض 🎉", Color(0xFF2563EB), Color.White)
        "IN_PROGRESS" -> Triple("قيد التنفيذ 🛠️", Color(0xFF7C3AED), Color.White)
        "COMPLETED" -> Triple("مكتمل ✅", Color(0xFF059669), Color.White)
        "CANCELLED" -> Triple("ملغي ❌", Color(0xFFDC2626), Color.White)
        else -> Triple(status, Color.Gray, Color.White)
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun UrgentRequestDetailsDialog(
    request: UrgentRequestEntity,
    offers: List<OfferEntity>,
    isProvider: Boolean,
    currentProviderId: String = "",
    onClose: () -> Unit,
    onAcceptOffer: (String) -> Unit,
    onSubmitOffer: (Double, Int, String) -> Unit
) {
    var offerPrice by remember { mutableStateOf("") }
    var offerEta by remember { mutableStateOf("30") }
    var offerNote by remember { mutableStateOf("") }
    var showOfferForm by remember { mutableStateOf(false) }

    val myOffer = offers.find { it.providerId == currentProviderId }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تفاصيل الطلب العاجل ⚡",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Request Information
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = request.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = request.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "📍 المدينة والحي: ${request.cityName} - ${request.area}", fontSize = 12.sp)
                        Text(text = "👤 صاحب الطلب: ${request.userName} (${request.userPhone})", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Offers section title
                Text(
                    text = "العروض المقدمة (${offers.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (offers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "لم تصل أي عروض حتى الآن...", fontSize = 13.sp, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(offers) { offer ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (offer.status == "ACCEPTED") MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = offer.providerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(text = "⭐ ${offer.providerRating} | ⏱️ الوصول خلال: ${offer.etaMinutes} دقيقة", fontSize = 11.sp, color = Color.Gray)
                                        }

                                        Text(
                                            text = "${offer.price.toInt()} ر.ي",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 16.sp
                                        )
                                    }

                                    if (offer.note.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "ملاحظة: ${offer.note}", fontSize = 12.sp)
                                    }

                                    if (!isProvider && request.status != "ACCEPTED" && request.status != "COMPLETED") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { onAcceptOffer(offer.id) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text("قبول العرض وبدء المحادثة 💬", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Provider Offer Input Form
                if (isProvider && (request.status == "OPEN" || request.status == "HAS_OFFERS")) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (!showOfferForm && myOffer == null) {
                        Button(
                            onClick = { showOfferForm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تقديم عرض سعر جديد 🏷️")
                        }
                    } else if (showOfferForm && myOffer == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = offerPrice,
                                onValueChange = { offerPrice = it },
                                label = { Text("السعر المقترح (بالريال اليمني)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = offerEta,
                                onValueChange = { offerEta = it },
                                label = { Text("وقت الوصول المتوقع (بالدقائق)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = offerNote,
                                onValueChange = { offerNote = it },
                                label = { Text("ملاحظة قصيرة للعميل (اختياري)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    val price = offerPrice.toDoubleOrNull() ?: 0.0
                                    val eta = offerEta.toIntOrNull() ?: 30
                                    if (price > 0) {
                                        onSubmitOffer(price, eta, offerNote)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تأكيد وإرسال العرض 🚀")
                            }
                        }
                    } else if (myOffer != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✅ لقد قدمت عرضاً بالفعل بسعر ${myOffer.price.toInt()} ر.ي",
                                modifier = Modifier.padding(10.dp),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAssistantSettingsPanel(
    settings: com.example.data.AdminSettingsEntity,
    viewModel: MainViewModel
) {
    var assistantEnabled by remember(settings.isAssistantEnabled) { mutableStateOf(settings.isAssistantEnabled) }
    var assistantVisible by remember(settings.isAssistantIconVisible) { mutableStateOf(settings.isAssistantIconVisible) }
    var assistantX by remember(settings.assistantPositionX) { mutableStateOf(settings.assistantPositionX) }
    var assistantY by remember(settings.assistantPositionY) { mutableStateOf(settings.assistantPositionY) }
    var assistantSize by remember(settings.assistantSize) { mutableStateOf(settings.assistantSize.toFloat()) }
    var assistantShape by remember(settings.assistantIconShape) { mutableStateOf(settings.assistantIconShape) }
    var assistantStyle by remember(settings.assistantIconStyle) { mutableStateOf(settings.assistantIconStyle) }

    var urgentEnabled by remember(settings.isUrgentRequestEnabled) { mutableStateOf(settings.isUrgentRequestEnabled) }
    var urgentVisible by remember(settings.isUrgentRequestIconVisible) { mutableStateOf(settings.isUrgentRequestIconVisible) }
    var urgentX by remember(settings.urgentRequestPositionX) { mutableStateOf(settings.urgentRequestPositionX) }
    var urgentY by remember(settings.urgentRequestPositionY) { mutableStateOf(settings.urgentRequestPositionY) }
    var urgentSize by remember(settings.urgentRequestSize) { mutableStateOf(settings.urgentRequestSize.toFloat()) }
    var urgentShape by remember(settings.urgentRequestIconShape) { mutableStateOf(settings.urgentRequestIconShape) }
    var urgentStyle by remember(settings.urgentRequestIconStyle) { mutableStateOf(settings.urgentRequestIconStyle) }
    var urgentSections by remember(settings.urgentAllowedSections) { mutableStateOf(settings.urgentAllowedSections) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: AIAssistant Admin Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🤖 إعدادات وأيقونة المساعد الذكي (أونلاين + أوفلاين)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل نظام المساعد الذكي بالكامل:", fontSize = 13.sp)
                    Switch(checked = assistantEnabled, onCheckedChange = { assistantEnabled = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إظهار الأيقونة العائمة على الشاشة:", fontSize = 13.sp)
                    Switch(checked = assistantVisible, onCheckedChange = { assistantVisible = it })
                }

                Text("شكل الأيقونة العائمة:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CIRCLE" to "دائري ⭕", "ROUNDED" to "منحني 🔲", "SQUARE" to "مربع ⏹️", "PILL" to "كبسولة 💊").forEach { (sh, label) ->
                        FilterChip(
                            selected = assistantShape == sh,
                            onClick = { assistantShape = sh },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Text("ثيم ومظهر الأيقونة:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("DEFAULT" to "عادي 🟢", "GOLDEN_3D" to "ذهبي 👑", "NEON" to "نيون ⚡", "MINIMAL" to "بسيط 🌙").forEach { (st, label) ->
                        FilterChip(
                            selected = assistantStyle == st,
                            onClick = { assistantStyle = st },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Text("حجم الأيقونة: ${assistantSize.toInt()} dp", fontSize = 12.sp)
                Slider(
                    value = assistantSize,
                    onValueChange = { assistantSize = it },
                    valueRange = 40f..100f,
                    steps = 12
                )

                Button(
                    onClick = {
                        viewModel.updateAssistantAdminSettings(
                            isEnabled = assistantEnabled,
                            isVisible = assistantVisible,
                            posX = assistantX,
                            posY = assistantY,
                            size = assistantSize.toInt(),
                            shape = assistantShape,
                            style = assistantStyle
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حفظ إعدادات المساعد الذكي 💾")
                }
            }
        }

        // Section 2: Urgent Requests Admin Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "⚡ إعدادات وأيقونة 'اطلب خدمتك الآن' (طلباتي)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل نظام الطلبات العاجلة والعروض:", fontSize = 13.sp)
                    Switch(checked = urgentEnabled, onCheckedChange = { urgentEnabled = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إظهار زر 'اطلب خدمتك الآن ⚡':", fontSize = 13.sp)
                    Switch(checked = urgentVisible, onCheckedChange = { urgentVisible = it })
                }

                Text("شكل الأيقونة العائمة:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CIRCLE" to "دائري ⭕", "ROUNDED" to "منحني 🔲", "SQUARE" to "مربع ⏹️", "PILL" to "كبسولة 💊").forEach { (sh, label) ->
                        FilterChip(
                            selected = urgentShape == sh,
                            onClick = { urgentShape = sh },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Text("ثيم ومظهر الأيقونة:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("DEFAULT" to "عادي 🟢", "GOLDEN_3D" to "ذهبي 👑", "NEON" to "نيون ⚡", "MINIMAL" to "بسيط 🌙").forEach { (st, label) ->
                        FilterChip(
                            selected = urgentStyle == st,
                            onClick = { urgentStyle = st },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Text("حجم الأيقونة: ${urgentSize.toInt()} dp", fontSize = 12.sp)
                Slider(
                    value = urgentSize,
                    onValueChange = { urgentSize = it },
                    valueRange = 40f..100f,
                    steps = 12
                )

                OutlinedTextField(
                    value = urgentSections,
                    onValueChange = { urgentSections = it },
                    label = { Text("الأقسام المسموح فيها بالطلبات العاجلة (ALL لجزء أو مفصولة بفواصل)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        viewModel.updateUrgentRequestAdminSettings(
                            isEnabled = urgentEnabled,
                            isVisible = urgentVisible,
                            posX = urgentX,
                            posY = urgentY,
                            size = urgentSize.toInt(),
                            shape = urgentShape,
                            style = urgentStyle,
                            allowedSections = urgentSections
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حفظ إعدادات 'اطلب خدمتك الآن' 💾")
                }
            }
        }
    }
}
