package com.aman.vaak.managers

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.aman.vaak.R
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class NotifyManagerImplTest {
    @Mock private lateinit var context: Context

    @Mock private lateinit var notificationManager: NotificationManager

    @Mock private lateinit var systemManager: SystemManager

    @Mock private lateinit var notification: Notification

    @Mock private lateinit var notificationBuilder: NotificationCompat.Builder

    private lateinit var notifyManager: NotifyManagerImpl

    @BeforeEach
    fun setup() {
        whenever(systemManager.isOreoOrHigher()).thenReturn(true)
        setupStringResources()
    }

    private fun setupStringResources() {
        whenever(context.getString(R.string.notify_channel_info))
            .thenReturn("Information")
        whenever(context.getString(R.string.notify_channel_info_desc))
            .thenReturn("General information notifications")
        whenever(context.getString(R.string.notify_channel_warning))
            .thenReturn("Warnings")
        whenever(context.getString(R.string.notify_channel_warning_desc))
            .thenReturn("Important warning notifications")
        whenever(context.getString(R.string.notify_channel_error))
            .thenReturn("Errors")
        whenever(context.getString(R.string.notify_channel_error_desc))
            .thenReturn("Critical error notifications")
    }

    @Nested
    inner class InitializationTests {
        @Test
        fun `should verify system manager interactions during initialization`() {
            // When
            notifyManager = NotifyManagerImpl(context, notificationManager, systemManager)

            // Then
            verify(systemManager).isOreoOrHigher()
            verify(notificationManager, times(3)).createNotificationChannel(any())
        }
    }

    @Nested
    inner class NotificationTests {
        @BeforeEach
        fun setup() {
            whenever(systemManager.createNotificationBuilder(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(notificationBuilder)
            whenever(notificationBuilder.build()).thenReturn(notification)
            notifyManager = NotifyManagerImpl(context, notificationManager, systemManager)
        }

        @Test
        fun `showInfo should create and show notification with correct parameters`() {
            // When
            notifyManager.showInfo("title", "message")

            // Then
            verify(systemManager).createNotificationBuilder(
                eq("vaak_info_channel"),
                eq("title"),
                eq("message"),
                eq(NotificationCompat.PRIORITY_DEFAULT),
                eq(true),
            )
            verify(notificationManager).notify(eq(1001), eq(notification))
        }

        @Test
        fun `showWarning should create and show notification with correct parameters`() {
            // When
            notifyManager.showWarning("title", "message")

            // Then
            verify(systemManager).createNotificationBuilder(
                eq("vaak_warning_channel"),
                eq("title"),
                eq("message"),
                eq(NotificationCompat.PRIORITY_HIGH),
                eq(true),
            )
            verify(notificationManager).notify(eq(1002), eq(notification))
        }

        @Test
        fun `showError should create and show notification with correct parameters`() {
            // When
            notifyManager.showError("title", "message")

            // Then
            verify(systemManager).createNotificationBuilder(
                eq("vaak_error_channel"),
                eq("title"),
                eq("message"),
                eq(NotificationCompat.PRIORITY_HIGH),
                eq(true),
            )
            verify(notificationManager).notify(eq(1003), eq(notification))
        }
    }

    @Nested
    inner class ReleaseTests {
        @Test
        fun `release should delete notification channels on Oreo and above`() {
            // Given
            notifyManager = NotifyManagerImpl(context, notificationManager, systemManager)

            // When
            notifyManager.release()

            // Then
            verify(notificationManager).deleteNotificationChannel("vaak_info_channel")
            verify(notificationManager).deleteNotificationChannel("vaak_warning_channel")
            verify(notificationManager).deleteNotificationChannel("vaak_error_channel")
        }
    }
}
