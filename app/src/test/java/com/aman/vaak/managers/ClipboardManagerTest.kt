package com.aman.vaak.managers

import com.aman.vaak.repositories.ClipboardRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class ClipboardManagerTest {
    @Mock
    private lateinit var repository: ClipboardRepository

    private lateinit var manager: ClipboardManager

    @Before
    fun setup() {
        manager = ClipboardManagerImpl(repository)
    }

    @Test
    fun `test pasteContent returns null when clipboard empty`() {
        // Given
        whenever(repository.getClipboardText()).thenReturn(null)

        // When
        val result = manager.pasteContent()

        // Then
        assertNull(result)
    }

    @Test
    fun `test pasteContent returns text from clipboard`() {
        // Given
        val expectedText = "Test Text"
        whenever(repository.getClipboardText()).thenReturn(expectedText)

        // When
        val result = manager.pasteContent()

        // Then
        assertEquals(expectedText, result)
    }
}
