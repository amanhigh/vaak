package com.aman.vaak.handlers

import android.content.Context
import android.view.View
import android.widget.Button
import com.aman.vaak.R
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.SettingsManager
import com.aman.vaak.models.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettingsHandlerTest {
    @Mock
    private lateinit var settingsManager: SettingsManager

    @Mock
    private lateinit var notifyManager: NotifyManager

    @Mock
    private lateinit var appContext: Context

    @Mock
    private lateinit var view: View

    @Mock
    private lateinit var settingsButton: Button

    @Mock
    private lateinit var languageButton: Button

    private lateinit var handler: SettingsHandlerImpl

    @BeforeEach
    fun setup() {
        handler =
            SettingsHandlerImpl(
                settingsManager,
                notifyManager,
                appContext,
            )

        whenever(view.context).thenReturn(appContext)
        whenever(view.findViewById<Button>(R.id.settingsButton)).thenReturn(settingsButton)
        whenever(view.findViewById<Button>(R.id.languageButton)).thenReturn(languageButton)
        whenever(appContext.getString(any())).thenReturn("Error")
    }

    @Nested
    inner class ViewLifecycleTests {
        @Test
        fun `onViewAttached sets up settings button`() {
            handler.attachView(view)

            verify(view).findViewById<Button>(R.id.settingsButton)
        }
    }

    @Nested
    inner class LanguageCyclingTests {
        @Test
        fun `cycleLanguage stays on auto-detect when no favorites`() {
            whenever(settingsManager.getVoiceInputLanguage()).thenReturn(null)
            whenever(settingsManager.getFavoriteLanguages()).thenReturn(emptyList())

            handler.cycleLanguage()

            verify(settingsManager).saveVoiceInputLanguage(null)
        }

        @Test
        fun `cycleLanguage goes to first favorite when on auto-detect`() {
            val favorites = listOf(Language.ENGLISH, Language.HINDI)
            whenever(settingsManager.getVoiceInputLanguage()).thenReturn(null)
            whenever(settingsManager.getFavoriteLanguages()).thenReturn(favorites)

            handler.cycleLanguage()

            verify(settingsManager).saveVoiceInputLanguage(Language.ENGLISH)
        }

        @Test
        fun `cycleLanguage goes to next favorite in list`() {
            val favorites = listOf(Language.ENGLISH, Language.HINDI, Language.SPANISH)
            whenever(settingsManager.getVoiceInputLanguage()).thenReturn(Language.ENGLISH)
            whenever(settingsManager.getFavoriteLanguages()).thenReturn(favorites)

            handler.cycleLanguage()

            verify(settingsManager).saveVoiceInputLanguage(Language.HINDI)
        }

        @Test
        fun `cycleLanguage wraps to auto-detect after last favorite`() {
            val favorites = listOf(Language.ENGLISH, Language.HINDI)
            whenever(settingsManager.getVoiceInputLanguage()).thenReturn(Language.HINDI)
            whenever(settingsManager.getFavoriteLanguages()).thenReturn(favorites)

            handler.cycleLanguage()

            verify(settingsManager).saveVoiceInputLanguage(null)
        }

        @Test
        fun `cycleLanguage goes to first favorite when current not in list`() {
            val favorites = listOf(Language.ENGLISH, Language.HINDI)
            whenever(settingsManager.getVoiceInputLanguage()).thenReturn(Language.SPANISH)
            whenever(settingsManager.getFavoriteLanguages()).thenReturn(favorites)

            handler.cycleLanguage()

            verify(settingsManager).saveVoiceInputLanguage(Language.ENGLISH)
        }

        @Test
        fun `cycleLanguage with single favorite cycles between it and auto-detect`() {
            val favorites = listOf(Language.ENGLISH)
            whenever(settingsManager.getVoiceInputLanguage()).thenReturn(Language.ENGLISH)
            whenever(settingsManager.getFavoriteLanguages()).thenReturn(favorites)

            handler.cycleLanguage()

            verify(settingsManager).saveVoiceInputLanguage(null)
        }
    }

    @Nested
    inner class SettingsActivityLaunchTests {
        @Test
        fun `launchSettings starts activity`() {
            handler.launchSettings()

            verify(appContext).startActivity(any())
        }
    }
}
