package com.aman.vaak.managers

import android.content.ContentResolver
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import javax.inject.Inject

/** Manages keyboard operations */
interface KeyboardManager {
    fun isKeyboardEnabled(): Boolean

    fun isKeyboardSelected(): Boolean

    fun getKeyboardSettingsIntent(): Intent

    fun showKeyboardSelector()

    /**
     * Gets currently selected input method from system settings
     * @return Package name of current input method or null if none selected
     */
    fun getDefaultInputMethod(): String?
}

class KeyboardManagerImpl
    @Inject
    constructor(
        private val packageName: String,
        private val inputMethodManager: InputMethodManager,
        private val contentResolver: ContentResolver,
    ) : KeyboardManager {
        override fun isKeyboardEnabled(): Boolean = inputMethodManager.enabledInputMethodList.map { it.id }.any { it.contains(packageName) }

        override fun isKeyboardSelected(): Boolean {
            val selectedId = getDefaultInputMethod()
            return selectedId?.contains(packageName) == true
        }

        override fun getKeyboardSettingsIntent(): Intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)

        override fun showKeyboardSelector() {
            inputMethodManager.showInputMethodPicker()
        }

        override fun getDefaultInputMethod(): String? = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    }
