package com.aman.vaak.handlers

import android.view.View
import com.aman.vaak.R
import com.aman.vaak.managers.TextManager
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class NumpadHandlerTest {
    @Mock
    private lateinit var textManager: TextManager

    @Mock
    private lateinit var view: View

    private lateinit var handler: NumpadHandlerImpl

    @BeforeEach
    fun setup() {
        handler = NumpadHandlerImpl(textManager)
    }

    @Nested
    inner class ViewLifecycleTests {
        @Test
        fun `onViewAttached queries button views`() {
            handler.attachView(view)

            verify(view).findViewById<View>(R.id.num0Button)
        }
    }

    @Nested
    inner class NumpadVisibilityTests {
        @Test
        fun `isNumpadVisible returns false initially`() {
            assertFalse(handler.isNumpadVisible())
        }

        @Test
        fun `showNumpad updates state to true`() {
            handler.attachView(view)

            handler.showNumpad()

            assertTrue(handler.isNumpadVisible())
        }

        @Test
        fun `hideNumpad updates state to false`() {
            handler.attachView(view)
            handler.showNumpad()

            handler.hideNumpad()

            assertFalse(handler.isNumpadVisible())
        }

        @Test
        fun `isNumpadVisible tracks state correctly after show and hide`() {
            handler.attachView(view)

            assertFalse(handler.isNumpadVisible())

            handler.showNumpad()
            assertTrue(handler.isNumpadVisible())

            handler.hideNumpad()
            assertFalse(handler.isNumpadVisible())
        }
    }
}
