package com.aman.vaak.handlers

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import com.aman.vaak.R
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.SettingsManager
import com.aman.vaak.models.SupportedLanguage
import javax.inject.Inject

interface SettingsHandler {
    /**
     * Start observing settings view state
     * @param parentView View containing settings UI elements
     * @param context Context for launching activities
     */
    fun startObservingView(
        parentView: View,
        context: Context,
    )

    /**
     * Cycle through supported languages
     */
    fun cycleLanguage()

    /**
     * Updates language button text to current language
     */
    fun updateLanguageDisplay()

    /**
     * Launch settings activity
     */
    fun launchSettings()

    /**
     * Releases resources and stops view observation
     */
    fun release()
}

class SettingsHandlerImpl
    @Inject
    constructor(
        private val settingsManager: SettingsManager,
        private val notifyManager: NotifyManager,
    ) : SettingsHandler {
        private var currentView: View? = null

        // FIXME: Should Context be Injected in Constructor
        private var currentContext: Context? = null

        override fun startObservingView(
            parentView: View,
            context: Context,
        ) {
            currentView = parentView
            currentContext = context

            setupLanguageButton()
            setupSettingsButton()
        }

        private fun setupLanguageButton() {
            currentView?.findViewById<Button>(R.id.languageButton)?.apply {
                setOnClickListener { cycleLanguage() }
                updateLanguageDisplay()
            }
        }

        private fun setupSettingsButton() {
            currentView?.findViewById<Button>(R.id.settingsButton)?.apply {
                setOnClickListener { launchSettings() }
            }
        }

        override fun cycleLanguage() {
            val currentLang = settingsManager.getTargetLanguage()
            val nextLang =
                when (currentLang) {
                    SupportedLanguage.ENGLISH.code -> SupportedLanguage.HINDI
                    SupportedLanguage.HINDI.code -> SupportedLanguage.PUNJABI
                    else -> SupportedLanguage.ENGLISH
                }
            settingsManager.saveTargetLanguage(nextLang.code)
            updateLanguageDisplay()
        }

        override fun updateLanguageDisplay() {
            currentView?.findViewById<Button>(R.id.languageButton)?.apply {
                text =
                    SupportedLanguage.values()
                        .first { it.code == settingsManager.getTargetLanguage() }
                        .display
            }
        }

        override fun launchSettings() {
            try {
                currentContext?.let { context ->
                    val intent =
                        Intent(context, VaakSettingsActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                notifyManager.showError(
                    title = currentContext?.getString(R.string.error_unknown) ?: "",
                    message = e.message ?: "Failed to launch settings",
                )
            }
        }

        override fun release() {
            currentView = null
            currentContext = null
        }
    }
