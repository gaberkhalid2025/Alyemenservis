package com.example.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

enum class VoiceStatus { IDLE, LISTENING, ERROR }

data class VoiceState(
    val status: VoiceStatus = VoiceStatus.IDLE,
    val partial: String = "",
    val message: String? = null,
    val canOpenSettings: Boolean = false,
    val canUseKeyboard: Boolean = false
)

/**
 * 🎙️ VoiceSearchHelper - تعرّف صوتي داخل التطبيق (SpeechRecognizer)
 * يُستعمل من الخيط الرئيسي فقط ويدعم سلسلة لهجات عربية احتياطية (ar-YE -> ar-SA -> ar-EG -> ar)
 */
class VoiceSearchHelper(
    private val context: Context,
    private val onFinal: (String) -> Unit,
    private val onState: (VoiceState) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private val tags = listOf("ar-YE", "ar-SA", "ar-EG", "ar") // سلسلة احتياطية؛ لا نفرض لهجة واحدة
    private var tagIndex = 0
    private var delivered = false

    fun hasMicPermission() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    fun isAvailable() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        if (!isAvailable()) {
            onState(VoiceState(VoiceStatus.ERROR, message = "جهازك لا يحتوي خدمة تعرّف صوتي (تطبيق Google). استعمل ميكروفون لوحة المفاتيح أو اكتب.", canUseKeyboard = true))
            return
        }
        if (!hasMicPermission()) {
            onState(VoiceState(VoiceStatus.ERROR, message = "امنح إذن الميكروفون لاستعمال البحث الصوتي."))
            return
        }
        tagIndex = 0
        delivered = false
        listen()
    }

    private fun listen() {
        destroy()
        val r = try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (_: Exception) {
            onState(VoiceState(VoiceStatus.ERROR, message = "تعذر تشغيل التعرف الصوتي.", canUseKeyboard = true))
            return
        }
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onState(VoiceState(VoiceStatus.LISTENING))
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (t.isNotBlank()) onState(VoiceState(VoiceStatus.LISTENING, partial = t))
            }

            override fun onResults(results: Bundle?) {
                val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
                if (t.isBlank()) {
                    handleError(SpeechRecognizer.ERROR_NO_MATCH)
                    return
                }
                delivered = true
                onState(VoiceState())
                onFinal(t)
                destroy()
            }

            override fun onError(error: Int) {
                handleError(error)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, tags[tagIndex])
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        try {
            r.startListening(intent)
        } catch (_: Exception) {
            handleError(SpeechRecognizer.ERROR_CLIENT)
        }
    }

    private fun handleError(code: Int) {
        if (delivered) return
        // 12/13 = ERROR_LANGUAGE_NOT_SUPPORTED / ERROR_LANGUAGE_UNAVAILABLE (أندرويد 12+): جرّب صيغة عربية أخرى
        if ((code == 12 || code == 13) && tagIndex < tags.lastIndex) {
            tagIndex++
            main.post { listen() }
            return
        }
        val (msg, settings, keyboard) = when (code) {
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                Triple("لم أسمع كلاماً واضحاً. حاول مجدداً وتحدث بقرب الميكروفون.", false, false)
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER ->
                Triple("التعرف الصوتي العربي يحتاج إنترنت (أو حزمة لغة منزّلة من إعدادات Google). يمكنك استعمال ميكروفون لوحة المفاتيح أو الكتابة.", true, true)
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                Triple("امنح إذن الميكروفون من إعدادات التطبيق.", false, false)
            SpeechRecognizer.ERROR_AUDIO ->
                Triple("تعذر استعمال الميكروفون.", false, true)
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                Triple("خدمة التعرف مشغولة؛ أعد المحاولة بعد لحظة.", false, false)
            12, 13 ->
                Triple("العربية غير متاحة في خدمة التعرف على جهازك. حدّث تطبيق Google أو استعمل ميكروفون لوحة المفاتيح.", true, true)
            else ->
                Triple("تعذر التعرف الصوتي (رمز $code).", false, true)
        }
        onState(VoiceState(VoiceStatus.ERROR, message = msg, canOpenSettings = settings, canUseKeyboard = keyboard))
        destroy()
    }

    fun stop() {
        try { recognizer?.stopListening() } catch (_: Exception) {}
    }

    fun cancel() {
        destroy()
        onState(VoiceState())
    }

    fun destroy() {
        try { recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
    }

    companion object {
        /** يفتح إعدادات الإدخال الصوتي؛ وإن لم تتوفر يفتح الإعدادات العامة. */
        fun openVoiceSettings(context: Context) {
            val intents = listOf(
                Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
                Intent(Settings.ACTION_LOCALE_SETTINGS),
                Intent(Settings.ACTION_SETTINGS)
            )
            for (i in intents) {
                try {
                    context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return
                } catch (_: Exception) {}
            }
        }
    }
}
