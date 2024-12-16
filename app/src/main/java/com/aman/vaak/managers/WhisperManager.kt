package com.aman.vaak.managers

import com.aman.vaak.models.WhisperConfig
import com.aman.vaak.models.WhisperResult
import com.aman.vaak.models.WhisperResponseFormat
import com.aallam.openai.api.audio.AudioResponseFormat
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.audio.Transcription
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.file.fileSource
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.source
import java.io.File
import javax.inject.Inject

/**
* Manager interface for handling Whisper API transcription requests
*/
interface WhisperManager {
   /**
    * Updates the configuration for Whisper API requests
    * Must be called before making transcription requests
    * @param config New configuration to be used for future requests
    */
   fun updateConfig(config: WhisperConfig)
   
   /**
    * Transcribes audio data using Whisper API
    * @param file The audio file to be transcribed
    * @param language Target language for transcription (ISO 639-1), defaults to "en"
    * @return WhisperResult containing either transcribed text or error details
    */
   suspend fun transcribeAudio(
       file: File,
       language: String = "en"
   ): WhisperResult
   
   /**
    * Releases resources held by the manager
    * Should be called when manager is no longer needed
    */
   fun release()
}

/**
* Implementation of WhisperManager that handles audio transcription using OpenAI's Whisper API
*/
class WhisperManagerImpl @Inject constructor(
   private val openAI: OpenAI,
   private val settingsManager: SettingsManager,
   private val fileManager: FileManager
) : WhisperManager {
   private var currentConfig: WhisperConfig? = null
   
   override fun updateConfig(config: WhisperConfig) {
       currentConfig = config
   }

   override suspend fun transcribeAudio(
       file: File,
       language: String
   ): WhisperResult = withContext(Dispatchers.IO) {
       val config = currentConfig ?: return@withContext WhisperResult.Error.ConfigurationError(
           message = "Whisper configuration not initialized"
       )
       
       try {
           if (!fileManager.isFileValid(file)) {
               return@withContext WhisperResult.Error.FileError(
                   message = "Audio file does not exist or cannot be read"
               )
           }

           val audioSource = fileManager.createFileSource(file)
           
           val request = TranscriptionRequest(
               audio = audioSource,
               model = ModelId(config.model),
               prompt = config.prompt,
               responseFormat = when(config.responseFormat) {
                   WhisperResponseFormat.JSON -> AudioResponseFormat.Json
                   WhisperResponseFormat.TEXT -> AudioResponseFormat.Text
               },
               temperature = config.temperature.toDouble(),
               language = language
           )
           
           val response: Transcription = openAI.transcription(request)
           
           WhisperResult.Success(
               text = response.text,
               language = language,
               duration = null
           )
           
       } catch (e: Exception) {
           WhisperResult.Error.NetworkError(
               message = "Transcription failed: ${e.message}",
               cause = e
           )
       }
   }

   override fun release() {
       openAI.close()
   }
}
