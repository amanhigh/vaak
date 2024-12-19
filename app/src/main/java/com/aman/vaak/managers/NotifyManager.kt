package com.aman.vaak.managers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
    fun showInfo(title: String, message: String, autoCancel: Boolean = true)

    /**
     * Shows a warning notification
     * @param title Notification title
     * @param message Notification message
     * @param autoCancel Whether notification should auto-cancel on tap
     */
    fun showWarning(title: String, message: String, autoCancel: Boolean = true)

    /**
     * Shows an error notification
     * @param title Notification title
     * @param message Notification message
     * @param autoCancel Whether notification should auto-cancel on tap
     */
    fun showError(title: String, message: String, autoCancel: Boolean = true)

    /** Cleans up resources and channels */
    fun release()
}

class NotifyManagerImpl @Inject constructor(@ApplicationContext private val context: Context) :
        NotifyManager {
    private companion object {
        const val CHANNEL_INFO_ID = "vaak_info_channel"
        const val CHANNEL_WARNING_ID = "vaak_warning_channel"
        const val CHANNEL_ERROR_ID = "vaak_error_channel"

        const val NOTIFICATION_INFO_ID = 1001
        const val NOTIFICATION_WARNING_ID = 1002
        const val NOTIFICATION_ERROR_ID = 1003
    }

    private val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val infoChannel =
                    NotificationChannel(
                                    CHANNEL_INFO_ID,
                                    context.getString(R.string.notify_channel_info),
                                    NotificationManager.IMPORTANCE_DEFAULT
                            )
                            .apply {
                                description = context.getString(R.string.notify_channel_info_desc)
                            }

            val warningChannel =
                    NotificationChannel(
                                    CHANNEL_WARNING_ID,
                                    context.getString(R.string.notify_channel_warning),
                                    NotificationManager.IMPORTANCE_HIGH
                            )
                            .apply {
                                description =
                                        context.getString(R.string.notify_channel_warning_desc)
                            }

            val errorChannel =
                    NotificationChannel(
                                    CHANNEL_ERROR_ID,
                                    context.getString(R.string.notify_channel_error),
                                    NotificationManager.IMPORTANCE_HIGH
                            )
                            .apply {
                                description = context.getString(R.string.notify_channel_error_desc)
                            }

            notificationManager.createNotificationChannels(
                    listOf(infoChannel, warningChannel, errorChannel)
            )
        }
    }

    private fun buildNotification(
            channelId: String,
            title: String,
            message: String,
            priority: Int,
            autoCancel: Boolean
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority)
                .setAutoCancel(autoCancel)
    }

    override fun showInfo(title: String, message: String, autoCancel: Boolean) {
        val notification =
                buildNotification(
                                CHANNEL_INFO_ID,
                                title,
                                message,
                                NotificationCompat.PRIORITY_DEFAULT,
                                autoCancel
                        )
                        .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_INFO_ID, notification)
    }

    override fun showWarning(title: String, message: String, autoCancel: Boolean) {
        val notification =
                buildNotification(
                                CHANNEL_WARNING_ID,
                                title,
                                message,
                                NotificationCompat.PRIORITY_HIGH,
                                autoCancel
                        )
                        .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_WARNING_ID, notification)
    }

    override fun showError(title: String, message: String, autoCancel: Boolean) {
        val notification =
                buildNotification(
                                CHANNEL_ERROR_ID,
                                title,
                                message,
                                NotificationCompat.PRIORITY_HIGH,
                                autoCancel
                        )
                        .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ERROR_ID, notification)
    }

    override fun release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(CHANNEL_INFO_ID)
            notificationManager.deleteNotificationChannel(CHANNEL_WARNING_ID)
            notificationManager.deleteNotificationChannel(CHANNEL_ERROR_ID)
        }
    }
}
