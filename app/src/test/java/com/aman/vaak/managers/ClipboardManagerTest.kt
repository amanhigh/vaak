package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ClipboardManagerTest {
    @Mock private lateinit var inputConnection: InputConnection
    private lateinit var manager: ClipboardManagerImpl

    @BeforeEach
    fun setup() {
        manager = ClipboardManagerImpl()
    }

    @Nested
    inner class WhenCopying {
        @Test
        fun `calls performContextMenuAction with copy id`() {
            manager.attachInputConnection(inputConnection)
            whenever(inputConnection.performContextMenuAction(android.R.id.copy)).thenReturn(true)

            val result = manager.copySelectedText()

            assertTrue(result)
            verify(inputConnection).performContextMenuAction(android.R.id.copy)
        }

        @Test
        fun `returns false when copy fails`() {
            manager.attachInputConnection(inputConnection)
            whenever(inputConnection.performContextMenuAction(android.R.id.copy)).thenReturn(false)

            val result = manager.copySelectedText()

            assertFalse(result)
        }

        @Test
        fun `throws exception when no input connection`() {
            assertThrows(InputNotConnectedException::class.java) {
                manager.copySelectedText()
            }
        }
    }

    @Nested
    inner class WhenPasting {
        @Test
        fun `calls performContextMenuAction with paste id`() {
            manager.attachInputConnection(inputConnection)
            whenever(inputConnection.performContextMenuAction(android.R.id.paste)).thenReturn(true)

            val result = manager.pasteText()

            assertTrue(result)
            verify(inputConnection).performContextMenuAction(android.R.id.paste)
        }

        @Test
        fun `returns false when paste fails`() {
            manager.attachInputConnection(inputConnection)
            whenever(inputConnection.performContextMenuAction(android.R.id.paste)).thenReturn(false)

            val result = manager.pasteText()

            assertFalse(result)
        }

        @Test
        fun `throws exception when no input connection`() {
            assertThrows(InputNotConnectedException::class.java) {
                manager.pasteText()
            }
        }
    }
}
