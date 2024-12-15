package com.aman.vaak.handlers

import android.inputmethodservice.InputMethodService
import android.view.View
import android.content.Intent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import com.aman.vaak.handlers.VaakSettingsActivity
import com.aman.vaak.R
import com.aman.vaak.managers.ClipboardManager
import com.aman.vaak.models.KeyboardState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VaakInputMethodService : InputMethodService() {
    @Inject lateinit var clipboardManager: ClipboardManager

    private var keyboardState: KeyboardState? = null

    override fun onCreateInputView(): View {
        return layoutInflater.inflate(R.layout.keyboard, null).apply {
            findViewById<Button>(R.id.pasteButton).setOnClickListener { handlePaste() }
            findViewById<Button>(R.id.switchKeyboardButton).setOnClickListener {
                handleSwitchKeyboard()
            }
            findViewById<Button>(R.id.settingsButton).setOnClickListener {
                handleSettings()
            }
        }
    }

    private fun handleSettings() {
        val intent = Intent(this, VaakSettingsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        keyboardState = KeyboardState(currentInputConnection, info)
    }

    override fun onFinishInput() {
        keyboardState = null
        super.onFinishInput()
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
