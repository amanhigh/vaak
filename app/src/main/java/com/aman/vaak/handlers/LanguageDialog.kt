package com.aman.vaak.handlers

import android.content.Context
import com.aman.vaak.R
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.SettingsManager
import com.aman.vaak.models.Language

interface LanguageDialog {
    /**
     * Updates the current language selection
     * @param selection Set of currently selected languages
     */
    fun updateSelection(selection: Set<Language?>)

    /**
     * Shows the language selection dialog
     * @param context Context to use for showing dialog
     * @param onSave Callback for when selection is saved
     */
    fun show(
        context: Context,
        onSave: (List<Language?>) -> Unit,
    )

    /**
     * Gets currently selected languages
     * @return List of selected languages
     */
    fun getSelectedLanguages(): List<Language?>
}

internal class FavoriteLanguageDialog(
    items: List<Language>,
    initialSelection: Set<Language>,
    settingsManager: SettingsManager,
    notifyManager: NotifyManager,
) : BaseLanguageDialog(
        items = items,
        initialSelection = initialSelection,
        settingsManager = settingsManager,
        notifyManager = notifyManager,
    ) {
    override fun getTitleResId() = R.string.lang_select_title

    override fun getMaxSelections() = 3

    override fun onSelectionChange(
        language: Language?,
        checked: Boolean,
    ) {
        if (checked) {
            selections.add(language)
        } else {
            selections.remove(language)
        }
    }
}

internal class VoiceInputLanguageDialog(
    items: List<Language?>,
    initialSelection: Set<Language?>,
    settingsManager: SettingsManager,
    notifyManager: NotifyManager,
) : BaseLanguageDialog(
        items = items,
        initialSelection = initialSelection,
        settingsManager = settingsManager,
        notifyManager = notifyManager,
    ) {
    override fun getTitleResId() = R.string.lang_select_voice_title

    override fun getMaxSelections() = 1 // Single selection only

    override fun onSelectionChange(
        language: Language?,
        checked: Boolean,
    ) {
        if (checked) {
            selections.clear()
            selections.add(language)
        } else if (language in selections) {
            selections.remove(language)
        }
    }
}
