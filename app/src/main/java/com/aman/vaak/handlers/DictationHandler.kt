package com.aman.vaak.handlers

import android.view.View
import android.widget.Button
import android.widget.TextView
import com.aman.vaak.R
import com.aman.vaak.managers.DictationException
import com.aman.vaak.managers.DictationManager
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.TextManager
import com.aman.vaak.managers.VoiceRecordingException
import com.aman.vaak.models.DictationState
import com.aman.vaak.models.DictationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface DictationHandler {
    /**
     * Start observing dictation state changes
     * @param parentView View containing dictation UI elements
     */
    fun startObservingState(parentView: View)

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

    /**
     * Releases resources and stops state observation
     */
    fun release()
}

@Singleton
class DictationHandlerImpl
    @Inject
    constructor(
        private val dictationManager: DictationManager,
        private val notifyManager: NotifyManager,
        private val textManager: TextManager,
        private val scope: CoroutineScope,
    ) : DictationHandler {
        private var stateCollectionJob: Job? = null
        private var currentView: View? = null

        override fun startObservingState(parentView: View) {
            currentView = parentView
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

        override fun release() {
            stateCollectionJob?.cancel()
            stateCollectionJob = null
            currentView = null
            dictationManager.release()
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
                    is SecurityException -> R.string.error_mic_permission
                    is DictationException.AlreadyDictatingException -> R.string.error_already_dictating
                    is DictationException.NotDictatingException -> R.string.error_not_dictating
                    is DictationException.TranscriptionFailedException -> R.string.error_transcribe_failed
                    is VoiceRecordingException.HardwareInitializationException -> R.string.error_record_state
                    else -> R.string.error_unknown
                }

            currentView?.context?.let { context ->
                notifyManager.showError(
                    title = context.getString(title),
                    message = error.message ?: "Details Unknown",
                )
            }
        }

        private fun formatTime(
            context: android.content.Context,
            millis: Long,
        ): String {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

            return when {
                minutes < 1 -> context.getString(R.string.timer_format_green, minutes, seconds)
                minutes < 2 -> context.getString(R.string.timer_format_yellow, minutes, seconds)
                else -> context.getString(R.string.timer_format_red, minutes, seconds)
            }
        }
    }
