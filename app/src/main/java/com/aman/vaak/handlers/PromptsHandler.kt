package com.aman.vaak.handlers

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.aman.vaak.R
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.PromptsManager
import com.aman.vaak.models.Prompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

interface PromptsHandler {
    /**
     * Start observing prompts view state
     * @param parentView View containing prompts UI elements
     */
    fun startObservingView(parentView: View)

    /**
     * Shows the prompts selection UI
     */
    fun showPrompts()

    /**
     * Hides the prompts selection UI
     */
    fun hidePrompts()
    // FIXME: Make Unuused Methods Private

    /**
     * Releases resources and stops view observation
     */
    fun release()
}

class PromptsHandlerImpl
    @Inject
    constructor(
        private val promptsManager: PromptsManager,
        private val textHandler: TextHandler,
        private val notifyManager: NotifyManager,
        private val scope: CoroutineScope,
    ) : PromptsHandler {
        private var currentView: View? = null

        override fun startObservingView(parentView: View) {
            currentView = parentView
        }

        override fun showPrompts() {
            scope.launch {
                try {
                    val prompts = promptsManager.getPrompts()
                    currentView?.post {
                        createPromptButtons(prompts)
                        currentView?.findViewById<LinearLayout>(R.id.promptsContainer)?.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }

        override fun hidePrompts() {
            currentView?.findViewById<LinearLayout>(R.id.promptsContainer)?.apply {
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

        private fun createPromptButtons(prompts: List<Prompt>) {
            currentView?.findViewById<LinearLayout>(R.id.promptsContainer)?.apply {
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

                // Setup hide button
                findViewById<Button>(R.id.hidePromptsButton).setOnClickListener {
                    hidePrompts()
                }
            }
        }

        private fun handlePromptSelection(prompt: Prompt) {
            try {
                textHandler.handleInsertText(prompt.content)
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
                    title = error.message ?: context.getString(R.string.error_unknown),
                    message = error.message ?: "Details Unknown",
                )
            }
        }

        override fun release() {
            currentView = null
        }
    }
