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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.aman.vaak.R
import com.aman.vaak.managers.ClipboardManager
import com.aman.vaak.managers.DictationException
import com.aman.vaak.managers.DictationManager
import com.aman.vaak.managers.InputNotConnectedException
import com.aman.vaak.managers.KeyboardManager
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.PromptsManager
import com.aman.vaak.managers.SettingsManager
import com.aman.vaak.managers.TextManager
import com.aman.vaak.managers.TextOperationFailedException
import com.aman.vaak.managers.TranscriptionException
import com.aman.vaak.managers.TranslationException
import com.aman.vaak.managers.VaakFileException
import com.aman.vaak.managers.VoiceManager
import com.aman.vaak.managers.VoiceRecordingException
import com.aman.vaak.models.DictationState
import com.aman.vaak.models.DictationStatus
import com.aman.vaak.models.KeyboardState
import com.aman.vaak.models.Prompt
import com.aman.vaak.models.SupportedLanguage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class VaakInputMethodService : InputMethodService() {
    @Inject lateinit var clipboardManager: ClipboardManager

    @Inject lateinit var textManager: TextManager

    @Inject lateinit var promptsManager: PromptsManager

    @Inject lateinit var voiceManager: VoiceManager

    @Inject lateinit var dictationManager: DictationManager

    @Inject lateinit var notifyManager: NotifyManager

    @Inject lateinit var keyboardManager: KeyboardManager

    @Inject lateinit var settingsManager: SettingsManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateCollectionJob: Job? = null
    private var keyboardState: KeyboardState? = null
    private var keyboardView: View? = null

    override fun onCreate() {
        super.onCreate()
        observeDictationState()
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
                setupPasteButton()
                setupSwitchButton()
                findViewById<Button>(R.id.settingsButton).setOnClickListener { handleSettings() }
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
                setupLanguageButton()
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
        val numpadButtons =
            listOf(
                R.id.num1Button, R.id.num2Button, R.id.num3Button,
                R.id.num4Button, R.id.num5Button, R.id.num6Button,
                R.id.num7Button, R.id.num8Button, R.id.num9Button,
                R.id.num0Button,
            )

        numpadButtons.forEach { id ->
            keyboardView?.findViewById<Button>(id)?.setOnClickListener { button ->
                handleTextOperation { textManager.insertText((button as Button).text.toString()) }
            }
        }
    }

    private fun setupLanguageButton() {
        keyboardView?.findViewById<Button>(R.id.languageButton)?.apply {
            setOnClickListener { cycleLanguage() }
            updateLanguageButton()
        }
    }

    private fun cycleLanguage() {
        val currentLang = settingsManager.getTargetLanguage()
        val nextLang =
            when (currentLang) {
                SupportedLanguage.ENGLISH.code -> SupportedLanguage.HINDI
                SupportedLanguage.HINDI.code -> SupportedLanguage.PUNJABI
                else -> SupportedLanguage.ENGLISH
            }
        settingsManager.saveTargetLanguage(nextLang.code)
        updateLanguageButton()
    }

    private fun updateLanguageButton() {
        keyboardView?.findViewById<Button>(R.id.languageButton)?.apply {
            text =
                SupportedLanguage.values()
                    .first { it.code == settingsManager.getTargetLanguage() }
                    .display
        }
    }

    private fun showNumpad() {
        keyboardView?.findViewById<LinearLayout>(R.id.numpadRow)?.visibility = View.VISIBLE
    }

    private fun hideNumpad() {
        keyboardView?.findViewById<LinearLayout>(R.id.numpadRow)?.visibility = View.GONE
    }

    private fun setupPasteButton() {
        keyboardView?.findViewById<Button>(R.id.pasteButton)?.apply {
            setOnClickListener { handlePaste() }
            setOnLongClickListener {
                showPrompts()
                true
            }
        }
    }

    private fun showPrompts() {
        serviceScope.launch {
            try {
                val prompts = promptsManager.getPrompts()
                keyboardView?.post {
                    createPromptButtons(prompts)
                    keyboardView?.findViewById<LinearLayout>(R.id.promptsContainer)?.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun hidePrompts() {
        keyboardView?.findViewById<LinearLayout>(R.id.promptsContainer)?.apply {
            visibility = View.GONE
            // Remove all views except the hide button
            var i = childCount - 1
            while (i >= 0) {
                val child = getChildAt(i)
                if (child.id != R.id.hidePromptsButton) {
                    removeViewAt(i)
                }
                i--
            }
        }
    }

    private fun createPromptButtons(prompts: List<Prompt>) {
        keyboardView?.findViewById<LinearLayout>(R.id.promptsContainer)?.apply {
            // Clear existing prompt buttons
            var i = childCount - 1
            while (i >= 0) {
                val child = getChildAt(i)
                if (child.id != R.id.hidePromptsButton) {
                    removeViewAt(i)
                }
                i--
            }

            // Add new prompt buttons
            prompts.forEach { prompt ->
                val button =
                    Button(context).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                40.dpToPx(context),
                            )
                        text = prompt.name
                        setOnClickListener {
                            handlePromptSelection(prompt)
                        }
                    }
                // Add at the beginning to keep hide button at bottom
                addView(button, 0)
            }

            // Setup hide button
            findViewById<Button>(R.id.hidePromptsButton).setOnClickListener {
                hidePrompts()
            }
        }
    }

    private fun handlePromptSelection(prompt: Prompt) {
        try {
            textManager.insertText(prompt.content)
            hidePrompts()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun observeDictationState() {
        stateCollectionJob?.cancel()
        stateCollectionJob =
            serviceScope.launch {
                try {
                    dictationManager.watchDictationState()
                        .collect { state ->
                            updateUiState(state)
                        }
                } catch (e: Exception) {
                    handleError(e)
                    notifyManager.showError(
                        title = getString(R.string.error_dictation_state),
                        message = getString(R.string.error_dictation_retry),
                    )
                }
            }
    }

    private fun updateUiState(state: DictationState) {
        keyboardView?.post {
            try {
                updateTimerUI(state)
                updateRecordingRowVisibility(state.status)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun updateRecordingRowVisibility(status: DictationStatus) {
        keyboardView?.apply {
            val isRecording = status == DictationStatus.RECORDING

            findViewById<Button>(R.id.pushToTalkButton).visibility =
                if (isRecording) View.GONE else View.VISIBLE

            findViewById<Button>(R.id.cancelButton).visibility =
                if (isRecording) View.VISIBLE else View.GONE

            findViewById<Button>(R.id.completeDictationButton).visibility =
                if (isRecording) View.VISIBLE else View.GONE
        }
    }

    private fun updateTimerUI(state: DictationState) {
        keyboardView?.apply {
            val timerText = findViewById<TextView>(R.id.dictationTimerText)
            timerText.text =
                when (state.status) {
                    DictationStatus.TRANSCRIBING -> getString(R.string.timer_transcribing)
                    DictationStatus.TRANSLATING -> getString(R.string.status_translating)
                    DictationStatus.RECORDING -> formatTime(state.timeMillis)
                    DictationStatus.IDLE -> getString(R.string.timer_initial)
                }
        }
    }

    private fun formatTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return when {
            minutes < 1 -> getString(R.string.timer_format_green, minutes, seconds)
            minutes < 2 -> getString(R.string.timer_format_yellow, minutes, seconds)
            else -> getString(R.string.timer_format_red, minutes, seconds)
        }
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
        serviceScope.launch {
            keyboardView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
            dictationManager.startDictation()
                .onFailure { e -> handleError(e as Exception) }
        }
    }

    private fun handleCancelDictation() {
        serviceScope.launch {
            keyboardView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_RELEASE)
            dictationManager.cancelDictation()
                .onSuccess { showToast("❌") }
                .onFailure { e -> handleError(e as Exception) }
        }
    }

    private fun handleCompleteDictation() {
        serviceScope.launch {
            keyboardView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_RELEASE)
            dictationManager.completeDictation()
                .onSuccess { text ->
                    try {
                        textManager.insertText(text)
                        showToast("✓")
                    } catch (e: Exception) {
                        handleError(e)
                    }
                }
                .onFailure { e -> handleError(e as Exception) }
        }
    }

    private fun handleTextOperation(operation: () -> Unit) {
        try {
            operation()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    private fun handleSelectAll() {
        handleTextOperation { textManager.selectAll() }
    }

    private fun handleCopy() {
        handleTextOperation {
            if (clipboardManager.copySelectedText()) {
                showToast("✓")
            }
        }
    }

    private fun handlePaste() {
        handleTextOperation {
            if (clipboardManager.pasteText()) {
                showToast("✓")
            }
        }
    }

    private fun handleEnter() {
        handleTextOperation { textManager.insertNewLine() }
    }

    private fun handleBackspace() {
        try {
            if (!textManager.deleteSelection()) {
                textManager.deleteCharacter()
            }
        } catch (e: Exception) {
            handleError(e)
        }
    }

    private fun handleBackspaceLongPress() {
        textManager.startContinuousDelete()
    }

    private fun handleBackspaceRelease() {
        textManager.stopContinuousDelete()
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
        handleTextOperation { textManager.insertSpace() }
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
            textManager.attachInputConnection(currentInputConnection)
            clipboardManager.attachInputConnection(currentInputConnection)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    override fun onFinishInput() {
        keyboardState = null
        textManager.detachInputConnection()
        clipboardManager.detachInputConnection()
        super.onFinishInput()
    }

    override fun onDestroy() {
        // FIXME: #A On Switching to another keyboard and coming back Dictation doesn't work silently failing.
        stateCollectionJob?.cancel()
        serviceScope.cancel()
        dictationManager.release()
        super.onDestroy()
    }
}
