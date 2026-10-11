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
            .connectTimeout(3500, TimeUnit.MILLISECONDS)
            .readTimeout(4000, TimeUnit.MILLISECONDS)
            .writeTimeout(3500, TimeUnit.MILLISECONDS)
            .build()
    }

    fun updateTypedText(text: String) {
        _typedText.value = text
    }

    fun clearChat(welcomeMsg: String = defaultWelcome) {
        _chatHistory.value = listOf(AssistantMessage(text = welcomeMsg.ifEmpty { defaultWelcome }, isUser = false))
    }

    /**
     * معالجة استفسار المستخدم وتوليد الرد بسرعة فائقة وفهم اللهجات اليمنية المتعددة
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

        val cleanPrompt = prompt.trim()
        val userMsg = AssistantMessage(text = cleanPrompt, isUser = true)
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

                // محاولة الاتصال بالذكاء التوليدي السريع مع الرجوع التلقائي والفوري للأوفلاين الذكي في حال البطء أو انقطاع النت
                val responseMsg = if (isOnline) {
                    try {
                        val onlineResult = kotlinx.coroutines.withTimeoutOrNull(4000L) {
                            queryGeminiApiOrFallback(
                                prompt = cleanPrompt,
                                providersList = providersList,
                                categoriesList = categoriesList,
                                settings = settings,
                                history = _chatHistory.value,
                                mainViewModel = mainViewModel
                            )
                        }
                        onlineResult ?: run {
                            val (localText, localEntities) = generateLocalOfflineResponse(cleanPrompt, mainViewModel)
                            AssistantMessage(text = localText, isUser = false, matchedEntities = localEntities)
                        }
                    } catch (_: Exception) {
                        val (localText, localEntities) = generateLocalOfflineResponse(cleanPrompt, mainViewModel)
                        AssistantMessage(text = localText, isUser = false, matchedEntities = localEntities)
                    }
                } else {
                    val (localText, localEntities) = generateLocalOfflineResponse(cleanPrompt, mainViewModel)
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
                    // حتى لو حدث أي خطأ غير متوقع، نولد إجابة أوفلاين فورية ومبهرة بدلاً من ترك المستخدم معلقاً
                    val (fallbackText, fallbackEntities) = generateLocalOfflineResponse(cleanPrompt, mainViewModel)
                    val fallbackMsg = AssistantMessage(text = fallbackText, isUser = false, matchedEntities = fallbackEntities)
                    _chatHistory.value = _chatHistory.value + fallbackMsg
                    _isGenerating.value = false
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
                dbContextText.append("بيانات شبكة دليل خدمات اليمن (RAG Context):\n")
                ragList.forEach { p ->
                    val catName = categoriesList.find { it.id == p.categoryId }?.name ?: "خدمة عامة"
                    dbContextText.append("- الفني: ${p.name} | التخصص: ${p.specialization.ifEmpty { p.profession }} | المنطقة: ${p.area} ${p.localNeighborhood} | الهاتف: ${p.phone} | التقييم: ${p.rating}⭐ | الحالة: ${if (p.isAvailable) "متاح" else "مشغول"}\n")
                }
                dbContextText.append("\nالدعم الفني والخدمات العامة:\n")
                dbContextText.append("- هاتف الدعم: ${settings.supportPhone} | واتساب: ${settings.supportWhatsapp}\n")

                val systemInstructionText = """
                    أنت 'المساعد الذكي لدليل خدمات اليمن 🇾🇪' - نموذج ذكاء اصطناعي فائق الذكاء، سريع ومجاني بالكامل، ينافس ويتفوق على أفضل النماذج العالمية (مثل كلود وديب سيك وجروك).
                    تتميز بالسمات الجوهرية التالية:
                    1. تتقن وتتحدث بجميع اللهجات اليمنية الأصيلة (الصنعانية، العدنية، التعزية، الحضرمية، التهامية، الإبية، وغيرها) وتجمع بين الفصاحة الذكية والروح اليمنية الدافئة المضيافة ("يا حيا ومسهلا بك يا خبير"، "أرحب تراحيب السيل"، "أبشر بسعدك وعزك"، "على عيني ورأسي يا غالي").
                    2. خبير وموسوعة عملية في حل وتفصيل أعطال الصيانة المنزلية (الكهرباء، الطاقة الشمسية، الإنفرترات وأكواد الأعطال، البطاريات، الغسالات، السباكة ومواطير المياه، التكييف والتبريد، تسريب الغاز، بنشر وميكانيك السيارات، وعوازل الأمطار).
                    3. تقدم خطوات تشخيص وإصلاح ذاتية عملية خطوة بخطوة (DIY) مع نصائح الأمان والوقاية، وتذكر التكلفة التقديرية بالريال اليمني لكل خدمة.
                    4. خبير بالجغرافيا اليمنية والمعالم والمدن والأحياء والمطاعم والأكلات الشعبية التراثية (سلتة، فحسة، زربيان، مندي، شاي عدني...).
                    5. تقدم إجابات حكيمة، بليغة، مفيدة، منسقة بنقاط واضحة وأيقونات جذابة دون حشو أو تعقيد.
                    6. ترشح الفنيين والمحلات المناسبة من السياق المتاح أدناه عند وجود استفسار عن خدمة محددة.

                    بيانات شبكة دليل خدمات اليمن المتاحة:
                    ${'$'}dbContextText
                """.trimIndent()

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

                // Generation Config for rapid responses without thinking delay
                val genConfigObj = JSONObject()
                genConfigObj.put("temperature", 0.7)
                genConfigObj.put("maxOutputTokens", 800)
                val thinkingConfigObj = JSONObject()
                thinkingConfigObj.put("thinkingBudget", 0)
                genConfigObj.put("thinkingConfig", thinkingConfigObj)

                val finalRequestJsonObj = JSONObject()
                finalRequestJsonObj.put("contents", contentsArray)
                finalRequestJsonObj.put("systemInstruction", systemInstructionObj)
                finalRequestJsonObj.put("generationConfig", genConfigObj)

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
            com.example.utils.AppErrorLogManager.logApiError("GeminiAssistant", "فشل استدعاء Gemini API: ${e.localizedMessage}", e)
        }

        val (localText, localEntities) = generateLocalOfflineResponse(prompt, mainViewModel)
        val response = AssistantMessage(text = localText, isUser = false, matchedEntities = localEntities)
        cacheResponse(prompt, response)
        return response
    }

    /**
     * محرك الذكاء الاصطناعي الأوفلاين الفائق (Local Offline Intelligence Engine)
     * يعمل 100% بدون إنترنت ويقدم تشخيصاً كاملاً، خطوات DIY، أسعار، وفهم شامل للهجات اليمنية
     */
    fun generateLocalOfflineResponse(prompt: String, viewModel: MainViewModel): Pair<String, List<Any>> {
        val qNormalized = normalizeArabic(prompt)
        val providers = viewModel.providers.value
        val stores = viewModel.stores.value
        val properties = viewModel.properties.value
        val categories = viewModel.categories.value
        val settings = viewModel.settings.value
        val selectedCity = viewModel.selectedCity.value.ifEmpty { "صنعاء" }

        val matchedEntities = mutableListOf<Any>()

        // 1. البحث عن فنيين مطابقين
        val matchedProviders = providers.filter { p ->
            val catName = categories.find { it.id == p.categoryId }?.name ?: ""
            val pNameNorm = normalizeArabic(p.name)
            val pProfNorm = normalizeArabic(p.profession)
            val pSpecNorm = normalizeArabic(p.specialization)
            val pAreaNorm = normalizeArabic(p.area)
            val catNameNorm = normalizeArabic(catName)

            pNameNorm.contains(qNormalized) ||
            (pProfNorm.isNotBlank() && (pProfNorm.contains(qNormalized) || qNormalized.contains(pProfNorm))) ||
            (pSpecNorm.isNotBlank() && (pSpecNorm.contains(qNormalized) || qNormalized.contains(pSpecNorm))) ||
            (pAreaNorm.isNotBlank() && qNormalized.contains(pAreaNorm)) ||
            (catNameNorm.isNotBlank() && qNormalized.contains(catNameNorm))
        }.take(3)
        matchedEntities.addAll(matchedProviders)

        // 2. البحث عن محلات ومراكز ومطاعم
        val storeKeywords = listOf("مطعم", "اكل", "غداء", "عشاء", "كافيه", "قهوه", "محل", "متجر", "سوبر", "صيدليه", "مستشفى", "عياده", "طبي", "سلته", "فحسه", "مندي", "زربيان")
        if (storeKeywords.any { qNormalized.contains(it) } || qNormalized.contains("شراء")) {
            val ms = stores.filter { s ->
                val sNameNorm = normalizeArabic(s.name)
                val sDescNorm = normalizeArabic(s.description)
                sNameNorm.contains(qNormalized) || sDescNorm.contains(qNormalized)
            }.take(3)
            matchedEntities.addAll(ms)
        }

        // 3. البحث عن عقارات
        if (qNormalized.contains("بيت") || qNormalized.contains("شقه") || qNormalized.contains("عقار") || qNormalized.contains("ايجار") || qNormalized.contains("بيع")) {
            val mp = properties.filter { pr ->
                val prTitleNorm = normalizeArabic(pr.title)
                val prDescNorm = normalizeArabic(pr.description)
                prTitleNorm.contains(qNormalized) || prDescNorm.contains(qNormalized)
            }.take(3)
            matchedEntities.addAll(mp)
        }

        // 4. استدعاء محرك الذكاء الأوفلاين الشامل (AiAssistantEngine)
        val appCtx = com.example.MyApplication.instance
        val engineResp = if (appCtx != null) {
            com.example.utils.AiAssistantEngine.getInstance(appCtx).queryAssistantSync(prompt, selectedCity)
        } else {
            null
        }

        val sb = StringBuilder()

        if (engineResp != null && engineResp.diySteps.isNotEmpty()) {
            sb.append(engineResp.title).append("\n\n")
            sb.append(engineResp.message).append("\n\n")
            sb.append("🛠️ *خطوات الفحص والإصلاح (DIY):*\n")
            engineResp.diySteps.forEachIndexed { i, step ->
                sb.append("${i + 1}. $step\n")
            }
            sb.append("\n")
            if (!engineResp.preventiveTips.isNullOrBlank()) {
                sb.append("⚠️ *نصيحة وقائية:*\n").append(engineResp.preventiveTips).append("\n\n")
            }
            if (!engineResp.estimatedCost.isNullOrBlank()) {
                sb.append("💰 *متوسط التكلفة التقديرية باليمن:* ").append(engineResp.estimatedCost).append("\n\n")
            }
        } else if (engineResp != null && engineResp.message.isNotBlank() && !engineResp.title.contains("المساعد الذكي لدليل اليمن")) {
            sb.append(engineResp.title).append("\n\n")
            sb.append(engineResp.message).append("\n\n")
        } else {
            // محرك اللهجات والمعلومات الإضافي المتقدم
            val fallbackText = generateDeepYemeniDialectResponse(qNormalized, prompt, selectedCity, settings)
            sb.append(fallbackText).append("\n\n")
        }

        if (matchedEntities.isNotEmpty()) {
            sb.append("👇 *عثرت لك على فنيين ومحلات معتمدة في الدليل لمساعدتك مباشرة:*")
        }

        return Pair(sb.toString().trim(), matchedEntities)
    }

    private fun generateDeepYemeniDialectResponse(
        qNorm: String,
        prompt: String,
        city: String,
        settings: AdminSettingsEntity
    ): String {
        val isYemeniGreeting = qNorm.contains("يا حيا") || qNorm.contains("حياك") || qNorm.contains("حياكم") || 
                               qNorm.contains("كيفك") || qNorm.contains("كيف حالك") || qNorm.contains("مسيك") || 
                               qNorm.contains("صبحك") || qNorm.contains("ارحب") || qNorm.contains("هلا والله") ||
                               qNorm.contains("مرحبا") || qNorm.contains("السلام عليكم")

        val isSupport = qNorm.contains("رقم") || qNorm.contains("اتصال") || qNorm.contains("دعم") || qNorm.contains("تواصل") || qNorm.contains("واتساب")
        val isJoin = qNorm.contains("تسجيل") || qNorm.contains("انضم") || qNorm.contains("حساب")
        val isPricing = qNorm.contains("سعر") || qNorm.contains("رسوم") || qNorm.contains("مجاني") || qNorm.contains("كم يكلف") || qNorm.contains("اجور")
        val isMap = qNorm.contains("خريطه") || qNorm.contains("موقع") || qNorm.contains("gps") || qNorm.contains("رادار")

        return when {
            isYemeniGreeting -> {
                "يا حيا ومسهلا بك يا خبير في دليل خدمات اليمن 🇾🇪!\n" +
                "أنا مساعدك الذكي المدمج والمجاني بالكامل، أعمل معك بسرعة خارقة حتى بدون إنترنت. تشتي سباك، كهربائي، مهندس طاقة شمسية، مكيفات، غسالات، سيارات، أو استفسار عن مطاعم وعقارات؟ أطلب وتدلل، كل شيء بين يدك!"
            }
            qNorm.contains("شمسيه") || qNorm.contains("الواح") || qNorm.contains("انفرتر") || qNorm.contains("انفيرتر") || qNorm.contains("بطاريه") || qNorm.contains("كنداكتر") || qNorm.contains("مافيش كهربا") || qNorm.contains("طفى") || qNorm.contains("شورت") -> {
                "⚡ *تشخيص منظومة الطاقة الشمسية والكهرباء (أوفلاين):*\n" +
                "1. افحص شاشة الإنفرتر وشاهد كود الخطأ (Fault Code).\n" +
                "2. تأكد من سلامة كابلات البطاريات وعدم وجود أملاح أو أسلاك متأكسدة.\n" +
                "3. إذا كان القاطع ينزل باستمرار، افصل الأحمال الثقيلة وافحص وجود شورت (التماس).\n" +
                "4. تأكد من نظافة الألواح من الغبار لضمان كفاءة الشحن.\n\n" +
                "💰 *متوسط تكلفة فحص المنظومة في اليمن:* 3,500 - 8,000 ريال يمني."
            }
            qNorm.contains("تسريب") || qNorm.contains("ماصوره") || qNorm.contains("تهريب") || qNorm.contains("دينمو") || qNorm.contains("غطاس") || qNorm.contains("بزبوز") || qNorm.contains("حنفيه") || qNorm.contains("مغسله") || qNorm.contains("مجاري") || qNorm.contains("ماطور") -> {
                "🔧 *إرشادات طوارئ السباكة ومواطير المياه (أوفلاين):*\n" +
                "1. أغلق المحبس (السكّر) الرئيسي للشقة أو الخزان فوراً لوقف تدفق الماء.\n" +
                "2. إذا كان الدينمو يدور بدون رفع ماء، قم بتنفيس برغي الهواء العلوي وسكب ماء نظيف.\n" +
                "3. إذا كان التسريب من صمام أو بزبوز، جرب وضع شريط تفلون (Teflon Tape) مؤقتاً.\n" +
                "4. في حال انسداد المجاري أو الصفاية، استخدم ماء حار مع كربونات وأسياخ تسليك مرنة.\n\n" +
                "💰 *متوسط تكلفة الصيانة:* 2,500 - 6,000 ريال يمني."
            }
            qNorm.contains("مكيف") || qNorm.contains("تبريد") || qNorm.contains("ما يبردش") || qNorm.contains("حار") || qNorm.contains("فريون") || qNorm.contains("سبليت") || qNorm.contains("ينقط ماء") || qNorm.contains("ثلج") -> {
                "❄️ *تشخيص أعطال التكييف والتبريد (أوفلاين):*\n" +
                "1. تأكد من ضبط الريموت على وضع (Cool ❄️) وحرارة 22-24.\n" +
                "2. فك فلاتر الهواء الأمامية واغسلها من الأتربة وجففها.\n" +
                "3. افحص خرطوم تصريف الماء من الانسداد.\n" +
                "4. إذا كان يخرج هواء حار فقط، فالمشكلة غالباً في غاز الفريون أو كابستر الكمبروسر.\n\n" +
                "💰 *متوسط تكلفة الشحن والصيانة:* 5,000 - 12,000 ريال يمني."
            }
            qNorm.contains("بنشر") || qNorm.contains("سياره") || qNorm.contains("تاير") || qNorm.contains("كفر") || qNorm.contains("سلف") || qNorm.contains("ما تدقش") || qNorm.contains("سطحه") || qNorm.contains("طرمبه") || qNorm.contains("عطلان بالشارع") -> {
                "🚗 *طوارئ وبنشر وميكانيك السيارات (أوفلاين):*\n" +
                "1. شغل الفلشر الرباعي وثبت السيارة بالهاند بريك وحجر خلف الإطار.\n" +
                "2. إذا كانت البطارية نائمة وما تدقش سلف، استخدم كابل اشتراك مع سيارة أخرى.\n" +
                "3. يمكنك طلب بنشر متنقل أو سطحة فورية من خلال الدليل ليصلك أينما كنت في $city!\n\n" +
                "💰 *متوسط تكلفة الرقع والاشتراك:* 2,000 - 5,000 ريال يمني."
            }
            qNorm.contains("غساله") || qNorm.contains("غسالات") -> {
                "🧺 *تشخيص وصيانة الغسالات المنزلية (أوفلاين):*\n" +
                "1. نظف فلتر طرمبة التصريف السفلي وتأكد من خلوه من العملات أو الخيوط العالقة.\n" +
                "2. افحص خرطوم الصرف وتأكد من عدم التوائه.\n" +
                "3. تأكد من اتزان أرجل الغسالة لمنع الاهتزاز والضجيج أثناء التنشيف.\n\n" +
                "💰 *متوسط تكلفة صيانة الغسالات:* 4,000 - 9,000 ريال يمني."
            }
            isPricing -> {
                "💰 *دليل الأسعار الاسترشادية للخدمات في اليمن:*\n" +
                "استخدام تطبيق دليل خدمات اليمن مجاني تماماً للبحث وتصفح الفنيين والمحلات. أسعار المعاينة والصيانة تبدأ من 2,500 ريال يمني وتحدد التكلفة الإجمالية للعمل بالتراضي المباشر بينك وبين مقدم الخدمة بكل شفافية."
            }
            isSupport -> {
                "📱 *للتواصل المباشر مع إدارة ودعم دليل خدمات اليمن:*\n" +
                "• هاتف الإدارة: *${settings.supportPhone}*\n" +
                "• واتساب الدعم الفني: *${settings.supportWhatsapp}*\n" +
                "نحن في خدمتك على مدار الساعة!"
            }
            isJoin -> {
                "📝 *الانضمام كفني محترف أو صاحب متجر/خدمة:*\n" +
                "يمكنك عرض خدماتك مجاناً والوصول لآلاف الزبائن؛ افتح القائمة الجانبية للتطبيق واضغط على 'طلب الانضمام كفني' وسجل بياناتك في دقيقة واحدة!"
            }
            isMap -> {
                "🗺️ يمكنك النقر على 'خريطة الخدمات' أو 'الرادار التفاعلي' لعرض مواقع جميع مقدمي الخدمات القريبين منك جغرافياً في $city دون أي حاجة للإنترنت!"
            }
            else -> {
                "يا هلا بك! استفسارك واضح: \"$prompt\".\n\n" +
                "أنا جاهز لمساعدتك فوراً في:\n" +
                "• ⚡ طلب فني صيانة معتمد في $city (سباكة، كهرباء، طاقة شمسية، مكيفات، غسالات، سيارات).\n" +
                "• 🏪 تصفح أقرب المحلات، المطاعم، والمراكز الطبية.\n" +
                "• 🗺️ فتح الخريطة والرادار الأوفلاين لاكتشاف الخدمات بدون نت.\n\n" +
                "💡 اضغط على زر '⚡ اطلب الآن' أو اكتب تفاصيل مشكلتك لأساعدك فوراً!"
            }
        }
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
