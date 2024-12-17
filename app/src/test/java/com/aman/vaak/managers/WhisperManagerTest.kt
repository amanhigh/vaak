package com.aman.vaak.managers

import com.aman.vaak.models.WhisperConfig
import com.aman.vaak.models.TranscriptionResult
import com.aman.vaak.models.TranscriptionException
import com.aallam.openai.api.audio.Transcription
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.test.runTest
import com.aallam.openai.api.file.FileSource
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.junit.jupiter.api.Assertions.*
import java.io.File

@ExtendWith(MockitoExtension::class)
class WhisperManagerTest {
    @Mock private lateinit var openAI: OpenAI
    @Mock private lateinit var settingsManager: SettingsManager
    @Mock private lateinit var fileManager: FileManager
    @Mock private lateinit var fileSource: FileSource
    private lateinit var manager: WhisperManager
    private lateinit var config: WhisperConfig
    private val testFile = File("test.wav")

    @BeforeEach
    fun setup() {
        manager = WhisperManagerImpl(openAI, settingsManager, fileManager)
        config = WhisperConfig(
            apiKey = "test-key",
            model = "whisper-1",
            language = "en"
        )
    }

    @Nested
    inner class WhenTranscribing {
        @Test
        fun `returns failure with ConfigurationError when config not initialized`() = runTest {
            // Test uninitialized state - no config setup needed
            val result = manager.transcribeAudio(testFile, "en")
            
            // Verify failure case
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is TranscriptionException.ConfigurationError)
            assertEquals(
                "Whisper configuration not initialized",
                exception?.message
            )
        }
        
        @Test
        fun `returns success with TranscriptionResult when transcription succeeds`() = runTest {
            // Given
            manager.updateConfig(config)
            val expectedText = "Hello World"
            whenever(fileManager.isFileValid(testFile)).thenReturn(true)
            whenever(fileManager.createFileSource(testFile)).thenReturn(fileSource)
            whenever(openAI.transcription(any())).thenReturn(Transcription(expectedText))
            
            // When
            val result = manager.transcribeAudio(testFile, "en")
            
            // Then
            assertTrue(result.isSuccess)
            val transcription = result.getOrNull()
            assertNotNull(transcription)
            assertEquals(expectedText, transcription?.text)
        }
        
        @Test 
        fun `returns failure with NetworkError when API fails`() = runTest {
            // Given
            manager.updateConfig(config)
            whenever(fileManager.isFileValid(testFile)).thenReturn(true)
            whenever(fileManager.createFileSource(testFile)).thenReturn(fileSource)
            whenever(openAI.transcription(any())).thenThrow(RuntimeException("API Error"))
            
            // When
            val result = manager.transcribeAudio(testFile, "en")
            
            // Then
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is TranscriptionException.NetworkError)
            assertEquals("Transcription failed: API Error", exception?.message)
        }
    }
}
