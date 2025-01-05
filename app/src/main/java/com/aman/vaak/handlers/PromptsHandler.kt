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

interface PromptsHandler : BaseViewHandler {
    /**
     * Shows the prompts selection UI
     */
    fun showPrompts()

    /**
     * Hides the prompts selection UI
     */
    fun hidePrompts()
}

@Singleton
class PromptsHandlerImpl
    @Inject
    constructor(
        private val promptsManager: PromptsManager,
        private val textManager: TextManager,
        private val notifyManager: NotifyManager,
        private val scope: CoroutineScope,
    ) : BaseViewHandlerImpl(), PromptsHandler {
        override fun onViewAttached(view: View) {
            setupHideButton(view)
        }

        override fun onViewDetached() {
            // No cleanup needed
        }

        // FIXME: Insert Yes/Continue Button for LLM Chat from Prompts
        private fun setupHideButton(view: View) {
            view.findViewById<Button>(R.id.hidePromptsButton)?.setOnClickListener {
                hidePrompts()
            }
        }

        override fun showPrompts() {
            scope.launch {
                try {
                    val prompts = promptsManager.getPrompts()
                    withView { view ->
                        view.post {
                            createPromptButtons(prompts)
                            requireView<LinearLayout>(R.id.promptsContainer).visibility = View.VISIBLE
                        }
                    }
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }

        override fun hidePrompts() {
            withView { view ->
                view.findViewById<LinearLayout>(R.id.promptsContainer)?.apply {
                    visibility = View.GONE
                    // Remove all views except the hide button
                    var i = childCount - 1
                    while (i >= 0) {
                        val child = getChildAt(i)
                        if (child.id != R.id.hidePromptsButton) {
                            removeViewAt(i)
                        }
                        i--
                    }
                }
            }
        }

        private fun createPromptButtons(prompts: List<Prompt>) {
            withView { view ->
                view.findViewById<LinearLayout>(R.id.promptsContainer)?.apply {
                    // Clear existing prompt buttons
                    var i = childCount - 1
                    while (i >= 0) {
                        val child = getChildAt(i)
                        if (child.id != R.id.hidePromptsButton) {
                            removeViewAt(i)
                        }
                        i--
                    }

                    // Add new prompt buttons
                    prompts.forEach { prompt ->
                        val button =
                            Button(context).apply {
                                layoutParams =
                                    LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        40.dpToPx(context),
                                    )
                                text = prompt.name
                                setOnClickListener {
                                    handlePromptSelection(prompt)
                                }
                            }
                        // Add at the beginning to keep hide button at bottom
                        addView(button, 0)
                    }
                }
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

        private fun handleError(error: Exception) {
            currentView?.context?.let { context ->
                notifyManager.showError(
                    title = error.message ?: context.getString(R.string.error_generic),
                    message = error.message ?: "Details Unknown",
                )
            }
        }
    }
