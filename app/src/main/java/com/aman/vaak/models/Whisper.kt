package com.aman.vaak.models

enum class SupportedLanguage(val code: String, val display: String) {
    ENGLISH("EN", "EN"),
    HINDI("HI", "हि"),
    PUNJABI("PA", "ਪੰ"),
}

abstract class BaseAIConfig(
    open val baseEndpoint: String = "https://api.openai.com/v1",
    open val model: String,
    open val systemPrompt: String,
)

data class ChatConfig(
    override val model: String,
    override val baseEndpoint: String = "https://api.openai.com/v1",
    override val systemPrompt: String = DEFAULT_TRANSLATION_PROMPT,
) : BaseAIConfig(baseEndpoint, model, systemPrompt) {
    companion object {
        const val DEFAULT_TRANSLATION_PROMPT = """
            You are a translator. Translate all input text to {LANGUAGE}.
            Provide only the direct translation without any explanations or additional text.
            Maintain the original formatting and punctuation.
        """
    }
}

data class WhisperConfig(
    override val model: String = "whisper-1",
    override val baseEndpoint: String = "https://api.openai.com/v1",
    override val systemPrompt: String = DEFAULT_TRANSCRIPTION_PROMPT,
    val language: String = "en",
    val temperature: Float = 0.2f,
    val responseFormat: WhisperResponseFormat = WhisperResponseFormat.JSON,
    val maxFileSize: Long = 25 * 1024 * 1024,
) : BaseAIConfig(baseEndpoint, model, systemPrompt) {
    val transcriptionEndpoint: String
        get() = "$baseEndpoint/audio/transcriptions"

    companion object {
        const val DEFAULT_TRANSCRIPTION_PROMPT = """
            Please use proper capitalization and punctuation in the transcription.
        """
    }
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
    val model: String,
    val systemPrompt: String? = null,
    val message: String,
)
