package com.aman.vaak.managers

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.nio.ByteBuffer
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive

/**
 * Manages voice recording operations for the keyboard.
 * Uses SystemManager for Android SDK audio operations to improve testability.
 */
interface VoiceManager {
    /**
     * Checks if voice recording is currently active
     * @return true if recording is in progress, false otherwise
     */
    fun isRecording(): Boolean

    /**
     * Starts voice recording if not already recording
     * @return true if recording started successfully, false otherwise
     */
    suspend fun startRecording(): Boolean

    /**
     * Cancels current recording if active
     * @return true if recording was canceled successfully, false otherwise
     */
    fun cancelRecording(): Boolean
}

class VoiceManagerImpl @Inject constructor(
    private val systemManager: SystemManager
) : VoiceManager {
    private enum class RecordingState { IDLE, RECORDING, ERROR }
    private var currentState: RecordingState = RecordingState.IDLE
    private var recordingBuffer: ByteBuffer? = null
    private var audioRecorder: AudioRecord? = null
    private var bufferSize: Int = 0

    override fun isRecording(): Boolean =
        currentState == RecordingState.RECORDING

    override suspend fun startRecording(): Boolean = withContext(Dispatchers.IO) {
        if (isRecording()) return@withContext false

        try {
            if (audioRecorder == null) {
                bufferSize = systemManager.getMinBufferSize(
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                audioRecorder = systemManager.createAudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            }

            recordingBuffer = ByteBuffer.allocateDirect(bufferSize)
            audioRecorder?.startRecording()
            currentState = RecordingState.RECORDING

            while (isActive && isRecording()) {
                val bytesRead = audioRecorder?.read(recordingBuffer!!, bufferSize) ?: -1
                if (bytesRead <= 0) {
                    currentState = RecordingState.ERROR
                    return@withContext false
                }
            }
            true
        } catch (e: Exception) {
            currentState = RecordingState.ERROR
            false
        }
    }

    override fun cancelRecording(): Boolean {
        if (!isRecording()) return false

        try {
            audioRecorder?.stop()
            recordingBuffer?.clear()
            recordingBuffer = null
            currentState = RecordingState.IDLE
            return true
        } catch (e: Exception) {
            currentState = RecordingState.ERROR
            return false
        }
    }

    private fun release() {
        cancelRecording()
        audioRecorder?.release()
        audioRecorder = null
    }

    protected fun finalize() {
        release()
    }
}
