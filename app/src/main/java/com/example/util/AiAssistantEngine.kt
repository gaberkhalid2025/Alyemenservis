package com.example.util

import android.content.Context
import androidx.annotation.Keep
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

@Keep
data class AiResponse(
    val title: String = "",
    val message: String = "",
    val diySteps: List<String> = emptyList(),
    val preventiveTips: String? = null,
    val chipLabel: String? = null,
    val suggestedCategory: String? = null,
    val referralAction: String? = null, // "CREATE_SERVICE_ORDER", "CHECK_BOOKINGS", "REQUEST_TECHNICIAN", "EXPLORE_STORES", "EXPLORE_MAP"
    val deepLinkRoute: String? = null,
    val isOfflineMode: Boolean = true,
    val estimatedCost: String? = null
)

@Keep
data class TroubleshootingGuide(
    val id: String,
    val category: String,
    val keywords: List<String>,
    val title: String,
    val diySteps: List<String>,
    val preventiveTips: String = "",
    val referralAction: String,
    val suggestedCategory: String,
    val chipLabel: String,
    val estimatedCost: String = ""
)

/**
 * 🇾🇪 AiAssistantEngine - محرك المساعد الذكي اليمني المتقدم
 * 
 * الميزات:
 * 1. دمج كامل مع Google Gemini API (نموذج gemini-3.5-flash) لتوليد ردود ذكية مخصصة.
 * 2. استخدام تقنية RAG (Retrieval-Augmented Generation) لربط الردود بقاعدة بيانات الفنيين والخدمات والمحلات في اليمن.
 * 3. حفظ سجل المحادثات في Firebase Firestore collection ("assistant_chat_history").
 * 4. دعم وضع العمل بدون إنترنت (Offline DIY Engine) بفهم اللهجة والمصطلحات المحلية اليمنية.
 * 5. توجيه ذكي للمستخدمين (Referral Actions) لحجز الفنيين أو تصفح الخريطة وطلب الخدمات السريعة.
 */
class AiAssistantEngine(private val context: Context) {

    private val geminiApi = GeminiApi()
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val guidesList = mutableListOf<TroubleshootingGuide>()
    private val faqList = mutableListOf<Pair<List<String>, String>>()
    private val queryCache = mutableMapOf<String, AiResponse>()

    init {
        loadOfflineData()
        loadBuiltInYemeniGuides()
    }

    /**
     * استعلام المساعد الذكي باستخدام Gemini API و RAG مع حفظ سجل المحادثة (Suspend Function)
     * 
     * @param prompt استفسار المستخدم
     * @return الرد الذكي المولد
     */
    suspend fun queryAssistant(prompt: String): String = withContext(Dispatchers.IO) {
        if (prompt.isBlank()) return@withContext "يرجى كتابة استفسارك أو مشكلتك لمساعدتك."

        // 1. جلب سياق البيانات ذات الصلة من Firebase / Local (RAG)
        val ragContext = getRelevantRagContext(prompt)

        // 2. إرسال الطلب إلى Gemini API
        val response = if (geminiApi.isApiKeyConfigured()) {
            val geminiResult = geminiApi.generateContent(prompt, ragContext)
            if (geminiResult.isNotBlank() && !geminiResult.contains("تعذر الحصول على استجابة")) {
                geminiResult
            } else {
                generateLocalFallbackAnswer(prompt)
            }
        } else {
            generateLocalFallbackAnswer(prompt)
        }

        // 3. تخزين سجل المحادثة في Firestore
        saveChatHistory(prompt, response)

        return@withContext response
    }

    /**
     * استعلام المساعد الذكي عبر Callback وإرجاع كائن AiResponse منظم لواجهات ولوحات التحكم
     */
    fun queryAssistant(prompt: String, onResult: (AiResponse) -> Unit) {
        val result = processQuery(prompt)
        onResult(result)
    }

    /**
     * جلب سياق معرفي من قاعدة البيانات لاستخدامه في RAG
     */
    private suspend fun getRelevantRagContext(prompt: String): String {
        val contextBuilder = StringBuilder()
        contextBuilder.append("تطبيق: دليل خدمات اليمن (منصة لحجز الفنيين وصيانة المنازل والمحلات التجارية والطاقة الشمسية والسباكة والكهرباء في اليمن).\n")

        // البحث في الإرشادات المحلية ذات الصلة
        val matchedGuide = findBestGuide(prompt)
        if (matchedGuide != null) {
            contextBuilder.append("إرشادات صيانة سريعة متوفرة:\n")
            contextBuilder.append("- العنوان: ${matchedGuide.title}\n")
            contextBuilder.append("- خطوات الإصلاح الذاتي: ${matchedGuide.diySteps.joinToString("، ")}\n")
            contextBuilder.append("- نصيحة وقائية: ${matchedGuide.preventiveTips}\n")
            contextBuilder.append("- التصنيف المقترح: ${matchedGuide.suggestedCategory}\n")
        }

        // جلب أفضل المزودين والمحلات ذات الصلة من Firestore
        try {
            val keywords = extractKeywords(prompt)
            if (keywords.isNotEmpty()) {
                val primaryKeyword = keywords.first()
                val providersSnap = firestore.collection("providers")
                    .whereArrayContains("specialties", primaryKeyword)
                    .limit(3)
                    .get()
                    .await()

                if (!providersSnap.isEmpty) {
                    contextBuilder.append("\nأبرز الفنيين المتاحين في هذا المجال:\n")
                    for (doc in providersSnap.documents) {
                        val name = doc.getString("name") ?: ""
                        val city = doc.getString("city") ?: "صنعاء"
                        val phone = doc.getString("phone") ?: ""
                        contextBuilder.append("- الفني: $name (المدينة: $city، رقم التواصل: $phone)\n")
                    }
                }
            }
        } catch (e: Exception) {
            // تجاهل خطأ جلب RAG في حال انقطاع الشبكة
        }

        return contextBuilder.toString()
    }

    /**
     * حفظ سجل المحادثة في Firestore
     */
    private fun saveChatHistory(userPrompt: String, botResponse: String) {
        try {
            val currentUserId = auth.currentUser?.uid ?: "GUEST_${System.currentTimeMillis()}"
            val historyDoc = hashMapOf<String, Any?>(
                "id" to EntityIdGenerator.generate(EntityIdGenerator.Prefix.CHAT),
                "userId" to currentUserId,
                "userMessage" to userPrompt,
                "botResponse" to botResponse,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("assistant_chat_history")
                .document(historyDoc["id"] as String)
                .set(historyDoc)
                .addOnFailureListener { /* صامت */ }
        } catch (e: Exception) {
            // صامت
        }
    }

    /**
     * توليد رد احتياطي محلي في حال غياب الإنترنت أو مفتاح API
     */
    private fun generateLocalFallbackAnswer(prompt: String): String {
        val localResponse = processQuery(prompt)
        val sb = StringBuilder()
        if (localResponse.title.isNotBlank()) {
            sb.append("🔧 **${localResponse.title}**\n\n")
        }
        sb.append(localResponse.message)
        if (localResponse.diySteps.isNotEmpty()) {
            sb.append("\n\n🛠️ **خطوات الفحص والإصلاح المقترحة:**\n")
            localResponse.diySteps.forEachIndexed { idx, step ->
                sb.append("${idx + 1}. $step\n")
            }
        }
        if (!localResponse.preventiveTips.isNullOrBlank()) {
            sb.append("\n💡 **نصيحة وقائية:** ${localResponse.preventiveTips}")
        }
        if (!localResponse.estimatedCost.isNullOrBlank()) {
            sb.append("\n💰 **التقدير التقريبي:** ${localResponse.estimatedCost}")
        }
        return sb.toString()
    }

    /**
     * معالجة الاستعلام وتقديم كائن استجابة منظم للاستخدام في الواجهات ولوحات التحكم
     * 
     * @param query نص الاستعلام
     * @return كائن AiResponse منظم
     */
    fun processQuery(query: String): AiResponse {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        if (cleanQuery.isEmpty()) {
            return AiResponse(
                title = "مرحباً بك!",
                message = "كيف يمكنني مساعدتك اليوم في خدمات الصيانة والفنيين في اليمن؟",
                chipLabel = "المساعد الذكي"
            )
        }

        queryCache[cleanQuery]?.let { return it }

        // البحث عن أفضل دليل صيانة
        val matchedGuide = findBestGuide(cleanQuery)
        if (matchedGuide != null) {
            val response = AiResponse(
                title = matchedGuide.title,
                message = "إليك إرشادات المعاينة والحل السريع لمشكلة (${matchedGuide.title}):",
                diySteps = matchedGuide.diySteps,
                preventiveTips = matchedGuide.preventiveTips.ifBlank { null },
                chipLabel = matchedGuide.chipLabel,
                suggestedCategory = matchedGuide.suggestedCategory,
                referralAction = matchedGuide.referralAction,
                estimatedCost = matchedGuide.estimatedCost.ifBlank { null },
                isOfflineMode = true
            )
            queryCache[cleanQuery] = response
            return response
        }

        // البحث في الأسئلة الشائعة
        for ((keywords, answer) in faqList) {
            if (keywords.any { cleanQuery.contains(it) }) {
                val response = AiResponse(
                    title = "إجابة المساعد الذكي",
                    message = answer,
                    chipLabel = "معلومات عامة",
                    referralAction = "EXPLORE_MAP"
                )
                queryCache[cleanQuery] = response
                return response
            }
        }

        // رد عام افتراضي
        val defaultResp = AiResponse(
            title = "طلب خدمة صيانة متخصصة",
            message = "تم تحليل استفسارك. للحصول على أفضل خدمة ننصحك بطلب فني متخصص أو تصفح الخريطة التفاعلية لاختيار أقرب المحلات والمراكز المعتمدة.",
            chipLabel = "خدمات الصيانة",
            suggestedCategory = "صيانة عامة",
            referralAction = "REQUEST_TECHNICIAN",
            diySteps = listOf(
                "تأكد من فصل التيار الكهربائي أو إغلاق محبس المياه الرئيسي عند أي عطل طارئ.",
                "حدد موقعك بدقة في التطبيق ليصلك أقرب فني مرخص.",
                "يمكنك التفاوض على السعر ومراجعة تقييمات العملاء السابقين قبل بدء العمل."
            )
        )
        queryCache[cleanQuery] = defaultResp
        return defaultResp
    }

    private fun findBestGuide(query: String): TroubleshootingGuide? {
        var bestGuide: TroubleshootingGuide? = null
        var maxScore = 0

        for (guide in guidesList) {
            var score = 0
            for (kw in guide.keywords) {
                if (query.contains(kw)) {
                    score += kw.length
                }
            }
            if (score > maxScore) {
                maxScore = score
                bestGuide = guide
            }
        }

        return if (maxScore >= 3) bestGuide else null
    }

    private fun extractKeywords(text: String): List<String> {
        val stopWords = setOf("في", "من", "على", "إلى", "عن", "مع", "هذا", "هذه", "هل", "كيف", "أريد", "عندي", "مشكلة")
        return text.split(Regex("[\\s,،.?!]+"))
            .map { it.trim() }
            .filter { it.length > 2 && it !in stopWords }
    }

    private fun loadOfflineData() {
        try {
            val jsonString = context.assets.open("ai_assistant_data.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            if (root.has("troubleshooting_guides")) {
                val guidesArray = root.getJSONArray("troubleshooting_guides")
                for (i in 0 until guidesArray.length()) {
                    val obj = guidesArray.getJSONObject(i)
                    val kws = mutableListOf<String>()
                    val kwArray = obj.optJSONArray("keywords")
                    if (kwArray != null) {
                        for (k in 0 until kwArray.length()) kws.add(kwArray.getString(k))
                    }
                    val steps = mutableListOf<String>()
                    val stArray = obj.optJSONArray("diy_steps")
                    if (stArray != null) {
                        for (s in 0 until stArray.length()) steps.add(stArray.getString(s))
                    }

                    guidesList.add(
                        TroubleshootingGuide(
                            id = obj.optString("id", "g_$i"),
                            category = obj.optString("category", ""),
                            keywords = kws,
                            title = obj.optString("title", ""),
                            diySteps = steps,
                            preventiveTips = obj.optString("preventive_tips", ""),
                            referralAction = obj.optString("referral_action", "REQUEST_TECHNICIAN"),
                            suggestedCategory = obj.optString("suggested_category", ""),
                            chipLabel = obj.optString("chip_label", "صيانة"),
                            estimatedCost = obj.optString("estimated_cost", "")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // صامت
        }
    }

    private fun loadBuiltInYemeniGuides() {
        guidesList.add(
            TroubleshootingGuide(
                id = "solar_inverter_fault",
                category = "طاقة شمسية",
                keywords = listOf("إنفرتر", "انفرتر", "طاقة شمسية", "بطارية", "شاحن شمسي", "لوح شمسي", "كهرباء مقطوعة"),
                title = "فحص منظومة الطاقة الشمسية والإنفرتر",
                diySteps = listOf(
                    "تأكد من نظافة الألواح الشمسية من الغبار لضمان كفاءة الشحن.",
                    "افحص قاطع البطارية وقاطع الألواح وتأكد من عدم فصل الفيوزات.",
                    "راقب رمز الخطأ الظاهر على شاشة الإنفرتر (مثل 04 لجهد البطارية المنخفض أو 07 للحمل الزائد).",
                    "أعد تشغيل قاطع الإنفرتر بعد إطفاء الأحمال الثقيلة."
                ),
                preventiveTips = "قم بقياس مستوى سائل البطاريات كل شهر وتنظيف أقطاب البطارية من الكبرتة.",
                referralAction = "REQUEST_TECHNICIAN",
                suggestedCategory = "طاقة شمسية",
                chipLabel = "طاقة شمسية ☀️",
                estimatedCost = "2,000 - 5,000 ريال يمني"
            )
        )

        guidesList.add(
            TroubleshootingGuide(
                id = "plumbing_leak",
                category = "سباكة",
                keywords = listOf("سباكة", "تسريب", "ماسورة", "حنفية", "خزان", "دينمو", "تهريب مياه", "سيفون"),
                title = "معالجة تسريب المياه وفحص السباكة",
                diySteps = listOf(
                    "أغلق المحبس الرئيسي المغذي للشقة أو العمارة فوراً لمنع هدر المياه.",
                    "حدد مصدر التسريب (كوع ماسورة، محبس زاوية، أو خلاط).",
                    "استخدم شريط التفلون الأبيض (Teflon Tape) لإحكام ربط السن إذا كان التسريب خفيفاً.",
                    "تأكد من عمل عوامة الخزان الأرضي أو العلوي بشكل سليم لمنع الفوضى."
                ),
                preventiveTips = "افحص ضغط مضخة المياه وتأكد من جودة المحابس الإيطالية المقاومة للصدأ.",
                referralAction = "REQUEST_TECHNICIAN",
                suggestedCategory = "سباكة",
                chipLabel = "سباكة وخزانات 🚰",
                estimatedCost = "1,500 - 4,000 ريال يمني"
            )
        )

        guidesList.add(
            TroubleshootingGuide(
                id = "ac_cooling",
                category = "تكييف وتبريد",
                keywords = listOf("مكيف", "تبريد", "فريون", "سبلت", "كمبروسر", "مروحة مكيف", "ما يبرد"),
                title = "فحص كفاءة المكيف والتبريد",
                diySteps = listOf(
                    "نظف فلاتر الهواء الداخلية بالماء الفاتر وجففها جيداً.",
                    "تأكد من ضبط الريموت على وضع التبريد (Cool ❄️) ودرجة حرارة 22-24 مئوية.",
                    "افحص الوحدة الخارجية وتأكد من دوران المروحة وعدم وجود عوائق أمامها."
                ),
                preventiveTips = "قم بإجراء صيانة دورية وغسيل للرادياتير مع بداية فصل الصيف لتفادي احتراق الكمبروسر.",
                referralAction = "REQUEST_TECHNICIAN",
                suggestedCategory = "تكييف وتبريد",
                chipLabel = "تكييف وتبريد ❄️",
                estimatedCost = "3,000 - 8,000 ريال يمني"
            )
        )

        faqList.add(
            Pair(
                listOf("أسعار", "تكلفة", "كم يكلف", "سعر الفني"),
                "تعتمد تكلفة الخدمات في دليل خدمات اليمن على نوع الصيانة والقطع المطلوبة. يمكنك مراجعة الأسعار التقديرية لكل فني والتفاوض المباشر عبر المحادثة المدمجة."
            )
        )
        faqList.add(
            Pair(
                listOf("ضمان", "كفالة", "هل يوجد ضمان"),
                "جميع الفنيين والمراكز المعتمدة يقدمون ضماناً على أعمال الصيانة والقطع الأصلية، ويمكنك التحقق من شارة الاعتماد الذهبية في ملف الفني."
            )
        )
    }
}
