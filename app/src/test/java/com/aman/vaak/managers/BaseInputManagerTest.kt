package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class BaseInputManagerTest {
    @Mock
    private lateinit var mockInputConnection: InputConnection

    @Mock
    private lateinit var anotherInputConnection: InputConnection

    private lateinit var baseInputManager: BaseInputManager

    @BeforeEach
    fun setup() {
        baseInputManager = BaseInputManagerImpl()
    }

    @Nested
    @DisplayName("Input Connection Management")
    inner class InputConnectionManagement {
        @Test
        fun `attachInputConnection stores provided connection`() {
            baseInputManager.attachInputConnection(mockInputConnection)

            val result = baseInputManager.requireInputConnection()

            assertEquals(mockInputConnection, result)
        }

        @Test
        fun `attachInputConnection handles null connection`() {
            baseInputManager.attachInputConnection(null)

            assertThrows<InputNotConnectedException> {
                baseInputManager.requireInputConnection()
            }
        }

        @Test
        fun `attachInputConnection replaces existing connection`() {
            // First attach one connection
            baseInputManager.attachInputConnection(mockInputConnection)

            // Then attach another connection
            baseInputManager.attachInputConnection(anotherInputConnection)

            val result = baseInputManager.requireInputConnection()

            assertEquals(anotherInputConnection, result)
        }

        @Test
        fun `detachInputConnection removes current connection`() {
            // First attach a connection
            baseInputManager.attachInputConnection(mockInputConnection)

            // Then detach it
            baseInputManager.detachInputConnection()

            assertThrows<InputNotConnectedException> {
                baseInputManager.requireInputConnection()
            }
        }

        @Test
        fun `detachInputConnection handles no existing connection gracefully`() {
            // Detach when no connection exists - should not throw
            baseInputManager.detachInputConnection()

            assertThrows<InputNotConnectedException> {
                baseInputManager.requireInputConnection()
            }
        }
    }

    @Nested
    @DisplayName("Require Input Connection")
    inner class RequireInputConnection {
        @Test
        fun `requireInputConnection returns attached connection`() {
            baseInputManager.attachInputConnection(mockInputConnection)

            val result = baseInputManager.requireInputConnection()

            assertEquals(mockInputConnection, result)
        }

        @Test
        fun `requireInputConnection throws when no connection attached`() {
            val exception =
                assertThrows<InputNotConnectedException> {
                    baseInputManager.requireInputConnection()
                }

            assertEquals("No input connection available", exception.message)
        }

        @Test
        fun `requireInputConnection throws after detaching connection`() {
            baseInputManager.attachInputConnection(mockInputConnection)
            baseInputManager.detachInputConnection()

            val exception =
                assertThrows<InputNotConnectedException> {
                    baseInputManager.requireInputConnection()
                }

            assertEquals("No input connection available", exception.message)
        }
    }

    @Nested
    @DisplayName("Exception Handling")
    inner class ExceptionHandling {
        @Test
        fun `InputNotConnectedException is InputOperationException`() {
            val exception = InputNotConnectedException()

            assert(exception is InputOperationException)
            assertEquals("No input connection available", exception.message)
        }

        @Test
        fun `InputOperationException can be created with custom message`() {
            // Test that InputNotConnectedException extends InputOperationException properly
            val exception = InputNotConnectedException()

            assertTrue(exception is InputOperationException)
            assertTrue(exception.message?.contains("input connection") == true)
        }
    }

    @Nested
    @DisplayName("State Management")
    inner class StateManagement {
        @Test
        fun `multiple attach operations maintain latest connection`() {
            baseInputManager.attachInputConnection(mockInputConnection)
            baseInputManager.attachInputConnection(anotherInputConnection)
            baseInputManager.attachInputConnection(mockInputConnection)

            val result = baseInputManager.requireInputConnection()

            assertEquals(mockInputConnection, result)
        }

        @Test
        fun `attach null after valid connection removes connection`() {
            baseInputManager.attachInputConnection(mockInputConnection)
            baseInputManager.attachInputConnection(null)

            assertThrows<InputNotConnectedException> {
                baseInputManager.requireInputConnection()
            }
        }

        @Test
        fun `detach followed by attach works correctly`() {
            baseInputManager.attachInputConnection(mockInputConnection)
            baseInputManager.detachInputConnection()
            baseInputManager.attachInputConnection(anotherInputConnection)

            val result = baseInputManager.requireInputConnection()

            assertEquals(anotherInputConnection, result)
        }
    }
}
