package com.aman.vaak.managers

import com.aman.vaak.models.DictationState
import com.aman.vaak.models.DictationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockitoExtension::class)
class DictationManagerTest {
    @Mock private lateinit var voiceManager: VoiceManager

    @Mock private lateinit var whisperManager: WhisperManager

    @Mock private lateinit var translateManager: TranslateManager

    @Mock private lateinit var fileManager: FileManager

    @Mock private lateinit var settingsManager: SettingsManager

    @Mock private lateinit var textManager: TextManager

    private lateinit var testScope: CoroutineScope
    private lateinit var dictationManager: DictationManagerImpl

    @BeforeEach
    fun setup() {
        testScope = CoroutineScope(StandardTestDispatcher())

        dictationManager =
            DictationManagerImpl(
                voiceManager = voiceManager,
                whisperManager = whisperManager,
                translateManager = translateManager,
                fileManager = fileManager,
                settingsManager = settingsManager,
                textManager = textManager,
                scope = testScope,
            )
    }

    @Nested
    inner class InitialState {
        @Test
        fun `starts in IDLE state with zero time`() =
            TestScope(StandardTestDispatcher()).runTest {
                val state: DictationState = dictationManager.watchDictationState().first()

                assertEquals(DictationStatus.IDLE, state.status)
                assertEquals(0L, state.timeMillis)
            }
    }

    @Nested
    inner class StartDictation {
        @Test
        fun `starts dictation successfully from IDLE state`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(voiceManager.startRecording()).thenReturn(Result.success(Unit))

                val result = dictationManager.startDictation()

                assertTrue(result.isSuccess)
                verify(voiceManager).startRecording()
            }

        @Test
        fun `throws AlreadyDictatingException when already recording`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(voiceManager.startRecording()).thenReturn(Result.success(Unit))

                dictationManager.startDictation()
                val result = dictationManager.startDictation()

                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is DictationException.AlreadyDictatingException)
            }

        @Test
        fun `cleans up state when voice recording fails`() =
            TestScope(StandardTestDispatcher()).runTest {
                val voiceException = Exception("Microphone not available")
                whenever(voiceManager.startRecording()).thenReturn(Result.failure(voiceException))

                val result = dictationManager.startDictation()

                assertTrue(result.isFailure)
                assertEquals(voiceException, result.exceptionOrNull())
            }
    }

    @Nested
    inner class CancelDictation {
        @Test
        fun `cancels dictation successfully when recording`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(voiceManager.startRecording()).thenReturn(Result.success(Unit))
                whenever(voiceManager.cancelRecording()).thenReturn(Result.success(Unit))

                dictationManager.startDictation()
                val result = dictationManager.cancelDictation()

                assertTrue(result.isSuccess)
                verify(voiceManager).cancelRecording()
            }

        @Test
        fun `throws NotDictatingException when already idle`() {
            val result = dictationManager.cancelDictation()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is DictationException.NotDictatingException)
            verify(voiceManager, never()).cancelRecording()
        }
    }

    @Nested
    inner class ResourceManagement {
        @Test
        fun `release cleans up all resources`() {
            dictationManager.release()

            verify(voiceManager).release()
            verify(whisperManager).release()
        }
    }
}
