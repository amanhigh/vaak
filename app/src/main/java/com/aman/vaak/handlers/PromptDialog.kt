package com.aman.vaak.handlers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aman.vaak.R
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.models.Prompt

interface PromptDialog {
    fun updatePrompts(prompts: List<Prompt>)
}

class PromptDialogImpl(
    private val onDeleteClick: (Prompt) -> Unit,
    private val onPromptClick: (Prompt) -> Unit,
    private val notifyManager: NotifyManager,
) : RecyclerView.Adapter<PromptDialogImpl.PromptViewHolder>(), PromptDialog {
    private var prompts: List<Prompt> = emptyList()

    override fun updatePrompts(newPrompts: List<Prompt>) {
        prompts = newPrompts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): PromptViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_prompt, parent, false)
        return PromptViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: PromptViewHolder,
        position: Int,
    ) {
        try {
            val prompt = prompts[position]
            holder.apply {
                promptName.text = prompt.name
                promptContent.text = prompt.content
                deleteButton.setOnClickListener {
                    try {
                        onDeleteClick(prompt)
                    } catch (e: Exception) {
                        handleError(e)
                    }
                }
                itemView.setOnClickListener {
                    try {
                        onPromptClick(prompt)
                    } catch (e: Exception) {
                        handleError(e)
                    }
                }
            }
        } catch (e: Exception) {
            handleError(e)
        }
    }

    override fun getItemCount(): Int = prompts.size

    private fun handleError(error: Exception) {
        notifyManager.showError(
            title = "Prompt Operation Error",
            message = error.message ?: "Unknown error occurred",
        )
    }

    class PromptViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val promptName: TextView = view.findViewById(R.id.promptName)
        val promptContent: TextView = view.findViewById(R.id.promptContent)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }
}
