package com.example.util

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 🤖 GeminiApi - واجهة الاتصال المباشر بنماذج Google Gemini API
 * 
 * الميزات:
 * 1. الاتصال المباشر بنموذج `gemini-2.5-flash` لمعالجة اللغة الطبيعية والدردشة الذكية السريعة.
 * 2. استخدام مفتاح الواجهة البرمجية الموفر بأمان عبر `BuildConfig.GEMINI_API_KEY`.
 * 3. ضبط مهلة OkHttpClient لضمان استقرار توليد النصوص المعقدة.
 * 4. سياق معرفي (System Instructions / Context) لإسناد إجابات دقيقة (RAG).
 * 5. ردود ذكية ومفصلة غير متصلة بالإنترنت (Offline Fallback System) تدعم الموثوقية التامة في اليمن.
 */
class GeminiApi {

    companion object {
        private const val TAG = "GeminiApi"
        private const val MODEL_NAME = "gemini-2.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * التحقق من توفر مفتاح Gemini API
     */
    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return !key.isNullOrBlank() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * توليد نص الإجابة عبر Gemini API مع إمكانية تمرير سياق إضافي
     * 
     * @param prompt استفسار المستخدم
     * @param systemContext سياق تعليمات النظام أو بيانات RAG المسترجعة
     * @return النص المولد من الذكاء الاصطناعي أو الرد الذكي غير المتصل
     */
    suspend fun generateContent(prompt: String, systemContext: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            Log.w(TAG, "Gemini API Key is not configured. Redirecting to offline fallback responder.")
            return@withContext getOfflineFallbackResponse(prompt)
        }

        try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
            
            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()
                
                val combinedPrompt = if (!systemContext.isNullOrBlank()) {
                    "سياق ومعلومات مرجعية:\n$systemContext\n\nاستفسار المستخدم:\n$prompt"
                } else {
                    prompt
                }
                
                partsArray.put(JSONObject().put("text", combinedPrompt))
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                // تعليمات النظام
                val systemInstructionObj = JSONObject().apply {
                    val sysParts = JSONArray().put(JSONObject().put("text", "أنت مساعد ذكي خبير وخادم لتطبيق 'دليل خدمات اليمن'. أجب بأسلوب ودود ومهني ولهجة يمنية مهذبة تناسب المستخدمين."))
                    put("parts", sysParts)
                }
                put("systemInstruction", systemInstructionObj)

                // إعدادات التوليد
                val genConfig = JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
                    put("topK", 40)
                    put("maxOutputTokens", 1024)
                }
                put("generationConfig", genConfig)
            }

            val requestBody = requestBodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                Log.e(TAG, "Gemini API error code: ${response.code}. Falling back offline.")
                return@withContext getOfflineFallbackResponse(prompt)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    if (text.isNotBlank()) {
                        return@withContext text.trim()
                    }
                }
            }

            return@withContext getOfflineFallbackResponse(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API call: ${e.message}. Launching offline fallback engine.")
            return@withContext getOfflineFallbackResponse(prompt)
        }
    }

    /**
     * 📡 نظام الردود غير المتصلة الذكية (Offline-First Local Knowledge Base)
     */
    private fun getOfflineFallbackResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("كهرباء") || lower.contains("كهربائي") || lower.contains("شمس") -> 
                "إليك رد مساعد اليمن غير المتصل 🔌: يمكنك العثور على أفضل مهندسي صيانة المولدات والمنظومات الشمسية والكهربائية المنزلية في صنعاء وعدن وبقية المحافظات عبر فتح قسم 'صيانة الأجهزة المنزلية والكهرباء' من القائمة الرئيسية مباشرة بدون إنترنت والتواصل معهم هاتفياً."
            lower.contains("طبيب") || lower.contains("مستشفى") || lower.contains("دكتور") || lower.contains("علاج") -> 
                "إليك رد مساعد اليمن غير المتصل 🩺: لحجز موعد طبي أو استعراض العيادات المتوفرة، توجه إلى قسم 'الخدمات الطبية والعيادات'. التطبيق يتيح لك استعراض أسماء الأطباء، تخصصاتهم، ومواقعهم الجغرافية، والاتصال المباشر بهم بدون إنترنت."
            lower.contains("مطعم") || lower.contains("وجبة") || lower.contains("أكل") || lower.contains("غداء") -> 
                "إليك رد مساعد اليمن غير المتصل 🍔: لاستعراض وجبات الطعام وأسعارها، افتح قائمة 'المطاعم والبوفيهات' من الشاشة الرئيسية، حيث يمكنك مشاهدة الوجبات وطلب توصيلها مجاناً عبر الهاتف دون حاجة للشبكة."
            lower.contains("صرف") || lower.contains("دولار") || lower.contains("سعر") || lower.contains("ريال") -> 
                "إليك رد مساعد اليمن غير المتصل 💰: لمعرفة أسعار الصرف وتحديثات السوق والتحويلات المالية، يرجى مراجعة قسم 'الصرافة والتحويلات' المتاح في تطبيقنا محلياً لمشاهدة آخر تسعيرة تم حفظها على جهازك."
            else -> 
                "أنت الآن في وضع غير متصل بالإنترنت 📡. دليل خدمات اليمن مصمم للعمل كاملاً دون شبكة! يمكنك تصفح مئات المهنيين ومحلات الصيانة، والمراكز التجارية، والأطباء، والاتصال بهم، بالإضافة إلى مراجعة حجوزاتك السابقة بالكامل."
        }
    }
}
