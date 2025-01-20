package com.aman.vaak.handlers

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.aman.vaak.R
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.PromptsManager
import com.aman.vaak.managers.TextManager
import com.aman.vaak.models.Prompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface PromptKeyHandler : BaseViewHandler {
    /** Shows the prompts selection UI */
    fun showPrompts()
}

@Singleton
class PromptKeyHandlerImpl
    @Inject
    constructor(
        private val promptsManager: PromptsManager,
        private val textManager: TextManager,
        private val notifyManager: NotifyManager,
        private val scope: CoroutineScope,
    ) : BaseViewHandlerImpl(), PromptKeyHandler {
        override fun onViewAttached(view: View) {
            setupHideButton(view)
        }

        override fun onViewDetached() {
            // No cleanup needed
        }

        private fun setupHideButton(view: View) {
            view.findViewById<Button>(R.id.hidePromptsButton)?.setOnClickListener { hidePrompts() }
        }

        override fun showPrompts() {
            scope.launch {
                try {
                    val prompts = promptsManager.getPrompts()
                    withView { view ->
                        view.post {
                            createPromptButtons(prompts)
                            requireView<LinearLayout>(R.id.promptsContainer).apply {
                                visibility = View.VISIBLE
                            }
                        }
                    }
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }

        private fun hidePrompts() {
            withView { view ->
                view.findViewById<LinearLayout>(R.id.promptsContainer)?.visibility = View.GONE
                view.findViewById<LinearLayout>(R.id.promptButtonsContainer)?.removeAllViews()
            }
        }

        private fun createPromptButtons(prompts: List<Prompt>) {
            withView { view ->
                view.findViewById<LinearLayout>(R.id.promptButtonsContainer)?.apply {
                    removeAllViews()
                    prompts.forEach { prompt -> addView(createPromptButton(prompt, context)) }
                }
            }
        }

        private fun createPromptButton(
            prompt: Prompt,
            context: Context,
        ): Button {
            return Button(context, null, 0, R.style.VaakKeyboardButton_Prompt).apply {
                text = prompt.name
                setOnClickListener { handlePromptSelection(prompt) }
            }
        }

        private fun handlePromptSelection(prompt: Prompt) {
            try {
                textManager.insertText(prompt.content)
                hidePrompts()
            } catch (e: Exception) {
                handleError(e)
            }
        }

        private fun Int.dpToPx(context: Context): Int {
            return (this * context.resources.displayMetrics.density).toInt()
        }

        override fun handleError(error: Exception) {
            currentView?.context?.let { context ->
                notifyManager.showError(
                    title = error.message ?: context.getString(R.string.error_generic),
                    message = error.message ?: "Details Unknown",
                )
            }
        }
    }
