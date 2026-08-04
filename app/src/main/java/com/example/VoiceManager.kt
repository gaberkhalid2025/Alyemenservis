package com.example

object VoiceManager {
    var onSpeak: ((String) -> Unit)? = null
    var onHear: (((String) -> Unit) -> Unit)? = null
}
