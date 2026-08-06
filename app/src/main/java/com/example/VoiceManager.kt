package com.example

import com.example.utils.*

object VoiceManager {
    var onSpeak: ((String) -> Unit)? = null
    var onHear: (((String) -> Unit) -> Unit)? = null
}
