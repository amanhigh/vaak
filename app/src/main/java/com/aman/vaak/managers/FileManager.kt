package com.aman.vaak.managers

import android.content.Context
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.file.fileSource
import okio.source
import java.io.File
import javax.inject.Inject

// File Validation Errors
sealed class VaakFileException(message: String) : TranscriptionException(message) {
    class FileNotFoundException(path: String) :
        VaakFileException("Audio file not found: $path")

    class InvalidFormatException :
        VaakFileException("Invalid audio file format")

    class EmptyFileException :
        VaakFileException("Audio file is empty")

    class FileTooLargeException(size: Long) :
        VaakFileException("File size $size exceeds limit")

    companion object {
        fun fileNotFound(path: String) = FileNotFoundException(path)

        fun invalidFormat() = InvalidFormatException()

        fun emptyFile() = EmptyFileException()

        fun fileTooLarge(size: Long) = FileTooLargeException(size)
    }
}

interface FileManager {
    fun createTempFile(extension: String): File

    fun deleteFile(file: File): Boolean

    fun createFileSource(file: File): FileSource

    fun fileExists(file: File): Boolean

    fun getFileSize(file: File): Long

    /**
     * Gets or creates a file in app's internal storage directory
     * @param filename Name of the file to get/create
     * @return File object in internal storage
     */
    fun getInternalFile(filename: String): File

    fun validateAudioFile(
        file: File,
        maxSize: Long,
    ): Result<Unit>

    /**
     * Reads content from a file
     * @param file File to read from
     * @return Content of file as string
     * @throws IOException if read fails
     */
    fun read(file: File): String

    /**
     * Writes content to a file
     * @param file File to write to
     * @param content Content to write
     * @throws IOException if write fails
     */
    fun write(
        file: File,
        content: String,
    )
}

class FileManagerImpl
    @Inject
    constructor(private val context: Context) : FileManager {
        override fun read(file: File): String = file.readText()

        override fun write(
            file: File,
            content: String,
        ) = file.writeText(content)

        override fun getInternalFile(filename: String): File = File(context.filesDir, filename)

        override fun createTempFile(extension: String): File {
            val filename = "temp_${System.currentTimeMillis()}.$extension"
            return File(context.cacheDir, filename)
        }

        override fun deleteFile(file: File): Boolean = file.delete()

        override fun createFileSource(file: File): FileSource =
            fileSource {
                name = file.name
                source = file.inputStream().source()
            }

        // Legacy methods below - To be reviewed for removal
        override fun fileExists(file: File): Boolean = file.exists()

        override fun getFileSize(file: File): Long = file.length()

        override fun validateAudioFile(
            file: File,
            maxSize: Long,
        ): Result<Unit> =
            runCatching {
                if (!fileExists(file)) {
                    throw VaakFileException.fileNotFound(file.path)
                }

                val size = getFileSize(file)
                if (size == 0L) {
                    throw VaakFileException.emptyFile()
                }
                if (size > maxSize) {
                    throw VaakFileException.fileTooLarge(size)
                }
            }
    }
