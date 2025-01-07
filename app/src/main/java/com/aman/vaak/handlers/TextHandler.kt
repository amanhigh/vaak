package com.aman.vaak.handlers

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.Button
import com.aman.vaak.R
import com.aman.vaak.managers.ClipboardManager
import com.aman.vaak.managers.InputNotConnectedException
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.TextManager
import com.aman.vaak.managers.TextOperationFailedException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface TextHandler : BaseViewHandler {
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
        private val promptKeyHandler: PromptKeyHandler,
        private val numpadHandler: NumpadHandler,
    ) : BaseViewHandlerImpl(), TextHandler {
        override fun onViewAttached(view: View) {
            setupTextButtons(view)
        }

        override fun onViewDetached() {
            // Clean up any view references if needed
        }

        private fun setupTextButtons(view: View) {
            setupPasteButton(view)
            setupSpaceButton(view)
            setupBackspaceButton(view)
            setupCopyButton(view)
            setupSelectAllButton(view)
            setupEnterButton(view)
            setupYesButton(view)
        }

        private fun setupYesButton(view: View) {
            view.findViewById<Button>(R.id.yesButton)?.setOnClickListener {
                try {
                    textManager.insertText("Yes, Lets Proceed.")
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }

        private fun setupPasteButton(view: View) {
            view.findViewById<Button>(R.id.pasteButton)?.apply {
                setOnClickListener {
                    try {
                        clipboardManager.pasteText()
                    } catch (e: Exception) {
                        handleError(e)
                    }
                }
                setOnLongClickListener {
                    promptKeyHandler.showPrompts()
                    true
                }
            }
        }

        private fun setupSpaceButton(view: View) {
            view.findViewById<Button>(R.id.spaceButton)?.apply {
                setOnClickListener {
                    try {
                        textManager.insertSpace()
                    } catch (e: Exception) {
                        handleError(e)
                    }
                }
                setOnLongClickListener {
                    numpadHandler.showNumpad()
                    true
                }
            }
        }

        private fun setupBackspaceButton(view: View) {
            view.findViewById<Button>(R.id.backspaceButton)?.apply {
                setOnClickListener {
                    try {
                        if (!textManager.deleteSelection()) {
                            textManager.deleteCharacter(1)
                        }
                    } catch (e: Exception) {
                        handleError(e)
                    }
                }
                setOnLongClickListener {
                    try {
                        textManager.startContinuousDelete()
                    } catch (e: Exception) {
                        handleError(e)
                    }
                    true
                }
                setOnTouchListener { _: View, event: MotionEvent ->
                    if (event.action == MotionEvent.ACTION_UP ||
                        event.action == MotionEvent.ACTION_CANCEL
                    ) {
                        handleBackspaceRelease()
                    }
                    false
                }
            }
        }

        private fun setupCopyButton(view: View) {
            view.findViewById<Button>(R.id.copyButton)?.setOnClickListener {
                try {
                    clipboardManager.copySelectedText()
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }

        private fun setupSelectAllButton(view: View) {
            view.findViewById<Button>(R.id.selectAllButton)?.setOnClickListener {
                try {
                    textManager.selectAll()
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }

        private fun setupEnterButton(view: View) {
            view.findViewById<Button>(R.id.enterButton)?.setOnClickListener {
                try {
                    textManager.insertNewLine()
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }

        private fun handleBackspaceRelease() {
            try {
                textManager.stopContinuousDelete()
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

        override fun handleError(error: Exception) {
            when (error) {
                is InputNotConnectedException -> {
                    notifyManager.showError(
                        title = context.getString(R.string.error_keyboard_connection),
                        message = context.getString(R.string.error_text_operation),
                    )
                }
                is TextOperationFailedException -> {
                    notifyManager.showError(
                        title = context.getString(R.string.error_text_operation),
                        message = error.message ?: context.getString(R.string.error_generic_details),
                    )
                }
                else -> {
                    notifyManager.showError(
                        title = context.getString(R.string.error_generic),
                        message = error.message ?: context.getString(R.string.error_generic_details),
                    )
                }
            }
        }
    }
