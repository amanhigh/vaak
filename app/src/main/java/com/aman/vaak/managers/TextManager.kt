package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import javax.inject.Inject

interface TextManager {
    /**
     * Attaches an InputConnection for text operations
     * @param ic InputConnection to be used for text operations
     * @return true if attachment successful, false otherwise
     */
    fun attachInputConnection(ic: InputConnection?): Boolean

    /**
     * Detaches current InputConnection and cleans up
     */
    fun detachInputConnection()

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
    
    private fun isInputConnected(): Boolean = inputConnection != null

    override fun attachInputConnection(ic: InputConnection?): Boolean {
        inputConnection = ic
        return isInputConnected()
    }

    override fun detachInputConnection() {
        inputConnection = null
    }

    override fun insertSpace(): Boolean {
        if (!isInputConnected()) return false
        return inputConnection?.commitText(" ", 1) ?: false
    }

    override fun handleBackspace(): Boolean {
        if (!isInputConnected()) return false
        return inputConnection?.deleteSurroundingText(1, 0) ?: false
    }

    override fun insertNewLine(): Boolean {
        if (!isInputConnected()) return false
        return inputConnection?.commitText("\n", 1) ?: false
    }

    override fun selectAll(): Boolean {
        if (!isInputConnected()) return false
        return inputConnection?.performContextMenuAction(android.R.id.selectAll) ?: false
    }
}
