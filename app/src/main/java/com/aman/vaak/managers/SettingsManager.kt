package com.aman.vaak.managers

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aman.vaak.models.ChatConfig
import com.aman.vaak.models.Language
import com.aman.vaak.models.WhisperConfig
import javax.inject.Inject

interface SettingsManager {
    fun getApiKey(): String?

    fun saveApiKey(apiKey: String)

    fun getTargetLanguage(): Language

    fun saveTargetLanguage(language: Language)

    fun getFavoriteLanguages(): List<Language>

    fun saveFavoriteLanguages(languages: List<Language>)

    fun getWhisperConfig(): WhisperConfig

    fun getChatConfig(): ChatConfig
}

class SettingsManagerImpl
    @Inject
    constructor(context: Context) : SettingsManager {
        private companion object {
            const val KEY_API_KEY = "api_key"
            const val KEY_TARGET_LANGUAGE = "target_language"
            const val KEY_FAVORITE_LANGUAGES = "favorite_languages"
            const val DEFAULT_LANGUAGE = "en"

            // Default configurations
            const val DEFAULT_WHISPER_MODEL = "whisper-1"
            const val DEFAULT_WHISPER_TEMPERATURE = 0.2f
            const val DEFAULT_BASE_ENDPOINT = "https://api.openai.com/v1"
            const val DEFAULT_CHAT_MODEL = "gpt-4o-mini"
            const val MAX_AUDIO_FILE_SIZE = 25 * 1024 * 1024L // 25MB
        }

        private val masterKey =
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

        private val sharedPreferences =
            EncryptedSharedPreferences.create(
                context,
                "secret_shared_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )

        override fun getApiKey(): String? {
            return sharedPreferences.getString(KEY_API_KEY, null)
        }

        override fun saveApiKey(apiKey: String) {
            sharedPreferences.edit().putString(KEY_API_KEY, apiKey).apply()
        }

        override fun getTargetLanguage(): Language {
            val code = sharedPreferences.getString(KEY_TARGET_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
            return Language.fromCode(code)
        }

        override fun saveTargetLanguage(language: Language) {
            sharedPreferences.edit().putString(KEY_TARGET_LANGUAGE, language.code).apply()
        }

        override fun getFavoriteLanguages(): List<Language> {
            val saved = sharedPreferences.getString(KEY_FAVORITE_LANGUAGES, null)
            return if (saved.isNullOrEmpty()) {
                listOf(Language.ENGLISH)
            } else {
                saved.split(",")
                    .mapNotNull { code ->
                        Language.values()
                            .find { it.code == code }
                    }
                    .takeIf { it.isNotEmpty() }
                    ?: listOf(Language.ENGLISH)
            }
        }

        override fun saveFavoriteLanguages(languages: List<Language>) {
            val languageCodes = languages.joinToString(",") { it.code }
            sharedPreferences.edit().putString(KEY_FAVORITE_LANGUAGES, languageCodes).apply()
        }

        override fun getWhisperConfig(): WhisperConfig {
            return WhisperConfig(
                model = DEFAULT_WHISPER_MODEL,
                temperature = DEFAULT_WHISPER_TEMPERATURE,
                baseEndpoint = DEFAULT_BASE_ENDPOINT,
                maxFileSize = MAX_AUDIO_FILE_SIZE,
                systemPrompt = WhisperConfig.DEFAULT_TRANSCRIPTION_PROMPT,
            )
        }

        override fun getChatConfig(): ChatConfig {
            return ChatConfig(
                model = DEFAULT_CHAT_MODEL,
                baseEndpoint = DEFAULT_BASE_ENDPOINT,
                systemPrompt = ChatConfig.DEFAULT_TRANSLATION_PROMPT,
            )
        }
    }
