package com.aman.vaak.managers

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
    inner class WhenCheckingKeyboardEnabled {
        @Test
        fun `returns true when keyboard in IME list`() {
            whenever(inputMethodInfo.id).thenReturn(packageName)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))

            assertTrue(manager.isKeyboardEnabled())
        }

        @Test
        fun `returns false when keyboard not in IME list`() {
            whenever(inputMethodInfo.id).thenReturn("other.keyboard")
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))

            assertFalse(manager.isKeyboardEnabled())
        }

        @Test
        fun `returns false when IME list empty`() {
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(emptyList())

            assertFalse(manager.isKeyboardEnabled())
        }
    }

    @Nested
    inner class WhenCheckingKeyboardSelection {
        @Test
        fun `returns true when keyboard is default IME`() {
            whenever(systemManager.getDefaultInputMethod())
                .thenReturn("$packageName/ComponentName")

            assertTrue(manager.isKeyboardSelected())
        }

        @Test
        fun `returns false when different keyboard is default`() {
            whenever(systemManager.getDefaultInputMethod())
                .thenReturn("other.keyboard/ComponentName")

            assertFalse(manager.isKeyboardSelected())
        }

        @Test
        fun `returns false when no default IME set`() {
            whenever(systemManager.getDefaultInputMethod()).thenReturn(null)

            assertFalse(manager.isKeyboardSelected())
        }
    }

    @Nested
    inner class WhenCheckingSetupState {
        @Test
        fun `returns NEEDS_ENABLING when keyboard not enabled`() {
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(emptyList())

            assertEquals(KeyboardSetupState.NEEDS_ENABLING, manager.getKeyboardSetupState())
        }

        @Test
        fun `returns NEEDS_SELECTION when enabled but not selected`() {
            whenever(inputMethodInfo.id).thenReturn(packageName)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
            whenever(systemManager.getDefaultInputMethod())
                .thenReturn("other.keyboard/ComponentName")

            assertEquals(KeyboardSetupState.NEEDS_SELECTION, manager.getKeyboardSetupState())
        }

        @Test
        fun `returns SETUP_COMPLETE when enabled and selected`() {
            whenever(inputMethodInfo.id).thenReturn(packageName)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
            whenever(systemManager.getDefaultInputMethod())
                .thenReturn("$packageName/ComponentName")

            assertEquals(KeyboardSetupState.SETUP_COMPLETE, manager.getKeyboardSetupState())
        }
    }

    @Nested
    inner class WhenShowingKeyboardSelector {
        @Test
        fun `shows IME picker using input method manager`() {
            manager.showKeyboardSelector()
            verify(inputMethodManager).showInputMethodPicker()
        }
    }
}