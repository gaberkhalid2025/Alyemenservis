package com.example.ui.screens.entities

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.example.ui.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.data.repositories.DashboardExtensionsRepositoryImpl
import com.example.data.repositories.GalleryRepositoryImpl
import com.example.data.repositories.MedicalRepository
import com.example.data.repositories.ProductsRepositoryImpl
import com.example.domain.entities.DoctorItem
import com.example.domain.entities.GalleryAlbumEntity
import com.example.domain.entities.ProductItemEntity
import com.example.ui.components.SmartAsyncImage
import com.example.ui.helpers.AppPreferenceHelper
import com.example.utils.VisualThemePalette
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun EmptyStateBox(msg: String, themeColors: VisualThemePalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .background(themeColors.surface.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = msg, color = themeColors.textSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun ProfileTabContent(
    selectedTab: Int,
    entityType: ProfileEntityType,
    provider: ProviderEntity?,
    store: StoreEntity?,
    property: PropertyEntity?,
    job: JobEntity?,
    products: List<ProductEntity>,
    entityReviews: List<RatingEntity>,
    entityDescription: String,
    themeColors: VisualThemePalette,
    isOwner: Boolean = false,
    onAddReviewClick: () -> Unit,
    onRequestBooking: () -> Unit = {},
    onOpenMapClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val entityId = provider?.id ?: store?.id ?: property?.id ?: job?.id ?: ""
    val entityPhone = provider?.phone ?: store?.phone ?: property?.phone ?: job?.phone ?: ""
    val workingHours = store?.workingHours?.ifBlank { "9:00 ص – 10:00 م" }
        ?: "8:00 ص – 10:00 م"

    val galleryAlbums by remember(entityId, context) {
        GalleryRepositoryImpl(context.applicationContext).getOwnerGallery(entityId)
    }.collectAsState(initial = emptyList<GalleryAlbumEntity>())

    val ownerProducts by remember(entityId, context) {
        ProductsRepositoryImpl(context.applicationContext).getOwnerProducts(entityId)
    }.collectAsState(initial = emptyList<ProductItemEntity>())

    val specialOffers by remember(entityId) {
        DashboardExtensionsRepositoryImpl().getSpecialOffers(entityId)
    }.collectAsState(initial = emptyList<SpecialOfferEntity>())

    val doctors by remember(entityId, context) {
        MedicalRepository(
            FirebaseFirestore.getInstance(),
            LocalAppCacheManager(context.applicationContext)
        ).getDoctors(entityId)
    }.collectAsState(initial = emptyList<DoctorItem>())

    val mergedProducts = remember(products, ownerProducts, entityId) {
        val fromGlobal = products.filter { it.storeId == entityId }
        val existingIds = fromGlobal.map { it.id }.toSet()
        val extra = ownerProducts.filter { it.id !in existingIds }.map { p ->
            ProductEntity(
                id = p.id,
                storeId = p.ownerId,
                name = p.title,
                price = p.priceYer,
                oldPrice = p.oldPriceYer,
                description = p.description,
                imageUrl = p.imageUrl,
                category = p.category,
                isAvailable = p.isAvailable,
                isOffer = p.isOffer || p.discountPercent > 0,
                discountPercent = p.discountPercent
            )
        }
        fromGlobal + extra
    }

    val mergedGalleryUrls = remember(provider, property, galleryAlbums) {
        val base = provider?.workPhotosBase64.orEmpty() + property?.images.orEmpty()
        val albumUrls = galleryAlbums.flatMap { album -> album.imageUrls }
        (base + albumUrls).filter { it.isNotBlank() }.distinct()
    }

    @Composable
    fun ReviewsSection() {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("التقييمات وآراء العملاء ⭐", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                if (!isOwner) {
                    Button(
                        onClick = onAddReviewClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("أضف تقييمك ⭐", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (entityReviews.isEmpty()) {
                EmptyStateBox("لا توجد تقييمات مسجلة بعد. كن أول من يكتب تقييماً!", themeColors)
            } else {
                entityReviews.forEach { review ->
                    ProfileReviewCard(review = review, themeColors = themeColors)
                }
            }
        }
    }

    @Composable
    fun InfoCard(title: String, lines: List<String>) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, themeColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 13.5.sp)
                lines.filter { it.isNotBlank() }.forEach { line ->
                    Text(text = line, color = themeColors.textPrimary, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }

    when (entityType) {
        ProfileEntityType.TECHNICIAN -> {
            // Tabs: 0 الخدمات, 1 التقييمات والآراء, 2 المعرض, 3 عن الفني, 4 أوقات العمل
            when (selectedTab) {
                0 -> {
                    val customServices = mergedProducts
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val servicesList = provider?.profession?.split("،", ",", "-")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                        InfoCard(
                            title = "🛠️ الخدمات التخصصية المعتمدة",
                            lines = if (servicesList.isNotEmpty()) {
                                servicesList.map { "• $it" }
                            } else {
                                listOf(
                                    "• ${provider?.specialization?.ifBlank { "خدمات صيانة وتركيبات متكاملة" } ?: "خدمات صيانة وتركيبات متكاملة"}",
                                    "• فحص وتشخيص الأعطال بدقة عالية",
                                    "• ضمان جودة التنفيذ والالتزام بالمواعيد"
                                )
                            }
                        )
                        if (customServices.isNotEmpty()) {
                            customServices.forEach { prod ->
                                ProfileProductCard(product = prod, themeColors = themeColors)
                            }
                        }
                    }
                }
                1 -> ReviewsSection()
                2 -> {
                    if (mergedGalleryUrls.isEmpty()) {
                        EmptyStateBox("لا توجد صور سابقة أعمال مرفوعة حالياً في المعرض.", themeColors)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(mergedGalleryUrls) { photo ->
                                Card(modifier = Modifier.size(135.dp), shape = RoundedCornerShape(12.dp)) {
                                    SmartAsyncImage(model = photo, contentDescription = "عمل سابق", modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
                3 -> {
                    InfoCard(
                        title = "👤 النبذة المهنية والخبرات",
                        lines = listOf(
                            provider?.specialization?.ifBlank { entityDescription } ?: entityDescription,
                            "📍 مناطق التغطية: ${listOfNotNull(provider?.area?.takeIf { it.isNotBlank() }, provider?.localNeighborhood?.takeIf { it.isNotBlank() }).joinToString(" - ").ifBlank { "اليمن" }}"
                        )
                    )
                }
                4 -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, themeColors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⏰ أوقات العمل والجاهزية الميدانية", fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 13.5.sp)
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (provider?.isAvailable == true) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (provider?.isAvailable == true) "🟢 متاح الآن لاستقبال الطلبات" else "🔴 مشغول حالياً",
                                    color = if (provider?.isAvailable == true) Color(0xFF10B981) else Color(0xFFEF4444),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text("• ساعات الدوام المسجلة: $workingHours", color = themeColors.textPrimary, fontSize = 12.sp)
                        }
                    }
                }
                else -> EmptyStateBox("لا توجد تفاصيل إضافية لهذا التبويب.", themeColors)
            }
        }

        ProfileEntityType.STORE, ProfileEntityType.RESTAURANT -> {
            // STORE: 0 الكتالوج والمنتجات, 1 العروض والخصومات, 2 التقييمات, 3 سياسة التوصيل, 4 طرق الدفع, 5 عن المتجر
            // RESTAURANT: 0 القائمة والأصناف, 1 العروض والخصومات, 2 التقييمات, 3 التوصيل والمناطق, 4 أوقات العمل, 5 عن المطعم
            when (selectedTab) {
                0 -> {
                    if (mergedProducts.isEmpty()) {
                        EmptyStateBox("لا توجد أصناف مدرجة في القائمة حالياً.", themeColors)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mergedProducts.forEach { prod ->
                                ProfileProductCard(product = prod, themeColors = themeColors)
                            }
                        }
                    }
                }
                1 -> {
                    val activeOffers = specialOffers.filter { it.isEnabled }
                    val productOffers = mergedProducts.filter { it.isOffer || it.discountPercent > 0 }
                    if (activeOffers.isEmpty() && productOffers.isEmpty()) {
                        EmptyStateBox("لا توجد عروض أو خصومات نشطة حالياً.", themeColors)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            activeOffers.forEach { offer ->
                                InfoCard(
                                    title = "🏷️ ${offer.title.ifBlank { "عرض خاص" }}",
                                    lines = listOfNotNull(
                                        offer.description.takeIf { it.isNotBlank() },
                                        offer.discountPercent.takeIf { it > 0 }?.let { "نسبة الخصم: $it%" },
                                        offer.couponCode.takeIf { it.isNotBlank() }?.let { "كود الكوبون: $it" }
                                    )
                                )
                            }
                            productOffers.forEach { prod ->
                                ProfileProductCard(product = prod, themeColors = themeColors)
                            }
                        }
                    }
                }
                2 -> ReviewsSection()
                3 -> {
                    InfoCard(
                        title = if (entityType == ProfileEntityType.RESTAURANT) "🛵 التوصيل ومناطق التغطية" else "🚚 سياسة التوصيل والضمان",
                        lines = listOf(
                            "• المنطقة الرئيسية: ${store?.cityId?.ifBlank { "صنعاء" } ?: "صنعاء"} - ${store?.localNeighborhood?.ifBlank { "جميع الأحياء" } ?: "جميع الأحياء"}",
                            "• توصيل سريع ومنسق مع العميل فور تأكيد الطلب",
                            "• إمكانية الاستلام المباشر أو التوصيل للعنوان"
                        )
                    )
                }
                4 -> {
                    if (entityType == ProfileEntityType.RESTAURANT) {
                        InfoCard(
                            title = "⏰ أوقات العمل",
                            lines = listOf(
                                "• ساعات العمل: $workingHours",
                                if (store?.isActive != false) "🟢 مفتوح ويستقبل الطلبات الآن" else "🔴 مغلق مؤقتاً"
                            )
                        )
                    } else {
                        InfoCard(
                            title = "💳 طرق الدفع المعتمدة",
                            lines = listOf(
                                "• الدفع نقداً عند الاستلام",
                                "• المحافظ الإلكترونية المعتمدة والتحويل البنكي حسب الاتفاق مع المتجر"
                            )
                        )
                    }
                }
                5 -> {
                    InfoCard(
                        title = if (entityType == ProfileEntityType.RESTAURANT) "🏪 عن المطعم" else "🏬 عن المتجر",
                        lines = listOf(
                            entityDescription,
                            "📍 العنوان: ${store?.cityId.orEmpty()} - ${store?.localNeighborhood.orEmpty()}",
                            "⏰ ساعات الدوام: $workingHours"
                        )
                    )
                }
                else -> EmptyStateBox("لا توجد تفاصيل إضافية لهذا التبويب.", themeColors)
            }
        }

        ProfileEntityType.MEDICAL -> {
            // Tabs: 0 العيادات والتخصصات, 1 الأطباء, 2 حجز موعد, 3 المختبر والأشعة, 4 الطوارئ وأوقات العمل, 5 التقييمات والآراء, 6 عن المركز
            when (selectedTab) {
                0 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoCard(
                            title = "🩺 العيادات والتخصصات المتوفرة",
                            lines = listOf(entityDescription)
                        )
                        if (mergedProducts.isEmpty()) {
                            EmptyStateBox("لم تتم إضافة قائمة فحوصات أو خدمات طبية بعد.", themeColors)
                        } else {
                            mergedProducts.forEach { prod ->
                                ProfileProductCard(product = prod, themeColors = themeColors)
                            }
                        }
                    }
                }
                1 -> {
                    if (doctors.isEmpty()) {
                        EmptyStateBox("لم يتم تسجيل أطباء في هذا المركز بعد.", themeColors)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            doctors.forEach { doc ->
                                InfoCard(
                                    title = "👨‍⚕️ د. ${doc.name}",
                                    lines = listOfNotNull(
                                        doc.specialty.takeIf { it.isNotBlank() }?.let { "التخصص: $it" },
                                        doc.workingHours.takeIf { it.isNotBlank() }?.let { "أوقات الدوام: $it" }
                                    )
                                )
                            }
                        }
                    }
                }
                2 -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📅 حجز موعد طبي مباشر", fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 14.sp)
                            Text("يمكنك تحديد الموعد والعيادة المناسبة مباشرة والتواصل الفوري مع المركز.", color = themeColors.textSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Button(
                                onClick = onRequestBooking,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تأكيد حجز موعد الآن 🩺", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                3 -> {
                    val labServices = mergedProducts.filter {
                        it.name.contains("مختبر") || it.name.contains("أشعة") || it.name.contains("فحص") || it.category.contains("lab", ignoreCase = true)
                    }
                    if (labServices.isEmpty()) {
                        InfoCard(
                            title = "🔬 المختبر والأشعة والتشخيص",
                            lines = listOf("تتوفر خدمات المختبر والفحوصات الطبية — تواصل مع المركز للاستفسار عن التفاصيل والأسعار.")
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            labServices.forEach { prod ->
                                ProfileProductCard(product = prod, themeColors = themeColors)
                            }
                        }
                    }
                }
                4 -> {
                    InfoCard(
                        title = "🚨 الطوارئ وأوقات العمل",
                        lines = listOf(
                            "• ساعات الدوام: $workingHours",
                            if (store?.isActive != false && provider?.isAvailable != false) "🟢 المركز متاح لاستقبال الحالات والمراجعين" else "⚪ خارج ساعات العمل حالياً"
                        )
                    )
                }
                5 -> ReviewsSection()
                6 -> {
                    InfoCard(
                        title = "🏥 عن المركز الطبي",
                        lines = listOf(
                            entityDescription,
                            "📍 الموقع: ${store?.cityId ?: provider?.area ?: "اليمن"} - ${store?.localNeighborhood ?: provider?.localNeighborhood ?: ""}",
                            if (!store?.medicalLicenseNo.isNullOrBlank()) "📜 رقم الترخيص الطبي: ${store?.medicalLicenseNo}" else ""
                        )
                    )
                }
                else -> EmptyStateBox("لا توجد تفاصيل إضافية لهذا التبويب.", themeColors)
            }
        }

        ProfileEntityType.REAL_ESTATE -> {
            // Tabs: 0 التفاصيل والمواصفات, 1 الصور والمعرض, 2 الموقع على الخريطة, 3 المرافق والخدمات, 4 التواصل مع المعلن
            when (selectedTab) {
                0 -> {
                    InfoCard(
                        title = "📐 تفاصيل ومواصفات العقار",
                        lines = listOfNotNull(
                            entityDescription,
                            property?.let { p ->
                                if (p.price > 0) "💰 السعر: ${p.price.toInt()} ${p.currency}" else null
                            },
                            property?.type?.takeIf { it.isNotBlank() }?.let { "📄 نوع العرض: ${if (it == "rent") "إيجار" else "بيع / تمليك"}" },
                            property?.propertyType?.takeIf { it.isNotBlank() }?.let { typeCode ->
                                val typeArabic = when (typeCode.lowercase()) {
                                    "apartment" -> "شقة"
                                    "house", "villa" -> "بيت / فيلا"
                                    "land" -> "أرض"
                                    "shop", "commercial" -> "محل / تجاري"
                                    else -> typeCode
                                }
                                "🏠 نوع العقار: $typeArabic"
                            },
                            property?.localNeighborhood?.takeIf { it.isNotBlank() }?.let { "📍 الحي / المنطقة: $it" },
                            property?.cityId?.takeIf { it.isNotBlank() }?.let { "🏙️ المدينة: $it" }
                        )
                    )
                }
                1 -> {
                    if (mergedGalleryUrls.isEmpty()) {
                        EmptyStateBox("لا توجد صور مضافة لهذا العقار حالياً.", themeColors)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(mergedGalleryUrls) { img ->
                                Card(modifier = Modifier.size(145.dp), shape = RoundedCornerShape(12.dp)) {
                                    SmartAsyncImage(model = img, contentDescription = "صورة العقار", modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
                2 -> {
                    InfoCard(
                        title = "🗺️ الموقع على الخريطة",
                        lines = listOf(
                            "• المدينة: ${property?.cityId?.ifBlank { "اليمن" } ?: "اليمن"}",
                            "• الحي / المنطقة: ${property?.localNeighborhood?.ifBlank { "غير محدد" } ?: "غير محدد"}",
                            if (property != null && property.latitude != 0.0 && property.longitude != 0.0) {
                                "📍 الإحداثيات: ${property.latitude}, ${property.longitude}"
                            } else {
                                "📍 يرجى التواصل مع المعلن لتحديد الموقع الدقيق للمعاينة"
                            }
                        )
                    )
                }
                3 -> {
                    if (mergedProducts.isEmpty()) {
                        InfoCard(
                            title = "🏢 العقارات والوحدات الإضافية",
                            lines = listOf("تواصل مع المكتب العقاري لطلب قائمة العقارات والوحدات المتاحة والمعاينة الميدانية.")
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mergedProducts.forEach { prod ->
                                ProfileProductCard(product = prod, themeColors = themeColors)
                            }
                        }
                    }
                }
                4 -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("📞 التواصل وطلب المعاينة", fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 14.sp)
                            if (entityPhone.isNotBlank()) {
                                Text("رقم المعلن: $entityPhone", color = themeColors.textPrimary, fontSize = 12.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = onRequestBooking,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("طلب معاينة 👁️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                if (entityPhone.isNotBlank()) {
                                    Button(
                                        onClick = {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$entityPhone")))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("اتصال 📞", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> EmptyStateBox("لا توجد تفاصيل إضافية لهذا التبويب.", themeColors)
            }
        }

        ProfileEntityType.JOB -> {
            // Tabs: 0 وصف الوظيفة, 1 المتطلبات والمؤهلات, 2 المزايا والراتب, 3 معلومات الشركة, 4 طريقة التقديم
            when (selectedTab) {
                0 -> InfoCard("📋 وصف الوظيفة", listOf(job?.description?.ifBlank { entityDescription } ?: entityDescription))
                1 -> InfoCard("🎓 المتطلبات والمؤهلات", listOf(job?.requirements?.ifBlank { "الالتزام والخبرة المناسبة في مجال العمل." } ?: "الالتزام والخبرة المناسبة في مجال العمل."))
                2 -> InfoCard("💼 المزايا والراتب", listOf("💰 الراتب / الحوافز: ${job?.salary?.ifBlank { "يُحدد بعد المقابلة" } ?: "يُحدد بعد المقابلة"}"))
                3 -> InfoCard("🏢 معلومات الشركة", listOf("• المنشأة: ${job?.companyName?.ifBlank { "شركة معلنة" } ?: "شركة معلنة"}", "• الموقع: ${job?.address?.ifBlank { job.cityId } ?: "اليمن"}"))
                4 -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, themeColors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⚡ طريقة التقديم", fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 14.sp)
                            Text("أرسل سيرتك الذاتية أو تواصل مباشرة مع صاحب العمل.", color = themeColors.textSecondary, fontSize = 12.sp)
                            val cleanPhone = AppPreferenceHelper.normalizePhoneNumber(entityPhone)
                            if (cleanPhone.isNotBlank()) {
                                Button(
                                    onClick = {
                                        val waNum = if (cleanPhone.startsWith("967")) cleanPhone else "967$cleanPhone"
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNum")))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("تقديم عبر واتساب 📄", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                else -> EmptyStateBox("لا توجد تفاصيل إضافية لهذا التبويب.", themeColors)
            }
        }

        ProfileEntityType.GENERAL -> {
            // Tabs: 0 الخدمات, 1 التقييمات, 2 عن المنشأة, 3 أوقات العمل
            when (selectedTab) {
                0 -> {
                    if (mergedProducts.isEmpty()) {
                        InfoCard("🛠️ الخدمات", listOf(entityDescription))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mergedProducts.forEach { ProfileProductCard(product = it, themeColors = themeColors) }
                        }
                    }
                }
                1 -> ReviewsSection()
                2 -> InfoCard("ℹ️ عن المنشأة", listOf(entityDescription))
                3 -> InfoCard("⏰ أوقات العمل", listOf("ساعات الدوام: $workingHours"))
                else -> EmptyStateBox("لا توجد تفاصيل إضافية لهذا التبويب.", themeColors)
            }
        }
    }
}

