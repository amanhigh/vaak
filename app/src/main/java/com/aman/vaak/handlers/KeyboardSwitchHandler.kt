package com.aman.vaak.handlers

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.Button
import com.aman.vaak.R
import com.aman.vaak.managers.KeyboardManager
import com.aman.vaak.managers.NotifyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface KeyboardSwitchHandler : BaseViewHandler {
    /**
     * Show keyboard selector directly
     */
    fun handleSwitchKeyboard()
}

@Singleton
class KeyboardSwitchHandlerImpl
    @Inject
    constructor(
        private val keyboardManager: KeyboardManager,
        private val notifyManager: NotifyManager,
        @ApplicationContext private val context: Context,
    ) : BaseViewHandlerImpl(), KeyboardSwitchHandler {
        override fun onViewAttached(view: View) {
            setupSwitchButton(view)
        }

        override fun onViewDetached() {
            // No cleanup needed
        }

        private fun setupSwitchButton(view: View) {
            view.findViewById<Button>(R.id.switchKeyboardButton)?.apply {
                setOnClickListener {
                    handlePreviousInputSwitch()
                }
                setOnLongClickListener {
                    handleSwitchKeyboard()
                    true
                }
            }
        }

        override fun handleSwitchKeyboard() {
            try {
                keyboardManager.showKeyboardSelector()
            } catch (e: Exception) {
                handleError(e)
            }
        }

        private fun handlePreviousInputSwitch() {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // This will be handled by InputMethodService's switchToPreviousInputMethod()
                    return
                }
                // Fallback for older versions
                keyboardManager.showKeyboardSelector()
            } catch (e: Exception) {
                handleError(e)
            }
        }

        private fun handleError(error: Exception) {
            notifyManager.showError(
                title = context.getString(R.string.error_keyboard_selector),
                message = error.message ?: context.getString(R.string.error_generic_details),
            )
        }
    }
