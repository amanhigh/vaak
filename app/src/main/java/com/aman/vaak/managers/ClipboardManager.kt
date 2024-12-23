package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import javax.inject.Inject

interface ClipboardManager {
    /**
     * Copies the currently selected text to clipboard
     * @return True if the copy was successful, false otherwise
     * @throws InputNotConnectedException if no input connection available
     */
    fun copySelectedText(): Boolean

    /**
     * Pastes text from clipboard at current cursor position
     * @return True if the paste was successful, false otherwise
     * @throws InputNotConnectedException if no input connection available
     */
    fun pasteText(): Boolean

    /**
     * Attaches an InputConnection for clipboard operations
     */
    fun attachInputConnection(inputConnection: InputConnection?)

    /**
     * Detaches current InputConnection
     */
    fun detachInputConnection()
}

class ClipboardManagerImpl
    @Inject
    constructor() : ClipboardManager {
        private var currentInputConnection: InputConnection? = null

        override fun attachInputConnection(inputConnection: InputConnection?) {
            currentInputConnection = inputConnection
        }

        override fun detachInputConnection() {
            currentInputConnection = null
        }

        override fun copySelectedText(): Boolean {
            val inputConnection = currentInputConnection ?: throw InputNotConnectedException()
            return inputConnection.performContextMenuAction(android.R.id.copy)
        }

        override fun pasteText(): Boolean {
            val inputConnection = currentInputConnection ?: throw InputNotConnectedException()
            return inputConnection.performContextMenuAction(android.R.id.paste)
        }
    }
