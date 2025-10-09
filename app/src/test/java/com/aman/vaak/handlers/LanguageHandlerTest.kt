package com.aman.vaak.handlers

import android.content.Context
import android.view.View
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.SettingsManager
import com.aman.vaak.models.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LanguageHandlerTest {
    @Mock
    private lateinit var settingsManager: SettingsManager

    @Mock
    private lateinit var notifyManager: NotifyManager

    @Mock
    private lateinit var favoriteDialog: LanguageDialog

    @Mock
    private lateinit var voiceInputDialog: LanguageDialog

    @Mock
    private lateinit var view: View

    @Mock
    private lateinit var context: Context

    private lateinit var handler: LanguageHandlerImpl

    @BeforeEach
    fun setup() {
        handler =
            LanguageHandlerImpl(
                settingsManager,
                notifyManager,
                favoriteDialog,
                voiceInputDialog,
            )

        whenever(view.context).thenReturn(context)
        whenever(context.getString(any())).thenReturn("Error")
    }

    @Nested
    inner class FavoriteLanguageDialogTests {
        @Test
        fun `showFavoriteLanguageSelection gets current favorites from settings`() {
            val favorites = listOf(Language.ENGLISH, Language.HINDI)
            whenever(settingsManager.getFavoriteLanguages()).thenReturn(favorites)

            handler.showFavoriteLanguageSelection(context)

            verify(settingsManager).getFavoriteLanguages()
            verify(favoriteDialog).updateSelection(favorites.toSet())
        }

        @Test
        fun `showFavoriteLanguageSelection shows dialog`() {
            whenever(settingsManager.getFavoriteLanguages()).thenReturn(emptyList())

            handler.showFavoriteLanguageSelection(context)

            verify(favoriteDialog).show(eq(context), any())
        }

        @Test
        fun `favorite dialog callback saves selected languages`() {
            val callbackCaptor = argumentCaptor<(List<Language?>) -> Unit>()
            whenever(settingsManager.getFavoriteLanguages()).thenReturn(emptyList())

            handler.showFavoriteLanguageSelection(context)
            verify(favoriteDialog).show(eq(context), callbackCaptor.capture())

            val selectedLanguages = listOf(Language.ENGLISH, Language.HINDI)
            callbackCaptor.firstValue.invoke(selectedLanguages)

            verify(settingsManager).saveFavoriteLanguages(selectedLanguages)
        }

        @Test
        fun `favorite dialog callback filters out null languages`() {
            val callbackCaptor = argumentCaptor<(List<Language?>) -> Unit>()
            whenever(settingsManager.getFavoriteLanguages()).thenReturn(emptyList())

            handler.showFavoriteLanguageSelection(context)
            verify(favoriteDialog).show(eq(context), callbackCaptor.capture())

            val selectedLanguages = listOf(Language.ENGLISH, null, Language.HINDI)
            callbackCaptor.firstValue.invoke(selectedLanguages)

            verify(settingsManager).saveFavoriteLanguages(listOf(Language.ENGLISH, Language.HINDI))
        }
    }

    @Nested
    inner class VoiceInputDialogTests {
        @Test
        fun `showVoiceInputLanguageSelection gets current voice input from settings`() {
            val voiceInputLang = Language.ENGLISH
            whenever(settingsManager.getVoiceInputLanguage()).thenReturn(voiceInputLang)

            handler.showVoiceInputLanguageSelection(context)

            verify(settingsManager).getVoiceInputLanguage()
            verify(voiceInputDialog).updateSelection(setOf(voiceInputLang))
        }

        @Test
        fun `showVoiceInputLanguageSelection handles null voice input`() {
            whenever(settingsManager.getVoiceInputLanguage()).thenReturn(null)

            handler.showVoiceInputLanguageSelection(context)

            verify(voiceInputDialog).updateSelection(emptySet())
        }

        @Test
        fun `voice input dialog callback saves first selected language`() {
            val callbackCaptor = argumentCaptor<(List<Language?>) -> Unit>()
            whenever(settingsManager.getVoiceInputLanguage()).thenReturn(null)

            handler.showVoiceInputLanguageSelection(context)
            verify(voiceInputDialog).show(eq(context), callbackCaptor.capture())

            val selectedLanguages = listOf(Language.ENGLISH, Language.HINDI)
            callbackCaptor.firstValue.invoke(selectedLanguages)

            verify(settingsManager).saveVoiceInputLanguage(Language.ENGLISH)
        }

        @Test
        fun `voice input dialog callback handles empty selection`() {
            val callbackCaptor = argumentCaptor<(List<Language?>) -> Unit>()
            whenever(settingsManager.getVoiceInputLanguage()).thenReturn(Language.ENGLISH)

            handler.showVoiceInputLanguageSelection(context)
            verify(voiceInputDialog).show(eq(context), callbackCaptor.capture())

            callbackCaptor.firstValue.invoke(emptyList())

            verify(settingsManager).saveVoiceInputLanguage(null)
        }
    }

    @Nested
    inner class ListenerManagementTests {
        @Test
        fun `registerFavoriteLanguagesListener stores listener`() {
            var listenerInvoked = false
            val listener = { listenerInvoked = true }

            handler.registerFavoriteLanguagesListener(listener)

            val callbackCaptor = argumentCaptor<(List<Language?>) -> Unit>()
            whenever(settingsManager.getFavoriteLanguages()).thenReturn(emptyList())
            handler.showFavoriteLanguageSelection(context)
            verify(favoriteDialog).show(eq(context), callbackCaptor.capture())

            callbackCaptor.firstValue.invoke(listOf(Language.ENGLISH))

            assertEquals(true, listenerInvoked)
        }

        @Test
        fun `registerVoiceInputListener stores listener`() {
            var listenerInvoked = false
            val listener = { listenerInvoked = true }

            handler.registerVoiceInputListener(listener)

            val callbackCaptor = argumentCaptor<(List<Language?>) -> Unit>()
            whenever(settingsManager.getVoiceInputLanguage()).thenReturn(null)
            handler.showVoiceInputLanguageSelection(context)
            verify(voiceInputDialog).show(eq(context), callbackCaptor.capture())

            callbackCaptor.firstValue.invoke(listOf(Language.ENGLISH))

            assertEquals(true, listenerInvoked)
        }

        @Test
        fun `onViewDetached clears listeners`() {
            var favoriteListenerInvoked = false
            var voiceListenerInvoked = false

            handler.registerFavoriteLanguagesListener { favoriteListenerInvoked = true }
            handler.registerVoiceInputListener { voiceListenerInvoked = true }

            handler.attachView(view)
            handler.detachView()

            val callbackCaptor = argumentCaptor<(List<Language?>) -> Unit>()
            whenever(settingsManager.getFavoriteLanguages()).thenReturn(emptyList())
            handler.showFavoriteLanguageSelection(context)
            verify(favoriteDialog).show(eq(context), callbackCaptor.capture())
            callbackCaptor.firstValue.invoke(listOf(Language.ENGLISH))

            assertEquals(false, favoriteListenerInvoked)
        }
    }

    @Nested
    inner class DisplayTextTests {
        @Test
        fun `getFavoriteLanguagesDisplayText formats multiple languages`() {
            val favorites = listOf(Language.ENGLISH, Language.HINDI)
            whenever(settingsManager.getFavoriteLanguages()).thenReturn(favorites)

            val displayText = handler.getFavoriteLanguagesDisplayText()

            assertEquals("English (English)\nहिन्दी (Hindi)", displayText)
        }

        @Test
        fun `getFavoriteLanguagesDisplayText returns empty for no favorites`() {
            whenever(settingsManager.getFavoriteLanguages()).thenReturn(emptyList())

            val displayText = handler.getFavoriteLanguagesDisplayText()

            assertEquals("", displayText)
        }

        @Test
        fun `getVoiceInputDisplayText returns language name`() {
            whenever(settingsManager.getVoiceInputLanguage()).thenReturn(Language.ENGLISH)

            val displayText = handler.getVoiceInputDisplayText()

            assertEquals("English (English)", displayText)
        }

        @Test
        fun `getVoiceInputDisplayText returns Auto Detect for null`() {
            whenever(settingsManager.getVoiceInputLanguage()).thenReturn(null)

            val displayText = handler.getVoiceInputDisplayText()

            assertEquals("Auto Detect", displayText)
        }
    }
}
