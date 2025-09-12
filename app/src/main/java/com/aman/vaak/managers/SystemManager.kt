package com.aman.vaak.managers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
     * Data class for audio recording parameters
     */
    data class AudioRecordParams(
        val source: Int,
        val sampleRate: Int,
        val channelConfig: Int,
        val audioFormat: Int,
        val bufferSize: Int,
    )

    /**
     * Data class for notification builder parameters
     */
    data class NotificationBuilderParams(
        val channelId: String,
        val title: String,
        val message: String,
        val priority: Int,
        val autoCancel: Boolean,
    )

    /**
     * Creates an AudioRecord instance for recording
     * @param params Audio recording configuration parameters
     * @return Configured AudioRecord instance
     */
    fun createAudioRecord(params: AudioRecordParams): AudioRecord

    /**
     * Creates notification builder with standard configuration
     * @param params Notification builder configuration parameters
     * @return Configured NotificationCompat.Builder
     */
    fun createNotificationBuilder(params: NotificationBuilderParams): NotificationCompat.Builder

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
     * Checks if the app has permission to draw overlays
     * @return true if overlay permission is granted, false otherwise
     */
    fun canDrawOverlays(): Boolean

    /**
     * Gets intent to request overlay permission
     * @return Intent to open overlay permission settings
     */
    fun getOverlaySettingsIntent(): Intent
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

        override fun createAudioRecord(params: SystemManager.AudioRecordParams): AudioRecord {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                throw SecurityException("RECORD_AUDIO permission not granted")
            }

            return AudioRecord(
                params.source,
                params.sampleRate,
                params.channelConfig,
                params.audioFormat,
                params.bufferSize,
            )
        }

        override fun checkSelfPermission(permission: String): Int = context.checkSelfPermission(permission)

        override fun hasRequiredPermissions(): Boolean =
            requiredPermissions.all { permission ->
                checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            }

        override fun getRequiredPermissions(): Array<String> = requiredPermissions

        override fun isOreoOrHigher(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

        override fun createNotificationBuilder(params: SystemManager.NotificationBuilderParams): NotificationCompat.Builder =
            NotificationCompat.Builder(context, params.channelId)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(params.title)
                .setContentText(params.message)
                .setPriority(params.priority)
                .setAutoCancel(params.autoCancel)

        override fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

        override fun getOverlaySettingsIntent(): Intent =
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
    }
