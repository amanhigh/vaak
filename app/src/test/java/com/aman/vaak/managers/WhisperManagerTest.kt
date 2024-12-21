package com.aman.vaak.managers

import com.aman.vaak.models.WhisperConfig
import com.aman.vaak.models.TranscriptionResult
import com.aman.vaak.models.TranscriptionException
import com.aman.vaak.models.WhisperResponseFormat
import com.aallam.openai.api.audio.AudioResponseFormat
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.audio.Transcription
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.test.runTest
import com.aallam.openai.api.file.FileSource
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.argThat
import java.io.File

@ExtendWith(MockitoExtension::class)
class WhisperManagerTest {
    @Mock
    private lateinit var openAI: OpenAI
    @Mock
    private lateinit var settingsManager: SettingsManager
    @Mock
    private lateinit var fileManager: FileManager
    @Mock
    private lateinit var fileSource: FileSource
    private lateinit var manager: WhisperManager
    private val testFile = File("test.wav")
    private val testApiKey = "test-key"

    @BeforeEach
    fun setup() {
        whenever(settingsManager.getApiKey()).thenReturn(testApiKey)
        manager = WhisperManagerImpl(openAI, settingsManager, fileManager)
    }

    @Nested
    inner class ConfigurationTests {
        @Test
        fun `initializes with defaults and API key from settings`() {
            val config = manager.getCurrentConfig()
            assertEquals(testApiKey, config.apiKey)
            assertEquals("whisper-1", config.model)
            assertEquals(0.2f, config.temperature)
            assertEquals("en", config.language)
            assertEquals(WhisperResponseFormat.JSON, config.responseFormat)
        }

        @Test
        fun `throws configuration error when API key missing`() {
            whenever(settingsManager.getApiKey()).thenReturn(null)
            val exception = assertThrows(TranscriptionException.ConfigurationError::class.java) {
                WhisperManagerImpl(openAI, settingsManager, fileManager)
            }
            assertEquals("API Key not found in settings", exception.message)
        }
        
        @Test
        fun `allows partial config updates`() {
            manager.updateConfig { it.copy(temperature = 0.8f) }
            val config = manager.getCurrentConfig()
            assertEquals(0.8f, config.temperature)
            assertEquals(testApiKey, config.apiKey)
            assertEquals("whisper-1", config.model)
        }
    }

    @Nested
    inner class TranscriptionTests {
        @Test
        fun `returns success with TranscriptionResult when transcription succeeds`() = runTest {
            val expectedText = "Hello World"
            whenever(fileManager.isFileValid(testFile)).thenReturn(true)
            whenever(fileManager.createFileSource(testFile)).thenReturn(fileSource)
            whenever(openAI.transcription(any())).thenReturn(Transcription(expectedText))
            
            val result = manager.transcribeAudio(testFile)
            
            assertTrue(result.isSuccess)
            val transcription = result.getOrNull()
            assertNotNull(transcription)
            assertEquals(expectedText, transcription?.text)
        }
        
        @Test 
        fun `returns failure with NetworkError when API fails`() = runTest {
            whenever(fileManager.isFileValid(testFile)).thenReturn(true)
            whenever(fileManager.createFileSource(testFile)).thenReturn(fileSource)
            whenever(openAI.transcription(any())).thenThrow(RuntimeException("API Error"))
            
            val result = manager.transcribeAudio(testFile)
            
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is TranscriptionException.NetworkError)
            assertEquals("Transcription failed: API Error", exception?.message)
        }

        @Test
        fun `uses language override when provided`() = runTest {
            whenever(fileManager.isFileValid(testFile)).thenReturn(true)
            whenever(fileManager.createFileSource(testFile)).thenReturn(fileSource)
            whenever(openAI.transcription(any())).thenReturn(Transcription("text"))
            
            manager.transcribeAudio(testFile, "fr")
            
            verify(openAI).transcription(argThat { request: TranscriptionRequest ->
                request.language == "fr"
            })
        }

        @Test
        fun `uses config language when no override provided`() = runTest {
            whenever(fileManager.isFileValid(testFile)).thenReturn(true)
            whenever(fileManager.createFileSource(testFile)).thenReturn(fileSource)
            whenever(openAI.transcription(any())).thenReturn(Transcription("text"))
            
            manager.transcribeAudio(testFile)
            
            verify(openAI).transcription(argThat { request: TranscriptionRequest ->
                request.language == "en"
            })
        }

        @Test
        fun `returns failure with FileError when file is invalid`() = runTest {
            whenever(fileManager.isFileValid(testFile)).thenReturn(false)
            
            val result = manager.transcribeAudio(testFile)
            
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is TranscriptionException.FileError)
            assertEquals("Audio file does not exist or cannot be read", exception?.message)
        }
    }
}
