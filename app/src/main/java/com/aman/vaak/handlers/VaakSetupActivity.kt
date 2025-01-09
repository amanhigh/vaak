package com.aman.vaak.handlers

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.aman.vaak.R
import com.aman.vaak.databinding.ActivitySetupBinding
import com.aman.vaak.managers.KeyboardManager
import com.aman.vaak.managers.SettingsManager
import com.aman.vaak.managers.SystemManager
import com.aman.vaak.models.VaakSetupState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VaakSetupActivity : AppCompatActivity() {
    @Inject lateinit var keyboardManager: KeyboardManager

    @Inject lateinit var systemManager: SystemManager

    @Inject lateinit var settingsManager: SettingsManager

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

    private fun getKeyboardSetupState(): VaakSetupState =
        when {
            !keyboardManager.isKeyboardEnabled() -> VaakSetupState.NEEDS_ENABLING
            !systemManager.hasRequiredPermissions() -> VaakSetupState.NEEDS_PERMISSIONS
            !(settingsManager.getApiKey()?.isNotEmpty() ?: false) -> VaakSetupState.NEEDS_API_KEY
            else ->
                if (keyboardManager.isKeyboardSelected()) {
                    VaakSetupState.SETUP_COMPLETE
                } else {
                    VaakSetupState.READY_FOR_USE
                }
        }

    private fun updateUIForNeedsEnabling() {
        binding.textInstructions.setText(R.string.setup_enable_keyboard)
        binding.btnAction.setText(R.string.btn_enable)
        binding.btnSetDefault.visibility = View.GONE
    }

    private fun updateUIForNeedsPermissions() {
        binding.textInstructions.setText(R.string.setup_permissions)
        binding.btnAction.setText(R.string.btn_grant)
        binding.btnSetDefault.visibility = View.GONE
    }

    private fun updateUIForNeedsOverlayPermission() {
        binding.textInstructions.setText(R.string.setup_overlay)
        binding.btnAction.setText(R.string.btn_grant)
        binding.btnSetDefault.visibility = View.GONE
    }

    private fun updateUIForNeedsApiKey() {
        binding.textInstructions.setText(R.string.setup_api_key)
        binding.btnAction.setText(R.string.btn_settings)
        binding.btnSetDefault.visibility = View.GONE
    }

    private fun updateUIForReadyForUse() {
        binding.textInstructions.setText(R.string.setup_ready)
        binding.btnAction.setText(R.string.btn_start)
        binding.btnSetDefault.visibility = View.VISIBLE
        binding.btnSetDefault.setText(R.string.btn_set_default)
    }

    private fun updateUIForSetupComplete() {
        binding.textInstructions.setText(R.string.setup_complete)
        binding.btnAction.setText(R.string.btn_start)
        binding.btnSetDefault.visibility = View.GONE
    }

    private fun updateSetupState() {
        when (getKeyboardSetupState()) {
            VaakSetupState.NEEDS_ENABLING -> updateUIForNeedsEnabling()
            VaakSetupState.NEEDS_PERMISSIONS -> updateUIForNeedsPermissions()
            VaakSetupState.NEEDS_OVERLAY_PERMISSION -> updateUIForNeedsOverlayPermission()
            VaakSetupState.NEEDS_API_KEY -> updateUIForNeedsApiKey()
            VaakSetupState.READY_FOR_USE -> updateUIForReadyForUse()
            VaakSetupState.SETUP_COMPLETE -> updateUIForSetupComplete()
        }
    }

    private fun handleSetupStateAction() {
        when (getKeyboardSetupState()) {
            VaakSetupState.NEEDS_ENABLING -> {
                startActivity(keyboardManager.getKeyboardSettingsIntent())
            }
            VaakSetupState.NEEDS_PERMISSIONS -> {
                requestPermissions(systemManager.getRequiredPermissions(), PERMISSION_REQUEST_CODE)
            }
            VaakSetupState.NEEDS_OVERLAY_PERMISSION -> {
                startActivity(systemManager.getOverlaySettingsIntent())
            }
            VaakSetupState.NEEDS_API_KEY -> {
                startActivity(Intent(this, VaakSettingsActivity::class.java))
            }
            VaakSetupState.READY_FOR_USE, VaakSetupState.SETUP_COMPLETE -> {
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
