package com.aman.vaak.models

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

/**
 * Represents the current state of the keyboard.
 *
 * @property inputConnection Current input connection for text manipulation
 * @property editorInfo Information about the current input field
 */
data class KeyboardState(
    val inputConnection: InputConnection?,
    val editorInfo: EditorInfo?,
)
