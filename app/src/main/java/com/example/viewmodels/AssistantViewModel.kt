package com.example.viewmodels

import com.example.utils.*

import androidx.lifecycle.ViewModel
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.PropertyEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AssistantCategory(val id: String, val titleAr: String, val icon: String) {
    object TechnicalIssue : AssistantCategory("TECHNICAL_ISSUE", "أعطال وصيانة فنية", "🛠️")
    object Restaurant : AssistantCategory("RESTAURANT", "مطاعم ومأكولات", "🍔")
    object Medical : AssistantCategory("MEDICAL", "خدمات طبية وصيدليات", "🩺")
    object Property : AssistantCategory("PROPERTY", "عقارات وإيجارات", "🏠")
    object Job : AssistantCategory("JOB", "وظائف وفرص عمل", "💼")
    object GeneralSupport : AssistantCategory("GENERAL_SUPPORT", "دعم فني واستفسارات", "ℹ️")
}

data class RecommendationCard(
    val id: String,
    val title: String,
    val categoryName: String,
    val phone: String,
    val rating: Double,
    val area: String,
    val isAvailable: Boolean,
    val imageUrl: String = "",
    val priceOrFee: String = "",
    val actionType: String = "CALL" // CALL, DIRECTIONS, BOOKING
)

data class AssistantResponse(
    val category: AssistantCategory,
    val textMessage: String,
    val initialFixSteps: List<String> = emptyList(),
    val recommendations: List<RecommendationCard> = emptyList(),
    val isOffline: Boolean = false
)

class AssistantViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf("مرحباً بك في المساعد الذكي لدليل اليمن! كيف يمكنني مساعدتك اليوم؟" to false)
    )
    val messages: StateFlow<List<Pair<String, Boolean>>> = _messages.asStateFlow()

    private val _lastResponse = MutableStateFlow<AssistantResponse?>(null)
    val lastResponse: StateFlow<AssistantResponse?> = _lastResponse.asStateFlow()

    fun normalizeArabic(text: String): String {
        var str = text.trim().lowercase()
        str = str.replace(Regex("[\\u064B-\\u0652]"), "")
        str = str.replace(Regex("[أإآ]"), "ا")
        str = str.replace("ى", "ي")
        str = str.replace("ة", "ه")
        return str
    }

    fun classifyUserIntent(prompt: String): AssistantCategory {
        val norm = normalizeArabic(prompt)
        return when {
            norm.contains("مكيف") || norm.contains("تبريد") || norm.contains("سباك") || 
            norm.contains("انسداد") || norm.contains("كهربا") || norm.contains("عطل") || 
            norm.contains("صيانه") || norm.contains("خربان") || norm.contains("مصلح") -> AssistantCategory.TechnicalIssue

            norm.contains("مطعم") || norm.contains("اكل") || norm.contains("وجبه") || 
            norm.contains("غداء") || norm.contains("عشاء") || norm.contains("كافيه") -> AssistantCategory.Restaurant

            norm.contains("دكتور") || norm.contains("طبيب") || norm.contains("مستشفي") || 
            norm.contains("عياده") || norm.contains("صيدليه") || norm.contains("اسنان") -> AssistantCategory.Medical

            norm.contains("عقار") || norm.contains("شقه") || norm.contains("ايجار") || 
            norm.contains("بيت") || norm.contains("ارض") || norm.contains("بيع") -> AssistantCategory.Property

            norm.contains("وظيفة") || norm.contains("عمل") || norm.contains("توظيف") || 
            norm.contains("تقديم") || norm.contains("سيره") -> AssistantCategory.Job

            else -> AssistantCategory.GeneralSupport
        }
    }

    fun processQuery(
        prompt: String,
        providers: List<ProviderEntity>,
        stores: List<StoreEntity>,
        properties: List<PropertyEntity>,
        isOnline: Boolean
    ): AssistantResponse {
        val category = classifyUserIntent(prompt)
        val norm = normalizeArabic(prompt)

        val recommendations = mutableListOf<RecommendationCard>()
        val fixSteps = mutableListOf<String>()

        when (category) {
            is AssistantCategory.TechnicalIssue -> {
                if (norm.contains("مكيف")) {
                    fixSteps.add("تأكد من تنظيف فلاتر الهواء الخارجية من الأتربة")
                    fixSteps.add("تحقق من ضبط درجة الحرارة في الريموت على 22 أو أقل مع تفعيل وضع التبريد (Cool)")
                    fixSteps.add("إذا استمر التنقيط أو صوت الضغط العالي، يرجى التواصل مع فني التكييف المعتمد فوراً")
                } else if (norm.contains("سباك") || norm.contains("انسداد")) {
                    fixSteps.add("قم بصب الماء المغلي مع القليل من الملح والخل في الحوض")
                    fixSteps.add("تأكد من إغلاق المحبس الرئيسي إذا كان هناك تسريب مياه")
                } else {
                    fixSteps.add("تأكد من فصل التيار الكهربائي الرئيسي في حال حدوث ماس أو شرارة")
                    fixSteps.add("استعن بالفني المخصص لتفادي الأعطال المعقدة")
                }

                // Filter matching technical providers
                val matched = providers.filter { p ->
                    val pName = normalizeArabic(p.name)
                    val pProf = normalizeArabic(p.profession)
                    val pSpec = normalizeArabic(p.specialization)
                    pName.contains(norm) || pProf.contains(norm) || pSpec.contains(norm) ||
                    norm.contains(pProf) || norm.contains(pSpec) || p.categoryId == "tech_maintenance"
                }.take(5)

                matched.forEach { p ->
                    recommendations.add(
                        RecommendationCard(
                            id = p.id,
                            title = p.name,
                            categoryName = p.specialization.ifEmpty { p.profession },
                            phone = p.phone,
                            rating = p.rating.toDouble(),
                            area = p.area,
                            isAvailable = p.isAvailable,
                            imageUrl = p.profileImage,
                            priceOrFee = "من ${p.previewPrice} ر.ي",
                            actionType = "CALL"
                        )
                    )
                }
            }

            is AssistantCategory.Restaurant -> {
                val matchedStores = stores.filter { s ->
                    s.categoryId == "restaurants" || normalizeArabic(s.name).contains("مطعم") || norm.contains(normalizeArabic(s.name))
                }.take(5)

                matchedStores.forEach { s ->
                    recommendations.add(
                        RecommendationCard(
                            id = s.id,
                            title = s.name,
                            categoryName = "مطعم ومأكولات",
                            phone = s.phone,
                            rating = s.rating.toDouble(),
                            area = s.localNeighborhood.ifEmpty { s.description },
                            isAvailable = s.isActive,
                            imageUrl = s.logoImage,
                            actionType = "DIRECTIONS"
                        )
                    )
                }
            }

            is AssistantCategory.Medical -> {
                val matchedProviders = providers.filter { p ->
                    val pProf = normalizeArabic(p.profession)
                    val pSpec = normalizeArabic(p.specialization)
                    pProf.contains("طبيب") || pProf.contains("دكتور") || pSpec.contains("اسنان") || pSpec.contains("طب")
                }.take(5)

                matchedProviders.forEach { p ->
                    recommendations.add(
                        RecommendationCard(
                            id = p.id,
                            title = p.name,
                            categoryName = p.specialization,
                            phone = p.phone,
                            rating = p.rating.toDouble(),
                            area = p.area,
                            isAvailable = p.isAvailable,
                            imageUrl = p.profileImage,
                            actionType = "BOOKING"
                        )
                    )
                }
            }

            is AssistantCategory.Property -> {
                val matchedProps = properties.take(5)
                matchedProps.forEach { prop ->
                    recommendations.add(
                        RecommendationCard(
                            id = prop.id,
                            title = prop.title,
                            categoryName = if (prop.type == "rent") "شقة/عقار للإيجار" else "عقار للبيع",
                            phone = prop.phone,
                            rating = prop.rating.toDouble(),
                            area = prop.localNeighborhood,
                            isAvailable = true,
                            priceOrFee = "${prop.price} ${prop.currency}",
                            actionType = "CALL"
                        )
                    )
                }
            }

            else -> {
                val topProviders = providers.filter { it.isVip || it.rating >= 4.5f }.take(3)
                topProviders.forEach { p ->
                    recommendations.add(
                        RecommendationCard(
                            id = p.id,
                            title = p.name,
                            categoryName = p.profession,
                            phone = p.phone,
                            rating = p.rating.toDouble(),
                            area = p.area,
                            isAvailable = p.isAvailable,
                            actionType = "CALL"
                        )
                    )
                }
            }
        }

        val textMsg = if (recommendations.isNotEmpty()) {
            "تم تحليل طلبك بنجاح ضمن تصنيف [${category.titleAr}]. إليك أفضل الخيارات والتوصيات المتاحة:"
        } else {
            "أهلاً بك! يمكنك طرح أي سؤال عن الصيانة الفنية، المطاعم، العيادات، العقارات والخدمات في اليمن."
        }

        val resp = AssistantResponse(
            category = category,
            textMessage = textMsg,
            initialFixSteps = fixSteps,
            recommendations = recommendations,
            isOffline = !isOnline
        )

        _lastResponse.value = resp
        return resp
    }
}
