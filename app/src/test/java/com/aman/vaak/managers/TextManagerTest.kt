package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertThrows
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
        fun `operations throw InputNotConnectedException in initial state`() {
            assertAll(
                "All operations should throw InputNotConnectedException without connection",
                { assertThrows(InputNotConnectedException::class.java) { manager.insertSpace() } },
                { assertThrows(InputNotConnectedException::class.java) { manager.handleBackspace() } },
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
                { assertThrows(InputNotConnectedException::class.java) { manager.insertSpace() } },
                { assertThrows(InputNotConnectedException::class.java) { manager.handleBackspace() } },
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

        @Test
        fun `throws TextOperationFailedException for null input connection`() {
            assertThrows(TextOperationFailedException::class.java) { manager.attachInputConnection(null) }
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

                manager.insertSpace()
                verify(inputConnection).commitText(" ", 1)
            }

            @Test
            fun `throws TextOperationFailedException when commit fails`() {
                whenever(inputConnection.commitText(" ", 1)).thenReturn(false)
                assertThrows(TextOperationFailedException::class.java) { manager.insertSpace() }
            }
        }

        @Nested
        inner class BackspaceOperation {
            @Test
            fun `deletes previous character successfully`() {
                whenever(inputConnection.deleteSurroundingText(1, 0)).thenReturn(true)

                manager.handleBackspace()
                verify(inputConnection).deleteSurroundingText(1, 0)
            }

            @Test
            fun `throws TextOperationFailedException when delete fails`() {
                whenever(inputConnection.deleteSurroundingText(1, 0)).thenReturn(false)
                assertThrows(TextOperationFailedException::class.java) { manager.handleBackspace() }
            }
        }

        @Nested
        inner class NewLineOperation {
            @Test
            fun `commits newline character successfully`() {
                whenever(inputConnection.commitText("\n", 1)).thenReturn(true)

                manager.insertNewLine()
                verify(inputConnection).commitText("\n", 1)
            }

            @Test
            fun `throws TextOperationFailedException when commit fails`() {
                whenever(inputConnection.commitText("\n", 1)).thenReturn(false)
                assertThrows(TextOperationFailedException::class.java) { manager.insertNewLine() }
            }
        }

        @Nested
        inner class SelectAllOperation {
            @Test
            fun `performs select all successfully`() {
                whenever(inputConnection.performContextMenuAction(android.R.id.selectAll)).thenReturn(true)

                manager.selectAll()
                verify(inputConnection).performContextMenuAction(android.R.id.selectAll)
            }

            @Test
            fun `throws TextOperationFailedException when select all fails`() {
                whenever(inputConnection.performContextMenuAction(android.R.id.selectAll)).thenReturn(false)
                assertThrows(TextOperationFailedException::class.java) { manager.selectAll() }
            }
        }
    }
}
