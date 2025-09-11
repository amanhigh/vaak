package com.aman.vaak.managers

import com.aman.vaak.models.ChatRequest
import com.aman.vaak.models.Language
import com.aman.vaak.models.TranslationConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockitoExtension::class)
class TranslateManagerTest {
    @Mock
    private lateinit var whisperManager: WhisperManager

    @Mock
    private lateinit var settingsManager: SettingsManager

    private lateinit var translateManager: TranslateManager

    @BeforeEach
    fun setup() {
        translateManager = TranslateManagerImpl(whisperManager, settingsManager)
    }

    @Nested
    @DisplayName("Input Validation")
    inner class InputValidation {
        @Test
        fun `translateText fails with empty text`() =
            TestScope(StandardTestDispatcher()).runTest {
                val result = translateManager.translateText("")

                assertTrue(result.isFailure)
                val exception = result.exceptionOrNull()
                assertTrue(exception is TranslationException.EmptyTextException)
                assertEquals("Text to translate is empty", exception?.message)
            }

        @Test
        fun `translateText fails with blank text`() =
            TestScope(StandardTestDispatcher()).runTest {
                val result = translateManager.translateText("   ")

                assertTrue(result.isFailure)
                val exception = result.exceptionOrNull()
                assertTrue(exception is TranslationException.EmptyTextException)
                assertEquals("Text to translate is empty", exception?.message)
            }

        @Test
        fun `translateText processes valid text`() =
            TestScope(StandardTestDispatcher()).runTest {
                val targetLanguage = Language.SPANISH
                val translationConfig =
                    TranslationConfig(
                        model = "gpt-4",
                        systemPrompt = "Translate to {LANGUAGE}",
                    )

                whenever(settingsManager.getTargetLanguage()).thenReturn(targetLanguage)
                whenever(settingsManager.getTranslationConfig()).thenReturn(translationConfig)
                whenever(whisperManager.chat(any())).thenReturn(Result.success("Hola mundo"))

                val result = translateManager.translateText("Hello world")

                assertTrue(result.isSuccess)
                assertEquals("Hola mundo", result.getOrNull())
            }
    }

    @Nested
    @DisplayName("Configuration Setup")
    inner class ConfigurationSetup {
        @Test
        fun `translateText retrieves target language from settings`() =
            TestScope(StandardTestDispatcher()).runTest {
                val targetLanguage = Language.FRENCH
                val translationConfig = TranslationConfig()

                whenever(settingsManager.getTargetLanguage()).thenReturn(targetLanguage)
                whenever(settingsManager.getTranslationConfig()).thenReturn(translationConfig)
                whenever(whisperManager.chat(any())).thenReturn(Result.success("Bonjour"))

                translateManager.translateText("Hello")

                verify(settingsManager).getTargetLanguage()
                verify(settingsManager).getTranslationConfig()
            }

        @Test
        fun `translateText uses English as default when target language is null`() =
            TestScope(StandardTestDispatcher()).runTest {
                val translationConfig =
                    TranslationConfig(
                        systemPrompt = "Translate to {LANGUAGE}",
                    )

                whenever(settingsManager.getTargetLanguage()).thenReturn(null)
                whenever(settingsManager.getTranslationConfig()).thenReturn(translationConfig)
                whenever(whisperManager.chat(any())).thenReturn(Result.success("Hello"))

                translateManager.translateText("Hello")

                verify(whisperManager).chat(
                    ChatRequest(
                        model = translationConfig.model,
                        systemPrompt = "Translate to English",
                        message = "Hello",
                    ),
                )
            }

        @Test
        fun `translateText replaces language placeholder in system prompt`() =
            TestScope(StandardTestDispatcher()).runTest {
                val targetLanguage = Language.GERMAN
                val translationConfig =
                    TranslationConfig(
                        model = "gpt-3.5-turbo",
                        systemPrompt = "Please translate the following to {LANGUAGE}: ",
                    )

                whenever(settingsManager.getTargetLanguage()).thenReturn(targetLanguage)
                whenever(settingsManager.getTranslationConfig()).thenReturn(translationConfig)
                whenever(whisperManager.chat(any())).thenReturn(Result.success("Hallo"))

                translateManager.translateText("Hello")

                verify(whisperManager).chat(
                    ChatRequest(
                        model = "gpt-3.5-turbo",
                        systemPrompt = "Please translate the following to German: ",
                        message = "Hello",
                    ),
                )
            }

        @Test
        fun `translateText creates correct ChatRequest`() =
            TestScope(StandardTestDispatcher()).runTest {
                val targetLanguage = Language.JAPANESE
                val translationConfig =
                    TranslationConfig(
                        model = "gpt-4-turbo",
                        systemPrompt = "Translate to {LANGUAGE}",
                    )
                val inputText = "Good morning"

                whenever(settingsManager.getTargetLanguage()).thenReturn(targetLanguage)
                whenever(settingsManager.getTranslationConfig()).thenReturn(translationConfig)
                whenever(whisperManager.chat(any())).thenReturn(Result.success("おはようございます"))

                translateManager.translateText(inputText)

                verify(whisperManager).chat(
                    ChatRequest(
                        model = "gpt-4-turbo",
                        systemPrompt = "Translate to Japanese",
                        message = inputText,
                    ),
                )
            }
    }

    @Nested
    @DisplayName("WhisperManager Integration")
    inner class WhisperManagerIntegration {
        @Test
        fun `translateText returns successful translation from WhisperManager`() =
            TestScope(StandardTestDispatcher()).runTest {
                val expectedTranslation = "Buenos días"
                val translationConfig = TranslationConfig()

                whenever(settingsManager.getTargetLanguage()).thenReturn(Language.SPANISH)
                whenever(settingsManager.getTranslationConfig()).thenReturn(translationConfig)
                whenever(whisperManager.chat(any())).thenReturn(Result.success(expectedTranslation))

                val result = translateManager.translateText("Good morning")

                assertTrue(result.isSuccess)
                assertEquals(expectedTranslation, result.getOrNull())
            }

        @Test
        fun `translateText calls whisperManager chat with correct parameters`() =
            TestScope(StandardTestDispatcher()).runTest {
                val translationConfig =
                    TranslationConfig(
                        model = "gpt-4",
                        systemPrompt = "Translate to {LANGUAGE}",
                    )

                whenever(settingsManager.getTargetLanguage()).thenReturn(Language.ITALIAN)
                whenever(settingsManager.getTranslationConfig()).thenReturn(translationConfig)
                whenever(whisperManager.chat(any())).thenReturn(Result.success("Ciao"))

                translateManager.translateText("Hello")

                verify(whisperManager).chat(any())
            }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {
        @Test
        fun `translateText maps EmptyResponseException to TranslationFailedException`() =
            TestScope(StandardTestDispatcher()).runTest {
                val translationConfig = TranslationConfig()

                whenever(settingsManager.getTargetLanguage()).thenReturn(Language.ENGLISH)
                whenever(settingsManager.getTranslationConfig()).thenReturn(translationConfig)
                whenever(whisperManager.chat(any())).thenReturn(
                    Result.failure(ChatCompletionException.EmptyResponseException()),
                )

                val result = translateManager.translateText("Hello")

                assertTrue(result.isFailure)
                val exception = result.exceptionOrNull()
                assertTrue(exception is TranslationException.TranslationFailedException)
                assertEquals("Translation failed: Empty response", exception?.message)
            }

        @Test
        fun `translateText maps NetworkException to TranslationFailedException`() =
            TestScope(StandardTestDispatcher()).runTest {
                val translationConfig = TranslationConfig()
                val networkError = "Connection timeout"

                whenever(settingsManager.getTargetLanguage()).thenReturn(Language.ENGLISH)
                whenever(settingsManager.getTranslationConfig()).thenReturn(translationConfig)
                whenever(whisperManager.chat(any())).thenReturn(
                    Result.failure(ChatCompletionException.NetworkException(networkError)),
                )

                val result = translateManager.translateText("Hello")

                assertTrue(result.isFailure)
                val exception = result.exceptionOrNull()
                assertTrue(exception is TranslationException.TranslationFailedException)
                assertEquals("Translation failed: Network error: Chat network error: $networkError", exception?.message)
            }

        @Test
        fun `translateText preserves other exceptions unchanged`() =
            TestScope(StandardTestDispatcher()).runTest {
                val translationConfig = TranslationConfig()
                val otherException = RuntimeException("Some other error")

                whenever(settingsManager.getTargetLanguage()).thenReturn(Language.ENGLISH)
                whenever(settingsManager.getTranslationConfig()).thenReturn(translationConfig)
                whenever(whisperManager.chat(any())).thenReturn(Result.failure(otherException))

                val result = translateManager.translateText("Hello")

                assertTrue(result.isFailure)
                val exception = result.exceptionOrNull()
                assertEquals(otherException, exception)
            }

        @Test
        fun `translateText handles CompletionFailedException as generic exception`() =
            TestScope(StandardTestDispatcher()).runTest {
                val translationConfig = TranslationConfig()
                val completionError = ChatCompletionException.CompletionFailedException("API error")

                whenever(settingsManager.getTargetLanguage()).thenReturn(Language.ENGLISH)
                whenever(settingsManager.getTranslationConfig()).thenReturn(translationConfig)
                whenever(whisperManager.chat(any())).thenReturn(Result.failure(completionError))

                val result = translateManager.translateText("Hello")

                assertTrue(result.isFailure)
                val exception = result.exceptionOrNull()
                assertEquals(completionError, exception)
            }
    }

    @Nested
    @DisplayName("TranslationException Factory Methods")
    inner class TranslationExceptionFactoryMethods {
        @Test
        fun `EmptyTextException has correct message`() {
            val exception = TranslationException.EmptyTextException()

            assertEquals("Text to translate is empty", exception.message)
            assertTrue(exception is TranslationException)
        }

        @Test
        fun `TranslationFailedException has correct message format`() {
            val errorDetails = "Network timeout occurred"
            val exception = TranslationException.TranslationFailedException(errorDetails)

            assertEquals("Translation failed: Network timeout occurred", exception.message)
            assertTrue(exception is TranslationException)
        }

        @Test
        fun `TranslationException is base class for all translation errors`() {
            val emptyException = TranslationException.EmptyTextException()
            val failedException = TranslationException.TranslationFailedException("Test error")

            assertTrue(emptyException is TranslationException)
            assertTrue(failedException is TranslationException)
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    inner class IntegrationScenarios {
        @Test
        fun `translateText handles complex language configuration scenario`() =
            TestScope(StandardTestDispatcher()).runTest {
                val targetLanguage = Language.CHINESE
                val translationConfig =
                    TranslationConfig(
                        model = "gpt-4-turbo",
                        systemPrompt = "You are a professional translator. Translate to {LANGUAGE} maintaining context and tone.",
                    )
                val inputText = "The weather is beautiful today"
                val expectedTranslation = "今天天气很好"

                whenever(settingsManager.getTargetLanguage()).thenReturn(targetLanguage)
                whenever(settingsManager.getTranslationConfig()).thenReturn(translationConfig)
                whenever(whisperManager.chat(any())).thenReturn(Result.success(expectedTranslation))

                val result = translateManager.translateText(inputText)

                assertTrue(result.isSuccess)
                assertEquals(expectedTranslation, result.getOrNull())

                verify(whisperManager).chat(
                    ChatRequest(
                        model = "gpt-4-turbo",
                        systemPrompt = "You are a professional translator. Translate to Chinese maintaining context and tone.",
                        message = inputText,
                    ),
                )
            }

        @Test
        fun `translateText handles multi-line text input`() =
            TestScope(StandardTestDispatcher()).runTest {
                val multiLineText = "Hello world.\nHow are you today?\nI hope you're doing well."
                val translationConfig = TranslationConfig()

                whenever(settingsManager.getTargetLanguage()).thenReturn(Language.FRENCH)
                whenever(settingsManager.getTranslationConfig()).thenReturn(translationConfig)
                whenever(
                    whisperManager.chat(any()),
                ).thenReturn(Result.success("Bonjour le monde.\nComment allez-vous aujourd'hui?\nJ'espère que vous allez bien."))

                val result = translateManager.translateText(multiLineText)

                assertTrue(result.isSuccess)
                verify(whisperManager).chat(any())
            }

        @Test
        fun `translateText preserves special characters and formatting`() =
            TestScope(StandardTestDispatcher()).runTest {
                val textWithSpecialChars = "Hello! How are you? 😊 Visit https://example.com"
                val translationConfig = TranslationConfig()

                whenever(settingsManager.getTargetLanguage()).thenReturn(Language.GERMAN)
                whenever(settingsManager.getTranslationConfig()).thenReturn(translationConfig)
                whenever(whisperManager.chat(any())).thenReturn(Result.success("Hallo! Wie geht es dir? 😊 Besuche https://example.com"))

                val result = translateManager.translateText(textWithSpecialChars)

                assertTrue(result.isSuccess)
                verify(whisperManager).chat(
                    ChatRequest(
                        model = translationConfig.model,
                        systemPrompt = translationConfig.systemPrompt.replace("{LANGUAGE}", "German"),
                        message = textWithSpecialChars,
                    ),
                )
            }
    }
}
