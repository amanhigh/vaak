package com.aman.vaak.managers

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
     * @return Result.success if recording started successfully, Result.failure with specific exception if failed
     * Possible exceptions:
     * - VoiceRecordingException.AlreadyRecordingException: Already recording
     * - VoiceRecordingException.HardwareInitializationException: Hardware or system error
     */
    suspend fun startRecording(): Result<Unit>

    /**
     * Stops current recording and returns recorded audio data
     * @return Result containing recorded audio as ByteArray if successful, failure with exception if error occurs
     * Possible exceptions:
     * - VoiceRecordingException.NotRecordingException: Not recording
     * - VoiceRecordingException.AudioDataReadException: Error processing audio data
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
    constructor(
        private val systemManager: SystemManager,
        private val scope: CoroutineScope,
    ) : VoiceManager {
        companion object {
            private const val SAMPLE_RATE = 44100
            private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
            private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        }

        @Volatile
        private var isRecording = false
        private var audioRecorder: AudioRecord? = null
        private val recordedData = mutableListOf<ByteArray>()
        private var bufferSize: Int = 0
        private var recordingJob: Job? = null

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

            try {
                recorder.startRecording()
                return recorder
            } catch (e: Exception) {
                recorder.release()
                throw VoiceRecordingException.HardwareInitializationException()
            }
        }

        private suspend fun processAudioData(recorder: AudioRecord): ByteArray? {
            return withContext(Dispatchers.IO) {
                val buffer = ByteArray(bufferSize)
                val bytesRead = recorder.read(buffer, 0, bufferSize)
                if (bytesRead > 0) {
                    buffer.copyOf(bytesRead)
                } else {
                    throw VoiceRecordingException.AudioDataReadException()
                }
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
            recordingJob?.cancel()
            recordingJob = null
            audioRecorder?.apply {
                stop()
                release()
            }
            audioRecorder = null
            recordedData.clear()
        }

        private suspend fun recordAudioLoop() {
            try {
                val recorder = audioRecorder ?: throw VoiceRecordingException.NotRecordingException()
                while (isRecording) {
                    val data = processAudioData(recorder)
                    if (data != null) {
                        recordedData.add(data)
                    }
                }
            } catch (e: Exception) {
                cleanupRecording()
                throw e
            }
        }

        override suspend fun startRecording(): Result<Unit> =
            runCatching {
                if (isRecording) {
                    throw VoiceRecordingException.AlreadyRecordingException()
                }

                // Initialize recorder first
                audioRecorder = createAndStartRecorder()
                isRecording = true

                // Launch recording loop in separate coroutine
                recordingJob =
                    scope.launch {
                        recordAudioLoop()
                    }
            }

        override suspend fun stopRecording(): Result<ByteArray> =
            runCatching {
                if (!isRecording) {
                    throw VoiceRecordingException.NotRecordingException()
                }

                // Stop recording first
                isRecording = false
                recordingJob?.join() // Wait for recording loop to complete

                try {
                    combineRecordedData()
                } finally {
                    cleanupRecording()
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
