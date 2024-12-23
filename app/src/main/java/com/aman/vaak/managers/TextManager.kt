package com.aman.vaak.managers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TextOperationException(message: String) : InputOperationException(message)

class TextOperationFailedException(operation: String) :
    TextOperationException("Failed to perform text operation: $operation")

interface TextManager : BaseInputManager {
    /**
     * Insert text at current cursor position
     * @throws InputNotConnectedException if no input connection available
     * @throws TextOperationFailedException if operation fails
     */
    fun insertText(text: String)

    /**
     * Deletes a specified number of characters before the cursor.
     * @param count The number of characters to delete (default is 1).
     * @return True if the deletion was successful, false otherwise.
     */
    fun deleteCharacter(count: Int = 1): Boolean

    /**
     * Deletes the currently selected text, if any.
     * @return True if the deletion was successful, false otherwise.
     */
    fun deleteSelection(): Boolean

    /**
     * Insert space at current cursor position
     * @throws InputNotConnectedException if no input connection available
     * @throws TextOperationFailedException if operation fails
     */
    fun insertSpace()

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
     * Starts continuous deletion of characters.
     */
    fun startContinuousDelete()

    /**
     * Stops continuous deletion of characters.
     */
    fun stopContinuousDelete()
}

class TextManagerImpl
    @Inject
    constructor(
        private val scope: CoroutineScope,
    ) : BaseInputManagerImpl(), TextManager {
        private var continuousDeleteJob: Job? = null

        override fun detachInputConnection() {
            stopContinuousDelete()
            super.detachInputConnection()
        }

        override fun insertText(text: String) {
            if (!requireInputConnection().commitText(text, 1)) {
                throw TextOperationFailedException("Insert text")
            }
        }

        override fun deleteCharacter(count: Int): Boolean {
            return requireInputConnection().deleteSurroundingText(count, 0)
        }

        override fun deleteSelection(): Boolean {
            val inputConnection = requireInputConnection()
            val selectedText = inputConnection.getSelectedText(0)
            if (selectedText != null && selectedText.isNotEmpty()) {
                return inputConnection.commitText("", 1)
            }
            return false
        }

        private fun deleteWord(): Boolean {
            val inputConnection = requireInputConnection()
            val beforeCursor = inputConnection.getTextBeforeCursor(50, 0) ?: return false
            val lastSpace = beforeCursor.lastIndexOf(' ')
            return if (lastSpace >= 0) {
                inputConnection.deleteSurroundingText(beforeCursor.length - lastSpace, 0)
            } else {
                inputConnection.deleteSurroundingText(beforeCursor.length, 0)
            }
        }

        override fun startContinuousDelete() {
            continuousDeleteJob?.cancel()
            continuousDeleteJob =
                scope.launch {
                    while (isActive) {
                        deleteCharacter()
                        delay(50)
                    }
                }
        }

        override fun stopContinuousDelete() {
            continuousDeleteJob?.cancel()
            continuousDeleteJob = null
        }

        override fun insertSpace() {
            insertText(" ")
        }

        override fun insertNewLine() {
            insertText("\n")
        }

        override fun selectAll() {
            val inputConnection = requireInputConnection()
            inputConnection.performContextMenuAction(android.R.id.selectAll)
        }
    }
