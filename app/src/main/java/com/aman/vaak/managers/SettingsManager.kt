package com.aman.vaak.managers

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aman.vaak.models.ChatConfig
import com.aman.vaak.models.Language
import com.aman.vaak.models.TranslationConfig
import com.aman.vaak.models.WhisperConfig
import javax.inject.Inject

interface SettingsManager {
    fun getApiKey(): String?

    fun saveApiKey(apiKey: String)

    fun getTargetLanguage(): Language?

    fun saveTargetLanguage(language: Language?)

    fun getFavoriteLanguages(): List<Language>

    fun saveFavoriteLanguages(languages: List<Language>)

    fun getWhisperConfig(): WhisperConfig

    fun getChatConfig(): ChatConfig

    fun getTranslationConfig(): TranslationConfig

    fun getVoiceInputLanguage(): Language? // null means auto-detect

    fun saveVoiceInputLanguage(language: Language?)

    fun getTranslationModel(): String

    fun saveTranslationModel(model: String)

    fun getTranslationPrompt(): String

    fun saveTranslationPrompt(prompt: String)

    fun resetAllTranslationSettingsToDefault()
}

class SettingsManagerImpl
    @Inject
    constructor(context: Context) : SettingsManager {
        private companion object {
            const val KEY_API_KEY = "api_key"
            const val KEY_TARGET_LANGUAGE = "target_language"
            const val KEY_FAVORITE_LANGUAGES = "favorite_languages"
            const val KEY_VOICE_INPUT_LANGUAGE = "voice_input_language"
            const val KEY_TRANSLATION_MODEL = "translation_model"
            const val KEY_TRANSLATION_PROMPT = "translation_prompt"
            const val DEFAULT_LANGUAGE = "en"
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

        override fun getVoiceInputLanguage(): Language? {
            val code = sharedPreferences.getString(KEY_VOICE_INPUT_LANGUAGE, null)
            return code?.let { Language.fromCode(it) }
        }

        override fun saveVoiceInputLanguage(language: Language?) {
            sharedPreferences.edit()
                .putString(KEY_VOICE_INPUT_LANGUAGE, language?.code)
                .apply()
        }

        override fun saveApiKey(apiKey: String) {
            sharedPreferences.edit().putString(KEY_API_KEY, apiKey).apply()
        }

        override fun getTargetLanguage(): Language? {
            val code = sharedPreferences.getString(KEY_TARGET_LANGUAGE, null)
            return code?.let { Language.fromCode(it) }
        }

        override fun saveTargetLanguage(language: Language?) {
            sharedPreferences.edit()
                .putString(KEY_TARGET_LANGUAGE, language?.code)
                .apply()
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
                language = getVoiceInputLanguage()?.code,
            )
        }

        override fun getChatConfig(): ChatConfig {
            return ChatConfig(
                model = getTranslationModel(),
                systemPrompt = getTranslationPrompt(),
            )
        }

        override fun getTranslationConfig(): TranslationConfig {
            return TranslationConfig(
                model = getTranslationModel(),
                systemPrompt = getTranslationPrompt(),
            )
        }

        override fun getTranslationModel(): String {
            return sharedPreferences.getString(KEY_TRANSLATION_MODEL, null)
                ?: TranslationConfig.DEFAULT_TRANSLATION_MODEL
        }

        override fun saveTranslationModel(model: String) {
            sharedPreferences.edit()
                .putString(KEY_TRANSLATION_MODEL, model)
                .apply()
        }

        override fun getTranslationPrompt(): String {
            return sharedPreferences.getString(KEY_TRANSLATION_PROMPT, null)
                ?: TranslationConfig.DEFAULT_TRANSLATION_PROMPT
        }

        override fun saveTranslationPrompt(prompt: String) {
            sharedPreferences.edit()
                .putString(KEY_TRANSLATION_PROMPT, prompt)
                .apply()
        }

        override fun resetAllTranslationSettingsToDefault() {
            sharedPreferences.edit()
                .remove(KEY_TRANSLATION_MODEL)
                .remove(KEY_TRANSLATION_PROMPT)
                .apply()
        }
    }
