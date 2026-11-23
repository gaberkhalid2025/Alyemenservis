@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.assistant
import com.example.ui.*

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.assistant.components.*
import com.example.NetworkUtils
import com.example.VoiceManager
import com.example.data.AdminSettingsEntity
import com.example.data.ProviderEntity
import com.example.ui.MainViewModel
import android.Manifest
import com.example.utils.VisualThemePalette
import com.example.utils.VoiceSearchHelper
import com.example.utils.VoiceState
import com.example.utils.VoiceStatus
import com.example.ui.viewmodels.AssistantViewModel
import kotlinx.coroutines.launch
import java.util.UUID

data class AssistantMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val matchedEntities: List<Any> = emptyList(),
    val safetyLevel: String = "LOW",
    val safetyWarning: String? = null,
    val immediateActions: List<String> = emptyList()
)

/**
 * 🤖 Modern Smart Assistant Dialog View
 * Cleanly modularized view using AssistantHeader, AssistantChipsRow, AssistantMessageItem, and AssistantInputBar.
 */
@Composable
fun SmartAssistantDialogView(
    viewModel: MainViewModel,
    settings: AdminSettingsEntity,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit,
    onChatOpen: (String) -> Unit,
    assistantViewModel: AssistantViewModel = viewModel(),
    onRequestQuickService: (() -> Unit)? = null,
    onNavigateToMap: () -> Unit = {}
) {
    val context = LocalContext.current
    var isOnline by remember {
        mutableStateOf(
            try { NetworkUtils.isNetworkAvailable(context) } catch (_: Exception) { true }
        )
    }
    DisposableEffect(context) {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                isOnline = true
            }
            override fun onLost(network: android.net.Network) {
                isOnline = try { NetworkUtils.isNetworkAvailable(context) } catch (_: Exception) { false }
            }
            override fun onCapabilitiesChanged(
                network: android.net.Network,
                networkCapabilities: android.net.NetworkCapabilities
            ) {
                isOnline = networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }
        try {
            cm?.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {}
        onDispose {
            try { cm?.unregisterNetworkCallback(callback) } catch (_: Exception) {}
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val chatHistory by assistantViewModel.chatHistory.collectAsState()
    val typedText by assistantViewModel.typedText.collectAsState()
    val isGenerating by assistantViewModel.isGenerating.collectAsState()

    var voiceState by remember { mutableStateOf(VoiceState()) }
    val voiceHelper = remember(context) {
        VoiceSearchHelper(
            context = context,
            onFinal = { spoken ->
                assistantViewModel.updateTypedText(spoken)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("🎙️ تم الاستماع: '$spoken'")
                }
            },
            onState = { st -> voiceState = st }
        )
    }
    DisposableEffect(voiceHelper) {
        onDispose { voiceHelper.destroy() }
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceHelper.start()
        } else {
            voiceState = VoiceState(
                status = VoiceStatus.ERROR,
                message = "يلزم منح إذن الميكروفون لاستعمال البحث الصوتي.",
                canUseKeyboard = true
            )
        }
    }
    val startVoiceSearch: () -> Unit = {
        if (voiceState.status == VoiceStatus.LISTENING) {
            voiceHelper.stop()
        } else if (!voiceHelper.hasMicPermission()) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            voiceHelper.start()
        }
    }

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    BackHandler(enabled = true, onBack = onDismiss)

    Surface(
        color = themeColors.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize() // FIXED: Removed root imePadding to prevent dialog background squishing
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                AssistantHeader(
                    isOnline = isOnline,
                    themeColors = themeColors,
                    onClearChat = {
                        assistantViewModel.clearChat(settings.welcomeMessage)
                        coroutineScope.launch { snackbarHostState.showSnackbar("🧹 تم مسح سجل المحادثة") }
                    },
                    onDismiss = onDismiss
                )

                // Navigation Chips
                AssistantChipsRow(
                    themeColors = themeColors,
                    onRequestQuickService = {
                        onDismiss()
                        if (onRequestQuickService != null) {
                            onRequestQuickService.invoke()
                        } else {
                            viewModel.navigateToScreen(AppScreens.QUICK_SERVICE_REQUEST)
                        }
                    },
                    onNavigateTechnicians = {
                        onDismiss()
                        viewModel.openBrowserTab("الرئيسية")
                        viewModel.navigateToScreen(AppScreens.USER_BROWSE)
                    },
                    onNavigateStores = {
                        onDismiss()
                        viewModel.openBrowserTab("المحلات والمتاجر")
                        viewModel.navigateToScreen(AppScreens.USER_BROWSE)
                    },
                    onNavigateRestaurants = {
                        onDismiss()
                        viewModel.openBrowserTab("المطاعم والكافيهات")
                        viewModel.navigateToScreen(AppScreens.USER_BROWSE)
                    },
                    onNavigateMedical = {
                        onDismiss()
                        viewModel.openBrowserTab("المراكز الطبية")
                        viewModel.navigateToScreen(AppScreens.USER_BROWSE)
                    },
                    onNavigateToMap = { 
                        onDismiss()
                        onNavigateToMap()
                    }
                )

                    // Voice Recognition Banner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .clickable { startVoiceSearch() },
                        colors = CardDefaults.cardColors(
                            containerColor = when (voiceState.status) {
                                VoiceStatus.LISTENING -> themeColors.accent.copy(alpha = 0.14f)
                                VoiceStatus.ERROR -> Color(0xFFEF4444).copy(alpha = 0.12f)
                                VoiceStatus.IDLE -> themeColors.surface
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (voiceState.status == VoiceStatus.ERROR) Color(0xFFEF4444).copy(alpha = 0.6f)
                            else themeColors.accent.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (voiceState.status == VoiceStatus.LISTENING) "🔴" else "🎙️", fontSize = 18.sp)
                                    Column {
                                        Text(
                                            text = when (voiceState.status) {
                                                VoiceStatus.LISTENING -> "جاري الاستماع... تحدث الآن"
                                                VoiceStatus.ERROR -> "تعذر التعرف الصوتي"
                                                VoiceStatus.IDLE -> "البحث الصوتي الذكي"
                                            },
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (voiceState.status == VoiceStatus.ERROR) Color(0xFFEF4444) else themeColors.accent
                                        )
                                        Text(
                                            text = when {
                                                voiceState.status == VoiceStatus.LISTENING && voiceState.partial.isNotBlank() -> voiceState.partial
                                                voiceState.status == VoiceStatus.ERROR && !voiceState.message.isNullOrBlank() -> voiceState.message!!
                                                else -> "انقر للتحدث أو البحث بصوتك (دعم متعدد للهجات العربية)"
                                            },
                                            fontSize = 9.5.sp,
                                            color = themeColors.textSecondary
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (voiceState.status == VoiceStatus.LISTENING) Color(0xFFEF4444) else themeColors.accent)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (voiceState.status == VoiceStatus.LISTENING) "إيقاف" else "تحدث الآن",
                                        fontSize = 9.sp,
                                        color = if (voiceState.status == VoiceStatus.LISTENING) Color.White else Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (voiceState.status == VoiceStatus.ERROR && voiceState.canOpenSettings) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "⚙️ فتح إعدادات الصوت",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.accent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { VoiceSearchHelper.openVoiceSettings(context) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Text(
                                        text = "✖️ إخفاء",
                                        fontSize = 9.5.sp,
                                        color = themeColors.textSecondary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { voiceHelper.cancel() }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Chat Messages List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chatHistory, key = { it.id }) { msg ->
                            AssistantMessageItem(
                                msg = msg,
                                viewModel = viewModel,
                                themeColors = themeColors,
                                onRequestQuickService = {
                                    onDismiss()
                                    if (onRequestQuickService != null) {
                                        onRequestQuickService.invoke()
                                    } else {
                                        viewModel.navigateToScreen(AppScreens.QUICK_SERVICE_REQUEST)
                                    }
                                },
                                onNavigateToMap = onNavigateToMap,
                                onChatOpen = onChatOpen
                            )
                        }

                        if (isGenerating) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                color = themeColors.accent,
                                                strokeWidth = 2.dp
                                            )
                                            Text("جاري البحث وتجهيز الرد...", fontSize = 11.sp, color = themeColors.textSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Prompt Suggestions
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val promptSuggestions = listOf(
                            "🔧 أقرب سباك في منطقتي",
                            "⚡ كهربائي منازل فوري",
                            "❄️ صيانة وتعبئة تكييف",
                            "🩺 عيادات ومراكز طبية",
                            "🚗 ميكانيكي وبنشر متنقل",
                            "🧹 نقل أثاث ونظافة"
                        )
                        items(promptSuggestions) { pText ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(themeColors.surface)
                                    .border(0.8.dp, themeColors.border, RoundedCornerShape(12.dp))
                                    .clickable { assistantViewModel.updateTypedText(pText.substringAfter(" ")) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(pText, fontSize = 10.sp, color = themeColors.textPrimary)
                            }
                        }
                    }

                    // Input Bar (Anchored directly above keyboard)
                    Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
                        AssistantInputBar(
                            typedText = typedText,
                            isGenerating = isGenerating,
                            themeColors = themeColors,
                            onTextChanged = { assistantViewModel.updateTypedText(it) },
                            onSend = {
                                try {
                                    if (typedText.isNotBlank() && !isGenerating) {
                                        assistantViewModel.sendUserQuery(
                                            prompt = typedText,
                                            isOnline = isOnline,
                                            mainViewModel = viewModel,
                                            settings = settings,
                                            onSpeechSpeak = {
                                                try {
                                                    VoiceManager.init(context)
                                                    VoiceManager.speak(it)
                                                } catch (_: Exception) {}
                                            },
                                            onError = { err ->
                                                coroutineScope.launch { snackbarHostState.showSnackbar(err) }
                                            },
                                            context = context
                                        )
                                    }
                                } catch (e: Exception) {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("تعذر معالجة الطلب: ${e.localizedMessage}") }
                                }
                            },
                            onMicClick = { startVoiceSearch() }
                        )
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
