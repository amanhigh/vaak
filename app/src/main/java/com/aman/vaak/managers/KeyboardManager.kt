package com.aman.vaak.managers

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
}

class KeyboardManagerImpl
    @Inject
    constructor(
        private val packageName: String,
        private val inputMethodManager: InputMethodManager,
        private val systemManager: SystemManager,
    ) : KeyboardManager {
        override fun isKeyboardEnabled(): Boolean = inputMethodManager.enabledInputMethodList.map { it.id }.any { it.contains(packageName) }

        override fun isKeyboardSelected(): Boolean {
            val selectedId = systemManager.getDefaultInputMethod()
            return selectedId?.contains(packageName) == true
        }

        override fun getKeyboardSettingsIntent(): Intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)

        override fun showKeyboardSelector() {
            inputMethodManager.showInputMethodPicker()
        }
    }
