package com.aman.vaak.repositories

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
class ClipboardRepositoryTest {
    @Mock
    private lateinit var clipboardManager: ClipboardManager

    private lateinit var repository: ClipboardRepository

    @Before
    fun setup() {
        repository = ClipboardRepositoryImpl(clipboardManager)
    }

    @Test
    fun `test getClipboardText returns null when clipboard empty`() {
        // Given
        whenever(clipboardManager.primaryClip).thenReturn(null)

        // When
        val result = repository.getClipboardText()

        // Then
        assertNull(result)
    }

    @Test
    fun `test getClipboardText returns text from clipboard`() {
        // Given
        val clipData = mock<ClipData>()
        val clipItem = mock<ClipData.Item>()
        val expectedText = "Test Text"

        whenever(clipboardManager.primaryClip).thenReturn(clipData)
        whenever(clipData.getItemAt(0)).thenReturn(clipItem)
        whenever(clipItem.text).thenReturn(expectedText)

        // When
        val result = repository.getClipboardText()

        // Then
        assertEquals(expectedText, result)
    }
}
