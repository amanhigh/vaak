package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.jupiter.api.Assertions.assertAll
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
class TextManagerTest {
    // FIXME: Add tests for TextManagerImpl which are Pending
    @Mock
    private lateinit var inputConnection: InputConnection
    private lateinit var manager: TextManagerImpl
    private lateinit var testScope: CoroutineScope

    @BeforeEach
    fun setup() {
        testScope = CoroutineScope(StandardTestDispatcher())
        manager = TextManagerImpl(testScope)
    }

    @Nested
    inner class WhenInputNotConnected {
        @Test
        fun `operations throw InputNotConnectedException in initial state`() {
            assertAll(
                "All operations should throw InputNotConnectedException without connection",
                { assertThrows(InputNotConnectedException::class.java) { manager.insertText("test") } },
                { assertThrows(InputNotConnectedException::class.java) { manager.insertSpace() } },
                { assertThrows(InputNotConnectedException::class.java) { manager.insertNewLine() } },
                { assertThrows(InputNotConnectedException::class.java) { manager.selectAll() } },
            )
        }

        @Test
        fun `operations throw InputNotConnectedException after detaching input`() {
            manager.attachInputConnection(inputConnection)
            manager.detachInputConnection()

            assertAll(
                "All operations should throw InputNotConnectedException after detachment",
                { assertThrows(InputNotConnectedException::class.java) { manager.insertText("test") } },
                { assertThrows(InputNotConnectedException::class.java) { manager.insertSpace() } },
                { assertThrows(InputNotConnectedException::class.java) { manager.insertNewLine() } },
                { assertThrows(InputNotConnectedException::class.java) { manager.selectAll() } },
            )
        }
    }

    @Nested
    inner class WhenAttachingInput {
        @Test
        fun `attaches valid input connection successfully`() {
            manager.attachInputConnection(inputConnection)
        }
    }

    @Nested
    inner class WhenInputConnected {
        @BeforeEach
        fun setupConnection() {
            manager.attachInputConnection(inputConnection)
        }

        @Nested
        inner class TextOperations {
            @Test
            fun `inserts text successfully`() {
                whenever(inputConnection.commitText("test", 1)).thenReturn(true)

                manager.insertText("test")
                verify(inputConnection).commitText("test", 1)
            }

            @Test
            fun `throws TextOperationFailedException when text insert fails`() {
                whenever(inputConnection.commitText("test", 1)).thenReturn(false)
                assertThrows(TextOperationFailedException::class.java) { manager.insertText("test") }
            }

            @Test
            fun `commits space character successfully`() {
                whenever(inputConnection.commitText(" ", 1)).thenReturn(true)

                manager.insertSpace()
                verify(inputConnection).commitText(" ", 1)
            }

            @Test
            fun `commits newline character successfully`() {
                whenever(inputConnection.commitText("\n", 1)).thenReturn(true)

                manager.insertNewLine()
                verify(inputConnection).commitText("\n", 1)
            }

            @Test
            fun `performs select all successfully`() {
                whenever(inputConnection.performContextMenuAction(android.R.id.selectAll)).thenReturn(true)

                manager.selectAll()
                verify(inputConnection).performContextMenuAction(android.R.id.selectAll)
            }

            @Test
            fun `deletes character successfully`() {
                whenever(inputConnection.deleteSurroundingText(1, 0)).thenReturn(true)

                assertTrue(manager.deleteCharacter())
                verify(inputConnection).deleteSurroundingText(1, 0)
            }

            @Test
            fun `returns false when delete character fails`() {
                whenever(inputConnection.deleteSurroundingText(1, 0)).thenReturn(false)
                assertFalse(manager.deleteCharacter())
            }

            @Test
            fun `deletes selection successfully`() {
                whenever(inputConnection.getSelectedText(0)).thenReturn("selected")
                whenever(inputConnection.commitText("", 1)).thenReturn(true)

                assertTrue(manager.deleteSelection())
                verify(inputConnection).commitText("", 1)
            }

            @Test
            fun `returns false when no text is selected`() {
                whenever(inputConnection.getSelectedText(0)).thenReturn(null)
                assertFalse(manager.deleteSelection())
            }
        }
    }
}
