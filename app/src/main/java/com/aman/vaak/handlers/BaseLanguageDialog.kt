package com.aman.vaak.handlers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aman.vaak.R
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.SettingsManager
import com.aman.vaak.models.Language

internal abstract class BaseLanguageDialog(
    protected val items: List<Language?>,
    initialSelection: Set<Language?>,
    protected val settingsManager: SettingsManager,
    protected val notifyManager: NotifyManager,
) : RecyclerView.Adapter<BaseLanguageDialog.LanguageViewHolder>(), LanguageDialog {
    protected val selections =
        mutableSetOf<Language?>().apply {
            addAll(initialSelection)
        }

    protected fun handleError(e: Exception) {
        notifyManager.showError(
            title = "Language Selection Error",
            message = e.message ?: "Unknown error",
        )
    }

    override fun updateSelection(selection: Set<Language?>) {
        selections.clear()
        selections.addAll(selection)
        notifyDataSetChanged()
    }

    override fun getSelectedLanguages(): List<Language?> = selections.toList()

    protected abstract fun getTitleResId(): Int

    protected abstract fun getMaxSelections(): Int?

    protected abstract fun onSelectionChange(
        language: Language?,
        checked: Boolean,
    )

    override fun onBindViewHolder(
        holder: LanguageViewHolder,
        position: Int,
    ) {
        val language = items[position]
        holder.bind(
            language = language,
            isSelected = language in selections,
        ) { checked ->
            when {
                // Case 1: Unselecting - Always allow
                !checked -> {
                    selections.remove(language)
                }

                // Case 2: Selecting when under limit - Allow
                checked && selections.size < getMaxSelections()!! -> {
                    selections.add(language)
                }

                // Case 3: Attempting to select when at limit - Show notification
                checked -> {
                    notifyManager.showWarning(
                        title = holder.itemView.context.getString(R.string.lang_limit_title),
                        message = "${getMaxSelections()} languages maximum",
                    )
                    // Checkbox state naturally reverts based on binding
                }
            }
            onSelectionChange(language, checked)
        }
    }

    override fun show(
        context: Context,
        onSave: (List<Language?>) -> Unit,
    ) {
        AlertDialog.Builder(context)
            .setTitle(getTitleResId())
            .setView(createRecyclerView(context))
            .setPositiveButton(R.string.btn_save) { _, _ ->
                onSave(getSelectedLanguages())
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    protected fun validateSelection(checked: Boolean) {
        getMaxSelections()?.let { max ->
            if (checked && selections.size >= max) {
                throw IllegalStateException("You can select up to $max languages")
            }
        }
    }

    private fun createRecyclerView(context: Context) =
        RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@BaseLanguageDialog
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): LanguageViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_language_selection, parent, false)
        return LanguageViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    internal inner class LanguageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val checkbox: CheckBox = view.findViewById(R.id.languageCheckbox)
        private val nativeName: TextView = view.findViewById(R.id.nativeNameText)
        private val englishName: TextView = view.findViewById(R.id.englishNameText)

        fun bind(
            language: Language?,
            isSelected: Boolean,
            onCheckedChange: (Boolean) -> Unit,
        ) {
            checkbox.isChecked = isSelected

            val context = itemView.context
            nativeName.text = language?.nativeName ?: context.getString(R.string.lang_auto_detect)

            if (language != null) {
                englishName.text = language.englishName
                englishName.visibility = View.VISIBLE
            } else {
                englishName.visibility = View.GONE
            }

            checkbox.setOnCheckedChangeListener { _, checked -> onCheckedChange(checked) }
        }
    }
}
