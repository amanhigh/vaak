package com.aman.vaak.managers

import com.aman.vaak.models.ChatRequest
import javax.inject.Inject

sealed class TranslationException(message: String) : Exception(message) {
    class EmptyTextException :
        TranslationException("Text to translate is empty")

    class TranslationFailedException(message: String) :
        TranslationException("Translation failed: $message")
}

interface TranslateManager {
    suspend fun translateText(text: String): Result<String>
}

class TranslateManagerImpl
    @Inject
    constructor(
        private val whisperManager: WhisperManager,
        private val settingsManager: SettingsManager,
    ) : TranslateManager {
        override suspend fun translateText(text: String): Result<String> =
            runCatching {
                if (text.isBlank()) {
                    throw TranslationException.EmptyTextException()
                }

                val targetLanguage = settingsManager.getTargetLanguage()
                val chatConfig = settingsManager.getChatConfig()
                val systemPrompt =
                    chatConfig.systemPrompt.replace(
                        "{LANGUAGE}",
                        targetLanguage?.englishName ?: "English",
                    )

                val request =
                    ChatRequest(
                        model = chatConfig.model,
                        systemPrompt = systemPrompt,
                        message = text,
                    )

                whisperManager.chat(request).getOrElse { e ->
                    throw when (e) {
                        is ChatCompletionException.EmptyResponseException ->
                            TranslationException.TranslationFailedException("Empty response")
                        is ChatCompletionException.NetworkException ->
                            TranslationException.TranslationFailedException("Network error: ${e.message}")
                        else -> e
                    }
                }
            }
    }
