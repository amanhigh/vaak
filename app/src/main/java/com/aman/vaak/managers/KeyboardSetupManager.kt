package com.aman.vaak.managers

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import com.aman.vaak.models.KeyboardSetupState
import javax.inject.Inject

/**
 * Manages keyboard setup state and operations
 */
interface KeyboardSetupManager {
    /**
     * Checks if keyboard is enabled in system IME settings
     */
    fun isKeyboardEnabled(): Boolean

    /**
     * Checks if keyboard is selected as current input method
     */
    fun isKeyboardSelected(): Boolean

    /**
     * Gets Intent to open system IME settings
     */
    fun getKeyboardSettingsIntent(): Intent

    /**
     * Shows IME picker dialog
     */
    fun showKeyboardSelector()

    /**
     * Gets current keyboard setup state
     */
    fun getKeyboardSetupState(): KeyboardSetupState
}

class KeyboardSetupManagerImpl @Inject constructor(
    private val packageName: String,
    private val inputMethodManager: InputMethodManager,
    private val systemManager: SystemManager
) : KeyboardSetupManager {

    override fun isKeyboardEnabled(): Boolean =
        inputMethodManager.enabledInputMethodList
            .map { it.id }
            .any { it.contains(packageName) }

    override fun isKeyboardSelected(): Boolean {
        val selectedId = systemManager.getDefaultInputMethod()
        return selectedId?.contains(packageName) == true
    }

    override fun getKeyboardSettingsIntent(): Intent =
        Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)

    override fun showKeyboardSelector() {
        inputMethodManager.showInputMethodPicker()
    }

    override fun getKeyboardSetupState(): KeyboardSetupState = when {
        !isKeyboardEnabled() -> KeyboardSetupState.NEEDS_ENABLING
        !isKeyboardSelected() -> KeyboardSetupState.NEEDS_SELECTION
        else -> KeyboardSetupState.SETUP_COMPLETE
    }
}