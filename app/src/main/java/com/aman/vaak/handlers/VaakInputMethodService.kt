package com.aman.vaak.handlers

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import com.aman.vaak.R
import com.aman.vaak.managers.ClipboardManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VaakInputMethodService : InputMethodService() {
    @Inject lateinit var clipboardManager: ClipboardManager

    override fun onCreateInputView(): View {
        return layoutInflater.inflate(R.layout.keyboard, null).apply {
            findViewById<Button>(R.id.pasteButton).setOnClickListener { handlePaste() }
        }
    }

    private fun handlePaste() {
        currentInputConnection?.let { inputConnection ->
            clipboardManager.pasteContent(inputConnection)
        }
    }
}
