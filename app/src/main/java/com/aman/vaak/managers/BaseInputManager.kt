package com.aman.vaak.managers

import android.view.inputmethod.InputConnection

sealed class InputOperationException(message: String) : Exception(message)

class InputNotConnectedException : InputOperationException("No input connection available")

interface BaseInputManager {
    /**
     * Attaches an InputConnection for operations
     */
    fun attachInputConnection(inputConnection: InputConnection?)

    /**
     * Detaches current InputConnection
     */
    fun detachInputConnection()

    /**
     * Gets the current input connection or throws if not available
     */
    fun requireInputConnection(): InputConnection
}

open class BaseInputManagerImpl : BaseInputManager {
    protected var currentInputConnection: InputConnection? = null

    override fun attachInputConnection(inputConnection: InputConnection?) {
        currentInputConnection = inputConnection
    }

    override fun detachInputConnection() {
        currentInputConnection = null
    }

    override fun requireInputConnection(): InputConnection {
        return currentInputConnection ?: throw InputNotConnectedException()
    }
}
