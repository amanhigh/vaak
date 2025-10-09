package com.aman.vaak.managers

import com.aallam.openai.api.audio.Transcription
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.client.OpenAI
import com.aman.vaak.models.ChatRequest
import com.aman.vaak.models.ValidationException
import com.aman.vaak.models.WhisperConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import javax.inject.Provider

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockitoExtension::class)
class WhisperManagerTest {
    @Mock private lateinit var settingsManager: SettingsManager

    @Mock private lateinit var fileManager: FileManager

    @Mock private lateinit var openAIProvider: Provider<OpenAI>

    @Mock private lateinit var openAI: OpenAI

    @Mock private lateinit var audioFile: File

    @Mock private lateinit var fileSource: FileSource

    private lateinit var whisperManager: WhisperManagerImpl

    @BeforeEach
    fun setup() {
        Mockito.lenient().whenever(openAIProvider.get()).thenReturn(openAI)

        whisperManager =
            WhisperManagerImpl(
                settingsManager = settingsManager,
                fileManager = fileManager,
                openAIProvider = openAIProvider,
            )
    }

    @Nested
    inner class TranscribeAudio {
        @Test
        fun `transcribes audio successfully with default language`() =
            TestScope(StandardTestDispatcher()).runTest {
                val whisperConfig = WhisperConfig()
                val transcription = Transcription(text = "Hello world")

                whenever(settingsManager.getWhisperConfig()).thenReturn(whisperConfig)
                whenever(fileManager.validateAudioFile(audioFile, whisperConfig.maxFileSize))
                    .thenReturn(Result.success(Unit))
                whenever(fileManager.createFileSource(audioFile)).thenReturn(fileSource)
                whenever(openAI.transcription(any<TranscriptionRequest>())).thenReturn(transcription)

                val result = whisperManager.transcribeAudio(audioFile)

                assertTrue(result.isSuccess)
                assertEquals("Hello world", result.getOrNull()?.text)
                verify(openAI).transcription(any<TranscriptionRequest>())
            }

        @Test
        fun `transcribes audio with custom language`() =
            TestScope(StandardTestDispatcher()).runTest {
                val whisperConfig = WhisperConfig()
                val transcription = Transcription(text = "नमस्ते")

                whenever(settingsManager.getWhisperConfig()).thenReturn(whisperConfig)
                whenever(fileManager.validateAudioFile(audioFile, whisperConfig.maxFileSize))
                    .thenReturn(Result.success(Unit))
                whenever(fileManager.createFileSource(audioFile)).thenReturn(fileSource)
                whenever(openAI.transcription(any<TranscriptionRequest>())).thenReturn(transcription)

                val result = whisperManager.transcribeAudio(audioFile, "hi")

                assertTrue(result.isSuccess)
                assertEquals("नमस्ते", result.getOrNull()?.text)
            }

        @Test
        fun `fails when configuration validation fails`() =
            TestScope(StandardTestDispatcher()).runTest {
                val invalidConfig = WhisperConfig(temperature = 2.0f)
                whenever(settingsManager.getWhisperConfig()).thenReturn(invalidConfig)

                val result = whisperManager.transcribeAudio(audioFile)

                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is ValidationException.InvalidTemperatureException)
            }

        @Test
        fun `fails when audio file validation fails`() =
            TestScope(StandardTestDispatcher()).runTest {
                val whisperConfig = WhisperConfig()
                val validationError = Exception("File too large")

                whenever(settingsManager.getWhisperConfig()).thenReturn(whisperConfig)
                whenever(fileManager.validateAudioFile(audioFile, whisperConfig.maxFileSize))
                    .thenReturn(Result.failure(validationError))

                val result = whisperManager.transcribeAudio(audioFile)

                assertTrue(result.isFailure)
                assertEquals(validationError, result.exceptionOrNull())
            }

        @Test
        fun `fails when invalid language provided`() =
            TestScope(StandardTestDispatcher()).runTest {
                val whisperConfig = WhisperConfig()

                whenever(settingsManager.getWhisperConfig()).thenReturn(whisperConfig)
                whenever(fileManager.validateAudioFile(audioFile, whisperConfig.maxFileSize))
                    .thenReturn(Result.success(Unit))

                val result = whisperManager.transcribeAudio(audioFile, "invalid_lang")

                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is ValidationException.InvalidLanguageException)
            }

        @Test
        fun `handles network error during transcription`() =
            TestScope(StandardTestDispatcher()).runTest {
                val whisperConfig = WhisperConfig()
                val networkError = RuntimeException("Network timeout")

                whenever(settingsManager.getWhisperConfig()).thenReturn(whisperConfig)
                whenever(fileManager.validateAudioFile(audioFile, whisperConfig.maxFileSize))
                    .thenReturn(Result.success(Unit))
                whenever(fileManager.createFileSource(audioFile)).thenReturn(fileSource)
                whenever(openAI.transcription(any<TranscriptionRequest>())).thenThrow(networkError)

                val result = whisperManager.transcribeAudio(audioFile)

                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is TranscriptionException.TranscriptionFailedException)
            }

        @Test
        fun `handles general error during transcription`() =
            TestScope(StandardTestDispatcher()).runTest {
                val whisperConfig = WhisperConfig()
                val generalError = RuntimeException("API error")

                whenever(settingsManager.getWhisperConfig()).thenReturn(whisperConfig)
                whenever(fileManager.validateAudioFile(audioFile, whisperConfig.maxFileSize))
                    .thenReturn(Result.success(Unit))
                whenever(fileManager.createFileSource(audioFile)).thenReturn(fileSource)
                whenever(openAI.transcription(any<TranscriptionRequest>())).thenThrow(generalError)

                val result = whisperManager.transcribeAudio(audioFile)

                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is TranscriptionException.TranscriptionFailedException)
            }
    }

    @Nested
    inner class ChatCompletion {
        @Test
        fun `handles network error during chat`() =
            TestScope(StandardTestDispatcher()).runTest {
                val chatRequest =
                    ChatRequest(
                        model = "gpt-4o-mini",
                        message = "Hello",
                    )
                val networkError = RuntimeException("Connection timeout")

                whenever(openAI.chatCompletion(any<ChatCompletionRequest>())).thenThrow(networkError)

                val result = whisperManager.chat(chatRequest)

                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is ChatCompletionException.CompletionFailedException)
            }

        @Test
        fun `handles general error during chat`() =
            TestScope(StandardTestDispatcher()).runTest {
                val chatRequest =
                    ChatRequest(
                        model = "gpt-4o-mini",
                        message = "Hello",
                    )
                val generalError = RuntimeException("API error")

                whenever(openAI.chatCompletion(any<ChatCompletionRequest>())).thenThrow(generalError)

                val result = whisperManager.chat(chatRequest)

                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is ChatCompletionException.CompletionFailedException)
            }
    }

    @Nested
    inner class ResourceManagement {
        @Test
        fun `release can be called multiple times safely`() {
            whisperManager.release()
            whisperManager.release()
        }
    }
}
