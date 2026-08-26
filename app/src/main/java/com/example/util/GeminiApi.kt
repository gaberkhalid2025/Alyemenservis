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
 * 1. الاتصال المباشر بنموذج `gemini-3.5-flash` لمعالجة اللغة الطبيعية والدردشة الذكية.
 * 2. استخدام مفتاح الواجهة البرمجية الموفر بأمان عبر `BuildConfig.GEMINI_API_KEY`.
 * 3. ضبط مهلة OkHttpClient إلى 60 ثانية للاتصال والقراءة لضمان استقرار توليد النصوص المعقدة.
 * 4. دعم حقن السياق المعرفي (System Instructions / Context) لإسناد إجابات دقيقة (RAG).
 * 5. معالجة آمنة للأخطاء وتقديم ردود احتياطية ذكية في حال انقطاع الشبكة.
 */
class GeminiApi {

    companion object {
        private const val TAG = "GeminiApi"
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
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
     * @return النص المولد من الذكاء الاصطناعي
     */
    suspend fun generateContent(prompt: String, systemContext: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            Log.w(TAG, "Gemini API Key is not configured or uses placeholder.")
            return@withContext "عذراً، خدمة الذكاء الاصطناعي السحابية تتطلب ضبط مفتاح Gemini API في إعدادات التطبيق."
        }

        try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
            
            val requestBodyJson = JSONObject().apply {
                // إعدادات المحتوى والأجزاء
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
                if (!systemContext.isNullOrBlank()) {
                    val systemInstructionObj = JSONObject().apply {
                        val sysParts = JSONArray().put(JSONObject().put("text", "أنت مساعد ذكي خبير وخادم لتطبيق 'دليل خدمات اليمن'. أجب بأسلوب ودود ومهني ولهجة يمنية مهذبة تناسب المستخدمين."))
                        put("parts", sysParts)
                    }
                    put("systemInstruction", systemInstructionObj)
                }

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
                Log.e(TAG, "Gemini API error code: ${response.code}, body: $responseBody")
                return@withContext "تعذر الحصول على استجابة من خدمة الذكاء الاصطناعي (رمز: ${response.code}). يرجى المحاولة لاحقاً."
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

            return@withContext "لم يتم العثور على رد مناسب للاستفسار."
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API call: ${e.message}", e)
            return@withContext "حدث خطأ أثناء التواصل مع خادم المساعد الذكي: ${e.localizedMessage ?: "يرجى التحقق من اتصال الإنترنت"}"
        }
    }
}
