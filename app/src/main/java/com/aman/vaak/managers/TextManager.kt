package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import javax.inject.Inject

sealed class TextOperationException(message: String) : Exception(message)

class InputNotConnectedException : TextOperationException("No input connection available")

class TextOperationFailedException(operation: String) :
    TextOperationException("Failed to perform text operation: $operation")

interface TextManager {
    /**
     * Attaches an InputConnection for text operations
     * @param ic InputConnection to be used for text operations
     * @throws TextOperationFailedException if attachment fails
     */
    fun attachInputConnection(ic: InputConnection?)

    /**
     * Detaches current InputConnection and cleans up
     */
    fun detachInputConnection()

    /**
     * Insert space at current cursor position
     * @throws InputNotConnectedException if no input connection available
     * @throws TextOperationFailedException if operation fails
     */
    fun insertSpace()

    /**
     * Delete character before cursor position
     * @throws InputNotConnectedException if no input connection available
     * @throws TextOperationFailedException if operation fails
     */
    fun handleBackspace()

    /**
     * Insert line break at current cursor position
     * @throws InputNotConnectedException if no input connection available
     * @throws TextOperationFailedException if operation fails
     */
    fun insertNewLine()

    /**
     * Select all text in current input field
     * @throws InputNotConnectedException if no input connection available
     * @throws TextOperationFailedException if operation fails
     */
    fun selectAll()

    /**
     * Insert text at current cursor position
     * @throws InputNotConnectedException if no input connection available
     * @throws TextOperationFailedException if operation fails
     */
    fun insertText(text: String)
}

class TextManagerImpl
    @Inject
    constructor() : TextManager {
        private var inputConnection: InputConnection? = null

        private fun validateConnection() {
            if (inputConnection == null) throw InputNotConnectedException()
        }

        private fun validateOperation(
            result: Boolean,
            operation: String,
        ) {
            if (!result) throw TextOperationFailedException(operation)
        }

        private fun isInputConnected(): Boolean = inputConnection != null

        override fun attachInputConnection(ic: InputConnection?) {
            inputConnection = ic
            validateOperation(isInputConnected(), "Input connection attachment")
        }

        override fun detachInputConnection() {
            inputConnection = null
        }

        override fun insertSpace() {
            validateConnection()
            val result = inputConnection?.commitText(" ", 1) ?: false
            validateOperation(result, "Insert space")
        }

        override fun handleBackspace() {
            validateConnection()
            val result = inputConnection?.deleteSurroundingText(1, 0) ?: false
            validateOperation(result, "Backspace")
        }

        override fun insertNewLine() {
            validateConnection()
            val result = inputConnection?.commitText("\n", 1) ?: false
            validateOperation(result, "Insert new line")
        }

        override fun selectAll() {
            validateConnection()
            val result = inputConnection?.performContextMenuAction(android.R.id.selectAll) ?: false
            validateOperation(result, "Select all")
        }

        override fun insertText(text: String) {
            validateConnection()
            val result = inputConnection?.commitText(text, 1) ?: false
            validateOperation(result, "Insert text")
        }
    }
