package com.aman.vaak.handlers

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import com.aman.vaak.R
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.SettingsManager
import com.aman.vaak.models.Language
import javax.inject.Inject

interface SettingsHandler : BaseViewHandler {
    /**
     * Cycle through supported languages
     */
    fun cycleLanguage()

    /**
     * Launch settings activity
     */
    fun launchSettings()
}

class SettingsHandlerImpl
    @Inject
    constructor(
        private val settingsManager: SettingsManager,
        private val notifyManager: NotifyManager,
        private val appContext: Context,
    ) : BaseViewHandlerImpl(), SettingsHandler {
        override fun handleError(error: Exception) {
            withView { view ->
                notifyManager.showError(
                    title = view.context.getString(R.string.error_settings_operation),
                    message = error.message ?: view.context.getString(R.string.error_generic_details),
                )
            }
        }

        override fun onViewAttached(view: View) {
            setupSettingsButton(view)
            setupLanguageButton()
            updateLanguageDisplay()
        }

        override fun onViewDetached() {
            // No cleanup needed
        }

        private fun setupSettingsButton(view: View) {
            view.findViewById<Button>(R.id.settingsButton)?.setOnClickListener {
                launchSettings()
            }
        }

        private fun setupLanguageButton() {
            requireView<Button>(R.id.languageButton).apply {
                setOnClickListener { cycleLanguage() }
                updateLanguageDisplay()
            }
        }

        override fun cycleLanguage() {
            try {
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
            } catch (e: Exception) {
                handleError(e)
            }
        }

        private fun updateLanguageDisplay() {
            try {
                withView { view ->
                    view.findViewById<Button>(R.id.languageButton)?.apply {
                        val lang = settingsManager.getTargetLanguage()
                        text = lang.displayCode
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }

        override fun launchSettings() {
            try {
                val intent =
                    Intent(appContext, VaakSettingsActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                appContext.startActivity(intent)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
