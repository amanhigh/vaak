package com.aman.vaak.handlers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aman.vaak.R
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.SettingsManager
import com.aman.vaak.models.Language
import javax.inject.Inject
import javax.inject.Singleton

interface LanguageHandler : BaseViewHandler {
    fun showLanguageSelection(context: Context)

    fun getCurrentLanguages(): List<Language>

    fun registerLanguageChangeListener(listener: () -> Unit)
}

@Singleton
class LanguageHandlerImpl
    @Inject
    constructor(
        private val settingsManager: SettingsManager,
        private val notifyManager: NotifyManager,
    ) : BaseViewHandlerImpl(), LanguageHandler {
        companion object {
            private const val MAX_SELECTABLE_LANGUAGES = 3
        }

        override fun onViewAttached(view: View) {
            setupLanguageButton(view)
            updateLanguageDisplay()
        }

        override fun onViewDetached() {
            // No cleanup needed
        }

        private fun setupLanguageButton(view: View) {
            view.findViewById<Button>(R.id.languageButton)?.apply {
                setOnClickListener { cycleLanguage() }
                updateLanguageDisplay()
            }
        }

        private fun cycleLanguage() {
            val currentLang = settingsManager.getTargetLanguage()
            val favorites = settingsManager.getFavoriteLanguages()

            // If no favorites, stay on English
            if (favorites.isEmpty()) {
                settingsManager.saveTargetLanguage(Language.ENGLISH)
                updateLanguageDisplay()
                return
            }

            // Find next language in favorites list
            val currentIndex = favorites.indexOf(currentLang)
            val nextLang =
                if (currentIndex == -1 || currentIndex == favorites.size - 1) {
                    favorites.first()
                } else {
                    favorites[currentIndex + 1]
                }

            settingsManager.saveTargetLanguage(nextLang)
            updateLanguageDisplay()
            languageChangeListener?.invoke()
        }

        private fun updateLanguageDisplay() {
            withView { view ->
                view.findViewById<Button>(R.id.languageButton)?.apply {
                    val lang = settingsManager.getTargetLanguage()
                    text = lang.displayCode
                }
            }
        }

        private inner class LanguageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val checkbox: CheckBox = view.findViewById(R.id.languageCheckbox)
            private val nativeName: TextView = view.findViewById(R.id.nativeNameText)
            private val englishName: TextView = view.findViewById(R.id.englishNameText)

            fun bind(
                language: Language,
                isSelected: Boolean,
                onCheckedChange: (Boolean) -> Unit,
            ) {
                checkbox.isChecked = isSelected
                nativeName.text = language.nativeName
                englishName.text = language.englishName
                checkbox.setOnCheckedChangeListener { _, checked -> onCheckedChange(checked) }
            }
        }

        private inner class LanguageAdapter : RecyclerView.Adapter<LanguageViewHolder>() {
            private val languages = Language.values().toList()
            private val selections = mutableSetOf<Language>()

            init {
                selections.addAll(settingsManager.getFavoriteLanguages())
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

            override fun onBindViewHolder(
                holder: LanguageViewHolder,
                position: Int,
            ) {
                val language = languages[position]
                holder.bind(language, language in selections) { checked ->
                    if (checked) {
                        if (selections.size >= MAX_SELECTABLE_LANGUAGES) {
                            holder.itemView.findViewById<CheckBox>(R.id.languageCheckbox).isChecked = false
                            notifyManager.showWarning(
                                title = holder.itemView.context.getString(R.string.lang_limit_title),
                                message = holder.itemView.context.getString(R.string.lang_limit_message),
                            )
                        } else {
                            selections.add(language)
                        }
                    } else {
                        selections.remove(language)
                    }
                }
            }

            override fun getItemCount() = languages.size

            fun getSelectedLanguages() = selections.toList()
        }

        private var languageChangeListener: (() -> Unit)? = null

        override fun registerLanguageChangeListener(listener: () -> Unit) {
            languageChangeListener = listener
        }

        private fun onLanguagesSelected(languages: List<Language>) {
            settingsManager.saveFavoriteLanguages(languages)
            languageChangeListener?.invoke()
        }

        override fun showLanguageSelection(context: Context) {
            val adapter = LanguageAdapter()
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_language_selection, null)
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.languageList)

            recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                this.adapter = adapter
            }

            AlertDialog.Builder(context)
                .setTitle(R.string.lang_select_title)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_save) { dialog, _ ->
                    val selections = adapter.getSelectedLanguages()
                    val languages =
                        if (selections.isNotEmpty()) {
                            selections
                        } else {
                            listOf(Language.ENGLISH)
                        }
                    onLanguagesSelected(languages)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.btn_cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        override fun getCurrentLanguages(): List<Language> = settingsManager.getFavoriteLanguages()
    }
