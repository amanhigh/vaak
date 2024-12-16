package com.aman.vaak.managers

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.provider.Settings
import javax.inject.Inject

/**
 * System Manager provides access to Android system services and static method calls.
 * This class centralizes all static Android SDK calls to make the code more testable.
 * Wrap any new static Android SDK method calls here.
 */
interface SystemManager {
    /**
     * Gets currently selected input method from system settings
     * @return Package name of current input method or null if none selected
     */
    fun getDefaultInputMethod(): String?

    /**
     * Gets minimum buffer size for audio recording
     * @param sampleRate - Audio sample rate in Hz (e.g. 44100)
     * @param channelConfig - Channel configuration from AudioFormat
     * @param audioFormat - Audio format from AudioFormat
     * @return Minimum buffer size in bytes or error code
     */
    fun getMinBufferSize(
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int
    ): Int

    /**
     * Creates an AudioRecord instance for recording
     * @param source - Recording source from MediaRecorder.AudioSource
     * @param sampleRate - Audio sample rate in Hz
     * @param channelConfig - Channel configuration from AudioFormat
     * @param audioFormat - Audio format from AudioFormat
     * @param bufferSize - Buffer size in bytes
     * @return Configured AudioRecord instance
     */
    fun createAudioRecord(
        source: Int,
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
        bufferSize: Int
    ): AudioRecord

    /**
     * Checks if a permission is granted
     * @param permission The permission to check
     * @return Permission grant status from PackageManager
     */
    fun checkSelfPermission(permission: String): Int

    /**
     * Gets the content resolver
     * @return System content resolver
     */
    fun getContentResolver(): ContentResolver

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
}

class SystemManagerImpl @Inject constructor(
    private val context: Context,
    private val contentResolver: ContentResolver
) : SystemManager {
    private val requiredPermissions = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.INTERNET
    )

    override fun getDefaultInputMethod(): String? =
        Settings.Secure.getString(
            contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )

    override fun getMinBufferSize(
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int
    ): Int = AudioRecord.getMinBufferSize(
        sampleRate,
        channelConfig,
        audioFormat
    )

    override fun createAudioRecord(
        source: Int,
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
        bufferSize: Int
    ): AudioRecord {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != 
            PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("RECORD_AUDIO permission not granted")
        }

        return AudioRecord(
            source,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
    }

    override fun checkSelfPermission(permission: String): Int =
        context.checkSelfPermission(permission)

    override fun getContentResolver(): ContentResolver = contentResolver

    override fun hasRequiredPermissions(): Boolean =
        requiredPermissions.all { permission ->
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }

    override fun getRequiredPermissions(): Array<String> = requiredPermissions
}
