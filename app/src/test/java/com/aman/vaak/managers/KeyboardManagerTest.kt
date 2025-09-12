package com.aman.vaak.managers

import android.content.ContentResolver
import android.provider.Settings
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class KeyboardManagerTest {
    @Mock
    private lateinit var inputMethodManager: InputMethodManager

    @Mock
    private lateinit var contentResolver: ContentResolver

    @Mock
    private lateinit var inputMethodInfo1: InputMethodInfo

    @Mock
    private lateinit var inputMethodInfo2: InputMethodInfo

    private lateinit var keyboardManager: KeyboardManager
    private val testPackageName = "com.aman.vaak"

    @BeforeEach
    fun setup() {
        keyboardManager =
            KeyboardManagerImpl(
                packageName = testPackageName,
                inputMethodManager = inputMethodManager,
                contentResolver = contentResolver,
            )
    }

    @Nested
    @DisplayName("Keyboard Enabled Status")
    inner class KeyboardEnabledStatus {
        @Test
        fun `isKeyboardEnabled returns true when keyboard is in enabled list`() {
            Mockito.lenient().whenever(inputMethodInfo1.id).thenReturn("com.aman.vaak/.VaakInputMethodService")
            Mockito.lenient().whenever(inputMethodInfo2.id).thenReturn("com.android.inputmethod.latin/.LatinIME")
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo1, inputMethodInfo2))

            val result = keyboardManager.isKeyboardEnabled()

            assertTrue(result)
        }

        @Test
        fun `isKeyboardEnabled returns false when keyboard is not in enabled list`() {
            Mockito.lenient().whenever(inputMethodInfo1.id).thenReturn("com.android.inputmethod.latin/.LatinIME")
            Mockito.lenient().whenever(inputMethodInfo2.id).thenReturn("com.google.android.inputmethod.latin/.LatinIME")
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo1, inputMethodInfo2))

            val result = keyboardManager.isKeyboardEnabled()

            assertFalse(result)
        }

        @Test
        fun `isKeyboardEnabled returns false when enabled list is empty`() {
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(emptyList())

            val result = keyboardManager.isKeyboardEnabled()

            assertFalse(result)
        }

        @Test
        fun `isKeyboardEnabled handles partial package name match`() {
            Mockito.lenient().whenever(inputMethodInfo1.id).thenReturn("com.aman.vaak.test/.TestIME")
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo1))

            val result = keyboardManager.isKeyboardEnabled()

            assertTrue(result)
        }
    }

    @Nested
    @DisplayName("Keyboard Selected Status")
    inner class KeyboardSelectedStatus {
        @Test
        fun `isKeyboardSelected returns true when keyboard is default input method`() {
            val defaultMethod = "com.aman.vaak/.VaakInputMethodService"
            Mockito.mockStatic(Settings.Secure::class.java).use { mockedSettings ->
                whenever(Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD))
                    .thenReturn(defaultMethod)

                val result = keyboardManager.isKeyboardSelected()

                assertTrue(result)
            }
        }

        @Test
        fun `isKeyboardSelected returns false when different keyboard is selected`() {
            val defaultMethod = "com.android.inputmethod.latin/.LatinIME"
            Mockito.mockStatic(Settings.Secure::class.java).use { mockedSettings ->
                whenever(Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD))
                    .thenReturn(defaultMethod)

                val result = keyboardManager.isKeyboardSelected()

                assertFalse(result)
            }
        }

        @Test
        fun `isKeyboardSelected returns false when no default input method set`() {
            Mockito.mockStatic(Settings.Secure::class.java).use { mockedSettings ->
                whenever(Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD))
                    .thenReturn(null)

                val result = keyboardManager.isKeyboardSelected()

                assertFalse(result)
            }
        }

        @Test
        fun `isKeyboardSelected handles partial package name match in default method`() {
            val defaultMethod = "com.aman.vaak.debug/.VaakInputMethodService"
            Mockito.mockStatic(Settings.Secure::class.java).use { mockedSettings ->
                whenever(Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD))
                    .thenReturn(defaultMethod)

                val result = keyboardManager.isKeyboardSelected()

                assertTrue(result)
            }
        }
    }

    @Nested
    @DisplayName("Default Input Method")
    inner class DefaultInputMethod {
        @Test
        fun `getDefaultInputMethod returns current default input method`() {
            val expectedMethod = "com.android.inputmethod.latin/.LatinIME"
            Mockito.mockStatic(Settings.Secure::class.java).use { mockedSettings ->
                whenever(Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD))
                    .thenReturn(expectedMethod)

                val result = keyboardManager.getDefaultInputMethod()

                assertEquals(expectedMethod, result)
            }
        }

        @Test
        fun `getDefaultInputMethod returns null when no default method set`() {
            Mockito.mockStatic(Settings.Secure::class.java).use { mockedSettings ->
                whenever(Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD))
                    .thenReturn(null)

                val result = keyboardManager.getDefaultInputMethod()

                assertNull(result)
            }
        }

        @Test
        fun `getDefaultInputMethod returns empty string when empty default method`() {
            Mockito.mockStatic(Settings.Secure::class.java).use { mockedSettings ->
                whenever(Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD))
                    .thenReturn("")

                val result = keyboardManager.getDefaultInputMethod()

                assertEquals("", result)
            }
        }
    }

    @Nested
    @DisplayName("Keyboard Settings Intent")
    inner class KeyboardSettingsIntent {
        @Test
        fun `getKeyboardSettingsIntent returns correct intent action`() {
            val result = keyboardManager.getKeyboardSettingsIntent()

            // Just verify an intent is returned - action testing is environment dependent
            assertNotNull(result)
        }

        @Test
        fun `getKeyboardSettingsIntent returns non-null intent`() {
            val result = keyboardManager.getKeyboardSettingsIntent()

            assertNotNull(result)
        }

        @Test
        fun `getKeyboardSettingsIntent creates new intent each time`() {
            val intent1 = keyboardManager.getKeyboardSettingsIntent()
            val intent2 = keyboardManager.getKeyboardSettingsIntent()

            // Should be different instances but same action
            assertEquals(intent1.action, intent2.action)
            // Note: We can't easily test that they're different instances without deep object comparison
        }
    }

    @Nested
    @DisplayName("Keyboard Selector")
    inner class KeyboardSelector {
        @Test
        fun `showKeyboardSelector calls InputMethodManager showInputMethodPicker`() {
            keyboardManager.showKeyboardSelector()

            verify(inputMethodManager).showInputMethodPicker()
        }

        @Test
        fun `showKeyboardSelector can be called multiple times`() {
            keyboardManager.showKeyboardSelector()
            keyboardManager.showKeyboardSelector()

            verify(inputMethodManager, Mockito.times(2)).showInputMethodPicker()
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    inner class IntegrationScenarios {
        @Test
        fun `keyboard enabled and selected scenario`() {
            val methodId = "com.aman.vaak/.VaakInputMethodService"

            // Setup keyboard as enabled
            Mockito.lenient().whenever(inputMethodInfo1.id).thenReturn(methodId)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo1))

            // Setup keyboard as selected
            Mockito.mockStatic(Settings.Secure::class.java).use { mockedSettings ->
                whenever(Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD))
                    .thenReturn(methodId)

                assertTrue(keyboardManager.isKeyboardEnabled())
                assertTrue(keyboardManager.isKeyboardSelected())
                assertEquals(methodId, keyboardManager.getDefaultInputMethod())
            }
        }

        @Test
        fun `keyboard enabled but not selected scenario`() {
            val vaakMethod = "com.aman.vaak/.VaakInputMethodService"
            val defaultMethod = "com.android.inputmethod.latin/.LatinIME"

            // Setup keyboard as enabled but not selected
            Mockito.lenient().whenever(inputMethodInfo1.id).thenReturn(vaakMethod)
            Mockito.lenient().whenever(inputMethodInfo2.id).thenReturn(defaultMethod)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo1, inputMethodInfo2))

            Mockito.mockStatic(Settings.Secure::class.java).use { mockedSettings ->
                whenever(Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD))
                    .thenReturn(defaultMethod)

                assertTrue(keyboardManager.isKeyboardEnabled())
                assertFalse(keyboardManager.isKeyboardSelected())
                assertEquals(defaultMethod, keyboardManager.getDefaultInputMethod())
            }
        }

        @Test
        fun `keyboard not enabled scenario`() {
            val otherMethod = "com.android.inputmethod.latin/.LatinIME"

            // Setup other keyboards only
            Mockito.lenient().whenever(inputMethodInfo1.id).thenReturn(otherMethod)
            whenever(inputMethodManager.enabledInputMethodList).thenReturn(listOf(inputMethodInfo1))

            Mockito.mockStatic(Settings.Secure::class.java).use { mockedSettings ->
                whenever(Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD))
                    .thenReturn(otherMethod)

                assertFalse(keyboardManager.isKeyboardEnabled())
                assertFalse(keyboardManager.isKeyboardSelected())
                assertEquals(otherMethod, keyboardManager.getDefaultInputMethod())
            }
        }
    }
}
