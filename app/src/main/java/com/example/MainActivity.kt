package com.example

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.AppNavigator
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette

/**
 * 📱 MainActivity
 * النشاط الرئيسي للتطبيق: إدارة دورة الحياة، أذونات النظام (الموقع، الإشعارات، الصوت)، والتنقل.
 */
class MainActivity : ComponentActivity() {

    private var lastBackPressTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            setTheme(R.style.Theme_MyApplication)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onCreate(savedInstanceState)

        // إعداد محرك الصوت مع النشاط
        VoiceManager.onHear = { callback ->
            try {
                VoiceManager.startListening(this, onResult = { text ->
                    callback(text)
                }, onError = { err ->
                    Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        checkNotificationPermission()
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val settingsState by viewModel.settings.collectAsState()
            val isInitialized by viewModel.isInitialized.collectAsState()
            val currentLang by viewModel.currentLanguage.collectAsState()
            val context = LocalContext.current

            // مراقبة حالة اتصال الإنترنت
            LaunchedEffect(context) {
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

            val colors: VisualThemePalette = remember(settingsState) {
                resolveThemePalette(settingsState)
            }

            BackHandler {
                val handled = viewModel.goBack()
                if (!handled) {
                    val now = System.currentTimeMillis()
                    if (now - lastBackPressTime < 2000) {
                        finish()
                    } else {
                        lastBackPressTime = now
                        Toast.makeText(context, "اضغط مرة أخرى للخروج / Press again to exit", Toast.LENGTH_SHORT).show()
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
                            SplashScreen(welcomeMessage = settingsState.splashWelcomeMessage)
                        } else {
                            AppNavigator(
                                viewModel = viewModel,
                                themeColors = colors,
                                permissionLauncher = permissionLauncher,
                                locationPermissions = locationPermissions
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    102
                )
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
            VoiceManager.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}
