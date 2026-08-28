package com.example

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * 🎙️ VoiceManager
 * محرك الصوت المتكامل: يدعم واجهات الاستماع القديمة والجديدة وتحويل النص إلى كلام (TTS)
 */
object VoiceManager : TextToSpeech.OnInitListener {

    var onSpeak: ((String) -> Unit)? = null
    var onHear: (((String) -> Unit) -> Unit)? = null

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var isSpeakingCallback: ((Boolean) -> Unit)? = null

    fun init(context: Context) {
        if (tts == null) {
            try {
                tts = TextToSpeech(context.applicationContext, this)
            } catch (e: Exception) {
                Log.e("VoiceManager", "Error initializing TTS", e)
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

    fun stop() {
        tts?.stop()
        isSpeakingCallback?.invoke(false)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    fun getInstance(context: Context): VoiceManager {
        init(context)
        return this
    }
}
