package com.aman.vaak.managers

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Manages voice recording operations for the keyboard. Uses SystemManager for Android SDK audio
 * operations to improve testability.
 */
interface VoiceManager {
    /**
     * Checks if voice recording is currently active
     * @return true if recording is in progress, false otherwise
     */
    fun isRecording(): Boolean

    /**
     * Starts voice recording if not already recording
     * @return Result.success if recording started successfully, Result.failure with specific
     * exception if failed Possible exceptions:
     * - IllegalStateException: Already recording
     * - RuntimeException: Hardware or system error
     */
    suspend fun startRecording(): Result<Unit>

    /**
     * Stops current recording and returns recorded audio data
     * @return Result containing recorded audio as ByteArray if successful, failure with exception
     * if error occurs Possible exceptions:
     * - IllegalStateException: Not recording
     * - RuntimeException: Error processing audio data
     */
    suspend fun stopRecording(): Result<ByteArray>

    /**
     * Cancels current recording if active, discarding any recorded data
     * @return true if recording was canceled successfully, false otherwise
     */
    fun cancelRecording(): Boolean

    /**
     * Releases all resources held by the VoiceManager Should be called from
     * InputActivity.onDestroy()
     */
    fun release()
}

class VoiceManagerImpl
    @Inject
    constructor(private val systemManager: SystemManager) :
    VoiceManager {
        companion object {
            private const val SAMPLE_RATE = 44100
            private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
            private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        }

        private enum class RecordingState {
            IDLE,
            RECORDING,
            ERROR,
        }

        private var currentState: RecordingState = RecordingState.IDLE
        private var audioRecorder: AudioRecord? = null
        private var bufferSize: Int = 0
        private val recordedData = mutableListOf<ByteArray>()
        private var recordingJob: Job? = null

        init {
            bufferSize = systemManager.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            setupAudioRecorder()
        }

        private fun setupAudioRecorder() {
            try {
                audioRecorder =
                    systemManager.createAudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        bufferSize,
                    )
            } catch (e: SecurityException) {
                currentState = RecordingState.ERROR
            }
        }

        override fun isRecording(): Boolean = currentState == RecordingState.RECORDING

        override suspend fun startRecording(): Result<Unit> =
            withContext(Dispatchers.IO) {
                runCatching {
                    if (isRecording()) {
                        throw IllegalStateException("Already recording")
                    }

                    val recorder =
                        audioRecorder
                            ?: throw RuntimeException("Hardware initialization failed")

                    recordedData.clear()
                    recorder.startRecording()
                    currentState = RecordingState.RECORDING

                    recordingJob =
                        launch {
                            val buffer = ByteArray(bufferSize)
                            while (isActive && isRecording()) {
                                val bytesRead = recorder.read(buffer, 0, bufferSize)
                                if (bytesRead > 0) {
                                    recordedData.add(buffer.copyOf(bytesRead))
                                } else {
                                    currentState = RecordingState.ERROR
                                    throw RuntimeException("Error reading audio data")
                                }
                            }
                        }
                }
            }

        override suspend fun stopRecording(): Result<ByteArray> =
            withContext(Dispatchers.IO) {
                runCatching {
                    if (!isRecording()) {
                        throw IllegalStateException("Not recording")
                    }

                    recordingJob?.cancelAndJoin()
                    audioRecorder?.stop()
                    currentState = RecordingState.IDLE

                    val totalSize = recordedData.sumOf { it.size }
                    val combinedData = ByteArray(totalSize)
                    var offset = 0

                    recordedData.forEach { buffer ->
                        buffer.copyInto(combinedData, offset)
                        offset += buffer.size
                    }

                    recordedData.clear()
                    combinedData
                }
                    .onFailure { currentState = RecordingState.ERROR }
            }

        override fun cancelRecording(): Boolean {
            if (!isRecording()) return false

            return try {
                recordingJob?.cancel()
                audioRecorder?.stop()
                recordedData.clear()
                currentState = RecordingState.IDLE
                true
            } catch (e: Exception) {
                currentState = RecordingState.ERROR
                false
            }
        }

        override fun release() {
            cancelRecording()
            audioRecorder?.release()
            audioRecorder = null
        }
    }
