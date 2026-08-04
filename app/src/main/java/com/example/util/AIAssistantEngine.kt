package com.example.util

import android.content.Context
import androidx.annotation.Keep
import com.example.data.CategoryEntity
import com.example.data.CityEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import org.json.JSONObject

@Keep
data class AssistantResponse(
    val title: String = "",
    val text: String,
    val actionLabel: String? = null,
    val actionRoute: String? = null,
    val isOnlineResult: Boolean = false,
    val suggestionChips: List<String> = emptyList()
)

@Keep
data class FAQItem(
    val keywords: List<String>,
    val title: String,
    val response: String,
    val actionLabel: String?,
    val actionRoute: String?
)

class AIAssistantEngine(private val context: Context) {

    private var welcomeMessage: String = "أهلاً بك في المساعد الذكي لدليل خدمات اليمن 🇾🇪! كيف يمكنني مساعدتك اليوم؟"
    private val suggestionsList = mutableListOf<String>()
    private val faqList = mutableListOf<FAQItem>()

    init {
        loadDataFromAssets()
    }

    private fun loadDataFromAssets() {
        try {
            val jsonStr = context.assets.open("ai_assistant_data.json").bufferedReader().use { it.readText() }
            val rootObj = JSONObject(jsonStr)

            welcomeMessage = rootObj.optString("welcome_message", welcomeMessage)

            val sugArray = rootObj.optJSONArray("suggestions")
            if (sugArray != null) {
                for (i in 0 until sugArray.length()) {
                    suggestionsList.add(sugArray.getString(i))
                }
            }

            val faqArray = rootObj.optJSONArray("faq")
            if (faqArray != null) {
                for (i in 0 until faqArray.length()) {
                    val obj = faqArray.getJSONObject(i)
                    val kwList = mutableListOf<String>()
                    val kwArr = obj.optJSONArray("keywords")
                    if (kwArr != null) {
                        for (j in 0 until kwArr.length()) {
                            kwList.add(kwArr.getString(j).lowercase())
                        }
                    }

                    faqList.add(
                        FAQItem(
                            keywords = kwList,
                            title = obj.optString("title"),
                            response = obj.optString("response"),
                            actionLabel = obj.optString("action_label", null),
                            actionRoute = obj.optString("action_route", null)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getWelcomeMessage(): String = welcomeMessage
    fun getDefaultSuggestions(): List<String> = suggestionsList.toList()

    fun processQuery(
        query: String,
        providers: List<ProviderEntity> = emptyList(),
        cities: List<CityEntity> = emptyList(),
        stores: List<StoreEntity> = emptyList(),
        categories: List<CategoryEntity> = emptyList(),
        isOnline: Boolean = true
    ): AssistantResponse {
        val cleanQuery = query.trim().lowercase()

        if (cleanQuery.isBlank()) {
            return AssistantResponse(
                text = "مرحباً بك! تفضل بكتابة استفسارك أو اختر من الاقتراحات أدناه.",
                suggestionChips = getDefaultSuggestions()
            )
        }

        // 1. Direct intent for Urgent Requests ("طلباتي" / "اطلب خدمتك الآن")
        if (cleanQuery.contains("عاجل") || cleanQuery.contains("طلباتي") || cleanQuery.contains("اطلب خدمتك") || cleanQuery.contains("طلب عاجل") || cleanQuery.contains("أبغى سباك الآن") || cleanQuery.contains("محتاج كهربائي الآن")) {
            return AssistantResponse(
                title = "نظام طلباتي والخدمات العاجلة ⚡",
                text = "يمكنك إنشاء طلب عاجل بنظام المزاد العكسي فوراً ليصل إلى جميع الفنيين المختصين والمحلات في مدينتك، وستصلك العروض مباشرة لاختيار الأفضل!",
                actionLabel = "اطلب خدمتك الآن ⚡",
                actionRoute = "CREATE_URGENT_REQUEST",
                suggestionChips = listOf("فتح شاشة طلباتي 📋", "كيف يعمل نظام طلباتي؟")
            )
        }

        // 2. Guided Online Search Engine (City + Category / Provider match)
        if (isOnline && providers.isNotEmpty()) {
            val matchedCity = cities.firstOrNull { city ->
                cleanQuery.contains(city.nameAr.lowercase()) || (city.nameEn.isNotBlank() && cleanQuery.contains(city.nameEn.lowercase()))
            }
            val matchedCat = categories.firstOrNull { cat ->
                cleanQuery.contains(cat.name.lowercase())
            }

            // Specific category / search in city
            if (matchedCat != null || matchedCity != null) {
                var matches = providers.filter { p -> !p.isBlocked && !p.isDeleted }
                if (matchedCity != null) {
                    matches = matches.filter { it.cityId == matchedCity.id || it.area.contains(matchedCity.nameAr, ignoreCase = true) }
                }
                if (matchedCat != null) {
                    matches = matches.filter { it.categoryId == matchedCat.id || it.profession.contains(matchedCat.name, ignoreCase = true) }
                }

                if (matches.isNotEmpty()) {
                    val top3 = matches.sortedByDescending { it.rating }.take(3)
                    val sb = StringBuilder()
                    sb.append("عثرت لك على أحدث الفنيين المتاحين")
                    if (matchedCat != null) sb.append(" في قسم (${matchedCat.name})")
                    if (matchedCity != null) sb.append(" بمدينة (${matchedCity.nameAr})")
                    sb.append(":\n\n")

                    top3.forEachIndexed { idx, p ->
                        val priceTxt = if (p.previewPrice > 0) " (معاينة: ${p.previewPrice.toInt()} ر.ي)" else ""
                        sb.append("${idx + 1}. *${p.name}* ⭐ ${p.rating} | 📍 ${p.area}$priceTxt\n")
                    }

                    sb.append("\nيمكنك فتح القسم لمشاهدة جميع التقييمات والاتصال المباشر.")

                    return AssistantResponse(
                        title = "نتائج البحث المباشرة 🔍",
                        text = sb.toString(),
                        actionLabel = "عرض كل نتائج القسم 🚀",
                        actionRoute = matchedCat?.id ?: "EXPLORE_PROVIDERS",
                        isOnlineResult = true,
                        suggestionChips = listOf("مقارنة الأسعار 💰", "اطلب خدمتك الآن ⚡")
                    )
                }
            }

            // Store working hours lookup
            if (cleanQuery.contains("دوام") || cleanQuery.contains("ساعات العمل") || cleanQuery.contains("متى يفتح")) {
                val matchedStore = stores.firstOrNull { cleanQuery.contains(it.name.lowercase()) }
                if (matchedStore != null) {
                    return AssistantResponse(
                        title = "أوقات الدوام ⏰",
                        text = "محل *${matchedStore.name}*:\n📍 الموقع: ${matchedStore.localNeighborhood}\n⏰ أوقات الدوام: ${matchedStore.workingHours}\n⭐ التقييم: ${matchedStore.rating}",
                        actionLabel = "فتح بطاقة المحل 🏪",
                        actionRoute = "STORE_DETAILS_${matchedStore.id}",
                        isOnlineResult = true,
                        suggestionChips = listOf("تصفح جميع المحلات 🏪", "طلب خدمة عاجلة ⚡")
                    )
                }
            }
        }

        // 3. Offline Keyword Match Engine (Decision Rules)
        var bestFaq: FAQItem? = null
        var maxScore = 0

        for (faq in faqList) {
            var score = 0
            for (kw in faq.keywords) {
                if (cleanQuery.contains(kw)) {
                    score += 1
                }
            }
            if (score > maxScore) {
                maxScore = score
                bestFaq = faq
            }
        }

        if (bestFaq != null && maxScore > 0) {
            return AssistantResponse(
                title = bestFaq.title,
                text = bestFaq.response,
                actionLabel = bestFaq.actionLabel,
                actionRoute = bestFaq.actionRoute,
                isOnlineResult = false,
                suggestionChips = listOf("اطلب خدمتك الآن ⚡", "انضمام الفنيين 📝", "حلول المشاكل ⚙️")
            )
        }

        // 4. Default Fallback
        return AssistantResponse(
            title = "المساعد الذكي 🤖",
            text = "أهلاً بك! لم أفهم طلبك بدقة، لكن يمكنك استخدام خيارات البحث المباشر في التطبيق، أو طلب خدمة عاجلة فورية عبر زر 'اطلب خدمتك الآن ⚡' لتصلك عروض الفنيين والمحلات فوراً.",
            actionLabel = "اطلب خدمتك الآن ⚡",
            actionRoute = "CREATE_URGENT_REQUEST",
            suggestionChips = getDefaultSuggestions()
        )
    }
}
