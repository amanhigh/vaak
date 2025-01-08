package com.aman.vaak.managers

import com.aman.vaak.models.Backup
import com.aman.vaak.models.BackupException
import com.aman.vaak.models.Language
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

interface BackupManager {
    suspend fun createBackup(): Result<File>

    suspend fun restoreBackup(file: File): Result<Unit>
}

class BackupManagerImpl
    @Inject
    constructor(
        private val settingsManager: SettingsManager,
        private val fileManager: FileManager,
        private val moshi: Moshi,
    ) : BackupManager {
        override suspend fun createBackup(): Result<File> =
            runCatching {
                withContext(Dispatchers.IO) {
                    val backup =
                        Backup(
                            version = Backup.CURRENT_VERSION,
                            targetLanguage = settingsManager.getTargetLanguage()?.code,
                            favoriteLanguages = settingsManager.getFavoriteLanguages().map { it.code },
                            voiceInputLanguage = settingsManager.getVoiceInputLanguage()?.code,
                        )

                    val adapter = moshi.adapter(Backup::class.java)
                    val json = adapter.toJson(backup)

                    val filename = "vaak_settings_${backup.timestamp}.json"
                    val file = fileManager.getDownloadsFile(filename)

                    try {
                        fileManager.write(file, json)
                        file
                    } catch (e: Exception) {
                        throw BackupException.StorageException(e.message ?: "Failed to write backup")
                    }
                }
            }

        override suspend fun restoreBackup(file: File): Result<Unit> =
            runCatching {
                withContext(Dispatchers.IO) {
                    val json = fileManager.read(file)

                    val backup =
                        moshi.adapter(Backup::class.java).fromJson(json)
                            ?: throw BackupException.FormatException("Invalid backup format")

                    backup.validate()
                    applyBackup(backup)
                }
            }

        private fun applyBackup(backup: Backup) {
            val favorites = backup.favoriteLanguages.map { Language.fromCode(it) }
            val target = backup.targetLanguage?.let { Language.fromCode(it) } ?: Language.ENGLISH
            val voiceInput = backup.voiceInputLanguage?.let { Language.fromCode(it) }

            settingsManager.saveFavoriteLanguages(favorites)
            settingsManager.saveTargetLanguage(target)
            settingsManager.saveVoiceInputLanguage(voiceInput)
        }
    }
