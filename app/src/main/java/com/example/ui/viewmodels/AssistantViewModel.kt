package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.ui.*
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

    private val sharedHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

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
        var updatedHistory = _chatHistory.value + userMsg
        if (updatedHistory.size > 75) {
            updatedHistory = updatedHistory.takeLast(75)
        }
        _chatHistory.value = updatedHistory
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
                    val (localText, localEntities) = generateLocalOfflineResponse(prompt, mainViewModel)
                    AssistantMessage(text = localText, isUser = false, matchedEntities = localEntities)
                }

                withContext(Dispatchers.Main) {
                    var updatedHistoryWithResp = _chatHistory.value + responseMsg
                    if (updatedHistoryWithResp.size > 75) {
                        updatedHistoryWithResp = updatedHistoryWithResp.takeLast(75)
                    }
                    _chatHistory.value = updatedHistoryWithResp
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

    private val cachedResponses = mutableMapOf<String, AssistantMessage>()

    fun getCachedResponse(query: String): AssistantMessage? {
        val normalized = normalizeArabic(query)
        return cachedResponses[normalized]
    }

    fun cacheResponse(query: String, response: AssistantMessage) {
        val normalized = normalizeArabic(query)
        cachedResponses[normalized] = response
    }

    private fun queryGeminiApiOrFallback(
        prompt: String,
        providersList: List<ProviderEntity>,
        categoriesList: List<CategoryEntity>,
        settings: AdminSettingsEntity,
        history: List<AssistantMessage>,
        mainViewModel: MainViewModel
    ): AssistantMessage {
        val cached = getCachedResponse(prompt)
        if (cached != null) {
            return cached
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

                sharedHttpClient.newCall(request).execute().use { apiResponse ->
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
                            val response = AssistantMessage(text = textVal, isUser = false, matchedEntities = matched)
                            cacheResponse(prompt, response)
                            return response
                        }
                    } else {
                        com.example.utils.AppErrorLogManager.logApiError("GeminiAssistant", "HTTP Error: ${apiResponse.code} - ${apiResponse.message}")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            com.example.utils.AppErrorLogManager.logApiError("GeminiAssistant", "فشل استدعاء Gemini API: ${e.localizedMessage}", e)
        }

        val (localText, localEntities) = generateLocalOfflineResponse(prompt, mainViewModel)
        val response = AssistantMessage(text = localText, isUser = false, matchedEntities = localEntities)
        cacheResponse(prompt, response)
        return response
    }

    fun generateLocalOfflineResponse(prompt: String, viewModel: MainViewModel): Pair<String, List<Any>> {
        val qNormalized = normalizeArabic(prompt)
        val providers = viewModel.providers.value
        val stores = viewModel.stores.value
        val properties = viewModel.properties.value
        val categories = viewModel.categories.value
        val settings = viewModel.settings.value

        val isSupportContact = qNormalized.contains("رقم") || qNormalized.contains("اتصال") || 
                                qNormalized.contains("دعم") || qNormalized.contains("تواصل") || 
                                qNormalized.contains("واتساب")

        val isJoinRequest = qNormalized.contains("تسجيل") || qNormalized.contains("انضم") || qNormalized.contains("حساب")
        val isPriceInfo = qNormalized.contains("سعر") || qNormalized.contains("رسوم") || qNormalized.contains("مجاني")
        val isMapFeature = qNormalized.contains("خريطه") || qNormalized.contains("موقع") || qNormalized.contains("gps")

        val matchedEntities = mutableListOf<Any>()

        // 1. Search Providers
        val professions = listOf("سباك", "كهربا", "دهان", "نجار", "حداد", "خياط", "سائق", "مصلح", "صيانه", "فني", "مهندس", "تكييف", "تبريد", "بناء", "مقاول", "طبيب", "تنظيف", "ميكانيك", "غساله", "غسالات")
        val isProviderSearch = professions.any { qNormalized.contains(it) } || qNormalized.contains("فني")
        val isWashingMachinePrice = (qNormalized.contains("غساله") || qNormalized.contains("غسالات")) && (qNormalized.contains("سعر") || qNormalized.contains("كم") || qNormalized.contains("تكلفه") || qNormalized.contains("صيانه"))

        if (isProviderSearch) {
            val provs = providers.filter { p ->
                val pNameNorm = normalizeArabic(p.name)
                val pProfNorm = normalizeArabic(p.profession)
                val pSpecNorm = normalizeArabic(p.specialization)
                pNameNorm.contains(qNormalized) || (pProfNorm.isNotBlank() && pProfNorm.contains(qNormalized)) || (pSpecNorm.isNotBlank() && pSpecNorm.contains(qNormalized)) || (pProfNorm.isNotBlank() && qNormalized.contains(pProfNorm)) ||
                    (qNormalized.contains("سباك") && (pProfNorm.contains("سباك") || pSpecNorm.contains("سباك")))
            }.take(3)
            matchedEntities.addAll(provs)
        }

        // 2. Search Stores (Restaurants, Medical, Stores)
        val storeKeywords = listOf("مطعم", "اكل", "غداء", "عشاء", "كافيه", "قهوه", "محل", "متجر", "سوبر", "صيدليه", "مستشفى", "عياده", "طبي")
        if (storeKeywords.any { qNormalized.contains(it) } || qNormalized.contains("شراء")) {
            val ms = stores.filter { s ->
                val sNameNorm = normalizeArabic(s.name)
                val sDescNorm = normalizeArabic(s.description)
                sNameNorm.contains(qNormalized) || sDescNorm.contains(qNormalized)
            }.take(3)
            matchedEntities.addAll(ms)
        }

        // 3. Search Properties
        if (qNormalized.contains("بيت") || qNormalized.contains("شقه") || qNormalized.contains("عقار") || qNormalized.contains("ايجار") || qNormalized.contains("بيع")) {
            val mp = properties.filter { pr ->
                val prTitleNorm = normalizeArabic(pr.title)
                val prDescNorm = normalizeArabic(pr.description)
                prTitleNorm.contains(qNormalized) || prDescNorm.contains(qNormalized)
            }.take(3)
            matchedEntities.addAll(mp)
        }

        if (isWashingMachinePrice) {
            val priceText = "🧺 متوسط تكلفة معاينة وصيانة الغسالات في دليل خدمات اليمن يتراوح بين 4,000 إلى 9,000 ريال يمني (حسب نوع الغسالة والعطل وقطع الغيار). يمكنك طلب فني صيانة غسالات معتمد للمعاينة المباشرة!"
            return Pair(priceText, matchedEntities)
        }

        if (matchedEntities.isNotEmpty()) {
            val sb = StringBuilder()
            sb.append("🔍 عثرت لك على النتائج التالية في دليل خدمات اليمن:\n\n")
            matchedEntities.forEachIndexed { index, ent ->
                when (ent) {
                    is com.example.data.ProviderEntity -> {
                        val catName = categories.find { it.id == ent.categoryId }?.name ?: ent.profession
                        sb.append("${index + 1}. *${ent.name}* (فني) | $catName\n")
                        sb.append("   📍 ${ent.area} | ⭐ ${ent.rating} | ${if (ent.isAvailable) "🟢 متاح" else "🔴 مشغول"}\n\n")
                    }
                    is com.example.data.StoreEntity -> {
                        sb.append("${index + 1}. *${ent.name}* (متجر/مطعم)\n")
                        sb.append("   📍 ${ent.localNeighborhood} | ⭐ ${ent.rating}\n\n")
                    }
                    is com.example.data.PropertyEntity -> {
                        sb.append("${index + 1}. *${ent.title}* (عقار)\n")
                        sb.append("   📍 ${ent.localNeighborhood} | 💰 ${ent.price} ${ent.currency}\n\n")
                    }
                }
            }
            sb.append("💡 انقر على البطاقة بالأسفل لمزيد من التفاصيل!")
            return Pair(sb.toString(), matchedEntities)
        }

        val textResult = when {
            qNormalized.contains("مرحبا") || qNormalized.contains("السلام") || qNormalized.contains("هلا") || qNormalized.contains("صباح") || qNormalized.contains("مساء") -> {
                "أهلاً وسهلاً بك في دليل خدمات اليمن 🇾🇪! أنا مساعدك الذكي المدمج لمساعدتك في الوصول لأفضل الفنيين، المحلات، العيادات، والمطاعم في ثوانٍ معدودة. كيف يمكنني مساعدتك اليوم؟"
            }
            isSupportContact -> {
                "📱 للتواصل المباشر والآمن مع الدعم الفني للدليل:\n- هاتف الإدارة: *${settings.supportPhone}*\n- واتساب الدعم الفني المباشر: *${settings.supportWhatsapp}*\nنحن هنا لخدمتك على مدار الساعة!"
            }
            isJoinRequest -> {
                "📝 للانضمام كفني محترف أو صاحب متجر/خدمة في الدليل وعرض خدماتك مجاناً، يرجى فتح القائمة الجانبية للتطبيق واختيار 'طلب الانضمام كفني' وتعبئة البيانات!"
            }
            isPriceInfo || qNormalized.contains("سعر سباك") || qNormalized.contains("تكلفه") -> {
                "💰 استخدام تطبيق دليل خدمات اليمن مجاني تماماً للبحث وتصفح الفنيين والمحلات. أسعار المعاينة والصيانة تبدأ من 3,000 ريال يمني وتحدد التكلفة الإجمالية للعمل بالتراضي المباشر بينك وبين مقدم الخدمة بكل شفافية."
            }
            isMapFeature || qNormalized.contains("رادار") -> {
                "🗺️ يمكنك النقر على 'خريطة الخدمات' أو 'الرادار التفاعلي' لعرض مواقع مقدمي الخدمات القريبين منك جغرافياً في حيك ومحافظتك دون أي حاجة للإنترنت!"
            }
            qNormalized.contains("عدن") || qNormalized.contains("صنعاء") || qNormalized.contains("تعز") || qNormalized.contains("حضرموت") || qNormalized.contains("الحديده") || qNormalized.contains("اب") || qNormalized.contains("مارب") || qNormalized.contains("ذمار") -> {
                "🇾🇪 دليل خدمات اليمن يغطي جميع المحافظات والمناطق اليمنية الرئيسية (صنعاء، عدن، تعز، حضرموت، إب، الحديدة، مأرب، ذمار، وغيرها) لتسهيل العثور على أقرب فني ومحل معتمد في منطقتك السكنية!"
            }
            qNormalized.contains("طوارئ") || qNormalized.contains("اسعاف") || qNormalized.contains("دفاع") || qNormalized.contains("حريق") || qNormalized.contains("شرطه") -> {
                "🚨 أرقام الطوارئ العامة الهامة في اليمن:\n- الدفاع المدني والإطفاء: *191*\n- طوارئ الكهرباء: *195*\n- طوارئ المياه والصرف الصحي: *192*\n- إسعاف الهلال الأحمر والنجدة: *199*\n\nيرجى التواصل المباشر مع هذه الجهات الرسمية فوراً في حالات الطوارئ الحيوية!"
            }
            qNormalized.contains("دفع") || qNormalized.contains("كاش") || qNormalized.contains("فلوس") || qNormalized.contains("محفظه") -> {
                "💳 يتيح تطبيقنا الدفع نقداً (كاش) مباشرة للفني، أو عبر المحافظ الإلكترونية اليمنية المعتمدة (مثل حاسب، جوال بي، كاش، الكريمي، وغيرها) بالتنسيق المباشر مع مقدم الخدمة."
            }
            qNormalized.contains("تقييم") || qNormalized.contains("شكوى") || qNormalized.contains("شكاوى") || qNormalized.contains("سيء") -> {
                "⭐ نحن نهتم بجودة الخدمات جداً! يمكنك تقييم الفني بنجوم وتوضيح تجربتك بعد إتمام الطلب مباشرة من شاشة الحجوزات، وإذا واجهتك أي مشكلة تواصل معنا فوراً عبر زر الدعم الفني وسنتعامل مع الشكوى بحسم!"
            }
            isProviderSearch -> {
                "🔧 طلبك واضح! يمكنك تصفح قسم الفنيين المختصين في التطبيق أو الضغط على زر '⚡ اطلب الآن' لإرسال طلب فوري وسريع لأقرب فني معتمد في حارتك."
            }
            else -> {
                "عذراً، لم أتمكن من فهم استفسارك بدقة (\"$prompt\"). يمكنك كتابة اسم المهنة أو الخدمة المطلوبة بوضوح مثل: (أحتاج سباك، كهربائي، صيانة غسالة، مطعم، عقار، دكتور) أو الضغط على زر '⚡ اطلب الآن' لإرسال طلب فوري."
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

    override fun onCleared() {
        super.onCleared()
        cachedResponses.clear()
    }
}
