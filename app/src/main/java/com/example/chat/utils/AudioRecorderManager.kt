package com.example.chat.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * 🎙️ RecordingState
 * Represents the lifecycle state of audio recording.
 */
enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED
}

/**
 * 🎙️ AudioRecorderManager
 * Production-grade audio recording manager with real-time amplitude polling,
 * elapsed timer, max-duration safeguard, and file cleanup.
 */
class AudioRecorderManager(private val context: Context) {

    private val TAG = "AudioRecorderManager"
    private var mediaRecorder: MediaRecorder? = null
    private var activeAudioFile: File? = null
    private var recordingStartTimeMs: Long = 0L

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _amplitudeFlow = MutableStateFlow(0)
    val amplitudeFlow: StateFlow<Int> = _amplitudeFlow.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val MAX_RECORDING_DURATION_MS = 5 * 60 * 1000L // 5 minutes max

    /**
     * Starts voice recording for a specific chat room.
     */
    fun startRecording(roomId: String): File? {
        if (_recordingState.value == RecordingState.RECORDING) {
            stopRecording()
        }

        val audioDir = File(context.cacheDir, "chat_audio").apply { if (!exists()) mkdirs() }
        val fileName = "voice_${roomId}_${System.currentTimeMillis()}.m4a"
        activeAudioFile = File(audioDir, fileName)

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(activeAudioFile?.absolutePath)
                prepare()
                start()
            }

            recordingStartTimeMs = System.currentTimeMillis()
            _recordingState.value = RecordingState.RECORDING
            _recordingDurationMs.value = 0L

            startPollingAmplitudeAndTimer()
            return activeAudioFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaRecorder", e)
            releaseRecorder()
            _recordingState.value = RecordingState.IDLE
            return null
        }
    }

    /**
     * Stops active recording and returns the recorded File along with its total duration in milliseconds.
     */
    fun stopRecording(): Pair<File?, Long> {
        if (_recordingState.value != RecordingState.RECORDING && _recordingState.value != RecordingState.PAUSED) {
            return Pair(null, 0L)
        }

        stopPolling()
        val duration = System.currentTimeMillis() - recordingStartTimeMs

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        } finally {
            releaseRecorder()
        }

        _recordingState.value = RecordingState.STOPPED
        val file = activeAudioFile
        _recordingDurationMs.value = duration
        return Pair(file, duration)
    }

    /**
     * Cancels active recording and securely removes the temporary audio file.
     */
    fun cancelRecording() {
        stopPolling()
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder during cancel", e)
        } finally {
            releaseRecorder()
        }

        activeAudioFile?.let {
            if (it.exists()) it.delete()
        }
        activeAudioFile = null
        _recordingState.value = RecordingState.IDLE
        _recordingDurationMs.value = 0L
        _amplitudeFlow.value = 0
    }

    private fun startPollingAmplitudeAndTimer() {
        stopPolling()
        pollingJob = scope.launch {
            while (isActive && _recordingState.value == RecordingState.RECORDING) {
                val elapsed = System.currentTimeMillis() - recordingStartTimeMs
                _recordingDurationMs.value = elapsed

                // Check max limit
                if (elapsed >= MAX_RECORDING_DURATION_MS) {
                    stopRecording()
                    break
                }

                // Poll max amplitude
                try {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    // Normalize to 0 - 100 range
                    val normalized = (maxAmp / 32767f * 100).toInt().coerceIn(0, 100)
                    _amplitudeFlow.value = normalized
                } catch (e: Exception) {
                    _amplitudeFlow.value = 0
                }

                delay(100)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (ignored: Exception) {}
        mediaRecorder = null
    }
}

/**
 * 🎵 AudioPlaybackManager
 * Handles voice message playback with progress flow and completion listener.
 */
class AudioPlaybackManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingMessageId = MutableStateFlow<String?>(null)
    val currentPlayingMessageId: StateFlow<String?> = _currentPlayingMessageId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    fun playAudio(messageId: String, audioSource: String) {
        if (_currentPlayingMessageId.value == messageId && _isPlaying.value) {
            pauseAudio()
            return
        }

        stopAudio()

        try {
            mediaPlayer = MediaPlayer().apply {
                if (audioSource.startsWith("http")) {
                    setDataSource(audioSource)
                } else {
                    setDataSource(context, Uri.fromFile(File(audioSource)))
                }
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    _currentPlayingMessageId.value = messageId
                    startProgressTracker()
                }
                setOnCompletionListener {
                    stopAudio()
                }
                setOnErrorListener { _, _, _ ->
                    stopAudio()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlaybackManager", "Error playing audio", e)
            stopAudio()
        }
    }

    fun pauseAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
    }

    fun resumeAudio() {
        mediaPlayer?.let {
            if (!_isPlaying.value) {
                it.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        }
    }

    fun stopAudio() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (ignored: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _currentPlayingMessageId.value = null
        _playbackProgress.value = 0f
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying && mp.duration > 0) {
                        _playbackProgress.value = mp.currentPosition.toFloat() / mp.duration.toFloat()
                    }
                }
                delay(100)
            }
        }
    }
}
