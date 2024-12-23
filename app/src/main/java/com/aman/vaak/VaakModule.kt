package com.aman.vaak

import android.app.NotificationManager
import android.content.Context
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.OpenAI
import com.aman.vaak.managers.ClipboardManager
import com.aman.vaak.managers.ClipboardManagerImpl
import com.aman.vaak.managers.DictationManager
import com.aman.vaak.managers.DictationManagerImpl
import com.aman.vaak.managers.FileManager
import com.aman.vaak.managers.FileManagerImpl
import com.aman.vaak.managers.KeyboardSetupManager
import com.aman.vaak.managers.KeyboardSetupManagerImpl
import com.aman.vaak.managers.NotifyManager
import com.aman.vaak.managers.NotifyManagerImpl
import com.aman.vaak.managers.SettingsManager
import com.aman.vaak.managers.SettingsManagerImpl
import com.aman.vaak.managers.SystemManager
import com.aman.vaak.managers.SystemManagerImpl
import com.aman.vaak.managers.TextManager
import com.aman.vaak.managers.TextManagerImpl
import com.aman.vaak.managers.VoiceManager
import com.aman.vaak.managers.VoiceManagerImpl
import com.aman.vaak.managers.WhisperManager
import com.aman.vaak.managers.WhisperManagerImpl
import com.aman.vaak.repositories.ClipboardRepository
import com.aman.vaak.repositories.ClipboardRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Module
@InstallIn(SingletonComponent::class)
object VaakModule {
    @Provides
    @Singleton
    fun provideAndroidClipboardManager(
        @ApplicationContext context: Context,
    ): android.content.ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

    @Provides
    @Singleton
    fun provideInputMethodManager(
        @ApplicationContext context: Context,
    ): InputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    @Provides
    @Singleton
    fun provideSystemManager(
        @ApplicationContext context: Context,
    ): SystemManager = SystemManagerImpl(context, context.contentResolver)

    @Provides
    @Singleton
    fun provideClipboardRepository(clipboardManager: android.content.ClipboardManager): ClipboardRepository =
        ClipboardRepositoryImpl(clipboardManager)

    @Provides
    @Singleton
    fun provideClipboardManager(repository: ClipboardRepository): ClipboardManager = ClipboardManagerImpl(repository)

    @Provides
    @Singleton
    fun provideKeyboardSetupManager(
        @ApplicationContext context: Context,
        inputMethodManager: InputMethodManager,
        systemManager: SystemManager,
        settingsManager: SettingsManager,
    ): KeyboardSetupManager =
        KeyboardSetupManagerImpl(
            packageName = context.packageName,
            inputMethodManager = inputMethodManager,
            systemManager = systemManager,
            settingsManager = settingsManager,
        )

    @Provides
    @Singleton
    fun provideSettingsManager(
        @ApplicationContext context: Context,
    ): SettingsManager = SettingsManagerImpl(context)

    @Provides @Singleton
    fun provideTextManager(scope: CoroutineScope): TextManager = TextManagerImpl(scope)

    @Provides
    @Singleton
    fun provideVoiceManager(
        systemManager: SystemManager,
        fileManager: FileManager,
        scope: CoroutineScope,
    ): VoiceManager = VoiceManagerImpl(systemManager, fileManager, scope)

    @Provides
    fun provideScope(): CoroutineScope = MainScope()

    @Provides
    @Singleton
    fun provideFileManager(
        @ApplicationContext context: Context,
    ): FileManager = FileManagerImpl(context)

    @Provides
    @Singleton
    fun provideDictationManager(
        voiceManager: VoiceManager,
        whisperManager: WhisperManager,
        fileManager: FileManager,
        scope: CoroutineScope,
    ): DictationManager = DictationManagerImpl(voiceManager, whisperManager, fileManager, scope)

    @Provides
    @Singleton
    fun provideOpenAIClient(settingsManager: SettingsManager): OpenAI {
        return OpenAI(
            token = settingsManager.getApiKey() ?: "",
            timeout = Timeout(socket = 60.seconds),
        )
    }

    @Provides
    @Singleton
    fun provideWhisperManager(
        openAI: OpenAI,
        settingsManager: SettingsManager,
        fileManager: FileManager,
    ): WhisperManager = WhisperManagerImpl(openAI, settingsManager, fileManager)

    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context,
    ): NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Provides
    @Singleton
    fun provideNotifyManager(
        @ApplicationContext context: Context,
        notificationManager: NotificationManager,
        systemManager: SystemManager,
    ): NotifyManager = NotifyManagerImpl(context, notificationManager, systemManager)

    @Provides
    @Singleton
    fun provideWindowManager(
        @ApplicationContext context: Context,
    ): WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
}
