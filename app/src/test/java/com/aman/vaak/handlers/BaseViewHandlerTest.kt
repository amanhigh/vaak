package com.aman.vaak.handlers

import android.view.View
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class BaseViewHandlerTest {
    @Mock
    private lateinit var mockView: View

    private lateinit var handler: TestViewHandler

    @BeforeEach
    fun setup() {
        handler = TestViewHandler()
    }

    @Nested
    inner class ViewAttachmentTests {
        @Test
        fun `attachView sets current view and calls onViewAttached`() {
            handler.attachView(mockView)

            assertTrue(handler.isViewAttached())
            assertEquals(mockView, handler.exposedCurrentView)
            assertTrue(handler.onViewAttachedCalled)
        }

        @Test
        fun `detachView clears current view and calls onViewDetached`() {
            handler.attachView(mockView)
            handler.detachView()

            assertFalse(handler.isViewAttached())
            assertEquals(null, handler.exposedCurrentView)
            assertTrue(handler.onViewDetachedCalled)
        }

        @Test
        fun `isViewAttached returns false initially`() {
            assertFalse(handler.isViewAttached())
        }

        @Test
        fun `isViewAttached returns true after attach`() {
            handler.attachView(mockView)
            assertTrue(handler.isViewAttached())
        }

        @Test
        fun `isViewAttached returns false after detach`() {
            handler.attachView(mockView)
            handler.detachView()
            assertFalse(handler.isViewAttached())
        }
    }

    @Nested
    inner class RequireViewTests {
        @Test
        fun `requireView returns view when found`() {
            val childView = View(null)
            handler.attachView(mockView)

            val result = handler.testRequireView(childView)

            assertNotNull(result)
            assertEquals(childView, result)
        }

        @Test
        fun `requireView throws ViewNotFoundException when view not attached`() {
            val exception =
                assertThrows(ViewNotFoundException::class.java) {
                    handler.testRequireViewById(android.R.id.content)
                }

            assertTrue(exception.message?.contains("not found") == true)
        }
    }

    @Nested
    inner class WithViewTests {
        @Test
        fun `withView executes action when view attached`() {
            var actionExecuted = false
            handler.attachView(mockView)

            val result =
                handler.testWithView {
                    actionExecuted = true
                }

            assertTrue(result)
            assertTrue(actionExecuted)
        }

        @Test
        fun `withView returns false when view not attached`() {
            var actionExecuted = false

            val result =
                handler.testWithView {
                    actionExecuted = true
                }

            assertFalse(result)
            assertFalse(actionExecuted)
        }

        @Test
        fun `withView provides correct view to action`() {
            var providedView: View? = null
            handler.attachView(mockView)

            handler.testWithView { view ->
                providedView = view
            }

            assertEquals(mockView, providedView)
        }
    }

    @Nested
    inner class ErrorHandlingTests {
        @Test
        fun `handleError is called with exception`() {
            val testException = RuntimeException("Test error")

            handler.triggerError(testException)

            assertTrue(handler.handleErrorCalled)
            assertEquals(testException, handler.lastError)
        }
    }

    @Nested
    inner class LifecycleTests {
        @Test
        fun `multiple attach calls update view correctly`() {
            val view1 = View(null)
            val view2 = View(null)

            handler.attachView(view1)
            assertEquals(view1, handler.exposedCurrentView)

            handler.attachView(view2)
            assertEquals(view2, handler.exposedCurrentView)
        }

        @Test
        fun `detach without attach does not throw`() {
            handler.detachView()
            assertFalse(handler.isViewAttached())
        }

        @Test
        fun `onViewAttached called every time view attached`() {
            handler.attachView(mockView)
            assertTrue(handler.onViewAttachedCalled)

            handler.onViewAttachedCalled = false
            handler.attachView(mockView)
            assertTrue(handler.onViewAttachedCalled)
        }

        @Test
        fun `onViewDetached called every time view detached`() {
            handler.attachView(mockView)
            handler.detachView()
            assertTrue(handler.onViewDetachedCalled)

            handler.onViewDetachedCalled = false
            handler.detachView()
            assertTrue(handler.onViewDetachedCalled)
        }
    }

    private class TestViewHandler : BaseViewHandlerImpl() {
        var onViewAttachedCalled = false
        var onViewDetachedCalled = false
        var handleErrorCalled = false
        var lastError: Exception? = null

        val exposedCurrentView: View?
            get() = currentView

        override fun onViewAttached(view: View) {
            onViewAttachedCalled = true
        }

        override fun onViewDetached() {
            onViewDetachedCalled = true
        }

        override fun handleError(error: Exception) {
            handleErrorCalled = true
            lastError = error
        }

        fun testRequireView(view: View): View {
            currentView = View(null)
            return view
        }

        fun testRequireViewById(id: Int): View {
            return requireView(id)
        }

        fun testWithView(action: (View) -> Unit): Boolean {
            return withView(action)
        }

        fun triggerError(error: Exception) {
            handleError(error)
        }
    }
}
