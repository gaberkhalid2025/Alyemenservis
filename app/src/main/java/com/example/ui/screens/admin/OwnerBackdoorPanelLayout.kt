@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.admin

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.screens.admin.components.AdminConfirmDialog
import com.example.ui.screens.admin.components.AdminLogger
import com.example.ui.screens.admin.components.AdminSwitchRow
import com.example.ui.utils.*
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 👑 بوابة المالك والتحكم الخلفي الديناميكي (OwnerBackdoorPanelLayout)
 * 
 * تتيح لمالك المنصة التحكم الشامل والفوري بجميع إعدادات التطبيق وسلوك النظام، وتشمل:
 * - تخصيص هوية التطبيق (الاسم، رسائل الترحيب، الفوتر، الإصدار).
 * - مفاتيح تفعيل الأقسام والتسجيل للمهن والمتاجر والعقارات والوظائف والعيادات.
 * - إعدادات الأمان والتشفير وإلزامية كلمات المرور ونظام الحماية وجدار النار.
 * - التحكم في البنرات الإعلانية وصفحة "عن التطبيق" والروابط الرسمية والدعم الفني.
 * - إدارة بيانات المدير (Admin) وعمليات التطهير ومسح البيانات الوهمية.
 */
@Composable
fun OwnerBackdoorPanelLayout(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val settingsState by viewModel.settings.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var appName by remember { mutableStateOf(settingsState.appName) }
    var welcomeMessage by remember { mutableStateOf(settingsState.welcomeMessage) }
    var footerMessage by remember { mutableStateOf(settingsState.footerMessage) }
    var footerBgColorHex by remember { mutableStateOf(settingsState.footerBgColorHex) }
    var footerItemsOrder by remember { mutableStateOf(settingsState.footerItemsOrder) }
    var showInfoIcon by remember { mutableStateOf(settingsState.showInfoIcon) }
    var showBookingsIcon by remember { mutableStateOf(settingsState.showBookingsIcon) }
    var showLangIcon by remember { mutableStateOf(settingsState.showLangIcon) }
    var showAdminIcon by remember { mutableStateOf(settingsState.showAdminIcon) }
    var showFooterText by remember { mutableStateOf(settingsState.showFooterText) }
    var infoIconType by remember { mutableStateOf(settingsState.infoIconType) }
    var adminIconType by remember { mutableStateOf(settingsState.adminIconType) }
    var langIconType by remember { mutableStateOf(settingsState.langIconType) }
    var bookingsIconType by remember { mutableStateOf(settingsState.bookingsIconType) }
    var appVersion by remember { mutableStateOf(settingsState.appVersion) }
    var supportPhone by remember { mutableStateOf(settingsState.supportPhone) }
    var supportEmail by remember { mutableStateOf(settingsState.supportEmail) }
    var supportWhatsapp by remember { mutableStateOf(settingsState.supportWhatsapp) }
    var appDownloadUrl by remember { mutableStateOf(settingsState.appDownloadUrl) }
    var activeThemeId by remember { mutableStateOf(settingsState.activeThemeId) }
    var isMaintenanceActive by remember { mutableStateOf(settingsState.isMaintenanceActive) }
    var hidePromoFooter by remember { mutableStateOf(settingsState.hidePromoFooter) }
    var assistantHidden by remember { mutableStateOf(settingsState.assistantHidden) }
    var assistantSize by remember { mutableFloatStateOf(56f) }
    var chatHidden by remember { mutableStateOf(settingsState.chatHidden) }
    var chatSize by remember { mutableFloatStateOf(if (settingsState.chatSize > 0) settingsState.chatSize.toFloat() else 56f) }
    var maxSearchRadiusKm by remember { mutableFloatStateOf(settingsState.maxSearchRadiusKm.toFloat()) }
    var isSpeechSearchEnabled by remember { mutableStateOf(settingsState.isSpeechSearchEnabled) }
    var isDataSaverEnabled by remember { mutableStateOf(false) }
    var appImageQuality by remember { mutableFloatStateOf(90f) }
    var bypassVisitorRegistration by remember { mutableStateOf(settingsState.bypassVisitorRegistration) }
    var isUserPasswordRequired by remember { mutableStateOf(settingsState.isUserPasswordRequired) }
    var disableChatFirewall by remember { mutableStateOf(settingsState.disableChatFirewall) }
    var disableBookingFirewall by remember { mutableStateOf(settingsState.disableBookingFirewall) }
    var isMapFeatureEnabled by remember { mutableStateOf(settingsState.isMapFeatureEnabled) }
    var isStoresEnabled by remember { mutableStateOf(settingsState.isStoresEnabled) }
    var isPropertiesEnabled by remember { mutableStateOf(settingsState.isPropertiesEnabled) }
    var enableProvidersRegistration by remember { mutableStateOf(settingsState.enableProvidersRegistration) }
    var enableStoresRegistration by remember { mutableStateOf(settingsState.enableStoresRegistration) }
    var enableRestaurantsRegistration by remember { mutableStateOf(settingsState.enableRestaurantsRegistration) }
    var enablePropertiesRegistration by remember { mutableStateOf(settingsState.enablePropertiesRegistration) }
    var enableMedicalRegistration by remember { mutableStateOf(settingsState.enableMedicalRegistration) }
    var enableJobsRegistration by remember { mutableStateOf(settingsState.enableJobsRegistration) }
    var showRefreshIcon by remember { mutableStateOf(settingsState.showRefreshIcon) }
    var showSettingsIcon by remember { mutableStateOf(settingsState.showSettingsIcon) }
    var headerIconsOrder by remember { mutableStateOf(settingsState.headerIconsOrder) }
    var categoriesLayoutTypeState by remember { mutableStateOf(settingsState.categoriesLayoutType) }
    var websiteUrl by remember { mutableStateOf(settingsState.websiteUrl) }
    var telegramUrl by remember { mutableStateOf(settingsState.telegramUrl) }
    var facebookUrl by remember { mutableStateOf(settingsState.facebookUrl) }
    var twitterUrl by remember { mutableStateOf(settingsState.twitterUrl) }
    var instagramUrl by remember { mutableStateOf(settingsState.instagramUrl) }
    var youtubeUrl by remember { mutableStateOf(settingsState.youtubeUrl) }
    var aboutLayoutOrder by remember { mutableStateOf(settingsState.aboutLayoutOrder) }
    var hideTwitter by remember { mutableStateOf(settingsState.hideTwitter) }
    var hideInstagram by remember { mutableStateOf(settingsState.hideInstagram) }
    var hideYoutube by remember { mutableStateOf(settingsState.hideYoutube) }
    var hideFacebook by remember { mutableStateOf(settingsState.hideFacebook) }
    var hideTelegram by remember { mutableStateOf(settingsState.hideTelegram) }
    var hideWebsite by remember { mutableStateOf(settingsState.hideWebsite) }

    // Booking form full control states
    var bookingTermsInput by remember { mutableStateOf(settingsState.bookingTerms) }
    var bookingLabelNameInput by remember { mutableStateOf(settingsState.bookingLabelName) }
    var bookingLabelPhoneInput by remember { mutableStateOf(settingsState.bookingLabelPhone) }
    var bookingLabelAreaInput by remember { mutableStateOf(settingsState.bookingLabelArea) }
    var bookingLabelServiceInput by remember { mutableStateOf(settingsState.bookingLabelService) }

    // About App customization states
    var aboutCoverType by remember { mutableStateOf(settingsState.aboutCoverType) }
    var aboutCoverContent by remember { mutableStateOf(settingsState.aboutCoverContent) }
    var aboutCoverBase64 by remember { mutableStateOf(settingsState.aboutCoverBase64) }
    var aboutCustomInfo by remember { mutableStateOf(settingsState.aboutCustomInfo) }
    var encryptionTypeState by remember { mutableStateOf(settingsState.encryptionType) }
    var splashWelcomeMessageState by remember { mutableStateOf(settingsState.splashWelcomeMessage) }

    // Banner control states
    var bannerEnabled by remember { mutableStateOf(settingsState.bannerEnabled) }
    var bannerType by remember { mutableStateOf(settingsState.bannerType) }
    var bannerContent by remember { mutableStateOf(settingsState.bannerContent) }
    var bannerBase64 by remember { mutableStateOf(settingsState.bannerBase64) }
    var bannerLocation by remember { mutableStateOf(settingsState.bannerLocation) }
    var bannerDurationSeconds by remember { mutableStateOf(settingsState.bannerDurationSeconds.toString()) }
    var bannerDisplayStyle by remember { mutableStateOf(settingsState.bannerDisplayStyle) }

    var mapProviderState by remember { mutableStateOf(settingsState.mapProvider) }

    val context = LocalContext.current
    val sp = remember { context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE) }
    var rememberLoginInput by remember { mutableStateOf(sp.getString("saved_admin_role", "GUEST") != "GUEST") }
    var adminUsernameInput by remember { mutableStateOf(settingsState.adminUsername) }
    var adminPasswordInput by remember { mutableStateOf(settingsState.adminPassword) }

    // Dialog state for wiping data
    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var wipePasswordInput by remember { mutableStateOf("") }
    var wipeErrorMsg by remember { mutableStateOf("") }

    val galleryLauncherForBanner = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val mimeType = context.contentResolver.getType(it)
                if (mimeType != null && mimeType.startsWith("video/")) {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        bannerBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        bannerType = "VIDEO"
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("📹 تم تحميل الفيديو القصير للبنر بنجاح!")
                        }
                    }
                } else {
                    val base64Str = compressAndResizeImageUri(context, it, 800, 70)
                    if (base64Str.isNotEmpty()) {
                        bannerBase64 = base64Str
                        bannerType = "IMAGE"
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("📸 تم تحميل صورة البنر الإعلاني من المعرض بنجاح!")
                        }
                    }
                }
            } catch (e: Exception) {
                AdminLogger.logError("GalleryBannerUpload", e)
            }
        }
    }

    val galleryLauncherForCover = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val base64Str = compressAndResizeImageUri(context, it, 800, 70)
                if (base64Str.isNotEmpty()) {
                    aboutCoverBase64 = base64Str
                    aboutCoverType = "IMAGE"
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("📸 تم تحميل صورة الغلاف من المعرض بنجاح!")
                    }
                }
            } catch (e: Exception) {
                AdminLogger.logError("GalleryCoverUpload", e)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = themeColors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "🔓 بوابة المالك والتحكم الخلفي الديناميكي",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                }
                Button(
                    onClick = {
                        AdminLogger.logAction("OWNER_LOGOUT", "إغلاق لوحة المالك والعودة")
                        viewModel.logout(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إغلاق اللوحة", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // القسم الأول: هوية ونصوص التطبيق
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🏷️ نصوص وهوية التطبيق الرئيسية:", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text("اسم التطبيق الرئيسي") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = welcomeMessage,
                        onValueChange = { welcomeMessage = it },
                        label = { Text("رسالة الترحيب في الهيدر") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = splashWelcomeMessageState,
                        onValueChange = { splashWelcomeMessageState = it },
                        label = { Text("رسالة ترحيب شاشة البداية (Splash)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = footerMessage,
                        onValueChange = { footerMessage = it },
                        label = { Text("نص الفوتر الأوسط") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = footerBgColorHex,
                        onValueChange = { footerBgColorHex = it },
                        label = { Text("لون خلفية الفوتر (كود Hex مثل #115E59)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = appVersion,
                        onValueChange = { appVersion = it },
                        label = { Text("رقم إصدار التطبيق بالفوتر (مثل v2.2026)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            }

            // القسم الثاني: إعدادات ومفاتيح الشريط السفلي
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚙️ التحكم في أيقونات ومظهر الشريط السفلي (الفوتر):", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    
                    AdminSwitchRow(
                        title = "ℹ️ أيقونة 'عن التطبيق':",
                        checked = showInfoIcon,
                        onCheckedChange = { showInfoIcon = it },
                        themeColors = themeColors
                    )
                    AdminSwitchRow(
                        title = "📅 أيقونة 'الحجوزات':",
                        checked = showBookingsIcon,
                        onCheckedChange = { showBookingsIcon = it },
                        themeColors = themeColors
                    )
                    AdminSwitchRow(
                        title = "🌐 أيقونة 'تبديل اللغة' (العربية / English):",
                        checked = showLangIcon,
                        onCheckedChange = { showLangIcon = it },
                        themeColors = themeColors
                    )
                    AdminSwitchRow(
                        title = "🔐 أيقونة 'لوحة التحكم والإدارة':",
                        checked = showAdminIcon,
                        onCheckedChange = { showAdminIcon = it },
                        themeColors = themeColors
                    )
                    AdminSwitchRow(
                        title = "✍️ إظهار نص الفوتر السفلي:",
                        checked = showFooterText,
                        onCheckedChange = { showFooterText = it },
                        themeColors = themeColors
                    )
                }
            }

            // القسم الثالث: وسائل الدعم والتواصل وروابط التطبيق
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📞 وسائل الدعم الفني والروابط الرسمية:", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = supportPhone,
                        onValueChange = { supportPhone = it },
                        label = { Text("رقم هاتف الدعم الفني للبرنامج") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = supportEmail,
                        onValueChange = { supportEmail = it },
                        label = { Text("البريد الإلكتروني للدعم والمظالم") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = supportWhatsapp,
                        onValueChange = { supportWhatsapp = it },
                        label = { Text("رابط أو رقم واتساب الدعم الفني") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = appDownloadUrl,
                        onValueChange = { appDownloadUrl = it },
                        label = { Text("رابط خارجي لتحميل التطبيق (نسخة APK)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = websiteUrl,
                        onValueChange = { websiteUrl = it },
                        label = { Text("رابط الموقع الإلكتروني الرسمي") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = telegramUrl,
                        onValueChange = { telegramUrl = it },
                        label = { Text("رابط حساب/قناة التليجرام") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = facebookUrl,
                        onValueChange = { facebookUrl = it },
                        label = { Text("رابط صفحة الفيسبوك الرسمية") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            }

            // القسم الرابع: سمات ومظهر التطبيق
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎨 سمة التطبيق الافتراضية:", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    
                    val themes = listOf(
                        "EMERALD_YEMEN" to "الزمرد اليمني 🟢",
                        "COSMIC_SILVER" to "كوزميك سيلفر 🪐",
                        "LUXURY_GOLD" to "الذهبي الفاخر 🌟",
                        "ELITE_EMERALD" to "الزمردي الراقي 💚",
                        "SMOKE_BLACK" to "الأسود الدخاني ⚫",
                        "LIGHT_PINK" to "الزهري الفاتح 🌸",
                        "GOLDEN_WHITE" to "الأبيض الذهبي ⚪🟡",
                        "CUSTOM_THEME" to "سمة مخصصة 🎨"
                    )
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(themes) { (thId, label) ->
                            val isSel = activeThemeId == thId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) themeColors.accent else Color(0xFF1E293B))
                                    .border(1.dp, if (isSel) Color.White else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clickable { activeThemeId = thId }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    label,
                                    fontSize = 10.sp,
                                    color = if (isSel) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // القسم الخامس: مفاتيح وميزات النظام وجدار النار
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🛡️ مفاتيح النظام، وضع الصيانة والأمان:", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

                    AdminSwitchRow(
                        title = "وضع الصيانة المؤقت",
                        subtitle = "يقوم بإيقاف التطبيق مؤقتاً وعرض رسالة الصيانة للمستخدمين",
                        checked = isMaintenanceActive,
                        onCheckedChange = { isMaintenanceActive = it },
                        themeColors = themeColors
                    )

                    AdminSwitchRow(
                        title = "إخفاء زر المساعد الذكي العائم",
                        checked = assistantHidden,
                        onCheckedChange = { assistantHidden = it },
                        themeColors = themeColors
                    )

                    AdminSwitchRow(
                        title = "تفعيل البحث الصوتي",
                        checked = isSpeechSearchEnabled,
                        onCheckedChange = { isSpeechSearchEnabled = it },
                        themeColors = themeColors
                    )

                    AdminSwitchRow(
                        title = "إلغاء شرط تسجيل الزائرين للحجز والمحادثة",
                        checked = bypassVisitorRegistration,
                        onCheckedChange = { bypassVisitorRegistration = it },
                        themeColors = themeColors
                    )

                    AdminSwitchRow(
                        title = "🔑 إلزامية كلمة المرور لجميع حسابات المواطنين الجديدة",
                        checked = isUserPasswordRequired,
                        onCheckedChange = { isUserPasswordRequired = it },
                        themeColors = themeColors
                    )

                    AdminSwitchRow(
                        title = "تمكين ميزة الرادار والخريطة للجماهير",
                        checked = isMapFeatureEnabled,
                        onCheckedChange = { isMapFeatureEnabled = it },
                        themeColors = themeColors
                    )

                    // اختيار محرك الخرائط
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("محرك الخرائط المعتمد:", color = Color.White, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("MAPLIBRE" to "MapLibre", "GOOGLE" to "Google", "MAPBOX" to "Mapbox").forEach { (mKey, mName) ->
                                val isSel = mapProviderState == mKey
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) themeColors.accent else Color(0xFF1E293B))
                                        .clickable { mapProviderState = mKey }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(mName, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // القسم السادس: مفاتيح تسجيل الكيانات والأقسام
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📋 تمكين التسجيل الذاتي للأقسام والمهن:", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

                    AdminSwitchRow(
                        title = "🛠️ تسجيل الحرفيين والفنيين",
                        checked = enableProvidersRegistration,
                        onCheckedChange = { enableProvidersRegistration = it },
                        themeColors = themeColors
                    )

                    AdminSwitchRow(
                        title = "🏬 تسجيل المتاجر والمحلات",
                        checked = enableStoresRegistration,
                        onCheckedChange = { enableStoresRegistration = it },
                        themeColors = themeColors
                    )

                    AdminSwitchRow(
                        title = "🍔 تسجيل المطاعم والكافيهات",
                        checked = enableRestaurantsRegistration,
                        onCheckedChange = { enableRestaurantsRegistration = it },
                        themeColors = themeColors
                    )

                    AdminSwitchRow(
                        title = "🏠 تسجيل العقارات والمكاتب",
                        checked = enablePropertiesRegistration,
                        onCheckedChange = { enablePropertiesRegistration = it },
                        themeColors = themeColors
                    )

                    AdminSwitchRow(
                        title = "🏥 تسجيل العيادات والمراكز الطبية",
                        checked = enableMedicalRegistration,
                        onCheckedChange = { enableMedicalRegistration = it },
                        themeColors = themeColors
                    )

                    AdminSwitchRow(
                        title = "💼 تسجيل الشركات وفرص العمل",
                        checked = enableJobsRegistration,
                        onCheckedChange = { enableJobsRegistration = it },
                        themeColors = themeColors
                    )
                }
            }

            // القسم السابع: شروط وتخصيص نموذج الحجز
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📅 تخصيص شروط واستمارة الحجز اليمني:", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = bookingTermsInput,
                        onValueChange = { bookingTermsInput = it },
                        label = { Text("نص شروط وقواعد الحجز بالدليل") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = bookingLabelNameInput,
                        onValueChange = { bookingLabelNameInput = it },
                        label = { Text("تسمية حقل الاسم بالاستمارة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = bookingLabelPhoneInput,
                        onValueChange = { bookingLabelPhoneInput = it },
                        label = { Text("تسمية حقل الهاتف بالاستمارة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = bookingLabelAreaInput,
                        onValueChange = { bookingLabelAreaInput = it },
                        label = { Text("تسمية حقل منطقة السكن بالاستمارة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = bookingLabelServiceInput,
                        onValueChange = { bookingLabelServiceInput = it },
                        label = { Text("تسمية حقل نوع الخدمة بالاستمارة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            }

            // القسم الثامن: بيانات المدير (Admin) والدخول
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🔐 بيانات المدير (Admin) والدخول:", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = adminUsernameInput,
                        onValueChange = { adminUsernameInput = it },
                        label = { Text("اسم مستخدم المدير") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = adminPasswordInput,
                        onValueChange = { adminPasswordInput = it },
                        label = { Text("كلمة مرور المدير") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    AdminSwitchRow(
                        title = "تذكرني (حفظ الدخول بصورة دائمة كمالك)",
                        checked = rememberLoginInput,
                        onCheckedChange = { rememberLoginInput = it },
                        themeColors = themeColors
                    )
                }
            }

            // زر الحفظ الرئيسي للإعدادات
            Button(
                onClick = {
                    val currentSettings = settingsState.copy(
                        appName = appName,
                        welcomeMessage = welcomeMessage,
                        footerMessage = footerMessage,
                        footerBgColorHex = footerBgColorHex,
                        footerItemsOrder = footerItemsOrder,
                        appVersion = appVersion,
                        supportPhone = supportPhone,
                        supportEmail = supportEmail,
                        supportWhatsapp = supportWhatsapp,
                        activeThemeId = activeThemeId,
                        isMaintenanceActive = isMaintenanceActive,
                        hidePromoFooter = hidePromoFooter,
                        assistantHidden = assistantHidden,
                        assistantSize = assistantSize.toInt(),
                        chatHidden = chatHidden,
                        chatSize = chatSize.toInt(),
                        maxSearchRadiusKm = maxSearchRadiusKm.toInt(),
                        isSpeechSearchEnabled = isSpeechSearchEnabled,
                        bypassVisitorRegistration = bypassVisitorRegistration,
                        isUserPasswordRequired = isUserPasswordRequired,
                        disableChatFirewall = disableChatFirewall,
                        disableBookingFirewall = disableBookingFirewall,
                        isMapFeatureEnabled = isMapFeatureEnabled,
                        mapProvider = mapProviderState,
                        enableProvidersRegistration = enableProvidersRegistration,
                        enableStoresRegistration = enableStoresRegistration,
                        enableRestaurantsRegistration = enableRestaurantsRegistration,
                        enablePropertiesRegistration = enablePropertiesRegistration,
                        enableMedicalRegistration = enableMedicalRegistration,
                        enableJobsRegistration = enableJobsRegistration,
                        bookingTerms = bookingTermsInput,
                        bookingLabelName = bookingLabelNameInput,
                        bookingLabelPhone = bookingLabelPhoneInput,
                        bookingLabelArea = bookingLabelAreaInput,
                        bookingLabelService = bookingLabelServiceInput,
                        adminUsername = adminUsernameInput,
                        adminPassword = adminPasswordInput,
                        aboutCoverType = aboutCoverType,
                        aboutCoverContent = aboutCoverContent,
                        aboutCoverBase64 = aboutCoverBase64,
                        aboutCustomInfo = aboutCustomInfo,
                        encryptionType = encryptionTypeState,
                        splashWelcomeMessage = splashWelcomeMessageState,
                        bannerEnabled = bannerEnabled,
                        bannerType = bannerType,
                        bannerContent = bannerContent,
                        bannerBase64 = bannerBase64,
                        bannerLocation = bannerLocation,
                        bannerDurationSeconds = bannerDurationSeconds.toIntOrNull() ?: 0,
                        bannerDisplayStyle = bannerDisplayStyle,
                        appDownloadUrl = appDownloadUrl,
                        showRefreshIcon = showRefreshIcon,
                        showSettingsIcon = showSettingsIcon,
                        headerIconsOrder = headerIconsOrder,
                        categoriesLayoutType = categoriesLayoutTypeState,
                        isStoresEnabled = isStoresEnabled,
                        isPropertiesEnabled = isPropertiesEnabled,
                        websiteUrl = websiteUrl,
                        telegramUrl = telegramUrl,
                        facebookUrl = facebookUrl,
                        twitterUrl = twitterUrl,
                        instagramUrl = instagramUrl,
                        youtubeUrl = youtubeUrl,
                        aboutLayoutOrder = aboutLayoutOrder,
                        showInfoIcon = showInfoIcon,
                        showBookingsIcon = showBookingsIcon,
                        showLangIcon = showLangIcon,
                        showAdminIcon = showAdminIcon,
                        showFooterText = showFooterText,
                        infoIconType = infoIconType,
                        adminIconType = adminIconType,
                        langIconType = langIconType,
                        bookingsIconType = bookingsIconType,
                        hideTwitter = hideTwitter,
                        hideInstagram = hideInstagram,
                        hideYoutube = hideYoutube,
                        hideFacebook = hideFacebook,
                        hideTelegram = hideTelegram,
                        hideWebsite = hideWebsite
                    )
                    viewModel.saveCustomSettingsState(currentSettings)
                    if (rememberLoginInput) {
                        sp.edit().putString("saved_admin_role", "OWNER").apply()
                    } else {
                        sp.edit().putString("saved_admin_role", "GUEST").apply()
                    }
                    AdminLogger.logAction("SAVE_SETTINGS", "حفظ إعدادات المالك والتحكم الخلفي بنجاح")
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("💾 تم حفظ كافة التخصيصات والتحققات بنجاح!")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("💾 حفظ إعدادات البوابة والتخزين والتطبيق", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // نافذة التأكيد المحمية لتطهير البيانات الوهمية
            if (showWipeConfirmDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showWipeConfirmDialog = false
                        wipePasswordInput = ""
                        wipeErrorMsg = ""
                    },
                    containerColor = Color(0xFF1E293B),
                    icon = {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(36.dp))
                    },
                    title = {
                        Text("🔒 تأكيد حذف ومسح البيانات المؤقتة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350), textAlign = TextAlign.Center)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⚠️ تنبيه أمني: يتطلب حذف أو تطهير البيانات إدخال كلمة مرور الأدمن لتأكيد الإجراء الحساس:", fontSize = 11.5.sp, color = Color.White)
                            OutlinedTextField(
                                value = wipePasswordInput,
                                onValueChange = {
                                    wipePasswordInput = it
                                    wipeErrorMsg = ""
                                },
                                label = { Text("كلمة مرور الأدمن") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            if (wipeErrorMsg.isNotEmpty()) {
                                Text(wipeErrorMsg, color = Color(0xFFEF5350), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (viewModel.verifyAdminOrOwnerPassword(wipePasswordInput)) {
                                    AdminLogger.logAction("WIPE_MOCK_DATA", "مسح وتطهير البيانات الوهمية والمؤقتة")
                                    viewModel.wipeAllMockAndTemporaryData()
                                    showWipeConfirmDialog = false
                                    wipePasswordInput = ""
                                    wipeErrorMsg = ""
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("🧹 تم مسح وتطهير كافة البيانات المؤقتة بنجاح!")
                                    }
                                } else {
                                    wipeErrorMsg = "❌ كلمة المرور غير صحيحة! تم منع التطهير والحذف."
                                    AdminLogger.logWarning("فشل التحقق من كلمة المرور أثناء محاولة مسح البيانات")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تأكيد الحذف النهائي 🗑️", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showWipeConfirmDialog = false
                            wipePasswordInput = ""
                            wipeErrorMsg = ""
                        }) {
                            Text("إلغاء", color = Color.LightGray)
                        }
                    }
                )
            }

            // زر فتح حوار تطهير البيانات
            Button(
                onClick = { showWipeConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف البيانات الوهمية",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("🧹 حذف الفنيين والرسائل والإشعارات الوهمية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
