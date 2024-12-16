package com.aman.vaak.handlers

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aman.vaak.R
import com.aman.vaak.databinding.ActivitySetupBinding
import com.aman.vaak.managers.KeyboardSetupManager
import com.aman.vaak.managers.SystemManager
import com.aman.vaak.models.KeyboardSetupState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VaakSetupActivity : AppCompatActivity() {

    @Inject
    lateinit var keyboardSetupManager: KeyboardSetupManager
    
    @Inject
    lateinit var systemManager: SystemManager
    
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

    private fun updateSetupState() {
        when (keyboardSetupManager.getKeyboardSetupState()) {
            KeyboardSetupState.NEEDS_ENABLING -> {
                binding.textInstructions.setText(R.string.enable_keyboard_instruction)
                binding.btnAction.setText(R.string.btn_enable_keyboard)
            }
            KeyboardSetupState.NEEDS_SELECTION -> {
                binding.textInstructions.setText(R.string.select_keyboard_instruction)
                binding.btnAction.setText(R.string.btn_select_keyboard)
            }
            KeyboardSetupState.NEEDS_PERMISSIONS -> {
                binding.textInstructions.setText(R.string.permissions_required)
                binding.btnAction.setText(R.string.btn_grant_permissions)
            }
            KeyboardSetupState.SETUP_COMPLETE -> {
                binding.textInstructions.setText(R.string.setup_complete)
                binding.btnAction.setText(R.string.btn_start_using)
            }
        }
    }

    private fun handleSetupStateAction() {
        when (keyboardSetupManager.getKeyboardSetupState()) {
            KeyboardSetupState.NEEDS_ENABLING -> {
                startActivity(keyboardSetupManager.getKeyboardSettingsIntent())
            }
            KeyboardSetupState.NEEDS_SELECTION -> {
                keyboardSetupManager.showKeyboardSelector()
            }
            KeyboardSetupState.NEEDS_PERMISSIONS -> {
                requestPermissions(
                    systemManager.getRequiredPermissions(),
                    PERMISSION_REQUEST_CODE
                )
            }
            KeyboardSetupState.SETUP_COMPLETE -> {
                // TODO: Navigate to next screen or finish setup
                finish()
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}
