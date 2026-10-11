package com.example.utils

import android.content.Context
import androidx.annotation.Keep
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
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

    companion object {
        @Volatile
        private var instance: AiAssistantEngine? = null

        fun getInstance(context: Context): AiAssistantEngine {
            return instance ?: synchronized(this) {
                instance ?: AiAssistantEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    private val guidesList = mutableListOf<TroubleshootingGuide>()
    private val faqList = mutableListOf<Pair<List<String>, String>>()
    private val queryCache = ConcurrentHashMap<String, AiResponse>()

    init {
        loadOfflineData()
        loadBuiltInYemeniGuides()
    }

    suspend fun refreshGuidesFromFirestore() {
        try {
            val snapshot = FirebaseFirestore.getInstance().collection("ai_guides").get().await()
            val newGuides = snapshot.documents.mapNotNull { doc ->
                doc.toObject(TroubleshootingGuide::class.java)
            }
            if (newGuides.isNotEmpty()) {
                synchronized(guidesList) {
                    guidesList.clear()
                    guidesList.addAll(newGuides)
                }
                synchronized(queryCache) {
                    queryCache.clear()
                }
            }
        } catch (e: Exception) {
            // Keep existing offline & built-in guides
        }
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

        guidesList.add(
            TroubleshootingGuide(
                id = "washing_machine_appliance_repair",
                category = "صيانة غسالات وأجهزة منزلية",
                keywords = listOf("غسالة", "غساله", "صيانة الغسالة", "سعر صيانة الغسالة", "تنشيف", "تصريف الغسالة", "غسالة اتوماتيك", "ثلاجة", "أجهزة منزلية"),
                title = "🧺 تشخيص وتسعيرة صيانة الغسالات والأجهزة المنزلية",
                diySteps = listOf(
                    "تأكد من نظافة فلتر طرمبة التصريف السفلي وإزالة أي عوالق أو عملات معدنية.",
                    "افحص خرطوم التصريف الخلفي وتأكد من عدم انثنائه أو انسداده.",
                    "تأكد من توازن أرجل الغسالة على أرضية مستوية لمنع الاهتزاز العالي أثناء التنشيف.",
                    "متوسط سعر المعاينة وصيانة الغسالات في اليمن يتراوح بين 4,000 إلى 9,000 ريال يمني حسب نوع العطل وقطع الغيار."
                ),
                preventiveTips = "ينصح بتنظيف فلتر الغسالة مرة كل شهر وعدم تجاوز الوزن المسموح للملابس.",
                referralAction = "CREATE_SERVICE_ORDER",
                suggestedCategory = "صيانة أجهزة منزلية",
                chipLabel = "🛠️ طلب فني صيانة غسالات الآن",
                estimatedCost = "4,000 - 9,000 ريال يمني"
            )
        )

        guidesList.add(
            TroubleshootingGuide(
                id = "refrigerator_freezer_repair",
                category = "ثلاجات وتبريد",
                keywords = listOf("ثلاجة", "ثلاجه", "فريزر", "تبريد الثلاجة", "الثلاجة ما تبردش", "تثلج", "تسريب فريون الثلاجة", "موتور الثلاجة", "تنزل ماء"),
                title = "🧊 تشخيص أعطال الثلاجات والفريزرات المنزلية",
                diySteps = listOf(
                    "تأكد من ضبط مؤشر التبريد (الثرموستات) على الدرجة المناسبة (3-4 في الصيف و2 في الشتاء).",
                    "افحص جوان (كاوتشوك) الباب وتأكد من إحكام إغلاقه وعدم تسريب الهواء البارد.",
                    "نظف شبكة المكثف الخلفية من الغبار المتراكم لضمان كفاءة تبادل الحرارة.",
                    "في حال تراكم الثلج في الفريزر وضعف التبريد في الكابينة السفلية، افحص سخان إذابة الثلج (Defrost Heater) ومروحة الهواء."
                ),
                preventiveTips = "تجنب وضع أطعمة ساخنة مباشرة داخل الثلاجة واترك مسافة 10 سم بين ظهر الثلاجة والجدار.",
                referralAction = "CREATE_SERVICE_ORDER",
                suggestedCategory = "صيانة أجهزة منزلية",
                chipLabel = "❄️ طلب فني صيانة ثلاجات",
                estimatedCost = "4,000 - 10,000 ريال يمني"
            )
        )

        guidesList.add(
            TroubleshootingGuide(
                id = "inverter_fault_codes_solar",
                category = "طاقة شمسية وانفرترات",
                keywords = listOf("كود انفرتر", "كود خطأ", "انفرتر يصفر", "fault 04", "fault 05", "fault 52", "fault 01", "بطارية هابطة", "انخفاض الفولت", "شاحن شمسي", "mppt"),
                title = "⚡ دليل أكواد أخطاء الإنفرترات والمنظومات الشمسية",
                diySteps = listOf(
                    "كود 04 / Low Battery: انخفاض فولتية البطاريات إلى حد الفصل؛ افصل الأحمال فوراً ودع الألواح تعيد الشحن.",
                    "كود 05 / Overload: زيادة الحمل عن طاقة الإنفرتر؛ افصل الأجهزة الثقيلة (مكواة، غسالة، مضخة) وأعد تشغيله.",
                    "كود 01 / Fan Fault: عطل أو توقف مروحة التبريد؛ نظف فتحات التهوية من الغبار وتأكد من دوران المروحة.",
                    "كود 52 / Bus Voltage Low: خلل في دائرة الفولتية الداخلية أو ضعف كبير في ألواح الطاقة."
                ),
                preventiveTips = "احرص على تنظيف ألواح الطاقة الشمسية دورياً وفحص أقطاب البطاريات من التأكسد.",
                referralAction = "CREATE_SERVICE_ORDER",
                suggestedCategory = "طاقة شمسية",
                chipLabel = "⚡ طلب مهندس طاقة شمسية",
                estimatedCost = "3,500 - 8,000 ريال يمني"
            )
        )

        guidesList.add(
            TroubleshootingGuide(
                id = "gas_stove_cylinder_leak",
                category = "غاز ومطابخ",
                keywords = listOf("غاز", "دبة غاز", "اسطوانة غاز", "تسريب غاز", "ريحة غاز", "فرن", "بوتجاز", "منظم الغاز", "عين البوتجاز"),
                title = "🔥 طوارئ تسريب أسطوانات الغاز والأفران",
                diySteps = listOf(
                    "🚨 عند شم رائحة غاز: لا تشعل أي نار ولا تلمس مفاتيح الكهرباء إطلاقاً وافتح النوافذ فوراً للتهوية.",
                    "أغلق صمام منظم الأسطوانة فوراً وأخرج الأسطوانة لمكان مفتوح إن أمكن.",
                    "لاختبار مكان التسريب: استخدم إسفنجة مبللة بالماء والصابون على الوصلات والمنظم؛ ظهور فقاعات يدل على مكان التسريب.",
                    "استبدل الخرطوم التالف أو الجلبة المطاطية (الواشر) فوراً ولا تستخدم خراطيم غير مخصصة للغاز."
                ),
                preventiveTips = "استبدل خرطوم الغسالة والمنظم كل سنتين واستخدم واشر نحاسي أو مطاطي أصلي.",
                referralAction = "CREATE_SERVICE_ORDER",
                suggestedCategory = "صيانة منزلية",
                chipLabel = "🛠️ طلب فني صيانة أفران وغاز",
                estimatedCost = "2,000 - 5,000 ريال يمني"
            )
        )

        guidesList.add(
            TroubleshootingGuide(
                id = "roof_leak_waterproofing",
                category = "عزل ودهانات",
                keywords = listOf("تسريب السطح", "عزل مائي", "مروان", "قطران", "تقشير الدهان", "رطوبة الجدار", "نشع ماء", "دهان", "معجون"),
                title = "🏠 معالجة تسريب مياه الأمطار وعزل الأسطح والرطوبة",
                diySteps = listOf(
                    "افحص ميازيب السطح ومجارير التصريف وتأكد من خلوها من الأتربة والشوائب.",
                    "حدد الشروخ والتشققات في صبة السطح وقم بتنظيفها وتعبئتها بمعجون إيبوكسي أو أسمنت مقاوم.",
                    "قم بطلاء السطح بطبقتين من العازل المطاطي (المروان أو الأكريليك المقاوم للشمس) مع شاش فايبر.",
                    "للجدران الداخلية المصابة بالرطوبة: قشر الدهان القديم ورش مانع رطوبة قبل إعادة التأسيس والمعجون."
                ),
                preventiveTips = "ينصح بتجديد فحص عوازل الأسطح سنوياً قبل حلول موسم الأمطار.",
                referralAction = "CREATE_SERVICE_ORDER",
                suggestedCategory = "صيانة وترميم",
                chipLabel = "🏗️ طلب مقاول عوازل وتشطيب",
                estimatedCost = "6,000 - 18,000 ريال يمني"
            )
        )
    }

    /**
     * معالجة الاستعلام وفهم السياق ولهجات اليمن فوري (Synchronous & Blazing Fast)
     */
    fun queryAssistantSync(
        prompt: String,
        currentCity: String = "صنعاء"
    ): AiResponse {
        val queryTrimmed = prompt.trim()
        val queryLower = queryTrimmed.lowercase(Locale.getDefault())

        if (queryLower.isBlank()) {
            return AiResponse(
                title = "مساعد دليل خدمات اليمن الذكي 🇾🇪",
                message = "يا حيا ومسهلا بك! أنا مساعدك الذكي المدمج والمجاني بالكامل، أعمل معك على مدار الساعة حتى بدون نت للإجابة على كافة الاستفسارات، حل المشاكل، وطلب الفنيين.",
                chipLabel = "🛠️ طلب خدمة جديدة",
                referralAction = "CREATE_SERVICE_ORDER"
            )
        }

        // 1. Check in-memory cache safely
        queryCache[queryLower]?.let { return it }

        // 2. التحقق من استعلامات الحجوزات والطلبات
        if (queryLower.contains("حجز") || queryLower.contains("طلباتي") || queryLower.contains("حالة الطلب") || queryLower.contains("حجوزاتي") || queryLower.contains("وين وصل طلبي")) {
            val resp = AiResponse(
                title = "📋 الاستعلام عن حالة الحجوزات والطلبات",
                message = "يمكنك متابعة حالة جميع طلباتك وحجوزاتك الحية ومعرفة هل قبلها الفني أو هل هو في الطريق إليك عبر شاشة الحجوزات المباشرة.",
                chipLabel = "📋 فتح سجل حجوزاتي الآن",
                referralAction = "CHECK_BOOKINGS",
                deepLinkRoute = "bookings_screen",
                isOfflineMode = true
            )
            queryCache[queryLower] = resp
            return resp
        }

        // 3. أرقام الطوارئ في اليمن
        if (queryLower.contains("طوارئ") || queryLower.contains("اسعاف") || queryLower.contains("دفاع مدني") || queryLower.contains("حريق") || queryLower.contains("نجده")) {
            val resp = AiResponse(
                title = "🚨 أرقام الطوارئ العامة الحيوية في اليمن",
                message = "• الدفاع المدني والإطفاء: *191*\n• طوارئ المياه والصرف الصحي: *192*\n• طوارئ الكهرباء: *195*\n• الهلال الأحمر والإسعاف: *199*\n\nيرجى الاتصال المباشر بهذه الأرقام المجانية فوراً في الحالات الحرجة!",
                chipLabel = "🚨 اتصال بالطوارئ",
                referralAction = "CREATE_SERVICE_ORDER",
                isOfflineMode = true
            )
            queryCache[queryLower] = resp
            return resp
        }

        // 4. المطابقة الذكية مع أدلة الصيانة والمصطلحات اليمنية
        var bestGuideMatch: TroubleshootingGuide? = null
        var maxScore = 0

        for (guide in guidesList) {
            var matchCount = 0
            for (kw in guide.keywords) {
                val kwLower = kw.lowercase(Locale.getDefault())
                if (queryLower.contains(kwLower)) {
                    matchCount += 4
                } else if (fastLevenshtein(queryLower, kwLower) <= 2) {
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
                message = "إليك خطوات فحص وإصلاح سريعة ومجربة (DIY)$costText:",
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
            return resp
        }

        // 5. فحص الأسئلة الشائعة
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
                return resp
            }
        }

        // 6. التحقق من المصطلحات واللهجات والأسئلة الشائعة اليمنية
        val yemeniResp = handleYemeniDialectAndCulture(queryLower, queryTrimmed, currentCity)
        if (yemeniResp != null) {
            queryCache[queryLower] = yemeniResp
            return yemeniResp
        }

        // 7. الرد الافتراضي الذكي الشامل
        val defaultResp = AiResponse(
            title = "المساعد الذكي لدليل اليمن 🇾🇪",
            message = "يا حيا ومسهلا بك! استفسارك واضح: \"$prompt\".\n\nأنا جاهز لخدمتك فوراً في أي من المجالات التالية:\n• ⚡ طلب فني صيانة معتمد في $currentCity (سباكة، كهرباء، طاقة شمسية، تكييف، غسالات، سيارات).\n• 🏪 تصفح المحلات، المطاعم، والمراكز الطبية القريبة منك.\n• 🗺️ فتح الخريطة والرادار لاكتشاف الخدمات بدون إنترنت.\n\n💡 اكتب طلبك بالتفصيل أو اضغط الزر أدناه لإرسال طلب فوري!",
            chipLabel = "⚡ اطلب فني الآن",
            referralAction = "CREATE_SERVICE_ORDER",
            deepLinkRoute = "urgent_request_create",
            isOfflineMode = true
        )
        queryCache[queryLower] = defaultResp
        return defaultResp
    }

    private fun handleYemeniDialectAndCulture(queryLower: String, prompt: String, currentCity: String): AiResponse? {
        val isGreeting = queryLower.contains("يا حيا") || queryLower.contains("حياك") || queryLower.contains("حياكم") ||
                queryLower.contains("كيفك") || queryLower.contains("كيف حالك") || queryLower.contains("ارحب") ||
                queryLower.contains("مرحبا") || queryLower.contains("السلام عليكم") || queryLower.contains("هلا") ||
                queryLower.contains("صبحك") || queryLower.contains("مسيك")

        if (isGreeting) {
            return AiResponse(
                title = "يا حيا ومسهلا بأهل اليمن 🇾🇪",
                message = "يا أهلاً وسهلاً بك وبأصلك الطيب! أرحب تراحيب السيل. أنا مساعدك الذكي اليمني، رفيقك في حل أي عطل منزلي والوصول لأفضل الفنيين والمحلات في كل محافظات اليمن. كيف أخدمك اليوم؟",
                chipLabel = "🛠️ طلب خدمة جديدة",
                referralAction = "CREATE_SERVICE_ORDER",
                isOfflineMode = true
            )
        }

        if (queryLower.contains("ما بش ماء") || queryLower.contains("ما به ماء") || queryLower.contains("انقطع الماء") || queryLower.contains("الخزان فاضي")) {
            return AiResponse(
                title = "💧 حلول مشكلة انقطاع المياه في المنزل",
                message = "• تأكد أولاً من عمل الدينمو/الماطور ووصول التيار الكهربائي إليه.\n• قم بتنفيس الدينمو من برغي الهواء العلوي في حال سماع صوت دوران بدون رفع ماء.\n• افحص عوامة الخزان الأرضي والعلوي للتأكد من عدم تعليق السكر أو المحبس.\n• يمكنك طلب صهريج (وايت ماء) أو سباك معتمد فوراً لمعاينة الشبكة وتوصيل الخطوط.",
                chipLabel = "🛠️ طلب سباك معتمد",
                referralAction = "CREATE_SERVICE_ORDER",
                suggestedCategory = "سباكة",
                isOfflineMode = true,
                estimatedCost = "3,000 - 6,000 ريال يمني"
            )
        }

        if (queryLower.contains("سلتة") || queryLower.contains("فحسة") || queryLower.contains("زربيان") || queryLower.contains("مندي") || queryLower.contains("شاهي عدني") || queryLower.contains("مطاعم")) {
            return AiResponse(
                title = "🍲 المأكولات والمطاعم اليمنية الشهيرة",
                message = "أصل الكرم واللذاذة اليمنية! تتوفر في الدليل أفضل المطاعم الشعبية والمميزة لتقديم السلتة الصنعانية الحارة، الفحسة باللحم البلدي، الزربيان العدني الأصيل، المندي والمظبي، والشاهي العدني الملبن بالهيل.\n\nتصفح قسم المطاعم والكافيهات بالدليل لمشاهدة القوائم والأسعار وأرقام التوصيل المباشر!",
                chipLabel = "🍽️ تصفح المطاعم القريبة",
                referralAction = "EXPLORE_STORES",
                isOfflineMode = true
            )
        }

        if (queryLower.contains("صنعاء القديمة") || queryLower.contains("باب اليمن") || queryLower.contains("دار الحجر") || queryLower.contains("قلعة القاهرة") || queryLower.contains("المعلا") || queryLower.contains("كريتر") || queryLower.contains("شبام")) {
            return AiResponse(
                title = "🏛️ عبق التاريخ والمعالم اليمنية الخالدة",
                message = "اليمن موطن الحضارات وأول ناطحات سحاب طينية في العالم (شبام حضرموت)، وصنعاء القديمة بتراثها المعماري الفريد ونوافذ القمريات البديعة، وباب اليمن الشامخ، وقلعة القاهرة بتعز الحالمة، وصيرة والمعلا في ثغر اليمن الباسم (عدن). دليلنا يغطي كل هذه المدن العريقة ليخدم أهلها بكل فخر!",
                chipLabel = "🗺️ استكشف الخريطة",
                referralAction = "EXPLORE_MAP",
                isOfflineMode = true
            )
        }

        if (queryLower.contains("اسعار") || queryLower.contains("كم يكلف") || queryLower.contains("تسعيرة") || queryLower.contains("اجور")) {
            return AiResponse(
                title = "💰 قائمة الأسعار الاسترشادية للصيانة في اليمن",
                message = "• معاينة سباكة / كهرباء: *2,500 - 4,000 ريال يمني*\n• صيانة دينمو أو ماطور ماء: *3,000 - 6,000 ريال يمني*\n• شحن فريون وصيانة مكيف: *5,000 - 10,000 ريال يمني*\n• صيانة غسالة أو ثلاجة: *4,000 - 8,000 ريال يمني*\n• فحص وبرمجة منظومة طاقة شمسية: *4,000 - 8,000 ريال يمني*\n• رقعة بنشر واشتراك بطارية: *1,500 - 3,500 ريال يمني*\n\nالأسعار استرشادية لضمان الشفافية وحمايتك من الاستغلال، والاتفاق النهائي يتم بالتراضي مع الفني.",
                chipLabel = "⚡ طلب فني بالتسعيرة العادلة",
                referralAction = "CREATE_SERVICE_ORDER",
                isOfflineMode = true
            )
        }

        return null
    }

    /**
     * معالجة الاستعلام وفهم السياق ولهجات اليمن مع دعم الكاشينج
     */
    suspend fun queryAssistant(
        prompt: String,
        currentCity: String = "صنعاء",
        isOnlineAvailable: Boolean = false,
        onResult: (AiResponse) -> Unit
    ) {
        val resp = queryAssistantSync(prompt, currentCity)
        onResult(resp)
    }

    private fun fastLevenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        if (lhsLength == 0) return rhsLength
        if (rhsLength == 0) return lhsLength
        if (kotlin.math.abs(lhsLength - rhsLength) > 3) return 99

        var prev = IntArray(lhsLength + 1) { it }
        var curr = IntArray(lhsLength + 1)

        for (i in 1..rhsLength) {
            curr[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                curr[j] = min(min(prev[j] + 1, curr[j - 1] + 1), prev[j - 1] + match)
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[lhsLength]
    }
}
