package com.aman.vaak.models

/**
 * Configuration for Whisper API client
 * @property apiKey OpenAI API key for authentication
 * @property baseEndpoint Base URL for OpenAI API endpoints
 * @property model Whisper model to use for transcription
 * @property language Default language code for transcription (ISO 639-1)
 * @property prompt System prompt to guide transcription quality
 * @property temperature Sampling temperature between 0 and 1
 * @property responseFormat Format for API response
 */
enum class SupportedLanguage(val code: String, val display: String) {
    ENGLISH("EN", "EN"),
    HINDI("HI", "हि"),
    PUNJABI("PA", "ਪੰ"),
}

data class WhisperConfig(
    val apiKey: String,
    val baseEndpoint: String = "https://api.openai.com/v1",
    val model: String = "whisper-1",
    val language: String = "en",
    val prompt: String = "Please use proper capitalization and punctuation in the transcription.",
    val temperature: Float = 0.2f,
    val responseFormat: WhisperResponseFormat = WhisperResponseFormat.JSON,
) {
    val transcriptionEndpoint: String
        get() = "$baseEndpoint/audio/transcriptions"
}

/**
 * Response format options for Whisper API
 * JSON: Returns transcription with metadata
 * TEXT: Returns plain transcribed text
 */
enum class WhisperResponseFormat {
    JSON,
    TEXT,
}

data class TranscriptionResult(
    val text: String,
    val segments: List<TranscriptionSegment> = emptyList(),
) {
    /**
     * Returns text formatted with paragraphs based on segments.
     * If no segments available, returns original text.
     */
    fun getSegmentedText(): String {
        if (segments.isEmpty()) return text
        return segments.joinToString("\n\n") { it.text }
    }
}

data class TranscriptionSegment(
    val text: String,
    // Start time in seconds
    val start: Float,
    // End time in seconds
    val end: Float,
)

data class ChatRequest(
    val model: String = "",
    val systemPrompt: String? = null,
    val message: String = "",
)
