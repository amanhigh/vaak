package com.aman.vaak.managers

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import com.aman.vaak.models.KeyboardSetupState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class KeyboardSetupManagerTest {
    @Mock private lateinit var inputMethodManager: InputMethodManager
    @Mock private lateinit var systemManager: SystemManager
    @Mock private lateinit var inputMethodInfo: InputMethodInfo

    private lateinit var manager: KeyboardSetupManager
    private val packageName = "com.aman.vaak"

    @Before
    fun setup() {
        manager = KeyboardSetupManagerImpl(
            packageName = packageName,
            inputMethodManager = inputMethodManager,
            systemManager = systemManager
        )
    }

    @Test
    fun `isKeyboardEnabled returns true when keyboard in IME list`() {
        // Given
        whenever(inputMethodInfo.id).thenReturn(packageName)
        whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))

        // When
        val result = manager.isKeyboardEnabled()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isKeyboardEnabled returns false when keyboard not in IME list`() {
        // Given
        whenever(inputMethodInfo.id).thenReturn("other.keyboard")
        whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))

        // When
        val result = manager.isKeyboardEnabled()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isKeyboardEnabled returns false when IME list empty`() {
        // Given
        whenever(inputMethodManager.enabledInputMethodList).thenReturn(emptyList())

        // When
        val result = manager.isKeyboardEnabled()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isKeyboardSelected returns true when keyboard is default IME`() {
        // Given
        whenever(systemManager.getDefaultInputMethod())
            .thenReturn("$packageName/ComponentName")

        // When
        val result = manager.isKeyboardSelected()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isKeyboardSelected returns false when different keyboard is default`() {
        // Given
        whenever(systemManager.getDefaultInputMethod())
            .thenReturn("other.keyboard/ComponentName")

        // When
        val result = manager.isKeyboardSelected()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isKeyboardSelected returns false when no default IME set`() {
        // Given
        whenever(systemManager.getDefaultInputMethod())
            .thenReturn(null)

        // When
        val result = manager.isKeyboardSelected()

        // Then
        assertFalse(result)
    }

    @Test
    fun `getKeyboardSetupState returns NEEDS_ENABLING when keyboard not enabled`() {
        // Given
        whenever(inputMethodManager.enabledInputMethodList).thenReturn(emptyList())

        // When
        val result = manager.getKeyboardSetupState()

        // Then
        assertEquals(KeyboardSetupState.NEEDS_ENABLING, result)
    }

    @Test
    fun `getKeyboardSetupState returns NEEDS_SELECTION when enabled but not selected`() {
        // Given
        whenever(inputMethodInfo.id).thenReturn(packageName)
        whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
        whenever(systemManager.getDefaultInputMethod())
            .thenReturn("other.keyboard/ComponentName")

        // When
        val result = manager.getKeyboardSetupState()

        // Then
        assertEquals(KeyboardSetupState.NEEDS_SELECTION, result)
    }

    @Test
    fun `getKeyboardSetupState returns SETUP_COMPLETE when enabled and selected`() {
        // Given
        whenever(inputMethodInfo.id).thenReturn(packageName)
        whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo))
        whenever(systemManager.getDefaultInputMethod())
            .thenReturn("$packageName/ComponentName")

        // When
        val result = manager.getKeyboardSetupState()

        // Then
        assertEquals(KeyboardSetupState.SETUP_COMPLETE, result)
    }

    @Test
    fun `showKeyboardSelector shows IME picker`() {
        // When
        manager.showKeyboardSelector()

        // Then
        verify(inputMethodManager).showInputMethodPicker()
    }
}
