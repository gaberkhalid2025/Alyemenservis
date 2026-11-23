package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import com.example.ui.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PendingProviderEntity
import com.example.ui.MainViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.utils.SecurityCryptoUtils
import com.example.utils.VisualThemePalette

private fun decryptRequestAttachmentForPreview(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""
    if (trimmed.startsWith("http://") ||
        trimmed.startsWith("https://") ||
        trimmed.startsWith("data:image") ||
        trimmed.startsWith("content://") ||
        trimmed.startsWith("file://")
    ) {
        return trimmed
    }
    return if (trimmed.startsWith("gcm:") || trimmed.startsWith("gcmx:") || trimmed.startsWith("enc::")) {
        SecurityCryptoUtils.decryptCrossDevice(trimmed).ifBlank {
            SecurityCryptoUtils.decrypt(trimmed)
        }
    } else {
        trimmed
    }
}

private fun PendingProviderEntity.buildAttachmentPreviewItems(): List<Pair<String, String>> {
    val items = mutableListOf<Pair<String, String>>()
    decryptRequestAttachmentForPreview(selfiePhotoBase64).takeIf { it.isNotBlank() }?.let {
        items.add("الصورة الشخصية / الشعار" to it)
    }
    decryptRequestAttachmentForPreview(idPhotoBase64).takeIf { it.isNotBlank() }?.let {
        items.add("بطاقة الهوية / الترخيص" to it)
    }
    workPhotosBase64.forEachIndexed { index, raw ->
        decryptRequestAttachmentForPreview(raw).takeIf { it.isNotBlank() }?.let {
            items.add("صورة مرفقة ${index + 1}" to it)
        }
    }
    return items
}

/**
 * 📨 AdminRequestsPanel
 * إدارة ومراجعة كافة طلبات الانضمام والتوثيق والاعتماد المعلقة لجميع القطاعات
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRequestsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pendingProviders by viewModel.pendingProviders.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val jobs by viewModel.jobs.collectAsState()
    val registeredUsersList by viewModel.registeredUsersList.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPendingTechnicians()
    }

    var selectedTab by remember { mutableStateOf("SERVICES") }
    var rejectingRequest by remember { mutableStateOf<PendingProviderEntity?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    var pendingDeletionTarget by remember { mutableStateOf<Triple<String, String, String>?>(null) } // type, id, title
    var previewingAttachmentsFor by remember { mutableStateOf<Pair<String, List<Pair<String, String>>>?>(null) }

    val pendingPropertiesList = properties.filter { !it.isApproved && !it.isDeleted }
    val pendingPropertiesFromProviders = pendingProviders.filter { 
        (it.status == "PENDING" || it.status.isEmpty()) && 
        (it.categoryId == "PROPERTY" || it.profession == "PROPERTY_OWNER" || it.categoryId.contains("عقار"))
    }
    val pendingPropertiesCount = pendingPropertiesList.size + pendingPropertiesFromProviders.size

    val pendingMedicalStores = stores.filter { !it.isApproved && !it.isDeleted && (it.sectionId == "medical" || it.categoryId.contains("طبي") || it.categoryId.contains("عياد") || it.categoryId.equals("MEDICAL", ignoreCase = true)) }
    val pendingMedicalProviders = pendingProviders.filter { (it.status == "PENDING" || it.status.isEmpty()) && (it.categoryId == "MEDICAL" || it.categoryId.equals("medical", ignoreCase = true) || it.categoryId.contains("طبي") || it.categoryId.contains("عياد") || it.categoryId.contains("مستشفى")) }
    val pendingMedicalCount = pendingMedicalStores.size + pendingMedicalProviders.size

    val pendingRestaurantsStores = stores.filter { !it.isApproved && !it.isDeleted && (it.sectionId == "restaurants" || it.categoryId.contains("مطعم") || it.categoryId.contains("كافيه") || it.categoryId.equals("RESTAURANT", ignoreCase = true)) }
    val pendingRestaurantsProviders = pendingProviders.filter { (it.status == "PENDING" || it.status.isEmpty()) && (it.categoryId == "RESTAURANT" || it.categoryId.equals("restaurant", ignoreCase = true) || it.categoryId.contains("مطعم") || it.categoryId.contains("كافيه")) }
    val pendingRestaurantsCount = pendingRestaurantsStores.size + pendingRestaurantsProviders.size

    val pendingRegularStores = stores.filter { !it.isApproved && !it.isDeleted && it.sectionId != "medical" && it.sectionId != "restaurants" && !it.categoryId.contains("طبي") && !it.categoryId.contains("عياد") && !it.categoryId.contains("مطعم") && !it.categoryId.contains("كافيه") && !it.categoryId.equals("MEDICAL", ignoreCase = true) && !it.categoryId.equals("RESTAURANT", ignoreCase = true) }
    val pendingStoresProviders = pendingProviders.filter { (it.status == "PENDING" || it.status.isEmpty()) && (it.categoryId == "STORE" || (it.profession == "STORE_OWNER" && it.categoryId != "MEDICAL" && it.categoryId != "RESTAURANT") || it.categoryId.contains("متجر") || it.categoryId.contains("محل")) }
    val pendingStoresCount = pendingRegularStores.size + pendingStoresProviders.size

    val pendingJobsList = jobs.filter { !it.isApproved && !it.isDeleted }
    val pendingJobsProviders = pendingProviders.filter { (it.status == "PENDING" || it.status.isEmpty()) && (it.categoryId.equals("JOB", ignoreCase = true) || it.categoryId.equals("JOB_SEEKER", ignoreCase = true) || it.profession == "JOB_POSTER" || it.profession == "JOB_SEEKER" || it.categoryId.contains("وظيفة") || it.categoryId.contains("توظيف") || it.categoryId.contains("باحث")) }
    val pendingJobsCount = pendingJobsList.size + pendingJobsProviders.size

    val pendingClientsProviders = pendingProviders.filter { (it.status == "PENDING" || it.status.isEmpty()) && (it.categoryId.equals("CLIENT", ignoreCase = true) || it.profession.equals("CLIENT", ignoreCase = true) || it.providerType.equals("CLIENT", ignoreCase = true) || it.categoryId.contains("عميل")) }

    val activeServicesPending = pendingProviders.filter {
        (it.status == "PENDING" || it.status.isEmpty()) &&
        it.categoryId != "STORE" && it.categoryId != "RESTAURANT" &&
        it.categoryId != "MEDICAL" && it.categoryId != "PROPERTY" &&
        it.categoryId != "JOB" && it.categoryId != "CLIENT" &&
        it.profession != "STORE_OWNER" && it.profession != "PROPERTY_OWNER" &&
        it.profession != "JOB_POSTER" && it.profession != "CLIENT" &&
        !it.categoryId.contains("متجر") && !it.categoryId.contains("محل") &&
        !it.categoryId.contains("مطعم") && !it.categoryId.contains("كافيه") &&
        !it.categoryId.contains("طبي") && !it.categoryId.contains("عياد") &&
        !it.categoryId.contains("عقار") && !it.categoryId.contains("وظيفة") &&
        !it.categoryId.contains("عميل")
    }
    val activeServicesCount = activeServicesPending.size

    val subTabs = listOf(
        Triple("SERVICES", "🔧 المهن والخدمات", activeServicesCount),
        Triple("PROPERTIES", "🏠 العقارات", pendingPropertiesCount),
        Triple("STORES", "🏪 المحلات", pendingStoresCount),
        Triple("MEDICAL", "🏥 المراكز الطبية", pendingMedicalCount),
        Triple("RESTAURANTS", "🍔 المطاعم", pendingRestaurantsCount),
        Triple("JOBS", "💼 إعلانات التوظيف", pendingJobsCount),
        Triple("USERS", "👤 المستخدمين", registeredUsersList.size + pendingClientsProviders.size)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📨 طلبات الانضمام والاعتماد المعلقة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(subTabs) { st ->
                val isSel = selectedTab == st.first
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { selectedTab = st.first }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Text("${st.second} (${st.third})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedTab) {
            "SERVICES" -> {
                if (activeServicesPending.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                        Text("لا توجد طلبات معلقة للمهن والخدمات حالياً.", fontSize = 12.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeServicesPending.forEach { req ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "الاسم: ${req.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "المهنة / الخدمة: ${req.customCategoryName.ifBlank { req.categoryId }}", fontSize = 11.5.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                    Text(text = "الهاتف: ${req.phone} | المنطقة: ${req.area} - ${req.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    if (req.specialization.isNotBlank()) {
                                        Text(text = "التخصص: ${req.specialization}", fontSize = 11.sp, color = themeColors.accent)
                                    }
                                    if (req.experienceYears.isNotBlank() || req.qualification.isNotBlank()) {
                                        val expQual = listOfNotNull(
                                            req.qualification.takeIf { it.isNotBlank() }?.let { "المؤهل: $it" },
                                            req.experienceYears.takeIf { it.isNotBlank() }?.let { "الخبرة: $it" }
                                        ).joinToString(" | ")
                                        Text(text = "🎓 $expQual", fontSize = 11.sp, color = Color.White)
                                    }
                                    if (req.description.isNotBlank()) {
                                        Text(text = "📝 نبذة: ${req.description}", fontSize = 10.5.sp, color = Color.LightGray)
                                    }
                                    if (req.gpsCoords.isNotBlank() || req.latitude != 0.0) {
                                        val coordsStr = if (req.gpsCoords.isNotBlank()) req.gpsCoords else "${req.latitude}, ${req.longitude}"
                                        Text(text = "📍 إحداثيات الموقع: $coordsStr", fontSize = 10.5.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Medium)
                                    } else {
                                        Text(text = "⚠️ بدون إحداثيات GPS — سيُستخدم مركز المدينة (${req.area.ifBlank { "صنعاء" }})", fontSize = 10.5.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Medium)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                                        if (req.selfiePhotoBase64.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.Black.copy(alpha = 0.3f),
                                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f))
                                            ) {
                                                Text("📷 تم إرفاق الصورة / الشعار", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                        if (req.idPhotoBase64.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.Black.copy(alpha = 0.3f),
                                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                                            ) {
                                                Text("📄 تم إرفاق بطاقة الهوية / الترخيص", fontSize = 10.sp, color = Color(0xFF10B981), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    if (req.password.isNotBlank() || req.passwordHash.isNotBlank()) {
                                        Text(text = "🔒 كلمة المرور: محمية ومشفرة", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                    }
                                    if (req.selfiePhotoBase64.isNotBlank() || req.idPhotoBase64.isNotBlank() || req.workPhotosBase64.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                val items = req.buildAttachmentPreviewItems()
                                                if (items.isNotEmpty()) {
                                                    previewingAttachmentsFor = req.name to items
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = "معاينة المرفقات", tint = themeColors.accent, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("👁️ معاينة الصور والمستندات", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { viewModel.approveRequest(req) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("موافقة واعتماد", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { rejectingRequest = req },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض الطلب", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "PROPERTIES" -> {
                if (pendingPropertiesList.isEmpty() && pendingPropertiesFromProviders.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                        Text("لا توجد عقارات بانتظار الموافقة حالياً.", fontSize = 12.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pendingPropertiesFromProviders.forEach { req ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "🏠 طلب انضمام عقار: ${req.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "الهاتف: ${req.phone} | المنطقة: ${req.area} - ${req.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    if (req.password.isNotBlank() || req.passwordHash.isNotBlank()) {
                                        Text(text = "🔒 كلمة المرور: محمية ومشفرة", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                    }
                                    if (req.selfiePhotoBase64.isNotBlank() || req.idPhotoBase64.isNotBlank() || req.workPhotosBase64.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                val items = req.buildAttachmentPreviewItems()
                                                if (items.isNotEmpty()) {
                                                    previewingAttachmentsFor = req.name to items
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = "معاينة المرفقات", tint = themeColors.accent, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("👁️ معاينة الصور والمستندات", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { viewModel.approveRequest(req) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("موافقة واعتماد", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { rejectingRequest = req },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        pendingPropertiesList.forEach { prop ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = prop.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "الموقع: ${prop.cityId} - ${prop.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Text(text = "السعر: ${prop.price} | الهاتف: ${prop.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { viewModel.setPropertyActive(prop.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("موافقة ونشر", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { pendingDeletionTarget = Triple("PROPERTY", prop.id, prop.title) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("حذف / رفض", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "STORES", "MEDICAL", "RESTAURANTS" -> {
                val (providerList, entityList, tabName) = when (selectedTab) {
                    "MEDICAL" -> Triple(pendingMedicalProviders, pendingMedicalStores, "المراكز الطبية")
                    "RESTAURANTS" -> Triple(pendingRestaurantsProviders, pendingRestaurantsStores, "المطاعم والكافيهات")
                    else -> Triple(pendingStoresProviders, pendingRegularStores, "المحلات والمتاجر")
                }
                if (providerList.isEmpty() && entityList.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                        Text("لا توجد طلبات معلقة لـ $tabName حالياً.", fontSize = 12.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        providerList.forEach { req ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "🏪 اسم المنشأة / المحل: ${req.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "الهاتف: ${req.phone} | المنطقة: ${req.area} - ${req.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    if (req.specialization.isNotBlank()) {
                                        Text(text = "النشاط: ${req.specialization}", fontSize = 11.sp, color = themeColors.accent)
                                    }
                                    if (req.password.isNotBlank() || req.passwordHash.isNotBlank()) {
                                        Text(text = "🔒 كلمة المرور: محمية ومشفرة", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                    }
                                    if (req.selfiePhotoBase64.isNotBlank() || req.idPhotoBase64.isNotBlank() || req.workPhotosBase64.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                val items = req.buildAttachmentPreviewItems()
                                                if (items.isNotEmpty()) {
                                                    previewingAttachmentsFor = req.name to items
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = "معاينة المرفقات", tint = themeColors.accent, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("👁️ معاينة الشعار والمستندات", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { viewModel.approveRequest(req) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("موافقة واعتماد", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { rejectingRequest = req },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض الطلب", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        entityList.forEach { s ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = s.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "التصنيف: ${s.categoryId} | الهاتف: ${s.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { viewModel.setStoreActive(s.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("موافقة واعتماد", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { pendingDeletionTarget = Triple("STORE", s.id, s.name) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض وحذف", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "JOBS" -> {
                if (pendingJobsList.isEmpty() && pendingJobsProviders.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                        Text("لا توجد إعلانات وظائف معلقة بانتظار الاعتماد.", fontSize = 12.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pendingJobsProviders.forEach { req ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "💼 وظيفة: ${req.customCategoryName.ifBlank { req.name }}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "جهة العمل: ${req.name} | الهاتف: ${req.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    if (req.specialization.isNotBlank()) {
                                        Text(text = "التفاصيل: ${req.specialization}", fontSize = 11.sp, color = themeColors.accent)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { viewModel.approveRequest(req) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("موافقة ونشر", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { rejectingRequest = req },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        pendingJobsList.forEach { job ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = job.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "جهة العمل: ${job.companyName} | الموقع: ${job.cityId}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { viewModel.setJobApproved(job.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("موافقة ونشر", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { pendingDeletionTarget = Triple("JOB", job.id, job.title) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض وحذف", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "USERS" -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (pendingClientsProviders.isNotEmpty()) {
                        Text("⏳ طلبات تسجيل مستخدمين معلقة (${pendingClientsProviders.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                        pendingClientsProviders.forEach { req ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "👤 اسم المستخدم: ${req.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "الهاتف: ${req.phone} | المدينة/المحافظة: ${req.area}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    if (req.password.isNotBlank() || req.passwordHash.isNotBlank()) {
                                        Text(text = "🔒 كلمة المرور: محمية ومشفرة", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { viewModel.approveRequest(req) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("تفعيل الحساب", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { rejectingRequest = req },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                    }
                    AdminUserManager(
                        mainViewModel = viewModel,
                        themeColors = themeColors,
                        isPanelMode = true
                    )
                }
            }
        }
    }

    rejectingRequest?.let { req ->
        AlertDialog(
            onDismissRequest = { rejectingRequest = null },
            title = { Text("رفض طلب الانضمام", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يرجى كتابة سبب الرفض لإشعار مقدم الطلب:", color = themeColors.textSecondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("سبب الرفض...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectRequest(req, rejectionReason)
                        rejectingRequest = null
                        rejectionReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الرفض", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingRequest = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    pendingDeletionTarget?.let { (type, targetId, title) ->
        AlertDialog(
            onDismissRequest = { pendingDeletionTarget = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                    Text("تأكيد الحذف / الرفض", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف أو رفض \"$title\" بشكل نهائي؟",
                    color = themeColors.textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (type) {
                            "PROPERTY" -> viewModel.deleteProperty(targetId)
                            "STORE" -> viewModel.deleteStore(targetId)
                            "JOB" -> viewModel.deleteJob(targetId)
                        }
                        pendingDeletionTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، حذف", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletionTarget = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    previewingAttachmentsFor?.let { (applicantName, attachments) ->
        AlertDialog(
            onDismissRequest = { previewingAttachmentsFor = null },
            title = {
                Text("📎 مرفقات: $applicantName", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    attachments.forEach { (label, source) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .padding(8.dp)
                        ) {
                            Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Spacer(modifier = Modifier.height(6.dp))
                            if (source.startsWith("http://") || source.startsWith("https://") || source.startsWith("content://") || source.startsWith("file://")) {
                                AsyncImage(
                                    model = source,
                                    contentDescription = label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                val decodedBitmap = remember(source) {
                                    runCatching {
                                        val cleanB64 = if (source.contains(",")) source.substringAfter(",") else source
                                        val bytes = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
                                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    }.getOrNull()
                                }
                                if (decodedBitmap != null) {
                                    Image(
                                        bitmap = decodedBitmap.asImageBitmap(),
                                        contentDescription = label,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(170.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Text("⚠️ تعذر فك ترميز هذا المرفق", fontSize = 11.sp, color = Color(0xFFFBBF24))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { previewingAttachmentsFor = null }) {
                    Text("إغلاق", color = themeColors.accent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
