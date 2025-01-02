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
        private companion object {
            const val DEFAULT_TRANSLATION_PROMPT = """
            You are a translator. Translate all input text to {LANGUAGE}.
            Provide only the direct translation without any explanations or additional text.
            Maintain the original formatting and punctuation.
        """
        }

        override suspend fun translateText(text: String): Result<String> =
            runCatching {
                if (text.isBlank()) {
                    throw TranslationException.EmptyTextException()
                }

                val targetLanguage =
                    when (settingsManager.getTargetLanguage()) {
                        "HI" -> "Hindi"
                        "PA" -> "Punjabi"
                        else -> "English"
                    }

                val systemPrompt = DEFAULT_TRANSLATION_PROMPT.replace("{LANGUAGE}", targetLanguage)

                val request =
                    ChatRequest(
                        model = "gpt-4o-mini",
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
