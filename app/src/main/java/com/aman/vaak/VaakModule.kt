package com.aman.vaak

import android.content.Context
import com.aman.vaak.managers.ClipboardManager
import com.aman.vaak.repositories.ClipboardRepository
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
    fun provideClipboardRepository(
        @ApplicationContext context: Context
    ): ClipboardRepository = ClipboardRepository(context)

    @Provides
    @Singleton
    fun provideClipboardManager(
        repository: ClipboardRepository
    ): ClipboardManager = ClipboardManager(repository)
}
