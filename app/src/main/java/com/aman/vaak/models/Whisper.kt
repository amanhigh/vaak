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
data class WhisperConfig(
    val apiKey: String,
    val baseEndpoint: String = "https://api.openai.com/v1",
    val model: String = "whisper-1",
    val language: String = "en",
    val prompt: String = "Please use proper capitalization and punctuation in the transcription.",
    val temperature: Float = 0.2f,
    val responseFormat: WhisperResponseFormat = WhisperResponseFormat.JSON
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
    TEXT;
}

/**
 * Represents the result of a Whisper API transcription request
 */
sealed class WhisperResult {
    /**
     * Successful transcription result
     * @property text Transcribed text
     * @property language Detected or specified language code
     * @property duration Duration of the audio in seconds
     */
    data class Success(
        val text: String,
        val language: String? = null,
        val duration: Float? = null
    ) : WhisperResult()
    
    /**
     * Error results from Whisper API requests
     */
    sealed class Error : WhisperResult() {
        /**
         * API-specific errors with HTTP status codes
         * @property code HTTP status code
         * @property message Error message from API
         */
        data class ApiError(
            val code: Int,
            val message: String
        ) : Error()
        
        /**
         * Network-related errors during API communication
         * @property message Error description
         * @property cause Original exception that caused the error
         */
        data class NetworkError(
            val message: String,
            val cause: Throwable? = null
        ) : Error()
        
        /**
         * Configuration-related errors
         * @property message Description of the configuration error
         */
        data class ConfigurationError(
            val message: String
        ) : Error()

        /**
         * File-related errors for audio input
         * @property message Description of the file error
         */
        data class FileError(
            val message: String
        ) : Error()
    }
}
