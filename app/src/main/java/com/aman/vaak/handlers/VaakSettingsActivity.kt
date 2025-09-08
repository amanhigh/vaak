package com.aman.vaak.handlers

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
    // XXX: More Settings like Output Speed.

    // XXX: Record Time Transcribed and Translated for approx Billing.

    // XXX: Reword via Mini Model, Setup Mini Model eg. 4o mini

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
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) = Unit
            },
        )

        // Language selection
        binding.languageButton.setOnClickListener {
            languageHandler.showFavoriteLanguageSelection(this)
        }

        // Translation settings
        setupTranslationSettings()

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

    private fun setupTranslationSettings() {
        setupTranslationModelSpinner()
        setupTranslationPromptInput()
        setupTranslationResetButton()
    }

    private fun setupTranslationModelSpinner() {
        val modelOptions = getModelOptions()
        val modelAdapter = createModelAdapter(modelOptions)

        binding.translationModelSpinner.adapter = modelAdapter
        setCurrentModelSelection(modelOptions)
        setModelSelectionListener(modelOptions)
    }

    private fun getModelOptions() =
        arrayOf(
            "gpt-4o" to getString(R.string.model_gpt4o),
            "gpt-4o-mini" to getString(R.string.model_gpt4o_mini),
            "gpt-3.5-turbo" to getString(R.string.model_gpt35_turbo),
        )

    private fun createModelAdapter(modelOptions: Array<Pair<String, String>>): ArrayAdapter<String> {
        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                modelOptions.map { it.second },
            )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return adapter
    }

    private fun setCurrentModelSelection(modelOptions: Array<Pair<String, String>>) {
        val currentModel = settingsManager.getTranslationModel()
        val currentIndex = modelOptions.indexOfFirst { it.first == currentModel }
        if (currentIndex >= 0) {
            binding.translationModelSpinner.setSelection(currentIndex)
        }
    }

    private fun setModelSelectionListener(modelOptions: Array<Pair<String, String>>) {
        binding.translationModelSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val selectedModel = modelOptions[position].first
                    settingsManager.saveTranslationModel(selectedModel)
                    showSnackbar("Model updated to ${modelOptions[position].second}")
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
    }

    private fun setupTranslationPromptInput() {
        binding.translationPromptInput.setText(settingsManager.getTranslationPrompt())
        binding.translationPromptInput.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    s?.let {
                        val prompt = it.toString().trim()
                        if (prompt.isNotEmpty()) {
                            settingsManager.saveTranslationPrompt(prompt)
                        }
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) = Unit
            },
        )
    }

    private fun setupTranslationResetButton() {
        binding.resetPromptButton.setOnClickListener {
            settingsManager.resetAllTranslationSettingsToDefault()
            updateUIToDefaults()
            showSnackbar(getString(R.string.settings_translation_reset_success))
        }
    }

    private fun updateUIToDefaults() {
        binding.translationPromptInput.setText(settingsManager.getTranslationPrompt())
        val modelOptions = getModelOptions()
        val defaultModel = settingsManager.getTranslationModel()
        val defaultIndex = modelOptions.indexOfFirst { it.first == defaultModel }
        if (defaultIndex >= 0) {
            binding.translationModelSpinner.setSelection(defaultIndex)
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
