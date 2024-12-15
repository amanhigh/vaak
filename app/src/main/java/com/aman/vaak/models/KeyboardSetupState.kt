package com.aman.vaak.models

/**
 * Represents the keyboard setup progress states
 */
enum class KeyboardSetupState {
    NEEDS_ENABLING,  // Keyboard needs to be enabled in system settings
    NEEDS_SELECTION, // Keyboard is enabled but not selected as current input method
    SETUP_COMPLETE   // Keyboard is enabled and selected as current input method
}
