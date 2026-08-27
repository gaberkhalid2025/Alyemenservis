package com.example.util

import android.content.Context
import androidx.annotation.Keep
import org.json.JSONObject
import java.util.Locale
import kotlin.math.min

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
 * 🇾🇪 محرك المساعد الذكي اليمني المتقدم
 * يفهم المصطلحات واللهجات اليمنية، الاستفسارات الطويلة، استعلامات الحجوزات، إرشادات الصيانة (DIY)، وإنشاء الطلبات الفورية
 */
class AiAssistantEngine(private val context: Context) {

    private val guidesList = mutableListOf<TroubleshootingGuide>()
    private val faqList = mutableListOf<Pair<List<String>, String>>()
    private val queryCache = mutableMapOf<String, AiResponse>()

    init {
        loadOfflineData()
        loadBuiltInYemeniGuides()
    }

    private fun loadOfflineData() {
        try {
            val jsonString = context.assets.open("ai_assistant_data.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)

            if (root.has("troubleshooting_guides")) {
                val guidesArray = root.getJSONArray("troubleshooting_guides")
                for (i in 0 until guidesArray.length()) {
                    val obj = guidesArray.getJSONObject(i)
                    val keywordsArr = obj.getJSONArray("keywords")
                    val kwList = mutableListOf<String>()
                    for (k in 0 until keywordsArr.length()) kwList.add(keywordsArr.getString(k))

                    val stepsArr = obj.getJSONArray("diy_steps")
                    val stepsList = mutableListOf<String>()
                    for (s in 0 until stepsArr.length()) stepsList.add(stepsArr.getString(s))

                    guidesList.add(
                        TroubleshootingGuide(
                            id = obj.getString("id"),
                            category = obj.optString("category", ""),
                            keywords = kwList,
                            title = obj.getString("title"),
                            diySteps = stepsList,
                            preventiveTips = obj.optString("preventive_tips", ""),
                            referralAction = obj.optString("referral_action", "REQUEST_TECHNICIAN"),
                            suggestedCategory = obj.optString("suggested_category", ""),
                            chipLabel = obj.optString("chip_label", "طلب فني متخصص"),
                            estimatedCost = obj.optString("estimated_cost", "")
                        )
                    )
                }
            }

            if (root.has("faq")) {
                val faqArr = root.getJSONArray("faq")
                for (i in 0 until faqArr.length()) {
                    val obj = faqArr.getJSONObject(i)
                    val keywordsArr = obj.getJSONArray("keywords")
                    val kwList = mutableListOf<String>()
                    for (k in 0 until keywordsArr.length()) kwList.add(keywordsArr.getString(k))
                    val answer = obj.getString("answer")
                    faqList.add(Pair(kwList, answer))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadBuiltInYemeniGuides() {
        // قاموس إضافي شامل للمصطلحات اليمنية والمواقف اليومية
        guidesList.add(
            TroubleshootingGuide(
                id = "plumbing_water_motor",
                category = "سباكة",
                keywords = listOf("دينمو", "ماطور", "ماطور ماء", "بزبوز", "حنفية", "خزان يقطر", "تسريب ماء", "سباك", "أشتي سباك", "مواسير", "عوامة الخزان"),
                title = "🔧 تشخيص أعطال السباكة وماطور الماء",
                diySteps = listOf(
                    "افحص قاطع الكهرباء الخاص بالدينمو وتأكد من وصول التيار.",
                    "تأكد من عدم وجود هواء في ماسورة السحب (قم بتنفيس الدينمو من برغي الهواء).",
                    "إذا كان البزبوز أو الحنفية تقطر، افحص الجلبة المطاطية (الواشر) واستبدلها.",
                    "أغلق المحبس الرئيسي فوراً في حال وجود تسريب قوي لتجنب إهدار المياه."
                ),
                referralAction = "CREATE_SERVICE_ORDER",
                suggestedCategory = "سباكة",
                chipLabel = "🛠️ إنشاء طلب سباك معتمد الآن",
                estimatedCost = "3,000 - 6,000 ريال يمني"
            )
        )

        guidesList.add(
            TroubleshootingGuide(
                id = "ac_cooling_issue",
                category = "تكييف وتبريد",
                keywords = listOf("مكيف", "تبريد", "ما يبردش", "حار", "كمبروسر", "غاز الفريون", "مكيف صحراوي", "مكيف سبليت", "ينقط ماء"),
                title = "❄️ تشخيص مشاكل التكييف والتبريد",
                diySteps = listOf(
                    "تأكد من ضبط الريموت على وضع التبريد (Cool ❄️) بدرجة حرارة بين 22-24.",
                    "قم بفك فلاتر الهواء الأمامية وغسلها جيداً من الغبار والأتربة وجففها.",
                    "افحص خرطوم تصريف الماء وتأكد من عدم انسداده بالأوساخ.",
                    "إذا كان الهواء يخرج حاراً والكمبروسر لا يعمل، قد يحتاج المكيف لفحص الفريون أو الكابستر."
                ),
                referralAction = "CREATE_SERVICE_ORDER",
                suggestedCategory = "تكييف وتبريد",
                chipLabel = "❄️ طلب فني تكييف للمعاينة",
                estimatedCost = "5,000 - 12,000 ريال يمني"
            )
        )

        guidesList.add(
            TroubleshootingGuide(
                id = "electricity_solar",
                category = "كهرباء وطاقة شمسية",
                keywords = listOf("كهرباء", "طاقة شمسية", "الواح", "انفرتر", "انفيرتر", "بطارية", "القاطع نزل", "التماس", "كنداكتر", "كهربائي", "مافيش كهرباء", "شورت"),
                title = "⚡ تشخيص منظومة الطاقة الشمسية والكهرباء",
                diySteps = listOf(
                    "افحص شاشة الانفرتر وشاهد كود الخطأ (Fault Code) الظاهر.",
                    "تأكد من مستوى فولتية البطاريات وعدم وجود أسلاك مفكوكة أو متأكسدة.",
                    "في حال نزول القاطع الرئيسي، افصل الأجهزة الثقيلة ثم ارفع القاطع بالتدريج لمعرفة الجهاز المسبب للالتماس.",
                    "تحذير: لا تلمس الأسلاك المكشوفة وتأكد من ارتداء حذاء عازل للأمان."
                ),
                referralAction = "CREATE_SERVICE_ORDER",
                suggestedCategory = "كهرباء",
                chipLabel = "⚡ طلب فني طاقة وكهرباء",
                estimatedCost = "4,000 - 8,000 ريال يمني"
            )
        )

        guidesList.add(
            TroubleshootingGuide(
                id = "car_puncture_battery",
                category = "ميكانيك وبنشر سيارات",
                keywords = listOf("سيارة", "بنشر", "تاير", "كفر", "بطارية سيارة", "ما تدقش سلف", "عطلان في الطريق", "ميكانيكي", "سطحة", "سحب سيارة", "طرمبة بنزين"),
                title = "🚗 خدمات طوارئ السيارات والبنشر",
                diySteps = listOf(
                    "أوقف السيارة في مكان آمن واشعل إشارات الطوارئ (الرباعي).",
                    "إذا كانت البطارية ضعيفة، استخدم كابل اشتراك مع سيارة أخرى أو اطلب فحص السلف.",
                    "في حال البنشر، تأكد من ثبات الهاندبريك ووضع حجر خلف الإطار قبل رفع الجك."
                ),
                referralAction = "CREATE_SERVICE_ORDER",
                suggestedCategory = "سيارات",
                chipLabel = "🚗 طلب فني طوارئ سيارات فوراً",
                estimatedCost = "3,000 - 10,000 ريال يمني"
            )
        )

        guidesList.add(
            TroubleshootingGuide(
                id = "real_estate_rental",
                category = "عقارات",
                keywords = listOf("شقة", "ايجار", "عقار", "بيت", "عمارة", "دكان", "مكتب", "أشتي استأجر", "عقارات", "سمسار"),
                title = "🏠 البحث عن العقارات والشقق المتاحة",
                diySteps = listOf(
                    "حدد المنطقة المرغوبة (حدة، الأصبحي، المطار، الستين، خور مكسر...).",
                    "يمكنك تصفح قسم العقارات مباشرة لمشاهدة الصور، الأسعار، والمواصفات.",
                    "تواصل مباشرة مع المالك أو المكتب عبر المحادثة أو زر الاتصال لمعاينة الموقع."
                ),
                referralAction = "EXPLORE_MAP",
                suggestedCategory = "عقارات",
                chipLabel = "🏢 تصفح العقارات على الخريطة",
                estimatedCost = ""
            )
        )

        guidesList.add(
            TroubleshootingGuide(
                id = "medical_clinic",
                category = "مراكز طبية وصيدليات",
                keywords = listOf("دكتور", "طبيب", "صيدلية", "مستشفى", "باطنية", "أسنان", "عيادة", "فحص", "مختبر", "علاج", "دواء"),
                title = "🏥 المراكز الطبية والعيادات المتخصصة",
                diySteps = listOf(
                    "في حالات الطوارئ القصوى يرجى التوجه لأقرب طوارئ مستشفى.",
                    "يمكنك تصفح العيادات والمراكز المعتمدة ومطابقة التأمين الصحي وساعات العمل.",
                    "استخدم خريطة الخدمات للوصول إلى أقرب صيدلية مناوبة بالقرب منك."
                ),
                referralAction = "EXPLORE_MAP",
                suggestedCategory = "مراكز طبية",
                chipLabel = "🏥 عرض العيادات والصيدليات القريبة",
                estimatedCost = ""
            )
        )
    }

    /**
     * معالجة الاستعلام وفهم السياق ولهجات اليمن مع دعم الكاشينج
     */
    fun queryAssistant(
        prompt: String,
        currentCity: String = "صنعاء",
        isOnlineAvailable: Boolean = false,
        onResult: (AiResponse) -> Unit
    ) {
        val queryLower = prompt.trim().lowercase(Locale.getDefault())

        if (queryLower.isBlank()) {
            onResult(
                AiResponse(
                    title = "مساعد دليل خدمات اليمن الذكي 🇾🇪",
                    message = "حياك الله! أنا مساعدك الشخصي للبحث عن الفنيين، المحلات، العقارات، حل الأعطال، والاستعلام عن طلباتك.",
                    chipLabel = "🛠️ طلب خدمة جديدة",
                    referralAction = "CREATE_SERVICE_ORDER"
                )
            )
            return
        }

        // Check in-memory cache
        if (queryCache.containsKey(queryLower)) {
            onResult(queryCache[queryLower]!!)
            return
        }

        // الاستعلام عن الحجوزات والطلبات السابقة
        if (queryLower.contains("حجز") || queryLower.contains("طلباتي") || queryLower.contains("حالة الطلب") || queryLower.contains("حجوزاتي") || queryLower.contains("وين وصل طلبي")) {
            val resp = AiResponse(
                title = "📋 الاستعلام عن حالة الحجوزات والطلبات",
                message = "يمكنك متابعة حالة جميع حجوزاتك وطلباتك المباشرة ومعرفة هل تم قبولها أو وصول الفني إليك من خلال شاشة الحجوزات.",
                chipLabel = "📋 فتح سجل حجوزاتي الآن",
                referralAction = "CHECK_BOOKINGS",
                deepLinkRoute = "bookings_screen",
                isOfflineMode = true
            )
            queryCache[queryLower] = resp
            onResult(resp)
            return
        }

        // المطابقة الذكية مع أدلة الصيانة والمصطلحات
        var bestGuideMatch: TroubleshootingGuide? = null
        var maxScore = 0

        for (guide in guidesList) {
            var matchCount = 0
            for (kw in guide.keywords) {
                val kwLower = kw.lowercase(Locale.getDefault())
                if (queryLower.contains(kwLower)) {
                    matchCount += 3
                } else if (levenshteinDistance(queryLower, kwLower) <= 2) {
                    matchCount += 1
                }
            }
            if (matchCount > maxScore) {
                maxScore = matchCount
                bestGuideMatch = guide
            }
        }

        if (bestGuideMatch != null && maxScore >= 2) {
            val costText = if (bestGuideMatch.estimatedCost.isNotBlank()) " | التكلفة التقديرية: ${bestGuideMatch.estimatedCost}" else ""
            val resp = AiResponse(
                title = bestGuideMatch.title,
                message = "إليك خطوات فحص وصيانة سريعة ومجربة (DIY)$costText:",
                diySteps = bestGuideMatch.diySteps,
                preventiveTips = bestGuideMatch.preventiveTips.ifEmpty { null },
                chipLabel = bestGuideMatch.chipLabel,
                suggestedCategory = bestGuideMatch.suggestedCategory,
                referralAction = bestGuideMatch.referralAction,
                deepLinkRoute = "urgent_request/${bestGuideMatch.suggestedCategory}",
                isOfflineMode = true,
                estimatedCost = bestGuideMatch.estimatedCost
            )
            queryCache[queryLower] = resp
            onResult(resp)
            return
        }

        // فحص الأسئلة الشائعة
        for ((kwList, answer) in faqList) {
            if (kwList.any { queryLower.contains(it.lowercase(Locale.getDefault())) }) {
                val resp = AiResponse(
                    title = "معلومات الخدمة المعتمدة",
                    message = answer,
                    chipLabel = "🛠️ إنشاء طلب صيانة مباشر",
                    referralAction = "CREATE_SERVICE_ORDER",
                    deepLinkRoute = "urgent_request_create",
                    isOfflineMode = true
                )
                queryCache[queryLower] = resp
                onResult(resp)
                return
            }
        }

        // الرد الافتراضي الذكي
        val defaultResp = AiResponse(
            title = "المساعد الذكي 🇾🇪",
            message = "فهمت استفسارك بخصوص: \"$prompt\" في مدينة $currentCity. يمكنك طلب فني معتمد أو البحث عبر الخريطة التفاعلية لاكتشاف أقرب مزودي الخدمات فوراً.",
            chipLabel = "🛠️ إنشاء طلب لهذه المشكلة الآن",
            referralAction = "CREATE_SERVICE_ORDER",
            deepLinkRoute = "urgent_request_create",
            isOfflineMode = true
        )
        queryCache[queryLower] = defaultResp
        onResult(defaultResp)
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1) { 0 }

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }
}
