package com.aman.vaak.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LanguageTest {
    @Nested
    inner class EnumProperties {
        @Test
        fun `all languages have non-blank properties`() {
            Language.values().forEach { language ->
                assertNotNull(language.code, "Language ${language.name} has null code")
                assertNotNull(language.displayCode, "Language ${language.name} has null displayCode")
                assertNotNull(language.nativeName, "Language ${language.name} has null nativeName")
                assertNotNull(language.englishName, "Language ${language.name} has null englishName")

                assert(language.code.isNotBlank()) { "Language ${language.name} has blank code" }
                assert(language.displayCode.isNotBlank()) { "Language ${language.name} has blank displayCode" }
                assert(language.nativeName.isNotBlank()) { "Language ${language.name} has blank nativeName" }
                assert(language.englishName.isNotBlank()) { "Language ${language.name} has blank englishName" }
            }
        }

        @Test
        fun `all languages have unique codes`() {
            val codes = Language.values().map { it.code }
            val uniqueCodes = codes.distinct()
            assertEquals(codes.size, uniqueCodes.size, "Duplicate language codes found")
        }

        @Test
        fun `all languages have unique display codes`() {
            val displayCodes = Language.values().map { it.displayCode }
            val uniqueDisplayCodes = displayCodes.distinct()
            assertEquals(displayCodes.size, uniqueDisplayCodes.size, "Duplicate display codes found")
        }

        @Test
        fun `English is first language with expected values`() {
            val english = Language.ENGLISH
            assertEquals("en", english.code)
            assertEquals("EN", english.displayCode)
            assertEquals("English", english.nativeName)
            assertEquals("English", english.englishName)
        }

        @Test
        fun `Hindi has expected values`() {
            val hindi = Language.HINDI
            assertEquals("hi", hindi.code)
            assertEquals("हि", hindi.displayCode)
            assertEquals("हिन्दी", hindi.nativeName)
            assertEquals("Hindi", hindi.englishName)
        }

        @Test
        fun `all major languages are present`() {
            val expectedLanguages =
                setOf(
                    "ENGLISH", "HINDI", "PUNJABI", "THAI", "SPANISH",
                    "FRENCH", "GERMAN", "ITALIAN", "PORTUGUESE",
                    "DUTCH", "JAPANESE", "KOREAN", "CHINESE",
                )

            val actualLanguages = Language.values().map { it.name }.toSet()
            assertEquals(expectedLanguages, actualLanguages)
        }
    }

    @Nested
    inner class FromCodeFunction {
        @Test
        fun `returns correct language for valid codes`() {
            assertEquals(Language.ENGLISH, Language.fromCode("en"))
            assertEquals(Language.HINDI, Language.fromCode("hi"))
            assertEquals(Language.SPANISH, Language.fromCode("es"))
            assertEquals(Language.FRENCH, Language.fromCode("fr"))
            assertEquals(Language.GERMAN, Language.fromCode("de"))
            assertEquals(Language.CHINESE, Language.fromCode("zh"))
        }

        @Test
        fun `returns English for invalid codes`() {
            assertEquals(Language.ENGLISH, Language.fromCode("invalid"))
            assertEquals(Language.ENGLISH, Language.fromCode("xx"))
            assertEquals(Language.ENGLISH, Language.fromCode(""))
            assertEquals(Language.ENGLISH, Language.fromCode("random"))
        }

        @Test
        fun `is case sensitive`() {
            assertEquals(Language.ENGLISH, Language.fromCode("en"))
            assertEquals(Language.ENGLISH, Language.fromCode("EN"))
            assertEquals(Language.ENGLISH, Language.fromCode("En"))
        }

        @Test
        fun `handles all valid language codes`() {
            Language.values().forEach { language ->
                assertEquals(
                    language,
                    Language.fromCode(language.code),
                    "fromCode failed for ${language.name} with code '${language.code}'",
                )
            }
        }
    }

    @Nested
    inner class FromDisplayCodeFunction {
        @Test
        fun `returns correct language for valid display codes`() {
            assertEquals(Language.ENGLISH, Language.fromDisplayCode("EN"))
            assertEquals(Language.HINDI, Language.fromDisplayCode("हि"))
            assertEquals(Language.PUNJABI, Language.fromDisplayCode("ਪੰ"))
            assertEquals(Language.THAI, Language.fromDisplayCode("ไท"))
            assertEquals(Language.JAPANESE, Language.fromDisplayCode("日"))
            assertEquals(Language.KOREAN, Language.fromDisplayCode("한"))
            assertEquals(Language.CHINESE, Language.fromDisplayCode("中"))
        }

        @Test
        fun `returns English for invalid display codes`() {
            assertEquals(Language.ENGLISH, Language.fromDisplayCode("invalid"))
            assertEquals(Language.ENGLISH, Language.fromDisplayCode("XX"))
            assertEquals(Language.ENGLISH, Language.fromDisplayCode(""))
            assertEquals(Language.ENGLISH, Language.fromDisplayCode("random"))
        }

        @Test
        fun `handles all valid display codes`() {
            Language.values().forEach { language ->
                assertEquals(
                    language,
                    Language.fromDisplayCode(language.displayCode),
                    "fromDisplayCode failed for ${language.name} with displayCode '${language.displayCode}'",
                )
            }
        }
    }

    @Nested
    inner class EdgeCases {
        @Test
        fun `fromCode handles null-like values gracefully`() {
            assertEquals(Language.ENGLISH, Language.fromCode(""))
            assertEquals(Language.ENGLISH, Language.fromCode("   "))
        }

        @Test
        fun `fromDisplayCode handles null-like values gracefully`() {
            assertEquals(Language.ENGLISH, Language.fromDisplayCode(""))
            assertEquals(Language.ENGLISH, Language.fromDisplayCode("   "))
        }

        @Test
        fun `companion object functions are consistent`() {
            Language.values().forEach { language ->
                val fromCode = Language.fromCode(language.code)
                val fromDisplayCode = Language.fromDisplayCode(language.displayCode)

                assertEquals(
                    fromCode,
                    fromDisplayCode,
                    "Inconsistency for ${language.name}: fromCode and fromDisplayCode return different results",
                )
            }
        }
    }
}

class LanguageSelectionModeTest {
    @Test
    fun `enum has expected values`() {
        val modes = LanguageSelectionMode.values()
        assertEquals(2, modes.size)
        assert(modes.contains(LanguageSelectionMode.FAVORITE))
        assert(modes.contains(LanguageSelectionMode.VOICE_INPUT))
    }

    @Test
    fun `enum values have expected names`() {
        assertEquals("FAVORITE", LanguageSelectionMode.FAVORITE.name)
        assertEquals("VOICE_INPUT", LanguageSelectionMode.VOICE_INPUT.name)
    }
}
