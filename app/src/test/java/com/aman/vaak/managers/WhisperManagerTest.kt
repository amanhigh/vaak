package com.aman.vaak.managers

import com.aallam.openai.api.audio.Transcription
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.io.IOException

@ExperimentalCoroutinesApi
@ExtendWith(MockitoExtension::class)
class WhisperManagerTest {
    @Mock
    private lateinit var openAI: OpenAI

    @Mock
    private lateinit var settingsManager: SettingsManager

    @Mock
    private lateinit var fileManager: FileManager

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var manager: WhisperManager
    private val testApiKey = "test-api-key"
    private val testFile = File("test.wav")

    @BeforeEach
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
    }

    @Nested
    inner class ConfigurationTests {
        private fun createManagerWithApiKey(apiKey: String?) {
            whenever(settingsManager.getApiKey()).thenReturn(apiKey)
            manager = WhisperManagerImpl(openAI, settingsManager, fileManager)
        }

        @Test
        fun `initialization succeeds with valid API key`() {
            createManagerWithApiKey(testApiKey)
            val config = manager.getCurrentConfig()
            assertEquals(testApiKey, config.apiKey)
            assertEquals("whisper-1", config.model)
            assertEquals(0.2f, config.temperature)
            assertEquals("en", config.language)
        }

        @Test
        fun `initialization fails with missing API key`() {
            assertThrows(TranscriptionException.InvalidApiKeyException::class.java) {
                createManagerWithApiKey(null)
            }.apply {
                assertEquals("Invalid or missing API key", message)
            }
        }

        @Test
        fun `config update succeeds with valid parameters`() {
            createManagerWithApiKey(testApiKey)
            val newTemperature = 0.5f
            manager.updateConfig { it.copy(temperature = newTemperature) }

            val updatedConfig = manager.getCurrentConfig()
            assertEquals(newTemperature, updatedConfig.temperature)
            assertEquals(testApiKey, updatedConfig.apiKey)
        }

        @Test
        fun `validation fails with invalid temperature`() =
            runTest {
                createManagerWithApiKey(testApiKey)
                manager.updateConfig { it.copy(temperature = 2.0f) }

                val result = manager.transcribeAudio(testFile)
                assertTrue(result.isFailure)
                assertThrows(TranscriptionException.InvalidTemperatureException::class.java) {
                    result.getOrThrow()
                }.apply {
                    assertEquals("Temperature must be between 0 and 1", message)
                }
            }

        @Test
        fun `validation fails with invalid model`() =
            runTest {
                createManagerWithApiKey(testApiKey)
                manager.updateConfig { it.copy(model = "invalid-model") }

                val result = manager.transcribeAudio(testFile)
                assertTrue(result.isFailure)
                assertThrows(TranscriptionException.InvalidModelException::class.java) {
                    result.getOrThrow()
                }.apply {
                    assertEquals("Invalid model specified: invalid-model", message)
                }
            }
    }

    @Nested
    inner class LanguageValidationTests {
        @BeforeEach
        fun setupLanguageValidation() =
            runTest {
                whenever(settingsManager.getApiKey()).thenReturn(testApiKey)
                manager = WhisperManagerImpl(openAI, settingsManager, fileManager)
            }

        @Test
        fun `validation succeeds with supported language`() =
            runTest {
                whenever(fileManager.createFileSource(any())).thenReturn(mock())
                whenever(openAI.transcription(any())).thenReturn(Transcription("text"))
                val result = manager.transcribeAudio(testFile, "en")
                assertTrue(result.isSuccess)
                assertEquals("text", result.getOrNull()?.text)
            }

        @Test
        fun `validation succeeds with null language`() =
            runTest {
                whenever(fileManager.createFileSource(any())).thenReturn(mock())
                whenever(openAI.transcription(any())).thenReturn(Transcription("text"))
                val result = manager.transcribeAudio(testFile, null)
                assertTrue(result.isSuccess)
                assertEquals("text", result.getOrNull()?.text)
            }

        @Test
        fun `validation fails with unsupported language`() =
            runTest {
                val invalidLanguage = "xx"
                val result = manager.transcribeAudio(testFile, invalidLanguage)

                assertTrue(result.isFailure)
                assertThrows(TranscriptionException.InvalidLanguageException::class.java) {
                    result.getOrThrow()
                }.apply {
                    assertEquals("Unsupported language code: $invalidLanguage", message)
                }
            }

        @Test
        fun `validation succeeds with supported language in different case`() =
            runTest {
                whenever(fileManager.createFileSource(any())).thenReturn(mock())
                whenever(openAI.transcription(any())).thenReturn(Transcription("text"))
                val result = manager.transcribeAudio(testFile, "EN")
                assertTrue(result.isSuccess)
                assertEquals("text", result.getOrNull()?.text)
            }
    }

    @Nested
    inner class TranscriptionTests {
        @BeforeEach
        fun setupTranscription() =
            runTest {
                whenever(fileManager.validateAudioFile(any(), any())).thenReturn(Result.success(Unit))
                whenever(settingsManager.getApiKey()).thenReturn(testApiKey)
                whenever(fileManager.createFileSource(any())).thenReturn(mock())
                manager = WhisperManagerImpl(openAI, settingsManager, fileManager)
            }

        @Test
        fun `transcription returns success with response text`() =
            runTest {
                val expectedText = "transcribed text"
                whenever(openAI.transcription(any())).thenReturn(Transcription(expectedText))

                val result = manager.transcribeAudio(testFile)
                assertTrue(result.isSuccess)
                assertEquals(expectedText, result.getOrNull()?.text)
            }

        @Test
        fun `transcription fails with network error on IOException`() =
            runTest {
                whenever(openAI.transcription(any())).thenAnswer { throw IOException("Network error") }

                val result = manager.transcribeAudio(testFile)
                assertTrue(result.isFailure)
                assertThrows(TranscriptionException.NetworkException::class.java) {
                    result.getOrThrow()
                }.apply {
                    assertEquals("Network error during transcription", message)
                }
            }

        @Test
        fun `transcription request succeeds with different language`() =
            runTest {
                whenever(openAI.transcription(any())).thenReturn(Transcription("text in french"))

                val result = manager.transcribeAudio(testFile, "fr")
                assertTrue(result.isSuccess)
                assertEquals("text in french", result.getOrNull()?.text)
            }
    }

    @Nested
    inner class ErrorHandlingTests {
        @BeforeEach
        fun setupErrorTests() {
            whenever(fileManager.validateAudioFile(any(), any())).thenReturn(Result.success(Unit))
            whenever(fileManager.createFileSource(any())).thenReturn(mock())
            whenever(settingsManager.getApiKey()).thenReturn(testApiKey)
            manager = WhisperManagerImpl(openAI, settingsManager, fileManager)
        }

        @Test
        fun `transcription fails when API returns error response`() =
            runTest {
                whenever(openAI.transcription(any())).thenThrow(IllegalStateException("401 Unauthorized"))

                val result = manager.transcribeAudio(testFile)
                assertTrue(result.isFailure)

                val exception =
                    assertThrows(TranscriptionException.TranscriptionFailedException::class.java) {
                        result.getOrThrow()
                    }
                assertTrue(exception.message?.contains("401 Unauthorized") == true)
            }

        @Test
        fun `transcription fails with details when API throws unknown exception`() =
            runTest {
                val testError = RuntimeException("Unknown API Error")
                whenever(openAI.transcription(any())).thenAnswer { throw testError }

                val result = manager.transcribeAudio(testFile)
                assertTrue(result.isFailure)

                val exception =
                    assertThrows(TranscriptionException.TranscriptionFailedException::class.java) {
                        result.getOrThrow()
                    }
                assertNotNull(exception.details)
                assertEquals("Unknown API Error", exception.details?.get("message"))
                assertEquals("RuntimeException", exception.details?.get("exception_type"))
                assertNotNull(exception.details?.get("stack_trace"))
            }

        @Test
        fun `transcription failure includes proper error details`() =
            runTest {
                val customError = Exception("Custom Test Error")
                whenever(openAI.transcription(any())).thenAnswer { throw customError }

                val result = manager.transcribeAudio(testFile)
                assertTrue(result.isFailure)

                val exception =
                    assertThrows(TranscriptionException.TranscriptionFailedException::class.java) {
                        result.getOrThrow()
                    }

                // Verify error details structure
                val details = exception.details
                assertNotNull(details)
                with(details!!) {
                    // Verify required fields exist
                    assertTrue(containsKey("exception_type"))
                    assertTrue(containsKey("message"))
                    assertTrue(containsKey("stack_trace"))

                    // Verify content
                    assertEquals("Exception", get("exception_type"))
                    assertEquals("Custom Test Error", get("message"))
                    assertNotNull(get("stack_trace"))
                    assertTrue((get("stack_trace") as String).isNotBlank())
                }
            }
    }
}
