package com.aman.vaak.handlers

import android.content.Context
import android.view.inputmethod.InputConnection
import com.aman.vaak.R
import com.aman.vaak.managers.ClipboardManager
import com.aman.vaak.managers.InputNotConnectedException
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.TextManager
import com.aman.vaak.managers.TextOperationFailedException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface TextHandler {
    /**
     * Copy selected text to clipboard
     * @return true if operation successful, false otherwise
     */
    fun handleCopy(): Boolean

    /**
     * Paste text from clipboard at current cursor position
     * @return true if operation successful, false otherwise
     */
    fun handlePaste(): Boolean

    /**
     * Select all text in current input field
     */
    fun handleSelectAll()

    /**
     * Insert a new line at current cursor position
     */
    fun handleEnter()

    /**
     * Insert a space at current cursor position
     */
    fun handleSpace()

    /**
     * Delete text based on current selection or cursor position
     */
    fun handleBackspace()

    /**
     * Start continuous delete operation
     */
    fun handleBackspaceLongPress()

    /**
     * Stop continuous delete operation
     */
    fun handleBackspaceRelease()

    /**
     * Insert text at current cursor position
     */
    fun handleInsertText(text: String)

    /**
     * Attach input connection for text operations
     */
    fun attachInputConnection(inputConnection: InputConnection)

    /**
     * Detach input connection
     */
    fun detachInputConnection()
}

@Singleton
class TextHandlerImpl
    @Inject
    constructor(
        private val clipboardManager: ClipboardManager,
        private val textManager: TextManager,
        private val notifyManager: NotifyManager,
        @ApplicationContext private val context: Context,
    ) : TextHandler {
        override fun handleCopy(): Boolean {
            return try {
                clipboardManager.copySelectedText()
            } catch (e: Exception) {
                handleError(e)
                false
            }
        }

        override fun handlePaste(): Boolean {
            return try {
                clipboardManager.pasteText()
            } catch (e: Exception) {
                handleError(e)
                false
            }
        }

        override fun handleSelectAll() {
            try {
                textManager.selectAll()
            } catch (e: Exception) {
                handleError(e)
            }
        }

        override fun handleEnter() {
            try {
                textManager.insertNewLine()
            } catch (e: Exception) {
                handleError(e)
            }
        }

        override fun handleSpace() {
            try {
                textManager.insertSpace()
            } catch (e: Exception) {
                handleError(e)
            }
        }

        override fun handleBackspace() {
            try {
                if (!textManager.deleteSelection()) {
                    textManager.deleteCharacter(1)
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }

        override fun handleBackspaceLongPress() {
            try {
                textManager.startContinuousDelete()
            } catch (e: Exception) {
                handleError(e)
            }
        }

        override fun handleBackspaceRelease() {
            try {
                textManager.stopContinuousDelete()
            } catch (e: Exception) {
                handleError(e)
            }
        }

        override fun handleInsertText(text: String) {
            try {
                textManager.insertText(text)
            } catch (e: Exception) {
                handleError(e)
            }
        }

        override fun attachInputConnection(inputConnection: InputConnection) {
            textManager.attachInputConnection(inputConnection)
            clipboardManager.attachInputConnection(inputConnection)
        }

        override fun detachInputConnection() {
            textManager.detachInputConnection()
            clipboardManager.detachInputConnection()
        }

        private fun handleError(error: Exception) {
            when (error) {
                is InputNotConnectedException -> {
                    notifyManager.showError(
                        title = context.getString(R.string.error_no_input),
                        message = context.getString(R.string.error_text_operation),
                    )
                }
                is TextOperationFailedException -> {
                    notifyManager.showError(
                        title = context.getString(R.string.error_text_operation),
                        message = error.message ?: context.getString(R.string.error_unknown),
                    )
                }
                else -> {
                    notifyManager.showError(
                        title = context.getString(R.string.error_unknown),
                        message = error.message ?: context.getString(R.string.error_unknown),
                    )
                }
            }
        }
    }
