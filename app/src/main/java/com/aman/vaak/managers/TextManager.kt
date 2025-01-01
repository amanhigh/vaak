package com.aman.vaak.managers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TextOperationException(message: String) : InputOperationException(message)

class TextOperationFailedException(operation: String) :
    TextOperationException("Failed to perform text operation: $operation")

interface TextManager : BaseInputManager {
    fun insertText(text: String)

    fun deleteCharacter(count: Int = 1): Boolean

    fun deleteSelection(): Boolean

    fun insertSpace()

    fun insertNewLine()

    fun selectAll()

    fun startContinuousDelete()

    fun stopContinuousDelete()
}

private class DeleteJob(private val textManager: TextManagerImpl) {
    private var job: Job? = null
    private var startTime: Long = 0L

    private data class Phase(
        val delay: Long,
        val action: () -> Boolean,
    )

    private val phases =
        listOf(
            // Medium character deletion (first second)
            Phase(100L) { textManager.deleteCharacter() },
            // Slow word deletion (second second)
            Phase(200L) { textManager.deleteWord() },
            // Fast word deletion (after 2 seconds)
            Phase(50L) { textManager.deleteWord() },
        )

    private fun getPhaseForElapsedTime(elapsedMs: Long): Phase {
        val phaseIndex =
            when {
                // First second - character deletion
                elapsedMs < 1000 -> 0
                // Second second - slow word deletion
                elapsedMs < 2000 -> 1
                // After 2 seconds - fast word deletion
                else -> 2
            }
        return phases[phaseIndex]
    }

    fun start(scope: CoroutineScope) {
        stop()
        startTime = System.currentTimeMillis()
        job =
            scope.launch {
                while (isActive) {
                    val phase = getPhaseForElapsedTime(System.currentTimeMillis() - startTime)
                    phase.action()
                    delay(phase.delay)
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
        startTime = 0L
    }
}

class TextManagerImpl
    @Inject
    constructor(
        private val scope: CoroutineScope,
    ) : BaseInputManagerImpl(), TextManager {
        private val deletionHandler = DeleteJob(this)

        override fun detachInputConnection() {
            stopContinuousDelete()
            super.detachInputConnection()
        }

        override fun insertText(text: String) {
            requireInputConnection().commitText(text, 1).takeIf { it }
                ?: throw TextOperationFailedException("Insert text")
        }

        override fun deleteCharacter(count: Int) = requireInputConnection().deleteSurroundingText(count, 0)

        override fun deleteSelection() =
            requireInputConnection().let { ic ->
                ic.getSelectedText(0)?.takeIf { it.isNotEmpty() }?.let {
                    ic.commitText("", 1)
                } ?: false
            }

        internal fun deleteWord(): Boolean {
            val ic = requireInputConnection()
            val text = ic.getTextBeforeCursor(50, 0) ?: return false
            val deleteLength = text.lastIndexOf(' ').let { if (it >= 0) text.length - it else text.length }
            return ic.deleteSurroundingText(deleteLength, 0)
        }

        override fun insertSpace() = insertText(" ")

        override fun insertNewLine() = insertText("\n")

        override fun selectAll() {
            requireInputConnection().performContextMenuAction(android.R.id.selectAll)
        }

        override fun startContinuousDelete() = deletionHandler.start(scope)

        override fun stopContinuousDelete() = deletionHandler.stop()
    }
