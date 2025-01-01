package com.aman.vaak.managers

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject

interface SettingsManager {
    fun getApiKey(): String?

    fun saveApiKey(apiKey: String)

    fun getTargetLanguage(): String

    fun saveTargetLanguage(language: String)
}

class SettingsManagerImpl
    @Inject
    constructor(context: Context) : SettingsManager {
        private companion object {
            const val KEY_API_KEY = "api_key"
            const val KEY_TARGET_LANGUAGE = "target_language"
            const val DEFAULT_LANGUAGE = "EN"
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

        override fun getTargetLanguage(): String {
            return sharedPreferences.getString(KEY_TARGET_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        }

        override fun saveTargetLanguage(language: String) {
            sharedPreferences.edit().putString(KEY_TARGET_LANGUAGE, language).apply()
        }
    }
