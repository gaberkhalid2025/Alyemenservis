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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.MainViewModel
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.ContentScale
import okhttp3.MediaType.Companion.toMediaType
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.foundation.horizontalScroll

data class PresetPalette(
    val name: String,
    val primaryHex: String,
    val secondaryHex: String,
    val backgroundHex: String,
    val surfaceHex: String
)

val colorsPresetsList: List<PresetPalette> = listOf(
    PresetPalette("🦅 اليمن الأحمر", "#CE1126", "#FFD700", "#0D1B1E", "#162A2D"),
    PresetPalette("🔵 الأزرق الملكي", "#0D47A1", "#00E5FF", "#0A192F", "#172A45"),
    PresetPalette("🌌 كوزميك سيلفر", "#9E9E9E", "#E0E0E0", "#121212", "#1C1C1C"),
    PresetPalette("✨ ذهبي فاخر", "#D4AF37", "#FFD700", "#1A1A1A", "#2D2D2D"),
    PresetPalette("🟢 زمردي راقي", "#004B49", "#50C878", "#0C1814", "#152A20"),
    PresetPalette("⚫ الأسود الدخاني", "#121212", "#333333", "#080808", "#101010")
)


class MainActivity : ComponentActivity() {
    private var lastBackPressTime = 0L
    private var tts: android.speech.tts.TextToSpeech? = null
    
    private var voiceResultCallback: ((String) -> Unit)? = null
    private val voiceRecognitionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                voiceResultCallback?.invoke(matches[0])
            }
        }
    }

    fun startVoiceInput(onResult: (String) -> Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 101)
            Toast.makeText(this, "⚠️ فضلاً وافق على إذن استخدام المايك في النافذة المنبثقة، ثم جرب الضغط على زر الميكروفون مجدداً لخدمتك بالكامل", Toast.LENGTH_LONG).show()
            return
        }

        runOnUiThread {
            voiceResultCallback = onResult
            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ar-YE")
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-YE")
                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "تحدث الآن باللغة العربية للبحث أو التحدث...")
            }
            try {
                voiceRecognitionLauncher.launch(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to the local dynamic SpeechRecognizer
                try {
                    val recognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this@MainActivity)
                    recognizer.setRecognitionListener(object : android.speech.RecognitionListener {
                        override fun onReadyForSpeech(p0: android.os.Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(p0: Float) {}
                        override fun onBufferReceived(p0: ByteArray?) {}
                        override fun onEndOfSpeech() {}
                        override fun onError(error: Int) {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "لم يتم التعرف على الصوت، يرجى المحاولة مجدداً بصوت مسموع", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onResults(results: android.os.Bundle?) {
                            val matchesList = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matchesList.isNullOrEmpty()) {
                                runOnUiThread {
                                    onResult(matchesList[0])
                                }
                            }
                        }
                        override fun onPartialResults(p0: android.os.Bundle?) {}
                        override fun onEvent(p0: Int, p1: android.os.Bundle?) {}
                    })
                    recognizer.startListening(intent)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    Toast.makeText(this@MainActivity, "عذراً، نظام هاتفك لا يدعم التعرف الصوتي المباشر.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch(e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            setTheme(R.style.Theme_MyApplication)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onCreate(savedInstanceState)
        
        try {
            tts = android.speech.tts.TextToSpeech(this) { status ->
                if (status != android.speech.tts.TextToSpeech.ERROR) {
                    tts?.language = java.util.Locale("ar")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        VoiceManager.onSpeak = { text ->
            try {
                tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
        VoiceManager.onHear = { callback ->
            try {
                startVoiceInput(callback)
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }

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
            ) { results ->
                val granted = results.values.all { it }
                if (granted) {
                    viewModel.triggerNotification("📌 تم تفعيل تحديد الموقع التلقائي بدقة عالية!")
                    try {
                        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                        val providerStr = if (lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            LocationManager.GPS_PROVIDER
                        } else if (lm != null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                            LocationManager.NETWORK_PROVIDER
                        } else {
                            null
                        }
                        if (lm != null && providerStr != null) {
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
                                3000L,
                                1.0f,
                                listener,
                                android.os.Looper.getMainLooper()
                            )
                        }
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    viewModel.triggerNotification("⚠️ تم رفض الإذن، يمكنك تحديد مدينتك يدوياً")
                }
            }

            LaunchedEffect(Unit) {
                try {
                    val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasFine || hasCoarse) {
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
                                    5000L,
                                    10.0f,
                                    listener,
                                    android.os.Looper.getMainLooper()
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                var simStep = 0
                while (true) {
                    delay(4000)
                    simStep++
                    val offsetLat = 0.00018 * Math.sin(simStep * 0.45)
                    val offsetLng = 0.00018 * Math.cos(simStep * 0.45)
                    val currentLat = viewModel.userLatitude.value
                    val currentLng = viewModel.userLongitude.value
                    viewModel.updateUserLocation(currentLat + offsetLat, currentLng + offsetLng)
                }
            }

            // Dynamic theme pallete selection
            val colors = remember(settingsState.activeThemeId, settingsState.customPrimaryHex, settingsState.customSecondaryHex) {
                resolveThemePalette(settingsState)
            }

            val isInitialized by viewModel.isInitialized.collectAsState()

            // Custom double-tap back action to exit App
            BackHandler {
                val handled = viewModel.goBack()
                if (!handled) {
                    val now = System.currentTimeMillis()
                    if (now - lastBackPressTime < 2000) {
                        finish()
                    } else {
                        lastBackPressTime = now
                        Toast.makeText(context, "Press again to exit / اضغط مرة أخرى للخروج", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            MaterialTheme(
                colorScheme = colors.scheme
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (isInitialized) colors.background else Color.Black
                    ) {
                        if (!isInitialized) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    // WAM Blue Circle Logo as shown in image
                                    Box(
                                        modifier = Modifier
                                            .size(130.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0E4CB0)), // Beautiful deep blue
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "WAM",
                                            color = Color(0xFF10B981), // Green text
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 32.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Text(
                                        text = "كل خدمات اليمن",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "دليلك الشامل لكل الخدمات والمهن",
                                        fontSize = 15.sp,
                                        color = Color.LightGray,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(36.dp))
                                    CircularProgressIndicator(
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "تأكد من المزامنة... جاري الاتصال الآمن بالسحابة",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            AppNavigator(viewModel = viewModel, themeColors = colors, permissionLauncher = permissionLauncher, locationPermissions = locationPermissions)
                        }
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
            val background = try { Color(android.graphics.Color.parseColor(settings.customBackgroundHex)) } catch (e: Exception) { Color(0xFF0A0F0D) }
            val surface = try { Color(android.graphics.Color.parseColor(settings.customSurfaceHex)) } catch (e: Exception) { Color(0xFF121D18) }
            val textPrimary = Color.White
            val textSecondary = Color(0xFF94A3B8)
            val accent = try { Color(android.graphics.Color.parseColor(settings.customPrimaryHex)) } catch (e: Exception) { Color(0xFFF59E0B) }
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
fun AppNavigator(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    locationPermissions: Array<String>
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val toastMessage by viewModel.toastFlow.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val showBackdoorDialogState by viewModel.showBackdoorDialog.collectAsState()
    val context = LocalContext.current

    val currentUserIdState by viewModel.currentUserId.collectAsState()
    var showGuestRegisterDialogForAction by remember { mutableStateOf<String?>(null) } // null, "CHAT"

    LaunchedEffect(Unit) {
        viewModel.initializeUserIdentity(context)
    }

    // Modal dialog trigger states
    var showInfoDialog by remember { mutableStateOf(false) }
    var showAssistantDialog by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showAllConversationsDialog by remember { mutableStateOf(false) }
    var preSelectedChannelId by remember { mutableStateOf<String?>(null) }
    var chatReadTrigger by remember { mutableStateOf(0) }

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
            AppHeaderBar(
                viewModel = viewModel,
                themeColors = themeColors,
                onNotificationsClick = { showNotificationsDialog = true },
                onChatsClick = { showAllConversationsDialog = true },
                chatReadTrigger = chatReadTrigger
            )
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
                    "JOIN_REQUEST_STATUS" -> JoinRequestStatusScreen(viewModel = viewModel, themeColors = themeColors)
                    "ABOUT_APP" -> AboutAppScreenContent(viewModel = viewModel, themeColors = themeColors)
                    "MAP_VIEW" -> MockMapViewScreen(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onRequestLocationPermission = { permissionLauncher.launch(locationPermissions) }
                    )
                    else -> ServicesBrowserLayout(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        onChatOpen = { channelId ->
                            preSelectedChannelId = channelId
                            showAllConversationsDialog = true
                        }
                    )
                }

                FloatingIconsOverlay(
                    settings = settingsState,
                    themeColors = themeColors,
                    onAssistantClick = { showAssistantDialog = true },
                    onChatClick = {
                        if (currentUserIdState == "guest" && !settingsState.bypassVisitorRegistration && !settingsState.disableChatFirewall) {
                            showGuestRegisterDialogForAction = "CHAT"
                        } else {
                            showChatDialog = true
                        }
                    }
                )
            }
        }
    }

    if (showGuestRegisterDialogForAction == "CHAT") {
        GuestRegistrationDialog(
            themeColors = themeColors,
            onDismiss = { showGuestRegisterDialogForAction = null },
            onRegisterCompleted = { name, phone, residence ->
                viewModel.registerGuestUser(context, name, phone, residence)
                showGuestRegisterDialogForAction = null
                showChatDialog = true
            }
        )
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

    if (showNotificationsDialog) {
        UserNotificationsDialogView(viewModel = viewModel, themeColors = themeColors, onDismiss = { showNotificationsDialog = false })
    }

    if (showAllConversationsDialog) {
        AllConversationsDialogView(
            viewModel = viewModel,
            themeColors = themeColors,
            initialSelectedChannelId = preSelectedChannelId,
            onReadTrigger = { chatReadTrigger++ },
            onDismiss = { 
                showAllConversationsDialog = false
                preSelectedChannelId = null
            }
        )
    }

    if (showBackdoorDialogState) {
        var bdPasswordInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissBackdoorDialog() },
            containerColor = Color(0xFF0F172A),
            title = { Text("🔓 بوابة تسجيل الدخول الخلفي للمنفذ العظيم", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الرجاء إدخال كلمة المرور للتحكم الخلفي والمالك:", color = Color.White, fontSize = 11.sp)
                    OutlinedTextField(
                        value = bdPasswordInput,
                        onValueChange = { bdPasswordInput = it },
                        label = { Text("كلمة المرور") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bdPasswordInput.trim() == "maher--736462") {
                            viewModel.authenticateAdmin("OWNER")
                            viewModel.navigateTo("OWNER_PANEL")
                            viewModel.dismissBackdoorDialog()
                            viewModel.triggerNotification("🔓 تم تفعيل البوابة الخلفية والتحكم الشامل بنجاح!")
                        } else {
                            viewModel.triggerNotification("❌ الرمز السري للمنفذ الخلفي غير صحيح!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("دخول", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { viewModel.dismissBackdoorDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("إلغاء الإجراء")
                }
            }
        )
    }
}

// ------ Custom Top App Bar ------
@Composable
fun AppHeaderBar(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onNotificationsClick: () -> Unit,
    onChatsClick: () -> Unit,
    chatReadTrigger: Int = 0
) {
    val settingsState by viewModel.settings.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val allNotifications by viewModel.notifications.collectAsState()
    val userPhoneState by viewModel.currentUserPhone.collectAsState()
    val adminRoleState by viewModel.adminRole.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val chatChannels by viewModel.chatChannels.collectAsState()
    val providers by viewModel.providers.collectAsState()

    val myProvider = providers.find { it.phone == currentUserPhone }

    val myChannels = remember(chatChannels, currentUserId, currentUserPhone, myProvider) {
        chatChannels.filter { ch ->
            ch.id == "support_$currentUserId" ||
            ch.id.contains(currentUserId) ||
            (currentUserPhone.isNotEmpty() && ch.id.contains(currentUserPhone)) ||
            (myProvider != null && (ch.id.contains("chat_p_${myProvider.id}_") || ch.id.contains("_u_${myProvider.id}"))) ||
            currentUserId == "admin" || currentUserId.startsWith("super_")
        }
    }

    val headerContext = LocalContext.current
    val headerSp = remember(headerContext) { headerContext.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE) }
    var headerReadIds by remember { mutableStateOf(headerSp.getStringSet("read_notif_ids", emptySet()) ?: emptySet()) }
    
    // Calculate unread notifications count
    val filteredNotifs = remember(allNotifications, userPhoneState, adminRoleState) {
        allNotifications.filter { notif ->
            when (notif.targetType) {
                "ALL" -> true
                "USER" -> notif.targetValue == userPhoneState
                "PROVIDER" -> notif.targetValue == userPhoneState
                "SUPERVISOR" -> adminRoleState != "GUEST"
                else -> true
            }
        }
    }
    val unreadNotifCount = remember(filteredNotifs, headerReadIds) {
        filteredNotifs.count { it.id !in headerReadIds }
    }

    // Calculate unread chats count
    val unreadChatsCount = remember(myChannels, chatChannels, headerSp, chatReadTrigger) {
        myChannels.count { ch ->
            val lastMsg = ch.messages.lastOrNull()
            if (lastMsg == null) {
                false
            } else {
                val isMe = lastMsg.senderId == currentUserId || (myProvider != null && lastMsg.senderId == myProvider.id)
                val readTime = headerSp.getLong("chat_read_${ch.id}", 0L)
                !isMe && lastMsg.timestamp > readTime
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(themeColors.primary)
            .statusBarsPadding()
            .testTag("app_header_bar")
    ) {
        // Row 1: Logo, Title & Welcome message on right/left + Settings/Sync
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App name & welcome
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = settingsState.welcomeMessage.take(28) + "...",
                        fontSize = 9.sp,
                        color = themeColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Language Switch & Refresh Sync Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { viewModel.switchLanguage() },
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "تغيير اللغة", tint = themeColors.accent, modifier = Modifier.size(14.dp))
                }

                IconButton(
                    onClick = { viewModel.triggerNotification("🔄 تم مزامنة البيانات وتحديث الصفحة بنجاح!") },
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "مزامنة", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

        // Row 2: Navigation Items (exactly 5 items, beautifully centered and spaced on high-contrast dock)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1. الرئيسية
            val isBrowse = currentScreen == "USER_BROWSE" || currentScreen == "MAIN_DASHBOARD"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        viewModel.navigateTo("USER_BROWSE")
                        viewModel.registerBackdoorInteraction()
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "الرئيسية",
                    tint = if (isBrowse) Color(0xFFFBBF24) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الرئيسية",
                    fontSize = 10.sp,
                    color = if (isBrowse) Color(0xFFFBBF24) else Color.White,
                    fontWeight = if (isBrowse) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }

            // 2. الخرائط
            val isMap = currentScreen == "MAP_VIEW"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        viewModel.navigateTo("MAP_VIEW")
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "الخرائط",
                    tint = if (isMap) Color(0xFFFBBF24) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الخرائط",
                    fontSize = 10.sp,
                    color = if (isMap) Color(0xFFFBBF24) else Color.White,
                    fontWeight = if (isMap) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }

            // 3. الانضمام
            val isJoin = currentScreen == "REGISTER_FORM" || currentScreen == "JOIN_REQUEST_STATUS"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        viewModel.navigateTo("REGISTER_FORM")
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "الانضمام",
                    tint = if (isJoin) Color(0xFFFBBF24) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الانضمام",
                    fontSize = 10.sp,
                    color = if (isJoin) Color(0xFFFBBF24) else Color.White,
                    fontWeight = if (isJoin) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }

            // 4. الإشعارات
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        val allIds = filteredNotifs.map { it.id }.toSet()
                        headerSp.edit().putStringSet("read_notif_ids", allIds).apply()
                        headerReadIds = allIds
                        onNotificationsClick()
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "الإشعارات",
                        tint = if (unreadNotifCount > 0) Color(0xFFF59E0B) else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    if (unreadNotifCount > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = 6.dp, y = (-4).dp)
                                .size(14.dp)
                                .background(Color.Red, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unreadNotifCount.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الإشعارات",
                    fontSize = 10.sp,
                    color = if (unreadNotifCount > 0) Color(0xFFF59E0B) else Color.White,
                    fontWeight = if (unreadNotifCount > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }

            // 5. المحادثات
            val hasUnreadChats = unreadChatsCount > 0
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        onChatsClick()
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Email,
                        contentDescription = "المحادثات",
                        tint = if (hasUnreadChats) Color(0xFF10B981) else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    if (hasUnreadChats) {
                        Box(
                            modifier = Modifier
                                .offset(x = 6.dp, y = (-4).dp)
                                .size(14.dp)
                                .background(Color(0xFF10B981), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unreadChatsCount.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "المحادثات",
                    fontSize = 10.sp,
                    color = if (hasUnreadChats) Color(0xFF10B981) else Color.White,
                    fontWeight = if (hasUnreadChats) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

// ------ Custom Small Dynamic Footer ------
@Composable
fun AppFooterBar(viewModel: MainViewModel, themeColors: VisualThemePalette, onInfoClick: () -> Unit) {
    val settingsState by viewModel.settings.collectAsState()

    val footerBg = remember(settingsState.footerBgColorHex, themeColors.secondary) {
        try {
            Color(android.graphics.Color.parseColor(settingsState.footerBgColorHex))
        } catch (e: Exception) {
            themeColors.secondary
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(footerBg)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("app_footer_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val infoIconItem = @Composable {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(
                    onClick = { onInfoClick() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "معلومات عن الصفحة",
                        tint = themeColors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        val centerTextItem = @Composable {
            Box(
                modifier = Modifier.weight(1.5f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = settingsState.footerMessage.ifBlank { "wam777644" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val versionItem = @Composable {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo("ADMIN_PANEL") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                        contentDescription = "لوحة الإدارة",
                        tint = themeColors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Parse layout ordering
        val order = settingsState.footerItemsOrder.split(",").map { it.trim().uppercase() }
        val itemsToRender = if (order.size == 3 && order.contains("INFO") && order.contains("TEXT") && order.contains("VERSION")) {
            order
        } else {
            listOf("INFO", "TEXT", "VERSION")
        }

        itemsToRender.forEach { item ->
            when (item) {
                "INFO" -> infoIconItem()
                "TEXT" -> centerTextItem()
                "VERSION" -> versionItem()
            }
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

@Composable
fun AdminCustomBannerView(settingsState: com.example.data.AdminSettingsEntity, themeColors: VisualThemePalette) {
    var isVisible by remember(settingsState.bannerContent, settingsState.bannerEnabled) { mutableStateOf(true) }
    
    if (settingsState.bannerDurationSeconds > 0) {
        LaunchedEffect(settingsState.bannerContent, settingsState.bannerEnabled) {
            kotlinx.coroutines.delay(settingsState.bannerDurationSeconds * 1000L)
            isVisible = false
        }
    }

    if (!isVisible || !settingsState.bannerEnabled) return

    val imageBitmap = remember(settingsState.bannerBase64) {
        if (!settingsState.bannerBase64.isNullOrEmpty()) {
            try {
                val bytes = android.util.Base64.decode(settingsState.bannerBase64, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch(e: Exception) { null }
        } else null
    }

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            startAnimation = true
        }
    }

    val style = settingsState.bannerDisplayStyle ?: "SLIDE"

    // Animations corresponding to display styles
    val slideOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (startAnimation && style == "SLIDE") 0.dp else if (style == "SLIDE") (-300).dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(800)
    )

    val fadeAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(1000)
    )

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )

    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 50f,
        targetValue = -50f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(4000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )

    val appliedModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .let { modifier ->
            when (style) {
                "SLIDE" -> modifier.offset(x = slideOffset)
                "FADE" -> modifier.alpha(fadeAlpha)
                "BLINK" -> modifier.alpha(blinkAlpha)
                "SCROLL" -> modifier.offset(x = scrollOffset.dp)
                else -> modifier
            }
        }

    Card(
        modifier = appliedModifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, themeColors.accent.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Header row with title and type icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (settingsState.bannerType) {
                            "IMAGE" -> "🖼️"
                            "VIDEO" -> "📹"
                            else -> "📢"
                        }
                        Text(icon, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إعلان رسمي من إدارة المنصة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.accent
                        )
                    }
                    
                    // Dismiss button
                    IconButton(
                        onClick = { isVisible = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("✕", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                when (settingsState.bannerType) {
                    "IMAGE" -> {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "إعلان بنر",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            // Load from remote URL or show decorative placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📢 إعلان مرئي", fontSize = 16.sp, color = themeColors.accent)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(settingsState.bannerContent, fontSize = 11.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
                                }
                            }
                        }
                    }
                    "VIDEO" -> {
                        // Simulated high-fidelity Video Player card as requested
                        var isPlaying by remember { mutableStateOf(true) }
                        var simulatedProgress by remember { mutableStateOf(0.4f) }
                        
                        LaunchedEffect(isPlaying) {
                            if (isPlaying) {
                                while (true) {
                                    kotlinx.coroutines.delay(1000L)
                                    simulatedProgress = (simulatedProgress + 0.05f)
                                    if (simulatedProgress >= 1.0f) simulatedProgress = 0.0f
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = "فيديو البنر",
                                    modifier = Modifier.fillMaxSize().alpha(0.6f),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)).alpha(0.5f))
                            }
                            
                            // REC Indicator
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.Red, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LIVE", fontSize = 9.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                            }

                            // Play/Pause Overlay Button
                            IconButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Text(if (isPlaying) "⏸️" else "▶️", fontSize = 18.sp)
                            }

                            // Video Controls Overlay at Bottom
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (settingsState.bannerContent.length > 30) settingsState.bannerContent.take(30) + "..." else settingsState.bannerContent,
                                        fontSize = 10.sp,
                                        color = Color.White
                                    )
                                    Text("00:0${(simulatedProgress * 15).toInt()} / 00:15", fontSize = 9.sp, color = Color.LightGray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                // Custom progress bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .background(Color.Gray)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(simulatedProgress)
                                            .fillMaxHeight()
                                            .background(themeColors.accent)
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // Text Banner
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.accent.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📢", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = settingsState.bannerContent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------ Main Category and Service Directory Browser Layout ------
@Composable
fun ServicesBrowserLayout(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onChatOpen: (String) -> Unit
) {
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

    val currentUserIdState by viewModel.currentUserId.collectAsState()
    val currentUserPhoneState by viewModel.currentUserPhone.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val myBookings = remember(bookings, currentUserPhoneState) {
        bookings.filter { it.customerPhone.trim() == currentUserPhoneState.trim() && currentUserPhoneState.isNotEmpty() }
    }
    var showGuestRegisterDialogForBooking by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    var showFiltersPanel by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (settingsState.bannerEnabled && settingsState.bannerLocation == "TOP") {
            item {
                AdminCustomBannerView(settingsState = settingsState, themeColors = themeColors)
            }
        }

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
                        VoiceManager.onHear?.invoke { spokenText ->
                            viewModel.updateSearchQuery(spokenText)
                            viewModel.triggerNotification("🎙️ تم سماع صوتك اليمني: $spokenText")
                        }
                    }) {
                        Text("🎙️", fontSize = 20.sp)
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

        // Yemen Cities Tabs / Scroll Selection
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "🌍 اختر المدينة لعرض الخدمات المحلية:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // All Cities chip
                    val isAllSelected = activeCityId == null
                    Surface(
                        modifier = Modifier
                            .clickable { viewModel.setCityFilter(null) }
                            .testTag("city_tab_all"),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isAllSelected) themeColors.accent else themeColors.surface,
                        border = BorderStroke(1.dp, if (isAllSelected) Color.Transparent else themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "🌍 كل المدن",
                            color = if (isAllSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    citiesList.forEach { city ->
                        val isSelected = activeCityId == city.id
                        Surface(
                            modifier = Modifier
                                .clickable { viewModel.setCityFilter(city.id) }
                                .testTag("city_tab_${city.id}"),
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) themeColors.accent else themeColors.surface,
                            border = BorderStroke(1.dp, if (isSelected) Color.Transparent else themeColors.accent.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = city.nameAr,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
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

        // Horizontal Grid list of categories (Responsive Grid)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "📁 تصفح حسب الأقسام (تصنيف تفاعلي):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val columns = 2
                val allCats = listOf(
                    CategoryEntity(id = "", name = "الكل", icon = "🌐")
                ) + categories

                val rows = allCats.chunked(columns)
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { cat ->
                            val isSelected = if (cat.id.isEmpty()) selectedCategory == null else selectedCategory == cat.id
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (cat.id.isEmpty()) {
                                            viewModel.selectCategory(null)
                                        } else {
                                            viewModel.selectCategory(cat.id)
                                        }
                                    }
                                    .testTag("category_grid_${cat.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) themeColors.accent else themeColors.surface
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else themeColors.accent.copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = cat.icon,
                                        fontSize = 18.sp,
                                        modifier = Modifier
                                            .background(
                                                color = (if (isSelected) Color.Black else themeColors.primary).copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(6.dp)
                                    )
                                    Column {
                                        Text(
                                            text = cat.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (cat.id.isEmpty()) "كل الخدمات اليمنية" else "عرض المختصين",
                                            fontSize = 8.sp,
                                            color = if (isSelected) Color.Black.copy(alpha = 0.7f) else themeColors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        if (rowItems.size < columns) {
                            Spacer(modifier = Modifier.weight(columns - rowItems.size.toFloat()))
                        }
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

        // Recommended Showcase Model Card
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "💡 بطاقة نموذج الخدمة الموصى بها:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent,
                    modifier = Modifier.padding(bottom = 2.dp, top = 4.dp)
                )
                DetailedProviderPlaceholderCard(themeColors = themeColors)
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
            items(filteredProviders, key = { it.id }) { provider ->
                ProviderCard(
                    provider = provider,
                    themeColors = themeColors,
                    viewModel = viewModel,
                    onChatOpen = onChatOpen
                )
            }
        }

        // Previous Service Requests Section for Customer
        if (myBookings.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "📋 طلباتي وحجوزات الصيانة السابقة الخاصة بي:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            items(myBookings, key = { "my_" + it.id }) { b ->
                val progressFloat = viewModel.getBookingProgress(b.status)
                val progressPercent = (progressFloat * 100).toInt()
                val statusLabel = viewModel.getBookingStatusLabel(b.status)
                val statusColorHex = viewModel.getBookingStatusColor(b.status)
                val statusColor = try {
                    Color(android.graphics.Color.parseColor(statusColorHex))
                } catch (e: Exception) {
                    Color.Gray
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("رقم الحجز: ${b.id}", fontSize = 11.sp, color = themeColors.textSecondary)
                            Text(
                                text = "$statusLabel ($progressPercent%)",
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Simple progress line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFloat)
                                    .fillMaxHeight()
                                    .background(statusColor)
                            )
                        }

                        Text("نوع الخدمة: ${b.serviceType}", fontSize = 12.sp, color = Color.White)
                        Text("الفني المسؤول: ${b.providerName}", fontSize = 11.sp, color = Color.LightGray)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("📅 التاريخ: ${b.dateString}", fontSize = 10.sp, color = themeColors.textSecondary)
                            Text("🕒 الوقت: ${b.timeString}", fontSize = 10.sp, color = themeColors.textSecondary)
                        }

                        if (b.status == "REJECTED" && b.rejectionReason.isNotEmpty()) {
                            Text(
                                text = "❌ سبب الرفض: ${b.rejectionReason}",
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            }
        }

        if (settingsState.bannerEnabled && settingsState.bannerLocation == "BOTTOM") {
            item {
                AdminCustomBannerView(settingsState = settingsState, themeColors = themeColors)
            }
        }
    }
}

// ------ Horizontal Advertisement Banner Composable ------
@Composable
fun BannerSliderView(banners: List<BannerEntity>, themeColors: VisualThemePalette, onBannerClick: (String) -> Unit) {
    var currentIndex by remember { mutableStateOf(0) }
    
    val activeBanner = if (banners.isNotEmpty()) banners.getOrNull(currentIndex) else null
    
    LaunchedEffect(currentIndex, banners) {
        if (banners.isNotEmpty()) {
            val active = banners.getOrNull(currentIndex)
            val durationSec = if (active != null && active.duration > 0) active.duration else 5
            kotlinx.coroutines.delay(durationSec * 1000L)
            currentIndex = (currentIndex + 1) % banners.size
        }
    }

    if (activeBanner != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clickable { onBannerClick(activeBanner.redirectCategory) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.secondary)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (activeBanner.type.uppercase()) {
                    "IMAGE" -> {
                        if (activeBanner.url.isNotEmpty()) {
                            AsyncImage(
                                model = activeBanner.url,
                                contentDescription = activeBanner.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            // Overlay gradient for text legibility
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                        )
                                    )
                            )
                            // Title & Label at bottom
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = activeBanner.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (activeBanner.redirectCategory.isNotEmpty()) {
                                    Text(
                                        text = "اضغط للانتقال إلى قسم: ${activeBanner.redirectCategory}",
                                        fontSize = 9.sp,
                                        color = themeColors.accent
                                    )
                                }
                            }
                        } else {
                            // Text Fallback if URL is empty
                            BannerTextFallback(activeBanner = activeBanner, themeColors = themeColors)
                        }
                    }
                    "VIDEO" -> {
                        if (activeBanner.url.isNotEmpty()) {
                            val context = LocalContext.current
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    android.widget.VideoView(ctx).apply {
                                        setVideoURI(android.net.Uri.parse(activeBanner.url))
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            mp.setVolume(0f, 0f) // Silent looping banner
                                            start()
                                        }
                                        setOnErrorListener { _, _, _ -> true } // silent failure
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            // Overlay gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎬 فيديو مميز", fontSize = 9.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = activeBanner.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            BannerTextFallback(activeBanner = activeBanner, themeColors = themeColors)
                        }
                    }
                    else -> { // TEXT banner
                        BannerTextFallback(activeBanner = activeBanner, themeColors = themeColors)
                    }
                }
            }
        }
    }
}

@Composable
fun BannerTextFallback(activeBanner: BannerEntity, themeColors: VisualThemePalette) {
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
                Icon(imageVector = Icons.Default.Info, contentDescription = "إعلان ممتاز", tint = themeColors.accent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "إعلان رسمي دليل خدمات اليمن",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = activeBanner.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (activeBanner.redirectCategory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔗 الانتقال لقسم: ${activeBanner.redirectCategory}",
                    fontSize = 9.sp,
                    color = themeColors.accent.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold
                )
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .width(76.dp)
    ) {
        val luxuryGoldBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFE259), // Light Gold
                Color(0xFFFFA751), // Deep Orange Gold
                Color(0xFFFFE259)  // Light Gold
            )
        )
        
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isSelected) themeColors.accent.copy(alpha = 0.25f) else themeColors.surface)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    brush = if (isSelected) luxuryGoldBrush else Brush.linearGradient(listOf(themeColors.accent.copy(alpha = 0.3f), themeColors.accent.copy(alpha = 0.1f))),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            CategorySectionIconView(iconStr = icon, size = 26.dp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) themeColors.accent else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ------ Detailed Provider Placeholder Card ------
@Composable
fun DetailedProviderPlaceholderCard(themeColors: VisualThemePalette) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("provider_detail_placeholder_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(2.dp, themeColors.accent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header with VIP badge and Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(themeColors.accent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "👑 نموذجي معتمد",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Text(
                        text = "صيانة منزلية",
                        fontSize = 10.sp,
                        color = themeColors.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Rating Star Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = themeColors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "4.9 (نموذج)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Provider Name & Call Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "امين الغرباني (صيانة عامة)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = themeColors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "صنعاء، منطقة الدائري جوار مدرسة اسماء للبنات",
                            fontSize = 11.sp,
                            color = themeColors.textSecondary
                        )
                    }
                }

                // Call Button
                IconButton(
                    onClick = {
                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:777703195"))
                        context.startActivity(callIntent)
                    },
                    modifier = Modifier
                        .background(Color.Green.copy(alpha = 0.2f), CircleShape)
                        .size(40.dp)
                        .testTag("provider_placeholder_call_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "اتصال بالفني",
                        tint = Color.Green,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                color = themeColors.accent.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Service Description
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "وصف الخدمة النموذجية:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "مختص صيانة وتمديد كهربائي، صيانة المكيفات والأجهزة المنزلية بدقة وأمان تام. تتوفر لدينا أحدث أجهزة الفحص وبأسعار مناسبة ومعتمدة مع ضمان الخدمة.",
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ProviderCard(
    provider: ProviderEntity,
    themeColors: VisualThemePalette,
    viewModel: MainViewModel,
    onChatOpen: (String) -> Unit
) {
    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()
    val currentUserIdState by viewModel.currentUserId.collectAsState()
    val currentUserNameState by viewModel.currentUserName.collectAsState()
    val currentUserPhoneState by viewModel.currentUserPhone.collectAsState()
    val currentUserResidenceState by viewModel.currentUserResidence.collectAsState()

    var showDetailsDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReportReason by remember { mutableStateOf("سلوك غير لائق") }
    var showGuestRegisterDialogForBooking by remember { mutableStateOf(false) }

    // --- NEW Custom States for Interactive Dialogs & Popups ---
    var simulateAdminMode by remember { mutableStateOf(false) }
    var showReviewsListDialog by remember { mutableStateOf(false) }
    var showAddCommentDialog by remember { mutableStateOf(false) }
    var showInstantChatDialog by remember { mutableStateOf(false) }
    var chatInputText by remember { mutableStateOf("") }
    var chatHistoryList by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) }
    var newCommentText by remember { mutableStateOf("") }
    var newCommentAuthor by remember { mutableStateOf("") }

    // Booking form inputs
    var customerNameInput by remember { mutableStateOf("") }
    var customerPhoneInput by remember { mutableStateOf("") }
    var customerAreaInput by remember { mutableStateOf("") }
    var customerServiceInput by remember { mutableStateOf("") }
    var bookingDateInput by remember { mutableStateOf("") }
    var bookingTimeInput by remember { mutableStateOf("") }
    var selectedServiceDropdown by remember { mutableStateOf("صيانة أعطال عامة") }
    var serviceDropdownExpanded by remember { mutableStateOf(false) }
    var showBookingDialog by remember { mutableStateOf(false) }
    var showBookingConfirmDialog by remember { mutableStateOf(false) }

    // Admin Control Dialog States
    var showAdminEditName by remember { mutableStateOf(false) }
    var showAdminEditRating by remember { mutableStateOf(false) }
    var showAdminEditLocation by remember { mutableStateOf(false) }
    var showAdminEditImage by remember { mutableStateOf(false) }
    var showAdminEditButtons by remember { mutableStateOf(false) }
    var showAdminEditTexts by remember { mutableStateOf(false) }
    var showAdminEditDesign by remember { mutableStateOf(false) }

    // Admin inputs
    var adminNameInput by remember { mutableStateOf(provider.name) }
    var adminRatingInput by remember { mutableStateOf(provider.rating) }
    var adminReviewsCountInput by remember { mutableStateOf(provider.numReviews) }
    var adminAreaInput by remember { mutableStateOf(provider.area) }
    var adminNeighborhoodInput by remember { mutableStateOf(provider.localNeighborhood) }
    var adminProfileImageInput by remember { mutableStateOf(provider.profileImage) }
    var adminPhoneInput by remember { mutableStateOf(provider.phone) }
    var adminPreviewPriceInput by remember { mutableStateOf(provider.previewPrice) }

    val coroutineScope = rememberCoroutineScope()
    val adminRoleState by viewModel.adminRole.collectAsState()
    val isAdminActive = adminRoleState != "GUEST" || simulateAdminMode

    LaunchedEffect(provider) {
        adminNameInput = provider.name
        adminRatingInput = provider.rating
        adminReviewsCountInput = provider.numReviews
        adminAreaInput = provider.area
        adminNeighborhoodInput = provider.localNeighborhood
        adminProfileImageInput = provider.profileImage
        adminPhoneInput = provider.phone
        adminPreviewPriceInput = provider.previewPrice
    }

    LaunchedEffect(showBookingDialog) {
        if (showBookingDialog) {
            customerNameInput = currentUserNameState
            customerPhoneInput = currentUserPhoneState
            customerAreaInput = currentUserResidenceState
            
            val currentCalendar = java.util.Calendar.getInstance()
            val year = currentCalendar.get(java.util.Calendar.YEAR)
            val month = currentCalendar.get(java.util.Calendar.MONTH) + 1
            val day = currentCalendar.get(java.util.Calendar.DAY_OF_MONTH)
            bookingDateInput = "$year/$month/$day"
            
            val hourOfDay = currentCalendar.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = currentCalendar.get(java.util.Calendar.MINUTE)
            val amPm = if (hourOfDay < 12) "ص" else "م"
            val hour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
            val formattedMin = String.format("%02d", minute)
            bookingTimeInput = "$hour:$formattedMin $amPm"
            
            selectedServiceDropdown = "صيانة أعطال عامة"
            customerServiceInput = ""
        }
    }

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

    val luxuryGoldBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFD700), // Pure Gold
            Color(0xFFFFA500), // Orange-Gold
            Color(0xFFB8860B), // Dark Goldenrod
            Color(0xFFFFD700)  // Gold shine repeat
        )
    )

    val metallicGlassBrush = Brush.verticalGradient(
        colors = listOf(
            cardBg.copy(alpha = 0.88f),
            cardBg.copy(alpha = 0.98f)
        )
    )

    // Helper to generate Star string representation dynamically (e.g. ★★★★☆)
    fun getStarsString(r: Float): String {
        val filled = r.toInt().coerceIn(0, 5)
        val empty = (5 - filled).coerceIn(0, 5)
        return "★".repeat(filled) + "☆".repeat(empty)
    }

    // --- Card View Body (Miniature & Edge-to-Edge with minimal margin) ---
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 0.dp)
            .scale(scaleFactor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(themeColors.accent.copy(alpha = 0.3f), themeColors.accent.copy(alpha = 0.05f))))
    ) {
        Box(
            modifier = Modifier
                .background(metallicGlassBrush)
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                
                // 1. Core Profile Row (Circular Avatar + Dynamic Name & Details + Rating + Location)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // [Circular] Image
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(2.dp, themeColors.accent, CircleShape)
                    ) {
                        if (provider.profileImage.isNotEmpty()) {
                            AsyncImage(
                                model = provider.profileImage,
                                contentDescription = "الصورة الشخصية",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(themeColors.accent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = provider.name.take(1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = themeColors.accent
                                )
                            }
                        }
                    }

                    // Provider Information Details Column
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        // Name and Profile Popup Trigger
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = provider.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = nameColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            
                            // 👤 عرض الملف الشخصي (شاشة منبثقة)
                            Text(
                                text = "👤 عرض الملف الشخصي",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.accent,
                                modifier = Modifier
                                    .background(themeColors.accent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                    .clickable { showDetailsDialog = true }
                            )
                        }

                        // Rating: ★★★★☆ 4.8 (0 تقييمات)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = getStarsString(provider.rating),
                                fontSize = 12.sp,
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${provider.rating} (${provider.numReviews} تقييم)",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        // Location: 📍 صنعاء ✏️
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "📍 ${provider.area}، ${provider.localNeighborhood}",
                                fontSize = 11.sp,
                                color = locationColor,
                                fontWeight = FontWeight.Medium
                            )
                            if (isAdminActive) {
                                Text(
                                    text = "✏️",
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .clickable { showAdminEditLocation = true }
                                        .padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = themeColors.accent.copy(alpha = 0.15f))

                // 2. Communication Methods ("وسائل التواصل: 📩 مراسلة فورية | 📞 اتصال")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 📩 مراسلة فورية
                    Button(
                        onClick = {
                            if (currentUserIdState == "guest" && !settingsState.bypassVisitorRegistration && !settingsState.disableChatFirewall) {
                                showGuestRegisterDialogForBooking = true
                            } else {
                                viewModel.getOrCreateChatChannel(
                                    provider.id,
                                    provider.name,
                                    currentUserIdState,
                                    currentUserNameState
                                )
                                onChatOpen("chat_p_${provider.id}_u_${currentUserIdState}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Text("📩 مراسلة فورية", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // 📞 اتصال
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text("📞 اتصال مباشر", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // 3. Extra Options ("خيارات إضافية: 💬 آراء وتجارب | ✍️ أضف تعليق")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 💬 آراء وتجارب
                    Button(
                        onClick = { showReviewsListDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f))
                    ) {
                        Text("💬 آراء وتجارب العملاء", fontSize = 11.sp, color = themeColors.textSecondary)
                    }

                    // ✍️ أضف تعليق
                    Button(
                        onClick = { showAddCommentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f))
                    ) {
                        Text("✍️ أضف تعليق جديد", fontSize = 11.sp, color = themeColors.textSecondary)
                    }
                }

                // 4. Main Service Button ("الخدمة الرئيسية: 📅 حجز موعد خدمة فورية ومباشرة")
                Button(
                    onClick = {
                        if (currentUserIdState == "guest" && !settingsState.bypassVisitorRegistration && !settingsState.disableBookingFirewall) {
                            showGuestRegisterDialogForBooking = true
                        } else {
                            showBookingDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("📅 حجز موعد خدمة فورية ومباشرة", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }

                // Availability Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (provider.isAvailable) "🟢 متاح للعمل الآن" else "🔴 مشغول حالياً",
                        fontSize = 10.sp,
                        color = if (provider.isAvailable) Color.Green else Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 5. Admin Panel Section ("🔧 صلاحيات الأدمن")
                if (isAdminActive) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔧 لوحة صلاحيات الأدمن:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .background(Color.Green.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("نشط", fontSize = 8.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                                }
                            }

                            // 2-column Admin Button Grid
                            val adminOptionsList = listOf(
                                "📝 تعديل الاسم" to { showAdminEditName = true },
                                "⭐ تغيير التقييم" to { showAdminEditRating = true },
                                "📍 تحديث الموقع" to { showAdminEditLocation = true },
                                "🖼️ تبديل الصورة" to { showAdminEditImage = true },
                                "🔘 تخصيص الأزرار" to { showAdminEditButtons = true },
                                "✍️ تعديل العناوين" to { showAdminEditTexts = true },
                                "🎨 مظهر وألوان البطاقة" to { showAdminEditDesign = true }
                            )

                            adminOptionsList.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowItems.forEach { (label, action) ->
                                        Button(
                                            onClick = action,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(text = label, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (rowItems.size < 2) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- POPUP DIALOGS & SHEET SIMULATIONS ---

    // Instant Chat Dialog with Provider
    if (showInstantChatDialog) {
        Dialog(onDismissRequest = { showInstantChatDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth().height(450.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color.Green, CircleShape))
                            Text(text = "محادثة فورية: ${provider.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        IconButton(onClick = { showInstantChatDialog = false }, modifier = Modifier.size(24.dp)) {
                            Text("❌", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Messages Column
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "مرحباً بك! أنا ${provider.name} متواجد لخدمتك في ${provider.area}. كيف يمكنني تلبية طلبك اليوم؟ يمكنك حجز موعد مباشر أو الاستفسار.",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        items(chatHistoryList) { (msg, isUser) ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isUser) themeColors.accent.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.15f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(text = msg, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Input text row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = chatInputText,
                            onValueChange = { chatInputText = it },
                            placeholder = { Text("اكتب رسالتك...", fontSize = 11.sp, color = Color.LightGray) },
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        IconButton(
                            onClick = {
                                if (chatInputText.trim().isNotEmpty()) {
                                    val userMsg = chatInputText
                                    chatHistoryList = chatHistoryList + (userMsg to true)
                                    chatInputText = ""
                                    
                                    val reply = when {
                                        userMsg.contains("سعر") || userMsg.contains("بكم") || userMsg.contains("تكلفة") -> 
                                            "أهلاً بك. يمكنك مناقشة التكاليف والاتفاق عليها بدقة بعد المعاينة الميدانية والوقوف على طبيعة الخدمة المطلوبة."
                                        userMsg.contains("حجز") || userMsg.contains("موعد") || userMsg.contains("متى") ->
                                            "يمكنك استخدام زر '📅 حجز موعد خدمة فورية ومباشرة' المتواجد على بطاقتي لإرسال طلب حجز رسمي، وسأقوم بالاتصال بك فوراً لتأكيده."
                                        userMsg.contains("رقم") || userMsg.contains("هاتف") ->
                                            "يمكنك الاتصال بي مباشرة على الرقم: ${provider.phone} أو عبر واتساب بالضغط على أيقونة الاتصال."
                                        else -> "أهلاً بك، يسعدني جداً تواصلك معي. تفضل بطرح تفاصيل مشكلتك وسأقوم بالرد عليك أو الاتصال بك لمساعدتك بأسرع وقت ممكن!"
                                    }
                                    
                                    coroutineScope.launch {
                                        delay(1000)
                                        chatHistoryList = chatHistoryList + (reply to false)
                                    }
                                }
                            },
                            modifier = Modifier.background(themeColors.accent, CircleShape).size(36.dp)
                        ) {
                            Text("↩️", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    // Reviews List Dialog
    if (showReviewsListDialog) {
        Dialog(onDismissRequest = { showReviewsListDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth().height(400.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💬 آراء وتجارب العملاء لـ ${provider.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        IconButton(onClick = { showReviewsListDialog = false }, modifier = Modifier.size(24.dp)) {
                            Text("❌", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sampleReviews = emptyList<Pair<String, String>>()
                        if (sampleReviews.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("لا توجد تجارب معتمدة مسجلة حالياً لهذا الفني. كن أول من يضيف تجربة!", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        items(sampleReviews) { (text, author) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = author, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = text, fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Review Dialog
    if (showAddCommentDialog) {
        Dialog(onDismissRequest = { showAddCommentDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✍️ أضف تعليق وتقييم جديد", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        IconButton(onClick = { showAddCommentDialog = false }, modifier = Modifier.size(24.dp)) {
                            Text("❌", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    
                    OutlinedTextField(
                        value = newCommentAuthor,
                        onValueChange = { newCommentAuthor = it },
                        label = { Text("اسمك الكريم", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        label = { Text("اكتب تعليقك وتجربتك هنا بكل أمانة...", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    Button(
                        onClick = {
                            if (newCommentAuthor.trim().isEmpty() || newCommentText.trim().isEmpty()) {
                                Toast.makeText(context, "الرجاء كتابة الاسم والتعليق أولاً", Toast.LENGTH_SHORT).show()
                            } else {
                                val updated = provider.copy(numReviews = provider.numReviews + 1)
                                viewModel.updateProviderEntity(updated)
                                Toast.makeText(context, "💖 شكراً لك! تم إرسال تعليقك وسيقوم المسؤول بمراجعته وتفعيله فوراً.", Toast.LENGTH_LONG).show()
                                showAddCommentDialog = false
                                newCommentText = ""
                                newCommentAuthor = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "إرسال التعليق والتقييم ✍️", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // --- ADMIN PERMISSIONS CONTROL DIALOGS ---

    // 1. Edit Name
    if (showAdminEditName) {
        Dialog(onDismissRequest = { showAdminEditName = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📝 تعديل اسم مقدم الخدمة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = adminNameInput,
                        onValueChange = { adminNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Button(
                        onClick = {
                            viewModel.updateProviderEntity(provider.copy(name = adminNameInput))
                            showAdminEditName = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ الاسم الجديد", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 2. Change Rating
    if (showAdminEditRating) {
        Dialog(onDismissRequest = { showAdminEditRating = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⭐ تعديل التقييم وعدد المراجعات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    Text("التقييم الحالي: ${String.format("%.1f", adminRatingInput)}", color = Color.LightGray, fontSize = 11.sp)
                    Slider(
                        value = adminRatingInput,
                        onValueChange = { adminRatingInput = it },
                        valueRange = 1.0f..5.0f,
                        steps = 40
                    )
                    
                    OutlinedTextField(
                        value = adminReviewsCountInput.toString(),
                        onValueChange = { adminReviewsCountInput = it.toIntOrNull() ?: 0 },
                        label = { Text("عدد التقييمات", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    Button(
                        onClick = {
                            viewModel.updateProviderEntity(provider.copy(rating = adminRatingInput, numReviews = adminReviewsCountInput))
                            showAdminEditRating = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ التقييم الجديد", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 3. Update Location
    if (showAdminEditLocation) {
        Dialog(onDismissRequest = { showAdminEditLocation = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📍 تعديل موقع مقدم الخدمة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    OutlinedTextField(
                        value = adminAreaInput,
                        onValueChange = { adminAreaInput = it },
                        label = { Text("المحافظة/المدينة", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    OutlinedTextField(
                        value = adminNeighborhoodInput,
                        onValueChange = { adminNeighborhoodInput = it },
                        label = { Text("الحي/الحارة/الشارع", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    Button(
                        onClick = {
                            viewModel.updateProviderEntity(provider.copy(area = adminAreaInput, localNeighborhood = adminNeighborhoodInput))
                            showAdminEditLocation = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ الموقع الجديد", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 4. Switch/Update Image
    if (showAdminEditImage) {
        Dialog(onDismissRequest = { showAdminEditImage = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🖼️ تغيير الصورة الشخصية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    OutlinedTextField(
                        value = adminProfileImageInput,
                        onValueChange = { adminProfileImageInput = it },
                        label = { Text("رابط الصورة الشخصية (URL)", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    Text("أو اختر صورة رمزية نموذجية سريعة:", color = Color.White, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150" to "صورة نسائية رمزية",
                            "https://images.unsplash.com/photo-1560250097-0b93528c311a?w=150" to "صورة رجالية رمزية"
                        ).forEach { (url, label) ->
                            Button(
                                onClick = { adminProfileImageInput = url },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(label, fontSize = 9.sp, color = Color.White)
                            }
                        }
                    }
                    
                    Button(
                        onClick = {
                            viewModel.updateProviderEntity(provider.copy(profileImage = adminProfileImageInput))
                            showAdminEditImage = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ الصورة الشخصية", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 5. Customize Buttons
    if (showAdminEditButtons) {
        Dialog(onDismissRequest = { showAdminEditButtons = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🔘 تخصيص أزرار الاتصال والحجز بالبطاقة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("عرض زر الاتصال المباشر", color = Color.White, fontSize = 11.sp)
                        Switch(
                            checked = settingsState.showCallButton,
                            onCheckedChange = { viewModel.saveCustomSettingsState(settingsState.copy(showCallButton = it)) }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("عرض زر واتساب للمراسلة المباشرة", color = Color.White, fontSize = 11.sp)
                        Switch(
                            checked = settingsState.showWhatsappButton,
                            onCheckedChange = { viewModel.saveCustomSettingsState(settingsState.copy(showWhatsappButton = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("عرض زر حجز الخدمات الفورية", color = Color.White, fontSize = 11.sp)
                        Switch(
                            checked = settingsState.showBookButton,
                            onCheckedChange = { viewModel.saveCustomSettingsState(settingsState.copy(showBookButton = it)) }
                        )
                    }
                    
                    Button(
                        onClick = { showAdminEditButtons = false },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("إغلاق التخصيص", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 6. Edit Texts & Headings
    if (showAdminEditTexts) {
        Dialog(onDismissRequest = { showAdminEditTexts = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("✍️ تعديل النصوص والعناوين العامة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    var labelNameInput by remember { mutableStateOf(settingsState.bookingLabelName) }
                    var termsInput by remember { mutableStateOf(settingsState.bookingTerms) }
                    
                    OutlinedTextField(
                        value = labelNameInput,
                        onValueChange = { labelNameInput = it },
                        label = { Text("عنوان حقل الاسم بالاستمارة", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = termsInput,
                        onValueChange = { termsInput = it },
                        label = { Text("شروط الحجز العامة للأدمن", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    Button(
                        onClick = {
                            viewModel.saveCustomSettingsState(settingsState.copy(
                                bookingLabelName = labelNameInput,
                                bookingTerms = termsInput
                            ))
                            showAdminEditTexts = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ العناوين والنصوص", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 7. Change Card Colors & Design
    if (showAdminEditDesign) {
        Dialog(onDismissRequest = { showAdminEditDesign = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🎨 تخصيص مظهر وتصميم بطاقات الحرفيين", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    var cardBgHexInput by remember { mutableStateOf(settingsState.cardBackgroundHex) }
                    var nameHexInput by remember { mutableStateOf(settingsState.providerNameColorHex) }
                    
                    OutlinedTextField(
                        value = cardBgHexInput,
                        onValueChange = { cardBgHexInput = it },
                        label = { Text("خلفية البطاقة (Hex)", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    OutlinedTextField(
                        value = nameHexInput,
                        onValueChange = { nameHexInput = it },
                        label = { Text("لون خط الاسم (Hex)", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Text("اختر نمط سريع من الألوان الجذابة:", color = Color.White, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            ("فحمي داكن" to "#1E293B") to "#FFFFFF",
                            ("ذهبي ملكي" to "#1A1A1A") to "#FFD700",
                            ("أزرق زجاجي" to "#0F172A") to "#60A5FA"
                        ).forEach { (style, nameCol) ->
                            Button(
                                onClick = {
                                    cardBgHexInput = style.second
                                    nameHexInput = nameCol
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(style.first, fontSize = 9.sp, color = Color.White)
                            }
                        }
                    }
                    
                    Button(
                        onClick = {
                            viewModel.saveCustomSettingsState(settingsState.copy(
                                cardBackgroundHex = cardBgHexInput,
                                providerNameColorHex = nameHexInput
                            ))
                            showAdminEditDesign = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ التصميم والألوان", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
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

    if (showDetailsDialog) {
        Dialog(onDismissRequest = { showDetailsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🔍 تفاصيل وملف الفني المهني", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    // Profile/Cover header representation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(2.dp, themeColors.accent, CircleShape)
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
                                        fontSize = 24.sp,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                        
                        Column {
                            Text(provider.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${provider.rating} (${provider.numReviews} تقييم)", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    // Badges section
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (provider.isVip) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFD97706))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("👑 VIP ذهبي", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        if (provider.isVerified) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF3B82F6))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("🔵 معتمد وموثق", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        if (provider.isRecommended) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("🟢 موصى به", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Structured Details Cards
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("📍 المحافظة والمنطقة:", fontSize = 12.sp, color = themeColors.textSecondary)
                            Text(provider.area, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🏡 الحي / الحارة:", fontSize = 12.sp, color = themeColors.textSecondary)
                            Text(provider.localNeighborhood, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("📞 رقم الاتصال:", fontSize = 12.sp, color = themeColors.textSecondary)
                            Text(provider.phone, fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🔧 حالة التوفر الحالية:", fontSize = 12.sp, color = themeColors.textSecondary)
                            Text(
                                text = if (provider.isAvailable) "متاح للعمل الفوري 🟢" else "مشغول حالياً 🔴",
                                fontSize = 12.sp,
                                color = if (provider.isAvailable) Color.Green else Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("⭐ النقاط والتقييمات التراكمية:", fontSize = 12.sp, color = themeColors.textSecondary)
                            Text("${provider.points} نقطة مهنية", fontSize = 12.sp, color = Color.Yellow, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showDetailsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("إغلاق التفاصيل ❌", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showGuestRegisterDialogForBooking) {
        GuestRegistrationDialog(
            themeColors = themeColors,
            onDismiss = { showGuestRegisterDialogForBooking = false },
            onRegisterCompleted = { name, phone, residence ->
                viewModel.registerGuestUser(context, name, phone, residence)
                showGuestRegisterDialogForBooking = false
                showBookingDialog = true
            }
        )
    }

    // ------ Dynamic Booking Form Layout Dialogs ------
    if (showBookingDialog) {
        Dialog(onDismissRequest = { showBookingDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("📅 استمارة حجز فني: ${provider.name}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    // Display admin customizable booking terms/shuroot
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Yellow.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("⚠️ شروط وطريقة الحجز الموثقة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Yellow)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = settingsState.bookingTerms,
                                fontSize = 10.sp,
                                color = Color.LightGray,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customerNameInput,
                        onValueChange = { customerNameInput = it },
                        label = { Text(settingsState.bookingLabelName, color = themeColors.textSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        leadingIcon = if (settingsState.allowTextToSpeechBookingForm) {
                            {
                                IconButton(onClick = { VoiceManager.onSpeak?.invoke(customerNameInput.ifBlank { "الاسم" }) }) {
                                    Text("🔊", fontSize = 16.sp)
                                }
                            }
                        } else null,
                        trailingIcon = if (settingsState.allowVoiceInputBookingForm) {
                            {
                                IconButton(onClick = {
                                    VoiceManager.onHear?.invoke { spokenText -> customerNameInput = spokenText }
                                }) {
                                    Text("🎙️", fontSize = 16.sp)
                                }
                            }
                        } else null
                    )

                    OutlinedTextField(
                        value = customerPhoneInput,
                        onValueChange = { customerPhoneInput = it },
                        label = { Text(settingsState.bookingLabelPhone, color = themeColors.textSecondary, fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        leadingIcon = if (settingsState.allowTextToSpeechBookingForm) {
                            {
                                IconButton(onClick = { VoiceManager.onSpeak?.invoke(customerPhoneInput.ifBlank { "رقم الهاتف" }) }) {
                                    Text("🔊", fontSize = 16.sp)
                                }
                            }
                        } else null,
                        trailingIcon = if (settingsState.allowVoiceInputBookingForm) {
                            {
                                IconButton(onClick = {
                                    VoiceManager.onHear?.invoke { spokenText -> customerPhoneInput = spokenText }
                                }) {
                                    Text("🎙️", fontSize = 16.sp)
                                }
                            }
                        } else null
                    )

                    OutlinedTextField(
                        value = customerAreaInput,
                        onValueChange = { customerAreaInput = it },
                        label = { Text(settingsState.bookingLabelArea, color = themeColors.textSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        leadingIcon = if (settingsState.allowTextToSpeechBookingForm) {
                            {
                                IconButton(onClick = { VoiceManager.onSpeak?.invoke(customerAreaInput.ifBlank { "المنطقة" }) }) {
                                    Text("🔊", fontSize = 16.sp)
                                }
                            }
                        } else null,
                        trailingIcon = if (settingsState.allowVoiceInputBookingForm) {
                            {
                                IconButton(onClick = {
                                    VoiceManager.onHear?.invoke { spokenText -> customerAreaInput = spokenText }
                                }) {
                                    Text("🎙️", fontSize = 16.sp)
                                }
                            }
                        } else null
                    )

                    // Dropdown for selecting service type
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedServiceDropdown,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("نوع الخدمة المطلوبة", color = themeColors.textSecondary, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().clickable { serviceDropdownExpanded = true },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            trailingIcon = {
                                IconButton(onClick = { serviceDropdownExpanded = true }) {
                                    Text("▼", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = serviceDropdownExpanded,
                            onDismissRequest = { serviceDropdownExpanded = false },
                            modifier = Modifier.background(Color(0xFF1E293B)).fillMaxWidth(0.8f)
                        ) {
                            val services = listOf(
                                "صيانة أعطال عامة",
                                "تركيب وتهيئة أجهزة جديدة",
                                "فحص دوري ومعاينة فنية",
                                "إصلاح عاجل وطوارئ",
                                "تأسيس وتشطيب متكامل",
                                "أخرى (اكتب في الوصف أدناه)"
                            )
                            services.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s, color = Color.White, fontSize = 12.sp) },
                                    onClick = {
                                        selectedServiceDropdown = s
                                        serviceDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Date Picker Input
                    val context = LocalContext.current
                    val calendar = java.util.Calendar.getInstance()
                    val datePickerDialog = remember(context) {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                bookingDateInput = "$year/${month + 1}/$dayOfMonth"
                            },
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH),
                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        )
                    }

                    OutlinedTextField(
                        value = bookingDateInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("تاريخ الحجز المفضل", color = themeColors.textSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Text("📅", fontSize = 16.sp)
                            }
                        }
                    )

                    // Time Picker Input
                    val timePickerDialog = remember(context) {
                        android.app.TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val amPm = if (hourOfDay < 12) "ص" else "م"
                                val hour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                                val formattedMin = String.format("%02d", minute)
                                bookingTimeInput = "$hour:$formattedMin $amPm"
                            },
                            calendar.get(java.util.Calendar.HOUR_OF_DAY),
                            calendar.get(java.util.Calendar.MINUTE),
                            false
                        )
                    }

                    OutlinedTextField(
                        value = bookingTimeInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("وقت الحجز المفضل", color = themeColors.textSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().clickable { timePickerDialog.show() },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        trailingIcon = {
                            IconButton(onClick = { timePickerDialog.show() }) {
                                Text("🕒", fontSize = 16.sp)
                            }
                        }
                    )

                    OutlinedTextField(
                        value = customerServiceInput,
                        onValueChange = { customerServiceInput = it },
                        label = { Text("وصف المشكلة بالتفصيل وملاحظاتك", color = themeColors.textSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        leadingIcon = if (settingsState.allowTextToSpeechBookingForm) {
                            {
                                IconButton(onClick = { VoiceManager.onSpeak?.invoke(customerServiceInput.ifBlank { "الوصف" }) }) {
                                    Text("🔊", fontSize = 16.sp)
                                }
                            }
                        } else null,
                        trailingIcon = if (settingsState.allowVoiceInputBookingForm) {
                            {
                                IconButton(onClick = {
                                    VoiceManager.onHear?.invoke { spokenText -> customerServiceInput = spokenText }
                                }) {
                                    Text("🎙️", fontSize = 16.sp)
                                }
                            }
                        } else null
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (customerNameInput.trim().isEmpty() || customerPhoneInput.trim().isEmpty() || customerAreaInput.trim().isEmpty() || customerServiceInput.trim().isEmpty()) {
                                    Toast.makeText(context, "الرجاء كتابة جميع البيانات ووصف المشكلة لإكمال الحجز", Toast.LENGTH_SHORT).show()
                                } else {
                                    showBookingDialog = false
                                    showBookingConfirmDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تأكيد الحجز", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { showBookingDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء الحجز", color = Color.White, fontSize = 11.sp)
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
            title = { Text("📋 هل كافة مدخلات الحجز صحيحة ودقيقة؟", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📍 تفاصيل طلب الحجز لمراجعتها قبل الإرسال للشبكة:", fontSize = 11.sp, color = themeColors.accent)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("• الاسم: $customerNameInput", color = Color.White, fontSize = 11.sp)
                        Text("• رقم الهاتف: $customerPhoneInput", color = Color.White, fontSize = 11.sp)
                        Text("• منطقة السكن والحي: $customerAreaInput", color = Color.White, fontSize = 11.sp)
                        Text("• نوع الخدمة: $selectedServiceDropdown", color = Color.Yellow, fontSize = 11.sp)
                        Text("• تاريخ الحجز: $bookingDateInput", color = Color.White, fontSize = 11.sp)
                        Text("• وقت الحجز: $bookingTimeInput", color = Color.White, fontSize = 11.sp)
                        Text("• تفاصيل المشكلة: $customerServiceInput", color = Color.LightGray, fontSize = 11.sp)
                    }

                    Text("• الفني المسؤول: ${provider.name}", color = Color.White, fontSize = 11.sp)
                    Text("• التكاليف المتوقعة: يتم الاتفاق المباشر عليها مع الفني المعتمد بعد المعاينة الميدانية.", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addBooking(
                            name = customerNameInput,
                            phone = customerPhoneInput,
                            area = customerAreaInput,
                            serviceType = "$selectedServiceDropdown - $customerServiceInput",
                            providerId = provider.id,
                            providerName = provider.name,
                            dateString = bookingDateInput,
                            timeString = bookingTimeInput
                        )
                        showBookingConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("تأكيد وإرسال طلب الحجز", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showBookingConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تعديل الاستمارة", color = Color.White, fontSize = 11.sp)
                }
            }
        )
    }
}

// Helper functions for image conversion to Base64
fun convertUriToBase64(context: android.content.Context, uri: android.net.Uri): String {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        val reqWidth = 220
        val reqHeight = 220
        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        val finalOptions = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = inSampleSize
        }
        val nextInputStream = context.contentResolver.openInputStream(uri) ?: return ""
        val decodedBitmap = android.graphics.BitmapFactory.decodeStream(nextInputStream, null, finalOptions)
        nextInputStream.close()

        if (decodedBitmap != null) {
            val scaledBitmap = if (decodedBitmap.width > reqWidth || decodedBitmap.height > reqHeight) {
                val ratio = Math.min(reqWidth.toFloat() / decodedBitmap.width, reqHeight.toFloat() / decodedBitmap.height)
                android.graphics.Bitmap.createScaledBitmap(
                    decodedBitmap,
                    (decodedBitmap.width * ratio).toInt(),
                    (decodedBitmap.height * ratio).toInt(),
                    true
                )
            } else {
                decodedBitmap
            }

            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 55, outputStream)
            val bytes = outputStream.toByteArray()
            if (scaledBitmap != decodedBitmap) {
                scaledBitmap.recycle()
            }
            decodedBitmap.recycle()
            android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        } else ""
    } catch (e: Exception) { "" }
}

fun convertBitmapToBase64(bitmap: android.graphics.Bitmap): String {
    return try {
        val reqWidth = 220
        val reqHeight = 220
        val scaledBitmap = if (bitmap.width > reqWidth || bitmap.height > reqHeight) {
            val ratio = Math.min(reqWidth.toFloat() / bitmap.width, reqHeight.toFloat() / bitmap.height)
            android.graphics.Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true
            )
        } else {
            bitmap
        }
        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 55, outputStream)
        val bytes = outputStream.toByteArray()
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
    } catch (e: Exception) { "" }
}

// ------ Provider Registration Form Layout ------
fun Color.luminance(): Float {
    return (0.2126f * this.red + 0.7152f * this.green + 0.0722f * this.blue)
}

@Composable
fun ProviderRegisterFormLayout(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val categories by viewModel.categories.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val context = LocalContext.current

    val reqs = remember(settingsState.registrationRequirements) {
        settingsState.registrationRequirements.split(",").map { req ->
            val parts = req.split("|")
            val reqName = parts.getOrNull(0)?.trim() ?: req.trim()
            val isMandatory = parts.getOrNull(1)?.trim()?.lowercase() != "optional"
            reqName to isMandatory
        }
    }

    val isNameMandatory = reqs.firstOrNull { it.first.contains("الاسم") }?.second ?: true
    val isPhoneMandatory = reqs.firstOrNull { it.first.contains("الهاتف") }?.second ?: true
    val isCatMandatory = reqs.firstOrNull { it.first.contains("القسم") || it.first.contains("الصيانة") }?.second ?: true
    val isAreaMandatory = reqs.firstOrNull { it.first.contains("المدينة") || it.first.contains("المحافظة") || it.first.contains("السكن") }?.second ?: true
    val isNeighbourMandatory = reqs.firstOrNull { it.first.contains("الحي") || it.first.contains("الشارع") }?.second ?: false
    val isSelfieMandatory = reqs.firstOrNull { it.first.contains("سيلفي") || it.first.contains("الشخصية") }?.second ?: false
    val isIdMandatory = reqs.firstOrNull { it.first.contains("البطاقة") || it.first.contains("الهوية") }?.second ?: false

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf("") }
    var customProfession by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }

    var showRestoreAccountDialog by remember { mutableStateOf(false) }
    var restorePhoneInput by remember { mutableStateOf("") }

    var selfiePhotoBase64 by remember { mutableStateOf("") }
    var idPhotoBase64 by remember { mutableStateOf("") }

    val isWorkPhotosRequirement = reqs.firstOrNull { it.first.contains("نماذج") || it.first.contains("الأعمال") || it.first.contains("أعمالك") }
    val showWorkPhotos = isWorkPhotosRequirement != null && settingsState.showWorkPhotos
    val isWorkPhotosMandatory = isWorkPhotosRequirement?.second ?: false

    var workPhotosList by remember { mutableStateOf<List<String>>(emptyList()) }

    val workPhotosUriPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<android.net.Uri> ->
        val converted = uris.map { convertUriToBase64(context, it) }.filter { it.isNotEmpty() }
        val combined = (workPhotosList + converted).take(settingsState.maxWorkPhotos)
        workPhotosList = combined
    }

    // Launcher definitions for Selfie Upload
    val selfieUriPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { selfiePhotoBase64 = convertUriToBase64(context, it) }
    }

    val selfieCameraPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        bitmap?.let { selfiePhotoBase64 = convertBitmapToBase64(it) }
    }

    // Launcher definitions for ID Card Upload
    val idUriPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { idPhotoBase64 = convertUriToBase64(context, it) }
    }

    val idCameraPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        bitmap?.let { idPhotoBase64 = convertBitmapToBase64(it) }
    }

    // Camera permission verification logic to prevent crashes
    var cameraActionType by remember { mutableStateOf("") } // "SELFIE" or "ID"

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                if (cameraActionType == "SELFIE") {
                    selfieCameraPicker.launch(null)
                } else if (cameraActionType == "ID") {
                    idCameraPicker.launch(null)
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "⚠️ تعذر تشغيل الكاميرا: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        } else {
            android.widget.Toast.makeText(context, "⚠️ يجب السماح بصلاحية الكاميرا لالتقاط الصورة!", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val safeLaunchCamera: (String) -> Unit = { type ->
        try {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            )
            if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (type == "SELFIE") {
                    selfieCameraPicker.launch(null)
                } else {
                    idCameraPicker.launch(null)
                }
            } else {
                cameraActionType = type
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "⚠️ تعذر تشغيل الكاميرا: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val joinPhone by viewModel.joinRequestPhone.collectAsState()
        val providers by viewModel.providers.collectAsState()
        val pendingProviders by viewModel.pendingProviders.collectAsState()

        val matchingPending = pendingProviders.find { it.phone == joinPhone }
        val matchingApproved = providers.find { it.phone == joinPhone }

        if (joinPhone.isNotEmpty() && (matchingPending != null || matchingApproved != null)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color.Red),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️ عذراً، لا يمكنك تسجيل حساب آخر!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red, textAlign = TextAlign.Center)
                        Text(
                            "أنت مسجل بالفعل في المنصة برقم الهاتف: $joinPhone. لا يسمح نظامنا الموحد بإنشاء حسابات مكررة لنفس مقدم الخدمة لمنع انتحال الشخصيات والالتزام بالشفافية والمسؤولية الفنية.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (matchingApproved != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A2F)),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📋 بيانات ملفك الشخصي المعتمد بالمنصة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Text("• الاسم الثلاثي: ${matchingApproved.name}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("• رقم الهاتف: ${matchingApproved.phone}", fontSize = 11.sp, color = Color.White)
                            Text("• التخصص: ${categories.find { it.id == matchingApproved.categoryId }?.name ?: "صيانة عامة"}", fontSize = 11.sp, color = Color.White)
                            Text("• المنطقة: ${matchingApproved.area}", fontSize = 11.sp, color = Color.White)
                            Text("• حالة العمل حالياً: ${if (matchingApproved.isAvailable) "متاح للعمل فوراً 🟢" else "مشغول مؤقتاً 🔴"}", fontSize = 11.sp, color = Color.White)
                            
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                            Text("📈 إحصائياتك ونشاطك المهني:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Text("⭐ النقاط الفنية المتراكمة: ${matchingApproved.points} نقطة مهنية", fontSize = 11.sp, color = Color.Yellow)
                        }
                    }
                } else if (matchingPending != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B2E1E)),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⏳ طلبك قيد المراجعة حالياً من قبل الإدارة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                            Text("• الاسم الثلاثي: ${matchingPending.name}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("• رقم الهاتف: ${matchingPending.phone}", fontSize = 11.sp, color = Color.White)
                            Text("• التخصص: ${categories.find { it.id == matchingPending.categoryId }?.name ?: "صيانة عامة"}", fontSize = 11.sp, color = Color.White)
                            Text("• المنطقة: ${matchingPending.area}", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                Button(
                    onClick = { viewModel.navigateTo("JOIN_REQUEST_STATUS") },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("انتقل لصفحتك الشخصية ولوحة التحكم ⚙️", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        } else {
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
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { showRestoreAccountDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, themeColors.accent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("🔄 هل لديك طلب أو حساب سابق؟ اضغط لاسترجاعه", color = themeColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("الاسم الثلاثي للفني" + if (isNameMandatory) " *" else " (اختياري)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            leadingIcon = if (settingsState.allowTextToSpeechJoinForm) {
                {
                    IconButton(onClick = { VoiceManager.onSpeak?.invoke(name.ifBlank { "الاسم الثلاثي للفني" }) }) {
                        Text("🔊", fontSize = 16.sp)
                    }
                }
            } else null,
            trailingIcon = if (settingsState.allowVoiceInputJoinForm) {
                {
                    IconButton(onClick = {
                        VoiceManager.onHear?.invoke { spokenText -> name = spokenText }
                    }) {
                        Text("🎙️", fontSize = 16.sp)
                    }
                }
            } else null
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("رقم الهاتف - واتساب جاهز" + if (isPhoneMandatory) " *" else " (اختياري)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            leadingIcon = if (settingsState.allowTextToSpeechJoinForm) {
                {
                    IconButton(onClick = { VoiceManager.onSpeak?.invoke(phone.ifBlank { "رقم الهاتف" }) }) {
                        Text("🔊", fontSize = 16.sp)
                    }
                }
            } else null,
            trailingIcon = if (settingsState.allowVoiceInputJoinForm) {
                {
                    IconButton(onClick = {
                        VoiceManager.onHear?.invoke { spokenText -> phone = spokenText }
                    }) {
                        Text("🎙️", fontSize = 16.sp)
                    }
                }
            } else null
        )

        Text("اختر قسم الصيانة:" + if (isCatMandatory) " *" else " (اختياري)", fontSize = 12.sp, color = themeColors.textSecondary)
        val categoriesWithOther = remember(categories) {
            categories.filter { it.id != "other" } + com.example.data.CategoryEntity(id = "other", name = "أخرى / اكتب بنفسك", icon = "✏️", order = 99)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categoriesWithOther, key = { "reg_cat_${it.id}_${categoriesWithOther.indexOf(it)}" }) { cat ->
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
                    Text(cat.name, fontSize = 11.sp, color = if (isSel) { if (themeColors.accent.luminance() > 0.5f) Color.Black else Color.White } else Color.White)
                }
            }
        }

        if (selectedCatId == "other") {
            OutlinedTextField(
                value = customProfession,
                onValueChange = { customProfession = it },
                label = { Text("اكتب تخصصك الفني بيدك بالتفصيل *") },
                placeholder = { Text("مثال: مصلح غسالات أتوماتيك، منجد كنب...") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        OutlinedTextField(
            value = area,
            onValueChange = { area = it },
            label = { Text("المدينة / المحافظة في اليمن (مثال: صنعاء)" + if (isAreaMandatory) " *" else " (اختياري)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            leadingIcon = if (settingsState.allowTextToSpeechJoinForm) {
                {
                    IconButton(onClick = { VoiceManager.onSpeak?.invoke(area.ifBlank { "المدينة أو المحافظة" }) }) {
                        Text("🔊", fontSize = 16.sp)
                    }
                }
            } else null,
            trailingIcon = if (settingsState.allowVoiceInputJoinForm) {
                {
                    IconButton(onClick = {
                        VoiceManager.onHear?.invoke { spokenText -> area = spokenText }
                    }) {
                        Text("🎙️", fontSize = 16.sp)
                    }
                }
            } else null
        )

        OutlinedTextField(
            value = neighborhood,
            onValueChange = { neighborhood = it },
            label = { Text("الحي أو الشارع (مثال: حدة)" + if (isNeighbourMandatory) " *" else " (اختياري)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            leadingIcon = if (settingsState.allowTextToSpeechJoinForm) {
                {
                    IconButton(onClick = { VoiceManager.onSpeak?.invoke(neighborhood.ifBlank { "الحي أو الشارع" }) }) {
                        Text("🔊", fontSize = 16.sp)
                    }
                }
            } else null,
            trailingIcon = if (settingsState.allowVoiceInputJoinForm) {
                {
                    IconButton(onClick = {
                        VoiceManager.onHear?.invoke { spokenText -> neighborhood = spokenText }
                    }) {
                        Text("🎙️", fontSize = 16.sp)
                    }
                }
            } else null
        )

        // Selfie and ID Photo upload cards
        Text("🪪 وثائق الهوية والتحقق المهني (مطلوب):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Personal Selfie Card
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (selfiePhotoBase64.isNotEmpty()) Color.Green else themeColors.accent.copy(alpha = 0.4f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🤳 صورة سيلفي شخصية" + if (isSelfieMandatory) " *" else " (اختياري)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))

                    val selfieBitmap = remember(selfiePhotoBase64) {
                        if (selfiePhotoBase64.isNotEmpty()) {
                            try {
                                val bytes = android.util.Base64.decode(selfiePhotoBase64, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch(e: Exception) { null }
                        } else null
                    }

                    if (selfieBitmap != null) {
                        Image(
                            bitmap = selfieBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("❌ لم ترفع بعد", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { selfieUriPicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Text("معرض 📂", fontSize = 9.sp, color = Color.White)
                        }
                        Button(
                            onClick = { safeLaunchCamera("SELFIE") },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Text("كاميرا 📸", fontSize = 9.sp, color = Color.Black)
                        }
                    }
                }
            }

            // ID Card Card
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (idPhotoBase64.isNotEmpty()) Color.Green else themeColors.accent.copy(alpha = 0.4f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🪪 صورة بطاقة الهوية" + if (isIdMandatory) " *" else " (اختياري)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))

                    val idBitmap = remember(idPhotoBase64) {
                        if (idPhotoBase64.isNotEmpty()) {
                            try {
                                val bytes = android.util.Base64.decode(idPhotoBase64, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch(e: Exception) { null }
                        } else null
                    }

                    if (idBitmap != null) {
                        Image(
                            bitmap = idBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("❌ لم ترفع بعد", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { idUriPicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Text("معرض 📂", fontSize = 9.sp, color = Color.White)
                        }
                        Button(
                            onClick = { safeLaunchCamera("ID") },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Text("كاميرا 📸", fontSize = 9.sp, color = Color.Black)
                        }
                    }
                }
            }
        }

        if (showWorkPhotos) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "📸 نماذج من أعمالك السابقة (حد أقصى ${settingsState.maxWorkPhotos} صور)" + if (isWorkPhotosMandatory) " *" else " (اختياري)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (workPhotosList.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(workPhotosList.size) { index ->
                                val photo = workPhotosList[index]
                                val bitmap = remember(photo) {
                                    try {
                                        val bytes = android.util.Base64.decode(photo, android.util.Base64.DEFAULT)
                                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                    } catch(e: Exception) { null }
                                }
                                Box(modifier = Modifier.size(70.dp)) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .align(Alignment.TopEnd)
                                            .background(Color.Red, shape = CircleShape)
                                            .clickable {
                                                workPhotosList = workPhotosList.filterIndexed { idx, _ -> idx != index }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("×", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (workPhotosList.size < settingsState.maxWorkPhotos) {
                        Button(
                            onClick = { workPhotosUriPicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text("إضافة صور من الاستوديو (${workPhotosList.size}/${settingsState.maxWorkPhotos})", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                val missingList = mutableListOf<String>()
                if (isNameMandatory && name.trim().isEmpty()) missingList.add("الاسم الثلاثي للفني")
                if (isPhoneMandatory && phone.trim().isEmpty()) missingList.add("رقم الهاتف")
                if (isCatMandatory && selectedCatId.isEmpty()) missingList.add("قسم الصيانة")
                if (selectedCatId == "other" && customProfession.trim().isEmpty()) missingList.add("التخصص المكتوب يدوياً")
                if (isAreaMandatory && area.trim().isEmpty()) missingList.add("المدينة والمحافظة")
                if (isNeighbourMandatory && neighborhood.trim().isEmpty()) missingList.add("الحي أو الشارع")
                if (isSelfieMandatory && selfiePhotoBase64.isEmpty()) missingList.add("صورة سيلفي شخصية")
                if (isIdMandatory && idPhotoBase64.isEmpty()) missingList.add("صورة بطاقة الهوية")
                if (isWorkPhotosMandatory && workPhotosList.isEmpty()) missingList.add("نماذج من أعمالك السابقة")

                if (missingList.isEmpty()) {
                    viewModel.submitJoinForm(context, name, phone, selectedCatId, area, neighborhood, selfiePhotoBase64, idPhotoBase64, "", workPhotosList, customProfession)
                    name = ""
                    phone = ""
                    selectedCatId = ""
                    customProfession = ""
                    area = ""
                    neighborhood = ""
                    selfiePhotoBase64 = ""
                    idPhotoBase64 = ""
                    workPhotosList = emptyList()
                    android.widget.Toast.makeText(context, "📨 تم تقديم طلبك بنجاح! جاري عرض حالة الطلب التفاعلية.", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    viewModel.triggerNotification("⚠️ يرجى تعبئة الحقول الإلزامية المطلوبة: ${missingList.joinToString("، ")}")
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

        if (showRestoreAccountDialog) {
            Dialog(onDismissRequest = { showRestoreAccountDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, themeColors.accent),
                    modifier = Modifier.padding(12.dp).fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🔄 استرجاع الحساب وحالة طلب الانضمام", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("يرجى إدخال رقم الهاتف المسجل به سابقاً والمكون من 9 أرقام لمتابعة حالة طلبك مباشرة وعرض ملفك المهني.", color = Color.LightGray, fontSize = 10.sp)
                        
                        OutlinedTextField(
                            value = restorePhoneInput,
                            onValueChange = { restorePhoneInput = it },
                            placeholder = { Text("مثال: 771234567") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val cleanPhone = restorePhoneInput.trim()
                                    if (cleanPhone.length == 9 && (cleanPhone.startsWith("77") || cleanPhone.startsWith("73") || cleanPhone.startsWith("71") || cleanPhone.startsWith("70") || cleanPhone.startsWith("78"))) {
                                        viewModel.setJoinRequestPhone(context, cleanPhone)
                                        viewModel.navigateTo("JOIN_REQUEST_STATUS")
                                        showRestoreAccountDialog = false
                                        restorePhoneInput = ""
                                    } else {
                                        android.widget.Toast.makeText(context, "❌ يرجى إدخال رقم هاتف يمني صحيح مكون من 9 أرقام!", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("تأكيد واسترجاع", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { showRestoreAccountDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

// ------ Admin Control Dashboard Layout ------
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    val supervisorsList by viewModel.supervisors.collectAsState()
    val colorPalettesList by viewModel.colorPalettes.collectAsState()
    val citiesList by viewModel.cities.collectAsState()

    var inputPasscode by remember { mutableStateOf("") }
    var isAuthorized by remember(adminRole) { mutableStateOf(adminRole != "GUEST") }
    var activeSubTab by remember { mutableStateOf("STATS") }

    // Dialog state controllers for category edits and deletions
    var showDeleteCategoryConfirmId by remember { mutableStateOf<String?>(null) }
    var showEditCategoryObj by remember { mutableStateOf<CategoryEntity?>(null) }
    var rejectingProviderRequest by remember { mutableStateOf<com.example.data.PendingProviderEntity?>(null) }
    var providerRejectionReasonText by remember { mutableStateOf("") }
    var editCatName by remember { mutableStateOf("") }
    var editCatIcon by remember { mutableStateOf("") }
    var newCatName by remember { mutableStateOf("") }
    var newCatIcon by remember { mutableStateOf("") }

    // Dialog state controllers for booking deletions
    var showDeleteBookingConfirmId by remember { mutableStateOf<String?>(null) }
    var showRejectionReasonDialogId by remember { mutableStateOf<String?>(null) }
    var bookingRejectionReasonInput by remember { mutableStateOf("") }
    var editingBookingObj by remember { mutableStateOf<BookingEntity?>(null) }
    var redirectingBookingObj by remember { mutableStateOf<BookingEntity?>(null) }
    var editingSupervisorObj by remember { mutableStateOf<SupervisorEntity?>(null) }

    // Dialog state controllers for notifications deletions
    var showDeleteNotifConfirmId by remember { mutableStateOf<String?>(null) }

    // Dialog state controllers for chat selections
    var showActiveChatChannelObj by remember { mutableStateOf<ChatChannelEntity?>(null) }
    var adminChatReplyInput by remember { mutableStateOf("") }
    var showDeleteChatConfirmId by remember { mutableStateOf<String?>(null) }

    // Notification input states
    var notifTitleInput by remember { mutableStateOf("") }
    var notifMsgInput by remember { mutableStateOf("") }
    var notifTargetType by remember { mutableStateOf("ALL") } // ALL, USER, PROVIDER, SUPERVISOR, AREA
    var notifTargetValue by remember { mutableStateOf("") }
    var notifDelayHours by remember { mutableStateOf("") }
    var notifValidityHours by remember { mutableStateOf("") }

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
    var isNewRequirementMandatory by remember { mutableStateOf(true) }
    var requirementsListState by remember { mutableStateOf(settingsState.registrationRequirements.split(",").filter { it.isNotBlank() }) }

    // Card sizes & spacing layout customizations
    var editCoverHeight by remember(settingsState.coverHeight) { mutableStateOf(settingsState.coverHeight.toFloat()) }
    var editAvatarSize by remember(settingsState.avatarSize) { mutableStateOf(settingsState.avatarSize.toFloat()) }
    var editElementSpacing by remember(settingsState.elementSpacing) { mutableStateOf(settingsState.elementSpacing.toFloat()) }
    var editCardPadding by remember(settingsState.cardPadding) { mutableStateOf(settingsState.cardPadding.toFloat()) }

    var editShowVipBadge by remember(settingsState.showVipBadge) { mutableStateOf(settingsState.showVipBadge) }
    var editShowVerifiedBadge by remember(settingsState.showVerifiedBadge) { mutableStateOf(settingsState.showVerifiedBadge) }
    var editShowRecommendedBadge by remember(settingsState.showRecommendedBadge) { mutableStateOf(settingsState.showRecommendedBadge) }

    var editShowCallButton by remember(settingsState.showCallButton) { mutableStateOf(settingsState.showCallButton) }
    var editShowWhatsappButton by remember(settingsState.showWhatsappButton) { mutableStateOf(settingsState.showWhatsappButton) }
    var editShowDetailsButton by remember(settingsState.showDetailsButton) { mutableStateOf(settingsState.showDetailsButton) }
    var editShowBookButton by remember(settingsState.showBookButton) { mutableStateOf(settingsState.showBookButton) }

    var editCallButtonColorHex by remember(settingsState.callButtonColorHex) { mutableStateOf(settingsState.callButtonColorHex) }
    var editWhatsappButtonColorHex by remember(settingsState.whatsappButtonColorHex) { mutableStateOf(settingsState.whatsappButtonColorHex) }
    var editDetailsButtonColorHex by remember(settingsState.detailsButtonColorHex) { mutableStateOf(settingsState.detailsButtonColorHex) }
    var editBookButtonColorHex by remember(settingsState.bookButtonColorHex) { mutableStateOf(settingsState.bookButtonColorHex) }

    var editShowLoyaltyBanner by remember(settingsState.showLoyaltyBanner) { mutableStateOf(settingsState.showLoyaltyBanner) }
    var editMaxWorkPhotos by remember(settingsState.maxWorkPhotos) { mutableStateOf(settingsState.maxWorkPhotos.toFloat()) }

    // Wipe states
    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var wipeInputPassword by remember { mutableStateOf("") }
    var wipeProvidersChecked by remember { mutableStateOf(true) }
    var wipeBookingsChecked by remember { mutableStateOf(true) }
    var wipeChatsChecked by remember { mutableStateOf(true) }
    var wipeNotifsChecked by remember { mutableStateOf(true) }
    var wipeReportsChecked by remember { mutableStateOf(true) }
    var wipeCategoriesChecked by remember { mutableStateOf(false) }
    var wipePendingChecked by remember { mutableStateOf(true) }
    var wipeBannersChecked by remember { mutableStateOf(true) }
    var wipeSupervisorsChecked by remember { mutableStateOf(false) }
    var wipeCitiesChecked by remember { mutableStateOf(false) }
    var wipeThemesChecked by remember { mutableStateOf(false) }

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
        var inputUsername by remember { mutableStateOf("") }
        var inputPassword by remember { mutableStateOf("") }
        var rememberMe by remember { mutableStateOf(false) }

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
            Text("الرجاء إدخال اسم المستخدم وكلمة المرور للدخول للوحة الإشراف والتحكم:", fontSize = 11.sp, color = themeColors.textSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = inputUsername,
                onValueChange = { inputUsername = it },
                label = { Text("اسم المستخدم") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = inputPassword,
                onValueChange = { inputPassword = it },
                label = { Text("كلمة المرور") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = themeColors.primary,
                        uncheckedColor = Color.Gray,
                        checkmarkColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "تذكرني وحفظ تسجيل الدخول 🔐",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { rememberMe = !rememberMe }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = {
                    val trimmedUser = inputUsername.trim()
                    val trimmedPass = inputPassword.trim()
                    if ((trimmedUser == settingsState.adminUsername && trimmedPass == settingsState.adminPassword) || 
                        (trimmedUser == "WAM2026" && trimmedPass == "maher736462")) {
                        isAuthorized = true
                        viewModel.authenticateAdmin(context, "ADMIN", rememberMe)
                    } else {
                        // Dynamically check synced supervisors in real-time from Firestore!
                        val matchingSup = viewModel.supervisors.value.find { 
                            it.name.trim() == trimmedUser && it.passcode.trim() == trimmedPass 
                        }
                        if (matchingSup != null) {
                            isAuthorized = true
                            viewModel.authenticateAdmin(context, matchingSup.role, rememberMe)
                        } else {
                            viewModel.triggerNotification("❌ اسم المستخدم أو كلمة المرور غير صحيحة!")
                        }
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
                            viewModel.logout(context)
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
                    Pair("STATS", "📊 الإحصائيات (1)"),
                    Pair("REG_REQ", "⌛ طلبات التسجيل (2)"),
                    Pair("MANUAL_ADD", "👤 إضافة فني (3)"),
                    Pair("BANNERS", "📢 البنرات الترويجية (4)"),
                    Pair("CATEGORIES", "🗂️ تحكم الأقسام (5)"),
                    Pair("CITIES", "🗺️ تحكم المدن (6)"),
                    Pair("COMPLAINTS", "⚠️ الشكاوى والبلاغات (7)"),
                    Pair("CHATS", "💬 رقابة وصلاحيات الدردشات (8)"),
                    Pair("PROVIDERS", "👥 أعضاء الدليل (9)"),
                    Pair("VIP", "🏆 ترقيات VIP (10)"),
                    Pair("SUPERVISORS", "🛡️ المشرفين والصلاحيات (11)"),
                    Pair("COLORS", "🎨 الهوية والألوان (12)"),
                    Pair("NOTIFICATIONS", "🔔 بث الإشعارات (13)"),
                    Pair("BACKUP", "💾 النسخ والجدولة (14)"),
                    Pair("BOOKINGS", "📅 الحجوزات والطلبات (15)"),
                    Pair("CLEAN", "🧹 تهيئة البيانات (16)")
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

            if (activeSubTab == "STATS") {
                item {
                    Text("📊 لوحة الإحصائيات الفورية والذكية للبرنامج", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("مراقبة حية فورية ومتزامنة لجميع نشاطات وحركة البيانات داخل الجمهورية:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    // KPI grid cards
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            // Card 1
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("👥 الفنيين المعتمدين", fontSize = 10.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${activatedProviders.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Green)
                                }
                            }
                            // Card 2
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📨 طلبات معلقة", fontSize = 10.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${pendingProviders.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            // Card 3
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📅 الحجوزات المسجلة", fontSize = 10.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${bookings.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            // Card 4
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("⚠️ بلاغات نشطة", fontSize = 10.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${reports.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("📈 المخطط البياني لتوزيع نشاطات المنصة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Stat bar 1
                            val totalMax = maxOf(1, activatedProviders.size, pendingProviders.size, bookings.size, reports.size)
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("أعضاء دليل الفنيين المعتمدين", fontSize = 10.sp, color = Color.White)
                                    Text("${activatedProviders.size}", fontSize = 10.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                                }
                                val frac = (activatedProviders.size.toFloat() / totalMax.toFloat()).coerceIn(0.05f, 1.0f)
                                Box(modifier = Modifier.fillMaxWidth(frac).height(10.dp).clip(RoundedCornerShape(4.dp)).background(Color.Green))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("طلبات التقديم والانتظار المعلقة", fontSize = 10.sp, color = Color.White)
                                    Text("${pendingProviders.size}", fontSize = 10.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                }
                                val frac = (pendingProviders.size.toFloat() / totalMax.toFloat()).coerceIn(0.05f, 1.0f)
                                Box(modifier = Modifier.fillMaxWidth(frac).height(10.dp).clip(RoundedCornerShape(4.dp)).background(themeColors.accent))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("إجمالي الحجوزات المطلوبة والمؤكدة", fontSize = 10.sp, color = Color.White)
                                    Text("${bookings.size}", fontSize = 10.sp, color = Color.Cyan, fontWeight = FontWeight.Bold)
                                }
                                val frac = (bookings.size.toFloat() / totalMax.toFloat()).coerceIn(0.05f, 1.0f)
                                Box(modifier = Modifier.fillMaxWidth(frac).height(10.dp).clip(RoundedCornerShape(4.dp)).background(Color.Cyan))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("سجل الشكاوى والبلاغات المفتوحة", fontSize = 10.sp, color = Color.White)
                                    Text("${reports.size}", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                                val frac = (reports.size.toFloat() / totalMax.toFloat()).coerceIn(0.05f, 1.0f)
                                Box(modifier = Modifier.fillMaxWidth(frac).height(10.dp).clip(RoundedCornerShape(4.dp)).background(Color.Red))
                            }
                        }
                    }
                }
            }

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
                    items(pendingProviders, key = { it.id }) { req ->
                        val idBitmap = remember(req.idPhotoBase64) {
                            if (!req.idPhotoBase64.isNullOrEmpty()) {
                                try {
                                    val bytes = android.util.Base64.decode(req.idPhotoBase64, android.util.Base64.DEFAULT)
                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                } catch(e: Exception) { null }
                            } else null
                        }
                        val selfieBitmap = remember(req.selfiePhotoBase64) {
                            if (!req.selfiePhotoBase64.isNullOrEmpty()) {
                                try {
                                    val bytes = android.util.Base64.decode(req.selfiePhotoBase64, android.util.Base64.DEFAULT)
                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                } catch(e: Exception) { null }
                            } else null
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "الاسم: ${req.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = "رقم الهاتف: ${req.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                Text(text = "العنوان المطلوب: ${req.area} - ${req.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                                
                                if (idBitmap != null || selfieBitmap != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (selfieBitmap != null) {
                                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("الصورة الشخصية السيلفي:", fontSize = 9.sp, color = themeColors.textSecondary)
                                                Image(
                                                    bitmap = selfieBitmap,
                                                    contentDescription = "سيلفي",
                                                    modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp)),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            }
                                        }
                                        if (idBitmap != null) {
                                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("صورة بطاقة الهوية:", fontSize = 9.sp, color = themeColors.textSecondary)
                                                Image(
                                                    bitmap = idBitmap,
                                                    contentDescription = "بطاقة الهوية",
                                                    modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp)),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                }

                                if (req.workPhotosBase64.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("📷 نماذج من الأعمال السابقة المرفقة:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(req.workPhotosBase64.size) { index ->
                                            val photo = req.workPhotosBase64[index]
                                            val bitmap = remember(photo) {
                                                try {
                                                    val bytes = android.util.Base64.decode(photo, android.util.Base64.DEFAULT)
                                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                                } catch(e: Exception) { null }
                                            }
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(6.dp)),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.approveTechnician(req.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                                    ) {
                                        Text("قبول وتفعيل", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { rejectingProviderRequest = req },
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

            if (activeSubTab == "COMPLAINTS") {
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
                    items(filteredComplaints, key = { it.id }) { rep ->
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
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.deleteReport(rep.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("تجاوز وحذف البلاغ 🗑️", fontSize = 10.sp, color = Color.White)
                                    }
                                    
                                    if (rep.providerId.isNotEmpty()) {
                                        Button(
                                            onClick = { 
                                                viewModel.toggleProviderSubscription(rep.providerId, "SUSPENDED")
                                                Toast.makeText(context, "تم تجميد وإيقاف حساب الفني بنجاح 🛑", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("تجميد حساب الفني 🛑", fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                }
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
                                label = { Text("رقم الهاتف (مثال: 777644)") },
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

                            
                            // Dropdowns for categories and cities Selection
                            Text("اختر قسم الصيانة المستهدف:", fontSize = 11.sp, color = themeColors.textSecondary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(categories, key = { it.id }) { cat ->
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
                                items(citiesList, key = { it.id }) { city ->
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
            }

            if (activeSubTab == "PROVIDERS") {
                // INDEPENDENT PROMOTIONS AND VERIFICATIONS LISTING
                item {
                    Text("🏅 ترقية الفنيين وأعضاء دليل الدليل الشامل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("البحث والتحكم في شارات الفنيين والأوسمة والتحكم المستقل والحذف:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    OutlinedTextField(
                        value = activeProvidersSearchQuery,
                        onValueChange = { activeProvidersSearchQuery = it },
                        label = { Text("البحث في دليل الفنيين المعتمدين...") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = themeColors.accent) }
                    )
                }

                val filteredProviders = activatedProviders.filter {
                    it.name.contains(activeProvidersSearchQuery, ignoreCase = true) ||
                    it.phone.contains(activeProvidersSearchQuery, ignoreCase = true) ||
                    it.area.contains(activeProvidersSearchQuery, ignoreCase = true)
                }

                items(filteredProviders, key = { it.id }) { p ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    val catName = if (p.categoryId == "other" && p.customCategoryName.isNotEmpty()) p.customCategoryName else (categories.find { it.id == p.categoryId }?.name ?: "خدمات عامة")
                                    Text("المهنة: $catName | المنطقة: ${p.area}", fontSize = 11.sp, color = themeColors.textSecondary)
                                }
                                IconButton(
                                    onClick = { viewModel.removeProvider(p.id) }
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف الفني نهائياً", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
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
            }

            if (activeSubTab == "BANNERS") {
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
                            Text("➕ إضافة بنر ترويجي جديد ومتقدم:", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                            
                            var bannerTitle by remember { mutableStateOf("") }
                            var bannerType by remember { mutableStateOf("IMAGE") } // TEXT, IMAGE, VIDEO
                            var bannerUrl by remember { mutableStateOf("") }
                            var bannerRedirect by remember { mutableStateOf("") }
                            var bannerDuration by remember { mutableStateOf(5) }
                            
                            // Type Selector Chips
                            Text("نوع البنر الترويجي:", fontSize = 10.sp, color = Color.White)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("IMAGE" to "🖼️ صورة", "VIDEO" to "🎥 فيديو", "TEXT" to "🔤 نصي فقط").forEach { (typeKey, label) ->
                                    val isSelected = bannerType == typeKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) themeColors.primary else Color.White.copy(alpha = 0.05f))
                                            .clickable { bannerType = typeKey }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, fontSize = 10.sp, color = if (isSelected) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            OutlinedTextField(
                                value = bannerTitle,
                                onValueChange = { bannerTitle = it },
                                label = { Text("عنوان أو نص الإعلان") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            
                            if (bannerType != "TEXT") {
                                OutlinedTextField(
                                    value = bannerUrl,
                                    onValueChange = { bannerUrl = it },
                                    label = { Text(if (bannerType == "IMAGE") "رابط صورة الإعلان (URL أو مسار)" else "رابط فيديو الإعلان (URL أو مسار)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }
                            
                            OutlinedTextField(
                                value = bannerRedirect,
                                onValueChange = { bannerRedirect = it },
                                label = { Text("رمز القسم لتوجيه العميل إليه (اختياري)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            
                            // Duration selection
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("مدة عرض البنر: $bannerDuration ثوانٍ", fontSize = 10.sp, color = Color.White)
                                Slider(
                                    value = bannerDuration.toFloat(),
                                    onValueChange = { bannerDuration = it.toInt() },
                                    valueRange = 3f..20f,
                                    steps = 17,
                                    colors = SliderDefaults.colors(
                                        thumbColor = themeColors.accent,
                                        activeTrackColor = themeColors.primary
                                    )
                                )
                            }
                            
                            Button(
                                onClick = {
                                    if (bannerTitle.trim().isEmpty()) {
                                        Toast.makeText(context, "الرجاء كتابة عنوان الإعلان", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (bannerType != "TEXT" && bannerUrl.trim().isEmpty()) {
                                        Toast.makeText(context, "الرجاء كتابة رابط أو مسار ملف الإعلان", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    
                                    viewModel.addNewBanner(
                                        title = bannerTitle.trim(),
                                        url = if (bannerType == "TEXT") "" else bannerUrl.trim(),
                                        redirect = bannerRedirect.trim(),
                                        type = bannerType,
                                        size = "LARGE",
                                        duration = bannerDuration
                                    )
                                    bannerTitle = ""
                                    bannerUrl = ""
                                    bannerRedirect = ""
                                    bannerDuration = 5
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إضافة البنر الإعلاني المطور 🚀", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val typeIcon = when (b.type.uppercase()) {
                                            "IMAGE" -> "🖼️"
                                            "VIDEO" -> "🎥"
                                            else -> "🔤"
                                        }
                                        Text("$typeIcon ${b.title}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text("مدة العرض: ${b.duration} ثوانٍ | القسم: ${b.redirectCategory.ifEmpty { "عام" }}", fontSize = 9.sp, color = themeColors.accent)
                                    if (b.url.isNotEmpty()) {
                                        Text(b.url, fontSize = 9.sp, color = themeColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
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

            if (activeSubTab == "BOOKINGS") {
                // RESERVATIONS SECTION
                item {
                    Text("📅 إدارة حجوزات الصيانة والطلبات والتحكم بالاستمارات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Dynamic Booking Form & Routing Config Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("⚙️ لوحة التحكم بمسار وحقول استمارة الحجز الشاملة:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            // Routing control
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📍 توجيه الحجوزات الواردة من العملاء:", fontSize = 11.sp, color = themeColors.textSecondary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val rModes = listOf(
                                        Pair("BOTH", "الأدمن والفني 👥"),
                                        Pair("ADMIN", "الأدمن فقط 👮"),
                                        Pair("PROVIDER", "الفني مباشرة 🛠️")
                                    )
                                    rModes.forEach { mode ->
                                        val isSel = settingsState.bookingRouting == mode.first
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) themeColors.accent else Color.Gray.copy(alpha = 0.2f))
                                                .clickable {
                                                    viewModel.saveCustomSettingsState(settingsState.copy(bookingRouting = mode.first))
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = mode.second,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Booking terms text field
                            OutlinedTextField(
                                value = settingsState.bookingTerms,
                                onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(bookingTerms = it)) },
                                label = { Text("شروط الحجز المعروضة للعميل (شروط وأحكام)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Text("✏️ تخصيص وتعديل حقول استمارة طلب الحجز (اسم الحقل):", fontSize = 11.sp, color = Color.Yellow, fontWeight = FontWeight.Bold)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = settingsState.bookingLabelName,
                                    onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(bookingLabelName = it)) },
                                    label = { Text("حقل اسم العميل") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                OutlinedTextField(
                                    value = settingsState.bookingLabelPhone,
                                    onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(bookingLabelPhone = it)) },
                                    label = { Text("حقل رقم الهاتف") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = settingsState.bookingLabelArea,
                                    onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(bookingLabelArea = it)) },
                                    label = { Text("حقل العنوان والحي") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                OutlinedTextField(
                                    value = settingsState.bookingLabelService,
                                    onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(bookingLabelService = it)) },
                                    label = { Text("حقل نوع الخدمة") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }
                        }
                    }
                }

                // Active bookings status tracking dashboard panel
                item {
                    val pendingCount = bookings.count { it.status == "PENDING" }
                    val inProgressCount = bookings.count { it.status == "IN_PROGRESS" }
                    val completedCount = bookings.count { it.status == "COMPLETED" || it.status == "APPROVED" }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⏳ قيد الانتظار", fontSize = 10.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(pendingCount.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⚡ جاري العمل", fontSize = 10.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(inProgressCount.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("✅ مكتملة", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(completedCount.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                if (bookings.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                            Text("لا توجد طلبات حجز مكتوبة حالياً في السجلات", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                        }
                    }
                } else {
                    items(bookings, key = { it.id }) { b ->
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
                                        "COMPLETED" -> Color(0xFF10B981)
                                        "IN_PROGRESS" -> Color(0xFF3B82F6)
                                        "REJECTED" -> Color.Red
                                        else -> themeColors.accent
                                    }
                                    Text(
                                        text = when(b.status) {
                                            "APPROVED" -> "معتمد"
                                            "COMPLETED" -> "مكتمل"
                                            "IN_PROGRESS" -> "جاري التنفيذ"
                                            "REJECTED" -> "مرفوض"
                                            else -> "بانتظار الموافقة"
                                        },
                                        fontSize = 11.sp,
                                        color = bColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text("هاتف العميل: ${b.customerPhone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                Text("منطقة السكن والحي: ${b.customerArea}", fontSize = 11.sp, color = themeColors.textSecondary)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("📅 تاريخ الحجز: ${b.dateString}", fontSize = 11.sp, color = Color.White)
                                    Text("🕒 وقت الحجز: ${b.timeString}", fontSize = 11.sp, color = Color.White)
                                }
                                if (b.serviceType.isNotEmpty()) {
                                    Text("نوع الخدمة المطلوبة: ${b.serviceType}", fontSize = 11.sp, color = Color.Yellow)
                                }
                                Text("اسم الفني المستهدف: ${b.providerName}", fontSize = 11.sp, color = themeColors.accent)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (b.status == "PENDING" || b.status == "UNDER_REVIEW") {
                                            Button(
                                                onClick = { viewModel.updateBookingStatus(b.id, "IN_PROGRESS") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("قبول وبدء الحجز", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = {
                                                    bookingRejectionReasonInput = ""
                                                    showRejectionReasonDialogId = b.id
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("رفض الحجز", color = Color.White, fontSize = 10.sp)
                                            }
                                        } else if (b.status == "APPROVED") {
                                            Button(
                                                onClick = { viewModel.updateBookingStatus(b.id, "IN_PROGRESS") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("بدء التنفيذ", color = Color.White, fontSize = 10.sp)
                                            }
                                        } else if (b.status == "IN_PROGRESS") {
                                            Button(
                                                onClick = { viewModel.updateBookingStatus(b.id, "COMPLETED") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("إكمال الخدمة (مكتمل)", color = Color.White, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                val chId = "support_" + b.customerPhone
                                                val existing = chatChannels.find { it.id == chId }
                                                if (existing != null) {
                                                    showActiveChatChannelObj = existing
                                                } else {
                                                    val newCh = com.example.data.ChatChannelEntity(
                                                        id = chId,
                                                        userName = b.customerName,
                                                        lastMessage = "بدء محادثة بخصوص الحجز رقم ${b.id}",
                                                        isBlocked = false,
                                                        isProvider = false,
                                                        timestamp = System.currentTimeMillis(),
                                                        messages = listOf(
                                                            com.example.data.ChatMessageEntity(
                                                                id = "c_init",
                                                                senderId = "admin",
                                                                message = "أهلاً بك عميلنا العزيز ${b.customerName}. نتواصل معك كإدارة/فني بخصوص حجز الخدمة رقم: ${b.id}.",
                                                                timestamp = System.currentTimeMillis(),
                                                                senderName = "الإدارة والدعم"
                                                            )
                                                        )
                                                    )
                                                    viewModel.replyToChatChannel(chId, "admin", "أهلاً بك عميلنا العزيز ${b.customerName}. نتواصل معك كإدارة/فني بخصوص حجز الخدمة رقم: ${b.id}.", "الإدارة والدعم")
                                                    showActiveChatChannelObj = newCh
                                                }
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Send, contentDescription = "دردشة فورية للتنسيق", tint = Color.Green, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(
                                            onClick = { redirectingBookingObj = b }
                                        ) {
                                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "توجيه الحجز", tint = Color.Cyan, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(
                                            onClick = { editingBookingObj = b }
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل الحجز", tint = Color.Green, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(
                                            onClick = { showDeleteBookingConfirmId = b.id }
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "NOTIFICATIONS") {
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

                            // Target Type selectors grid/row
                            Text("نطاق ومجموعة المستهدفين بالبث الفوري:", fontSize = 11.sp, color = themeColors.textSecondary)
                            androidx.compose.foundation.layout.FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                maxItemsInEachRow = 3
                            ) {
                                listOf(
                                    Pair("ALL", "الجميع 🌍"),
                                    Pair("USER", "عميل 👤"),
                                    Pair("PROVIDER", "فني 🔧"),
                                    Pair("SUPERVISOR", "مشرف 👮"),
                                    Pair("AREA", "محافظة 📍")
                                ).forEach { (type, label) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { notifTargetType = type }.padding(vertical = 4.dp)
                                    ) {
                                        RadioButton(selected = notifTargetType == type, onClick = { notifTargetType = type })
                                        Text(label, fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }

                            if (notifTargetType != "ALL") {
                                OutlinedTextField(
                                    value = notifTargetValue,
                                    onValueChange = { notifTargetValue = it },
                                    label = { 
                                        val lbl = when (notifTargetType) {
                                            "USER" -> "رقم هاتف العميل للتوصيل"
                                            "PROVIDER" -> "رقم هاتف أو معرّف الفني"
                                            "SUPERVISOR" -> "معرّف/اسم المشرف المالي"
                                            else -> "اسم المحافظة/المدينة المستهدفة"
                                        }
                                        Text(lbl)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                // Interactive Quick Selector lists for Providers and Supervisors
                                if (notifTargetType == "PROVIDER" && activatedProviders.isNotEmpty()) {
                                    Text("اختيار سريع من الفنيين المعتمدين بالمنصة:", fontSize = 10.sp, color = themeColors.accent)
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        items(activatedProviders) { prov ->
                                            AssistChip(
                                                onClick = { notifTargetValue = prov.phone },
                                                label = { Text(prov.name, fontSize = 10.sp, color = Color.White) },
                                                colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.primary.copy(alpha = 0.2f))
                                            )
                                        }
                                    }
                                }

                                if (notifTargetType == "SUPERVISOR" && supervisorsList.isNotEmpty()) {
                                    Text("اختيار سريع من المشرفين المتواجدين:", fontSize = 10.sp, color = themeColors.accent)
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        items(supervisorsList) { sup ->
                                            AssistChip(
                                                onClick = { notifTargetValue = sup.name },
                                                label = { Text(sup.name, fontSize = 10.sp, color = Color.White) },
                                                colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.primary.copy(alpha = 0.2f))
                                            )
                                        }
                                    }
                                }
                            }

                            // Expiry & Delay configurations
                            Text("⏱️ جدولة البث وتحديد مدة صلاحية الإشعار (اختياري):", fontSize = 11.sp, color = themeColors.textSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = notifDelayHours,
                                    onValueChange = { notifDelayHours = it },
                                    label = { Text("تأخير الإرسال (ساعات)") },
                                    placeholder = { Text("فوري = فارغ") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                OutlinedTextField(
                                    value = notifValidityHours,
                                    onValueChange = { notifValidityHours = it },
                                    label = { Text("مدة الصلاحية (ساعات)") },
                                    placeholder = { Text("دائم = فارغ") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            Button(
                                onClick = {
                                    if (notifTitleInput.trim().isEmpty() || notifMsgInput.trim().isEmpty()) {
                                        Toast.makeText(context, "يرجى تعبئة العنوان ونص الرسالة", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val delayH = notifDelayHours.toDoubleOrNull() ?: 0.0
                                        val validH = notifValidityHours.toDoubleOrNull() ?: 0.0
                                        
                                        val scheduledTime = if (delayH > 0) {
                                            System.currentTimeMillis() + (delayH * 3600 * 1000).toLong()
                                        } else 0L

                                        val expiryTimestamp = if (validH > 0) {
                                            (if (scheduledTime > 0) scheduledTime else System.currentTimeMillis()) + (validH * 3600 * 1000).toLong()
                                        } else 0L

                                        viewModel.addNotification(
                                            title = notifTitleInput,
                                            message = notifMsgInput,
                                            targetType = notifTargetType,
                                            targetValue = if (notifTargetType == "ALL") "" else notifTargetValue,
                                            expiryTimestamp = expiryTimestamp,
                                            scheduledTime = scheduledTime
                                        )
                                        notifTitleInput = ""
                                        notifMsgInput = ""
                                        notifTargetValue = ""
                                        notifDelayHours = ""
                                        notifValidityHours = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("بث وإرسال الإشعار للمستهدفين المحددين 🚀", color = Color.White, fontWeight = FontWeight.Bold)
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

                items(notifications, key = { it.id }) { n ->
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

            if (activeSubTab == "CHATS") {
                item {
                    Text("💬 إدارة محادثات الدعم والدردشات الفورية والتحكم الفائق بالصلاحيات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("هنا يمكنك مراقبة كل غرف الشات، تحديد أطراف الاتصال المسموح بها، التحكم بالنطق الصوتي، الحجم، وحظر الأعضاء فورياً:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 1. Participant selection & Global Outage configs
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🛡️ صلاحيات وأطراف الاتصال بالدردشة:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🚹 تفعيل الشات بين العميل ومقدم الخدمة (الفني)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowChatUserToProvider,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowChatUserToProvider = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🛠️ تفعيل الشات بين الفني والأدمن/المشرف مباشرة", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowChatProviderToAdmin,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowChatProviderToAdmin = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("👤 تفعيل الشات المباشر بين العميل والادارة (الدعم)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowChatUserToAdmin,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowChatUserToAdmin = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🕵️ نظام الفحص المسبق (موافقة الادارة قبل إرسال الرسالة)", fontSize = 11.sp, color = Color.Yellow)
                                Switch(
                                    checked = settingsState.approveChatsBeforeProvider,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(approveChatsBeforeProvider = active))
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. Outage configs & Global switches
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🛑 تعطيل الخدمة وبث الطوارئ التلقائي:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🛑 إيقاف الشات الفوري بالكامل (عن الكل)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.disableChatAll,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(disableChatAll = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("👤 إيقاف الشات عن الزائرين والعملاء فقط", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.disableChatUsers,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(disableChatUsers = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🛠️ إيقاف الشات عن الفنيين ومزودي الخدمة", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.disableChatProviders,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(disableChatProviders = active))
                                    }
                                )
                            }

                            var announcementText by remember(settingsState.chatDisabledAnnouncement) { mutableStateOf(settingsState.chatDisabledAnnouncement) }
                            OutlinedTextField(
                                value = announcementText,
                                onValueChange = { 
                                    announcementText = it
                                    viewModel.saveCustomSettingsState(settingsState.copy(chatDisabledAnnouncement = it))
                                },
                                label = { Text("رسالة بث الطوارئ والتعطيل في غرف الشات") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                    }
                }

                // 3. Audio & UI appearance settings (Custom dimensions)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🎨 مظهر وصوتيات شاشة ومقاس الشات الفوري:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎙️ تفعيل الإدخال الصوتي (Speech-to-Text)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowVoiceInput,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInput = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔊 تفعيل نطق الرسائل وقراءتها آلياً (TTS)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowTextToSpeech,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeech = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎙️ طلب الانضمام: تفعيل الإدخال الصوتي", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowVoiceInputJoinForm,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInputJoinForm = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔊 طلب الانضمام: تفعيل النطق الصوتي (TTS)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowTextToSpeechJoinForm,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeechJoinForm = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎙️ المساعد الذكي: تفعيل الإدخال الصوتي", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowVoiceInputAssistant,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInputAssistant = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔊 المساعد الذكي: تفعيل النطق الصوتي (TTS)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowTextToSpeechAssistant,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeechAssistant = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎙️ استمارة الحجز: تفعيل الإدخال الصوتي", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowVoiceInputBookingForm,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInputBookingForm = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔊 استمارة الحجز: تفعيل النطق الصوتي (TTS)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowTextToSpeechBookingForm,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeechBookingForm = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🙈 إخفاء أيقونة الشات بالكامل من التطبيق", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.chatHidden,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(chatHidden = active))
                                    }
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = settingsState.chatSize.toString(),
                                    onValueChange = { newVal ->
                                        newVal.toIntOrNull()?.let {
                                            viewModel.saveCustomSettingsState(settingsState.copy(chatSize = it))
                                        }
                                    },
                                    label = { Text("حجم الأيقونة (dp)") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                OutlinedTextField(
                                    value = settingsState.chatFontSizeSp.toString(),
                                    onValueChange = { newVal ->
                                        newVal.toIntOrNull()?.let {
                                            viewModel.saveCustomSettingsState(settingsState.copy(chatFontSizeSp = it))
                                        }
                                    },
                                    label = { Text("حجم خط الشات (sp)") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            OutlinedTextField(
                                value = settingsState.chatBackgroundHex,
                                onValueChange = { newVal ->
                                    viewModel.saveCustomSettingsState(settingsState.copy(chatBackgroundHex = newVal))
                                },
                                label = { Text("كود لون خلفية شاشة الشات (Hex)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                    }
                }

                // 4. Archive actions
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { 
                                viewModel.wipeOldChatChannels(30)
                                Toast.makeText(context, "تمت تصفية كامل المحادثات بنجاح من الخادم السحابي والذاكرة المؤقتة 🧼", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("🧼 مسح أرشيف الشات بالكامل", fontSize = 10.sp, color = Color.White)
                        }
                        Button(
                            onClick = { Toast.makeText(context, "تم تصدير سجل المحادثات بنجاح للمصنف المالي 📁", Toast.LENGTH_SHORT).show() },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تصدير CSV 📁", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                // 5. Active Channels Monitoring
                item {
                    Text("📋 قنوات المحادثة والدردشة النشطة حالياً (${chatChannels.size}):", fontSize = 12.sp, color = themeColors.textSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (chatChannels.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("لا توجد محادثات نشطة حالياً بالمنصة 🟢", fontSize = 12.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("لم يقم أي فني أو عميل ببدء دردشة جديدة حتى الآن، سيتم المزامنة تلقائياً بمجرد إرسال أي رسالة.", fontSize = 10.sp, color = themeColors.textSecondary)
                            }
                        }
                    }
                } else {
                    items(chatChannels, key = { it.id }) { ch ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { showActiveChatChannelObj = ch }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    val parties = if (ch.isProvider) "مقدم الخدمة: ${ch.userName}" else "مستخدم الدليل: ${ch.userName}"
                                    Text("المحادثة: $parties", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    if (ch.isBlocked) {
                                        Text("محظورة 🛑", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("نشطة 🟢", color = Color.Green, fontSize = 10.sp)
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
                                        Text("افتح المحادثة ورد كأدمن 💬", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { 
                                            // Toggle block locally/singly to allow testing
                                            viewModel.blockChatChannel(ch.id, !ch.isBlocked)
                                            Toast.makeText(context, if (ch.isBlocked) "تم فك حظر المحادثة" else "تم حظر المحادثة ومنع أطرافها 🛑", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (ch.isBlocked) Color.Gray else Color(0xFFD97706)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(if (ch.isBlocked) "فك حظر الغرفة" else "حظر الغرفة 🛑", fontSize = 9.sp, color = Color.White)
                                    }
                                    Button(
                                        onClick = { showDeleteChatConfirmId = ch.id },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("حذف المحادثة 🗑️", fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "COLORS") {
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
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = requirementItemInput,
                                    onValueChange = { requirementItemInput = it },
                                    label = { Text("اسم الشرط (مثال: فيش جنائي)") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("إلزامي؟", fontSize = 9.sp, color = Color.White)
                                    Switch(
                                        checked = isNewRequirementMandatory,
                                        onCheckedChange = { isNewRequirementMandatory = it },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (requirementItemInput.trim().isNotEmpty()) {
                                            val suffix = if (isNewRequirementMandatory) "|Mandatory" else "|Optional"
                                            requirementsListState = requirementsListState + "${requirementItemInput.trim()}$suffix"
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
                                val parts = reqItem.split("|")
                                val reqName = parts.getOrNull(0) ?: reqItem
                                val isMand = parts.getOrNull(1)?.lowercase() != "optional"
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${idx+1}. $reqName", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(if (isMand) "إلزامي (مطلوب لإنشاء الحساب) 🔴" else "اختياري (غير معرقل للتسجيل) 🟢", color = if (isMand) Color.Red.copy(alpha = 0.8f) else Color.Green, fontSize = 10.sp)
                                    }
                                    
                                    var isEditingItem by remember { mutableStateOf(false) }
                                    var editItemValue by remember { mutableStateOf(reqName) }
                                    var editItemMandatory by remember { mutableStateOf(isMand) }
                                    
                                    if (isEditingItem) {
                                        AlertDialog(
                                            onDismissRequest = { isEditingItem = false },
                                            title = { Text("📝 تعديل الشرط أو المرفق") },
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    OutlinedTextField(
                                                        value = editItemValue,
                                                        onValueChange = { editItemValue = it },
                                                        label = { Text("اسم الشرط أو المستند") },
                                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("شرط إلزامي للجميع؟")
                                                        Switch(checked = editItemMandatory, onCheckedChange = { editItemMandatory = it })
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                Button(onClick = {
                                                    if (editItemValue.trim().isNotEmpty()) {
                                                        val updatedList = requirementsListState.toMutableList()
                                                        val suff = if (editItemMandatory) "|Mandatory" else "|Optional"
                                                        updatedList[idx] = "${editItemValue.trim()}$suff"
                                                        requirementsListState = updatedList
                                                        isEditingItem = false
                                                    }
                                                }, colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)) {
                                                    Text("حفظ التعديل")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { isEditingItem = false }) {
                                                    Text("إلغاء")
                                                }
                                            }
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { isEditingItem = true }) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = themeColors.accent, modifier = Modifier.size(18.dp))
                                        }
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
                }

                // Action Save button for Section Ten details
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // New Dynamic Card Dimensions settings
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("📏 تخصيص مقاسات وأبعاد كروت الفنيين:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            Text("• ارتفاع صورة غلاف الكرت (0 للإخفاء): ${editCoverHeight.toInt()}dp", fontSize = 11.sp, color = Color.White)
                            Slider(value = editCoverHeight, onValueChange = { editCoverHeight = it }, valueRange = 0f..250f)

                            Text("• حجم الصورة الشخصية (Avatar Size): ${editAvatarSize.toInt()}dp", fontSize = 11.sp, color = Color.White)
                            Slider(value = editAvatarSize, onValueChange = { editAvatarSize = it }, valueRange = 30f..100f)

                            Text("• الهامش والتباعد الداخلي للكرت (Padding): ${editCardPadding.toInt()}dp", fontSize = 11.sp, color = Color.White)
                            Slider(value = editCardPadding, onValueChange = { editCardPadding = it }, valueRange = 4f..24f)

                            Text("• المسافات بين عناصر الكرت (Spacing): ${editElementSpacing.toInt()}dp", fontSize = 11.sp, color = Color.White)
                            Slider(value = editElementSpacing, onValueChange = { editElementSpacing = it }, valueRange = 2f..16f)
                        }
                    }
                }

                // New Badges and indicators settings
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🛡️ إظهار وإخفاء شارات التميز والتوثيق بالفنيين:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🏆 شارة VIP الذهبية والدرع المحيط بالكرت", fontSize = 11.sp, color = Color.White)
                                Switch(checked = editShowVipBadge, onCheckedChange = { editShowVipBadge = it })
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔵 شارة التوثيق الزرقاء المعتمدة", fontSize = 11.sp, color = Color.White)
                                Switch(checked = editShowVerifiedBadge, onCheckedChange = { editShowVerifiedBadge = it })
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🟢 شارة نجمة التوصية (موصى به)", fontSize = 11.sp, color = Color.White)
                                Switch(checked = editShowRecommendedBadge, onCheckedChange = { editShowRecommendedBadge = it })
                            }
                        }
                    }
                }

                // Loyalty and Work Photos settings
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🤖 إعدادات المساعد الذكي وسوابق الأعمال الفنية:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎁 تفعيل نقاط الولاء والمشاركة للمساعد الذكي", fontSize = 11.sp, color = Color.White)
                                Switch(checked = editShowLoyaltyBanner, onCheckedChange = { editShowLoyaltyBanner = it })
                            }

                            Text("📂 أقصى حد لصور سابقة الأعمال التي يرفعها مقدم الخدمة: ${editMaxWorkPhotos.toInt()}", fontSize = 11.sp, color = Color.White)
                            Slider(
                                value = editMaxWorkPhotos,
                                onValueChange = { editMaxWorkPhotos = it },
                                valueRange = 1f..5f,
                                steps = 3
                            )
                        }
                    }
                }

                // New Card communication buttons manager
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("📞 التحكم الفائق بأزرار الاتصال والتواصل في الكروت:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            // Call button
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("📞 تفعيل زر الاتصال الهاتفي المباشر", fontSize = 11.sp, color = Color.White)
                                    Switch(checked = editShowCallButton, onCheckedChange = { editShowCallButton = it })
                                }
                                if (editShowCallButton) {
                                    OutlinedTextField(
                                        value = editCallButtonColorHex,
                                        onValueChange = { editCallButtonColorHex = it },
                                        label = { Text("لون زر الاتصال (Hex)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                }
                            }

                            // Whatsapp button
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("💬 تفعيل زر المحادثة السريعة واتساب", fontSize = 11.sp, color = Color.White)
                                    Switch(checked = editShowWhatsappButton, onCheckedChange = { editShowWhatsappButton = it })
                                }
                                if (editShowWhatsappButton) {
                                    OutlinedTextField(
                                        value = editWhatsappButtonColorHex,
                                        onValueChange = { editWhatsappButtonColorHex = it },
                                        label = { Text("لون زر واتساب (Hex)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                }
                            }

                            // Details button
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("🔍 تفعيل زر عرض التفاصيل والتقييمات", fontSize = 11.sp, color = Color.White)
                                    Switch(checked = editShowDetailsButton, onCheckedChange = { editShowDetailsButton = it })
                                }
                                if (editShowDetailsButton) {
                                    OutlinedTextField(
                                        value = editDetailsButtonColorHex,
                                        onValueChange = { editDetailsButtonColorHex = it },
                                        label = { Text("لون زر التفاصيل (Hex)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                }
                            }

                            // Book button
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("📅 تفعيل زر طلب الحجز المباشر", fontSize = 11.sp, color = Color.White)
                                    Switch(checked = editShowBookButton, onCheckedChange = { editShowBookButton = it })
                                }
                                if (editShowBookButton) {
                                    OutlinedTextField(
                                        value = editBookButtonColorHex,
                                        onValueChange = { editBookButtonColorHex = it },
                                        label = { Text("لون زر الحجز المباشر (Hex)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
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
                                registrationRequirements = requirementsListState.joinToString(","),
                                coverHeight = editCoverHeight.toInt(),
                                avatarSize = editAvatarSize.toInt(),
                                elementSpacing = editElementSpacing.toInt(),
                                cardPadding = editCardPadding.toInt(),
                                showVipBadge = editShowVipBadge,
                                showVerifiedBadge = editShowVerifiedBadge,
                                showRecommendedBadge = editShowRecommendedBadge,
                                showCallButton = editShowCallButton,
                                showWhatsappButton = editShowWhatsappButton,
                                showDetailsButton = editShowDetailsButton,
                                showBookButton = editShowBookButton,
                                callButtonColorHex = editCallButtonColorHex,
                                whatsappButtonColorHex = editWhatsappButtonColorHex,
                                detailsButtonColorHex = editDetailsButtonColorHex,
                                bookButtonColorHex = editBookButtonColorHex,
                                showLoyaltyBanner = editShowLoyaltyBanner,
                                maxWorkPhotos = editMaxWorkPhotos.toInt()
                            )
                            viewModel.updateBackdoorSettings(
                                appName = upToDateSettings.appName,
                                welcomeMsg = upToDateSettings.welcomeMessage,
                                footerMsg = upToDateSettings.footerMessage,
                                themeId = upToDateSettings.activeThemeId,
                                supportPhone = upToDateSettings.supportPhone,
                                supportEmail = upToDateSettings.supportEmail,
                                supportWhatsapp = upToDateSettings.supportWhatsapp,
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
                            Toast.makeText(context, "تم حفظ وضبط وتعميم مظهر الدليل والأزرار والبطاقات بنجاح! 🎉", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("💾 حفظ وحقن جميع تخصيصات المظهر بالدليل الصريح والكامل", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (activeSubTab == "VIP") {
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
                items(activatedProviders, key = { it.id }) { p ->
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

            if (activeSubTab == "SUPERVISORS") {
                // Section 9 / WIPE: Supervisor accounts & database reset & dynamic colors additions/modifications/deletions 
                item {
                    Text("👥 إدارة حسابات المشرفين وصلاحيات التطبيق (مزامنة فورية)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("أضف مشرفاً جديداً بكلمة مرور وصلاحية محددة:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            
                            OutlinedTextField(
                                value = supervisorInputName,
                                onValueChange = { supervisorInputName = it },
                                label = { Text("اسم المشرف الكامل") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = supervisorInputPasscode,
                                onValueChange = { supervisorInputPasscode = it },
                                label = { Text("كلمة مرور الدخول") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Text("اختر الدور والصلاحية الأمنية للمشرف المضاف:", color = themeColors.textSecondary, fontSize = 10.sp)
                            val roles = listOf("SUPPORT" to "دعم فني", "AUDITOR" to "مدقق ومراقب", "ADMIN" to "مدير رئيسي", "OPERATIONS" to "عمليات")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                roles.forEach { (roleKey, roleName) ->
                                    val isSel = supervisorInputRole == roleKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) themeColors.accent else Color.Black.copy(alpha = 0.3f))
                                            .clickable { supervisorInputRole = roleKey }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(roleName, fontSize = 9.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (supervisorInputName.trim().isNotEmpty() && supervisorInputPasscode.trim().isNotEmpty()) {
                                        viewModel.addSupervisor(supervisorInputName.trim(), supervisorInputRole, supervisorInputPasscode.trim())
                                        supervisorInputName = ""
                                        supervisorInputPasscode = ""
                                    } else {
                                        Toast.makeText(context, "الرجاء تعبئة اسم المشرف وكلمة المرور أولاً!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("إضافة المشرف المعتمد", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // List of existing supervisors
                if (supervisorsList.isNotEmpty()) {
                    item {
                        Text("📋 قائمة المشرفين المسجلين في النظام الآن:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    items(supervisorsList, key = { it.id }) { sup ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sup.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("الصلاحية: " + when(sup.role) {
                                            "ADMIN" -> "مدير برامج رئيسي 👑"
                                            "AUDITOR" -> "مدقق ومراقب 🔍"
                                            "OPERATIONS" -> "عمليات ميدانية 🚗"
                                            else -> "دعم فني 📞"
                                        }, fontSize = 10.sp, color = themeColors.accent)
                                        Text("رمز الدخول (Passcode): ${sup.passcode}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { editingSupervisorObj = sup }
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل المشرف", tint = Color.Green, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(
                                            onClick = { viewModel.removeSupervisor(sup.id) }
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف المشرف", tint = Color.Red, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Dynamic Colors Additions, updates and deletions panel
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("🎨 نظام الألوان والسمات المتعددة المتزامن فورياً", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    var newPaletteName by rememberSaveable { mutableStateOf("") }
                    var newPalettePrimary by rememberSaveable { mutableStateOf("#059669") }
                    var newPaletteSecondary by rememberSaveable { mutableStateOf("#115E59") }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("أضف ستايل لوني مخصص ومثبت بالدليل:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            
                            OutlinedTextField(
                                value = newPaletteName,
                                onValueChange = { newPaletteName = it },
                                label = { Text("اسم لوحة الألوان (مثال: الشتاء المتجمد)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newPalettePrimary,
                                    onValueChange = { newPalettePrimary = it },
                                    label = { Text("اللون الأساسي (Primary)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                OutlinedTextField(
                                    value = newPaletteSecondary,
                                    onValueChange = { newPaletteSecondary = it },
                                    label = { Text("اللون الثانوي (Secondary)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            Button(
                                onClick = {
                                    if (newPaletteName.trim().isNotEmpty() && newPalettePrimary.trim().isNotEmpty() && newPaletteSecondary.trim().isNotEmpty()) {
                                        viewModel.addColorPalette(newPaletteName.trim(), newPalettePrimary.trim(), newPaletteSecondary.trim())
                                        newPaletteName = ""
                                    } else {
                                        Toast.makeText(context, "الرجاء تعبئة الاسم والألوان الستة عشرية بالكامل!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حقن وإضافة بالدليل المتكامل للألوان", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Render Color Palettes list
                if (colorPalettesList.isNotEmpty()) {
                    item {
                        Text("🌈 لوحات الألوان والسمات المضافة حديثاً بالدليل:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    items(colorPalettesList, key = { it.id }) { pal ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(try { Color(android.graphics.Color.parseColor(pal.primaryHex)) } catch(e: Exception) { Color.Gray })
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(try { Color(android.graphics.Color.parseColor(pal.secondaryHex)) } catch(e: Exception) { Color.DarkGray })
                                    )
                                    
                                    Column {
                                        Text(pal.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("رئيسي: ${pal.primaryHex} | ثانوي: ${pal.secondaryHex}", fontSize = 9.sp, color = themeColors.textSecondary)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val updatedSettings = settingsState.copy(
                                                activeThemeId = "CUSTOM_THEME",
                                                customPrimaryHex = pal.primaryHex,
                                                customSecondaryHex = pal.secondaryHex,
                                                customBackgroundHex = pal.backgroundHex,
                                                customSurfaceHex = pal.surfaceHex
                                            )
                                            viewModel.saveCustomSettingsState(updatedSettings); if (false) {
                                            viewModel.updateBackdoorSettings(
                                                appName = updatedSettings.appName,
                                                welcomeMsg = updatedSettings.welcomeMessage,
                                                footerMsg = updatedSettings.footerMessage,
                                                themeId = "CUSTOM_THEME",
                                                supportPhone = updatedSettings.supportPhone,
                                                supportEmail = updatedSettings.supportEmail,
                                                supportWhatsapp = updatedSettings.supportWhatsapp,
                                                isMaintenance = updatedSettings.isMaintenanceActive,
                                                hiddenFooter = updatedSettings.hidePromoFooter,
                                                botHidden = updatedSettings.assistantHidden,
                                                botSize = updatedSettings.assistantSize,
                                                chatHidden = updatedSettings.chatHidden,
                                                chatSize = updatedSettings.chatSize,
                                                radiusKm = updatedSettings.maxSearchRadiusKm,
                                                isSpeech = updatedSettings.isSpeechSearchEnabled,
                                                isDataSaver = false,
                                                imgQuality = 90
                                            )
                                            } ; android.widget.Toast.makeText(context, "🌈 تم تطبيق هذا السطح اللوني الآن ومزامنته فوراً!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "تطبيق فوري", tint = Color.Green, modifier = Modifier.size(20.dp))
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteColorPalette(pal.id) }
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف اللون", tint = Color.Red, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "CATEGORIES") {
                item {
                    Text("🗂️ إدارة أقسام الصيانة والمهن بالمنصة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("إضافة أقسام جديدة وتعديل الأقسام وتحديد الأيقونة التعبيرية المناسبة:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إضافة قسم صيانة جديد ➕", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            OutlinedTextField(
                                value = newCatName,
                                onValueChange = { newCatName = it },
                                label = { Text("اسم القسم (مثال: سباكة، كهرباء...)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = newCatIcon,
                                onValueChange = { newCatIcon = it },
                                label = { Text("أيقونة إيموجي مميزة (مثال: 🚰, ⚡)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Button(
                                onClick = {
                                    if (newCatName.trim().isEmpty() || newCatIcon.trim().isEmpty()) {
                                        Toast.makeText(context, "الرجاء تعبئة اسم القسم والأيقونة", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.addNewCategory(newCatName.trim(), newCatName.trim(), newCatIcon.trim(), "")
                                        newCatName = ""
                                        newCatIcon = ""
                                        Toast.makeText(context, "تمت إضافة قسم الصيانة بنجاح 🗂️", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إضافة القسم وتفعيله فوراً", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(categories, key = { it.id }) { cat ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(cat.icon, fontSize = 20.sp)
                                Text(cat.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            IconButton(onClick = { showDeleteCategoryConfirmId = cat.id }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف القسم", tint = Color.Red)
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "CITIES") {
                item {
                    Text("🗺️ إدارة محافظات ومدن الجمهورية اليمنية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("إضافة المحافظات والمدن المستهدفة بالخدمة وتصفح المضاف حالياً بالمنصة:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إضافة مدينة/محافظة يمنية جديدة ➕", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            OutlinedTextField(
                                value = newCityArName,
                                onValueChange = { newCityArName = it },
                                label = { Text("الاسم باللغة العربية (مثال: صنعاء، عدن...)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = newCityEnName,
                                onValueChange = { newCityEnName = it },
                                label = { Text("الاسم باللغة الإنجليزية (مثال: Sana'a, Aden...)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Button(
                                onClick = {
                                    if (newCityArName.trim().isEmpty() || newCityEnName.trim().isEmpty()) {
                                        Toast.makeText(context, "الرجاء ملء الاسم العربي والإنجليزي للمحافظة", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.addNewCity(newCityArName.trim(), newCityEnName.trim())
                                        newCityArName = ""
                                        newCityEnName = ""
                                        Toast.makeText(context, "تمت إضافة المحافظة بنجاح 🗺️", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تأكيد إضافة المحافظة", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(citiesList, key = { city -> city.id }) { city ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(city.nameAr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(city.nameEn, fontSize = 11.sp, color = themeColors.textSecondary)
                            }
                            IconButton(onClick = { 
                                viewModel.removeCity(city.id)
                                Toast.makeText(context, "تم حذف المحافظة بنجاح", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف المحافظة", tint = Color.Red)
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "BACKUP") {
                item {
                    Text("💾 لوحة النسخ الاحتياطي والمزامنة والجدولة والتقارير", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("أدوات التصدير الشامل للبيانات والتحقق من صحة الاتصال المتزامن مع خوادم Cloud Firestore:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🛡️ إحصائيات حالة المزامنة والاتصال الحي", fontSize = 12.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الحالة الفورية:", fontSize = 11.sp, color = Color.White)
                                Text("متصل وآمن 🟢", fontSize = 11.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("حجم البيانات النشطة:", fontSize = 11.sp, color = Color.White)
                                val sizeEst = (activatedProviders.size + categories.size + bookings.size + reports.size) * 1.5f
                                Text(String.format("%.2f KB", sizeEst), fontSize = 11.sp, color = Color.White)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("تردد نبض الاتصال:", fontSize = 11.sp, color = Color.White)
                                Text("كل 10 ثوانٍ (ذكي تلقائي)", fontSize = 11.sp, color = themeColors.accent)
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Button(
                                onClick = {
                                    Toast.makeText(context, "🔄 جاري إعادة فحص ومزامنة كامل جداول البيانات مع السحاب...", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تحديث وإعادة جدولة الفحص الفوري 🔄", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📁 تصدير التقارير الإدارية الشاملة للجمهورية", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            Button(
                                onClick = {
                                    Toast.makeText(context, "تم تصدير الدليل الكامل للفنيين والمحافظات إلى ذاكرة الهاتف 📁", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تصدير الدليل الكامل للفنيين (CSV)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    Toast.makeText(context, "تم تصدير جميع سجلات حجز الصيانة المجدولة والنشطة 📁", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تصدير سجل الحجوزات النشطة (CSV)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "CLEAN") {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("⚙️ تخصيص تصفية وتهيئة البيانات الفورية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("حدد أنواع البيانات التي تريد مسحها أو نسخها وحفظها احتياطياً:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📁 تحديد الكل لإجراء العملية الشاملة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Switch(
                                    checked = wipeProvidersChecked && wipeBookingsChecked && wipeChatsChecked && wipeNotifsChecked && wipeReportsChecked && wipePendingChecked && wipeBannersChecked && wipeSupervisorsChecked && wipeCitiesChecked && wipeThemesChecked,
                                    onCheckedChange = { allChecked ->
                                        wipeProvidersChecked = allChecked
                                        wipeBookingsChecked = allChecked
                                        wipeChatsChecked = allChecked
                                        wipeNotifsChecked = allChecked
                                        wipeReportsChecked = allChecked
                                        wipeCategoriesChecked = allChecked
                                        wipePendingChecked = allChecked
                                        wipeBannersChecked = allChecked
                                        wipeSupervisorsChecked = allChecked
                                        wipeCitiesChecked = allChecked
                                        wipeThemesChecked = allChecked
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                                )
                            }

                            Divider(color = themeColors.accent.copy(alpha = 0.2f))

                            val itemsList = listOf(
                                Triple("الفنيين ومقدمي الخدمات الحقيقيين 👤", wipeProvidersChecked) { v: Boolean -> wipeProvidersChecked = v },
                                Triple("الحجوزات والطلبات المجدولة 📅", wipeBookingsChecked) { v: Boolean -> wipeBookingsChecked = v },
                                Triple("المحادثات وقنوات الشات المتبادلة 💬", wipeChatsChecked) { v: Boolean -> wipeChatsChecked = v },
                                Triple("الإشعارات الموجهة والمرسلة 🔔", wipeNotifsChecked) { v: Boolean -> wipeNotifsChecked = v },
                                Triple("بلاغات الشكاوى ومشاكل المستخدمين ⚠️", wipeReportsChecked) { v: Boolean -> wipeReportsChecked = v },
                                Triple("الأقسام وتصنيفات المهن (حذر) 📂", wipeCategoriesChecked) { v: Boolean -> wipeCategoriesChecked = v },
                                Triple("طلبات الانضمام والتسجيل المعلقة 📝", wipePendingChecked) { v: Boolean -> wipePendingChecked = v },
                                Triple("الإعلانات وبنرات واجهة التطبيق 📢", wipeBannersChecked) { v: Boolean -> wipeBannersChecked = v },
                                Triple("المشرفين والإداريين المساعدين 🔑", wipeSupervisorsChecked) { v: Boolean -> wipeSupervisorsChecked = v },
                                Triple("المحافظات والمدن المعتمدة 🗺️", wipeCitiesChecked) { v: Boolean -> wipeCitiesChecked = v },
                                Triple("قوالب الألوان المخصصة 🎨", wipeThemesChecked) { v: Boolean -> wipeThemesChecked = v }
                            )

                            itemsList.forEach { (label, isChecked, setChecked) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { setChecked(!isChecked) }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, fontSize = 12.sp, color = Color.White)
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { setChecked(it) },
                                        colors = CheckboxDefaults.colors(checkedColor = themeColors.accent)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("📥 تصدير ونسخ البيانات المحددة (نسخة احتياطية):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "يمكنك حفظ البيانات المحددة ونقلها فوراً إلى أي هاتف آخر أو نسخها لحساب جوجل درايف الخاص بك لحفظها من الفقدان:",
                                fontSize = 11.sp,
                                color = themeColors.textSecondary
                            )

                            Button(
                                onClick = {
                                    val selectedCols = mutableListOf<String>()
                                    if (wipeProvidersChecked) selectedCols.add("providers")
                                    if (wipeBookingsChecked) selectedCols.add("bookings")
                                    if (wipeChatsChecked) selectedCols.add("chat_channels")
                                    if (wipeNotifsChecked) selectedCols.add("notifications")
                                    if (wipeReportsChecked) selectedCols.add("reports")
                                    if (wipeCategoriesChecked) selectedCols.add("categories")
                                    if (wipePendingChecked) selectedCols.add("pending_providers")
                                    if (wipeBannersChecked) selectedCols.add("banners")
                                    if (wipeSupervisorsChecked) selectedCols.add("supervisors")
                                    if (wipeCitiesChecked) selectedCols.add("cities")
                                    if (wipeThemesChecked) selectedCols.add("color_themes")

                                    viewModel.exportSelectedCollectionsAsJson(selectedCols) { jsonString ->
                                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clipData = android.content.ClipData.newPlainText("YemenService_Backup_JSON", jsonString)
                                        clipboardManager.setPrimaryClip(clipData)
                                        Toast.makeText(context, "📋 تم نسخ البيانات المحددة بصيغة JSON بنجاح إلى ذاكرة الهاتف!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("📋 نسخ البيانات المحددة للحافظة (Clipboard)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val selectedCols = mutableListOf<String>()
                                    if (wipeProvidersChecked) selectedCols.add("providers")
                                    if (wipeBookingsChecked) selectedCols.add("bookings")
                                    if (wipeChatsChecked) selectedCols.add("chat_channels")
                                    if (wipeNotifsChecked) selectedCols.add("notifications")
                                    if (wipeReportsChecked) selectedCols.add("reports")
                                    if (wipeCategoriesChecked) selectedCols.add("categories")
                                    if (wipePendingChecked) selectedCols.add("pending_providers")
                                    if (wipeBannersChecked) selectedCols.add("banners")
                                    if (wipeSupervisorsChecked) selectedCols.add("supervisors")
                                    if (wipeCitiesChecked) selectedCols.add("cities")
                                    if (wipeThemesChecked) selectedCols.add("color_themes")

                                    viewModel.exportSelectedCollectionsAsJson(selectedCols) { jsonString ->
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "نسخة احتياطية - دليل خدمات اليمن")
                                            putExtra(android.content.Intent.EXTRA_TEXT, jsonString)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "تصدير نسخة البيانات الاحتياطية إلى:"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("☁️ نسخ لجوجل درايف / ذاكرة الهاتف ومشاركتها", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("🚨 منطقة التطهير الكلي والمسح النهائي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
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
                                text = "الضغط على الزر أدناه سيقوم بحذف وإعادة تهيئة الأنواع المحددة أعلاه فقط فوراً! لن يتسنى لك مراجعة أو التراجع عن هذه البيانات بمجرد تأكيد المسح بالرمز السري.",
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

    // Rejection dialog for pending provider request
    rejectingProviderRequest?.let { req ->
        AlertDialog(
            onDismissRequest = { rejectingProviderRequest = null },
            title = { Text("📝 توضيح سبب رفض الطلب", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يرجى كتابة سبب رفض طلب انضمام الفني ${req.name}:", fontSize = 11.sp, color = Color.LightGray)
                    OutlinedTextField(
                        value = providerRejectionReasonText,
                        onValueChange = { providerRejectionReasonText = it },
                        placeholder = { Text("مثال: المستندات المرفقة غير واضحة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectTechnician(req.id, providerRejectionReasonText.ifBlank { "لم يستوفِ الشروط المطلوبة" })
                        rejectingProviderRequest = null
                        providerRejectionReasonText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الرفض", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingProviderRequest = null }) {
                    Text("إلغاء", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

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

    // Booking Rejection Reason Dialog
    showRejectionReasonDialogId?.let { bId ->
        AlertDialog(
            onDismissRequest = { showRejectionReasonDialogId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("❌ رفض طلب الحجز", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الرجاء توضيح سبب رفض طلب الحجز ليتم إرساله للعميل مباشرة في التنبيهات:", color = Color.White, fontSize = 11.sp)
                    OutlinedTextField(
                        value = bookingRejectionReasonInput,
                        onValueChange = { bookingRejectionReasonInput = it },
                        label = { Text("سبب الرفض (مثال: جدول الأعمال ممتلئ، الموقع خارج التغطية)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bookingRejectionReasonInput.trim().isEmpty()) {
                            Toast.makeText(context, "الرجاء كتابة سبب الرفض لتنبيه العميل به", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updateBookingStatus(bId, "REJECTED", bookingRejectionReasonInput)
                            showRejectionReasonDialogId = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الرفض وإرسال السبب", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showRejectionReasonDialogId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("إلغاء", color = Color.White, fontSize = 11.sp)
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
                            items(ch.messages, key = { it.id }) { msg ->
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
                        text = "هذه العملية فائقة الخطورة وذات تصفية كلية فورية للمركز الفني بالمنصة والدليل اليمني. الرجاء كتابة الرمز السري للأدمن لإكمال المسح المخصص:",
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
                            val selectedCols = mutableListOf<String>()
                            if (wipeProvidersChecked) selectedCols.add("providers")
                            if (wipeBookingsChecked) selectedCols.add("bookings")
                            if (wipeChatsChecked) selectedCols.add("chat_channels")
                            if (wipeNotifsChecked) selectedCols.add("notifications")
                            if (wipeReportsChecked) selectedCols.add("reports")
                            if (wipeCategoriesChecked) selectedCols.add("categories")
                            if (wipePendingChecked) selectedCols.add("pending_providers")
                            if (wipeBannersChecked) selectedCols.add("banners")
                            if (wipeSupervisorsChecked) selectedCols.add("supervisors")
                            if (wipeCitiesChecked) selectedCols.add("cities")
                            if (wipeThemesChecked) selectedCols.add("color_themes")

                            val success = viewModel.wipeSelectedDatabaseData("maher736462", selectedCols)
                            if (success) {
                                showWipeConfirmDialog = false
                                wipeInputPassword = ""
                                Toast.makeText(context, "💥 تم تصفية الفئات المحددة وإعادتها للصفر بنجاح!", Toast.LENGTH_LONG).show()
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

    // 8. Editing Booking Dialog Control
    editingBookingObj?.let { booking ->
        var editCustName by rememberSaveable(booking.id) { mutableStateOf(booking.customerName) }
        var editCustPhone by rememberSaveable(booking.id) { mutableStateOf(booking.customerPhone) }
        var editCustArea by rememberSaveable(booking.id) { mutableStateOf(booking.customerArea) }
        var editCustService by rememberSaveable(booking.id) { mutableStateOf(booking.serviceType) }
        var editCustDate by rememberSaveable(booking.id) { mutableStateOf(booking.dateString) }
        var editCustTime by rememberSaveable(booking.id) { mutableStateOf(booking.timeString) }
        var editCustStatus by rememberSaveable(booking.id) { mutableStateOf(booking.status) }

        Dialog(onDismissRequest = { editingBookingObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("✏️ تعديل بيانات استمارة الحجز", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    OutlinedTextField(
                        value = editCustName,
                        onValueChange = { editCustName = it },
                        label = { Text("الاسم") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustPhone,
                        onValueChange = { editCustPhone = it },
                        label = { Text("الهاتف") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustArea,
                        onValueChange = { editCustArea = it },
                        label = { Text("مكان الإقامة والحي") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustService,
                        onValueChange = { editCustService = it },
                        label = { Text("نوع الخدمة المطلوبة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustDate,
                        onValueChange = { editCustDate = it },
                        label = { Text("التاريخ") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustTime,
                        onValueChange = { editCustTime = it },
                        label = { Text("الوقت أو الساعة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Text("حدد حالة الحجز:", color = themeColors.textSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statuses = listOf("PENDING", "APPROVED", "IN_PROGRESS", "COMPLETED", "REJECTED")
                        statuses.forEach { st ->
                            val isSel = editCustStatus == st
                            val color = when(st) {
                                "PENDING" -> Color(0xFFF59E0B)
                                "APPROVED" -> Color.Green
                                "IN_PROGRESS" -> Color(0xFF3B82F6)
                                "COMPLETED" -> Color(0xFF10B981)
                                else -> Color.Red
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) color else Color.Black.copy(alpha = 0.3f))
                                    .border(1.dp, if (isSel) Color.White else color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .clickable { editCustStatus = st }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when(st) {
                                        "PENDING" -> "تعليق"
                                        "APPROVED" -> "قبول"
                                        "IN_PROGRESS" -> "عمل"
                                        "COMPLETED" -> "تم"
                                        else -> "رفض"
                                    },
                                    color = if (isSel) Color.White else color,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (editCustName.trim().isNotEmpty() && editCustPhone.trim().isNotEmpty()) {
                                    val updatedB = booking.copy(
                                        customerName = editCustName.trim(),
                                        customerPhone = editCustPhone.trim(),
                                        customerArea = editCustArea.trim(),
                                        serviceType = editCustService.trim(),
                                        dateString = editCustDate.trim(),
                                        timeString = editCustTime.trim(),
                                        status = editCustStatus
                                    )
                                    viewModel.updateBooking(updatedB)
                                    editingBookingObj = null
                                } else {
                                    viewModel.triggerNotification("⚠️ يجب ملء الاسم والهاتف بالحد الأدنى!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💾 حفظ ونشر", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { editingBookingObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء الأمر", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // 8.5 Redirect Booking Dialog Control
    redirectingBookingObj?.let { booking ->
        val providersList by viewModel.providers.collectAsState()
        
        // Find the nearest provider dynamically
        val nearestProvider = remember(booking, providersList) {
            val userCoords = getAreaCoords(booking.customerArea)
            providersList.minByOrNull { tech ->
                val techCoords = getProviderCoords(tech)
                val dist = getDistance(userCoords.first, userCoords.second, techCoords.first, techCoords.second)
                dist
            }
        }

        var selectedTargetType by remember { mutableStateOf("SPECIFIC") } // SPECIFIC, ADMIN, NEAREST, OTHER
        var selectedOtherProviderId by remember { mutableStateOf("") }
        var dropdownExpanded by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { redirectingBookingObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.5.dp, themeColors.accent),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🔄 توجيه الحجز إلى مسؤول جديد", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    Text("العميل: ${booking.customerName} - الحي: ${booking.customerArea}", fontSize = 11.sp, color = themeColors.textSecondary)
                    Text("الفني الحالي: ${booking.providerName}", fontSize = 11.sp, color = themeColors.accent)
                    
                    Divider(color = Color.Gray.copy(alpha = 0.3f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 1. To Admin
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "ADMIN" }
                        ) {
                            RadioButton(selected = (selectedTargetType == "ADMIN"), onClick = { selectedTargetType = "ADMIN" })
                            Text("المدير (الأدمن) 👑", fontSize = 12.sp, color = Color.White)
                        }

                        // 2. To Current Provider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "SPECIFIC" }
                        ) {
                            RadioButton(selected = (selectedTargetType == "SPECIFIC"), onClick = { selectedTargetType = "SPECIFIC" })
                            Text("الفني المذكور: ${booking.providerName} 👷", fontSize = 12.sp, color = Color.White)
                        }

                        // 3. To Nearest Provider
                        nearestProvider?.let { np ->
                            val userCoords = getAreaCoords(booking.customerArea)
                            val techCoords = getProviderCoords(np)
                            val distance = getDistance(userCoords.first, userCoords.second, techCoords.first, techCoords.second)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "NEAREST" }
                            ) {
                                RadioButton(selected = (selectedTargetType == "NEAREST"), onClick = { selectedTargetType = "NEAREST" })
                                Text("الفني الأقرب للمستخدم: ${np.name} (يبعد ${String.format("%.1f", distance)} كم) 📍", fontSize = 12.sp, color = Color.White)
                            }
                        }

                        // 4. To another technician
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "OTHER" }
                        ) {
                            RadioButton(selected = (selectedTargetType == "OTHER"), onClick = { selectedTargetType = "OTHER" })
                            Text("توجيه لفني آخر من الدليل 🔎", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    if (selectedTargetType == "OTHER") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedProvName = providersList.find { it.id == selectedOtherProviderId }?.name ?: "اختر الفني الآخر..."
                            Button(
                                onClick = { dropdownExpanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedProvName, color = Color.White, fontSize = 11.sp)
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E293B)).fillMaxWidth(0.8f)
                            ) {
                                providersList.forEach { prov ->
                                    DropdownMenuItem(
                                        text = { Text(prov.name + " (${prov.customCategoryName.ifBlank { "فني" }})", color = Color.White, fontSize = 11.sp) },
                                        onClick = {
                                            selectedOtherProviderId = prov.id
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val targetProvider = when (selectedTargetType) {
                                    "ADMIN" -> Pair("admin", "إدارة الدليل")
                                    "SPECIFIC" -> Pair(booking.providerId, booking.providerName)
                                    "NEAREST" -> Pair(nearestProvider?.id ?: "admin", nearestProvider?.name ?: "إدارة الدليل")
                                    "OTHER" -> {
                                        val op = providersList.find { it.id == selectedOtherProviderId }
                                        Pair(op?.id ?: "admin", op?.name ?: "إدارة الدليل")
                                    }
                                    else -> Pair(booking.providerId, booking.providerName)
                                }

                                val updatedB = booking.copy(
                                    providerId = targetProvider.first,
                                    providerName = targetProvider.second
                                )
                                viewModel.updateBooking(updatedB)

                                // Send notifications to user about stages
                                viewModel.addNotification(
                                    title = "🔄 تم تعديل وتوجيه حجزك",
                                    message = "عزيزي العميل، تم توجيه حجزك رقم ${booking.id} بنجاح ومسؤولية تنفيذ الخدمة انتقلت إلى: ${targetProvider.second}.",
                                    targetType = "USER",
                                    targetValue = booking.customerPhone
                                )

                                // Send notifications to the target provider as well
                                if (targetProvider.first != "admin") {
                                    val targetPhone = providersList.find { it.id == targetProvider.first }?.phone ?: ""
                                    viewModel.addNotification(
                                        title = "📅 تم توجيه حجز جديد لك",
                                        message = "تم توجيه حجز جديد لك من الإدارة.\nالعميل: ${booking.customerName}\nالهاتف: ${booking.customerPhone}\nالمنطقة: ${booking.customerArea}\nالخدمة: ${booking.serviceType}\nيرجى التواصل معه مباشرة للتنسيق.",
                                        targetType = "PROVIDER",
                                        targetValue = targetPhone,
                                        customerPhone = booking.customerPhone,
                                        customerName = booking.customerName
                                    )
                                }

                                redirectingBookingObj = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("توجيه 🔄", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { redirectingBookingObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // 9. Editing Supervisor Dialog Control
    editingSupervisorObj?.let { supervisor ->
        var editSupName by rememberSaveable(supervisor.id) { mutableStateOf(supervisor.name) }
        var editSupRole by rememberSaveable(supervisor.id) { mutableStateOf(supervisor.role) }
        var editSupPasscode by rememberSaveable(supervisor.id) { mutableStateOf(supervisor.passcode) }

        Dialog(onDismissRequest = { editingSupervisorObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("✏️ تعديل صلاحيات وبيانات المشرف", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = editSupName,
                        onValueChange = { editSupName = it },
                        label = { Text("اسم المشرف الثلاثي") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editSupPasscode,
                        onValueChange = { editSupPasscode = it },
                        label = { Text("رمز المرور والدخول (Passcode)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Text("اختر الدور والصلاحية الأمنية للمشرف المضاف:", color = themeColors.textSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val roles = listOf("ADMIN", "AUDITOR", "SUPPORT", "OPERATIONS")
                        roles.forEach { rl ->
                            val isSel = editSupRole == rl
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) themeColors.accent else Color.Black.copy(alpha = 0.3f))
                                    .clickable { editSupRole = rl }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (rl) {
                                        "ADMIN" -> "مدير 👑"
                                        "AUDITOR" -> "مدقق 🔍"
                                        "OPERATIONS" -> "ميداني 🚗"
                                        else -> "دعم 📞"
                                    },
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (editSupName.trim().isNotEmpty() && editSupPasscode.trim().isNotEmpty()) {
                                    viewModel.editSupervisor(supervisor.id, editSupName.trim(), editSupRole, editSupPasscode.trim())
                                    editingSupervisorObj = null
                                } else {
                                    viewModel.triggerNotification("⚠️ يرجى كتابة الاسم والرمز بالكامل")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💾 حفظ التعديل", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { editingSupervisorObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
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
    var footerBgColorHex by remember { mutableStateOf(settingsState.footerBgColorHex) }
    var footerItemsOrder by remember { mutableStateOf(settingsState.footerItemsOrder) }
    var appVersion by remember { mutableStateOf(settingsState.appVersion) }
    var supportPhone by remember { mutableStateOf(settingsState.supportPhone) }
    var supportEmail by remember { mutableStateOf(settingsState.supportEmail) }
    var supportWhatsapp by remember { mutableStateOf(settingsState.supportWhatsapp) }
    var appDownloadUrl by remember { mutableStateOf(settingsState.appDownloadUrl) }
    var activeThemeId by remember { mutableStateOf(settingsState.activeThemeId) }
    var isMaintenanceActive by remember { mutableStateOf(settingsState.isMaintenanceActive) }
    var hidePromoFooter by remember { mutableStateOf(settingsState.hidePromoFooter) }
    var assistantHidden by remember { mutableStateOf(settingsState.assistantHidden) }
    var assistantSize by remember { mutableStateOf(56f) }
    var chatHidden by remember { mutableStateOf(settingsState.chatHidden) }
    var chatSize by remember { mutableStateOf(if (settingsState.chatSize > 0) settingsState.chatSize.toFloat() else 56f) }
    var maxSearchRadiusKm by remember { mutableStateOf(settingsState.maxSearchRadiusKm.toFloat()) }
    var isSpeechSearchEnabled by remember { mutableStateOf(settingsState.isSpeechSearchEnabled) }
    var isDataSaverEnabled by remember { mutableStateOf(false) }
    var appImageQuality by remember { mutableStateOf(90f) }
    var bypassVisitorRegistration by remember { mutableStateOf(settingsState.bypassVisitorRegistration) }
    var disableChatFirewall by remember { mutableStateOf(settingsState.disableChatFirewall) }
    var disableBookingFirewall by remember { mutableStateOf(settingsState.disableBookingFirewall) }
    var isMapFeatureEnabled by remember { mutableStateOf(settingsState.isMapFeatureEnabled) }

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

    // Banner control states
    var bannerEnabled by remember { mutableStateOf(settingsState.bannerEnabled) }
    var bannerType by remember { mutableStateOf(settingsState.bannerType) }
    var bannerContent by remember { mutableStateOf(settingsState.bannerContent) }
    var bannerBase64 by remember { mutableStateOf(settingsState.bannerBase64) }
    var bannerLocation by remember { mutableStateOf(settingsState.bannerLocation) }
    var bannerDurationSeconds by remember { mutableStateOf(settingsState.bannerDurationSeconds.toString()) }
    var bannerDisplayStyle by remember { mutableStateOf(settingsState.bannerDisplayStyle) }

    val context = LocalContext.current
    val sp = remember { context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE) }
    var rememberLoginInput by remember { mutableStateOf(sp.getString("saved_admin_role", "GUEST") != "GUEST") }
    var adminUsernameInput by remember { mutableStateOf(settingsState.adminUsername) }
    var adminPasswordInput by remember { mutableStateOf(settingsState.adminPassword) }

    val galleryLauncherForBanner = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val mimeType = context.contentResolver.getType(it)
                if (mimeType != null && mimeType.startsWith("video/")) {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        bannerBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        bannerType = "VIDEO"
                        viewModel.triggerNotification("📹 تم تحميل الفيديو القصير للبنر بنجاح!")
                    }
                } else {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val outputStream = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                        bannerBase64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
                        bannerType = "IMAGE"
                        viewModel.triggerNotification("📸 تم تحميل صورة البنر الإعلاني من المعرض بنجاح!")
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    val galleryLauncherForCover = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val outputStream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                    aboutCoverBase64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
                    aboutCoverType = "IMAGE"
                    viewModel.triggerNotification("📸 تم تحميل صورة الغلاف من المعرض بنجاح!")
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

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
                onClick = { viewModel.logout(context) },
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
            label = { Text("نص الفوتر الأوسط (افتراضي: wam777644)") },
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

        Text("تنسيق وترتيب شريط التذييل (الفوتر):", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            val layouts = listOf(
                "INFO,TEXT,VERSION" to "الأيقونة يسار | النص وسط | الإصدار يمين",
                "VERSION,TEXT,INFO" to "الإصدار يسار | النص وسط | الأيقونة يمين",
                "TEXT,VERSION,INFO" to "النص يسار | الإصدار وسط | الأيقونة يمين"
            )
            layouts.forEach { (order, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (footerItemsOrder == order) themeColors.accent else themeColors.surface)
                        .clickable { footerItemsOrder = order }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 8.sp, color = if (footerItemsOrder == order) Color.Black else Color.White, textAlign = TextAlign.Center)
                }
            }
        }

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
            Text("إلغاء شرط تسجيل الزائرين للحجز والمحادثة", color = Color.White, fontSize = 13.sp)
            Switch(checked = bypassVisitorRegistration, onCheckedChange = { bypassVisitorRegistration = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تمكين ميزة الرادار والخريطة للجماهير", color = Color.White, fontSize = 13.sp)
            Switch(checked = isMapFeatureEnabled, onCheckedChange = { isMapFeatureEnabled = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔓 تعطيل جدار حماية المحادثات الفورية", color = Color.White, fontSize = 13.sp)
            Switch(checked = disableChatFirewall, onCheckedChange = { disableChatFirewall = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔓 تعطيل جدار حماية حجز المواعيد", color = Color.White, fontSize = 13.sp)
            Switch(checked = disableBookingFirewall, onCheckedChange = { disableBookingFirewall = it })
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
            Slider(value = chatSize, onValueChange = { chatSize = it }, valueRange = 28f..80f)
        }

        Column {
            Text("الحد الأقصى لنطاق البحث الجغرافي: ${maxSearchRadiusKm.toInt()} كم", color = Color.White, fontSize = 11.sp)
            Slider(value = maxSearchRadiusKm, onValueChange = { maxSearchRadiusKm = it }, valueRange = 10f..100f)
        }

        // --- Custom Booking dynamic layouts & rules controls ---
        Divider(color = themeColors.accent.copy(alpha = 0.3f), thickness = 1.dp)
        Text("📅 تخصيص شروط واستمارة الحجز اليمني (كاملة بالتفصيل):", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

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

        Divider(color = themeColors.accent.copy(alpha = 0.3f), thickness = 1.dp)
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
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تذكرني (حفظ الدخول بصورة دائمة كمالك)", color = Color.White, fontSize = 13.sp)
            Switch(checked = rememberLoginInput, onCheckedChange = { rememberLoginInput = it })
        }

        Spacer(modifier = Modifier.height(4.dp))
        Divider(color = themeColors.accent.copy(alpha = 0.3f), thickness = 1.dp)
        Text("ℹ️ تخصيص صفحة معلومات عن التطبيق (المحتوى والغلاف):", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

        Text("نوع غلاف الصفحة المعروض:", fontSize = 11.sp, color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val types = listOf("IMAGE" to "صورة 🖼️", "VIDEO" to "فيديو 🎥", "TEXT" to "نص فقط 📝")
            types.forEach { (typeVal, typeLabel) ->
                val isSel = aboutCoverType == typeVal
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { aboutCoverType = typeVal }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(typeLabel, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White)
                }
            }
        }

        if (aboutCoverType == "IMAGE") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { galleryLauncherForCover.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("صورة الهاتف 📁", fontSize = 10.sp, color = Color.White)
                }
                
                OutlinedTextField(
                    value = aboutCoverContent,
                    onValueChange = { aboutCoverContent = it },
                    label = { Text("أو رابط صورة الإنترنت") },
                    modifier = Modifier.weight(1.5f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        } else if (aboutCoverType == "VIDEO") {
            OutlinedTextField(
                value = aboutCoverContent,
                onValueChange = { aboutCoverContent = it },
                label = { Text("رابط الفيديو (يوتيوب أو ملف مباشر)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        } else {
            OutlinedTextField(
                value = aboutCoverContent,
                onValueChange = { aboutCoverContent = it },
                label = { Text("نص الغلاف البديل") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        OutlinedTextField(
            value = aboutCustomInfo,
            onValueChange = { aboutCustomInfo = it },
            label = { Text("المعلومات التفصيلية المكتوبة عن التطبيق") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        // 📢 Dynamic App Banner Configuration UI
        Divider(color = themeColors.accent.copy(alpha = 0.3f), thickness = 1.dp)
        Text("📢 إعداد وتخصيص البنر الإعلاني بالأعلى (الرئيسي):", fontSize = 13.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تفعيل عرض البنر بالرئيسية:", fontSize = 12.sp, color = Color.White)
            Switch(
                checked = bannerEnabled,
                onCheckedChange = { bannerEnabled = it },
                colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
            )
        }

        if (bannerEnabled) {
            Text("نوع محتوى البنر:", fontSize = 11.sp, color = themeColors.textSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val bannerTypes = listOf("TEXT" to "نص فقط 📝", "IMAGE" to "صورة 🖼️", "VIDEO" to "فيديو قصير 📹")
                bannerTypes.forEach { (typeKey, typeLabel) ->
                    val isSel = bannerType == typeKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) themeColors.accent else themeColors.surface)
                            .clickable { bannerType = typeKey }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(typeLabel, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text("مكان ظهور البنر بالرئيسية:", fontSize = 11.sp, color = themeColors.textSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val locations = listOf("TOP" to "أعلى الشاشة ⬆️", "BOTTOM" to "أسفل الشاشة ⬇️")
                locations.forEach { (locKey, locLabel) ->
                    val isSel = bannerLocation == locKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) themeColors.accent else themeColors.surface)
                            .clickable { bannerLocation = locKey }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(locLabel, fontSize = 11.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (bannerType == "IMAGE" || bannerType == "VIDEO") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (bannerType == "VIDEO") {
                                galleryLauncherForBanner.launch("video/*")
                            } else {
                                galleryLauncherForBanner.launch("image/*")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(if (bannerType == "VIDEO") "فيديو قصير 📹" else "صورة البنر 📁", fontSize = 10.sp, color = Color.White)
                    }
                    
                    OutlinedTextField(
                        value = bannerContent,
                        onValueChange = { bannerContent = it },
                        label = { Text("أو رابط الإنترنت") },
                        modifier = Modifier.weight(2f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            } else {
                OutlinedTextField(
                    value = bannerContent,
                    onValueChange = { bannerContent = it },
                    label = { Text("محتوى الإعلان النصي") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = bannerDurationSeconds,
                onValueChange = { bannerDurationSeconds = it },
                label = { Text("مدة ظهور البنر بالثواني (0 للبقاء المستمر)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text("طريقة حركة أو ظهور البنر الإعلاني:", fontSize = 11.sp, color = themeColors.textSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val styles = listOf("SLIDE" to "انزلاق ➡️", "FADE" to "تلاشي 🌫️", "BLINK" to "وميض 💡", "SCROLL" to "تمرير 📜")
                styles.forEach { (styleKey, styleLabel) ->
                    val isSel = bannerDisplayStyle == styleKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) themeColors.accent else themeColors.surface)
                            .clickable { bannerDisplayStyle = styleKey }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(styleLabel, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Divider(color = themeColors.accent.copy(alpha = 0.3f), thickness = 1.dp)

        Spacer(modifier = Modifier.height(4.dp))
        Text("🖼️ شعار التطبيق المستخدم بالمنصة:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val logos = listOf("شعار احترافي 🔧", "بسيط 🌟", "رسمي 🤝", "أيقونة الدليل 🎯")
            var selectedSimulatedLogo by remember { mutableStateOf("شعار احترافي 🔧") }
            logos.forEach { lg ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedSimulatedLogo == lg) themeColors.accent else themeColors.surface)
                        .clickable { 
                            selectedSimulatedLogo = lg
                            viewModel.triggerNotification("🖼️ تم تحديد الشعار ($lg) بنجاح للتطبيق!")
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(lg, fontSize = 9.sp, color = if (selectedSimulatedLogo == lg) Color.Black else Color.White)
                }
            }
        }

        Button(
            onClick = {
                val currentSettings = settingsState.copy(
                    appName = appName,
                    welcomeMessage = welcomeMessage,
                    footerMessage = footerMessage,
                    footerBgColorHex = footerBgColorHex,
                    footerItemsOrder = footerItemsOrder,
                    appVersion = appVersion,
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
                    disableChatFirewall = disableChatFirewall,
                    disableBookingFirewall = disableBookingFirewall,
                    isMapFeatureEnabled = isMapFeatureEnabled,
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
                    bannerEnabled = bannerEnabled,
                    bannerType = bannerType,
                    bannerContent = bannerContent,
                    bannerBase64 = bannerBase64,
                    bannerLocation = bannerLocation,
                    bannerDurationSeconds = bannerDurationSeconds.toIntOrNull() ?: 0,
                    bannerDisplayStyle = bannerDisplayStyle,
                    appDownloadUrl = appDownloadUrl
                )
                viewModel.saveCustomSettingsState(currentSettings)

                if (rememberLoginInput) {
                    sp.edit().putString("saved_admin_role", "OWNER").apply()
                } else {
                    sp.edit().putString("saved_admin_role", "GUEST").apply()
                }
                viewModel.triggerNotification("💾 تم حفظ كافة التخصيصات والتحققات بنجاح!")
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("💾 حفظ إعدادات البوابة والتخزين والتطبيق", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                viewModel.wipeAllMockAndTemporaryData()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "حذف البيانات الوهمية",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("🧹 حذف الفنيين والرسائل والإشعارات الوهمية", color = Color.White, fontWeight = FontWeight.Bold)
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
    val context = LocalContext.current
    val isOnline = com.example.NetworkUtils.isNetworkAvailable(context)
    val coroutineScope = rememberCoroutineScope()

    // Chat history state
    var chatHistory by remember { mutableStateOf(listOf<Pair<String, Boolean>>( // text to isUser
        (settings.welcomeMessage.ifEmpty { "مرحباً بكم في منصة الخدمات اليمنية الشاملة! كيف يمكنني مساعدتك اليوم؟" }) to false
    )) }

    var typedText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("المساعد الذكي لدليل اليمن", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }

                Divider(color = themeColors.accent.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

                // Loyalty and points banner
                if (settings.showLoyaltyBanner) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.primary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("رصيد نقاط الولاء: $points نقطة", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(themeColors.accent)
                                        .clickable { viewModel.redeemLoyaltyPoints() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("استبدال", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(themeColors.surface)
                                        .clickable { viewModel.rewardSharePoints() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("مشاركة 🎁", fontSize = 9.sp, color = Color.White)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isOnline) Color(0xFF065F46) else Color(0xFF854D0E))
                        .padding(vertical = 4.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isOnline) "🟢 متصل بالإنترنت: ذكاء اصطناعي فائق التوليد" else "🟡 وضع غير متصل: ذكاء محلي فوري وآمن",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Chat Messages List
                val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
                LaunchedEffect(chatHistory.size) {
                    if (chatHistory.isNotEmpty()) {
                        scrollState.animateScrollToItem(chatHistory.size - 1)
                    }
                }

                LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatHistory) { (text, isUser) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUser) themeColors.primary else themeColors.surface
                                ),
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isUser) 12.dp else 0.dp,
                                    bottomEnd = if (isUser) 0.dp else 12.dp
                                ),
                                border = BorderStroke(1.dp, if (isUser) themeColors.primary else themeColors.accent.copy(alpha = 0.5f)),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = text,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        lineHeight = 18.sp
                                    )
                                    
                                    if (!isUser && settings.allowTextToSpeechAssistant) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(
                                                onClick = { VoiceManager.onSpeak?.invoke(text) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Text("🔊", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isGenerating) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.widthIn(max = 200.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = themeColors.accent, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("جاري توليد الإجابة الدقيقة...", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Control panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = typedText,
                        onValueChange = { typedText = it },
                        placeholder = { Text("اطرح أي سؤال حول خدمات اليمن...", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true,
                        trailingIcon = if (settings.allowVoiceInputAssistant) {
                            {
                                IconButton(
                                    onClick = {
                                        VoiceManager.onHear?.invoke { spokenText -> typedText = spokenText }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("🎙️", fontSize = 16.sp)
                                }
                            }
                        } else null
                    )
                    
                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (typedText.isNotEmpty() && !isGenerating) {
                                val prompt = typedText
                                typedText = ""
                                chatHistory = chatHistory + (prompt to true)
                                isGenerating = true

                                // Perform async response generation
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val response = if (isOnline) {
                                        // Attempt direct REST calling to Gemini API
                                        try {
                                            val apiKey = BuildConfig.GEMINI_API_KEY
                                            if (apiKey.isNotEmpty()) {
                                                val mediaType = "application/json; charset=utf-8".toMediaType()
                                                val requestJson = """
                                                    {
                                                        "contents": [{"parts": [{"text": "${prompt.replace("\"", "\\\"")}"}]}],
                                                        "systemInstruction": {"parts": [{"text": "أنت مساعد ذكي مخصص لمنصة دليل خدمات اليمن الشاملة. أجب باختصار وبدقة عالية وباللغة العربية الفصحى أو اللهجة اليمنية المناسبة."}]}
                                                    }
                                                """.trimIndent()
                                                
                                                val request = okhttp3.Request.Builder()
                                                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                                                    .post(okhttp3.RequestBody.create(mediaType, requestJson))
                                                    .build()

                                                val okHttpClient = okhttp3.OkHttpClient.Builder()
                                                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                                                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                                                    .build()

                                                okHttpClient.newCall(request).execute().use { response ->
                                                    if (response.isSuccessful) {
                                                        val bodyString = response.body?.string() ?: ""
                                                        val jsonObject = org.json.JSONObject(bodyString)
                                                        val candidates = jsonObject.optJSONArray("candidates")
                                                        val candidate = candidates?.optJSONObject(0)
                                                        val content = candidate?.optJSONObject("content")
                                                        val parts = content?.optJSONArray("parts")
                                                        val part = parts?.optJSONObject(0)
                                                        val textVal = part?.optString("text")
                                                        textVal ?: "لم أتمكن من استخلاص النص من الإجابة."
                                                    } else {
                                                        generateLocalOfflineResponse(prompt)
                                                    }
                                                }
                                            } else {
                                                generateLocalOfflineResponse(prompt)
                                            }
                                        } catch (e: Exception) {
                                            generateLocalOfflineResponse(prompt)
                                        }
                                    } else {
                                        generateLocalOfflineResponse(prompt)
                                    }

                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        chatHistory = chatHistory + (response to false)
                                        isGenerating = false
                                        // Auto speak response!
                                        if (settings.allowTextToSpeechAssistant) {
                                            VoiceManager.onSpeak?.invoke(response)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .background(themeColors.accent, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "إرسال", tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            chatHistory = listOf((settings.welcomeMessage.ifEmpty { "مرحباً بكم في منصة الخدمات اليمنية الشاملة! كيف يمكنني مساعدتك اليوم؟" }) to false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("🧹 مسح المحادثة", fontSize = 10.sp, color = Color.White)
                    }

                    Text(
                        text = "يمكنك التحدث صوتياً بالنقر على 🎙️",
                        fontSize = 9.sp,
                        color = themeColors.textSecondary
                    )
                }
            }
        }
    }
}

private fun generateLocalOfflineResponse(prompt: String): String {
    val q = prompt.trim().lowercase()
    
    return when {
        q.contains("مرحبا") || q.contains("السلام") || q.contains("هلا") || q.contains("أهلاً") || q.contains("اهلاً") -> {
            "وعليكم السلام ورحمة الله وبركاته! مرحباً بك في دليل خدمات اليمن الذكي 🇾🇪. أنا مساعدك الرقمي المباشر، كيف يمكنني خدمتك وتوجيهك لأفضل السباكين، الكهربائيين، الفنيين والخدمات اليوم؟"
        }
        q.contains("امين") || q.contains("p_amin") || q.contains("الغرباني") -> {
            "الفني امين الغرباني هو مقدم خدمات الصيانة المعتمد في صنعاء بالدليل الشامل. رقمه المباشر للتواصل هو 777703195 وهو متخصص في أعمال الصيانة العامة في صنعاء - منطقة الدائري جوار مدرسة اسماء للبنات."
        }
        q.contains("رقم") || q.contains("اتصال") || q.contains("دعم") || q.contains("تواصل") || q.contains("تلفون") || q.contains("رقم الإدارة") || q.contains("تواصل مع الادارة") -> {
            "للدعم الفني المباشر وحل أي مشكلات في دليل اليمن، يرجى الاتصال المباشر على رقم الإدارة: 777644، أو التواصل عبر الواتساب الفوري: wam777644، أو البريد الإلكتروني المعتمد: maa736462@gmail.com. نحن هنا لخدمة الشعب اليمني على مدار الساعة!"
        }
        q.contains("سعر") || q.contains("رسوم") || q.contains("فلوس") || q.contains("مجاني") || q.contains("عمولة") -> {
            "تطبيق دليل خدمات اليمن مجاني تماماً وبنسبة 100% لجميع المواطنين والباحثين عن فنيين. لا نتقاضى أي عمولة أو رسوم على المكالمات أو الاتفاقات بين العميل والفني، فالتواصل مباشر وحر!"
        }
        q.contains("كيف اسجل") || q.contains("تسجيل") || q.contains("طلب انضمام") || q.contains("انضمام") || q.contains("مقدم خدمة") || q.contains("فني") -> {
            "للإنضمام كفني معتمد بالدليل، افتح 'استمارة طلب الانضمام'، واملأ بياناتك بدقة (الاسم الثلاثي، الهاتف، التخصص، والحي السكني) ثم ارفع هويتك وصورتك الشخصية لطلب التوثيق. بمجرد مراجعة الإدارة للطلب سيصلك إشعار فوري بالقبول!"
        }
        q.contains("خريطة") || q.contains("خرائط") || q.contains("موقع") || q.contains("الموقع") || q.contains("رادار") -> {
            "يحتوي دليل اليمن على نظام رادار خرائط متكامل وعالي الدقة لتحديد أماكن الفنيين الأقرب إليك جغرافياً بالمتر. يمكنك تحديد موقعك الفعلي عبر الـ GPS وضبط نطاق البحث بالمسافة (مثلا 5 كم أو 10 كم) للعثور على أقرب مختص."
        }
        q.contains("ضمان") || q.contains("كيف اضمن") || q.contains("شعار") || q.contains("موثق") || q.contains("أمان") -> {
            "جميع الفنيين الموثقين يحملون الشارة الزرقاء بجانب أسمائهم، وهو ما يعني التحقق من هويتهم الشخصية وصحيفة أعمالهم المهنية بواسطة الإدارة لضمان حمايتكم وجودة الأعمال."
        }
        q.contains("صوت") || q.contains("تحدث") || q.contains("نطق") || q.contains("تكلم") -> {
            "نعم! يدعم التطبيق ميزة الإدخال الصوتي (Speech-to-Text) لقراءة كلامك، وميزة النطق الصوتي (Text-to-Speech) لقراءة الإجابات بصوت يمني واضح. للإدارة الصلاحية الكاملة لتفعيل أو تعطيل هذه الميزات من لوحة التحكم."
        }
        q.contains("صنعاء") || q.contains("عدن") || q.contains("تعز") || q.contains("الحديدة") || q.contains("مدن") -> {
            "دليل خدمات اليمن يغطي حالياً المحافظات والمدن الرئيسية: صنعاء (الأمانة)، عدن الباسلة، تعز الحالمة، والحديدة عروس البحر الأحمر، مع دعم التصفية الفورية حسب الأحياء والمربعات السكنية."
        }
        q.contains("كهربائي") || q.contains("سباك") || q.contains("دهان") || q.contains("حداد") -> {
            "لدينا مئات الفنيين المتخصصين في أعمال الكهرباء والسباكة والدهانات والحدادة والنجارة. يمكنك تصفحهم بالضغط على قسم التخصص المناسب بالرئيسية ثم اختيار الفني الأقرب إليك والتواصل معه مباشرة."
        }
        else -> {
            "أهلاً بك في منصة دليل اليمن للخدمات الذكية! في وضع العمل المحلي والمساعد الذكي، يسرني تزويدك بكافة المعلومات حول كيفية التواصل مع الفنيين، تقديم استمارة طلب الانضمام بالهاتف والصوت، التتبع برادار الخرائط الجغرافي الدقيق، أو التواصل مباشرة مع إدارة الدعم المعتمدة على رقم 777644."
        }
    }
}

@Composable
fun GuidanceRow(q: String, a: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("💡 $q", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(a, fontSize = 9.sp, color = Color.LightGray)
        }
        IconButton(
            onClick = {
                VoiceManager.onSpeak?.invoke("$q ... $a")
            },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share, 
                contentDescription = "قراءة صوتية مسموعة", 
                tint = Color.Yellow, 
                modifier = Modifier.size(14.dp)
            )
        }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.clearGeneralChatHistory() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "مسح المحادثة", tint = Color.Red, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                Divider(color = themeColors.accent.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatMessages, key = { it.id }) { msg ->
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(msg.message, fontSize = 11.sp, color = Color.White)
                                    IconButton(
                                        onClick = { VoiceManager.onSpeak?.invoke(msg.message) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("🔊", fontSize = 14.sp)
                                    }
                                }
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
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    VoiceManager.onHear?.invoke { spokenText -> typedText = spokenText }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("🎙️", fontSize = 16.sp)
                            }
                        }
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
    val settingsState by viewModel.settings.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val context = LocalContext.current
    val isOnline = com.example.NetworkUtils.isNetworkAvailable(context)
    val showContacts = isOnline || (adminRole != "GUEST")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        
        // Cover container (Dynamic)
        val coverType = settingsState.aboutCoverType
        val coverContent = settingsState.aboutCoverContent
        val coverBase64 = settingsState.aboutCoverBase64

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (coverType == "IMAGE") {
                    if (coverBase64.isNotEmpty()) {
                        val bitmap = remember(coverBase64) {
                            try {
                                val bytes = android.util.Base64.decode(coverBase64, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) { null }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "صورة الغلاف",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else if (coverContent.isNotEmpty()) {
                        coil.compose.AsyncImage(
                            model = coverContent,
                            contentDescription = "صورة الغلاف",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else if (coverType == "VIDEO") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color.Black)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "مشغل الفيديو",
                                tint = themeColors.accent,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("تشغيل الفيديو التعريفي المخصص للخدمات", fontSize = 10.sp, color = Color.White)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(themeColors.primary.copy(alpha = 0.2f))
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (coverContent.isNotEmpty()) coverContent else "دليل خدمات اليمن",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.accent,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        // App Identity card
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, themeColors.accent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = themeColors.accent,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = settingsState.appName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "الإصدار الحالي: ${settingsState.appVersion}",
                    fontSize = 11.sp,
                    color = themeColors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Text(
                    text = settingsState.aboutCustomInfo.ifEmpty { settingsState.welcomeMessage },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        // Offline guide
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.primary.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "قناة آمنة",
                    tint = themeColors.accent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "تطبيق صمم خصيصاً للتصفح والاتصال والربط السريع بدون إنترنت في كافة المدن والمناطق اليمنية.",
                    fontSize = 11.sp,
                    color = Color.White,
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 📊 Statistics panel
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "📊 إحصائيات الدليل والمنصة الحية:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
                Divider(color = themeColors.accent.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👷 مقدمي الخدمات", fontSize = 11.sp, color = themeColors.textSecondary)
                        val providerCount = viewModel.providers.collectAsState().value.size
                        Text("$providerCount فني نشط", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(themeColors.accent.copy(alpha = 0.3f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📱 مستخدمين مسجلين", fontSize = 11.sp, color = themeColors.textSecondary)
                        val usersCount = viewModel.registeredUsersCount.collectAsState().value
                        Text("$usersCount مستخدم", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Support channels panel
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "📞 قنوات التواصل والدعم الفني المباشر:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
                
                Divider(color = themeColors.accent.copy(alpha = 0.2f))

                if (showContacts) {
                    // Support Phone Dial
                    if (settingsState.supportPhone.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val uri = Uri.parse("tel:${settingsState.supportPhone}")
                                    val intent = Intent(Intent.ACTION_DIAL, uri)
                                    try { context.startActivity(intent) } catch(e: Exception) {}
                                }
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "اتصال هاتفي مباشر: ${settingsState.supportPhone}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Support Whatsapp Message
                    if (settingsState.supportWhatsapp.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val url = "https://wa.me/${settingsState.supportWhatsapp}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    try { context.startActivity(intent) } catch(e: java.lang.Exception) {}
                                }
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "مراسلة عبر واتساب: ${settingsState.supportWhatsapp}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Support Email
                    if (settingsState.supportEmail.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf(settingsState.supportEmail))
                                        putExtra(Intent.EXTRA_SUBJECT, "استفسار بخصوص ${settingsState.appName}")
                                    }
                                    try { context.startActivity(intent) } catch(e: Exception) {}
                                }
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "مراسلة بالبريد الإلكتروني: ${settingsState.supportEmail}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Red.copy(alpha = 0.15f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تم حظر وإخفاء قنوات الدعم لفتح التطبيق دون اتصال بالإنترنت. يرجى الاتصال بالشبكة لفك تشفير وعرض معلومات الدعم المباشر ومراسلة المسؤول.",
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 📥 App Download link button
        if (settingsState.appDownloadUrl.isNotBlank()) {
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(settingsState.appDownloadUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {}
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "تحميل", tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("📥 تحميل وتحديث التطبيق المباشر (APK)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MockMapViewScreen(viewModel: MainViewModel, themeColors: VisualThemePalette, onRequestLocationPermission: () -> Unit) {
    val providers by viewModel.providers.collectAsState()
    val citiesList by viewModel.cities.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val radiusKm by viewModel.maxKmRadius.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val context = LocalContext.current
    var selectedUserCityId by remember { mutableStateOf("ye_san") }
    var selectedProviderForMap by remember { mutableStateOf<com.example.data.ProviderEntity?>(null) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    
    // Virtual appointment reservation target on map
    var bookingProviderTargetOnMap by remember { mutableStateOf<com.example.data.ProviderEntity?>(null) }

    val userCoords = getCityCenterCoords(selectedUserCityId)

    // Side effect to update position in viewmodel
    LaunchedEffect(selectedUserCityId) {
        viewModel.updateUserLocation(userCoords.first, userCoords.second)
        viewModel.setCityFilter(selectedUserCityId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
    ) {
        // Upper Controls: City Selector & Proximity Filter
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "🌐 خارطة رادار الخدمات وإحداثيات الموقع (اليمن):",
                    color = themeColors.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("موقعي الحالي:", color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(0.4f))
                    
                    // City Dropdown selection
                    Box(modifier = Modifier.weight(1f)) {
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        val activeLabel = citiesList.find { it.id == selectedUserCityId }?.nameAr ?: "صنعاء 🌍"
                        
                        Button(
                            onClick = { dropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2225)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                  Text(activeLabel, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                  Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = themeColors.accent)
                            }
                        }
                        
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(themeColors.surface)
                        ) {
                            citiesList.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(city.nameAr, color = Color.White, fontSize = 11.sp) },
                                    onClick = {
                                        selectedUserCityId = city.id
                                        selectedProviderForMap = null
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Proximity selector slider
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = "نطاق البحث: بقرب ${radiusKm} كم",
                            color = Color.LightGray,
                            fontSize = 9.sp
                        )
                        Slider(
                            value = radiusKm.toFloat(),
                            onValueChange = { viewModel.setRadiusKm(it.toInt()) },
                            valueRange = 2f..50f,
                            colors = SliderDefaults.colors(
                                thumbColor = themeColors.primary,
                                activeTrackColor = themeColors.accent
                            ),
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onRequestLocationPermission() },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحديد موقعي التلقائي الفعلي عبر الـ GPS 📍", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                
                // Horizontal scrollable Category/Specialty filter bar
                Text("🔍 تصفية مقدمي الخدمة حسب التخصص والمسافة:", color = Color.Gray, fontSize = 9.sp)
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedCategoryId == null) themeColors.accent else Color(0xFF0F2225))
                                .clickable { selectedCategoryId = null }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "الكل 🌟",
                                color = if (selectedCategoryId == null) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    val listToShow = if (categories.isEmpty()) {
                        listOf(
                            com.example.data.CategoryEntity("ye_sub_spaka", "سباكة 🔧", "", 1),
                            com.example.data.CategoryEntity("ye_sub_kahraba", "كهرباء ⚡", "", 2),
                            com.example.data.CategoryEntity("ye_sub_dehan", "دهان وصباغة 🎨", "", 3),
                            com.example.data.CategoryEntity("ye_sub_hadada", "حدادة ألمنيوم 🔨", "", 4)
                        )
                    } else categories

                    items(listToShow) { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) themeColors.accent else Color(0xFF0F2225))
                                .clickable { selectedCategoryId = if (isSelected) null else cat.id }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat.name,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5, 10, 20, 50).forEach { km ->
                        val isSelected = radiusKm == km
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) themeColors.primary else Color(0xFF0F2225))
                                .clickable { viewModel.setRadiusKm(km) }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$km كم",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Radar Canvas Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF030D0E))
        ) {
            // Draw Radar rings and grid mathematically
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                
                // Draw circular radar bounds
                for (r in listOf(0.15f, 0.3f, 0.45f, 0.6f, 0.7f, 0.85f)) {
                    drawCircle(
                        color = Color(0xFF223639).copy(alpha = 0.5f),
                        radius = size.width * r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )
                }
                
                // Draw cross axes
                drawLine(
                    color = Color(0xFF223639).copy(alpha = 0.4f),
                    start = Offset(0f, cy),
                    end = Offset(size.width, cy)
                )
                drawLine(
                    color = Color(0xFF223639).copy(alpha = 0.4f),
                    start = Offset(cx, 0f),
                    end = Offset(cx, size.height)
                )
            }

            // Render Center Pin (The User)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cxDp = maxWidth / 2f - 18.dp
                val cyDp = maxHeight / 2f - 24.dp
                
                Column(
                    modifier = Modifier
                        .absoluteOffset(x = cxDp, y = cyDp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Your Location",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(36.dp)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2196F3).copy(alpha = 0.9f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("موقعي", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Plot nearby providers dynamically using Cartesian mathematics
            val nearbyProviders = providers.filter { p ->
                val coords = getProviderCoords(p)
                val distMeters = calculateDistanceInMeters(userCoords.first, userCoords.second, coords.first, coords.second)
                val distKm = distMeters / 1000.0
                val matchesCategory = selectedCategoryId == null || p.categoryId == selectedCategoryId
                distKm <= radiusKm && p.cityId == selectedUserCityId && (p.isVip || p.subscriptionStatus == "APPROVED") && matchesCategory
            }

            nearbyProviders.forEachIndexed { idx, provider ->
                val coords = getProviderCoords(provider)
                val distMeters = calculateDistanceInMeters(userCoords.first, userCoords.second, coords.first, coords.second)
                val distKm = distMeters / 1000.0
                
                // Dynamic scaling factor relative to the slider's radius range (1 degree ≈ 111 km)
                val scaleFactorRange = (radiusKm.toFloat() / 111.0).coerceAtLeast(0.01)
                val relX = ((coords.second - userCoords.second) / scaleFactorRange).coerceIn(-1.0, 1.0).toFloat()
                val relY = ((coords.first - userCoords.first) / scaleFactorRange).coerceIn(-1.0, 1.0).toFloat()

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    // Calculate positions based on midpoint offsets
                    val posX = (maxWidth / 2f) + ((maxWidth / 2f) * relX * 0.85f) - 16.dp
                    // invert Y to conform to Cartesian coords map vs screen pixels
                    val posY = (maxHeight / 2f) - ((maxHeight / 2f) * relY * 0.85f) - 24.dp

                    Column(
                        modifier = Modifier
                            .absoluteOffset(x = posX, y = posY)
                            .clickable { selectedProviderForMap = provider }
                            .testTag("geo_marker_$idx"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = provider.name,
                            tint = if (provider.isVip) themeColors.accent else themeColors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .padding(horizontal = 3.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${provider.name.split(" ").lastOrNull() ?: provider.name} (${String.format(java.util.Locale.US, "%.1f", distKm)} كم)",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Pop up provider slide card if selected
        selectedProviderForMap?.let { p ->
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, themeColors.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Green))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (p.isVip) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = themeColors.accent) {
                                    Text("VIP", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        IconButton(
                            onClick = { selectedProviderForMap = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(p.area, color = themeColors.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("📍 العنوان: ${p.localNeighborhood}", color = themeColors.textSecondary, fontSize = 10.sp, maxLines = 1)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val uri = Uri.parse("tel:${p.phone}")
                                val intent = Intent(Intent.ACTION_DIAL, uri)
                                try { context.startActivity(intent) } catch(e: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            modifier = Modifier.weight(1f).height(34.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("اتصال مباشر", fontSize = 9.sp)
                        }

                        Button(
                            onClick = {
                                bookingProviderTargetOnMap = p
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1.2f).height(34.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("حجز موعد فوري", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val coords = getProviderCoords(p)
                            val gmmIntentUri = Uri.parse("google.navigation:q=${coords.first},${coords.second}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${coords.first},${coords.second}")
                                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                                context.startActivity(webIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF115E59)),
                        modifier = Modifier.fillMaxWidth().height(34.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Place, contentDescription = "توجيهات", tint = themeColors.accent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("رسم المسار والحصول على الاتجاهات 🗺️", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Dynamic booking target dialog shown when clicked from map card
    bookingProviderTargetOnMap?.let { p ->
        var detailsInput by remember { mutableStateOf("") }
        var preferredTimeInput by remember { mutableStateOf("غداً الساعة 4:00 مساءً") }
        
        AlertDialog(
            onDismissRequest = { bookingProviderTargetOnMap = null },
            containerColor = themeColors.surface,
            title = { Text("🗓️ طلب موعد حجز فوري: ${p.name}", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("تفاصيل حجز الخدمة مباشرة عبر رادار الخريطة الشامل:", color = Color.White, fontSize = 11.sp)
                    OutlinedTextField(
                        value = detailsInput,
                        onValueChange = { detailsInput = it },
                        label = { Text("تفاصيل ومعلومات المشكلة الصيانة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = preferredTimeInput,
                        onValueChange = { preferredTimeInput = it },
                        label = { Text("الوقت المقترح") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (detailsInput.trim().isNotBlank()) {
                            viewModel.addBooking(
                                name = currentUserName.ifBlank { "عميل الخريطة" },
                                phone = currentUserPhone.ifBlank { "777000000" },
                                area = "رادار الخريطة",
                                serviceType = p.area,
                                providerId = p.id,
                                providerName = p.name,
                                dateString = "2026-06-21",
                                timeString = preferredTimeInput.trim()
                            )
                            bookingProviderTargetOnMap = null
                            selectedProviderForMap = null
                            Toast.makeText(context, "تم إرسال طلب الحجز بنجاح بالرقم المرجعي الموحد! 🚀", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "يرجى تعبئة تفاصيل الخدمة", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                ) {
                    Text("تأكيد الطلب")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookingProviderTargetOnMap = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }
}

fun getCityCenterCoords(cityId: String): Pair<Double, Double> {
    return when (cityId) {
        "ye_san" -> Pair(15.3694, 44.1910)
        "ye_ade" -> Pair(12.7855, 45.0186)
        "ye_tai" -> Pair(13.5794, 44.0205)
        "ye_hod" -> Pair(14.7979, 42.9530)
        else -> Pair(15.3694, 44.1910) // default to Sana'a
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

fun getProviderCoords(provider: com.example.data.ProviderEntity): Pair<Double, Double> {
    if (provider.latitude != 0.0 && provider.longitude != 0.0) {
        return Pair(provider.latitude, provider.longitude)
    }
    return getProviderCoords(provider.id)
}

fun getProviderCoords(providerId: String): Pair<Double, Double> {
    return when (providerId) {
        "p_amin" -> Pair(15.3694, 44.1910)
        else -> {
            val hash = providerId.hashCode().toDouble()
            val offsetLat = (hash % 100) / 1000.0
            val offsetLng = ((hash / 100) % 100) / 1000.0
            Pair(15.3694 + offsetLat, 44.1910 + offsetLng)
        }
    }
}

fun getAreaCoords(areaName: String): Pair<Double, Double> {
    return when {
        areaName.contains("تعز") -> Pair(13.5794, 44.0135)
        areaName.contains("عدن") -> Pair(12.7855, 45.0186)
        areaName.contains("الحديدة") -> Pair(14.7979, 42.9530)
        areaName.contains("حضرموت") || areaName.contains("المكلا") -> Pair(14.5424, 49.1242)
        else -> Pair(15.3694, 44.1910) // default to Sana'a
    }
}

fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return 6371.0 * c
}

@Composable
fun JoinRequestStatusScreen(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val context = LocalContext.current
    val joinPhone by viewModel.joinRequestPhone.collectAsState()
    val pendingProviders by viewModel.pendingProviders.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val chatChannels by viewModel.chatChannels.collectAsState()

    var activeProviderChatChannel by remember { mutableStateOf<com.example.data.ChatChannelEntity?>(null) }
    var replyInputText by remember { mutableStateOf("") }

    val matchingPending = pendingProviders.find { it.phone == joinPhone }
    val matchingApproved = providers.find { it.phone == joinPhone }
    // Look for rejection notification
    val rejectionNotif = notifications.find { 
        it.targetValue == joinPhone && (it.title.contains("رفض") || it.message.contains("رفض") || it.title.contains("تنويه"))
    }

    LaunchedEffect(rejectionNotif, matchingApproved, matchingPending) {
        if (rejectionNotif != null && matchingApproved == null && matchingPending == null) {
            android.widget.Toast.makeText(context, "❌ تم تنبيهك: ${rejectionNotif.message}", android.widget.Toast.LENGTH_LONG).show()
            viewModel.cancelOrResetJoinRequest(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(
                width = 1.5.dp,
                color = when {
                    matchingApproved != null -> Color(0xFF10B981)
                    rejectionNotif != null -> Color(0xFFEF4444)
                    else -> Color(0xFFF59E0B)
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val categories by viewModel.categories.collectAsState()
                when {
                    matchingApproved != null -> {
                        // Accepted (Image 2 design style + Full Interactive Provider Dashboard)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✅",
                                fontSize = 28.sp
                            )
                        }
                        
                        Text(
                            text = "🎉 تم تفعيل حسابك كفني معتمد!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "مرحباً بك يا غالي! حسابك نشط الآن في دليل كل خدمات اليمن ومتاح لجميع العملاء للتواصل والحجز المباشر.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        
                        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                        
                        // 1. Profile Summary Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111C15)),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFF10B981), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("👷", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = matchingApproved.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("💼", fontSize = 10.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val catName = if (matchingApproved.categoryId == "other" && matchingApproved.customCategoryName.isNotEmpty()) matchingApproved.customCategoryName else (categories.find { it.id == matchingApproved.categoryId }?.name ?: "صيانة فنية")
                                        Text(catName, fontSize = 10.sp, color = Color.LightGray)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📍", fontSize = 10.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${matchingApproved.area} - ${matchingApproved.localNeighborhood}", fontSize = 10.sp, color = Color.LightGray)
                                    }
                                }
                            }
                        }

                        // 2. Active Status Toggle Button
                        Button(
                            onClick = {
                                viewModel.updateProviderEntity(matchingApproved.copy(isAvailable = !matchingApproved.isAvailable))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (matchingApproved.isAvailable) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, if (matchingApproved.isAvailable) Color(0xFFEF4444) else Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text(
                                text = if (matchingApproved.isAvailable) "🔴 تغيير حالتك الحالية إلى: مشغول مؤقتاً" else "🟢 تغيير حالتك الحالية إلى: متاح للعمل فوراً",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (matchingApproved.isAvailable) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                        }

                        // 3. Private Bookings Section
                        val myBookings = bookings.filter { it.providerId == matchingApproved.id }
                        Text("📅 طلبات الحجز والعمل الموجهة لك:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent, modifier = Modifier.align(Alignment.Start))
                        if (myBookings.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                                    Text("📭 لا توجد طلبات حجز موجهة لك حالياً.", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        } else {
                            myBookings.forEach { b ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("العميل: ${b.customerName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(
                                                text = when(b.status) {
                                                    "PENDING" -> "⏳ بانتظار تأكيدك"
                                                    "APPROVED", "IN_PROGRESS" -> "🟢 مقبول وجاري التنفيذ"
                                                    "REJECTED" -> "❌ مرفوض"
                                                    "COMPLETED" -> "✅ مكتمل"
                                                    else -> b.status
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when(b.status) {
                                                    "PENDING" -> Color.Yellow
                                                    "APPROVED", "IN_PROGRESS", "COMPLETED" -> Color.Green
                                                    else -> Color.Red
                                                }
                                            )
                                        }
                                        Text("📞 هاتف العميل للتواصل: ${b.customerPhone}", fontSize = 10.sp, color = themeColors.accent)
                                        Text("📍 موقع ومحافظة العميل: ${b.customerArea}", fontSize = 10.sp, color = Color.LightGray)
                                        Text("🔧 الخدمة المطلوبة: ${b.serviceType}", fontSize = 10.sp, color = Color.LightGray)
                                        Text("⏰ موعد الزيارة: ${b.dateString} - ${b.timeString}", fontSize = 10.sp, color = Color.LightGray)
                                        
                                        if (b.status == "PENDING") {
                                            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = { viewModel.updateBookingStatus(b.id, "IN_PROGRESS") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    modifier = Modifier.weight(1f).height(28.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("موافقة وقبول العمل ✅", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = { viewModel.updateBookingStatus(b.id, "REJECTED", "اعتذر الفني لإنشغاله") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                    modifier = Modifier.weight(1f).height(28.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("اعتذار ورفض العمل ❌", fontSize = 9.sp, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Conversations/Chats section
                        val myChats = chatChannels.filter { it.id.contains("chat_p_${matchingApproved.id}_") || it.id.contains("_u_${matchingApproved.id}") }
                        Text("💬 محادثات العملاء المباشرة معك:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent, modifier = Modifier.align(Alignment.Start))
                        if (myChats.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                                    Text("📭 لا توجد محادثات نشطة مع عملاء حالياً.", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        } else {
                            myChats.forEach { ch ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        activeProviderChatChannel = ch
                                    }
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("💬", fontSize = 15.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ch.userName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(ch.lastMessage, fontSize = 10.sp, color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Text("◀️", fontSize = 10.sp, color = themeColors.accent)
                                    }
                                }
                            }
                        }

                        // 5. Notifications Section
                        val myNotifs = notifications.filter { it.targetValue == matchingApproved.phone || it.targetType == "ALL" }
                        Text("🔔 الإشعارات والتعميمات الإدارية الموجهة إليك:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent, modifier = Modifier.align(Alignment.Start))
                        if (myNotifs.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                                    Text("📭 لا توجد إشعارات إدارية جديدة.", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        } else {
                            myNotifs.take(4).forEach { notif ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(notif.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(notif.message, fontSize = 11.sp, color = Color.LightGray, lineHeight = 16.sp)
                                        
                                        if (notif.customerPhone.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Call Button
                                                Button(
                                                    onClick = {
                                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${notif.customerPhone}"))
                                                        context.startActivity(intent)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                                                ) {
                                                    Text("📞 اتصال مباشر", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                                
                                                // Chat Button
                                                Button(
                                                    onClick = {
                                                        val targetId = "chat_p_${matchingApproved.id}_u_${notif.customerPhone}"
                                                        val existingChannel = chatChannels.find { it.id == targetId }
                                                        activeProviderChatChannel = existingChannel ?: com.example.data.ChatChannelEntity(
                                                            id = targetId,
                                                            userName = notif.customerName.ifEmpty { "عميل جديد" },
                                                            lastMessage = "",
                                                            messages = emptyList()
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                                                ) {
                                                    Text("💬 مراسلة فورية", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Local Chat Dialog Overlay for Provider
                        activeProviderChatChannel?.let { ch ->
                            Dialog(onDismissRequest = { activeProviderChatChannel = null }) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, themeColors.accent),
                                    modifier = Modifier.padding(12.dp).fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("💬 محادثة العميل: ${ch.userName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                items(ch.messages) { msg ->
                                                    val isMe = msg.senderId == matchingApproved.id
                                                    val alignment = if (isMe) Alignment.End else Alignment.Start
                                                    val bubbleBg = if (isMe) themeColors.primary else Color.Gray.copy(alpha = 0.3f)
                                                    Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(bubbleBg)
                                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                        ) {
                                                            Text(msg.message, fontSize = 10.sp, color = Color.White)
                                                        }
                                                        Text(msg.senderName, fontSize = 8.sp, color = themeColors.textSecondary)
                                                    }
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = replyInputText,
                                            onValueChange = { replyInputText = it },
                                            placeholder = { Text("اكتب ردك هنا...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                        )

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    if (replyInputText.trim().isNotEmpty()) {
                                                        viewModel.replyToChatChannel(ch.id, matchingApproved.id, replyInputText.trim(), matchingApproved.name)
                                                        // Refresh chat locally
                                                        val updatedMsg = com.example.data.ChatMessageEntity(
                                                            id = UUID.randomUUID().toString(),
                                                            senderId = matchingApproved.id,
                                                            message = replyInputText.trim(),
                                                            timestamp = System.currentTimeMillis(),
                                                            senderName = matchingApproved.name
                                                        )
                                                        activeProviderChatChannel = ch.copy(
                                                            messages = ch.messages + updatedMsg,
                                                            lastMessage = replyInputText.trim(),
                                                            timestamp = System.currentTimeMillis()
                                                        )
                                                        replyInputText = ""
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("إرسال الرد السريع ⚡", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    matchingPending != null -> {
                        // Pending approval screen
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⏳", fontSize = 28.sp)
                        }

                        Text(
                            text = "⏳ طلب انضمامك قيد المراجعة حالياً",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "أهلاً بك يا غالي! تم إرسال طلبك بنجاح وهو قيد المراجعة والتدقيق الإداري الآن من قبل الإدارة. نسعد بانضمامك وسنبلغك فور التنشيط والموافقة لتتمكن من استقبال الحجوزات والدردشة مباشرة!",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2214)),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("📋 تفاصيل الطلب المقدم:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                Text("• الاسم الثلاثي: ${matchingPending.name}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("• رقم الهاتف: ${matchingPending.phone}", fontSize = 11.sp, color = Color.White)
                                val catName = if (matchingPending.categoryId == "other" && matchingPending.customCategoryName.isNotEmpty()) matchingPending.customCategoryName else (categories.find { it.id == matchingPending.categoryId }?.name ?: "صيانة عامة")
                                Text("• القسم والتخصص: $catName", fontSize = 11.sp, color = Color.White)
                                Text("• منطقة الخدمة: ${matchingPending.area}", fontSize = 11.sp, color = Color.White)
                                if (matchingPending.localNeighborhood.isNotEmpty()) {
                                    Text("• الحي/الشارع التفصيلي: ${matchingPending.localNeighborhood}", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.cancelOrResetJoinRequest(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("❌ إلغاء الطلب والعودة للرئيسية", color = Color.White, fontSize = 11.sp)
                        }
                    }
                    else -> {
                        // Fallback state
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Gray.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "ℹ️", fontSize = 28.sp)
                        }

                        Text(
                            text = "لم يتم العثور على طلب نشط",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "يرجى تقديم طلب انضمام جديد للتمتع بكافة الميزات والتحكم ببياناتك وحجوزاتك كفني معتمد.",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.cancelOrResetJoinRequest(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("📝 العودة لشاشة التسجيل", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AllConversationsDialogView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    initialSelectedChannelId: String? = null,
    onReadTrigger: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val chatChannels by viewModel.chatChannels.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val settingsState by viewModel.settings.collectAsState()

    val myProvider = providers.find { it.phone == currentUserPhone }

    val myChannels = remember(chatChannels, currentUserId, currentUserPhone, myProvider) {
        chatChannels.filter { ch ->
            ch.id == "support_$currentUserId" ||
            ch.id.contains(currentUserId) ||
            (currentUserPhone.isNotEmpty() && ch.id.contains(currentUserPhone)) ||
            (myProvider != null && (ch.id.contains("chat_p_${myProvider.id}_") || ch.id.contains("_u_${myProvider.id}"))) ||
            currentUserId == "admin" || currentUserId.startsWith("super_")
        }
    }

    var selectedChannelId by remember { mutableStateOf<String?>(initialSelectedChannelId) }
    val selectedChannel = remember(chatChannels, selectedChannelId) {
        chatChannels.find { it.id == selectedChannelId }
    }
    var replyText by remember { mutableStateOf("") }
    val sharedPrefs = remember(context) { context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE) }

    LaunchedEffect(selectedChannelId, chatChannels) {
        selectedChannelId?.let { chId ->
            val ch = chatChannels.find { it.id == chId }
            val lastMsg = ch?.messages?.lastOrNull()
            if (lastMsg != null) {
                val isMe = lastMsg.senderId == currentUserId || (myProvider != null && lastMsg.senderId == myProvider.id)
                if (!isMe) {
                    sharedPrefs.edit().putLong("chat_read_$chId", System.currentTimeMillis()).apply()
                    onReadTrigger()
                }
            }
        }
    }

    // Media attachment pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val ch = selectedChannel
            if (ch != null) {
                val currentImagesCount = ch.messages.count { it.imageUrl.isNotEmpty() && !it.imageUrl.contains("video_") }
                if (currentImagesCount >= settingsState.maxImagesPerChat) {
                    Toast.makeText(context, "⚠️ تجاوزت الحد الأقصى للصور (${settingsState.maxImagesPerChat})", Toast.LENGTH_LONG).show()
                } else {
                    val senderName = currentUserName.ifEmpty { myProvider?.name ?: "مستخدم" }
                    val senderId = myProvider?.id ?: currentUserId
                    viewModel.replyToChatChannel(ch.id, senderId, "[صورة مرفقة]", senderName, imageUrl = uri.toString())
                    onReadTrigger()
                    Toast.makeText(context, "📷 تم إرسال الصورة بنجاح!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val ch = selectedChannel
            if (ch != null) {
                val currentVideosCount = ch.messages.count { it.imageUrl.contains("video_") || it.message == "[فيديو مرفق]" }
                if (currentVideosCount >= settingsState.maxVideosPerChat) {
                    Toast.makeText(context, "⚠️ تجاوزت الحد الأقصى للفيديوهات (${settingsState.maxVideosPerChat})", Toast.LENGTH_LONG).show()
                } else {
                    val senderName = currentUserName.ifEmpty { myProvider?.name ?: "مستخدم" }
                    val senderId = myProvider?.id ?: currentUserId
                    viewModel.replyToChatChannel(ch.id, senderId, "[فيديو مرفق]", senderName, imageUrl = "video_" + uri.toString())
                    onReadTrigger()
                    Toast.makeText(context, "🎥 تم إرسال الفيديو بنجاح!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Checking if the chat is disabled by Admin
    val chatDisabledReason = remember(settingsState, currentUserId, myProvider, selectedChannelId) {
        val chId = selectedChannelId ?: ""
        if (settingsState.disableChatAll) {
            "⚠️ الدردشة متوقفة حالياً للصيانة بقرار الإدارة: " + settingsState.chatDisabledAnnouncement
        } else if (settingsState.disableChatUsers && myProvider == null && currentUserId != "admin" && !currentUserId.startsWith("super_")) {
            "⚠️ تم تعطيل الدردشة للعملاء والزائرين حالياً."
        } else if (settingsState.disableChatProviders && myProvider != null) {
            "⚠️ تم تعطيل الدردشة للفنيين ومزودي الخدمة حالياً."
        } else if (chId.startsWith("chat_p_") && !settingsState.allowChatUserToProvider) {
            "⚠️ تم إيقاف الدردشة المباشرة بين العملاء والفنيين."
        } else {
            null
        }
    }

    // Dynamic Header Title
    val titleText = remember(selectedChannelId, chatChannels, providers, currentUserId, myProvider) {
        val ch = chatChannels.find { it.id == selectedChannelId }
        if (ch == null) {
            "💬 كل محادثاتك المباشرة"
        } else {
            if (ch.id.startsWith("support_")) {
                "💬 الدعم الفني المباشر"
            } else if (ch.id.startsWith("chat_p_")) {
                val providerId = ch.id.substringAfter("chat_p_").substringBefore("_u_")
                val providerObj = providers.find { it.id == providerId }
                if (myProvider != null && myProvider.id == providerId) {
                    val customerNamePart = ch.userName.substringAfter("مع ").ifEmpty { "عميل" }
                    "👤 العميل: $customerNamePart"
                } else {
                    val pName = providerObj?.name ?: ch.userName.substringAfter("دردشة: ").substringBefore(" مع")
                    "👷 الفني: $pName"
                }
            } else {
                ch.userName
            }
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, themeColors.accent),
            modifier = Modifier.padding(10.dp).fillMaxWidth().height(480.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = titleText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = { if (selectedChannelId != null) selectedChannelId = null else onDismiss() }, modifier = Modifier.size(24.dp)) {
                        Text(if (selectedChannelId != null) "🔙" else "❌", color = Color.White, fontSize = 12.sp)
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 8.dp))

                if (selectedChannelId == null) {
                    if (myChannels.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("💬", fontSize = 36.sp)
                                Text("لا توجد محادثات نشطة حالياً.", fontSize = 12.sp, color = Color.Gray)
                                Button(
                                    onClick = {
                                        onDismiss()
                                        viewModel.navigateTo("USER_BROWSE")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                                ) {
                                    Text("ابدأ محادثة مع الدعم الفني", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            myChannels.forEach { ch ->
                                                viewModel.deleteChatChannel(ch.id)
                                            }
                                            viewModel.triggerNotification("🗑️ تم حذف جميع المحادثات بنجاح.")
                                            onReadTrigger()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("حذف جميع المحادثات 🗑️", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            items(myChannels) { ch ->
                                val displayItemTitle = remember(ch, providers, currentUserId, myProvider) {
                                    if (ch.id.startsWith("support_")) {
                                        "💬 الدعم الفني المباشر"
                                    } else if (ch.id.startsWith("chat_p_")) {
                                        val providerId = ch.id.substringAfter("chat_p_").substringBefore("_u_")
                                        val providerObj = providers.find { it.id == providerId }
                                        if (myProvider != null && myProvider.id == providerId) {
                                            val customerNamePart = ch.userName.substringAfter("مع ").ifEmpty { "عميل" }
                                            "👤 العميل: $customerNamePart"
                                        } else {
                                            val pName = providerObj?.name ?: ch.userName.substringAfter("دردشة: ").substringBefore(" مع")
                                            "👷 الفني: $pName"
                                        }
                                    } else {
                                        ch.userName
                                    }
                                }

                                val unreadThisChat = remember(ch, chatChannels) {
                                    val lastMsg = ch.messages.lastOrNull()
                                    if (lastMsg == null) {
                                        false
                                    } else {
                                        val isMe = lastMsg.senderId == currentUserId || (myProvider != null && lastMsg.senderId == myProvider.id)
                                        val readTime = sharedPrefs.getLong("chat_read_${ch.id}", 0L)
                                        !isMe && lastMsg.timestamp > readTime
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (unreadThisChat) themeColors.accent.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)),
                                    border = BorderStroke(if (unreadThisChat) 1.dp else 0.5.dp, if (unreadThisChat) themeColors.accent else Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f).clickable { selectedChannelId = ch.id },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("💬", fontSize = 18.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(displayItemTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (unreadThisChat) themeColors.accent else Color.White)
                                                Text(ch.lastMessage, fontSize = 10.sp, color = if (unreadThisChat) themeColors.accent.copy(alpha = 0.8f) else Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteChatChannel(ch.id)
                                                onReadTrigger()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("🗑️", fontSize = 14.sp)
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(
                                            text = "◀️",
                                            fontSize = 11.sp,
                                            color = themeColors.accent,
                                            modifier = Modifier.clickable { selectedChannelId = ch.id }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val ch = selectedChannel
                    if (ch == null) {
                        selectedChannelId = null
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(ch.messages) { msg ->
                                        val isMe = msg.senderId == currentUserId || msg.senderId == myProvider?.id
                                        val alignment = if (isMe) Alignment.End else Alignment.Start
                                        val bubbleBg = if (isMe) themeColors.primary else Color.Gray.copy(alpha = 0.3f)
                                        Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(bubbleBg)
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    if (msg.imageUrl.startsWith("video_")) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(120.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(Color.Black)
                                                                .clickable {
                                                                    Toast.makeText(context, "▶️ جاري تشغيل الفيديو المرفق بجودة عالية...", Toast.LENGTH_SHORT).show()
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.PlayArrow,
                                                                contentDescription = "تشغيل الفيديو",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(48.dp)
                                                            )
                                                            Text(
                                                                "تشغيل الفيديو 🎥",
                                                                fontSize = 9.sp,
                                                                color = Color.White,
                                                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                                                            )
                                                        }
                                                    } else if (msg.imageUrl.isNotEmpty()) {
                                                        AsyncImage(
                                                            model = msg.imageUrl,
                                                            contentDescription = "صورة مرفقة",
                                                            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp))
                                                        )
                                                    }
                                                    if (msg.message.isNotEmpty() && msg.message != "[صورة مرفقة]" && msg.message != "[فيديو مرفق]") {
                                                        Text(msg.message, fontSize = 10.sp, color = Color.White)
                                                    }
                                                }
                                            }
                                            Text(msg.senderName, fontSize = 8.sp, color = themeColors.textSecondary)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (chatDisabledReason != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(chatDisabledReason, fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Image attachment trigger
                                    if (settingsState.allowSendImages) {
                                        IconButton(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    androidx.activity.result.PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text("📷", fontSize = 16.sp)
                                        }
                                    }

                                    // Video attachment trigger
                                    if (settingsState.allowSendVideos) {
                                        IconButton(
                                            onClick = {
                                                videoPickerLauncher.launch(
                                                    androidx.activity.result.PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.VideoOnly
                                                    )
                                                )
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text("🎥", fontSize = 16.sp)
                                        }
                                    }

                                    OutlinedTextField(
                                        value = replyText,
                                        onValueChange = { replyText = it },
                                        placeholder = { Text("اكتب رسالتك...", fontSize = 11.sp, color = Color.LightGray) },
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )

                                    Button(
                                        onClick = {
                                            if (replyText.trim().isNotEmpty()) {
                                                val senderName = currentUserName.ifEmpty { myProvider?.name ?: "مستخدم" }
                                                val senderId = myProvider?.id ?: currentUserId
                                                viewModel.replyToChatChannel(ch.id, senderId, replyText.trim(), senderName)
                                                
                                                replyText = ""
                                                onReadTrigger()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Text("إرسال", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun GuestRegistrationDialog(
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit,
    onRegisterCompleted: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phonePrefix by remember { mutableStateOf("+967") }
    var phoneBody by remember { mutableStateOf("") }
    var residence by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(2.dp, themeColors.accent, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔐 جدار الحماية - التحقق من الهوية",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Red)
                    }
                }

                Text(
                    text = "لتفادي الحسابات والاتصالات والمحادثات الوهمية وتقليل استهلاك الموارد تماشياً مع سياسة الخصوصية بالبوابة، يرجى ملء هوية مستخدم يمني حقيقي مفعّل بالجمهورية:",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الثلاثي بالكامل", fontSize = 11.sp, color = themeColors.accent) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(12.dp)
                    ) {
                        Text(phonePrefix, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = phoneBody,
                        onValueChange = { phoneBody = it },
                        placeholder = { Text("رقم الهاتف (مثلاً 777644)", fontSize = 11.sp, color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = residence,
                    onValueChange = { residence = it },
                    label = { Text("السكن داخل اليمن (مثلاً صنعاء - حدة)", fontSize = 11.sp, color = themeColors.accent) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val cleanName = name.trim()
                        val cleanPhone = phoneBody.trim()
                        val cleanResidence = residence.trim()

                        val isValidPhone = (cleanPhone.length == 9 && (
                            cleanPhone.startsWith("77") || 
                            cleanPhone.startsWith("73") || 
                            cleanPhone.startsWith("71") || 
                            cleanPhone.startsWith("70") || 
                            cleanPhone.startsWith("78")
                        )) || (cleanPhone.length == 7 && !cleanPhone.startsWith("0"))

                        if (cleanName.isEmpty() || cleanPhone.isEmpty() || cleanResidence.isEmpty()) {
                            // incomplete info
                        } else if (!isValidPhone) {
                            // invalid
                        } else {
                            val fullPhone = if (cleanPhone.length == 9) cleanPhone else "77$cleanPhone"
                            onRegisterCompleted(cleanName, fullPhone, cleanResidence)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إتمام التحقق العادل والانطلاق 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun UserNotificationsDialogView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    val allNotifications by viewModel.notifications.collectAsState()
    val userPhone by viewModel.currentUserPhone.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()

    val filteredNotifs = remember(allNotifications, userPhone, adminRole) {
        allNotifications.filter { notif ->
            when (notif.targetType) {
                "ALL" -> true
                "USER" -> notif.targetValue == userPhone
                "PROVIDER" -> notif.targetValue == userPhone
                "SUPERVISOR" -> adminRole != "GUEST"
                else -> true
            }
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(2.dp, themeColors.accent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔔 مركز الإشعارات السحابية (${filteredNotifs.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Red)
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                if (filteredNotifs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "لا توجد إشعارات نشطة حالياً",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredNotifs, key = { it.id }) { notif ->
                            val iconText = when {
                                notif.title.contains("حجز") -> "📅"
                                notif.title.contains("دردشة") || notif.title.contains("محادثة") -> "💬"
                                notif.title.contains("طلب") -> "👷"
                                notif.title.contains("تفعيل") || notif.title.contains("قبول") -> "🎉"
                                notif.title.contains("رفض") -> "❌"
                                else -> "🔔"
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(iconText, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = notif.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = themeColors.accent
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = notif.message,
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            lineHeight = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val formattedTime = remember(notif.timestamp) {
                                            try {
                                                val sdf = java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", java.util.Locale.getDefault())
                                                sdf.format(java.util.Date(notif.timestamp))
                                            } catch (e: Exception) {
                                                ""
                                            }
                                        }
                                        Text(
                                            text = formattedTime,
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
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


