package com.aman.vaak.handlers

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.Toast
import com.aman.vaak.R
import com.aman.vaak.managers.ClipboardManager
import com.aman.vaak.managers.DictationException
import com.aman.vaak.managers.InputNotConnectedException
import com.aman.vaak.managers.KeyboardManager
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.TextOperationFailedException
import com.aman.vaak.managers.TranscriptionException
import com.aman.vaak.managers.TranslationException
import com.aman.vaak.managers.VaakFileException
import com.aman.vaak.managers.VoiceManager
import com.aman.vaak.managers.VoiceRecordingException
import com.aman.vaak.models.KeyboardState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

@AndroidEntryPoint
class VaakInputMethodService : InputMethodService() {
    // FIXME: Remove Unused Managers migrate logic to handlers they are part of if any.
    @Inject lateinit var clipboardManager: ClipboardManager

    @Inject lateinit var numpadHandler: NumpadHandler

    @Inject lateinit var promptsHandler: PromptsHandler

    @Inject lateinit var voiceManager: VoiceManager

    @Inject lateinit var dictationHandler: DictationHandler

    @Inject lateinit var notifyManager: NotifyManager

    @Inject lateinit var keyboardManager: KeyboardManager

    @Inject lateinit var settingsHandler: SettingsHandler

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var keyboardState: KeyboardState? = null
    private var keyboardView: View? = null

    override fun onCreate() {
        super.onCreate()
        startFloatingButton()
    }

    // FIXME: File Growing too Large Refactor and Break
    private fun startFloatingButton() {
        val intent = Intent(this, FloatingButtonService::class.java)
        startService(intent)
    }

    override fun onCreateInputView(): View {
        return try {
            layoutInflater.inflate(R.layout.keyboard, null).apply {
                keyboardView = this
                dictationHandler.startObservingState(this)
                promptsHandler.startObservingView(this)
                setupPasteButton()
                setupSwitchButton()
                findViewById<Button>(R.id.settingsButton).setOnClickListener { settingsHandler.launchSettings() }
                findViewById<Button>(R.id.selectAllButton).setOnClickListener { handleSelectAll() }
                findViewById<Button>(R.id.copyButton).setOnClickListener { handleCopy() }
                // FIXME: #B Improve Layout Bring Del Button Right.
                findViewById<Button>(R.id.enterButton).setOnClickListener { handleEnter() }
                findViewById<Button>(R.id.spaceButton).setOnClickListener { handleSpace() }
                findViewById<Button>(R.id.cancelButton).setOnClickListener { handleCancelDictation() }
                findViewById<Button>(R.id.completeDictationButton).setOnClickListener { handleCompleteDictation() }
                findViewById<Button>(R.id.hideNumpadButton).setOnClickListener { hideNumpad() }

                setupBackspaceButton()
                setupNumpadButtons()
                setupSpaceButton()
                setupDictateButton()
                settingsHandler.startObservingView(this, this@VaakInputMethodService)
            }
        } catch (e: Exception) {
            handleError(e)
            View(this)
        }
    }

    private fun setupSpaceButton() {
        keyboardView?.findViewById<Button>(R.id.spaceButton)?.apply {
            setOnClickListener { handleSpace() }
            setOnLongClickListener {
                showNumpad()
                true
            }
        }
    }

    private fun setupNumpadButtons() {
        keyboardView?.let { view ->
            numpadHandler.setupNumpadButtons(view)
        }
    }

    private fun showNumpad() {
        keyboardView?.let { view ->
            numpadHandler.showNumpad(view)
        }
    }

    private fun hideNumpad() {
        keyboardView?.let { view ->
            numpadHandler.hideNumpad(view)
        }
    }

    private fun setupPasteButton() {
        keyboardView?.findViewById<Button>(R.id.pasteButton)?.apply {
            setOnClickListener { handlePaste() }
            setOnLongClickListener {
                promptsHandler.showPrompts()
                true
            }
        }
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun handleError(error: Exception) {
        val errorTitle =
            when (error) {
                // Permission and Hardware Errors
                is SecurityException ->
                    getString(R.string.error_mic_permission)
                is VoiceRecordingException.HardwareInitializationException ->
                    getString(R.string.error_record_state)

                // Dictation State Errors
                is DictationException.AlreadyDictatingException ->
                    getString(R.string.error_already_dictating)
                is DictationException.NotDictatingException ->
                    getString(R.string.error_not_dictating)
                is DictationException.TranscriptionFailedException ->
                    getString(R.string.error_transcribe_failed)
                is InputNotConnectedException ->
                    getString(R.string.error_no_input)
                is TextOperationFailedException ->
                    getString(R.string.error_text_operation)

                // API Related Errors
                is TranscriptionException.InvalidApiKeyException ->
                    getString(R.string.error_invalid_api_key)
                is TranscriptionException.InvalidModelException ->
                    getString(R.string.error_invalid_model)
                is TranscriptionException.InvalidLanguageException ->
                    getString(R.string.error_invalid_language)
                is TranscriptionException.InvalidTemperatureException ->
                    getString(R.string.error_invalid_temperature)
                is TranscriptionException.NetworkException ->
                    getString(R.string.error_network_transcription)
                is TranscriptionException.TranscriptionFailedException ->
                    getString(R.string.error_transcription_failed)

                // File Related Errors
                is VaakFileException.FileNotFoundException ->
                    getString(R.string.error_file_not_found)
                is VaakFileException.InvalidFormatException ->
                    getString(R.string.error_file_invalid)
                is VaakFileException.EmptyFileException ->
                    getString(R.string.error_file_empty)
                is VaakFileException.FileTooLargeException ->
                    getString(R.string.error_file_too_large)

                // Translation Errors
                is TranslationException.EmptyTextException ->
                    getString(R.string.error_empty_text)
                is TranslationException.TranslationFailedException ->
                    getString(R.string.error_translation_failed)

                else ->
                    getString(R.string.error_unknown)
            }

        notifyManager.showError(title = errorTitle, message = error.message ?: "Details Unknown")
    }

    private fun isRecordingActive(): Boolean {
        return keyboardView?.findViewById<Button>(R.id.cancelButton)
            ?.visibility == View.VISIBLE
    }

    private fun handleStartDictation() {
        keyboardView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
        dictationHandler.handleStartDictation()
    }

    private fun handleCancelDictation() {
        keyboardView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_RELEASE)
        dictationHandler.handleCancelDictation()
    }

    private fun handleCompleteDictation() {
        keyboardView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_RELEASE)
        dictationHandler.handleCompleteDictation()
    }

    private fun handleTextOperation(operation: () -> Unit) {
        try {
            operation()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    @Inject
    lateinit var textHandler: TextHandler

    private fun handleSelectAll() {
        textHandler.handleSelectAll()
    }

    private fun handleCopy() {
        textHandler.handleCopy()
        showToast("📋")
    }

    private fun handlePaste() {
        textHandler.handlePaste()
        showToast("📄")
    }

    private fun handleEnter() {
        textHandler.handleEnter()
    }

    private fun handleBackspace() {
        textHandler.handleBackspace()
    }

    private fun handleBackspaceLongPress() {
        textHandler.handleBackspaceLongPress()
    }

    private fun handleBackspaceRelease() {
        textHandler.handleBackspaceRelease()
    }

    private fun setupBackspaceButton() {
        keyboardView?.findViewById<Button>(R.id.backspaceButton)?.apply {
            setOnClickListener { handleBackspace() }
            setOnLongClickListener {
                handleBackspaceLongPress()
                true
            }
            setOnTouchListener { _: View, event: MotionEvent ->
                if (event.action == MotionEvent.ACTION_UP ||
                    event.action == MotionEvent.ACTION_CANCEL
                ) {
                    handleBackspaceRelease()
                }
                false
            }
        }
    }

    private fun setupDictateButton() {
        keyboardView?.findViewById<Button>(R.id.pushToTalkButton)?.apply {
            setOnTouchListener { view, event -> handleRecordingTouch(view, event) }
        }
    }

    private fun handleRecordingTouch(
        view: View,
        event: MotionEvent,
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!isRecordingActive()) {
                    handleStartDictation()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isRecordingActive()) {
                    handleCompleteDictation()
                }
                return true
            }
        }
        return false
    }

    private fun handleSpace() {
        textHandler.handleSpace()
    }

    private fun handleSettings() {
        val intent =
            Intent(this, VaakSettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        startActivity(intent)
    }

    private fun setupSwitchButton() {
        keyboardView?.findViewById<Button>(R.id.switchKeyboardButton)?.apply {
            setOnClickListener {
                handlePreviousInputSwitch()
            }
            setOnLongClickListener {
                handleSwitchKeyboard()
                true
            }
        }
    }

    private fun handlePreviousInputSwitch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchToPreviousInputMethod()
        } else {
            // Fallback for older versions
            handleSwitchKeyboard()
        }
    }

    private fun handleSwitchKeyboard() {
        // TODO: #B Floating Button for Keyboard Switch between Last and VaaK.
        keyboardManager.showKeyboardSelector()
    }

    private fun showToast(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    override fun onStartInput(
        info: EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInput(info, restarting)
        keyboardState = KeyboardState(currentInputConnection, info)
        try {
            textHandler.attachInputConnection(currentInputConnection)
            clipboardManager.attachInputConnection(currentInputConnection)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    override fun onFinishInput() {
        keyboardState = null
        textHandler.detachInputConnection()
        clipboardManager.detachInputConnection()
        super.onFinishInput()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        dictationHandler.release()
        promptsHandler.release()
        settingsHandler.release()
        super.onDestroy()
    }
}
