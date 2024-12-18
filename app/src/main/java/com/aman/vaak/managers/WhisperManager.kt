package com.aman.vaak.managers

import com.aman.vaak.models.WhisperConfig
import com.aman.vaak.models.TranscriptionResult
import com.aman.vaak.models.TranscriptionException
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
     * Returns current active configuration
     * @return Currently active WhisperConfig
     */
    fun getCurrentConfig(): WhisperConfig
    
    /**
     * Updates specific parameters of the configuration
     * Allows partial config updates without affecting other parameters
     * @param update Lambda that modifies current config
     */
    fun updateConfig(update: (WhisperConfig) -> WhisperConfig)
   
    /**
     * Transcribes audio data using Whisper API
     * Uses current active configuration
     * @param file Audio file to transcribe
     * @param language Target language code (ISO 639-1) - overrides config language if provided
     * @return TranscriptionResult on success
     */
    suspend fun transcribeAudio(
        file: File,
        language: String? = null
    ): Result<TranscriptionResult>
   
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
    private var whisperConfig = initializeConfig()
    
    private fun initializeConfig(): WhisperConfig {
        val apiKey = settingsManager.getApiKey()
            ?: throw TranscriptionException.ConfigurationError("API Key not found in settings")
        
        return WhisperConfig(
            apiKey = apiKey
            // Use all other defaults from data class
        )
    }

    override fun getCurrentConfig(): WhisperConfig = whisperConfig

    override fun updateConfig(update: (WhisperConfig) -> WhisperConfig) {
        whisperConfig = update(whisperConfig)
    }

    override suspend fun transcribeAudio(file: File, language: String?): Result<TranscriptionResult> = runCatching {
        withContext(Dispatchers.IO) {
       
            if (!fileManager.isFileValid(file)) {
                throw TranscriptionException.FileError("Audio file does not exist or cannot be read")
            }

            try {
                val audioSource = fileManager.createFileSource(file)
           
                val request = TranscriptionRequest(
                    audio = audioSource,
                    model = ModelId(whisperConfig.model),
                    prompt = whisperConfig.prompt,
                    responseFormat = AudioResponseFormat.Json,
                    temperature = whisperConfig.temperature.toDouble(),
                    language = language ?: whisperConfig.language
                )
           
                val response = openAI.transcription(request)
           
                TranscriptionResult(
                    text = response.text,
                    duration = null
                )
            } catch (e: Exception) {
                throw TranscriptionException.NetworkError(
                    message = "Transcription failed: ${e.message}",
                    cause = e
                )
            }
        }
    }

    override fun release() {
        openAI.close()
    }
}