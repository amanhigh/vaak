package com.aman.vaak.handlers

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import com.aman.vaak.R
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.SettingsManager
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
                setOnLongClickListener {
                    toggleTranslation()
                    true
                }
                updateLanguageDisplay()
            }
        }

        private fun toggleTranslation() {
            try {
                val currentTarget = settingsManager.getTargetLanguage()
                val currentVoiceInput = settingsManager.getVoiceInputLanguage()

                val newTarget =
                    if (currentTarget == null && currentVoiceInput != null) {
                        currentVoiceInput // Enable translation
                    } else {
                        null // Disable translation
                    }

                settingsManager.saveTargetLanguage(newTarget)
                updateLanguageDisplay()
            } catch (e: Exception) {
                handleError(e)
            }
        }

        override fun cycleLanguage() {
            try {
                val currentLang = settingsManager.getVoiceInputLanguage()
                val favorites = settingsManager.getFavoriteLanguages()

                // If no favorites, stay on auto-detect
                if (favorites.isEmpty()) {
                    settingsManager.saveVoiceInputLanguage(null)
                    updateLanguageDisplay()
                    return
                }

                val nextLang =
                    when {
                        // If current is null (auto-detect), go to first favorite
                        currentLang == null -> favorites.first()
                        // If current is last favorite, go back to auto-detect
                        favorites.indexOf(currentLang) == favorites.size - 1 -> null
                        // Otherwise go to next favorite
                        else -> {
                            val currentIndex = favorites.indexOf(currentLang)
                            favorites[currentIndex + 1]
                        }
                    }

                settingsManager.saveVoiceInputLanguage(nextLang)
                updateLanguageDisplay()
            } catch (e: Exception) {
                handleError(e)
            }
        }

        private fun updateLanguageDisplay() {
            try {
                withView { view ->
                    view.findViewById<Button>(R.id.languageButton)?.apply {
                        val voiceInputLang = settingsManager.getVoiceInputLanguage()
                        val targetLang = settingsManager.getTargetLanguage()
                        val hasTranslation = targetLang == voiceInputLang

                        text =
                            when {
                                voiceInputLang == null -> "AUTO"
                                hasTranslation -> "${voiceInputLang.displayCode} ⚡️"
                                else -> voiceInputLang.displayCode
                            }
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
