package com.aman.vaak.models

data class DictationState(
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val timeMillis: Long = 0L,
    val isError: Boolean = false,
    val errorMessage: String? = null,
)
