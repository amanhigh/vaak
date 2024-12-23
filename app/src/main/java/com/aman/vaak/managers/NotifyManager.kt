package com.aman.vaak.managers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aman.vaak.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface NotifyManager {
    /**
     * Shows an informational notification
     * @param title Notification title
     * @param message Notification message
     * @param autoCancel Whether notification should auto-cancel on tap
     */
    fun showInfo(
        title: String,
        message: String,
        autoCancel: Boolean = true,
    )

    /**
     * Shows a warning notification
     * @param title Notification title
     * @param message Notification message
     * @param autoCancel Whether notification should auto-cancel on tap
     */
    fun showWarning(
        title: String,
        message: String,
        autoCancel: Boolean = true,
    )

    /**
     * Shows an error notification
     * @param title Notification title
     * @param message Notification message
     * @param autoCancel Whether notification should auto-cancel on tap
     */
    fun showError(
        title: String,
        message: String,
        autoCancel: Boolean = true,
    )

    /** Cleans up resources and channels */
    fun release()
}

class NotifyManagerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val notificationManager: NotificationManager,
        private val systemManager: SystemManager,
    ) : NotifyManager {
        private companion object {
            const val CHANNEL_INFO_ID = "vaak_info_channel"
            const val CHANNEL_WARNING_ID = "vaak_warning_channel"
            const val CHANNEL_ERROR_ID = "vaak_error_channel"

            const val NOTIFICATION_INFO_ID = 1001
            const val NOTIFICATION_WARNING_ID = 1002
            const val NOTIFICATION_ERROR_ID = 1003
        }

        init {
            createNotificationChannels()
        }

        private fun createNotificationChannels() {
            if (systemManager.isOreoOrHigher()) {
                listOf(
                    buildInfoChannel(),
                    buildWarningChannel(),
                    buildErrorChannel(),
                ).forEach { channel ->
                    notificationManager.createNotificationChannel(channel)
                }
            }
        }

        @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
        private fun buildInfoChannel() =
            NotificationChannel(
                CHANNEL_INFO_ID,
                context.getString(R.string.notify_channel_info),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notify_channel_info_desc)
            }

        @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
        private fun buildWarningChannel() =
            NotificationChannel(
                CHANNEL_WARNING_ID,
                context.getString(R.string.notify_channel_warning),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notify_channel_warning_desc)
            }

        @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
        private fun buildErrorChannel() =
            NotificationChannel(
                CHANNEL_ERROR_ID,
                context.getString(R.string.notify_channel_error),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notify_channel_error_desc)
            }

        private fun buildNotification(
            channelId: String,
            title: String,
            message: String,
            priority: Int,
            autoCancel: Boolean,
        ): Notification =
            systemManager
                .createNotificationBuilder(
                    channelId,
                    title,
                    message,
                    priority,
                    autoCancel,
                )
                .build()

        override fun showInfo(
            title: String,
            message: String,
            autoCancel: Boolean,
        ) {
            val notification =
                buildNotification(
                    CHANNEL_INFO_ID,
                    title,
                    message,
                    NotificationCompat.PRIORITY_DEFAULT,
                    autoCancel,
                )
            notificationManager.notify(NOTIFICATION_INFO_ID, notification)
        }

        override fun showWarning(
            title: String,
            message: String,
            autoCancel: Boolean,
        ) {
            val notification =
                buildNotification(
                    CHANNEL_WARNING_ID,
                    title,
                    message,
                    NotificationCompat.PRIORITY_HIGH,
                    autoCancel,
                )
            notificationManager.notify(NOTIFICATION_WARNING_ID, notification)
        }

        override fun showError(
            title: String,
            message: String,
            autoCancel: Boolean,
        ) {
            val notification =
                buildNotification(
                    CHANNEL_ERROR_ID,
                    title,
                    message,
                    NotificationCompat.PRIORITY_HIGH,
                    autoCancel,
                )
            notificationManager.notify(NOTIFICATION_ERROR_ID, notification)
        }

        override fun release() {
            if (systemManager.isOreoOrHigher()) {
                listOf(
                    CHANNEL_INFO_ID,
                    CHANNEL_WARNING_ID,
                    CHANNEL_ERROR_ID,
                ).forEach { channelId ->
                    notificationManager.deleteNotificationChannel(channelId)
                }
            }
        }
    }
