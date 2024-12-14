package com.aman.vaak

import android.content.Context
import com.aman.vaak.managers.ClipboardManagerImpl
import com.aman.vaak.managers.ClipboardManager
import com.aman.vaak.repositories.ClipboardRepository
import com.aman.vaak.repositories.ClipboardRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VaakModule {
    @Provides
    @Singleton
    fun provideAndroidClipboardManager(
        @ApplicationContext context: Context
    ): android.content.ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

    @Provides
    @Singleton
    fun provideClipboardRepository(
        clipboardManager: android.content.ClipboardManager
    ): ClipboardRepository = ClipboardRepositoryImpl(clipboardManager)

    @Provides
    @Singleton
    fun provideClipboardManager(
        repository: ClipboardRepository
    ): ClipboardManager = ClipboardManagerImpl(repository)
}