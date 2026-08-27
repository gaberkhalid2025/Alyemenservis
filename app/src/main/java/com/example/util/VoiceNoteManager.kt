package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

/**
 * Manager for recording and playing audio voice notes in the chat system.
 */
class VoiceNoteManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null
    private var recordingStartTime: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _currentlyPlayingUrl = MutableStateFlow<String?>(null)
    val currentlyPlayingUrl: StateFlow<String?> = _currentlyPlayingUrl.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    fun startRecording(): File? {
        try {
            stopPlaying()
            val audioDir = File(context.cacheDir, "voice_notes").apply { if (!exists()) mkdirs() }
            val file = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = file

            mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recordingStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDurationSeconds.value = 0
            return file
        } catch (e: Exception) {
            Log.e("VoiceNoteManager", "Error starting recording", e)
            cancelRecording()
            return null
        }
    }

    fun stopRecording(): Pair<File, Int>? {
        if (!_isRecording.value) return null
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isRecording.value = false
            val durationSec = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt().coerceAtLeast(1)
            val file = currentRecordingFile
            if (file != null && file.exists() && file.length() > 0) {
                Pair(file, durationSec)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("VoiceNoteManager", "Error stopping recording", e)
            cancelRecording()
            null
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
        } catch (e: Exception) {
            Log.e("VoiceNoteManager", "Error canceling recorder", e)
        } finally {
            mediaRecorder = null
            _isRecording.value = false
            _recordingDurationSeconds.value = 0
            currentRecordingFile?.delete()
            currentRecordingFile = null
        }
    }

    fun playAudio(urlOrPath: String, onCompletion: () -> Unit = {}) {
        try {
            if (_currentlyPlayingUrl.value == urlOrPath) {
                // If same audio is playing, stop/pause it
                stopPlaying()
                return
            }

            stopPlaying()
            _currentlyPlayingUrl.value = urlOrPath

            mediaPlayer = MediaPlayer().apply {
                if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
                    setDataSource(urlOrPath)
                } else {
                    setDataSource(context, android.net.Uri.parse(urlOrPath))
                }
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.start()
                }
                setOnCompletionListener {
                    stopPlaying()
                    onCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlaying()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceNoteManager", "Error playing audio", e)
            stopPlaying()
        }
    }

    fun stopPlaying() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("VoiceNoteManager", "Error releasing player", e)
        } finally {
            mediaPlayer = null
            _currentlyPlayingUrl.value = null
            _playbackProgress.value = 0f
        }
    }

    fun release() {
        cancelRecording()
        stopPlaying()
    }
}
