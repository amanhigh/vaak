package com.aman.vaak.managers

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aman.vaak.R
import javax.inject.Inject

/**
 * System Manager provides access to Android system services and static method calls. This class
 * centralizes all static Android SDK calls to make the code more testable. Wrap any new static
 * Android SDK method calls here.
 */
interface SystemManager {
    /**
     * Gets minimum buffer size for audio recording
     * @param sampleRate
     * - Audio sample rate in Hz (e.g. 44100)
     * @param channelConfig
     * - Channel configuration from AudioFormat
     * @param audioFormat
     * - Audio format from AudioFormat
     * @return Minimum buffer size in bytes or error code
     */
    fun getMinBufferSize(
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
    ): Int

    /**
     * Creates an AudioRecord instance for recording
     * @param source
     * - Recording source from MediaRecorder.AudioSource
     * @param sampleRate
     * - Audio sample rate in Hz
     * @param channelConfig
     * - Channel configuration from AudioFormat
     * @param audioFormat
     * - Audio format from AudioFormat
     * @param bufferSize
     * - Buffer size in bytes
     * @return Configured AudioRecord instance
     */
    fun createAudioRecord(
        source: Int,
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
        bufferSize: Int,
    ): AudioRecord

    /**
     * Checks if a permission is granted
     * @param permission The permission to check
     * @return Permission grant status from PackageManager
     */
    fun checkSelfPermission(permission: String): Int

    /**
     * Checks if all required permissions are granted
     * @return true if all permissions granted, false otherwise
     */
    fun hasRequiredPermissions(): Boolean

    /**
     * Gets array of permissions required by the keyboard
     * @return Array of required permission strings
     */
    fun getRequiredPermissions(): Array<String>

    /**
     * Checks if device API level is 26 (Oreo) or higher
     * @return true if running on Oreo or higher
     */
    fun isOreoOrHigher(): Boolean

    /**
     * Creates notification builder with standard configuration
     * @param channelId Channel identifier for notification
     * @param title Notification title
     * @param message Notification message
     * @param priority Priority level from NotificationCompat
     * @param autoCancel Whether notification auto-cancels on tap
     * @return Configured NotificationCompat.Builder
     */
    fun createNotificationBuilder(
        channelId: String,
        title: String,
        message: String,
        priority: Int,
        autoCancel: Boolean,
    ): NotificationCompat.Builder
}

class SystemManagerImpl
    @Inject
    constructor(private val context: Context) :
    SystemManager {
        private val requiredPermissions =
            arrayOf(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.POST_NOTIFICATIONS,
            )

        override fun getMinBufferSize(
            sampleRate: Int,
            channelConfig: Int,
            audioFormat: Int,
        ): Int = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        override fun createAudioRecord(
            source: Int,
            sampleRate: Int,
            channelConfig: Int,
            audioFormat: Int,
            bufferSize: Int,
        ): AudioRecord {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                throw SecurityException("RECORD_AUDIO permission not granted")
            }

            return AudioRecord(source, sampleRate, channelConfig, audioFormat, bufferSize)
        }

        override fun checkSelfPermission(permission: String): Int = context.checkSelfPermission(permission)

        override fun hasRequiredPermissions(): Boolean =
            requiredPermissions.all { permission ->
                checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            }

        override fun getRequiredPermissions(): Array<String> = requiredPermissions

        override fun isOreoOrHigher(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

        override fun createNotificationBuilder(
            channelId: String,
            title: String,
            message: String,
            priority: Int,
            autoCancel: Boolean,
        ): NotificationCompat.Builder =
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority)
                .setAutoCancel(autoCancel)
    }
