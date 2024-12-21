package com.aman.vaak.managers

import com.aman.vaak.models.DictationState
import com.aman.vaak.models.DictationStatus
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
}

/**
 * Manages voice dictation process including recording, transcription and state management.
 */
interface DictationManager {
    /**
     * Returns a Flow of DictationState for observing dictation status
     * @return StateFlow of current DictationState
     */
    fun getDictationState(): Flow<DictationState>

    /**
     * Starts a new dictation session
     * @return Result<Unit> indicating success or failure
     * @throws DictationException.AlreadyDictatingException if recording is already in progress
     */
    suspend fun startDictation(): Result<Unit>

    /**
     * Completes current dictation session and returns transcribed text
     * @return Result<String> containing transcribed text on success
     * @throws DictationException.NotDictatingException if no recording is in progress
     * @throws DictationException.TranscriptionFailedException if transcription fails
     */
    suspend fun completeDictation(): Result<String>

    /**
     * Cancels current dictation session if active
     * @return Result<Unit> indicating success or failure
     * @throws DictationException.NotDictatingException if no recording is in progress
     */
    fun cancelDictation(): Result<Unit>

    /**
     * Releases all resources held by the DictationManager
     * Should be called from Activity/Service onDestroy()
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
        private val dictationState = MutableStateFlow(DictationState())
        private var timerJob: Job? = null
        private var startTime: Long = 0L

        override fun getDictationState(): Flow<DictationState> = dictationState.asStateFlow()

        private fun performCleanup(error: Exception? = null) {
            stopTimer()
            startTime = 0L
            transitionState(
                if (error != null) DictationStatus.ERROR else DictationStatus.IDLE,
                error = error,
            )
        }

        private fun transitionState(
            status: DictationStatus,
            error: Exception? = null,
        ) {
            if (status == DictationStatus.RECORDING) {
                startTimer()
            } else {
                stopTimer()
            }

            dictationState.update { current ->
                current.copy(
                    status = status,
                    error = error,
                    timeMillis = if (status == DictationStatus.RECORDING) current.timeMillis else 0L,
                )
            }
        }

        private fun validateCanStartDictation() {
            if (dictationState.value.status != DictationStatus.IDLE) {
                throw DictationException.AlreadyDictatingException()
            }
        }

        private fun validateCanCompleteDictation() {
            if (dictationState.value.status != DictationStatus.RECORDING) {
                throw DictationException.NotDictatingException()
            }
        }

        private fun validateCanCancelDictation() {
            if (dictationState.value.status !in listOf(DictationStatus.RECORDING, DictationStatus.TRANSCRIBING)) {
                throw DictationException.NotDictatingException()
            }
        }

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
                        delay(1000)
                    }
                }
        }

        private fun stopTimer() {
            timerJob?.cancel()
            timerJob = null
        }

        private suspend fun processRecording(): Result<String> =
            runCatching {
                val audioData = voiceManager.stopRecording().getOrThrow()
                val audioFile = fileManager.saveAudioFile(audioData, "wav")
                try {
                    whisperManager.transcribeAudio(audioFile).getOrThrow().text
                } finally {
                    fileManager.deleteAudioFile(audioFile)
                }
            }

        override suspend fun startDictation(): Result<Unit> =
            runCatching {
                validateCanStartDictation()
                voiceManager.startRecording().getOrThrow()
                transitionState(DictationStatus.RECORDING)
            }.onFailure { error ->
                performCleanup(error as? Exception)
            }

        override suspend fun completeDictation(): Result<String> =
            runCatching {
                validateCanCompleteDictation()
                transitionState(DictationStatus.TRANSCRIBING)
                try {
                    val result = processRecording().getOrThrow()
                    performCleanup()
                    result
                } catch (e: Exception) {
                    performCleanup(e)
                    throw DictationException.TranscriptionFailedException(e)
                }
            }

        override fun cancelDictation(): Result<Unit> =
            runCatching {
                validateCanCancelDictation()
                voiceManager.cancelRecording().getOrThrow()
                performCleanup()
            }

        override fun release() {
            performCleanup()
            voiceManager.release()
            whisperManager.release()
        }
    }
