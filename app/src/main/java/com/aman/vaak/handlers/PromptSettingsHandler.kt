package com.aman.vaak.handlers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aman.vaak.databinding.ItemPromptBinding
import com.aman.vaak.models.Prompt

class PromptSettingsHandler(
    private val onDeleteClick: (Prompt) -> Unit,
    private val onPromptClick: (Prompt) -> Unit,
) : RecyclerView.Adapter<PromptSettingsHandler.PromptViewHolder>() {
    private var prompts: List<Prompt> = emptyList()

    fun updatePrompts(newPrompts: List<Prompt>) {
        prompts = newPrompts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): PromptViewHolder {
        val binding =
            ItemPromptBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return PromptViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PromptViewHolder,
        position: Int,
    ) {
        holder.bind(prompts[position])
    }

    override fun getItemCount(): Int = prompts.size

    inner class PromptViewHolder(
        private val binding: ItemPromptBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(prompt: Prompt) {
            binding.apply {
                promptName.text = prompt.name
                promptContent.text = prompt.content
                deleteButton.setOnClickListener {
                    onDeleteClick(prompt)
                }
                root.setOnClickListener {
                    onPromptClick(prompt)
                }
            }
        }
    }
}
