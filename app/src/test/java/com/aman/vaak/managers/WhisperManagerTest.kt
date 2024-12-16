package com.aman.vaak.managers

import com.aman.vaak.models.WhisperConfig
import com.aman.vaak.models.WhisperResult
import com.aallam.openai.api.audio.Transcription
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.test.runTest
import com.aallam.openai.api.file.FileSource
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import java.io.ByteArrayInputStream
import java.io.InputStream
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
        fun `returns ConfigError when config not initialized`() = runTest {
            // Test uninitialized state - no config setup needed
            val result = manager.transcribeAudio(testFile, "en")
            
            // Verify error type and message
            assertTrue(result is WhisperResult.Error.ConfigurationError)
            assertEquals(
                "Whisper configuration not initialized",
                (result as WhisperResult.Error.ConfigurationError).message
            )
        }
        
        @Test
        fun `returns Success with transcription response`() = runTest {
            // Given
            manager.updateConfig(config)
            val expectedText = "Hello World"
            whenever(fileManager.isFileValid(testFile)).thenReturn(true)
            whenever(fileManager.createFileSource(testFile)).thenReturn(fileSource)
            whenever(openAI.transcription(any())).thenReturn(Transcription(expectedText))
            
            // When
            val result = manager.transcribeAudio(testFile, "en")
            
            // Then
            assertTrue(result is WhisperResult.Success)
            assertEquals(expectedText, (result as WhisperResult.Success).text)
        }
        
        @Test 
        fun `returns NetworkError on OpenAI exception`() = runTest {
            // Given
            manager.updateConfig(config)
            whenever(fileManager.isFileValid(testFile)).thenReturn(true)
            whenever(fileManager.createFileSource(testFile)).thenReturn(fileSource)
            whenever(openAI.transcription(any())).thenThrow(RuntimeException("API Error"))
            
            // When
            val result = manager.transcribeAudio(testFile, "en")
            
            // Then
            assertTrue(result is WhisperResult.Error.NetworkError)
        }
    }
}
