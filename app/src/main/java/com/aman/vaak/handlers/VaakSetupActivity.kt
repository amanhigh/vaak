package com.aman.vaak.handlers

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.aman.vaak.R
import com.aman.vaak.databinding.ActivitySetupBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VaakSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOpenSettings.setOnClickListener { openKeyboardSettings() }

        checkKeyboardState()
    }

    override fun onResume() {
        super.onResume()
        checkKeyboardState()
    }

    private fun openKeyboardSettings() {
        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
    }

    private fun checkKeyboardState() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val isEnabled =
                inputMethodManager.enabledInputMethodList.map { it.id }.any {
                    it.contains(packageName)
                }

        binding.textInstructions.setText(
                if (isEnabled) R.string.keyboard_enabled else R.string.enable_keyboard_instruction
        )
    }
}
