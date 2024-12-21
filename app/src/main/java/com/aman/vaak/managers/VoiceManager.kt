package com.aman.vaak.managers

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class VoiceRecordingException(message: String) : Exception(message) {
    class AlreadyRecordingException : VoiceRecordingException("Already recording")

    class NotRecordingException : VoiceRecordingException("Not currently recording")

    class HardwareInitializationException : VoiceRecordingException("Unable to Create Audio Recorder")

    class AudioDataReadException : VoiceRecordingException("Error reading audio data")
}

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
     * @return Result<Unit> indicating success or failure
     * @throws NotRecordingException if no recording is in progress
     */
    fun cancelRecording(): Result<Unit>

    /**
     * Releases all resources held by the VoiceManager
     * Should be called from InputActivity.onDestroy()
     */
    fun release()
}

class VoiceManagerImpl
    @Inject
    constructor(private val systemManager: SystemManager) : VoiceManager {
        companion object {
            private const val SAMPLE_RATE = 44100
            private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
            private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        }

        @Volatile
        private var isRecording = false
        private val recordedData = mutableListOf<ByteArray>()
        private var bufferSize: Int = 0

        init {
            bufferSize = systemManager.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        }

        override fun isRecording(): Boolean = isRecording

        private fun createAndStartRecorder(): AudioRecord {
            val recorder =
                systemManager.createAudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize,
                )
            recorder.startRecording()
            isRecording = true
            return recorder
        }

        private fun processAudioData(recorder: AudioRecord): ByteArray? {
            val buffer = ByteArray(bufferSize)
            val bytesRead = recorder.read(buffer, 0, bufferSize)
            return if (bytesRead > 0) {
                buffer.copyOf(bytesRead)
            } else {
                null
            }
        }

        private fun combineRecordedData(): ByteArray {
            val totalSize = recordedData.sumOf { it.size }
            val combinedData = ByteArray(totalSize)
            var offset = 0
            recordedData.forEach { buffer ->
                buffer.copyInto(combinedData, offset)
                offset += buffer.size
            }
            return combinedData
        }

        private fun cleanupRecording() {
            isRecording = false
            recordedData.clear()
        }

        override suspend fun startRecording(): Result<Unit> =
            withContext(Dispatchers.IO) {
                runCatching {
                    if (isRecording) {
                        throw VoiceRecordingException.AlreadyRecordingException()
                    }

                    val recorder = createAndStartRecorder()
                    try {
                        while (isRecording) {
                            val data = processAudioData(recorder)
                            if (data != null) {
                                recordedData.add(data)
                            } else {
                                throw VoiceRecordingException.AudioDataReadException()
                            }
                        }
                    } finally {
                        recorder.stop()
                        recorder.release()
                    }
                }
            }

        override suspend fun stopRecording(): Result<ByteArray> =
            withContext(Dispatchers.IO) {
                runCatching {
                    if (!isRecording) {
                        throw VoiceRecordingException.NotRecordingException()
                    }

                    val recordedAudio = combineRecordedData()
                    cleanupRecording()
                    recordedAudio
                }
            }

        override fun cancelRecording(): Result<Unit> =
            runCatching {
                if (!isRecording) {
                    throw VoiceRecordingException.NotRecordingException()
                }
                cleanupRecording()
            }

        override fun release() {
            cleanupRecording()
        }
    }
