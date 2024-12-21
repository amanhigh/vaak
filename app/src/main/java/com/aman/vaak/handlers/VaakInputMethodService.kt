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
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.TextManager
import com.aman.vaak.managers.VoiceManager
import com.aman.vaak.managers.VoiceRecordingException
import com.aman.vaak.models.DictationState
import com.aman.vaak.models.DictationStatus
import com.aman.vaak.models.KeyboardState
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
            }
        }
    }

    private fun updateTimerUI(state: DictationState) {
        keyboardView?.post {
            val timerText = keyboardView?.findViewById<TextView>(R.id.dictationTimerText)
            timerText?.text =
                when {
                    state.error != null -> getErrorText(state.error)
                    state.status == DictationStatus.TRANSCRIBING -> getString(R.string.timer_transcribing)
                    state.status == DictationStatus.RECORDING -> formatTime(state.timeMillis)
                    else -> getString(R.string.timer_initial)
                }
        }
    }

    private fun formatTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return getString(R.string.timer_format, minutes, seconds)
    }

    private fun getErrorText(error: Exception): String {
        val (displayError, detailMessage) =
            when (error) {
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
                else ->
                    Pair(getString(R.string.error_unknown), "Unknown Error")
            }

        // Show notification with both messages
        notifyManager.showError(
            title = displayError,
            message = "$detailMessage: ${error.message}",
        )

        return displayError
    }

    private fun handleVoiceRecord() {
        dictationScope.launch {
            dictationManager.startDictation()
                .onFailure { e -> getErrorText(e as Exception) }
        }
    }

    private fun handleCancelRecord() {
        dictationManager.cancelDictation()
            .onSuccess { showToast("❌") }
            .onFailure { e -> getErrorText(e as Exception) }
    }

    private fun handleCompleteDictation() {
        dictationScope.launch {
            dictationManager.completeDictation()
                .onSuccess { text ->
                    textManager.insertText(text)
                    showToast("✓")
                }
                .onFailure { e -> getErrorText(e as Exception) }
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

        // Force reset dictation state on new input
        dictationManager.release()
    }

    override fun onFinishInput() {
        keyboardState = null
        textManager.detachInputConnection()
        dictationManager.cancelDictation() // This now returns Result but we can ignore it during cleanup
        super.onFinishInput()
    }

    override fun onDestroy() {
        dictationManager.release()
        notifyManager.release()
        super.onDestroy()
    }
}
