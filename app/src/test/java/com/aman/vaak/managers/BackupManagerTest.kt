package com.aman.vaak.managers

import com.aman.vaak.models.Backup
import com.aman.vaak.models.BackupException
import com.aman.vaak.models.Language
import com.aman.vaak.models.Prompt
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@ExtendWith(MockitoExtension::class)
class BackupManagerTest {
    @Mock
    private lateinit var settingsManager: SettingsManager

    @Mock
    private lateinit var fileManager: FileManager

    @Mock
    private lateinit var moshi: Moshi

    @Mock
    private lateinit var promptsManager: PromptsManager

    @Mock
    private lateinit var scope: CoroutineScope

    @Mock
    private lateinit var backupAdapter: com.squareup.moshi.JsonAdapter<Backup>

    private lateinit var backupManager: BackupManagerImpl

    private val testPrompts =
        listOf(
            Prompt("1", "Test prompt 1", "Test content 1"),
            Prompt("2", "Test prompt 2", "Test content 2"),
        )

    @BeforeEach
    fun setup() {
        backupManager =
            BackupManagerImpl(
                settingsManager = settingsManager,
                fileManager = fileManager,
                moshi = moshi,
                promptsManager = promptsManager,
                scope = scope,
            )
    }

    @Nested
    inner class `createBackup` {
        @Test
        fun `creates backup successfully`() =
            TestScope(StandardTestDispatcher()).runTest {
                val mockFile = File("/downloads/vaak_settings_123456.json")
                val expectedJson = """{"version":"1.0","targetLanguage":"en"}"""

                whenever(moshi.adapter(Backup::class.java)).thenReturn(backupAdapter)
                whenever(promptsManager.getPrompts()).thenReturn(testPrompts)
                whenever(settingsManager.getTargetLanguage()).thenReturn(Language.ENGLISH)
                whenever(settingsManager.getFavoriteLanguages()).thenReturn(listOf(Language.ENGLISH))
                whenever(settingsManager.getVoiceInputLanguage()).thenReturn(null)
                whenever(fileManager.getDownloadsFile(any())).thenReturn(mockFile)
                whenever(backupAdapter.toJson(any())).thenReturn(expectedJson)

                val result = backupManager.createBackup()

                assertTrue(result.isSuccess)
                assertEquals(mockFile, result.getOrNull())
                verify(fileManager).write(mockFile, expectedJson)
            }

        @Test
        fun `handles file write error with storage exception`() =
            TestScope(StandardTestDispatcher()).runTest {
                val mockFile = File("/downloads/test.json")
                val expectedJson = """{"version":"1.0"}"""

                whenever(moshi.adapter(Backup::class.java)).thenReturn(backupAdapter)
                whenever(promptsManager.getPrompts()).thenReturn(emptyList())
                whenever(settingsManager.getTargetLanguage()).thenReturn(Language.ENGLISH)
                whenever(settingsManager.getFavoriteLanguages()).thenReturn(listOf(Language.ENGLISH))
                whenever(settingsManager.getVoiceInputLanguage()).thenReturn(null)
                whenever(fileManager.getDownloadsFile(any())).thenReturn(mockFile)
                whenever(backupAdapter.toJson(any())).thenReturn(expectedJson)
                whenever(fileManager.write(any(), any())).thenThrow(RuntimeException("Disk full"))

                val result = backupManager.createBackup()

                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is BackupException.StorageException)
                assertEquals("Backup storage error: Disk full", result.exceptionOrNull()?.message)
            }

        @Test
        fun `handles JSON serialization error`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(moshi.adapter(Backup::class.java)).thenReturn(backupAdapter)
                whenever(promptsManager.getPrompts()).thenReturn(emptyList())
                whenever(settingsManager.getTargetLanguage()).thenReturn(Language.ENGLISH)
                whenever(settingsManager.getFavoriteLanguages()).thenReturn(listOf(Language.ENGLISH))
                whenever(settingsManager.getVoiceInputLanguage()).thenReturn(null)
                whenever(backupAdapter.toJson(any())).thenThrow(RuntimeException("JSON error"))

                val result = backupManager.createBackup()

                assertTrue(result.isFailure)
            }
    }

    @Nested
    inner class `restoreBackup` {
        @Test
        fun `handles invalid backup format`() =
            TestScope(StandardTestDispatcher()).runTest {
                val backupFile = File("/path/to/backup.json")
                val backupJson = """invalid json"""

                whenever(moshi.adapter(Backup::class.java)).thenReturn(backupAdapter)
                whenever(fileManager.read(backupFile)).thenReturn(backupJson)
                whenever(backupAdapter.fromJson(backupJson)).thenReturn(null)

                val result = backupManager.restoreBackup(backupFile)

                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is BackupException.FormatException)
                assertEquals("Backup format error: Invalid backup format", result.exceptionOrNull()?.message)
            }

        @Test
        fun `handles file read error`() =
            TestScope(StandardTestDispatcher()).runTest {
                val backupFile = File("/path/to/backup.json")

                whenever(fileManager.read(backupFile)).thenThrow(RuntimeException("File not found"))

                val result = backupManager.restoreBackup(backupFile)

                assertTrue(result.isFailure)
            }

        @Test
        fun `handles JSON parsing error`() =
            TestScope(StandardTestDispatcher()).runTest {
                val backupFile = File("/path/to/backup.json")
                val backupJson = """{"malformed":"json}"""

                whenever(moshi.adapter(Backup::class.java)).thenReturn(backupAdapter)
                whenever(fileManager.read(backupFile)).thenReturn(backupJson)
                whenever(backupAdapter.fromJson(backupJson)).thenThrow(RuntimeException("JSON parse error"))

                val result = backupManager.restoreBackup(backupFile)

                assertTrue(result.isFailure)
            }
    }

    @Nested
    inner class `backup functionality` {
        @Test
        fun `uses current version in backup`() =
            TestScope(StandardTestDispatcher()).runTest {
                val mockFile = File("/downloads/test.json")

                whenever(moshi.adapter(Backup::class.java)).thenReturn(backupAdapter)
                whenever(promptsManager.getPrompts()).thenReturn(emptyList())
                whenever(settingsManager.getTargetLanguage()).thenReturn(Language.ENGLISH)
                whenever(settingsManager.getFavoriteLanguages()).thenReturn(listOf(Language.ENGLISH))
                whenever(settingsManager.getVoiceInputLanguage()).thenReturn(null)
                whenever(fileManager.getDownloadsFile(any())).thenReturn(mockFile)
                whenever(backupAdapter.toJson(any())).thenReturn("{}")

                backupManager.createBackup()

                verify(backupAdapter).toJson(
                    org.mockito.kotlin.argThat { backup ->
                        backup.version == Backup.CURRENT_VERSION
                    },
                )
            }

        @Test
        fun `generates timestamped filename`() =
            TestScope(StandardTestDispatcher()).runTest {
                val mockFile = File("/downloads/vaak_settings_123456.json")

                whenever(moshi.adapter(Backup::class.java)).thenReturn(backupAdapter)
                whenever(promptsManager.getPrompts()).thenReturn(emptyList())
                whenever(settingsManager.getTargetLanguage()).thenReturn(Language.ENGLISH)
                whenever(settingsManager.getFavoriteLanguages()).thenReturn(listOf(Language.ENGLISH))
                whenever(settingsManager.getVoiceInputLanguage()).thenReturn(null)
                whenever(fileManager.getDownloadsFile(any())).thenReturn(mockFile)
                whenever(backupAdapter.toJson(any())).thenReturn("{}")

                backupManager.createBackup()

                verify(fileManager).getDownloadsFile(
                    org.mockito.kotlin.argThat { filename ->
                        filename.startsWith("vaak_settings_") && filename.endsWith(".json")
                    },
                )
            }

        @Test
        fun `collects all manager data for backup`() =
            TestScope(StandardTestDispatcher()).runTest {
                val mockFile = File("/downloads/test.json")
                val targetLang = Language.SPANISH
                val favLangs = listOf(Language.ENGLISH, Language.FRENCH)
                val voiceLang = Language.GERMAN

                whenever(moshi.adapter(Backup::class.java)).thenReturn(backupAdapter)
                whenever(promptsManager.getPrompts()).thenReturn(testPrompts)
                whenever(settingsManager.getTargetLanguage()).thenReturn(targetLang)
                whenever(settingsManager.getFavoriteLanguages()).thenReturn(favLangs)
                whenever(settingsManager.getVoiceInputLanguage()).thenReturn(voiceLang)
                whenever(fileManager.getDownloadsFile(any())).thenReturn(mockFile)
                whenever(backupAdapter.toJson(any())).thenReturn("{}")

                backupManager.createBackup()

                verify(promptsManager).getPrompts()
                verify(settingsManager).getTargetLanguage()
                verify(settingsManager).getFavoriteLanguages()
                verify(settingsManager).getVoiceInputLanguage()
            }
    }

    @Nested
    inner class `error handling` {
        @Test
        fun `handles storage exception with null message`() =
            TestScope(StandardTestDispatcher()).runTest {
                val mockFile = File("/downloads/test.json")

                whenever(moshi.adapter(Backup::class.java)).thenReturn(backupAdapter)
                whenever(promptsManager.getPrompts()).thenReturn(emptyList())
                whenever(settingsManager.getTargetLanguage()).thenReturn(Language.ENGLISH)
                whenever(settingsManager.getFavoriteLanguages()).thenReturn(listOf(Language.ENGLISH))
                whenever(settingsManager.getVoiceInputLanguage()).thenReturn(null)
                whenever(fileManager.getDownloadsFile(any())).thenReturn(mockFile)
                whenever(backupAdapter.toJson(any())).thenReturn("{}")
                whenever(fileManager.write(any(), any())).thenThrow(RuntimeException())

                val result = backupManager.createBackup()

                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is BackupException.StorageException)
                assertEquals("Backup storage error: Failed to write backup", result.exceptionOrNull()?.message)
            }

        @Test
        fun `createBackup returns failure when promptsManager fails`() =
            TestScope(StandardTestDispatcher()).runTest {
                whenever(promptsManager.getPrompts()).thenThrow(RuntimeException("Manager failure"))

                val result = backupManager.createBackup()

                assertFalse(result.isSuccess)
            }
    }
}
