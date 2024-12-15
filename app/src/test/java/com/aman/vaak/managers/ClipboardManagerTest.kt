package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import com.aman.vaak.repositories.ClipboardRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class ClipboardManagerTest {
    @Mock private lateinit var repository: ClipboardRepository

    @Mock private lateinit var inputConnection: InputConnection

    private lateinit var manager: ClipboardManager

    @Before
    fun setup() {
        manager = ClipboardManagerImpl(repository)
    }

    @Test
    fun `test pasteContent returns false when clipboard empty`() {
        // Given
        whenever(repository.getClipboardText()).thenReturn(null)

        // When
        val result = manager.pasteContent(inputConnection)

        // Then
        assertFalse(result)
    }

    @Test
    fun `test pasteContent commits text to input connection`() {
        // Given
        val expectedText = "Test Text"
        whenever(repository.getClipboardText()).thenReturn(expectedText)

        // When
        val result = manager.pasteContent(inputConnection)

        // Then
        assertTrue(result)
        verify(inputConnection).commitText(expectedText, 1)
    }
}
