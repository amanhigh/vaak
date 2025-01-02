package com.aman.vaak.handlers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aman.vaak.databinding.ItemPromptBinding
import com.aman.vaak.models.Prompt

class PromptHandler(
    private val onDeleteClick: (Prompt) -> Unit,
) : RecyclerView.Adapter<PromptHandler.PromptViewHolder>() {
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
            binding.promptName.text = prompt.name
            binding.promptContent.text = prompt.content
            binding.deleteButton.setOnClickListener {
                onDeleteClick(prompt)
            }
        }
    }
}
