package com.aman.vaak.managers

import com.aallam.openai.api.audio.AudioResponseFormat
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aman.vaak.models.ChatRequest
import com.aman.vaak.models.Language
import com.aman.vaak.models.TranscriptionResult
import com.aman.vaak.models.TranscriptionSegment
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

        private fun validateConfiguration() {
            settingsManager.getWhisperConfig().validate()
        }

        private fun validateAudioFile(file: File) {
            val config = settingsManager.getWhisperConfig()
            fileManager.validateAudioFile(file, config.maxFileSize).getOrThrow()
        }

        private fun validateLanguage(language: String?) {
            language?.let {
                if (Language.values().none { lang -> lang.code == it.lowercase() }) {
                    throw com.aman.vaak.models.ValidationException.InvalidLanguageException(it)
                }
            }
        }

        private fun prepareTranscriptionRequest(
            file: File,
            language: String?,
        ): TranscriptionRequest {
            val config = settingsManager.getWhisperConfig()
            val audioSource = fileManager.createFileSource(file)

            return TranscriptionRequest(
                audio = audioSource,
                model = ModelId(config.model),
                prompt = config.systemPrompt,
                responseFormat = AudioResponseFormat.Json,
                temperature = config.temperature.toDouble(),
                language = language ?: config.language,
            )
        }

        private suspend fun executeTranscriptionRequest(request: TranscriptionRequest): TranscriptionResult =
            withContext(NonCancellable + Dispatchers.IO) {
                supervisorScope {
                    try {
                        val client = getOrCreateOpenAI()
                        val response = client.transcription(request)

                        TranscriptionResult(
                            text = response.text,
                            segments =
                                response.segments?.map { segment ->
                                    TranscriptionSegment(
                                        text = segment.text,
                                        start = segment.start.toFloat(),
                                        end = segment.end.toFloat(),
                                    )
                                } ?: emptyList(),
                        )
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

        private fun createChatMessages(request: ChatRequest): List<ChatMessage> {
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

            return messages
        }

        private fun buildChatRequest(
            request: ChatRequest,
            messages: List<ChatMessage>,
        ): ChatCompletionRequest {
            return ChatCompletionRequest(
                model = ModelId(request.model),
                messages = messages,
            )
        }

        private suspend fun executeChatRequest(
            client: OpenAI,
            chatRequest: ChatCompletionRequest,
        ): String {
            val response = client.chatCompletion(chatRequest)
            return response.choices.firstOrNull()?.message?.content
                ?: throw ChatCompletionException.EmptyResponseException()
        }

        private fun handleChatError(e: Exception): ChatCompletionException {
            release()
            return when (e) {
                is CancellationException ->
                    ChatCompletionException.CompletionFailedException(e.message ?: "Cancelled")
                is IOException ->
                    ChatCompletionException.NetworkException(e.message ?: "Network error")
                else ->
                    ChatCompletionException.CompletionFailedException("[${e.javaClass.simpleName}]: ${e.message}")
            }
        }

        override suspend fun chat(request: ChatRequest): Result<String> =
            runCatching {
                withContext(NonCancellable + Dispatchers.IO) {
                    supervisorScope {
                        try {
                            val client = getOrCreateOpenAI()
                            val messages = createChatMessages(request)
                            val chatRequest = buildChatRequest(request, messages)
                            executeChatRequest(client, chatRequest)
                        } catch (e: Exception) {
                            throw handleChatError(e)
                        }
                    }
                }
            }

        override fun release() {
            openAI?.close()
            openAI = null
        }
    }
