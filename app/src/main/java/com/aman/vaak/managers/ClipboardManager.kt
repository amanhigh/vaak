package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import com.aman.vaak.repositories.ClipboardRepository
import javax.inject.Inject

/** Interface for managing clipboard operations. */
interface ClipboardManager {
    /**
     * Pastes content into the input connection.
     *
     * @param inputConnection The current input connection to paste into
     * @return true if paste was successful, false otherwise
     */
    fun pasteContent(inputConnection: InputConnection): Boolean
}

class ClipboardManagerImpl
    @Inject
    constructor(private val repository: ClipboardRepository) :
    ClipboardManager {
        override fun pasteContent(inputConnection: InputConnection): Boolean {
            val text = repository.getClipboardText() ?: return false
            inputConnection.commitText(text, 1)
            return true
        }
    }
