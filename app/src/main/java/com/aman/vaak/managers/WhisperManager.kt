package com.aman.vaak.managers

import com.aallam.openai.api.audio.AudioResponseFormat
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aman.vaak.models.ChatRequest
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

sealed class ChatCompletionException(message: String) : Exception(message) {
    class EmptyResponseException :
        ChatCompletionException("Empty response from chat completion")

    class NetworkException(cause: String) :
        ChatCompletionException("Chat network error: $cause")

    class CompletionFailedException(cause: String) :
        ChatCompletionException("Chat completion failed: $cause")
}

/**
* Manager interface for handling Whisper API transcription requests
*/
interface WhisperManager {
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
     * Executes a chat completion request
     * @param request The chat request containing model, prompt and message
     * @return Result containing response text or error
     */
    suspend fun chat(request: ChatRequest): Result<String>

    /**
     * Releases resources held by the manager
     * Should be called when manager is no longer needed
     */
    fun release()
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

            // FIXME: Move below to settings.
            private val SUPPORTED_LANGUAGES = setOf("en", "es", "fr", "de", "it", "pt", "nl", "ja", "ko", "zh")
            private val SUPPORTED_MODELS = setOf("whisper-1")
        }

        private var whisperConfig = initializeConfig()

        private fun initializeConfig(): WhisperConfig {
            // FIXME: Move Get Whisper Config to Settings
            val apiKey =
                settingsManager.getApiKey()
                    ?: throw TranscriptionException.InvalidApiKeyException()

            return WhisperConfig(
                apiKey = apiKey,
            )
        }

        // FIXME: Move to Validation to Config Model File along with Related Exceptions.
        private fun validateConfiguration() {
            val config = whisperConfig

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

        override suspend fun chat(request: ChatRequest): Result<String> =
            runCatching {
                withContext(NonCancellable + Dispatchers.IO) {
                    supervisorScope {
                        try {
                            val client = getOrCreateOpenAI()
                            val messages = mutableListOf<ChatMessage>()

                            request.systemPrompt?.let {
                                messages.add(
                                    ChatMessage(
                                        role = ChatRole.System,
                                        content = it,
                                    ),
                                )
                            }

                            messages.add(
                                ChatMessage(
                                    role = ChatRole.User,
                                    content = request.message,
                                ),
                            )

                            val chatRequest =
                                ChatCompletionRequest(
                                    model = ModelId(request.model),
                                    messages = messages,
                                )

                            val response = client.chatCompletion(chatRequest)
                            response.choices.firstOrNull()?.message?.content
                                ?: throw ChatCompletionException.EmptyResponseException()
                        } catch (e: Exception) {
                            release()
                            throw when (e) {
                                is CancellationException ->
                                    ChatCompletionException.CompletionFailedException(e.message ?: "Cancelled")
                                is IOException ->
                                    ChatCompletionException.NetworkException(e.message ?: "Network error")
                                else ->
                                    ChatCompletionException.CompletionFailedException("[${e.javaClass.simpleName}]: ${e.message}")
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
