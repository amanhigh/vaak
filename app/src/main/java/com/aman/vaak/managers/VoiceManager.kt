package com.aman.vaak.managers

import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import java.io.File
import javax.inject.Inject

sealed class VoiceRecordingException(message: String) : Exception(message) {
    class AlreadyRecordingException : VoiceRecordingException("Already recording")

    class NotRecordingException : VoiceRecordingException("Not currently recording")

    class HardwareInitializationException : VoiceRecordingException("Unable to Create Audio Recorder")
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
    suspend fun stopRecording(): Result<File>

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
        private val fileManager: FileManager,
        private val scope: CoroutineScope,
    ) : VoiceManager {
        private var mediaRecorder: MediaRecorder? = null
        private var outputFile: File? = null

        @Volatile
        private var isRecording = false

        private fun setupMediaRecorder(): Result<Unit> =
            runCatching {
                val file = fileManager.createTempFile("m4a")
                outputFile = file

                mediaRecorder =
                    MediaRecorder().apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setOutputFile(file.absolutePath)
                        prepare()
                    }
            }

        override fun isRecording(): Boolean = isRecording

        override suspend fun startRecording(): Result<Unit> =
            runCatching {
                if (isRecording) {
                    throw VoiceRecordingException.AlreadyRecordingException()
                }

                try {
                    setupMediaRecorder().getOrThrow()
                    mediaRecorder?.start() ?: throw VoiceRecordingException.HardwareInitializationException()
                    isRecording = true
                } catch (e: Exception) {
                    cleanupOnError()
                    throw when (e) {
                        is VoiceRecordingException -> e
                        else -> VoiceRecordingException.HardwareInitializationException()
                    }
                }
            }

        override suspend fun stopRecording(): Result<File> =
            runCatching {
                if (!isRecording) {
                    throw VoiceRecordingException.NotRecordingException()
                }

                try {
                    mediaRecorder?.apply {
                        stop()
                        release()
                    }
                    isRecording = false
                    mediaRecorder = null

                    outputFile ?: throw VoiceRecordingException.NotRecordingException()
                } catch (e: Exception) {
                    cleanupOnError()
                    throw e
                }
            }

        override fun cancelRecording(): Result<Unit> =
            runCatching {
                if (!isRecording) {
                    throw VoiceRecordingException.NotRecordingException()
                }
                cleanup()
            }

        private fun cleanupOnError() {
            outputFile?.let { fileManager.deleteFile(it) }
            cleanup()
        }

        private fun cleanup() {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
            } catch (e: Exception) {
                // Ignore errors during cleanup
            } finally {
                isRecording = false
                mediaRecorder = null
                outputFile = null
            }
        }

        override fun release() {
            cleanup()
        }
    }
