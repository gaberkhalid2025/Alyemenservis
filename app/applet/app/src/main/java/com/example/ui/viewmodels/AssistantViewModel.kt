package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminSettingsEntity
import com.example.data.CategoryEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.PropertyEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.assistant.AssistantMessage
import com.example.utils.YemeniDialectFormatter
import com.example.utils.YemeniSynonyms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * 🤖 Yaman AI Local Pro — AssistantViewModel
 * الحل الهندسي المستدام 100% (أمان تام، بدون مفاتيح API، بدون خوادم، يعمل بدون إنترنت، حجم إضافي أقل من 100 كيلوبايت):
 * - الطبقة الأولى: البحث المحلي الذكي في قاعدة البيانات (Local RAG) مع دعم المرادفات واللهجة اليمنية.
 * - الطبقة الثالثة: الإجابة الافتراضية الآمنة (Fallback) لضمان عدم تعطل التطبيق أبداً.
 */
class AssistantViewModel : ViewModel() {

    private val defaultWelcome = YemeniDialectFormatter.format("حياك الله ومية هلا يا غالي في دليل خدمات اليمن 🇾🇪! أنا مساعدك الذكي المحلي، جاهز أبحث لك فوراً في قاعدة بيانات التطبيق عن أي فني، رقم هاتف، محل، مطعم، أو عقار. وش تطلب اليوم؟")

    private val _chatHistory = MutableStateFlow<List<AssistantMessage>>(
        listOf(AssistantMessage(text = defaultWelcome, isUser = false))
    )
    val chatHistory: StateFlow<List<AssistantMessage>> = _chatHistory.asStateFlow()

    private val _typedText = MutableStateFlow("")
    val typedText: StateFlow<String> = _typedText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val cachedResponses = mutableMapOf<String, AssistantMessage>()

    fun updateTypedText(text: String) {
        _typedText.value = text
    }

    fun clearChat(welcomeMsg: String = defaultWelcome) {
        _chatHistory.value = listOf(AssistantMessage(text = welcomeMsg.ifEmpty { defaultWelcome }, isUser = false))
        cachedResponses.clear()
    }

    private fun getCachedResponse(query: String): AssistantMessage? {
        return cachedResponses[normalizeArabic(query)]
    }

    private fun cacheResponse(query: String, response: AssistantMessage) {
        cachedResponses[normalizeArabic(query)] = response
    }

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
        if (updatedHistory.size > 100) {
            updatedHistory = updatedHistory.takeLast(100)
        }
        _chatHistory.value = updatedHistory
        _typedText.value = ""
        _isGenerating.value = true

        val cached = getCachedResponse(prompt)
        if (cached != null) {
            viewModelScope.launch {
                _chatHistory.value = _chatHistory.value + cached
                _isGenerating.value = false
                if (settings.allowTextToSpeechAssistant) {
                    onSpeechSpeak?.invoke(cached.text)
                }
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val providers = mainViewModel.providers.value
                val stores = mainViewModel.stores.value
                val properties = mainViewModel.properties.value
                val categories = mainViewModel.categories.value

                val responseMsg = generateLocalRagResponse(prompt, providers, stores, properties, categories, settings)

                withContext(Dispatchers.Main) {
                    var updatedHistoryWithResp = _chatHistory.value + responseMsg
                    if (updatedHistoryWithResp.size > 100) {
                        updatedHistoryWithResp = updatedHistoryWithResp.takeLast(100)
                    }
                    _chatHistory.value = updatedHistoryWithResp
                    _isGenerating.value = false
                    cacheResponse(prompt, responseMsg)

                    if (settings.allowTextToSpeechAssistant) {
                        onSpeechSpeak?.invoke(responseMsg.text)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    val fallbackMsg = AssistantMessage(
                        text = YemeniDialectFormatter.format("ابشر ولا تشيل هم يا غالي! حصل التماس بسيط، جرب تكرار سؤالك أو تواصل مع الدعم الفني مباشرة على الرقم: ${settings.supportPhone}"),
                        isUser = false
                    )
                    _chatHistory.value = _chatHistory.value + fallbackMsg
                }
            }
        }
    }

    private fun generateLocalRagResponse(
        prompt: String,
        providers: List<ProviderEntity>,
        stores: List<StoreEntity>,
        properties: List<PropertyEntity>,
        categories: List<CategoryEntity>,
        settings: AdminSettingsEntity
    ): AssistantMessage {
        val qNorm = normalizeArabic(prompt)
        val expandedKeywords = YemeniSynonyms.expandQuery(prompt).map { kw: String -> normalizeArabic(kw) }
        val matchedEntities = mutableListOf<Any>()

        // 1. Search Providers & Artisans
        val matchedProvs = providers.filter { p: ProviderEntity ->
            val pName = normalizeArabic(p.name)
            val pProf = normalizeArabic(p.profession)
            val pSpec = normalizeArabic(p.specialization)
            val pArea = normalizeArabic(p.area)
            val pHood = normalizeArabic(p.localNeighborhood)

            expandedKeywords.any { kw ->
                pName.contains(kw) || pProf.contains(kw) || pSpec.contains(kw) || pArea.contains(kw) || pHood.contains(kw)
            } || qNorm.contains("فني") || qNorm.contains("معلم")
        }.take(8)

        if (matchedProvs.isNotEmpty()) {
            matchedEntities.addAll(matchedProvs)
            val sb = StringBuilder()
            sb.append("حياك الله يا غالي وأبشر بالعين والراس! 🇾🇪 عثرت لك في قاعدة بيانات التطبيق على الفنيين التاليين:\n\n")
            matchedProvs.forEachIndexed { idx, p ->
                val catName = categories.find { it.id == p.categoryId }?.name ?: p.profession
                sb.append("${idx + 1}. *${p.name}* (${p.specialization.ifEmpty { p.profession }})\n")
                sb.append("   📞 هاتف التواصل: *${p.phone}*\n")
                sb.append("   📍 الموقع: ${p.area} - ${p.localNeighborhood}\n")
                sb.append("   ⭐ التقييم: ${p.rating}/5.0 | ${if (p.isAvailable) "🟢 متاح الآن" else "🔴 مشغول"}\n\n")
            }
            sb.append("💡 انقر على بطاقة الفني بالأسفل للتواصل المباشر أو اطلب الخدمة فوراً!")
            return AssistantMessage(text = sb.toString(), isUser = false, matchedEntities = matchedEntities)
        }

        // 2. Search Stores, Restaurants & Medical Centers
        val matchedStores = stores.filter { s: StoreEntity ->
            val sName = normalizeArabic(s.name)
            val sDesc = normalizeArabic(s.description)
            val sHood = normalizeArabic(s.localNeighborhood)
            expandedKeywords.any { kw -> sName.contains(kw) || sDesc.contains(kw) || sHood.contains(kw) }
        }.take(6)

        if (matchedStores.isNotEmpty()) {
            matchedEntities.addAll(matchedStores)
            val sb = StringBuilder()
            sb.append("ابشر يا غالي! لقيت لك هذه المحلات والمراكز في قاعدة البيانات:\n\n")
            matchedStores.forEachIndexed { idx, s ->
                sb.append("${idx + 1}. *${s.name}*\n")
                sb.append("   📍 العنوان: ${s.localNeighborhood}\n")
                sb.append("   ⭐ التقييم: ${s.rating}/5.0\n\n")
            }
            sb.append("💡 انقر على البطاقة أدناه لعرض التفاصيل الكاملة!")
            return AssistantMessage(text = sb.toString(), isUser = false, matchedEntities = matchedEntities)
        }

        // 3. Search Properties
        val matchedProps = properties.filter { pr: PropertyEntity ->
            val prTitle = normalizeArabic(pr.title)
            val prDesc = normalizeArabic(pr.description)
            expandedKeywords.any { kw -> prTitle.contains(kw) || prDesc.contains(kw) }
        }.take(5)

        if (matchedProps.isNotEmpty()) {
            matchedEntities.addAll(matchedProps)
            val sb = StringBuilder()
            sb.append("حياك الله، إليك العقارات المتوفرة في النظام:\n\n")
            matchedProps.forEachIndexed { idx, pr ->
                sb.append("${idx + 1}. *${pr.title}*\n")
                sb.append("   📍 ${pr.localNeighborhood} | 💰 ${pr.price} ${pr.currency}\n\n")
            }
            sb.append("💡 انقر على البطاقة أدناه للتفاصيل!")
            return AssistantMessage(text = sb.toString(), isUser = false, matchedEntities = matchedEntities)
        }

        // 4. Emergency / Contact Info / General App Queries
        val textResult = when {
            qNorm.contains("دعم") || qNorm.contains("اتصال") || qNorm.contains("رقم") || qNorm.contains("واتساب") -> {
                "📱 للتواصل المباشر مع الدعم الفني لدليل خدمات اليمن:\n- هاتف الإدارة: *${settings.supportPhone}*\n- واتساب الدعم الفني: *${settings.supportWhatsapp}*\nنحن بخدمتك على مدار الساعة يا غالي!"
            }
            qNorm.contains("طوارئ") || qNorm.contains("اسعاف") || qNorm.contains("شرطه") || qNorm.contains("حريق") -> {
                "🚨 أرقام الطوارئ الهامة في اليمن:\n- الدفاع المدني: *191*\n- طوارئ الكهرباء: *195*\n- طوارئ المياه: *192*\n- الإسعاف والنجدة: *199*\nحفظكم الله من كل مكروه!"
            }
            else -> {
                // الطبقة الثالثة: الإجابة الافتراضية الآمنة (Fallback)
                YemeniDialectFormatter.format("عذراً يا غالي، ما لقيت مطابقة مباشرة لطلبك في قاعدة بيانات التطبيق الحالية. يمكنك كتابة اسم المهنة بوضوح (مثل: سباك، كهربائي، طبيب، مطعم) أو التواصل مع الدعم الفني على الرقم: ${settings.supportPhone} أبشر بمن يخدمك!")
            }
        }

        return AssistantMessage(text = textResult, isUser = false, matchedEntities = emptyList())
    }

    /**
     * Backward compatibility wrapper for tests
     */
    fun generateLocalOfflineResponse(prompt: String, viewModel: MainViewModel): Pair<String, List<Any>> {
        val providers = viewModel.providers.value
        val stores = viewModel.stores.value
        val properties = viewModel.properties.value
        val categories = viewModel.categories.value
        val settings = viewModel.settings.value
        val msg = generateLocalRagResponse(prompt, providers, stores, properties, categories, settings)
        return Pair(msg.text, msg.matchedEntities)
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
