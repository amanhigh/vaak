package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import javax.inject.Inject
import kotlin.Result

interface DictationManager {
    fun isDictating(): Boolean
    suspend fun startDictation(): Result<Unit>
    suspend fun completeDictation(): Result<String>
    fun cancelDictation(): Boolean
    fun attachInputConnection(inputConnection: InputConnection?)
    fun detachInputConnection()

    /**
     * Releases all resources held by the DictationManager Cascades release to:
     * - VoiceManager
     * - WhisperManager
     * - InputConnection detachment Should be called from InputActivity.onDestroy()
     */
    fun release()
}

class DictationManagerImpl
@Inject
constructor(
        private val voiceManager: VoiceManager,
        private val whisperManager: WhisperManager,
        private val fileManager: FileManager
) : DictationManager {

    private var inputConnection: InputConnection? = null
    private var currentlyTranscribing: Boolean = false

    override fun isDictating(): Boolean = voiceManager.isRecording() || currentlyTranscribing

    override fun release() {
        // Cancel any ongoing operations
        if (isDictating()) {
            cancelDictation()
        }

        // Release dependent managers
        voiceManager.release()
        whisperManager.release()

        // Detach input connection
        detachInputConnection()

        // Reset state
        currentlyTranscribing = false
    }

    override suspend fun startDictation(): Result<Unit> = runCatching {
        if (isDictating()) {
            throw IllegalStateException("Cannot start dictation while already dictating")
        }
        voiceManager.startRecording().getOrThrow()
    }

    override suspend fun completeDictation(): Result<String> = runCatching {
        if (!voiceManager.isRecording()) {
            throw IllegalStateException("Cannot complete dictation when not recording")
        }

        currentlyTranscribing = true
        try {
            // Stop recording and get audio data
            val audioData = voiceManager.stopRecording().getOrThrow()

            // Save audio to temporary file
            val audioFile = fileManager.saveAudioFile(audioData, "wav")

            try {
                // Transcribe audio
                val result = whisperManager.transcribeAudio(audioFile).getOrThrow()

                // Insert transcribed text
                inputConnection?.commitText(result.text, 1)

                result.text
            } finally {
                // Cleanup temp file
                fileManager.deleteAudioFile(audioFile)
            }
        } finally {
            currentlyTranscribing = false
        }
    }

    override fun cancelDictation(): Boolean {
        if (!isDictating()) return false

        val cancelled =
                if (voiceManager.isRecording()) {
                    voiceManager.cancelRecording()
                } else {
                    true // Allow cancellation during transcription
                }

        currentlyTranscribing = false
        return cancelled
    }

    override fun attachInputConnection(inputConnection: InputConnection?) {
        this.inputConnection = inputConnection
    }

    override fun detachInputConnection() {
        inputConnection = null
    }
}
