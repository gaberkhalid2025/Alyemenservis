package com.example.utils

import com.example.data.AdminSettingsEntity
import com.example.data.CategoryEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.assistant.AssistantMessage
import java.util.Locale

data class AssistantDirectorySnapshot(
    val providers: List<ProviderEntity>,
    val stores: List<StoreEntity>,
    val properties: List<PropertyEntity>,
    val categories: List<CategoryEntity>,
    val settings: AdminSettingsEntity,
    val currentCityName: String
)

/**
 * 🧠 AssistantBrain
 * واجهة موحّدة لمحرك المساعد الذكي تمنع الازدواجية وتضمن أولوية تحذيرات السلامة العالية.
 */
interface AssistantBrain {
    suspend fun answer(
        prompt: String,
        isOnline: Boolean,
        snapshot: AssistantDirectorySnapshot
    ): AssistantMessage
}

/**
 * 🇾🇪 LocalGuideBrain
 * محرك موحد يجمع اللهجة اليمنية، أدلة AiAssistantEngine المعتمدة، وبحث الدليل المحلي
 * بدون أي استهلاك لحصة Firebase Spark أو مفاتيح مدفوعة مهما زاد عدد المستخدمين.
 */
class LocalGuideBrain(
    private val engine: AiAssistantEngine?
) : AssistantBrain {

    private val yemeniDialectMap = linkedMapOf(
        "ابغى" to "اريد",
        "ابا" to "اريد",
        "باغي" to "اريد",
        "اشتي" to "اريد",
        "شتي" to "اريد",
        "اشتيك" to "اريد",
        "ايش" to "ما",
        "ماهو" to "ما",
        "فين" to "اين",
        "وين" to "اين",
        "بكم" to "ما سعر",
        "بكام" to "ما سعر",
        "ثلاجه" to "صيانة ثلاجات",
        "غساله" to "صيانة غسالات",
        "سخان" to "صيانة سخانات",
        "مكيف" to "صيانة مكيفات",
        "بزبوز" to "سباك حنفية",
        "دينمو" to "سباك ماطور ماء",
        "ماطور" to "ماطور ماء سباك",
        "كهربا" to "كهربائي",
        "بنشري" to "بنشري اطارات",
        "ميكانيكي" to "ميكانيكي سيارات",
        "سمكري" to "سمكري سيارات",
        "صيدليه" to "صيدلية",
        "دواء" to "صيدلية",
        "علاج" to "صيدلية",
        "دكتور" to "مركز طبي",
        "مستشفى" to "مركز طبي",
        "عياده" to "مركز طبي",
        "شقه" to "شقة",
        "بيت" to "منزل",
        "عمارة" to "عقار",
        "حوش" to "عقار",
        "حده" to "صنعاء حدة",
        "السبعين" to "صنعاء السبعين",
        "شميله" to "صنعاء شميلة",
        "التحرير" to "صنعاء التحرير",
        "مذبح" to "صنعاء مذبح",
        "الستين" to "صنعاء الستين"
    )

    fun translateDialect(rawQuery: String): String {
        var translated = normalizeArabic(rawQuery)
        yemeniDialectMap.forEach { (dialect, standard) ->
            if (translated.contains(dialect)) {
                translated = translated.replace(dialect, standard)
            }
        }
        return translated
    }

    override suspend fun answer(
        prompt: String,
        isOnline: Boolean,
        snapshot: AssistantDirectorySnapshot
    ): AssistantMessage = answerSync(prompt, isOnline, snapshot)

    fun answerSync(
        prompt: String,
        isOnline: Boolean,
        snapshot: AssistantDirectorySnapshot
    ): AssistantMessage {
        val resp: AiResponse? = engine?.queryAssistantSync(
            prompt = prompt,
            currentCity = snapshot.currentCityName.ifBlank { "صنعاء" },
            isOnlineAvailable = isOnline
        )

        val matchedEntities = findMatchingEntities(prompt, snapshot)
        if (resp != null && hasRichGuideContent(resp)) {
            val isHigh = resp.safetyLevel.equals("HIGH", ignoreCase = true)
            val sb = StringBuilder()
            if (isHigh) {
                sb.append("🚨 *${resp.title}*\n\n")
                resp.safetyWarning?.let { sb.append("⚠️ $it\n\n") }
                if (resp.immediateActions.isNotEmpty()) {
                    sb.append("🛡️ خطوات الأمان الفورية:\n")
                    resp.immediateActions.forEachIndexed { idx, act -> sb.append("${idx + 1}. $act\n") }
                }
            } else {
                sb.append("🤖 *${resp.title}*\n\n")
                if (resp.message.isNotBlank()) sb.append("${resp.message}\n\n")
                if (resp.diySteps.isNotEmpty()) {
                    sb.append("📋 خطوات الفحص السريع:\n")
                    resp.diySteps.forEachIndexed { idx, step -> sb.append("${idx + 1}. $step\n") }
                }
            }
            resp.preventiveTips?.takeIf { it.isNotBlank() }?.let { sb.append("\n💡 نصيحة وقائية: $it\n") }
            resp.estimatedCost?.takeIf { it.isNotBlank() }?.let { sb.append("\n💰 التكلفة التقديرية: $it\n") }

            val related = if (matchedEntities.isNotEmpty()) matchedEntities else findProvidersForCategory(resp.suggestedCategory.orEmpty(), snapshot.providers)
            if (related.isNotEmpty()) {
                sb.append("\n👇 عثرت لك على نتائج معتمدة في دليلك:")
            }
            return AssistantMessage(
                text = sb.toString().trim(),
                isUser = false,
                matchedEntities = related,
                safetyLevel = resp.safetyLevel,
                safetyWarning = resp.safetyWarning,
                immediateActions = resp.immediateActions
            )
        }

        return buildDirectoryOrFaqFallback(prompt, matchedEntities, snapshot)
    }

    private fun hasRichGuideContent(resp: AiResponse): Boolean {
        val isSpecific = resp.title.startsWith("📋") || resp.title == "معلومات الخدمة المعتمدة"
        return resp.safetyLevel.equals("HIGH", ignoreCase = true) ||
            resp.diySteps.isNotEmpty() ||
            resp.immediateActions.isNotEmpty() ||
            !resp.estimatedCost.isNullOrBlank() ||
            (isSpecific && resp.message.isNotBlank())
    }

    private fun findProvidersForCategory(catRaw: String, providers: List<ProviderEntity>): List<Any> {
        val cat = normalizeArabic(catRaw)
        if (cat.isBlank()) return emptyList()
        return providers.filter { p ->
            normalizeArabic(p.profession).contains(cat) ||
                normalizeArabic(p.specialization).contains(cat)
        }.take(3)
    }

    private fun findMatchingEntities(prompt: String, snapshot: AssistantDirectorySnapshot): List<Any> {
        val qNorm = normalizeArabic(prompt)
        val translated = translateDialect(prompt)
        val matched = mutableListOf<Any>()

        val professions = listOf(
            "سباك", "كهربا", "دهان", "نجار", "حداد", "خياط", "سائق", "مصلح",
            "صيانه", "فني", "مهندس", "تكييف", "تبريد", "بناء", "مقاول", "طبيب",
            "تنظيف", "ميكانيك", "غساله", "غسالات", "ثلاجه", "انفرتر", "شمسيه"
        )
        if (professions.any { qNorm.contains(it) || translated.contains(it) }) {
            matched.addAll(snapshot.providers.filter { p ->
                val pName = normalizeArabic(p.name)
                val pProf = normalizeArabic(p.profession)
                val pSpec = normalizeArabic(p.specialization)
                pName.contains(qNorm) ||
                    (pProf.isNotBlank() && (pProf.contains(qNorm) || qNorm.contains(pProf) || translated.contains(pProf))) ||
                    (pSpec.isNotBlank() && (pSpec.contains(qNorm) || qNorm.contains(pSpec) || translated.contains(pSpec)))
            }.take(3))
        }

        val storeKeys = listOf("مطعم", "اكل", "غداء", "عشاء", "كافيه", "قهوه", "محل", "متجر", "سوبر", "مول", "مركز تجاري", "صيدليه", "مستشفى", "عياده", "طبي")
        if (storeKeys.any { qNorm.contains(it) || translated.contains(it) } || qNorm.contains("شراء")) {
            matched.addAll(snapshot.stores.filter { s ->
                val sName = normalizeArabic(s.name)
                val sDesc = normalizeArabic(s.description)
                sName.contains(qNorm) || sDesc.contains(qNorm) || storeKeys.any { k -> qNorm.contains(k) && (sName.contains(k) || sDesc.contains(k)) }
            }.take(3))
        }

        if (qNorm.contains("بيت") || qNorm.contains("شقه") || qNorm.contains("عقار") || qNorm.contains("ايجار") || qNorm.contains("بيع")) {
            matched.addAll(snapshot.properties.filter { pr ->
                normalizeArabic(pr.title).contains(qNorm) || normalizeArabic(pr.description).contains(qNorm)
            }.take(3))
        }
        return matched.distinct()
    }

    private fun buildDirectoryOrFaqFallback(
        prompt: String,
        matchedEntities: List<Any>,
        snapshot: AssistantDirectorySnapshot
    ): AssistantMessage {
        val qNorm = normalizeArabic(prompt)
        if ((qNorm.contains("غساله") || qNorm.contains("غسالات")) &&
            (qNorm.contains("سعر") || qNorm.contains("كم") || qNorm.contains("تكلفه") || qNorm.contains("صيانه"))
        ) {
            return AssistantMessage(
                text = "🧺 متوسط تكلفة معاينة وصيانة الغسالات في دليل خدمات اليمن يتراوح بين 4,000 إلى 9,000 ريال يمني (حسب نوع الغسالة والعطل وقطع الغيار). يمكنك طلب فني صيانة غسالات معتمد للمعاينة المباشرة!",
                isUser = false,
                matchedEntities = matchedEntities
            )
        }

        if (matchedEntities.isNotEmpty()) {
            val sb = StringBuilder("🔍 عثرت لك على النتائج التالية في دليل خدمات اليمن:\n\n")
            matchedEntities.forEachIndexed { index, ent ->
                when (ent) {
                    is ProviderEntity -> {
                        val catName = snapshot.categories.find { it.id == ent.categoryId }?.name ?: ent.profession
                        sb.append("${index + 1}. *${ent.name}* (فني) | $catName\n")
                        sb.append("   📍 ${ent.area} | ⭐ ${ent.rating} | ${if (ent.isAvailable) "🟢 متاح" else "🔴 مشغول"}\n\n")
                    }
                    is StoreEntity -> {
                        sb.append("${index + 1}. *${ent.name}* (متجر/منشأة)\n")
                        sb.append("   📍 ${ent.localNeighborhood.ifBlank { ent.cityId }} | ⭐ ${ent.rating}\n\n")
                    }
                    is PropertyEntity -> {
                        sb.append("${index + 1}. *${ent.title}* (عقار)\n")
                        sb.append("   📍 ${ent.localNeighborhood.ifBlank { ent.cityId }} | 💰 ${ent.price} ${ent.currency}\n\n")
                    }
                }
            }
            sb.append("💡 انقر على البطاقة بالأسفل لمزيد من التفاصيل!")
            return AssistantMessage(text = sb.toString(), isUser = false, matchedEntities = matchedEntities)
        }

        val isSupportContact = qNorm.contains("رقم") || qNorm.contains("اتصال") ||
            qNorm.contains("دعم") || qNorm.contains("تواصل") || qNorm.contains("واتساب")
        val isJoinRequest = qNorm.contains("تسجيل") || qNorm.contains("انضم") || qNorm.contains("حساب")
        val isPriceInfo = qNorm.contains("سعر") || qNorm.contains("رسوم") || qNorm.contains("مجاني")
        val isMapFeature = qNorm.contains("خريطه") || qNorm.contains("موقع") || qNorm.contains("gps")

        val textResult = when {
            qNorm.contains("مرحبا") || qNorm.contains("السلام") || qNorm.contains("هلا") || qNorm.contains("صباح") || qNorm.contains("مساء") -> {
                "أهلاً وسهلاً بك في دليل خدمات اليمن 🇾🇪! أنا مساعدك الذكي المدمج لمساعدتك في الوصول لأفضل الفنيين، المحلات، العيادات، والمطاعم في ثوانٍ معدودة. كيف يمكنني مساعدتك اليوم؟"
            }
            isSupportContact -> {
                "📱 للتواصل المباشر والآمن مع الدعم الفني للدليل:\n- هاتف الإدارة: *${snapshot.settings.supportPhone}*\n- واتساب الدعم الفني المباشر: *${snapshot.settings.supportWhatsapp}*\nنحن هنا لخدمتك على مدار الساعة!"
            }
            isJoinRequest -> {
                "📝 للانضمام كفني محترف أو صاحب متجر/خدمة في الدليل وعرض خدماتك مجاناً، يرجى فتح القائمة الجانبية للتطبيق واختيار 'طلب الانضمام' وتعبئة البيانات!"
            }
            isPriceInfo || qNorm.contains("تكلفه") -> {
                "💰 استخدام تطبيق دليل خدمات اليمن مجاني تماماً للبحث وتصفح الفنيين والمحلات. أسعار المعاينة والصيانة تبدأ من 3,000 ريال يمني وتحدد التكلفة الإجمالية للعمل بالتراضي المباشر."
            }
            isMapFeature || qNorm.contains("رادار") -> {
                "🗺️ يمكنك النقر على 'خريطة الخدمات' لعرض مواقع مقدمي الخدمات القريبين منك جغرافياً وبلاغات الطريق دون استهلاك يذكر للبيانات!"
            }
            qNorm.contains("عدن") || qNorm.contains("صنعاء") || qNorm.contains("تعز") || qNorm.contains("حضرموت") || qNorm.contains("الحديده") || qNorm.contains("اب") || qNorm.contains("مارب") || qNorm.contains("ذمار") -> {
                "🇾🇪 دليل خدمات اليمن يغطي جميع المحافظات والمناطق اليمنية الرئيسية لتسهيل العثور على أقرب فني ومحل معتمد في منطقتك السكنية!"
            }
            qNorm.contains("طوارئ") || qNorm.contains("اسعاف") || qNorm.contains("دفاع") || qNorm.contains("حريق") || qNorm.contains("شرطه") -> {
                "🚨 أرقام الطوارئ العامة الهامة في اليمن:\n- الدفاع المدني والإطفاء: *191*\n- طوارئ الكهرباء: *195*\n- طوارئ المياه والصرف الصحي: *192*\n- إسعاف الهلال الأحمر والنجدة: *199*\n\nيرجى التواصل المباشر مع هذه الجهات الرسمية فوراً في حالات الطوارئ الحيوية!"
            }
            qNorm.contains("دفع") || qNorm.contains("كاش") || qNorm.contains("فلوس") || qNorm.contains("محفظه") -> {
                "💳 يتيح تطبيقنا الدفع نقداً (كاش) مباشرة للفني، أو عبر المحافظ الإلكترونية اليمنية المعتمدة بالتنسيق المباشر مع مقدم الخدمة."
            }
            qNorm.contains("تقييم") || qNorm.contains("شكوى") || qNorm.contains("شكاوى") || qNorm.contains("سيء") -> {
                "⭐ نحن نهتم بجودة الخدمات جداً! يمكنك تقييم الفني بنجوم وتوضيح تجربتك بعد إتمام الطلب مباشرة من شاشة الحجوزات، أو مراسلة الدعم الفني وسنتابع الشكوى فوراً."
            }
            else -> {
                "عذراً، لم أتمكن من فهم استفسارك بدقة (\"$prompt\"). يمكنك كتابة نوع العطل أو المهنة المطلوبة بوضوح مثل: (أحتاج سباك، كهربائي، صيانة غسالة، مطعم، عقار، دكتور) أو الضغط على زر '⚡ اطلب الآن' لإرسال طلب فوري."
            }
        }
        return AssistantMessage(text = textResult, isUser = false, matchedEntities = emptyList())
    }

    companion object {
        fun normalizeArabic(text: String): String {
            var str = text.trim().lowercase(Locale.ROOT)
            str = str.replace(Regex("[\\u064B-\\u0652]"), "")
            str = str.replace(Regex("[أإآ]"), "ا")
            str = str.replace("ى", "ي")
            str = str.replace("ة", "ه")
            return str
        }
    }
}
