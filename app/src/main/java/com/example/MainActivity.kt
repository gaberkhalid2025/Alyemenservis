package com.example

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.data.models.*
import com.example.ui.MainViewModel
import com.example.ui.navigation.AppNavigator
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.home.*
import com.example.ui.screens.map.*
import com.example.ui.screens.bookings.*
import com.example.ui.screens.admin.*
import com.example.ui.screens.assistant.*
import com.example.ui.screens.register.*
import com.example.ui.screens.status.*
import com.example.ui.screens.about.*
import com.example.utils.*
import com.example.utils.*
import com.example.viewmodels.*

class MainActivity : ComponentActivity() {
    private var lastBackPressTime = 0L
    private var tts: android.speech.tts.TextToSpeech? = null
    
    private var voiceResultCallback: ((String) -> Unit)? = null
    private val voiceRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
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
            if (androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO)) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("🎙️ إذن الميكروفون")
                    .setMessage("نحتاج إلى إذن الميكروفون لتمكين ميزة البحث الصوتي والأوامر الصوتية الذكية مع المساعد الذكي لمساعدتك بسرعة وسهولة.")
                    .setPositiveButton("منح الإذن 👍") { _, _ ->
                        androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 101)
                    }
                    .setNegativeButton("إلغاء ❌") { dialog, _ ->
                        dialog.dismiss()
                        Toast.makeText(this, "تم إلغاء منح إذن الصوت. يمكنك البحث بالكتابة بدلاً من ذلك.", Toast.LENGTH_SHORT).show()
                    }
                    .create()
                    .show()
            } else {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 101)
                Toast.makeText(this, "⚠️ فضلاً وافق على إذن استخدام المايك في النافذة المنبثقة لخدمتك بالكامل", Toast.LENGTH_LONG).show()
            }
            return
        }

        runOnUiThread {
            voiceResultCallback = onResult
            val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ar-YE")
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-YE")
                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "تحدث الآن باللغة العربية للبحث أو التحدث...")
            }
            try {
                voiceRecognitionLauncher.launch(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    val recognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this@MainActivity)
                    recognizer.setRecognitionListener(object : android.speech.RecognitionListener {
                        override fun onReadyForSpeech(p0: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(p0: Float) {}
                        override fun onBufferReceived(p0: ByteArray?) {}
                        override fun onEndOfSpeech() {}
                        override fun onError(error: Int) {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "لم يتم التعرف على الصوت، يرجى المحاولة مجدداً بصوت مسموع", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onResults(results: Bundle?) {
                            val matchesList = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matchesList.isNullOrEmpty()) {
                                runOnUiThread {
                                    onResult(matchesList[0])
                                }
                            }
                        }
                        override fun onPartialResults(p0: Bundle?) {}
                        override fun onEvent(p0: Int, p1: Bundle?) {}
                    })
                    recognizer.startListening(intent)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    Toast.makeText(this@MainActivity, "عذراً، نظام هاتفك لا يدعم التعرف الصوتي المباشر.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startFusedLocationUpdates(activity: ComponentActivity, viewModel: MainViewModel) {
        try {
            val context = activity.applicationContext
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        viewModel.updateUserLocation(location.latitude, location.longitude)
                    }
                }
                
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 5000L
                ).setMinUpdateIntervalMillis(3000L).build()
                
                val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        for (loc in result.locations) {
                            viewModel.updateUserLocation(loc.latitude, loc.longitude)
                        }
                    }
                }
                
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    android.os.Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
            com.example.util.SecurityManager.verifyAppSignature(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
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
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
            try {
                val firebaseAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
                if (BuildConfig.DEBUG) {
                    firebaseAppCheck.installAppCheckProviderFactory(
                        com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
                    )
                } else {
                    firebaseAppCheck.installAppCheckProviderFactory(
                        com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
                    )
                }
            } catch (appCheckEx: Exception) {
                appCheckEx.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val docRef = firestore.collection("settings").document("main_settings")
            docRef.get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    val initSettings = com.example.data.AdminSettingsEntity(
                        isStoresEnabled = true,
                        isPropertiesEnabled = true
                    )
                    docRef.set(initSettings)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        this,
                        "android.permission.POST_NOTIFICATIONS"
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    if (androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.POST_NOTIFICATIONS")) {
                        android.app.AlertDialog.Builder(this)
                            .setTitle("🔔 إذن الإشعارات")
                            .setMessage("نحتاج إلى إذن الإشعارات لإرسال التحديثات الهامة حول حجوزاتك، العروض الجديدة، ورسائل الدردشة الفورية.")
                            .setPositiveButton("منح الإذن 👍") { _, _ ->
                                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf("android.permission.POST_NOTIFICATIONS"), 102)
                            }
                            .setNegativeButton("إلغاء ❌") { dialog, _ ->
                                dialog.dismiss()
                                Toast.makeText(this, "تم إلغاء الإشعارات. لن تتلقى تنبيهات بالرسائل الجديدة.", Toast.LENGTH_SHORT).show()
                            }
                            .create()
                            .show()
                    } else {
                        androidx.core.app.ActivityCompat.requestPermissions(
                            this,
                            arrayOf("android.permission.POST_NOTIFICATIONS"),
                            102
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            com.example.util.FirestoreLocalBackupWorker.schedulePeriodicBackup(this)
            com.example.util.SecurityManager.verifyAppSignature(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val settingsState by viewModel.settings.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()

            val context = LocalContext.current

            LaunchedEffect(context) {
                viewModel.initializeUserIdentity(context)
                try {
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                    if (cm != null) {
                        val activeNetwork = cm.activeNetwork
                        val capabilities = cm.getNetworkCapabilities(activeNetwork)
                        viewModel.updateOnlineStatus(capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)

                        val request = android.net.NetworkRequest.Builder()
                            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .build()
                        cm.registerNetworkCallback(request, object : android.net.ConnectivityManager.NetworkCallback() {
                            override fun onAvailable(network: android.net.Network) {
                                viewModel.updateOnlineStatus(true)
                            }
                            override fun onLost(network: android.net.Network) {
                                viewModel.updateOnlineStatus(false)
                            }
                        })
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

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
                    startFusedLocationUpdates(this@MainActivity, viewModel)
                } else {
                    viewModel.triggerNotification("⚠️ تم رفض إذن الموقع، يمكنك تحديد مدينتك يدوياً")
                }
            }

            LaunchedEffect(Unit) {
                try {
                    val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasFine || hasCoarse) {
                        startFusedLocationUpdates(this@MainActivity, viewModel)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val colors = remember(settingsState) {
                resolveThemePalette(settingsState)
            }

            val isInitialized by viewModel.isInitialized.collectAsState()
            val currentLang by viewModel.currentLanguage.collectAsState()

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
                val appStrings = if (currentLang == "en") com.example.ui.EnStrings else com.example.ui.ArStrings
                CompositionLocalProvider(
                    LocalLayoutDirection provides (if (currentLang == "en") LayoutDirection.Ltr else LayoutDirection.Rtl),
                    com.example.ui.LocalAppStrings provides appStrings
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (isInitialized) colors.background else Color.Black
                    ) {
                        if (!isInitialized) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 28.dp)
                                ) {
                                    // الدائرة والشعار WAM
                                    Box(
                                        modifier = Modifier
                                            .size(140.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1D58B8)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "WAM",
                                            color = Color(0xFF00DC82),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 36.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(36.dp))

                                    // العنوان الرئيسي
                                    Text(
                                        text = "كل خدمات اليمن",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // العنوان الفرعي
                                    Text(
                                        text = "دليلك الشامل لكل الخدمات والمهن",
                                        fontSize = 16.sp,
                                        color = Color(0xFFD1D5DB),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(38.dp))

                                    // مؤشر التحميل الأخضر/الفيروزي
                                    CircularProgressIndicator(
                                        color = Color(0xFF00DC82),
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )

                                    Spacer(modifier = Modifier.height(38.dp))

                                    // النص الترحيبي التعريفي
                                    val splashMessage = if (settingsState.splashWelcomeMessage.isNotBlank()) {
                                        settingsState.splashWelcomeMessage
                                    } else {
                                        "التطبيق الأول في اليمن والوطن العربي الذي يربط مقدمي الخدمات وأصحاب المهن بالمستخدمين فورياً"
                                    }
                                    Text(
                                        text = splashMessage,
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 22.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp)
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
