package com.aman.vaak.handlers

import android.content.Context
import com.aman.vaak.R
import com.aman.vaak.managers.KeyboardManager
import com.aman.vaak.managers.NotifyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface KeyboardSwitchHandler {
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
    ) : KeyboardSwitchHandler {
        override fun handleSwitchKeyboard() {
            try {
                keyboardManager.showKeyboardSelector()
            } catch (e: Exception) {
                handleError(e)
            }
        }

        private fun handleError(error: Exception) {
            notifyManager.showError(
                title = context.getString(R.string.error_keyboard_selector),
                message = error.message ?: context.getString(R.string.error_unknown),
            )
        }
    }
