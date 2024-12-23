package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import com.aman.vaak.repositories.ClipboardRepository
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ClipboardManagerTest {
    @Mock private lateinit var repository: ClipboardRepository

    @Mock private lateinit var inputConnection: InputConnection
    private lateinit var manager: ClipboardManager

    @BeforeEach
    fun setup() {
        manager = ClipboardManagerImpl(repository)
    }

    @Nested
    inner class WhenPastingContent {
        @Test
        fun `returns true and commits text when repository has content`() {
            val expectedText = "Test Text"
            whenever(repository.getClipboardText()).thenReturn(expectedText)

            val result = manager.pasteContent(inputConnection)

            assertTrue(result)
            verify(inputConnection).commitText(expectedText, 1)
        }

        @Test
        fun `returns false and avoids interaction when repository returns no text`() {
            whenever(repository.getClipboardText()).thenReturn(null)

            val result = manager.pasteContent(inputConnection)

            assertFalse(result)
            verifyNoInteractions(inputConnection)
        }
    }
}
