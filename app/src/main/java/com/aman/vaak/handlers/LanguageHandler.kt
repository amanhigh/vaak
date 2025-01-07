package com.aman.vaak.handlers

import android.content.Context
import android.view.View
import com.aman.vaak.R
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.SettingsManager
import javax.inject.Inject
import javax.inject.Named

interface LanguageHandler : BaseViewHandler {
    /**
     * Shows the favorite languages selection dialog
     */
    fun showFavoriteLanguageSelection(context: Context)

    /**
     * Shows the voice input language selection dialog
     */
    fun showVoiceInputLanguageSelection(context: Context)

    /**
     * Register listener for favorite languages changes
     */
    fun registerFavoriteLanguagesListener(listener: () -> Unit)

    /**
     * Register listener for voice input language changes
     */
    fun registerVoiceInputListener(listener: () -> Unit)

    /**
     * Gets display text for favorite languages
     */
    fun getFavoriteLanguagesDisplayText(): String

    /**
     * Gets display text for voice input language
     */
    fun getVoiceInputDisplayText(): String
}

class LanguageHandlerImpl
    @Inject
    constructor(
        private val settingsManager: SettingsManager,
        private val notifyManager: NotifyManager,
        @Named("favoriteDialog") private val favoriteDialog: LanguageDialog,
        @Named("voiceInputDialog") private val voiceInputDialog: LanguageDialog,
    ) : BaseViewHandlerImpl(), LanguageHandler {
        private var favoriteLanguagesListener: (() -> Unit)? = null
        private var voiceInputListener: (() -> Unit)? = null

        override fun registerFavoriteLanguagesListener(listener: () -> Unit) {
            favoriteLanguagesListener = listener
        }

        override fun registerVoiceInputListener(listener: () -> Unit) {
            voiceInputListener = listener
        }

        override fun handleError(error: Exception) {
            currentView?.context?.let { context ->
                notifyManager.showError(
                    title = context.getString(R.string.error_language_selection),
                    message = error.message ?: context.getString(R.string.error_generic_details),
                )
            }
        }

        override fun showFavoriteLanguageSelection(context: Context) {
            try {
                val initialSelection = settingsManager.getFavoriteLanguages().toSet()
                favoriteDialog.apply {
                    updateSelection(initialSelection)
                    show(context) { selectedLanguages ->
                        settingsManager.saveFavoriteLanguages(selectedLanguages.filterNotNull())
                        favoriteLanguagesListener?.invoke()
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }

        override fun showVoiceInputLanguageSelection(context: Context) {
            try {
                val initialSelection = setOfNotNull(settingsManager.getVoiceInputLanguage())
                voiceInputDialog.apply {
                    updateSelection(initialSelection)
                    show(context) { selectedLanguages ->
                        settingsManager.saveVoiceInputLanguage(selectedLanguages.firstOrNull())
                        voiceInputListener?.invoke()
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }

        override fun getFavoriteLanguagesDisplayText(): String {
            return settingsManager.getFavoriteLanguages()
                .joinToString("\n") { "${it.nativeName} (${it.englishName})" }
        }

        override fun getVoiceInputDisplayText(): String {
            return settingsManager.getVoiceInputLanguage()?.let { language ->
                "${language.nativeName} (${language.englishName})"
            } ?: "Auto Detect"
        }

        override fun onViewAttached(view: View) {
            // No setup needed
        }

        override fun onViewDetached() {
            favoriteLanguagesListener = null
            voiceInputListener = null
        }
    }
