package com.aman.vaak.handlers

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import com.aman.vaak.R
import com.aman.vaak.managers.ClipboardManager
import com.aman.vaak.managers.DictationManager
import com.aman.vaak.managers.TextManager
import com.aman.vaak.managers.VoiceManager
import com.aman.vaak.models.KeyboardState
import com.aman.vaak.models.TranscriptionException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VaakInputMethodService : InputMethodService() {
    @Inject lateinit var clipboardManager: ClipboardManager
    @Inject lateinit var textManager: TextManager
    @Inject lateinit var voiceManager: VoiceManager
    @Inject lateinit var dictationManager: DictationManager
    @Inject lateinit var dictationScope: CoroutineScope

    private var keyboardState: KeyboardState? = null
    private var keyboardView: View? = null

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

    private fun handleVoiceRecord() {
        dictationScope.launch {
            try {
                updateButtonStates(true)
                dictationManager.startDictation().getOrThrow()
            } catch (e: Exception) {
                handleDictationError(e)
                updateButtonStates(false)
            }
        }
    }

    private fun handleCancelRecord() {
        if (dictationManager.cancelDictation()) {
            updateButtonStates(false)
            showToast(getString(R.string.dictation_error_cancelled))
        }
    }

    private fun handleCompleteDictation() {
        dictationScope.launch {
            try {
                dictationManager.completeDictation().getOrThrow()
                updateButtonStates(false)
            } catch (e: Exception) {
                handleDictationError(e)
                updateButtonStates(false)
            }
        }
    }

    private fun handleSettings() {
        val intent =
                Intent(this, VaakSettingsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        startActivity(intent)
    }

    override fun onCreateInputView(): View {
        return layoutInflater.inflate(R.layout.keyboard, null).apply {
            keyboardView = this
            findViewById<Button>(R.id.pasteButton).setOnClickListener { handlePaste() }
            findViewById<Button>(R.id.switchKeyboardButton).setOnClickListener {
                handleSwitchKeyboard()
            }
            findViewById<Button>(R.id.settingsButton).setOnClickListener { handleSettings() }
            findViewById<Button>(R.id.selectAllButton).setOnClickListener { handleSelectAll() }
            findViewById<Button>(R.id.copyButton).setOnClickListener { handleCopy() }
            findViewById<Button>(R.id.enterButton).setOnClickListener { handleEnter() }
            findViewById<Button>(R.id.backspaceButton).setOnClickListener { handleBackspace() }
            findViewById<Button>(R.id.spaceButton).setOnClickListener { handleSpace() }
            findViewById<Button>(R.id.pushToTalkButton).setOnClickListener { handleVoiceRecord() }
            findViewById<Button>(R.id.cancelButton).setOnClickListener { handleCancelRecord() }
            findViewById<Button>(R.id.completeDictationButton).setOnClickListener {
                handleCompleteDictation()
            }
        }
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        keyboardState = KeyboardState(currentInputConnection, info)
        textManager.attachInputConnection(currentInputConnection)
        dictationManager.attachInputConnection(currentInputConnection)
    }

    override fun onFinishInput() {
        keyboardState = null
        textManager.detachInputConnection()
        dictationManager.detachInputConnection()
        if (dictationManager.isDictating()) {
            dictationManager.cancelDictation()
        }
        updateButtonStates(false)
        super.onFinishInput()
    }

    override fun onDestroy() {
        dictationManager.release()
        super.onDestroy()
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

    private fun handleDictationError(error: Exception) {
        val message =
                when (error) {
                    is SecurityException -> getString(R.string.dictation_error_mic_denied)
                    is IllegalStateException -> getString(R.string.dictation_error_start)
                    is TranscriptionException.NetworkError ->
                            getString(R.string.dictation_error_network)
                    else -> getString(R.string.dictation_error_transcribe)
                }
        showToast(message)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun handlePaste() {
        keyboardState?.inputConnection?.let { inputConnection ->
            clipboardManager.pasteContent(inputConnection)
        }
    }

    private fun handleSwitchKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)?.showInputMethodPicker()
    }
}
