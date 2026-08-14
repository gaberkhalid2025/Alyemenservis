package com.example.util

import com.example.utils.*

import android.content.Context
import androidx.annotation.Keep
import org.json.JSONObject
import java.io.InputStream
import kotlin.math.min

@Keep
data class AiResponse(
    val title: String = "",
    val message: String = "",
    val diySteps: List<String> = emptyList(),
    val chipLabel: String? = null,
    val suggestedCategory: String? = null,
    val referralAction: String? = null, // "REQUEST_TECHNICIAN", "EXPLORE_STORES", "OPEN_DEEP_LINK"
    val deepLinkRoute: String? = null,
    val isOfflineMode: Boolean = true
)

@Keep
data class TroubleshootingGuide(
    val id: String,
    val category: String,
    val keywords: List<String>,
    val title: String,
    val diySteps: List<String>,
    val referralAction: String,
    val suggestedCategory: String,
    val chipLabel: String
)

class AiAssistantEngine(private val context: Context) {

    private val guidesList = mutableListOf<TroubleshootingGuide>()
    private val faqList = mutableListOf<Pair<List<String>, String>>()

    init {
        loadOfflineData()
    }

    /**
     * Load JSON data from assets/ai_assistant_data.json
     */
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
                            referralAction = obj.optString("referral_action", "REQUEST_TECHNICIAN"),
                            suggestedCategory = obj.optString("suggested_category", ""),
                            chipLabel = obj.optString("chip_label", "طلب فني متخصص")
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

    /**
     * Process query using dual-mode: Try online contextual AI or fallback to offline keyword DIY engine
     */
    fun queryAssistant(
        prompt: String,
        currentCity: String = "صنعاء",
        isOnlineAvailable: Boolean = false,
        onResult: (AiResponse) -> Unit
    ) {
        val queryLower = prompt.trim().lowercase()

        if (queryLower.isBlank()) {
            onResult(
                AiResponse(
                    title = "مساعد الخدمات الذكي",
                    message = "مرحباً بك! يمكنك كتابة استفسارك عن الأعطال أو المساعدة في الطلبات لتقديم أفضل النصائح والفنيين المباشرين.",
                    isOfflineMode = !isOnlineAvailable
                )
            )
            return
        }

        // 1. Check Offline Keyword Guide matching
        var bestGuideMatch: TroubleshootingGuide? = null
        var maxScore = 0

        for (guide in guidesList) {
            var matchCount = 0
            for (kw in guide.keywords) {
                if (queryLower.contains(kw.lowercase())) {
                    matchCount += 2
                } else if (levenshteinDistance(queryLower, kw.lowercase()) <= 2) {
                    matchCount += 1
                }
            }
            if (matchCount > maxScore) {
                maxScore = matchCount
                bestGuideMatch = guide
            }
        }

        if (bestGuideMatch != null && maxScore >= 1) {
            onResult(
                AiResponse(
                    title = bestGuideMatch.title,
                    message = "إليك نصائح وإجراءات صيانة سريعة وآمنة يمكنك تجريبها بنفسك (DIY):",
                    diySteps = bestGuideMatch.diySteps,
                    chipLabel = bestGuideMatch.chipLabel,
                    suggestedCategory = bestGuideMatch.suggestedCategory,
                    referralAction = bestGuideMatch.referralAction,
                    deepLinkRoute = "urgent_request/${bestGuideMatch.suggestedCategory}",
                    isOfflineMode = true
                )
            )
            return
        }

        // 2. Check FAQ
        for ((kwList, answer) in faqList) {
            if (kwList.any { queryLower.contains(it.lowercase()) }) {
                onResult(
                    AiResponse(
                        title = "معلومات الخدمة",
                        message = answer,
                        chipLabel = "🛠️ إنشاء طلب جديد",
                        referralAction = "REQUEST_TECHNICIAN",
                        deepLinkRoute = "urgent_request_create",
                        isOfflineMode = true
                    )
                )
                return
            }
        }

        // 3. Generic fallback
        onResult(
            AiResponse(
                title = "المساعد الذكي",
                message = "بناءً على طلبك في مدينة $currentCity، نقترح عليك الاستعانة بأقرب المتخصصين في المهن والمحلات المجاوة للحصول على خدمة سريعة.",
                chipLabel = "🛠️ طلب أقرب فني لهذه المشكلة الآن",
                referralAction = "REQUEST_TECHNICIAN",
                deepLinkRoute = "urgent_request_create",
                isOfflineMode = true
            )
        )
    }

    /**
     * Levenshtein Distance algorithm for fuzzy tolerance
     */
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
