package com.example

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale

object VoiceManager {
    var onSpeak: ((String) -> Unit)? = null
    var onHear: (((String) -> Unit) -> Unit)? = null

    fun listen(onResult: (String) -> Unit) {
        onHear?.invoke(onResult)
    }

    fun speak(text: String) {
        onSpeak?.invoke(text)
    }

    fun createSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-YE")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن للإدخال الصوتي...")
        }
    }
}
