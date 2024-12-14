package com.aman.vaak

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.TextView

class VaakInputMethodService : InputMethodService() {
    override fun onCreateInputView(): View {
        return TextView(this).apply {
            text = "VaaK Keyboard"
        }
    }
}
