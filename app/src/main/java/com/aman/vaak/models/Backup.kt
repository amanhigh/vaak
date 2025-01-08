package com.aman.vaak.models

data class Backup(
    val timestamp: Long = System.currentTimeMillis(),
    val version: String,
    val targetLanguage: String?,
    val favoriteLanguages: List<String>,
    val voiceInputLanguage: String?,
) {
    companion object {
        const val CURRENT_VERSION = "1.0"
        val SUPPORTED_VERSIONS = setOf("1.0")
    }

    fun validate() {
        if (version !in SUPPORTED_VERSIONS) {
            throw BackupException.VersionException(version)
        }

        val invalidCodes = mutableListOf<String>()

        favoriteLanguages.forEach { code ->
            if (!isValidLanguageCode(code)) {
                invalidCodes.add("favorite: $code")
            }
        }

        targetLanguage?.let { code ->
            if (!isValidLanguageCode(code)) {
                invalidCodes.add("target: $code")
            }
        }

        voiceInputLanguage?.let { code ->
            if (!isValidLanguageCode(code)) {
                invalidCodes.add("voice: $code")
            }
        }

        if (invalidCodes.isNotEmpty()) {
            throw BackupException.ValidationException("Invalid language codes: ${invalidCodes.joinToString()}")
        }
    }

    private fun isValidLanguageCode(code: String): Boolean = Language.values().any { it.code == code }
}

sealed class BackupException(message: String) : Exception(message) {
    class StorageException(message: String) :
        BackupException("Backup storage error: $message")

    class FormatException(message: String) :
        BackupException("Backup format error: $message")

    class ValidationException(message: String) :
        BackupException("Backup validation failed: $message")

    class VersionException(version: String) :
        BackupException("Incompatible backup version: $version")
}
