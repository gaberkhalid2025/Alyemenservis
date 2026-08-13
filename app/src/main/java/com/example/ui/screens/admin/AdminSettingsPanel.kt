package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager
import com.example.util.UserRole

@Composable
fun AdminSettingsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    val currentRole = RoleManager.fromRoleString(viewModel.adminRole.value)
    if (!PermissionGuard.hasPermission(currentRole, "MANAGE_SETTINGS")) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("🔒 ليس لديك صلاحية للوصول إلى هذه اللوحة", color = Color.White, fontSize = 14.sp)
                Text("يرجى التواصل مع المالك أو المدير الرئيسي", color = Color.Gray, fontSize = 12.sp)
            }
        }
        return
    }

    val settingsState by viewModel.settings.collectAsState()
    val activeSubTab = state.activeSubTabState.value

    // Sync settingsState to state variables if they are currently uninitialized/empty
    LaunchedEffect(settingsState) {
        if (state.editPrimaryHexState.value.isBlank()) state.editPrimaryHexState.value = settingsState.customPrimaryHex
        if (state.editSecondaryHexState.value.isBlank()) state.editSecondaryHexState.value = settingsState.customSecondaryHex
        if (state.editCardBgHexState.value.isBlank()) state.editCardBgHexState.value = settingsState.cardBackgroundHex
        if (state.editProviderNameHexState.value.isBlank()) state.editProviderNameHexState.value = settingsState.providerNameColorHex
        if (state.editLocationHexState.value.isBlank()) state.editLocationHexState.value = settingsState.locationColorHex
        if (state.editRatingHexState.value.isBlank()) state.editRatingHexState.value = settingsState.ratingColorHex
        if (state.editVipBadgeHexState.value.isBlank()) state.editVipBadgeHexState.value = settingsState.vipBadgeColorHex
        if (state.editVerifiedHexState.value.isBlank()) state.editVerifiedHexState.value = settingsState.verifiedBadgeColorHex
        if (state.editRecommendedHexState.value.isBlank()) state.editRecommendedHexState.value = settingsState.recommendedBadgeColorHex
        if (state.editFontSelectedState.value.isBlank()) state.editFontSelectedState.value = settingsState.activeFontFamily
        
        state.editCoverHeightState.value = settingsState.coverHeight.toFloat()
        state.editAvatarSizeState.value = settingsState.avatarSize.toFloat()
        state.editElementSpacingState.value = settingsState.elementSpacing.toFloat()
        state.editCardPaddingState.value = settingsState.cardPadding.toFloat()

        state.editShowVipBadgeState.value = settingsState.showVipBadge
        state.editShowVerifiedBadgeState.value = settingsState.showVerifiedBadge
        state.editShowRecommendedBadgeState.value = settingsState.showRecommendedBadge
        state.editShowCallButtonState.value = settingsState.showCallButton
        state.editShowWhatsappButtonState.value = settingsState.showWhatsappButton
        state.editShowDetailsButtonState.value = settingsState.showDetailsButton
        state.editShowBookButtonState.value = settingsState.showBookButton
        state.editCallButtonColorHexState.value = settingsState.callButtonColorHex
        state.editWhatsappButtonColorHexState.value = settingsState.whatsappButtonColorHex
        state.editDetailsButtonColorHexState.value = settingsState.detailsButtonColorHex
        state.editBookButtonColorHexState.value = settingsState.bookButtonColorHex

        val reqs = settingsState.registrationRequirements.split(",").filter { it.isNotBlank() }
        state.requirementsListStateState.value = reqs
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = when (activeSubTab) {
                        "COLORS" -> "🎨 تخصيص ألوان الهوية والتطبيق"
                        "GOLDEN_ICONS" -> "👑 تخصيص الخطوط وحجم وأيقونات التنقل"
                        "CARD_CUSTOMIZER" -> "🎛️ تخصيص أبعاد ومقاسات وأزرار البطاقات"
                        "NEW_SECTION_CREATOR" -> "➕ إضافة وإدارة الأقسام والخدمات والمحافظ"
                        "REG_FORMS_MANAGER" -> "📋 تخصيص استمارات التسجيل للأعضاء"
                        else -> "⚙️ الإحصائيات والهوية وتخصيص الواجهات"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
                Text(
                    text = "تعديلات مباشرة ومزامنة تلقائية مع قواعد البيانات لضمان استقرار التطبيق.",
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Render specific Panel based on Sub-Tab
        when (activeSubTab) {
            "COLORS" -> {
                ColorCustomizerPanel(viewModel = viewModel, settingsState = settingsState, themeColors = themeColors, state = state)
            }
            "GOLDEN_ICONS" -> {
                GoldenIconsPanel(viewModel = viewModel, settingsState = settingsState, themeColors = themeColors, state = state)
            }
            "CARD_CUSTOMIZER" -> {
                CardCustomizerPanel(viewModel = viewModel, settingsState = settingsState, themeColors = themeColors, state = state)
            }
            "NEW_SECTION_CREATOR" -> {
                NewSectionCreatorPanel(viewModel = viewModel, settingsState = settingsState, themeColors = themeColors, state = state)
            }
            "REG_FORMS_MANAGER" -> {
                RegFormsManagerPanel(viewModel = viewModel, settingsState = settingsState, themeColors = themeColors, state = state)
            }
        }
    }
}

@Composable
fun ColorCustomizerPanel(viewModel: MainViewModel, settingsState: AdminSettingsEntity, themeColors: VisualThemePalette, state: AdminPanelState) {
    var primaryHex = state.editPrimaryHexState.value
    var secondaryHex = state.editSecondaryHexState.value
    var backgroundHex by remember { mutableStateOf("#0F172A") }
    var cardBgHex = state.editCardBgHexState.value
    var nameColorHex = state.editProviderNameHexState.value
    var ratingColorHex = state.editRatingHexState.value
    var locationColorHex = state.editLocationHexState.value
    var vipColorHex = state.editVipBadgeHexState.value
    var verifiedColorHex = state.editVerifiedHexState.value
    var recommendedColorHex = state.editRecommendedHexState.value

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🎨 الثيمات الجاهزة للتطبيق (اختر من بين 8 ألوان رسمية):", fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 12.sp)
        
        val colorPresets = listOf(
            Triple("🟢 الأخضر الزمردي", "#10B981", "#059669"),
            Triple("🔵 النيلي الملكي", "#3B82F6", "#2563EB"),
            Triple("🟣 البنفسجي الفاخر", "#8B5CF6", "#7C3AED"),
            Triple("🟡 الذهبي الملكي", "#F59E0B", "#D97706"),
            Triple("🔴 الأحـمر العـنابي", "#EF4444", "#DC2626"),
            Triple("🩵 الفيروزي العصري", "#06B6D4", "#0891B2"),
            Triple("🟠 البرتقالي الدافئ", "#F97316", "#EA580C"),
            Triple("⚙️ الرمادي المظلم", "#64748B", "#475569")
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            colorPresets.chunked(2).forEach { rowPresets ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowPresets.forEach { (name, pPrimary, pSecondary) ->
                        Button(
                            onClick = {
                                state.editPrimaryHexState.value = pPrimary
                                state.editSecondaryHexState.value = pSecondary
                                state.editCardBgHexState.value = "#1E293B"
                                backgroundHex = "#0F172A"
                                state.editProviderNameHexState.value = pPrimary
                                state.editRatingHexState.value = "#F59E0B"
                                state.editLocationHexState.value = "#94A3B8"
                                state.editVipBadgeHexState.value = "#F59E0B"
                                state.editVerifiedHexState.value = pPrimary
                                state.editRecommendedHexState.value = pPrimary
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = parseHexColorSafe(pPrimary, themeColors.primary)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(name, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        
        @Composable
        fun ColorInputField(label: String, value: String, onValueChange: (String) -> Unit) {
            val parsedColor = remember(value) { parseHexColorSafe(value, Color.Gray) }
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(parsedColor)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    )
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        label = { Text(label, fontSize = 10.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                        ),
                        singleLine = true
                    )
                }
            }
        }

        Text("🎨 ألوان الهوية الرئيسية والتطبيق:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        ColorInputField("اللون الأساسي للبراند (Primary)", primaryHex) { state.editPrimaryHexState.value = it }
        ColorInputField("اللون الثانوي للبراند (Secondary)", secondaryHex) { state.editSecondaryHexState.value = it }
        ColorInputField("لون خلفية التطبيق (Background)", backgroundHex) { backgroundHex = it }

        Text("🎛️ ألوان بطاقات وتفاصيل مقدمي الخدمات:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        ColorInputField("لون خلفية البطاقة (Card Background)", cardBgHex) { state.editCardBgHexState.value = it }
        ColorInputField("لون اسم الفني/المقدم (Name Color)", nameColorHex) { state.editProviderNameHexState.value = it }
        ColorInputField("لون أيقونة التقييم (Rating Stars)", ratingColorHex) { state.editRatingHexState.value = it }
        ColorInputField("لون الموقع والمسافة (Location Color)", locationColorHex) { state.editLocationHexState.value = it }

        Text("👑 ألوان الشارات والاعتمادات المميزة:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        ColorInputField("لون شارة التميز (VIP Badge)", vipColorHex) { state.editVipBadgeHexState.value = it }
        ColorInputField("لون شارة التوثيق (Verified Badge)", verifiedColorHex) { state.editVerifiedHexState.value = it }
        ColorInputField("لون شارة التوصية (Recommended Badge)", recommendedColorHex) { state.editRecommendedHexState.value = it }

        Button(
            onClick = {
                viewModel.saveCustomSettingsState(
                    settingsState.copy(
                        customPrimaryHex = primaryHex,
                        customSecondaryHex = secondaryHex,
                        customBackgroundHex = backgroundHex,
                        cardBackgroundHex = cardBgHex,
                        providerNameColorHex = nameColorHex,
                        ratingColorHex = ratingColorHex,
                        locationColorHex = locationColorHex,
                        vipBadgeColorHex = vipColorHex,
                        verifiedBadgeColorHex = verifiedColorHex,
                        recommendedBadgeColorHex = recommendedColorHex
                    )
                )
                viewModel.triggerNotification("🎨 تم تحديث وحفظ ألوان الهوية بنجاح!")
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("حفظ إعدادات الهوية والألوان 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun GoldenIconsPanel(viewModel: MainViewModel, settingsState: AdminSettingsEntity, themeColors: VisualThemePalette, state: AdminPanelState) {
    var activeFont = state.editFontSelectedState.value
    var fontScale by remember(settingsState.globalFontScale) { mutableStateOf(settingsState.globalFontScale) }
    
    var homeIcon by remember(settingsState.topHomeIcon) { mutableStateOf(settingsState.topHomeIcon) }
    var mapsIcon by remember(settingsState.topMapsIcon) { mutableStateOf(settingsState.topMapsIcon) }
    var joinIcon by remember(settingsState.topJoinIcon) { mutableStateOf(settingsState.topJoinIcon) }
    var notifIcon by remember(settingsState.topNotifIcon) { mutableStateOf(settingsState.topNotifIcon) }
    var chatsIcon by remember(settingsState.topChatsIcon) { mutableStateOf(settingsState.topChatsIcon) }

    var infoIcon by remember(settingsState.bottomInfoIcon) { mutableStateOf(settingsState.bottomInfoIcon) }
    var bookingsIcon by remember(settingsState.bottomBookingsIcon) { mutableStateOf(settingsState.bottomBookingsIcon) }
    var langIcon by remember(settingsState.bottomLangIcon) { mutableStateOf(settingsState.bottomLangIcon) }
    var adminIcon by remember(settingsState.bottomAdminIcon) { mutableStateOf(settingsState.bottomAdminIcon) }

    var iconSize by remember(settingsState.navIconSizeDp) { mutableStateOf(settingsState.navIconSizeDp.toFloat()) }
    var topStyle by remember(settingsState.topNavIconStyle) { mutableStateOf(settingsState.topNavIconStyle) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🔤 اختيار الخط الافتراضي للتطبيق:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val fonts = listOf("CAIRO" to "Cairo", "DEFAULT" to "Default", "TAHOMA" to "Tahoma", "AMIRI" to "Amiri")
            fonts.forEach { (key, label) ->
                val isSel = activeFont == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { state.editFontSelectedState.value = key }
                        .padding(vertical = 8.dp)
                        .border(1.dp, if (isSel) Color.White else Color.Transparent, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (isSel) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("📏 تكبير/تصغير حجم خطوط التطبيق: (${String.format("%.1f", fontScale)}x)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Slider(
            value = fontScale,
            onValueChange = { fontScale = it },
            valueRange = 0.8f..1.5f,
            colors = SliderDefaults.colors(
                thumbColor = themeColors.accent,
                activeTrackColor = themeColors.accent
            )
        )

        Text("✨ نمط الأيقونات العلوية والسفلية:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val styles = listOf("GOLDEN_3D" to "👑 ذهبي 3D", "METALLIC" to "💿 ميتاليك", "MINIMAL" to "📱 مينيمل")
            styles.forEach { (key, label) ->
                val isSel = topStyle == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { topStyle = key }
                        .padding(vertical = 8.dp)
                        .border(1.dp, if (isSel) Color.White else Color.Transparent, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (isSel) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("📐 حجم الأيقونات في شريط التنقل: (${iconSize.toInt()} dp)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Slider(
            value = iconSize,
            onValueChange = { iconSize = it },
            valueRange = 20f..40f,
            colors = SliderDefaults.colors(
                thumbColor = themeColors.accent,
                activeTrackColor = themeColors.accent
            )
        )

        Text("🖼️ تخصيص الأيقونات والرموز (Emojis):", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("الأيقونات العلوية:", color = themeColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = homeIcon, onValueChange = { homeIcon = it }, label = { Text("الرئيسية", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = mapsIcon, onValueChange = { mapsIcon = it }, label = { Text("الخرائط", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = joinIcon, onValueChange = { joinIcon = it }, label = { Text("التسجيل", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = notifIcon, onValueChange = { notifIcon = it }, label = { Text("الإشعارات", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = chatsIcon, onValueChange = { chatsIcon = it }, label = { Text("الدردشات", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("الأيقونات السفلية:", color = themeColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = infoIcon, onValueChange = { infoIcon = it }, label = { Text("المعلومات", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = bookingsIcon, onValueChange = { bookingsIcon = it }, label = { Text("الحجوزات", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = langIcon, onValueChange = { langIcon = it }, label = { Text("اللغة", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = adminIcon, onValueChange = { adminIcon = it }, label = { Text("الأدمن", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        }

        Button(
            onClick = {
                viewModel.saveCustomSettingsState(
                    settingsState.copy(
                        activeFontFamily = activeFont,
                        globalFontScale = fontScale,
                        topHomeIcon = homeIcon,
                        topMapsIcon = mapsIcon,
                        topJoinIcon = joinIcon,
                        topNotifIcon = notifIcon,
                        topChatsIcon = chatsIcon,
                        bottomInfoIcon = infoIcon,
                        bottomBookingsIcon = bookingsIcon,
                        bottomLangIcon = langIcon,
                        bottomAdminIcon = adminIcon,
                        navIconSizeDp = iconSize.toInt(),
                        topNavIconStyle = topStyle
                    )
                )
                viewModel.triggerNotification("👑 تم حفظ إعدادات الخطوط والأيقونات بنجاح!")
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("حفظ تخصيصات الواجهة والخطوط 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun CardCustomizerPanel(viewModel: MainViewModel, settingsState: AdminSettingsEntity, themeColors: VisualThemePalette, state: AdminPanelState) {
    var coverHeight = state.editCoverHeightState.value
    var avatarSize = state.editAvatarSizeState.value
    var spacing = state.editElementSpacingState.value
    var padding = state.editCardPaddingState.value

    var showVip = state.editShowVipBadgeState.value
    var showVerified = state.editShowVerifiedBadgeState.value
    var showRecommended = state.editShowRecommendedBadgeState.value

    var showCall = state.editShowCallButtonState.value
    var showWhatsapp = state.editShowWhatsappButtonState.value
    var showDetails = state.editShowDetailsButtonState.value
    var showBook = state.editShowBookButtonState.value

    var callBtnColor = state.editCallButtonColorHexState.value
    var whatsappBtnColor = state.editWhatsappButtonColorHexState.value
    var detailsBtnColor = state.editDetailsButtonColorHexState.value
    var bookBtnColor = state.editBookButtonColorHexState.value

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📐 أبعاد ومقاسات بطاقات مقدمي الخدمات:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ارتفاع غلاف البطاقة (Cover Height): (${coverHeight.toInt()} dp)", color = Color.White, fontSize = 10.sp)
                Slider(value = coverHeight, onValueChange = { state.editCoverHeightState.value = it }, valueRange = 0f..200f)

                Text("قطر الصورة الشخصية (Avatar Size): (${avatarSize.toInt()} dp)", color = Color.White, fontSize = 10.sp)
                Slider(value = avatarSize, onValueChange = { state.editAvatarSizeState.value = it }, valueRange = 30f..100f)

                Text("التباعد الداخلي للبطاقة (Padding): (${padding.toInt()} dp)", color = Color.White, fontSize = 10.sp)
                Slider(value = padding, onValueChange = { state.editCardPaddingState.value = it }, valueRange = 4f..24f)

                Text("التباعد بين العناصر الداخلية (Spacing): (${spacing.toInt()} dp)", color = Color.White, fontSize = 10.sp)
                Slider(value = spacing, onValueChange = { state.editElementSpacingState.value = it }, valueRange = 2f..16f)
            }
        }

        Text("🎛️ تفعيل أزرار التفاعل والألوان المخصصة:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                @Composable
                fun ButtonControlRow(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit, colorValue: String, onColorChange: (String) -> Unit) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Switch(checked = isChecked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent))
                        }
                        if (isChecked) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(parseHexColorSafe(colorValue, Color.Gray))
                                )
                                OutlinedTextField(
                                    value = colorValue,
                                    onValueChange = onColorChange,
                                    label = { Text("لون الزر (Hex)", fontSize = 8.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                ButtonControlRow("زر الاتصال الهاتفي (Call Button)", showCall, { state.editShowCallButtonState.value = it }, callBtnColor) { state.editCallButtonColorHexState.value = it }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                ButtonControlRow("زر الواتساب الفوري (WhatsApp Button)", showWhatsapp, { state.editShowWhatsappButtonState.value = it }, whatsappBtnColor) { state.editWhatsappButtonColorHexState.value = it }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                ButtonControlRow("زر تفاصيل الملف الشخصي (Details Button)", showDetails, { state.editShowDetailsButtonState.value = it }, detailsBtnColor) { state.editDetailsButtonColorHexState.value = it }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                ButtonControlRow("زر حجز الخدمة الفوري (Booking Button)", showBook, { state.editShowBookButtonState.value = it }, bookBtnColor) { state.editBookButtonColorHexState.value = it }
            }
        }

        Text("🛡️ عرض شارات التقييم والاعتماد على البطاقة:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("شارة تميز (VIP Badge)", color = Color.White, fontSize = 11.sp)
                    Switch(checked = showVip, onCheckedChange = { state.editShowVipBadgeState.value = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("شارة التوثيق (Verified Badge)", color = Color.White, fontSize = 11.sp)
                    Switch(checked = showVerified, onCheckedChange = { state.editShowVerifiedBadgeState.value = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("شارة التوصية (Recommended Badge)", color = Color.White, fontSize = 11.sp)
                    Switch(checked = showRecommended, onCheckedChange = { state.editShowRecommendedBadgeState.value = it })
                }
            }
        }

        Button(
            onClick = {
                viewModel.saveCustomSettingsState(
                    settingsState.copy(
                        coverHeight = coverHeight.toInt(),
                        avatarSize = avatarSize.toInt(),
                        elementSpacing = spacing.toInt(),
                        cardPadding = padding.toInt(),
                        showCallButton = showCall,
                        showWhatsappButton = showWhatsapp,
                        showDetailsButton = showDetails,
                        showBookButton = showBook,
                        callButtonColorHex = callBtnColor,
                        whatsappButtonColorHex = whatsappBtnColor,
                        detailsButtonColorHex = detailsBtnColor,
                        bookButtonColorHex = bookBtnColor,
                        showVipBadge = showVip,
                        showVerifiedBadge = showVerified,
                        showRecommendedBadge = showRecommended
                    )
                )
                viewModel.triggerNotification("🎛️ تم حفظ إعدادات بطاقات وتفاصيل مقدمي الخدمات بنجاح!")
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("حفظ تخصيصات البطاقات المميزة 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun RegFormsManagerPanel(viewModel: MainViewModel, settingsState: AdminSettingsEntity, themeColors: VisualThemePalette, state: AdminPanelState) {
    val reqNameInput = state.requirementItemInputState.value
    val isMandatoryInput = state.isNewRequirementMandatoryState.value
    var reqsList by remember { mutableStateOf(state.requirementsListStateState.value) }

    var enableProvidersReg by remember(settingsState.enableProvidersRegistration) { mutableStateOf(settingsState.enableProvidersRegistration) }
    var enableStoresReg by remember(settingsState.enableStoresRegistration) { mutableStateOf(settingsState.enableStoresRegistration) }
    var enableRestaurantsReg by remember(settingsState.enableRestaurantsRegistration) { mutableStateOf(settingsState.enableRestaurantsRegistration) }
    var enablePropertiesReg by remember(settingsState.enablePropertiesRegistration) { mutableStateOf(settingsState.enablePropertiesRegistration) }
    var enableMedicalReg by remember(settingsState.enableMedicalRegistration) { mutableStateOf(settingsState.enableMedicalRegistration) }
    var enableJobsReg by remember(settingsState.enableJobsRegistration) { mutableStateOf(settingsState.enableJobsRegistration) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📋 تفعيل استمارات التسجيل للأقسام الرئيسية:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("استمارة تسجيل مقدمي الخدمات / الفنيين", color = Color.White, fontSize = 11.sp)
                    Switch(checked = enableProvidersReg, onCheckedChange = { enableProvidersReg = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("استمارة تسجيل المحلات والمراكز التجارية", color = Color.White, fontSize = 11.sp)
                    Switch(checked = enableStoresReg, onCheckedChange = { enableStoresReg = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("استمارة تسجيل المطاعم والكافيهات", color = Color.White, fontSize = 11.sp)
                    Switch(checked = enableRestaurantsReg, onCheckedChange = { enableRestaurantsReg = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("استمارة إضافة العقارات والأراضي", color = Color.White, fontSize = 11.sp)
                    Switch(checked = enablePropertiesReg, onCheckedChange = { enablePropertiesReg = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("استمارة تسجيل المراكز الطبية والعيادات", color = Color.White, fontSize = 11.sp)
                    Switch(checked = enableMedicalReg, onCheckedChange = { enableMedicalReg = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("استمارة إعلانات الوظائف والتقديم", color = Color.White, fontSize = 11.sp)
                    Switch(checked = enableJobsReg, onCheckedChange = { enableJobsReg = it })
                }
            }
        }

        Text("✏️ تخصيص حقول استمارة تسجيل مقدمي الخدمات:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("إضافة حقل جديد للاستمارة:", color = themeColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = reqNameInput,
                    onValueChange = { state.requirementItemInputState.value = it },
                    placeholder = { Text("مثال: صورة رخصة القيادة المهنية", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الحقل إلزامي لإنهاء التسجيل؟", color = Color.White, fontSize = 10.sp)
                    Switch(checked = isMandatoryInput, onCheckedChange = { state.isNewRequirementMandatoryState.value = it })
                }
                Button(
                    onClick = {
                        if (reqNameInput.isNotBlank()) {
                            val mandatorySuffix = if (isMandatoryInput) "Mandatory" else "Optional"
                            val newItem = "${reqNameInput.trim()}|$mandatorySuffix"
                            if (!reqsList.contains(newItem)) {
                                reqsList = reqsList + newItem
                                state.requirementsListStateState.value = reqsList
                            }
                            state.requirementItemInputState.value = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("إضافة الحقل ➕", color = Color.Black, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("الحقول الحالية المعتمدة في استمارة التسجيل:", color = themeColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                if (reqsList.isEmpty()) {
                    Text("لا توجد حقول مخصصة. سيتم استخدام الحقول التلقائية.", color = Color.LightGray, fontSize = 10.sp)
                } else {
                    reqsList.forEach { req ->
                        val parts = req.split("|")
                        val name = parts.getOrElse(0) { req }
                        val mandatory = parts.getOrElse(1) { "Optional" } == "Mandatory"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(if (mandatory) "إلزامي ⚠️" else "اختياري ✅", color = if (mandatory) Color.Yellow else Color.Green, fontSize = 8.sp)
                                }
                                IconButton(onClick = {
                                    reqsList = reqsList.filter { it != req }
                                    state.requirementsListStateState.value = reqsList
                                }, modifier = Modifier.size(24.dp)) {
                                    Text("🗑️", color = Color.Red, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                viewModel.saveCustomSettingsState(
                    settingsState.copy(
                        registrationRequirements = reqsList.joinToString(","),
                        enableProvidersRegistration = enableProvidersReg,
                        enableStoresRegistration = enableStoresReg,
                        enableRestaurantsRegistration = enableRestaurantsReg,
                        enablePropertiesRegistration = enablePropertiesReg,
                        enableMedicalRegistration = enableMedicalReg,
                        enableJobsRegistration = enableJobsReg
                    )
                )
                viewModel.triggerNotification("📋 تم حفظ تخصيص استمارات التسجيل والتحكم بنجاح!")
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("حفظ استمارات التسجيل المعتمدة 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun NewSectionCreatorPanel(viewModel: MainViewModel, settingsState: AdminSettingsEntity, themeColors: VisualThemePalette, state: AdminPanelState) {
    var sectionName by remember { mutableStateOf("") }
    var sectionIcon by remember { mutableStateOf("") }
    var sectionType by remember { mutableStateOf("store") } 
    var registrationTerms by remember { mutableStateOf("") }
    var requiredFields by remember { mutableStateOf("الاسم,الوصف,الهاتف,الموقع") }
    var maxPhotosVal by remember { mutableStateOf("5") }
    var allowPdfInput by remember { mutableStateOf(true) }

    var sectionsList by remember(settingsState.dynamicSectionsData) {
        mutableStateOf(DynamicSection.parseDynamicSections(settingsState.dynamicSectionsData))
    }

    val storesList by viewModel.stores.collectAsState()
    val propertiesList by viewModel.properties.collectAsState()
    val jobsList by viewModel.jobs.collectAsState()
    val providersList by viewModel.providers.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📊 إحصائيات الأعضاء والنشاط للأقسام الحالية:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val stats = listOf(
                    "المحلات والمراكز" to "${storesList.size} محل تجاري",
                    "العقارات والأراضي" to "${propertiesList.size} عقار معروض",
                    "إعلانات الوظائف" to "${jobsList.size} وظيفة منشورة",
                    "الفنيين والمهن" to "${providersList.size} مقدم خدمات معتمد"
                )
                stats.forEach { (title, count) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(title, color = Color.LightGray, fontSize = 11.sp)
                        Text(count, color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text("➕ إنشاء قسم ديناميكي جديد مخصص:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sectionName,
                    onValueChange = { sectionName = it },
                    label = { Text("اسم القسم الجديد", fontSize = 10.sp) },
                    placeholder = { Text("مثال: خدمات التوصيل والمشاوير", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = sectionIcon,
                    onValueChange = { sectionIcon = it },
                    label = { Text("أيقونة القسم (Emoji)", fontSize = 10.sp) },
                    placeholder = { Text("مثال: 🚗", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    singleLine = true
                )
                
                Text("نوع بيانات القسم (هيكلية العرض):", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val types = listOf("store" to "🏪 محلات تجارية", "property" to "🏠 عقارات وأراضي")
                    types.forEach { (key, label) ->
                        val isSel = sectionType == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) themeColors.accent else themeColors.surface)
                                .clickable { sectionType = key }
                                .padding(vertical = 8.dp)
                                .border(1.dp, if (isSel) Color.White else Color.Transparent, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isSel) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = requiredFields,
                    onValueChange = { requiredFields = it },
                    label = { Text("الحقول المطلوبة في الاستمارة (مفصولة بفاصلة)", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = registrationTerms,
                    onValueChange = { registrationTerms = it },
                    label = { Text("شروط التسجيل والاعتماد للقسم", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = maxPhotosVal,
                        onValueChange = { maxPhotosVal = it },
                        label = { Text("أقصى صور", fontSize = 10.sp) },
                        modifier = Modifier.width(100.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ملف PDF؟", color = Color.White, fontSize = 10.sp)
                        Switch(checked = allowPdfInput, onCheckedChange = { allowPdfInput = it })
                    }
                }

                Button(
                    onClick = {
                        if (sectionName.isNotBlank() && sectionIcon.isNotBlank()) {
                            val newId = "dyn_${System.currentTimeMillis().toString().takeLast(6)}"
                            val newSec = DynamicSection(
                                id = newId,
                                name = sectionName.trim(),
                                icon = sectionIcon.trim(),
                                isEnabled = true,
                                type = sectionType,
                                order = sectionsList.size + 1,
                                terms = registrationTerms.trim(),
                                maxPhotos = maxPhotosVal.toIntOrNull() ?: 5,
                                showPhotos = true,
                                allowPdf = allowPdfInput,
                                requiredFields = requiredFields.trim()
                            )
                            sectionsList = sectionsList + newSec
                            viewModel.saveCustomSettingsState(
                                settingsState.copy(
                                    dynamicSectionsData = DynamicSection.serializeDynamicSections(sectionsList)
                                )
                            )
                            viewModel.triggerNotification("➕ تم إنشاء وتعميد القسم الجديد $sectionName بنجاح سحابياً!")
                            sectionName = ""
                            sectionIcon = ""
                            registrationTerms = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إنشاء واعتماد القسم الجديد فوراً 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Text("📋 إدارة الأقسام الحالية وتعديل حالتها:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        if (sectionsList.isEmpty()) {
            Text("لا توجد أقسام مخصصة حالياً.", color = Color.LightGray, fontSize = 11.sp)
        } else {
            sectionsList.forEach { sec ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(sec.icon, fontSize = 18.sp)
                                Column {
                                    Text(sec.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("النوع: ${if (sec.type == "store") "🏪 دليل محلات" else "🏠 عقارات"} | المعرف: ${sec.id}", color = Color.LightGray, fontSize = 8.sp)
                                }
                            }
                            Switch(
                                checked = sec.isEnabled,
                                onCheckedChange = { isChecked ->
                                    sectionsList = sectionsList.map {
                                        if (it.id == sec.id) it.copy(isEnabled = isChecked) else it
                                    }
                                    viewModel.saveCustomSettingsState(
                                        settingsState.copy(
                                            dynamicSectionsData = DynamicSection.serializeDynamicSections(sectionsList)
                                        )
                                    )
                                    viewModel.triggerNotification("🔧 تم تحديث نشاط القسم ${sec.name} بنجاح!")
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    sectionsList = sectionsList.filter { it.id != sec.id }
                                    viewModel.saveCustomSettingsState(
                                        settingsState.copy(
                                            dynamicSectionsData = DynamicSection.serializeDynamicSections(sectionsList)
                                        )
                                    )
                                    viewModel.triggerNotification("🗑️ تم حذف القسم ${sec.name} بنجاح!")
                                }
                            ) {
                                Text("حذف القسم 🗑️", color = Color.Red, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseHexColorSafe(hex: String, fallback: Color): Color {
    if (hex.isBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}
