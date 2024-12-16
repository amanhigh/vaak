package com.aman.vaak.models

/**
 * Represents the keyboard setup progress states
 */
enum class KeyboardSetupState {
    NEEDS_ENABLING,  // Keyboard needs to be enabled in system settings
    NEEDS_SELECTION, // Keyboard is enabled but not selected as current input method
    NEEDS_PERMISSIONS, // Keyboard setup complete but missing permissions
    SETUP_COMPLETE   // Keyboard is enabled, selected and has required permissions
}
