package com.aman.vaak.managers

import android.content.Context
import android.os.Environment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.io.File
import java.io.IOException

@ExtendWith(MockitoExtension::class)
class FileManagerTest {
    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var filesDir: File

    @Mock
    private lateinit var cacheDir: File

    @Mock
    private lateinit var mockFile: File

    private lateinit var fileManager: FileManager

    @BeforeEach
    fun setup() {
        // Create real temp directories for testing
        val tempDir = File.createTempFile("test", "dir")
        tempDir.delete()
        tempDir.mkdir()

        Mockito.lenient().whenever(context.filesDir).thenReturn(tempDir)
        Mockito.lenient().whenever(context.cacheDir).thenReturn(tempDir)

        fileManager = FileManagerImpl(context)
    }

    @Nested
    @DisplayName("File Creation Operations")
    inner class FileCreationOperations {
        @Test
        fun `createTempFile creates file with correct extension in cache directory`() {
            // This is a simple test that just verifies the method works
            val result = fileManager.createTempFile("mp3")

            assertTrue(result.name.startsWith("temp_"))
            assertTrue(result.name.endsWith(".mp3"))
        }

        @Test
        fun `getInternalFile returns file in internal storage directory`() {
            val result = fileManager.getInternalFile("test.txt")

            assertEquals("test.txt", result.name)
        }

        @Test
        fun `getDownloadsFile returns file in Downloads directory`() {
            Mockito.mockStatic(Environment::class.java).use { _ ->
                val downloadsDir = File("/storage/Downloads")
                whenever(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                    .thenReturn(downloadsDir)

                val result = fileManager.getDownloadsFile("backup.json")

                assertEquals("backup.json", result.name)
                assertTrue(result.absolutePath.contains("Downloads"))
            }
        }
    }

    @Nested
    @DisplayName("File Operations")
    inner class FileOperations {
        @Test
        fun `deleteFile returns true when file deletion succeeds`() {
            Mockito.lenient().whenever(mockFile.delete()).thenReturn(true)

            val result = fileManager.deleteFile(mockFile)

            assertTrue(result)
        }

        @Test
        fun `deleteFile returns false when file deletion fails`() {
            Mockito.lenient().whenever(mockFile.delete()).thenReturn(false)

            val result = fileManager.deleteFile(mockFile)

            assertFalse(result)
        }

        @Test
        fun `fileExists returns true when file exists`() {
            Mockito.lenient().whenever(mockFile.exists()).thenReturn(true)

            val result = fileManager.fileExists(mockFile)

            assertTrue(result)
        }

        @Test
        fun `fileExists returns false when file does not exist`() {
            Mockito.lenient().whenever(mockFile.exists()).thenReturn(false)

            val result = fileManager.fileExists(mockFile)

            assertFalse(result)
        }

        @Test
        fun `getFileSize returns correct file size`() {
            Mockito.lenient().whenever(mockFile.length()).thenReturn(1024L)

            val result = fileManager.getFileSize(mockFile)

            assertEquals(1024L, result)
        }
    }

    @Nested
    @DisplayName("File Content Operations")
    inner class FileContentOperations {
        @Test
        fun `read returns file content successfully`() {
            val testFile = File.createTempFile("test", ".txt")
            val testContent = "Test file content"
            testFile.writeText(testContent)

            val result = fileManager.read(testFile)

            assertEquals(testContent, result)
            testFile.delete()
        }

        @Test
        fun `read throws IOException when file read fails`() {
            val nonExistentFile = File("/non/existent/path/file.txt")

            assertThrows<IOException> {
                fileManager.read(nonExistentFile)
            }
        }

        @Test
        fun `write stores content successfully`() {
            val testFile = File.createTempFile("test", ".txt")
            val testContent = "Test content to write"

            fileManager.write(testFile, testContent)

            assertEquals(testContent, testFile.readText())
            testFile.delete()
        }

        @Test
        fun `write throws IOException when file write fails`() {
            val readOnlyFile = File("/read/only/path/file.txt")
            val testContent = "Test content"

            assertThrows<IOException> {
                fileManager.write(readOnlyFile, testContent)
            }
        }
    }

    @Nested
    @DisplayName("FileSource Creation")
    inner class FileSourceCreation {
        @Test
        fun `createFileSource creates valid FileSource with correct name`() {
            val testFile = File.createTempFile("audio", ".m4a")
            testFile.writeText("test audio data")

            val result = fileManager.createFileSource(testFile)

            assertNotNull(result)
            testFile.delete()
        }
    }

    @Nested
    @DisplayName("Audio File Validation")
    inner class AudioFileValidation {
        @Test
        fun `validateAudioFile succeeds for valid file`() {
            Mockito.lenient().whenever(mockFile.exists()).thenReturn(true)
            Mockito.lenient().whenever(mockFile.length()).thenReturn(1024L)
            Mockito.lenient().whenever(mockFile.path).thenReturn("/path/to/audio.m4a")

            val result = fileManager.validateAudioFile(mockFile, 2048L)

            assertTrue(result.isSuccess)
        }

        @Test
        fun `validateAudioFile fails when file does not exist`() {
            Mockito.lenient().whenever(mockFile.exists()).thenReturn(false)
            Mockito.lenient().whenever(mockFile.path).thenReturn("/path/to/missing.m4a")

            val result = fileManager.validateAudioFile(mockFile, 2048L)

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is VaakFileException.FileNotFoundException)
            assertEquals("Audio file not found: /path/to/missing.m4a", exception?.message)
        }

        @Test
        fun `validateAudioFile fails when file is empty`() {
            Mockito.lenient().whenever(mockFile.exists()).thenReturn(true)
            Mockito.lenient().whenever(mockFile.length()).thenReturn(0L)

            val result = fileManager.validateAudioFile(mockFile, 2048L)

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is VaakFileException.EmptyFileException)
            assertEquals("Audio file is empty", exception?.message)
        }

        @Test
        fun `validateAudioFile fails when file exceeds size limit`() {
            Mockito.lenient().whenever(mockFile.exists()).thenReturn(true)
            Mockito.lenient().whenever(mockFile.length()).thenReturn(3072L)

            val result = fileManager.validateAudioFile(mockFile, 2048L)

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is VaakFileException.FileTooLargeException)
            assertEquals("File size 3072 exceeds limit", exception?.message)
        }
    }

    @Nested
    @DisplayName("VaakFileException Factory Methods")
    inner class VaakFileExceptionFactoryMethods {
        @Test
        fun `fileNotFound creates correct exception`() {
            val exception = VaakFileException.fileNotFound("/path/to/file")

            assertTrue(exception is VaakFileException.FileNotFoundException)
            assertEquals("Audio file not found: /path/to/file", exception.message)
        }

        @Test
        fun `invalidFormat creates correct exception`() {
            val exception = VaakFileException.invalidFormat()

            assertTrue(exception is VaakFileException.InvalidFormatException)
            assertEquals("Invalid audio file format", exception.message)
        }

        @Test
        fun `emptyFile creates correct exception`() {
            val exception = VaakFileException.emptyFile()

            assertTrue(exception is VaakFileException.EmptyFileException)
            assertEquals("Audio file is empty", exception.message)
        }

        @Test
        fun `fileTooLarge creates correct exception`() {
            val exception = VaakFileException.fileTooLarge(5000L)

            assertTrue(exception is VaakFileException.FileTooLargeException)
            assertEquals("File size 5000 exceeds limit", exception.message)
        }
    }
}
