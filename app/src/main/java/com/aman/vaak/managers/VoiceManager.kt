package com.aman.vaak.managers

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.nio.ByteBuffer
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext

interface VoiceManager {
    fun isRecording(): Boolean
    suspend fun startRecording(): Boolean
    fun cancelRecording(): Boolean
}

class VoiceManagerImpl @Inject constructor(
    private val audioRecorder: AudioRecord,
    private val bufferSize: Int = calculateMinBufferSize()
) : VoiceManager {
    private enum class RecordingState { IDLE, RECORDING, ERROR }
    private var currentState: RecordingState = RecordingState.IDLE
    private var recordingBuffer: ByteBuffer? = null

    override fun isRecording(): Boolean = 
        currentState == RecordingState.RECORDING

    override suspend fun startRecording(): Boolean = withContext(Dispatchers.IO) {
        if (isRecording()) return@withContext false
        
        try {
            recordingBuffer = ByteBuffer.allocateDirect(bufferSize)
            audioRecorder.startRecording()
            currentState = RecordingState.RECORDING

            while (isActive && isRecording()) {
                val bytesRead = audioRecorder.read(recordingBuffer!!, bufferSize)
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
            audioRecorder.stop()
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
        audioRecorder.release()
    }

    protected fun finalize() {
        release()
    }

    private companion object {
        private fun calculateMinBufferSize(): Int =
            AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
    }
}
