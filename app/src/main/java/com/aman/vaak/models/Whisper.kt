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
    override val model: String = DEFAULT_CHAT_MODEL,
    override val baseEndpoint: String = DEFAULT_BASE_ENDPOINT,
    override val systemPrompt: String = DEFAULT_TRANSLATION_PROMPT,
) : BaseAIConfig(baseEndpoint, model, systemPrompt) {
    companion object {
        const val DEFAULT_CHAT_MODEL = "gpt-4o-mini"
        const val DEFAULT_BASE_ENDPOINT = "https://api.openai.com/v1"
        const val DEFAULT_TRANSLATION_PROMPT = """
            You are a translator. Translate all input text to {LANGUAGE}.
            Provide only the direct translation without any explanations or additional text.
            Maintain the original formatting and punctuation.
        """
    }
}

sealed class ValidationException(message: String) : Exception(message) {
    class InvalidTemperatureException :
        ValidationException("Temperature must be between 0 and 1")

    class InvalidModelException(model: String) :
        ValidationException("Invalid model specified: $model")

    class InvalidLanguageException(language: String) :
        ValidationException("Unsupported language code: $language")
}

data class WhisperConfig(
    override val model: String = DEFAULT_WHISPER_MODEL,
    override val baseEndpoint: String = DEFAULT_BASE_ENDPOINT,
    override val systemPrompt: String = DEFAULT_TRANSCRIPTION_PROMPT,
    val language: String? = DEFAULT_LANGUAGE,
    val temperature: Float = DEFAULT_TEMPERATURE,
    val responseFormat: WhisperResponseFormat = WhisperResponseFormat.JSON,
    val maxFileSize: Long = MAX_FILE_SIZE,
) : BaseAIConfig(baseEndpoint, model, systemPrompt) {
    val transcriptionEndpoint: String
        get() = "$baseEndpoint/audio/transcriptions"

    fun validate() {
        if (temperature !in 0.0f..1.0f) {
            throw ValidationException.InvalidTemperatureException()
        }
        // Other validations can be added here
    }

    companion object {
        const val DEFAULT_WHISPER_MODEL = "whisper-1"
        const val DEFAULT_BASE_ENDPOINT = "https://api.openai.com/v1"
        const val DEFAULT_LANGUAGE = "en"
        const val DEFAULT_TEMPERATURE = 0.2f
        const val MAX_FILE_SIZE = 25 * 1024 * 1024L // 25MB

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
