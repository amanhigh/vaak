package com.aman.vaak.handlers

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aman.vaak.R
import com.aman.vaak.databinding.ActivitySettingsBinding
import com.aman.vaak.databinding.DialogAddPromptBinding
import com.aman.vaak.managers.PromptsManager
import com.aman.vaak.managers.SettingsManager
import com.aman.vaak.models.Prompt
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VaakSettingsActivity : AppCompatActivity() {
    @Inject lateinit var settingsManager: SettingsManager

    @Inject lateinit var promptsManager: PromptsManager

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var promptHandler: PromptHandler

    // TODO: Backup and Restore of Settings (Excluding API Key)

    // TODO: More Settings like Input Language, Output Speed.

    // TODO: Record Time Transcribed and Translated for approx Billing.

    // TODO: Add About Page and Issue Reporting with Help, Donations.

    // TODO: Reword via Mini Model, Setup Mini Model eg. 4o mini

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupPromptsList()
        loadPrompts()
    }

    private fun setupViews() {
        // Load existing API key if any
        binding.apiKeyInput.setText(settingsManager.getApiKey())

        binding.saveButton.setOnClickListener {
            val apiKey = binding.apiKeyInput.text.toString()
            settingsManager.saveApiKey(apiKey)
            Toast.makeText(this, R.string.api_key_saved, Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.addPromptButton.setOnClickListener {
            showAddPromptDialog()
        }
    }

    private fun setupPromptsList() {
        promptHandler =
            PromptHandler { prompt ->
                handleDeletePrompt(prompt)
            }
        binding.promptsList.adapter = promptHandler
    }

    private fun loadPrompts() {
        lifecycleScope.launch {
            try {
                val prompts = promptsManager.getPrompts()
                promptHandler.updatePrompts(prompts)
            } catch (e: Exception) {
                Toast.makeText(this@VaakSettingsActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddPromptDialog() {
        val dialogBinding = DialogAddPromptBinding.inflate(layoutInflater)
        val dialog =
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_add_prompt_title)
                .setView(dialogBinding.root)
                .create()

        dialogBinding.saveButton.setOnClickListener {
            val name = dialogBinding.promptNameInput.text?.toString() ?: return@setOnClickListener
            val content = dialogBinding.promptContentInput.text?.toString() ?: return@setOnClickListener

            if (name.isNotBlank() && content.isNotBlank()) {
                savePrompt(name, content)
                dialog.dismiss()
            }
        }

        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun savePrompt(
        name: String,
        content: String,
    ) {
        lifecycleScope.launch {
            try {
                promptsManager.savePrompt(Prompt(name = name, content = content))
                    .onSuccess { loadPrompts() }
                    .onFailure { e ->
                        Toast.makeText(this@VaakSettingsActivity, e.message, Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(this@VaakSettingsActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleDeletePrompt(prompt: Prompt) {
        lifecycleScope.launch {
            try {
                promptsManager.deletePrompt(prompt.id)
                    .onSuccess { loadPrompts() }
                    .onFailure { e ->
                        Toast.makeText(this@VaakSettingsActivity, e.message, Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(this@VaakSettingsActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
