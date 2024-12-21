package com.aman.vaak.managers

import android.content.Context
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.file.fileSource
import okio.source
import java.io.File
import java.io.IOException
import javax.inject.Inject

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
}

class FileManagerImpl
    @Inject
    constructor(private val context: Context) : FileManager {
        override fun isFileValid(file: File): Boolean = file.exists() && file.canRead()

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
        override fun deleteAudioFile(file: File): Boolean = if (file.exists()) file.delete() else false
    }
