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
import com.aman.vaak.managers.DictationManager
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.TextManager
import com.aman.vaak.managers.VoiceManager
import com.aman.vaak.models.DictationState
import com.aman.vaak.models.KeyboardState
import com.aman.vaak.models.TranscriptionException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class VaakInputMethodService : InputMethodService() {
    @Inject lateinit var clipboardManager: ClipboardManager

    @Inject lateinit var textManager: TextManager

    @Inject lateinit var voiceManager: VoiceManager

    @Inject lateinit var dictationManager: DictationManager

    @Inject lateinit var dictationScope: CoroutineScope

    @Inject lateinit var notifyManager: NotifyManager

    private var keyboardState: KeyboardState? = null
    private var keyboardView: View? = null

    override fun onCreateInputView(): View {
        observeDictationState()
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
        dictationScope.launch {
            dictationManager.getDictationState().collect { state ->
                updateTimerUI(state)
                updateButtonStates(state.isRecording)
            }
        }
    }

    private fun updateTimerUI(state: DictationState) {
        keyboardView?.apply {
            val timerText = findViewById<TextView>(R.id.dictationTimerText)
            timerText.text =
                when {
                    state.isError -> getErrorText(state.errorMessage)
                    state.isTranscribing -> getString(R.string.timer_transcribing)
                    state.isRecording -> formatTime(state.timeMillis)
                    else -> getString(R.string.timer_initial)
                }
        }
    }

    private fun formatTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return getString(R.string.timer_format, minutes, seconds)
    }

    private fun getErrorText(errorMessage: String?): String {
        val displayError =
            when {
                errorMessage?.contains("permission", ignoreCase = true) == true ->
                    getString(R.string.error_mic_permission)
                errorMessage?.contains("network", ignoreCase = true) == true ->
                    getString(R.string.error_network_failed)
                errorMessage?.contains("transcription", ignoreCase = true) == true ->
                    getString(R.string.error_transcribe_failed)
                else -> getString(R.string.error_record_state)
            }

        // Show notification with both messages
        notifyManager.showError(
            title = displayError,
            message = "Technical details: $errorMessage",
        )

        return displayError
    }

    private fun handleVoiceRecord() {
        dictationScope.launch {
            try {
                dictationManager.startDictation().getOrThrow()
            } catch (e: Exception) {
                handleDictationError(e)
            }
        }
    }

    private fun handleCancelRecord() {
        if (dictationManager.cancelDictation()) {
            showToast("❌") // Using emoji directly or we can add a string resource if preferred
        }
    }

    private fun handleCompleteDictation() {
        dictationScope.launch {
            try {
                dictationManager.completeDictation().getOrThrow()
            } catch (e: Exception) {
                handleDictationError(e)
            }
        }
    }

    private fun handleDictationError(error: Exception) {
        val message =
            when (error) {
                is SecurityException -> getString(R.string.error_mic_permission)
                is IllegalStateException -> getString(R.string.error_record_state)
                is TranscriptionException.NetworkError -> getString(R.string.error_network_failed)
                else -> getString(R.string.error_transcribe_failed)
            }

        // Show both friendly message and technical details
        notifyManager.showError(
            title = message,
            message = "Technical details: ${error.message}",
        )
    }

    private fun updateButtonStates(isRecording: Boolean) {
        keyboardView?.apply {
            findViewById<Button>(R.id.pushToTalkButton).visibility =
                if (isRecording) View.GONE else View.VISIBLE
            findViewById<Button>(R.id.cancelButton).visibility =
                if (isRecording) View.VISIBLE else View.GONE
            findViewById<Button>(R.id.completeDictationButton).visibility =
                if (isRecording) View.VISIBLE else View.GONE
        }
    }

    private fun handleSelectAll() {
        textManager.selectAll()
    }

    private fun handleCopy() {
        // FIXME: Implement copy functionality
    }

    private fun handleEnter() {
        textManager.insertNewLine()
    }

    private fun handleBackspace() {
        textManager.handleBackspace()
    }

    private fun handleSpace() {
        textManager.insertSpace()
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
        textManager.attachInputConnection(currentInputConnection)
        dictationManager.attachInputConnection(currentInputConnection)

        // Force reset dictation state on new input
        dictationManager.release()
        updateButtonStates(false)
    }

    override fun onFinishInput() {
        keyboardState = null
        textManager.detachInputConnection()
        dictationManager.detachInputConnection()
        dictationManager.cancelDictation()
        updateButtonStates(false)
        super.onFinishInput()
    }

    override fun onDestroy() {
        dictationManager.release()
        notifyManager.release()
        super.onDestroy()
    }
}
