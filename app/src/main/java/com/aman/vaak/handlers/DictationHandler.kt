package com.aman.vaak.handlers

import android.view.View
import android.widget.Button
import android.widget.TextView
import com.aman.vaak.R
import com.aman.vaak.managers.DictationException
import com.aman.vaak.managers.DictationManager
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.TextManager
import com.aman.vaak.managers.TranscriptionException
import com.aman.vaak.managers.TranslationException
import com.aman.vaak.managers.VaakFileException
import com.aman.vaak.managers.VoiceRecordingException
import com.aman.vaak.models.DictationState
import com.aman.vaak.models.DictationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface DictationHandler : BaseViewHandler {
    /**
     * Handles starting a new dictation session
     */
    fun handleStartDictation()

    /**
     * Handles canceling current dictation
     */
    fun handleCancelDictation()

    /**
     * Handles completing current dictation
     */
    fun handleCompleteDictation()
}

@Singleton
class DictationHandlerImpl
    @Inject
    constructor(
        private val dictationManager: DictationManager,
        private val notifyManager: NotifyManager,
        private val textManager: TextManager,
        private val scope: CoroutineScope,
    ) : BaseViewHandlerImpl(), DictationHandler {
        companion object {
            private const val SECONDS_PER_MINUTE = 60
        }

        private var stateCollectionJob: Job? = null

        override fun onViewAttached(view: View) {
            setupDictationViews(view)
            startObservingState()
        }

        override fun onViewDetached() {
            stateCollectionJob?.cancel()
            stateCollectionJob = null
            dictationManager.release()
        }

        private fun setupDictationViews(view: View) {
            // Setup view bindings and listeners
        }

        private fun startObservingState() {
            stateCollectionJob?.cancel()
            stateCollectionJob =
                scope.launch {
                    try {
                        dictationManager.watchDictationState()
                            .collect { state ->
                                updateUiState(state)
                            }
                    } catch (e: Exception) {
                        handleError(e)
                        notifyManager.showError(
                            title = currentView?.context?.getString(R.string.error_dictation_state) ?: "",
                            message = currentView?.context?.getString(R.string.error_dictation_retry) ?: "",
                        )
                    }
                }
        }

        override fun handleStartDictation() {
            scope.launch {
                try {
                    dictationManager.startDictation().getOrThrow()
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }

        override fun handleCancelDictation() {
            scope.launch {
                try {
                    dictationManager.cancelDictation()
                        .onSuccess {
                            notifyManager.showInfo("", "❌")
                        }
                        .onFailure { e -> handleError(e as Exception) }
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }

        override fun handleCompleteDictation() {
            scope.launch {
                try {
                    dictationManager.completeDictation()
                        .onSuccess { text ->
                            textManager.insertText(text)
                            notifyManager.showInfo("", "✓")
                        }
                        .onFailure { e -> handleError(e as Exception) }
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }

        private fun updateUiState(state: DictationState) {
            currentView?.let { view ->
                updateTimerUI(view, state)
                updateRecordingRowVisibility(view, state.status)
            }
        }

        private fun updateTimerUI(
            parentView: View,
            state: DictationState,
        ) {
            val context = parentView.context
            parentView.findViewById<TextView>(R.id.dictationTimerText)?.text =
                when (state.status) {
                    DictationStatus.IDLE -> context.getString(R.string.timer_initial)
                    DictationStatus.RECORDING -> formatTime(context, state.timeMillis)
                    DictationStatus.TRANSCRIBING -> context.getString(R.string.timer_transcribing)
                    DictationStatus.TRANSLATING -> context.getString(R.string.timer_translating)
                }
        }

        private fun updateRecordingRowVisibility(
            parentView: View,
            status: DictationStatus,
        ) {
            val isRecording = status == DictationStatus.RECORDING

            parentView.apply {
                findViewById<Button>(R.id.pushToTalkButton)?.visibility =
                    if (isRecording) View.GONE else View.VISIBLE

                findViewById<Button>(R.id.cancelButton)?.visibility =
                    if (isRecording) View.VISIBLE else View.GONE

                findViewById<Button>(R.id.completeDictationButton)?.visibility =
                    if (isRecording) View.VISIBLE else View.GONE
            }
        }

        private fun handleError(error: Exception) {
            val title =
                when (error) {
                    // Permission and Hardware Errors
                    is SecurityException ->
                        currentView?.context?.getString(R.string.error_mic_permission)
                    is VoiceRecordingException.HardwareInitializationException ->
                        currentView?.context?.getString(R.string.error_record_state)
                    // Dictation State Errors
                    is DictationException.AlreadyDictatingException ->
                        currentView?.context?.getString(R.string.error_already_dictating)
                    is DictationException.NotDictatingException ->
                        currentView?.context?.getString(R.string.error_not_dictating)
                    is DictationException.TranscriptionFailedException ->
                        currentView?.context?.getString(R.string.error_transcribe_failed)
                    // API Related Errors
                    is TranscriptionException.InvalidApiKeyException ->
                        currentView?.context?.getString(R.string.error_invalid_api_key)
                    is TranscriptionException.InvalidModelException ->
                        currentView?.context?.getString(R.string.error_invalid_model)
                    is TranscriptionException.InvalidLanguageException ->
                        currentView?.context?.getString(R.string.error_invalid_language)
                    is TranscriptionException.InvalidTemperatureException ->
                        currentView?.context?.getString(R.string.error_invalid_temperature)
                    is TranscriptionException.NetworkException ->
                        currentView?.context?.getString(R.string.error_network_transcription)
                    is TranscriptionException.TranscriptionFailedException ->
                        currentView?.context?.getString(R.string.error_transcription_failed)
                    // Translation Errors
                    is TranslationException.EmptyTextException ->
                        currentView?.context?.getString(R.string.error_empty_text)
                    is TranslationException.TranslationFailedException ->
                        currentView?.context?.getString(R.string.error_translation_failed)
                    // File Related Errors
                    is VaakFileException.FileNotFoundException ->
                        currentView?.context?.getString(R.string.error_file_not_found)
                    is VaakFileException.InvalidFormatException ->
                        currentView?.context?.getString(R.string.error_file_invalid)
                    is VaakFileException.EmptyFileException ->
                        currentView?.context?.getString(R.string.error_file_empty)
                    is VaakFileException.FileTooLargeException ->
                        currentView?.context?.getString(R.string.error_file_too_large)
                    else ->
                        currentView?.context?.getString(R.string.error_unknown)
                }

            currentView?.context?.let { context ->
                notifyManager.showError(
                    title = title ?: context.getString(R.string.error_unknown),
                    message = error.message ?: "Details Unknown",
                )
            }
        }

        private fun formatTime(
            context: android.content.Context,
            millis: Long,
        ): String {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % SECONDS_PER_MINUTE

            return when {
                minutes < 1 -> context.getString(R.string.timer_format_green, minutes, seconds)
                minutes < 2 -> context.getString(R.string.timer_format_yellow, minutes, seconds)
                else -> context.getString(R.string.timer_format_red, minutes, seconds)
            }
        }
    }
