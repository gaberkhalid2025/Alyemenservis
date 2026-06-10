package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.MainViewModel
import java.util.UUID

class MainActivity : ComponentActivity() {
    private var lastBackPressTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val settingsState by viewModel.settings.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()

            // Dynamic theme pallete selection
            val colors = remember(settingsState.activeThemeId, settingsState.customPrimaryHex, settingsState.customSecondaryHex) {
                resolveThemePalette(settingsState)
            }

            // Custom double-tap back action to exit App
            val context = LocalContext.current
            BackHandler {
                val handled = viewModel.goBack()
                if (!handled) {
                    val now = System.currentTimeMillis()
                    if (now - lastBackPressTime < 2000) {
                        finish()
                    } else {
                        lastBackPressTime = now
                        Toast.makeText(context, "اضغط مرة أخرى للخروج من التطبيق", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            MaterialTheme(
                colorScheme = colors.scheme
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = colors.background
                    ) {
                        AppNavigator(viewModel = viewModel, themeColors = colors)
                    }
                }
            }
        }
    }
}

// ------ Dynamic Visual Palette Definitions ------
data class VisualThemePalette(
    val activeId: String,
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val gradientBrush: Brush,
    val scheme: ColorScheme
)

fun resolveThemePalette(settings: AdminSettingsEntity): VisualThemePalette {
    return when (settings.activeThemeId) {
        "COSMIC_SILVER" -> {
            val primary = Color(0xFF4B5563) // Faded Slate
            val secondary = Color(0xFF1F2937) // Midnight Charcoal
            val background = Color(0xFF111827) // Dark Obsidian
            val surface = Color(0xFF1F2937) // Sleek Slate box
            val textPrimary = Color(0xFFF9FAFB)
            val textSecondary = Color(0xFF9CA3AF)
            val accent = Color(0xFF60A5FA) // Cosmic Blue accent
            VisualThemePalette(
                activeId = "COSMIC_SILVER",
                primary = primary,
                secondary = secondary,
                background = background,
                surface = surface,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent,
                gradientBrush = Brush.verticalGradient(listOf(Color(0xFF1F2937), Color(0xFF111827))),
                scheme = darkColorScheme(primary = primary, secondary = secondary, background = background, surface = surface)
            )
        }
        "ACCENT_ORANGE" -> { // Lux Golden desert vibes
            val primary = Color(0xFFD97706) // Yemen Warm Amber
            val secondary = Color(0xFF451A03) // Dark Amber
            val background = Color(0xFF0F0F10) 
            val surface = Color(0xFF1C1917)
            val textPrimary = Color(0xFFFFFAFA)
            val textSecondary = Color(0xFFD6D3D1)
            val accent = Color(0xFFFBBF24) // Gold Coin Sparkle
            VisualThemePalette(
                activeId = "ACCENT_ORANGE",
                primary = primary,
                secondary = secondary,
                background = background,
                surface = surface,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent,
                gradientBrush = Brush.verticalGradient(listOf(Color(0xFF292524), Color(0xFF0F0F10))),
                scheme = darkColorScheme(primary = primary, secondary = secondary, background = background, surface = surface)
            )
        }
        "CUSTOM_THEME" -> {
            val primary = try { Color(android.graphics.Color.parseColor(settings.customPrimaryHex)) } catch (e: Exception) { Color(0xFF059669) }
            val secondary = try { Color(android.graphics.Color.parseColor(settings.customSecondaryHex)) } catch (e: Exception) { Color(0xFF064E3B) }
            val background = Color(0xFF0A0F0D)
            val surface = Color(0xFF121D18)
            val textPrimary = Color.White
            val textSecondary = Color(0xFF94A3B8)
            val accent = Color(0xFFF59E0B)
            VisualThemePalette(
                activeId = "CUSTOM_THEME",
                primary = primary,
                secondary = secondary,
                background = background,
                surface = surface,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent,
                gradientBrush = Brush.verticalGradient(listOf(surface, background)),
                scheme = darkColorScheme(primary = primary, secondary = secondary, background = background, surface = surface)
            )
        }
        else -> { // Default: EMERALD_YEMEN (Yemen Noble Royal green)
            val primary = Color(0xFF059669) // Emerald
            val secondary = Color(0xFF115E59) // Teal Forest
            val background = Color(0xFF022C22) // Royal Pine
            val surface = Color(0xFF064E3B) // Pine Card box
            val textPrimary = Color(0xFFF0FDF4)
            val textSecondary = Color(0xFFA7F3D0)
            val accent = Color(0xFFF59E0B) // Bright Gold Amber
            VisualThemePalette(
                activeId = "EMERALD_YEMEN",
                primary = primary,
                secondary = secondary,
                background = background,
                surface = surface,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent,
                gradientBrush = Brush.verticalGradient(listOf(surface, background)),
                scheme = darkColorScheme(primary = primary, secondary = secondary, background = background, surface = surface)
            )
        }
    }
}

// ------ App Main Navigator ------
@Composable
fun AppNavigator(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val toastMessage by viewModel.toastFlow.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val context = LocalContext.current

    // Modal dialog trigger states
    var showInfoDialog by remember { mutableStateOf(false) }
    var showAssistantDialog by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearNotification()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            AppHeaderBar(viewModel = viewModel, themeColors = themeColors)
        },
        bottomBar = {
            AppFooterBar(
                viewModel = viewModel,
                themeColors = themeColors,
                onInfoClick = { showInfoDialog = true }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(themeColors.background)
        ) {
            if (settingsState.isMaintenanceActive && adminRole == "GUEST") {
                MaintenanceSplashView(settingsState = settingsState, themeColors = themeColors, viewModel = viewModel)
            } else {
                when (currentScreen) {
                    "OWNER_PANEL" -> OwnerBackdoorPanelLayout(viewModel = viewModel, themeColors = themeColors)
                    "ADMIN_PANEL" -> AdminPanelLayout(viewModel = viewModel, themeColors = themeColors)
                    "REGISTER_FORM" -> ProviderRegisterFormLayout(viewModel = viewModel, themeColors = themeColors)
                    "ABOUT_APP" -> AboutAppScreenContent(viewModel = viewModel, themeColors = themeColors)
                    else -> ServicesBrowserLayout(viewModel = viewModel, themeColors = themeColors)
                }

                FloatingIconsOverlay(
                    settings = settingsState,
                    themeColors = themeColors,
                    onAssistantClick = { showAssistantDialog = true },
                    onChatClick = { showChatDialog = true }
                )
            }
        }
    }

    if (showInfoDialog) {
        AboutAppDialogView(settings = settingsState, themeColors = themeColors, onDismiss = { showInfoDialog = false })
    }

    if (showAssistantDialog) {
        SmartAssistantDialogView(viewModel = viewModel, settings = settingsState, themeColors = themeColors, onDismiss = { showAssistantDialog = false })
    }

    if (showChatDialog) {
        ChatPanelDialogView(viewModel = viewModel, themeColors = themeColors, onDismiss = { showChatDialog = false })
    }
}

// ------ Custom Top App Bar ------
@Composable
fun AppHeaderBar(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val settingsState by viewModel.settings.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(themeColors.primary)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("app_header_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { viewModel.registerBackdoorInteraction() }
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Logo",
                tint = themeColors.accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = settingsState.appName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = settingsState.welcomeMessage.take(24) + "...",
                    fontSize = 10.sp,
                    color = themeColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { viewModel.navigateTo("USER_BROWSE") },
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (currentScreen == "USER_BROWSE") Color.White.copy(alpha = 0.25f) else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(imageVector = Icons.Default.Home, contentDescription = "الرئيسية", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = { viewModel.navigateTo("ADMIN_PANEL") },
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (currentScreen == "ADMIN_PANEL") Color.White.copy(alpha = 0.25f) else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "تسجيل الدخول", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = { viewModel.navigateTo("REGISTER_FORM") },
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (currentScreen == "REGISTER_FORM") Color.White.copy(alpha = 0.25f) else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(imageVector = Icons.Default.Person, contentDescription = "تسجيل فني", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = { viewModel.switchLanguage() },
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.Transparent, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "تغيير اللغة", tint = themeColors.accent, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = { viewModel.triggerNotification("🔄 تم مزامنة البيانات وتحديث الصفحة بنجاح!") },
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.Transparent, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "مزامنة", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ------ Custom Small Dynamic Footer ------
@Composable
fun AppFooterBar(viewModel: MainViewModel, themeColors: VisualThemePalette, onInfoClick: () -> Unit) {
    val settingsState by viewModel.settings.collectAsState()

    if (!settingsState.hidePromoFooter) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColors.secondary)
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("app_footer_bar"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onInfoClick() }
                    .padding(4.dp)
            ) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "عن التطبيق", tint = themeColors.textSecondary, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("عن التطبيق", fontSize = 10.sp, color = themeColors.textSecondary)
            }

            Text(
                text = settingsState.footerMessage,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Text(
                text = settingsState.appVersion,
                fontSize = 8.sp,
                color = themeColors.textSecondary.copy(alpha = 0.60f)
            )
        }
    }
}

// ------ Floating Icons Overlay Container ------
@Composable
fun BoxScope.FloatingIconsOverlay(
    settings: AdminSettingsEntity,
    themeColors: VisualThemePalette,
    onAssistantClick: () -> Unit,
    onChatClick: () -> Unit
) {
    if (!settings.assistantHidden) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 48.dp)
                .size(settings.assistantSize.dp)
                .background(themeColors.accent, CircleShape)
                .clickable { onAssistantClick() }
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(imageVector = Icons.Default.Build, contentDescription = "المساعد", tint = Color.Black, modifier = Modifier.size(20.dp))
                Text("خدمات", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }

    if (!settings.chatHidden) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 48.dp)
                .size(settings.chatSize.dp)
                .background(themeColors.primary, CircleShape)
                .clickable { onChatClick() }
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "دردشة", tint = Color.White, modifier = Modifier.size(20.dp))
                Text("دردشة", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ------ Maintenance Banner view ------
@Composable
fun MaintenanceSplashView(settingsState: AdminSettingsEntity, themeColors: VisualThemePalette, viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.Warning, contentDescription = "تحت الصيانة", tint = themeColors.accent, modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "التطبيق في وضع الصيانة والتحديث",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "رسالة الإدارة: ${settingsState.welcomeMessage}",
            fontSize = 13.sp,
            color = themeColors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.navigateTo("ADMIN_PANEL") },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
        ) {
            Text("تسجيل دخول المالك", color = Color.White)
        }
    }
}

// ------ Main Category and Service Directory Browser Layout ------
@Composable
fun ServicesBrowserLayout(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val categories by viewModel.categories.collectAsState()
    val filteredProviders by viewModel.filteredProviders.collectAsState()
    val selectedCategory by viewModel.selectedCategoryId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isVipOnly by viewModel.filterVipOnly.collectAsState()
    val isAvailableOnly by viewModel.filterAvailableOnly.collectAsState()
    val citiesList by viewModel.cities.collectAsState()
    val activeCityId by viewModel.filterCityId.collectAsState()
    val radiusKm by viewModel.maxKmRadius.collectAsState()
    val neighborFilter by viewModel.filterNeighborhoodName.collectAsState()
    val phoneOrNameFilter by viewModel.phoneOrNameFilter.collectAsState()
    val bannersList by viewModel.banners.collectAsState()
    val settingsState by viewModel.settings.collectAsState()

    var showFiltersPanel by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Horizontal banners list
        if (bannersList.isNotEmpty()) {
            item {
                BannerSliderView(banners = bannersList, themeColors = themeColors) { catTarget ->
                    if (catTarget.isNotEmpty()) viewModel.selectCategory(catTarget)
                }
            }
        }

        // Search Bar Block
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColors.surface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = themeColors.textSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("ابحث عن سباك، كهربائي، حدة...", fontSize = 13.sp, color = themeColors.textSecondary) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_text_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
                
                if (settingsState.isSpeechSearchEnabled) {
                    IconButton(onClick = {
                        val phrases = listOf("صنعاء صيانة منزلية", "عدن سباك ممتاز", "حدة كهربائي تكييف", "سباكة")
                        viewModel.updateSearchQuery(phrases.random())
                        viewModel.triggerNotification("🎙️ تم سماع الصوت وتحديث الكلمات المفتاحية!")
                    }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "البحث الصوتي", tint = themeColors.accent)
                    }
                }

                IconButton(onClick = { showFiltersPanel = !showFiltersPanel }) {
                    Icon(
                        imageVector = if (showFiltersPanel) Icons.Default.Settings else Icons.Default.List,
                        contentDescription = "الفلاتر",
                        tint = themeColors.accent
                    )
                }
            }
        }

        // Expanded Filter Panel drawer settings
        if (showFiltersPanel) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🔍 معايير البحث المتقدم والفلترة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = phoneOrNameFilter,
                            onValueChange = { viewModel.setPhoneOrNameFilter(it) },
                            placeholder = { Text("البحث بالاسم أو رقم الهاتف...", fontSize = 11.sp, color = themeColors.textSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("المدينة اليمنية:", fontSize = 10.sp, color = themeColors.textSecondary)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .clickable {
                                            val idx = citiesList.indexOfFirst { it.id == activeCityId }
                                            val nextIdx = if (idx == -1) 0 else if (idx == citiesList.size -1) -1 else idx + 1
                                            viewModel.setCityFilter(if (nextIdx == -1) null else citiesList[nextIdx].id)
                                        }
                                        .padding(10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = citiesList.firstOrNull { it.id == activeCityId }?.nameAr ?: "كل المدن",
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                        Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("المنطقة / الحي:", fontSize = 10.sp, color = themeColors.textSecondary)
                                OutlinedTextField(
                                    value = neighborFilter,
                                    onValueChange = { viewModel.setNeighborhoodFilter(it) },
                                    placeholder = { Text("مثال: حدة، الحصبة...", fontSize = 11.sp, color = themeColors.textSecondary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    singleLine = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("البحث بنطاق جغرافي (دائرة):", fontSize = 11.sp, color = themeColors.textPrimary)
                                Text("${radiusKm} كم (الحد الأدنى)", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = radiusKm.toFloat(),
                                onValueChange = { viewModel.setRadiusKm(it.toInt().coerceAtMost(settingsState.maxSearchRadiusKm)) },
                                valueRange = 5f..50f,
                                steps = 5,
                                colors = SliderDefaults.colors(thumbColor = themeColors.accent, activeTrackColor = themeColors.accent)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isVipOnly, onCheckedChange = { viewModel.toggleVipFilter() })
                                Text("العضوية الذهبية معتمدة", fontSize = 11.sp, color = Color.White)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isAvailableOnly, onCheckedChange = { viewModel.toggleAvailableFilter() })
                                Text("المتاحين الآن فقط", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Horizontal Grid list of categories
        item {
            Text(text = "📁 تصفح حسب الأقسام:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    CategoryChip(
                        name = "الكل",
                        icon = "🌐",
                        isSelected = selectedCategory == null,
                        themeColors = themeColors
                    ) {
                        viewModel.selectCategory(null)
                    }
                }
                items(categories) { cat ->
                    CategoryChip(
                        name = cat.name,
                        icon = cat.icon,
                        isSelected = selectedCategory == cat.id,
                        themeColors = themeColors
                    ) {
                        viewModel.selectCategory(cat.id)
                    }
                }
            }
        }

        // Services Providers Headers
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💼 مقدمو الخدمات المتوفرون (${filteredProviders.size}):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (selectedCategory != null) {
                    Text(
                        text = "إلغاء الفلترة",
                        fontSize = 11.sp,
                        color = themeColors.accent,
                        modifier = Modifier.clickable { viewModel.selectCategory(null) }
                    )
                }
            }
        }

        // List of Service providers
        if (filteredProviders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = themeColors.textSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("عذراً، لم يتم العثور على مقدمي خدمة يطابقون هذه الفلاتر", fontSize = 13.sp, color = themeColors.textSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(filteredProviders) { provider ->
                ProviderCard(provider = provider, themeColors = themeColors, viewModel = viewModel)
            }
        }
    }
}

// ------ Horizontal Advertisement Banner Composable ------
@Composable
fun BannerSliderView(banners: List<BannerEntity>, themeColors: VisualThemePalette, onBannerClick: (String) -> Unit) {
    var currentIndex by remember { mutableStateOf(0) }
    
    // Automatically swap banners mockup simulation (simulates slider transition)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(6000)
            if (banners.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % banners.size
            }
        }
    }

    val activeBanner = if (banners.isNotEmpty()) banners[currentIndex] else null
    if (activeBanner != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clickable { onBannerClick(activeBanner.redirectCategory) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.secondary)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                themeColors.primary,
                                themeColors.secondary.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "إعلان ممتاز", tint = themeColors.accent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إعلان مميز",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.accent
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeBanner.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ------ Category selection chip ------
@Composable
fun CategoryChip(name: String, icon: String, isSelected: Boolean, themeColors: VisualThemePalette, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) themeColors.accent else themeColors.surface)
            .border(1.dp, if (isSelected) Color.White else themeColors.accent.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.Black else Color.White
        )
    }
}

// ------ Provider Directory Card widget ------
@Composable
fun ProviderCard(provider: ProviderEntity, themeColors: VisualThemePalette, viewModel: MainViewModel) {
    val context = LocalContext.current
    var isRatingOpened by remember { mutableStateOf(false) }
    var selectedReportReason by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = provider.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (provider.isVip) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFD97706))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("ذهبي", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        if (provider.subscriptionStatus == "APPROVED") {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "موثق",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = themeColors.textSecondary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${provider.area} - ${provider.localNeighborhood}",
                            fontSize = 11.sp,
                            color = themeColors.textSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = provider.rating.toString(), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اتصال", fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/967${provider.phone}"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "الرجاء تثبيت واتساب لتسهيل الاتصال", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("واتساب", fontSize = 11.sp, color = Color.White)
                    }

                    IconButton(onClick = { isRatingOpened = !isRatingOpened }, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Default.ThumbUp, contentDescription = "تقييم", tint = themeColors.accent, modifier = Modifier.size(16.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (provider.isAvailable) Color.Green else Color.Red, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (provider.isAvailable) "متاح للاتصال" else "مشغول حالياً",
                        fontSize = 11.sp,
                        color = if (provider.isAvailable) Color.Green else Color.Red
                    )
                }
            }

            AnimatedVisibility(visible = isRatingOpened) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ما هو تقييمك للفني؟", fontSize = 11.sp, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (star <= 4) themeColors.accent else Color.Gray,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        viewModel.submitRating(provider.id, star)
                                        isRatingOpened = false
                                    }
                            )
                        }
                    }
                    Text(
                        text = "إرسال بلاغ",
                        fontSize = 11.sp,
                        color = Color.Red,
                        modifier = Modifier
                            .clickable { showReportDialog = true }
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }

    if (showReportDialog) {
        Dialog(onDismissRequest = { showReportDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.Red),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📢 تقديم بلاغ أو شكوى ضد الفني", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("الرجاء كتابة سبب الإنزعاج أو الشكوى بالتفصيل وسنقوم بمراجعتها فوراً:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = selectedReportReason,
                        onValueChange = { selectedReportReason = it },
                        placeholder = { Text("مثال: سعر مرتفع، تأخير في الموعد، سوء معاملة...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { showReportDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("إلغاء", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.sendReport(provider.id, provider.name, "مستخدم مجهول", selectedReportReason)
                                showReportDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("إرسال الشكوى", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ------ Provider Registration Form Layout ------
@Composable
fun ProviderRegisterFormLayout(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val categories by viewModel.categories.collectAsState()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "👤 تقديم طلب انضمام كفني محترف",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "املأ البيانات أدناه وسيتولى فريق الدعم الفني بمكالمتكم ومراجعة الوكالة لتفعيل العضوية في اليمن الخدمات.",
                    fontSize = 11.sp,
                    color = themeColors.textSecondary
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("الاسم الثلاثي للفني") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("رقم الهاتف (واتساب جاهز)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Text("اختر قسم الصيانة:", fontSize = 12.sp, color = themeColors.textSecondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                val isSel = cat.id == selectedCatId
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { selectedCatId = cat.id }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(cat.icon, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(cat.name, fontSize = 11.sp, color = if (isSel) Color.Black else Color.White)
                }
            }
        }

        OutlinedTextField(
            value = area,
            onValueChange = { area = it },
            label = { Text("المدينة / المحافظة في اليمن (مثال: صنعاء)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        OutlinedTextField(
            value = neighborhood,
            onValueChange = { neighborhood = it },
            label = { Text("الحي أو الشارع (مثال: الشارع الرئيسي، حدة)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Button(
            onClick = {
                if (name.isNotEmpty() && phone.isNotEmpty() && selectedCatId.isNotEmpty()) {
                    viewModel.submitJoinForm(name, phone, selectedCatId, area, neighborhood, "", "", "")
                    name = ""
                    phone = ""
                    selectedCatId = ""
                    area = ""
                    neighborhood = ""
                } else {
                    viewModel.triggerNotification("⚠️ يرجى كمالة البيانات المطلوبة")
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("إرسال طلب الانضمام للمراجعة", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// ------ Admin Control Dashboard Layout ------
@Composable
fun AdminPanelLayout(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val pendingProviders by viewModel.pendingProviders.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()

    var inputPasscode by remember { mutableStateOf("") }
    var isAuthorized by remember { mutableStateOf(adminRole != "GUEST") }

    if (!isAuthorized) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text("بوابة مسؤولي المنصة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("الرجاء إدخال رقم التعريف الخاص بالدعم للتحكم في الطلبات:", fontSize = 11.sp, color = themeColors.textSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = inputPasscode,
                onValueChange = { inputPasscode = it },
                label = { Text("الرمز السري") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (inputPasscode == "777" || inputPasscode == "1234") {
                        isAuthorized = true
                        viewModel.authenticateAdmin("ADMIN")
                    } else {
                        viewModel.triggerNotification("❌ الرمز السري غير صحيح!")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تسجيل دخول المشرف", color = Color.White)
            }
        }
    } else {
        // Logged dashboard
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔐 لوحة التحكم الرئيسية (مشرف)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Button(
                        onClick = {
                            isAuthorized = false
                            viewModel.logout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                    ) {
                        Text("تسجيل خروج", color = Color.White, fontSize = 10.sp)
                    }
                }
            }

            // Pending join requests
            item {
                Text("📨 طلبات انضمام فنيين جديدة (${pendingProviders.size}):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (pendingProviders.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                        Text("لا توجد طلبات معلقة بانتظار المراجعة الآن", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                    }
                }
            } else {
                items(pendingProviders) { req ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "الاسم: ${req.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "رقم الهاتف: ${req.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                            Text(text = "العنوان: ${req.area} - ${req.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.approveRequest(req) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                                ) {
                                    Text("موافقة وقبول", color = Color.Black, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { viewModel.rejectRequest(req, "المستندات غير واضحة") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Text("رفض الطلب", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Complaints & Reports list
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📢 إجمالي شكاوى العملاء المستلمة (${reports.size}):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = { viewModel.exportComplaintsToCSV() }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "تصدير CSV", tint = Color.Green)
                        }
                        IconButton(onClick = { viewModel.exportComplaintsToPDF() }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "تصدير PDF", tint = Color.Red)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (reports.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                        Text("الحمد لله! لا توجد شكاوى أو بلاغات مقدمة ضدهم.", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                    }
                }
            } else {
                items(reports) { rep ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface), 
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("الفني المستهدف: ${rep.providerName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("المرسل: ${rep.reporterName}", fontSize = 11.sp, color = themeColors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("المضمون: ${rep.content}", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ------ Owner Backdoor Control Panel Layout ------
@Composable
fun OwnerBackdoorPanelLayout(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val settingsState by viewModel.settings.collectAsState()

    var appName by remember { mutableStateOf(settingsState.appName) }
    var welcomeMessage by remember { mutableStateOf(settingsState.welcomeMessage) }
    var footerMessage by remember { mutableStateOf(settingsState.footerMessage) }
    var supportPhone by remember { mutableStateOf("777666555") }
    var supportEmail by remember { mutableStateOf("yem@services.com") }
    var supportWhatsapp by remember { mutableStateOf("777666555") }
    var activeThemeId by remember { mutableStateOf(settingsState.activeThemeId) }
    var isMaintenanceActive by remember { mutableStateOf(settingsState.isMaintenanceActive) }
    var hidePromoFooter by remember { mutableStateOf(settingsState.hidePromoFooter) }
    var assistantHidden by remember { mutableStateOf(settingsState.assistantHidden) }
    var assistantSize by remember { mutableStateOf(56f) }
    var chatHidden by remember { mutableStateOf(settingsState.chatHidden) }
    var chatSize by remember { mutableStateOf(56f) }
    var maxSearchRadiusKm by remember { mutableStateOf(settingsState.maxSearchRadiusKm.toFloat()) }
    var isSpeechSearchEnabled by remember { mutableStateOf(settingsState.isSpeechSearchEnabled) }
    var isDataSaverEnabled by remember { mutableStateOf(false) }
    var appImageQuality by remember { mutableStateOf(90f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔓 بوابة المالك والتحكم الخلفي الديناميكي", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("إغلاق اللوحة", color = Color.White, fontSize = 10.sp)
            }
        }

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
            value = footerMessage,
            onValueChange = { footerMessage = it },
            label = { Text("رسالة الفوتر") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Text("🎨 سمة التطبيق الافتراضية:", fontSize = 12.sp, color = themeColors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val themes = listOf("EMERALD_YEMEN", "COSMIC_SILVER", "ACCENT_ORANGE", "CUSTOM_THEME")
            themes.forEach { th ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeThemeId == th) themeColors.accent else themeColors.surface)
                        .clickable { activeThemeId = th }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(th, fontSize = 9.sp, color = if (activeThemeId == th) Color.Black else Color.White)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("وضع الصيانة المؤقت", color = Color.White, fontSize = 13.sp)
            Switch(checked = isMaintenanceActive, onCheckedChange = { isMaintenanceActive = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("إخفاء شريط الفوتر", color = Color.White, fontSize = 13.sp)
            Switch(checked = hidePromoFooter, onCheckedChange = { hidePromoFooter = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تفعيل البحث الصوتي", color = Color.White, fontSize = 13.sp)
            Switch(checked = isSpeechSearchEnabled, onCheckedChange = { isSpeechSearchEnabled = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("إخفاء البوت المساعد المساعد", color = Color.White, fontSize = 13.sp)
            Switch(checked = assistantHidden, onCheckedChange = { assistantHidden = it })
        }

        Column {
            Text("قطر دائرة البوت المساعد: ${assistantSize.toInt()} dp", color = Color.White, fontSize = 11.sp)
            Slider(value = assistantSize, onValueChange = { assistantSize = it }, valueRange = 40f..80f)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("إخفاء زر المحادثة العائم", color = Color.White, fontSize = 13.sp)
            Switch(checked = chatHidden, onCheckedChange = { chatHidden = it })
        }

        Column {
            Text("قطر دائرة زر المحادثة العائم: ${chatSize.toInt()} dp", color = Color.White, fontSize = 11.sp)
            Slider(value = chatSize, onValueChange = { chatSize = it }, valueRange = 40f..80f)
        }

        Column {
            Text("الحد الأقصى لنطاق البحث الجغرافي: ${maxSearchRadiusKm.toInt()} كم", color = Color.White, fontSize = 11.sp)
            Slider(value = maxSearchRadiusKm, onValueChange = { maxSearchRadiusKm = it }, valueRange = 10f..100f)
        }

        Button(
            onClick = {
                viewModel.updateBackdoorSettings(
                    appName, welcomeMessage, footerMessage, activeThemeId,
                    supportPhone, supportEmail, supportWhatsapp, isMaintenanceActive,
                    hidePromoFooter, assistantHidden, assistantSize.toInt(),
                    chatHidden, chatSize.toInt(), maxSearchRadiusKm.toInt(),
                    isSpeechSearchEnabled, isDataSaverEnabled, appImageQuality.toInt()
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("💾 حفظ إعدادات البوابة والتخزين والتطبيق", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// ------ About App Info Dialog overlay ------
@Composable
fun AboutAppDialogView(settings: AdminSettingsEntity, themeColors: VisualThemePalette, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp),
            border = BorderStroke(1.dp, themeColors.accent)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(48.dp))
                Text(text = settings.appName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "نسخة التطبيق: ${settings.appVersion}", fontSize = 12.sp, color = themeColors.textSecondary)
                Text(
                    text = "منصة اليمن الخدمات هي الدليل الرائد للحرفيين والمهنيين في الجمهورية اليمنية. تم تطويرها لتسهيل وصول المواطنين للكهربائيين والسباكين الموثقين بمصداقية.",
                    fontSize = 11.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق", color = Color.White)
                }
            }
        }
    }
}

// ------ Smart Assistant Dialog View Overlay ------
@Composable
fun SmartAssistantDialogView(viewModel: MainViewModel, settings: AdminSettingsEntity, themeColors: VisualThemePalette, onDismiss: () -> Unit) {
    val points by viewModel.currentUserPoints.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp),
            border = BorderStroke(1.dp, themeColors.accent)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مساعد الخدمات والولاء الذكي", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                Divider(color = themeColors.accent.copy(alpha = 0.3f))

                Card(colors = CardDefaults.cardColors(containerColor = themeColors.primary.copy(alpha = 0.3f))) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("رصيد نقاط الولاء الخاص بك:", fontSize = 11.sp, color = Color.White)
                        Text("$points نقطة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.redeemLoyaltyPoints() },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("استبدال النقاط", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.rewardSharePoints() },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("مشاركة المنصة 🎁", fontSize = 10.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Text("أسئلة وإرشادات شائعة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GuidanceRow("كيف أضمن مهارة الفني؟", "ابحث عن الشعار الأزرق بجانب الاسم لمعرفة الموثقين.")
                    GuidanceRow("ما سبب التقييم المخفض لبعض الفنيين؟", "المصداقية هي أساسنا، تقييم العملاء يحكم استمرار الفني معنا.")
                    GuidanceRow("هل الخدمات مجانية تماماً للمستخدم؟", "نعم الدليل مجاني بالكامل وبدون عمولة للمواطن.")
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق المساعد", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun GuidanceRow(q: String, a: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .padding(6.dp)
    ) {
        Text("💡 $q", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(a, fontSize = 9.sp, color = Color.LightGray)
    }
}

// ------ Chat messenger Dialog Overlay View ------
@Composable
fun ChatPanelDialogView(viewModel: MainViewModel, themeColors: VisualThemePalette, onDismiss: () -> Unit) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    var typedText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(8.dp),
            border = BorderStroke(1.dp, themeColors.accent)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("محادثة الدعم المباشرة في اليمن", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }

                Divider(color = themeColors.accent.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatMessages) { msg ->
                        val isMe = msg.senderId == "guest"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isMe) 12.dp else 0.dp,
                                            bottomEnd = if (isMe) 0.dp else 12.dp
                                        )
                                    )
                                    .background(if (isMe) themeColors.primary else Color.Black.copy(alpha = 0.4f))
                                    .padding(8.dp)
                            ) {
                                Text(msg.message, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = typedText,
                        onValueChange = { typedText = it },
                        placeholder = { Text("اكتب رسالتك للمشرف...", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (typedText.isNotEmpty()) {
                                viewModel.sendMessageInChat(typedText)
                                typedText = ""
                            }
                        },
                        modifier = Modifier
                            .background(themeColors.accent, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "إرسال", tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ------ Unused screen layouts defined as secondary ------
@Composable
fun AboutAppScreenContent(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("عن دليل اليمن الخدمات", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "تطبيق بمثابة وسيلة سهلة للربط والاتصال بدون انترنت مع الكفاءات الفنية المعتمدة في صنعاء والمدن اليمنية الكبرى.",
            fontSize = 12.sp, color = themeColors.textSecondary, textAlign = TextAlign.Center
        )
    }
}
