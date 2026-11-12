package com.example.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.sin

/**
 * 🔊 ArabicNavigationVoiceEngine
 * محرك الصوت العربي واليمني للملاحة:
 * 1. يكتشف تلقائياً جميع محركات النطق المثبتة (مثل Google Speech Services) إذا كان محرك الجهاز الافتراضي (مثل Samsung TTS) لا يدعم العربية.
 * 2. يوفّر نغمات توجيه اتجاهية مدمجة (AudioTrack PCM) كبديل فوري يعمل 100% بدون إنترنت إذا لم تتوفر حزمة نطق عربي على الجهاز.
 * 3. يوجّه المستخدم مباشرةً إلى محرك Google أو إعدادات النظام بدلاً من شاشات التحميل الفارغة.
 */
class ArabicNavigationVoiceEngine(
    private val context: Context,
    private val onStateChanged: (VoiceEngineState) -> Unit
) {
    companion object {
        private const val TAG = "NavVoiceEngine"
        const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"

        private val ARABIC_LOCALES = listOf(
            Locale.forLanguageTag("ar-YE"),
            Locale.forLanguageTag("ar-SA"),
            Locale.forLanguageTag("ar-EG"),
            Locale.forLanguageTag("ar-AE"),
            Locale.forLanguageTag("ar-JO"),
            Locale.forLanguageTag("ar-XA"),
            Locale("ar")
        )

        fun isPackageInstalled(context: Context, packageName: String): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(packageName, 0)
                }
                true
            } catch (_: Throwable) {
                false
            }
        }

        fun openGoogleTtsVoiceDownload(context: Context): Boolean {
            val googleInstalled = isPackageInstalled(context, GOOGLE_TTS_PACKAGE)
            if (googleInstalled) {
                val targeted = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                    setPackage(GOOGLE_TTS_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (tryStart(context, targeted)) return true
            }
            if (openSystemTtsSettings(context)) return true
            return openGoogleTtsPlayStore(context)
        }

        fun openSystemTtsSettings(context: Context): Boolean {
            val intents = listOf(
                Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return intents.any { tryStart(context, it) }
        }

        fun openGoogleTtsPlayStore(context: Context): Boolean {
            val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GOOGLE_TTS_PACKAGE"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (tryStart(context, market)) return true
            val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$GOOGLE_TTS_PACKAGE"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return tryStart(context, web)
        }

        private fun tryStart(context: Context, intent: Intent): Boolean {
            return try {
                context.startActivity(intent)
                true
            } catch (_: Throwable) {
                false
            }
        }

        /** يفحص كل المحركات المثبتة للتحقق من توفر الصوت العربي (لأغراض الفحص والتشخيص). */
        suspend fun probeArabicSupport(context: Context): Pair<Boolean, String?> = withContext(Dispatchers.Main) {
            val defaultProbe = probeSingleEngine(context, null)
            if (defaultProbe.first) return@withContext defaultProbe

            val engines = defaultProbe.second?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
            val ordered = buildList {
                if (isPackageInstalled(context, GOOGLE_TTS_PACKAGE)) add(GOOGLE_TTS_PACKAGE)
                engines.filter { it != GOOGLE_TTS_PACKAGE }.forEach { add(it) }
            }.distinct()

            for (pkg in ordered) {
                val res = probeSingleEngine(context, pkg)
                if (res.first) return@withContext true to pkg
            }
            false to null
        }

        private suspend fun probeSingleEngine(context: Context, enginePkg: String?): Pair<Boolean, String?> =
            withTimeoutOrNull(3_000L) {
                suspendCancellableCoroutine { cont ->
                    var tts: TextToSpeech? = null
                    val listener = TextToSpeech.OnInitListener { status ->
                        val engine = tts
                        if (status != TextToSpeech.SUCCESS || engine == null) {
                            val pkgs = runCatching { engine?.engines?.joinToString("|") { it.name } }.getOrNull()
                            runCatching { engine?.shutdown() }
                            if (cont.isActive) cont.resume(false to pkgs)
                            return@OnInitListener
                        }
                        val ok = selectBestArabicLocale(engine)
                        val pkgs = runCatching { engine.engines.joinToString("|") { it.name } }.getOrNull()
                        val label = enginePkg ?: engine.defaultEngine ?: pkgs
                        runCatching { engine.shutdown() }
                        if (cont.isActive) cont.resume(ok to label)
                    }
                    tts = if (enginePkg != null) TextToSpeech(context, listener, enginePkg) else TextToSpeech(context, listener)
                    cont.invokeOnCancellation { runCatching { tts?.shutdown() } }
                }
            } ?: (false to null)

        fun selectBestArabicLocale(engine: TextToSpeech): Boolean {
            // 1) فحص الأصوات المثبتة محلياً أولاً (بدون حاجة للإنترنت)
            val voices = runCatching { engine.voices }.getOrNull()
            if (!voices.isNullOrEmpty()) {
                val arabicVoices = voices.filter { v ->
                    v.locale.language.equals("ar", ignoreCase = true) &&
                        !v.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                }
                val offlineVoice = arabicVoices.firstOrNull { !it.isNetworkConnectionRequired }
                    ?: arabicVoices.firstOrNull()
                if (offlineVoice != null) {
                    val vr = runCatching { engine.setVoice(offlineVoice) }.getOrDefault(TextToSpeech.ERROR)
                    if (vr != TextToSpeech.ERROR) {
                        return true
                    }
                }
            }
            // 2) تجربة اللهجات واللغات العربية المدعومة
            for (loc in ARABIC_LOCALES) {
                val r = runCatching { engine.setLanguage(loc) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
                if (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED && r >= 0) {
                    return true
                }
            }
            return false
        }
    }

    data class VoiceEngineState(
        val isInitializing: Boolean = true,
        val isArabicReady: Boolean = false,
        val activeEnginePackage: String? = null,
        val activeEngineLabel: String = "",
        val googleTtsInstalled: Boolean = false,
        val usingAudioCueFallback: Boolean = false
    )

    private var tts: TextToSpeech? = null
    private var disposed = false
    private var triedEngines = mutableSetOf<String>()
    private var currentState = VoiceEngineState(
        googleTtsInstalled = isPackageInstalled(context, GOOGLE_TTS_PACKAGE)
    )

    fun initialize() {
        disposed = false
        triedEngines.clear()
        updateState(
            VoiceEngineState(
                isInitializing = true,
                googleTtsInstalled = isPackageInstalled(context, GOOGLE_TTS_PACKAGE)
            )
        )
        bindEngine(null)
    }

    private fun bindEngine(enginePackage: String?) {
        if (disposed) return
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {}

        val listener = TextToSpeech.OnInitListener { status ->
            if (disposed) return@OnInitListener
            val engine = tts ?: return@OnInitListener
            val currentPkg = enginePackage ?: runCatching { engine.defaultEngine }.getOrNull() ?: "default"
            triedEngines.add(currentPkg)

            if (status == TextToSpeech.SUCCESS && selectBestArabicLocale(engine)) {
                runCatching {
                    engine.setPitch(1.0f)
                    engine.setSpeechRate(0.96f)
                }
                val label = when {
                    currentPkg.contains("google", ignoreCase = true) -> "Google للنطق العربي"
                    currentPkg.contains("samsung", ignoreCase = true) -> "Samsung للنطق العربي"
                    else -> "محرك النطق العربي"
                }
                updateState(
                    VoiceEngineState(
                        isInitializing = false,
                        isArabicReady = true,
                        activeEnginePackage = currentPkg,
                        activeEngineLabel = label,
                        googleTtsInstalled = isPackageInstalled(context, GOOGLE_TTS_PACKAGE),
                        usingAudioCueFallback = false
                    )
                )
                return@OnInitListener
            }

            // المحرك الحالي لا يدعم العربية -> نبحث عن محرك بديل مثبت (مثل Google Speech Services)
            val candidates = buildList {
                if (isPackageInstalled(context, GOOGLE_TTS_PACKAGE) && !triedEngines.contains(GOOGLE_TTS_PACKAGE)) {
                    add(GOOGLE_TTS_PACKAGE)
                }
                runCatching { engine.engines }.getOrNull()?.forEach { info ->
                    if (!triedEngines.contains(info.name)) add(info.name)
                }
            }.distinct()

            val nextEngine = candidates.firstOrNull()
            if (nextEngine != null) {
                Log.i(TAG, "Default TTS ($currentPkg) lacks Arabic; trying fallback engine: $nextEngine")
                bindEngine(nextEngine)
            } else {
                // لم يتوفر أي محرك TTS عربي مثبت -> تفعيل نغمات التوجيه المدمجة فوراً
                runCatching { engine.shutdown() }
                tts = null
                updateState(
                    VoiceEngineState(
                        isInitializing = false,
                        isArabicReady = false,
                        activeEnginePackage = null,
                        activeEngineLabel = "نغمات التوجيه الذكية (أوفلاين)",
                        googleTtsInstalled = isPackageInstalled(context, GOOGLE_TTS_PACKAGE),
                        usingAudioCueFallback = true
                    )
                )
            }
        }

        tts = try {
            if (enginePackage != null) TextToSpeech(context.applicationContext, listener, enginePackage)
            else TextToSpeech(context.applicationContext, listener)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to create TextToSpeech($enginePackage): ${t.message}")
            null
        }
    }

    fun speak(text: String, maneuverType: ManeuverType? = null, urgent: Boolean = false) {
        if (text.isBlank() || disposed) return
        val engine = tts
        if (currentState.isArabicReady && engine != null) {
            val mode = if (urgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            }
            val res = runCatching {
                engine.speak(text, mode, params, "nav_${System.nanoTime()}")
            }.getOrDefault(TextToSpeech.ERROR)
            if (res == TextToSpeech.SUCCESS) return
        }
        // بديل فوري مضمون 100% أوفلاين بدون إنترنت: نغمة اتجاهية واضحة
        NavAudioCuePlayer.play(maneuverType)
    }

    fun shutdown() {
        disposed = true
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {}
        tts = null
    }

    private fun updateState(newState: VoiceEngineState) {
        currentState = newState
        onStateChanged(newState)
    }
}

/**
 * 🔔 NavAudioCuePlayer
 * مشغل نغمات اتجاهية خفيف يعمل عبر AudioTrack PCM 100% بدون أي تبعيات أو إنترنت
 * لضمان تنبيه السائق صوتياً عند المنعطفات حتى لو لم تتوفر حزمة TTS عربية على جهازه.
 */
object NavAudioCuePlayer {
    private const val SAMPLE_RATE = 22050

    fun play(maneuverType: ManeuverType?) {
        val tones: List<Pair<Double, Int>> = when (maneuverType) {
            ManeuverType.RIGHT, ManeuverType.SLIGHT_RIGHT, ManeuverType.SHARP_RIGHT ->
                listOf(523.25 to 110, 783.99 to 150) // نغمة صاعدة (يمين)
            ManeuverType.LEFT, ManeuverType.SLIGHT_LEFT, ManeuverType.SHARP_LEFT ->
                listOf(783.99 to 110, 523.25 to 150) // نغمة هابطة (يسار)
            ManeuverType.UTURN, ManeuverType.ROUNDABOUT ->
                listOf(587.33 to 90, 0.0 to 50, 587.33 to 130) // نغمة مزدوجة (دوار/التفاف)
            ManeuverType.DESTINATION ->
                listOf(523.25 to 110, 659.25 to 110, 783.99 to 220) // نغمة وصول مبهجة
            else ->
                listOf(659.25 to 110) // نغمة تنبيه مفردة
        }

        Thread {
            playTonesInternal(tones)
        }.apply {
            isDaemon = true
            start()
        }
    }

    private fun playTonesInternal(tones: List<Pair<Double, Int>>) {
        var track: AudioTrack? = null
        try {
            val totalSamples = tones.sumOf { (_, ms) -> (SAMPLE_RATE * ms) / 1000 }
            if (totalSamples <= 0) return
            val pcm = ShortArray(totalSamples)
            var offset = 0
            for ((freq, ms) in tones) {
                val count = (SAMPLE_RATE * ms) / 1000
                val fadeSamples = (count / 6).coerceAtLeast(1)
                for (i in 0 until count) {
                    val sample = if (freq <= 0.0) {
                        0.0
                    } else {
                        val env = when {
                            i < fadeSamples -> i.toDouble() / fadeSamples
                            i > count - fadeSamples -> (count - i).toDouble() / fadeSamples
                            else -> 1.0
                        }
                        sin(2.0 * PI * freq * i / SAMPLE_RATE) * env * 0.35
                    }
                    if (offset < pcm.size) {
                        pcm[offset++] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                }
            }

            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            track = AudioTrack(
                attrs,
                format,
                pcm.size * 2,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.write(pcm, 0, pcm.size)
                track.play()
                val totalMs = tones.sumOf { it.second }.toLong()
                Thread.sleep(totalMs + 80L)
            }
        } catch (_: Throwable) {
        } finally {
            try { track?.stop() } catch (_: Throwable) {}
            try { track?.release() } catch (_: Throwable) {}
        }
    }
}
