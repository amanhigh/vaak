package com.aman.vaak.managers

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject

interface SettingsManager {
    fun getApiKey(): String?

    fun saveApiKey(apiKey: String)
}

class SettingsManagerImpl
    @Inject
    constructor(context: Context) : SettingsManager {
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
            return sharedPreferences.getString("api_key", null)
        }

        override fun saveApiKey(apiKey: String) {
            sharedPreferences.edit().putString("api_key", apiKey).apply()
        }
    }
