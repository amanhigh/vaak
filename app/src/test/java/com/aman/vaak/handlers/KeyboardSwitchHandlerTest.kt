package com.aman.vaak.handlers

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import com.aman.vaak.R
import com.aman.vaak.managers.KeyboardManager
import com.aman.vaak.managers.NotifyManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KeyboardSwitchHandlerTest {
    @Mock
    private lateinit var keyboardManager: KeyboardManager

    @Mock
    private lateinit var notifyManager: NotifyManager

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var view: View

    @Mock
    private lateinit var imeService: InputMethodService

    private lateinit var handler: KeyboardSwitchHandlerImpl

    @BeforeEach
    fun setup() {
        handler =
            KeyboardSwitchHandlerImpl(
                keyboardManager,
                notifyManager,
                context,
            )

        whenever(view.context).thenReturn(context)
        whenever(context.getString(any())).thenReturn("Error")
    }

    @Nested
    inner class IMEServiceTests {
        @Test
        fun `attachIME stores IME service`() {
            handler.attachIME(imeService)

            handler.attachView(view)

            verify(view).findViewById<View>(R.id.switchKeyboardButton)
        }
    }

    @Nested
    inner class KeyboardSwitchTests {
        @Test
        fun `handleSwitchKeyboard shows keyboard selector`() {
            handler.handleSwitchKeyboard()

            verify(keyboardManager).showKeyboardSelector()
        }
    }

    @Nested
    inner class ViewLifecycleTests {
        @Test
        fun `onViewAttached sets up switch button`() {
            handler.attachView(view)

            verify(view).findViewById<View>(R.id.switchKeyboardButton)
        }
    }
}
