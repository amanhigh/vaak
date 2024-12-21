package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import com.aman.vaak.models.DictationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.Result

sealed class DictationException(message: String) : Exception(message) {
    class AlreadyDictatingException : DictationException("Already recording or transcribing")

    class NotDictatingException : DictationException("Not currently recording")

    class TranscriptionFailedException(cause: Throwable) : DictationException("Failed to transcribe audio: ${cause.message}")

    class InputConnectionException : DictationException("No input connection available")
}

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
        private val scope: CoroutineScope,
    ) : DictationManager {
        private var inputConnection: InputConnection? = null
        private val dictationState = MutableStateFlow(DictationState())
        private var timerJob: Job? = null
        private var startTime: Long = 0L

        override fun getDictationState(): Flow<DictationState> = dictationState.asStateFlow()

        private fun startTimer() {
            startTime = System.currentTimeMillis()
            timerJob =
                scope.launch {
                    while (isActive) {
                        val currentTime = System.currentTimeMillis()
                        dictationState.update {
                            it.copy(
                                timeMillis = currentTime - startTime,
                            )
                        }
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
            if (dictationState.value.isRecording || dictationState.value.isTranscribing) {
                cancelDictation()
            }

            // Release dependent managers
            voiceManager.release()
            whisperManager.release()

            // Detach input connection
            detachInputConnection()

            // Reset state and stop timer
            stopTimer()
            resetState()
        }

        private fun resetState() {
            dictationState.value = DictationState()
            voiceManager.cancelRecording() // Force cancel any stuck recording
            stopTimer()
        }

        override suspend fun startDictation(): Result<Unit> =
            runCatching {
                if (dictationState.value.isRecording || dictationState.value.isTranscribing) {
                    throw DictationException.AlreadyDictatingException()
                }

                try {
                    voiceManager.startRecording().getOrThrow()
                    dictationState.update {
                        it.copy(
                            isRecording = true,
                            error = null,
                        )
                    }
                    startTimer()
                } catch (e: VoiceRecordingException) {
                    dictationState.update {
                        it.copy(error = e)
                    }
                    throw e
                }
            }

        override suspend fun completeDictation(): Result<String> =
            runCatching {
                if (!dictationState.value.isRecording) {
                    throw DictationException.NotDictatingException()
                }

                if (inputConnection == null) {
                    throw DictationException.InputConnectionException()
                }

                dictationState.update {
                    it.copy(
                        isRecording = false,
                        isTranscribing = true,
                        error = null,
                    )
                }
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
                    } catch (e: Exception) {
                        throw DictationException.TranscriptionFailedException(e)
                    } finally {
                        // Cleanup temp file
                        fileManager.deleteAudioFile(audioFile)
                        dictationState.update {
                            it.copy(
                                isTranscribing = false,
                            )
                        }
                    }
                } catch (e: Exception) {
                    dictationState.update {
                        it.copy(error = e)
                    }
                    throw e
                }
            }

        override fun cancelDictation(): Boolean {
            if (!(dictationState.value.isRecording || dictationState.value.isTranscribing)) return false

            val cancelled =
                if (dictationState.value.isRecording) {
                    voiceManager.cancelRecording()
                } else {
                    true // Allow cancellation during transcription
                }

            stopTimer()
            dictationState.value = DictationState()
            return cancelled
        }

        override fun attachInputConnection(inputConnection: InputConnection?) {
            this.inputConnection = inputConnection
        }

        override fun detachInputConnection() {
            inputConnection = null
        }
    }
