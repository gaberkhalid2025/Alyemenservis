package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * 🎙️ VoiceManager
 * محرك الصوت المتكامل: يدعم تحويل النص إلى كلام (TTS) والتعرف على الصوت (STT) باللغة العربية.
 */
object VoiceManager : TextToSpeech.OnInitListener {

    private const val TAG = "VoiceManager"

    var onSpeak: ((String) -> Unit)? = null
    var onHear: (((String) -> Unit) -> Unit)? = null

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var isSpeakingCallback: ((Boolean) -> Unit)? = null
    var isListeningCallback: ((Boolean) -> Unit)? = null

    private var speechRecognizer: SpeechRecognizer? = null

    fun init(context: Context) {
        if (tts == null) {
            try {
                tts = TextToSpeech(context.applicationContext, this)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing TTS", e)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ar")) ?: TextToSpeech.LANG_MISSING_DATA
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(0.95f)
            isInitialized = true

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeakingCallback?.invoke(true)
                }

                override fun onDone(utteranceId: String?) {
                    isSpeakingCallback?.invoke(false)
                }

                override fun onError(utteranceId: String?) {
                    isSpeakingCallback?.invoke(false)
                }
            })
        }
    }

    fun speak(text: String, utteranceId: String = "ai_speech") {
        if (isInitialized && tts != null) {
            val params = Bundle()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
        onSpeak?.invoke(text)
    }

    /**
     * تشغيل الاستماع الصوتي المباشر (STT)
     */
    fun startListening(context: Context, onResult: (String) -> Unit, onError: ((String) -> Unit)? = null) {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onError?.invoke("التعرف على الصوت غير مدعوم على هذا الجهاز")
                return
            }

            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListeningCallback?.invoke(true)
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListeningCallback?.invoke(false)
                    }

                    override fun onError(error: Int) {
                        isListeningCallback?.invoke(false)
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرف على الصوت"
                            SpeechRecognizer.ERROR_NETWORK -> "خطأ في اتصال الشبكة"
                            SpeechRecognizer.ERROR_AUDIO -> "خطأ في تسجيل الصوت"
                            else -> "حدث خطأ في التعرف الصوتي ($error)"
                        }
                        onError?.invoke(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        isListeningCallback?.invoke(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onResult(matches[0])
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-YE")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-YE")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن باللغة العربية...")
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListeningCallback?.invoke(false)
            Log.e(TAG, "Error starting speech recognition", e)
            onError?.invoke(e.localizedMessage ?: "فشل بدء التعرف على الصوت")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListeningCallback?.invoke(false)
    }

    fun stop() {
        tts?.stop()
        speechRecognizer?.stopListening()
        isSpeakingCallback?.invoke(false)
        isListeningCallback?.invoke(false)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    fun getInstance(context: Context): VoiceManager {
        init(context)
        return this
    }
}
