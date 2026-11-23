package com.example

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

data class TtsDiagnosticStatus(
    val isInitialized: Boolean,
    val activeEngine: String?,
    val installedEnginesCount: Int,
    val googleTtsInstalled: Boolean,
    val selectedLanguage: String?,
    val hasAudioFocus: Boolean
)

/**
 * 🎙️ VoiceManager
 * محرك الصوت المتكامل: يدعم واجهات الاستماع القديمة والجديدة وتحويل النص إلى كلام (TTS)
 */
object VoiceManager : TextToSpeech.OnInitListener {

    var onSpeak: ((String) -> Unit)? = null
    var onHear: (((String) -> Unit) -> Unit)? = null

    private var tts: TextToSpeech? = null
    private var appContext: Context? = null
    private var triedGoogleFallback = false
    @Volatile
    private var isInitialized = false
    @Volatile
    private var isArabicAvailable = false
    @Volatile
    private var isInitializing = false
    @Volatile
    private var isDispatchingOnSpeak = false

    @Volatile
    private var pendingSpeechText: String? = null
    @Volatile
    private var pendingUtteranceId: String = "ai_speech"
    @Volatile
    private var activeUtteranceId: String = "ai_speech"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var audioFocusRequest: AudioFocusRequest? = null
    @Volatile
    private var hasAudioFocus = false

    var isSpeakingCallback: ((Boolean) -> Unit)? = null

    private fun requestAudioFocus() {
        val ctx = appContext ?: return
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(false)
                    .build().also { audioFocusRequest = it }
                val res = am.requestAudioFocus(req)
                hasAudioFocus = res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                val res = am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                hasAudioFocus = res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            Log.w("VoiceManager", "AudioFocus request skipped: ${e.message}")
        }
    }

    private fun abandonAudioFocus() {
        val ctx = appContext ?: return
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (_: Exception) {
        } finally {
            hasAudioFocus = false
        }
    }

    @Synchronized
    fun init(context: Context) {
        appContext = context.applicationContext
        if (tts == null && !isInitializing) {
            try {
                isInitializing = true
                triedGoogleFallback = false
                tts = TextToSpeech(context.applicationContext, this)
            } catch (e: Exception) {
                isInitializing = false
                Log.e("VoiceManager", "Error initializing TTS", e)
            }
        }
    }

    override fun onInit(status: Int) {
        isInitializing = false
        if (status == TextToSpeech.SUCCESS) {
            try {
                val yemeniLocale = Locale.forLanguageTag("ar-YE")
                val arabicLocale = Locale.forLanguageTag("ar")
                var result = tts?.setLanguage(yemeniLocale) ?: TextToSpeech.LANG_MISSING_DATA
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    result = tts?.setLanguage(arabicLocale) ?: TextToSpeech.LANG_MISSING_DATA
                }
                if ((result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) &&
                    !triedGoogleFallback && appContext != null &&
                    com.example.utils.ArabicNavigationVoiceEngine.isPackageInstalled(
                        appContext!!,
                        com.example.utils.ArabicNavigationVoiceEngine.GOOGLE_TTS_PACKAGE
                    )
                ) {
                    triedGoogleFallback = true
                    isInitializing = true
                    try { tts?.shutdown() } catch (_: Exception) {}
                    tts = TextToSpeech(
                        appContext!!,
                        this,
                        com.example.utils.ArabicNavigationVoiceEngine.GOOGLE_TTS_PACKAGE
                    )
                    return
                }
                val arabicSupported = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                isArabicAvailable = arabicSupported || Locale.getDefault().language.equals("ar", ignoreCase = true)
                if (!arabicSupported && isArabicAvailable) {
                    tts?.setLanguage(Locale.getDefault())
                }
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(0.95f)
                isInitialized = true

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        if (utteranceId != null) activeUtteranceId = utteranceId
                        mainHandler.post { isSpeakingCallback?.invoke(true) }
                    }

                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == null || utteranceId == activeUtteranceId) {
                            abandonAudioFocus()
                            mainHandler.post { isSpeakingCallback?.invoke(false) }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (utteranceId == null || utteranceId == activeUtteranceId) {
                            abandonAudioFocus()
                            mainHandler.post { isSpeakingCallback?.invoke(false) }
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        if (utteranceId == null || utteranceId == activeUtteranceId) {
                            abandonAudioFocus()
                            mainHandler.post { isSpeakingCallback?.invoke(false) }
                        }
                    }
                })

                val queuedText = pendingSpeechText
                val queuedId = pendingUtteranceId
                pendingSpeechText = null
                if (!queuedText.isNullOrBlank()) {
                    if (isArabicAvailable) {
                        activeUtteranceId = queuedId
                        requestAudioFocus()
                        val params = Bundle()
                        tts?.speak(queuedText, TextToSpeech.QUEUE_FLUSH, params, queuedId)
                    } else {
                        Log.w("VoiceManager", "Arabic TTS not available; skipping speech to avoid garbled audio")
                        mainHandler.post { isSpeakingCallback?.invoke(false) }
                    }
                }
            } catch (e: Exception) {
                Log.e("VoiceManager", "Error configuring TTS onInit", e)
                isInitialized = false
                pendingSpeechText = null
                try { tts?.shutdown() } catch (_: Exception) {}
                tts = null
            }
        } else {
            isInitialized = false
            pendingSpeechText = null
            try { tts?.shutdown() } catch (_: Exception) {}
            tts = null
            Log.w("VoiceManager", "TTS initialization failed with status: $status")
        }
    }

    fun speak(text: String, utteranceId: String = "ai_speech") {
        if (text.isBlank()) return
        activeUtteranceId = utteranceId
        if (isInitialized && tts != null) {
            if (!isArabicAvailable) {
                Log.w("VoiceManager", "Arabic TTS unavailable on device; text shown without speech")
                mainHandler.post { isSpeakingCallback?.invoke(false) }
                return
            }
            try {
                requestAudioFocus()
                val params = Bundle()
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            } catch (e: Exception) {
                abandonAudioFocus()
                Log.e("VoiceManager", "Error speaking text", e)
            }
        } else {
            pendingSpeechText = text
            pendingUtteranceId = utteranceId
            if (!isDispatchingOnSpeak && tts == null && !isInitializing) {
                try {
                    isDispatchingOnSpeak = true
                    onSpeak?.invoke(text)
                } finally {
                    isDispatchingOnSpeak = false
                }
            }
        }
    }

    fun stop() {
        try {
            pendingSpeechText = null
            tts?.stop()
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error stopping TTS", e)
        } finally {
            abandonAudioFocus()
        }
        mainHandler.post { isSpeakingCallback?.invoke(false) }
    }

    fun shutdown() {
        try {
            pendingSpeechText = null
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error shutting down TTS", e)
        } finally {
            abandonAudioFocus()
            audioFocusRequest = null
            tts = null
            isInitialized = false
            isArabicAvailable = false
            isInitializing = false
            isDispatchingOnSpeak = false
            isSpeakingCallback = null
            onSpeak = null
            onHear = null
        }
    }

    fun diagnoseTtsStatus(context: Context): TtsDiagnosticStatus {
        val engines = runCatching { tts?.engines }.getOrNull().orEmpty()
        val langTag = runCatching { tts?.voice?.locale?.toLanguageTag() }.getOrNull()
        return TtsDiagnosticStatus(
            isInitialized = isInitialized,
            activeEngine = runCatching { tts?.defaultEngine }.getOrNull(),
            installedEnginesCount = engines.size,
            googleTtsInstalled = com.example.utils.ArabicNavigationVoiceEngine.isPackageInstalled(
                context,
                com.example.utils.ArabicNavigationVoiceEngine.GOOGLE_TTS_PACKAGE
            ),
            selectedLanguage = langTag,
            hasAudioFocus = hasAudioFocus
        )
    }

    fun getInstance(context: Context): VoiceManager {
        init(context)
        return this
    }
}
