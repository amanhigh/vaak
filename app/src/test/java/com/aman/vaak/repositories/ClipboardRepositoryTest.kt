package com.aman.vaak.repositories

import android.content.ClipData
import android.content.ClipboardManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ClipboardRepositoryTest {
    @Mock
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var repository: ClipboardRepository

    @BeforeEach
    fun setup() {
        repository = ClipboardRepositoryImpl(clipboardManager)
    }

    @Nested
    inner class WhenClipboardEmpty {
        @BeforeEach
        fun setupEmptyClipboard() {
            whenever(clipboardManager.primaryClip).thenReturn(null)
        }

        @Test
        fun `returns null from empty clipboard`() {
            val result = repository.getClipboardText()
            assertNull(result)
        }
    }

    @Nested
    inner class WhenClipboardHasContent {
        private val clipData = mock<ClipData>()
        private val clipItem = mock<ClipData.Item>()
        private val expectedText = "Test Text"

        @BeforeEach
        fun setupClipboardContent() {
            whenever(clipboardManager.primaryClip).thenReturn(clipData)
            whenever(clipData.getItemAt(0)).thenReturn(clipItem)
            whenever(clipItem.text).thenReturn(expectedText)
        }

        @Test
        fun `returns text from clipboard successfully`() {
            val result = repository.getClipboardText()
            assertEquals(expectedText, result)
        }
    }
}
