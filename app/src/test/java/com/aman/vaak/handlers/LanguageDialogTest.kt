package com.aman.vaak.handlers

import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.SettingsManager
import com.aman.vaak.models.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LanguageDialogTest {
    @Mock
    private lateinit var settingsManager: SettingsManager

    @Mock
    private lateinit var notifyManager: NotifyManager

    @Nested
    inner class FavoriteLanguageDialogTests {
        private lateinit var dialog: FavoriteLanguageDialog

        @BeforeEach
        fun setup() {
            val items = listOf(Language.ENGLISH, Language.HINDI, Language.SPANISH)
            val initialSelection = setOf(Language.ENGLISH)
            dialog =
                FavoriteLanguageDialog(
                    items,
                    initialSelection,
                    settingsManager,
                    notifyManager,
                )
        }

        @Test
        fun `dialog can be instantiated`() {
            assertNotNull(dialog)
        }

        @Test
        fun `getSelectedLanguages returns initial selection`() {
            val selected = dialog.getSelectedLanguages()

            assertEquals(1, selected.size)
            assertTrue(selected.contains(Language.ENGLISH))
        }
    }

    @Nested
    inner class VoiceInputLanguageDialogTests {
        private lateinit var dialog: VoiceInputLanguageDialog

        @BeforeEach
        fun setup() {
            val items = listOf(null, Language.ENGLISH, Language.HINDI, Language.SPANISH)
            val initialSelection = setOf<Language?>(Language.ENGLISH)
            dialog =
                VoiceInputLanguageDialog(
                    items,
                    initialSelection,
                    settingsManager,
                    notifyManager,
                )
        }

        @Test
        fun `dialog can be instantiated`() {
            assertNotNull(dialog)
        }

        @Test
        fun `getSelectedLanguages returns initial selection`() {
            val selected = dialog.getSelectedLanguages()

            assertEquals(1, selected.size)
            assertTrue(selected.contains(Language.ENGLISH))
        }
    }
}
