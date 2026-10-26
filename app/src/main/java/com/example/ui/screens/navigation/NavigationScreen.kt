@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.navigation

import android.content.Context
import android.speech.tts.TextToSpeech
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.utils.*
import java.util.Locale

@Composable
fun NavigationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var isNavigating by remember { mutableStateOf(false) }
    var instruction by remember { mutableStateOf("ابدأ بالبحث عن وجهة") }
    var distance by remember { mutableStateOf("0 م") }
    
    // Smart Navigator Initialization
    val isDownloaded = remember { ValhallaTileDownloader.isTilesDownloaded(context) }
    val navigator = remember {
        if (isDownloaded) RealValhallaNavigator(context) else MockValhallaNavigator
    }

    val tts = remember {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale("ar")
            }
        }
        ttsInstance
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الملاحة الذكية") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Map WebView
            AndroidView(factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    loadUrl("file:///android_asset/map.html")
                }
            }, modifier = Modifier.fillMaxSize())

            // Unified Map Control Stack (Left Side)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Navigation Toggle (Top)
                IconButton(
                    onClick = { isNavigating = !isNavigating },
                    modifier = Modifier.testTag("nav_mode_toggle").size(48.dp)
                ) {
                    Icon(
                        if (isNavigating) Icons.Default.Navigation else Icons.Default.Map,
                        "تبديل وضع الملاحة",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // GPS Re-locate Icon (Below Navigation)
                IconButton(
                    onClick = { /* Implement Re-locate Logic */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        "إعادة تحديد الموقع",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Add more map icons here later...
            }

            // Search Bar
            TextField(
                value = "",
                onValueChange = { /* Search Logic */ },
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth().testTag("search_bar"),
                placeholder = { Text("بحث عن وجهة...") },
                leadingIcon = { Icon(Icons.Default.Search, "بحث") }
            )

            // Top Card: Instructions
            Card(modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp, start = 16.dp, end = 16.dp).fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Directions, "تعليمات")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(instruction)
                }
            }

            // Bottom Card: Distance & End
            Card(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("المسافة: $distance")
                    Button(onClick = { isNavigating = false }, modifier = Modifier.testTag("end_nav_button")) {
                        Icon(Icons.Default.Stop, "إيقاف الملاحة")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إنهاء")
                    }
                }
            }
        }
    }
}
