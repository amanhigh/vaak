package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import javax.inject.Inject

interface TextManager {
    /**
     * Insert space at current cursor position
     * @return true if operation successful, false otherwise
     */
    fun insertSpace(): Boolean

    /**
     * Delete character before cursor position
     * @return true if operation successful, false otherwise
     */
    fun handleBackspace(): Boolean

    /**
     * Insert line break at current cursor position
     * @return true if operation successful, false otherwise
     */
    fun insertNewLine(): Boolean

    /**
     * Select all text in current input field
     * @return true if operation successful, false otherwise
     */
    fun selectAll(): Boolean
}

class TextManagerImpl @Inject constructor() : TextManager {
    private var inputConnection: InputConnection? = null
    
    fun attachInputConnection(ic: InputConnection?) {
        inputConnection = ic
    }

    override fun insertSpace(): Boolean {
        return inputConnection?.commitText(" ", 1) ?: false
    }

    override fun handleBackspace(): Boolean {
        return inputConnection?.deleteSurroundingText(1, 0) ?: false
    }

    override fun insertNewLine(): Boolean {
        return inputConnection?.commitText("\n", 1) ?: false
    }

    override fun selectAll(): Boolean {
        val ic = inputConnection ?: return false
        return ic.performContextMenuAction(android.R.id.selectAll)
    }
}
