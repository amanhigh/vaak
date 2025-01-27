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

    companion object {
        private const val SLOW_WORD_DELETION_DELAY_MS = 200L
        private const val MEDIUM_WORD_DELETION_DELAY_MS = 100L
        private const val FAST_WORD_DELETION_DELAY_MS = 50L
        private const val FIRST_PHASE_DURATION_MS = 1000L
        private const val SECOND_PHASE_DURATION_MS = 3000L
    }

    private val phases =
        listOf(
            // Slow word deletion (first second)
            Phase(SLOW_WORD_DELETION_DELAY_MS) { textManager.deleteWord() },
            // Medium word deletion (second second)
            Phase(MEDIUM_WORD_DELETION_DELAY_MS) { textManager.deleteWord() },
            // Fast word deletion (after 3 seconds)
            Phase(FAST_WORD_DELETION_DELAY_MS) { textManager.deleteWord() },
        )

    private fun getPhaseForElapsedTime(elapsedMs: Long): Phase {
        val phaseIndex =
            when {
                // First second - character deletion
                elapsedMs < FIRST_PHASE_DURATION_MS -> 0
                // Second second - slow word deletion
                elapsedMs < SECOND_PHASE_DURATION_MS -> 1
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

        companion object {
            private const val MAX_TEXT_LENGTH_FOR_WORD_DELETION = 50
        }

        internal fun deleteWord(): Boolean {
            val ic = requireInputConnection()
            val text = ic.getTextBeforeCursor(MAX_TEXT_LENGTH_FOR_WORD_DELETION, 0) ?: return false
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
