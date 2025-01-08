package com.aman.vaak.handlers

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aman.vaak.R
import com.aman.vaak.databinding.DialogAddPromptBinding
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.PromptsManager
import com.aman.vaak.models.Prompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class PromptOperationException(message: String) : Exception(message) {
    class ValidationException(message: String) :
        PromptOperationException("Invalid prompt: $message")

    class DialogCreationError(cause: Throwable) :
        PromptOperationException("Create dialog error: ${cause.message}")

    class SaveException(cause: Throwable) :
        PromptOperationException("Failed to save prompt: ${cause.message}")

    class DeleteException(cause: Throwable) :
        PromptOperationException("Failed to delete prompt: ${cause.message}")
}

interface PromptHandler : BaseViewHandler {
    fun showPromptDialog(existingPrompt: Prompt? = null)
}

@Singleton
class PromptHandlerImpl
    @Inject
    constructor(
        private val promptsManager: PromptsManager,
        private val notifyManager: NotifyManager,
        private val scope: CoroutineScope,
    ) : BaseViewHandlerImpl(), PromptHandler {
        private var currentPrompts = emptyList<Prompt>()

        private val adapter by lazy {
            PromptDialogImpl(
                onDeleteClick = { prompt -> handleDeletePrompt(prompt) },
                onPromptClick = { prompt -> showPromptDialog(prompt) },
                notifyManager = notifyManager,
            ).also { adapter ->
                adapter.updatePrompts(currentPrompts)
            }
        }

        override fun handleError(error: Exception) {
            withView { view ->
                notifyManager.showError(
                    title = view.context.getString(R.string.error_prompt_operation),
                    message = error.message ?: view.context.getString(R.string.error_generic_details),
                )
            }
        }

        override fun onViewAttached(view: View) {
            view.findViewById<RecyclerView>(R.id.promptsList)?.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@PromptHandlerImpl.adapter
            }

            setupAddButton(view)

            loadPrompts()
        }

        override fun onViewDetached() {
            currentPrompts = emptyList()
        }

        private fun setupAddButton(view: View) {
            view.findViewById<Button>(R.id.addPromptButton)?.setOnClickListener {
                showPromptDialog()
            }
        }

        private fun loadPrompts() {
            scope.launch {
                try {
                    val prompts = promptsManager.getPrompts()
                    currentPrompts = prompts
                    adapter.updatePrompts(prompts)
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }

        override fun showPromptDialog(existingPrompt: Prompt?) {
            try {
                withView { view ->
                    val dialogBinding =
                        DialogAddPromptBinding.inflate(
                            LayoutInflater.from(view.context),
                        )

                    // Create dialog using view's context
                    AlertDialog.Builder(view.context)
                        .setTitle(
                            if (existingPrompt == null) {
                                R.string.prompt_dialog_add
                            } else {
                                R.string.prompt_dialog_edit
                            },
                        )
                        .setView(dialogBinding.root)
                        .create()
                        .also { dialog ->
                            setupDialogFields(dialogBinding, existingPrompt)
                            setupDialogButtons(dialogBinding, dialog, existingPrompt)
                            dialog.show()
                        }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }

        private fun setupDialogFields(
            dialogBinding: DialogAddPromptBinding,
            existingPrompt: Prompt?,
        ) {
            existingPrompt?.let { prompt ->
                dialogBinding.apply {
                    promptNameInput.setText(prompt.name)
                    promptContentInput.setText(prompt.content)
                }
            }
        }

        private fun setupDialogButtons(
            dialogBinding: DialogAddPromptBinding,
            dialog: AlertDialog,
            existingPrompt: Prompt?,
        ) {
            dialogBinding.saveButton.setOnClickListener {
                val name = dialogBinding.promptNameInput.text?.toString() ?: return@setOnClickListener
                val content = dialogBinding.promptContentInput.text?.toString() ?: return@setOnClickListener

                if (name.isNotBlank() && content.isNotBlank()) {
                    val prompt =
                        existingPrompt?.copy(
                            name = name,
                            content = content,
                            updatedAt = System.currentTimeMillis(),
                        ) ?: Prompt(name = name, content = content)

                    savePrompt(prompt)
                    dialog.dismiss()
                }
            }

            dialogBinding.cancelButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        private fun savePrompt(prompt: Prompt) {
            scope.launch {
                try {
                    if (prompt.name.isBlank() || prompt.content.isBlank()) {
                        throw PromptOperationException.ValidationException("Name and content cannot be empty")
                    }
                    promptsManager.savePrompt(prompt)
                        .onSuccess { loadPrompts() }
                        .onFailure { e ->
                            throw PromptOperationException.SaveException(e)
                        }
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }

        private fun handleDeletePrompt(prompt: Prompt) {
            scope.launch {
                try {
                    promptsManager.deletePrompt(prompt.id)
                        .onSuccess { loadPrompts() }
                        .onFailure { e ->
                            throw PromptOperationException.DeleteException(e)
                        }
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }
    }
