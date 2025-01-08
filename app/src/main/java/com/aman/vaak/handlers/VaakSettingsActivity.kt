package com.aman.vaak.handlers

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aman.vaak.R
import com.aman.vaak.databinding.ActivitySettingsBinding
import com.aman.vaak.managers.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VaakSettingsActivity : AppCompatActivity() {
    @Inject lateinit var settingsManager: SettingsManager

    @Inject lateinit var languageHandler: LanguageHandler

    @Inject lateinit var promptManagementHandler: PromptHandler

    @Inject lateinit var aboutDialog: AboutDialog

    @Inject lateinit var backupHandler: BackupHandler

    private lateinit var binding: ActivitySettingsBinding
    // TODO: More Settings like Output Speed.

    // TODO: Record Time Transcribed and Translated for approx Billing.

    // TODO: Reword via Mini Model, Setup Mini Model eg. 4o mini

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupHandlers()
    }

    private fun setupHandlers() {
        promptManagementHandler.attachView(binding.root)
        languageHandler.attachView(binding.root)
        backupHandler.attachView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        promptManagementHandler.detachView()
        languageHandler.detachView()
        backupHandler.detachView()
    }

    private fun setupViews() {
        // Load existing API key if any
        binding.apiKeyInput.setText(settingsManager.getApiKey())

        binding.saveButton.setOnClickListener {
            val apiKey = binding.apiKeyInput.text.toString()
            settingsManager.saveApiKey(apiKey)
            Toast.makeText(this, R.string.settings_api_key_saved, Toast.LENGTH_SHORT).show()
            finish()
        }

        // Favorite languages button
        binding.languageButton.setOnClickListener {
            languageHandler.showFavoriteLanguageSelection(this)
        }

        // Voice input language button
        binding.voiceInputLanguageButton.setOnClickListener {
            languageHandler.showVoiceInputLanguageSelection(this)
        }

        // About button
        binding.aboutButton.setOnClickListener {
            aboutDialog.show(this)
        }

        languageHandler.registerFavoriteLanguagesListener {
            updateLanguageDisplays()
        }

        languageHandler.registerVoiceInputListener {
            updateLanguageDisplays()
        }

        updateLanguageDisplays()
    }

    private fun updateLanguageDisplays() {
        binding.selectedLanguagesText.text = languageHandler.getFavoriteLanguagesDisplayText()
        binding.voiceInputLanguageText.text = languageHandler.getVoiceInputDisplayText()
    }
}
