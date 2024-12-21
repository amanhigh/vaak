package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertFalse
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
class TextManagerTest {
    @Mock
    private lateinit var inputConnection: InputConnection
    private lateinit var manager: TextManagerImpl

    @BeforeEach
    fun setup() {
        manager = TextManagerImpl()
    }

    @Nested
    inner class WhenInputNotConnected {
        @Test
        fun `operations return false in initial state`() {
            assertAll(
                "All operations should fail without connection",
                { assertFalse(manager.insertSpace()) },
                { assertFalse(manager.handleBackspace()) },
                { assertFalse(manager.insertNewLine()) },
                { assertFalse(manager.selectAll()) },
            )
        }

        @Test
        fun `operations return false after detaching input`() {
            manager.attachInputConnection(inputConnection)
            manager.detachInputConnection()

            assertAll(
                "All operations should fail after detachment",
                { assertFalse(manager.insertSpace()) },
                { assertFalse(manager.handleBackspace()) },
                { assertFalse(manager.insertNewLine()) },
                { assertFalse(manager.selectAll()) },
            )
        }
    }

    @Nested
    inner class WhenAttachingInput {
        @Test
        fun `returns true for valid input connection`() {
            assertTrue(manager.attachInputConnection(inputConnection))
        }

        @Test
        fun `returns false for null input connection`() {
            assertFalse(manager.attachInputConnection(null))
        }
    }

    @Nested
    inner class WhenInputConnected {
        @BeforeEach
        fun setupConnection() {
            manager.attachInputConnection(inputConnection)
        }

        @Nested
        inner class SpaceOperation {
            @Test
            fun `commits space character successfully`() {
                whenever(inputConnection.commitText(" ", 1)).thenReturn(true)

                assertTrue(manager.insertSpace())
                verify(inputConnection).commitText(" ", 1)
            }

            @Test
            fun `returns false when commit fails`() {
                whenever(inputConnection.commitText(" ", 1)).thenReturn(false)
                assertFalse(manager.insertSpace())
            }
        }

        @Nested
        inner class BackspaceOperation {
            @Test
            fun `deletes previous character successfully`() {
                whenever(inputConnection.deleteSurroundingText(1, 0)).thenReturn(true)

                assertTrue(manager.handleBackspace())
                verify(inputConnection).deleteSurroundingText(1, 0)
            }

            @Test
            fun `returns false when delete fails`() {
                whenever(inputConnection.deleteSurroundingText(1, 0)).thenReturn(false)
                assertFalse(manager.handleBackspace())
            }
        }

        @Nested
        inner class NewLineOperation {
            @Test
            fun `commits newline character successfully`() {
                whenever(inputConnection.commitText("\n", 1)).thenReturn(true)

                assertTrue(manager.insertNewLine())
                verify(inputConnection).commitText("\n", 1)
            }

            @Test
            fun `returns false when commit fails`() {
                whenever(inputConnection.commitText("\n", 1)).thenReturn(false)
                assertFalse(manager.insertNewLine())
            }
        }

        @Nested
        inner class SelectAllOperation {
            @Test
            fun `performs select all successfully`() {
                whenever(inputConnection.performContextMenuAction(android.R.id.selectAll)).thenReturn(true)

                assertTrue(manager.selectAll())
                verify(inputConnection).performContextMenuAction(android.R.id.selectAll)
            }

            @Test
            fun `returns false when select all fails`() {
                whenever(inputConnection.performContextMenuAction(android.R.id.selectAll)).thenReturn(false)
                assertFalse(manager.selectAll())
            }
        }
    }
}
