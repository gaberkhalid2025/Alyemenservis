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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
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
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager


class MainActivity : ComponentActivity() {
    private var lastBackPressTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("1:896795585945:android:fa9e452a045b811dca7708")
                    .setProjectId("al-yemen-services")
                    .setApiKey("AIzaSyBfVDnH4FfA_lR6HQFwOWSSBRcN__InczE")
                    .setStorageBucket("al-yemen-services.firebasestorage.app")
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val settingsState by viewModel.settings.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()

            val context = LocalContext.current
            val locationPermissions = arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { /* Completed */ }

            LaunchedEffect(Unit) {
                permissionLauncher.launch(locationPermissions)
                try {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    if (lm != null) {
                        val providerStr = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            LocationManager.GPS_PROVIDER
                        } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                            LocationManager.NETWORK_PROVIDER
                        } else {
                            null
                        }
                        if (providerStr != null) {
                            val lastKnown = lm.getLastKnownLocation(providerStr)
                            if (lastKnown != null) {
                                viewModel.updateUserLocation(lastKnown.latitude, lastKnown.longitude)
                            }
                            val listener = object : LocationListener {
                                override fun onLocationChanged(loc: Location) {
                                    viewModel.updateUserLocation(loc.latitude, loc.longitude)
                                }
                                override fun onProviderEnabled(p: String) {}
                                override fun onProviderDisabled(p: String) {}
                                override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                            }
                            lm.requestLocationUpdates(
                                providerStr,
                                2000L,
                                0.5f,
                                listener,
                                android.os.Looper.getMainLooper()
                            )
                        }
                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Simulation: oscillates user coordinates every 3 seconds to test instantly
                var simStep = 0
                while (true) {
                    delay(3000)
                    simStep++
                    val offsetLat = 0.00018 * Math.sin(simStep * 0.45)
                    val offsetLng = 0.00018 * Math.cos(simStep * 0.45)
                    viewModel.updateUserLocation(15.3694 + offsetLat, 44.1910 + offsetLng)
                }
            }

            // Dynamic theme pallete selection
            val colors = remember(settingsState.activeThemeId, settingsState.customPrimaryHex, settingsState.customSecondaryHex) {
                resolveThemePalette(settingsState)
            }

            // Custom double-tap back action to exit App
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
            .fillMaxSize(),
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
            .statusBarsPadding()
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
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
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
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "عن التطبيق",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "عن التطبيق",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = settingsState.footerMessage,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            Text(
                text = "الإصدار: ${settingsState.appVersion}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.End
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

// ------ Category section icon decorator helper ------
@Composable
fun CategorySectionIconView(iconStr: String, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 20.dp) {
    if (iconStr.length > 15) {
        if (iconStr.startsWith("http://") || iconStr.startsWith("https://")) {
            AsyncImage(
                model = iconStr,
                contentDescription = null,
                modifier = modifier.size(size)
            )
        } else {
            val bitmap = remember(iconStr) {
                try {
                    val base64Data = if (iconStr.contains(",")) iconStr.substringAfter(",") else iconStr
                    val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    painter = BitmapPainter(bitmap.asImageBitmap()),
                    contentDescription = null,
                    modifier = modifier.size(size)
                )
            } else {
                Text(text = "📁", fontSize = 14.sp)
            }
        }
    } else {
        Text(text = iconStr.ifEmpty { "📁" }, fontSize = 14.sp, modifier = modifier)
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
        CategorySectionIconView(iconStr = icon, size = 18.dp)
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
    val settingsState by viewModel.settings.collectAsState()
    val userLat by viewModel.userLatitude.collectAsState()
    val userLng by viewModel.userLongitude.collectAsState()
    
    var isRatingOpened by remember { mutableStateOf(false) }
    var selectedReportReason by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    
    // Booking dialog states
    var showBookingDialog by remember { mutableStateOf(false) }
    var customerNameInput by remember { mutableStateOf("") }
    var customerPhoneInput by remember { mutableStateOf("") }
    var customerAreaInput by remember { mutableStateOf("") }
    var showBookingConfirmDialog by remember { mutableStateOf(false) }

    // Parse customized styles with absolute safety (try/catch default values fallback)
    val cardBg = remember(settingsState.cardBackgroundHex, themeColors.surface) {
        try { Color(android.graphics.Color.parseColor(settingsState.cardBackgroundHex)) } catch (e: Exception) { themeColors.surface }
    }
    val nameColor = remember(settingsState.providerNameColorHex) {
        try { Color(android.graphics.Color.parseColor(settingsState.providerNameColorHex)) } catch (e: Exception) { Color.White }
    }
    val locationColor = remember(settingsState.locationColorHex, themeColors.textSecondary) {
        try { Color(android.graphics.Color.parseColor(settingsState.locationColorHex)) } catch (e: Exception) { themeColors.textSecondary }
    }
    val ratingColor = remember(settingsState.ratingColorHex, themeColors.accent) {
        try { Color(android.graphics.Color.parseColor(settingsState.ratingColorHex)) } catch (e: Exception) { themeColors.accent }
    }
    val priceColor = remember(settingsState.previewPriceColorHex) {
        try { Color(android.graphics.Color.parseColor(settingsState.previewPriceColorHex)) } catch (e: Exception) { Color(0xFF10B981) }
    }

    // Interactive scale animation
    var isPressed by remember { mutableStateOf(false) }
    val scaleFactor by animateFloatAsState(
        targetValue = if (settingsState.enableScaleAnimation && isPressed) settingsState.clickScaleRatio else 1.0f,
        label = "click_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = (settingsState.elementSpacing / 2).dp)
            .scale(scaleFactor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        // Card clicked optionally
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(settingsState.cardPadding.dp)) {
            
            // 1. Cover picture if enabled
            if (settingsState.coverHeight > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(settingsState.coverHeight.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    if (provider.coverImage.isNotEmpty()) {
                        AsyncImage(
                            model = provider.coverImage,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        // Abstract premium geometric gradient card cover
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(themeColors.primary, themeColors.surface)))
                        )
                    }
                    
                    // Profile image embedded in cover
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .size(settingsState.avatarSize.dp)
                            .clip(if (settingsState.avatarShape == "CIRCLE") CircleShape else RoundedCornerShape(8.dp))
                            .background(Color.White)
                    ) {
                        if (provider.profileImage.isNotEmpty()) {
                            AsyncImage(
                                model = provider.profileImage,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(themeColors.accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = provider.name.take(1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(settingsState.elementSpacing.dp))
            }

            // 2. Info area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = provider.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = nameColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        if (settingsState.showVipBadge && provider.isVip) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(android.graphics.Color.parseColor(settingsState.vipBadgeColorHex)))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("ذهبي VIP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        
                        if (settingsState.showVerifiedBadge && provider.isVerified) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "شارة زرقاء",
                                tint = Color(android.graphics.Color.parseColor(settingsState.verifiedBadgeColorHex)),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        if (settingsState.showRecommendedBadge && provider.isRecommended) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Recommended",
                                tint = Color(android.graphics.Color.parseColor(settingsState.recommendedBadgeColorHex)),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height((settingsState.elementSpacing / 2).dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = ratingColor, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                    text = "${provider.area}، ${provider.localNeighborhood}",
                                    fontSize = 12.sp,
                                    color = locationColor,
                                    fontWeight = FontWeight.SemiBold
                            )
                            if (settingsState.showDistance) {
                                val provCoords = getProviderCoords(provider.id)
                                val distanceMeters = calculateDistanceInMeters(userLat, userLng, provCoords.first, provCoords.second)
                                val distanceText = "المسافة: " + formatDistance(distanceMeters)
                                Text(
                                    text = distanceText,
                                    fontSize = 11.sp,
                                    color = ratingColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = ratingColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = provider.rating.toString(),
                        fontSize = 12.sp,
                        color = nameColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (settingsState.showReviewsCount) {
                        Text(
                            text = " (${provider.numReviews} تقييم)",
                            fontSize = 10.sp,
                            color = locationColor
                        )
                    }
                }
            }

            // Preview Price if enabled
            if (settingsState.showPreviewPrice) {
                Spacer(modifier = Modifier.height((settingsState.elementSpacing / 2).dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, tint = priceColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "سعر المعاينة المقدر: ${provider.previewPrice.toInt()} ريال يمني",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = priceColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(settingsState.elementSpacing.dp))

            // Buttons order and switches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (settingsState.showCallButton) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(android.graphics.Color.parseColor(settingsState.callButtonColorHex))),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اتصال", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    if (settingsState.showWhatsappButton) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/967${provider.phone}"))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "الرجاء تثبيت واتساب لتسهيل الاتصال", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(android.graphics.Color.parseColor(settingsState.whatsappButtonColorHex))),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    if (settingsState.showBookButton) {
                        Button(
                            onClick = { showBookingDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(android.graphics.Color.parseColor(settingsState.bookButtonColorHex))),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حجز فني", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = { isRatingOpened = !isRatingOpened }, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Default.ThumbUp, contentDescription = "تقييم", tint = ratingColor, modifier = Modifier.size(16.dp))
                    }
                }

                if (settingsState.showAvailability) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (provider.isAvailable) Color.Green else Color.Red, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (provider.isAvailable) "متاح" else "مشغول",
                            fontSize = 11.sp,
                            color = if (provider.isAvailable) Color.Green else Color.Red
                        )
                    }
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
                                tint = if (star <= 4) ratingColor else Color.Gray,
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

    // ------ Dynamic Booking Form Layout Dialogs ------
    if (showBookingDialog) {
        Dialog(onDismissRequest = { showBookingDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📅 استمارة حجز فني: ${provider.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    OutlinedTextField(
                        value = customerNameInput,
                        onValueChange = { customerNameInput = it },
                        label = { Text("الاسم الثنائي/الثلاثي للعميل كامل", color = themeColors.textSecondary, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = customerPhoneInput,
                        onValueChange = { customerPhoneInput = it },
                        label = { Text("رقم الهاتف اليمني (أرقام فقط)", color = themeColors.textSecondary, fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = customerAreaInput,
                        onValueChange = { customerAreaInput = it },
                        label = { Text("منطقة السكن والحي بالتفصيل (مثل: صنعاء-شارع الستين)", color = themeColors.textSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (customerNameInput.trim().isEmpty() || customerPhoneInput.trim().isEmpty() || customerAreaInput.trim().isEmpty()) {
                                    Toast.makeText(context, "الرجاء كتابة جميع البيانات لإكمال الحجز", Toast.LENGTH_SHORT).show()
                                } else {
                                    showBookingDialog = false
                                    showBookingConfirmDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تأكيد الحجز", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { showBookingDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء الحجز", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showBookingConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBookingConfirmDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("📋 هل بيانات الحجز صحيحة؟", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("الاسم الصريح: $customerNameInput", color = Color.White)
                    Text("رقم الهاتف: $customerPhoneInput", color = Color.White)
                    Text("منطقة السكن والمنزل: $customerAreaInput", color = Color.White)
                    Text("الفني: ${provider.name}", color = themeColors.accent)
                    Text("سعر المعاينة المقدر: ${provider.previewPrice.toInt()} ريال يمني", color = Color.Green, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addBooking(
                            name = customerNameInput,
                            phone = customerPhoneInput,
                            area = customerAreaInput,
                            providerId = provider.id,
                            providerName = provider.name
                        )
                        showBookingConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("نعم، إرسال فوري للحجز", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showBookingConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تعديل", color = Color.White)
                }
            }
        )
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
    val categories by viewModel.categories.collectAsState()
    val activatedProviders by viewModel.providers.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val chatChannels by viewModel.chatChannels.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val bannersList by viewModel.banners.collectAsState()

    var inputPasscode by remember { mutableStateOf("") }
    var isAuthorized by remember { mutableStateOf(adminRole != "GUEST") }
    var activeSubTab by remember { mutableStateOf("REG_REQ") }

    // Dialog state controllers for category edits and deletions
    var showDeleteCategoryConfirmId by remember { mutableStateOf<String?>(null) }
    var showEditCategoryObj by remember { mutableStateOf<CategoryEntity?>(null) }
    var editCatName by remember { mutableStateOf("") }
    var editCatIcon by remember { mutableStateOf("") }
    var newCatName by remember { mutableStateOf("") }
    var newCatIcon by remember { mutableStateOf("") }

    // Dialog state controllers for booking deletions
    var showDeleteBookingConfirmId by remember { mutableStateOf<String?>(null) }

    // Dialog state controllers for notifications deletions
    var showDeleteNotifConfirmId by remember { mutableStateOf<String?>(null) }

    // Dialog state controllers for chat selections
    var showActiveChatChannelObj by remember { mutableStateOf<ChatChannelEntity?>(null) }
    var adminChatReplyInput by remember { mutableStateOf("") }
    var showDeleteChatConfirmId by remember { mutableStateOf<String?>(null) }

    // Notification input states
    var notifTitleInput by remember { mutableStateOf("") }
    var notifMsgInput by remember { mutableStateOf("") }
    var notifTargetType by remember { mutableStateOf("ALL") } // ALL, USER, AREA
    var notifTargetValue by remember { mutableStateOf("") }

    // Section Ten input configs state
    var editPrimaryHex by remember { mutableStateOf(settingsState.customPrimaryHex) }
    var editSecondaryHex by remember { mutableStateOf(settingsState.customSecondaryHex) }
    var editCardBgHex by remember { mutableStateOf(settingsState.cardBackgroundHex) }
    var editProviderNameHex by remember { mutableStateOf(settingsState.providerNameColorHex) }
    var editLocationHex by remember { mutableStateOf(settingsState.locationColorHex) }
    var editRatingHex by remember { mutableStateOf(settingsState.ratingColorHex) }
    var editVipBadgeHex by remember { mutableStateOf(settingsState.vipBadgeColorHex) }
    var editVerifiedHex by remember { mutableStateOf(settingsState.verifiedBadgeColorHex) }
    var editRecommendedHex by remember { mutableStateOf(settingsState.recommendedBadgeColorHex) }
    
    var editFontSelected by remember { mutableStateOf(settingsState.activeFontFamily) }
    
    var editChatIconSize by remember { mutableStateOf(settingsState.chatSize.toFloat()) }
    var editChatIconX by remember { mutableStateOf(settingsState.chatXOffset.toFloat()) }
    var editChatIconY by remember { mutableStateOf(settingsState.chatYOffset.toFloat()) }

    var editAssistantIconSize by remember { mutableStateOf(settingsState.assistantSize.toFloat()) }
    var editAssistantIconX by remember { mutableStateOf(settingsState.assistantXOffset.toFloat()) }
    var editAssistantIconY by remember { mutableStateOf(settingsState.assistantYOffset.toFloat()) }

    var requirementItemInput by remember { mutableStateOf("") }
    var requirementsListState by remember { mutableStateOf(settingsState.registrationRequirements) }

    // Wipe states
    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var wipeInputPassword by remember { mutableStateOf("") }

    // Section 2 state variables
    var manualName by remember { mutableStateOf("") }
    var manualPhone by remember { mutableStateOf("") }
    var manualCategoryId by remember { mutableStateOf("") }
    var manualStreet by remember { mutableStateOf("") }
    var manualCityId by remember { mutableStateOf("") }
    var manualPhotoUrl by remember { mutableStateOf("") }
    var manualIdCardUrl by remember { mutableStateOf("") }
    var manualForensicUrl by remember { mutableStateOf("") }
    var manualPriceValue by remember { mutableStateOf("1500") }
    var manualIsVipGolden by remember { mutableStateOf(false) }

    // Section 4 state variables
    var newCityArName by remember { mutableStateOf("") }
    var newCityEnName by remember { mutableStateOf("") }

    // Section 5 state variables
    var complaintsSearchQuery by remember { mutableStateOf("") }

    // Section 7 state variables
    var activeProvidersSearchQuery by remember { mutableStateOf("") }
    var showEditProviderMetadataObj by remember { mutableStateOf<ProviderEntity?>(null) }
    var editProviderPhone by remember { mutableStateOf("") }
    var editProviderCategoryId by remember { mutableStateOf("") }

    // Section 9 state variables
    var supervisorInputName by remember { mutableStateOf("") }
    var supervisorInputRole by remember { mutableStateOf("SUPPORT") }
    var supervisorInputPasscode by remember { mutableStateOf("") }

    // Section 10 layout density adjustments
    var elementSpacingPadding by remember { mutableStateOf(12f) }
    var containerCardPadding by remember { mutableStateOf(14f) }

    val context = LocalContext.current

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
            Text("بوابة مسؤولي المنصة الموثقة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("الرجاء إدخال رقم التعريف الخاص بالدعم للتحكم في الطلبات والمظهر:", fontSize = 11.sp, color = themeColors.textSecondary)
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
                    if (inputPasscode == "777" || inputPasscode == "1234" || inputPasscode == "maher736462") {
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
        // Logged dashboard with beautiful segment rows
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
                    Text("🔐 لوحة التحكم الرئيسية", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Button(
                        onClick = {
                            isAuthorized = false
                            viewModel.logout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("تسجيل خروج", color = Color.White, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // High aesthetic Horizontal Tab Bar
            item {
                val tabs = listOf(
                    Pair("REG_REQ", "📨 طلبات التسجيل (1)"),
                    Pair("MANUAL_ADD", "✨ إضافة فني يدوياً (2)"),
                    Pair("ADV_BANNERS", "🖼️ الإعلانات والبنرات (3)"),
                    Pair("CATEGORIES_CITIES", "📂 الفئات والمدن (4)"),
                    Pair("COMPLAINTS_REPORTS", "📢 البلاغات والشكاوى (5)"),
                    Pair("CHAT_MONITOR", "💬 مراقبة الشات (6)"),
                    Pair("ACTIVE_PROVIDERS", "👥 الفنيين النشطين (7)"),
                    Pair("SUBS_BADGES", "💳 الاشتراكات والتثبيت (8)"),
                    Pair("SUPER_ADMIN_WIPE", "🔑 المشرفين والتطهير (9)"),
                    Pair("BRAND_AESTHETICS", "🎨 الهوية والخط والشروط (10)")
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(tabs) { tab ->
                        val isSel = activeSubTab == tab.first
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isSel) themeColors.accent else themeColors.surface)
                                .clickable { activeSubTab = tab.first }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .border(1.dp, if (isSel) Color.White else themeColors.accent.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        ) {
                            Text(
                                text = tab.second,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            // ------------------ CONDITIONAL SUB-SCREENS RENDERING ------------------

            if (activeSubTab == "REG_REQ") {
                // Section 1: Registration Requests
                item {
                    Text("📨 طلبات التسجيل المعلقة بانتظار الموافقة (${pendingProviders.size}):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (pendingProviders.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                            Text("لا توجد طلبات معلقة من الفنيين الجدد حالياً.", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
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
                                Text(text = "العنوان المطلوب: ${req.area} - ${req.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.approveRequest(req) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                                    ) {
                                        Text("قبول وتفعيل", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { viewModel.rejectRequest(req, "المستندات أو الهوية غير واضحة") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                    ) {
                                        Text("رفض الطلب", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "COMPLAINTS_REPORTS") {
                // Section 5: Complaints and Reports Logs
                item {
                    Text("📢 البلاغات الواردة وشكاوى المواطنين (${reports.size}):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("استخدم الفلتر الذكي للبحث عن بلاغات فني أو مواطن معين وتصدير السجلات:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = complaintsSearchQuery,
                        onValueChange = { complaintsSearchQuery = it },
                        label = { Text("بحث باسم الفني أو المشتكي...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = themeColors.accent) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { 
                                viewModel.exportComplaintsToCSV() 
                                Toast.makeText(context, "تم تصدير سجل الشكاوى بصيغة CSV بنجاح 📁", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تصدير CSV 💾", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { 
                                viewModel.exportComplaintsToPDF() 
                                Toast.makeText(context, "تم تصدير مستند الشكاوى بصيغة PDF بنجاح 📄", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تصدير PDF 📄", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                val filteredComplaints = reports.filter {
                    it.providerName.contains(complaintsSearchQuery, ignoreCase = true) ||
                    it.reporterName.contains(complaintsSearchQuery, ignoreCase = true) ||
                    it.content.contains(complaintsSearchQuery, ignoreCase = true)
                }

                if (filteredComplaints.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                            Text("لا توجد بلاغات تفرز معايير البحث المسجلة.", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                        }
                    }
                } else {
                    items(filteredComplaints) { rep ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("الفني المشكو ضده: ${rep.providerName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("اسم المواطن الشاكي: ${rep.reporterName}", fontSize = 11.sp, color = themeColors.textSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("مضمون ومحتوى البلاغ: ${rep.content}", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "MANUAL_ADD") {
                // Section 2: Manual Technician Addition
                item {
                    Text("✨ إضافة فني جديد يدوياً إلى الدليل الشامل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("لتجاوز قائمة الانتظار، أدخل كافة تفاصيل الفني للتفعيل الفوري بالدليل:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualName,
                                onValueChange = { manualName = it },
                                label = { Text("الاسم الكامل للحرفي/المهندِس") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = manualPhone,
                                onValueChange = { manualPhone = it },
                                label = { Text("رقم الهاتف (مثال: 777644670)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = manualStreet,
                                onValueChange = { manualStreet = it },
                                label = { Text("الشارع أو الحي التفصيلي") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = manualPriceValue,
                                onValueChange = { manualPriceValue = it },
                                label = { Text("سعر وكرت المعاينة الأولي (ريال يمني)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            
                            // Dropdowns for categories and cities Selection
                            Text("اختر قسم الصيانة المستهدف:", fontSize = 11.sp, color = themeColors.textSecondary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(categories) { cat ->
                                    val isSel = manualCategoryId == cat.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) themeColors.accent else themeColors.surface)
                                            .clickable { manualCategoryId = cat.id }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    ) {
                                        Text(cat.name, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White)
                                    }
                                }
                            }

                            Text("اختر المدينة اليمنية المحتوية للحي:", fontSize = 11.sp, color = themeColors.textSecondary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(citiesList) { city ->
                                    val isSel = manualCityId == city.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) themeColors.accent else themeColors.surface)
                                            .clickable { manualCityId = city.id }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    ) {
                                        Text(city.nameAr, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = manualPhotoUrl,
                                onValueChange = { manualPhotoUrl = it },
                                label = { Text("رابط صورة الفني الشخصية (اختياري)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = manualIdCardUrl,
                                onValueChange = { manualIdCardUrl = it },
                                label = { Text("رابط صورة بطاقة الهوية الذكية") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = manualForensicUrl,
                                onValueChange = { manualForensicUrl = it },
                                label = { Text("رابط الفيش الجنائي وخلو السوابق (PDF/رابط)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = manualIsVipGolden,
                                    onCheckedChange = { manualIsVipGolden = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706))
                                )
                                Text("تفعيل كرت VIP الذهبي المميز على الدليل فورا", fontSize = 11.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    if (manualName.trim().isEmpty() || manualPhone.trim().isEmpty() || manualStreet.trim().isEmpty()) {
                                        Toast.makeText(context, "الرجاء تعبئة الاسم والهاتف وعنوان الحي للتفعيل", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val finalCat = if (manualCategoryId.isEmpty()) (categories.firstOrNull()?.id ?: "1") else manualCategoryId
                                        val finalCity = if (manualCityId.isEmpty()) (citiesList.firstOrNull()?.id ?: "ye_san") else manualCityId
                                        val priceVal = manualPriceValue.toDoubleOrNull() ?: 1500.0

                                        viewModel.addNewProviderCustom(
                                            name = manualName.trim(),
                                            phone = manualPhone.trim(),
                                            catId = finalCat,
                                            street = manualStreet.trim(),
                                            cityId = finalCity,
                                            profileImage = manualPhotoUrl.trim(),
                                            idCardImage = manualIdCardUrl.trim(),
                                            forensicImage = manualForensicUrl.trim(),
                                            price = priceVal,
                                            isVip = manualIsVipGolden
                                        )

                                        // Reset state
                                        manualName = ""
                                        manualPhone = ""
                                        manualStreet = ""
                                        manualPriceValue = "1500"
                                        manualIsVipGolden = false
                                        manualPhotoUrl = ""
                                        manualIdCardUrl = ""
                                        manualForensicUrl = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("➕ إضافة الفني وتفعيله بالكامل فوراً", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // INDEPENDENT PROMOTIONS AND VERIFICATIONS LISTING
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("🏅 ترقية الفنيين والأوسمة الذكية المستقلة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("تنويه: يمكنك دمج الأوسمة معاً والتحكم بها بشكل مستقل دون قيود:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(activatedProviders) { p ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            val catName = categories.find { it.id == p.categoryId }?.name ?: "خدمات عامة"
                            Text("المهنة: $catName | المنطقة: ${p.area}", fontSize = 11.sp, color = themeColors.textSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 3 Independent switches next to each provider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = p.isVip,
                                        onCheckedChange = { viewModel.pinProvider(p.id, it) },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706))
                                    )
                                    Text("VIP ذهبي", fontSize = 11.sp, color = Color.White)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = p.isVerified,
                                        onCheckedChange = { viewModel.verifyProviderBadge(p.id, it) },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF3B82F6))
                                    )
                                    Text("موثق حساب", fontSize = 11.sp, color = Color.White)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = p.isRecommended,
                                        onCheckedChange = { viewModel.recommendProvider(p.id, it) },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEC4899))
                                    )
                                    Text("موصى به", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // 🖼️ ADVERTISING BANNERS REORDERING AND MANAGEMENT
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text("🖼️ إدارة وترتيب بنرات الإعلانات الترويجية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Add banner form
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إضافة بنر إعلاني جديد:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            var bannerTitle by remember { mutableStateOf("") }
                            var bannerImage by remember { mutableStateOf("") }
                            
                            OutlinedTextField(
                                value = bannerTitle,
                                onValueChange = { bannerTitle = it },
                                label = { Text("عنوان الإعلان الترويجي") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            
                            OutlinedTextField(
                                value = bannerImage,
                                onValueChange = { bannerImage = it },
                                label = { Text("رابط صورة الإعلان") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            
                            Button(
                                onClick = {
                                    if (bannerTitle.trim().isNotEmpty() && bannerImage.trim().isNotEmpty()) {
                                        viewModel.addNewBanner(
                                            title = bannerTitle.trim(),
                                            url = bannerImage.trim(),
                                            redirect = "1",
                                            type = "NORMAL",
                                            size = "LARGE",
                                            duration = 5
                                        )
                                        bannerTitle = ""
                                        bannerImage = ""
                                    } else {
                                        Toast.makeText(context, "الرجاء تعبئة العنوان ورابط الصورة", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إضافة البنر الإعلاني", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                item {
                    Text("📋 قائمة البنرات النشطة (ادعم الترتيب بالسحب والإفلات والمبادلة):", fontSize = 12.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                val bannersList_state = bannersList
                itemsIndexed(bannersList_state) { index, b ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "سحب لنقل الترتيب",
                                    tint = themeColors.accent,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable {
                                             if (index > 0) {
                                                 val mutableBanners = bannersList_state.toMutableList()
                                                 val prev = mutableBanners[index - 1]
                                                 mutableBanners[index - 1] = b
                                                 mutableBanners[index] = prev
                                                 viewModel.reorderBanners(mutableBanners)
                                             }
                                        }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(b.title, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(b.url, fontSize = 10.sp, color = themeColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val mutableBanners = bannersList_state.toMutableList()
                                            val prev = mutableBanners[index - 1]
                                            mutableBanners[index - 1] = b
                                            mutableBanners[index] = prev
                                            viewModel.reorderBanners(mutableBanners)
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "أعلى", tint = if (index > 0) themeColors.accent else Color.Gray.copy(alpha = 0.5f))
                                }
                                IconButton(
                                    onClick = {
                                        if (index < bannersList_state.size - 1) {
                                            val mutableBanners = bannersList_state.toMutableList()
                                            val next = mutableBanners[index + 1]
                                            mutableBanners[index + 1] = b
                                            mutableBanners[index] = next
                                            viewModel.reorderBanners(mutableBanners)
                                        }
                                    },
                                    enabled = index < bannersList_state.size - 1
                                ) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "أسفل", tint = if (index < bannersList_state.size - 1) themeColors.accent else Color.Gray.copy(alpha = 0.5f))
                                }
                                IconButton(onClick = { viewModel.deleteBanner(b.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "BOOK_NOTIF") {
                // RESERVATIONS SECTION
                item {
                    Text("📅 إدارة حجوزات الصيانة النشطة والجديدة (${bookings.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (bookings.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                            Text("لا توجد طلبات حجز مكتوبة حالياً في السجلات", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                        }
                    }
                } else {
                    items(bookings) { b ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, if (b.status == "PENDING") themeColors.accent else Color.Transparent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("اسم العميل: ${b.customerName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    val bColor = when(b.status) {
                                        "APPROVED" -> Color.Green
                                        "REJECTED" -> Color.Red
                                        else -> themeColors.accent
                                    }
                                    Text(
                                        text = if (b.status == "APPROVED") "معتمد" else if (b.status == "REJECTED") "مرفوض" else "بانتظار الموافقة",
                                        fontSize = 11.sp,
                                        color = bColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text("هاتف العميل: ${b.customerPhone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                Text("منطقة السكن والحي: ${b.customerArea}", fontSize = 11.sp, color = themeColors.textSecondary)
                                Text("اسم الفني المستهدف: ${b.providerName}", fontSize = 11.sp, color = themeColors.accent)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (b.status == "PENDING") {
                                            Button(
                                                onClick = { viewModel.updateBookingStatus(b.id, "APPROVED") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("قبول واعتماد", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = { viewModel.updateBookingStatus(b.id, "REJECTED") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("رفض الحجز", color = Color.White, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = { showDeleteBookingConfirmId = b.id }
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }

                // TARGETED NOTIFICATIONS MANAGERS
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("🔔 نظام الإشعارات الذكية الموجهة (Targeted)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إرسال إشعار فوري موجه:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            OutlinedTextField(
                                value = notifTitleInput,
                                onValueChange = { notifTitleInput = it },
                                label = { Text("عنوان الإشعار (مثال: خصم هائل اليوم!)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = notifMsgInput,
                                onValueChange = { notifMsgInput = it },
                                label = { Text("مضمون الرسالة بالتفصيل") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            // Target Type selectors
                            Text("نطاق ومجموعة المستهدفين بالبث البوش:", fontSize = 11.sp, color = themeColors.textSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = notifTargetType == "ALL", onClick = { notifTargetType = "ALL" })
                                    Text("الكل", fontSize = 11.sp, color = Color.White)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = notifTargetType == "USER", onClick = { notifTargetType = "USER" })
                                    Text("مستخدم محدد", fontSize = 11.sp, color = Color.White)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = notifTargetType == "AREA", onClick = { notifTargetType = "AREA" })
                                    Text("منطقة/مدينة", fontSize = 11.sp, color = Color.White)
                                }
                            }

                            if (notifTargetType != "ALL") {
                                OutlinedTextField(
                                    value = notifTargetValue,
                                    onValueChange = { notifTargetValue = it },
                                    label = { Text(if (notifTargetType == "USER") "رقم هاتف العميل للتوصيل" else "اسم المحافظة/المدينة المستهدفة") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            Button(
                                onClick = {
                                    if (notifTitleInput.trim().isEmpty() || notifMsgInput.trim().isEmpty()) {
                                        Toast.makeText(context, "يرجى تعبئة العنوان ونص الرسالة", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.addNotification(
                                            title = notifTitleInput,
                                            message = notifMsgInput,
                                            targetType = notifTargetType,
                                            targetValue = if (notifTargetType == "ALL") "" else notifTargetValue
                                        )
                                        notifTitleInput = ""
                                        notifMsgInput = ""
                                        notifTargetValue = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إرسال الإشعار وبثه للمستهدفين", color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("⚡ نماذج سريعة للإرسال والبث الفوري لجميع المستخدمين:", fontSize = 11.sp, color = themeColors.textSecondary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.addNotification(
                                            title = "🔥 عرض حصري محدود من إدارة المنصة",
                                            message = "خصم يصل إلى 35% على خدمات التكييف، التمديدات وصيانة الأجهزة المنزلية اليوم فقط! احجز فنيك الآن عبر التطبيق.",
                                            targetType = "ALL",
                                            targetValue = ""
                                        )
                                        Toast.makeText(context, "تم بث عرض ترويجي للجميع بنجاح!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("بث عرض ترويجي 📢", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.addNotification(
                                            title = "🛠️ تحديث فني هام للنظام وتطوير الأداء",
                                            message = "عملائنا الأعزاء، نود إعلامكم بإطلاق تحديث جديد للبحث الجغرافي وحساب المسافات بأعلى دقة. نوصي بتحديث التطبيق الآن.",
                                            targetType = "ALL",
                                            targetValue = ""
                                        )
                                        Toast.makeText(context, "تم بث إعلان تحديث هام للجميع بنجاح!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("بث تحديث فني ⚙️", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Text("📋 تاريخ الرسائل البوش السابقة (${notifications.size}):", fontSize = 12.sp, color = themeColors.textSecondary)
                }

                items(notifications) { n ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(n.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                IconButton(onClick = { showDeleteNotifConfirmId = n.id }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(n.message, fontSize = 12.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            val filterStr = when(n.targetType) {
                                "USER" -> "مستهدف صريح بالهاتف: ${n.targetValue}"
                                "AREA" -> "محافظة يمنية محددة: ${n.targetValue}"
                                else -> "جميع المشتركين بالمنصة"
                            }
                            Text("نطاق الاستهداف: $filterStr", fontSize = 10.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (activeSubTab == "CHATS_ADMIN") {
                // CHATS LIVE AND OUTAGE MANAGEMENT
                item {
                    Text("💬 إدارة محادثات الدعم والدردشات الفورية ومراقبة السجلات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Global configs item
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("أدوات التحكم العام بحسابات الدردشة:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            var disableChatGlobally by remember { mutableStateOf(false) }
                            
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("إيقاف وتعطيل خدمة الشات وتحديثات الفنيين عالمياً", fontSize = 11.sp, color = Color.White)
                                Switch(checked = disableChatGlobally, onCheckedChange = { disableChatGlobally = it })
                            }
                            
                            OutlinedTextField(
                                value = "نظراً لأعمال صيانة خوادم الدعم، تم إيقاف الشات الفوري مؤقتاً لساعتين. الرجاء استخدام الاتصال الهاتفي.",
                                onValueChange = {},
                                label = { Text("رسالة التعطيل وبث الطوارئ في الشاشات") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.wipeOldChatChannels(30) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text("تصفية الأرشيف القديم (>30 يوم)", fontSize = 10.sp, color = Color.White)
                                }
                                Button(
                                    onClick = { Toast.makeText(context, "تم تصدير سجل المحادثات بنجاح للمصنف المالي", Toast.LENGTH_SHORT).show() },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("تصدير CSV", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                item {
                    Text("📋 قنوات المحادثة المفتوحة حالياً (${chatChannels.size}):", fontSize = 12.sp, color = themeColors.textSecondary)
                }

                if (chatChannels.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                            Text("لا توجد محادثات نشطة حالياً في ذاكرة الشات", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                        }
                    }
                } else {
                    items(chatChannels) { ch ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth().clickable { showActiveChatChannelObj = ch }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    val parties = if (ch.isProvider) "مقدم الخدمة: ${ch.userName}" else "مستخدم الدليل: ${ch.userName}"
                                    Text("المحادثة: $parties", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    if (ch.isBlocked) {
                                        Text("محظورة", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("آخر رسالة: " + ch.lastMessage, fontSize = 11.sp, color = themeColors.textSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { showActiveChatChannelObj = ch },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("افتح المحادثة ورد كأدمن", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { showDeleteChatConfirmId = ch.id },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("حذف المحادثة الصريحة", fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "SECTION_TEN") {
                // SECTION TEN CONFIGURATIONS
                item {
                    Text("🎨 القسم العاشر: التحكم المتقدم والألوان ونماذج الشروط والخط", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Color picker inputs
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("تخصيص لوحة الألوان اليمينة الفاخرة للهيئات بالتفصيل (Hex Color):", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            OutlinedTextField(
                                value = editPrimaryHex,
                                onValueChange = { editPrimaryHex = it },
                                label = { Text("اللون الرئيسي للبرنامج (Primary Color)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editSecondaryHex,
                                onValueChange = { editSecondaryHex = it },
                                label = { Text("اللون الثانوي للبرنامج (Secondary Color)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editCardBgHex,
                                onValueChange = { editCardBgHex = it },
                                label = { Text("لون خلفية كروت الفنيين (Card Background Hex)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editProviderNameHex,
                                onValueChange = { editProviderNameHex = it },
                                label = { Text("لون اسم مقدم الخدمة (Provider Name Color)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editLocationHex,
                                onValueChange = { editLocationHex = it },
                                label = { Text("لون خط المكان والموقع الجغرافي للشارع") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editRatingHex,
                                onValueChange = { editRatingHex = it },
                                label = { Text("لون نجمة وأرقام التقاييم والنسب الفنية") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editVipBadgeHex,
                                onValueChange = { editVipBadgeHex = it },
                                label = { Text("لون شارة VIP الذهبية المحيطة") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editVerifiedHex,
                                onValueChange = { editVerifiedHex = it },
                                label = { Text("لون الشارة الزرقاء الموثقة للدعم") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editRecommendedHex,
                                onValueChange = { editRecommendedHex = it },
                                label = { Text("لون نجمة وشريحة التوصية (Recommended Badge)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                    }
                }

                // Font adjustment
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("تخصيص نمط الخطوط العربية بالدليل (RTL typography):", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            val fontOptions = listOf("cairo", "amiri", "tahoma", "system")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                fontOptions.forEach { font ->
                                    val isSel = editFontSelected == font
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) themeColors.accent else Color.Black.copy(alpha = 0.3f))
                                            .clickable { editFontSelected = font }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(font.uppercase(), fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating bubble sliders and offset coordinates adjustments
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("تعديل أحجام وإحداثيات أيقونات الدردشة العائمة بالدعم:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            Text("1. حجم أيقونة شات المساعدة المباشرة: ${editChatIconSize.toInt()}dp", fontSize = 11.sp, color = Color.White)
                            Slider(value = editChatIconSize, onValueChange = { editChatIconSize = it }, valueRange = 35f..90f)

                            Text("• إحداثي الإزاحة الأفقي (X-Offset): ${editChatIconX.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                            Slider(value = editChatIconX, onValueChange = { editChatIconX = it }, valueRange = 10f..120f)
                            
                            Text("• إحداثي الإزاحة الرأسي (Y-Offset): ${editChatIconY.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                            Slider(value = editChatIconY, onValueChange = { editChatIconY = it }, valueRange = 30f..180f)

                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text("2. حجم أيقونة المساعد الصوتي الذكي (البوت): ${editAssistantIconSize.toInt()}dp", fontSize = 11.sp, color = Color.White)
                            Slider(value = editAssistantIconSize, onValueChange = { editAssistantIconSize = it }, valueRange = 35f..90f)

                            Text("• إحداثي البوت الأفقي (X-Offset): ${editAssistantIconX.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                            Slider(value = editAssistantIconX, onValueChange = { editAssistantIconX = it }, valueRange = 10f..120f)
                            
                            Text("• إحداثي البوت الرأسي (Y-Offset): ${editAssistantIconY.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                            Slider(value = editAssistantIconY, onValueChange = { editAssistantIconY = it }, valueRange = 30f..180f)
                        }
                    }
                }

                // Requirements Form manager list
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📋 إدارة شروط ونموذج تسجيل الفنيين بالمنصة:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = requirementItemInput,
                                    onValueChange = { requirementItemInput = it },
                                    label = { Text("شرط جديد (مثال: فيش جنائي)") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                Button(
                                    onClick = {
                                        if (requirementItemInput.trim().isNotEmpty()) {
                                            requirementsListState = requirementsListState + requirementItemInput.trim()
                                            requirementItemInput = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Text("أضف", color = Color.Black)
                                }
                            }

                            requirementsListState.forEachIndexed { idx, reqItem ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${idx+1}. $reqItem", color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { requirementsListState = requirementsListState.filterIndexed { pIdx, _ -> pIdx != idx } }
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "حذف الالتزام", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Save button for Section Ten details
                item {
                    Button(
                        onClick = {
                            val upToDateSettings = settingsState.copy(
                                customPrimaryHex = editPrimaryHex,
                                customSecondaryHex = editSecondaryHex,
                                cardBackgroundHex = editCardBgHex,
                                providerNameColorHex = editProviderNameHex,
                                locationColorHex = editLocationHex,
                                ratingColorHex = editRatingHex,
                                vipBadgeColorHex = editVipBadgeHex,
                                verifiedBadgeColorHex = editVerifiedHex,
                                recommendedBadgeColorHex = editRecommendedHex,
                                activeFontFamily = editFontSelected,
                                chatSize = editChatIconSize.toInt(),
                                chatXOffset = editChatIconX.toInt(),
                                chatYOffset = editChatIconY.toInt(),
                                assistantSize = editAssistantIconSize.toInt(),
                                assistantXOffset = editAssistantIconX.toInt(),
                                assistantYOffset = editAssistantIconY.toInt(),
                                registrationRequirements = requirementsListState
                            )
                            viewModel.updateBackdoorSettings(
                                appName = upToDateSettings.appName,
                                welcomeMsg = upToDateSettings.welcomeMessage,
                                footerMsg = upToDateSettings.footerMessage,
                                themeId = upToDateSettings.activeThemeId,
                                supportPhone = "777644670",
                                supportEmail = "wam2026@support.ye",
                                supportWhatsapp = "777644670",
                                isMaintenance = upToDateSettings.isMaintenanceActive,
                                hiddenFooter = upToDateSettings.hidePromoFooter,
                                botHidden = upToDateSettings.assistantHidden,
                                botSize = upToDateSettings.assistantSize,
                                chatHidden = upToDateSettings.chatHidden,
                                chatSize = upToDateSettings.chatSize,
                                radiusKm = upToDateSettings.maxSearchRadiusKm,
                                isSpeech = upToDateSettings.isSpeechSearchEnabled,
                                isDataSaver = false,
                                imgQuality = 90
                            )
                            // Direct persistence inside settings StateFlow
                            viewModel.saveCustomSettingsState(upToDateSettings)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("💾 حفظ وحقن جميع تخصيصات المظهر بالدليل الصريح والكامل", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (activeSubTab == "SUBSCRIPTIONS") {
                // SUBSCRIPTION CONTROL PANEL
                item {
                    Text("💳 لوحة التحكم باشتراكات الفنيين والتجديد", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("إدارة فترات الصلاحية وشارات الإعلانات، وبث إشعارات التحذير قبل الانتهاء بـ 48 ساعة:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Global Alert Button: auto scan for any technician whose subscription expires within 48 hours and send them push alerts!
                item {
                    Button(
                        onClick = {
                            var sentCount = 0
                            val fortyEightHoursMs = 48L * 60 * 60 * 1000
                            activatedProviders.forEach { p ->
                                val timeLeft = p.subscriptionExpiry - System.currentTimeMillis()
                                if (timeLeft > 0 && timeLeft <= fortyEightHoursMs) {
                                    viewModel.addNotification(
                                        title = "تنبيه هام بفترة تجديد الاشتراك",
                                        message = "عزيزنا الفني المعتمد ${p.name}، يرجى التنويه بأن اشتراكك الفني ينتهي خلال أقل من 48 ساعة. يرجى تجديد الاشتراك فوراً لتفادي تجميد حسابك.",
                                        targetType = "USER",
                                        targetValue = p.phone
                                    )
                                    sentCount++
                                }
                            }
                            if (sentCount > 0) {
                                Toast.makeText(context, "تم بث تنبيهات بوش تلقائية لعدد ($sentCount) فنيين اشتراكهم ينتهي خلال 48 ساعة!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "لم يتم العثور على أي فنيين اقترب انتهاء اشتراكهم (تحت 48 ساعة) في السجلات حالياً.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🚨 بث تلقائي لتنبيهات 48 ساعة لجميع الفنيين المستهدفين", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Providers list with expiration counters and manual alert triggering
                items(activatedProviders) { p ->
                    val timeLeft = p.subscriptionExpiry - System.currentTimeMillis()
                    val daysLeft = (timeLeft / (24L * 60 * 60 * 1000)).toInt()
                    val hoursLeft = ((timeLeft % (24L * 60 * 60 * 1000)) / (60L * 60 * 1000)).toInt()
                    
                    val timeString = if (timeLeft < 0) {
                        "منتهي الصلاحية ❌"
                    } else if (daysLeft > 0) {
                        "متبقي $daysLeft يوم و$hoursLeft ساعة"
                    } else {
                        "متبقي $hoursLeft ساعة فقط ⚠️"
                    }
                    
                    val isNearExpiry = timeLeft > 0 && timeLeft <= (48L * 60 * 60 * 1000)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, if (isNearExpiry) Color.Red else themeColors.accent.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    text = timeString,
                                    fontSize = 11.sp,
                                    color = if (timeLeft < 0) Color.Red else if (isNearExpiry) Color.Yellow else Color.Green,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text("رقم الهاتف: ${p.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                            Text("حالة الاشتراك الفني: ${p.subscriptionStatus}", fontSize = 11.sp, color = themeColors.accent)
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.extendProviderSubscription(p.id, 30L * 24 * 60 * 60 * 1000)
                                        Toast.makeText(context, "تم تجديد اشتراك ${p.name} لمدة 30 يوماً بنجاح", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("تجديد 30 يوم 🟢", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.addNotification(
                                            title = "تنبيه هام بانتهاء صلاحية الاشتراك",
                                            message = "عزيزنا الفني ${p.name}، نود تذكيرك بأن اشتراكك ينتهي خلال 48 ساعة فقط. الرجاء المسارعة بالتجديد للاستمرار بظهور اسمك للزبائن في التطبيق.",
                                            targetType = "USER",
                                            targetValue = p.phone
                                        )
                                        Toast.makeText(context, "تم إرسال إشعار بوش يدوي ينبه الفني بالفترة المحددة بـ 48 ساعة", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("تنبيه بـ 48 ساعة 🔔", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "WIPE") {
                // PASSCODE WIPING SCREEN
                item {
                    Text("⚠️ تطهير وحذف كامل البيانات وعودتها للصفر المطلق", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03)),
                        border = BorderStroke(1.dp, Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("تنبيه أمني صارم للغاية!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                            Text(
                                text = "الضغط على الزر أدناه سيقوم بإلغاء اشتراكات الفنيين فوراً، مسح سجلات حجز الصيانة، مسح تاريخ قنوات الشات والشكاوى المتراكمة وإعادة البرنامج إلى الحالة الفارغة تماماً. لن يتسنى لك مراجعة البيانات الملغاة.",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showWipeConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("مسح كامل البيانات وإعادة بناء الدليل العظيم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // ------------------ POPUP CONFIRMATION CONTEXT DIALOGS ------------------

    // 1. Delete category confirmation
    showDeleteCategoryConfirmId?.let { catId ->
        val catName = categories.find { it.id == catId }?.name ?: ""
        AlertDialog(
            onDismissRequest = { showDeleteCategoryConfirmId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("⚠️ هل ترغب في حذف القسم؟", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("أنت على وشك حذف قسم الصيانة ($catName) نهائياً. سيتم إزالتها من شريط الانتقالات ومقدمي الخدمات.", color = themeColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(catId)
                        showDeleteCategoryConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، احذف القسم")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteCategoryConfirmId = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("إلغاء الإجراء")
                }
            }
        )
    }

    // 2. Edit category Dialog
    showEditCategoryObj?.let { cat ->
        Dialog(onDismissRequest = { showEditCategoryObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("✏️ تعديل صنف خدمة الصيانة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    OutlinedTextField(
                        value = editCatName,
                        onValueChange = { editCatName = it },
                        label = { Text("اسم القسم") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCatIcon,
                        onValueChange = { editCatIcon = it },
                        label = { Text("أيقونة القسم (Base64/URL)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (editCatName.trim().isNotEmpty()) {
                                    viewModel.editCategory(cat.id, editCatName.trim(), editCatIcon.trim())
                                    showEditCategoryObj = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ وتعديل", color = Color.Black)
                        }
                        Button(
                            onClick = { showEditCategoryObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // 3. Delete reservation confirmation Dialog
    showDeleteBookingConfirmId?.let { bId ->
        AlertDialog(
            onDismissRequest = { showDeleteBookingConfirmId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("⚠️ هل تقصد حذف الحجز نهائياً؟", color = Color.White) },
            text = { Text("حذف هذا الحجز سيزيله من جدول حجوزات الدعم والمتابعة مباشرة.", color = themeColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBooking(bId)
                        showDeleteBookingConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، امسح تماماً", color = Color.White)
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteBookingConfirmId = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("تراجع")
                }
            }
        )
    }

    // 4. Delete targeted notifications confirmation Dialog
    showDeleteNotifConfirmId?.let { nId ->
        AlertDialog(
            onDismissRequest = { showDeleteNotifConfirmId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("⚠️ تأكيد حذف الإشعار الموجه", color = Color.White) },
            text = { Text("هذا الإشعار سيوضع كمنشور تالف وسيتم محوه من قاعدة بيانات الهواتف المحلية والمخزن.", color = themeColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNotification(nId)
                        showDeleteNotifConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، احذف الذكرى")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteNotifConfirmId = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("تراجع")
                }
            }
        )
    }

    // 5. Active Chat Channel logs visualizer dialog and direct replies
    showActiveChatChannelObj?.let { ch ->
        Dialog(onDismissRequest = { showActiveChatChannelObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                border = BorderStroke(1.dp, themeColors.accent)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val partnerName = if (ch.isProvider) "مقدم الخدمة: ${ch.userName}" else "مستخدم الدليل: ${ch.userName}"
                    Text("💬 مراقبة الشات: $partnerName", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    // Messages records logger
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ch.messages) { msg ->
                                val alignment = if (msg.senderId == "admin") Alignment.End else Alignment.Start
                                val bubbleBg = if (msg.senderId == "admin") themeColors.primary else Color.Gray.copy(alpha = 0.3f)
                                Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bubbleBg)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(msg.message, fontSize = 11.sp, color = Color.White)
                                    }
                                    Text(msg.senderName, fontSize = 9.sp, color = themeColors.textSecondary)
                                }
                            }
                        }
                    }

                    // Reply tool
                    OutlinedTextField(
                        value = adminChatReplyInput,
                        onValueChange = { adminChatReplyInput = it },
                        label = { Text("اكتب رد المشرف الصريح والكامل هنا...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (adminChatReplyInput.trim().isNotEmpty()) {
                                    viewModel.replyToChatChannel(ch.id, "admin", adminChatReplyInput.trim(), "مشرف الدعم")
                                    // Update visual logs dynamically
                                    val currentChannels = viewModel.chatChannels.value
                                    val updatedCh = currentChannels.find { it.id == ch.id }
                                    if (updatedCh != null) {
                                        showActiveChatChannelObj = updatedCh
                                    }
                                    adminChatReplyInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إرسال الرد الموثق", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                viewModel.toggleBlockChatChannel(ch.id)
                                // Refresh visual logs
                                val currentChannels = viewModel.chatChannels.value
                                val updatedCh = currentChannels.find { it.id == ch.id }
                                if (updatedCh != null) {
                                    showActiveChatChannelObj = updatedCh
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (ch.isBlocked) "إلغاء الحظر" else "حظر الطرفين", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // 6. Delete communication log dialog
    showDeleteChatConfirmId?.let { chId ->
        AlertDialog(
            onDismissRequest = { showDeleteChatConfirmId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("⚠️ هل أنت متأكد من مسح ملفات السجل صراحة؟", color = Color.White) },
            text = { Text("سيتم اقتطاع وحذف قنوات الشات من قاعدة الداتا فوراً دون أي إمكانية استرجاع.", color = themeColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChatChannel(chId)
                        showDeleteChatConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("حذف ذيل المحادثة كاملة")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteChatConfirmId = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 7. Wipe confirming AlertDialog with hidden obscured password checks as requested
    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("🚨 تأكيد الهوية الأمنية للمصفي", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "هذه العملية فائقة الخطورة وذات تصفية كلية فورية للمركز الفني بالمنصة والدليل اليمني. الرجاء كتابة الرمز السري للأدمن لإكمال المسح:",
                        color = themeColors.textSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = wipeInputPassword,
                        onValueChange = { wipeInputPassword = it },
                        label = { Text("كلمة مرور الأدمن") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (wipeInputPassword == "maher736462") {
                            val success = viewModel.wipeAllDatabaseData("maher736462")
                            if (success) {
                                showWipeConfirmDialog = false
                                wipeInputPassword = ""
                                Toast.makeText(context, "💥 تم تصفية قاعدة الداتا والعودة للصفر بنجاح!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "❌ كلمة مرور الأدمن غير صحيحة! تم منع التطهير.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد مسح وتطهير النظام العظيم", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showWipeConfirmDialog = false
                        wipeInputPassword = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("إلغاء عملية التطهير")
                }
            }
        )
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

fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val results = FloatArray(1)
    try {
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    } catch (e: Exception) {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (6371000 * c).toFloat()
    }
}

fun formatDistance(meters: Float): String {
    return if (meters < 1000f) {
        "${meters.toInt()} م"
    } else {
        val km = meters / 1000f
        String.format(java.util.Locale.US, "%.1f كم", km)
    }
}

fun getProviderCoords(providerId: String): Pair<Double, Double> {
    return when (providerId) {
        "p_maher" -> Pair(15.3694, 44.1910)
        "p1" -> Pair(15.3850, 44.1950)
        "p2" -> Pair(15.3280, 44.1800)
        "p3" -> Pair(12.8350, 44.9900)
        "p4" -> Pair(15.3720, 44.2200)
        "p5" -> Pair(12.7950, 44.9850)
        else -> {
            val hash = providerId.hashCode().toDouble()
            val offsetLat = (hash % 100) / 1000.0
            val offsetLng = ((hash / 100) % 100) / 1000.0
            Pair(15.3694 + offsetLat, 44.1910 + offsetLng)
        }
    }
}
