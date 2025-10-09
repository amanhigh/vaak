package com.aman.vaak.managers

import android.media.MediaRecorder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockitoExtension::class)
class VoiceManagerTest {
    @Mock
    private lateinit var fileManager: FileManager

    @Mock
    private lateinit var mockFile: File

    private lateinit var voiceManager: VoiceManager

    @BeforeEach
    fun setup() {
        voiceManager = VoiceManagerImpl(fileManager)
    }

    @Nested
    @DisplayName("Recording State Management")
    inner class RecordingStateManagement {
        @Test
        fun `isRecording returns false initially`() {
            assertFalse(voiceManager.isRecording())
        }

        @Test
        fun `isRecording returns true when recording started successfully`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(fileManager.createTempFile("m4a")).thenReturn(mockFile)
                whenever(mockFile.absolutePath).thenReturn("/path/to/temp.m4a")

                Mockito.mockConstruction(MediaRecorder::class.java) { mock, _ ->
                    // Mock MediaRecorder behavior
                }.use {
                    val result = voiceManager.startRecording()

                    assertTrue(result.isSuccess)
                    assertTrue(voiceManager.isRecording())
                }
            }

        @Test
        fun `isRecording returns false after successful stop`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(fileManager.createTempFile("m4a")).thenReturn(mockFile)
                whenever(mockFile.absolutePath).thenReturn("/path/to/temp.m4a")

                Mockito.mockConstruction(MediaRecorder::class.java) { mock, _ ->
                    // Mock MediaRecorder behavior
                }.use {
                    voiceManager.startRecording()
                    val result = voiceManager.stopRecording()

                    assertTrue(result.isSuccess)
                    assertFalse(voiceManager.isRecording())
                }
            }

        @Test
        fun `isRecording returns false after successful cancel`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(fileManager.createTempFile("m4a")).thenReturn(mockFile)
                whenever(mockFile.absolutePath).thenReturn("/path/to/temp.m4a")

                Mockito.mockConstruction(MediaRecorder::class.java) { mock, _ ->
                    // Mock MediaRecorder behavior
                }.use {
                    voiceManager.startRecording()
                    val result = voiceManager.cancelRecording()

                    assertTrue(result.isSuccess)
                    assertFalse(voiceManager.isRecording())
                }
            }
    }

    @Nested
    @DisplayName("Start Recording")
    inner class StartRecording {
        @Test
        fun `startRecording succeeds with proper MediaRecorder setup`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(fileManager.createTempFile("m4a")).thenReturn(mockFile)
                whenever(mockFile.absolutePath).thenReturn("/path/to/temp.m4a")

                Mockito.mockConstruction(MediaRecorder::class.java) { mock, _ ->
                    // Mock MediaRecorder methods
                }.use {
                    val result = voiceManager.startRecording()

                    assertTrue(result.isSuccess)
                    assertTrue(voiceManager.isRecording())
                    verify(fileManager).createTempFile("m4a")
                }
            }

        @Test
        fun `startRecording fails when already recording`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(fileManager.createTempFile("m4a")).thenReturn(mockFile)
                whenever(mockFile.absolutePath).thenReturn("/path/to/temp.m4a")

                Mockito.mockConstruction(MediaRecorder::class.java) { mock, _ ->
                    // Mock MediaRecorder behavior
                }.use {
                    voiceManager.startRecording()

                    val result = voiceManager.startRecording()

                    assertTrue(result.isFailure)
                    val exception = result.exceptionOrNull()
                    assertTrue(exception is VoiceRecordingException.AlreadyRecordingException)
                    assertEquals("Already recording", exception?.message)
                }
            }

        @Test
        fun `startRecording fails when MediaRecorder setup throws exception`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(fileManager.createTempFile("m4a")).thenReturn(mockFile)
                whenever(mockFile.absolutePath).thenReturn("/path/to/temp.m4a")

                Mockito.mockConstruction(MediaRecorder::class.java) { mock, _ ->
                    doThrow(RuntimeException("MediaRecorder setup failed")).whenever(mock).prepare()
                }.use {
                    val result = voiceManager.startRecording()

                    assertTrue(result.isFailure)
                    val exception = result.exceptionOrNull()
                    assertTrue(exception is VoiceRecordingException.HardwareInitializationException)
                    assertEquals("Unable to Create Audio Recorder", exception?.message)
                    assertFalse(voiceManager.isRecording())
                }
            }

        @Test
        fun `startRecording fails when MediaRecorder start throws exception`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(fileManager.createTempFile("m4a")).thenReturn(mockFile)
                whenever(mockFile.absolutePath).thenReturn("/path/to/temp.m4a")

                Mockito.mockConstruction(MediaRecorder::class.java) { mock, _ ->
                    doThrow(RuntimeException("MediaRecorder start failed")).whenever(mock).start()
                }.use {
                    val result = voiceManager.startRecording()

                    assertTrue(result.isFailure)
                    val exception = result.exceptionOrNull()
                    assertTrue(exception is VoiceRecordingException.HardwareInitializationException)
                    assertEquals("Unable to Create Audio Recorder", exception?.message)
                    assertFalse(voiceManager.isRecording())
                }
            }

        @Test
        fun `startRecording creates temp file with m4a extension`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(fileManager.createTempFile("m4a")).thenReturn(mockFile)
                whenever(mockFile.absolutePath).thenReturn("/path/to/temp.m4a")

                Mockito.mockConstruction(MediaRecorder::class.java) { mock, _ ->
                    // Mock MediaRecorder behavior
                }.use {
                    voiceManager.startRecording()

                    verify(fileManager).createTempFile("m4a")
                }
            }
    }

    @Nested
    @DisplayName("Stop Recording")
    inner class StopRecording {
        @Test
        fun `stopRecording succeeds and returns audio file`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(fileManager.createTempFile("m4a")).thenReturn(mockFile)
                whenever(mockFile.absolutePath).thenReturn("/path/to/temp.m4a")

                Mockito.mockConstruction(MediaRecorder::class.java) { mock, _ ->
                    // Mock MediaRecorder behavior
                }.use {
                    voiceManager.startRecording()

                    val result = voiceManager.stopRecording()

                    assertTrue(result.isSuccess)
                    assertEquals(mockFile, result.getOrNull())
                    assertFalse(voiceManager.isRecording())
                }
            }

        @Test
        fun `stopRecording fails when not recording`() =
            TestScope(StandardTestDispatcher()).runTest {
                val result = voiceManager.stopRecording()

                assertTrue(result.isFailure)
                val exception = result.exceptionOrNull()
                assertTrue(exception is VoiceRecordingException.NotRecordingException)
                assertEquals("Not currently recording", exception?.message)
            }

        @Test
        fun `stopRecording handles MediaRecorder stop exception gracefully`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(fileManager.createTempFile("m4a")).thenReturn(mockFile)
                whenever(mockFile.absolutePath).thenReturn("/path/to/temp.m4a")

                Mockito.mockConstruction(MediaRecorder::class.java) { mock, _ ->
                    doThrow(RuntimeException("MediaRecorder stop failed")).whenever(mock).stop()
                }.use {
                    voiceManager.startRecording()

                    val result = voiceManager.stopRecording()

                    assertTrue(result.isFailure)
                    assertFalse(voiceManager.isRecording())
                }
            }
    }

    @Nested
    @DisplayName("Cancel Recording")
    inner class CancelRecording {
        @Test
        fun `cancelRecording succeeds when recording is active`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(fileManager.createTempFile("m4a")).thenReturn(mockFile)
                whenever(mockFile.absolutePath).thenReturn("/path/to/temp.m4a")

                Mockito.mockConstruction(MediaRecorder::class.java) { mock, _ ->
                    // Mock MediaRecorder behavior
                }.use {
                    voiceManager.startRecording()

                    val result = voiceManager.cancelRecording()

                    assertTrue(result.isSuccess)
                    assertFalse(voiceManager.isRecording())
                }
            }

        @Test
        fun `cancelRecording fails when not recording`() {
            val result = voiceManager.cancelRecording()

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is VoiceRecordingException.NotRecordingException)
            assertEquals("Not currently recording", exception?.message)
        }

        @Test
        fun `cancelRecording succeeds and cleans up properly when recording is active`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(fileManager.createTempFile("m4a")).thenReturn(mockFile)
                whenever(mockFile.absolutePath).thenReturn("/path/to/temp.m4a")

                Mockito.mockConstruction(MediaRecorder::class.java) { mock, _ ->
                    // Mock MediaRecorder behavior
                }.use {
                    voiceManager.startRecording()

                    val result = voiceManager.cancelRecording()

                    assertTrue(result.isSuccess)
                    assertFalse(voiceManager.isRecording())
                    // Note: File deletion verification depends on internal implementation
                }
            }
    }

    @Nested
    @DisplayName("Resource Management")
    inner class ResourceManagement {
        @Test
        fun `release cleans up all resources`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(fileManager.createTempFile("m4a")).thenReturn(mockFile)
                whenever(mockFile.absolutePath).thenReturn("/path/to/temp.m4a")

                Mockito.mockConstruction(MediaRecorder::class.java) { mock, _ ->
                    // Mock MediaRecorder behavior
                }.use {
                    voiceManager.startRecording()

                    voiceManager.release()

                    assertFalse(voiceManager.isRecording())
                }
            }

        @Test
        fun `release can be called when not recording`() {
            voiceManager.release()

            assertFalse(voiceManager.isRecording())
        }

        @Test
        fun `release does not delete file when called directly`() {
            voiceManager.release()

            verify(fileManager, never()).deleteFile(any())
        }
    }

    @Nested
    @DisplayName("VoiceRecordingException Factory Methods")
    inner class VoiceRecordingExceptionFactoryMethods {
        @Test
        fun `AlreadyRecordingException has correct message`() {
            val exception = VoiceRecordingException.AlreadyRecordingException()

            assertEquals("Already recording", exception.message)
            assertTrue(exception is VoiceRecordingException)
        }

        @Test
        fun `NotRecordingException has correct message`() {
            val exception = VoiceRecordingException.NotRecordingException()

            assertEquals("Not currently recording", exception.message)
            assertTrue(exception is VoiceRecordingException)
        }

        @Test
        fun `HardwareInitializationException has correct message`() {
            val exception = VoiceRecordingException.HardwareInitializationException()

            assertEquals("Unable to Create Audio Recorder", exception.message)
            assertTrue(exception is VoiceRecordingException)
        }
    }
}
