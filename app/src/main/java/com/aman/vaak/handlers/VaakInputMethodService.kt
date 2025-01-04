package com.aman.vaak.handlers

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.Toast
import com.aman.vaak.R
import com.aman.vaak.managers.InputNotConnectedException
import com.aman.vaak.managers.NotifyManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class VaakInputMethodService : InputMethodService() {
    @Inject lateinit var numpadHandler: NumpadHandler

    @Inject lateinit var promptsHandler: PromptsHandler

    @Inject lateinit var dictationHandler: DictationHandler

    @Inject lateinit var notifyManager: NotifyManager

    @Inject lateinit var keyboardSwitchHandler: KeyboardSwitchHandler

    @Inject lateinit var settingsHandler: SettingsHandler

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var keyboardView: View? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onCreateInputView(): View {
        return try {
            layoutInflater.inflate(R.layout.keyboard, null).apply {
                keyboardView = this

                // Attach all handlers
                dictationHandler.attachView(this)
                numpadHandler.attachView(this)
                promptsHandler.attachView(this)
                settingsHandler.attachView(this)

                // Setup other buttons and listeners
                // FIXME: #A Move Setup of Buttons to respective handlers on onViewAttached
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

                setupBackspaceButton()
                setupSpaceButton()
                setupDictateButton()
            }
        } catch (e: Exception) {
            handleError(e)
            View(this)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()

        // Detach all handlers
        dictationHandler.detachView()
        numpadHandler.detachView()
        promptsHandler.detachView()
        settingsHandler.detachView()

        super.onDestroy()
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

    private fun showNumpad() {
        numpadHandler.showNumpad()
    }

    private fun hideNumpad() {
        numpadHandler.hideNumpad()
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

    private fun handleError(error: Exception) {
        val errorTitle =
            when (error) {
                is InputNotConnectedException ->
                    getString(R.string.error_no_input)
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

    // Thread-safe flag to track recording start phase
    // This prevents race conditions where UP event processes before recording is fully established
    private var isStartingRecording = AtomicBoolean(false)

    // Brief delay to prevent accidental completions from quick touch events
    // This handles cases where DOWN and UP events fire almost simultaneously
    private val startDebounceTime = 300L // milliseconds

    /**
     * Handles touch events for recording button with proper synchronization to prevent:
     * 1. Accidental quick completions when user intends long press
     * 2. Race conditions between start and complete operations
     * 3. Invalid states from parallel event processing
     */
    private fun handleRecordingTouch(
        view: View,
        event: MotionEvent,
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Prevent multiple recording starts
                if (isRecordingActive()) {
                    return true
                }

                // Mark start phase to prevent premature completion
                isStartingRecording.set(true)

                serviceScope.launch {
                    try {
                        // Start recording and wait briefly to debounce
                        handleStartDictation()
                        delay(startDebounceTime)
                    } catch (e: Exception) {
                        handleError(e)
                    } finally {
                        // Always clear starting flag even if start fails
                        isStartingRecording.set(false)
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Only complete if:
                // 1. Not in starting phase (prevents premature completion)
                // 2. Actually recording (prevents invalid state)
                if (!isStartingRecording.get() && isRecordingActive()) {
                    serviceScope.launch {
                        handleCompleteDictation()
                    }
                }
                return true
            }
        }
        return false
    }

    private fun handleSpace() {
        textHandler.handleSpace()
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
            keyboardSwitchHandler.handleSwitchKeyboard()
        }
    }

    private fun handleSwitchKeyboard() {
        // TODO: Floating Button for Keyboard Switch between Last and VaaK.
        keyboardSwitchHandler.handleSwitchKeyboard()
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
        try {
            textHandler.attachInputConnection(currentInputConnection)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    override fun onFinishInput() {
        textHandler.detachInputConnection()
        super.onFinishInput()
    }
}
