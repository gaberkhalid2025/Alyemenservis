package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminSettingsEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.assistant.AssistantMessage
import com.example.utils.AiAssistantEngine
import com.example.utils.AssistantDirectorySnapshot
import com.example.utils.LocalGuideBrain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 🤖 AssistantViewModel
 * يدير جلسة المساعد الذكي بالكامل عبر المحرك الموحد [LocalGuideBrain] و[AiAssistantEngine]
 * مجاناً وبلا حدود على حصة Spark أو مفاتيح مدفوعة، مع كاش محلي للردود المتكررة.
 */
class AssistantViewModel : ViewModel() {

    private var cachedEngine: AiAssistantEngine? = null
    private var cachedBrain: LocalGuideBrain? = null

    private fun getOrCreateBrain(context: Context?): LocalGuideBrain {
        val currentBrain = cachedBrain
        if (currentBrain != null && (cachedEngine != null || context == null)) {
            return currentBrain
        }
        return synchronized(this) {
            if (cachedEngine == null && context != null) {
                cachedEngine = AiAssistantEngine(context.applicationContext)
                cachedBrain = LocalGuideBrain(cachedEngine)
            } else if (cachedBrain == null) {
                cachedBrain = LocalGuideBrain(cachedEngine)
            }
            cachedBrain!!
        }
    }

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

    fun searchLocalSmart(rawQuery: String): String {
        return getOrCreateBrain(null).translateDialect(rawQuery)
    }

    fun callOptionalServerFallback(
        serverEndpoint: String?,
        prompt: String,
        onResult: (String) -> Unit,
        onFallbackToLocal: () -> Unit
    ) {
        if (serverEndpoint.isNullOrBlank()) {
            onFallbackToLocal()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val optimizedQuery = searchLocalSmart(prompt)
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val jsonBody = JSONObject().apply {
                    put("model", "jais-900m")
                    put("prompt", optimizedQuery)
                    put("stream", false)
                }
                val request = Request.Builder()
                    .url(serverEndpoint)
                    .post(jsonBody.toString().toRequestBody(mediaType))
                    .build()

                sharedHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val jsonObj = JSONObject(bodyStr)
                        val text = jsonObj.optString("response").ifEmpty {
                            jsonObj.optString("text").ifEmpty {
                                jsonObj.optJSONArray("choices")?.optJSONObject(0)?.optString("text") ?: ""
                            }
                        }
                        if (text.isNotBlank()) {
                            withContext(Dispatchers.Main) { onResult(text) }
                            return@use
                        }
                    }
                    withContext(Dispatchers.Main) { onFallbackToLocal() }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onFallbackToLocal() }
            }
        }
    }

    fun clearChat(welcomeMsg: String = defaultWelcome) {
        _chatHistory.value = listOf(AssistantMessage(text = welcomeMsg.ifEmpty { defaultWelcome }, isUser = false))
    }

    fun sendUserQuery(
        prompt: String,
        isOnline: Boolean,
        mainViewModel: MainViewModel,
        settings: AdminSettingsEntity,
        onSpeechSpeak: ((String) -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
        context: Context? = null
    ) {
        if (prompt.isBlank() || _isGenerating.value) return

        val cached = getCachedResponse(prompt)
        if (cached != null) {
            val userMsg = AssistantMessage(text = prompt.trim(), isUser = true)
            var updatedHistory = _chatHistory.value + userMsg + cached
            if (updatedHistory.size > 100) updatedHistory = updatedHistory.takeLast(100)
            _chatHistory.value = updatedHistory
            _typedText.value = ""
            if (settings.allowTextToSpeechAssistant) {
                onSpeechSpeak?.invoke(cached.text)
            }
            return
        }

        val userMsg = AssistantMessage(text = prompt.trim(), isUser = true)
        var updatedHistory = _chatHistory.value + userMsg
        if (updatedHistory.size > 100) {
            updatedHistory = updatedHistory.takeLast(100)
        }
        _chatHistory.value = updatedHistory
        _typedText.value = ""
        _isGenerating.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val brain = getOrCreateBrain(context)
                val snapshot = AssistantDirectorySnapshot(
                    providers = mainViewModel.providers.value,
                    stores = mainViewModel.stores.value,
                    properties = mainViewModel.properties.value,
                    categories = mainViewModel.categories.value,
                    settings = settings,
                    currentCityName = mainViewModel.homeViewModel.selectedCityName.value.ifBlank { "صنعاء" }
                )
                val responseMsg = brain.answer(prompt, isOnline, snapshot)
                cacheResponse(prompt, responseMsg)

                withContext(Dispatchers.Main) {
                    var updatedHistoryWithResp = _chatHistory.value + responseMsg
                    if (updatedHistoryWithResp.size > 100) {
                        updatedHistoryWithResp = updatedHistoryWithResp.takeLast(100)
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
                    onError?.invoke("حدث خطأ: ${e.localizedMessage}")
                }
            }
        }
    }

    private val cachedResponses = object : LinkedHashMap<String, AssistantMessage>(40, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, AssistantMessage>?): Boolean {
            return size > 40
        }
    }

    @Synchronized
    fun getCachedResponse(query: String): AssistantMessage? {
        return cachedResponses[normalizeArabic(query)]
    }

    @Synchronized
    fun cacheResponse(query: String, response: AssistantMessage) {
        cachedResponses[normalizeArabic(query)] = response
    }

    fun generateLocalOfflineResponse(prompt: String, viewModel: MainViewModel): Pair<String, List<Any>> {
        val msg = generateLocalOfflineMessage(prompt, viewModel)
        return Pair(msg.text, msg.matchedEntities)
    }

    fun generateLocalOfflineMessage(prompt: String, viewModel: MainViewModel): AssistantMessage {
        val brain = getOrCreateBrain(null)
        val snapshot = AssistantDirectorySnapshot(
            providers = viewModel.providers.value,
            stores = viewModel.stores.value,
            properties = viewModel.properties.value,
            categories = viewModel.categories.value,
            settings = viewModel.settings.value,
            currentCityName = viewModel.homeViewModel.selectedCityName.value.ifBlank { "صنعاء" }
        )
        return brain.answerSync(prompt, isOnline = false, snapshot = snapshot)
    }

    fun normalizeArabic(text: String): String = LocalGuideBrain.normalizeArabic(text)

    override fun onCleared() {
        super.onCleared()
        cachedResponses.clear()
    }
}
