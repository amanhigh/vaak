package com.aman.vaak.handlers

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import com.aman.vaak.R
import com.aman.vaak.managers.InputNotConnectedException
import com.aman.vaak.managers.NotifyManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

@AndroidEntryPoint
class VaakInputMethodService : InputMethodService() {
    @Inject lateinit var numpadHandler: NumpadHandler

    @Inject lateinit var promptKeyHandler: PromptKeyHandler

    @Inject lateinit var dictationHandler: DictationHandler

    @Inject lateinit var notifyManager: NotifyManager

    @Inject lateinit var keyboardSwitchHandler: KeyboardSwitchHandler

    @Inject lateinit var settingsHandler: SettingsHandler

    @Inject lateinit var textHandler: TextHandler

    @Inject lateinit var languageHandler: LanguageHandler

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var keyboardView: View? = null

    override fun onCreate() {
        super.onCreate()
    }

    // TODO: #B Add Logo for Application

    // TODO: Add Developer Guide
    override fun onCreateInputView(): View {
        return try {
            layoutInflater.inflate(R.layout.keyboard, null).apply {
                keyboardView = this

                // Attach all handlers
                dictationHandler.attachView(this)
                numpadHandler.attachView(this)
                promptKeyHandler.attachView(this)
                settingsHandler.attachView(this)
                textHandler.attachView(this)
                keyboardSwitchHandler.also { handler ->
                    handler.attachView(this)
                    handler.attachIME(this@VaakInputMethodService)
                }
                languageHandler.attachView(this)
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
        promptKeyHandler.detachView()
        settingsHandler.detachView()
        textHandler.detachView()
        keyboardSwitchHandler.detachView()
        languageHandler.detachView()

        super.onDestroy()
    }

    private fun handleError(error: Exception) {
        val errorTitle =
            when (error) {
                is InputNotConnectedException ->
                    getString(R.string.error_keyboard_connection)
                else ->
                    getString(R.string.error_generic)
            }

        notifyManager.showError(title = errorTitle, message = error.message ?: "Details Unknown")
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
