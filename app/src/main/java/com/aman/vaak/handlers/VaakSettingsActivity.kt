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

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load existing API key if any
        binding.apiKeyInput.setText(settingsManager.getApiKey())

        binding.saveButton.setOnClickListener {
            val apiKey = binding.apiKeyInput.text.toString()
            settingsManager.saveApiKey(apiKey)
            Toast.makeText(this, R.string.api_key_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
