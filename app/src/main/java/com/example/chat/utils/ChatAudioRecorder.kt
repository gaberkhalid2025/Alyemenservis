package com.example.chat.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * 🎙️ ChatAudioRecorder
 * Manages audio recording and streaming chunks for voice messages.
 */
class ChatAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMillis: Long = 0

    fun startRecording(roomId: String): File? {
        val fileName = "audio_${roomId}_${System.currentTimeMillis()}.m4a"
        outputFile = File(context.cacheDir, fileName)

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile?.absolutePath)
            
            try {
                prepare()
                start()
                startTimeMillis = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        return outputFile
    }

    fun stopRecording(): Long {
        val duration = System.currentTimeMillis() - startTimeMillis
        try {
            recorder?.stop()
            recorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            recorder = null
        }
        return duration
    }
}
