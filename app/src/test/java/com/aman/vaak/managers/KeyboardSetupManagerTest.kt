package com.aman.vaak.managers

import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import com.aman.vaak.models.KeyboardSetupState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.jupiter.api.Assertions.*

@ExtendWith(MockitoExtension::class)
class KeyboardSetupManagerTest {
    @Mock private lateinit var inputMethodManager: InputMethodManager
    @Mock private lateinit var systemManager: SystemManager
    @Mock private lateinit var inputMethodInfo: InputMethodInfo

    private lateinit var manager: KeyboardSetupManager
    private val packageName = "com.aman.vaak"

    @BeforeEach
    fun setup() {
        manager = KeyboardSetupManagerImpl(
            packageName = packageName,
            inputMethodManager = inputMethodManager,
            systemManager = systemManager
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
    inner class WhenEnablingKeyboard {
        @Test
        fun `returns NEEDS_SELECTION when enabled but not selected`() {
            whenever(inputMethodInfo.id).thenReturn(packageName)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
            whenever(systemManager.getDefaultInputMethod()).thenReturn("other.keyboard")
            
            assertEquals(KeyboardSetupState.NEEDS_SELECTION, manager.getKeyboardSetupState())
        }
    }


    @Nested
    inner class WhenKeyboardEnabled {
        @Test
        fun `shows IME picker for keyboard selection`() {
            manager.showKeyboardSelector()
            verify(inputMethodManager).showInputMethodPicker()
        }
        
        @Test
        fun `proceeds to selection state when enabled`() {
            whenever(inputMethodInfo.id).thenReturn(packageName)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
            whenever(systemManager.getDefaultInputMethod()).thenReturn("other.keyboard")
            assertEquals(KeyboardSetupState.NEEDS_SELECTION, manager.getKeyboardSetupState())
        }
    }

    @Nested
    inner class WhenKeyboardSelected {
        @Test
        fun `proceeds to permissions state when selected but permissions not granted`() {
            whenever(inputMethodInfo.id).thenReturn(packageName)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
            whenever(systemManager.getDefaultInputMethod()).thenReturn(packageName)
            whenever(systemManager.hasRequiredPermissions()).thenReturn(false)
            assertEquals(KeyboardSetupState.NEEDS_PERMISSIONS, manager.getKeyboardSetupState())
        }
    }

    @Nested
    inner class WhenNeedingPermissions {
        @Test
        fun `stays in permissions state until granted`() {
            whenever(inputMethodInfo.id).thenReturn(packageName)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
            whenever(systemManager.getDefaultInputMethod()).thenReturn(packageName)
            whenever(systemManager.hasRequiredPermissions()).thenReturn(false)
            assertEquals(KeyboardSetupState.NEEDS_PERMISSIONS, manager.getKeyboardSetupState())
        }

        @Test
        fun `proceeds to complete when permissions granted`() {
            whenever(inputMethodInfo.id).thenReturn(packageName)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
            whenever(systemManager.getDefaultInputMethod()).thenReturn(packageName)
            whenever(systemManager.hasRequiredPermissions()).thenReturn(true)
            assertEquals(KeyboardSetupState.SETUP_COMPLETE, manager.getKeyboardSetupState())
        }
    }

    @Nested
    inner class WhenSetupComplete {
        @Test
        fun `confirms all requirements met for complete setup`() {
            whenever(inputMethodInfo.id).thenReturn(packageName)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
            whenever(systemManager.getDefaultInputMethod()).thenReturn(packageName)
            whenever(systemManager.hasRequiredPermissions()).thenReturn(true)
            assertEquals(KeyboardSetupState.SETUP_COMPLETE, manager.getKeyboardSetupState())
        }
    }
}