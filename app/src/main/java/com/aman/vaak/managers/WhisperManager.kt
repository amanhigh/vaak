package com.aman.vaak.managers

import com.aallam.openai.api.audio.AudioResponseFormat
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aman.vaak.models.TranscriptionResult
import com.aman.vaak.models.WhisperConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider

sealed class TranscriptionException(message: String) : Exception(message) {
    class InvalidApiKeyException :
        TranscriptionException("Invalid or missing API key")

    class InvalidModelException(model: String) :
        TranscriptionException("Invalid model specified: $model")

    class InvalidLanguageException(language: String) :
        TranscriptionException("Unsupported language code: $language")

    class InvalidTemperatureException :
        TranscriptionException("Temperature must be between 0 and 1")

    class NetworkException(message: String) :
        TranscriptionException(message)

    class TranscriptionFailedException(message: String) :
        TranscriptionException(message)
}

sealed class TranslationException(message: String) : Exception(message) {
    class EmptyTextException :
        TranslationException("Text to translate is empty")

    class TranslationFailedException(message: String) :
        TranslationException("Translation failed: $message")
}

/**
* Manager interface for handling Whisper API transcription requests
*/
interface WhisperManager {
    /**
     * Returns current active configuration
     * @return Currently active WhisperConfig
     */
    fun getCurrentConfig(): WhisperConfig

    /**
     * Updates specific parameters of the configuration
     * Allows partial config updates without affecting other parameters
     * @param update Lambda that modifies current config
     */
    fun updateConfig(update: (WhisperConfig) -> WhisperConfig)

    /**
     * Transcribes audio data using Whisper API
     * Uses current active configuration
     * @param file Audio file to transcribe
     * @param language Target language code (ISO 639-1) - overrides config language if provided
     * @return Result containing TranscriptionResult on success, or specific TranscriptionException on failure
     */
    suspend fun transcribeAudio(
        file: File,
        language: String? = null,
    ): Result<TranscriptionResult>

    /**
     * Releases resources held by the manager
     * Should be called when manager is no longer needed
     */
    fun release()

    /**
     * Translates text using OpenAI's chat completion API
     * @param text The text to translate
     * @return Result containing translated text on success, or specific TranslationException on failure
     */
    suspend fun translateText(text: String): Result<String>
}

/**
* Implementation of WhisperManager that handles audio transcription using OpenAI's Whisper API
*/
class WhisperManagerImpl
    @Inject
    constructor(
        private val settingsManager: SettingsManager,
        private val fileManager: FileManager,
        private val openAIProvider: Provider<OpenAI>,
    ) : WhisperManager {
        private var openAI: OpenAI? = null

        private fun getOrCreateOpenAI(): OpenAI {
            if (openAI == null) {
                openAI = openAIProvider.get()
            }
            return openAI!!
        }

        companion object {
            const val MAX_FILE_SIZE: Long = 25 * 1024 * 1024 // 25MB

            // FIXME: Create Language Manaager for Translation etc ?
            private val SUPPORTED_LANGUAGES = setOf("en", "es", "fr", "de", "it", "pt", "nl", "ja", "ko", "zh")
            private val SUPPORTED_MODELS = setOf("whisper-1")
        }

        private var whisperConfig = initializeConfig()

        private fun initializeConfig(): WhisperConfig {
            // FIXME: Remove Settings Manager Injection if not required.
            val apiKey =
                settingsManager.getApiKey()
                    ?: throw TranscriptionException.InvalidApiKeyException()

            return WhisperConfig(
                apiKey = apiKey,
            )
        }

        override fun getCurrentConfig(): WhisperConfig = whisperConfig

        override fun updateConfig(update: (WhisperConfig) -> WhisperConfig) {
            whisperConfig = update(whisperConfig)
        }

        // FIXME: Move to Validation to Config Model File along with Related Exceptions.
        private fun validateConfiguration() {
            val config = getCurrentConfig()

            if (config.apiKey.isBlank()) {
                throw TranscriptionException.InvalidApiKeyException()
            }

            if (!SUPPORTED_MODELS.contains(config.model)) {
                throw TranscriptionException.InvalidModelException(config.model)
            }

            if (config.temperature !in 0.0f..1.0f) {
                throw TranscriptionException.InvalidTemperatureException()
            }
        }

        private fun validateAudioFile(file: File) {
            fileManager.validateAudioFile(file, MAX_FILE_SIZE.toLong()).getOrThrow()
        }

        private fun validateLanguage(language: String?) {
            language?.let {
                if (!SUPPORTED_LANGUAGES.contains(it.lowercase())) {
                    throw TranscriptionException.InvalidLanguageException(it)
                }
            }
        }

        private fun prepareTranscriptionRequest(
            file: File,
            language: String?,
        ): TranscriptionRequest {
            val audioSource = fileManager.createFileSource(file)

            return TranscriptionRequest(
                audio = audioSource,
                model = ModelId(whisperConfig.model),
                prompt = whisperConfig.prompt,
                responseFormat = AudioResponseFormat.Json,
                temperature = whisperConfig.temperature.toDouble(),
                language = language ?: whisperConfig.language,
            )
        }

        private suspend fun executeTranscriptionRequest(request: TranscriptionRequest): TranscriptionResult =
            withContext(NonCancellable + Dispatchers.IO) {
                supervisorScope {
                    try {
                        val client = getOrCreateOpenAI()
                        val response = client.transcription(request)
                        TranscriptionResult(text = response.text, duration = null)
                    } catch (e: Exception) {
                        // Close and recreate client on any error
                        release()
                        throw when (e) {
                            is CancellationException ->
                                TranscriptionException.TranscriptionFailedException(
                                    "Transcription cancelled: ${e.message}",
                                )
                            is IOException -> TranscriptionException.NetworkException("Network error: ${e.message}")
                            else -> TranscriptionException.TranscriptionFailedException("[${e.javaClass.simpleName}]: ${e.message}")
                        }
                    }
                }
            }

        override suspend fun transcribeAudio(
            file: File,
            language: String?,
        ): Result<TranscriptionResult> =
            runCatching {
                validateConfiguration()
                validateAudioFile(file)
                validateLanguage(language)
                val request = prepareTranscriptionRequest(file, language)
                executeTranscriptionRequest(request)
            }

        override suspend fun translateText(text: String): Result<String> =
            runCatching {
                if (text.isBlank()) {
                    throw TranslationException.EmptyTextException()
                }

                // FIXME: Refactor extract Chat Completion Method and use in Translate Method.
                // This will be useful later for other Logic like Summary of Spoken Text.
                withContext(NonCancellable + Dispatchers.IO) {
                    supervisorScope {
                        try {
                            val client = getOrCreateOpenAI()
                            val chatRequest =
                                ChatCompletionRequest(
                                    // FIXME: Move to Settings make configurable
                                    model = ModelId("gpt-4o-mini"),
                                    messages =
                                        listOf(
                                            ChatMessage(
                                                role = ChatRole.User,
                                                // FIXME: Refactor to better Handle Language Code and add more languages.
                                                content = "Translate to ${when (settingsManager.getTargetLanguage()) {
                                                    "HI" -> "Hindi"
                                                    "PA" -> "Punjabi"
                                                    else -> "English"
                                                }}: $text",
                                            ),
                                        ),
                                )

                            val response = client.chatCompletion(chatRequest)
                            response.choices.firstOrNull()?.message?.content
                                ?: throw TranslationException.TranslationFailedException("Empty response from translation service")
                        } catch (e: Exception) {
                            release()
                            throw when (e) {
                                is CancellationException ->
                                    TranslationException.TranslationFailedException("Translation cancelled: ${e.message}")
                                is IOException ->
                                    TranslationException.TranslationFailedException("Network error: ${e.message}")
                                else ->
                                    TranslationException.TranslationFailedException("[${e.javaClass.simpleName}]: ${e.message}")
                            }
                        }
                    }
                }
            }

        override fun release() {
            openAI?.close()
            openAI = null
        }
    }
