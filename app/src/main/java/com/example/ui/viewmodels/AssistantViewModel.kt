package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AdminSettingsEntity
import com.example.data.CategoryEntity
import com.example.data.ProviderEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.assistant.AssistantMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 🤖 AssistantViewModel
 * إدارة المحادثة والمنطق البرمجي للمساعد الذكي لدليل خدمات اليمن.
 * يدعم الذكاء الاصطناعي التوليدي عبر Gemini API ومحرك الاستجابة المحلي بدون إنترنت.
 */
class AssistantViewModel : ViewModel() {

    private val defaultWelcome = "مرحباً بك في المساعد الذكي لدليل خدمات اليمن 🇾🇪! كيف يمكنني خدمتك ومساعدتك اليوم؟"

    private val _chatHistory = MutableStateFlow<List<AssistantMessage>>(
        listOf(AssistantMessage(text = defaultWelcome, isUser = false))
    )
    val chatHistory: StateFlow<List<AssistantMessage>> = _chatHistory.asStateFlow()

    private val _typedText = MutableStateFlow("")
    val typedText: StateFlow<String> = _typedText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    fun updateTypedText(text: String) {
        _typedText.value = text
    }

    fun clearChat(welcomeMsg: String = defaultWelcome) {
        _chatHistory.value = listOf(AssistantMessage(text = welcomeMsg.ifEmpty { defaultWelcome }, isUser = false))
    }

    /**
     * معالجة استفسار المستخدم وتوليد الرد.
     */
    fun sendUserQuery(
        prompt: String,
        isOnline: Boolean,
        mainViewModel: MainViewModel,
        settings: AdminSettingsEntity,
        onSpeechSpeak: ((String) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (prompt.isBlank() || _isGenerating.value) return

        val userMsg = AssistantMessage(text = prompt.trim(), isUser = true)
        _chatHistory.value = _chatHistory.value + userMsg
        _typedText.value = ""
        _isGenerating.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val providersList = mainViewModel.providers.value
                val categoriesList = mainViewModel.categories.value

                val responseMsg = if (isOnline) {
                    queryGeminiApiOrFallback(
                        prompt = prompt,
                        providersList = providersList,
                        categoriesList = categoriesList,
                        settings = settings,
                        history = _chatHistory.value,
                        mainViewModel = mainViewModel
                    )
                } else {
                    val (localText, localProvs) = generateLocalOfflineResponse(prompt, mainViewModel)
                    AssistantMessage(text = localText, isUser = false, matchedProviders = localProvs)
                }

                withContext(Dispatchers.Main) {
                    _chatHistory.value = _chatHistory.value + responseMsg
                    _isGenerating.value = false
                    if (settings.allowTextToSpeechAssistant) {
                        onSpeechSpeak?.invoke(responseMsg.text)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    val errMsg = "حدث خطأ: ${e.localizedMessage}"
                    onError?.invoke(errMsg)
                }
            }
        }
    }

    private fun queryGeminiApiOrFallback(
        prompt: String,
        providersList: List<ProviderEntity>,
        categoriesList: List<CategoryEntity>,
        settings: AdminSettingsEntity,
        history: List<AssistantMessage>,
        mainViewModel: MainViewModel
    ): AssistantMessage {
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
            catNameNorm.contains(qNormalized)
        }.take(5)

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

                val contentsArray = JSONArray()
                history.takeLast(6).forEach { hMsg ->
                    val contentObj = JSONObject()
                    contentObj.put("role", if (hMsg.isUser) "user" else "model")
                    val partsArray = JSONArray()
                    val partObj = JSONObject()
                    partObj.put("text", hMsg.text)
                    partsArray.put(partObj)
                    contentObj.put("parts", partsArray)
                    contentsArray.put(contentObj)
                }

                val systemInstructionObj = JSONObject()
                val sysParts = JSONArray()
                val sysPart = JSONObject()
                sysPart.put("text", systemInstructionText)
                sysParts.put(sysPart)
                systemInstructionObj.put("parts", sysParts)

                val finalRequestJsonObj = JSONObject()
                finalRequestJsonObj.put("contents", contentsArray)
                finalRequestJsonObj.put("systemInstruction", systemInstructionObj)

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                    .post(RequestBody.create(mediaType, finalRequestJsonObj.toString()))
                    .build()

                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()

                val apiResponse = okHttpClient.newCall(request).execute()
                if (apiResponse.isSuccessful) {
                    val bodyString = apiResponse.body?.string() ?: ""
                    val jsonObject = JSONObject(bodyString)
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
            // Fallback
        }

        val (localText, localProvs) = generateLocalOfflineResponse(prompt, mainViewModel)
        return AssistantMessage(text = localText, isUser = false, matchedProviders = localProvs)
    }

    fun generateLocalOfflineResponse(prompt: String, viewModel: MainViewModel): Pair<String, List<ProviderEntity>> {
        val qNormalized = normalizeArabic(prompt)
        val providers = viewModel.providers.value
        val categories = viewModel.categories.value
        val settings = viewModel.settings.value

        val isSupportContact = qNormalized.contains("رقم") || qNormalized.contains("اتصال") || 
                               qNormalized.contains("دعم") || qNormalized.contains("تواصل") || 
                               qNormalized.contains("واتساب")

        val isJoinRequest = qNormalized.contains("تسجيل") || qNormalized.contains("انضم") || qNormalized.contains("حساب")
        val isPriceInfo = qNormalized.contains("سعر") || qNormalized.contains("رسوم") || qNormalized.contains("مجاني")
        val isMapFeature = qNormalized.contains("خريطه") || qNormalized.contains("موقع") || qNormalized.contains("gps")

        val professions = listOf(
            "سباك", "كهربا", "دهان", "نجار", "حداد", "خياط", "سائق", "مصلح", "صيانه", "فني", 
            "مهندس", "تكييف", "تبريد", "بناء", "مقاول", "طبيب", "تنظيف", "ميكانيك"
        )
        val hasProfessionKeyword = professions.any { qNormalized.contains(it) }
        val isProviderSearch = hasProfessionKeyword || qNormalized.contains("ابحث") || 
                               providers.any { normalizeArabic(it.name).contains(qNormalized) }

        if (isProviderSearch) {
            val matchedProviders = providers.filter { p ->
                val pNameNorm = normalizeArabic(p.name)
                val pProfNorm = normalizeArabic(p.profession)
                val pSpecNorm = normalizeArabic(p.specialization)
                pNameNorm.contains(qNormalized) || pProfNorm.contains(qNormalized) || 
                pSpecNorm.contains(qNormalized) || qNormalized.contains(pProfNorm)
            }.take(5)

            if (matchedProviders.isNotEmpty()) {
                val sb = StringBuilder()
                sb.append("🔍 عثرت لك على الفنيين المعتمدين في دليل خدمات اليمن:\n\n")
                matchedProviders.forEachIndexed { index, p ->
                    val catName = categories.find { it.id == p.categoryId }?.name ?: p.profession
                    val statusSymbol = if (p.isAvailable) "🟢 متاح" else "🔴 مشغول"
                    sb.append("${index + 1}. *${p.name}* | $catName\n")
                    sb.append("   📱 ${p.phone} | 📍 ${p.area} | ⭐ ${p.rating} | $statusSymbol\n\n")
                }
                sb.append("💡 يمكنك النقر على بطاقة الفني بالأسفل للاتصال به فوراً!")
                return Pair(sb.toString(), matchedProviders)
            }
        }

        val textResult = when {
            qNormalized.contains("مرحبا") || qNormalized.contains("السلام") || qNormalized.contains("هلا") -> {
                "أهلاً وسهلاً بك في دليل خدمات اليمن 🇾🇪! أنا مساعدك الذكي لمساعدتك في الوصول لأفضل الفنيين والخدمات."
            }
            isSupportContact -> {
                "📱 للتواصل المباشر مع الدعم الفني:\n- هاتف: *${settings.supportPhone}*\n- واتساب: *${settings.supportWhatsapp}*"
            }
            isJoinRequest -> {
                "📝 للانضمام كفني أو متجر في الدليل، استخدم شاشة 'طلب الانضمام' في القائمة الرئيسية."
            }
            isPriceInfo -> {
                "💰 استخدام تطبيق دليل خدمات اليمن مجاني تماماً وبدون أي عمولات."
            }
            isMapFeature -> {
                "🗺️ يمكنك النقر على 'خريطة الخدمات' لعرض التغطية الجغرافية والفنيين الأقرب لك."
            }
            else -> {
                "أهلاً بك! يمكنك سؤالي عن أي خدمة أو فني (كهرباء، سباكة، تكييف) أو الضغط على '⚡ اطلب خدمتك الآن'."
            }
        }
        return Pair(textResult, emptyList())
    }

    fun normalizeArabic(text: String): String {
        var str = text.trim().lowercase(Locale.ROOT)
        str = str.replace(Regex("[\\u064B-\\u0652]"), "")
        str = str.replace(Regex("[أإآ]"), "ا")
        str = str.replace("ى", "ي")
        str = str.replace("ة", "ه")
        return str
    }
}
