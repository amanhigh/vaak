package com.aman.vaak.models

enum class Language(
    val code: String,
    val displayCode: String,
    val nativeName: String,
    val englishName: String,
) {
    ENGLISH("en", "EN", "English", "English"),
    HINDI("hi", "हि", "हिन्दी", "Hindi"),
    PUNJABI("pa", "ਪੰ", "ਪੰਜਾਬੀ", "Punjabi"),
    THAI("th", "ไท", "ไทย", "Thai"),
    SPANISH("es", "ES", "Español", "Spanish"),
    FRENCH("fr", "FR", "Français", "French"),
    GERMAN("de", "DE", "Deutsch", "German"),
    ITALIAN("it", "IT", "Italiano", "Italian"),
    PORTUGUESE("pt", "PT", "Português", "Portuguese"),
    DUTCH("nl", "NL", "Nederlands", "Dutch"),
    JAPANESE("ja", "日", "日本語", "Japanese"),
    KOREAN("ko", "한", "한국어", "Korean"),
    CHINESE("zh", "中", "中文", "Chinese"),
    ;

    companion object {
        fun fromCode(code: String): Language = values().find { it.code == code } ?: ENGLISH

        fun fromDisplayCode(displayCode: String): Language = values().find { it.displayCode == displayCode } ?: ENGLISH
    }
}

enum class LanguageSelectionMode {
    FAVORITE, // Multi-select (max 3) for keyboard cycling
    VOICE_INPUT, // Single-select with auto-detect
}
