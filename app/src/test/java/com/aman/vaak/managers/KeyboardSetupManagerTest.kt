package com.aman.vaak.managers

import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import com.aman.vaak.models.KeyboardSetupState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class KeyboardSetupManagerTest {
    @Mock private lateinit var inputMethodManager: InputMethodManager

    @Mock private lateinit var systemManager: SystemManager

    @Mock private lateinit var settingsManager: SettingsManager

    @Mock private lateinit var inputMethodInfo: InputMethodInfo

    private lateinit var manager: KeyboardSetupManager
    private val packageName = "com.aman.vaak"

    @BeforeEach
    fun setup() {
        manager =
            KeyboardSetupManagerImpl(
                packageName = packageName,
                inputMethodManager = inputMethodManager,
                systemManager = systemManager,
                settingsManager = settingsManager,
            )
    }

    @Nested
    inner class WhenFirstInstalled {
        @Test
        fun `returns NEEDS_ENABLING when keyboard not in IME list`() {
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(emptyList())
            assertEquals(KeyboardSetupState.NEEDS_ENABLING, manager.getKeyboardSetupState())
        }

        @Test
        fun `returns NEEDS_ENABLING when other keyboards in IME list`() {
            whenever(inputMethodInfo.id).thenReturn("other.keyboard")
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
            assertEquals(KeyboardSetupState.NEEDS_ENABLING, manager.getKeyboardSetupState())
        }
    }

    @Nested
    inner class WhenEnabled {
        @Test
        fun `shows IME picker when requested`() {
            manager.showKeyboardSelector()
            verify(inputMethodManager).showInputMethodPicker()
        }

        @Test
        fun `proceeds to permissions check when selected`() {
            whenever(inputMethodInfo.id).thenReturn(packageName)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
            whenever(systemManager.hasRequiredPermissions()).thenReturn(false)
            assertEquals(KeyboardSetupState.NEEDS_PERMISSIONS, manager.getKeyboardSetupState())
        }

        @Test
        fun `returns READY_FOR_USE when enabled but not selected`() {
            whenever(inputMethodInfo.id).thenReturn(packageName)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
            whenever(systemManager.getDefaultInputMethod()).thenReturn("other.keyboard")
            whenever(systemManager.hasRequiredPermissions()).thenReturn(true)
            whenever(settingsManager.getApiKey()).thenReturn("valid-key")
            assertEquals(KeyboardSetupState.READY_FOR_USE, manager.getKeyboardSetupState())
        }
    }

    @Nested
    inner class WhenPermissionsGranted {
        @Test
        fun `proceeds to API key check when permissions granted`() {
            whenever(inputMethodInfo.id).thenReturn(packageName)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
            whenever(systemManager.hasRequiredPermissions()).thenReturn(true)
            whenever(settingsManager.getApiKey()).thenReturn(null)
            assertEquals(KeyboardSetupState.NEEDS_API_KEY, manager.getKeyboardSetupState())
        }

        @Test
        fun `proceeds to API key check when empty API key`() {
            whenever(inputMethodInfo.id).thenReturn(packageName)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
            whenever(systemManager.hasRequiredPermissions()).thenReturn(true)
            whenever(settingsManager.getApiKey()).thenReturn("")
            assertEquals(KeyboardSetupState.NEEDS_API_KEY, manager.getKeyboardSetupState())
        }
    }

    @Nested
    inner class WhenApiKeyConfigured {
        @Test
        fun `returns READY_FOR_USE when not selected as default`() {
            whenever(inputMethodInfo.id).thenReturn(packageName)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
            whenever(systemManager.hasRequiredPermissions()).thenReturn(true)
            whenever(settingsManager.getApiKey()).thenReturn("valid-key")
            whenever(systemManager.getDefaultInputMethod()).thenReturn("other.keyboard")
            assertEquals(KeyboardSetupState.READY_FOR_USE, manager.getKeyboardSetupState())
        }

        @Test
        fun `returns SETUP_COMPLETE when selected as default`() {
            whenever(inputMethodInfo.id).thenReturn(packageName)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
            whenever(systemManager.hasRequiredPermissions()).thenReturn(true)
            whenever(settingsManager.getApiKey()).thenReturn("valid-key")
            whenever(systemManager.getDefaultInputMethod()).thenReturn(packageName)
            assertEquals(KeyboardSetupState.SETUP_COMPLETE, manager.getKeyboardSetupState())
        }
    }
}
