package com.aman.vaak.handlers

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.aman.vaak.R
import com.aman.vaak.databinding.ActivitySetupBinding
import com.aman.vaak.managers.KeyboardSetupManager
import com.aman.vaak.managers.SettingsManager
import com.aman.vaak.managers.SystemManager
import com.aman.vaak.models.KeyboardSetupState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VaakSetupActivity : AppCompatActivity() {
    @Inject lateinit var keyboardSetupManager: KeyboardSetupManager

    @Inject lateinit var systemManager: SystemManager

    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAction.setOnClickListener { handleSetupStateAction() }

        updateSetupState()
    }

    override fun onResume() {
        super.onResume()
        updateSetupState()
    }

    @Inject lateinit var settingsManager: SettingsManager

    private fun updateSetupState() {
        when (keyboardSetupManager.getKeyboardSetupState()) {
            KeyboardSetupState.NEEDS_ENABLING -> {
                binding.textInstructions.setText(R.string.enable_keyboard_instruction)
                binding.btnAction.setText(R.string.btn_enable_keyboard)
                binding.btnSetDefault.visibility = View.GONE
            }
            KeyboardSetupState.NEEDS_PERMISSIONS -> {
                binding.textInstructions.setText(R.string.permissions_required)
                binding.btnAction.setText(R.string.btn_grant_permissions)
                binding.btnSetDefault.visibility = View.GONE
            }
            KeyboardSetupState.NEEDS_API_KEY -> {
                binding.textInstructions.setText(R.string.setup_api_key)
                binding.btnAction.setText(R.string.settings)
                binding.btnSetDefault.visibility = View.GONE
            }
            KeyboardSetupState.READY_FOR_USE -> {
                binding.textInstructions.setText(R.string.setup_ready)
                binding.btnAction.setText(R.string.btn_start_using)
                binding.btnSetDefault.visibility = View.VISIBLE
                binding.btnSetDefault.setText(R.string.btn_set_default)
            }
            KeyboardSetupState.SETUP_COMPLETE -> {
                binding.textInstructions.setText(R.string.setup_complete)
                binding.btnAction.setText(R.string.btn_start_using)
                binding.btnSetDefault.visibility = View.GONE
            }
        }
    }

    private fun handleSetupStateAction() {
        when (keyboardSetupManager.getKeyboardSetupState()) {
            KeyboardSetupState.NEEDS_ENABLING -> {
                startActivity(keyboardSetupManager.getKeyboardSettingsIntent())
            }
            KeyboardSetupState.NEEDS_PERMISSIONS -> {
                requestPermissions(systemManager.getRequiredPermissions(), PERMISSION_REQUEST_CODE)
            }
            KeyboardSetupState.NEEDS_API_KEY -> {
                startActivity(Intent(this, VaakSettingsActivity::class.java))
            }
            KeyboardSetupState.READY_FOR_USE, KeyboardSetupState.SETUP_COMPLETE -> {
                finish()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            updateSetupState()
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}
