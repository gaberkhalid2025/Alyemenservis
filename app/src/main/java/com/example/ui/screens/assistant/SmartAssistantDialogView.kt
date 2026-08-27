@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.assistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.NetworkUtils
import com.example.VoiceManager
import com.example.data.AdminSettingsEntity
import com.example.data.ProviderEntity
import com.example.ui.MainViewModel
import com.example.ui.ProviderCard
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import java.util.Locale
import java.util.UUID

data class AssistantMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val matchedProviders: List<ProviderEntity> = emptyList()
)

/**
 * 🤖 Modern, Clutter-Free Smart Assistant (Online + Offline + Voice Search + Quick Actions)
 */
@Composable
fun SmartAssistantDialogView(
    viewModel: MainViewModel,
    settings: AdminSettingsEntity,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit,
    onChatOpen: (String) -> Unit,
    onRequestQuickService: () -> Unit = {},
    onNavigateToMap: () -> Unit = {}
) {
    val context = LocalContext.current
    val isOnline = NetworkUtils.isNetworkAvailable(context)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Speech Recognizer Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val spokenMatches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.updateSearchQuery(spokenText)
                viewModel.triggerNotification("🎙️ تم الاستماع: '$spokenText'")
            }
        }
    }

    val defaultWelcome = settings.welcomeMessage.ifEmpty { 
        "مرحباً بك في المساعد الذكي لدليل خدمات اليمن 🇾🇪! كيف يمكنني خدمتك ومساعدتك اليوم؟" 
    }

    var chatHistory by remember { 
        mutableStateOf(listOf(AssistantMessage(text = defaultWelcome, isUser = false))) 
    }

    var typedText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

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
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    color = Color(0xFF1E293B),
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
                                        .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🤖", fontSize = 18.sp)
                                }
                                Column {
                                    Text(
                                        text = "المساعد الذكي لدليل اليمن",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
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
                                        chatHistory = listOf(AssistantMessage(text = defaultWelcome, isUser = false))
                                        Toast.makeText(context, "🧹 تم مسح سجل المحادثة", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "مسح المحادثة",
                                        tint = Color.LightGray,
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
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Navigation & Action Chips Row
                Surface(
                    color = Color(0xFF131D2E),
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
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E293B)),
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
                                label = { Text("🏬 المتاجر والمراكز", fontSize = 10.5.sp, color = Color.White) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            AssistChip(
                                onClick = {
                                    viewModel.navigateTo("RESTAURANTS_VIEW")
                                    onDismiss()
                                },
                                label = { Text("🍽️ المطاعم والكافيهات", fontSize = 10.5.sp, color = Color.White) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            AssistChip(
                                onClick = {
                                    viewModel.navigateTo("MEDICAL_VIEW")
                                    onDismiss()
                                },
                                label = { Text("🏥 المراكز الطبية", fontSize = 10.5.sp, color = Color.White) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            AssistChip(
                                onClick = {
                                    viewModel.navigateTo("BOOKINGS_VIEW")
                                    onDismiss()
                                },
                                label = { Text("📋 حجوزاتي وطلباتي", fontSize = 10.5.sp, color = Color.White) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            AssistChip(
                                onClick = onNavigateToMap,
                                label = { Text("📍 خريطة الخدمات", fontSize = 10.5.sp, color = Color.White) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Voice Recognition Action Banner
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
                                        typedText = spoken
                                    }
                                }
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
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
                                    color = Color.LightGray
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
                                        containerColor = if (msg.isUser) Color(0xFF0284C7) else Color(0xFF1E293B)
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (msg.isUser) 12.dp else 2.dp,
                                        bottomEnd = if (msg.isUser) 2.dp else 12.dp
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (msg.isUser) Color(0xFF0284C7) else Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.widthIn(max = 320.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = msg.text,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            lineHeight = 18.sp
                                        )

                                        if (!msg.isUser) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Divider(color = Color.White.copy(alpha = 0.1f))
                                            Spacer(modifier = Modifier.height(6.dp))

                                            // In-message Quick Action Chips
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

                                                // Text to Speech
                                                IconButton(
                                                    onClick = { VoiceManager.onSpeak?.invoke(msg.text) },
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "استماع",
                                                        tint = Color.LightGray,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Interactive Provider Response Cards
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
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
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
                                        Text("جاري توليد الإجابة الذكية...", fontSize = 11.sp, color = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Prompt Chips
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
                                .background(Color(0xFF1E293B))
                                .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .clickable { typedText = pText.substringAfter(" ") }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(pText, fontSize = 10.sp, color = Color.White)
                        }
                    }
                }

                // Bottom Input Bar
                Surface(
                    color = Color(0xFF1E293B),
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
                            onValueChange = { typedText = it },
                            placeholder = { Text("اكتب سؤالك أو اطلب خدمة هنا...", fontSize = 11.sp, color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = themeColors.accent,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            )
                        )

                        // Send Button
                        IconButton(
                            onClick = {
                                if (typedText.isNotBlank() && !isGenerating) {
                                    val prompt = typedText.trim()
                                    typedText = ""
                                    chatHistory = chatHistory + AssistantMessage(text = prompt, isUser = true)
                                    isGenerating = true

                                    coroutineScope.launch(Dispatchers.IO) {
                                        val responseMsg = processAssistantQuery(
                                            prompt = prompt,
                                            isOnline = isOnline,
                                            viewModel = viewModel,
                                            settings = settings,
                                            chatHistory = chatHistory
                                        )

                                        withContext(Dispatchers.Main) {
                                            chatHistory = chatHistory + responseMsg
                                            isGenerating = false
                                            if (settings.allowTextToSpeechAssistant) {
                                                VoiceManager.onSpeak?.invoke(responseMsg.text)
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = typedText.isNotBlank() && !isGenerating,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (typedText.isNotBlank()) themeColors.accent else Color.DarkGray)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "إرسال",
                                tint = if (typedText.isNotBlank()) Color.Black else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ⚡ Process Assistant Query (Online with Gemini API or Offline Local Database Engine)
 */
private suspend fun processAssistantQuery(
    prompt: String,
    isOnline: Boolean,
    viewModel: MainViewModel,
    settings: AdminSettingsEntity,
    chatHistory: List<AssistantMessage>
): AssistantMessage {
    val providersList = viewModel.providers.value
    val categoriesList = viewModel.categories.value

    fun normalizeArabic(text: String): String {
        var str = text.trim().lowercase(Locale.ROOT)
        str = str.replace(Regex("[\\u064B-\\u0652]"), "")
        str = str.replace(Regex("[أإآ]"), "ا")
        str = str.replace("ى", "ي")
        str = str.replace("ة", "ه")
        return str
    }

    val qNormalized = normalizeArabic(prompt)
    val matched = providersList.filter { p ->
        val catName = categoriesList.find { it.id == p.categoryId }?.name ?: ""
        val pNameNorm = normalizeArabic(p.name)
        val pProfNorm = normalizeArabic(p.profession)
        val pSpecNorm = normalizeArabic(p.specialization)
        val pAreaNorm = normalizeArabic(p.area)
        val catNameNorm = normalizeArabic(catName)

        pNameNorm.contains(qNormalized) ||
        pProfNorm.contains(qNormalized) ||
        pSpecNorm.contains(qNormalized) ||
        pAreaNorm.contains(qNormalized) ||
        catNameNorm.contains(qNormalized) ||
        qNormalized.split(" ", "،", ",").any { word ->
            val wordNorm = normalizeArabic(word)
            wordNorm.length > 2 && (
                pNameNorm.contains(wordNorm) ||
                pProfNorm.contains(wordNorm) ||
                pSpecNorm.contains(wordNorm) ||
                pAreaNorm.contains(wordNorm) ||
                catNameNorm.contains(wordNorm)
            )
        }
    }.take(5)

    if (isOnline) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isNotEmpty()) {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val ragList = if (matched.isNotEmpty()) matched else providersList.filter { it.isVip }.take(10)

                val dbContextText = StringBuilder()
                dbContextText.append("البيانات المسترجعة من قاعدة بيانات المنصة في اليمن (RAG Data):\n")
                ragList.forEach { p ->
                    val catName = categoriesList.find { it.id == p.categoryId }?.name ?: "خدمة عامة"
                    dbContextText.append("- الفني: ${p.name} | الهاتف: ${p.phone} | المنطقة: ${p.area} | الحي: ${p.localNeighborhood} | التخصص: ${p.specialization.ifEmpty { p.profession }} | القسم: $catName | التقييم: ${p.rating}/5.0 | الحالة: ${if (p.isAvailable) "متاح" else "مشغول"}\n")
                }
                dbContextText.append("\nمعلومات الدعم الفني:\n")
                dbContextText.append("- هاتف الدعم: ${settings.supportPhone}\n")
                dbContextText.append("- واتساب: ${settings.supportWhatsapp}\n")

                val systemInstructionText = "أنت 'مساعد منصة دليل خدمات اليمن الذكي'. أجب باللغة العربية الفصحى أو اللهجة اليمنية المحببة باختصار ودقة. اقترح بيانات الفنيين المسترجعة عند السؤال عن خدمات أو صيانات.\n\nالبيانات المتاحة:\n$dbContextText"

                val contentsArray = org.json.JSONArray()
                chatHistory.takeLast(6).forEach { hMsg ->
                    val contentObj = org.json.JSONObject()
                    contentObj.put("role", if (hMsg.isUser) "user" else "model")
                    val partsArray = org.json.JSONArray()
                    val partObj = org.json.JSONObject()
                    partObj.put("text", hMsg.text)
                    partsArray.put(partObj)
                    contentObj.put("parts", partsArray)
                    contentsArray.put(contentObj)
                }
                val currentPromptObj = org.json.JSONObject()
                currentPromptObj.put("role", "user")
                val currentParts = org.json.JSONArray()
                val currentPart = org.json.JSONObject()
                currentPart.put("text", prompt)
                currentParts.put(currentPart)
                currentPromptObj.put("parts", currentParts)
                contentsArray.put(currentPromptObj)

                val systemInstructionObj = org.json.JSONObject()
                val sysParts = org.json.JSONArray()
                val sysPart = org.json.JSONObject()
                sysPart.put("text", systemInstructionText)
                sysParts.put(sysPart)
                systemInstructionObj.put("parts", sysParts)

                val finalRequestJsonObj = org.json.JSONObject()
                finalRequestJsonObj.put("contents", contentsArray)
                finalRequestJsonObj.put("systemInstruction", systemInstructionObj)

                val request = okhttp3.Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                    .post(okhttp3.RequestBody.create(mediaType, finalRequestJsonObj.toString()))
                    .build()

                val okHttpClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val apiResponse = okHttpClient.newCall(request).execute()
                if (apiResponse.isSuccessful) {
                    val bodyString = apiResponse.body?.string() ?: ""
                    val jsonObject = org.json.JSONObject(bodyString)
                    val candidates = jsonObject.optJSONArray("candidates")
                    val candidate = candidates?.optJSONObject(0)
                    val content = candidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val part = parts?.optJSONObject(0)
                    val textVal = part?.optString("text")
                    if (!textVal.isNullOrBlank()) {
                        return AssistantMessage(text = textVal, isUser = false, matchedProviders = matched)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to local offline engine on error
        }
    }

    val (localText, localProvs) = generateLocalOfflineResponse(prompt, viewModel)
    return AssistantMessage(text = localText, isUser = false, matchedProviders = localProvs)
}

fun generateLocalOfflineResponse(prompt: String, viewModel: MainViewModel): Pair<String, List<ProviderEntity>> {
    fun normalizeArabic(text: String): String {
        var str = text.trim().lowercase(Locale.ROOT)
        str = str.replace(Regex("[\\u064B-\\u0652]"), "")
        str = str.replace(Regex("[أإآ]"), "ا")
        str = str.replace("ى", "ي")
        str = str.replace("ة", "ه")
        return str
    }

    val qNormalized = normalizeArabic(prompt)
    val providers = viewModel.providers.value
    val categories = viewModel.categories.value
    val settings = viewModel.settings.value

    val isSupportContact = qNormalized.contains("رقم") || qNormalized.contains("اتصال") || 
                           qNormalized.contains("دعم") || qNormalized.contains("تواصل") || 
                           qNormalized.contains("تلفون") || qNormalized.contains("شكوى") || 
                           qNormalized.contains("مشكله") || qNormalized.contains("مساعده") || 
                           qNormalized.contains("واتساب") || qNormalized.contains("بريد")

    val isJoinRequest = qNormalized.contains("تسجيل") || qNormalized.contains("انضم") || 
                        qNormalized.contains("اريد") || qNormalized.contains("تقديم") || 
                        qNormalized.contains("عضو") || qNormalized.contains("حساب") || qNormalized.contains("اشتراك")

    val isPriceInfo = qNormalized.contains("سعر") || qNormalized.contains("رسوم") || 
                      qNormalized.contains("مجاني") || qNormalized.contains("عموله") || 
                      qNormalized.contains("تكلف") || qNormalized.contains("كم ياخذ")

    val isMapFeature = qNormalized.contains("خريطه") || qNormalized.contains("خرائط") || 
                       qNormalized.contains("موقع") || qNormalized.contains("رادار") || 
                       qNormalized.contains("تحديد") || qNormalized.contains("مسافه") ||
                       qNormalized.contains("gps")

    val isCityInfo = qNormalized.contains("مدينه") || qNormalized.contains("مدن") || 
                     qNormalized.contains("محافظه") || qNormalized.contains("تغطيه")

    val professions = listOf(
        "سباك", "كهربا", "دهان", "نجار", "حداد", "خياط", "سائق", "مصلح", "صيانه", "فني", 
        "مهندس", "تكييف", "تبريد", "بناء", "مقاول", "طبيب", "تنظيف", "ميكانيك"
    )
    val hasProfessionKeyword = professions.any { qNormalized.contains(it) }
    
    val isProviderSearch = hasProfessionKeyword || qNormalized.contains("ابحث") || 
                           qNormalized.contains("مقدم") || qNormalized.contains("رقم فني") ||
                           providers.any { normalizeArabic(it.name).contains(qNormalized) || qNormalized.contains(normalizeArabic(it.name)) }

    if (isProviderSearch) {
        var cityFilterId: String? = null
        if (qNormalized.contains("صنعاء")) cityFilterId = "ye_san"
        else if (qNormalized.contains("عدن")) cityFilterId = "ye_ade"
        else if (qNormalized.contains("تعز")) cityFilterId = "ye_tai"
        else if (qNormalized.contains("الحديده")) cityFilterId = "ye_hod"

        val matchedCategories = categories.filter { cat ->
            val normCat = normalizeArabic(cat.name)
            qNormalized.contains(normCat) || normCat.contains(qNormalized)
        }
        val matchedCatIds = matchedCategories.map { it.id }.toSet()

        val matchedProviders = providers.filter { p ->
            val pNameNorm = normalizeArabic(p.name)
            val pProfNorm = normalizeArabic(p.profession)
            val pSpecNorm = normalizeArabic(p.specialization)
            
            val matchesSearch = pNameNorm.contains(qNormalized) || pProfNorm.contains(qNormalized) || 
                                pSpecNorm.contains(qNormalized) || qNormalized.contains(pNameNorm) ||
                                qNormalized.contains(pProfNorm) || matchedCatIds.contains(p.categoryId)

            val matchesCity = cityFilterId == null || p.cityId == cityFilterId
            
            matchesSearch && matchesCity && !p.isBlocked
        }.sortedWith(compareByDescending<ProviderEntity> { it.rating }.thenByDescending { it.isAvailable })

        if (matchedProviders.isNotEmpty()) {
            val sb = StringBuilder()
            val citySuffix = if (cityFilterId != null) " في ${if (cityFilterId == "ye_san") "صنعاء" else if (cityFilterId == "ye_ade") "عدن" else if (cityFilterId == "ye_tai") "تعز" else "الحديدة"}" else ""
            sb.append("🔍 عثرت لك على الفنيين المعتمدين في دليل اليمن${citySuffix}:\n\n")
            
            matchedProviders.take(5).forEachIndexed { index, p ->
                val catName = categories.find { it.id == p.categoryId }?.name ?: p.profession.ifEmpty { "خدمة عامة" }
                val statusSymbol = if (p.isAvailable) "🟢 متاح" else "🔴 مشغول"
                val vipBadge = if (p.isVip) " 🏆 [VIP]" else ""
                
                sb.append("${index + 1}. الفني: *${p.name}*${vipBadge}\n")
                sb.append("   💼 التخصص: *${catName}* (${p.specialization.ifEmpty { p.profession }})\n")
                sb.append("   📱 الهاتف: *${p.phone}* | 📍 ${p.area} - ${p.localNeighborhood}\n")
                sb.append("   ⭐ التقييم: *${String.format(Locale.US, "%.1f", p.rating)}/5.0* | ${statusSymbol}\n\n")
            }
            sb.append("💡 يمكنك النقر على بطاقة الفني بالأسفل للاتصال به أو حجز موعد معه فوراً!")
            return Pair(sb.toString(), matchedProviders.take(5))
        } else {
            val suggestedCats = categories.take(4).map { it.name }.joinToString("، ")
            return Pair("لم أعثر على فني مطابق للاسم أو التخصص بدقة، ولكن الدليل يضم نخبة الفنيين في صنعاء، عدن، تعز، إب وباقي المحافظات. يمكنك تفقد الأقسام: (${suggestedCats}) أو الضغط على زر '⚡ اطلب خدمتك الآن'.", emptyList())
        }
    }

    val textResult = when {
        qNormalized.contains("مرحبا") || qNormalized.contains("السلام") || qNormalized.contains("هلا") || 
        qNormalized.contains("صباح") || qNormalized.contains("مساء") -> {
            "أهلاً وسهلاً بك في دليل خدمات اليمن 🇾🇪! أنا مساعدك الذكي لمساعدتك في الوصول لأفضل الفنيين، المراكز الطبية، المتاجر والمطاعم في منطقتك بكل سرعة وسهولة."
        }
        isSupportContact -> {
            "📱 للتواصل المباشر مع إدارة ودعم دليل خدمات اليمن:\n" +
            "- هاتف الدعم المباشر: *${settings.supportPhone}*\n" +
            "- واتساب الإدارة: *${settings.supportWhatsapp}*\n" +
            "- البريد الإلكتروني: *${settings.supportEmail}*"
        }
        isJoinRequest -> {
            "📝 للانضمام كفني أو متجر معتمد في الدليل، يمكنك فتح استمارة 'طلب الانضمام' من الشاشة الرئيسية، وكتابة بياناتك وهاتفك لتوثيق حسابك ووصول العملاء إليك فوراً."
        }
        isPriceInfo -> {
            "💰 تطبيق دليل خدمات اليمن مجاني تماماً وبدون أي عمولات أو رسوم على الاتفاقات المباشرة بينك وبين الفني أو المتجر."
        }
        isMapFeature -> {
            "🗺️ يمكنك النقر على خيار 'خريطة الخدمات' بالأعلى لعرض الفنيين والمراكز القريبة من موقعك الجغرافي بالأمتار."
        }
        isCityInfo -> {
            "🇾🇪 يغطي دليل خدمات اليمن كافة المحافظات والمدن اليمنية (صنعاء، عدن، تعز، الحديدة، إب، حضرموت، ذمار، مأرب وباقي المناطق) مع التصفية السريعة حسب الحي والشارع."
        }
        else -> {
            "أهلاً بك! يمكنك سؤالي عن أي فني أو مهندس (كهرباء، سباكة، تكييف، دهان، ميكانيك) أو الضغط على '⚡ اطلب خدمتك الآن' لإطلاق طلب عاجل واستقبال عروض الأسعار فوراً!"
        }
    }
    return Pair(textResult, emptyList())
}

