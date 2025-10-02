package com.aman.vaak.handlers

import android.content.Context
import android.view.View
import android.view.inputmethod.InputConnection
import com.aman.vaak.R
import com.aman.vaak.managers.ClipboardManager
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.TextManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class TextHandlerTest {
    @Mock
    private lateinit var clipboardManager: ClipboardManager

    @Mock
    private lateinit var textManager: TextManager

    @Mock
    private lateinit var notifyManager: NotifyManager

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var promptKeyHandler: PromptKeyHandler

    @Mock
    private lateinit var numpadHandler: NumpadHandler

    @Mock
    private lateinit var view: View

    @Mock
    private lateinit var inputConnection: InputConnection

    private lateinit var handler: TextHandlerImpl

    @BeforeEach
    fun setup() {
        handler =
            TextHandlerImpl(
                clipboardManager,
                textManager,
                notifyManager,
                context,
                promptKeyHandler,
                numpadHandler,
            )

        whenever(view.context).thenReturn(context)
    }

    @Nested
    inner class ViewLifecycleTests {
        @Test
        fun `onViewAttached is called when view attached`() {
            handler.attachView(view)

            verify(view).findViewById<View>(R.id.pasteButton)
        }
    }

    @Nested
    inner class InputConnectionTests {
        @Test
        fun `attachInputConnection attaches to both text and clipboard managers`() {
            handler.attachInputConnection(inputConnection)

            verify(textManager).attachInputConnection(inputConnection)
            verify(clipboardManager).attachInputConnection(inputConnection)
        }

        @Test
        fun `detachInputConnection detaches from both managers`() {
            handler.detachInputConnection()

            verify(textManager).detachInputConnection()
            verify(clipboardManager).detachInputConnection()
        }
    }
}
