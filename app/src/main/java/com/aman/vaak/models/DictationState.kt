package com.aman.vaak.models

/**
 * Represents the Voice Dictation State Machine
 *
 * State Transitions:
 * IDLE -> RECORDING -> TRANSCRIBING -> IDLE
 *   \                     |
 *    \                   |
 *     -----> ERROR <-----
 *
 * States:
 * - IDLE: Initial state, ready to start recording
 * - RECORDING: Actively recording audio, timer active
 * - TRANSCRIBING: Processing recorded audio
 * - ERROR: Error occurred during any state
 *
 * Time Tracking:
 * - timeMillis only relevant during RECORDING state
 * - reset to 0 in all other states
 *
 * Error Handling:
 * - error field populated only in ERROR state
 * - null in all other states
 */
enum class DictationStatus {
    IDLE,
    RECORDING,
    TRANSCRIBING,
    ERROR,
}

data class DictationState(
    val status: DictationStatus = DictationStatus.IDLE,
    val timeMillis: Long = 0L,
    val error: Exception? = null,
)
