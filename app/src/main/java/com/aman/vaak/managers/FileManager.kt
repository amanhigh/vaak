package com.aman.vaak.managers

import android.content.Context
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.file.fileSource
import okio.source
import java.io.File
import java.io.IOException
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
    /**
     * Checks if given file exists and is readable
     * @param file File to validate
     * @return true if file exists and is readable, false otherwise
     */
    fun isFileValid(file: File): Boolean

    /**
     * Creates FileSource instance from given file for API usage
     * @param file Source file to convert
     * @return FileSource instance ready for API consumption
     * @throws IOException if file is not accessible
     */
    fun createFileSource(file: File): FileSource

    /**
     * Saves audio data to temporary file with specified extension
     * @param data Raw audio data to save
     * @param extension File extension to use (e.g. "wav", "mp3")
     * @return File reference to saved audio
     * @throws IOException if file creation fails
     */
    fun saveAudioFile(
        data: ByteArray,
        extension: String,
    ): File

    /**
     * Deletes audio file
     * @param file File to delete
     * @return true if deletion successful, false otherwise
     */
    fun deleteAudioFile(file: File): Boolean

    /**
     * Checks if file exists on filesystem
     * @param file File to check
     * @return true if file exists, false otherwise
     */
    fun fileExists(file: File): Boolean

    /**
     * Gets size of file in bytes
     * @param file File to check
     * @return Size of file in bytes
     */
    fun getFileSize(file: File): Long

    /**
     * Validates audio file meets all requirements
     * @param file File to validate
     * @param maxSize Maximum allowed file size in bytes
     * @return Result.success if valid, Result.failure with AudioFileException if invalid
     */
    fun validateAudioFile(
        file: File,
        maxSize: Long,
    ): Result<Unit>
}

class FileManagerImpl
    @Inject
    constructor(private val context: Context) : FileManager {
        override fun isFileValid(file: File): Boolean = fileExists(file) && file.canRead()

        override fun createFileSource(file: File): FileSource =
            fileSource {
                name = file.name
                source = file.inputStream().source()
            }

        override fun saveAudioFile(
            data: ByteArray,
            extension: String,
        ): File {
            val tempFile = File(context.cacheDir, "audio_${System.currentTimeMillis()}.$extension")
            tempFile.writeBytes(data)
            return tempFile
        }

        // FIXME: Cleanup Temp Files
        override fun deleteAudioFile(file: File): Boolean = if (fileExists(file)) file.delete() else false

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
