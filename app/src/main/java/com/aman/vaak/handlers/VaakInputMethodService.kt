package com.aman.vaak.handlers

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.aman.vaak.R
import com.aman.vaak.managers.ClipboardManager
import com.aman.vaak.managers.DictationException
import com.aman.vaak.managers.DictationManager
import com.aman.vaak.managers.InputNotConnectedException
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.TextManager
import com.aman.vaak.managers.TextOperationFailedException
import com.aman.vaak.managers.TranscriptionException
import com.aman.vaak.managers.VaakFileException
import com.aman.vaak.managers.VoiceManager
import com.aman.vaak.managers.VoiceRecordingException
import com.aman.vaak.models.DictationState
import com.aman.vaak.models.DictationStatus
import com.aman.vaak.models.KeyboardState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class VaakInputMethodService : InputMethodService() {
    @Inject lateinit var clipboardManager: ClipboardManager

    @Inject lateinit var textManager: TextManager

    @Inject lateinit var voiceManager: VoiceManager

    @Inject lateinit var dictationManager: DictationManager

    @Inject lateinit var notifyManager: NotifyManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateCollectionJob: Job? = null
    private var keyboardState: KeyboardState? = null
    private var keyboardView: View? = null

    override fun onCreate() {
        super.onCreate()
        observeDictationState()
    }

    override fun onCreateInputView(): View {
        return layoutInflater.inflate(R.layout.keyboard, null).apply {
            keyboardView = this
            findViewById<Button>(R.id.pasteButton).setOnClickListener { handlePaste() }
            findViewById<Button>(R.id.switchKeyboardButton).setOnClickListener { handleSwitchKeyboard() }
            findViewById<Button>(R.id.settingsButton).setOnClickListener { handleSettings() }
            findViewById<Button>(R.id.selectAllButton).setOnClickListener { handleSelectAll() }
            findViewById<Button>(R.id.copyButton).setOnClickListener { handleCopy() }
            findViewById<Button>(R.id.enterButton).setOnClickListener { handleEnter() }
            findViewById<Button>(R.id.backspaceButton).setOnClickListener { handleBackspace() }
            findViewById<Button>(R.id.spaceButton).setOnClickListener { handleSpace() }
            findViewById<Button>(R.id.pushToTalkButton).setOnClickListener { handleVoiceRecord() }
            findViewById<Button>(R.id.cancelButton).setOnClickListener { handleCancelRecord() }
            findViewById<Button>(R.id.completeDictationButton).setOnClickListener { handleCompleteDictation() }
        }
    }

    private fun observeDictationState() {
        // FIXME: Should we check in onCreate before starting instead of Cancelling ?
        // Only cancel previous collection job
        stateCollectionJob?.cancel()

        stateCollectionJob =
            serviceScope.launch {
                dictationManager.getDictationState().collect { state ->
                    updateUiState(state)
                }
            }
    }

    private fun updateUiState(state: DictationState) {
        keyboardView?.post {
            updateTimerUI(state)
            updateButtonStates(state.status)
        }
    }

    private fun updateButtonStates(status: DictationStatus) {
        keyboardView?.apply {
            findViewById<Button>(R.id.pushToTalkButton).isEnabled = status == DictationStatus.IDLE
            val recordingActive = status == DictationStatus.RECORDING
            findViewById<Button>(R.id.cancelButton).isEnabled = recordingActive
            findViewById<Button>(R.id.completeDictationButton).isEnabled = recordingActive
        }
    }

    private fun updateTimerUI(state: DictationState) {
        keyboardView?.apply {
            val timerText = findViewById<TextView>(R.id.dictationTimerText)
            timerText.text =
                when (state.status) {
                    DictationStatus.TRANSCRIBING -> getString(R.string.timer_transcribing)
                    DictationStatus.RECORDING -> formatTime(state.timeMillis)
                    DictationStatus.IDLE -> getString(R.string.timer_initial)
                }
        }
    }

    private fun formatTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return getString(R.string.timer_format, minutes, seconds)
    }

    private fun handleError(error: Exception) {
        val (displayError, detailMessage) =
            when (error) {
                // Existing errors
                is SecurityException ->
                    Pair(getString(R.string.error_mic_permission), "Permission Error")
                is VoiceRecordingException.HardwareInitializationException ->
                    Pair(getString(R.string.error_mic_permission), "Audio Recorder Creation Failed")
                is VoiceRecordingException.AudioDataReadException ->
                    Pair(getString(R.string.error_record_failed), "Recording Error")
                is DictationException.AlreadyDictatingException ->
                    Pair(getString(R.string.error_already_dictating), "Already Dictating Error")
                is DictationException.NotDictatingException ->
                    Pair(getString(R.string.error_not_dictating), "Not Dictating Error")
                is DictationException.TranscriptionFailedException ->
                    Pair(getString(R.string.error_transcribe_failed), "Transcription Error")
                is InputNotConnectedException ->
                    Pair(getString(R.string.error_no_input), "Input Connection Error")
                is TextOperationFailedException ->
                    Pair(getString(R.string.error_text_operation), "Text Operation Error")

                // New Transcription Errors
                is TranscriptionException.InvalidApiKeyException ->
                    Pair(getString(R.string.error_invalid_api_key), error.message ?: "Invalid API Key")
                is TranscriptionException.InvalidModelException ->
                    Pair(getString(R.string.error_invalid_model), error.message ?: "Invalid Model")
                is TranscriptionException.InvalidLanguageException ->
                    Pair(getString(R.string.error_invalid_language), error.message ?: "Language Not Supported")
                is TranscriptionException.InvalidTemperatureException ->
                    Pair(getString(R.string.error_invalid_temperature), error.message ?: "Invalid Temperature")
                is TranscriptionException.NetworkException ->
                    Pair(getString(R.string.error_network_transcription), error.message ?: "Network Error")
                is TranscriptionException.TranscriptionFailedException ->
                    Pair(getString(R.string.error_transcription_failed), error.message ?: "Transcription Failed")

                // File Errors with specific types
                is VaakFileException.FileNotFoundException ->
                    Pair(getString(R.string.error_file_not_found), error.message)
                is VaakFileException.InvalidFormatException ->
                    Pair(getString(R.string.error_file_invalid), error.message)
                is VaakFileException.EmptyFileException ->
                    Pair(getString(R.string.error_file_empty), error.message)
                is VaakFileException.FileTooLargeException ->
                    Pair(getString(R.string.error_file_too_large), error.message)
                is VaakFileException -> // Fallback for any new file exceptions
                    Pair(getString(R.string.error_unknown), error.message)

                else ->
                    Pair(getString(R.string.error_unknown), error.message ?: "Unknown Error")
            }

        notifyManager.showError(title = displayError, message = detailMessage ?: "Error occurred")
    }

    private fun handleVoiceRecord() {
        serviceScope.launch {
            dictationManager.startDictation()
                .onFailure { e -> handleError(e as Exception) }
        }
    }

    private fun handleCancelRecord() {
        serviceScope.launch {
            dictationManager.cancelDictation()
                .onSuccess { showToast("❌") }
                .onFailure { e -> handleError(e as Exception) }
        }
    }

    private fun handleCompleteDictation() {
        serviceScope.launch {
            dictationManager.completeDictation()
                .onSuccess { text ->
                    textManager.insertText(text)
                    showToast("✓")
                }
                .onFailure { e -> handleError(e as Exception) }
        }
    }

    private fun handleTextOperation(operation: () -> Unit) {
        try {
            operation()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    private fun handleSelectAll() {
        handleTextOperation { textManager.selectAll() }
    }

    private fun handleCopy() {
        // FIXME: Implement copy functionality
    }

    private fun handleEnter() {
        handleTextOperation { textManager.insertNewLine() }
    }

    private fun handleBackspace() {
        handleTextOperation { textManager.handleBackspace() }
    }

    private fun handleSpace() {
        handleTextOperation { textManager.insertSpace() }
    }

    private fun handleSettings() {
        val intent =
            Intent(this, VaakSettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        startActivity(intent)
    }

    private fun handlePaste() {
        keyboardState?.inputConnection?.let { inputConnection ->
            clipboardManager.pasteContent(inputConnection)
        }
    }

    private fun handleSwitchKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)?.showInputMethodPicker()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStartInput(
        info: EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInput(info, restarting)
        keyboardState = KeyboardState(currentInputConnection, info)
        try {
            textManager.attachInputConnection(currentInputConnection)
        } catch (e: Exception) {
            handleError(e)
        }

        // Force reset dictation state on new input
        dictationManager.cancelDictation()
            .onFailure { e -> handleError(e as Exception) }
    }

    override fun onFinishInput() {
        keyboardState = null
        textManager.detachInputConnection()
        dictationManager.cancelDictation()
            .onFailure { e -> handleError(e as Exception) }
        super.onFinishInput()
    }

    override fun onDestroy() {
        stateCollectionJob?.cancel()
        serviceScope.cancel()
        dictationManager.release()
        notifyManager.release()
        super.onDestroy()
    }
}
