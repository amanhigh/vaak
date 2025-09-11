package com.aman.vaak.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BackupTest {
    @Nested
    inner class DefaultValues {
        @Test
        fun `default constructor sets current timestamp`() {
            val beforeCreation = System.currentTimeMillis()
            val backup =
                Backup(
                    version = "1.0",
                    targetLanguage = null,
                    favoriteLanguages = emptyList(),
                    voiceInputLanguage = null,
                )
            val afterCreation = System.currentTimeMillis()

            assertTrue(backup.timestamp >= beforeCreation)
            assertTrue(backup.timestamp <= afterCreation)
        }

        @Test
        fun `default prompts list is empty`() {
            val backup =
                Backup(
                    version = "1.0",
                    targetLanguage = null,
                    favoriteLanguages = emptyList(),
                    voiceInputLanguage = null,
                )

            assertTrue(backup.prompts.isEmpty())
        }
    }

    @Nested
    inner class BackupCreation {
        @Test
        fun `creates backup with all properties correctly`() {
            val timestamp = 12345L
            val version = "1.0"
            val targetLanguage = "en"
            val favoriteLanguages = listOf("en", "hi", "es")
            val voiceInputLanguage = "hi"
            val prompts =
                listOf(
                    Prompt(name = "Test1", content = "Content1"),
                    Prompt(name = "Test2", content = "Content2"),
                )

            val backup =
                Backup(
                    timestamp = timestamp,
                    version = version,
                    targetLanguage = targetLanguage,
                    favoriteLanguages = favoriteLanguages,
                    voiceInputLanguage = voiceInputLanguage,
                    prompts = prompts,
                )

            assertEquals(timestamp, backup.timestamp)
            assertEquals(version, backup.version)
            assertEquals(targetLanguage, backup.targetLanguage)
            assertEquals(favoriteLanguages, backup.favoriteLanguages)
            assertEquals(voiceInputLanguage, backup.voiceInputLanguage)
            assertEquals(prompts, backup.prompts)
        }

        @Test
        fun `creates minimal backup correctly`() {
            val backup =
                Backup(
                    version = "1.0",
                    targetLanguage = null,
                    favoriteLanguages = emptyList(),
                    voiceInputLanguage = null,
                )

            assertEquals("1.0", backup.version)
            assertEquals(null, backup.targetLanguage)
            assertTrue(backup.favoriteLanguages.isEmpty())
            assertEquals(null, backup.voiceInputLanguage)
            assertTrue(backup.prompts.isEmpty())
        }
    }

    @Nested
    inner class ValidationTests {
        @Test
        fun `validate passes for valid backup`() {
            val backup =
                Backup(
                    version = "1.0",
                    targetLanguage = "en",
                    favoriteLanguages = listOf("en", "hi", "es"),
                    voiceInputLanguage = "hi",
                )

            // Should not throw exception
            backup.validate()
        }

        @Test
        fun `validate passes for minimal valid backup`() {
            val backup =
                Backup(
                    version = "1.0",
                    targetLanguage = null,
                    favoriteLanguages = emptyList(),
                    voiceInputLanguage = null,
                )

            // Should not throw exception
            backup.validate()
        }

        @Test
        fun `validate throws VersionException for invalid version`() {
            val backup =
                Backup(
                    version = "2.0",
                    targetLanguage = null,
                    favoriteLanguages = emptyList(),
                    voiceInputLanguage = null,
                )

            val exception =
                assertThrows(BackupException.VersionException::class.java) {
                    backup.validate()
                }
            assertTrue(exception.message?.contains("2.0") == true)
        }

        @Test
        fun `validate throws VersionException for unsupported version`() {
            val backup =
                Backup(
                    version = "0.9",
                    targetLanguage = null,
                    favoriteLanguages = emptyList(),
                    voiceInputLanguage = null,
                )

            assertThrows(BackupException.VersionException::class.java) {
                backup.validate()
            }
        }

        @Test
        fun `validate throws ValidationException for invalid favorite language`() {
            val backup =
                Backup(
                    version = "1.0",
                    targetLanguage = null,
                    favoriteLanguages = listOf("en", "invalid", "hi"),
                    voiceInputLanguage = null,
                )

            val exception =
                assertThrows(BackupException.ValidationException::class.java) {
                    backup.validate()
                }
            assertTrue(exception.message?.contains("favorite: invalid") == true)
        }

        @Test
        fun `validate throws ValidationException for invalid target language`() {
            val backup =
                Backup(
                    version = "1.0",
                    targetLanguage = "invalid",
                    favoriteLanguages = emptyList(),
                    voiceInputLanguage = null,
                )

            val exception =
                assertThrows(BackupException.ValidationException::class.java) {
                    backup.validate()
                }
            assertTrue(exception.message?.contains("target: invalid") == true)
        }

        @Test
        fun `validate throws ValidationException for invalid voice input language`() {
            val backup =
                Backup(
                    version = "1.0",
                    targetLanguage = null,
                    favoriteLanguages = emptyList(),
                    voiceInputLanguage = "invalid",
                )

            val exception =
                assertThrows(BackupException.ValidationException::class.java) {
                    backup.validate()
                }
            assertTrue(exception.message?.contains("voice: invalid") == true)
        }

        @Test
        fun `validate throws ValidationException with multiple invalid languages`() {
            val backup =
                Backup(
                    version = "1.0",
                    targetLanguage = "badtarget",
                    favoriteLanguages = listOf("en", "badfav", "hi"),
                    voiceInputLanguage = "badvoice",
                )

            val exception =
                assertThrows(BackupException.ValidationException::class.java) {
                    backup.validate()
                }
            val message = exception.message ?: ""
            assertTrue(message.contains("target: badtarget"))
            assertTrue(message.contains("favorite: badfav"))
            assertTrue(message.contains("voice: badvoice"))
        }

        @Test
        fun `validate passes with all supported languages`() {
            val allLanguageCodes = Language.values().map { it.code }
            val backup =
                Backup(
                    version = "1.0",
                    targetLanguage = "en",
                    favoriteLanguages = allLanguageCodes.take(3),
                    voiceInputLanguage = "hi",
                )

            // Should not throw exception
            backup.validate()
        }
    }

    @Nested
    inner class LanguageCodeValidation {
        @Test
        fun `isValidLanguageCode returns true for all valid codes`() {
            val backup =
                Backup(
                    version = "1.0",
                    targetLanguage = null,
                    favoriteLanguages = emptyList(),
                    voiceInputLanguage = null,
                )

            Language.values().forEach { language ->
                // All valid language codes should pass validation
                backup.copy(favoriteLanguages = listOf(language.code)).validate()
            }
        }

        @Test
        fun `isValidLanguageCode returns false for invalid codes`() {
            val invalidCodes = listOf("xx", "invalid", "", "toolong", "12")

            invalidCodes.forEach { code ->
                val backup =
                    Backup(
                        version = "1.0",
                        targetLanguage = null,
                        favoriteLanguages = listOf(code),
                        voiceInputLanguage = null,
                    )

                assertThrows(BackupException.ValidationException::class.java) {
                    backup.validate()
                }
            }
        }
    }

    @Nested
    inner class CompanionObjectTests {
        @Test
        fun `CURRENT_VERSION is correct`() {
            assertEquals("1.0", Backup.CURRENT_VERSION)
        }

        @Test
        fun `SUPPORTED_VERSIONS contains current version`() {
            assertTrue(Backup.SUPPORTED_VERSIONS.contains(Backup.CURRENT_VERSION))
        }

        @Test
        fun `SUPPORTED_VERSIONS has expected size`() {
            assertEquals(1, Backup.SUPPORTED_VERSIONS.size)
        }

        @Test
        fun `SUPPORTED_VERSIONS contains only valid versions`() {
            Backup.SUPPORTED_VERSIONS.forEach { version ->
                assertNotNull(version)
                assertTrue(version.isNotBlank())
            }
        }
    }

    @Nested
    inner class DataClassBehavior {
        @Test
        fun `equals works correctly`() {
            val backup1 =
                Backup(
                    timestamp = 1000L,
                    version = "1.0",
                    targetLanguage = "en",
                    favoriteLanguages = listOf("en", "hi"),
                    voiceInputLanguage = "hi",
                    prompts = listOf(Prompt(name = "Test", content = "Content")),
                )

            val backup2 = backup1.copy()

            assertEquals(backup1, backup2)
        }

        @Test
        fun `copy function works correctly`() {
            val original =
                Backup(
                    version = "1.0",
                    targetLanguage = "en",
                    favoriteLanguages = listOf("en", "hi"),
                    voiceInputLanguage = "hi",
                )

            val copied = original.copy(version = "1.0", targetLanguage = "es")

            assertEquals("1.0", copied.version)
            assertEquals("es", copied.targetLanguage)
            assertEquals(original.favoriteLanguages, copied.favoriteLanguages)
            assertEquals(original.voiceInputLanguage, copied.voiceInputLanguage)
        }

        @Test
        fun `toString includes key information`() {
            val backup =
                Backup(
                    version = "1.0",
                    targetLanguage = "en",
                    favoriteLanguages = listOf("en", "hi"),
                    voiceInputLanguage = "hi",
                )

            val string = backup.toString()
            assertTrue(string.contains("1.0"))
            assertTrue(string.contains("en"))
            assertTrue(string.contains("hi"))
        }
    }

    @Nested
    inner class EdgeCases {
        @Test
        fun `handles empty favorite languages list`() {
            val backup =
                Backup(
                    version = "1.0",
                    targetLanguage = "en",
                    favoriteLanguages = emptyList(),
                    voiceInputLanguage = "hi",
                )

            // Should not throw exception
            backup.validate()
        }

        @Test
        fun `handles large favorite languages list`() {
            val allCodes = Language.values().map { it.code }
            val backup =
                Backup(
                    version = "1.0",
                    targetLanguage = "en",
                    favoriteLanguages = allCodes,
                    voiceInputLanguage = "hi",
                )

            // Should not throw exception
            backup.validate()
        }

        @Test
        fun `handles duplicate language codes in favorites`() {
            val backup =
                Backup(
                    version = "1.0",
                    targetLanguage = null,
                    favoriteLanguages = listOf("en", "en", "hi"),
                    voiceInputLanguage = null,
                )

            // Should not throw exception for duplicates
            backup.validate()
        }

        @Test
        fun `handles very old timestamp`() {
            val backup =
                Backup(
                    timestamp = 0L,
                    version = "1.0",
                    targetLanguage = null,
                    favoriteLanguages = emptyList(),
                    voiceInputLanguage = null,
                )

            assertEquals(0L, backup.timestamp)
        }

        @Test
        fun `handles future timestamp`() {
            val futureTime = System.currentTimeMillis() + 1000000L
            val backup =
                Backup(
                    timestamp = futureTime,
                    version = "1.0",
                    targetLanguage = null,
                    favoriteLanguages = emptyList(),
                    voiceInputLanguage = null,
                )

            assertEquals(futureTime, backup.timestamp)
        }
    }
}

class BackupExceptionTest {
    @Nested
    inner class StorageException {
        @Test
        fun `creates exception with correct message`() {
            val exception = BackupException.StorageException("test message")

            assertTrue(exception.message?.contains("Backup storage error: test message") == true)
            assertTrue(exception is BackupException)
        }
    }

    @Nested
    inner class FormatException {
        @Test
        fun `creates exception with correct message`() {
            val exception = BackupException.FormatException("invalid format")

            assertTrue(exception.message?.contains("Backup format error: invalid format") == true)
            assertTrue(exception is BackupException)
        }
    }

    @Nested
    inner class ValidationException {
        @Test
        fun `creates exception with correct message`() {
            val exception = BackupException.ValidationException("validation failed")

            assertTrue(exception.message?.contains("Backup validation failed: validation failed") == true)
            assertTrue(exception is BackupException)
        }
    }

    @Nested
    inner class VersionException {
        @Test
        fun `creates exception with correct message`() {
            val exception = BackupException.VersionException("2.0")

            assertTrue(exception.message?.contains("Incompatible backup version: 2.0") == true)
            assertTrue(exception is BackupException)
        }
    }

    @Test
    fun `all exceptions extend BackupException`() {
        val storage = BackupException.StorageException("test")
        val format = BackupException.FormatException("test")
        val validation = BackupException.ValidationException("test")
        val version = BackupException.VersionException("test")

        assertTrue(storage is BackupException)
        assertTrue(format is BackupException)
        assertTrue(validation is BackupException)
        assertTrue(version is BackupException)
    }

    @Test
    fun `all exceptions extend Exception`() {
        val storage = BackupException.StorageException("test")
        val format = BackupException.FormatException("test")
        val validation = BackupException.ValidationException("test")
        val version = BackupException.VersionException("test")

        assertTrue(storage is Exception)
        assertTrue(format is Exception)
        assertTrue(validation is Exception)
        assertTrue(version is Exception)
    }
}
