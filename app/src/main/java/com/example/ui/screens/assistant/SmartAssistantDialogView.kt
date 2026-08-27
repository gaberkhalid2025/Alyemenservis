@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.assistant

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.NetworkUtils
import com.example.VoiceManager
import com.example.data.AdminSettingsEntity
import com.example.data.ProviderEntity
import com.example.ui.MainViewModel
import com.example.ui.ProviderCard
import com.example.utils.VisualThemePalette
import com.example.viewmodels.AssistantViewModel
import kotlinx.coroutines.launch
import java.util.UUID

data class AssistantMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val matchedProviders: List<ProviderEntity> = emptyList()
)

/**
 * 🤖 Modern Smart Assistant Dialog View
 * واجهة المحادثة التفاعلية للمساعد الذكي بدليل خدمات اليمن.
 * يرتبط بـ AssistantViewModel ويدعم البحث الصوتي والذكاء التوليدي وسجل المحادثة.
 */
@Composable
fun SmartAssistantDialogView(
    viewModel: MainViewModel,
    settings: AdminSettingsEntity,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit,
    onChatOpen: (String) -> Unit,
    assistantViewModel: AssistantViewModel = viewModel(),
    onRequestQuickService: () -> Unit = {},
    onNavigateToMap: () -> Unit = {}
) {
    val context = LocalContext.current
    val isOnline = NetworkUtils.isNetworkAvailable(context)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val chatHistory by assistantViewModel.chatHistory.collectAsState()
    val typedText by assistantViewModel.typedText.collectAsState()
    val isGenerating by assistantViewModel.isGenerating.collectAsState()

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val spokenMatches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                assistantViewModel.updateTypedText(spokenText)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("🎙️ تم الاستماع: '$spokenText'")
                }
            }
        }
    }

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = themeColors.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Surface(
                        color = themeColors.surface,
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(themeColors.primary, themeColors.accent))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🤖", fontSize = 18.sp)
                                    }
                                    Column {
                                        Text(
                                            text = "المساعد الذكي لدليل اليمن",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = themeColors.textPrimary
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B))
                                            )
                                            Text(
                                                text = if (isOnline) "متصل بالذكاء التوليدي" else "وضع أوفلاين (ذكاء محلي)",
                                                fontSize = 10.sp,
                                                color = if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Clear Chat Button
                                    IconButton(
                                        onClick = {
                                            assistantViewModel.clearChat(settings.welcomeMessage)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("🧹 تم مسح سجل المحادثة")
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "مسح المحادثة",
                                            tint = themeColors.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Close Button
                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "إغلاق",
                                            tint = themeColors.textPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick Navigation & Action Chips Row
                    Surface(
                        color = themeColors.surface.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                AssistChip(
                                    onClick = onRequestQuickService,
                                    label = { Text("⚡ اطلب خدمتك الآن", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFEF4444)),
                                    border = null,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = onRequestQuickService,
                                    label = { Text("🔧 طلب أقرب فني", fontSize = 10.5.sp, color = themeColors.accent, fontWeight = FontWeight.Bold) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.surface),
                                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        viewModel.navigateTo("STORES_VIEW")
                                        onDismiss()
                                    },
                                    label = { Text("🏬 المتاجر والمراكز", fontSize = 10.5.sp, color = themeColors.textPrimary) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.surface),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        viewModel.navigateTo("RESTAURANTS_VIEW")
                                        onDismiss()
                                    },
                                    label = { Text("🍽️ المطاعم والكافيهات", fontSize = 10.5.sp, color = themeColors.textPrimary) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.surface),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        viewModel.navigateTo("MEDICAL_VIEW")
                                        onDismiss()
                                    },
                                    label = { Text("🏥 المراكز الطبية", fontSize = 10.5.sp, color = themeColors.textPrimary) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.surface),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = onNavigateToMap,
                                    label = { Text("📍 خريطة الخدمات", fontSize = 10.5.sp, color = themeColors.textPrimary) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.surface),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // Voice Recognition Banner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .clickable {
                                try {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-YE")
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث باسم الخدمة أو الفني أو المشكلة...")
                                    }
                                    speechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    VoiceManager.onHear?.invoke { spoken ->
                                        if (spoken.isNotEmpty()) {
                                            assistantViewModel.updateTypedText(spoken)
                                        }
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🎙️", fontSize = 18.sp)
                                Column {
                                    Text(
                                        text = "البحث الصوتي الذكي",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.accent
                                    )
                                    Text(
                                        text = "انقر للتحدث أو البحث بصوتك فوراً",
                                        fontSize = 9.5.sp,
                                        color = themeColors.textSecondary
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(themeColors.accent)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("تحدث الآن", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
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
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (msg.isUser) themeColors.primary else themeColors.surface
                                        ),
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (msg.isUser) 12.dp else 2.dp,
                                            bottomEnd = if (msg.isUser) 2.dp else 12.dp
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (msg.isUser) themeColors.primary else themeColors.border
                                        ),
                                        modifier = Modifier.widthIn(max = 320.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = msg.text,
                                                fontSize = 12.sp,
                                                color = themeColors.textPrimary,
                                                lineHeight = 18.sp
                                            )

                                            if (!msg.isUser) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                HorizontalDivider(color = themeColors.border)
                                                Spacer(modifier = Modifier.height(6.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                                            .border(0.8.dp, Color(0xFFEF4444), RoundedCornerShape(6.dp))
                                                            .clickable { onRequestQuickService() }
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text("⚡ اطلب الآن", fontSize = 9.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                                            .border(0.8.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
                                                            .clickable { onNavigateToMap() }
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text("📍 الخريطة", fontSize = 9.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                                    }

                                                    Spacer(modifier = Modifier.weight(1f))

                                                    IconButton(
                                                        onClick = { VoiceManager.onSpeak?.invoke(msg.text) },
                                                        modifier = Modifier.size(22.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = "استماع",
                                                            tint = themeColors.textSecondary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (msg.matchedProviders.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "👇 الفنيين المقترحين لطلبك (اتصال / حجز مباشر):",
                                        fontSize = 11.sp,
                                        color = themeColors.accent,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        msg.matchedProviders.forEach { provider ->
                                            ProviderCard(
                                                provider = provider,
                                                themeColors = themeColors,
                                                viewModel = viewModel,
                                                onChatOpen = onChatOpen
                                            )
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
                                            Text("جاري توليد الإجابة الذكية...", fontSize = 11.sp, color = themeColors.textSecondary)
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

                    // Bottom Input Bar
                    Surface(
                        color = themeColors.surface,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = typedText,
                                onValueChange = { assistantViewModel.updateTypedText(it) },
                                placeholder = { Text("اكتب سؤالك أو اطلب خدمة هنا...", fontSize = 11.sp, color = themeColors.textSecondary) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = themeColors.textPrimary,
                                    unfocusedTextColor = themeColors.textPrimary,
                                    focusedBorderColor = themeColors.accent,
                                    unfocusedBorderColor = themeColors.border,
                                    focusedContainerColor = themeColors.background,
                                    unfocusedContainerColor = themeColors.background
                                )
                            )

                            // Send Button
                            IconButton(
                                onClick = {
                                    if (typedText.isNotBlank() && !isGenerating) {
                                        assistantViewModel.sendUserQuery(
                                            prompt = typedText,
                                            isOnline = isOnline,
                                            mainViewModel = viewModel,
                                            settings = settings,
                                            onSpeechSpeak = { VoiceManager.onSpeak?.invoke(it) },
                                            onError = { err ->
                                                coroutineScope.launch { snackbarHostState.showSnackbar(err) }
                                            }
                                        )
                                    }
                                },
                                enabled = typedText.isNotBlank() && !isGenerating,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (typedText.isNotBlank()) themeColors.accent else themeColors.surface)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "إرسال",
                                    tint = if (typedText.isNotBlank()) Color.Black else themeColors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
