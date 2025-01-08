package com.aman.vaak.handlers

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import com.aman.vaak.R
import com.aman.vaak.databinding.ActivitySettingsBinding
import com.aman.vaak.managers.SettingsManager
import com.google.android.material.snackbar.Snackbar
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
        // API Key handling
        binding.apiKeyInput.setText(settingsManager.getApiKey())
        binding.apiKeyInput.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    s?.let {
                        settingsManager.saveApiKey(it.toString())
                        showSnackbar(getString(R.string.settings_api_key_saved))
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {}
            },
        )

        // Language selection
        binding.languageButton.setOnClickListener {
            languageHandler.showFavoriteLanguageSelection(this)
        }

        // About button
        binding.aboutButton.setOnClickListener {
            aboutDialog.show(this)
        }

        languageHandler.registerFavoriteLanguagesListener {
            updateLanguageDisplays()
        }

        updateLanguageDisplays()
    }

    private fun updateLanguageDisplays() {
        binding.selectedLanguagesText.text = languageHandler.getFavoriteLanguagesDisplayText()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
