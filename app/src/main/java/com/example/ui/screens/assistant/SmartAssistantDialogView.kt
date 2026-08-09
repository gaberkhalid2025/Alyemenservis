@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.assistant
import android.content.Intent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import okhttp3.MediaType.Companion.toMediaType

import com.example.*

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.utils.*
import com.example.ui.MainViewModel
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
import com.example.ui.*
import com.example.ui.utils.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class AssistantMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val matchedProviders: List<com.example.data.ProviderEntity> = emptyList()
)

// ------ Smart Assistant Dialog View Overlay ------
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
    val points by viewModel.currentUserPoints.collectAsState()
    val context = LocalContext.current
    val isOnline = com.example.NetworkUtils.isNetworkAvailable(context)
    val coroutineScope = rememberCoroutineScope()

    // Chat history state using AssistantMessage model
    var chatHistory by remember { mutableStateOf(listOf(
        AssistantMessage(
            text = settings.welcomeMessage.ifEmpty { "مرحباً بكم في منصة الخدمات اليمنية الشاملة! كيف يمكنني مساعدتك اليوم؟" },
            isUser = false
        )
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

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.accent.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, themeColors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            VoiceManager.onHear?.invoke { spokenText ->
                                if (spokenText.isNotEmpty()) {
                                    viewModel.updateSearchQuery(spokenText)
                                    viewModel.triggerNotification("🎙️ المساعد الذكي: جاري فلترة الخدمات وعرض نتائج البحث لـ '$spokenText' فوراً!")
                                    onDismiss()
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🎙️", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("البحث الصوتي الذكي والمباشر", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Text("انطق اسم الخدمة (مثال: سباك، طبيب، تنظيف) للوصول للنتائج فوراً", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
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
                    items(chatHistory, key = { it.id }) { msg ->
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
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
                                        bottomStart = if (msg.isUser) 12.dp else 0.dp,
                                        bottomEnd = if (msg.isUser) 0.dp else 12.dp
                                    ),
                                    border = BorderStroke(1.dp, if (msg.isUser) themeColors.primary else themeColors.accent.copy(alpha = 0.5f)),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = msg.text,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            lineHeight = 18.sp
                                        )
                                        
                                        if (!msg.isUser) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            // Interactive Action Chips
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                            ) {
                                                item {
                                                    AssistChip(
                                                        onClick = onRequestQuickService,
                                                        label = { Text("🛠️ طلب أقرب فني لهذه المشكلة", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themeColors.accent) },
                                                        colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.surface)
                                                    )
                                                }
                                                item {
                                                    AssistChip(
                                                        onClick = {
                                                            typedText = "محلات المواد القريبة"
                                                        },
                                                        label = { Text("🏬 محلات المواد القريبة", fontSize = 10.sp, color = Color.White) },
                                                        colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.surface)
                                                    )
                                                }
                                                item {
                                                    AssistChip(
                                                        onClick = {
                                                            val topProv = msg.matchedProviders.firstOrNull() ?: viewModel.providers.value.firstOrNull()
                                                            if (topProv != null) {
                                                                try {
                                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${topProv.phone}"))
                                                                    context.startActivity(intent)
                                                                } catch (e: Exception) {
                                                                    Toast.makeText(context, "📞 هاتف الفني: ${topProv.phone}", Toast.LENGTH_LONG).show()
                                                                }
                                                            } else {
                                                                Toast.makeText(context, "📱 هاتف الدعم: ${settings.supportPhone}", Toast.LENGTH_LONG).show()
                                                            }
                                                        },
                                                        label = { Text("📞 اتصل بفني", fontSize = 10.sp, color = Color.White) },
                                                        colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.surface)
                                                    )
                                                }
                                                item {
                                                    AssistChip(
                                                        onClick = onRequestQuickService,
                                                        label = { Text("📅 احجز موعداً", fontSize = 10.sp, color = Color.White) },
                                                        colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.surface)
                                                    )
                                                }
                                                item {
                                                    AssistChip(
                                                        onClick = onNavigateToMap,
                                                        label = { Text("📍 اعرض على الخريطة", fontSize = 10.sp, color = Color.White) },
                                                        colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.surface)
                                                    )
                                                }
                                            }

                                            if (settings.allowTextToSpeechAssistant) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    IconButton(
                                                        onClick = { VoiceManager.onSpeak?.invoke(msg.text) },
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

                            // Render interactive provider cards directly inside chat list!
                            if (msg.matchedProviders.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "👇 بطاقات استجابة تفاعلية (يمكنك الحجز أو الاتصال فوراً):",
                                    fontSize = 11.sp,
                                    color = themeColors.accent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
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
                                chatHistory = chatHistory + AssistantMessage(text = prompt, isUser = true)
                                isGenerating = true

                                // Perform async response generation
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val providersList = viewModel.providers.value
                                    val categoriesList = viewModel.categories.value

                                    val normalizeArabicLocal = { text: String ->
                                        var str = text.trim().lowercase()
                                        str = str.replace(Regex("[\\u064B-\\u0652]"), "")
                                        str = str.replace(Regex("[أإآ]"), "ا")
                                        str = str.replace("ى", "ي")
                                        str = str.replace("ة", "ه")
                                        str
                                    }
                                    val qNormalized = normalizeArabicLocal(prompt)
                                    val matched = providersList.filter { p ->
                                        val catName = categoriesList.find { it.id == p.categoryId }?.name ?: ""
                                        val pNameNorm = normalizeArabicLocal(p.name)
                                        val pProfNorm = normalizeArabicLocal(p.profession)
                                        val pSpecNorm = normalizeArabicLocal(p.specialization)
                                        val pAreaNorm = normalizeArabicLocal(p.area)
                                        val catNameNorm = normalizeArabicLocal(catName)

                                        pNameNorm.contains(qNormalized) ||
                                        pProfNorm.contains(qNormalized) ||
                                        pSpecNorm.contains(qNormalized) ||
                                        pAreaNorm.contains(qNormalized) ||
                                        catNameNorm.contains(qNormalized) ||
                                        qNormalized.split(" ", "،", ",").any { word ->
                                            val wordNorm = normalizeArabicLocal(word)
                                            wordNorm.length > 2 && (
                                                pNameNorm.contains(wordNorm) ||
                                                pProfNorm.contains(wordNorm) ||
                                                pSpecNorm.contains(wordNorm) ||
                                                pAreaNorm.contains(wordNorm) ||
                                                catNameNorm.contains(wordNorm)
                                            )
                                        }
                                    }.take(5)

                                    val responseMsg = if (isOnline) {
                                        // Attempt direct REST calling to Gemini API
                                        try {
                                            val apiKey = BuildConfig.GEMINI_API_KEY
                                            if (apiKey.isNotEmpty()) {
                                                val mediaType = "application/json; charset=utf-8".toMediaType()
                                                
                                                val ragList = if (matched.isNotEmpty()) matched else providersList.filter { it.isVip }.take(10)

                                                val dbContextText = StringBuilder()
                                                dbContextText.append("البيانات المسترجعة من قاعدة بيانات المنصة في اليمن (RAG Data):\\n")
                                                ragList.forEach { p ->
                                                    val catName = categoriesList.find { it.id == p.categoryId }?.name ?: "خدمة عامة"
                                                    dbContextText.append("- الفني: ${p.name} | رقم الهاتف: ${p.phone} | المنطقة: ${p.area} | الحي: ${p.localNeighborhood} | التخصص: ${p.specialization.ifEmpty { p.profession }} | القسم: $catName | التقييم: ${p.rating}/5.0 | الحالة: ${if (p.isAvailable) "متاح" else "مشغول"}\\n")
                                                }
                                                dbContextText.append("\\nمعلومات الدعم الفني الشامل للمنصة:\\n")
                                                dbContextText.append("- رقم هاتف الدعم: ${settings.supportPhone}\\n")
                                                dbContextText.append("- واتساب الإدارة: ${settings.supportWhatsapp}\\n")
                                                dbContextText.append("- بريد الشكاوى: ${settings.supportEmail}\\n")

                                                val systemInstructionText = "أنت 'مساعد منصة دليل خدمات اليمن الذكي'. نظام خبير مخصص للرد الفوري والدقيق للغاية باللغة العربية الفصحى أو اللهجة اليمنية المحببة.\\n\\nتصنيف الأسئلة ومعالجتها:\\n1. إذا كان السؤال عن فني أو مهندس: قم بتحليل الرغبة واقترح أسماء من البيانات المسترجعة (RAG) المرفقة بالأسفل، مع توفير أرقام هواتفهم ومناطقهم بكل أمانة ودقة دون اختراع أرقام أو بيانات.\\n2. إذا كان السؤال عن الدعم الفني: زوّد المستخدم بهواتف الدعم أو واتساب المرفق.\\n3. إذا كان خارج خدمات صيانة المنصة: اعتذر بأدب ووجهه لما هو مفيد بلهجة يمنية ترحيبية ودية.\\n\\nالبيانات الحية المتاحة:\\n$dbContextText"

                                                val contentsArray = org.json.JSONArray()
                                                chatHistory.forEach { hMsg ->
                                                    val contentObj = org.json.JSONObject()
                                                    contentObj.put("role", if (hMsg.isUser) "user" else "model")
                                                    val partsArray = org.json.JSONArray()
                                                    val partObj = org.json.JSONObject()
                                                    partObj.put("text", hMsg.text)
                                                    partsArray.put(partObj)
                                                    contentObj.put("parts", partsArray)
                                                    contentsArray.put(contentObj)
                                                }
                                                // Append current prompt
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

                                                val requestJson = finalRequestJsonObj.toString()
                                                
                                                val request = okhttp3.Request.Builder()
                                                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                                                    .post(okhttp3.RequestBody.create(mediaType, requestJson))
                                                    .build()

                                                val okHttpClient = okhttp3.OkHttpClient.Builder()
                                                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                                                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                                                    .build()

                                                okHttpClient.newCall(request).execute().use { apiResp ->
                                                    if (apiResp.isSuccessful) {
                                                        val bodyString = apiResp.body?.string() ?: ""
                                                        val jsonObject = org.json.JSONObject(bodyString)
                                                        val candidates = jsonObject.optJSONArray("candidates")
                                                        val candidate = candidates?.optJSONObject(0)
                                                        val content = candidate?.optJSONObject("content")
                                                        val parts = content?.optJSONArray("parts")
                                                        val part = parts?.optJSONObject(0)
                                                        val textVal = part?.optString("text")
                                                        val apiText = textVal ?: "لم أتمكن من استخلاص النص من الإجابة."
                                                        AssistantMessage(text = apiText, isUser = false, matchedProviders = matched)
                                                    } else {
                                                        val (localText, localProvs) = generateLocalOfflineResponse(prompt, viewModel)
                                                        AssistantMessage(text = localText, isUser = false, matchedProviders = localProvs)
                                                    }
                                                }
                                            } else {
                                                val (localText, localProvs) = generateLocalOfflineResponse(prompt, viewModel)
                                                AssistantMessage(text = localText, isUser = false, matchedProviders = localProvs)
                                            }
                                        } catch (e: Exception) {
                                            val (localText, localProvs) = generateLocalOfflineResponse(prompt, viewModel)
                                            AssistantMessage(text = localText, isUser = false, matchedProviders = localProvs)
                                        }
                                    } else {
                                        val (localText, localProvs) = generateLocalOfflineResponse(prompt, viewModel)
                                        AssistantMessage(text = localText, isUser = false, matchedProviders = localProvs)
                                    }

                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        chatHistory = chatHistory + responseMsg
                                        isGenerating = false
                                        // Auto speak response!
                                        if (settings.allowTextToSpeechAssistant) {
                                            VoiceManager.onSpeak?.invoke(responseMsg.text)
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
                            chatHistory = listOf(AssistantMessage(
                                text = settings.welcomeMessage.ifEmpty { "مرحباً بكم في منصة الخدمات اليمنية الشاملة! كيف يمكنني مساعدتك اليوم؟" },
                                isUser = false
                            ))
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

fun generateLocalOfflineResponse(prompt: String, viewModel: MainViewModel): Pair<String, List<com.example.data.ProviderEntity>> {
    // 1. Helper Arabic Normalization Function for robust NLP
    fun normalizeArabic(text: String): String {
        var str = text.trim().lowercase()
        // Strip diacritics (harakat)
        str = str.replace(Regex("[\\u064B-\\u0652]"), "")
        // Normalize Alefs
        str = str.replace(Regex("[\\u0622\\u0623\\u0625]"), "ا") // أإآ
        // Normalize Yeh/Alef Maqsurah
        str = str.replace("ى", "ي")
        // Normalize Teh Marbutah
        str = str.replace("ة", "ه")
        return str
    }

    val qNormalized = normalizeArabic(prompt)
    val providers = viewModel.providers.value
    val categories = viewModel.categories.value
    val settings = viewModel.settings.value

    // 2. Question Classification & NLP Categorization
    val isSupportContact = qNormalized.contains("رقم") || qNormalized.contains("اتصال") || 
                           qNormalized.contains("دعم") || qNormalized.contains("تواصل") || 
                           qNormalized.contains("تلفون") || qNormalized.contains("شكوى") || 
                           qNormalized.contains("مشكله") || qNormalized.contains("مظالم") || 
                           qNormalized.contains("مساعده") || qNormalized.contains("واتساب") ||
                           qNormalized.contains("بريد") || qNormalized.contains("ايميل")

    val isJoinRequest = qNormalized.contains("تسجيل") || qNormalized.contains("انضم") || 
                        qNormalized.contains("اريد") || qNormalized.contains("طلب") || 
                        qNormalized.contains("تقديم") || qNormalized.contains("عضو") || 
                        qNormalized.contains("حساب") || qNormalized.contains("اشتراك")

    val isPriceInfo = qNormalized.contains("سعر") || qNormalized.contains("رسوم") || 
                      qNormalized.contains("مجاني") || qNormalized.contains("فلوس") || 
                      qNormalized.contains("عموله") || qNormalized.contains("تكلف") ||
                      qNormalized.contains("كم ياخذ") || qNormalized.contains("مجانا")

    val isMapFeature = qNormalized.contains("خريطه") || qNormalized.contains("خرائط") || 
                       qNormalized.contains("موقع") || qNormalized.contains("رادار") || 
                       qNormalized.contains("تحديد") || qNormalized.contains("مسافه") ||
                       qNormalized.contains("جي بي اس") || qNormalized.contains("gps")

    val isCityInfo = qNormalized.contains("مدينه") || qNormalized.contains("مدن") || 
                     qNormalized.contains("محافظه") || qNormalized.contains("محافظات") ||
                     qNormalized.contains("تغطي") || qNormalized.contains("تغطيه")

    // Determine if user is searching for specific professions/keywords
    val professions = listOf(
        "سباك", "كهربا", "دهان", "نجار", "حداد", "خياط", "سائق", "مصلح", "صيانه", "فني", 
        "مهندس", "تكييف", "تبريد", "بناء", "مقاول", "طبيب", "تنظيف", "ميكانيك"
    )
    val hasProfessionKeyword = professions.any { qNormalized.contains(it) }
    
    // RAG: Check if query searches for providers
    val isProviderSearch = hasProfessionKeyword || qNormalized.contains("ابحث") || 
                           qNormalized.contains("مقدم") || qNormalized.contains("رقم فني") ||
                           providers.any { normalizeArabic(it.name).contains(qNormalized) || qNormalized.contains(normalizeArabic(it.name)) }

    // 3. RAG Search Logic
    if (isProviderSearch) {
        // Detect selected city filters from the query
        var cityFilterId: String? = null
        if (qNormalized.contains("صنعاء")) cityFilterId = "ye_san"
        else if (qNormalized.contains("عدن")) cityFilterId = "ye_ade"
        else if (qNormalized.contains("تعز")) cityFilterId = "ye_tai"
        else if (qNormalized.contains("الحديده")) cityFilterId = "ye_hod"

        // Search in categories to find target category id
        val matchedCategories = categories.filter { cat ->
            val normCat = normalizeArabic(cat.name)
            qNormalized.contains(normCat) || normCat.contains(qNormalized)
        }
        val matchedCatIds = matchedCategories.map { it.id }.toSet()

        // Filter providers based on category, name, profession, and city
        val matchedProviders = providers.filter { p ->
            val pNameNorm = normalizeArabic(p.name)
            val pProfNorm = normalizeArabic(p.profession)
            val pSpecNorm = normalizeArabic(p.specialization)
            
            val matchesSearch = pNameNorm.contains(qNormalized) || pProfNorm.contains(qNormalized) || 
                                pSpecNorm.contains(qNormalized) || qNormalized.contains(pNameNorm) ||
                                qNormalized.contains(pProfNorm) || matchedCatIds.contains(p.categoryId)

            val matchesCity = cityFilterId == null || p.cityId == cityFilterId
            
            matchesSearch && matchesCity && !p.isBlocked
        }.sortedWith(compareByDescending<com.example.data.ProviderEntity> { it.rating }.thenByDescending { it.isAvailable })

        if (matchedProviders.isNotEmpty()) {
            val sb = StringBuilder()
            val citySuffix = if (cityFilterId != null) " في ${if (cityFilterId == "ye_san") "صنعاء" else if (cityFilterId == "ye_ade") "عدن" else if (cityFilterId == "ye_tai") "تعز" else "الحديدة"}" else ""
            sb.append("🔍 لقد عثرت لك على الفنيين المعتمدين والمناسبين في دليل خدمات اليمن${citySuffix}:\n\n")
            
            matchedProviders.take(5).forEachIndexed { index, p ->
                val catName = categories.find { it.id == p.categoryId }?.name ?: p.profession.ifEmpty { "خدمة عامة" }
                val statusSymbol = if (p.isAvailable) "🟢 متاح الآن" else "🔴 مشغول حالياً"
                val vipBadge = if (p.isVip) " 🏆 [مميز VIP]" else ""
                
                sb.append("${index + 1}. الفني: *${p.name}*${vipBadge}\n")
                sb.append("   💼 التخصص: *${catName}* | *${p.specialization.ifEmpty { p.profession }}*\n")
                sb.append("   📱 رقم الهاتف: *${p.phone}*\n")
                sb.append("   📍 المنطقة والحي: *${p.area} - ${p.localNeighborhood}*\n")
                sb.append("   ⭐ التقييم: *${String.format(java.util.Locale.US, "%.1f", p.rating)} / 5.0* | الحالة: ${statusSymbol}\n\n")
            }
            sb.append("💡 يمكنك النقر على زر الاتصال المباشر بالفني أو مراسلته عبر الواتساب فوراً من واجهة التطبيق!")
            return Pair(sb.toString(), matchedProviders.take(5))
        } else {
            // Suggest alternatives
            val suggestedCats = categories.take(4).map { it.name }.joinToString("، ")
            return Pair("لم أعثر على فنيين نشطين بالاسم أو التخصص الدقيق المحدد في طلبك حالياً، ولكن الدليل يضم مئات السباكين والكهربائيين في صنعاء، عدن، تعز والحديدة. يمكنك مراجعة الأقسام الرئيسية في التطبيق مثل: (${suggestedCats}).", emptyList())
        }
    }

    // 4. Classify informational/conversational topics
    val textResult = when {
        qNormalized.contains("مرحبا") || qNormalized.contains("السلام") || qNormalized.contains("هلا") || 
        qNormalized.contains("اهلان") || qNormalized.contains("صباح") || qNormalized.contains("مساء") -> {
            "وعليكم السلام ورحمة الله وبركاته! مرحباً بك في دليل خدمات اليمن الذكي 🇾🇪. أنا مساعدك الرقمي الفوري، يسعدني تزويدك بأرقام وهواتف أفضل السباكين، الكهربائيين، الفنيين والخدمات بكل سهولة. كيف يمكنني مساعدتك اليوم؟"
        }
        isSupportContact -> {
            "📱 للدعم الفني المباشر واستفسارات منصة دليل خدمات اليمن:\n" +
            "- هاتف الدعم: *${settings.supportPhone}*\n" +
            "- واتساب الدعم: *${settings.supportWhatsapp}*\n" +
            "- البريد الإلكتروني: *${settings.supportEmail}*\n" +
            "نحن متواجدون لخدمتك في أي وقت لحل المشكلات وتوثيق الحسابات!"
        }
        isJoinRequest -> {
            "📝 للإنضمام كفني أو مقدم خدمة معتمد بالدليل، يرجى التوجه لـ 'استمارة طلب الانضمام' من القائمة الرئيسية، واملأ بياناتك بدقة (الاسم، الهاتف، التخصص، والحي السكني) ثم ارفع هويتك لطلب التوثيق الفوري من الإدارة!"
        }
        isPriceInfo -> {
            "💰 تطبيق دليل خدمات اليمن مجاني تماماً وبنسبة 100% لكافة المواطنين. لا نتقاضى أي عمولات أو رسوم إضافية على المكالمات أو الاتفاقات المباشرة بين العميل ومقدم الخدمة."
        }
        isMapFeature -> {
            "🗺️ يحتوي التطبيق على نظام رادار خرائط متكامل وعالي الدقة لتحديد أماكن الفنيين الأقرب جغرافياً إليك بالمتر. يمكنك تحديد موقعك الفعلي وضبط نطاق المسافة للعثور على أقرب الفنيين."
        }
        isCityInfo -> {
            "🇾🇪 يغطي دليل خدمات اليمن حالياً المحافظات والمدن الرئيسية: صنعاء (الأمانة)، عدن الباسلة، تعز الحالمة، والحديدة عروس البحر الأحمر، مع دعم التصفية الفورية حسب الأحياء والمربعات السكنية."
        }
        qNormalized.contains("ضمان") || qNormalized.contains("كيف اضمن") || qNormalized.contains("شعار") || 
        qNormalized.contains("موثق") || qNormalized.contains("امان") || qNormalized.contains("حمايه") -> {
            "🔒 جميع الفنيين المعتمدين يحملون الشارة الزرقاء بجانب أسمائهم، وهو ما يعني التحقق من هويتهم الشخصية وصحيفة أعمالهم المهنية بواسطة الإدارة لضمان حمايتكم جودة الأعمال."
        }
        else -> {
            "أهلاً بك في منصة دليل اليمن للخدمات الذكية! في وضع العمل المحلي والمساعد الذكي، يسرني تزويدك بكافة المعلومات حول كيفية التواصل مع الفنيين، تقديم استمارة طلب الانضمام بالهاتف والصوت، التتبع برادار الخرائط الجغرافي الدقيق، أو التواصل مباشرة مع إدارة الدعم المعتمدة على رقم ${settings.supportPhone}."
        }
    }
    return Pair(textResult, emptyList())
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

