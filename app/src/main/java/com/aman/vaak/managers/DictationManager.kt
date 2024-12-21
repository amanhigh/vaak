package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import javax.inject.Inject
import kotlin.Result
import com.aman.vaak.models.DictationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.lang.IllegalStateException
import kotlinx.coroutines.launch
import com.aman.vaak.managers.VoiceManager
import com.aman.vaak.managers.WhisperManager

interface DictationManager {
    fun getDictationState(): Flow<DictationState>
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
    private val fileManager: FileManager,
    private val scope: CoroutineScope
) : DictationManager {

    private var inputConnection: InputConnection? = null
    private val _dictationState = MutableStateFlow(DictationState())
    private var timerJob: Job? = null
    private var startTime: Long = 0L

    override fun getDictationState(): Flow<DictationState> = _dictationState.asStateFlow()

    private fun startTimer() {
        startTime = System.currentTimeMillis()
        timerJob = scope.launch {
            while(isActive) {
                val currentTime = System.currentTimeMillis()
                _dictationState.update { it.copy(
                    timeMillis = currentTime - startTime
                )}
                delay(1000) // Update every second
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun release() {
        // Cancel any ongoing operations
        if (_dictationState.value.isRecording || _dictationState.value.isTranscribing) {
            cancelDictation()
        }

        // Release dependent managers
        voiceManager.release()
        whisperManager.release()

        // Detach input connection
        detachInputConnection()

        // Reset state and stop timer
        stopTimer()
        _dictationState.value = DictationState()
    }

    override suspend fun startDictation(): Result<Unit> = runCatching {
        if (_dictationState.value.isRecording || _dictationState.value.isTranscribing) {
            throw IllegalStateException("Cannot start dictation while already dictating")
        }

        try {
            voiceManager.startRecording().getOrThrow()
            _dictationState.update { it.copy(
                isRecording = true,
                isError = false,
                errorMessage = null
            )}
            startTimer()
        } catch (e: Exception) {
            _dictationState.update { it.copy(
                isError = true,
                errorMessage = e.message ?: "Failed to start recording"
            )}
            throw e
        }
    }

    override suspend fun completeDictation(): Result<String> = runCatching {
        if (!_dictationState.value.isRecording) {
            throw IllegalStateException("Cannot complete dictation when not recording")
        }

        _dictationState.update { it.copy(
            isRecording = false,
            isTranscribing = true
        )}
        stopTimer()

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
                _dictationState.update { it.copy(
                    isTranscribing = false
                )}
            }
        } catch (e: Exception) {
            _dictationState.update { it.copy(
                isError = true,
                errorMessage = e.message ?: "Transcription failed"
            )}
            throw e
        }
    }

    override fun cancelDictation(): Boolean {
        if (!(_dictationState.value.isRecording || _dictationState.value.isTranscribing)) return false

        val cancelled = if (_dictationState.value.isRecording) {
            voiceManager.cancelRecording()
        } else {
            true // Allow cancellation during transcription
        }

        stopTimer()
        _dictationState.value = DictationState()
        return cancelled
    }

    override fun attachInputConnection(inputConnection: InputConnection?) {
        this.inputConnection = inputConnection
    }

    override fun detachInputConnection() {
        inputConnection = null
    }
}
