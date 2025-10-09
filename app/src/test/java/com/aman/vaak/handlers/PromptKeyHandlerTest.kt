package com.aman.vaak.handlers

import android.content.Context
import android.view.View
import com.aman.vaak.R
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.PromptsManager
import com.aman.vaak.managers.TextManager
import com.aman.vaak.models.Prompt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockitoExtension::class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PromptKeyHandlerTest {
    @Mock
    private lateinit var promptsManager: PromptsManager

    @Mock
    private lateinit var textManager: TextManager

    @Mock
    private lateinit var notifyManager: NotifyManager

    @Mock
    private lateinit var view: View

    @Mock
    private lateinit var context: Context

    private lateinit var testScope: TestScope
    private lateinit var handler: PromptKeyHandlerImpl

    @BeforeEach
    fun setup() {
        testScope = TestScope(StandardTestDispatcher())
        handler =
            PromptKeyHandlerImpl(
                promptsManager,
                textManager,
                notifyManager,
                testScope,
            )

        whenever(view.context).thenReturn(context)
        whenever(context.getString(any())).thenReturn("Error")
    }

    @Nested
    inner class ViewLifecycleTests {
        @Test
        fun `onViewAttached queries button views`() {
            handler.attachView(view)

            verify(view).findViewById<View>(R.id.hidePromptsButton)
        }
    }

    @Nested
    inner class ShowPromptsTests {
        @Test
        fun `showPrompts loads prompts from manager`() =
            testScope.runTest {
                val prompts =
                    listOf(
                        Prompt(name = "Prompt 1", content = "Content 1", priority = 1),
                        Prompt(name = "Prompt 2", content = "Content 2", priority = 2),
                    )
                whenever(promptsManager.getPrompts()).thenReturn(prompts)
                handler.attachView(view)

                handler.showPrompts()
                advanceUntilIdle()

                verify(promptsManager).getPrompts()
            }
    }
}
