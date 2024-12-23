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

    fun isFileValid(file: File): Boolean

    fun fileExists(file: File): Boolean

    fun getFileSize(file: File): Long

    fun validateAudioFile(
        file: File,
        maxSize: Long,
    ): Result<Unit>
}

// FIXME: Update FileManager tests after MediaRecorder changes
class FileManagerImpl
    @Inject
    constructor(private val context: Context) : FileManager {
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
        override fun isFileValid(file: File): Boolean = fileExists(file) && file.canRead()

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
                if (!isFileValid(file)) {
                    throw VaakFileException.invalidFormat()
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
